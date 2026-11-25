package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.model.RookieProspect
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftScoutingScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler { viewModel.openDraftScoutingScreen(false) }

    val managed = uiState.managedFranchise
    var selectedPosFilter by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableIntStateOf(0) } // 0: Consensus Rank, 1: Overall, 2: Potential
    var draftFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val activePool = remember(uiState.activeOffSeasonSession, uiState.upcomingDraftClass) {
        val off = uiState.activeOffSeasonSession
        if (off != null && !off.isDraftCompleted && off.draftProspectPool.isNotEmpty()) {
            off.draftProspectPool
        } else {
            uiState.upcomingDraftClass
        }
    }

    val positions = remember(activePool) {
        listOf("ALL") + activePool.map { it.position }.distinct()
    }

    val filteredProspects = remember(activePool, selectedPosFilter, sortBy) {
        val base = if (selectedPosFilter == "ALL") activePool else activePool.filter { it.position == selectedPosFilter }
        when (sortBy) {
            1 -> base.sortedByDescending { it.overallRating }
            2 -> base.sortedByDescending { it.potentialRating }
            else -> base.sortedBy { it.consensusRank }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("draft_scouting_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Draft War Room & Scouting",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${uiState.upcomingDraftClassYear} Procedural Draft Class • ${managed?.teamFullName ?: "Select Franchise"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openDraftScoutingScreen(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_draft_scouting")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.generateProceduralDraftClass()
                            draftFeedback = true to "Generated new procedural ${uiState.upcomingDraftClassYear} draft class."
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_refresh_procedural_class")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Generate Procedural Draft Class",
                            tint = GoldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("draft_scouting_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Managed Franchise Selector Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_managed_franchise_selector"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MANAGED FRANCHISE WAR ROOM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = managed?.teamFullName ?: "No Franchise Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            if (managed != null) {
                                Surface(
                                    color = Color(managed.colorHex).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${managed.rosterRating} OVR • ${managed.roster.size} Roster",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(uiState.franchises) { franchise ->
                                val selected = franchise.id == managed?.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setManagedFranchise(franchise.id) },
                                    label = {
                                        Text(
                                            text = franchise.nickname,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.testTag("chip_manage_franchise_${franchise.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Procedural Class Generator Banner
            item {
                val undraftedCount = activePool.count { !it.isDrafted }
                val avgOvr = activePool.takeIf { it.isNotEmpty() }?.map { it.overallRating }?.average()?.toInt() ?: 0
                val topPot = activePool.maxOfOrNull { it.potentialRating } ?: 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PROCEDURAL DRAFT GENERATOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent
                                )
                                Text(
                                    text = "$undraftedCount/${activePool.size} Available • Avg $avgOvr OVR • Ceiling $topPot POT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "CBA Rookie Wage Scale: ${uiState.unionAgreement.rookieScaleMultiplierPct}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.generateProceduralDraftClass()
                                    draftFeedback = true to "New procedural ${uiState.upcomingDraftClassYear} draft class generated with updated OVR/POT scouting."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_generate_draft_class")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Regenerate Class", tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Class", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }

                        // Position & Sort Controls
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(positions) { pos ->
                                FilterChip(
                                    selected = selectedPosFilter == pos,
                                    onClick = { selectedPosFilter = pos },
                                    label = { Text(pos, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("chip_pos_$pos")
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Consensus", 1 to "Sort OVR", 2 to "Sort POT").forEach { (idx, label) ->
                                FilterChip(
                                    selected = sortBy == idx,
                                    onClick = { sortBy = idx },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("chip_sort_$idx")
                                )
                            }
                        }
                    }
                }
            }

            if (draftFeedback != null) {
                item {
                    Surface(
                        color = (if (draftFeedback!!.first) EmeraldAccent else CrimsonAlert).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("banner_draft_feedback")
                    ) {
                        Text(
                            text = draftFeedback!!.second,
                            color = if (draftFeedback!!.first) EmeraldAccent else CrimsonAlert,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            items(filteredProspects, key = { it.id }) { prospect ->
                ProspectScoutingCard(
                    prospect = prospect,
                    managedNickname = managed?.nickname ?: "Franchise",
                    draftedTeamName = prospect.draftedByFranchiseId?.let { id ->
                        uiState.franchises.find { it.id == id }?.nickname
                    },
                    onPickProspect = {
                        val res = viewModel.pickProspectForManagedFranchise(prospect.id)
                        draftFeedback = res
                    }
                )
            }
        }
    }
}

@Composable
private fun ProspectScoutingCard(
    prospect: RookieProspect,
    managedNickname: String,
    draftedTeamName: String?,
    onPickProspect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scouting_prospect_${prospect.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (prospect.isDrafted) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            else GoldPrimary.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (prospect.consensusRank <= 3) GoldPrimary else RoyalBlueAccent.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${prospect.consensusRank}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (prospect.consensusRank <= 3) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = "${prospect.name} • ${prospect.position}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${prospect.archetype} • Age ${prospect.age} • ${prospect.collegiateMetrics.schoolName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${prospect.overallRating} OVR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${prospect.potentialRating} POT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(
                text = "College: ${prospect.collegiateMetrics.formattedStatLine} • Upside +${prospect.upsidePoints} (${prospect.scoutingReport.potentialGrade})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scout ${prospect.scoutingReport.scoutGrade}/99 • Combine ${prospect.scoutingReport.combineAthleticScore}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (prospect.isDrafted) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Drafted #${prospect.draftedPickNumber ?: "-"} by ${draftedTeamName ?: "Club"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onPickProspect,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_draft_prospect_${prospect.id}")
                    ) {
                        Text(
                            text = "Pick for $managedNickname",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
