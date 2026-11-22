package com.example.model

import kotlin.math.roundToInt
import kotlin.random.Random

enum class RefereeTier(
    val label: String,
    val badgeColorHex: Long
) {
    VETERAN_CREW_CHIEF("Veteran Crew Chief", 0xFFF59E0B),
    SENIOR_OFFICIAL("Senior Floor Official", 0xFF06B6D4),
    DEVELOPMENTAL_CALLUP("Call-Up Official", 0xFF8B5CF6)
}

enum class RefereeArchetype(
    val label: String,
    val styleSummary: String
) {
    BY_THE_BOOK("By-The-Book Analyst", "Precise rulebook execution; resists home-crowd pressure"),
    PLAYER_COMMUNICATOR("Player Communicator", "De-escalates heated rivalries and coach disputes"),
    LET_THEM_PLAY("Let-Them-Play Flow", "Boosts pace and ratings; slight risk on late contact"),
    STRICT_WHISTLE("Strict Whistle", "Disciplined foul enforcement; sparks occasional coach friction")
}

data class RefereeProfile(
    val id: String,
    val name: String,
    val yearsExperience: Int,
    val tier: RefereeTier,
    val archetype: RefereeArchetype,
    val accuracyRating: Int,
    val composureRating: Int,
    val fatigueLevel: Int = 12,
    val blownCallsSeason: Int = 0,
    val gamesOfficiatedSeason: Int = 0,
    val isRestedOrBenched: Boolean = false,
    val benchWeeksRemaining: Int = 0,
    val isUnionSteward: Boolean = false
) {
    fun effectiveAccuracy(
        replacementRefsActive: Boolean = false,
        techErrorReductionPct: Int = 0
    ): Int {
        val fatiguePenalty = (fatigueLevel / 5)
        val benchPenalty = if (isRestedOrBenched) 6 else 0
        val replacementPenalty = if (replacementRefsActive) 18 else 0
        val techBoost = (techErrorReductionPct / 4)
        return (accuracyRating - fatiguePenalty - benchPenalty - replacementPenalty + techBoost)
            .coerceIn(40, 99)
    }

    val conditionLabel: String
        get() = when {
            isRestedOrBenched && benchWeeksRemaining > 0 -> "Suspended / Rest (${benchWeeksRemaining}W)"
            isRestedOrBenched -> "Scheduled Rest"
            fatigueLevel >= 65 -> "High Fatigue ($fatigueLevel%)"
            fatigueLevel >= 40 -> "Moderate Load ($fatigueLevel%)"
            else -> "Fresh ($fatigueLevel%)"
        }
}

data class RefereeAssignment(
    val matchId: String,
    val week: Int,
    val homeTeamId: String,
    val awayTeamId: String,
    val matchupTitle: String,
    val leadRefereeId: String,
    val leadRefereeName: String,
    val isMarqueeOrRivalry: Boolean = false,
    val stakesLabel: String = "Regular Slate",
    val projectedBlownCallRiskPct: Int = 14
)

data class RefereeUnionState(
    val unionName: String = "National League Officials Association (NLOA)",
    val unionSatisfaction: Int = 72,
    val lockoutRisk: Int = 18,
    val annualRetainerMillions: Long = 8L,
    val perGameBonusK: Int = 15,
    val cbaYearsRemaining: Int = 2,
    val mandatoryRestThreshold: Int = 65,
    val workloadProtectionActive: Boolean = true,
    val publicDisciplineProtection: Boolean = false,
    val replacementRefsActive: Boolean = false
) {
    val statusLabel: String
        get() = when {
            replacementRefsActive -> "LOCKOUT: REPLACEMENT CREWS ACTIVE"
            lockoutRisk >= 65 -> "STRIKE THREAT CRITICAL"
            lockoutRisk >= 42 -> "TENSE UNION RELATIONS"
            unionSatisfaction >= 75 -> "LABOR HARMONY"
            else -> "STABLE BARGAINING"
        }
}

