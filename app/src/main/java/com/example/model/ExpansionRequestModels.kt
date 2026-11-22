package com.example.model

import kotlin.math.roundToLong

enum class ExpansionApplicationStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    DECLINED("Declined")
}

data class ExpansionRequestImpact(
    val currentLeagueMarketReachMillions: Double,
    val projectedMarketReachGainMillions: Double,
    val projectedMarketReachGainPct: Double,
    val projectedPostApprovalMarketReachMillions: Double,
    val currentAnnualLeagueRevenueMillions: Long,
    val projectedAnnualBroadcastUpliftMillions: Long,
    val projectedAnnualLocalSponsorRevenueMillions: Long,
    val projectedAnnualGateRevenueMillions: Long,
    val projectedAnnualRevenueGainMillions: Long,
    val projectedAnnualRevenueGainPct: Double,
    val projectedPostApprovalAnnualRevenueMillions: Long,
    val immediateExpansionFeeMillions: Long,
    val projectedValuationUpliftMillions: Long,
    val perTeamBroadcastDilutionNote: String
)

data class ExpansionRequest(
    val id: String,
    val city: MarketCity,
    val proposedNickname: String,
    val brandColorHex: Long,
    val secondaryColorHex: Long = FranchiseIdentityEngine.defaultSecondaryFor(brandColorHex),
    val ownershipType: OwnershipType,
    val ownerArchetype: OwnerArchetype,
    val ownershipGroupName: String,
    val ownerFullName: String,
    val bidSummary: String,
    val arenaPlanSummary: String,
    val requestedYear: Int,
    val requestedWeek: Int,
    val status: ExpansionApplicationStatus = ExpansionApplicationStatus.PENDING,
    val decisionNotes: String = "",
    val impact: ExpansionRequestImpact
)

object ExpansionRequestEngine {

    private val defaultNicknamesByCityId = mapOf(
        "city_london" to "Monarchs",
        "city_tokyo" to "Shoguns",
        "city_mexico" to "Aztecas",
        "city_berlin" to "Eagles",
        "city_toronto" to "Northmen",
        "city_vancouver" to "Cascadia",
        "city_atlanta" to "Phoenix",
        "city_nashville" to "Sounds",
        "city_paris" to "Lumiere",
        "city_prospect_boston" to "Minutemen",
        "city_prospect_sf" to "Founders",
        "city_prospect_miami" to "Flamingos",
        "city_prospect_phoenix" to "Suns",
        "city_prospect_sd" to "Breakers",
        "city_prospect_philly" to "Liberty",
        "city_prospect_charlotte" to "Crowns",
        "city_prospect_slc" to "Peaks",
        "city_prospect_portland" to "Pioneers",
        "city_prospect_sa" to "Guardians",
        "city_prospect_kc" to "Royals"
    )

    private val archetypeRotation = listOf(
        OwnerArchetype.GLOBAL_EXPANSIONIST,
        OwnerArchetype.BROADCAST_SHOWMAN,
        OwnerArchetype.PLAYERS_FIRST_PATRON,
        OwnerArchetype.ANALYTICS_VISIONARY,
        OwnerArchetype.COMMUNITY_STEWARD
    )

    private val ownershipRotation = listOf(
        OwnershipType.SOVEREIGN_SYNDICATE,
        OwnershipType.TECH_BILLIONAIRE,
        OwnershipType.MEDIA_MOGUL,
        OwnershipType.PRIVATE_EQUITY,
        OwnershipType.COMMUNITY_TRUST
    )

    fun computeMarketReachGainMillions(city: MarketCity): Double {
        val raw = (city.populationMillions * 0.35) +
            (city.sportsAppetite / 250.0) +
            if (city.isInternational) 0.5 else 0.1
        return (Math.round(raw.coerceAtLeast(0.5) * 10.0) / 10.0)
    }

    fun computeLocalSponsorRevenueMillions(city: MarketCity): Long {
        return (city.populationMillions * city.tier.mediaMultiplier * 3.8)
            .roundToLong()
            .coerceAtLeast(city.localSponsorDepthMillions.coerceAtLeast(18L))
    }

    fun computeAnnualBroadcastUpliftMillions(city: MarketCity): Long {
        val intlBonus = if (city.isInternational) 12.0 else 5.0
        val digitalFactor = city.digitalStreamingIndex * 0.08
        val marketFeeFactor = city.expansionFeeMillions * 0.12
        return (intlBonus + digitalFactor + marketFeeFactor).roundToLong().coerceAtLeast(8L)
    }

