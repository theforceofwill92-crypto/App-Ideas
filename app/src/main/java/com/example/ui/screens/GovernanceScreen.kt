package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DraftParitySystem
import com.example.model.PolicyStrictness
import com.example.model.SalaryCapType
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@Composable
fun GovernanceScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    val union = uiState.unionAgreement
    val rules = uiState.ruleBook

    var revenueShareSlider by remember(union.playerRevenueSharePct) { mutableStateOf(union.playerRevenueSharePct.toFloat()) }
    var selectedCapType by remember(union.salaryCapType) { mutableStateOf(union.salaryCapType) }
    var selectedAntiDoping by remember(rules.antiDopingPolicy) { mutableStateOf(rules.antiDopingPolicy) }
    var selectedTampering by remember(rules.antiTamperingGamblingPolicy) { mutableStateOf(rules.antiTamperingGamblingPolicy) }
    var selectedParity by remember(rules.draftParity) { mutableStateOf(rules.draftParity) }
    var fastPaceToggle by remember(rules.shotClockFastPace) { mutableStateOf(rules.shotClockFastPace) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("governance_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "COMMISSIONER GOVERNANCE",
                style = MaterialTheme.typography.labelSmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Labor Unions & Rulebook",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Negotiate Collective Bargaining Agreements with the Players Union, avert strikes, enforce integrity rules, and maintain competitive balance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Labor Union Summit Entry Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(2.dp, if (union.strikeRisk > 30) CrimsonAlert else EmeraldAccent, RoundedCornerShape(16.dp))
                    .clickable { viewModel.openLaborUnionScreen(true) }
                    .testTag("btn_open_union_summit_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = (if (union.strikeRisk > 30) CrimsonAlert else EmeraldAccent).copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (union.strikeRisk > 30) CrimsonAlert else EmeraldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LABOR UNION SUMMIT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (union.strikeRisk > 30) CrimsonAlert else EmeraldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Address Player Demands & Avert Strike",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Negotiate salaries, concussion safety protocols, and private jet travel conditions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.openLaborUnionScreen(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (union.strikeRisk > 30) CrimsonAlert else GoldPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_enter_union_summit")
                    ) {
                        Text(
                            text = if (union.strikeRisk > 30) "Avert Strike" else "Negotiate",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Disciplinary Desk Entry Card
        item {
            val discState = uiState.disciplinaryState
            val pendingCount = discState.pendingCases.size
            val appealsCount = discState.appealedCases.size
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.5.dp, CyanAccent.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.openDisciplinaryDesk(true) }
                    .testTag("card_disciplinary_governance_entry"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Balance,
                            contentDescription = "Disciplinary Desk",
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DISCIPLINARY DESK & PRECEDENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Suspensions, Fines & Arbitration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "$pendingCount Pending Hearings • $appealsCount Union Appeals • ${discState.precedentConsistencyScore}% Consistency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.openDisciplinaryDesk(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_open_disciplinary_from_governance")
                    ) {
                        Text(
                            text = "Docket",
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Labor Union CBA Relations Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("union_cba_card")
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PLAYERS UNION (CBA ACCORD)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = if (union.strikeRisk > 40) CrimsonAlert.copy(alpha = 0.15f) else EmeraldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "STRIKE RISK: ${union.strikeRisk}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (union.strikeRisk > 40) CrimsonAlert else EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ProgressBarWithLabel(
                        label = "Union Satisfaction Rating",
                        progress = union.unionSatisfaction / 100f,
                        displayValue = "${union.unionSatisfaction}/100",
                        color = if (union.unionSatisfaction >= 70) EmeraldAccent else GoldPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Player Revenue Share Split: ${revenueShareSlider.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = revenueShareSlider,
                        onValueChange = { revenueShareSlider = it },
                        valueRange = 46f..54f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Salary Cap Structure:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    SalaryCapType.values().forEach { capType ->
                        val isSel = selectedCapType == capType
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedCapType = capType }
                                .border(1.dp, if (isSel) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = if (isSel) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSel, onClick = { selectedCapType = capType })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = capType.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.updateUnionCBA(
                                revenueSharePct = revenueShareSlider.toInt(),
                                salaryCapType = selectedCapType,
                                salaryCapMillions = 80L,
                                healthFundMillions = 18L
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_ratify_cba")
                    ) {
                        Text("Ratify CBA Agreement with Union", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Integrity & Anti-Doping Governance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("integrity_rules_card")
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LEAGUE INTEGRITY & FAIR PLAY CODEX",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Strict enforcement raises integrity against scandals, while lenient policies keep stars on the field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ANTI-DOPING ENFORCEMENT LEVEL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    PolicyStrictness.values().forEach { policy ->
                        val isSel = selectedAntiDoping == policy
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedAntiDoping = policy },
                            label = { Text("${policy.title} (${if (policy.integrityModifier >= 0) "+" else ""}${policy.integrityModifier} Integrity)") },
                            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ANTI-TAMPERING & MATCH-FIXING ZERO TOLERANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    PolicyStrictness.values().forEach { policy ->
                        val isSel = selectedTampering == policy
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedTampering = policy },
                            label = { Text(policy.title) },
                            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("DRAFT LOTTERY & COMPETITIVE PARITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    DraftParitySystem.values().forEach { parity ->
                        val isSel = selectedParity == parity
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedParity = parity },
                            label = { Text(parity.title) },
                            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fast-Pace Shot Clock / Action Rules", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = fastPaceToggle,
                            onCheckedChange = { fastPaceToggle = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.updateRuleBook(
                                antiDoping = selectedAntiDoping,
                                antiTampering = selectedTampering,
                                paritySystem = selectedParity,
                                shotClockFastPace = fastPaceToggle
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_enact_rules")
                    ) {
                        Text("Enact Official League Bylaws", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
