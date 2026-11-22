package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun TradeDeskScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closeTradeDesk()
    }

    val prefill = uiState.prefilledTradeMachineSetup
    var selectedDeskTab by remember(prefill) { mutableStateOf(if (prefill != null) 1 else 0) } // 0: Review, 1: Machine, 2: Pick Tracker, 3: Ledger
    var showRejectDialogForProposal by remember { mutableStateOf<TradeProposal?>(null) }
    var rejectionReason by remember { mutableStateOf("Asset disparity violates league competitive parity standards.") }

    val tradeRules = uiState.ruleBook.tradeRules
    val isDeadlineWeek = tradeRules.isTradeDeadlineWeek(uiState.currentWeek, uiState.seasonPhase)
    val isTradeOpen = tradeRules.isTradeWindowOpen(uiState.currentWeek, uiState.seasonPhase)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("trade_desk_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "COMMISSIONER TRADE DESK",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            if (uiState.pendingTrades.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CrimsonAlert,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${uiState.pendingTrades.size} NEW",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isTradeOpen) "Season ${uiState.currentYear} • Regular Season Week ${uiState.currentWeek} • Deadline: Week ${tradeRules.tradeDeadlineWeek}"
                            else "Season ${uiState.currentYear} • Trade Window Closed (Deadline Week ${tradeRules.tradeDeadlineWeek} Passed)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDeadlineWeek) CrimsonAlert else if (isTradeOpen) GoldPrimary else TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeTradeDesk() },
                        modifier = Modifier
                            .testTag("btn_close_trade_desk")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to League Dashboard"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.openTradeBlock() },
                        modifier = Modifier
                            .testTag("btn_open_trade_block_from_desk")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Trade Block",
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Trade Block",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
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
            // Deadline Status Alert Strip
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = when {
                    isDeadlineWeek -> CrimsonAlert.copy(alpha = 0.18f)
                    isTradeOpen -> AmberWarning.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDeadlineWeek) CrimsonAlert else if (isTradeOpen) AmberWarning.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isDeadlineWeek) Icons.Default.Warning else if (isTradeOpen) Icons.Default.AccessTime else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isDeadlineWeek) CrimsonAlert else if (isTradeOpen) AmberWarning else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isDeadlineWeek -> "TRADE DEADLINE WEEK: All deals must be ratified before Week ${tradeRules.tradeDeadlineWeek + 1}!"
                                isTradeOpen -> "Trading Window Active: ${tradeRules.tradeDeadlineWeek - uiState.currentWeek} week(s) until deadline."
                                else -> "Trade Window Closed: Trades prohibited until the off-season summit."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeadlineWeek) CrimsonAlert else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Tab Navigation
            ScrollableTabRow(
                selectedTabIndex = selectedDeskTab,
                edgePadding = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tab_row_trade_desk"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedDeskTab == 0,
                    onClick = { selectedDeskTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Review")
                            if (uiState.pendingTrades.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("(${uiState.pendingTrades.size})", color = AmberWarning, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_review_desk")
                )
                Tab(
                    selected = selectedDeskTab == 1,
                    onClick = { selectedDeskTab = 1 },
                    text = { Text("Trade Machine") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_trade_machine")
                )
                Tab(
                    selected = selectedDeskTab == 2,
                    onClick = { selectedDeskTab = 2 },
                    text = { Text("Pick Tracker (${uiState.tradedPickLedger.size})") },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_pick_tracker")
                )
                Tab(
                    selected = selectedDeskTab == 3,
                    onClick = { selectedDeskTab = 3 },
                    text = { Text("Ledger (${uiState.completedTrades.size})") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_trade_ledger")
                )
            }

            // Tab Content
            when (selectedDeskTab) {
                0 -> ReviewDeskContent(
                    pendingTrades = uiState.pendingTrades,
                    isTradeOpen = isTradeOpen,
                    onApprove = { viewModel.approveTrade(it) },
                    onInitiateReject = { showRejectDialogForProposal = it }
                )
                1 -> TradeMachineContent(
                    franchises = uiState.franchises,
                    tradeRules = tradeRules,
                    isTradeOpen = isTradeOpen,
                    prefilledSetup = prefill,
                    onExecuteTrade = { teamAId, teamBId, pA, picksA, pB, picksB ->
                        viewModel.proposeAndExecuteTrade(teamAId, teamBId, pA, picksA, pB, picksB)
                    }
                )
                2 -> DraftPickTrackerContent(
                    franchises = uiState.franchises,
                    tradedPickLedger = uiState.tradedPickLedger,
                    currentYear = uiState.currentYear
                )
                3 -> TradeLedgerContent(
                    completedTrades = uiState.completedTrades
                )
            }
        }
    }

    // Reject / Veto Dialog
    if (showRejectDialogForProposal != null) {
        val proposal = showRejectDialogForProposal!!
        AlertDialog(
            onDismissRequest = { showRejectDialogForProposal = null },
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = CrimsonAlert) },
            title = { Text("Commissioner Action: Reject or Veto") },
            text = {
                Column {
                    Text(
                        text = "Specify rationale for rejecting trade between ${proposal.teamAName} and ${proposal.teamBName}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_veto_reason"),
                        label = { Text("Reason for Rejection / Veto") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectTrade(proposal.id, isVeto = true, reason = rejectionReason)
                        showRejectDialogForProposal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                    modifier = Modifier.testTag("btn_confirm_veto")
                ) {
                    Text("Commissioner Veto", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.rejectTrade(proposal.id, isVeto = false, reason = rejectionReason)
                        showRejectDialogForProposal = null
                    },
                    modifier = Modifier.testTag("btn_confirm_reject")
                ) {
                    Text("Decline Offer")
                }
            }
        )
    }
}

