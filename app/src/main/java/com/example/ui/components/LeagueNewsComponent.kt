package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

/**
 * High-performance, dynamic League News Component.
 * Dynamically generates and displays headlines covering expansion drafts, broadcast rights deals,
 * labor union disputes, commissioner bylaws, and financial milestones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueNewsComponent(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier,
    maxDisplayCount: Int = 10,
    showHeaderAction: Boolean = true
) {
    var selectedCategoryFilter by remember { mutableStateOf<NewsCategory?>(null) }
    var selectedNewsItemForDetail by remember { mutableStateOf<LeagueNewsItem?>(null) }
    var isGeneratingNews by remember { mutableStateOf(false) }

    // Parse all raw strings from uiState into structured news items
    val allStructuredNews = remember(uiState.recentNews, uiState.currentWeek, uiState.currentYear) {
        uiState.recentNews.map { raw ->
            LeagueNewsParser.parseFromRawString(raw, uiState.currentWeek, uiState.currentYear)
        }
    }

    val filteredNews = remember(allStructuredNews, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            allStructuredNews
        } else {
            allStructuredNews.filter { it.category == selectedCategoryFilter }
        }
    }

    val latestHeadline = allStructuredNews.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_league_news_component")
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "League News Wire",
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "COMMISSIONER WIRE",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = CrimsonAlert,
                                shape = CircleShape,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonAlert,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "League News Intelligence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (showHeaderAction) {
                    Button(
                        onClick = {
                            isGeneratingNews = true
                            viewModel.generateDynamicHeadline(selectedCategoryFilter)
                            isGeneratingNews = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("btn_generate_breaking_news")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Generate Wire",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Real-time dispatch wire covering completed expansion drafts, signed broadcast media pacts, labor union collective bargaining, and competition bylaws.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Breaking News Ticker Bar
            if (latestHeadline != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedNewsItemForDetail = latestHeadline }
                        .testTag("news_ticker_bar"),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (latestHeadline.impact) {
                                NewsImpact.BREAKING -> CrimsonAlert
                                NewsImpact.CRITICAL -> AmberAccent
                                NewsImpact.MAJOR -> RoyalBlueAccent
                                NewsImpact.UPDATE -> EmeraldAccent
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = latestHeadline.impact.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (latestHeadline.impact == NewsImpact.CRITICAL) Color.Black else Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = latestHeadline.headline,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("row_news_category_filters"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isAllSelected = selectedCategoryFilter == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { selectedCategoryFilter = null },
                        label = {
                            Text(
                                text = "All Wire (${allStructuredNews.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("filter_news_all")
                    )
                }

                items(NewsCategory.values()) { category ->
                    val isSelected = selectedCategoryFilter == category
                    val count = allStructuredNews.count { it.category == category }
                    val tagSuffix = when (category) {
                        NewsCategory.EXPANSION_DRAFT -> "expansion"
                        NewsCategory.BROADCAST_DEAL -> "broadcast"
                        NewsCategory.LABOR_UNION -> "labor"
                        NewsCategory.RULES_GOVERNANCE -> "rules"
                        NewsCategory.FINANCIALS -> "financials"
                        NewsCategory.FREE_AGENCY -> "free_agency"
                        NewsCategory.COMPETITION -> "competition"
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategoryFilter = if (isSelected) null else category
                        },
                        label = {
                            Text(
                                text = "${category.displayName} ($count)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = null,
                                tint = Color(category.badgeColorHex),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("filter_news_$tagSuffix")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Category Generator Shortcuts
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "DYNAMIC HEADLINE GENERATOR (EVENT DISPATCHES)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Expansion Draft Generator
                        OutlinedButton(
                            onClick = {
                                viewModel.generateDynamicHeadline(NewsCategory.EXPANSION_DRAFT)
                                selectedCategoryFilter = NewsCategory.EXPANSION_DRAFT
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_dispatch_expansion_draft"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                Text("Expansion", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        // Broadcast Deal Generator
                        OutlinedButton(
                            onClick = {
                                viewModel.generateDynamicHeadline(NewsCategory.BROADCAST_DEAL)
                                selectedCategoryFilter = NewsCategory.BROADCAST_DEAL
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_dispatch_broadcast_deal"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.LiveTv, contentDescription = null, tint = RoyalBlueAccent, modifier = Modifier.size(14.dp))
                                Text("Broadcast", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        // Labor Dispute Generator
                        OutlinedButton(
                            onClick = {
                                viewModel.generateDynamicHeadline(NewsCategory.LABOR_UNION)
                                selectedCategoryFilter = NewsCategory.LABOR_UNION
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_dispatch_labor_dispute"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Handshake, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(14.dp))
                                Text("Labor/CBA", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // News Headlines List
            if (filteredNews.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No dispatches for selected category yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.generateDynamicHeadline(selectedCategoryFilter) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Generate ${selectedCategoryFilter?.displayName ?: "League"} Headline")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredNews.take(maxDisplayCount).forEachIndexed { index, item ->
                        NewsCardItem(
                            item = item,
                            onClick = { selectedNewsItemForDetail = item },
                            onExecuteAction = { actionType ->
                                when (actionType) {
                                    NewsActionType.NAVIGATE_EXPANSION -> viewModel.setSelectedTab(2) // Markets & Expansion
                                    NewsActionType.NAVIGATE_BROADCAST -> viewModel.openBroadcastNegotiations(true)
                                    NewsActionType.NAVIGATE_LABOR -> viewModel.openLaborUnionScreen(true)
                                    NewsActionType.NAVIGATE_RULES -> viewModel.setSelectedTab(4) // Rules & Governance
                                    NewsActionType.NAVIGATE_FINANCES -> viewModel.setSelectedTab(3) // Finances
                                    NewsActionType.NAVIGATE_FREE_AGENCY -> viewModel.openFreeAgencyPool()
                                    NewsActionType.NONE -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog for Full Press Dispatch
    if (selectedNewsItemForDetail != null) {
        val item = selectedNewsItemForDetail!!
        AlertDialog(
            onDismissRequest = { selectedNewsItemForDetail = null },
            modifier = Modifier.testTag("dialog_news_dossier"),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when (item.impact) {
                            NewsImpact.BREAKING -> CrimsonAlert
                            NewsImpact.CRITICAL -> AmberAccent
                            NewsImpact.MAJOR -> RoyalBlueAccent
                            NewsImpact.UPDATE -> EmeraldAccent
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.impact.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (item.impact == NewsImpact.CRITICAL) Color.Black else Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(item.category.badgeColorHex)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.headline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Source: ${item.sourceAgency}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.timestampLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }
            },
            confirmButton = {
                if (item.actionType != NewsActionType.NONE) {
                    Button(
                        onClick = {
                            val action = item.actionType
                            selectedNewsItemForDetail = null
                            when (action) {
                                NewsActionType.NAVIGATE_EXPANSION -> viewModel.setSelectedTab(2)
                                NewsActionType.NAVIGATE_BROADCAST -> viewModel.openBroadcastNegotiations(true)
                                NewsActionType.NAVIGATE_LABOR -> viewModel.openLaborUnionScreen(true)
                                NewsActionType.NAVIGATE_RULES -> viewModel.setSelectedTab(4)
                                NewsActionType.NAVIGATE_FINANCES -> viewModel.setSelectedTab(3)
                                NewsActionType.NAVIGATE_FREE_AGENCY -> viewModel.openFreeAgencyPool()
                                NewsActionType.NONE -> {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text(
                            text = when (item.actionType) {
                                NewsActionType.NAVIGATE_EXPANSION -> "Go to Expansion"
                                NewsActionType.NAVIGATE_BROADCAST -> "Go to Broadcast Deals"
                                NewsActionType.NAVIGATE_LABOR -> "Go to Labor Union"
                                NewsActionType.NAVIGATE_RULES -> "Go to Rules"
                                NewsActionType.NAVIGATE_FINANCES -> "Go to Finances"
                                NewsActionType.NAVIGATE_FREE_AGENCY -> "Go to Free Agency Pool"
                                NewsActionType.NONE -> "OK"
                            },
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    TextButton(onClick = { selectedNewsItemForDetail = null }) {
                        Text("Dismiss")
                    }
                }
            },
            dismissButton = {
                if (item.actionType != NewsActionType.NONE) {
                    TextButton(onClick = { selectedNewsItemForDetail = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
fun NewsCardItem(
    item: LeagueNewsItem,
    onClick: () -> Unit,
    onExecuteAction: (NewsActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_news_item"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Meta Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Impact badge
                    Surface(
                        color = when (item.impact) {
                            NewsImpact.BREAKING -> CrimsonAlert
                            NewsImpact.CRITICAL -> AmberAccent
                            NewsImpact.MAJOR -> RoyalBlueAccent
                            NewsImpact.UPDATE -> EmeraldAccent
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.impact.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (item.impact == NewsImpact.CRITICAL) Color.Black else Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Category tag
                    Surface(
                        color = Color(item.category.badgeColorHex).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(item.category),
                                contentDescription = null,
                                tint = Color(item.category.badgeColorHex),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(item.category.badgeColorHex),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = item.timestampLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            Text(
                text = item.headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Summary
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wire: ${item.sourceAgency}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (item.actionType != NewsActionType.NONE) {
                    val actionLabel = when (item.actionType) {
                        NewsActionType.NAVIGATE_EXPANSION -> "View Draft"
                        NewsActionType.NAVIGATE_BROADCAST -> "View Deal"
                        NewsActionType.NAVIGATE_LABOR -> "View CBA"
                        NewsActionType.NAVIGATE_RULES -> "View Rules"
                        NewsActionType.NAVIGATE_FINANCES -> "View Finances"
                        NewsActionType.NAVIGATE_FREE_AGENCY -> "View Pool"
                        NewsActionType.NONE -> ""
                    }
                    val actionTag = when (item.actionType) {
                        NewsActionType.NAVIGATE_EXPANSION -> "btn_news_action_expansion"
                        NewsActionType.NAVIGATE_BROADCAST -> "btn_news_action_broadcast"
                        NewsActionType.NAVIGATE_LABOR -> "btn_news_action_labor"
                        NewsActionType.NAVIGATE_RULES -> "btn_news_action_rules"
                        NewsActionType.NAVIGATE_FINANCES -> "btn_news_action_finances"
                        NewsActionType.NAVIGATE_FREE_AGENCY -> "btn_news_action_free_agency"
                        NewsActionType.NONE -> ""
                    }

                    OutlinedButton(
                        onClick = { onExecuteAction(item.actionType) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag(actionTag)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: NewsCategory): ImageVector {
    return when (category) {
        NewsCategory.EXPANSION_DRAFT -> Icons.Default.Groups
        NewsCategory.BROADCAST_DEAL -> Icons.Default.LiveTv
        NewsCategory.LABOR_UNION -> Icons.Default.Handshake
        NewsCategory.RULES_GOVERNANCE -> Icons.Default.Gavel
        NewsCategory.FINANCIALS -> Icons.Default.AttachMoney
        NewsCategory.FREE_AGENCY -> Icons.Default.PersonAdd
        NewsCategory.COMPETITION -> Icons.Default.EmojiEvents
    }
}
