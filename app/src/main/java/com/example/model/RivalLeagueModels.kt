package com.example.model

import com.example.data.InitialLeagueData
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class RivalWatchAction(
    val title: String,
    val badgeTag: String,
    val description: String,
    val cashCostMillions: Long,
    val ownerApprovalDelta: Int,
    val unionSatisfactionDelta: Int,
    val strikeRiskDelta: Int,
    val capitalDrainMillions: Long,
    val hypeDrain: Int,
    val fanbaseDeltaMillions: Double
) {
    COUNTER_PROGRAM(
        title = "Counter-Program",
        badgeTag = "MEDIA BLITZ",
        description = "Launch competing prime-time showcases & bonus pools to starve rival TV windows.",
        cashCostMillions = 12L,
        ownerApprovalDelta = 4,
        unionSatisfactionDelta = 2,
        strikeRiskDelta = -2,
        capitalDrainMillions = 35L,
        hypeDrain = 16,
        fanbaseDeltaMillions = 0.2
    ),
    BLACKLIST_DEFECTORS(
        title = "Blacklist Defectors",
        badgeTag = "HARDLINE BAN",
        description = "Bar breakaway signees from league honors & facilities; thrills hawks, angers the Union.",
        cashCostMillions = 0L,
        ownerApprovalDelta = 7,
        unionSatisfactionDelta = -10,
        strikeRiskDelta = 9,
        capitalDrainMillions = 22L,
        hypeDrain = 18,
        fanbaseDeltaMillions = -0.1
    ),
    PROPOSE_MERGER(
        title = "Propose Merger",
        badgeTag = "BUYOUT PACT",
        description = "Offer a buyout & collective roster re-entry; wins Union peace at Owners' expense.",
        cashCostMillions = 20L,
        ownerApprovalDelta = -6,
        unionSatisfactionDelta = 9,
        strikeRiskDelta = -8,
        capitalDrainMillions = 60L,
        hypeDrain = 24,
        fanbaseDeltaMillions = 0.4
    )
}

data class RivalFranchiseCandidate(
    val id: String,
    val cityId: String,
    val cityName: String,
    val nickname: String,
    val colorHex: Long,
    val secondaryColorHex: Long,
    val rosterRating: Int,
    val fanBaseMillions: Double,
    val marketPopulationMillions: Double,
    val stadiumCapacity: Int,
    val arenaName: String,
    val localSponsorRevenueMillions: Long,
    val annualBroadcastContributionMillions: Long,
    val signatureStarName: String,
    val isSelected: Boolean = true
) {
    val teamFullName: String
        get() = "$cityName $nickname"
}

data class RivalAbsorptionNegotiationEvent(
    val id: String,
    val rivalLeagueName: String,
    val sportType: SportType,
    val seasonYear: Int,
    val seasonsSurvived: Int,
    val rivalHypeSnapshot: Int,
    val rivalCapitalSnapshot: Long,
    val candidateTeams: List<RivalFranchiseCandidate>,
    val baseTvBoostMillions: Long = 26L,
    val ownerApprovalHitPerTeam: Int = 5,
    val headline: String,
    val description: String
) {
    val selectedTeams: List<RivalFranchiseCandidate>
        get() = candidateTeams.filter { it.isSelected }

    val selectedCount: Int
        get() = selectedTeams.size

    val canApproveAbsorption: Boolean
        get() = selectedCount in 2..4

    val totalOwnerApprovalHit: Int
        get() = if (selectedCount == 0) 0 else -(4 + (selectedCount * ownerApprovalHitPerTeam))

    val totalPermanentTvBoostMillions: Long
        get() = if (selectedCount == 0) 0L else baseTvBoostMillions + selectedTeams.sumOf { it.annualBroadcastContributionMillions }

    val totalNewFanReachMillions: Double
        get() = ((selectedTeams.sumOf { it.fanBaseMillions }) * 10.0).roundToInt() / 10.0
}