@Composable
fun ReviewDeskContent(
    pendingTrades: List<TradeProposal>,
    isTradeOpen: Boolean,
    onApprove: (String) -> Unit,
    onInitiateReject: (TradeProposal) -> Unit
) {
    if (pendingTrades.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("empty_pending_trades"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Commissioner Trade Desk Clear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No pending trades awaiting review. Simulate regular season weeks or use the Trade Machine to propose new player swaps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("list_pending_trades"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pendingTrades) { proposal ->
                TradeProposalCard(
                    proposal = proposal,
                    isTradeOpen = isTradeOpen,
                    onApprove = { onApprove(proposal.id) },
                    onReject = { onInitiateReject(proposal) }
                )
            }
        }
    }
}

@Composable
fun TradeProposalCard(
    proposal: TradeProposal,
    isTradeOpen: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("proposal_card_${proposal.id}")
            .border(1.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Teams and Fairness Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "WEEK ${proposal.weekProposed} TRADE PROPOSAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${proposal.teamAName} ⇄ ${proposal.teamBName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Surface(
                    color = if (proposal.fairnessScore >= 75) EmeraldAccent.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Fairness: ${proposal.fairnessScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (proposal.fairnessScore >= 75) EmeraldAccent else AmberWarning,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two Column Asset Swap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Team A Sends
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${proposal.teamAName} Sends",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlueAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        proposal.teamASendsPlayers.forEach { player ->
                            Text(
                                text = "• ${player.name} (${player.position}, ${player.overallRating} OVR, \$${player.salaryMillions}M)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        proposal.teamASendsPicks.forEach { pick ->
                            Text(
                                text = "• ${pick.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trade Value: ${proposal.teamATotalTradeValue} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Team B Sends
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${proposal.teamBName} Sends",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        proposal.teamBSendsPlayers.forEach { player ->
                            Text(
                                text = "• ${player.name} (${player.position}, ${player.overallRating} OVR, \$${player.salaryMillions}M)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        proposal.teamBSendsPicks.forEach { pick ->
                            Text(
                                text = "• ${pick.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trade Value: ${proposal.teamBTotalTradeValue} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rationale
            Text(
                text = "Strategic Motivation: ${proposal.rationaleTeamA}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_reject_trade_${proposal.id}")
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject / Veto")
                }

                Button(
                    onClick = onApprove,
                    enabled = isTradeOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_approve_trade_${proposal.id}")
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTradeOpen) "Ratify Trade" else "Deadline Passed", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TradeMachineContent(
    franchises: List<Franchise>,
    tradeRules: TradeRules,
    isTradeOpen: Boolean,
    prefilledSetup: com.example.viewmodel.PrefilledTradeSetup? = null,
    onExecuteTrade: (String, String, List<RosterPlayer>, List<DraftPick>, List<RosterPlayer>, List<DraftPick>) -> Pair<Boolean, String>
) {
    if (franchises.size < 2) return

    var teamAIndex by remember(prefilledSetup) {
        mutableStateOf(
            prefilledSetup?.let { s -> franchises.indexOfFirst { it.id == s.teamAId }.takeIf { it >= 0 } } ?: 0
        )
    }
    var teamBIndex by remember(prefilledSetup) {
        mutableStateOf(
            prefilledSetup?.let { s -> franchises.indexOfFirst { it.id == s.teamBId }.takeIf { it >= 0 } }
                ?: if (teamAIndex == 1) 0 else 1
        )
    }

    val teamA = franchises[teamAIndex]
    val teamB = franchises[teamBIndex]

    val selectedPlayersA = remember(prefilledSetup) {
        mutableStateListOf<RosterPlayer>().apply {
            val pid = prefilledSetup?.playerAId
            if (pid != null) {
                teamA.roster.find { it.id == pid }?.let { add(it) }
            }
        }
    }
    val selectedPicksA = remember(prefilledSetup) { mutableStateListOf<DraftPick>() }
    val selectedPlayersB = remember(prefilledSetup) {
        mutableStateListOf<RosterPlayer>().apply {
            val pid = prefilledSetup?.playerBId
            if (pid != null) {
                teamB.roster.find { it.id == pid }?.let { add(it) }
            }
        }
    }
    val selectedPicksB = remember(prefilledSetup) { mutableStateListOf<DraftPick>() }

    var executionFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val valA = selectedPlayersA.sumOf { it.tradeValue } + selectedPicksA.sumOf { it.tradeValue }
    val valB = selectedPlayersB.sumOf { it.tradeValue } + selectedPicksB.sumOf { it.tradeValue }
    val fairness = TradeEngine.calculateFairnessScore(valA, valB)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("trade_machine_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Trade Machine Header & Live Fairness Meter
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "COMMISSIONER TRADE MACHINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Construct & Evaluate Custom Swaps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = if (fairness >= 75) EmeraldAccent else AmberWarning,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Fairness: $fairness%",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (fairness / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (fairness >= 75) EmeraldAccent else AmberWarning,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Team A Value: $valA pts • Team B Value: $valB pts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Team Pickers & Asset Selectors
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Team A Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text("Select Franchise A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedA by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expandedA = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_select_team_a"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(teamA.nickname, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(
                        expanded = expandedA,
                        onDismissRequest = { expandedA = false }
                    ) {
                        franchises.forEachIndexed { index, franchise ->
                            if (index != teamBIndex) {
                                DropdownMenuItem(
                                    text = { Text(franchise.teamFullName) },
                                    onClick = {
                                        teamAIndex = index
                                        selectedPlayersA.clear()
                                        selectedPicksA.clear()
                                        expandedA = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Team B Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text("Select Franchise B", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedB by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expandedB = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_select_team_b"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(teamB.nickname, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(
                        expanded = expandedB,
                        onDismissRequest = { expandedB = false }
                    ) {
                        franchises.forEachIndexed { index, franchise ->
                            if (index != teamAIndex) {
                                DropdownMenuItem(
                                    text = { Text(franchise.teamFullName) },
                                    onClick = {
                                        teamBIndex = index
                                        selectedPlayersB.clear()
                                        selectedPicksB.clear()
                                        expandedB = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Side-by-Side Asset Checklists
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Team A Assets
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${teamA.nickname} Assets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RoyalBlueAccent)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Players (${teamA.roster.size})", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                        teamA.roster.forEach { player ->
                            val isChecked = player in selectedPlayersA
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedPlayersA.remove(player) else selectedPlayersA.add(player)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedPlayersA.add(player) else selectedPlayersA.remove(player)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${player.name} (${player.position})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        if (teamA.isPlayerOnTradeBlock(player.id)) {
                                            Text("• BLOCK", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                                        }
                                    }
                                    Text("${player.overallRating} OVR • \$${player.salaryMillions}M", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (tradeRules.allowDraftPickTrading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Draft Picks (${teamA.draftPicks.size})", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                            teamA.draftPicks.forEach { pick ->
                                val isChecked = pick in selectedPicksA
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedPicksA.remove(pick) else selectedPicksA.add(pick)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (it) selectedPicksA.add(pick) else selectedPicksA.remove(pick)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = pick.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (pick.isTraded) CyanAccent else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (pick.isTraded) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (pick.isTraded) {
                                            Text(
                                                text = "Acquired from ${pick.acquiredFromFranchiseName ?: pick.originalFranchiseName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = GoldPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Team B Assets
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${teamB.nickname} Assets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeonAmber)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Players (${teamB.roster.size})", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                        teamB.roster.forEach { player ->
                            val isChecked = player in selectedPlayersB
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedPlayersB.remove(player) else selectedPlayersB.add(player)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedPlayersB.add(player) else selectedPlayersB.remove(player)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${player.name} (${player.position})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        if (teamB.isPlayerOnTradeBlock(player.id)) {
                                            Text("• BLOCK", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                                        } else if (teamA.isSeekingPlayer(player.id)) {
                                            Text("• TARGET", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = CyanAccent)
                                        }
                                    }
                                    Text("${player.overallRating} OVR • \$${player.salaryMillions}M", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (tradeRules.allowDraftPickTrading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Draft Picks (${teamB.draftPicks.size})", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                            teamB.draftPicks.forEach { pick ->
                                val isChecked = pick in selectedPicksB
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedPicksB.remove(pick) else selectedPicksB.add(pick)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (it) selectedPicksB.add(pick) else selectedPicksB.remove(pick)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = pick.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (pick.isTraded) CyanAccent else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (pick.isTraded) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (pick.isTraded) {
                                            Text(
                                                text = "Acquired from ${pick.acquiredFromFranchiseName ?: pick.originalFranchiseName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = GoldPrimary
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

        // Feedback Banner
        if (executionFeedback != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (executionFeedback!!.first) EmeraldAccent.copy(alpha = 0.2f) else CrimsonAlert.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = executionFeedback!!.second,
                        color = if (executionFeedback!!.first) EmeraldAccent else CrimsonAlert,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Execute Custom Trade Button
        item {
            Button(
                onClick = {
                    val result = onExecuteTrade(
                        teamA.id,
                        teamB.id,
                        selectedPlayersA.toList(),
                        selectedPicksA.toList(),
                        selectedPlayersB.toList(),
                        selectedPicksB.toList()
                    )
                    executionFeedback = result
                    if (result.first) {
                        selectedPlayersA.clear()
                        selectedPicksA.clear()
                        selectedPlayersB.clear()
                        selectedPicksB.clear()
                    }
                },
                enabled = isTradeOpen && (selectedPlayersA.isNotEmpty() || selectedPicksA.isNotEmpty()) && (selectedPlayersB.isNotEmpty() || selectedPicksB.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_execute_custom_trade")
                    .minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Handshake, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTradeOpen) "Propose & Ratify Trade" else "Trade Deadline Passed",
                    color = Color.Black,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun DraftPickTrackerContent(
    franchises: List<Franchise>,
    tradedPickLedger: List<TradedPickLedgerEntry>,
    currentYear: Int
) {
    var filterMode by remember { mutableIntStateOf(0) } // 0: Traded Picks Ledger, 1: All Team Cupboards

    val activeTradedCount = tradedPickLedger.count { it.status == TradedPickStatus.PENDING_CONVEYANCE }
    val exercisedCount = tradedPickLedger.count { it.status == TradedPickStatus.EXERCISED_IN_DRAFT }
    val deferredCount = tradedPickLedger.count { it.status == TradedPickStatus.PROTECTION_DEFERRED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("list_traded_picks_tracker"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_pick_tracker_summary"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LEAGUE DRAFT CAPITAL TRACKER",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Traded Picks, Protections & Draft Conveyance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Every pick swapped through the Trade Desk updates actual Round 1 & Round 2 Rookie Draft ownership. Top-3 protected picks retain if they land in the lottery top 3 and roll over unprotected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PickTrackerStatPill("Active Traded", "$activeTradedCount", GoldPrimary, Modifier.weight(1f))
                        PickTrackerStatPill("Exercised", "$exercisedCount", EmeraldAccent, Modifier.weight(1f))
                        PickTrackerStatPill("Deferred", "$deferredCount", CyanAccent, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterMode == 0,
                            onClick = { filterMode = 0 },
                            label = { Text("Traded Ledger (${tradedPickLedger.size})") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_pick_tracker_ledger")
                        )
                        FilterChip(
                            selected = filterMode == 1,
                            onClick = { filterMode = 1 },
                            label = { Text("Team Cupboards (${franchises.size})") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_pick_tracker_cupboards")
                        )
                    }
                }
            }
        }

        if (filterMode == 0) {
            if (tradedPickLedger.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_traded_picks"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Traded Draft Picks Yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Approve a proposal on the Review tab or swap picks in the Trade Machine. Traded picks immediately bind to the Off-Season Rookie Draft.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(tradedPickLedger, key = { it.pickId }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_traded_pick_${entry.pickId}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (entry.round == 1) GoldPrimary else RoyalBlueAccent.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${entry.year} R${entry.round}",
                                            color = if (entry.round == 1) Color.Black else RoyalBlueAccent,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (entry.projectedSlot != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = CyanAccent.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (entry.status == TradedPickStatus.EXERCISED_IN_DRAFT) "Pick #${entry.projectedSlot}" else "Proj. #${entry.projectedSlot}",
                                                color = CyanAccent,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    color = Color(entry.status.badgeColorHex).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = entry.status.label,
                                        color = Color(entry.status.badgeColorHex),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${entry.originalFranchiseName} ➔ ${entry.toFranchiseName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val weekText = entry.weekTraded?.let { "Traded Week $it • " } ?: ""
                            Text(
                                text = "${weekText}Terms: ${entry.protectionDescription}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (entry.outcomeSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.outcomeSummary,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            items(franchises, key = { it.id }) { team ->
                val sortedPicks = team.draftPicks.sortedWith(compareBy<DraftPick> { it.year }.thenBy { it.round })
                val acquiredCount = sortedPicks.count { it.isTraded }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_cupboard_${team.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = team.teamFullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${sortedPicks.size} Picks ($acquiredCount via Trade)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (acquiredCount > 0) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        sortedPicks.forEach { pick ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${pick.year} R${pick.round} (${if (pick.isTraded) "via ${pick.originalFranchiseName}" else "Own"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (pick.isTraded) CyanAccent else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (pick.isTraded) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (pick.year == currentYear && pick.round == 1) "Next Draft • ${pick.protectionDescription}" else pick.protectionDescription,
                                    style = MaterialTheme.typography.labelSmall,
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

@Composable
private fun PickTrackerStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TradeLedgerContent(
    completedTrades: List<TradeProposal>
) {
    if (completedTrades.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("empty_trade_ledger"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No completed transactions in the ledger yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("list_completed_trades"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(completedTrades) { trade ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${trade.teamAName} ⇄ ${trade.teamBName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(trade.status.badgeColorHex).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = trade.status.label,
                                    color = Color(trade.status.badgeColorHex),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trade.commissionerReviewNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (trade.teamASendsPicks.isNotEmpty() || trade.teamBSendsPicks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val allPicksText = buildList {
                                if (trade.teamASendsPicks.isNotEmpty()) {
                                    add("${trade.teamAName} sent ${trade.teamASendsPicks.joinToString { "${it.year} R${it.round} (${it.originalFranchiseName})" }}")
                                }
                                if (trade.teamBSendsPicks.isNotEmpty()) {
                                    add("${trade.teamBName} sent ${trade.teamBSendsPicks.joinToString { "${it.year} R${it.round} (${it.originalFranchiseName})" }}")
                                }
                            }.joinToString(" • ")
                            Text(
                                text = "Draft Capital: $allPicksText",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
