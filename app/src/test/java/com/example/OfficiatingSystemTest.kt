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
import com.example.model.OfficiatingEngine
import com.example.model.OfficiatingTechType
import com.example.ui.screens.OfficiatingHubScreen
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
class OfficiatingSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun officiatingAssignmentsUnionAiTechAndBlownCallBacklashEndToEnd() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        val initialState = vm.uiState.value
        assertTrue(initialState.officiatingState.referees.size >= 8)
        assertTrue(initialState.officiatingState.assignments.isNotEmpty())

        // 1. Referee Assignment & Load Management
        val firstMatch = initialState.officiatingState.assignments.first()
        val alternateRef = initialState.officiatingState.referees.last {
            it.id != firstMatch.leadRefereeId && !it.isRestedOrBenched
        }
        vm.assignRefereeToMatch(firstMatch.matchId, alternateRef.id)
        val reassigned = vm.uiState.value.officiatingState.assignments.first { it.matchId == firstMatch.matchId }
        assertEquals(alternateRef.id, reassigned.leadRefereeId)

        vm.autoOptimizeRefereeAssignments()
        vm.toggleRefereeRest(alternateRef.id)
        assertTrue(vm.uiState.value.officiatingState.referees.first { it.id == alternateRef.id }.isRestedOrBenched)
        vm.toggleRefereeRest(alternateRef.id)
        assertFalse(vm.uiState.value.officiatingState.referees.first { it.id == alternateRef.id }.isRestedOrBenched)

        // 2. Invest in Replay Technology & AI Officiating
        val cashBeforeTech = vm.uiState.value.cashReservesMillions
        val accuracyBeforeTech = vm.uiState.value.officiatingState.leagueCallAccuracyPct
        val unionSatBeforeTech = vm.uiState.value.officiatingState.unionState.unionSatisfaction

        val (hubOk, _) = vm.investInOfficiatingTech(OfficiatingTechType.CENTRAL_REPLAY_HUB.id)
        val (aiOk, _) = vm.investInOfficiatingTech(OfficiatingTechType.AI_FOUL_COPILOT.id)
        assertTrue(hubOk && aiOk)

        val afterTechState = vm.uiState.value
        assertEquals(2, afterTechState.officiatingState.installedTechCount)
        assertTrue(afterTechState.cashReservesMillions < cashBeforeTech)
        assertTrue(afterTechState.officiatingState.leagueCallAccuracyPct > accuracyBeforeTech)
        assertTrue(afterTechState.officiatingState.totalErrorReductionPct >= 30)
        assertTrue(afterTechState.officiatingState.unionState.unionSatisfaction < unionSatBeforeTech)
        assertTrue(afterTechState.ruleBook.instantReplayEnabled)

        // 3. Negotiate with Referee Union (NLOA)
        val lockoutRiskBeforeUnion = afterTechState.officiatingState.unionState.lockoutRisk
        val (raiseOk, _) = vm.negotiateRefUnionRaise(2L, 5)
        assertTrue(raiseOk)
        vm.toggleRefUnionDisciplineShield()
        val (cbaOk, _) = vm.ratifyNewRefereeCba()
        assertTrue(cbaOk)

        val postUnion = vm.uiState.value.officiatingState.unionState
        assertTrue(postUnion.publicDisciplineProtection)
        assertEquals(4, postUnion.cbaYearsRemaining)
        assertTrue(postUnion.lockoutRisk <= lockoutRiskBeforeUnion)

        // 4. Blown Call Controversy & Public Backlash Resolution
        val integrityBeforeScandal = vm.uiState.value.leagueIntegrityScore
        val incident = vm.triggerOfficiatingAuditControversy()
        assertFalse(incident.isResolved)
        assertTrue(vm.uiState.value.officiatingState.activeUnresolvedIncidents.isNotEmpty())
        assertTrue(vm.uiState.value.leagueIntegrityScore <= integrityBeforeScandal)

        val options = OfficiatingEngine.backlashOptionsForIncident(incident, vm.uiState.value.officiatingState)
        val disciplineChoice = options.first { it.benchesReferee }
        val (resolvedOk, _) = vm.resolveBlownCallIncident(incident.id, disciplineChoice)
        assertTrue(resolvedOk)

        val finalOff = vm.uiState.value.officiatingState
        assertTrue(finalOff.activeUnresolvedIncidents.isEmpty())
        assertTrue(finalOff.incidents.first { it.id == incident.id }.isResolved)
        val benchedRef = finalOff.referees.first { it.id == incident.offendingRefereeId }
        assertTrue(benchedRef.isRestedOrBenched && benchedRef.benchWeeksRemaining == 2)

        // 5. Persistence Round-Trip
        val repo = LeagueRepository(context)
        repo.saveOfficiatingState(finalOff)
        val restored = repo.loadOfficiatingState(
            franchises = vm.uiState.value.franchises,
            currentWeek = vm.uiState.value.currentWeek,
            matches = vm.uiState.value.currentWeekMatches
        )
        assertNotNull(restored)
        assertEquals(2, restored!!.installedTechCount)
        assertEquals(4, restored.unionState.cbaYearsRemaining)
        assertTrue(restored.incidents.any { it.id == incident.id && it.isResolved })
    }

    @Test
    fun officiatingHubScreenInteractiveTabsAndControls() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                OfficiatingHubScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("officiating_hub_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_officiating_telemetry").assertIsDisplayed()

        // Tab 0: Auto-Optimize referee crews
        composeTestRule.onNodeWithTag("btn_auto_optimize_refs").performClick()
        composeTestRule.waitForIdle()

        // Tab 1: Ref Union Bargaining
        composeTestRule.onNodeWithTag("tab_officiating_union").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_ref_union_status").assertIsDisplayed()
        val satBefore = vm.uiState.value.officiatingState.unionState.unionSatisfaction
        composeTestRule.onNodeWithTag("officiating_tab_list")
            .performScrollToNode(hasTestTag("btn_ref_union_raise"))
        composeTestRule.onNodeWithTag("btn_ref_union_raise").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.uiState.value.officiatingState.unionState.unionSatisfaction >= satBefore)

        // Tab 2: AI & Replay Investments
        composeTestRule.onNodeWithTag("tab_officiating_tech").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("officiating_tab_list")
            .performScrollToNode(hasTestTag("btn_invest_tech_tech_replay_hub"))
        composeTestRule.onNodeWithTag("btn_invest_tech_tech_replay_hub").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, vm.uiState.value.officiatingState.installedTechCount)

        // Tab 3: Blown Call Backlash Drill & Resolution
        composeTestRule.onNodeWithTag("tab_officiating_backlash").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_run_l2m_audit").performClick()
        composeTestRule.waitForIdle()

        val activeId = vm.uiState.value.officiatingState.activeUnresolvedIncidents.first().id
        composeTestRule.onNodeWithTag("officiating_tab_list")
            .performScrollToNode(hasTestTag("backlash_choice_${activeId}_resp_l2m_apology"))
        composeTestRule.onNodeWithTag("backlash_choice_${activeId}_resp_l2m_apology").performClick()
        composeTestRule.waitForIdle()

        assertTrue(vm.uiState.value.officiatingState.activeUnresolvedIncidents.isEmpty())
        composeTestRule.onNodeWithTag("officiating_tab_list")
            .performScrollToNode(hasTestTag("resolved_incident_${activeId}"))
        composeTestRule.onNodeWithTag("resolved_incident_${activeId}").assertIsDisplayed()
    }

    @Test
    fun homeScreenLaunchesOfficiatingHubAndReturnsCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen_content")
            .performScrollToNode(hasTestTag("btn_open_officiating_hub"))
        composeTestRule.onNodeWithTag("btn_open_officiating_hub").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("officiating_hub_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_back_officiating").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("main_scaffold").assertIsDisplayed()
    }
}
