package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.InitialLeagueData
import com.example.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

data class LaborTriggerEvaluationResult(
    val triggeredEvent: LaborUnionTriggerEvent?,
    val updatedUnionAgreement: LaborUnionAgreement,
    val headlines: List<String>,
    val negotiationOverlay: LaborNegotiationOverlaySession? = null
)

class CoreSimulationViewModel : ViewModel() {

    private val _simulationState = MutableStateFlow(CoreSimulationState())
    val simulationState: StateFlow<CoreSimulationState> = _simulationState.asStateFlow()

    private val _standaloneLeagueState = MutableStateFlow(LeagueUiState())
    val standaloneLeagueState: StateFlow<LeagueUiState> = _standaloneLeagueState.asStateFlow()

    private val _laborUnionEvents = MutableSharedFlow<LaborUnionTriggerEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val laborUnionEvents: SharedFlow<LaborUnionTriggerEvent> = _laborUnionEvents.asSharedFlow()

    private val _seasonProgressionEvents = MutableSharedFlow<SeasonProgressionEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val seasonProgressionEvents: SharedFlow<SeasonProgressionEvent> = _seasonProgressionEvents.asSharedFlow()

    private val _dashboardNotifications = MutableSharedFlow<DashboardSnackbarEvent>(
        replay = 0,
        extraBufferCapacity = 32
    )
    val dashboardNotifications: SharedFlow<DashboardSnackbarEvent> = _dashboardNotifications.asSharedFlow()

    private val _dashboardTrends = MutableStateFlow<List<DashboardTrendPoint>>(emptyList())
    val dashboardTrends: StateFlow<List<DashboardTrendPoint>> = _dashboardTrends.asStateFlow()

    private val reachedFinancialMilestones = mutableSetOf<String>()
    private val emittedLaborTriggerIds = mutableSetOf<String>()
    private var wasLowCashWarningActive: Boolean = false

    fun clearNotificationTracking() {
        reachedFinancialMilestones.clear()
        emittedLaborTriggerIds.clear()
        wasLowCashWarningActive = false
        _dashboardTrends.value = emptyList()
        _simulationState.update {
            it.copy(
                trendBaselineValuationMillions = 420L,
                dashboardTrends = emptyList()
            )
        }
    }

    fun computeFanSentimentMetrics(franchises: List<Franchise>): Triple<Int, Int, Int> {
        val avgMorale = if (franchises.isNotEmpty()) franchises.map { it.fanMorale }.average().roundToInt() else 75
        val avgLoyalty = if (franchises.isNotEmpty()) franchises.map { it.fanLoyalty }.average().roundToInt() else 75
        val composite = ((avgMorale * 0.55) + (avgLoyalty * 0.45)).roundToInt().coerceIn(0, 100)
        return Triple(composite, avgMorale, avgLoyalty)
    }

    fun computeExecutiveTrendMetrics(state: LeagueUiState): Triple<Long, Double, Int> {
        val exclusivityFactor = if (state.activeBroadcast.hasExclusivity) {
            1.0
        } else {
            ((100 - state.activeBroadcast.exclusivityLostPenaltyPct.coerceIn(0, 50)) / 100.0)
        }
        val weeklyRevenueM = if (state.unionAgreement.isLockoutActive) {
            0L
        } else {
            state.lastRevenueBreakdown?.grossLeagueRevenueMillions?.coerceAtLeast(1L)
                ?: (((state.activeBroadcast.annualPayoutMillions / 16.0) * exclusivityFactor * 0.15) +
                    (state.franchises.sumOf { it.localSponsorRevenueMillions + it.stadium.namingRightsRevenue } / 16.0 * 0.15))
                    .roundToLong()
                    .coerceAtLeast(4L)
        }
        val (sentiment, _, _) = computeFanSentimentMetrics(state.franchises)
        val rawRating = (state.activeBroadcast.reachMultiplier * 2.35) +
            (state.totalLeagueFanbaseMillions * 0.08) +
            ((sentiment - 68) * 0.025) -
            (if (!state.activeBroadcast.hasExclusivity) 0.9 else 0.0) -
            (if (state.unionAgreement.isLockoutActive) 2.4 else 0.0)
        val tvRating = (Math.round(rawRating.coerceIn(0.8, 12.5) * 10.0) / 10.0)
        val playerSatisfactionPct = state.unionAgreement.unionSatisfaction.coerceIn(5, 100)
        return Triple(weeklyRevenueM, tvRating, playerSatisfactionPct)
    }

    fun seedDashboardTrendsIfNeeded(state: LeagueUiState): List<DashboardTrendPoint> {
        if (state.dashboardTrends.isNotEmpty()) {
            val baseVal = state.dashboardTrends.first().leagueValuationMillions.coerceAtLeast(100L)
            _simulationState.update {
                it.copy(
                    trendBaselineValuationMillions = baseVal,
                    dashboardTrends = state.dashboardTrends
                )
            }
            _dashboardTrends.value = state.dashboardTrends
            return state.dashboardTrends
        }

        val (currentSentiment, currentMorale, currentLoyalty) = computeFanSentimentMetrics(state.franchises)
        val (currentRevM, currentTvRating, currentPlayerSat) = computeExecutiveTrendMetrics(state)
        val snapshots = state.financialSnapshots
        val seeded: List<DashboardTrendPoint>
        val baselineVal: Long

        if (snapshots.size >= 2) {
            baselineVal = snapshots.first().valuation.toLong().coerceAtLeast(100L)
            val lastIdx = snapshots.size - 1
            seeded = snapshots.mapIndexed { idx, snap ->
                val isLast = idx == lastIdx
                val valM = if (isLast) state.leagueValuationMillions else snap.valuation.toLong().coerceAtLeast(100L)
                val cashM = if (isLast) state.cashReservesMillions else snap.cashReserves.toLong().coerceAtLeast(0L)
                val growthPct = ((valM - baselineVal).toFloat() / baselineVal.toFloat()) * 100f
                val stepBack = lastIdx - idx
                val pointSentiment = if (isLast) {
                    currentSentiment
                } else {
                    (currentSentiment - (stepBack * 2)).coerceIn(30, 99)
                }
                val pointMorale = if (isLast) currentMorale else (currentMorale - (stepBack * 2)).coerceIn(30, 99)
                val pointLoyalty = if (isLast) currentLoyalty else (currentLoyalty - stepBack).coerceIn(30, 99)
                val pointRev = if (isLast) currentRevM else snap.totalRevenue.roundToLong().coerceAtLeast(2L)
                val pointTv = if (isLast) currentTvRating else (Math.round((currentTvRating - stepBack * 0.12).coerceIn(1.2, 12.5) * 10.0) / 10.0)
                val pointPlayerSat = if (isLast) currentPlayerSat else (currentPlayerSat - stepBack).coerceIn(20, 99)
                DashboardTrendPoint(
                    id = "seed_${state.currentYear}_${idx}_${snap.weekNumber}",
                    periodLabel = snap.periodLabel,
                    seasonYear = state.currentYear,
                    weekNumber = snap.weekNumber,
                    leagueValuationMillions = valM,
                    cashReservesMillions = cashM,
                    financialGrowthPct = growthPct,
                    fanSentimentPct = pointSentiment,
                    avgMoralePct = pointMorale,
                    avgLoyaltyPct = pointLoyalty,
                    totalFanbaseMillions = state.totalLeagueFanbaseMillions,
                    leagueRevenueMillions = pointRev,
                    tvBroadcastRating = pointTv,
                    playerSatisfactionPct = pointPlayerSat
                )
            }.takeLast(24)
        } else {
            baselineVal = state.leagueValuationMillions.coerceAtLeast(100L)
            seeded = listOf(
                DashboardTrendPoint(
                    id = "seed_${state.currentYear}_w${state.currentWeek}",
                    periodLabel = "W${state.currentWeek}",
                    seasonYear = state.currentYear,
                    weekNumber = state.currentWeek,
                    leagueValuationMillions = state.leagueValuationMillions,
                    cashReservesMillions = state.cashReservesMillions,
                    financialGrowthPct = 0f,
                    fanSentimentPct = currentSentiment,
                    avgMoralePct = currentMorale,
                    avgLoyaltyPct = currentLoyalty,
                    totalFanbaseMillions = state.totalLeagueFanbaseMillions,
                    leagueRevenueMillions = currentRevM,
                    tvBroadcastRating = currentTvRating,
                    playerSatisfactionPct = currentPlayerSat
                )
            )
        }

        _simulationState.update {
            it.copy(
                trendBaselineValuationMillions = baselineVal,
                dashboardTrends = seeded
            )
        }
        _dashboardTrends.value = seeded
        return seeded
    }

    fun appendDashboardTrendPoint(
        state: LeagueUiState,
        periodLabel: String,
        eventTag: String = periodLabel
    ): List<DashboardTrendPoint> {
        val currentSeries = state.dashboardTrends.ifEmpty {
            _simulationState.value.dashboardTrends.ifEmpty { seedDashboardTrendsIfNeeded(state) }
        }
        val baselineVal = (_simulationState.value.trendBaselineValuationMillions.takeIf { it > 0L }
            ?: currentSeries.firstOrNull()?.leagueValuationMillions
            ?: state.leagueValuationMillions).coerceAtLeast(100L)
        val (sentiment, morale, loyalty) = computeFanSentimentMetrics(state.franchises)
        val (revMillions, tvRating, playerSat) = computeExecutiveTrendMetrics(state)
        val growthPct = ((state.leagueValuationMillions - baselineVal).toFloat() / baselineVal.toFloat()) * 100f
        val newPoint = DashboardTrendPoint(
            id = "trend_${state.currentYear}_w${state.currentWeek}_${eventTag}_${currentSeries.size}",
            periodLabel = periodLabel,
            seasonYear = state.currentYear,
            weekNumber = state.currentWeek,
            leagueValuationMillions = state.leagueValuationMillions,
            cashReservesMillions = state.cashReservesMillions,
            financialGrowthPct = growthPct,
            fanSentimentPct = sentiment,
            avgMoralePct = morale,
            avgLoyaltyPct = loyalty,
            totalFanbaseMillions = state.totalLeagueFanbaseMillions,
            leagueRevenueMillions = revMillions,
            tvBroadcastRating = tvRating,
            playerSatisfactionPct = playerSat
        )
        val updated = (currentSeries + newPoint).takeLast(24)
        _simulationState.update {
            it.copy(
                trendBaselineValuationMillions = baselineVal,
                dashboardTrends = updated
            )
        }
        _dashboardTrends.value = updated
        return updated
    }

    fun emitDashboardSnackbar(event: DashboardSnackbarEvent) {
        _dashboardNotifications.tryEmit(event)
    }

