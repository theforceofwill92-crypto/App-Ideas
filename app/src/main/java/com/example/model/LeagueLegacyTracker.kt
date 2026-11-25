package com.example.model

enum class LegacyMilestoneCategory(
    val title: String,
    val colorHex: Long
) {
    CHAMPIONSHIPS("Dynasties & Titles", 0xFFF59E0B),
    BROADCAST("Media & Broadcast", 0xFF06B6D4),
    EXPANSION("Franchise Growth", 0xFF8B5CF6),
    FINANCE("Treasury & Venues", 0xFF10B981),
    LABOR("Labor & Governance", 0xFF3B82F6),
    FANBASE("Fan Coalition", 0xFFEC4899)
}

enum class LeagueLegacyMilestoneId(
    val title: String,
    val description: String,
    val category: LegacyMilestoneCategory,
    val targetProgress: Int,
    val rewardLegacyPoints: Int,
    val unitLabel: String
) {
    DYNASTY_BUILDER(
        title = "Dynasty Builder",
        description = "Crown consecutive league champions (2+ back-to-back titles by the same franchise).",
        category = LegacyMilestoneCategory.CHAMPIONSHIPS,
        targetProgress = 2,
        rewardLegacyPoints = 110,
        unitLabel = "consecutive titles"
    ),
    TV_MOGUL(
        title = "TV Mogul",
        description = "Negotiate a record-breaking national broadcast contract worth \$150M+/yr.",
        category = LegacyMilestoneCategory.BROADCAST,
        targetProgress = 150,
        rewardLegacyPoints = 100,
        unitLabel = "\$M/yr TV deal"
    ),
    EXPANSION_ARCHITECT(
        title = "Expansion Architect",
        description = "Grow the league footprint to 10 or more active member franchises.",
        category = LegacyMilestoneCategory.EXPANSION,
        targetProgress = 10,
        rewardLegacyPoints = 85,
        unitLabel = "franchises"
    ),
    BILLION_DOLLAR_SHIELD(
        title = "Billion-Dollar Shield",
        description = "Elevate league enterprise valuation to \$1,000M (\$1.0B) or higher.",
        category = LegacyMilestoneCategory.FINANCE,
        targetProgress = 1000,
        rewardLegacyPoints = 120,
        unitLabel = "\$M valuation"
    ),
    GOLDEN_LABOR_ACCORD(
        title = "Golden Labor Accord",
        description = "Complete 3+ consecutive lockout-free seasons with union satisfaction >= 75%.",
        category = LegacyMilestoneCategory.LABOR,
        targetProgress = 3,
        rewardLegacyPoints = 80,
        unitLabel = "peaceful seasons"
    ),
    ARENA_NAMING_MAGNATE(
        title = "Arena Naming Magnate",
        description = "Generate \$40M+/yr in combined stadium naming rights revenue across league venues.",
        category = LegacyMilestoneCategory.FINANCE,
        targetProgress = 40,
        rewardLegacyPoints = 75,
        unitLabel = "\$M naming rights"
    ),
    IRON_COMMISSIONER(
        title = "Iron Commissioner",
        description = "Steward the league across 5+ completed championship seasons.",
        category = LegacyMilestoneCategory.LABOR,
        targetProgress = 5,
        rewardLegacyPoints = 95,
        unitLabel = "seasons completed"
    ),
    BELOVED_STEWARD(
        title = "Beloved League Steward",
        description = "Expand global reach to 25M+ fans while sustaining 80%+ average club loyalty.",
        category = LegacyMilestoneCategory.FANBASE,
        targetProgress = 25,
        rewardLegacyPoints = 85,
        unitLabel = "M global fans"
    )
}

data class LeagueLegacyMilestone(
    val id: LeagueLegacyMilestoneId,
    val title: String = id.title,
    val description: String = id.description,
    val category: LegacyMilestoneCategory = id.category,
    val currentProgress: Int = 0,
    val targetProgress: Int = id.targetProgress,
    val rewardLegacyPoints: Int = id.rewardLegacyPoints,
    val isUnlocked: Boolean = false,
    val unlockedYear: Int? = null,
    val progressNote: String = ""
) {
    val progressFraction: Float
        get() = if (targetProgress <= 0) 1f else (currentProgress.toFloat() / targetProgress.toFloat()).coerceIn(0f, 1f)
}

data class DynastyStreakSummary(
    val maxConsecutiveStreak: Int,
    val activeChampionStreak: Int,
    val dynastyTeamName: String?
)

data class LeagueLegacyEvaluationResult(
    val updatedState: JobSecurityLegacyState,
    val newlyUnlockedMilestones: List<LeagueLegacyMilestone>,
    val bonusLegacyPointsAwarded: Int,
    val headlines: List<String>
)

object LeagueLegacyTracker {

