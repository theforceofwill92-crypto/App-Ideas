package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExpansionApplicationStatus
import com.example.model.ExpansionRequest
import com.example.model.ExpansionRequestEngine
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import java.util.Locale

private enum class ExpansionFilterTab(val label: String) {
    PENDING("Pending"),
    REVIEWED("Reviewed"),
    ALL("All")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpansionRequestsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.openExpansionRequestsManagement(false)
    }

    var selectedFilter by remember { mutableStateOf(ExpansionFilterTab.PENDING) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    val pendingRequests = uiState.pendingExpansionRequests
    val reviewedRequests = uiState.reviewedExpansionRequests
    val displayedRequests = remember(selectedFilter, pendingRequests, reviewedRequests) {
        when (selectedFilter) {
            ExpansionFilterTab.PENDING -> pendingRequests
            ExpansionFilterTab.REVIEWED -> reviewedRequests
            ExpansionFilterTab.ALL -> pendingRequests + reviewedRequests
        }
    }

    val currentAnnualRevenueMillions = remember(
        uiState.franchises,
        uiState.activeBroadcast,
        uiState.activeSponsorship
    ) {
        ExpansionRequestEngine.estimateCurrentAnnualLeagueRevenueMillions(
            franchises = uiState.franchises,
            activeBroadcast = uiState.activeBroadcast,
            activeSponsorship = uiState.activeSponsorship
        )
    }

    val totalPendingReachGain = remember(pendingRequests) {
        Math.round(pendingRequests.sumOf { it.impact.projectedMarketReachGainMillions } * 10.0) / 10.0
    }
    val totalPendingRevGain = remember(pendingRequests) {
        pendingRequests.sumOf { it.impact.projectedAnnualRevenueGainMillions }
    }
    val totalPendingFees = remember(pendingRequests) {
        pendingRequests.sumOf { it.impact.immediateExpansionFeeMillions }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("expansion_requests_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Expansion Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pendingRequests.size} Pending • Market & Revenue Impact",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openExpansionRequestsManagement(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_expansion_requests")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to League"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val scouted = viewModel.scoutNewExpansionRequest()
                            actionFeedback = if (scouted != null) {
                                "New bid received from ${scouted.city.name} ${scouted.proposedNickname} (+${formatOneDecimal(scouted.impact.projectedMarketReachGainMillions)}M reach)."
                            } else {
                                "All eligible expansion markets currently have active bids or franchises."
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_scout_expansion_request")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scout",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
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
                .testTag("expansion_requests_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // League Baseline & Potential Upside Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_expansion_league_baseline"),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, CyanAccent.copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        CyanAccent.copy(alpha = 0.12f),
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        GoldPrimary.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LEAGUE GROWTH BASELINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${uiState.franchises.size} Franchises Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("metric_active_franchises_count")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BaselineKpiTile(
                                label = "Market Reach",
                                value = "${formatOneDecimal(uiState.totalLeagueFanbaseMillions)}M Fans",
                                accent = CyanAccent,
                                testTagName = "metric_current_market_reach",
                                modifier = Modifier.weight(1f)
                            )
                            BaselineKpiTile(
                                label = "Annual Revenue",
                                value = "\$${currentAnnualRevenueMillions}M/yr",
                                accent = EmeraldAccent,
                                testTagName = "metric_current_league_revenue",
                                modifier = Modifier.weight(1f)
                            )
                            BaselineKpiTile(
                                label = "Treasury Cash",
                                value = "\$${uiState.cashReservesMillions}M",
                                accent = GoldPrimary,
                                testTagName = "metric_current_treasury_cash",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (pendingRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Combined Pending Upside: +${formatOneDecimal(totalPendingReachGain)}M Reach • +\$${totalPendingRevGain}M/yr Revenue • +\$${totalPendingFees}M Fees",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inline Action Feedback Banner
            if (actionFeedback != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("banner_expansion_action_feedback"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.6f)),
                        colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.14f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = actionFeedback!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { actionFeedback = null },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val counts = mapOf(
                        ExpansionFilterTab.PENDING to pendingRequests.size,
                        ExpansionFilterTab.REVIEWED to reviewedRequests.size,
                        ExpansionFilterTab.ALL to (pendingRequests.size + reviewedRequests.size)
                    )
                    val tags = mapOf(
                        ExpansionFilterTab.PENDING to "chip_filter_pending",
                        ExpansionFilterTab.REVIEWED to "chip_filter_reviewed",
                        ExpansionFilterTab.ALL to "chip_filter_all"
                    )
                    ExpansionFilterTab.values().forEach { tab ->
                        val isSelected = selectedFilter == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = tab },
                            label = {
                                Text(
                                    text = "${tab.label} (${counts[tab] ?: 0})",
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
                                selectedLabelColor = CyanAccent
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(tags[tab] ?: "chip_filter")
                        )
                    }
                }
            }

            if (displayedRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_expansion_requests_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilter == ExpansionFilterTab.REVIEWED) {
                                    "No Reviewed Applications Yet"
                                } else {
                                    "All Pending Expansion Requests Processed"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scout international and high-growth domestic candidate cities to solicit new franchise proposals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val scouted = viewModel.scoutNewExpansionRequest()
                                    selectedFilter = ExpansionFilterTab.PENDING
                                    actionFeedback = if (scouted != null) {
                                        "New bid received from ${scouted.city.name} ${scouted.proposedNickname}."
                                    } else {
                                        "No additional open cities available."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Scout New Expansion Bid", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(displayedRequests, key = { it.id }) { req ->
                    val cba = remember(
                        req.city,
                        uiState.franchises,
                        uiState.activeBroadcast,
                        uiState.activeSponsorship
                    ) {
                        ExpansionRequestEngine.analyzeMarketCostBenefit(
                            city = req.city,
                            franchises = uiState.franchises,
                            activeBroadcast = uiState.activeBroadcast,
                            activeSponsorship = uiState.activeSponsorship
                        )
                    }
                    ExpansionRequestManagementCard(
                        request = req,
                        costBenefit = cba,
                        onApproveDirect = {
                            val success = viewModel.approveExpansionRequest(req.id, launchDraftImmediately = false)
                            if (success) {
                                actionFeedback = "Approved ${req.city.name} ${req.proposedNickname}! Market reach +${formatOneDecimal(req.impact.projectedMarketReachGainMillions)}M fans, annual revenue +\$${req.impact.projectedAnnualRevenueGainMillions}M/yr."
                            }
                        },
                        onApproveAndDraft = {
                            viewModel.approveExpansionRequest(req.id, launchDraftImmediately = true)
                        },
                        onDecline = {
                            val success = viewModel.declineExpansionRequest(req.id)
                            if (success) {
                                actionFeedback = "Declined ${req.city.name} ${req.proposedNickname} expansion proposal."
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BaselineKpiTile(
    label: String,
    value: String,
    accent: Color,
    testTagName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(testTagName)
            )
        }
    }
}

@Composable
private fun ExpansionRequestManagementCard(
    request: ExpansionRequest,
    costBenefit: com.example.model.MarketCostBenefitAnalysis,
    onApproveDirect: () -> Unit,
    onApproveAndDraft: () -> Unit,
    onDecline: () -> Unit
) {
    val gradeColor = Color(request.city.viabilityGrade.badgeColorHex)
    val brandColor = Color(request.brandColorHex)
    val secondaryColor = Color(request.secondaryColorHex)
    val archColor = Color(request.ownerArchetype.badgeColorHex)
    val impact = request.impact

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expansion_request_card_${request.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.2.dp,
            color = when (request.status) {
                ExpansionApplicationStatus.APPROVED -> EmeraldAccent.copy(alpha = 0.65f)
                ExpansionApplicationStatus.DECLINED -> CrimsonAccent.copy(alpha = 0.5f)
                ExpansionApplicationStatus.PENDING -> gradeColor.copy(alpha = 0.45f)
            }
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Header: Identity & Viability Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(brandColor.copy(alpha = 0.30f), secondaryColor.copy(alpha = 0.25f))
                                )
                            )
                            .border(
                                2.dp,
                                androidx.compose.ui.graphics.Brush.linearGradient(listOf(brandColor, secondaryColor)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = request.city.name.take(3).uppercase(Locale.US),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = brandColor
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${request.city.name} ${request.proposedNickname}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${request.city.country} • Pop ${request.city.populationMillions}M • Filed Y${request.requestedYear} W${request.requestedWeek}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = gradeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = request.city.viabilityGrade.shortBadge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = gradeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ownership & Arena Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = archColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${request.ownerFullName} • ${request.ownerArchetype.shortBadge}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = archColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (request.city.isInternational) {
                    Surface(
                        color = VioletAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "INTL GATEWAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = VioletAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = request.bidSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Venue: ${request.arenaPlanSummary}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Market Reach & League Revenue Impact Projection Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("impact_panel_${request.id}"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PROJECTED EXPANSION IMPACT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Market Reach Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = CyanAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Market Reach",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${formatOneDecimal(impact.projectedMarketReachGainMillions)}M Fans",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    modifier = Modifier.testTag("reach_gain_${request.id}")
                                )
                                Text(
                                    text = "+${formatOneDecimal(impact.projectedMarketReachGainPct)}% (${formatOneDecimal(impact.currentLeagueMarketReachMillions)}M → ${formatOneDecimal(impact.projectedPostApprovalMarketReachMillions)}M)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // League Revenue Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "League Revenue",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+\$${impact.projectedAnnualRevenueGainMillions}M/yr",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent,
                                    modifier = Modifier.testTag("revenue_gain_${request.id}")
                                )
                                Text(
                                    text = "+${formatOneDecimal(impact.projectedAnnualRevenueGainPct)}% (\$${impact.currentAnnualLeagueRevenueMillions}M → \$${impact.projectedPostApprovalAnnualRevenueMillions}M)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Detailed Revenue Stream Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TV +\$${impact.projectedAnnualBroadcastUpliftMillions}M",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlueAccent
                        )
                        Text(
                            text = "Sponsors +\$${impact.projectedAnnualLocalSponsorRevenueMillions}M",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldAccent
                        )
                        Text(
                            text = "Gate +\$${impact.projectedAnnualGateRevenueMillions}M",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Treasury Fee: +\$${impact.immediateExpansionFeeMillions}M",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            text = "Valuation: +\$${impact.projectedValuationUpliftMillions}M",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = impact.perTeamBroadcastDilutionNote,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = GoldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {}
                            .testTag("cba_summary_${request.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CBA ${costBenefit.cbaGrade} • Outlay \$${costBenefit.totalCapitalOutlayMillions}M",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = GoldPrimary
                            )
                            Text(
                                text = "+\$${costBenefit.projectedNetAnnualBenefitMillions}M/yr Net",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "+${costBenefit.fiveYearRoiPct.toInt()}% 5Y • ${costBenefit.paybackYears}y",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (request.status == ExpansionApplicationStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .heightIn(min = 48.dp)
                            .testTag("btn_decline_request_${request.id}")
                    ) {
                        Text("Decline", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApproveDirect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .heightIn(min = 48.dp)
                            .testTag("btn_approve_request_${request.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontWeight = FontWeight.Black, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = onApproveAndDraft,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GoldPrimary.copy(alpha = 0.2f),
                            contentColor = GoldPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .heightIn(min = 48.dp)
                            .testTag("btn_approve_draft_request_${request.id}")
                    ) {
                        Text("Draft", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            } else {
                val isApproved = request.status == ExpansionApplicationStatus.APPROVED
                val statusColor = if (isApproved) EmeraldAccent else CrimsonAccent
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isApproved) Icons.Default.Verified else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = request.status.label.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                            if (request.decisionNotes.isNotBlank()) {
                                Text(
                                    text = request.decisionNotes,
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

private fun formatOneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)
