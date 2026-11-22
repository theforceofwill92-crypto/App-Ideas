package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.example.model.BroadcastBidStatus
import com.example.model.CommissionerBackground
import com.example.model.SportType
import com.example.ui.screens.BroadcastNegotiationsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BroadcastBiddingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun broadcastBiddingEngineEscalatesOffersUpdatesImpactsAndSignsWinningDeal() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val initialSession = vm.uiState.value.broadcastBiddingSession
        assertEquals(5, initialSession.offers.size)
        assertEquals(1, initialSession.biddingRound)
        assertTrue(initialSession.offers.any { it.bidStatus == BroadcastBidStatus.LEADING_BID })

        val firstOffer = initialSession.offers.first()
        val initialCumulative = firstOffer.impact.cumulativeGrossValueMillions
        assertEquals(firstOffer.termYears, firstOffer.impact.yearlyProjections.size)

        // Run competitive bidding round -> escalates active bids and advances round counter
        val round2Session = vm.runBroadcastBiddingRound()
        assertEquals(2, round2Session.biddingRound)
        assertNotNull(round2Session.lastEscalationSummary)
        val escalatedFirstOffer = round2Session.offers.first { it.id == firstOffer.id }
        assertTrue(escalatedFirstOffer.impact.cumulativeGrossValueMillions > initialCumulative)

        // Counter-structure selected offer to 6-year term + higher upfront signing bonus
        val preAdjustTotal = escalatedFirstOffer.impact.cumulativeGrossValueMillions
        vm.updateBroadcastOfferStructure(
            offerId = escalatedFirstOffer.id,
            termYears = 6,
            lumpSumBonusMillions = escalatedFirstOffer.lumpSumBonusMillions + 10L,
            annualPayoutMillions = escalatedFirstOffer.annualPayoutMillions + 5L,
            currentPatience = escalatedFirstOffer.currentPatience,
            executiveNote = "Extended 6-year structure",
            markCountered = true
        )
        val adjustedOffer = vm.uiState.value.broadcastBiddingSession.offers.first { it.id == escalatedFirstOffer.id }
        assertEquals(6, adjustedOffer.termYears)
        assertEquals(6, adjustedOffer.impact.yearlyProjections.size)
        assertTrue(adjustedOffer.impact.cumulativeGrossValueMillions > preAdjustTotal)

        // Accept and sign the winning offer
        val cashBefore = vm.uiState.value.cashReservesMillions
        val valBefore = vm.uiState.value.leagueValuationMillions
        val fansBefore = vm.uiState.value.totalLeagueFanbaseMillions

        val signed = vm.acceptBroadcastOffer(adjustedOffer.id)
        assertTrue(signed)

        val afterSign = vm.uiState.value
        assertEquals(adjustedOffer.annualPayoutMillions, afterSign.activeBroadcast.annualPayoutMillions)
        assertEquals(adjustedOffer.termYears, afterSign.activeBroadcast.yearsRemaining)
        assertEquals(cashBefore + adjustedOffer.lumpSumBonusMillions, afterSign.cashReservesMillions)
        assertTrue(afterSign.leagueValuationMillions > valBefore)
        assertTrue(afterSign.totalLeagueFanbaseMillions > fansBefore)
        assertFalse(afterSign.showBroadcastNegotiationsScreen)

        // Verify custom league creation resets bidding round back to 1
        vm.createNewCustomLeague("Broadcast Test League", SportType.BASKETBALL, CommissionerBackground.MARKETING_MAVEN)
        assertEquals(1, vm.uiState.value.broadcastBiddingSession.biddingRound)
        assertEquals(5, vm.uiState.value.broadcastBiddingSession.offers.size)
    }

    @Test
    fun broadcastNegotiationsScreenDisplaysOffersBiddingRoundsAndLongTermImpact() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                BroadcastNegotiationsScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("broadcast_negotiations_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_broadcast_market_overview").assertIsDisplayed()

        // Run a competitive bidding round from UI
        composeTestRule.onNodeWithTag("btn_run_bidding_round")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, vm.uiState.value.broadcastBiddingSession.biddingRound)
        composeTestRule.onNodeWithTag("banner_bidding_round_summary").assertIsDisplayed()

        // Verify media network offer cards render and can be selected
        composeTestRule.onNodeWithTag("broadcast_negotiations_list").performScrollToIndex(2)
        composeTestRule.onNodeWithTag("bidder_card_bid_ansn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bidder_card_bid_omnistream")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("offer_bid_omnistream", vm.uiState.value.broadcastBiddingSession.selectedOfferId)

        // Apply tactical concession & verify long-term financial impact evaluator card + table
        composeTestRule.onNodeWithTag("broadcast_negotiations_list").performScrollToIndex(5)
        composeTestRule.onNodeWithTag("btn_tactic_exclusivity")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("broadcast_negotiations_list").performScrollToIndex(6)
        composeTestRule.onNodeWithTag("long_term_financial_impact_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("metric_total_contract_value")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("M", substring = true)
        composeTestRule.onNodeWithTag("metric_treasury_inflow")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("metric_valuation_reach_lift")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Fans", substring = true)
        composeTestRule.onNodeWithTag("metric_per_team_tv_share")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("yearly_projection_table")
            .performScrollTo()
            .assertIsDisplayed()

        // Accept and sign final contract
        composeTestRule.onNodeWithTag("broadcast_negotiations_list").performScrollToIndex(7)
        composeTestRule.onNodeWithTag("btn_sign_final_deal")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(vm.uiState.value.activeBroadcast.partnerName.contains("OmniStream"))
    }

    @Test
    fun financesScreenNavigatesToBroadcastWarRoomAndBack() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Finances tab -> Contracts & Deals sub-tab -> Open Bidding War Room
        composeTestRule.onNodeWithTag("nav_tab_finances").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tab_commercial_deals").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_enter_war_room")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("broadcast_negotiations_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_back_negotiations").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finances_screen_content").assertIsDisplayed()
    }
}
