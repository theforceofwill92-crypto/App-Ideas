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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.components.TeamBadge
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RivalryHubScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.closeRivalryHub() }

    var originFilter by remember { mutableStateOf<RivalryOrigin?>(null) }
    val selectedTeamId = uiState.selectedRivalryTeamFilterId

    val activeRivalries = remember(uiState.rivalries, originFilter, selectedTeamId) {
        uiState.rivalries
            .filter { it.isActiveRivalry }
            .filter { r -> originFilter == null || originFilter in r.origins }
            .filter { r -> selectedTeamId == null || r.involves(selectedTeamId) }
            .sortedByDescending { it.intensity }
    }

    val emergingSparks = remember(uiState.rivalries, originFilter, selectedTeamId) {
        uiState.rivalries
            .filter { !it.isActiveRivalry }
            .filter { r -> originFilter == null || originFilter in r.origins }
            .filter { r -> selectedTeamId == null || r.involves(selectedTeamId) }
            .sortedByDescending { it.intensity }
    }

    val totalRivalryTicketSurge = remember(uiState.franchises) {
        Math.round(uiState.franchises.sumOf { it.rivalryTicketBonusMillions } * 10.0) / 10.0
    }
    val hottestRivalry = remember(uiState.rivalries) {
        uiState.rivalries.filter { it.isActiveRivalry }.maxByOrNull { it.intensity }
    }

    // Matchup Multiplier Preview state
    var previewHomeId by remember(uiState.franchises, selectedTeamId) {
        mutableStateOf(selectedTeamId ?: uiState.franchises.firstOrNull()?.id.orEmpty())
    }
    var previewAwayId by remember(uiState.franchises, previewHomeId) {
        val defaultOpp = hottestRivalry?.takeIf { it.involves(previewHomeId) }?.opponentIdOf(previewHomeId)
            ?: uiState.franchises.firstOrNull { it.id != previewHomeId }?.id.orEmpty()
        mutableStateOf(defaultOpp)
    }

    val previewHome = uiState.franchises.find { it.id == previewHomeId }
    val previewAway = uiState.franchises.find { it.id == previewAwayId }
    val previewImpact = remember(previewHome, previewAway, uiState.rivalries, uiState.cities) {
        if (previewHome != null && previewAway != null && previewHome.id != previewAway.id) {
            RivalryEngine.evaluateMatchupImpact(previewHome, previewAway, uiState.rivalries, uiState.cities, isPlayoffs = false)
        } else null
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rivalry_hub_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rivalry Matrix & Derbies",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${uiState.rivalries.count { it.isActiveRivalry }} Active Rivalries • Ticket & Morale Multipliers",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonAlert
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeRivalryHub() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_close_rivalry_hub")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to League Desk"
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
                .testTag("rivalry_hub_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Banner & KPI Strip
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CrimsonAlert.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        CrimsonAlert.copy(alpha = 0.20f),
                                        AmberWarning.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Rivalry Heat",
                                    tint = CrimsonAlert,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "LEAGUE RIVALRY INTELLIGENCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CrimsonAlert,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "Feuds intensify through Playoff History, Geographical Proximity, and Divisional Competition — unlocking boosted Ticket Revenue and amplified Fan Morale swings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RivalryKpiTile(
                                    label = "ACTIVE FEUDS",
                                    value = "${uiState.rivalries.count { it.isActiveRivalry }}",
                                    accent = CrimsonAlert,
                                    modifier = Modifier.weight(1f),
                                    testTag = "kpi_active_rivalries"
                                )
                                RivalryKpiTile(
                                    label = "TOP INTENSITY",
                                    value = "${hottestRivalry?.intensity ?: 0}/100",
                                    accent = GoldPrimary,
                                    modifier = Modifier.weight(1f),
                                    testTag = "kpi_top_rivalry_intensity"
                                )
                                RivalryKpiTile(
                                    label = "GATE SURGE",
                                    value = "+\$${totalRivalryTicketSurge}M",
                                    accent = EmeraldAccent,
                                    modifier = Modifier.weight(1f),
                                    testTag = "kpi_rivalry_gate_surge"
                                )
                            }
                        }
                    }
                }
            }

            // Origin Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FILTER BY RIVALRY CATALYST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = originFilter == null,
                                onClick = { originFilter = null },
                                label = { Text("All Origins") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chip_rivalry_origin_all")
                            )
                        }
                        items(RivalryOrigin.values().toList()) { origin ->
                            val isSel = originFilter == origin
                            FilterChip(
                                selected = isSel,
                                onClick = { originFilter = if (isSel) null else origin },
                                label = { Text(origin.displayName) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(origin.badgeColorHex))
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chip_rivalry_origin_${origin.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = "FILTER BY FRANCHISE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTeamId == null,
                                onClick = { viewModel.setRivalryTeamFilter(null) },
                                label = { Text("All Clubs") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chip_rivalry_team_all")
                            )
                        }
                        items(uiState.franchises, key = { it.id }) { team ->
                            val isSel = selectedTeamId == team.id
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setRivalryTeamFilter(if (isSel) null else team.id) },
                                label = { Text(team.nickname) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(team.colorHex))
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chip_rivalry_team_${team.id}")
                            )
                        }
                    }
                }
            }

            // Active Rivalries Section
            item {
                SectionHeader(title = "Active Rivalries (${activeRivalries.size})")
            }

            if (activeRivalries.isEmpty()) {
                item {
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
                                imageVector = Icons.Default.Shield,
                                contentDescription = "No Active Rivalries Match Filter",
                                tint = TextSecondary,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No active rivalries match this filter yet.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simulate divisional clashes, regional derbies, or playoff rounds to ignite new feuds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(activeRivalries, key = { it.id }) { rivalry ->
                    val teamA = uiState.franchises.find { it.id == rivalry.teamAId }
                    val teamB = uiState.franchises.find { it.id == rivalry.teamBId }
                    if (teamA != null && teamB != null) {
                        RivalryDetailCard(
                            rivalry = rivalry,
                            teamA = teamA,
                            teamB = teamB
                        )
                    }
                }
            }

            // Emerging Sparks (12..24 intensity)
            if (emergingSparks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Emerging Sparks (${emergingSparks.size})")
                }
                items(emergingSparks, key = { it.id }) { spark ->
                    val teamA = uiState.franchises.find { it.id == spark.teamAId }
                    val teamB = uiState.franchises.find { it.id == spark.teamBId }
                    if (teamA != null && teamB != null) {
                        EmergingSparkCard(
                            rivalry = spark,
                            teamA = teamA,
                            teamB = teamB
                        )
                    }
                }
            }

            // Matchup Multiplier Previewer
            if (uiState.franchises.size >= 2) {
                item {
                    SectionHeader(title = "Matchup Gate & Morale Previewer")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Inspect projected Ticket Revenue Multiplier and Fan Morale swings if any two clubs collide next week:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "HOME ARENA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.franchises, key = { "h_${it.id}" }) { t ->
                                    FilterChip(
                                        selected = previewHomeId == t.id,
                                        onClick = {
                                            previewHomeId = t.id
                                            if (previewAwayId == t.id) {
                                                previewAwayId = uiState.franchises.firstOrNull { it.id != t.id }?.id.orEmpty()
                                            }
                                        },
                                        label = { Text(t.nickname) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    )
                                }
                            }
                            Text(
                                text = "VISITING CHALLENGER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.franchises.filter { it.id != previewHomeId }, key = { "a_${it.id}" }) { t ->
                                    FilterChip(
                                        selected = previewAwayId == t.id,
                                        onClick = { previewAwayId = t.id },
                                        label = { Text(t.nickname) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    )
                                }
                            }

                            if (previewImpact != null && previewHome != null && previewAway != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (previewImpact.isActiveRivalry) CrimsonAlert.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (previewImpact.isActiveRivalry) {
                                                "${previewImpact.rivalry?.rivalryName} (${previewImpact.rivalry?.tier?.title})"
                                            } else {
                                                "Standard Matchup (${previewHome.cityName} vs ${previewAway.cityName})"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (previewImpact.isActiveRivalry) CrimsonAlert else MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            MultiplierStatBox(
                                                title = "TICKET GATE",
                                                value = "${previewImpact.ticketRevenueMultiplier}x",
                                                subtitle = if (previewImpact.homeBonusTicketMillions > 0) "+\$${previewImpact.homeBonusTicketMillions}M gate" else "Base gate",
                                                color = EmeraldAccent,
                                                modifier = Modifier.weight(1f)
                                            )
                                            MultiplierStatBox(
                                                title = "MORALE SWING",
                                                value = "${previewImpact.moraleMultiplier}x",
                                                subtitle = "+${previewImpact.winMoraleDelta}W / ${previewImpact.lossMoraleDelta}L",
                                                color = GoldPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            MultiplierStatBox(
                                                title = "CROWD HYPE",
                                                value = "+${previewImpact.crowdHypeBoost}",
                                                subtitle = "Home boost",
                                                color = CyanAccent,
                                                modifier = Modifier.weight(1f)
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
    }
}

@Composable
private fun RivalryKpiTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier.testTag(testTag),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = accent
            )
        }
    }
}

