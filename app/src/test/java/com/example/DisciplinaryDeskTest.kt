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
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.*
import com.example.ui.screens.DisciplinaryDeskScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class DisciplinaryDeskTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allSportsSeedRealisticInfractionsAndCharterCalibratedPrecedents() {
        val zeroToleranceCharter = LeagueRuleBook(
            rules = InitialLeagueData.getDefaultLeagueRules(),
            antiDopingPolicy = PolicyStrictness.ZERO_TOLERANCE,
            antiTamperingGamblingPolicy = PolicyStrictness.ZERO_TOLERANCE
        )

        SportType.values().forEach { sport ->
            val franchises = InitialLeagueData.getInitialFranchises(sport)
            val state = DisciplinaryEngine.createDefaultState(
                sportType = sport,
                franchises = franchises,
                ruleBook = zeroToleranceCharter
            )
            assertEquals(sport, state.sportType)
            assertEquals(DisciplinaryCategory.values().size, state.precedents.size)
            assertTrue("Expected pending cases seeded for $sport", state.pendingCases.size >= 3)
            assertTrue(state.pendingCases.all { it.sportType == sport })

            val subStd = state.standardFor(DisciplinaryCategory.SUBSTANCE_POLICY)
            val gamStd = state.standardFor(DisciplinaryCategory.GAMBLING_INTEGRITY)
            assertEquals(PrecedentPosture.ZERO_TOLERANCE, subStd.posture)
            assertEquals(PrecedentPosture.ZERO_TOLERANCE, gamStd.posture)
            assertTrue(subStd.benchmarkGames > DisciplinaryCategory.SUBSTANCE_POLICY.defaultGames)
            assertTrue(gamStd.benchmarkFineThousands > DisciplinaryCategory.GAMBLING_INTEGRITY.defaultFineThousands)
        }
    }

    @Test
    fun alignedVsInconsistentRulingsAndLandmarkPrecedentUpdate() {
        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val ruleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules())
        val union = LaborUnionAgreement()
        val state = DisciplinaryEngine.createDefaultState(SportType.BASKETBALL, franchises, ruleBook)
        val target = state.pendingCases.first()

        val std = state.standardFor(target.category)
        val (expGames, expFine) = std.expectedPenaltyFor(target.severity, target.priorOffensesCount)

        // 1. Aligned ruling boosts consistency and has low appeal risk
        val alignedEval = DisciplinaryEngine.evaluateProposedRuling(
            incident = target,
            proposedGames = expGames,
            proposedFineThousands = expFine,
            setNewPrecedent = false,
            state = state,
            unionAgreement = union,
            ruleBook = ruleBook
        )
        assertEquals(PrecedentAlignmentTier.ALIGNED, alignedEval.alignmentTier)
        assertTrue(alignedEval.consistencyDelta > 0)
        assertTrue(alignedEval.appealRiskPct < 40)

        // 2. Arbitrary break without landmark update drops consistency & spikes appeal risk
        val excessiveGames = (expGames + 5).coerceAtMost(target.category.maxGames)
        val arbitraryEval = DisciplinaryEngine.evaluateProposedRuling(
            incident = target,
            proposedGames = excessiveGames,
            proposedFineThousands = (expFine * 2).coerceAtMost(target.category.maxFineThousands),
            setNewPrecedent = false,
            state = state,
            unionAgreement = union,
            ruleBook = ruleBook
        )
        assertEquals(PrecedentAlignmentTier.ARBITRARY_BREAK, arbitraryEval.alignmentTier)
        assertTrue(arbitraryEval.consistencyDelta < 0)
        assertTrue(arbitraryEval.appealRiskPct >= 50)

        // 3. Establishing a new landmark precedent updates the category standard & increments count
        val strictGames = (expGames + 3).coerceAtMost(target.category.maxGames)
        val landmarkOutcome = DisciplinaryEngine.adjudicateIncident(
            state = state,
            franchises = franchises,
            unionAgreement = union,
            ruleBook = ruleBook,
            incidentId = target.id,
            suspensionGames = strictGames,
            fineThousands = (expFine + 40).coerceAtMost(target.category.maxFineThousands),
            setNewPrecedent = true,
            commissionerNote = "Establishing zero-tolerance airborne safety precedent.",
            random = Random(99)
        )
        assertEquals(1, landmarkOutcome.updatedState.landmarkPrecedentsCount)
        val updatedStd = landmarkOutcome.updatedState.standardFor(target.category)
        assertTrue(
            "Expected landmark standard benchmarkGames (${updatedStd.benchmarkGames}) >= baseline (${std.benchmarkGames})",
            updatedStd.benchmarkGames >= std.benchmarkGames
        )
        assertEquals(target.caseCode, updatedStd.lastCaseCitation)
    }

    @Test
    fun suspensionsDisablePlayerAvailabilityAndTickDownWeekly() {
        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val ruleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules())
        val union = LaborUnionAgreement()
        val state = DisciplinaryEngine.createDefaultState(SportType.BASKETBALL, franchises, ruleBook)
        val target = state.pendingCases.first()

        val initialTeam = franchises.first { it.id == target.franchiseId }
        val initialLineupRating = initialTeam.effectiveLineupRating

        // Suspend player for 3 games
        val outcome = DisciplinaryEngine.adjudicateIncident(
            state = state,
            franchises = franchises,
            unionAgreement = union,
            ruleBook = ruleBook,
            incidentId = target.id,
            suspensionGames = 3,
            fineThousands = 50,
            setNewPrecedent = false,
            commissionerNote = "Mandatory safety suspension.",
            random = Random(42)
        )

        val postTeam = outcome.updatedFranchises.first { it.id == target.franchiseId }
        val suspendedPlayer = postTeam.roster.first { it.id == target.playerId }
        assertTrue(suspendedPlayer.isSuspended)
        assertTrue(suspendedPlayer.isUnavailableForGame)
        assertEquals(3, suspendedPlayer.suspendedGamesRemaining)
        assertEquals(0, suspendedPlayer.effectiveOverallRating)
        assertEquals(0.0, suspendedPlayer.statProductionMultiplier, 0.001)
        assertTrue(postTeam.suspendedPlayersCount >= 1)
        assertTrue(
            "Team effective rating should drop or re-optimize when starter suspended ($initialLineupRating vs ${postTeam.effectiveLineupRating})",
            postTeam.effectiveLineupRating <= initialLineupRating
        )

        // Verify suspended player produces no box score stats in simulated match
        val oppTeam = outcome.updatedFranchises.first { it.id != postTeam.id }
        val match = LeagueSimulationEngine.generateMatchesForWeek(1, outcome.updatedFranchises, SportType.BASKETBALL).first()
        val boxScore = RecordBookEngine.generateMatchPlayerStats(
            match = match,
            home = postTeam,
            away = oppTeam,
            homeScore = 104,
            awayScore = 99,
            sportType = SportType.BASKETBALL,
            seasonYear = 2026,
            isPlayoffs = false,
            random = Random(7)
        )
        assertFalse(
            "Suspended player ${suspendedPlayer.name} must not appear in match box score",
            boxScore.any { it.playerId == suspendedPlayer.id }
        )

        // Tick down 3 weeks and verify player is reinstated cleanly
        var rollingTeams = outcome.updatedFranchises
        val tick1 = DisciplinaryEngine.advanceWeeklySuspensions(rollingTeams)
        rollingTeams = tick1.first
        assertEquals(2, rollingTeams.first { it.id == target.franchiseId }.roster.first { it.id == target.playerId }.suspendedGamesRemaining)

        val tick2 = DisciplinaryEngine.advanceWeeklySuspensions(rollingTeams)
        rollingTeams = tick2.first
        assertEquals(1, rollingTeams.first { it.id == target.franchiseId }.roster.first { it.id == target.playerId }.suspendedGamesRemaining)

        val tick3 = DisciplinaryEngine.advanceWeeklySuspensions(rollingTeams)
        rollingTeams = tick3.first
        val reinstated = rollingTeams.first { it.id == target.franchiseId }.roster.first { it.id == target.playerId }
        assertEquals(0, reinstated.suspendedGamesRemaining)
        assertFalse(reinstated.isSuspended)
        assertTrue(tick3.second.any { it.contains(reinstated.name) && it.contains("REINSTATEMENT") })
    }

    @Test
    fun viewModelWorkflowAppealsPrecedentAdjustmentAndPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.createNewCustomLeague("Apex Justice League", SportType.HOCKEY, CommissionerBackground.ANTITRUST_ATTORNEY)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val startDisc = vm.uiState.value.disciplinaryState
        assertEquals(SportType.HOCKEY, startDisc.sportType)
        assertTrue(startDisc.pendingCases.isNotEmpty())

        // Calibrate a category precedent policy
        val policyMsg = vm.updateDisciplinaryPrecedentPolicy(
            category = DisciplinaryCategory.FLAGRANT_FOUL,
            benchmarkGames = 4,
            benchmarkFineThousands = 75,
            posture = PrecedentPosture.FIRM
        )
        assertTrue(policyMsg.contains("Firm Deterrence"))
        assertEquals(
            4,
            vm.uiState.value.disciplinaryState.standardFor(DisciplinaryCategory.FLAGRANT_FOUL).benchmarkGames
        )

        // Pick a pending case and issue an excessive sentence (+5G over expected) to force a Union Arbitration Appeal
        val targetCase = vm.uiState.value.disciplinaryState.pendingCases.first()
        val std = vm.uiState.value.disciplinaryState.standardFor(targetCase.category)
        val (expGames, _) = std.expectedPenaltyFor(targetCase.severity, targetCase.priorOffensesCount)
        val excessiveGames = (expGames + 4).coerceAtMost(targetCase.category.maxGames)

        val (ruledOk, _) = vm.adjudicateDisciplinaryCase(
            incidentId = targetCase.id,
            suspensionGames = excessiveGames,
            fineThousands = targetCase.category.maxFineThousands,
            setNewPrecedent = false,
            commissionerNote = "Harsh deterrence message."
        )
        assertTrue(ruledOk)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val afterRuling = vm.uiState.value.disciplinaryState
        assertTrue(
            "Excessive suspension should trigger Players Union Arbitration Appeal",
            afterRuling.appealedCases.any { it.id == targetCase.id }
        )

        // Resolve the union appeal via REDUCE_TO_PRECEDENT
        val (appealResolved, appealMsg) = vm.resolveDisciplinaryAppeal(
            incidentId = targetCase.id,
            action = AppealResolutionAction.REDUCE_TO_PRECEDENT
        )
        assertTrue(appealResolved)
        assertTrue(appealMsg.contains("precedent"))
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val finalCase = vm.uiState.value.disciplinaryState.incidents.first { it.id == targetCase.id }
        assertEquals(DisciplinaryStatus.APPEAL_RESOLVED, finalCase.status)

        // Verify live RosterPlayer suspension count was adjusted down to match precedent
        val livePlayer = vm.uiState.value.franchises
            .first { it.id == targetCase.franchiseId }
            .roster.first { it.id == targetCase.playerId }
        assertEquals(finalCase.ruling!!.suspensionGames, livePlayer.suspendedGamesRemaining)

        // Verify persistence round-trip in LeagueRepository including RosterPlayer suspension count
        val repo = LeagueRepository(context)
        val restoredDisc = repo.loadDisciplinaryState(SportType.HOCKEY, vm.uiState.value.ruleBook)
        assertNotNull(restoredDisc)
        assertEquals(
            DisciplinaryStatus.APPEAL_RESOLVED,
            restoredDisc!!.incidents.first { it.id == targetCase.id }.status
        )
        runBlocking {
            val s = vm.uiState.value
            repo.saveLeague(
                leagueName = s.leagueName,
                sportType = s.sportType,
                commissionerBackground = s.commissionerBackground,
                currentYear = s.currentYear,
                currentWeek = s.currentWeek,
                seasonPhase = s.seasonPhase,
                cashReservesMillions = s.cashReservesMillions,
                leagueValuationMillions = s.leagueValuationMillions,
                leagueIntegrityScore = s.leagueIntegrityScore,
                competitiveParityScore = s.competitiveParityScore,
                unionAgreement = s.unionAgreement,
                activeBroadcast = s.activeBroadcast,
                franchises = s.franchises,
                cities = s.cities,
                campaigns = s.campaigns,
                history = s.valuationHistory,
                newsFeed = s.recentNews,
                ruleBook = s.ruleBook
            )
            val loadedLeague = repo.loadLeague()
            assertNotNull(loadedLeague)
            val restoredTeams = repo.deserializeFranchises(loadedLeague!!.franchisesJson, s.currentYear)
            val persistedPlayer = restoredTeams
                .first { it.id == targetCase.franchiseId }
                .roster.first { it.id == targetCase.playerId }
            assertEquals(finalCase.ruling!!.suspensionGames, persistedPlayer.suspendedGamesRemaining)
        }
    }

    @Test
    fun disciplinaryDeskScreenInteractiveReviewPrecedentAndCasebookUi() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val firstCase = vm.uiState.value.disciplinaryState.pendingCases.first()

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                DisciplinaryDeskScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("disciplinary_desk_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_disciplinary_overview").assertIsDisplayed()

        // Scroll to first pending incident's Review & Rule button and open modal
        composeTestRule.onNodeWithTag("disciplinary_list")
            .performScrollToNode(hasTestTag("btn_review_case_${firstCase.id}"))
        composeTestRule.onNodeWithTag("btn_review_case_${firstCase.id}").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("disciplinary_ruling_modal").assertIsDisplayed()

        // Adjust suspension length (+1G) and toggle new precedent baseline
        composeTestRule.onNodeWithTag("btn_games_plus").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_fine_plus").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("switch_set_new_precedent").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("input_ruling_note").performScrollTo().performTextInput("Consistent baseline ruling")

        composeTestRule.onNodeWithTag("btn_issue_ruling").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Feedback banner should be visible and incident moved out of pending
        composeTestRule.onNodeWithTag("disciplinary_feedback_banner").assertIsDisplayed()
        assertFalse(vm.uiState.value.disciplinaryState.pendingCases.any { it.id == firstCase.id })

        // Switch to Precedents tab and calibrate a category standard
        composeTestRule.onNodeWithTag("tab_disc_precedent").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("disciplinary_list")
            .performScrollToNode(hasTestTag("btn_edit_precedent_OFFICIAL_ABUSE"))
        composeTestRule.onNodeWithTag("btn_edit_precedent_OFFICIAL_ABUSE").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("disciplinary_precedent_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chip_posture_FIRM").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_save_precedent").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            PrecedentPosture.FIRM,
            vm.uiState.value.disciplinaryState.standardFor(DisciplinaryCategory.OFFICIAL_ABUSE).posture
        )

        // Switch to Casebook tab and verify ruled case is listed
        composeTestRule.onNodeWithTag("tab_disc_casebook").performClick()
        composeTestRule.waitForIdle()
        val isAppealPending = vm.uiState.value.disciplinaryState.appealedCases.any { it.id == firstCase.id }
        val expectedCardTag = if (isAppealPending) "card_appeal_${firstCase.id}" else "card_casebook_${firstCase.id}"
        composeTestRule.onNodeWithTag("disciplinary_list")
            .performScrollToNode(hasTestTag(expectedCardTag))
        composeTestRule.onNodeWithTag(expectedCardTag).assertIsDisplayed()
    }

    @Test
    fun deskGroupedHubsRenderAndLaunchDisciplinaryDesk() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        listOf(
            "group_integrity_governance",
            "group_personnel_rosters",
            "group_business_expansion",
            "group_competition_lore"
        ).forEach { groupTag ->
            composeTestRule.onNodeWithTag("home_screen_content")
                .performScrollToNode(hasTestTag(groupTag))
            composeTestRule.onNodeWithTag(groupTag).assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag("home_screen_content")
            .performScrollToNode(hasTestTag("btn_open_disciplinary_desk"))
        composeTestRule.onNodeWithTag("btn_open_disciplinary_desk").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("disciplinary_desk_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_back_disciplinary_desk").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("main_scaffold").assertIsDisplayed()
    }
}
