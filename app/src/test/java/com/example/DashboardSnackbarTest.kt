package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.model.BroadcastContract
import com.example.model.BroadcastPlatform
import com.example.model.NetworkScope
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardSnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun laborDisputeTriggersDashboardSnackbarAndResolveActionOpensOverlay() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Escalate strike tension until critical labor dispute triggers (12% -> 30% -> 48% -> 66%)
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_snackbar")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_snackbar_message")
            .assertIsDisplayed()
            .assertTextContains("Labor Dispute:", substring = true)

        // Dismiss first overlay if auto-opened, then tap Snackbar action to re-open overlay
        vm.dismissLaborNegotiationOverlay()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_snackbar_action")
            .assertIsDisplayed()
            .assertTextContains("Resolve")
            .performClick()
        composeTestRule.waitForIdle()

        assertNotNull(vm.uiState.value.activeLaborNegotiationOverlay)
    }

    @Test
    fun financialMilestoneTriggersDashboardSnackbarAndFinancesActionNavigatesToTab3() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        val landmarkContract = BroadcastContract(
            id = "test_landmark_broadcast",
            partnerName = "Global Prime Sports",
            platform = BroadcastPlatform.GLOBAL_STREAMER,
            annualPayoutMillions = 180L,
            yearsRemaining = 5,
            reachMultiplier = 2.0,
            networkScope = NetworkScope.GLOBAL_STREAMING
        )

        // Valuation moves from 420M -> 420 + 90 + 160 = 670M (surpassing $500M milestone)
        vm.signNegotiatedBroadcastContract(
            contract = landmarkContract,
            lumpSumBonusMillions = 80L
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_snackbar")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_snackbar_message")
            .assertIsDisplayed()
            .assertTextContains("Financial Milestone:", substring = true)

        composeTestRule.onNodeWithTag("dashboard_snackbar_action")
            .assertIsDisplayed()
            .assertTextContains("Finances")
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(3, vm.uiState.value.selectedTab)
    }

    @Test
    fun lowCashLiquidityAlertDisplaysAndDismissesCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        vm.coreSimulationViewModel.evaluateFinancialMilestonesAndEmit(
            previousValuation = 500L,
            newValuation = 500L,
            previousCash = 40L,
            newCash = 10L
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_snackbar")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_snackbar_message")
            .assertIsDisplayed()
            .assertTextContains("Treasury Alert:", substring = true)

        composeTestRule.onNodeWithTag("dashboard_snackbar_dismiss")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_snackbar")
            .assertDoesNotExist()
    }
}
