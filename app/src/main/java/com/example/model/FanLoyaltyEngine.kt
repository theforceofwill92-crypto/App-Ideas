package com.example.model

import kotlin.math.roundToInt

enum class FanLoyaltyTier(
    val title: String,
    val minScore: Int,
    val badgeColorHex: Long,
    val summary: String
) {
    ALIENATED("Alienated", 0, 0xFFEF4444, "Empty upper decks, fan price boycotts & stalled merch sales"),
    RESTLESS("Restless", 30, 0xFFF59E0B, "Price-sensitive supporters demanding better results"),
    STEADY("Steady", 50, 0xFF3B82F6, "Reliable matchday attendance & consistent merch volume"),
    DEVOTED("Devoted", 70, 0xFF10B981, "Strong sellout demand, loyal merch buyers & sponsor appeal"),
    DIEHARD("Diehard Faithful", 85, 0xFFFFB800, "Packed fortress arenas, peak pricing resilience & commercial surge")
}

enum class FranchiseMarketingCampaignType(
    val title: String,
    val description: String,
    val costMillions: Long,
    val durationWeeks: Int,
    val loyaltyPerWeek: Int,
    val immediateLoyaltyBoost: Int,
    val fanbaseGrowthPct: Double,
    val targetSegment: String
) {
    COMMUNITY_FAN_FEST(
        title = "Neighborhood Fan Fest",
        description = "Free pre-game fan zones, youth clinics & community player appearances",
        costMillions = 4L,
        durationWeeks = 4,
        loyaltyPerWeek = 3,
        immediateLoyaltyBoost = 3,
        fanbaseGrowthPct = 3.5,
        targetSegment = "Families & Local Community"
    ),
    SEASON_TICKET_REWARDS(
        title = "Season-Ticket Loyalty Club",
        description = "Member concession rebates, priority playoff access & jersey vouchers",
        costMillions = 7L,
        durationWeeks = 5,
        loyaltyPerWeek = 4,
        immediateLoyaltyBoost = 4,
        fanbaseGrowthPct = 4.0,
        targetSegment = "Core Ticket Holders"
    ),
    CITY_HERITAGE_BLITZ(
        title = "City Heritage & Merch Blitz",
        description = "Iconic throwback uniform nights, street murals & downtown arena takeover",
        costMillions = 9L,
        durationWeeks = 4,
        loyaltyPerWeek = 4,
        immediateLoyaltyBoost = 5,
        fanbaseGrowthPct = 6.5,
        targetSegment = "Metro Supporters"
    ),
    GLOBAL_BRAND_TOUR(
        title = "Global Brand & Creator Spotlight",
        description = "Creator docu-series access, flagship broadcast promo & global fan push",
        costMillions = 12L,
        durationWeeks = 6,
        loyaltyPerWeek = 5,
        immediateLoyaltyBoost = 5,
        fanbaseGrowthPct = 9.0,
        targetSegment = "National & Global Audience"
    )
}

data class ActiveFranchiseCampaign(
    val type: FranchiseMarketingCampaignType,
    val remainingWeeks: Int,
    val totalLoyaltyGained: Int = 0
)

data class FanLoyaltyDrivers(
    val performanceDelta: Int = 0,
    val pricingDelta: Int = 0,
    val marketingDelta: Int = 0,
    val rivalryDelta: Int = 0,
    val lastSummary: String = "Preseason loyalty baseline"
) {
    val netDelta: Int
        get() = performanceDelta + pricingDelta + marketingDelta + rivalryDelta
}

data class TicketPriceLoyaltyPreview(
    val fairPrice: Int,
    val immediateDelta: Int,
    val weeklyTrendDelta: Int,
    val verdictLabel: String,
    val attendanceRate: Double,
    val projectedGateMillions: Double
)

data class FanLoyaltyWeekResult(
    val updatedFranchises: List<Franchise>,
    val headlines: List<String>
)

object FanLoyaltyEngine {

    fun tierFor(loyalty: Int): FanLoyaltyTier = when {
        loyalty >= FanLoyaltyTier.DIEHARD.minScore -> FanLoyaltyTier.DIEHARD
        loyalty >= FanLoyaltyTier.DEVOTED.minScore -> FanLoyaltyTier.DEVOTED
        loyalty >= FanLoyaltyTier.STEADY.minScore -> FanLoyaltyTier.STEADY
        loyalty >= FanLoyaltyTier.RESTLESS.minScore -> FanLoyaltyTier.RESTLESS
        else -> FanLoyaltyTier.ALIENATED
    }

