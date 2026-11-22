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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.CommissionerBackground
import com.example.model.LeagueRuleBook
import com.example.model.LeagueSimulationEngine
import com.example.model.RecordBookEngine
import com.example.model.RuleCommitteeEngine
import com.example.model.RuleCommitteeFocusLever
import com.example.model.SportRuleProposalStatus
import com.example.model.SportType
import com.example.ui.screens.RuleCommitteeScreen
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
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RuleCommitteeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allSportsProvideRealisticSportSpecificCommitteeProposals() {
        SportType.values().forEach { sport ->
            val state = RuleCommitteeEngine.createDefaultState(sport)
            assertEquals(sport, state.sportType)
            assertEquals(5, state.members.size)
            assertTrue("Expected at least 6 proposals for $sport", state.proposals.size >= 6)
            assertTrue(state.proposals.all { it.sportType == sport })
            assertTrue(state.proposals.all { it.simImpactTags.isNotEmpty() })
        }
    }

    @Test
    fun enactedSportSpecificRulesVisiblyAlterSimulationOutcomesAndBoxScores() {
        val baseFranchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val ruleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules())
        val baseMatches = LeagueSimulationEngine.generateMatchesForWeek(1, baseFranchises, SportType.BASKETBALL)

        // High-scoring pace rules (4-Point Arc + 14-Second Reset => +18.5 PPG/team)
        val highTempoState = RuleCommitteeEngine.createDefaultState(SportType.BASKETBALL).let { st ->
            st.copy(
                proposals = st.proposals.map { p ->
                    if (p.id == "rc_bball_4pt_arc" || p.id == "rc_bball_14s_reset") {
                        p.copy(status = SportRuleProposalStatus.ENACTED)
                    } else p
                }
            )
        }
        val highMods = RuleCommitteeEngine.computeSimulationModifiers(highTempoState, SportType.BASKETBALL)
        assertTrue(highMods.scoringDeltaPerTeam >= 18.0)

        // Defensive physicality rule (Hand-Checking => -10.5 PPG/team, 1.26x defense stats)
        val defenseGrindState = RuleCommitteeEngine.createDefaultState(SportType.BASKETBALL).let { st ->
            st.copy(
                proposals = st.proposals.map { p ->
                    if (p.id == "rc_bball_hand_check") {
                        p.copy(status = SportRuleProposalStatus.ENACTED)
                    } else p
                }
            )
        }
        val defMods = RuleCommitteeEngine.computeSimulationModifiers(defenseGrindState, SportType.BASKETBALL)
        assertTrue(defMods.scoringDeltaPerTeam <= -10.0)
        assertTrue(defMods.defenseStatMultiplier > 1.2)

        var totalHighScore = 0
        var totalDefScore = 0
        val sampleRounds = 12
        var lastHighPlayed = emptyList<com.example.model.GameMatch>()
        repeat(sampleRounds) {
            val highMutable = baseFranchises.toMutableList()
            val highPlayed = LeagueSimulationEngine.simulateRegularSeasonMatches(
                week = 1,
                sport = SportType.BASKETBALL,
                ruleBook = ruleBook,
                matchesToPlay = baseMatches,
                updatedFranchises = highMutable,
                rivalries = emptyList(),
                cities = emptyList(),
                committeeModifiers = highMods
            )
            lastHighPlayed = highPlayed
            totalHighScore += highPlayed.sumOf { it.homeScore + it.awayScore }

            val defMutable = baseFranchises.toMutableList()
            val defPlayed = LeagueSimulationEngine.simulateRegularSeasonMatches(
                week = 1,
                sport = SportType.BASKETBALL,
                ruleBook = ruleBook,
                matchesToPlay = baseMatches,
                updatedFranchises = defMutable,
                rivalries = emptyList(),
                cities = emptyList(),
                committeeModifiers = defMods
            )
            totalDefScore += defPlayed.sumOf { it.homeScore + it.awayScore }
        }

        val avgHighPerGame = totalHighScore.toDouble() / (sampleRounds * baseMatches.size)
        val avgDefPerGame = totalDefScore.toDouble() / (sampleRounds * baseMatches.size)
        assertTrue(
            "High tempo rules avg ($avgHighPerGame) should clearly exceed defensive hand-check rules avg ($avgDefPerGame)",
            avgHighPerGame - avgDefPerGame > 35.0
        )
        assertTrue(lastHighPlayed.all { it.ruleImpactNote.isNotBlank() })

        // Verify defenseStatMultiplier boosts defensive box score totals in RecordBookEngine
        val sampleMatch = lastHighPlayed.first()
        val home = baseFranchises[0]
        val away = baseFranchises[1]
        val baseDefTotal = (1..10).sumOf { seed ->
            RecordBookEngine.generateMatchPlayerStats(
                match = sampleMatch, home = home, away = away,
                homeScore = 105, awayScore = 100,
                sportType = SportType.BASKETBALL, seasonYear = 2026, isPlayoffs = false,
                random = Random(seed),
                defenseStatMultiplier = 1.0
            ).sumOf { it.stealsBlocks }
        }
        val boostedDefTotal = (1..10).sumOf { seed ->
            RecordBookEngine.generateMatchPlayerStats(
                match = sampleMatch, home = home, away = away,
                homeScore = 105, awayScore = 100,
                sportType = SportType.BASKETBALL, seasonYear = 2026, isPlayoffs = false,
                random = Random(seed),
                defenseStatMultiplier = 1.5
            ).sumOf { it.stealsBlocks }
        }
        assertTrue(
            "Defensive multiplier (1.5x) should increase total steals/blocks ($boostedDefTotal vs $baseDefTotal)",
            boostedDefTotal > baseDefTotal
        )
    }

    @Test
    fun viewModelWorkflowTelemetryAndPersistenceAcrossSports() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        // Switch league to Hockey and verify Rule Committee re-aligns to Hockey-specific proposals
        vm.createNewCustomLeague("Northern Ice League", SportType.HOCKEY, CommissionerBackground.MARKETING_MAVEN)
        assertEquals(SportType.HOCKEY, vm.uiState.value.ruleCommitteeState.sportType)
        assertTrue(vm.uiState.value.ruleCommitteeState.proposals.any { it.id == "rc_hock_full_pp" })

        val reviewed = vm.reviewProposalInCommittee("rc_hock_full_pp")
        assertNotNull(reviewed)
        assertTrue(reviewed!!.committeeVotesFor + reviewed.committeeVotesAgainst == 5)

        val (enactedOk, _) = vm.enactCommitteeProposal("rc_hock_full_pp")
        assertTrue(enactedOk)
        val (padOk, _) = vm.enactCommitteeProposal("rc_hock_smaller_pads")
        assertTrue(padOk)

        // Advance Week 1 and inspect recorded simulation impact log
        vm.advanceWeek()
        val postSim = vm.uiState.value
        assertEquals(1, postSim.ruleCommitteeState.impactLogs.size)
        val weekLog = postSim.ruleCommitteeState.impactLogs.first()
        assertEquals(2, weekLog.activeRulesCount)
        assertTrue(weekLog.activeRuleTitles.contains("Full 2-Minute Minor Power Play (Uninterrupted)"))
        assertTrue(postSim.currentWeekMatches.all { it.ruleImpactNote.isNotBlank() })

        // Verify persistence round-trip in LeagueRepository
        val repo = LeagueRepository(context)
        repo.saveRuleCommitteeState(postSim.ruleCommitteeState)
        val restored = repo.loadRuleCommitteeState(SportType.HOCKEY)
        assertNotNull(restored)
        assertEquals(2, restored!!.enactedProposals.size)
        assertEquals(1, restored.impactLogs.size)

        // Repeal one rule cleanly
        val (repealOk, _) = vm.repealCommitteeProposal("rc_hock_smaller_pads")
        assertTrue(repealOk)
        assertEquals(1, vm.uiState.value.ruleCommitteeState.enactedProposals.size)
    }

    @Test
    fun ruleCommitteeScreenInteractiveDocketDraftAndTelemetry() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                RuleCommitteeScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("rule_committee_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_committee_overview").assertIsDisplayed()

        // Review and enact Basketball 4-Point Arc proposal
        composeTestRule.onNodeWithTag("rule_committee_list")
            .performScrollToNode(hasTestTag("btn_review_proposal_rc_bball_4pt_arc"))
        composeTestRule.onNodeWithTag("rule_committee_list")
            .performTouchInput { swipeUp(startY = bottom * 0.7f, endY = bottom * 0.35f) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_review_proposal_rc_bball_4pt_arc").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("rule_committee_list")
            .performTouchInput { swipeUp(startY = bottom * 0.7f, endY = bottom * 0.45f) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_enact_proposal_rc_bball_4pt_arc").performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            vm.uiState.value.ruleCommitteeState.enactedProposals.any { it.id == "rc_bball_4pt_arc" }
        )

        // Switch to Draft Rule tab, create & immediately enact custom Player Safety rule
        composeTestRule.onNodeWithTag("tab_committee_draft").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("input_custom_rule_title").performTextInput("Zero-Contact Charge Circle")
        composeTestRule.onNodeWithTag("rule_committee_list")
            .performScrollToNode(hasTestTag("chip_lever_player_safety"))
        composeTestRule.onNodeWithTag("chip_lever_player_safety").performClick()
        composeTestRule.onNodeWithTag("rule_committee_list")
            .performScrollToNode(hasTestTag("btn_intensity_3"))
        composeTestRule.onNodeWithTag("btn_intensity_3").performClick()
        composeTestRule.onNodeWithTag("rule_committee_list")
            .performScrollToNode(hasTestTag("btn_enact_custom_proposal"))
        composeTestRule.onNodeWithTag("btn_enact_custom_proposal").performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            vm.uiState.value.ruleCommitteeState.enactedProposals.any {
                it.title == "Zero-Contact Charge Circle" && it.focusLever == RuleCommitteeFocusLever.PLAYER_SAFETY
            }
        )

        // Advance one simulated week and inspect Telemetry tab
        vm.advanceWeek()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_committee_telemetry").performClick()
        composeTestRule.waitForIdle()
        val latestLogId = vm.uiState.value.ruleCommitteeState.impactLogs.first().id
        composeTestRule.onNodeWithTag("rule_committee_list")
            .performScrollToNode(hasTestTag("card_sim_log_$latestLogId"))
        composeTestRule.onNodeWithTag("card_sim_log_$latestLogId").assertIsDisplayed()
    }
}
