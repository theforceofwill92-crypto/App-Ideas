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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinorLeagueScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.closeMinorLeagueHub() }

    val minorState = uiState.minorLeagueState
    val affiliates = minorState.affiliates
    val selectedAffiliate = remember(affiliates, uiState.selectedMinorAffiliateId) {
        affiliates.find { it.id == uiState.selectedMinorAffiliateId } ?: affiliates.firstOrNull()
    }
    val selectedParent = remember(uiState.franchises, selectedAffiliate) {
        selectedAffiliate?.let { aff -> uiState.franchises.find { it.id == aff.parentFranchiseId } }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Standings", "Depth & Dev", "Markets", "Finances", "Playoffs & Awards")

    val rankedAffiliates = remember(affiliates) {
        affiliates.sortedWith(
            compareByDescending<MinorLeagueAffiliate> { it.winPct }
                .thenByDescending { it.pointDifferential }
                .thenByDescending { it.pointsFor }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("minor_league_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${uiState.sportType.displayName.uppercase()} DEVELOPMENT SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = minorState.leagueName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeMinorLeagueHub() },
                        modifier = Modifier
                            .testTag("btn_close_minor_league")
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
                    if (selectedParent != null) {
                        TextButton(
                            onClick = {
                                viewModel.closeMinorLeagueHub()
                                viewModel.openDepthChart(selectedParent.id)
                            },
                            modifier = Modifier
                                .testTag("btn_minor_to_depth_chart")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = "Open Depth Charts",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Depth Chart", color = CyanAccent, fontWeight = FontWeight.Bold)
                        }
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
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circuit Overview & Scaling Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("minor_circuit_overview_card")
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
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
                                    text = minorState.scalingFormatSummary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "1-to-1 affiliated depth development • Positions ${uiState.sportType.primaryPositionSummary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${minorState.playoffFieldSize}-TEAM PLAYOFFS",
                                    color = EmeraldAccent,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinorMetricChip(
                                label = "AFFILIATES",
                                value = "${affiliates.size}",
                                accent = CyanAccent,
                                modifier = Modifier.weight(1f)
                            )
                            MinorMetricChip(
                                label = "CIRCUIT REV",
                                value = "$${String.format("%.1f", minorState.totalCircuitRevenueMillions)}M",
                                accent = EmeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            MinorMetricChip(
                                label = "ATTENDANCE",
                                value = formatCompactCount(minorState.totalCircuitAttendance),
                                accent = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MinorMetricChip(
                                label = "+1 OVR GAINS",
                                value = "+${minorState.totalOvrBreakthroughs}",
                                accent = VioletAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Affiliate Selector Ribbon
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("minor_affiliate_selector")
                ) {
                    items(affiliates, key = { it.id }) { aff ->
                        val isSelected = selectedAffiliate?.id == aff.id
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { viewModel.selectMinorAffiliate(aff.id) }
                                .testTag("chip_minor_affiliate_${aff.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
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
                                        .background(Color(aff.colorHex))
                                )
                                Column {
                                    Text(
                                        text = aff.fullName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${aff.wins}-${aff.losses} • ${aff.parentFranchiseName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab Strip
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = EmeraldAccent,
                    edgePadding = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("minor_league_tabs")
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            modifier = Modifier.testTag("minor_tab_${title.lowercase().replace(" ", "_").replace("&", "and")}"),
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == idx) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            // Tab Body
            when (selectedTab) {
                0 -> {
                    item {
                        MinorSectionTitle(
                            title = "Circuit Standings & Top Seed Cut",
                            subtitle = "Top ${minorState.playoffFieldSize} affiliates qualify automatically (scales with parent expansion)"
                        )
                    }
                    itemsIndexed(rankedAffiliates, key = { _, a -> a.id }) { idx, aff ->
                        val seed = idx + 1
                        val inPlayoffs = seed <= minorState.playoffFieldSize
                        MinorStandingRowCard(
                            seed = seed,
                            affiliate = aff,
                            inPlayoffs = inPlayoffs,
                            isSelected = aff.id == selectedAffiliate?.id,
                            onSelect = { viewModel.selectMinorAffiliate(aff.id) }
                        )
                    }
                    if (minorState.currentWeekMatches.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            MinorSectionTitle(
                                title = "Latest Minor Circuit Results",
                                subtitle = "Simulated weekly alongside parent fixtures"
                            )
                        }
                        items(minorState.currentWeekMatches, key = { it.id }) { m ->
                            MinorMatchResultCard(match = m)
                        }
                    }
                }

                1 -> {
                    if (selectedAffiliate != null && selectedParent != null) {
                        val assignedPlayers = MinorLeagueEngine.getParentAssignedDepthPlayers(selectedParent)
                        val benchStandbyPlayers = MinorLeagueEngine.getEligibleParentBenchPlayers(selectedParent)
                            .filter { !it.isAssignedToMinorLeague }

                        item {
                            MinorSectionTitle(
                                title = "${selectedAffiliate.fullName} — Active Depth Pipeline",
                                subtitle = "Assigned bench players earn devXP (+1 OVR per 100 XP) & remain parent-owned"
                            )
                        }

                        if (assignedPlayers.isEmpty()) {
                            item {
                                MinorEmptyNoteCard("No parent depth players currently assigned. Move healthy bench players below to develop them here.")
                            }
                        } else {
                            items(assignedPlayers, key = { it.id }) { player ->
                                val stat = minorState.playerStats[player.id]
                                MinorAssignedDepthPlayerCard(
                                    player = player,
                                    stat = stat,
                                    sportType = uiState.sportType,
                                    onRecallToBench = {
                                        viewModel.toggleBenchPlayerMinorAssignment(selectedParent.id, player.id, false)
                                    },
                                    onPromoteToStarter = {
                                        viewModel.promoteDepthPlayerFromMinor(selectedParent.id, player.id)
                                    }
                                )
                            }
                        }

                        item {
                            MinorSectionTitle(
                                title = "${selectedParent.teamFullName} Parent Bench Standby",
                                subtitle = "Healthy reserve players kept with the parent club (not earning minor league minutes)"
                            )
                        }
                        if (benchStandbyPlayers.isEmpty()) {
                            item {
                                MinorEmptyNoteCard("All healthy bench depth players for ${selectedParent.nickname} are currently active in ${selectedAffiliate.nickname} development games.")
                            }
                        } else {
                            items(benchStandbyPlayers, key = { it.id }) { benchPlayer ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("minor_bench_standby_${benchPlayer.id}"),
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
                                            Text(
                                                text = "${benchPlayer.name} (${benchPlayer.position})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${benchPlayer.overallRating} OVR • Age ${benchPlayer.age} • ${benchPlayer.archetype}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.toggleBenchPlayerMinorAssignment(selectedParent.id, benchPlayer.id, true)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                            modifier = Modifier
                                                .testTag("btn_assign_minor_${benchPlayer.id}")
                                                .minimumInteractiveComponentSize()
                                        ) {
                                            Text("Assign Minor", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            MinorSectionTitle(
                                title = "Two-Way Affiliate Supplementals",
                                subtitle = "Regional signees filling remaining on-court/field positions alongside parent depth"
                            )
                        }
                        items(selectedAffiliate.supplementalProspects, key = { it.id }) { prospect ->
                            val stat = minorState.playerStats[prospect.id]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${prospect.name} • ${prospect.position}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${prospect.archetype} • Age ${prospect.age} • \$${String.format("%.2f", prospect.salaryMillions)}M two-way",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (stat != null && stat.gamesPlayed > 0) {
                                            Text(
                                                text = "Stats: ${stat.primaryPerGame} ${minorPrimaryStatLabel(uiState.sportType)} • ${stat.secondaryPerGame} ${minorSecondaryStatLabel(uiState.sportType)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyanAccent
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${prospect.overallRating} OVR",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = GoldPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        MinorSectionTitle(
                            title = "Feeder Markets & Ticket Pricing",
                            subtitle = "Regional cities grow alongside parent franchises; adjust tickets ($12–$48)"
                        )
                    }
                    items(affiliates, key = { it.id }) { aff ->
                        MinorMarketCard(
                            affiliate = aff,
                            onAdjustTicket = { delta ->
                                viewModel.updateMinorAffiliateTicketPrice(aff.id, aff.ticketPrice + delta)
                            }
                        )
                    }
                }

                3 -> {
                    item {
                        MinorCircuitFinancesSummaryCard(minorState = minorState)
                    }
                    item {
                        MinorSectionTitle(
                            title = "Affiliate P&L & Valuations",
                            subtitle = "Gate receipts, local sponsors, circuit streaming & parent stipends vs payroll/travel"
                        )
                    }
                    items(affiliates.sortedByDescending { it.finances.netOperatingBalanceMillions }, key = { it.id }) { aff ->
                        MinorAffiliateFinanceCard(affiliate = aff)
                    }
                }

                else -> {
                    item {
                        MinorSectionTitle(
                            title = "${minorState.playoffFieldSize}-Team Scaling Postseason",
                            subtitle = "Automatically scales (2 -> 4 -> 6 -> 8 teams) as new parent clubs join the league"
                        )
                    }
                    if (minorState.playoffChampionName != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("minor_champion_banner")
                                    .border(1.5.dp, GoldPrimary, RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.14f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Minor League Champion",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "SEASON ${uiState.currentYear} DEVELOPMENT CHAMPION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = GoldPrimary
                                        )
                                        Text(
                                            text = minorState.playoffChampionName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                        minorState.playoffRunnerUpName?.let { ru ->
                                            Text(
                                                text = "Defeated $ru in Minor Finals",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (minorState.playoffMatches.isEmpty()) {
                        item {
                            MinorEmptyNoteCard(
                                "Postseason bracket locks at Week 15 alongside the parent playoffs (${minorState.playoffFieldSize} affiliates qualify)."
                            )
                        }
                    } else {
                        items(minorState.playoffMatches, key = { it.id }) { match ->
                            MinorMatchResultCard(match = match)
                        }
                    }

                    if (minorState.annualAwards.isNotEmpty()) {
                        item {
                            MinorSectionTitle(
                                title = "Season ${uiState.currentYear} Minor League Honors",
                                subtitle = "Annual individual accolades & top call-up performers"
                            )
                        }
                        items(minorState.annualAwards, key = { it.id }) { award ->
                            MinorAwardCard(award = award)
                        }
                    }

                    if (minorState.weeklyAwards.isNotEmpty()) {
                        item {
                            MinorSectionTitle(
                                title = "Prospects of the Week",
                                subtitle = "Weekly standouts across the ${minorState.leagueName}"
                            )
                        }
                        items(minorState.weeklyAwards.take(8), key = { "${it.seasonYear}_${it.week}_${it.playerId}" }) { potw ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                        Text(
                                            text = "W${potw.week} • ${potw.playerName} (${potw.position})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${potw.affiliateName} (${potw.parentFranchiseName})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CyanAccent
                                        )
                                        Text(
                                            text = potw.statSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(color = GoldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "IMP ${potw.impactScore}",
                                            color = GoldPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (minorState.pastSeasons.isNotEmpty()) {
                        item {
                            MinorSectionTitle(
                                title = "Circuit Title & Scaling Archive",
                                subtitle = "Historical minor league champions & growth milestones"
                            )
                        }
                        items(minorState.pastSeasons, key = { it.year }) { archive ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${archive.year} • ${archive.championAffiliateName} (${archive.championParentName})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = GoldPrimary
                                    )
                                    Text(
                                        text = "Defeated ${archive.runnerUpAffiliateName} • ${archive.affiliatesCount} clubs (${archive.playoffFieldSize}-team playoffs) • Rev \$${String.format("%.1f", archive.circuitRevenueMillions)}M",
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

@Composable
private fun MinorMetricChip(
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = accent
            )
        }
    }
}

@Composable
private fun MinorStandingRowCard(
    seed: Int,
    affiliate: MinorLeagueAffiliate,
    inPlayoffs: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    isSelected -> EmeraldAccent
                    inPlayoffs -> CyanAccent.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (inPlayoffs) EmeraldAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$seed",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (inPlayoffs) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = affiliate.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (affiliate.isExpansionAffiliate) {
                        Surface(color = VioletAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "EXPANSION",
                                color = VioletAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Parent: ${affiliate.parentFranchiseName} • ${affiliate.marketTierLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${affiliate.wins}-${affiliate.losses}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary
                )
                val diffSign = if (affiliate.pointDifferential >= 0) "+" else ""
                Text(
                    text = "Diff $diffSign${affiliate.pointDifferential}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (affiliate.pointDifferential >= 0) EmeraldAccent else CrimsonAlert
                )
            }
        }
    }
}

@Composable
private fun MinorAssignedDepthPlayerCard(
    player: RosterPlayer,
    stat: MinorLeaguePlayerStat?,
    sportType: SportType,
    onRecallToBench: () -> Unit,
    onPromoteToStarter: () -> Unit
) {
    val xp = stat?.devXp ?: 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("minor_assigned_player_${player.id}")
            .border(1.dp, EmeraldAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${player.name} (${player.position})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (player.minorLeagueOvrProgress > 0) {
                            Surface(color = VioletAccent.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "+${player.minorLeagueOvrProgress} DEV",
                                    color = VioletAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${player.archetype} • Age ${player.age} • \$${player.salaryMillions}M/yr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${player.overallRating} OVR",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary
                )
            }

            if (stat != null && stat.gamesPlayed > 0) {
                Text(
                    text = "${stat.gamesPlayed} GP • ${stat.primaryPerGame} ${minorPrimaryStatLabel(sportType)} • ${stat.secondaryPerGame} ${minorSecondaryStatLabel(sportType)} • Dev XP $xp/100",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent
                )
            } else {
                Text(
                    text = "Assigned to affiliate rotation • Dev XP $xp/100 (+1 OVR at 100 XP)",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldAccent
                )
            }

            LinearProgressIndicator(
                progress = { (xp.coerceIn(0, 100)) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = EmeraldAccent,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRecallToBench,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_recall_minor_${player.id}")
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Recall to Bench", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onPromoteToStarter,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_callup_starter_${player.id}")
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Call Up Starter", color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MinorMarketCard(
    affiliate: MinorLeagueAffiliate,
    onAdjustTicket: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("minor_market_card_${affiliate.id}"),
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
                        text = "${affiliate.fullName} — ${affiliate.arenaName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${affiliate.marketTierLabel} • Pop ${affiliate.populationMillions}M • ${String.format("%,d", affiliate.arenaCapacity)} seats",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = CyanAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "LOY ${affiliate.fanLoyalty}/100",
                        color = CyanAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Feeder Ticket Price: \$${affiliate.ticketPrice}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Text(
                        text = "Avg Gate: ${String.format("%,d", affiliate.finances.avgAttendancePerGame)}/gm • Appetite ${affiliate.marketAppetite}/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { onAdjustTicket(-2) },
                        enabled = affiliate.ticketPrice > 12,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .testTag("btn_minor_ticket_minus_${affiliate.id}")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("-\$2", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAdjustTicket(2) },
                        enabled = affiliate.ticketPrice < 48,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .testTag("btn_minor_ticket_plus_${affiliate.id}")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("+\$2", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MinorCircuitFinancesSummaryCard(minorState: MinorLeagueState) {
    val net = minorState.totalCircuitNetMillions
    val netColor = if (net >= 0) EmeraldAccent else CrimsonAlert
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, netColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "MINOR CIRCUIT CONSOLIDATED TREASURY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = netColor
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MinorMetricChip(
                    label = "REVENUE",
                    value = "\$${String.format("%.2f", minorState.totalCircuitRevenueMillions)}M",
                    accent = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                MinorMetricChip(
                    label = "EXPENSES",
                    value = "\$${String.format("%.2f", minorState.totalCircuitExpensesMillions)}M",
                    accent = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                MinorMetricChip(
                    label = "NET P&L",
                    value = "${if (net >= 0) "+" else ""}\$${String.format("%.2f", net)}M",
                    accent = netColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MinorAffiliateFinanceCard(affiliate: MinorLeagueAffiliate) {
    val f = affiliate.finances
    val net = f.netOperatingBalanceMillions
    val netColor = if (net >= 0) EmeraldAccent else CrimsonAlert
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = affiliate.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (net >= 0) "+" else ""}\$${String.format("%.2f", net)}M Net",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = netColor
                )
            }
            Text(
                text = "Gate \$${String.format("%.2f", f.ticketRevenueMillions)}M • Sponsors \$${String.format("%.2f", f.localSponsorMillions)}M • Stream \$${String.format("%.2f", f.streamingRevenueMillions)}M • Stipend \$${String.format("%.2f", f.parentSubsidyMillions)}M",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = "Payroll & Ops: \$${String.format("%.2f", f.totalExpensesMillions)}M • Valuation: \$${String.format("%.1f", f.affiliateValuationMillions)}M",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent
            )
        }
    }
}

@Composable
private fun MinorMatchResultCard(match: MinorLeagueMatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = match.roundName.ifBlank { "Week ${match.week}" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (match.isPlayoff) GoldPrimary else CyanAccent
                )
                if (match.isPlayed) {
                    Text(
                        text = "${String.format("%,d", match.attendance)} fans",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val scoreText = if (match.isPlayed) {
                "${match.homeAffiliateName} ${match.homeScore} — ${match.awayScore} ${match.awayAffiliateName}"
            } else {
                "${match.homeAffiliateName} vs ${match.awayAffiliateName} (Scheduled)"
            }
            Text(
                text = scoreText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (match.topPerformerSummary.isNotBlank()) {
                Text(
                    text = match.topPerformerSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldAccent
                )
            }
        }
    }
}

@Composable
private fun MinorAwardCard(award: MinorLeagueAward) {
    val accent = Color(award.type.colorHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = award.type.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
                Surface(color = accent.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = award.type.badge,
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = "${award.playerName} (${award.position}) — ${award.affiliateName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Parent Club: ${award.parentFranchiseName} • ${award.statLine}",
                style = MaterialTheme.typography.bodySmall,
                color = CyanAccent
            )
            Text(
                text = award.narrative,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MinorEmptyNoteCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun MinorSectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun minorPrimaryStatLabel(sportType: SportType): String = when (sportType) {
    SportType.BASKETBALL -> "PPG"
    SportType.FOOTBALL -> "YPG"
    SportType.SOCCER -> "GPG"
    SportType.HOCKEY -> "PTS"
}

private fun minorSecondaryStatLabel(sportType: SportType): String = when (sportType) {
    SportType.BASKETBALL -> "RPG"
    SportType.FOOTBALL -> "TD"
    SportType.SOCCER -> "APG"
    SportType.HOCKEY -> "AST"
}

private fun formatCompactCount(count: Int): String = when {
    count >= 1_000_000 -> "${((count / 100_000.0).roundToInt() / 10.0)}M"
    count >= 1_000 -> "${((count / 100.0).roundToInt() / 10.0)}K"
    else -> count.toString()
}
