package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.*
import com.example.ui.components.RelocationVoteModal
import com.example.ui.components.StadiumUltimatumCard
import com.example.ui.theme.MyApplicationTheme
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
class StadiumDealRelocationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun offseasonTriggersUltimatumOnlyWhenStadiumAgeOver25AndConditionUnder50() {
        val baseTeams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL).mapIndexed { idx, team ->
            when (idx) {
                0 -> team.copy(stadiumAge = 25, stadiumCondition = 51) // ticks -> age 26 (>25), cond <50 -> triggers!
                1 -> team.copy(stadiumAge = 25, stadiumCondition = 80) // ticks -> age 26 (>25), cond >=50 -> no trigger
                else -> team.copy(stadiumAge = 10, stadiumCondition = 40) // ticks -> age 11 (<=25), cond <50 -> no trigger
            }
        }

        val check = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = StadiumDealState(),
            franchises = baseTeams,
            seasonYear = 2026
        )

        assertEquals(1, check.triggeredUltimatums.size)
        val triggered = check.triggeredUltimatums.first()
        assertEquals(baseTeams[0].id, triggered.franchiseId)
        assertEquals(26, triggered.stadiumAge)
        assertTrue(triggered.stadiumCondition < 50)
        assertEquals(triggered, check.updatedStadiumDealState.activeUltimatum)

        // Same year idempotency check
        val repeatCheck = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = check.updatedStadiumDealState,
            franchises = check.updatedFranchises,
            seasonYear = 2026
        )
        assertTrue(repeatCheck.triggeredUltimatums.isEmpty())
    }

    @Test
    fun ultimatumChoicesApplyPublicPrivateFiftyPercentThreatAndLeagueSubsidy() {
        val teams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val cities = InitialLeagueData.getDefaultCities()
        val chicago = teams.first { it.id == "team_chicago" }

        val check = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = StadiumDealState(),
            franchises = teams,
            seasonYear = 2026
        )
        assertNotNull(check.updatedStadiumDealState.activeUltimatum)

        // Choice 1: Public Taxpayer Funding (+Owner, -Fan, builds new stadium)
        val pubOutcome = StadiumDealEngine.resolveStadiumUltimatum(
            currentState = check.updatedStadiumDealState,
            franchises = check.updatedFranchises,
            cities = cities,
            currentCashMillions = 90L,
            choice = StadiumUltimatumChoice.LOBBY_PUBLIC_TAX_FUNDING
        )
        assertTrue(pubOutcome.success)
        assertTrue(pubOutcome.builtNewStadium)
        assertFalse(pubOutcome.triggeredRelocationThreat)
        assertTrue(pubOutcome.ownerApprovalDelta > 0)
        assertTrue(pubOutcome.fanSentimentDelta < 0)
        val pubTeam = pubOutcome.updatedFranchises.first { it.id == chicago.id }
        assertEquals(0, pubTeam.stadiumAge)
        assertEquals(100, pubTeam.stadiumCondition)

        // Choice 3: Spend League Funds (-League Cash, +Owner, builds new stadium)
        val insufficient = StadiumDealEngine.resolveStadiumUltimatum(
            currentState = check.updatedStadiumDealState,
            franchises = check.updatedFranchises,
            cities = cities,
            currentCashMillions = 10L,
            choice = StadiumUltimatumChoice.SUBSIDIZE_WITH_LEAGUE_FUNDS
        )
        assertFalse(insufficient.success)

        val subOutcome = StadiumDealEngine.resolveStadiumUltimatum(
            currentState = check.updatedStadiumDealState,
            franchises = check.updatedFranchises,
            cities = cities,
            currentCashMillions = 90L,
            choice = StadiumUltimatumChoice.SUBSIDIZE_WITH_LEAGUE_FUNDS
        )
        assertTrue(subOutcome.success)
        assertTrue(subOutcome.builtNewStadium)
        assertEquals(-28L, subOutcome.cashDeltaMillions)
        assertTrue(subOutcome.ownerApprovalDelta > 0)
        val subTeam = subOutcome.updatedFranchises.first { it.id == chicago.id }
        assertEquals(0, subTeam.stadiumAge)
        assertEquals(100, subTeam.stadiumCondition)

        // Choice 2 (safe roll): +Fans, -Owner, builds new stadium locally
        val safePriv = StadiumDealEngine.resolveStadiumUltimatum(
            currentState = check.updatedStadiumDealState,
            franchises = check.updatedFranchises,
            cities = cities,
            currentCashMillions = 90L,
            choice = StadiumUltimatumChoice.FORCE_OWNER_PRIVATE_FUNDING,
            forceRelocationRoll = false
        )
        assertTrue(safePriv.success)
        assertFalse(safePriv.triggeredRelocationThreat)
        assertTrue(safePriv.builtNewStadium)
        assertTrue(safePriv.fanSentimentDelta > 0)
        assertTrue(safePriv.ownerApprovalDelta < 0)

        // Choice 2 (threat roll): triggers Relocation Vote with 3 cities
        val threatPriv = StadiumDealEngine.resolveStadiumUltimatum(
            currentState = check.updatedStadiumDealState,
            franchises = check.updatedFranchises,
            cities = cities,
            currentCashMillions = 90L,
            choice = StadiumUltimatumChoice.FORCE_OWNER_PRIVATE_FUNDING,
            forceRelocationRoll = true
        )
        assertTrue(threatPriv.success)
        assertTrue(threatPriv.triggeredRelocationThreat)
        assertFalse(threatPriv.builtNewStadium)
        val relocVote = threatPriv.updatedStadiumDealState.activeRelocationVote
        assertNotNull(relocVote)
        assertEquals(3, relocVote!!.cityDeals.size)
        assertTrue(relocVote.cityDeals.all { it.cityName.isNotBlank() && it.newStadiumCapacity >= 20500 })

        // Approve Relocation -> moves to 2nd offered city with brand new stadium
        val targetDeal = relocVote.cityDeals[1]
        val approved = StadiumDealEngine.resolveRelocationVote(
            currentState = threatPriv.updatedStadiumDealState,
            franchises = threatPriv.updatedFranchises,
            cities = cities,
            ruleBook = LeagueRuleBook(),
            approve = true,
            selectedCityIdOverride = targetDeal.cityId
        )
        assertTrue(approved.success)
        assertTrue(approved.approved)
        assertNull(approved.updatedStadiumDealState.activeRelocationVote)
        val movedTeam = approved.updatedFranchises.first { it.id == relocVote.franchiseId }
        assertEquals(targetDeal.cityName, movedTeam.cityName)
        assertEquals(0, movedTeam.stadiumAge)
        assertEquals(100, movedTeam.stadiumCondition)
        assertTrue(approved.updatedCities.first { it.id == targetDeal.cityId }.hasFranchise)
        assertFalse(approved.updatedCities.first { it.id == relocVote.currentCityId }.hasFranchise)

        // Veto Relocation -> stays in old city, -Owner, +Fans, local stabilization
        val vetoed = StadiumDealEngine.resolveRelocationVote(
            currentState = threatPriv.updatedStadiumDealState,
            franchises = threatPriv.updatedFranchises,
            cities = cities,
            ruleBook = LeagueRuleBook(),
            approve = false
        )
        assertTrue(vetoed.success)
        assertFalse(vetoed.approved)
        assertNull(vetoed.updatedStadiumDealState.activeRelocationVote)
        val stayedTeam = vetoed.updatedFranchises.first { it.id == relocVote.franchiseId }
        assertEquals(relocVote.currentCityName, stayedTeam.cityName)
        assertTrue(vetoed.ownerApprovalDelta < 0)
        assertTrue(vetoed.fanSentimentDelta > 0)
    }

    @Test
    fun repositoryPersistsStadiumFieldsAndDealStateRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)

        val teams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val agedTeams = teams.mapIndexed { i, f ->
            if (i == 0) f.copy(stadiumAge = 29, stadiumCondition = 39) else f
        }
        val json = repo.serializeFranchises(agedTeams)
        val loadedTeams = repo.deserializeFranchises(json)
        assertEquals(29, loadedTeams[0].stadiumAge)
        assertEquals(39, loadedTeams[0].stadiumCondition)

        val dealState = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = StadiumDealState(),
            franchises = teams,
            seasonYear = 2026
        ).updatedStadiumDealState
        repo.saveStadiumDealState(dealState)
        val loadedState = repo.loadStadiumDealState()
        assertNotNull(loadedState)
        assertNotNull(loadedState!!.activeUltimatum)
        assertEquals(dealState.activeUltimatum!!.franchiseId, loadedState.activeUltimatum!!.franchiseId)
        assertEquals(2026, loadedState.lastTickedSeasonYear)
    }

    @Test
    fun uiFlowShowsUltimatumCardAndRelocationVoteModalEndToEnd() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("apex_league_data", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("apex_league_settings", Context.MODE_PRIVATE).edit().clear().commit()

        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Trigger off-season rollover so aging teams hit stadiumAge > 25 && stadiumCondition < 50
        vm.finalizeAndInaugurateNewSeason()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val afterOffseason = vm.uiState.value
        val activeUlt = afterOffseason.stadiumDealState.activeUltimatum
        assertNotNull("Expected Stadium Ultimatum after first offseason", activeUlt)

        composeTestRule.setContent {
            MyApplicationTheme {
                val state by vm.uiState.collectAsState()
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    state.stadiumDealState.activeUltimatum?.let { ult ->
                        StadiumUltimatumCard(
                            ultimatum = ult,
                            leagueCashMillions = state.cashReservesMillions,
                            onSelectChoice = { choice ->
                                vm.resolveStadiumUltimatumChoice(
                                    choice = choice,
                                    forceRelocationRoll = if (choice == StadiumUltimatumChoice.FORCE_OWNER_PRIVATE_FUNDING) true else null
                                )
                            }
                        )
                    }
                }
                state.stadiumDealState.activeRelocationVote?.let { vote ->
                    RelocationVoteModal(
                        voteState = vote,
                        onSelectCity = { id -> vm.selectRelocationVoteCity(id) },
                        onApproveRelocation = { vm.resolveRelocationVote(approve = true) },
                        onVetoRelocation = { vm.resolveRelocationVote(approve = false) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("stadium_ultimatum_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stadium_ultimatum_age").assertTextContains("26y", substring = true)

        // Click Choice 2: Force Owner Private Funding (forced 50% threat roll = true)
        composeTestRule.onNodeWithTag("btn_ultimatum_FORCE_OWNER_PRIVATE_FUNDING")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Relocation Vote Modal opens with 3 city deals
        composeTestRule.onNodeWithTag("relocation_vote_modal").assertIsDisplayed()
        val vote = vm.uiState.value.stadiumDealState.activeRelocationVote!!
        assertEquals(3, vote.cityDeals.size)
        val secondCity = vote.cityDeals[1]

        composeTestRule.onNodeWithTag("card_relocation_city_${secondCity.cityId}")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(secondCity.cityId, vm.uiState.value.stadiumDealState.activeRelocationVote!!.selectedCityId)

        // Approve Relocation -> moves team and resets stadium to 0 age / 100 cond
        composeTestRule.onNodeWithTag("btn_approve_relocation").performClick()
        composeTestRule.waitForIdle()

        assertNull(vm.uiState.value.stadiumDealState.activeRelocationVote)
        val movedTeam = vm.uiState.value.franchises.first { it.id == vote.franchiseId }
        assertEquals(secondCity.cityName, movedTeam.cityName)
        assertEquals(0, movedTeam.stadiumAge)
        assertEquals(100, movedTeam.stadiumCondition)
    }
}