    fun analyzeChampionshipStreak(
        valuationHistory: List<LeagueValuationHistory>,
        currentSeasonChampionName: String? = null
    ): DynastyStreakSummary {
        val entries = valuationHistory
            .distinctBy { it.year }
            .sortedBy { it.year }
            .map { it.year to it.championName.trim() }
            .filter { it.second.isNotBlank() && !it.second.contains("TBD", ignoreCase = true) }
            .toMutableList()

        if (!currentSeasonChampionName.isNullOrBlank() &&
            !currentSeasonChampionName.contains("TBD", ignoreCase = true) &&
            entries.none { it.second == currentSeasonChampionName.trim() && it.first == (entries.lastOrNull()?.first ?: -1) }
        ) {
            val nextYear = (entries.lastOrNull()?.first ?: 2025) + 1
            entries.add(nextYear to currentSeasonChampionName.trim())
        }

        if (entries.isEmpty()) {
            return DynastyStreakSummary(maxConsecutiveStreak = 0, activeChampionStreak = 0, dynastyTeamName = null)
        }

        var maxStreak = 1
        var currentStreak = 1
        var dynastyLeader: String? = entries.first().second
        var currentTeam = entries.first().second

        for (i in 1 until entries.size) {
            val champ = entries[i].second
            if (champ.equals(currentTeam, ignoreCase = true)) {
                currentStreak++
                if (currentStreak >= maxStreak) {
                    maxStreak = currentStreak
                    dynastyLeader = champ
                }
            } else {
                currentTeam = champ
                currentStreak = 1
            }
        }

        return DynastyStreakSummary(
            maxConsecutiveStreak = maxStreak,
            activeChampionStreak = currentStreak,
            dynastyTeamName = dynastyLeader
        )
    }

