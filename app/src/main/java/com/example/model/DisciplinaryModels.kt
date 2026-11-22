package com.example.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

enum class DisciplinaryCategory(
    val title: String,
    val shortLabel: String,
    val defaultGames: Int,
    val defaultFineThousands: Int,
    val maxGames: Int,
    val maxFineThousands: Int,
    val badgeColorHex: Long,
    val summary: String
) {
    FLAGRANT_FOUL(
        title = "Flagrant / Reckless Play",
        shortLabel = "Flagrant Play",
        defaultGames = 2,
        defaultFineThousands = 35,
        maxGames = 10,
        maxFineThousands = 150,
        badgeColorHex = 0xFFF59E0B,
        summary = "Dangerous non-sport plays, airborne undercuts, blindside head contact, or reckless sticks/tackles."
    ),
    ALTERCATION_BRAWL(
        title = "Bench / On-Field Altercation",
        shortLabel = "Altercation",
        defaultGames = 3,
        defaultFineThousands = 50,
        maxGames = 12,
        maxFineThousands = 200,
        badgeColorHex = 0xFFEF4444,
        summary = "Leaving the bench area during an altercation, throwing punches, or escalating post-whistle scrums."
    ),
    OFFICIAL_ABUSE(
        title = "Official Intimidation & Abuse",
        shortLabel = "Ref Abuse",
        defaultGames = 1,
        defaultFineThousands = 40,
        maxGames = 8,
        maxFineThousands = 175,
        badgeColorHex = 0xFFEC4899,
        summary = "Physical contact with officials, throwing equipment near referees, or explosive public integrity accusations."
    ),
    SUBSTANCE_POLICY(
        title = "PED & Substance Violation",
        shortLabel = "Substance Policy",
        defaultGames = 8,
        defaultFineThousands = 120,
        maxGames = 24,
        maxFineThousands = 500,
        badgeColorHex = 0xFF8B5CF6,
        summary = "Positive test for banned performance-enhancing agents, masking compounds, or missed protocol windows."
    ),
    GAMBLING_INTEGRITY(
        title = "Wagering & Integrity Breach",
        shortLabel = "Gambling",
        defaultGames = 16,
        defaultFineThousands = 250,
        maxGames = 32,
        maxFineThousands = 1000,
        badgeColorHex = 0xFFDC2626,
        summary = "Wagering on league slate props, sharing confidential injury reports, or violating anti-corruption firewalls."
    ),
    CONDUCT_TAMPERING(
        title = "Detrimental Conduct & Tampering",
        shortLabel = "Conduct",
        defaultGames = 2,
        defaultFineThousands = 75,
        maxGames = 10,
        maxFineThousands = 250,
        badgeColorHex = 0xFF06B6D4,
        summary = "Locker room conduct detrimental to club stability, unauthorized recruitment contact, or social media scandals."
    )
}

enum class InfractionSeverity(
    val label: String,
    val scaleMultiplier: Double,
    val badgeColorHex: Long
) {
    MINOR("Minor Breach", 0.55, 0xFF38BDF8),
    MODERATE("Standard Breach", 1.0, 0xFFF59E0B),
    SEVERE("Severe Escalation", 1.5, 0xFFEF4444),
    CRITICAL("Critical League Crisis", 2.0, 0xFFDC2626)
}

enum class PrecedentPosture(
    val label: String,
    val multiplier: Double,
    val description: String
) {
    LENIENT("Lenient Warning", 0.65, "Prioritizes player union harmony and light financial deterrents over multi-week bans."),
    BALANCED("Balanced Standard", 1.0, "Follows orthodox pro-league sentencing scaled proportionately for repeat offenders."),
    FIRM("Firm Deterrence", 1.35, "Emphasizes safety and deterrence with stiffer game suspensions."),
    ZERO_TOLERANCE("Zero Tolerance", 1.75, "Maximum deterrence posture boosting integrity while testing union patience.")
}

enum class DisciplinaryStatus(val label: String) {
    PENDING_HEARING("Awaiting Ruling"),
    RULED("Sentence Enforced"),
    APPEALED_PENDING("Union Appeal Filed"),
    APPEAL_RESOLVED("Arbitration Finalized")
}

enum class PrecedentAlignmentTier(val label: String, val badgeColorHex: Long) {
    ALIGNED("Aligned with Precedent", 0xFF10B981),
    SLIGHT_VARIANCE("Measured Discretion", 0xFF06B6D4),
    NEW_LANDMARK("New Precedent Set", 0xFFF59E0B),
    ARBITRARY_BREAK("Inconsistent Departure", 0xFFEF4444)
}

enum class AppealResolutionAction(val label: String) {
    UPHOLD_ORIGINAL("Uphold Sentence"),
    REDUCE_TO_PRECEDENT("Align to Precedent"),
    SPLIT_COMPROMISE("Split Compromise")
}

data class PrecedentStandard(
    val category: DisciplinaryCategory,
    val benchmarkGames: Int,
    val benchmarkFineThousands: Int,
    val repeatOffenderMultiplier: Double = 1.5,
    val posture: PrecedentPosture = PrecedentPosture.BALANCED,
    val priorRulingsCount: Int = 2,
    val lastCaseCitation: String = "Baseline League Charter",
    val landmarkNote: String = ""
) {
    fun expectedPenaltyFor(
        severity: InfractionSeverity,
        priorOffensesCount: Int
    ): Pair<Int, Int> {
        val repeatFactor = if (priorOffensesCount <= 0) 1.0 else {
            1.0 + (priorOffensesCount * (repeatOffenderMultiplier - 1.0)).coerceAtMost(1.5)
        }
        val rawGames = (benchmarkGames * severity.scaleMultiplier * repeatFactor)
        val expectedGames = if (benchmarkGames == 0 && severity == InfractionSeverity.MINOR) {
            0
        } else {
            rawGames.roundToInt().coerceIn(0, category.maxGames)
        }
        val expectedFine = (benchmarkFineThousands * severity.scaleMultiplier * repeatFactor)
            .roundToInt()
            .coerceIn(0, category.maxFineThousands)
        return expectedGames to expectedFine
    }
}

data class DisciplinaryRuling(
    val suspensionGames: Int,
    val fineThousands: Int,
    val issuedYear: Int,
    val issuedWeek: Int,
    val setNewPrecedent: Boolean,
    val alignmentTier: PrecedentAlignmentTier,
    val deviationSummary: String,
    val consistencyDelta: Int,
    val appealRiskPct: Int,
    val commissionerNote: String = "",
    val appealOutcomeSummary: String = ""
)

