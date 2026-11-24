package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.RivalLeague
import com.example.model.RivalWatchAction
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.VioletAccent

@Composable
fun RivalWatchModal(
    rivalLeague: RivalLeague,
    cashReservesMillions: Long,
    ownerApprovalRating: Int,
    unionHappinessPct: Int,
    freeAgentPoolSize: Int,
    onExecuteAction: (RivalWatchAction) -> Unit,
    onDismiss: () -> Unit
) {
    val threatAccent = Color(rivalLeague.threatColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .heightIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("rival_watch_modal")
                .border(1.5.dp, threatAccent.copy(alpha = 0.7f), RoundedCornerShape(22.dp)),
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
                                .background(threatAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (rivalLeague.isActive) Icons.Default.Visibility else Icons.Default.Shield,
                                contentDescription = "Rival Watch",
                                tint = threatAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RIVAL WATCH WAR ROOM",
                                style = MaterialTheme.typography.labelSmall,
                                color = threatAccent,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.9.sp
                            )
                            Text(
                                text = rivalLeague.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = threatAccent.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = rivalLeague.threatLevelLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = threatAccent,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("rival_status_badge")
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_close_rival_watch_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Rival Watch",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Rival Capital & Hype Meters
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Capital bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RIVAL CAPITAL RESERVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "\$${rivalLeague.capital}M",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (rivalLeague.capital <= 0L) EmeraldAccent else GoldPrimary,
                                        modifier = Modifier.testTag("rival_capital_text")
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (rivalLeague.capital / 150f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (rivalLeague.capital <= 30L) CrimsonAlert else GoldPrimary,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            }

                            // Hype bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MARKET & MEDIA HYPE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${rivalLeague.hype}/100",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (rivalLeague.hype >= 65) CrimsonAlert else CyanAccent,
                                        modifier = Modifier.testTag("rival_hype_text")
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (rivalLeague.hype / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (rivalLeague.hype >= 65) CrimsonAlert else CyanAccent,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            }

                            Text(
                                text = rivalLeague.lastOffseasonPoachSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TradeOffBadge(
                                    text = "Survived: ${rivalLeague.seasonsSurvived}Y",
                                    color = if (rivalLeague.seasonsSurvived >= 3) CrimsonAlert else CyanAccent
                                )
                                if (rivalLeague.permanentTvRevenueBoostMillions > 0L) {
                                    TradeOffBadge(
                                        text = "+\$${rivalLeague.permanentTvRevenueBoostMillions}M/yr TV Locked",
                                        color = EmeraldAccent
                                    )
                                } else if (rivalLeague.isActive && !rivalLeague.hasResolvedAbsorptionEvent) {
                                    TradeOffBadge(
                                        text = "Summit at 3–4Y & 70+ Hype",
                                        color = GoldPrimary
                                    )
                                }
                                if (rivalLeague.absorbedTeamNames.isNotEmpty()) {
                                    TradeOffBadge(
                                        text = "${rivalLeague.absorbedTeamNames.size} Absorbed",
                                        color = GoldPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Commissioner Trade-off Resources Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RivalResourcePill(
                            label = "Comm. Cash",
                            value = "\$${cashReservesMillions}M",
                            accent = GoldPrimary,
                            valueTag = "rival_comm_cash_text",
                            modifier = Modifier.weight(1f)
                        )
                        RivalResourcePill(
                            label = "Owner Approval",
                            value = "${ownerApprovalRating}%",
                            accent = EmeraldAccent,
                            valueTag = "rival_owner_approval_text",
                            modifier = Modifier.weight(1f)
                        )
                        RivalResourcePill(
                            label = "Union Happy",
                            value = "${unionHappinessPct}%",
                            accent = RoyalBlueAccent,
                            valueTag = "rival_union_happiness_text",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Latest Action Feedback Banner
                    Surface(
                        color = threatAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, threatAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rival_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (rivalLeague.isActive) Icons.Default.Warning else Icons.Default.Public,
                                contentDescription = null,
                                tint = threatAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = rivalLeague.lastActionFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Poached Players Roster List
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rival_poached_players_section")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "POACHED PLAYERS (${rivalLeague.poachedPlayers.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (rivalLeague.poachedPlayers.isEmpty()) EmeraldAccent else CrimsonAlert
                                )
                                Text(
                                    text = "FA Pool: $freeAgentPoolSize Players",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("rival_fa_pool_count_text")
                                )
                            }

                            if (rivalLeague.poachedPlayers.isEmpty()) {
                                Text(
                                    text = if (rivalLeague.isActive) {
                                        "No free agents poached yet. Rival scouts attempt to lure 1–3 high-rated stars each off-season."
                                    } else {
                                        "${rivalLeague.name} has folded (\$0M capital). All poached players have returned to your Free-Agent Pool."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                rivalLeague.poachedPlayers.forEach { player ->
                                    val isBlacklisted = player.id in rivalLeague.blacklistedPlayerIds
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("rival_poached_player_${player.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    color = CrimsonAlert.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = player.position,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = CrimsonAlert,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = player.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Age ${player.age} • \$${player.askingSalaryMillions}M Ask${if (isBlacklisted) " • BLACKLISTED" else ""}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isBlacklisted) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = GoldPrimary.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${player.overallRating} OVR",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = GoldPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3 Commissioner Actions
                    Text(
                        text = "COUNTER-MEASURES & STRATEGIC ACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        letterSpacing = 0.8.sp
                    )

                    RivalWatchAction.entries.forEach { action ->
                        RivalWatchActionCard(
                            action = action,
                            isRivalActive = rivalLeague.isActive,
                            rivalCapital = rivalLeague.capital,
                            hasEnoughCash = cashReservesMillions >= action.cashCostMillions,
                            onClick = { onExecuteAction(action) }
                        )
                    }

                    if (rivalLeague.historyLog.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "INTELLIGENCE LOG",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                rivalLeague.historyLog.take(4).forEach { entry ->
                                    Text(
                                        text = "• $entry",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_close_rival_watch"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Rival Watch", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RivalResourcePill(
    label: String,
    value: String,
    accent: Color,
    valueTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = accent,
                modifier = Modifier.testTag(valueTag)
            )
        }
    }
}

@Composable
private fun RivalWatchActionCard(
    action: RivalWatchAction,
    isRivalActive: Boolean,
    rivalCapital: Long,
    hasEnoughCash: Boolean,
    onClick: () -> Unit
) {
    val actionAccent: Color
    val actionIcon: ImageVector
    when (action) {
        RivalWatchAction.COUNTER_PROGRAM -> {
            actionAccent = CyanAccent
            actionIcon = Icons.Default.Tv
        }
        RivalWatchAction.BLACKLIST_DEFECTORS -> {
            actionAccent = CrimsonAlert
            actionIcon = Icons.Default.Gavel
        }
        RivalWatchAction.PROPOSE_MERGER -> {
            actionAccent = VioletAccent
            actionIcon = Icons.Default.Handshake
        }
    }

    val enabled = isRivalActive && hasEnoughCash
    val effectiveDrain = if (action == RivalWatchAction.PROPOSE_MERGER && rivalCapital in 1L..50L) {
        rivalCapital
    } else {
        action.capitalDrainMillions
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) actionAccent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        imageVector = actionIcon,
                        contentDescription = action.title,
                        tint = actionAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = actionAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = action.badgeTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = actionAccent,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Trade-off Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TradeOffBadge(
                    text = if (action.cashCostMillions == 0L) "\$0M Cash" else "-\$${action.cashCostMillions}M Cash",
                    color = if (action.cashCostMillions == 0L) EmeraldAccent else GoldPrimary
                )
                TradeOffBadge(
                    text = "${formatSigned(action.ownerApprovalDelta)} Owner",
                    color = if (action.ownerApprovalDelta >= 0) EmeraldAccent else CrimsonAlert
                )
                TradeOffBadge(
                    text = "${formatSigned(action.unionSatisfactionDelta)} Union",
                    color = if (action.unionSatisfactionDelta >= 0) RoyalBlueAccent else CrimsonAlert
                )
                TradeOffBadge(
                    text = "-\$$effectiveDrain M Rival",
                    color = actionAccent
                )
            }

            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_rival_action_${action.name}")
            ) {
                Text(
                    text = when {
                        !isRivalActive -> "Rival Folded"
                        !hasEnoughCash -> "Insufficient Cash (Need \$${action.cashCostMillions}M)"
                        else -> "Execute ${action.title}"
                    },
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun TradeOffBadge(
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun formatSigned(value: Int): String = if (value >= 0) "+$value" else "$value"
