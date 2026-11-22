package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficiatingHubScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler {
        viewModel.openOfficiatingHub(false)
    }

    val offState = uiState.officiatingState
    val unresolvedCount = offState.activeUnresolvedIncidents.size
    var selectedSubTab by remember { mutableIntStateOf(if (unresolvedCount > 0) 3 else 0) }
    var expandedMatchId by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedSubTab) {
        listState.scrollToItem(0)
    }

    val accuracyColor = when {
        offState.leagueCallAccuracyPct >= 94.0 -> EmeraldAccent
        offState.leagueCallAccuracyPct >= 89.0 -> GoldPrimary
        else -> CrimsonAlert
    }
    val lockoutColor = when {
        offState.unionState.lockoutRisk >= 65 -> CrimsonAlert
        offState.unionState.lockoutRisk >= 40 -> AmberWarning
        else -> EmeraldAccent
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("officiating_hub_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LEAGUE OPERATIONS & INTEGRITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Officiating & Replay Command",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openOfficiatingHub(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_officiating")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
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
        ) {
            // 4-Section Uniform TabRow
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GoldPrimary
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Crews", maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_officiating_assignments")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Union", maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_officiating_union")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("AI/Replay", maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_officiating_tech")
                )
                Tab(
                    selected = selectedSubTab == 3,
                    onClick = { selectedSubTab = 3 },
                    text = {
                        Text(
                            text = if (unresolvedCount > 0) "Backlash ($unresolvedCount)" else "Backlash",
                            maxLines = 1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (unresolvedCount > 0) CrimsonAlert else Color.Unspecified
                        )
                    },
                    modifier = Modifier.testTag("tab_officiating_backlash")
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("officiating_tab_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Telemetry Hero Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, accuracyColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .testTag("card_officiating_telemetry"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accuracyColor.copy(alpha = 0.12f),
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OfficiatingMetricTile(
                                    label = "ACCURACY",
                                    value = "${offState.leagueCallAccuracyPct}%",
                                    sub = "${offState.totalBlownCalls} Blown",
                                    accent = accuracyColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_officiating_accuracy")
                                )
                                OfficiatingMetricTile(
                                    label = "INTEGRITY",
                                    value = "${uiState.leagueIntegrityScore}/100",
                                    sub = if (uiState.leagueIntegrityScore >= 80) "Trusted" else "Under Fire",
                                    accent = if (uiState.leagueIntegrityScore >= 80) CyanAccent else GoldPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_officiating_integrity")
                                )
                                OfficiatingMetricTile(
                                    label = "REF UNION",
                                    value = "${offState.unionState.lockoutRisk}%",
                                    sub = "Sat ${offState.unionState.unionSatisfaction}%",
                                    accent = lockoutColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_ref_union_risk")
                                )
                                OfficiatingMetricTile(
                                    label = "AI TECH",
                                    value = "${offState.installedTechCount}/${offState.techCatalog.size}",
                                    sub = "-${offState.totalErrorReductionPct}% Err",
                                    accent = VioletAccent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("metric_officiating_tech")
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = offState.lastAuditHeadline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (unresolvedCount > 0 && selectedSubTab != 3) {
                    item {
                        Surface(
                            color = CrimsonAlert.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, CrimsonAlert),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubTab = 3 }
                                .semantics(mergeDescendants = true) {}
                                .testTag("banner_urgent_blown_calls")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "$unresolvedCount Active Blown-Call Backlash Incident(s) — Tap to Address",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "RESOLVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CrimsonAlert
                                )
                            }
                        }
                    }
                }

                if (actionFeedback != null) {
                    item {
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                                .testTag("banner_officiating_feedback")
                        ) {
                            Text(
                                text = actionFeedback ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
                when (selectedSubTab) {
                    0 -> {
                        // TAB 0: REFEREE ASSIGNMENTS & CREW ROSTER
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = "Week ${uiState.currentWeek} Assignments",
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.autoOptimizeRefereeAssignments()
                                        actionFeedback = "Veteran Crew Chiefs prioritized onto marquee & rivalry matchups."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_auto_optimize_refs")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto-Assign", color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (offState.assignments.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = "No matchups scheduled for current phase. Simulate forward to generate officiating slate.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        } else {
                            items(offState.assignments, key = { it.matchId }) { assignment ->
                                val isExpanded = expandedMatchId == assignment.matchId
                                val riskColor = when {
                                    assignment.projectedBlownCallRiskPct >= 22 -> CrimsonAlert
                                    assignment.projectedBlownCallRiskPct >= 14 -> GoldPrimary
                                    else -> EmeraldAccent
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (assignment.isMarqueeOrRivalry) GoldPrimary.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .testTag("assignment_card_${assignment.matchId}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = assignment.stakesLabel.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (assignment.isMarqueeOrRivalry) GoldPrimary else CyanAccent,
                                                    fontSize = 10.sp
                                                )
                                                Text(
                                                    text = assignment.matchupTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Surface(
                                                color = riskColor.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${assignment.projectedBlownCallRiskPct}% Error Risk",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = riskColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Crew Chief: ${assignment.leadRefereeName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    expandedMatchId = if (isExpanded) null else assignment.matchId
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("btn_swap_ref_${assignment.matchId}")
                                            ) {
                                                Text(
                                                    text = if (isExpanded) "Done" else "Reassign",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        AnimatedVisibility(visible = isExpanded) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                Text(
                                                    text = "Select Active Crew Chief:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    val selectableRefs = offState.referees.filter { !it.isRestedOrBenched }
                                                    items(selectableRefs, key = { it.id }) { candidate ->
                                                        val selected = candidate.id == assignment.leadRefereeId
                                                        FilterChip(
                                                            selected = selected,
                                                            onClick = {
                                                                viewModel.assignRefereeToMatch(assignment.matchId, candidate.id)
                                                                actionFeedback = "Reassigned ${candidate.name} (${candidate.effectiveAccuracy(offState.unionState.replacementRefsActive, offState.totalErrorReductionPct)} Acc) to ${assignment.matchupTitle}."
                                                                expandedMatchId = null
                                                            },
                                                            label = {
                                                                Text(
                                                                    text = "${candidate.name.substringAfter(" ")} (${candidate.accuracyRating})",
                                                                    fontSize = 11.sp
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .minimumInteractiveComponentSize()
                                                                .testTag("assign_ref_chip_${assignment.matchId}_${candidate.id}")
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SectionHeader(title = "League Referee Pool (${offState.referees.size} Officials)")
                        }

                        items(offState.referees, key = { it.id }) { ref ->
                            val tierColor = Color(ref.tier.badgeColorHex)
                            val effAcc = ref.effectiveAccuracy(
                                offState.unionState.replacementRefsActive,
                                offState.totalErrorReductionPct
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (ref.isRestedOrBenched) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else tierColor.copy(alpha = 0.45f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .testTag("referee_card_${ref.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = ref.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black
                                            )
                                            if (ref.isUnionSteward) {
                                                Surface(
                                                    color = GoldPrimary.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "UNION REP",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = GoldPrimary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${ref.tier.label} • ${ref.archetype.label} (${ref.yearsExperience}y)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tierColor
                                        )
                                        Text(
                                            text = "Eff. Acc $effAcc • ${ref.conditionLabel} • ${ref.gamesOfficiatedSeason}G / ${ref.blownCallsSeason} Err",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.toggleRefereeRest(ref.id)
                                            actionFeedback = if (!ref.isRestedOrBenched) {
                                                "Rested ${ref.name} to recover fatigue."
                                            } else {
                                                "Activated ${ref.name} back into crew rotation."
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("btn_toggle_ref_rest_${ref.id}")
                                    ) {
                                        Text(
                                            text = if (ref.isRestedOrBenched) "Activate" else "Rest",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: REFEREE UNION (NLOA) BARGAINING & LABOR PROTECTIONS
                        val union = offState.unionState
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, lockoutColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                    .testTag("card_ref_union_status"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = union.unionName.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = lockoutColor
                                            )
                                            Text(
                                                text = union.statusLabel,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Surface(
                                            color = lockoutColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${union.cbaYearsRemaining}y CBA",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Black,
                                                color = lockoutColor,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Referee Satisfaction: ${union.unionSatisfaction}% • Lockout Threat: ${union.lockoutRisk}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (union.unionSatisfaction / 100f).coerceIn(0.05f, 1f) },
                                        color = if (union.unionSatisfaction >= 60) EmeraldAccent else CrimsonAlert,
                                        trackColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Annual Retainer: \$${union.annualRetainerMillions}M • Per-Game Bonus: \$${union.perGameBonusK}K • Rest Cap: ${union.mandatoryRestThreshold}% Fatigue",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Collective Bargaining & Compensation")
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val (_, msg) = viewModel.negotiateRefUnionRaise(2L, 5)
                                        actionFeedback = msg
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_ref_union_raise")
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Approve Pay Bump (+\$2M Retainer & +\$5K/Game)",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val (_, msg) = viewModel.ratifyNewRefereeCba()
                                        actionFeedback = msg
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_ratify_ref_cba")
                                ) {
                                    Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sign 4-Year NLOA Peace Accord (\$4M Signing Bonus)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Union Work Rules & Safeguards")
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Mandatory Fatigue Rest Cap (60%)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Prevents back-to-back high-fatigue burnout grievances (+7% Satisfaction, improves accuracy).",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = union.workloadProtectionActive,
                                            onCheckedChange = {
                                                viewModel.toggleRefUnionWorkloadProtection()
                                                actionFeedback = "Updated NLOA workload & fatigue rest policy."
                                            },
                                            modifier = Modifier.testTag("switch_ref_workload_cap")
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("L2M Public Discipline Due-Process Shield", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Shields officials from automatic union walkouts when you publish Last-Two-Minute apologies.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = union.publicDisciplineProtection,
                                            onCheckedChange = {
                                                viewModel.toggleRefUnionDisciplineShield()
                                                actionFeedback = "Updated NLOA public review due-process shield."
                                            },
                                            modifier = Modifier.testTag("switch_ref_discipline_shield")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: REPLAY TECHNOLOGY & AI OFFICIATING INVESTMENTS
                        item {
                            Surface(
                                color = CyanAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("banner_officiating_tech_summary")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = CyanAccent)
                                    Column {
                                        Text(
                                            text = "Active Automation Stack: ${offState.installedTechCount}/${offState.techCatalog.size} Modules",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "-${offState.totalErrorReductionPct}% Blown Call Rate • -${offState.totalBacklashDampeningPct}% Outrage Dampening • \$${offState.weeklyTechOpsCostMillions}M/wk Ops",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        items(offState.techCatalog, key = { it.type.id }) { item ->
                            val t = item.type
                            val accent = if (item.isInstalled) EmeraldAccent else CyanAccent
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.5.dp,
                                        if (item.isInstalled) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .testTag("tech_card_${t.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isInstalled) EmeraldAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = accent.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (item.isInstalled) "DEPLOYED" else "\$${t.costMillions}M",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = t.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "-${t.errorReductionPct}% Error Rate • +${t.integrityBoost} Integrity • Union ${if (t.unionFrictionDelta >= 0) "+${t.unionFrictionDelta}" else "${t.unionFrictionDelta}"}% • \$${t.weeklyOpsMillions}M/wk",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )

                                    if (!item.isInstalled) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                val (_, msg) = viewModel.investInOfficiatingTech(t.id)
                                                actionFeedback = msg
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                                .testTag("btn_invest_tech_${t.id}")
                                        ) {
                                            Icon(Icons.Default.Upgrade, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Invest \$${t.costMillions}M & Deploy",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // TAB 3: BLOWN CALLS & PUBLIC BACKLASH MANAGEMENT
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = "Backlash & L2M Desk",
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        val inc = viewModel.triggerOfficiatingAuditControversy()
                                        actionFeedback = "L2M Review flagged ${inc.severity.label} in ${inc.matchupTitle}."
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_run_l2m_audit")
                                ) {
                                    Text("L2M Audit Drill", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val activeIncidents = offState.activeUnresolvedIncidents
                        if (activeIncidents.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_no_active_backlash"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldAccent)
                                            Text("Zero Unresolved Officiating Controversies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "High-profile blown calls from close games and playoffs surface here for Commissioner crisis response. You can also trigger a Last-Two-Minute Audit Drill above.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            activeIncidents.forEach { incident ->
                                val sevColor = Color(incident.severity.accentHex)
                                val choices = OfficiatingEngine.backlashOptionsForIncident(incident, offState)
                                item(key = "header_${incident.id}") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.5.dp, sevColor, RoundedCornerShape(16.dp))
                                            .testTag("incident_card_${incident.id}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = sevColor.copy(alpha = 0.18f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = incident.severity.label.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = sevColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Week ${incident.week} • ${incident.fanOutragePct}% Outrage",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = sevColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${incident.matchupTitle} (${incident.finalScoreSummary})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = "${incident.callDescription}. Offending official: ${incident.offendingRefereeName} (Affected: ${incident.affectedTeamName}).",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Select Commissioner Backlash Response Below:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                        }
                                    }
                                }

                                items(choices, key = { "${incident.id}_${it.id}" }) { choice ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .clickable {
                                                val (_, msg) = viewModel.resolveBlownCallIncident(incident.id, choice)
                                                actionFeedback = msg
                                            }
                                            .testTag("backlash_choice_${incident.id}_${choice.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = choice.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (choice.costMillions > 0L) {
                                                    Text(
                                                        text = "-\$${choice.costMillions}M",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = GoldPrimary
                                                    )
                                                }
                                            }
                                            Text(
                                                text = choice.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Integrity ${if (choice.integrityDelta >= 0) "+" else ""}${choice.integrityDelta} • Fans ${if (choice.affectedFanMoraleDelta >= 0) "+" else ""}${choice.affectedFanMoraleDelta} • Union ${if (choice.unionSatisfactionDelta >= 0) "+" else ""}${choice.unionSatisfactionDelta}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val resolvedHistory = offState.incidents.filter { it.isResolved }
                        if (resolvedHistory.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                SectionHeader(title = "Resolved Controversy Ledger (${resolvedHistory.size})")
                            }
                            items(resolvedHistory, key = { it.id }) { past ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("resolved_incident_${past.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "W${past.week} • ${past.matchupTitle}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                        Text(
                                            text = past.chosenResponseTitle ?: "Commissioner Ruling Logged",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = past.resolutionHeadline ?: past.callDescription,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficiatingMetricTile(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