data class DisciplinaryIncident(
    val id: String,
    val caseCode: String,
    val sportType: SportType,
    val category: DisciplinaryCategory,
    val severity: InfractionSeverity,
    val playerId: String,
    val playerName: String,
    val playerPosition: String,
    val playerOverall: Int,
    val franchiseId: String,
    val franchiseName: String,
    val franchiseColorHex: Long,
    val opponentName: String,
    val seasonYear: Int,
    val weekNumber: Int,
    val priorOffensesCount: Int,
    val incidentTitle: String,
    val incidentSummary: String,
    val evidenceSummary: String,
    val precedentCitation: String,
    val recommendedGames: Int,
    val recommendedFineThousands: Int,
    val status: DisciplinaryStatus = DisciplinaryStatus.PENDING_HEARING,
    val ruling: DisciplinaryRuling? = null,
    val appealBrief: String? = null
)

data class PrecedentEvaluation(
    val expectedGames: Int,
    val expectedFineThousands: Int,
    val proposedGames: Int,
    val proposedFineThousands: Int,
    val alignmentTier: PrecedentAlignmentTier,
    val deviationLabel: String,
    val consistencyDelta: Int,
    val appealRiskPct: Int,
    val integrityDelta: Int,
    val unionSatisfactionDelta: Int,
    val fanTrustDelta: Int,
    val ownerConfidenceDelta: Int
)

data class DisciplinaryDeskState(
    val sportType: SportType = SportType.BASKETBALL,
    val incidents: List<DisciplinaryIncident> = emptyList(),
    val precedents: List<PrecedentStandard> = emptyList(),
    val precedentConsistencyScore: Int = 86,
    val totalFinesCollectedThousands: Long = 0L,
    val totalSuspensionsIssuedGames: Int = 0,
    val landmarkPrecedentsCount: Int = 0,
    val nextCaseSequence: Int = 104
) {
    val pendingCases: List<DisciplinaryIncident>
        get() = incidents.filter { it.status == DisciplinaryStatus.PENDING_HEARING }

    val appealedCases: List<DisciplinaryIncident>
        get() = incidents.filter { it.status == DisciplinaryStatus.APPEALED_PENDING }

    val resolvedCases: List<DisciplinaryIncident>
        get() = incidents.filter {
            it.status == DisciplinaryStatus.RULED || it.status == DisciplinaryStatus.APPEAL_RESOLVED
        }

    fun standardFor(category: DisciplinaryCategory): PrecedentStandard =
        precedents.firstOrNull { it.category == category } ?: PrecedentStandard(
            category = category,
            benchmarkGames = category.defaultGames,
            benchmarkFineThousands = category.defaultFineThousands
        )
}

data class DisciplinaryAdjudicationOutcome(
    val updatedState: DisciplinaryDeskState,
    val updatedFranchises: List<Franchise>,
    val updatedUnion: LaborUnionAgreement,
    val cashTreasuryGainMillions: Long,
    val integrityDelta: Int,
    val fanAppealDelta: Int,
    val ownerApprovalDelta: Int,
    val appealTriggered: Boolean,
    val headline: String,
    val feedbackMessage: String
)

data class WeeklyDisciplinaryTickResult(
    val updatedState: DisciplinaryDeskState,
    val updatedFranchises: List<Franchise>,
    val newIncidents: List<DisciplinaryIncident>,
    val reinstatedPlayers: List<String>,
    val headlines: List<String>
)

object DisciplinaryEngine {

    private data class IncidentTemplate(
        val category: DisciplinaryCategory,
        val severity: InfractionSeverity,
        val title: String,
        val summaryPattern: String,
        val evidencePattern: String
    )

    fun createDefaultPrecedents(
        ruleBook: LeagueRuleBook = LeagueRuleBook()
    ): List<PrecedentStandard> {
        val dopingBoost = when (ruleBook.antiDopingPolicy) {
            PolicyStrictness.LENIENT -> 0.7
            PolicyStrictness.BALANCED -> 1.0
            PolicyStrictness.ZERO_TOLERANCE -> 1.45
        }
        val tamperingBoost = when (ruleBook.antiTamperingGamblingPolicy) {
            PolicyStrictness.LENIENT -> 0.75
            PolicyStrictness.BALANCED -> 1.0
            PolicyStrictness.ZERO_TOLERANCE -> 1.4
        }

        return DisciplinaryCategory.values().map { cat ->
            val scale = when (cat) {
                DisciplinaryCategory.SUBSTANCE_POLICY -> dopingBoost
                DisciplinaryCategory.GAMBLING_INTEGRITY,
                DisciplinaryCategory.CONDUCT_TAMPERING -> tamperingBoost
                else -> 1.0
            }
            val posture = when {
                scale >= 1.35 -> PrecedentPosture.ZERO_TOLERANCE
                scale <= 0.8 -> PrecedentPosture.LENIENT
                else -> PrecedentPosture.BALANCED
            }
            PrecedentStandard(
                category = cat,
                benchmarkGames = (cat.defaultGames * scale).roundToInt().coerceIn(0, cat.maxGames),
                benchmarkFineThousands = (cat.defaultFineThousands * scale).roundToInt().coerceIn(5, cat.maxFineThousands),
                repeatOffenderMultiplier = if (posture == PrecedentPosture.ZERO_TOLERANCE) 1.75 else 1.5,
                posture = posture,
                priorRulingsCount = 2,
                lastCaseCitation = "Charter Standard §${cat.ordinal + 1}0${cat.ordinal + 2}",
                landmarkNote = if (posture == PrecedentPosture.ZERO_TOLERANCE) {
                    "Elevated by strict League Charter policy enforcement."
                } else {
                    "Standard CBA disciplinary benchmark."
                }
            )
        }
    }

