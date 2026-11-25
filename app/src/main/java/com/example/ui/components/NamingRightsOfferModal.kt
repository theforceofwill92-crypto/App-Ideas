package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.NamingRightsOfferSession
import com.example.model.NamingRightsSponsorOffer
import com.example.model.NamingRightsTriggerReason
import com.example.model.SponsorBankruptcyScandalEvent
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary

@Composable
fun SponsorBankruptcyScandalModal(
    scandal: SponsorBankruptcyScandalEvent,
    onPickReplacement: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* User must acknowledge scandal and select replacement sponsor */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("sponsor_bankruptcy_scandal_modal")
                .border(2.dp, CrimsonAlert.copy(alpha = 0.9f), RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CrimsonAlert.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Sponsor Bankruptcy Scandal",
                                tint = CrimsonAlert,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SPONSOR BANKRUPTCY SCANDAL • Y${scandal.seasonYear}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CrimsonAlert,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${scandal.bankruptSponsorName} Collapses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = CrimsonAlert.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "TIER 3 DEFAULT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CrimsonAlert,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = scandal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScandalImpactPill(
                                label = "Deal Voided",
                                value = "-\$${scandal.lostAnnualRevenueMillions}M/yr",
                                color = CrimsonAlert,
                                modifier = Modifier.weight(1f)
                            )
                            ScandalImpactPill(
                                label = "League PR",
                                value = "-${scandal.leaguePrDrain} Pts",
                                color = AmberAccent,
                                modifier = Modifier.weight(1f)
                            )
                            ScandalImpactPill(
                                label = "Fan Morale",
                                value = "-${scandal.fanMoraleDrain}%",
                                color = CrimsonAlert,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Button(
                    onClick = onPickReplacement,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_scandal_pick_replacement")
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = "Pick Replacement Sponsor",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pick Replacement Sponsor",
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun NamingRightsOfferModal(
    session: NamingRightsOfferSession,
    onSelectSponsor: (String) -> Unit
) {
    var selectedOfferId by remember(session.id) {
        mutableStateOf(
            session.offers.firstOrNull { it.isEligible }?.id
                ?: session.offers.firstOrNull()?.id.orEmpty()
        )
    }
    val selectedOffer = session.offers.firstOrNull { it.id == selectedOfferId && it.isEligible }

    val borderAccent = when (session.triggerReason) {
        NamingRightsTriggerReason.BANKRUPTCY_SCANDAL -> CrimsonAlert
        NamingRightsTriggerReason.NEW_STADIUM_BUILT -> GoldPrimary
        NamingRightsTriggerReason.CONTRACT_EXPIRED -> CyanAccent
    }

    Dialog(
        onDismissRequest = { /* Commissioner must select a stadium naming rights partner */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .heightIn(max = 740.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("naming_rights_offer_modal")
                .border(2.dp, borderAccent.copy(alpha = 0.85f), RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(borderAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stadium,
                                contentDescription = "Naming Rights Offer",
                                tint = borderAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NAMING RIGHTS OFFER • Y${session.seasonYear}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = borderAccent,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = session.franchiseName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = borderAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = session.triggerReason.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = borderAccent,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (session.triggerReason == NamingRightsTriggerReason.BANKRUPTCY_SCANDAL) {
                        Surface(
                            color = CrimsonAlert.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = "Scandal Replacement",
                                    tint = CrimsonAlert,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${session.previousSponsorName ?: "Prior Tier 3 sponsor"} went bankrupt (-${session.scandalPrDrain} League PR). Select a replacement naming rights partner for ${session.cityName}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "${session.triggerReason.label} for ${session.franchiseName} (${session.teamWins}-${session.teamLosses} record). Naming rights revenue boosts annual league revenue and expands next season's Salary Cap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Text(
                        text = "SELECT STADIUM SPONSORSHIP PACKAGE (6 OFFERS • MAX 2 PER TIER)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 0.7.sp
                    )

                    val tierCounts = mutableMapOf<Int, Int>()
                    session.offers.forEach { offer ->
                        val occurrence = (tierCounts[offer.tier] ?: 0) + 1
                        tierCounts[offer.tier] = occurrence
                        NamingRightsSponsorOfferCard(
                            offer = offer,
                            tierOccurrence = occurrence,
                            isSelected = selectedOfferId == offer.id && offer.isEligible,
                            onClick = {
                                if (offer.isEligible) {
                                    selectedOfferId = offer.id
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { selectedOffer?.let { onSelectSponsor(it.id) } },
                    enabled = selectedOffer != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_confirm_naming_rights")
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Sign Naming Rights Deal",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedOffer != null) {
                            "Sign ${selectedOffer.sponsorName} (+\$${selectedOffer.annualRevenueMillions}M/yr)"
                        } else {
                            "Select an Eligible Sponsor"
                        },
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NamingRightsSponsorOfferCard(
    offer: NamingRightsSponsorOffer,
    tierOccurrence: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tierAccent = when (offer.tier) {
        1 -> EmeraldAccent
        2 -> CyanAccent
        else -> AmberAccent
    }
    val cardBorder = when {
        !offer.isEligible -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        isSelected -> GoldPrimary
        else -> tierAccent.copy(alpha = 0.45f)
    }
    val cardTag = if (tierOccurrence <= 1) {
        "card_naming_offer_tier_${offer.tier}"
    } else {
        "card_naming_offer_tier_${offer.tier}_$tierOccurrence"
    }

    Surface(
        color = when {
            !offer.isEligible -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            isSelected -> GoldPrimary.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (offer.isEligible) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag(cardTag)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            !offer.isEligible -> Icons.Default.Lock
                            isSelected -> Icons.Default.CheckCircle
                            else -> Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = if (!offer.isEligible) "Locked Sponsor Tier" else offer.sponsorName,
                        tint = if (!offer.isEligible) MaterialTheme.colorScheme.onSurfaceVariant else if (isSelected) GoldPrimary else tierAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = offer.tierTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (offer.isEligible) tierAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = offer.sponsorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = if (offer.isEligible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = tierAccent.copy(alpha = if (offer.isEligible) 0.16f else 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+\$${offer.annualRevenueMillions}M/yr",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = if (offer.isEligible) tierAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = offer.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${offer.termYears} Yr Term",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                val riskColor = when {
                    !offer.isEligible -> CrimsonAlert
                    offer.scandalRiskPct > 0 -> AmberAccent
                    else -> EmeraldAccent
                }
                Surface(
                    color = riskColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = offer.ineligibilityReason ?: offer.riskSummary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScandalImpactPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
