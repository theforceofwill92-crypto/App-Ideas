package com.example.model

import kotlin.math.roundToInt
import kotlin.random.Random

data class InjurySimulationResult(
    val updatedFranchises: List<Franchise>,
    val newInjuryLogs: List<InjuryEventLog>,
    val headlines: List<String>
)

data class RecoveryTickResult(
    val updatedFranchises: List<Franchise>,
    val clearanceHeadlines: List<String>
)

object InjuryEngine {

    private val injuryContexts = listOf(
        "awkward landing attacking the lane in Q3",
        "hard screen collision near the perimeter",
        "non-contact deceleration strain in fast-break recovery",
        "tangled footing battling for a contested loose ball",
        "mid-air contact while finishing through defensive help"
    )

    fun countUnresolvedStarterHoles(franchise: Franchise): Int {
        if (franchise.roster.isEmpty()) return 0
        val starters = getActiveStarters(franchise)
        return starters.count { it.isUnavailableForGame }
    }

    fun getActiveStarters(franchise: Franchise): List<RosterPlayer> {
        if (franchise.roster.isEmpty()) return emptyList()
        val explicitStarters = franchise.roster.filter { it.isStarter }.sortedBy { it.depthOrder }
        return if (explicitStarters.isNotEmpty()) {
            explicitStarters.take(5)
        } else {
            franchise.roster.sortedBy { it.depthOrder }.take(5)
        }
    }

    fun getBenchPlayers(franchise: Franchise): List<RosterPlayer> {
        if (franchise.roster.isEmpty()) return emptyList()
        val starterIds = getActiveStarters(franchise).map { it.id }.toSet()
        return franchise.roster
            .filter { it.id !in starterIds }
            .sortedWith(compareBy<RosterPlayer> { it.activeInjury?.isOnInjuredReserve == true }.thenBy { it.depthOrder }.thenByDescending { it.overallRating })
    }

    /**
     * Computes live team match rating from the current 5-man Starting Depth Chart:
     * - Healthy starter contributes full OVR
     * - Playing hurt (Day-to-Day / Moderate) applies clinical OVR penalty (-3..-8)
     * - Leaving a Medically OUT / IR starter unadjusted creates a severe 56 OVR hole penalty
     * - Promoting a healthy backup recovers strength relative to the backup's real OVR
     */
    fun calculateDepthChartEffectiveRating(franchise: Franchise): Int {
        val conditioningBonus = franchise.facilities.conditioningOvrBonus
        if (franchise.roster.isEmpty()) return (franchise.rosterRating + conditioningBonus).coerceIn(52, 99)
        val starters = getActiveStarters(franchise)
        if (starters.isEmpty()) return (franchise.rosterRating + conditioningBonus).coerceIn(52, 99)

        // Compare healthy baseline top-5 OVR against current active 5-man starting unit
        val baselineTop5 = franchise.roster
            .sortedByDescending { it.overallRating }
            .take(5)
            .map { it.overallRating }
            .average()

        val activeUnitAvg = starters.map { p ->
            when {
                p.isUnavailableForGame -> 56.0 // Empty/unadjusted starter hole hurts severely
                !p.isInjured -> p.overallRating.toDouble()
                else -> p.effectiveOverallRating.toDouble()
            }
        }.average()

        val lineupDelta = (activeUnitAvg - baselineTop5).roundToInt()
        return (franchise.rosterRating + lineupDelta + conditioningBonus).coerceIn(52, 99)
    }

    fun advanceWeeklyRecovery(
        franchises: List<Franchise>,
        bonusRestWeek: Boolean = false
    ): RecoveryTickResult {
        val clearanceHeadlines = mutableListOf<String>()
        val updated = franchises.map { team ->
            val updatedRoster = team.roster.map { player ->
                val inj = player.activeInjury ?: return@map player
                // Bench / IR rest speeds up minor Day-to-Day healing by 1 additional week; Tier 3+ Training Centers accelerate rehab
                val facilityRecoveryBonus = if (team.facilities.trainingTier >= 3 && inj.weeksRemaining >= 2) 1 else 0
                val baseStep = if (bonusRestWeek) 2 else if (!player.isStarter && inj.type.severity == InjurySeverity.DAY_TO_DAY) 2 else 1
                val recoveryStep = baseStep + facilityRecoveryBonus
                val remaining = inj.weeksRemaining - recoveryStep
                if (remaining <= 0) {
                    clearanceHeadlines.add("MEDICAL CLEARANCE: ${player.name} (${team.teamFullName}) has recovered from ${inj.type.displayName} and is cleared to return!")
                    player.copy(activeInjury = null)
                } else {
                    player.copy(activeInjury = inj.copy(weeksRemaining = remaining))
                }
            }
            team.copy(roster = updatedRoster)
        }
        return RecoveryTickResult(updatedFranchises = updated, clearanceHeadlines = clearanceHeadlines)
    }

