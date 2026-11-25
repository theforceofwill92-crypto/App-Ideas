package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.LaborNegotiationChoice
import com.example.model.LaborNegotiationOverlaySession
import com.example.model.LaborTriggerSeverity
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Full-screen Composable overlay triggered during labor union negotiations,
 * presenting executive choices along with projected financial, fan sentiment,
 * and strike-risk impacts.
 */
@Composable
fun LaborUnionNegotiationOverlay(
    session: LaborNegotiationOverlaySession,
    onSelectChoice: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenCbaTable: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val severityColor = when (session.severity) {
        LaborTriggerSeverity.CRITICAL -> CrimsonAlert
        LaborTriggerSeverity.WARNING -> GoldPrimary
        LaborTriggerSeverity.INFO -> CyanAccent
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("labor_negotiation_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Block background pass-through while keeping explicit actions in charge */ }
                )
        )
        Card(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .border(1.5.dp, severityColor.copy(alpha = 0.85f), RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Gradient Urgency Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    severityColor.copy(alpha = 0.24f),
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    GoldPrimary.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(severityColor.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Handshake,
                                        contentDescription = "Labor Union Negotiation",
                                        tint = severityColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "LABOR NEGOTIATION • ${session.severity.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.9.sp,
                                        color = severityColor
                                    )
                                    Text(
                                        text = session.triggerType.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_dismiss_labor_overlay")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Defer Negotiation Overlay",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = session.headline,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("labor_overlay_headline")
                        )

                        Text(
                            text = session.subheadline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        // Baseline Executive Context Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("labor_overlay_baseline"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BaselinePill(
                                label = "Cash",
                                value = "\$${session.currentCashMillions}M",
                                color = EmeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            BaselinePill(
                                label = "Team Morale",
                                value = "${session.currentTeamMoralePct}%",
                                color = GoldPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("labor_overlay_team_morale_baseline")
                            )
                            BaselinePill(
                                label = "Fans",
                                value = "${session.currentFanSentimentPct}%",
                                color = CyanAccent,
                                modifier = Modifier.weight(1f)
                            )
                            BaselinePill(
                                label = "Strike",
                                value = "${session.currentStrikeRiskPct}%",
                                color = if (session.currentStrikeRiskPct >= 40) CrimsonAlert else GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Union Representative Quote Banner
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Union Representative Statement",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "\"${session.unionRepQuote}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 17.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "— ${session.unionRepName}, ${session.unionRepTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "SELECT NEGOTIATION RESPONSE & PROJECTED IMPACT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    session.choices.forEachIndexed { index, choice ->
                        LaborNegotiationChoiceCard(
                            index = index,
                            choice = choice,
                            baselineFanSentimentPct = session.currentFanSentimentPct,
                            baselineTeamMoralePct = session.currentTeamMoralePct,
                            onSelect = { onSelectChoice(choice.id) }
                        )
                    }

                    // Bottom Defer / Open Full CBA Table Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("btn_defer_labor_overlay")
                        ) {
                            Text(
                                text = "Defer",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (onOpenCbaTable != null) {
                            Button(
                                onClick = onOpenCbaTable,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_open_cba_table_from_overlay")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Open Full CBA Table",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Full CBA Table",
                                    color = Color.Black,
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
}

@Composable
private fun BaselinePill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LaborNegotiationChoiceCard(
    index: Int,
    choice: LaborNegotiationChoice,
    baselineFanSentimentPct: Int,
    baselineTeamMoralePct: Int = 78,
    onSelect: () -> Unit
) {
    val isHardline = choice.projectedStrikeRiskDelta > 0
    val stanceColor = when {
        isHardline -> CrimsonAlert
        choice.projectedRevenueShareDeltaPct > 0 || choice.projectedCbaYearsSet == 5 -> EmeraldAccent
        else -> GoldPrimary
    }

    val cashText = buildString {
        val sign = if (choice.projectedCashImpactMillions >= 0L) "+" else "-"
        append("$sign\$${abs(choice.projectedCashImpactMillions)}M Cash")
        if (choice.projectedWeeklyNetImpactMillions != 0L) {
            val wkSign = if (choice.projectedWeeklyNetImpactMillions > 0L) "+" else "-"
            append(" • $wkSign\$${abs(choice.projectedWeeklyNetImpactMillions)}M/wk")
        }
        if (choice.projectedValuationImpactMillions != 0L) {
            val valSign = if (choice.projectedValuationImpactMillions > 0L) "+" else "-"
            append(" • $valSign\$${abs(choice.projectedValuationImpactMillions)}M Val")
        }
    }
    val financialColor = when {
        choice.projectedCashImpactMillions < 0L && choice.projectedValuationImpactMillions <= 0L -> CrimsonAlert
        choice.projectedCashImpactMillions < 0L -> AmberWarning
        choice.projectedValuationImpactMillions < 0L -> AmberWarning
        else -> EmeraldAccent
    }

    val weightedSentimentDelta = ((choice.projectedFanMoraleDelta * 0.55) + (choice.projectedFanLoyaltyDelta * 0.45)).roundToInt()
    val projectedSentiment = (baselineFanSentimentPct + weightedSentimentDelta).coerceIn(0, 100)
    val sentimentSign = if (weightedSentimentDelta >= 0) "+" else ""
    val sentimentText = "${sentimentSign}${weightedSentimentDelta}% Sentiment ($baselineFanSentimentPct% → $projectedSentiment%)"
    val sentimentColor = if (weightedSentimentDelta >= 0) CyanAccent else CrimsonAlert

    val strikeSign = if (choice.projectedStrikeRiskDelta > 0) "+" else ""
    val strikeText = "${strikeSign}${choice.projectedStrikeRiskDelta}% Strike Risk"
    val strikeColor = if (choice.projectedStrikeRiskDelta <= 0) EmeraldAccent else CrimsonAlert

    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("labor_overlay_choice_$index")
            .border(1.dp, stanceColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = stanceColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = choice.stanceTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = stanceColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = strikeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = strikeColor
                )
            }

            Text(
                text = "${index + 1}. ${choice.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = choice.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            if (choice.requestedSalaryCapMillions != null ||
                choice.requestedRosterSizeRule != null ||
                choice.clearsLockout ||
                choice.activatesLockoutOnReject
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    choice.requestedSalaryCapMillions?.let { capM ->
                        Surface(
                            color = GoldPrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Cap \$${capM}M",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    choice.requestedRosterSizeRule?.let { rosterRule ->
                        Surface(
                            color = CyanAccent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = rosterRule.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (choice.clearsLockout) {
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Lifts Lockout",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (choice.activatesLockoutOnReject) {
                        Surface(
                            color = CrimsonAlert.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Lockout Risk",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = CrimsonAlert,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Projected Financial, Team Morale & Salary Structure Impacts
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (choice.projectedTeamMoraleDelta != 0) {
                    val tmSign = if (choice.projectedTeamMoraleDelta > 0) "+" else ""
                    val tmAfter = (baselineTeamMoralePct + choice.projectedTeamMoraleDelta).coerceIn(10, 99)
                    val tmColor = if (choice.projectedTeamMoraleDelta > 0) EmeraldAccent else CrimsonAlert
                    ImpactProjectionChip(
                        icon = Icons.Default.Groups,
                        label = "Team Morale",
                        value = "$tmSign${choice.projectedTeamMoraleDelta}% ($baselineTeamMoralePct% → $tmAfter%)",
                        accent = tmColor,
                        testTag = "labor_overlay_team_morale_$index"
                    )
                }

                val structureParts = buildList {
                    choice.requestedSalaryCapMillions?.let { add("Cap \$${it}M") }
                    if (choice.requestedLuxuryTaxThresholdDelta != 0L) {
                        val s = if (choice.requestedLuxuryTaxThresholdDelta > 0) "+" else ""
                        add("Lux Tax $s\$${choice.requestedLuxuryTaxThresholdDelta}M")
                    }
                    if (choice.requestedRookieScaleDeltaPct != 0) {
                        val s = if (choice.requestedRookieScaleDeltaPct > 0) "+" else ""
                        add("Rookie Scale $s${choice.requestedRookieScaleDeltaPct}%")
                    }
                    if (choice.requestedMaxContractCapDeltaPct != 0) {
                        val s = if (choice.requestedMaxContractCapDeltaPct > 0) "+" else ""
                        add("Max Tier $s${choice.requestedMaxContractCapDeltaPct}%")
                    }
                    if (choice.requestedMinSalaryDeltaK != 0L) {
                        val s = if (choice.requestedMinSalaryDeltaK > 0) "+" else ""
                        add("Min $s\$${choice.requestedMinSalaryDeltaK}K")
                    }
                    if (choice.projectedRevenueShareDeltaPct != 0) {
                        val s = if (choice.projectedRevenueShareDeltaPct > 0) "+" else ""
                        add("Rev Split $s${choice.projectedRevenueShareDeltaPct}%")
                    }
                    choice.requestedSalaryCapType?.let { add(it.displayName) }
                }
                if (structureParts.isNotEmpty()) {
                    ImpactProjectionChip(
                        icon = Icons.Default.Tune,
                        label = "Salary Structure",
                        value = structureParts.joinToString(" • "),
                        accent = GoldPrimary,
                        testTag = "labor_overlay_salary_structure_$index"
                    )
                }

                ImpactProjectionChip(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Financial",
                    value = cashText,
                    accent = financialColor,
                    testTag = "labor_overlay_financial_$index"
                )
                ImpactProjectionChip(
                    icon = Icons.Default.Favorite,
                    label = "Fan Sentiment",
                    value = sentimentText,
                    accent = sentimentColor,
                    testTag = "labor_overlay_sentiment_$index"
                )
            }
        }
    }
}

@Composable
private fun ImpactProjectionChip(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    testTag: String
) {
    Surface(
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
