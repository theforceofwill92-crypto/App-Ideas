package com.example.model

import kotlin.math.pow
import kotlin.math.roundToLong

enum class BroadcastBidStatus(val label: String, val badgeColorHex: Long) {
    LEADING_BID("LEADING BID", 0xFF10B981),
    ACTIVE_OFFER("ACTIVE BID", 0xFF3B82F6),
    COUNTER_PENDING("COUNTERED", 0xFFF59E0B),
    WITHDRAWN("WITHDRAWN", 0xFFEF4444)
}

data class BroadcastYearlyProjection(
    val yearOffset: Int,
    val seasonYear: Int,
    val grossPayoutMillions: Long,
    val leagueTreasuryCutMillions: Long,
    val playerShareCostMillions: Long,
    val ownerPoolMillions: Long,
    val cumulativeGrossMillions: Long,
    val baselineCurrentAnnualMillions: Long,
    val netAdvantageVsCurrentMillions: Long
)

data class BroadcastLongTermImpact(
    val termYears: Int,
    val upfrontBonusMillions: Long,
    val year1AnnualPayoutMillions: Long,
    val finalYearAnnualPayoutMillions: Long,
    val annualEscalatorPct: Double,
    val cumulativeGrossValueMillions: Long,
    val totalLeagueTreasuryInflowMillions: Long,
    val averagePerTeamAnnualShareMillions: Double,
    val projectedValuationLiftMillions: Long,
    val projectedFanReachGrowthMillions: Double,
    val netGainVsCurrentContractMillions: Long,
    val yearlyProjections: List<BroadcastYearlyProjection>,
    val strategicVerdict: String
)

data class BroadcastContractOffer(
    val id: String,
    val bidder: NetworkBidder,
    val annualPayoutMillions: Long,
    val lumpSumBonusMillions: Long,
    val termYears: Int,
    val annualEscalatorPct: Double,
    val playoffBonusPerGameMillions: Long,
    val digitalExclusivityIncluded: Boolean = false,
    val playoffPrimetimeIncluded: Boolean = false,
    val currentPatience: Int,
    val latestExecutiveNote: String,
    val bidStatus: BroadcastBidStatus = BroadcastBidStatus.ACTIVE_OFFER,
    val maxAnnualCeilingMillions: Long,
    val maxBonusCeilingMillions: Long,
    val impact: BroadcastLongTermImpact,
    val isExclusivityExpired: Boolean = false
)

data class BroadcastBiddingSession(
    val biddingRound: Int = 1,
    val offers: List<BroadcastContractOffer> = emptyList(),
    val selectedOfferId: String? = null,
    val auctionLog: List<String> = emptyList(),
    val lastEscalationSummary: String? = null,
    val exclusivityRoundsTotal: Int = 3,
    val exclusivityRoundsRemaining: Int = 3,
    val exclusivityExpired: Boolean = false,
    val nonExclusiveRevenuePenaltyPct: Int = 22,
    val nonExclusiveReachPenaltyPct: Int = 20,
    val lastPeriodicBidWeek: Int = 0,
    val periodicBidsReceivedCount: Int = 0
)

data class PeriodicBroadcastTickResult(
    val updatedSession: BroadcastBiddingSession,
    val bidderName: String? = null,
    val headline: String? = null,
    val exclusivityJustExpired: Boolean = false
)

object BroadcastBiddingEngine {

    const val EXPIRY_REVENUE_PENALTY_PCT = 22
    const val EXPIRY_BONUS_PENALTY_PCT = 35
    const val EXPIRY_REACH_PENALTY_PCT = 20

    private fun defaultEscalatorForScope(scope: NetworkScope): Double = when (scope) {
        NetworkScope.GLOBAL_STREAMING -> 7.0
        NetworkScope.NATIONAL_CABLE -> 4.5
        NetworkScope.REGIONAL_RSN -> 5.0
        NetworkScope.FREE_TO_AIR -> 3.0
    }

