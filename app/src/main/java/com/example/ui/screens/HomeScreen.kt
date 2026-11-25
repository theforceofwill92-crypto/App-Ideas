package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AllStarEngine
import com.example.model.BroadcastBidStatus
import com.example.model.InjuryEngine
import com.example.model.LaborTriggerSeverity
import com.example.model.SeasonPhase
import com.example.model.StadiumDealEngine
import com.example.ui.components.LeagueNewsComponent
import com.example.ui.components.SectionHeader
import com.example.ui.components.StadiumUltimatumCard
import com.example.ui.components.TeamBadge
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

private data class DeskHubActionSpec(
    val key: String,
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val subtitle: String,
    val buttonLabel: String,
    val buttonTestTag: String,
    val rowTestTag: String,
    val onClick: () -> Unit,
    val secondaryButtonLabel: String? = null,
    val secondaryButtonTestTag: String? = null,
    val onSecondaryClick: (() -> Unit)? = null
)

@Composable
fun HomeScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val simulationState by viewModel.coreSimulationViewModel.simulationState.collectAsStateWithLifecycle()
    val persistedLeagueState by viewModel.persistedLeagueState.collectAsStateWithLifecycle()
    val fiveYearRevenueHistory by viewModel.fiveYearRevenueHistory.collectAsStateWithLifecycle()

    // Prepare grouped Hub metrics outside LazyListScope
    val discState = uiState.disciplinaryState
    val pendingCasesCount = discState.pendingCases.size
    val appealsCount = discState.appealedCases.size
    val suspendedCount = uiState.franchises.sumOf { it.suspendedPlayersCount }
    val discAccent = when {
        appealsCount > 0 -> CrimsonAlert
        pendingCasesCount > 0 -> AmberWarning
        else -> CyanAccent
    }

    val offState = uiState.officiatingState
    val activeControversies = offState.activeUnresolvedIncidents.size
    val offAccent = when {
        activeControversies > 0 || offState.unionState.lockoutRisk >= 65 -> CrimsonAlert
        offState.installedTechCount >= 2 -> EmeraldAccent
        else -> CyanAccent
    }

    val pendingBranchesCount = uiState.crisisState.scheduledBranches.size
    val resolvedCrisesCount = uiState.crisisState.resolvedHistory.size
    val isHighRisk = uiState.unionAgreement.strikeRisk >= 35
    val unionAccent = if (isHighRisk) CrimsonAlert else GoldPrimary
    val integrityHeaderBadge = when {
        appealsCount + activeControversies > 0 -> "${appealsCount + activeControversies} Urgent"
        pendingBranchesCount > 0 -> "$pendingBranchesCount Branches"
        pendingCasesCount > 0 -> "$pendingCasesCount Hearings"
        else -> "Steady"
    }

    val tradeRules = uiState.ruleBook.tradeRules
    val isDeadlineWeek = tradeRules.isTradeDeadlineWeek(uiState.currentWeek, uiState.seasonPhase)
    val pendingTrades = uiState.pendingTrades.size
    val totalBlockListings = uiState.franchises.sumOf { it.tradeBlockListings.size }
    val totalInjured = remember(uiState.franchises) {
        uiState.franchises.sumOf { t -> t.roster.count { it.isInjured } }
    }
    val totalHoles = remember(uiState.franchises) {
        uiState.franchises.sumOf { InjuryEngine.countUnresolvedStarterHoles(it) }
    }
    val depthAccent = when {
        totalHoles > 0 -> CrimsonAlert
        totalInjured > 0 -> AmberWarning
        else -> EmeraldAccent
    }
    val poolCount = uiState.freeAgentPool.size
    val topAgent = uiState.freeAgentPool.maxByOrNull { it.overallRating }
    val minorState = uiState.minorLeagueState
    val topAff = minorState.affiliates.maxByOrNull { it.winPct }

    val topBid = uiState.broadcastBiddingSession.offers
        .filter { it.bidStatus != BroadcastBidStatus.WITHDRAWN }
        .maxByOrNull { it.impact.cumulativeGrossValueMillions }
    val pendingReqCount = uiState.pendingExpansionRequests.size
    val totalReqReach = Math.round(
        uiState.pendingExpansionRequests.sumOf { it.impact.projectedMarketReachGainMillions } * 10.0
    ) / 10.0
    val totalReqRev = uiState.pendingExpansionRequests.sumOf { it.impact.projectedAnnualRevenueGainMillions }
    val ongoingBuilds = uiState.franchises.sumOf { it.facilities.ongoingProjects.size }
    val totalCapacity = uiState.franchises.sumOf { it.stadiumCapacity }
    val agingStadiumCount = remember(uiState.franchises) {
        uiState.franchises.count { StadiumDealEngine.qualifiesForUltimatum(it) }
    }

    val asState = uiState.allStarRedesignState
    val asImpact = remember(
        asState,
        uiState.franchises,
        uiState.activeBroadcast,
        uiState.cities,
        uiState.ruleBook
    ) {
        AllStarEngine.computeProjectedImpact(
            redesignState = asState,
            franchises = uiState.franchises,
            activeBroadcast = uiState.activeBroadcast,
            cities = uiState.cities,
            ruleBook = uiState.ruleBook
        )
    }
    val isAllStarNow = uiState.seasonPhase == SeasonPhase.ALL_STAR_INTERNATIONAL
    val allStarAccent = when {
        isAllStarNow -> GoldPrimary
        asImpact.playerEffortScore < 60 -> CrimsonAlert
        else -> VioletAccent
    }

    val latestPotw = uiState.weeklyAwards.firstOrNull()
    val activeRivalries = uiState.rivalries.filter { it.isActiveRivalry }
    val topRivalry = activeRivalries.maxByOrNull { it.intensity }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Compact Stadium Hero Header
        item(key = "hero_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_league_banner),
                    contentDescription = "League Stadium Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    NavyDarkBackground.copy(alpha = 0.85f),
                                    NavyDarkBackground
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(color = GoldPrimary, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = uiState.sportType.displayName.uppercase(),
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "YEAR ${uiState.currentYear} • ${uiState.seasonPhase.title.uppercase()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.leagueName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Commissioner Office • ${uiState.commissionerBackground.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Primary Simulation Control Card
        item(key = "advance_week_bar") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("advance_week_card")
                    .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (uiState.seasonPhase) {
                                SeasonPhase.PRE_SEASON -> "PRE-SEASON RECRUITMENT"
                                SeasonPhase.REGULAR_SEASON -> "WEEK ${uiState.currentWeek} OF ${uiState.totalWeeks}"
                                SeasonPhase.ALL_STAR_INTERNATIONAL -> "GLOBAL ALL-STAR BREAK"
                                SeasonPhase.PLAYOFFS -> "CHAMPIONSHIP PLAYOFFS"
                                SeasonPhase.OFF_SEASON -> "ANNUAL OFF-SEASON SUMMIT"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when (uiState.seasonPhase) {
                                SeasonPhase.PRE_SEASON -> "Launch Regular Season Week 1"
                                SeasonPhase.REGULAR_SEASON -> "Simulate Week & Broadcast"
                                SeasonPhase.PLAYOFFS -> "Simulate Playoff Round"
                                SeasonPhase.ALL_STAR_INTERNATIONAL -> "Host Showcase Matches"
                                SeasonPhase.OFF_SEASON -> "Enter Off-Season (${uiState.currentYear} ➔ ${uiState.currentYear + 1})"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) {
                                viewModel.openOffSeasonSummit(true)
                            } else {
                                viewModel.advanceWeek()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("advance_week_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) Icons.Default.Analytics else Icons.Default.PlayArrow,
                            contentDescription = "Advance",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) "Off-Season" else "Simulate",
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Executive Telemetry Strip
        item(key = "executive_kpis") {
            MainDashboardMetricsPanel(
                uiState = uiState,
                simulationState = simulationState,
                persistedLeagueState = persistedLeagueState,
                fiveYearRevenueHistory = fiveYearRevenueHistory,
                onSeasonWeekClick = { viewModel.setSelectedTab(5) },
                onBankBalanceClick = { viewModel.setSelectedTab(3) },
                onFanSentimentClick = { viewModel.openFanSentimentScreen(true) },
                onStrikeRiskClick = { viewModel.openLaborUnionScreen(true) },
                onIntegrityClick = { viewModel.openOfficiatingHub(true) },
                onJobSecurityClick = { viewModel.openJobSecurityLegacyScreen(true) },
                onFranchisesClick = { viewModel.setSelectedTab(1) },
                onOpenBroadcast = { viewModel.openBroadcastNegotiations(true) },
                onOpenExpansion = { viewModel.openExpansionRequestsManagement(true) },
                onOpenFacilities = { viewModel.openFacilityHub() },
                onOpenSchedule = { viewModel.openScheduleCalendarScreen(true) },
                onOpenOffSeason = { viewModel.openOffSeasonSummit(true) },
                onOpenCrisisHub = { viewModel.openCrisisEventsScreen(true) },
                onOpenRivalWatch = { viewModel.openRivalWatchModal(true) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Priority Urgent Alerts (shown only when a crisis, stadium ultimatum, or CBA trigger requires immediate ruling)
        uiState.stadiumDealState.activeUltimatum?.let { ultimatum ->
            item(key = "active_stadium_ultimatum") {
                StadiumUltimatumCard(
                    ultimatum = ultimatum,
                    leagueCashMillions = uiState.cashReservesMillions,
                    onSelectChoice = { choice -> viewModel.resolveStadiumUltimatumChoice(choice) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (uiState.unionAgreement.isLockoutActive) {
            item(key = "home_lockout_banner") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("home_lockout_banner")
                        .border(1.5.dp, CrimsonAlert, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OWNER LOCKOUT IN EFFECT (${uiState.unionAgreement.lockoutWeeksLost}w lost)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CrimsonAlert
                            )
                            Text(
                                text = "Regular season games & broadcast revenues are frozen until a CBA accord is signed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.openLaborNegotiationOverlay() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_home_resolve_lockout")
                        ) {
                            Text(
                                text = "Emergency Accord",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (uiState.activeDilemma != null) {
            item(key = "active_dilemma_alert") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("active_dilemma_alert")
                        .border(1.5.dp, CrimsonAlert, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Crisis",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val d = uiState.activeDilemma
                            Text(
                                text = "CRISIS STAGE ${d.stageNumber}/${d.totalStages} • ${d.archetype.displayTitle.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CrimsonAlert
                            )
                            Text(
                                text = d.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (uiState.activeLaborTrigger != null) {
            item(key = "labor_trigger_alert") {
                val trigger = uiState.activeLaborTrigger
                val accent = when (trigger.severity) {
                    LaborTriggerSeverity.CRITICAL -> CrimsonAlert
                    LaborTriggerSeverity.WARNING -> GoldPrimary
                    LaborTriggerSeverity.INFO -> CyanAccent
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("labor_union_trigger_alert")
                        .border(1.5.dp, accent, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = "Labor Union Event Trigger",
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LABOR TRIGGER • ${trigger.severity.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = accent
                                )
                                Text(
                                    text = trigger.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissLaborTrigger() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_dismiss_labor_trigger")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Labor Trigger",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = trigger.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Strike Risk: ${trigger.strikeRiskSnapshot}% • CBA: ${trigger.cbaYearsRemainingSnapshot} yr(s)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                            Button(
                                onClick = { viewModel.openLaborUnionFromTrigger() },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_trigger_open_union")
                            ) {
                                Text(
                                    text = "Negotiate CBA",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // 5. GROUPED HUB 1: Integrity, Labor & Officiating
        deskGroupedHub(
            groupKey = "integrity_governance",
            title = "INTEGRITY, LABOR & OFFICIATING",
            subtitle = "Tribunals, referees & Players Association",
            accent = if (appealsCount > 0 || activeControversies > 0 || isHighRisk) CrimsonAlert else GoldPrimary,
            statusBadge = integrityHeaderBadge,
            headerTestTag = "group_integrity_governance",
            rows = listOf(
                DeskHubActionSpec(
                    key = "disc_desk",
                    icon = Icons.Default.Balance,
                    accent = discAccent,
                    title = "Disciplinary Desk & Precedent",
                    subtitle = "$pendingCasesCount Pending • $appealsCount Appeal • $suspendedCount Susp • ${discState.precedentConsistencyScore}% Cons.",
                    buttonLabel = if (pendingCasesCount > 0 || appealsCount > 0) "Rule" else "Desk",
                    buttonTestTag = "btn_open_disciplinary_desk",
                    rowTestTag = "banner_disciplinary_desk",
                    onClick = { viewModel.openDisciplinaryDesk(true) }
                ),
                DeskHubActionSpec(
                    key = "officiating",
                    icon = Icons.Default.Gavel,
                    accent = offAccent,
                    title = if (activeControversies > 0) "Officiating ($activeControversies Blown Call)" else "Officiating & Replay Command",
                    subtitle = "${offState.leagueCallAccuracyPct}% Accuracy • Union Risk ${offState.unionState.lockoutRisk}% • ${offState.installedTechCount} Tech",
                    buttonLabel = if (activeControversies > 0) "Respond" else "Oversee",
                    buttonTestTag = "btn_open_officiating_hub",
                    rowTestTag = "banner_officiating_hub",
                    onClick = { viewModel.openOfficiatingHub(true) }
                ),
                DeskHubActionSpec(
                    key = "labor_cba",
                    icon = Icons.Default.Handshake,
                    accent = if (uiState.unionAgreement.isLockoutActive) CrimsonAlert else unionAccent,
                    title = when {
                        uiState.unionAgreement.isLockoutActive -> "LOCKOUT ACTIVE • Play Suspended"
                        uiState.unionAgreement.cbaYearsRemaining <= 1 -> "CBA Expiring (${uiState.unionAgreement.cbaYearsRemaining}y) • Labor Talks"
                        isHighRisk -> "CBA Labor Dispute Warning"
                        else -> "Players Union & CBA Accord"
                    },
                    subtitle = if (uiState.unionAgreement.isLockoutActive) {
                        "LOCKOUT ACTIVE (${uiState.unionAgreement.lockoutWeeksLost}w lost) • CBA: ${uiState.unionAgreement.cbaYearsRemaining}y left"
                    } else {
                        "CBA: ${uiState.unionAgreement.cbaYearsRemaining}y left • Strike ${uiState.unionAgreement.strikeRisk}% • ${uiState.unionAgreement.playerRevenueSharePct}% Split"
                    },
                    buttonLabel = if (uiState.unionAgreement.isLockoutActive) "Accord" else if (isHighRisk) "Avert" else "CBA",
                    buttonTestTag = "btn_home_labor_union",
                    rowTestTag = "labor_union_banner",
                    onClick = { viewModel.openLaborUnionScreen(true) }
                ),
                DeskHubActionSpec(
                    key = "crisis_command",
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    accent = if (pendingBranchesCount > 0) CrimsonAlert else AmberWarning,
                    title = if (pendingBranchesCount > 0) "Crisis Storylines ($pendingBranchesCount Brewing)" else "Crisis Storylines & Radar",
                    subtitle = "$pendingBranchesCount Scheduled Branches • $resolvedCrisesCount Resolved • 8 Arcs",
                    buttonLabel = if (pendingBranchesCount > 0) "Track" else "Briefs",
                    buttonTestTag = "btn_open_crisis_hub",
                    rowTestTag = "banner_crisis_hub",
                    onClick = { viewModel.openCrisisEventsScreen(true) }
                ),
                DeskHubActionSpec(
                    key = "job_security_legacy",
                    icon = Icons.Default.VerifiedUser,
                    accent = Color(uiState.jobSecurityLegacyState.securityTier.colorHex),
                    title = "Job Security (${uiState.jobSecurityLegacyState.jobSecurityScore}%) & Legacy",
                    subtitle = "${uiState.jobSecurityLegacyState.securityTier.label} • ${uiState.jobSecurityLegacyState.legacyPoints} Pts (${uiState.jobSecurityLegacyState.legacyTier.title})",
                    buttonLabel = if (uiState.jobSecurityLegacyState.pendingAnnualReview != null) "Review" else "Meter",
                    buttonTestTag = "btn_open_job_security",
                    rowTestTag = "banner_job_security",
                    onClick = { viewModel.openJobSecurityLegacyScreen(true) }
                )
            )
        )

        // 6. GROUPED HUB 2: Rosters, Trades & Development
        deskGroupedHub(
            groupKey = "personnel_rosters",
            title = "ROSTERS, TRADES & DEVELOPMENT",
            subtitle = "Transactions, lineups, free agents & affiliates",
            accent = if (isDeadlineWeek || totalHoles > 0) CrimsonAlert else AmberWarning,
            statusBadge = if (pendingTrades > 0) "$pendingTrades Offers" else "${uiState.franchises.size} Clubs",
            headerTestTag = "group_personnel_rosters",
            rows = listOf(
                DeskHubActionSpec(
                    key = "trade_desk",
                    icon = Icons.Default.SwapHoriz,
                    accent = if (isDeadlineWeek) CrimsonAlert else AmberWarning,
                    title = if (isDeadlineWeek) "Trade Deadline ($pendingTrades Pending)" else "Trade Desk ($pendingTrades Pending)",
                    subtitle = "$totalBlockListings on Trade Block • Deadline W${tradeRules.tradeDeadlineWeek}",
                    buttonLabel = "Desk",
                    buttonTestTag = "btn_open_trade_desk",
                    rowTestTag = "banner_trade_desk",
                    secondaryButtonLabel = "Block",
                    secondaryButtonTestTag = "btn_open_trade_block",
                    onSecondaryClick = { viewModel.openTradeBlock() },
                    onClick = { viewModel.openTradeDesk(true) }
                ),
                DeskHubActionSpec(
                    key = "depth_charts",
                    icon = Icons.Default.LocalHospital,
                    accent = depthAccent,
                    title = "Depth Charts & Injury Watch",
                    subtitle = if (totalHoles > 0) "$totalHoles starter hole(s) • $totalInjured injured" else "$totalInjured active injuries • Starters set",
                    buttonLabel = "Lineups",
                    buttonTestTag = "btn_open_depth_charts",
                    rowTestTag = "banner_depth_charts",
                    onClick = { viewModel.openDepthChart() }
                ),
                DeskHubActionSpec(
                    key = "free_agency",
                    icon = Icons.Default.PersonAdd,
                    accent = VioletAccent,
                    title = "Free Agency Pool ($poolCount Avail.)",
                    subtitle = topAgent?.let { "Top FA: ${it.name} (${it.overallRating} OVR • \$${it.askingSalaryMillions}M)" }
                        ?: "Cap-compliant depth signings",
                    buttonLabel = "Pool",
                    buttonTestTag = "btn_open_free_agency_pool",
                    rowTestTag = "banner_free_agency_pool",
                    onClick = { viewModel.openFreeAgencyPool() }
                ),
                DeskHubActionSpec(
                    key = "minor_league",
                    icon = Icons.Default.Hub,
                    accent = EmeraldAccent,
                    title = "${minorState.leagueName} (${minorState.affiliates.size} Teams)",
                    subtitle = topAff?.let { "Leader: ${it.fullName} (${it.wins}-${it.losses}) • +${minorState.totalOvrBreakthroughs} OVR" }
                        ?: "Prospect development & playoffs",
                    buttonLabel = "Circuit",
                    buttonTestTag = "btn_open_minor_league",
                    rowTestTag = "banner_minor_league",
                    onClick = { viewModel.openMinorLeagueHub() }
                )
            )
        )

        // 7. GROUPED HUB 3: Business, Media & League Growth
        deskGroupedHub(
            groupKey = "business_expansion",
            title = "BUSINESS, MEDIA & LEAGUE GROWTH",
            subtitle = "TV contracts, expansion, venues & marquee events",
            accent = CyanAccent,
            statusBadge = "\$${uiState.cashReservesMillions}M Cash",
            headerTestTag = "group_business_expansion",
            rows = listOf(
                DeskHubActionSpec(
                    key = "broadcast",
                    icon = Icons.Default.LiveTv,
                    accent = if (uiState.broadcastBiddingSession.exclusivityExpired) AmberWarning else CyanAccent,
                    title = "Broadcast Rights • ${uiState.activeBroadcast.partnerName}",
                    subtitle = run {
                        val exclText = if (uiState.broadcastBiddingSession.exclusivityExpired) {
                            "Non-Exclusive (-22%)"
                        } else {
                            "Exclusivity: ${uiState.broadcastBiddingSession.exclusivityRoundsRemaining} left"
                        }
                        if (topBid != null) {
                            "\$${uiState.activeBroadcast.annualPayoutMillions}M/yr • Top: \$${topBid.impact.cumulativeGrossValueMillions}M • $exclText"
                        } else {
                            "\$${uiState.activeBroadcast.annualPayoutMillions}M/yr • ${uiState.activeBroadcast.yearsRemaining}y left • $exclText"
                        }
                    },
                    buttonLabel = "Bids",
                    buttonTestTag = "btn_home_negotiate_war_room",
                    rowTestTag = "broadcast_partner_banner",
                    onClick = { viewModel.openBroadcastNegotiations(true) }
                ),
                DeskHubActionSpec(
                    key = "expansion",
                    icon = Icons.Default.AddBusiness,
                    accent = CyanAccent,
                    title = "Expansion Requests ($pendingReqCount Pending)",
                    subtitle = if (pendingReqCount > 0) {
                        "+${totalReqReach}M Reach • +\$${totalReqRev}M/yr Revenue"
                    } else {
                        "All bids reviewed • Scout new markets"
                    },
                    buttonLabel = "Review",
                    buttonTestTag = "btn_home_expansion_requests",
                    rowTestTag = "banner_expansion_requests",
                    onClick = { viewModel.openExpansionRequestsManagement(true) }
                ),
                DeskHubActionSpec(
                    key = "facilities",
                    icon = Icons.Default.Stadium,
                    accent = if (agingStadiumCount > 0) CrimsonAlert else EmeraldAccent,
                    title = when {
                        agingStadiumCount > 0 -> "Stadiums & Facilities ($agingStadiumCount Aging)"
                        ongoingBuilds > 0 -> "Stadiums & Facilities ($ongoingBuilds Builds)"
                        else -> "Stadiums & Facilities Hub"
                    },
                    subtitle = "%,d League Seats • %d At-Risk Arenas".format(totalCapacity, agingStadiumCount),
                    buttonLabel = "Build",
                    buttonTestTag = "btn_open_facility_hub",
                    rowTestTag = "banner_facilities",
                    onClick = { viewModel.openFacilityHub() }
                ),
                DeskHubActionSpec(
                    key = "all_star",
                    icon = Icons.Default.Stars,
                    accent = allStarAccent,
                    title = if (isAllStarNow) "All-Star Live • ${asState.hostCityName}" else "All-Star Festivities Lab",
                    subtitle = "${asState.selectedFormat.displayName} • ${asImpact.projectedViewersMillions}M TV (${asImpact.tvRatingsGrade})",
                    buttonLabel = if (isAllStarNow) "Host" else "Redesign",
                    buttonTestTag = "btn_open_all_star_hub",
                    rowTestTag = "banner_all_star_redesign",
                    onClick = { viewModel.openAllStarRedesignScreen(true) }
                ),
                DeskHubActionSpec(
                    key = "rival_watch",
                    icon = Icons.Default.Visibility,
                    accent = Color(uiState.rivalLeague.threatColorHex),
                    title = when {
                        uiState.rivalLeague.activeAbsorptionNegotiation != null ->
                            "Merger Summit • ${uiState.rivalLeague.name}"
                        uiState.rivalLeague.isActive ->
                            "Rival Watch • ${uiState.rivalLeague.name}"
                        uiState.rivalLeague.absorbedTeamNames.isNotEmpty() ->
                            "Rival Absorbed • ${uiState.rivalLeague.absorbedTeamNames.size} Clubs Joined"
                        else ->
                            "Rival Watch • ${uiState.rivalLeague.name} (Folded)"
                    },
                    subtitle = when {
                        uiState.rivalLeague.activeAbsorptionNegotiation != null ->
                            "Survived ${uiState.rivalLeague.seasonsSurvived}Y @ ${uiState.rivalLeague.hype} Hype • Absorb 2–4 Teams for Massive TV Boost!"
                        uiState.rivalLeague.isActive ->
                            "\$${uiState.rivalLeague.capital}M Cap • ${uiState.rivalLeague.hype} Hype • ${uiState.rivalLeague.seasonsSurvived}Y Survived • ${uiState.rivalLeague.poachedPlayers.size} Poached"
                        uiState.rivalLeague.permanentTvRevenueBoostMillions > 0L ->
                            "Absorbed • +\$${uiState.rivalLeague.permanentTvRevenueBoostMillions}M/yr Permanent TV Revenue Escalator Active"
                        else ->
                            "Bankrupt / Absorbed • All poached stars back in Free Agency"
                    },
                    buttonLabel = if (uiState.rivalLeague.activeAbsorptionNegotiation != null) "Summit" else "Watch",
                    buttonTestTag = "btn_open_rival_watch",
                    rowTestTag = "banner_rival_watch",
                    onClick = { viewModel.openRivalWatchModal(true) }
                ),
                DeskHubActionSpec(
                    key = "fan_sentiment_pulse",
                    icon = Icons.Default.Campaign,
                    accent = if (uiState.fanSentimentState.divergenceGap >= 15) AmberWarning else CyanAccent,
                    title = "Fan Sentiment & Media Pulse (${uiState.fanSentimentState.polarizationLabel})",
                    subtitle = "Casual ${uiState.fanSentimentState.casualSentimentScore}% vs Die-Hard ${uiState.fanSentimentState.dieHardSentimentScore}% • Social, Radio & Print",
                    buttonLabel = "Pulse",
                    buttonTestTag = "btn_open_fan_sentiment",
                    rowTestTag = "banner_fan_sentiment",
                    onClick = { viewModel.openFanSentimentScreen(true) }
                )
            )
        )

        // 8. GROUPED HUB 4: Competition, History & Season Pulse
        deskGroupedHub(
            groupKey = "competition_lore",
            title = "COMPETITION, HISTORY & OFF-SEASON",
            subtitle = "Awards, Hall of Fame, rivalries & annual summit",
            accent = GoldPrimary,
            statusBadge = "${uiState.recordBook.allRecordsCount} Records",
            headerTestTag = "group_competition_lore",
            rows = listOf(
                DeskHubActionSpec(
                    key = "records",
                    icon = Icons.Default.EmojiEvents,
                    accent = GoldPrimary,
                    title = "Record Book & Hall of Fame",
                    subtitle = latestPotw?.let { "POTW W${it.week}: ${it.playerName} (${it.franchiseName})" }
                        ?: "${uiState.recordBook.allRecordsCount} Records • ${uiState.hallOfFame.size} Immortals",
                    buttonLabel = "Records",
                    buttonTestTag = "btn_open_record_book",
                    rowTestTag = "banner_record_book",
                    onClick = { viewModel.openRecordBook(true) }
                ),
                DeskHubActionSpec(
                    key = "rivalries",
                    icon = Icons.Default.Whatshot,
                    accent = CrimsonAlert,
                    title = "League Rivalries (${activeRivalries.size} Active)",
                    subtitle = topRivalry?.let { "${it.rivalryName} • ${it.intensity}/100 Heat • ${it.ticketRevenueMultiplier}x Gate" }
                        ?: "Playoff, Geographic & Divisional Feuds",
                    buttonLabel = "Rivalries",
                    buttonTestTag = "btn_open_rivalry_hub",
                    rowTestTag = "banner_rivalries",
                    onClick = { viewModel.openRivalryHub() }
                ),
                DeskHubActionSpec(
                    key = "schedule_calendar",
                    icon = Icons.Default.CalendarMonth,
                    accent = CyanAccent,
                    title = "Schedule & Calendar (${uiState.ruleBook.regularSeasonWeeks}W)",
                    subtitle = "${uiState.seasonScheduleState.activeSeasonSchedule?.holidayShowcaseCount ?: 0} Holiday Marquees • ${uiState.seasonScheduleState.activeSeasonSchedule?.internationalGameCount ?: 0} Global Games",
                    buttonLabel = "Calendar",
                    buttonTestTag = "btn_open_schedule_calendar",
                    rowTestTag = "banner_schedule_calendar",
                    onClick = { viewModel.openScheduleCalendarScreen(true) }
                ),
                DeskHubActionSpec(
                    key = "offseason",
                    icon = Icons.Default.Analytics,
                    accent = GoldPrimary,
                    title = if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) "Live Off-Season Summit" else "Off-Season & Draft Dossier",
                    subtitle = "Awards • Retirements • Lottery • FA • Draft • Schedule",
                    buttonLabel = if (uiState.seasonPhase == SeasonPhase.OFF_SEASON) "Enter" else "Review",
                    buttonTestTag = "btn_review_offseason",
                    rowTestTag = "banner_offseason_review",
                    onClick = { viewModel.openOffSeasonSummit(true) }
                )
            )
        )

        // 9. Match Results Section
        if (uiState.currentWeekMatches.isNotEmpty()) {
            item(key = "matches_header") {
                SectionHeader(
                    title = "Week ${if (uiState.seasonPhase == SeasonPhase.REGULAR_SEASON) (uiState.currentWeek - 1).coerceAtLeast(1) else uiState.currentWeek} Match Results",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(uiState.currentWeekMatches, key = { it.id }) { match ->
                val home = uiState.franchises.find { it.id == match.homeTeamId }
                val away = uiState.franchises.find { it.id == match.awayTeamId }
                if (home != null && away != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("match_card_${match.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TeamBadge(
                                    cityName = home.cityName,
                                    nickname = home.nickname,
                                    colorHex = home.colorHex,
                                    secondaryColorHex = home.secondaryColorHex,
                                    subtitle = "${home.wins}-${home.losses}",
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (match.isPlayed) "${match.homeScore} - ${match.awayScore}" else "VS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (match.isPlayed) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                TeamBadge(
                                    cityName = away.cityName,
                                    nickname = away.nickname,
                                    colorHex = away.colorHex,
                                    secondaryColorHex = away.secondaryColorHex,
                                    subtitle = "${away.wins}-${away.losses}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (match.isHolidayMarquee && match.holidayTentpoleLabel != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("match_holiday_badge_${match.id}")
                                ) {
                                    Text(
                                        text = "${match.holidayTentpoleLabel} • Holiday Marquee",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (match.isInternationalShowcase && match.internationalVenueLabel != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("match_intl_badge_${match.id}")
                                ) {
                                    Text(
                                        text = "International Series: ${match.internationalVenueLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (match.rivalryTitle != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = CrimsonAlert.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f)),
                                    modifier = Modifier.testTag("match_rivalry_badge_${match.id}")
                                ) {
                                    Text(
                                        text = "${match.rivalryTitle} • ${match.ticketRevenueMultiplier}x Gate • ${match.moraleMultiplier}x Morale",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonAlert,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (match.ruleImpactNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = CyanAccent.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                                    modifier = Modifier.testTag("match_rule_impact_badge_${match.id}")
                                ) {
                                    Text(
                                        text = match.ruleImpactNote,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (match.highlightStory.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = match.highlightStory,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 10. League News Intelligence Wire Component
        item(key = "league_news_wire") {
            Spacer(modifier = Modifier.height(8.dp))
            LeagueNewsComponent(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxDisplayCount = 8,
                showHeaderAction = true
            )
        }
    }
}

private fun LazyListScope.deskGroupedHub(
    groupKey: String,
    title: String,
    subtitle: String,
    accent: Color,
    statusBadge: String,
    headerTestTag: String,
    rows: List<DeskHubActionSpec>
) {
    val borderColor = accent.copy(alpha = 0.42f)

    item(key = "${groupKey}_header") {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag(headerTestTag)
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = stroke)
                    drawLine(borderColor, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = stroke)
                    drawLine(borderColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = stroke)
                },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.11f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.9.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    color = accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    rows.forEachIndexed { idx, row ->
        val isLast = idx == rows.lastIndex
        item(key = "${groupKey}_${row.key}") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .drawBehind {
                            val stroke = 1.dp.toPx()
                            drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = stroke)
                            drawLine(borderColor, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = stroke)
                            if (isLast) {
                                drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke)
                            }
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = if (isLast) RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp) else RectangleShape
                ) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                        )
                        DeskHubActionRow(
                            icon = row.icon,
                            accent = row.accent,
                            title = row.title,
                            subtitle = row.subtitle,
                            buttonLabel = row.buttonLabel,
                            buttonTestTag = row.buttonTestTag,
                            rowTestTag = row.rowTestTag,
                            onClick = row.onClick,
                            secondaryButtonLabel = row.secondaryButtonLabel,
                            secondaryButtonTestTag = row.secondaryButtonTestTag,
                            onSecondaryClick = row.onSecondaryClick
                        )
                    }
                }
                if (isLast) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DeskHubActionRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    buttonLabel: String,
    buttonTestTag: String,
    rowTestTag: String,
    onClick: () -> Unit,
    secondaryButtonLabel: String? = null,
    secondaryButtonTestTag: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(rowTestTag)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        if (secondaryButtonLabel != null && secondaryButtonTestTag != null && onSecondaryClick != null) {
            OutlinedButton(
                onClick = onSecondaryClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .minimumInteractiveComponentSize()
                    .testTag(secondaryButtonTestTag)
            ) {
                Text(
                    text = secondaryButtonLabel,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(buttonTestTag)
        ) {
            val useWhiteText = accent == CrimsonAlert || accent == VioletAccent
            Text(
                text = buttonLabel,
                color = if (useWhiteText) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
