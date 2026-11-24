package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.*
import com.example.ui.screens.FreeAgencyPoolScreen
import com.example.ui.screens.JobSecurityLegacyScreen
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
class TeamReputationAndLegacyMilestonesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ownerBehaviorImpactsTeamReputationAndFreeAgentSigningLikelihood() {
        val baseState = LeagueUiState()
        val union = baseState.unionAgreement
        val marqueeAgent = baseState.freeAgentPool.first { it.overallRating >= 84 }

        val premierTeam = baseState.franchises.first().copy(
            annualPayrollMillions = union.salaryCapMillions + 4L,
            relocationCount = 0,
            lowSpendingSeasonsCount = 0,
            championships = 2,
            wins = 12,
            losses = 2,
            fanLoyalty = 90,
            stadiumCondition = 92,
            facilities = FranchiseFacilities(stadiumTier = 4, trainingTier = 4),
            ownership = baseState.franchises.first().ownership.copy(
                type = OwnershipType.COMMUNITY_TRUST,
                ownerApproval = 90,
                primaryOwner = baseState.franchises.first().ownership.primaryOwner.copy(
                    archetype = OwnerArchetype.PLAYERS_FIRST_PATRON
                )
            )
        )

        val troubledTeam = baseState.franchises.last().copy(
            annualPayrollMillions = 44L, // ~55% of cap
            relocationCount = 2,
            lastRelocatedSeasonYear = 2026,
            lowSpendingSeasonsCount = 2,
            championships = 0,
            wins = 3,
            losses = 11,
            fanMorale = 42,
            stadiumCondition = 36,
            facilities = FranchiseFacilities(stadiumTier = 1, trainingTier = 1),
            ownership = baseState.franchises.last().ownership.copy(
                type = OwnershipType.PRIVATE_EQUITY,
                ownerApproval = 40,
                primaryOwner = baseState.franchises.last().ownership.primaryOwner.copy(
                    archetype = OwnerArchetype.FISCAL_HAWK
                )
            )
        )

        val premierRep = TeamReputationEngine.evaluateTeamReputation(premierTeam, union, 2026)
        val troubledRep = TeamReputationEngine.evaluateTeamReputation(troubledTeam, union, 2026)

        assertTrue("Premier team reputation (${premierRep.score}) should beat troubled team (${troubledRep.score})", premierRep.score >= 80 && troubledRep.score < 42)
        assertEquals(TeamReputationTier.PREMIER_DESTINATION, premierRep.tier)
        assertEquals(TeamReputationTier.TOXIC_OWNERSHIP, troubledRep.tier)
        assertTrue(troubledRep.relocationPenalty >= 28)
        assertTrue(troubledRep.lowSpendingPenalty >= 20)

        val premierLikelihood = TeamReputationEngine.evaluateSigningLikelihood(marqueeAgent, premierTeam, premierRep, union)
        val troubledLikelihood = TeamReputationEngine.evaluateSigningLikelihood(marqueeAgent, troubledTeam, troubledRep, union)

        assertTrue(premierLikelihood.willAcceptOffer)
        assertTrue(premierLikelihood.likelihoodPct >= 75)
        assertFalse("Marquee FA should decline troubled team offer", troubledLikelihood.willAcceptOffer)
        assertTrue(troubledLikelihood.likelihoodPct < 36)
        assertNotNull(troubledLikelihood.refusalReason)

        // Verify FreeAgencyEngine blocks signing for troubled team while allowing premier team
        val blockedResult = FreeAgencyEngine.signFreeAgentToFranchise(
            franchise = troubledTeam,
            freeAgent = marqueeAgent,
            freeAgentPool = baseState.freeAgentPool,
            ruleBook = baseState.ruleBook.copy(salaryCapStrictness = SalaryCapStrictness.UNRESTRICTED_MARKET),
            unionAgreement = union,
            week = 2,
            seasonYear = 2026
        )
        assertFalse("Low reputation team should be refused by marquee FA", blockedResult.success)
        assertTrue(blockedResult.message.contains("refused", ignoreCase = true) || blockedResult.message.contains("reputation", ignoreCase = true))