enum class OfficiatingTechType(
    val id: String,
    val title: String,
    val shortTitle: String,
    val description: String,
    val costMillions: Long,
    val weeklyOpsMillions: Long,
    val errorReductionPct: Int,
    val integrityBoost: Int,
    val unionFrictionDelta: Int,
    val backlashDampenPct: Int
) {
    CENTRAL_REPLAY_HUB(
        id = "tech_replay_hub",
        title = "Centralized Replay Command Hub",
        shortTitle = "Replay Hub",
        description = "Dedicated remote video review bunker resolving out-of-bounds, clock, and buzzer-beater calls in under 25 seconds.",
        costMillions = 6L,
        weeklyOpsMillions = 1L,
        errorReductionPct = 14,
        integrityBoost = 4,
        unionFrictionDelta = -2,
        backlashDampenPct = 15
    ),
    LIVE_CHALLENGE_MIC(
        id = "tech_challenge_mic",
        title = "Coach Challenge & Live Mic Feed",
        shortTitle = "Mic & Challenge",
        description = "Broadcasts real-time referee pool audio and expands team coach challenges to defuse stadium and social backlash.",
        costMillions = 4L,
        weeklyOpsMillions = 0L,
        errorReductionPct = 8,
        integrityBoost = 4,
        unionFrictionDelta = +2,
        backlashDampenPct = 30
    ),
    HAWKEYE_OPTICAL_TRACKING(
        id = "tech_hawkeye_tracking",
        title = "Hawk-Eye Optical Line Tracking",
        shortTitle = "Hawk-Eye Lines",
        description = "Sub-millimeter multi-camera spatial triangulation automating boundary lines and shot/goal trajectories.",
        costMillions = 10L,
        weeklyOpsMillions = 1L,
        errorReductionPct = 18,
        integrityBoost = 5,
        unionFrictionDelta = -5,
        backlashDampenPct = 15
    ),
    AI_FOUL_COPILOT(
        id = "tech_ai_foul_copilot",
        title = "AI Micro-Contact & Whistle Co-Pilot",
        shortTitle = "AI Co-Pilot",
        description = "Real-time biomechanics computer vision alerts floor crews to phantom contact, flops, and missed flagrant fouls.",
        costMillions = 14L,
        weeklyOpsMillions = 2L,
        errorReductionPct = 24,
        integrityBoost = 6,
        unionFrictionDelta = -8,
        backlashDampenPct = 20
    ),
    AUTONOMOUS_AI_BOUNDARY(
        id = "tech_autonomous_ai_judge",
        title = "Autonomous AI Boundary & Clock Judge",
        shortTitle = "Autonomous AI",
        description = "Full automated electronic adjudication on boundary lines, shot clocks, and goaltending—sparks sharp referee union pushback.",
        costMillions = 18L,
        weeklyOpsMillions = 2L,
        errorReductionPct = 28,
        integrityBoost = 7,
        unionFrictionDelta = -12,
        backlashDampenPct = 25
    )
}

data class OfficiatingTechItem(
    val type: OfficiatingTechType,
    val isInstalled: Boolean = false,
    val installedYear: Int? = null
)

enum class BlownCallSeverity(
    val label: String,
    val baseOutragePct: Int,
    val integrityHit: Int,
    val accentHex: Long
) {
    MODERATE("Late-Game Controversy", 44, 4, 0xFFF59E0B),
    HIGH_PROFILE("Viral Buzzer-Beater Error", 68, 7, 0xFFEF4444),
    PLAYOFF_SCANDAL("Playoff Series-Altering Scandal", 88, 11, 0xFFDC2626)
}

data class BacklashResponseChoice(
    val id: String,
    val title: String,
    val stanceTag: String,
    val description: String,
    val integrityDelta: Int,
    val affectedFanMoraleDelta: Int,
    val unionSatisfactionDelta: Int,
    val lockoutRiskDelta: Int,
    val costMillions: Long = 0L,
    val benchesReferee: Boolean = false
)

data class BlownCallIncident(
    val id: String,
    val week: Int,
    val seasonYear: Int,
    val matchId: String,
    val matchupTitle: String,
    val finalScoreSummary: String,
    val offendingRefereeId: String,
    val offendingRefereeName: String,
    val affectedTeamId: String,
    val affectedTeamName: String,
    val benefitingTeamName: String,
    val callDescription: String,
    val severity: BlownCallSeverity,
    val fanOutragePct: Int,
    val integrityDamage: Int,
    val isResolved: Boolean = false,
    val chosenResponseTitle: String? = null,
    val resolutionHeadline: String? = null
)

data class OfficiatingState(
    val referees: List<RefereeProfile> = emptyList(),
    val assignments: List<RefereeAssignment> = emptyList(),
    val unionState: RefereeUnionState = RefereeUnionState(),
    val techCatalog: List<OfficiatingTechItem> = emptyList(),
    val incidents: List<BlownCallIncident> = emptyList(),
    val leagueCallAccuracyPct: Double = 92.8,
    val totalCallsAudited: Int = 180,
    val totalBlownCalls: Int = 13,
    val lastAuditHeadline: String = "Weekly crew slate calibrated and ready for assignment."
) {
    val installedTechCount: Int
        get() = techCatalog.count { it.isInstalled }

    val totalErrorReductionPct: Int
        get() = techCatalog.filter { it.isInstalled }.sumOf { it.type.errorReductionPct }.coerceAtMost(75)

    val totalBacklashDampeningPct: Int
        get() = techCatalog.filter { it.isInstalled }.sumOf { it.type.backlashDampenPct }.coerceAtMost(70)

    val weeklyTechOpsCostMillions: Long
        get() = techCatalog.filter { it.isInstalled }.sumOf { it.type.weeklyOpsMillions }

    val activeUnresolvedIncidents: List<BlownCallIncident>
        get() = incidents.filter { !it.isResolved }
}