    private fun defaultPlayoffBonusForScope(scope: NetworkScope): Long = when (scope) {
        NetworkScope.GLOBAL_STREAMING -> 6L
        NetworkScope.NATIONAL_CABLE -> 5L
        NetworkScope.FREE_TO_AIR -> 4L
        NetworkScope.REGIONAL_RSN -> 3L
    }

    fun effectiveReachForOffer(offer: BroadcastContractOffer): Double {
        return if (offer.isExclusivityExpired) {
            (offer.bidder.reachMultiplier * (1.0 - (EXPIRY_REACH_PENALTY_PCT / 100.0))).coerceAtLeast(0.85)
        } else {
            offer.bidder.reachMultiplier
        }
    }

    fun evaluateLongTermImpact(
        year1AnnualPayoutMillions: Long,
        lumpSumBonusMillions: Long,
        termYears: Int,
        annualEscalatorPct: Double,
        playoffBonusPerGameMillions: Long,
        reachMultiplier: Double,
        scope: NetworkScope,
        currentContract: BroadcastContract,
        teamCount: Int,
        playerRevenueSharePct: Int,
        currentFanbaseMillions: Double,
        currentYear: Int
    ): BroadcastLongTermImpact {
        val safeTerms = termYears.coerceIn(1, 7)
        val safeTeams = teamCount.coerceAtLeast(1)
        val clampedPlayerShare = playerRevenueSharePct.coerceIn(35, 65) / 100.0
        val escalatorRate = (annualEscalatorPct / 100.0).coerceIn(0.0, 0.15)

        val projections = mutableListOf<BroadcastYearlyProjection>()
        var cumulativeGross = lumpSumBonusMillions.toDouble()
        var totalAnnualSum = 0L
        var totalTreasuryFromAnnual = 0L
        var baselineCumulative = 0L

        for (y in 1..safeTerms) {
            val yearMultiplier = (1.0 + escalatorRate).pow(y - 1)
            val annualGross = (year1AnnualPayoutMillions * yearMultiplier).roundToLong().coerceAtLeast(year1AnnualPayoutMillions)
            val treasuryCut = (annualGross * 0.15).roundToLong().coerceAtLeast(1L)
            val playerCut = (annualGross * clampedPlayerShare).roundToLong()
            val ownerPool = (annualGross - playerCut).coerceAtLeast(1L)
            cumulativeGross += annualGross
            totalAnnualSum += annualGross
            totalTreasuryFromAnnual += treasuryCut

            val baselineAnnual = if (y <= currentContract.yearsRemaining) currentContract.annualPayoutMillions else (currentContract.annualPayoutMillions * 0.92).roundToLong()
            baselineCumulative += baselineAnnual
            val netAdvantage = cumulativeGross.roundToLong() - baselineCumulative

            projections.add(
                BroadcastYearlyProjection(
                    yearOffset = y,
                    seasonYear = currentYear + (y - 1),
                    grossPayoutMillions = annualGross,
                    leagueTreasuryCutMillions = treasuryCut,
                    playerShareCostMillions = playerCut,
                    ownerPoolMillions = ownerPool,
                    cumulativeGrossMillions = cumulativeGross.roundToLong(),
                    baselineCurrentAnnualMillions = baselineAnnual,
                    netAdvantageVsCurrentMillions = netAdvantage
                )
            )
        }

        val cumulativeTotal = lumpSumBonusMillions + totalAnnualSum
        val avgAnnual = totalAnnualSum.toDouble() / safeTerms
        val avgOwnerPool = avgAnnual * (1.0 - clampedPlayerShare)
        val perTeamAvg = Math.round((avgOwnerPool / safeTeams) * 10.0) / 10.0
        val totalTreasuryInflow = lumpSumBonusMillions + totalTreasuryFromAnnual
        val valuationLift = ((year1AnnualPayoutMillions / 2L) + (lumpSumBonusMillions * 2L) + ((safeTerms - 2).coerceAtLeast(0) * 6L)).coerceAtLeast(10L)
        val reachGrowth = Math.round((currentFanbaseMillions * ((reachMultiplier - 1.0) * 0.14).coerceAtLeast(0.02)) * 10.0) / 10.0
        val netVsCurrent = cumulativeTotal - baselineCumulative

        val finalYearPayout = projections.lastOrNull()?.grossPayoutMillions ?: year1AnnualPayoutMillions
        val verdict = when {
            reachMultiplier >= 2.0 && netVsCurrent >= 0L -> "Grassroots Audience Surge"
            lumpSumBonusMillions >= 38L -> "High-Liquidity Upfront Windfall"
            safeTerms >= 5 && annualEscalatorPct >= 4.5 -> "Franchise-Anchoring Long-Term Deal"
            scope == NetworkScope.GLOBAL_STREAMING -> "Global Digital Growth Catalyst"
            else -> "Balanced Media Stability Pact"
        }

        return BroadcastLongTermImpact(
            termYears = safeTerms,
            upfrontBonusMillions = lumpSumBonusMillions,
            year1AnnualPayoutMillions = year1AnnualPayoutMillions,
            finalYearAnnualPayoutMillions = finalYearPayout,
            annualEscalatorPct = annualEscalatorPct,
            cumulativeGrossValueMillions = cumulativeTotal,
            totalLeagueTreasuryInflowMillions = totalTreasuryInflow,
            averagePerTeamAnnualShareMillions = perTeamAvg,
            projectedValuationLiftMillions = valuationLift,
            projectedFanReachGrowthMillions = reachGrowth,
            netGainVsCurrentContractMillions = netVsCurrent,
            yearlyProjections = projections,
            strategicVerdict = verdict
        )
    }

