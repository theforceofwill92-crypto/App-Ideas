package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.*
import com.example.ui.components.NamingRightsOfferModal
import com.example.ui.components.SponsorBankruptcyScandalModal
import com.example.ui.screens.FacilityHubScreen
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
class StadiumNamingRightsAndFacilitiesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun facilityHubScreenComposesWithoutFormatCrashAndShowsNamingRightsCard() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.createNewCustomLeague("Facility Crash Check", SportType.BASKETBALL, CommissionerBackground.MARKETING_MAVEN)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            MyApplicationTheme {
                FacilityHubScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        val firstTeamId = vm.uiState.value.franchises.first().id
        composeTestRule.onNodeWithTag("facility_hub_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_franchise_valuation_$firstTeamId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("facility_hub_list")
            .performScrollToNode(hasTestTag("card_stadium_naming_rights_$firstTeamId"))
        composeTestRule.onNodeWithTag("card_stadium_naming_rights_$firstTeamId").assertIsDisplayed()
    }

    @Test
    fun namingRightsOffersEnforceTier2WinningRecordGateAndTriggerOnNewStadiumAndExpiry() {
        val teams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val cities = InitialLeagueData.getDefaultCities()
        val chicago = teams.first { it.id == "team_chicago" }
            .copy(wins = 5, losses = 9, stadiumAge = 26, stadiumCondition = 44)

        val under500Offers = StadiumDealEngine.generateNamingRightsOffersForFranchise(
            franchise = chicago,
            seasonYear = 2026
        )
        assertEquals(3, under500Offers.size)
        val t1 = under500Offers.first { it.tier == 1 }
        val t2Locked = under500Offers.first { it.tier == 2 }
        val t3 = under500Offers.first { it.tier == 3 }
        assertTrue(t1.isEligible)
        assertFalse("Tier 2 should be locked without winning record", t2Locked.isEligible)
        assertTrue(t2Locked.requiresWinningRecord)
        assertTrue(t3.isEligible)
        assertEquals(20, t3.scandalRiskPct)
        assertTrue(t1.annualRevenueMillions < t2Locked.annualRevenueMillions)
        assertTrue(t2Locked.annualRevenueMillions < t3.annualRevenueMillions)

        val over500Offers = StadiumDealEngine.generateNamingRightsOffersForFranchise(
            franchise = chicago.copy(wins = 10, losses = 4),
            seasonYear = 2026
        )
        assertTrue("Tier 2 should unlock with winning record", over500Offers.first { it.tier == 2 }.isEligible)

        // New stadium built via ultimatum clears old sponsor and opens Naming Rights Offer modal
        val ultimatumState = StadiumDealState(
            activeUltimatum = StadiumUltimatumEvent(
                id = "ult_chi",
                franchiseId = chicago.id,
                franchiseName = chicago.teamFullName,
                cityName = chicago.cityName,
                ownerName = chicago.ownership.primaryOwner.fullName,
                ownershipGroupName = chicago.ownership.name,
                franchiseColorHex = chicago.colorHex,
                stadiumAge = 26,
                stadiumCondition = 44,
                stadiumCapacity = chicago.stadiumCapacity,
                seasonYear = 2026,
                headline = "Ultimatum",
                description = "Needs new arena"
            )
        )
        val rebuiltOutcome = StadiumDealEngine.resolveStadiumUltimatum(
            currentState = ultimatumState,
            franchises = teams.map { if (it.id == chicago.id) chicago else it },
            cities = cities,
            currentCashMillions = 90L,
            choice = StadiumUltimatumChoice.LOBBY_PUBLIC_TAX_FUNDING
        )
        assertTrue(rebuiltOutcome.success)
        assertTrue(rebuiltOutcome.builtNewStadium)
        val offerSession = rebuiltOutcome.updatedStadiumDealState.activeNamingRightsOffer
        assertNotNull("New stadium build should trigger Naming Rights Offer session", offerSession)
        assertEquals(NamingRightsTriggerReason.NEW_STADIUM_BUILT, offerSession!!.triggerReason)
        assertEquals(chicago.id, offerSession.franchiseId)

        // Trying to pick locked Tier 2 fails; picking Tier 1 succeeds
        val lockedPick = StadiumDealEngine.resolveNamingRightsOffer(
            currentState = rebuiltOutcome.updatedStadiumDealState,
            franchises = rebuiltOutcome.updatedFranchises,
            offerId = offerSession.offers.first { it.tier == 2 }.id
        )
        assertFalse(lockedPick.success)

        val tier1Pick = StadiumDealEngine.resolveNamingRightsOffer(
            currentState = rebuiltOutcome.updatedStadiumDealState,
            franchises = rebuiltOutcome.updatedFranchises,
            offerId = offerSession.offers.first { it.tier == 1 }.id
        )
        assertTrue(tier1Pick.success)
        assertNull(tier1Pick.updatedStadiumDealState.activeNamingRightsOffer)
        val signedChicago = tier1Pick.updatedFranchises.first { it.id == chicago.id }
        assertEquals(1, signedChicago.stadium.sponsorTier)
        assertTrue(signedChicago.stadium.namingRightsRevenue > 0L)
        assertTrue(signedChicago.stadium.sponsorYearsLeft > 0)

        // Expiring sponsorYearsLeft -> 0 in offseason also triggers Naming Rights Offer
        val expiringTeams = teams.mapIndexed { idx, team ->
            if (idx == 0) {
                team.copy(
                    stadium = TeamStadium(
                        sponsorName = "Expiring Corp",
                        sponsorTier = 1,
                        sponsorYearsLeft = 1,
                        namingRightsRevenue = 7L
                    ),
                    stadiumAge = 5,
                    stadiumCondition = 90
                )
            } else {
                team.copy(
                    stadium = TeamStadium("Steady Corp", 1, 4, 6L),
                    stadiumAge = 5,
                    stadiumCondition = 90
                )
            }
        }
        val expiryCheck = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = StadiumDealState(),
            franchises = expiringTeams,
            seasonYear = 2027,
            forceTier3ScandalRoll = false
        )
        assertNotNull(expiryCheck.updatedStadiumDealState.activeNamingRightsOffer)
        assertEquals(NamingRightsTriggerReason.CONTRACT_EXPIRED, expiryCheck.updatedStadiumDealState.activeNamingRightsOffer!!.triggerReason)
        assertEquals(expiringTeams.first().id, expiryCheck.updatedStadiumDealState.activeNamingRightsOffer!!.franchiseId)
        assertFalse(expiryCheck.updatedFranchises.first().stadium.hasActiveSponsor)
    }

    @Test
    fun tier3BankruptcyScandalCancelsDealDrainsPrForcesReplacementAndRevenueBoostsNextCap() {
        val teams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL).mapIndexed { idx, team ->
            if (idx == 0) {
                team.copy(
                    stadiumAge = 6,
                    stadiumCondition = 92,
                    stadium = TeamStadium(
                        sponsorName = "NovaVault Crypto",
                        sponsorTier = 3,
                        sponsorYearsLeft = 3,
                        namingRightsRevenue = 28L
                    )
                )
            } else {
                team.copy(
                    stadiumAge = 6,
                    stadiumCondition = 92,
                    stadium = TeamStadium(
                        sponsorName = "Civic Partner $idx",
                        sponsorTier = 1,
                        sponsorYearsLeft = 4,
                        namingRightsRevenue = 6L
                    )
                )
            }
        }

        val scandalCheck = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = StadiumDealState(),
            franchises = teams,
            seasonYear = 2027,
            forceTier3ScandalRoll = true
        )
        assertEquals(1, scandalCheck.triggeredBankruptcyScandals.size)
        assertEquals(StadiumDealEngine.TIER3_LEAGUE_PR_DRAIN, scandalCheck.totalLeaguePrDrain)
        val scandal = scandalCheck.updatedStadiumDealState.activeBankruptcyScandal
        assertNotNull(scandal)
        assertEquals("NovaVault Crypto", scandal!!.bankruptSponsorName)
        assertFalse("Bankrupt sponsor should be cancelled", scandalCheck.updatedFranchises.first().stadium.hasActiveSponsor)
        assertEquals(0L, scandalCheck.updatedFranchises.first().stadium.namingRightsRevenue)

        val offerAfterScandal = scandalCheck.updatedStadiumDealState.activeNamingRightsOffer
        assertNotNull(offerAfterScandal)
        assertEquals(NamingRightsTriggerReason.BANKRUPTCY_SCANDAL, offerAfterScandal!!.triggerReason)

        // Verify namingRightsRevenue increases Franchise statement + annual league revenue + next season's Salary Cap
        val union = LaborUnionAgreement(salaryCapMillions = 80L)
        val broadcast = LeagueUiState().activeBroadcast
        val lowNamingTeams = teams.map {
            it.copy(stadium = TeamStadium("Civic Safe", 1, 3, 2L))
        }
        val highNamingTeams = teams.map {
            it.copy(stadium = TeamStadium("Titan Deals", 3, 3, 26L))
        }
        val lowProj = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(lowNamingTeams, broadcast, union)
        val highProj = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(highNamingTeams, broadcast, union)
        assertTrue(highProj.totalNamingRightsRevenueMillions > lowProj.totalNamingRightsRevenueMillions)
        assertTrue(highProj.annualLeagueRevenueMillions > lowProj.annualLeagueRevenueMillions)
        assertTrue(
            "Higher naming rights revenue should raise next season Salary Cap (${highProj.nextSalaryCapMillions} vs ${lowProj.nextSalaryCapMillions})",
            highProj.nextSalaryCapMillions > lowProj.nextSalaryCapMillions
        )

        val stmtWithDeal = highNamingTeams.first().calculateFinancialStatement(broadcast, highNamingTeams.size)
        assertEquals(26.0, stmtWithDeal.namingRightsRevenueMillions, 0.01)
        assertTrue(stmtWithDeal.localSponsorRevenueMillions >= 26.0)
    }

    @Test
    fun repositoryAndModalsSupportFullNamingRightsAndScandalWorkflow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        val baseState = LeagueUiState()
        val sponsoredTeam = baseState.franchises.first().copy(
            wins = 11,
            losses = 3,
            stadium = TeamStadium(
                sponsorName = "ApexCore Semiconductors",
                sponsorTier = 2,
                sponsorYearsLeft = 4,
                namingRightsRevenue = 14L
            )
        )
        val restoredTeam = repo.deserializeFranchises(
            repo.serializeFranchises(listOf(sponsoredTeam) + baseState.franchises.drop(1)),
            fallbackYear = 2026
        ).first()
        assertEquals("ApexCore Semiconductors", restoredTeam.stadium.sponsorName)
        assertEquals(2, restoredTeam.stadium.sponsorTier)
        assertEquals(4, restoredTeam.stadium.sponsorYearsLeft)
        assertEquals(14L, restoredTeam.stadium.namingRightsRevenue)

        val session = StadiumDealEngine.buildNamingRightsOfferSession(
            franchise = sponsoredTeam,
            seasonYear = 2027,
            triggerReason = NamingRightsTriggerReason.BANKRUPTCY_SCANDAL,
            previousSponsorName = "NovaVault Crypto",
            scandalPrDrain = 9
        )
        val scandal = SponsorBankruptcyScandalEvent(
            id = "scandal_1",
            franchiseId = sponsoredTeam.id,
            franchiseName = sponsoredTeam.teamFullName,
            cityName = sponsoredTeam.cityName,
            bankruptSponsorName = "NovaVault Crypto",
            lostAnnualRevenueMillions = 25L,
            seasonYear = 2027,
            leaguePrDrain = 9,
            fanMoraleDrain = 4,
            headline = "Scandal",
            description = "NovaVault collapsed."
        )
        val dealState = StadiumDealState(
            activeNamingRightsOffer = session,
            activeBankruptcyScandal = scandal
        )
        repo.saveStadiumDealState(dealState)
        val loadedDealState = repo.loadStadiumDealState()
        assertNotNull(loadedDealState)
        assertEquals("NovaVault Crypto", loadedDealState!!.activeBankruptcyScandal!!.bankruptSponsorName)
        assertEquals(3, loadedDealState.activeNamingRightsOffer!!.offers.size)

        // Compose UI test: Scandal Modal -> Pick Replacement -> Naming Rights Modal -> Select Tier 2 -> Sign
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<LeagueUiState>
        flow.value = vm.uiState.value.copy(
            franchises = vm.uiState.value.franchises.map { if (it.id == sponsoredTeam.id) sponsoredTeam else it },
            stadiumDealState = dealState
        )

        composeTestRule.setContent {
            val ui by vm.uiState.collectAsState()
            MyApplicationTheme {
                ui.stadiumDealState.activeBankruptcyScandal?.let { sc ->
                    SponsorBankruptcyScandalModal(
                        scandal = sc,
                        onPickReplacement = { vm.acknowledgeSponsorBankruptcyScandal() }
                    )
                }
                if (ui.stadiumDealState.activeBankruptcyScandal == null && ui.stadiumDealState.activeNamingRightsOffer != null) {
                    NamingRightsOfferModal(
                        session = ui.stadiumDealState.activeNamingRightsOffer!!,
                        onSelectSponsor = { id -> vm.selectNamingRightsOffer(id) }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sponsor_bankruptcy_scandal_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_scandal_pick_replacement").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("naming_rights_offer_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_naming_offer_tier_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_confirm_naming_rights").performClick()
        composeTestRule.waitForIdle()

        assertNull(vm.uiState.value.stadiumDealState.activeNamingRightsOffer)
        val updatedTeam = vm.uiState.value.franchises.first { it.id == sponsoredTeam.id }
        assertEquals(2, updatedTeam.stadium.sponsorTier)
        assertTrue(updatedTeam.stadium.namingRightsRevenue > 0L)
    }
}
