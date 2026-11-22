package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplinaryDeskScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.openDisciplinaryDesk(false)
    }

    val discState = uiState.disciplinaryState
    val pendingCases = discState.pendingCases
    val appealedCases = discState.appealedCases
    val resolvedCases = discState.resolvedCases
    val activeSuspendedCount = remember(uiState.franchises) {
        uiState.franchises.sumOf { it.suspendedPlayersCount }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Docket, 1: Precedents, 2: Casebook & Appeals
    var activeReviewCaseId by remember { mutableStateOf<String?>(null) }
    var editingPrecedentCategory by remember { mutableStateOf<DisciplinaryCategory?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

    val activeReviewCase = remember(activeReviewCaseId, discState.incidents) {
        discState.incidents.firstOrNull { it.id == activeReviewCaseId }
    }

    val consistencyColor = when {
        discState.precedentConsistencyScore >= 82 -> EmeraldAccent
        discState.precedentConsistencyScore >= 65 -> GoldPrimary
        else -> CrimsonAlert
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("disciplinary_desk_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LEAGUE JUSTICE & PRECEDENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Commissioner Disciplinary Desk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openDisciplinaryDesk(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_disciplinary_desk")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to League"
                        )
                    }
                },
                actions = {
                    Surface(
                        color = consistencyColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, consistencyColor.copy(alpha = 0.45f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "${discState.precedentConsistencyScore}% Consistency",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = consistencyColor,
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
                    modifier = Modifier.testTag("tab_disc_docket"),
                    text = {
                        Text(
                            text = "Docket (${pendingCases.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_disc_precedent"),
                    text = {
                        Text(
                            text = "Precedents",
                            fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("tab_disc_casebook"),
                    text = {
                        val appealsSuffix = if (appealedCases.isNotEmpty()) " • ${appealedCases.size} Appeal" else ""
                        Text(
                            text = "Casebook (${resolvedCases.size + appealedCases.size})$appealsSuffix",
                            fontWeight = if (selectedTab == 2) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            if (feedbackBanner != null) {
                Surface(
                    color = CyanAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("disciplinary_feedback_banner"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Ruling Update",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feedbackBanner!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { feedbackBanner = null },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Message",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("disciplinary_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overview Telemetry Card
                item(key = "overview") {
                    DisciplinaryOverviewCard(
                        discState = discState,
                        activeSuspendedCount = activeSuspendedCount,
                        integrityScore = uiState.leagueIntegrityScore,
                        unionSatisfaction = uiState.unionAgreement.unionSatisfaction
                    )
                }

                when (selectedTab) {
                    0 -> {
                        item(key = "docket_controls") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ACTIVE HEARING DOCKET",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "${pendingCases.size} incident(s) awaiting Commissioner verdict",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val created = viewModel.generateDisciplinaryIncidentNow()
                                        feedbackBanner = if (created != null) {
                                            "Logged ${created.caseCode}: ${created.playerName} (${created.category.shortLabel}) added to docket."
                                        } else {
                                            "Unable to log new disciplinary incident."
                                        }
                                    },
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_generate_incident")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAlert,
                                        contentDescription = "Log Incident",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Log Incident",
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        if (pendingCases.isEmpty()) {
                            item(key = "docket_empty") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = "Clear Docket",
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Disciplinary Docket Clear",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "No pending hearings this week. Advance the week or log an investigation referral.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(pendingCases, key = { it.id }) { incident ->
                                PendingIncidentCard(
                                    incident = incident,
                                    standard = discState.standardFor(incident.category),
                                    onReview = { activeReviewCaseId = incident.id }
                                )
                            }
                        }
                    }

                    1 -> {
                        item(key = "precedent_intro") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Balance,
                                        contentDescription = "Precedent Guide",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Calibrate standing baselines for each category. Arbitrary departures without updating precedent erode consistency and fuel Players Union grievances.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(discState.precedents, key = { it.category.name }) { standard ->
                            PrecedentStandardCard(
                                standard = standard,
                                onEdit = { editingPrecedentCategory = standard.category }
                            )
                        }
                    }

                    2 -> {
                        if (appealedCases.isNotEmpty()) {
                            item(key = "appeals_header") {
                                Text(
                                    text = "UNION ARBITRATION APPEALS (${appealedCases.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonAlert,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            items(appealedCases, key = { "appeal_${it.id}" }) { incident ->
                                AppealArbitrationCard(
                                    incident = incident,
                                    standard = discState.standardFor(incident.category),
                                    onResolve = { action ->
                                        val (_, msg) = viewModel.resolveDisciplinaryAppeal(incident.id, action)
                                        feedbackBanner = msg
                                    }
                                )
                            }
                        }

                        val suspendedRosterPlayers = uiState.franchises.flatMap { team ->
                            team.roster.filter { it.isSuspended }.map { player -> team to player }
                        }
                        if (suspendedRosterPlayers.isNotEmpty()) {
                            item(key = "active_suspensions_header") {
                                Text(
                                    text = "ACTIVE SUSPENSIONS (${suspendedRosterPlayers.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarning,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            item(key = "active_suspensions_card") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_active_suspensions")
                                        .border(1.dp, AmberWarning.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        suspendedRosterPlayers.forEach { (team, player) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${player.name} (${player.position})",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${team.teamFullName} • Out of lineup",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Surface(
                                                    color = CrimsonAlert.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${player.suspendedGamesRemaining}G LEFT",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = CrimsonAlert,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "casebook_header") {
                            Text(
                                text = "ADJUDICATED CASEBOOK (${resolvedCases.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        if (resolvedCases.isEmpty() && appealedCases.isEmpty()) {
                            item(key = "casebook_empty") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No Adjudicated Cases Yet",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Rulings issued from the Docket appear here with full precedent attribution.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(resolvedCases, key = { "casebook_${it.id}" }) { incident ->
                                CasebookEntryCard(incident = incident)
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeReviewCase != null) {
        DisciplinaryRulingDialog(
            incident = activeReviewCase,
            uiState = uiState,
            onDismiss = { activeReviewCaseId = null },
            onIssueRuling = { games, fineThousands, setNewPrecedent, note ->
                val (_, message) = viewModel.adjudicateDisciplinaryCase(
                    incidentId = activeReviewCase.id,
                    suspensionGames = games,
                    fineThousands = fineThousands,
                    setNewPrecedent = setNewPrecedent,
                    commissionerNote = note
                )
                feedbackBanner = message
                activeReviewCaseId = null
            }
        )
    }

    if (editingPrecedentCategory != null) {
        val std = discState.standardFor(editingPrecedentCategory!!)
        PrecedentEditorDialog(
            standard = std,
            onDismiss = { editingPrecedentCategory = null },
            onSave = { games, fineThousands, posture ->
                val msg = viewModel.updateDisciplinaryPrecedentPolicy(
                    category = std.category,
                    benchmarkGames = games,
                    benchmarkFineThousands = fineThousands,
                    posture = posture
                )
                feedbackBanner = msg
                editingPrecedentCategory = null
            }
        )
    }
}

@Composable
private fun DisciplinaryOverviewCard(
    discState: DisciplinaryDeskState,
    activeSuspendedCount: Int,
    integrityScore: Int,
    unionSatisfaction: Int
) {
    val consistencyColor = when {
        discState.precedentConsistencyScore >= 82 -> EmeraldAccent
        discState.precedentConsistencyScore >= 65 -> GoldPrimary
        else -> CrimsonAlert
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_disciplinary_overview")
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PRECEDENT & ENFORCEMENT INDEX",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Consistency ${discState.precedentConsistencyScore}/100 • Integrity $integrityScore/100",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Surface(
                    color = consistencyColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Union $unionSatisfaction%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (unionSatisfaction >= 60) EmeraldAccent else AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverviewMetricChip(
                    label = "Pending",
                    value = "${discState.pendingCases.size}",
                    accent = if (discState.pendingCases.isNotEmpty()) AmberWarning else EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                OverviewMetricChip(
                    label = "Appeals",
                    value = "${discState.appealedCases.size}",
                    accent = if (discState.appealedCases.isNotEmpty()) CrimsonAlert else CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                OverviewMetricChip(
                    label = "Suspended",
                    value = "$activeSuspendedCount",
                    accent = if (activeSuspendedCount > 0) CrimsonAlert else EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                OverviewMetricChip(
                    label = "Fines",
                    value = DisciplinaryEngine.formatFine(discState.totalFinesCollectedThousands.toInt()),
                    accent = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverviewMetricChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PendingIncidentCard(
    incident: DisciplinaryIncident,
    standard: PrecedentStandard,
    onReview: () -> Unit
) {
    val categoryColor = Color(incident.category.badgeColorHex)
    val severityColor = Color(incident.severity.badgeColorHex)
    val (expGames, expFine) = standard.expectedPenaltyFor(incident.severity, incident.priorOffensesCount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_disc_incident_${incident.id}")
            .border(1.dp, severityColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = incident.category.shortLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = severityColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = incident.severity.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "${incident.caseCode} • W${incident.weekNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(incident.franchiseColorHex))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = incident.incidentTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${incident.playerName} (${incident.playerPosition}, ${incident.playerOverall} OVR) • ${incident.franchiseName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = incident.incidentSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prior Offenses: ${incident.priorOffensesCount} • ${standard.posture.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Precedent Target: ${expGames}G • ${DisciplinaryEngine.formatFine(expFine)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent
                        )
                    }
                    Button(
                        onClick = onReview,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_review_case_${incident.id}")
                    ) {
                        Text(
                            text = "Review & Rule",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrecedentStandardCard(
    standard: PrecedentStandard,
    onEdit: () -> Unit
) {
    val categoryColor = Color(standard.category.badgeColorHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_precedent_${standard.category.name}")
            .border(1.dp, categoryColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = standard.category.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = categoryColor
                    )
                    Text(
                        text = standard.category.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = GoldPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = standard.posture.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Standard Baseline: ${standard.benchmarkGames}G • ${DisciplinaryEngine.formatFine(standard.benchmarkFineThousands)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Repeat Scaling: ${standard.repeatOffenderMultiplier}x • Ref: ${standard.lastCaseCitation}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("btn_edit_precedent_${standard.category.name}")
                ) {
                    Text(
                        text = "Calibrate",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AppealArbitrationCard(
    incident: DisciplinaryIncident,
    standard: PrecedentStandard,
    onResolve: (AppealResolutionAction) -> Unit
) {
    val ruling = incident.ruling ?: return
    val (precGames, precFine) = standard.expectedPenaltyFor(incident.severity, incident.priorOffensesCount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_appeal_${incident.id}")
            .border(1.5.dp, CrimsonAlert.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${incident.caseCode} • UNION ARBITRATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = CrimsonAlert
                )
                Text(
                    text = "Ruled: ${ruling.suspensionGames}G / ${DisciplinaryEngine.formatFine(ruling.fineThousands)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
            Text(
                text = "${incident.playerName} (${incident.franchiseName}) — ${incident.incidentTitle}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            if (!incident.appealBrief.isNullOrBlank()) {
                Text(
                    text = incident.appealBrief,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Standard Precedent Reference: ${precGames}G / ${DisciplinaryEngine.formatFine(precFine)}",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onResolve(AppealResolutionAction.UPHOLD_ORIGINAL) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_uphold_appeal_${incident.id}")
                ) {
                    Text(
                        text = "Uphold",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Button(
                    onClick = { onResolve(AppealResolutionAction.SPLIT_COMPROMISE) },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_compromise_appeal_${incident.id}")
                ) {
                    Text(
                        text = "Compromise",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Button(
                    onClick = { onResolve(AppealResolutionAction.REDUCE_TO_PRECEDENT) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_reduce_to_precedent_${incident.id}")
                ) {
                    Text(
                        text = "Match Prec.",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CasebookEntryCard(incident: DisciplinaryIncident) {
    val ruling = incident.ruling ?: return
    val alignColor = Color(ruling.alignmentTier.badgeColorHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_casebook_${incident.id}")
            .border(1.dp, alignColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${incident.caseCode} • Y${ruling.issuedYear} W${ruling.issuedWeek}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = alignColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = ruling.alignmentTier.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = alignColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "${incident.playerName} (${incident.franchiseName}) — ${incident.incidentTitle}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Imposed: ${ruling.suspensionGames} Game(s) • Fine ${DisciplinaryEngine.formatFine(ruling.fineThousands)}" +
                    if (ruling.setNewPrecedent) " • New Category Landmark" else "",
                style = MaterialTheme.typography.bodySmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = ruling.deviationSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ruling.appealOutcomeSummary.isNotBlank()) {
                Text(
                    text = "Arbitration: ${ruling.appealOutcomeSummary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (ruling.commissionerNote.isNotBlank()) {
                Text(
                    text = "Memo: \"${ruling.commissionerNote}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DisciplinaryRulingDialog(
    incident: DisciplinaryIncident,
    uiState: LeagueUiState,
    onDismiss: () -> Unit,
    onIssueRuling: (Int, Int, Boolean, String) -> Unit
) {
    val category = incident.category
    val fineStep = if (category.maxFineThousands >= 400) 25 else 5
    var proposedGames by remember(incident.id) { mutableIntStateOf(incident.recommendedGames) }
    var proposedFineThousands by remember(incident.id) { mutableIntStateOf(incident.recommendedFineThousands) }
    var setNewPrecedent by remember(incident.id) { mutableStateOf(false) }
    var noteText by remember(incident.id) { mutableStateOf("") }

    val eval = remember(proposedGames, proposedFineThousands, setNewPrecedent, uiState.disciplinaryState) {
        DisciplinaryEngine.evaluateProposedRuling(
            incident = incident,
            proposedGames = proposedGames,
            proposedFineThousands = proposedFineThousands,
            setNewPrecedent = setNewPrecedent,
            state = uiState.disciplinaryState,
            unionAgreement = uiState.unionAgreement,
            ruleBook = uiState.ruleBook
        )
    }

    val alignColor = Color(eval.alignmentTier.badgeColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp)
                .testTag("disciplinary_ruling_modal")
                .border(1.5.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${incident.caseCode} • ${category.shortLabel.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = incident.incidentTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_cancel_ruling")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Ruling Modal")
                    }
                }

                Text(
                    text = "${incident.playerName} (${incident.playerPosition}, ${incident.playerOverall} OVR) • ${incident.franchiseName} • Prior Offenses: ${incident.priorOffensesCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = incident.evidenceSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Citation: ${incident.precedentCitation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                // Suspension Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Suspension Length",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$proposedGames Games",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.testTag("txt_proposed_games")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { proposedGames = (proposedGames - 1).coerceAtLeast(0) },
                        enabled = proposedGames > 0,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_games_minus")
                    ) {
                        Text("-1G", fontWeight = FontWeight.Black)
                    }
                    Slider(
                        value = proposedGames.toFloat(),
                        onValueChange = { proposedGames = it.roundToInt().coerceIn(0, category.maxGames) },
                        valueRange = 0f..category.maxGames.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("slider_suspension_games"),
                        colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                    )
                    OutlinedButton(
                        onClick = { proposedGames = (proposedGames + 1).coerceAtMost(category.maxGames) },
                        enabled = proposedGames < category.maxGames,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_games_plus")
                    ) {
                        Text("+1G", fontWeight = FontWeight.Black)
                    }
                }

                // Fine Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Financial Fine",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DisciplinaryEngine.formatFine(proposedFineThousands),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = EmeraldAccent,
                        modifier = Modifier.testTag("txt_proposed_fine")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { proposedFineThousands = (proposedFineThousands - fineStep).coerceAtLeast(0) },
                        enabled = proposedFineThousands > 0,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_fine_minus")
                    ) {
                        Text("-\$${fineStep}K", fontWeight = FontWeight.Black)
                    }
                    Slider(
                        value = proposedFineThousands.toFloat(),
                        onValueChange = {
                            proposedFineThousands = ((it / fineStep).roundToInt() * fineStep)
                                .coerceIn(0, category.maxFineThousands)
                        },
                        valueRange = 0f..category.maxFineThousands.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("slider_fine_thousands"),
                        colors = SliderDefaults.colors(thumbColor = EmeraldAccent, activeTrackColor = EmeraldAccent)
                    )
                    OutlinedButton(
                        onClick = { proposedFineThousands = (proposedFineThousands + fineStep).coerceAtMost(category.maxFineThousands) },
                        enabled = proposedFineThousands < category.maxFineThousands,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_fine_plus")
                    ) {
                        Text("+\$${fineStep}K", fontWeight = FontWeight.Black)
                    }
                }

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            proposedGames = (eval.expectedGames - 1).coerceAtLeast(0)
                            proposedFineThousands = (eval.expectedFineThousands * 0.65).roundToInt()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_preset_lenient")
                    ) {
                        Text("Lenient", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            proposedGames = eval.expectedGames
                            proposedFineThousands = eval.expectedFineThousands
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_preset_match_precedent")
                    ) {
                        Text("Match Precedent", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            proposedGames = (eval.expectedGames + 3).coerceAtMost(category.maxGames)
                            proposedFineThousands = (eval.expectedFineThousands * 1.5).roundToInt().coerceAtMost(category.maxFineThousands)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_preset_strict")
                    ) {
                        Text("Maximum", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Live Precedent Comparison Preview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_precedent_preview")
                        .border(1.dp, alignColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = alignColor.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = eval.alignmentTier.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = alignColor
                            )
                            Text(
                                text = "Appeal Risk: ${eval.appealRiskPct}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (eval.appealRiskPct >= 45) CrimsonAlert else CyanAccent
                            )
                        }
                        Text(
                            text = eval.deviationLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val consSign = if (eval.consistencyDelta >= 0) "+" else ""
                        val intSign = if (eval.integrityDelta >= 0) "+" else ""
                        val unionSign = if (eval.unionSatisfactionDelta >= 0) "+" else ""
                        Text(
                            text = "Expected: ${eval.expectedGames}G / ${DisciplinaryEngine.formatFine(eval.expectedFineThousands)}  •  " +
                                "Consistency $consSign${eval.consistencyDelta}  •  Integrity $intSign${eval.integrityDelta}  •  Union $unionSign${eval.unionSatisfactionDelta}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Establish as New ${category.shortLabel} Precedent",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Updates league benchmark so future cases follow this standard",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = setNewPrecedent,
                        onCheckedChange = { setNewPrecedent = it },
                        modifier = Modifier.testTag("switch_set_new_precedent")
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Commissioner Memorandum (Optional)") },
                    placeholder = { Text("Cite safety rationale or mitigating factors...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ruling_note")
                )

                Button(
                    onClick = { onIssueRuling(proposedGames, proposedFineThousands, setNewPrecedent, noteText) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_issue_ruling")
                ) {
                    Text(
                        text = "Issue Official Ruling (${proposedGames}G • ${DisciplinaryEngine.formatFine(proposedFineThousands)})",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecedentEditorDialog(
    standard: PrecedentStandard,
    onDismiss: () -> Unit,
    onSave: (Int, Int, PrecedentPosture) -> Unit
) {
    val category = standard.category
    val fineStep = if (category.maxFineThousands >= 400) 25 else 5
    var benchmarkGames by remember(standard.category) { mutableIntStateOf(standard.benchmarkGames) }
    var benchmarkFine by remember(standard.category) { mutableIntStateOf(standard.benchmarkFineThousands) }
    var posture by remember(standard.category) { mutableStateOf(standard.posture) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .heightIn(max = 620.dp)
                .testTag("disciplinary_precedent_modal")
                .border(1.5.dp, CyanAccent.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRECEDENT CALIBRATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Calibration Modal")
                    }
                }

                Text(
                    text = "Enforcement Posture",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrecedentPosture.values().toList().chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { p ->
                                FilterChip(
                                    selected = posture == p,
                                    onClick = {
                                        posture = p
                                        benchmarkGames = (category.defaultGames * p.multiplier).roundToInt().coerceIn(0, category.maxGames)
                                        benchmarkFine = (category.defaultFineThousands * p.multiplier).roundToInt().coerceIn(5, category.maxFineThousands)
                                    },
                                    label = {
                                        Text(
                                            text = p.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chip_posture_${p.name}")
                                )
                            }
                        }
                    }
                }

                Text(
                    text = posture.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Baseline Games: ${benchmarkGames}G", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { benchmarkGames = (benchmarkGames - 1).coerceAtLeast(0) },
                            enabled = benchmarkGames > 0,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_std_games_minus")
                        ) { Text("-1G") }
                        OutlinedButton(
                            onClick = { benchmarkGames = (benchmarkGames + 1).coerceAtMost(category.maxGames) },
                            enabled = benchmarkGames < category.maxGames,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_std_games_plus")
                        ) { Text("+1G") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Baseline Fine: ${DisciplinaryEngine.formatFine(benchmarkFine)}", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { benchmarkFine = (benchmarkFine - fineStep).coerceAtLeast(0) },
                            enabled = benchmarkFine > 0,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_std_fine_minus")
                        ) { Text("-\$${fineStep}K") }
                        OutlinedButton(
                            onClick = { benchmarkFine = (benchmarkFine + fineStep).coerceAtMost(category.maxFineThousands) },
                            enabled = benchmarkFine < category.maxFineThousands,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_std_fine_plus")
                        ) { Text("+\$${fineStep}K") }
                    }
                }

                Button(
                    onClick = { onSave(benchmarkGames, benchmarkFine, posture) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_save_precedent")
                ) {
                    Text(
                        text = "Update League Precedent Standard",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
