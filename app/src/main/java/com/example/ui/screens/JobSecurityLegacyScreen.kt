package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSecurityLegacyScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.openJobSecurityLegacyScreen(false) }

    val secState = uiState.jobSecurityLegacyState
    val initialTab = if (secState.pendingAnnualReview != null && !secState.pendingAnnualReview.isRatified) 1 else 0
    var selectedTab by remember(secState.pendingAnnualReview?.id) { mutableIntStateOf(initialTab) }
    var selectedPledge by remember(secState.pendingAnnualReview?.id) {
        val lowestPillar = secState.latestBreakdown.allReports.minByOrNull { it.score }?.pillar ?: StakeholderPillar.OWNERS
        mutableStateOf(
            when (lowestPillar) {
                StakeholderPillar.OWNERS -> ReviewResponseChoice.OWNER_GROWTH_CHARTER
                StakeholderPillar.PLAYERS -> ReviewResponseChoice.PLAYER_PARTNERSHIP_ACCORD
                StakeholderPillar.FANS -> ReviewResponseChoice.FAN_ACCESS_BLITZ
                StakeholderPillar.MEDIA -> ReviewResponseChoice.MEDIA_SPECTACLE_SHOWCASE
            }
        )
    }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("job_security_legacy_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Job Security & Legacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${secState.securityTier.label} • ${secState.legacyTier.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openJobSecurityLegacyScreen(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_job_security")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Commissioner Desk"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GoldPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Coalition", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_security_overview")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val badge = if (secState.pendingAnnualReview != null) " (Ready)" else ""
                        Text("Review$badge", fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier.testTag("tab_security_review")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Ledger (${secState.reviewHistory.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_security_ledger")
                )
            }

            bannerMessage?.let { msg ->
                Surface(
                    color = EmeraldAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldLight,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item(key = "meters_header") {
                            DualSecurityAndLegacyCards(secState = secState)
                        }
                        if (secState.latestBreakdown.activeRiskAlerts.isNotEmpty()) {
                            item(key = "risk_alerts") {
                                RiskAlertsCard(alerts = secState.latestBreakdown.activeRiskAlerts)
                            }
                        }
                        item(key = "pillars_heading") {
                            Text(
                                text = "4-PILLAR STAKEHOLDER MANDATE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                        }
                        items(secState.latestBreakdown.allReports, key = { it.pillar.name }) { report ->
                            StakeholderPillarCard(
                                report = report,
                                onNavigateAction = {
                                    viewModel.openJobSecurityLegacyScreen(false)
                                    when (report.pillar) {
                                        StakeholderPillar.OWNERS -> viewModel.setSelectedTab(4)
                                        StakeholderPillar.PLAYERS -> viewModel.openLaborUnionScreen(true)
                                        StakeholderPillar.FANS -> viewModel.openOfficiatingHub(true)
                                        StakeholderPillar.MEDIA -> viewModel.openBroadcastNegotiations(true)
                                    }
                                }
                            )
                        }
                        item(key = "quick_review_cta") {
                            OutlinedButton(
                                onClick = { selectedTab = 1 },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("btn_jump_to_annual_review")
                            ) {
                                Icon(imageVector = Icons.Default.Assignment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Annual Performance Review Boardroom", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    1 -> {
                        item(key = "annual_review_panel") {
                            AnnualReviewEmbeddedPanel(
                                uiState = uiState,
                                selectedPledge = selectedPledge,
                                onSelectPledge = { selectedPledge = it },
                                onConveneNow = {
                                    val rev = viewModel.conveneAnnualPerformanceReviewNow()
                                    bannerMessage = "Board convened Season ${rev.seasonYear} Review: Grade ${rev.overallGrade.letter} (${rev.contractAction.label})."
                                },
                                onRatifyReview = { choice ->
                                    val outcome = viewModel.ratifyPendingAnnualReview(choice)
                                    if (outcome != null) {
                                        bannerMessage = outcome.feedbackMessage
                                    }
                                }
                            )
                        }
                    }

                    2 -> {
                        if (secState.reviewHistory.isEmpty()) {
                            item(key = "empty_ledger") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "No Completed Annual Reviews Yet",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "Complete a season or convene an executive Board review to record graded evaluations and legacy milestones in the permanent ledger.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = { selectedTab = 1 },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("btn_ledger_go_review")
                                        ) {
                                            Text("Go to Review Boardroom", color = Color.Black, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(secState.reviewHistory, key = { it.id }) { rev ->
                                HistoricalReviewCard(review = rev)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobSecurityLegacyStripCard(
    uiState: LeagueUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val secState = uiState.jobSecurityLegacyState
    val breakdown = secState.latestBreakdown
    val secColor = Color(secState.securityTier.colorHex)
    val legColor = Color(secState.legacyTier.colorHex)
    val pendingBadge = secState.pendingAnnualReview != null && !secState.pendingAnnualReview.isRatified

    Card(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("dashboard_job_security_legacy_strip"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, secColor.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Job Security & Legacy Meter",
                        tint = secColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "JOB SECURITY & LEGACY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = secColor
                        )
                        Text(
                            text = "${secState.jobSecurityScore}% (${secState.securityTier.label}) • ${secState.legacyPoints} Legacy Pts",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = (if (pendingBadge) GoldPrimary else legColor).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (pendingBadge) "REVIEW DUE" else secState.legacyTier.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = if (pendingBadge) GoldPrimary else legColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                breakdown.allReports.forEach { rep ->
                    val pColor = Color(rep.pillar.colorHex)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = rep.pillar.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${rep.score}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = pColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualSecurityAndLegacyCards(secState: JobSecurityLegacyState) {
    val secTier = secState.securityTier
    val secColor = Color(secTier.colorHex)
    val legTier = secState.legacyTier
    val legColor = Color(legTier.colorHex)
    val animSec by animateFloatAsState(
        targetValue = (secState.jobSecurityScore / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 280f),
        label = "jobSecurityMeter"
    )
    val animLegacy by animateFloatAsState(
        targetValue = secState.legacyProgressToNextTier,
        animationSpec = spring(stiffness = 280f),
        label = "legacyMeter"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, secColor.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("job_security_meter_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "JOB SECURITY METER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = secColor
                        )
                        Text(
                            text = "${secState.jobSecurityScore}% • ${secTier.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Surface(
                        color = secColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${secState.contractYearsRemaining}Y TERM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = secColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { animSec },
                    color = secColor,
                    trackColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                Text(
                    text = secTier.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = secState.latestBreakdown.backgroundPerkNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, legColor.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("legacy_meter_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMISSIONER LEGACY METER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = legColor
                        )
                        Text(
                            text = "${secState.legacyPoints} Pts • ${legTier.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "${secState.seasonsServed} Seasons • ${secState.extensionsEarned} Ext.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { animLegacy },
                    color = legColor,
                    trackColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                val nextTargetLabel = legTier.nextTierPoints?.let { "Next tier at $it Legacy Points" }
                    ?: "Peak Executive Hall of Fame Tier Unlocked"
                Text(
                    text = "${legTier.crestSubtitle} • $nextTargetLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RiskAlertsCard(alerts: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "BOARD PRESSURE ALERTS (${alerts.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = CrimsonAlert
            )
            alerts.forEach { alert ->
                Text(
                    text = "• $alert",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StakeholderPillarCard(
    report: StakeholderPillarReport,
    onNavigateAction: () -> Unit
) {
    val accent = Color(report.pillar.colorHex)
    val icon: ImageVector = when (report.pillar) {
        StakeholderPillar.OWNERS -> Icons.Default.AccountBalance
        StakeholderPillar.PLAYERS -> Icons.Default.Handshake
        StakeholderPillar.FANS -> Icons.Default.Groups
        StakeholderPillar.MEDIA -> Icons.Default.LiveTv
    }
    val actionTitle = when (report.pillar) {
        StakeholderPillar.OWNERS -> "Board & Governance"
        StakeholderPillar.PLAYERS -> "Players Union & CBA"
        StakeholderPillar.FANS -> "Officiating & Integrity"
        StakeholderPillar.MEDIA -> "Broadcast Rights"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_pillar_${report.pillar.name}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = report.pillar.displayTitle,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = report.pillar.displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = report.pillar.roleSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "${report.score}% (${report.statusLabel})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
            }

            LinearProgressIndicator(
                progress = { (report.score / 100f).coerceIn(0f, 1f) },
                color = accent,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            report.positiveDrivers.forEach { pos ->
                Text(
                    text = "+ $pos",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldLight
                )
            }
            report.riskDrivers.forEach { risk ->
                Text(
                    text = "! $risk",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrimsonAlert
                )
            }

            OutlinedButton(
                onClick = onNavigateAction,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_pillar_action_${report.pillar.name}")
            ) {
                Text("Manage $actionTitle", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnnualReviewEmbeddedPanel(
    uiState: LeagueUiState,
    selectedPledge: ReviewResponseChoice,
    onSelectPledge: (ReviewResponseChoice) -> Unit,
    onConveneNow: () -> Unit,
    onRatifyReview: (ReviewResponseChoice) -> Unit,
    isReviewMode: Boolean = false,
    onProceedNext: (() -> Unit)? = null
) {
    val secState = uiState.jobSecurityLegacyState
    val pending = secState.pendingAnnualReview
    val latestArchived = secState.reviewHistory.firstOrNull()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("annual_review_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ANNUAL PERFORMANCE REVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                    Text(
                        text = pending?.let { "Season ${it.seasonYear} Formal Board Evaluation" }
                            ?: latestArchived?.let { "Season ${it.seasonYear} Review Ratified (${it.overallGrade.letter})" }
                            ?: "Mid-Season & Year-End Executive Docket",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                if (pending != null) {
                    Surface(
                        color = Color(pending.overallGrade.colorHex).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = pending.overallGrade.letter,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(pending.overallGrade.colorHex),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (pending == null && !isReviewMode) {
                Text(
                    text = "Convene the Owners, Players Association, Fans Audit, and Broadcast Consortium for an immediate Season ${uiState.currentYear} Performance Review, or finish the Championship Finals to trigger it automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onConveneNow,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_convene_annual_review")
                ) {
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Convene Season ${uiState.currentYear} Performance Review",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
                if (latestArchived != null) {
                    HistoricalReviewCard(review = latestArchived)
                }
            } else if (pending != null) {
                val actionColor = Color(pending.contractAction.colorHex)
                Surface(
                    color = actionColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, actionColor.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = pending.contractAction.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = actionColor
                        )
                        Text(
                            text = pending.contractAction.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Composite: ${pending.compositeApprovalScore}% • Security ${pending.jobSecurityBefore}% ➔ ${pending.jobSecurityAfter}% • +${pending.legacyPointsAwarded} Base Legacy Pts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }

                pending.verdicts.forEach { verdict ->
                    val vColor = Color(verdict.pillar.colorHex)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, vColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${verdict.pillar.shortLabel} • ${verdict.representativeName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = vColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${verdict.score}% (${verdict.gradeLetter})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = vColor
                                )
                            }
                            Text(
                                text = "\"${verdict.quote}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = pending.strategicMandateNextYear,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )

                if (!isReviewMode) {
                    Text(
                        text = "SELECT EXECUTIVE CHARTER PLEDGE TO RATIFY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReviewResponseChoice.entries.forEach { choice ->
                            val isChosen = selectedPledge == choice
                            val cColor = Color(choice.pillar.colorHex)
                            Surface(
                                color = if (isChosen) cColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isChosen) 1.5.dp else 1.dp,
                                    color = if (isChosen) cColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable { onSelectPledge(choice) }
                                    .testTag("choice_pledge_${choice.name}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${choice.badgeTag}: ${choice.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (isChosen) cColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = choice.impactSummary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onRatifyReview(selectedPledge) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_ratify_annual_review")
                    ) {
                        Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ratify Review & Sign ${selectedPledge.badgeTag}",
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (onProceedNext != null) {
                OutlinedButton(
                    onClick = onProceedNext,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_review_proceed_summary")
                ) {
                    Text("Proceed to Parity & Launch Summary", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HistoricalReviewCard(review: AnnualPerformanceReview) {
    val gradeColor = Color(review.overallGrade.colorHex)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_review_history_${review.seasonYear}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Season ${review.seasonYear} • ${review.contractAction.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Champion: ${review.championTeamName} • Val \$${review.leagueValuationSnapshotMillions}M",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = gradeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${review.overallGrade.letter} (${review.compositeApprovalScore}%)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = gradeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "Owners ${review.ownersScore}% • Players ${review.playersScore}% • Fans ${review.fansScore}% • Media ${review.mediaScore}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Job Security ${review.jobSecurityBefore}% ➔ ${review.jobSecurityAfter}% • +${review.legacyPointsAwarded} Legacy Pts${review.commissionerResponseChosen?.let { " • ${it.badgeTag}" } ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        }
    }
}
