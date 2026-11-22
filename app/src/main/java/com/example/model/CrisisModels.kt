package com.example.model

import kotlin.random.Random

enum class CrisisArchetype(
    val displayTitle: String,
    val categoryLabel: String,
    val accentHex: Long,
    val summary: String
) {
    SPORTSBOOK_INTEGRITY(
        displayTitle = "Wagering & Integrity Breach",
        categoryLabel = "Integrity & Rules",
        accentHex = 0xFFEF4444,
        summary = "Suspicious betting markets, inside injury leaks, and federal sportsbook inquiries."
    ),
    ARENA_RELOCATION(
        displayTitle = "Arena Lease & Relocation Ultimatum",
        categoryLabel = "Franchise Stability",
        accentHex = 0xFFF59E0B,
        summary = "Owners threatening franchise relocation unless municipal or league subsidies arrive."
    ),
    SPONSOR_MORALITY(
        displayTitle = "Title Sponsor & Broadcast Fallout",
        categoryLabel = "Commercial & Media",
        accentHex = 0xFF06B6D4,
        summary = "On-air controversies triggering morality clause standoffs with flagship partners."
    ),
    SUPERSTAR_REVOLT(
        displayTitle = "Superstar Revolt & Medical Trust",
        categoryLabel = "Locker Room & Labor",
        accentHex = 0xFF8B5CF6,
        summary = "Franchise cornerstones challenging medical protocols, playing time, or ownership."
    ),
    CYBER_RANSOMWARE(
        displayTitle = "Ticketing & Broadcast Cyber-Ransom",
        categoryLabel = "Security & Operations",
        accentHex = 0xFFDC2626,
        summary = "Targeted cyber extortion threatening stadium gates and live streaming feeds."
    ),
    OWNERSHIP_FRAUD(
        displayTitle = "Ownership Insolvency & Audit Alert",
        categoryLabel = "Financial Governance",
        accentHex = 0xFFEAB308,
        summary = "Forensic vetting uncovers hidden debt covenants or misrepresented club collateral."
    ),
    CONCUSSION_SAFETY(
        displayTitle = "Player Safety Protocol Leak",
        categoryLabel = "Health & Union Trust",
        accentHex = 0xFFEC4899,
        summary = "Whistleblowers allege rushed return-to-play clearance after visible impact events."
    ),
    LABOR_OFFICIAL_SICKOUT(
        displayTitle = "Officiating & Venue Crew Sickout",
        categoryLabel = "Officiating & Operations",
        accentHex = 0xFF38BDF8,
        summary = "Travel burnout and bonus disputes spark coordinated primetime officiating walkouts."
    )
}

data class CrisisBranchFollowUpSpec(
    val followUpNodeId: String,
    val delayWeeks: Int,
    val teaserLabel: String
)

data class ScheduledCrisisBranch(
    val id: String,
    val arcId: String,
    val archetype: CrisisArchetype,
    val followUpNodeId: String,
    val triggerYear: Int,
    val triggerWeek: Int,
    val stageNumber: Int,
    val totalStages: Int,
    val teaserLabel: String,
    val priorDecisionLabel: String,
    val targetFranchiseId: String? = null,
    val targetFranchiseName: String? = null,
    val targetPlayerId: String? = null,
    val targetPlayerName: String? = null
)

data class ResolvedCrisisRecord(
    val id: String,
    val arcId: String,
    val archetype: CrisisArchetype,
    val stageNumber: Int,
    val totalStages: Int,
    val seasonYear: Int,
    val weekNumber: Int,
    val title: String,
    val chosenLabel: String,
    val narrativeOutcome: String,
    val consequenceTags: List<String> = emptyList(),
    val affectedEntityLabel: String = "",
    val spawnedBranchTeaser: String? = null
)

data class CrisisSystemState(
    val scheduledBranches: List<ScheduledCrisisBranch> = emptyList(),
    val resolvedHistory: List<ResolvedCrisisRecord> = emptyList(),
    val completedArcIds: Set<String> = emptySet(),
    val lastTriggeredArcId: String? = null
)

data class CrisisThreatMetric(
    val key: String,
    val title: String,
    val riskScore: Int,
    val driverSummary: String,
    val accentHex: Long
)

data class CrisisResolutionResult(
    val updatedCrisisState: CrisisSystemState,
    val updatedFranchises: List<Franchise>,
    val updatedUnion: LaborUnionAgreement,
    val updatedOfficiatingState: OfficiatingState,
    val updatedDisciplinaryState: DisciplinaryDeskState,
    val cashDeltaMillions: Long,
    val valuationDeltaMillions: Long,
    val fanbaseDeltaMillions: Double,
    val integrityDelta: Int,
    val parityDelta: Int,
    val ownerApprovalDelta: Int,
    val headline: String,
    val spawnedBranch: ScheduledCrisisBranch?
)

object CrisisEngine {

    private val SCHEDULED_MILESTONE_WEEKS = setOf(2, 5, 8, 11, 14)

    fun buildCrisisForWeekOrFollowUp(
        crisisState: CrisisSystemState,
        sportType: SportType,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        cities: List<MarketCity>,
        leagueIntegrityScore: Int,
        unionAgreement: LaborUnionAgreement,
        officiatingState: OfficiatingState,
        seasonYear: Int,
        week: Int,
        random: Random = Random.Default
    ): Pair<CrisisSystemState, CommissionerDilemma?> {
        // 1. Prioritize any scheduled branching follow-up due this year/week (or overdue)
        val dueBranch = crisisState.scheduledBranches.firstOrNull {
            it.triggerYear < seasonYear || (it.triggerYear == seasonYear && it.triggerWeek <= week)
        }
        if (dueBranch != null) {
            val remaining = crisisState.scheduledBranches.filterNot { it.id == dueBranch.id }
            val dilemma = materializeBranchNode(
                branch = dueBranch,
                sportType = sportType,
                franchises = franchises,
                activeBroadcast = activeBroadcast,
                activeSponsorship = activeSponsorship,
                cities = cities,
                currentWeek = week
            )
            return crisisState.copy(scheduledBranches = remaining) to dilemma
        }

        // 2. Determine whether a fresh Stage 1 Crisis erupts this week
        val dynamicStressTriggered = (week % 3 == 1) && (
            leagueIntegrityScore < 62 ||
                unionAgreement.strikeRisk > 52 ||
                officiatingState.unionState.lockoutRisk > 55
            )
        val isScheduledWeek = week in SCHEDULED_MILESTONE_WEEKS
        if (!isScheduledWeek && !dynamicStressTriggered) {
            return crisisState to null
        }

        return triggerFreshCrisis(
            crisisState = crisisState,
            sportType = sportType,
            franchises = franchises,
            activeBroadcast = activeBroadcast,
            activeSponsorship = activeSponsorship,
            cities = cities,
            leagueIntegrityScore = leagueIntegrityScore,
            unionAgreement = unionAgreement,
            officiatingState = officiatingState,
            seasonYear = seasonYear,
            week = week,
            preferredArchetype = null,
            random = random
        )
    }

    fun triggerFreshCrisis(
        crisisState: CrisisSystemState,
        sportType: SportType,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        cities: List<MarketCity>,
        leagueIntegrityScore: Int,
        unionAgreement: LaborUnionAgreement,
        officiatingState: OfficiatingState,
        seasonYear: Int,
        week: Int,
        preferredArchetype: CrisisArchetype? = null,
        random: Random = Random.Default
    ): Pair<CrisisSystemState, CommissionerDilemma?> {
        if (franchises.isEmpty()) return crisisState to null

        val activeScheduledArcs = crisisState.scheduledBranches.map { it.arcId }.toSet()
        val usedRecently = crisisState.completedArcIds + activeScheduledArcs
        val chosenArchetype = preferredArchetype ?: pickContextualArchetype(
            usedArcKeys = usedRecently,
            lastArcId = crisisState.lastTriggeredArcId,
            leagueIntegrityScore = leagueIntegrityScore,
            unionAgreement = unionAgreement,
            officiatingState = officiatingState,
            random = random
        )

        val entityContext = resolveEntityContext(
            archetype = chosenArchetype,
            franchises = franchises,
            random = random
        )
        val arcId = "arc_${chosenArchetype.name.lowercase()}_y${seasonYear}_w$week"
        val openingNodeId = openingNodeIdFor(chosenArchetype)

        val dilemma = buildNodeDilemma(
            nodeId = openingNodeId,
            arcId = arcId,
            archetype = chosenArchetype,
            sportType = sportType,
            franchises = franchises,
            activeBroadcast = activeBroadcast,
            activeSponsorship = activeSponsorship,
            cities = cities,
            week = week,
            targetFranchise = entityContext.first,
            targetPlayer = entityContext.second,
            priorDecisionLabel = null,
            isBranchFollowUp = false
        )

        val nextState = crisisState.copy(lastTriggeredArcId = chosenArchetype.name)
        return nextState to dilemma
    }

