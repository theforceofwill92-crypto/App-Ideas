package com.example.model

enum class LaborTriggerType(val label: String) {
    CBA_EXPIRED("CBA Expired — Mandatory Renewal"),
    CBA_EXPIRING_SOON("CBA Expiring Soon"),
    STRIKE_THREAT_CRITICAL("Critical Strike Threat"),
    WORK_STOPPAGE_ALERT("Imminent Work Stoppage"),
    REVENUE_SHARE_DISPUTE("Revenue Share Dispute"),
    TRAVEL_SAFETY_GRIEVANCE("Travel & Safety Grievance"),
    LOCKOUT_ACTIVE("Owner Lockout Active — Games Suspended"),
    CONTRACT_DISPUTE_RANDOM("Random Labor Contract Dispute")
}

enum class LaborTriggerSeverity(val label: String) {
    INFO("ADVISORY"),
    WARNING("URGENT"),
    CRITICAL("CRISIS")
}

data class LaborUnionTriggerEvent(
    val id: String,
    val type: LaborTriggerType,
    val severity: LaborTriggerSeverity,
    val title: String,
    val description: String,
    val recommendedAction: String,
    val seasonYear: Int,
    val week: Int,
    val strikeRiskSnapshot: Int,
    val cbaYearsRemainingSnapshot: Int,
    val autoPromptNegotiation: Boolean = false,
    val disputeScenarioId: String? = null
)

data class WeeklyRevenueBreakdown(
    val seasonYear: Int,
    val weekNumber: Int,
    val phaseLabel: String,
    val weeklyBroadcastRevenueMillions: Double,
    val weeklySponsorRevenueMillions: Double,
    val weeklyNamingRightsRevenueMillions: Double = 0.0,
    val loyaltyGateShareMillions: Double,
    val rivalryGateShareMillions: Double,
    val luxuryTaxCollectedMillions: Long,
    val allStarShowcaseRevenueMillions: Long = 0L,
    val strikeRevenuePenaltyMillions: Double = 0.0,
    val grossLeagueRevenueMillions: Long,
    val playerRevenueSharePct: Int,
    val playerRevenueShareCostMillions: Double,
    val operationsCostMillions: Long,
    val marketingCostMillions: Double,
    val welfareAndGrantsCostMillions: Double,
    val netCashFlowMillions: Long,
    val valuationDeltaMillions: Long,
    val snapshot: FinancialSnapshot
)

sealed class SeasonProgressionEvent {
    data class RegularSeasonLaunched(val year: Int) : SeasonProgressionEvent()
    data class WeekSimulated(
        val year: Int,
        val simulatedWeek: Int,
        val nextWeek: Int,
        val nextPhase: SeasonPhase,
        val netCashFlowMillions: Long
    ) : SeasonProgressionEvent()
    data class AllStarShowcaseCompleted(val year: Int, val revenueMillions: Long) : SeasonProgressionEvent()
    data class PlayoffsLocked(val year: Int, val qualifiedTeamCount: Int) : SeasonProgressionEvent()
    data class PlayoffRoundCompleted(
        val year: Int,
        val roundName: String,
        val championName: String? = null
    ) : SeasonProgressionEvent()
    data class NewSeasonInaugurated(
        val newYear: Int,
        val cbaYearsRemaining: Int
    ) : SeasonProgressionEvent()
}

