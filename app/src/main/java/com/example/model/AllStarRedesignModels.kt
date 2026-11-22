package com.example.model

import com.example.viewmodel.LeagueUiState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class AllStarFormatType(
    val displayName: String,
    val shortLabel: String,
    val realWorldInspiration: String,
    val formatTag: String,
    val description: String,
    val rosterStructure: String,
    val baseViewershipIndex: Int,
    val basePlayerEffortScore: Int,
    val baseRevenueMillions: Long,
    val injuryRiskLabel: String,
    val fanSentimentBoost: Int,
    val unionFrictionDelta: Int,
    val accentHex: Long
) {
    CLASSIC_CONFERENCE(
        displayName = "East vs. West Classic",
        shortLabel = "East vs West",
        realWorldInspiration = "Traditional Conference Exhibition",
        formatTag = "LEGACY CRISIS",
        description = "Standard conference-vs-conference exhibition game. Familiar to purists, but suffers from zero-defense coasting and declining casual TV ratings.",
        rosterStructure = "2 Conference Squads (12v12) • Standard 4 Quarters",
        baseViewershipIndex = 46,
        basePlayerEffortScore = 36,
        baseRevenueMillions = 12L,
        injuryRiskLabel = "Low",
        fanSentimentBoost = 0,
        unionFrictionDelta = 0,
        accentHex = 0xFFEF4444
    ),
    USA_VS_WORLD(
        displayName = "USA vs. The World",
        shortLabel = "USA vs World",
        realWorldInspiration = "Global National Pride Rivalry",
        formatTag = "GLOBAL PRIDE",
        description = "Pits domestic American superstars against an International All-Star roster. National pride ignites real defensive intensity and global broadcast surges.",
        rosterStructure = "Team USA vs. Team World (12v12) • Global Primetime",
        baseViewershipIndex = 84,
        basePlayerEffortScore = 82,
        baseRevenueMillions = 24L,
        injuryRiskLabel = "Moderate",
        fanSentimentBoost = 5,
        unionFrictionDelta = 1,
        accentHex = 0xFF06B6D4
    ),
    CAPTAINS_ELAM_DRAFT(
        displayName = "Captains Live Draft & Elam",
        shortLabel = "Captains Draft",
        realWorldInspiration = "Televised Playground Draft + Target Score",
        formatTag = "CLUTCH FINISH",
        description = "Top two fan vote-getters draft mixed rosters live on broadcast before playing to an untimed fourth-quarter Target Score finale.",
        rosterStructure = "Team Captain A vs. Captain B • Untimed Target Score Q4",
        baseViewershipIndex = 79,
        basePlayerEffortScore = 75,
        baseRevenueMillions = 21L,
        injuryRiskLabel = "Low-Mod",
        fanSentimentBoost = 4,
        unionFrictionDelta = 0,
        accentHex = 0xFFF59E0B
    ),
    FOUR_TEAM_MINI_TOURNEY(
        displayName = "4-Team Mini-Tournament",
        shortLabel = "4-Team Blitz",
        realWorldInspiration = "3 All-Star Squads + Rising Stars Bracket",
        formatTag = "FAST BRACKET",
        description = "Four 8-player squads (3 Veteran All-Star teams + Rising Stars Champions) battle in rapid games to 40 points, injecting hungry young talent.",
        rosterStructure = "4 Squads (8v8) • Single-Elimination Semis & Final to 40",
        baseViewershipIndex = 82,
        basePlayerEffortScore = 78,
        baseRevenueMillions = 22L,
        injuryRiskLabel = "Low",
        fanSentimentBoost = 4,
        unionFrictionDelta = -1,
        accentHex = 0xFF3B82F6
    ),
    FOUR_NATIONS_CUP(
        displayName = "4-Nations International Summit",
        shortLabel = "4-Nations",
        realWorldInspiration = "NHL 4-Nations Style Round-Robin Cup",
        formatTag = "PEAK INTENSITY",
        description = "Four powerhouse squads (USA, Canada, Europe, Rest of World) clash in a short round-robin bracket with fierce national rivalry.",
        rosterStructure = "4 National Teams • Round-Robin Showcase + Championship",
        baseViewershipIndex = 90,
        basePlayerEffortScore = 91,
        baseRevenueMillions = 28L,
        injuryRiskLabel = "Elevated",
        fanSentimentBoost = 6,
        unionFrictionDelta = 3,
        accentHex = 0xFF10B981
    ),
    SKILLS_CARNIVAL(
        displayName = "Non-Contact Skills & 1v1 Carnival",
        shortLabel = "Skills Carnival",
        realWorldInspiration = "Pro-Style Decathlon & 1v1 King Bracket",
        formatTag = "ZERO INJURY FEAR",
        description = "Replaces half-hearted full-court contact with high-stakes 1v1 duels, precision shooting, obstacle relays, and mic'd-up skill showdowns.",
        rosterStructure = "1v1 King of the Court + 3-Team Skills Decathlon",
        baseViewershipIndex = 74,
        basePlayerEffortScore = 86,
        baseRevenueMillions = 18L,
        injuryRiskLabel = "Near Zero",
        fanSentimentBoost = 3,
        unionFrictionDelta = -3,
        accentHex = 0xFF8B5CF6
    )
}