    private fun markLeadingOffer(offers: List<BroadcastContractOffer>): List<BroadcastContractOffer> {
        val activeLeaderId = offers
            .filter { it.bidStatus != BroadcastBidStatus.WITHDRAWN }
            .maxByOrNull { it.impact.cumulativeGrossValueMillions }
            ?.id

        return offers.map { offer ->
            when {
                offer.bidStatus == BroadcastBidStatus.WITHDRAWN -> offer
                offer.id == activeLeaderId -> offer.copy(bidStatus = BroadcastBidStatus.LEADING_BID)
                offer.bidStatus == BroadcastBidStatus.COUNTER_PENDING -> offer
                else -> offer.copy(bidStatus = BroadcastBidStatus.ACTIVE_OFFER)
            }
        }
    }

    fun createInitialSession(
        bidders: List<NetworkBidder>,
        currentContract: BroadcastContract,
        teamCount: Int,
        playerRevenueSharePct: Int,
        currentFanbaseMillions: Double,
        currentYear: Int
    ): BroadcastBiddingSession {
        val rawOffers = bidders.map { bidder ->
            val escalator = defaultEscalatorForScope(bidder.scope)
            val playoffBonus = defaultPlayoffBonusForScope(bidder.scope)
            val impact = evaluateLongTermImpact(
                year1AnnualPayoutMillions = bidder.baseAnnualPayoutMillions,
                lumpSumBonusMillions = bidder.baseLumpSumMillions,
                termYears = bidder.preferredTermYears,
                annualEscalatorPct = escalator,
                playoffBonusPerGameMillions = playoffBonus,
                reachMultiplier = bidder.reachMultiplier,
                scope = bidder.scope,
                currentContract = currentContract,
                teamCount = teamCount,
                playerRevenueSharePct = playerRevenueSharePct,
                currentFanbaseMillions = currentFanbaseMillions,
                currentYear = currentYear
            )
            BroadcastContractOffer(
                id = "offer_${bidder.id}",
                bidder = bidder,
                annualPayoutMillions = bidder.baseAnnualPayoutMillions,
                lumpSumBonusMillions = bidder.baseLumpSumMillions,
                termYears = bidder.preferredTermYears,
                annualEscalatorPct = escalator,
                playoffBonusPerGameMillions = playoffBonus,
                currentPatience = bidder.patienceRating,
                latestExecutiveNote = bidder.executiveQuote,
                bidStatus = BroadcastBidStatus.ACTIVE_OFFER,
                maxAnnualCeilingMillions = (bidder.baseAnnualPayoutMillions * 1.35).roundToLong(),
                maxBonusCeilingMillions = (bidder.baseLumpSumMillions + 26L).coerceAtMost(75L),
                impact = impact,
                isExclusivityExpired = false
            )
        }

        val ranked = markLeadingOffer(rawOffers)
        val leader = ranked.find { it.bidStatus == BroadcastBidStatus.LEADING_BID } ?: ranked.firstOrNull()
        val openingLog = if (leader != null) {
            "Round 1 Bidding Opened: ${ranked.size} networks submitted rights proposals (${leader.bidder.name} leads at \$${leader.impact.cumulativeGrossValueMillions}M total). Exclusivity window: 3 cycles."
        } else {
            "Broadcast Bidding Desk Ready."
        }

        return BroadcastBiddingSession(
            biddingRound = 1,
            offers = ranked,
            selectedOfferId = leader?.id ?: ranked.firstOrNull()?.id,
            auctionLog = listOf(openingLog),
            lastEscalationSummary = null,
            exclusivityRoundsTotal = 3,
            exclusivityRoundsRemaining = 3,
            exclusivityExpired = false,
            nonExclusiveRevenuePenaltyPct = EXPIRY_REVENUE_PENALTY_PCT,
            nonExclusiveReachPenaltyPct = EXPIRY_REACH_PENALTY_PCT,
            lastPeriodicBidWeek = 0,
            periodicBidsReceivedCount = 0
        )
    }