    fun createDefaultState(
        sportType: SportType,
        franchises: List<Franchise>,
        ruleBook: LeagueRuleBook = LeagueRuleBook(),
        seasonYear: Int = 2026,
        currentWeek: Int = 1
    ): DisciplinaryDeskState {
        val precedents = createDefaultPrecedents(ruleBook)
        val initialState = DisciplinaryDeskState(
            sportType = sportType,
            precedents = precedents,
            precedentConsistencyScore = 86,
            nextCaseSequence = 101
        )
        if (franchises.isEmpty()) return initialState

        val starterCases = mutableListOf<DisciplinaryIncident>()
        var seq = initialState.nextCaseSequence
        val templates = templatesForSport(sportType)
        val chosenTemplates = listOf(templates[0], templates[2], templates[4 % templates.size])
        val candidates = franchises.shuffled(Random(seasonYear * 31 + currentWeek))

        chosenTemplates.forEachIndexed { idx, tpl ->
            val team = candidates[idx % candidates.size]
            val opp = candidates[(idx + 1) % candidates.size]
            val player = team.roster.firstOrNull { !it.isUnavailableForGame && it.isStarter }
                ?: team.roster.firstOrNull()
                ?: return@forEachIndexed
            val priors = if (idx == 2) 1 else 0
            val std = precedents.first { it.category == tpl.category }
            val (recG, recF) = std.expectedPenaltyFor(tpl.severity, priors)
            starterCases.add(
                DisciplinaryIncident(
                    id = "disc_${seasonYear}_w${currentWeek}_$seq",
                    caseCode = "CASE-$seasonYear-$seq",
                    sportType = sportType,
                    category = tpl.category,
                    severity = tpl.severity,
                    playerId = player.id,
                    playerName = player.name,
                    playerPosition = player.position,
                    playerOverall = player.overallRating,
                    franchiseId = team.id,
                    franchiseName = team.teamFullName,
                    franchiseColorHex = team.colorHex,
                    opponentName = opp.teamFullName,
                    seasonYear = seasonYear,
                    weekNumber = currentWeek,
                    priorOffensesCount = priors,
                    incidentTitle = tpl.title,
                    incidentSummary = tpl.summaryPattern
                        .replace("{PLAYER}", player.name)
                        .replace("{TEAM}", team.teamFullName)
                        .replace("{OPP}", opp.teamFullName),
                    evidenceSummary = tpl.evidencePattern.replace("{PLAYER}", player.name),
                    precedentCitation = "${std.lastCaseCitation} (${std.benchmarkGames}G / \$${std.benchmarkFineThousands}K baseline)",
                    recommendedGames = recG,
                    recommendedFineThousands = recF,
                    status = DisciplinaryStatus.PENDING_HEARING
                )
            )
            seq++
        }

        return initialState.copy(
            incidents = starterCases,
            nextCaseSequence = seq
        )
    }

    fun evaluateProposedRuling(
        incident: DisciplinaryIncident,
        proposedGames: Int,
        proposedFineThousands: Int,
        setNewPrecedent: Boolean,
        state: DisciplinaryDeskState,
        unionAgreement: LaborUnionAgreement,
        ruleBook: LeagueRuleBook
    ): PrecedentEvaluation {
        val standard = state.standardFor(incident.category)
        val (expectedGames, expectedFine) = standard.expectedPenaltyFor(
            severity = incident.severity,
            priorOffensesCount = incident.priorOffensesCount
        )

        val gamesDiff = proposedGames - expectedGames
        val fineDiffRatio = if (expectedFine > 0) {
            (proposedFineThousands - expectedFine).toDouble() / expectedFine.toDouble()
        } else {
            if (proposedFineThousands > 25) 0.5 else 0.0
        }
        val gamesRatio = if (expectedGames > 0) {
            gamesDiff.toDouble() / expectedGames.toDouble()
        } else {
            proposedGames.toDouble() * 0.5
        }
        val combinedAbsDeviation = (abs(gamesRatio) * 0.65) + (abs(fineDiffRatio) * 0.35)

        val alignmentTier: PrecedentAlignmentTier
        val deviationLabel: String
        val consistencyDelta: Int

        when {
            abs(gamesDiff) <= 1 && abs(fineDiffRatio) <= 0.25 -> {
                alignmentTier = PrecedentAlignmentTier.ALIGNED
                deviationLabel = "Aligned with League Precedent (${expectedGames}G • \$${expectedFine}K)"
                consistencyDelta = +4
            }
            setNewPrecedent -> {
                alignmentTier = PrecedentAlignmentTier.NEW_LANDMARK
                val dir = if (gamesDiff >= 0) "Stricter Landmark" else "Reformed Landmark"
                deviationLabel = "$dir (${proposedGames}G vs prior ${expectedGames}G)"
                consistencyDelta = if (combinedAbsDeviation > 1.2) -1 else +2
            }
            combinedAbsDeviation <= 0.45 -> {
                alignmentTier = PrecedentAlignmentTier.SLIGHT_VARIANCE
                val dir = if (gamesDiff >= 0) "+${gamesDiff}G" else "${gamesDiff}G"
                deviationLabel = "Measured Discretion ($dir vs ${expectedGames}G benchmark)"
                consistencyDelta = +1
            }
            else -> {
                alignmentTier = PrecedentAlignmentTier.ARBITRARY_BREAK
                val dir = if (gamesDiff > 0) "Excessive (+${gamesDiff}G)" else if (gamesDiff < 0) "Too Lenient (${gamesDiff}G)" else "Fine Outlier"
                deviationLabel = "Precedent Break: $dir without updating policy"
                consistencyDelta = -((combinedAbsDeviation * 7).roundToInt().coerceIn(4, 14))
            }
        }

        // Compute appeal risk: spikes sharply when suspension exceeds benchmark without landmark justification or union satisfaction is low
        val harshGamesOver = (proposedGames - expectedGames).coerceAtLeast(0)
        val harshFineOverPct = ((fineDiffRatio).coerceAtLeast(0.0) * 28.0).roundToInt()
        val unionSensitivity = ((100 - unionAgreement.unionSatisfaction) / 4).coerceIn(0, 20)
        val consistencyPenalty = if (alignmentTier == PrecedentAlignmentTier.ARBITRARY_BREAK && gamesDiff > 0) 22 else 0
        val landmarkMitigation = if (setNewPrecedent) -6 else 0
        val appealRiskPct = (8 + (harshGamesOver * 14) + harshFineOverPct + unionSensitivity + consistencyPenalty + landmarkMitigation)
            .coerceIn(4, 92)

        // Integrity vs Union vs Fan impact
        val strictEnough = proposedGames >= expectedGames || (proposedGames == 0 && expectedGames <= 1 && proposedFineThousands >= expectedFine)
        val integrityBase = when {
            incident.category in listOf(DisciplinaryCategory.GAMBLING_INTEGRITY, DisciplinaryCategory.SUBSTANCE_POLICY) ->
                if (proposedGames >= expectedGames) +4 else -6
            strictEnough -> +2
            else -> -3
        }
        val consistencyIntegrityMod = if (alignmentTier == PrecedentAlignmentTier.ARBITRARY_BREAK) -3 else if (alignmentTier == PrecedentAlignmentTier.ALIGNED) +1 else 0
        val charterBonus = if (ruleBook.antiDopingPolicy == PolicyStrictness.ZERO_TOLERANCE && incident.category == DisciplinaryCategory.SUBSTANCE_POLICY && proposedGames >= expectedGames) 1 else 0
        val integrityDelta = (integrityBase + consistencyIntegrityMod + charterBonus).coerceIn(-10, 8)

        val unionDelta = when {
            harshGamesOver >= 3 -> -(harshGamesOver * 2 + 2)
            harshGamesOver >= 1 && !setNewPrecedent -> -3
            proposedGames < expectedGames -> +2
            alignmentTier == PrecedentAlignmentTier.ALIGNED -> 0
            else -> -1
        }.coerceIn(-14, 5)

        val fanDelta = when {
            incident.severity >= InfractionSeverity.SEVERE && proposedGames == 0 -> -5
            alignmentTier == PrecedentAlignmentTier.ALIGNED -> +2
            alignmentTier == PrecedentAlignmentTier.NEW_LANDMARK && proposedGames >= expectedGames -> +3
            else -> -2
        }

        val ownerDelta = when {
            proposedGames > expectedGames + 2 -> -3 // team/owners dislike losing stars unfairly long
            integrityDelta >= 3 -> +2
            else -> 0
        }

        return PrecedentEvaluation(
            expectedGames = expectedGames,
            expectedFineThousands = expectedFine,
            proposedGames = proposedGames,
            proposedFineThousands = proposedFineThousands,
            alignmentTier = alignmentTier,
            deviationLabel = deviationLabel,
            consistencyDelta = consistencyDelta,
            appealRiskPct = appealRiskPct,
            integrityDelta = integrityDelta,
            unionSatisfactionDelta = unionDelta,
            fanTrustDelta = fanDelta,
            ownerConfidenceDelta = ownerDelta
        )
    }