@Composable
private fun RivalryDetailCard(
    rivalry: TeamRivalry,
    teamA: Franchise,
    teamB: Franchise
) {
    val tierColor = Color(rivalry.tier.badgeColorHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_rivalry_${rivalry.id}")
            .border(1.2.dp, tierColor.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Title + Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rivalry.rivalryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "H2H Series: ${teamA.nickname} ${rivalry.teamAWins} – ${rivalry.teamBWins} ${teamB.nickname} • ${rivalry.playoffMeetingsCount} Playoff Clashes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = tierColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = rivalry.tier.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Teams Matchup Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TeamBadge(
                    cityName = teamA.cityName,
                    nickname = teamA.nickname,
                    colorHex = teamA.colorHex,
                    secondaryColorHex = teamA.secondaryColorHex,
                    subtitle = "${teamA.division.shortCode} (${teamA.wins}-${teamA.losses})",
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = tierColor,
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                TeamBadge(
                    cityName = teamB.cityName,
                    nickname = teamB.nickname,
                    colorHex = teamB.colorHex,
                    secondaryColorHex = teamB.secondaryColorHex,
                    subtitle = "${teamB.division.shortCode} (${teamB.wins}-${teamB.losses})",
                    modifier = Modifier.weight(1f)
                )
            }

            // Intensity Meter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RIVALRY INTENSITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${rivalry.intensity} / 100",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = tierColor
                    )
                }
                LinearProgressIndicator(
                    progress = { (rivalry.intensity / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = tierColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }

            // Origins Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rivalry.origins.forEach { origin ->
                    val oColor = Color(origin.badgeColorHex)
                    Surface(
                        color = oColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, oColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = origin.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = oColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Multipliers Impact Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MultiplierStatBox(
                    title = "TICKET REVENUE",
                    value = "${rivalry.ticketRevenueMultiplier}x",
                    subtitle = "+${((rivalry.ticketRevenueMultiplier - 1.0) * 100).toInt()}% gate surge",
                    color = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                MultiplierStatBox(
                    title = "MORALE SWING",
                    value = "${rivalry.moraleMultiplier}x",
                    subtitle = "High-stakes morale",
                    color = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MultiplierStatBox(
                    title = "HOME HYPE",
                    value = "+${rivalry.preGameCrowdHypeBoost} Boost",
                    subtitle = "${rivalry.closeGamesCount} thrillers",
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            if (rivalry.lastMeetingSummary.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rivalry.lastMeetingSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiplierStatBox(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmergingSparkCard(
    rivalry: TeamRivalry,
    teamA: Franchise,
    teamB: Franchise
) {
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
                    text = "${teamA.cityName} ${teamA.nickname} vs ${teamB.cityName} ${teamB.nickname}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${rivalry.rivalryName} • ${rivalry.origins.joinToString(" + ") { it.displayName }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = AmberWarning.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${rivalry.intensity}/25 Heat",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = AmberWarning,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