data class AllStarIncentiveModifier(
    val id: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val costMillions: Long,
    val viewershipDelta: Int,
    val effortDelta: Int,
    val unionStrikeDelta: Int,
    val isEnabled: Boolean = false
)

data class AllStarSideEvent(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val viewershipBonus: Int,
    val effortBonus: Int,
    val costMillions: Long,
    val isSelected: Boolean = false
)

data class AllStarProjectedImpact(
    val projectedViewershipIndex: Int,
    val projectedViewersMillions: Double,
    val baselineLegacyViewersMillions: Double,
    val viewershipDeltaPct: Int,
    val playerEffortScore: Int,
    val playerApathyRiskPct: Int,
    val projectedGrossRevenueMillions: Long,
    val projectedCostMillions: Long,
    val projectedNetRevenueMillions: Long,
    val projectedFanbaseGainMillions: Double,
    val projectedMoraleDelta: Int,
    val projectedStrikeRiskDelta: Int,
    val tvRatingsGrade: String,
    val apathyDiagnosis: String
)

data class AllStarShowcaseRecap(
    val seasonYear: Int,
    val formatType: AllStarFormatType,
    val hostCityName: String,
    val headline: String,
    val matchupScoreSummary: String,
    val mvpPlayerName: String,
    val mvpTeamName: String,
    val mvpStatLine: String,
    val actualViewersMillions: Double,
    val viewershipRatingScore: Int,
    val playerEffortGrade: String,
    val playerEffortScore: Int,
    val netRevenueMillions: Long,
    val fanbaseGainMillions: Double,
    val isDressRehearsal: Boolean = false
)

data class AllStarRedesignState(
    val selectedFormat: AllStarFormatType = AllStarFormatType.CLASSIC_CONFERENCE,
    val incentives: List<AllStarIncentiveModifier> = AllStarEngine.defaultIncentives(),
    val sideEvents: List<AllStarSideEvent> = AllStarEngine.defaultSideEvents(),
    val hostCityId: String = "ny",
    val hostCityName: String = "New York",
    val consecutiveSameFormatYears: Int = 2,
    val isRedesignedAndPromoted: Boolean = false,
    val lastRecap: AllStarShowcaseRecap? = null,
    val recapHistory: List<AllStarShowcaseRecap> = emptyList()
)

data class AllStarSimulationOutcome(
    val recap: AllStarShowcaseRecap,
    val netRevenueMillions: Long,
    val fanbaseGainMillions: Double,
    val moraleDelta: Int,
    val loyaltyDelta: Int,
    val strikeRiskDelta: Int,
    val valuationGainMillions: Long,
    val headline: String,
    val updatedRedesignState: AllStarRedesignState
)

object AllStarEngine {