    private fun pickContextualArchetype(
        usedArcKeys: Set<String>,
        lastArcId: String?,
        leagueIntegrityScore: Int,
        unionAgreement: LaborUnionAgreement,
        officiatingState: OfficiatingState,
        random: Random
    ): CrisisArchetype {
        val candidates = CrisisArchetype.values().filter { arch ->
            val keyMatch = usedArcKeys.any { it.contains(arch.name.lowercase()) }
            !keyMatch && arch.name != lastArcId
        }.ifEmpty {
            CrisisArchetype.values().filter { it.name != lastArcId }.ifEmpty { CrisisArchetype.values().toList() }
        }

        // Bias toward high-stress league weaknesses cleanly
        val stressed = mutableListOf<CrisisArchetype>()
        if (leagueIntegrityScore < 75) {
            stressed.addAll(listOf(CrisisArchetype.SPORTSBOOK_INTEGRITY, CrisisArchetype.OWNERSHIP_FRAUD))
        }
        if (unionAgreement.strikeRisk >= 35 || unionAgreement.unionSatisfaction < 65) {
            stressed.addAll(listOf(CrisisArchetype.SUPERSTAR_REVOLT, CrisisArchetype.CONCUSSION_SAFETY))
        }
        if (officiatingState.unionState.lockoutRisk >= 45 || officiatingState.leagueCallAccuracyPct < 91.0) {
            stressed.add(CrisisArchetype.LABOR_OFFICIAL_SICKOUT)
        }
        val matchingStress = stressed.filter { it in candidates }
        return if (matchingStress.isNotEmpty() && random.nextInt(100) < 65) {
            matchingStress[random.nextInt(matchingStress.size)]
        } else {
            candidates[random.nextInt(candidates.size)]
        }
    }

    private fun resolveEntityContext(
        archetype: CrisisArchetype,
        franchises: List<Franchise>,
        random: Random
    ): Pair<Franchise, RosterPlayer?> {
        val sortedByLowestMorale = franchises.sortedBy { it.fanMorale }
        val sortedByLowestPayroll = franchises.sortedBy { it.annualPayrollMillions }
        val sortedByTopPlayer = franchises.sortedByDescending { f -> f.roster.maxOfOrNull { it.overallRating } ?: 75 }

        val team = when (archetype) {
            CrisisArchetype.ARENA_RELOCATION, CrisisArchetype.OWNERSHIP_FRAUD ->
                sortedByLowestMorale.take(3).randomOrNull(random) ?: franchises.first()
            CrisisArchetype.SUPERSTAR_REVOLT ->
                sortedByTopPlayer.take(3).randomOrNull(random) ?: franchises.first()
            CrisisArchetype.CYBER_RANSOMWARE, CrisisArchetype.SPONSOR_MORALITY ->
                franchises.sortedByDescending { it.fanBaseMillions }.take(3).randomOrNull(random) ?: franchises.first()
            else -> franchises.randomOrNull(random) ?: sortedByLowestPayroll.first()
        }

        val player = when (archetype) {
            CrisisArchetype.SUPERSTAR_REVOLT ->
                team.roster.maxByOrNull { it.overallRating }
            CrisisArchetype.SPORTSBOOK_INTEGRITY, CrisisArchetype.CONCUSSION_SAFETY ->
                team.roster.filter { !it.isUnavailableForGame }.sortedByDescending { it.overallRating }.drop(1).firstOrNull()
                    ?: team.roster.firstOrNull()
            else -> team.roster.firstOrNull()
        }
        return team to player
    }

    private fun openingNodeIdFor(archetype: CrisisArchetype): String = when (archetype) {
        CrisisArchetype.SPORTSBOOK_INTEGRITY -> "arc_sportsbook_s1"
        CrisisArchetype.ARENA_RELOCATION -> "arc_relocation_s1"
        CrisisArchetype.SPONSOR_MORALITY -> "arc_sponsor_s1"
        CrisisArchetype.SUPERSTAR_REVOLT -> "arc_revolt_s1"
        CrisisArchetype.CYBER_RANSOMWARE -> "arc_cyber_s1"
        CrisisArchetype.OWNERSHIP_FRAUD -> "arc_fraud_s1"
        CrisisArchetype.CONCUSSION_SAFETY -> "arc_safety_s1"
        CrisisArchetype.LABOR_OFFICIAL_SICKOUT -> "arc_sickout_s1"
    }

    private fun materializeBranchNode(
        branch: ScheduledCrisisBranch,
        sportType: SportType,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        cities: List<MarketCity>,
        currentWeek: Int
    ): CommissionerDilemma {
        val team = branch.targetFranchiseId?.let { tid -> franchises.firstOrNull { it.id == tid } }
            ?: franchises.firstOrNull()
        val player = branch.targetPlayerId?.let { pid -> team?.roster?.firstOrNull { it.id == pid } }
            ?: team?.roster?.firstOrNull()
        return buildNodeDilemma(
            nodeId = branch.followUpNodeId,
            arcId = branch.arcId,
            archetype = branch.archetype,
            sportType = sportType,
            franchises = franchises,
            activeBroadcast = activeBroadcast,
            activeSponsorship = activeSponsorship,
            cities = cities,
            week = currentWeek,
            targetFranchise = team,
            targetPlayer = player,
            priorDecisionLabel = branch.priorDecisionLabel,
            isBranchFollowUp = true
        )
    }