data class RivalLeague(
    val isActive: Boolean = true,
    val name: String = "Apex Rogue Association",
    val capital: Long = 120L,
    val hype: Int = 58,
    val poachedPlayers: List<FreeAgentPlayer> = emptyList(),
    val sportType: SportType = SportType.BASKETBALL,
    val blacklistedPlayerIds: Set<String> = emptySet(),
    val lastPoachedSeasonYear: Int = 0,
    val totalPoachesCount: Int = 0,
    val mergerAttemptsCount: Int = 0,
    val seasonsSurvived: Int = 0,
    val highHypeSeasonsCount: Int = 0,
    val permanentTvRevenueBoostMillions: Long = 0L,
    val hasResolvedAbsorptionEvent: Boolean = false,
    val absorbedTeamNames: List<String> = emptyList(),
    val activeAbsorptionNegotiation: RivalAbsorptionNegotiationEvent? = null,
    val lastOffseasonPoachSummary: String = "Breakaway investors scouting high-rated free agents ahead of offseason.",
    val lastActionFeedback: String = "Monitoring insurgent league capitalization and broadcast buzz.",
    val historyLog: List<String> = emptyList()
) {
    val threatLevelLabel: String
        get() = when {
            activeAbsorptionNegotiation != null -> "Merger Summit"
            !isActive && absorbedTeamNames.isNotEmpty() -> "Absorbed (${absorbedTeamNames.size} Clubs)"
            !isActive -> "Folded / Absorbed"
            capital >= 110L && hype >= 65 -> "Critical Threat"
            capital >= 60L || hype >= 50 -> "Active Challenger"
            else -> "Weakened"
        }

    val threatColorHex: Long
        get() = when {
            activeAbsorptionNegotiation != null -> 0xFFD4AF37
            !isActive -> 0xFF10B981
            capital >= 110L && hype >= 65 -> 0xFFEF4444
            capital >= 60L || hype >= 50 -> 0xFFF59E0B
            else -> 0xFF06B6D4
        }
}

data class RivalPoachResult(
    val updatedRivalLeague: RivalLeague,
    val updatedFreeAgentPool: List<FreeAgentPlayer>,
    val newlyPoachedPlayers: List<FreeAgentPlayer>,
    val returnedPoolPlayers: List<FreeAgentPlayer> = emptyList(),
    val didFoldRivalLeague: Boolean = false,
    val triggeredAbsorptionNegotiation: RivalAbsorptionNegotiationEvent? = null,
    val headline: String? = null
)

data class RivalActionOutcome(
    val success: Boolean,
    val message: String,
    val updatedRivalLeague: RivalLeague,
    val updatedFreeAgentPool: List<FreeAgentPlayer>,
    val cashDeltaMillions: Long = 0L,
    val ownerApprovalDelta: Int = 0,
    val unionSatisfactionDelta: Int = 0,
    val strikeRiskDelta: Int = 0,
    val fanbaseDeltaMillions: Double = 0.0,
    val didFoldRivalLeague: Boolean = false,
    val returnedPlayersCount: Int = 0,
    val headline: String? = null
)

data class RivalAbsorptionOutcome(
    val success: Boolean,
    val absorbed: Boolean,
    val message: String,
    val updatedRivalLeague: RivalLeague,
    val absorbedFranchises: List<Franchise> = emptyList(),
    val updatedCities: List<MarketCity> = emptyList(),
    val updatedFreeAgentPool: List<FreeAgentPlayer> = emptyList(),
    val ownerApprovalDelta: Int = 0,
    val permanentTvBoostMillions: Long = 0L,
    val fanbaseDeltaMillions: Double = 0.0,
    val headline: String = ""
)

object RivalLeagueEngine {

    const val SURVIVAL_MIN_SEASONS_FAST_TRACK = 3
    const val SURVIVAL_STANDARD_SEASONS = 4
    const val HIGH_HYPE_TRIGGER_THRESHOLD = 70
    const val SUSTAINED_HIGH_HYPE_THRESHOLD = 65

    private data class CandidateIdentitySeed(
        val cityId: String,
        val cityName: String,
        val nickname: String,
        val colorHex: Long,
        val secondaryHex: Long,
        val populationMillions: Double,
        val stadiumCapacity: Int,
        val arenaName: String,
        val starName: String
    )

    private val fallbackMarketSeeds = listOf(
        CandidateIdentitySeed("rival_austin", "Austin", "Outlaws", 0xFFD97706L, 0xFF1F2937L, 2.4, 20500, "Lone Star Pavilion", "Darius Cole"),
        CandidateIdentitySeed("rival_louisville", "Louisville", "Thoroughbreds", 0xFF991B1BL, 0xFFFDE68AL, 1.4, 21200, "Bluegrass Garden", "Trey Callahan"),
        CandidateIdentitySeed("rival_st_louis", "St. Louis", "Archers", 0xFF1D4ED8L, 0xFF93C5FDL, 2.8, 20800, "Gateway Forum", "Marcus Holloway"),
        CandidateIdentitySeed("rival_san_diego", "San Diego", "Riptide", 0xFF0891B2L, 0xFFF59E0BL, 3.3, 19800, "Pacific Harbor Dome", "Mateo Cruz"),
        CandidateIdentitySeed("rival_nashville", "Nashville", "Sound", 0xFF7C3AEDL, 0xFFFBBF24L, 2.1, 20200, "Music Row Arena", "Jalen Brooks"),
        CandidateIdentitySeed("rival_montreal", "Montreal", "Royals", 0xFF0F766EL, 0xFFE5E7EBL, 4.2, 21500, "Centre Champlain", "Luc Tremblay"),
        CandidateIdentitySeed("rival_baltimore", "Baltimore", "Clippers", 0xFFEA580CL, 0xFF111827L, 2.8, 20000, "Harborfront Coliseum", "Andre Price"),
        CandidateIdentitySeed("rival_kansas_city", "Kansas City", "Monarchs", 0xFF4F46E5L, 0xFFFCD34DL, 2.2, 19600, "Heartland Center", "Kieran Vance")
    )

