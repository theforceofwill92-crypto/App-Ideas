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
import com.example.model.CommissionerBackground
import com.example.model.ExpansionApplicationStatus
import com.example.model.ExpansionRequestEngine
import com.example.model.SportType
import com.example.ui.screens.ExpansionRequestsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpansionRequestsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expansionRequestsImpactCalculationsAndApproveDeclineWorkflowUpdateLeagueMetrics() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val initialState = vm.uiState.value
        assertTrue(initialState.pendingExpansionRequests.isNotEmpty())
        val initialTeamCount = initialState.franchises.size
        val initialReach = initialState.totalLeagueFanbaseMillions
        val initialCash = initialState.cashReservesMillions
        val initialValuation = initialState.leagueValuationMillions
        val initialAnnualRevenue = ExpansionRequestEngine.estimateCurrentAnnualLeagueRevenueMillions(
            franchises = initialState.franchises,
            activeBroadcast = initialState.activeBroadcast,
            activeSponsorship = initialState.activeSponsorship
        )

        val firstReq = initialState.pendingExpansionRequests.first()
        assertTrue(firstReq.impact.projectedMarketReachGainMillions > 0.0)
        assertTrue(firstReq.impact.projectedAnnualRevenueGainMillions > 0L)
        assertEquals(
            Math.round((initialReach + firstReq.impact.projectedMarketReachGainMillions) * 10.0) / 10.0,
            firstReq.impact.projectedPostApprovalMarketReachMillions,
            0.05
        )

        // Approve the first expansion request directly
        val approved = vm.approveExpansionRequest(firstReq.id, launchDraftImmediately = false)
        assertTrue(approved)

        val afterApprove = vm.uiState.value
        assertEquals(initialTeamCount + 1, afterApprove.franchises.size)
        assertTrue(afterApprove.totalLeagueFanbaseMillions > initialReach)
        assertEquals(initialCash + firstReq.city.expansionFeeMillions, afterApprove.cashReservesMillions)
        assertEquals(initialValuation + (firstReq.city.expansionFeeMillions * 2L), afterApprove.leagueValuationMillions)

        val postAnnualRevenue = ExpansionRequestEngine.estimateCurrentAnnualLeagueRevenueMillions(
            franchises = afterApprove.franchises,
            activeBroadcast = afterApprove.activeBroadcast,
            activeSponsorship = afterApprove.activeSponsorship
        )
        assertTrue(postAnnualRevenue > initialAnnualRevenue)
        assertFalse(afterApprove.pendingExpansionRequests.any { it.id == firstReq.id })
        assertTrue(afterApprove.reviewedExpansionRequests.any {
            it.id == firstReq.id && it.status == ExpansionApplicationStatus.APPROVED
        })

        // Decline the next pending expansion request
        val secondReq = afterApprove.pendingExpansionRequests.first()
        val cashBeforeDecline = afterApprove.cashReservesMillions
        val reachBeforeDecline = afterApprove.totalLeagueFanbaseMillions
        val teamCountBeforeDecline = afterApprove.franchises.size

        val declined = vm.declineExpansionRequest(secondReq.id, reason = "Insufficient arena readiness")
        assertTrue(declined)

        val afterDecline = vm.uiState.value
        assertEquals(teamCountBeforeDecline, afterDecline.franchises.size)
        assertEquals(cashBeforeDecline, afterDecline.cashReservesMillions)
        assertEquals(reachBeforeDecline, afterDecline.totalLeagueFanbaseMillions, 0.01)
        assertFalse(afterDecline.pendingExpansionRequests.any { it.id == secondReq.id })
        assertTrue(afterDecline.reviewedExpansionRequests.any {
            it.id == secondReq.id && it.status == ExpansionApplicationStatus.DECLINED
        })

        // Reset league resets queue cleanly
        vm.createNewCustomLeague("Expansion Reset Test", SportType.BASKETBALL, CommissionerBackground.MARKETING_MAVEN)
        val resetState = vm.uiState.value
        assertTrue(resetState.reviewedExpansionRequests.isEmpty())
        assertTrue(resetState.pendingExpansionRequests.isNotEmpty())
    }

    @Test
    fun expansionRequestsScreenDisplaysImpactsAndHandlesApprovalAndDeclineActions() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val initialState = vm.uiState.value
        val req1 = initialState.pendingExpansionRequests[0]
        val req2 = initialState.pendingExpansionRequests[1]

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                ExpansionRequestsScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("expansion_requests_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("metric_current_market_reach", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("metric_current_league_revenue", useUnmergedTree = true).assertIsDisplayed()

        // Scroll list to ensure first request card and its projections are visible
        composeTestRule.onNodeWithTag("expansion_requests_list").performScrollToIndex(2)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("expansion_request_card_${req1.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reach_gain_${req1.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("Fans", substring = true)
        composeTestRule.onNodeWithTag("revenue_gain_${req1.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("/yr", substring = true)
        composeTestRule.onNodeWithTag("cba_summary_${req1.id}")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("CBA", substring = true)

        // Approve first request via UI button
        composeTestRule.onNodeWithTag("btn_approve_request_${req1.id}")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(initialState.franchises.size + 1, vm.uiState.value.franchises.size)
        composeTestRule.onNodeWithTag("expansion_requests_list").performScrollToIndex(1)
        composeTestRule.onNodeWithTag("banner_expansion_action_feedback").assertIsDisplayed()

        // Scroll to and decline second request via UI button
        composeTestRule.onNodeWithTag("expansion_requests_list").performScrollToIndex(3)
        composeTestRule.onNodeWithTag("btn_decline_request_${req2.id}")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(vm.uiState.value.pendingExpansionRequests.any { it.id == req2.id })

        // Switch to Reviewed tab and verify reviewed item appears
        composeTestRule.onNodeWithTag("expansion_requests_list").performScrollToIndex(2)
        composeTestRule.onNodeWithTag("chip_filter_reviewed")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("expansion_requests_list").performScrollToIndex(3)
        composeTestRule.onNodeWithTag("expansion_request_card_${req2.id}").assertIsDisplayed()
    }

    @Test
    fun marketsScreenNavigatesToExpansionRequestsScreenAndBack() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Markets tab
        composeTestRule.onNodeWithTag("nav_tab_markets").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_open_expansion_requests")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("expansion_requests_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_back_expansion_requests").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("markets_screen_content").assertIsDisplayed()
    }

    @Test
    fun buildExpansionArenaDeductsCashUpgradesMarketAndUpdatesCostBenefitAnalysis() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val stateBefore = vm.uiState.value
        val targetCity = stateBefore.cities.first { !it.hasFranchise && !it.hasBuiltExpansionArena }
        val cbaBefore = ExpansionRequestEngine.analyzeMarketCostBenefit(
            city = targetCity,
            franchises = stateBefore.franchises,
            activeBroadcast = stateBefore.activeBroadcast,
            activeSponsorship = stateBefore.activeSponsorship
        )
        assertFalse(cbaBefore.hasConstructedArena)
        assertTrue(cbaBefore.upfrontArenaBuildCostMillions > 0L)
        assertTrue(cbaBefore.projectedNetAnnualBenefitMillions > 0L)

        val tier = com.example.model.ExpansionArenaTier.PREMIER_ARENA
        val cashBefore = stateBefore.cashReservesMillions
        val (built, _) = vm.buildExpansionArena(targetCity.id, tier)
        assertTrue(built)

        val stateAfter = vm.uiState.value
        val updatedCity = stateAfter.cities.first { it.id == targetCity.id }
        assertEquals(cashBefore - tier.buildCostMillions, stateAfter.cashReservesMillions)
        assertTrue(updatedCity.hasBuiltExpansionArena)
        assertEquals(tier.capacity, updatedCity.arenaCapacity)
        assertEquals(targetCity.localSponsorDepthMillions + tier.sponsorBoostMillions, updatedCity.localSponsorDepthMillions)

        val cbaAfter = ExpansionRequestEngine.analyzeMarketCostBenefit(
            city = updatedCity,
            franchises = stateAfter.franchises,
            activeBroadcast = stateAfter.activeBroadcast,
            activeSponsorship = stateAfter.activeSponsorship
        )
        assertTrue(cbaAfter.hasConstructedArena)
        assertEquals(0L, cbaAfter.totalCapitalOutlayMillions)
        assertTrue(cbaAfter.projectedNetAnnualBenefitMillions >= cbaBefore.projectedNetAnnualBenefitMillions)
    }
}