    fun applyExclusivityExpiryPenalty(
        session: BroadcastBiddingSession,
        currentContract: BroadcastContract,
        teamCount: Int,
        playerRevenueSharePct: Int,
        currentFanbaseMillions: Double,
        currentYear: Int
    ): BroadcastBiddingSession {
        if (session.exclusivityExpired) return session
        val penalizedOffers = session.offers.map { offer ->
            if (offer.bidStatus == BroadcastBidStatus.WITHDRAWN) {
                offer.copy(isExclusivityExpired = true)
            } else {
                val cutAnnual = (offer.annualPayoutMillions * (1.0 - EXPIRY_REVENUE_PENALTY_PCT / 100.0))
                    .roundToLong()
                    .coerceAtLeast(32L)
                val cutBonus = (offer.lumpSumBonusMillions * (1.0 - EXPIRY_BONUS_PENALTY_PCT / 100.0))
                    .roundToLong()
                    .coerceAtLeast(5L)
                val reducedReach = (offer.bidder.reachMultiplier * (1.0 - EXPIRY_REACH_PENALTY_PCT / 100.0))
                    .coerceAtLeast(0.85)
                val newImpact = evaluateLongTermImpact(
                    year1AnnualPayoutMillions = cutAnnual,
                    lumpSumBonusMillions = cutBonus,
                    termYears = offer.termYears,
                    annualEscalatorPct = offer.annualEscalatorPct,
                    playoffBonusPerGameMillions = (offer.playoffBonusPerGameMillions - 1L).coerceAtLeast(1L),
                    reachMultiplier = reducedReach,
                    scope = offer.bidder.scope,
                    currentContract = currentContract,
                    teamCount = teamCount,
                    playerRevenueSharePct = playerRevenueSharePct,
                    currentFanbaseMillions = currentFanbaseMillions,
                    currentYear = currentYear
                )
                offer.copy(
                    annualPayoutMillions = cutAnnual,
                    lumpSumBonusMillions = cutBonus,
                    digitalExclusivityIncluded = false,
                    currentPatience = (offer.currentPatience - 14).coerceAtLeast(12),
                    latestExecutiveNote = "Exclusivity window lapsed! Re-priced as non-exclusive package (-${EXPIRY_REVENUE_PENALTY_PCT}% annual, -${EXPIRY_REACH_PENALTY_PCT}% fan reach).",
                    impact = newImpact,
                    isExclusivityExpired = true
                )
            }
        }
        val ranked = markLeadingOffer(penalizedOffers)
        val expiryNote = "EXCLUSIVITY EXPIRED: Waiting past the broadcast exclusivity window reduced all network bids by ${EXPIRY_REVENUE_PENALTY_PCT}% payout & ${EXPIRY_REACH_PENALTY_PCT}% fan reach."
        return session.copy(
            offers = ranked,
            exclusivityRoundsRemaining = 0,
            exclusivityExpired = true,
            auctionLog = (listOf(expiryNote) + session.auctionLog).take(20),
            lastEscalationSummary = expiryNote
        )
    }