data class OfficiatingWeekResult(
    val updatedState: OfficiatingState,
    val newIncidents: List<BlownCallIncident>,
    val updatedFranchises: List<Franchise>,
    val integrityDelta: Int,
    val weeklyTechOpsCostMillions: Long,
    val headlines: List<String>
)

object OfficiatingEngine {

    private val controversialCallScenarios = listOf(
        "Missed shooting contact on a potential game-winning buzzer attempt",
        "Erroneous late-clock possession reversal that flipped final possession",
        "Phantom flagrant technical ejection on the visiting star in the final two minutes",
        "Unreviewed goaltending / goal-line infraction that swung a 1-possession finish",
        "Inconsistent whistle on back-court trap resulting in decisive turnover"
    )

    fun defaultReferees(): List<RefereeProfile> = listOf(
        RefereeProfile("ref_1", "Marcus Sterling", 18, RefereeTier.VETERAN_CREW_CHIEF, RefereeArchetype.BY_THE_BOOK, 94, 92, fatigueLevel = 14, isUnionSteward = true),
        RefereeProfile("ref_2", "Elena Vasquez", 15, RefereeTier.VETERAN_CREW_CHIEF, RefereeArchetype.PLAYER_COMMUNICATOR, 91, 95, fatigueLevel = 10, isUnionSteward = true),
        RefereeProfile("ref_3", "Darnell Crawford", 14, RefereeTier.VETERAN_CREW_CHIEF, RefereeArchetype.STRICT_WHISTLE, 90, 88, fatigueLevel = 18),
        RefereeProfile("ref_4", "Kenji Takahashi", 11, RefereeTier.SENIOR_OFFICIAL, RefereeArchetype.BY_THE_BOOK, 88, 86, fatigueLevel = 16),
        RefereeProfile("ref_5", "Claire Gallagher", 9, RefereeTier.SENIOR_OFFICIAL, RefereeArchetype.PLAYER_COMMUNICATOR, 86, 89, fatigueLevel = 12),
        RefereeProfile("ref_6", "Victor Alvarez", 8, RefereeTier.SENIOR_OFFICIAL, RefereeArchetype.LET_THEM_PLAY, 84, 83, fatigueLevel = 22),
        RefereeProfile("ref_7", "Trevor Foster", 6, RefereeTier.SENIOR_OFFICIAL, RefereeArchetype.STRICT_WHISTLE, 82, 79, fatigueLevel = 24),
        RefereeProfile("ref_8", "Nadia Okafor", 4, RefereeTier.DEVELOPMENTAL_CALLUP, RefereeArchetype.LET_THEM_PLAY, 77, 76, fatigueLevel = 10),
        RefereeProfile("ref_9", "Lukas Lindqvist", 3, RefereeTier.DEVELOPMENTAL_CALLUP, RefereeArchetype.BY_THE_BOOK, 76, 74, fatigueLevel = 8),
        RefereeProfile("ref_10", "Samir Patel", 2, RefereeTier.DEVELOPMENTAL_CALLUP, RefereeArchetype.PLAYER_COMMUNICATOR, 74, 75, fatigueLevel = 6)
    )

    fun defaultTechCatalog(): List<OfficiatingTechItem> =
        OfficiatingTechType.values().map { type ->
            OfficiatingTechItem(type = type, isInstalled = false, installedYear = null)
        }

    fun createDefaultState(
        franchises: List<Franchise>,
        currentWeek: Int,
        matches: List<GameMatch>
    ): OfficiatingState {
        val refs = defaultReferees()
        val tech = defaultTechCatalog()
        val union = RefereeUnionState()
        val assignments = buildAssignmentsForMatches(
            matches = matches,
            referees = refs,
            franchises = franchises,
            unionState = union,
            techCatalog = tech,
            week = currentWeek
        )
        return OfficiatingState(
            referees = refs,
            assignments = assignments,
            unionState = union,
            techCatalog = tech,
            incidents = emptyList(),
            leagueCallAccuracyPct = 92.8,
            totalCallsAudited = 180,
            totalBlownCalls = 13,
            lastAuditHeadline = "NLOA crews assigned for Week $currentWeek matchups."
        )
    }

