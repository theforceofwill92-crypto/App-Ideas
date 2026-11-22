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
import com.example.model.CoreSimulationState
import com.example.model.DashboardSnackbarEvent
import com.example.model.SeasonPhase
import com.example.ui.components.DashboardSnackbarEffect
import com.example.ui.components.DashboardSnackbarHost
import com.example.ui.components.RechartsDualTrendLineChart
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
                    onSeasonWeekClick = { viewModel.setSelectedTab(5) },
                    onBankBalanceClick = { viewModel.setSelectedTab(3) },
                    onFanSentimentClick = { viewModel.setSelectedTab(1) },
                    onStrikeRiskClick = { viewModel.openLaborUnionScreen(true) }
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
                        avgMorale = avgMorale.roundToInt(),
                        avgLoyalty = avgLoyalty.roundToInt(),
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
                        avgMorale = avgMorale.roundToInt(),
                        avgLoyalty = avgLoyalty.roundToInt(),
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

            JobSecurityLegacyStripCard(
                uiState = uiState,
                onClick = { (onJobSecurityClick ?: onIntegrityClick)?.invoke() }
            )

            // League Financial Growth & Fan Sentiment Line Chart
            val trendSeries = uiState.dashboardTrends.ifEmpty { simulationState.dashboardTrends }
            RechartsDualTrendLineChart(
                data = trendSeries,
                modifier = Modifier.fillMaxWidth()
            )
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
    avgMorale: Int,
    avgLoyalty: Int,
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
                text = "${globalFanbaseMillions}M Fans • M $avgMorale / L $avgLoyalty",
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
