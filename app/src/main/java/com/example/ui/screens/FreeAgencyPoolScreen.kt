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
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeAgencyPoolScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.closeFreeAgencyPool() }

    val selectedTeam = remember(uiState.franchises, uiState.selectedFreeAgencyFranchiseId) {
        uiState.franchises.find { it.id == uiState.selectedFreeAgencyFranchiseId }
            ?: uiState.franchises.firstOrNull()
    }

    val teamNeedsMap = remember(uiState.franchises, uiState.sportType, uiState.ruleBook) {
        uiState.franchises.associate { team ->
            team.id to FreeAgencyEngine.evaluateDepthChartNeeds(team, uiState.sportType, uiState.ruleBook)
        }
    }

    val selectedNeed = selectedTeam?.let { teamNeedsMap[it.id] }
    val positions = remember(uiState.sportType) { FreeAgencyEngine.positionsForSport(uiState.sportType) }
    var activeFilter by remember { mutableStateOf("ALL") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRosterReleaseSection by remember { mutableStateOf(false) }

    val filteredAgents = remember(uiState.freeAgentPool, activeFilter) {
        when (activeFilter) {
            "ALL" -> uiState.freeAgentPool
            "MARQUEE" -> uiState.freeAgentPool.filter { it.overallRating >= 80 }
            "VALUE" -> uiState.freeAgentPool.filter { it.askingSalaryMillions <= 6L }
            else -> uiState.freeAgentPool.filter { it.position.equals(activeFilter, ignoreCase = true) }
        }
    }

    val teamsNeedingDepthCount = remember(teamNeedsMap) {
        teamNeedsMap.values.count { it.priorityScore > 0 }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("free_agency_pool_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LEAGUE PERSONNEL & CAP DESK",
                            style = MaterialTheme.typography.labelSmall,
                            color = VioletAccent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Free Agency Pool",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeFreeAgencyPool() },
                        modifier = Modifier
                            .testTag("btn_close_free_agency_pool")
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
                        onClick = {
                            val (_, msg) = viewModel.triggerAutoFillDepthCharts()
                            statusMessage = msg
                        },
                        modifier = Modifier
                            .testTag("btn_auto_fill_depth_charts")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Auto-Fill Depth Charts",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Auto-Fill",
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Black,
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
                .testTag("free_agency_pool_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (statusMessage != null) {
                item {
                    Surface(
                        color = VioletAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fa_status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { statusMessage = null }) {
                                Text("Dismiss", style = MaterialTheme.typography.labelSmall, color = VioletAccent)
                            }
                        }
                    }
                }
            }

            // Market Summary & Auto-Sign Control Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_free_agency_summary")
                        .border(1.2.dp, VioletAccent.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(VioletAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Free Agency Market",
                                        tint = VioletAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${uiState.freeAgentPool.size} Available Free Agents",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$teamsNeedingDepthCount team(s) seeking depth • ${uiState.freeAgentSignings.size} deal(s) signed & reported",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
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
                                onClick = {
                                    val (_, msg) = viewModel.triggerAutoFillDepthCharts()
                                    statusMessage = msg
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_auto_fill_depth_banner")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Sign Depth Charts",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.refreshFreeAgentPool()
                                    statusMessage = "Scouted fresh international releases & two-way candidates into pool."
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_refresh_free_agency_pool")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scout Pool",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scout",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Franchise Target Selector
            item {
                SectionHeader(title = "Signing Franchise & Cap Room", modifier = Modifier.padding(top = 2.dp))
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fa_team_selector")
                ) {
                    items(uiState.franchises, key = { it.id }) { team ->
                        val isSelected = selectedTeam?.id == team.id
                        val need = teamNeedsMap[team.id]
                        val hasUrgent = (need?.urgentHolesCount ?: 0) > 0 || (need?.healthyCount ?: 8) < 8
                        val borderColor = when {
                            hasUrgent -> CrimsonAlert
                            isSelected -> VioletAccent
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { viewModel.selectFreeAgencyFranchise(team.id) }
                                .testTag("fa_team_chip_${team.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) VioletAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
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
                                    color = if (isSelected) VioletAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = if (hasUrgent) CrimsonAlert else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${team.roster.size}/${need?.maxRosterSlots ?: 15}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasUrgent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTeam != null && selectedNeed != null) {
                item {
                    val capLimit = uiState.unionAgreement.salaryCapMillions
                    val capRoom = capLimit - selectedTeam.annualPayrollMillions
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_selected_fa_club_${selectedTeam.id}")
                            .border(1.dp, Color(selectedTeam.colorHex).copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
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
                                        text = "${selectedTeam.teamFullName} (${selectedTeam.wins}-${selectedTeam.losses})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Depth: ${selectedNeed.healthyCount} Healthy (${selectedTeam.roster.size}/${selectedNeed.maxRosterSlots} slots) • Needs: ${selectedNeed.neededPositions.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (capRoom >= 0) "+\$${capRoom}M Room" else "Over Cap \$${-capRoom}M",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (capRoom >= 0) EmeraldAccent else AmberWarning
                                    )
                                    Text(
                                        text = "\$${selectedTeam.annualPayrollMillions}M / \$${capLimit}M (${uiState.ruleBook.salaryCapStrictness.shortLabel})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val teamRep = remember(selectedTeam, capLimit) {
                                TeamReputationEngine.evaluateReputation(selectedTeam, capLimit)
                            }
                            val repColor = Color(teamRep.tier.colorHex)
                            Surface(
                                color = repColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, repColor.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_fa_team_reputation")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Team Reputation: ${teamRep.tier.label} (${teamRep.score}/100)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = repColor
                                        )
                                        Text(
                                            text = "${teamRep.relocationCount} Reloc • ${teamRep.payrollPctOfCap}% Cap",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = if (teamRep.reputationSalaryPremiumMillions > 0L) {
                                            "FA Demand: +\$${teamRep.reputationSalaryPremiumMillions}M/yr premium • ${teamRep.summaryNote}"
                                        } else {
                                            teamRep.summaryNote
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.openDepthChart(selectedTeam.id) },
                                    modifier = Modifier.testTag("btn_fa_view_depth_chart")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListNumbered,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Depth Chart", color = GoldPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = { showRosterReleaseSection = !showRosterReleaseSection },
                                    modifier = Modifier.testTag("btn_toggle_waiver_release")
                                ) {
                                    Text(
                                        text = if (showRosterReleaseSection) "Hide Waivers" else "Waive Player (${selectedTeam.roster.size})",
                                        color = CrimsonAlert,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (showRosterReleaseSection) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Waive Bench Depth to Free Agency Pool (Min 6 players):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                selectedTeam.roster.sortedByDescending { it.depthOrder }.take(4).forEach { rp ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${rp.name} (${rp.position}, ${rp.overallRating} OVR • \$${rp.salaryMillions}M)",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                val (_, msg) = viewModel.releasePlayerToFreeAgency(selectedTeam.id, rp.id)
                                                statusMessage = msg
                                            },
                                            enabled = selectedTeam.roster.size > 6,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("btn_waive_player_${rp.id}")
                                        ) {
                                            Text("Waive", fontSize = 11.sp, color = CrimsonAlert)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Position & Market Filter Ribbon
            item {
                val filterOptions = listOf("ALL" to "All (${uiState.freeAgentPool.size})") +
                    positions.map { it to it } +
                    listOf("MARQUEE" to "80+ OVR", "VALUE" to "Budget <=$6M")

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions, key = { it.first }) { (key, label) ->
                        FilterChip(
                            selected = activeFilter == key,
                            onClick = { activeFilter = key },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (activeFilter == key) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chip_fa_filter_$key")
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Available Free Agents (${filteredAgents.size})")
            }

            items(filteredAgents, key = { it.id }) { agent ->
                val projectedTerms = selectedTeam?.let { team ->
                    FreeAgencyEngine.calculateRealisticContract(
                        freeAgent = agent,
                        team = team,
                        ruleBook = uiState.ruleBook,
                        unionAgreement = uiState.unionAgreement,
                        isUrgentDepthNeed = (selectedNeed?.urgentHolesCount ?: 0) > 0
                    )
                }
                val hasOpenSlot = (selectedNeed?.openRosterSlots ?: 0) > 0
                val canSign = selectedTeam != null && hasOpenSlot && (projectedTerms?.isCapCompliant == true)
                val signingOdds = selectedTeam?.let { team ->
                    TeamReputationEngine.evaluateFreeAgentSigningOdds(
                        freeAgent = agent,
                        franchise = team,
                        salaryCapMillions = uiState.unionAgreement.salaryCapMillions,
                        offerSalaryMillions = projectedTerms?.salaryMillions ?: agent.askingSalaryMillions,
                        isUrgentDepthNeed = (selectedNeed?.urgentHolesCount ?: 0) > 0
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fa_player_card_${agent.id}")
                        .border(1.dp, Color(agent.tierColorHex).copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(agent.tierColorHex).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = agent.position,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(agent.tierColorHex)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = agent.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            color = Color(agent.tierColorHex).copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = agent.marketTierLabel.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(agent.tierColorHex),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${agent.archetype} • Age ${agent.age} (${agent.experienceYears}Y Pro)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${agent.formerFranchiseName} • ${agent.bioTag}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${agent.overallRating}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(agent.tierColorHex)
                                )
                                Text(
                                    text = "OVR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (signingOdds != null) {
                            val oddsAccent = when {
                                signingOdds.likelihoodPct >= 72 -> EmeraldAccent
                                signingOdds.likelihoodPct >= 45 -> AmberWarning
                                else -> CrimsonAlert
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = oddsAccent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("fa_signing_odds_${agent.id}")
                                ) {
                                    Text(
                                        text = "Sign Odds: ${signingOdds.likelihoodPct}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = oddsAccent,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = signingOdds.driverSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("fa_reputation_note_${agent.id}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val termsText = if (projectedTerms != null) {
                                    "\$${projectedTerms.salaryMillions}M/yr • ${projectedTerms.contractYears}Y (${projectedTerms.contractTypeLabel})"
                                } else {
                                    "Ask: \$${agent.askingSalaryMillions}M/yr • ${agent.preferredYears}Y"
                                }
                                Text(
                                    text = termsText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (projectedTerms != null) {
                                    Text(
                                        text = if (hasOpenSlot) projectedTerms.complianceNote else "Roster at max capacity",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (canSign) EmeraldAccent else CrimsonAlert,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (selectedTeam != null) {
                                        val (_, msg) = viewModel.signFreeAgentForFranchise(selectedTeam.id, agent.id)
                                        statusMessage = msg
                                    }
                                },
                                enabled = canSign,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldAccent,
                                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_sign_fa_${agent.id}")
                            ) {
                                Text(
                                    text = "Sign",
                                    color = if (canSign) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Transaction Wire History
            item {
                SectionHeader(
                    title = "Recent Free Agency Signings (${uiState.freeAgentSignings.size})",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (uiState.freeAgentSignings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_fa_signings_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No free agent contracts finalized yet. Advance weeks or tap Auto-Sign Depth Charts to watch teams fill open spots via realistic contracts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(uiState.freeAgentSignings.take(15), key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fa_tx_card_${tx.id}")
                            .border(1.dp, VioletAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
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
                                    text = "${tx.franchiseName} sign ${tx.playerName} (${tx.position}, ${tx.overallRating} OVR)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = if (tx.isAutoSigned) VioletAccent.copy(alpha = 0.2f) else EmeraldAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (tx.isAutoSigned) "AUTO • WK ${tx.week}" else "SIGNED • WK ${tx.week}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (tx.isAutoSigned) VioletAccent else EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${tx.contractYears}Y / \$${tx.salaryMillions}M per yr • ${tx.contractTypeLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldPrimary
                            )
                            Text(
                                text = "Depth Chart Note: ${tx.reason}",
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