    fun evaluateAndUnlockMilestones(
        current: JobSecurityLegacyState,
        input: CommissionerPerformanceInput,
        isSeasonRollover: Boolean = false
    ): LeagueLegacyEvaluationResult {
        val existingById = current.milestones.associateBy { it.id }

        val streakSummary = analyzeChampionshipStreak(
            valuationHistory = input.valuationHistory,
            currentSeasonChampionName = input.seasonChampionName
        )
        val bestStreak = maxOf(
            current.maxConsecutiveChampionStreak,
            streakSummary.maxConsecutiveStreak
        )
        val resolvedDynastyTeam = if (streakSummary.maxConsecutiveStreak >= current.maxConsecutiveChampionStreak) {
            streakSummary.dynastyTeamName ?: current.dynastyTeamName
        } else {
            current.dynastyTeamName ?: streakSummary.dynastyTeamName
        }

        val recordBroadcast = maxOf(
            current.recordBroadcastAnnualMillions,
            input.activeBroadcast.annualPayoutMillions
        )

        val lockoutFreeSeasons = if (isSeasonRollover) {
            if (!input.unionAgreement.isLockoutActive && input.unionAgreement.unionSatisfaction >= 72) {
                current.consecutiveLockoutFreeSeasons + 1
            } else if (input.unionAgreement.isLockoutActive) {
                0
            } else {
                current.consecutiveLockoutFreeSeasons
            }
        } else {
            val completedPeaceful = maxOf(current.seasonsServed, input.valuationHistory.distinctBy { it.year }.size)
            if (!input.unionAgreement.isLockoutActive && input.unionAgreement.unionSatisfaction >= 75) {
                maxOf(current.consecutiveLockoutFreeSeasons, completedPeaceful)
            } else {
                current.consecutiveLockoutFreeSeasons
            }
        }

        val namingRightsTotalM = input.franchises.sumOf { it.stadium.namingRightsRevenue.coerceAtLeast(0L) }.toInt()
        val seasonsCompletedCount = maxOf(
            current.seasonsServed,
            input.valuationHistory.distinctBy { it.year }.size
        )
        val avgFanLoyalty = if (input.franchises.isNotEmpty()) {
            input.franchises.map { it.fanLoyalty }.average().toInt()
        } else 75

        val rawProgressById = mapOf(
            LeagueLegacyMilestoneId.DYNASTY_BUILDER to bestStreak,
            LeagueLegacyMilestoneId.TV_MOGUL to recordBroadcast.toInt(),
            LeagueLegacyMilestoneId.EXPANSION_ARCHITECT to input.franchises.size,
            LeagueLegacyMilestoneId.BILLION_DOLLAR_SHIELD to input.leagueValuationMillions.toInt(),
            LeagueLegacyMilestoneId.GOLDEN_LABOR_ACCORD to lockoutFreeSeasons,
            LeagueLegacyMilestoneId.ARENA_NAMING_MAGNATE to namingRightsTotalM,
            LeagueLegacyMilestoneId.IRON_COMMISSIONER to seasonsCompletedCount,
            LeagueLegacyMilestoneId.BELOVED_STEWARD to if (avgFanLoyalty >= 80) input.totalFanbaseMillions.toInt() else (input.totalFanbaseMillions.toInt().coerceAtMost(18))
        )

        val newlyUnlocked = mutableListOf<LeagueLegacyMilestone>()
        val updatedMilestones = LeagueLegacyMilestoneId.entries.map { id ->
            val prev = existingById[id]
            val currentVal = rawProgressById[id] ?: 0
            val alreadyUnlocked = prev?.isUnlocked == true
            val meetsThreshold = when (id) {
                LeagueLegacyMilestoneId.BELOVED_STEWARD -> input.totalFanbaseMillions >= 25.0 && avgFanLoyalty >= 80
                else -> currentVal >= id.targetProgress
            }
            val nowUnlocked = alreadyUnlocked || meetsThreshold
            val clampedProgress = if (nowUnlocked) maxOf(id.targetProgress, currentVal) else currentVal.coerceIn(0, id.targetProgress)

            val note = when (id) {
                LeagueLegacyMilestoneId.DYNASTY_BUILDER ->
                    if (nowUnlocked) {
                        "Unlocked: ${resolvedDynastyTeam ?: "Repeat Champion"} captured $bestStreak consecutive title(s)."
                    } else if (bestStreak == 1 && !resolvedDynastyTeam.isNullOrBlank()) {
                        "1/2 titles: $resolvedDynastyTeam defending championship."
                    } else {
                        "0/2 consecutive titles recorded yet."
                    }
                LeagueLegacyMilestoneId.TV_MOGUL ->
                    if (nowUnlocked) {
                        "Record rights contract: \$${recordBroadcast}M/yr (${input.activeBroadcast.partnerName})."
                    } else {
                        "Peak TV deal \$${recordBroadcast}M / \$${id.targetProgress}M/yr target."
                    }
                LeagueLegacyMilestoneId.EXPANSION_ARCHITECT ->
                    "${input.franchises.size} / ${id.targetProgress} active clubs in league."
                LeagueLegacyMilestoneId.BILLION_DOLLAR_SHIELD ->
                    "\$${input.leagueValuationMillions}M / \$1,000M enterprise valuation."
                LeagueLegacyMilestoneId.GOLDEN_LABOR_ACCORD ->
                    if (!input.unionAgreement.isLockoutActive) {
                        "$lockoutFreeSeasons / ${id.targetProgress} lockout-free seasons (Union ${input.unionAgreement.unionSatisfaction}%)."
                    } else {
                        "Reset: Lockout active — restore CBA peace to rebuild streak."
                    }
                LeagueLegacyMilestoneId.ARENA_NAMING_MAGNATE ->
                    "\$${namingRightsTotalM}M / \$${id.targetProgress}M annual naming rights revenue."
                LeagueLegacyMilestoneId.IRON_COMMISSIONER ->
                    "$seasonsCompletedCount / ${id.targetProgress} completed seasons served."
                LeagueLegacyMilestoneId.BELOVED_STEWARD ->
                    "${String.format("%.1f", input.totalFanbaseMillions)}M fans • ${avgFanLoyalty}% avg loyalty (80%+ req)."
            }

            val updatedItem = LeagueLegacyMilestone(
                id = id,
                title = id.title,
                description = id.description,
                category = id.category,
                currentProgress = clampedProgress,
                targetProgress = id.targetProgress,
                rewardLegacyPoints = id.rewardLegacyPoints,
                isUnlocked = nowUnlocked,
                unlockedYear = if (nowUnlocked) (prev?.unlockedYear ?: input.seasonYear) else null,
                progressNote = note
            )
            if (!alreadyUnlocked && nowUnlocked) {
                newlyUnlocked.add(updatedItem)
            }
            updatedItem
        }

        val bonusLegacyPoints = newlyUnlocked.sumOf { it.rewardLegacyPoints }
        val headlines = newlyUnlocked.map { m ->
            "LEAGUE LEGACY MILESTONE UNLOCKED: '${m.title}' (+${m.rewardLegacyPoints} Legacy Pts) — ${m.progressNote}"
        }
        val nextState = current.copy(
            legacyPoints = current.legacyPoints + bonusLegacyPoints,
            milestones = updatedMilestones,
            unlockedMilestonesCount = updatedMilestones.count { it.isUnlocked },
            recordBroadcastAnnualMillions = recordBroadcast,
            consecutiveLockoutFreeSeasons = lockoutFreeSeasons,
            maxConsecutiveChampionStreak = bestStreak,
            dynastyTeamName = resolvedDynastyTeam,
            lastUpdateHeadline = headlines.firstOrNull() ?: current.lastUpdateHeadline
        )

        return LeagueLegacyEvaluationResult(
            updatedState = nextState,
            newlyUnlockedMilestones = newlyUnlocked,
            bonusLegacyPointsAwarded = bonusLegacyPoints,
            headlines = headlines
        )
    }
}
