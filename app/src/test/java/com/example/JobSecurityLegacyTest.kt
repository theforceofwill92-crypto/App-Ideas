package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.LeagueRepository
import com.example.model.*
import com.example.ui.screens.JobSecurityLegacyScreen
import com.example.ui.screens.OffSeasonSummitScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JobSecurityLegacyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stakeholderScoresAndAnnualReviewReactToStrongVsDistressedPerformance() {
        val base = LeagueUiState()

        // High-performing scenario
        val thrivingFranchises = base.franchises.map {
            it.copy(
                fanMorale = 92,
                fanLoyalty = 90,
                ownership = it.ownership.copy(ownerApproval = 92)
            )
        }
        val strongInput = base.copy(
            cashReservesMillions = 180L,
            leagueValuationMillions = 760L,
            totalLeagueFanbaseMillions = 26.5,
            leagueIntegrityScore = 94,
            competitiveParityScore = 90,
            ownerApprovalRating = 92,
            unionAgreement = base.unionAgreement.copy(
                unionSatisfaction = 90,
                strikeRisk = 8,
                playerRevenueSharePct = 51,
                cbaYearsRemaining = 4,
                mandatoryPrivateCharterFlights = true,
                maxBackToBackGamesLimit = true,
                independentConcussionProtocol = true
            ),
            activeBroadcast = base.activeBroadcast.copy(annualPayoutMillions = 44L),
            franchises = thrivingFranchises,
            boardVoteHistory = listOf(
                BoardVoteResult(
                    id = "v1",
                    proposalTitle = "TV Expansion",
                    proposalSummary = "Unanimous yes",
                    category = GovernanceProposalCategory.RULE_CHANGE,
                    ballots = emptyList(),
                    yesVotes = 6,
                    noVotes = 0,
                    abstainVotes = 0,
                    totalVotes = 6,
                    passed = true,
                    enactedViaCommissionerOverride = false,
                    year = 2026,
                    week = 4
                )
            ),
            officiatingState = base.officiatingState.copy(
                leagueCallAccuracyPct = 94.0,
                incidents = base.officiatingState.incidents.map { it.copy(isResolved = true) }
            ),
            disciplinaryState = base.disciplinaryState.copy(precedentConsistencyScore = 94)
        ).toPerformanceInput(seasonChampionNameOverride = thrivingFranchises.first().teamFullName)

        val strongBreakdown = JobSecurityLegacyEngine.evaluateStakeholders(strongInput)
        assertTrue(strongBreakdown.ownersScore >= 84)
        assertTrue(strongBreakdown.playersScore >= 84)
        assertTrue(strongBreakdown.fansScore >= 82)
        assertTrue(strongBreakdown.mediaScore >= 78)
        assertTrue(strongBreakdown.activeRiskAlerts.isEmpty())

        val strongReview = JobSecurityLegacyEngine.generateAnnualReview(
            current = JobSecurityLegacyEngine.createInitialState(strongInput),
            input = strongInput
        )
        assertTrue(strongReview.overallGrade in listOf(PerformanceReviewGrade.A_PLUS, PerformanceReviewGrade.A, PerformanceReviewGrade.B_PLUS))
        assertEquals(CommissionerContractAction.MULTI_YEAR_EXTENSION, strongReview.contractAction)
        assertTrue(strongReview.legacyPointsAwarded >= 75)
        assertEquals(4, strongReview.verdicts.size)

        // Distressed scenario
        val distressedFranchises = base.franchises.map {
            it.copy(
                fanMorale = 34,
                fanLoyalty = 36,
                ownership = it.ownership.copy(ownerApproval = 38)
            )
        }
        val unresolvedScandal = BlownCallIncident(
            id = "sc1",
            week = 10,
            seasonYear = 2026,
            matchId = "m10_1",
            matchupTitle = "BOS @ NYC",
            finalScoreSummary = "102-101",
            offendingRefereeId = "r1",
            offendingRefereeName = "Ref Test",
            affectedTeamId = distressedFranchises[0].id,
            affectedTeamName = distressedFranchises[0].teamFullName,
            benefitingTeamName = distressedFranchises[1].teamFullName,
            callDescription = "Missed buzzer whistle",
            severity = BlownCallSeverity.MODERATE,
            fanOutragePct = 92,
            integrityDamage = 10,
            isResolved = false
        )
        val weakInput = base.copy(
            cashReservesMillions = 8L,
            leagueValuationMillions = 340L,
            leagueIntegrityScore = 44,
            competitiveParityScore = 45,
            ownerApprovalRating = 36,
            unionAgreement = base.unionAgreement.copy(
                unionSatisfaction = 30,
                strikeRisk = 78,
                playerRevenueSharePct = 46,
                cbaYearsRemaining = 0,
                mandatoryPrivateCharterFlights = false,
                maxBackToBackGamesLimit = false,
                independentConcussionProtocol = false
            ),
            franchises = distressedFranchises,
            boardVoteHistory = listOf(
                BoardVoteResult(
                    id = "v2",
                    proposalTitle = "Forced Mandate",
                    proposalSummary = "Overridden",
                    category = GovernanceProposalCategory.SALARY_CAP_POLICY,
                    ballots = emptyList(),
                    yesVotes = 2,
                    noVotes = 4,
                    abstainVotes = 0,
                    totalVotes = 6,
                    passed = true,
                    enactedViaCommissionerOverride = true,
                    year = 2026,
                    week = 8
                )
            ),
            officiatingState = base.officiatingState.copy(
                leagueCallAccuracyPct = 62.0,
                incidents = listOf(unresolvedScandal)
            ),
            disciplinaryState = base.disciplinaryState.copy(precedentConsistencyScore = 40)
        ).toPerformanceInput()

        val weakBreakdown = JobSecurityLegacyEngine.evaluateStakeholders(weakInput)
        assertTrue(weakBreakdown.ownersScore < 56)
        assertTrue(weakBreakdown.playersScore < 55)
        assertTrue(weakBreakdown.fansScore < 60)
        assertTrue(weakBreakdown.projectedJobSecurityScore < 52)
        assertTrue(weakBreakdown.activeRiskAlerts.size >= 3)

        val weakReview = JobSecurityLegacyEngine.generateAnnualReview(
            current = JobSecurityLegacyEngine.createInitialState(weakInput).copy(jobSecurityScore = 42),
            input = weakInput
        )
        assertTrue(weakReview.overallGrade in listOf(PerformanceReviewGrade.D, PerformanceReviewGrade.F, PerformanceReviewGrade.C))
        assertTrue(
            weakReview.contractAction in listOf(
                CommissionerContractAction.HOT_SEAT_ULTIMATUM,
                CommissionerContractAction.SURVIVED_CONFIDENCE_VOTE,
                CommissionerContractAction.CONDITIONAL_PROBATION
            )
        )
    }

    @Test
    fun repositoryPersistsAndRestoresJobSecurityLegacyState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        val baseState = LeagueUiState()
        val input = baseState.toPerformanceInput()
        val initSec = JobSecurityLegacyEngine.createInitialState(input)
        val review = JobSecurityLegacyEngine.generateAnnualReview(initSec, input)
        val ratified = JobSecurityLegacyEngine.ratifyAnnualReview(
            initSec,
            review,
            ReviewResponseChoice.MEDIA_SPECTACLE_SHOWCASE
        )

        repo.saveJobSecurityLegacyState(ratified.updatedState)
        val loaded = repo.loadJobSecurityLegacyState(input)
        assertNotNull(loaded)
        assertEquals(ratified.updatedState.jobSecurityScore, loaded!!.jobSecurityScore)
        assertEquals(ratified.updatedState.legacyPoints, loaded.legacyPoints)
        assertEquals(1, loaded.seasonsServed)
        assertEquals(1, loaded.reviewHistory.size)
        assertEquals(ReviewResponseChoice.MEDIA_SPECTACLE_SHOWCASE, loaded.reviewHistory.first().commissionerResponseChosen)
    }

    @Test
    fun viewModelConveneRatifyAndSeasonRolloverPreserveAnnualReviewHistory() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.createNewCustomLeague("Apex Security Test League", SportType.BASKETBALL, CommissionerBackground.WALL_STREET_FINANCIER)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val initialLegacy = vm.uiState.value.jobSecurityLegacyState.legacyPoints
        val review = vm.conveneAnnualPerformanceReviewNow()
        assertNotNull(vm.uiState.value.jobSecurityLegacyState.pendingAnnualReview)
        assertEquals(2026, review.seasonYear)

        val beforeVal = vm.uiState.value.leagueValuationMillions
        val outcome = vm.ratifyPendingAnnualReview(ReviewResponseChoice.OWNER_GROWTH_CHARTER)
        assertNotNull(outcome)
        val afterSec = vm.uiState.value.jobSecurityLegacyState
        assertNull(afterSec.pendingAnnualReview)
        assertEquals(1, afterSec.seasonsServed)
        assertEquals(1, afterSec.reviewHistory.size)
        assertTrue(afterSec.legacyPoints > initialLegacy)
        assertEquals(beforeVal + 12L, vm.uiState.value.leagueValuationMillions)

        // Now complete season rollover cleanly (should roll into 2027 without duplicating 2026 review)
        vm.finalizeAndInaugurateNewSeason()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2027, vm.uiState.value.currentYear)
        assertEquals(1, vm.uiState.value.jobSecurityLegacyState.reviewHistory.count { it.seasonYear == 2026 })
    }

    @Test
    fun jobSecurityLegacyScreenInteractiveConveneRatifyAndOffSeasonStep9Work() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.createNewCustomLeague("UI Meter League", SportType.FOOTBALL, CommissionerBackground.MARKETING_MAVEN)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            MyApplicationTheme {
                if (state.showOffSeasonScreen) {
                    OffSeasonSummitScreen(uiState = state, viewModel = vm)
                } else {
                    JobSecurityLegacyScreen(uiState = state, viewModel = vm)
                }
            }
        }

        composeTestRule.onNodeWithTag("job_security_legacy_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_security_meter_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("legacy_meter_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_security_lazy_column")
            .performScrollToNode(hasTestTag("card_pillar_OWNERS"))
        composeTestRule.onNodeWithTag("card_pillar_OWNERS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_security_lazy_column")
            .performScrollToNode(hasTestTag("card_pillar_PLAYERS"))
        composeTestRule.onNodeWithTag("card_pillar_PLAYERS").assertIsDisplayed()

        // Switch to Review tab, convene review, ratify with Fans pledge, verify Ledger
        composeTestRule.onNodeWithTag("tab_security_review").performClick()
        composeTestRule.onNodeWithTag("btn_convene_annual_review").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("choice_pledge_FAN_ACCESS_BLITZ").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_ratify_annual_review").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("tab_security_ledger").performClick()
        composeTestRule.onNodeWithTag("card_review_history_2026").assertIsDisplayed()

        // Verify Off-Season Summit Step 9 (ANNUAL_REVIEW) renders cleanly
        composeTestRule.runOnIdle {
            vm.openOffSeasonSummit(true)
            vm.setOffSeasonStep(OffSeasonStep.ANNUAL_REVIEW)
        }
        composeTestRule.onNodeWithTag("offseason_steps_lazy_row")
            .performScrollToNode(hasTestTag("tab_offseason_step_9"))
        composeTestRule.onNodeWithTag("tab_offseason_step_9").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offseason_summit_content")
            .performScrollToNode(hasTestTag("annual_review_card"))
        composeTestRule.onNodeWithTag("annual_review_card").assertIsDisplayed()
    }
}
