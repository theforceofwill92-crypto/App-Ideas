package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityHubScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.closeFacilityHub() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedTeam = remember(uiState.franchises, uiState.selectedFacilityFranchiseId) {
        uiState.franchises.find { it.id == uiState.selectedFacilityFranchiseId }
            ?: uiState.franchises.firstOrNull()
    }

    val leagueAvgValuation = remember(uiState.franchises, uiState.activeBroadcast) {
        if (uiState.franchises.isEmpty()) 0L
        else uiState.franchises.sumOf {
            it.calculateValuationBreakdown(uiState.activeBroadcast, uiState.franchises.size).totalValuationMillions
        } / uiState.franchises.size
    }
    val totalLeagueSeats = remember(uiState.franchises) {
        uiState.franchises.sumOf { it.stadiumCapacity }
    }
    val totalActiveProjects = remember(uiState.franchises) {
        uiState.franchises.sumOf { it.facilities.ongoingProjects.size }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("facility_hub_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stadiums & Facilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Reserves: \$${uiState.cashReservesMillions}M • ${totalActiveProjects} Active Build(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeFacilityHub() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_close_facility_hub")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to League Desk"
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
                .testTag("facility_hub_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // League Infrastructure Overview Strip
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LEAGUE INFRASTRUCTURE INDEX",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "%,d Total League Seats".format(totalLeagueSeats),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Avg Club Valuation: \$${leagueAvgValuation}M • Treasury: \$${uiState.cashReservesMillions}M",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stadium,
                                contentDescription = "League Arenas",
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Club Selector Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT FRANCHISE CAMPUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("facility_club_selector")
                    ) {
                        items(uiState.franchises, key = { it.id }) { team ->
                            val isSelected = team.id == selectedTeam?.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectFacilityFranchise(team.id) },
                                label = {
                                    Text(
                                        text = "${team.cityName} (${team.facilities.averageTier}★)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(team.colorHex))
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("facility_club_chip_${team.id}")
                            )
                        }
                    }
                }
            }

            if (selectedTeam != null) {
                val valuation = selectedTeam.calculateValuationBreakdown(uiState.activeBroadcast, uiState.franchises.size)
                val stmt = selectedTeam.calculateFinancialStatement(uiState.activeBroadcast, uiState.franchises.size)
                val fac = selectedTeam.facilities

                // Selected Franchise Valuation & Infrastructure Hero Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_franchise_valuation_${selectedTeam.id}")
                            .border(1.5.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(selectedTeam.colorHex).copy(alpha = 0.25f),
                                            GoldPrimary.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${selectedTeam.teamFullName} • ${selectedTeam.alignmentLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldPrimary,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "\$${valuation.totalValuationMillions}M Valuation",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "%,d Seats • Age ${selectedTeam.stadiumAge}y • ${selectedTeam.stadiumCondition}% Cond • \$${fac.cumulativeInvestedMillions}M Invested".format(selectedTeam.stadiumCapacity),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (selectedTeam.stadiumAge > 25 && selectedTeam.stadiumCondition < 50) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        color = EmeraldAccent.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.6f))
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${fac.averageTier} / 5.0",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black,
                                                color = EmeraldAccent
                                            )
                                            Text(
                                                text = "AVG TIER",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = EmeraldAccent
                                            )
                                        }
                                    }
                                }

                                // Valuation Breakdown Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ValuationMiniPill(
                                        label = "Market & Brand",
                                        value = "\$${valuation.marketAndBrandMillions}M",
                                        color = CyanAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ValuationMiniPill(
                                        label = "Rev Multiple",
                                        value = "\$${valuation.commercialMultipleMillions}M",
                                        color = EmeraldAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ValuationMiniPill(
                                        label = "Arena Assets",
                                        value = "\$${valuation.facilityAssetMillions}M",
                                        color = GoldPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Live Operational Impact Strip
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "+${fac.conditioningOvrBonus} OVR Cond.",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                        Text(
                                            text = "-${fac.injuryRiskReductionPct}% Injury Risk",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent
                                        )
                                        Text(
                                            text = "+\$${stmt.luxurySuiteRevenueMillions}M Suites",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Construction Pipeline (if any)
                if (fac.ongoingProjects.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Active Construction Pipeline (${fac.ongoingProjects.size})")
                    }
                    items(fac.ongoingProjects, key = { it.id }) { proj ->
                        val completedWeeks = (proj.totalWeeks - proj.remainingWeeks).coerceAtLeast(0)
                        val progress = (completedWeeks.toFloat() / proj.totalWeeks.coerceAtLeast(1)).coerceIn(0.08f, 1f)
                        val expediteCost = (proj.remainingWeeks * 4L).coerceAtLeast(3L)
                        val accent = Color(proj.type.badgeColorHex)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_ongoing_project_${proj.id}")
                                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${proj.type.shortName.uppercase()} • TIER ${proj.targetTier} BUILD",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = accent
                                        )
                                        Text(
                                            text = proj.projectName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        color = accent.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${proj.remainingWeeks}W Left",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = accent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = accent,
                                    trackColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val rewardParts = buildList {
                                        if (proj.seatingCapacityGain > 0) add("+${proj.seatingCapacityGain} Seats")
                                        if (proj.annualSponsorGainMillions > 0L) add("+\$${proj.annualSponsorGainMillions}M/yr")
                                        if (proj.rosterOvrGain > 0) add("+${proj.rosterOvrGain} OVR")
                                        add("+\$${proj.valuationBoostMillions}M Val")
                                    }.joinToString(" • ")

                                    Text(
                                        text = rewardParts,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            val (_, msg) = viewModel.expediteFacilityProject(selectedTeam.id, proj.id)
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        },
                                        enabled = uiState.cashReservesMillions >= expediteCost,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("btn_expedite_facility_${proj.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FastForward,
                                            contentDescription = "Expedite Project",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Expedite (\$${expediteCost}M)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Upgradeable Facility Tracks
                item {
                    SectionHeader(title = "Infrastructure Investment Tracks")
                }

                items(FacilityType.values().toList(), key = { it.name }) { facilityType ->
                    val currentTier = fac.tierOf(facilityType)
                    val nextSpec = FacilityEngine.getNextUpgradeSpec(fac, facilityType)
                    val ongoing = fac.ongoingProjects.find { it.type == facilityType }
                    val accent = Color(facilityType.badgeColorHex)
                    val icon = when (facilityType) {
                        FacilityType.STADIUM_SEATING -> Icons.Default.Stadium
                        FacilityType.TRAINING_CENTER -> Icons.Default.FitnessCenter
                        FacilityType.LUXURY_SUITES -> Icons.Default.WorkspacePremium
                    }

                    FacilityTrackCard(
                        type = facilityType,
                        icon = icon,
                        accent = accent,
                        currentTier = currentTier,
                        nextSpec = nextSpec,
                        ongoingProject = ongoing,
                        availableCashMillions = uiState.cashReservesMillions,
                        onInvest = {
                            val (_, msg) = viewModel.investInFacilityUpgrade(selectedTeam.id, facilityType)
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ValuationMiniPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun FacilityTrackCard(
    type: FacilityType,
    icon: ImageVector,
    accent: Color,
    currentTier: Int,
    nextSpec: FacilityUpgradeSpec?,
    ongoingProject: FacilityProject?,
    availableCashMillions: Long,
    onInvest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_facility_track_${type.name.lowercase()}")
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = type.title,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "TIER $currentTier/${type.maxTier}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 5-Step Tier Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (tier in 1..type.maxTier) {
                    val isFilled = tier <= currentTier
                    val isBuilding = ongoingProject?.targetTier == tier
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isFilled -> accent
                                    isBuilding -> accent.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                    )
                }
            }

            if (nextSpec != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "NEXT: Tier ${nextSpec.targetTier} — ${nextSpec.projectName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${nextSpec.durationWeeks}W Build",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                        val deliverables = buildList {
                            if (nextSpec.seatingCapacityGain > 0) add("+%,d Seats".format(nextSpec.seatingCapacityGain))
                            if (nextSpec.annualSponsorGainMillions > 0L) add("+\$${nextSpec.annualSponsorGainMillions}M/yr Commercial")
                            if (nextSpec.rosterOvrGain > 0) add("+${nextSpec.rosterOvrGain} OVR Conditioning & -10% Inj")
                            add("+\$${nextSpec.valuationBoostMillions}M Valuation")
                            if (nextSpec.loyaltyGain > 0) add("+${nextSpec.loyaltyGain} Loyalty")
                        }.joinToString(" • ")

                        Text(
                            text = deliverables,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }

                val canInvest = ongoingProject == null && availableCashMillions >= nextSpec.costMillions
                Button(
                    onClick = onInvest,
                    enabled = canInvest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_invest_facility_${type.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = if (ongoingProject != null) Icons.Default.Engineering else Icons.Default.Construction,
                        contentDescription = null,
                        tint = if (canInvest) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            ongoingProject != null -> "Under Construction (${ongoingProject.remainingWeeks}W Remaining)"
                            availableCashMillions < nextSpec.costMillions -> "Requires \$${nextSpec.costMillions}M Reserves"
                            else -> "Invest \$${nextSpec.costMillions}M • Build Tier ${nextSpec.targetTier}"
                        },
                        color = if (canInvest) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Surface(
                    color = EmeraldAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Max Tier Reached",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "World-Class Tier 5 Flagship Complete",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                    }
                }
            }
        }
    }
}
