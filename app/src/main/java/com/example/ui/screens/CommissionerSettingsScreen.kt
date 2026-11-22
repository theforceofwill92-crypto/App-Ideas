package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InitialLeagueData
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommissionerSettingsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler { viewModel.closeLeagueSettings() }

    val ruleBook = uiState.ruleBook
    val rules = ruleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
    val isSalaryCapEnabled = ruleBook.salaryCapStrictness != SalaryCapStrictness.UNRESTRICTED_MARKET &&
        (rules.find { it.id == "rule_salary_cap" }?.isEnacted ?: true)
    val isInjuriesEnabled = ruleBook.injuryFrequency != InjuryFrequencyRule.INJURIES_OFF

    var showResetConfirm by remember { mutableStateOf(false) }
    var capSliderValue by remember(uiState.unionAgreement.salaryCapMillions) {
        mutableStateOf(uiState.unionAgreement.salaryCapMillions.toFloat())
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("commissioner_settings_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Commissioner Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.leagueName} • Rule & Simulation Toggles",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeLeagueSettings() },
                        modifier = Modifier.testTag("btn_close_league_settings")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.testTag("btn_settings_reset_defaults")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Rules to Default",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("commissioner_settings_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Live Ruleset Summary Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_summary_banner"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ACTIVE SIMULATION PROFILE",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = GoldPrimary,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "Changes apply immediately to upcoming matches, free agency & standings.",
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
                                SettingsSummaryPill(
                                    label = "Salary Cap",
                                    value = if (isSalaryCapEnabled) "\$${uiState.unionAgreement.salaryCapMillions}M" else "OFF",
                                    accent = if (isSalaryCapEnabled) EmeraldAccent else CrimsonAlert,
                                    modifier = Modifier.weight(1f)
                                )
                                SettingsSummaryPill(
                                    label = "Injuries",
                                    value = ruleBook.injuryFrequency.shortLabel,
                                    accent = if (isInjuriesEnabled) AmberWarning else CyanAccent,
                                    modifier = Modifier.weight(1f)
                                )
                                SettingsSummaryPill(
                                    label = "Game Length",
                                    value = ruleBook.gameLength.shortLabel,
                                    accent = RoyalBlueAccent,
                                    modifier = Modifier.weight(1f)
                                )
                                SettingsSummaryPill(
                                    label = "Parity",
                                    value = "${uiState.competitiveParityScore}/100",
                                    accent = GoldPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2. Salary Cap & League Economics
                item {
                    SettingsSectionCard(
                        title = "SALARY CAP & ECONOMICS",
                        subtitle = "Toggle cap enforcement, ceiling amount, and small-market parity subsidies.",
                        icon = Icons.Default.AccountBalance,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.testTag("settings_section_salary_cap")
                    ) {
                        SettingsSwitchRow(
                            title = "Enforce League Salary Cap",
                            subtitle = if (isSalaryCapEnabled) {
                                "Active (${ruleBook.salaryCapStrictness.displayName}) at \$${uiState.unionAgreement.salaryCapMillions}M ceiling."
                            } else {
                                "Unrestricted Free Market — franchises can spend without payroll limits."
                            },
                            checked = isSalaryCapEnabled,
                            onCheckedChange = { viewModel.setSalaryCapEnabled(it) },
                            accentColor = EmeraldAccent,
                            testTag = "switch_salary_cap_enabled"
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Cap Enforcement Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val strictnessOptions = listOf(
                            SalaryCapStrictness.STRICT_HARD_CAP to "chip_settings_strictness_hard",
                            SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX to "chip_settings_strictness_progressive",
                            SalaryCapStrictness.MODERATE_SOFT_CAP to "chip_settings_strictness_moderate",
                            SalaryCapStrictness.UNRESTRICTED_MARKET to "chip_settings_strictness_unrestricted"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            strictnessOptions.forEach { (strictness, tag) ->
                                SettingsOptionChip(
                                    label = strictness.shortLabel,
                                    selected = ruleBook.salaryCapStrictness == strictness,
                                    accentColor = EmeraldAccent,
                                    onClick = { viewModel.updateSalaryCapStrictness(strictness) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tag)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ruleBook.salaryCapStrictness.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Annual Salary Cap Ceiling",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isSalaryCapEnabled) "\$${capSliderValue.roundToInt()}M / Team" else "Bypassed (Free Market)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSalaryCapEnabled) EmeraldAccent else TextSecondary
                                    )
                                }

                                Slider(
                                    value = capSliderValue,
                                    onValueChange = { capSliderValue = it },
                                    onValueChangeFinished = {
                                        viewModel.updateSalaryCapAmount(capSliderValue.roundToInt().toLong())
                                    },
                                    valueRange = 50f..220f,
                                    enabled = isSalaryCapEnabled,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("slider_salary_cap_millions"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = EmeraldAccent,
                                        activeTrackColor = EmeraldAccent
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(80L, 110L, 140L, 175L).forEach { preset ->
                                        val isCurrentPreset = uiState.unionAgreement.salaryCapMillions == preset
                                        SettingsOptionChip(
                                            label = "\$${preset}M",
                                            selected = isCurrentPreset && isSalaryCapEnabled,
                                            accentColor = EmeraldAccent,
                                            enabled = isSalaryCapEnabled,
                                            onClick = {
                                                capSliderValue = preset.toFloat()
                                                viewModel.updateSalaryCapAmount(preset)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("chip_cap_preset_$preset")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val revSharingOn = rules.find { it.id == "rule_revenue_sharing" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Small-Market Revenue Sharing",
                            subtitle = "Pools 25% of national TV rights and luxury taxes into competitive grants.",
                            checked = revSharingOn,
                            onCheckedChange = { desired ->
                                if (desired != revSharingOn) viewModel.toggleLeagueRule("rule_revenue_sharing")
                            },
                            accentColor = EmeraldAccent,
                            testTag = "switch_revenue_sharing"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val rookieScaleOn = rules.find { it.id == "rule_rookie_wage_scale" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Rookie Wage Scale",
                            subtitle = "Standardizes incoming draftee salaries over their first 4 pro seasons.",
                            checked = rookieScaleOn,
                            onCheckedChange = { desired ->
                                if (desired != rookieScaleOn) viewModel.toggleLeagueRule("rule_rookie_wage_scale")
                            },
                            accentColor = EmeraldAccent,
                            testTag = "switch_rookie_wage_scale"
                        )
                    }
                }

                // 3. Player Health, Injury Frequency & Roster Size
                item {
                    SettingsSectionCard(
                        title = "PLAYER HEALTH & INJURY FREQUENCY",
                        subtitle = "Control in-game injury rates across regular season and playoff simulations.",
                        icon = Icons.Default.MedicalServices,
                        accentColor = AmberWarning,
                        modifier = Modifier.testTag("settings_section_injuries")
                    ) {
                        SettingsSwitchRow(
                            title = "Enable In-Game Injuries",
                            subtitle = if (isInjuriesEnabled) {
                                "Active (${ruleBook.injuryFrequency.displayName}) — depth charts & medical staff matter."
                            } else {
                                "Injuries Disabled — players stay 100% healthy throughout every matchup."
                            },
                            checked = isInjuriesEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateInjuryFrequency(
                                    if (enabled) InjuryFrequencyRule.STANDARD_REALISTIC else InjuryFrequencyRule.INJURIES_OFF
                                )
                            },
                            accentColor = AmberWarning,
                            testTag = "switch_injuries_enabled"
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Injury Frequency Intensity",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val injuryOptions = listOf(
                            InjuryFrequencyRule.INJURIES_OFF to "chip_injury_off",
                            InjuryFrequencyRule.LOW_PROTECTED to "chip_injury_low",
                            InjuryFrequencyRule.STANDARD_REALISTIC to "chip_injury_standard",
                            InjuryFrequencyRule.HIGH_GRIND to "chip_injury_high"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            injuryOptions.forEach { (freq, tag) ->
                                SettingsOptionChip(
                                    label = freq.shortLabel,
                                    selected = ruleBook.injuryFrequency == freq,
                                    accentColor = AmberWarning,
                                    onClick = { viewModel.updateInjuryFrequency(freq) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tag)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ruleBook.injuryFrequency.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Active Roster Depth Limit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val rosterOptions = listOf(
                            RosterSizeRule.COMPACT_12 to ("12 Compact" to "chip_settings_roster_compact"),
                            RosterSizeRule.STANDARD_15 to ("15 Standard" to "chip_settings_roster_standard"),
                            RosterSizeRule.EXPANDED_18 to ("18 Expanded" to "chip_settings_roster_expanded")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rosterOptions.forEach { (rule, pair) ->
                                SettingsOptionChip(
                                    label = pair.first,
                                    selected = ruleBook.rosterSizeRule == rule,
                                    accentColor = RoyalBlueAccent,
                                    onClick = { viewModel.updateRosterSizeRule(rule) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(pair.second)
                                )
                            }
                        }
                    }
                }

                // 4. Game Length, Overtime & Gameplay Pace
                item {
                    SettingsSectionCard(
                        title = "GAME LENGTH & MATCH FORMAT",
                        subtitle = "Configure regulation length, scoring scale, overtime tiebreakers & pacing.",
                        icon = Icons.Default.Timer,
                        accentColor = CyanAccent,
                        modifier = Modifier.testTag("settings_section_gameplay")
                    ) {
                        Text(
                            text = "Regulation Game Length",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val lengthOptions = listOf(
                            GameLengthRule.SHORTENED_36 to "chip_game_length_36",
                            GameLengthRule.STANDARD_48 to "chip_game_length_48",
                            GameLengthRule.MARATHON_60 to "chip_game_length_60"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            lengthOptions.forEach { (length, tag) ->
                                val isSelected = ruleBook.gameLength == length
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 64.dp)
                                        .clickable(role = Role.RadioButton) {
                                            viewModel.updateGameLength(length)
                                        }
                                        .testTag(tag),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) CyanAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) CyanAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = length.shortLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) CyanAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${length.scoreScaleMultiplier}x Score",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ruleBook.gameLength.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Overtime Format",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val otOptions = listOf(
                            OvertimeFormat.STANDARD_PERIOD to "chip_settings_ot_standard",
                            OvertimeFormat.TARGET_SCORE_ELAM to "chip_settings_ot_target",
                            OvertimeFormat.SUDDEN_DEATH to "chip_settings_ot_sudden",
                            OvertimeFormat.PENALTY_SHOOTOUT to "chip_settings_ot_shootout"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otOptions.forEach { (ot, tag) ->
                                SettingsOptionChip(
                                    label = ot.shortLabel,
                                    selected = ruleBook.overtimeFormat == ot,
                                    accentColor = CyanAccent,
                                    onClick = { viewModel.updateOvertimeFormat(ot) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tag)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val isShotClockOn = rules.find { it.id == "rule_shot_clock" }?.isEnacted ?: ruleBook.shotClockFastPace
                        SettingsSwitchRow(
                            title = "Fast-Pace Shot Clock",
                            subtitle = "Accelerates offensive possessions for higher-scoring broadcasts.",
                            checked = isShotClockOn,
                            onCheckedChange = { viewModel.toggleShotClockPaceSetting(it) },
                            accentColor = CyanAccent,
                            testTag = "switch_shot_clock"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val isReplayOn = rules.find { it.id == "rule_instant_replay" }?.isEnacted ?: ruleBook.instantReplayEnabled
                        SettingsSwitchRow(
                            title = "Central Instant Replay Hub",
                            subtitle = "Eliminates disputed officiating errors in clutch 1–3 point finishes.",
                            checked = isReplayOn,
                            onCheckedChange = { viewModel.toggleInstantReplaySetting(it) },
                            accentColor = CyanAccent,
                            testTag = "switch_instant_replay"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val isFreedomOn = rules.find { it.id == "rule_freedom_of_movement" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Freedom of Movement Emphasis",
                            subtitle = "Strictly penalizes perimeter hand-checking to protect star playmakers.",
                            checked = isFreedomOn,
                            onCheckedChange = { desired ->
                                if (desired != isFreedomOn) viewModel.toggleLeagueRule("rule_freedom_of_movement")
                            },
                            accentColor = CyanAccent,
                            testTag = "switch_freedom_movement"
                        )
                    }
                }

                // 5. Postseason, Draft & Integrity Bylaws
                item {
                    SettingsSectionCard(
                        title = "PLAYOFFS, DRAFT & INTEGRITY",
                        subtitle = "Toggle postseason qualification, draft lottery odds, and codex integrity enforcement.",
                        icon = Icons.Default.Gavel,
                        accentColor = GoldPrimary,
                        modifier = Modifier.testTag("settings_section_governance")
                    ) {
                        val draftLotteryOn = rules.find { it.id == "rule_draft_lottery" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Weighted Draft Lottery",
                            subtitle = "Prevents deliberate tanking by awarding weighted odds to bottom 4 teams.",
                            checked = draftLotteryOn,
                            onCheckedChange = { desired ->
                                if (desired != draftLotteryOn) viewModel.toggleLeagueRule("rule_draft_lottery")
                            },
                            accentColor = GoldPrimary,
                            testTag = "switch_draft_lottery"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val divAutoOn = rules.find { it.id == "rule_division_auto_playoffs" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Division Winners Auto-Berth",
                            subtitle = "Guarantees every regional division leader a postseason spot.",
                            checked = divAutoOn,
                            onCheckedChange = { desired ->
                                if (desired != divAutoOn) viewModel.toggleLeagueRule("rule_division_auto_playoffs")
                            },
                            accentColor = GoldPrimary,
                            testTag = "switch_division_auto_playoffs"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val playInOn = rules.find { it.id == "rule_play_in_tournament" }?.isEnacted ?: false
                        SettingsSwitchRow(
                            title = "Global Play-In Tournament",
                            subtitle = "Adds single-elimination wild-card showcase before Round 1.",
                            checked = playInOn,
                            onCheckedChange = { desired ->
                                if (desired != playInOn) viewModel.toggleLeagueRule("rule_play_in_tournament")
                            },
                            accentColor = GoldPrimary,
                            testTag = "switch_play_in_tournament"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val antiDopingOn = rules.find { it.id == "rule_anti_doping" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Strict Anti-Doping Codex",
                            subtitle = "Independent random testing boosting League Integrity.",
                            checked = antiDopingOn,
                            onCheckedChange = { desired ->
                                if (desired != antiDopingOn) viewModel.toggleLeagueRule("rule_anti_doping")
                            },
                            accentColor = GoldPrimary,
                            testTag = "switch_anti_doping"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val antiGamblingOn = rules.find { it.id == "rule_anti_gambling" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Zero-Tolerance Gambling & Tampering",
                            subtitle = "Protects league integrity with automatic bans for wagering violations.",
                            checked = antiGamblingOn,
                            onCheckedChange = { desired ->
                                if (desired != antiGamblingOn) viewModel.toggleLeagueRule("rule_anti_gambling")
                            },
                            accentColor = GoldPrimary,
                            testTag = "switch_anti_gambling"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val tradeDeadlineOn = rules.find { it.id == "rule_trade_deadline" }?.isEnacted ?: true
                        SettingsSwitchRow(
                            title = "Mid-Season Trade Deadline",
                            subtitle = "Locks rosters ahead of the postseason push.",
                            checked = tradeDeadlineOn,
                            onCheckedChange = { desired ->
                                if (desired != tradeDeadlineOn) viewModel.toggleLeagueRule("rule_trade_deadline")
                            },
                            accentColor = GoldPrimary,
                            testTag = "switch_trade_deadline"
                        )
                    }
                }

                // Bottom Save & Close Button
                item {
                    Button(
                        onClick = { viewModel.closeLeagueSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .testTag("btn_settings_done"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apply & Return to League",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text(
                    text = "Restore Default League Settings?",
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    text = "Resets Salary Cap (\$80M Progressive Tax), Injury Frequency (Standard 1.0x), Game Length (48 Min), and all league charter rules back to initial defaults."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetRulesToDefault()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Restore Defaults", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSummaryPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor
            )
        )
    }
}

@Composable
private fun SettingsOptionChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            selected -> accentColor
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected && enabled) 1.5.dp else 1.dp,
            color = if (selected && enabled) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    selected -> Color.Black
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
