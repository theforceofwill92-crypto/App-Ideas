package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
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
fun RecordBookScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.openRecordBook(false) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Leaders & Box, 1: Player Records, 2: Franchise Records, 3: Hall of Fame, 4: Weekly Honors
    var leadersSubMode by remember { mutableStateOf(0) } // 0: Season Leaders, 1: Latest Week Box Scores

    val statLabels = remember(uiState.sportType) { RecordBookEngine.getStatLabels(uiState.sportType) }
    val sortedSeasonLeaders = remember(uiState.seasonPlayerStats) {
        uiState.seasonPlayerStats.values.sortedWith(
            compareByDescending<PlayerSeasonStats> { it.totalPoints }
                .thenByDescending { it.ppg }
                .thenByDescending { it.totalAssists }
        )
    }
    val sortedWeekBoxScores = remember(uiState.currentWeekPlayerStats) {
        uiState.currentWeekPlayerStats.sortedByDescending { it.efficiencyScore }
    }
    val sortedHallOfFame = remember(uiState.hallOfFame) {
        uiState.hallOfFame.sortedByDescending { it.inductionYear }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("record_book_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LEAGUE ARCHIVES & RECORD BOOK",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Stats, Records & Hall of Fame",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openRecordBook(false) },
                        modifier = Modifier
                            .testTag("btn_close_record_book")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary
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
            // Latest Player of the Week Spotlight
            val latestPotw = uiState.weeklyAwards.firstOrNull()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("potw_spotlight_card")
                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(alpha = 0.16f), RoyalBlueAccent.copy(alpha = 0.08f))
                            )
                        )
                        .padding(14.dp)
                ) {
                    if (latestPotw != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Player of the Week",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (latestPotw.isPlayoffs) "PLAYOFF PLAYER OF THE WEEK • W${latestPotw.week}" else "PLAYER OF THE WEEK • WEEK ${latestPotw.week}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "${latestPotw.playerName} (${latestPotw.position}) • ${latestPotw.franchiseName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${latestPotw.statLineSummary} vs ${latestPotw.opponentName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SEASON ${uiState.currentYear} STATS ENGINE READY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "Simulate Week 1 from the Commissioner Desk to crown the first Player of the Week and log box scores.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Tab Strip
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GoldPrimary,
                modifier = Modifier.testTag("record_book_tabs")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stats & Box", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_rb_leaders")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Player Records", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_rb_player_records")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Franchise Records", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_rb_franchise_records")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Hall of Fame (${sortedHallOfFame.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_rb_hall_of_fame")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Awards & Honors", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_rb_weekly_awards")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("record_book_content"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    // TAB 0: SEASON LEADERS & WEEKLY BOX SCORES
                    0 -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = leadersSubMode == 0,
                                    onClick = { leadersSubMode = 0 },
                                    label = { Text("Season Leaders (${sortedSeasonLeaders.size})") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chip_season_leaders")
                                )
                                FilterChip(
                                    selected = leadersSubMode == 1,
                                    onClick = { leadersSubMode = 1 },
                                    label = { Text("Last Week Box (${sortedWeekBoxScores.size})") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chip_week_box_scores")
                                )
                            }
                        }

                        if (leadersSubMode == 0) {
                            if (sortedSeasonLeaders.isEmpty()) {
                                item {
                                    EmptyArchiveCard(
                                        title = "No Season Stats Logged Yet",
                                        subtitle = "Simulate at least one regular season or playoff week to track every roster player's stats."
                                    )
                                }
                            } else {
                                item {
                                    SectionHeader(title = "Season ${uiState.currentYear} Statistical Leaders")
                                }
                                itemsIndexed(sortedSeasonLeaders) { index, stat ->
                                    SeasonLeaderRowCard(rank = index + 1, stat = stat, labels = statLabels)
                                }
                            }
                        } else {
                            if (sortedWeekBoxScores.isEmpty()) {
                                item {
                                    EmptyArchiveCard(
                                        title = "No Match Box Scores Available",
                                        subtitle = "Advance a regular season or playoff week to inspect every player's game-by-game performance."
                                    )
                                }
                            } else {
                                item {
                                    SectionHeader(title = "Latest Week Box Scores (All Players)")
                                }
                                items(sortedWeekBoxScores) { line ->
                                    GameBoxScoreRowCard(line = line, labels = statLabels)
                                }
                            }
                        }
                    }

                    // TAB 1: PLAYER RECORDS (GAME, SEASON, CAREER)
                    1 -> {
                        item {
                            SectionHeader(title = "Player Single-Game Records")
                        }
                        items(uiState.recordBook.playerGameRecords) { rec ->
                            RecordEntryCard(entry = rec)
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SectionHeader(title = "Player Single-Season Records")
                        }
                        items(uiState.recordBook.playerSeasonRecords) { rec ->
                            RecordEntryCard(entry = rec)
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SectionHeader(title = "Player All-Time Career Records")
                        }
                        items(uiState.recordBook.playerCareerRecords) { rec ->
                            RecordEntryCard(entry = rec)
                        }
                    }

                    // TAB 2: FRANCHISE RECORDS (GAME, SEASON)
                    2 -> {
                        item {
                            SectionHeader(title = "Franchise Single-Game Records")
                        }
                        items(uiState.recordBook.franchiseGameRecords) { rec ->
                            RecordEntryCard(entry = rec)
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SectionHeader(title = "Franchise Single-Season Records")
                        }
                        items(uiState.recordBook.franchiseSeasonRecords) { rec ->
                            RecordEntryCard(entry = rec)
                        }
                    }

                    // TAB 3: HALL OF FAME GALLERY
                    3 -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "APEX IMMORTALS GALLERY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = GoldPrimary
                                        )
                                        Text(
                                            text = "New Hall of Fame classes are enshrined annually after Free Agency during the Off-Season Summit.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        items(sortedHallOfFame, key = { it.id }) { inductee ->
                            HallOfFameInducteeCard(inductee = inductee, labels = statLabels)
                        }
                    }

                    // TAB 4: YEARLY AWARDS GALA & WEEKLY HONORS HISTORY
                    4 -> {
                        if (uiState.pastAwardsCeremonies.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Annual Awards Show Archives")
                            }
                            items(uiState.pastAwardsCeremonies) { ceremony ->
                                YearlyAwardsArchiveCard(ceremony = ceremony)
                            }
                        }

                        item {
                            SectionHeader(title = "Player of the Week Honors Log")
                        }
                        if (uiState.weeklyAwards.isEmpty()) {
                            item {
                                EmptyArchiveCard(
                                    title = "No Weekly Honors Awarded Yet",
                                    subtitle = "Each simulated regular season or playoff week automatically awards Player of the Week to the highest-impact star."
                                )
                            }
                        } else {
                            items(uiState.weeklyAwards) { award ->
                                WeeklyAwardRowCard(award = award)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonLeaderRowCard(
    rank: Int,
    stat: PlayerSeasonStats,
    labels: SportStatLabels
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("season_leader_${stat.playerId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (rank <= 3) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(34.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stat.playerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = RoyalBlueAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${stat.position} • ${stat.overallRating} OVR",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlueAccent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "${stat.franchiseName} • ${stat.gamesPlayed} REG${if (stat.playoffGamesPlayed > 0) " + ${stat.playoffGamesPlayed} PO" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${stat.ppg} ${labels.primaryShort}/G",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary
                )
                Text(
                    text = "${stat.totalPoints} ${labels.primaryShort} • ${stat.rpg} ${labels.secondaryShort} • ${stat.apg} ${labels.tertiaryShort}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldAccent,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun GameBoxScoreRowCard(
    line: PlayerGameStatLine,
    labels: SportStatLabels
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("box_score_${line.playerId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${line.playerName} (${line.position})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (line.isPlayoffs) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = GoldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "PLAYOFFS",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${line.minutes} MIN • ${line.efficiencyScore} EFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${line.franchiseName} ${if (line.teamWon) "W" else "L"} vs ${line.opponentName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${line.points} ${labels.primaryShort} | ${line.rebounds} ${labels.secondaryShort} | ${line.assists} ${labels.tertiaryShort} | ${line.stealsBlocks} ${labels.defenseShort}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = EmeraldAccent
            )
        }
    }
}

@Composable
private fun RecordEntryCard(entry: RecordEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_entry_${entry.id}")
            .border(
                width = 1.dp,
                color = if (entry.isActiveEraRecord) GoldPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isActiveEraRecord) GoldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.recordTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    if (entry.isActiveEraRecord) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = EmeraldAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "NEW ERA",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.holderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${entry.teamName} • ${entry.contextLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                color = if (entry.isActiveEraRecord) EmeraldAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
            ) {
                Text(
                    text = entry.valueDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = if (entry.isActiveEraRecord) EmeraldAccent else GoldPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun HallOfFameInducteeCard(
    inductee: HallOfFameInductee,
    labels: SportStatLabels
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hof_inductee_${inductee.id}")
            .border(1.5.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.22f))
                            .border(1.5.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = inductee.position,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CLASS OF ${inductee.inductionYear} • ${inductee.legacyTier.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                        Text(
                            text = inductee.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = inductee.primaryFranchiseName,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    color = GoldPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${inductee.championships}x RINGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HofStatPill(label = "Career ${labels.primaryShort}", value = "${inductee.careerPoints}", modifier = Modifier.weight(1f))
                HofStatPill(label = "Career ${labels.secondaryShort}", value = "${inductee.careerRebounds}", modifier = Modifier.weight(1f))
                HofStatPill(label = "Career ${labels.tertiaryShort}", value = "${inductee.careerAssists}", modifier = Modifier.weight(1f))
                HofStatPill(label = "Peak OVR", value = "${inductee.peakRating}", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = inductee.bio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${inductee.inductionQuote}\"",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HofStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = EmeraldAccent)
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun WeeklyAwardRowCard(award: WeeklyPlayerAward) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_award_${award.seasonYear}_w${award.week}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = GoldPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (award.isPlayoffs) "PO W${award.week}" else "W${award.week}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${award.playerName} (${award.position}) • ${award.franchiseName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${award.statLineSummary} vs ${award.opponentName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldAccent,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${award.impactScore} EFF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = CyanAccent
            )
        }
    }
}

@Composable
private fun EmptyArchiveCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun YearlyAwardsArchiveCard(ceremony: YearlyAwardsCeremony) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("yearly_awards_archive_${ceremony.year}")
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ceremony.ceremonyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary
                )
                Surface(color = GoldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "${ceremony.year} GALA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ceremony.awards.forEach { w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = w.awardType.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(w.awardType.accentColorHex),
                        modifier = Modifier.width(82.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${w.winnerName} (${w.franchiseName})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = w.primaryStatLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
