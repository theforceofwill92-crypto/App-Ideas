package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.model.DashboardSnackbarEvent
import com.example.ui.components.DashboardSnackbarEffect
import com.example.ui.components.DashboardSnackbarHost
import com.example.ui.components.DilemmaDialog
import com.example.ui.components.LaborUnionNegotiationOverlay
import com.example.ui.components.NewLeagueDialog
import com.example.ui.components.RelocationVoteModal
import com.example.ui.components.RivalAbsorptionNegotiationModal
import com.example.ui.components.RivalWatchModal
import com.example.ui.screens.*
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LeagueViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: LeagueViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarAlert by remember { mutableStateOf<DashboardSnackbarEvent?>(null) }

    DashboardSnackbarEffect(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onActiveAlertChange = { activeSnackbarAlert = it }
    )

    if (uiState.activeBoardVoteModal != null) {
        val voteModal = uiState.activeBoardVoteModal!!
        com.example.ui.components.BoardOfGovernorsVoteDialog(
            voteResult = voteModal,
            onDismiss = { viewModel.dismissBoardVoteModal() },
            onExecutiveOverride = if (!voteModal.passed) {
                { viewModel.overrideLastVetoedVote() }
            } else null
        )
    }

    if (uiState.showBroadcastNegotiationsScreen) {
        BroadcastNegotiationsScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showSponsorshipNegotiationsScreen) {
        CorporateSponsorNegotiationsScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showAllStarRedesignScreen) {
        AllStarRedesignScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showOfficiatingScreen) {
        OfficiatingHubScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showRuleCommitteeScreen) {
        RuleCommitteeScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showDisciplinaryDeskScreen) {
        DisciplinaryDeskScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showCrisisEventsScreen) {
        CrisisEventsScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showScheduleCalendarScreen) {
        ScheduleCalendarScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showJobSecurityLegacyScreen) {
        JobSecurityLegacyScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showFanSentimentScreen) {
        FanSentimentScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showLaborUnionScreen) {
        LaborUnionNegotiationsScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.activeExpansionDraftSession != null) {
        ExpansionDraftScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showExpansionRequestsScreen) {
        ExpansionRequestsScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showOffSeasonScreen) {
        OffSeasonSummitScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showTradeDeskScreen) {
        TradeDeskScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showRecordBookScreen) {
        RecordBookScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showDepthChartScreen) {
        DepthChartScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showTradeBlockScreen) {
        TradeBlockScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showRivalryHubScreen) {
        RivalryHubScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showFacilityHubScreen) {
        FacilityHubScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showFreeAgencyPoolScreen) {
        FreeAgencyPoolScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showMinorLeagueScreen) {
        MinorLeagueScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    if (uiState.showLeagueSettingsScreen) {
        CommissionerSettingsScreen(uiState = uiState, viewModel = viewModel)
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            DashboardSnackbarHost(
                hostState = snackbarHostState,
                currentAlert = activeSnackbarAlert
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_apex_logo),
                            contentDescription = "Apex League Crest",
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("app_bar_crest")
                        )
                        Column {
                            Text(
                                text = uiState.leagueName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Commissioner's Desk • ${uiState.sportType.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openLeagueSettings() },
                        modifier = Modifier.testTag("btn_open_league_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Commissioner Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleNewLeagueDialog(true) },
                        modifier = Modifier.testTag("btn_new_league")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "New League",
                            tint = GoldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Desk") },
                    label = { Text("Desk", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_desk")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Teams") },
                    label = { Text("Teams", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_teams")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.Public, contentDescription = "Markets") },
                    label = { Text("Markets", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_markets")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.MonetizationOn, contentDescription = "Finances") },
                    label = { Text("Finances", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_finances")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = { Icon(Icons.Default.Gavel, contentDescription = "Rules") },
                    label = { Text("Rules", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_rules")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 5,
                    onClick = { viewModel.setSelectedTab(5) },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Standings") },
                    label = { Text("Table", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_standings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> HomeScreen(uiState = uiState, viewModel = viewModel)
                1 -> FranchisesScreen(uiState = uiState, viewModel = viewModel)
                2 -> MarketsScreen(uiState = uiState, viewModel = viewModel)
                3 -> FinancesScreen(uiState = uiState, viewModel = viewModel)
                4 -> LeagueRulesScreen(uiState = uiState, viewModel = viewModel)
                5 -> StandingsScheduleScreen(uiState = uiState, viewModel = viewModel)
            }

            // High-Stakes Commissioner Dilemma Modal (if triggered)
            if (uiState.activeDilemma != null) {
                DilemmaDialog(
                    dilemma = uiState.activeDilemma!!,
                    onChoiceSelected = { choiceIndex ->
                        viewModel.resolveDilemma(choiceIndex)
                    }
                )
            }

            // New League Inauguration Dialog
            if (uiState.showNewLeagueDialog) {
                NewLeagueDialog(
                    onDismiss = { viewModel.toggleNewLeagueDialog(false) },
                    onCreateLeague = { name, sport, bg ->
                        viewModel.createNewCustomLeague(name, sport, bg)
                    }
                )
            }

            // Triggered Labor Union Negotiation Overlay
            if (uiState.activeLaborNegotiationOverlay != null) {
                LaborUnionNegotiationOverlay(
                    session = uiState.activeLaborNegotiationOverlay!!,
                    onSelectChoice = { choiceId -> viewModel.resolveLaborNegotiationChoice(choiceId) },
                    onDismiss = { viewModel.dismissLaborNegotiationOverlay() },
                    onOpenCbaTable = {
                        viewModel.dismissLaborNegotiationOverlay()
                        viewModel.openLaborUnionScreen(true)
                    }
                )
            }

            if (uiState.showRivalWatchModal) {
                RivalWatchModal(
                    rivalLeague = uiState.rivalLeague,
                    cashReservesMillions = uiState.cashReservesMillions,
                    ownerApprovalRating = uiState.ownerApprovalRating,
                    unionHappinessPct = uiState.unionAgreement.unionSatisfaction,
                    freeAgentPoolSize = uiState.freeAgentPool.size,
                    onExecuteAction = { action -> viewModel.performRivalWatchAction(action) },
                    onDismiss = { viewModel.openRivalWatchModal(false) }
                )
            }

            uiState.stadiumDealState.activeRelocationVote?.let { voteState ->
                RelocationVoteModal(
                    voteState = voteState,
                    onSelectCity = { cityId -> viewModel.selectRelocationVoteCity(cityId) },
                    onApproveRelocation = { viewModel.resolveRelocationVote(approve = true) },
                    onVetoRelocation = { viewModel.resolveRelocationVote(approve = false) }
                )
            }

            uiState.rivalLeague.activeAbsorptionNegotiation?.let { negotiationEvent ->
                RivalAbsorptionNegotiationModal(
                    event = negotiationEvent,
                    onToggleCandidate = { candId -> viewModel.toggleRivalAbsorptionCandidate(candId) },
                    onAcceptAbsorption = { viewModel.resolveRivalAbsorptionNegotiation(acceptAbsorption = true) },
                    onRejectMerger = { viewModel.resolveRivalAbsorptionNegotiation(acceptAbsorption = false) }
                )
            }
        }
    }
}
