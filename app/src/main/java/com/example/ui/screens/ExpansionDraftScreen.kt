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
import com.example.data.InitialLeagueData
import com.example.model.ExpansionDraftPlayer
import com.example.model.ExpansionDraftSourceType
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

private enum class DraftPoolSourceFilter(val label: String) {
    ALL("All Eligible"),
    EXISTING_ROSTERS("Existing Rosters"),
    FREE_AGENCY("Free Agency Pool")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpansionDraftScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val session = uiState.activeExpansionDraftSession ?: return

    var selectedPositionFilter by remember { mutableStateOf("ALL") }
    var selectedSourceFilter by remember { mutableStateOf(DraftPoolSourceFilter.ALL) }
    var selectedTeamFilterId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedViewTab by remember { mutableStateOf(0) } // 0: Available Pool, 1: Drafted Roster
    var showCancelDialog by remember { mutableStateOf(false) }

    BackHandler {
        showCancelDialog = true
    }

    val (assignedConf, assignedDiv) = remember(session.targetCity.name) {
        InitialLeagueData.getGeographicAlignment(session.targetCity.name)
    }

    val rosterEligibleCount = remember(session.availablePool) {
        session.availablePool.count { it.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER }
    }
    val freeAgentEligibleCount = remember(session.availablePool) {
        session.availablePool.count { it.sourceType == ExpansionDraftSourceType.FREE_AGENCY_POOL }
    }

    val allPositions = remember(session.availablePool, session.draftedPlayers) {
        val set = (session.availablePool.map { it.position } + session.draftedPlayers.map { it.position }).toSet()
        listOf("ALL") + set.sorted()
    }