    fun adjudicateIncident(
        state: DisciplinaryDeskState,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement,
        ruleBook: LeagueRuleBook,
        incidentId: String,
        suspensionGames: Int,
        fineThousands: Int,
        setNewPrecedent: Boolean,
        commissionerNote: String,
        random: Random = Random.Default
    ): DisciplinaryAdjudicationOutcome {
        val target = state.incidents.firstOrNull { it.id == incidentId }
            ?: return DisciplinaryAdjudicationOutcome(
                updatedState = state,
                updatedFranchises = franchises,
                updatedUnion = unionAgreement,
                cashTreasuryGainMillions = 0L,
                integrityDelta = 0,
                fanAppealDelta = 0,
                ownerApprovalDelta = 0,
                appealTriggered = false,
                headline = "",
                feedbackMessage = "Case not found on the disciplinary docket."
            )

        val clampedGames = suspensionGames.coerceIn(0, target.category.maxGames)
        val clampedFine = fineThousands.coerceIn(0, target.category.maxFineThousands)
        val eval = evaluateProposedRuling(
            incident = target,
            proposedGames = clampedGames,
            proposedFineThousands = clampedFine,
            setNewPrecedent = setNewPrecedent,
            state = state,
            unionAgreement = unionAgreement,
            ruleBook = ruleBook
        )

        // Check whether Union Arbitration Appeal is triggered
        val forceAppeal = (clampedGames - eval.expectedGames >= 2 && (eval.appealRiskPct >= 40 || clampedGames == target.category.maxGames))
        val appealTriggered = clampedGames >= 2 && (forceAppeal || random.nextInt(100) < eval.appealRiskPct)

        val newStatus = if (appealTriggered) DisciplinaryStatus.APPEALED_PENDING else DisciplinaryStatus.RULED
        val ruling = DisciplinaryRuling(
            suspensionGames = clampedGames,
            fineThousands = clampedFine,
            issuedYear = target.seasonYear,
            issuedWeek = target.weekNumber,
            setNewPrecedent = setNewPrecedent,
            alignmentTier = eval.alignmentTier,
            deviationSummary = eval.deviationLabel,
            consistencyDelta = eval.consistencyDelta,
            appealRiskPct = eval.appealRiskPct,
            commissionerNote = commissionerNote.trim()
        )
        val appealBrief = if (appealTriggered) {
            val diff = clampedGames - eval.expectedGames
            if (diff > 0) {
                "NBPA Grievance Filed: ${target.playerName}'s camp challenges a ${clampedGames}-game ban (+${diff}G over ${target.category.shortLabel} precedent of ${eval.expectedGames}G)."
            } else {
                "NBPA Expedited Appeal: Union contests suspension length (${clampedGames}G) citing mitigating on-field provocation."
            }
        } else null

        val updatedIncidents = state.incidents.map { inc ->
            if (inc.id == incidentId) {
                inc.copy(status = newStatus, ruling = ruling, appealBrief = appealBrief)
            } else inc
        }

        val updatedPrecedents = state.precedents.map { std ->
            if (std.category == target.category) {
                if (setNewPrecedent) {
                    val impliedScale = target.severity.scaleMultiplier *
                        (if (target.priorOffensesCount <= 0) 1.0 else 1.0 + target.priorOffensesCount * 0.4)
                    val normalizedGames = (clampedGames / impliedScale).roundToInt().coerceIn(0, std.category.maxGames)
                    val normalizedFine = (clampedFine / impliedScale).roundToInt().coerceIn(5, std.category.maxFineThousands)
                    val nextPosture = when {
                        normalizedGames > std.benchmarkGames + 1 -> PrecedentPosture.ZERO_TOLERANCE
                        normalizedGames > std.benchmarkGames -> PrecedentPosture.FIRM
                        normalizedGames < std.benchmarkGames -> PrecedentPosture.LENIENT
                        else -> std.posture
                    }
                    std.copy(
                        benchmarkGames = normalizedGames,
                        benchmarkFineThousands = normalizedFine,
                        posture = nextPosture,
                        priorRulingsCount = std.priorRulingsCount + 1,
                        lastCaseCitation = target.caseCode,
                        landmarkNote = "Updated by ${target.caseCode} (${clampedGames}G / \$${clampedFine}K)"
                    )
                } else {
                    std.copy(
                        priorRulingsCount = std.priorRulingsCount + 1,
                        lastCaseCitation = target.caseCode
                    )
                }
            } else std
        }

        // Apply suspension to real RosterPlayer on franchise
        val updatedFranchises = franchises.map { team ->
            if (team.id == target.franchiseId) {
                val updatedRoster = team.roster.map { player ->
                    if (player.id == target.playerId || player.name == target.playerName) {
                        player.copy(
                            suspendedGamesRemaining = maxOf(player.suspendedGamesRemaining, clampedGames)
                        )
                    } else player
                }
                val optimized = InjuryEngine.autoOptimizeDepthChart(
                    team.copy(
                        roster = updatedRoster,
                        fanMorale = (team.fanMorale + eval.fanTrustDelta / 2).coerceIn(25, 99),
                        ownership = team.ownership.copy(
                            ownerApproval = (team.ownership.ownerApproval + eval.ownerConfidenceDelta).coerceIn(20, 99)
                        )
                    )
                )
                optimized
            } else team
        }

        val prevTotalFineThousands = state.totalFinesCollectedThousands
        val newTotalFineThousands = prevTotalFineThousands + clampedFine
        val cashGainMillions = (newTotalFineThousands / 1000L) - (prevTotalFineThousands / 1000L)

        val updatedUnion = unionAgreement.copy(
            unionSatisfaction = (unionAgreement.unionSatisfaction + eval.unionSatisfactionDelta).coerceIn(10, 100),
            strikeRisk = (unionAgreement.strikeRisk - (eval.unionSatisfactionDelta / 2)).coerceIn(0, 100)
        )

        val nextState = state.copy(
            incidents = updatedIncidents,
            precedents = updatedPrecedents,
            precedentConsistencyScore = (state.precedentConsistencyScore + eval.consistencyDelta).coerceIn(20, 100),
            totalFinesCollectedThousands = newTotalFineThousands,
            totalSuspensionsIssuedGames = state.totalSuspensionsIssuedGames + clampedGames,
            landmarkPrecedentsCount = state.landmarkPrecedentsCount + if (setNewPrecedent) 1 else 0
        )

        val sentenceDesc = if (clampedGames > 0) "${clampedGames}-game suspension & \$${clampedFine}K fine" else "\$${clampedFine}K fine (no suspension)"
        val precedentTag = if (setNewPrecedent) " [New ${target.category.shortLabel} Precedent]" else ""
        val appealTag = if (appealTriggered) " • Union filed arbitration appeal" else ""
        val headline = "DISCIPLINARY RULING (${target.caseCode}): ${target.playerName} (${target.franchiseName}) issued $sentenceDesc for ${target.incidentTitle}.$precedentTag$appealTag"

        return DisciplinaryAdjudicationOutcome(
            updatedState = nextState,
            updatedFranchises = updatedFranchises,
            updatedUnion = updatedUnion,
            cashTreasuryGainMillions = cashGainMillions,
            integrityDelta = eval.integrityDelta,
            fanAppealDelta = eval.fanTrustDelta,
            ownerApprovalDelta = eval.ownerConfidenceDelta,
            appealTriggered = appealTriggered,
            headline = headline,
            feedbackMessage = "Issued $sentenceDesc on ${target.caseCode}. ${eval.deviationLabel}.$appealTag"
        )
    }