    fun simulateMatchInjuries(
        playedMatches: List<GameMatch>,
        franchises: List<Franchise>,
        week: Int,
        seasonYear: Int,
        isPlayoffs: Boolean,
        random: Random = Random.Default,
        injuryFrequency: InjuryFrequencyRule = InjuryFrequencyRule.STANDARD_REALISTIC,
        gameLength: GameLengthRule = GameLengthRule.STANDARD_48,
        committeeInjuryMultiplier: Double = 1.0
    ): InjurySimulationResult {
        if (injuryFrequency == InjuryFrequencyRule.INJURIES_OFF || injuryFrequency.chanceMultiplier <= 0.0) {
            return InjurySimulationResult(
                updatedFranchises = franchises,
                newInjuryLogs = emptyList(),
                headlines = emptyList()
            )
        }

        val mutableFranchises = franchises.map { it.copy(roster = it.roster.toList()) }.toMutableList()
        val newLogs = mutableListOf<InjuryEventLog>()
        val headlines = mutableListOf<String>()
        val ruleMultiplier = (injuryFrequency.chanceMultiplier * gameLength.injuryRiskMultiplier * committeeInjuryMultiplier)
            .coerceAtLeast(0.05)

        playedMatches.forEach { match ->
            if (!match.isPlayed) return@forEach

            val teamIds = listOf(match.homeTeamId, match.awayTeamId)
            teamIds.forEachIndexed { sideIdx, teamId ->
                val tIdx = mutableFranchises.indexOfFirst { it.id == teamId }
                if (tIdx == -1) return@forEachIndexed
                val team = mutableFranchises[tIdx]
                val oppId = if (sideIdx == 0) match.awayTeamId else match.homeTeamId
                val opponentName = mutableFranchises.find { it.id == oppId }?.teamFullName ?: "League Rival"

                // Check active starters / healthy eligible players eligible to get injured in this game
                val activeParticipants = getActiveStarters(team).filter { !it.isUnavailableForGame }
                if (activeParticipants.isEmpty()) return@forEachIndexed

                // Aggravation chance if playing hurt (28% base), otherwise ~10% base; reduced by Training Center & scaled by League Settings
                val hobbledStarter = activeParticipants.firstOrNull { it.isInjured }
                val riskReduction = team.facilities.injuryRiskReductionPct
                val hobbledChance = (28 * (100 - riskReduction) / 100.0 * ruleMultiplier).roundToInt().coerceIn(4, 75)
                val baseChance = (10 * (100 - riskReduction) / 100.0 * ruleMultiplier).roundToInt().coerceIn(2, 50)
                val injuryOccurs = if (hobbledStarter != null && random.nextInt(100) < hobbledChance) {
                    true
                } else {
                    random.nextInt(100) < baseChance
                }

                if (!injuryOccurs) return@forEachIndexed

                val targetPlayer = if (hobbledStarter != null && random.nextInt(100) < 60) {
                    hobbledStarter
                } else {
                    activeParticipants[random.nextInt(activeParticipants.size)]
                }

                val injType = pickWeightedInjuryType(
                    isAggravation = targetPlayer.isInjured,
                    random = random
                )
                val weeksOut = if (injType.minWeeks == injType.maxWeeks) {
                    injType.minWeeks
                } else {
                    random.nextInt(injType.minWeeks, injType.maxWeeks + 1)
                }
                val contextPhrase = injuryContexts[random.nextInt(injuryContexts.size)]
                val injury = PlayerInjury(
                    id = "inj_${seasonYear}_w${week}_${targetPlayer.id}",
                    type = injType,
                    weeksRemaining = weeksOut,
                    initialWeeks = weeksOut,
                    sustainedWeek = week,
                    sustainedOpponent = opponentName,
                    gameContext = "Sustained via $contextPhrase vs $opponentName",
                    isOnInjuredReserve = injType.severity == InjurySeverity.SEVERE
                )

                // If player gets a SEVERE injury, automatically mark IR; otherwise leave on depth chart requiring manager action if OUT
                val updatedRoster = team.roster.map { p ->
                    if (p.id == targetPlayer.id) p.copy(activeInjury = injury) else p
                }
                mutableFranchises[tIdx] = team.copy(roster = updatedRoster)

                val actionNote = if (!injType.severity.canPlayThrough) {
                    "OUT $weeksOut wk(s) — depth chart adjustment required!"
                } else {
                    "${injType.severity.label.substringBefore(" (")} ($weeksOut wk, ${injType.ovrPenalty} OVR)"
                }

                headlines.add(
                    "INJURY REPORT (W$week): ${targetPlayer.name} (${team.nickname}, ${targetPlayer.position}) suffered ${injType.displayName} vs $opponentName — $actionNote"
                )

                newLogs.add(
                    InjuryEventLog(
                        id = injury.id,
                        week = week,
                        seasonYear = seasonYear,
                        playerId = targetPlayer.id,
                        playerName = targetPlayer.name,
                        position = targetPlayer.position,
                        franchiseId = team.id,
                        franchiseName = team.teamFullName,
                        injuryType = injType,
                        weeksOut = weeksOut,
                        opponentName = opponentName,
                        context = injury.gameContext,
                        isPlayoffs = isPlayoffs
                    )
                )
            }
        }

        return InjurySimulationResult(
            updatedFranchises = mutableFranchises,
            newInjuryLogs = newLogs,
            headlines = headlines
        )
    }

    private fun pickWeightedInjuryType(isAggravation: Boolean, random: Random): InjuryType {
        val roll = random.nextInt(100)
        return if (isAggravation) {
            when {
                roll < 45 -> InjuryType.HAMSTRING_STRAIN
                roll < 75 -> InjuryType.MCL_SPRAIN
                roll < 92 -> InjuryType.RIB_FRACTURE
                else -> InjuryType.ACHILLES_TEAR
            }
        } else {
            when {
                roll < 22 -> InjuryType.ANKLE_SPRAIN
                roll < 36 -> InjuryType.WRIST_CONTUSION
                roll < 48 -> InjuryType.HIP_POINTER
                roll < 64 -> InjuryType.HAMSTRING_STRAIN
                roll < 76 -> InjuryType.SHOULDER_IMPINGEMENT
                roll < 86 -> InjuryType.CONCUSSION_PROTOCOL
                roll < 93 -> InjuryType.RIB_FRACTURE
                roll < 98 -> InjuryType.MCL_SPRAIN
                else -> InjuryType.ACHILLES_TEAR
            }
        }
    }

    fun promotePlayerToStarter(
        franchise: Franchise,
        targetPlayerId: String,
        replaceStarterId: String? = null
    ): Franchise {
        val target = franchise.roster.find { it.id == targetPlayerId } ?: return franchise
        if (target.isStarter) return franchise

        val currentStarters = getActiveStarters(franchise)
        if (currentStarters.size < 5) {
            val reindexed = reindexDepthChart(
                franchise.roster.map { p ->
                    if (p.id == targetPlayerId) p.copy(
                        isStarter = true,
                        depthOrder = currentStarters.size,
                        isAssignedToMinorLeague = false,
                        activeInjury = p.activeInjury?.copy(isOnInjuredReserve = false)
                    ) else p
                }
            )
            return franchise.copy(roster = reindexed)
        }

        // Choose starter to replace: explicit choice > medically OUT starter > injured starter > matching-pos lowest > overall lowest
        val starterToDemote = currentStarters.find { it.id == replaceStarterId }
            ?: currentStarters.firstOrNull { it.isMedicallyOut }
            ?: currentStarters.firstOrNull { it.isInjured }
            ?: currentStarters.filter { it.position == target.position }.minByOrNull { it.effectiveOverallRating }
            ?: currentStarters.minByOrNull { it.effectiveOverallRating }
            ?: return franchise

        val swapped = franchise.roster.map { p ->
            when (p.id) {
                target.id -> p.copy(
                    isStarter = true,
                    depthOrder = starterToDemote.depthOrder,
                    isAssignedToMinorLeague = false,
                    activeInjury = p.activeInjury?.copy(isOnInjuredReserve = false)
                )
                starterToDemote.id -> p.copy(
                    isStarter = false,
                    depthOrder = target.depthOrder.coerceAtLeast(5)
                )
                else -> p
            }
        }

        return franchise.copy(roster = reindexDepthChart(swapped))
    }

    fun demoteStarterToBench(franchise: Franchise, starterId: String): Franchise {
        val starter = franchise.roster.find { it.id == starterId } ?: return franchise
        if (!starter.isStarter) return franchise

        val bench = getBenchPlayers(franchise).filter { !it.isMedicallyOut }
        val replacement = bench.filter { it.position == starter.position }.maxByOrNull { it.effectiveOverallRating }
            ?: bench.maxByOrNull { it.effectiveOverallRating }

        if (replacement == null) {
            // Still move starter down if fewer than 5 non-out bench
            val updated = franchise.roster.map { p ->
                if (p.id == starterId) p.copy(isStarter = false, depthOrder = franchise.roster.size) else p
            }
            return franchise.copy(roster = reindexDepthChart(updated))
        }

        return promotePlayerToStarter(franchise, replacement.id, replaceStarterId = starterId)
    }

    fun toggleInjuredReserve(franchise: Franchise, playerId: String): Franchise {
        val target = franchise.roster.find { it.id == playerId } ?: return franchise
        val inj = target.activeInjury ?: return franchise
        val puttingOnIr = !inj.isOnInjuredReserve

        var updatedFranchise = franchise.copy(
            roster = franchise.roster.map { p ->
                if (p.id == playerId) {
                    p.copy(
                        activeInjury = inj.copy(isOnInjuredReserve = puttingOnIr),
                        isStarter = if (puttingOnIr) false else p.isStarter,
                        isAssignedToMinorLeague = if (puttingOnIr) false else p.isAssignedToMinorLeague
                    )
                } else p
            }
        )

        // If placing a starter onto IR, automatically pull best healthy bench player up to fill starter spot
        if (puttingOnIr && target.isStarter) {
            val healthyBench = getBenchPlayers(updatedFranchise).filter { !it.isMedicallyOut }
            val bestBackup = healthyBench.filter { it.position == target.position }.maxByOrNull { it.effectiveOverallRating }
                ?: healthyBench.maxByOrNull { it.effectiveOverallRating }
            if (bestBackup != null) {
                updatedFranchise = updatedFranchise.copy(
                    roster = updatedFranchise.roster.map { p ->
                        if (p.id == bestBackup.id) p.copy(isStarter = true, depthOrder = target.depthOrder, isAssignedToMinorLeague = false) else p
                    }
                )
            }
        }

        return updatedFranchise.copy(roster = reindexDepthChart(updatedFranchise.roster))
    }

    fun movePlayerDepthOrder(franchise: Franchise, playerId: String, moveUp: Boolean): Franchise {
        val ordered = franchise.roster.sortedWith(
            compareByDescending<RosterPlayer> { it.isStarter }.thenBy { it.depthOrder }.thenByDescending { it.overallRating }
        ).toMutableList()
        val idx = ordered.indexOfFirst { it.id == playerId }
        if (idx == -1) return franchise
        val swapIdx = if (moveUp) idx - 1 else idx + 1
        if (swapIdx !in ordered.indices) return franchise

        val tmp = ordered[idx]
        ordered[idx] = ordered[swapIdx]
        ordered[swapIdx] = tmp

        val reindexed = ordered.mapIndexed { i, p ->
            val starter = i < 5
            p.copy(
                depthOrder = i,
                isStarter = starter,
                isAssignedToMinorLeague = if (starter) false else p.isAssignedToMinorLeague
            )
        }
        return franchise.copy(roster = reindexed)
    }

    fun autoOptimizeDepthChart(franchise: Franchise): Franchise {
        if (franchise.roster.isEmpty()) return franchise
        // Put healthy & eligible highest effectiveOVR players in 0..4, medically OUT/IR or suspended on bench
        val sorted = franchise.roster.sortedWith(
            compareBy<RosterPlayer> { it.isUnavailableForGame }
                .thenBy { it.isInjured } // slight preference for healthy at equal tier
                .thenByDescending { it.effectiveOverallRating }
                .thenByDescending { it.overallRating }
        )
        val reindexed = sorted.mapIndexed { index, player ->
            val starter = index < 5 && !player.isUnavailableForGame
            player.copy(
                depthOrder = index,
                isStarter = starter,
                isAssignedToMinorLeague = if (starter || player.isUnavailableForGame) false else player.isAssignedToMinorLeague
            )
        }
        return franchise.copy(roster = reindexed)
    }

    fun recoverOffSeasonInjuries(franchises: List<Franchise>): List<Franchise> {
        return franchises.map { team ->
            val healedRoster = team.roster.map { p ->
                val inj = p.activeInjury ?: return@map p
                // 10+ week off-season recovers normal injuries; very severe late injuries keep at most 1-2W if severe
                val remaining = if (inj.type.severity == InjurySeverity.SEVERE && inj.weeksRemaining > 5) {
                    inj.weeksRemaining - 5
                } else 0
                if (remaining <= 0) {
                    p.copy(activeInjury = null)
                } else {
                    p.copy(activeInjury = inj.copy(weeksRemaining = remaining, isOnInjuredReserve = false))
                }
            }
            autoOptimizeDepthChart(team.copy(roster = healedRoster))
        }
    }

    private fun reindexDepthChart(roster: List<RosterPlayer>): List<RosterPlayer> {
        val starters = roster.filter { it.isStarter }.sortedBy { it.depthOrder }.take(5)
        val starterIds = starters.map { it.id }.toSet()
        val bench = roster.filter { it.id !in starterIds }
            .sortedWith(compareBy<RosterPlayer> { it.activeInjury?.isOnInjuredReserve == true }.thenBy { it.depthOrder }.thenByDescending { it.overallRating })
        return (starters + bench).mapIndexed { index, player ->
            player.copy(
                depthOrder = index,
                isStarter = index < 5 && player.id in starterIds
            )
        }
    }
}
