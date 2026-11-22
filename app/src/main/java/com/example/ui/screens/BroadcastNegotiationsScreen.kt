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
import com.example.data.InitialLeagueData
import com.example.model.BroadcastBidStatus
import com.example.model.BroadcastBiddingEngine
import com.example.model.BroadcastContractOffer
import com.example.model.NetworkScope
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastNegotiationsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val session = remember(uiState.broadcastBiddingSession) {
        if (uiState.broadcastBiddingSession.offers.isNotEmpty()) {
            uiState.broadcastBiddingSession
        } else {
            BroadcastBiddingEngine.createInitialSession(
                bidders = InitialLeagueData.getNetworkBidders(),
                currentContract = uiState.activeBroadcast,
                teamCount = uiState.franchises.size,
                playerRevenueSharePct = uiState.unionAgreement.playerRevenueSharePct,
                currentFanbaseMillions = uiState.totalLeagueFanbaseMillions,
                currentYear = uiState.currentYear
            )
        }
    }

    var scopeFilterIndex by remember { mutableStateOf(0) } // 0: All, 1: National, 2: Regional

    val filteredOffers = remember(session.offers, scopeFilterIndex) {
        when (scopeFilterIndex) {
            1 -> session.offers.filter {
                it.bidder.scope == NetworkScope.NATIONAL_CABLE ||
                    it.bidder.scope == NetworkScope.GLOBAL_STREAMING ||
                    it.bidder.scope == NetworkScope.FREE_TO_AIR
            }
            2 -> session.offers.filter { it.bidder.scope == NetworkScope.REGIONAL_RSN }
            else -> session.offers
        }
    }

    val currentOffer = filteredOffers.find { it.id == session.selectedOfferId }
        ?: filteredOffers.firstOrNull()
        ?: session.offers.first()

    val isWithdrawn = currentOffer.bidStatus == BroadcastBidStatus.WITHDRAWN
    val activeBiddersCount = session.offers.count { it.bidStatus != BroadcastBidStatus.WITHDRAWN }
    val leadingOffer = session.offers
        .filter { it.bidStatus != BroadcastBidStatus.WITHDRAWN }
        .maxByOrNull { it.impact.cumulativeGrossValueMillions }

    BackHandler {
        viewModel.openBroadcastNegotiations(false)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BROADCAST WAR ROOM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Media Rights Bidding & Long-Term Valuation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openBroadcastNegotiations(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_negotiations")
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "\$${uiState.cashReservesMillions}M",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("broadcast_negotiations_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("broadcast_negotiations_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)
        ) {
            // 1. Live Bidding Market Overview & Auction Trigger
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .testTag("card_broadcast_market_overview"),
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
                                    text = "COMPETITIVE RIGHTS AUCTION • ROUND ${session.biddingRound}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Current Deal: ${uiState.activeBroadcast.partnerName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "\$${uiState.activeBroadcast.annualPayoutMillions}M/yr • ${uiState.activeBroadcast.yearsRemaining} yrs left • $activeBiddersCount/${session.offers.size} active bidders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (leadingOffer != null) {
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "TOP BID",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = EmeraldAccent
                                        )
                                        Text(
                                            text = "\$${leadingOffer.impact.cumulativeGrossValueMillions}M",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.runBroadcastBiddingRound() },
                            enabled = activeBiddersCount > 1,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("btn_run_bidding_round")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Run bidding round",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeBiddersCount > 1) {
                                    "Run Competitive Bidding Round ${session.biddingRound + 1}"
                                } else {
                                    "Final Network Offer On Table"
                                },
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        }

                        AnimatedVisibility(visible = session.lastEscalationSummary != null) {
                            session.lastEscalationSummary?.let { summary ->
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .testTag("banner_bidding_round_summary")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. Scope Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = scopeFilterIndex == 0,
                            onClick = { scopeFilterIndex = 0 },
                            label = { Text("All Networks (${session.offers.size})") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_all_networks")
                        )
                    }
                    item {
                        FilterChip(
                            selected = scopeFilterIndex == 1,
                            onClick = { scopeFilterIndex = 1 },
                            label = { Text("National") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_national_networks")
                        )
                    }
                    item {
                        FilterChip(
                            selected = scopeFilterIndex == 2,
                            onClick = { scopeFilterIndex = 2 },
                            label = { Text("Regional RSNs") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_regional_rsns")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Network Contract Offers Carousel
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("broadcast_offers_carousel")
                ) {
                    items(filteredOffers, key = { it.id }) { offer ->
                        BroadcastOfferCard(
                            offer = offer,
                            isSelected = offer.id == currentOffer.id,
                            onClick = { viewModel.selectBroadcastOffer(offer.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. Executive Boardroom Persona & Patience Thermometer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("executive_boardroom_card"),
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(currentOffer.bidder.scope.badgeColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentOffer.bidder.scope == NetworkScope.GLOBAL_STREAMING) {
                                        Icons.Default.CastConnected
                                    } else {
                                        Icons.Default.LiveTv
                                    },
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentOffer.bidder.executiveName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentOffer.bidder.executiveTitle} • ${currentOffer.bidder.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = Color(currentOffer.bidStatus.badgeColorHex).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = currentOffer.bidStatus.label,
                                    color = Color(currentOffer.bidStatus.badgeColorHex),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "\"${currentOffer.latestExecutiveNote}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val patienceProgress = currentOffer.currentPatience / 100f
                        val patienceColor = when {
                            currentOffer.currentPatience > 65 -> EmeraldAccent
                            currentOffer.currentPatience > 30 -> GoldPrimary
                            else -> CrimsonAlert
                        }
                        ProgressBarWithLabel(
                            label = "Executive Patience / Deal Thermometer",
                            progress = patienceProgress,
                            displayValue = when {
                                currentOffer.currentPatience > 65 -> "${currentOffer.currentPatience}% (Receptive)"
                                currentOffer.currentPatience > 30 -> "${currentOffer.currentPatience}% (Pushing Back)"
                                currentOffer.currentPatience > 0 -> "${currentOffer.currentPatience}% (Nearing Walkout!)"
                                else -> "WITHDRAWN"
                            },
                            color = patienceColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Interactive Contract Structure Sliders
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("negotiation_controls_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CONTRACT STRUCTURE & COUNTER-OFFER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Adjust Term Length & Upfront Signing Bonus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Longer contracts lock in multi-year escalator compounding (+${currentOffer.annualEscalatorPct}%/yr) and boost league valuation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contract Term Length:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${currentOffer.termYears} YEARS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Slider(
                            value = currentOffer.termYears.toFloat(),
                            enabled = !isWithdrawn,
                            onValueChange = { newYearsFloat ->
                                val newYears = newYearsFloat.toInt().coerceIn(1, 7)
                                val termDelta = newYears - currentOffer.bidder.preferredTermYears
                                val adjustedAnnual = (currentOffer.annualPayoutMillions + (termDelta - (currentOffer.termYears - currentOffer.bidder.preferredTermYears)) * 3L)
                                    .coerceAtLeast(35L)
                                val note = if (newYears >= currentOffer.bidder.preferredTermYears) {
                                    "A $newYears-year commitment provides solid schedule predictability and stronger escalator compounding."
                                } else {
                                    "$newYears years is short for our programming grid — we trimmed annual rights accordingly."
                                }
                                viewModel.updateBroadcastOfferStructure(
                                    offerId = currentOffer.id,
                                    termYears = newYears,
                                    lumpSumBonusMillions = currentOffer.lumpSumBonusMillions,
                                    annualPayoutMillions = adjustedAnnual,
                                    currentPatience = currentOffer.currentPatience,
                                    executiveNote = note,
                                    markCountered = true
                                )
                            },
                            valueRange = 1f..7f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                            modifier = Modifier.testTag("slider_term_length")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Upfront Signing Bonus:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "+\$${currentOffer.lumpSumBonusMillions}M CASH",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Slider(
                            value = currentOffer.lumpSumBonusMillions.toFloat().coerceIn(5f, 70f),
                            enabled = !isWithdrawn,
                            onValueChange = { newBonusFloat ->
                                val newBonus = newBonusFloat.roundToLong().coerceIn(5L, 70L)
                                val bonusDiff = newBonus - currentOffer.lumpSumBonusMillions
                                val adjustedAnnual = (currentOffer.annualPayoutMillions - (bonusDiff / currentOffer.termYears.coerceAtLeast(1) * 0.65).roundToLong())
                                    .coerceAtLeast(35L)
                                val patienceDrop = if (newBonus > currentOffer.bidder.baseLumpSumMillions * 1.35) 2 else 0
                                val note = if (patienceDrop > 0) {
                                    "Requesting \$${newBonus}M upfront stretches our immediate liquidity — balancing annual rights to \$${adjustedAnnual}M/yr."
                                } else {
                                    "Restructured signing bonus to \$${newBonus}M upfront with \$${adjustedAnnual}M Year-1 rights."
                                }
                                viewModel.updateBroadcastOfferStructure(
                                    offerId = currentOffer.id,
                                    termYears = currentOffer.termYears,
                                    lumpSumBonusMillions = newBonus,
                                    annualPayoutMillions = adjustedAnnual,
                                    currentPatience = (currentOffer.currentPatience - patienceDrop).coerceAtLeast(10),
                                    executiveNote = note,
                                    markCountered = true
                                )
                            },
                            valueRange = 5f..70f,
                            steps = 12,
                            colors = SliderDefaults.colors(thumbColor = EmeraldAccent, activeTrackColor = EmeraldAccent),
                            modifier = Modifier.testTag("slider_lump_sum")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Annual Escalating Payout:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Y1 \$${currentOffer.impact.year1AnnualPayoutMillions}M → Y${currentOffer.termYears} \$${currentOffer.impact.finalYearAnnualPayoutMillions}M",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 6. Tactical Negotiation Maneuvers
            item {
                Text(
                    text = "COMMISSIONER TACTICAL MANEUVERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (currentOffer.currentPatience >= 25) {
                                viewModel.updateBroadcastOfferStructure(
                                    offerId = currentOffer.id,
                                    termYears = currentOffer.termYears,
                                    lumpSumBonusMillions = currentOffer.lumpSumBonusMillions + 6L,
                                    annualPayoutMillions = currentOffer.annualPayoutMillions + 8L,
                                    currentPatience = currentOffer.currentPatience - 22,
                                    executiveNote = "Playing rival networks against us? Fine — +\$8M/yr and +\$6M bonus added, but watch your leverage.",
                                    markCountered = true
                                )
                            }
                        },
                        enabled = !isWithdrawn && currentOffer.currentPatience >= 25,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("btn_tactic_bidding_war"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Leverage Rival Network Bidding War", fontWeight = FontWeight.Bold)
                            Text(
                                "+\$8M/yr & +\$6M Lump-Sum (Costs 22% Patience)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.updateBroadcastOfferStructure(
                                offerId = currentOffer.id,
                                termYears = currentOffer.termYears.coerceAtLeast(4),
                                lumpSumBonusMillions = currentOffer.lumpSumBonusMillions + 15L,
                                annualPayoutMillions = currentOffer.annualPayoutMillions,
                                currentPatience = (currentOffer.currentPatience + 12).coerceAtMost(100),
                                executiveNote = "Exclusive digital live streaming rights unlocked! Adding +\$15M upfront cash happily.",
                                digitalExclusivity = true,
                                markCountered = true
                            )
                        },
                        enabled = !isWithdrawn && !currentOffer.digitalExclusivityIncluded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("btn_tactic_exclusivity"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (currentOffer.digitalExclusivityIncluded) "Digital Live Exclusivity Granted" else "Offer Digital Live Exclusivity",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "+\$15M Lump-Sum Bonus, +12% Patience (Locks 4+ Yrs)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.updateBroadcastOfferStructure(
                                offerId = currentOffer.id,
                                termYears = currentOffer.termYears,
                                lumpSumBonusMillions = currentOffer.lumpSumBonusMillions,
                                annualPayoutMillions = currentOffer.annualPayoutMillions + 4L,
                                currentPatience = (currentOffer.currentPatience + 25).coerceAtMost(100),
                                executiveNote = "Exclusive playoff primetime windows calmed our board. Raising annual payout +\$4M/yr.",
                                playoffPrimetime = true,
                                markCountered = true
                            )
                        },
                        enabled = !isWithdrawn && !currentOffer.playoffPrimetimeIncluded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("btn_tactic_playoff_primetime"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (currentOffer.playoffPrimetimeIncluded) "Postseason Primetime Conceded" else "Grant Postseason Primetime Exclusivity",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Restores +25% Patience, +\$4M/yr Annual Rights",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 7. Long-Term Financial Impact Evaluator
            item {
                val impact = currentOffer.impact
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyanAccent.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .testTag("long_term_financial_impact_card"),
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
                                    text = "LONG-TERM FINANCIAL IMPACT EVALUATOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "${impact.termYears}-Year Horizon vs Current Contract",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = impact.strategicVerdict,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2x2 Impact Metric Tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImpactMetricTile(
                                title = "CUMULATIVE GROSS",
                                value = "\$${impact.cumulativeGrossValueMillions}M",
                                subtitle = if (impact.netGainVsCurrentContractMillions >= 0) {
                                    "+\$${impact.netGainVsCurrentContractMillions}M vs current"
                                } else {
                                    "${impact.netGainVsCurrentContractMillions}M vs current"
                                },
                                accent = if (impact.netGainVsCurrentContractMillions >= 0) EmeraldAccent else CrimsonAlert,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("metric_total_contract_value")
                            )
                            ImpactMetricTile(
                                title = "LEAGUE TREASURY",
                                value = "\$${impact.totalLeagueTreasuryInflowMillions}M",
                                subtitle = "Upfront + 15% Rights Cut",
                                accent = GoldPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("metric_treasury_inflow")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImpactMetricTile(
                                title = "VALUATION & FAN LIFT",
                                value = "+\$${impact.projectedValuationLiftMillions}M Val",
                                subtitle = "+${impact.projectedFanReachGrowthMillions}M Fans (${currentOffer.bidder.reachMultiplier}x)",
                                accent = CyanAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("metric_valuation_reach_lift")
                            )
                            ImpactMetricTile(
                                title = "PER-TEAM TV SHARE",
                                value = "\$${impact.averagePerTeamAnnualShareMillions}M/yr",
                                subtitle = "After ${uiState.unionAgreement.playerRevenueSharePct}% CBA Share",
                                accent = EmeraldAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("metric_per_team_tv_share")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "YEAR-BY-YEAR CONTRACT PROJECTION (+${impact.annualEscalatorPct}%/YR ESCALATOR)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                                .testTag("yearly_projection_table"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("SEASON", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("GROSS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("OWNERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("NET DELTA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            impact.yearlyProjections.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Y${row.yearOffset} (${row.seasonYear})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    Text(
                                        text = "\$${row.grossPayoutMillions}M",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    Text(
                                        text = "\$${row.ownerPoolMillions}M",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    Text(
                                        text = if (row.netAdvantageVsCurrentMillions >= 0) {
                                            "+\$${row.netAdvantageVsCurrentMillions}M"
                                        } else {
                                            "${row.netAdvantageVsCurrentMillions}M"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (row.netAdvantageVsCurrentMillions >= 0) EmeraldAccent else CrimsonAlert,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (session.auctionLog.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "RECENT AUCTION ACTIVITY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            session.auctionLog.take(3).forEach { entry ->
                                Text(
                                    text = "• $entry",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 8. Total Package Summary & Finalize Deal CTA
            item {
                val impact = currentOffer.impact
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, if (isWithdrawn) CrimsonAlert else EmeraldAccent, RoundedCornerShape(16.dp))
                        .testTag("total_package_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL CONTRACT PACKAGE VALUE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "\$${impact.cumulativeGrossValueMillions}M TOTAL",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("UPFRONT BONUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+\$${currentOffer.lumpSumBonusMillions}M", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                            }
                            Column {
                                Text("YR-1 ANNUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\$${currentOffer.annualPayoutMillions}M/yr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CyanAccent)
                            }
                            Column {
                                Text("TERM LENGTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentOffer.termYears} Years", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                            Column {
                                Text("REACH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentOffer.bidder.reachMultiplier}x", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.acceptBroadcastOffer(currentOffer.id) },
                            enabled = !isWithdrawn && currentOffer.currentPatience > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(52.dp)
                                .testTag("btn_sign_final_deal"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWithdrawn) {
                                    "Network Withdrew From Auction"
                                } else {
                                    "Accept & Sign (+\$${currentOffer.lumpSumBonusMillions}M Upfront)"
                                },
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastOfferCard(
    offer: BroadcastContractOffer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isWithdrawn = offer.bidStatus == BroadcastBidStatus.WITHDRAWN
    val borderColor = when {
        isSelected -> GoldPrimary
        offer.bidStatus == BroadcastBidStatus.LEADING_BID -> EmeraldAccent.copy(alpha = 0.7f)
        isWithdrawn -> CrimsonAlert.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Card(
        modifier = Modifier
            .width(248.dp)
            .minimumInteractiveComponentSize()
            .clickable { onClick() }
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("bidder_card_${offer.bidder.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(offer.bidder.scope.badgeColorHex).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = offer.bidder.scope.label.uppercase(),
                        color = Color(offer.bidder.scope.badgeColorHex),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(offer.bidStatus.badgeColorHex).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = offer.bidStatus.label,
                        color = Color(offer.bidStatus.badgeColorHex),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = offer.bidder.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isWithdrawn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Exec: ${offer.bidder.executiveName} • ${offer.termYears} yrs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\$${offer.impact.cumulativeGrossValueMillions}M Total Value",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = if (isWithdrawn) CrimsonAlert else EmeraldAccent
            )
            Text(
                text = "\$${offer.annualPayoutMillions}M/yr (+${offer.annualEscalatorPct}%/yr) • +\$${offer.lumpSumBonusMillions}M Bonus",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImpactMetricTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
