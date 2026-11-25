package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt

enum class StakeholderPillar(
    val displayTitle: String,
    val shortLabel: String,
    val colorHex: Long,
    val roleSummary: String
) {
    OWNERS(
        displayTitle = "Owners & Board",
        shortLabel = "Owners",
        colorHex = 0xFFF59E0B,
        roleSummary = "Franchise valuations, treasury cash, governance votes & cost certainty"
    ),
    PLAYERS(
        displayTitle = "Players & Union",
        shortLabel = "Players",
        colorHex = 0xFF10B981,
        roleSummary = "CBA revenue split, strike risk, travel welfare & disciplinary fairness"
    ),
    FANS(
        displayTitle = "Fans & Gates",
        shortLabel = "Fans",
        colorHex = 0xFF06B6D4,
        roleSummary = "Arena morale, supporter loyalty, rivalries, parity & officiating trust"
    ),
    MEDIA(
        displayTitle = "Media & Broadcast",
        shortLabel = "Media",
        colorHex = 0xFF8B5CF6,
        roleSummary = "TV rights payouts, All-Star viewership, holiday marquees & crisis poise"
    )
}

enum class JobSecurityTier(
    val label: String,
    val badgeText: String,
    val colorHex: Long,
    val summary: String
) {
    IRONCLAD_MANDATE(
        label = "Ironclad Mandate",
        badgeText = "IRONCLAD",
        colorHex = 0xFF10B981,
        summary = "Commanding multi-stakeholder coalition; contract extension territory."
    ),
    SECURE_TENURE(
        label = "Secure Standing",
        badgeText = "SECURE",
        colorHex = 0xFF06B6D4,
        summary = "Broad institutional backing with manageable executive pressure points."
    ),
    UNDER_SCRUTINY(
        label = "Under Scrutiny",
        badgeText = "SCRUTINY",
        colorHex = 0xFFF59E0B,
        summary = "Restless stakeholders expect tangible wins before the annual review."
    ),
    HOT_SEAT(
        label = "Hot Seat",
        badgeText = "HOT SEAT",
        colorHex = 0xFFEF4444,
        summary = "Owners and media are questioning leadership; turnover risk is rising."
    ),
    TERMINATION_IMMINENT(
        label = "Vote of No Confidence",
        badgeText = "CRITICAL RISK",
        colorHex = 0xFFDC2626,
        summary = "Emergency coalition repair required to survive the Board confidence vote."
    );

    companion object {
        fun fromScore(score: Int): JobSecurityTier = when {
            score >= 85 -> IRONCLAD_MANDATE
            score >= 70 -> SECURE_TENURE
            score >= 55 -> UNDER_SCRUTINY
            score >= 38 -> HOT_SEAT
            else -> TERMINATION_IMMINENT
        }
    }
}

enum class LegacyTier(
    val title: String,
    val minPoints: Int,
    val nextTierPoints: Int?,
    val colorHex: Long,
    val crestSubtitle: String
) {
    INTERIM_CUSTODIAN(
        title = "Interim Custodian",
        minPoints = 0,
        nextTierPoints = 200,
        colorHex = 0xFF94A3B8,
        crestSubtitle = "Laying the organizational foundation"
    ),
    ESTABLISHED_STEWARD(
        title = "Established Steward",
        minPoints = 200,
        nextTierPoints = 400,
        colorHex = 0xFF06B6D4,
        crestSubtitle = "Trusted steward of competitive balance"
    ),
    ARCHITECT_OF_GROWTH(
        title = "Architect of Growth",
        minPoints = 400,
        nextTierPoints = 650,
        colorHex = 0xFF10B981,
        crestSubtitle = "Expanded commercial footprint & stability"
    ),
    GOLDEN_ERA_COMMISSIONER(
        title = "Golden Era Commissioner",
        minPoints = 650,
        nextTierPoints = 950,
        colorHex = 0xFFF59E0B,
        crestSubtitle = "Landmark era of ratings, parity & prestige"
    ),
    HALL_OF_FAME_ICON(
        title = "Hall of Fame Executive",
        minPoints = 950,
        nextTierPoints = null,
        colorHex = 0xFF8B5CF6,
        crestSubtitle = "Immortalized architect of modern league history"
    );

    companion object {
        fun fromPoints(points: Int): LegacyTier =
            values().lastOrNull { points >= it.minPoints } ?: INTERIM_CUSTODIAN
    }
}

enum class PerformanceReviewGrade(
    val letter: String,
    val title: String,
    val colorHex: Long
) {
    A_PLUS("A+", "Historic Executive Mastery", 0xFF10B981),
    A("A", "Premier League Leadership", 0xFF10B981),
    B_PLUS("B+", "Strong Multi-Party Stewardship", 0xFF06B6D4),
    B("B", "Solid Operational Tenure", 0xFF06B6D4),
    C("C", "Mixed Review — Friction Emerging", 0xFFF59E0B),
    D("D", "Formal Board Probation", 0xFFEF4444),
    F("F", "No-Confidence Crisis", 0xFFDC2626);

    companion object {
        fun fromScore(score: Int): PerformanceReviewGrade = when {
            score >= 91 -> A_PLUS
            score >= 83 -> A
            score >= 76 -> B_PLUS
            score >= 68 -> B
            score >= 56 -> C
            score >= 43 -> D
            else -> F
        }
    }
}