    private val rivalNicknames = listOf(
        "Renegades", "Stallions", "Monarchs", "Vanguard", "Centurions", "Voyagers", "Comets", "Corsairs"
    )
    private val rivalColors = listOf(
        0xFFD97706L to 0xFF1E293BL,
        0xFF0891B2L to 0xFFFDE68AL,
        0xFF7C3AEDL to 0xFFF3F4F6L,
        0xFFDC2626L to 0xFFFBBF24L
    )

    fun defaultNameForSport(sportType: SportType): String = when (sportType) {
        SportType.BASKETBALL -> "Continental Hoops Alliance"
        SportType.FOOTBALL -> "North American Blitz League"
        SportType.HOCKEY -> "Northern Breakaway Circuit"
        SportType.SOCCER -> "Atlantic Rebel Super League"
    }

    fun createDefaultRivalLeague(sportType: SportType): RivalLeague = RivalLeague(
        isActive = true,
        name = defaultNameForSport(sportType),
        capital = 120L,
        hype = 58,
        poachedPlayers = emptyList(),
        sportType = sportType,
        blacklistedPlayerIds = emptySet(),
        lastPoachedSeasonYear = 0,
        totalPoachesCount = 0,
        mergerAttemptsCount = 0,
        seasonsSurvived = 0,
        highHypeSeasonsCount = 0,
        permanentTvRevenueBoostMillions = 0L,
        hasResolvedAbsorptionEvent = false,
        absorbedTeamNames = emptyList(),
        activeAbsorptionNegotiation = null,
        lastOffseasonPoachSummary = "Breakaway financiers hold \$120M war chest targeting prime free agents.",
        lastActionFeedback = "Rival Watch desk online.",
        historyLog = listOf("Breakaway syndicate '${defaultNameForSport(sportType)}' incorporated with \$120M backing.")
    )

    fun determineOffseasonPoachTargetCount(capital: Long, hype: Int): Int {
        if (capital <= 0L) return 0
        val compositeThreat = (capital.coerceAtMost(220L) * 0.65) + (hype.coerceIn(0, 100) * 1.1)
        return when {
            compositeThreat >= 140.0 && capital >= 75L -> 3
            compositeThreat >= 92.0 && capital >= 35L -> 2
            else -> 1
        }.coerceIn(1, 3)
    }

    fun isEligibleForAbsorptionNegotiation(
        rivalLeague: RivalLeague,
        seasonsSurvived: Int = rivalLeague.seasonsSurvived,
        hype: Int = rivalLeague.hype,
        highHypeSeasonsCount: Int = rivalLeague.highHypeSeasonsCount
    ): Boolean {
        if (!rivalLeague.isActive || rivalLeague.capital <= 0L) return false
        if (rivalLeague.hasResolvedAbsorptionEvent || rivalLeague.activeAbsorptionNegotiation != null) return false
        if (seasonsSurvived < SURVIVAL_MIN_SEASONS_FAST_TRACK) return false
        if (hype < HIGH_HYPE_TRIGGER_THRESHOLD) return false
        return seasonsSurvived >= SURVIVAL_STANDARD_SEASONS ||
            (seasonsSurvived >= SURVIVAL_MIN_SEASONS_FAST_TRACK && (hype >= 75 || highHypeSeasonsCount >= 2))
    }

