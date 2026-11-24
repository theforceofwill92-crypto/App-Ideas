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
import com.example.ui.components.RivalAbsorptionNegotiationModal
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
class RivalLeagueAbsorptionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun richFreeAgentPool(): List<FreeAgentPlayer> = (1..16).map { i ->
        FreeAgentPlayer(
            id = "fa_pool_$i",
            name = "Star Agent $i",
            position = listOf("PG", "SG", "SF", "PF", "C")[i % 5],
            overallRating = 90 - i,
            age = 24 + (i % 6),
            archetype = "All-League Anchor",
            experienceYears = 5,
            askingSalaryMillions = (22L - i).coerceAtLeast(5L),
            preferredYears = 3,
            formerFranchiseName = "Free Agent Pool",
            bioTag = "Prime market target"
        )
    }

    @Test
    fun absorptionTriggersOnlyAfterThreeToFourSeasonsWithHighHype() {
        val cities = InitialLeagueData.getDefaultCities()
        var rival = RivalLeagueEngine.createDefaultRivalLeague(SportType.BASKETBALL).copy(
            capital = 240L,
            hype = 66
        )
        var pool = richFreeAgentPool()

        // Season 1 -> 1 season survived: must NOT trigger
        val r1 = RivalLeagueEngine.processOffseasonPoaching(rival, pool, 2026, cities)
        rival = r1.updatedRivalLeague
        pool = r1.updatedFreeAgentPool
        assertEquals(1, rival.seasonsSurvived)
        assertNull(r1.triggeredAbsorptionNegotiation)
        assertNull(rival.activeAbsorptionNegotiation)

        // Season 2 -> 2 seasons survived: must NOT trigger
        val r2 = RivalLeagueEngine.processOffseasonPoaching(rival, pool, 2027, cities)
        rival = r2.updatedRivalLeague
        pool = r2.updatedFreeAgentPool
        assertEquals(2, rival.seasonsSurvived)
        assertNull(r2.triggeredAbsorptionNegotiation)
        assertNull(rival.activeAbsorptionNegotiation)

        // Season 3 with low hype should NOT trigger
        val lowHypeRival = rival.copy(hype = 45, capital = 180L)
        val r3Low = RivalLeagueEngine.processOffseasonPoaching(lowHypeRival, pool, 2028, cities)
        assertEquals(3, r3Low.updatedRivalLeague.seasonsSurvived)
        assertNull(r3Low.triggeredAbsorptionNegotiation)

        // Season 3 with sustained high hype (>= 70 & 2+ high-hype seasons) DOES trigger
        val r3High = RivalLeagueEngine.processOffseasonPoaching(rival, pool, 2028, cities)
        val event = r3High.triggeredAbsorptionNegotiation
        assertNotNull(event)
        assertEquals(3, r3High.updatedRivalLeague.seasonsSurvived)
        assertTrue(r3High.updatedRivalLeague.hype >= RivalLeagueEngine.HIGH_HYPE_TRIGGER_THRESHOLD)
        assertEquals(4, event!!.candidateTeams.size)
        assertEquals(3, event.selectedCount)
        assertTrue(event.canApproveAbsorption)

        // Candidate cities must not duplicate already occupied league cities
        val occupiedNames = cities.filter { city -> city.hasFranchise }.map { city -> city.name.lowercase() }.toSet()
        assertTrue(event.candidateTeams.none { cand -> cand.cityName.lowercase() in occupiedNames })
    }

    @Test
    fun candidateToggleEnforcesTwoToFourRange() {
        val cities = InitialLeagueData.getDefaultCities()
        val rivalWithEvent = RivalLeagueEngine.createDefaultRivalLeague(SportType.BASKETBALL).copy(
            seasonsSurvived = 4,
            highHypeSeasonsCount = 3,
            hype = 82,
            capital = 110L
        )
        val event = RivalLeagueEngine.buildAbsorptionNegotiationEvent(rivalWithEvent, cities, 2029)
        var state = rivalWithEvent.copy(activeAbsorptionNegotiation = event)
        assertEquals(3, state.activeAbsorptionNegotiation!!.selectedCount)

        // Select 4th team -> 4 selected
        val unselected = state.activeAbsorptionNegotiation!!.candidateTeams.first { !it.isSelected }
        state = RivalLeagueEngine.toggleAbsorptionCandidate(state, unselected.id)
        assertEquals(4, state.activeAbsorptionNegotiation!!.selectedCount)

        // Deselect two teams -> 2 selected (minimum)
        val firstSelected = state.activeAbsorptionNegotiation!!.candidateTeams[0]
        val secondSelected = state.activeAbsorptionNegotiation!!.candidateTeams[1]
        state = RivalLeagueEngine.toggleAbsorptionCandidate(state, firstSelected.id)
        state = RivalLeagueEngine.toggleAbsorptionCandidate(state, secondSelected.id)
        assertEquals(2, state.activeAbsorptionNegotiation!!.selectedCount)

        // Attempting to go below 2 is blocked
        val thirdSelected = state.activeAbsorptionNegotiation!!.candidateTeams[2]
        state = RivalLeagueEngine.toggleAbsorptionCandidate(state, thirdSelected.id)
        assertEquals(2, state.activeAbsorptionNegotiation!!.selectedCount)
    }

    @Test
    fun acceptingAndRejectingAbsorptionProduceExpectedTradeoffs() {
        val sport = SportType.BASKETBALL
        val cities = InitialLeagueData.getDefaultCities()
        val franchises = InitialLeagueData.getInitialFranchises(sport)
        val ruleBook = LeagueRuleBook()
        val pool = richFreeAgentPool()
        val poisedRival = RivalLeagueEngine.createDefaultRivalLeague(sport).copy(
            seasonsSurvived = 4,
            highHypeSeasonsCount = 3,
            hype = 84,
            capital = 95L,
            poachedPlayers = pool.take(4)
        )
        val event = RivalLeagueEngine.buildAbsorptionNegotiationEvent(poisedRival, cities, 2029)
        val readyRival = poisedRival.copy(activeAbsorptionNegotiation = event)

        // Reject branch
        val rejected = RivalLeagueEngine.resolveAbsorptionNegotiation(
            rivalLeague = readyRival,
            existingFranchises = franchises,
            existingCities = cities,
            ruleBook = ruleBook,
            freeAgentPool = pool.drop(4),
            sportType = sport,
            seasonYear = 2029,
            acceptAbsorption = false
        )
        assertTrue(rejected.success)
        assertFalse(rejected.absorbed)
        assertTrue(rejected.updatedRivalLeague.isActive)
        assertEquals(110L, rejected.updatedRivalLeague.capital)
        assertEquals(3, rejected.ownerApprovalDelta)
        assertEquals(0L, rejected.permanentTvBoostMillions)
        assertNull(rejected.updatedRivalLeague.activeAbsorptionNegotiation)
        assertTrue(rejected.updatedRivalLeague.hasResolvedAbsorptionEvent)

        // Accept branch with 4 teams selected
        val fourthId = event.candidateTeams.first { !it.isSelected }.id
        val fourSelectedRival = RivalLeagueEngine.toggleAbsorptionCandidate(readyRival, fourthId)
        val absorbed = RivalLeagueEngine.resolveAbsorptionNegotiation(
            rivalLeague = fourSelectedRival,
            existingFranchises = franchises,
            existingCities = cities,
            ruleBook = ruleBook,
            freeAgentPool = pool.drop(4),
            sportType = sport,
            seasonYear = 2029,
            acceptAbsorption = true
        )
        assertTrue(absorbed.success)
        assertTrue(absorbed.absorbed)
        assertEquals(4, absorbed.absorbedFranchises.size)
        assertTrue(absorbed.absorbedFranchises.all { it.roster.isNotEmpty() && it.draftPicks.isNotEmpty() })
        assertFalse(absorbed.updatedRivalLeague.isActive)
        assertEquals(0L, absorbed.updatedRivalLeague.capital)
        assertTrue(absorbed.ownerApprovalDelta <= -20)
        assertTrue(absorbed.permanentTvBoostMillions >= 60L)
        assertEquals(absorbed.permanentTvBoostMillions, absorbed.updatedRivalLeague.permanentTvRevenueBoostMillions)
        assertEquals(4, absorbed.updatedRivalLeague.absorbedTeamNames.size)
    }

    @Test
    fun repositoryPersistsAbsorptionNegotiationAndPermanentTvBoost() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        val cities = InitialLeagueData.getDefaultCities()
        val base = RivalLeagueEngine.createDefaultRivalLeague(SportType.FOOTBALL).copy(
            seasonsSurvived = 4,
            highHypeSeasonsCount = 2,
            hype = 79,
            capital = 88L,
            permanentTvRevenueBoostMillions = 68L,
            hasResolvedAbsorptionEvent = false,
            absorbedTeamNames = listOf("Austin Outlaws")
        )
        val event = RivalLeagueEngine.buildAbsorptionNegotiationEvent(base, cities, 2030)
        val original = base.copy(activeAbsorptionNegotiation = event)

        repo.saveRivalLeague(original)
        val loaded = repo.loadRivalLeague()
        assertNotNull(loaded)
        assertEquals(4, loaded!!.seasonsSurvived)
        assertEquals(2, loaded.highHypeSeasonsCount)
        assertEquals(68L, loaded.permanentTvRevenueBoostMillions)
        assertEquals(listOf("Austin Outlaws"), loaded.absorbedTeamNames)
        assertNotNull(loaded.activeAbsorptionNegotiation)
        assertEquals(4, loaded.activeAbsorptionNegotiation!!.candidateTeams.size)
        assertEquals(event.totalPermanentTvBoostMillions, loaded.activeAbsorptionNegotiation!!.totalPermanentTvBoostMillions)
    }

    @Test
    fun modalAndViewModelAbsorbTeamsAndLockPermanentTvRevenueBoostAcrossFutureDeals() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("apex_league_data", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("apex_league_settings", Context.MODE_PRIVATE).edit().clear().commit()

        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val initialState = vm.uiState.value
        val initialTeamCount = initialState.franchises.size
        val initialBroadcastPayout = initialState.activeBroadcast.annualPayoutMillions
        val initialOwnerApproval = initialState.ownerApprovalRating

        // Simulate rival surviving 3 seasons with high hype into the 4th offseason trigger
        val pool = initialState.freeAgentPool.ifEmpty { richFreeAgentPool() }
        val r3 = initialState.rivalLeague.copy(
            seasonsSurvived = 3,
            highHypeSeasonsCount = 2,
            capital = 160L,
            hype = 80,
            lastPoachedSeasonYear = initialState.currentYear - 1
        )
        val poachTrigger = RivalLeagueEngine.processOffseasonPoaching(
            rivalLeague = r3,
            freeAgentPool = pool,
            seasonYear = initialState.currentYear,
            existingCities = initialState.cities
        )
        assertNotNull(poachTrigger.triggeredAbsorptionNegotiation)

        val repo = LeagueRepository(app)
        repo.saveRivalLeague(poachTrigger.updatedRivalLeague)
        val vmWithEvent = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val activeEvent = vmWithEvent.uiState.value.rivalLeague.activeAbsorptionNegotiation
        assertNotNull(activeEvent)

        composeTestRule.setContent {
            MyApplicationTheme {
                val state by vmWithEvent.uiState.collectAsState()
                state.rivalLeague.activeAbsorptionNegotiation?.let { ev ->
                    RivalAbsorptionNegotiationModal(
                        event = ev,
                        onToggleCandidate = { id -> vmWithEvent.toggleRivalAbsorptionCandidate(id) },
                        onAcceptAbsorption = { vmWithEvent.resolveRivalAbsorptionNegotiation(acceptAbsorption = true) },
                        onRejectMerger = { vmWithEvent.resolveRivalAbsorptionNegotiation(acceptAbsorption = false) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("rival_absorption_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("absorption_selection_counter").assertTextEquals("Select 2–4 teams (3 chosen)")

        // Click the 4th unselected candidate to absorb all 4 top rival clubs
        val fourthCandidate = activeEvent!!.candidateTeams.first { !it.isSelected }
        composeTestRule.onNodeWithTag("absorption_candidate_${fourthCandidate.id}")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("absorption_selection_counter").assertTextEquals("Select 2–4 teams (4 chosen)")

        val expectedBoost = vmWithEvent.uiState.value.rivalLeague.activeAbsorptionNegotiation!!.totalPermanentTvBoostMillions
        val expectedOwnerHit = vmWithEvent.uiState.value.rivalLeague.activeAbsorptionNegotiation!!.totalOwnerApprovalHit

        composeTestRule.onNodeWithTag("btn_absorb_rival_teams").performClick()
        composeTestRule.waitForIdle()

        val postState = vmWithEvent.uiState.value
        assertNull(postState.rivalLeague.activeAbsorptionNegotiation)
        assertFalse(postState.rivalLeague.isActive)
        assertEquals(initialTeamCount + 4, postState.franchises.size)
        assertEquals(expectedBoost, postState.rivalLeague.permanentTvRevenueBoostMillions)
        assertEquals(initialBroadcastPayout + expectedBoost, postState.activeBroadcast.annualPayoutMillions)
        assertEquals((initialOwnerApproval + expectedOwnerHit).coerceIn(15, 100), postState.ownerApprovalRating)

        // Verify permanent TV boost carries onto any newly negotiated future TV contract
        val nextDeal = BroadcastContract(
            id = "deal_future_test",
            partnerName = "Apex Global Network",
            platform = BroadcastPlatform.GLOBAL_STREAMER,
            annualPayoutMillions = 120L,
            yearsRemaining = 4,
            reachMultiplier = 1.4,
            bonusPerPlayoffGameMillions = 3L,
            networkScope = NetworkScope.GLOBAL_STREAMING
        )
        vmWithEvent.signNegotiatedBroadcastContract(nextDeal, lumpSumBonusMillions = 15L)
        assertEquals(120L + expectedBoost, vmWithEvent.uiState.value.activeBroadcast.annualPayoutMillions)
    }
}
