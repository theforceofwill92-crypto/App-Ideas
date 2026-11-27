package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Undo
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
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCommitteeScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.openRuleCommitteeScreen(false)
    }

    val committeeState = uiState.ruleCommitteeState
    val sport = uiState.sportType
    val modifiers = remember(committeeState, sport) {
        RuleCommitteeEngine.computeSimulationModifiers(committeeState, sport)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Docket, 1: Draft Proposal, 2: Sim Telemetry
    var statusFilter by remember { mutableStateOf<String>("ALL") } // ALL, ENACTED, PENDING
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    // Custom Proposal Form State
    var customTitle by remember { mutableStateOf("") }
    var customSponsor by remember { mutableStateOf("Commissioner Rules Taskforce") }
    var customSummary by remember { mutableStateOf("") }
    var selectedLever by remember { mutableStateOf(RuleCommitteeFocusLever.OFFENSIVE_TEMPO) }
    var selectedIntensity by remember { mutableIntStateOf(2) }

    val listState = rememberLazyListState()
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

    val filteredProposals = remember(committeeState.proposals, statusFilter) {
        when (statusFilter) {
            "ENACTED" -> committeeState.proposals.filter { it.status == SportRuleProposalStatus.ENACTED }
            "PENDING" -> committeeState.proposals.filter { it.status != SportRuleProposalStatus.ENACTED }
            else -> committeeState.proposals
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("rule_committee_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "COMPETITION & RULE COMMITTEE",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${sport.displayName} Rules Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openRuleCommitteeScreen(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_rule_committee")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to League Rules"
                        )
                    }
                },
                actions = {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "${modifiers.activeRuleCount} Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                    text = { Text("Docket (${committeeState.proposals.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_committee_docket")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Draft Rule", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_committee_draft")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Sim Impact (${committeeState.impactLogs.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_committee_telemetry")
                )
            }

            if (statusFeedback != null) {
                Surface(
                    color = EmeraldAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("committee_feedback_banner"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusFeedback!!,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { statusFeedback = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss Feedback", tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("rule_committee_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    uiState.leagueSponsors.activeBreachWarning?.let { warning ->
                        item(key = "rc_sponsor_breach_warning") {
                            SponsorBreachWarningCard(
                                warning = warning,
                                onDismiss = { viewModel.dismissSponsorBreachWarning() }
                            )
                        }
                    }

                    if (uiState.leagueSponsors.pendingOffseasonRuleVotes.isNotEmpty()) {
                        item(key = "rc_sponsor_rule_demands_header") {
                            OffseasonSponsorRuleVotesCard(
                                votes = uiState.leagueSponsors.pendingOffseasonRuleVotes,
                                isOffSeason = uiState.seasonPhase == SeasonPhase.OFF_SEASON,
                                onVote = { ruleId, approve ->
                                    val res = viewModel.voteOnSponsorRuleDemand(ruleId, approve)
                                    statusFeedback = res.message
                                }
                            )
                        }
                    }

                    // Live Simulation Engine Impact Telemetry Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .testTag("card_committee_overview"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
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
                                            text = "LIVE SIMULATION ENGINE MODIFIERS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = GoldPrimary
                                        )
                                        Text(
                                            text = "${sport.displayName} Active Rule Footprint",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Surface(
                                        color = if (modifiers.activeRuleCount > 0) EmeraldAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (modifiers.activeRuleCount > 0) "${modifiers.activeRuleCount} Live Laws" else "Baseline Sim",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (modifiers.activeRuleCount > 0) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val scoreLabel = if (sport == SportType.SOCCER || sport == SportType.HOCKEY) {
                                        val s = if (modifiers.scoringDeltaPerTeam >= 0) "+" else ""
                                        "$s${String.format(Locale.US, "%.2f", modifiers.scoringDeltaPerTeam)} G/Gm"
                                    } else {
                                        val s = if (modifiers.scoringDeltaPerTeam >= 0) "+" else ""
                                        "$s${String.format(Locale.US, "%.1f", modifiers.scoringDeltaPerTeam)} PPG"
                                    }
                                    CommitteeMetricPill(
                                        label = "SCORING SHIFT",
                                        value = scoreLabel,
                                        accent = if (modifiers.scoringDeltaPerTeam > 0.1) EmeraldAccent else if (modifiers.scoringDeltaPerTeam < -0.1) CrimsonAlert else CyanAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CommitteeMetricPill(
                                        label = "INJURY RATE",
                                        value = "${String.format(Locale.US, "%.2f", modifiers.injuryRiskMultiplier)}x",
                                        accent = if (modifiers.injuryRiskMultiplier < 0.96) EmeraldAccent else if (modifiers.injuryRiskMultiplier > 1.05) AmberWarning else CyanAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CommitteeMetricPill(
                                        label = "CLUTCH / OT",
                                        value = if (modifiers.convertDrawsToShootout) "Shootout" else "+${modifiers.closeGameSwingPct}%",
                                        accent = GoldPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Standing Committee Panel Members
                                Text(
                                    text = "STANDING COMMITTEE PANEL (${committeeState.members.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(committeeState.members) { member ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                Text(
                                                    text = member.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = member.role.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    color = CyanAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = statusFilter == "ALL",
                                onClick = { statusFilter = "ALL" },
                                label = { Text("All (${committeeState.proposals.size})") },
                                modifier = Modifier.testTag("filter_rc_all")
                            )
                            FilterChip(
                                selected = statusFilter == "ENACTED",
                                onClick = { statusFilter = "ENACTED" },
                                label = { Text("Enacted (${committeeState.enactedProposals.size})") },
                                modifier = Modifier.testTag("filter_rc_enacted")
                            )
                            FilterChip(
                                selected = statusFilter == "PENDING",
                                onClick = { statusFilter = "PENDING" },
                                label = { Text("Docket (${committeeState.pendingDocketProposals.size})") },
                                modifier = Modifier.testTag("filter_rc_pending")
                            )
                        }
                    }

                    // Sport-Specific Proposal Cards
                    items(filteredProposals, key = { it.id }) { proposal ->
                        SportRuleProposalCard(
                            proposal = proposal,
                            onReview = {
                                val res = viewModel.reviewProposalInCommittee(proposal.id)
                                if (res != null) {
                                    statusFeedback = res.committeeReviewNote
                                }
                            },
                            onEnact = {
                                val (_, msg) = viewModel.enactCommitteeProposal(proposal.id)
                                statusFeedback = msg
                            },
                            onRepeal = {
                                val (_, msg) = viewModel.repealCommitteeProposal(proposal.id)
                                statusFeedback = msg
                            }
                        )
                    }
                } else if (selectedTab == 1) {
                    // Draft Custom Sport-Specific Proposal Tab (split into discrete LazyColumn items for smooth scrolling on compact screens)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .testTag("card_custom_proposal_builder"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "COMMISSIONER RULE INITIATIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Propose New ${sport.displayName} Rule",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Author a custom amendment targeted to ${sport.displayName}. Choose its simulation lever and intensity, then submit it to the Rule Committee docket or enact it immediately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = customTitle,
                                    onValueChange = { customTitle = it },
                                    label = { Text("Rule Title") },
                                    placeholder = { Text("e.g., Bonus Free-Kick Box Perimeter") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_custom_rule_title")
                                )

                                OutlinedTextField(
                                    value = customSponsor,
                                    onValueChange = { customSponsor = it },
                                    label = { Text("Sponsor / Committee Working Group") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_custom_rule_sponsor")
                                )

                                OutlinedTextField(
                                    value = customSummary,
                                    onValueChange = { customSummary = it },
                                    label = { Text("Rule Text & Competitive Rationale") },
                                    placeholder = { Text("Describe the on-field mechanics...") },
                                    minLines = 2,
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_custom_rule_summary")
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "SIMULATION ENGINE TARGET LEVER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(RuleCommitteeFocusLever.values().toList(), key = { it.name }) { lever ->
                        val isSel = lever == selectedLever
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { selectedLever = lever }
                                .testTag("chip_lever_${lever.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lever.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = lever.shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = CyanAccent
                                    )
                                }
                                Text(
                                    text = lever.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "MODIFIER INTENSITY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1 to "Moderate", 2 to "Bold", 3 to "Radical").forEach { (lvl, label) ->
                                    val isSel = selectedIntensity == lvl
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedIntensity = lvl },
                                        label = { Text(label, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_intensity_$lvl")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val drafted = viewModel.draftAndProposeCustomCommitteeRule(
                                            title = customTitle,
                                            sponsorName = customSponsor,
                                            focusLever = selectedLever,
                                            summary = customSummary,
                                            intensityLevel = selectedIntensity,
                                            immediatelyEnact = false
                                        )
                                        customTitle = ""
                                        customSummary = ""
                                        statusFeedback = "Queued '${drafted.title}' onto the Rule Committee docket."
                                        selectedTab = 0
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_submit_custom_proposal"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add to Docket", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val drafted = viewModel.draftAndProposeCustomCommitteeRule(
                                            title = customTitle,
                                            sponsorName = customSponsor,
                                            focusLever = selectedLever,
                                            summary = customSummary,
                                            intensityLevel = selectedIntensity,
                                            immediatelyEnact = true
                                        )
                                        customTitle = ""
                                        customSummary = ""
                                        statusFeedback = "Enacted '${drafted.title}' immediately into ${sport.displayName} simulation!"
                                        selectedTab = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_enact_custom_proposal"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Propose & Enact", color = Color.Black, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                } else {
                    // Tab 2: Live Simulation Telemetry & Comparison Log
                    item {
                        val baseline = RuleCommitteeEngine.sportBaselineCombinedScore(sport)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "SIMULATION ENGINE OUTCOME TELEMETRY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${sport.displayName} Combined Scoring Baseline: ${String.format(Locale.US, "%.1f", baseline)} pts/goals per game",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Each simulated week logs actual combined scoring averages, close finishes/shootouts, injuries, and enacted rule effects against the unamended baseline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (committeeState.impactLogs.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("empty_committee_telemetry_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "No Telemetry Yet",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No Weeks Simulated Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Enact a rule on the Docket tab and advance a week from the Dashboard to compare live simulation outcomes against baseline.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(committeeState.impactLogs, key = { it.id }) { log ->
                            val delta = log.deltaVsBaseline
                            val deltaColor = if (delta >= 0) EmeraldAccent else CrimsonAlert
                            val deltaSign = if (delta >= 0) "+" else ""
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_sim_log_${log.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                            text = "Year ${log.seasonYear} • Week ${log.weekNumber}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = GoldPrimary
                                        )
                                        Surface(
                                            color = deltaColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$deltaSign${String.format(Locale.US, "%.1f", delta)} vs Baseline",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = deltaColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Measured Avg Combined Score: ${String.format(Locale.US, "%.1f", log.measuredAvgCombinedScore)} (Baseline ${String.format(Locale.US, "%.1f", log.sportBaselineCombinedScore)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Close/OT Finishes: ${log.closeOrOvertimeGamesCount} • Weekly Injuries: ${log.injuriesOccurredCount} • Active Laws: ${log.activeRulesCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (log.activeRuleTitles.isNotEmpty()) {
                                        Text(
                                            text = "Enacted: ${log.activeRuleTitles.joinToString(", ")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = log.featuredImpactHeadline,
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
        }
    }
}

@Composable
private fun CommitteeMetricPill(
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
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SportRuleProposalCard(
    proposal: SportRuleAmendment,
    onReview: () -> Unit,
    onEnact: () -> Unit,
    onRepeal: () -> Unit
) {
    val statusColor = when (proposal.status) {
        SportRuleProposalStatus.ENACTED -> EmeraldAccent
        SportRuleProposalStatus.COMMITTEE_RECOMMENDED -> CyanAccent
        SportRuleProposalStatus.TABLED -> AmberWarning
        SportRuleProposalStatus.PROPOSED_DOCKET -> GoldPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (proposal.isEnacted) 1.5.dp else 1.dp,
                color = statusColor.copy(alpha = if (proposal.isEnacted) 0.7f else 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("card_committee_proposal_${proposal.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${proposal.categoryLabel.uppercase(Locale.US)} • ${proposal.sponsorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = proposal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = proposal.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = proposal.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Visible Simulation Engine Effect Callout
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SIMULATION ENGINE EFFECT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        fontSize = 10.sp
                    )
                    Text(
                        text = proposal.simulationEffectSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    if (proposal.simImpactTags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            items(proposal.simImpactTags) { tag ->
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (proposal.committeeReviewNote.isNotBlank()) {
                Text(
                    text = proposal.committeeReviewNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!proposal.isEnacted) {
                    OutlinedButton(
                        onClick = onReview,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_review_proposal_${proposal.id}")
                    ) {
                        Icon(Icons.Default.HowToVote, contentDescription = "Committee Review", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Review", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onEnact,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_enact_proposal_${proposal.id}")
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = "Enact Rule", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enact Law", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                } else {
                    OutlinedButton(
                        onClick = onRepeal,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("btn_repeal_proposal_${proposal.id}")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Repeal Rule", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Repeal Active Simulation Rule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OffseasonSponsorRuleVotesCard(
    votes: List<OffseasonSponsorRuleVoteItem>,
    isOffSeason: Boolean,
    onVote: (ruleId: String, approve: Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.6.dp, GoldPrimary, RoundedCornerShape(16.dp))
            .testTag("card_committee_sponsor_rule_votes"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
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
                        text = if (isOffSeason) "OFFSEASON RULES COMMITTEE VOTE" else "QUEUED FOR OFFSEASON COMMITTEE VOTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Sponsor Rule Covenant Ballot (${votes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = GoldPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${votes.size} Pending",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            votes.forEach { vote ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_committee_sponsor_vote_${vote.ruleId}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${vote.sponsorName} • ${vote.slotType.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(vote.slotType.accentHex)
                        )
                        Text(
                            text = vote.ruleName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (vote.summary.isNotBlank()) {
                            Text(
                                text = vote.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Pass: +\$${vote.bonusPayout}M/yr Revenue • -${vote.fanPenalty} Fan • -${vote.unionPenalty} Union  |  Reject: 50% Payout Slash",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onVote(vote.ruleId, false) },
                                border = BorderStroke(1.2.dp, CrimsonAlert),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_committee_reject_sponsor_rule_${vote.ruleId}")
                            ) {
                                Text("Vote Down", fontWeight = FontWeight.Black)
                            }
                            Button(
                                onClick = { onVote(vote.ruleId, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_committee_pass_sponsor_rule_${vote.ruleId}")
                            ) {
                                Text("Pass Law", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