        val allowedResult = FreeAgencyEngine.signFreeAgentToFranchise(
            franchise = premierTeam,
            freeAgent = marqueeAgent,
            freeAgentPool = baseState.freeAgentPool,
            ruleBook = baseState.ruleBook.copy(salaryCapStrictness = SalaryCapStrictness.UNRESTRICTED_MARKET),
            unionAgreement = union,
            week = 2,
            seasonYear = 2026
        )
        assertTrue("Premier destination should sign marquee FA cleanly", allowedResult.success)
    }

    @Test
    fun repositoryPersistsOwnerBehaviorAndLegacyMilestones() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        val baseState = LeagueUiState()
        val modifiedTeam = baseState.franchises.first().copy(
            relocationCount = 2,
            lastRelocatedSeasonYear = 2028,
            lowSpendingSeasonsCount = 3
        )
        val restoredTeam = repo.deserializeFranchises(
            repo.serializeFranchises(listOf(modifiedTeam) + baseState.franchises.drop(1)),
            fallbackYear = 2028
        ).first()

        assertEquals(2, restoredTeam.relocationCount)
        assertEquals(2028, restoredTeam.lastRelocatedSeasonYear)
        assertEquals(3, restoredTeam.lowSpendingSeasonsCount)

        // Evaluate Dynasty Builder + TV Mogul and persist JobSecurityLegacyState
        val input1 = baseState.copy(currentYear = 2026).toPerformanceInput(modifiedTeam.teamFullName)
        val afterFirstTitle = JobSecurityLegacyEngine.evaluateLegacyMilestones(
            current = baseState.jobSecurityLegacyState,
            input = input1,
            newlyCrownedChampion = modifiedTeam,
            championSeasonYear = 2026
        ).updatedState
        assertEquals(1, afterFirstTitle.currentChampionStreakCount)
        assertFalse(afterFirstTitle.legacyMilestones.first { it.id == LegacyMilestoneId.DYNASTY_BUILDER }.isUnlocked)

        val recordTv = baseState.activeBroadcast.copy(
            partnerName = "OmniStream Global+",
            annualPayoutMillions = 104L
        )
        val input2 = baseState.copy(
            currentYear = 2027,
            activeBroadcast = recordTv
        ).toPerformanceInput(modifiedTeam.teamFullName)
        val eval2 = JobSecurityLegacyEngine.evaluateLegacyMilestones(
            current = afterFirstTitle,
            input = input2,
            newlyCrownedChampion = modifiedTeam,
            championSeasonYear = 2027
        )
        val state2 = eval2.updatedState
        assertTrue(state2.legacyMilestones.first { it.id == LegacyMilestoneId.DYNASTY_BUILDER }.isUnlocked)
        assertTrue(state2.legacyMilestones.first { it.id == LegacyMilestoneId.TV_MOGUL }.isUnlocked)
        assertEquals(2, state2.maxChampionStreakCount)
        assertEquals(104L, state2.recordBroadcastAnnualMillions)
        assertTrue(eval2.bonusLegacyPointsAwarded >= 220)

        repo.saveJobSecurityLegacyState(state2)
        val loaded = repo.loadJobSecurityLegacyState(input2)
        assertNotNull(loaded)
        assertEquals(2, loaded!!.maxChampionStreakCount)
        assertEquals(104L, loaded.recordBroadcastAnnualMillions)
        assertTrue(loaded.legacyMilestones.first { it.id == LegacyMilestoneId.DYNASTY_BUILDER }.isUnlocked)
        assertTrue(loaded.legacyMilestones.first { it.id == LegacyMilestoneId.TV_MOGUL }.isUnlocked)
        assertEquals(2, loaded.championshipLedger.size)
    }

    @Test
    fun viewModelTvMogulUnlockAndUiScreensRenderReputationAndLegacyMilestones() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.createNewCustomLeague("Reputation & Legacy Sim", SportType.BASKETBALL, CommissionerBackground.MARKETING_MAVEN)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Relocation increments relocationCount via relocateFranchise
        val targetTeamId = vm.uiState.value.franchises.first().id
        val availableCity = vm.uiState.value.cities.first { !it.hasFranchise }
        vm.relocateFranchise(targetTeamId, availableCity.id)
        val relocatedTeam = vm.uiState.value.franchises.first { it.id == targetTeamId }
        assertEquals(1, relocatedTeam.relocationCount)

        // Signing a record $96M/yr TV deal unlocks TV Mogul milestone and awards +100 Legacy Points
        val preLegacyPts = vm.uiState.value.jobSecurityLegacyState.legacyPoints
        val blockbuster = BroadcastContract(
            id = "tv_blockbuster_1",
            partnerName = "OmniStream Global+",
            platform = BroadcastPlatform.GLOBAL_STREAMER,
            annualPayoutMillions = 96L,
            yearsRemaining = 5,
            reachMultiplier = 1.9,
            upfrontSigningBonusMillions = 40L,
            networkScope = NetworkScope.GLOBAL_STREAMING
        )
        vm.signNegotiatedBroadcastContract(blockbuster, lumpSumBonusMillions = 40L)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val afterSec = vm.uiState.value.jobSecurityLegacyState
        assertTrue("TV Mogul should be unlocked", afterSec.legacyMilestones.first { it.id == LegacyMilestoneId.TV_MOGUL }.isUnlocked)
        assertTrue("Legacy points should increase by at least 100", afterSec.legacyPoints >= preLegacyPts + 100)
        assertEquals(96L, afterSec.recordBroadcastAnnualMillions)

        // Make one team troubled (2 relocations + very low payroll) to verify FreeAgencyPoolScreen UI + refusal banner
        val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<LeagueUiState>
        val troubledClub = vm.uiState.value.franchises.first { it.id == targetTeamId }.copy(
            relocationCount = 3,
            lowSpendingSeasonsCount = 2,
            annualPayrollMillions = 42L,
            ownership = relocatedTeam.ownership.copy(
                type = OwnershipType.PRIVATE_EQUITY,
                ownerApproval = 38,
                primaryOwner = relocatedTeam.ownership.primaryOwner.copy(archetype = OwnerArchetype.FISCAL_HAWK)
            )
        )
        val marqueeFa = vm.uiState.value.freeAgentPool.first { it.overallRating >= 84 }
        flow.value = vm.uiState.value.copy(
            franchises = vm.uiState.value.franchises.map { if (it.id == targetTeamId) troubledClub else it },
            ruleBook = vm.uiState.value.ruleBook.copy(salaryCapStrictness = SalaryCapStrictness.UNRESTRICTED_MARKET),
            selectedFreeAgencyFranchiseId = targetTeamId
        )

        var showLegacyTabTest by androidx.compose.runtime.mutableStateOf(false)
        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            MyApplicationTheme {
                if (!showLegacyTabTest) {
                    FreeAgencyPoolScreen(uiState = state, viewModel = vm)
                } else {
                    JobSecurityLegacyScreen(uiState = state, viewModel = vm)
                }
            }
        }

        composeTestRule.onNodeWithTag("free_agency_pool_list")
            .performScrollToNode(hasTestTag("card_fa_team_reputation_$targetTeamId"))
        composeTestRule.onNodeWithTag("card_fa_team_reputation_$targetTeamId").assertIsDisplayed()

        composeTestRule.onNodeWithTag("free_agency_pool_list")
            .performScrollToNode(hasTestTag("fa_likelihood_badge_${marqueeFa.id}"))
        composeTestRule.onNodeWithTag("fa_likelihood_badge_${marqueeFa.id}").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_sign_fa_${marqueeFa.id}").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("free_agency_pool_list")
            .performScrollToNode(hasTestTag("fa_status_banner"))
        composeTestRule.onNodeWithTag("fa_status_banner").assertIsDisplayed()

        // Switch to JobSecurityLegacyScreen -> Legacy Milestones tab
        composeTestRule.runOnIdle { showLegacyTabTest = true }
        composeTestRule.onNodeWithTag("tab_security_milestones").performClick()
        composeTestRule.onNodeWithTag("legacy_tracker_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_security_lazy_column")
            .performScrollToNode(hasTestTag("card_legacy_milestone_DYNASTY_BUILDER"))
        composeTestRule.onNodeWithTag("card_legacy_milestone_DYNASTY_BUILDER").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_security_lazy_column")
            .performScrollToNode(hasTestTag("card_legacy_milestone_TV_MOGUL"))
        composeTestRule.onNodeWithTag("card_legacy_milestone_TV_MOGUL").assertIsDisplayed()
    }
}
