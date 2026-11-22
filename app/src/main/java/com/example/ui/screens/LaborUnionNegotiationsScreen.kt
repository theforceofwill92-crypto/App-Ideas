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
import com.example.model.SalaryCapType
import com.example.ui.components.LaborUnionNegotiationOverlay
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaborUnionNegotiationsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val union = uiState.unionAgreement

    var selectedSubTab by remember { mutableStateOf(0) } // 0: CBA & Salary Cap, 1: Player Reps & Demands, 2: Strike Crisis Center

    // Salary Cap & Payroll Rules
    var salaryCapMillions by remember(union.salaryCapMillions) { mutableStateOf(union.salaryCapMillions.toFloat()) }
    var salaryCapType by remember(union.salaryCapType) { mutableStateOf(union.salaryCapType) }

    // Pillar 1: Salaries & Compensation
    var revenueSharePct by remember(union.playerRevenueSharePct) { mutableStateOf(union.playerRevenueSharePct.toFloat()) }
    var minSalaryK by remember(union.minPlayerSalaryK) { mutableStateOf(union.minPlayerSalaryK.toFloat()) }
    var transitionFundMillions by remember(union.postCareerTransitionFundMillions) { mutableStateOf(union.postCareerTransitionFundMillions.toFloat()) }

    // Pillar 2: Safety Regulations
    var concussionProtocol by remember(union.independentConcussionProtocol) { mutableStateOf(union.independentConcussionProtocol) }
    var hybridTurfMandate by remember(union.advancedHybridTurfMandate) { mutableStateOf(union.advancedHybridTurfMandate) }
    var healthWelfareMillions by remember(union.playerHealthWelfareMillions) { mutableStateOf(union.playerHealthWelfareMillions.toFloat()) }

    // Pillar 3: Travel Conditions
    var privateCharters by remember(union.mandatoryPrivateCharterFlights) { mutableStateOf(union.mandatoryPrivateCharterFlights) }
    var backToBackLimits by remember(union.maxBackToBackGamesLimit) { mutableStateOf(union.maxBackToBackGamesLimit) }
    var fiveStarHotels by remember(union.fiveStarHotelSingleRooms) { mutableStateOf(union.fiveStarHotelSingleRooms) }

    // Dynamic Strike & Satisfaction Calculation
    val projectedSatisfaction by remember(
        revenueSharePct, minSalaryK, salaryCapMillions, salaryCapType, privateCharters, backToBackLimits,
        concussionProtocol, hybridTurfMandate, fiveStarHotels, healthWelfareMillions, transitionFundMillions
    ) {
        derivedStateOf {
            var score = 50
            score += ((revenueSharePct - 48) * 5).toInt()
            score += (((minSalaryK - 500) / 40)).toInt()
            score += (((salaryCapMillions - 80) * 0.25)).toInt() // Higher cap gives players more total payroll
            if (salaryCapType == SalaryCapType.FREE_MARKET) score += 6
            if (salaryCapType == SalaryCapType.HARD_CAP) score -= 4
            if (privateCharters) score += 12
            if (backToBackLimits) score += 10
            if (concussionProtocol) score += 8
            if (hybridTurfMandate) score += 6
            if (fiveStarHotels) score += 5
            score += (((healthWelfareMillions - 10) * 1.5)).toInt()
            score += ((transitionFundMillions * 1.5)).toInt()
            score.coerceIn(30, 99)
        }
    }

    val projectedStrikeRisk by remember(projectedSatisfaction) {
        derivedStateOf { (100 - projectedSatisfaction).coerceIn(2, 70) }
    }

    BackHandler(enabled = uiState.activeLaborNegotiationOverlay == null) {
        viewModel.openLaborUnionScreen(false)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("labor_union_negotiations_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Labor Union Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Collective Bargaining (CBA) • Strike Threats • Salary Caps",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openLaborUnionScreen(false) },
                        modifier = Modifier.testTag("btn_back_union_screen")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Surface(
                        color = (if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("chip_strike_threat_status")
                    ) {
                        Text(
                            text = if (projectedStrikeRisk > 30) "STRIKE THREAT: $projectedStrikeRisk%" else "LABOR PEACE: $projectedStrikeRisk%",
                            color = if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
                .testTag("labor_union_content"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)
        ) {
            // Union Mood & Strike Threat Gauge Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            1.5.dp,
                            if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent,
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("union_status_overview_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "PLAYERS ASSOCIATION COUNCIL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent
                                    )
                                    Text(
                                        text = "Collective Bargaining Accord",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${union.cbaYearsRemaining} Years Left on CBA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ProgressBarWithLabel(
                            label = "Union Satisfaction Rating",
                            progress = projectedSatisfaction / 100f,
                            displayValue = "$projectedSatisfaction% (${if (projectedSatisfaction >= 70) "Accord Likely" else if (projectedSatisfaction >= 50) "Tense Standoff" else "Strike Imminent"})",
                            color = if (projectedSatisfaction >= 70) EmeraldAccent else GoldPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ProgressBarWithLabel(
                            label = "Potential Strike Risk Meter",
                            progress = projectedStrikeRisk / 100f,
                            displayValue = when {
                                projectedStrikeRisk > 40 -> "$projectedStrikeRisk% (CRITICAL STRIKE WARNING)"
                                projectedStrikeRisk > 20 -> "$projectedStrikeRisk% (Simmering Discontent)"
                                else -> "$projectedStrikeRisk% (Labor Harmony Secured)"
                            },
                            color = if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.openLaborNegotiationOverlay() },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                (if (projectedStrikeRisk > 30) CrimsonAlert else GoldPrimary).copy(alpha = 0.65f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("btn_open_labor_negotiation_overlay")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = "Open Labor Negotiation Choices",
                                tint = if (projectedStrikeRisk > 30) CrimsonAlert else GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Convene Union Table Ultimatum & Projections",
                                fontWeight = FontWeight.Bold,
                                color = if (projectedStrikeRisk > 30) CrimsonAlert else GoldPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Sub-Tab Switcher (3 Tabs)
            item {
                TabRow(
                    selectedTabIndex = selectedSubTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = GoldPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    indicator = {}
                ) {
                    Tab(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        text = {
                            Text(
                                text = "CBA & Salary Cap",
                                fontWeight = if (selectedSubTab == 0) FontWeight.Black else FontWeight.Normal,
                                color = if (selectedSubTab == 0) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("tab_cba_salary_cap")
                    )
                    Tab(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        text = {
                            Text(
                                text = "Player Reps",
                                fontWeight = if (selectedSubTab == 1) FontWeight.Black else FontWeight.Normal,
                                color = if (selectedSubTab == 1) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("tab_player_reps")
                    )
                    Tab(
                        selected = selectedSubTab == 2,
                        onClick = { selectedSubTab = 2 },
                        text = {
                            Text(
                                text = "Strike Crisis",
                                fontWeight = if (selectedSubTab == 2) FontWeight.Black else FontWeight.Normal,
                                color = if (selectedSubTab == 2) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("tab_strike_threats")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (selectedSubTab == 0) {
                // ============================================
                // === 0. CBA & SALARY CAP CONFIGURATION ======
                // ============================================

                // Salary Cap Management Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp))
                            .testTag("card_salary_cap_controls"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "SALARY CAP & PAYROLL RULES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = GoldPrimary,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "\$${salaryCapMillions.toInt()}M CAP",
                                        fontWeight = FontWeight.Black,
                                        color = GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Calibrate team payroll ceilings and spending flexibility to maintain competitive balance between small-market clubs and big-market spenders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Salary Cap Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Franchise Salary Cap Level:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("\$${salaryCapMillions.toInt()}M / team", fontWeight = FontWeight.Black, color = GoldPrimary)
                            }

                            Slider(
                                value = salaryCapMillions,
                                onValueChange = { salaryCapMillions = it },
                                valueRange = 60f..160f,
                                steps = 19,
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                                modifier = Modifier.testTag("slider_salary_cap")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("\$60M Strict Parity", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                val minPayrollFloor = (salaryCapMillions * 0.90).toInt()
                                Text("Payroll Floor: \$$minPayrollFloor M (90%)", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent, fontWeight = FontWeight.Bold)
                                Text("\$160M Super-Payroll", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("SALARY CAP ENFORCEMENT MECHANISM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Three Cap Type Cards
                            SalaryCapType.values().forEach { capType ->
                                val isSelected = salaryCapType == capType
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable { salaryCapType = capType }
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .testTag(
                                            when (capType) {
                                                SalaryCapType.HARD_CAP -> "btn_cap_type_hard"
                                                SalaryCapType.SOFT_CAP_LUXURY_TAX -> "btn_cap_type_soft"
                                                SalaryCapType.FREE_MARKET -> "btn_cap_type_free"
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { salaryCapType = capType },
                                            colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(capType.displayName, fontWeight = FontWeight.Bold, color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = when (capType) {
                                                    SalaryCapType.HARD_CAP -> "Zero payroll exceptions. Highest competitive parity score (+8), strictly enforces parity across all franchises."
                                                    SalaryCapType.SOFT_CAP_LUXURY_TAX -> "Franchises can exceed cap with a 100% luxury tax dollar-for-dollar penalty pooled into union & parity funds."
                                                    SalaryCapType.FREE_MARKET -> "Unlimited payroll spending. Favored by superstar athletes and billionaire owners; reduces competitive balance."
                                                },
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Player Salaries & Revenue Split Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .testTag("card_salaries_pillar"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Paid, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PLAYER SALARIES & REVENUE SPLIT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Revenue Share Split
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("League Revenue Split to Players:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Surface(color = EmeraldAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                    Text("${revenueSharePct.toInt()}% Players / ${100 - revenueSharePct.toInt()}% Owners", fontWeight = FontWeight.Black, color = EmeraldAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Slider(
                                value = revenueSharePct,
                                onValueChange = { revenueSharePct = it },
                                valueRange = 46f..55f,
                                steps = 8,
                                colors = SliderDefaults.colors(thumbColor = EmeraldAccent, activeTrackColor = EmeraldAccent),
                                modifier = Modifier.testTag("slider_revenue_share")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Minimum Player Salary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Minimum Player Salary Floor:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Surface(color = CyanAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                    Text("\$${minSalaryK.toInt()}K / yr", fontWeight = FontWeight.Black, color = CyanAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Slider(
                                value = minSalaryK,
                                onValueChange = { minSalaryK = it },
                                valueRange = 500f..1500f,
                                steps = 9,
                                colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                                modifier = Modifier.testTag("slider_min_salary")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Post-Career Pension & Transition Fund
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Post-Career Transition Fund:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("\$${transitionFundMillions.toInt()}M Pool", fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                            Slider(
                                value = transitionFundMillions,
                                onValueChange = { transitionFundMillions = it },
                                valueRange = 2f..15f,
                                steps = 12,
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                                modifier = Modifier.testTag("slider_transition_fund")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else if (selectedSubTab == 1) {
                // ============================================
                // === 1. PLAYER REPS & DEMANDS ===============
                // ============================================

                item {
                    Text(
                        text = "PLAYER REPRESENTATIVES DELEGATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Rep 1: DeMarcus Vance
                item {
                    PlayerRepCard(
                        name = "DeMarcus 'Iron Will' Vance",
                        title = "Union Executive Director & Veteran Rep",
                        focus = "Medical welfare, independent concussion neurologists, career transition fund",
                        quote = "Players put their lives on the line for 82 fixtures a year. We will not compromise on independent sideline doctors and post-career healthcare.",
                        approvalPct = (projectedSatisfaction + 2).coerceIn(30, 99),
                        accentColor = EmeraldAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Rep 2: Jaxson Montgomery
                item {
                    PlayerRepCard(
                        name = "Jaxson 'Flash' Montgomery",
                        title = "All-Pro Superstar Player Council Leader",
                        focus = "Salary cap ceiling, luxury tax exceptions, private jet travel",
                        quote = "Do not suppress star compensation with an immovable hard cap. We want luxury charter flights so our bodies can recover after back-to-back cross-country road games.",
                        approvalPct = if (privateCharters && salaryCapMillions >= 85) 86 else 52,
                        accentColor = GoldPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Rep 3: Marcus Delgado
                item {
                    PlayerRepCard(
                        name = "Marcus 'Rookie' Delgado",
                        title = "Bench & Developmental Talent Representative",
                        focus = "Minimum salary floor (\$750K+), schedule fatigue limits (no 3-in-4s), single hotel rooms",
                        quote = "The bottom half of the roster needs security. Guarantee a living floor and end brutal 3 games in 4 nights travel schedules.",
                        approvalPct = if (minSalaryK >= 750 && backToBackLimits) 88 else 45,
                        accentColor = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Safety Regulations Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .testTag("card_safety_pillar"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLAYER SAFETY & MEDICAL REGULATIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = EmeraldAccent)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Independent Concussion Protocol", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Mandates neutral sideline neurologist evaluation before return-to-play.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = concussionProtocol, onCheckedChange = { concussionProtocol = it }, modifier = Modifier.testTag("toggle_concussion_protocol"))
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Advanced Hybrid Turf Mandate", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Eliminates high-friction artificial surfaces to prevent non-contact injuries.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = hybridTurfMandate, onCheckedChange = { hybridTurfMandate = it }, modifier = Modifier.testTag("toggle_hybrid_turf"))
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Annual Medical & Rehab Fund:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("\$${healthWelfareMillions.toInt()}M / yr", fontWeight = FontWeight.Bold, color = EmeraldAccent)
                            }
                            Slider(
                                value = healthWelfareMillions,
                                onValueChange = { healthWelfareMillions = it },
                                valueRange = 10f..30f,
                                steps = 9,
                                colors = SliderDefaults.colors(thumbColor = EmeraldAccent, activeTrackColor = EmeraldAccent),
                                modifier = Modifier.testTag("slider_health_fund")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Travel Conditions Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .testTag("card_travel_pillar"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("TRAVEL CONDITIONS & LOAD FATIGUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = CyanAccent)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Mandatory Private Jet Charters", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Bans commercial flights; mandates spacious luxury aircraft for all road games.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = privateCharters, onCheckedChange = { privateCharters = it }, modifier = Modifier.testTag("toggle_private_charters"))
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Schedule Fatigue Limits (No 3-in-4s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Eliminates grueling 3 games in 4 nights; guarantees 24h rest post-flight.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = backToBackLimits, onCheckedChange = { backToBackLimits = it }, modifier = Modifier.testTag("toggle_back_to_back_limits"))
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Five-Star Single-Occupancy Hotel Suites", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Guarantees private suites in premier hotels for athlete recovery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = fiveStarHotels, onCheckedChange = { fiveStarHotels = it }, modifier = Modifier.testTag("toggle_five_star_hotels"))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else {
                // ============================================
                // === 2. STRIKE CRISIS CENTER ================
                // ============================================

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent, RoundedCornerShape(16.dp))
                            .testTag("card_strike_crisis_center"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (projectedStrikeRisk > 30) CrimsonAlert.copy(alpha = 0.12f) else EmeraldAccent.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = if (projectedStrikeRisk > 30) Icons.Default.Warning else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent
                                    )
                                    Text(
                                        text = if (projectedStrikeRisk > 30) "STRIKE THREAT ALERT" else "LABOR PEACE PRESERVED",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$projectedStrikeRisk% PROBABILITY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (projectedStrikeRisk > 30) CrimsonAlert else EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (projectedStrikeRisk > 30) {
                                    "Player reps have authorized a union walkout vote. If a Collective Bargaining Agreement is not signed before kickoff, teams will strike, freezing television payouts and arena revenues."
                                } else {
                                    "Tensions are within acceptable operational limits. Players are willing to take the field under the current negotiated CBA framework."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Estimated Financial Damage
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("REVENUE AT RISK PER STRIKE WEEK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text("-\$18.5M / wk in lost media & gate receipts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = CrimsonAlert)
                                    }
                                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = CrimsonAlert)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Interactive De-escalation & Crisis Interventions
                item {
                    Text(
                        text = "COMMISSIONER CRISIS INTERVENTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("card_crisis_actions"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Action 1: Grant Emergency Welfare
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Inject Emergency Medical Grant (-\$5M)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Deposits \$5M immediately into player welfare fund to cool strike tension.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.handleStrikeDeescalationGrant(5L) },
                                    enabled = uiState.cashReservesMillions >= 5L,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_deescalate_grant")
                                ) {
                                    Text("Grant (\$5M)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            // Action 2: Call Emergency Summit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Convene Emergency Player-Owner Summit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Mediates between player reps and franchise owners in closed-door talks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.callEmergencyPlayerSummit() },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_emergency_summit")
                                ) {
                                    Text("Mediate", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            // Action 3: Threaten Lockout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Threaten Management Lockout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CrimsonAlert)
                                    Text("Hardline stance. Boosts owner leverage but escalates union strike resolve.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.threatenManagementLockout() },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_threaten_lockout")
                                ) {
                                    Text("Hardline", color = CrimsonAlert, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // ============================================
            // === RATIFICATION SUMMARY & EXECUTION =======
            // ============================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(2.dp, GoldPrimary, RoundedCornerShape(16.dp))
                        .testTag("card_cba_ratification_summary"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NEGOTIATED CBA TERMS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = GoldPrimary)
                            Surface(
                                color = (if (projectedStrikeRisk <= 20) EmeraldAccent else GoldPrimary).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (projectedStrikeRisk <= 20) "STRIKE AVERTED" else "STRIKE RISK: $projectedStrikeRisk%",
                                    color = if (projectedStrikeRisk <= 20) EmeraldAccent else GoldPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("UNION MOOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$projectedSatisfaction/100", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = EmeraldAccent)
                            }
                            Column {
                                Text("SALARY CAP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\$${salaryCapMillions.toInt()}M", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = GoldPrimary)
                            }
                            Column {
                                Text("REVENUE SPLIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${revenueSharePct.toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                            }
                            Column {
                                Text("MIN SALARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\$${minSalaryK.toInt()}K", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = CyanAccent)
                            }
                        }

                        val projectedBoardVote = remember(
                            uiState.franchises,
                            revenueSharePct,
                            salaryCapType,
                            salaryCapMillions,
                            minSalaryK,
                            healthWelfareMillions,
                            privateCharters,
                            backToBackLimits,
                            concussionProtocol,
                            hybridTurfMandate,
                            transitionFundMillions,
                            fiveStarHotels
                        ) {
                            viewModel.previewCbaOwnerVotes(
                                revenueSharePct = revenueSharePct.toInt(),
                                salaryCapType = salaryCapType,
                                salaryCapMillions = salaryCapMillions.toLong(),
                                minSalaryK = minSalaryK.toLong(),
                                playerHealthWelfareMillions = healthWelfareMillions.toLong(),
                                mandatoryPrivateCharters = privateCharters,
                                backToBackLimits = backToBackLimits,
                                concussionProtocol = concussionProtocol,
                                hybridTurfMandate = hybridTurfMandate,
                                transitionFundMillions = transitionFundMillions.toLong(),
                                fiveStarHotels = fiveStarHotels
                            )
                        }

                        com.example.ui.components.BoardVotePreviewStrip(
                            voteResult = projectedBoardVote,
                            title = "OWNER BOARD VOTE WHIP COUNT"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.submitCbaToOwnerVote(
                                    revenueSharePct = revenueSharePct.toInt(),
                                    salaryCapType = salaryCapType,
                                    salaryCapMillions = salaryCapMillions.toLong(),
                                    minSalaryK = minSalaryK.toLong(),
                                    playerHealthWelfareMillions = healthWelfareMillions.toLong(),
                                    mandatoryPrivateCharters = privateCharters,
                                    backToBackLimits = backToBackLimits,
                                    concussionProtocol = concussionProtocol,
                                    hybridTurfMandate = hybridTurfMandate,
                                    transitionFundMillions = transitionFundMillions.toLong(),
                                    fiveStarHotels = fiveStarHotels
                                )
                            },
                            enabled = projectedSatisfaction >= 45,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (projectedBoardVote.passed) GoldPrimary else AmberWarning
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("btn_ratify_final_cba"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.HowToVote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (projectedBoardVote.passed) {
                                    "Call Board Vote & Ratify CBA (${projectedBoardVote.marginSummary})"
                                } else {
                                    "Put CBA to Board Vote (At Risk ${projectedBoardVote.marginSummary})"
                                },
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        }

        if (uiState.activeLaborNegotiationOverlay != null) {
            LaborUnionNegotiationOverlay(
                session = uiState.activeLaborNegotiationOverlay!!,
                onSelectChoice = { choiceId -> viewModel.resolveLaborNegotiationChoice(choiceId) },
                onDismiss = { viewModel.dismissLaborNegotiationOverlay() },
                onOpenCbaTable = null
            )
        }
    }
}

@Composable
fun PlayerRepCard(
    name: String,
    title: String,
    focus: String,
    quote: String,
    approvalPct: Int,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                    }
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }

                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$approvalPct% Approval",
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.FormatQuote, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\"$quote\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Core Demands: $focus",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
