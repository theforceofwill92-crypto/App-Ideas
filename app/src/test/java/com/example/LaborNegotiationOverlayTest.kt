package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.LaborUnionNegotiationsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LaborNegotiationOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun laborNegotiationOverlayTriggersWithChoicesAndAppliesFinancialAndFanSentimentImpacts() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        // Escalate strike risk to trigger critical labor union negotiation event + overlay
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()

        assertNotNull("Critical strike risk must trigger negotiation overlay", vm.uiState.value.activeLaborNegotiationOverlay)

        // Open Labor Union Negotiations screen where the overlay renders on top
        vm.openLaborUnionFromTrigger()
        assertTrue(vm.uiState.value.showLaborUnionScreen)

        val preCash = vm.uiState.value.cashReservesMillions
        val preValuation = vm.uiState.value.leagueValuationMillions
        val preStrike = vm.uiState.value.unionAgreement.strikeRisk
        val preAvgMorale = vm.uiState.value.franchises.map { it.fanMorale }.average()
        val overlay = vm.uiState.value.activeLaborNegotiationOverlay!!
        val concessionChoice = overlay.choices.first()

        composeTestRule.setContent {
            MyApplicationTheme {
                val uiState = vm.uiState.collectAsStateWithLifecycle().value
                LaborUnionNegotiationsScreen(uiState = uiState, viewModel = vm)
            }
        }

        // Verify overlay, headline, baseline strip, and financial/fan sentiment impact chips render
        composeTestRule.onNodeWithTag("labor_negotiation_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_headline").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_baseline").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_choice_0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_financial_0", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_sentiment_0", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1. ${concessionChoice.title}").performScrollTo().assertIsDisplayed()

        // Select concession choice and verify financial, fan sentiment, and strike risk outcomes
        composeTestRule.onNodeWithTag("labor_overlay_choice_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val afterState = vm.uiState.value
        assertNull("Overlay should dismiss after choice resolution", afterState.activeLaborNegotiationOverlay)
        assertEquals(
            (preCash + concessionChoice.projectedCashImpactMillions).coerceAtLeast(0L),
            afterState.cashReservesMillions
        )
        assertEquals(
            (preValuation + concessionChoice.projectedValuationImpactMillions).coerceAtLeast(100L),
            afterState.leagueValuationMillions
        )
        assertTrue(
            "Concession choice should reduce strike risk",
            afterState.unionAgreement.strikeRisk < preStrike
        )
        val postAvgMorale = afterState.franchises.map { it.fanMorale }.average()
        assertTrue(
            "Concession choice should lift fan morale ($preAvgMorale -> $postAvgMorale)",
            postAvgMorale > preAvgMorale
        )
    }

    @Test
    fun laborNegotiationOverlayCanBeSummonedInsideUnionScreenAndDismissed() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        vm.openLaborUnionScreen(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                val uiState = vm.uiState.collectAsStateWithLifecycle().value
                LaborUnionNegotiationsScreen(uiState = uiState, viewModel = vm)
            }
        }

        // Summon overlay via the Union Table Ultimatum button inside LaborUnionNegotiationsScreen
        composeTestRule.onNodeWithTag("btn_open_labor_negotiation_overlay").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(vm.uiState.value.activeLaborNegotiationOverlay)
        composeTestRule.onNodeWithTag("labor_negotiation_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_choice_2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_financial_2", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_sentiment_2", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        // Dismiss overlay without mutating financials
        val cashBeforeDismiss = vm.uiState.value.cashReservesMillions
        composeTestRule.onNodeWithTag("btn_dismiss_labor_overlay").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNull(vm.uiState.value.activeLaborNegotiationOverlay)
        assertEquals(cashBeforeDismiss, vm.uiState.value.cashReservesMillions)
    }
}
