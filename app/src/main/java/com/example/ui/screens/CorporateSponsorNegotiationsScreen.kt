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
import com.example.data.InitialLeagueData
import com.example.model.*
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorporateSponsorNegotiationsScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val sponsorBidders = remember { InitialLeagueData.getCorporateSponsorBidders() }
    var selectedBidderIndex by remember { mutableStateOf(0) }
    var selectedSectorFilter by remember { mutableStateOf<SponsorSector?>(null) }

    val filteredBidders = remember(selectedSectorFilter) {
        if (selectedSectorFilter == null) sponsorBidders
        else sponsorBidders.filter { it.sector == selectedSectorFilter }
    }

    val currentBidder = filteredBidders.getOrNull(selectedBidderIndex) ?: sponsorBidders[0]

    // Minigame Negotiation State for Current Corporate Bidder
    var termYears by remember(currentBidder.id) { mutableStateOf(currentBidder.preferredTermYears) }
    var lumpSumBonus by remember(currentBidder.id) { mutableStateOf(currentBidder.baseLumpSumMillions.toFloat()) }
    var annualPayout by remember(currentBidder.id) { mutableStateOf(currentBidder.baseAnnualPayoutMillions.toFloat()) }
    var patienceRating by remember(currentBidder.id) { mutableStateOf(currentBidder.patienceRating) }
    var executiveMood by remember(currentBidder.id) { mutableStateOf(currentBidder.executiveQuote) }
    var reputationConcession by remember(currentBidder.id) { mutableStateOf(0) } // Dynamic reputation adjustment from tactical concessions

    // Hardware back navigation
    BackHandler {
        viewModel.openCorporateSponsorNegotiations(false)
    }

    val netReputationDelta = currentBidder.reputationEffect + reputationConcession
    val projectedLeagueIntegrity = (uiState.leagueIntegrityScore + netReputationDelta).coerceIn(20, 100)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("corporate_sponsor_negotiations_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CORPORATE SPONSORSHIP BOARDROOM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Partnership Negotiation Minigame & Reputation Levers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openCorporateSponsorNegotiations(false) },
                        modifier = Modifier
                            .testTag("btn_back_sponsor_negotiations")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // League Integrity & Treasury Display
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Integrity: ${uiState.leagueIntegrityScore}/100",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }
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
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)
        ) {
            // Sector Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSectorFilter == null,
                            onClick = {
                                selectedSectorFilter = null
                                selectedBidderIndex = 0
                            },
                            label = { Text("All Corporations (${sponsorBidders.size})") },
                            modifier = Modifier.testTag("filter_all_sponsors")
                        )
                    }
                    items(SponsorSector.values()) { sector ->
                        val isSelected = selectedSectorFilter == sector
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSectorFilter = if (isSelected) null else sector
                                selectedBidderIndex = 0
                            },
                            label = { Text(sector.title) },
                            modifier = Modifier.testTag("filter_sector_${sector.name.lowercase()}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Corporate Bidders Carousel Cards
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredBidders) { bidder ->
                        val isSelected = bidder.id == currentBidder.id
                        Card(
                            modifier = Modifier
                                .width(235.dp)
                                .clickable {
                                    val idx = filteredBidders.indexOf(bidder)
                                    selectedBidderIndex = idx
                                }
                                .border(
                                    1.5.dp,
                                    if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp)
                                )
                                .testTag("sponsor_bidder_card_${bidder.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) EmeraldAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
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
                                        color = Color(bidder.sector.badgeColorHex).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = bidder.sector.title.uppercase(),
                                            color = Color(bidder.sector.badgeColorHex),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        color = if (bidder.reputationEffect >= 0) EmeraldAccent.copy(alpha = 0.18f) else CrimsonAlert.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Rep: ${if (bidder.reputationEffect >= 0) "+${bidder.reputationEffect}" else "${bidder.reputationEffect}"}",
                                            color = if (bidder.reputationEffect >= 0) EmeraldAccent else CrimsonAlert,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = bidder.corporationName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Exec: ${bidder.executiveName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\$${bidder.baseAnnualPayoutMillions}M/yr + \$${bidder.baseLumpSumMillions}M Bonus",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Executive Boardroom Persona & Patience Thermometer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("sponsor_executive_card"),
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
                                    .background(Color(currentBidder.sector.badgeColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (currentBidder.sector) {
                                        SponsorSector.FINTECH_CRYPTO -> Icons.Default.CurrencyBitcoin
                                        SponsorSector.SPORTSBOOK_GAMBLING -> Icons.Default.Casino
                                        SponsorSector.ECO_RENEWABLES -> Icons.Default.Eco
                                        SponsorSector.GLOBAL_AIRLINE_LUXURY -> Icons.Default.FlightTakeoff
                                        SponsorSector.AUTOMOTIVE_MOBILITY -> Icons.Default.DirectionsCar
                                        SponsorSector.BEVERAGE_QUICK_SERVICE -> Icons.Default.LocalDrink
                                    },
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentBidder.executiveName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentBidder.executiveTitle} • ${currentBidder.corporationName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Executive Dialogue Speech Bubble
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "\"$executiveMood\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Deal Patience Thermometer
                        val patienceProgress = patienceRating / 100f
                        val patienceColor = when {
                            patienceRating > 65 -> EmeraldAccent
                            patienceRating > 30 -> GoldPrimary
                            else -> CrimsonAlert
                        }
                        ProgressBarWithLabel(
                            label = "Executive Board Patience / Deal Thermometer",
                            progress = patienceProgress,
                            displayValue = when {
                                patienceRating > 65 -> "$patienceRating% (Receptive & Flexible)"
                                patienceRating > 30 -> "$patienceRating% (Hesitant / Hardball)"
                                patienceRating > 0 -> "$patienceRating% (Walkout Danger!)"
                                else -> "WALKED OUT"
                            },
                            color = patienceColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Reputation Impact & League Integrity Barometer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (netReputationDelta >= 0) EmeraldAccent.copy(alpha = 0.5f) else CrimsonAlert.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("reputation_impact_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (netReputationDelta >= 0) EmeraldAccent.copy(alpha = 0.08f) else CrimsonAlert.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "LEAGUE REPUTATION & INTEGRITY IMPACT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (netReputationDelta >= 0) EmeraldAccent else CrimsonAlert,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Public Perception & Ethics Forecast",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = if (netReputationDelta >= 0) EmeraldAccent else CrimsonAlert,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (netReputationDelta >= 0) "+$netReputationDelta REPUTATION" else "$netReputationDelta REPUTATION",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when {
                                netReputationDelta >= 10 -> "Substantial public praise! Elevates league credibility and attracts wholesome family demographics."
                                netReputationDelta >= 0 -> "Favorable commercial alliance. Standard positive corporate synergy."
                                netReputationDelta >= -8 -> "Moderate public backlash over corporate ethics, but easily offset by large financial injection."
                                else -> "Severe integrity scrutiny! Media and player representatives will condemn gambling or controversial ties."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Integrity: ${uiState.leagueIntegrityScore}/100", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Projected Integrity: $projectedLeagueIntegrity/100", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (projectedLeagueIntegrity >= uiState.leagueIntegrityScore) EmeraldAccent else CrimsonAlert)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Interactive Negotiation Sliders (Term Length vs Upfront Lump-Sum)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("sponsor_negotiation_controls_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CONTRACT TERMS & EQUITY BALANCING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = EmeraldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Balance Partnership Term vs. Upfront Capital",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Long-term agreements lock in multi-year sponsorship cash flow. Shorter deals protect league reputation if public sentiment shifts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slider 1: Term Length (Years)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sponsorship Term Length:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$termYears YEARS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Slider(
                            value = termYears.toFloat(),
                            onValueChange = { newYears ->
                                termYears = newYears.toInt()
                                val termDelta = termYears - currentBidder.preferredTermYears
                                annualPayout = (currentBidder.baseAnnualPayoutMillions + (termDelta * 2.5f)).coerceAtLeast(15f)
                                executiveMood = if (termYears >= currentBidder.preferredTermYears) {
                                    "A $termYears-year partnership provides our branding team ample runway for national activations. We approve."
                                } else {
                                    "$termYears years is brief for a corporate title sponsorship. We will trim our annual rights payout accordingly."
                                }
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = EmeraldAccent, activeTrackColor = EmeraldAccent),
                            modifier = Modifier.testTag("slider_sponsor_term_length")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Slider 2: Upfront Signing Bonus ($M)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Upfront Partnership Signing Bonus:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "+\$${lumpSumBonus.toInt()}M CASH",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Immediate corporate treasury injection deposited directly into league bank reserves.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = lumpSumBonus,
                            onValueChange = { newBonus ->
                                val bonusDiff = newBonus - lumpSumBonus
                                lumpSumBonus = newBonus
                                annualPayout = (annualPayout - (bonusDiff / termYears.coerceAtLeast(1) * 0.65f)).coerceAtLeast(15f)
                                if (newBonus > currentBidder.baseLumpSumMillions * 1.35f) {
                                    patienceRating = (patienceRating - 3).coerceAtLeast(10)
                                    executiveMood = "Demanding \$${newBonus.toInt()}M upfront stretches our corporate marketing allocation. Tread carefully, Commissioner."
                                }
                            },
                            valueRange = 5f..60f,
                            steps = 11,
                            colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                            modifier = Modifier.testTag("slider_sponsor_lump_sum")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Annual Rights Fee Output
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Negotiated Annual Corporate Rights:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\$${annualPayout.toInt()}M / year",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tactical Maneuvers with Reputation & Financial Trade-offs
            item {
                Text(
                    text = "BOARDROOM TACTICAL MANEUVERS & REPUTATION TRADEOFFS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tactic 1: Jersey Crest & Arena Deck Exclusivity (More Money, Slight Fan Resistance)
                    OutlinedButton(
                        onClick = {
                            if (patienceRating >= 20) {
                                patienceRating = (patienceRating - 15).coerceAtLeast(10)
                                annualPayout += 6f
                                lumpSumBonus += 8f
                                reputationConcession -= 3
                                executiveMood = "Exclusive center-court logo and front-of-jersey crest rights secured! We will gladly boost annual purse by +$6M/yr and +$8M upfront."
                            }
                        },
                        enabled = patienceRating >= 20,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_tactic_jersey_crest"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Grant Primary Jersey & Court Crest Rights", fontWeight = FontWeight.Bold)
                            Text("+$6M/yr & +$8M Cash (-3 Reputation, Costs 15% Patience)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Tactic 2: Mandatory Youth & Community ESG Pledge (Boosts Reputation & Patience)
                    OutlinedButton(
                        onClick = {
                            patienceRating = (patienceRating + 20).coerceAtMost(100)
                            reputationConcession += 6
                            annualPayout -= 2f
                            executiveMood = "A joint community philanthropic initiative?! Our ESG investors applaud this. We will gladly accept and bolster our executive patience."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_tactic_esg_pledge"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mandate Community & Grassroots Philanthropy Pledge", fontWeight = FontWeight.Bold)
                            Text("+6 Reputation Boost, +20% Patience (-$2M/yr fee concession)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Tactic 3: Playoff Title Naming Rights (Maximizes Annual Rights Fee)
                    OutlinedButton(
                        onClick = {
                            if (patienceRating >= 25) {
                                patienceRating = (patienceRating - 20).coerceAtLeast(10)
                                annualPayout += 10f
                                reputationConcession -= 2
                                executiveMood = "The Apex Championship Series Presented by ${currentBidder.corporationName}?! That's billboard gold. You have our full commitment."
                            }
                        },
                        enabled = patienceRating >= 25,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_tactic_playoff_presenting"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bestow Playoff Title & Trophy Presenting Rights", fontWeight = FontWeight.Bold)
                            Text("+$10M/yr Annual Inflow (-2 Reputation, Costs 20% Patience)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Total Package Summary & Ratification
            item {
                val totalValue = lumpSumBonus + (annualPayout * termYears)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, EmeraldAccent, RoundedCornerShape(16.dp))
                        .testTag("sponsor_package_card"),
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
                                text = "TOTAL PARTNERSHIP COMMITMENT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "\$${totalValue.toInt()}M TOTAL",
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
                                Text("UPFRONT LUMP-SUM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+\$${lumpSumBonus.toInt()}M Cash", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                            }
                            Column {
                                Text("ANNUAL PAYOUT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("\$${annualPayout.toInt()}M / yr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CyanAccent)
                            }
                            Column {
                                Text("TERM LENGTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$termYears Years", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                            Column {
                                Text("REPUTATION DELTA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (netReputationDelta >= 0) "+$netReputationDelta Rep" else "$netReputationDelta Rep",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netReputationDelta >= 0) EmeraldAccent else CrimsonAlert
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sign Deal Button
                        Button(
                            onClick = {
                                val finalizedSponsorship = ActiveLeagueSponsorship(
                                    id = "spon_${currentBidder.id}_${System.currentTimeMillis() % 1000}",
                                    corporationName = currentBidder.corporationName,
                                    sector = currentBidder.sector,
                                    tier = currentBidder.tier,
                                    annualPayoutMillions = annualPayout.roundToLong(),
                                    yearsRemaining = termYears,
                                    upfrontSigningBonusMillions = lumpSumBonus.roundToLong(),
                                    reputationImpact = netReputationDelta,
                                    fanMoraleBonus = currentBidder.fanMoraleBonus,
                                    activationDescription = currentBidder.activationInitiative
                                )
                                viewModel.signNegotiatedCorporateSponsorship(
                                    sponsorship = finalizedSponsorship,
                                    lumpSumBonusMillions = lumpSumBonus.roundToLong()
                                )
                            },
                            enabled = patienceRating > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_sign_sponsor_deal")
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign Corporate Sponsorship (+\$${lumpSumBonus.toInt()}M Upfront)",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