    fun computeAnnualGateRevenueMillions(city: MarketCity): Long {
        val estimatedTicketPrice = 60.0 + (city.medianIncomeK * 0.25)
        val attendanceFactor = (city.sportsAppetite / 100.0).coerceIn(0.65, 0.99)
        val rawGate = (city.arenaCapacity * attendanceFactor * estimatedTicketPrice * 8.0) / 1_000_000.0
        return rawGate.roundToLong().coerceAtLeast(6L)
    }

    fun estimateCurrentAnnualLeagueRevenueMillions(
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship
    ): Long {
        val broadcastAnnual = activeBroadcast.annualPayoutMillions
        val sponsorAnnual = franchises.sumOf {
            it.localSponsorRevenueMillions.toDouble() + it.facilities.annualSuitePremiumMillions
        }.roundToLong()
        val gateAnnual = franchises.sumOf { team ->
            val occ = (team.fanLoyalty / 100.0).coerceIn(0.55, 0.98)
            ((team.stadiumCapacity * occ * team.ticketPrice * 8.0) / 1_000_000.0)
                .roundToLong()
                .coerceAtLeast(4L)
        }
        val leagueSponsorAnnual = activeSponsorship.annualPayoutMillions
        return (broadcastAnnual + sponsorAnnual + gateAnnual + leagueSponsorAnnual).coerceAtLeast(20L)
    }

    fun evaluateImpact(
        city: MarketCity,
        currentFranchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        currentMarketReachMillions: Double
    ): ExpansionRequestImpact {
        val safeReach = currentMarketReachMillions.coerceAtLeast(1.0)
        val reachGain = computeMarketReachGainMillions(city)
        val reachGainPct = Math.round(((reachGain / safeReach) * 100.0) * 10.0) / 10.0
        val postReach = Math.round((safeReach + reachGain) * 10.0) / 10.0

        val currentAnnualRev = estimateCurrentAnnualLeagueRevenueMillions(
            franchises = currentFranchises,
            activeBroadcast = activeBroadcast,
            activeSponsorship = activeSponsorship
        )
        val broadcastUplift = computeAnnualBroadcastUpliftMillions(city)
        val localSponsor = computeLocalSponsorRevenueMillions(city)
        val annualGate = computeAnnualGateRevenueMillions(city)
        val totalRevGain = broadcastUplift + localSponsor + annualGate
        val revGainPct = Math.round(((totalRevGain.toDouble() / currentAnnualRev.toDouble()) * 100.0) * 10.0) / 10.0
        val postAnnualRev = currentAnnualRev + totalRevGain

        val teamCount = currentFranchises.size.coerceAtLeast(1)
        val beforeBroadcastShare = Math.round((activeBroadcast.annualPayoutMillions.toDouble() / teamCount) * 10.0) / 10.0
        val afterBroadcastShare = Math.round(((activeBroadcast.annualPayoutMillions + broadcastUplift).toDouble() / (teamCount + 1)) * 10.0) / 10.0
        val shareDelta = Math.round((afterBroadcastShare - beforeBroadcastShare) * 10.0) / 10.0
        val dilutionNote = if (shareDelta >= 0.0) {
            "Per-team national TV share improves +\$${shareDelta}M/yr (${teamCount + 1}-way split offset by rights escalator)."
        } else {
            "Per-team national TV split shifts ${shareDelta}M/yr initially, offset by +\$${city.expansionFeeMillions}M fee payout."
        }

        return ExpansionRequestImpact(
            currentLeagueMarketReachMillions = Math.round(safeReach * 10.0) / 10.0,
            projectedMarketReachGainMillions = reachGain,
            projectedMarketReachGainPct = reachGainPct,
            projectedPostApprovalMarketReachMillions = postReach,
            currentAnnualLeagueRevenueMillions = currentAnnualRev,
            projectedAnnualBroadcastUpliftMillions = broadcastUplift,
            projectedAnnualLocalSponsorRevenueMillions = localSponsor,
            projectedAnnualGateRevenueMillions = annualGate,
            projectedAnnualRevenueGainMillions = totalRevGain,
            projectedAnnualRevenueGainPct = revGainPct,
            projectedPostApprovalAnnualRevenueMillions = postAnnualRev,
            immediateExpansionFeeMillions = city.expansionFeeMillions,
            projectedValuationUpliftMillions = city.expansionFeeMillions * 2L,
            perTeamBroadcastDilutionNote = dilutionNote
        )
    }