    fun calculateMatchBlownCallRiskPct(
        referee: RefereeProfile,
        isMarqueeOrRivalry: Boolean,
        unionState: RefereeUnionState,
        techCatalog: List<OfficiatingTechItem>
    ): Int {
        val reductionPct = techCatalog.filter { it.isInstalled }.sumOf { it.type.errorReductionPct }.coerceAtMost(75)
        val effAcc = referee.effectiveAccuracy(
            replacementRefsActive = unionState.replacementRefsActive,
            techErrorReductionPct = reductionPct
        )
        val rawRisk = (100 - effAcc) + (if (isMarqueeOrRivalry) 8 else 0) + (if (unionState.replacementRefsActive) 12 else 0)
        val afterTech = (rawRisk * (1.0 - (reductionPct / 115.0))).roundToInt()
        return afterTech.coerceIn(3, 62)
    }

    fun buildAssignmentsForMatches(
        matches: List<GameMatch>,
        referees: List<RefereeProfile>,
        franchises: List<Franchise>,
        unionState: RefereeUnionState,
        techCatalog: List<OfficiatingTechItem>,
        week: Int,
        existingAssignments: List<RefereeAssignment> = emptyList()
    ): List<RefereeAssignment> {
        val activePool = referees.filter { !it.isRestedOrBenched && it.benchWeeksRemaining <= 0 }
            .ifEmpty { referees }
        val sortedPool = activePool.sortedByDescending {
            it.effectiveAccuracy(unionState.replacementRefsActive) - (it.fatigueLevel / 2)
        }
        val usedIds = mutableSetOf<String>()
        val existingByMatchId = existingAssignments.associateBy { it.matchId }

        return matches.mapIndexed { idx, match ->
            val home = franchises.find { it.id == match.homeTeamId }
            val away = franchises.find { it.id == match.awayTeamId }
            val title = if (home != null && away != null) {
                "${away.cityName} @ ${home.cityName}"
            } else {
                "Matchup #${idx + 1}"
            }
            val isMarquee = match.rivalryIntensity >= 35 ||
                match.roundName.isNotBlank() ||
                ((home?.rosterRating ?: 75) >= 82 && (away?.rosterRating ?: 75) >= 82)
            val stakes = when {
                match.roundName.isNotBlank() -> match.roundName
                match.rivalryTitle != null -> "Rivalry: ${match.rivalryTitle}"
                isMarquee -> "Marquee National TV"
                else -> "Regular Slate"
            }

            val preservedRef = existingByMatchId[match.id]?.let { old ->
                activePool.find { it.id == old.leadRefereeId }
            }
            val chosenRef = if (preservedRef != null && preservedRef.id !in usedIds) {
                preservedRef
            } else {
                sortedPool.firstOrNull { it.id !in usedIds } ?: sortedPool[idx % sortedPool.size]
            }
            usedIds.add(chosenRef.id)

            RefereeAssignment(
                matchId = match.id,
                week = match.week.takeIf { it > 0 } ?: week,
                homeTeamId = match.homeTeamId,
                awayTeamId = match.awayTeamId,
                matchupTitle = title,
                leadRefereeId = chosenRef.id,
                leadRefereeName = if (unionState.replacementRefsActive) "Replacement Crew (${chosenRef.name} Sub)" else chosenRef.name,
                isMarqueeOrRivalry = isMarquee,
                stakesLabel = stakes,
                projectedBlownCallRiskPct = calculateMatchBlownCallRiskPct(
                    referee = chosenRef,
                    isMarqueeOrRivalry = isMarquee,
                    unionState = unionState,
                    techCatalog = techCatalog
                )
            )
        }
    }

    fun reassignReferee(
        state: OfficiatingState,
        matchId: String,
        newRefereeId: String
    ): OfficiatingState {
        val newRef = state.referees.find { it.id == newRefereeId } ?: return state
        if (newRef.isRestedOrBenched && newRef.benchWeeksRemaining > 0) return state

        val targetAssignment = state.assignments.find { it.matchId == matchId } ?: return state
        val previousRefId = targetAssignment.leadRefereeId
        val conflictingAssignment = state.assignments.find { it.leadRefereeId == newRefereeId && it.matchId != matchId }

        val updatedAssignments = state.assignments.map { assignment ->
            when (assignment.matchId) {
                matchId -> assignment.copy(
                    leadRefereeId = newRef.id,
                    leadRefereeName = if (state.unionState.replacementRefsActive) "Replacement Crew (${newRef.name} Sub)" else newRef.name,
                    projectedBlownCallRiskPct = calculateMatchBlownCallRiskPct(
                        referee = newRef,
                        isMarqueeOrRivalry = assignment.isMarqueeOrRivalry,
                        unionState = state.unionState,
                        techCatalog = state.techCatalog
                    )
                )
                conflictingAssignment?.matchId -> {
                    val swappedRef = state.referees.find { it.id == previousRefId } ?: newRef
                    assignment.copy(
                        leadRefereeId = swappedRef.id,
                        leadRefereeName = swappedRef.name,
                        projectedBlownCallRiskPct = calculateMatchBlownCallRiskPct(
                            referee = swappedRef,
                            isMarqueeOrRivalry = assignment.isMarqueeOrRivalry,
                            unionState = state.unionState,
                            techCatalog = state.techCatalog
                        )
                    )
                }
                else -> assignment
            }
        }
        return state.copy(
            assignments = updatedAssignments,
            lastAuditHeadline = "Assigned ${newRef.name} (${newRef.tier.label}) to ${targetAssignment.matchupTitle}."
        )
    }