    fun applyChoiceConsequences(
        crisisState: CrisisSystemState,
        dilemma: CommissionerDilemma,
        choiceIndex: Int,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement,
        officiatingState: OfficiatingState,
        disciplinaryState: DisciplinaryDeskState,
        seasonYear: Int,
        currentWeek: Int,
        regularSeasonWeeks: Int = 14
    ): CrisisResolutionResult {
        val choice = dilemma.choices.getOrElse(choiceIndex) { dilemma.choices.first() }
        val targetTeamId = dilemma.targetFranchiseId
        val targetPlayerId = dilemma.targetPlayerId

        // 1. Update Franchises (fan morale, owner approval, chemistry, player morale & real game suspensions)
        val updatedFranchises = franchises.map { team ->
            if (targetTeamId != null && team.id == targetTeamId) {
                val updatedRoster = if (targetPlayerId != null && choice.playerSuspensionGames != 0) {
                    team.roster.map { player ->
                        if (player.id == targetPlayerId) {
                            player.copy(
                                suspendedGamesRemaining = if (choice.playerSuspensionGames > 0) {
                                    maxOf(player.suspendedGamesRemaining, choice.playerSuspensionGames)
                                } else {
                                    player.suspendedGamesRemaining
                                }
                            )
                        } else player
                    }
                } else team.roster

                val loyaltyDelta = choice.teamChemistryImpact + (choice.playerMoraleImpact / 2)
                val adjustedTeam = team.copy(
                    roster = updatedRoster,
                    fanMorale = (team.fanMorale + choice.fanMoraleImpact).coerceIn(20, 99),
                    fanLoyalty = (team.fanLoyalty + loyaltyDelta).coerceIn(15, 100),
                    ownership = team.ownership.copy(
                        ownerApproval = (team.ownership.ownerApproval + choice.ownerApprovalImpact).coerceIn(15, 99)
                    )
                )
                if (choice.playerSuspensionGames > 0) {
                    InjuryEngine.autoOptimizeDepthChart(adjustedTeam)
                } else {
                    adjustedTeam
                }
            } else if (choice.fanMoraleImpact != 0 || choice.ownerApprovalImpact != 0) {
                val rippleFan = (choice.fanMoraleImpact / 3)
                val rippleOwner = (choice.ownerApprovalImpact / 3)
                team.copy(
                    fanMorale = (team.fanMorale + rippleFan).coerceIn(25, 99),
                    ownership = team.ownership.copy(
                        ownerApproval = (team.ownership.ownerApproval + rippleOwner).coerceIn(20, 99)
                    )
                )
            } else team
        }

        // 2. Union Agreement
        val newUnionSat = (unionAgreement.unionSatisfaction + choice.unionSatisfactionImpact).coerceIn(10, 100)
        val newStrikeRisk = (100 - newUnionSat).coerceIn(0, 95)
        val updatedUnion = unionAgreement.copy(
            unionSatisfaction = newUnionSat,
            strikeRisk = newStrikeRisk
        )

        // 3. Officiating State
        val updatedOfficiating = if (choice.officiatingAccuracyImpact != 0 || choice.refLockoutRiskImpact != 0) {
            officiatingState.copy(
                leagueCallAccuracyPct = Math.round(
                    (officiatingState.leagueCallAccuracyPct + choice.officiatingAccuracyImpact * 0.4).coerceIn(80.0, 99.5) * 10.0
                ) / 10.0,
                unionState = officiatingState.unionState.copy(
                    lockoutRisk = (officiatingState.unionState.lockoutRisk + choice.refLockoutRiskImpact).coerceIn(0, 95)
                )
            )
        } else officiatingState

        // 4. Disciplinary Consistency
        val updatedDisciplinary = if (choice.precedentConsistencyImpact != 0) {
            disciplinaryState.copy(
                precedentConsistencyScore = (disciplinaryState.precedentConsistencyScore + choice.precedentConsistencyImpact)
                    .coerceIn(20, 100)
            )
        } else disciplinaryState

        // 5. Schedule follow-up branch if this decision opens Act II / Act III
        var nextScheduledBranches = crisisState.scheduledBranches
        var nextCompletedArcs = crisisState.completedArcIds
        var spawnedBranch: ScheduledCrisisBranch? = null

        if (choice.branchFollowUp != null) {
            val rawTriggerWeek = currentWeek + choice.branchFollowUp.delayWeeks
            val (triggerYear, triggerWeek) = if (rawTriggerWeek > regularSeasonWeeks) {
                (seasonYear + 1) to ((rawTriggerWeek - regularSeasonWeeks).coerceIn(2, regularSeasonWeeks))
            } else {
                seasonYear to rawTriggerWeek
            }
            val branch = ScheduledCrisisBranch(
                id = "branch_${dilemma.arcId}_${choice.branchFollowUp.followUpNodeId}",
                arcId = dilemma.arcId,
                archetype = dilemma.archetype,
                followUpNodeId = choice.branchFollowUp.followUpNodeId,
                triggerYear = triggerYear,
                triggerWeek = triggerWeek,
                stageNumber = dilemma.stageNumber + 1,
                totalStages = maxOf(dilemma.totalStages, dilemma.stageNumber + 1),
                teaserLabel = choice.branchFollowUp.teaserLabel,
                priorDecisionLabel = choice.label,
                targetFranchiseId = dilemma.targetFranchiseId,
                targetFranchiseName = dilemma.targetFranchiseName,
                targetPlayerId = dilemma.targetPlayerId,
                targetPlayerName = dilemma.targetPlayerName
            )
            nextScheduledBranches = (nextScheduledBranches + branch).take(12)
            spawnedBranch = branch
        } else {
            nextCompletedArcs = nextCompletedArcs + dilemma.arcId
        }

        // 6. Log to Consequence History Ledger
        val tags = buildList {
            if (choice.cashImpactMillions != 0L) add("${if (choice.cashImpactMillions > 0) "+" else ""}\$${choice.cashImpactMillions}M Cash")
            if (choice.valuationImpactMillions != 0L) add("${if (choice.valuationImpactMillions > 0) "+" else ""}\$${choice.valuationImpactMillions}M Val")
            if (choice.fanbaseImpactMillions != 0.0) {
                val p = if (choice.fanbaseImpactMillions > 0) "+" else ""
                add("$p${choice.fanbaseImpactMillions}M Fans")
            }
            if (choice.integrityImpact != 0) add("${if (choice.integrityImpact > 0) "+" else ""}${choice.integrityImpact} Integrity")
            if (choice.unionSatisfactionImpact != 0) add("${if (choice.unionSatisfactionImpact > 0) "+" else ""}${choice.unionSatisfactionImpact} Union")
            if (choice.ownerApprovalImpact != 0) add("${if (choice.ownerApprovalImpact > 0) "+" else ""}${choice.ownerApprovalImpact} Owners")
            if (choice.playerSuspensionGames > 0) add("${choice.playerSuspensionGames}G Susp.")
            if (choice.officiatingAccuracyImpact != 0) add("${if (choice.officiatingAccuracyImpact > 0) "+" else ""}${choice.officiatingAccuracyImpact} Ref Acc.")
        }
        val entityLabel = listOfNotNull(dilemma.targetPlayerName, dilemma.targetFranchiseName)
            .joinToString(" • ")

        val historyRecord = ResolvedCrisisRecord(
            id = "hist_${dilemma.nodeId}_y${seasonYear}_w$currentWeek",
            arcId = dilemma.arcId,
            archetype = dilemma.archetype,
            stageNumber = dilemma.stageNumber,
            totalStages = dilemma.totalStages,
            seasonYear = seasonYear,
            weekNumber = currentWeek,
            title = dilemma.title,
            chosenLabel = choice.label,
            narrativeOutcome = choice.narrativeOutcome,
            consequenceTags = tags,
            affectedEntityLabel = entityLabel,
            spawnedBranchTeaser = spawnedBranch?.let {
                "${it.teaserLabel} (Est. Y${it.triggerYear} W${it.triggerWeek})"
            }
        )

        val nextCrisisState = crisisState.copy(
            scheduledBranches = nextScheduledBranches,
            resolvedHistory = (listOf(historyRecord) + crisisState.resolvedHistory).take(40),
            completedArcIds = nextCompletedArcs
        )

        val stageSuffix = if (dilemma.totalStages > 1) " [Stage ${dilemma.stageNumber}/${dilemma.totalStages}]" else ""
        val headline = "CRISIS RULING$stageSuffix — ${dilemma.title}: ${choice.narrativeOutcome}"

        return CrisisResolutionResult(
            updatedCrisisState = nextCrisisState,
            updatedFranchises = updatedFranchises,
            updatedUnion = updatedUnion,
            updatedOfficiatingState = updatedOfficiating,
            updatedDisciplinaryState = updatedDisciplinary,
            cashDeltaMillions = choice.cashImpactMillions,
            valuationDeltaMillions = choice.valuationImpactMillions,
            fanbaseDeltaMillions = choice.fanbaseImpactMillions,
            integrityDelta = choice.integrityImpact,
            parityDelta = choice.parityImpact,
            ownerApprovalDelta = choice.ownerApprovalImpact,
            headline = headline,
            spawnedBranch = spawnedBranch
        )
    }

    fun computeThreatRadar(
        integrityScore: Int,
        unionAgreement: LaborUnionAgreement,
        officiatingState: OfficiatingState,
        franchises: List<Franchise>,
        disciplinaryState: DisciplinaryDeskState
    ): List<CrisisThreatMetric> {
        val avgOwnerApproval = franchises.map { it.ownership.ownerApproval }.average().takeIf { !it.isNaN() } ?: 75.0
        val avgMorale = franchises.map { it.fanMorale }.average().takeIf { !it.isNaN() } ?: 75.0
        val integrityRisk = (100 - integrityScore + (100 - disciplinaryState.precedentConsistencyScore) / 3).coerceIn(8, 95)
        val laborRisk = ((unionAgreement.strikeRisk * 0.7) + ((100 - unionAgreement.unionSatisfaction) * 0.3)).toInt().coerceIn(8, 95)
        val ownershipRisk = ((100 - avgOwnerApproval) * 0.8 + (100 - avgMorale) * 0.2).toInt().coerceIn(8, 95)
        val operationsRisk = ((officiatingState.unionState.lockoutRisk * 0.6) + ((100.0 - officiatingState.leagueCallAccuracyPct) * 3.0)).toInt().coerceIn(8, 95)

        return listOf(
            CrisisThreatMetric(
                key = "radar_integrity",
                title = "Wagering, Fraud & Integrity Exposure",
                riskScore = integrityRisk,
                driverSummary = "League Integrity $integrityScore/100 • Precedent Consistency ${disciplinaryState.precedentConsistencyScore}/100",
                accentHex = if (integrityRisk >= 55) 0xFFEF4444 else 0xFFF59E0B
            ),
            CrisisThreatMetric(
                key = "radar_labor",
                title = "Player Safety & Labor Unrest",
                riskScore = laborRisk,
                driverSummary = "Strike Risk ${unionAgreement.strikeRisk}% • Union Satisfaction ${unionAgreement.unionSatisfaction}/100",
                accentHex = if (laborRisk >= 55) 0xFFEF4444 else 0xFF8B5CF6
            ),
            CrisisThreatMetric(
                key = "radar_ownership",
                title = "Franchise Lease & Ownership Stability",
                riskScore = ownershipRisk,
                driverSummary = "Avg Owner Approval ${avgOwnerApproval.toInt()}% • Fan Morale ${avgMorale.toInt()}%",
                accentHex = if (ownershipRisk >= 55) 0xFFF59E0B else 0xFF10B981
            ),
            CrisisThreatMetric(
                key = "radar_operations",
                title = "Broadcast, Cyber & Officiating Resilience",
                riskScore = operationsRisk,
                driverSummary = "Ref Call Accuracy ${officiatingState.leagueCallAccuracyPct}% • Ref Lockout Risk ${officiatingState.unionState.lockoutRisk}%",
                accentHex = if (operationsRisk >= 55) 0xFFEF4444 else 0xFF06B6D4
            )
        )
    }