    fun buildRequestForCity(
        city: MarketCity,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        currentMarketReachMillions: Double,
        seasonYear: Int,
        week: Int,
        indexSeed: Int = 0
    ): ExpansionRequest {
        val hashIdx = kotlin.math.abs(city.id.hashCode()) + indexSeed
        val nickname = defaultNicknamesByCityId[city.id]
            ?: listOf("Stallions", "Monarchs", "Titans", "Voyagers", "Guardians", "Legends")[hashIdx % 6]
        val ownershipType = if (city.isInternational) {
            OwnershipType.SOVEREIGN_SYNDICATE
        } else {
            ownershipRotation[hashIdx % ownershipRotation.size]
        }
        val archetype = if (city.isInternational) {
            OwnerArchetype.GLOBAL_EXPANSIONIST
        } else {
            archetypeRotation[hashIdx % archetypeRotation.size]
        }
        val palette = FranchiseIdentityEngine.brandColorPalette
        val colorHex = palette[hashIdx % palette.size].hex
        val secondaryColorHex = palette[(hashIdx + 7) % palette.size].hex
            .let { if (it == colorHex) FranchiseIdentityEngine.defaultSecondaryFor(colorHex) else it }
        val previewOwnership = OwnerGovernanceEngine.buildExpansionOwnershipGroup(
            city = city,
            ownershipType = ownershipType,
            existingFranchises = franchises,
            preferredArchetype = archetype,
            customOwnerName = null
        )

        val impact = evaluateImpact(
            city = city,
            currentFranchises = franchises,
            activeBroadcast = activeBroadcast,
            activeSponsorship = activeSponsorship,
            currentMarketReachMillions = currentMarketReachMillions
        )

        val gatewayTag = if (city.isInternational) "international flagship" else "${city.region} high-growth"
        val bidSummary = "${city.investorGroupProfile} proposes an expansion charter for the ${city.name} $nickname " +
            "with \$${city.expansionFeeMillions}M capital commitment into a $gatewayTag territory (${city.populationMillions}M metro)."
        val arenaPlanSummary = "${city.arenaName} (${city.arenaCapacity} seats) — ${city.arenaReadinessStatus}."

        return ExpansionRequest(
            id = "req_${city.id}_y${seasonYear}_w$week",
            city = city,
            proposedNickname = nickname,
            brandColorHex = colorHex,
            secondaryColorHex = secondaryColorHex,
            ownershipType = ownershipType,
            ownerArchetype = previewOwnership.primaryOwner.archetype,
            ownershipGroupName = previewOwnership.name,
            ownerFullName = previewOwnership.primaryOwner.fullName,
            bidSummary = bidSummary,
            arenaPlanSummary = arenaPlanSummary,
            requestedYear = seasonYear,
            requestedWeek = week,
            status = ExpansionApplicationStatus.PENDING,
            impact = impact
        )
    }

    fun seedInitialRequests(
        cities: List<MarketCity>,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        currentMarketReachMillions: Double = 18.2,
        seasonYear: Int = 2026,
        week: Int = 1
    ): List<ExpansionRequest> {
        val candidates = cities
            .filter { !it.hasFranchise }
            .sortedByDescending { it.overallViabilityScore }
            .take(4)

        return candidates.mapIndexed { idx, city ->
            buildRequestForCity(
                city = city,
                franchises = franchises,
                activeBroadcast = activeBroadcast,
                activeSponsorship = activeSponsorship,
                currentMarketReachMillions = currentMarketReachMillions,
                seasonYear = seasonYear,
                week = week,
                indexSeed = idx
            )
        }
    }

    fun recalculateImpacts(
        requests: List<ExpansionRequest>,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        currentMarketReachMillions: Double
    ): List<ExpansionRequest> {
        return requests.map { req ->
            req.copy(
                impact = evaluateImpact(
                    city = req.city,
                    currentFranchises = franchises,
                    activeBroadcast = activeBroadcast,
                    activeSponsorship = activeSponsorship,
                    currentMarketReachMillions = currentMarketReachMillions
                )
            )
        }
    }
}