    fun autoOptimizeAssignments(state: OfficiatingState): OfficiatingState {
        val availableRefs = state.referees
            .filter { !it.isRestedOrBenched && it.benchWeeksRemaining <= 0 }
            .sortedByDescending {
                it.effectiveAccuracy(state.unionState.replacementRefsActive, state.totalErrorReductionPct) - (it.fatigueLevel / 2)
            }
            .ifEmpty { state.referees }

        val prioritizedMatches = state.assignments.sortedByDescending { it.isMarqueeOrRivalry }
        val updatedByMatchId = mutableMapOf<String, RefereeAssignment>()
        prioritizedMatches.forEachIndexed { idx, assignment ->
            val ref = availableRefs[idx % availableRefs.size]
            updatedByMatchId[assignment.matchId] = assignment.copy(
                leadRefereeId = ref.id,
                leadRefereeName = ref.name,
                projectedBlownCallRiskPct = calculateMatchBlownCallRiskPct(
                    referee = ref,
                    isMarqueeOrRivalry = assignment.isMarqueeOrRivalry,
                    unionState = state.unionState,
                    techCatalog = state.techCatalog
                )
            )
        }
        val finalOrder = state.assignments.map { updatedByMatchId[it.matchId] ?: it }
        return state.copy(
            assignments = finalOrder,
            lastAuditHeadline = "Auto-optimized referee crews: Veteran Chiefs routed to marquee & rivalry games."
        )
    }

    fun toggleRefereeRest(
        state: OfficiatingState,
        refereeId: String,
        franchises: List<Franchise>
    ): OfficiatingState {
        val updatedRefs = state.referees.map { ref ->
            if (ref.id == refereeId) {
                val nextRest = !ref.isRestedOrBenched
                ref.copy(
                    isRestedOrBenched = nextRest,
                    benchWeeksRemaining = if (!nextRest) 0 else ref.benchWeeksRemaining,
                    fatigueLevel = if (nextRest) (ref.fatigueLevel - 15).coerceAtLeast(0) else ref.fatigueLevel
                )
            } else ref
        }
        val fakeMatches = state.assignments.map {
            GameMatch(
                id = it.matchId,
                week = it.week,
                homeTeamId = it.homeTeamId,
                awayTeamId = it.awayTeamId,
                roundName = if (it.stakesLabel.contains("Playoff") || it.stakesLabel.contains("Finals")) it.stakesLabel else "",
                rivalryIntensity = if (it.isMarqueeOrRivalry) 40 else 0
            )
        }
        val rebuiltAssignments = buildAssignmentsForMatches(
            matches = fakeMatches,
            referees = updatedRefs,
            franchises = franchises,
            unionState = state.unionState,
            techCatalog = state.techCatalog,
            week = state.assignments.firstOrNull()?.week ?: 1,
            existingAssignments = state.assignments
        )
        val toggled = updatedRefs.find { it.id == refereeId }
        val note = if (toggled?.isRestedOrBenched == true) {
            "Placed ${toggled.name} on load-management rest and reassigned open crew slots."
        } else {
            "Cleared ${toggled?.name ?: "official"} back into the active referee pool."
        }
        return state.copy(
            referees = updatedRefs,
            assignments = rebuiltAssignments,
            lastAuditHeadline = note
        )
    }

