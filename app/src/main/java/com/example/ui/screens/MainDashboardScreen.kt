package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LeagueRevenueHistoryEntity
import com.example.data.LeagueState
import com.example.model.BroadcastBidStatus
import com.example.model.CoreSimulationState
import com.example.model.DashboardSnackbarEvent
import com.example.model.LeagueSimulationEngine
import com.example.model.SeasonPhase
import com.example.model.StadiumDealEngine
import com.example.ui.components.DashboardSnackbarEffect
import com.example.ui.components.DashboardSnackbarHost
import com.example.ui.components.RechartsDualTrendLineChart
import com.example.ui.components.RechartsExecutiveTrendsChart
import com.example.ui.components.RechartsFiveYearBroadcastContractsChart
import com.example.ui.components.RechartsFiveYearRevenueChart
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlin.math.roundToInt

/**
 * Standalone Main Dashboard Composable connected directly to [LeagueViewModel] and its
 * [com.example.viewmodel.CoreSimulationViewModel] state streams.
 */
@Composable
fun MainDashboardScreen(
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val simulationState by viewModel.coreSimulationViewModel.simulationState.collectAsStateWithLifecycle()
    val persistedLeagueState by viewModel.persistedLeagueState.collectAsStateWithLifecycle()
    val fiveYearRevenueHistory by viewModel.fiveYearRevenueHistory.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarAlert by remember { mutableStateOf<DashboardSnackbarEvent?>(null) }

    DashboardSnackbarEffect(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onActiveAlertChange = { activeSnackbarAlert = it }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            DashboardSnackbarHost(
                hostState = snackbarHostState,
                currentAlert = activeSnackbarAlert
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("main_dashboard_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardExecutiveTopCard(
                    uiState = uiState,
                    onAdvanceWeek = {
                        if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) {
                            viewModel.openOffSeasonSummit(true)
                        } else {
                            viewModel.advanceWeek()
                        }
                    }
                )
            }

            item {
                MainDashboardMetricsPanel(
                    uiState = uiState,
                    simulationState = simulationState,
                    persistedLeagueState = persistedLeagueState,
                    fiveYearRevenueHistory = fiveYearRevenueHistory,
                    onSeasonWeekClick = { viewModel.setSelectedTab(5) },
                    onBankBalanceClick = { viewModel.setSelectedTab(3) },
                    onFanSentimentClick = { viewModel.setSelectedTab(1) },
                    onStrikeRiskClick = { viewModel.openLaborUnionScreen(true) },
                    onFranchisesClick = { viewModel.setSelectedTab(1) },
                    onOpenBroadcast = { viewModel.openBroadcastNegotiations(true) },
                    onOpenExpansion = { viewModel.openExpansionRequestsManagement(true) },
                    onOpenFacilities = { viewModel.openFacilityHub() },
                    onOpenSchedule = { viewModel.openScheduleCalendarScreen(true) },
                    onOpenOffSeason = { viewModel.openOffSeasonSummit(true) },
                    onOpenCrisisHub = { viewModel.openCrisisEventsScreen(true) },
                    onOpenRivalWatch = { viewModel.openRivalWatchModal(true) },
                    onOpenLeagueSponsors = { viewModel.openLeagueSponsorsScreen(true) }
                )
            }
        }
    }
}

@Composable
private fun DashboardExecutiveTopCard(
    uiState: LeagueUiState,
    onAdvanceWeek: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_executive_header"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surfaceVariant,
                            CyanAccent.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${uiState.sportType.displayName.uppercase()} • SEASON ${uiState.currentYear}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.leagueName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.seasonPhase.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAdvanceWeek,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dashboard_simulate_week_button")
                ) {
                    Icon(
                        imageVector = if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) Icons.Default.Analytics else Icons.Default.PlayArrow,
                        contentDescription = "Advance Season Simulation",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) "Off-Season" else "Advance",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * High-level executive league metrics dashboard showing Current Season Week,
 * Bank Balance (with weekly cash flow & valuation), and Fan Sentiment
 * (composite loyalty/morale score & global fanbase), plus secondary governance KPIs.
 */
