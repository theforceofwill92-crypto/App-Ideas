package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InitialLeagueData
import com.example.model.BroadcastBidStatus
import com.example.model.BroadcastContract
import com.example.model.Conference
import com.example.model.FanLoyaltyEngine
import com.example.model.FinancialSnapshot
import com.example.model.Franchise
import com.example.model.FranchiseFinancialStatement
import com.example.model.SalaryCapStrictness
import com.example.model.calculateFinancialStatement
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember(uiState.selectedFinancesSubTab) { mutableStateOf(uiState.selectedFinancesSubTab) }
    var timeFilterIndex by remember { mutableStateOf(0) } // 0: All Weeks, 1: Recent 6 Weeks

    // Franchise Revenues Filters & Cap Feedback
    var selectedConferenceFilter by remember { mutableStateOf<Conference?>(null) }
    var revenueSortMode by remember { mutableStateOf(0) } // 0: Total, 1: Ticket, 2: Merch, 3: Broadcast, 4: Margin
    var editingTicketPriceFranchise by remember { mutableStateOf<Franchise?>(null) }
    var viewingStatementFranchise by remember { mutableStateOf<FranchiseFinancialStatement?>(null) }
    var capActionFeedback by remember { mutableStateOf<String?>(null) }

    val salaryCapMillions = uiState.unionAgreement.salaryCapMillions
    val capStrictness = uiState.ruleBook.salaryCapStrictness
    val overCapTeamsCount = remember(uiState.franchises, salaryCapMillions) {
        uiState.franchises.count { it.annualPayrollMillions > salaryCapMillions }
    }
    val estimatedLuxuryTaxPool = remember(uiState.franchises, salaryCapMillions, capStrictness) {
        if (capStrictness == SalaryCapStrictness.UNRESTRICTED_MARKET) 0L
        else {
            val rate = if (capStrictness == SalaryCapStrictness.STRICT_HARD_CAP) 1.0 else capStrictness.penaltyRatePct / 100.0
            uiState.franchises.sumOf { f ->
                val over = (f.annualPayrollMillions - salaryCapMillions).coerceAtLeast(0L)
                if (over > 0L) kotlin.math.round(over * rate).toLong().coerceAtLeast(1L) else 0L
            }
        }
    }

    val broadcastDeals = InitialLeagueData.getInitialBroadcastDeals()
    val snapshots = uiState.financialSnapshots
    val filteredSnapshots = remember(snapshots, timeFilterIndex) {
        if (timeFilterIndex == 1 && snapshots.size > 6) {
            snapshots.takeLast(6)
        } else {
            snapshots
        }
    }

    // Franchise Financial Statements
    val franchiseStatements = remember(uiState.franchises, uiState.activeBroadcast) {
        uiState.franchises.map { it.calculateFinancialStatement(uiState.activeBroadcast, uiState.franchises.size) }
    }

    val totalLeagueTicketRevenue = remember(franchiseStatements) { franchiseStatements.sumOf { it.ticketRevenueMillions } }
    val totalLeagueMerchRevenue = remember(franchiseStatements) { franchiseStatements.sumOf { it.merchandiseRevenueMillions } }
    val totalLeagueBroadcastRevenue = remember(franchiseStatements) { franchiseStatements.sumOf { it.broadcastRevenueMillions } }
    val totalLeagueFranchiseRevenue = remember(franchiseStatements) { franchiseStatements.sumOf { it.totalRevenueMillions } }

    val avgTicketPrice = remember(uiState.franchises) {
        if (uiState.franchises.isNotEmpty()) uiState.franchises.map { it.ticketPrice }.average().toInt() else 65
    }
    val avgAttendancePct = remember(franchiseStatements) {
        if (franchiseStatements.isNotEmpty()) franchiseStatements.map { it.attendancePct }.average().toInt() else 85
    }
    val topMerchTeam = remember(franchiseStatements) { franchiseStatements.maxByOrNull { it.merchandiseRevenueMillions } }
    val topTicketTeam = remember(franchiseStatements) { franchiseStatements.maxByOrNull { it.ticketRevenueMillions } }

    val easternCount = remember(uiState.franchises) { uiState.franchises.count { it.conference == Conference.EASTERN } }
    val westernCount = remember(uiState.franchises) { uiState.franchises.count { it.conference == Conference.WESTERN } }

    // Filtered & Sorted Franchise Statements
    val filteredFranchiseStatements = remember(franchiseStatements, uiState.franchises, selectedConferenceFilter, revenueSortMode) {
        val filtered = franchiseStatements.filter { stmt ->
            val franchise = uiState.franchises.find { it.id == stmt.franchiseId }
            selectedConferenceFilter == null || franchise?.conference == selectedConferenceFilter
        }
        when (revenueSortMode) {
            1 -> filtered.sortedByDescending { it.ticketRevenueMillions }
            2 -> filtered.sortedByDescending { it.merchandiseRevenueMillions }
            3 -> filtered.sortedByDescending { it.broadcastRevenueMillions }
            4 -> filtered.sortedByDescending { it.profitMarginPct }
            else -> filtered.sortedByDescending { it.totalRevenueMillions }
        }
    }

    // Revenue streams totals for Treasury Charts
    val broadcastRevAnnual = uiState.activeBroadcast.annualPayoutMillions * 0.15f
    val sponsorRevAnnual = (uiState.franchises.sumOf { it.localSponsorRevenueMillions } * 0.15f)
    val expansionRevAnnual = uiState.franchises.size * 5f
    val luxuryTaxAnnual = uiState.financialSnapshots.sumOf { it.luxuryTaxRevenue.toDouble() }.toFloat().coerceAtLeast(3f)

    val revenueSlices = listOf(
        SliceItem("Broadcast Rights", broadcastRevAnnual, CyanAccent),
        SliceItem("Local Sponsors", sponsorRevAnnual, EmeraldAccent),
        SliceItem("Expansion Fees", expansionRevAnnual, GoldPrimary),
        SliceItem("Luxury Tax", luxuryTaxAnnual, Color(0xFFA855F7))
    )

    // Operating expenses totals for Treasury Charts
    val opsExpense = (3f + uiState.franchises.size * 0.5f) * 16f
    val marketingExpense = uiState.campaigns.filter { it.isActive }.sumOf { it.costMillions.toDouble() }.toFloat().coerceAtLeast(12f)
    val welfareExpense = uiState.unionAgreement.playerHealthWelfareMillions.toFloat()
    val recruitmentExpense = 8f

    val expenseSlices = listOf(
        SliceItem("League Operations", opsExpense, RoyalBlueAccent),
        SliceItem("Marketing Blitz", marketingExpense, GoldPrimary),
        SliceItem("Player Welfare", welfareExpense, EmeraldAccent),
        SliceItem("Talent Grants", recruitmentExpense, CrimsonAlert)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("finances_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "Financial Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "League revenue, salary caps & broadcast rights",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Top Unified Financial Overview Hub (Revenue • Salary Caps • Broadcast)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .testTag("financial_overview_hero_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinancialOverviewPillarTile(
                        title = "REVENUE",
                        value = "\$${String.format("%.0f", totalLeagueFranchiseRevenue)}M",
                        subtitle = "Cash \$${uiState.cashReservesMillions}M",
                        accent = EmeraldAccent,
                        onClick = {
                            selectedSubTab = 0
                            viewModel.setFinancesSubTab(0)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overview_pillar_revenue")
                    )
                    FinancialOverviewPillarTile(
                        title = "SALARY CAP",
                        value = "\$${salaryCapMillions}M",
                        subtitle = if (overCapTeamsCount > 0) "$overCapTeamsCount over" else "Compliant",
                        accent = if (overCapTeamsCount > 0) AmberAccent else CyanAccent,
                        onClick = {
                            selectedSubTab = 3
                            viewModel.setFinancesSubTab(3)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overview_pillar_salary_cap")
                    )
                    FinancialOverviewPillarTile(
                        title = "BROADCAST",
                        value = "\$${uiState.activeBroadcast.annualPayoutMillions}M/yr",
                        subtitle = "${uiState.activeBroadcast.yearsRemaining} yrs left",
                        accent = GoldPrimary,
                        onClick = {
                            selectedSubTab = 2
                            viewModel.setFinancesSubTab(2)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overview_pillar_broadcast")
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Sub-Tab Switcher (4 Tabs: Revenues, Treasury, Broadcast, Caps)
        item {
            TabRow(
                selectedTabIndex = selectedSubTab.coerceIn(0, 3),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = GoldPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                indicator = {}
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = {
                        selectedSubTab = 0
                        viewModel.setFinancesSubTab(0)
                    },
                    text = {
                        Text(
                            text = "Revenues",
                            fontWeight = if (selectedSubTab == 0) FontWeight.Black else FontWeight.Normal,
                            color = if (selectedSubTab == 0) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tab_franchise_revenues")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = {
                        selectedSubTab = 1
                        viewModel.setFinancesSubTab(1)
                    },
                    text = {
                        Text(
                            text = "Treasury",
                            fontWeight = if (selectedSubTab == 1) FontWeight.Black else FontWeight.Normal,
                            color = if (selectedSubTab == 1) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tab_financial_charts")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = {
                        selectedSubTab = 2
                        viewModel.setFinancesSubTab(2)
                    },
                    text = {
                        Text(
                            text = "Broadcast",
                            fontWeight = if (selectedSubTab == 2) FontWeight.Black else FontWeight.Normal,
                            color = if (selectedSubTab == 2) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tab_commercial_deals")
                )
                Tab(
                    selected = selectedSubTab == 3,
                    onClick = {
                        selectedSubTab = 3
                        viewModel.setFinancesSubTab(3)
                    },
                    text = {
                        Text(
                            text = "Caps",
                            fontWeight = if (selectedSubTab == 3) FontWeight.Black else FontWeight.Normal,
                            color = if (selectedSubTab == 3) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tab_salary_caps")
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (selectedSubTab == 0) {
            // ==========================================
            // === 0. FRANCHISE FINANCIAL OVERVIEW TAB ===
            // ==========================================

            // 1. Three Core Revenue Stream KPI Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_ticket_sales"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                                Text("TICKET SALES", style = MaterialTheme.typography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\$${String.format("%.1f", totalLeagueTicketRevenue)}M",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Text(
                                text = "Avg \$${avgTicketPrice}/tkt • ${avgAttendancePct}% Att.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_merchandise"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Checkroom, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                Text("MERCHANDISE", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\$${String.format("%.1f", totalLeagueMerchRevenue)}M",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "Top: ${topMerchTeam?.franchiseName?.split(" ")?.lastOrNull() ?: "Empire"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_broadcast_deals"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                Text("BROADCAST DEALS", style = MaterialTheme.typography.labelSmall, color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\$${String.format("%.1f", totalLeagueBroadcastRevenue)}M",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent
                            )
                            Text(
                                text = "Central + RSN Rights",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. Revenue Split Composition Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("league_revenue_split_card")
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AGGREGATE FRANCHISE REVENUE CONTRIBUTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Text(
                                text = "Total: \$${String.format("%.1f", totalLeagueFranchiseRevenue)}M",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Segmented Multi-Color Revenue Bar
                        val ticketPct = if (totalLeagueFranchiseRevenue > 0) (totalLeagueTicketRevenue / totalLeagueFranchiseRevenue).toFloat() else 0.33f
                        val merchPct = if (totalLeagueFranchiseRevenue > 0) (totalLeagueMerchRevenue / totalLeagueFranchiseRevenue).toFloat() else 0.33f
                        val bcastPct = if (totalLeagueFranchiseRevenue > 0) (totalLeagueBroadcastRevenue / totalLeagueFranchiseRevenue).toFloat() else 0.34f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(ticketPct.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .background(GoldPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(merchPct.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .background(EmeraldAccent)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(bcastPct.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .background(CyanAccent)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldPrimary))
                                Text("Ticket Sales (${(ticketPct * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldAccent))
                                Text("Merchandise (${(merchPct * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanAccent))
                                Text("Broadcast Deals (${(bcastPct * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2b. League-Wide Revenue Management Card
            item {
                val isSharingActive = uiState.ruleBook.isRuleEnacted("rule_revenue_sharing")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .testTag("card_league_revenue_controls"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "LEAGUE-WIDE REVENUE MANAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Gate Pricing Directive & Revenue Sharing Pool",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.applyLeagueWideTicketPriceAdjustment(-5) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_league_ticket_relief")
                            ) {
                                Text("-\$5 Gate Relief", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.applyLeagueWideTicketPriceAdjustment(5) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_league_ticket_premium")
                            ) {
                                Text("+\$5 Gate Surge", color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Small-Market Revenue Sharing Pool",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSharingActive) "Enacted • Pooling 15% gate/tax for parity" else "Inactive • Franchises retain full local gate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSharingActive,
                                onCheckedChange = { viewModel.toggleLeagueRule("rule_revenue_sharing") },
                                modifier = Modifier.testTag("switch_revenue_sharing")
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Conference Filters & Sorting Bar
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CONFERENCE FILTER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedConferenceFilter == null,
                                onClick = { selectedConferenceFilter = null },
                                label = { Text("All (${franchiseStatements.size})") },
                                modifier = Modifier.testTag("filter_conf_all")
                            )
                            FilterChip(
                                selected = selectedConferenceFilter == Conference.EASTERN,
                                onClick = { selectedConferenceFilter = Conference.EASTERN },
                                label = { Text("East (${easternCount})") },
                                modifier = Modifier.testTag("filter_conf_east")
                            )
                            FilterChip(
                                selected = selectedConferenceFilter == Conference.WESTERN,
                                onClick = { selectedConferenceFilter = Conference.WESTERN },
                                label = { Text("West (${westernCount})") },
                                modifier = Modifier.testTag("filter_conf_west")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "SORT FRANCHISES BY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = revenueSortMode == 0,
                                onClick = { revenueSortMode = 0 },
                                label = { Text("Total Revenue") },
                                modifier = Modifier.testTag("sort_rev_total")
                            )
                        }
                        item {
                            FilterChip(
                                selected = revenueSortMode == 1,
                                onClick = { revenueSortMode = 1 },
                                label = { Text("Ticket Sales") },
                                modifier = Modifier.testTag("sort_rev_ticket")
                            )
                        }
                        item {
                            FilterChip(
                                selected = revenueSortMode == 2,
                                onClick = { revenueSortMode = 2 },
                                label = { Text("Merchandise") },
                                modifier = Modifier.testTag("sort_rev_merch")
                            )
                        }
                        item {
                            FilterChip(
                                selected = revenueSortMode == 3,
                                onClick = { revenueSortMode = 3 },
                                label = { Text("Broadcast Deals") },
                                modifier = Modifier.testTag("sort_rev_broadcast")
                            )
                        }
                        item {
                            FilterChip(
                                selected = revenueSortMode == 4,
                                onClick = { revenueSortMode = 4 },
                                label = { Text("Profit Margin") },
                                modifier = Modifier.testTag("sort_rev_margin")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 4. Franchise Financial Overview Cards List
            items(filteredFranchiseStatements, key = { it.franchiseId }) { statement ->
                val franchise = uiState.franchises.find { it.id == statement.franchiseId }
                if (franchise != null) {
                    FranchiseRevenueCard(
                        franchise = franchise,
                        statement = statement,
                        onAdjustTicketPrice = { editingTicketPriceFranchise = franchise },
                        onViewStatement = { viewingStatementFranchise = statement }
                    )
                }
            }
        } else if (selectedSubTab == 1) {
            // ==========================================
            // === 1. LEAGUE TREASURY & ANALYTICS TAB ===
            // ==========================================

            // Time Horizon Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIME HORIZON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = timeFilterIndex == 0,
                            onClick = { timeFilterIndex = 0 },
                            label = { Text("All Season (${snapshots.size} Wks)") },
                            modifier = Modifier.testTag("filter_all_weeks")
                        )
                        FilterChip(
                            selected = timeFilterIndex == 1,
                            onClick = { timeFilterIndex = 1 },
                            label = { Text("Recent 6 Wks") },
                            modifier = Modifier.testTag("filter_recent_weeks")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Financial KPI Cards Row
            item {
                val latest = snapshots.lastOrNull()
                val netMarginPct = if (latest != null && latest.totalRevenue > 0) {
                    ((latest.netCashFlow / latest.totalRevenue) * 100).toInt()
                } else 32

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExecutiveMetricCard(
                            title = "Operating Margin",
                            value = "$netMarginPct%",
                            icon = Icons.Default.PieChart,
                            accentColor = if (netMarginPct >= 20) EmeraldAccent else GoldPrimary,
                            subtitle = "EBITDA Efficiency",
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_operating_margin"
                        )
                        ExecutiveMetricCard(
                            title = "Weekly Cashflow",
                            value = "${if ((latest?.netCashFlow ?: 2.4f) >= 0) "+" else ""}\$${String.format("%.1f", latest?.netCashFlow ?: 2.4f)}M",
                            icon = Icons.Default.Paid,
                            accentColor = EmeraldAccent,
                            subtitle = "Net Weekly Inflow",
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_weekly_cashflow"
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExecutiveMetricCard(
                            title = "Cash Reserves",
                            value = "\$${uiState.cashReservesMillions}M",
                            icon = Icons.Default.AccountBalance,
                            accentColor = CyanAccent,
                            subtitle = "Liquid Treasury",
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_cash_reserves"
                        )
                        ExecutiveMetricCard(
                            title = "Enterprise Value",
                            value = "\$${uiState.leagueValuationMillions}M",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = GoldPrimary,
                            subtitle = "Total Market Cap",
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_enterprise_valuation"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 1. RECHARTS AREA CHART: Cash Reserves Trajectory
            item {
                RechartsAreaChart(
                    data = filteredSnapshots,
                    primaryColor = EmeraldAccent,
                    secondaryColor = GoldPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. RECHARTS BAR CHART: Revenue vs Operating Expenses
            item {
                RechartsBarChart(
                    data = filteredSnapshots,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. RECHARTS DONUT CHARTS: Revenue Streams & Operating Expenses
            item {
                RechartsDonutChart(
                    title = "League Revenue Streams Breakdown",
                    slices = revenueSlices,
                    centerValue = "\$${String.format("%.0f", revenueSlices.sumOf { it.value.toDouble() })}M",
                    centerLabel = "ANNUAL REV",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                RechartsDonutChart(
                    title = "Operating Expense Allocation",
                    slices = expenseSlices,
                    centerValue = "\$${String.format("%.0f", expenseSlices.sumOf { it.value.toDouble() })}M",
                    centerLabel = "TOTAL EXP",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Executive P&L Financial Statement Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("financial_ledger_card")
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EXECUTIVE INCOME STATEMENT (ANNUALIZED)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Inflows
                        LedgerRow(label = "Broadcast Rights Central Cut", amount = "+\$${String.format("%.1f", broadcastRevAnnual)}M", color = EmeraldAccent)
                        LedgerRow(label = "Local Franchise Sponsorship Tax (15%)", amount = "+\$${String.format("%.1f", sponsorRevAnnual)}M", color = EmeraldAccent)
                        LedgerRow(label = "Franchise Expansion Buy-in Fees", amount = "+\$${String.format("%.1f", expansionRevAnnual)}M", color = EmeraldAccent)
                        LedgerRow(label = "Salary Cap Luxury Tax Penalties", amount = "+\$${String.format("%.1f", luxuryTaxAnnual)}M", color = EmeraldAccent)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Outflows
                        LedgerRow(label = "Central Operations & Executive Staff", amount = "-\$${String.format("%.1f", opsExpense)}M", color = CrimsonAlert)
                        LedgerRow(label = "Marketing, Media & Global Blitz", amount = "-\$${String.format("%.1f", marketingExpense)}M", color = CrimsonAlert)
                        LedgerRow(label = "Players Union Health & Welfare Fund", amount = "-\$${String.format("%.1f", welfareExpense)}M", color = CrimsonAlert)
                        LedgerRow(label = "Franchise Talent Grants & Subsidies", amount = "-\$${String.format("%.1f", recruitmentExpense)}M", color = CrimsonAlert)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        // Net
                        val netTotal = (broadcastRevAnnual + sponsorRevAnnual + expansionRevAnnual + luxuryTaxAnnual) - (opsExpense + marketingExpense + welfareExpense + recruitmentExpense)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET ANNUAL SURPLUS / DEFICIT",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (netTotal >= 0) "+" else ""}\$${String.format("%.1f", netTotal)}M",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (netTotal >= 0) EmeraldAccent else CrimsonAlert
                            )
                        }
                    }
                }
            }
        } else if (selectedSubTab == 2) {
            // ==========================================
            // === 2. BROADCAST & COMMERCIAL DEALS TAB ===
            // ==========================================

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(2.dp, GoldPrimary, RoundedCornerShape(16.dp))
                        .clickable { viewModel.openBroadcastNegotiations(true) }
                        .testTag("btn_open_war_room_banner"),
                    colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.14f)),
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
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val activeBidCount = uiState.broadcastBiddingSession.offers.count { it.bidStatus != com.example.model.BroadcastBidStatus.WITHDRAWN }
                            Text(
                                text = "MEDIA RIGHTS BIDDING DESK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Network Auction & Valuation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$activeBidCount media networks bidding (Round ${uiState.broadcastBiddingSession.biddingRound}) — evaluate multi-year payouts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.openBroadcastNegotiations(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_enter_war_room")
                        ) {
                            Text("View Bids", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                val activeDeal = uiState.activeBroadcast
                val remainingTotalValue = activeDeal.annualPayoutMillions * activeDeal.yearsRemaining
                val topBid = uiState.broadcastBiddingSession.offers
                    .filter { it.bidStatus != BroadcastBidStatus.WITHDRAWN }
                    .maxByOrNull { it.impact.cumulativeGrossValueMillions }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.5.dp, CyanAccent.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .testTag("card_active_broadcast_spotlight"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ACTIVE LEAGUE BROADCAST DEAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = activeDeal.partnerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "\$${activeDeal.annualPayoutMillions}M/yr • ${activeDeal.yearsRemaining} yrs remaining (\$$remainingTotalValue M value)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${activeDeal.reachMultiplier}x Reach",
                                    color = EmeraldAccent,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (topBid != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Top Market Bid: ${topBid.bidder.name} (\$${topBid.annualPayoutMillions}M/yr • \$${topBid.impact.cumulativeGrossValueMillions}M total)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldPrimary
                            )
                        }
                    }
                }
            }

            // Interactive Corporate Sponsorship Negotiation Minigame Banner
            item {
                val activeSpon = uiState.activeSponsorship
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { viewModel.openCorporateSponsorNegotiations(true) }
                        .testTag("banner_corporate_sponsorship_room")
                        .border(1.5.dp, EmeraldAccent, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CORPORATE SPONSORSHIP BOARDROOM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Sponsor Partnership Negotiation Minigame",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Negotiate high-value partnerships across Fintech, Sportsbooks, Clean Energy & Luxury brands with reputation impacts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { viewModel.openCorporateSponsorNegotiations(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_enter_sponsor_boardroom")
                            ) {
                                Text("Negotiate", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current Active Partnership Badge
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Active: ${activeSpon.corporationName} (${activeSpon.tier.title})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${activeSpon.sector.title} • \$${activeSpon.annualPayoutMillions}M/yr • ${activeSpon.yearsRemaining} yrs left",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    color = if (activeSpon.reputationImpact >= 0) EmeraldAccent.copy(alpha = 0.15f) else CrimsonAlert.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Rep: ${if (activeSpon.reputationImpact >= 0) "+${activeSpon.reputationImpact}" else "${activeSpon.reputationImpact}"}",
                                        color = if (activeSpon.reputationImpact >= 0) EmeraldAccent else CrimsonAlert,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Broadcast Rights Market Overview")
            }

            items(broadcastDeals) { deal ->
                val isCurrent = uiState.activeBroadcast.partnerName == deal.partnerName
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("broadcast_card_${deal.id}")
                        .border(
                            1.5.dp,
                            if (isCurrent) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) GoldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deal.partnerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${deal.platform.label} • ${deal.platform.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isCurrent) {
                                Surface(
                                    color = GoldPrimary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE CONTRACT",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ANNUAL RIGHTS FEE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\$${deal.annualPayoutMillions}M / year", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = EmeraldAccent)
                            }

                            if (!isCurrent) {
                                Button(
                                    onClick = { viewModel.negotiateBroadcast(deal) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .testTag("btn_sign_deal_${deal.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Text("Sign Contract", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Fan Engagement & Marketing Blitz", modifier = Modifier.padding(top = 16.dp))
            }

            items(uiState.campaigns) { campaign ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("campaign_card_${campaign.id}")
                        .border(
                            1.dp,
                            if (campaign.isActive) CyanAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = campaign.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = campaign.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (campaign.isActive) {
                                Surface(
                                    color = CyanAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${campaign.remainingWeeks} WEEKS LEFT",
                                        color = CyanAccent,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "COST: \$${campaign.costMillions}M • +${campaign.fanGrowthPct}% Fanbase • +3 Loyalty Now & +1–2/wk",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = "Target: ${campaign.targetSegment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!campaign.isActive) {
                                Button(
                                    onClick = { viewModel.launchCampaign(campaign.id) },
                                    enabled = uiState.cashReservesMillions >= campaign.costMillions,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .testTag("btn_launch_${campaign.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Text("Launch (\$${campaign.costMillions}M)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // === 3. TEAM SALARY CAPS & TAX TAB ========
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .testTag("card_salary_cap_controls"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LEAGUE SALARY CAP & LUXURY TAX",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Cap Ceiling: \$${salaryCapMillions}M",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${capStrictness.displayName} • $overCapTeamsCount/${uiState.franchises.size} over cap",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("TAX POOL", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = EmeraldAccent)
                                    Text("\$${estimatedLuxuryTaxPool}M", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = EmeraldAccent)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateSalaryCapAmount(salaryCapMillions - 5L) },
                                enabled = salaryCapMillions > 50L,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_cap_decrease")
                            ) {
                                Text("-\$5M", fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = salaryCapMillions.toFloat().coerceIn(50f, 220f),
                                onValueChange = { viewModel.updateSalaryCapAmount(it.roundToLong()) },
                                valueRange = 50f..220f,
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("slider_salary_cap_ceiling")
                            )
                            OutlinedButton(
                                onClick = { viewModel.updateSalaryCapAmount(salaryCapMillions + 5L) },
                                enabled = salaryCapMillions < 220L,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_cap_increase")
                            ) {
                                Text("+\$5M", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "ENFORCEMENT STRICTNESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(SalaryCapStrictness.values()) { mode ->
                                val isSel = capStrictness == mode
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateSalaryCapStrictness(mode) },
                                    label = { Text(mode.shortLabel) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("chip_cap_mode_${mode.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val (_, message) = viewModel.enforceSalaryCapComplianceAndCollectTax()
                                capActionFeedback = message
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("btn_collect_luxury_tax")
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (estimatedLuxuryTaxPool > 0L) {
                                    "Enforce Cap & Collect \$${estimatedLuxuryTaxPool}M Luxury Tax"
                                } else {
                                    "Run League Cap Compliance Audit"
                                },
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        }

                        if (capActionFeedback != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = CyanAccent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("banner_cap_tax_feedback")
                            ) {
                                Text(
                                    text = capActionFeedback!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SectionHeader(title = "Team Payroll & Cap Space Ledger")
            }

            items(uiState.franchises, key = { "cap_${it.id}" }) { team ->
                TeamSalaryCapCard(
                    franchise = team,
                    salaryCapMillions = salaryCapMillions,
                    strictness = capStrictness,
                    onAdjustPayroll = { delta ->
                        val (_, msg) = viewModel.adjustFranchisePayrollDirective(team.id, delta)
                        capActionFeedback = msg
                    }
                )
            }
        }
    }

    // Modal: Adjust Ticket Price Dialog
    if (editingTicketPriceFranchise != null) {
        val team = editingTicketPriceFranchise!!
        var tempPrice by remember { mutableStateOf(team.ticketPrice.toFloat()) }
        val pricePreview = remember(tempPrice, team) {
            FanLoyaltyEngine.previewTicketPriceImpact(team, tempPrice.toInt())
        }
        val projectedGate = pricePreview.projectedGateMillions

        Dialog(onDismissRequest = { editingTicketPriceFranchise = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp))
                    .testTag("dialog_adjust_ticket_price"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TICKET PRICING STRATEGY",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = team.teamFullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(team.colorHex))
                                .border(1.5.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = team.nickname.take(2).uppercase(),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Set average admission price per home game. Higher prices increase per-game yield but may depress arena attendance if fan morale falls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TICKET PRICE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "\$${tempPrice.toInt()} / seat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                    }

                    Slider(
                        value = tempPrice,
                        onValueChange = { tempPrice = it },
                        valueRange = 25f..200f,
                        steps = 34,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldPrimary,
                            activeTrackColor = GoldPrimary
                        ),
                        modifier = Modifier.testTag("slider_ticket_price")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("\$25 Min", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("\$200 Premium", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Projected Gate Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("PROJECTED ANNUAL GATE REVENUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "\$${String.format("%.1f", projectedGate)}M / season",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "Based on ${team.stadiumCapacity} capacity • ${(pricePreview.attendanceRate * 100).toInt()}% projected attendance (${team.fanLoyalty}/100 loyalty).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val verdictColor = when {
                                pricePreview.weeklyTrendDelta > 0 -> EmeraldAccent
                                pricePreview.weeklyTrendDelta < -1 -> CrimsonAlert
                                pricePreview.weeklyTrendDelta < 0 -> AmberWarning
                                else -> GoldPrimary
                            }
                            Text(
                                text = "Loyalty Verdict (Fair ~\$${pricePreview.fairPrice}): ${pricePreview.verdictLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Immediate Loyalty Shift: ${if (pricePreview.immediateDelta >= 0) "+${pricePreview.immediateDelta}" else "${pricePreview.immediateDelta}"} • Weekly Trend: ${if (pricePreview.weeklyTrendDelta >= 0) "+${pricePreview.weeklyTrendDelta}" else "${pricePreview.weeklyTrendDelta}"}/wk",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingTicketPriceFranchise = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateFranchiseTicketPrice(team.id, tempPrice.toInt())
                                editingTicketPriceFranchise = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.testTag("btn_save_ticket_price")
                        ) {
                            Text("Apply (\$${tempPrice.toInt()})", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal: Comprehensive Franchise Financial Statement Dossier
    if (viewingStatementFranchise != null) {
        val stmt = viewingStatementFranchise!!
        val franchise = uiState.franchises.find { it.id == stmt.franchiseId }

        Dialog(onDismissRequest = { viewingStatementFranchise = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, CyanAccent, RoundedCornerShape(20.dp))
                    .testTag("dialog_franchise_statement_dossier"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AUDITED FINANCIAL STATEMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = stmt.franchiseName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (franchise != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(franchise.colorHex))
                                    .border(1.5.dp, CyanAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = franchise.nickname.take(2).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("REVENUE STREAMS (INFLOWS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = EmeraldAccent)
                    Spacer(modifier = Modifier.height(6.dp))

                    LedgerRow(label = "Ticket Sales & Gate Receipts", amount = "+\$${stmt.ticketRevenueMillions}M", color = EmeraldAccent)
                    if (stmt.rivalryBonusMillions > 0.0) {
                        LedgerRow(label = "  ↳ Rivalry Gate Multiplier Surge", amount = "+\$${stmt.rivalryBonusMillions}M", color = GoldPrimary)
                    }
                    LedgerRow(label = "Merchandise & Apparel Licensing", amount = "+\$${stmt.merchandiseRevenueMillions}M", color = EmeraldAccent)
                    LedgerRow(label = "Broadcast Rights (Central + RSN)", amount = "+\$${stmt.broadcastRevenueMillions}M", color = EmeraldAccent)
                    LedgerRow(label = "Local Arena Sponsorships & Luxury Boxes", amount = "+\$${stmt.localSponsorRevenueMillions}M", color = EmeraldAccent)
                    if (stmt.luxurySuiteRevenueMillions > 0.0) {
                        LedgerRow(label = "  ↳ VIP Luxury Suites Premium", amount = "+\$${stmt.luxurySuiteRevenueMillions}M", color = GoldPrimary)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Text("OPERATING EXPENSES (OUTFLOWS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = CrimsonAlert)
                    Spacer(modifier = Modifier.height(6.dp))

                    LedgerRow(label = "Active Player Payroll", amount = "-\$${stmt.payrollExpenseMillions}M", color = CrimsonAlert)
                    LedgerRow(label = "Stadium Operations, Travel & Gameday", amount = "-\$${stmt.operationsExpenseMillions}M", color = CrimsonAlert)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NET OPERATING PROFIT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text(
                            text = "${if (stmt.netOperatingIncomeMillions >= 0) "+" else ""}\$${stmt.netOperatingIncomeMillions}M (${stmt.profitMarginPct}%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (stmt.netOperatingIncomeMillions >= 0) EmeraldAccent else CrimsonAlert
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("COMMERCIAL & VALUATION HIGHLIGHTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            Text(
                                text = "• Franchise Valuation: \$${stmt.franchiseValuationMillions}M • %,d Arena Seats".format(franchise?.stadiumCapacity ?: 22000),
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Fan Loyalty: ${stmt.fanLoyalty}/100 (${stmt.loyaltyTierTitle}) -> ${if (stmt.loyaltyRevenueImpactMillions >= 0) "+" else ""}\$${stmt.loyaltyRevenueImpactMillions}M rev effect",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (stmt.loyaltyRevenueImpactMillions >= 0) EmeraldAccent else CrimsonAlert,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("• Multipliers: ${stmt.loyaltyMerchMultiplier}x Merch • ${stmt.loyaltySponsorMultiplier}x Local Sponsors", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Top Merch: ${stmt.topSellingProduct}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Media Partner: ${stmt.rsnPartner}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewingStatementFranchise = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Dossier", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FranchiseRevenueCard(
    franchise: Franchise,
    statement: FranchiseFinancialStatement,
    onAdjustTicketPrice: () -> Unit,
    onViewStatement: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("franchise_revenue_card_${franchise.id}")
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Team Identity and Net Income Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(franchise.colorHex))
                            .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = franchise.nickname.take(2).uppercase(),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Text(
                            text = franchise.teamFullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${franchise.conference.shortCode} • %,d Seats • Val \$${statement.franchiseValuationMillions}M".format(franchise.stadiumCapacity),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (statement.netOperatingIncomeMillions >= 0) EmeraldAccent.copy(alpha = 0.15f) else CrimsonAlert.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (statement.netOperatingIncomeMillions >= 0) EmeraldAccent else CrimsonAlert
                    )
                ) {
                    Text(
                        text = "${if (statement.netOperatingIncomeMillions >= 0) "+" else ""}\$${statement.netOperatingIncomeMillions}M (${statement.profitMarginPct}%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (statement.netOperatingIncomeMillions >= 0) EmeraldAccent else CrimsonAlert,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fan Loyalty Revenue Impact Strip
            val loyaltyAccent = Color(franchise.loyaltyTier.badgeColorHex)
            Surface(
                color = loyaltyAccent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, loyaltyAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fan Loyalty: ${statement.fanLoyalty}/100 (${statement.loyaltyTierTitle})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = loyaltyAccent
                    )
                    Text(
                        text = "${if (statement.loyaltyRevenueImpactMillions >= 0) "+" else ""}\$${statement.loyaltyRevenueImpactMillions}M rev • ${statement.loyaltyMerchMultiplier}x merch",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (statement.loyaltyRevenueImpactMillions >= 0) EmeraldAccent else CrimsonAlert
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Revenue Stream Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1: Ticket Sales
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
                            Text("TICKET SALES", style = MaterialTheme.typography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                        Text(
                            text = "\$${statement.ticketRevenueMillions}M",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                        Text(
                            text = "${franchise.stadiumCapacity / 1000}k cap @ \$${franchise.ticketPrice}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                        Text(
                            text = "${statement.attendancePct}% attend.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 8.sp
                        )
                        if (statement.rivalryBonusMillions > 0.0) {
                            Text(
                                text = "+\$${statement.rivalryBonusMillions}M rivalry",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                // Column 2: Merchandise
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                            Text("MERCHANDISE", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                        Text(
                            text = "\$${statement.merchandiseRevenueMillions}M",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent
                        )
                        Text(
                            text = "${franchise.fanBaseMillions}M fans",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                        Text(
                            text = "${franchise.fanMorale}/100 morale",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 8.sp
                        )
                    }
                }

                // Column 3: Broadcast Deals
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                            Text("BROADCAST", style = MaterialTheme.typography.labelSmall, color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                        Text(
                            text = "\$${statement.broadcastRevenueMillions}M",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent
                        )
                        Text(
                            text = "Central + RSN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                        Text(
                            text = "Regional TV Deal",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Proportional Segmented Revenue Bar for Franchise
            val tot = statement.totalRevenueMillions.coerceAtLeast(1.0)
            val tPct = (statement.ticketRevenueMillions / tot).toFloat().coerceIn(0.05f, 0.90f)
            val mPct = (statement.merchandiseRevenueMillions / tot).toFloat().coerceIn(0.05f, 0.90f)
            val bPct = (statement.broadcastRevenueMillions / tot).toFloat().coerceIn(0.05f, 0.90f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                Box(modifier = Modifier.weight(tPct).fillMaxHeight().background(GoldPrimary))
                Box(modifier = Modifier.weight(mPct).fillMaxHeight().background(EmeraldAccent))
                Box(modifier = Modifier.weight(bPct).fillMaxHeight().background(CyanAccent))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAdjustTicketPrice,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_adjust_ticket_${franchise.id}")
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ticket Price (\$${franchise.ticketPrice})", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                }

                Button(
                    onClick = onViewStatement,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_view_statement_${franchise.id}")
                        .minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Itemized P&L", style = MaterialTheme.typography.labelSmall, color = CyanAccent)
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(
    label: String,
    amount: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun FinancialOverviewPillarTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable { onClick() }
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TeamSalaryCapCard(
    franchise: Franchise,
    salaryCapMillions: Long,
    strictness: SalaryCapStrictness,
    onAdjustPayroll: (Long) -> Unit
) {
    val diff = franchise.annualPayrollMillions - salaryCapMillions
    val isOver = diff > 0L
    val utilization = (franchise.annualPayrollMillions.toFloat() / salaryCapMillions.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1.3f)
    val barColor = when {
        isOver -> CrimsonAlert
        utilization >= 0.88f -> GoldPrimary
        else -> EmeraldAccent
    }
    val estTax = if (isOver && strictness != SalaryCapStrictness.UNRESTRICTED_MARKET) {
        val rate = if (strictness == SalaryCapStrictness.STRICT_HARD_CAP) 1.0 else strictness.penaltyRatePct / 100.0
        kotlin.math.round(diff * rate).toLong().coerceAtLeast(1L)
    } else 0L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(1.dp, barColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .testTag("team_salary_cap_card_${franchise.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(franchise.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = franchise.nickname.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Column {
                        Text(
                            text = franchise.teamFullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Payroll: \$${franchise.annualPayrollMillions}M / \$${salaryCapMillions}M Cap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = barColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isOver) "OVER +\$${diff}M" else "\$${-diff}M Space",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { utilization.coerceIn(0f, 1f) },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            if (estTax > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Luxury Tax Liability: \$${estTax}M (${strictness.shortLabel})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonAlert
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAdjustPayroll(-5L) },
                    enabled = franchise.annualPayrollMillions > 35L,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_trim_payroll_${franchise.id}")
                ) {
                    Text("-\$5M Relief", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAdjustPayroll(5L) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_expand_payroll_${franchise.id}")
                ) {
                    Text("+\$5M Spend", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