    private fun buildNodeDilemma(
        nodeId: String,
        arcId: String,
        archetype: CrisisArchetype,
        sportType: SportType,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        activeSponsorship: ActiveLeagueSponsorship,
        cities: List<MarketCity>,
        week: Int,
        targetFranchise: Franchise?,
        targetPlayer: RosterPlayer?,
        priorDecisionLabel: String?,
        isBranchFollowUp: Boolean
    ): CommissionerDilemma {
        val team = targetFranchise ?: franchises.first()
        val teamName = team.teamFullName
        val city = team.cityName
        val player = targetPlayer ?: team.roster.firstOrNull()
        val playerName = player?.name ?: "Veteran Starter"
        val playerPos = player?.position ?: "Star"
        val networkName = activeBroadcast.partnerName
        val sponsorName = activeSponsorship.corporationName
        val expansionCity = cities.firstOrNull { !it.hasFranchise }?.name ?: "Seattle"
        val sportNoun = when (sportType) {
            SportType.BASKETBALL -> "hardwood"
            SportType.FOOTBALL -> "gridiron"
            SportType.SOCCER -> "pitch"
            SportType.HOCKEY -> "ice"
        }

        return when (nodeId) {
            // ==================== ARC 1: SPORTSBOOK INTEGRITY ====================
            "arc_sportsbook_s1" -> CommissionerDilemma(
                id = "dil_${arcId}_s1",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 1,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "Offshore Parlay Ring Tied to $teamName",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 1 • INTEGRITY ALERT",
                description = "Sportsbook integrity monitors detected unusual micro-betting spikes involving $playerName ($playerPos, $teamName) prop lines seconds before injury updates became public. Investigators need immediate direction ahead of $networkName's weekend broadcast.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                choices = listOf(
                    CommissionerChoice(
                        id = "suspend_audit",
                        label = "Suspend $playerName (5G) & Launch Federal Audit",
                        narrativeOutcome = "Player pulled off active roster immediately as federal monitors subpoena device logs.",
                        cashImpactMillions = -3L,
                        fanMoraleImpact = -4,
                        unionSatisfactionImpact = -9,
                        integrityImpact = +14,
                        ownerApprovalImpact = -6,
                        playerSuspensionGames = 5,
                        precedentConsistencyImpact = +5,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_sportsbook_s2_audit",
                            delayWeeks = 2,
                            teaserLabel = "Federal Audit Subpoena Findings"
                        )
                    ),
                    CommissionerChoice(
                        id = "quiet_fine",
                        label = "Issue Quiet Compliance Fine & Seal File",
                        narrativeOutcome = "Keeps $playerName in uniform for TV ratings, but compliance officers warn the cover-up is volatile.",
                        cashImpactMillions = +2L,
                        fanMoraleImpact = +1,
                        unionSatisfactionImpact = +4,
                        integrityImpact = -14,
                        ownerApprovalImpact = +9,
                        precedentConsistencyImpact = -8,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_sportsbook_s2_leak",
                            delayWeeks = 2,
                            teaserLabel = "Whistleblower Leaks Sealed File"
                        )
                    ),
                    CommissionerChoice(
                        id = "joint_taskforce",
                        label = "Convene Joint League-Union Monitoring Panel",
                        narrativeOutcome = "Players Association co-signs real-time geofencing alerts without a hasty ban.",
                        cashImpactMillions = -2L,
                        fanMoraleImpact = +3,
                        unionSatisfactionImpact = +8,
                        integrityImpact = +6,
                        ownerApprovalImpact = +2,
                        precedentConsistencyImpact = +3,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_sportsbook_s2_tribunal",
                            delayWeeks = 2,
                            teaserLabel = "Joint Panel Final Recommendation"
                        )
                    )
                )
            )
            "arc_sportsbook_s2_audit" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_audit",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "Audit Uncovers Second Leak Source Inside $city",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • AUDIT ESCALATION",
                description = "Your federal audit worked: investigators cleared $playerName of point-shaving, but proved an assistant trainer on $teamName sold lineup sheets to third-party handicappers.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "publish_full",
                        label = "Publish Unredacted Dossier & Strip $teamName Draft Capital",
                        narrativeOutcome = "Total transparency shocks owners, yet sportsbooks and regulators hail the league as a gold standard.",
                        cashImpactMillions = +4L,
                        valuationImpactMillions = +12L,
                        integrityImpact = +12,
                        ownerApprovalImpact = -10,
                        unionSatisfactionImpact = +6,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_sportsbook_s3_reform",
                            delayWeeks = 2,
                            teaserLabel = "Board Reform Ratification Vote"
                        )
                    ),
                    CommissionerChoice(
                        id = "terminate_trainer_only",
                        label = "Ban Assistant Staffer Quietly & Reinstate $playerName",
                        narrativeOutcome = "Contains organizational embarrassment for $teamName while closing the gambling probe cleanly.",
                        cashImpactMillions = 0L,
                        integrityImpact = +4,
                        ownerApprovalImpact = +8,
                        fanMoraleImpact = +4
                    )
                )
            )
            "arc_sportsbook_s3_reform" -> CommissionerDilemma(
                id = "dil_${arcId}_s3_reform",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 3,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "Anti-Corruption Firewall Charter Act",
                category = archetype.categoryLabel,
                urgencyTag = "FINAL ACT • REFORM VOTE",
                description = "After your unredacted integrity report, $sponsorName and $networkName offer a permanent compliance bonus if the league adopts encrypted biometric injury reporting.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "adopt_firewall",
                        label = "Mandate League-Wide Biometric Injury Portal (-$5M)",
                        narrativeOutcome = "Firewall seals every locker room against tipsters and unlocks a long-term valuation surge!",
                        cashImpactMillions = -5L,
                        valuationImpactMillions = +24L,
                        integrityImpact = +10,
                        fanbaseImpactMillions = +0.6,
                        precedentConsistencyImpact = +6
                    ),
                    CommissionerChoice(
                        id = "voluntary_opt_in",
                        label = "Make Compliance Portal Optional for Clubs",
                        narrativeOutcome = "Saves immediate cap spending, though regulators note uneven enforcement across markets.",
                        cashImpactMillions = +3L,
                        ownerApprovalImpact = +7,
                        integrityImpact = -3
                    )
                )
            )
            "arc_sportsbook_s2_leak" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_leak",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Investigative Outlet Publishes Sealed $city Betting File!",
                category = archetype.categoryLabel,
                urgencyTag = "BRANCH FALLOUT • COVER-UP LEAK",
                description = "Because you sealed the initial $playerName probe, a whistleblower handed the redacted compliance memo to national press! Fans and lawmakers demand immediate accountability.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "emergency_ban_apology",
                        label = "Impose Late 8-Game Suspension & $6M League Restitution",
                        narrativeOutcome = "Painful public humility stabilizes sponsors, though trust suffered a major bruise.",
                        cashImpactMillions = -6L,
                        fanMoraleImpact = -6,
                        integrityImpact = +10,
                        unionSatisfactionImpact = -8,
                        playerSuspensionGames = 8,
                        valuationImpactMillions = -10L
                    ),
                    CommissionerChoice(
                        id = "stonewall_press",
                        label = "Dismiss Leaks & Subpoena Informants",
                        narrativeOutcome = "Adversarial stance turns media headlines toxic and scares away blue-chip advertisers.",
                        cashImpactMillions = -12L,
                        fanMoraleImpact = -14,
                        fanbaseImpactMillions = -0.7,
                        integrityImpact = -18,
                        valuationImpactMillions = -22L
                    )
                )
            )
            "arc_sportsbook_s2_tribunal" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_tribunal",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Joint League-Union Tribunal Verdict on $playerName",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 • TRIBUNAL RULING",
                description = "Your bipartisan taskforce traced an acquaintance betting on $playerName props after casual text exchanges. The panel delivers final sentencing options.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "standard_3g",
                        label = "Enforce Balanced 3-Game Ban & Mandatory Education",
                        narrativeOutcome = "Fair, transparent precedent satisfies both sportsbook partners and the Players Association.",
                        cashImpactMillions = +1L,
                        integrityImpact = +10,
                        unionSatisfactionImpact = +4,
                        playerSuspensionGames = 3,
                        precedentConsistencyImpact = +6
                    ),
                    CommissionerChoice(
                        id = "probation_fine",
                        label = "Probation Only ($750K Fine, 0 Games)",
                        narrativeOutcome = "Star stays active for the playoff push, though rival GMs murmur about soft enforcement.",
                        cashImpactMillions = +2L,
                        ownerApprovalImpact = +6,
                        integrityImpact = -4,
                        precedentConsistencyImpact = -3
                    )
                )
            )

            // ==================== ARC 2: ARENA RELOCATION ULTIMATUM ====================
            "arc_relocation_s1" -> CommissionerDilemma(
                id = "dil_${arcId}_s1",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 1,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "$teamName Owner Threatens $expansionCity Relocation",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 1 • LEASE ULTIMATUM",
                description = "Controlling ownership in $city issued an ultimatum: approve a league-backed $22M arena renovation package this week or they will invoke an exit clause to explore moving $teamName to $expansionCity.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                choices = listOf(
                    CommissionerChoice(
                        id = "cofund_arena",
                        label = "Approve $20M League Renovation Loan & Lease Lock",
                        narrativeOutcome = "Secures $teamName in $city under an airtight 15-year non-relocation covenant.",
                        cashImpactMillions = -20L,
                        fanMoraleImpact = +14,
                        ownerApprovalImpact = +12,
                        integrityImpact = +4,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_relocation_s2_rebate",
                            delayWeeks = 2,
                            teaserLabel = "Small-Market Revenue Sharing Demand"
                        )
                    ),
                    CommissionerChoice(
                        id = "enforce_lease",
                        label = "Block Relocation Talks & Threaten Charter Forfeiture",
                        narrativeOutcome = "Protects local fans in $city, infuriating ownership who vows an antitrust injunction.",
                        cashImpactMillions = 0L,
                        fanMoraleImpact = +10,
                        ownerApprovalImpact = -15,
                        integrityImpact = +12,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_relocation_s2_lawsuit",
                            delayWeeks = 2,
                            teaserLabel = "Ownership Federal Antitrust Filing"
                        )
                    ),
                    CommissionerChoice(
                        id = "approve_exploratory",
                        label = "Permit $expansionCity Exploratory Bid ($14M Relocation Fee)",
                        narrativeOutcome = "Brings immediate fee revenue into treasury, but furious fans boycott remaining home games in $city.",
                        cashImpactMillions = +14L,
                        fanMoraleImpact = -20,
                        fanbaseImpactMillions = -0.6,
                        ownerApprovalImpact = +14,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_relocation_s2_bids",
                            delayWeeks = 2,
                            teaserLabel = "$city Home Fan Boycott & Rival Bid"
                        )
                    )
                )
            )
            "arc_relocation_s2_rebate" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_rebate",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Small-Market Clubs Demand Equal Arena Subsidy Rebates",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • SUBSIDY PRECEDENT",
                description = "After seeing your $20M arena loan for $teamName, three other mid-market owners demand matching facility infrastructure rebates.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "facility_pool",
                        label = "Launch Shared $12M League Infrastructure Matching Fund",
                        narrativeOutcome = "Stabilizes franchise values league-wide and locks in multi-year arena leases.",
                        cashImpactMillions = -12L,
                        valuationImpactMillions = +20L,
                        ownerApprovalImpact = +15,
                        parityImpact = +5
                    ),
                    CommissionerChoice(
                        id = "deny_rebates",
                        label = "Restrict Subsidy to $city Lease Emergency Only",
                        narrativeOutcome = "Preserves treasury cash reserves, leaving several mid-market owners frustrated.",
                        cashImpactMillions = 0L,
                        ownerApprovalImpact = -8,
                        integrityImpact = +4
                    )
                )
            )
            "arc_relocation_s2_lawsuit" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_lawsuit",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "$teamName Ownership Files Federal Restraint-of-Trade Suit",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • COURT INJUNCTION",
                description = "$teamName ownership followed through after you blocked talks: their counsel filed for summary discovery against league charter relocation bylaws.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "stand_firm",
                        label = "Counter-Sue for Charter Breach (-$6M Legal Escrow)",
                        narrativeOutcome = "Board rallies behind charter sovereignty while municipal leaders back you in court.",
                        cashImpactMillions = -6L,
                        integrityImpact = +10,
                        ownerApprovalImpact = +6,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_relocation_s3_settlement",
                            delayWeeks = 2,
                            teaserLabel = "Binding Arbitration Lease Verdict"
                        )
                    ),
                    CommissionerChoice(
                        id = "buyout_owner",
                        label = "Broker Local Minority Ownership Buyout (-$14M Bridge)",
                        narrativeOutcome = "Replaces hostile governor with civic coalition ownership, keeping $teamName rooted in $city.",
                        cashImpactMillions = -14L,
                        valuationImpactMillions = +16L,
                        fanMoraleImpact = +16,
                        ownerApprovalImpact = +10
                    )
                )
            )
            "arc_relocation_s3_settlement" -> CommissionerDilemma(
                id = "dil_${arcId}_s3_settlement",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 3,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "Federal Arbitrator Backs Charter Sovereignty Over $teamName",
                category = archetype.categoryLabel,
                urgencyTag = "FINAL ACT • ARBITRATION TRIUMPH",
                description = "A federal panel upheld league relocation bylaws! Ownership now offers to drop appeals if granted tax-exempt naming rights concessions in $city.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "grant_naming_concession",
                        label = "Settle: Enforce Lease + Grant Arena Naming Partner Share",
                        narrativeOutcome = "Case dismissed with prejudice; $teamName sells out home dates behind surging local pride.",
                        cashImpactMillions = +6L,
                        valuationImpactMillions = +18L,
                        fanMoraleImpact = +12,
                        integrityImpact = +8
                    ),
                    CommissionerChoice(
                        id = "recover_legal_fees",
                        label = "Demand Full $5M Legal Fee Recovery Without Concessions",
                        narrativeOutcome = "Enforces strict authority and recovers treasury costs.",
                        cashImpactMillions = +5L,
                        integrityImpact = +10,
                        ownerApprovalImpact = -4
                    )
                )
            )
            "arc_relocation_s2_bids" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_bids",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "$city Fans Stage Attendance Blackout Over Relocation Talks",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • FAN REVOLT",
                description = "Home attendance in $city plummeted 40% during protests! Meanwhile, $expansionCity's civic group tabled a guaranteed stadium package.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "halt_and_apologize",
                        label = "Halt Move & Launch $city Fan Appreciation Ticket Relief",
                        narrativeOutcome = "Restores partial goodwill in $city while preserving future expansion fees for clean new franchises.",
                        cashImpactMillions = -5L,
                        fanMoraleImpact = +14,
                        ownerApprovalImpact = -6
                    ),
                    CommissionerChoice(
                        id = "collect_full_exit_fee",
                        label = "Greenlight Conditional Move & Collect $28M Exit Assessment",
                        narrativeOutcome = "Massive cash infusion boosts league valuation even as $city mourns lost heritage.",
                        cashImpactMillions = +28L,
                        valuationImpactMillions = +22L,
                        fanbaseImpactMillions = -0.4,
                        integrityImpact = -6
                    )
                )
            )

            // ==================== ARC 3: SPONSOR BOYCOTT & MORALITY CLAUSE ====================
            "arc_sponsor_s1" -> CommissionerDilemma(
                id = "dil_${arcId}_s1",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 1,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Hot-Mic Controversy Sparks $sponsorName Pullout Threat",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 1 • COMMERCIAL CRISIS",
                description = "During $networkName's primetime $sportNoun broadcast of $teamName, an unmuted sideline clip went viral overnight. Title partner $sponsorName gave 24 hours before freezing quarterly sponsorship payouts.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                choices = listOf(
                    CommissionerChoice(
                        id = "apology_grant",
                        label = "Issue League Apology & $6M Youth Community Initiative",
                        narrativeOutcome = "Swift civic action cools social backlash and keeps $sponsorName branding on center stage.",
                        cashImpactMillions = -6L,
                        fanMoraleImpact = +8,
                        integrityImpact = +8,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_sponsor_s2_relaunch",
                            delayWeeks = 2,
                            teaserLabel = "Joint Community Broadcast Campaign"
                        )
                    ),
                    CommissionerChoice(
                        id = "suspend_talent",
                        label = "Fine $teamName $2M & Enforce Broadcast Production Control",
                        narrativeOutcome = "$sponsorName stays, though broadcasters and coaches criticize heavy-handed censorship.",
                        cashImpactMillions = +2L,
                        integrityImpact = +5,
                        unionSatisfactionImpact = -7,
                        teamChemistryImpact = -6,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_sponsor_s2_ratings",
                            delayWeeks = 2,
                            teaserLabel = "Broadcast Talent Grievance & Audience Shift"
                        )
                    ),
                    CommissionerChoice(
                        id = "defend_editorial",
                        label = "Reject Sponsor Ultimatum & Risk $sponsorName Clause",
                        narrativeOutcome = "Defends authentic live sports audio, triggering a high-stakes commercial standoff.",
                        cashImpactMillions = -10L,
                        unionSatisfactionImpact = +10,
                        ownerApprovalImpact = -12,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_sponsor_s2_walkout",
                            delayWeeks = 2,
                            teaserLabel = "Competing Title Sponsor Replacement Bid"
                        )
                    )
                )
            )
            "arc_sponsor_s2_relaunch" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_relaunch",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "$sponsorName Proposes Extended Cause-Marketing Renewal",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • COMMERCIAL REBOUND",
                description = "Your youth initiative reversed brand sentiment! $sponsorName wants to deepen their league partnership ahead of playoff programming.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "sign_renewal",
                        label = "Ratify Expanded Sponsor Package (+$16M Cash)",
                        narrativeOutcome = "Turns crisis into a major commercial win for all franchises.",
                        cashImpactMillions = +16L,
                        valuationImpactMillions = +15L,
                        fanbaseImpactMillions = +0.5,
                        ownerApprovalImpact = +12
                    ),
                    CommissionerChoice(
                        id = "keep_terms",
                        label = "Retain Standard Contract & Cap Commercial Slots",
                        narrativeOutcome = "Keeps broadcast pacing tight for core fans while honoring the community pledge.",
                        cashImpactMillions = +5L,
                        fanMoraleImpact = +6
                    )
                )
            )
            "arc_sponsor_s2_ratings" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_ratings",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Live Tape-Delay Mandate Triggers Crew & Fan Debate",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • PRODUCTION BACKLASH",
                description = "$networkName producers warn that your new 15-second broadcast control delay is stripping raw emotion from $teamName telecasts.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "restore_live_audio",
                        label = "Lift Tape-Delay & Establish Clear Mic Zones",
                        narrativeOutcome = "Balanced policy restores ratings momentum without risking sponsor opt-outs.",
                        fanMoraleImpact = +6,
                        unionSatisfactionImpact = +5,
                        valuationImpactMillions = +6L
                    ),
                    CommissionerChoice(
                        id = "keep_strict_delay",
                        label = "Maintain Zero-Risk Sanitized Feed",
                        narrativeOutcome = "Sponsors stay safe, though ratings dip slightly among core fans.",
                        integrityImpact = +4,
                        fanMoraleImpact = -5,
                        ownerApprovalImpact = +4
                    )
                )
            )
            "arc_sponsor_s2_walkout" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_walkout",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "$sponsorName Exits — Rival Fintech Bids for Patch Rights",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • REPLACEMENT AUCTION",
                description = "$sponsorName formally exited after your refusal to apologize, leaving prime broadcast inventory open while two aggressive brands bid.",
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "high_bid_fintech",
                        label = "Award Emergency Replacement Deal (+$14M, Mixed Integrity)",
                        narrativeOutcome = "Recovers lost treasury revenue quickly, though consumer groups scrutinize the new partner.",
                        cashImpactMillions = +14L,
                        integrityImpact = -5,
                        ownerApprovalImpact = +10
                    ),
                    CommissionerChoice(
                        id = "clean_regional_coalition",
                        label = "Assemble Vetted Blue-Chip Brand Coalition (+$8M)",
                        narrativeOutcome = "Lower short-term payout, yet rebuilds gold-standard commercial credibility.",
                        cashImpactMillions = +8L,
                        integrityImpact = +8,
                        valuationImpactMillions = +8L
                    )
                )
            )

            // ==================== ARC 4: SUPERSTAR REVOLT & MEDICAL TRUST ====================
            "arc_revolt_s1" -> CommissionerDilemma(
                id = "dil_${arcId}_s1",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 1,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "$playerName Leads Public Medical Revolt Against $teamName",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 1 • STAR PLAYER CRISIS",
                description = "All-League cornerstone $playerName ($playerPos) skipped $teamName shootaround and accused club trainers of pressuring injured stars back prematurely before $networkName showcases.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                choices = listOf(
                    CommissionerChoice(
                        id = "independent_medical",
                        label = "Mandate Independent League Physicians (-$5M Medical Audit)",
                        narrativeOutcome = "Strips clubs of unilateral injury clearance; Players Union cheers historic player-first reform.",
                        cashImpactMillions = -5L,
                        unionSatisfactionImpact = +14,
                        integrityImpact = +10,
                        ownerApprovalImpact = -8,
                        playerMoraleImpact = +14,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_revolt_s2_accord",
                            delayWeeks = 2,
                            teaserLabel = "Club Medical Staff Compliance Audit"
                        )
                    ),
                    CommissionerChoice(
                        id = "back_ownership",
                        label = "Back $teamName Front Office & Fine $playerName ($250K)",
                        narrativeOutcome = "Asserts contractual attendance rules, sparking immediate locker room alienation.",
                        cashImpactMillions = +1L,
                        ownerApprovalImpact = +12,
                        unionSatisfactionImpact = -14,
                        teamChemistryImpact = -14,
                        playerMoraleImpact = -16,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_revolt_s2_holdout",
                            delayWeeks = 2,
                            teaserLabel = "$playerName Indefinite Trade Sit-Out"
                        )
                    ),
                    CommissionerChoice(
                        id = "emergency_summit",
                        label = "Mediate Closed-Door Joint Settlement (-$2M)",
                        narrativeOutcome = "Restores $teamName chemistry through shared injury workload limits without league upheaval.",
                        cashImpactMillions = -2L,
                        teamChemistryImpact = +6,
                        unionSatisfactionImpact = +5,
                        ownerApprovalImpact = +4,
                        parityImpact = +3
                    )
                )
            )
            "arc_revolt_s2_accord" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_accord",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Medical Audit Uncovers Altered Clearance Logs on $teamName",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • AUDIT FINDINGS",
                description = "Your independent physicians discovered $teamName trainers downplayed two strain reports! $playerName agrees to return immediately if accountability follows.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "fine_team_medical",
                        label = "Levy $4M Medical Violation Fine & Guarantee CBA Peace",
                        narrativeOutcome = "$playerName returns energized as league union trust skyrockets.",
                        cashImpactMillions = +4L,
                        integrityImpact = +10,
                        unionSatisfactionImpact = +10,
                        teamChemistryImpact = +10,
                        playerMoraleImpact = +12
                    ),
                    CommissionerChoice(
                        id = "reprimand_quietly",
                        label = "Issue Private Club Reprimand & Seal Medical Records",
                        narrativeOutcome = "De-escalates owner friction while keeping the player in uniform.",
                        ownerApprovalImpact = +8,
                        integrityImpact = -5,
                        unionSatisfactionImpact = -4
                    )
                )
            )
            "arc_revolt_s2_holdout" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_holdout",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "$playerName Enters Full Sit-Out Demanding Trade From $city",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • HOLD-OUT ESCALATION",
                description = "Furious over the fine, $playerName refused to suit up for $teamName! National sponsors ask why a marquee attraction is off the $sportNoun.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "fine_holdout",
                        label = "Suspend Without Pay (3G) Until Contract Honored",
                        narrativeOutcome = "Hardline enforcement halts copycat holdouts, though $teamName tumbles without its centerpiece.",
                        ownerApprovalImpact = +10,
                        unionSatisfactionImpact = -12,
                        fanMoraleImpact = -10,
                        playerSuspensionGames = 3,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_revolt_s3_trade",
                            delayWeeks = 2,
                            teaserLabel = "Expedited Blockbuster Exit Deal"
                        )
                    ),
                    CommissionerChoice(
                        id = "mediate_exit",
                        label = "Appoint Neutral Arbitrator to Guarantee Clean Clearance",
                        narrativeOutcome = "Brings $playerName back to the lineup safely while sparing further season disruption.",
                        cashImpactMillions = -2L,
                        unionSatisfactionImpact = +8,
                        teamChemistryImpact = +6,
                        fanMoraleImpact = +6
                    )
                )
            )
            "arc_revolt_s3_trade" -> CommissionerDilemma(
                id = "dil_${arcId}_s3_trade",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 3,
                totalStages = 3,
                nodeId = nodeId,
                week = week,
                title = "League Brokers Truce Between $playerName & $teamName",
                category = archetype.categoryLabel,
                urgencyTag = "FINAL ACT • CONTRACT RESOLUTION",
                description = "After weeks of national tension, $teamName ownership and $playerName's camp agree to accept Commissioner-guided terms to finish the season.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "structured_return",
                        label = "Install Neutral Injury Monitor & Expunge Escalating Fines",
                        narrativeOutcome = "$playerName suits up down the stretch; drama boosts $networkName ratings.",
                        fanMoraleImpact = +8,
                        unionSatisfactionImpact = +8,
                        teamChemistryImpact = +8,
                        valuationImpactMillions = +10L
                    ),
                    CommissionerChoice(
                        id = "no_concession_return",
                        label = "Enforce Full Probation Conditions Through Playoffs",
                        narrativeOutcome = "Ownership retains strict disciplinary authority.",
                        integrityImpact = +6,
                        ownerApprovalImpact = +8,
                        unionSatisfactionImpact = -6
                    )
                )
            )

            // ==================== ARC 5: CYBER RANSOMWARE ====================
            "arc_cyber_s1" -> CommissionerDilemma(
                id = "dil_${arcId}_s1",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 1,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Ransomware Locks $teamName Arena Gates & Streaming Auth",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 1 • CYBER EMERGENCY",
                description = "Hours before marquee weekend action, threat actors encrypted ticket scanners and $networkName verification servers, demanding $9M or promising leaked fan payment credentials.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                choices = listOf(
                    CommissionerChoice(
                        id = "open_free_gates",
                        label = "Refuse Ransom, Open Manual Arena Gates (-$10M Gate)",
                        narrativeOutcome = "Fans swarm packed arenas via paper vouchers! Federal cyber experts praise refusal to fund hackers.",
                        cashImpactMillions = -10L,
                        fanMoraleImpact = +15,
                        fanbaseImpactMillions = +0.5,
                        integrityImpact = +12,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_cyber_s2_fbi",
                            delayWeeks = 2,
                            teaserLabel = "Zero-Trust Security Overhaul"
                        )
                    ),
                    CommissionerChoice(
                        id = "pay_settlement",
                        label = "Authorize Quiet $9M Decryption Payout",
                        narrativeOutcome = "Games start on time, though intelligence officials warn paying hackers paints a target on league infrastructure.",
                        cashImpactMillions = -9L,
                        integrityImpact = -12,
                        ownerApprovalImpact = +5,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_cyber_s2_extortion",
                            delayWeeks = 2,
                            teaserLabel = "Secondary Extortion Leak Demand"
                        )
                    ),
                    CommissionerChoice(
                        id = "postpone_slate",
                        label = "Postpone Slate 48 Hours & Rebuild Clean Backups (-$5M)",
                        narrativeOutcome = "Broadcasters absorb minor schedule churn while security wipes malware completely.",
                        cashImpactMillions = -5L,
                        integrityImpact = +6,
                        ownerApprovalImpact = -6,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_cyber_s2_makeup",
                            delayWeeks = 2,
                            teaserLabel = "Doubleheader Make-Good Slate"
                        )
                    )
                )
            )
            "arc_cyber_s2_fbi" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_fbi",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Cyber Taskforce Secures Fan Data — Insurance Rebate Offered",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • RECOVERY",
                description = "No fan credit cards were breached after you isolated affected nodes! Insurers offer a steep premium discount if clubs standardize digital ticketing.",
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "adopt_cloud_security",
                        label = "Roll Out Unified League Cyber Shield (-$4M, +$18M Valuation)",
                        narrativeOutcome = "Eliminates future attack vectors and reassures enterprise media partners.",
                        cashImpactMillions = -4L,
                        valuationImpactMillions = +18L,
                        integrityImpact = +8
                    ),
                    CommissionerChoice(
                        id = "claim_insurance_rebate",
                        label = "Pocket $7M Cyber Insurance Recovery",
                        narrativeOutcome = "Replenishes treasury cash rapidly after gate losses.",
                        cashImpactMillions = +7L,
                        ownerApprovalImpact = +8
                    )
                )
            )
            "arc_cyber_s2_extortion" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_extortion",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Hackers Demand Extra $7M Not to Leak VIP Treasury Logs!",
                category = archetype.categoryLabel,
                urgencyTag = "BRANCH FALLOUT • DOUBLE EXTORTION",
                description = "Exactly as security analysts warned: after receiving your first payout, the syndicate returned with copies of $teamName's confidential luxury suite logs.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "hand_over_feds",
                        label = "Disclose Breach Publicly & Call Federal Investigators",
                        narrativeOutcome = "Painful PR disclosure stops ongoing blackmail permanently.",
                        cashImpactMillions = -4L,
                        fanMoraleImpact = -6,
                        integrityImpact = +10,
                        valuationImpactMillions = -8L
                    ),
                    CommissionerChoice(
                        id = "quiet_second_wire",
                        label = "Settle Quietly Again (-$7M, Severe Integrity Risk)",
                        narrativeOutcome = "Buys silence through playoffs at steep treasury and ethical cost.",
                        cashImpactMillions = -7L,
                        integrityImpact = -16,
                        ownerApprovalImpact = -10
                    )
                )
            )
            "arc_cyber_s2_makeup" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_makeup",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Rescheduled Doubleheader Draws Surge $networkName Audience",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • MAKE-GOOD SLATE",
                description = "Your rebuilt backups held strong and $networkName turned the rescheduled games into a primetime twin-bill event!",
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "primetime_showcase",
                        label = "Co-Promote Special Doubleheader Bonus (+$9M TV Split)",
                        narrativeOutcome = "Strong ratings recovery delights owners and restores cash flow.",
                        cashImpactMillions = +9L,
                        fanMoraleImpact = +6,
                        ownerApprovalImpact = +10
                    ),
                    CommissionerChoice(
                        id = "refund_travel_vouchers",
                        label = "Reimburse Out-of-Town Ticket Holders (-$2M, Huge Goodwill)",
                        narrativeOutcome = "Fans rave about accountability.",
                        cashImpactMillions = -2L,
                        fanMoraleImpact = +12,
                        fanbaseImpactMillions = +0.4
                    )
                )
            )

            // ==================== ARC 6: OWNERSHIP FRAUD ====================
            "arc_fraud_s1" -> CommissionerDilemma(
                id = "dil_${arcId}_s1",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 1,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Audit Flags Debt Covenant Fraud Inside $teamName Ownership",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 1 • FINANCIAL AUDIT",
                description = "League credit monitors discovered $teamName's managing partner pledged unapproved franchise equity as collateral for distressed outside real estate loans.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                choices = listOf(
                    CommissionerChoice(
                        id = "freeze_receivership",
                        label = "Freeze Voting Rights & Trigger League Receivership Sale",
                        narrativeOutcome = "Protects league credit rating cleanly, even as current ownership screams overreach.",
                        cashImpactMillions = -4L,
                        integrityImpact = +16,
                        ownerApprovalImpact = -10,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_fraud_s2_auction",
                            delayWeeks = 2,
                            teaserLabel = "Vetted Blue-Chip Consortium Bid"
                        )
                    ),
                    CommissionerChoice(
                        id = "escrow_cure",
                        label = "Grant 30-Day Escrow Cure & $6M Liquidity Bond",
                        narrativeOutcome = "Avoids disruptive mid-season sale if the partner can prove solvency next fortnight.",
                        cashImpactMillions = +6L,
                        integrityImpact = -4,
                        ownerApprovalImpact = +8,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_fraud_s2_verdict",
                            delayWeeks = 2,
                            teaserLabel = "Liquidity Cure Deadline Verdict"
                        )
                    ),
                    CommissionerChoice(
                        id = "shield_brand",
                        label = "Accept Private Paper Waiver & Avoid Public Inquiry",
                        narrativeOutcome = "Avoids short-term lender panic, leaving ticking debt exposure under the surface.",
                        cashImpactMillions = 0L,
                        integrityImpact = -15,
                        ownerApprovalImpact = +10,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_fraud_s2_expose",
                            delayWeeks = 2,
                            teaserLabel = "Creditor Lien Default Shock"
                        )
                    )
                )
            )
            "arc_fraud_s2_auction" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_auction",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Vetted Ownership Consortium Bids Record Sum for $teamName",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • SALE APPROVAL",
                description = "Your swift receivership attracted a deeply capitalized civic investment group ready to stabilize $teamName's balance sheet.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "ratify_sale",
                        label = "Approve Debt-Free Ownership Transfer (+$15M Fee & +$25M Val)",
                        narrativeOutcome = "Record franchise valuation reset lifts credit outlook for the entire league!",
                        cashImpactMillions = +15L,
                        valuationImpactMillions = +25L,
                        ownerApprovalImpact = +14,
                        fanMoraleImpact = +10
                    ),
                    CommissionerChoice(
                        id = "minority_board_oversight",
                        label = "Attach Strict Debt-Ceiling Charter Conditions (+$8M)",
                        narrativeOutcome = "Guarantees long-term fiscal discipline with zero debt risk.",
                        cashImpactMillions = +8L,
                        integrityImpact = +10,
                        valuationImpactMillions = +12L
                    )
                )
            )
            "arc_fraud_s2_verdict" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_verdict",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "$teamName Escrow Deadline Expires Short of Liquidity Target",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • CURE VERDICT",
                description = "The 30-day cure period ended with $teamName's managing partner $18M short. Creditors ask whether the league will step in.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "forfeit_bond_and_sell",
                        label = "Forfeit Bond & Mandate Majority Recapitalization",
                        narrativeOutcome = "New minority partners inject cash and restore league solvency cleanly.",
                        cashImpactMillions = +4L,
                        integrityImpact = +10,
                        valuationImpactMillions = +8L
                    ),
                    CommissionerChoice(
                        id = "extend_bridge",
                        label = "Issue $8M League Emergency Bridge Loan",
                        narrativeOutcome = "Prevents immediate upheaval at risk of owner morale strain.",
                        cashImpactMillions = -8L,
                        ownerApprovalImpact = +6,
                        integrityImpact = -4
                    )
                )
            )
            "arc_fraud_s2_expose" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_expose",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Bankruptcy Filing Freeze Hits $teamName Payroll Accounts!",
                category = archetype.categoryLabel,
                urgencyTag = "BRANCH FALLOUT • INSOLVENCY SHOCK",
                description = "Because the earlier debt flag was waived, external creditors filed an involuntary lien freezing $teamName's biweekly player payroll!",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "cover_payroll",
                        label = "Advance $12M From Treasury & Seize Club Control",
                        narrativeOutcome = "Players receive paychecks on time, averting an emergency strike, at steep cash cost.",
                        cashImpactMillions = -12L,
                        unionSatisfactionImpact = +6,
                        integrityImpact = +6,
                        valuationImpactMillions = -12L
                    ),
                    CommissionerChoice(
                        id = "delay_paycheck",
                        label = "Allow 10-Day Creditor Arbitration Stay",
                        narrativeOutcome = "Delays player paychecks—Union fury and league valuation drop sharply.",
                        unionSatisfactionImpact = -22,
                        integrityImpact = -14,
                        valuationImpactMillions = -20L
                    )
                )
            )

            // ==================== ARC 7: SAFETY PROTOCOL WHISTLEBLOWER ====================
            "arc_safety_s1" -> CommissionerDilemma(
                id = "dil_${arcId}_s1",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 1,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Whistleblower Exposes Missed Concussion Protocol for $playerName",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 1 • PLAYER SAFETY ALERT",
                description = "An independent spotter released sideline audio showing $teamName skipped the mandatory neuro-evaluation after $playerName took a hard $sportNoun collision late in the 4th.",
                targetFranchiseId = team.id,
                targetFranchiseName = teamName,
                targetPlayerId = player?.id,
                targetPlayerName = playerName,
                choices = listOf(
                    CommissionerChoice(
                        id = "spotter_veto",
                        label = "Grant Spotters Absolute Unilateral Removal Power (-$4M)",
                        narrativeOutcome = "Players Union hails real safety authority; coaches grumble over sudden late-game pulls.",
                        cashImpactMillions = -4L,
                        integrityImpact = +15,
                        unionSatisfactionImpact = +12,
                        ownerApprovalImpact = -6,
                        officiatingAccuracyImpact = +4,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_safety_s2_protocol",
                            delayWeeks = 2,
                            teaserLabel = "Spotter Veto Playoff Ratification"
                        )
                    ),
                    CommissionerChoice(
                        id = "fine_club",
                        label = "Fine $teamName $3M & Mandate Trainer Retraining",
                        narrativeOutcome = "Acknowledges the error financially without changing live game control.",
                        cashImpactMillions = +3L,
                        integrityImpact = +4,
                        unionSatisfactionImpact = -4,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_safety_s2_repeat",
                            delayWeeks = 2,
                            teaserLabel = "Second Unchecked Head-Impact Incident"
                        )
                    ),
                    CommissionerChoice(
                        id = "dispute_leak",
                        label = "Dismiss Whistleblower Claim as Protocol Ambiguity",
                        narrativeOutcome = "Protects $teamName from sanctions, sparking outrage among medical associations and the Union.",
                        cashImpactMillions = 0L,
                        integrityImpact = -14,
                        unionSatisfactionImpact = -16,
                        fanMoraleImpact = -8,
                        branchFollowUp = CrisisBranchFollowUpSpec(
                            followUpNodeId = "arc_safety_s2_hearing",
                            delayWeeks = 2,
                            teaserLabel = "Senate & Union Safety Subpoena"
                        )
                    )
                )
            )
            "arc_safety_s2_protocol" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_protocol",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Coaches Push Back After Spotters Pull Two Stars Late",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • SAFETY CHECKPOINT",
                description = "Two tight finishes ended with star players pulled under your new spotter veto rule. Fans ask if evaluations can move faster.",
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "sideline_ai_tents",
                        label = "Fund Rapid Sideline Diagnostic Tents (-$3M)",
                        narrativeOutcome = "Cuts evaluation delays in half while preserving elite player brain safety.",
                        cashImpactMillions = -3L,
                        fanMoraleImpact = +8,
                        unionSatisfactionImpact = +6,
                        integrityImpact = +8
                    ),
                    CommissionerChoice(
                        id = "keep_strict_veto",
                        label = "Keep Full Mandatory 10-Minute Quiet-Room Rule",
                        narrativeOutcome = "Zero compromise on health metrics; medical boards applaud.",
                        integrityImpact = +10,
                        unionSatisfactionImpact = +8,
                        ownerApprovalImpact = -4
                    )
                )
            )
            "arc_safety_s2_repeat" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_repeat",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Second Concussion Protocol Miss Fuels Union Grievance",
                category = archetype.categoryLabel,
                urgencyTag = "STAGE 2 BRANCH • REPEAT LAPSE",
                description = "Because retraining left team doctors in charge of clearance, another questionable play went unflagged!",
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "upgrade_to_independent",
                        label = "Install Neutral Neurologists League-Wide Now (-$6M)",
                        narrativeOutcome = "Closes the loophole and halts union escalation.",
                        cashImpactMillions = -6L,
                        integrityImpact = +10,
                        unionSatisfactionImpact = +10
                    ),
                    CommissionerChoice(
                        id = "escalate_team_fines",
                        label = "Double Team Financial Penalties (+$4M Cash)",
                        narrativeOutcome = "Boosts treasury cash, though union trust remains frayed.",
                        cashImpactMillions = +4L,
                        unionSatisfactionImpact = -10,
                        integrityImpact = -2
                    )
                )
            )
            "arc_safety_s2_hearing" -> CommissionerDilemma(
                id = "dil_${arcId}_s2_hearing",
                arcId = arcId,
                archetype = archetype,
                stageNumber = 2,
                totalStages = 2,
                nodeId = nodeId,
                week = week,
                title = "Federal Safety Hearing Slams League Concussion Policy",
                category = archetype.categoryLabel,
                urgencyTag = "BRANCH FALLOUT • LEGISLATIVE INQUIRY",
                description = "Lawmakers publicized team medical text logs contradicting your earlier dismissal. Emergency reform is mandatory to avoid antitrust scrutiny.",
                priorDecisionLabel = priorDecisionLabel,
                isBranchFollowUp = true,
                choices = listOf(
                    CommissionerChoice(
                        id = "comprehensive_reform",
                        label = "Sign Binding Union Medical Charter (-$9M Settlement)",
                        narrativeOutcome = "Expensive overhaul finally stops legislative threats and heals player relations.",
                        cashImpactMillions = -9L,
                        unionSatisfactionImpact = +14,
                        integrityImpact = +12,
                        valuationImpactMillions = -8L
                    ),
                    CommissionerChoice(
                        id = "minimal_tweaks",
                        label = "Offer Advisory Guidelines Only",
                        narrativeOutcome = "Severe reputational damage depresses franchise valuations.",
                        integrityImpact = -16,
                        fanMoraleImpact = -12,
                        fanbaseImpactMillions = -0.5,
                        valuationImpactMillions = -18L
                    )
                )
            )

            // ==================== ARC 8: PRIMETIME OFFICIAL & CREW SICKOUT ====================
            else -> when (nodeId) {
                "arc_sickout_s1" -> CommissionerDilemma(
                    id = "dil_${arcId}_s1",
                    arcId = arcId,
                    archetype = archetype,
                    stageNumber = 1,
                    totalStages = 2,
                    nodeId = nodeId,
                    week = week,
                    title = "Officials Union Threatens Coordinated Primetime Sickout",
                    category = archetype.categoryLabel,
                    urgencyTag = "STAGE 1 • OFFICIATING CRISIS",
                    description = "Citing relentless travel and disputed playoff grading, senior referees notified Operations of a coordinated sickout targeting this week's $networkName slate involving $teamName.",
                    targetFranchiseId = team.id,
                    targetFranchiseName = teamName,
                    choices = listOf(
                        CommissionerChoice(
                            id = "stipend_charter",
                            label = "Approve $7M Travel Stipend & Grading Transparency",
                            narrativeOutcome = "Crews report full-strength; union tensions cool and whistle accuracy rises.",
                            cashImpactMillions = -7L,
                            integrityImpact = +6,
                            officiatingAccuracyImpact = +4,
                            refLockoutRiskImpact = -20,
                            ownerApprovalImpact = -4,
                            branchFollowUp = CrisisBranchFollowUpSpec(
                                followUpNodeId = "arc_sickout_s2_ratify",
                                delayWeeks = 2,
                                teaserLabel = "Long-Term Crew Modernization Accord"
                            )
                        ),
                        CommissionerChoice(
                            id = "replacement_crews",
                            label = "Call Ref Bluff & Deploy Reserve Replacement Officials",
                            narrativeOutcome = "Saves stipend cash, though unprepared reserve crews face immediate scrutiny under playoff lights.",
                            cashImpactMillions = +2L,
                            valuationImpactMillions = -8L,
                            integrityImpact = -10,
                            officiatingAccuracyImpact = -12,
                            refLockoutRiskImpact = +22,
                            fanMoraleImpact = -8,
                            branchFollowUp = CrisisBranchFollowUpSpec(
                                followUpNodeId = "arc_sickout_s2_blown_call",
                                delayWeeks = 2,
                                teaserLabel = "Replacement Crew Final-Second Disaster"
                            )
                        ),
                        CommissionerChoice(
                            id = "condense_showcase",
                            label = "Rebalance Travel Pods & Cut Midweek Flights",
                            narrativeOutcome = "De-escalates crew fatigue without major cash outflow.",
                            cashImpactMillions = -2L,
                            refLockoutRiskImpact = -10,
                            officiatingAccuracyImpact = +2
                        )
                    )
                )
                "arc_sickout_s2_ratify" -> CommissionerDilemma(
                    id = "dil_${arcId}_s2_ratify",
                    arcId = arcId,
                    archetype = archetype,
                    stageNumber = 2,
                    totalStages = 2,
                    nodeId = nodeId,
                    week = week,
                    title = "Referees Union Signs 3-Year Replay & Training Compact",
                    category = archetype.categoryLabel,
                    urgencyTag = "STAGE 2 BRANCH • STABILITY ACCORD",
                    description = "With travel concerns settled, officials agree to pilot automated review upgrades that boost league accuracy.",
                    priorDecisionLabel = priorDecisionLabel,
                    isBranchFollowUp = true,
                    choices = listOf(
                        CommissionerChoice(
                            id = "fund_replay_academy",
                            label = "Co-Fund Full-Time Replay Command Center (-$4M)",
                            narrativeOutcome = "Lockout risk plunges while whistle consistency reaches best-in-pro-sports marks.",
                            cashImpactMillions = -4L,
                            officiatingAccuracyImpact = +6,
                            refLockoutRiskImpact = -15,
                            integrityImpact = +10,
                            valuationImpactMillions = +10L
                        ),
                        CommissionerChoice(
                            id = "standard_contract_sign",
                            label = "Sign Baseline 3-Year CBA Without Extra Spend",
                            narrativeOutcome = "Locks in peace through the championship window.",
                            refLockoutRiskImpact = -12,
                            ownerApprovalImpact = +6
                        )
                    )
                )
                else -> CommissionerDilemma( // arc_sickout_s2_blown_call
                    id = "dil_${arcId}_s2_blown_call",
                    arcId = arcId,
                    archetype = archetype,
                    stageNumber = 2,
                    totalStages = 2,
                    nodeId = nodeId,
                    week = week,
                    title = "Replacement Crew Botches Final Play — Coaches Demand Settlement!",
                    category = archetype.categoryLabel,
                    urgencyTag = "BRANCH FALLOUT • WHISTLE DISASTER",
                    description = "Just as critics feared, an inexperienced replacement crew missed an obvious buzzer-beating infraction in $teamName's game! $networkName replays loop nonstop.",
                    targetFranchiseId = team.id,
                    targetFranchiseName = teamName,
                    priorDecisionLabel = priorDecisionLabel,
                    isBranchFollowUp = true,
                    choices = listOf(
                        CommissionerChoice(
                            id = "settle_refs_immediately",
                            label = "Settle Immediately With Referees Union (-$10M Catch-Up)",
                            narrativeOutcome = "Brings veteran crews back before playoffs and stops bleeding credibility.",
                            cashImpactMillions = -10L,
                            officiatingAccuracyImpact = +12,
                            refLockoutRiskImpact = -28,
                            fanMoraleImpact = +6,
                            integrityImpact = +8
                        ),
                        CommissionerChoice(
                            id = "stick_with_scabs",
                            label = "Double Down With Replacements & Fine Critics",
                            narrativeOutcome = "Controversies spiral across headlines; fans question legitimacy of standings.",
                            cashImpactMillions = -3L,
                            fanMoraleImpact = -14,
                            integrityImpact = -16,
                            fanbaseImpactMillions = -0.5,
                            valuationImpactMillions = -16L
                        )
                    )
                )
            }
        }
    }
}