    fun defaultIncentives(): List<AllStarIncentiveModifier> = listOf(
        AllStarIncentiveModifier(
            id = "inc_elam_target",
            title = "Elam Target-Score Ending",
            shortLabel = "Elam Ending",
            description = "Turns off the game clock in the final frame—first team to reach Target Score (+24) wins on a clutch make.",
            costMillions = 0L,
            viewershipDelta = 10,
            effortDelta = 14,
            unionStrikeDelta = 0,
            isEnabled = false
        ),
        AllStarIncentiveModifier(
            id = "inc_prize_pool",
            title = "$5M Championship Prize Purse",
            shortLabel = "$5M Purse",
            description = "Awards $5M cash bonus split among winning roster players and their chosen youth charities.",
            costMillions = 5L,
            viewershipDelta = 6,
            effortDelta = 16,
            unionStrikeDelta = -3,
            isEnabled = false
        ),
        AllStarIncentiveModifier(
            id = "inc_playoff_stakes",
            title = "Finals Home-Court Advantage Stakes",
            shortLabel = "Finals Stakes",
            description = "Winning conference/captain grants tiebreaker home-court edge in the Championship Finals.",
            costMillions = 0L,
            viewershipDelta = 11,
            effortDelta = 12,
            unionStrikeDelta = 4,
            isEnabled = false
        ),
        AllStarIncentiveModifier(
            id = "inc_mic_altcast",
            title = "Live Player Mic'd-Up & Creator Alt-Cast",
            shortLabel = "Youth Alt-Cast",
            description = "Interactive split-screen broadcast with live player mics, streaming creators, and real-time shot telemetry.",
            costMillions = 2L,
            viewershipDelta = 12,
            effortDelta = 4,
            unionStrikeDelta = 1,
            isEnabled = false
        ),
        AllStarIncentiveModifier(
            id = "inc_workload_cap",
            title = "Veteran Minute Cap & Sports-Science Protocol",
            shortLabel = "Minute Cap",
            description = "Caps star minutes at 22 max to eliminate injury paranoia and encourage full-speed bursts.",
            costMillions = 1L,
            viewershipDelta = -2,
            effortDelta = 11,
            unionStrikeDelta = -4,
            isEnabled = false
        )
    )

    fun defaultSideEvents(): List<AllStarSideEvent> = listOf(
        AllStarSideEvent(
            id = "ev_1v1_king",
            name = "1v1 King of the Court Bracket",
            category = "Duel",
            description = "Single-elimination 1-on-1 showdown settling league superstar bragging rights.",
            viewershipBonus = 7,
            effortBonus = 8,
            costMillions = 2L,
            isSelected = false
        ),
        AllStarSideEvent(
            id = "ev_precision_shootout",
            name = "Deep-Range & Precision Shootout",
            category = "Accuracy",
            description = "Rapid-fire rack & long-distance moneyball challenge under primetime lights.",
            viewershipBonus = 5,
            effortBonus = 4,
            costMillions = 1L,
            isSelected = true
        ),
        AllStarSideEvent(
            id = "ev_aerial_freestyle",
            name = "Aerial Freestyle & Flair Showcase",
            category = "Showmanship",
            description = "High-flying acrobatic showcase judged by legends and live fan mobile polls.",
            viewershipBonus = 4,
            effortBonus = 2,
            costMillions = 1L,
            isSelected = true
        ),
        AllStarSideEvent(
            id = "ev_rising_stars",
            name = "Rising Stars & Affiliate Blitz",
            category = "Future Stars",
            description = "Top rookies & minor-league call-ups playing with chip-on-shoulder hunger.",
            viewershipBonus = 3,
            effortBonus = 6,
            costMillions = 1L,
            isSelected = true
        ),
        AllStarSideEvent(
            id = "ev_skills_relay",
            name = "Team Tactical Skills Relay",
            category = "Teamwork",
            description = "Timed passing, agility, and clutch execution relay between franchise trios.",
            viewershipBonus = 3,
            effortBonus = 4,
            costMillions = 1L,
            isSelected = false
        )
    )