    fun buildAbsorptionNegotiationEvent(
        rivalLeague: RivalLeague,
        existingCities: List<MarketCity>,
        seasonYear: Int
    ): RivalAbsorptionNegotiationEvent {
        val occupiedCityNames = existingCities.filter { it.hasFranchise }.map { it.name.lowercase() }.toSet()
        val openRealCities = existingCities
            .filter { !it.hasFranchise && it.name.lowercase() !in occupiedCityNames }
            .sortedByDescending { it.populationMillions }

        val candidates = mutableListOf<RivalFranchiseCandidate>()
        val usedCityNames = occupiedCityNames.toMutableSet()

        openRealCities.take(4).forEachIndexed { idx, city ->
            usedCityNames.add(city.name.lowercase())
            val colorPair = rivalColors[idx % rivalColors.size]
            val nick = rivalNicknames[idx % rivalNicknames.size]
            val starName = rivalLeague.poachedPlayers.getOrNull(idx)?.name
                ?: fallbackMarketSeeds[idx % fallbackMarketSeeds.size].starName
            val tvBonus = (12L + (city.populationMillions * 1.6).roundToLong()).coerceIn(11L, 20L)
            val rating = (86 - idx).coerceIn(80, 88)
            candidates.add(
                RivalFranchiseCandidate(
                    id = "rival_cand_${city.id}_$seasonYear",
                    cityId = city.id,
                    cityName = city.name,
                    nickname = nick,
                    colorHex = colorPair.first,
                    secondaryColorHex = colorPair.second,
                    rosterRating = rating,
                    fanBaseMillions = ((city.populationMillions * 0.42) * 10.0).roundToInt() / 10.0,
                    marketPopulationMillions = city.populationMillions,
                    stadiumCapacity = city.arenaCapacity,
                    arenaName = "${city.name} Union Pavilion",
                    localSponsorRevenueMillions = (city.populationMillions * 5.2).roundToLong().coerceIn(14L, 38L),
                    annualBroadcastContributionMillions = tvBonus,
                    signatureStarName = starName,
                    isSelected = idx < 3 // Default top 3 pre-selected; user can adjust 2..4
                )
            )
        }

        var seedIdx = 0
        while (candidates.size < 4 && seedIdx < fallbackMarketSeeds.size) {
            val seed = fallbackMarketSeeds[seedIdx++]
            if (seed.cityName.lowercase() in usedCityNames) continue
            usedCityNames.add(seed.cityName.lowercase())
            val idx = candidates.size
            val starName = rivalLeague.poachedPlayers.getOrNull(idx)?.name ?: seed.starName
            val tvBonus = (12L + (seed.populationMillions * 1.5).roundToLong()).coerceIn(11L, 19L)
            val rating = (86 - idx).coerceIn(80, 88)
            candidates.add(
                RivalFranchiseCandidate(
                    id = "rival_cand_${seed.cityId}_$seasonYear",
                    cityId = seed.cityId,
                    cityName = seed.cityName,
                    nickname = seed.nickname.replaceFirstChar { it.uppercase() },
                    colorHex = seed.colorHex,
                    secondaryColorHex = seed.secondaryHex,
                    rosterRating = rating,
                    fanBaseMillions = ((seed.populationMillions * 0.42) * 10.0).roundToInt() / 10.0,
                    marketPopulationMillions = seed.populationMillions,
                    stadiumCapacity = seed.stadiumCapacity,
                    arenaName = seed.arenaName,
                    localSponsorRevenueMillions = (seed.populationMillions * 5.2).roundToLong().coerceIn(14L, 36L),
                    annualBroadcastContributionMillions = tvBonus,
                    signatureStarName = starName,
                    isSelected = idx < 3
                )
            )
        }

        return RivalAbsorptionNegotiationEvent(
            id = "rival_absorb_${seasonYear}_${rivalLeague.seasonsSurvived}",
            rivalLeagueName = rivalLeague.name,
            sportType = rivalLeague.sportType,
            seasonYear = seasonYear,
            seasonsSurvived = rivalLeague.seasonsSurvived,
            rivalHypeSnapshot = rivalLeague.hype,
            rivalCapitalSnapshot = rivalLeague.capital,
            candidateTeams = candidates,
            baseTvBoostMillions = 26L,
            ownerApprovalHitPerTeam = 5,
            headline = "HIGH-STAKES MERGER SUMMIT: ${rivalLeague.name} Survived ${rivalLeague.seasonsSurvived} Seasons (${rivalLeague.hype} Hype)!",
            description = "${rivalLeague.name} has cemented major prime-time demand after ${rivalLeague.seasonsSurvived} seasons. Networks are offering a massive permanent national TV escalator if you absorb their top 2–4 franchises—though incumbent owners warn of dilution and governance friction."
        )
    }

