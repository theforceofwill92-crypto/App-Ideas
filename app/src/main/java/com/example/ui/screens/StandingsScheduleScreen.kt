package com.example.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Conference
import com.example.model.Division
import com.example.model.Franchise
import com.example.model.SeasonPhase
import com.example.model.calculatePlayoffSeeds
import com.example.ui.components.SectionHeader
import com.example.ui.components.TeamBadge
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RoyalBlueAccent
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import com.example.viewmodel.StandingsSortMode
import com.example.viewmodel.StandingsViewModel

@Composable
fun StandingsScheduleScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier,
    standingsViewModel: StandingsViewModel? = null
) {
    val context = LocalContext.current
    val effectiveStandingsVm = standingsViewModel ?: remember(context.applicationContext) {
        StandingsViewModel(context.applicationContext as Application)
    }

    LaunchedEffect(uiState.franchises, uiState.currentYear, uiState.currentWeek) {
        effectiveStandingsVm.syncStandingsFromFranchises(
            franchises = uiState.franchises,
            seasonYear = uiState.currentYear,
            currentWeek = uiState.currentWeek
        )
    }

    val roomStandings by effectiveStandingsVm.liveStandings.collectAsStateWithLifecycle()
    val sortMode by effectiveStandingsVm.sortMode.collectAsStateWithLifecycle()

    var standingsViewMode by remember { mutableStateOf(0) } // 0: Overall, 1: By Conference, 2: By Division

    val playoffRules = uiState.ruleBook.playoffRules
    val playoffSeeds = remember(uiState.franchises, playoffRules) {
        calculatePlayoffSeeds(uiState.franchises, playoffRules)
    }
    val byeTeams = remember(playoffSeeds) {
        playoffSeeds.filter { it.hasBye }.map { it.franchise }
    }

    val sortedTeams = remember(uiState.franchises, roomStandings, sortMode) {
        val franchiseMap = uiState.franchises.associateBy { it.id }
        val roomOrdered = roomStandings.mapNotNull { standing ->
            franchiseMap[standing.franchiseId]?.copy(
                wins = standing.wins,
                losses = standing.losses,
                pointsFor = standing.pointsFor,
                pointsAgainst = standing.pointsAgainst
            )
        }
        if (roomOrdered.size == uiState.franchises.size && roomOrdered.isNotEmpty()) {
            roomOrdered
        } else {
            when (sortMode) {
                StandingsSortMode.WIN_PCT -> uiState.franchises.sortedWith(
                    compareByDescending<Franchise> { it.winPct }
                        .thenByDescending { it.pointsFor - it.pointsAgainst }
                        .thenByDescending { it.wins }
                )
                StandingsSortMode.POINT_DIFF -> uiState.franchises.sortedWith(
                    compareByDescending<Franchise> { it.pointsFor - it.pointsAgainst }
                        .thenByDescending { it.winPct }
                )
                StandingsSortMode.WINS -> uiState.franchises.sortedWith(
                    compareByDescending<Franchise> { it.wins }
                        .thenByDescending { it.pointsFor - it.pointsAgainst }
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("standings_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "COMPETITION TABLE",
                style = MaterialTheme.typography.labelSmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Standings & Conferences",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track regular season division races, conference seeding, and historical dynasties across the league.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.openRecordBook(true) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_standings_open_record_book")
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Record Book, Player Stats & Hall of Fame (${uiState.hallOfFame.size})",
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.openScheduleCalendarScreen(true) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_standings_open_schedule")
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Season Schedule, Holidays & Global Slate (${uiState.ruleBook.regularSeasonWeeks}W)",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Room DB Live League Standings Summary & Sort Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("standings_room_summary_card")
                    .border(1.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Live Room Standings Table",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE LEAGUE STANDINGS • ROOM DB",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent,
                                letterSpacing = 0.7.sp
                            )
                        }
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${roomStandings.size.coerceAtLeast(sortedTeams.size)} Clubs • Y${uiState.currentYear}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    val topLeader = roomStandings.firstOrNull()
                    if (topLeader != null) {
                        val leaderDiff = topLeader.pointDifferential
                        val leaderDiffStr = if (leaderDiff >= 0) "+$leaderDiff" else "$leaderDiff"
                        Text(
                            text = "League Leader: ${topLeader.teamFullName} (${topLeader.wins}-${topLeader.losses}, Diff $leaderDiffStr)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StandingsSortMode.entries.forEach { mode ->
                            val selected = sortMode == mode
                            Surface(
                                color = if (selected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { effectiveStandingsVm.setSortMode(mode) }
                                    .testTag("standings_sort_${mode.name}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (selected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Reigning Champion Banner if Season Completed
        if (uiState.seasonChampion != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("champion_coronation_banner")
                        .border(2.dp, GoldPrimary, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COMMISSIONER CUP CHAMPION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Text(
                                text = uiState.seasonChampion.teamFullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Runner-Up: ${uiState.seasonRunnerUp?.teamFullName ?: "TBD"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Playoff Bracket Section (if in playoffs or concluded)
        if (uiState.playoffMatches.isNotEmpty()) {
            item {
                SectionHeader(title = "Championship Playoff Bracket", modifier = Modifier.padding(top = 8.dp))
            }
            if (byeTeams.isNotEmpty() && uiState.playoffMatches.any { it.roundName.contains("Wild Card", ignoreCase = true) && !it.isPlayed }) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = GoldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1ST-ROUND BYES:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${byeTeams.joinToString(" & ") { it.teamFullName }} advance directly to Semifinals",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            itemsIndexed(uiState.playoffMatches) { index, match ->
                val home = uiState.franchises.find { it.id == match.homeTeamId }
                val away = uiState.franchises.find { it.id == match.awayTeamId }
                if (home != null && away != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = match.roundName.ifEmpty { if (index < 2) "SEMIFINAL MATCH ${index + 1}" else "CHAMPIONSHIP FINALS" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${home.teamFullName} vs ${away.teamFullName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (match.isPlayed) "${match.homeScore} - ${match.awayScore}" else "PENDING",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (match.isPlayed) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (match.rivalryTitle != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = CrimsonAlert.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f)),
                                    modifier = Modifier.testTag("playoff_rivalry_badge_${match.id}")
                                ) {
                                    Text(
                                        text = "${match.rivalryTitle} • ${match.ticketRevenueMultiplier}x Gate • ${match.moraleMultiplier}x Morale",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonAlert,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // View Mode Tabs: [Overall Table] | [By Conference] | [By Division]
        item {
            TabRow(
                selectedTabIndex = standingsViewMode,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("tab_row_standings_mode")
            ) {
                Tab(
                    selected = standingsViewMode == 0,
                    onClick = { standingsViewMode = 0 },
                    text = { Text("League Overall", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_standings_overall")
                )
                Tab(
                    selected = standingsViewMode == 1,
                    onClick = { standingsViewMode = 1 },
                    text = { Text("Conferences", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_standings_conferences")
                )
                Tab(
                    selected = standingsViewMode == 2,
                    onClick = { standingsViewMode = 2 },
                    text = { Text("Divisions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_standings_divisions")
                )
            }
        }

        // Mode 0: Overall Standings Table
        if (standingsViewMode == 0) {
            itemsIndexed(sortedTeams) { index, team ->
                val rank = index + 1
                val seedInfo = playoffSeeds.find { it.franchise.id == team.id }
                val isPlayoffSeed = seedInfo != null
                StandingRowCard(
                    rank = rank,
                    team = team,
                    isPlayoffSeed = isPlayoffSeed,
                    showConferenceTag = true,
                    hasBye = seedInfo?.hasBye == true,
                    playoffTag = seedInfo?.eligibilityTag
                )
            }
        }

        // Mode 1: By Conference Standings
        if (standingsViewMode == 1) {
            uiState.ruleBook.effectiveConferences(uiState.franchises).forEach { conf ->
                val confTeams = sortedTeams.filter { it.conference == conf }
                item {
                    Surface(
                        color = Color(conf.badgeColorHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = conf.displayName.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(conf.badgeColorHex),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${confTeams.size} Teams",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(conf.badgeColorHex)
                            )
                        }
                    }
                }

                if (confTeams.isEmpty()) {
                    item {
                        Text(
                            text = "No teams in this conference.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    itemsIndexed(confTeams) { index, team ->
                        val confSeed = index + 1
                        val isPlayoffSeed = confSeed <= 2 // Top seeds from conference
                        StandingRowCard(
                            rank = confSeed,
                            team = team,
                            isPlayoffSeed = isPlayoffSeed,
                            showConferenceTag = false,
                            subtitlePrefix = "${team.division.shortCode} • "
                        )
                    }
                }
            }
        }

        // Mode 2: By Division Standings
        if (standingsViewMode == 2) {
            Division.values().forEach { division ->
                val divTeams = sortedTeams.filter { it.division == division }
                if (divTeams.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 6.dp)
                                .border(1.dp, Color(division.conference.badgeColorHex).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = division.displayName.uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(division.conference.badgeColorHex)
                                    )
                                    Text(
                                        text = "${division.conference.displayName} • ${division.geographicRegion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                                Surface(
                                    color = Color(division.conference.badgeColorHex).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${divTeams.size} Teams",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(division.conference.badgeColorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(divTeams) { index, team ->
                        val isLeader = index == 0
                        StandingRowCard(
                            rank = index + 1,
                            team = team,
                            isPlayoffSeed = isLeader,
                            showConferenceTag = false,
                            isDivisionLeader = isLeader
                        )
                    }
                }
            }
        }

        // Historical Dynasties Archive
        if (uiState.valuationHistory.isNotEmpty()) {
            item {
                SectionHeader(title = "Dynasty Archives & Valuation Records", modifier = Modifier.padding(top = 16.dp))
            }
            itemsIndexed(uiState.valuationHistory) { _, hist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Season ${hist.year} Champion: ${hist.championName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Valuation: \$${hist.valuationMillions}M • Total Revenue: \$${hist.totalRevenueMillions}M",
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

@Composable
private fun StandingRowCard(
    rank: Int,
    team: Franchise,
    isPlayoffSeed: Boolean,
    showConferenceTag: Boolean,
    isDivisionLeader: Boolean = false,
    hasBye: Boolean = false,
    playoffTag: String? = null,
    subtitlePrefix: String = ""
) {
    val diff = team.pointsFor - team.pointsAgainst
    val diffFormatted = if (diff >= 0) "+$diff" else "$diff"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag("standing_row_${team.id}")
            .border(
                1.dp,
                if (hasBye) GoldPrimary else if (isDivisionLeader || isPlayoffSeed) GoldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (hasBye) GoldPrimary.copy(alpha = 0.12f)
            else if (isDivisionLeader) RoyalBlueAccent.copy(alpha = 0.10f)
            else if (isPlayoffSeed) GoldPrimary.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
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
                color = if (hasBye) GoldPrimary else if (isPlayoffSeed) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamBadge(
                        cityName = team.cityName,
                        nickname = team.nickname,
                        colorHex = team.colorHex,
                        secondaryColorHex = team.secondaryColorHex
                    )
                    if (hasBye) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = GoldPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "BYE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (playoffTag != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (isDivisionLeader || playoffTag.contains("DIV")) RoyalBlueAccent.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isDivisionLeader || playoffTag.contains("DIV")) "DIV" else "WC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDivisionLeader || playoffTag.contains("DIV")) RoyalBlueAccent else CyanAccent,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (isDivisionLeader) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leader",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (showConferenceTag) {
                    Text(
                        text = "${team.conference.shortCode} • ${team.division.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(team.conference.badgeColorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 48.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${team.wins} - ${team.losses}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("standing_record_${team.id}")
                )
                Text(
                    text = "${subtitlePrefix}${String.format("%.3f", team.winPct)} • PF ${team.pointsFor}/PA ${team.pointsAgainst} • Diff: $diffFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (diff >= 0) EmeraldAccent else CrimsonAlert,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("standing_diff_${team.id}")
                )
            }
        }
    }
}
