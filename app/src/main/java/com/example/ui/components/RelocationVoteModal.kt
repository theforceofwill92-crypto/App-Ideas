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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.model.RelocationDealOffer
import com.example.model.RelocationVoteState
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RoyalBlueAccent

@Composable
fun RelocationVoteModal(
    voteState: RelocationVoteState,
    onSelectCity: (String) -> Unit,
    onApproveRelocation: () -> Unit,
    onVetoRelocation: () -> Unit
) {
    val selectedDeal = voteState.cityDeals.firstOrNull { it.cityId == voteState.selectedCityId }
        ?: voteState.cityDeals.firstOrNull()

    Dialog(
        onDismissRequest = { /* Commissioner must Approve or Veto Relocation */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .heightIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("relocation_vote_modal")
                .border(2.dp, CrimsonAlert.copy(alpha = 0.85f), RoundedCornerShape(22.dp)),
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
                                .background(CrimsonAlert.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = "Relocation Vote",
                                tint = CrimsonAlert,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RELOCATION VOTE • Y${voteState.seasonYear}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CrimsonAlert,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${voteState.franchiseName} Threaten Move",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = CrimsonAlert.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "3 SUITOR DEALS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CrimsonAlert,
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${voteState.ownerName} (${voteState.ownershipGroupName}) walked away from private stadium talks in ${voteState.currentCityName} (${voteState.stadiumAge} yrs old, ${voteState.stadiumCondition}% condition). Select a suitor stadium deal below to Approve Relocation, or Veto Relocation to keep the franchise home.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text(
                        text = "CITIES OFFERING STADIUM DEALS (${voteState.cityDeals.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 0.8.sp
                    )

                    voteState.cityDeals.forEach { deal ->
                        val isSelected = deal.cityId == (selectedDeal?.cityId ?: voteState.selectedCityId)
                        RelocationDealOfferCard(
                            deal = deal,
                            isSelected = isSelected,
                            onClick = { onSelectCity(deal.cityId) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Commissioner Ruling Buttons: Approve Relocation OR Veto Relocation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onVetoRelocation,
                        border = BorderStroke(1.5.dp, RoyalBlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_veto_relocation")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Veto Relocation",
                                tint = RoyalBlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Veto Relocation",
                                color = RoyalBlueAccent,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onApproveRelocation,
                        enabled = selectedDeal != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_approve_relocation")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = "Approve Relocation",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Approve Relocation",
                                fontWeight = FontWeight.Black,
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
private fun RelocationDealOfferCard(
    deal: RelocationDealOffer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = if (isSelected) EmeraldAccent else CyanAccent
    Surface(
        color = if (isSelected) EmeraldAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("card_relocation_city_${deal.cityId}")
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
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected city deal" else "Select city deal",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${deal.cityName} (${deal.region})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = deal.arenaName,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    color = GoldPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${deal.publicContributionPct}% Public",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = deal.dealPitch,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DealStatPill("Cap: %,d".format(deal.newStadiumCapacity), CyanAccent)
                DealStatPill("Sponsor: \$${deal.projectedSponsorRevenueMillions}M/yr", EmeraldAccent)
                DealStatPill("Fee: +\$${deal.relocationFeeMillions}M", GoldPrimary)
            }
        }
    }
}

@Composable
private fun DealStatPill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