data class LaborNegotiationChoice(
    val id: String,
    val title: String,
    val stanceTag: String,
    val summary: String,
    val projectedCashImpactMillions: Long,
    val projectedWeeklyNetImpactMillions: Long,
    val projectedValuationImpactMillions: Long,
    val projectedFanMoraleDelta: Int,
    val projectedFanLoyaltyDelta: Int,
    val projectedUnionSatisfactionDelta: Int,
    val projectedStrikeRiskDelta: Int,
    val projectedRevenueShareDeltaPct: Int = 0,
    val projectedCbaYearsSet: Int? = null,
    val enactsCharterFlights: Boolean = false,
    val enactsBackToBackLimit: Boolean = false,
    val opensCbaTableAfter: Boolean = false,
    val requestedSalaryCapMillions: Long? = null,
    val requestedSalaryCapType: SalaryCapType? = null,
    val requestedRosterSizeRule: RosterSizeRule? = null,
    val clearsLockout: Boolean = false,
    val activatesLockoutOnReject: Boolean = false,
    val projectedTeamMoraleDelta: Int = projectedFanMoraleDelta,
    val requestedLuxuryTaxThresholdDelta: Long = 0L,
    val requestedRookieScaleDeltaPct: Int = 0,
    val requestedMaxContractCapDeltaPct: Int = 0,
    val requestedMinSalaryDeltaK: Long = 0L
)

data class LaborNegotiationOverlaySession(
    val id: String,
    val triggerType: LaborTriggerType,
    val severity: LaborTriggerSeverity,
    val headline: String,
    val subheadline: String,
    val unionRepName: String,
    val unionRepTitle: String,
    val unionRepQuote: String,
    val currentCashMillions: Long,
    val currentFanSentimentPct: Int,
    val currentStrikeRiskPct: Int,
    val choices: List<LaborNegotiationChoice>,
    val currentTeamMoralePct: Int = 78,
    val disputeScenarioId: String? = null
)

data class DashboardTrendPoint(
    val id: String,
    val periodLabel: String,
    val seasonYear: Int,
    val weekNumber: Int,
    val leagueValuationMillions: Long,
    val cashReservesMillions: Long,
    val financialGrowthPct: Float,
    val fanSentimentPct: Int,
    val avgMoralePct: Int,
    val avgLoyaltyPct: Int,
    val totalFanbaseMillions: Double,
    val leagueRevenueMillions: Long = 0L,
    val tvBroadcastRating: Double = 4.2,
    val playerSatisfactionPct: Int = 78
)

data class CoreSimulationState(
    val currentYear: Int = 2026,
    val currentWeek: Int = 1,
    val totalWeeks: Int = 16,
    val seasonPhase: SeasonPhase = SeasonPhase.REGULAR_SEASON,
    val isSimulationRunning: Boolean = false,
    val lastRevenueBreakdown: WeeklyRevenueBreakdown? = null,
    val financialSnapshots: List<FinancialSnapshot> = emptyList(),
    val activeLaborTrigger: LaborUnionTriggerEvent? = null,
    val activeLaborNegotiationOverlay: LaborNegotiationOverlaySession? = null,
    val laborTriggerHistory: List<LaborUnionTriggerEvent> = emptyList(),
    val lastProgressionEvent: SeasonProgressionEvent? = null,
    val trendBaselineValuationMillions: Long = 420L,
    val dashboardTrends: List<DashboardTrendPoint> = emptyList()
)

enum class DashboardAlertType {
    LABOR_DISPUTE,
    FINANCIAL_MILESTONE,
    SEASON_MILESTONE,
    OFFICIATING_ALERT,
    DISCIPLINARY_ALERT,
    CRISIS_ALERT
}

enum class DashboardAlertSeverity {
    CRITICAL,
    MILESTONE,
    WARNING,
    INFO
}

enum class DashboardSnackbarRoute {
    LABOR_UNION,
    FINANCES,
    STANDINGS,
    OFFICIATING,
    RULE_COMMITTEE,
    DISCIPLINARY_DESK,
    CRISIS_COMMAND,
    JOB_SECURITY_LEGACY,
    RIVAL_WATCH,
    STADIUM_ULTIMATUM,
    FAN_SENTIMENT_PULSE,
    BROADCAST_WAR_ROOM,
    LEAGUE_SPONSORS
}

data class DashboardSnackbarEvent(
    val id: String,
    val type: DashboardAlertType,
    val severity: DashboardAlertSeverity,
    val message: String,
    val actionLabel: String? = null,
    val targetRoute: DashboardSnackbarRoute? = null
)