    fun createDefaultState(cities: List<MarketCity>): AllStarRedesignState {
        val defaultCity = cities.firstOrNull { it.hasFranchise } ?: cities.firstOrNull()
        return AllStarRedesignState(
            selectedFormat = AllStarFormatType.CLASSIC_CONFERENCE,
            incentives = defaultIncentives(),
            sideEvents = defaultSideEvents(),
            hostCityId = defaultCity?.id ?: "ny",
            hostCityName = defaultCity?.name ?: "New York",
            consecutiveSameFormatYears = 2,
            isRedesignedAndPromoted = false,
            lastRecap = null,
            recapHistory = emptyList()
        )
    }

    fun computeProjectedImpact(
        redesignState: AllStarRedesignState,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        cities: List<MarketCity>,
        ruleBook: LeagueRuleBook
    ): AllStarProjectedImpact {
        val format = redesignState.selectedFormat
        val hostCity = cities.find { it.id == redesignState.hostCityId }
        val activeIncentives = redesignState.incentives.filter { it.isEnabled }
        val selectedEvents = redesignState.sideEvents.filter { it.isSelected }

        // Stagnation decay if running the same format year after year (heaviest on legacy classic)
        val stalenessPenalty = when {
            format == AllStarFormatType.CLASSIC_CONFERENCE -> (redesignState.consecutiveSameFormatYears * 5).coerceAtMost(18)
            redesignState.consecutiveSameFormatYears >= 2 -> ((redesignState.consecutiveSameFormatYears - 1) * 3).coerceAtMost(10)
            else -> 0
        }

        val noveltyBoost = if (format != AllStarFormatType.CLASSIC_CONFERENCE && redesignState.consecutiveSameFormatYears <= 1) 6 else 0
        val cityHypeBonus = when {
            hostCity == null -> 2
            hostCity.isInternational -> 7
            hostCity.tier == MarketTier.TIER_1_MEGA -> 6
            hostCity.tier == MarketTier.HIGH_GROWTH_SUNBELT -> 4
            else -> 3
        }
        val broadcastBonus = ((activeBroadcast.reachMultiplier - 1.0) * 8.0).roundToInt().coerceIn(0, 12)
        val paceBonus = if (ruleBook.shotClockFastPace) 3 else -2

        val incentivesViewDelta = activeIncentives.sumOf { it.viewershipDelta }
        val incentivesEffortDelta = activeIncentives.sumOf { it.effortDelta }
        val eventsViewDelta = selectedEvents.sumOf { it.viewershipBonus }.coerceAtMost(16)
        val eventsEffortDelta = selectedEvents.sumOf { it.effortBonus }.coerceAtMost(14)

        val avgLoyalty = if (franchises.isNotEmpty()) franchises.map { it.fanLoyalty }.average() else 75.0
        val loyaltyAdj = ((avgLoyalty - 70.0) * 0.2).roundToInt()

        val projectedViewershipIndex = (
            format.baseViewershipIndex +
                incentivesViewDelta +
                eventsViewDelta +
                cityHypeBonus +
                broadcastBonus +
                paceBonus +
                noveltyBoost +
                loyaltyAdj -
                stalenessPenalty
            ).coerceIn(22, 99)

        val playerEffortScore = (
            format.basePlayerEffortScore +
                incentivesEffortDelta +
                eventsEffortDelta +
                (if (format != AllStarFormatType.CLASSIC_CONFERENCE) 3 else 0) -
                (stalenessPenalty / 2)
            ).coerceIn(18, 99)

        val playerApathyRiskPct = (100 - playerEffortScore).coerceIn(3, 95)

        val baselineLegacyViewersMillions = 5.8
        val projectedViewersMillions = ((2.4 + (projectedViewershipIndex * 0.165)) * 10.0).roundToInt() / 10.0
        val viewershipDeltaPct = (((projectedViewersMillions - baselineLegacyViewersMillions) / baselineLegacyViewersMillions) * 100.0).roundToInt()

        val incentiveCost = activeIncentives.sumOf { it.costMillions }
        val eventCost = selectedEvents.sumOf { it.costMillions }
        val totalCost = incentiveCost + eventCost

        val marketCapacityBonus = ((hostCity?.arenaCapacity ?: 20000) / 5000L).coerceIn(3L, 8L)
        val ratingsRevenueBonus = (projectedViewershipIndex / 10L)
        val broadcastKickback = (activeBroadcast.annualPayoutMillions / 14L).coerceIn(2L, 8L)
        val grossRevenue = (format.baseRevenueMillions + marketCapacityBonus + ratingsRevenueBonus + broadcastKickback + (selectedEvents.size * 1L))
            .coerceAtLeast(12L)
        val netRevenue = (grossRevenue - totalCost).coerceAtLeast(10L)

        val fanbaseGainMillions = ((0.35 + (projectedViewershipIndex / 42.0) + (if (hostCity?.isInternational == true) 0.45 else 0.15)) * 10.0).roundToInt() / 10.0
        val moraleDelta = (format.fanSentimentBoost + (if (playerEffortScore >= 75) 2 else -1) + (if (projectedViewershipIndex >= 80) 1 else 0))
            .coerceIn(-3, 8)
        val strikeRiskDelta = format.unionFrictionDelta + activeIncentives.sumOf { it.unionStrikeDelta }

        val tvGrade = when {
            projectedViewershipIndex >= 90 -> "A+"
            projectedViewershipIndex >= 82 -> "A"
            projectedViewershipIndex >= 74 -> "B+"
            projectedViewershipIndex >= 64 -> "B"
            projectedViewershipIndex >= 52 -> "C"
            else -> "D-"
        }

        val diagnosis = when {
            playerEffortScore >= 82 && projectedViewershipIndex >= 80 ->
                "Electrifying Showcase — Stars Locked In & Ratings Surging"
            playerEffortScore >= 68 ->
                "Competitive Exhibition — Healthy Effort & Engaged Broadcast"
            playerEffortScore >= 52 ->
                "Mixed Engagement — Add Stakes or Target Ending to Curb Coasting"
            else ->
                "Critical Player Apathy — Zero-Defense Exhibition Hurting TV Value"
        }

        return AllStarProjectedImpact(
            projectedViewershipIndex = projectedViewershipIndex,
            projectedViewersMillions = projectedViewersMillions,
            baselineLegacyViewersMillions = baselineLegacyViewersMillions,
            viewershipDeltaPct = viewershipDeltaPct,
            playerEffortScore = playerEffortScore,
            playerApathyRiskPct = playerApathyRiskPct,
            projectedGrossRevenueMillions = grossRevenue,
            projectedCostMillions = totalCost,
            projectedNetRevenueMillions = netRevenue,
            projectedFanbaseGainMillions = fanbaseGainMillions,
            projectedMoraleDelta = moraleDelta,
            projectedStrikeRiskDelta = strikeRiskDelta,
            tvRatingsGrade = tvGrade,
            apathyDiagnosis = diagnosis
        )
    }