    fun resolveAppeal(
        state: DisciplinaryDeskState,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement,
        incidentId: String,
        action: AppealResolutionAction
    ): DisciplinaryAdjudicationOutcome {
        val target = state.incidents.firstOrNull { it.id == incidentId }
            ?: return DisciplinaryAdjudicationOutcome(
                updatedState = state,
                updatedFranchises = franchises,
                updatedUnion = unionAgreement,
                cashTreasuryGainMillions = 0L,
                integrityDelta = 0,
                fanAppealDelta = 0,
                ownerApprovalDelta = 0,
                appealTriggered = false,
                headline = "",
                feedbackMessage = "Appealed case not found."
            )
        val ruling = target.ruling ?: return DisciplinaryAdjudicationOutcome(
            updatedState = state,
            updatedFranchises = franchises,
            updatedUnion = unionAgreement,
            cashTreasuryGainMillions = 0L,
            integrityDelta = 0,
            fanAppealDelta = 0,
            ownerApprovalDelta = 0,
            appealTriggered = false,
            headline = "",
            feedbackMessage = "Case has not been ruled yet."
        )

        val std = state.standardFor(target.category)
        val (precGames, precFine) = std.expectedPenaltyFor(target.severity, target.priorOffensesCount)

        val (finalGames, finalFine, unionDelta, consistencyDelta, integrityDelta, note) = when (action) {
            AppealResolutionAction.UPHOLD_ORIGINAL -> {
                val uDelta = if (ruling.alignmentTier == PrecedentAlignmentTier.ARBITRARY_BREAK) -6 else -2
                AppealCalc(
                    finalGames = ruling.suspensionGames,
                    finalFine = ruling.fineThousands,
                    unionDelta = uDelta,
                    consistencyDelta = if (ruling.alignmentTier == PrecedentAlignmentTier.ARBITRARY_BREAK) -2 else +2,
                    integrityDelta = +3,
                    outcomeNote = "Arbitrator upheld original ${ruling.suspensionGames}G / \$${ruling.fineThousands}K sentence."
                )
            }
            AppealResolutionAction.REDUCE_TO_PRECEDENT -> {
                AppealCalc(
                    finalGames = precGames,
                    finalFine = precFine,
                    unionDelta = +6,
                    consistencyDelta = +5,
                    integrityDelta = +1,
                    outcomeNote = "Aligned on appeal to league precedent (${precGames}G / \$${precFine}K)."
                )
            }
            AppealResolutionAction.SPLIT_COMPROMISE -> {
                val midG = ((ruling.suspensionGames + precGames) / 2.0).roundToInt().coerceAtLeast(0)
                val midF = ((ruling.fineThousands + precFine) / 2.0).roundToInt().coerceAtLeast(0)
                AppealCalc(
                    finalGames = midG,
                    finalFine = midF,
                    unionDelta = +3,
                    consistencyDelta = +2,
                    integrityDelta = +1,
                    outcomeNote = "Settlement compromise reached: ${midG}G / \$${midF}K."
                )
            }
        }

        val fineDelta = finalFine - ruling.fineThousands
        val gamesDelta = finalGames - ruling.suspensionGames

        val updatedRuling = ruling.copy(
            suspensionGames = finalGames,
            fineThousands = finalFine,
            appealOutcomeSummary = note
        )
        val updatedIncidents = state.incidents.map { inc ->
            if (inc.id == incidentId) {
                inc.copy(status = DisciplinaryStatus.APPEAL_RESOLVED, ruling = updatedRuling)
            } else inc
        }

        val updatedFranchises = franchises.map { team ->
            if (team.id == target.franchiseId) {
                val updatedRoster = team.roster.map { p ->
                    if (p.id == target.playerId || p.name == target.playerName) {
                        val adjusted = (p.suspendedGamesRemaining + gamesDelta).coerceAtLeast(0)
                        p.copy(suspendedGamesRemaining = adjusted)
                    } else p
                }
                InjuryEngine.autoOptimizeDepthChart(team.copy(roster = updatedRoster))
            } else team
        }

        val nextState = state.copy(
            incidents = updatedIncidents,
            precedentConsistencyScore = (state.precedentConsistencyScore + consistencyDelta).coerceIn(20, 100),
            totalFinesCollectedThousands = (state.totalFinesCollectedThousands + fineDelta).coerceAtLeast(0L),
            totalSuspensionsIssuedGames = (state.totalSuspensionsIssuedGames + gamesDelta).coerceAtLeast(0)
        )
        val updatedUnion = unionAgreement.copy(
            unionSatisfaction = (unionAgreement.unionSatisfaction + unionDelta).coerceIn(10, 100),
            strikeRisk = (unionAgreement.strikeRisk - (unionDelta / 2)).coerceIn(0, 100)
        )
        val headline = "ARBITRATION DECISION (${target.caseCode}): ${target.playerName} (${target.franchiseName}) — $note"

        return DisciplinaryAdjudicationOutcome(
            updatedState = nextState,
            updatedFranchises = updatedFranchises,
            updatedUnion = updatedUnion,
            cashTreasuryGainMillions = 0L,
            integrityDelta = integrityDelta,
            fanAppealDelta = if (action == AppealResolutionAction.REDUCE_TO_PRECEDENT) +1 else 0,
            ownerApprovalDelta = if (finalGames < ruling.suspensionGames) +2 else -1,
            appealTriggered = false,
            headline = headline,
            feedbackMessage = note
        )
    }