    fun computeFairTicketPrice(franchise: Franchise): Int {
        val talentAdj = ((franchise.rosterRating - 72) * 0.85).roundToInt()
        val formAdj = ((franchise.winPct - 0.5) * 22.0).roundToInt()
        val pedigreeAdj = franchise.championships.coerceAtMost(3) * 3
        val marketAdj = ((franchise.fanBaseMillions - 1.5) * 4.0).roundToInt()
        return (62 + talentAdj + formAdj + pedigreeAdj + marketAdj).coerceIn(45, 115)
    }

    fun computeWeeklyPricingDelta(franchise: Franchise, price: Int = franchise.ticketPrice): Int {
        val fair = computeFairTicketPrice(franchise)
        val diff = price - fair
        val winningTeam = franchise.winPct >= 0.55
        return when {
            diff <= -18 -> +3
            diff <= -8 -> +2
            diff <= -3 -> +1
            diff <= 8 -> 0
            diff <= 18 -> if (winningTeam) 0 else -1
            diff <= 32 -> if (winningTeam) -1 else -2
            diff <= 50 -> if (winningTeam) -2 else -4
            else -> if (winningTeam) -3 else -5
        }
    }

    fun evaluateTicketPriceReaction(franchise: Franchise, oldPrice: Int, newPrice: Int): Int {
        val fair = computeFairTicketPrice(franchise)
        val priceDelta = newPrice - oldPrice
        if (priceDelta == 0) return 0

        return if (priceDelta > 0) {
            val aboveFair = (newPrice - fair).coerceAtLeast(0)
            val stepPenalty = (priceDelta / 12).coerceAtLeast(1)
            val gougeExtra = when {
                aboveFair >= 45 -> 3
                aboveFair >= 25 -> 2
                aboveFair >= 12 -> 1
                else -> 0
            }
            if (newPrice <= fair) 0 else -(stepPenalty + gougeExtra).coerceIn(1, 7)
        } else {
            val dropMagnitude = (-priceDelta / 10).coerceAtLeast(1)
            val valueBonus = if (newPrice <= fair - 5) 1 else 0
            (dropMagnitude + valueBonus).coerceIn(1, 5)
        }
    }

    fun computeAttendanceRate(
        franchise: Franchise,
        priceOverride: Int = franchise.ticketPrice,
        loyaltyOverride: Int = franchise.fanLoyalty
    ): Double {
        val fair = computeFairTicketPrice(franchise).toDouble()
        val loyaltyNorm = (loyaltyOverride.coerceIn(0, 100) - 65) / 100.0
        val loyaltyAttendanceBoost = loyaltyNorm * 0.36 // -23% at 0, +12.6% at 100
        val formBoost = (franchise.winPct - 0.5) * 0.14
        val moraleBoost = ((franchise.fanMorale - 70) / 100.0) * 0.08

        // Loyal fans stay resilient to premium pricing; low loyalty hurts elasticity cleanly
        val overpriceRatio = ((priceOverride - fair) / 100.0).coerceAtLeast(0.0)
        val priceSensitivity = (1.15 - (loyaltyOverride / 110.0)).coerceIn(0.25, 1.05)
        val priceElasticityDrag = overpriceRatio * priceSensitivity * 0.55

        val cheapSeatLift = (((fair - priceOverride) / 100.0).coerceAtLeast(0.0)) * 0.14

        return (0.79 + loyaltyAttendanceBoost + formBoost + moraleBoost + cheapSeatLift - priceElasticityDrag)
            .coerceIn(0.45, 1.0)
    }

    fun merchandiseLoyaltyMultiplier(fanLoyalty: Int): Double {
        return (0.70 + (fanLoyalty.coerceIn(0, 100) / 100.0) * 0.58).coerceIn(0.65, 1.30)
    }

    fun sponsorLoyaltyMultiplier(fanLoyalty: Int): Double {
        return (0.80 + (fanLoyalty.coerceIn(0, 100) / 100.0) * 0.40).coerceIn(0.75, 1.25)
    }