@Composable
fun MainDashboardMetricsPanel(
    uiState: LeagueUiState,
    simulationState: CoreSimulationState,
    onSeasonWeekClick: () -> Unit,
    onBankBalanceClick: () -> Unit,
    onFanSentimentClick: () -> Unit,
    onStrikeRiskClick: () -> Unit,
    onIntegrityClick: (() -> Unit)? = null,
    onJobSecurityClick: (() -> Unit)? = null,
    persistedLeagueState: LeagueState? = null,
    fiveYearRevenueHistory: List<LeagueRevenueHistoryEntity> = emptyList(),
    onFranchisesClick: (() -> Unit)? = null,
    onOpenBroadcast: (() -> Unit)? = null,
    onOpenExpansion: (() -> Unit)? = null,
    onOpenFacilities: (() -> Unit)? = null,
    onOpenSchedule: (() -> Unit)? = null,
    onOpenOffSeason: (() -> Unit)? = null,
    onOpenCrisisHub: (() -> Unit)? = null,
    onOpenRivalWatch: (() -> Unit)? = null,
    onOpenLeagueSponsors: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val avgMorale = if (uiState.franchises.isNotEmpty()) {
        uiState.franchises.map { it.fanMorale }.average()
    } else 75.0
    val avgLoyalty = if (uiState.franchises.isNotEmpty()) {
        uiState.franchises.map { it.fanLoyalty }.average()
    } else 75.0
    val fanSentimentScore = ((avgMorale * 0.55) + (avgLoyalty * 0.45)).roundToInt().coerceIn(0, 100)
    val sentimentLabel = when {
        fanSentimentScore >= 85 -> "Electric"
        fanSentimentScore >= 72 -> "Devoted"
        fanSentimentScore >= 58 -> "Steady"
        fanSentimentScore >= 42 -> "Restless"
        else -> "Frustrated"
    }
    val sentimentColor = when {
        fanSentimentScore >= 72 -> CyanAccent
        fanSentimentScore >= 55 -> GoldPrimary
        else -> CrimsonAlert
    }

    val weekProgressTarget = when (uiState.seasonPhase) {
        SeasonPhase.PRE_SEASON -> 0.05f
        SeasonPhase.REGULAR_SEASON -> (uiState.currentWeek.toFloat() / uiState.totalWeeks.coerceAtLeast(1)).coerceIn(0.05f, 1f)
        SeasonPhase.ALL_STAR_INTERNATIONAL -> 0.5f
        SeasonPhase.PLAYOFFS -> 0.92f
        SeasonPhase.OFF_SEASON -> 1f
    }
    val animatedWeekProgress by animateFloatAsState(
        targetValue = weekProgressTarget,
        animationSpec = spring(stiffness = 300f),
        label = "seasonWeekProgress"
    )

    val netCashFlow = simulationState.lastRevenueBreakdown?.netCashFlowMillions
    val bankDeltaLabel = when {
        netCashFlow == null -> "Enterprise Val: \$${uiState.leagueValuationMillions}M"
        netCashFlow >= 0L -> "+\$${netCashFlow}M/wk • Val: \$${uiState.leagueValuationMillions}M"
        else -> "-\$${kotlin.math.abs(netCashFlow)}M/wk • Val: \$${uiState.leagueValuationMillions}M"
    }

    val milestoneText = when (uiState.seasonPhase) {
        SeasonPhase.PRE_SEASON -> "Training Camp • Week 1 Tip-off Next"
        SeasonPhase.REGULAR_SEASON -> when {
            uiState.currentWeek < 7 -> "All-Star Showcase in ${7 - uiState.currentWeek} wk(s)"
            uiState.currentWeek < 14 -> "Playoffs Lock in ${14 - uiState.currentWeek} wk(s)"
            else -> "Final Regular Season Push"
        }
        SeasonPhase.ALL_STAR_INTERNATIONAL -> "Mid-Season Global Showcase"
        SeasonPhase.PLAYOFFS -> "Championship Bracket Active"
        SeasonPhase.OFF_SEASON -> "Draft, Free Agency & Rollover"
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_dashboard_panel")
    ) {
        val isWide = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 840.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SeasonWeekMetricCard(
                        uiState = uiState,
                        animatedProgress = animatedWeekProgress,
                        milestoneText = milestoneText,
                        onClick = onSeasonWeekClick,
                        modifier = Modifier.weight(1f)
                    )
                    BankBalanceMetricCard(
                        cashReservesMillions = uiState.cashReservesMillions,
                        bankDeltaLabel = bankDeltaLabel,
                        isPositiveFlow = (netCashFlow ?: 1L) >= 0L,
                        onClick = onBankBalanceClick,
                        modifier = Modifier.weight(1f)
                    )
                    FanSentimentMetricCard(
                        sentimentScore = fanSentimentScore,
                        sentimentLabel = sentimentLabel,
                        sentimentColor = sentimentColor,
                        globalFanbaseMillions = uiState.totalLeagueFanbaseMillions,
                        casualScore = uiState.fanSentimentState.casualSentimentScore,
                        dieHardScore = uiState.fanSentimentState.dieHardSentimentScore,
                        onClick = onFanSentimentClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                SeasonWeekMetricCard(
                    uiState = uiState,
                    animatedProgress = animatedWeekProgress,
                    milestoneText = milestoneText,
                    onClick = onSeasonWeekClick,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BankBalanceMetricCard(
                        cashReservesMillions = uiState.cashReservesMillions,
                        bankDeltaLabel = bankDeltaLabel,
                        isPositiveFlow = (netCashFlow ?: 1L) >= 0L,
                        onClick = onBankBalanceClick,
                        modifier = Modifier.weight(1f)
                    )
                    FanSentimentMetricCard(
                        sentimentScore = fanSentimentScore,
                        sentimentLabel = sentimentLabel,
                        sentimentColor = sentimentColor,
                        globalFanbaseMillions = uiState.totalLeagueFanbaseMillions,
                        casualScore = uiState.fanSentimentState.casualSentimentScore,
                        dieHardScore = uiState.fanSentimentState.dieHardSentimentScore,
                        onClick = onFanSentimentClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Secondary Executive Metrics Row: Union Strike Risk, League Integrity, Competitive Parity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val strikeAccent = if (uiState.unionAgreement.strikeRisk > 40) CrimsonAlert else EmeraldAccent
                CompactExecutivePill(
                    label = "Strike Risk",
                    value = "${uiState.unionAgreement.strikeRisk}%",
                    subLabel = "${uiState.unionAgreement.cbaYearsRemaining}y CBA",
                    icon = Icons.Default.Group,
                    accent = strikeAccent,
                    onClick = onStrikeRiskClick,
                    modifier = Modifier.weight(1f),
                    testTag = "metric_strike_risk"
                )
                CompactExecutivePill(
                    label = "Integrity",
                    value = "${uiState.leagueIntegrityScore}/100",
                    subLabel = "Officiating",
                    icon = Icons.Default.Gavel,
                    accent = if (uiState.leagueIntegrityScore >= 80) EmeraldAccent else GoldPrimary,
                    onClick = onIntegrityClick,
                    modifier = Modifier.weight(1f),
                    testTag = "metric_integrity"
                )
                CompactExecutivePill(
                    label = "Parity",
                    value = "${uiState.competitiveParityScore}/100",
                    subLabel = "${uiState.franchises.size} Teams",
                    icon = Icons.Default.Equalizer,
                    accent = RoyalBlueAccent,
                    onClick = onSeasonWeekClick,
                    modifier = Modifier.weight(1f),
                    testTag = "metric_parity"
                )
            }

            // Core League Overview Row (Total Revenue, Active Franchises, Active Labor Union Status)
            val liveRevenueProjection = remember(
                uiState.franchises,
                uiState.activeBroadcast,
                uiState.unionAgreement,
                uiState.leagueSponsors
            ) {
                LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
                    franchises = uiState.franchises,
                    activeBroadcast = uiState.activeBroadcast,
                    unionAgreement = uiState.unionAgreement,
                    leagueSponsors = uiState.leagueSponsors
                )
            }
            val totalRevenueM = persistedLeagueState?.totalRevenueMillions
                ?: liveRevenueProjection.annualLeagueRevenueMillions
            val namingRevM = persistedLeagueState?.annualNamingRightsRevenueMillions
                ?: liveRevenueProjection.totalNamingRightsRevenueMillions
            val broadcastRevM = persistedLeagueState?.annualBroadcastRevenueMillions
                ?: uiState.activeBroadcast.annualPayoutMillions
            val franchiseCount = persistedLeagueState?.franchiseCount ?: uiState.franchises.size
            val eastCount = uiState.franchises.count { it.conference == com.example.model.Conference.EASTERN }
            val westCount = uiState.franchises.count { it.conference == com.example.model.Conference.WESTERN }
            val isLockout = persistedLeagueState?.isLockoutActive ?: uiState.unionAgreement.isLockoutActive
            val cbaYears = persistedLeagueState?.cbaYearsRemaining ?: uiState.unionAgreement.cbaYearsRemaining
            val strikePct = persistedLeagueState?.strikeRisk ?: uiState.unionAgreement.strikeRisk
            val salaryCapM = persistedLeagueState?.salaryCapMillions ?: uiState.unionAgreement.salaryCapMillions
            val unionBadgeValue = when {
                isLockout -> "Lockout"
                cbaYears <= 1 -> "Expiring (${cbaYears}y)"
                else -> "Active CBA"
            }
            val unionAccent = when {
                isLockout || strikePct >= 50 -> CrimsonAlert
                cbaYears <= 1 -> AmberWarning
                else -> EmeraldAccent
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactExecutivePill(
                    label = "Total Revenue",
                    value = "\$${totalRevenueM}M/yr",
                    subLabel = "TV \$${broadcastRevM}M • Naming \$${namingRevM}M",
                    icon = Icons.Default.TrendingUp,
                    accent = EmeraldAccent,
                    onClick = onBankBalanceClick,
                    modifier = Modifier.weight(1f),
                    testTag = "dashboard_metric_total_revenue"
                )
                CompactExecutivePill(
                    label = "Franchises",
                    value = "$franchiseCount Clubs",
                    subLabel = "${eastCount}E • ${westCount}W • Cap \$${salaryCapM}M",
                    icon = Icons.Default.Groups,
                    accent = CyanAccent,
                    onClick = onFranchisesClick ?: onFanSentimentClick,
                    modifier = Modifier.weight(1f),
                    testTag = "dashboard_metric_franchise_count"
                )
                CompactExecutivePill(
                    label = "Labor Union",
                    value = unionBadgeValue,
                    subLabel = "${cbaYears}y CBA • Strike ${strikePct}%",
                    icon = Icons.Default.Handshake,
                    accent = unionAccent,
                    onClick = onStrikeRiskClick,
                    modifier = Modifier.weight(1f),
                    testTag = "dashboard_metric_labor_status"
                )
            }

            UpcomingKeyDecisionsCard(
                uiState = uiState,
                onOpenLabor = onStrikeRiskClick,
                onOpenFacilities = onOpenFacilities ?: onFanSentimentClick,
                onOpenBroadcast = onOpenBroadcast ?: onBankBalanceClick,
                onOpenExpansion = onOpenExpansion ?: onFranchisesClick ?: onFanSentimentClick,
                onOpenCrisis = onOpenCrisisHub ?: onIntegrityClick ?: onSeasonWeekClick,
                onOpenSchedule = onOpenSchedule ?: onSeasonWeekClick,
                onOpenOffSeason = onOpenOffSeason ?: onSeasonWeekClick,
                onOpenRivalWatch = onOpenRivalWatch ?: onSeasonWeekClick,
                onOpenLeagueSponsors = onOpenLeagueSponsors ?: onBankBalanceClick
            )

            JobSecurityLegacyStripCard(
                uiState = uiState,
                onClick = { (onJobSecurityClick ?: onIntegrityClick)?.invoke() }
            )

            // 5-Year Room Database League Revenue Growth Line Chart
            RechartsFiveYearRevenueChart(
                records = fiveYearRevenueHistory,
                modifier = Modifier.fillMaxWidth()
            )

            // 5-Season Franchise Broadcast Contracts Chart (Television & Streaming)
            RechartsFiveYearBroadcastContractsChart(
                franchises = uiState.franchises,
                records = fiveYearRevenueHistory,
                activeBroadcast = uiState.activeBroadcast,
                currentYear = uiState.currentYear,
                managedFranchiseId = uiState.managedFranchiseId,
                modifier = Modifier.fillMaxWidth()
            )

            // League Financial Growth & Fan Sentiment Line Chart
            val trendSeries = uiState.dashboardTrends.ifEmpty { simulationState.dashboardTrends }
            RechartsDualTrendLineChart(
                data = trendSeries,
                modifier = Modifier.fillMaxWidth()
            )
            RechartsExecutiveTrendsChart(
                data = trendSeries,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class UpcomingDecisionSpec(
    val key: String,
    val badge: String,
    val title: String,
    val detail: String,
    val accent: Color,
    val actionLabel: String,
    val onClick: () -> Unit
)

@Composable
private fun UpcomingKeyDecisionsCard(
    uiState: LeagueUiState,
    onOpenLabor: () -> Unit,
    onOpenFacilities: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onOpenExpansion: () -> Unit,
    onOpenCrisis: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenOffSeason: () -> Unit,
    onOpenRivalWatch: () -> Unit,
    onOpenLeagueSponsors: () -> Unit
) {
    val decisions = buildList {
        if (uiState.unionAgreement.isLockoutActive) {
            add(
                UpcomingDecisionSpec(
                    key = "lockout_accord",
                    badge = "URGENT",
                    title = "Ratify Emergency CBA Accord",
                    detail = "Lockout active (${uiState.unionAgreement.lockoutWeeksLost}w lost) — play suspended until resolved.",
                    accent = CrimsonAlert,
                    actionLabel = "Negotiate",
                    onClick = onOpenLabor
                )
            )
        }
        uiState.activeDilemma?.let { dilemma ->
            add(
                UpcomingDecisionSpec(
                    key = "crisis_dilemma",
                    badge = "CRISIS",
                    title = dilemma.title,
                    detail = "Stage ${dilemma.stageNumber}/${dilemma.totalStages} executive ruling awaiting commissioner call.",
                    accent = CrimsonAlert,
                    actionLabel = "Rule",
                    onClick = onOpenCrisis
                )
            )
        }
        uiState.stadiumDealState.activeUltimatum?.let { ultimatum ->
            add(
                UpcomingDecisionSpec(
                    key = "stadium_ultimatum",
                    badge = "ARENA",
                    title = "Stadium Deal: ${ultimatum.franchiseName}",
                    detail = "${ultimatum.stadiumAge}y arena at ${ultimatum.stadiumCondition}% condition requires financing.",
                    accent = AmberWarning,
                    actionLabel = "Review",
                    onClick = onOpenFacilities
                )
            )
        }
        uiState.leagueSponsors.activeUltimatum?.let { sponUlt ->
            add(
                UpcomingDecisionSpec(
                    key = "sponsor_ultimatum",
                    badge = "ULTIMATUM",
                    title = "Sponsor Ultimatum: ${sponUlt.sponsorName}",
                    detail = "\$${sponUlt.sponsorPayoutMillions}M/yr ${sponUlt.slotType.displayName} deal demands max ${sponUlt.requiredMaxSuspensionGames}G / \$${sponUlt.requiredMaxFineThousands}K.",
                    accent = CrimsonAlert,
                    actionLabel = "Resolve",
                    onClick = onOpenLeagueSponsors
                )
            )
        }
        uiState.stadiumDealState.activeNamingRightsOffer?.let { offer ->
            add(
                UpcomingDecisionSpec(
                    key = "naming_rights",
                    badge = "SPONSOR",
                    title = "Naming Rights: ${offer.franchiseName}",
                    detail = "${offer.offers.size} procedural sponsor bids ready to boost cap revenue.",
                    accent = GoldPrimary,
                    actionLabel = "Select",
                    onClick = onOpenFacilities
                )
            )
        }
        uiState.rivalLeague.activeAbsorptionNegotiation?.let {
            add(
                UpcomingDecisionSpec(
                    key = "rival_merger",
                    badge = "MERGER",
                    title = "Rival Merger: ${uiState.rivalLeague.name}",
                    detail = "Absorb top rival clubs for a permanent TV broadcast boost.",
                    accent = GoldPrimary,
                    actionLabel = "Summit",
                    onClick = onOpenRivalWatch
                )
            )
        }
        if (!uiState.unionAgreement.isLockoutActive &&
            (uiState.unionAgreement.cbaYearsRemaining <= 2 || uiState.unionAgreement.strikeRisk >= 30 || uiState.activeLaborTrigger != null)
        ) {
            add(
                UpcomingDecisionSpec(
                    key = "cba_expiration",
                    badge = "LABOR",
                    title = "CBA Talks (${uiState.unionAgreement.cbaYearsRemaining}y Remaining)",
                    detail = "Strike risk ${uiState.unionAgreement.strikeRisk}% • Salary Cap \$${uiState.unionAgreement.salaryCapMillions}M.",
                    accent = if (uiState.unionAgreement.cbaYearsRemaining <= 1) CrimsonAlert else AmberWarning,
                    actionLabel = "CBA",
                    onClick = onOpenLabor
                )
            )
        }
        if (uiState.pendingExpansionRequests.isNotEmpty()) {
            add(
                UpcomingDecisionSpec(
                    key = "expansion_bids",
                    badge = "EXPANSION",
                    title = "${uiState.pendingExpansionRequests.size} Expansion Application(s)",
                    detail = "Evaluate market bids, new arena deals & expansion draft impact.",
                    accent = CyanAccent,
                    actionLabel = "Bids",
                    onClick = onOpenExpansion
                )
            )
        }
        val activeBroadcastOffers = uiState.broadcastBiddingSession.offers.count { it.bidStatus != BroadcastBidStatus.WITHDRAWN }
        if (activeBroadcastOffers > 0 || uiState.activeBroadcast.yearsRemaining <= 2 || uiState.broadcastBiddingSession.exclusivityExpired) {
            add(
                UpcomingDecisionSpec(
                    key = "broadcast_bids",
                    badge = "TV RIGHTS",
                    title = "TV Broadcast Bids (${uiState.activeBroadcast.yearsRemaining}y left)",
                    detail = if (uiState.broadcastBiddingSession.exclusivityExpired) {
                        "Exclusivity expired — sign a partner to recover full TV payouts."
                    } else {
                        "$activeBroadcastOffers network offer(s) active • Current: \$${uiState.activeBroadcast.annualPayoutMillions}M/yr."
                    },
                    accent = if (uiState.broadcastBiddingSession.exclusivityExpired) AmberWarning else CyanAccent,
                    actionLabel = "Negotiate",
                    onClick = onOpenBroadcast
                )
            )
        }
        val atRiskArenas = uiState.franchises.count { StadiumDealEngine.qualifiesForUltimatum(it) }
        if (uiState.stadiumDealState.activeUltimatum == null && atRiskArenas > 0) {
            add(
                UpcomingDecisionSpec(
                    key = "aging_arenas",
                    badge = "VENUES",
                    title = "$atRiskArenas Aging Arena(s) Flagged",
                    detail = "Stadiums >25 yrs & <50% condition face off-season ultimatums.",
                    accent = AmberWarning,
                    actionLabel = "Inspect",
                    onClick = onOpenFacilities
                )
            )
        }
        val (milestoneTitle, milestoneDetail, milestoneAction, milestoneClick) = when (uiState.seasonPhase) {
            SeasonPhase.OFF_SEASON -> listOf("Annual Off-Season Summit", "Complete awards, expansion, draft lottery & new schedule.", "Summit", onOpenOffSeason)
            SeasonPhase.PLAYOFFS -> listOf("Championship Bracket & Rollover", "Advance playoff rounds toward the Finals & Off-Season Summit.", "Calendar", onOpenSchedule)
            else -> when {
                uiState.currentWeek < 7 -> listOf("All-Star Showcase Break (W7)", "Prep All-Star format in ${7 - uiState.currentWeek} week(s).", "Schedule", onOpenSchedule)
                uiState.currentWeek <= uiState.ruleBook.tradeRules.tradeDeadlineWeek -> listOf(
                    "Trade Deadline (W${uiState.ruleBook.tradeRules.tradeDeadlineWeek})",
                    "Review contenders & cap moves in ${(uiState.ruleBook.tradeRules.tradeDeadlineWeek - uiState.currentWeek).coerceAtLeast(0)} week(s).",
                    "Schedule",
                    onOpenSchedule
                )
                else -> listOf(
                    "Playoff Bracket Seeding (${uiState.totalWeeks}W)",
                    "Regular season concludes in ${(uiState.totalWeeks - uiState.currentWeek).coerceAtLeast(0)} week(s).",
                    "Schedule",
                    onOpenSchedule
                )
            }
        }
        @Suppress("UNCHECKED_CAST")
        add(
            UpcomingDecisionSpec(
                key = "season_milestone",
                badge = "UPCOMING",
                title = milestoneTitle as String,
                detail = milestoneDetail as String,
                accent = EmeraldAccent,
                actionLabel = milestoneAction as String,
                onClick = milestoneClick as () -> Unit
            )
        )
    }.take(4)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_upcoming_decisions_card")
            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "Upcoming Key Decisions",
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPCOMING KEY DECISIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 0.8.sp
                    )
                }
                Surface(
                    color = GoldPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${decisions.size} ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            decisions.forEach { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, item.accent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = item.onClick)
                        .testTag("upcoming_decision_${item.key}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = item.accent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = item.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = item.accent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = item.detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = item.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonWeekMetricCard(
    uiState: LeagueUiState,
    animatedProgress: Float,
    milestoneText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weekHeadline = when (uiState.seasonPhase) {
        SeasonPhase.PRE_SEASON -> "Pre-Season"
        SeasonPhase.REGULAR_SEASON -> "Week ${uiState.currentWeek} of ${uiState.totalWeeks}"
        SeasonPhase.ALL_STAR_INTERNATIONAL -> "All-Star Week"
        SeasonPhase.PLAYOFFS -> "Playoffs • W${uiState.currentWeek}"
        SeasonPhase.OFF_SEASON -> "Off-Season"
    }

    Card(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("dashboard_metric_season_week")
            .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Current Season Week",
                            tint = GoldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CURRENT SEASON WEEK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = weekHeadline,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    color = GoldPrimary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${uiState.currentYear}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GoldPrimary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Text(
                text = milestoneText,
                style = MaterialTheme.typography.bodySmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BankBalanceMetricCard(
    cashReservesMillions: Long,
    bankDeltaLabel: String,
    isPositiveFlow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (cashReservesMillions >= 25L && isPositiveFlow) EmeraldAccent else if (cashReservesMillions < 15L) CrimsonAlert else GoldPrimary
    Card(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("dashboard_metric_bank_balance")
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BANK BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "League Bank Balance",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "\$${cashReservesMillions}M",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.testTag("metric_cash_reserves")
            )

            Text(
                text = bankDeltaLabel,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FanSentimentMetricCard(
    sentimentScore: Int,
    sentimentLabel: String,
    sentimentColor: Color,
    globalFanbaseMillions: Double,
    casualScore: Int,
    dieHardScore: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedSentiment by animateFloatAsState(
        targetValue = (sentimentScore / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 300f),
        label = "fanSentimentProgress"
    )

    Card(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("dashboard_metric_fan_sentiment")
            .border(1.dp, sentimentColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FAN SENTIMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(sentimentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Fan Sentiment",
                        tint = sentimentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$sentimentScore%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = sentimentLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = sentimentColor,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            LinearProgressIndicator(
                progress = { animatedSentiment },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = sentimentColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Text(
                text = "Casual $casualScore% • Die-Hard $dieHardScore% (${globalFanbaseMillions}M)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("metric_global_fanbase")
            )
        }
    }
}

@Composable
private fun CompactExecutivePill(
    label: String,
    value: String,
    subLabel: String,
    icon: ImageVector,
    accent: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    testTag: String
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            CompactExecutivePillContent(label = label, value = value, subLabel = subLabel, icon = icon, accent = accent)
        }
    } else {
        Card(
            modifier = modifier
                .testTag(testTag)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            CompactExecutivePillContent(label = label, value = value, subLabel = subLabel, icon = icon, accent = accent)
        }
    }
}

@Composable
private fun CompactExecutivePillContent(
    label: String,
    value: String,
    subLabel: String,
    icon: ImageVector,
    accent: Color
) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
}
