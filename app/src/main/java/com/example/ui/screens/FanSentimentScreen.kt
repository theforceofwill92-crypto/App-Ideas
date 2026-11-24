package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanSentimentScreen(
    uiState: LeagueUiState,
    viewModel: LeagueViewModel
) {
    BackHandler { viewModel.openFanSentimentScreen(false) }

    val fs = uiState.fanSentimentState
    var selectedChannel by remember { mutableStateOf(FanMediaChannel.ALL) }
    var selectedSegmentFilter by remember { mutableStateOf(FanSegmentFilter.ALL) }
    var actionFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val filteredSocial = remember(fs.socialFeed, selectedSegmentFilter) {
        when (selectedSegmentFilter) {
            FanSegmentFilter.ALL -> fs.socialFeed
            FanSegmentFilter.CASUAL -> fs.socialFeed.filter { it.segment == FanSegmentType.CASUAL }
            FanSegmentFilter.DIE_HARD -> fs.socialFeed.filter { it.segment == FanSegmentType.DIE_HARD }
        }
    }

    val filteredRadio = remember(fs.radioSegments, selectedSegmentFilter) {
        when (selectedSegmentFilter) {
            FanSegmentFilter.ALL -> fs.radioSegments
            FanSegmentFilter.CASUAL -> fs.radioSegments.filter { it.callerSegment == FanSegmentType.CASUAL || it.casualApprovalPct != it.dieHardApprovalPct }
            FanSegmentFilter.DIE_HARD -> fs.radioSegments.filter { it.callerSegment == FanSegmentType.DIE_HARD || it.casualApprovalPct != it.dieHardApprovalPct }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fan_sentiment_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Fan Sentiment & Media Pulse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Casual ${fs.casualSentimentScore}% • Die-Hard ${fs.dieHardSentimentScore}% • ${fs.polarizationLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openFanSentimentScreen(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_back_fan_sentiment")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Commissioner Desk"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedChannel.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GoldPrimary
            ) {
                FanMediaChannel.entries.forEach { channel ->
                    val tag = when (channel) {
                        FanMediaChannel.ALL -> "tab_media_all"
                        FanMediaChannel.SOCIAL -> "tab_media_social"
                        FanMediaChannel.RADIO -> "tab_media_radio"
                        FanMediaChannel.NEWSPAPER -> "tab_media_newspaper"
                    }
                    Tab(
                        selected = selectedChannel == channel,
                        onClick = { selectedChannel = channel },
                        text = {
                            Text(
                                text = channel.label,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag(tag)
                    )
                }
            }

            actionFeedback?.let { (ok, msg) ->
                val accent = if (ok) EmeraldAccent else CrimsonAlert
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("fan_initiative_feedback")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = "Initiative Status",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("fan_sentiment_feed_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Casual vs Die-Hard Scorecard & Outreach Card
                if (selectedChannel == FanMediaChannel.ALL) {
                    item(key = "cohort_comparison") {
                        FanCohortComparisonCard(
                            state = fs,
                            cashMillions = uiState.cashReservesMillions,
                            onLaunchCasualInitiative = {
                                actionFeedback = viewModel.hostFanPulseInitiative(FanSegmentType.CASUAL)
                            },
                            onLaunchDieHardInitiative = {
                                actionFeedback = viewModel.hostFanPulseInitiative(FanSegmentType.DIE_HARD)
                            }
                        )
                    }
                }

                // 2. Segment filter chips (All / Casual / Die-Hard)
                if (selectedChannel != FanMediaChannel.NEWSPAPER) {
                    item(key = "segment_filter_row") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(FanSegmentFilter.entries, key = { it.name }) { filter ->
                                val tag = when (filter) {
                                    FanSegmentFilter.ALL -> "chip_segment_all"
                                    FanSegmentFilter.CASUAL -> "chip_segment_casual"
                                    FanSegmentFilter.DIE_HARD -> "chip_segment_diehard"
                                }
                                FilterChip(
                                    selected = selectedSegmentFilter == filter,
                                    onClick = { selectedSegmentFilter = filter },
                                    label = {
                                        Text(
                                            text = filter.label,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag(tag)
                                )
                            }
                        }
                    }
                }

                // 3. Decision Reaction Log Strip (Latest Commissioner Impact)
                if (fs.decisionReactions.isNotEmpty() && selectedChannel == FanMediaChannel.ALL) {
                    item(key = "recent_decision_impact_header") {
                        FanSectionTitle(
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            title = "RECENT DECISION REACTIONS",
                            badge = "${fs.decisionReactions.size} Logged",
                            accent = GoldPrimary
                        )
                    }
                    item(key = "recent_decision_impact_rail") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(fs.decisionReactions.take(6), key = { it.id }) { rec ->
                                DecisionReactionSummaryChipCard(rec)
                            }
                        }
                    }
                }

                // 4. Newspaper Headlines Section
                if (selectedChannel == FanMediaChannel.ALL || selectedChannel == FanMediaChannel.NEWSPAPER) {
                    item(key = "newspaper_header") {
                        FanSectionTitle(
                            icon = Icons.Default.Newspaper,
                            title = "NEWSPAPER HEADLINES",
                            badge = "${fs.newspaperHeadlines.size} Editions",
                            accent = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val papersToShow = if (selectedChannel == FanMediaChannel.ALL) {
                        fs.newspaperHeadlines.take(3)
                    } else {
                        fs.newspaperHeadlines
                    }
                    items(papersToShow, key = { it.id }) { paper ->
                        NewspaperHeadlineCard(item = paper)
                    }
                }

                // 5. Sports Talk Radio Segments Section
                if (selectedChannel == FanMediaChannel.ALL || selectedChannel == FanMediaChannel.RADIO) {
                    item(key = "radio_header") {
                        FanSectionTitle(
                            icon = Icons.Default.Radio,
                            title = "SPORTS TALK RADIO SEGMENTS",
                            badge = "${filteredRadio.size} On-Air",
                            accent = AmberWarning
                        )
                    }
                    val radioToShow = if (selectedChannel == FanMediaChannel.ALL) {
                        filteredRadio.take(3)
                    } else {
                        filteredRadio
                    }
                    items(radioToShow, key = { it.id }) { segment ->
                        SportsTalkRadioCard(segment = segment)
                    }
                }

                // 6. Dynamic Social Media Feed Section
                if (selectedChannel == FanMediaChannel.ALL || selectedChannel == FanMediaChannel.SOCIAL) {
                    item(key = "social_header") {
                        FanSectionTitle(
                            icon = Icons.Default.Forum,
                            title = "SOCIAL MEDIA FEED",
                            badge = "${filteredSocial.size} Posts",
                            accent = CyanAccent
                        )
                    }
                    val postsToShow = if (selectedChannel == FanMediaChannel.ALL) {
                        filteredSocial.take(8)
                    } else {
                        filteredSocial
                    }
                    items(postsToShow, key = { it.id }) { post ->
                        SocialMediaPostCard(post = post)
                    }
                }
            }
        }
    }
}

@Composable
private fun FanCohortComparisonCard(
    state: FanSentimentState,
    cashMillions: Long,
    onLaunchCasualInitiative: () -> Unit,
    onLaunchDieHardInitiative: () -> Unit
) {
    val casualAnim by animateFloatAsState(
        targetValue = (state.casualSentimentScore / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 280f),
        label = "casualSentimentBar"
    )
    val dieHardAnim by animateFloatAsState(
        targetValue = (state.dieHardSentimentScore / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 280f),
        label = "dieHardSentimentBar"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_fan_cohort_comparison"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COHORT SENTIMENT TRACKER",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.9.sp
                    )
                    Text(
                        text = "Casual vs. Die-Hard Fanbase Split",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                val polColor = when {
                    state.divergenceGap >= 15 -> CrimsonAlert
                    state.divergenceGap >= 7 -> AmberWarning
                    else -> EmeraldAccent
                }
                Surface(
                    color = polColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, polColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Gap ${state.divergenceGap} pts • ${state.polarizationLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = polColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CohortMeterColumn(
                    title = "CASUAL FANS",
                    score = state.casualSentimentScore,
                    delta = state.lastCasualDelta,
                    mood = state.casualMoodLabel,
                    subtitle = "Highlights, price & spectacle",
                    progress = casualAnim,
                    accent = CyanAccent,
                    testTag = "cohort_meter_casual",
                    modifier = Modifier.weight(1f)
                )
                CohortMeterColumn(
                    title = "DIE-HARD FANS",
                    score = state.dieHardSentimentScore,
                    delta = state.lastDieHardDelta,
                    mood = state.dieHardMoodLabel,
                    subtitle = "Integrity, loyalty & home roots",
                    progress = dieHardAnim,
                    accent = GoldPrimary,
                    testTag = "cohort_meter_diehard",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = state.lastPulseNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLaunchCasualInitiative,
                    enabled = cashMillions >= 3L,
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_initiative_casual")
                ) {
                    Text(
                        text = "Casual Blitz ($3M)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onLaunchDieHardInitiative,
                    enabled = cashMillions >= 3L,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_initiative_diehard")
                ) {
                    Text(
                        text = "Purist Town Hall ($3M)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CohortMeterColumn(
    title: String,
    score: Int,
    delta: Int,
    mood: String,
    subtitle: String,
    progress: Float,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (delta != 0) {
                    val deltaColor = if (delta > 0) EmeraldAccent else CrimsonAlert
                    Text(
                        text = if (delta > 0) "+$delta" else "$delta",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = deltaColor,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "$mood • $subtitle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DecisionReactionSummaryChipCard(record: FanDecisionReactionRecord) {
    val cColor = if (record.casualDelta >= 0) EmeraldAccent else CrimsonAlert
    val dColor = if (record.dieHardDelta >= 0) EmeraldAccent else CrimsonAlert
    val cSign = if (record.casualDelta >= 0) "+${record.casualDelta}" else "${record.casualDelta}"
    val dSign = if (record.dieHardDelta >= 0) "+${record.dieHardDelta}" else "${record.dieHardDelta}"

    Card(
        modifier = Modifier
            .width(240.dp)
            .testTag("reaction_record_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.category.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = record.timestampLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = record.decisionTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Casual $cSign",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = cColor
                )
                Text(
                    text = "Die-Hard $dSign",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = dColor
                )
            }
        }
    }
}

@Composable
private fun NewspaperHeadlineCard(item: NewspaperHeadlineItem) {
    val toneColor = Color(item.tone.badgeColorHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("newspaper_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.publicationName,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = toneColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.sectionKicker,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = toneColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Text(
                text = item.headline,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subheadline,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = GoldPrimary
            )
            Text(
                text = item.leadParagraph,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.byline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = item.timestampLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SportsTalkRadioCard(segment: SportsTalkRadioSegment) {
    val callerColor = Color(segment.callerSegment.accentColorHex)
    val heatColor = when {
        segment.heatednessScore >= 75 -> CrimsonAlert
        segment.heatednessScore >= 55 -> AmberWarning
        else -> CyanAccent
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("radio_card_${segment.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f)),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = CrimsonAlert.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ON AIR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CrimsonAlert,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${segment.showName} • ${segment.hostNames}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Heat ${segment.heatednessScore}/100",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = heatColor
                )
            }

            Text(
                text = segment.segmentTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = segment.hostTake,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = callerColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, callerColor.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CALLER: ${segment.callerName} (${segment.callerCity}) • ${segment.callerSegment.shortLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = callerColor
                    )
                    Text(
                        text = segment.callerQuote,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Call-In Poll: Casual ${segment.casualApprovalPct}% vs Die-Hard ${segment.dieHardApprovalPct}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = segment.timestampLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SocialMediaPostCard(post: SocialMediaPostItem) {
    val segColor = Color(post.segment.accentColorHex)
    val toneColor = Color(post.tone.badgeColorHex)
    val initials = post.displayName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "FN" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("social_post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, segColor.copy(alpha = 0.35f)),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(segColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = segColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = post.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = segColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = post.segment.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = segColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${post.handle} • ${post.timestampLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = toneColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = post.tone.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = toneColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.hashtags.joinToString(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = segColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Reposts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${post.repostsCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Likes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FanSectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    badge: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}
