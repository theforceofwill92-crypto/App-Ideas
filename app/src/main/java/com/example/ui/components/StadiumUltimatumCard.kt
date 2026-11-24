package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Stadium
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
import com.example.model.StadiumUltimatumChoice
import com.example.model.StadiumUltimatumEvent
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RoyalBlueAccent

@Composable
fun StadiumUltimatumCard(
    ultimatum: StadiumUltimatumEvent,
    leagueCashMillions: Long,
    onSelectChoice: (StadiumUltimatumChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val teamAccent = Color(ultimatum.franchiseColorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stadium_ultimatum_card")
            .border(1.5.dp, CrimsonAlert.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(teamAccent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stadium,
                        contentDescription = "Stadium Ultimatum",
                        tint = CrimsonAlert,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STADIUM ULTIMATUM • Y${ultimatum.seasonYear}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = CrimsonAlert,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = ultimatum.franchiseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = CrimsonAlert.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Age ${ultimatum.stadiumAge}y",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = CrimsonAlert,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("stadium_ultimatum_age")
                    )
                }
                Surface(
                    color = GoldPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${ultimatum.stadiumCondition}% Cond",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("stadium_ultimatum_condition")
                    )
                }
            }

            Text(
                text = ultimatum.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 3 Action Choices
            StadiumUltimatumChoice.entries.forEach { choice ->
                val accent = when (choice) {
                    StadiumUltimatumChoice.LOBBY_PUBLIC_TAX_FUNDING -> CyanAccent
                    StadiumUltimatumChoice.FORCE_OWNER_PRIVATE_FUNDING -> CrimsonAlert
                    StadiumUltimatumChoice.SUBSIDIZE_WITH_LEAGUE_FUNDS -> GoldPrimary
                }
                val icon = when (choice) {
                    StadiumUltimatumChoice.LOBBY_PUBLIC_TAX_FUNDING -> Icons.Default.LocationCity
                    StadiumUltimatumChoice.FORCE_OWNER_PRIVATE_FUNDING -> Icons.Default.Gavel
                    StadiumUltimatumChoice.SUBSIDIZE_WITH_LEAGUE_FUNDS -> Icons.Default.AccountBalance
                }
                val canAfford = leagueCashMillions >= choice.leagueCashCostMillions

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                Icon(
                                    imageVector = icon,
                                    contentDescription = choice.title,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = choice.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                color = accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = choice.badgeTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = choice.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UltimatumChip(
                                text = "${if (choice.ownerApprovalDelta >= 0) "+" else ""}${choice.ownerApprovalDelta} Owner",
                                color = if (choice.ownerApprovalDelta >= 0) EmeraldAccent else CrimsonAlert
                            )
                            UltimatumChip(
                                text = "${if (choice.fanSentimentDelta >= 0) "+" else ""}${choice.fanSentimentDelta} Fans",
                                color = if (choice.fanSentimentDelta >= 0) RoyalBlueAccent else CrimsonAlert
                            )
                            if (choice.leagueCashCostMillions > 0L) {
                                UltimatumChip(
                                    text = "-\$${choice.leagueCashCostMillions}M Cash",
                                    color = GoldPrimary
                                )
                            }
                            if (choice.relocationThreatProbabilityPct > 0) {
                                UltimatumChip(
                                    text = "${choice.relocationThreatProbabilityPct}% Reloc Threat",
                                    color = CrimsonAlert
                                )
                            } else {
                                UltimatumChip(
                                    text = "New Stadium",
                                    color = EmeraldAccent
                                )
                            }
                        }

                        Button(
                            onClick = { onSelectChoice(choice) },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_ultimatum_${choice.name}")
                        ) {
                            Text(
                                text = if (!canAfford) {
                                    "Need \$${choice.leagueCashCostMillions}M League Cash"
                                } else {
                                    choice.title
                                },
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UltimatumChip(
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
