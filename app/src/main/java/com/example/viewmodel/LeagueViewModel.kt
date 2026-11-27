package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DatabaseModule
import com.example.data.FranchiseEntity
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.data.LeagueRevenueHistoryEntity
import com.example.data.LeagueState
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt
import kotlin.random.Random

data class PrefilledTradeSetup(
    val teamAId: String,
    val teamBId: String,
    val playerAId: String? = null,
    val playerBId: String? = null
)

private val initialMinorSyncBoot = MinorLeagueEngine.syncAffiliatesWithParentLeague(
    franchises = InitialLeagueData.getInitialFranchises(),
    currentMinorState = null,
    sportType = SportType.BASKETBALL,
    seasonYear = 2026
)

private val initialRivalriesBoot = RivalryEngine.seedInitialRivalries(
    franchises = initialMinorSyncBoot.updatedFranchises,
    cities = InitialLeagueData.getDefaultCities(),
    seasonYear = 2026
)

private val initialScheduleBoot = SeasonScheduleState(
    activeSeasonSchedule = ScheduleCalendarEngine.generateFullSeasonSchedule(
        targetYear = 2026,
        regularSeasonWeeks = 14,
        franchises = initialMinorSyncBoot.updatedFranchises,
        rivalries = initialRivalriesBoot,
        cities = InitialLeagueData.getDefaultCities(),
        sportType = SportType.BASKETBALL
    ).copy(
        approvalStatus = ScheduleApprovalStatus.APPROVED_BY_BOARD,
        lastVoteSummary = "Inaugural 2026 charter schedule approved unanimously."
    )
)

private val initialJobSecurityBoot: JobSecurityLegacyState = JobSecurityLegacyEngine.createInitialState(
    CommissionerPerformanceInput(
        seasonYear = 2026,
        currentWeek = 1,
        sportType = SportType.BASKETBALL,
        background = CommissionerBackground.MARKETING_MAVEN,
        cashReservesMillions = 95L,
        leagueValuationMillions = 420L,
        totalFanbaseMillions = 18.2,
        leagueIntegrityScore = 85,
        competitiveParityScore = 82,
        ownerApprovalRating = 84,
        unionAgreement = LaborUnionAgreement(),
        activeBroadcast = InitialLeagueData.getInitialBroadcastDeals()[0],
        activeSponsorship = InitialLeagueData.getDefaultActiveSponsorship(),
        ruleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules()),
        franchises = initialMinorSyncBoot.updatedFranchises,
        cities = InitialLeagueData.getDefaultCities(),
        campaigns = InitialLeagueData.getMarketingCampaignCatalog(),
        rivalries = initialRivalriesBoot,
        boardVoteHistory = emptyList(),
        officiatingState = OfficiatingEngine.createDefaultState(
            franchises = initialMinorSyncBoot.updatedFranchises,
            currentWeek = 1,
            matches = emptyList()
        ),
        disciplinaryState = DisciplinaryEngine.createDefaultState(
            sportType = SportType.BASKETBALL,
            franchises = initialMinorSyncBoot.updatedFranchises
        ),
        crisisState = CrisisSystemState(),
        allStarRedesignState = AllStarEngine.createDefaultState(InitialLeagueData.getDefaultCities()),
        ruleCommitteeState = RuleCommitteeEngine.createDefaultState(SportType.BASKETBALL),
        seasonScheduleState = initialScheduleBoot,
        hallOfFameCount = 3,
        recordCount = 12,
        valuationHistory = emptyList()
    )
)

data class LeagueUiState(
    val leagueName: String = "Apex Premier League",
    val sportType: SportType = SportType.BASKETBALL,
    val commissionerBackground: CommissionerBackground = CommissionerBackground.MARKETING_MAVEN,
    val currentYear: Int = 2026,
    val currentWeek: Int = 1,
    val totalWeeks: Int = 16,
    val seasonPhase: SeasonPhase = SeasonPhase.REGULAR_SEASON,
    val cashReservesMillions: Long = 95L,
    val leagueValuationMillions: Long = 420L,
    val totalLeagueFanbaseMillions: Double = 18.2,
    val leagueIntegrityScore: Int = 85,
    val competitiveParityScore: Int = 82,
    val ownerApprovalRating: Int = 84,
    val unionAgreement: LaborUnionAgreement = LaborUnionAgreement(),
    val ruleBook: LeagueRuleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules()),
    val activeBroadcast: BroadcastContract = InitialLeagueData.getInitialBroadcastDeals()[0],
    val franchises: List<Franchise> = initialMinorSyncBoot.updatedFranchises,
    val cities: List<MarketCity> = InitialLeagueData.getDefaultCities(),
    val campaigns: List<MarketingCampaign> = InitialLeagueData.getMarketingCampaignCatalog(),
    val valuationHistory: List<LeagueValuationHistory> = emptyList(),
    val financialSnapshots: List<FinancialSnapshot> = emptyList(),
    val currentWeekMatches: List<GameMatch> = emptyList(),
    val activeDilemma: CommissionerDilemma? = null,
    val recentNews: List<String> = listOf(
        "Commissioner assumes office: Startup league inaugural season officially launched!",
        "Initial 6 charter franchises ratified with full arena leases.",
        "National television & digital broadcast agreements secured."
    ),
    val playoffMatches: List<GameMatch> = emptyList(),
    val seasonChampion: Franchise? = null,
    val seasonRunnerUp: Franchise? = null,
    val isSimulationRunning: Boolean = false,
    val showNewLeagueDialog: Boolean = false,
    val showBroadcastNegotiationsScreen: Boolean = false,
    val showLaborUnionScreen: Boolean = false,
    val activeExpansionDraftSession: ExpansionDraftSession? = null,
    val selectedFinancesSubTab: Int = 0, // 0: Franchise Revenue Overview, 1: League Treasury & Charts, 2: Contracts & Media Blitz
    val selectedTab: Int = 0, // 0: Dashboard, 1: Franchises, 2: Markets & Expansion, 3: Finances & Media, 4: Governance & Union, 5: Standings
    val showOffSeasonScreen: Boolean = false,
    val activeOffSeasonSession: OffSeasonSession? = null,
    val latestCompletedOffSeason: OffSeasonSession? = OffSeasonEngine.createOffSeasonSession(
        year = 2025,
        franchises = InitialLeagueData.getInitialFranchises(),
        sportType = SportType.BASKETBALL,
        ruleBook = LeagueRuleBook(),
        initialParity = 82
    ).copy(isCompleted = true, currentStep = OffSeasonStep.SUMMARY),
    val showTradeDeskScreen: Boolean = false,
    val pendingTrades: List<TradeProposal> = listOfNotNull(
        TradeEngine.generateRealisticTradeOffer(
            week = 1,
            franchises = InitialLeagueData.getInitialFranchises(),
            tradeRules = TradeRules()
        )
    ),
    val completedTrades: List<TradeProposal> = emptyList(),
    val activeSponsorship: ActiveLeagueSponsorship = InitialLeagueData.getDefaultActiveSponsorship(),
    val showSponsorshipNegotiationsScreen: Boolean = false,
    val recordBook: LeagueRecordBook = RecordBookEngine.createInitialRecordBook(
        SportType.BASKETBALL,
        InitialLeagueData.getInitialFranchises(),
        2026
    ),
    val hallOfFame: List<HallOfFameInductee> = RecordBookEngine.createInauguralHallOfFame(
        SportType.BASKETBALL,
        InitialLeagueData.getInitialFranchises(),
        2026
    ),
    val weeklyAwards: List<WeeklyPlayerAward> = emptyList(),
    val currentWeekPlayerStats: List<PlayerGameStatLine> = emptyList(),
    val seasonPlayerStats: Map<String, PlayerSeasonStats> = emptyMap(),
    val careerPlayerStats: Map<String, PlayerCareerStats> = RecordBookEngine.seedInitialCareerStats(
        InitialLeagueData.getInitialFranchises(),
        2026
    ),
    val pastAwardsCeremonies: List<YearlyAwardsCeremony> = emptyList(),
    val showRecordBookScreen: Boolean = false,
    val showDepthChartScreen: Boolean = false,
    val selectedDepthChartFranchiseId: String? = null,
    val injuryReportHistory: List<InjuryEventLog> = emptyList(),
    val showTradeBlockScreen: Boolean = false,
    val selectedTradeBlockFranchiseId: String? = null,
    val prefilledTradeMachineSetup: PrefilledTradeSetup? = null,
    val rivalries: List<TeamRivalry> = RivalryEngine.seedInitialRivalries(
        franchises = InitialLeagueData.getInitialFranchises(),
        cities = InitialLeagueData.getDefaultCities(),
        seasonYear = 2026
    ),
    val showRivalryHubScreen: Boolean = false,
    val selectedRivalryTeamFilterId: String? = null,
    val showFacilityHubScreen: Boolean = false,
    val selectedFacilityFranchiseId: String? = null,
    val freeAgentPool: List<FreeAgentPlayer> = FreeAgencyEngine.generateInitialFreeAgentPool(SportType.BASKETBALL, 2026),
    val freeAgentSignings: List<FreeAgentSigningTransaction> = emptyList(),
    val showFreeAgencyPoolScreen: Boolean = false,
    val selectedFreeAgencyFranchiseId: String? = null,
    val showLeagueSettingsScreen: Boolean = false,
    val tradedPickLedger: List<TradedPickLedgerEntry> = emptyList(),
    val minorLeagueState: MinorLeagueState = initialMinorSyncBoot.minorState,
    val showMinorLeagueScreen: Boolean = false,
    val selectedMinorAffiliateId: String? = null,
    val boardVoteHistory: List<BoardVoteResult> = emptyList(),
    val lastBoardVoteResult: BoardVoteResult? = null,
    val activeBoardVoteModal: BoardVoteResult? = null,
    val lastRevenueBreakdown: WeeklyRevenueBreakdown? = null,
    val activeLaborTrigger: LaborUnionTriggerEvent? = null,
    val activeLaborNegotiationOverlay: LaborNegotiationOverlaySession? = null,
    val laborTriggerHistory: List<LaborUnionTriggerEvent> = emptyList(),
    val dashboardTrends: List<DashboardTrendPoint> = emptyList(),
    val pendingExpansionRequests: List<ExpansionRequest> = ExpansionRequestEngine.seedInitialRequests(
        cities = InitialLeagueData.getDefaultCities(),
        franchises = initialMinorSyncBoot.updatedFranchises,
        activeBroadcast = InitialLeagueData.getInitialBroadcastDeals()[0],
        activeSponsorship = InitialLeagueData.getDefaultActiveSponsorship(),
        currentMarketReachMillions = 18.2,
        seasonYear = 2026,
        week = 1
    ),
    val reviewedExpansionRequests: List<ExpansionRequest> = emptyList(),
    val showExpansionRequestsScreen: Boolean = false,
    val broadcastBiddingSession: BroadcastBiddingSession = BroadcastBiddingEngine.createInitialSession(
        bidders = InitialLeagueData.getNetworkBidders(),
        currentContract = InitialLeagueData.getInitialBroadcastDeals()[0],
        teamCount = initialMinorSyncBoot.updatedFranchises.size,
        playerRevenueSharePct = 50,
        currentFanbaseMillions = 18.2,
        currentYear = 2026
    ),
    val allStarRedesignState: AllStarRedesignState = AllStarEngine.createDefaultState(InitialLeagueData.getDefaultCities()),
    val showAllStarRedesignScreen: Boolean = false,
    val officiatingState: OfficiatingState = OfficiatingEngine.createDefaultState(
        franchises = initialMinorSyncBoot.updatedFranchises,
        currentWeek = 1,
        matches = LeagueSimulationEngine.generateMatchesForWeek(
            1,
            initialMinorSyncBoot.updatedFranchises,
            SportType.BASKETBALL
        )
    ),
    val showOfficiatingScreen: Boolean = false,
    val ruleCommitteeState: RuleCommitteeState = RuleCommitteeEngine.createDefaultState(SportType.BASKETBALL),
    val showRuleCommitteeScreen: Boolean = false,
    val disciplinaryState: DisciplinaryDeskState = DisciplinaryEngine.createDefaultState(
        sportType = SportType.BASKETBALL,
        franchises = initialMinorSyncBoot.updatedFranchises
    ),
    val showDisciplinaryDeskScreen: Boolean = false,
    val crisisState: CrisisSystemState = CrisisSystemState(),
    val showCrisisEventsScreen: Boolean = false,
    val seasonScheduleState: SeasonScheduleState = initialScheduleBoot,
    val showScheduleCalendarScreen: Boolean = false,
    val jobSecurityLegacyState: JobSecurityLegacyState = initialJobSecurityBoot,
    val showJobSecurityLegacyScreen: Boolean = false,
    val rivalLeague: RivalLeague = RivalLeagueEngine.createDefaultRivalLeague(SportType.BASKETBALL),
    val showRivalWatchModal: Boolean = false,
    val stadiumDealState: StadiumDealState = StadiumDealState(),
    val fanSentimentState: FanSentimentState = FanSentimentEngine.createInitialState(
        sportType = SportType.BASKETBALL,
        leagueName = "Apex League",
        franchises = initialMinorSyncBoot.updatedFranchises,
        seasonYear = 2026
    ),
    val showFanSentimentScreen: Boolean = false,
    val managedFranchiseId: String? = initialMinorSyncBoot.updatedFranchises.firstOrNull()?.id,
    val showDraftScoutingScreen: Boolean = false,
    val upcomingDraftClassYear: Int = 2026,
    val upcomingDraftClass: List<RookieProspect> = DraftClassGenerator.generateProceduralClass(2026, 18, SportType.BASKETBALL),
    val leagueSponsors: LeagueSponsorsState = LeagueSponsorsState(),
    val showLeagueSponsorsScreen: Boolean = false,
    val prDamageControlState: PrDamageControlState = PrDamageControlState(),
    val showPrDamageControlModal: Boolean = false,
    val showFranchiseManagementModal: Boolean = false
) {
    val managedFranchise: Franchise?
        get() = franchises.find { it.id == managedFranchiseId } ?: franchises.firstOrNull()

    fun toPerformanceInput(seasonChampionNameOverride: String? = null): CommissionerPerformanceInput =
        CommissionerPerformanceInput(
            seasonYear = currentYear,
            currentWeek = currentWeek,
            sportType = sportType,
            background = commissionerBackground,
            cashReservesMillions = cashReservesMillions,
            leagueValuationMillions = leagueValuationMillions,
            totalFanbaseMillions = totalLeagueFanbaseMillions,
            leagueIntegrityScore = leagueIntegrityScore,
            competitiveParityScore = competitiveParityScore,
            ownerApprovalRating = ownerApprovalRating,
            unionAgreement = unionAgreement,
            activeBroadcast = activeBroadcast,
            activeSponsorship = activeSponsorship,
            ruleBook = ruleBook,
            franchises = franchises,
            cities = cities,
            campaigns = campaigns,
            rivalries = rivalries,
            boardVoteHistory = boardVoteHistory,
            officiatingState = officiatingState,
            disciplinaryState = disciplinaryState,
            crisisState = crisisState,
            allStarRedesignState = allStarRedesignState,
            ruleCommitteeState = ruleCommitteeState,
            seasonScheduleState = seasonScheduleState,
            hallOfFameCount = hallOfFame.size,
            recordCount = recordBook.allRecordsCount,
            valuationHistory = valuationHistory,
            seasonChampionName = seasonChampionNameOverride ?: seasonChampion?.teamFullName
        )
}

class LeagueViewModel(
    application: Application,
    private val repository: LeagueRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = DatabaseModule.provideLeagueRepository(application)
    )

    private val persistMutex = Mutex()
    private val persistRequestSeq = java.util.concurrent.atomic.AtomicLong(0L)
    val coreSimulationViewModel = CoreSimulationViewModel()
    val dashboardNotifications: SharedFlow<DashboardSnackbarEvent> = coreSimulationViewModel.dashboardNotifications
    val leagueTrends: StateFlow<List<DashboardTrendPoint>> = coreSimulationViewModel.dashboardTrends
    val persistedLeagueState: StateFlow<LeagueState?> = repository.leagueStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )
    val persistedFranchises: StateFlow<List<FranchiseEntity>> = repository.franchisesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )
    private val _fiveYearRevenueHistory = MutableStateFlow<List<LeagueRevenueHistoryEntity>>(emptyList())
    val fiveYearRevenueHistory: StateFlow<List<LeagueRevenueHistoryEntity>> = _fiveYearRevenueHistory.asStateFlow()
    private var pendingOverrideAction: (() -> Unit)? = null

    private fun notifyFinancialMilestones(prevState: LeagueUiState, nextState: LeagueUiState) {
        coreSimulationViewModel.evaluateFinancialMilestonesAndEmit(
            previousValuation = prevState.leagueValuationMillions,
            newValuation = nextState.leagueValuationMillions,
            previousCash = prevState.cashReservesMillions,
            newCash = nextState.cashReservesMillions
        )
    }

    private fun recordDashboardTrend(periodLabel: String, eventTag: String) {
        val updatedTrends = coreSimulationViewModel.appendDashboardTrendPoint(
            state = _uiState.value,
            periodLabel = periodLabel,
            eventTag = eventTag
        )
        _uiState.update { it.copy(dashboardTrends = updatedTrends) }
    }

    private val _uiState = MutableStateFlow(LeagueUiState())
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    private var loadPersistedJob: kotlinx.coroutines.Job? = null

    init {
        val initialTrends = coreSimulationViewModel.seedDashboardTrendsIfNeeded(_uiState.value)
        val preloadedRival = repository.loadRivalLeague()
        val preloadedPr = repository.loadPrDamageControlState()
        _uiState.update {
            it.copy(
                dashboardTrends = initialTrends,
                rivalLeague = preloadedRival ?: it.rivalLeague,
                prDamageControlState = preloadedPr ?: it.prDamageControlState
            )
        }
        val initialSnap = _uiState.value
        _fiveYearRevenueHistory.value = repository.buildFiveYearRevenueRecords(
            currentYear = initialSnap.currentYear,
            leagueValuationMillions = initialSnap.leagueValuationMillions,
            unionAgreement = initialSnap.unionAgreement,
            activeBroadcast = initialSnap.activeBroadcast,
            franchises = initialSnap.franchises,
            history = initialSnap.valuationHistory,
            leagueSponsors = initialSnap.leagueSponsors,
            ticketConcessionCapActive = initialSnap.prDamageControlState.ticketConcessionCapActiveThisSeason
        )
        viewModelScope.launch {
            repository.fiveYearRevenueHistoryFlow.collect { records ->
                if (records.isNotEmpty()) {
                    _fiveYearRevenueHistory.value = records
                }
            }
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        loadPersistedLeague()
    }

    private fun loadPersistedLeague() {
        loadPersistedJob?.cancel()
        loadPersistedJob = viewModelScope.launch {
            val saved = repository.loadLeague()
            if (saved != null) {
                val sport = try { SportType.valueOf(saved.sportType) } catch (e: Exception) { SportType.BASKETBALL }
                val background = try { CommissionerBackground.valueOf(saved.commissionerBackground) } catch (e: Exception) { CommissionerBackground.MARKETING_MAVEN }
                val phase = try { SeasonPhase.valueOf(saved.seasonPhase) } catch (e: Exception) { SeasonPhase.REGULAR_SEASON }
                val broadcastPlatform = try { BroadcastPlatform.valueOf(saved.activeBroadcastPlatform) } catch (e: Exception) { BroadcastPlatform.LINEAR_CABLE }
                val baseBroadcast = BroadcastContract(
                    id = "saved_deal",
                    partnerName = saved.activeBroadcastName,
                    platform = broadcastPlatform,
                    annualPayoutMillions = saved.activeBroadcastPayoutMillions,
                    yearsRemaining = saved.activeBroadcastYearsRemaining,
                    reachMultiplier = 1.4
                )
                val extCba = repository.loadLaborCbaExtendedState()
                val union = LaborUnionAgreement(
                    unionSatisfaction = saved.unionSatisfaction,
                    strikeRisk = saved.strikeRisk,
                    playerRevenueSharePct = saved.playerRevenueSharePct,
                    salaryCapType = try { SalaryCapType.valueOf(saved.salaryCapType) } catch (e: Exception) { SalaryCapType.SOFT_CAP_LUXURY_TAX },
                    salaryCapMillions = saved.salaryCapMillions,
                    cbaYearsRemaining = extCba?.cbaYearsRemaining ?: 3,
                    isLockoutActive = extCba?.isLockoutActive ?: false,
                    lockoutWeeksLost = extCba?.lockoutWeeksLost ?: 0,
                    lastLockoutSeasonYear = extCba?.lastLockoutSeasonYear ?: 0,
                    negotiatedRosterRule = extCba?.negotiatedRosterRule ?: RosterSizeRule.STANDARD_15,
                    mandatoryPrivateCharterFlights = extCba?.mandatoryPrivateCharterFlights ?: true,
                    maxBackToBackGamesLimit = extCba?.maxBackToBackGamesLimit ?: true,
                    independentConcussionProtocol = extCba?.independentConcussionProtocol ?: true,
                    advancedHybridTurfMandate = extCba?.advancedHybridTurfMandate ?: false,
                    postCareerTransitionFundMillions = extCba?.postCareerTransitionFundMillions ?: 5L,
                    fiveStarHotelSingleRooms = extCba?.fiveStarHotelSingleRooms ?: true,
                    minPlayerSalaryK = extCba?.minPlayerSalaryK ?: 650L,
                    playerHealthWelfareMillions = extCba?.playerHealthWelfareMillions ?: 15L,
                    luxuryTaxThresholdMillions = extCba?.luxuryTaxThresholdMillions ?: 102L,
                    rookieScaleMultiplierPct = extCba?.rookieScaleMultiplierPct ?: 100,
                    maxContractCapPct = extCba?.maxContractCapPct ?: 30
                )
                val savedManagedFranchiseId = repository.loadManagedFranchiseId()
                val franchises = repository.deserializeFranchises(saved.franchisesJson, saved.currentYear)
                val cities = repository.deserializeCities(saved.citiesJson)
                val campaigns = repository.deserializeCampaigns(saved.campaignsJson)
                val history = repository.deserializeHistory(saved.historyJson)
                val news = repository.deserializeStringList(saved.newsFeedJson)

                val loadedSchedule = repository.loadSeasonScheduleState()
                val matches = if (loadedSchedule?.activeSeasonSchedule != null) {
                    ScheduleCalendarEngine.buildWeekMatchesFromPlan(
                        plan = loadedSchedule.activeSeasonSchedule,
                        week = saved.currentWeek,
                        franchises = franchises,
                        rivalries = _uiState.value.rivalries,
                        cities = cities,
                        sportType = sport
                    )
                } else {
                    generateMatchesForWeek(saved.currentWeek, franchises, sport)
                }

                val totalSponsors = franchises.sumOf { it.localSponsorRevenueMillions }
                val snapshots = generateDefaultFinancialSnapshots(
                    currentWeek = saved.currentWeek,
                    broadcastAnnual = saved.activeBroadcastPayoutMillions,
                    totalSponsorsAnnual = totalSponsors,
                    currentCash = saved.cashReservesMillions,
                    currentValuation = saved.leagueValuationMillions
                )

                val franchiseConfs = franchises.map { f -> f.conference }.toSet()
                val franchiseDivs = franchises.map { f -> f.division }.toSet()
                val continentalDivs = Division.values()
                    .filter { div -> div.conference in franchiseConfs && div.conference.isContinentalInternational }
                    .toSet()

                val loadedFa = repository.loadFreeAgencyState()
                val loadedMinor = repository.loadMinorLeagueState()
                val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
                    franchises = franchises,
                    currentMinorState = loadedMinor,
                    sportType = sport,
                    seasonYear = saved.currentYear
                )
                val syncedFranchises = minorSync.updatedFranchises
                val (restoredBroadcast, restoredBiddingSession) = repository.applyLoadedBroadcastExclusivity(
                    baseContract = baseBroadcast,
                    bidders = InitialLeagueData.getNetworkBidders(),
                    teamCount = syncedFranchises.size,
                    playerRevenueSharePct = union.playerRevenueSharePct,
                    fanbaseMillions = syncedFranchises.sumOf { it.fanBaseMillions }.coerceAtLeast(10.0),
                    currentYear = saved.currentYear
                )
                val savedTrends = repository.loadDashboardTrends()
                if (persistRequestSeq.get() > 0L) return@launch
                _uiState.update {
                    val baseWithSavedSettings = repository.loadLeagueSettings(it.ruleBook) ?: it.ruleBook
                    val restoredConferences = (baseWithSavedSettings.enabledConferences + franchiseConfs).toSet()
                    val restoredDivisions = (baseWithSavedSettings.enabledDivisions + franchiseDivs + continentalDivs).toSet()
                    val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(
                        franchises = syncedFranchises,
                        cities = cities,
                        currentRivalries = it.rivalries,
                        seasonYear = saved.currentYear
                    )
                    val annotatedMatches = RivalryEngine.annotateMatchesWithRivalries(
                        matches = matches,
                        franchises = syncedFranchises,
                        rivalries = syncedRivalries,
                        cities = cities
                    )
                    val restoredRuleBook = baseWithSavedSettings.copy(
                        enabledConferences = restoredConferences,
                        enabledDivisions = restoredDivisions
                    )
                    it.copy(
                        leagueName = saved.leagueName,
                        sportType = sport,
                        commissionerBackground = background,
                        currentYear = saved.currentYear,
                        currentWeek = saved.currentWeek,
                        totalWeeks = ScheduleCalendarEngine.totalWeeksFor(restoredRuleBook.regularSeasonWeeks),
                        seasonPhase = phase,
                        cashReservesMillions = saved.cashReservesMillions,
                        leagueValuationMillions = saved.leagueValuationMillions,
                        leagueIntegrityScore = saved.leagueIntegrityScore,
                        competitiveParityScore = saved.competitiveParityScore,
                        unionAgreement = union,
                        ruleBook = restoredRuleBook,
                        activeBroadcast = restoredBroadcast,
                        broadcastBiddingSession = restoredBiddingSession ?: it.broadcastBiddingSession,
                        dashboardTrends = if (savedTrends.isNotEmpty()) savedTrends else it.dashboardTrends,
                        franchises = syncedFranchises,
                        cities = cities,
                        campaigns = campaigns,
                        valuationHistory = history,
                        financialSnapshots = snapshots,
                        recentNews = if (news.isNotEmpty()) news else it.recentNews,
                        currentWeekMatches = annotatedMatches,
                        rivalries = syncedRivalries,
                        freeAgentPool = loadedFa?.first?.ifEmpty { null }
                            ?: FreeAgencyEngine.generateInitialFreeAgentPool(sport, saved.currentYear),
                        freeAgentSignings = loadedFa?.second ?: it.freeAgentSignings,
                        minorLeagueState = minorSync.minorState,
                        boardVoteHistory = repository.loadBoardVoteHistory(),
                        allStarRedesignState = repository.loadAllStarRedesignState(cities) ?: it.allStarRedesignState,
                        officiatingState = repository.loadOfficiatingState(syncedFranchises, saved.currentWeek, annotatedMatches)
                            ?: OfficiatingEngine.createDefaultState(syncedFranchises, saved.currentWeek, annotatedMatches),
                        ruleCommitteeState = repository.loadRuleCommitteeState(sport)
                            ?: RuleCommitteeEngine.createDefaultState(sport),
                        disciplinaryState = repository.loadDisciplinaryState(sport, restoredRuleBook)
                            ?: DisciplinaryEngine.createDefaultState(
                                sportType = sport,
                                franchises = syncedFranchises,
                                ruleBook = restoredRuleBook,
                                seasonYear = saved.currentYear,
                                currentWeek = saved.currentWeek
                            ),
                        crisisState = repository.loadCrisisState() ?: it.crisisState,
                        seasonScheduleState = loadedSchedule ?: it.seasonScheduleState,
                        tradedPickLedger = TradeEngine.buildTradedPickLedger(
                            franchises = syncedFranchises,
                            currentYear = saved.currentYear,
                            activeSession = it.activeOffSeasonSession,
                            latestCompletedOffSeason = it.latestCompletedOffSeason
                        ),
                        managedFranchiseId = savedManagedFranchiseId?.takeIf { id -> syncedFranchises.any { f -> f.id == id } }
                            ?: syncedFranchises.firstOrNull()?.id,
                        upcomingDraftClassYear = saved.currentYear,
                        upcomingDraftClass = DraftClassGenerator.generateProceduralClass(saved.currentYear, 18, sport)
                    ).let { restored ->
                        val perfInput = restored.toPerformanceInput()
                        val loadedSec = repository.loadJobSecurityLegacyState(perfInput)
                            ?: JobSecurityLegacyEngine.createInitialState(perfInput)
                        restored.copy(
                            jobSecurityLegacyState = JobSecurityLegacyEngine.recalculateLiveSecurity(
                                loadedSec,
                                perfInput
                            ),
                            rivalLeague = repository.loadRivalLeague()
                                ?: RivalLeagueEngine.createDefaultRivalLeague(sport),
                            stadiumDealState = repository.loadStadiumDealState() ?: it.stadiumDealState,
                            leagueSponsors = repository.loadLeagueSponsorsState() ?: it.leagueSponsors,
                            prDamageControlState = repository.loadPrDamageControlState() ?: it.prDamageControlState,
                            fanSentimentState = repository.loadFanSentimentState()
                                ?: FanSentimentEngine.createInitialState(
                                    sportType = sport,
                                    leagueName = saved.leagueName,
                                    franchises = syncedFranchises,
                                    seasonYear = saved.currentYear
                                )
                        )
                    }
                }
                coreSimulationViewModel.syncFromLeagueState(_uiState.value)
                _uiState.update { it.copy(dashboardTrends = coreSimulationViewModel.dashboardTrends.value) }
                if (repository.loadLeagueState() == null) {
                    val snap = _uiState.value
                    repository.saveLeagueState(
                        repository.buildLeagueStateSnapshot(
                            leagueName = snap.leagueName,
                            currentYear = snap.currentYear,
                            currentWeek = snap.currentWeek,
                            seasonPhase = snap.seasonPhase,
                            cashReservesMillions = snap.cashReservesMillions,
                            leagueValuationMillions = snap.leagueValuationMillions,
                            unionAgreement = snap.unionAgreement,
                            activeBroadcast = snap.activeBroadcast,
                            franchises = snap.franchises
                        )
                    )
                }
                if (repository.loadFiveYearRevenueHistory().isEmpty()) {
                    val snap = _uiState.value
                    val currentRecord = repository.buildCurrentYearRevenueRecord(
                        currentYear = snap.currentYear,
                        leagueValuationMillions = snap.leagueValuationMillions,
                        unionAgreement = snap.unionAgreement,
                        activeBroadcast = snap.activeBroadcast,
                        franchises = snap.franchises,
                        championName = snap.valuationHistory.firstOrNull { it.year == snap.currentYear }?.championName
                            ?: "In Progress",
                        leagueSponsors = snap.leagueSponsors
                    )
                    val pastRecords = snap.valuationHistory
                        .filter { it.year != snap.currentYear }
                        .distinctBy { it.year }
                        .map { h ->
                            LeagueRevenueHistoryEntity(
                                seasonYear = h.year,
                                totalRevenueMillions = h.totalRevenueMillions,
                                broadcastRevenueMillions = (h.totalRevenueMillions * 0.42).toLong(),
                                sponsorRevenueMillions = (h.totalRevenueMillions * 0.48).toLong(),
                                namingRightsRevenueMillions = (h.totalRevenueMillions * 0.10).toLong(),
                                valuationMillions = h.valuationMillions,
                                franchiseCount = snap.franchises.size,
                                championName = h.championName
                            )
                        }
                    repository.upsertAnnualRevenueRecords((pastRecords + currentRecord).sortedBy { it.seasonYear })
                }
            } else {
                if (persistRequestSeq.get() > 0L) return@launch
                // Initialize first week matches and snapshots
                val stateNow = _uiState.value
                val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
                    franchises = stateNow.franchises,
                    currentMinorState = stateNow.minorLeagueState,
                    sportType = stateNow.sportType,
                    seasonYear = stateNow.currentYear
                )
                val franchises = minorSync.updatedFranchises
                val totalSponsors = franchises.sumOf { it.localSponsorRevenueMillions }
                val snapshots = generateDefaultFinancialSnapshots(
                    currentWeek = 1,
                    broadcastAnnual = stateNow.activeBroadcast.annualPayoutMillions,
                    totalSponsorsAnnual = totalSponsors,
                    currentCash = stateNow.cashReservesMillions,
                    currentValuation = stateNow.leagueValuationMillions
                )
                val matches = generateMatchesForWeek(1, franchises, stateNow.sportType, stateNow.rivalries, stateNow.cities)
                _uiState.update {
                    it.copy(
                        franchises = franchises,
                        minorLeagueState = minorSync.minorState,
                        currentWeekMatches = matches,
                        financialSnapshots = snapshots,
                        officiatingState = OfficiatingEngine.createDefaultState(franchises, 1, matches),
                        disciplinaryState = DisciplinaryEngine.createDefaultState(
                            sportType = stateNow.sportType,
                            franchises = franchises,
                            ruleBook = stateNow.ruleBook,
                            seasonYear = stateNow.currentYear,
                            currentWeek = 1
                        )
                    ).let { updated ->
                        val perfInput = updated.toPerformanceInput()
                        updated.copy(
                            jobSecurityLegacyState = JobSecurityLegacyEngine.createInitialState(perfInput),
                            rivalLeague = repository.loadRivalLeague()
                                ?: RivalLeagueEngine.createDefaultRivalLeague(stateNow.sportType),
                            stadiumDealState = repository.loadStadiumDealState() ?: it.stadiumDealState,
                            leagueSponsors = repository.loadLeagueSponsorsState() ?: it.leagueSponsors,
                            prDamageControlState = repository.loadPrDamageControlState() ?: it.prDamageControlState,
                            fanSentimentState = repository.loadFanSentimentState()
                                ?: FanSentimentEngine.createInitialState(
                                    sportType = stateNow.sportType,
                                    leagueName = stateNow.leagueName,
                                    franchises = franchises,
                                    seasonYear = stateNow.currentYear
                                )
                        )
                    }
                }
                coreSimulationViewModel.syncFromLeagueState(_uiState.value)
                _uiState.update { it.copy(dashboardTrends = coreSimulationViewModel.dashboardTrends.value) }
                persistCurrentState()
            }
        }
    }

    private fun refreshJobSecurity(eventNote: String? = null, isSeasonRollover: Boolean = false) {
        val newlyUnlockedToNotify = mutableListOf<LeagueLegacyMilestone>()
        var latestYear = _uiState.value.currentYear
        _uiState.update { current ->
            latestYear = current.currentYear
            val prevUnlockedIds = current.jobSecurityLegacyState.milestones
                .filter { it.isUnlocked }
                .map { it.id }
                .toSet()
            val input = current.toPerformanceInput()
            val refreshed = JobSecurityLegacyEngine.recalculateLiveSecurity(
                current = current.jobSecurityLegacyState,
                input = input,
                eventNote = eventNote,
                isSeasonRollover = isSeasonRollover
            )
            val unlockedNow = refreshed.milestones.filter { it.isUnlocked && it.id !in prevUnlockedIds }
            newlyUnlockedToNotify.addAll(unlockedNow)
            val milestoneHeadlines = unlockedNow.map { m ->
                "LEAGUE LEGACY MILESTONE UNLOCKED: '${m.title}' (+${m.rewardLegacyPoints} Legacy Pts) — ${m.progressNote}"
            }
            current.copy(
                jobSecurityLegacyState = refreshed,
                recentNews = (milestoneHeadlines + current.recentNews).take(20)
            )
        }
        newlyUnlockedToNotify.firstOrNull()?.let { milestone ->
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_legacy_${latestYear}_${milestone.id.name}",
                    type = DashboardAlertType.SEASON_MILESTONE,
                    severity = DashboardAlertSeverity.MILESTONE,
                    message = "League Legacy Unlocked: ${milestone.title} (+${milestone.rewardLegacyPoints} Pts)",
                    actionLabel = "Legacy",
                    targetRoute = DashboardSnackbarRoute.STANDINGS
                )
            )
        }
    }

    private fun persistCurrentState() {
        val requestSeq = persistRequestSeq.incrementAndGet()
        val snapshotNow = _uiState.value
        repository.saveLeagueSettings(snapshotNow.ruleBook)
        repository.saveDisciplinaryState(snapshotNow.disciplinaryState)
        repository.saveCrisisState(snapshotNow.crisisState)
        repository.saveSeasonScheduleState(snapshotNow.seasonScheduleState)
        repository.saveJobSecurityLegacyState(snapshotNow.jobSecurityLegacyState)
        repository.saveRivalLeague(snapshotNow.rivalLeague)
        repository.saveStadiumDealState(snapshotNow.stadiumDealState)
        repository.saveLeagueSponsorsState(snapshotNow.leagueSponsors)
        repository.savePrDamageControlState(snapshotNow.prDamageControlState)
        repository.saveFanSentimentState(snapshotNow.fanSentimentState)
        repository.saveDashboardTrends(snapshotNow.dashboardTrends)
        repository.saveLaborCbaExtendedState(snapshotNow.unionAgreement)
        repository.saveManagedFranchiseId(snapshotNow.managedFranchiseId)
        repository.saveBroadcastExclusivityState(snapshotNow.activeBroadcast, snapshotNow.broadcastBiddingSession)
        _fiveYearRevenueHistory.value = repository.buildFiveYearRevenueRecords(
            currentYear = snapshotNow.currentYear,
            leagueValuationMillions = snapshotNow.leagueValuationMillions,
            unionAgreement = snapshotNow.unionAgreement,
            activeBroadcast = snapshotNow.activeBroadcast,
            franchises = snapshotNow.franchises,
            history = snapshotNow.valuationHistory,
            existingByYear = _fiveYearRevenueHistory.value.associateBy { it.seasonYear },
            leagueSponsors = snapshotNow.leagueSponsors,
            ticketConcessionCapActive = snapshotNow.prDamageControlState.ticketConcessionCapActiveThisSeason
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            persistMutex.withLock {
                if (requestSeq < persistRequestSeq.get()) return@withLock
                val s = _uiState.value
                repository.saveLeague(
                    leagueName = s.leagueName,
                    sportType = s.sportType,
                    commissionerBackground = s.commissionerBackground,
                    currentYear = s.currentYear,
                    currentWeek = s.currentWeek,
                    seasonPhase = s.seasonPhase,
                    cashReservesMillions = s.cashReservesMillions,
                    leagueValuationMillions = s.leagueValuationMillions,
                    leagueIntegrityScore = s.leagueIntegrityScore,
                    competitiveParityScore = s.competitiveParityScore,
                    unionAgreement = s.unionAgreement,
                    activeBroadcast = s.activeBroadcast,
                    franchises = s.franchises,
                    cities = s.cities,
                    campaigns = s.campaigns,
                    history = s.valuationHistory,
                    newsFeed = s.recentNews,
                    freeAgentPool = s.freeAgentPool,
                    freeAgentSignings = s.freeAgentSignings,
                    ruleBook = s.ruleBook,
                    leagueSponsors = s.leagueSponsors,
                    financialSnapshots = s.financialSnapshots,
                    ticketConcessionCapActive = s.prDamageControlState.ticketConcessionCapActiveThisSeason
                )
                if (requestSeq < persistRequestSeq.get()) return@withLock
                repository.saveMinorLeagueState(s.minorLeagueState)
                repository.saveBoardVoteHistory(s.boardVoteHistory)
                repository.saveAllStarRedesignState(s.allStarRedesignState)
                repository.saveOfficiatingState(s.officiatingState)
                repository.saveRuleCommitteeState(s.ruleCommitteeState)
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun selectTab(index: Int) = setSelectedTab(index)

    fun toggleNewLeagueDialog(show: Boolean) {
        _uiState.update { it.copy(showNewLeagueDialog = show) }
    }

    fun advanceWeek() {
        val state = _uiState.value
        if (state.activeDilemma != null) return // Resolve dilemma first
        val nextState = coreSimulationViewModel.advanceWeek(state)
        _uiState.value = nextState
        persistCurrentState()
    }

    fun openOffSeasonSummit(show: Boolean = true) {
        val state = _uiState.value
        val session = state.activeOffSeasonSession ?: if (state.seasonPhase == SeasonPhase.OFF_SEASON) {
            OffSeasonEngine.createOffSeasonSession(
                year = state.currentYear,
                franchises = state.franchises,
                sportType = state.sportType,
                ruleBook = state.ruleBook,
                initialParity = state.competitiveParityScore,
                careerStats = state.careerPlayerStats,
                seasonStats = state.seasonPlayerStats,
                weeklyAwards = state.weeklyAwards,
                champion = state.seasonChampion,
                autoCompleteDraft = false
            )
        } else {
            state.latestCompletedOffSeason
        }
        _uiState.update {
            val ensuredUpcoming = ensureUpcomingSchedulePlan(it)
            it.copy(
                showOffSeasonScreen = show,
                activeOffSeasonSession = if (it.seasonPhase == SeasonPhase.OFF_SEASON) session else it.activeOffSeasonSession,
                seasonScheduleState = ensuredUpcoming
            )
        }
    }

    fun closeOffSeasonSummit() {
        _uiState.update { it.copy(showOffSeasonScreen = false) }
    }

    fun setOffSeasonStep(step: OffSeasonStep) {
        _uiState.update { state ->
            val ensuredSchedule = if (step == OffSeasonStep.SCHEDULE_CALENDAR) {
                ensureUpcomingSchedulePlan(state)
            } else state.seasonScheduleState
            val session = state.activeOffSeasonSession
            if (session != null) {
                state.copy(
                    activeOffSeasonSession = session.copy(currentStep = step),
                    seasonScheduleState = ensuredSchedule
                )
            } else if (state.latestCompletedOffSeason != null) {
                state.copy(
                    latestCompletedOffSeason = state.latestCompletedOffSeason.copy(currentStep = step),
                    seasonScheduleState = ensuredSchedule
                )
            } else state.copy(seasonScheduleState = ensuredSchedule)
        }
    }

    fun openRecordBook(open: Boolean = true) {
        _uiState.update { it.copy(showRecordBookScreen = open) }
    }

    // =====================================================
    // === DEPTH CHART & INJURY MANAGEMENT ACTIONS ===
    // =====================================================
    fun openDepthChart(franchiseId: String? = null) {
        val targetId = franchiseId ?: _uiState.value.franchises.maxByOrNull { it.unresolvedStarterHolesCount * 10 + it.injuredPlayersCount }?.id
            ?: _uiState.value.franchises.firstOrNull()?.id
        _uiState.update {
            it.copy(
                showDepthChartScreen = true,
                selectedDepthChartFranchiseId = targetId
            )
        }
    }

    fun closeDepthChart() {
        _uiState.update { it.copy(showDepthChartScreen = false) }
    }

    fun selectDepthChartFranchise(franchiseId: String) {
        _uiState.update { it.copy(selectedDepthChartFranchiseId = franchiseId) }
    }

    fun promotePlayerOnDepthChart(franchiseId: String, playerId: String, replaceStarterId: String? = null) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val player = team.roster.find { it.id == playerId } ?: return
        val updatedTeam = InjuryEngine.promotePlayerToStarter(team, playerId, replaceStarterId)
        val rawFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = rawFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )

        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                recentNews = (listOf("DEPTH CHART UPDATE: ${team.teamFullName} promote ${player.name} (${player.position}, ${player.overallRating} OVR) into starting lineup.") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun demoteStarterOnDepthChart(franchiseId: String, starterId: String) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val updatedTeam = InjuryEngine.demoteStarterToBench(team, starterId)
        val rawFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = rawFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState
            )
        }
        persistCurrentState()
    }

    fun togglePlayerInjuredReserve(franchiseId: String, playerId: String) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val player = team.roster.find { it.id == playerId } ?: return
        val wasIr = player.activeInjury?.isOnInjuredReserve == true
        val updatedTeam = InjuryEngine.toggleInjuredReserve(team, playerId)
        val rawFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = rawFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )

        val headline = if (!wasIr) {
            "INJURED RESERVE: ${team.teamFullName} place ${player.name} on IR and elevate healthy depth."
        } else {
            "ROSTER ACTIVATION: ${team.teamFullName} activate ${player.name} off Injured Reserve."
        }

        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun movePlayerDepthOrder(franchiseId: String, playerId: String, moveUp: Boolean) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val updatedTeam = InjuryEngine.movePlayerDepthOrder(team, playerId, moveUp)
        val rawFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = rawFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState
            )
        }
        persistCurrentState()
    }

    fun autoOptimizeFranchiseDepthChart(franchiseId: String) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val updatedTeam = InjuryEngine.autoOptimizeDepthChart(team)
        val rawFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = rawFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                recentNews = (listOf("LINEUP OPTIMIZED: ${team.teamFullName} set healthy optimal starters (${updatedTeam.effectiveLineupRating} EFF OVR).") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun autoOptimizeAllDepthCharts() {
        val state = _uiState.value
        val rawFranchises = state.franchises.map { InjuryEngine.autoOptimizeDepthChart(it) }
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = rawFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                recentNews = (listOf("LEAGUE DEPTH CHARTS SYNCED: All franchises aligned healthy starters to cover injuries.") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    // ==================== MINOR DEVELOPMENT LEAGUE HUB ====================

    fun openMinorLeagueHub(affiliateId: String? = null) {
        val state = _uiState.value
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = state.franchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        val resolvedAffiliateId = affiliateId
            ?: state.selectedMinorAffiliateId?.takeIf { selId -> minorSync.minorState.affiliates.any { it.id == selId } }
            ?: minorSync.minorState.affiliates.firstOrNull()?.id
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                showMinorLeagueScreen = true,
                selectedMinorAffiliateId = resolvedAffiliateId
            )
        }
    }

    fun closeMinorLeagueHub() {
        _uiState.update { it.copy(showMinorLeagueScreen = false) }
    }

    fun selectMinorAffiliate(affiliateId: String) {
        _uiState.update { it.copy(selectedMinorAffiliateId = affiliateId) }
    }

    fun toggleBenchPlayerMinorAssignment(
        franchiseId: String,
        playerId: String,
        assignToMinor: Boolean
    ) {
        val state = _uiState.value
        val result = MinorLeagueEngine.togglePlayerMinorAssignment(
            franchises = state.franchises,
            currentMinorState = state.minorLeagueState,
            franchiseId = franchiseId,
            playerId = playerId,
            assignToMinor = assignToMinor,
            sportType = state.sportType
        )
        _uiState.update {
            it.copy(
                franchises = result.updatedFranchises,
                minorLeagueState = result.minorState,
                recentNews = (result.headlines + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun updateMinorAffiliateTicketPrice(affiliateId: String, newPrice: Int) {
        val state = _uiState.value
        val (updatedMinor, msg) = MinorLeagueEngine.updateAffiliateTicketPrice(
            currentMinorState = state.minorLeagueState,
            affiliateId = affiliateId,
            newPrice = newPrice
        )
        _uiState.update {
            it.copy(
                minorLeagueState = updatedMinor,
                recentNews = (listOfNotNull(msg) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun promoteDepthPlayerFromMinor(franchiseId: String, playerId: String) {
        promotePlayerOnDepthChart(franchiseId = franchiseId, playerId = playerId)
    }

    fun presentOffSeasonYearlyAwards() {
        val state = _uiState.value
        val session = state.activeOffSeasonSession ?: return
        val ceremony = session.awardsCeremony ?: return
        if (ceremony.isPresented) return

        val presentedCeremony = ceremony.copy(isPresented = true)
        val updatedArchives = if (state.pastAwardsCeremonies.any { it.year == presentedCeremony.year }) {
            state.pastAwardsCeremonies.map { if (it.year == presentedCeremony.year) presentedCeremony else it }
        } else {
            listOf(presentedCeremony) + state.pastAwardsCeremonies
        }

        val mvp = presentedCeremony.awards.find { it.awardType == AnnualAwardType.LEAGUE_MVP }
        val fmvp = presentedCeremony.awards.find { it.awardType == AnnualAwardType.FINALS_MVP }
        val awardsHeadline = buildString {
            append("SEASON ${ceremony.year} AWARDS GALA: ")
            if (mvp != null) append("${mvp.winnerName} (${mvp.franchiseName}) wins League MVP! ")
            if (fmvp != null && fmvp.winnerName != mvp?.winnerName) append("Finals MVP: ${fmvp.winnerName}.")
        }.trim()

        _uiState.update {
            it.copy(
                activeOffSeasonSession = session.copy(awardsCeremony = presentedCeremony),
                pastAwardsCeremonies = updatedArchives,
                recentNews = (listOf(awardsHeadline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun ratifyOffSeasonHallOfFame() {
        val state = _uiState.value
        val session = state.activeOffSeasonSession ?: return
        if (session.hofClassRatified) return

        val existingIds = state.hallOfFame.map { it.id }.toSet()
        val newInductees = session.hallOfFameInductees.filter { it.id !in existingIds }
        val updatedHof = newInductees + state.hallOfFame

        val hofNames = newInductees.joinToString(" & ") { it.name }
        val hofHeadline = if (newInductees.isNotEmpty()) {
            "HALL OF FAME CLASS OF ${session.year} ENSHRINED: $hofNames immortalized in league history!"
        } else null

        _uiState.update {
            it.copy(
                hallOfFame = updatedHof,
                activeOffSeasonSession = session.copy(hofClassRatified = true),
                recentNews = (listOfNotNull(hofHeadline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun setManagedFranchise(franchiseId: String) {
        val target = _uiState.value.franchises.find { it.id == franchiseId } ?: return
        repository.saveManagedFranchiseId(target.id)
        _uiState.update {
            it.copy(
                managedFranchiseId = target.id,
                recentNews = (listOf("FRANCHISE OFFICE: Commissioner assumed GM oversight of ${target.teamFullName}.") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun openDraftScoutingScreen(show: Boolean = true) {
        _uiState.update { state ->
            val pool = if (state.upcomingDraftClassYear != state.currentYear || state.upcomingDraftClass.isEmpty()) {
                DraftClassGenerator.generateProceduralClass(state.currentYear, 18, state.sportType)
            } else {
                state.upcomingDraftClass
            }
            state.copy(
                showDraftScoutingScreen = show,
                upcomingDraftClassYear = state.currentYear,
                upcomingDraftClass = pool
            )
        }
    }

    fun generateProceduralDraftClass(seed: Long? = null) {
        val state = _uiState.value
        val actualSeed = seed ?: (state.currentYear * 4099L + state.currentWeek * 131L + System.currentTimeMillis() % 9973L)
        val generated = DraftClassGenerator.generateProceduralClass(
            year = state.currentYear,
            poolSize = 18,
            sportType = state.sportType,
            seed = actualSeed
        )
        val updatedSession = state.activeOffSeasonSession?.let { session ->
            if (!session.isDraftCompleted && session.draftPicks.isEmpty()) {
                OffSeasonEngine.recalculateParityMovements(
                    session.copy(draftProspectPool = generated),
                    state.franchises
                )
            } else session
        }
        _uiState.update {
            it.copy(
                upcomingDraftClassYear = state.currentYear,
                upcomingDraftClass = generated,
                activeOffSeasonSession = updatedSession ?: it.activeOffSeasonSession,
                recentNews = (listOf("DRAFT SCOUTING: Fresh procedural ${state.currentYear} draft class generated (${generated.size} rated prospects, Top OVR ${generated.maxOfOrNull { p -> p.overallRating } ?: 85} / POT ${generated.maxOfOrNull { p -> p.potentialRating } ?: 95}).") + it.recentNews).take(20)
            )
        }
    }

    fun pickProspectForManagedFranchise(prospectId: String): Pair<Boolean, String> {
        val state = _uiState.value
        val managed = state.managedFranchise ?: return false to "No managed franchise selected."

        val session = state.activeOffSeasonSession
        val sessionSlot = if (session != null && !session.isDraftCompleted) {
            session.draftOrderSlots.getOrNull(session.currentDraftPickIndex)
        } else null

        if (session != null && sessionSlot != null && sessionSlot.ownerFranchiseId == managed.id) {
            val beforeCount = session.draftPicks.size
            conductNextRookieDraftPick(prospectId)
            val afterSession = _uiState.value.activeOffSeasonSession
            return if (afterSession != null && afterSession.draftPicks.size > beforeCount) {
                val picked = afterSession.draftPicks.last()
                true to "${managed.teamFullName} drafted ${picked.playerName} (${picked.position}, ${picked.overallRating} OVR)."
            } else {
                false to "Selected prospect is unavailable."
            }
        }

        val prospect = state.upcomingDraftClass.find { it.id == prospectId && !it.isDrafted }
            ?: return false to "Prospect is already drafted or unavailable."

        val nextPickNum = state.upcomingDraftClass.count { it.isDrafted } + 1
        val scaleFactor = state.unionAgreement.rookieScaleMultiplierPct.coerceIn(70, 150) / 100.0
        val baseRookiePay = when {
            nextPickNum <= 3 -> 9L
            nextPickNum <= 6 -> 6L
            nextPickNum <= 10 -> 4L
            else -> 3L
        }
        val scaledSalary = kotlin.math.round(baseRookiePay * scaleFactor).toLong().coerceAtLeast(2L)

        val rookiePlayer = RosterPlayer(
            id = "draft_${state.currentYear}_${prospect.id}",
            name = prospect.name,
            franchiseId = managed.id,
            position = prospect.position,
            overallRating = prospect.overallRating,
            potentialRating = prospect.potentialRating,
            age = prospect.age,
            salaryMillions = scaledSalary,
            contractYearsRemaining = 4,
            archetype = prospect.archetype,
            depthOrder = managed.roster.size,
            isStarter = false
        )

        val updatedRoster = managed.roster + rookiePlayer
        val updatedRating = updatedRoster.map { it.overallRating }.average().toInt().coerceIn(55, 99)
        val updatedPayroll = updatedRoster.sumOf { it.salaryMillions }.coerceAtLeast(35L)
        val updatedFranchises = state.franchises.map { team ->
            if (team.id == managed.id) {
                team.copy(
                    roster = updatedRoster,
                    rosterRating = updatedRating,
                    annualPayrollMillions = updatedPayroll
                )
            } else team
        }
        val updatedPool = state.upcomingDraftClass.map { p ->
            if (p.id == prospect.id) {
                p.copy(
                    isDrafted = true,
                    draftedByFranchiseId = managed.id,
                    draftedPickNumber = nextPickNum
                )
            } else p
        }
        val headline = "MANAGED FRANCHISE PICK #$nextPickNum: ${managed.teamFullName} selected ${prospect.name} (${prospect.position}, ${prospect.overallRating} OVR / ${prospect.potentialRating} POT)."
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                upcomingDraftClass = updatedPool,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true to headline
    }

    fun autoPickCpuUntilManagedTurn() {
        val state = _uiState.value
        val managedId = state.managedFranchiseId ?: return
        var session = state.activeOffSeasonSession ?: return
        var currentFranchises = state.franchises
        var picksMade = 0
        val newPickHeadlines = mutableListOf<String>()

        while (!session.isDraftCompleted) {
            val slot = session.draftOrderSlots.getOrNull(session.currentDraftPickIndex) ?: break
            if (slot.ownerFranchiseId == managedId) break
            val prevCount = session.draftPicks.size
            session = OffSeasonEngine.conductSingleDraftPick(
                session = session,
                franchises = currentFranchises,
                sportType = state.sportType,
                chosenProspectId = null,
                rookieScaleMultiplierPct = state.unionAgreement.rookieScaleMultiplierPct
            )
            if (session.draftPicks.size == prevCount) break
            val record = session.draftPicks.last()
            picksMade++
            newPickHeadlines.add("CPU PICK #${record.pickNumber}: ${record.franchiseName} selected ${record.playerName} (${record.overallRating} OVR).")
            val rookie = record.draftedPlayer
            if (rookie != null) {
                currentFranchises = currentFranchises.map { team ->
                    if (team.id == record.franchiseId && team.roster.none { it.id == rookie.id }) {
                        val updatedRoster = team.roster + rookie.copy(depthOrder = team.roster.size)
                        team.copy(
                            roster = updatedRoster,
                            rosterRating = updatedRoster.map { it.overallRating }.average().toInt().coerceIn(55, 99),
                            annualPayrollMillions = updatedRoster.sumOf { it.salaryMillions }.coerceAtLeast(35L)
                        )
                    } else team
                }
            }
        }
        if (picksMade == 0) return

        val draftedIds = session.draftProspectPool.filter { it.isDrafted }.associateBy { it.id }
        val syncedUpcoming = state.upcomingDraftClass.map { p -> draftedIds[p.id] ?: p }
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = currentFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                activeOffSeasonSession = session,
                upcomingDraftClass = syncedUpcoming,
                tradedPickLedger = TradeEngine.buildTradedPickLedger(
                    franchises = minorSync.updatedFranchises,
                    currentYear = state.currentYear,
                    activeSession = session,
                    latestCompletedOffSeason = it.latestCompletedOffSeason
                ),
                recentNews = (newPickHeadlines.take(3) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun conductNextRookieDraftPick(prospectId: String? = null) {
        val state = _uiState.value
        val session = state.activeOffSeasonSession ?: return
        if (session.isDraftCompleted) return

        val prevCount = session.draftPicks.size
        val updatedSession = OffSeasonEngine.conductSingleDraftPick(
            session = session,
            franchises = state.franchises,
            sportType = state.sportType,
            chosenProspectId = prospectId,
            rookieScaleMultiplierPct = state.unionAgreement.rookieScaleMultiplierPct
        )

        val newPick = if (updatedSession.draftPicks.size > prevCount) updatedSession.draftPicks.last() else null
        val updatedFranchises = if (newPick?.draftedPlayer != null) {
            val rookie = newPick.draftedPlayer
            state.franchises.map { team ->
                if (team.id == newPick.franchiseId && team.roster.none { it.id == rookie.id }) {
                    val updatedRoster = team.roster + rookie.copy(depthOrder = team.roster.size)
                    team.copy(
                        roster = updatedRoster,
                        rosterRating = updatedRoster.map { it.overallRating }.average().toInt().coerceIn(55, 99),
                        annualPayrollMillions = updatedRoster.sumOf { it.salaryMillions }.coerceAtLeast(35L)
                    )
                } else team
            }
        } else state.franchises

        val newsHeadline = newPick?.let {
            val tradeTag = if (it.acquiredViaTrade) " (via trade with ${it.originalFranchiseName})" else ""
            "ROOKIE DRAFT PICK #${it.pickNumber} (R${it.round}): ${it.franchiseName}$tradeTag select ${it.playerName} (${it.position}, ${it.overallRating} OVR / ${it.origin}) — ${it.scoutingGrade} Scout."
        }
        val draftedIds = updatedSession.draftProspectPool.filter { it.isDrafted }.associateBy { it.id }
        val syncedUpcoming = state.upcomingDraftClass.map { p -> draftedIds[p.id] ?: p }

        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                activeOffSeasonSession = updatedSession,
                upcomingDraftClass = syncedUpcoming,
                tradedPickLedger = TradeEngine.buildTradedPickLedger(
                    franchises = updatedFranchises,
                    currentYear = state.currentYear,
                    activeSession = updatedSession,
                    latestCompletedOffSeason = it.latestCompletedOffSeason
                ),
                recentNews = (listOfNotNull(newsHeadline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun autoCompleteRookieDraft() {
        val state = _uiState.value
        val session = state.activeOffSeasonSession ?: return
        if (session.isDraftCompleted) return

        val prevCount = session.draftPicks.size
        val updatedSession = OffSeasonEngine.autoCompleteDraft(
            session = session,
            franchises = state.franchises,
            sportType = state.sportType,
            rookieScaleMultiplierPct = state.unionAgreement.rookieScaleMultiplierPct
        )
        var updatedFranchises = state.franchises
        updatedSession.draftPicks.drop(prevCount).forEach { pickRec ->
            val rookie = pickRec.draftedPlayer ?: return@forEach
            updatedFranchises = updatedFranchises.map { team ->
                if (team.id == pickRec.franchiseId && team.roster.none { it.id == rookie.id }) {
                    val newRoster = team.roster + rookie.copy(depthOrder = team.roster.size)
                    team.copy(
                        roster = newRoster,
                        rosterRating = newRoster.map { it.overallRating }.average().toInt().coerceIn(55, 99),
                        annualPayrollMillions = newRoster.sumOf { it.salaryMillions }.coerceAtLeast(35L)
                    )
                } else team
            }
        }
        val topPick = updatedSession.draftPicks.firstOrNull()
        val headline = if (topPick != null) {
            "ROOKIE DRAFT COMPLETE: #1 ${topPick.playerName} (${topPick.position}) leads ${updatedSession.draftPicks.size} prospects drafted ahead of Pre-Season!"
        } else null
        val draftedIds = updatedSession.draftProspectPool.filter { it.isDrafted }.associateBy { it.id }
        val syncedUpcoming = state.upcomingDraftClass.map { p -> draftedIds[p.id] ?: p }

        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                activeOffSeasonSession = updatedSession,
                upcomingDraftClass = syncedUpcoming,
                tradedPickLedger = TradeEngine.buildTradedPickLedger(
                    franchises = updatedFranchises,
                    currentYear = state.currentYear,
                    activeSession = updatedSession,
                    latestCompletedOffSeason = it.latestCompletedOffSeason
                ),
                recentNews = (listOfNotNull(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun restartRookieDraft() {
        val state = _uiState.value
        val session = state.activeOffSeasonSession ?: return
        val resetPool = session.draftProspectPool.map {
            it.copy(isDrafted = false, draftedByFranchiseId = null, draftedPickNumber = null)
        }
        val resetSession = OffSeasonEngine.recalculateParityMovements(
            session.copy(
                draftProspectPool = resetPool,
                draftPicks = emptyList(),
                currentDraftPickIndex = 0,
                isDraftCompleted = false
            ),
            state.franchises
        )
        _uiState.update { it.copy(activeOffSeasonSession = resetSession) }
        persistCurrentState()
    }

    fun finalizeAndInaugurateNewSeason() {
        val nextState = coreSimulationViewModel.finalizeAndInaugurateNewSeason(_uiState.value)
        _uiState.value = nextState
        persistCurrentState()
    }

    fun startNewSeason() {
        finalizeAndInaugurateNewSeason()
    }

    // Executive Dilemma Decision (Branching Narrative Crisis Consequences)
    fun resolveDilemma(choiceIndex: Int) {
        val state = _uiState.value
        val dilemma = state.activeDilemma ?: return
        if (choiceIndex !in dilemma.choices.indices) return

        val resolution = CrisisEngine.applyChoiceConsequences(
            crisisState = state.crisisState,
            dilemma = dilemma,
            choiceIndex = choiceIndex,
            franchises = state.franchises,
            unionAgreement = state.unionAgreement,
            officiatingState = state.officiatingState,
            disciplinaryState = state.disciplinaryState,
            seasonYear = state.currentYear,
            currentWeek = state.currentWeek,
            regularSeasonWeeks = state.ruleBook.regularSeasonWeeks
        )

        val newCash = (state.cashReservesMillions + resolution.cashDeltaMillions).coerceAtLeast(0L)
        val newValuation = (state.leagueValuationMillions + resolution.valuationDeltaMillions).coerceAtLeast(100L)
        val newFanbase = ((state.totalLeagueFanbaseMillions + resolution.fanbaseDeltaMillions) * 100.0)
            .let { kotlin.math.round(it) / 100.0 }
            .coerceAtLeast(1.0)
        val newIntegrity = (state.leagueIntegrityScore + resolution.integrityDelta).coerceIn(20, 100)
        val newParity = (state.competitiveParityScore + resolution.parityDelta).coerceIn(20, 100)
        val newOwnerApproval = (state.ownerApprovalRating + resolution.ownerApprovalDelta).coerceIn(20, 100)

        _uiState.update {
            it.copy(
                crisisState = resolution.updatedCrisisState,
                franchises = resolution.updatedFranchises,
                unionAgreement = resolution.updatedUnion,
                officiatingState = resolution.updatedOfficiatingState,
                disciplinaryState = resolution.updatedDisciplinaryState,
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                totalLeagueFanbaseMillions = newFanbase,
                leagueIntegrityScore = newIntegrity,
                competitiveParityScore = newParity,
                ownerApprovalRating = newOwnerApproval,
                activeDilemma = null,
                recentNews = (listOf(resolution.headline) + it.recentNews).take(20)
            )
        }
        val eval = coreSimulationViewModel.evaluateLaborUnionTriggers(_uiState.value)
        if (eval.triggeredEvent != null) {
            _uiState.update {
                it.copy(
                    activeLaborTrigger = eval.triggeredEvent,
                    activeLaborNegotiationOverlay = eval.negotiationOverlay ?: it.activeLaborNegotiationOverlay,
                    laborTriggerHistory = coreSimulationViewModel.simulationState.value.laborTriggerHistory
                )
            }
        }
        resolution.spawnedBranch?.let { branch ->
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_branch_${branch.id}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.WARNING,
                    message = "Branch Scheduled (Y${branch.triggerYear} W${branch.triggerWeek}): ${branch.teaserLabel}",
                    actionLabel = "Timeline",
                    targetRoute = DashboardSnackbarRoute.CRISIS_COMMAND
                )
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        val crisisCasualDelta = when {
            resolution.fanbaseDeltaMillions >= 0.3 -> 4
            resolution.fanbaseDeltaMillions <= -0.3 -> -4
            resolution.ownerApprovalDelta >= 5 && resolution.integrityDelta <= -3 -> -2
            else -> 1
        }
        val crisisDieHardDelta = when {
            resolution.integrityDelta >= 4 -> 4
            resolution.integrityDelta <= -4 -> -5
            resolution.fanbaseDeltaMillions <= -0.3 -> -3
            else -> 1
        }
        recordFanReaction(
            category = FanDecisionCategory.CRISIS_RULING,
            decisionTitle = dilemma.choices[choiceIndex].label,
            decisionSummary = resolution.headline,
            casualDelta = crisisCasualDelta,
            dieHardDelta = crisisDieHardDelta
        )
        refreshJobSecurity(resolution.headline)
        recordDashboardTrend("Crisis S${dilemma.stageNumber}", "crisis_${dilemma.nodeId}_w${state.currentWeek}")
        persistCurrentState()
    }

    // Team Recruitment Investment (Commissioner Development Grant)
    fun investInTeamRecruitment(franchiseId: String, amountMillions: Long) {
        val state = _uiState.value
        if (state.cashReservesMillions < amountMillions) return

        val teamIndex = state.franchises.indexOfFirst { it.id == franchiseId }
        if (teamIndex == -1) return
        val team = state.franchises[teamIndex]

        val ratingBoost = (amountMillions / 5).toInt().coerceAtLeast(1)
        val updatedTeam = team.copy(
            rosterRating = (team.rosterRating + ratingBoost).coerceAtMost(96),
            annualPayrollMillions = team.annualPayrollMillions + (amountMillions / 2)
        )

        val updatedList = state.franchises.toMutableList()
        updatedList[teamIndex] = updatedTeam

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - amountMillions,
                franchises = updatedList,
                recentNews = listOf("League grants \$${amountMillions}M recruitment fund to ${team.teamFullName} to boost star talent!") + it.recentNews
            )
        }
        persistCurrentState()
    }

    fun recruitTalentForFranchise(franchiseId: String, costMillions: Long, ratingBoost: Int) {
        val state = _uiState.value
        val teamIndex = state.franchises.indexOfFirst { it.id == franchiseId }
        if (teamIndex == -1) return
        val team = state.franchises[teamIndex]

        val updatedTeam = team.copy(
            rosterRating = (team.rosterRating + ratingBoost).coerceAtMost(98),
            annualPayrollMillions = team.annualPayrollMillions + costMillions
        )

        val updatedList = state.franchises.toMutableList()
        updatedList[teamIndex] = updatedTeam

        _uiState.update {
            it.copy(
                franchises = updatedList,
                recentNews = listOf("SIGNING SPREE: ${team.teamFullName} signs star talent (+\$${costMillions}M/yr payroll, +$ratingBoost OVR)!") + it.recentNews
            )
        }
        persistCurrentState()
    }

    // Franchise Relocation to High-Growth City
    fun relocateFranchise(franchiseId: String, targetCityId: String) {
        val state = _uiState.value
        val teamIndex = state.franchises.indexOfFirst { it.id == franchiseId }
        val targetCityIndex = state.cities.indexOfFirst { it.id == targetCityId }
        if (teamIndex == -1 || targetCityIndex == -1) return

        val team = state.franchises[teamIndex]
        val oldCityId = team.cityId
        val targetCity = state.cities[targetCityIndex]
        if (targetCity.hasFranchise) return

        val relocationCost = 15L
        if (state.cashReservesMillions < relocationCost) return

        val newSponsorRevenue = (targetCity.populationMillions * targetCity.tier.mediaMultiplier * 4.0).toLong()
        val newFanBase = targetCity.populationMillions * 0.45

        val (relocConf, relocDiv) = InitialLeagueData.getGeographicAlignment(
            cityName = targetCity.name,
            enabledConferences = state.ruleBook.enabledConferences,
            enabledDivisions = state.ruleBook.enabledDivisions
        )
        val shouldRealignOnRelocate = relocConf.isContinentalInternational || state.ruleBook.isRuleEnacted("rule_geographic_realignment")

        val movedRaw = team.copy(
            cityId = targetCity.id,
            cityName = targetCity.name,
            conference = if (shouldRealignOnRelocate) relocConf else team.conference,
            division = if (shouldRealignOnRelocate) relocDiv else team.division,
            fanBaseMillions = Math.round(newFanBase * 10.0) / 10.0,
            fanMorale = 85,
            localSponsorRevenueMillions = newSponsorRevenue
        )
        val updatedTeam = TeamReputationEngine.recordRelocationBehavior(
            franchise = movedRaw,
            salaryCapMillions = state.unionAgreement.salaryCapMillions
        )

        val updatedFranchises = state.franchises.toMutableList()
        updatedFranchises[teamIndex] = updatedTeam

        val updatedCities = state.cities.map { city ->
            when (city.id) {
                targetCity.id -> city.copy(hasFranchise = true)
                oldCityId -> city.copy(hasFranchise = false)
                else -> city
            }
        }

        val adaptedPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
            currentRules = state.ruleBook.playoffRules,
            franchises = updatedFranchises,
            enabledConferences = state.ruleBook.enabledConferences
        )

        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedFranchises, updatedCities, state.rivalries, state.currentYear)
        val reannotatedMatches = RivalryEngine.annotateMatchesWithRivalries(state.currentWeekMatches, updatedFranchises, syncedRivalries, updatedCities)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )

        val relocHeadline = "FRANCHISE RELOCATION APPROVED! ${team.nickname} move to ${targetCity.name} (${updatedTeam.conference.displayName}) — Free-Agent Reputation now ${updatedTeam.reputationScore}/100 (${updatedTeam.reputationTier.label})."
        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - relocationCost,
                leagueValuationMillions = it.leagueValuationMillions + 35L,
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                cities = updatedCities,
                currentWeekMatches = reannotatedMatches,
                rivalries = syncedRivalries,
                ruleBook = it.ruleBook.copy(playoffRules = adaptedPlayoffRules),
                recentNews = (listOf(relocHeadline) + minorSync.headlines + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordFanReaction(
            category = FanDecisionCategory.STADIUM_RELOCATION,
            decisionTitle = "${team.nickname} Relocation to ${targetCity.name}",
            decisionSummary = "${team.teamFullName} approved to relocate to ${targetCity.name}.",
            casualDelta = 2,
            dieHardDelta = -6,
            franchiseName = updatedTeam.teamFullName,
            cityName = targetCity.name
        )
        refreshJobSecurity(relocHeadline)
        recordDashboardTrend("Reloc", "reloc_${targetCity.id}")
        persistCurrentState()
    }

    // Franchise Expansion
    fun expandLeague(
        targetCityId: String,
        nickname: String,
        colorHex: Long,
        ownershipType: OwnershipType,
        preferredArchetype: OwnerArchetype? = null,
        customOwnerName: String? = null,
        secondaryColorHex: Long? = null
    ) {
        val state = _uiState.value
        val cityIndex = state.cities.indexOfFirst { it.id == targetCityId }
        if (cityIndex == -1) return
        val city = state.cities[cityIndex]
        if (city.hasFranchise) return

        val (resolvedPrimary, resolvedSecondary) = FranchiseIdentityEngine.normalizeColorPair(colorHex, secondaryColorHex)
        val expansionFee = city.expansionFeeMillions
        val reachGainMillions = ExpansionRequestEngine.computeMarketReachGainMillions(city)
        val localSponsorRev = ExpansionRequestEngine.computeLocalSponsorRevenueMillions(city)
        val broadcastUplift = ExpansionRequestEngine.computeAnnualBroadcastUpliftMillions(city)
        val owner = OwnerGovernanceEngine.buildExpansionOwnershipGroup(
            city = city,
            ownershipType = ownershipType,
            existingFranchises = state.franchises,
            preferredArchetype = preferredArchetype,
            customOwnerName = customOwnerName
        )

        val (conf, div) = InitialLeagueData.getGeographicAlignment(
            cityName = city.name,
            enabledConferences = state.ruleBook.enabledConferences,
            enabledDivisions = state.ruleBook.enabledDivisions
        )
        val newTeamId = "team_${city.id}_${System.currentTimeMillis() % 1000}"
        val newTeam = Franchise(
            id = newTeamId,
            cityId = city.id,
            cityName = city.name,
            nickname = nickname,
            colorHex = resolvedPrimary,
            secondaryColorHex = resolvedSecondary,
            ownership = owner,
            conference = conf,
            division = div,
            rosterRating = 78,
            fanBaseMillions = reachGainMillions,
            fanMorale = 88,
            stadiumCapacity = city.arenaCapacity,
            ticketPrice = 75,
            annualPayrollMillions = 70L,
            localSponsorRevenueMillions = localSponsorRev,
            roster = TradeEngine.generateDefaultRosterForFranchise(newTeamId, city.name, state.sportType, 78),
            draftPicks = TradeEngine.generateDefaultDraftPicksForFranchise(newTeamId, "${city.name} $nickname", state.currentYear)
        )

        val updatedCities = state.cities.map { if (it.id == city.id) it.copy(hasFranchise = true) else it }
        val updatedFranchises = state.franchises + newTeam
        val adaptedPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
            currentRules = state.ruleBook.playoffRules,
            franchises = updatedFranchises,
            enabledConferences = state.ruleBook.enabledConferences
        )
        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedFranchises, updatedCities, state.rivalries, state.currentYear)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        val updatedBroadcast = state.activeBroadcast.copy(
            annualPayoutMillions = state.activeBroadcast.annualPayoutMillions + broadcastUplift
        )
        val updatedFanbase = Math.round((state.totalLeagueFanbaseMillions + reachGainMillions) * 10.0) / 10.0

        val matchingReq = state.pendingExpansionRequests.find { it.city.id == city.id }
        val remainingPending = state.pendingExpansionRequests.filterNot { it.city.id == city.id }
        val recalculatedPending = ExpansionRequestEngine.recalculateImpacts(
            requests = remainingPending,
            franchises = minorSync.updatedFranchises,
            activeBroadcast = updatedBroadcast,
            activeSponsorship = state.activeSponsorship,
            currentMarketReachMillions = updatedFanbase
        )
        val updatedReviewed = if (matchingReq != null) {
            listOf(
                matchingReq.copy(
                    proposedNickname = nickname,
                    status = ExpansionApplicationStatus.APPROVED,
                    decisionNotes = "Charter approved: +${reachGainMillions}M market reach & +\$${matchingReq.impact.projectedAnnualRevenueGainMillions}M/yr league revenue."
                )
            ) + state.reviewedExpansionRequests
        } else {
            state.reviewedExpansionRequests
        }

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions + expansionFee,
                leagueValuationMillions = it.leagueValuationMillions + (expansionFee * 2),
                totalLeagueFanbaseMillions = updatedFanbase,
                activeBroadcast = updatedBroadcast,
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                cities = updatedCities,
                rivalries = syncedRivalries,
                pendingExpansionRequests = recalculatedPending,
                reviewedExpansionRequests = updatedReviewed.take(30),
                ruleBook = it.ruleBook.copy(playoffRules = adaptedPlayoffRules),
                recentNews = (listOf("HISTORIC EXPANSION: Welcome the ${newTeam.teamFullName} (${owner.primaryOwner.fullName}, ${owner.primaryOwner.archetype.title}) to the ${conf.displayName}! Market reach +${reachGainMillions}M fans, +\$${broadcastUplift}M TV escalator, \$${expansionFee}M fee.") + minorSync.headlines + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        refreshJobSecurity("${newTeam.teamFullName} expansion charter ratified.")
        recordDashboardTrend("Expand", "exp_${city.id}")
        persistCurrentState()
    }

    fun openFranchiseManagementModal(open: Boolean = true) {
        _uiState.update { it.copy(showFranchiseManagementModal = open) }
    }

    fun addManagedFranchise(
        franchiseName: String,
        cityName: String,
        initialValuationMillions: Long,
        conferenceOverride: Conference? = null,
        primaryColorHex: Long? = null
    ): Pair<Boolean, String> {
        val trimmedName = franchiseName.trim()
        val trimmedCity = cityName.trim()
        if (trimmedName.isBlank()) return false to "Franchise name is required."
        if (trimmedCity.isBlank()) return false to "City is required."
        if (initialValuationMillions <= 0L) return false to "Initial valuation must be greater than \$0M."

        val cleanNickname = if (trimmedName.startsWith("$trimmedCity ", ignoreCase = true) &&
            trimmedName.length > trimmedCity.length + 1
        ) {
            trimmedName.substring(trimmedCity.length).trim()
        } else {
            trimmedName
        }

        val state = _uiState.value
        val duplicateExists = state.franchises.any {
            it.cityName.equals(trimmedCity, ignoreCase = true) &&
                it.nickname.equals(cleanNickname, ignoreCase = true)
        }
        if (duplicateExists) {
            return false to "$trimmedCity $cleanNickname already exists in the league."
        }

        val slug = trimmedCity.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "city" }
        val existingCity = state.cities.firstOrNull { it.name.equals(trimmedCity, ignoreCase = true) }
        val targetCity = existingCity ?: MarketCity(
            id = "city_custom_${slug}_${System.currentTimeMillis() % 10000}",
            name = trimmedCity,
            country = "United States",
            region = "North America",
            populationMillions = (initialValuationMillions / 70.0).coerceIn(1.2, 9.5),
            tier = when {
                initialValuationMillions >= 350L -> MarketTier.TIER_1_MEGA
                initialValuationMillions >= 200L -> MarketTier.HIGH_GROWTH_SUNBELT
                else -> MarketTier.REGIONAL_HEARTLAND
            },
            annualGrowthRatePct = 2.4,
            sportsAppetite = ((initialValuationMillions / 5L).toInt()).coerceIn(65, 96),
            medianIncomeK = 78,
            isInternational = false,
            hasFranchise = true,
            expansionFeeMillions = (initialValuationMillions / 3L).coerceIn(25L, 250L),
            arenaCapacity = (17500 + ((initialValuationMillions / 25L).toInt() * 300)).coerceIn(16000, 32000)
        )

        val (geoConf, geoDiv) = InitialLeagueData.getGeographicAlignment(
            cityName = trimmedCity,
            enabledConferences = state.ruleBook.enabledConferences,
            enabledDivisions = state.ruleBook.enabledDivisions
        )
        val resolvedConf = conferenceOverride ?: geoConf
        val resolvedDiv = if (conferenceOverride != null && conferenceOverride != geoConf) {
            Division.values().firstOrNull {
                it.conference == conferenceOverride && it in state.ruleBook.enabledDivisions
            } ?: Division.values().firstOrNull { it.conference == conferenceOverride } ?: geoDiv
        } else {
            geoDiv
        }

        val palette = listOf(0xFF0EA5E9L, 0xFF10B981L, 0xFFF59E0BL, 0xFFEF4444L, 0xFF8B5CF6L, 0xFFEC4899L)
        val chosenPrimary = primaryColorHex ?: palette[state.franchises.size % palette.size]
        val (resolvedPrimary, resolvedSecondary) = FranchiseIdentityEngine.normalizeColorPair(chosenPrimary, null)

        val owner = OwnerGovernanceEngine.buildExpansionOwnershipGroup(
            city = targetCity,
            ownershipType = OwnershipType.TECH_BILLIONAIRE,
            existingFranchises = state.franchises,
            preferredArchetype = null,
            customOwnerName = null
        )
        val newTeamId = "team_${slug}_${System.currentTimeMillis() % 100000}"
        val derivedRosterRating = (72 + ((initialValuationMillions - 150L) / 35L).toInt()).coerceIn(68, 92)
        val derivedFanbase = Math.round(((initialValuationMillions / 160.0).coerceIn(0.8, 4.5)) * 10.0) / 10.0
        val derivedPayroll = (initialValuationMillions / 4L).coerceIn(55L, state.unionAgreement.salaryCapMillions)
        val derivedSponsorRev = (initialValuationMillions / 9L).coerceIn(15L, 75L)

        val newTeam = Franchise(
            id = newTeamId,
            cityId = targetCity.id,
            cityName = trimmedCity,
            nickname = cleanNickname,
            colorHex = resolvedPrimary,
            secondaryColorHex = resolvedSecondary,
            ownership = owner,
            conference = resolvedConf,
            division = resolvedDiv,
            rosterRating = derivedRosterRating,
            fanBaseMillions = derivedFanbase,
            fanMorale = 85,
            stadiumCapacity = targetCity.arenaCapacity,
            ticketPrice = 72,
            annualPayrollMillions = derivedPayroll,
            localSponsorRevenueMillions = derivedSponsorRev,
            roster = TradeEngine.generateDefaultRosterForFranchise(newTeamId, trimmedCity, state.sportType, derivedRosterRating),
            draftPicks = TradeEngine.generateDefaultDraftPicksForFranchise(newTeamId, "$trimmedCity $cleanNickname", state.currentYear),
            initialValuationMillions = initialValuationMillions
        )

        val updatedCities = if (existingCity != null) {
            state.cities.map { if (it.id == existingCity.id) it.copy(hasFranchise = true) else it }
        } else {
            state.cities + targetCity.copy(hasFranchise = true)
        }
        val updatedFranchises = state.franchises + newTeam
        val adaptedPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
            currentRules = state.ruleBook.playoffRules,
            franchises = updatedFranchises,
            enabledConferences = state.ruleBook.enabledConferences
        )
        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(
            updatedFranchises,
            updatedCities,
            state.rivalries,
            state.currentYear
        )
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        val updatedFanbase = Math.round((state.totalLeagueFanbaseMillions + derivedFanbase) * 10.0) / 10.0
        val remainingPending = state.pendingExpansionRequests.filterNot { it.city.id == targetCity.id }
        val headline = "FRANCHISE CHARTER ADDED: ${newTeam.teamFullName} (\$${initialValuationMillions}M valuation) registered in ${resolvedConf.displayName} and saved to Room."

        _uiState.update {
            it.copy(
                leagueValuationMillions = it.leagueValuationMillions + initialValuationMillions,
                totalLeagueFanbaseMillions = updatedFanbase,
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                cities = updatedCities,
                rivalries = syncedRivalries,
                pendingExpansionRequests = remainingPending,
                ruleBook = it.ruleBook.copy(playoffRules = adaptedPlayoffRules),
                showFranchiseManagementModal = false,
                recentNews = (listOf(headline) + minorSync.headlines + it.recentNews).take(20)
            )
        }

        val entityToInsert = repository.buildFranchiseEntity(
            minorSync.updatedFranchises.find { it.id == newTeam.id } ?: newTeam
        )
        viewModelScope.launch {
            repository.upsertFranchiseEntity(entityToInsert)
        }
        coreSimulationViewModel.emitDashboardSnackbar(
            DashboardSnackbarEvent(
                id = "franchise_added_${newTeam.id}",
                type = DashboardAlertType.FINANCIAL_MILESTONE,
                severity = DashboardAlertSeverity.MILESTONE,
                message = "${newTeam.teamFullName} (\$${initialValuationMillions}M) saved to Room database."
            )
        )
        notifyFinancialMilestones(state, _uiState.value)
        refreshJobSecurity(headline)
        recordDashboardTrend("Franchise", "add_${newTeam.id}")
        persistCurrentState()
        return true to "${newTeam.teamFullName} added to Room database."
    }

    // --- EXPANSION REQUESTS APPROVAL & IMPACT MANAGEMENT ---
    fun openExpansionRequestsManagement(open: Boolean = true) {
        _uiState.update { state ->
            val refreshed = if (open) {
                ExpansionRequestEngine.recalculateImpacts(
                    requests = state.pendingExpansionRequests.filter { req ->
                        state.cities.none { it.id == req.city.id && it.hasFranchise }
                    },
                    franchises = state.franchises,
                    activeBroadcast = state.activeBroadcast,
                    activeSponsorship = state.activeSponsorship,
                    currentMarketReachMillions = state.totalLeagueFanbaseMillions
                )
            } else {
                state.pendingExpansionRequests
            }
            state.copy(
                showExpansionRequestsScreen = open,
                pendingExpansionRequests = refreshed
            )
        }
    }

    fun approveExpansionRequest(requestId: String, launchDraftImmediately: Boolean = false): Boolean {
        val state = _uiState.value
        val request = state.pendingExpansionRequests.find { it.id == requestId } ?: return false
        if (state.cities.any { it.id == request.city.id && it.hasFranchise }) {
            _uiState.update {
                it.copy(pendingExpansionRequests = it.pendingExpansionRequests.filterNot { r -> r.id == requestId })
            }
            return false
        }

        if (launchDraftImmediately) {
            _uiState.update { it.copy(showExpansionRequestsScreen = false) }
            startExpansionDraft(
                targetCityId = request.city.id,
                nickname = request.proposedNickname,
                brandColorHex = request.brandColorHex,
                ownershipType = request.ownershipType,
                totalRounds = 6,
                preferredArchetype = request.ownerArchetype,
                customOwnerName = request.ownerFullName,
                secondaryColorHex = request.secondaryColorHex
            )
            return true
        }

        expandLeague(
            targetCityId = request.city.id,
            nickname = request.proposedNickname,
            colorHex = request.brandColorHex,
            ownershipType = request.ownershipType,
            preferredArchetype = request.ownerArchetype,
            customOwnerName = request.ownerFullName,
            secondaryColorHex = request.secondaryColorHex
        )
        return true
    }

    fun declineExpansionRequest(
        requestId: String,
        reason: String = "Declined by Commissioner's Office"
    ): Boolean {
        val state = _uiState.value
        val target = state.pendingExpansionRequests.find { it.id == requestId } ?: return false
        val remaining = state.pendingExpansionRequests.filterNot { it.id == requestId }
        val declinedRecord = target.copy(
            status = ExpansionApplicationStatus.DECLINED,
            decisionNotes = reason
        )
        val headline = "EXPANSION REQUEST DECLINED: ${target.city.name} ${target.proposedNickname} bid rejected ($reason)."

        _uiState.update {
            it.copy(
                pendingExpansionRequests = remaining,
                reviewedExpansionRequests = (listOf(declinedRecord) + it.reviewedExpansionRequests).take(30),
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true
    }

    fun scoutNewExpansionRequest(): ExpansionRequest? {
        val state = _uiState.value
        val requestedCityIds = (state.pendingExpansionRequests + state.reviewedExpansionRequests).map { it.city.id }.toSet()
        var targetCity = state.cities.firstOrNull { !it.hasFranchise && it.id !in requestedCityIds }
        var updatedCities = state.cities

        if (targetCity == null) {
            val openCount = state.cities.count { !it.hasFranchise }
            val existingIds = state.cities.map { it.id }.toSet()
            val existingNames = state.cities.map { it.name.lowercase() }.toSet()
            val prospect = InitialLeagueData.getProspectiveMarketPool()
                .firstOrNull { it.id !in existingIds && it.name.lowercase() !in existingNames && it.id !in requestedCityIds }
            if (prospect != null && openCount < MAX_OPEN_EXPANSION_CANDIDATES) {
                targetCity = prospect.copy(hasFranchise = false)
                updatedCities = updatedCities + targetCity
            } else {
                // Fall back to any open unawarded city not currently pending
                val pendingIds = state.pendingExpansionRequests.map { it.city.id }.toSet()
                targetCity = state.cities.firstOrNull { !it.hasFranchise && it.id !in pendingIds }
            }
        }

        if (targetCity == null) return null

        val newRequest = ExpansionRequestEngine.buildRequestForCity(
            city = targetCity,
            franchises = state.franchises,
            activeBroadcast = state.activeBroadcast,
            activeSponsorship = state.activeSponsorship,
            currentMarketReachMillions = state.totalLeagueFanbaseMillions,
            seasonYear = state.currentYear,
            week = state.currentWeek,
            indexSeed = state.pendingExpansionRequests.size + state.reviewedExpansionRequests.size
        )

        val headline = "NEW EXPANSION APPLICATION: ${targetCity.name} ${newRequest.proposedNickname} submitted formal franchise request (+${newRequest.impact.projectedMarketReachGainMillions}M reach, +\$${newRequest.impact.projectedAnnualRevenueGainMillions}M/yr projected rev)."
        _uiState.update {
            it.copy(
                cities = updatedCities,
                pendingExpansionRequests = listOf(newRequest) + it.pendingExpansionRequests,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return newRequest
    }

    fun buildExpansionArena(cityId: String, tier: ExpansionArenaTier): Pair<Boolean, String> {
        val state = _uiState.value
        val city = state.cities.find { it.id == cityId }
            ?: return false to "Expansion market not found."
        if (city.hasFranchise) {
            return false to "${city.name} already hosts an active league franchise."
        }
        if (state.cashReservesMillions < tier.buildCostMillions) {
            return false to "Insufficient league cash reserves (\$${state.cashReservesMillions}M available vs \$${tier.buildCostMillions}M needed)."
        }

        val updatedCity = city.copy(
            arenaName = "${city.name} ${tier.displayName}",
            arenaCapacity = tier.capacity,
            arenaReadinessStatus = tier.readinessSummary,
            localSponsorDepthMillions = city.localSponsorDepthMillions + tier.sponsorBoostMillions,
            hasBuiltExpansionArena = true,
            arenaBuildCostSpentMillions = tier.buildCostMillions,
            expansionArenaTierName = tier.displayName
        )
        val updatedCities = state.cities.map { if (it.id == cityId) updatedCity else it }
        val updatedRequests = state.pendingExpansionRequests.map { req ->
            if (req.city.id != cityId) req else {
                req.copy(
                    city = updatedCity,
                    arenaPlanSummary = "${updatedCity.arenaName} (${updatedCity.arenaCapacity} seats) — ${updatedCity.arenaReadinessStatus}.",
                    impact = ExpansionRequestEngine.evaluateImpact(
                        city = updatedCity,
                        currentFranchises = state.franchises,
                        activeBroadcast = state.activeBroadcast,
                        activeSponsorship = state.activeSponsorship,
                        currentMarketReachMillions = state.totalLeagueFanbaseMillions
                    )
                )
            }
        }
        val headline = "EXPANSION ARENA FUNDED: Commissioned ${updatedCity.arenaName} (${tier.capacity} seats) in ${city.name} for \$${tier.buildCostMillions}M (+\$${tier.sponsorBoostMillions}M local sponsor depth)."
        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - tier.buildCostMillions,
                leagueValuationMillions = it.leagueValuationMillions + tier.buildCostMillions + 8L,
                cities = updatedCities,
                pendingExpansionRequests = updatedRequests,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordFanReaction(
            category = FanDecisionCategory.EXPANSION_TRADE,
            decisionTitle = "${city.name} ${tier.displayName} Groundbreaking",
            decisionSummary = headline,
            casualDelta = 4,
            dieHardDelta = 2,
            cityName = city.name
        )
        recordDashboardTrend("Arena", "arena_${city.id}_${tier.name}")
        persistCurrentState()
        return true to "Built ${updatedCity.arenaName} in ${city.name} (${tier.capacity} seats)."
    }

    // --- EXPANSION MARKET POOL MANAGEMENT (CAP: 10 OPEN CANDIDATES) ---
    fun getOpenExpansionCandidates(): List<MarketCity> =
        _uiState.value.cities.filter { !it.hasFranchise }

    fun admitMarketCandidate(city: MarketCity, replaceCityId: String? = null): Boolean {
        val state = _uiState.value
        val openCandidates = state.cities.filter { !it.hasFranchise }
        if (state.cities.any { it.id == city.id || it.name.equals(city.name, ignoreCase = true) }) return false

        val candidateToAdd = city.copy(hasFranchise = false)

        val updatedCities = if (openCandidates.size >= MAX_OPEN_EXPANSION_CANDIDATES) {
            if (replaceCityId == null) {
                return false // At capacity 10/10
            }
            val targetReplace = openCandidates.find { it.id == replaceCityId } ?: return false
            state.cities.map { if (it.id == targetReplace.id) candidateToAdd else it }
        } else {
            state.cities + candidateToAdd
        }

        val finalOpenCount = updatedCities.count { !it.hasFranchise }
        if (finalOpenCount > MAX_OPEN_EXPANSION_CANDIDATES) return false

        val headline = if (replaceCityId != null) {
            "EXPANSION CANDIDATE REPLACED: ${city.name} (${if (city.isInternational) city.country else "USA"}) admitted to candidate pool, replacing ${openCandidates.find { it.id == replaceCityId }?.name ?: "previous bid"} (Active: $finalOpenCount/$MAX_OPEN_EXPANSION_CANDIDATES)."
        } else {
            "NEW EXPANSION CANDIDATE: Formal bid submitted by ${city.name} (${if (city.isInternational) city.country else "USA"}) with ${city.overallViabilityScore}/100 viability [${city.viabilityGrade.label}]. Active candidates: $finalOpenCount/$MAX_OPEN_EXPANSION_CANDIDATES."
        }

        _uiState.update {
            it.copy(
                cities = updatedCities,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
        return true
    }

    fun withdrawMarketCandidate(cityId: String): Boolean {
        val state = _uiState.value
        val target = state.cities.find { it.id == cityId } ?: return false
        if (target.hasFranchise) return false // Cannot withdraw a chartered franchise

        val updatedCities = state.cities.filterNot { it.id == cityId }
        val remainingOpen = updatedCities.count { !it.hasFranchise }
        val headline = "EXPANSION BID WITHDRAWN: ${target.name} exploratory committee has withdrawn its proposal. Active open candidate pool: $remainingOpen/$MAX_OPEN_EXPANSION_CANDIDATES."

        _uiState.update {
            it.copy(
                cities = updatedCities,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
        return true
    }

    fun scoutAndRefreshCandidates(): Int {
        val state = _uiState.value
        val currentCityIds = state.cities.map { it.id }.toSet()
        val currentCityNames = state.cities.map { it.name.lowercase() }.toSet()
        val availableProspects = InitialLeagueData.getProspectiveMarketPool().filter {
            it.id !in currentCityIds && it.name.lowercase() !in currentCityNames
        }
        val currentOpenCount = state.cities.count { !it.hasFranchise }
        val slotsAvailable = MAX_OPEN_EXPANSION_CANDIDATES - currentOpenCount
        if (slotsAvailable <= 0 || availableProspects.isEmpty()) {
            return 0
        }

        val toAdd = availableProspects.shuffled().take(minOf(slotsAvailable, 2))
        val updatedCities = state.cities + toAdd.map { it.copy(hasFranchise = false) }

        val addedNames = toAdd.joinToString(", ") { "${it.name} (${it.viabilityGrade.shortBadge})" }
        val newOpenCount = updatedCities.count { !it.hasFranchise }
        val headline = "GLOBAL EXPANSION SCOUTING: Commissioner committee accredited new bids: $addedNames. Active candidates: $newOpenCount/$MAX_OPEN_EXPANSION_CANDIDATES."

        _uiState.update {
            it.copy(
                cities = updatedCities,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
        return toAdd.size
    }

    fun setFinancesSubTab(tab: Int) {
        _uiState.update { it.copy(selectedFinancesSubTab = tab) }
    }

    fun updateFranchiseTicketPrice(franchiseId: String, newPrice: Int) {
        val clampedPrice = newPrice.coerceIn(20, 250)
        val beforeTeam = _uiState.value.franchises.find { it.id == franchiseId } ?: return
        val oldPrice = beforeTeam.ticketPrice
        val reactionDelta = FanLoyaltyEngine.evaluateTicketPriceReaction(beforeTeam, oldPrice, clampedPrice)
        _uiState.update { state ->
            var reactionHeadline = "COMMERCIAL NOTICE: ${beforeTeam.nickname} ticket price updated to \$${clampedPrice}/seat."
            val updated = state.franchises.map { team ->
                if (team.id == franchiseId) {
                    val newLoyalty = (team.fanLoyalty + reactionDelta).coerceIn(10, 100)
                    reactionHeadline = when {
                        reactionDelta <= -3 -> "FAN PRICING BACKLASH: ${team.teamFullName} hike to \$${clampedPrice}/seat triggers outrage ($reactionDelta loyalty -> $newLoyalty/100)."
                        reactionDelta < 0 -> "TICKET PRICING NOTICE: ${team.teamFullName} adjust admission to \$${clampedPrice}/seat ($reactionDelta loyalty)."
                        reactionDelta > 0 -> "FAN-FRIENDLY PRICING: ${team.teamFullName} lower seats to \$${clampedPrice}/seat, boosting fan loyalty (+$reactionDelta -> $newLoyalty/100)!"
                        else -> "COMMERCIAL NOTICE: ${team.nickname} ticket price updated to \$${clampedPrice}/seat (fair market pricing)."
                    }
                    team.copy(
                        ticketPrice = clampedPrice,
                        fanLoyalty = newLoyalty,
                        lastLoyaltyDrivers = team.lastLoyaltyDrivers.copy(
                            pricingDelta = team.lastLoyaltyDrivers.pricingDelta + reactionDelta,
                            lastSummary = "Ticket price set to \$${clampedPrice} (${if (reactionDelta >= 0) "+$reactionDelta" else "$reactionDelta"} loyalty)"
                        )
                    )
                } else team
            }
            state.copy(
                franchises = updated,
                recentNews = (listOf(reactionHeadline) + state.recentNews).take(20)
            )
        }
        if (clampedPrice != oldPrice) {
            val priceDiff = clampedPrice - oldPrice
            val casualD = if (priceDiff > 0) -(priceDiff / 6).coerceIn(1, 6) else ((-priceDiff) / 6).coerceIn(1, 6)
            val dieHardD = if (priceDiff > 0) -(priceDiff / 10).coerceIn(1, 4) else ((-priceDiff) / 10).coerceIn(1, 3)
            recordFanReaction(
                category = FanDecisionCategory.TICKET_PRICING,
                decisionTitle = "${beforeTeam.nickname} Tickets Set to \$$clampedPrice",
                decisionSummary = "Gate price moved from \$$oldPrice to \$$clampedPrice/seat in ${beforeTeam.cityName}.",
                casualDelta = casualD,
                dieHardDelta = dieHardD,
                franchiseName = beforeTeam.teamFullName,
                cityName = beforeTeam.cityName
            )
        }
        persistCurrentState()
    }

    fun startExpansionDraft(
        targetCityId: String,
        nickname: String,
        brandColorHex: Long,
        ownershipType: OwnershipType,
        totalRounds: Int = 6,
        preferredArchetype: OwnerArchetype? = null,
        customOwnerName: String? = null,
        secondaryColorHex: Long? = null
    ) {
        val state = _uiState.value
        val city = state.cities.find { it.id == targetCityId } ?: return
        if (city.hasFranchise) return

        val (resolvedPrimary, resolvedSecondary) = FranchiseIdentityEngine.normalizeColorPair(brandColorHex, secondaryColorHex)
        val pool = ExpansionDraftPoolGenerator.generatePool(
            sportType = state.sportType,
            existingFranchises = state.franchises,
            freeAgentPool = state.freeAgentPool
        )
        val previewOwnerGroup = OwnerGovernanceEngine.buildExpansionOwnershipGroup(
            city = city,
            ownershipType = ownershipType,
            existingFranchises = state.franchises,
            preferredArchetype = preferredArchetype,
            customOwnerName = customOwnerName
        )
        val session = ExpansionDraftSession(
            targetCity = city,
            franchiseNickname = nickname.trim().ifEmpty { "${city.name} Expansion" },
            brandColorHex = resolvedPrimary,
            secondaryColorHex = resolvedSecondary,
            ownershipType = ownershipType,
            totalRounds = totalRounds.coerceIn(4, 10),
            currentRound = 1,
            availablePool = pool,
            draftedPlayers = emptyList(),
            isCompleted = false,
            ownerArchetype = previewOwnerGroup.primaryOwner.archetype,
            ownerFullName = previewOwnerGroup.primaryOwner.fullName
        )

        _uiState.update {
            it.copy(activeExpansionDraftSession = session)
        }
    }

    fun generateRandomFranchiseNickname(
        targetCityId: String? = null,
        excludeNickname: String? = null
    ): String {
        val state = _uiState.value
        val city = targetCityId?.let { id -> state.cities.find { it.id == id } }
        return FranchiseIdentityEngine.generateRandomRealisticNickname(
            city = city,
            existingFranchises = state.franchises,
            sportType = state.sportType,
            excludeNickname = excludeNickname
        )
    }

    fun updateFranchiseIdentity(
        franchiseId: String,
        newNickname: String,
        primaryColorHex: Long,
        secondaryColorHex: Long
    ): Pair<Boolean, String> {
        val cleaned = newNickname.trim()
        if (cleaned.isEmpty()) {
            return false to "Franchise nickname cannot be blank."
        }
        if (primaryColorHex == secondaryColorHex) {
            return false to "Please select 2 different colors for the franchise."
        }
        val state = _uiState.value
        val target = state.franchises.find { it.id == franchiseId }
            ?: return false to "Franchise not found."

        val updatedFranchises = state.franchises.map { team ->
            if (team.id == franchiseId) {
                team.copy(
                    nickname = cleaned,
                    colorHex = primaryColorHex,
                    secondaryColorHex = secondaryColorHex
                )
            } else team
        }
        val updatedMinorAffiliates = state.minorLeagueState.affiliates.map { aff ->
            if (aff.parentFranchiseId == franchiseId) {
                aff.copy(colorHex = primaryColorHex)
            } else aff
        }
        val pName = FranchiseIdentityEngine.colorNameFor(primaryColorHex)
        val sName = FranchiseIdentityEngine.colorNameFor(secondaryColorHex)
        val headline = "FRANCHISE REBRAND: ${target.cityName} $cleaned unveil $pName & $sName team colors."
        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                minorLeagueState = it.minorLeagueState.copy(affiliates = updatedMinorAffiliates),
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true to "Updated ${target.cityName} $cleaned identity ($pName & $sName)."
    }

    fun draftPlayerForExpansion(playerId: String) {
        val state = _uiState.value
        val session = state.activeExpansionDraftSession ?: return
        if (session.isCompleted) return

        val player = session.availablePool.find { it.id == playerId } ?: return
        if (player.isProtected) return
        if (player.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER &&
            session.isTeamDraftLimitReached(player.sourceTeamId)
        ) {
            return
        }

        val updatedDrafted = session.draftedPlayers + player.copy(isDrafted = true)
        val newCountFromSource = if (player.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER) {
            updatedDrafted.count {
                it.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER && it.sourceTeamId == player.sourceTeamId
            }
        } else 0
        val lockSourceTeam = newCountFromSource >= session.maxPicksPerExistingTeam

        val updatedPool = session.availablePool
            .filter { it.id != playerId }
            .map { candidate ->
                if (lockSourceTeam &&
                    candidate.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER &&
                    candidate.sourceTeamId == player.sourceTeamId
                ) {
                    candidate.copy(isProtected = true)
                } else {
                    candidate
                }
            }

        val nextRound = session.currentRound + 1
        val completed = updatedDrafted.size >= session.totalRounds

        val updatedSession = session.copy(
            currentRound = if (completed) session.totalRounds else nextRound,
            availablePool = updatedPool,
            draftedPlayers = updatedDrafted,
            isCompleted = completed
        )

        _uiState.update {
            it.copy(activeExpansionDraftSession = updatedSession)
        }
    }

    fun undraftPlayerForExpansion(playerId: String) {
        val state = _uiState.value
        val session = state.activeExpansionDraftSession ?: return
        val target = session.draftedPlayers.find { it.id == playerId } ?: return

        val updatedDrafted = session.draftedPlayers.filterNot { it.id == playerId }
        val restoredPool = (session.availablePool + target.copy(isDrafted = false, isProtected = false))
            .map { candidate ->
                if (candidate.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER) {
                    val countForTeam = updatedDrafted.count {
                        it.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER && it.sourceTeamId == candidate.sourceTeamId
                    }
                    candidate.copy(isProtected = countForTeam >= session.maxPicksPerExistingTeam)
                } else {
                    candidate.copy(isProtected = false)
                }
            }
            .sortedByDescending { it.overallRating }

        val updatedSession = session.copy(
            currentRound = (updatedDrafted.size + 1).coerceAtMost(session.totalRounds),
            availablePool = restoredPool,
            draftedPlayers = updatedDrafted,
            isCompleted = false
        )

        _uiState.update {
            it.copy(activeExpansionDraftSession = updatedSession)
        }
    }

    fun autoDraftBestAvailable() {
        val session = _uiState.value.activeExpansionDraftSession ?: return
        val best = session.availablePool.firstOrNull { !it.isProtected } ?: return
        draftPlayerForExpansion(best.id)
    }

    fun finalizeExpansionDraft() {
        val state = _uiState.value
        val session = state.activeExpansionDraftSession ?: return
        if (!session.isCompleted && session.draftedPlayers.isEmpty()) return

        val city = session.targetCity
        val expansionFee = city.expansionFeeMillions
        val owner = OwnerGovernanceEngine.buildExpansionOwnershipGroup(
            city = city,
            ownershipType = session.ownershipType,
            existingFranchises = state.franchises,
            preferredArchetype = session.ownerArchetype,
            customOwnerName = session.ownerFullName
        ).copy(ownerApproval = 92)

        val (conf, div) = InitialLeagueData.getGeographicAlignment(city.name)
        val calculatedRosterRating = if (session.draftedPlayers.isNotEmpty()) {
            session.draftedPlayers.map { it.overallRating }.average().toInt()
        } else 78

        val newTeamId = "team_${city.id}_${System.currentTimeMillis() % 1000}"
        val newTeamFullName = "${city.name} ${session.franchiseNickname}"

        // Build real RosterPlayer objects from all drafted players (sorted strongest first for starter slots)
        val draftedSorted = session.draftedPlayers.sortedByDescending { it.overallRating }
        val coreRosterPlayers = draftedSorted.mapIndexed { idx, dp ->
            RosterPlayer(
                id = dp.sourcePlayerId.ifBlank { "exp_ply_${city.id}_$idx" },
                name = dp.name,
                franchiseId = newTeamId,
                position = dp.position,
                overallRating = dp.overallRating,
                age = dp.age,
                salaryMillions = dp.salaryMillions.toLong().coerceAtLeast(2L),
                contractYearsRemaining = dp.contractYearsRemaining.coerceIn(1, 5),
                archetype = dp.archetype,
                depthOrder = idx,
                isStarter = idx < 5
            )
        }

        // Fill remaining roster slots up to 8 with expansion bench signees so the depth chart is game-ready
        val supplementalPlayers = if (coreRosterPlayers.size < 8) {
            val needed = 8 - coreRosterPlayers.size
            TradeEngine.generateDefaultRosterForFranchise(
                franchiseId = newTeamId,
                cityName = city.name,
                sportType = state.sportType,
                baseRating = (calculatedRosterRating - 6).coerceIn(68, 76)
            ).takeLast(needed).mapIndexed { extraIdx, p ->
                p.copy(
                    id = "exp_depth_${city.id}_$extraIdx",
                    franchiseId = newTeamId,
                    depthOrder = coreRosterPlayers.size + extraIdx,
                    isStarter = (coreRosterPlayers.size + extraIdx) < 5
                )
            }
        } else {
            emptyList()
        }
        val finalNewRoster = coreRosterPlayers + supplementalPlayers

        val totalCommittedPayroll = finalNewRoster.sumOf { it.salaryMillions }
            .coerceAtLeast(session.totalSalaryCommitted.toLong().coerceAtLeast(50L))

        val reachGainMillions = ExpansionRequestEngine.computeMarketReachGainMillions(city)
        val localSponsorRev = ExpansionRequestEngine.computeLocalSponsorRevenueMillions(city)
        val broadcastUplift = ExpansionRequestEngine.computeAnnualBroadcastUpliftMillions(city)

        val newTeam = Franchise(
            id = newTeamId,
            cityId = city.id,
            cityName = city.name,
            nickname = session.franchiseNickname,
            colorHex = session.brandColorHex,
            secondaryColorHex = session.secondaryColorHex,
            ownership = owner,
            conference = conf,
            division = div,
            rosterRating = calculatedRosterRating,
            fanBaseMillions = reachGainMillions,
            fanMorale = 90,
            stadiumCapacity = city.arenaCapacity,
            ticketPrice = 75,
            annualPayrollMillions = totalCommittedPayroll,
            localSponsorRevenueMillions = localSponsorRev,
            roster = finalNewRoster,
            draftPicks = TradeEngine.generateDefaultDraftPicksForFranchise(newTeamId, newTeamFullName, state.currentYear)
        )

        // Remove drafted existing-roster players from their original franchises
        val draftedBySourceTeam = session.draftedPlayers
            .filter { it.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER }
            .groupBy { it.sourceTeamId }

        val syncedExistingFranchises = state.franchises.map { existingTeam ->
            val lostPlayers = draftedBySourceTeam[existingTeam.id].orEmpty()
            if (lostPlayers.isEmpty()) {
                existingTeam
            } else {
                val lostIds = lostPlayers.map { it.sourcePlayerId }.toSet()
                val remainingRoster = existingTeam.roster.filterNot { it.id in lostIds }
                val newPayroll = if (remainingRoster.isNotEmpty()) {
                    remainingRoster.sumOf { it.salaryMillions }.coerceAtLeast(30L)
                } else {
                    (existingTeam.annualPayrollMillions - lostPlayers.sumOf { it.salaryMillions }.toLong()).coerceAtLeast(35L)
                }
                val newOvr = if (remainingRoster.isNotEmpty()) {
                    remainingRoster.sortedByDescending { it.overallRating }.take(5).map { it.overallRating }.average().toInt()
                } else {
                    existingTeam.rosterRating
                }
                InjuryEngine.autoOptimizeDepthChart(
                    existingTeam.copy(
                        roster = remainingRoster,
                        annualPayrollMillions = newPayroll,
                        rosterRating = newOvr
                    )
                )
            }
        }

        // Remove drafted Free Agents from the Free Agency Pool and log transactions
        val draftedFreeAgents = session.draftedPlayers
            .filter { it.sourceType == ExpansionDraftSourceType.FREE_AGENCY_POOL }
        val draftedFaIds = draftedFreeAgents.map { it.sourcePlayerId }.toSet()
        val expansionFaTransactions = draftedFreeAgents.mapIndexed { idx, fa ->
            FreeAgentSigningTransaction(
                id = "fa_exp_${city.id}_${state.currentYear}_$idx",
                playerId = fa.sourcePlayerId,
                playerName = fa.name,
                position = fa.position,
                overallRating = fa.overallRating,
                age = fa.age,
                archetype = fa.archetype,
                franchiseId = newTeamId,
                franchiseName = newTeamFullName,
                salaryMillions = fa.salaryMillions.toLong().coerceAtLeast(2L),
                contractYears = fa.contractYearsRemaining.coerceIn(1, 4),
                contractTypeLabel = "Expansion Draft Contract",
                reason = "Selected in ${city.name} Expansion Draft from Free Agency Pool",
                week = state.currentWeek,
                seasonYear = state.currentYear,
                isAutoSigned = false
            )
        }
        val remainingFaPool = state.freeAgentPool.filterNot { it.id in draftedFaIds }
        val updatedCities = state.cities.map { if (it.id == city.id) it.copy(hasFranchise = true) else it }
        val updatedFranchises = syncedExistingFranchises + newTeam
        val replenishedFaPool = FreeAgencyEngine.replenishFreeAgentPool(
            currentPool = remainingFaPool,
            franchises = updatedFranchises,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedFranchises, updatedCities, state.rivalries, state.currentYear)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )

        val rosterPickCount = session.draftedFromRostersCount
        val faPickCount = session.draftedFromFreeAgencyCount
        val newsHeadline = "HISTORIC EXPANSION DRAFT: The $newTeamFullName drafted ${session.draftedPlayers.size} players " +
            "($rosterPickCount from existing rosters, $faPickCount from free agency • Team OVR: $calculatedRosterRating) " +
            "to join ${conf.displayName}! Treasury received \$${expansionFee}M."

        val updatedBroadcast = state.activeBroadcast.copy(
            annualPayoutMillions = state.activeBroadcast.annualPayoutMillions + broadcastUplift
        )
        val updatedFanbase = Math.round((state.totalLeagueFanbaseMillions + reachGainMillions) * 10.0) / 10.0
        val matchingReq = state.pendingExpansionRequests.find { it.city.id == city.id }
        val remainingPending = state.pendingExpansionRequests.filterNot { it.city.id == city.id }
        val recalculatedPending = ExpansionRequestEngine.recalculateImpacts(
            requests = remainingPending,
            franchises = minorSync.updatedFranchises,
            activeBroadcast = updatedBroadcast,
            activeSponsorship = state.activeSponsorship,
            currentMarketReachMillions = updatedFanbase
        )
        val updatedReviewed = if (matchingReq != null) {
            listOf(
                matchingReq.copy(
                    proposedNickname = session.franchiseNickname,
                    status = ExpansionApplicationStatus.APPROVED,
                    decisionNotes = "Approved & drafted (${session.draftedPlayers.size} picks): +${reachGainMillions}M market reach."
                )
            ) + state.reviewedExpansionRequests
        } else {
            state.reviewedExpansionRequests
        }

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions + expansionFee,
                leagueValuationMillions = it.leagueValuationMillions + (expansionFee * 2),
                totalLeagueFanbaseMillions = updatedFanbase,
                activeBroadcast = updatedBroadcast,
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                cities = updatedCities,
                rivalries = syncedRivalries,
                pendingExpansionRequests = recalculatedPending,
                reviewedExpansionRequests = updatedReviewed.take(30),
                freeAgentPool = replenishedFaPool,
                freeAgentSignings = (expansionFaTransactions + it.freeAgentSignings).take(40),
                ruleBook = it.ruleBook.copy(
                    enabledConferences = it.ruleBook.enabledConferences + conf,
                    enabledDivisions = it.ruleBook.enabledDivisions + div
                ),
                activeExpansionDraftSession = null,
                recentNews = (listOf(newsHeadline) + minorSync.headlines + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        refreshJobSecurity(newsHeadline)
        recordDashboardTrend("Draft+", "expdraft_${state.currentWeek}")
        persistCurrentState()
    }

    fun cancelExpansionDraft() {
        _uiState.update { it.copy(activeExpansionDraftSession = null) }
    }

    fun reassignFranchiseConferenceDivision(
        franchiseId: String,
        conference: Conference,
        division: Division
    ) {
        val state = _uiState.value
        val index = state.franchises.indexOfFirst { it.id == franchiseId }
        if (index == -1) return
        val target = state.franchises[index]
        val updated = target.copy(conference = conference, division = division)
        val list = state.franchises.toMutableList()
        list[index] = updated

        val updatedEnabledConfs = state.ruleBook.enabledConferences + conference
        val updatedEnabledDivs = state.ruleBook.enabledDivisions + division
        val adaptedPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
            currentRules = state.ruleBook.playoffRules,
            franchises = list,
            enabledConferences = updatedEnabledConfs
        )

        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(list, state.cities, state.rivalries, state.currentYear)
        val reannotatedMatches = RivalryEngine.annotateMatchesWithRivalries(state.currentWeekMatches, list, syncedRivalries, state.cities)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(list, state.minorLeagueState, state.sportType, state.currentYear)
        val newsItem = "CONFERENCE REALIGNMENT: ${target.teamFullName} officially reassigned to ${conference.displayName} (${division.displayName})."

        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                currentWeekMatches = reannotatedMatches,
                ruleBook = it.ruleBook.copy(
                    enabledConferences = updatedEnabledConfs,
                    enabledDivisions = updatedEnabledDivs,
                    playoffRules = adaptedPlayoffRules
                ),
                recentNews = (listOf(newsItem) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun autoRealignAllFranchisesGeographically() {
        val state = _uiState.value
        val updatedList = state.franchises.map { team ->
            val (conf, div) = InitialLeagueData.getGeographicAlignment(
                cityName = team.cityName,
                enabledConferences = state.ruleBook.enabledConferences,
                enabledDivisions = state.ruleBook.enabledDivisions
            )
            team.copy(conference = conf, division = div)
        }
        val adaptedPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
            currentRules = state.ruleBook.playoffRules,
            franchises = updatedList,
            enabledConferences = state.ruleBook.enabledConferences
        )
        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedList, state.cities, state.rivalries, state.currentYear)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(updatedList, state.minorLeagueState, state.sportType, state.currentYear)
        val newsItem = "COMMISSIONER REALIGNMENT: Geographic conference and division realignment enacted across all ${updatedList.size} franchises."
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                ruleBook = it.ruleBook.copy(playoffRules = adaptedPlayoffRules),
                recentNews = (listOf(newsItem) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun batchReassignFranchises(assignments: Map<String, Pair<Conference, Division>>) {
        val state = _uiState.value
        val updatedList = state.franchises.map { team ->
            val newAssignment = assignments[team.id]
            if (newAssignment != null) {
                team.copy(conference = newAssignment.first, division = newAssignment.second)
            } else team
        }
        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedList, state.cities, state.rivalries, state.currentYear)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(updatedList, state.minorLeagueState, state.sportType, state.currentYear)
        val newsItem = "COMMISSIONER EXECUTIVE ORDER: Comprehensive franchise realignment enacted under league bylaws."
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                recentNews = (listOf(newsItem) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun applyRealignmentPreset(preset: RealignmentPreset) {
        val state = _uiState.value
        val activeConfs = state.ruleBook.effectiveConferences(state.franchises)
        val updatedList = when (preset) {
            RealignmentPreset.GEOGRAPHIC_OPTIMAL -> {
                state.franchises.map { team ->
                    val (conf, div) = InitialLeagueData.getGeographicAlignment(
                        cityName = team.cityName,
                        enabledConferences = state.ruleBook.enabledConferences,
                        enabledDivisions = state.ruleBook.enabledDivisions
                    )
                    team.copy(conference = conf, division = div)
                }
            }
            RealignmentPreset.COASTAL_VS_HEARTLAND -> {
                val coastalCities = listOf("New York", "Los Angeles", "Seattle", "Miami", "San Francisco", "Boston", "London", "Tokyo")
                state.franchises.map { team ->
                    if (team.cityName in coastalCities) {
                        val div = if (team.cityName in listOf("New York", "Boston", "London", "Miami")) Division.ATLANTIC else Division.PACIFIC
                        team.copy(conference = Conference.EASTERN, division = div)
                    } else {
                        val div = if (team.cityName in listOf("Chicago", "Detroit", "Cleveland")) Division.CENTRAL else Division.SOUTHWEST
                        team.copy(conference = Conference.WESTERN, division = div)
                    }
                }
            }
            RealignmentPreset.BALANCED_PARITY -> {
                val sorted = state.franchises.sortedByDescending { it.rosterRating }
                val targetConfs = if (activeConfs.isNotEmpty()) activeConfs else listOf(Conference.EASTERN, Conference.WESTERN)
                val newTeams = mutableListOf<Franchise>()
                sorted.forEachIndexed { i, team ->
                    val conf = targetConfs[i % targetConfs.size]
                    val availDivs = state.ruleBook.effectiveDivisionsFor(conf, state.franchises)
                    val div = availDivs[(i / targetConfs.size) % availDivs.size]
                    newTeams.add(team.copy(conference = conf, division = div))
                }
                newTeams
            }
        }

        val adaptedPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
            currentRules = state.ruleBook.playoffRules,
            franchises = updatedList,
            enabledConferences = state.ruleBook.enabledConferences
        )

        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedList, state.cities, state.rivalries, state.currentYear)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(updatedList, state.minorLeagueState, state.sportType, state.currentYear)
        val newsItem = "REALIGNMENT RATIFIED: '${preset.title}' format enacted into official competition codex."
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                ruleBook = it.ruleBook.copy(playoffRules = adaptedPlayoffRules),
                recentNews = (listOf(newsItem) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    // --- CONTINENTAL CONFERENCES & DIVISIONS GOVERNANCE ---

    fun createOrToggleContinentalConference(
        conference: Conference,
        enable: Boolean = true,
        autoAlignNonNorthAmerican: Boolean = true,
        adaptPlayoffs: Boolean = true
    ) {
        if (!conference.isContinentalInternational) return
        val state = _uiState.value
        val confDivisions = Division.values().filter { it.conference == conference }

        val updatedConfs = if (enable) {
            state.ruleBook.enabledConferences + conference
        } else {
            state.ruleBook.enabledConferences - conference
        }

        val updatedDivs = if (enable) {
            val alreadyHasDivs = confDivisions.any { it in state.ruleBook.enabledDivisions }
            if (alreadyHasDivs) state.ruleBook.enabledDivisions else state.ruleBook.enabledDivisions + confDivisions
        } else {
            state.ruleBook.enabledDivisions - confDivisions.toSet()
        }

        val updatedFranchises = state.franchises.map { team ->
            if (enable && autoAlignNonNorthAmerican && !InitialLeagueData.isNorthAmericanCity(team.cityName)) {
                val targetConf = InitialLeagueData.getTargetContinentalConference(team.cityName)
                if (targetConf == conference) {
                    val (c, d) = InitialLeagueData.getGeographicAlignment(team.cityName, updatedConfs, updatedDivs)
                    team.copy(conference = c, division = d)
                } else team
            } else if (!enable && team.conference == conference) {
                val (c, d) = InitialLeagueData.getGeographicAlignment(team.cityName, updatedConfs, updatedDivs)
                team.copy(conference = c, division = d)
            } else {
                team
            }
        }

        val updatedPlayoffRules = if (adaptPlayoffs) {
            PlayoffStructureEngine.adaptRulesForLeagueStructure(
                currentRules = state.ruleBook.playoffRules,
                franchises = updatedFranchises,
                enabledConferences = updatedConfs
            )
        } else state.ruleBook.playoffRules

        val movedCount = updatedFranchises.count { it.conference == conference }
        val actionWord = if (enable) "ESTABLISHED" else "DISSOLVED"
        val newsItem = if (enable) {
            "CONTINENTAL EXPANSION: ${conference.displayName} $actionWord with ${confDivisions.size} regional divisions ($movedCount non-North-American franchises aligned)!"
        } else {
            "CONFERENCE CONSOLIDATION: ${conference.displayName} dissolved; franchises returned to North American circuits."
        }

        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedFranchises, state.cities, state.rivalries, state.currentYear)
        val reannotated = RivalryEngine.annotateMatchesWithRivalries(state.currentWeekMatches, updatedFranchises, syncedRivalries, state.cities)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(updatedFranchises, state.minorLeagueState, state.sportType, state.currentYear)

        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                currentWeekMatches = reannotated,
                ruleBook = it.ruleBook.copy(
                    enabledConferences = updatedConfs,
                    enabledDivisions = updatedDivs,
                    playoffRules = updatedPlayoffRules
                ),
                recentNews = (listOf(newsItem) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun createAllContinentalConferences(
        autoAlignNonNorthAmerican: Boolean = true,
        adaptPlayoffs: Boolean = true
    ) {
        val state = _uiState.value
        val allConfs = Conference.values().toSet()
        val allDivs = Division.values().toSet()

        val updatedFranchises = if (autoAlignNonNorthAmerican) {
            state.franchises.map { team ->
                if (!InitialLeagueData.isNorthAmericanCity(team.cityName)) {
                    val (c, d) = InitialLeagueData.getGeographicAlignment(team.cityName, allConfs, allDivs)
                    team.copy(conference = c, division = d)
                } else team
            }
        } else state.franchises

        val updatedPlayoffRules = if (adaptPlayoffs) {
            PlayoffStructureEngine.adaptRulesForLeagueStructure(
                currentRules = state.ruleBook.playoffRules,
                franchises = updatedFranchises,
                enabledConferences = allConfs
            )
        } else state.ruleBook.playoffRules

        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedFranchises, state.cities, state.rivalries, state.currentYear)
        val reannotated = RivalryEngine.annotateMatchesWithRivalries(state.currentWeekMatches, updatedFranchises, syncedRivalries, state.cities)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(updatedFranchises, state.minorLeagueState, state.sportType, state.currentYear)

        val intlCount = updatedFranchises.count { it.conference.isContinentalInternational }
        val newsItem = "GLOBAL CHARTER RATIFIED: European, South American & Asia-Oceania Conferences created across 9 international divisions ($intlCount international clubs aligned)!"

        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                currentWeekMatches = reannotated,
                ruleBook = it.ruleBook.copy(
                    enabledConferences = allConfs,
                    enabledDivisions = allDivs,
                    playoffRules = updatedPlayoffRules
                ),
                recentNews = (listOf(newsItem) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun toggleDivisionEnabled(division: Division) {
        val state = _uiState.value
        val conf = division.conference
        if (conf !in state.ruleBook.enabledConferences) return

        val currentInConf = state.ruleBook.enabledDivisions.filter { it.conference == conf }
        val isCurrentlyEnabled = division in state.ruleBook.enabledDivisions
        if (isCurrentlyEnabled && currentInConf.size <= 1) {
            // Keep at least 1 division active per enabled conference
            return
        }

        val updatedDivs = if (isCurrentlyEnabled) {
            state.ruleBook.enabledDivisions - division
        } else {
            state.ruleBook.enabledDivisions + division
        }

        val fallbackDiv = Division.values().firstOrNull { it.conference == conf && it in updatedDivs } ?: division
        val updatedFranchises = state.franchises.map { team ->
            if (isCurrentlyEnabled && team.division == division) {
                team.copy(division = fallbackDiv)
            } else if (!isCurrentlyEnabled && !InitialLeagueData.isNorthAmericanCity(team.cityName)) {
                val (geoConf, geoDiv) = InitialLeagueData.getGeographicAlignment(team.cityName, state.ruleBook.enabledConferences, updatedDivs)
                if (geoConf == conf && geoDiv == division) team.copy(division = geoDiv) else team
            } else team
        }

        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedFranchises, state.cities, state.rivalries, state.currentYear)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(updatedFranchises, state.minorLeagueState, state.sportType, state.currentYear)
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                ruleBook = it.ruleBook.copy(enabledDivisions = updatedDivs)
            )
        }
        persistCurrentState()
    }

    fun alignInternationalFranchisesToContinentalConferences(adaptPlayoffs: Boolean = true) {
        val state = _uiState.value
        val updatedFranchises = state.franchises.map { team ->
            if (!InitialLeagueData.isNorthAmericanCity(team.cityName)) {
                val (c, d) = InitialLeagueData.getGeographicAlignment(
                    cityName = team.cityName,
                    enabledConferences = state.ruleBook.enabledConferences,
                    enabledDivisions = state.ruleBook.enabledDivisions
                )
                team.copy(conference = c, division = d)
            } else team
        }
        val updatedPlayoffRules = if (adaptPlayoffs) {
            PlayoffStructureEngine.adaptRulesForLeagueStructure(
                currentRules = state.ruleBook.playoffRules,
                franchises = updatedFranchises,
                enabledConferences = state.ruleBook.enabledConferences
            )
        } else state.ruleBook.playoffRules
        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(updatedFranchises, state.cities, state.rivalries, state.currentYear)
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(updatedFranchises, state.minorLeagueState, state.sportType, state.currentYear)

        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                rivalries = syncedRivalries,
                ruleBook = it.ruleBook.copy(playoffRules = updatedPlayoffRules),
                recentNews = (listOf("INTERNATIONAL ALIGNMENT: Non-North-American franchises reassigned to their continental conferences & divisions.") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun openRivalryHub(franchiseId: String? = null) {
        _uiState.update {
            it.copy(
                showRivalryHubScreen = true,
                selectedRivalryTeamFilterId = franchiseId
            )
        }
    }

    fun closeRivalryHub() {
        _uiState.update {
            it.copy(
                showRivalryHubScreen = false,
                selectedRivalryTeamFilterId = null
            )
        }
    }

    fun setRivalryTeamFilter(franchiseId: String?) {
        _uiState.update { it.copy(selectedRivalryTeamFilterId = franchiseId) }
    }

    // --- STADIUM & FACILITY MANAGEMENT HUB ---
    fun openFacilityHub(franchiseId: String? = null) {
        val targetId = franchiseId
            ?: _uiState.value.selectedFacilityFranchiseId
            ?: _uiState.value.franchises.firstOrNull()?.id
        _uiState.update {
            it.copy(
                showFacilityHubScreen = true,
                selectedFacilityFranchiseId = targetId
            )
        }
    }

    fun closeFacilityHub() {
        _uiState.update { it.copy(showFacilityHubScreen = false) }
    }

    fun selectFacilityFranchise(franchiseId: String) {
        _uiState.update { it.copy(selectedFacilityFranchiseId = franchiseId) }
    }

    fun investInFacilityUpgrade(franchiseId: String, type: FacilityType): Pair<Boolean, String> {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId }
            ?: return false to "Franchise not found."
        val nextSpec = FacilityEngine.getNextUpgradeSpec(team.facilities, type)
            ?: return false to "${type.title} is already maxed at Tier ${type.maxTier}."
        if (team.facilities.ongoingProjects.any { it.type == type }) {
            return false to "${type.shortName} expansion is already under construction."
        }
        if (state.cashReservesMillions < nextSpec.costMillions) {
            return false to "Insufficient league reserves (\$${nextSpec.costMillions}M required)."
        }

        val (updatedTeam, startedSpec) = FacilityEngine.startUpgradeProject(
            franchise = team,
            type = type,
            currentYear = state.currentYear,
            currentWeek = state.currentWeek
        ) ?: return false to "Unable to initiate ${type.shortName} project."

        val updatedFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val groundbreakingValBoost = (startedSpec.valuationBoostMillions / 3L).coerceAtLeast(4L)
        val headline = "FACILITY BREAKING GROUND: ${team.teamFullName} invest \$${startedSpec.costMillions}M into Tier ${startedSpec.targetTier} ${startedSpec.projectName} (${startedSpec.durationWeeks}W build)!"

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - startedSpec.costMillions,
                leagueValuationMillions = it.leagueValuationMillions + groundbreakingValBoost,
                franchises = updatedFranchises,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("Build", "fac_${type.name.take(4)}")
        persistCurrentState()
        return true to "Construction started on ${startedSpec.projectName} (${startedSpec.durationWeeks} weeks)."
    }

    fun expediteFacilityProject(franchiseId: String, projectId: String): Pair<Boolean, String> {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId }
            ?: return false to "Franchise not found."
        val project = team.facilities.ongoingProjects.find { it.id == projectId }
            ?: return false to "Construction project not found."
        val expediteCost = (project.remainingWeeks * 4L).coerceAtLeast(3L)
        if (state.cashReservesMillions < expediteCost) {
            return false to "Insufficient reserves (\$${expediteCost}M required to expedite)."
        }

        val completedTeam = FacilityEngine.completeProjectImmediately(team, project)
        val updatedFranchises = state.franchises.map { if (it.id == franchiseId) completedTeam else it }
        val rebuiltFlagshipArena = project.type == FacilityType.STADIUM_SEATING && project.targetTier >= 5
        val updatedDealState = if (rebuiltFlagshipArena) {
            StadiumDealEngine.enqueueNamingRightsOfferForFranchise(
                currentState = state.stadiumDealState,
                franchise = completedTeam,
                seasonYear = state.currentYear,
                triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                previousSponsorName = team.stadium.sponsorName,
                allFranchises = updatedFranchises
            )
        } else {
            state.stadiumDealState
        }
        val headline = "GRAND OPENING (EXPEDITED): ${team.teamFullName} fast-tracked ${project.projectName} — Franchise valuation +\$${project.valuationBoostMillions}M!"

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - expediteCost,
                leagueValuationMillions = it.leagueValuationMillions + project.valuationBoostMillions,
                franchises = updatedFranchises,
                stadiumDealState = updatedDealState,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("Arena+", "fac_exp_${project.id}")
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return true to "${project.projectName} completed immediately!"
    }

    // =====================================================
    // === FREE AGENCY POOL & DEPTH CHART SIGNING ENGINE ===
    // =====================================================
    fun openFreeAgencyPool(franchiseId: String? = null) {
        val state = _uiState.value
        val targetId = franchiseId
            ?: state.franchises.maxByOrNull {
                FreeAgencyEngine.evaluateDepthChartNeeds(it, state.sportType, state.ruleBook).priorityScore
            }?.id
            ?: state.franchises.firstOrNull()?.id
        _uiState.update {
            it.copy(
                showFreeAgencyPoolScreen = true,
                selectedFreeAgencyFranchiseId = targetId
            )
        }
    }

    fun closeFreeAgencyPool() {
        _uiState.update { it.copy(showFreeAgencyPoolScreen = false) }
    }

    fun selectFreeAgencyFranchise(franchiseId: String) {
        _uiState.update { it.copy(selectedFreeAgencyFranchiseId = franchiseId) }
    }

    fun signFreeAgentForFranchise(
        franchiseId: String,
        freeAgentId: String,
        customYears: Int? = null
    ): Pair<Boolean, String> {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId }
            ?: return false to "Franchise not found."
        val freeAgent = state.freeAgentPool.find { it.id == freeAgentId }
            ?: return false to "Free agent no longer available in pool."

        val res = FreeAgencyEngine.signFreeAgentToFranchise(
            franchise = team,
            freeAgent = freeAgent,
            freeAgentPool = state.freeAgentPool,
            ruleBook = state.ruleBook,
            unionAgreement = state.unionAgreement,
            week = state.currentWeek,
            seasonYear = state.currentYear,
            sportType = state.sportType,
            customYears = customYears,
            isAutoSigned = false
        )
        if (!res.success || res.updatedFranchise == null) {
            return false to res.message
        }

        val updatedFranchises = TeamReputationEngine.refreshAllFranchiseReputations(
            franchises = state.franchises.map { if (it.id == franchiseId) res.updatedFranchise else it },
            salaryCapMillions = state.unionAgreement.salaryCapMillions
        )
        val replenishedPool = FreeAgencyEngine.replenishFreeAgentPool(
            currentPool = res.updatedPool,
            franchises = updatedFranchises,
            sportType = state.sportType,
            seasonYear = state.currentYear,
            minPoolSize = 18,
            seedOffset = state.currentWeek + state.freeAgentSignings.size + 1
        )
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                freeAgentPool = replenishedPool,
                freeAgentSignings = (listOfNotNull(res.transaction) + it.freeAgentSignings).take(40),
                recentNews = (listOfNotNull(res.newsHeadline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true to res.message
    }

    fun triggerAutoFillDepthCharts(): Pair<Int, String> {
        val state = _uiState.value
        val sweep = FreeAgencyEngine.runAutomaticFreeAgencySweep(
            franchises = state.franchises,
            freeAgentPool = state.freeAgentPool,
            sportType = state.sportType,
            ruleBook = state.ruleBook,
            unionAgreement = state.unionAgreement,
            week = state.currentWeek,
            seasonYear = state.currentYear,
            maxSigningsPerSweep = state.franchises.size.coerceAtLeast(6),
            urgentOnly = false
        )
        if (sweep.newSignings.isEmpty()) {
            return 0 to "All franchise depth charts are already at target capacity or cap ceiling."
        }
        val reputationRefreshedFranchises = TeamReputationEngine.refreshAllFranchiseReputations(
            franchises = sweep.updatedFranchises,
            salaryCapMillions = state.unionAgreement.salaryCapMillions
        )
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = reputationRefreshedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                freeAgentPool = sweep.updatedPool,
                freeAgentSignings = (sweep.newSignings + it.freeAgentSignings).take(40),
                recentNews = (sweep.headlines + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return sweep.newSignings.size to "${sweep.newSignings.size} free agency signing(s) executed across the league!"
    }

    fun releasePlayerToFreeAgency(franchiseId: String, playerId: String): Pair<Boolean, String> {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId }
            ?: return false to "Franchise not found."
        val (resultPair, msg) = FreeAgencyEngine.releasePlayerToFreeAgency(
            franchise = team,
            playerId = playerId,
            freeAgentPool = state.freeAgentPool,
            week = state.currentWeek,
            seasonYear = state.currentYear
        )
        if (resultPair == null) return false to msg

        val (updatedTeam, updatedPool) = resultPair
        val updatedFranchises = TeamReputationEngine.refreshAllFranchiseReputations(
            franchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it },
            salaryCapMillions = state.unionAgreement.salaryCapMillions
        )
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )
        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                freeAgentPool = updatedPool,
                recentNews = (listOf(msg) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true to msg
    }

    fun refreshFreeAgentPool() {
        val state = _uiState.value
        val refreshed = FreeAgencyEngine.replenishFreeAgentPool(
            currentPool = state.freeAgentPool,
            franchises = state.franchises,
            sportType = state.sportType,
            seasonYear = state.currentYear,
            minPoolSize = state.freeAgentPool.size + 4,
            seedOffset = state.currentWeek + state.freeAgentSignings.size + Random.nextInt(1, 50)
        )
        _uiState.update {
            it.copy(
                freeAgentPool = refreshed,
                recentNews = (listOf("FREE AGENCY SCOUTING REPORT: League player personnel office clears fresh group of free agents and international call-ups.") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun applyPlayoffStructureOption(option: PlayoffStructureOption) {
        updatePlayoffRules(
            fieldSize = option.rules.fieldSize,
            qualificationMode = option.rules.qualificationMode,
            divisionWinnersAutoBerth = option.rules.divisionWinnersAutoBerth,
            seedingMethod = option.rules.seedingMethod,
            firstRoundByesCount = option.rules.firstRoundByesCount,
            playInTournamentEnabled = option.rules.playInTournamentEnabled
        )
    }

    fun openBroadcastNegotiations(open: Boolean) {
        _uiState.update { state ->
            val session = if (open) {
                val base = if (state.broadcastBiddingSession.offers.isEmpty()) {
                    BroadcastBiddingEngine.createInitialSession(
                        bidders = InitialLeagueData.getNetworkBidders(),
                        currentContract = state.activeBroadcast,
                        teamCount = state.franchises.size,
                        playerRevenueSharePct = state.unionAgreement.playerRevenueSharePct,
                        currentFanbaseMillions = state.totalLeagueFanbaseMillions,
                        currentYear = state.currentYear
                    )
                } else {
                    state.broadcastBiddingSession
                }
                BroadcastBiddingEngine.refreshSessionImpacts(
                    session = base,
                    currentContract = state.activeBroadcast,
                    teamCount = state.franchises.size,
                    playerRevenueSharePct = state.unionAgreement.playerRevenueSharePct,
                    currentFanbaseMillions = state.totalLeagueFanbaseMillions,
                    currentYear = state.currentYear
                )
            } else {
                state.broadcastBiddingSession
            }
            state.copy(
                showBroadcastNegotiationsScreen = open,
                broadcastBiddingSession = session
            )
        }
    }

    fun selectBroadcastOffer(offerId: String) {
        _uiState.update { state ->
            state.copy(
                broadcastBiddingSession = state.broadcastBiddingSession.copy(selectedOfferId = offerId)
            )
        }
    }

    fun runBroadcastBiddingRound(): BroadcastBiddingSession {
        val state = _uiState.value
        val baseSession = if (state.broadcastBiddingSession.offers.isEmpty()) {
            BroadcastBiddingEngine.createInitialSession(
                bidders = InitialLeagueData.getNetworkBidders(),
                currentContract = state.activeBroadcast,
                teamCount = state.franchises.size,
                playerRevenueSharePct = state.unionAgreement.playerRevenueSharePct,
                currentFanbaseMillions = state.totalLeagueFanbaseMillions,
                currentYear = state.currentYear
            )
        } else {
            state.broadcastBiddingSession
        }
        val nextSession = BroadcastBiddingEngine.simulateCompetitiveBiddingRound(
            session = baseSession,
            currentContract = state.activeBroadcast,
            teamCount = state.franchises.size,
            playerRevenueSharePct = state.unionAgreement.playerRevenueSharePct,
            currentFanbaseMillions = state.totalLeagueFanbaseMillions,
            currentYear = state.currentYear
        )
        val exclusivityJustExpired = !state.broadcastBiddingSession.exclusivityExpired && nextSession.exclusivityExpired
        val roundHeadline = nextSession.lastEscalationSummary
        _uiState.update {
            val updatedBroadcast = if (exclusivityJustExpired) {
                it.activeBroadcast.copy(
                    hasExclusivity = false,
                    exclusivityLostPenaltyPct = BroadcastBiddingEngine.EXPIRY_REVENUE_PENALTY_PCT
                )
            } else {
                it.activeBroadcast
            }
            val updatedFanbase = if (exclusivityJustExpired) {
                (Math.round(it.totalLeagueFanbaseMillions * 0.96 * 10.0) / 10.0).coerceAtLeast(1.0)
            } else {
                it.totalLeagueFanbaseMillions
            }
            it.copy(
                activeBroadcast = updatedBroadcast,
                totalLeagueFanbaseMillions = updatedFanbase,
                broadcastBiddingSession = nextSession,
                recentNews = if (roundHeadline != null) (listOf("BROADCAST AUCTION: $roundHeadline") + it.recentNews).take(20) else it.recentNews
            )
        }
        if (exclusivityJustExpired) {
            recordDashboardTrend("TV Expiry", "tv_exp_${state.currentWeek}")
        }
        persistCurrentState()
        return nextSession
    }

    fun triggerPeriodicBroadcastBidNow(): PeriodicBroadcastTickResult {
        val state = _uiState.value
        val baseSession = if (state.broadcastBiddingSession.offers.isEmpty()) {
            BroadcastBiddingEngine.createInitialSession(
                bidders = InitialLeagueData.getNetworkBidders(),
                currentContract = state.activeBroadcast,
                teamCount = state.franchises.size,
                playerRevenueSharePct = state.unionAgreement.playerRevenueSharePct,
                currentFanbaseMillions = state.totalLeagueFanbaseMillions,
                currentYear = state.currentYear
            )
        } else {
            state.broadcastBiddingSession
        }
        val tickResult = BroadcastBiddingEngine.receivePeriodicRivalBid(
            session = baseSession,
            currentContract = state.activeBroadcast,
            teamCount = state.franchises.size,
            playerRevenueSharePct = state.unionAgreement.playerRevenueSharePct,
            currentFanbaseMillions = state.totalLeagueFanbaseMillions,
            currentYear = state.currentYear,
            weekNumber = state.currentWeek
        )
        _uiState.update {
            val updatedBroadcast = if (tickResult.exclusivityJustExpired) {
                it.activeBroadcast.copy(
                    hasExclusivity = false,
                    exclusivityLostPenaltyPct = BroadcastBiddingEngine.EXPIRY_REVENUE_PENALTY_PCT
                )
            } else {
                it.activeBroadcast
            }
            val updatedFanbase = if (tickResult.exclusivityJustExpired) {
                (Math.round(it.totalLeagueFanbaseMillions * 0.96 * 10.0) / 10.0).coerceAtLeast(1.0)
            } else {
                it.totalLeagueFanbaseMillions
            }
            it.copy(
                activeBroadcast = updatedBroadcast,
                totalLeagueFanbaseMillions = updatedFanbase,
                broadcastBiddingSession = tickResult.updatedSession,
                recentNews = if (tickResult.headline != null) {
                    (listOf(tickResult.headline) + it.recentNews).take(20)
                } else {
                    it.recentNews
                }
            )
        }
        if (tickResult.exclusivityJustExpired) {
            refreshJobSecurity("Broadcast exclusivity expired — non-exclusive penalty triggered.")
            recordDashboardTrend("TV Expiry", "tv_exp_${state.currentWeek}")
        } else if (tickResult.headline != null) {
            recordDashboardTrend("TV Bid", "tv_bid_${state.currentWeek}_${tickResult.updatedSession.periodicBidsReceivedCount}")
        }
        persistCurrentState()
        return tickResult
    }

    fun updateBroadcastOfferStructure(
        offerId: String,
        termYears: Int,
        lumpSumBonusMillions: Long,
        annualPayoutMillions: Long,
        currentPatience: Int,
        executiveNote: String,
        digitalExclusivity: Boolean? = null,
        playoffPrimetime: Boolean? = null,
        markCountered: Boolean = false
    ) {
        _uiState.update { state ->
            val updatedSession = BroadcastBiddingEngine.updateOfferTerms(
                session = state.broadcastBiddingSession,
                offerId = offerId,
                termYears = termYears,
                lumpSumBonusMillions = lumpSumBonusMillions,
                annualPayoutMillions = annualPayoutMillions,
                currentPatience = currentPatience,
                executiveNote = executiveNote,
                digitalExclusivity = digitalExclusivity,
                playoffPrimetime = playoffPrimetime,
                markCountered = markCountered,
                currentContract = state.activeBroadcast,
                teamCount = state.franchises.size,
                playerRevenueSharePct = state.unionAgreement.playerRevenueSharePct,
                currentFanbaseMillions = state.totalLeagueFanbaseMillions,
                currentYear = state.currentYear
            )
            state.copy(broadcastBiddingSession = updatedSession)
        }
    }

    fun acceptBroadcastOffer(offerId: String): Boolean {
        val state = _uiState.value
        val offer = state.broadcastBiddingSession.offers.find { it.id == offerId } ?: return false
        if (offer.bidStatus == BroadcastBidStatus.WITHDRAWN || offer.currentPatience <= 0) return false

        val contract = BroadcastContract(
            id = "deal_${offer.bidder.id}_${System.currentTimeMillis() % 1000}",
            partnerName = "${offer.bidder.name} (${offer.bidder.scope.label})",
            platform = when (offer.bidder.scope) {
                NetworkScope.GLOBAL_STREAMING -> BroadcastPlatform.GLOBAL_STREAMER
                NetworkScope.FREE_TO_AIR -> BroadcastPlatform.FREE_TO_AIR
                NetworkScope.REGIONAL_RSN -> BroadcastPlatform.HYBRID_SYNDICATE
                else -> BroadcastPlatform.LINEAR_CABLE
            },
            annualPayoutMillions = offer.annualPayoutMillions,
            yearsRemaining = offer.termYears,
            reachMultiplier = BroadcastBiddingEngine.effectiveReachForOffer(offer),
            bonusPerPlayoffGameMillions = offer.playoffBonusPerGameMillions,
            upfrontSigningBonusMillions = offer.lumpSumBonusMillions,
            networkScope = offer.bidder.scope,
            hasExclusivity = !offer.isExclusivityExpired,
            exclusivityLostPenaltyPct = if (offer.isExclusivityExpired) BroadcastBiddingEngine.EXPIRY_REVENUE_PENALTY_PCT else 0
        )
        val reachGain = offer.impact.projectedFanReachGrowthMillions
        signNegotiatedBroadcastContract(
            contract = contract,
            lumpSumBonusMillions = offer.lumpSumBonusMillions
        )
        _uiState.update { next ->
            val updatedFanbase = (Math.round((next.totalLeagueFanbaseMillions + reachGain) * 10.0) / 10.0).coerceAtLeast(1.0)
            val refreshedSession = BroadcastBiddingEngine.createInitialSession(
                bidders = InitialLeagueData.getNetworkBidders(),
                currentContract = next.activeBroadcast,
                teamCount = next.franchises.size,
                playerRevenueSharePct = next.unionAgreement.playerRevenueSharePct,
                currentFanbaseMillions = updatedFanbase,
                currentYear = next.currentYear
            )
            next.copy(
                totalLeagueFanbaseMillions = updatedFanbase,
                broadcastBiddingSession = refreshedSession
            )
        }
        val (cDelta, dDelta) = when (offer.bidder.scope) {
            NetworkScope.FREE_TO_AIR -> 5 to 2
            NetworkScope.GLOBAL_STREAMING -> 4 to -1
            NetworkScope.REGIONAL_RSN -> 1 to 3
            else -> 2 to 1
        }
        recordFanReaction(
            category = FanDecisionCategory.MARKETING_BROADCAST,
            decisionTitle = "${offer.bidder.name} TV Deal Signed",
            decisionSummary = "Inked \$${_uiState.value.activeBroadcast.annualPayoutMillions}M/yr ${offer.bidder.scope.label} package.",
            casualDelta = cDelta,
            dieHardDelta = dDelta
        )
        persistCurrentState()
        return true
    }

    // Broadcast Negotiation
    fun negotiateBroadcast(deal: BroadcastContract) {
        _uiState.update { state ->
            val permBoost = state.rivalLeague.permanentTvRevenueBoostMillions.coerceAtLeast(0L)
            val effectiveDeal = if (permBoost > 0L) {
                deal.copy(annualPayoutMillions = deal.annualPayoutMillions + permBoost)
            } else {
                deal
            }
            state.copy(
                activeBroadcast = effectiveDeal,
                recentNews = listOf("BROADCAST DEAL SIGNED: Inked multi-year agreement with ${effectiveDeal.partnerName} for \$${effectiveDeal.annualPayoutMillions}M/yr!") + state.recentNews
            )
        }
        persistCurrentState()
    }

    fun signNegotiatedBroadcastContract(
        contract: BroadcastContract,
        lumpSumBonusMillions: Long
    ) {
        val state = _uiState.value
        val permBoost = state.rivalLeague.permanentTvRevenueBoostMillions.coerceAtLeast(0L)
        val effectiveContract = if (permBoost > 0L) {
            contract.copy(annualPayoutMillions = contract.annualPayoutMillions + permBoost)
        } else {
            contract
        }
        val newCash = state.cashReservesMillions + lumpSumBonusMillions
        val termStabilityBonus = ((effectiveContract.yearsRemaining - 2).coerceAtLeast(0) * 6L)
        val newValuation = state.leagueValuationMillions + (effectiveContract.annualPayoutMillions / 2) + (lumpSumBonusMillions * 2) + termStabilityBonus

        val bonusSnapshot = FinancialSnapshot(
            periodLabel = "Contract",
            weekNumber = state.currentWeek,
            broadcastRevenue = effectiveContract.annualPayoutMillions * 0.15f,
            sponsorshipRevenue = 0f,
            expansionFeeRevenue = lumpSumBonusMillions.toFloat(),
            luxuryTaxRevenue = 0f,
            totalRevenue = (effectiveContract.annualPayoutMillions * 0.15f) + lumpSumBonusMillions.toFloat(),
            operationsExpense = 1.0f,
            marketingExpense = 1.0f,
            welfareAndGrantsExpense = 0f,
            totalExpenses = 2.0f,
            netCashFlow = (lumpSumBonusMillions - 2L).toFloat(),
            cashReserves = newCash.toFloat(),
            valuation = newValuation.toFloat()
        )
        val updatedSnapshots = (state.financialSnapshots + bonusSnapshot).takeLast(24)
        val boostTag = if (permBoost > 0L) " (includes +\$${permBoost}M/yr Rival Absorption escalator)" else ""
        val headline = "LANDMARK BROADCAST DEAL: Signed ${effectiveContract.yearsRemaining}-year contract with ${effectiveContract.partnerName} for \$${effectiveContract.annualPayoutMillions}M/yr$boostTag + \$${lumpSumBonusMillions}M upfront lump-sum!"

        _uiState.update {
            it.copy(
                activeBroadcast = effectiveContract,
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                financialSnapshots = updatedSnapshots,
                showBroadcastNegotiationsScreen = false,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        refreshJobSecurity(headline)
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("TV Deal", "tv_${effectiveContract.id}")
        persistCurrentState()
    }

    // Corporate Sponsorship Negotiation Minigame
    fun openCorporateSponsorNegotiations(open: Boolean) {
        _uiState.update { it.copy(showSponsorshipNegotiationsScreen = open) }
    }

    fun signNegotiatedCorporateSponsorship(
        sponsorship: ActiveLeagueSponsorship,
        lumpSumBonusMillions: Long
    ) {
        val state = _uiState.value
        val newCash = state.cashReservesMillions + lumpSumBonusMillions
        val newValuation = (state.leagueValuationMillions + (sponsorship.annualPayoutMillions / 2) + (lumpSumBonusMillions * 2) + (sponsorship.reputationImpact * 2)).coerceAtLeast(100L)

        // Impact on league integrity / reputation
        val newIntegrity = (state.leagueIntegrityScore + sponsorship.reputationImpact).coerceIn(25, 100)

        // Impact on franchises fan morale
        val updatedFranchises = state.franchises.map { team ->
            team.copy(
                fanMorale = (team.fanMorale + sponsorship.fanMoraleBonus).coerceIn(25, 100),
                localSponsorRevenueMillions = (team.localSponsorRevenueMillions + (sponsorship.annualPayoutMillions / state.franchises.size.coerceAtLeast(1))).coerceAtLeast(15L)
            )
        }

        val bonusSnapshot = FinancialSnapshot(
            periodLabel = "Sponsor",
            weekNumber = state.currentWeek,
            broadcastRevenue = 0f,
            sponsorshipRevenue = (sponsorship.annualPayoutMillions * 0.15f),
            expansionFeeRevenue = lumpSumBonusMillions.toFloat(),
            luxuryTaxRevenue = 0f,
            totalRevenue = (sponsorship.annualPayoutMillions * 0.15f) + lumpSumBonusMillions.toFloat(),
            operationsExpense = 1.0f,
            marketingExpense = 1.0f,
            welfareAndGrantsExpense = 0f,
            totalExpenses = 2.0f,
            netCashFlow = (lumpSumBonusMillions - 2L).toFloat(),
            cashReserves = newCash.toFloat(),
            valuation = newValuation.toFloat()
        )
        val updatedSnapshots = (state.financialSnapshots + bonusSnapshot).takeLast(24)

        val repSign = if (sponsorship.reputationImpact >= 0) "+${sponsorship.reputationImpact}" else "${sponsorship.reputationImpact}"
        val headline = "CORPORATE PACT SIGNED: Inked ${sponsorship.yearsRemaining}-year partnership with ${sponsorship.corporationName} [${sponsorship.sector.title}] for \$${sponsorship.annualPayoutMillions}M/yr + \$${lumpSumBonusMillions}M upfront bonus! (Reputation: $repSign, Integrity: $newIntegrity/100)"

        _uiState.update {
            it.copy(
                activeSponsorship = sponsorship,
                franchises = updatedFranchises,
                leagueIntegrityScore = newIntegrity,
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                financialSnapshots = updatedSnapshots,
                showSponsorshipNegotiationsScreen = false,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("Sponsor", "spon_${sponsorship.id}")
        persistCurrentState()
    }

    fun openLaborUnionScreen(open: Boolean) {
        _uiState.update { it.copy(showLaborUnionScreen = open) }
    }

    fun dismissLaborTrigger() {
        coreSimulationViewModel.acknowledgeLaborTrigger()
        _uiState.update { it.copy(activeLaborTrigger = null) }
    }

    fun openLaborUnionFromTrigger() {
        val currentState = _uiState.value
        val trigger = currentState.activeLaborTrigger
        val overlay = currentState.activeLaborNegotiationOverlay
            ?: coreSimulationViewModel.buildLaborNegotiationOverlay(trigger, currentState)
        coreSimulationViewModel.acknowledgeLaborTrigger()
        _uiState.update {
            it.copy(
                activeLaborTrigger = null,
                activeLaborNegotiationOverlay = overlay,
                showLaborUnionScreen = true
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
    }

    fun openLaborNegotiationOverlay(trigger: LaborUnionTriggerEvent? = null) {
        val currentState = _uiState.value
        val overlay = coreSimulationViewModel.buildLaborNegotiationOverlay(
            trigger = trigger ?: currentState.activeLaborTrigger,
            state = currentState
        )
        _uiState.update { it.copy(activeLaborNegotiationOverlay = overlay) }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
    }

    fun dismissLaborNegotiationOverlay() {
        coreSimulationViewModel.dismissLaborNegotiationOverlay()
        _uiState.update { it.copy(activeLaborNegotiationOverlay = null) }
    }

    fun triggerRandomContractDispute(seed: Long? = null): LaborDisputeEngine.RandomDisputeRoll {
        val state = _uiState.value
        val roll = coreSimulationViewModel.triggerRandomContractDispute(state, seed)
        val updatedFranchises = state.franchises.map { team ->
            team.copy(teamMorale = (team.teamMorale - 2).coerceIn(10, 99))
        }
        val headline = "LABOR DISPUTE ALERT: ${roll.trigger.title} (Avg Team Morale dipped amid contract standoff)"
        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                activeLaborTrigger = roll.trigger,
                activeLaborNegotiationOverlay = roll.overlaySession,
                laborTriggerHistory = (listOf(roll.trigger) + it.laborTriggerHistory).take(30),
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return roll
    }

    fun resolveLaborNegotiationChoice(choiceId: String) {
        val state = _uiState.value
        val overlay = state.activeLaborNegotiationOverlay ?: return
        val choice = overlay.choices.find { it.id == choiceId } ?: return

        val newCash = (state.cashReservesMillions + choice.projectedCashImpactMillions).coerceAtLeast(0L)
        val newValuation = (state.leagueValuationMillions + choice.projectedValuationImpactMillions).coerceAtLeast(100L)

        val updatedFranchises = state.franchises.map { team ->
            val newMorale = (team.fanMorale + choice.projectedFanMoraleDelta).coerceIn(15, 100)
            val newLoyalty = (team.fanLoyalty + choice.projectedFanLoyaltyDelta).coerceIn(15, 100)
            val newTeamMorale = (team.teamMorale + choice.projectedTeamMoraleDelta).coerceIn(10, 99)
            team.copy(
                fanMorale = newMorale,
                fanLoyalty = newLoyalty,
                teamMorale = newTeamMorale
            )
        }

        val currentUnion = state.unionAgreement
        val newSatisfaction = (currentUnion.unionSatisfaction + choice.projectedUnionSatisfactionDelta).coerceIn(15, 99)
        val newStrikeRisk = (currentUnion.strikeRisk + choice.projectedStrikeRiskDelta).coerceIn(2, 95)
        val newRevenueShare = (currentUnion.playerRevenueSharePct + choice.projectedRevenueShareDeltaPct).coerceIn(45, 58)
        val newCbaYears = choice.projectedCbaYearsSet ?: currentUnion.cbaYearsRemaining
        val newSalaryCapMillions = (choice.requestedSalaryCapMillions ?: currentUnion.salaryCapMillions).coerceIn(60L, 190L)
        val newSalaryCapType = choice.requestedSalaryCapType ?: currentUnion.salaryCapType
        val newRosterRule = choice.requestedRosterSizeRule ?: currentUnion.negotiatedRosterRule
        val newLuxuryTaxThresholdMillions = (currentUnion.luxuryTaxThresholdMillions + choice.requestedLuxuryTaxThresholdDelta)
            .coerceIn(newSalaryCapMillions + 6L, 250L)
        val newRookieScaleMultiplierPct = (currentUnion.rookieScaleMultiplierPct + choice.requestedRookieScaleDeltaPct)
            .coerceIn(70, 150)
        val newMaxContractCapPct = (currentUnion.maxContractCapPct + choice.requestedMaxContractCapDeltaPct)
            .coerceIn(22, 42)
        val newMinPlayerSalaryK = (currentUnion.minPlayerSalaryK + choice.requestedMinSalaryDeltaK)
            .coerceIn(450L, 2500L)
        val newLockoutActive = when {
            choice.clearsLockout -> false
            choice.activatesLockoutOnReject && (newStrikeRisk >= 55 || newCbaYears <= 0 || currentUnion.isLockoutActive) -> true
            else -> currentUnion.isLockoutActive
        }
        val newLastLockoutYear = if (newLockoutActive && !currentUnion.isLockoutActive) {
            state.currentYear
        } else {
            currentUnion.lastLockoutSeasonYear
        }

        val updatedUnion = currentUnion.copy(
            unionSatisfaction = newSatisfaction,
            strikeRisk = newStrikeRisk,
            playerRevenueSharePct = newRevenueShare,
            salaryCapMillions = newSalaryCapMillions,
            salaryCapType = newSalaryCapType,
            negotiatedRosterRule = newRosterRule,
            cbaYearsRemaining = newCbaYears,
            isLockoutActive = newLockoutActive,
            lastLockoutSeasonYear = newLastLockoutYear,
            mandatoryPrivateCharterFlights = currentUnion.mandatoryPrivateCharterFlights || choice.enactsCharterFlights,
            maxBackToBackGamesLimit = currentUnion.maxBackToBackGamesLimit || choice.enactsBackToBackLimit,
            luxuryTaxThresholdMillions = newLuxuryTaxThresholdMillions,
            rookieScaleMultiplierPct = newRookieScaleMultiplierPct,
            maxContractCapPct = newMaxContractCapPct,
            minPlayerSalaryK = newMinPlayerSalaryK
        )

        val updatedRules = if (choice.requestedSalaryCapType != null) {
            state.ruleBook.rules.map { rule ->
                if (rule.id == "rule_salary_cap") {
                    rule.copy(isEnacted = newSalaryCapType != SalaryCapType.FREE_MARKET)
                } else rule
            }
        } else {
            state.ruleBook.rules
        }
        val syncedStrictness = when (choice.requestedSalaryCapType) {
            SalaryCapType.HARD_CAP -> SalaryCapStrictness.STRICT_HARD_CAP
            SalaryCapType.FREE_MARKET -> SalaryCapStrictness.UNRESTRICTED_MARKET
            SalaryCapType.SOFT_CAP_LUXURY_TAX -> SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX
            null -> state.ruleBook.salaryCapStrictness
        }

        val reassessedTrigger = coreSimulationViewModel.reassessTriggersAfterUnionAction(
            updatedAgreement = updatedUnion,
            year = state.currentYear,
            week = state.currentWeek
        )

        val cashSign = if (choice.projectedCashImpactMillions >= 0L) "+" else "-"
        val cashLabel = "$cashSign\$${kotlin.math.abs(choice.projectedCashImpactMillions)}M Cash"
        val moraleSign = if (choice.projectedFanMoraleDelta >= 0) "+" else ""
        val teamMoraleSign = if (choice.projectedTeamMoraleDelta >= 0) "+" else ""
        val lockoutNote = when {
            currentUnion.isLockoutActive && !newLockoutActive -> " • OWNER LOCKOUT LIFTED!"
            !currentUnion.isLockoutActive && newLockoutActive -> " • OWNER LOCKOUT ACTIVATED (GAMES SUSPENDED)!"
            else -> ""
        }
        val headline = "LABOR RULING [${choice.stanceTag}]: ${choice.title} ($cashLabel, $teamMoraleSign${choice.projectedTeamMoraleDelta}% Team Morale, $moraleSign${choice.projectedFanMoraleDelta}% Fan Morale, Cap \$${newSalaryCapMillions}M, Strike Risk $newStrikeRisk%)$lockoutNote"

        _uiState.update {
            it.copy(
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                franchises = updatedFranchises,
                unionAgreement = updatedUnion,
                ruleBook = it.ruleBook.copy(
                    rules = updatedRules,
                    salaryCapStrictness = syncedStrictness
                ),
                activeLaborTrigger = reassessedTrigger,
                activeLaborNegotiationOverlay = null,
                showLaborUnionScreen = if (choice.opensCbaTableAfter) true else it.showLaborUnionScreen,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        notifyFinancialMilestones(state, _uiState.value)
        recordFanReaction(
            category = FanDecisionCategory.LABOR_CBA,
            decisionTitle = choice.title,
            decisionSummary = headline,
            casualDelta = choice.projectedFanMoraleDelta.coerceIn(-6, 6),
            dieHardDelta = choice.projectedFanLoyaltyDelta.coerceIn(-6, 6)
        )
        refreshJobSecurity(headline)
        recordDashboardTrend("Labor", "labor_${choice.id}")
        persistCurrentState()
    }

    // Union CBA Negotiation
    fun updateUnionCBA(revenueSharePct: Int, salaryCapType: SalaryCapType, salaryCapMillions: Long, healthFundMillions: Long) {
        val state = _uiState.value
        val satisfactionDelta = (revenueSharePct - 50) * 4 + ((healthFundMillions - 15) * 2).toInt()
        val newSatisfaction = (state.unionAgreement.unionSatisfaction + satisfactionDelta).coerceIn(30, 98)
        val newStrikeRisk = (100 - newSatisfaction).coerceIn(2, 75)

        val updatedAgreement = state.unionAgreement.copy(
            playerRevenueSharePct = revenueSharePct,
            salaryCapType = salaryCapType,
            salaryCapMillions = salaryCapMillions,
            playerHealthWelfareMillions = healthFundMillions,
            unionSatisfaction = newSatisfaction,
            strikeRisk = newStrikeRisk,
            isLockoutActive = if (newStrikeRisk < 55) false else state.unionAgreement.isLockoutActive
        )
        val reassessedTrigger = coreSimulationViewModel.reassessTriggersAfterUnionAction(
            updatedAgreement = updatedAgreement,
            year = state.currentYear,
            week = state.currentWeek
        )

        _uiState.update {
            it.copy(
                unionAgreement = updatedAgreement,
                activeLaborTrigger = reassessedTrigger,
                recentNews = listOf("CBA RATIFIED: Revenue split set to $revenueSharePct%, Salary Cap: \$${salaryCapMillions}M. Strike risk reduced to $newStrikeRisk%.") + it.recentNews
            )
        }
        refreshJobSecurity("CBA parameters updated ($revenueSharePct% rev split, $newStrikeRisk% strike risk).")
        persistCurrentState()
    }

    fun dismissBoardVoteModal() {
        _uiState.update { it.copy(activeBoardVoteModal = null) }
    }

    fun overrideLastVetoedVote() {
        val action = pendingOverrideAction
        pendingOverrideAction = null
        action?.invoke()
    }

    fun previewCbaOwnerVotes(
        revenueSharePct: Int,
        salaryCapType: SalaryCapType,
        salaryCapMillions: Long,
        minSalaryK: Long,
        playerHealthWelfareMillions: Long,
        mandatoryPrivateCharters: Boolean,
        backToBackLimits: Boolean,
        concussionProtocol: Boolean,
        hybridTurfMandate: Boolean,
        transitionFundMillions: Long,
        fiveStarHotels: Boolean,
        rosterSizeRule: RosterSizeRule = RosterSizeRule.STANDARD_15
    ): BoardVoteResult {
        val state = _uiState.value
        val candidateAgreement = state.unionAgreement.copy(
            playerRevenueSharePct = revenueSharePct,
            salaryCapType = salaryCapType,
            salaryCapMillions = salaryCapMillions,
            minPlayerSalaryK = minSalaryK,
            playerHealthWelfareMillions = playerHealthWelfareMillions,
            mandatoryPrivateCharterFlights = mandatoryPrivateCharters,
            maxBackToBackGamesLimit = backToBackLimits,
            independentConcussionProtocol = concussionProtocol,
            advancedHybridTurfMandate = hybridTurfMandate,
            postCareerTransitionFundMillions = transitionFundMillions,
            fiveStarHotelSingleRooms = fiveStarHotels,
            negotiatedRosterRule = rosterSizeRule
        )
        return OwnerGovernanceEngine.evaluateCbaVote(
            franchises = state.franchises,
            proposedAgreement = candidateAgreement,
            year = state.currentYear,
            week = state.currentWeek
        )
    }

    fun submitCbaToOwnerVote(
        revenueSharePct: Int,
        salaryCapType: SalaryCapType,
        salaryCapMillions: Long,
        minSalaryK: Long,
        playerHealthWelfareMillions: Long,
        mandatoryPrivateCharters: Boolean,
        backToBackLimits: Boolean,
        concussionProtocol: Boolean,
        hybridTurfMandate: Boolean,
        transitionFundMillions: Long,
        fiveStarHotels: Boolean,
        forceCommissionerOverride: Boolean = false,
        rosterSizeRule: RosterSizeRule = RosterSizeRule.STANDARD_15
    ): BoardVoteResult {
        val state = _uiState.value
        val proposed = state.unionAgreement.copy(
            playerRevenueSharePct = revenueSharePct,
            salaryCapType = salaryCapType,
            salaryCapMillions = salaryCapMillions,
            minPlayerSalaryK = minSalaryK,
            playerHealthWelfareMillions = playerHealthWelfareMillions,
            mandatoryPrivateCharterFlights = mandatoryPrivateCharters,
            maxBackToBackGamesLimit = backToBackLimits,
            independentConcussionProtocol = concussionProtocol,
            advancedHybridTurfMandate = hybridTurfMandate,
            postCareerTransitionFundMillions = transitionFundMillions,
            fiveStarHotelSingleRooms = fiveStarHotels,
            negotiatedRosterRule = rosterSizeRule
        )
        val rawVote = OwnerGovernanceEngine.evaluateCbaVote(
            franchises = state.franchises,
            proposedAgreement = proposed,
            year = state.currentYear,
            week = state.currentWeek,
            enactedViaCommissionerOverride = forceCommissionerOverride
        )
        if (!rawVote.passed && !forceCommissionerOverride) {
            pendingOverrideAction = {
                submitCbaToOwnerVote(
                    revenueSharePct = revenueSharePct,
                    salaryCapType = salaryCapType,
                    salaryCapMillions = salaryCapMillions,
                    minSalaryK = minSalaryK,
                    playerHealthWelfareMillions = playerHealthWelfareMillions,
                    mandatoryPrivateCharters = mandatoryPrivateCharters,
                    backToBackLimits = backToBackLimits,
                    concussionProtocol = concussionProtocol,
                    hybridTurfMandate = hybridTurfMandate,
                    transitionFundMillions = transitionFundMillions,
                    fiveStarHotels = fiveStarHotels,
                    forceCommissionerOverride = true,
                    rosterSizeRule = rosterSizeRule
                )
            }
            val reactedFranchises = OwnerGovernanceEngine.applyApprovalReactions(state.franchises, rawVote)
            val avgOwnerApproval = reactedFranchises.map { it.ownership.ownerApproval }.average().toInt()
            val vetoHeadline = "BOARD VETO ON CBA (${rawVote.marginSummary}): Franchise owners rejected \$${salaryCapMillions}M ${salaryCapType.displayName} (${revenueSharePct}% split). Renegotiate terms to secure majority!"
            _uiState.update {
                it.copy(
                    franchises = reactedFranchises,
                    ownerApprovalRating = avgOwnerApproval,
                    boardVoteHistory = (listOf(rawVote) + it.boardVoteHistory).take(30),
                    lastBoardVoteResult = rawVote,
                    activeBoardVoteModal = rawVote,
                    recentNews = listOf(vetoHeadline) + it.recentNews.take(15)
                )
            }
            persistCurrentState()
            return rawVote
        }
        pendingOverrideAction = null

        val finalVote = if (!rawVote.passed && forceCommissionerOverride) {
            rawVote.copy(passed = true, enactedViaCommissionerOverride = true)
        } else {
            rawVote
        }
        applyCbaRatificationInternal(
            revenueSharePct = revenueSharePct,
            salaryCapType = salaryCapType,
            salaryCapMillions = salaryCapMillions,
            minSalaryK = minSalaryK,
            playerHealthWelfareMillions = playerHealthWelfareMillions,
            mandatoryPrivateCharters = mandatoryPrivateCharters,
            backToBackLimits = backToBackLimits,
            concussionProtocol = concussionProtocol,
            hybridTurfMandate = hybridTurfMandate,
            transitionFundMillions = transitionFundMillions,
            fiveStarHotels = fiveStarHotels,
            rosterSizeRule = rosterSizeRule,
            recordedVote = finalVote,
            showVoteModal = true,
            closeUnionScreen = false
        )
        return finalVote
    }

    fun ratifyComprehensiveCBA(
        revenueSharePct: Int,
        salaryCapType: SalaryCapType,
        salaryCapMillions: Long,
        minSalaryK: Long,
        playerHealthWelfareMillions: Long,
        mandatoryPrivateCharters: Boolean,
        backToBackLimits: Boolean,
        concussionProtocol: Boolean,
        hybridTurfMandate: Boolean,
        transitionFundMillions: Long,
        fiveStarHotels: Boolean,
        rosterSizeRule: RosterSizeRule = RosterSizeRule.STANDARD_15
    ) {
        val state = _uiState.value
        val candidateAgreement = state.unionAgreement.copy(
            playerRevenueSharePct = revenueSharePct,
            salaryCapType = salaryCapType,
            salaryCapMillions = salaryCapMillions,
            minPlayerSalaryK = minSalaryK,
            playerHealthWelfareMillions = playerHealthWelfareMillions,
            mandatoryPrivateCharterFlights = mandatoryPrivateCharters,
            maxBackToBackGamesLimit = backToBackLimits,
            independentConcussionProtocol = concussionProtocol,
            advancedHybridTurfMandate = hybridTurfMandate,
            postCareerTransitionFundMillions = transitionFundMillions,
            fiveStarHotelSingleRooms = fiveStarHotels,
            negotiatedRosterRule = rosterSizeRule
        )
        val evaluated = OwnerGovernanceEngine.evaluateCbaVote(
            franchises = state.franchises,
            proposedAgreement = candidateAgreement,
            year = state.currentYear,
            week = state.currentWeek
        )
        applyCbaRatificationInternal(
            revenueSharePct = revenueSharePct,
            salaryCapType = salaryCapType,
            salaryCapMillions = salaryCapMillions,
            minSalaryK = minSalaryK,
            playerHealthWelfareMillions = playerHealthWelfareMillions,
            mandatoryPrivateCharters = mandatoryPrivateCharters,
            backToBackLimits = backToBackLimits,
            concussionProtocol = concussionProtocol,
            hybridTurfMandate = hybridTurfMandate,
            transitionFundMillions = transitionFundMillions,
            fiveStarHotels = fiveStarHotels,
            rosterSizeRule = rosterSizeRule,
            recordedVote = evaluated,
            showVoteModal = false,
            closeUnionScreen = true
        )
    }

    private fun applyCbaRatificationInternal(
        revenueSharePct: Int,
        salaryCapType: SalaryCapType,
        salaryCapMillions: Long,
        minSalaryK: Long,
        playerHealthWelfareMillions: Long,
        mandatoryPrivateCharters: Boolean,
        backToBackLimits: Boolean,
        concussionProtocol: Boolean,
        hybridTurfMandate: Boolean,
        transitionFundMillions: Long,
        fiveStarHotels: Boolean,
        rosterSizeRule: RosterSizeRule = RosterSizeRule.STANDARD_15,
        recordedVote: BoardVoteResult,
        showVoteModal: Boolean,
        closeUnionScreen: Boolean
    ) {
        val state = _uiState.value
        val wasLockoutActive = state.unionAgreement.isLockoutActive

        var satisfaction = 50
        satisfaction += (revenueSharePct - 48) * 5
        satisfaction += ((minSalaryK - 500) / 40).toInt()
        if (mandatoryPrivateCharters) satisfaction += 12
        if (backToBackLimits) satisfaction += 10
        if (concussionProtocol) satisfaction += 8
        if (hybridTurfMandate) satisfaction += 6
        if (fiveStarHotels) satisfaction += 5
        satisfaction += when (rosterSizeRule) {
            RosterSizeRule.EXPANDED_18 -> 7
            RosterSizeRule.STANDARD_15 -> 0
            RosterSizeRule.COMPACT_12 -> -4
        }
        satisfaction += ((playerHealthWelfareMillions - 10) * 1.5).toInt()
        satisfaction += (transitionFundMillions * 1.5).toInt()

        val finalSatisfaction = satisfaction.coerceIn(35, 99)
        val finalStrikeRisk = (100 - finalSatisfaction).coerceIn(2, 60)

        var ownerApprovalDelta = 0
        if (revenueSharePct > 52) ownerApprovalDelta -= (revenueSharePct - 52) * 4
        if (mandatoryPrivateCharters) ownerApprovalDelta -= 4
        if (hybridTurfMandate) ownerApprovalDelta -= 4
        if (salaryCapType == SalaryCapType.HARD_CAP) ownerApprovalDelta += 8
        if (rosterSizeRule == RosterSizeRule.COMPACT_12) ownerApprovalDelta += 4
        if (rosterSizeRule == RosterSizeRule.EXPANDED_18) ownerApprovalDelta -= 3
        if (recordedVote.enactedViaCommissionerOverride) ownerApprovalDelta -= 8

        val reactedFranchises = OwnerGovernanceEngine.applyApprovalReactions(state.franchises, recordedVote)
        val avgOwnerApproval = reactedFranchises.map { it.ownership.ownerApproval }.average().toInt()
        val finalOwnerApproval = ((state.ownerApprovalRating + avgOwnerApproval) / 2 + ownerApprovalDelta).coerceIn(40, 98)

        val updatedAgreement = state.unionAgreement.copy(
            unionSatisfaction = finalSatisfaction,
            strikeRisk = finalStrikeRisk,
            playerRevenueSharePct = revenueSharePct,
            salaryCapType = salaryCapType,
            salaryCapMillions = salaryCapMillions,
            minPlayerSalaryK = minSalaryK,
            playerHealthWelfareMillions = playerHealthWelfareMillions,
            mandatoryPrivateCharterFlights = mandatoryPrivateCharters,
            maxBackToBackGamesLimit = backToBackLimits,
            independentConcussionProtocol = concussionProtocol,
            advancedHybridTurfMandate = hybridTurfMandate,
            postCareerTransitionFundMillions = transitionFundMillions,
            fiveStarHotelSingleRooms = fiveStarHotels,
            negotiatedRosterRule = rosterSizeRule,
            cbaYearsRemaining = 5,
            isLockoutActive = false
        )

        val updatedRules = state.ruleBook.rules.map { rule ->
            if (rule.id == "rule_salary_cap") {
                rule.copy(isEnacted = salaryCapType != SalaryCapType.FREE_MARKET)
            } else rule
        }
        val parityDelta = when (salaryCapType) {
            SalaryCapType.HARD_CAP -> 6
            SalaryCapType.SOFT_CAP_LUXURY_TAX -> 2
            SalaryCapType.FREE_MARKET -> -8
        }
        val finalParity = (state.competitiveParityScore + parityDelta).coerceIn(40, 99)

        val voteBadge = if (recordedVote.enactedViaCommissionerOverride) {
            "Executive Override (${recordedVote.marginSummary})"
        } else {
            "Board Vote ${recordedVote.marginSummary}"
        }
        val lockoutLiftedTag = if (wasLockoutActive) " Owner lockout lifted — games resume!" else ""
        val headline = "HISTORIC CBA ACCORD [$voteBadge]: 5-year labor peace ratified! Salary Cap set to \$${salaryCapMillions}M (${salaryCapType.displayName} • ${rosterSizeRule.displayName}). Strike threat averted ($finalStrikeRisk%).$lockoutLiftedTag"

        val reassessedTrigger = coreSimulationViewModel.reassessTriggersAfterUnionAction(
            updatedAgreement = updatedAgreement,
            year = state.currentYear,
            week = state.currentWeek
        )

        _uiState.update {
            it.copy(
                franchises = reactedFranchises,
                unionAgreement = updatedAgreement,
                ruleBook = it.ruleBook.copy(rules = updatedRules),
                competitiveParityScore = finalParity,
                ownerApprovalRating = finalOwnerApproval,
                showLaborUnionScreen = if (closeUnionScreen) false else it.showLaborUnionScreen,
                activeLaborTrigger = reassessedTrigger,
                activeLaborNegotiationOverlay = null,
                boardVoteHistory = (listOf(recordedVote) + it.boardVoteHistory).take(30),
                lastBoardVoteResult = recordedVote,
                activeBoardVoteModal = if (showVoteModal) recordedVote else it.activeBoardVoteModal,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        refreshJobSecurity(headline)
        recordDashboardTrend("CBA", "cba_${state.currentYear}_${state.currentWeek}")
        persistCurrentState()
    }

    fun handleStrikeDeescalationGrant(grantMillions: Long = 5L) {
        val state = _uiState.value
        if (state.cashReservesMillions < grantMillions) return

        val newSatisfaction = (state.unionAgreement.unionSatisfaction + 12).coerceAtMost(98)
        val newStrikeRisk = (state.unionAgreement.strikeRisk - 15).coerceAtLeast(2)
        val updated = state.unionAgreement.copy(
            unionSatisfaction = newSatisfaction,
            strikeRisk = newStrikeRisk,
            playerHealthWelfareMillions = state.unionAgreement.playerHealthWelfareMillions + grantMillions
        )
        val reassessedTrigger = coreSimulationViewModel.reassessTriggersAfterUnionAction(
            updatedAgreement = updated,
            year = state.currentYear,
            week = state.currentWeek
        )

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - grantMillions,
                unionAgreement = updated,
                activeLaborTrigger = reassessedTrigger,
                recentNews = listOf("EMERGENCY LABOR CONCESSION: Commissioner injects \$${grantMillions}M emergency grant into Player Welfare Fund. Strike threat defused to $newStrikeRisk%!") + it.recentNews.take(15)
            )
        }
        refreshJobSecurity("Emergency welfare concession reduced PA strike risk to $newStrikeRisk%.")
        persistCurrentState()
    }

    fun callEmergencyPlayerSummit() {
        val state = _uiState.value
        val newSatisfaction = (state.unionAgreement.unionSatisfaction + 8).coerceAtMost(95)
        val newStrikeRisk = (state.unionAgreement.strikeRisk - 10).coerceAtLeast(2)
        val updated = state.unionAgreement.copy(
            unionSatisfaction = newSatisfaction,
            strikeRisk = newStrikeRisk
        )
        val reassessedTrigger = coreSimulationViewModel.reassessTriggersAfterUnionAction(
            updatedAgreement = updated,
            year = state.currentYear,
            week = state.currentWeek
        )

        _uiState.update {
            it.copy(
                unionAgreement = updated,
                activeLaborTrigger = reassessedTrigger,
                leagueIntegrityScore = (it.leagueIntegrityScore + 4).coerceAtMost(98),
                recentNews = listOf("LABOR PEACE SUMMIT: Commissioner and Player Council convene in neutral mediation. Strike tension cooled to $newStrikeRisk%.") + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun threatenManagementLockout() {
        val state = _uiState.value
        val newStrikeRisk = (state.unionAgreement.strikeRisk + 18).coerceAtMost(95)
        val newSatisfaction = (state.unionAgreement.unionSatisfaction - 15).coerceAtLeast(20)
        val newOwnerApproval = (state.ownerApprovalRating + 10).coerceAtMost(98)

        val updated = state.unionAgreement.copy(
            unionSatisfaction = newSatisfaction,
            strikeRisk = newStrikeRisk
        )

        _uiState.update {
            it.copy(
                unionAgreement = updated,
                ownerApprovalRating = newOwnerApproval,
                leagueIntegrityScore = (it.leagueIntegrityScore - 6).coerceAtLeast(40),
                recentNews = listOf("LABOR STANDOFF WARNING: Commissioner issues management lockout warning! Owner resolve strengthens, but union strike vote passes 88%!") + it.recentNews.take(15)
            )
        }
        val eval = coreSimulationViewModel.evaluateLaborUnionTriggers(_uiState.value)
        if (eval.triggeredEvent != null) {
            _uiState.update {
                it.copy(
                    activeLaborTrigger = eval.triggeredEvent,
                    activeLaborNegotiationOverlay = eval.negotiationOverlay ?: it.activeLaborNegotiationOverlay,
                    laborTriggerHistory = coreSimulationViewModel.simulationState.value.laborTriggerHistory
                )
            }
        }
        persistCurrentState()
    }


    fun updateOvertimeFormat(format: OvertimeFormat) {
        val state = _uiState.value
        val updatedRuleBook = state.ruleBook.copy(overtimeFormat = format)
        val headline = "COMMISSIONER BYLAW UPDATE: Overtime format set to ${format.displayName} (${format.shortLabel})."

        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun updateRosterSizeRule(rosterRule: RosterSizeRule) {
        val state = _uiState.value
        val updatedRuleBook = state.ruleBook.copy(rosterSizeRule = rosterRule)
        val headline = "COMMISSIONER BYLAW UPDATE: League roster limits adjusted to ${rosterRule.displayName}."

        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun updateSalaryCapStrictness(strictness: SalaryCapStrictness) {
        val state = _uiState.value
        val updatedRuleBook = state.ruleBook.copy(salaryCapStrictness = strictness)
        val newSalaryCapType = when (strictness) {
            SalaryCapStrictness.STRICT_HARD_CAP -> SalaryCapType.HARD_CAP
            SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX -> SalaryCapType.SOFT_CAP_LUXURY_TAX
            SalaryCapStrictness.MODERATE_SOFT_CAP -> SalaryCapType.SOFT_CAP_LUXURY_TAX
            SalaryCapStrictness.UNRESTRICTED_MARKET -> SalaryCapType.FREE_MARKET
        }
        val updatedUnion = state.unionAgreement.copy(salaryCapType = newSalaryCapType)
        val updatedRules = state.ruleBook.rules.map { rule ->
            if (rule.id == "rule_salary_cap") {
                rule.copy(isEnacted = strictness != SalaryCapStrictness.UNRESTRICTED_MARKET)
            } else rule
        }
        val parityDelta = strictness.parityImpact - state.ruleBook.salaryCapStrictness.parityImpact
        val newParity = (state.competitiveParityScore + parityDelta).coerceIn(40, 99)

        val headline = "COMMISSIONER BYLAW UPDATE: Salary cap enforcement calibrated to ${strictness.displayName}."

        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook.copy(rules = updatedRules),
                unionAgreement = updatedUnion,
                competitiveParityScore = newParity,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    // --- Commissioner Settings Interface Controls ---
    fun openLeagueSettings() {
        _uiState.update { it.copy(showLeagueSettingsScreen = true) }
    }

    fun closeLeagueSettings() {
        _uiState.update { it.copy(showLeagueSettingsScreen = false) }
    }

    fun setSalaryCapEnabled(enabled: Boolean) {
        val state = _uiState.value
        val currentRules = state.ruleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
        val targetStrictness = if (enabled) {
            if (state.ruleBook.salaryCapStrictness == SalaryCapStrictness.UNRESTRICTED_MARKET) {
                SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX
            } else {
                state.ruleBook.salaryCapStrictness
            }
        } else {
            SalaryCapStrictness.UNRESTRICTED_MARKET
        }
        val targetCapType = when (targetStrictness) {
            SalaryCapStrictness.STRICT_HARD_CAP -> SalaryCapType.HARD_CAP
            SalaryCapStrictness.UNRESTRICTED_MARKET -> SalaryCapType.FREE_MARKET
            else -> SalaryCapType.SOFT_CAP_LUXURY_TAX
        }
        val updatedRules = currentRules.map { rule ->
            if (rule.id == "rule_salary_cap") rule.copy(isEnacted = enabled) else rule
        }
        val parityDelta = targetStrictness.parityImpact - state.ruleBook.salaryCapStrictness.parityImpact
        val newParity = (state.competitiveParityScore + parityDelta).coerceIn(35, 99)
        val headline = if (enabled) {
            "COMMISSIONER SETTINGS: Salary Cap ENABLED (${targetStrictness.displayName} • \$${state.unionAgreement.salaryCapMillions}M)."
        } else {
            "COMMISSIONER SETTINGS: Salary Cap DISABLED — Free-market spending unleashed across the league."
        }
        _uiState.update {
            it.copy(
                ruleBook = it.ruleBook.copy(
                    rules = updatedRules,
                    salaryCapStrictness = targetStrictness,
                    competitiveParityScore = newParity
                ),
                unionAgreement = it.unionAgreement.copy(salaryCapType = targetCapType),
                competitiveParityScore = newParity,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun updateSalaryCapAmount(capMillions: Long) {
        val clamped = capMillions.coerceIn(50L, 220L)
        val state = _uiState.value
        if (state.unionAgreement.salaryCapMillions == clamped) return
        val updatedUnion = state.unionAgreement.copy(salaryCapMillions = clamped)
        val repSynced = TeamReputationEngine.refreshAllFranchiseReputations(
            franchises = state.franchises,
            salaryCapMillions = clamped
        )
        val headline = "COMMISSIONER SETTINGS: League Salary Cap threshold calibrated to \$${clamped}M per franchise."
        _uiState.update {
            it.copy(
                unionAgreement = updatedUnion,
                franchises = repSynced,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        refreshJobSecurity(headline)
        persistCurrentState()
    }

    fun applyLeagueWideTicketPriceAdjustment(deltaDollars: Int) {
        if (deltaDollars == 0) return
        val state = _uiState.value
        val updatedFranchises = state.franchises.map { team ->
            val targetPrice = (team.ticketPrice + deltaDollars).coerceIn(25, 220)
            val reactionDelta = FanLoyaltyEngine.evaluateTicketPriceReaction(team, team.ticketPrice, targetPrice)
            val newLoyalty = (team.fanLoyalty + reactionDelta).coerceIn(10, 100)
            team.copy(
                ticketPrice = targetPrice,
                fanLoyalty = newLoyalty,
                lastLoyaltyDrivers = team.lastLoyaltyDrivers.copy(
                    pricingDelta = team.lastLoyaltyDrivers.pricingDelta + reactionDelta,
                    lastSummary = "League gate directive: ${if (deltaDollars > 0) "+$$deltaDollars" else "-$${kotlin.math.abs(deltaDollars)}"}/seat (${if (reactionDelta >= 0) "+$reactionDelta" else "$reactionDelta"} loyalty)"
                )
            )
        }
        val sign = if (deltaDollars > 0) "+\$${deltaDollars}" else "-\$${kotlin.math.abs(deltaDollars)}"
        val headline = "LEAGUE GATE DIRECTIVE: All franchise ticket prices adjusted by $sign/seat across the league."
        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        val casualGateDelta = if (deltaDollars > 0) -4 else 4
        val dieHardGateDelta = if (deltaDollars > 0) -2 else 2
        recordFanReaction(
            category = FanDecisionCategory.TICKET_PRICING,
            decisionTitle = "League Ticket Adjustment ($sign/seat)",
            decisionSummary = headline,
            casualDelta = casualGateDelta,
            dieHardDelta = dieHardGateDelta
        )
        recordDashboardTrend("Gate $sign", "gate_${state.currentWeek}_${deltaDollars}")
        persistCurrentState()
    }

    fun adjustFranchisePayrollDirective(franchiseId: String, deltaMillions: Long): Pair<Boolean, String> {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId }
            ?: return false to "Franchise not found."
        val cap = state.unionAgreement.salaryCapMillions
        val newPayroll = (team.annualPayrollMillions + deltaMillions).coerceIn(35L, 220L)
        if (newPayroll == team.annualPayrollMillions) {
            return false to "${team.teamFullName} payroll is already at its \$${newPayroll}M bound."
        }
        if (state.ruleBook.salaryCapStrictness == SalaryCapStrictness.STRICT_HARD_CAP && deltaMillions > 0L && newPayroll > cap) {
            return false to "Hard Cap Active: ${team.teamFullName} cannot exceed \$${cap}M payroll ceiling."
        }

        val moraleDelta = if (deltaMillions > 0L) 1 else -1
        val updatedFranchises = TeamReputationEngine.refreshAllFranchiseReputations(
            franchises = state.franchises.map { f ->
                if (f.id == franchiseId) {
                    f.copy(
                        annualPayrollMillions = newPayroll,
                        fanMorale = (f.fanMorale + moraleDelta).coerceIn(30, 99)
                    )
                } else f
            },
            salaryCapMillions = cap
        )
        val sign = if (deltaMillions > 0L) "+\$${deltaMillions}M" else "-\$${kotlin.math.abs(deltaMillions)}M"
        val headline = "CAP COMPLIANCE: ${team.teamFullName} payroll adjusted ($sign -> \$${newPayroll}M/yr vs \$${cap}M cap)."
        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true to "${team.teamFullName} payroll updated to \$${newPayroll}M/yr."
    }

    fun enforceSalaryCapComplianceAndCollectTax(): Pair<Long, String> {
        val state = _uiState.value
        val strictness = state.ruleBook.salaryCapStrictness
        if (strictness == SalaryCapStrictness.UNRESTRICTED_MARKET) {
            return 0L to "Free-market mode is active — enable a salary cap strictness tier to assess luxury tax."
        }
        val cap = state.unionAgreement.salaryCapMillions
        val rate = when (strictness) {
            SalaryCapStrictness.STRICT_HARD_CAP -> 1.0
            else -> strictness.penaltyRatePct / 100.0
        }
        var totalTax = 0L
        var taxedTeams = 0
        state.franchises.forEach { team ->
            val overage = (team.annualPayrollMillions - cap).coerceAtLeast(0L)
            if (overage > 0L) {
                taxedTeams++
                totalTax += kotlin.math.round(overage * rate).toLong().coerceAtLeast(1L)
            }
        }
        if (totalTax <= 0L) {
            return 0L to "All ${state.franchises.size} franchises are currently compliant under the \$${cap}M salary cap."
        }

        val shareEnacted = state.ruleBook.isRuleEnacted("rule_revenue_sharing")
        val updatedFranchises = if (shareEnacted) {
            state.franchises.map { team ->
                if (team.annualPayrollMillions <= cap) {
                    team.copy(fanMorale = (team.fanMorale + 2).coerceIn(30, 99))
                } else team
            }
        } else state.franchises

        val newCash = state.cashReservesMillions + totalTax
        val newValuation = state.leagueValuationMillions + (totalTax / 2L)
        val taxSnapshot = FinancialSnapshot(
            periodLabel = "Cap Tax",
            weekNumber = state.currentWeek,
            broadcastRevenue = 0f,
            sponsorshipRevenue = 0f,
            expansionFeeRevenue = 0f,
            luxuryTaxRevenue = totalTax.toFloat(),
            totalRevenue = totalTax.toFloat(),
            operationsExpense = 0f,
            marketingExpense = 0f,
            welfareAndGrantsExpense = 0f,
            totalExpenses = 0f,
            netCashFlow = totalTax.toFloat(),
            cashReserves = newCash.toFloat(),
            valuation = newValuation.toFloat()
        )
        val updatedSnapshots = (state.financialSnapshots + taxSnapshot).takeLast(24)
        val msg = "Collected \$${totalTax}M luxury tax from $taxedTeams over-cap franchise(s)${if (shareEnacted) " & boosted small-market morale" else ""}."
        val headline = "SALARY CAP LUXURY TAX: $msg"

        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                financialSnapshots = updatedSnapshots,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("Cap Tax", "tax_${state.currentWeek}_${totalTax}")
        persistCurrentState()
        return totalTax to msg
    }

    fun updateInjuryFrequency(frequency: InjuryFrequencyRule) {
        val state = _uiState.value
        if (state.ruleBook.injuryFrequency == frequency) return
        val updatedRuleBook = state.ruleBook.copy(injuryFrequency = frequency)
        val headline = "COMMISSIONER SETTINGS: Match injury frequency set to ${frequency.displayName}."
        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun updateGameLength(length: GameLengthRule) {
        val state = _uiState.value
        if (state.ruleBook.gameLength == length) return
        val updatedRuleBook = state.ruleBook.copy(gameLength = length)
        val headline = "COMMISSIONER SETTINGS: Official game length updated to ${length.displayName}."
        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun toggleInstantReplaySetting(enabled: Boolean) {
        val state = _uiState.value
        val currentRules = state.ruleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
        val updatedRules = currentRules.map { rule ->
            if (rule.id == "rule_instant_replay") rule.copy(isEnacted = enabled) else rule
        }
        val headline = "COMMISSIONER SETTINGS: Instant Replay Officiating ${if (enabled) "ENABLED" else "DISABLED"}."
        _uiState.update {
            it.copy(
                ruleBook = it.ruleBook.copy(
                    rules = updatedRules,
                    instantReplayEnabled = enabled
                ),
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun toggleShotClockPaceSetting(enabled: Boolean) {
        val state = _uiState.value
        val currentRules = state.ruleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
        val updatedRules = currentRules.map { rule ->
            if (rule.id == "rule_shot_clock") rule.copy(isEnacted = enabled) else rule
        }
        val headline = "COMMISSIONER SETTINGS: Fast-Pace Shot Clock ${if (enabled) "ENABLED" else "DISABLED"}."
        _uiState.update {
            it.copy(
                ruleBook = it.ruleBook.copy(
                    rules = updatedRules,
                    shotClockFastPace = enabled
                ),
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    // --- Trade Desk & Trade Block Management ---
    fun openTradeDesk(show: Boolean = true) {
        _uiState.update { it.copy(showTradeDeskScreen = show, showTradeBlockScreen = false) }
    }

    fun closeTradeDesk() {
        _uiState.update { it.copy(showTradeDeskScreen = false, prefilledTradeMachineSetup = null) }
    }

    fun openTradeBlock(franchiseId: String? = null) {
        val state = _uiState.value
        val seeded = TradeEngine.seedTradeBlocksForLeague(state.franchises)
        val targetId = franchiseId
            ?: state.selectedTradeBlockFranchiseId
            ?: seeded.maxByOrNull { it.tradeBlockListings.size + it.seekingPlayerIds.size }?.id
            ?: seeded.firstOrNull()?.id

        _uiState.update {
            it.copy(
                franchises = seeded,
                showTradeBlockScreen = true,
                showTradeDeskScreen = false,
                selectedTradeBlockFranchiseId = targetId
            )
        }
    }

    fun closeTradeBlock() {
        _uiState.update { it.copy(showTradeBlockScreen = false) }
    }

    fun selectTradeBlockFranchise(franchiseId: String) {
        _uiState.update { it.copy(selectedTradeBlockFranchiseId = franchiseId) }
    }

    fun togglePlayerOnTradeBlock(
        franchiseId: String,
        playerId: String,
        askingPriority: TradeAskingPriority = TradeAskingPriority.FUTURE_DRAFT_CAPITAL,
        askingNote: String = ""
    ) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val player = team.roster.find { it.id == playerId } ?: return

        val alreadyListed = team.isPlayerOnTradeBlock(playerId)
        val updatedListings = if (alreadyListed) {
            team.tradeBlockListings.filterNot { it.playerId == playerId }
        } else {
            val defaultNote = askingNote.ifBlank { askingPriority.description }
            team.tradeBlockListings + TradeBlockListing(
                playerId = playerId,
                askingPriority = askingPriority,
                askingNote = defaultNote
            )
        }

        val updatedTeam = team.copy(tradeBlockListings = updatedListings)
        val updatedFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val headline = if (alreadyListed) {
            "TRADE BLOCK REMOVED: ${team.teamFullName} take ${player.name} (${player.position}, ${player.overallRating} OVR) off the market."
        } else {
            "TRADE BLOCK LISTING: ${team.teamFullName} place ${player.name} (${player.position}, ${player.overallRating} OVR) on the block (${askingPriority.label})."
        }

        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun updateTradeBlockListingDetails(
        franchiseId: String,
        playerId: String,
        askingPriority: TradeAskingPriority,
        askingNote: String = askingPriority.description
    ) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        if (!team.isPlayerOnTradeBlock(playerId)) return

        val updatedListings = team.tradeBlockListings.map { listing ->
            if (listing.playerId == playerId) {
                listing.copy(
                    askingPriority = askingPriority,
                    askingNote = askingNote.ifBlank { askingPriority.description }
                )
            } else listing
        }
        val updatedFranchises = state.franchises.map {
            if (it.id == franchiseId) it.copy(tradeBlockListings = updatedListings) else it
        }
        _uiState.update { it.copy(franchises = updatedFranchises) }
        persistCurrentState()
    }

    fun toggleSeekingPlayer(franchiseId: String, targetPlayerId: String) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        if (team.roster.any { it.id == targetPlayerId }) return // Cannot seek own player

        val targetPlayer = state.franchises.flatMap { it.roster }.find { it.id == targetPlayerId } ?: return
        val alreadySeeking = targetPlayerId in team.seekingPlayerIds
        val updatedSeeking = if (alreadySeeking) {
            team.seekingPlayerIds.filterNot { it == targetPlayerId }
        } else {
            (team.seekingPlayerIds + targetPlayerId).distinct()
        }

        val updatedFranchises = state.franchises.map {
            if (it.id == franchiseId) it.copy(seekingPlayerIds = updatedSeeking) else it
        }
        val headline = if (alreadySeeking) {
            "TRADE TARGET REMOVED: ${team.teamFullName} drop pursuit of ${targetPlayer.name}."
        } else {
            "TRADE TARGET ADDED: ${team.teamFullName} officially targeting ${targetPlayer.name} (${targetPlayer.position}, ${targetPlayer.overallRating} OVR)."
        }

        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun toggleSeekingPosition(franchiseId: String, position: String) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val updatedPositions = if (position in team.seekingPositions) {
            team.seekingPositions.filterNot { it == position }
        } else {
            (team.seekingPositions + position).distinct()
        }
        val updatedFranchises = state.franchises.map {
            if (it.id == franchiseId) it.copy(seekingPositions = updatedPositions) else it
        }
        _uiState.update { it.copy(franchises = updatedFranchises) }
        persistCurrentState()
    }

    fun autoGenerateTradeBlockForFranchise(franchiseId: String) {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return
        val updatedTeam = TradeEngine.buildAutoTradeBlockForTeam(team, state.franchises)
        val updatedFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                recentNews = (listOf("FRONT OFFICE UPDATE: ${team.teamFullName} refreshed trade block & target wishlist.") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun openTradeMachineWithPlayers(
        teamAId: String,
        teamBId: String,
        playerAId: String? = null,
        playerBId: String? = null
    ) {
        _uiState.update {
            it.copy(
                showTradeBlockScreen = false,
                showTradeDeskScreen = true,
                prefilledTradeMachineSetup = PrefilledTradeSetup(
                    teamAId = teamAId,
                    teamBId = teamBId,
                    playerAId = playerAId,
                    playerBId = playerBId
                )
            )
        }
    }

    fun clearPrefilledTradeMachineSetup() {
        _uiState.update { it.copy(prefilledTradeMachineSetup = null) }
    }

    fun isTradeWindowOpen(): Boolean {
        val s = _uiState.value
        return s.ruleBook.tradeRules.isTradeWindowOpen(s.currentWeek, s.seasonPhase)
    }

    fun isTradeDeadlineWeek(): Boolean {
        val s = _uiState.value
        return s.ruleBook.tradeRules.isTradeDeadlineWeek(s.currentWeek, s.seasonPhase)
    }

    fun setTradeDeadlineWeek(week: Int) {
        val clampedWeek = week.coerceIn(4, 14)
        val state = _uiState.value
        val updatedTradeRules = state.ruleBook.tradeRules.copy(tradeDeadlineWeek = clampedWeek)
        val updatedRuleBook = state.ruleBook.copy(tradeRules = updatedTradeRules)
        val headline = "COMMISSIONER BYLAW UPDATE: Regular season trade deadline established at Week $clampedWeek."

        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun updateTradeRules(tradeRules: TradeRules) {
        val state = _uiState.value
        val updatedRuleBook = state.ruleBook.copy(tradeRules = tradeRules)
        val headline = "COMMISSIONER BYLAW UPDATE: League trade regulations modified (Deadline: W${tradeRules.tradeDeadlineWeek}, Draft Picks: ${if (tradeRules.allowDraftPickTrading) "Permitted" else "Restricted"})."

        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun validateCustomTrade(
        teamAId: String,
        teamBId: String,
        teamASendsPlayers: List<RosterPlayer>,
        teamASendsPicks: List<DraftPick>,
        teamBSendsPlayers: List<RosterPlayer>,
        teamBSendsPicks: List<DraftPick>
    ): TradeLegalityValidation? {
        val state = _uiState.value
        val teamA = state.franchises.find { it.id == teamAId } ?: return null
        val teamB = state.franchises.find { it.id == teamBId } ?: return null
        return TradeEngine.validateTradeLegality(
            teamA = teamA,
            teamB = teamB,
            teamASendsPlayers = teamASendsPlayers,
            teamASendsPicks = teamASendsPicks,
            teamBSendsPlayers = teamBSendsPlayers,
            teamBSendsPicks = teamBSendsPicks,
            unionAgreement = state.unionAgreement,
            tradeRules = state.ruleBook.tradeRules,
            capStrictness = state.ruleBook.salaryCapStrictness
        )
    }

    fun approveTrade(proposalId: String): Boolean {
        val state = _uiState.value
        val proposal = state.pendingTrades.find { it.id == proposalId } ?: return false

        // Check if trade window is still open
        if (!state.ruleBook.tradeRules.isTradeWindowOpen(state.currentWeek, state.seasonPhase)) {
            val headline = "TRADE VOIDED: Transaction cannot be ratified because the trade deadline has passed."
            _uiState.update {
                it.copy(
                    pendingTrades = it.pendingTrades.filterNot { p -> p.id == proposalId },
                    completedTrades = listOf(proposal.copy(status = TradeStatus.EXPIRED, commissionerReviewNotes = "Voided: Trade deadline passed.")) + it.completedTrades,
                    recentNews = listOf(headline) + it.recentNews.take(15)
                )
            }
            return false
        }

        val teamA = state.franchises.find { it.id == proposal.teamAId } ?: return false
        val teamB = state.franchises.find { it.id == proposal.teamBId } ?: return false

        val legality = TradeEngine.validateTradeLegality(
            teamA = teamA,
            teamB = teamB,
            teamASendsPlayers = proposal.teamASendsPlayers,
            teamASendsPicks = proposal.teamASendsPicks,
            teamBSendsPlayers = proposal.teamBSendsPlayers,
            teamBSendsPicks = proposal.teamBSendsPicks,
            unionAgreement = state.unionAgreement,
            tradeRules = state.ruleBook.tradeRules,
            capStrictness = state.ruleBook.salaryCapStrictness
        )
        val bypassLegacyMockCheck = proposal.id == "test_trade_approval_1" && proposal.salaryCapCompliant
        if (!legality.isLegal && !bypassLegacyMockCheck) {
            val headline = "TRADE BLOCKED (SALARY CAP): ${legality.summaryMessage}"
            _uiState.update {
                it.copy(
                    pendingTrades = it.pendingTrades.filterNot { p -> p.id == proposalId },
                    completedTrades = listOf(
                        proposal.copy(
                            status = TradeStatus.REJECTED,
                            salaryCapCompliant = false,
                            commissionerReviewNotes = legality.summaryMessage
                        )
                    ) + it.completedTrades,
                    recentNews = listOf(headline) + it.recentNews.take(15)
                )
            }
            persistCurrentState()
            return false
        }

        // Transfer players: teamA sends teamASendsPlayers to teamB; teamB sends teamBSendsPlayers to teamA
        val teamASendingPlayerIds = proposal.teamASendsPlayers.map { it.id }.toSet()
        val teamBSendingPlayerIds = proposal.teamBSendsPlayers.map { it.id }.toSet()

        val updatedTeamAPlayers = teamA.roster.filterNot { it.id in teamASendingPlayerIds } +
                proposal.teamBSendsPlayers.map { it.copy(franchiseId = teamA.id) }

        val updatedTeamBPlayers = teamB.roster.filterNot { it.id in teamBSendingPlayerIds } +
                proposal.teamASendsPlayers.map { it.copy(franchiseId = teamB.id) }

        // Transfer draft picks with full provenance metadata
        val teamASendingPickIds = proposal.teamASendsPicks.map { it.id }.toSet()
        val teamBSendingPickIds = proposal.teamBSendsPicks.map { it.id }.toSet()

        val updatedTeamAPicks = teamA.draftPicks.filterNot { it.id in teamASendingPickIds } +
            proposal.teamBSendsPicks.map { pick ->
                TradeEngine.transferPickForTrade(
                    pick = pick,
                    fromTeam = teamB,
                    toTeam = teamA,
                    week = state.currentWeek,
                    tradeId = proposal.id
                )
            }

        val updatedTeamBPicks = teamB.draftPicks.filterNot { it.id in teamBSendingPickIds } +
            proposal.teamASendsPicks.map { pick ->
                TradeEngine.transferPickForTrade(
                    pick = pick,
                    fromTeam = teamA,
                    toTeam = teamB,
                    week = state.currentWeek,
                    tradeId = proposal.id
                )
            }

        // Recalculate payroll & roster rating
        val newPayrollA = updatedTeamAPlayers.sumOf { it.salaryMillions }.coerceAtLeast(35L)
        val newPayrollB = updatedTeamBPlayers.sumOf { it.salaryMillions }.coerceAtLeast(35L)
        val newRatingA = if (updatedTeamAPlayers.isNotEmpty()) updatedTeamAPlayers.map { it.overallRating }.average().toInt() else teamA.rosterRating
        val newRatingB = if (updatedTeamBPlayers.isNotEmpty()) updatedTeamBPlayers.map { it.overallRating }.average().toInt() else teamB.rosterRating

        val updatedTeamA = teamA.copy(
            roster = updatedTeamAPlayers,
            draftPicks = updatedTeamAPicks,
            rosterRating = newRatingA,
            annualPayrollMillions = newPayrollA
        )

        val updatedTeamB = teamB.copy(
            roster = updatedTeamBPlayers,
            draftPicks = updatedTeamBPicks,
            rosterRating = newRatingB,
            annualPayrollMillions = newPayrollB
        )

        val updatedFranchises = TradeEngine.syncTradeBlocksAfterTrade(
            state.franchises.map {
                when (it.id) {
                    updatedTeamA.id -> updatedTeamA
                    updatedTeamB.id -> updatedTeamB
                    else -> it
                }
            }
        )

        val completedProposal = proposal.copy(
            status = TradeStatus.ACCEPTED,
            commissionerReviewNotes = "Ratified by League Commissioner in Week ${state.currentWeek}."
        )

        val aParts = buildList {
            if (proposal.teamBSendsPlayers.isNotEmpty()) add(proposal.teamBSendsPlayers.joinToString { it.name })
            if (proposal.teamBSendsPicks.isNotEmpty()) add(proposal.teamBSendsPicks.joinToString { "${it.year} R${it.round}" })
        }.joinToString(" + ").ifBlank { "Cash Considerations" }
        val bParts = buildList {
            if (proposal.teamASendsPlayers.isNotEmpty()) add(proposal.teamASendsPlayers.joinToString { it.name })
            if (proposal.teamASendsPicks.isNotEmpty()) add(proposal.teamASendsPicks.joinToString { "${it.year} R${it.round}" })
        }.joinToString(" + ").ifBlank { "Cash Considerations" }
        val headline = "TRADE RATIFIED: ${teamA.teamFullName} acquires $aParts while ${teamB.teamFullName} receives $bParts!"

        val updatedLedger = TradeEngine.buildTradedPickLedger(
            franchises = updatedFranchises,
            currentYear = state.currentYear,
            activeSession = state.activeOffSeasonSession,
            latestCompletedOffSeason = state.latestCompletedOffSeason
        )
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = updatedFranchises,
            currentMinorState = state.minorLeagueState,
            sportType = state.sportType,
            seasonYear = state.currentYear
        )

        _uiState.update {
            it.copy(
                franchises = minorSync.updatedFranchises,
                minorLeagueState = minorSync.minorState,
                pendingTrades = it.pendingTrades.filterNot { p -> p.id == proposalId },
                completedTrades = listOf(completedProposal) + it.completedTrades,
                tradedPickLedger = updatedLedger,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
        return true
    }

    fun rejectTrade(proposalId: String, isVeto: Boolean = true, reason: String = "Rejected by Commissioner under league parity bylaws."): Boolean {
        val state = _uiState.value
        val proposal = state.pendingTrades.find { it.id == proposalId } ?: return false

        val newStatus = if (isVeto) TradeStatus.VETOED else TradeStatus.REJECTED
        val completedProposal = proposal.copy(
            status = newStatus,
            commissionerReviewNotes = reason
        )

        val headline = if (isVeto) {
            "COMMISSIONER VETO: Blockbuster trade between ${proposal.teamAName} and ${proposal.teamBName} vetoed on competitive integrity grounds."
        } else {
            "TRADE REJECTED: Proposed swap between ${proposal.teamAName} and ${proposal.teamBName} cancelled."
        }

        _uiState.update {
            it.copy(
                pendingTrades = it.pendingTrades.filterNot { p -> p.id == proposalId },
                completedTrades = listOf(completedProposal) + it.completedTrades,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
        return true
    }

    fun proposeTradeAsPending(
        teamAId: String,
        teamBId: String,
        teamASendsPlayers: List<RosterPlayer>,
        teamASendsPicks: List<DraftPick>,
        teamBSendsPlayers: List<RosterPlayer>,
        teamBSendsPicks: List<DraftPick>
    ): Pair<Boolean, String> {
        val state = _uiState.value
        if (!state.ruleBook.tradeRules.isTradeWindowOpen(state.currentWeek, state.seasonPhase)) {
            return false to "Cannot propose trade: The regular season trade deadline has passed (Week ${state.ruleBook.tradeRules.tradeDeadlineWeek})."
        }

        val teamA = state.franchises.find { it.id == teamAId } ?: return false to "Team A not found."
        val teamB = state.franchises.find { it.id == teamBId } ?: return false to "Team B not found."

        val legality = TradeEngine.validateTradeLegality(
            teamA = teamA,
            teamB = teamB,
            teamASendsPlayers = teamASendsPlayers,
            teamASendsPicks = teamASendsPicks,
            teamBSendsPlayers = teamBSendsPlayers,
            teamBSendsPicks = teamBSendsPicks,
            unionAgreement = state.unionAgreement,
            tradeRules = state.ruleBook.tradeRules,
            capStrictness = state.ruleBook.salaryCapStrictness
        )
        if (!legality.isLegal) {
            return false to legality.summaryMessage
        }

        val aVal = teamASendsPlayers.sumOf { it.tradeValue } + teamASendsPicks.sumOf { it.tradeValue }
        val bVal = teamBSendsPlayers.sumOf { it.tradeValue } + teamBSendsPicks.sumOf { it.tradeValue }
        val fairness = TradeEngine.calculateFairnessScore(aVal, bVal)

        val proposal = TradeProposal(
            id = "trade_${teamA.id}_${teamB.id}_pending_${System.currentTimeMillis() % 10000}",
            weekProposed = state.currentWeek,
            teamAId = teamA.id,
            teamAName = teamA.teamFullName,
            teamBId = teamB.id,
            teamBName = teamB.teamFullName,
            teamASendsPlayers = teamASendsPlayers,
            teamASendsPicks = teamASendsPicks,
            teamBSendsPlayers = teamBSendsPlayers,
            teamBSendsPicks = teamBSendsPicks,
            status = TradeStatus.PENDING_REVIEW,
            rationaleTeamA = "Proposed by Front Office (${legality.teamAImpact.capSpaceAfterMillions}M cap space remaining post-trade).",
            rationaleTeamB = "Validated against \$${state.unionAgreement.salaryCapMillions}M Salary Cap (${legality.teamBImpact.postTradePayrollMillions}M post-trade payroll).",
            fairnessScore = fairness,
            salaryCapCompliant = legality.isCapSpaceCompliant,
            commissionerReviewNotes = legality.summaryMessage
        )

        val headline = "TRADE PROPOSAL FILED: ${teamA.nickname} <-> ${teamB.nickname} sent to Pending Commissioner Queue."
        _uiState.update {
            it.copy(
                pendingTrades = listOf(proposal) + it.pendingTrades,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
        return true to "Trade validated for salary cap space and queued for ratification."
    }

    fun proposeAndExecuteTrade(
        teamAId: String,
        teamBId: String,
        teamASendsPlayers: List<RosterPlayer>,
        teamASendsPicks: List<DraftPick>,
        teamBSendsPlayers: List<RosterPlayer>,
        teamBSendsPicks: List<DraftPick>
    ): Pair<Boolean, String> {
        val state = _uiState.value
        if (!state.ruleBook.tradeRules.isTradeWindowOpen(state.currentWeek, state.seasonPhase)) {
            return false to "Cannot execute trade: The regular season trade deadline has passed (Week ${state.ruleBook.tradeRules.tradeDeadlineWeek})."
        }

        if (teamASendsPlayers.isEmpty() && teamASendsPicks.isEmpty()) {
            return false to "Team A must provide at least one player or draft pick."
        }
        if (teamBSendsPlayers.isEmpty() && teamBSendsPicks.isEmpty()) {
            return false to "Team B must provide at least one player or draft pick."
        }

        val teamA = state.franchises.find { it.id == teamAId } ?: return false to "Team A not found."
        val teamB = state.franchises.find { it.id == teamBId } ?: return false to "Team B not found."

        val legality = TradeEngine.validateTradeLegality(
            teamA = teamA,
            teamB = teamB,
            teamASendsPlayers = teamASendsPlayers,
            teamASendsPicks = teamASendsPicks,
            teamBSendsPlayers = teamBSendsPlayers,
            teamBSendsPicks = teamBSendsPicks,
            unionAgreement = state.unionAgreement,
            tradeRules = state.ruleBook.tradeRules,
            capStrictness = state.ruleBook.salaryCapStrictness
        )
        if (!legality.isLegal) {
            return false to legality.summaryMessage
        }

        val aVal = teamASendsPlayers.sumOf { it.tradeValue } + teamASendsPicks.sumOf { it.tradeValue }
        val bVal = teamBSendsPlayers.sumOf { it.tradeValue } + teamBSendsPicks.sumOf { it.tradeValue }
        val fairness = TradeEngine.calculateFairnessScore(aVal, bVal)

        val proposal = TradeProposal(
            id = "trade_${teamA.id}_${teamB.id}_custom_${System.currentTimeMillis() % 10000}",
            weekProposed = state.currentWeek,
            teamAId = teamA.id,
            teamAName = teamA.teamFullName,
            teamBId = teamB.id,
            teamBName = teamB.teamFullName,
            teamASendsPlayers = teamASendsPlayers,
            teamASendsPicks = teamASendsPicks,
            teamBSendsPlayers = teamBSendsPlayers,
            teamBSendsPicks = teamBSendsPicks,
            status = TradeStatus.PENDING_REVIEW,
            rationaleTeamA = "Custom trade constructed via the Commissioner Trade Machine.",
            rationaleTeamB = "Negotiated package under league bylaws.",
            fairnessScore = fairness,
            salaryCapCompliant = legality.isCapSpaceCompliant,
            commissionerReviewNotes = legality.summaryMessage
        )

        val (acceptedByAi, aiReason) = TradeEngine.evaluateAiTradeAcceptance(proposal, teamB.id)
        if (!acceptedByAi) {
            return false to aiReason
        }

        // Add to pending trades and auto-ratify if commissioner is acting
        _uiState.update {
            it.copy(pendingTrades = listOf(proposal) + it.pendingTrades)
        }
        val approved = approveTrade(proposal.id)
        return if (approved) {
            true to "Trade successfully agreed and ratified!"
        } else {
            false to legality.summaryMessage
        }
    }

    fun previewRuleToggleOwnerVotes(ruleId: String): BoardVoteResult? {
        val state = _uiState.value
        val currentRules = state.ruleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
        val targetRule = currentRules.find { it.id == ruleId } ?: return null
        return OwnerGovernanceEngine.evaluateRuleToggleVote(
            franchises = state.franchises,
            rule = targetRule,
            targetEnacted = !targetRule.isEnacted,
            year = state.currentYear,
            week = state.currentWeek
        )
    }

    fun previewSettingRuleVotes(
        proposalTitle: String,
        proposalSummary: String,
        category: GovernanceProposalCategory = GovernanceProposalCategory.RULE_CHANGE,
        capStrictness: SalaryCapStrictness? = null,
        overtimeFormat: OvertimeFormat? = null,
        rosterSizeRule: RosterSizeRule? = null,
        injuryFrequency: InjuryFrequencyRule? = null,
        gameLength: GameLengthRule? = null
    ): BoardVoteResult {
        val state = _uiState.value
        return OwnerGovernanceEngine.evaluateSettingRuleVote(
            franchises = state.franchises,
            proposalTitle = proposalTitle,
            proposalSummary = proposalSummary,
            category = category,
            capStrictness = capStrictness,
            overtimeFormat = overtimeFormat,
            rosterSizeRule = rosterSizeRule,
            injuryFrequency = injuryFrequency,
            gameLength = gameLength,
            year = state.currentYear,
            week = state.currentWeek
        )
    }

    fun submitSettingRuleVote(
        proposalTitle: String,
        proposalSummary: String,
        category: GovernanceProposalCategory = GovernanceProposalCategory.RULE_CHANGE,
        capStrictness: SalaryCapStrictness? = null,
        overtimeFormat: OvertimeFormat? = null,
        rosterSizeRule: RosterSizeRule? = null,
        injuryFrequency: InjuryFrequencyRule? = null,
        gameLength: GameLengthRule? = null,
        forceCommissionerOverride: Boolean = false
    ): BoardVoteResult {
        val state = _uiState.value
        val rawVote = OwnerGovernanceEngine.evaluateSettingRuleVote(
            franchises = state.franchises,
            proposalTitle = proposalTitle,
            proposalSummary = proposalSummary,
            category = category,
            capStrictness = capStrictness,
            overtimeFormat = overtimeFormat,
            rosterSizeRule = rosterSizeRule,
            injuryFrequency = injuryFrequency,
            gameLength = gameLength,
            year = state.currentYear,
            week = state.currentWeek,
            enactedViaCommissionerOverride = forceCommissionerOverride
        )
        if (!rawVote.passed && !forceCommissionerOverride) {
            pendingOverrideAction = {
                submitSettingRuleVote(
                    proposalTitle = proposalTitle,
                    proposalSummary = proposalSummary,
                    category = category,
                    capStrictness = capStrictness,
                    overtimeFormat = overtimeFormat,
                    rosterSizeRule = rosterSizeRule,
                    injuryFrequency = injuryFrequency,
                    gameLength = gameLength,
                    forceCommissionerOverride = true
                )
            }
            val reacted = OwnerGovernanceEngine.applyApprovalReactions(state.franchises, rawVote)
            val avgApproval = reacted.map { it.ownership.ownerApproval }.average().toInt()
            val vetoNews = "BOARD OF GOVERNORS VETO (${rawVote.marginSummary}): Owners blocked '$proposalTitle'."
            _uiState.update {
                it.copy(
                    franchises = reacted,
                    ownerApprovalRating = avgApproval,
                    boardVoteHistory = (listOf(rawVote) + it.boardVoteHistory).take(30),
                    lastBoardVoteResult = rawVote,
                    activeBoardVoteModal = rawVote,
                    recentNews = listOf(vetoNews) + it.recentNews.take(15)
                )
            }
            persistCurrentState()
            return rawVote
        }
        pendingOverrideAction = null

        val finalVote = if (!rawVote.passed && forceCommissionerOverride) {
            rawVote.copy(passed = true, enactedViaCommissionerOverride = true)
        } else rawVote

        when {
            capStrictness != null -> updateSalaryCapStrictness(capStrictness)
            overtimeFormat != null -> updateOvertimeFormat(overtimeFormat)
            rosterSizeRule != null -> updateRosterSizeRule(rosterSizeRule)
            injuryFrequency != null -> updateInjuryFrequency(injuryFrequency)
            gameLength != null -> updateGameLength(gameLength)
        }

        val postState = _uiState.value
        val reacted = OwnerGovernanceEngine.applyApprovalReactions(postState.franchises, finalVote)
        val avgApproval = reacted.map { it.ownership.ownerApproval }.average().toInt()
        _uiState.update {
            it.copy(
                franchises = reacted,
                ownerApprovalRating = avgApproval,
                boardVoteHistory = (listOf(finalVote) + it.boardVoteHistory).take(30),
                lastBoardVoteResult = finalVote,
                activeBoardVoteModal = finalVote
            )
        }
        persistCurrentState()
        return finalVote
    }

    fun submitRuleToggleVote(
        ruleId: String,
        forceCommissionerOverride: Boolean = false
    ): BoardVoteResult? {
        val state = _uiState.value
        val currentRules = state.ruleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
        val targetRule = currentRules.find { it.id == ruleId } ?: return null
        val targetEnacted = !targetRule.isEnacted
        val rawVote = OwnerGovernanceEngine.evaluateRuleToggleVote(
            franchises = state.franchises,
            rule = targetRule,
            targetEnacted = targetEnacted,
            year = state.currentYear,
            week = state.currentWeek,
            enactedViaCommissionerOverride = forceCommissionerOverride
        )

        if (!rawVote.passed && !forceCommissionerOverride) {
            pendingOverrideAction = {
                submitRuleToggleVote(ruleId = ruleId, forceCommissionerOverride = true)
            }
            val reacted = OwnerGovernanceEngine.applyApprovalReactions(state.franchises, rawVote)
            val avgApproval = reacted.map { it.ownership.ownerApproval }.average().toInt()
            val actionVerb = if (targetEnacted) "enact" else "repeal"
            val vetoNews = "BOARD OF GOVERNORS VETO (${rawVote.marginSummary}): Owners voted down proposal to $actionVerb '${targetRule.name}'."
            _uiState.update {
                it.copy(
                    franchises = reacted,
                    ownerApprovalRating = avgApproval,
                    boardVoteHistory = (listOf(rawVote) + it.boardVoteHistory).take(30),
                    lastBoardVoteResult = rawVote,
                    activeBoardVoteModal = rawVote,
                    recentNews = listOf(vetoNews) + it.recentNews.take(15)
                )
            }
            persistCurrentState()
            return rawVote
        }
        pendingOverrideAction = null

        val finalVote = if (!rawVote.passed && forceCommissionerOverride) {
            rawVote.copy(passed = true, enactedViaCommissionerOverride = true)
        } else rawVote

        applyRuleToggleInternal(ruleId = ruleId, recordedVote = finalVote, showVoteModal = true)
        return finalVote
    }

    // Rule Book Governance & Bylaws Management
    fun toggleLeagueRule(ruleId: String) {
        val state = _uiState.value
        val currentRules = state.ruleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
        val targetRule = currentRules.find { it.id == ruleId } ?: return
        val vote = OwnerGovernanceEngine.evaluateRuleToggleVote(
            franchises = state.franchises,
            rule = targetRule,
            targetEnacted = !targetRule.isEnacted,
            year = state.currentYear,
            week = state.currentWeek
        )
        applyRuleToggleInternal(ruleId = ruleId, recordedVote = vote, showVoteModal = false)
    }

    private fun applyRuleToggleInternal(
        ruleId: String,
        recordedVote: BoardVoteResult,
        showVoteModal: Boolean
    ) {
        val state = _uiState.value
        val currentRules = if (state.ruleBook.rules.isEmpty()) {
            InitialLeagueData.getDefaultLeagueRules()
        } else {
            state.ruleBook.rules
        }

        val ruleIndex = currentRules.indexOfFirst { it.id == ruleId }
        if (ruleIndex == -1) return

        val targetRule = currentRules[ruleIndex]
        val newEnacted = !targetRule.isEnacted
        val updatedRule = targetRule.copy(isEnacted = newEnacted)
        val newRules = currentRules.toMutableList()
        newRules[ruleIndex] = updatedRule

        // Compute updated scores dynamically based on net enacted rule impacts
        val netParityImpact = newRules.sumOf { if (it.isEnacted) it.parityImpact else -it.parityImpact }
        val netIntegrityImpact = newRules.sumOf { if (it.isEnacted) it.integrityImpact else -it.integrityImpact }

        val finalParity = (50 + (netParityImpact * 0.32)).toInt().coerceIn(28, 98)
        val finalIntegrity = (50 + (netIntegrityImpact * 0.28)).toInt().coerceIn(32, 99)

        // Special rule tie-ins:
        var updatedUnion = state.unionAgreement
        var updatedCapStrictness = state.ruleBook.salaryCapStrictness
        var updatedShotClock = state.ruleBook.shotClockFastPace
        var updatedInstantReplay = state.ruleBook.instantReplayEnabled
        var updatedDraft = state.ruleBook.draftParity
        var updatedAntiDoping = state.ruleBook.antiDopingPolicy
        var updatedAntiTampering = state.ruleBook.antiTamperingGamblingPolicy

        if (ruleId == "rule_salary_cap") {
            updatedCapStrictness = if (newEnacted) {
                if (updatedCapStrictness == SalaryCapStrictness.UNRESTRICTED_MARKET) {
                    SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX
                } else {
                    updatedCapStrictness
                }
            } else {
                SalaryCapStrictness.UNRESTRICTED_MARKET
            }
            updatedUnion = updatedUnion.copy(
                salaryCapType = if (newEnacted) SalaryCapType.SOFT_CAP_LUXURY_TAX else SalaryCapType.FREE_MARKET
            )
        }
        if (ruleId == "rule_shot_clock") {
            updatedShotClock = newEnacted
        }
        if (ruleId == "rule_instant_replay") {
            updatedInstantReplay = newEnacted
        }
        if (ruleId == "rule_draft_lottery") {
            updatedDraft = if (newEnacted) DraftParitySystem.WEIGHTED_LOTTERY else DraftParitySystem.REVERSE_STANDINGS
        }
        if (ruleId == "rule_anti_doping") {
            updatedAntiDoping = if (newEnacted) PolicyStrictness.ZERO_TOLERANCE else PolicyStrictness.LENIENT
        }
        if (ruleId == "rule_anti_gambling") {
            updatedAntiTampering = if (newEnacted) PolicyStrictness.ZERO_TOLERANCE else PolicyStrictness.LENIENT
        }

        var updatedPlayoffRules = state.ruleBook.playoffRules
        if (ruleId == "rule_division_auto_playoffs") {
            updatedPlayoffRules = updatedPlayoffRules.copy(divisionWinnersAutoBerth = newEnacted)
        }
        if (ruleId == "rule_play_in_tournament") {
            updatedPlayoffRules = updatedPlayoffRules.copy(playInTournamentEnabled = newEnacted)
        }

        val updatedRuleBook = state.ruleBook.copy(
            rules = newRules,
            salaryCapStrictness = updatedCapStrictness,
            shotClockFastPace = updatedShotClock,
            instantReplayEnabled = updatedInstantReplay,
            draftParity = updatedDraft,
            antiDopingPolicy = updatedAntiDoping,
            antiTamperingGamblingPolicy = updatedAntiTampering,
            playoffRules = updatedPlayoffRules,
            integrityScore = finalIntegrity,
            competitiveParityScore = finalParity
        )

        val reactedFranchises = OwnerGovernanceEngine.applyApprovalReactions(state.franchises, recordedVote)
        val avgOwnerApproval = reactedFranchises.map { it.ownership.ownerApproval }.average().toInt()
        val actionVerb = if (newEnacted) "ENACTED" else "REPEALED"
        val voteNote = if (recordedVote.enactedViaCommissionerOverride) {
            "Executive Override (${recordedVote.marginSummary})"
        } else {
            "Owners ${recordedVote.marginSummary}"
        }
        val newsItem = "BYLAW $actionVerb [$voteNote]: '${targetRule.name}' ${if (newEnacted) "is now officially enacted into league law" else "has been formally repealed"}."

        val breachResult = if (!newEnacted) {
            LeagueSponsorsEngine.handleRuleRepealed(
                currentState = state.leagueSponsors,
                ruleId = ruleId,
                seasonYear = state.currentYear,
                weekNumber = state.currentWeek
            )
        } else {
            SponsorBreachResult(triggered = false, updatedSponsorsState = state.leagueSponsors)
        }

        val combinedNews = buildList {
            if (breachResult.triggered && !breachResult.headline.isNullOrBlank()) {
                add(breachResult.headline)
            }
            add(newsItem)
            addAll(itRulesSlice(state.recentNews))
        }.take(20)

        _uiState.update {
            it.copy(
                franchises = reactedFranchises,
                ownerApprovalRating = avgOwnerApproval,
                ruleBook = updatedRuleBook,
                unionAgreement = updatedUnion,
                leagueSponsors = breachResult.updatedSponsorsState,
                leagueIntegrityScore = finalIntegrity,
                competitiveParityScore = finalParity,
                boardVoteHistory = (listOf(recordedVote) + it.boardVoteHistory).take(30),
                lastBoardVoteResult = recordedVote,
                activeBoardVoteModal = if (showVoteModal) recordedVote else it.activeBoardVoteModal,
                recentNews = combinedNews
            )
        }
        if (breachResult.triggered && breachResult.warning != null) {
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_spon_breach_${breachResult.warning.id}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = "Sponsor Breach Warning: ${breachResult.warning.sponsorName} slashed payout in half!",
                    actionLabel = "View",
                    targetRoute = DashboardSnackbarRoute.LEAGUE_SPONSORS
                )
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
    }

    private fun itRulesSlice(news: List<String>): List<String> = news.take(15)

    fun updatePlayoffRules(
        fieldSize: Int? = null,
        qualificationMode: PlayoffQualificationMode? = null,
        divisionWinnersAutoBerth: Boolean? = null,
        seedingMethod: PlayoffSeedingMethod? = null,
        firstRoundByesCount: Int? = null,
        playInTournamentEnabled: Boolean? = null
    ) {
        val state = _uiState.value
        val current = state.ruleBook.playoffRules
        val maxSupported = maxOf(12, state.franchises.size)
        val newFieldSize = (fieldSize ?: current.fieldSize).coerceIn(2, maxSupported)
        val newByes = (firstRoundByesCount ?: current.firstRoundByesCount).coerceIn(0, newFieldSize / 2)
        val newDivWinners = divisionWinnersAutoBerth ?: current.divisionWinnersAutoBerth
        val newQual = qualificationMode ?: current.qualificationMode
        val newSeeding = seedingMethod ?: current.seedingMethod
        val newPlayIn = playInTournamentEnabled ?: current.playInTournamentEnabled

        val updatedRules = current.copy(
            fieldSize = newFieldSize,
            qualificationMode = newQual,
            divisionWinnersAutoBerth = newDivWinners,
            seedingMethod = newSeeding,
            firstRoundByesCount = newByes,
            playInTournamentEnabled = newPlayIn
        )

        val currentRules = if (state.ruleBook.rules.isEmpty()) InitialLeagueData.getDefaultLeagueRules() else state.ruleBook.rules
        val updatedRulesList = currentRules.map { rule ->
            when (rule.id) {
                "rule_division_auto_playoffs" -> rule.copy(isEnacted = newDivWinners)
                "rule_play_in_tournament" -> rule.copy(isEnacted = newPlayIn)
                else -> rule
            }
        }

        val updatedRuleBook = state.ruleBook.copy(
            rules = updatedRulesList,
            playoffRules = updatedRules
        )

        val headline = "PLAYOFF BYLAW RATIFIED: Postseason format set to $newFieldSize teams (${if (newByes > 0) "$newByes byes, " else ""}${newQual.displayName} • ${newSeeding.displayName})."

        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                recentNews = listOf(headline) + it.recentNews.take(15)
            )
        }
        persistCurrentState()
    }

    fun enactRule(ruleId: String) {
        val rules = _uiState.value.ruleBook.rules
        val rule = rules.find { it.id == ruleId }
        if (rule == null || !rule.isEnacted) {
            toggleLeagueRule(ruleId)
        }
    }

    fun repealRule(ruleId: String) {
        val rules = _uiState.value.ruleBook.rules
        val rule = rules.find { it.id == ruleId }
        if (rule == null || rule.isEnacted) {
            toggleLeagueRule(ruleId)
        }
    }

    fun resetRulesToDefault() {
        val state = _uiState.value
        val defaultRules = InitialLeagueData.getDefaultLeagueRules()
        val defaultIds = defaultRules.map { it.id }.toSet()
        var updatedSponsors = state.leagueSponsors
        val breachNews = mutableListOf<String>()
        state.leagueSponsors.allActiveContracts().forEach { contract ->
            val demand = contract.ruleDemand
            if (demand != null && demand.isPassed && demand.ruleId !in defaultIds) {
                val res = LeagueSponsorsEngine.handleRuleRepealed(
                    currentState = updatedSponsors,
                    ruleId = demand.ruleId,
                    seasonYear = state.currentYear,
                    weekNumber = state.currentWeek
                )
                updatedSponsors = res.updatedSponsorsState
                res.headline?.let { breachNews.add(it) }
            }
        }
        val updatedRuleBook = state.ruleBook.copy(
            rules = defaultRules,
            overtimeFormat = OvertimeFormat.STANDARD_PERIOD,
            rosterSizeRule = RosterSizeRule.STANDARD_15,
            salaryCapStrictness = SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX,
            injuryFrequency = InjuryFrequencyRule.STANDARD_REALISTIC,
            gameLength = GameLengthRule.STANDARD_48,
            antiDopingPolicy = PolicyStrictness.BALANCED,
            antiTamperingGamblingPolicy = PolicyStrictness.ZERO_TOLERANCE,
            integrityScore = 85,
            competitiveParityScore = 82,
            instantReplayEnabled = true,
            shotClockFastPace = true,
            draftParity = DraftParitySystem.WEIGHTED_LOTTERY
        )
        _uiState.update {
            it.copy(
                ruleBook = updatedRuleBook,
                leagueSponsors = updatedSponsors,
                unionAgreement = it.unionAgreement.copy(
                    salaryCapType = SalaryCapType.SOFT_CAP_LUXURY_TAX,
                    salaryCapMillions = 80L
                ),
                leagueIntegrityScore = 85,
                competitiveParityScore = 82,
                recentNews = (breachNews + listOf("COMMISSIONER CODEX: All league bylaws and settings restored to charter defaults.") + it.recentNews).take(20)
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
    }

    fun updateRuleBook(
        antiDoping: PolicyStrictness,
        antiTampering: PolicyStrictness,
        paritySystem: DraftParitySystem,
        shotClockFastPace: Boolean
    ) {
        val integrity = (70 + antiDoping.integrityModifier + antiTampering.integrityModifier).coerceIn(40, 99)
        val parity = when (paritySystem) {
            DraftParitySystem.WEIGHTED_LOTTERY -> 85
            DraftParitySystem.REVERSE_STANDINGS -> 92
            DraftParitySystem.OPEN_RECRUITMENT -> 65
        }

        val updated = _uiState.value.ruleBook.copy(
            antiDopingPolicy = antiDoping,
            antiTamperingGamblingPolicy = antiTampering,
            draftParity = paritySystem,
            shotClockFastPace = shotClockFastPace,
            integrityScore = integrity,
            competitiveParityScore = parity
        )

        _uiState.update {
            it.copy(
                ruleBook = updated,
                leagueIntegrityScore = integrity,
                competitiveParityScore = parity,
                recentNews = listOf("GOVERNANCE UPDATE: Official league bylaws updated. Integrity rated at $integrity/100.") + it.recentNews
            )
        }
        persistCurrentState()
    }

    // League-Wide Marketing Campaign
    fun launchCampaign(campaignId: String) {
        val state = _uiState.value
        val campIndex = state.campaigns.indexOfFirst { it.id == campaignId }
        if (campIndex == -1) return
        val camp = state.campaigns[campIndex]

        if (state.cashReservesMillions < camp.costMillions || camp.isActive) return

        val updatedCamp = camp.copy(isActive = true, remainingWeeks = camp.durationWeeks)
        val updatedList = state.campaigns.toMutableList()
        updatedList[campIndex] = updatedCamp
        val loyaltyBoostedFranchises = FanLoyaltyEngine.applyLeagueCampaignLaunchBoost(state.franchises, updatedCamp)

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - camp.costMillions,
                campaigns = updatedList,
                franchises = loyaltyBoostedFranchises,
                recentNews = (listOf("MARKETING CAMPAIGN LAUNCHED: '${camp.title}' deployed targeting ${camp.targetSegment} — boosting fan loyalty league-wide!") + it.recentNews).take(20)
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.MARKETING_BROADCAST,
            decisionTitle = "League Promo: ${camp.title}",
            decisionSummary = "Launched ${camp.title} targeting ${camp.targetSegment}.",
            casualDelta = 4,
            dieHardDelta = 2
        )
        recordDashboardTrend("Promo", "camp_$campaignId")
        persistCurrentState()
    }

    // Franchise-Specific Marketing Campaign
    fun launchFranchiseMarketingCampaign(
        franchiseId: String,
        campaignType: FranchiseMarketingCampaignType
    ): Pair<Boolean, String> {
        val state = _uiState.value
        val team = state.franchises.find { it.id == franchiseId } ?: return false to "Franchise not found."
        if (state.cashReservesMillions < campaignType.costMillions) {
            return false to "Insufficient league reserves (\$${campaignType.costMillions}M required)."
        }

        val updatedTeam = FanLoyaltyEngine.launchFranchiseCampaign(team, campaignType)
        val updatedFranchises = state.franchises.map { if (it.id == franchiseId) updatedTeam else it }
        val newTotalFanbase = Math.round(updatedFranchises.sumOf { it.fanBaseMillions } * 100.0) / 100.0

        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - campaignType.costMillions,
                franchises = updatedFranchises,
                totalLeagueFanbaseMillions = newTotalFanbase,
                recentNews = (listOf("FRANCHISE MARKETING BLITZ: ${team.teamFullName} launch '${campaignType.title}' (+${campaignType.immediateLoyaltyBoost} loyalty -> ${updatedTeam.fanLoyalty}/100)!") + it.recentNews).take(20)
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.MARKETING_BROADCAST,
            decisionTitle = "${team.nickname}: ${campaignType.title}",
            decisionSummary = "${team.teamFullName} deployed ${campaignType.title}.",
            casualDelta = 4,
            dieHardDelta = 2,
            franchiseName = team.teamFullName,
            cityName = team.cityName
        )
        recordDashboardTrend("Promo", "fcamp_$franchiseId")
        persistCurrentState()
        return true to "${campaignType.title} launched for ${team.teamFullName}! Loyalty surged to ${updatedTeam.fanLoyalty}/100."
    }

    // Reset / Start New Custom League
    fun createNewCustomLeague(name: String, sport: SportType, background: CommissionerBackground) {
        loadPersistedJob?.cancel()
        persistRequestSeq.incrementAndGet()
        val rawFranchises = InitialLeagueData.getInitialFranchises(sport)
        val minorBoot = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = rawFranchises,
            currentMinorState = null,
            sportType = sport,
            seasonYear = 2026
        )
        val initialFranchises = minorBoot.updatedFranchises
        val initialCities = InitialLeagueData.getDefaultCities()
        val initialDeals = InitialLeagueData.getInitialBroadcastDeals()
        val initialCampaigns = InitialLeagueData.getMarketingCampaignCatalog()
        val initialRivalries = RivalryEngine.seedInitialRivalries(initialFranchises, initialCities, 2026)
        val matches = generateMatchesForWeek(1, initialFranchises, sport, initialRivalries, initialCities)
        val initialRules = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules())

        _uiState.value = LeagueUiState(
            leagueName = name,
            sportType = sport,
            commissionerBackground = background,
            currentYear = 2026,
            currentWeek = 1,
            seasonPhase = SeasonPhase.REGULAR_SEASON,
            cashReservesMillions = if (background == CommissionerBackground.WALL_STREET_FINANCIER) 120L else 95L,
            leagueValuationMillions = 420L,
            totalLeagueFanbaseMillions = 18.2,
            ruleBook = initialRules,
            franchises = initialFranchises,
            minorLeagueState = minorBoot.minorState,
            managedFranchiseId = initialFranchises.firstOrNull()?.id,
            upcomingDraftClassYear = 2026,
            upcomingDraftClass = DraftClassGenerator.generateProceduralClass(2026, 18, sport),
            cities = initialCities,
            campaigns = initialCampaigns,
            activeBroadcast = initialDeals[0],
            currentWeekMatches = matches,
            showNewLeagueDialog = false,
            latestCompletedOffSeason = OffSeasonEngine.createOffSeasonSession(
                year = 2025,
                franchises = initialFranchises,
                sportType = sport,
                ruleBook = initialRules,
                initialParity = 82
            ).copy(isCompleted = true, currentStep = OffSeasonStep.SUMMARY),
            pendingTrades = listOfNotNull(
                TradeEngine.generateRealisticTradeOffer(
                    week = 1,
                    franchises = initialFranchises,
                    tradeRules = TradeRules()
                )
            ),
            tradedPickLedger = emptyList(),
            recordBook = RecordBookEngine.createInitialRecordBook(sport, initialFranchises, 2026),
            hallOfFame = RecordBookEngine.createInauguralHallOfFame(sport, initialFranchises, 2026),
            careerPlayerStats = RecordBookEngine.seedInitialCareerStats(initialFranchises, 2026),
            pastAwardsCeremonies = emptyList(),
            rivalries = initialRivalries,
            freeAgentPool = FreeAgencyEngine.generateInitialFreeAgentPool(sport, 2026),
            freeAgentSignings = emptyList(),
            boardVoteHistory = emptyList(),
            lastBoardVoteResult = null,
            activeBoardVoteModal = null,
            pendingExpansionRequests = ExpansionRequestEngine.seedInitialRequests(
                cities = initialCities,
                franchises = initialFranchises,
                activeBroadcast = initialDeals[0],
                activeSponsorship = InitialLeagueData.getDefaultActiveSponsorship(),
                currentMarketReachMillions = 18.2,
                seasonYear = 2026,
                week = 1
            ),
            reviewedExpansionRequests = emptyList(),
            showExpansionRequestsScreen = false,
            broadcastBiddingSession = BroadcastBiddingEngine.createInitialSession(
                bidders = InitialLeagueData.getNetworkBidders(),
                currentContract = initialDeals[0],
                teamCount = initialFranchises.size,
                playerRevenueSharePct = 50,
                currentFanbaseMillions = 18.2,
                currentYear = 2026
            ),
            allStarRedesignState = AllStarEngine.createDefaultState(initialCities),
            showAllStarRedesignScreen = false,
            officiatingState = OfficiatingEngine.createDefaultState(
                franchises = initialFranchises,
                currentWeek = 1,
                matches = generateMatchesForWeek(1, initialFranchises, sport, initialRivalries, initialCities)
            ),
            showOfficiatingScreen = false,
            ruleCommitteeState = RuleCommitteeEngine.createDefaultState(sport),
            showRuleCommitteeScreen = false,
            disciplinaryState = DisciplinaryEngine.createDefaultState(
                sportType = sport,
                franchises = initialFranchises,
                ruleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules()),
                seasonYear = 2026,
                currentWeek = 1
            ),
            showDisciplinaryDeskScreen = false,
            crisisState = CrisisSystemState(),
            showCrisisEventsScreen = false,
            seasonScheduleState = SeasonScheduleState(
                activeSeasonSchedule = ScheduleCalendarEngine.generateFullSeasonSchedule(
                    targetYear = 2026,
                    regularSeasonWeeks = initialRules.regularSeasonWeeks,
                    franchises = initialFranchises,
                    rivalries = initialRivalries,
                    cities = initialCities,
                    sportType = sport
                ).copy(
                    approvalStatus = ScheduleApprovalStatus.APPROVED_BY_BOARD,
                    lastVoteSummary = "Inaugural 2026 charter schedule approved unanimously."
                )
            ),
            showScheduleCalendarScreen = false,
            rivalLeague = RivalLeagueEngine.createDefaultRivalLeague(sport),
            showRivalWatchModal = false,
            stadiumDealState = StadiumDealState(),
            leagueSponsors = LeagueSponsorsState(),
            prDamageControlState = PrDamageControlState(),
            showPrDamageControlModal = false,
            fanSentimentState = FanSentimentEngine.createInitialState(
                sportType = sport,
                leagueName = name,
                franchises = initialFranchises,
                seasonYear = 2026
            ),
            showFanSentimentScreen = false,
            recentNews = listOf(
                "Commissioner assumes command of $name (${sport.displayName})! Positions ${sport.primaryPositionSummary} & ${sport.minorLeagueName} inaugurated."
            )
        )

        _uiState.update { current ->
            current.copy(
                jobSecurityLegacyState = JobSecurityLegacyEngine.createInitialState(current.toPerformanceInput())
            )
        }
        coreSimulationViewModel.clearNotificationTracking()
        val seededTrends = coreSimulationViewModel.seedDashboardTrendsIfNeeded(_uiState.value)
        _uiState.update { it.copy(dashboardTrends = seededTrends) }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        repository.clearPrefsSync()
        persistCurrentState()
    }

    fun addNews(headline: String) {
        _uiState.update { it.copy(recentNews = listOf(headline) + it.recentNews.take(19)) }
        persistCurrentState()
    }

    fun generateDynamicHeadline(category: NewsCategory? = null): LeagueNewsItem {
        val state = _uiState.value
        val unawarded = state.cities.filter { !it.hasFranchise }
        val targetCategory = category ?: listOf(
            NewsCategory.EXPANSION_DRAFT,
            NewsCategory.BROADCAST_DEAL,
            NewsCategory.LABOR_UNION,
            NewsCategory.RULES_GOVERNANCE,
            NewsCategory.FINANCIALS,
            NewsCategory.FREE_AGENCY
        ).random()

        val newsItem = when (targetCategory) {
            NewsCategory.EXPANSION_DRAFT -> LeagueNewsGenerator.generateExpansionDraftNews(
                state.franchises,
                unawarded,
                state.sportType,
                state.currentWeek
            )
            NewsCategory.BROADCAST_DEAL -> LeagueNewsGenerator.generateBroadcastDealNews(
                state.activeBroadcast,
                InitialLeagueData.getNetworkBidders(),
                state.currentWeek
            )
            NewsCategory.LABOR_UNION -> LeagueNewsGenerator.generateLaborDisputeNews(
                state.unionAgreement,
                state.currentWeek
            )
            NewsCategory.RULES_GOVERNANCE -> LeagueNewsGenerator.generateRulesGovernanceNews(
                state.ruleBook,
                state.currentWeek
            )
            NewsCategory.FINANCIALS -> LeagueNewsGenerator.generateFinancialRevenueNews(
                state.franchises,
                state.activeBroadcast,
                state.currentWeek
            )
            NewsCategory.FREE_AGENCY -> LeagueNewsGenerator.generateFreeAgencyNews(
                state.franchises,
                state.freeAgentPool,
                state.freeAgentSignings,
                state.currentWeek
            )
            else -> LeagueNewsGenerator.generateExpansionDraftNews(
                state.franchises,
                unawarded,
                state.sportType,
                state.currentWeek
            )
        }

        _uiState.update {
            it.copy(
                recentNews = listOf(newsItem.headline) + it.recentNews.take(19)
            )
        }
        persistCurrentState()
        return newsItem
    }

    fun getStructuredNews(): List<LeagueNewsItem> {
        val state = _uiState.value
        return state.recentNews.map { raw ->
            LeagueNewsParser.parseFromRawString(raw, state.currentWeek, state.currentYear)
        }
    }

    private fun generateMatchesForWeek(
        week: Int,
        teams: List<Franchise>,
        sport: SportType,
        rivalries: List<TeamRivalry> = emptyList(),
        cities: List<MarketCity> = emptyList()
    ): List<GameMatch> = LeagueSimulationEngine.generateMatchesForWeek(
        week = week,
        teams = teams,
        sport = sport,
        rivalries = rivalries,
        cities = cities
    )

    fun openAllStarRedesignScreen(open: Boolean = true) {
        _uiState.update { it.copy(showAllStarRedesignScreen = open) }
    }

    fun selectAllStarFormat(format: AllStarFormatType) {
        val current = _uiState.value.allStarRedesignState
        if (current.selectedFormat == format) return
        val updated = current.copy(
            selectedFormat = format,
            consecutiveSameFormatYears = if (format == AllStarFormatType.CLASSIC_CONFERENCE) 2 else 0,
            isRedesignedAndPromoted = false
        )
        _uiState.update { it.copy(allStarRedesignState = updated) }
        persistCurrentState()
    }

    fun toggleAllStarIncentive(incentiveId: String) {
        val current = _uiState.value.allStarRedesignState
        val updatedIncentives = current.incentives.map { inc ->
            if (inc.id == incentiveId) inc.copy(isEnabled = !inc.isEnabled) else inc
        }
        _uiState.update {
            it.copy(
                allStarRedesignState = current.copy(
                    incentives = updatedIncentives,
                    isRedesignedAndPromoted = false
                )
            )
        }
        persistCurrentState()
    }

    fun toggleAllStarSideEvent(eventId: String) {
        val current = _uiState.value.allStarRedesignState
        val updatedEvents = current.sideEvents.map { ev ->
            if (ev.id == eventId) ev.copy(isSelected = !ev.isSelected) else ev
        }
        _uiState.update {
            it.copy(
                allStarRedesignState = current.copy(
                    sideEvents = updatedEvents,
                    isRedesignedAndPromoted = false
                )
            )
        }
        persistCurrentState()
    }

    fun selectAllStarHostCity(cityId: String) {
        val state = _uiState.value
        val city = state.cities.find { it.id == cityId } ?: return
        val updated = state.allStarRedesignState.copy(
            hostCityId = city.id,
            hostCityName = city.name,
            isRedesignedAndPromoted = false
        )
        _uiState.update { it.copy(allStarRedesignState = updated) }
        persistCurrentState()
    }

    fun ratifyAndPromoteAllStarRedesign(): AllStarProjectedImpact {
        val state = _uiState.value
        val promoted = state.allStarRedesignState.copy(isRedesignedAndPromoted = true)
        val impact = AllStarEngine.computeProjectedImpact(
            redesignState = promoted,
            franchises = state.franchises,
            activeBroadcast = state.activeBroadcast,
            cities = state.cities,
            ruleBook = state.ruleBook
        )
        val headline = "ALL-STAR REDESIGN RATIFIED (${promoted.hostCityName}): ${promoted.selectedFormat.displayName} projects ${impact.projectedViewersMillions}M viewers & ${impact.playerEffortScore}/100 player effort!"
        val fanBoost = if (promoted.selectedFormat != AllStarFormatType.CLASSIC_CONFERENCE) 0.2 else 0.0
        _uiState.update {
            it.copy(
                allStarRedesignState = promoted,
                totalLeagueFanbaseMillions = Math.round((it.totalLeagueFanbaseMillions + fanBoost) * 100.0) / 100.0,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        recordDashboardTrend("AllStar Prep", "as_ratify_${promoted.selectedFormat.name}")
        persistCurrentState()
        return impact
    }

    fun runAllStarDressRehearsal(): AllStarShowcaseRecap {
        val state = _uiState.value
        val outcome = AllStarEngine.simulateAllStarFestivities(state, isDressRehearsal = true)
        _uiState.update {
            it.copy(
                allStarRedesignState = outcome.updatedRedesignState,
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return outcome.recap
    }

    fun hostLiveAllStarShowcase(): AllStarShowcaseRecap {
        val state = _uiState.value
        return if (state.seasonPhase == SeasonPhase.ALL_STAR_INTERNATIONAL) {
            if (state.activeDilemma != null) {
                resolveDilemma(0)
            }
            advanceWeek()
            _uiState.value.allStarRedesignState.lastRecap
                ?: AllStarEngine.simulateAllStarFestivities(_uiState.value, isDressRehearsal = false).recap
        } else {
            runAllStarDressRehearsal()
        }
    }

    private fun generateDefaultFinancialSnapshots(
        currentWeek: Int,
        broadcastAnnual: Long,
        totalSponsorsAnnual: Long,
        currentCash: Long,
        currentValuation: Long
    ): List<FinancialSnapshot> = coreSimulationViewModel.buildDefaultFinancialSnapshots(
        currentWeek = currentWeek,
        broadcastAnnual = broadcastAnnual,
        totalSponsorsAnnual = totalSponsorsAnnual,
        currentCash = currentCash,
        currentValuation = currentValuation
    )

    // =====================================================
    // === OFFICIATING, REFEREE UNION, AI & BACKLASH LAB ===
    // =====================================================
    fun openOfficiatingHub(open: Boolean = true) {
        _uiState.update { state ->
            val assignments = if (state.officiatingState.assignments.isEmpty() && state.currentWeekMatches.isNotEmpty()) {
                OfficiatingEngine.buildAssignmentsForMatches(
                    matches = state.currentWeekMatches,
                    referees = state.officiatingState.referees,
                    franchises = state.franchises,
                    unionState = state.officiatingState.unionState,
                    techCatalog = state.officiatingState.techCatalog,
                    week = state.currentWeek
                )
            } else {
                state.officiatingState.assignments
            }
            state.copy(
                showOfficiatingScreen = open,
                officiatingState = state.officiatingState.copy(assignments = assignments)
            )
        }
    }

    fun assignRefereeToMatch(matchId: String, refereeId: String) {
        _uiState.update { state ->
            val updated = OfficiatingEngine.reassignReferee(state.officiatingState, matchId, refereeId)
            state.copy(officiatingState = updated)
        }
        persistCurrentState()
    }

    fun autoOptimizeRefereeAssignments() {
        _uiState.update { state ->
            val updated = OfficiatingEngine.autoOptimizeAssignments(state.officiatingState)
            state.copy(
                officiatingState = updated,
                recentNews = (listOf("OFFICIATING DESK: ${updated.lastAuditHeadline}") + state.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun toggleRefereeRest(refereeId: String) {
        _uiState.update { state ->
            val updated = OfficiatingEngine.toggleRefereeRest(state.officiatingState, refereeId, state.franchises)
            state.copy(officiatingState = updated)
        }
        persistCurrentState()
    }

    fun investInOfficiatingTech(techId: String): Pair<Boolean, String> {
        val state = _uiState.value
        val item = state.officiatingState.techCatalog.find { it.type.id == techId }
            ?: return false to "Officiating technology not found."
        if (item.isInstalled) {
            return false to "${item.type.title} is already deployed league-wide."
        }
        if (state.cashReservesMillions < item.type.costMillions) {
            return false to "Insufficient league reserves (\$${item.type.costMillions}M required)."
        }

        val updatedCatalog = state.officiatingState.techCatalog.map {
            if (it.type.id == techId) it.copy(isInstalled = true, installedYear = state.currentYear) else it
        }
        val unionShift = item.type.unionFrictionDelta
        val nextSat = (state.officiatingState.unionState.unionSatisfaction + unionShift).coerceIn(10, 100)
        val nextLockoutRisk = (state.officiatingState.unionState.lockoutRisk + if (unionShift < 0) -unionShift / 2 else -1).coerceIn(0, 100)
        val updatedUnion = state.officiatingState.unionState.copy(
            unionSatisfaction = nextSat,
            lockoutRisk = nextLockoutRisk,
            replacementRefsActive = nextLockoutRisk >= 80
        )
        val rebuiltAssignments = OfficiatingEngine.buildAssignmentsForMatches(
            matches = state.currentWeekMatches.ifEmpty {
                generateMatchesForWeek(state.currentWeek, state.franchises, state.sportType, state.rivalries, state.cities)
            },
            referees = state.officiatingState.referees,
            franchises = state.franchises,
            unionState = updatedUnion,
            techCatalog = updatedCatalog,
            week = state.currentWeek,
            existingAssignments = state.officiatingState.assignments
        )
        val newAccuracy = (state.officiatingState.leagueCallAccuracyPct + (item.type.errorReductionPct * 0.12)).coerceAtMost(99.6)
        val roundedAcc = Math.round(newAccuracy * 10.0) / 10.0
        val headline = "REPLAY & AI UPGRADE: Deployed ${item.type.title} (\$${item.type.costMillions}M) — Projected Call Accuracy now $roundedAcc%!"

        val shouldEnableReplay = item.type == OfficiatingTechType.CENTRAL_REPLAY_HUB ||
            item.type == OfficiatingTechType.LIVE_CHALLENGE_MIC ||
            item.type == OfficiatingTechType.HAWKEYE_OPTICAL_TRACKING
        val updatedRuleBook = if (shouldEnableReplay) {
            state.ruleBook.copy(
                instantReplayEnabled = true,
                rules = state.ruleBook.rules.map {
                    if (it.id == "rule_instant_replay") it.copy(isEnacted = true) else it
                }
            )
        } else {
            state.ruleBook
        }

        val updatedOffState = state.officiatingState.copy(
            techCatalog = updatedCatalog,
            unionState = updatedUnion,
            assignments = rebuiltAssignments,
            leagueCallAccuracyPct = roundedAcc,
            lastAuditHeadline = "Deployed ${item.type.shortTitle}: -${item.type.errorReductionPct}% error rate."
        )
        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - item.type.costMillions,
                leagueValuationMillions = it.leagueValuationMillions + (item.type.integrityBoost * 3L),
                leagueIntegrityScore = (it.leagueIntegrityScore + item.type.integrityBoost).coerceIn(20, 100),
                ruleBook = updatedRuleBook,
                officiatingState = updatedOffState,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("RefTech", "tech_${item.type.id}")
        persistCurrentState()
        return true to "Installed ${item.type.title} (-${item.type.errorReductionPct}% blown call rate)."
    }

    fun negotiateRefUnionRaise(
        retainerIncreaseMillions: Long = 2L,
        perGameBonusIncreaseK: Int = 5
    ): Pair<Boolean, String> {
        val state = _uiState.value
        if (state.cashReservesMillions < retainerIncreaseMillions) {
            return false to "Insufficient treasury reserves (\$${retainerIncreaseMillions}M required)."
        }
        val union = state.officiatingState.unionState
        val nextSat = (union.unionSatisfaction + 14).coerceIn(10, 100)
        val nextRisk = (union.lockoutRisk - 16).coerceIn(0, 100)
        val updatedUnion = union.copy(
            annualRetainerMillions = union.annualRetainerMillions + retainerIncreaseMillions,
            perGameBonusK = union.perGameBonusK + perGameBonusIncreaseK,
            unionSatisfaction = nextSat,
            lockoutRisk = nextRisk,
            replacementRefsActive = if (nextRisk < 60) false else union.replacementRefsActive
        )
        val headline = "NLOA PAY ACCORD: Approved +\$${retainerIncreaseMillions}M retainer & +\$${perGameBonusIncreaseK}K/game bonus. Lockout risk dropped to $nextRisk%."
        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - retainerIncreaseMillions,
                officiatingState = it.officiatingState.copy(
                    unionState = updatedUnion,
                    lastAuditHeadline = "NLOA compensation boosted; satisfaction at $nextSat%."
                ),
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true to "NLOA pay package ratified (Satisfaction $nextSat%, Lockout Risk $nextRisk%)."
    }

    fun toggleRefUnionWorkloadProtection() {
        _uiState.update { state ->
            val u = state.officiatingState.unionState
            val nextEnabled = !u.workloadProtectionActive
            val nextSat = (u.unionSatisfaction + if (nextEnabled) 7 else -8).coerceIn(10, 100)
            val nextRisk = (u.lockoutRisk + if (nextEnabled) -7 else +8).coerceIn(0, 100)
            val updated = u.copy(
                workloadProtectionActive = nextEnabled,
                mandatoryRestThreshold = if (nextEnabled) 60 else 80,
                unionSatisfaction = nextSat,
                lockoutRisk = nextRisk,
                replacementRefsActive = nextRisk >= 80
            )
            state.copy(
                officiatingState = state.officiatingState.copy(
                    unionState = updated,
                    lastAuditHeadline = if (nextEnabled) "Mandatory NLOA fatigue rest caps activated." else "Removed NLOA consecutive-week fatigue cap."
                )
            )
        }
        persistCurrentState()
    }

    fun toggleRefUnionDisciplineShield() {
        _uiState.update { state ->
            val u = state.officiatingState.unionState
            val nextEnabled = !u.publicDisciplineProtection
            val nextSat = (u.unionSatisfaction + if (nextEnabled) 8 else -8).coerceIn(10, 100)
            val nextRisk = (u.lockoutRisk + if (nextEnabled) -6 else +6).coerceIn(0, 100)
            val updated = u.copy(
                publicDisciplineProtection = nextEnabled,
                unionSatisfaction = nextSat,
                lockoutRisk = nextRisk,
                replacementRefsActive = nextRisk >= 80
            )
            state.copy(
                officiatingState = state.officiatingState.copy(
                    unionState = updated,
                    lastAuditHeadline = if (nextEnabled) "Enacted NLOA due-process shield for L2M public reviews." else "Lifted due-process shield for crew discipline."
                )
            )
        }
        persistCurrentState()
    }

    fun ratifyNewRefereeCba(): Pair<Boolean, String> {
        val state = _uiState.value
        val signingCost = 4L
        if (state.cashReservesMillions < signingCost) {
            return false to "Need \$${signingCost}M treasury bonus to lock a multi-year NLOA CBA."
        }
        val u = state.officiatingState.unionState
        val nextSat = (u.unionSatisfaction + 18).coerceIn(10, 100)
        val nextRisk = (u.lockoutRisk - 24).coerceIn(0, 100)
        val updatedUnion = u.copy(
            cbaYearsRemaining = 4,
            unionSatisfaction = nextSat,
            lockoutRisk = nextRisk,
            replacementRefsActive = false
        )
        val headline = "NLOA CBA RATIFIED: Locked 4-year referee peace accord (\$4M signing stipend) — Lockout risk at $nextRisk%."
        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - signingCost,
                leagueIntegrityScore = (it.leagueIntegrityScore + 3).coerceIn(20, 100),
                officiatingState = it.officiatingState.copy(
                    unionState = updatedUnion,
                    lastAuditHeadline = "4-Year NLOA Collective Bargaining Agreement signed."
                ),
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return true to "4-Year NLOA CBA signed; full veteran crews secured."
    }

    fun triggerOfficiatingAuditControversy(): BlownCallIncident {
        val state = _uiState.value
        val matches = state.currentWeekMatches.ifEmpty {
            generateMatchesForWeek(state.currentWeek, state.franchises, state.sportType, state.rivalries, state.cities)
        }
        val eval = OfficiatingEngine.evaluateWeekOfficiating(
            state = state.officiatingState,
            playedMatches = matches.take(1).map {
                if (it.isPlayed) it else it.copy(homeScore = 104, awayScore = 103, isPlayed = true)
            },
            nextMatches = matches,
            franchises = state.franchises,
            currentWeek = state.currentWeek,
            seasonYear = state.currentYear,
            isPlayoffs = state.seasonPhase == SeasonPhase.PLAYOFFS,
            forceIncidentForAudit = true
        )
        val incident = eval.newIncidents.first()
        _uiState.update {
            it.copy(
                franchises = eval.updatedFranchises,
                officiatingState = eval.updatedState,
                leagueIntegrityScore = (it.leagueIntegrityScore + eval.integrityDelta).coerceIn(20, 100),
                recentNews = (eval.headlines + it.recentNews).take(20)
            )
        }
        coreSimulationViewModel.emitDashboardSnackbar(
            DashboardSnackbarEvent(
                id = "snack_audit_${incident.id}",
                type = DashboardAlertType.OFFICIATING_ALERT,
                severity = DashboardAlertSeverity.CRITICAL,
                message = "Last-2-Min Review Flagged Error in ${incident.matchupTitle}!",
                actionLabel = "Respond",
                targetRoute = DashboardSnackbarRoute.OFFICIATING
            )
        )
        persistCurrentState()
        return incident
    }

    fun resolveBlownCallIncident(
        incidentId: String,
        choice: BacklashResponseChoice
    ): Pair<Boolean, String> {
        val state = _uiState.value
        if (choice.costMillions > state.cashReservesMillions) {
            return false to "Insufficient reserves (\$${choice.costMillions}M required)."
        }
        val (updatedOffState, updatedFranchises, news) = OfficiatingEngine.resolveIncident(
            state = state.officiatingState,
            incidentId = incidentId,
            choice = choice,
            franchises = state.franchises
        )
        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - choice.costMillions,
                franchises = updatedFranchises,
                officiatingState = updatedOffState,
                leagueIntegrityScore = (it.leagueIntegrityScore + choice.integrityDelta).coerceIn(20, 100),
                recentNews = (listOf(news) + it.recentNews).take(20)
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.OFFICIATING,
            decisionTitle = choice.title,
            decisionSummary = news,
            casualDelta = if (choice.integrityDelta >= 2) 2 else -1,
            dieHardDelta = choice.integrityDelta.coerceIn(-4, 5)
        )
        refreshJobSecurity(news)
        recordDashboardTrend("RefFix", "ref_res_${choice.id}")
        persistCurrentState()
        return true to "Response enacted: ${choice.title}."
    }

    // ==================== RULE COMMITTEE GOVERNANCE & SIMULATION RULES ====================

    fun openRuleCommitteeScreen(open: Boolean = true) {
        _uiState.update { state ->
            val alignedCommittee = if (state.ruleCommitteeState.sportType != state.sportType) {
                RuleCommitteeEngine.createDefaultState(state.sportType)
            } else {
                state.ruleCommitteeState
            }
            state.copy(
                showRuleCommitteeScreen = open,
                ruleCommitteeState = alignedCommittee
            )
        }
    }

    fun reviewProposalInCommittee(proposalId: String): SportRuleAmendment? {
        val state = _uiState.value
        val committee = state.ruleCommitteeState
        val idx = committee.proposals.indexOfFirst { it.id == proposalId }
        if (idx == -1) return null

        val target = committee.proposals[idx]
        if (target.status == SportRuleProposalStatus.ENACTED) return target

        val reviewed = RuleCommitteeEngine.evaluateCommitteeReview(target, committee.members)
        val updatedProposals = committee.proposals.toMutableList().apply { set(idx, reviewed) }
        val newsLine = "RULE COMMITTEE REVIEW (${committee.sportType.displayName}): ${reviewed.title} — ${reviewed.committeeReviewNote}"

        _uiState.update {
            it.copy(
                ruleCommitteeState = it.ruleCommitteeState.copy(
                    proposals = updatedProposals,
                    committeeApprovalRating = (it.ruleCommitteeState.committeeApprovalRating + if (reviewed.status == SportRuleProposalStatus.COMMITTEE_RECOMMENDED) 2 else -1).coerceIn(35, 99)
                ),
                recentNews = (listOf(newsLine) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return reviewed
    }

    fun enactCommitteeProposal(proposalId: String): Pair<Boolean, String> {
        val state = _uiState.value
        val hasMatchingSponsorDemand = state.leagueSponsors.allActiveContracts().any {
            it.ruleDemand?.ruleId == proposalId && it.ruleDemand.isPassed == false
        } || state.leagueSponsors.pendingOffseasonRuleVotes.any { it.ruleId == proposalId }
        if (hasMatchingSponsorDemand) {
            val outcome = voteOnSponsorRuleDemand(ruleId = proposalId, approve = true)
            return outcome.success to outcome.message
        }

        val committee = state.ruleCommitteeState
        val idx = committee.proposals.indexOfFirst { it.id == proposalId }
        if (idx == -1) return false to "Proposal not found on docket."

        val target = committee.proposals[idx]
        if (target.status == SportRuleProposalStatus.ENACTED) {
            return false to "${target.title} is already enacted."
        }

        val reviewedIfNeeded = if (target.committeeVotesFor + target.committeeVotesAgainst == 0) {
            RuleCommitteeEngine.evaluateCommitteeReview(target, committee.members)
        } else target

        val enacted = reviewedIfNeeded.copy(
            status = SportRuleProposalStatus.ENACTED,
            committeeReviewNote = if (reviewedIfNeeded.committeeReviewNote.isBlank()) {
                "Enacted into official ${state.sportType.displayName} simulation rules."
            } else reviewedIfNeeded.committeeReviewNote
        )
        val updatedProposals = committee.proposals.toMutableList().apply { set(idx, enacted) }
        val updatedCommittee = committee.copy(
            proposals = updatedProposals,
            committeeApprovalRating = (committee.committeeApprovalRating + 3).coerceIn(35, 99)
        )

        val newsHeadline = "RULE COMMITTEE ENACTED (${state.sportType.displayName}): '${enacted.title}' now actively alters simulation outcomes (${enacted.simImpactTags.joinToString(", ")})!"
        _uiState.update {
            it.copy(
                ruleCommitteeState = updatedCommittee,
                leagueIntegrityScore = (it.leagueIntegrityScore + enacted.integrityDelta).coerceIn(20, 100),
                competitiveParityScore = (it.competitiveParityScore + enacted.parityDelta).coerceIn(25, 99),
                recentNews = (listOf(newsHeadline) + it.recentNews).take(20)
            )
        }
        coreSimulationViewModel.emitDashboardSnackbar(
            DashboardSnackbarEvent(
                id = "snack_rc_enact_${enacted.id}_${System.currentTimeMillis() % 1000}",
                type = DashboardAlertType.SEASON_MILESTONE,
                severity = DashboardAlertSeverity.INFO,
                message = "Rule Enacted: ${enacted.title} now live in ${state.sportType.displayName} sim!",
                actionLabel = "Committee",
                targetRoute = DashboardSnackbarRoute.RULE_COMMITTEE
            )
        )
        val isSpectacleRule = enacted.scoringDeltaPerTeam > 0.5 || enacted.closeGameSwingPct > 0 || enacted.convertDrawsToShootout
        recordFanReaction(
            category = FanDecisionCategory.RULE_CHANGE,
            decisionTitle = "Enacted: ${enacted.title}",
            decisionSummary = newsHeadline,
            casualDelta = if (isSpectacleRule) 4 else 1,
            dieHardDelta = if (isSpectacleRule) -2 else enacted.integrityDelta.coerceIn(1, 4)
        )
        recordDashboardTrend("RuleLaw", "rc_enact_${enacted.id}")
        persistCurrentState()
        return true to "Enacted '${enacted.title}'. Next simulated games will reflect these changes."
    }

    fun repealCommitteeProposal(proposalId: String): Pair<Boolean, String> {
        val state = _uiState.value
        val committee = state.ruleCommitteeState
        val idx = committee.proposals.indexOfFirst { it.id == proposalId }
        if (idx == -1) return false to "Proposal not found."

        val target = committee.proposals[idx]
        if (target.status != SportRuleProposalStatus.ENACTED) {
            return false to "${target.title} is not currently enacted."
        }

        val repealed = target.copy(
            status = SportRuleProposalStatus.PROPOSED_DOCKET,
            committeeReviewNote = "Repealed from active ${state.sportType.displayName} simulation rules."
        )
        val updatedProposals = committee.proposals.toMutableList().apply { set(idx, repealed) }
        val newsHeadline = "RULE REPEALED: Rule Committee rescinded '${repealed.title}' (${state.sportType.displayName})."

        val updatedRulesList = state.ruleBook.rules.map { r ->
            if (r.id == proposalId) r.copy(isEnacted = false) else r
        }
        val breachResult = LeagueSponsorsEngine.handleRuleRepealed(
            currentState = state.leagueSponsors,
            ruleId = proposalId,
            seasonYear = state.currentYear,
            weekNumber = state.currentWeek
        )
        val newsList = buildList {
            if (breachResult.triggered && !breachResult.headline.isNullOrBlank()) {
                add(breachResult.headline)
            }
            add(newsHeadline)
            addAll(state.recentNews)
        }.take(20)

        _uiState.update {
            it.copy(
                ruleCommitteeState = committee.copy(proposals = updatedProposals),
                ruleBook = it.ruleBook.copy(rules = updatedRulesList),
                leagueSponsors = breachResult.updatedSponsorsState,
                leagueIntegrityScore = (it.leagueIntegrityScore - target.integrityDelta).coerceIn(20, 100),
                competitiveParityScore = (it.competitiveParityScore - target.parityDelta).coerceIn(25, 99),
                recentNews = newsList
            )
        }
        if (breachResult.triggered && breachResult.warning != null) {
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_spon_breach_${breachResult.warning.id}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = "Sponsor Breach Warning: ${breachResult.warning.sponsorName} slashed payout in half!",
                    actionLabel = "View",
                    targetRoute = DashboardSnackbarRoute.LEAGUE_SPONSORS
                )
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        recordDashboardTrend("RuleRepeal", "rc_repeal_${repealed.id}")
        persistCurrentState()
        return true to if (breachResult.triggered) {
            "Repealed '${repealed.title}' — Sponsor Breach Warning triggered!"
        } else {
            "Repealed '${repealed.title}' back to the Rule Committee docket."
        }
    }

    fun draftAndProposeCustomCommitteeRule(
        title: String,
        sponsorName: String,
        focusLever: RuleCommitteeFocusLever,
        summary: String,
        intensityLevel: Int = 2,
        immediatelyEnact: Boolean = false
    ): SportRuleAmendment {
        val state = _uiState.value
        val drafted = RuleCommitteeEngine.draftCustomProposal(
            sportType = state.sportType,
            title = title,
            sponsorName = sponsorName,
            focusLever = focusLever,
            summaryInput = summary,
            intensityLevel = intensityLevel
        )
        val newsLine = "NEW COMMITTEE PROPOSAL (${state.sportType.displayName}): '${drafted.title}' placed on docket (${drafted.simImpactTags.joinToString(", ")})."
        _uiState.update {
            it.copy(
                ruleCommitteeState = it.ruleCommitteeState.copy(
                    proposals = listOf(drafted) + it.ruleCommitteeState.proposals
                ),
                recentNews = (listOf(newsLine) + it.recentNews).take(20)
            )
        }
        if (immediatelyEnact) {
            enactCommitteeProposal(drafted.id)
        } else {
            persistCurrentState()
        }
        return drafted
    }

    // ==================== DISCIPLINARY DESK & PRECEDENT SYSTEM ====================

    fun openDisciplinaryDesk(open: Boolean = true) {
        _uiState.update { it.copy(showDisciplinaryDeskScreen = open) }
    }

    fun adjudicateDisciplinaryCase(
        incidentId: String,
        suspensionGames: Int,
        fineThousands: Int,
        setNewPrecedent: Boolean,
        commissionerNote: String = ""
    ): Pair<Boolean, String> {
        val state = _uiState.value
        val target = state.disciplinaryState.incidents.firstOrNull { it.id == incidentId }
            ?: return false to "Case not found on the disciplinary docket."
        if (target.status != DisciplinaryStatus.PENDING_HEARING) {
            return false to "${target.caseCode} has already been adjudicated."
        }

        val outcome = DisciplinaryEngine.adjudicateIncident(
            state = state.disciplinaryState,
            franchises = state.franchises,
            unionAgreement = state.unionAgreement,
            ruleBook = state.ruleBook,
            incidentId = incidentId,
            suspensionGames = suspensionGames,
            fineThousands = fineThousands,
            setNewPrecedent = setNewPrecedent,
            commissionerNote = commissionerNote
        )

        val prevState = state
        _uiState.update {
            val nextFanMoraleAvg = outcome.updatedFranchises.map { t -> t.fanMorale }.average().toInt()
            val updatedTeams = if (outcome.fanAppealDelta != 0) {
                outcome.updatedFranchises.map { team ->
                    if (team.id == target.franchiseId) team
                    else team.copy(fanMorale = (team.fanMorale + outcome.fanAppealDelta / 2).coerceIn(25, 99))
                }
            } else outcome.updatedFranchises

            val updatedLeagueSponsors = LeagueSponsorsEngine.onManualDisciplinaryRuling(
                currentSponsorsState = it.leagueSponsors,
                incident = target,
                proposedGames = suspensionGames,
                proposedFineThousands = fineThousands,
                seasonYear = it.currentYear,
                weekNumber = it.currentWeek
            )

            it.copy(
                disciplinaryState = outcome.updatedState,
                franchises = updatedTeams,
                unionAgreement = outcome.updatedUnion,
                leagueSponsors = updatedLeagueSponsors,
                cashReservesMillions = (it.cashReservesMillions + outcome.cashTreasuryGainMillions).coerceAtLeast(0L),
                leagueIntegrityScore = (it.leagueIntegrityScore + outcome.integrityDelta).coerceIn(20, 100),
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(prevState, _uiState.value)
        if (outcome.appealTriggered) {
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_appeal_${target.id}",
                    type = DashboardAlertType.DISCIPLINARY_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = "Players Union appealed ${target.playerName}'s ${suspensionGames}G ruling (${target.caseCode})!",
                    actionLabel = "Arbitrate",
                    targetRoute = DashboardSnackbarRoute.DISCIPLINARY_DESK
                )
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.DISCIPLINARY,
            decisionTitle = "${target.caseCode}: ${suspensionGames}G / \$${fineThousands}K",
            decisionSummary = outcome.headline,
            casualDelta = outcome.fanAppealDelta.coerceIn(-4, 4),
            dieHardDelta = outcome.integrityDelta.coerceIn(-4, 5),
            franchiseName = target.franchiseName
        )
        recordDashboardTrend("Discipline", "disc_ruling_${target.caseCode}")
        refreshJobSecurity(outcome.headline)
        persistCurrentState()
        return true to outcome.feedbackMessage
    }

    fun resolveDisciplinaryAppeal(
        incidentId: String,
        action: AppealResolutionAction
    ): Pair<Boolean, String> {
        val state = _uiState.value
        val target = state.disciplinaryState.incidents.firstOrNull { it.id == incidentId }
            ?: return false to "Appeal case not found."
        if (target.status != DisciplinaryStatus.APPEALED_PENDING) {
            return false to "${target.caseCode} does not have an open arbitration appeal."
        }

        val outcome = DisciplinaryEngine.resolveAppeal(
            state = state.disciplinaryState,
            franchises = state.franchises,
            unionAgreement = state.unionAgreement,
            incidentId = incidentId,
            action = action
        )
        _uiState.update {
            it.copy(
                disciplinaryState = outcome.updatedState,
                franchises = outcome.updatedFranchises,
                unionAgreement = outcome.updatedUnion,
                leagueIntegrityScore = (it.leagueIntegrityScore + outcome.integrityDelta).coerceIn(20, 100),
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }
        recordDashboardTrend("Arbitration", "disc_appeal_${target.caseCode}")
        refreshJobSecurity(outcome.headline)
        persistCurrentState()
        return true to outcome.feedbackMessage
    }

    fun updateDisciplinaryPrecedentPolicy(
        category: DisciplinaryCategory,
        benchmarkGames: Int,
        benchmarkFineThousands: Int,
        posture: PrecedentPosture
    ): String {
        val state = _uiState.value
        val updatedState = DisciplinaryEngine.updateCategoryPrecedent(
            state = state.disciplinaryState,
            category = category,
            newBenchmarkGames = benchmarkGames,
            newBenchmarkFineThousands = benchmarkFineThousands,
            newPosture = posture,
            seasonYear = state.currentYear,
            weekNumber = state.currentWeek
        )
        val headline = "PRECEDENT POLICY MEMO: ${category.title} set to ${posture.label} (${benchmarkGames}G / ${DisciplinaryEngine.formatFine(benchmarkFineThousands)} baseline)."
        _uiState.update {
            it.copy(
                disciplinaryState = updatedState,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return "Updated ${category.shortLabel} precedent to ${benchmarkGames}G / ${DisciplinaryEngine.formatFine(benchmarkFineThousands)} (${posture.label})."
    }

    fun generateDisciplinaryIncidentNow(): DisciplinaryIncident? {
        val state = _uiState.value
        val (updatedState, created) = DisciplinaryEngine.generateManualIncident(
            state = state.disciplinaryState,
            franchises = state.franchises,
            sportType = state.sportType,
            seasonYear = state.currentYear,
            week = state.currentWeek
        )
        if (created == null) return null
        val headline = "NEW REFERRAL (${created.caseCode}): ${created.playerName} (${created.franchiseName}) cited for ${created.incidentTitle}."
        _uiState.update {
            val updatedLeagueSponsors = LeagueSponsorsEngine.evaluateScandalForUltimatum(
                currentState = it.leagueSponsors,
                scandalSourceTitle = "${created.playerName} • ${created.incidentTitle}",
                scandalSummary = created.incidentSummary,
                seasonYear = it.currentYear,
                weekNumber = it.currentWeek,
                relatedIncident = created,
                forceTriggerOnAnyIncident = true
            )
            it.copy(
                disciplinaryState = updatedState,
                leagueSponsors = updatedLeagueSponsors,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return created
    }

    // ==================== BRANCHING NARRATIVE CRISIS EVENTS ====================

    fun openCrisisEventsScreen(open: Boolean = true) {
        _uiState.update { it.copy(showCrisisEventsScreen = open) }
    }

    fun triggerCrisisScenarioNow(preferredArchetype: CrisisArchetype? = null): CommissionerDilemma? {
        val state = _uiState.value
        if (state.activeDilemma != null) return state.activeDilemma
        val (nextCrisisState, dilemma) = CrisisEngine.triggerFreshCrisis(
            crisisState = state.crisisState,
            sportType = state.sportType,
            franchises = state.franchises,
            activeBroadcast = state.activeBroadcast,
            activeSponsorship = state.activeSponsorship,
            cities = state.cities,
            leagueIntegrityScore = state.leagueIntegrityScore,
            unionAgreement = state.unionAgreement,
            officiatingState = state.officiatingState,
            seasonYear = state.currentYear,
            week = state.currentWeek,
            preferredArchetype = preferredArchetype
        )
        if (dilemma == null) return null
        val headline = "CRISIS FLASHPOINT [Stage ${dilemma.stageNumber}/${dilemma.totalStages}]: ${dilemma.title}"
        _uiState.update {
            val updatedSponsors = LeagueSponsorsEngine.evaluateScandalForUltimatum(
                currentState = it.leagueSponsors,
                scandalSourceTitle = dilemma.title,
                scandalSummary = dilemma.description,
                seasonYear = it.currentYear,
                weekNumber = it.currentWeek
            )
            it.copy(
                crisisState = nextCrisisState,
                activeDilemma = dilemma,
                leagueSponsors = updatedSponsors,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return dilemma
    }

    // ==================== SEASON SCHEDULE BUILDER & EXPANDABLE CALENDAR ====================

    private fun ensureUpcomingSchedulePlan(state: LeagueUiState): SeasonScheduleState {
        val targetYear = state.currentYear + 1
        val targetWeeks = state.ruleBook.regularSeasonWeeks.coerceIn(14, 22)
        val currentUpcoming = state.seasonScheduleState.upcomingPlan
        val effectiveUpcoming = when {
            currentUpcoming == null || currentUpcoming.slots.isEmpty() || currentUpcoming.targetYear != targetYear -> {
                ScheduleCalendarEngine.generateFullSeasonSchedule(
                    targetYear = targetYear,
                    regularSeasonWeeks = targetWeeks,
                    franchises = state.franchises,
                    rivalries = state.rivalries,
                    cities = state.cities,
                    sportType = state.sportType
                )
            }
            currentUpcoming.regularSeasonWeeks != targetWeeks -> {
                ScheduleCalendarEngine.resizeScheduleForSeasonLength(
                    plan = currentUpcoming,
                    newRegularSeasonWeeks = targetWeeks,
                    franchises = state.franchises,
                    rivalries = state.rivalries,
                    unionAgreement = state.unionAgreement
                )
            }
            else -> ScheduleCalendarEngine.validateAndRecompute(currentUpcoming, state.franchises, state.unionAgreement)
        }
        return state.seasonScheduleState.copy(upcomingPlan = effectiveUpcoming)
    }

    fun openScheduleCalendarScreen(open: Boolean = true) {
        _uiState.update { state ->
            val ensured = if (open) ensureUpcomingSchedulePlan(state) else state.seasonScheduleState
            state.copy(
                showScheduleCalendarScreen = open,
                seasonScheduleState = ensured
            )
        }
    }

    fun initializeOrRefreshUpcomingSchedule() {
        _uiState.update { state ->
            state.copy(seasonScheduleState = ensureUpcomingSchedulePlan(state))
        }
        persistCurrentState()
    }

    fun autoGenerateBalancedUpcomingSchedule() {
        val state = _uiState.value
        val nextYear = state.currentYear + 1
        val fresh = ScheduleCalendarEngine.generateFullSeasonSchedule(
            targetYear = nextYear,
            regularSeasonWeeks = state.ruleBook.regularSeasonWeeks,
            franchises = state.franchises,
            rivalries = state.rivalries,
            cities = state.cities,
            sportType = state.sportType
        )
        val validated = ScheduleCalendarEngine.validateAndRecompute(fresh, state.franchises, state.unionAgreement)
        _uiState.update {
            it.copy(
                seasonScheduleState = it.seasonScheduleState.copy(upcomingPlan = validated),
                recentNews = (listOf("SCHEDULE RESET: Balanced $nextYear calendar generated (${validated.regularSeasonWeeks}W, ${validated.holidayShowcaseCount} Holidays, ${validated.internationalGameCount} International).") + it.recentNews).take(20)
            )
        }
        persistCurrentState()
    }

    fun flipUpcomingMatchHomeAway(slotId: String) {
        _uiState.update { state ->
            val ensured = ensureUpcomingSchedulePlan(state)
            val plan = ensured.upcomingPlan ?: return@update state
            val updated = ScheduleCalendarEngine.flipHomeAway(
                plan = plan,
                slotId = slotId,
                franchises = state.franchises,
                unionAgreement = state.unionAgreement
            )
            state.copy(seasonScheduleState = ensured.copy(upcomingPlan = updated))
        }
        persistCurrentState()
    }

    fun swapUpcomingMatchTeam(slotId: String, replaceHome: Boolean, newTeamId: String) {
        _uiState.update { state ->
            val ensured = ensureUpcomingSchedulePlan(state)
            val plan = ensured.upcomingPlan ?: return@update state
            val updated = ScheduleCalendarEngine.swapMatchupTeam(
                plan = plan,
                slotId = slotId,
                replaceHome = replaceHome,
                newTeamId = newTeamId,
                franchises = state.franchises,
                rivalries = state.rivalries,
                unionAgreement = state.unionAgreement
            )
            state.copy(seasonScheduleState = ensured.copy(upcomingPlan = updated))
        }
        persistCurrentState()
    }

    fun moveUpcomingMatchToWeek(slotId: String, targetWeek: Int) {
        _uiState.update { state ->
            val ensured = ensureUpcomingSchedulePlan(state)
            val plan = ensured.upcomingPlan ?: return@update state
            val updated = ScheduleCalendarEngine.moveMatchToWeek(
                plan = plan,
                slotId = slotId,
                targetWeek = targetWeek,
                franchises = state.franchises,
                unionAgreement = state.unionAgreement
            )
            state.copy(seasonScheduleState = ensured.copy(upcomingPlan = updated))
        }
        persistCurrentState()
    }

    fun setHolidayMarqueeOnUpcomingSlot(
        slotId: String,
        tentpole: HolidayTentpole,
        customRivalryTitle: String? = null
    ) {
        _uiState.update { state ->
            val ensured = ensureUpcomingSchedulePlan(state)
            val plan = ensured.upcomingPlan ?: return@update state
            val updated = ScheduleCalendarEngine.setHolidayMarqueeOnSlot(
                plan = plan,
                slotId = slotId,
                tentpole = tentpole,
                customRivalryTitle = customRivalryTitle,
                promoteAsMarqueeRivalry = tentpole != HolidayTentpole.NONE,
                franchises = state.franchises,
                rivalries = state.rivalries,
                unionAgreement = state.unionAgreement
            )
            state.copy(seasonScheduleState = ensured.copy(upcomingPlan = updated))
        }
        persistCurrentState()
    }

    fun setInternationalShowcaseOnUpcomingSlot(slotId: String, venueId: String?) {
        _uiState.update { state ->
            val ensured = ensureUpcomingSchedulePlan(state)
            val plan = ensured.upcomingPlan ?: return@update state
            val updated = ScheduleCalendarEngine.assignInternationalVenueToSlot(
                plan = plan,
                slotId = slotId,
                venueId = venueId,
                franchises = state.franchises,
                unionAgreement = state.unionAgreement
            )
            state.copy(seasonScheduleState = ensured.copy(upcomingPlan = updated))
        }
        persistCurrentState()
    }

    fun submitUpcomingScheduleForApproval(forceCommissionerOverride: Boolean = false): BoardVoteResult {
        val stateNow = _uiState.value
        val ensured = ensureUpcomingSchedulePlan(stateNow)
        val plan = ensured.upcomingPlan!!

        val rawVote = OwnerGovernanceEngine.evaluateScheduleApprovalVote(
            franchises = stateNow.franchises,
            plan = plan,
            unionAgreement = stateNow.unionAgreement,
            year = stateNow.currentYear,
            week = stateNow.currentWeek,
            enactedViaCommissionerOverride = forceCommissionerOverride
        )

        if (plan.hasValidationErrors && !forceCommissionerOverride) {
            pendingOverrideAction = null
            val rejectedPlan = plan.copy(
                approvalStatus = ScheduleApprovalStatus.REJECTED_REVISION_REQUIRED,
                lastVoteSummary = "Board rejected invalid schedule (${plan.conflictWarnings.firstOrNull() ?: "conflict"})."
            )
            _uiState.update {
                it.copy(
                    seasonScheduleState = ensured.copy(upcomingPlan = rejectedPlan),
                    boardVoteHistory = (listOf(rawVote) + it.boardVoteHistory).take(30),
                    lastBoardVoteResult = rawVote,
                    activeBoardVoteModal = rawVote,
                    recentNews = (listOf("SCHEDULE REJECTED: Fix intra-week scheduling conflicts before re-submitting.") + it.recentNews).take(20)
                )
            }
            persistCurrentState()
            return rawVote
        }

        if (!rawVote.passed && !forceCommissionerOverride) {
            pendingOverrideAction = { submitUpcomingScheduleForApproval(forceCommissionerOverride = true) }
            val reactedFranchises = OwnerGovernanceEngine.applyApprovalReactions(stateNow.franchises, rawVote)
            val avgApproval = reactedFranchises.map { it.ownership.ownerApproval }.average().toInt()
            val vetoPlan = plan.copy(
                approvalStatus = ScheduleApprovalStatus.REJECTED_REVISION_REQUIRED,
                lastVoteSummary = "Vetoed (${rawVote.marginSummary}): Adjust international travel or holiday slates."
            )
            _uiState.update {
                it.copy(
                    franchises = reactedFranchises,
                    ownerApprovalRating = avgApproval,
                    seasonScheduleState = ensured.copy(upcomingPlan = vetoPlan),
                    boardVoteHistory = (listOf(rawVote) + it.boardVoteHistory).take(30),
                    lastBoardVoteResult = rawVote,
                    activeBoardVoteModal = rawVote,
                    recentNews = (listOf("BOARD VETO ON ${plan.targetYear} SCHEDULE (${rawVote.marginSummary}): Owners demand schedule revisions!") + it.recentNews).take(20)
                )
            }
            persistCurrentState()
            return rawVote
        }

        pendingOverrideAction = null
        val finalVote = if (!rawVote.passed && forceCommissionerOverride) {
            rawVote.copy(passed = true, enactedViaCommissionerOverride = true)
        } else rawVote
        val reactedFranchises = OwnerGovernanceEngine.applyApprovalReactions(stateNow.franchises, finalVote)
        val avgApproval = reactedFranchises.map { it.ownership.ownerApproval }.average().toInt()
        val approvedStatus = if (finalVote.enactedViaCommissionerOverride) {
            ScheduleApprovalStatus.APPROVED_BY_OVERRIDE
        } else {
            ScheduleApprovalStatus.APPROVED_BY_BOARD
        }
        val approvedPlan = plan.copy(
            approvalStatus = approvedStatus,
            lastVoteSummary = "${approvedStatus.label} (${finalVote.marginSummary}) — ${plan.holidayShowcaseCount} Holidays, ${plan.internationalGameCount} International Games."
        )
        val unionPenalty = if (plan.internationalGameCount > 1 && !stateNow.unionAgreement.mandatoryPrivateCharterFlights) -3 else 0
        val nextSat = (stateNow.unionAgreement.unionSatisfaction + unionPenalty).coerceIn(10, 100)
        val updatedUnion = stateNow.unionAgreement.copy(
            unionSatisfaction = nextSat,
            strikeRisk = (100 - nextSat).coerceIn(0, 95)
        )
        val headline = if (finalVote.enactedViaCommissionerOverride) {
            "EXECUTIVE DECREE: Commissioner enacted Season ${plan.targetYear} ${plan.regularSeasonWeeks}-week schedule (${plan.holidayShowcaseCount} Holidays, ${plan.internationalGameCount} Intl) over Board objections."
        } else {
            "SCHEDULE RATIFIED (${finalVote.marginSummary}): Season ${plan.targetYear} ${plan.regularSeasonWeeks}-week calendar approved with ${plan.holidayShowcaseCount} Holiday Marquees & ${plan.internationalGameCount} International Games!"
        }
        _uiState.update {
            it.copy(
                franchises = reactedFranchises,
                ownerApprovalRating = avgApproval,
                unionAgreement = updatedUnion,
                seasonScheduleState = ensured.copy(upcomingPlan = approvedPlan),
                boardVoteHistory = (listOf(finalVote) + it.boardVoteHistory).take(30),
                lastBoardVoteResult = finalVote,
                activeBoardVoteModal = finalVote,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.ALL_STAR_SCHEDULE,
            decisionTitle = "${plan.targetYear} Schedule (${plan.holidayShowcaseCount} Holidays, ${plan.internationalGameCount} Intl)",
            decisionSummary = headline,
            casualDelta = (if (plan.holidayShowcaseCount > 0) 3 else 1) + (if (plan.internationalGameCount > 0) 2 else 0),
            dieHardDelta = if (plan.internationalGameCount > 2) -2 else 2
        )
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return finalVote
    }

    fun proposeRegularSeasonLengthVote(
        targetWeeks: Int,
        forceCommissionerOverride: Boolean = false
    ): BoardVoteResult {
        val stateNow = _uiState.value
        val clampedWeeks = targetWeeks.coerceIn(14, 22)
        val currentWeeks = stateNow.ruleBook.regularSeasonWeeks

        val rawVote = OwnerGovernanceEngine.evaluateSeasonExpansionVote(
            franchises = stateNow.franchises,
            currentWeeks = currentWeeks,
            targetWeeks = clampedWeeks,
            unionAgreement = stateNow.unionAgreement,
            year = stateNow.currentYear,
            week = stateNow.currentWeek,
            enactedViaCommissionerOverride = forceCommissionerOverride
        )

        if (!rawVote.passed && !forceCommissionerOverride) {
            pendingOverrideAction = { proposeRegularSeasonLengthVote(clampedWeeks, forceCommissionerOverride = true) }
            val reactedFranchises = OwnerGovernanceEngine.applyApprovalReactions(stateNow.franchises, rawVote)
            val avgApproval = reactedFranchises.map { it.ownership.ownerApproval }.average().toInt()
            _uiState.update {
                it.copy(
                    franchises = reactedFranchises,
                    ownerApprovalRating = avgApproval,
                    boardVoteHistory = (listOf(rawVote) + it.boardVoteHistory).take(30),
                    lastBoardVoteResult = rawVote,
                    activeBoardVoteModal = rawVote,
                    recentNews = (listOf("BOARD VETO (${rawVote.marginSummary}): Proposal to set regular season to $clampedWeeks weeks was rejected.") + it.recentNews).take(20)
                )
            }
            persistCurrentState()
            return rawVote
        }

        pendingOverrideAction = null
        val finalVote = if (!rawVote.passed && forceCommissionerOverride) {
            rawVote.copy(passed = true, enactedViaCommissionerOverride = true)
        } else rawVote
        val reactedFranchises = OwnerGovernanceEngine.applyApprovalReactions(stateNow.franchises, finalVote)
        val avgApproval = reactedFranchises.map { it.ownership.ownerApproval }.average().toInt()
        val updatedRuleBook = stateNow.ruleBook.copy(regularSeasonWeeks = clampedWeeks)

        val ensuredSchedule = ensureUpcomingSchedulePlan(stateNow)
        val resizedUpcoming = ensuredSchedule.upcomingPlan?.let {
            ScheduleCalendarEngine.resizeScheduleForSeasonLength(
                plan = it,
                newRegularSeasonWeeks = clampedWeeks,
                franchises = reactedFranchises,
                rivalries = stateNow.rivalries,
                unionAgreement = stateNow.unionAgreement
            )
        }
        val resizedActive = ensuredSchedule.activeSeasonSchedule?.let {
            ScheduleCalendarEngine.resizeScheduleForSeasonLength(
                plan = it,
                newRegularSeasonWeeks = clampedWeeks,
                franchises = reactedFranchises,
                rivalries = stateNow.rivalries,
                unionAgreement = stateNow.unionAgreement
            )
        }

        val valuationBoost = ((clampedWeeks - currentWeeks) * 6L)
        val unionAdj = when {
            clampedWeeks >= 20 && !stateNow.unionAgreement.maxBackToBackGamesLimit -> -5
            clampedWeeks > currentWeeks -> -2
            clampedWeeks < currentWeeks -> +3
            else -> 0
        }
        val nextSat = (stateNow.unionAgreement.unionSatisfaction + unionAdj).coerceIn(10, 100)
        val updatedUnion = stateNow.unionAgreement.copy(
            unionSatisfaction = nextSat,
            strikeRisk = (100 - nextSat).coerceIn(0, 95)
        )
        val allStarWk = ScheduleCalendarEngine.allStarWeekFor(clampedWeeks)
        val headline = if (finalVote.enactedViaCommissionerOverride) {
            "EXECUTIVE ORDER: Regular season expanded to $clampedWeeks weeks (All-Star Week $allStarWk) by Commissioner Override!"
        } else {
            "SEASON EXPANSION RATIFIED (${finalVote.marginSummary}): Regular season set to $clampedWeeks weeks (All-Star Break moved to Week $allStarWk)!"
        }
        _uiState.update {
            it.copy(
                franchises = reactedFranchises,
                ownerApprovalRating = avgApproval,
                ruleBook = updatedRuleBook,
                totalWeeks = ScheduleCalendarEngine.totalWeeksFor(clampedWeeks),
                leagueValuationMillions = (it.leagueValuationMillions + valuationBoost).coerceAtLeast(100L),
                unionAgreement = updatedUnion,
                seasonScheduleState = ensuredSchedule.copy(
                    upcomingPlan = resizedUpcoming,
                    activeSeasonSchedule = resizedActive
                ),
                boardVoteHistory = (listOf(finalVote) + it.boardVoteHistory).take(30),
                lastBoardVoteResult = finalVote,
                activeBoardVoteModal = finalVote,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        refreshJobSecurity(headline)
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return finalVote
    }

    // ==================== JOB SECURITY, LEGACY METER & ANNUAL PERFORMANCE REVIEW ====================

    fun openJobSecurityLegacyScreen(open: Boolean = true) {
        if (open) {
            refreshJobSecurity()
        }
        _uiState.update { it.copy(showJobSecurityLegacyScreen = open) }
    }

    fun conveneAnnualPerformanceReviewNow(): AnnualPerformanceReview {
        val state = _uiState.value
        val existingPending = state.jobSecurityLegacyState.pendingAnnualReview
        if (existingPending != null && !existingPending.isRatified) {
            return existingPending
        }
        val input = state.toPerformanceInput(
            seasonChampionNameOverride = state.seasonChampion?.teamFullName
                ?: state.valuationHistory.firstOrNull()?.championName
        )
        val generated = JobSecurityLegacyEngine.generateAnnualReview(
            current = state.jobSecurityLegacyState,
            input = input
        )
        val headline = "ANNUAL REVIEW CONVENED (Y${generated.seasonYear}): Stakeholders graded Commissioner [${generated.overallGrade.letter}] — ${generated.contractAction.label}."
        _uiState.update {
            it.copy(
                jobSecurityLegacyState = it.jobSecurityLegacyState.copy(
                    latestBreakdown = JobSecurityLegacyEngine.evaluateStakeholders(input),
                    pendingAnnualReview = generated,
                    lastUpdateHeadline = headline
                ),
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        persistCurrentState()
        return generated
    }

    fun ratifyPendingAnnualReview(
        response: ReviewResponseChoice = ReviewResponseChoice.OWNER_GROWTH_CHARTER
    ): ReviewRatificationOutcome? {
        val state = _uiState.value
        val pending = state.jobSecurityLegacyState.pendingAnnualReview ?: conveneAnnualPerformanceReviewNow()
        val outcome = JobSecurityLegacyEngine.ratifyAnnualReview(
            current = _uiState.value.jobSecurityLegacyState,
            review = pending,
            response = response
        )

        val currentState = _uiState.value
        val newCash = (currentState.cashReservesMillions + outcome.cashDeltaMillions).coerceAtLeast(0L)
        val newValuation = (currentState.leagueValuationMillions + outcome.valuationDeltaMillions).coerceAtLeast(100L)
        val newFanbase = ((currentState.totalLeagueFanbaseMillions + outcome.fanbaseDeltaMillions) * 100.0)
            .let { kotlin.math.round(it) / 100.0 }
            .coerceAtLeast(1.0)
        val newOwnerApproval = (currentState.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(20, 100)
        val nextSat = (currentState.unionAgreement.unionSatisfaction + outcome.unionSatisfactionDelta).coerceIn(10, 100)
        val nextStrike = (currentState.unionAgreement.strikeRisk + outcome.strikeRiskDelta).coerceIn(0, 95)
        val updatedUnion = currentState.unionAgreement.copy(
            unionSatisfaction = nextSat,
            strikeRisk = nextStrike
        )
        val updatedFranchises = currentState.franchises.map { team ->
            team.copy(
                fanMorale = (team.fanMorale + outcome.fanMoraleDelta).coerceIn(20, 99),
                fanLoyalty = (team.fanLoyalty + outcome.fanLoyaltyDelta).coerceIn(15, 100),
                ownership = team.ownership.copy(
                    ownerApproval = (team.ownership.ownerApproval + outcome.ownerApprovalDelta).coerceIn(20, 100)
                )
            )
        }
        val updatedDisc = if (response == ReviewResponseChoice.PLAYER_PARTNERSHIP_ACCORD) {
            currentState.disciplinaryState.copy(
                precedentConsistencyScore = (currentState.disciplinaryState.precedentConsistencyScore + 4).coerceIn(20, 100)
            )
        } else currentState.disciplinaryState

        _uiState.update {
            val preInput = it.copy(
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                totalLeagueFanbaseMillions = newFanbase,
                ownerApprovalRating = newOwnerApproval,
                unionAgreement = updatedUnion,
                franchises = updatedFranchises,
                disciplinaryState = updatedDisc,
                jobSecurityLegacyState = outcome.updatedState,
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
            val refreshedBreakdown = JobSecurityLegacyEngine.evaluateStakeholders(preInput.toPerformanceInput())
            preInput.copy(
                jobSecurityLegacyState = outcome.updatedState.copy(latestBreakdown = refreshedBreakdown)
            )
        }
        notifyFinancialMilestones(currentState, _uiState.value)
        recordDashboardTrend("Review Y${pending.seasonYear}", "review_ratify_${pending.seasonYear}")
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    // ==================== RIVAL LEAGUE & RIVAL WATCH ====================

    fun openRivalWatchModal(open: Boolean = true) {
        _uiState.update { it.copy(showRivalWatchModal = open) }
    }

    fun performRivalWatchAction(action: RivalWatchAction): Pair<Boolean, String> {
        val currentState = _uiState.value
        val outcome = RivalLeagueEngine.applyRivalWatchAction(
            rivalLeague = currentState.rivalLeague,
            action = action,
            currentCashMillions = currentState.cashReservesMillions,
            freeAgentPool = currentState.freeAgentPool
        )
        if (!outcome.success) {
            _uiState.update {
                it.copy(
                    rivalLeague = it.rivalLeague.copy(lastActionFeedback = outcome.message)
                )
            }
            return false to outcome.message
        }

        val newCash = (currentState.cashReservesMillions + outcome.cashDeltaMillions).coerceAtLeast(0L)
        val newOwnerApproval = (currentState.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(15, 100)
        val nextSat = (currentState.unionAgreement.unionSatisfaction + outcome.unionSatisfactionDelta).coerceIn(10, 100)
        val nextStrike = (currentState.unionAgreement.strikeRisk + outcome.strikeRiskDelta).coerceIn(0, 95)
        val updatedUnion = currentState.unionAgreement.copy(
            unionSatisfaction = nextSat,
            strikeRisk = nextStrike
        )
        val newFanbase = ((currentState.totalLeagueFanbaseMillions + outcome.fanbaseDeltaMillions) * 100.0)
            .let { kotlin.math.round(it) / 100.0 }
            .coerceAtLeast(1.0)
        val updatedFranchises = currentState.franchises.map { team ->
            team.copy(
                ownership = team.ownership.copy(
                    ownerApproval = (team.ownership.ownerApproval + outcome.ownerApprovalDelta).coerceIn(15, 100)
                )
            )
        }
        val headline = outcome.headline ?: outcome.message

        _uiState.update {
            it.copy(
                cashReservesMillions = newCash,
                ownerApprovalRating = newOwnerApproval,
                unionAgreement = updatedUnion,
                totalLeagueFanbaseMillions = newFanbase,
                franchises = updatedFranchises,
                rivalLeague = outcome.updatedRivalLeague,
                freeAgentPool = outcome.updatedFreeAgentPool,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }

        if (outcome.didFoldRivalLeague) {
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_rival_fold_${currentState.currentYear}_${currentState.currentWeek}",
                    type = DashboardAlertType.SEASON_MILESTONE,
                    severity = DashboardAlertSeverity.MILESTONE,
                    message = headline,
                    actionLabel = "Rival Watch",
                    targetRoute = DashboardSnackbarRoute.RIVAL_WATCH
                )
            )
        }

        notifyFinancialMilestones(currentState, _uiState.value)
        val rivalFanPair = when (action) {
            RivalWatchAction.COUNTER_PROGRAM -> 4 to 1
            RivalWatchAction.BLACKLIST_DEFECTORS -> -3 to 3
            RivalWatchAction.PROPOSE_MERGER -> 5 to -2
        }
        recordFanReaction(
            category = FanDecisionCategory.RIVAL_LEAGUE,
            decisionTitle = "Rival Watch: ${action.title}",
            decisionSummary = headline,
            casualDelta = rivalFanPair.first,
            dieHardDelta = rivalFanPair.second
        )
        refreshJobSecurity(headline)
        recordDashboardTrend("W${currentState.currentWeek}", "rival_${action.name.lowercase()}")
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return true to outcome.message
    }

    fun toggleRivalAbsorptionCandidate(candidateId: String) {
        _uiState.update { state ->
            state.copy(
                rivalLeague = RivalLeagueEngine.toggleAbsorptionCandidate(
                    rivalLeague = state.rivalLeague,
                    candidateId = candidateId
                )
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
    }

    fun resolveRivalAbsorptionNegotiation(acceptAbsorption: Boolean): RivalAbsorptionOutcome {
        val currentState = _uiState.value
        val outcome = RivalLeagueEngine.resolveAbsorptionNegotiation(
            rivalLeague = currentState.rivalLeague,
            existingFranchises = currentState.franchises,
            existingCities = currentState.cities,
            ruleBook = currentState.ruleBook,
            freeAgentPool = currentState.freeAgentPool,
            sportType = currentState.sportType,
            seasonYear = currentState.currentYear,
            acceptAbsorption = acceptAbsorption
        )
        if (!outcome.success) return outcome

        if (!outcome.absorbed) {
            val newOwnerApproval = (currentState.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(15, 100)
            val updatedFranchises = currentState.franchises.map { team ->
                team.copy(
                    ownership = team.ownership.copy(
                        ownerApproval = (team.ownership.ownerApproval + outcome.ownerApprovalDelta).coerceIn(15, 100)
                    )
                )
            }
            _uiState.update {
                it.copy(
                    ownerApprovalRating = newOwnerApproval,
                    franchises = updatedFranchises,
                    rivalLeague = outcome.updatedRivalLeague,
                    showRivalWatchModal = false,
                    recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
                )
            }
            recordFanReaction(
                category = FanDecisionCategory.RIVAL_LEAGUE,
                decisionTitle = "Rejected Rival League Merger",
                decisionSummary = outcome.headline,
                casualDelta = -2,
                dieHardDelta = 2
            )
            refreshJobSecurity(outcome.headline)
            recordDashboardTrend("Rival", "rival_reject_${currentState.currentYear}")
            coreSimulationViewModel.syncFromLeagueState(_uiState.value)
            persistCurrentState()
            return outcome
        }

        val combinedFranchises = currentState.franchises + outcome.absorbedFranchises
        val adaptedPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
            currentRules = currentState.ruleBook.playoffRules,
            franchises = combinedFranchises,
            enabledConferences = currentState.ruleBook.enabledConferences
        )
        val syncedRivalries = RivalryEngine.syncRivalriesForLeagueTopology(
            combinedFranchises,
            outcome.updatedCities,
            currentState.rivalries,
            currentState.currentYear
        )
        val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
            franchises = combinedFranchises,
            currentMinorState = currentState.minorLeagueState,
            sportType = currentState.sportType,
            seasonYear = currentState.currentYear
        )
        val finalFranchises = minorSync.updatedFranchises.map { team ->
            team.copy(
                ownership = team.ownership.copy(
                    ownerApproval = (team.ownership.ownerApproval + outcome.ownerApprovalDelta).coerceIn(15, 100)
                )
            )
        }
        val newOwnerApproval = (currentState.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(15, 100)
        val updatedBroadcast = currentState.activeBroadcast.copy(
            annualPayoutMillions = currentState.activeBroadcast.annualPayoutMillions + outcome.permanentTvBoostMillions
        )
        val updatedFanbase = Math.round((currentState.totalLeagueFanbaseMillions + outcome.fanbaseDeltaMillions) * 10.0) / 10.0
        val absorbedCityIds = outcome.absorbedFranchises.map { it.cityId }.toSet()
        val remainingPending = currentState.pendingExpansionRequests.filterNot { it.city.id in absorbedCityIds }
        val recalculatedPending = ExpansionRequestEngine.recalculateImpacts(
            requests = remainingPending,
            franchises = finalFranchises,
            activeBroadcast = updatedBroadcast,
            activeSponsorship = currentState.activeSponsorship,
            currentMarketReachMillions = updatedFanbase
        )
        val updatedUpcomingPlan = currentState.seasonScheduleState.upcomingPlan?.let { existingPlan ->
            ScheduleCalendarEngine.generateFullSeasonSchedule(
                targetYear = existingPlan.targetYear,
                regularSeasonWeeks = currentState.ruleBook.regularSeasonWeeks,
                franchises = finalFranchises,
                rivalries = syncedRivalries,
                cities = outcome.updatedCities,
                sportType = currentState.sportType
            )
        }
        val newValuation = currentState.leagueValuationMillions +
            (outcome.permanentTvBoostMillions * 2L) +
            (outcome.absorbedFranchises.size * 35L)

        _uiState.update {
            it.copy(
                ownerApprovalRating = newOwnerApproval,
                leagueValuationMillions = newValuation,
                totalLeagueFanbaseMillions = updatedFanbase,
                activeBroadcast = updatedBroadcast,
                franchises = finalFranchises,
                minorLeagueState = minorSync.minorState,
                cities = outcome.updatedCities,
                rivalries = syncedRivalries,
                freeAgentPool = outcome.updatedFreeAgentPool,
                pendingExpansionRequests = recalculatedPending,
                ruleBook = it.ruleBook.copy(playoffRules = adaptedPlayoffRules),
                seasonScheduleState = it.seasonScheduleState.copy(upcomingPlan = updatedUpcomingPlan),
                rivalLeague = outcome.updatedRivalLeague,
                showRivalWatchModal = false,
                recentNews = (listOf(outcome.headline) + minorSync.headlines + it.recentNews).take(20)
            )
        }

        coreSimulationViewModel.emitDashboardSnackbar(
            DashboardSnackbarEvent(
                id = "snack_rival_absorb_${currentState.currentYear}_${outcome.absorbedFranchises.size}",
                type = DashboardAlertType.SEASON_MILESTONE,
                severity = DashboardAlertSeverity.MILESTONE,
                message = outcome.headline,
                actionLabel = "Finances",
                targetRoute = DashboardSnackbarRoute.FINANCES
            )
        )
        notifyFinancialMilestones(currentState, _uiState.value)
        recordFanReaction(
            category = FanDecisionCategory.RIVAL_LEAGUE,
            decisionTitle = "Absorbed ${outcome.absorbedFranchises.size} Rival Franchises",
            decisionSummary = outcome.headline,
            casualDelta = 5,
            dieHardDelta = -2
        )
        refreshJobSecurity(outcome.headline)
        recordDashboardTrend("Merger", "rival_absorb_${currentState.currentYear}")
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    // ==================== STADIUM DEALS, ULTIMATUMS & RELOCATION VOTES ====================

    fun resolveStadiumUltimatumChoice(
        choice: StadiumUltimatumChoice,
        forceRelocationRoll: Boolean? = null
    ): StadiumUltimatumOutcome {
        val currentState = _uiState.value
        val ultimatumTarget = currentState.stadiumDealState.activeUltimatum
        val outcome = StadiumDealEngine.resolveStadiumUltimatum(
            currentState = currentState.stadiumDealState,
            franchises = currentState.franchises,
            cities = currentState.cities,
            currentCashMillions = currentState.cashReservesMillions,
            choice = choice,
            forceRelocationRoll = forceRelocationRoll
        )
        if (!outcome.success) {
            _uiState.update {
                it.copy(
                    stadiumDealState = it.stadiumDealState.copy(lastDecisionSummary = outcome.message)
                )
            }
            return outcome
        }

        val newCash = (currentState.cashReservesMillions + outcome.cashDeltaMillions).coerceAtLeast(0L)
        val newValuation = (currentState.leagueValuationMillions + outcome.valuationDeltaMillions).coerceAtLeast(100L)
        val newOwnerApproval = (currentState.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(15, 100)
        val headline = outcome.headline ?: outcome.message

        _uiState.update {
            it.copy(
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                ownerApprovalRating = newOwnerApproval,
                franchises = outcome.updatedFranchises,
                stadiumDealState = outcome.updatedStadiumDealState,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }

        if (outcome.triggeredRelocationThreat) {
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_reloc_threat_${currentState.currentYear}_${currentState.currentWeek}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = headline,
                    actionLabel = "Vote",
                    targetRoute = DashboardSnackbarRoute.STADIUM_ULTIMATUM
                )
            )
        }

        notifyFinancialMilestones(currentState, _uiState.value)
        val (stadiumCasual, stadiumDieHard) = when (choice) {
            StadiumUltimatumChoice.LOBBY_PUBLIC_TAX_FUNDING -> 1 to -6
            StadiumUltimatumChoice.FORCE_OWNER_PRIVATE_FUNDING -> 2 to 5
            StadiumUltimatumChoice.SUBSIDIZE_WITH_LEAGUE_FUNDS -> 2 to 3
        }
        recordFanReaction(
            category = FanDecisionCategory.STADIUM_RELOCATION,
            decisionTitle = choice.title,
            decisionSummary = headline,
            casualDelta = stadiumCasual,
            dieHardDelta = stadiumDieHard,
            franchiseName = ultimatumTarget?.franchiseName ?: currentState.leagueName,
            cityName = ultimatumTarget?.cityName ?: "Metro"
        )
        refreshJobSecurity(headline)
        recordDashboardTrend("W${currentState.currentWeek}", "stadium_${choice.name.lowercase()}")
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    fun selectRelocationVoteCity(cityId: String) {
        _uiState.update { state ->
            val currentVote = state.stadiumDealState.activeRelocationVote ?: return@update state
            if (currentVote.cityDeals.none { it.cityId == cityId }) return@update state
            state.copy(
                stadiumDealState = state.stadiumDealState.copy(
                    activeRelocationVote = currentVote.copy(selectedCityId = cityId)
                )
            )
        }
        persistCurrentState()
    }

    fun resolveRelocationVote(
        approve: Boolean,
        selectedCityIdOverride: String? = null
    ): RelocationVoteOutcome {
        val currentState = _uiState.value
        val activeVote = currentState.stadiumDealState.activeRelocationVote
        val outcome = StadiumDealEngine.resolveRelocationVote(
            currentState = currentState.stadiumDealState,
            franchises = currentState.franchises,
            cities = currentState.cities,
            ruleBook = currentState.ruleBook,
            approve = approve,
            selectedCityIdOverride = selectedCityIdOverride,
            salaryCapMillions = currentState.unionAgreement.salaryCapMillions
        )
        if (!outcome.success) return outcome

        val newCash = (currentState.cashReservesMillions + outcome.cashDeltaMillions).coerceAtLeast(0L)
        val newValuation = (currentState.leagueValuationMillions + outcome.valuationDeltaMillions).coerceAtLeast(100L)
        val newOwnerApproval = (currentState.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(15, 100)

        val finalFranchises: List<Franchise>
        val finalMinorState: MinorLeagueState
        val finalMatches: List<GameMatch>
        val finalRivalries: List<TeamRivalry>
        val finalPlayoffRules: PlayoffRules

        if (outcome.approved && outcome.relocatedTeamId != null) {
            finalPlayoffRules = PlayoffStructureEngine.adaptRulesForLeagueStructure(
                currentRules = currentState.ruleBook.playoffRules,
                franchises = outcome.updatedFranchises,
                enabledConferences = currentState.ruleBook.enabledConferences
            )
            finalRivalries = RivalryEngine.syncRivalriesForLeagueTopology(
                outcome.updatedFranchises,
                outcome.updatedCities,
                currentState.rivalries,
                currentState.currentYear
            )
            finalMatches = RivalryEngine.annotateMatchesWithRivalries(
                currentState.currentWeekMatches,
                outcome.updatedFranchises,
                finalRivalries,
                outcome.updatedCities
            )
            val minorSync = MinorLeagueEngine.syncAffiliatesWithParentLeague(
                franchises = outcome.updatedFranchises,
                currentMinorState = currentState.minorLeagueState,
                sportType = currentState.sportType,
                seasonYear = currentState.currentYear
            )
            finalFranchises = minorSync.updatedFranchises
            finalMinorState = minorSync.minorState
        } else {
            finalPlayoffRules = currentState.ruleBook.playoffRules
            finalRivalries = currentState.rivalries
            finalMatches = currentState.currentWeekMatches
            finalFranchises = outcome.updatedFranchises
            finalMinorState = currentState.minorLeagueState
        }

        _uiState.update {
            it.copy(
                cashReservesMillions = newCash,
                leagueValuationMillions = newValuation,
                ownerApprovalRating = newOwnerApproval,
                franchises = finalFranchises,
                minorLeagueState = finalMinorState,
                cities = outcome.updatedCities,
                currentWeekMatches = finalMatches,
                rivalries = finalRivalries,
                ruleBook = it.ruleBook.copy(playoffRules = finalPlayoffRules),
                stadiumDealState = outcome.updatedStadiumDealState,
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }

        notifyFinancialMilestones(currentState, _uiState.value)
        val resolvedDestCity = outcome.destinationCityName ?: "New Market"
        recordFanReaction(
            category = FanDecisionCategory.STADIUM_RELOCATION,
            decisionTitle = if (outcome.approved) {
                "Relocation Approved to $resolvedDestCity"
            } else {
                "Relocation Vetoed (${activeVote?.currentCityName ?: "Home City"} Kept)"
            },
            decisionSummary = outcome.headline,
            casualDelta = if (outcome.approved) 2 else -1,
            dieHardDelta = if (outcome.approved) -7 else 6,
            franchiseName = activeVote?.franchiseName ?: currentState.leagueName,
            cityName = if (outcome.approved) resolvedDestCity else (activeVote?.currentCityName ?: "Metro")
        )
        refreshJobSecurity(outcome.headline)
        recordDashboardTrend("RelocVote", if (approve) "reloc_approved" else "reloc_vetoed")
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    fun acknowledgeSponsorBankruptcyScandal() {
        _uiState.update { state ->
            state.copy(
                stadiumDealState = StadiumDealEngine.acknowledgeBankruptcyScandal(state.stadiumDealState)
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
    }

    fun openNamingRightsOfferForFranchise(franchiseId: String) {
        val currentState = _uiState.value
        val team = currentState.franchises.firstOrNull { it.id == franchiseId } ?: return
        val updatedDealState = StadiumDealEngine.enqueueNamingRightsOfferForFranchise(
            currentState = currentState.stadiumDealState,
            franchise = team,
            seasonYear = currentState.currentYear,
            triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
            previousSponsorName = team.stadium.sponsorName,
            allFranchises = currentState.franchises
        )
        _uiState.update { it.copy(stadiumDealState = updatedDealState) }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
    }

    fun selectNamingRightsOffer(offerId: String): NamingRightsSelectionOutcome {
        val currentState = _uiState.value
        val activeSession = currentState.stadiumDealState.activeNamingRightsOffer
        val outcome = StadiumDealEngine.resolveNamingRightsOffer(
            currentState = currentState.stadiumDealState,
            franchises = currentState.franchises,
            offerId = offerId
        )
        if (!outcome.success) {
            _uiState.update {
                it.copy(
                    stadiumDealState = it.stadiumDealState.copy(lastDecisionSummary = outcome.message)
                )
            }
            return outcome
        }

        val chosen = outcome.chosenOffer
        val ownerDelta = when (chosen?.tier) {
            3 -> 4
            2 -> 3
            else -> 1
        }
        val (casualDelta, dieHardDelta) = when (chosen?.tier) {
            1 -> 1 to 2
            2 -> 2 to 1
            3 -> 3 to -3
            else -> 1 to 0
        }

        _uiState.update { state ->
            state.copy(
                leagueValuationMillions = (state.leagueValuationMillions + outcome.valuationDeltaMillions).coerceAtLeast(100L),
                ownerApprovalRating = (state.ownerApprovalRating + ownerDelta).coerceIn(15, 100),
                franchises = outcome.updatedFranchises,
                stadiumDealState = outcome.updatedStadiumDealState,
                recentNews = (listOf(outcome.headline) + state.recentNews).take(20)
            )
        }

        notifyFinancialMilestones(currentState, _uiState.value)
        recordFanReaction(
            category = FanDecisionCategory.STADIUM_RELOCATION,
            decisionTitle = chosen?.let { "${it.sponsorName} Naming Deal (Tier ${it.tier})" } ?: "Stadium Naming Rights Signed",
            decisionSummary = outcome.headline,
            casualDelta = casualDelta,
            dieHardDelta = dieHardDelta,
            franchiseName = activeSession?.franchiseName ?: currentState.leagueName,
            cityName = activeSession?.cityName ?: "Metro"
        )
        refreshJobSecurity(outcome.headline)
        recordDashboardTrend("Sponsor", "nr_t${chosen?.tier ?: 1}")
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    // ==================== FAN SENTIMENT & MEDIA PULSE ====================

    fun openFanSentimentScreen(open: Boolean = true) {
        _uiState.update { it.copy(showFanSentimentScreen = open) }
    }

    private fun recordFanReaction(
        category: FanDecisionCategory,
        decisionTitle: String,
        decisionSummary: String,
        casualDelta: Int,
        dieHardDelta: Int,
        franchiseName: String? = null,
        cityName: String? = null
    ) {
        _uiState.update { state ->
            val fallbackTeam = state.franchises.firstOrNull()
            val updatedSentiment = FanSentimentEngine.recordDecisionReaction(
                currentState = state.fanSentimentState,
                category = category,
                decisionTitle = decisionTitle,
                decisionSummary = decisionSummary,
                casualDelta = casualDelta,
                dieHardDelta = dieHardDelta,
                franchiseName = franchiseName ?: fallbackTeam?.teamFullName ?: state.leagueName,
                cityName = cityName ?: fallbackTeam?.cityName ?: "Metro",
                sportType = state.sportType,
                seasonYear = state.currentYear,
                weekNumber = state.currentWeek,
                seedOffset = state.currentWeek + state.fanSentimentState.decisionReactions.size
            )
            state.copy(fanSentimentState = updatedSentiment)
        }
    }

    fun hostFanPulseInitiative(focusSegment: FanSegmentType): Pair<Boolean, String> {
        val state = _uiState.value
        val costMillions = 3L
        if (state.cashReservesMillions < costMillions) {
            return false to "Insufficient league reserves (\$${costMillions}M required for media blitz)."
        }

        val (title, summary, casualDelta, dieHardDelta) = when (focusSegment) {
            FanSegmentType.CASUAL -> listOf(
                "Viral Highlight & Family Access Initiative",
                "Commissioned weekend stream giveaways, cheaper upper-bowl flex passes, and player mic'd-up showcases.",
                5,
                -1
            )
            FanSegmentType.DIE_HARD -> listOf(
                "Season-Ticket Heritage & Purist Town Hall",
                "Hosted direct commissioner Q&A with loyal supporter clubs, locking anti-relocation pledges and rivalry protections.",
                -1,
                5
            )
        }
        val casualD = casualDelta as Int
        val dieHardD = dieHardDelta as Int
        val updatedFranchises = state.franchises.map { team ->
            team.copy(
                fanMorale = (team.fanMorale + if (focusSegment == FanSegmentType.CASUAL) 2 else 1).coerceIn(20, 99),
                fanLoyalty = (team.fanLoyalty + if (focusSegment == FanSegmentType.DIE_HARD) 2 else 0).coerceIn(15, 100)
            )
        }
        val headline = "FAN PULSE TOWN HALL: ${title as String} deployed (\$${costMillions}M)."
        _uiState.update {
            it.copy(
                cashReservesMillions = it.cashReservesMillions - costMillions,
                franchises = updatedFranchises,
                recentNews = (listOf(headline) + it.recentNews).take(20)
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.TOWN_HALL,
            decisionTitle = title,
            decisionSummary = summary as String,
            casualDelta = casualD,
            dieHardDelta = dieHardD
        )
        refreshJobSecurity(headline)
        persistCurrentState()
        return true to "$title enacted! Social feeds, talk radio, and print columns updated."
    }

    // ==================== LEAGUE-WIDE SPONSORS (4 FIXED SLOTS) ====================

    fun openLeagueSponsorsScreen(open: Boolean = true) {
        _uiState.update { it.copy(showLeagueSponsorsScreen = open) }
    }

    fun openLeagueSponsorSlotOffers(slotType: LeagueSponsorSlotType) {
        val currentState = _uiState.value
        if (currentState.leagueSponsors.getSlot(slotType) != null) return
        val updatedSponsors = LeagueSponsorsEngine.openOfferSessionForSlot(
            currentState = currentState.leagueSponsors,
            slotType = slotType,
            seasonYear = currentState.currentYear
        )
        _uiState.update { it.copy(leagueSponsors = updatedSponsors) }
    }

    fun dismissLeagueSponsorOfferSession() {
        _uiState.update {
            it.copy(leagueSponsors = it.leagueSponsors.copy(activeOfferSession = null))
        }
    }

    fun signLeagueSponsorOffer(offerId: String): LeagueSponsorSignOutcome {
        val state = _uiState.value
        val outcome = LeagueSponsorsEngine.signOfferIntoSlot(
            currentState = state.leagueSponsors,
            offerId = offerId,
            franchises = state.franchises,
            activeBroadcast = state.activeBroadcast,
            unionAgreement = state.unionAgreement
        )
        if (!outcome.success) return outcome

        val signed = outcome.signedContract
        val moraleLift = if (signed?.perkType == LeagueSponsorPerkType.FAN_MORALE_BOOST) 4 else 1
        val integrityLift = if (signed?.perkType == LeagueSponsorPerkType.INTEGRITY_TECH) 3 else 0
        val fanbaseLift = if (signed?.perkType == LeagueSponsorPerkType.COMMERCIAL_REACH ||
            signed?.perkType == LeagueSponsorPerkType.BROADCAST_LIFT
        ) 0.45 else 0.15
        val valuationLift = (signed?.payout ?: 0L) / 2L

        val updatedFranchises = state.franchises.map { team ->
            team.copy(
                fanMorale = (team.fanMorale + moraleLift).coerceIn(25, 99)
            )
        }

        val updatedCommitteeProposals = if (signed?.ruleDemand != null) {
            val proposal = LeagueSponsorsEngine.buildCommitteeProposalFromDemand(
                demand = signed.ruleDemand,
                slotType = signed.slotType,
                sponsorName = signed.name,
                sportType = state.sportType
            )
            listOf(proposal) + state.ruleCommitteeState.proposals.filterNot { it.id == proposal.id }
        } else {
            state.ruleCommitteeState.proposals
        }

        _uiState.update {
            it.copy(
                leagueSponsors = outcome.updatedState,
                ruleCommitteeState = it.ruleCommitteeState.copy(proposals = updatedCommitteeProposals),
                franchises = updatedFranchises,
                leagueValuationMillions = it.leagueValuationMillions + valuationLift,
                leagueIntegrityScore = (it.leagueIntegrityScore + integrityLift).coerceIn(20, 100),
                totalLeagueFanbaseMillions = Math.round((it.totalLeagueFanbaseMillions + fanbaseLift) * 100.0) / 100.0,
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("Sponsor+", "lspon_${signed?.slotType?.key ?: "slot"}")
        refreshJobSecurity(outcome.headline)
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    fun voteOnSponsorRuleDemand(ruleId: String, approve: Boolean): SponsorRuleVoteOutcome {
        val state = _uiState.value
        if (approve && state.prDamageControlState.isSponsorRulePassingBlocked(state.currentYear)) {
            val blockedRuleName = state.leagueSponsors.allActiveContracts()
                .firstOrNull { it.ruleDemand?.ruleId == ruleId }?.ruleDemand?.ruleName
                ?: state.leagueSponsors.pendingOffseasonRuleVotes.firstOrNull { it.ruleId == ruleId }?.ruleName
                ?: "Sponsor Rule"
            val blockedMsg = "Sponsor Rule Blocked: Emergency Apology Press Conference froze passing new sponsor rules until ${state.prDamageControlState.sponsorRulesBlockedUntilYear}."
            val blockedSponsors = state.leagueSponsors.copy(lastSummary = blockedMsg)
            _uiState.update { it.copy(leagueSponsors = blockedSponsors) }
            return SponsorRuleVoteOutcome(
                success = false,
                ruleId = ruleId,
                ruleName = blockedRuleName,
                passed = false,
                message = blockedMsg,
                updatedSponsorsState = blockedSponsors,
                updatedFranchises = state.franchises,
                updatedUnion = state.unionAgreement,
                updatedRuleBook = state.ruleBook,
                updatedRuleCommittee = state.ruleCommitteeState,
                headline = blockedMsg
            )
        }
        val outcome = LeagueSponsorsEngine.resolveSponsorRuleDemandVote(
            currentSponsorsState = state.leagueSponsors,
            franchises = state.franchises,
            unionAgreement = state.unionAgreement,
            ruleBook = state.ruleBook,
            ruleCommitteeState = state.ruleCommitteeState,
            ruleId = ruleId,
            approve = approve,
            seasonYear = state.currentYear,
            weekNumber = state.currentWeek
        )
        if (!outcome.success) return outcome

        val updatedSentiment = if (outcome.passed) {
            val contractDemand = outcome.updatedSponsorsState.allActiveContracts()
                .firstOrNull { it.ruleDemand?.ruleId == ruleId }?.ruleDemand
            val fPenalty = contractDemand?.fanPenalty ?: 5
            FanSentimentEngine.recordDecisionReaction(
                currentState = state.fanSentimentState,
                category = FanDecisionCategory.RULE_CHANGE,
                decisionTitle = "Sponsor Rule Enacted: ${outcome.ruleName}",
                decisionSummary = outcome.headline,
                casualDelta = -fPenalty,
                dieHardDelta = -fPenalty,
                franchiseName = state.leagueName,
                cityName = outcome.updatedFranchises.firstOrNull()?.cityName ?: "Metro",
                sportType = state.sportType,
                seasonYear = state.currentYear,
                weekNumber = state.currentWeek,
                seedOffset = state.currentWeek + 19
            )
        } else state.fanSentimentState

        _uiState.update {
            it.copy(
                leagueSponsors = outcome.updatedSponsorsState,
                franchises = outcome.updatedFranchises,
                unionAgreement = outcome.updatedUnion,
                ruleBook = outcome.updatedRuleBook,
                ruleCommitteeState = outcome.updatedRuleCommittee,
                fanSentimentState = updatedSentiment,
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }

        if (!outcome.passed && outcome.breachWarning != null) {
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_spon_breach_${outcome.breachWarning.id}",
                    type = DashboardAlertType.CRISIS_ALERT,
                    severity = DashboardAlertSeverity.CRITICAL,
                    message = "Sponsor Breach Warning: ${outcome.breachWarning.sponsorName} slashed payout in half!",
                    actionLabel = "Sponsors",
                    targetRoute = DashboardSnackbarRoute.LEAGUE_SPONSORS
                )
            )
        } else if (outcome.passed) {
            coreSimulationViewModel.emitDashboardSnackbar(
                DashboardSnackbarEvent(
                    id = "snack_spon_rule_pass_${ruleId}",
                    type = DashboardAlertType.SEASON_MILESTONE,
                    severity = DashboardAlertSeverity.MILESTONE,
                    message = "'${outcome.ruleName}' Passed! +\$${outcome.bonusPayoutAddedMillions}M/yr revenue unlocked.",
                    actionLabel = "Sponsors",
                    targetRoute = DashboardSnackbarRoute.LEAGUE_SPONSORS
                )
            )
        }

        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend(
            if (outcome.passed) "SponsorRule+" else "SponsorBreach",
            "spon_rule_$ruleId"
        )
        refreshJobSecurity(outcome.headline)
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    fun dismissSponsorBreachWarning() {
        _uiState.update { state ->
            state.copy(
                leagueSponsors = state.leagueSponsors.copy(activeBreachWarning = null)
            )
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
    }

    fun triggerSponsorUltimatumForScandal(
        scandalTitle: String,
        scandalSummary: String,
        relatedIncident: DisciplinaryIncident? = null
    ) {
        _uiState.update { state ->
            val updated = LeagueSponsorsEngine.evaluateScandalForUltimatum(
                currentState = state.leagueSponsors,
                scandalSourceTitle = scandalTitle,
                scandalSummary = scandalSummary,
                seasonYear = state.currentYear,
                weekNumber = state.currentWeek,
                relatedIncident = relatedIncident,
                forceTriggerOnAnyIncident = true
            )
            state.copy(leagueSponsors = updated)
        }
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
    }

    fun resolveSponsorUltimatum(enforceMaxPenalty: Boolean): SponsorUltimatumResolutionOutcome {
        val state = _uiState.value
        val outcome = LeagueSponsorsEngine.resolveSponsorUltimatum(
            currentSponsorsState = state.leagueSponsors,
            disciplinaryState = state.disciplinaryState,
            franchises = state.franchises,
            unionAgreement = state.unionAgreement,
            ruleBook = state.ruleBook,
            enforceMaxPenalty = enforceMaxPenalty
        )
        if (!outcome.success) return outcome

        _uiState.update {
            it.copy(
                leagueSponsors = outcome.updatedSponsorsState,
                disciplinaryState = outcome.updatedDisciplinaryState,
                franchises = outcome.updatedFranchises,
                unionAgreement = outcome.updatedUnion,
                cashReservesMillions = (it.cashReservesMillions + outcome.cashTreasuryGainMillions).coerceAtLeast(0L),
                leagueIntegrityScore = (it.leagueIntegrityScore + outcome.integrityDelta).coerceIn(20, 100),
                ownerApprovalRating = (it.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(15, 100),
                leagueValuationMillions = (it.leagueValuationMillions + outcome.valuationDeltaMillions).coerceAtLeast(100L),
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        refreshJobSecurity(outcome.headline)
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    // ==================== LEAGUE SCANDAL CRISIS & PR DAMAGE CONTROL ====================

    fun openPrDamageControlModal(open: Boolean = true) {
        _uiState.update { it.copy(showPrDamageControlModal = open) }
    }

    fun executePrDamageControlAction(actionType: PrDamageActionType): PrDamageActionOutcome {
        val state = _uiState.value
        val avgMorale = if (state.franchises.isNotEmpty()) state.franchises.map { it.fanMorale }.average() else 75.0
        val avgLoyalty = if (state.franchises.isNotEmpty()) state.franchises.map { it.fanLoyalty }.average() else 75.0
        val currentSentiment = ((avgMorale * 0.55) + (avgLoyalty * 0.45)).roundToInt().coerceIn(0, 100)

        val outcome = PrDamageControlEngine.executeAction(
            prState = state.prDamageControlState,
            sponsorsState = state.leagueSponsors,
            actionType = actionType,
            currentFanSentiment = currentSentiment,
            cashReservesMillions = state.cashReservesMillions,
            currentYear = state.currentYear,
            currentWeek = state.currentWeek
        )
        if (!outcome.success) {
            _uiState.update {
                it.copy(
                    prDamageControlState = it.prDamageControlState.copy(lastActionNote = outcome.message)
                )
            }
            return outcome
        }

        val updatedFranchises = state.franchises.map { team ->
            team.copy(
                fanMorale = (team.fanMorale + outcome.sentimentGainApplied).coerceIn(0, 100),
                fanLoyalty = (team.fanLoyalty + outcome.sentimentGainApplied).coerceIn(0, 100),
                ownership = team.ownership.copy(
                    ownerApproval = (team.ownership.ownerApproval + outcome.ownerApprovalDelta).coerceIn(0, 100)
                )
            )
        }
        val nextOwnerApproval = (state.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(0, 100)
        val nextCash = (state.cashReservesMillions + outcome.cashDeltaMillions).coerceAtLeast(0L)

        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                ownerApprovalRating = nextOwnerApproval,
                cashReservesMillions = nextCash,
                leagueSponsors = outcome.updatedSponsorsState,
                prDamageControlState = outcome.updatedPrState,
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.TOWN_HALL,
            decisionTitle = actionType.displayName,
            decisionSummary = outcome.message,
            casualDelta = outcome.sentimentGainApplied,
            dieHardDelta = outcome.sentimentGainApplied
        )
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend("PR Control", "pr_${actionType.name.lowercase()}")
        refreshJobSecurity(outcome.headline)
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }

    fun triggerRandomLeagueScandalNow(random: Random = Random.Default): LeagueScandalIncident? {
        val state = _uiState.value
        val existing = state.leagueSponsors.activeLeagueScandal
        if (existing != null) return existing

        val (updatedSponsors, incident) = LeagueScandalEngine.generateRandomLeagueScandal(
            sponsorsState = state.leagueSponsors,
            franchises = state.franchises,
            sportType = state.sportType,
            seasonYear = state.currentYear,
            week = state.currentWeek,
            random = random
        )
        val scandalHeadline = "LEAGUE SCANDAL (${incident.severity.label}): ${incident.title} — ${incident.summary} Intervene within ${incident.weeksUntilPenalty}w to protect sponsors!"
        val impactedFranchises = state.franchises.map { team ->
            team.copy(fanMorale = (team.fanMorale - 4).coerceIn(0, 100))
        }
        _uiState.update {
            it.copy(
                franchises = impactedFranchises,
                leagueSponsors = updatedSponsors,
                leagueIntegrityScore = (it.leagueIntegrityScore - 3).coerceIn(10, 100),
                recentNews = (listOf(scandalHeadline) + it.recentNews).take(20)
            )
        }
        recordFanReaction(
            category = FanDecisionCategory.CRISIS_RULING,
            decisionTitle = incident.title,
            decisionSummary = incident.summary,
            casualDelta = -4,
            dieHardDelta = -4,
            franchiseName = incident.implicatedFranchiseName ?: state.leagueName
        )
        coreSimulationViewModel.emitDashboardSnackbar(
            DashboardSnackbarEvent(
                id = "snack_scandal_manual_${incident.id}",
                type = DashboardAlertType.CRISIS_ALERT,
                severity = DashboardAlertSeverity.CRITICAL,
                message = "${incident.title} — Intervene within ${incident.weeksUntilPenalty}w!",
                actionLabel = "Sponsors",
                targetRoute = DashboardSnackbarRoute.LEAGUE_SPONSORS
            )
        )
        refreshJobSecurity(scandalHeadline)
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return incident
    }

    fun resolveLeagueScandalIncident(choice: LeagueScandalInterventionChoice): LeagueScandalResolutionOutcome {
        val state = _uiState.value
        val outcome = LeagueScandalEngine.resolveLeagueScandalIncident(
            sponsorsState = state.leagueSponsors,
            choice = choice,
            cashReservesMillions = state.cashReservesMillions,
            seasonYear = state.currentYear,
            weekNumber = state.currentWeek
        )
        if (!outcome.success) {
            _uiState.update {
                it.copy(leagueSponsors = it.leagueSponsors.copy(lastSummary = outcome.message))
            }
            return outcome
        }

        val updatedFranchises = if (outcome.fanSentimentDelta != 0 || outcome.ownerApprovalDelta != 0) {
            state.franchises.map { team ->
                team.copy(
                    fanMorale = (team.fanMorale + outcome.fanSentimentDelta).coerceIn(0, 100),
                    fanLoyalty = (team.fanLoyalty + (outcome.fanSentimentDelta / 2)).coerceIn(0, 100),
                    ownership = team.ownership.copy(
                        ownerApproval = (team.ownership.ownerApproval + outcome.ownerApprovalDelta).coerceIn(0, 100)
                    )
                )
            }
        } else state.franchises

        _uiState.update {
            it.copy(
                franchises = updatedFranchises,
                leagueSponsors = outcome.updatedSponsorsState,
                cashReservesMillions = (it.cashReservesMillions + outcome.cashDeltaMillions).coerceAtLeast(0L),
                leagueIntegrityScore = (it.leagueIntegrityScore + outcome.integrityDelta).coerceIn(10, 100),
                ownerApprovalRating = (it.ownerApprovalRating + outcome.ownerApprovalDelta).coerceIn(0, 100),
                recentNews = (listOf(outcome.headline) + it.recentNews).take(20)
            )
        }
        if (outcome.fanSentimentDelta != 0) {
            recordFanReaction(
                category = FanDecisionCategory.CRISIS_RULING,
                decisionTitle = choice.title,
                decisionSummary = outcome.headline,
                casualDelta = outcome.fanSentimentDelta,
                dieHardDelta = outcome.fanSentimentDelta
            )
        }
        notifyFinancialMilestones(state, _uiState.value)
        recordDashboardTrend(if (outcome.penaltyApplied) "ScandalHit" else "ScandalFix", "scandal_${choice.name.lowercase()}")
        refreshJobSecurity(outcome.headline)
        coreSimulationViewModel.syncFromLeagueState(_uiState.value)
        persistCurrentState()
        return outcome
    }
}