    fun receivePeriodicRivalBid(
        session: BroadcastBiddingSession,
        currentContract: BroadcastContract,
        teamCount: Int,
        playerRevenueSharePct: Int,
        currentFanbaseMillions: Double,
        currentYear: Int,
        weekNumber: Int
    ): PeriodicBroadcastTickResult {
        if (session.offers.isEmpty()) {
            return PeriodicBroadcastTickResult(updatedSession = session)
        }
        val activeNonLeading = session.offers.filter {
            it.bidStatus != BroadcastBidStatus.WITHDRAWN && it.bidStatus != BroadcastBidStatus.LEADING_BID
        }
        val targetPool = if (activeNonLeading.isNotEmpty()) activeNonLeading else session.offers.filter {
            it.bidStatus != BroadcastBidStatus.WITHDRAWN
        }
        if (targetPool.isEmpty()) {
            return PeriodicBroadcastTickResult(updatedSession = session)
        }

        val chosen = targetPool[(weekNumber + session.periodicBidsReceivedCount) % targetPool.size]
        val nextRoundsRemaining = (session.exclusivityRoundsRemaining - 1).coerceAtLeast(0)
        val annualBoost = if (!session.exclusivityExpired) 6L + ((weekNumber % 3).toLong()) else 3L
        val bonusBoost = if (!session.exclusivityExpired) 4L else 1L
        val raisedAnnual = (chosen.annualPayoutMillions + annualBoost).coerceAtMost(chosen.maxAnnualCeilingMillions + 10L)
        val raisedBonus = (chosen.lumpSumBonusMillions + bonusBoost).coerceAtMost(chosen.maxBonusCeilingMillions + 6L)

        val updatedOffers = session.offers.map { offer ->
            if (offer.id != chosen.id) offer else {
                val newImpact = evaluateLongTermImpact(
                    year1AnnualPayoutMillions = raisedAnnual,
                    lumpSumBonusMillions = raisedBonus,
                    termYears = offer.termYears,
                    annualEscalatorPct = offer.annualEscalatorPct,
                    playoffBonusPerGameMillions = offer.playoffBonusPerGameMillions,
                    reachMultiplier = effectiveReachForOffer(offer),
                    scope = offer.bidder.scope,
                    currentContract = currentContract,
                    teamCount = teamCount,
                    playerRevenueSharePct = playerRevenueSharePct,
                    currentFanbaseMillions = currentFanbaseMillions,
                    currentYear = currentYear
                )
                offer.copy(
                    annualPayoutMillions = raisedAnnual,
                    lumpSumBonusMillions = raisedBonus,
                    maxAnnualCeilingMillions = maxOf(offer.maxAnnualCeilingMillions, raisedAnnual),
                    maxBonusCeilingMillions = maxOf(offer.maxBonusCeilingMillions, raisedBonus),
                    latestExecutiveNote = "Periodic Rival Bid (W$weekNumber): ${offer.bidder.name} submitted an unsolicited \$${raisedAnnual}M/yr + \$${raisedBonus}M bonus counter-bid!",
                    impact = newImpact
                )
            }
        }

        val ranked = markLeadingOffer(updatedOffers)
        val expiryWarning = if (!session.exclusivityExpired && nextRoundsRemaining > 0) {
            " ($nextRoundsRemaining cycle${if (nextRoundsRemaining == 1) "" else "s"} left before exclusivity expires)"
        } else ""
        val bidLog = "Week $weekNumber Rival Bid: ${chosen.bidder.name} escalated to \$${raisedAnnual}M/yr + \$${raisedBonus}M bonus.$expiryWarning"

        var nextSession = session.copy(
            offers = ranked,
            selectedOfferId = chosen.id,
            exclusivityRoundsRemaining = nextRoundsRemaining,
            lastPeriodicBidWeek = weekNumber,
            periodicBidsReceivedCount = session.periodicBidsReceivedCount + 1,
            auctionLog = (listOf(bidLog) + session.auctionLog).take(20),
            lastEscalationSummary = bidLog
        )

        var justExpired = false
        if (!nextSession.exclusivityExpired && nextRoundsRemaining <= 0) {
            nextSession = applyExclusivityExpiryPenalty(
                session = nextSession,
                currentContract = currentContract,
                teamCount = teamCount,
                playerRevenueSharePct = playerRevenueSharePct,
                currentFanbaseMillions = currentFanbaseMillions,
                currentYear = currentYear
            )
            justExpired = true
        }

        val headline = if (justExpired) {
            "BROADCAST EXCLUSIVITY EXPIRED (W$weekNumber): Holding out triggered a -${EXPIRY_REVENUE_PENALTY_PCT}% non-exclusive market haircut!"
        } else {
            "RIVAL TV BID (W$weekNumber): ${chosen.bidder.name} offers \$${raisedAnnual}M/yr + \$${raisedBonus}M upfront!"
        }

        return PeriodicBroadcastTickResult(
            updatedSession = nextSession,
            bidderName = chosen.bidder.name,
            headline = headline,
            exclusivityJustExpired = justExpired
        )
    }

