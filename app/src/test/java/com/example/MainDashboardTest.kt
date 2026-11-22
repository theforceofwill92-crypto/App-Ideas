package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
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
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainDashboardDisplaysMetricsAndUpdatesOnSimulate() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val initialState = vm.uiState.value
        val avgMorale = initialState.franchises.map { it.fanMorale }.average()
        val avgLoyalty = initialState.franchises.map { it.fanLoyalty }.average()
        val expectedSentiment = ((avgMorale * 0.55) + (avgLoyalty * 0.45)).roundToInt().coerceIn(0, 100)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }

        composeTestRule.onNodeWithTag("main_dashboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_metric_season_week").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_metric_bank_balance").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_metric_fan_sentiment").assertIsDisplayed()

        composeTestRule.onNodeWithText("Week ${initialState.currentWeek} of ${initialState.totalWeeks}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("\$${initialState.cashReservesMillions}M")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("$expectedSentiment%")
            .assertIsDisplayed()

        // Advance simulation via dashboard button and confirm reactive update from ViewModel
        composeTestRule.onNodeWithTag("dashboard_simulate_week_button").performClick()
        composeTestRule.waitForIdle()

        val updatedState = vm.uiState.value
        assertEquals(initialState.currentWeek + 1, updatedState.currentWeek)
        composeTestRule.onNodeWithText("Week ${updatedState.currentWeek} of ${updatedState.totalWeeks}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("\$${updatedState.cashReservesMillions}M")
            .assertIsDisplayed()
    }

    @Test
    fun mainDashboardMetricCardsNavigateToCorrespondingTabs() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }

        composeTestRule.onNodeWithTag("dashboard_metric_season_week").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(5, vm.uiState.value.selectedTab)

        composeTestRule.onNodeWithTag("dashboard_metric_bank_balance").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(3, vm.uiState.value.selectedTab)

        composeTestRule.onNodeWithTag("dashboard_metric_fan_sentiment").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, vm.uiState.value.selectedTab)

        composeTestRule.onNodeWithTag("metric_strike_risk").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.uiState.value.showLaborUnionScreen)
    }
}
