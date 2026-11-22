package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.LeagueRepository
import com.example.model.AllStarEngine
import com.example.model.AllStarFormatType
import com.example.model.SeasonPhase
import com.example.ui.screens.AllStarRedesignScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllStarRedesignTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allStarRedesignEngineCombatsDecliningViewershipAndPlayerApathyAndExecutesWeek7() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val baselineState = vm.uiState.value
        val legacyImpact = AllStarEngine.computeProjectedImpact(
            redesignState = baselineState.allStarRedesignState,
            franchises = baselineState.franchises,
            activeBroadcast = baselineState.activeBroadcast,
            cities = baselineState.cities,
            ruleBook = baselineState.ruleBook
        )
        assertEquals(AllStarFormatType.CLASSIC_CONFERENCE, baselineState.allStarRedesignState.selectedFormat)
        assertTrue(legacyImpact.playerApathyRiskPct >= 50)

        // Redesign format + incentives + side event + international host city
        vm.selectAllStarFormat(AllStarFormatType.FOUR_NATIONS_CUP)
        vm.toggleAllStarIncentive("inc_elam_target")
        vm.toggleAllStarIncentive("inc_prize_pool")
        vm.toggleAllStarSideEvent("ev_1v1_king")
        val intlCity = vm.uiState.value.cities.firstOrNull { it.isInternational } ?: vm.uiState.value.cities.first()
        vm.selectAllStarHostCity(intlCity.id)

        val promotedImpact = vm.ratifyAndPromoteAllStarRedesign()
        assertTrue(vm.uiState.value.allStarRedesignState.isRedesignedAndPromoted)
        assertTrue(promotedImpact.projectedViewersMillions > legacyImpact.projectedViewersMillions)
        assertTrue(promotedImpact.playerEffortScore > legacyImpact.playerEffortScore + 25)
        assertTrue(promotedImpact.playerApathyRiskPct < legacyImpact.playerApathyRiskPct)
        assertTrue(promotedImpact.projectedNetRevenueMillions > legacyImpact.projectedNetRevenueMillions)

        // Advance through Week 7 (ALL_STAR_INTERNATIONAL) to execute real redesigned showcase
        var guard = 0
        while (vm.uiState.value.seasonPhase != SeasonPhase.ALL_STAR_INTERNATIONAL && guard < 12) {
            if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
            if (vm.uiState.value.activeLaborNegotiationOverlay != null) vm.dismissLaborNegotiationOverlay()
            vm.advanceWeek()
            guard++
        }
        assertEquals(SeasonPhase.ALL_STAR_INTERNATIONAL, vm.uiState.value.seasonPhase)

        val cashBeforeShowcase = vm.uiState.value.cashReservesMillions
        val fanbaseBeforeShowcase = vm.uiState.value.totalLeagueFanbaseMillions

        val recap = vm.hostLiveAllStarShowcase()
        assertEquals(AllStarFormatType.FOUR_NATIONS_CUP, recap.formatType)
        assertEquals(intlCity.name, recap.hostCityName)
        assertTrue(recap.mvpPlayerName.isNotBlank())
        assertTrue(recap.playerEffortScore >= 80)
        assertTrue(vm.uiState.value.cashReservesMillions > cashBeforeShowcase)
        assertTrue(vm.uiState.value.totalLeagueFanbaseMillions > fanbaseBeforeShowcase)
        assertNotNull(vm.uiState.value.allStarRedesignState.lastRecap)

        // Repository persistence round-trip
        val repo = LeagueRepository(context)
        repo.saveAllStarRedesignState(vm.uiState.value.allStarRedesignState)
        val restored = repo.loadAllStarRedesignState(vm.uiState.value.cities)
        assertNotNull(restored)
        assertEquals(AllStarFormatType.FOUR_NATIONS_CUP, restored!!.selectedFormat)
        assertEquals(intlCity.id, restored.hostCityId)
        assertEquals(1, restored.recapHistory.size)
    }

    @Test
    fun allStarRedesignScreenInteractiveControlsAndHomeNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                AllStarRedesignScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("all_star_redesign_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_all_star_telemetry").assertIsDisplayed()
        composeTestRule.onNodeWithTag("banner_apathy_diagnosis").assertIsDisplayed()

        // Switch to USA vs World format
        composeTestRule.onNodeWithTag("all_star_redesign_list")
            .performScrollToNode(hasTestTag("format_card_USA_VS_WORLD"))
        composeTestRule.onNodeWithTag("format_card_USA_VS_WORLD").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AllStarFormatType.USA_VS_WORLD, vm.uiState.value.allStarRedesignState.selectedFormat)

        // Enable Target-Score Elam Ending & 1v1 Bracket
        composeTestRule.onNodeWithTag("all_star_redesign_list")
            .performScrollToNode(hasTestTag("incentive_switch_inc_elam_target"))
        composeTestRule.onNodeWithTag("incentive_switch_inc_elam_target").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.uiState.value.allStarRedesignState.incentives.first { it.id == "inc_elam_target" }.isEnabled)

        composeTestRule.onNodeWithTag("all_star_redesign_list")
            .performScrollToNode(hasTestTag("event_card_ev_1v1_king"))
        composeTestRule.onNodeWithTag("event_card_ev_1v1_king").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.uiState.value.allStarRedesignState.sideEvents.first { it.id == "ev_1v1_king" }.isSelected)

        // Ratify redesign and run showcase simulation
        composeTestRule.onNodeWithTag("all_star_redesign_list")
            .performScrollToNode(hasTestTag("btn_ratify_all_star_format"))
        composeTestRule.onNodeWithTag("btn_ratify_all_star_format").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.uiState.value.allStarRedesignState.isRedesignedAndPromoted)

        composeTestRule.onNodeWithTag("btn_host_all_star_showcase").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("all_star_redesign_list")
            .performScrollToNode(hasTestTag("card_all_star_recap"))
        composeTestRule.onNodeWithTag("card_all_star_recap").assertIsDisplayed()
    }

    @Test
    fun homeScreenLaunchesAllStarRedesignHubAndReturnsCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen_content")
            .performScrollToNode(hasTestTag("btn_open_all_star_hub"))
        composeTestRule.onNodeWithTag("btn_open_all_star_hub").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("all_star_redesign_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_back_all_star").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("main_scaffold").assertIsDisplayed()
    }
}
