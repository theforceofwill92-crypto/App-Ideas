package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllStarRedesignScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler {
        viewModel.openAllStarRedesignScreen(false)
    }

    val redesign = uiState.allStarRedesignState
    val impact = remember(
        redesign,
        uiState.franchises,
        uiState.activeBroadcast,
        uiState.cities,
        uiState.ruleBook
    ) {
        AllStarEngine.computeProjectedImpact(
            redesignState = redesign,
            franchises = uiState.franchises,
            activeBroadcast = uiState.activeBroadcast,
            cities = uiState.cities,
            ruleBook = uiState.ruleBook
        )
    }

    var actionFeedback by remember { mutableStateOf<String?>(null) }
    val hostCandidates = remember(uiState.cities) {
        val franchiseCities = uiState.cities.filter { it.hasFranchise }
        val internationalCities = uiState.cities.filter { it.isInternational && !it.hasFranchise }.take(3)
        (franchiseCities + internationalCities).distinctBy { it.id }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("all_star_redesign_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MID-SEASON SPECTACLE LAB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "All-Star Festivities Redesign",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openAllStarRedesignScreen(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_all_star")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("all_star_redesign_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Live Broadcast Viewership & Player Apathy Telemetry Hero Card
            item {
                val effortColor = when {
                    impact.playerEffortScore >= 80 -> EmeraldAccent
                    impact.playerEffortScore >= 60 -> GoldPrimary
                    else -> CrimsonAlert
                }
                val viewColor = when {
                    impact.projectedViewershipIndex >= 80 -> CyanAccent
                    impact.projectedViewershipIndex >= 60 -> GoldPrimary
                    else -> CrimsonAlert
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, viewColor.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .testTag("card_all_star_telemetry"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        viewColor.copy(alpha = 0.12f),
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ACTIVE: ${redesign.selectedFormat.displayName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = viewColor,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Host: ${redesign.hostCityName} • Week 7 Showcase",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = viewColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, viewColor)
                            ) {
                                Text(
                                    text = "TV Grade ${impact.tvRatingsGrade}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = viewColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Three Live Telemetry KPI Tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelemetryTile(
                                label = "VIEWERSHIP",
                                value = "${impact.projectedViewersMillions}M",
                                delta = "${if (impact.viewershipDeltaPct >= 0) "+" else ""}${impact.viewershipDeltaPct}% vs Legacy",
                                accent = viewColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("metric_all_star_viewership")
                            )
                            TelemetryTile(
                                label = "PLAYER EFFORT",
                                value = "${impact.playerEffortScore}/100",
                                delta = "${impact.playerApathyRiskPct}% Apathy Risk",
                                accent = effortColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("metric_all_star_effort")
                            )
                            TelemetryTile(
                                label = "NET REVENUE",
                                value = "\$${impact.projectedNetRevenueMillions}M",
                                delta = "+${impact.projectedFanbaseGainMillions}M Fans",
                                accent = EmeraldAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("metric_all_star_revenue")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Bars for Broadcast Reach & Competitive Effort
                        Text(
                            text = "Player Competitiveness vs. Apathy (${impact.playerEffortScore}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (impact.playerEffortScore / 100f).coerceIn(0.05f, 1f) },
                            color = effortColor,
                            trackColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Apathy & Ratings Diagnosis Strip
                        Surface(
                            color = effortColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, effortColor.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                                .testTag("banner_apathy_diagnosis")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (impact.playerEffortScore >= 68) Icons.Default.Verified else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = effortColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = impact.apathyDiagnosis,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Real-World Format Redesign Selector
            item {
                SectionHeader(title = "Real-World All-Star Formats")
            }

            items(AllStarFormatType.values(), key = { it.name }) { format ->
                val isSelected = redesign.selectedFormat == format
                val accent = Color(format.accentHex)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.selectAllStarFormat(format) }
                        .testTag("format_card_${format.name}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = accent.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = format.formatTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = accent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = format.realWorldInspiration,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = format.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    color = accent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = format.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FormatBadge("TV ${format.baseViewershipIndex}", CyanAccent)
                            FormatBadge("Effort ${format.basePlayerEffortScore}", EmeraldAccent)
                            FormatBadge("Injury: ${format.injuryRiskLabel}", GoldPrimary)
                        }
                    }
                }
            }

            // 3. Anti-Apathy Stakes & Broadcast Incentives
            item {
                SectionHeader(title = "Anti-Apathy Stakes & Incentives")
            }

            items(redesign.incentives, key = { it.id }) { incentive ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (incentive.isEnabled) GoldPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("incentive_card_${incentive.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = incentive.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = incentive.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TV ${if (incentive.viewershipDelta >= 0) "+" else ""}${incentive.viewershipDelta} • Effort +${incentive.effortDelta} • Cost \$${incentive.costMillions}M",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = incentive.isEnabled,
                            onCheckedChange = { viewModel.toggleAllStarIncentive(incentive.id) },
                            modifier = Modifier.testTag("incentive_switch_${incentive.id}")
                        )
                    }
                }
            }

            // 4. Weekend Skills & Festivities Lineup
            item {
                SectionHeader(title = "Weekend Skills & Showcase Lineup")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    redesign.sideEvents.forEach { ev ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleAllStarSideEvent(ev.id) }
                                .border(
                                    1.dp,
                                    if (ev.isSelected) CyanAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("event_card_${ev.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ev.isSelected) CyanAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ev.category.uppercase()} • ${ev.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ev.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "+${ev.viewershipBonus} TV • +${ev.effortBonus} Effort • \$${ev.costMillions}M",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Checkbox(
                                    checked = ev.isSelected,
                                    onCheckedChange = { viewModel.toggleAllStarSideEvent(ev.id) },
                                    modifier = Modifier.testTag("event_checkbox_${ev.id}")
                                )
                            }
                        }
                    }
                }
            }

            // 5. Host City Selection
            item {
                SectionHeader(title = "Showcase Host City")
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("row_host_cities")
                ) {
                    items(hostCandidates, key = { it.id }) { city ->
                        val isHost = redesign.hostCityId == city.id
                        FilterChip(
                            selected = isHost,
                            onClick = { viewModel.selectAllStarHostCity(city.id) },
                            label = {
                                Text(
                                    text = if (city.isInternational) "${city.name} (Global)" else city.name,
                                    fontWeight = if (isHost) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("host_city_chip_${city.id}")
                        )
                    }
                }
            }

            // 6. Commissioner Launch & Simulation Actions
            item {
                val isAllStarWeek = uiState.seasonPhase == SeasonPhase.ALL_STAR_INTERNATIONAL
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val res = viewModel.ratifyAndPromoteAllStarRedesign()
                                actionFeedback = "Promoted ${redesign.selectedFormat.shortLabel} in ${redesign.hostCityName} (${res.projectedViewersMillions}M proj. viewers)."
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("btn_ratify_all_star_format")
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (redesign.isRedesignedAndPromoted) "Promoted" else "Lock In Redesign",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val recap = viewModel.hostLiveAllStarShowcase()
                                actionFeedback = recap.headline
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("btn_host_all_star_showcase")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAllStarWeek) "Host Week 7 Showcase" else "Simulate Showcase",
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    AnimatedVisibility(visible = actionFeedback != null) {
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                                .testTag("banner_all_star_feedback")
                        ) {
                            Text(
                                text = actionFeedback ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // 7. Showcase Box Score & Ratings Recap Card (when available)
            if (redesign.lastRecap != null) {
                item {
                    val recap = redesign.lastRecap
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp))
                            .testTag("card_all_star_recap"),
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
                                    text = if (recap.isDressRehearsal) "SHOWCASE SIMULATION PREVIEW" else "OFFICIAL ALL-STAR BOX RECAP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    letterSpacing = 0.8.sp
                                )
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Effort ${recap.playerEffortGrade} • ${recap.actualViewersMillions}M TV",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = recap.matchupScoreSummary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "MVP: ${recap.mvpPlayerName} (${recap.mvpTeamName})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Text(
                                text = "${recap.mvpStatLine} • +\$${recap.netRevenueMillions}M Net Treasury",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    delta: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = delta,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FormatBadge(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