    fun evaluateWeekOfficiating(
        state: OfficiatingState,
        playedMatches: List<GameMatch>,
        nextMatches: List<GameMatch>,
        franchises: List<Franchise>,
        currentWeek: Int,
        seasonYear: Int,
        isPlayoffs: Boolean,
        forceIncidentForAudit: Boolean = false
    ): OfficiatingWeekResult {
        val assignmentByMatch = state.assignments.associateBy { it.matchId }
        val assignedRefIds = mutableSetOf<String>()
        val newIncidents = mutableListOf<BlownCallIncident>()
        val headlines = mutableListOf<String>()
        val updatedFranchises = franchises.toMutableList()

        val errorReductionPct = state.totalErrorReductionPct
        val refMap = state.referees.associateBy { it.id }.toMutableMap()

        var auditedCallsDelta = (playedMatches.size.coerceAtLeast(1) * 28)
        var blownCallsThisWeek = 0
        var integrityDelta = 0
        var fatiguePenaltyToUnion = 0

        playedMatches.forEachIndexed { idx, match ->
            val assignment = assignmentByMatch[match.id]
            val assignedRef = assignment?.let { refMap[it.leadRefereeId] }
                ?: refMap.values.filter { !it.isRestedOrBenched }.minByOrNull { it.fatigueLevel }
                ?: refMap.values.first()
            assignedRefIds.add(assignedRef.id)

            val margin = kotlin.math.abs(match.homeScore - match.awayScore)
            val isTightGame = margin <= 6
            val riskPct = calculateMatchBlownCallRiskPct(
                referee = assignedRef,
                isMarqueeOrRivalry = assignment?.isMarqueeOrRivalry ?: (isPlayoffs || match.rivalryIntensity >= 35),
                unionState = state.unionState,
                techCatalog = state.techCatalog
            )
            // High-profile controversy triggers when dice hits calibrated threshold (or forced for audit drill)
            val triggerThreshold = if (isTightGame) riskPct else (riskPct / 3).coerceAtLeast(2)
            val rolledBlownCall = if (forceIncidentForAudit && idx == 0) {
                true
            } else {
                Random.nextInt(100) < triggerThreshold && (newIncidents.isEmpty() || isPlayoffs)
            }

            if (rolledBlownCall) {
                blownCallsThisWeek += 1
                val homeTeam = updatedFranchises.find { it.id == match.homeTeamId }
                val awayTeam = updatedFranchises.find { it.id == match.awayTeamId }
                val loserTeam = when {
                    homeTeam != null && match.homeScore < match.awayScore -> homeTeam
                    awayTeam != null -> awayTeam
                    else -> updatedFranchises.first()
                }
                val winnerTeam = if (loserTeam.id == homeTeam?.id) awayTeam ?: loserTeam else homeTeam ?: loserTeam

                val severity = when {
                    isPlayoffs -> BlownCallSeverity.PLAYOFF_SCANDAL
                    isTightGame || (assignment?.isMarqueeOrRivalry == true) -> BlownCallSeverity.HIGH_PROFILE
                    else -> BlownCallSeverity.MODERATE
                }
                val dampenFactor = 1.0 - (state.totalBacklashDampeningPct / 100.0)
                val outrage = (severity.baseOutragePct * dampenFactor).roundToInt().coerceIn(18, 95)
                val hit = (severity.integrityHit * dampenFactor).roundToInt().coerceAtLeast(2)
                integrityDelta -= hit

                val scenario = controversialCallScenarios[(currentWeek + idx + margin) % controversialCallScenarios.size]
                val incident = BlownCallIncident(
                    id = "call_${seasonYear}_w${currentWeek}_${match.id}_${state.incidents.size + newIncidents.size + 1}",
                    week = currentWeek,
                    seasonYear = seasonYear,
                    matchId = match.id,
                    matchupTitle = "${awayTeam?.teamFullName ?: "Away"} at ${homeTeam?.teamFullName ?: "Home"}",
                    finalScoreSummary = "${awayTeam?.nickname ?: "Away"} ${match.awayScore} - ${homeTeam?.nickname ?: "Home"} ${match.homeScore}",
                    offendingRefereeId = assignedRef.id,
                    offendingRefereeName = if (state.unionState.replacementRefsActive) "Replacement Crew (${assignedRef.name} Sub)" else assignedRef.name,
                    affectedTeamId = loserTeam.id,
                    affectedTeamName = loserTeam.teamFullName,
                    benefitingTeamName = winnerTeam.teamFullName,
                    callDescription = scenario,
                    severity = severity,
                    fanOutragePct = outrage,
                    integrityDamage = hit
                )
                newIncidents.add(incident)
                headlines.add("OFFICIATING BACKLASH: ${incident.offendingRefereeName} under fire after ${incident.severity.label.lowercase()} in ${incident.matchupTitle}!")

                val affectedIdx = updatedFranchises.indexOfFirst { it.id == loserTeam.id }
                if (affectedIdx >= 0) {
                    val t = updatedFranchises[affectedIdx]
                    updatedFranchises[affectedIdx] = t.copy(fanMorale = (t.fanMorale - 3).coerceIn(20, 99))
                }
            }

            if (assignedRef.fatigueLevel >= state.unionState.mandatoryRestThreshold) {
                fatiguePenaltyToUnion += 3
            }

            refMap[assignedRef.id] = assignedRef.copy(
                gamesOfficiatedSeason = assignedRef.gamesOfficiatedSeason + 1,
                blownCallsSeason = assignedRef.blownCallsSeason + (if (rolledBlownCall) 1 else 0),
                fatigueLevel = (assignedRef.fatigueLevel + if (isPlayoffs || assignment?.isMarqueeOrRivalry == true) 16 else 11).coerceAtMost(95)
            )
        }

        // Rested / unassigned officials recover fatigue & serve out suspensions
        val updatedRefs = state.referees.map { original ->
            val working = refMap[original.id] ?: original
            if (working.id !in assignedRefIds) {
                val nextBenchWeeks = (working.benchWeeksRemaining - 1).coerceAtLeast(0)
                working.copy(
                    fatigueLevel = (working.fatigueLevel - 24).coerceAtLeast(4),
                    benchWeeksRemaining = nextBenchWeeks,
                    isRestedOrBenched = if (working.benchWeeksRemaining > 0 && nextBenchWeeks == 0) false else working.isRestedOrBenched
                )
            } else {
                working
            }
        }

        // Clean week with zero controversies builds integrity gently
        if (newIncidents.isEmpty() && playedMatches.isNotEmpty() && !state.unionState.replacementRefsActive) {
            integrityDelta += 1
        }

        val updatedSatisfaction = (state.unionState.unionSatisfaction - fatiguePenaltyToUnion).coerceIn(15, 100)
        val updatedLockoutRisk = (state.unionState.lockoutRisk + (fatiguePenaltyToUnion / 2)).coerceIn(0, 100)
        val replacementActive = updatedLockoutRisk >= 80 || state.unionState.replacementRefsActive
        val updatedUnion = state.unionState.copy(
            unionSatisfaction = updatedSatisfaction,
            lockoutRisk = updatedLockoutRisk,
            replacementRefsActive = replacementActive
        )

        val totalAudited = state.totalCallsAudited + auditedCallsDelta
        val totalBlown = state.totalBlownCalls + blownCallsThisWeek
        val baseAccuracy = (((totalAudited - totalBlown).toDouble() / totalAudited.coerceAtLeast(1)) * 100.0)
        val techAccuracyLift = (errorReductionPct * 0.08)
        val lockoutDrag = if (replacementActive) -4.2 else 0.0
        val newAccuracyPct = (Math.round((baseAccuracy + techAccuracyLift + lockoutDrag).coerceIn(72.0, 99.6) * 10.0) / 10.0)

        val nextAssignments = if (nextMatches.isNotEmpty()) {
            buildAssignmentsForMatches(
                matches = nextMatches,
                referees = updatedRefs,
                franchises = updatedFranchises,
                unionState = updatedUnion,
                techCatalog = state.techCatalog,
                week = currentWeek + if (forceIncidentForAudit) 0 else 1,
                existingAssignments = emptyList()
            )
        } else {
            state.assignments
        }

        val summaryHeadline = when {
            newIncidents.isNotEmpty() -> newIncidents.first().let {
                "Controversy in ${it.matchupTitle}: ${it.severity.label} (${it.fanOutragePct}% Fan Outrage)."
            }
            fatiguePenaltyToUnion > 0 -> "NLOA Grievance: Overworked officials logged burnout complaints."
            else -> "Clean officiating slate ($newAccuracyPct% call accuracy across Week $currentWeek)."
        }

        val finalState = state.copy(
            referees = updatedRefs,
            assignments = nextAssignments,
            unionState = updatedUnion,
            incidents = (newIncidents + state.incidents).take(20),
            leagueCallAccuracyPct = newAccuracyPct,
            totalCallsAudited = totalAudited,
            totalBlownCalls = totalBlown,
            lastAuditHeadline = summaryHeadline
        )
        return OfficiatingWeekResult(
            updatedState = finalState,
            newIncidents = newIncidents,
            updatedFranchises = updatedFranchises,
            integrityDelta = integrityDelta,
            weeklyTechOpsCostMillions = state.weeklyTechOpsCostMillions,
            headlines = headlines
        )
    }