    fun refreshSessionImpacts(
        session: BroadcastBiddingSession,
        currentContract: BroadcastContract,
        teamCount: Int,
        playerRevenueSharePct: Int,
        currentFanbaseMillions: Double,
        currentYear: Int
    ): BroadcastBiddingSession {
        val updated = session.offers.map { offer ->
            val newImpact = evaluateLongTermImpact(
                year1AnnualPayoutMillions = offer.annualPayoutMillions,
                lumpSumBonusMillions = offer.lumpSumBonusMillions,
                termYears = offer.termYears,
                annualEscalatorPct = offer.annualEscalatorPct,
                playoffBonusPerGameMillions = offer.playoffBonusPerGameMillions,
                reachMultiplier = effectiveReachForOffer(offer),
                scope = offer.bidder.scope,
                currentContract = currentContract,
                teamCount = teamCount,
                playerRevenueSharePct = playerRevenueSharePct,
                currentFanbaseMillions = currentFanbaseMillions,
                currentYear = currentYear
            )
            offer.copy(impact = newImpact)
        }
        return session.copy(offers = markLeadingOffer(updated))
    }

    fun simulateCompetitiveBiddingRound(
        session: BroadcastBiddingSession,
        currentContract: BroadcastContract,
        teamCount: Int,
        playerRevenueSharePct: Int,
        currentFanbaseMillions: Double,
        currentYear: Int
    ): BroadcastBiddingSession {
        val nextRound = session.biddingRound + 1
        val activeBefore = session.offers.filter { it.bidStatus != BroadcastBidStatus.WITHDRAWN }
        if (activeBefore.size <= 1) {
            return session.copy(
                lastEscalationSummary = "Only one active network bidder remains at the table — ready to finalize contract."
            )
        }

        val currentTopValue = activeBefore.maxOfOrNull { it.impact.cumulativeGrossValueMillions } ?: 0L
        var raisedCount = 0
        val withdrawnNames = mutableListOf<String>()

        val steppedOffers = session.offers.mapIndexed { idx, offer ->
            if (offer.bidStatus == BroadcastBidStatus.WITHDRAWN) {
                offer
            } else {
                val isTrailing = offer.impact.cumulativeGrossValueMillions < currentTopValue
                val hasRoom = offer.annualPayoutMillions < offer.maxAnnualCeilingMillions ||
                    offer.lumpSumBonusMillions < offer.maxBonusCeilingMillions
                val shouldWithdraw = !hasRoom && (nextRound >= 4 || offer.currentPatience <= 20) && activeBefore.size - withdrawnNames.size > 2

                if (shouldWithdraw) {
                    withdrawnNames.add(offer.bidder.name)
                    offer.copy(
                        bidStatus = BroadcastBidStatus.WITHDRAWN,
                        currentPatience = 0,
                        latestExecutiveNote = "We've reached our network board's capital ceiling and are withdrawing from this round."
                    )
                } else if (hasRoom) {
                    val annualBump = when {
                        isTrailing && offer.bidder.scope == NetworkScope.GLOBAL_STREAMING -> 7L
                        isTrailing -> 5L
                        idx % 2 == 0 -> 4L
                        else -> 3L
                    }
                    val bonusBump = if (isTrailing) 4L else 2L
                    val newAnnual = (offer.annualPayoutMillions + annualBump).coerceAtMost(offer.maxAnnualCeilingMillions)
                    val newBonus = (offer.lumpSumBonusMillions + bonusBump).coerceAtMost(offer.maxBonusCeilingMillions)
                    val newEscalator = Math.round(((offer.annualEscalatorPct + 0.5).coerceAtMost(9.5)) * 10.0) / 10.0
                    val newPatience = (offer.currentPatience - if (isTrailing) 6 else 4).coerceAtLeast(15)
                    raisedCount++

                    val newImpact = evaluateLongTermImpact(
                        year1AnnualPayoutMillions = newAnnual,
                        lumpSumBonusMillions = newBonus,
                        termYears = offer.termYears,
                        annualEscalatorPct = newEscalator,
                        playoffBonusPerGameMillions = offer.playoffBonusPerGameMillions,
                        reachMultiplier = effectiveReachForOffer(offer),
                        scope = offer.bidder.scope,
                        currentContract = currentContract,
                        teamCount = teamCount,
                        playerRevenueSharePct = playerRevenueSharePct,
                        currentFanbaseMillions = currentFanbaseMillions,
                        currentYear = currentYear
                    )
                    offer.copy(
                        annualPayoutMillions = newAnnual,
                        lumpSumBonusMillions = newBonus,
                        annualEscalatorPct = newEscalator,
                        currentPatience = newPatience,
                        bidStatus = BroadcastBidStatus.ACTIVE_OFFER,
                        latestExecutiveNote = "Round $nextRound Escalation: Raised offer to \$${newAnnual}M/yr + \$${newBonus}M signing bonus (+${newEscalator}%/yr escalator).",
                        impact = newImpact
                    )
                } else {
                    offer.copy(
                        currentPatience = (offer.currentPatience - 5).coerceAtLeast(10),
                        latestExecutiveNote = "Holding firm at \$${offer.annualPayoutMillions}M/yr — maximum executive authorization reached."
                    )
                }
            }
        }

        val ranked = markLeadingOffer(steppedOffers)
        val leader = ranked.find { it.bidStatus == BroadcastBidStatus.LEADING_BID } ?: ranked.firstOrNull()
        val nextExclusivityRemaining = (session.exclusivityRoundsRemaining - 1).coerceAtLeast(0)
        val summary = buildString {
            append("Round $nextRound Complete: $raisedCount networks raised bids")
            if (withdrawnNames.isNotEmpty()) {
                append(", ${withdrawnNames.size} withdrew (${withdrawnNames.first()})")
            }
            if (leader != null) {
                append(". ${leader.bidder.name} leads at \$${leader.impact.cumulativeGrossValueMillions}M total.")
            }
        }

        val preExpirySession = session.copy(
            biddingRound = nextRound,
            offers = ranked,
            selectedOfferId = session.selectedOfferId ?: leader?.id,
            auctionLog = (listOf(summary) + session.auctionLog).take(20),
            lastEscalationSummary = summary,
            exclusivityRoundsRemaining = nextExclusivityRemaining
        )

        return if (!preExpirySession.exclusivityExpired && nextExclusivityRemaining <= 0) {
            applyExclusivityExpiryPenalty(
                session = preExpirySession,
                currentContract = currentContract,
                teamCount = teamCount,
                playerRevenueSharePct = playerRevenueSharePct,
                currentFanbaseMillions = currentFanbaseMillions,
                currentYear = currentYear
            )
        } else {
            preExpirySession
        }
    }

