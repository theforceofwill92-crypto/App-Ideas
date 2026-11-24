package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.example.model.BroadcastContract
import com.example.model.BroadcastPlatform
import com.example.model.CommissionerBackground
import com.example.model.NetworkScope
import com.example.model.SportType
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardTrendsChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun viewModelTracksFinancialGrowthAndFanSentimentOverTimeAndResetsOnNewLeague() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val initialTrends = vm.leagueTrends.value
        assertTrue(initialTrends.isNotEmpty())
        val initialCount = initialTrends.size

        // Simulate two regular-season weeks
        vm.advanceWeek()
        vm.advanceWeek()

        val afterWeeks = vm.leagueTrends.value
        assertEquals(initialCount + 2, afterWeeks.size)
        assertEquals("W1", afterWeeks[afterWeeks.size - 2].periodLabel)
        assertEquals("W2", afterWeeks.last().periodLabel)

        // Sign broadcast deal to produce positive financial growth point
        val deal = BroadcastContract(
            id = "trend_test_tv",
            partnerName = "Apex Stream",
            platform = BroadcastPlatform.GLOBAL_STREAMER,
            annualPayoutMillions = 160L,
            yearsRemaining = 4,
            reachMultiplier = 1.9,
            networkScope = NetworkScope.GLOBAL_STREAMING
        )
        vm.signNegotiatedBroadcastContract(contract = deal, lumpSumBonusMillions = 50L)

        val afterDeal = vm.leagueTrends.value
        assertEquals(initialCount + 3, afterDeal.size)
        assertEquals("TV Deal", afterDeal.last().periodLabel)
        assertTrue(afterDeal.last().financialGrowthPct > 0f)
        assertTrue(afterDeal.last().fanSentimentPct in 0..100)

        // Reset via custom league creation
        vm.createNewCustomLeague(
            name = "Fresh Hockey League",
            sport = SportType.HOCKEY,
            background = CommissionerBackground.MARKETING_MAVEN
        )
        val resetTrends = vm.leagueTrends.value
        assertTrue(resetTrends.isNotEmpty())
        assertTrue(resetTrends.none { it.periodLabel == "TV Deal" })
    }

    @Test
    fun dashboardDisplaysGrowthAndSentimentLineChartAndUpdatesOnSimulation() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("recharts_growth_sentiment_line_chart")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("trend_growth_value", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("trend_sentiment_value", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("trend_line_canvas")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("trend_scrub_details")
            .performScrollTo()
            .assertIsDisplayed()

        // Interactive toggles remain responsive
        composeTestRule.onNodeWithTag("trend_toggle_finance")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("trend_toggle_finance")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Simulate a week from dashboard and verify chart tracks new W1 period
        composeTestRule.onNodeWithTag("main_dashboard_screen")
            .performScrollToIndex(0)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_simulate_week_button")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("main_dashboard_screen")
            .performScrollToIndex(1)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("trend_scrub_details")
            .performScrollTo()
            .assertTextContains("W1 •", substring = true)

        // Verify Recharts Executive Trends (Revenue, TV Ratings, Player Satisfaction) renders & toggles cleanly
        val latestPoint = vm.leagueTrends.value.last()
        assertTrue(latestPoint.leagueRevenueMillions > 0L)
        assertTrue(latestPoint.tvBroadcastRating > 0.0)
        assertTrue(latestPoint.playerSatisfactionPct in 0..100)

        composeTestRule.onNodeWithTag("recharts_executive_trends_chart")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("executive_trends_canvas")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("executive_trends_scrub_details")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("trend_toggle_revenue").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("trend_toggle_tv_ratings").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("trend_toggle_player_satisfaction").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }
}
