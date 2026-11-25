package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.data.LeagueState
import com.example.model.BroadcastContract
import com.example.model.CommissionerBackground
import com.example.model.LaborUnionAgreement
import com.example.model.SeasonPhase
import com.example.model.SportType
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomLeagueStateAndDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun roomLeagueStateDaoPersistsFinancesFranchiseCountAndLaborUnionStatus() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = db.leagueStateDao()
            val initialState = LeagueState(
                id = 1,
                leagueName = "Apex Test League",
                currentYear = 2027,
                currentWeek = 5,
                seasonPhase = SeasonPhase.REGULAR_SEASON.name,
                totalRevenueMillions = 248L,
                cashReservesMillions = 112L,
                leagueValuationMillions = 540L,
                annualBroadcastRevenueMillions = 120L,
                annualSponsorRevenueMillions = 86L,
                annualNamingRightsRevenueMillions = 42L,
                franchiseCount = 8,
                isLaborUnionActive = true,
                isLockoutActive = false,
                laborUnionStatus = "Active CBA Accord",
                cbaYearsRemaining = 3,
                unionSatisfaction = 74,
                strikeRisk = 18,
                salaryCapMillions = 145L
            )

            dao.upsertLeagueState(initialState)
            val loaded = dao.getLeagueState()
            assertNotNull(loaded)
            assertEquals(248L, loaded!!.totalRevenueMillions)
            assertEquals(112L, loaded.cashReservesMillions)
            assertEquals(8, loaded.franchiseCount)
            assertTrue(loaded.isLaborUnionActive)
            assertFalse(loaded.isLockoutActive)
            assertEquals("Active CBA Accord", loaded.laborUnionStatus)

            val updatedLockout = initialState.copy(
                franchiseCount = 10,
                isLaborUnionActive = false,
                isLockoutActive = true,
                laborUnionStatus = "Owner Lockout Active",
                strikeRisk = 92
            )
            dao.upsertLeagueState(updatedLockout)

            val emitted = dao.getLeagueStateFlow().first()
            assertNotNull(emitted)
            assertEquals(10, emitted!!.franchiseCount)
            assertFalse(emitted.isLaborUnionActive)
            assertTrue(emitted.isLockoutActive)
            assertEquals("Owner Lockout Active", emitted.laborUnionStatus)

            dao.deleteLeagueState()
            assertNull(dao.getLeagueState())
        } finally {
            db.close()
        }
    }

    @Test
    fun repositorySaveAndResetSynchronizesLeagueStateSnapshot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.resetLeague()

        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val broadcast = InitialLeagueData.getInitialBroadcastDeals().first()
        val union = LaborUnionAgreement(
            unionSatisfaction = 68,
            strikeRisk = 25,
            cbaYearsRemaining = 2,
            isLockoutActive = false,
            salaryCapMillions = 140L
        )

        repo.saveLeague(
            leagueName = "Persisted Premier League",
            sportType = SportType.BASKETBALL,
            commissionerBackground = CommissionerBackground.MARKETING_MAVEN,
            currentYear = 2026,
            currentWeek = 4,
            seasonPhase = SeasonPhase.REGULAR_SEASON,
            cashReservesMillions = 105L,
            leagueValuationMillions = 460L,
            leagueIntegrityScore = 88,
            competitiveParityScore = 84,
            unionAgreement = union,
            activeBroadcast = broadcast,
            franchises = franchises,
            cities = InitialLeagueData.getDefaultCities(),
            campaigns = InitialLeagueData.getMarketingCampaignCatalog(),
            history = emptyList(),
            newsFeed = listOf("Week 4 league report")
        )

        val persisted = repo.loadLeagueState()
        assertNotNull("Saving league must upsert LeagueState in Room", persisted)
        assertEquals("Persisted Premier League", persisted!!.leagueName)
        assertEquals(franchises.size, persisted.franchiseCount)
        assertTrue(persisted.isLaborUnionActive)
        assertFalse(persisted.isLockoutActive)
        assertEquals("Active CBA Accord", persisted.laborUnionStatus)
        assertTrue(persisted.totalRevenueMillions >= broadcast.annualPayoutMillions)

        repo.resetLeague()
        assertNull("Resetting league must clear LeagueState row", repo.loadLeagueState())
    }

    @Test
    fun mainDashboardDisplaysCoreLeagueStatsAndUpcomingDecisionsFromViewModel() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val uiState = vm.uiState.value
        val expectedClubs = "${uiState.franchises.size} Clubs"

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("main_dashboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_metric_total_revenue")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("M/yr", substring = true)

        composeTestRule.onNodeWithTag("dashboard_metric_franchise_count")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(expectedClubs, substring = true)

        composeTestRule.onNodeWithTag("dashboard_metric_labor_status")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Active CBA", substring = true)

        composeTestRule.onNodeWithTag("dashboard_upcoming_decisions_card")
            .performScrollTo()
            .assertIsDisplayed()

        // Escalate labor tension into critical state/lockout and verify dashboard reflects it
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()

        val afterThreat = vm.uiState.value
        if (afterThreat.unionAgreement.isLockoutActive) {
            composeTestRule.onNodeWithTag("dashboard_metric_labor_status")
                .performScrollTo()
                .assertTextContains("Lockout", substring = true)
            composeTestRule.onNodeWithTag("upcoming_decision_lockout_accord")
                .performScrollTo()
                .assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithTag("upcoming_decision_cba_expiration")
                .performScrollTo()
                .assertIsDisplayed()
        }

        // Clicking Franchise Count navigates to Franchises tab (1)
        composeTestRule.onNodeWithTag("dashboard_metric_franchise_count")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, vm.uiState.value.selectedTab)

        // Clicking Labor Union status opens Labor Union Negotiations screen
        composeTestRule.onNodeWithTag("dashboard_metric_labor_status")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.uiState.value.showLaborUnionScreen)
    }
}