    fun processOffseasonPoaching(
        rivalLeague: RivalLeague,
        freeAgentPool: List<FreeAgentPlayer>,
        seasonYear: Int,
        existingCities: List<MarketCity> = emptyList()
    ): RivalPoachResult {
        if (!rivalLeague.isActive || rivalLeague.capital <= 0L) {
            return if (rivalLeague.poachedPlayers.isNotEmpty() || rivalLeague.isActive) {
                val folded = foldRivalLeagueInternal(
                    rivalLeague = rivalLeague,
                    freeAgentPool = freeAgentPool,
                    reasonHeadline = "RIVAL COLLAPSE: ${rivalLeague.name} ran out of capital; ${rivalLeague.poachedPlayers.size} player(s) returned to Free Agency."
                )
                RivalPoachResult(
                    updatedRivalLeague = folded.first,
                    updatedFreeAgentPool = folded.second,
                    newlyPoachedPlayers = emptyList(),
                    returnedPoolPlayers = rivalLeague.poachedPlayers,
                    didFoldRivalLeague = true,
                    headline = folded.first.lastOffseasonPoachSummary
                )
            } else {
                RivalPoachResult(
                    updatedRivalLeague = rivalLeague,
                    updatedFreeAgentPool = freeAgentPool,
                    newlyPoachedPlayers = emptyList()
                )
            }
        }

        if (rivalLeague.lastPoachedSeasonYear >= seasonYear) {
            return RivalPoachResult(
                updatedRivalLeague = rivalLeague,
                updatedFreeAgentPool = freeAgentPool,
                newlyPoachedPlayers = emptyList()
            )
        }

        val targetCount = determineOffseasonPoachTargetCount(rivalLeague.capital, rivalLeague.hype)
        val candidates = freeAgentPool
            .filter { it.id !in rivalLeague.blacklistedPlayerIds }
            .sortedWith(compareByDescending<FreeAgentPlayer> { it.overallRating }.thenByDescending { it.askingSalaryMillions })

        val highRatedFirst = candidates.filter { it.overallRating >= 78 }.ifEmpty { candidates }
        val selected = highRatedFirst.take(targetCount)

        val poachedIds = selected.map { it.id }.toSet()
        val remainingPool = freeAgentPool.filterNot { it.id in poachedIds }
        val signingSpend = selected.sumOf { player ->
            (player.askingSalaryMillions * 1.25).roundToLong().coerceIn(8L, 26L)
        }
        val nextCapital = (rivalLeague.capital - signingSpend).coerceAtLeast(0L)
        val hypeGain = if (selected.isEmpty()) 2 else (selected.size * 5) + if (selected.any { it.overallRating >= 84 }) 4 else 0
        val nextHype = (rivalLeague.hype + hypeGain).coerceIn(10, 100)
        val mergedPoached = (selected + rivalLeague.poachedPlayers).distinctBy { it.id }
        val namesLabel = selected.joinToString(", ") { "${it.name} (${it.overallRating} OVR)" }

        if (nextCapital <= 0L) {
            val collapseMsg = "RIVAL BANKRUPT (Y$seasonYear): ${rivalLeague.name} overextended chasing $namesLabel, hit \$0M capital, and folded — ${mergedPoached.size} player(s) returned to Free Agency!"
            val (foldedRival, restoredPool) = foldRivalLeagueInternal(
                rivalLeague = rivalLeague.copy(
                    capital = 0L,
                    poachedPlayers = mergedPoached,
                    lastPoachedSeasonYear = seasonYear,
                    totalPoachesCount = rivalLeague.totalPoachesCount + selected.size
                ),
                freeAgentPool = remainingPool,
                reasonHeadline = collapseMsg
            )
            return RivalPoachResult(
                updatedRivalLeague = foldedRival,
                updatedFreeAgentPool = restoredPool,
                newlyPoachedPlayers = selected,
                returnedPoolPlayers = mergedPoached,
                didFoldRivalLeague = true,
                headline = collapseMsg
            )
        }

        val nextSeasonsSurvived = rivalLeague.seasonsSurvived + 1
        val nextHighHypeSeasons = if (nextHype >= SUSTAINED_HIGH_HYPE_THRESHOLD) {
            rivalLeague.highHypeSeasonsCount + 1
        } else {
            rivalLeague.highHypeSeasonsCount
        }

        val poachSummary = if (selected.isEmpty()) {
            "Y$seasonYear Off-Season: ${rivalLeague.name} retained stars across season $nextSeasonsSurvived ($nextHype Hype)."
        } else {
            "Y$seasonYear Off-Season: ${rivalLeague.name} poached ${selected.size} star(s): $namesLabel (-\$$signingSpend M Cap)."
        }

        var updatedRival = rivalLeague.copy(
            capital = nextCapital,
            hype = nextHype,
            poachedPlayers = mergedPoached,
            lastPoachedSeasonYear = seasonYear,
            totalPoachesCount = rivalLeague.totalPoachesCount + selected.size,
            seasonsSurvived = nextSeasonsSurvived,
            highHypeSeasonsCount = nextHighHypeSeasons,
            lastOffseasonPoachSummary = poachSummary,
            historyLog = (listOf(poachSummary) + rivalLeague.historyLog).take(16)
        )

        var triggeredNegotiation: RivalAbsorptionNegotiationEvent? = null
        if (isEligibleForAbsorptionNegotiation(updatedRival, nextSeasonsSurvived, nextHype, nextHighHypeSeasons)) {
            val event = buildAbsorptionNegotiationEvent(
                rivalLeague = updatedRival,
                existingCities = existingCities,
                seasonYear = seasonYear
            )
            triggeredNegotiation = event
            updatedRival = updatedRival.copy(
                activeAbsorptionNegotiation = event,
                lastActionFeedback = event.headline,
                historyLog = (listOf(event.headline) + updatedRival.historyLog).take(16)
            )
        }

        return RivalPoachResult(
            updatedRivalLeague = updatedRival,
            updatedFreeAgentPool = remainingPool,
            newlyPoachedPlayers = selected,
            didFoldRivalLeague = false,
            triggeredAbsorptionNegotiation = triggeredNegotiation,
            headline = triggeredNegotiation?.headline ?: if (selected.isNotEmpty()) "RIVAL POACH ALERT: $poachSummary" else null
        )
    }

