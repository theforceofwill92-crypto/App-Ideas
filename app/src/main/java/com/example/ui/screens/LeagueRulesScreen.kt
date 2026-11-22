package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.window.Dialog
import com.example.model.Conference
import com.example.model.Division
import com.example.model.Franchise
import com.example.model.GameLengthRule
import com.example.model.InjuryFrequencyRule
import com.example.model.LeagueRule
import com.example.model.OvertimeFormat
import com.example.model.PlayoffQualificationMode
import com.example.model.PlayoffRules
import com.example.model.PlayoffSeedInfo
import com.example.model.PlayoffSeedingMethod
import com.example.model.PlayoffStructureEngine
import com.example.model.RealignmentPreset
import com.example.model.RosterSizeRule
import com.example.model.RuleCategory
import com.example.model.SalaryCapStrictness
import com.example.model.calculatePlayoffSeeds
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueRulesScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val ruleBook = uiState.ruleBook
    val rules = ruleBook.rules.ifEmpty { com.example.data.InitialLeagueData.getDefaultLeagueRules() }
    val franchises = uiState.franchises
    val playoffRules = ruleBook.playoffRules

    var selectedCategoryFilter by remember { mutableStateOf<RuleCategory?>(null) }
    var expandedRuleId by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showRealignmentWorkbenchDialog by remember { mutableStateOf(false) }
    var showContinentalBuilderDialog by remember { mutableStateOf(false) }
    var showPlayoffRulesWorkbenchDialog by remember { mutableStateOf(false) }
    var showTradeRulesWorkbenchDialog by remember { mutableStateOf(false) }
    var selectedTeamForMove by remember { mutableStateOf<Franchise?>(null) }
    var inspectedBoardVote by remember { mutableStateOf<com.example.model.BoardVoteResult?>(null) }

    val activeConferences = remember(franchises, ruleBook.enabledConferences) {
        ruleBook.effectiveConferences(franchises)
    }
    val activeDivisions = remember(franchises, ruleBook.enabledDivisions, activeConferences) {
        Division.values().filter { div ->
            div.conference in activeConferences && (div in ruleBook.enabledDivisions || franchises.any { it.division == div })
        }
    }
    val possiblePlayoffStructures = remember(franchises, ruleBook.enabledConferences, ruleBook.enabledDivisions) {
        PlayoffStructureEngine.generatePossibleStructures(franchises, ruleBook.enabledConferences, ruleBook.enabledDivisions)
    }

    val currentProjectedSeeds = remember(franchises, playoffRules) {
        calculatePlayoffSeeds(franchises, playoffRules)
    }

    val filteredRules = remember(rules, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) rules else rules.filter { it.category == selectedCategoryFilter }
    }

    val enactedCount = remember(rules) { rules.count { it.isEnacted } }
    val repealedCount = remember(rules) { rules.count { !it.isEnacted } }

    val isSalaryCapEnacted = remember(rules) { rules.find { it.id == "rule_salary_cap" }?.isEnacted ?: true }
    val isDraftLotteryEnacted = remember(rules) { rules.find { it.id == "rule_draft_lottery" }?.isEnacted ?: true }
    val isShotClockEnacted = remember(rules) { rules.find { it.id == "rule_shot_clock" }?.isEnacted ?: true }
    val isRevenueSharingEnacted = remember(rules) { rules.find { it.id == "rule_revenue_sharing" }?.isEnacted ?: true }
    val isGeoRealignmentEnacted = remember(rules) { rules.find { it.id == "rule_geographic_realignment" }?.isEnacted ?: true }
    val isDivisionAutoPlayoffEnacted = remember(rules) { rules.find { it.id == "rule_division_auto_playoffs" }?.isEnacted ?: true }

    val confAverages = remember(franchises, activeConferences) {
        activeConferences.map { conf ->
            val confTeams = franchises.filter { it.conference == conf }
            val avg = if (confTeams.isNotEmpty()) confTeams.map { it.rosterRating }.average() else 0.0
            conf to avg
        }
    }
    val parityDelta = remember(confAverages) {
        val populatedAvgs = confAverages.filter { pair -> franchises.any { it.conference == pair.first } }.map { it.second }
        if (populatedAvgs.size >= 2) (populatedAvgs.max() - populatedAvgs.min()) else 0.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("league_rules_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RULES COMMISSIONER",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Game-Wide Rules & Governance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.openLeagueSettings() },
                        modifier = Modifier.testTag("btn_rules_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Commissioner Settings",
                            tint = CyanAccent
                        )
                    }
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("btn_reset_default_rules")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Charter",
                            tint = GoldPrimary
                        )
                    }
                }
            }

            Text(
                text = "Toggle game-wide competition rules, overtime formats, team roster sizes, injury frequency, game length, and salary cap enforcement strictness across the league.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Sport-Specific Competition & Rule Committee Hub Card
        item {
            val committee = uiState.ruleCommitteeState
            val modifiers = remember(committee, uiState.sportType) {
                com.example.model.RuleCommitteeEngine.computeSimulationModifiers(committee, uiState.sportType)
            }
            val scoreDeltaText = if (uiState.sportType == com.example.model.SportType.SOCCER || uiState.sportType == com.example.model.SportType.HOCKEY) {
                val sign = if (modifiers.scoringDeltaPerTeam >= 0) "+" else ""
                "$sign${String.format(java.util.Locale.US, "%.2f", modifiers.scoringDeltaPerTeam)} G/Gm"
            } else {
                val sign = if (modifiers.scoringDeltaPerTeam >= 0) "+" else ""
                "$sign${String.format(java.util.Locale.US, "%.1f", modifiers.scoringDeltaPerTeam)} PPG"
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.4.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.openRuleCommitteeScreen(true) }
                    .testTag("card_rule_committee_entry"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${uiState.sportType.displayName.uppercase(java.util.Locale.US)} RULE COMMITTEE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Propose & Enact Simulation Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${modifiers.activeRuleCount} Active Rules • Score $scoreDeltaText • Injury ${String.format(java.util.Locale.US, "%.2f", modifiers.injuryRiskMultiplier)}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.openRuleCommitteeScreen(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_open_rule_committee")
                        ) {
                            Text(
                                text = "Committee",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            val discState = uiState.disciplinaryState
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.openDisciplinaryDesk(true) }
                    .testTag("card_disciplinary_rules_entry"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DISCIPLINARY DESK & PRECEDENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Suspensions, Fines & Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${discState.pendingCases.size} Pending • ${discState.appealedCases.size} Appeals • ${discState.precedentConsistencyScore}% Consistency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.openDisciplinaryDesk(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_open_disciplinary_from_rules")
                    ) {
                        Text(
                            text = "Hearings",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            var selectedLengthCandidate by remember(ruleBook.regularSeasonWeeks) {
                mutableIntStateOf(ruleBook.regularSeasonWeeks)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, RoyalBlueAccent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .testTag("card_rules_season_length"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REGULAR SEASON CALENDAR LENGTH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = RoyalBlueAccent,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${ruleBook.regularSeasonWeeks} Weeks • All-Star Week ${com.example.model.ScheduleCalendarEngine.allStarWeekFor(ruleBook.regularSeasonWeeks)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.openScheduleCalendarScreen(true) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_rules_open_schedule")
                        ) {
                            Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(com.example.model.ScheduleCalendarEngine.ALLOWED_SEASON_LENGTHS) { wkOpt ->
                            FilterChip(
                                selected = selectedLengthCandidate == wkOpt,
                                onClick = { selectedLengthCandidate = wkOpt },
                                label = { Text("${wkOpt}W", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("chip_rules_season_weeks_$wkOpt")
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.proposeRegularSeasonLengthVote(selectedLengthCandidate) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("btn_rules_vote_season_length")
                    ) {
                        Text(
                            text = "Vote to Set Regular Season to ${selectedLengthCandidate} Weeks",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Rules Commissioner Game-Wide Controls Card (Board of Governors Vote-Gated)
        item {
            RulesCommissionerControlCard(
                ruleBook = ruleBook,
                onUpdateOvertime = { fmt ->
                    viewModel.submitSettingRuleVote(
                        proposalTitle = "Overtime Format: ${fmt.displayName}",
                        proposalSummary = fmt.description,
                        overtimeFormat = fmt
                    )
                },
                onUpdateRoster = { rosterRule ->
                    viewModel.submitSettingRuleVote(
                        proposalTitle = "Roster Limit: ${rosterRule.displayName}",
                        proposalSummary = rosterRule.description,
                        rosterSizeRule = rosterRule
                    )
                },
                onUpdateCapStrictness = { strictness ->
                    viewModel.submitSettingRuleVote(
                        proposalTitle = "Salary Cap Policy: ${strictness.displayName}",
                        proposalSummary = strictness.description,
                        category = com.example.model.GovernanceProposalCategory.SALARY_CAP_POLICY,
                        capStrictness = strictness
                    )
                },
                onUpdateInjuryFrequency = { freq ->
                    viewModel.submitSettingRuleVote(
                        proposalTitle = "Injury Frequency: ${freq.displayName}",
                        proposalSummary = freq.description,
                        injuryFrequency = freq
                    )
                },
                onUpdateGameLength = { len ->
                    viewModel.submitSettingRuleVote(
                        proposalTitle = "Game Length: ${len.displayName}",
                        proposalSummary = len.description,
                        gameLength = len
                    )
                },
                onOpenSettings = { viewModel.openLeagueSettings() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            com.example.ui.components.BoardOfGovernorsLedgerCard(
                votes = uiState.boardVoteHistory,
                onSelectVote = { inspectedBoardVote = it }
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Balance & Performance Gauges Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("league_balance_gauges_card")
                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "COMPETITIVE PARITY & BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            color = if (uiState.competitiveParityScore >= 75) EmeraldAccent.copy(alpha = 0.18f) else CrimsonAlert.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (uiState.competitiveParityScore >= 80) "BALANCED COMPETITION"
                                else if (uiState.competitiveParityScore >= 60) "MODERATE DISPARITY"
                                else "DYNASTY MONOPOLY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (uiState.competitiveParityScore >= 75) EmeraldAccent else CrimsonAlert,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ProgressBarWithLabel(
                        label = "Parity Equilibrium Index",
                        progress = (uiState.competitiveParityScore / 100f).coerceIn(0f, 1f),
                        displayValue = "${uiState.competitiveParityScore}/100",
                        color = if (uiState.competitiveParityScore >= 75) EmeraldAccent else GoldPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Scoring Pace Metric
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = if (isShotClockEnacted) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Scoring Pace",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isShotClockEnacted) "+15% High Tempo" else "Defensive Grind",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isShotClockEnacted) AmberAccent else TextSecondary
                                )
                            }
                        }

                        // Integrity Index
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Integrity",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.leagueIntegrityScore}/100 Clean",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }

                        // Small Market Protection
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationCity,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Small Markets",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isRevenueSharingEnacted && isSalaryCapEnacted) "Shielded (5★)" else if (isSalaryCapEnacted) "Moderate (3★)" else "Vulnerable (1★)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRevenueSharingEnacted && isSalaryCapEnacted) EmeraldAccent else CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Labor Union CBA Relations Card (Direct Shortcut)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openLaborUnionScreen(true) }
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("banner_union_cba_rules_screen"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(RoyalBlueAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = "Union",
                            tint = RoyalBlueAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PLAYERS UNION ACCORD",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlueAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Collective Bargaining Agreement (CBA)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Strike Risk: ${uiState.unionAgreement.strikeRisk}% • ${uiState.unionAgreement.playerRevenueSharePct}% Revenue Split",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.openLaborUnionScreen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_cba_summit_shortcut")
                    ) {
                        Text("Bargain", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Conference & Division Realignment Codex Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_conference_realignment_bylaw")
                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Realignment",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CONFERENCE & DIVISION REALIGNMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "League Alignment Governance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = if (isGeoRealignmentEnacted) EmeraldAccent.copy(alpha = 0.15f) else CrimsonAlert.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isGeoRealignmentEnacted) "Travel Bylaw: Enacted" else "Open Circuit Alignment",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGeoRealignmentEnacted) EmeraldAccent else CrimsonAlert,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Manage competitive balance across ${activeConferences.size} conferences and ${activeDivisions.size} divisions. Create European, South American & Asia-Oceania conferences for non-North-American franchises or apply alignment presets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Conference Balance Cards (wraps cleanly for 2..5 conferences)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        confAverages.chunked(2).forEach { pairRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pairRow.forEach { (conf, avgOvr) ->
                                    val teamsInConf = franchises.filter { it.conference == conf }
                                    val confColor = Color(conf.badgeColorHex)
                                    val confDivs = ruleBook.effectiveDivisionsFor(conf, franchises)
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, confColor.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = conf.shortCode,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = confColor
                                                )
                                                Text(
                                                    text = "${teamsInConf.size} Teams",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (teamsInConf.isNotEmpty()) "${String.format("%.1f", avgOvr)} OVR" else "Open",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = confDivs.joinToString(" • ") { it.shortCode },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (pairRow.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Parity Delta & Auto playoff note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inter-Conference Parity Delta: ${String.format("%.1f", parityDelta)} OVR (${if (parityDelta <= 2.5) "Optimal Balance" else "Moderate Imbalance"})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (parityDelta <= 2.5) EmeraldAccent else NeonAmber,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showRealignmentWorkbenchDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_open_realignment_workbench"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Realignment",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { showContinentalBuilderDialog = true },
                            modifier = Modifier
                                .weight(1.1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_rules_continental_conferences"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Continental",
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Postseason Playoff Rules Codex Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_playoff_rules_codex")
                    .border(1.2.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Playoffs",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "POSTSEASON TOURNAMENT & PLAYOFF RULES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Playoff Eligibility & Seeding Codex",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${playoffRules.fieldSize} Teams Qualify",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Adaptive bylaws governing postseason field size, multi-conference champion berths, division crowns, wild card spots, and tournament seeding (${possiblePlayoffStructures.size} formats tailored to current alignment).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val populatedConfs = remember(franchises) { franchises.map { it.conference }.distinct() }
                    val populatedDivs = remember(franchises) { franchises.map { it.division }.distinct() }
                    Surface(
                        color = CyanAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Active Topology: ${populatedConfs.size} Conf (${populatedConfs.joinToString(", ") { it.shortCode }}) • ${populatedDivs.size} Populated Divs",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rule Specification Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "BYES & BRACKET",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (playoffRules.firstRoundByesCount > 0) "${playoffRules.firstRoundByesCount} First-Round Byes" else "No Byes (All Play)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (playoffRules.firstRoundByesCount > 0) "Top seeds advance past opening round" else "All qualifiers play opening round",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "ELIGIBILITY / SEEDING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlueAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = playoffRules.qualificationMode.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${playoffRules.seedingMethod.displayName} • ${if (playoffRules.divisionWinnersAutoBerth) "Div Champs Protected" else "Pure Merit"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live projected bracket teaser
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CURRENT PROJECTED PLAYOFF PICTURE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${currentProjectedSeeds.size} of ${playoffRules.fieldSize} Clinched",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            currentProjectedSeeds.take(4).forEach { seedInfo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (seedInfo.hasBye) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "#${seedInfo.seed}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = if (seedInfo.hasBye) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${seedInfo.franchise.teamFullName} (${seedInfo.franchise.conference.shortCode})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = if (seedInfo.hasBye) EmeraldAccent.copy(alpha = 0.15f) else RoyalBlueAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = seedInfo.eligibilityTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (seedInfo.hasBye) EmeraldAccent else RoyalBlueAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showPlayoffRulesWorkbenchDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_configure_playoff_rules"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Configure Playoff Rules & Eligibility",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Trade Regulations & Trade Deadline Governance Card
        item {
            val tradeRules = ruleBook.tradeRules
            val isDeadlineWeek = tradeRules.isTradeDeadlineWeek(uiState.currentWeek, uiState.seasonPhase)
            val isOpen = tradeRules.isTradeWindowOpen(uiState.currentWeek, uiState.seasonPhase)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_trade_regulations_codex")
                    .border(1.2.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AmberWarning.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Trade Regulations",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ROSTER TRANSACTIONS & TRADES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarning,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Trade Deadline & Regulations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = if (isDeadlineWeek) CrimsonAlert.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Deadline: Week ${tradeRules.tradeDeadlineWeek}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDeadlineWeek) CrimsonAlert else AmberWarning,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Commissioner rules determining the in-season trade cutoff week, draft pick trading authorizations, and commissioner approval prerogatives.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "TRADING WINDOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isOpen) "Active (Open)" else "Closed",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isOpen) EmeraldAccent else CrimsonAlert
                                )
                                Text(
                                    text = "Current: Week ${uiState.currentWeek} • Deadline: W${tradeRules.tradeDeadlineWeek}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "DRAFT PICK TRADING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlueAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (tradeRules.allowDraftPickTrading) "Picks Permitted" else "Players Only",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Up to ${tradeRules.maxFuturePickYears} future draft cycles",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onClick = { showTradeRulesWorkbenchDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_configure_trade_rules"),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Configure Trade Deadline",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.openTradeDesk(true) },
                            modifier = Modifier.testTag("btn_rules_open_trade_desk"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trade Desk",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section Title & Category Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(
                    title = "Competition Bylaws Codex",
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$enactedCount Enacted • $repealedCount Repealed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All Rules (${rules.size})") },
                        modifier = Modifier.testTag("filter_all_rules")
                    )
                }
                items(RuleCategory.values()) { category ->
                    val isSel = selectedCategoryFilter == category
                    val count = rules.count { it.category == category }
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCategoryFilter = if (isSel) null else category },
                        label = { Text("${category.title} ($count)") },
                        modifier = Modifier.testTag("filter_${category.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Rules List
        items(filteredRules) { rule ->
            val projectedVote = remember(rule.id, rule.isEnacted, uiState.franchises) {
                viewModel.previewRuleToggleOwnerVotes(rule.id)
            }
            RuleManagementCard(
                rule = rule,
                isExpanded = expandedRuleId == rule.id,
                projectedVote = projectedVote,
                onToggleExpand = {
                    expandedRuleId = if (expandedRuleId == rule.id) null else rule.id
                },
                onToggleEnact = {
                    viewModel.submitRuleToggleVote(rule.id)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (inspectedBoardVote != null) {
        com.example.ui.components.BoardOfGovernorsVoteDialog(
            voteResult = inspectedBoardVote!!,
            onDismiss = { inspectedBoardVote = null }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Restore Charter Bylaws?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will restore all 12 league competition rules (including the Salary Cap, Draft Lottery, Geographic Realignment, and Shot Clock) back to their charter default enacted state.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetRulesToDefault()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Restore Defaults", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Realignment Workbench Dialog
    if (showRealignmentWorkbenchDialog) {
        RealignmentWorkbenchDialog(
            franchises = franchises,
            enabledConferences = ruleBook.enabledConferences,
            enabledDivisions = ruleBook.enabledDivisions,
            isGeoRuleEnacted = isGeoRealignmentEnacted,
            onToggleGeoRule = {
                viewModel.toggleLeagueRule("rule_geographic_realignment")
            },
            onApplyPreset = { preset ->
                viewModel.applyRealignmentPreset(preset)
            },
            onOpenContinentalBuilder = {
                showContinentalBuilderDialog = true
            },
            onSelectTeamForMove = { team ->
                selectedTeamForMove = team
            },
            onDismiss = {
                showRealignmentWorkbenchDialog = false
            }
        )
    }

    // Continental Conferences & Divisions Builder Dialog
    if (showContinentalBuilderDialog) {
        ContinentalConferenceBuilderDialog(
            franchises = franchises,
            enabledConferences = ruleBook.enabledConferences,
            enabledDivisions = ruleBook.enabledDivisions,
            currentPlayoffRules = playoffRules,
            onToggleConference = { conf, enable ->
                viewModel.createOrToggleContinentalConference(conf, enable = enable, autoAlignNonNorthAmerican = true, adaptPlayoffs = true)
            },
            onCreateAllContinental = {
                viewModel.createAllContinentalConferences(autoAlignNonNorthAmerican = true, adaptPlayoffs = true)
            },
            onToggleDivision = { div ->
                viewModel.toggleDivisionEnabled(div)
            },
            onAutoAlignInternational = {
                viewModel.alignInternationalFranchisesToContinentalConferences(adaptPlayoffs = true)
            },
            onDismiss = { showContinentalBuilderDialog = false }
        )
    }

    // Team Move Realignment Dialog
    if (selectedTeamForMove != null) {
        RealignmentTeamMoveDialog(
            franchise = selectedTeamForMove!!,
            enabledConferences = ruleBook.enabledConferences,
            enabledDivisions = ruleBook.enabledDivisions,
            franchises = franchises,
            onConfirm = { conf, div ->
                viewModel.reassignFranchiseConferenceDivision(selectedTeamForMove!!.id, conf, div)
                selectedTeamForMove = null
            },
            onDismiss = {
                selectedTeamForMove = null
            }
        )
    }

    // Playoff Rules Workbench Dialog
    if (showPlayoffRulesWorkbenchDialog) {
        PlayoffRulesWorkbenchDialog(
            playoffRules = playoffRules,
            franchises = franchises,
            enabledConferences = ruleBook.enabledConferences,
            enabledDivisions = ruleBook.enabledDivisions,
            onSaveRules = { updatedRules ->
                viewModel.updatePlayoffRules(
                    fieldSize = updatedRules.fieldSize,
                    qualificationMode = updatedRules.qualificationMode,
                    divisionWinnersAutoBerth = updatedRules.divisionWinnersAutoBerth,
                    seedingMethod = updatedRules.seedingMethod,
                    firstRoundByesCount = updatedRules.firstRoundByesCount,
                    playInTournamentEnabled = updatedRules.playInTournamentEnabled
                )
                showPlayoffRulesWorkbenchDialog = false
            },
            onDismiss = {
                showPlayoffRulesWorkbenchDialog = false
            }
        )
    }

    // Trade Rules & Deadline Workbench Dialog
    if (showTradeRulesWorkbenchDialog) {
        TradeRulesWorkbenchDialog(
            tradeRules = ruleBook.tradeRules,
            currentWeek = uiState.currentWeek,
            onSaveRules = { updatedRules ->
                viewModel.updateTradeRules(updatedRules)
                showTradeRulesWorkbenchDialog = false
            },
            onDismiss = {
                showTradeRulesWorkbenchDialog = false
            }
        )
    }
}

@Composable
fun RealignmentWorkbenchDialog(
    franchises: List<Franchise>,
    enabledConferences: Set<Conference>,
    enabledDivisions: Set<Division>,
    isGeoRuleEnacted: Boolean,
    onToggleGeoRule: () -> Unit,
    onApplyPreset: (RealignmentPreset) -> Unit,
    onOpenContinentalBuilder: () -> Unit,
    onSelectTeamForMove: (Franchise) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedConferenceTab by remember { mutableStateOf<Conference?>(null) }

    val activeConferences = remember(franchises, enabledConferences) {
        Conference.values().filter { conf ->
            conf in enabledConferences || franchises.any { it.conference == conf }
        }
    }
    val continentalCount = activeConferences.count { it.isContinentalInternational }

    val confMetrics = remember(franchises, activeConferences) {
        activeConferences.map { conf ->
            val teams = franchises.filter { it.conference == conf }
            val avg = if (teams.isNotEmpty()) teams.map { it.rosterRating }.average() else 0.0
            Triple(conf, teams.size, avg)
        }
    }
    val parityDelta = remember(confMetrics) {
        val populated = confMetrics.filter { it.second > 0 }.map { it.third }
        if (populated.size >= 2) (populated.max() - populated.min()) else 0.0
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("dialog_realignment_workbench")
                .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMISSIONER REALIGNMENT WORKBENCH",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Conference & Division Governance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_realignment_workbench")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bylaw Rule Control
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isGeoRuleEnacted) EmeraldAccent.copy(alpha = 0.12f) else CrimsonAlert.copy(alpha = 0.12f)
                            ),
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
                                        text = "TRAVEL BYLAW: GEOGRAPHIC REALIGNMENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGeoRuleEnacted) EmeraldAccent else CrimsonAlert
                                    )
                                    Text(
                                        text = if (isGeoRuleEnacted) "Rule Enacted: Regional corridors enforced" else "Rule Repealed: Free circuit movement",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Button(
                                    onClick = onToggleGeoRule,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isGeoRuleEnacted) EmeraldAccent else CrimsonAlert
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_toggle_workbench_geo_rule")
                                ) {
                                    Text(
                                        text = if (isGeoRuleEnacted) "Enacted" else "Repealed",
                                        color = if (isGeoRuleEnacted) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Continental Conferences Launchpad Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenContinentalBuilder() }
                                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .testTag("btn_workbench_open_continental_builder"),
                            colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.10f)),
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
                                        text = "CONTINENTAL CONFERENCES (EUR • SA • APAC)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = CyanAccent
                                    )
                                    Text(
                                        text = "$continentalCount of 3 global conferences active • Tap to create or configure international divisions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(Icons.Default.Public, contentDescription = null, tint = CyanAccent)
                            }
                        }
                    }

                    // Presets Bar
                    item {
                        Column {
                            Text(
                                text = "REALIGNMENT PRESET FORMATS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onApplyPreset(RealignmentPreset.GEOGRAPHIC_OPTIMAL) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_preset_geo_optimal"),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Geo Optimal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onApplyPreset(RealignmentPreset.COASTAL_VS_HEARTLAND) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_preset_coastal"),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Coastal Split", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onApplyPreset(RealignmentPreset.BALANCED_PARITY) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_preset_balanced"),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Parity Split", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Parity Metrics Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ACTIVE CONFERENCES (${activeConferences.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Parity Delta: ${String.format("%.1f", parityDelta)} OVR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (parityDelta <= 2.5) EmeraldAccent else NeonAmber
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(confMetrics) { (conf, count, avgOvr) ->
                                        val confColor = Color(conf.badgeColorHex)
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, confColor.copy(alpha = 0.4f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(conf.shortCode, style = MaterialTheme.typography.labelSmall, color = confColor, fontWeight = FontWeight.Bold)
                                                Text(if (count > 0) "${String.format("%.1f", avgOvr)}" else "-", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                                Text("$count teams", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Filter Tabs
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedConferenceTab == null,
                                    onClick = { selectedConferenceTab = null },
                                    label = { Text("All (${franchises.size})") },
                                    modifier = Modifier.testTag("tab_all_conferences")
                                )
                            }
                            items(activeConferences) { conf ->
                                val count = franchises.count { it.conference == conf }
                                FilterChip(
                                    selected = selectedConferenceTab == conf,
                                    onClick = { selectedConferenceTab = conf },
                                    label = { Text("${conf.shortCode} ($count)") },
                                    modifier = Modifier.testTag("tab_${conf.name.lowercase()}_conf")
                                )
                            }
                        }
                    }

                    // Conference Sections
                    val shownConfs = if (selectedConferenceTab == null) activeConferences else listOf(selectedConferenceTab!!)
                    items(shownConfs) { conf ->
                        val confDivs = Division.values().filter { div ->
                            div.conference == conf && (div in enabledDivisions || franchises.any { it.division == div })
                        }
                        ConferenceAlignmentSection(
                            conference = conf,
                            divisions = confDivs,
                            franchises = franchises.filter { it.conference == conf },
                            onSelectTeamForMove = onSelectTeamForMove
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Ratification Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_ratify_realignment_workbench"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ratify Realignment Bylaws",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ConferenceAlignmentSection(
    conference: Conference,
    divisions: List<Division>,
    franchises: List<Franchise>,
    onSelectTeamForMove: (Franchise) -> Unit
) {
    val confColor = Color(conference.badgeColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, confColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conference.displayName.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = confColor
                )
                Surface(
                    color = confColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${franchises.size} Teams Assigned",
                        style = MaterialTheme.typography.labelSmall,
                        color = confColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            divisions.forEach { div ->
                val divTeams = franchises.filter { it.division == div }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = div.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${divTeams.size} teams",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (divTeams.isEmpty()) {
                            Text(
                                text = "No franchises currently assigned",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            divTeams.forEach { team ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(team.colorHex)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = team.cityName.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = team.teamFullName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${team.rosterRating} OVR • \$${team.annualPayrollMillions}M",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onSelectTeamForMove(team) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("btn_reassign_team_${team.id}")
                                    ) {
                                        Text("Reassign", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldPrimary)
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
fun RealignmentTeamMoveDialog(
    franchise: Franchise,
    enabledConferences: Set<Conference>,
    enabledDivisions: Set<Division>,
    franchises: List<Franchise>,
    onConfirm: (Conference, Division) -> Unit,
    onDismiss: () -> Unit
) {
    val availableConferences = remember(enabledConferences, franchises) {
        Conference.values().filter { it in enabledConferences || franchises.any { f -> f.conference == it } }
    }
    var selectedConference by remember { mutableStateOf(franchise.conference) }
    var selectedDivision by remember {
        val availableDivs = Division.values().filter { it.conference == selectedConference && (it in enabledDivisions || it == franchise.division) }
            .ifEmpty { Division.values().filter { it.conference == selectedConference } }
        mutableStateOf(if (franchise.division in availableDivs) franchise.division else availableDivs.first())
    }

    val (recConf, recDiv) = remember(franchise.cityName, enabledConferences, enabledDivisions) {
        com.example.data.InitialLeagueData.getGeographicAlignment(franchise.cityName, enabledConferences, enabledDivisions)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_realignment_team_move")
                .border(2.dp, GoldPrimary, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "REASSIGN FRANCHISE ALIGNMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = franchise.teamFullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Current: ${franchise.conference.displayName} • ${franchise.division.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Geographic Recommendation Pill
                Surface(
                    color = GoldPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Geographic Recommendation: ${recConf.displayName} • ${recDiv.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step 1: Select Conference
                Text(
                    text = "SELECT CONFERENCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableConferences.chunked(2).forEach { rowConfs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowConfs.forEach { conf ->
                                val isSelected = selectedConference == conf
                                val confColor = Color(conf.badgeColorHex)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedConference = conf
                                            val available = Division.values().filter {
                                                it.conference == conf && (it in enabledDivisions || it == franchise.division)
                                            }.ifEmpty { Division.values().filter { it.conference == conf } }
                                            if (selectedDivision !in available) {
                                                selectedDivision = available.first()
                                            }
                                        }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) confColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .testTag("btn_select_conf_${conf.name.lowercase()}"),
                                    color = if (isSelected) confColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = conf.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) confColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (rowConfs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step 2: Select Division
                Text(
                    text = "SELECT DIVISION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val availableDivisions = Division.values().filter {
                    it.conference == selectedConference && (it in enabledDivisions || it == franchise.division)
                }.ifEmpty { Division.values().filter { it.conference == selectedConference } }
                availableDivisions.forEach { div ->
                    val isDivSelected = selectedDivision == div
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { selectedDivision = div }
                            .border(
                                width = if (isDivSelected) 2.dp else 1.dp,
                                color = if (isDivSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("btn_select_div_${div.name.lowercase()}"),
                        color = if (isDivSelected) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = div.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDivSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = div.geographicRegion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isDivSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selectedConference, selectedDivision) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_confirm_team_reassign"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirm Reassignment", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RuleManagementCard(
    rule: LeagueRule,
    isExpanded: Boolean,
    projectedVote: com.example.model.BoardVoteResult? = null,
    onToggleExpand: () -> Unit,
    onToggleEnact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = if (rule.isEnacted) EmeraldAccent else CrimsonAlert,
        label = "status_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rule_card_${rule.id}")
            .border(
                width = if (rule.isEnacted) 1.2.dp else 1.dp,
                color = if (rule.isEnacted) EmeraldAccent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnacted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category Tag + Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color(rule.category.badgeColorHex).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = rule.category.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(rule.category.badgeColorHex),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (rule.isEnacted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (rule.isEnacted) "ENACTED" else "REPEALED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rule Name
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Summary
            Text(
                text = rule.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Impact Metric Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (rule.parityImpact != 0) {
                    MetricChip(
                        icon = Icons.Default.Balance,
                        label = "${if (rule.parityImpact > 0) "+" else ""}${rule.parityImpact} Parity",
                        tint = if (rule.parityImpact > 0) EmeraldAccent else AmberAccent
                    )
                }
                if (rule.scoringTempoModifier != 0) {
                    MetricChip(
                        icon = Icons.Default.Speed,
                        label = "${if (rule.scoringTempoModifier > 0) "+" else ""}${rule.scoringTempoModifier}% Tempo",
                        tint = AmberAccent
                    )
                }
                if (rule.integrityImpact != 0) {
                    MetricChip(
                        icon = Icons.Default.Gavel,
                        label = "${if (rule.integrityImpact > 0) "+" else ""}${rule.integrityImpact} Integrity",
                        tint = CyanAccent
                    )
                }
                if (rule.fanAppealModifier != 0) {
                    MetricChip(
                        icon = Icons.Default.Star,
                        label = "${if (rule.fanAppealModifier > 0) "+" else ""}${rule.fanAppealModifier} Fan Appeal",
                        tint = GoldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Enacted vs. Repealed Effect Comparison Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = if (rule.isEnacted) "Active Effect:" else "Effect if Enacted:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            modifier = Modifier.width(105.dp)
                        )
                        Text(
                            text = rule.enactedEffect,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = if (!rule.isEnacted) "Active Repeal Effect:" else "Repeal Consequence:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonAlert,
                            modifier = Modifier.width(105.dp)
                        )
                        Text(
                            text = rule.repealedEffect,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Expandable Stakeholder Perspectives
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = "LEAGUE STAKEHOLDER DELIBERATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Pro Argument
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rule.proPerspective,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Contra Argument
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rule.contraPerspective,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: [Deliberation Toggle] & [Enact / Repeal Button]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onToggleExpand,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_expand_rule_${rule.id}")
                ) {
                    Text(
                        text = if (isExpanded) "Hide Stakeholder Debates ▲" else "Stakeholder Debates ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (rule.isEnacted) {
                    // Button to Vote on Repeal
                    OutlinedButton(
                        onClick = onToggleEnact,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CrimsonAlert
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, CrimsonAlert.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_repeal_rule_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val whipLabel = projectedVote?.let { " (${it.marginSummary})" } ?: ""
                        Text(
                            text = "Vote Repeal$whipLabel",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    // Button to Vote to Enact
                    Button(
                        onClick = onToggleEnact,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_enact_rule_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val whipLabel = projectedVote?.let { " (${it.marginSummary})" } ?: ""
                        Text(
                            text = "Vote Enact$whipLabel",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PlayoffRulesWorkbenchDialog(
    playoffRules: PlayoffRules,
    franchises: List<Franchise>,
    enabledConferences: Set<Conference>,
    enabledDivisions: Set<Division>,
    onSaveRules: (PlayoffRules) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFieldSize by remember { mutableStateOf(playoffRules.fieldSize) }
    var selectedByes by remember { mutableStateOf(playoffRules.firstRoundByesCount) }
    var selectedQualMode by remember { mutableStateOf(playoffRules.qualificationMode) }
    var selectedDivAutoBerth by remember { mutableStateOf(playoffRules.divisionWinnersAutoBerth) }
    var selectedSeedingMethod by remember { mutableStateOf(playoffRules.seedingMethod) }
    var selectedPlayIn by remember { mutableStateOf(playoffRules.playInTournamentEnabled) }

    // Keep byes within legal range when field size shrinks
    LaunchedEffect(selectedFieldSize) {
        val maxByes = selectedFieldSize / 2
        if (selectedByes > maxByes) {
            selectedByes = maxByes
        }
    }

    val draftRules = remember(selectedFieldSize, selectedByes, selectedQualMode, selectedDivAutoBerth, selectedSeedingMethod, selectedPlayIn) {
        PlayoffRules(
            fieldSize = selectedFieldSize,
            qualificationMode = selectedQualMode,
            divisionWinnersAutoBerth = selectedDivAutoBerth,
            seedingMethod = selectedSeedingMethod,
            firstRoundByesCount = selectedByes,
            playInTournamentEnabled = selectedPlayIn
        )
    }

    val possibleStructures = remember(franchises, enabledConferences, enabledDivisions) {
        PlayoffStructureEngine.generatePossibleStructures(franchises, enabledConferences, enabledDivisions)
    }
    val populatedConfs = remember(franchises) { franchises.map { it.conference }.distinct() }
    val populatedDivs = remember(franchises) { franchises.map { it.division }.distinct() }

    val projectedSeeds = remember(franchises, draftRules) {
        calculatePlayoffSeeds(franchises, draftRules)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .testTag("dialog_playoff_rules_workbench")
                .border(2.dp, EmeraldAccent, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMISSIONER PLAYOFF RULES CODEX",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Adaptive Playoff Structures & Eligibility",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_playoff_rules_workbench")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 0. Adaptive Structures for Current Conferences & Divisions
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CyanAccent.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "POSSIBLE STRUCTURES FOR CURRENT ALIGNMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent
                                )
                                Text(
                                    text = "League Topology: ${populatedConfs.size} Active Conferences (${populatedConfs.joinToString(", ") { it.shortCode }}) • ${populatedDivs.size} Populated Divisions • ${franchises.size} Franchises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                possibleStructures.forEach { option ->
                                    val isMatched = draftRules.fieldSize == option.rules.fieldSize &&
                                        draftRules.qualificationMode == option.rules.qualificationMode &&
                                        draftRules.seedingMethod == option.rules.seedingMethod &&
                                        draftRules.firstRoundByesCount == option.rules.firstRoundByesCount
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                selectedFieldSize = option.rules.fieldSize
                                                selectedQualMode = option.rules.qualificationMode
                                                selectedDivAutoBerth = option.rules.divisionWinnersAutoBerth
                                                selectedSeedingMethod = option.rules.seedingMethod
                                                selectedByes = option.rules.firstRoundByesCount
                                                selectedPlayIn = option.rules.playInTournamentEnabled
                                            }
                                            .border(
                                                width = if (isMatched) 1.5.dp else 1.dp,
                                                color = if (isMatched) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .testTag("btn_apply_playoff_structure_${option.id}"),
                                        color = if (isMatched) EmeraldAccent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMatched) EmeraldAccent else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (option.recommendedBadge != null) {
                                                    Surface(
                                                        color = if (isMatched) EmeraldAccent else GoldPrimary.copy(alpha = 0.18f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isMatched) "APPLIED" else option.recommendedBadge,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (isMatched) Color.Black else GoldPrimary,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${option.subtitle} • ${option.roundsSummary}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyanAccent,
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = option.rationale,
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

                    // 1. Field Size Selection
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "1. PLAYOFF FIELD SIZE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = "Number of franchises advancing to the postseason bracket (adaptive up to 12 teams for expanded global conferences)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val sizes = listOf(
                                    2 to "2 Teams\n(Finals)",
                                    4 to "4 Teams\n(Semifinals)",
                                    6 to "6 Teams\n(Wild Card)",
                                    8 to "8 Teams\n(Quarterfinals)",
                                    10 to "10 Teams\n(Global 10)",
                                    12 to "12 Teams\n(World 12)"
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    sizes.chunked(3).forEach { sizeRow ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            sizeRow.forEach { (size, label) ->
                                                val isSel = selectedFieldSize == size
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .defaultMinSize(minHeight = 48.dp)
                                                        .clickable { selectedFieldSize = size }
                                                        .border(
                                                            width = if (isSel) 2.dp else 1.dp,
                                                            color = if (isSel) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .testTag("btn_playoff_size_$size"),
                                                    color = if (isSel) EmeraldAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                                            color = if (isSel) EmeraldAccent else MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 10.sp,
                                                            lineHeight = 13.sp,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

                    // 2. First-Round Byes
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "2. FIRST-ROUND BYE PRIVILEGES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                    Surface(
                                        color = GoldPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${selectedByes} Byes Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Top regular season or conference winners skip the opening round and advance directly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val byeOptions = listOf(
                                    0 to "0 Byes",
                                    1 to "1 Bye",
                                    2 to "2 Byes",
                                    4 to "4 Byes",
                                    6 to "6 Byes"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    byeOptions.forEach { (count, label) ->
                                        val isSel = selectedByes == count
                                        val isEnabled = count <= selectedFieldSize / 2
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable(enabled = isEnabled) { selectedByes = count }
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .testTag("btn_byes_$count"),
                                            color = if (isSel) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = if (isEnabled) 1f else 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                                    color = if (isSel) GoldPrimary else if (isEnabled) MaterialTheme.colorScheme.onSurface else TextMuted,
                                                    fontSize = 10.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Division Champions & Wild Card Eligibility
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "3. ELIGIBILITY ALLOCATION & WILD CARDS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlueAccent
                                )
                                Text(
                                    text = "Defines how conference & division winners and wild card contenders earn tournament berths",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Division Champions Auto-Berth Toggle
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedDivAutoBerth = !selectedDivAutoBerth }
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedDivAutoBerth) EmeraldAccent else CrimsonAlert,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .testTag("btn_toggle_div_champs_berth"),
                                    color = if (selectedDivAutoBerth) EmeraldAccent.copy(alpha = 0.12f) else CrimsonAlert.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Division Champions Auto-Berth",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (selectedDivAutoBerth) "Guaranteed playoff berths & top seed priority" else "Repealed: Pure at-large league win% biding",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = if (selectedDivAutoBerth) EmeraldAccent else CrimsonAlert,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (selectedDivAutoBerth) "Enacted" else "Repealed",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedDivAutoBerth) Color.Black else Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Qualification Mode Selector (4 Adaptive Modes)
                                Text(
                                    text = "QUALIFICATION STRUCTURE (${populatedConfs.size} CONFERENCES)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val modes = listOf(
                                    PlayoffQualificationMode.CONFERENCE_SPLIT to ("btn_qual_mode_conf" to "Conference Allocation\n(Balanced Berths)"),
                                    PlayoffQualificationMode.CONFERENCE_CHAMPIONS_WILD_CARD to ("btn_qual_mode_conf_wc" to "Conf. Champions\n+ Global Wild Cards"),
                                    PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE to ("btn_qual_mode_div_showcase" to "All Division Winners\n+ At-Large Pool"),
                                    PlayoffQualificationMode.OVERALL_LEAGUE_TABLE to ("btn_qual_mode_overall" to "Single League Table\n(Best Overall Win%)")
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    modes.chunked(2).forEach { modeRow ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            modeRow.forEach { (mode, meta) ->
                                                val (tag, label) = meta
                                                val isSel = selectedQualMode == mode
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { selectedQualMode = mode }
                                                        .border(
                                                            width = if (isSel) 2.dp else 1.dp,
                                                            color = if (isSel) RoyalBlueAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .testTag(tag),
                                                    color = if (isSel) RoyalBlueAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                                            color = if (isSel) RoyalBlueAccent else MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 10.sp,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Play-In Tournament Toggle
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPlayIn = !selectedPlayIn }
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedPlayIn) AmberAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .testTag("btn_toggle_play_in"),
                                    color = if (selectedPlayIn) AmberAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Play-In Tournament (Seeds 7–10)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (selectedPlayIn) "Active: 4-team sudden death tournament for final 2 playoff seeds" else "Inactive: Top seeds clinch playoff berths directly",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = if (selectedPlayIn) AmberAccent else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (selectedPlayIn) "Active" else "Inactive",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedPlayIn) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Seeding Method
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "4. SEEDING & BRACKET ORDERING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "Determines how qualifiers are seeded in the tournament bracket",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val seedingOptions = listOf(
                                    PlayoffSeedingMethod.DIVISION_WINNERS_FIRST to ("Division Winners Top Seeds" to "btn_seeding_div_winners"),
                                    PlayoffSeedingMethod.PURE_WIN_PERCENTAGE to ("Strict Win Percentage" to "btn_seeding_pure_win_pct"),
                                    PlayoffSeedingMethod.CONFERENCE_PROTECTED to ("Conference Protected" to "btn_seeding_conf_protected")
                                )

                                seedingOptions.forEach { (method, pair) ->
                                    val (label, tag) = pair
                                    val isSel = selectedSeedingMethod == method
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { selectedSeedingMethod = method }
                                            .border(
                                                width = if (isSel) 2.dp else 1.dp,
                                                color = if (isSel) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .testTag(tag),
                                        color = if (isSel) GoldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = method.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            if (isSel) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Live Postseason Eligibility & Projection
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "5. LIVE POSTSEASON SEEDING PROJECTION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                    Text(
                                        text = "${projectedSeeds.size} Qualifiers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Real-time bracket seed simulation under your selected rules:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                projectedSeeds.forEach { seedInfo ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (seedInfo.hasBye) GoldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = if (seedInfo.hasBye) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "#${seedInfo.seed}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (seedInfo.hasBye) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = seedInfo.franchise.teamFullName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${seedInfo.franchise.wins}-${seedInfo.franchise.losses} • ${String.format("%.3f", seedInfo.franchise.winPct)} • ${seedInfo.franchise.alignmentLabel}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            Surface(
                                                color = when {
                                                    seedInfo.hasBye -> GoldPrimary.copy(alpha = 0.18f)
                                                    seedInfo.isDivisionChampion -> RoyalBlueAccent.copy(alpha = 0.18f)
                                                    else -> CyanAccent.copy(alpha = 0.18f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = seedInfo.eligibilityTag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when {
                                                        seedInfo.hasBye -> GoldPrimary
                                                        seedInfo.isDivisionChampion -> RoyalBlueAccent
                                                        else -> CyanAccent
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "PROJECTED TOURNAMENT BRACKET",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val confBreakdown = projectedSeeds.groupBy { it.franchise.conference }
                                    .entries
                                    .joinToString(" • ") { "${it.key.shortCode}: ${it.value.size}" }
                                if (confBreakdown.isNotBlank()) {
                                    Text(
                                        text = "Berths by Conference -> $confBreakdown",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                val bracketSummary = when {
                                    selectedFieldSize <= 2 -> "Championship Finals: #1 ${projectedSeeds.getOrNull(0)?.franchise?.nickname ?: "TBD"} vs #2 ${projectedSeeds.getOrNull(1)?.franchise?.nickname ?: "TBD"}"
                                    selectedFieldSize <= 4 -> if (selectedByes >= 1) {
                                        "Opening Semifinal: #2 vs #3 (Seed #1 on BYE) • Championship Finals: #1 vs Semifinal Winner"
                                    } else {
                                        "Semifinals: #1 vs #4 & #2 vs #3 • Championship Finals"
                                    }
                                    selectedFieldSize <= 6 -> "Wild Card Round: #3 vs #6 & #4 vs #5 (#1 & #2 BYE) • Semifinals • Finals"
                                    selectedFieldSize <= 8 -> "Quarterfinals: 1v8, 2v7, 3v6, 4v5 • Semifinals • Finals"
                                    selectedFieldSize <= 10 -> "Global Opening Round: 7v10 & 8v9 (Top 6 BYE) • Quarterfinals (8) • Semis • Finals"
                                    else -> "Global Opening Round: 5v12, 6v11, 7v10, 8v9 (Top 4 BYE) • Quarterfinals (8) • Semis • Finals"
                                }
                                Text(
                                    text = bracketSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action
                Button(
                    onClick = { onSaveRules(draftRules) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_ratify_playoff_rules"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ratify Playoff Rules Bylaw",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RulesCommissionerControlCard(
    ruleBook: com.example.model.LeagueRuleBook,
    onUpdateOvertime: (OvertimeFormat) -> Unit,
    onUpdateRoster: (RosterSizeRule) -> Unit,
    onUpdateCapStrictness: (SalaryCapStrictness) -> Unit,
    onUpdateInjuryFrequency: (InjuryFrequencyRule) -> Unit = {},
    onUpdateGameLength: (GameLengthRule) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_rules_commissioner_controls")
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Commissioner Controls",
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RULES COMMISSIONER",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Game-Wide Competition Rules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = EmeraldAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Commissioner Powers Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Direct executive control over league-wide gameplay parameters. Calibrate overtime resolution systems, enforce active and reserve roster capacity limits, and dictate financial salary cap strictness.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // 1. OVERTIME FORMAT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("control_overtime_format")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OVERTIME FORMAT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = AmberAccent,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        color = AmberAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ruleBook.overtimeFormat.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Detail display for current format
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = ruleBook.overtimeFormat.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = ruleBook.overtimeFormat.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+${ruleBook.overtimeFormat.scoringTempoBonus}% Scoring Tempo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+${ruleBook.overtimeFormat.fanAppealBonus}% Fan Excitement",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val formats = listOf(
                        OvertimeFormat.STANDARD_PERIOD to "chip_ot_standard",
                        OvertimeFormat.TARGET_SCORE_ELAM to "chip_ot_elam",
                        OvertimeFormat.SUDDEN_DEATH to "chip_ot_sudden_death",
                        OvertimeFormat.PENALTY_SHOOTOUT to "chip_ot_shootout"
                    )

                    formats.forEach { (format, tag) ->
                        val isSelected = ruleBook.overtimeFormat == format
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onUpdateOvertime(format) }
                                .testTag(tag),
                            color = if (isSelected) AmberAccent else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AmberAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = format.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. ROSTER SIZES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("control_roster_sizes")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = RoyalBlueAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROSTER SIZES & BENCH LIMITS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = RoyalBlueAccent,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        color = RoyalBlueAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${ruleBook.rosterSizeRule.activeSlots} Active + ${ruleBook.rosterSizeRule.reserveSlots} Res",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlueAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = ruleBook.rosterSizeRule.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = ruleBook.rosterSizeRule.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (ruleBook.rosterSizeRule.injuryResiliencyBonus >= 0) EmeraldAccent.copy(alpha = 0.12f) else CrimsonAlert.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HealthAndSafety,
                                        contentDescription = null,
                                        tint = if (ruleBook.rosterSizeRule.injuryResiliencyBonus >= 0) EmeraldAccent else CrimsonAlert,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Injury Resiliency: ${if (ruleBook.rosterSizeRule.injuryResiliencyBonus >= 0) "+${ruleBook.rosterSizeRule.injuryResiliencyBonus}%" else "${ruleBook.rosterSizeRule.injuryResiliencyBonus}%"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ruleBook.rosterSizeRule.injuryResiliencyBonus >= 0) EmeraldAccent else CrimsonAlert,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                color = if (ruleBook.rosterSizeRule.payrollBurdenModifier <= 0) EmeraldAccent.copy(alpha = 0.12f) else NeonAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = if (ruleBook.rosterSizeRule.payrollBurdenModifier <= 0) EmeraldAccent else NeonAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Payroll Overhead: ${if (ruleBook.rosterSizeRule.payrollBurdenModifier >= 0) "+${ruleBook.rosterSizeRule.payrollBurdenModifier}%" else "${ruleBook.rosterSizeRule.payrollBurdenModifier}%"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ruleBook.rosterSizeRule.payrollBurdenModifier <= 0) EmeraldAccent else NeonAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rosterRules = listOf(
                        RosterSizeRule.COMPACT_12 to ("12 Active (Compact)" to "chip_roster_compact"),
                        RosterSizeRule.STANDARD_15 to ("15 + 2 Two-Way" to "chip_roster_standard"),
                        RosterSizeRule.EXPANDED_18 to ("18 + 4 Taxi Squad" to "chip_roster_expanded")
                    )

                    rosterRules.forEach { (rule, pair) ->
                        val (label, tag) = pair
                        val isSelected = ruleBook.rosterSizeRule == rule
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onUpdateRoster(rule) }
                                .testTag(tag),
                            color = if (isSelected) RoyalBlueAccent else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) RoyalBlueAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // 3. SALARY CAP ENFORCEMENT STRICTNESS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("control_cap_strictness")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SALARY CAP STRICTNESS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ruleBook.salaryCapStrictness.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = ruleBook.salaryCapStrictness.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = ruleBook.salaryCapStrictness.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (ruleBook.salaryCapStrictness.parityImpact >= 0) EmeraldAccent.copy(alpha = 0.12f) else CrimsonAlert.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Balance,
                                        contentDescription = null,
                                        tint = if (ruleBook.salaryCapStrictness.parityImpact >= 0) EmeraldAccent else CrimsonAlert,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Parity: ${if (ruleBook.salaryCapStrictness.parityImpact >= 0) "+${ruleBook.salaryCapStrictness.parityImpact}" else "${ruleBook.salaryCapStrictness.parityImpact}"} pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ruleBook.salaryCapStrictness.parityImpact >= 0) EmeraldAccent else CrimsonAlert,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                color = CyanAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Percent,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tax Rate: ${ruleBook.salaryCapStrictness.penaltyRatePct}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val capRules = listOf(
                        SalaryCapStrictness.STRICT_HARD_CAP to "chip_strictness_hard",
                        SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX to "chip_strictness_progressive",
                        SalaryCapStrictness.MODERATE_SOFT_CAP to "chip_strictness_moderate",
                        SalaryCapStrictness.UNRESTRICTED_MARKET to "chip_strictness_unrestricted"
                    )

                    capRules.forEach { (strictness, tag) ->
                        val isSelected = ruleBook.salaryCapStrictness == strictness
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onUpdateCapStrictness(strictness) }
                                .testTag(tag),
                            color = if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strictness.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // 4. INJURY FREQUENCY
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("control_injury_frequency")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INJURY FREQUENCY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = NeonAmber,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Surface(
                        color = NeonAmber.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ruleBook.injuryFrequency.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val injuryChoices = listOf(
                        InjuryFrequencyRule.INJURIES_OFF to "chip_rules_injury_off",
                        InjuryFrequencyRule.LOW_PROTECTED to "chip_rules_injury_low",
                        InjuryFrequencyRule.STANDARD_REALISTIC to "chip_rules_injury_standard",
                        InjuryFrequencyRule.HIGH_GRIND to "chip_rules_injury_high"
                    )
                    injuryChoices.forEach { (freq, tag) ->
                        val isSelected = ruleBook.injuryFrequency == freq
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onUpdateInjuryFrequency(freq) }
                                .testTag(tag),
                            color = if (isSelected) NeonAmber else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NeonAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freq.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // 5. GAME LENGTH
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("control_game_length")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REGULATION GAME LENGTH",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Surface(
                        color = CyanAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ruleBook.gameLength.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val lengthChoices = listOf(
                        GameLengthRule.SHORTENED_36 to "chip_rules_length_36",
                        GameLengthRule.STANDARD_48 to "chip_rules_length_48",
                        GameLengthRule.MARATHON_60 to "chip_rules_length_60"
                    )
                    lengthChoices.forEach { (len, tag) ->
                        val isSelected = ruleBook.gameLength == len
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onUpdateGameLength(len) }
                                .testTag(tag),
                            color = if (isSelected) CyanAccent else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyanAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${len.shortLabel} (${len.scoreScaleMultiplier}x)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_open_full_settings_console"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Full Commissioner Settings Console",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
        }
    }
}

@Composable
fun TradeRulesWorkbenchDialog(
    tradeRules: com.example.model.TradeRules,
    currentWeek: Int,
    onSaveRules: (com.example.model.TradeRules) -> Unit,
    onDismiss: () -> Unit
) {
    var deadlineWeek by remember { mutableStateOf(tradeRules.tradeDeadlineWeek.toFloat()) }
    var allowDraftPicks by remember { mutableStateOf(tradeRules.allowDraftPickTrading) }
    var requireCommishApproval by remember { mutableStateOf(tradeRules.requireCommissionerApproval) }
    var salaryMatchingStrictness by remember { mutableStateOf(tradeRules.salaryMatchingStrictnessPct.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_trade_rules_workbench"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberWarning.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
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
                                .background(AmberWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TRADE GOVERNANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Trade Regulations Workbench",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("btn_close_trade_workbench")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trade Deadline Week Slider
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Trade Deadline Week",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current calendar: Week $currentWeek of 16",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = AmberWarning,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Week ${deadlineWeek.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = deadlineWeek,
                            onValueChange = { deadlineWeek = it },
                            valueRange = 4f..14f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = AmberWarning,
                                activeTrackColor = AmberWarning
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_trade_deadline_week")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Week 4 (Early)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            Text("Week 10 (Standard)", style = MaterialTheme.typography.labelSmall, color = AmberWarning, fontSize = 10.sp)
                            Text("Week 14 (Late)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Draft Pick Trading Switch
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                            Text(
                                text = "Draft Pick Trading",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Allow franchises to exchange future 1st & 2nd round selections",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = allowDraftPicks,
                            onCheckedChange = { allowDraftPicks = it },
                            modifier = Modifier.testTag("switch_allow_draft_picks")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Commissioner Approval Switch
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                            Text(
                                text = "Commissioner Veto Authority",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All trade deals require commissioner review before finalizing",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = requireCommishApproval,
                            onCheckedChange = { requireCommishApproval = it },
                            modifier = Modifier.testTag("switch_require_approval")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save / Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cancel_trade_rules")
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val updated = tradeRules.copy(
                                tradeDeadlineWeek = deadlineWeek.toInt(),
                                allowDraftPickTrading = allowDraftPicks,
                                requireCommissionerApproval = requireCommishApproval,
                                salaryMatchingStrictnessPct = salaryMatchingStrictness.toInt()
                            )
                            onSaveRules(updated)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_trade_rules")
                            .minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Bylaw", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
