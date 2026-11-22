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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthChartScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.closeDepthChart() }

    val selectedFranchise = remember(uiState.franchises, uiState.selectedDepthChartFranchiseId) {
        uiState.franchises.find { it.id == uiState.selectedDepthChartFranchiseId }
            ?: uiState.franchises.firstOrNull()
    }

    val activeStarters = remember(selectedFranchise) {
        selectedFranchise?.let { InjuryEngine.getActiveStarters(it) } ?: emptyList()
    }
    val benchPlayers = remember(selectedFranchise) {
        selectedFranchise?.let { InjuryEngine.getBenchPlayers(it) } ?: emptyList()
    }

    val totalLeagueInjured = remember(uiState.franchises) {
        uiState.franchises.sumOf { it.injuredPlayersCount }
    }
    val totalLeagueHoles = remember(uiState.franchises) {
        uiState.franchises.sumOf { it.unresolvedStarterHolesCount }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("depth_chart_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MEDICAL CENTER & LINEUPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Depth Charts & Injuries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeDepthChart() },
                        modifier = Modifier
                            .testTag("btn_close_depth_chart")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to League",
                            tint = GoldPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.autoOptimizeAllDepthCharts() },
                        modifier = Modifier
                            .testTag("btn_optimize_all_depth_charts")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Optimize All",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Optimize All",
                            color = EmeraldAccent,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("depth_chart_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // League Medical Summary Strip
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("league_medical_summary_card")
                        .border(
                            1.2.dp,
                            if (totalLeagueHoles > 0) CrimsonAlert.copy(alpha = 0.7f) else GoldPrimary.copy(alpha = 0.45f),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalLeagueHoles > 0) CrimsonAlert.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (totalLeagueHoles > 0) CrimsonAlert.copy(alpha = 0.2f) else EmeraldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = "League Medical Status",
                                    tint = if (totalLeagueHoles > 0) CrimsonAlert else EmeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (totalLeagueHoles > 0) {
                                        "$totalLeagueHoles Unadjusted Injured Starter(s)"
                                    } else if (totalLeagueInjured > 0) {
                                        "$totalLeagueInjured League Player(s) On Injury Report"
                                    } else {
                                        "All Starting Lineups Medically Cleared"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Week ${uiState.currentWeek} • Bench rest accelerates minor recovery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Franchise Selector Ribbon
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("depth_chart_team_selector")
                ) {
                    items(uiState.franchises, key = { it.id }) { team ->
                        val isSelected = selectedFranchise?.id == team.id
                        val holes = team.unresolvedStarterHolesCount
                        val injuredCount = team.injuredPlayersCount
                        val chipBorderColor = when {
                            holes > 0 -> CrimsonAlert
                            injuredCount > 0 -> AmberWarning
                            isSelected -> GoldPrimary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }

                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { viewModel.selectDepthChartFranchise(team.id) }
                                .testTag("chip_depth_team_${team.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, chipBorderColor)
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
                                if (holes > 0) {
                                    Surface(
                                        color = CrimsonAlert,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${holes} OUT",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (injuredCount > 0) {
                                    Surface(
                                        color = AmberWarning,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$injuredCount INJ",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
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
                // Selected Team Lineup Readiness Card
                item {
                    val baseOvr = selectedFranchise.rosterRating
                    val effOvr = selectedFranchise.effectiveLineupRating
                    val delta = effOvr - baseOvr
                    val holes = selectedFranchise.unresolvedStarterHolesCount

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("team_readiness_card")
                            .border(
                                1.2.dp,
                                if (holes > 0) CrimsonAlert else Color(selectedFranchise.colorHex).copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${selectedFranchise.teamFullName} • ${selectedFranchise.wins}-${selectedFranchise.losses}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (holes > 0) {
                                            "URGENT: $holes starter(s) medically OUT — promote healthy bench depth!"
                                        } else if (selectedFranchise.injuredPlayersCount > 0) {
                                            "${selectedFranchise.injuredPlayersCount} player(s) nursing injuries on roster"
                                        } else {
                                            "100% Healthy Starting 5 — Full Match Effectiveness"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (holes > 0) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "EFFECTIVE OVR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$effOvr",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Black,
                                            color = when {
                                                holes > 0 -> CrimsonAlert
                                                delta < 0 -> AmberWarning
                                                else -> EmeraldAccent
                                            }
                                        )
                                        Text(
                                            text = " / $baseOvr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.autoOptimizeFranchiseDepthChart(selectedFranchise.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (holes > 0) CrimsonAlert else GoldPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_optimize_team_depth_chart")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = if (holes > 0) Color.White else Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (holes > 0) "Fix Holes" else "Optimize",
                                        color = if (holes > 0) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.openFreeAgencyPool(selectedFranchise.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .testTag("btn_depth_open_fa_pool")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Free Agency Pool",
                                        tint = VioletAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "FA Pool (${uiState.freeAgentPool.size})",
                                        color = VioletAccent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            val teamAffiliate = uiState.minorLeagueState.affiliates.find { it.parentFranchiseId == selectedFranchise.id }
                            if (teamAffiliate != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.openMinorLeagueHub(teamAffiliate.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_depth_open_minor_league")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = "Minor Development League",
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${uiState.minorLeagueState.leagueName}: ${teamAffiliate.fullName} (${teamAffiliate.wins}-${teamAffiliate.losses})",
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Starting Lineup (Slots 1-5)
                item {
                    SectionHeader(
                        title = "Starting Lineup (Active 5)",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                itemsIndexed(activeStarters, key = { _, p -> "starter_${p.id}" }) { index, player ->
                    DepthChartPlayerCard(
                        player = player,
                        slotLabel = "STARTER ${index + 1}",
                        isStarterCard = true,
                        canMoveUp = index > 0,
                        canMoveDown = true,
                        onMoveUp = { viewModel.movePlayerDepthOrder(selectedFranchise.id, player.id, moveUp = true) },
                        onMoveDown = { viewModel.movePlayerDepthOrder(selectedFranchise.id, player.id, moveUp = false) },
                        onPromote = null,
                        onDemote = { viewModel.demoteStarterOnDepthChart(selectedFranchise.id, player.id) },
                        onToggleIr = if (player.isInjured) {
                            { viewModel.togglePlayerInjuredReserve(selectedFranchise.id, player.id) }
                        } else null,
                        onToggleMinorAssignment = null
                    )
                }

                // Bench Rotation & Injured Reserve
                item {
                    SectionHeader(
                        title = "Bench Depth & Injured Reserve (${benchPlayers.size})",
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                itemsIndexed(benchPlayers, key = { _, p -> "bench_${p.id}" }) { index, player ->
                    DepthChartPlayerCard(
                        player = player,
                        slotLabel = if (player.activeInjury?.isOnInjuredReserve == true) "INJURED RESERVE" else "BENCH #${index + 1}",
                        isStarterCard = false,
                        canMoveUp = true,
                        canMoveDown = index < benchPlayers.lastIndex,
                        onMoveUp = { viewModel.movePlayerDepthOrder(selectedFranchise.id, player.id, moveUp = true) },
                        onMoveDown = { viewModel.movePlayerDepthOrder(selectedFranchise.id, player.id, moveUp = false) },
                        onPromote = if (!player.isMedicallyOut) {
                            { viewModel.promotePlayerOnDepthChart(selectedFranchise.id, player.id) }
                        } else null,
                        onDemote = null,
                        onToggleIr = if (player.isInjured) {
                            { viewModel.togglePlayerInjuredReserve(selectedFranchise.id, player.id) }
                        } else null,
                        onToggleMinorAssignment = if (!player.isMedicallyOut) {
                            {
                                viewModel.toggleBenchPlayerMinorAssignment(
                                    franchiseId = selectedFranchise.id,
                                    playerId = player.id,
                                    assignToMinor = !player.isAssignedToMinorLeague
                                )
                            }
                        } else null
                    )
                }
            }

            // League Medical & Injury Wire
            item {
                SectionHeader(
                    title = "League Injury Wire (${uiState.injuryReportHistory.size})",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.injuryReportHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_injury_wire_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Clean bill of health so far this season. Simulate games to track live match injuries and medical timelines.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.injuryReportHistory.take(15), key = { it.id }) { event ->
                    val sevColor = Color(event.injuryType.severity.badgeColorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("injury_log_${event.id}")
                            .border(1.dp, sevColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${event.playerName} (${event.position}) • ${event.franchiseName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = sevColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (event.isPlayoffs) "PLAYOFFS • ${event.weeksOut}W" else "WK ${event.week} • ${event.weeksOut}W",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = sevColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${event.injuryType.displayName} (${event.injuryType.severity.label})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = sevColor
                            )
                            Text(
                                text = event.context,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DepthChartPlayerCard(
    player: RosterPlayer,
    slotLabel: String,
    isStarterCard: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPromote: (() -> Unit)?,
    onDemote: (() -> Unit)?,
    onToggleIr: (() -> Unit)?,
    onToggleMinorAssignment: (() -> Unit)?
) {
    val activeInj = player.activeInjury
    val statusColor = when {
        player.isSuspended -> CrimsonAlert
        !player.isInjured -> EmeraldAccent
        player.isMedicallyOut -> CrimsonAlert
        else -> Color(activeInj!!.type.severity.badgeColorHex)
    }
    val borderColor = when {
        player.isSuspended -> CrimsonAlert
        isStarterCard && player.isMedicallyOut -> CrimsonAlert
        player.isInjured -> statusColor.copy(alpha = 0.65f)
        isStarterCard -> GoldPrimary.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("depth_player_card_${player.id}")
            .border(if (isStarterCard && player.isUnavailableForGame) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isStarterCard && player.isUnavailableForGame) {
                CrimsonAlert.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Position Circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isStarterCard) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.position,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isStarterCard) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = slotLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isStarterCard) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = statusColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = player.healthStatusLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
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
                            text = "${player.archetype} • Age ${player.age} • \$${player.salaryMillions}M/yr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // OVR Rating Block
                Column(horizontalAlignment = Alignment.End) {
                    if (player.isSuspended) {
                        Text(
                            text = "SUSP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = CrimsonAlert
                        )
                        Text(
                            text = "${player.suspendedGamesRemaining}G Left • Base ${player.overallRating}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonAlert,
                            fontSize = 10.sp
                        )
                    } else if (player.isMedicallyOut) {
                        Text(
                            text = "OUT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = CrimsonAlert
                        )
                        Text(
                            text = "Base ${player.overallRating}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    } else if (player.isInjured) {
                        Text(
                            text = "${player.effectiveOverallRating} OVR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                        Text(
                            text = "Base ${player.overallRating} (${activeInj!!.type.ovrPenalty})",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = "${player.overallRating} OVR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                        Text(
                            text = "100% Output",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (player.isSuspended) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CrimsonAlert.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "LEAGUE SUSPENSION (${player.suspendedGamesRemaining} game(s) remaining) — Ineligible for game action (0% output).",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Clinical Injury Box (if injured)
            if (activeInj != null && player.isInjured) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${activeInj.type.displayName} (${activeInj.type.bodyPart})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Text(
                                text = "${activeInj.weeksRemaining} wk(s) left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                        }
                        Text(
                            text = if (player.isMedicallyOut) {
                                "${activeInj.type.clinicalNote} Production: 0% (DNP)"
                            } else {
                                "${activeInj.type.clinicalNote} Stat Output: ${(player.statProductionMultiplier * 100).toInt()}%"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Depth Chart & Medical Controls
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_move_up_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up Depth Chart",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_move_down_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down Depth Chart",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onToggleIr != null) {
                    val isOnIr = activeInj?.isOnInjuredReserve == true
                    OutlinedButton(
                        onClick = onToggleIr,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .testTag("btn_toggle_ir_${player.id}")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = if (isOnIr) "Activate" else "Place on IR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnIr) EmeraldAccent else CrimsonAlert
                        )
                    }
                }

                if (onToggleMinorAssignment != null) {
                    val assigned = player.isAssignedToMinorLeague
                    OutlinedButton(
                        onClick = onToggleMinorAssignment,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .testTag("btn_depth_toggle_minor_${player.id}")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = if (assigned) "In Minor" else "Send Minor",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (assigned) EmeraldAccent else CyanAccent
                        )
                    }
                }

                if (onPromote != null) {
                    Button(
                        onClick = onPromote,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .testTag("btn_promote_player_${player.id}")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = "Promote",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (onDemote != null) {
                    OutlinedButton(
                        onClick = onDemote,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .testTag("btn_demote_player_${player.id}")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = if (player.isMedicallyOut) "Sub Out" else "Bench",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (player.isMedicallyOut) CrimsonAlert else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