    fun previewTicketPriceImpact(franchise: Franchise, proposedPrice: Int): TicketPriceLoyaltyPreview {
        val fair = computeFairTicketPrice(franchise)
        val immediate = evaluateTicketPriceReaction(franchise, franchise.ticketPrice, proposedPrice)
        val weekly = computeWeeklyPricingDelta(franchise, proposedPrice)
        val projectedLoyalty = (franchise.fanLoyalty + immediate).coerceIn(10, 100)
        val attRate = computeAttendanceRate(franchise, priceOverride = proposedPrice, loyaltyOverride = projectedLoyalty)
        val avgAtt = (franchise.stadiumCapacity * attRate).toInt()
        val projectedGate = Math.round(((avgAtt * proposedPrice * 8) / 1_000_000.0) * 10.0) / 10.0

        val verdict = when {
            weekly >= 2 -> "Fan-Friendly Value (+Loyalty)"
            weekly == 1 -> "Accessible Pricing (+Loyalty)"
            weekly == 0 -> "Fair Market Equilibrium"
            weekly >= -2 -> "Premium Stretch (Mild Drag)"
            else -> "Price Gouging Alert (Fan Backlash)"
        }

        return TicketPriceLoyaltyPreview(
            fairPrice = fair,
            immediateDelta = immediate,
            weeklyTrendDelta = weekly,
            verdictLabel = verdict,
            attendanceRate = attRate,
            projectedGateMillions = projectedGate
        )
    }

    fun applyLeagueCampaignLaunchBoost(
        franchises: List<Franchise>,
        campaign: MarketingCampaign
    ): List<Franchise> {
        return franchises.map { team ->
            val boost = if (campaign.isInternational && team.conference.isContinentalInternational) 4 else 3
            val newLoyalty = (team.fanLoyalty + boost).coerceIn(10, 100)
            val growth = (campaign.fanGrowthPct / 100.0) * 0.25
            val newFanBase = Math.round((team.fanBaseMillions * (1.0 + growth)) * 100.0) / 100.0
            team.copy(
                fanLoyalty = newLoyalty,
                fanBaseMillions = newFanBase,
                lastLoyaltyDrivers = team.lastLoyaltyDrivers.copy(
                    marketingDelta = team.lastLoyaltyDrivers.marketingDelta + boost,
                    lastSummary = "Launched '${campaign.title}' (+$boost loyalty)"
                )
            )
        }
    }

    fun launchFranchiseCampaign(
        franchise: Franchise,
        campaignType: FranchiseMarketingCampaignType
    ): Franchise {
        val newLoyalty = (franchise.fanLoyalty + campaignType.immediateLoyaltyBoost).coerceIn(10, 100)
        val newFanBase = Math.round((franchise.fanBaseMillions * (1.0 + campaignType.fanbaseGrowthPct / 100.0)) * 100.0) / 100.0
        val newActive = ActiveFranchiseCampaign(
            type = campaignType,
            remainingWeeks = campaignType.durationWeeks,
            totalLoyaltyGained = campaignType.immediateLoyaltyBoost
        )
        return franchise.copy(
            fanLoyalty = newLoyalty,
            fanBaseMillions = newFanBase,
            activeFranchiseCampaign = newActive,
            lastLoyaltyDrivers = FanLoyaltyDrivers(
                marketingDelta = campaignType.immediateLoyaltyBoost,
                lastSummary = "Launched ${campaignType.title} (+${campaignType.immediateLoyaltyBoost} launch boost)"
            )
        )
    }

    fun applyChampionLoyaltyBoost(franchise: Franchise): Franchise {
        val boosted = (franchise.fanLoyalty + 8).coerceIn(10, 100)
        return franchise.copy(
            fanLoyalty = boosted,
            lastLoyaltyDrivers = franchise.lastLoyaltyDrivers.copy(
                performanceDelta = franchise.lastLoyaltyDrivers.performanceDelta + 8,
                lastSummary = "Championship Parade Loyalty Surge (+8)"
            )
        )
    }

