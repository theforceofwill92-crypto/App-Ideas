package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CrisisArchetype
import com.example.model.CrisisEngine
import com.example.ui.components.DilemmaDialog
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisEventsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.openCrisisEventsScreen(false)
    }

    val crisisState = uiState.crisisState
    val scheduledBranches = crisisState.scheduledBranches
    val resolvedHistory = crisisState.resolvedHistory
    val threatMetrics = remember(
        uiState.leagueIntegrityScore,
        uiState.unionAgreement,
        uiState.officiatingState,
        uiState.franchises,
        uiState.disciplinaryState
    ) {
        CrisisEngine.computeThreatRadar(
            integrityScore = uiState.leagueIntegrityScore,
            unionAgreement = uiState.unionAgreement,
            officiatingState = uiState.officiatingState,
            franchises = uiState.franchises,
            disciplinaryState = uiState.disciplinaryState
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active & Branches, 1: Consequence Ledger, 2: Threat Radar
    var selectedArchetype by remember { mutableStateOf<CrisisArchetype?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("crisis_events_screen"),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "BRANCHING NARRATIVE COMMAND",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonAlert,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Crisis Events & Storylines",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.openCrisisEventsScreen(false) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_back_crisis_events")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Desk"
                            )
                        }
                    },
                    actions = {
                        Surface(
                            color = CrimsonAlert.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "${scheduledBranches.size} Branches",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CrimsonAlert,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GoldPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_crisis_active"),
                        text = {
                            Text(
                                text = "Branches (${scheduledBranches.size})",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_crisis_ledger"),
                        text = {
                            Text(
                                text = "Ledger (${resolvedHistory.size})",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("tab_crisis_radar"),
                        text = {
                            Text(
                                text = "Threat Radar",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("crisis_events_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Summary & Situation Trigger Launcher
                            item(key = "crisis_launcher") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_crisis_launcher"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "SITUATION ROOM TELEMETRY",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GoldPrimary,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.8.sp
                                                )
                                                Text(
                                                    text = "Y${uiState.currentYear} • Week ${uiState.currentWeek} • Integrity ${uiState.leagueIntegrityScore}/100",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                color = EmeraldAccent.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${crisisState.completedArcIds.size} Arcs Closed",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldAccent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Select a specific crisis focus or let league conditions trigger a multi-stage storyline with delayed Act II/III fallout.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            item(key = "arch_all") {
                                                FilterChip(
                                                    selected = selectedArchetype == null,
                                                    onClick = { selectedArchetype = null },
                                                    label = { Text("Contextual Auto") },
                                                    modifier = Modifier.testTag("chip_crisis_arch_AUTO")
                                                )
                                            }
                                            items(CrisisArchetype.values().toList(), key = { it.name }) { arch ->
                                                FilterChip(
                                                    selected = selectedArchetype == arch,
                                                    onClick = { selectedArchetype = arch },
                                                    label = { Text(arch.displayTitle) },
                                                    modifier = Modifier.testTag("chip_crisis_arch_${arch.name}")
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.triggerCrisisScenarioNow(selectedArchetype) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                                .testTag("btn_trigger_branching_crisis")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = "Trigger Crisis",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (uiState.activeDilemma != null) {
                                                    "Review Pending Active Crisis Ruling"
                                                } else if (selectedArchetype != null) {
                                                    "Convene Emergency Briefing (${selectedArchetype!!.categoryLabel})"
                                                } else {
                                                    "Trigger Branching Crisis Briefing"
                                                },
                                                color = Color.White,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }

                            // Pending Branch Follow-ups Header & Cards
                            item(key = "branches_header") {
                                Text(
                                    text = "DEVELOPING BRANCHES & FALLOUT TIMELINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            if (scheduledBranches.isEmpty()) {
                                item(key = "branches_empty") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("crisis_branches_empty"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "No pending branches",
                                                tint = CyanAccent
                                            )
                                            Text(
                                                text = "No secondary crisis branches currently brewing. Decisions that defer, investigate, or escalate will schedule follow-up confrontations here.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(scheduledBranches, key = { it.id }) { branch ->
                                    val archColor = Color(branch.archetype.accentHex)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("card_scheduled_branch_${branch.followUpNodeId}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = BorderStroke(1.dp, archColor.copy(alpha = 0.55f)),
                                        shape = RoundedCornerShape(14.dp)
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
                                                Surface(
                                                    color = archColor.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${branch.archetype.displayTitle} • Stage ${branch.stageNumber}/${branch.totalStages}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = archColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Surface(
                                                    color = CrimsonAlert.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "ETA Y${branch.triggerYear} W${branch.triggerWeek}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CrimsonAlert,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = branch.teaserLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Triggered by: ${branch.priorDecisionLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VioletAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val targets = listOfNotNull(
                                                branch.targetFranchiseName,
                                                branch.targetPlayerName
                                            )
                                            if (targets.isNotEmpty()) {
                                                Text(
                                                    text = "Stakeholders: ${targets.joinToString(" • ")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CyanAccent
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Crisis Archetypes Directory
                            item(key = "archetypes_header") {
                                Text(
                                    text = "LEAGUE CRISIS ARCHETYPES (8 MULTI-STAGE ARCS)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            items(CrisisArchetype.values().toList(), key = { "dir_${it.name}" }) { arch ->
                                val archColor = Color(arch.accentHex)
                                val hasScheduled = scheduledBranches.any { it.archetype == arch }
                                val resolvedCount = resolvedHistory.count { it.archetype == arch }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_archetype_${arch.name}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, archColor.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(archColor.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                                contentDescription = arch.displayTitle,
                                                tint = archColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = arch.displayTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = arch.summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = if (hasScheduled) CrimsonAlert.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    hasScheduled -> "Active Branch"
                                                    resolvedCount > 0 -> "$resolvedCount Rulings"
                                                    else -> "Ready"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasScheduled) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Consequence Ledger
                            if (resolvedHistory.isEmpty()) {
                                item(key = "ledger_empty") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("crisis_ledger_empty"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Gavel,
                                                contentDescription = "Empty Ledger",
                                                tint = GoldPrimary
                                            )
                                            Text(
                                                text = "No crisis decrees have been issued yet. Your rulings, multi-system consequences, and spawned Act II/III chains will be archived here.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(resolvedHistory, key = { it.id }) { rec ->
                                    val archColor = Color(rec.archetype.accentHex)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("card_crisis_history_${rec.id}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = BorderStroke(1.dp, archColor.copy(alpha = 0.45f)),
                                        shape = RoundedCornerShape(14.dp)
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
                                                Text(
                                                    text = "${rec.archetype.displayTitle} • Stage ${rec.stageNumber}/${rec.totalStages}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = archColor
                                                )
                                                Text(
                                                    text = "Y${rec.seasonYear} • W${rec.weekNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary
                                                )
                                            }
                                            Text(
                                                text = rec.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (rec.affectedEntityLabel.isNotBlank()) {
                                                Text(
                                                    text = rec.affectedEntityLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CyanAccent
                                                )
                                            }
                                            Text(
                                                text = "Decree: ${rec.chosenLabel}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = rec.narrativeOutcome,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (rec.consequenceTags.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    rec.consequenceTags.chunked(3).forEach { tagRow ->
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            tagRow.forEach { tag ->
                                                                val isPositive = tag.startsWith("+")
                                                                val c = if (isPositive) EmeraldAccent else CrimsonAlert
                                                                Surface(
                                                                    color = c.copy(alpha = 0.14f),
                                                                    shape = RoundedCornerShape(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = tag,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = c,
                                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            rec.spawnedBranchTeaser?.let { teaser ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                                        contentDescription = "Spawned Branch",
                                                        tint = GoldPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Spawned Branch: $teaser",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Threat Radar
                            item(key = "radar_intro") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_radar_overview"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Radar,
                                            contentDescription = "Threat Radar",
                                            tint = CyanAccent
                                        )
                                        Text(
                                            text = "Elevated sector exposure dynamically increases the likelihood of contextual crises erupting across your league.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            items(threatMetrics, key = { it.key }) { metric ->
                                val riskColor = Color(metric.accentHex)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_threat_${metric.key}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(14.dp)
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
                                            Text(
                                                text = metric.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${metric.riskScore}% Risk",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Black,
                                                color = riskColor
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { (metric.riskScore / 100f).coerceIn(0f, 1f) },
                                            color = riskColor,
                                            trackColor = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = metric.driverSummary,
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

        // Render DilemmaDialog directly if a crisis fires while viewing Situation Room
        if (uiState.activeDilemma != null) {
            DilemmaDialog(
                dilemma = uiState.activeDilemma,
                onChoiceSelected = { idx -> viewModel.resolveDilemma(idx) }
            )
        }
    }
}