    fun simulateAllStarFestivities(
        state: LeagueUiState,
        isDressRehearsal: Boolean = false
    ): AllStarSimulationOutcome {
        val redesign = state.allStarRedesignState
        val impact = computeProjectedImpact(
            redesignState = redesign,
            franchises = state.franchises,
            activeBroadcast = state.activeBroadcast,
            cities = state.cities,
            ruleBook = state.ruleBook
        )

        // Pick real active stars from franchises to headline MVP & matchup
        val allHealthyPlayers = state.franchises.flatMap { franchise ->
            franchise.roster.filter { !it.isMedicallyOut }.map { player -> player to franchise }
        }
        val topCandidates = allHealthyPlayers.sortedByDescending { it.first.effectiveOverallRating }.take(6)
        val mvpPair = topCandidates.firstOrNull()
        val mvpName = mvpPair?.first?.name ?: "Marcus Vance"
        val mvpTeamName = mvpPair?.second?.teamFullName ?: (state.franchises.firstOrNull()?.teamFullName ?: "Apex Stars")

        val format = redesign.selectedFormat
        val sportScale = state.sportType.baseScoreScale
        val scoreSummary = when (format) {
            AllStarFormatType.CLASSIC_CONFERENCE ->
                "East ${sportScale + 78} - West ${sportScale + 74} (Minimal defense exhibition)"
            AllStarFormatType.USA_VS_WORLD ->
                "World ${sportScale + 42} - USA ${sportScale + 39} (Down-to-the-wire classic)"
            AllStarFormatType.CAPTAINS_ELAM_DRAFT ->
                "Team $mvpName ${sportScale + 54} - Team Rival ${sportScale + 51} (Target Score Winner!)"
            AllStarFormatType.FOUR_TEAM_MINI_TOURNEY ->
                "All-Star Gold 42 - Rising Stars Blitz 38 (Tournament Final)"
            AllStarFormatType.FOUR_NATIONS_CUP ->
                "Team North America 4 - Team Europe 3 (Overtime Championship Classic)"
            AllStarFormatType.SKILLS_CARNIVAL ->
                "$mvpName sweeps 1v1 King Crown + Precision Decathlon (96 pts)"
        }

        val mvpStatLine = when (state.sportType) {
            SportType.BASKETBALL -> "34 PTS • 7 AST • 6 REB • Clutch Winner"
            SportType.FOOTBALL -> "385 YDS • 4 TD • 0 INT"
            SportType.SOCCER -> "2 GOALS • 1 AST • MOTM"
            SportType.HOCKEY -> "2 G • 2 A • +4 +/- Rating"
        }

        val effortGrade = when {
            impact.playerEffortScore >= 88 -> "A+"
            impact.playerEffortScore >= 78 -> "A"
            impact.playerEffortScore >= 68 -> "B+"
            impact.playerEffortScore >= 55 -> "B-"
            else -> "D"
        }

        val actualViewers = impact.projectedViewersMillions
        val headline = if (isDressRehearsal) {
            "ALL-STAR PREVIEW (${redesign.hostCityName}): ${format.displayName} projects ${actualViewers}M viewers ($effortGrade effort, +\$${impact.projectedNetRevenueMillions}M net)."
        } else {
            "${format.displayName.uppercase()} IN ${redesign.hostCityName.uppercase()}: $mvpName ($mvpTeamName) wins MVP! ${actualViewers}M viewers ($effortGrade effort) • +\$${impact.projectedNetRevenueMillions}M net revenue."
        }

        val recap = AllStarShowcaseRecap(
            seasonYear = state.currentYear,
            formatType = format,
            hostCityName = redesign.hostCityName,
            headline = headline,
            matchupScoreSummary = scoreSummary,
            mvpPlayerName = mvpName,
            mvpTeamName = mvpTeamName,
            mvpStatLine = mvpStatLine,
            actualViewersMillions = actualViewers,
            viewershipRatingScore = impact.projectedViewershipIndex,
            playerEffortGrade = effortGrade,
            playerEffortScore = impact.playerEffortScore,
            netRevenueMillions = impact.projectedNetRevenueMillions,
            fanbaseGainMillions = impact.projectedFanbaseGainMillions,
            isDressRehearsal = isDressRehearsal
        )

        val updatedRedesignState = if (isDressRehearsal) {
            redesign.copy(lastRecap = recap)
        } else {
            redesign.copy(
                consecutiveSameFormatYears = redesign.consecutiveSameFormatYears + 1,
                lastRecap = recap,
                recapHistory = (listOf(recap) + redesign.recapHistory).take(10)
            )
        }

        val valuationGain = (14L + (impact.projectedNetRevenueMillions / 2L) + (if (impact.projectedViewershipIndex >= 80) 8L else 2L))
        val loyaltyDelta = when {
            impact.playerEffortScore >= 80 -> 2
            impact.playerEffortScore >= 65 -> 1
            else -> -1
        }

        return AllStarSimulationOutcome(
            recap = recap,
            netRevenueMillions = impact.projectedNetRevenueMillions,
            fanbaseGainMillions = impact.projectedFanbaseGainMillions,
            moraleDelta = impact.projectedMoraleDelta,
            loyaltyDelta = loyaltyDelta,
            strikeRiskDelta = impact.projectedStrikeRiskDelta,
            valuationGainMillions = valuationGain,
            headline = headline,
            updatedRedesignState = updatedRedesignState
        )
    }
}