    fun processWeeklyLoyaltyUpdates(
        franchises: List<Franchise>,
        playedMatches: List<GameMatch>,
        activeLeagueCampaigns: List<MarketingCampaign>,
        rivalryBonusesByTeamId: Map<String, Double>,
        isPlayoffs: Boolean
    ): FanLoyaltyWeekResult {
        val headlines = mutableListOf<String>()
        val matchByTeamId = mutableMapOf<String, GameMatch>()
        playedMatches.filter { it.isPlayed }.forEach { m ->
            matchByTeamId[m.homeTeamId] = m
            matchByTeamId[m.awayTeamId] = m
        }

        val activeLeagueCampCount = activeLeagueCampaigns.count { it.isActive && it.remainingWeeks > 0 }

        val updated = franchises.map { team ->
            val match = matchByTeamId[team.id]
            // 1. Performance delta
            var perfDelta = 0
            if (match != null) {
                val isHome = match.homeTeamId == team.id
                val won = if (isHome) match.homeScore > match.awayScore else match.awayScore > match.homeScore
                val margin = kotlin.math.abs(match.homeScore - match.awayScore)
                perfDelta += if (won) {
                    (if (isPlayoffs) 4 else 2) + (if (margin <= 4) 1 else 0)
                } else {
                    if (isPlayoffs) -2 else -2
                }
            }
            if (team.wins + team.losses >= 3) {
                when {
                    team.winPct >= 0.70 -> perfDelta += 1
                    team.winPct <= 0.30 -> perfDelta -= 1
                }
            }

            // 2. Ticket pricing delta
            val pricingDelta = computeWeeklyPricingDelta(team)

            // 3. Marketing campaigns delta (franchise-targeted + league-wide)
            var mktDelta = 0
            var nextCampaign = team.activeFranchiseCampaign
            var fanBaseGrowth = 0.0
            if (nextCampaign != null && nextCampaign.remainingWeeks > 0) {
                mktDelta += nextCampaign.type.loyaltyPerWeek
                fanBaseGrowth += (nextCampaign.type.fanbaseGrowthPct / nextCampaign.type.durationWeeks) / 100.0
                val rem = nextCampaign.remainingWeeks - 1
                nextCampaign = if (rem > 0) {
                    nextCampaign.copy(
                        remainingWeeks = rem,
                        totalLoyaltyGained = nextCampaign.totalLoyaltyGained + nextCampaign.type.loyaltyPerWeek
                    )
                } else null
            }

            if (activeLeagueCampCount > 0) {
                val leagueCampBoost = activeLeagueCampaigns
                    .filter { it.isActive && it.remainingWeeks > 0 }
                    .sumOf { camp ->
                        val intlBonus = if (camp.isInternational && team.conference.isContinentalInternational) 2 else 1
                        intlBonus.toInt()
                    }.coerceAtMost(4)
                mktDelta += leagueCampBoost
            }

            // 4. Rivalry atmosphere bonus if home clash earned extra gate
            val rivalryDelta = if ((rivalryBonusesByTeamId[team.id] ?: 0.0) > 0.0 && match != null) {
                val isHome = match.homeTeamId == team.id
                val won = if (isHome) match.homeScore > match.awayScore else match.awayScore > match.homeScore
                if (won) 1 else 0
            } else 0

            val totalDelta = perfDelta + pricingDelta + mktDelta + rivalryDelta
            val newLoyalty = (team.fanLoyalty + totalDelta).coerceIn(10, 100)
            val newFanBase = Math.round((team.fanBaseMillions * (1.0 + fanBaseGrowth)) * 100.0) / 100.0

            val oldTier = tierFor(team.fanLoyalty)
            val newTier = tierFor(newLoyalty)
            if (newTier != oldTier) {
                if (newLoyalty > team.fanLoyalty && newTier == FanLoyaltyTier.DIEHARD) {
                    headlines.add("FAN LOYALTY SURGE: ${team.teamFullName} fanbase reaches ${newTier.title} status ($newLoyalty/100), boosting arena fill & sponsor value!")
                } else if (newLoyalty < team.fanLoyalty && newTier.minScore <= FanLoyaltyTier.RESTLESS.minScore) {
                    headlines.add("FAN LOYALTY ALERT: ${team.teamFullName} loyalty slides to ${newTier.title} ($newLoyalty/100) amid ${if (pricingDelta < 0) "high ticket prices" else "on-court struggles"}!")
                }
            } else if (pricingDelta <= -3 && totalDelta <= -3) {
                headlines.add("TICKET BACKLASH: ${team.cityName} supporters protest \$${team.ticketPrice}/seat pricing, hitting ${team.nickname} fan loyalty ($newLoyalty/100).")
            }

            val summaryParts = buildList {
                if (perfDelta != 0) add("Perf ${if (perfDelta > 0) "+$perfDelta" else "$perfDelta"}")
                if (pricingDelta != 0) add("Price ${if (pricingDelta > 0) "+$pricingDelta" else "$pricingDelta"}")
                if (mktDelta != 0) add("Mkt +$mktDelta")
                if (rivalryDelta > 0) add("Rivalry +$rivalryDelta")
            }
            val summaryText = if (summaryParts.isEmpty()) "Steady week (no shift)" else summaryParts.joinToString(" • ")

            team.copy(
                fanLoyalty = newLoyalty,
                fanBaseMillions = newFanBase,
                activeFranchiseCampaign = nextCampaign,
                lastLoyaltyDrivers = FanLoyaltyDrivers(
                    performanceDelta = perfDelta,
                    pricingDelta = pricingDelta,
                    marketingDelta = mktDelta,
                    rivalryDelta = rivalryDelta,
                    lastSummary = summaryText
                )
            )
        }

        return FanLoyaltyWeekResult(
            updatedFranchises = updated,
            headlines = headlines.distinct().take(3)
        )
    }
}
