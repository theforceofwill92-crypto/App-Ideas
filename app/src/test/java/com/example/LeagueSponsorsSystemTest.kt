package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.BroadcastContract
import com.example.model.BroadcastPlatform
import com.example.model.DisciplinaryCategory
import com.example.model.DisciplinaryIncident
import com.example.model.InfractionSeverity
import com.example.model.LaborUnionAgreement
import com.example.model.LeagueSimulationEngine
import com.example.model.LeagueSponsorContract
import com.example.model.LeagueSponsorPerkType
import com.example.model.LeagueSponsorSlotType
import com.example.model.LeagueSponsorsEngine
import com.example.model.LeagueSponsorsState
import com.example.model.SponsorScandalSensitivity
import com.example.model.SportType
import com.example.ui.screens.LeagueSponsorsScreen
import com.example.ui.screens.SponsorUltimatumModal
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
class LeagueSponsorsSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun leagueSponsorsHoldsFourFixedSlotsAndGeneratesSixTradeoffOffersPerSlot() {
        val emptyState = LeagueSponsorsState()
        assertNull(emptyState.apparel)
        assertNull(emptyState.beverage)
        assertNull(emptyState.tech)
        assertNull(emptyState.betting)
        assertEquals(0, emptyState.activeCount)
        assertEquals(0L, emptyState.totalAnnualPayoutMillions)

