package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.LaborUnionNegotiationsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun cbaExpiringLaborDisputeAndLockoutSuspendsPlayUntilAccordRatified() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        // Start from regular season Week 1 and force lockout via repeated hardline threats
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()
        vm.threatenManagementLockout()
        val lockoutOverlay = vm.uiState.value.activeLaborNegotiationOverlay
        assertNotNull(lockoutOverlay)

        // Pick the hardline choice that triggers lockout when strike risk >= 55
        val hardlineChoice = lockoutOverlay!!.choices.last()
        vm.resolveLaborNegotiationChoice(hardlineChoice.id)

        val lockedOut = vm.uiState.value
        assertTrue("Hardline rejection at high strike risk must activate lockout", lockedOut.unionAgreement.isLockoutActive)

        val weekBefore = lockedOut.currentWeek
        val winsSumBefore = lockedOut.franchises.sumOf { it.wins }
        vm.advanceWeek()
        val afterSuspendedWeek = vm.uiState.value
        assertEquals(weekBefore + 1, afterSuspendedWeek.currentWeek)
        assertEquals(1, afterSuspendedWeek.unionAgreement.lockoutWeeksLost)
        assertEquals("Games must be suspended during active lockout", winsSumBefore, afterSuspendedWeek.franchises.sumOf { it.wins })

        // Render LaborUnionNegotiationsScreen and verify Expiration Timer Badge, Lockout Banner, & Roster Rules Card
        vm.openLaborUnionScreen(true)
        composeTestRule.setContent {
            MyApplicationTheme {
                val uiState = vm.uiState.collectAsStateWithLifecycle().value
                LaborUnionNegotiationsScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cba_expiration_timer_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("banner_lockout_active").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_union_content").performScrollToNode(hasTestTag("card_cba_roster_rules"))
        composeTestRule.onNodeWithTag("card_cba_roster_rules").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_union_content").performScrollToNode(hasTestTag("btn_cba_roster_EXPANDED_18"))
        composeTestRule.onNodeWithTag("btn_cba_roster_EXPANDED_18").performClick()
        composeTestRule.waitForIdle()

        // Ratify comprehensive CBA -> lifts lockout and persists expanded roster rule
        vm.ratifyComprehensiveCBA(
            revenueSharePct = 51,
            salaryCapType = com.example.model.SalaryCapType.SOFT_CAP_LUXURY_TAX,
            salaryCapMillions = 92L,
            minSalaryK = 750L,
            playerHealthWelfareMillions = 18L,
            mandatoryPrivateCharters = true,
            backToBackLimits = true,
            concussionProtocol = true,
            hybridTurfMandate = true,
            transitionFundMillions = 6L,
            fiveStarHotels = true,
            rosterSizeRule = com.example.model.RosterSizeRule.EXPANDED_18
        )
        val afterAccord = vm.uiState.value
        assertFalse("Ratifying CBA must lift active lockout", afterAccord.unionAgreement.isLockoutActive)
        assertEquals(com.example.model.RosterSizeRule.EXPANDED_18, afterAccord.unionAgreement.negotiatedRosterRule)
        assertEquals(92L, afterAccord.unionAgreement.salaryCapMillions)
        assertTrue(afterAccord.unionAgreement.cbaYearsRemaining >= 3)
    }
}
