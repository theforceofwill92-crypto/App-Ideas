package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CommissionerChoice
import com.example.model.CommissionerDilemma
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.VioletAccent

@Composable
fun DilemmaDialog(
    dilemma: CommissionerDilemma,
    onChoiceSelected: (Int) -> Unit
) {
    val archetypeColor = Color(dilemma.archetype.accentHex)

    Dialog(
        onDismissRequest = { /* Force commissioner ruling */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = 360.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("dilemma_dialog_card")
                .border(2.dp, CrimsonAlert, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Urgency Header + Stage Badge
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrimsonAlert.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Crisis Alert",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dilemma.urgencyTag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimsonAlert,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = archetypeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("dilemma_stage_badge")
                        ) {
                            Text(
                                text = "STAGE ${dilemma.stageNumber}/${dilemma.totalStages}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = archetypeColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = dilemma.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                // Causal Chain Callout (if triggered by an earlier decision)
                if (dilemma.isBranchFollowUp && !dilemma.priorDecisionLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = VioletAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dilemma_prior_decision_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                contentDescription = "Triggered Branch",
                                tint = VioletAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "FALLOUT FROM PRIOR RULING: ${dilemma.priorDecisionLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VioletAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = dilemma.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${dilemma.archetype.displayTitle} • ${dilemma.category} • Week ${dilemma.week}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )

                val stakeholders = listOfNotNull(
                    dilemma.targetFranchiseName?.takeIf { it.isNotBlank() }?.let { "Club: $it" },
                    dilemma.targetPlayerName?.takeIf { it.isNotBlank() }?.let { "Player: $it" }
                )
                if (stakeholders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stakeholders.joinToString("  •  "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = dilemma.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CHOOSE COMMISSIONER DECREE:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                dilemma.choices.forEachIndexed { index, choice ->
                    val pills = buildConsequencePills(choice)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("dilemma_choice_$index")
                            .clickable { onChoiceSelected(index) }
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${choice.label}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = choice.consequenceSummary.ifBlank { choice.narrativeOutcome },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (pills.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    pills.chunked(3).forEach { rowPills ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowPills.forEach { (label, pillColor) ->
                                                Surface(
                                                    color = pillColor.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pillColor,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            choice.branchFollowUp?.let { branch ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldPrimary.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                        contentDescription = "Spawns Branch",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Branches: ${branch.teaserLabel} (+${branch.delayWeeks}w)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }
                            } ?: Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Closes Arc",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Concludes Crisis Arc",
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
        }
    }
}

private fun buildConsequencePills(choice: CommissionerChoice): List<Pair<String, Color>> {
    val list = mutableListOf<Pair<String, Color>>()
    if (choice.cashImpactMillions != 0L) {
        val p = if (choice.cashImpactMillions > 0) "+" else ""
        list.add("$p\$${choice.cashImpactMillions}M Cash" to if (choice.cashImpactMillions > 0) EmeraldAccent else CrimsonAlert)
    }
    if (choice.valuationImpactMillions != 0L) {
        val p = if (choice.valuationImpactMillions > 0) "+" else ""
        list.add("$p\$${choice.valuationImpactMillions}M Val" to if (choice.valuationImpactMillions > 0) EmeraldAccent else CrimsonAlert)
    }
    if (choice.fanbaseImpactMillions != 0.0) {
        val p = if (choice.fanbaseImpactMillions > 0) "+" else ""
        list.add("$p${choice.fanbaseImpactMillions}M Fans" to if (choice.fanbaseImpactMillions > 0) CyanAccent else CrimsonAlert)
    }
    if (choice.integrityImpact != 0) {
        val p = if (choice.integrityImpact > 0) "+" else ""
        list.add("$p${choice.integrityImpact} Integrity" to if (choice.integrityImpact > 0) EmeraldAccent else CrimsonAlert)
    }
    if (choice.ownerApprovalImpact != 0) {
        val p = if (choice.ownerApprovalImpact > 0) "+" else ""
        list.add("$p${choice.ownerApprovalImpact} Owners" to if (choice.ownerApprovalImpact > 0) EmeraldAccent else CrimsonAlert)
    }
    if (choice.unionSatisfactionImpact != 0) {
        val p = if (choice.unionSatisfactionImpact > 0) "+" else ""
        list.add("$p${choice.unionSatisfactionImpact} Union" to if (choice.unionSatisfactionImpact > 0) EmeraldAccent else CrimsonAlert)
    }
    if (choice.fanMoraleImpact != 0) {
        val p = if (choice.fanMoraleImpact > 0) "+" else ""
        list.add("$p${choice.fanMoraleImpact} Morale" to if (choice.fanMoraleImpact > 0) CyanAccent else CrimsonAlert)
    }
    if (choice.playerSuspensionGames > 0) {
        list.add("${choice.playerSuspensionGames}G Susp" to GoldPrimary)
    }
    if (choice.officiatingAccuracyImpact != 0) {
        val p = if (choice.officiatingAccuracyImpact > 0) "+" else ""
        list.add("$p${choice.officiatingAccuracyImpact} Ref Acc" to if (choice.officiatingAccuracyImpact > 0) CyanAccent else CrimsonAlert)
    }
    if (choice.refLockoutRiskImpact != 0) {
        val p = if (choice.refLockoutRiskImpact > 0) "+" else ""
        list.add("$p${choice.refLockoutRiskImpact} Ref Risk" to if (choice.refLockoutRiskImpact < 0) EmeraldAccent else CrimsonAlert)
    }
    return list
}
