package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.model.SalaryCapStrictness
import com.example.ui.screens.FinancesScreen
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
class FinancialOverviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun viewModelManagesLeagueRevenueSalaryCapsAndLuxuryTax() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val initialPrices = vm.uiState.value.franchises.map { it.id to it.ticketPrice }.toMap()
        val trendsBefore = vm.uiState.value.dashboardTrends.size

        // League-wide ticket surge (+$5/seat)
        vm.applyLeagueWideTicketPriceAdjustment(5)
        val afterSurge = vm.uiState.value
        afterSurge.franchises.forEach { team ->
            val prev = initialPrices.getValue(team.id)
            assertEquals((prev + 5).coerceIn(25, 220), team.ticketPrice)
        }
        assertTrue(afterSurge.recentNews.first().contains("LEAGUE GATE DIRECTIVE"))
        assertTrue(afterSurge.dashboardTrends.size >= trendsBefore)

        // League-wide gate relief (-$5/seat)
        vm.applyLeagueWideTicketPriceAdjustment(-5)
        vm.uiState.value.franchises.forEach { team ->
            assertEquals(initialPrices.getValue(team.id), team.ticketPrice)
        }

        // Salary Cap Strict Hard Cap blocks over-cap payroll increase
        val targetTeam = vm.uiState.value.franchises.first()
        vm.updateSalaryCapAmount(90L)
        vm.updateSalaryCapStrictness(SalaryCapStrictness.STRICT_HARD_CAP)

        val (hardBlocked, hardMsg) = vm.adjustFranchisePayrollDirective(targetTeam.id, 50L)
        assertFalse(hardBlocked)
        assertTrue(hardMsg.contains("Hard Cap Active"))

        // Lower cap and switch to Progressive Luxury Tax -> allows collecting luxury tax pool
        vm.updateSalaryCapStrictness(SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX)
        vm.updateSalaryCapAmount(60L)
        val cashBeforeTax = vm.uiState.value.cashReservesMillions
        val valBeforeTax = vm.uiState.value.leagueValuationMillions
        val snapshotsBefore = vm.uiState.value.financialSnapshots.size

        val (taxCollected, taxSummary) = vm.enforceSalaryCapComplianceAndCollectTax()
        assertTrue(taxCollected > 0L)
        assertTrue(taxSummary.contains("luxury tax"))
        assertEquals(cashBeforeTax + taxCollected, vm.uiState.value.cashReservesMillions)
        assertTrue(vm.uiState.value.leagueValuationMillions >= valBeforeTax)
        assertTrue(vm.uiState.value.financialSnapshots.size >= snapshotsBefore)
    }

    @Test
    fun financialOverviewScreenNavigatesAndControlsRevenueSalaryCapsAndBroadcastDeals() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        val firstTeam = vm.uiState.value.franchises.first()
        val initialTicketPrice = firstTeam.ticketPrice

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                FinancesScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Unified overview hero & pillars visible
        composeTestRule.onNodeWithTag("financial_overview_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overview_pillar_revenue").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overview_pillar_salary_cap").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overview_pillar_broadcast").assertIsDisplayed()

        // 2. League-wide ticket surge from Revenues tab
        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("card_league_revenue_controls"))
        composeTestRule.onNodeWithTag("card_league_revenue_controls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("btn_league_ticket_premium"))
        composeTestRule.onNodeWithTag("btn_league_ticket_premium").performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            (initialTicketPrice + 5).coerceIn(25, 220),
            vm.uiState.value.franchises.first { it.id == firstTeam.id }.ticketPrice
        )

        // Toggle small-market revenue sharing pool
        val sharingBefore = vm.uiState.value.ruleBook.isRuleEnacted("rule_revenue_sharing")
        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("switch_revenue_sharing"))
        composeTestRule.onNodeWithTag("switch_revenue_sharing").performClick()
        composeTestRule.waitForIdle()
        assertEquals(!sharingBefore, vm.uiState.value.ruleBook.isRuleEnacted("rule_revenue_sharing"))

        // 3. Scroll back to top hero & switch to Salary Caps tab
        composeTestRule.onNodeWithTag("finances_screen_content").performScrollToIndex(1)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("overview_pillar_salary_cap").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("card_salary_cap_controls"))
        composeTestRule.onNodeWithTag("card_salary_cap_controls").assertIsDisplayed()

        val capBefore = vm.uiState.value.unionAgreement.salaryCapMillions
        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("btn_cap_decrease"))
        composeTestRule.onNodeWithTag("btn_cap_decrease").performClick()
        composeTestRule.waitForIdle()
        assertEquals(capBefore - 5L, vm.uiState.value.unionAgreement.salaryCapMillions)

        // Switch cap mode to Progressive Luxury Tax and collect tax
        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("chip_cap_mode_PROGRESSIVE_LUXURY_TAX"))
        composeTestRule.onNodeWithTag("chip_cap_mode_PROGRESSIVE_LUXURY_TAX").performClick()
        composeTestRule.waitForIdle()

        // Lower cap enough so at least one team triggers luxury tax
        vm.updateSalaryCapAmount(60L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("btn_collect_luxury_tax"))
        composeTestRule.onNodeWithTag("btn_collect_luxury_tax").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("banner_cap_tax_feedback"))
        composeTestRule.onNodeWithTag("banner_cap_tax_feedback")
            .assertIsDisplayed()
            .assertTextContains("luxury tax", substring = true)

        // Adjust team payroll from Salary Cap Ledger card
        val payrollBefore = vm.uiState.value.franchises.first { it.id == firstTeam.id }.annualPayrollMillions
        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("btn_expand_payroll_${firstTeam.id}"))
        composeTestRule.onNodeWithTag("btn_expand_payroll_${firstTeam.id}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            (payrollBefore + 5L).coerceIn(35L, 220L),
            vm.uiState.value.franchises.first { it.id == firstTeam.id }.annualPayrollMillions
        )

        // 4. Scroll back to top hero, switch to Broadcast tab & open Bidding War Room
        composeTestRule.onNodeWithTag("finances_screen_content").performScrollToIndex(1)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("overview_pillar_broadcast").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("card_active_broadcast_spotlight"))
        composeTestRule.onNodeWithTag("card_active_broadcast_spotlight").assertIsDisplayed()

        composeTestRule.onNodeWithTag("finances_screen_content")
            .performScrollToNode(hasTestTag("btn_enter_war_room"))
        composeTestRule.onNodeWithTag("btn_enter_war_room").performClick()
        composeTestRule.waitForIdle()

        assertTrue(vm.uiState.value.showBroadcastNegotiationsScreen)
    }
}
