package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffSeasonSummitScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    // Hardware/system back press returns to main desk
    BackHandler {
        viewModel.closeOffSeasonSummit()
    }

    val session = uiState.activeOffSeasonSession ?: uiState.latestCompletedOffSeason
    if (session == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No off-season session available", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val isReviewMode = uiState.seasonPhase != SeasonPhase.OFF_SEASON || session.isCompleted
    val currentStep = session.currentStep

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("offseason_summit_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ANNUAL OFF-SEASON SUMMIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isReviewMode) CyanAccent.copy(alpha = 0.2f) else EmeraldAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isReviewMode) "REVIEW MODE" else "LIVE SUMMIT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isReviewMode) CyanAccent else EmeraldAccent,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Season ${session.year} ➔ Season ${session.year + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeOffSeasonSummit() },
                        modifier = Modifier.testTag("btn_close_offseason_summit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
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
                .testTag("offseason_summit_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step Navigation Selector
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offseason_step_navigation_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "OFF-SEASON PHASES (INTERACTIVE & REVIEWABLE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("offseason_steps_lazy_row")
                        ) {
                            items(OffSeasonStep.values()) { step ->
                                val isSelected = currentStep == step
                                Surface(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clickable { viewModel.setOffSeasonStep(step) }
                                        .testTag("tab_offseason_step_${step.stepIndex}"),
                                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${step.stepIndex}. ${step.title}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Parity Impact Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_offseason_parity_gauge")
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "COMPETITIVE PARITY EQUILIBRIUM",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${session.initialParityScore}/100",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${session.finalParityScore}/100",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent
                                )
                            }
                        }

                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "+${session.finalParityScore - session.initialParityScore} Parity Compression",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Dynamic Step Content
            when (currentStep) {
                OffSeasonStep.AWARDS_SHOW -> {
                    item {
                        YearlyAwardsShowPanel(
                            session = session,
                            sportType = uiState.sportType,
                            isReviewMode = isReviewMode,
                            onPresentTrophies = { viewModel.presentOffSeasonYearlyAwards() },
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.RETIREMENTS) }
                        )
                    }
                }
                OffSeasonStep.RETIREMENTS -> {
                    item {
                        RetirementsPanel(
                            retirements = session.retirements,
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.DRAFT_LOTTERY) }
                        )
                    }
                }
                OffSeasonStep.DRAFT_LOTTERY -> {
                    item {
                        DraftLotteryPanel(
                            lotteryResults = session.lotteryResults,
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.FREE_AGENCY) }
                        )
                    }
                }
                OffSeasonStep.FREE_AGENCY -> {
                    item {
                        FreeAgencyPanel(
                            signings = session.freeAgencySignings,
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.HALL_OF_FAME) }
                        )
                    }
                }
                OffSeasonStep.HALL_OF_FAME -> {
                    item {
                        HallOfFameInductionPanel(
                            session = session,
                            sportType = uiState.sportType,
                            isReviewMode = isReviewMode,
                            onEnshrineClass = { viewModel.ratifyOffSeasonHallOfFame() },
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.LEAGUE_DRAFT) }
                        )
                    }
                }
                OffSeasonStep.LEAGUE_DRAFT -> {
                    item {
                        LeagueDraftPanel(
                            session = session,
                            franchises = uiState.franchises,
                            sportType = uiState.sportType,
                            isReviewMode = isReviewMode,
                            onSelectProspect = { prospectId -> viewModel.conductNextRookieDraftPick(prospectId) },
                            onAutoPickNext = { viewModel.conductNextRookieDraftPick(null) },
                            onAutoCompleteDraft = { viewModel.autoCompleteRookieDraft() },
                            onRestartDraft = { viewModel.restartRookieDraft() },
                            onProceedToSchedule = { viewModel.setOffSeasonStep(OffSeasonStep.SCHEDULE_CALENDAR) },
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.SUMMARY) }
                        )
                    }
                }
                OffSeasonStep.SUMMARY -> {
                    item {
                        SummaryParityPanel(
                            session = session,
                            upcomingPlan = uiState.seasonScheduleState.upcomingPlan,
                            jobSecurityState = uiState.jobSecurityLegacyState,
                            isReviewMode = isReviewMode,
                            onOpenSchedule = { viewModel.setOffSeasonStep(OffSeasonStep.SCHEDULE_CALENDAR) },
                            onOpenAnnualReview = { viewModel.setOffSeasonStep(OffSeasonStep.ANNUAL_REVIEW) },
                            onFinalize = { viewModel.finalizeAndInaugurateNewSeason() },
                            onClose = { viewModel.closeOffSeasonSummit() }
                        )
                    }
                }
                OffSeasonStep.SCHEDULE_CALENDAR -> {
                    item {
                        ScheduleCalendarEditorContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            isEmbeddedInOffSeason = true,
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.ANNUAL_REVIEW) }
                        )
                    }
                }
                OffSeasonStep.ANNUAL_REVIEW -> {
                    item {
                        var offSeasonPledge by remember(uiState.jobSecurityLegacyState.pendingAnnualReview?.id) {
                            mutableStateOf(ReviewResponseChoice.OWNER_GROWTH_CHARTER)
                        }
                        AnnualReviewEmbeddedPanel(
                            uiState = uiState,
                            selectedPledge = offSeasonPledge,
                            onSelectPledge = { offSeasonPledge = it },
                            onConveneNow = { viewModel.conveneAnnualPerformanceReviewNow() },
                            onRatifyReview = { choice -> viewModel.ratifyPendingAnnualReview(choice) },
                            isReviewMode = isReviewMode,
                            onProceedNext = { viewModel.setOffSeasonStep(OffSeasonStep.SUMMARY) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: YEARLY AWARDS SHOW PANEL (START OF OFF-SEASON)
// -------------------------------------------------------------
@Composable
private fun YearlyAwardsShowPanel(
    session: OffSeasonSession,
    sportType: SportType,
    isReviewMode: Boolean,
    onPresentTrophies: () -> Unit,
    onProceedNext: () -> Unit
) {
    val ceremony = session.awardsCeremony ?: RecordBookEngine.computeYearlyAwards(
        year = session.year,
        sportType = sportType,
        franchises = emptyList(),
        seasonStats = emptyMap(),
        weeklyAwards = emptyList(),
        champion = null
    )
    val labels = remember(sportType) { RecordBookEngine.getStatLabels(sportType) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("panel_offseason_awards_show"),
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PHASE 1 • YEARLY AWARDS SHOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = ceremony.ceremonyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = if (ceremony.isPresented) EmeraldAccent.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (ceremony.isPresented) "PRESENTED" else "HOST: ${ceremony.hostCity.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ceremony.isPresented) EmeraldAccent else GoldPrimary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kicking off the off-season live from ${ceremony.hostCity}, annual trophies are awarded directly from regular season and playoff player statistics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (ceremony.mvpFinalists.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "MVP BALLOT FINALISTS & VOTE SHARE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ceremony.mvpFinalists.forEachIndexed { idx, finalist ->
                            Column(modifier = Modifier.testTag("mvp_finalist_${idx + 1}")) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${idx + 1} ${finalist.playerName} (${finalist.position}) • ${finalist.teamName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (idx == 0) FontWeight.Black else FontWeight.Bold,
                                        color = if (idx == 0) GoldPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${finalist.voteSharePct}% Vote",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldAccent
                                    )
                                }
                                Text(
                                    text = finalist.statLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { (finalist.voteSharePct / 100f).coerceIn(0.05f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (idx == 0) GoldPrimary else RoyalBlueAccent,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "SEASON ${ceremony.year} TROPHY RECIPIENTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = GoldPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ceremony.awards.forEach { winner ->
                    val accent = Color(winner.awardType.accentColorHex)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("award_winner_${winner.awardType.name.lowercase()}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = accent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = winner.awardType.displayTitle.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = accent,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = winner.position,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${winner.winnerName} • ${winner.franchiseName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = winner.primaryStatLine,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = winner.supportingNarrative,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (ceremony.allLeagueFirstTeam.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "ALL-LEAGUE FIRST TEAM SELECTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ceremony.allLeagueFirstTeam.forEach { star ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${star.position} • ${star.playerName} (${star.franchiseName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${star.ppg} ${labels.primaryShort} / ${star.rpg} ${labels.secondaryShort} / ${star.apg} ${labels.tertiaryShort}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isReviewMode && !ceremony.isPresented) {
                Button(
                    onClick = onPresentTrophies,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_present_yearly_awards")
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Present Season ${ceremony.year} Annual Trophies", color = Color.Black, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onProceedNext,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_next_retirements")
            ) {
                Text("Proceed to Player Retirements ➔", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: RETIREMENTS PANEL
// -------------------------------------------------------------
@Composable
private fun RetirementsPanel(
    retirements: List<RetiredPlayer>,
    onProceedNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("panel_offseason_retirements"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PHASE 2 • PLAYER RETIREMENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberAccent,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Veterans Exit & Salary Cap Relief",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = AmberAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${retirements.size} Legends Stepping Down",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Veteran stars conclude their careers. Their departures free up substantial payroll cap room while naturally equalizing dynasty roster ratings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                retirements.forEach { player ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_retired_player_${player.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = AmberAccent.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = player.position,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberAccent,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = player.legacyTier,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${player.teamName} • Age ${player.age} (${player.yearsActive} Seasons)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Peak: ${player.peakRating} OVR",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Team Impact: -${player.teamRatingPenalty} OVR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonAlert,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cap Space Opened: +$${player.capFreedMillions}M",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onProceedNext,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_next_draft_lottery")
            ) {
                Text("Proceed to Draft Lottery Draw ➔", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: DRAFT LOTTERY PANEL
// -------------------------------------------------------------
@Composable
private fun DraftLotteryPanel(
    lotteryResults: List<DraftLotteryResult>,
    onProceedNext: () -> Unit
) {
    val winner = lotteryResults.firstOrNull { it.finalDraftPick == 1 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("panel_offseason_lottery"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PHASE 3 • DRAFT LOTTERY DRAW",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Weighted Odds & Order Allocation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = CyanAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Weighted Ping-Pong Draw",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Rebuilding franchises with lower regular season records are awarded higher lottery odds to secure top draft picks and restore competitive balance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Highlight #1 Pick Winner
            if (winner != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GoldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = GoldPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("#1", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LOTTERY WINNER • #1 OVERALL PICK",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (winner.isTradedPick) "${winner.pickOwnerFranchiseName} (via ${winner.franchiseName})" else winner.franchiseName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${winner.lotteryOddsPct}% Lottery Odds • ${if (winner.spotMovement > 0) "Jumped +${winner.spotMovement} spots!" else "Secured top pick"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (winner.protectionOutcomeNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = winner.protectionOutcomeNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Results List
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lotteryResults.forEach { result ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_lottery_result_${result.finalDraftPick}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = when (result.finalDraftPick) {
                                        1 -> GoldPrimary
                                        2 -> CyanAccent
                                        3 -> EmeraldAccent
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "#${result.finalDraftPick}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (result.finalDraftPick <= 3) Color.Black else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (result.isTradedPick) "${result.pickOwnerFranchiseName} (via ${result.franchiseName})" else result.franchiseName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Odds: ${result.lotteryOddsPct}% • ${String.format("%.3f", result.regularSeasonWinPct)} Win %",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                    if (result.protectionOutcomeNote.isNotBlank()) {
                                        Text(
                                            text = result.protectionOutcomeNote,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (result.isTradedPick) GoldPrimary else CyanAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = when {
                                    result.spotMovement > 0 -> EmeraldAccent.copy(alpha = 0.15f)
                                    result.spotMovement < 0 -> AmberAccent.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = when {
                                        result.spotMovement > 0 -> "+${result.spotMovement} Spots"
                                        result.spotMovement < 0 -> "${result.spotMovement} Spots"
                                        else -> "No Change"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        result.spotMovement > 0 -> EmeraldAccent
                                        result.spotMovement < 0 -> AmberAccent
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onProceedNext,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_next_free_agency")
            ) {
                Text("Proceed to Free Agency Frenzy ➔", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 6: ROOKIE DRAFT WAR ROOM (POST-FA, PRE-PRESEASON)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeagueDraftPanel(
    session: OffSeasonSession,
    franchises: List<Franchise>,
    sportType: SportType,
    isReviewMode: Boolean,
    onSelectProspect: (String) -> Unit,
    onAutoPickNext: () -> Unit,
    onAutoCompleteDraft: () -> Unit,
    onRestartDraft: () -> Unit,
    onProceedToSchedule: () -> Unit,
    onProceedNext: () -> Unit
) {
    var boardTab by remember { mutableIntStateOf(if (session.isDraftCompleted || isReviewMode) 2 else 0) }
    var expandedProspectId by remember { mutableStateOf<String?>(null) }

    val availableProspects = remember(session.draftProspectPool) {
        session.draftProspectPool.filter { !it.isDrafted }
    }
    val onTheClockSlot = remember(session.currentDraftPickIndex, session.draftOrderSlots) {
        session.draftOrderSlots.getOrNull(session.currentDraftPickIndex)
    }
    val onTheClockTeam = remember(session.currentDraftPickIndex, session.draftOrderFranchiseIds, franchises) {
        val teamId = session.draftOrderFranchiseIds.getOrNull(session.currentDraftPickIndex)
        franchises.find { it.id == teamId }
    }
    val onTheClockNeeds = remember(onTheClockTeam, session.retirements, session.freeAgencySignings, sportType) {
        if (onTheClockTeam != null) {
            OffSeasonEngine.getTeamDraftNeeds(onTheClockTeam, session.retirements, session.freeAgencySignings, sportType)
        } else emptyList()
    }
    val recommendedPair = remember(onTheClockTeam, availableProspects, session.currentDraftPickIndex) {
        if (onTheClockTeam != null && !session.isDraftCompleted && availableProspects.isNotEmpty()) {
            OffSeasonEngine.evaluateBestProspectForTeam(
                team = onTheClockTeam,
                availableProspects = availableProspects,
                retirements = session.retirements,
                freeAgencySignings = session.freeAgencySignings,
                sportType = sportType,
                draftSlot = session.currentDraftPickIndex + 1
            )
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("panel_offseason_draft"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PHASE 6 • ROOKIE DRAFT WAR ROOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Scouting & Collegiate Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = if (session.isDraftCompleted) EmeraldAccent.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (session.isDraftCompleted) "${session.draftPicks.size} Picks Complete"
                        else "Pick ${session.currentDraftPickIndex + 1}/${session.draftOrderFranchiseIds.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (session.isDraftCompleted) EmeraldAccent else GoldPrimary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Held after Free Agency and before Pre-Season camp. Teams evaluate remaining roster gaps alongside verified scouting grades and collegiate performance metrics before locking rookies in for Pre-Season.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (session.tradedPickAdjustments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_draft_traded_picks_banner"),
                    color = GoldPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "TRADED PICKS & PROTECTIONS APPLIED TO THIS DRAFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        session.tradedPickAdjustments.forEach { adj ->
                            Text(
                                text = "• $adj",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ON THE CLOCK CARD
            if (!isReviewMode && !session.isDraftCompleted && onTheClockTeam != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_draft_on_the_clock"),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(color = GoldPrimary, shape = RoundedCornerShape(4.dp)) {
                                    val roundPrefix = onTheClockSlot?.let { "R${it.round} " } ?: ""
                                    Text(
                                        text = "ON THE CLOCK • $roundPrefix#${session.currentDraftPickIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = onTheClockTeam.teamFullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "${onTheClockTeam.rosterRating} OVR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }

                        if (onTheClockSlot != null && onTheClockSlot.isTraded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = CyanAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("badge_on_clock_traded_pick")
                            ) {
                                Text(
                                    text = "PICK ACQUIRED VIA TRADE • ORIG: ${onTheClockSlot.originalFranchiseName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Post-FA Needs: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            onTheClockNeeds.forEach { needPos ->
                                Surface(
                                    color = CrimsonAlert.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = needPos,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonAlert,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (recommendedPair != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Top Front-Office Target: ${recommendedPair.first.name} (${recommendedPair.first.position}, Scout ${recommendedPair.first.scoutingReport.scoutGrade} • College ${recommendedPair.first.collegiateMetrics.collegiateMetricScore})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAutoPickNext,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_draft_autopick_next")
                            ) {
                                Text("Auto-Pick Next", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onAutoCompleteDraft,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_draft_sim_all")
                            ) {
                                Text("Sim Entire Draft", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filter Sub-tabs: Available Prospects / Need Matches / Drafted Ledger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    0 to "Big Board (${availableProspects.size})",
                    1 to "Need Fit",
                    2 to "Draft Ledger (${session.draftPicks.size})"
                )
                tabs.forEach { (idx, label) ->
                    val selected = boardTab == idx
                    FilterChip(
                        selected = selected,
                        onClick = { boardTab = idx },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_draft_tab_$idx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (boardTab == 0 || boardTab == 1) {
                val displayedProspects = remember(availableProspects, boardTab, onTheClockNeeds, session.currentDraftPickIndex) {
                    val base = if (boardTab == 1 && onTheClockNeeds.isNotEmpty()) {
                        availableProspects.filter { it.position in onTheClockNeeds }
                            .ifEmpty { availableProspects }
                    } else availableProspects
                    base.sortedByDescending { it.evaluateTeamFitScore(onTheClockNeeds, session.currentDraftPickIndex + 1) }
                }

                if (displayedProspects.isEmpty()) {
                    Text(
                        text = "All scheduled Round 1 rookie selections are complete. Switch to Draft Ledger to audit all team picks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayedProspects.forEach { prospect ->
                            val isNeedMatch = prospect.position in onTheClockNeeds
                            val isExpanded = expandedProspectId == prospect.id
                            val fitScore = prospect.evaluateTeamFitScore(onTheClockNeeds, session.currentDraftPickIndex + 1)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedProspectId = if (isExpanded) null else prospect.id
                                    }
                                    .testTag("card_prospect_${prospect.id}"),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isNeedMatch) 1.5.dp else 1.dp,
                                    color = if (isNeedMatch) EmeraldAccent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(
                                                color = if (prospect.consensusRank <= 3) GoldPrimary else RoyalBlueAccent.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "#${prospect.consensusRank} ${prospect.position}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (prospect.consensusRank <= 3) Color.Black else RoyalBlueAccent,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = prospect.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "${prospect.collegiateMetrics.classYear} • ${prospect.collegiateMetrics.schoolName} (${prospect.collegiateMetrics.conferenceName})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isNeedMatch) {
                                            Surface(
                                                color = EmeraldAccent.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "NEED FIT",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldAccent,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Collegiate Metrics & Scouting Grades Summary Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = CyanAccent.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                                Text(
                                                    text = "SCOUT GRADE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = CyanAccent,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "${prospect.scoutingReport.scoutGrade}/99 (${prospect.scoutingReport.potentialGrade})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        Surface(
                                            color = GoldPrimary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                                Text(
                                                    text = "COLLEGE SCORE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = GoldPrimary,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "${prospect.collegiateMetrics.collegiateMetricScore}/100 (${prospect.collegiateMetrics.gamesPlayed}G)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        Surface(
                                            color = RoyalBlueAccent.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(0.85f)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                                Text(
                                                    text = "PROJ OVR",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = RoyalBlueAccent,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "${prospect.scoutingReport.projectedOverall} (Fit $fitScore)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "College Metrics: ${prospect.collegiateMetrics.formattedStatLine}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = prospect.collegiateMetrics.accolades,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = GoldPrimary
                                    )

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                                            Text(
                                                text = "EXECUTIVE SCOUTING REPORT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyanAccent,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = prospect.scoutingReport.executiveSummary,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Athletic Combine: ${prospect.scoutingReport.combineAthleticScore} • Defense: ${prospect.scoutingReport.defensiveGrade} • IQ: ${prospect.scoutingReport.iqAndIntangibles}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Strengths: ${prospect.scoutingReport.strengths.joinToString(" • ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = EmeraldAccent
                                            )
                                            Text(
                                                text = "Development Note: ${prospect.scoutingReport.weaknessNote}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = AmberWarning
                                            )
                                            Text(
                                                text = "Pro Comp: ${prospect.scoutingReport.proComparison} (Age ${prospect.age})",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            onClick = { expandedProspectId = if (isExpanded) null else prospect.id },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Text(
                                                text = if (isExpanded) "Hide Scouting File" else "Full Scouting Report",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (!isReviewMode && !session.isDraftCompleted && onTheClockTeam != null) {
                                            Button(
                                                onClick = { onSelectProspect(prospect.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("btn_draft_prospect_${prospect.id}")
                                            ) {
                                                Text(
                                                    text = "Draft to ${onTheClockTeam.nickname}",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // DRAFT LEDGER TAB
                if (session.draftPicks.isEmpty()) {
                    Text(
                        text = "No picks announced yet. Use On The Clock controls above or select a prospect on the Big Board.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        session.draftPicks.forEach { pick ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("item_draft_pick_${pick.pickNumber}"),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(
                                                color = if (pick.pickNumber == 1) GoldPrimary else EmeraldAccent.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "R${pick.round} • #${pick.pickNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (pick.pickNumber == 1) Color.Black else EmeraldAccent,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = pick.franchiseName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (pick.acquiredViaTrade) {
                                                    Text(
                                                        text = "VIA TRADE (${pick.originalFranchiseName})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 9.sp,
                                                        color = GoldPrimary
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            color = EmeraldAccent.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "+${pick.teamBoost} OVR Boost",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldAccent,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${pick.playerName} (${pick.position})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = "${pick.archetype} • ${pick.origin}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = RoyalBlueAccent.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${pick.overallRating} OVR",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = RoyalBlueAccent,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = GoldPrimary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Grade: ${pick.potentialGrade}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = GoldPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (pick.selectionRationale.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = pick.selectionRationale,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = CyanAccent
                                        )
                                    }
                                }
                            }
                        }

                        if (!isReviewMode && session.isDraftCompleted) {
                            OutlinedButton(
                                onClick = onRestartDraft,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_restart_rookie_draft")
                            ) {
                                Text("Re-Open & Reset Draft War Room", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onProceedToSchedule,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_draft_to_schedule")
            ) {
                Text("Next: Edit & Approve Season Schedule ➔", color = Color.Black, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onProceedNext,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_next_parity_summary")
            ) {
                Text("Jump to Parity & Pre-Season Launch ➔", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: FREE AGENCY PANEL
// -------------------------------------------------------------
@Composable
private fun FreeAgencyPanel(
    signings: List<FreeAgencySigning>,
    onProceedNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("panel_offseason_free_agency"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RoyalBlueAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = RoyalBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PHASE 4 • FREE AGENCY FRENZY",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlueAccent,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Star Contract Acquisitions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = RoyalBlueAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Salary Cap Regulated",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalBlueAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Marquee unrestricted stars sign new contracts under league salary cap strictness. Rebuilding clubs invest cap liquidity to reinforce frontline rosters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                signings.forEach { fa ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_free_agent_${fa.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = RoyalBlueAccent.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = fa.position,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = RoyalBlueAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = fa.playerName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "+${fa.teamBoost} OVR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${fa.formerTeamName} ➔ ${fa.newTeamName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$${fa.salaryMillions}M/yr • ${fa.termYears} Yrs",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = fa.signingType,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onProceedNext,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_next_hall_of_fame")
            ) {
                Text("Proceed to Hall of Fame Induction ➔", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 5: HALL OF FAME INDUCTION CEREMONY (AFTER FREE AGENCY)
// -------------------------------------------------------------
@Composable
private fun HallOfFameInductionPanel(
    session: OffSeasonSession,
    sportType: SportType,
    isReviewMode: Boolean,
    onEnshrineClass: () -> Unit,
    onProceedNext: () -> Unit
) {
    val labels = remember(sportType) { RecordBookEngine.getStatLabels(sportType) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("panel_offseason_hall_of_fame"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PHASE 5 • HALL OF FAME CEREMONY",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Class of ${session.year} Enshrinement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = if (session.hofClassRatified) EmeraldAccent.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (session.hofClassRatified) "ENSHRINED" else "POST-FA CEREMONY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (session.hofClassRatified) EmeraldAccent else GoldPrimary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Following Free Agency, the Commissioner and Hall of Fame Committee formally enshrine eligible legends into permanent league immortality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                session.hallOfFameInductees.forEach { inductee ->
                    HallOfFameInducteeCard(inductee = inductee, labels = labels)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isReviewMode && !session.hofClassRatified && session.hallOfFameInductees.isNotEmpty()) {
                Button(
                    onClick = onEnshrineClass,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_ratify_hof_class")
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enshrine Class of ${session.year} into Hall of Fame", color = Color.Black, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onProceedNext,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_next_rookie_draft")
            ) {
                Text("Proceed to Rookie Draft ➔", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 6: PARITY AUDIT & REVIEW PANEL
// -------------------------------------------------------------
@Composable
private fun SummaryParityPanel(
    session: OffSeasonSession,
    upcomingPlan: SeasonSchedulePlan?,
    jobSecurityState: JobSecurityLegacyState,
    isReviewMode: Boolean,
    onOpenSchedule: () -> Unit,
    onOpenAnnualReview: () -> Unit,
    onFinalize: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("panel_offseason_summary"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PHASE 7 • PARITY & POWER RANKINGS",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Off-Season Reconstruction Audit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = EmeraldAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Parity Compressed",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Comprehensive audit of franchise power ratings following retirements, draft lottery, rookie drafting, and free agency. Parity equilibrium elevated across the league.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Parity Progress
            ProgressBarWithLabel(
                label = "League Parity Equilibrium Index",
                progress = (session.finalParityScore / 100f).coerceIn(0f, 1f),
                displayValue = "${session.finalParityScore}/100 (+${session.finalParityScore - session.initialParityScore} pts)",
                color = EmeraldAccent
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Team-by-team Movement Table
            Text(
                text = "FRANCHISE OFF-SEASON MOVEMENT & GRADES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                session.teamMovements.forEach { movement ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_team_movement_${movement.franchiseId}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = movement.franchiseName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${movement.startRating} OVR ➔ ${movement.endRating} OVR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = when {
                                            movement.netChange > 0 -> EmeraldAccent.copy(alpha = 0.18f)
                                            movement.netChange < 0 -> CrimsonAlert.copy(alpha = 0.18f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (movement.netChange >= 0) "+${movement.netChange}" else "${movement.netChange}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (movement.netChange >= 0) EmeraldAccent else CrimsonAlert,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = GoldPrimary.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = movement.grade,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = GoldPrimary,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Acquisition: ${movement.primaryAcquisition}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "Departure: ${movement.primaryLoss}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (upcomingPlan != null) {
                val schedColor = if (upcomingPlan.isApproved) EmeraldAccent else GoldPrimary
                Surface(
                    color = schedColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, schedColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_summary_schedule_status")
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
                                text = "SEASON ${upcomingPlan.targetYear} SCHEDULE (${upcomingPlan.regularSeasonWeeks}W)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = schedColor
                            )
                            Text(
                                text = "${upcomingPlan.approvalStatus.label} • ${upcomingPlan.holidayShowcaseCount} Holidays • ${upcomingPlan.internationalGameCount} Global",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onOpenSchedule,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_summary_open_schedule")
                        ) {
                            Text("Edit Schedule", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            run {
                val pendingRev = jobSecurityState.pendingAnnualReview
                val revColor = if (pendingRev != null) Color(pendingRev.overallGrade.colorHex) else Color(jobSecurityState.securityTier.colorHex)
                Surface(
                    color = revColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, revColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_summary_annual_review_status")
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
                                text = if (pendingRev != null) "ANNUAL REVIEW [${pendingRev.overallGrade.letter}]" else "JOB SECURITY & LEGACY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = revColor
                            )
                            Text(
                                text = pendingRev?.let { "${it.contractAction.label} • +${it.legacyPointsAwarded} Legacy Pts" }
                                    ?: "Security ${jobSecurityState.jobSecurityScore}% • ${jobSecurityState.legacyPoints} Legacy Pts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onOpenAnnualReview,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_summary_open_annual_review")
                        ) {
                            Text(if (pendingRev != null) "Ratify" else "Review", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!isReviewMode) {
                Button(
                    onClick = onFinalize,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_finalize_offseason")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ratify Off-Season & Launch ${session.year + 1} Pre-Season",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_dismiss_review_mode")
                ) {
                    Text("Return to Commissioner Desk", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
