package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.model.Conference
import com.example.model.Division
import com.example.model.FanLoyaltyEngine
import com.example.model.Franchise
import com.example.model.FranchiseIdentityEngine
import com.example.model.FranchiseMarketingCampaignType
import com.example.model.FreeAgencyEngine
import com.example.model.MarketCity
import com.example.model.TeamReputationEngine
import com.example.model.calculateFinancialStatement
import com.example.ui.components.FranchiseIdentityCustomizer
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.components.SectionHeader
import com.example.ui.components.TeamBadge
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@Composable
fun FranchisesScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFranchiseForRelocation by remember { mutableStateOf<Franchise?>(null) }
    var selectedFranchiseForRecruitment by remember { mutableStateOf<Franchise?>(null) }
    var selectedFranchiseForRealignment by remember { mutableStateOf<Franchise?>(null) }
    var selectedFranchiseIdForMarketing by remember { mutableStateOf<String?>(null) }
    var selectedFranchiseForIdentity by remember { mutableStateOf<Franchise?>(null) }
    var showConferenceSummitDialog by remember { mutableStateOf(false) }
    var showContinentalBuilderDialog by remember { mutableStateOf(false) }

    var selectedConferenceFilter by remember { mutableStateOf<Conference?>(null) }
    var selectedDivisionFilter by remember { mutableStateOf<Division?>(null) }

    val activeConferences = remember(uiState.franchises, uiState.ruleBook.enabledConferences) {
        uiState.ruleBook.effectiveConferences(uiState.franchises)
    }
    val activeDivisions = remember(uiState.franchises, uiState.ruleBook.enabledDivisions, activeConferences) {
        Division.values().filter { div ->
            div.conference in activeConferences && (div in uiState.ruleBook.enabledDivisions || uiState.franchises.any { it.division == div })
        }
    }

    val filteredFranchises = remember(uiState.franchises, selectedConferenceFilter, selectedDivisionFilter) {
        uiState.franchises.filter { franchise ->
            val matchConf = selectedConferenceFilter == null || franchise.conference == selectedConferenceFilter
            val matchDiv = selectedDivisionFilter == null || franchise.division == selectedDivisionFilter
            matchConf && matchDiv
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("franchises_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FRANCHISE GOVERNANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Franchises & Conferences",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showContinentalBuilderDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_open_continental_builder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Continental Conferences",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Global",
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OutlinedButton(
                        onClick = { showConferenceSummitDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_open_alignment_summit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Realignment",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Realign",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Text(
                text = "Organize North American & international clubs across conferences, establish European, South American & Asia-Oceania divisions, recruit star talent, and supervise payrolls.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Conference Alignment Summary Banner
        item {
            val continentalEnabledCount = activeConferences.count { it.isContinentalInternational }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("conference_alignment_summary_card")
                    .border(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LEAGUE STRUCTURE & ALIGNMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${activeConferences.size} Conf • ${activeDivisions.size} Div • ${uiState.franchises.size} Teams",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeConferences.chunked(2).forEach { rowConfs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowConfs.forEach { conf ->
                                    val confCount = uiState.franchises.count { it.conference == conf }
                                    val confDivs = activeDivisions.filter { it.conference == conf }
                                    val confColor = Color(conf.badgeColorHex)
                                    val isSel = selectedConferenceFilter == conf
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedConferenceFilter = if (isSel) null else conf
                                                selectedDivisionFilter = null
                                            }
                                            .testTag("summary_conf_card_${conf.name.lowercase()}"),
                                        color = if (isSel) confColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.2.dp,
                                            if (isSel) confColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(confColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = conf.displayName.replace(" Conference", " Conf."),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = confColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$confCount Teams",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = confDivs.joinToString(" • ") { it.shortCode },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowConfs.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showContinentalBuilderDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_banner_continental_conferences"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (continentalEnabledCount == 0) {
                                "Create Continental Conferences (EUR • SA • APAC)"
                            } else {
                                "Manage Continental Conferences ($continentalEnabledCount/3 Active)"
                            },
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Conference & Division Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedConferenceFilter == null && selectedDivisionFilter == null,
                        onClick = {
                            selectedConferenceFilter = null
                            selectedDivisionFilter = null
                        },
                        label = { Text("All Teams (${uiState.franchises.size})") },
                        modifier = Modifier.testTag("filter_all_teams")
                    )
                }
                items(activeConferences) { conf ->
                    val confCount = uiState.franchises.count { it.conference == conf }
                    FilterChip(
                        selected = selectedConferenceFilter == conf && selectedDivisionFilter == null,
                        onClick = {
                            selectedConferenceFilter = if (selectedConferenceFilter == conf) null else conf
                            selectedDivisionFilter = null
                        },
                        label = { Text("${conf.shortCode} ($confCount)") },
                        modifier = Modifier.testTag("filter_${conf.name.lowercase()}_conf")
                    )
                }
                items(activeDivisions) { division ->
                    val isSel = selectedDivisionFilter == division
                    val count = uiState.franchises.count { it.division == division }
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            if (isSel) {
                                selectedDivisionFilter = null
                            } else {
                                selectedDivisionFilter = division
                                selectedConferenceFilter = division.conference
                            }
                        },
                        label = { Text("${division.displayName.replace(" Division", "")} ($count)") },
                        modifier = Modifier.testTag("filter_div_${division.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Franchises List
        items(filteredFranchises) { franchise ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("franchise_card_${franchise.id}")
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with badge and conference/division alignment tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TeamBadge(
                            cityName = franchise.cityName,
                            nickname = franchise.nickname,
                            colorHex = franchise.colorHex,
                            secondaryColorHex = franchise.secondaryColorHex,
                            subtitle = "${franchise.wins}W - ${franchise.losses}L • ${franchise.championships} Titles"
                        )

                        // Conference & Division Pill Badge
                        Surface(
                            color = Color(franchise.conference.badgeColorHex).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(franchise.conference.badgeColorHex).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { selectedFranchiseForRealignment = franchise }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${franchise.conference.shortCode} • ${franchise.division.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(franchise.conference.badgeColorHex)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Realign",
                                    tint = Color(franchise.conference.badgeColorHex),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val isManaged = uiState.managedFranchise?.id == franchise.id
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Team Morale: ${franchise.teamMorale}% • Payroll: \$${franchise.annualPayrollMillions}M",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (franchise.teamMorale >= 70) EmeraldAccent else AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = isManaged,
                            onClick = { viewModel.setManagedFranchise(franchise.id) },
                            label = {
                                Text(
                                    text = if (isManaged) "MANAGED CLUB" else "Manage Club",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("btn_manage_club_${franchise.id}")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Roster, Fan Loyalty & Morale Progress
                    ProgressBarWithLabel(
                        label = "Recruitment & Star Talent",
                        progress = (franchise.rosterRating - 50) / 49f,
                        displayValue = "${franchise.rosterRating} OVR",
                        color = GoldPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val loyaltyColor = Color(franchise.loyaltyTier.badgeColorHex)
                    val stmtPreview = remember(franchise, uiState.activeBroadcast, uiState.franchises.size) {
                        franchise.calculateFinancialStatement(uiState.activeBroadcast, uiState.franchises.size)
                    }
                    Surface(
                        color = loyaltyColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, loyaltyColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFranchiseIdForMarketing = franchise.id }
                            .testTag("card_fan_loyalty_${franchise.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            ProgressBarWithLabel(
                                label = "Fan Loyalty (${franchise.loyaltyTier.title})",
                                progress = franchise.fanLoyalty / 100f,
                                displayValue = "${franchise.fanLoyalty}/100",
                                color = loyaltyColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rev Impact: ${if (stmtPreview.loyaltyRevenueImpactMillions >= 0) "+" else ""}\$${stmtPreview.loyaltyRevenueImpactMillions}M • Tkt \$${franchise.ticketPrice} (Fair \$${franchise.fairTicketPrice})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (franchise.activeFranchiseCampaign != null) "Promo Active" else "Boost →",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = loyaltyColor
                                )
                            }
                            Text(
                                text = "Drivers: ${franchise.lastLoyaltyDrivers.lastSummary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ProgressBarWithLabel(
                        label = "Club & Fan Morale",
                        progress = franchise.fanMorale / 100f,
                        displayValue = "${franchise.fanMorale}%",
                        color = EmeraldAccent
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Financial metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ANNUAL PAYROLL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "\$${franchise.annualPayrollMillions}M",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (franchise.annualPayrollMillions > uiState.unionAgreement.salaryCapMillions) CrimsonAlert else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = "LOCAL SPONSORS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "\$${franchise.localSponsorRevenueMillions}M/yr",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }

                        Column {
                            Text(
                                text = "FANBASE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${franchise.fanBaseMillions}M",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ownership Group & Principal Governor details
                    val primaryOwner = franchise.ownership.primaryOwner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_ownership_${franchise.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = primaryOwner.fullName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        com.example.ui.components.OwnerArchetypeBadge(archetype = primaryOwner.archetype)
                                    }
                                    Text(
                                        text = "${franchise.ownership.name} • ${franchise.ownership.type.title} • \$${franchise.ownership.netWorthBillions}B",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${franchise.ownership.ownerApproval}% Approval",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (franchise.ownership.ownerApproval >= 70) EmeraldAccent else CrimsonAlert
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${primaryOwner.roleTitle}: ${primaryOwner.archetype.manifesto}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Team Reputation & Free-Agent Signing Appeal
                    val rep = TeamReputationEngine.evaluateReputation(
                        franchise = franchise,
                        salaryCapMillions = uiState.unionAgreement.salaryCapMillions
                    )
                    val repAccent = Color(rep.tier.colorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, repAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .testTag("card_team_reputation_${franchise.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "TEAM REPUTATION • ${rep.score}/100",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = repAccent
                                    )
                                    Surface(
                                        color = repAccent.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("badge_team_reputation_${franchise.id}")
                                    ) {
                                        Text(
                                            text = rep.tier.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = repAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${rep.baseFreeAgentLikelihoodPct}% FA Appeal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = repAccent
                                )
                            }
                            Text(
                                text = buildString {
                                    append("${rep.relocationCount} Reloc • ${rep.payrollPctOfCap}% Cap Spend")
                                    if (rep.reputationSalaryPremiumMillions > 0L) {
                                        append(" • +\$${rep.reputationSalaryPremiumMillions}M FA Demand")
                                    }
                                    append(" — ${rep.summaryNote}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lineup Readiness & Medical Depth Chart Status
                    val injCount = franchise.injuredPlayersCount
                    val holesCount = franchise.unresolvedStarterHolesCount
                    val healthAccent = when {
                        holesCount > 0 -> CrimsonAlert
                        injCount > 0 -> AmberWarning
                        else -> EmeraldAccent
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, healthAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        holesCount > 0 -> "$holesCount Starter Hole(s) • ${franchise.effectiveLineupRating} EFF OVR"
                                        injCount > 0 -> "$injCount Injured • ${franchise.effectiveLineupRating} EFF OVR"
                                        else -> "Lineup Ready • ${franchise.effectiveLineupRating} EFF OVR"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = healthAccent
                                )
                                Text(
                                    text = "${franchise.roster.size}-Man Roster • ${franchise.tradeBlockListings.size} On Trade Block",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.openTradeBlock(franchise.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .testTag("btn_trade_block_${franchise.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Block",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.openDepthChart(franchise.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .testTag("btn_depth_chart_${franchise.id}")
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        tint = healthAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Depth",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = healthAccent
                                    )
                                }
                            }
                        }
                    }

                    // Rivalry Summary & Quick Access
                    val teamRivalries = uiState.rivalries.filter { it.isActiveRivalry && it.involves(franchise.id) }
                    val topTeamRivalry = teamRivalries.maxByOrNull { it.intensity }
                    val topRivalTeam = topTeamRivalry?.let { r ->
                        val oppId = r.opponentIdOf(franchise.id)
                        uiState.franchises.find { it.id == oppId }
                    }
                    if (topTeamRivalry != null && topRivalTeam != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = CrimsonAlert.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openRivalryHub(franchise.id) }
                                .testTag("card_franchise_rivalry_${franchise.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = "Top Rivalry",
                                        tint = CrimsonAlert,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "${topTeamRivalry.rivalryName} vs ${topRivalTeam.nickname} (${topTeamRivalry.intensity}/100)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonAlert,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${teamRivalries.size} active feud(s) • ${topTeamRivalry.ticketRevenueMultiplier}x Gate • ${topTeamRivalry.moraleMultiplier}x Morale",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "View →",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = CrimsonAlert
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stadium & Infrastructure Strip
                    val fac = franchise.facilities
                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openFacilityHub(franchise.id) }
                            .testTag("card_franchise_facilities_${franchise.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val needsStadium = franchise.stadiumAge > 25 && franchise.stadiumCondition < 50
                                val stadiumDeal = franchise.stadium
                                Text(
                                    text = "${"%,d".format(franchise.stadiumCapacity)} Seats • \$${stmtPreview.franchiseValuationMillions}M Value",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (needsStadium) CrimsonAlert else EmeraldAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Age ${franchise.stadiumAge}y • ${franchise.stadiumCondition}% Cond • Std T${fac.stadiumTier} • Trn T${fac.trainingTier}${if (fac.ongoingProjects.isNotEmpty()) " (${fac.ongoingProjects.size} Building)" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (needsStadium) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (stadiumDeal.hasActiveSponsor) {
                                        "${stadiumDeal.sponsorName} • T${stadiumDeal.sponsorTier} (\$${stadiumDeal.namingRightsRevenue}M/yr, ${stadiumDeal.sponsorYearsLeft}y)"
                                    } else {
                                        "No Naming Sponsor"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (stadiumDeal.sponsorTier == 3) AmberAccent else if (stadiumDeal.hasActiveSponsor) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.openFacilityHub(franchise.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_manage_facilities_${franchise.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stadium,
                                    contentDescription = "Manage Facilities",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Facilities",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Free Agency & Depth Chart Fill Strip
                    val faNeed = FreeAgencyEngine.evaluateDepthChartNeeds(franchise, uiState.sportType, uiState.ruleBook)
                    val capRoomM = uiState.unionAgreement.salaryCapMillions - franchise.annualPayrollMillions
                    Surface(
                        color = VioletAccent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VioletAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openFreeAgencyPool(franchise.id) }
                            .testTag("card_franchise_free_agency_${franchise.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Depth ${franchise.roster.size}/${faNeed.maxRosterSlots} • ${if (capRoomM >= 0) "+\$${capRoomM}M Cap" else "Over Cap \$${-capRoomM}M"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = VioletAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (faNeed.priorityScore > 0) faNeed.primaryReason else "Full Rotation (${faNeed.healthyCount} healthy)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.openFreeAgencyPool(franchise.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_open_fa_pool_${franchise.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Free Agency Pool",
                                    tint = VioletAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FA Pool",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VioletAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Commissioner Actions for Franchise (Recruit, Realign, Relocate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedFranchiseForRecruitment = franchise },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_recruit_${franchise.id}")
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recruit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { selectedFranchiseForRealignment = franchise },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_realign_${franchise.id}")
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(franchise.conference.badgeColorHex))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Realign", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(franchise.conference.badgeColorHex))
                        }

                        Button(
                            onClick = { selectedFranchiseForRelocation = franchise },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_relocate_${franchise.id}")
                                .minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Relocate", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { selectedFranchiseIdForMarketing = franchise.id },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_franchise_marketing_${franchise.id}")
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(franchise.loyaltyTier.badgeColorHex).copy(alpha = 0.6f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Marketing & Fan Loyalty",
                            tint = Color(franchise.loyaltyTier.badgeColorHex),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (franchise.activeFranchiseCampaign != null) {
                                "Campaign: ${franchise.activeFranchiseCampaign.type.title} (${franchise.activeFranchiseCampaign.remainingWeeks}w left)"
                            } else {
                                "Fan Loyalty & Marketing Campaigns (${franchise.fanLoyalty}/100)"
                            },
                            color = Color(franchise.loyaltyTier.badgeColorHex),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { selectedFranchiseForIdentity = franchise },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_customize_franchise_${franchise.id}")
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(franchise.colorHex).copy(alpha = 0.65f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Customize Franchise Colors & Nickname",
                            tint = Color(franchise.secondaryColorHex),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rebrand & Colors (${FranchiseIdentityEngine.colorNameFor(franchise.colorHex)} / ${FranchiseIdentityEngine.colorNameFor(franchise.secondaryColorHex)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Fan Loyalty & Targeted Franchise Marketing Dialog
    val activeMarketingTeam = selectedFranchiseIdForMarketing?.let { id ->
        uiState.franchises.find { it.id == id }
    }
    if (activeMarketingTeam != null) {
        val stmt = activeMarketingTeam.calculateFinancialStatement(uiState.activeBroadcast, uiState.franchises.size)
        val tierColor = Color(activeMarketingTeam.loyaltyTier.badgeColorHex)
        val priceTrend = FanLoyaltyEngine.computeWeeklyPricingDelta(activeMarketingTeam)
        var campaignFeedback by remember(activeMarketingTeam.id) { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { selectedFranchiseIdForMarketing = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .border(2.dp, tierColor, RoundedCornerShape(20.dp))
                    .testTag("dialog_franchise_loyalty_marketing"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 580.dp)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "FAN LOYALTY & COMMERCIAL STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = tierColor,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = activeMarketingTeam.teamFullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${activeMarketingTeam.loyaltyTier.title} (${activeMarketingTeam.fanLoyalty}/100) — ${activeMarketingTeam.loyaltyTier.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "REVENUE & ELASTICITY IMPACT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "Attendance: ${stmt.attendancePct}% • Merch Multiplier: ${stmt.loyaltyMerchMultiplier}x • Sponsor Multiplier: ${stmt.loyaltySponsorMultiplier}x",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Net Loyalty Revenue Impact: ${if (stmt.loyaltyRevenueImpactMillions >= 0) "+" else ""}\$${stmt.loyaltyRevenueImpactMillions}M vs baseline",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (stmt.loyaltyRevenueImpactMillions >= 0) EmeraldAccent else CrimsonAlert
                                )
                                Text(
                                    text = "Ticket Pricing: \$${activeMarketingTeam.ticketPrice}/seat vs Fair \$${activeMarketingTeam.fairTicketPrice} (${if (priceTrend >= 0) "+$priceTrend" else "$priceTrend"} loyalty/wk)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (priceTrend < 0) CrimsonAlert else EmeraldAccent
                                )
                            }
                        }
                    }

                    if (activeMarketingTeam.activeFranchiseCampaign != null) {
                        val activeCamp = activeMarketingTeam.activeFranchiseCampaign!!
                        item {
                            Surface(
                                color = CyanAccent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "ACTIVE CLUB CAMPAIGN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = activeCamp.type.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${activeCamp.remainingWeeks} week(s) remaining • +${activeCamp.type.loyaltyPerWeek} Loyalty/wk • +${activeCamp.totalLoyaltyGained} gained so far",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (campaignFeedback != null) {
                        item {
                            Text(
                                text = campaignFeedback!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item {
                        Text(
                            text = "LAUNCH TARGETED MARKETING CAMPAIGN (Treasury: \$${uiState.cashReservesMillions}M)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                    }

                    items(FranchiseMarketingCampaignType.values().toList()) { campType ->
                        val isSameActive = activeMarketingTeam.activeFranchiseCampaign?.type == campType
                        val canAfford = uiState.cashReservesMillions >= campType.costMillions && !isSameActive
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = campType.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = campType.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "+${campType.immediateLoyaltyBoost} Now • +${campType.loyaltyPerWeek}/wk × ${campType.durationWeeks}w • +${campType.fanbaseGrowthPct}% Fanbase",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val (_, msg) = viewModel.launchFranchiseMarketingCampaign(activeMarketingTeam.id, campType)
                                        campaignFeedback = msg
                                    },
                                    enabled = canAfford,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_launch_club_camp_${campType.name.lowercase()}")
                                ) {
                                    Text(
                                        text = if (isSameActive) "Active" else "\$${campType.costMillions}M",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedFranchiseIdForMarketing = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Individual Franchise Realignment Dialog
    if (selectedFranchiseForRealignment != null) {
        val target = selectedFranchiseForRealignment!!
        var chosenConf by remember(target) { mutableStateOf(target.conference) }
        var chosenDiv by remember(target) {
            mutableStateOf(
                if (target.division.conference == chosenConf) target.division
                else Division.values().first { it.conference == chosenConf }
            )
        }

        Dialog(onDismissRequest = { selectedFranchiseForRealignment = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_realign_franchise")
                    .border(1.5.dp, GoldPrimary, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "FRANCHISE REALIGNMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = target.teamFullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Current: ${target.conference.displayName} • ${target.division.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SELECT CONFERENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeConferences.chunked(2).forEach { rowConfs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowConfs.forEach { conf ->
                                    val isSel = chosenConf == conf
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                chosenConf = conf
                                                val divsInConf = uiState.ruleBook.effectiveDivisionsFor(conf, uiState.franchises)
                                                if (chosenDiv !in divsInConf) {
                                                    chosenDiv = divsInConf.first()
                                                }
                                            },
                                        color = if (isSel) Color(conf.badgeColorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.2.dp,
                                            if (isSel) Color(conf.badgeColorHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = conf.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color(conf.badgeColorHex) else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${conf.shortCode} • ${conf.regionLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                if (rowConfs.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SELECT DIVISION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val availableDivisions = uiState.ruleBook.effectiveDivisionsFor(chosenConf, uiState.franchises)
                    availableDivisions.forEach { div ->
                        val isSel = chosenDiv == div
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { chosenDiv = div }
                                .border(
                                    1.dp,
                                    if (isSel) Color(chosenConf.badgeColorHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) Color(chosenConf.badgeColorHex).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSel, onClick = { chosenDiv = div })
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = div.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = div.geographicRegion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedFranchiseForRealignment = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.reassignFranchiseConferenceDivision(
                                    franchiseId = target.id,
                                    conference = chosenConf,
                                    division = chosenDiv
                                )
                                selectedFranchiseForRealignment = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Text("Confirm Reassignment", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Conference Realignment Summit Full Dialog
    if (showConferenceSummitDialog) {
        Dialog(onDismissRequest = { showConferenceSummitDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .testTag("dialog_conference_summit")
                    .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "COMMISSIONER REALIGNMENT SUMMIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "League Alignment Codex",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Organize North American and international clubs across enabled conferences & divisions, or establish new Continental Conferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Optimize Travel + Continental Creator Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.autoRealignAllFranchisesGeographically()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_auto_realign_geographic")
                        ) {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto Geo", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { showContinentalBuilderDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_summit_continental_builder")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Continental Conf.", color = CyanAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Divisions breakdown scrollable list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        activeConferences.forEach { conf ->
                            item {
                                Surface(
                                    color = Color(conf.badgeColorHex).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${conf.displayName.uppercase()} (${uiState.franchises.count { it.conference == conf }} TEAMS)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(conf.badgeColorHex),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            val confDivisions = uiState.ruleBook.effectiveDivisionsFor(conf, uiState.franchises)
                            confDivisions.forEach { div ->
                                val divTeams = uiState.franchises.filter { it.division == div }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = div.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${divTeams.size} teams",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (divTeams.isEmpty()) {
                                                Text(
                                                    text = "No teams assigned",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            } else {
                                                divTeams.forEach { team ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "• ${team.teamFullName}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        TextButton(
                                                            onClick = {
                                                                selectedFranchiseForRealignment = team
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("Move", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showConferenceSummitDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close Summit", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Continental Conferences & Divisions Builder Dialog
    if (showContinentalBuilderDialog) {
        ContinentalConferenceBuilderDialog(
            franchises = uiState.franchises,
            enabledConferences = uiState.ruleBook.enabledConferences,
            enabledDivisions = uiState.ruleBook.enabledDivisions,
            currentPlayoffRules = uiState.ruleBook.playoffRules,
            onToggleConference = { conf, enable ->
                viewModel.createOrToggleContinentalConference(conf, enable = enable, autoAlignNonNorthAmerican = true, adaptPlayoffs = true)
            },
            onCreateAllContinental = {
                viewModel.createAllContinentalConferences(autoAlignNonNorthAmerican = true, adaptPlayoffs = true)
            },
            onToggleDivision = { div ->
                viewModel.toggleDivisionEnabled(div)
            },
            onAutoAlignInternational = {
                viewModel.alignInternationalFranchisesToContinentalConferences(adaptPlayoffs = true)
            },
            onDismiss = { showContinentalBuilderDialog = false }
        )
    }

    // Talent Recruitment Dialog
    if (selectedFranchiseForRecruitment != null) {
        val franchise = selectedFranchiseForRecruitment!!
        Dialog(onDismissRequest = { selectedFranchiseForRecruitment = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_recruit_talent")
                    .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "TALENT RECRUITMENT AGENCY",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = franchise.teamFullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Current Roster: ${franchise.rosterRating} OVR • Payroll: \$${franchise.annualPayrollMillions}M",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val tiers = listOf(
                        Triple("Tier 1: Generational All-Pro Star", 18L, 5),
                        Triple("Tier 2: Prime Roster Contender", 10L, 3),
                        Triple("Tier 3: Veteran Role Playmaker", 5L, 2)
                    )

                    tiers.forEach { (label, costMillions, boost) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.recruitTalentForFranchise(franchise.id, costMillions, boost)
                                    selectedFranchiseForRecruitment = null
                                }
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "+$boost OVR Roster Rating", style = MaterialTheme.typography.bodySmall, color = EmeraldAccent)
                                }
                                Surface(
                                    color = GoldPrimary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "\$${costMillions}M/yr",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = { selectedFranchiseForRecruitment = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Franchise Relocation Dialog
    if (selectedFranchiseForRelocation != null) {
        val franchise = selectedFranchiseForRelocation!!
        val availableCities = uiState.cities.filter { !it.hasFranchise }

        Dialog(onDismissRequest = { selectedFranchiseForRelocation = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .testTag("dialog_relocate_franchise")
                    .border(2.dp, CrimsonAlert, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "RELOCATE FRANCHISE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Relocate ${franchise.teamFullName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Move this team to an untapped high-growth metropolitan market. Boosts fanbase and stadium economics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(availableCities) { city ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.relocateFranchise(franchise.id, city.id)
                                        selectedFranchiseForRelocation = null
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)
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
                                            text = "${city.name}, ${city.country}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val cityTargetConf = com.example.data.InitialLeagueData.getGeographicAlignment(
                                            city.name,
                                            uiState.ruleBook.enabledConferences,
                                            uiState.ruleBook.enabledDivisions
                                        )
                                        Text(
                                            text = "${city.tier.label} • Pop: ${city.populationMillions}M • ${cityTargetConf.first.shortCode} (${cityTargetConf.second.shortCode})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Select",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = { selectedFranchiseForRelocation = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Rebrand & Dual Color Identity Customization Dialog
    if (selectedFranchiseForIdentity != null) {
        val franchise = selectedFranchiseForIdentity!!
        var nicknameInput by remember(franchise.id) { mutableStateOf(franchise.nickname) }
        var primaryHex by remember(franchise.id) { mutableLongStateOf(franchise.colorHex) }
        var secondaryHex by remember(franchise.id) {
            mutableLongStateOf(
                if (franchise.secondaryColorHex != franchise.colorHex) franchise.secondaryColorHex
                else FranchiseIdentityEngine.defaultSecondaryFor(franchise.colorHex)
            )
        }

        Dialog(onDismissRequest = { selectedFranchiseForIdentity = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp))
                    .testTag("franchise_identity_modal"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "FRANCHISE BRANDING STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${franchise.cityName} Identity & Colors",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FranchiseIdentityCustomizer(
                        cityName = franchise.cityName,
                        nickname = nicknameInput,
                        onNicknameChange = { nicknameInput = it },
                        onRandomizeNickname = {
                            nicknameInput = viewModel.generateRandomFranchiseNickname(
                                targetCityId = franchise.cityId,
                                excludeNickname = nicknameInput
                            )
                        },
                        primaryColorHex = primaryHex,
                        secondaryColorHex = secondaryHex,
                        onColorsChanged = { p, s ->
                            primaryHex = p
                            secondaryHex = s
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedFranchiseForIdentity = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val (ok, _) = viewModel.updateFranchiseIdentity(
                                    franchiseId = franchise.id,
                                    newNickname = nicknameInput,
                                    primaryColorHex = primaryHex,
                                    secondaryColorHex = secondaryHex
                                )
                                if (ok) {
                                    selectedFranchiseForIdentity = null
                                }
                            },
                            enabled = nicknameInput.isNotBlank() && primaryHex != secondaryHex,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_save_franchise_identity")
                        ) {
                            Text("Save Identity", color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinentalConferenceBuilderDialog(
    franchises: List<Franchise>,
    enabledConferences: Set<Conference>,
    enabledDivisions: Set<Division>,
    currentPlayoffRules: com.example.model.PlayoffRules,
    onToggleConference: (Conference, Boolean) -> Unit,
    onCreateAllContinental: () -> Unit,
    onToggleDivision: (Division) -> Unit,
    onAutoAlignInternational: () -> Unit,
    onDismiss: () -> Unit
) {
    val continentalConferences = remember {
        listOf(Conference.EUROPEAN, Conference.SOUTH_AMERICAN, Conference.ASIA_OCEANIA)
    }
    val nonNaFranchises = remember(franchises) {
        franchises.filter { !com.example.data.InitialLeagueData.isNorthAmericanCity(it.cityName) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("dialog_continental_conference_builder")
                .border(2.dp, CyanAccent, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INTERNATIONAL EXPANSION CHARTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Continental Conferences & Divisions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_continental_builder")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Establish dedicated European, South American, and Asia-Oceania Conferences with regional divisions for non-North-American franchises. Playoff bylaws adapt automatically as conferences grow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick charter action bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateAllContinental,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_create_all_continental_conferences")
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create All 3 Global Conf.",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OutlinedButton(
                        onClick = onAutoAlignInternational,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_auto_align_intl_franchises")
                    ) {
                        Text(
                            text = "Auto-Align (${nonNaFranchises.size})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    continentalConferences.forEach { conf ->
                        item {
                            val isEnabled = conf in enabledConferences
                            val confColor = Color(conf.badgeColorHex)
                            val confDivs = Division.values().filter { it.conference == conf }
                            val matchingIntlClubs = nonNaFranchises.filter {
                                com.example.data.InitialLeagueData.getTargetContinentalConference(it.cityName) == conf
                            }
                            val assignedClubs = franchises.filter { it.conference == conf }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_continental_conf_${conf.name.lowercase()}")
                                    .border(
                                        width = if (isEnabled) 1.5.dp else 1.dp,
                                        color = if (isEnabled) confColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isEnabled) confColor.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(confColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = conf.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isEnabled) confColor else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${assignedClubs.size} assigned • ${matchingIntlClubs.size} regional clubs in league",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onToggleConference(conf, !isEnabled) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isEnabled) EmeraldAccent else confColor
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("btn_toggle_continental_${conf.name.lowercase()}")
                                        ) {
                                            Text(
                                                text = if (isEnabled) "Active ✓" else "Create",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = conf.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isEnabled) "REGIONAL DIVISIONS (TAP TO INCLUDE/EXCLUDE):" else "INCLUDED DIVISIONS UPON CREATION:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    confDivs.forEach { div ->
                                        val isDivActive = isEnabled && (div in enabledDivisions)
                                        val divTeamCount = franchises.count { it.division == div }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .clickable(enabled = isEnabled) { onToggleDivision(div) }
                                                .border(
                                                    1.dp,
                                                    if (isDivActive) confColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .testTag("btn_toggle_div_${div.name.lowercase()}"),
                                            color = if (isDivActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${div.displayName} (${div.shortCode})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDivActive) confColor else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${div.geographicRegion}${if (divTeamCount > 0) " • $divTeamCount teams" else ""}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (isDivActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isDivActive) EmeraldAccent else TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = EmeraldAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "ADAPTIVE PLAYOFF RULES SYNCED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = currentPlayoffRules.formatSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_done_continental_builder"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

