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
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeBlockScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.closeTradeBlock() }

    var selectedTab by remember { mutableStateOf(0) } // 0: Team Block, 1: League Marketplace, 2: Smart Matches
    var showSeekBrowserDialog by remember { mutableStateOf(false) }
    var editingListingPlayer by remember { mutableStateOf<Pair<RosterPlayer, TradeBlockListing>?>(null) }

    val selectedFranchise = remember(uiState.franchises, uiState.selectedTradeBlockFranchiseId) {
        uiState.franchises.find { it.id == uiState.selectedTradeBlockFranchiseId }
            ?: uiState.franchises.firstOrNull()
    }

    val statLabels = remember(uiState.sportType) { RecordBookEngine.getStatLabels(uiState.sportType) }
    val positions = remember(uiState.sportType) { TradeEngine.getPositionsForSport(uiState.sportType) }
    val tradeRules = uiState.ruleBook.tradeRules
    val isTradeOpen = tradeRules.isTradeWindowOpen(uiState.currentWeek, uiState.seasonPhase)
    val isDeadlineWeek = tradeRules.isTradeDeadlineWeek(uiState.currentWeek, uiState.seasonPhase)

    val totalLeagueListings = remember(uiState.franchises) {
        uiState.franchises.sumOf { it.tradeBlockListings.size }
    }
    val totalLeagueSeeking = remember(uiState.franchises) {
        uiState.franchises.sumOf { it.seekingPlayerIds.size }
    }

    val smartMatches = remember(uiState.franchises, uiState.seasonPlayerStats, tradeRules) {
        TradeEngine.buildMatchmakingRecommendations(
            franchises = uiState.franchises,
            seasonStats = uiState.seasonPlayerStats,
            tradeRules = tradeRules
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("trade_block_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TRADE BLOCK & TARGET WISHLIST",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "League Trade Block",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeTradeBlock() },
                        modifier = Modifier
                            .testTag("btn_close_trade_block")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.openTradeDesk(true) },
                        modifier = Modifier
                            .testTag("btn_jump_to_trade_desk")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Open Trade Desk",
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Trade Desk",
                            color = AmberWarning,
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
            // Market Status Strip
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = when {
                    isDeadlineWeek -> CrimsonAlert.copy(alpha = 0.16f)
                    isTradeOpen -> GoldPrimary.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDeadlineWeek) CrimsonAlert else if (isTradeOpen) GoldPrimary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$totalLeagueListings On Block • $totalLeagueSeeking Targets Active",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isTradeOpen) "Window Open (W${uiState.currentWeek}/${tradeRules.tradeDeadlineWeek})" else "Deadline Passed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isDeadlineWeek) CrimsonAlert else if (isTradeOpen) EmeraldAccent else TextSecondary
                    )
                }
            }

            // Sub-Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tab_row_trade_block"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Team Block", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_team_trade_block")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("League ($totalLeagueListings)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_league_trade_block")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Matches (${smartMatches.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_block_matches")
                )
            }

            when (selectedTab) {
                0 -> TeamTradeBlockTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    selectedFranchise = selectedFranchise,
                    positions = positions,
                    statLabels = statLabels,
                    isTradeOpen = isTradeOpen,
                    onOpenSeekBrowser = { showSeekBrowserDialog = true },
                    onEditListing = { player, listing -> editingListingPlayer = player to listing }
                )
                1 -> LeagueMarketplaceTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    positions = positions,
                    statLabels = statLabels,
                    isTradeOpen = isTradeOpen
                )
                2 -> SmartMatchesTab(
                    matches = smartMatches,
                    uiState = uiState,
                    viewModel = viewModel,
                    statLabels = statLabels,
                    isTradeOpen = isTradeOpen
                )
            }
        }
    }

    // Dialog to Browse Rival Players Across League & Add to "Seeking in Return"
    if (showSeekBrowserDialog && selectedFranchise != null) {
        SeekRivalPlayersDialog(
            seekingFranchise = selectedFranchise,
            allFranchises = uiState.franchises,
            positions = positions,
            seasonStats = uiState.seasonPlayerStats,
            careerStats = uiState.careerPlayerStats,
            statLabels = statLabels,
            onToggleSeek = { targetId ->
                viewModel.toggleSeekingPlayer(selectedFranchise.id, targetId)
            },
            onDismiss = { showSeekBrowserDialog = false }
        )
    }

    // Dialog to Edit Asking Priority & Notes for a Listed Player
    if (editingListingPlayer != null && selectedFranchise != null) {
        val (player, currentListing) = editingListingPlayer!!
        var chosenPriority by remember(currentListing) { mutableStateOf(currentListing.askingPriority) }
        var customNote by remember(currentListing) { mutableStateOf(currentListing.askingNote) }

        AlertDialog(
            onDismissRequest = { editingListingPlayer = null },
            icon = { Icon(Icons.Default.Sell, contentDescription = null, tint = GoldPrimary) },
            title = { Text("Asking Package • ${player.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${player.position} • ${player.overallRating} OVR • \$${player.salaryMillions}M/yr",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select Return Priority:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TradeAskingPriority.values().forEach { priority ->
                        val isSel = chosenPriority == priority
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chosenPriority = priority
                                    customNote = priority.description
                                },
                            color = if (isSel) Color(priority.badgeColorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSel) Color(priority.badgeColorHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = {
                                        chosenPriority = priority
                                        customNote = priority.description
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(priority.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(priority.description, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = { Text("Front Office Asking Note") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_trade_block_note"),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTradeBlockListingDetails(
                            franchiseId = selectedFranchise.id,
                            playerId = player.id,
                            askingPriority = chosenPriority,
                            askingNote = customNote
                        )
                        editingListingPlayer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier.testTag("btn_save_trade_block_asking")
                ) {
                    Text("Save Terms", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingListingPlayer = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TeamTradeBlockTab(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    selectedFranchise: Franchise?,
    positions: List<String>,
    statLabels: SportStatLabels,
    isTradeOpen: Boolean,
    onOpenSeekBrowser: () -> Unit,
    onEditListing: (RosterPlayer, TradeBlockListing) -> Unit
) {
    val allPlayersMap = remember(uiState.franchises) {
        uiState.franchises.flatMap { team ->
            team.roster.map { p -> p.id to (p to team) }
        }.toMap()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("team_trade_block_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Franchise Selector Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trade_block_team_selector")
            ) {
                items(uiState.franchises, key = { it.id }) { team ->
                    val isSelected = selectedFranchise?.id == team.id
                    val listedCount = team.tradeBlockListings.size
                    val seekingCount = team.seekingPlayerIds.size

                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { viewModel.selectTradeBlockFranchise(team.id) }
                            .testTag("chip_block_team_${team.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(team.colorHex))
                            )
                            Text(
                                text = team.nickname,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (listedCount > 0 || seekingCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$listedCount/$seekingCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedFranchise != null) {
            // Franchise Front Office Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trade_block_franchise_summary")
                        .border(1.2.dp, Color(selectedFranchise.colorHex).copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${selectedFranchise.teamFullName} (${selectedFranchise.wins}-${selectedFranchise.losses})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cap Payroll: \$${selectedFranchise.annualPayrollMillions}M / \$${uiState.unionAgreement.salaryCapMillions}M • ${selectedFranchise.effectiveLineupRating} EFF OVR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.autoGenerateTradeBlockForFranchise(selectedFranchise.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .testTag("btn_auto_trade_block")
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Set", color = GoldPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Section 1: Players Willing to Trade (On the Trade Block)
            val listedPlayers = selectedFranchise.tradeBlockListings.mapNotNull { listing ->
                selectedFranchise.roster.find { it.id == listing.playerId }?.let { it to listing }
            }

            item {
                SectionHeader(
                    title = "On the Trade Block (${listedPlayers.size})",
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (listedPlayers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_team_trade_block"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No players currently listed by ${selectedFranchise.nickname}. Select players from the Active Roster below to place them on the Trade Block.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(listedPlayers, key = { (p, _) -> "block_${p.id}" }) { (player, listing) ->
                    val priorityColor = Color(listing.askingPriority.badgeColorHex)
                    TradeBlockPlayerStatsCard(
                        player = player,
                        seasonStats = uiState.seasonPlayerStats[player.id],
                        careerStats = uiState.careerPlayerStats[player.id],
                        statLabels = statLabels,
                        accentColor = priorityColor,
                        headerBadgeText = listing.askingPriority.label.uppercase(),
                        askingNote = listing.askingNote,
                        cardTag = "listed_player_card_${player.id}",
                        actionsContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onEditListing(player, listing) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_edit_asking_${player.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Asking Terms", style = MaterialTheme.typography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.togglePlayerOnTradeBlock(selectedFranchise.id, player.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_remove_from_block_${player.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimsonAlert)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pull Off Block", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                }
            }

            // Section 2: Players Seeking in Return & Positional Targets
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader(title = "Seeking in Return (${selectedFranchise.seekingPlayerIds.size})")
                    Button(
                        onClick = onOpenSeekBrowser,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .testTag("btn_open_seek_browser")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Seek Players", color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Target Positions Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target Positions Wanted:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(positions) { pos ->
                            val isWanted = pos in selectedFranchise.seekingPositions
                            FilterChip(
                                selected = isWanted,
                                onClick = { viewModel.toggleSeekingPosition(selectedFranchise.id, pos) },
                                label = { Text(pos, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("chip_seek_pos_${pos.lowercase()}")
                            )
                        }
                    }
                }
            }

            val soughtTargets = selectedFranchise.seekingPlayerIds.mapNotNull { allPlayersMap[it] }
            if (soughtTargets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_seeking_targets"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No rival players targeted yet. Tap 'Seek Players' to scout league rosters & stats and mark players wanted in return.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(soughtTargets, key = { (p, _) -> "seek_${p.id}" }) { (targetPlayer, owningTeam) ->
                    val isOnRivalBlock = owningTeam.isPlayerOnTradeBlock(targetPlayer.id)
                    val firstListedId = selectedFranchise.tradeBlockListings.firstOrNull()?.playerId

                    TradeBlockPlayerStatsCard(
                        player = targetPlayer,
                        seasonStats = uiState.seasonPlayerStats[targetPlayer.id],
                        careerStats = uiState.careerPlayerStats[targetPlayer.id],
                        statLabels = statLabels,
                        accentColor = if (isOnRivalBlock) EmeraldAccent else CyanAccent,
                        headerBadgeText = if (isOnRivalBlock) "${owningTeam.nickname.uppercase()} • ON BLOCK" else owningTeam.nickname.uppercase(),
                        askingNote = if (isOnRivalBlock) "Mutual opportunity: ${owningTeam.nickname} has placed ${targetPlayer.name} on their trade block!" else "Targeted rival player (${owningTeam.teamFullName})",
                        cardTag = "seeking_player_card_${targetPlayer.id}",
                        actionsContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.openTradeMachineWithPlayers(
                                            teamAId = selectedFranchise.id,
                                            teamBId = owningTeam.id,
                                            playerAId = firstListedId,
                                            playerBId = targetPlayer.id
                                        )
                                    },
                                    enabled = isTradeOpen,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_offer_for_seeking_${targetPlayer.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Build Offer", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.toggleSeekingPlayer(selectedFranchise.id, targetPlayer.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_remove_seeking_${targetPlayer.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Drop Target", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                }
            }

            // Section 3: Active Roster Players Available to Add to Trade Block
            val nonListedRoster = selectedFranchise.roster.filterNot { selectedFranchise.isPlayerOnTradeBlock(it.id) }
                .sortedWith(compareByDescending<RosterPlayer> { it.isStarter }.thenByDescending { it.overallRating })

            item {
                SectionHeader(
                    title = "Active Roster (${nonListedRoster.size} Off-Block)",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(nonListedRoster, key = { "roster_${it.id}" }) { player ->
                TradeBlockPlayerStatsCard(
                    player = player,
                    seasonStats = uiState.seasonPlayerStats[player.id],
                    careerStats = uiState.careerPlayerStats[player.id],
                    statLabels = statLabels,
                    accentColor = MaterialTheme.colorScheme.outline,
                    headerBadgeText = if (player.isStarter) "STARTER" else "BENCH",
                    askingNote = null,
                    cardTag = "roster_offblock_card_${player.id}",
                    actionsContent = {
                        OutlinedButton(
                            onClick = {
                                val defaultPriority = if (selectedFranchise.rosterRating >= 81) {
                                    TradeAskingPriority.CONTENDER_UPGRADE
                                } else {
                                    TradeAskingPriority.FUTURE_DRAFT_CAPITAL
                                }
                                viewModel.togglePlayerOnTradeBlock(selectedFranchise.id, player.id, defaultPriority)
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_add_to_block_${player.id}")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Place on Trade Block", color = GoldPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LeagueMarketplaceTab(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    positions: List<String>,
    statLabels: SportStatLabels,
    isTradeOpen: Boolean
) {
    var filterPosition by remember { mutableStateOf<String?>(null) }
    var filterPriority by remember { mutableStateOf<TradeAskingPriority?>(null) }

    val myTeam = remember(uiState.franchises, uiState.selectedTradeBlockFranchiseId) {
        uiState.franchises.find { it.id == uiState.selectedTradeBlockFranchiseId }
            ?: uiState.franchises.firstOrNull()
    }

    val allListings = remember(uiState.franchises, filterPosition, filterPriority) {
        uiState.franchises.flatMap { team ->
            team.tradeBlockListings.mapNotNull { listing ->
                val player = team.roster.find { it.id == listing.playerId } ?: return@mapNotNull null
                val matchesPos = filterPosition == null || player.position == filterPosition
                val matchesPri = filterPriority == null || listing.askingPriority == filterPriority
                if (matchesPos && matchesPri) Triple(team, player, listing) else null
            }
        }.sortedByDescending { (_, player, _) -> player.overallRating }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("league_marketplace_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filterPosition == null,
                            onClick = { filterPosition = null },
                            label = { Text("All Pos") }
                        )
                    }
                    items(positions) { pos ->
                        FilterChip(
                            selected = filterPosition == pos,
                            onClick = { filterPosition = if (filterPosition == pos) null else pos },
                            label = { Text(pos) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filterPriority == null,
                            onClick = { filterPriority = null },
                            label = { Text("All Priorities") }
                        )
                    }
                    items(TradeAskingPriority.values()) { pri ->
                        FilterChip(
                            selected = filterPriority == pri,
                            onClick = { filterPriority = if (filterPriority == pri) null else pri },
                            label = { Text(pri.label) }
                        )
                    }
                }
            }
        }

        if (allListings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_league_trade_block"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No players match the selected filters on the League Trade Block.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(allListings, key = { (team, player, _) -> "mkt_${team.id}_${player.id}" }) { (team, player, listing) ->
                val needsText = if (team.seekingPositions.isNotEmpty()) {
                    "Seeking: ${team.seekingPositions.joinToString("/")} • ${listing.askingNote}"
                } else {
                    listing.askingNote
                }
                val partnerTeam = if (myTeam != null && myTeam.id != team.id) {
                    myTeam
                } else {
                    uiState.franchises.firstOrNull { it.id != team.id }
                }

                TradeBlockPlayerStatsCard(
                    player = player,
                    seasonStats = uiState.seasonPlayerStats[player.id],
                    careerStats = uiState.careerPlayerStats[player.id],
                    statLabels = statLabels,
                    accentColor = Color(listing.askingPriority.badgeColorHex),
                    headerBadgeText = "${team.nickname.uppercase()} • ${listing.askingPriority.label.uppercase()}",
                    askingNote = needsText,
                    cardTag = "marketplace_player_${player.id}",
                    actionsContent = {
                        if (partnerTeam != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isAlreadySeeking = partnerTeam.isSeekingPlayer(player.id)
                                OutlinedButton(
                                    onClick = { viewModel.toggleSeekingPlayer(partnerTeam.id, player.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_market_seek_${player.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (isAlreadySeeking) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAlreadySeeking) "Seeking (${partnerTeam.nickname})" else "Mark Target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.openTradeMachineWithPlayers(
                                            teamAId = partnerTeam.id,
                                            teamBId = team.id,
                                            playerAId = partnerTeam.tradeBlockListings.firstOrNull()?.playerId,
                                            playerBId = player.id
                                        )
                                    },
                                    enabled = isTradeOpen,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_make_offer_${player.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Make Offer", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SmartMatchesTab(
    matches: List<TradeBlockMatch>,
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    statLabels: SportStatLabels,
    isTradeOpen: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_matches_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (matches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_smart_matches"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "List players on teams' Trade Blocks and add targets to 'Seeking in Return' to unlock live compatibility matches.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(matches, key = { it.id }) { match ->
                val accent = if (match.isMutualInterest) EmeraldAccent else GoldPrimary
                val statA = uiState.seasonPlayerStats[match.teamAOfferedPlayer.id]
                val statB = uiState.seasonPlayerStats[match.teamBTargetPlayer.id]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_match_card_${match.id}")
                        .border(1.2.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
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
                                color = accent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (match.isMutualInterest) "MUTUAL BLOCK MATCH" else "TRADE MATCH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = (if (match.fairnessScore >= 75) EmeraldAccent else AmberWarning).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${match.fairnessScore}% Fair",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (match.fairnessScore >= 75) EmeraldAccent else AmberWarning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = (if (match.salaryCapCompliant) EmeraldAccent else CrimsonAlert).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (match.salaryCapCompliant) "Cap OK" else "Add Filler",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (match.salaryCapCompliant) EmeraldAccent else CrimsonAlert,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = match.fitHeadline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Player A summary
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(match.teamA.nickname, style = MaterialTheme.typography.labelSmall, color = RoyalBlueAccent, fontWeight = FontWeight.Black)
                                    Text("${match.teamAOfferedPlayer.name} (${match.teamAOfferedPlayer.position})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${match.teamAOfferedPlayer.overallRating} OVR • \$${match.teamAOfferedPlayer.salaryMillions}M", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                                    Text(
                                        text = if (statA != null && statA.gamesPlayed > 0) "${statA.ppg} ${statLabels.primaryShort} • ${statA.apg} ${statLabels.tertiaryShort}" else "Val ${match.teamAOfferedPlayer.tradeValue} pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Player B summary
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(match.teamB.nickname, style = MaterialTheme.typography.labelSmall, color = AmberWarning, fontWeight = FontWeight.Black)
                                    Text("${match.teamBTargetPlayer.name} (${match.teamBTargetPlayer.position})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${match.teamBTargetPlayer.overallRating} OVR • \$${match.teamBTargetPlayer.salaryMillions}M", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                                    Text(
                                        text = if (statB != null && statB.gamesPlayed > 0) "${statB.ppg} ${statLabels.primaryShort} • ${statB.apg} ${statLabels.tertiaryShort}" else "Val ${match.teamBTargetPlayer.tradeValue} pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.openTradeMachineWithPlayers(
                                    teamAId = match.teamA.id,
                                    teamBId = match.teamB.id,
                                    playerAId = match.teamAOfferedPlayer.id,
                                    playerBId = match.teamBTargetPlayer.id
                                )
                            },
                            enabled = isTradeOpen,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_draft_match_${match.id}")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTradeOpen) "Draft Deal in Trade Machine" else "Trade Window Closed",
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekRivalPlayersDialog(
    seekingFranchise: Franchise,
    allFranchises: List<Franchise>,
    positions: List<String>,
    seasonStats: Map<String, PlayerSeasonStats>,
    careerStats: Map<String, PlayerCareerStats>,
    statLabels: SportStatLabels,
    onToggleSeek: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val rivalTeams = remember(allFranchises, seekingFranchise.id) {
        allFranchises.filter { it.id != seekingFranchise.id }
    }
    var selectedTeamFilterId by remember { mutableStateOf<String?>(null) }
    var selectedPosFilter by remember { mutableStateOf<String?>(null) }

    val candidates = remember(rivalTeams, selectedTeamFilterId, selectedPosFilter) {
        rivalTeams
            .filter { selectedTeamFilterId == null || it.id == selectedTeamFilterId }
            .flatMap { team ->
                team.roster.filter { selectedPosFilter == null || it.position == selectedPosFilter }
                    .map { it to team }
            }
            .sortedWith(
                compareByDescending<Pair<RosterPlayer, Franchise>> { (p, team) -> team.isPlayerOnTradeBlock(p.id) }
                    .thenByDescending { (p, _) -> p.overallRating }
            )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .testTag("dialog_seek_rival_players")
                .border(1.5.dp, CyanAccent, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SCOUT RIVAL ROSTERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Seeking for ${seekingFranchise.nickname}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("btn_close_seek_dialog")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rival Franchise Filter
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTeamFilterId == null,
                            onClick = { selectedTeamFilterId = null },
                            label = { Text("All Rivals") }
                        )
                    }
                    items(rivalTeams) { rival ->
                        FilterChip(
                            selected = selectedTeamFilterId == rival.id,
                            onClick = { selectedTeamFilterId = if (selectedTeamFilterId == rival.id) null else rival.id },
                            label = { Text(rival.nickname) }
                        )
                    }
                }

                // Position Filter
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedPosFilter == null,
                            onClick = { selectedPosFilter = null },
                            label = { Text("All Pos") }
                        )
                    }
                    items(positions) { pos ->
                        FilterChip(
                            selected = selectedPosFilter == pos,
                            onClick = { selectedPosFilter = if (selectedPosFilter == pos) null else pos },
                            label = { Text(pos) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates, key = { (p, _) -> "seek_pick_${p.id}" }) { (player, team) ->
                        val isSeeking = seekingFranchise.isSeekingPlayer(player.id)
                        val isOnBlock = team.isPlayerOnTradeBlock(player.id)
                        val sStat = seasonStats[player.id]
                        val cStat = careerStats[player.id]

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isSeeking) CyanAccent else if (isOnBlock) EmeraldAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "${player.name} (${player.position})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isOnBlock) {
                                            Surface(color = EmeraldAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    text = "ON BLOCK",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = EmeraldAccent,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${team.nickname} • ${player.overallRating} OVR • Age ${player.age} • \$${player.salaryMillions}M",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldPrimary
                                    )
                                    val statsSummary = when {
                                        sStat != null && sStat.gamesPlayed > 0 ->
                                            "${sStat.gamesPlayed}GP: ${sStat.ppg} ${statLabels.primaryShort} / ${sStat.rpg} ${statLabels.secondaryShort} / ${sStat.apg} ${statLabels.tertiaryShort}"
                                        cStat != null && cStat.gamesPlayed > 0 ->
                                            "Career (${cStat.gamesPlayed}GP): ${cStat.careerPpg} ${statLabels.primaryShort} • Peak ${cStat.peakRating} OVR"
                                        else -> "Trade Value ${player.tradeValue} pts • ${player.healthStatusLabel}"
                                    }
                                    Text(
                                        text = statsSummary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onToggleSeek(player.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSeeking) CyanAccent else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .testTag("btn_toggle_seek_${player.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Text(
                                        text = if (isSeeking) "Seeking" else "+ Seek",
                                        color = if (isSeeking) Color.Black else CyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
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
private fun TradeBlockPlayerStatsCard(
    player: RosterPlayer,
    seasonStats: PlayerSeasonStats?,
    careerStats: PlayerCareerStats?,
    statLabels: SportStatLabels,
    accentColor: Color,
    headerBadgeText: String,
    askingNote: String?,
    cardTag: String,
    actionsContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(cardTag)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.position,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = accentColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = headerBadgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            if (player.isInjured) {
                                Surface(
                                    color = CrimsonAlert.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = player.healthStatusLabel.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CrimsonAlert,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${player.archetype} • Age ${player.age} • \$${player.salaryMillions}M (${player.contractYearsRemaining}yr)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (player.isInjured && !player.isMedicallyOut) "${player.effectiveOverallRating} EFF" else "${player.overallRating} OVR",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                    Text(
                        text = "Val ${player.tradeValue} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            // Integrated Live Season / Career Stats Strip
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (seasonStats != null && seasonStats.gamesPlayed > 0) {
                        StatBadgeCell(label = statLabels.primaryShort, value = "${seasonStats.ppg}", accent = GoldPrimary)
                        StatBadgeCell(label = statLabels.secondaryShort, value = "${seasonStats.rpg}", accent = CyanAccent)
                        StatBadgeCell(label = statLabels.tertiaryShort, value = "${seasonStats.apg}", accent = EmeraldAccent)
                        StatBadgeCell(label = statLabels.defenseShort, value = "${seasonStats.spg}", accent = RoyalBlueAccent)
                        StatBadgeCell(
                            label = if (seasonStats.weeklyHonorsCount > 0) "POTW" else "GP",
                            value = if (seasonStats.weeklyHonorsCount > 0) "${seasonStats.weeklyHonorsCount}x" else "${seasonStats.gamesPlayed}",
                            accent = if (seasonStats.weeklyHonorsCount > 0) GoldPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    } else if (careerStats != null && careerStats.gamesPlayed > 0) {
                        StatBadgeCell(label = "CAREER", value = "${careerStats.careerPpg} ${statLabels.primaryShort}", accent = GoldPrimary)
                        StatBadgeCell(label = "SEASONS", value = "${careerStats.seasonsPlayed}", accent = CyanAccent)
                        StatBadgeCell(label = "GAMES", value = "${careerStats.gamesPlayed}", accent = EmeraldAccent)
                        StatBadgeCell(label = "BEST GM", value = "${careerStats.bestGamePoints}", accent = RoyalBlueAccent)
                    } else {
                        Text(
                            text = "Season stats update automatically once regular season games are simulated.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!askingNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = askingNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            actionsContent()
        }
    }
}

@Composable
private fun StatBadgeCell(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = accent
        )
    }
}