    fun toggleAbsorptionCandidate(
        rivalLeague: RivalLeague,
        candidateId: String
    ): RivalLeague {
        val event = rivalLeague.activeAbsorptionNegotiation ?: return rivalLeague
        val currentSelectedCount = event.selectedCount
        val target = event.candidateTeams.find { it.id == candidateId } ?: return rivalLeague
        // Enforce 2..4 range smoothly
        if (target.isSelected && currentSelectedCount <= 2) return rivalLeague
        if (!target.isSelected && currentSelectedCount >= 4) return rivalLeague

        val updatedCandidates = event.candidateTeams.map { cand ->
            if (cand.id == candidateId) cand.copy(isSelected = !cand.isSelected) else cand
        }
        return rivalLeague.copy(
            activeAbsorptionNegotiation = event.copy(candidateTeams = updatedCandidates)
        )
    }

    fun resolveAbsorptionNegotiation(
        rivalLeague: RivalLeague,
        existingFranchises: List<Franchise>,
        existingCities: List<MarketCity>,
        ruleBook: LeagueRuleBook,
        freeAgentPool: List<FreeAgentPlayer>,
        sportType: SportType,
        seasonYear: Int,
        acceptAbsorption: Boolean
    ): RivalAbsorptionOutcome {
        val event = rivalLeague.activeAbsorptionNegotiation ?: return RivalAbsorptionOutcome(
            success = false,
            absorbed = false,
            message = "No active rival absorption negotiation event.",
            updatedRivalLeague = rivalLeague
        )

        if (!acceptAbsorption) {
            val headline = "RIVAL PACT REJECTED: ${rivalLeague.name} remains an independent competitor (+3 Owner Approval for exclusivity)."
            val updatedRival = rivalLeague.copy(
                capital = rivalLeague.capital + 15L,
                hype = (rivalLeague.hype + 4).coerceAtMost(100),
                hasResolvedAbsorptionEvent = true,
                activeAbsorptionNegotiation = null,
                lastActionFeedback = headline,
                historyLog = (listOf(headline) + rivalLeague.historyLog).take(16)
            )
            return RivalAbsorptionOutcome(
                success = true,
                absorbed = false,
                message = headline,
                updatedRivalLeague = updatedRival,
                updatedCities = existingCities,
                updatedFreeAgentPool = freeAgentPool,
                ownerApprovalDelta = 3,
                permanentTvBoostMillions = 0L,
                fanbaseDeltaMillions = 0.0,
                headline = headline
            )
        }

        val chosenCandidates = event.selectedTeams
        if (chosenCandidates.size !in 2..4) {
            return RivalAbsorptionOutcome(
                success = false,
                absorbed = false,
                message = "Select between 2 and 4 rival teams to absorb.",
                updatedRivalLeague = rivalLeague
            )
        }

        val tvBoost = event.totalPermanentTvBoostMillions
        val ownerHit = event.totalOwnerApprovalHit
        val fanReachGain = event.totalNewFanReachMillions

        var workingCities = existingCities
        val existingCityIds = existingCities.map { it.id }.toMutableSet()
        val existingCityNames = existingCities.associateBy { it.name.lowercase() }.toMutableMap()
        val existingPoached = rivalLeague.poachedPlayers.toMutableList()
        val createdFranchises = mutableListOf<Franchise>()

        chosenCandidates.forEachIndexed { idx, cand ->
            val matchedCity = existingCityNames[cand.cityName.lowercase()]
            val resolvedCityId = matchedCity?.id ?: cand.cityId
            if (matchedCity != null) {
                workingCities = workingCities.map { city ->
                    if (city.id == matchedCity.id) city.copy(hasFranchise = true) else city
                }
            } else if (resolvedCityId !in existingCityIds) {
                existingCityIds.add(resolvedCityId)
                val isMontreal = cand.cityName.equals("Montreal", ignoreCase = true)
                val newCity = MarketCity(
                    id = resolvedCityId,
                    name = cand.cityName,
                    country = if (isMontreal) "Canada" else "USA",
                    region = if (cand.cityName in setOf("San Diego", "Austin")) "West" else "East",
                    populationMillions = cand.marketPopulationMillions,
                    tier = if (cand.marketPopulationMillions >= 3.0) MarketTier.TIER_1_MEGA else MarketTier.HIGH_GROWTH_SUNBELT,
                    annualGrowthRatePct = 2.4,
                    sportsAppetite = 82,
                    medianIncomeK = 74,
                    isInternational = isMontreal,
                    hasFranchise = true,
                    expansionFeeMillions = 50L,
                    arenaName = cand.arenaName,
                    arenaCapacity = cand.stadiumCapacity
                )
                workingCities = workingCities + newCity
                existingCityNames[cand.cityName.lowercase()] = newCity
            }

            val cityObj = existingCityNames[cand.cityName.lowercase()] ?: workingCities.first()
            val owner = OwnerGovernanceEngine.buildExpansionOwnershipGroup(
                city = cityObj,
                ownershipType = if (idx % 2 == 0) OwnershipType.MEDIA_MOGUL else OwnershipType.TECH_BILLIONAIRE,
                existingFranchises = existingFranchises + createdFranchises
            )
            val (conf, div) = InitialLeagueData.getGeographicAlignment(
                cityName = cand.cityName,
                enabledConferences = ruleBook.enabledConferences,
                enabledDivisions = ruleBook.enabledDivisions
            )
            val teamId = "team_rival_${resolvedCityId}_$seasonYear"
            val generatedRoster = TradeEngine.generateDefaultRosterForFranchise(
                teamId,
                cand.cityName,
                sportType,
                cand.rosterRating
            ).toMutableList()

            // Assign a returning poached star if available onto the absorbed team's roster
            if (existingPoached.isNotEmpty() && generatedRoster.isNotEmpty()) {
                val starFa = existingPoached.removeAt(0)
                val boostedStar = generatedRoster[0].copy(
                    id = "pl_absorbed_${starFa.id}",
                    name = starFa.name,
                    position = starFa.position,
                    overallRating = starFa.overallRating.coerceAtLeast(generatedRoster[0].overallRating),
                    age = starFa.age,
                    salaryMillions = starFa.askingSalaryMillions.coerceIn(6L, 28L),
                    isStarter = true,
                    depthOrder = 0
                )
                generatedRoster[0] = boostedStar
            }

            createdFranchises.add(
                Franchise(
                    id = teamId,
                    cityId = resolvedCityId,
                    cityName = cand.cityName,
                    nickname = cand.nickname,
                    colorHex = cand.colorHex,
                    secondaryColorHex = cand.secondaryColorHex,
                    ownership = owner,
                    conference = conf,
                    division = div,
                    rosterRating = cand.rosterRating,
                    fanBaseMillions = cand.fanBaseMillions,
                    fanMorale = 84,
                    fanLoyalty = 80,
                    stadiumCapacity = cand.stadiumCapacity,
                    stadiumAge = 5,
                    stadiumCondition = 91,
                    ticketPrice = 80,
                    annualPayrollMillions = generatedRoster.sumOf { it.salaryMillions }.coerceAtLeast(65L),
                    localSponsorRevenueMillions = cand.localSponsorRevenueMillions,
                    roster = generatedRoster,
                    draftPicks = TradeEngine.generateDefaultDraftPicksForFranchise(
                        teamId,
                        cand.teamFullName,
                        seasonYear
                    )
                )
            )
        }

        // Return any leftover poached players to Free Agency pool
        val existingPoolIds = freeAgentPool.map { it.id }.toSet()
        val returningFa = existingPoached.filter { it.id !in existingPoolIds }
        val updatedFaPool = (freeAgentPool + returningFa).sortedByDescending { it.overallRating }

        val teamNames = createdFranchises.joinToString(", ") { it.teamFullName }
        val headline = "HISTORIC RIVAL ABSORPTION: Absorbed ${createdFranchises.size} ${rivalLeague.name} clubs ($teamNames)! +\$${tvBoost}M/yr Permanent TV Boost ($ownerHit% Owner Approval)."

        val updatedRival = rivalLeague.copy(
            isActive = false,
            capital = 0L,
            hype = 0,
            poachedPlayers = emptyList(),
            permanentTvRevenueBoostMillions = rivalLeague.permanentTvRevenueBoostMillions + tvBoost,
            hasResolvedAbsorptionEvent = true,
            absorbedTeamNames = rivalLeague.absorbedTeamNames + createdFranchises.map { it.teamFullName },
            activeAbsorptionNegotiation = null,
            lastOffseasonPoachSummary = headline,
            lastActionFeedback = headline,
            historyLog = (listOf(headline) + rivalLeague.historyLog).take(16)
        )

        return RivalAbsorptionOutcome(
            success = true,
            absorbed = true,
            message = headline,
            updatedRivalLeague = updatedRival,
            absorbedFranchises = createdFranchises,
            updatedCities = workingCities,
            updatedFreeAgentPool = updatedFaPool,
            ownerApprovalDelta = ownerHit,
            permanentTvBoostMillions = tvBoost,
            fanbaseDeltaMillions = fanReachGain,
            headline = headline
        )
    }