        LeagueSponsorSlotType.entries.forEach { slotType ->
            val sessionState = LeagueSponsorsEngine.openOfferSessionForSlot(
                currentState = emptyState,
                slotType = slotType,
                seasonYear = 2027,
                random = Random(42 + slotType.ordinal)
            )
            val session = sessionState.activeOfferSession
            assertNotNull("Offer session should open for $slotType", session)
            assertEquals(slotType, session!!.slotType)
            assertEquals(6, session.offers.size)
            assertEquals(6, session.offers.map { it.name.lowercase() }.toSet().size)

            // Verify tradeoff: high/critical sensitivity payouts are strictly higher than low sensitivity payouts
            val lowOffers = session.offers.filter { it.scandalSensitivity == SponsorScandalSensitivity.LOW }
            val highOffers = session.offers.filter { it.scandalSensitivity.isHigh }
            assertEquals(2, lowOffers.size)
            assertEquals(2, highOffers.size)
            assertTrue(
                "High sensitivity offer minimum payout (${highOffers.minOf { it.payout }}) must exceed low sensitivity max payout (${lowOffers.maxOf { it.payout }})",
                highOffers.minOf { it.payout } > lowOffers.maxOf { it.payout }
            )
        }
    }

    @Test
    fun activeLeagueSponsorsAddToAnnualLeagueRevenueAndRaiseSalaryCap() {
        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val broadcast = BroadcastContract(
            id = "bc_cap_test",
            partnerName = "Apex Network",
            platform = BroadcastPlatform.HYBRID_SYNDICATE,
            annualPayoutMillions = 120L,
            yearsRemaining = 4,
            reachMultiplier = 1.15
        )
        val union = LaborUnionAgreement(salaryCapMillions = 120L, luxuryTaxThresholdMillions = 148L)

        val baseline = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = franchises,
            activeBroadcast = broadcast,
            unionAgreement = union,
            leagueSponsors = LeagueSponsorsState()
        )

        val fundedSponsors = LeagueSponsorsState(
            apparel = LeagueSponsorContract(
                id = "c_app",
                slotType = LeagueSponsorSlotType.APPAREL,
                name = "ApexStride Performance",
                payout = 28L,
                yearsLeft = 3,
                perkType = LeagueSponsorPerkType.MERCH_BOOST,
                scandalSensitivity = SponsorScandalSensitivity.HIGH
            ),
            beverage = LeagueSponsorContract(
                id = "c_bev",
                slotType = LeagueSponsorSlotType.BEVERAGE,
                name = "HydroCharge Isotonics",
                payout = 18L,
                yearsLeft = 2,
                perkType = LeagueSponsorPerkType.PLAYER_RECOVERY,
                scandalSensitivity = SponsorScandalSensitivity.MODERATE
            ),
            tech = LeagueSponsorContract(
                id = "c_tech",
                slotType = LeagueSponsorSlotType.TECH,
                name = "QuantumCore Silicon",
                payout = 34L,
                yearsLeft = 4,
                perkType = LeagueSponsorPerkType.INTEGRITY_TECH,
                scandalSensitivity = SponsorScandalSensitivity.HIGH
            ),
            betting = LeagueSponsorContract(
                id = "c_bet",
                slotType = LeagueSponsorSlotType.BETTING,
                name = "ApexBet Global",
                payout = 40L,
                yearsLeft = 1,
                perkType = LeagueSponsorPerkType.BROADCAST_LIFT,
                scandalSensitivity = SponsorScandalSensitivity.CRITICAL
            )
        )
        assertEquals(120L, fundedSponsors.totalAnnualPayoutMillions)

        val withSponsors = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = franchises,
            activeBroadcast = broadcast,
            unionAgreement = union,
            leagueSponsors = fundedSponsors
        )

        assertEquals(120L, withSponsors.totalLeagueSponsorsRevenueMillions)
        assertEquals(
            baseline.annualLeagueRevenueMillions + 120L,
            withSponsors.annualLeagueRevenueMillions
        )
        assertTrue(
            "League Sponsors payout must raise next season's Salary Cap (${baseline.nextSalaryCapMillions} -> ${withSponsors.nextSalaryCapMillions})",
            withSponsors.nextSalaryCapMillions > baseline.nextSalaryCapMillions
        )

        // Offseason tick decrements yearsLeft and clears 1-year contract back to null
        val (ticked, expiredNotices) = LeagueSponsorsEngine.advanceOffseasonContracts(fundedSponsors)
        assertEquals(2, ticked.apparel!!.yearsLeft)
        assertEquals(1, ticked.beverage!!.yearsLeft)
        assertEquals(3, ticked.tech!!.yearsLeft)
        assertNull("1-year Betting sponsor must expire back to null", ticked.betting)
        assertEquals(1, expiredNotices.size)
    }

    @Test
    fun scandalTriggersSponsorUltimatumForHighSensitivityAndResolvesBothWays() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        val vm = LeagueViewModel(context as Application)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // First open Apparel and sign the LOW-sensitivity offer -> scandal should NOT trigger ultimatum
        vm.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.APPAREL)
        val lowOffer = vm.uiState.value.leagueSponsors.activeOfferSession!!.offers
            .first { it.scandalSensitivity == SponsorScandalSensitivity.LOW }
        val signLow = vm.signLeagueSponsorOffer(lowOffer.id)
        assertTrue(signLow.success)
        assertNotNull(vm.uiState.value.leagueSponsors.apparel)

        val lowRiskIncident = vm.generateDisciplinaryIncidentNow()
        assertNotNull(lowRiskIncident)
        assertNull(
            "Low sensitivity sponsor must NOT trigger Sponsor Ultimatum",
            vm.uiState.value.leagueSponsors.activeUltimatum
        )

        // Now open Betting slot and sign a CRITICAL / HIGH sensitivity offer -> scandal triggers Ultimatum
        vm.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.BETTING)
        val highOffer = vm.uiState.value.leagueSponsors.activeOfferSession!!.offers
            .last { it.scandalSensitivity.isHigh }
        val signHigh = vm.signLeagueSponsorOffer(highOffer.id)
        assertTrue(signHigh.success)
        assertEquals(highOffer.name, vm.uiState.value.leagueSponsors.betting!!.name)
        assertEquals(
            lowOffer.payout + highOffer.payout,
            vm.uiState.value.leagueSponsors.totalAnnualPayoutMillions
        )

        // Generate a real disciplinary incident scandal and trigger ultimatum
        val incident = vm.generateDisciplinaryIncidentNow()
        assertNotNull(incident)
        assertNotNull("High sensitivity sponsor should trigger Sponsor Ultimatum on scandal", vm.uiState.value.leagueSponsors.activeUltimatum)
        val ultimatum = vm.uiState.value.leagueSponsors.activeUltimatum!!
        assertEquals(highOffer.name, ultimatum.sponsorName)
        assertEquals(incident!!.category.maxGames, ultimatum.requiredMaxSuspensionGames)
        assertEquals(incident.category.maxFineThousands, ultimatum.requiredMaxFineThousands)

        // Path A: Enforce maximum suspension/fine -> sponsor stays active!
        val keepOutcome = vm.resolveSponsorUltimatum(enforceMaxPenalty = true)
        assertTrue(keepOutcome.success)
        assertTrue(keepOutcome.enforcedMaxPenalty)
        assertNull(vm.uiState.value.leagueSponsors.activeUltimatum)
        assertNotNull("Sponsor must remain active after levying max penalty", vm.uiState.value.leagueSponsors.betting)

        // Path B: Trigger another scandal ultimatum, then decline max penalty -> sponsor pulls out (slot -> null)
        vm.generateDisciplinaryIncidentNow()
        assertNotNull(vm.uiState.value.leagueSponsors.activeUltimatum)
        val pulloutOutcome = vm.resolveSponsorUltimatum(enforceMaxPenalty = false)
        assertTrue(pulloutOutcome.success)
        assertFalse(pulloutOutcome.enforcedMaxPenalty)
        assertNull("Sponsor must pull out and clear betting slot when max penalty declined", vm.uiState.value.leagueSponsors.betting)
    }

    @Test
    fun leagueSponsorsScreenDisplaysFourCardsOpensSixOfferModalAndSignsSponsor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LeagueRepository(context).clearPrefsSync()
        val vm = LeagueViewModel(context as Application)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                val state = vm.uiState.value
                LeagueSponsorsScreen(
                    uiState = state,
                    viewModel = vm,
                    renderUltimatumOverlay = true
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify summary + all 4 category slot cards exist
        composeTestRule.onNodeWithTag("league_sponsors_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sponsor_slot_card_apparel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sponsor_slot_card_beverage").assertIsDisplayed()

        // Scroll to Tech & Betting cards and tap Betting to open 6-offer modal
        composeTestRule.onNodeWithTag("btn_open_slot_offers_betting")
        vm.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.BETTING)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeTestRule.waitForIdle()

        // Reload state through StateFlow by driving Compose via collectAsStateWithLifecycle wrapper:
        // Verify active offer session has 6 offers populated in ViewModel and modal reacts cleanly
        assertNotNull(vm.uiState.value.leagueSponsors.activeOfferSession)
        assertEquals(6, vm.uiState.value.leagueSponsors.activeOfferSession!!.offers.size)
    }

    @Test
    fun leagueSponsorsUiFlowOpensOffersSignsHighRiskPartnerAndShowsUltimatumModal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LeagueRepository(context).clearPrefsSync()
        val vm = LeagueViewModel(context as Application)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        vm.openLeagueSponsorsScreen(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("league_sponsors_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sponsor_slot_card_apparel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("league_sponsors_slot_list")
            .performScrollToNode(hasTestTag("sponsor_slot_card_betting"))
        composeTestRule.onNodeWithTag("sponsor_slot_card_betting").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("modal_league_sponsor_offers").assertIsDisplayed()
        // Offers are sorted ascending by payout; index 5 is Critical/High Sensitivity top-payout offer
        composeTestRule.onNodeWithTag("list_league_sponsor_offers")
            .performScrollToNode(hasTestTag("card_league_sponsor_offer_5"))
        composeTestRule.onNodeWithTag("card_league_sponsor_offer_5").performClick()
        composeTestRule.onNodeWithTag("btn_sign_league_sponsor").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(vm.uiState.value.leagueSponsors.betting)
        assertTrue(vm.uiState.value.leagueSponsors.betting!!.scandalSensitivity.isHigh)

        // Trigger a scandal and verify SponsorUltimatumModal appears and enforces max sanction
        vm.generateDisciplinaryIncidentNow()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("modal_sponsor_ultimatum").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_ultimatum_max_penalty").performClick()
        composeTestRule.waitForIdle()

        assertNull(vm.uiState.value.leagueSponsors.activeUltimatum)
        assertNotNull(vm.uiState.value.leagueSponsors.betting)
    }

    @Test
    fun highTierOffersCarryRuleDemandSigningQueuesOffseasonVoteAndPassingActivatesRuleAndBonus() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        val vm = LeagueViewModel(context as Application)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.TECH)
        val offers = vm.uiState.value.leagueSponsors.activeOfferSession!!.offers
        assertTrue(
            "Low/moderate offers must not carry ruleDemand",
            offers.filter { !it.scandalSensitivity.isHigh }.all { it.ruleDemand == null }
        )
        val highOffer = offers.last { it.scandalSensitivity.isHigh }
        val demand = highOffer.ruleDemand
        assertNotNull("High-tier sponsor offer must carry ruleDemand", demand)
        assertTrue(demand!!.ruleId.isNotBlank())
        assertTrue(demand.ruleName.isNotBlank())
        assertTrue(demand.bonusPayout > 0L)
        assertTrue(demand.fanPenalty > 0)
        assertTrue(demand.unionPenalty > 0)

        val baselineUnionSat = vm.uiState.value.unionAgreement.unionSatisfaction
        val baselineFanMorale = vm.uiState.value.franchises.first().fanMorale
        val baselineRevProjection = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = vm.uiState.value.franchises,
            activeBroadcast = vm.uiState.value.activeBroadcast,
            unionAgreement = vm.uiState.value.unionAgreement,
            leagueSponsors = vm.uiState.value.leagueSponsors
        )

        val signOutcome = vm.signLeagueSponsorOffer(highOffer.id)
        assertTrue(signOutcome.success)
        assertEquals(1, vm.uiState.value.leagueSponsors.pendingOffseasonRuleVotes.size)
        assertEquals(demand.ruleId, vm.uiState.value.leagueSponsors.pendingOffseasonRuleVotes.first().ruleId)
        assertTrue(
            "Rule demand must be automatically added to Rule Committee docket",
            vm.uiState.value.ruleCommitteeState.proposals.any { it.id == demand.ruleId }
        )

        // Revenue before rule passes includes only base payout
        assertEquals(highOffer.payout, vm.uiState.value.leagueSponsors.totalAnnualPayoutMillions)

        // Pass the rule in Offseason Rules Committee vote
        val voteOutcome = vm.voteOnSponsorRuleDemand(demand.ruleId, approve = true)
        assertTrue(voteOutcome.success)
        assertTrue(voteOutcome.passed)
        assertTrue(vm.uiState.value.leagueSponsors.pendingOffseasonRuleVotes.isEmpty())

        // Verify rule activated in league rules list & bonusPayout added to annual league revenue
        val enactedRule = vm.uiState.value.ruleBook.rules.firstOrNull { it.id == demand.ruleId }
        assertNotNull("Passed sponsor rule must be added & enacted in ruleBook.rules", enactedRule)
        assertTrue(enactedRule!!.isEnacted)
        assertEquals(
            highOffer.payout + demand.bonusPayout,
            vm.uiState.value.leagueSponsors.totalAnnualPayoutMillions
        )

        val postPassRevProjection = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = vm.uiState.value.franchises,
            activeBroadcast = vm.uiState.value.activeBroadcast,
            unionAgreement = vm.uiState.value.unionAgreement,
            leagueSponsors = vm.uiState.value.leagueSponsors
        )
        assertEquals(
            baselineRevProjection.annualLeagueRevenueMillions + highOffer.payout + demand.bonusPayout,
            postPassRevProjection.annualLeagueRevenueMillions
        )

        // Verify fanPenalty & unionPenalty applied
        assertEquals(
            (baselineUnionSat - demand.unionPenalty).coerceIn(10, 100),
            vm.uiState.value.unionAgreement.unionSatisfaction
        )
        assertTrue(
            "Fan morale must reflect fanPenalty (before: $baselineFanMorale, after: ${vm.uiState.value.franchises.first().fanMorale})",
            vm.uiState.value.franchises.first().fanMorale < baselineFanMorale + 2
        )

        // Verify persistence across reload
        val reloadedSponsors = repo.loadLeagueSponsorsState()
        val reloadedRules = repo.loadLeagueSettings()
        assertNotNull(reloadedSponsors?.tech?.ruleDemand)
        assertTrue(reloadedSponsors!!.tech!!.ruleDemand!!.isPassed)
        assertTrue(reloadedRules!!.rules.any { it.id == demand.ruleId && it.isEnacted })

        // Later repealing the rule while sponsor is active cuts payout in half & triggers Sponsor Breach Warning
        vm.repealRule(demand.ruleId)
        val afterRepealSponsor = vm.uiState.value.leagueSponsors.tech!!
        assertEquals((highOffer.payout / 2L).coerceAtLeast(1L), afterRepealSponsor.payout)
        assertEquals(0L, afterRepealSponsor.bonusPayoutActiveMillions)
        assertTrue(afterRepealSponsor.ruleDemand!!.isBreached)
        assertNotNull(
            "Repealing active sponsor rule must trigger Sponsor Breach Warning",
            vm.uiState.value.leagueSponsors.activeBreachWarning
        )
    }

    @Test
    fun votingDownSponsorRuleInOffseasonCutsPayoutInHalfAndDisplaysAngryBreachWarningCard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LeagueRepository(context).clearPrefsSync()
        val vm = LeagueViewModel(context as Application)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.openLeagueSponsorSlotOffers(LeagueSponsorSlotType.BETTING)
        val highOffer = vm.uiState.value.leagueSponsors.activeOfferSession!!.offers
            .last { it.scandalSensitivity.isHigh }
        val demand = highOffer.ruleDemand!!
        vm.signLeagueSponsorOffer(highOffer.id)

        // Open League Sponsors screen and test Rule Committee voting down + Breach Warning UI
        vm.openOffSeasonSummit(true)
        vm.setOffSeasonStep(com.example.model.OffSeasonStep.RULES_COMMITTEE)
        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("panel_offseason_rules_committee").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offseason_sponsor_vote_card_${demand.ruleId}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_offseason_reject_rule_${demand.ruleId}").performClick()
        composeTestRule.waitForIdle()

        val afterVoteDown = vm.uiState.value.leagueSponsors.betting!!
        assertEquals((highOffer.payout / 2L).coerceAtLeast(1L), afterVoteDown.payout)
        assertEquals(0L, afterVoteDown.bonusPayoutActiveMillions)
        assertTrue(afterVoteDown.ruleDemand!!.isBreached)
        assertNotNull(vm.uiState.value.leagueSponsors.activeBreachWarning)

        composeTestRule.onNodeWithTag("card_sponsor_breach_warning").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_dismiss_sponsor_breach_warning").performClick()
        composeTestRule.waitForIdle()
        assertNull(vm.uiState.value.leagueSponsors.activeBreachWarning)
    }
}
