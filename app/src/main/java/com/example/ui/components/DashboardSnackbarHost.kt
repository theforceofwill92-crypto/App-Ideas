package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DashboardAlertSeverity
import com.example.model.DashboardAlertType
import com.example.model.DashboardSnackbarEvent
import com.example.model.DashboardSnackbarRoute
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DashboardSnackbarEffect(
    viewModel: LeagueViewModel,
    snackbarHostState: SnackbarHostState,
    onActiveAlertChange: (DashboardSnackbarEvent?) -> Unit
) {
    LaunchedEffect(viewModel) {
        viewModel.dashboardNotifications.collectLatest { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            onActiveAlertChange(event)
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                when (event.targetRoute) {
                    DashboardSnackbarRoute.LABOR_UNION -> viewModel.openLaborNegotiationOverlay()
                    DashboardSnackbarRoute.FINANCES -> viewModel.setSelectedTab(3)
                    DashboardSnackbarRoute.STANDINGS -> viewModel.setSelectedTab(5)
                    DashboardSnackbarRoute.OFFICIATING -> viewModel.openOfficiatingHub(true)
                    DashboardSnackbarRoute.RULE_COMMITTEE -> viewModel.openRuleCommitteeScreen(true)
                    DashboardSnackbarRoute.DISCIPLINARY_DESK -> viewModel.openDisciplinaryDesk(true)
                    DashboardSnackbarRoute.CRISIS_COMMAND -> viewModel.openCrisisEventsScreen(true)
                    DashboardSnackbarRoute.JOB_SECURITY_LEGACY -> viewModel.openJobSecurityLegacyScreen(true)
                    DashboardSnackbarRoute.RIVAL_WATCH -> viewModel.openRivalWatchModal(true)
                    DashboardSnackbarRoute.STADIUM_ULTIMATUM -> viewModel.setSelectedTab(0)
                    DashboardSnackbarRoute.FAN_SENTIMENT_PULSE -> viewModel.openFanSentimentScreen(true)
                    DashboardSnackbarRoute.BROADCAST_WAR_ROOM -> viewModel.openBroadcastNegotiations(true)
                    DashboardSnackbarRoute.LEAGUE_SPONSORS -> viewModel.openLeagueSponsorsScreen(true)
                    null -> Unit
                }
            }
        }
    }
}

@Composable
fun DashboardSnackbarHost(
    hostState: SnackbarHostState,
    currentAlert: DashboardSnackbarEvent?,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { snackbarData ->
        DashboardSnackbarCard(
            snackbarData = snackbarData,
            alert = currentAlert
        )
    }
}

@Composable
private fun DashboardSnackbarCard(
    snackbarData: SnackbarData,
    alert: DashboardSnackbarEvent?
) {
    val accentColor = resolveSnackbarAccent(alert)
    val badgeTitle = resolveSnackbarTitle(alert)
    val iconVector = resolveSnackbarIcon(alert)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .testTag("dashboard_snackbar"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.75f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 6.dp, bottom = 8.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = badgeTitle,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = badgeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (snackbarData.visuals.withDismissAction) {
                        IconButton(
                            onClick = { snackbarData.dismiss() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_snackbar_dismiss")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = snackbarData.visuals.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_snackbar_message")
                    )

                    val actionLabel = snackbarData.visuals.actionLabel
                    if (!actionLabel.isNullOrBlank()) {
                        TextButton(
                            onClick = { snackbarData.performAction() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_snackbar_action")
                        ) {
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveSnackbarAccent(alert: DashboardSnackbarEvent?): Color {
    return when (alert?.severity) {
        DashboardAlertSeverity.CRITICAL -> CrimsonAlert
        DashboardAlertSeverity.WARNING -> GoldPrimary
        DashboardAlertSeverity.MILESTONE -> when (alert.type) {
            DashboardAlertType.FINANCIAL_MILESTONE -> EmeraldAccent
            DashboardAlertType.SEASON_MILESTONE -> CyanAccent
            DashboardAlertType.LABOR_DISPUTE -> GoldPrimary
            DashboardAlertType.OFFICIATING_ALERT -> CyanAccent
            DashboardAlertType.DISCIPLINARY_ALERT -> GoldPrimary
            DashboardAlertType.CRISIS_ALERT -> CrimsonAlert
        }
        DashboardAlertSeverity.INFO, null -> GoldPrimary
    }
}

private fun resolveSnackbarTitle(alert: DashboardSnackbarEvent?): String {
    return when (alert?.type) {
        DashboardAlertType.LABOR_DISPUTE -> when (alert.severity) {
            DashboardAlertSeverity.CRITICAL -> "CRITICAL LABOR ALERT"
            else -> "LABOR UNION NOTICE"
        }
        DashboardAlertType.FINANCIAL_MILESTONE -> when (alert.severity) {
            DashboardAlertSeverity.CRITICAL -> "TREASURY LIQUIDITY ALERT"
            else -> "FINANCIAL MILESTONE"
        }
        DashboardAlertType.SEASON_MILESTONE -> "LEAGUE MILESTONE"
        DashboardAlertType.OFFICIATING_ALERT -> when (alert.severity) {
            DashboardAlertSeverity.CRITICAL -> "OFFICIATING CRISIS"
            else -> "OFFICIATING & REPLAY BRIEF"
        }
        DashboardAlertType.DISCIPLINARY_ALERT -> when (alert.severity) {
            DashboardAlertSeverity.CRITICAL -> "DISCIPLINARY ARBITRATION"
            else -> "DISCIPLINARY DOCKET BRIEF"
        }
        DashboardAlertType.CRISIS_ALERT -> when (alert.severity) {
            DashboardAlertSeverity.CRITICAL -> "BRANCHING CRISIS ESCALATION"
            else -> "CRISIS COMMAND BRIEF"
        }
        null -> "COMMISSIONER BRIEF"
    }
}

private fun resolveSnackbarIcon(alert: DashboardSnackbarEvent?): ImageVector {
    return when (alert?.type) {
        DashboardAlertType.LABOR_DISPUTE -> if (alert.severity == DashboardAlertSeverity.CRITICAL) {
            Icons.Default.Warning
        } else {
            Icons.Default.Gavel
        }
        DashboardAlertType.FINANCIAL_MILESTONE -> if (alert.severity == DashboardAlertSeverity.CRITICAL) {
            Icons.Default.Warning
        } else {
            Icons.Default.AccountBalance
        }
        DashboardAlertType.SEASON_MILESTONE -> Icons.Default.EmojiEvents
        DashboardAlertType.OFFICIATING_ALERT -> if (alert.severity == DashboardAlertSeverity.CRITICAL) {
            Icons.Default.Warning
        } else {
            Icons.Default.Gavel
        }
        DashboardAlertType.DISCIPLINARY_ALERT -> if (alert.severity == DashboardAlertSeverity.CRITICAL) {
            Icons.Default.Warning
        } else {
            Icons.Default.Gavel
        }
        DashboardAlertType.CRISIS_ALERT -> Icons.Default.Warning
        null -> Icons.Default.Info
    }
}