    fun applyRivalWatchAction(
        rivalLeague: RivalLeague,
        action: RivalWatchAction,
        currentCashMillions: Long,
        freeAgentPool: List<FreeAgentPlayer>
    ): RivalActionOutcome {
        if (!rivalLeague.isActive) {
            return RivalActionOutcome(
                success = false,
                message = "${rivalLeague.name} has already folded and its players have returned.",
                updatedRivalLeague = rivalLeague,
                updatedFreeAgentPool = freeAgentPool
            )
        }
        if (currentCashMillions < action.cashCostMillions) {
            return RivalActionOutcome(
                success = false,
                message = "Need \$${action.cashCostMillions}M Commissioner Cash to execute ${action.title}.",
                updatedRivalLeague = rivalLeague,
                updatedFreeAgentPool = freeAgentPool
            )
        }

        val effectiveCapitalDrain = when (action) {
            RivalWatchAction.PROPOSE_MERGER -> if (rivalLeague.capital <= 50L) rivalLeague.capital else action.capitalDrainMillions
            else -> action.capitalDrainMillions
        }
        val nextCapital = (rivalLeague.capital - effectiveCapitalDrain).coerceAtLeast(0L)
        val nextHype = (rivalLeague.hype - action.hypeDrain).coerceIn(0, 100)
        val updatedBlacklist = if (action == RivalWatchAction.BLACKLIST_DEFECTORS) {
            rivalLeague.blacklistedPlayerIds + rivalLeague.poachedPlayers.map { it.id }
        } else {
            rivalLeague.blacklistedPlayerIds
        }
        val nextMergerAttempts = if (action == RivalWatchAction.PROPOSE_MERGER) {
            rivalLeague.mergerAttemptsCount + 1
        } else {
            rivalLeague.mergerAttemptsCount
        }

        if (nextCapital <= 0L) {
            val returnedCount = rivalLeague.poachedPlayers.size
            val foldHeadline = if (action == RivalWatchAction.PROPOSE_MERGER) {
                "MERGER & FOLD COMPLETE: ${rivalLeague.name} folded into league jurisdiction (\$0M capital left); $returnedCount poached player(s) returned to Free Agency!"
            } else {
                "RIVAL LEAGUE FOLDED: ${action.title} exhausted ${rivalLeague.name}'s capital to \$0M; $returnedCount poached player(s) returned to Free Agency!"
            }
            val (foldedRival, restoredPool) = foldRivalLeagueInternal(
                rivalLeague = rivalLeague.copy(
                    capital = 0L,
                    hype = 0,
                    blacklistedPlayerIds = updatedBlacklist,
                    mergerAttemptsCount = nextMergerAttempts
                ),
                freeAgentPool = freeAgentPool,
                reasonHeadline = foldHeadline
            )
            return RivalActionOutcome(
                success = true,
                message = foldHeadline,
                updatedRivalLeague = foldedRival,
                updatedFreeAgentPool = restoredPool,
                cashDeltaMillions = -action.cashCostMillions,
                ownerApprovalDelta = action.ownerApprovalDelta,
                unionSatisfactionDelta = action.unionSatisfactionDelta,
                strikeRiskDelta = action.strikeRiskDelta,
                fanbaseDeltaMillions = action.fanbaseDeltaMillions,
                didFoldRivalLeague = true,
                returnedPlayersCount = returnedCount,
                headline = foldHeadline
            )
        }

        val actionSummary = "${action.title}: ${rivalLeague.name} capital down \$$effectiveCapitalDrain M to \$$nextCapital M, hype $nextHype/100."
        val updatedRival = rivalLeague.copy(
            capital = nextCapital,
            hype = nextHype,
            blacklistedPlayerIds = updatedBlacklist,
            mergerAttemptsCount = nextMergerAttempts,
            lastActionFeedback = actionSummary,
            historyLog = (listOf(actionSummary) + rivalLeague.historyLog).take(16)
        )
        return RivalActionOutcome(
            success = true,
            message = actionSummary,
            updatedRivalLeague = updatedRival,
            updatedFreeAgentPool = freeAgentPool,
            cashDeltaMillions = -action.cashCostMillions,
            ownerApprovalDelta = action.ownerApprovalDelta,
            unionSatisfactionDelta = action.unionSatisfactionDelta,
            strikeRiskDelta = action.strikeRiskDelta,
            fanbaseDeltaMillions = action.fanbaseDeltaMillions,
            didFoldRivalLeague = false,
            returnedPlayersCount = 0,
            headline = "RIVAL WATCH (${action.badgeTag}): $actionSummary"
        )
    }

    private fun foldRivalLeagueInternal(
        rivalLeague: RivalLeague,
        freeAgentPool: List<FreeAgentPlayer>,
        reasonHeadline: String
    ): Pair<RivalLeague, List<FreeAgentPlayer>> {
        val existingIds = freeAgentPool.map { it.id }.toSet()
        val returningPlayers = rivalLeague.poachedPlayers.filter { it.id !in existingIds }
        val restoredPool = (freeAgentPool + returningPlayers).sortedByDescending { it.overallRating }
        val foldedRival = rivalLeague.copy(
            isActive = false,
            capital = 0L,
            hype = 0,
            poachedPlayers = emptyList(),
            activeAbsorptionNegotiation = null,
            lastOffseasonPoachSummary = reasonHeadline,
            lastActionFeedback = reasonHeadline,
            historyLog = (listOf(reasonHeadline) + rivalLeague.historyLog).take(16)
        )
        return foldedRival to restoredPool
    }
}
