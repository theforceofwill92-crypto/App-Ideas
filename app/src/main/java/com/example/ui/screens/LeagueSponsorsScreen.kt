package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LeagueSimulationEngine
import com.example.model.LeagueSponsorContract
import com.example.model.LeagueSponsorOfferSession
import com.example.model.LeagueSponsorSlotType
import com.example.model.SponsorScandalSensitivity
import com.example.model.SponsorUltimatumEvent
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueSponsorsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    renderUltimatumOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { viewModel.openLeagueSponsorsScreen(false) }

    val sponsorsState = uiState.leagueSponsors
    val capProjection = remember(
        uiState.franchises,
        uiState.activeBroadcast,
        uiState.unionAgreement,
        sponsorsState
    ) {
        LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = uiState.franchises,
            activeBroadcast = uiState.activeBroadcast,
            unionAgreement = uiState.unionAgreement,
            leagueSponsors = sponsorsState
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("league_sponsors_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "League Sponsors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "4 Official Category Slots • Salary Cap Revenue Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openLeagueSponsorsScreen(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_league_sponsors")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back from League Sponsors",
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWide = maxWidth >= 600.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 880.dp)
                    .align(Alignment.TopCenter)
                    .testTag("league_sponsors_slot_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary_card") {
                    LeagueSponsorsSummaryCard(
                        activeCount = sponsorsState.activeCount,
                        totalAnnualPayout = sponsorsState.totalAnnualPayoutMillions,
                        projectedNextCap = capProjection.nextSalaryCapMillions,
                        annualRevenueWithSponsors = capProjection.annualLeagueRevenueMillions,
                        highRiskCount = sponsorsState.highSensitivityContracts().size,
                        lastSummary = sponsorsState.lastSummary
                    )
                }

                if (sponsorsState.expiredSlotNotices.isNotEmpty()) {
                    item(key = "expiry_notices") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("league_sponsors_expiry_banner")
                                .border(1.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "RECENT OFFSEASON CONTRACT EXPIRATIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = AmberWarning
                                )
                                sponsorsState.expiredSlotNotices.forEach { notice ->
                                    Text(
                                        text = notice,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (isWide) {
                    item(key = "slots_grid_row_1") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LeagueSponsorSlotCard(
                                slotType = LeagueSponsorSlotType.APPAREL,
                                contract = sponsorsState.apparel,
                                onOpenOffers = { viewModel.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.APPAREL) },
                                modifier = Modifier.weight(1f)
                            )
                            LeagueSponsorSlotCard(
                                slotType = LeagueSponsorSlotType.BEVERAGE,
                                contract = sponsorsState.beverage,
                                onOpenOffers = { viewModel.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.BEVERAGE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item(key = "slots_grid_row_2") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LeagueSponsorSlotCard(
                                slotType = LeagueSponsorSlotType.TECH,
                                contract = sponsorsState.tech,
                                onOpenOffers = { viewModel.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.TECH) },
                                modifier = Modifier.weight(1f)
                            )
                            LeagueSponsorSlotCard(
                                slotType = LeagueSponsorSlotType.BETTING,
                                contract = sponsorsState.betting,
                                onOpenOffers = { viewModel.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.BETTING) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    LeagueSponsorSlotType.entries.forEach { slotType ->
                        item(key = "slot_card_${slotType.key}") {
                            LeagueSponsorSlotCard(
                                slotType = slotType,
                                contract = sponsorsState.getSlot(slotType),
                                onOpenOffers = { viewModel.openLeagueSponsorSlotOffers(slotType) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    sponsorsState.activeOfferSession?.let { session ->
        LeagueSponsorOffersModal(
            session = session,
            onSignOffer = { offerId -> viewModel.signLeagueSponsorOffer(offerId) },
            onDismiss = { viewModel.dismissLeagueSponsorOfferSession() }
        )
    }

    if (renderUltimatumOverlay) {
        sponsorsState.activeUltimatum?.let { ultimatum ->
            SponsorUltimatumModal(
                ultimatum = ultimatum,
                onEnforceMaxPenalty = { viewModel.resolveSponsorUltimatum(enforceMaxPenalty = true) },
                onLetSponsorPullOut = { viewModel.resolveSponsorUltimatum(enforceMaxPenalty = false) }
            )
        }
    }
}

@Composable
private fun LeagueSponsorsSummaryCard(
    activeCount: Int,
    totalAnnualPayout: Long,
    projectedNextCap: Long,
    annualRevenueWithSponsors: Long,
    highRiskCount: Int,
    lastSummary: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("league_sponsors_summary_card")
            .border(1.5.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surfaceVariant,
                            EmeraldAccent.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LEAGUE COMMERCIAL PORTFOLIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$activeCount / 4 Partner Slots Filled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = if (highRiskCount > 0) AmberWarning.copy(alpha = 0.2f) else EmeraldAccent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (highRiskCount > 0) "$highRiskCount High-Risk Clause" else "Low PR Exposure",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (highRiskCount > 0) AmberWarning else EmeraldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SPONSOR PAYOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "+\$${totalAnnualPayout}M/yr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ANNUAL REV",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "\$${annualRevenueWithSponsors}M",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "PROJ. CAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "\$${projectedNextCap}M",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                    }
                }
            }

            Text(
                text = lastSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeagueSponsorSlotCard(
    slotType: LeagueSponsorSlotType,
    contract: LeagueSponsorContract?,
    onOpenOffers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(slotType.accentHex)
    val slotIcon: ImageVector = when (slotType) {
        LeagueSponsorSlotType.APPAREL -> Icons.Default.Checkroom
        LeagueSponsorSlotType.BEVERAGE -> Icons.Default.LocalDrink
        LeagueSponsorSlotType.TECH -> Icons.Default.Memory
        LeagueSponsorSlotType.BETTING -> Icons.Default.QueryStats
    }
    val sensitivityColor = contract?.scandalSensitivity?.let { Color(it.badgeColorHex) } ?: accent
    val isEmpty = contract == null

    Card(
        modifier = modifier
            .testTag("sponsor_slot_card_${slotType.key}")
            .then(
                if (isEmpty) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onOpenOffers)
                } else Modifier
            )
            .border(
                width = if (isEmpty) 1.5.dp else 1.5.dp,
                color = if (isEmpty) accent.copy(alpha = 0.55f) else sensitivityColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slotIcon,
                            contentDescription = "${slotType.displayName} Sponsor Slot",
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = slotType.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accent,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = slotType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (contract == null) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OPEN SLOT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(contract.scandalSensitivity.badgeColorHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = contract.scandalSensitivity.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(contract.scandalSensitivity.badgeColorHex),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (contract == null) {
                Text(
                    text = slotType.emptyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenOffers,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_open_slot_offers_${slotType.key}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddBusiness,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View 6 Sponsor Offers",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = contract.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (contract.tagline.isNotBlank()) {
                            Text(
                                text = contract.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PAYOUT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "+\$${contract.payout}M/yr",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent
                                )
                            }
                            Column {
                                Text(
                                    text = "TERM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${contract.yearsLeft} yr(s) left",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "PERK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = contract.perkType.shortBadge,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(contract.perkType.accentHex)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (contract.scandalSensitivity.isHigh) Icons.Default.Warning else Icons.Default.VerifiedUser,
                                contentDescription = "Scandal Sensitivity Status",
                                tint = sensitivityColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${contract.scandalSensitivity.label}: ${contract.scandalSensitivity.clauseSummary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (contract.scandalSensitivity.isHigh) sensitivityColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeagueSponsorOffersModal(
    session: LeagueSponsorOfferSession,
    onSignOffer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOfferId by remember(session.id) {
        mutableStateOf(session.offers.firstOrNull()?.id.orEmpty())
    }
    val selectedOffer = session.offers.firstOrNull { it.id == selectedOfferId }
    val slotAccent = Color(session.slotType.accentHex)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .border(2.dp, slotAccent, RoundedCornerShape(20.dp))
                .testTag("modal_league_sponsor_offers"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${session.slotType.displayName.uppercase()} SPONSORSHIP AUCTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = slotAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${session.offers.size} Competing Partner Bids",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_close_sponsor_offers_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Sponsor Offers"
                        )
                    }
                }

                Text(
                    text = "Trade off annual payout size (which adds directly to league revenue & raises next season's Salary Cap) against scandal sensitivity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("list_league_sponsor_offers"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = session.offers.size,
                        key = { index -> session.offers[index].id }
                    ) { index ->
                        val offer = session.offers[index]
                        val isSelected = offer.id == selectedOfferId
                        val sensColor = Color(offer.scandalSensitivity.badgeColorHex)
                        val perkColor = Color(offer.perkType.accentHex)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { selectedOfferId = offer.id }
                                .testTag("card_league_sponsor_offer_$index")
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GoldPrimary else sensColor.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = offer.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = offer.tagline,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        color = EmeraldAccent.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "+\$${offer.payout}M/yr",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = EmeraldAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = sensColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = offer.scandalSensitivity.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = sensColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        color = perkColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = offer.perkType.shortBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = perkColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${offer.termYears}Y Term",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }

                                Text(
                                    text = offer.tradeoffNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (offer.scandalSensitivity.isHigh) sensColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (selectedOfferId.isNotBlank()) {
                            onSignOffer(selectedOfferId)
                        }
                    },
                    enabled = selectedOffer != null,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_sign_league_sponsor")
                ) {
                    Text(
                        text = selectedOffer?.let {
                            "Sign ${it.name} (+\$${it.payout}M/yr)"
                        } ?: "Select a Sponsor Offer",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun SponsorUltimatumModal(
    ultimatum: SponsorUltimatumEvent,
    onEnforceMaxPenalty: () -> Unit,
    onLetSponsorPullOut: () -> Unit
) {
    val riskColor = when (ultimatum.scandalSensitivity) {
        SponsorScandalSensitivity.CRITICAL -> CrimsonAlert
        else -> AmberWarning
    }

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, riskColor, RoundedCornerShape(20.dp))
                .testTag("modal_sponsor_ultimatum"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(riskColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Sponsor Ultimatum",
                            tint = riskColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SPONSOR ULTIMATUM • ${ultimatum.slotType.displayName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = riskColor,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${ultimatum.sponsorName} Threatens Pullout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = ultimatum.scandalSourceTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ultimatum.scandalSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Deal At Risk: \$${ultimatum.sponsorPayoutMillions}M/yr (${ultimatum.scandalSensitivity.label})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = riskColor
                        )
                        ultimatum.relatedPlayerName?.let { playerName ->
                            Text(
                                text = "Implicated Player: $playerName (${ultimatum.relatedFranchiseName ?: "League Roster"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "${ultimatum.sponsorName} will void their official ${ultimatum.slotType.displayName} contract unless the Commissioner enforces a maximum suspension (${ultimatum.requiredMaxSuspensionGames} games) and maximum fine (\$${ultimatum.requiredMaxFineThousands}K).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onEnforceMaxPenalty,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_ultimatum_max_penalty")
                ) {
                    Text(
                        text = "Levy Max Penalty (${ultimatum.requiredMaxSuspensionGames}G / \$${ultimatum.requiredMaxFineThousands}K) & Keep Sponsor",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onLetSponsorPullOut,
                    border = BorderStroke(1.5.dp, CrimsonAlert),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_ultimatum_pull_out")
                ) {
                    Text(
                        text = "Decline Max Sanctions — Sponsor Pulls Out (-\$$${ultimatum.sponsorPayoutMillions}M/yr)".replace("-\$\$", "-\$"),
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