    private data class AppealCalc(
        val finalGames: Int,
        val finalFine: Int,
        val unionDelta: Int,
        val consistencyDelta: Int,
        val integrityDelta: Int,
        val outcomeNote: String
    )

    fun updateCategoryPrecedent(
        state: DisciplinaryDeskState,
        category: DisciplinaryCategory,
        newBenchmarkGames: Int,
        newBenchmarkFineThousands: Int,
        newPosture: PrecedentPosture,
        seasonYear: Int,
        weekNumber: Int
    ): DisciplinaryDeskState {
        val clampedG = newBenchmarkGames.coerceIn(0, category.maxGames)
        val clampedF = newBenchmarkFineThousands.coerceIn(0, category.maxFineThousands)
        val updatedPrecedents = state.precedents.map { std ->
            if (std.category == category) {
                std.copy(
                    benchmarkGames = clampedG,
                    benchmarkFineThousands = clampedF,
                    posture = newPosture,
                    repeatOffenderMultiplier = if (newPosture == PrecedentPosture.ZERO_TOLERANCE) 1.75 else 1.5,
                    lastCaseCitation = "Policy Memo Y$seasonYear-W$weekNumber",
                    landmarkNote = "Set to ${newPosture.label} (${clampedG}G / \$${clampedF}K baseline)"
                )
            } else std
        }
        return state.copy(
            precedents = updatedPrecedents,
            precedentConsistencyScore = (state.precedentConsistencyScore + 2).coerceIn(20, 100),
            landmarkPrecedentsCount = state.landmarkPrecedentsCount + 1
        )
    }

    fun advanceWeeklySuspensions(franchises: List<Franchise>): Pair<List<Franchise>, List<String>> {
        val headlines = mutableListOf<String>()
        val updated = franchises.map { team ->
            var changed = false
            val updatedRoster = team.roster.map { player ->
                if (player.suspendedGamesRemaining > 0) {
                    changed = true
                    val rem = player.suspendedGamesRemaining - 1
                    if (rem == 0) {
                        headlines.add("REINSTATEMENT: ${player.name} (${team.teamFullName}) has completed their league suspension and is eligible to return.")
                    }
                    player.copy(suspendedGamesRemaining = rem)
                } else player
            }
            if (changed) InjuryEngine.autoOptimizeDepthChart(team.copy(roster = updatedRoster)) else team
        }
        return updated to headlines
    }

    fun generateWeeklyInfractions(
        state: DisciplinaryDeskState,
        franchises: List<Franchise>,
        playedMatches: List<GameMatch>,
        ruleBook: LeagueRuleBook,
        sportType: SportType,
        seasonYear: Int,
        week: Int,
        random: Random = Random.Default
    ): WeeklyDisciplinaryTickResult {
        val (postSuspensionTeams, reinstatementHeadlines) = advanceWeeklySuspensions(franchises)
        if (postSuspensionTeams.isEmpty()) {
            return WeeklyDisciplinaryTickResult(
                updatedState = state,
                updatedFranchises = postSuspensionTeams,
                newIncidents = emptyList(),
                reinstatedPlayers = emptyList(),
                headlines = reinstatementHeadlines
            )
        }

        // Keep pending queue manageable while realistically logging 1 new case on active weeks (up to 2 if physical/scandal heavy)
        val pendingCount = state.pendingCases.size
        val targetNewCount = when {
            pendingCount >= 6 -> 0
            pendingCount >= 4 -> if (random.nextInt(100) < 40) 1 else 0
            else -> if (random.nextInt(100) < 80) 1 else 2
        }
        val templates = templatesForSport(sportType)
        val newCases = mutableListOf<DisciplinaryIncident>()
        var seq = state.nextCaseSequence

        repeat(targetNewCount) { idx ->
            val match = playedMatches.takeIf { it.isNotEmpty() }?.let { it[random.nextInt(it.size)] }
            val team = if (match != null) {
                val teamId = if ((idx + random.nextInt(2)) % 2 == 0) match.homeTeamId else match.awayTeamId
                postSuspensionTeams.firstOrNull { it.id == teamId } ?: postSuspensionTeams[random.nextInt(postSuspensionTeams.size)]
            } else {
                postSuspensionTeams[random.nextInt(postSuspensionTeams.size)]
            }
            val oppName = if (match != null) {
                val oppId = if (team.id == match.homeTeamId) match.awayTeamId else match.homeTeamId
                postSuspensionTeams.firstOrNull { it.id == oppId }?.teamFullName ?: "Conference Rival"
            } else {
                postSuspensionTeams.firstOrNull { it.id != team.id }?.teamFullName ?: "Conference Rival"
            }
            val eligiblePlayers = team.roster.filter { !it.isUnavailableForGame }
            val player = if (eligiblePlayers.isNotEmpty()) {
                eligiblePlayers[random.nextInt(eligiblePlayers.size)]
            } else {
                team.roster.firstOrNull()
            }
            if (player != null) {
                // Prioritize categories that don't already flood the pending docket
                val existingCats = (state.pendingCases + newCases).map { it.category }.toSet()
                val preferredTemplates = templates.filter { it.category !in existingCats }.ifEmpty { templates }
                val tpl = preferredTemplates[random.nextInt(preferredTemplates.size)]
                val priorOffenses = state.incidents.count {
                    it.playerId == player.id && it.status != DisciplinaryStatus.PENDING_HEARING
                } + if (random.nextInt(100) < 22) 1 else 0
                val std = state.standardFor(tpl.category)
                val (recG, recF) = std.expectedPenaltyFor(tpl.severity, priorOffenses)

                newCases.add(
                    DisciplinaryIncident(
                        id = "disc_${seasonYear}_w${week}_$seq",
                        caseCode = "CASE-$seasonYear-$seq",
                        sportType = sportType,
                        category = tpl.category,
                        severity = tpl.severity,
                        playerId = player.id,
                        playerName = player.name,
                        playerPosition = player.position,
                        playerOverall = player.overallRating,
                        franchiseId = team.id,
                        franchiseName = team.teamFullName,
                        franchiseColorHex = team.colorHex,
                        opponentName = oppName,
                        seasonYear = seasonYear,
                        weekNumber = week,
                        priorOffensesCount = priorOffenses,
                        incidentTitle = tpl.title,
                        incidentSummary = tpl.summaryPattern
                            .replace("{PLAYER}", player.name)
                            .replace("{TEAM}", team.teamFullName)
                            .replace("{OPP}", oppName),
                        evidenceSummary = tpl.evidencePattern.replace("{PLAYER}", player.name),
                        precedentCitation = "${std.lastCaseCitation} (${std.benchmarkGames}G / \$${std.benchmarkFineThousands}K baseline)",
                        recommendedGames = recG,
                        recommendedFineThousands = recF,
                        status = DisciplinaryStatus.PENDING_HEARING
                    )
                )
                seq++
            }
        }

        val alertHeadlines = newCases.map {
            "DISCIPLINARY DOCKET (${it.caseCode}): ${it.playerName} (${it.franchiseName}) referred for ${it.incidentTitle}."
        }
        val nextState = state.copy(
            sportType = sportType,
            incidents = newCases + state.incidents,
            nextCaseSequence = seq
        )
        return WeeklyDisciplinaryTickResult(
            updatedState = nextState,
            updatedFranchises = postSuspensionTeams,
            newIncidents = newCases,
            reinstatedPlayers = reinstatementHeadlines,
            headlines = reinstatementHeadlines + alertHeadlines
        )
    }