    fun updateOfferTerms(
        session: BroadcastBiddingSession,
        offerId: String,
        termYears: Int,
        lumpSumBonusMillions: Long,
        annualPayoutMillions: Long,
        currentPatience: Int,
        executiveNote: String,
        digitalExclusivity: Boolean? = null,
        playoffPrimetime: Boolean? = null,
        markCountered: Boolean = false,
        currentContract: BroadcastContract,
        teamCount: Int,
        playerRevenueSharePct: Int,
        currentFanbaseMillions: Double,
        currentYear: Int
    ): BroadcastBiddingSession {
        val updated = session.offers.map { offer ->
            if (offer.id == offerId && offer.bidStatus != BroadcastBidStatus.WITHDRAWN) {
                val safeTerm = termYears.coerceIn(1, 7)
                val safeAnnual = annualPayoutMillions.coerceAtLeast(35L)
                val safeBonus = lumpSumBonusMillions.coerceIn(5L, 90L)
                val safePatience = currentPatience.coerceIn(0, 100)
                val newImpact = evaluateLongTermImpact(
                    year1AnnualPayoutMillions = safeAnnual,
                    lumpSumBonusMillions = safeBonus,
                    termYears = safeTerm,
                    annualEscalatorPct = offer.annualEscalatorPct,
                    playoffBonusPerGameMillions = offer.playoffBonusPerGameMillions,
                    reachMultiplier = effectiveReachForOffer(offer),
                    scope = offer.bidder.scope,
                    currentContract = currentContract,
                    teamCount = teamCount,
                    playerRevenueSharePct = playerRevenueSharePct,
                    currentFanbaseMillions = currentFanbaseMillions,
                    currentYear = currentYear
                )
                offer.copy(
                    termYears = safeTerm,
                    lumpSumBonusMillions = safeBonus,
                    annualPayoutMillions = safeAnnual,
                    currentPatience = safePatience,
                    latestExecutiveNote = executiveNote,
                    digitalExclusivityIncluded = digitalExclusivity ?: offer.digitalExclusivityIncluded,
                    playoffPrimetimeIncluded = playoffPrimetime ?: offer.playoffPrimetimeIncluded,
                    bidStatus = if (safePatience <= 0) BroadcastBidStatus.WITHDRAWN else if (markCountered) BroadcastBidStatus.COUNTER_PENDING else offer.bidStatus,
                    impact = newImpact
                )
            } else offer
        }

        val ranked = markLeadingOffer(updated)
        return session.copy(offers = ranked)
    }
}
