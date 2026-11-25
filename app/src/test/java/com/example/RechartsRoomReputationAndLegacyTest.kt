package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.data.LeagueRevenueHistoryEntity
import com.example.model.CommissionerBackground
import com.example.model.FreeAgencyEngine
import com.example.model.JobSecurityLegacyEngine
import com.example.model.LaborUnionAgreement
import com.example.model.LeagueLegacyMilestoneId
import com.example.model.LeagueLegacyTracker
import com.example.model.LeagueRuleBook
import com.example.model.LeagueValuationHistory
import com.example.model.SportType
import com.example.model.TeamReputationEngine
import com.example.model.TeamReputationTier
import com.example.ui.components.RechartsFiveYearRevenueChart
import com.example.ui.screens.FreeAgencyPoolScreen
import com.example.ui.screens.JobSecurityLegacyScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RechartsRoomReputationAndLegacyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun roomRevenueHistoryStoresLatestFiveYearsAndDashboardRendersFiveYearLineChart(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = db.leagueRevenueHistoryDao()
            val sevenYears = (2021..2027).mapIndexed { idx, year ->
                LeagueRevenueHistoryEntity(
                    seasonYear = year,
                    totalRevenueMillions = 160L + (idx * 25L),
                    broadcastRevenueMillions = 85L + (idx * 12L),
                    sponsorRevenueMillions = 55L + (idx * 8L),
                    namingRightsRevenueMillions = 20L + (idx * 5L),
                    valuationMillions = 420L + (idx * 70L),
                    franchiseCount = 8 + (idx / 3),
                    championName = "Apex Titans"
                )
            }
            dao.upsertAllAnnualRevenues(sevenYears)
            val latestFive = dao.getLastFiveYearsRevenue().sortedBy { it.seasonYear }
            assertEquals(5, latestFive.size)
            assertEquals(listOf(2023, 2024, 2025, 2026, 2027), latestFive.map { it.seasonYear })
            assertEquals(210L, latestFive.first().totalRevenueMillions)
            assertEquals(310L, latestFive.last().totalRevenueMillions)
        } finally {
            db.close()
        }

        // Verify Room-backed 5-year chart renders cleanly in MainDashboardScreen
        val app = context as Application
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("recharts_five_year_revenue_chart")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("five_year_revenue_growth_badge")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("five_year_revenue_headline")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("M / yr", substring = true)
        composeTestRule.onNodeWithTag("five_year_revenue_canvas")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("five_year_revenue_scrub_details")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun rechartsFiveYearRevenueChartCalculatesGrowthDeltaAcrossRoomRecords() {
        val sampleRecords = listOf(
            LeagueRevenueHistoryEntity(2023, 180L, 90L, 65L, 25L, 480L, 8, "Boston Celtics"),
            LeagueRevenueHistoryEntity(2024, 205L, 102L, 73L, 30L, 535L, 8, "Los Angeles Royals"),
            LeagueRevenueHistoryEntity(2025, 232L, 118L, 80L, 34L, 610L, 9, "Los Angeles Royals"),
            LeagueRevenueHistoryEntity(2026, 255L, 130L, 86L, 39L, 690L, 10, "Miami Heatwaves"),
            LeagueRevenueHistoryEntity(2027, 290L, 155L, 92L, 43L, 795L, 10, "Boston Celtics")
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                RechartsFiveYearRevenueChart(records = sampleRecords)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("recharts_five_year_revenue_chart").assertIsDisplayed()
        composeTestRule.onNodeWithTag("five_year_revenue_growth_badge")
            .assertTextContains("+61.1%", substring = true)
        composeTestRule.onNodeWithTag("five_year_revenue_headline")
            .assertTextContains("$290M / yr", substring = true)
        composeTestRule.onNodeWithTag("five_year_revenue_scrub_details")
            .assertTextContains("Y2027", substring = true)
            .assertTextContains("10 Clubs", substring = true)
    }

    @Test
    fun teamReputationPenalizesRelocationsAndLowSpendingAndImpactsFreeAgencyOdds() {
        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val capMillions = 140L
        val premierClub = franchises.first().copy(
            annualPayrollMillions = 138L,
            relocationCount = 0,
            lowSpendingSeasons = 0,
            ownership = franchises.first().ownership.copy(ownerApproval = 88)
        )
        val frugalRelocatedClub = franchises.last().copy(
            annualPayrollMillions = 68L, // 49% of salary cap (<72% threshold)
            relocationCount = 2,         // Frequent relocations
            lowSpendingSeasons = 2,
            ownership = franchises.last().ownership.copy(ownerApproval = 55)
        )

        val premierRep = TeamReputationEngine.evaluateReputation(premierClub, capMillions)
        val tarnishedRep = TeamReputationEngine.evaluateReputation(frugalRelocatedClub, capMillions)

        assertTrue("Premier club should outscore tarnished club", premierRep.score >= tarnishedRep.score + 35)
        assertTrue(tarnishedRep.relocationPenalty >= 34)
        assertTrue(tarnishedRep.lowSpendingPenalty >= 26)
        assertTrue(tarnishedRep.reputationSalaryPremiumMillions >= 4L)
        assertEquals(TeamReputationTier.AVOIDED_FRANCHISE, tarnishedRep.tier)

        val pool = FreeAgencyEngine.generateInitialFreeAgentPool(SportType.BASKETBALL, 2026)
        val marqueeAgent = pool.maxBy { it.overallRating }

        val premierOdds = TeamReputationEngine.evaluateFreeAgentSigningOdds(
            freeAgent = marqueeAgent,
            franchise = premierClub,
            salaryCapMillions = capMillions
        )
        val tarnishedOdds = TeamReputationEngine.evaluateFreeAgentSigningOdds(
            freeAgent = marqueeAgent,
            franchise = frugalRelocatedClub,
            salaryCapMillions = capMillions
        )

        assertTrue("Free agents must be far more likely to sign with respected clubs", premierOdds.likelihoodPct >= tarnishedOdds.likelihoodPct + 35)
        assertTrue("Avoided franchises must have depressed odds & refusal notes", tarnishedOdds.likelihoodPct < 36)
        assertNotNull(tarnishedOdds.refusalReason)

        val attemptFailed = FreeAgencyEngine.signFreeAgentToFranchise(
            franchise = frugalRelocatedClub,
            freeAgent = marqueeAgent,
            freeAgentPool = pool,
            ruleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules()),
            unionAgreement = LaborUnionAgreement(salaryCapMillions = capMillions),
            week = 3,
            seasonYear = 2026,
            sportType = SportType.BASKETBALL
        )
        assertFalse("Marquee FA should decline chronically under-spending/relocated club", attemptFailed.success)
        assertTrue(attemptFailed.message.contains("declined", ignoreCase = true))

        // Verify FreeAgencyPoolScreen surfaces Team Reputation strip & FA signing odds
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            MyApplicationTheme {
                FreeAgencyPoolScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("free_agency_pool_list")
            .performScrollToNode(hasTestTag("card_fa_team_reputation"))
        composeTestRule.onNodeWithTag("card_fa_team_reputation").assertIsDisplayed()

        val firstAgent = vm.uiState.value.freeAgentPool.first()
        composeTestRule.onNodeWithTag("free_agency_pool_list")
            .performScrollToNode(hasTestTag("fa_player_card_${firstAgent.id}"))
        composeTestRule.onNodeWithTag("fa_signing_odds_${firstAgent.id}").assertIsDisplayed()
    }

    @Test
    fun leagueLegacyTrackerUnlocksDynastyBuilderAndTvMogulPersistsAndRendersMilestones() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        // 1. Verify Dynasty Builder + TV Mogul milestone evaluation without double-counting
        val dynastyTeam = "Los Angeles Royals"
        val baseState = LeagueUiState(
            currentYear = 2027,
            activeBroadcast = InitialLeagueData.getInitialBroadcastDeals()
                .first()
                .copy(partnerName = "Prime Global Network", annualPayoutMillions = 165L),
            valuationHistory = listOf(
                LeagueValuationHistory(2026, 520L, 210L, dynastyTeam),
                LeagueValuationHistory(2027, 640L, 245L, dynastyTeam)
            )
        )
        val input = baseState.toPerformanceInput(seasonChampionNameOverride = dynastyTeam)
        val initialSec = JobSecurityLegacyEngine.createInitialState(LeagueUiState().toPerformanceInput())
        val eval = LeagueLegacyTracker.evaluateAndUnlockMilestones(initialSec, input)

        val unlockedIds = eval.newlyUnlockedMilestones.map { it.id }.toSet()
        assertTrue("Consecutive championships must unlock Dynasty Builder", unlockedIds.contains(LeagueLegacyMilestoneId.DYNASTY_BUILDER))
        assertTrue("$165M/yr broadcast deal must unlock TV Mogul", unlockedIds.contains(LeagueLegacyMilestoneId.TV_MOGUL))
        assertEquals(LeagueLegacyMilestoneId.DYNASTY_BUILDER.rewardLegacyPoints + LeagueLegacyMilestoneId.TV_MOGUL.rewardLegacyPoints, eval.bonusLegacyPointsAwarded)
        assertEquals(2, eval.updatedState.maxConsecutiveChampionStreak)
        assertEquals(165L, eval.updatedState.recordBroadcastAnnualMillions)

        // Re-evaluating the same unlocked state must not award points twice
        val secondEval = LeagueLegacyTracker.evaluateAndUnlockMilestones(eval.updatedState, input)
        assertEquals(0, secondEval.bonusLegacyPointsAwarded)
        assertTrue(secondEval.newlyUnlockedMilestones.isEmpty())

        // Persist & reload milestone state via Repository
        repo.saveJobSecurityLegacyState(eval.updatedState)
        val reloaded = repo.loadJobSecurityLegacyState(input)
        assertNotNull(reloaded)
        assertEquals(eval.updatedState.unlockedMilestonesCount, reloaded!!.unlockedMilestonesCount)
        assertTrue(reloaded.milestones.first { it.id == LeagueLegacyMilestoneId.DYNASTY_BUILDER }.isUnlocked)
        assertTrue(reloaded.milestones.first { it.id == LeagueLegacyMilestoneId.TV_MOGUL }.isUnlocked)

        // 2. Verify live ViewModel TV Mogul unlock + UI rendering in JobSecurityLegacyScreen Legacy tab
        val app = context as Application
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.createNewCustomLeague("Milestone Showcase League", SportType.BASKETBALL, CommissionerBackground.MARKETING_MAVEN)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val megaContract = vm.uiState.value.activeBroadcast.copy(
            id = "tv_mogul_offer_170m",
            partnerName = "Apex Stream Ultra",
            annualPayoutMillions = 170L,
            yearsRemaining = 4
        )
        vm.signNegotiatedBroadcastContract(megaContract, lumpSumBonusMillions = 20L)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val vmSec = vm.uiState.value.jobSecurityLegacyState
        assertTrue(vmSec.milestones.first { it.id == LeagueLegacyMilestoneId.TV_MOGUL }.isUnlocked)

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            MyApplicationTheme {
                JobSecurityLegacyScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tab_legacy_milestones").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("legacy_tracker_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_security_lazy_column")
            .performScrollToNode(hasTestTag("card_legacy_milestone_DYNASTY_BUILDER"))
        composeTestRule.onNodeWithTag("card_legacy_milestone_DYNASTY_BUILDER").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_security_lazy_column")
            .performScrollToNode(hasTestTag("card_legacy_milestone_TV_MOGUL"))
        composeTestRule.onNodeWithTag("card_legacy_milestone_TV_MOGUL")
            .assertIsDisplayed()
            .assertTextContains("UNLOCKED", substring = true)
    }
}