enum class CommissionerContractAction(
    val label: String,
    val description: String,
    val colorHex: Long
) {
    MULTI_YEAR_EXTENSION(
        label = "Multi-Year Extension Awarded",
        description = "Board voted to extend commissioner contract (+2 yrs) with expanded mandate.",
        colorHex = 0xFF10B981
    ),
    MANDATE_RENEWED(
        label = "Annual Mandate Renewed",
        description = "Coalition affirmed confidence for the upcoming league calendar.",
        colorHex = 0xFF06B6D4
    ),
    CONDITIONAL_PROBATION(
        label = "Conditional Probation Issued",
        description = "Owners placed tenure on formal watch pending stakeholder stabilization.",
        colorHex = 0xFFF59E0B
    ),
    HOT_SEAT_ULTIMATUM(
        label = "Hot-Seat Performance Ultimatum",
        description = "Board trimmed term cushion and demanded immediate turnaround benchmarks.",
        colorHex = 0xFFEF4444
    ),
    SURVIVED_CONFIDENCE_VOTE(
        label = "Narrow Confidence Survival",
        description = "Survived an emergency confidence motion via final-hour board concessions.",
        colorHex = 0xFFDC2626
    )
}

enum class ReviewResponseChoice(
    val title: String,
    val pillar: StakeholderPillar,
    val impactSummary: String,
    val badgeTag: String
) {
    OWNER_GROWTH_CHARTER(
        title = "Sign Owner Margin & Valuation Charter",
        pillar = StakeholderPillar.OWNERS,
        impactSummary = "+6 Owner Approval • +\$12M Enterprise Valuation • +3 Job Security",
        badgeTag = "OWNER PLEDGE"
    ),
    PLAYER_PARTNERSHIP_ACCORD(
        title = "Ratify Player Welfare & Fairness Accord",
        pillar = StakeholderPillar.PLAYERS,
        impactSummary = "+7 Union Satisfaction • -6% Strike Risk • +4 Precedent Consistency",
        badgeTag = "PLAYERS PLEDGE"
    ),
    FAN_ACCESS_BLITZ(
        title = "Fund Fan-First Pricing & Rivalry Blitz",
        pillar = StakeholderPillar.FANS,
        impactSummary = "+5 Fan Morale • +4 Fan Loyalty • +0.4M Fanbase (-\$4M Cash)",
        badgeTag = "FANS PLEDGE"
    ),
    MEDIA_SPECTACLE_SHOWCASE(
        title = "Unveil Prime-Time Media & All-Star Compact",
        pillar = StakeholderPillar.MEDIA,
        impactSummary = "+5 Broadcast Reach • +\$3M Sponsor Buzz • +4 Media Standing",
        badgeTag = "MEDIA PLEDGE"
    )
}

data class StakeholderPillarReport(
    val pillar: StakeholderPillar,
    val score: Int,
    val statusLabel: String,
    val headlineDriver: String,
    val positiveDrivers: List<String>,
    val riskDrivers: List<String>
)

data class StakeholderScoreBreakdown(
    val ownersReport: StakeholderPillarReport,
    val playersReport: StakeholderPillarReport,
    val fansReport: StakeholderPillarReport,
    val mediaReport: StakeholderPillarReport,
    val compositeApprovalScore: Int,
    val projectedJobSecurityScore: Int,
    val activeRiskAlerts: List<String>,
    val backgroundPerkNote: String
) {
    val ownersScore: Int get() = ownersReport.score
    val playersScore: Int get() = playersReport.score
    val fansScore: Int get() = fansReport.score
    val mediaScore: Int get() = mediaReport.score

    val allReports: List<StakeholderPillarReport>
        get() = listOf(ownersReport, playersReport, fansReport, mediaReport)
}

data class StakeholderReviewVerdict(
    val pillar: StakeholderPillar,
    val representativeName: String,
    val representativeTitle: String,
    val score: Int,
    val gradeLetter: String,
    val quote: String,
    val keyFactors: List<String>
)

data class AnnualPerformanceReview(
    val id: String,
    val seasonYear: Int,
    val ownersScore: Int,
    val playersScore: Int,
    val fansScore: Int,
    val mediaScore: Int,
    val compositeApprovalScore: Int,
    val overallGrade: PerformanceReviewGrade,
    val jobSecurityBefore: Int,
    val jobSecurityAfter: Int,
    val jobSecurityDelta: Int,
    val legacyPointsAwarded: Int,
    val legacyTotalAfter: Int,
    val contractAction: CommissionerContractAction,
    val leagueValuationSnapshotMillions: Long,
    val cashSnapshotMillions: Long,
    val fanbaseSnapshotMillions: Double,
    val championTeamName: String,
    val verdicts: List<StakeholderReviewVerdict>,
    val strategicMandateNextYear: String,
    val commissionerResponseChosen: ReviewResponseChoice? = null,
    val isRatified: Boolean = false
)

data class ReviewRatificationOutcome(
    val updatedState: JobSecurityLegacyState,
    val cashDeltaMillions: Long,
    val valuationDeltaMillions: Long,
    val fanbaseDeltaMillions: Double,
    val ownerApprovalDelta: Int,
    val unionSatisfactionDelta: Int,
    val strikeRiskDelta: Int,
    val fanMoraleDelta: Int,
    val fanLoyaltyDelta: Int,
    val headline: String,
    val feedbackMessage: String
)