    fun generateManualIncident(
        state: DisciplinaryDeskState,
        franchises: List<Franchise>,
        sportType: SportType,
        seasonYear: Int,
        week: Int,
        random: Random = Random.Default
    ): Pair<DisciplinaryDeskState, DisciplinaryIncident?> {
        if (franchises.isEmpty()) return state to null
        val team = franchises[random.nextInt(franchises.size)]
        val opp = franchises.firstOrNull { it.id != team.id } ?: team
        val player = team.roster.filter { !it.isUnavailableForGame }.randomOrNull(random)
            ?: team.roster.firstOrNull()
            ?: return state to null

        val templates = templatesForSport(sportType)
        val pendingCats = state.pendingCases.map { it.category }.toSet()
        val tpl = templates.filter { it.category !in pendingCats }.ifEmpty { templates }.random(random)
        val seq = state.nextCaseSequence
        val priorOffenses = state.incidents.count {
            it.playerId == player.id && it.status != DisciplinaryStatus.PENDING_HEARING
        }
        val std = state.standardFor(tpl.category)
        val (recG, recF) = std.expectedPenaltyFor(tpl.severity, priorOffenses)

        val incident = DisciplinaryIncident(
            id = "disc_${seasonYear}_w${week}_$seq",
            caseCode = "CASE-$seasonYear-$seq",
            sportType = sportType,
            category = tpl.category,
            severity = tpl.severity,
            playerId = player.id,
            playerName = player.name,
            playerPosition = player.position,
            playerOverall = player.overallRating,
            franchiseId = team.id,
            franchiseName = team.teamFullName,
            franchiseColorHex = team.colorHex,
            opponentName = opp.teamFullName,
            seasonYear = seasonYear,
            weekNumber = week,
            priorOffensesCount = priorOffenses,
            incidentTitle = tpl.title,
            incidentSummary = tpl.summaryPattern
                .replace("{PLAYER}", player.name)
                .replace("{TEAM}", team.teamFullName)
                .replace("{OPP}", opp.teamFullName),
            evidenceSummary = tpl.evidencePattern.replace("{PLAYER}", player.name),
            precedentCitation = "${std.lastCaseCitation} (${std.benchmarkGames}G / \$${std.benchmarkFineThousands}K baseline)",
            recommendedGames = recG,
            recommendedFineThousands = recF,
            status = DisciplinaryStatus.PENDING_HEARING
        )
        return state.copy(
            incidents = listOf(incident) + state.incidents,
            nextCaseSequence = seq + 1
        ) to incident
    }