    val filteredPool = remember(
        session.availablePool,
        selectedSourceFilter,
        selectedTeamFilterId,
        selectedPositionFilter,
        searchQuery
    ) {
        session.availablePool.filter { player ->
            val matchesSource = when (selectedSourceFilter) {
                DraftPoolSourceFilter.ALL -> true
                DraftPoolSourceFilter.EXISTING_ROSTERS -> player.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER
                DraftPoolSourceFilter.FREE_AGENCY -> player.sourceType == ExpansionDraftSourceType.FREE_AGENCY_POOL
            }
            val matchesTeam = if (selectedSourceFilter == DraftPoolSourceFilter.EXISTING_ROSTERS && selectedTeamFilterId != null) {
                player.sourceTeamId == selectedTeamFilterId
            } else true
            val matchesPos = selectedPositionFilter == "ALL" || player.position == selectedPositionFilter
            val matchesQuery = searchQuery.isBlank() ||
                player.name.contains(searchQuery, ignoreCase = true) ||
                player.sourceTeamName.contains(searchQuery, ignoreCase = true) ||
                player.archetype.contains(searchQuery, ignoreCase = true)
            matchesSource && matchesTeam && matchesPos && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("expansion_draft_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Expansion Draft War Room",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        val ownerNote = listOfNotNull(session.ownerFullName, session.ownerArchetype?.shortBadge).joinToString(" • ")
                        Text(
                            text = "${session.targetCity.name} ${session.franchiseNickname} • ${assignedConf.shortCode}${if (ownerNote.isNotBlank()) " • Gov: $ownerNote" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.testTag("btn_cancel_expansion_draft")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Abort Expansion Draft",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (!session.isCompleted && session.availablePool.any { !it.isProtected }) {
                        FilledTonalButton(
                            onClick = { viewModel.autoDraftBestAvailable() },
                            modifier = Modifier
                                .testTag("btn_auto_pick_draft")
                                .padding(end = 8.dp)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto Pick", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("expansion_draft_content"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)
            ) {
                // Header Card: Expansion Team Identity & Draft Tracker
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("expansion_team_header_card")
                            .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(session.brandColorHex))
                                            .border(2.dp, GoldPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = session.franchiseNickname.take(2).uppercase(),
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${session.targetCity.name} ${session.franchiseNickname}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "${assignedConf.shortCode} • ${assignedDiv.displayName} • ${session.ownershipType.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    color = if (session.isCompleted) EmeraldAccent.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (session.isCompleted) EmeraldAccent else GoldPrimary)
                                ) {
                                    Text(
                                        text = if (session.isCompleted) "DRAFT COMPLETE" else "PICK ${session.currentRound} OF ${session.totalRounds}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (session.isCompleted) EmeraldAccent else GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { session.draftedPlayers.size.toFloat() / session.totalRounds.toFloat().coerceAtLeast(1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (session.isCompleted) EmeraldAccent else GoldPrimary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("SELECTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${session.draftedPlayers.size} / ${session.totalRounds}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("PROJECTED OVR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${session.averageOvr} OVR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = if (session.averageOvr >= 80) GoldPrimary else CyanAccent)
                                }
                                Column {
                                    Text("PAYROLL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("\$${session.totalSalaryCommitted}M/yr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("SOURCES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${session.draftedFromRostersCount} Club • ${session.draftedFromFreeAgencyCount} FA",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent
                                    )
                                }
                            }
                        }
                    }
                }

                // Completed State Banner with Ratification Button
                if (session.isCompleted) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("draft_completed_celebration_card")
                                .border(2.dp, EmeraldAccent, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Celebration, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "EXPANSION DRAFT CONCLUDED!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = "${session.franchiseNickname} selected ${session.draftedPlayers.size} players (${session.draftedFromRostersCount} from existing rosters, ${session.draftedFromFreeAgencyCount} from free agency • Avg ${session.averageOvr} OVR).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.finalizeExpansionDraft() },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_finalize_expansion_draft")
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ratify ${session.franchiseNickname} & Enter League (+\$${session.targetCity.expansionFeeMillions}M)",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary View Switcher (Available Pool vs Drafted Roster)
                item {
                    TabRow(
                        selectedTabIndex = selectedViewTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = GoldPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedViewTab == 0,
                            onClick = { selectedViewTab = 0 },
                            text = {
                                Text(
                                    "Draft Board (${session.availablePool.size})",
                                    fontWeight = if (selectedViewTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedViewTab == 0) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("tab_available_pool")
                        )
                        Tab(
                            selected = selectedViewTab == 1,
                            onClick = { selectedViewTab = 1 },
                            text = {
                                Text(
                                    "Selected Roster (${session.draftedPlayers.size}/${session.totalRounds})",
                                    fontWeight = if (selectedViewTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedViewTab == 1) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("tab_drafted_roster")
                        )
                    }
                }

                if (selectedViewTab == 0) {
                    // Source Selection Bar: All vs Existing Rosters vs Free Agency Pool
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("source_filter_bar"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val sourceTabs = listOf(
                                    Triple(DraftPoolSourceFilter.ALL, "All (${session.availablePool.size})", "chip_source_all"),
                                    Triple(DraftPoolSourceFilter.EXISTING_ROSTERS, "Rosters ($rosterEligibleCount)", "chip_source_rosters"),
                                    Triple(DraftPoolSourceFilter.FREE_AGENCY, "Free Agency ($freeAgentEligibleCount)", "chip_source_free_agency")
                                )
                                sourceTabs.forEach { (source, label, tag) ->
                                    val isSelected = selectedSourceFilter == source
                                    val accent = when (source) {
                                        DraftPoolSourceFilter.ALL -> GoldPrimary
                                        DraftPoolSourceFilter.EXISTING_ROSTERS -> RoyalBlueAccent
                                        DraftPoolSourceFilter.FREE_AGENCY -> CyanAccent
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .clickable {
                                                selectedSourceFilter = source
                                                if (source != DraftPoolSourceFilter.EXISTING_ROSTERS) {
                                                    selectedTeamFilterId = null
                                                }
                                            }
                                            .testTag(tag),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Source Protection Rule Callout
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (selectedSourceFilter == DraftPoolSourceFilter.FREE_AGENCY) Icons.Default.Public else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (selectedSourceFilter == DraftPoolSourceFilter.FREE_AGENCY) CyanAccent else RoyalBlueAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (selectedSourceFilter) {
                                            DraftPoolSourceFilter.EXISTING_ROSTERS ->
                                                "Unprotected roster players from existing franchises (max ${session.maxPicksPerExistingTeam} picks per club)."
                                            DraftPoolSourceFilter.FREE_AGENCY ->
                                                "Unrestricted Free Agency Pool — sign veterans or international standouts with zero club caps."
                                            DraftPoolSourceFilter.ALL ->
                                                "Mix picks from Existing Franchise Rosters (max ${session.maxPicksPerExistingTeam}/team) and the live Free Agency Pool."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Club-by-club filter row when browsing Existing Rosters
                            if (selectedSourceFilter == DraftPoolSourceFilter.EXISTING_ROSTERS) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        FilterChip(
                                            selected = selectedTeamFilterId == null,
                                            onClick = { selectedTeamFilterId = null },
                                            label = { Text("All Clubs") },
                                            modifier = Modifier.testTag("chip_team_filter_all")
                                        )
                                    }
                                    items(uiState.franchises, key = { it.id }) { team ->
                                        val picksTaken = session.draftedCountFromTeam(team.id)
                                        val capped = picksTaken >= session.maxPicksPerExistingTeam
                                        FilterChip(
                                            selected = selectedTeamFilterId == team.id,
                                            onClick = {
                                                selectedTeamFilterId = if (selectedTeamFilterId == team.id) null else team.id
                                            },
                                            label = {
                                                Text(
                                                    text = if (capped) "${team.nickname} (MAX)" else "${team.nickname} ($picksTaken/${session.maxPicksPerExistingTeam})"
                                                )
                                            },
                                            modifier = Modifier.testTag("chip_team_filter_${team.id}")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by player, club, or archetype...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_search_draft_pool"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(allPositions) { pos ->
                                    FilterChip(
                                        selected = selectedPositionFilter == pos,
                                        onClick = { selectedPositionFilter = pos },
                                        label = { Text(pos) },
                                        modifier = Modifier.testTag("chip_pos_$pos")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Available Pool Player Cards
                    if (filteredPool.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No eligible players match your filter", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        items(filteredPool, key = { it.id }) { player ->
                            DraftablePlayerCard(
                                player = player,
                                canDraft = !session.isCompleted && !player.isProtected,
                                onDraft = { viewModel.draftPlayerForExpansion(player.id) }
                            )
                        }
                    }
                } else {
                    // Drafted Roster List
                    if (session.draftedPlayers.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No players selected yet. Pick from Existing Rosters or the Free Agency Pool!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            session.draftedPlayers.mapIndexed { idx, p -> Pair(idx + 1, p) },
                            key = { it.second.id }
                        ) { (roundNum, player) ->
                            DraftedRosterCard(
                                roundNumber = roundNum,
                                player = player,
                                onUndraft = { viewModel.undraftPlayerForExpansion(player.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Abort Expansion Draft?") },
            text = { Text("Exiting now will cancel the expansion draft session for ${session.targetCity.name}. No expansion fees or roster transfers will occur.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelExpansionDraft()
                    },
                    modifier = Modifier.testTag("btn_confirm_abort_draft")
                ) {
                    Text("Abort Draft", color = CrimsonAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Continue Drafting")
                }
            }
        )
    }
}

@Composable
fun DraftablePlayerCard(
    player: ExpansionDraftPlayer,
    canDraft: Boolean,
    onDraft: () -> Unit
) {
    val isFreeAgent = player.sourceType == ExpansionDraftSourceType.FREE_AGENCY_POOL
    val sourceAccent = if (isFreeAgent) CyanAccent else RoyalBlueAccent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("card_draft_player_${player.id}")
            .border(
                width = 1.dp,
                color = if (player.isProtected) CrimsonAlert.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (player.isProtected) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Surface(
                        color = when {
                            player.overallRating >= 82 -> GoldPrimary
                            player.overallRating >= 77 -> EmeraldAccent
                            else -> CyanAccent
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${player.overallRating}",
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${player.position} • Age ${player.age} • ${player.sourceTeamName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "\$${player.salaryMillions}M • ${player.contractYearsRemaining}y",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (player.isProtected) {
                        Surface(
                            color = CrimsonAlert.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Team Protected",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonAlert,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onDraft,
                            enabled = canDraft,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .testTag("btn_draft_player_${player.id}")
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFreeAgent) "Sign" else "Draft",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = sourceAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, sourceAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isFreeAgent) "Free Agency Pool" else "Existing Roster",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = sourceAccent,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = player.archetype,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (player.keyTrait.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = player.keyTrait,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraftedRosterCard(
    roundNumber: Int,
    player: ExpansionDraftPlayer,
    onUndraft: () -> Unit = {}
) {
    val isFreeAgent = player.sourceType == ExpansionDraftSourceType.FREE_AGENCY_POOL
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("card_drafted_round_$roundNumber")
            .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = EmeraldAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, EmeraldAccent)
                ) {
                    Text(
                        text = "RND $roundNumber",
                        fontWeight = FontWeight.Black,
                        color = EmeraldAccent,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = GoldPrimary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${player.overallRating}",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Column {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${player.position} • ${if (isFreeAgent) "Signed from Free Agency" else "Drafted from ${player.sourceTeamName}"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "\$${player.salaryMillions}M",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = EmeraldAccent
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onUndraft,
                    modifier = Modifier.testTag("btn_undraft_player_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Release ${player.name} Pick",
                        tint = CrimsonAlert
                    )
                }
            }
        }
    }
}