data class JobSecurityLegacyState(
    val jobSecurityScore: Int = 78,
    val legacyPoints: Int = 120,
    val contractYearsRemaining: Int = 3,
    val seasonsServed: Int = 0,
    val extensionsEarned: Int = 0,
    val warningsIssued: Int = 0,
    val latestBreakdown: StakeholderScoreBreakdown,
    val pendingAnnualReview: AnnualPerformanceReview? = null,
    val reviewHistory: List<AnnualPerformanceReview> = emptyList(),
    val lastUpdateHeadline: String = "Inaugural Board mandate established across Owners, Players, Fans & Media.",
    val milestones: List<LeagueLegacyMilestone> = emptyList(),
    val unlockedMilestonesCount: Int = 0,
    val recordBroadcastAnnualMillions: Long = 65L,
    val consecutiveLockoutFreeSeasons: Int = 0,
    val maxConsecutiveChampionStreak: Int = 0,
    val dynastyTeamName: String? = null
) {
    val securityTier: JobSecurityTier
        get() = JobSecurityTier.fromScore(jobSecurityScore)

    val legacyTier: LegacyTier
        get() = LegacyTier.fromPoints(legacyPoints)

    val legacyProgressToNextTier: Float
        get() {
            val tier = legacyTier
            val nextPts = tier.nextTierPoints ?: return 1f
            val span = (nextPts - tier.minPoints).coerceAtLeast(1)
            return ((legacyPoints - tier.minPoints).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }

    val legacyMeterPct: Int
        get() = ((legacyPoints.toDouble() / 1000.0) * 100.0).roundToInt().coerceIn(0, 100)
}

data class CommissionerPerformanceInput(
    val seasonYear: Int,
    val currentWeek: Int,
    val sportType: SportType,
    val background: CommissionerBackground,
    val cashReservesMillions: Long,
    val leagueValuationMillions: Long,
    val totalFanbaseMillions: Double,
    val leagueIntegrityScore: Int,
    val competitiveParityScore: Int,
    val ownerApprovalRating: Int,
    val unionAgreement: LaborUnionAgreement,
    val activeBroadcast: BroadcastContract,
    val activeSponsorship: ActiveLeagueSponsorship,
    val ruleBook: LeagueRuleBook,
    val franchises: List<Franchise>,
    val cities: List<MarketCity>,
    val campaigns: List<MarketingCampaign>,
    val rivalries: List<TeamRivalry>,
    val boardVoteHistory: List<BoardVoteResult>,
    val officiatingState: OfficiatingState,
    val disciplinaryState: DisciplinaryDeskState,
    val crisisState: CrisisSystemState,
    val allStarRedesignState: AllStarRedesignState,
    val ruleCommitteeState: RuleCommitteeState,
    val seasonScheduleState: SeasonScheduleState,
    val hallOfFameCount: Int,
    val recordCount: Int,
    val valuationHistory: List<LeagueValuationHistory>,
    val seasonChampionName: String? = null
)

object JobSecurityLegacyEngine {

    fun createInitialState(input: CommissionerPerformanceInput): JobSecurityLegacyState {
        val breakdown = evaluateStakeholders(input)
        val initialSecurity = breakdown.projectedJobSecurityScore.coerceIn(68, 86)
        val base = JobSecurityLegacyState(
            jobSecurityScore = initialSecurity,
            legacyPoints = 120,
            contractYearsRemaining = 3,
            seasonsServed = 0,
            extensionsEarned = 0,
            warningsIssued = 0,
            latestBreakdown = breakdown,
            pendingAnnualReview = null,
            reviewHistory = emptyList(),
            lastUpdateHeadline = "Board confidence initialized at $initialSecurity% across all 4 league pillars.",
            recordBroadcastAnnualMillions = input.activeBroadcast.annualPayoutMillions
        )
        return LeagueLegacyTracker.evaluateAndUnlockMilestones(base, input, isSeasonRollover = false).updatedState
    }

    fun evaluateStakeholders(input: CommissionerPerformanceInput): StakeholderScoreBreakdown {
        val franchises = input.franchises
        val avgOwnerGroupApproval = if (franchises.isNotEmpty()) {
            franchises.map { it.ownership.ownerApproval }.average()
        } else 80.0
        val avgMorale = if (franchises.isNotEmpty()) franchises.map { it.fanMorale }.average() else 75.0
        val avgLoyalty = if (franchises.isNotEmpty()) franchises.map { it.fanLoyalty }.average() else 75.0

        // ---------------- 1. OWNERS & BOARD ----------------
        val valuationScore = (65.0 + ((input.leagueValuationMillions - 420L).toDouble() / 16.0)).coerceIn(30.0, 99.0)
        val cashHealthScore = when {
            input.cashReservesMillions >= 160L -> 94.0
            input.cashReservesMillions >= 95L -> 84.0
            input.cashReservesMillions >= 45L -> 72.0
            input.cashReservesMillions >= 15L -> 55.0
            else -> 32.0
        }
        val recentVotes = input.boardVoteHistory.take(10)
        val overrideCount = recentVotes.count { it.enactedViaCommissionerOverride }
        val votePassRateScore = if (recentVotes.isNotEmpty()) {
            val passedPct = recentVotes.count { it.passed && !it.enactedViaCommissionerOverride }.toDouble() / recentVotes.size
            (58.0 + passedPct * 38.0 - overrideCount * 6.0).coerceIn(30.0, 99.0)
        } else 80.0
        val ownerBgBonus = when (input.background) {
            CommissionerBackground.WALL_STREET_FINANCIER -> 4.0
            CommissionerBackground.ANTITRUST_ATTORNEY -> 2.0
            else -> 0.0
        }
        val rawOwners = (input.ownerApprovalRating * 0.36) +
            (avgOwnerGroupApproval * 0.24) +
            (valuationScore * 0.20) +
            (cashHealthScore * 0.12) +
            (votePassRateScore * 0.08) +
            ownerBgBonus
        val ownersScore = rawOwners.roundToInt().coerceIn(15, 99)

        val ownerPositives = buildList {
            if (input.leagueValuationMillions >= 450L) add("Enterprise valuation climbing to \$${input.leagueValuationMillions}M")
            if (input.cashReservesMillions >= 80L) add("Healthy league treasury (\$${input.cashReservesMillions}M reserves)")
            if (recentVotes.any { it.passed && !it.enactedViaCommissionerOverride }) add("Strong Board ballot coalition on key resolutions")
            if (franchises.size > 6) add("Expansion growth to ${franchises.size} member franchises")
            if (isEmpty()) add("Baseline franchise equity remains stable")
        }.take(3)
        val ownerRisks = buildList {
            if (input.cashReservesMillions < 25L) add("Thin liquidity buffer (\$${input.cashReservesMillions}M cash)")
            if (overrideCount > 0) add("$overrideCount executive veto override(s) strained Governor trust")
            if (avgOwnerGroupApproval < 68.0) add("Franchise governors restless (${avgOwnerGroupApproval.roundToInt()}% avg club approval)")
        }.take(2)

        // ---------------- 2. PLAYERS & UNION ----------------
        val union = input.unionAgreement
        val strikePeaceScore = (100.0 - (union.strikeRisk * 0.92)).coerceIn(10.0, 99.0)
        val welfareCount = listOf(
            union.mandatoryPrivateCharterFlights,
            union.maxBackToBackGamesLimit,
            union.independentConcussionProtocol
        ).count { it }
        val welfareScore = 62.0 + (welfareCount * 11.0)
        val revSplitScore = when {
            union.playerRevenueSharePct >= 52 -> 94.0
            union.playerRevenueSharePct >= 50 -> 86.0
            union.playerRevenueSharePct >= 48 -> 72.0
            else -> 54.0
        }
        val cbaTermScore = when {
            union.cbaYearsRemaining >= 3 -> 90.0
            union.cbaYearsRemaining == 2 -> 80.0
            union.cbaYearsRemaining == 1 -> 66.0
            else -> 40.0
        }
        val pendingAppeals = input.disciplinaryState.appealedCases.size
        val disciplinaryFairness = (input.disciplinaryState.precedentConsistencyScore - (pendingAppeals * 6)).coerceIn(25, 100)
        val unresolvedInjuryHoles = franchises.sumOf { InjuryEngine.countUnresolvedStarterHoles(it) }
        val playerBgBonus = when (input.background) {
            CommissionerBackground.FORMER_LEGEND -> 5.0
            CommissionerBackground.ANTITRUST_ATTORNEY -> 2.0
            else -> 0.0
        }
        val rawPlayers = (union.unionSatisfaction * 0.34) +
            (strikePeaceScore * 0.24) +
            (revSplitScore * 0.14) +
            (welfareScore * 0.10) +
            (cbaTermScore * 0.08) +
            (disciplinaryFairness * 0.10) -
            (unresolvedInjuryHoles * 2.5) +
            playerBgBonus
        val playersScore = rawPlayers.roundToInt().coerceIn(15, 99)

        val playerPositives = buildList {
            if (union.strikeRisk <= 25) add("Low labor tension (${union.strikeRisk}% strike risk)")
            if (welfareCount >= 2) add("$welfareCount/3 player welfare protections active")
            if (input.disciplinaryState.precedentConsistencyScore >= 82) add("Consistent tribunal precedent (${input.disciplinaryState.precedentConsistencyScore}%)")
            if (union.playerRevenueSharePct >= 50) add("Player-aligned ${union.playerRevenueSharePct}% BRI split")
            if (isEmpty()) add("Active CBA framework keeping locker rooms focused")
        }.take(3)
        val playerRisks = buildList {
            if (union.strikeRisk >= 45) add("Elevated PA strike risk (${union.strikeRisk}%)")
            if (union.cbaYearsRemaining <= 1) add("CBA horizon urgent (${union.cbaYearsRemaining} yr remaining)")
            if (pendingAppeals > 0) add("$pendingAppeals unresolved union arbitration appeal(s)")
            if (unresolvedInjuryHoles > 0) add("$unresolvedInjuryHoles unpatched lineup injury hole(s)")
        }.take(2)

        // ---------------- 3. FANS & GATES ----------------
        val fanSentiment = (avgMorale * 0.55) + (avgLoyalty * 0.45)
        val activeRivalriesCount = input.rivalries.count { it.isActiveRivalry }
        val rivalryHeatBonus = (activeRivalriesCount * 2.0).coerceAtMost(10.0)
        val unresolvedBlownCalls = input.officiatingState.activeUnresolvedIncidents.size
        val officiatingTrustScore = (input.officiatingState.leagueCallAccuracyPct - (unresolvedBlownCalls * 7.0)).coerceIn(30.0, 100.0)
        val activeCampaignBoost = (input.campaigns.count { it.isActive } * 2.5).coerceAtMost(8.0)
        val fanBgBonus = when (input.background) {
            CommissionerBackground.MARKETING_MAVEN -> 4.0
            else -> 0.0
        }
        val rawFans = (fanSentiment * 0.50) +
            (input.competitiveParityScore * 0.22) +
            (officiatingTrustScore * 0.16) +
            rivalryHeatBonus +
            activeCampaignBoost +
            fanBgBonus
        val fansScore = rawFans.roundToInt().coerceIn(15, 99)

        val fanPositives = buildList {
            if (avgLoyalty >= 75.0) add("Strong arena retention (${avgLoyalty.roundToInt()}% avg loyalty)")
            if (input.competitiveParityScore >= 78) add("Tight league parity (${input.competitiveParityScore}/100)")
            if (activeRivalriesCount >= 2) add("$activeRivalriesCount heated rivalries boosting ticket demand")
            if (input.officiatingState.leagueCallAccuracyPct >= 86) add("High officiating accuracy (${input.officiatingState.leagueCallAccuracyPct}%)")
            if (isEmpty()) add("Steady global fan footprint (${input.totalFanbaseMillions}M)")
        }.take(3)
        val fanRisks = buildList {
            if (unresolvedBlownCalls > 0) add("$unresolvedBlownCalls unaddressed blown-call controversy")
            if (avgMorale < 65.0) add("Supporter morale dipping (${avgMorale.roundToInt()}%)")
            if (input.competitiveParityScore < 68) add("Competitive imbalance hurting small-market interest")
        }.take(2)

        // ---------------- 4. MEDIA & BROADCAST ----------------
        val allStarImpact = AllStarEngine.computeProjectedImpact(
            redesignState = input.allStarRedesignState,
            franchises = franchises,
            activeBroadcast = input.activeBroadcast,
            cities = input.cities,
            ruleBook = input.ruleBook
        )
        val broadcastValueScore = (62.0 + ((input.activeBroadcast.annualPayoutMillions - 16L).toDouble() * 1.1)).coerceIn(45.0, 99.0)
        val allStarRatingScore = (58.0 + (allStarImpact.projectedViewersMillions * 3.8)).coerceIn(45.0, 99.0)
        val schedulePlan = input.seasonScheduleState.activeSeasonSchedule ?: input.seasonScheduleState.upcomingPlan
        val tentpoleCount = (schedulePlan?.holidayShowcaseCount ?: 0) + (schedulePlan?.internationalGameCount ?: 0)
        val tentpoleScore = (66.0 + tentpoleCount * 4.0).coerceIn(60.0, 96.0)
        val activeRuleCount = input.ruleCommitteeState.enactedProposals.size
        val scandalPenalty = (input.crisisState.scheduledBranches.size * 3.0) + (unresolvedBlownCalls * 4.5)
        val mediaBgBonus = when (input.background) {
            CommissionerBackground.MARKETING_MAVEN -> 5.0
            CommissionerBackground.ANTITRUST_ATTORNEY -> 2.0
            else -> 0.0
        }
        val rawMedia = (broadcastValueScore * 0.30) +
            (input.leagueIntegrityScore * 0.28) +
            (allStarRatingScore * 0.22) +
            (tentpoleScore * 0.14) +
            (activeRuleCount * 2.0).coerceAtMost(6.0) -
            scandalPenalty +
            mediaBgBonus
        val mediaScore = rawMedia.roundToInt().coerceIn(15, 99)

        val mediaPositives = buildList {
            add("${input.activeBroadcast.partnerName} deal (\$${input.activeBroadcast.annualPayoutMillions}M/yr)")
            if (input.leagueIntegrityScore >= 80) add("Clean institutional brand (${input.leagueIntegrityScore}/100 integrity)")
            if (allStarImpact.projectedViewersMillions >= 6.5) add("All-Star TV draw projected at ${allStarImpact.projectedViewersMillions}M (${allStarImpact.tvRatingsGrade})")
            if (tentpoleCount >= 2) add("$tentpoleCount holiday & global TV tentpoles booked")
        }.take(3)
        val mediaRisks = buildList {
            if (input.leagueIntegrityScore < 70) add("Brand integrity damaged (${input.leagueIntegrityScore}/100)")
            if (input.activeBroadcast.yearsRemaining <= 1) add("TV rights renewal window approaching (${input.activeBroadcast.yearsRemaining}y left)")
            if (input.crisisState.scheduledBranches.isNotEmpty()) add("${input.crisisState.scheduledBranches.size} ongoing media crisis thread(s)")
        }.take(2)

        // Composite & Projected Job Security
        val composite = ((ownersScore * 0.32) + (playersScore * 0.24) + (fansScore * 0.24) + (mediaScore * 0.20))
            .roundToInt()
            .coerceIn(10, 99)

        val criticalPenalty = buildList {
            if (union.strikeRisk >= 75) add(8)
            if (input.cashReservesMillions < 15L) add(6)
            if (input.leagueIntegrityScore < 50) add(7)
            if (input.officiatingState.unionState.lockoutRisk >= 75) add(5)
        }.sum()

        val projectedSecurity = (composite - criticalPenalty).coerceIn(12, 99)

        val activeRiskAlerts = buildList {
            if (union.strikeRisk >= 55) add("PA Strike Risk at ${union.strikeRisk}% threatens work stoppage")
            if (input.cashReservesMillions < 20L) add("League cash critically low (\$${input.cashReservesMillions}M)")
            if (unresolvedBlownCalls > 0) add("$unresolvedBlownCalls officiating scandal(s) fueling fan & media backlash")
            if (overrideCount >= 2) add("Frequent Board veto overrides eroding Governor confidence")
            if (input.leagueIntegrityScore < 65) add("League Integrity (${input.leagueIntegrityScore}/100) under scrutiny")
        }

        val bgNote = when (input.background) {
            CommissionerBackground.MARKETING_MAVEN -> "Perk active: Media & Marketing Titan buffers Fan & Broadcast metrics."
            CommissionerBackground.WALL_STREET_FINANCIER -> "Perk active: Financial Strategist anchors Owner & Valuation confidence."
            CommissionerBackground.FORMER_LEGEND -> "Perk active: Former MVP Athlete stabilizes Player & CBA alignment."
            CommissionerBackground.ANTITRUST_ATTORNEY -> "Perk active: Constitutional Counsel reinforces Owner, Union & Integrity standing."
        }

        return StakeholderScoreBreakdown(
            ownersReport = StakeholderPillarReport(
                pillar = StakeholderPillar.OWNERS,
                score = ownersScore,
                statusLabel = statusTextForScore(ownersScore),
                headlineDriver = ownerPositives.first(),
                positiveDrivers = ownerPositives,
                riskDrivers = ownerRisks
            ),
            playersReport = StakeholderPillarReport(
                pillar = StakeholderPillar.PLAYERS,
                score = playersScore,
                statusLabel = statusTextForScore(playersScore),
                headlineDriver = playerPositives.first(),
                positiveDrivers = playerPositives,
                riskDrivers = playerRisks
            ),
            fansReport = StakeholderPillarReport(
                pillar = StakeholderPillar.FANS,
                score = fansScore,
                statusLabel = statusTextForScore(fansScore),
                headlineDriver = fanPositives.first(),
                positiveDrivers = fanPositives,
                riskDrivers = fanRisks
            ),
            mediaReport = StakeholderPillarReport(
                pillar = StakeholderPillar.MEDIA,
                score = mediaScore,
                statusLabel = statusTextForScore(mediaScore),
                headlineDriver = mediaPositives.first(),
                positiveDrivers = mediaPositives,
                riskDrivers = mediaRisks
            ),
            compositeApprovalScore = composite,
            projectedJobSecurityScore = projectedSecurity,
            activeRiskAlerts = activeRiskAlerts,
            backgroundPerkNote = bgNote
        )
    }

    fun recalculateLiveSecurity(
        current: JobSecurityLegacyState,
        input: CommissionerPerformanceInput,
        eventNote: String? = null,
        isSeasonRollover: Boolean = false
    ): JobSecurityLegacyState {
        val breakdown = evaluateStakeholders(input)
        val target = breakdown.projectedJobSecurityScore
        val blended = ((current.jobSecurityScore * 0.45) + (target * 0.55)).roundToInt().coerceIn(10, 99)
        val refreshedPending = current.pendingAnnualReview?.let { pending ->
            if (!pending.isRatified && pending.seasonYear == input.seasonYear) {
                pending.copy(
                    ownersScore = breakdown.ownersScore,
                    playersScore = breakdown.playersScore,
                    fansScore = breakdown.fansScore,
                    mediaScore = breakdown.mediaScore,
                    compositeApprovalScore = breakdown.compositeApprovalScore,
                    overallGrade = PerformanceReviewGrade.fromScore(breakdown.compositeApprovalScore)
                )
            } else pending
        }
        val intermediate = current.copy(
            jobSecurityScore = blended,
            latestBreakdown = breakdown,
            pendingAnnualReview = refreshedPending,
            lastUpdateHeadline = eventNote ?: current.lastUpdateHeadline
        )
        return LeagueLegacyTracker.evaluateAndUnlockMilestones(
            current = intermediate,
            input = input,
            isSeasonRollover = isSeasonRollover
        ).updatedState
    }

    fun generateAnnualReview(
        current: JobSecurityLegacyState,
        input: CommissionerPerformanceInput
    ): AnnualPerformanceReview {
        val breakdown = evaluateStakeholders(input)
        val composite = breakdown.compositeApprovalScore
        val grade = PerformanceReviewGrade.fromScore(composite)

        val securityShift = when {
            composite >= 90 -> 8
            composite >= 82 -> 5
            composite >= 74 -> 2
            composite >= 65 -> 0
            composite >= 55 -> -4
            else -> -9
        }
        val securityBefore = current.jobSecurityScore
        val securityAfter = ((securityBefore * 0.35) + (breakdown.projectedJobSecurityScore * 0.65) + securityShift * 0.4)
            .roundToInt()
            .coerceIn(12, 99)

        // Legacy Points Formula: performance + valuation milestone + parity + expansion + HOF prestige
        val baseLegacyFromGrade = when (grade) {
            PerformanceReviewGrade.A_PLUS -> 95
            PerformanceReviewGrade.A -> 78
            PerformanceReviewGrade.B_PLUS -> 64
            PerformanceReviewGrade.B -> 50
            PerformanceReviewGrade.C -> 32
            PerformanceReviewGrade.D -> 16
            PerformanceReviewGrade.F -> 5
        }
        val expansionBonus = ((input.franchises.size - 6).coerceAtLeast(0)) * 6
        val growthBonus = ((input.leagueValuationMillions - 420L) / 80L).toInt().coerceIn(0, 35)
        val integrityBonus = if (input.leagueIntegrityScore >= 85) 12 else if (input.leagueIntegrityScore >= 75) 5 else 0
        val parityBonus = if (input.competitiveParityScore >= 80) 10 else 0
        val legacyAwarded = (baseLegacyFromGrade + expansionBonus + growthBonus + integrityBonus + parityBonus).coerceAtLeast(5)
        val legacyTotalAfter = current.legacyPoints + legacyAwarded

        val contractAction = when {
            composite >= 82 && securityAfter >= 80 -> CommissionerContractAction.MULTI_YEAR_EXTENSION
            composite >= 68 && securityAfter >= 62 -> CommissionerContractAction.MANDATE_RENEWED
            composite >= 54 -> CommissionerContractAction.CONDITIONAL_PROBATION
            securityAfter >= 38 -> CommissionerContractAction.HOT_SEAT_ULTIMATUM
            else -> CommissionerContractAction.SURVIVED_CONFIDENCE_VOTE
        }

        val leadOwner = input.franchises.maxByOrNull { it.ownership.netWorthBillions }?.ownership?.primaryOwner
        val championLabel = input.seasonChampionName ?: "League Champion TBD"

        val verdicts = listOf(
            StakeholderReviewVerdict(
                pillar = StakeholderPillar.OWNERS,
                representativeName = leadOwner?.fullName ?: "Board Executive Committee",
                representativeTitle = leadOwner?.roleTitle ?: "Chair of the Board of Governors",
                score = breakdown.ownersScore,
                gradeLetter = PerformanceReviewGrade.fromScore(breakdown.ownersScore).letter,
                quote = when {
                    breakdown.ownersScore >= 82 -> "Franchise asset growth to \$${input.leagueValuationMillions}M validates your capital stewardship."
                    breakdown.ownersScore >= 65 -> "Solid balance-sheet discipline, though governors watch cost controls closely."
                    else -> "Ownership confidence is fractured — margin protection and board consensus must improve."
                },
                keyFactors = breakdown.ownersReport.positiveDrivers + breakdown.ownersReport.riskDrivers
            ),
            StakeholderReviewVerdict(
                pillar = StakeholderPillar.PLAYERS,
                representativeName = "Marcus Vance",
                representativeTitle = "Players Association Executive Director",
                score = breakdown.playersScore,
                gradeLetter = PerformanceReviewGrade.fromScore(breakdown.playersScore).letter,
                quote = when {
                    breakdown.playersScore >= 82 -> "With ${input.unionAgreement.strikeRisk}% strike risk and fair tribunal standards, locker rooms back league stability."
                    breakdown.playersScore >= 65 -> "CBA dialogue remains workable, but player health and revenue-split terms remain active priorities."
                    else -> "Players are mobilizing against cap rigidity and disciplinary disparities."
                },
                keyFactors = breakdown.playersReport.positiveDrivers + breakdown.playersReport.riskDrivers
            ),
            StakeholderReviewVerdict(
                pillar = StakeholderPillar.FANS,
                representativeName = "Supporter Coalition Audit",
                representativeTitle = "${input.totalFanbaseMillions}M Global Fanbase Index",
                score = breakdown.fansScore,
                gradeLetter = PerformanceReviewGrade.fromScore(breakdown.fansScore).letter,
                quote = when {
                    breakdown.fansScore >= 82 -> "Packed arenas, balanced parity (${input.competitiveParityScore}/100), and electric rivalries kept gates surging."
                    breakdown.fansScore >= 65 -> "Ticket demand is steady, though officiating consistency and close races matter next season."
                    else -> "Supporters are tuning out amid blown calls and lopsided standings."
                },
                keyFactors = breakdown.fansReport.positiveDrivers + breakdown.fansReport.riskDrivers
            ),
            StakeholderReviewVerdict(
                pillar = StakeholderPillar.MEDIA,
                representativeName = input.activeBroadcast.partnerName,
                representativeTitle = "National Rights & Sponsor Consortium",
                score = breakdown.mediaScore,
                gradeLetter = PerformanceReviewGrade.fromScore(breakdown.mediaScore).letter,
                quote = when {
                    breakdown.mediaScore >= 82 -> "Prime-time windows, holiday tentpoles, and clean league integrity make this a flagship property."
                    breakdown.mediaScore >= 65 -> "Broadcast ratings hold firm; stronger All-Star engagement will unlock bigger renewals."
                    else -> "Advertisers and network executives are demanding cleaner crisis governance and marquee juice."
                },
                keyFactors = breakdown.mediaReport.positiveDrivers + breakdown.mediaReport.riskDrivers
            )
        )

        val lowestPillar = breakdown.allReports.minByOrNull { it.score }?.pillar ?: StakeholderPillar.OWNERS
        val mandateNextYear = when (lowestPillar) {
            StakeholderPillar.OWNERS -> "Board Mandate: Strengthen cash reserves, protect franchise valuations, and secure unanimous Board votes."
            StakeholderPillar.PLAYERS -> "Board Mandate: Reduce strike risk below 25%, reinforce CBA peace, and maintain consistent tribunal rulings."
            StakeholderPillar.FANS -> "Board Mandate: Elevate arena morale, sharpen officiating accuracy, and spotlight marquee rivalries."
            StakeholderPillar.MEDIA -> "Board Mandate: Grow broadcast valuation, elevate All-Star viewership, and resolve narrative crises swiftly."
        }

        return AnnualPerformanceReview(
            id = "review_${input.seasonYear}_${abs((composite * 31) + input.franchises.size)}",
            seasonYear = input.seasonYear,
            ownersScore = breakdown.ownersScore,
            playersScore = breakdown.playersScore,
            fansScore = breakdown.fansScore,
            mediaScore = breakdown.mediaScore,
            compositeApprovalScore = composite,
            overallGrade = grade,
            jobSecurityBefore = securityBefore,
            jobSecurityAfter = securityAfter,
            jobSecurityDelta = securityAfter - securityBefore,
            legacyPointsAwarded = legacyAwarded,
            legacyTotalAfter = legacyTotalAfter,
            contractAction = contractAction,
            leagueValuationSnapshotMillions = input.leagueValuationMillions,
            cashSnapshotMillions = input.cashReservesMillions,
            fanbaseSnapshotMillions = input.totalFanbaseMillions,
            championTeamName = championLabel,
            verdicts = verdicts,
            strategicMandateNextYear = mandateNextYear,
            commissionerResponseChosen = null,
            isRatified = false
        )
    }

    fun ratifyAnnualReview(
        current: JobSecurityLegacyState,
        review: AnnualPerformanceReview,
        response: ReviewResponseChoice?
    ): ReviewRatificationOutcome {
        val chosen = response ?: ReviewResponseChoice.OWNER_GROWTH_CHARTER
        var cashDelta = 0L
        var valDelta = 0L
        var fanbaseDelta = 0.0
        var ownerDelta = 0
        var unionSatDelta = 0
        var strikeDelta = 0
        var fanMoraleDelta = 0
        var fanLoyaltyDelta = 0
        var securityBonus = 2
        var legacyBonus = 8

        when (chosen) {
            ReviewResponseChoice.OWNER_GROWTH_CHARTER -> {
                ownerDelta = 6
                valDelta = 12L
                unionSatDelta = -2
                securityBonus = 3
                legacyBonus = 10
            }
            ReviewResponseChoice.PLAYER_PARTNERSHIP_ACCORD -> {
                unionSatDelta = 7
                strikeDelta = -6
                securityBonus = 3
                legacyBonus = 10
            }
            ReviewResponseChoice.FAN_ACCESS_BLITZ -> {
                cashDelta = -4L
                fanMoraleDelta = 5
                fanLoyaltyDelta = 4
                fanbaseDelta = 0.4
                securityBonus = 2
                legacyBonus = 10
            }
            ReviewResponseChoice.MEDIA_SPECTACLE_SHOWCASE -> {
                valDelta = 8L
                fanbaseDelta = 0.5
                ownerDelta = 2
                securityBonus = 3
                legacyBonus = 12
            }
        }

        val finalSecurity = (review.jobSecurityAfter + securityBonus).coerceIn(15, 99)
        val finalLegacyAward = review.legacyPointsAwarded + legacyBonus
        val nextLegacyTotal = current.legacyPoints + finalLegacyAward
        val ratified = review.copy(
            jobSecurityAfter = finalSecurity,
            jobSecurityDelta = finalSecurity - review.jobSecurityBefore,
            legacyPointsAwarded = finalLegacyAward,
            legacyTotalAfter = nextLegacyTotal,
            commissionerResponseChosen = chosen,
            isRatified = true
        )

        val existingFiltered = current.reviewHistory.filterNot { it.seasonYear == ratified.seasonYear }
        val updatedHistory = (listOf(ratified) + existingFiltered).take(20)

        val nextContractYears = when (ratified.contractAction) {
            CommissionerContractAction.MULTI_YEAR_EXTENSION -> (current.contractYearsRemaining + 1).coerceIn(3, 5)
            CommissionerContractAction.MANDATE_RENEWED -> current.contractYearsRemaining.coerceAtLeast(2)
            CommissionerContractAction.CONDITIONAL_PROBATION -> (current.contractYearsRemaining - 1).coerceAtLeast(1)
            CommissionerContractAction.HOT_SEAT_ULTIMATUM,
            CommissionerContractAction.SURVIVED_CONFIDENCE_VOTE -> 1
        }
        val gainedExtension = if (ratified.contractAction == CommissionerContractAction.MULTI_YEAR_EXTENSION) 1 else 0
        val gainedWarning = if (
            ratified.contractAction == CommissionerContractAction.CONDITIONAL_PROBATION ||
            ratified.contractAction == CommissionerContractAction.HOT_SEAT_ULTIMATUM ||
            ratified.contractAction == CommissionerContractAction.SURVIVED_CONFIDENCE_VOTE
        ) 1 else 0

        val headline = "ANNUAL REVIEW Y${ratified.seasonYear} [${ratified.overallGrade.letter}]: ${ratified.contractAction.label} • Job Security $finalSecurity% • +$finalLegacyAward Legacy Pts (${chosen.badgeTag})."
        val feedback = "Ratified ${ratified.seasonYear} Review (${ratified.overallGrade.letter}, $finalSecurity% Security, +$finalLegacyAward Legacy) with '${chosen.title}'."

        val nextState = current.copy(
            jobSecurityScore = finalSecurity,
            legacyPoints = nextLegacyTotal,
            contractYearsRemaining = nextContractYears,
            seasonsServed = current.seasonsServed + 1,
            extensionsEarned = current.extensionsEarned + gainedExtension,
            warningsIssued = current.warningsIssued + gainedWarning,
            pendingAnnualReview = null,
            reviewHistory = updatedHistory,
            lastUpdateHeadline = headline
        )

        return ReviewRatificationOutcome(
            updatedState = nextState,
            cashDeltaMillions = cashDelta,
            valuationDeltaMillions = valDelta,
            fanbaseDeltaMillions = fanbaseDelta,
            ownerApprovalDelta = ownerDelta,
            unionSatisfactionDelta = unionSatDelta,
            strikeRiskDelta = strikeDelta,
            fanMoraleDelta = fanMoraleDelta,
            fanLoyaltyDelta = fanLoyaltyDelta,
            headline = headline,
            feedbackMessage = feedback
        )
    }

    private fun statusTextForScore(score: Int): String = when {
        score >= 86 -> "Thriving"
        score >= 74 -> "Supportive"
        score >= 60 -> "Watchful"
        score >= 45 -> "Restless"
        else -> "Hostile"
    }
}
