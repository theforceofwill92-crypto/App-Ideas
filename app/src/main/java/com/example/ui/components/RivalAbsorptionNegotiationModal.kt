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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
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
import com.example.model.RivalAbsorptionNegotiationEvent
import com.example.model.RivalFranchiseCandidate
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary

@Composable
fun RivalAbsorptionNegotiationModal(
    event: RivalAbsorptionNegotiationEvent,
    onToggleCandidate: (String) -> Unit,
    onAcceptAbsorption: () -> Unit,
    onRejectMerger: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* High-stakes summit requires an executive decision */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .heightIn(max = 760.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("rival_absorption_modal")
                .border(1.5.dp, GoldPrimary.copy(alpha = 0.85f), RoundedCornerShape(22.dp)),
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = "High-Stakes Merger Summit",
                                tint = GoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HIGH-STAKES ABSORPTION SUMMIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.9.sp
                            )
                            Text(
                                text = event.rivalLeagueName,
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
                            text = "${event.seasonsSurvived}Y SURVIVED • ${event.rivalHypeSnapshot} HYPE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonAlert,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("absorption_survival_badge")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Narrative Pitch
                    Surface(
                        color = GoldPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = event.headline,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummitStatChip(
                                    label = "Rival Hype",
                                    value = "${event.rivalHypeSnapshot}/100",
                                    accent = CrimsonAlert,
                                    modifier = Modifier.weight(1f)
                                )
                                SummitStatChip(
                                    label = "Rival Reserve",
                                    value = "\$${event.rivalCapitalSnapshot}M",
                                    accent = GoldPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                SummitStatChip(
                                    label = "Select Bounds",
                                    value = "${event.selectedCount}/4 Clubs",
                                    accent = CyanAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Candidate Selector Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOP RIVAL FRANCHISES TO ABSORB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Select 2–4 teams (${event.selectedCount} chosen)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (event.canApproveAbsorption) EmeraldAccent else CrimsonAlert,
                            modifier = Modifier.testTag("absorption_selection_counter")
                        )
                    }

                    // Candidate Cards
                    event.candidateTeams.forEach { candidate ->
                        val lockMinimum = candidate.isSelected && event.selectedCount <= 2
                        AbsorptionCandidateCard(
                            candidate = candidate,
                            lockMinimum = lockMinimum,
                            onClick = { onToggleCandidate(candidate.id) }
                        )
                    }

                    // Live Trade-Off Summary Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("absorption_tradeoff_summary")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "NEGOTIATION TRADE-OFF SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TradeoffMetricBox(
                                    icon = Icons.Default.Tv,
                                    label = "Permanent TV Boost",
                                    value = "+\$${event.totalPermanentTvBoostMillions}M/yr",
                                    accent = EmeraldAccent,
                                    valueTag = "absorption_tv_boost_text",
                                    modifier = Modifier.weight(1f)
                                )
                                TradeoffMetricBox(
                                    icon = Icons.Default.Warning,
                                    label = "Owner Approval Hit",
                                    value = "${event.totalOwnerApprovalHit}%",
                                    accent = CrimsonAlert,
                                    valueTag = "absorption_owner_hit_text",
                                    modifier = Modifier.weight(1f)
                                )
                                TradeoffMetricBox(
                                    icon = Icons.Default.Groups,
                                    label = "New Fan Reach",
                                    value = "+${event.totalNewFanReachMillions}M",
                                    accent = CyanAccent,
                                    valueTag = "absorption_fan_reach_text",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Absorbed clubs immediately join your standings & future TV contracts permanently lock in the +\$${event.totalPermanentTvBoostMillions}M/yr escalator. Rejecting grants +3% Owner Approval but keeps ${event.rivalLeagueName} active.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAcceptAbsorption,
                        enabled = event.canApproveAbsorption,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_absorb_rival_teams")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Absorb ${event.selectedCount} Teams & Lock +\$${event.totalPermanentTvBoostMillions}M/yr TV",
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onRejectMerger,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_reject_rival_absorption")
                    ) {
                        Text(
                            text = "Walk Away & Reject Merger (+3% Owner Approval)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummitStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AbsorptionCandidateCard(
    candidate: RivalFranchiseCandidate,
    lockMinimum: Boolean,
    onClick: () -> Unit
) {
    val teamColor = Color(candidate.colorHex)
    val borderColor = if (candidate.isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val containerColor = if (candidate.isSelected) {
        GoldPrimary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (candidate.isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("absorption_candidate_${candidate.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Team Emblem
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(teamColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = candidate.cityName.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = candidate.teamFullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = GoldPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${candidate.rosterRating} OVR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Star: ${candidate.signatureStarName} • ${candidate.fanBaseMillions}M Fans • +\$${candidate.annualBroadcastContributionMillions}M/yr TV",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (lockMinimum) {
                    Text(
                        text = "Minimum 2 teams required — select another team first to swap.",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = GoldPrimary
                    )
                }
            }

            Icon(
                imageVector = if (candidate.isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (candidate.isSelected) "Selected ${candidate.teamFullName}" else "Select ${candidate.teamFullName}",
                tint = if (candidate.isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TradeoffMetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    valueTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                modifier = Modifier.testTag(valueTag)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