    fun backlashOptionsForIncident(
        incident: BlownCallIncident,
        state: OfficiatingState
    ): List<BacklashResponseChoice> {
        val hasChallengeMic = state.techCatalog.any { it.type == OfficiatingTechType.LIVE_CHALLENGE_MIC && it.isInstalled }
        val l2mUnionHit = if (state.unionState.publicDisciplineProtection) -2 else -6
        val l2mIntegrityBoost = (incident.integrityDamage / 2).coerceAtLeast(3) + (if (hasChallengeMic) 2 else 0)

        return listOf(
            BacklashResponseChoice(
                id = "resp_l2m_apology",
                title = "Publish Transparent L2M Report & Apology",
                stanceTag = "PUBLIC TRANSPARENCY",
                description = "Publicly concede the error in the Last-Two-Minute report and explain rule mechanics to restore fan credibility.",
                integrityDelta = +l2mIntegrityBoost,
                affectedFanMoraleDelta = +4,
                unionSatisfactionDelta = l2mUnionHit,
                lockoutRiskDelta = +4,
                costMillions = 0L,
                benchesReferee = false
            ),
            BacklashResponseChoice(
                id = "resp_bench_crew",
                title = "Bench Crew Chief & Enforce Accountability Fine",
                stanceTag = "HARDLINE DISCIPLINE",
                description = "Bench ${incident.offendingRefereeName} for 2 weeks and reassign marquee playoff eligibility. Wins furious fans; angers NLOA.",
                integrityDelta = +(incident.integrityDamage).coerceAtLeast(5),
                affectedFanMoraleDelta = +6,
                unionSatisfactionDelta = -12,
                lockoutRiskDelta = +10,
                costMillions = 0L,
                benchesReferee = true
            ),
            BacklashResponseChoice(
                id = "resp_back_crew",
                title = "Stand Behind On-Court Judgment Call",
                stanceTag = "UNION SOLIDARITY",
                description = "Back the officiating crew's real-time interpretation and reject protest appeals. Locks in NLOA loyalty at fans' expense.",
                integrityDelta = -3,
                affectedFanMoraleDelta = -5,
                unionSatisfactionDelta = +9,
                lockoutRiskDelta = -8,
                costMillions = 0L,
                benchesReferee = false
            ),
            BacklashResponseChoice(
                id = "resp_emergency_audit",
                title = "Fund \$2M Independent Calibration & Rules Clinic",
                stanceTag = "SYSTEMIC REFORM",
                description = "Deploy \$2M into joint referee-coach film calibration workshops so both fans and union reps view the fix as constructive.",
                integrityDelta = +5,
                affectedFanMoraleDelta = +3,
                unionSatisfactionDelta = +4,
                lockoutRiskDelta = -4,
                costMillions = 2L,
                benchesReferee = false
            )
        )
    }