    fun evaluateFinancialMilestonesAndEmit(
        previousValuation: Long,
        newValuation: Long,
        previousCash: Long,
        newCash: Long
    ) {
        val cashMilestones = listOf(150L, 250L, 500L, 1000L)
        cashMilestones.lastOrNull { threshold ->
            previousCash < threshold &&
                newCash >= threshold &&
                reachedFinancialMilestones.add("cash_$threshold")
        }?.let { threshold ->
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "milestone_cash_$threshold",
                    type = DashboardAlertType.FINANCIAL_MILESTONE,
                    severity = DashboardAlertSeverity.MILESTONE,
                    message = "Treasury Milestone: Bank balance crossed \$${threshold}M (\$$newCash M)!",
                    actionLabel = "Finances",
                    targetRoute = DashboardSnackbarRoute.FINANCES
                )
            )
        }

        val valuationMilestones = listOf(500L, 750L, 1000L, 1500L, 2000L, 3000L, 5000L)
        valuationMilestones.lastOrNull { threshold ->
            previousValuation < threshold &&
                newValuation >= threshold &&
                reachedFinancialMilestones.add("val_$threshold")
        }?.let { threshold ->
            val label = if (threshold >= 1000L) {
                val billions = threshold / 1000.0
                if (threshold % 1000L == 0L) "\$${threshold / 1000}B" else "\$${billions}B"
            } else {
                "\$${threshold}M"
            }
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "milestone_val_$threshold",
                    type = DashboardAlertType.FINANCIAL_MILESTONE,
                    severity = DashboardAlertSeverity.MILESTONE,
                    message = "Financial Milestone: League valuation surpassed $label (\$$newValuation M)!",
                    actionLabel = "Finances",
                    targetRoute = DashboardSnackbarRoute.FINANCES
                )
            )
        }

        if (newCash < 15L && !wasLowCashWarningActive) {
            wasLowCashWarningActive = true
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "alert_low_cash_$newCash",
                    type = DashboardAlertType.FINANCIAL_MILESTONE,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = "Treasury Alert: League cash reserves fell to \$${newCash}M!",
                    actionLabel = "Finances",
                    targetRoute = DashboardSnackbarRoute.FINANCES
                )
            )
        } else if (newCash >= 25L) {
            wasLowCashWarningActive = false
        }
    }

    fun syncFromLeagueState(state: LeagueUiState) {
        _standaloneLeagueState.value = state
        val resolvedTrends = if (state.dashboardTrends.isNotEmpty()) {
            state.dashboardTrends
        } else if (_dashboardTrends.value.isNotEmpty()) {
            _dashboardTrends.value
        } else {
            seedDashboardTrendsIfNeeded(state)
        }
        _simulationState.update {
            it.copy(
                currentYear = state.currentYear,
                currentWeek = state.currentWeek,
                totalWeeks = state.totalWeeks,
                seasonPhase = state.seasonPhase,
                isSimulationRunning = state.isSimulationRunning,
                financialSnapshots = state.financialSnapshots,
                lastRevenueBreakdown = state.lastRevenueBreakdown ?: it.lastRevenueBreakdown,
                activeLaborTrigger = state.activeLaborTrigger,
                activeLaborNegotiationOverlay = state.activeLaborNegotiationOverlay,
                laborTriggerHistory = state.laborTriggerHistory.ifEmpty { it.laborTriggerHistory },
                dashboardTrends = resolvedTrends
            )
        }
        _dashboardTrends.value = resolvedTrends
    }

    fun seedStandaloneLeagueState(state: LeagueUiState) {
        syncFromLeagueState(state)
    }

    fun simulateStandaloneWeek(): LeagueUiState {
        val next = advanceWeek(_standaloneLeagueState.value)
        _standaloneLeagueState.value = next
        return next
    }

    fun buildDefaultFinancialSnapshots(
        currentWeek: Int,
        broadcastAnnual: Long,
        totalSponsorsAnnual: Long,
        currentCash: Long,
        currentValuation: Long
    ): List<FinancialSnapshot> = LeagueSimulationEngine.generateDefaultFinancialSnapshots(
        currentWeek = currentWeek,
        broadcastAnnual = broadcastAnnual,
        totalSponsorsAnnual = totalSponsorsAnnual,
        currentCash = currentCash,
        currentValuation = currentValuation
    )

    fun calculateWeeklyRevenue(
        state: LeagueUiState,
        franchises: List<Franchise>,
        rivalryTicketBonusMillions: Double,
        fanGrowthBonusPct: Double,
        facilityValuationGainMillions: Long,
        specialEventBonusMillions: Double = 0.0
    ): WeeklyRevenueBreakdown {
        val breakdown = LeagueSimulationEngine.calculateWeeklyRevenue(
            seasonYear = state.currentYear,
            week = state.currentWeek,
            franchises = franchises,
            activeBroadcast = state.activeBroadcast,
            unionAgreement = state.unionAgreement,
            campaigns = state.campaigns,
            rivalryTicketBonusMillions = rivalryTicketBonusMillions,
            fanGrowthBonusPct = fanGrowthBonusPct,
            facilityValuationGainMillions = facilityValuationGainMillions,
            specialEventBonusMillions = specialEventBonusMillions,
            leagueSponsors = state.leagueSponsors
        )
        val newCash = (state.cashReservesMillions + breakdown.netCashFlowMillions).coerceAtLeast(0L)
        val newValuation = (state.leagueValuationMillions + breakdown.valuationDeltaMillions).coerceAtLeast(100L)
        return breakdown.copy(
            snapshot = breakdown.snapshot.copy(
                cashReserves = newCash.toFloat(),
                valuation = newValuation.toFloat()
            )
        )
    }

    fun evaluateLaborUnionTriggers(
        state: LeagueUiState,
        revenueBreakdown: WeeklyRevenueBreakdown? = null,
        justRolledSeason: Boolean = false
    ): LaborTriggerEvaluationResult {
        var union = state.unionAgreement
        val year = state.currentYear
        val week = state.currentWeek
        val history = _simulationState.value.laborTriggerHistory
        val headlines = mutableListOf<String>()

        fun alreadyTriggeredRecently(type: LaborTriggerType, minWeekGap: Int = 4): Boolean =
            history.any {
                it.type == type &&
                    it.seasonYear == year &&
                    kotlin.math.abs(it.week - week) < minWeekGap
            }

        var candidateEvent: LaborUnionTriggerEvent? = null

        // 0. Active Owner Lockout suspending operations
        if (union.isLockoutActive && !alreadyTriggeredRecently(LaborTriggerType.LOCKOUT_ACTIVE, minWeekGap = 2)) {
            candidateEvent = LaborUnionTriggerEvent(
                id = "labor_${year}_w${week}_lockout",
                type = LaborTriggerType.LOCKOUT_ACTIVE,
                severity = LaborTriggerSeverity.CRITICAL,
                title = "LEAGUE LOCKOUT ACTIVE — GAMES SUSPENDED",
                description = "CBA negotiations collapsed and owners locked out the Players Association (${union.lockoutWeeksLost} week(s) lost). Weekly broadcasts and ticket gates are frozen until a new accord passes.",
                recommendedAction = "Ratify a compromise CBA raising the salary cap or roster rules to lift the lockout immediately.",
                seasonYear = year,
                week = week,
                strikeRiskSnapshot = union.strikeRisk,
                cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
                autoPromptNegotiation = true
            )
        }
        // 1. Imminent work stoppage (Strike Risk >= 75%)
        else if (union.strikeRisk >= 75 && !alreadyTriggeredRecently(LaborTriggerType.WORK_STOPPAGE_ALERT, minWeekGap = 3)) {
            candidateEvent = LaborUnionTriggerEvent(
                id = "labor_${year}_w${week}_stoppage",
                type = LaborTriggerType.WORK_STOPPAGE_ALERT,
                severity = LaborTriggerSeverity.CRITICAL,
                title = "IMMINENT WORK STOPPAGE (${union.strikeRisk}% Strike Risk)",
                description = "Players Association executive committee authorized a walkout vote. Gate revenues are taking a 30% confidence hit until concessions or a summit occur.",
                recommendedAction = "Open Labor Union Negotiations to deploy an emergency grant, convene a summit, or ratify a revised CBA.",
                seasonYear = year,
                week = week,
                strikeRiskSnapshot = union.strikeRisk,
                cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
                autoPromptNegotiation = true
            )
        }
        // 2. Expired CBA (0 years remaining)
        else if (union.cbaYearsRemaining <= 0 && !alreadyTriggeredRecently(LaborTriggerType.CBA_EXPIRED, minWeekGap = 5)) {
            if (justRolledSeason) {
                val escalatedRisk = (union.strikeRisk + 12).coerceAtMost(95)
                val reducedSat = (union.unionSatisfaction - 8).coerceAtLeast(20)
                val triggersLockout = escalatedRisk >= 60
                union = union.copy(
                    strikeRisk = escalatedRisk,
                    unionSatisfaction = reducedSat,
                    isLockoutActive = union.isLockoutActive || triggersLockout,
                    lastLockoutSeasonYear = if (triggersLockout) year else union.lastLockoutSeasonYear
                )
            }
            candidateEvent = LaborUnionTriggerEvent(
                id = "labor_${year}_w${week}_cba_expired",
                type = LaborTriggerType.CBA_EXPIRED,
                severity = LaborTriggerSeverity.CRITICAL,
                title = if (union.isLockoutActive) "CBA EXPIRED — LOCKOUT ENFORCED" else "CBA EXPIRED — MANDATORY UNION NEGOTIATIONS",
                description = "The Collective Bargaining Agreement has expired (0 years remaining). Players refuse to play under old cap & roster terms as strike risk hits ${union.strikeRisk}%.",
                recommendedAction = "Submit and ratify a new 5-year CBA with Salary Cap & Roster Rules concessions to prevent prolonged lockout.",
                seasonYear = year,
                week = week,
                strikeRiskSnapshot = union.strikeRisk,
                cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
                autoPromptNegotiation = true
            )
        }
        // 3. Critical strike risk escalation (>= 55% or satisfaction <= 45%)
        else if ((union.strikeRisk >= 55 || union.unionSatisfaction <= 45) &&
            !alreadyTriggeredRecently(LaborTriggerType.STRIKE_THREAT_CRITICAL, minWeekGap = 4)
        ) {
            candidateEvent = LaborUnionTriggerEvent(
                id = "labor_${year}_w${week}_strike_critical",
                type = LaborTriggerType.STRIKE_THREAT_CRITICAL,
                severity = LaborTriggerSeverity.CRITICAL,
                title = "UNION STRIKE THREAT ESCALATING (${union.strikeRisk}%)",
                description = "Player satisfaction has dropped to ${union.unionSatisfaction}% under current cap and welfare terms. Union reps demand immediate table talks.",
                recommendedAction = "Address player welfare, revenue split, or travel terms inside Labor Union Management.",
                seasonYear = year,
                week = week,
                strikeRiskSnapshot = union.strikeRisk,
                cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
                autoPromptNegotiation = true
            )
        }
        // 4. CBA Expiring Soon (1 year remaining — generates Labor Dispute demanding Salary Cap or Roster Rules changes)
        else if (union.cbaYearsRemaining == 1 &&
            (justRolledSeason || week == 1 || week == 10 || week == 14) &&
            !alreadyTriggeredRecently(LaborTriggerType.CBA_EXPIRING_SOON, minWeekGap = 6)
        ) {
            candidateEvent = LaborUnionTriggerEvent(
                id = "labor_${year}_w${week}_cba_expiring",
                type = LaborTriggerType.CBA_EXPIRING_SOON,
                severity = LaborTriggerSeverity.WARNING,
                title = "LABOR DISPUTE: CBA EXPIRING — CAP & ROSTER DEMANDS",
                description = "Only 1 season remains on the CBA expiration timer. Players demand a higher Salary Cap (+\$12M) and Expanded 18-Man Roster slots—failing to reach a deal risks a lockout.",
                recommendedAction = "Negotiate Salary Cap & Roster Rules concessions or ratify a new 5-year CBA before expiration.",
                seasonYear = year,
                week = week,
                strikeRiskSnapshot = union.strikeRisk,
                cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
                autoPromptNegotiation = true
            )
        }
        // 5. Revenue share imbalance during strong league earnings or Hard Cap restriction
        else if (union.playerRevenueSharePct < 49 &&
            ((revenueBreakdown?.grossLeagueRevenueMillions ?: 0L) >= 18L || union.salaryCapType == SalaryCapType.HARD_CAP) &&
            !alreadyTriggeredRecently(LaborTriggerType.REVENUE_SHARE_DISPUTE, minWeekGap = 6)
        ) {
            candidateEvent = LaborUnionTriggerEvent(
                id = "labor_${year}_w${week}_rev_share",
                type = LaborTriggerType.REVENUE_SHARE_DISPUTE,
                severity = LaborTriggerSeverity.WARNING,
                title = "PLAYERS UNION DEMANDS REVENUE SPLIT REVIEW",
                description = "With league revenues surging and player share at ${union.playerRevenueSharePct}%, union reps filed a formal CBA revenue-split grievance.",
                recommendedAction = "Open CBA Negotiations to adjust player revenue share or expand welfare benefits.",
                seasonYear = year,
                week = week,
                strikeRiskSnapshot = union.strikeRisk,
                cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
                autoPromptNegotiation = false
            )
        }
        // 6. Travel & Safety Grievance with international expansion or missing flight/B2B protections
        else {
            val hasInternational = state.franchises.any { it.conference.isContinentalInternational }
            val missingTravelSafety = !union.mandatoryPrivateCharterFlights || !union.maxBackToBackGamesLimit
            if ((hasInternational && missingTravelSafety) &&
                !alreadyTriggeredRecently(LaborTriggerType.TRAVEL_SAFETY_GRIEVANCE, minWeekGap = 7)
            ) {
                candidateEvent = LaborUnionTriggerEvent(
                    id = "labor_${year}_w${week}_travel_safety",
                    type = LaborTriggerType.TRAVEL_SAFETY_GRIEVANCE,
                    severity = LaborTriggerSeverity.WARNING,
                    title = "INTERNATIONAL TRAVEL & SAFETY GRIEVANCE",
                    description = "Intercontinental expansion travel without mandatory private charters or back-to-back limits triggered a formal union safety notice.",
                    recommendedAction = "Enable Charter Flights & Back-to-Back Caps in the Labor Union Summit.",
                    seasonYear = year,
                    week = week,
                    strikeRiskSnapshot = union.strikeRisk,
                    cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
                    autoPromptNegotiation = false
                )
            }
        }

        var overlaySession: LaborNegotiationOverlaySession? = null
        if (candidateEvent == null && state.seasonPhase == SeasonPhase.REGULAR_SEASON) {
            val disputeRoll = LaborDisputeEngine.maybeGenerateRandomContractDispute(
                state = state.copy(unionAgreement = union),
                triggerHistory = _simulationState.value.laborTriggerHistory
            )
            if (disputeRoll != null) {
                candidateEvent = disputeRoll.trigger
                overlaySession = disputeRoll.overlaySession
            }
        }

        if (candidateEvent != null) {
            headlines.add("LABOR TRIGGER [${candidateEvent.severity.label}]: ${candidateEvent.title}")
            if (candidateEvent.autoPromptNegotiation && overlaySession == null) {
                overlaySession = buildLaborNegotiationOverlay(candidateEvent, state.copy(unionAgreement = union))
            }
            _simulationState.update { sim ->
                sim.copy(
                    activeLaborTrigger = candidateEvent,
                    activeLaborNegotiationOverlay = overlaySession ?: sim.activeLaborNegotiationOverlay,
                    laborTriggerHistory = (listOf(candidateEvent) + sim.laborTriggerHistory).take(30)
                )
            }
            _laborUnionEvents.tryEmit(candidateEvent)
            if (emittedLaborTriggerIds.add(candidateEvent.id)) {
                val snackbarSeverity = when (candidateEvent.severity) {
                    LaborTriggerSeverity.CRITICAL -> DashboardAlertSeverity.CRITICAL
                    LaborTriggerSeverity.WARNING -> DashboardAlertSeverity.WARNING
                    LaborTriggerSeverity.INFO -> DashboardAlertSeverity.INFO
                }
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_${candidateEvent.id}",
                        type = DashboardAlertType.LABOR_DISPUTE,
                        severity = snackbarSeverity,
                        message = "Labor Dispute: ${candidateEvent.title}",
                        actionLabel = "Resolve",
                        targetRoute = DashboardSnackbarRoute.LABOR_UNION
                    )
                )
            }
        }

        return LaborTriggerEvaluationResult(
            triggeredEvent = candidateEvent,
            updatedUnionAgreement = union,
            headlines = headlines,
            negotiationOverlay = overlaySession
        )
    }

    fun triggerRandomContractDispute(
        state: LeagueUiState,
        seed: Long? = null
    ): LaborDisputeEngine.RandomDisputeRoll {
        val roll = LaborDisputeEngine.maybeGenerateRandomContractDispute(
            state = state,
            triggerHistory = _simulationState.value.laborTriggerHistory,
            seed = seed,
            forceTrigger = true
        )!!
        _simulationState.update { sim ->
            sim.copy(
                activeLaborTrigger = roll.trigger,
                activeLaborNegotiationOverlay = roll.overlaySession,
                laborTriggerHistory = (listOf(roll.trigger) + sim.laborTriggerHistory).take(30)
            )
        }
        _laborUnionEvents.tryEmit(roll.trigger)
        if (emittedLaborTriggerIds.add(roll.trigger.id)) {
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_${roll.trigger.id}",
                    type = DashboardAlertType.LABOR_DISPUTE,
                    severity = when (roll.trigger.severity) {
                        LaborTriggerSeverity.CRITICAL -> DashboardAlertSeverity.CRITICAL
                        LaborTriggerSeverity.WARNING -> DashboardAlertSeverity.WARNING
                        LaborTriggerSeverity.INFO -> DashboardAlertSeverity.INFO
                    },
                    message = "Contract Dispute: ${roll.trigger.title}",
                    actionLabel = "Negotiate",
                    targetRoute = DashboardSnackbarRoute.LABOR_UNION
                )
            )
        }
        return roll
    }

    fun acknowledgeLaborTrigger() {
        _simulationState.update { it.copy(activeLaborTrigger = null) }
        _standaloneLeagueState.update { it.copy(activeLaborTrigger = null) }
    }

    fun dismissLaborNegotiationOverlay() {
        _simulationState.update { it.copy(activeLaborNegotiationOverlay = null) }
        _standaloneLeagueState.update { it.copy(activeLaborNegotiationOverlay = null) }
    }

    fun buildLaborNegotiationOverlay(
        trigger: LaborUnionTriggerEvent?,
        state: LeagueUiState
    ): LaborNegotiationOverlaySession {
        if (trigger?.type == LaborTriggerType.CONTRACT_DISPUTE_RANDOM) {
            return LaborDisputeEngine.maybeGenerateRandomContractDispute(
                state = state,
                triggerHistory = emptyList(),
                seed = trigger.id.hashCode().toLong(),
                forceTrigger = true
            )!!.overlaySession.copy(
                id = "overlay_${trigger.id}",
                headline = trigger.title,
                subheadline = trigger.description
            )
        }
        val avgTeamMorale = if (state.franchises.isNotEmpty()) state.franchises.map { it.teamMorale }.average().toInt() else 78
        val avgMorale = if (state.franchises.isNotEmpty()) state.franchises.map { it.fanMorale }.average() else 75.0
        val avgLoyalty = if (state.franchises.isNotEmpty()) state.franchises.map { it.fanLoyalty }.average() else 75.0
        val fanSentimentPct = ((avgMorale * 0.55) + (avgLoyalty * 0.45)).toInt().coerceIn(0, 100)
        val triggerType = trigger?.type ?: when {
            state.unionAgreement.isLockoutActive -> LaborTriggerType.LOCKOUT_ACTIVE
            state.unionAgreement.cbaYearsRemaining <= 0 -> LaborTriggerType.CBA_EXPIRED
            state.unionAgreement.strikeRisk >= 55 -> LaborTriggerType.STRIKE_THREAT_CRITICAL
            state.unionAgreement.playerRevenueSharePct < 49 -> LaborTriggerType.REVENUE_SHARE_DISPUTE
            !state.unionAgreement.mandatoryPrivateCharterFlights || !state.unionAgreement.maxBackToBackGamesLimit ->
                LaborTriggerType.TRAVEL_SAFETY_GRIEVANCE
            else -> LaborTriggerType.CBA_EXPIRING_SOON
        }
        val severity = trigger?.severity ?: when {
            state.unionAgreement.isLockoutActive || state.unionAgreement.strikeRisk >= 55 || state.unionAgreement.cbaYearsRemaining <= 0 -> LaborTriggerSeverity.CRITICAL
            state.unionAgreement.strikeRisk >= 35 || state.unionAgreement.cbaYearsRemaining <= 1 -> LaborTriggerSeverity.WARNING
            else -> LaborTriggerSeverity.INFO
        }

        val (repName, repTitle, repQuote) = when (triggerType) {
            LaborTriggerType.CONTRACT_DISPUTE_RANDOM -> Triple(
                "Marcus Vance",
                "PA Bargaining Committee",
                "Players demand fair cap growth and wage structure predictability to keep team locker-room morale steady."
            )
            LaborTriggerType.LOCKOUT_ACTIVE -> Triple(
                "Marcus Vance & Devon Brooks",
                "PA Emergency Bargaining Committee",
                "Arenas are dark and broadcasts are suspended. Lift the lockout by conceding salary cap room and expanded roster slots."
            )
            LaborTriggerType.WORK_STOPPAGE_ALERT,
            LaborTriggerType.STRIKE_THREAT_CRITICAL -> Triple(
                "Marcus Vance",
                "PA Executive Director",
                "Our locker rooms are unified. Without meaningful relief on welfare or revenue share, players are prepared to withhold services."
            )
            LaborTriggerType.CBA_EXPIRED,
            LaborTriggerType.CBA_EXPIRING_SOON -> Triple(
                "Devon Brooks",
                "Lead CBA Counsel",
                "With the CBA clock ticking down, players demand higher salary cap room and expanded 18-man rosters. Refuse and we risk a full lockout."
            )
            LaborTriggerType.REVENUE_SHARE_DISPUTE -> Triple(
                "Devon Brooks",
                "Lead CBA Counsel",
                "Media rights and gate receipts jumped this season—players expect their share of gross league revenue to reflect that growth."
            )
            LaborTriggerType.TRAVEL_SAFETY_GRIEVANCE -> Triple(
                "Elena Rostova",
                "Player Health & Travel Chair",
                "Cross-continental scheduling without private charters and back-to-back limits is burning out stars and risking injuries."
            )
        }

        val headline = trigger?.title ?: when (triggerType) {
            LaborTriggerType.CONTRACT_DISPUTE_RANDOM -> "PA CONTRACT DISPUTE — SALARY STRUCTURE TALKS"
            LaborTriggerType.LOCKOUT_ACTIVE -> "LEAGUE LOCKOUT CRISIS — EMERGENCY ACCORD"
            LaborTriggerType.CBA_EXPIRED -> "CBA TABLE DEADLOCK — RENEWAL DECISION"
            LaborTriggerType.WORK_STOPPAGE_ALERT -> "IMMINENT WORK STOPPAGE SHOWDOWN"
            LaborTriggerType.STRIKE_THREAT_CRITICAL -> "STRIKE CRISIS BARGAINING FLASHPOINT"
            LaborTriggerType.REVENUE_SHARE_DISPUTE -> "REVENUE SPLIT COUNTER-PROPOSAL"
            LaborTriggerType.TRAVEL_SAFETY_GRIEVANCE -> "TRAVEL & SAFETY CHARTER ULTIMATUM"
            LaborTriggerType.CBA_EXPIRING_SOON -> "CBA EXPIRING LABOR DISPUTE — CAP & ROSTER RULES"
        }

        val subheadline = trigger?.description
            ?: "Union leadership requested an immediate Commissioner ruling on the ${state.currentYear} labor table (${state.unionAgreement.strikeRisk}% strike risk)."

        val elevatedCapMillions = (state.unionAgreement.salaryCapMillions + 12L).coerceIn(85L, 160L)
        val choices = when (triggerType) {
            LaborTriggerType.LOCKOUT_ACTIVE -> listOf(
                LaborNegotiationChoice(
                    id = "choice_end_lockout_compromise",
                    title = "Lift Lockout: +\$12M Cap (\$${elevatedCapMillions}M) & 18-Man Roster",
                    stanceTag = "END LOCKOUT • 5-YR CBA",
                    summary = "Ratify a 5-year CBA granting +\$12M salary cap room and Expanded 18-man rosters to immediately resume games.",
                    projectedCashImpactMillions = -5L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = 20L,
                    projectedFanMoraleDelta = 9,
                    projectedFanLoyaltyDelta = 6,
                    projectedUnionSatisfactionDelta = 20,
                    projectedStrikeRiskDelta = -26,
                    projectedRevenueShareDeltaPct = 1,
                    projectedCbaYearsSet = 5,
                    requestedSalaryCapMillions = elevatedCapMillions,
                    requestedSalaryCapType = SalaryCapType.SOFT_CAP_LUXURY_TAX,
                    requestedRosterSizeRule = RosterSizeRule.EXPANDED_18,
                    clearsLockout = true
                ),
                LaborNegotiationChoice(
                    id = "choice_end_lockout_bridge",
                    title = "Suspend Lockout via 2-Year Binding Arbitration",
                    stanceTag = "ARBITRATION BRIDGE",
                    summary = "Reopen arenas under a 2-year CBA extension and transition straight to the custom CBA bargaining table.",
                    projectedCashImpactMillions = -2L,
                    projectedWeeklyNetImpactMillions = 0L,
                    projectedValuationImpactMillions = 8L,
                    projectedFanMoraleDelta = 5,
                    projectedFanLoyaltyDelta = 3,
                    projectedUnionSatisfactionDelta = 11,
                    projectedStrikeRiskDelta = -15,
                    projectedCbaYearsSet = 2,
                    clearsLockout = true,
                    opensCbaTableAfter = true
                ),
                LaborNegotiationChoice(
                    id = "choice_maintain_lockout",
                    title = "Hold Lockout Line & Enforce Owner Freeze",
                    stanceTag = "LOCKOUT HARDLINE",
                    summary = "Refuse cap and roster concessions—keeps arenas dark and escalates weekly financial and TV rating losses.",
                    projectedCashImpactMillions = 0L,
                    projectedWeeklyNetImpactMillions = -5L,
                    projectedValuationImpactMillions = -16L,
                    projectedFanMoraleDelta = -10,
                    projectedFanLoyaltyDelta = -6,
                    projectedUnionSatisfactionDelta = -10,
                    projectedStrikeRiskDelta = 12,
                    activatesLockoutOnReject = true
                )
            )

            LaborTriggerType.WORK_STOPPAGE_ALERT,
            LaborTriggerType.STRIKE_THREAT_CRITICAL -> listOf(
                LaborNegotiationChoice(
                    id = "choice_welfare_concession",
                    title = "Emergency Welfare Grant & Neutral Arbitration",
                    stanceTag = "PLAYER CONCESSION",
                    summary = "Inject \$6M into player health and guarantee neutral arbitration to immediately cool walkout momentum.",
                    projectedCashImpactMillions = -6L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 10L,
                    projectedFanMoraleDelta = 6,
                    projectedFanLoyaltyDelta = 4,
                    projectedUnionSatisfactionDelta = 15,
                    projectedStrikeRiskDelta = -18,
                    clearsLockout = true
                ),
                LaborNegotiationChoice(
                    id = "choice_balanced_safeguards",
                    title = "Balanced +1% Split & Charter Safeguards",
                    stanceTag = "BALANCED COMPROMISE",
                    summary = "Trade a modest +1% revenue share and travel protections for an immediate return-to-work peace pledge.",
                    projectedCashImpactMillions = -2L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 6L,
                    projectedFanMoraleDelta = 3,
                    projectedFanLoyaltyDelta = 2,
                    projectedUnionSatisfactionDelta = 10,
                    projectedStrikeRiskDelta = -12,
                    projectedRevenueShareDeltaPct = 1,
                    enactsCharterFlights = true,
                    enactsBackToBackLimit = true,
                    clearsLockout = true
                ),
                LaborNegotiationChoice(
                    id = "choice_hardline_escrow",
                    title = "Hold Cap Ceiling & Enforce Owner Escrow",
                    stanceTag = "OWNER HARDLINE",
                    summary = "Protect franchise payroll margins and retain \$4M escrow, risking severe fan backlash and lockout escalation.",
                    projectedCashImpactMillions = 4L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = -14L,
                    projectedFanMoraleDelta = -9,
                    projectedFanLoyaltyDelta = -5,
                    projectedUnionSatisfactionDelta = -10,
                    projectedStrikeRiskDelta = 12,
                    activatesLockoutOnReject = true
                )
            )

            LaborTriggerType.CBA_EXPIRED,
            LaborTriggerType.CBA_EXPIRING_SOON -> listOf(
                LaborNegotiationChoice(
                    id = "choice_ratify_5yr_accord",
                    title = "Ratify 5-Yr CBA: \$${elevatedCapMillions}M Cap & 18-Man Roster",
                    stanceTag = "5-YEAR ACCORD",
                    summary = "Pay \$4M bonus, raise salary cap to \$${elevatedCapMillions}M Soft Cap, and expand active rosters to 18 slots for 5 years of labor peace.",
                    projectedCashImpactMillions = -4L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 22L,
                    projectedFanMoraleDelta = 7,
                    projectedFanLoyaltyDelta = 5,
                    projectedUnionSatisfactionDelta = 16,
                    projectedStrikeRiskDelta = -20,
                    projectedRevenueShareDeltaPct = 1,
                    projectedCbaYearsSet = 5,
                    requestedSalaryCapMillions = elevatedCapMillions,
                    requestedSalaryCapType = SalaryCapType.SOFT_CAP_LUXURY_TAX,
                    requestedRosterSizeRule = RosterSizeRule.EXPANDED_18,
                    clearsLockout = true
                ),
                LaborNegotiationChoice(
                    id = "choice_interim_bridge",
                    title = "Sign 1-Year Bridge & Open Custom CBA Table",
                    stanceTag = "INTERIM BRIDGE",
                    summary = "Authorize a \$1M good-faith bridge extension to avert a lockout while fine-tuning cap and roster sliders.",
                    projectedCashImpactMillions = -1L,
                    projectedWeeklyNetImpactMillions = 0L,
                    projectedValuationImpactMillions = 5L,
                    projectedFanMoraleDelta = 2,
                    projectedFanLoyaltyDelta = 1,
                    projectedUnionSatisfactionDelta = 6,
                    projectedStrikeRiskDelta = -8,
                    projectedCbaYearsSet = 1,
                    clearsLockout = true,
                    opensCbaTableAfter = true
                ),
                LaborNegotiationChoice(
                    id = "choice_hard_cap_offer",
                    title = "Enforce Compact 12 & Hard Cap (Risks Lockout)",
                    stanceTag = "HARDLINE STANCE",
                    summary = "Reject roster expansion and impose a strict hard cap freeze; failing to reach a deal activates an immediate lockout if impasse escalates.",
                    projectedCashImpactMillions = 3L,
                    projectedWeeklyNetImpactMillions = 2L,
                    projectedValuationImpactMillions = -12L,
                    projectedFanMoraleDelta = -8,
                    projectedFanLoyaltyDelta = -4,
                    projectedUnionSatisfactionDelta = -12,
                    projectedStrikeRiskDelta = 18,
                    requestedSalaryCapType = SalaryCapType.HARD_CAP,
                    requestedRosterSizeRule = RosterSizeRule.COMPACT_12,
                    activatesLockoutOnReject = true
                )
            )

            LaborTriggerType.REVENUE_SHARE_DISPUTE -> listOf(
                LaborNegotiationChoice(
                    id = "choice_raise_split_2pct",
                    title = "Raise Player Revenue Share (+2%)",
                    stanceTag = "REVENUE CONCESSION",
                    summary = "Share booming broadcast revenue with players, lifting fan excitement while trimming weekly league cash flow.",
                    projectedCashImpactMillions = -3L,
                    projectedWeeklyNetImpactMillions = -2L,
                    projectedValuationImpactMillions = 8L,
                    projectedFanMoraleDelta = 5,
                    projectedFanLoyaltyDelta = 3,
                    projectedUnionSatisfactionDelta = 14,
                    projectedStrikeRiskDelta = -16,
                    projectedRevenueShareDeltaPct = 2
                ),
                LaborNegotiationChoice(
                    id = "choice_transition_stipend",
                    title = "Post-Career Stipend (+1% Split Compromise)",
                    stanceTag = "TARGETED COMPROMISE",
                    summary = "Direct \$5M into player transition annuities with a +1% revenue adjustment to bridge owners and union reps.",
                    projectedCashImpactMillions = -5L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 6L,
                    projectedFanMoraleDelta = 4,
                    projectedFanLoyaltyDelta = 2,
                    projectedUnionSatisfactionDelta = 10,
                    projectedStrikeRiskDelta = -12,
                    projectedRevenueShareDeltaPct = 1
                ),
                LaborNegotiationChoice(
                    id = "choice_defend_split",
                    title = "Reject Mid-Term Split Reopening",
                    stanceTag = "FISCAL DEFENSE",
                    summary = "Hold the existing revenue collar to protect league treasury liquidity despite negative fan and union headlines.",
                    projectedCashImpactMillions = 2L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = -8L,
                    projectedFanMoraleDelta = -6,
                    projectedFanLoyaltyDelta = -3,
                    projectedUnionSatisfactionDelta = -9,
                    projectedStrikeRiskDelta = 10
                )
            )

            LaborTriggerType.TRAVEL_SAFETY_GRIEVANCE -> listOf(
                LaborNegotiationChoice(
                    id = "choice_full_safety_charter",
                    title = "Mandate Private Charters & Back-to-Back Caps",
                    stanceTag = "FULL SAFETY CHARTER",
                    summary = "Invest \$5M in league charter logistics and schedule relief—protecting star availability and boosting fan buzz.",
                    projectedCashImpactMillions = -5L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 10L,
                    projectedFanMoraleDelta = 6,
                    projectedFanLoyaltyDelta = 4,
                    projectedUnionSatisfactionDelta = 15,
                    projectedStrikeRiskDelta = -18,
                    enactsCharterFlights = true,
                    enactsBackToBackLimit = true
                ),
                LaborNegotiationChoice(
                    id = "choice_international_charter_only",
                    title = "Subsidize Intercontinental Charters Only",
                    stanceTag = "PARTIAL CONCESSION",
                    summary = "Cover overseas charter flights for \$2M while keeping domestic road schedules intact.",
                    projectedCashImpactMillions = -2L,
                    projectedWeeklyNetImpactMillions = 0L,
                    projectedValuationImpactMillions = 4L,
                    projectedFanMoraleDelta = 2,
                    projectedFanLoyaltyDelta = 1,
                    projectedUnionSatisfactionDelta = 8,
                    projectedStrikeRiskDelta = -9,
                    enactsCharterFlights = true
                ),
                LaborNegotiationChoice(
                    id = "choice_defer_travel_mandate",
                    title = "Defer Travel Rules Until Off-Season",
                    stanceTag = "COST CONTAINMENT",
                    summary = "Avoid immediate charter overhead, risking player fatigue headlines and cooled fanbase enthusiasm.",
                    projectedCashImpactMillions = 0L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = -6L,
                    projectedFanMoraleDelta = -5,
                    projectedFanLoyaltyDelta = -3,
                    projectedUnionSatisfactionDelta = -8,
                    projectedStrikeRiskDelta = 10
                )
            )
            LaborTriggerType.CONTRACT_DISPUTE_RANDOM -> LaborDisputeEngine.maybeGenerateRandomContractDispute(
                state = state,
                triggerHistory = emptyList(),
                forceTrigger = true
            )!!.overlaySession.choices
        }

        return LaborNegotiationOverlaySession(
            id = trigger?.id ?: "labor_overlay_${state.currentYear}_w${state.currentWeek}_${triggerType.name}",
            triggerType = triggerType,
            severity = severity,
            headline = headline,
            subheadline = subheadline,
            unionRepName = repName,
            unionRepTitle = repTitle,
            unionRepQuote = repQuote,
            currentCashMillions = state.cashReservesMillions,
            currentFanSentimentPct = fanSentimentPct,
            currentStrikeRiskPct = state.unionAgreement.strikeRisk,
            choices = choices,
            currentTeamMoralePct = avgTeamMorale,
            disputeScenarioId = trigger?.disputeScenarioId
        )
    }

    fun reassessTriggersAfterUnionAction(
        updatedAgreement: LaborUnionAgreement,
        year: Int,
        week: Int
    ): LaborUnionTriggerEvent? {
        val current = _simulationState.value.activeLaborTrigger ?: return null
        val resolved = when (current.type) {
            LaborTriggerType.CONTRACT_DISPUTE_RANDOM -> true
            LaborTriggerType.LOCKOUT_ACTIVE -> !updatedAgreement.isLockoutActive && updatedAgreement.cbaYearsRemaining >= 1
            LaborTriggerType.CBA_EXPIRED,
            LaborTriggerType.CBA_EXPIRING_SOON -> !updatedAgreement.isLockoutActive && updatedAgreement.cbaYearsRemaining >= 2
            LaborTriggerType.STRIKE_THREAT_CRITICAL,
            LaborTriggerType.WORK_STOPPAGE_ALERT -> !updatedAgreement.isLockoutActive && updatedAgreement.strikeRisk < 50 && updatedAgreement.unionSatisfaction >= 50
            LaborTriggerType.REVENUE_SHARE_DISPUTE -> updatedAgreement.playerRevenueSharePct >= 49
            LaborTriggerType.TRAVEL_SAFETY_GRIEVANCE ->
                updatedAgreement.mandatoryPrivateCharterFlights && updatedAgreement.maxBackToBackGamesLimit
        }
        val nextActive = if (resolved) null else current.copy(
            seasonYear = year,
            week = week,
            strikeRiskSnapshot = updatedAgreement.strikeRisk,
            cbaYearsRemainingSnapshot = updatedAgreement.cbaYearsRemaining
        )
        _simulationState.update { it.copy(activeLaborTrigger = nextActive) }
        return nextActive
    }

    fun advanceWeek(state: LeagueUiState): LeagueUiState {
        if (state.activeDilemma != null) return state

        val nextState = when (state.seasonPhase) {
            SeasonPhase.PRE_SEASON -> launchRegularSeasonFromPreSeason(state)
            SeasonPhase.REGULAR_SEASON -> simulateRegularSeasonWeek(state)
            SeasonPhase.ALL_STAR_INTERNATIONAL -> simulateAllStarWeek(state)
            SeasonPhase.PLAYOFFS -> simulatePlayoffWeek(state)
            SeasonPhase.OFF_SEASON -> state.copy(showOffSeasonScreen = true)
        }
        evaluateFinancialMilestonesAndEmit(
            previousValuation = state.leagueValuationMillions,
            newValuation = nextState.leagueValuationMillions,
            previousCash = state.cashReservesMillions,
            newCash = nextState.cashReservesMillions
        )
        syncFromLeagueState(nextState)
        return nextState
    }

    private fun launchRegularSeasonFromPreSeason(state: LeagueUiState): LeagueUiState {
        val faSweep = FreeAgencyEngine.runAutomaticFreeAgencySweep(
            franchises = state.franchises,
            freeAgentPool = state.freeAgentPool,
            sportType = state.sportType,
            ruleBook = state.ruleBook,
            unionAgreement = state.unionAgreement,
            week = 1,
            seasonYear = state.currentYear,
            maxSigningsPerSweep = 4,
            urgentOnly = false
        )
        val openingHeadline = "Pre-season concludes. Regular season games officially underway!"
        val openingMatches = ScheduleCalendarEngine.buildWeekMatchesFromPlan(
            plan = state.seasonScheduleState.activeSeasonSchedule,
            week = 1,
            franchises = faSweep.updatedFranchises,
            rivalries = state.rivalries,
            cities = state.cities,
            sportType = state.sportType
        )
        val refreshedOfficiating = state.officiatingState.copy(
            assignments = OfficiatingEngine.buildAssignmentsForMatches(
                matches = openingMatches,
                referees = state.officiatingState.referees,
                franchises = faSweep.updatedFranchises,
                unionState = state.officiatingState.unionState,
                techCatalog = state.officiatingState.techCatalog,
                week = 1
            )
        )
        val preState = state.copy(
            seasonPhase = SeasonPhase.REGULAR_SEASON,
            currentWeek = 1,
            totalWeeks = ScheduleCalendarEngine.totalWeeksFor(state.ruleBook.regularSeasonWeeks),
            franchises = faSweep.updatedFranchises,
            freeAgentPool = faSweep.updatedPool,
            freeAgentSignings = (faSweep.newSignings + state.freeAgentSignings).take(40),
            currentWeekMatches = openingMatches,
            officiatingState = refreshedOfficiating
        )
        val triggerEval = evaluateLaborUnionTriggers(preState, revenueBreakdown = null, justRolledSeason = false)
        _seasonProgressionEvents.tryEmit(SeasonProgressionEvent.RegularSeasonLaunched(state.currentYear))
        return preState.copy(
            unionAgreement = triggerEval.updatedUnionAgreement,
            activeLaborTrigger = triggerEval.triggeredEvent ?: preState.activeLaborTrigger,
            activeLaborNegotiationOverlay = triggerEval.negotiationOverlay ?: preState.activeLaborNegotiationOverlay,
            laborTriggerHistory = _simulationState.value.laborTriggerHistory,
            recentNews = (triggerEval.headlines + listOf(openingHeadline) + faSweep.headlines + state.recentNews).take(20)
        )
    }

    private fun simulateLockoutSuspendedWeek(state: LeagueUiState): LeagueUiState {
        val week = state.currentWeek
        val regWeeks = state.ruleBook.regularSeasonWeeks.coerceIn(14, 22)
        val nextWeek = week + 1
        val nextPhase = when {
            week >= regWeeks -> SeasonPhase.OFF_SEASON
            else -> SeasonPhase.REGULAR_SEASON
        }
        val newCash = (state.cashReservesMillions - 5L).coerceAtLeast(0L)
        val newValuation = (state.leagueValuationMillions - 8L).coerceAtLeast(100L)
        val hitFranchises = state.franchises.map { team ->
            team.copy(
                fanMorale = (team.fanMorale - 3).coerceIn(15, 99),
                fanLoyalty = (team.fanLoyalty - 2).coerceIn(15, 100)
            )
        }
        val updatedUnion = state.unionAgreement.copy(
            lockoutWeeksLost = state.unionAgreement.lockoutWeeksLost + 1,
            strikeRisk = (state.unionAgreement.strikeRisk + 3).coerceAtMost(95),
            unionSatisfaction = (state.unionAgreement.unionSatisfaction - 2).coerceAtLeast(15)
        )
        val lockoutHeadline = "LOCKOUT SUSPENSION (W$week): Games canceled due to unresolved CBA dispute (-\$5M cash, -\$8M valuation)."
        val lockoutSnapshot = FinancialSnapshot(
            periodLabel = "W$week",
            weekNumber = week,
            broadcastRevenue = 0f,
            sponsorshipRevenue = 0f,
            expansionFeeRevenue = 0f,
            luxuryTaxRevenue = 0f,
            totalRevenue = 0f,
            operationsExpense = 5.0f,
            marketingExpense = 0f,
            welfareAndGrantsExpense = 0f,
            totalExpenses = 5.0f,
            netCashFlow = -5.0f,
            cashReserves = newCash.toFloat(),
            valuation = newValuation.toFloat()
        )
        val updatedSnapshots = (state.financialSnapshots + lockoutSnapshot).takeLast(24)

        var nextState = state.copy(
            currentWeek = nextWeek,
            seasonPhase = nextPhase,
            franchises = hitFranchises,
            unionAgreement = updatedUnion,
            cashReservesMillions = newCash,
            leagueValuationMillions = newValuation,
            financialSnapshots = updatedSnapshots,
            lastRevenueBreakdown = WeeklyRevenueBreakdown(
                seasonYear = state.currentYear,
                weekNumber = week,
                phaseLabel = "Week $week Lockout Suspension",
                weeklyBroadcastRevenueMillions = 0.0,
                weeklySponsorRevenueMillions = 0.0,
                loyaltyGateShareMillions = 0.0,
                rivalryGateShareMillions = 0.0,
                luxuryTaxCollectedMillions = 0L,
                grossLeagueRevenueMillions = 0L,
                playerRevenueSharePct = updatedUnion.playerRevenueSharePct,
                playerRevenueShareCostMillions = 0.0,
                operationsCostMillions = 5L,
                marketingCostMillions = 0.0,
                welfareAndGrantsCostMillions = 0.0,
                netCashFlowMillions = -5L,
                valuationDeltaMillions = -8L,
                snapshot = lockoutSnapshot
            ),
            recentNews = (listOf(lockoutHeadline) + state.recentNews).take(20)
        )

        if (nextPhase == SeasonPhase.OFF_SEASON) {
            val offSeasonSession = OffSeasonEngine.createOffSeasonSession(
                year = state.currentYear,
                franchises = nextState.franchises,
                sportType = state.sportType,
                ruleBook = state.ruleBook,
                initialParity = state.competitiveParityScore,
                careerStats = state.careerPlayerStats,
                seasonStats = state.seasonPlayerStats,
                weeklyAwards = state.weeklyAwards,
                champion = state.seasonChampion,
                autoCompleteDraft = false
            )
            nextState = nextState.copy(activeOffSeasonSession = offSeasonSession, showOffSeasonScreen = true)
        }

        val triggerEval = evaluateLaborUnionTriggers(nextState, revenueBreakdown = null, justRolledSeason = false)
        val lockoutSentiment = FanSentimentEngine.recordDecisionReaction(
            currentState = nextState.fanSentimentState,
            category = FanDecisionCategory.LABOR_CBA,
            decisionTitle = "Week $week Lockout Cancellations",
            decisionSummary = lockoutHeadline,
            casualDelta = -3,
            dieHardDelta = -4,
            franchiseName = nextState.leagueName,
            cityName = nextState.franchises.firstOrNull()?.cityName ?: "Metro",
            sportType = nextState.sportType,
            seasonYear = nextState.currentYear,
            weekNumber = week,
            seedOffset = week + 40
        )
        val preTrend = nextState.copy(
            unionAgreement = triggerEval.updatedUnionAgreement,
            activeLaborTrigger = triggerEval.triggeredEvent ?: nextState.activeLaborTrigger,
            activeLaborNegotiationOverlay = triggerEval.negotiationOverlay ?: nextState.activeLaborNegotiationOverlay,
            laborTriggerHistory = _simulationState.value.laborTriggerHistory,
            fanSentimentState = lockoutSentiment,
            jobSecurityLegacyState = JobSecurityLegacyEngine.recalculateLiveSecurity(
                current = nextState.jobSecurityLegacyState,
                input = nextState.toPerformanceInput(),
                eventNote = lockoutHeadline
            )
        )
        val updatedTrends = appendDashboardTrendPoint(
            state = preTrend,
            periodLabel = "W$week",
            eventTag = "lockout_w$week"
        )
        _seasonProgressionEvents.tryEmit(
            SeasonProgressionEvent.WeekSimulated(
                year = state.currentYear,
                simulatedWeek = week,
                nextWeek = nextWeek,
                nextPhase = nextPhase,
                netCashFlowMillions = -5L
            )
        )
        return preTrend.copy(dashboardTrends = updatedTrends)
    }

    private fun simulateRegularSeasonWeek(state: LeagueUiState): LeagueUiState {
        if (state.unionAgreement.isLockoutActive) {
            return simulateLockoutSuspendedWeek(state)
        }
        val week = state.currentWeek
        val sport = state.sportType
        val committeeModifiers = RuleCommitteeEngine.computeSimulationModifiers(state.ruleCommitteeState, sport)
        val recoveryTick = InjuryEngine.advanceWeeklyRecovery(state.franchises)
        val updatedFranchises = recoveryTick.updatedFranchises.map { it.copy() }.toMutableList()
        val activePlanMatches = state.seasonScheduleState.activeSeasonSchedule
            ?.slotsForWeek(week)
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.toGameMatch(updatedFranchises, state.rivalries, state.cities) }
        val matchesToPlay = activePlanMatches ?: state.currentWeekMatches.ifEmpty {
            LeagueSimulationEngine.generateMatchesForWeek(week, updatedFranchises, sport, state.rivalries, state.cities)
        }
        val playedMatches = LeagueSimulationEngine.simulateRegularSeasonMatches(
            week = week,
            sport = sport,
            ruleBook = state.ruleBook,
            matchesToPlay = matchesToPlay,
            updatedFranchises = updatedFranchises,
            rivalries = state.rivalries,
            cities = state.cities,
            committeeModifiers = committeeModifiers
        )

        val weekPlayerStats = mutableListOf<PlayerGameStatLine>()
        playedMatches.forEach { m ->
            if (m.isPlayed) {
                val home = updatedFranchises.find { it.id == m.homeTeamId }
                val away = updatedFranchises.find { it.id == m.awayTeamId }
                if (home != null && away != null) {
                    weekPlayerStats.addAll(
                        RecordBookEngine.generateMatchPlayerStats(
                            match = m, home = home, away = away,
                            homeScore = m.homeScore, awayScore = m.awayScore,
                            sportType = sport, seasonYear = state.currentYear, isPlayoffs = false,
                            defenseStatMultiplier = committeeModifiers.defenseStatMultiplier,
                            reboundStatMultiplier = committeeModifiers.reboundStatMultiplier
                        )
                    )
                }
            }
        }

        val injurySim = InjuryEngine.simulateMatchInjuries(
            playedMatches = playedMatches,
            franchises = updatedFranchises,
            week = week,
            seasonYear = state.currentYear,
            isPlayoffs = false,
            injuryFrequency = state.ruleBook.injuryFrequency,
            gameLength = state.ruleBook.gameLength,
            committeeInjuryMultiplier = committeeModifiers.injuryRiskMultiplier
        )

        val rivalryResult = RivalryEngine.processPlayedMatchups(
            playedMatches = playedMatches,
            franchises = injurySim.updatedFranchises,
            currentRivalries = state.rivalries,
            cities = state.cities,
            sportType = sport,
            seasonYear = state.currentYear,
            isPlayoffs = false
        )
        val postRivalryFranchises = LeagueSimulationEngine.applyRivalryWeekResults(
            franchises = injurySim.updatedFranchises,
            rivalryResult = rivalryResult
        )
        val annotatedPlayedMatches = RivalryEngine.annotateMatchesWithRivalries(
            matches = playedMatches,
            franchises = postRivalryFranchises,
            rivalries = rivalryResult.updatedRivalries,
            cities = state.cities,
            isPlayoffs = false
        )

        val loyaltyResult = FanLoyaltyEngine.processWeeklyLoyaltyUpdates(
            franchises = postRivalryFranchises,
            playedMatches = annotatedPlayedMatches,
            activeLeagueCampaigns = state.campaigns,
            rivalryBonusesByTeamId = rivalryResult.ticketBonusByFranchiseId,
            isPlayoffs = false
        )
        val facilityTick = FacilityEngine.advanceWeeklyConstruction(loyaltyResult.updatedFranchises)
        val faSweep = FreeAgencyEngine.runAutomaticFreeAgencySweep(
            franchises = facilityTick.updatedFranchises,
            freeAgentPool = state.freeAgentPool,
            sportType = sport,
            ruleBook = state.ruleBook,
            unionAgreement = state.unionAgreement,
            week = week,
            seasonYear = state.currentYear,
            maxSigningsPerSweep = 3,
            urgentOnly = false
        )
        val postLoyaltyFranchises = faSweep.updatedFranchises.toMutableList()

        val statsResult = LeagueSimulationEngine.processWeekPlayerStatsAndRecords(
            currentYear = state.currentYear,
            sportType = sport,
            seasonPlayerStats = state.seasonPlayerStats,
            careerPlayerStats = state.careerPlayerStats,
            recordBook = state.recordBook,
            weekStats = weekPlayerStats,
            playedMatches = annotatedPlayedMatches,
            updatedFranchises = postLoyaltyFranchises,
            week = week,
            isPlayoffs = false
        )
        val updatedWeeklyAwards = if (statsResult.playerOfTheWeek != null) {
            listOf(statsResult.playerOfTheWeek) + state.weeklyAwards
        } else state.weeklyAwards
        val potwHeadline = statsResult.playerOfTheWeek?.let {
            "PLAYER OF THE WEEK (W$week): ${it.playerName} (${it.franchiseName}) — ${it.statLineSummary}!"
        }

        var fanGrowthBonus = 0.0
        val updatedCampaigns = state.campaigns.map { camp ->
            if (camp.isActive && camp.remainingWeeks > 0) {
                fanGrowthBonus += camp.fanGrowthPct / camp.durationWeeks
                val newWeeks = camp.remainingWeeks - 1
                camp.copy(remainingWeeks = newWeeks, isActive = newWeeks > 0)
            } else camp
        }

        val specialEventBroadcastBonus = annotatedPlayedMatches.sumOf { it.scheduleBroadcastBonusMillions }
        val specialEventFanReachGain = annotatedPlayedMatches.sumOf { it.scheduleGlobalReachGainMillions }

        val revenueBreakdown = calculateWeeklyRevenue(
            state = state,
            franchises = postLoyaltyFranchises,
            rivalryTicketBonusMillions = rivalryResult.totalRivalryTicketBonusMillions,
            fanGrowthBonusPct = fanGrowthBonus,
            facilityValuationGainMillions = facilityTick.totalLeagueValuationGainMillions,
            specialEventBonusMillions = specialEventBroadcastBonus
        )

        val updatedFanbase = ((state.totalLeagueFanbaseMillions * (1.0 + (fanGrowthBonus / 100.0) + 0.005)) + specialEventFanReachGain).let {
            Math.round(it * 100.0) / 100.0
        }
        val sortedWins = postLoyaltyFranchises.map { it.wins }.sorted()
        val winSpread = if (sortedWins.isNotEmpty()) sortedWins.last() - sortedWins.first() else 0
        val newParity = (95 - (winSpread * 4)).coerceIn(50, 98)

        val regWeeks = state.ruleBook.regularSeasonWeeks.coerceIn(14, 22)
        val allStarWeek = ScheduleCalendarEngine.allStarWeekFor(regWeeks)
        val nextWeek = week + 1
        val nextPhase = when {
            week == allStarWeek -> SeasonPhase.ALL_STAR_INTERNATIONAL
            week >= regWeeks -> SeasonPhase.PLAYOFFS
            else -> SeasonPhase.REGULAR_SEASON
        }

        val newCash = (state.cashReservesMillions + revenueBreakdown.netCashFlowMillions).coerceAtLeast(0L)
        val newValuation = (state.leagueValuationMillions + revenueBreakdown.valuationDeltaMillions).coerceAtLeast(100L)
        val updatedSnapshots = (state.financialSnapshots + revenueBreakdown.snapshot).takeLast(24)
        val newsMessage = "Week $week action concluded. Highlights: ${playedMatches.firstOrNull()?.highlightStory ?: ""}"

        val openCount = state.cities.count { !it.hasFranchise }
        var currentCitiesList = state.cities
        var extraNews: String? = null
        var newlyAddedCity: MarketCity? = null
        if (openCount < MAX_OPEN_EXPANSION_CANDIDATES && (week % 4 == 0 || openCount < 6)) {
            val existingIds = currentCitiesList.map { it.id }.toSet()
            val existingNames = currentCitiesList.map { it.name.lowercase() }.toSet()
            val prospect = InitialLeagueData.getProspectiveMarketPool()
                .filter { it.id !in existingIds && it.name.lowercase() !in existingNames }
                .shuffled()
                .firstOrNull()
            if (prospect != null) {
                val openProspect = prospect.copy(hasFranchise = false)
                currentCitiesList = currentCitiesList + openProspect
                newlyAddedCity = openProspect
                extraNews = "INCOMING EXPANSION PETITION: ${prospect.name} (${if (prospect.isInternational) prospect.country else "USA"}) submitted formal expansion dossier! Viability: ${prospect.overallViabilityScore}/100 [${prospect.viabilityGrade.label}]."
            }
        }

        val existingPendingIds = state.pendingExpansionRequests.map { it.city.id }.toSet()
        val existingReviewedIds = state.reviewedExpansionRequests.map { it.city.id }.toSet()
        var updatedPendingRequests = ExpansionRequestEngine.recalculateImpacts(
            requests = state.pendingExpansionRequests.filter { req ->
                currentCitiesList.none { it.id == req.city.id && it.hasFranchise }
            },
            franchises = postLoyaltyFranchises,
            activeBroadcast = state.activeBroadcast,
            activeSponsorship = state.activeSponsorship,
            currentMarketReachMillions = updatedFanbase
        )
        if (updatedPendingRequests.size < 4 && (newlyAddedCity != null || week % 4 == 0)) {
            val candidateForRequest = newlyAddedCity
                ?: currentCitiesList.firstOrNull { !it.hasFranchise && it.id !in existingPendingIds && it.id !in existingReviewedIds }
            if (candidateForRequest != null && updatedPendingRequests.none { it.city.id == candidateForRequest.id }) {
                val incomingRequest = ExpansionRequestEngine.buildRequestForCity(
                    city = candidateForRequest,
                    franchises = postLoyaltyFranchises,
                    activeBroadcast = state.activeBroadcast,
                    activeSponsorship = state.activeSponsorship,
                    currentMarketReachMillions = updatedFanbase,
                    seasonYear = state.currentYear,
                    week = nextWeek,
                    indexSeed = updatedPendingRequests.size + state.reviewedExpansionRequests.size
                )
                updatedPendingRequests = updatedPendingRequests + incomingRequest
            }
        }

        val tradeRules = state.ruleBook.tradeRules
        val updatedPendingTrades = state.pendingTrades.toMutableList()
        val updatedCompletedTrades = state.completedTrades.toMutableList()
        var tradeNewsHeadline: String? = null

        if (tradeRules.isTradeWindowOpen(nextWeek, nextPhase)) {
            val isDeadlineFrenzy = tradeRules.isTradeDeadlineWeek(nextWeek, nextPhase)
            if (isDeadlineFrenzy) {
                tradeNewsHeadline = "TRADE DEADLINE WEEK: Front offices enter final negotiations before the Week ${tradeRules.tradeDeadlineWeek} deadline!"
            }
            if (isDeadlineFrenzy || nextWeek % 2 == 0 || updatedPendingTrades.isEmpty()) {
                val newOffer = TradeEngine.generateRealisticTradeOffer(nextWeek, updatedFranchises, tradeRules)
                if (newOffer != null && updatedPendingTrades.none { it.teamAId == newOffer.teamAId && it.teamBId == newOffer.teamBId }) {
                    updatedPendingTrades.add(0, newOffer)
                    if (tradeNewsHeadline == null) {
                        tradeNewsHeadline = "BLOCKBUSTER TRADE PROPOSAL: ${newOffer.teamAName} and ${newOffer.teamBName} submit deal for commissioner review!"
                    }
                }
            }
        } else if (state.currentWeek <= tradeRules.tradeDeadlineWeek && nextWeek > tradeRules.tradeDeadlineWeek) {
            tradeNewsHeadline = "TRADE DEADLINE PASSED: The trade window is officially closed for the remainder of Season ${state.currentYear}."
            updatedPendingTrades.forEach { pending ->
                updatedCompletedTrades.add(0, pending.copy(status = TradeStatus.EXPIRED, commissionerReviewNotes = "Expired: Trade deadline elapsed."))
            }
            updatedPendingTrades.clear()
        }

        val minorWeekTick = MinorLeagueEngine.simulateRegularSeasonWeek(
            currentMinorState = state.minorLeagueState,
            franchises = postLoyaltyFranchises,
            sportType = sport,
            week = week,
            seasonYear = state.currentYear,
            gameLength = state.ruleBook.gameLength
        )

        val newImpactLog = RuleCommitteeEngine.buildImpactLog(
            seasonYear = state.currentYear,
            weekNumber = week,
            sportType = sport,
            playedMatches = annotatedPlayedMatches,
            modifiers = committeeModifiers,
            injuriesCount = injurySim.newInjuryLogs.size
        )
        val updatedCommitteeState = state.ruleCommitteeState.copy(
            impactLogs = (listOf(newImpactLog) + state.ruleCommitteeState.impactLogs).take(24)
        )

        var resultState = state.copy(
            currentWeek = nextWeek,
            totalWeeks = ScheduleCalendarEngine.totalWeeksFor(regWeeks),
            seasonPhase = nextPhase,
            franchises = minorWeekTick.updatedFranchises,
            minorLeagueState = minorWeekTick.minorState,
            cities = currentCitiesList,
            pendingExpansionRequests = updatedPendingRequests,
            cashReservesMillions = newCash,
            leagueValuationMillions = newValuation,
            totalLeagueFanbaseMillions = updatedFanbase,
            competitiveParityScore = newParity,
            campaigns = updatedCampaigns,
            currentWeekMatches = annotatedPlayedMatches,
            financialSnapshots = updatedSnapshots,
            lastRevenueBreakdown = revenueBreakdown,
            pendingTrades = updatedPendingTrades,
            completedTrades = updatedCompletedTrades,
            recordBook = statsResult.recordBook,
            weeklyAwards = updatedWeeklyAwards,
            currentWeekPlayerStats = weekPlayerStats,
            seasonPlayerStats = statsResult.seasonStats,
            careerPlayerStats = statsResult.careerStats,
            injuryReportHistory = (injurySim.newInjuryLogs + state.injuryReportHistory).take(50),
            rivalries = rivalryResult.updatedRivalries,
            freeAgentPool = faSweep.updatedPool,
            freeAgentSignings = (faSweep.newSignings + state.freeAgentSignings).take(40),
            ruleCommitteeState = updatedCommitteeState
        )

        if (nextPhase == SeasonPhase.PLAYOFFS) {
            val bracket = LeagueSimulationEngine.buildInitialPlayoffBracket(
                week = nextWeek,
                franchises = resultState.franchises,
                playoffRules = resultState.ruleBook.playoffRules,
                rivalries = resultState.rivalries,
                cities = resultState.cities
            )
            if (bracket != null) {
                resultState = resultState.copy(
                    playoffMatches = bracket.first,
                    recentNews = (listOf(bracket.second) + resultState.recentNews).take(20)
                )
                val seedCount = calculatePlayoffSeeds(resultState.franchises, resultState.ruleBook.playoffRules).size
                _seasonProgressionEvents.tryEmit(
                    SeasonProgressionEvent.PlayoffsLocked(resultState.currentYear, seedCount)
                )
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_playoffs_${resultState.currentYear}",
                        type = DashboardAlertType.SEASON_MILESTONE,
                        severity = DashboardAlertSeverity.INFO,
                        message = "Playoffs Locked: Top $seedCount teams qualify for the ${resultState.currentYear} bracket!",
                        actionLabel = "Table",
                        targetRoute = DashboardSnackbarRoute.STANDINGS
                    )
                )
            }
        }

        val upcomingWeekMatches = if (nextPhase != SeasonPhase.PLAYOFFS) {
            ScheduleCalendarEngine.buildWeekMatchesFromPlan(
                plan = resultState.seasonScheduleState.activeSeasonSchedule,
                week = nextWeek,
                franchises = resultState.franchises,
                rivalries = resultState.rivalries,
                cities = resultState.cities,
                sportType = sport
            )
        } else {
            resultState.playoffMatches.filter { !it.isPlayed }
        }
        val officiatingEval = OfficiatingEngine.evaluateWeekOfficiating(
            state = resultState.officiatingState,
            playedMatches = annotatedPlayedMatches,
            nextMatches = upcomingWeekMatches,
            franchises = resultState.franchises,
            currentWeek = week,
            seasonYear = resultState.currentYear,
            isPlayoffs = false
        )
        resultState = resultState.copy(
            franchises = officiatingEval.updatedFranchises,
            officiatingState = officiatingEval.updatedState,
            leagueIntegrityScore = (resultState.leagueIntegrityScore + officiatingEval.integrityDelta).coerceIn(20, 100),
            cashReservesMillions = (resultState.cashReservesMillions - officiatingEval.weeklyTechOpsCostMillions).coerceAtLeast(0L)
        )
        officiatingEval.newIncidents.firstOrNull {
            it.severity == BlownCallSeverity.HIGH_PROFILE || it.severity == BlownCallSeverity.PLAYOFF_SCANDAL
        }?.let { scandal ->
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_ref_${scandal.id}",
                    type = DashboardAlertType.OFFICIATING_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = "Officiating Backlash: ${scandal.callDescription} in ${scandal.matchupTitle}",
                    actionLabel = "Inspect",
                    targetRoute = DashboardSnackbarRoute.OFFICIATING
                )
            )
        } ?: if (officiatingEval.updatedState.unionState.lockoutRisk >= 65) {
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_ref_union_${resultState.currentYear}_w$week",
                    type = DashboardAlertType.OFFICIATING_ALERT,
                    severity = DashboardAlertSeverity.WARNING,
                    message = "Ref Union Alert: NLOA lockout threat at ${officiatingEval.updatedState.unionState.lockoutRisk}%!",
                    actionLabel = "Resolve",
                    targetRoute = DashboardSnackbarRoute.OFFICIATING
                )
            )
        } else Unit

        val disciplinaryTick = DisciplinaryEngine.generateWeeklyInfractions(
            state = resultState.disciplinaryState,
            franchises = resultState.franchises,
            playedMatches = annotatedPlayedMatches,
            ruleBook = resultState.ruleBook,
            sportType = sport,
            seasonYear = resultState.currentYear,
            week = week
        )
        resultState = resultState.copy(
            franchises = disciplinaryTick.updatedFranchises,
            disciplinaryState = disciplinaryTick.updatedState
        )
        disciplinaryTick.newIncidents.firstOrNull {
            it.severity == InfractionSeverity.SEVERE || it.severity == InfractionSeverity.CRITICAL
        }?.let { urgentCase ->
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_disc_${urgentCase.id}",
                    type = DashboardAlertType.DISCIPLINARY_ALERT,
                    severity = if (urgentCase.severity == InfractionSeverity.CRITICAL) DashboardAlertSeverity.CRITICAL else DashboardAlertSeverity.WARNING,
                    message = "Disciplinary Hearing (${urgentCase.caseCode}): ${urgentCase.playerName} — ${urgentCase.incidentTitle}",
                    actionLabel = "Rule Now",
                    targetRoute = DashboardSnackbarRoute.DISCIPLINARY_DESK
                )
            )
        }

        val (updatedCrisisState, triggeredDilemma) = CrisisEngine.buildCrisisForWeekOrFollowUp(
            crisisState = resultState.crisisState,
            sportType = sport,
            franchises = resultState.franchises,
            activeBroadcast = resultState.activeBroadcast,
            activeSponsorship = resultState.activeSponsorship,
            cities = resultState.cities,
            leagueIntegrityScore = resultState.leagueIntegrityScore,
            unionAgreement = resultState.unionAgreement,
            officiatingState = resultState.officiatingState,
            seasonYear = resultState.currentYear,
            week = week
        )
        val crisisHeadline = triggeredDilemma?.let {
            val branchTag = if (it.isBranchFollowUp) "BRANCHING ESCALATION" else "CRISIS ALERT"
            "$branchTag [Stage ${it.stageNumber}/${it.totalStages}]: ${it.title}"
        }
        if (triggeredDilemma?.isBranchFollowUp == true) {
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_crisis_${triggeredDilemma.id}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = "Fallout Escalated: ${triggeredDilemma.title} (from prior decision)",
                    actionLabel = "Timeline",
                    targetRoute = DashboardSnackbarRoute.CRISIS_COMMAND
                )
            )
        }
        val postRebuildStadiumState = facilityTick.rebuiltStadiumTeamIds.fold(resultState.stadiumDealState) { acc, teamId ->
            val rebuilt = resultState.franchises.find { it.id == teamId } ?: return@fold acc
            StadiumDealEngine.enqueueNamingRightsOfferForFranchise(
                currentState = acc,
                franchise = rebuilt,
                seasonYear = resultState.currentYear,
                triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                allFranchises = resultState.franchises
            )
        }
        var postScandalLeagueSponsors = disciplinaryTick.newIncidents.fold(resultState.leagueSponsors) { acc, incident ->
            LeagueSponsorsEngine.evaluateScandalForUltimatum(
                currentState = acc,
                scandalSourceTitle = "${incident.playerName} • ${incident.incidentTitle}",
                scandalSummary = incident.incidentSummary,
                seasonYear = resultState.currentYear,
                weekNumber = week,
                relatedIncident = incident
            )
        }
        if (triggeredDilemma != null) {
            postScandalLeagueSponsors = LeagueSponsorsEngine.evaluateScandalForUltimatum(
                currentState = postScandalLeagueSponsors,
                scandalSourceTitle = triggeredDilemma.title,
                scandalSummary = triggeredDilemma.description,
                seasonYear = resultState.currentYear,
                weekNumber = week
            )
        }
        resultState = resultState.copy(
            crisisState = updatedCrisisState,
            activeDilemma = triggeredDilemma,
            stadiumDealState = postRebuildStadiumState,
            leagueSponsors = postScandalLeagueSponsors
        )

        val triggerEval = evaluateLaborUnionTriggers(
            state = resultState,
            revenueBreakdown = revenueBreakdown,
            justRolledSeason = false
        )

        val compiledNews = (
            triggerEval.headlines +
                listOfNotNull(crisisHeadline) +
                disciplinaryTick.headlines +
                officiatingEval.headlines +
                minorWeekTick.headlines +
                faSweep.headlines +
                facilityTick.headlines +
                loyaltyResult.headlines +
                rivalryResult.headlines +
                injurySim.headlines +
                recoveryTick.clearanceHeadlines +
                statsResult.brokenAlerts +
                listOfNotNull(potwHeadline, tradeNewsHeadline, extraNews, newsMessage) +
                resultState.recentNews
            ).take(20)

        _simulationState.update {
            it.copy(
                lastRevenueBreakdown = revenueBreakdown,
                financialSnapshots = updatedSnapshots,
                lastProgressionEvent = SeasonProgressionEvent.WeekSimulated(
                    year = state.currentYear,
                    simulatedWeek = week,
                    nextWeek = nextWeek,
                    nextPhase = nextPhase,
                    netCashFlowMillions = revenueBreakdown.netCashFlowMillions
                )
            )
        }
        _seasonProgressionEvents.tryEmit(
            SeasonProgressionEvent.WeekSimulated(
                year = state.currentYear,
                simulatedWeek = week,
                nextWeek = nextWeek,
                nextPhase = nextPhase,
                netCashFlowMillions = revenueBreakdown.netCashFlowMillions
            )
        )

        val periodicBroadcastResult = if (week % 3 == 0 && resultState.broadcastBiddingSession.offers.isNotEmpty()) {
            BroadcastBiddingEngine.receivePeriodicRivalBid(
                session = resultState.broadcastBiddingSession,
                currentContract = resultState.activeBroadcast,
                teamCount = resultState.franchises.size,
                playerRevenueSharePct = resultState.unionAgreement.playerRevenueSharePct,
                currentFanbaseMillions = resultState.totalLeagueFanbaseMillions,
                currentYear = resultState.currentYear,
                weekNumber = week
            )
        } else null

        if (periodicBroadcastResult != null && periodicBroadcastResult.headline != null) {
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_tv_bid_${resultState.currentYear}_w$week",
                    type = if (periodicBroadcastResult.exclusivityJustExpired) DashboardAlertType.CRISIS_ALERT else DashboardAlertType.FINANCIAL_MILESTONE,
                    severity = if (periodicBroadcastResult.exclusivityJustExpired) DashboardAlertSeverity.CRITICAL else DashboardAlertSeverity.INFO,
                    message = periodicBroadcastResult.headline,
                    actionLabel = "Bids",
                    targetRoute = DashboardSnackbarRoute.BROADCAST_WAR_ROOM
                )
            )
        }

        val updatedActiveBroadcast = if (periodicBroadcastResult?.exclusivityJustExpired == true && resultState.activeBroadcast.hasExclusivity) {
            resultState.activeBroadcast.copy(
                hasExclusivity = false,
                exclusivityLostPenaltyPct = BroadcastBiddingEngine.EXPIRY_REVENUE_PENALTY_PCT
            )
        } else {
            resultState.activeBroadcast
        }
        val postBroadcastFanbase = if (periodicBroadcastResult?.exclusivityJustExpired == true) {
            (Math.round((resultState.totalLeagueFanbaseMillions * 0.96) * 100.0) / 100.0).coerceAtLeast(1.0)
        } else {
            resultState.totalLeagueFanbaseMillions
        }

        val finalPreTrendState = resultState.copy(
            unionAgreement = triggerEval.updatedUnionAgreement,
            activeLaborTrigger = triggerEval.triggeredEvent ?: resultState.activeLaborTrigger,
            activeLaborNegotiationOverlay = triggerEval.negotiationOverlay ?: resultState.activeLaborNegotiationOverlay,
            laborTriggerHistory = _simulationState.value.laborTriggerHistory,
            broadcastBiddingSession = periodicBroadcastResult?.updatedSession ?: resultState.broadcastBiddingSession,
            activeBroadcast = updatedActiveBroadcast,
            totalLeagueFanbaseMillions = postBroadcastFanbase,
            recentNews = (listOfNotNull(periodicBroadcastResult?.headline) + compiledNews).take(20)
        )
        val updatedFanSentiment = FanSentimentEngine.runWeeklyBroadcastPulse(
            currentState = finalPreTrendState.fanSentimentState,
            franchises = finalPreTrendState.franchises,
            sportType = sport,
            seasonYear = finalPreTrendState.currentYear,
            weekNumber = week
        )
        val securityRefreshedPreTrend = finalPreTrendState.copy(
            fanSentimentState = updatedFanSentiment,
            jobSecurityLegacyState = JobSecurityLegacyEngine.recalculateLiveSecurity(
                current = finalPreTrendState.jobSecurityLegacyState,
                input = finalPreTrendState.toPerformanceInput(),
                eventNote = "Week $week metrics logged (${finalPreTrendState.jobSecurityLegacyState.latestBreakdown.compositeApprovalScore}% coalition index)."
            )
        )
        val updatedTrends = appendDashboardTrendPoint(
            state = securityRefreshedPreTrend,
            periodLabel = "W$week",
            eventTag = "sim_w$week"
        )
        return securityRefreshedPreTrend.copy(dashboardTrends = updatedTrends)
    }

    private fun simulateAllStarWeek(state: LeagueUiState): LeagueUiState {
        val recoveryTick = InjuryEngine.advanceWeeklyRecovery(state.franchises)
        val facilityTick = FacilityEngine.advanceWeeklyConstruction(recoveryTick.updatedFranchises)
        val faSweep = FreeAgencyEngine.runAutomaticFreeAgencySweep(
            franchises = facilityTick.updatedFranchises,
            freeAgentPool = state.freeAgentPool,
            sportType = state.sportType,
            ruleBook = state.ruleBook,
            unionAgreement = state.unionAgreement,
            week = state.currentWeek,
            seasonYear = state.currentYear,
            maxSigningsPerSweep = 2,
            urgentOnly = false
        )
        val simulationOutcome = AllStarEngine.simulateAllStarFestivities(
            state = state.copy(franchises = faSweep.updatedFranchises),
            isDressRehearsal = false
        )
        val adjustedFranchises = faSweep.updatedFranchises.map { team ->
            team.copy(
                fanMorale = (team.fanMorale + simulationOutcome.moraleDelta).coerceIn(25, 99),
                fanLoyalty = (team.fanLoyalty + simulationOutcome.loyaltyDelta).coerceIn(15, 100)
            )
        }
        val updatedUnion = state.unionAgreement.copy(
            strikeRisk = (state.unionAgreement.strikeRisk + simulationOutcome.strikeRiskDelta).coerceIn(0, 100)
        )
        val nextMatches = ScheduleCalendarEngine.buildWeekMatchesFromPlan(
            plan = state.seasonScheduleState.activeSeasonSchedule,
            week = state.currentWeek,
            franchises = adjustedFranchises,
            rivalries = state.rivalries,
            cities = state.cities,
            sportType = state.sportType
        )
        val postAllStarStadiumState = facilityTick.rebuiltStadiumTeamIds.fold(state.stadiumDealState) { acc, teamId ->
            val rebuilt = adjustedFranchises.find { it.id == teamId } ?: return@fold acc
            StadiumDealEngine.enqueueNamingRightsOfferForFranchise(
                currentState = acc,
                franchise = rebuilt,
                seasonYear = state.currentYear,
                triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                allFranchises = adjustedFranchises
            )
        }
        val nextState = state.copy(
            seasonPhase = SeasonPhase.REGULAR_SEASON,
            franchises = adjustedFranchises,
            freeAgentPool = faSweep.updatedPool,
            freeAgentSignings = (faSweep.newSignings + state.freeAgentSignings).take(40),
            unionAgreement = updatedUnion,
            stadiumDealState = postAllStarStadiumState,
            cashReservesMillions = state.cashReservesMillions + simulationOutcome.netRevenueMillions,
            totalLeagueFanbaseMillions = Math.round((state.totalLeagueFanbaseMillions + simulationOutcome.fanbaseGainMillions) * 100.0) / 100.0,
            leagueValuationMillions = state.leagueValuationMillions + simulationOutcome.valuationGainMillions + facilityTick.totalLeagueValuationGainMillions,
            allStarRedesignState = simulationOutcome.updatedRedesignState,
            currentWeekMatches = nextMatches,
            recentNews = (listOf(simulationOutcome.headline) + faSweep.headlines + facilityTick.headlines + recoveryTick.clearanceHeadlines + state.recentNews).take(20)
        )
        _seasonProgressionEvents.tryEmit(SeasonProgressionEvent.AllStarShowcaseCompleted(state.currentYear, simulationOutcome.netRevenueMillions))
        val allStarSentiment = FanSentimentEngine.recordDecisionReaction(
            currentState = nextState.fanSentimentState,
            category = FanDecisionCategory.ALL_STAR_SCHEDULE,
            decisionTitle = "All-Star Showcase (${state.allStarRedesignState.selectedFormat.displayName})",
            decisionSummary = simulationOutcome.headline,
            casualDelta = (simulationOutcome.moraleDelta + 2).coerceIn(1, 6),
            dieHardDelta = simulationOutcome.loyaltyDelta.coerceIn(-3, 5),
            franchiseName = state.leagueName,
            cityName = state.allStarRedesignState.hostCityName,
            sportType = state.sportType,
            seasonYear = state.currentYear,
            weekNumber = state.currentWeek,
            seedOffset = state.currentYear + 11
        )
        val securityNextState = nextState.copy(
            fanSentimentState = allStarSentiment,
            jobSecurityLegacyState = JobSecurityLegacyEngine.recalculateLiveSecurity(
                current = nextState.jobSecurityLegacyState,
                input = nextState.toPerformanceInput(),
                eventNote = "All-Star Showcase complete: ${simulationOutcome.recap.actualViewersMillions}M viewers boosted Media & Fan standing."
            )
        )
        val updatedTrends = appendDashboardTrendPoint(
            state = securityNextState,
            periodLabel = "AllStar",
            eventTag = "allstar_w${state.currentWeek}"
        )
        return securityNextState.copy(dashboardTrends = updatedTrends)
    }

    private fun simulatePlayoffWeek(state: LeagueUiState): LeagueUiState {
        val sport = state.sportType
        val baseScore = sport.baseScoreScale
        val unplayed = state.playoffMatches.filter { !it.isPlayed }
        if (unplayed.isEmpty()) return state

        val committeeModifiers = RuleCommitteeEngine.computeSimulationModifiers(state.ruleCommitteeState, sport)
        val commScoreDelta = if (committeeModifiers.sportType == sport) committeeModifiers.scoringDeltaPerTeam else 0.0
        val commVar = if (committeeModifiers.sportType == sport) committeeModifiers.varianceDelta.coerceAtLeast(0) else 0

        val recoveryTick = InjuryEngine.advanceWeeklyRecovery(state.franchises)
        val recoveredFranchises = recoveryTick.updatedFranchises
        val roundWinners = mutableListOf<Franchise>()

        val playedCurrentRound = unplayed.map { match ->
            val home = recoveredFranchises.find { it.id == match.homeTeamId }!!
            val away = recoveredFranchises.find { it.id == match.awayTeamId }!!
            val rivalryImpact = RivalryEngine.evaluateMatchupImpact(
                home = home, away = away, rivalries = state.rivalries, cities = state.cities, isPlayoffs = true
            )
            val lengthScale = state.ruleBook.gameLength.scoreScaleMultiplier
            val rawHomeScore = ((baseScore + (home.effectiveLineupRating * 0.4) + rivalryImpact.crowdHypeBoost + commScoreDelta + Random.nextInt(4 - commVar, 14 + commVar)) * lengthScale).toInt().coerceAtLeast(1)
            val awayScore = ((baseScore + (away.effectiveLineupRating * 0.4) + commScoreDelta + Random.nextInt(2 - commVar, 12 + commVar)) * lengthScale).toInt().coerceAtLeast(1)
            val finalHome = if (rawHomeScore == awayScore) rawHomeScore + 1 else rawHomeScore
            val winner = if (finalHome > awayScore) home else away
            roundWinners.add(winner)
            val note = if (committeeModifiers.activeRuleCount > 0) {
                "Playoff Rule Active: ${committeeModifiers.activeRuleTitles.firstOrNull() ?: "Rule Committee"}"
            } else ""
            match.copy(
                homeScore = finalHome,
                awayScore = awayScore,
                isPlayed = true,
                highlightStory = "${winner.teamFullName} victorious in ${match.roundName} ($finalHome - $awayScore)!",
                ruleImpactNote = note
            )
        }

        val allMatchesSoFar = state.playoffMatches.map { m -> playedCurrentRound.find { it.id == m.id } ?: m }
        val playoffRoundStats = mutableListOf<PlayerGameStatLine>()
        playedCurrentRound.forEach { m ->
            val home = recoveredFranchises.find { it.id == m.homeTeamId }
            val away = recoveredFranchises.find { it.id == m.awayTeamId }
            if (home != null && away != null) {
                playoffRoundStats.addAll(
                    RecordBookEngine.generateMatchPlayerStats(
                        match = m, home = home, away = away,
                        homeScore = m.homeScore, awayScore = m.awayScore,
                        sportType = sport, seasonYear = state.currentYear, isPlayoffs = true,
                        defenseStatMultiplier = committeeModifiers.defenseStatMultiplier,
                        reboundStatMultiplier = committeeModifiers.reboundStatMultiplier
                    )
                )
            }
        }

        val playoffInjurySim = InjuryEngine.simulateMatchInjuries(
            playedMatches = playedCurrentRound,
            franchises = recoveredFranchises,
            week = state.currentWeek,
            seasonYear = state.currentYear,
            isPlayoffs = true,
            injuryFrequency = state.ruleBook.injuryFrequency,
            gameLength = state.ruleBook.gameLength,
            committeeInjuryMultiplier = committeeModifiers.injuryRiskMultiplier
        )
        val playoffRivalryResult = RivalryEngine.processPlayedMatchups(
            playedMatches = playedCurrentRound,
            franchises = playoffInjurySim.updatedFranchises,
            currentRivalries = state.rivalries,
            cities = state.cities,
            sportType = sport,
            seasonYear = state.currentYear,
            isPlayoffs = true
        )
        val postRivalryPlayoffFranchises = LeagueSimulationEngine.applyRivalryWeekResults(
            franchises = playoffInjurySim.updatedFranchises,
            rivalryResult = playoffRivalryResult
        )
        val playoffLoyaltyResult = FanLoyaltyEngine.processWeeklyLoyaltyUpdates(
            franchises = postRivalryPlayoffFranchises,
            playedMatches = playedCurrentRound,
            activeLeagueCampaigns = state.campaigns,
            rivalryBonusesByTeamId = playoffRivalryResult.ticketBonusByFranchiseId,
            isPlayoffs = true
        )
        val playoffFacilityTick = FacilityEngine.advanceWeeklyConstruction(playoffLoyaltyResult.updatedFranchises)
        val playoffFaSweep = FreeAgencyEngine.runAutomaticFreeAgencySweep(
            franchises = playoffFacilityTick.updatedFranchises,
            freeAgentPool = state.freeAgentPool,
            sportType = sport,
            ruleBook = state.ruleBook,
            unionAgreement = state.unionAgreement,
            week = state.currentWeek,
            seasonYear = state.currentYear,
            maxSigningsPerSweep = 2,
            urgentOnly = true
        )
        val postInjuryPlayoffFranchises = playoffFaSweep.updatedFranchises
        val annotatedAllMatchesSoFar = RivalryEngine.annotateMatchesWithRivalries(
            matches = allMatchesSoFar,
            franchises = postInjuryPlayoffFranchises,
            rivalries = playoffRivalryResult.updatedRivalries,
            cities = state.cities,
            isPlayoffs = true
        )

        val roundLabel = unplayed.firstOrNull()?.roundName?.ifBlank { "Playoffs" } ?: "Playoffs"
        val playoffRevenue = LeagueSimulationEngine.calculatePlayoffRoundRevenue(
            seasonYear = state.currentYear,
            week = state.currentWeek,
            roundLabel = roundLabel,
            franchises = postInjuryPlayoffFranchises,
            activeBroadcast = state.activeBroadcast,
            unionAgreement = state.unionAgreement,
            rivalryTicketBonusMillions = playoffRivalryResult.totalRivalryTicketBonusMillions,
            facilityValuationGainMillions = playoffFacilityTick.totalLeagueValuationGainMillions
        )
        val playoffRivalryCashCut = playoffRevenue.netCashFlowMillions

        val statsResult = LeagueSimulationEngine.processWeekPlayerStatsAndRecords(
            currentYear = state.currentYear,
            sportType = sport,
            seasonPlayerStats = state.seasonPlayerStats,
            careerPlayerStats = state.careerPlayerStats,
            recordBook = state.recordBook,
            weekStats = playoffRoundStats,
            playedMatches = playedCurrentRound,
            updatedFranchises = postInjuryPlayoffFranchises,
            week = state.currentWeek,
            isPlayoffs = true
        )
        val updatedWeeklyAwards = if (statsResult.playerOfTheWeek != null) {
            listOf(statsResult.playerOfTheWeek) + state.weeklyAwards
        } else state.weeklyAwards
        val potwNews = statsResult.playerOfTheWeek?.let {
            "PLAYOFF PLAYER OF THE WEEK: ${it.playerName} (${it.franchiseName}) shines in $roundLabel — ${it.statLineSummary}!"
        }

        val nextWeek = state.currentWeek + 1
        val currentRoundName = unplayed.first().roundName
        val isParentFinalsCompleted = currentRoundName == "Championship Finals" ||
            (unplayed.size == 1 && (state.playoffMatches.size > 1 || state.ruleBook.playoffRules.fieldSize == 2))

        if (isParentFinalsCompleted) {
            val finalsMatch = playedCurrentRound.last()
            val home = postInjuryPlayoffFranchises.find { it.id == finalsMatch.homeTeamId }!!
            val away = postInjuryPlayoffFranchises.find { it.id == finalsMatch.awayTeamId }!!
            val champion = if (finalsMatch.homeScore > finalsMatch.awayScore) home else away
            val runnerUp = if (finalsMatch.homeScore > finalsMatch.awayScore) away else home

            val preMinorFinalsFranchises = postInjuryPlayoffFranchises.map {
                if (it.id == champion.id) {
                    FanLoyaltyEngine.applyChampionLoyaltyBoost(it.copy(championships = it.championships + 1, fanMorale = 99))
                } else it
            }
            val minorPoFinals = MinorLeagueEngine.advancePlayoffRound(
                currentMinorState = state.minorLeagueState,
                franchises = preMinorFinalsFranchises,
                sportType = sport,
                week = state.currentWeek,
                seasonYear = state.currentYear,
                completeAllRemainingRounds = true
            )
            val updatedFranchises = TeamReputationEngine.refreshAllFranchiseReputations(
                franchises = minorPoFinals.updatedFranchises,
                salaryCapMillions = state.unionAgreement.salaryCapMillions,
                incrementLowSpendingSeason = true
            )
            val championPlayerIds = champion.roster.map { it.id }.toSet()
            val finalCareerStats = statsResult.careerStats.mapValues { (pId, cStat) ->
                if (pId in championPlayerIds) cStat.copy(championships = cStat.championships + 1) else cStat
            }
            val annualRevProjection = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
                franchises = updatedFranchises,
                activeBroadcast = state.activeBroadcast,
                unionAgreement = state.unionAgreement,
                leagueSponsors = state.leagueSponsors
            )
            val newHistory = LeagueValuationHistory(
                year = state.currentYear,
                valuationMillions = state.leagueValuationMillions,
                totalRevenueMillions = annualRevProjection.annualLeagueRevenueMillions,
                championName = champion.teamFullName
            )
            val offSeasonSession = OffSeasonEngine.createOffSeasonSession(
                year = state.currentYear,
                franchises = updatedFranchises,
                sportType = state.sportType,
                ruleBook = state.ruleBook,
                initialParity = state.competitiveParityScore,
                careerStats = finalCareerStats,
                seasonStats = statsResult.seasonStats,
                weeklyAwards = updatedWeeklyAwards,
                champion = champion,
                autoCompleteDraft = false
            )
            _seasonProgressionEvents.tryEmit(
                SeasonProgressionEvent.PlayoffRoundCompleted(state.currentYear, currentRoundName, champion.teamFullName)
            )
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_champ_${state.currentYear}_${champion.id}",
                    type = DashboardAlertType.SEASON_MILESTONE,
                    severity = DashboardAlertSeverity.MILESTONE,
                    message = "Champion Crowned: ${champion.teamFullName} win the ${state.currentYear} Championship!",
                    actionLabel = "Table",
                    targetRoute = DashboardSnackbarRoute.STANDINGS
                )
            )
            val rivalPoachResult = RivalLeagueEngine.processOffseasonPoaching(
                rivalLeague = state.rivalLeague,
                freeAgentPool = playoffFaSweep.updatedPool,
                seasonYear = state.currentYear,
                existingCities = state.cities
            )
            if (rivalPoachResult.triggeredAbsorptionNegotiation != null) {
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_rival_absorption_${state.currentYear}",
                        type = DashboardAlertType.CRISIS_ALERT,
                        severity = DashboardAlertSeverity.CRITICAL,
                        message = rivalPoachResult.triggeredAbsorptionNegotiation.headline,
                        actionLabel = "Summit",
                        targetRoute = DashboardSnackbarRoute.RIVAL_WATCH
                    )
                )
            } else if (rivalPoachResult.headline != null && (rivalPoachResult.newlyPoachedPlayers.isNotEmpty() || rivalPoachResult.didFoldRivalLeague)) {
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_rival_poach_${state.currentYear}",
                        type = if (rivalPoachResult.didFoldRivalLeague) DashboardAlertType.SEASON_MILESTONE else DashboardAlertType.CRISIS_ALERT,
                        severity = if (rivalPoachResult.didFoldRivalLeague) DashboardAlertSeverity.MILESTONE else DashboardAlertSeverity.WARNING,
                        message = rivalPoachResult.headline,
                        actionLabel = "Rival Watch",
                        targetRoute = DashboardSnackbarRoute.RIVAL_WATCH
                    )
                )
            }
            val finalsRecords = updatedFranchises.associate { it.id to (it.wins to it.losses) }
            val stadiumCheckRaw = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
                currentState = state.stadiumDealState,
                franchises = updatedFranchises,
                seasonYear = state.currentYear,
                seasonRecordsByFranchiseId = finalsRecords
            )
            val stadiumDealAfterRebuilds = playoffFacilityTick.rebuiltStadiumTeamIds.fold(stadiumCheckRaw.updatedStadiumDealState) { acc, teamId ->
                val rebuilt = stadiumCheckRaw.updatedFranchises.find { it.id == teamId } ?: return@fold acc
                StadiumDealEngine.enqueueNamingRightsOfferForFranchise(
                    currentState = acc,
                    franchise = rebuilt,
                    seasonYear = state.currentYear,
                    triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                    allFranchises = stadiumCheckRaw.updatedFranchises
                )
            }
            val stadiumCheck = stadiumCheckRaw.copy(updatedStadiumDealState = stadiumDealAfterRebuilds)
            if (stadiumCheck.triggeredBankruptcyScandals.isNotEmpty()) {
                val scandal = stadiumCheck.triggeredBankruptcyScandals.first()
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_sponsor_scandal_${state.currentYear}_${scandal.franchiseId}",
                        type = DashboardAlertType.CRISIS_ALERT,
                        severity = DashboardAlertSeverity.CRITICAL,
                        message = scandal.headline,
                        actionLabel = "Replace",
                        targetRoute = DashboardSnackbarRoute.STADIUM_ULTIMATUM
                    )
                )
            } else if (stadiumCheck.triggeredUltimatums.isNotEmpty()) {
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_stadium_ultimatum_${state.currentYear}",
                        type = DashboardAlertType.CRISIS_ALERT,
                        severity = DashboardAlertSeverity.CRITICAL,
                        message = stadiumCheck.triggeredUltimatums.first().headline,
                        actionLabel = "Desk",
                        targetRoute = DashboardSnackbarRoute.STADIUM_ULTIMATUM
                    )
                )
            }
            val preUpcomingSchedule = ScheduleCalendarEngine.generateFullSeasonSchedule(
                targetYear = state.currentYear + 1,
                regularSeasonWeeks = state.ruleBook.regularSeasonWeeks,
                franchises = stadiumCheck.updatedFranchises,
                rivalries = playoffRivalryResult.updatedRivalries,
                cities = state.cities,
                sportType = sport
            )
            val preTrendState = state.copy(
                currentWeek = nextWeek,
                seasonPhase = SeasonPhase.OFF_SEASON,
                seasonChampion = champion,
                seasonRunnerUp = runnerUp,
                franchises = stadiumCheck.updatedFranchises,
                minorLeagueState = minorPoFinals.minorState,
                cashReservesMillions = state.cashReservesMillions + playoffRivalryCashCut,
                leagueValuationMillions = state.leagueValuationMillions + playoffFacilityTick.totalLeagueValuationGainMillions,
                leagueIntegrityScore = (state.leagueIntegrityScore - stadiumCheck.totalLeaguePrDrain).coerceIn(20, 100),
                lastRevenueBreakdown = playoffRevenue,
                playoffMatches = annotatedAllMatchesSoFar,
                valuationHistory = listOf(newHistory) + state.valuationHistory,
                activeOffSeasonSession = offSeasonSession,
                showOffSeasonScreen = true,
                seasonScheduleState = state.seasonScheduleState.copy(upcomingPlan = preUpcomingSchedule),
                recordBook = statsResult.recordBook,
                weeklyAwards = updatedWeeklyAwards,
                currentWeekPlayerStats = playoffRoundStats,
                seasonPlayerStats = statsResult.seasonStats,
                careerPlayerStats = finalCareerStats,
                injuryReportHistory = (playoffInjurySim.newInjuryLogs + state.injuryReportHistory).take(50),
                rivalries = playoffRivalryResult.updatedRivalries,
                freeAgentPool = rivalPoachResult.updatedFreeAgentPool,
                freeAgentSignings = (playoffFaSweep.newSignings + state.freeAgentSignings).take(40),
                rivalLeague = rivalPoachResult.updatedRivalLeague,
                stadiumDealState = stadiumCheck.updatedStadiumDealState,
                tradedPickLedger = TradeEngine.buildTradedPickLedger(
                    franchises = stadiumCheck.updatedFranchises,
                    currentYear = state.currentYear,
                    activeSession = offSeasonSession,
                    latestCompletedOffSeason = state.latestCompletedOffSeason
                ),
                recentNews = (listOf("CHAMPION CROWNED! ${champion.teamFullName} lift the Apex Commissioner Trophy!") + stadiumCheck.headlines + listOfNotNull(rivalPoachResult.headline) + minorPoFinals.headlines + playoffFaSweep.headlines + playoffFacilityTick.headlines + playoffRivalryResult.headlines + playoffInjurySim.headlines + recoveryTick.clearanceHeadlines + statsResult.brokenAlerts + listOfNotNull(potwNews) + state.recentNews).take(20)
            )
            val reviewInput = preTrendState.toPerformanceInput(seasonChampionNameOverride = champion.teamFullName)
            val legacyEval = LeagueLegacyTracker.evaluateAndUnlockMilestones(
                current = preTrendState.jobSecurityLegacyState,
                input = reviewInput,
                isSeasonRollover = false
            )
            legacyEval.newlyUnlockedMilestones.firstOrNull()?.let { milestone ->
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_legacy_milestone_${state.currentYear}_${milestone.id.name}",
                        type = DashboardAlertType.SEASON_MILESTONE,
                        severity = DashboardAlertSeverity.MILESTONE,
                        message = "League Legacy Unlocked: ${milestone.title} (+${milestone.rewardLegacyPoints} Pts)",
                        actionLabel = "Legacy",
                        targetRoute = DashboardSnackbarRoute.STANDINGS
                    )
                )
            }
            val generatedReview = JobSecurityLegacyEngine.generateAnnualReview(
                current = legacyEval.updatedState,
                input = reviewInput
            )
            val reviewHeadline = "ANNUAL PERFORMANCE REVIEW READY (Y${generatedReview.seasonYear}): Grade [${generatedReview.overallGrade.letter}] • ${generatedReview.contractAction.label} (+${generatedReview.legacyPointsAwarded} Legacy Pts)."
            val withReviewState = preTrendState.copy(
                jobSecurityLegacyState = legacyEval.updatedState.copy(
                    latestBreakdown = JobSecurityLegacyEngine.evaluateStakeholders(reviewInput),
                    pendingAnnualReview = generatedReview,
                    lastUpdateHeadline = legacyEval.headlines.firstOrNull() ?: reviewHeadline
                ),
                recentNews = (legacyEval.headlines + listOf(reviewHeadline) + preTrendState.recentNews).take(20)
            )
            val updatedTrends = appendDashboardTrendPoint(
                state = withReviewState,
                periodLabel = "Finals",
                eventTag = "finals_${state.currentYear}"
            )
            return withReviewState.copy(dashboardTrends = updatedTrends)
        } else {
            val minorPoRound = MinorLeagueEngine.advancePlayoffRound(
                currentMinorState = state.minorLeagueState,
                franchises = postInjuryPlayoffFranchises,
                sportType = sport,
                week = state.currentWeek,
                seasonYear = state.currentYear,
                completeAllRemainingRounds = false
            )
            val postMinorPlayoffFranchises = minorPoRound.updatedFranchises
            val (nextMatches, roundHeadline) = buildNextPlayoffRound(
                currentRoundName = currentRoundName,
                nextWeek = nextWeek,
                franchises = postMinorPlayoffFranchises,
                playoffRules = state.ruleBook.playoffRules,
                roundWinners = roundWinners,
                rivalries = playoffRivalryResult.updatedRivalries,
                cities = state.cities
            )
            val playoffOfficiating = OfficiatingEngine.evaluateWeekOfficiating(
                state = state.officiatingState,
                playedMatches = playedCurrentRound,
                nextMatches = nextMatches,
                franchises = postMinorPlayoffFranchises,
                currentWeek = state.currentWeek,
                seasonYear = state.currentYear,
                isPlayoffs = true
            )
            playoffOfficiating.newIncidents.firstOrNull()?.let { scandal ->
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_po_ref_${scandal.id}",
                        type = DashboardAlertType.OFFICIATING_ALERT,
                        severity = DashboardAlertSeverity.CRITICAL,
                        message = "Playoff Officiating Storm: ${scandal.matchupTitle} (${scandal.fanOutragePct}% outrage)",
                        actionLabel = "Inspect",
                        targetRoute = DashboardSnackbarRoute.OFFICIATING
                    )
                )
            }
            val playoffDisciplinary = DisciplinaryEngine.generateWeeklyInfractions(
                state = state.disciplinaryState,
                franchises = playoffOfficiating.updatedFranchises,
                playedMatches = playedCurrentRound,
                ruleBook = state.ruleBook,
                sportType = state.sportType,
                seasonYear = state.currentYear,
                week = state.currentWeek
            )
            _seasonProgressionEvents.tryEmit(
                SeasonProgressionEvent.PlayoffRoundCompleted(state.currentYear, currentRoundName, null)
            )
            val playoffRoundStadiumState = playoffFacilityTick.rebuiltStadiumTeamIds.fold(state.stadiumDealState) { acc, teamId ->
                val rebuilt = playoffDisciplinary.updatedFranchises.find { it.id == teamId } ?: return@fold acc
                StadiumDealEngine.enqueueNamingRightsOfferForFranchise(
                    currentState = acc,
                    franchise = rebuilt,
                    seasonYear = state.currentYear,
                    triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                    allFranchises = playoffDisciplinary.updatedFranchises
                )
            }
            val playoffRoundSponsors = playoffDisciplinary.newIncidents.fold(state.leagueSponsors) { acc, incident ->
                LeagueSponsorsEngine.evaluateScandalForUltimatum(
                    currentState = acc,
                    scandalSourceTitle = "${incident.playerName} • ${incident.incidentTitle}",
                    scandalSummary = incident.incidentSummary,
                    seasonYear = state.currentYear,
                    weekNumber = state.currentWeek,
                    relatedIncident = incident
                )
            }
            val preTrendRoundState = state.copy(
                currentWeek = nextWeek,
                franchises = playoffDisciplinary.updatedFranchises,
                disciplinaryState = playoffDisciplinary.updatedState,
                officiatingState = playoffOfficiating.updatedState,
                stadiumDealState = playoffRoundStadiumState,
                leagueSponsors = playoffRoundSponsors,
                leagueIntegrityScore = (state.leagueIntegrityScore + playoffOfficiating.integrityDelta).coerceIn(20, 100),
                minorLeagueState = minorPoRound.minorState,
                cashReservesMillions = (state.cashReservesMillions + playoffRivalryCashCut - playoffOfficiating.weeklyTechOpsCostMillions).coerceAtLeast(0L),
                leagueValuationMillions = state.leagueValuationMillions + playoffFacilityTick.totalLeagueValuationGainMillions,
                lastRevenueBreakdown = playoffRevenue,
                playoffMatches = annotatedAllMatchesSoFar + nextMatches,
                recordBook = statsResult.recordBook,
                weeklyAwards = updatedWeeklyAwards,
                currentWeekPlayerStats = playoffRoundStats,
                seasonPlayerStats = statsResult.seasonStats,
                careerPlayerStats = statsResult.careerStats,
                injuryReportHistory = (playoffInjurySim.newInjuryLogs + state.injuryReportHistory).take(50),
                rivalries = playoffRivalryResult.updatedRivalries,
                freeAgentPool = playoffFaSweep.updatedPool,
                freeAgentSignings = (playoffFaSweep.newSignings + state.freeAgentSignings).take(40),
                recentNews = (listOf(roundHeadline) + playoffDisciplinary.headlines + playoffOfficiating.headlines + minorPoRound.headlines + playoffFaSweep.headlines + playoffFacilityTick.headlines + playoffRivalryResult.headlines + playoffInjurySim.headlines + recoveryTick.clearanceHeadlines + statsResult.brokenAlerts + listOfNotNull(potwNews) + state.recentNews).take(20)
            )
            val securityRoundState = preTrendRoundState.copy(
                jobSecurityLegacyState = JobSecurityLegacyEngine.recalculateLiveSecurity(
                    current = preTrendRoundState.jobSecurityLegacyState,
                    input = preTrendRoundState.toPerformanceInput(),
                    eventNote = "$roundLabel concluded; playoff ratings & officiating audited."
                )
            )
            val updatedTrends = appendDashboardTrendPoint(
                state = securityRoundState,
                periodLabel = "PO W$nextWeek",
                eventTag = "po_w$nextWeek"
            )
            return securityRoundState.copy(dashboardTrends = updatedTrends)
        }
    }

    private fun buildNextPlayoffRound(
        currentRoundName: String,
        nextWeek: Int,
        franchises: List<Franchise>,
        playoffRules: PlayoffRules,
        roundWinners: List<Franchise>,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>
    ): Pair<List<GameMatch>, String> {
        val seeds = calculatePlayoffSeeds(franchises, playoffRules)
        val matches: List<GameMatch>
        val headline: String
        when (currentRoundName) {
            "Global Opening Round" -> {
                val byeTeamsCount = (8 - roundWinners.size).coerceAtLeast(0)
                val qfField = seeds.take(byeTeamsCount).map { it.franchise } + roundWinners
                val qfMatches = mutableListOf<GameMatch>()
                for (i in 0 until qfField.size / 2) {
                    qfMatches.add(
                        GameMatch("playoff_qf_${i + 1}", nextWeek, qfField[i].id, qfField[qfField.size - 1 - i].id, roundName = "Playoff Quarterfinals")
                    )
                }
                matches = qfMatches
                headline = "Global Opening Round concluded! 8-team Playoff Quarterfinals bracket locked."
            }
            "Wild Card Opening Round" -> {
                val seed1 = seeds[0].franchise
                val seed2 = seeds[1].franchise
                matches = listOf(
                    GameMatch("playoff_semi1", nextWeek, seed1.id, roundWinners.last().id, roundName = "Championship Semifinals"),
                    GameMatch("playoff_semi2", nextWeek, seed2.id, roundWinners.first().id, roundName = "Championship Semifinals")
                )
                headline = "Wild Card round complete! Semifinals: ${seed1.teamFullName} vs ${roundWinners.last().teamFullName} & ${seed2.teamFullName} vs ${roundWinners.first().teamFullName}"
            }
            "Playoff Quarterfinals" -> {
                val semiField = if (roundWinners.size == 3 && seeds.isNotEmpty()) {
                    listOf(seeds[0].franchise) + roundWinners
                } else roundWinners
                matches = listOf(
                    GameMatch("playoff_semi1", nextWeek, semiField[0].id, semiField.getOrElse(3) { semiField.last() }.id, roundName = "Championship Semifinals"),
                    GameMatch("playoff_semi2", nextWeek, semiField.getOrElse(1) { semiField[0] }.id, semiField.getOrElse(2) { semiField.last() }.id, roundName = "Championship Semifinals")
                )
                headline = "Quarterfinals concluded! 4 contenders advance to the Championship Semifinals."
            }
            else -> {
                val finalsHome = if (roundWinners.size >= 2) roundWinners[0] else {
                    seeds.firstOrNull { s -> s.franchise.id != roundWinners[0].id }?.franchise ?: roundWinners[0]
                }
                val finalsAway = if (roundWinners.size >= 2) roundWinners[1] else roundWinners[0]
                matches = listOf(
                    GameMatch("playoff_finals", nextWeek, finalsHome.id, finalsAway.id, roundName = "Championship Finals")
                )
                headline = "Semifinals concluded! Finals showdown: ${finalsHome.teamFullName} vs ${finalsAway.teamFullName}"
            }
        }
        val annotated = RivalryEngine.annotateMatchesWithRivalries(matches, franchises, rivalries, cities, isPlayoffs = true)
        return annotated to headline
    }

    fun finalizeAndInaugurateNewSeason(state: LeagueUiState): LeagueUiState {
        val rawSession = state.activeOffSeasonSession ?: OffSeasonEngine.createOffSeasonSession(
            year = state.currentYear,
            franchises = state.franchises,
            sportType = state.sportType,
            ruleBook = state.ruleBook,
            initialParity = state.competitiveParityScore,
            careerStats = state.careerPlayerStats,
            seasonStats = state.seasonPlayerStats,
            weeklyAwards = state.weeklyAwards,
            champion = state.seasonChampion,
            autoCompleteDraft = true
        )
        val session = if (!rawSession.isDraftCompleted) {
            OffSeasonEngine.autoCompleteDraft(rawSession, state.franchises, state.sportType)
        } else rawSession

        val archivedCeremony = session.awardsCeremony?.copy(isPresented = true)
        val updatedPastCeremonies = if (archivedCeremony != null) {
            if (state.pastAwardsCeremonies.any { it.year == archivedCeremony.year }) {
                state.pastAwardsCeremonies.map { if (it.year == archivedCeremony.year) archivedCeremony else it }
            } else listOf(archivedCeremony) + state.pastAwardsCeremonies
        } else state.pastAwardsCeremonies

        val existingHofIds = state.hallOfFame.map { it.id }.toSet()
        val newInductees = session.hallOfFameInductees.filter { it.id !in existingHofIds }
        val updatedHallOfFame = newInductees + state.hallOfFame

        val finishedSeasonRecords = state.franchises.associate { it.id to (it.wins to it.losses) }
        val preHealFranchises = state.franchises.map { team ->
            val movement = session.teamMovements.find { it.franchiseId == team.id }
            val newRating = movement?.endRating ?: team.rosterRating
            val draftedForTeam = session.draftPicks.filter { it.franchiseId == team.id }.mapNotNull { it.draftedPlayer }
            val existingPlayerIds = team.roster.map { it.id }.toSet()
            val newRookiesToAdd = draftedForTeam.filter { it.id !in existingPlayerIds }.mapIndexed { offset, rp ->
                rp.copy(depthOrder = team.roster.size + offset, isStarter = false)
            }
            val addedPayroll = newRookiesToAdd.sumOf { it.salaryMillions }
            team.copy(
                rosterRating = newRating,
                annualPayrollMillions = team.annualPayrollMillions + addedPayroll,
                roster = team.roster + newRookiesToAdd,
                wins = 0, losses = 0, pointsFor = 0, pointsAgainst = 0, rivalryTicketBonusMillions = 0.0
            )
        }
        val healedFranchises = InjuryEngine.recoverOffSeasonInjuries(preHealFranchises)
        val offSeasonFacilityTick = FacilityEngine.advanceWeeklyConstruction(healedFranchises, completeAllRemaining = true)
        val updatedFranchises = FacilityEngine.applyOffSeasonFacilityAppreciation(offSeasonFacilityTick.updatedFranchises)
        val facilityAppreciationDelta = updatedFranchises.sumOf { it.facilities.accumulatedFacilityValueMillions } -
            offSeasonFacilityTick.updatedFranchises.sumOf { it.facilities.accumulatedFacilityValueMillions }

        val updatedCareerStats = state.careerPlayerStats.mapValues { (_, cStat) ->
            if (!cStat.isRetired) cStat.copy(seasonsPlayed = cStat.seasonsPlayed + 1) else cStat
        }
        val nextYear = state.currentYear + 1
        val newBroadcastDeal = if (state.activeBroadcast.yearsRemaining <= 1) {
            state.activeBroadcast.copy(
                annualPayoutMillions = (state.activeBroadcast.annualPayoutMillions * 1.15).toLong(),
                yearsRemaining = 3
            )
        } else {
            state.activeBroadcast.copy(yearsRemaining = state.activeBroadcast.yearsRemaining - 1)
        }

        val (rolledLeagueSponsors, sponsorExpiryNews) = LeagueSponsorsEngine.advanceOffseasonContracts(state.leagueSponsors)

        // Decrement CBA years remaining & scale next season's Salary Cap from annual league revenue (including stadium naming rights & league sponsors)
        val capProjection = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = state.franchises,
            activeBroadcast = newBroadcastDeal,
            unionAgreement = state.unionAgreement,
            leagueSponsors = rolledLeagueSponsors
        )
        val decrementedCbaYears = (state.unionAgreement.cbaYearsRemaining - 1).coerceAtLeast(0)
        val rolledUnionAgreement = state.unionAgreement.copy(
            cbaYearsRemaining = decrementedCbaYears,
            salaryCapMillions = capProjection.nextSalaryCapMillions
        )

        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(
            franchises = updatedFranchises, cities = state.cities, currentRivalries = state.rivalries, seasonYear = nextYear
        )
        val targetRegWeeks = state.ruleBook.regularSeasonWeeks.coerceIn(14, 22)
        val rawUpcoming = state.seasonScheduleState.upcomingPlan
        val promotedSchedule = when {
            rawUpcoming != null && rawUpcoming.slots.isNotEmpty() -> {
                val aligned = if (rawUpcoming.regularSeasonWeeks != targetRegWeeks) {
                    ScheduleCalendarEngine.resizeScheduleForSeasonLength(
                        plan = rawUpcoming.copy(targetYear = nextYear),
                        newRegularSeasonWeeks = targetRegWeeks,
                        franchises = updatedFranchises,
                        rivalries = syncedRivalries,
                        unionAgreement = rolledUnionAgreement
                    )
                } else {
                    rawUpcoming.copy(targetYear = nextYear)
                }
                if (aligned.isApproved) aligned else aligned.copy(
                    approvalStatus = ScheduleApprovalStatus.APPROVED_BY_BOARD,
                    lastVoteSummary = "Ratified automatically with $nextYear season inauguration."
                )
            }
            else -> ScheduleCalendarEngine.generateFullSeasonSchedule(
                targetYear = nextYear,
                regularSeasonWeeks = targetRegWeeks,
                franchises = updatedFranchises,
                rivalries = syncedRivalries,
                cities = state.cities,
                sportType = state.sportType
            ).copy(
                approvalStatus = ScheduleApprovalStatus.APPROVED_BY_BOARD,
                lastVoteSummary = "Auto-generated $targetRegWeeks-week schedule for Season $nextYear."
            )
        }
        val nextScheduleHistory = state.seasonScheduleState.activeSeasonSchedule?.let {
            (listOf(it) + state.seasonScheduleState.pastSeasonPlans).take(6)
        } ?: state.seasonScheduleState.pastSeasonPlans
        val nextSeasonScheduleState = SeasonScheduleState(
            upcomingPlan = null,
            activeSeasonSchedule = promotedSchedule,
            pastSeasonPlans = nextScheduleHistory
        )
        val initialMatches = ScheduleCalendarEngine.buildWeekMatchesFromPlan(
            plan = promotedSchedule,
            week = 1,
            franchises = updatedFranchises,
            rivalries = syncedRivalries,
            cities = state.cities,
            sportType = state.sportType
        )
        val completedSession = session.copy(
            isCompleted = true, awardsCeremony = archivedCeremony, hofClassRatified = true, currentStep = OffSeasonStep.SUMMARY
        )
        val hofNote = if (newInductees.isNotEmpty()) " Hall of Fame Class (${newInductees.joinToString(", ") { it.name }}) enshrined!" else ""
        val facilityNote = if (facilityAppreciationDelta > 0L) " Franchise infrastructure appreciated +\$${facilityAppreciationDelta}M." else ""
        val schedNote = " ${promotedSchedule.regularSeasonWeeks}W calendar (${promotedSchedule.holidayShowcaseCount} Holiday, ${promotedSchedule.internationalGameCount} Intl) locked."
        val capHeadlineNote = if (capProjection.salaryCapIncreaseMillions > 0L) {
            " Salary Cap rose +\$${capProjection.salaryCapIncreaseMillions}M to \$${capProjection.nextSalaryCapMillions}M (backed by \$${capProjection.totalNamingRightsRevenueMillions}M stadium naming rights & \$${capProjection.annualLeagueRevenueMillions}M annual revenue)."
        } else ""
        val headline = "OFF-SEASON CONCLUDED: Season $nextYear Pre-Season Training Camp opens with ${session.draftPicks.size} newly drafted rookies! Parity Index: ${session.finalParityScore}/100.$hofNote$facilityNote$schedNote$capHeadlineNote"

        val refreshedFaPool = FreeAgencyEngine.replenishFreeAgentPool(
            currentPool = state.freeAgentPool, franchises = updatedFranchises, sportType = state.sportType,
            seasonYear = nextYear, minPoolSize = 22, seedOffset = nextYear
        )
        val offseasonRivalCheck = RivalLeagueEngine.processOffseasonPoaching(
            rivalLeague = state.rivalLeague,
            freeAgentPool = refreshedFaPool,
            seasonYear = state.currentYear,
            existingCities = state.cities
        )
        if (offseasonRivalCheck.triggeredAbsorptionNegotiation != null) {
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_rival_absorption_roll_${state.currentYear}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = offseasonRivalCheck.triggeredAbsorptionNegotiation.headline,
                    actionLabel = "Summit",
                    targetRoute = DashboardSnackbarRoute.RIVAL_WATCH
                )
            )
        }
        val rolledPickFranchises = TradeEngine.rollDraftPicksForNewSeason(
            franchises = updatedFranchises, finishedYear = state.currentYear, nextYear = nextYear,
            deferredSlots = session.draftOrderSlots.filter { it.retainedDueToProtection }
        )
        val nextSeasonLedger = TradeEngine.buildTradedPickLedger(
            franchises = rolledPickFranchises, currentYear = nextYear, activeSession = null, latestCompletedOffSeason = completedSession
        )
        val minorRollover = MinorLeagueEngine.rolloverForNewSeason(
            currentMinorState = state.minorLeagueState, franchises = rolledPickFranchises,
            sportType = state.sportType, finishedYear = state.currentYear, nextYear = nextYear
        )
        val stadiumRolloverRaw = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = state.stadiumDealState,
            franchises = minorRollover.updatedFranchises,
            seasonYear = state.currentYear,
            seasonRecordsByFranchiseId = finishedSeasonRecords
        )
        val stadiumStateAfterRebuilds = offSeasonFacilityTick.rebuiltStadiumTeamIds.fold(stadiumRolloverRaw.updatedStadiumDealState) { acc, teamId ->
            val rebuilt = stadiumRolloverRaw.updatedFranchises.find { it.id == teamId } ?: return@fold acc
            val rec = finishedSeasonRecords[teamId]
            StadiumDealEngine.enqueueNamingRightsOfferForFranchise(
                currentState = acc,
                franchise = rebuilt,
                seasonYear = state.currentYear,
                triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                winsOverride = rec?.first,
                lossesOverride = rec?.second,
                allFranchises = stadiumRolloverRaw.updatedFranchises
            )
        }
        val stadiumRolloverCheck = stadiumRolloverRaw.copy(updatedStadiumDealState = stadiumStateAfterRebuilds)
        if (stadiumRolloverCheck.triggeredBankruptcyScandals.isNotEmpty()) {
            val scandal = stadiumRolloverCheck.triggeredBankruptcyScandals.first()
            emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_sponsor_scandal_roll_${state.currentYear}_${scandal.franchiseId}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = scandal.headline,
                    actionLabel = "Replace",
                    targetRoute = DashboardSnackbarRoute.STADIUM_ULTIMATUM
                )
            )
        }

        val rolledRefs = state.officiatingState.referees.map {
            it.copy(
                fatigueLevel = 8,
                blownCallsSeason = 0,
                gamesOfficiatedSeason = 0,
                isRestedOrBenched = false,
                benchWeeksRemaining = 0
            )
        }
        val rolledRefUnion = state.officiatingState.unionState.copy(
            cbaYearsRemaining = (state.officiatingState.unionState.cbaYearsRemaining - 1).coerceAtLeast(0)
        )
        val rolledOfficiatingState = state.officiatingState.copy(
            referees = rolledRefs,
            unionState = rolledRefUnion,
            assignments = OfficiatingEngine.buildAssignmentsForMatches(
                matches = initialMatches,
                referees = rolledRefs,
                franchises = minorRollover.updatedFranchises,
                unionState = rolledRefUnion,
                techCatalog = state.officiatingState.techCatalog,
                week = 1
            ),
            lastAuditHeadline = "Season $nextYear NLOA officiating roster reset and certified."
        )

        val reputationSyncedFranchises = TeamReputationEngine.refreshAllFranchiseReputations(
            franchises = stadiumRolloverCheck.updatedFranchises,
            salaryCapMillions = rolledUnionAgreement.salaryCapMillions,
            incrementLowSpendingSeason = false
        )
        val preTriggerState = state.copy(
            currentYear = nextYear,
            currentWeek = 1,
            totalWeeks = ScheduleCalendarEngine.totalWeeksFor(targetRegWeeks),
            seasonPhase = SeasonPhase.PRE_SEASON,
            competitiveParityScore = session.finalParityScore,
            franchises = reputationSyncedFranchises,
            minorLeagueState = minorRollover.minorState,
            freeAgentPool = offseasonRivalCheck.updatedFreeAgentPool,
            rivalLeague = offseasonRivalCheck.updatedRivalLeague,
            stadiumDealState = stadiumRolloverCheck.updatedStadiumDealState,
            leagueSponsors = rolledLeagueSponsors,
            unionAgreement = rolledUnionAgreement,
            officiatingState = rolledOfficiatingState,
            leagueValuationMillions = state.leagueValuationMillions + offSeasonFacilityTick.totalLeagueValuationGainMillions + facilityAppreciationDelta,
            leagueIntegrityScore = (state.leagueIntegrityScore - stadiumRolloverCheck.totalLeaguePrDrain).coerceIn(20, 100),
            activeBroadcast = newBroadcastDeal,
            currentWeekMatches = initialMatches,
            playoffMatches = emptyList(),
            seasonChampion = null,
            seasonRunnerUp = null,
            showOffSeasonScreen = false,
            activeOffSeasonSession = null,
            latestCompletedOffSeason = completedSession,
            seasonScheduleState = nextSeasonScheduleState,
            hallOfFame = updatedHallOfFame,
            pastAwardsCeremonies = updatedPastCeremonies,
            seasonPlayerStats = emptyMap(),
            currentWeekPlayerStats = emptyList(),
            careerPlayerStats = updatedCareerStats,
            tradedPickLedger = nextSeasonLedger
        )

        val basePreRolloverSecurity = preTriggerState.jobSecurityLegacyState
        val pendingOrFreshReview = when {
            basePreRolloverSecurity.pendingAnnualReview != null && !basePreRolloverSecurity.pendingAnnualReview.isRatified ->
                basePreRolloverSecurity.pendingAnnualReview
            basePreRolloverSecurity.reviewHistory.none { it.seasonYear == state.currentYear } ->
                JobSecurityLegacyEngine.generateAnnualReview(
                    current = basePreRolloverSecurity,
                    input = state.toPerformanceInput(
                        seasonChampionNameOverride = state.seasonChampion?.teamFullName
                            ?: state.valuationHistory.firstOrNull()?.championName
                    )
                )
            else -> null
        }

        val postReviewRolloverState = if (pendingOrFreshReview != null) {
            val lowestPillar = listOf(
                StakeholderPillar.OWNERS to pendingOrFreshReview.ownersScore,
                StakeholderPillar.PLAYERS to pendingOrFreshReview.playersScore,
                StakeholderPillar.FANS to pendingOrFreshReview.fansScore,
                StakeholderPillar.MEDIA to pendingOrFreshReview.mediaScore
            ).minByOrNull { it.second }?.first ?: StakeholderPillar.OWNERS
            val autoChoice = when (lowestPillar) {
                StakeholderPillar.OWNERS -> ReviewResponseChoice.OWNER_GROWTH_CHARTER
                StakeholderPillar.PLAYERS -> ReviewResponseChoice.PLAYER_PARTNERSHIP_ACCORD
                StakeholderPillar.FANS -> ReviewResponseChoice.FAN_ACCESS_BLITZ
                StakeholderPillar.MEDIA -> ReviewResponseChoice.MEDIA_SPECTACLE_SHOWCASE
            }
            val outcome = JobSecurityLegacyEngine.ratifyAnnualReview(
                current = basePreRolloverSecurity,
                review = pendingOrFreshReview,
                response = autoChoice
            )
            preTriggerState.copy(
                cashReservesMillions = (preTriggerState.cashReservesMillions + outcome.cashDeltaMillions).coerceAtLeast(0L),
                leagueValuationMillions = (preTriggerState.leagueValuationMillions + outcome.valuationDeltaMillions).coerceAtLeast(100L),
                totalLeagueFanbaseMillions = ((preTriggerState.totalLeagueFanbaseMillions + outcome.fanbaseDeltaMillions) * 100.0)
                    .let { kotlin.math.round(it) / 100.0 }
                    .coerceAtLeast(1.0),
                ownerApprovalRating = (preTriggerState.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(20, 100),
                unionAgreement = preTriggerState.unionAgreement.copy(
                    unionSatisfaction = (preTriggerState.unionAgreement.unionSatisfaction + outcome.unionSatisfactionDelta).coerceIn(10, 100),
                    strikeRisk = (preTriggerState.unionAgreement.strikeRisk + outcome.strikeRiskDelta).coerceIn(0, 95)
                ),
                franchises = preTriggerState.franchises.map { team ->
                    team.copy(
                        fanMorale = (team.fanMorale + outcome.fanMoraleDelta).coerceIn(20, 99),
                        fanLoyalty = (team.fanLoyalty + outcome.fanLoyaltyDelta).coerceIn(15, 100),
                        ownership = team.ownership.copy(
                            ownerApproval = (team.ownership.ownerApproval + outcome.ownerApprovalDelta).coerceIn(20, 100)
                        )
                    )
                },
                jobSecurityLegacyState = outcome.updatedState,
                recentNews = (listOf(outcome.headline) + preTriggerState.recentNews).take(20)
            )
        } else preTriggerState

        val triggerEval = evaluateLaborUnionTriggers(postReviewRolloverState, revenueBreakdown = null, justRolledSeason = true)
        val rolloverSentiment = FanSentimentEngine.recordDecisionReaction(
            currentState = postReviewRolloverState.fanSentimentState,
            category = FanDecisionCategory.EXPANSION_TRADE,
            decisionTitle = "Season $nextYear Inauguration & Draft Class",
            decisionSummary = headline,
            casualDelta = 3,
            dieHardDelta = 2,
            franchiseName = postReviewRolloverState.leagueName,
            cityName = postReviewRolloverState.franchises.firstOrNull()?.cityName ?: "Metro",
            sportType = postReviewRolloverState.sportType,
            seasonYear = nextYear,
            weekNumber = 0,
            seedOffset = nextYear
        )
        val preTrendFinalState = postReviewRolloverState.copy(
            unionAgreement = triggerEval.updatedUnionAgreement,
            activeLaborTrigger = triggerEval.triggeredEvent ?: postReviewRolloverState.activeLaborTrigger,
            activeLaborNegotiationOverlay = triggerEval.negotiationOverlay ?: postReviewRolloverState.activeLaborNegotiationOverlay,
            laborTriggerHistory = _simulationState.value.laborTriggerHistory,
            fanSentimentState = rolloverSentiment,
            recentNews = (triggerEval.headlines + listOf(headline) + stadiumRolloverCheck.headlines + listOfNotNull(offseasonRivalCheck.headline) + minorRollover.headlines + offSeasonFacilityTick.headlines + postReviewRolloverState.recentNews).take(20)
        ).let { s ->
            val beforeUnlockedIds = s.jobSecurityLegacyState.milestones
                .filter { it.isUnlocked }
                .map { it.id }
                .toSet()
            val updatedSec = JobSecurityLegacyEngine.recalculateLiveSecurity(
                current = s.jobSecurityLegacyState,
                input = s.toPerformanceInput(),
                isSeasonRollover = true
            )
            val newlyUnlocked = updatedSec.milestones.filter { it.isUnlocked && it.id !in beforeUnlockedIds }
            newlyUnlocked.firstOrNull()?.let { milestone ->
                emitDashboardSnackbar(
                    DashboardSnackbarEvent(
                        id = "snack_legacy_rollover_${nextYear}_${milestone.id.name}",
                        type = DashboardAlertType.SEASON_MILESTONE,
                        severity = DashboardAlertSeverity.MILESTONE,
                        message = "League Legacy Unlocked: ${milestone.title} (+${milestone.rewardLegacyPoints} Pts)",
                        actionLabel = "Legacy",
                        targetRoute = DashboardSnackbarRoute.STANDINGS
                    )
                )
            }
            val rolloverMilestoneHeadlines = newlyUnlocked.map { m ->
                "LEAGUE LEGACY MILESTONE UNLOCKED: '${m.title}' (+${m.rewardLegacyPoints} Legacy Pts) — ${m.progressNote}"
            }
            s.copy(
                jobSecurityLegacyState = updatedSec,
                recentNews = (rolloverMilestoneHeadlines + s.recentNews).take(20)
            )
        }
        val updatedTrends = appendDashboardTrendPoint(
            state = preTrendFinalState,
            periodLabel = "Y${nextYear % 100} W1",
            eventTag = "rollover_$nextYear"
        )
        val finalState = preTrendFinalState.copy(dashboardTrends = updatedTrends)

        syncFromLeagueState(finalState)
        _seasonProgressionEvents.tryEmit(SeasonProgressionEvent.NewSeasonInaugurated(nextYear, finalState.unionAgreement.cbaYearsRemaining))
        return finalState
    }
}