    private fun templatesForSport(sport: SportType): List<IncidentTemplate> = when (sport) {
        SportType.BASKETBALL -> listOf(
            IncidentTemplate(
                category = DisciplinaryCategory.FLAGRANT_FOUL,
                severity = InfractionSeverity.SEVERE,
                title = "Flagrant-2 Airborne Closeout Undercut",
                summaryPattern = "During Q4 against {OPP}, {PLAYER} ({TEAM}) stepped underneath an airborne shooter on a transition three, leading to an immediate Flagrant-2 ejection.",
                evidencePattern = "Replay Bunker angle confirms reckless foot placement inside the shooter's landing zone."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.ALTERCATION_BRAWL,
                severity = InfractionSeverity.SEVERE,
                title = "Court-Side Shoving & Bench Escalation",
                summaryPattern = "After a hard box-out against {OPP}, {PLAYER} ({TEAM}) threw an open-handed strike and triggered both benches to spill onto the hardwood.",
                evidencePattern = "Broadcast slow-motion and baseline security cameras corroborate forceful retaliation."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.OFFICIAL_ABUSE,
                severity = InfractionSeverity.MODERATE,
                title = "Postgame Officiating Integrity Tirade",
                summaryPattern = "Following a one-possession loss to {OPP}, {PLAYER} ({TEAM}) alleged at the podium that the officiating crew 'rigged the whistle down the stretch.'",
                evidencePattern = "Verified postgame locker room transcript and NBA-pool audio log."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.SUBSTANCE_POLICY,
                severity = InfractionSeverity.SEVERE,
                title = "Prohibited Recovery Peptide Screening",
                summaryPattern = "Independent lab testing flagged {PLAYER} ({TEAM}) for a banned synthetic recovery peptide in an unannounced midweek urine sample.",
                evidencePattern = "Both Sample A and certified Sample B lab toxicology analyses confirmed non-negative."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.GAMBLING_INTEGRITY,
                severity = InfractionSeverity.CRITICAL,
                title = "Third-Party Parlay & Injury Tip Inquiry",
                summaryPattern = "Integrity monitors detected suspicious line movement tied to {PLAYER} ({TEAM}) minutes allocation prior to tipoff versus {OPP}.",
                evidencePattern = "Sportsbook geofence telemetry and encrypted message audit forwarded by Security."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.CONDUCT_TAMPERING,
                severity = InfractionSeverity.MODERATE,
                title = "Unauthorized Free-Agency Recruitment Contact",
                summaryPattern = "League compliance uncovered pre-window recruitment messages sent by {PLAYER} ({TEAM}) courting a rival star under contract with {OPP}.",
                evidencePattern = "Tampering arbitration filing submitted by {OPP} front office counsel."
            )
        )
        SportType.FOOTBALL -> listOf(
            IncidentTemplate(
                category = DisciplinaryCategory.FLAGRANT_FOUL,
                severity = InfractionSeverity.SEVERE,
                title = "Late Crown-of-Helmet Blindside Hit",
                summaryPattern = "{PLAYER} ({TEAM}) launched helmet-first into a defenseless receiver on a completed sideline route against {OPP}, drawing an automatic disqualification.",
                evidencePattern = "All-22 tracking shows forcible contact well after the whistle and clear launch angle."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.ALTERCATION_BRAWL,
                severity = InfractionSeverity.SEVERE,
                title = "Helmet-Swinging Pileup Brawl",
                summaryPattern = "During a goal-line stand vs {OPP}, {PLAYER} ({TEAM}) ripped off an opponent's helmet and sparked a multi-player sideline brawl.",
                evidencePattern = "SkyCam footage and field judge report confirm initiation of the altercation."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.OFFICIAL_ABUSE,
                severity = InfractionSeverity.MODERATE,
                title = "Sideline Contact with Line Judge",
                summaryPattern = "Disputing a pass interference no-call against {OPP}, {PLAYER} ({TEAM}) bumped the down judge while slamming a tablet onto the turf.",
                evidencePattern = "Sideline parabolic mic and Referee Union formal grievance log."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.SUBSTANCE_POLICY,
                severity = InfractionSeverity.SEVERE,
                title = "Banned Stimulant & Diuretic Violation",
                summaryPattern = "{PLAYER} ({TEAM}) returned a positive screening for a prohibited masking diuretic ahead of Week preparation.",
                evidencePattern = "League Medical Toxicology Board confirmed B-sample validation."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.GAMBLING_INTEGRITY,
                severity = InfractionSeverity.CRITICAL,
                title = "Facility Sportsbook Mobile Login Breach",
                summaryPattern = "Compliance monitors flagged mobile parlay wagers placed from inside {TEAM}'s practice facility linked to {PLAYER}.",
                evidencePattern = "Device MAC telemetry cross-referenced with licensed sportsbook compliance feed."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.CONDUCT_TAMPERING,
                severity = InfractionSeverity.MODERATE,
                title = "Detrimental Post-Practice Locker Altercation",
                summaryPattern = "{PLAYER} ({TEAM}) engaged in an unexcused media outburst criticizing ownership strategy prior to facing {OPP}.",
                evidencePattern = "Club General Manager incident referral and verified press pool clip."
            )
        )
        SportType.SOCCER -> listOf(
            IncidentTemplate(
                category = DisciplinaryCategory.FLAGRANT_FOUL,
                severity = InfractionSeverity.SEVERE,
                title = "Two-Footed Studs-Up Lunge",
                summaryPattern = "In the 78th minute against {OPP}, {PLAYER} ({TEAM}) dove in with both boots off the turf, catching an opponent's ankle on a breakaway.",
                evidencePattern = "VAR goal-line & touchline angles confirm excessive force and direct cleat contact."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.ALTERCATION_BRAWL,
                severity = InfractionSeverity.MODERATE,
                title = "Technical Area Tunnel Confrontation",
                summaryPattern = "After full time against {OPP}, {PLAYER} ({TEAM}) sparked a heated tunnel scrum requiring stadium stewards to separate both squads.",
                evidencePattern = "Fourth official match report and stadium tunnel CCTV capture."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.OFFICIAL_ABUSE,
                severity = InfractionSeverity.MODERATE,
                title = "Touchline Confrontation with Fourth Official",
                summaryPattern = "{PLAYER} ({TEAM}) sarcastically applauded inches from the referee's face and shoved the match ball away against {OPP}.",
                evidencePattern = "Match Commissioner report and broadcast touchline audio."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.SUBSTANCE_POLICY,
                severity = InfractionSeverity.SEVERE,
                title = "Out-of-Competition Whereabouts Failure",
                summaryPattern = "Doping control officers reported {PLAYER} ({TEAM}) logged a second missed biological passport check plus elevated hematocrit markers.",
                evidencePattern = "Anti-Doping Tribunal dossier and lab longitudinal profile."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.GAMBLING_INTEGRITY,
                severity = InfractionSeverity.CRITICAL,
                title = "Yellow-Card Spot-Fixing Alert",
                summaryPattern = "Global integrity monitors flagged abnormal single-market wagers on a deliberate tactical booking involving {PLAYER} ({TEAM}) vs {OPP}.",
                evidencePattern = "International betting integrity alert & tactical footage review."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.CONDUCT_TAMPERING,
                severity = InfractionSeverity.MODERATE,
                title = "Unauthorized Agent Tapping-Up Leak",
                summaryPattern = "Leaked audio implicated {PLAYER} ({TEAM}) in coordinating an unauthorized transfer tap-up with {OPP}'s starting attacker.",
                evidencePattern = "Club Legal Counsel dossier and League Compliance subpoena."
            )
        )
        SportType.HOCKEY -> listOf(
            IncidentTemplate(
                category = DisciplinaryCategory.FLAGRANT_FOUL,
                severity = InfractionSeverity.SEVERE,
                title = "Blindside High-Speed Boarding Hit",
                summaryPattern = "{PLAYER} ({TEAM}) delivered a late cross-check from behind driving an {OPP} skater head-first into the end boards.",
                evidencePattern = "Department of Player Safety overhead feed confirms principal point of contact to head/neck."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.ALTERCATION_BRAWL,
                severity = InfractionSeverity.MODERATE,
                title = "Third-Period Line Brawl & Instigator",
                summaryPattern = "After a whistle in the crease against {OPP}, {PLAYER} ({TEAM}) dropped the gloves as an instigator inside the final five minutes.",
                evidencePattern = "Linesman incident log and penalty-box camera review."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.OFFICIAL_ABUSE,
                severity = InfractionSeverity.MODERATE,
                title = "Slashing Boards & Verbal Abuse of Linesman",
                summaryPattern = "Assessed a late misconduct vs {OPP}, {PLAYER} ({TEAM}) shattered a stick across the glass toward the officials' tunnel.",
                evidencePattern = "Referee match misconduct sheet and bench-cam footage."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.SUBSTANCE_POLICY,
                severity = InfractionSeverity.SEVERE,
                title = "Banned Anabolic Metabolite Positive",
                summaryPattern = "Random league screening detected a prohibited anabolic recovery metabolite in {PLAYER} ({TEAM}).",
                evidencePattern = "WADA-accredited reference laboratory B-sample confirmation."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.GAMBLING_INTEGRITY,
                severity = InfractionSeverity.CRITICAL,
                title = "Proxy Hockey Slate Wagering Breach",
                summaryPattern = "Compliance audit tied a proxy sportsbook account wagering on conference totals to {PLAYER} ({TEAM}).",
                evidencePattern = "Financial forensics and NHL/League Integrity Unit affidavit."
            ),
            IncidentTemplate(
                category = DisciplinaryCategory.CONDUCT_TAMPERING,
                severity = InfractionSeverity.MINOR,
                title = "Bench Equipment Sabotage & Fine Breach",
                summaryPattern = "{PLAYER} ({TEAM}) was cited for unsportsmanlike equipment tampering during television timeouts vs {OPP}.",
                evidencePattern = "On-ice technician report and broadcaster isolated iso-angle."
            )
        )
    }

    fun formatFine(fineThousands: Int): String =
        if (fineThousands >= 1000) {
            String.format(Locale.US, "$%.2fM", fineThousands / 1000.0)
        } else {
            "\$${fineThousands}K"
        }
}
