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
import com.example.ui.components.TeamBadge
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.openScheduleCalendarScreen(false)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("schedule_calendar_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LEAGUE SCHEDULE & CALENDAR DESK",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Season Calendar, Holidays & Global Slate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openScheduleCalendarScreen(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_close_schedule_calendar")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Commissioner Desk"
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
                .testTag("schedule_calendar_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScheduleCalendarEditorContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    isEmbeddedInOffSeason = false,
                    onProceedNext = null
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleCalendarEditorContent(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    isEmbeddedInOffSeason: Boolean = false,
    onProceedNext: (() -> Unit)? = null
) {
    val plan = uiState.seasonScheduleState.upcomingPlan ?: uiState.seasonScheduleState.activeSeasonSchedule
    val franchises = uiState.franchises

    if (plan == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_schedule_empty"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Initialize Upcoming ${uiState.currentYear + 1} Regular Season Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Button(
                    onClick = { viewModel.initializeOrRefreshUpcomingSchedule() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_init_upcoming_schedule")
                ) {
                    Text("Generate Post-Draft Schedule Draft", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
        return
    }

    var selectedWeek by remember(plan.targetYear, plan.regularSeasonWeeks) {
        mutableIntStateOf(1.coerceIn(1, plan.regularSeasonWeeks))
    }
    var filterMode by remember { mutableIntStateOf(0) } // 0: Week, 1: Holiday Slates, 2: International
    var selectedSeasonWeeksCandidate by remember(uiState.ruleBook.regularSeasonWeeks) {
        mutableIntStateOf(uiState.ruleBook.regularSeasonWeeks)
    }

    var editingTeamsSlot by remember { mutableStateOf<ScheduledGameSlot?>(null) }
    var editingHolidaySlot by remember { mutableStateOf<ScheduledGameSlot?>(null) }
    var editingIntlSlot by remember { mutableStateOf<ScheduledGameSlot?>(null) }

    val statusColor = when (plan.approvalStatus) {
        ScheduleApprovalStatus.APPROVED_BY_BOARD -> EmeraldAccent
        ScheduleApprovalStatus.APPROVED_BY_OVERRIDE -> GoldPrimary
        ScheduleApprovalStatus.REJECTED_REVISION_REQUIRED -> CrimsonAlert
        ScheduleApprovalStatus.DRAFT_IN_PROGRESS -> CyanAccent
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Approval Status & Executive Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_schedule_approval_status")
                .border(1.3.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEmbeddedInOffSeason) "PHASE 8 • POST-DRAFT SCHEDULE APPROVAL" else "UPCOMING REGULAR SEASON SLATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                        Text(
                            text = "Season ${plan.targetYear} Calendar (${plan.regularSeasonWeeks} Weeks)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("badge_schedule_approval_status")
                    ) {
                        Text(
                            text = plan.approvalStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScheduleMetricChip("${plan.slots.size} Games", CyanAccent)
                    ScheduleMetricChip("${plan.holidayShowcaseCount} Holidays", GoldPrimary)
                    ScheduleMetricChip("${plan.internationalGameCount} International", EmeraldAccent)
                    ScheduleMetricChip("+\$${plan.projectedBroadcastBonusMillions}M TV", VioletAccent)
                    ScheduleMetricChip("+${plan.projectedGlobalReachGainMillions}M Fans", CyanAccent)
                }

                Text(
                    text = plan.lastVoteSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (plan.conflictWarnings.isNotEmpty()) {
                    Surface(
                        color = (if (plan.hasValidationErrors) CrimsonAlert else GoldPrimary).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            (if (plan.hasValidationErrors) CrimsonAlert else GoldPrimary).copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_schedule_warnings")
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            plan.conflictWarnings.forEach { warn ->
                                Text(
                                    text = "• $warn",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (plan.hasValidationErrors) CrimsonAlert else GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.autoGenerateBalancedUpcomingSchedule() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_schedule_autobuild")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Auto-Balance Schedule",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rebuild", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.submitUpcomingScheduleForApproval() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.58f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_submit_schedule_approval")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = "Submit Schedule for Board Approval",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (plan.isApproved) "Re-Submit to Board" else "Submit to Board",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Expandable Regular Season by Board Vote Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_season_length_vote")
                .border(1.dp, RoyalBlueAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allStarWk = ScheduleCalendarEngine.allStarWeekFor(uiState.ruleBook.regularSeasonWeeks)
                Text(
                    text = "REGULAR SEASON LENGTH EXPANSION (BOARD VOTE)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = RoyalBlueAccent
                )
                Text(
                    text = "Current Charter: ${uiState.ruleBook.regularSeasonWeeks} Weeks (All-Star Break Week $allStarWk). Expanding adds broadcast & gate inventory while increasing player workload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ScheduleCalendarEngine.ALLOWED_SEASON_LENGTHS) { weeksOption ->
                        val selected = selectedSeasonWeeksCandidate == weeksOption
                        FilterChip(
                            selected = selected,
                            onClick = { selectedSeasonWeeksCandidate = weeksOption },
                            label = {
                                Text(
                                    text = if (weeksOption == uiState.ruleBook.regularSeasonWeeks) "${weeksOption}W (Live)" else "${weeksOption}W",
                                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chip_season_weeks_$weeksOption")
                        )
                    }
                }
                Button(
                    onClick = { viewModel.proposeRegularSeasonLengthVote(selectedSeasonWeeksCandidate) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_vote_season_length")
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = "Vote on Regular Season Length",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call Board Vote: Set Season to ${selectedSeasonWeeksCandidate} Weeks",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 3. Calendar Scope Filter + Week Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0 to "By Week",
                1 to "Holidays (${plan.holidayShowcaseCount})",
                2 to "Global (${plan.internationalGameCount})"
            ).forEach { (idx, label) ->
                val active = filterMode == idx
                FilterChip(
                    selected = active,
                    onClick = { filterMode = idx },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_schedule_filter_$idx")
                )
            }
        }

        if (filterMode == 0) {
            val allStarWk = ScheduleCalendarEngine.allStarWeekFor(plan.regularSeasonWeeks)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items((1..plan.regularSeasonWeeks).toList()) { wk ->
                    val weekSlots = plan.slotsForWeek(wk)
                    val hasHol = weekSlots.any { it.isHolidayGame }
                    val hasIntl = weekSlots.any { it.isInternationalGame }
                    val selected = selectedWeek == wk
                    Surface(
                        color = when {
                            selected -> GoldPrimary
                            hasHol -> CrimsonAlert.copy(alpha = 0.16f)
                            hasIntl -> EmeraldAccent.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                selected -> GoldPrimary
                                hasHol -> CrimsonAlert.copy(alpha = 0.55f)
                                hasIntl -> EmeraldAccent.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            }
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { selectedWeek = wk }
                            .testTag("chip_schedule_week_$wk")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "W$wk",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                            val tag = when {
                                wk == allStarWk -> "ASG"
                                hasHol && hasIntl -> "HOL+GLB"
                                hasHol -> "HOL"
                                hasIntl -> "GLB"
                                else -> "REG"
                            }
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Filtered Slots List
        val displayedSlots = remember(plan, filterMode, selectedWeek) {
            when (filterMode) {
                1 -> plan.slots.filter { it.isHolidayGame }.ifEmpty { plan.slotsForWeek(selectedWeek) }
                2 -> plan.slots.filter { it.isInternationalGame }.ifEmpty { plan.slotsForWeek(selectedWeek) }
                else -> plan.slotsForWeek(selectedWeek)
            }
        }

        displayedSlots.forEach { slot ->
            val home = franchises.find { it.id == slot.homeTeamId }
            val away = franchises.find { it.id == slot.awayTeamId }
            if (home != null && away != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_schedule_match_${slot.id}")
                        .border(
                            1.dp,
                            when {
                                slot.isHolidayGame -> GoldPrimary.copy(alpha = 0.65f)
                                slot.isInternationalGame -> EmeraldAccent.copy(alpha = 0.65f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                            },
                            RoundedCornerShape(14.dp)
                        ),
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
                            Text(
                                text = "WEEK ${slot.week}" + if (slot.rivalryTitle != null) " • ${slot.rivalryTitle}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${slot.projectedGateMultiplier}x Gate" +
                                    if (slot.projectedTvBonusMillions > 0.0) " • +\$${slot.projectedTvBonusMillions}M TV" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TeamBadge(
                                cityName = home.cityName,
                                nickname = home.nickname,
                                colorHex = home.colorHex,
                                secondaryColorHex = home.secondaryColorHex,
                                subtitle = "HOME",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "VS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            TeamBadge(
                                cityName = away.cityName,
                                nickname = away.nickname,
                                colorHex = away.colorHex,
                                secondaryColorHex = away.secondaryColorHex,
                                subtitle = "AWAY",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (slot.isHolidayGame || slot.isInternationalGame) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (slot.isHolidayGame) {
                                    Surface(
                                        color = GoldPrimary.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("badge_slot_holiday_${slot.id}")
                                    ) {
                                        Text(
                                            text = "${slot.holidayTentpole.displayName} • Marquee Rivalry",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (slot.isInternationalGame) {
                                    Surface(
                                        color = EmeraldAccent.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("badge_slot_intl_${slot.id}")
                                    ) {
                                        Text(
                                            text = "Global Showcase: ${slot.internationalVenueLabel ?: "Neutral Site"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.flipUpcomingMatchHomeAway(slot.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_flip_ha_${slot.id}")
                            ) {
                                Text("Flip H/A", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { editingTeamsSlot = slot },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_edit_matchup_${slot.id}")
                            ) {
                                Text("Edit / Swap", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { editingHolidaySlot = slot },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_set_holiday_${slot.id}")
                            ) {
                                Text("Holiday", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { editingIntlSlot = slot },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_set_intl_${slot.id}")
                            ) {
                                Text("Global", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        if (isEmbeddedInOffSeason && onProceedNext != null) {
            Button(
                onClick = onProceedNext,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("btn_schedule_to_summary")
            ) {
                Text("Proceed to Final Parity & Launch ➔", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }

    // Dialog 1: Edit Teams / Move Week
    editingTeamsSlot?.let { slot ->
        var replaceHome by remember(slot.id) { mutableStateOf(false) }
        var targetWeek by remember(slot.id) { mutableIntStateOf(slot.week) }
        AlertDialog(
            onDismissRequest = { editingTeamsSlot = null },
            title = { Text("Edit Week ${slot.week} Matchup", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = replaceHome,
                            onClick = { replaceHome = true },
                            label = { Text("Swap Home Team") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !replaceHome,
                            onClick = { replaceHome = false },
                            label = { Text("Swap Away Team") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "Select opponent to rotate into slot (automatically balances intra-week pairings):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(franchises.filter { it.id != slot.homeTeamId && it.id != slot.awayTeamId }) { candidate ->
                            AssistChip(
                                onClick = {
                                    viewModel.swapUpcomingMatchTeam(slot.id, replaceHome, candidate.id)
                                    editingTeamsSlot = null
                                },
                                label = { Text(candidate.cityName, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("chip_swap_team_${candidate.id}")
                            )
                        }
                    }
                    HorizontalDivider()
                    Text("Or move this matchup to another Week:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((1..plan.regularSeasonWeeks).filter { it != slot.week }) { wk ->
                            FilterChip(
                                selected = targetWeek == wk,
                                onClick = { targetWeek = wk },
                                label = { Text("W$wk") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (targetWeek != slot.week) {
                        viewModel.moveUpcomingMatchToWeek(slot.id, targetWeek)
                        selectedWeek = targetWeek
                    }
                    editingTeamsSlot = null
                }) {
                    Text("Apply Week")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTeamsSlot = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog 2: Holiday Marquee Rivalry Assignment
    editingHolidaySlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { editingHolidaySlot = null },
            title = { Text("Assign Holiday Marquee Rivalry", fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(HolidayTentpole.values()) { tentpole ->
                        val isSelected = slot.holidayTentpole == tentpole
                        Surface(
                            color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setHolidayMarqueeOnUpcomingSlot(slot.id, tentpole)
                                    editingHolidaySlot = null
                                }
                                .testTag("option_holiday_${tentpole.name}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = tentpole.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (tentpole == HolidayTentpole.NONE) {
                                        tentpole.description
                                    } else {
                                        "${tentpole.description} (+\$${tentpole.tvBonusMillions}M TV • ${tentpole.gateMultiplier}x Gate)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingHolidaySlot = null }) { Text("Cancel") }
            }
        )
    }

    // Dialog 3: International Showcase Venue Assignment
    editingIntlSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { editingIntlSlot = null },
            title = { Text("Schedule International Showcase", fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Surface(
                            color = if (!slot.isInternationalGame) CyanAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setInternationalShowcaseOnUpcomingSlot(slot.id, null)
                                    editingIntlSlot = null
                                }
                                .testTag("option_intl_domestic")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Domestic Home Arena (Standard)", fontWeight = FontWeight.Bold)
                                Text("Keep matchup in the home franchise's domestic arena.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    items(InternationalVenueCatalog.allVenues) { venue ->
                        val isSelected = slot.internationalVenueId == venue.id
                        Surface(
                            color = if (isSelected) EmeraldAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setInternationalShowcaseOnUpcomingSlot(slot.id, venue.id)
                                    editingIntlSlot = null
                                }
                                .testTag("option_intl_${venue.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${venue.cityName}, ${venue.country} — ${venue.venueName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "+${venue.fanbaseGainMillions}M Global Reach • +\$${venue.tvBonusMillions}M TV • ${venue.gateMultiplier}x Gate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingIntlSlot = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ScheduleMetricChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
