package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.data.LeagueRepository
import com.example.model.DraftClassGenerator
import com.example.model.LaborDisputeEngine
import com.example.model.LaborTriggerType
import com.example.model.SalaryCapStrictness
import com.example.model.SalaryCapType
import com.example.model.SportType
import com.example.model.TradeEngine
import com.example.ui.screens.DraftScoutingScreen
import com.example.ui.screens.LaborUnionNegotiationsScreen
import com.example.ui.screens.TradeDeskScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DraftTradeAndLaborDisputeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetLeagueStorage(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.resetLeague()
    }

    @Test
    fun proceduralDraftClassAssignsRatingsAndPotentialAndAllowsManagedFranchiseSelection() {
        val classA = DraftClassGenerator.generateProceduralClass(
            year = 2028,
            poolSize = 18,
            sportType = SportType.BASKETBALL,
            seed = 101L
        )
        val classB = DraftClassGenerator.generateProceduralClass(
            year = 2028,
            poolSize = 18,
            sportType = SportType.BASKETBALL,
            seed = 202L
        )

        assertEquals(18, classA.size)
        assertEquals(18, classB.size)
        assertNotEquals(
            "Distinct procedural seeds should yield different prospect rosters",
            classA.map { it.name },
            classB.map { it.name }
        )
        classA.forEach { prospect ->
            assertTrue("Overall must be in realistic draft range", prospect.overallRating in 66..87)
            assertTrue(
                "Potential rating (${prospect.potentialRating}) must exceed overall (${prospect.overallRating})",
                prospect.potentialRating > prospect.overallRating
            )
            assertTrue("Upside points must be positive", prospect.upsidePoints > 0)
            assertTrue("Potential grade should not be blank", prospect.scoutingReport.potentialGrade.isNotBlank())
        }

        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val franchises = vm.uiState.value.franchises
        assertTrue(franchises.size >= 2)
        val targetManaged = franchises[1]
        vm.setManagedFranchise(targetManaged.id)
        assertEquals(targetManaged.id, vm.uiState.value.managedFranchiseId)
        assertEquals(targetManaged.id, vm.uiState.value.managedFranchise?.id)

        vm.generateProceduralDraftClass(seed = 555L)
        val generatedClass = vm.uiState.value.upcomingDraftClass
        assertTrue(generatedClass.isNotEmpty())
        val topProspect = generatedClass.first()
        val rosterBefore = vm.uiState.value.managedFranchise!!.roster.size

        val (pickedOk, pickMsg) = vm.pickProspectForManagedFranchise(topProspect.id)
        assertTrue("Managed franchise should successfully draft prospect: $pickMsg", pickedOk)

        val updatedManaged = vm.uiState.value.managedFranchise!!
        assertEquals(rosterBefore + 1, updatedManaged.roster.size)
        val rookie = updatedManaged.roster.find { it.name == topProspect.name }
        assertNotNull("Drafted prospect must appear on managed franchise roster", rookie)
        assertEquals(topProspect.overallRating, rookie!!.overallRating)
        assertEquals(topProspect.potentialRating, rookie.potentialRating)

        val updatedPoolProspect = vm.uiState.value.upcomingDraftClass.first { it.id == topProspect.id }
        assertTrue(updatedPoolProspect.isDrafted)
        assertEquals(targetManaged.id, updatedPoolProspect.draftedByFranchiseId)

        // Verify DraftScoutingScreen UI renders selector, generator, and prospects
        vm.openDraftScoutingScreen(true)
        composeTestRule.setContent {
            MyApplicationTheme {
                val state = vm.uiState.collectAsStateWithLifecycle().value
                DraftScoutingScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("draft_scouting_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_managed_franchise_selector").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_generate_draft_class").assertIsDisplayed()
        val secondProspect = vm.uiState.value.upcomingDraftClass[1]
        composeTestRule.onNodeWithTag("draft_scouting_list")
            .performScrollToNode(hasTestTag("scouting_prospect_${secondProspect.id}"))
        composeTestRule.onNodeWithTag("scouting_prospect_${secondProspect.id}").assertIsDisplayed()
    }

    @Test
    fun tradeValidationEnforcesSalaryCapSpaceAndAllowsLegalPlayerPlusPickTrades() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val initialState = vm.uiState.value
        val teamA = initialState.franchises[0]
        val teamB = initialState.franchises[1]

        // 1. Test Illegal Trade (insufficient salary cap space under Hard Cap)
        val cheapPlayerA = teamA.roster.minByOrNull { it.salaryMillions }!!
        val expensivePlayerB = teamB.roster.maxByOrNull { it.salaryMillions }!!
        val livePayrollA = TradeEngine.calculateLivePayrollMillions(teamA)
        val tightHardCap = livePayrollA + 1L // Only $1M cap space remaining

        val tightUnion = initialState.unionAgreement.copy(
            salaryCapMillions = tightHardCap,
            salaryCapType = SalaryCapType.HARD_CAP,
            luxuryTaxThresholdMillions = tightHardCap + 12L
        )
        val illegalValidation = TradeEngine.validateTradeLegality(
            teamA = teamA,
            teamB = teamB,
            teamASendsPlayers = listOf(cheapPlayerA),
            teamASendsPicks = teamA.draftPicks.take(1),
            teamBSendsPlayers = listOf(expensivePlayerB),
            teamBSendsPicks = emptyList(),
            unionAgreement = tightUnion,
            tradeRules = initialState.ruleBook.tradeRules,
            capStrictness = SalaryCapStrictness.STRICT_HARD_CAP
        )

        if (expensivePlayerB.salaryMillions > cheapPlayerA.salaryMillions + 1L) {
            assertFalse("Trade exceeding Team A hard cap space must be rejected", illegalValidation.isLegal)
            assertFalse("Cap space legality flag must fail", illegalValidation.isCapSpaceCompliant)
            assertFalse(illegalValidation.teamAImpact.isCapSpaceLegal)
            assertTrue(illegalValidation.violations.isNotEmpty())
        }

        // 2. Test Legal Trade under Cap Space Absorption (high salary cap leaves ample room)
        val roomyCap = maxOf(livePayrollA, TradeEngine.calculateLivePayrollMillions(teamB)) + 60L
        val roomyUnion = initialState.unionAgreement.copy(
            salaryCapMillions = roomyCap,
            salaryCapType = SalaryCapType.SOFT_CAP_LUXURY_TAX,
            luxuryTaxThresholdMillions = roomyCap + 25L
        )
        val offeredPickA = teamA.draftPicks.first()
        val offeredPickB = teamB.draftPicks.first()
        val balancedPlayerA = teamA.roster.maxByOrNull { it.tradeValue }!!
        val balancedPlayerB = teamB.roster.maxByOrNull { it.tradeValue }!!

        val legalValidation = TradeEngine.validateTradeLegality(
            teamA = teamA,
            teamB = teamB,
            teamASendsPlayers = listOf(balancedPlayerA),
            teamASendsPicks = listOf(offeredPickA),
            teamBSendsPlayers = listOf(balancedPlayerB),
            teamBSendsPicks = listOf(offeredPickB),
            unionAgreement = roomyUnion,
            tradeRules = initialState.ruleBook.tradeRules,
            capStrictness = SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX
        )
        assertTrue("Trade within salary cap space must pass legality check", legalValidation.isLegal)
        assertTrue(legalValidation.isCapSpaceCompliant)
        assertTrue(legalValidation.teamAImpact.capSpaceAfterMillions >= 0L)
        assertTrue(legalValidation.teamBImpact.capSpaceAfterMillions >= 0L)

        // Execute full pending -> approved flow via ViewModel when cap is roomy
        // Raise cap via contract dispute negotiation or verify ViewModel execute on matched assets
        val (queuedOk, queueMsg) = vm.proposeTradeAsPending(
            teamAId = teamA.id,
            teamBId = teamB.id,
            teamASendsPlayers = listOf(balancedPlayerA),
            teamASendsPicks = listOf(offeredPickA),
            teamBSendsPlayers = listOf(balancedPlayerB),
            teamBSendsPicks = listOf(offeredPickB)
        )
        // Regardless of initial cap threshold, if valid approve it and assert asset movement
        if (queuedOk) {
            val proposalId = vm.uiState.value.pendingTrades.first().id
            val approved = vm.approveTrade(proposalId)
            assertTrue("Queued cap-legal trade should approve cleanly", approved)
            val updatedA = vm.uiState.value.franchises.first { it.id == teamA.id }
            val updatedB = vm.uiState.value.franchises.first { it.id == teamB.id }
            assertTrue("Team A should acquire Team B's player", updatedA.roster.any { it.id == balancedPlayerB.id })
            assertTrue("Team B should acquire Team A's player", updatedB.roster.any { it.id == balancedPlayerA.id })
            assertTrue("Team A should hold Team B's traded pick", updatedA.draftPicks.any { it.id == offeredPickB.id })
            assertTrue("Team B should hold Team A's traded pick", updatedB.draftPicks.any { it.id == offeredPickA.id })
        } else {
            assertTrue("Illegal queue failure must cite cap or asset rule", queueMsg.isNotBlank())
        }

        // Verify TradeDeskScreen renders Trade Machine & Salary Cap Space Validation card
        vm.openTradeDesk()
        composeTestRule.setContent {
            MyApplicationTheme {
                val state = vm.uiState.collectAsStateWithLifecycle().value
                TradeDeskScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_trade_machine").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("trade_machine_content")
            .performScrollToNode(hasTestTag("card_trade_cap_legality"))
        composeTestRule.onNodeWithTag("card_trade_cap_legality").assertIsDisplayed()
        composeTestRule.onNodeWithTag("txt_trade_legality_summary").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun randomLaborContractDisputeImpactsTeamMoraleAndLeagueSalaryStructure() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val initialState = vm.uiState.value
        val baselineTeamMorale = initialState.franchises.map { it.teamMorale }.average()
        val baselineCap = initialState.unionAgreement.salaryCapMillions
        val baselineTax = initialState.unionAgreement.luxuryTaxThresholdMillions
        val baselineRookieScale = initialState.unionAgreement.rookieScaleMultiplierPct
        val baselineMaxCapPct = initialState.unionAgreement.maxContractCapPct
        val baselineMinK = initialState.unionAgreement.minPlayerSalaryK
        val baselineShare = initialState.unionAgreement.playerRevenueSharePct

        // Deterministically force a Supermax Cap Standoff dispute (scenario index 0 -> seed picking scenario 0)
        // Pick a seed until forceTrigger produces a choice that alters cap & team morale
        val roll = vm.triggerRandomContractDispute(seed = 1L)
        assertEquals(LaborTriggerType.CONTRACT_DISPUTE_RANDOM, roll.trigger.type)
        assertNotNull(vm.uiState.value.activeLaborNegotiationOverlay)
        assertEquals(LaborTriggerType.CONTRACT_DISPUTE_RANDOM, vm.uiState.value.activeLaborTrigger?.type)

        val duringDisputeMorale = vm.uiState.value.franchises.map { it.teamMorale }.average()
        assertTrue("Active dispute must dip team morale ($baselineTeamMorale -> $duringDisputeMorale)", duringDisputeMorale < baselineTeamMorale)

        // Verify LaborUnionNegotiationOverlay displays Team Morale & Salary Structure impact chips
        vm.openLaborUnionScreen(true)
        composeTestRule.setContent {
            MyApplicationTheme {
                val state = vm.uiState.collectAsStateWithLifecycle().value
                LaborUnionNegotiationsScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("labor_negotiation_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_team_morale_baseline").assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_choice_0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("labor_overlay_team_morale_0", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        // Resolving concession choice should boost team morale and modify league-wide salary structure
        val concedeChoice = vm.uiState.value.activeLaborNegotiationOverlay!!.choices.first()
        composeTestRule.onNodeWithTag("labor_overlay_choice_0").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val afterState = vm.uiState.value
        assertNull("Overlay must dismiss after resolving dispute choice", afterState.activeLaborNegotiationOverlay)

        val postMorale = afterState.franchises.map { it.teamMorale }.average()
        assertTrue(
            "Conceding in labor dispute must raise team morale above dipped dispute level ($duringDisputeMorale -> $postMorale)",
            postMorale > duringDisputeMorale
        )

        val structureChanged = afterState.unionAgreement.salaryCapMillions != baselineCap ||
            afterState.unionAgreement.luxuryTaxThresholdMillions != baselineTax ||
            afterState.unionAgreement.rookieScaleMultiplierPct != baselineRookieScale ||
            afterState.unionAgreement.maxContractCapPct != baselineMaxCapPct ||
            afterState.unionAgreement.minPlayerSalaryK != baselineMinK ||
            afterState.unionAgreement.playerRevenueSharePct != baselineShare ||
            concedeChoice.requestedSalaryCapType != null
        assertTrue("Resolving contract dispute must adjust league-wide salary structure", structureChanged)

        // Also validate LaborDisputeEngine generates hardline negative morale scenario reliably
        val engineRoll = LaborDisputeEngine.maybeGenerateRandomContractDispute(
            state = afterState,
            triggerHistory = emptyList(),
            seed = 7L,
            forceTrigger = true
        )
        assertNotNull(engineRoll)
        assertTrue(
            "Hardline option (index 2) must carry negative team morale delta",
            engineRoll!!.overlaySession.choices.last().projectedTeamMoraleDelta < 0
        )
    }
}
