package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import com.example.model.ExpansionArenaTier
import com.example.model.ExpansionRequestEngine
import com.example.model.FranchiseIdentityEngine
import com.example.model.MAX_OPEN_EXPANSION_CANDIDATES
import com.example.model.MarketCity
import com.example.model.MarketTier
import com.example.model.MarketViabilityGrade
import com.example.model.OwnerArchetype
import com.example.model.OwnerGovernanceEngine
import com.example.model.OwnershipType
import com.example.data.InitialLeagueData
import com.example.ui.components.FranchiseIdentityCustomizer
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCityForExpansion by remember { mutableStateOf<MarketCity?>(null) }
    var selectedCityForDossier by remember { mutableStateOf<MarketCity?>(null) }
    var comparatorCity by remember { mutableStateOf<MarketCity?>(null) }
    var showComparatorMode by remember { mutableStateOf(false) }
    var showScoutingDialog by remember { mutableStateOf(false) }
    var candidateToWithdraw by remember { mutableStateOf<MarketCity?>(null) }

    var filterCategory by remember { mutableStateOf(0) } // 0: All, 1: Open Bids Only, 2: International, 3: Sunbelt, 4: National (USA), 5: Prime (A+), 6: Emerging (B/C)
    var sortCriteria by remember { mutableStateOf(0) } // 0: Viability, 1: Market Size (Pop), 2: Fan Interest, 3: Growth %, 4: Expansion Fee

    val allCities = uiState.cities

    // Filter cities
    val filteredCities = remember(allCities, filterCategory, sortCriteria) {
        val filtered = when (filterCategory) {
            1 -> allCities.filter { !it.hasFranchise }
            2 -> allCities.filter { it.isInternational }
            3 -> allCities.filter { it.annualGrowthRatePct >= 3.0 }
            4 -> allCities.filter { !it.isInternational }
            5 -> allCities.filter { it.overallViabilityScore >= 80 }
            6 -> allCities.filter { it.overallViabilityScore < 60 }
            else -> allCities
        }

        when (sortCriteria) {
            1 -> filtered.sortedByDescending { it.populationMillions }
            2 -> filtered.sortedByDescending { it.sportsAppetite }
            3 -> filtered.sortedByDescending { it.annualGrowthRatePct }
            4 -> filtered.sortedByDescending { it.expansionFeeMillions }
            else -> filtered.sortedByDescending { it.overallViabilityScore }
        }
    }

    val openBidsCount = allCities.count { !it.hasFranchise }
    val highestFanInterest = allCities.maxByOrNull { it.sportsAppetite }
    val fastestGrowth = allCities.maxByOrNull { it.annualGrowthRatePct }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("markets_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        // Executive Header
        item {
            Text(
                text = "COMMISSIONER EXPANSION COMMITTEE",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Market Size & Fan Interest Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Evaluate candidate metropolitan markets using population data, sports appetite, and digital fan interest to maximize expansion revenue and regional footprint.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Expansion Draft Hub Action Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp))
                    .testTag("expansion_draft_hub_banner"),
                colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.12f)),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "FRANCHISE EXPANSION COMMITTEE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                        }
                        Text(
                            text = "Expansion Draft & League Growth",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select candidate cities, ratify ownership, and host a live round-by-round expansion draft.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val firstOpenCity = allCities.firstOrNull { !it.hasFranchise }
                    Button(
                        onClick = {
                            if (firstOpenCity != null) {
                                selectedCityForExpansion = firstOpenCity
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_open_expansion_draft_hub")
                    ) {
                        Text("Host Draft", color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Expansion Requests Management View Trigger Card
            val pendingReqCount = uiState.pendingExpansionRequests.size
            val totalReqReach = Math.round(uiState.pendingExpansionRequests.sumOf { it.impact.projectedMarketReachGainMillions } * 10.0) / 10.0
            val totalReqRev = uiState.pendingExpansionRequests.sumOf { it.impact.projectedAnnualRevenueGainMillions }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { viewModel.openExpansionRequestsManagement(true) }
                    .border(1.5.dp, CyanAccent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .testTag("card_open_expansion_requests"),
                colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.1f)),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Text(
                                text = "EXPANSION REQUESTS QUEUE ($pendingReqCount PENDING)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent
                            )
                        }
                        Text(
                            text = "Approve & Review Market Impact",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (pendingReqCount > 0) {
                                "Potential upside: +${totalReqReach}M fans reach • +\$${totalReqRev}M/yr revenue"
                            } else {
                                "Inspect reviewed bids or scout new franchise expansion requests."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.openExpansionRequestsManagement(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_open_expansion_requests")
                    ) {
                        Text("Manage", color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Open Expansion Candidate Cap Status Gauge (Max: 10 Candidates)
        item {
            val openCandidates = allCities.filter { !it.hasFranchise }
            val openCount = openCandidates.size
            val isFull = openCount >= MAX_OPEN_EXPANSION_CANDIDATES
            val capacityFraction = (openCount.toFloat() / MAX_OPEN_EXPANSION_CANDIDATES).coerceIn(0f, 1f)

            val capColor = when {
                openCount >= 10 -> CrimsonAccent
                openCount >= 8 -> AmberWarning
                else -> EmeraldAccent
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.5.dp, capColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .testTag("card_candidate_cap_gauge"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = capColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "OPEN EXPANSION CANDIDATE POOL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = capColor,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            color = capColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$openCount / $MAX_OPEN_EXPANSION_CANDIDATES ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = capColor,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("badge_open_candidates_count")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { capacityFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("progress_candidate_cap"),
                        color = capColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isFull) {
                            "Pool is at maximum legal capacity (10 candidates). Withdraw or charter a candidate market before admitting new national or international bids."
                        } else {
                            "League constitution strictly limits open candidate bids to 10 simultaneously. ${MAX_OPEN_EXPANSION_CANDIDATES - openCount} candidate slot(s) currently open for national or international applications."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showScoutingDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_scout_new_markets")
                                .minimumInteractiveComponentSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scout Markets",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.scoutAndRefreshCandidates() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_auto_scout_bids")
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(GoldPrimary))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFull) "Pool Full (10/10)" else "Auto-Inflow Bids",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Summary Metric Gauges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("OPEN CITIES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$openBidsCount Markets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = GoldPrimary)
                        Text("Available for Bids", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("TOP FAN PASSION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${highestFanInterest?.name ?: "Mexico City"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = EmeraldAccent, maxLines = 1)
                        Text("${highestFanInterest?.sportsAppetite ?: 98}/100 Appetite", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent, fontSize = 9.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("TOP GROWTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${fastestGrowth?.name ?: "Austin"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CyanAccent, maxLines = 1)
                        Text("+${fastestGrowth?.annualGrowthRatePct ?: 4.8}% annual", style = MaterialTheme.typography.labelSmall, color = CyanAccent, fontSize = 9.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Market Viability Matrix Visual Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("viability_matrix_banner"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MARKET SIZE VS. FAN INTEREST MATRIX",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = CyanAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "2x2 STRATEGIC QUADRANT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quadrant 1
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Mega Megacities", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                Text("High Pop + High Fan Interest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("London, Tokyo, Mexico City", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                            }
                        }
                        // Quadrant 2
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Booming Sunbelt", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                                Text("Rapid Growth + Fan Passion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Austin, Vegas, Nashville", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Filter Chips Row
        item {
            Text("FILTER BY CATEGORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = filterCategory == 0,
                        onClick = { filterCategory = 0 },
                        label = { Text("All Markets (${allCities.size})") },
                        modifier = Modifier.testTag("filter_all_markets")
                    )
                }
                item {
                    FilterChip(
                        selected = filterCategory == 1,
                        onClick = { filterCategory = 1 },
                        label = { Text("Open Candidates Only ($openBidsCount)") },
                        modifier = Modifier.testTag("filter_open_candidates")
                    )
                }
                item {
                    FilterChip(
                        selected = filterCategory == 2,
                        onClick = { filterCategory = 2 },
                        label = { Text("International Gateways") },
                        modifier = Modifier.testTag("filter_international_gateways")
                    )
                }
                item {
                    FilterChip(
                        selected = filterCategory == 3,
                        onClick = { filterCategory = 3 },
                        label = { Text("High-Growth Sunbelt (>3%)") },
                        modifier = Modifier.testTag("filter_high_growth")
                    )
                }
                item {
                    FilterChip(
                        selected = filterCategory == 4,
                        onClick = { filterCategory = 4 },
                        label = { Text("National (USA)") },
                        modifier = Modifier.testTag("filter_national_markets")
                    )
                }
                item {
                    FilterChip(
                        selected = filterCategory == 5,
                        onClick = { filterCategory = 5 },
                        label = { Text("Prime Viability (A+)") },
                        modifier = Modifier.testTag("filter_prime_viability")
                    )
                }
                item {
                    FilterChip(
                        selected = filterCategory == 6,
                        onClick = { filterCategory = 6 },
                        label = { Text("Emerging & Speculative") },
                        modifier = Modifier.testTag("filter_emerging_markets")
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Sort Selector Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SORT BY METRIC:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = { sortCriteria = 0 },
                        label = { Text("Viability", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sortCriteria == 0) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { sortCriteria = 1 },
                        label = { Text("Market Size", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sortCriteria == 1) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { sortCriteria = 2 },
                        label = { Text("Fan Interest", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sortCriteria == 2) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { sortCriteria = 3 },
                        label = { Text("Growth", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sortCriteria == 3) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Candidate City Evaluation Cards
        items(filteredCities) { city ->
            val hasActiveTeam = city.hasFranchise
            val existingTeam = uiState.franchises.find { it.cityId == city.id }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("city_evaluation_card_${city.id}")
                    .border(
                        1.dp,
                        if (city.isInternational) CyanAccent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with City, Country, Tier, and Viability Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (city.isInternational) {
                                    Surface(
                                        color = CyanAccent.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "INTL GATEWAY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "NATIONAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${city.country} • ${city.region} • ${city.tier.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Viability Grade Badge
                        Surface(
                            color = Color(city.viabilityGrade.badgeColorHex).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(city.viabilityGrade.badgeColorHex))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${city.overallViabilityScore}/100",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(city.viabilityGrade.badgeColorHex)
                                )
                                Text(
                                    text = city.viabilityGrade.shortBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(city.viabilityGrade.badgeColorHex),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Facilities & Investor Group Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Stadium, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                                Text(
                                    text = city.arenaReadinessStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = city.investorGroupProfile,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fan Interest & Sports Appetite Progress Bar
                    ProgressBarWithLabel(
                        label = "Fan Interest & Passion Appetite",
                        progress = city.sportsAppetite / 100f,
                        displayValue = "${city.sportsAppetite}/100",
                        color = if (city.sportsAppetite >= 90) EmeraldAccent else GoldPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Market Size & Demographics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MARKET SIZE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${city.populationMillions}M Pop", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("ANNUAL GROWTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+${city.annualGrowthRatePct}% / yr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                        }
                        Column {
                            Text("MEDIAN INCOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("\$${city.medianIncomeK}K", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("LOCAL SPONSORS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("\$${city.localSponsorDepthMillions}M/yr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val cityCba = remember(
                        city,
                        uiState.franchises,
                        uiState.activeBroadcast,
                        uiState.activeSponsorship
                    ) {
                        ExpansionRequestEngine.analyzeMarketCostBenefit(
                            city = city,
                            franchises = uiState.franchises,
                            activeBroadcast = uiState.activeBroadcast,
                            activeSponsorship = uiState.activeSponsorship
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_cba_strip_${city.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "5Y ROI +${cityCba.fiveYearRoiPct.toInt()}% • Grade ${cityCba.cbaGrade}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Text(
                                text = "+\$${cityCba.projectedNetAnnualBenefitMillions}M/yr Net",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                            if (city.hasBuiltExpansionArena) {
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Arena Built",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons (Evaluate Dossier & Award Franchise & Withdraw)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedCityForDossier = city },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_evaluate_${city.id}")
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dossier", style = MaterialTheme.typography.labelMedium)
                        }

                        if (!hasActiveTeam) {
                            Button(
                                onClick = { selectedCityForExpansion = city },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("btn_award_${city.id}")
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Award & Draft", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }

                            // Withdraw Candidate Button (frees up slot in 10-cap pool)
                            OutlinedButton(
                                onClick = { candidateToWithdraw = city },
                                modifier = Modifier
                                    .testTag("btn_withdraw_${city.id}")
                                    .minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAccent),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CrimsonAccent.copy(alpha = 0.6f)))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Withdraw ${city.name}", tint = CrimsonAccent, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "CHARTERED",
                                    color = EmeraldAccent,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Comprehensive City Evaluation Dossier Dialog
    if (selectedCityForDossier != null) {
        val city = allCities.find { it.id == selectedCityForDossier!!.id } ?: selectedCityForDossier!!
        val compCity = comparatorCity
        val cba = remember(city, uiState.franchises, uiState.activeBroadcast, uiState.activeSponsorship) {
            ExpansionRequestEngine.analyzeMarketCostBenefit(
                city = city,
                franchises = uiState.franchises,
                activeBroadcast = uiState.activeBroadcast,
                activeSponsorship = uiState.activeSponsorship
            )
        }
        var arenaBuildFeedback by remember(city.id) { mutableStateOf<Pair<Boolean, String>?>(null) }

        Dialog(onDismissRequest = {
            selectedCityForDossier = null
            comparatorCity = null
            showComparatorMode = false
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(8.dp)
                    .border(2.dp, CyanAccent, RoundedCornerShape(20.dp))
                    .testTag("city_dossier_modal"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EXPANSION INTELLIGENCE DOSSIER",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${city.name}, ${city.country}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Surface(
                            color = GoldPrimary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${city.overallViabilityScore}/100",
                                color = Color.Black,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Market Profile Overview
                    Text(
                        text = "Classification: ${city.marketQuadrantLabel} • ${city.tier.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Side-by-side or detailed metrics
                    ProgressBarWithLabel(
                        label = "Fan Interest & Passion Index",
                        progress = city.sportsAppetite / 100f,
                        displayValue = "${city.sportsAppetite}/100 Score",
                        color = EmeraldAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBarWithLabel(
                        label = "Digital Streaming & Mobile Viewers",
                        progress = city.digitalStreamingIndex / 100f,
                        displayValue = "${city.digitalStreamingIndex}/100 Rating",
                        color = CyanAccent
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Economic Projections Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("PROJECTED YEAR 1 ECONOMICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Arena: ${city.arenaName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${city.arenaCapacity} seats", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Local Sponsorship Depth", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\$${city.localSponsorDepthMillions}M/yr", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Expansion Buy-In Fee", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\$${city.expansionFeeMillions}M Treasury Inflow", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cost-Benefit Analysis Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .testTag("dossier_cost_benefit_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "COST-BENEFIT ANALYSIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary
                                )
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "GRADE ${cba.cbaGrade}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Upfront Arena Outlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (cba.hasConstructedArena) "Funded (\$0M left)" else "\$${cba.totalCapitalOutlayMillions}M",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cba.hasConstructedArena) EmeraldAccent else AmberWarning
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Net Annual Benefit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "+\$${cba.projectedNetAnnualBenefitMillions}M/yr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payback Window / 5-Yr ROI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${cba.paybackYears} yrs • +${cba.fiveYearRoiPct.toInt()}% ROI",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cba.recommendationNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!city.hasFranchise) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .testTag("card_build_expansion_arena")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BUILD EXPANSION ARENA",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = CyanAccent
                                    )
                                    Text(
                                        text = "Cash: \$${uiState.cashReservesMillions}M",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (city.hasBuiltExpansionArena) {
                                        "Commissioned: ${city.expansionArenaTierName.ifBlank { city.arenaName }} (${city.arenaCapacity} seats)"
                                    } else {
                                        "Pre-fund stadium construction to boost local sponsors, capacity, and expansion ROI."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ExpansionArenaTier.values().forEach { tier ->
                                    val isCurrentTier = city.hasBuiltExpansionArena && city.expansionArenaTierName == tier.displayName
                                    OutlinedButton(
                                        onClick = { arenaBuildFeedback = viewModel.buildExpansionArena(city.id, tier) },
                                        enabled = !city.hasBuiltExpansionArena && uiState.cashReservesMillions >= tier.buildCostMillions,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .minimumInteractiveComponentSize()
                                            .testTag("btn_build_arena_${tier.name}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isCurrentTier) "${tier.displayName} (Built)" else tier.displayName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrentTier) EmeraldAccent else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${tier.capacity} seats • +\$${tier.sponsorBoostMillions}M/yr sponsors",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "-\$${tier.buildCostMillions}M",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Black,
                                                color = GoldPrimary
                                            )
                                        }
                                    }
                                }
                                if (arenaBuildFeedback != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val (ok, msg) = arenaBuildFeedback!!
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ok) EmeraldAccent else CrimsonAlert
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedCityForDossier = null
                            comparatorCity = null
                        }) {
                            Text("Close")
                        }
                        if (!city.hasFranchise) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    selectedCityForExpansion = city
                                    selectedCityForDossier = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("Proceed to Award", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Award Expansion Franchise Dialog
    if (selectedCityForExpansion != null) {
        val city = selectedCityForExpansion!!
        var nicknameInput by remember(city.id) {
            mutableStateOf(
                FranchiseIdentityEngine.generateRandomRealisticNickname(
                    city = city,
                    existingFranchises = uiState.franchises,
                    sportType = uiState.sportType
                )
            )
        }
        var selectedOwnershipType by remember { mutableStateOf(OwnershipType.SOVEREIGN_SYNDICATE) }
        var selectedArchetype by remember(city.id) {
            mutableStateOf(
                OwnerGovernanceEngine.buildExpansionOwnershipGroup(
                    city = city,
                    ownershipType = OwnershipType.SOVEREIGN_SYNDICATE,
                    existingFranchises = uiState.franchises
                ).primaryOwner.archetype
            )
        }
        var customOwnerNameInput by remember(city.id) { mutableStateOf("") }
        val previewOwnerGroup = remember(city.id, selectedOwnershipType, selectedArchetype, customOwnerNameInput, uiState.franchises) {
            OwnerGovernanceEngine.buildExpansionOwnershipGroup(
                city = city,
                ownershipType = selectedOwnershipType,
                existingFranchises = uiState.franchises,
                preferredArchetype = selectedArchetype,
                customOwnerName = customOwnerNameInput.ifBlank { null }
            )
        }
        var selectedColor by remember(city.id) { mutableStateOf(FranchiseIdentityEngine.brandColorPalette[12].hex) } // Crimson Red
        var selectedSecondaryColor by remember(city.id) { mutableStateOf(FranchiseIdentityEngine.brandColorPalette[8].hex) } // Championship Gold
        var selectedDraftRounds by remember { mutableStateOf(6) }

        Dialog(onDismissRequest = { selectedCityForExpansion = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp))
                    .testTag("expansion_ratification_modal"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "RATIFY EXPANSION FRANCHISE",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "New Franchise in ${city.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "League treasury will collect \$${city.expansionFeeMillions}M immediately upon ratification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val (assignedConf, assignedDiv) = com.example.data.InitialLeagueData.getGeographicAlignment(city.name)
                    Surface(
                        color = Color(assignedConf.badgeColorHex).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(assignedConf.badgeColorHex).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(assignedConf.badgeColorHex), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Geographic Alignment: ${assignedConf.shortCode} • ${assignedDiv.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(assignedConf.badgeColorHex)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FranchiseIdentityCustomizer(
                        cityName = city.name,
                        nickname = nicknameInput,
                        onNicknameChange = { nicknameInput = it },
                        onRandomizeNickname = {
                            nicknameInput = viewModel.generateRandomFranchiseNickname(
                                targetCityId = city.id,
                                excludeNickname = nicknameInput
                            )
                        },
                        primaryColorHex = selectedColor,
                        secondaryColorHex = selectedSecondaryColor,
                        onColorsChanged = { primaryHex, secondaryHex ->
                            selectedColor = primaryHex
                            selectedSecondaryColor = secondaryHex
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("SELECT OWNERSHIP GROUP TYPE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    OwnershipType.values().forEach { ownType ->
                        val isSel = ownType == selectedOwnershipType
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedOwnershipType = ownType }
                                .border(
                                    1.dp,
                                    if (isSel) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(ownType.title, fontWeight = FontWeight.Bold, color = if (isSel) GoldPrimary else MaterialTheme.colorScheme.onSurface)
                                Text(ownType.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("OWNER GOVERNOR & VOTING ARCHETYPE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Appoints a Board of Governors seat who votes on CBA terms and league rule changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customOwnerNameInput,
                        onValueChange = { customOwnerNameInput = it },
                        label = { Text("Principal Owner Name (Optional)") },
                        placeholder = { Text(previewOwnerGroup.primaryOwner.fullName) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_expansion_owner_name")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(OwnerArchetype.values()) { arch ->
                            val isArchSel = arch == selectedArchetype
                            val archColor = Color(arch.badgeColorHex)
                            FilterChip(
                                selected = isArchSel,
                                onClick = { selectedArchetype = arch },
                                label = { Text(arch.shortBadge, fontWeight = if (isArchSel) FontWeight.Black else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = archColor.copy(alpha = 0.2f),
                                    selectedLabelColor = archColor
                                ),
                                modifier = Modifier.testTag("chip_expansion_archetype_${arch.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = Color(selectedArchetype.badgeColorHex).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedArchetype.badgeColorHex).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${previewOwnerGroup.primaryOwner.fullName} • ${selectedArchetype.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(selectedArchetype.badgeColorHex)
                            )
                            Text(
                                text = selectedArchetype.manifesto,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("EXPANSION DRAFT SIZE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Draft from unprotected existing franchise rosters and the live Free Agency Pool.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(6 to "6 Picks (Core)", 8 to "8 Picks (Full)", 10 to "10 Picks (Deep)").forEach { (rounds, label) ->
                            val isSelected = selectedDraftRounds == rounds
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clickable { selectedDraftRounds = rounds }
                                    .testTag("chip_expansion_rounds_$rounds"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val ratificationCba = remember(
                        city,
                        uiState.franchises,
                        uiState.activeBroadcast,
                        uiState.activeSponsorship
                    ) {
                        ExpansionRequestEngine.analyzeMarketCostBenefit(
                            city = city,
                            franchises = uiState.franchises,
                            activeBroadcast = uiState.activeBroadcast,
                            activeSponsorship = uiState.activeSponsorship
                        )
                    }
                    Surface(
                        color = GoldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CBA ${ratificationCba.cbaGrade} • 5Y ROI +${ratificationCba.fiveYearRoiPct.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Text(
                                text = "+\$${ratificationCba.projectedNetAnnualBenefitMillions}M/yr Net • ${ratificationCba.paybackYears}y Payback",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (nicknameInput.isNotBlank()) {
                                    viewModel.startExpansionDraft(
                                        targetCityId = city.id,
                                        nickname = nicknameInput.trim(),
                                        brandColorHex = selectedColor,
                                        ownershipType = selectedOwnershipType,
                                        totalRounds = selectedDraftRounds,
                                        preferredArchetype = selectedArchetype,
                                        customOwnerName = customOwnerNameInput.ifBlank { null },
                                        secondaryColorHex = selectedSecondaryColor
                                    )
                                    selectedCityForExpansion = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_confirm_expansion_draft")
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Host Expansion Draft & Ratify (+\$${city.expansionFeeMillions}M)", color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { selectedCityForExpansion = null }) {
                                Text("Cancel")
                            }

                            TextButton(
                                onClick = {
                                    if (nicknameInput.isNotBlank()) {
                                        viewModel.expandLeague(
                                            targetCityId = city.id,
                                            nickname = nicknameInput.trim(),
                                            colorHex = selectedColor,
                                            ownershipType = selectedOwnershipType,
                                            preferredArchetype = selectedArchetype,
                                            customOwnerName = customOwnerNameInput.ifBlank { null },
                                            secondaryColorHex = selectedSecondaryColor
                                        )
                                        selectedCityForExpansion = null
                                    }
                                },
                                modifier = Modifier.testTag("btn_confirm_ratification")
                            ) {
                                Text("Quick Charter (Skip Draft)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Candidate Withdrawal Confirmation Dialog
    if (candidateToWithdraw != null) {
        val city = candidateToWithdraw!!
        AlertDialog(
            onDismissRequest = { candidateToWithdraw = null },
            title = {
                Text(
                    text = "Withdraw Expansion Bid?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Withdraw the formal expansion proposal for ${city.name} (${if (city.isInternational) city.country else "USA"})? This will remove this market from active consideration and open up a candidate slot in the league pool (currently $openBidsCount/$MAX_OPEN_EXPANSION_CANDIDATES active)."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.withdrawMarketCandidate(city.id)
                        candidateToWithdraw = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    modifier = Modifier.testTag("btn_confirm_withdraw_bid")
                ) {
                    Text("Withdraw Bid", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { candidateToWithdraw = null }) {
                    Text("Keep Active")
                }
            }
        )
    }

    // Prospective Markets Scouting & Inflow Dialog
    if (showScoutingDialog) {
        val currentCityIds = allCities.map { it.id }.toSet()
        val currentCityNames = allCities.map { it.name.lowercase() }.toSet()
        val prospectiveCatalog = remember(allCities) {
            InitialLeagueData.getProspectiveMarketPool().filter {
                it.id !in currentCityIds && it.name.lowercase() !in currentCityNames
            }
        }
        val openCandidates = allCities.filter { !it.hasFranchise }
        val isPoolFull = openCandidates.size >= MAX_OPEN_EXPANSION_CANDIDATES

        var prospectFilter by remember { mutableStateOf(0) } // 0: All, 1: National (USA), 2: International, 3: Prime (A+), 4: Emerging/Speculative
        var selectedReplacementCityId by remember { mutableStateOf(openCandidates.firstOrNull()?.id) }

        val filteredProspects = remember(prospectiveCatalog, prospectFilter) {
            when (prospectFilter) {
                1 -> prospectiveCatalog.filter { !it.isInternational }
                2 -> prospectiveCatalog.filter { it.isInternational }
                3 -> prospectiveCatalog.filter { it.overallViabilityScore >= 80 }
                4 -> prospectiveCatalog.filter { it.overallViabilityScore < 60 }
                else -> prospectiveCatalog
            }
        }

        Dialog(onDismissRequest = { showScoutingDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(4.dp)
                    .border(2.dp, CyanAccent, RoundedCornerShape(20.dp))
                    .testTag("prospective_markets_dialog"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GLOBAL EXPANSION SCOUTING",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Prospective Market Inquiries",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }

                        IconButton(
                            onClick = { showScoutingDialog = false },
                            modifier = Modifier.testTag("btn_close_scouting_dialog")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close dialog")
                        }
                    }

                    // Pool Status Notice
                    Surface(
                        color = if (isPoolFull) CrimsonAccent.copy(alpha = 0.12f) else EmeraldAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPoolFull) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isPoolFull) CrimsonAccent else EmeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Candidate Pool Status: ${openCandidates.size} / $MAX_OPEN_EXPANSION_CANDIDATES Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPoolFull) CrimsonAccent else EmeraldAccent
                                )
                            }
                            if (isPoolFull) {
                                Text(
                                    text = "Pool is full! Selecting an applicant will replace the chosen candidate below:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Replacement selector chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(openCandidates) { c ->
                                        FilterChip(
                                            selected = selectedReplacementCityId == c.id,
                                            onClick = { selectedReplacementCityId = c.id },
                                            label = { Text("Replace ${c.name}", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CrimsonAccent.copy(alpha = 0.2f),
                                                selectedLabelColor = CrimsonAccent
                                            )
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "${MAX_OPEN_EXPANSION_CANDIDATES - openCandidates.size} open slot(s) remaining. Applicants can be admitted immediately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Prospect Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = prospectFilter == 0,
                                onClick = { prospectFilter = 0 },
                                label = { Text("All (${prospectiveCatalog.size})", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_prospect_all")
                            )
                        }
                        item {
                            FilterChip(
                                selected = prospectFilter == 1,
                                onClick = { prospectFilter = 1 },
                                label = { Text("National (USA)", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_prospect_national")
                            )
                        }
                        item {
                            FilterChip(
                                selected = prospectFilter == 2,
                                onClick = { prospectFilter = 2 },
                                label = { Text("International", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_prospect_intl")
                            )
                        }
                        item {
                            FilterChip(
                                selected = prospectFilter == 3,
                                onClick = { prospectFilter = 3 },
                                label = { Text("Prime (A+)", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_prospect_prime")
                            )
                        }
                        item {
                            FilterChip(
                                selected = prospectFilter == 4,
                                onClick = { prospectFilter = 4 },
                                label = { Text("Emerging (B/C)", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_prospect_emerging")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Prospective Markets List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("prospects_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProspects) { prospect ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .testTag("card_prospect_${prospect.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = prospect.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Surface(
                                                    color = if (prospect.isInternational) CyanAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (prospect.isInternational) prospect.country else "USA",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (prospect.isInternational) CyanAccent else MaterialTheme.colorScheme.primary,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${prospect.region} • ${prospect.tier.label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }

                                        // Viability Badge
                                        Surface(
                                            color = Color(prospect.viabilityGrade.badgeColorHex).copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(prospect.viabilityGrade.badgeColorHex))
                                        ) {
                                            Text(
                                                text = "${prospect.overallViabilityScore}/100 (${prospect.viabilityGrade.shortBadge})",
                                                color = Color(prospect.viabilityGrade.badgeColorHex),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Demographics summary row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Pop: ${prospect.populationMillions}M", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                                        Text("Growth: +${prospect.annualGrowthRatePct}%", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = EmeraldAccent)
                                        Text("Appetite: ${prospect.sportsAppetite}/100", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = GoldPrimary)
                                        Text("Fee: \$${prospect.expansionFeeMillions}M", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Arena: ${prospect.arenaReadinessStatus} • Group: ${prospect.investorGroupProfile}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            val replaceId = if (isPoolFull) selectedReplacementCityId else null
                                            val admitted = viewModel.admitMarketCandidate(prospect, replaceId)
                                            if (admitted) {
                                                showScoutingDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPoolFull) AmberWarning else CyanAccent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_admit_market_${prospect.id}")
                                            .height(38.dp)
                                    ) {
                                        Text(
                                            text = if (isPoolFull) "Replace Selected & Admit" else "Admit to Active Candidates",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
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