    fun resolveIncident(
        state: OfficiatingState,
        incidentId: String,
        choice: BacklashResponseChoice,
        franchises: List<Franchise>
    ): Triple<OfficiatingState, List<Franchise>, String> {
        val incident = state.incidents.find { it.id == incidentId }
            ?: return Triple(state, franchises, "Controversy incident not found.")

        val resolutionLine = "${choice.title}: ${incident.matchupTitle} controversy closed."
        val updatedIncidents = state.incidents.map {
            if (it.id == incidentId) {
                it.copy(
                    isResolved = true,
                    chosenResponseTitle = choice.title,
                    resolutionHeadline = resolutionLine
                )
            } else it
        }

        val updatedRefs = if (choice.benchesReferee) {
            state.referees.map { ref ->
                if (ref.id == incident.offendingRefereeId) {
                    ref.copy(isRestedOrBenched = true, benchWeeksRemaining = 2)
                } else ref
            }
        } else state.referees

        val nextSat = (state.unionState.unionSatisfaction + choice.unionSatisfactionDelta).coerceIn(10, 100)
        val nextRisk = (state.unionState.lockoutRisk + choice.lockoutRiskDelta).coerceIn(0, 100)
        val updatedUnion = state.unionState.copy(
            unionSatisfaction = nextSat,
            lockoutRisk = nextRisk,
            replacementRefsActive = nextRisk >= 80
        )

        val updatedFranchises = franchises.map { team ->
            if (team.id == incident.affectedTeamId) {
                team.copy(
                    fanMorale = (team.fanMorale + choice.affectedFanMoraleDelta).coerceIn(20, 99),
                    fanLoyalty = (team.fanLoyalty + if (choice.affectedFanMoraleDelta > 0) 2 else -1).coerceIn(15, 100)
                )
            } else team
        }

        val fakeMatches = state.assignments.map {
            GameMatch(
                id = it.matchId,
                week = it.week,
                homeTeamId = it.homeTeamId,
                awayTeamId = it.awayTeamId,
                roundName = if (it.stakesLabel.contains("Playoff")) it.stakesLabel else "",
                rivalryIntensity = if (it.isMarqueeOrRivalry) 40 else 0
            )
        }
        val rebuiltAssignments = if (choice.benchesReferee) {
            buildAssignmentsForMatches(
                matches = fakeMatches,
                referees = updatedRefs,
                franchises = updatedFranchises,
                unionState = updatedUnion,
                techCatalog = state.techCatalog,
                week = incident.week
            )
        } else state.assignments

        val nextState = state.copy(
            referees = updatedRefs,
            assignments = rebuiltAssignments,
            unionState = updatedUnion,
            incidents = updatedIncidents,
            lastAuditHeadline = resolutionLine
        )
        val news = "COMMISSIONER OFFICIATING RULING: $resolutionLine"
        return Triple(nextState, updatedFranchises, news)
    }
}
