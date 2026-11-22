package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.*
import com.example.ui.screens.CrisisEventsScreen
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrisisNarrativeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allEightArchetypesBindDynamicSportTeamsPlayersAndEntities() {
        val cities = InitialLeagueData.getDefaultCities()
        val broadcast = InitialLeagueData.getInitialBroadcastDeals()[0]
        val sponsor = InitialLeagueData.getDefaultActiveSponsorship()
        val union = LaborUnionAgreement()

        SportType.values().forEach { sport ->
            val franchises = InitialLeagueData.getInitialFranchises(sport)
            val offState = OfficiatingEngine.createDefaultState(franchises, 1, emptyList())

            CrisisArchetype.values().forEach { archetype ->
                val (_, dilemma) = CrisisEngine.triggerFreshCrisis(
                    crisisState = CrisisSystemState(),
                    sportType = sport,
                    franchises = franchises,
                    activeBroadcast = broadcast,
                    activeSponsorship = sponsor,
                    cities = cities,
                    leagueIntegrityScore = 80,
                    unionAgreement = union,
                    officiatingState = offState,
                    seasonYear = 2026,
                    week = 3,
                    preferredArchetype = archetype,
                    random = Random(42)
                )
                assertNotNull("Expected Stage 1 dilemma for $sport / $archetype", dilemma)
                val node = dilemma!!
                assertEquals(archetype, node.archetype)
                assertEquals(1, node.stageNumber)
                assertFalse(node.isBranchFollowUp)
                assertTrue("Expected at least 2 choices for $archetype", node.choices.size >= 2)
                assertNotNull("Expected real target franchise for $archetype", node.targetFranchiseId)
                assertTrue(
                    "Target franchise should exist in league franchises",
                    franchises.any { it.id == node.targetFranchiseId && it.teamFullName == node.targetFranchiseName }
                )
            }
        }
    }

    @Test
    fun multiStageBranchingDivergesByDecisionAndCarriesIntoActTwoAndActThree() {
        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val cities = InitialLeagueData.getDefaultCities()
        val broadcast = InitialLeagueData.getInitialBroadcastDeals()[0]
        val sponsor = InitialLeagueData.getDefaultActiveSponsorship()
        val union = LaborUnionAgreement()
        val offState = OfficiatingEngine.createDefaultState(franchises, 1, emptyList())
        val discState = DisciplinaryEngine.createDefaultState(SportType.BASKETBALL, franchises)

        val (seededState, stage1) = CrisisEngine.triggerFreshCrisis(
            crisisState = CrisisSystemState(),
            sportType = SportType.BASKETBALL,
            franchises = franchises,
            activeBroadcast = broadcast,
            activeSponsorship = sponsor,
            cities = cities,
            leagueIntegrityScore = 82,
            unionAgreement = union,
            officiatingState = offState,
            seasonYear = 2026,
            week = 2,
            preferredArchetype = CrisisArchetype.SPORTSBOOK_INTEGRITY,
            random = Random(7)
        )
        assertNotNull(stage1)
        val s1 = stage1!!

        // Path A: Choice 0 (Suspend player 5G + Federal Audit -> arc_sportsbook_s2_audit in +2w)
        val pathAResult = CrisisEngine.applyChoiceConsequences(
            crisisState = seededState,
            dilemma = s1,
            choiceIndex = 0,
            franchises = franchises,
            unionAgreement = union,
            officiatingState = offState,
            disciplinaryState = discState,
            seasonYear = 2026,
            currentWeek = 2
        )
        assertEquals("arc_sportsbook_s2_audit", pathAResult.spawnedBranch?.followUpNodeId)
        assertEquals(4, pathAResult.spawnedBranch?.triggerWeek)
        assertTrue(pathAResult.integrityDelta > 0)
        val targetTeamA = pathAResult.updatedFranchises.first { it.id == s1.targetFranchiseId }
        val suspendedPlayer = targetTeamA.roster.first { it.id == s1.targetPlayerId }
        assertEquals(5, suspendedPlayer.suspendedGamesRemaining)

        // Path B: Choice 1 (Quiet fine & seal file -> arc_sportsbook_s2_leak)
        val pathBResult = CrisisEngine.applyChoiceConsequences(
            crisisState = seededState,
            dilemma = s1,
            choiceIndex = 1,
            franchises = franchises,
            unionAgreement = union,
            officiatingState = offState,
            disciplinaryState = discState,
            seasonYear = 2026,
            currentWeek = 2
        )
        assertEquals("arc_sportsbook_s2_leak", pathBResult.spawnedBranch?.followUpNodeId)
        assertTrue(pathBResult.integrityDelta < 0)
        assertTrue(pathBResult.ownerApprovalDelta > 0)
        val targetTeamB = pathBResult.updatedFranchises.first { it.id == s1.targetFranchiseId }
        assertEquals(0, targetTeamB.roster.first { it.id == s1.targetPlayerId }.suspendedGamesRemaining)

        // Advance Path A into Week 4 -> materializes Stage 2 (arc_sportsbook_s2_audit)
        val (postW4State, stage2) = CrisisEngine.buildCrisisForWeekOrFollowUp(
            crisisState = pathAResult.updatedCrisisState,
            sportType = SportType.BASKETBALL,
            franchises = pathAResult.updatedFranchises,
            activeBroadcast = broadcast,
            activeSponsorship = sponsor,
            cities = cities,
            leagueIntegrityScore = 82 + pathAResult.integrityDelta,
            unionAgreement = pathAResult.updatedUnion,
            officiatingState = pathAResult.updatedOfficiatingState,
            seasonYear = 2026,
            week = 4
        )
        assertNotNull(stage2)
        val s2 = stage2!!
        assertEquals("arc_sportsbook_s2_audit", s2.nodeId)
        assertEquals(2, s2.stageNumber)
        assertTrue(s2.isBranchFollowUp)
        assertEquals(s1.choices[0].label, s2.priorDecisionLabel)

        // Resolve Stage 2 Choice 0 -> spawns Stage 3 (arc_sportsbook_s3_reform) at Week 6
        val stage2Result = CrisisEngine.applyChoiceConsequences(
            crisisState = postW4State,
            dilemma = s2,
            choiceIndex = 0,
            franchises = pathAResult.updatedFranchises,
            unionAgreement = pathAResult.updatedUnion,
            officiatingState = pathAResult.updatedOfficiatingState,
            disciplinaryState = pathAResult.updatedDisciplinaryState,
            seasonYear = 2026,
            currentWeek = 4
        )
        assertEquals("arc_sportsbook_s3_reform", stage2Result.spawnedBranch?.followUpNodeId)

        val (postW6State, stage3) = CrisisEngine.buildCrisisForWeekOrFollowUp(
            crisisState = stage2Result.updatedCrisisState,
            sportType = SportType.BASKETBALL,
            franchises = stage2Result.updatedFranchises,
            activeBroadcast = broadcast,
            activeSponsorship = sponsor,
            cities = cities,
            leagueIntegrityScore = 90,
            unionAgreement = stage2Result.updatedUnion,
            officiatingState = stage2Result.updatedOfficiatingState,
            seasonYear = 2026,
            week = 6
        )
        assertNotNull(stage3)
        assertEquals(3, stage3!!.stageNumber)
        assertEquals("arc_sportsbook_s3_reform", stage3.nodeId)

        // Closing Stage 3 marks entire arc complete with no remaining scheduled branch for arcId
        val finalResult = CrisisEngine.applyChoiceConsequences(
            crisisState = postW6State,
            dilemma = stage3,
            choiceIndex = 0,
            franchises = stage2Result.updatedFranchises,
            unionAgreement = stage2Result.updatedUnion,
            officiatingState = stage2Result.updatedOfficiatingState,
            disciplinaryState = stage2Result.updatedDisciplinaryState,
            seasonYear = 2026,
            currentWeek = 6
        )
        assertNull(finalResult.spawnedBranch)
        assertTrue(s1.arcId in finalResult.updatedCrisisState.completedArcIds)
        assertEquals(3, finalResult.updatedCrisisState.resolvedHistory.size)
    }

    @Test
    fun crisisConsequencesMutateOfficiatingValuationAndRepositoryPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val initialValuation = vm.uiState.value.leagueValuationMillions
        val initialRefRisk = vm.uiState.value.officiatingState.unionState.lockoutRisk
        val initialRefAcc = vm.uiState.value.officiatingState.leagueCallAccuracyPct

        // Trigger Officiating & Venue Crew Sickout manually
        val sickout = vm.triggerCrisisScenarioNow(CrisisArchetype.LABOR_OFFICIAL_SICKOUT)
        assertNotNull(sickout)
        assertEquals(CrisisArchetype.LABOR_OFFICIAL_SICKOUT, vm.uiState.value.activeDilemma?.archetype)

        // Choice 1: Deploy replacement officials (+$2M cash, ref accuracy drops, lockout risk climbs)
        vm.resolveDilemma(1)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val after = vm.uiState.value
        assertNull(after.activeDilemma)
        assertTrue(after.officiatingState.unionState.lockoutRisk > initialRefRisk)
        assertTrue(after.officiatingState.leagueCallAccuracyPct < initialRefAcc)
        assertTrue(after.leagueValuationMillions < initialValuation)
        assertEquals(1, after.crisisState.scheduledBranches.size)
        assertEquals(1, after.crisisState.resolvedHistory.size)

        // Verify direct Repository persistence round-trip
        val repo = LeagueRepository(context)
        repo.saveCrisisState(after.crisisState)
        val loaded = repo.loadCrisisState()
        assertNotNull(loaded)
        assertEquals(after.crisisState.scheduledBranches.size, loaded!!.scheduledBranches.size)
        assertEquals(after.crisisState.resolvedHistory.size, loaded.resolvedHistory.size)
        assertEquals(
            after.crisisState.scheduledBranches.first().followUpNodeId,
            loaded.scheduledBranches.first().followUpNodeId
        )
        assertEquals(after.crisisState.lastTriggeredArcId, loaded.lastTriggeredArcId)
    }

    @Test
    fun crisisEventsScreenRendersInteractiveWorkflowAndLedger() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                val state by vm.uiState.collectAsState()
                CrisisEventsScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("crisis_events_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_trigger_branching_crisis").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Active DilemmaDialog appears with stage indicator & branching choices
        composeTestRule.onNodeWithTag("dilemma_dialog_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dilemma_stage_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dilemma_choice_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Branch should appear on Active & Branches tab
        val scheduled = vm.uiState.value.crisisState.scheduledBranches
        assertTrue(scheduled.isNotEmpty())
        val firstBranchTag = "card_scheduled_branch_${scheduled.first().followUpNodeId}"
        composeTestRule.onNodeWithTag("crisis_events_list").performScrollToNode(hasTestTag(firstBranchTag))
        composeTestRule.onNodeWithTag(firstBranchTag).assertIsDisplayed()

        // Switch to Consequence Ledger tab
        composeTestRule.onNodeWithTag("tab_crisis_ledger").performClick()
        composeTestRule.waitForIdle()
        val firstHistory = vm.uiState.value.crisisState.resolvedHistory.first()
        composeTestRule.onNodeWithTag("card_crisis_history_${firstHistory.id}").assertIsDisplayed()

        // Switch to Threat Radar tab
        composeTestRule.onNodeWithTag("tab_crisis_radar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_threat_radar_integrity").assertIsDisplayed()
    }

    @Test
    fun homeDeskHubLaunchesAndReturnsFromCrisisEventsScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen_content")
            .performScrollToNode(hasTestTag("btn_open_crisis_hub"))
        composeTestRule.onNodeWithTag("btn_open_crisis_hub").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("crisis_events_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_back_crisis_events").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("main_scaffold").assertIsDisplayed()
    }
}
