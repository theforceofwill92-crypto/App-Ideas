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
import com.example.ui.screens.OffSeasonSummitScreen
import com.example.ui.screens.ScheduleCalendarScreen
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
class ScheduleCalendarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun generateScheduleFlipSwapMoveHolidayAndInternationalWorkCleanly() {
        val teams = InitialLeagueData.getInitialFranchises()
        val cities = InitialLeagueData.getDefaultCities()
        val rivalries = RivalryEngine.seedInitialRivalries(teams, cities, 2026)
        val union = LaborUnionAgreement()

        val plan = ScheduleCalendarEngine.generateFullSeasonSchedule(
            targetYear = 2027,
            regularSeasonWeeks = 14,
            franchises = teams,
            rivalries = rivalries,
            cities = cities,
            sportType = SportType.BASKETBALL
        )

        assertEquals(14, plan.regularSeasonWeeks)
        assertFalse(plan.hasValidationErrors)
        assertTrue(plan.holidayShowcaseCount >= 2)
        assertTrue(plan.internationalGameCount >= 1)
        assertEquals(14 * (teams.size / 2), plan.slots.size)

        // Every week has zero duplicate teams
        for (w in 1..14) {
            val wSlots = plan.slotsForWeek(w)
            val seen = wSlots.flatMap { listOf(it.homeTeamId, it.awayTeamId) }
            assertEquals(seen.size, seen.distinct().size)
        }

        // Flip home/away on week 2 first game
        val w2Slot = plan.slotsForWeek(2).first()
        val flipped = ScheduleCalendarEngine.flipHomeAway(plan, w2Slot.id, teams, union)
        val flippedSlot = flipped.slots.first { it.id == w2Slot.id }
        assertEquals(w2Slot.awayTeamId, flippedSlot.homeTeamId)
        assertEquals(w2Slot.homeTeamId, flippedSlot.awayTeamId)
        assertFalse(flipped.hasValidationErrors)

        // Swap team cleanly without causing week conflict
        val outsider = teams.first { it.id != flippedSlot.homeTeamId && it.id != flippedSlot.awayTeamId }
        val swapped = ScheduleCalendarEngine.swapMatchupTeam(
            plan = flipped,
            slotId = flippedSlot.id,
            replaceHome = false,
            newTeamId = outsider.id,
            franchises = teams,
            rivalries = rivalries,
            unionAgreement = union
        )
        assertEquals(outsider.id, swapped.slots.first { it.id == flippedSlot.id }.awayTeamId)
        assertFalse(swapped.hasValidationErrors)

        // Assign Thanksgiving Holiday Rivalry + Tokyo International Showcase
        val w4Slot = swapped.slotsForWeek(4).first()
        val holidayAssigned = ScheduleCalendarEngine.setHolidayMarqueeOnSlot(
            plan = swapped,
            slotId = w4Slot.id,
            tentpole = HolidayTentpole.THANKSGIVING_SLATE,
            customRivalryTitle = "Harvest Hardwood Classic",
            franchises = teams,
            rivalries = rivalries,
            unionAgreement = union
        )
        val intlAssigned = ScheduleCalendarEngine.assignInternationalVenueToSlot(
            plan = holidayAssigned,
            slotId = w4Slot.id,
            venueId = "intl_tokyo",
            franchises = teams,
            unionAgreement = union
        )
        val finalSlot = intlAssigned.slots.first { it.id == w4Slot.id }
        assertEquals(HolidayTentpole.THANKSGIVING_SLATE, finalSlot.holidayTentpole)
        assertTrue(finalSlot.isMarqueeRivalry)
        assertEquals("Harvest Hardwood Classic", finalSlot.rivalryTitle)
        assertEquals("intl_tokyo", finalSlot.internationalVenueId)
        assertTrue(finalSlot.projectedTvBonusMillions > 8.0)

        // Move matchup to Week 5 cleanly
        val moved = ScheduleCalendarEngine.moveMatchToWeek(
            plan = intlAssigned,
            slotId = finalSlot.id,
            targetWeek = 5,
            franchises = teams,
            unionAgreement = union
        )
        assertEquals(5, moved.slots.first { it.id == finalSlot.id }.week)
        assertFalse(moved.hasValidationErrors)
    }

    @Test
    fun expandableRegularSeasonVoteUpdatesRuleBookScheduleAndSimulationPlayoffThreshold() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(14, vm.uiState.value.ruleBook.regularSeasonWeeks)
        assertEquals(16, vm.uiState.value.totalWeeks)

        // Vote to expand regular season from 14 -> 18 weeks
        val vote = vm.proposeRegularSeasonLengthVote(18)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        if (!vote.passed) {
            vm.overrideLastVetoedVote()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }

        val expandedState = vm.uiState.value
        assertEquals(18, expandedState.ruleBook.regularSeasonWeeks)
        assertEquals(20, expandedState.totalWeeks)
        assertEquals(18, expandedState.seasonScheduleState.activeSeasonSchedule?.regularSeasonWeeks)

        // Submit post-draft schedule for approval with custom Holiday + London International game
        vm.initializeOrRefreshUpcomingSchedule()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val upcoming = vm.uiState.value.seasonScheduleState.upcomingPlan!!
        assertEquals(18, upcoming.regularSeasonWeeks)

        val w2Slot = upcoming.slotsForWeek(2).first()
        vm.setHolidayMarqueeOnUpcomingSlot(w2Slot.id, HolidayTentpole.NEW_YEAR_CLASSIC, "Millennium Derby")
        vm.setInternationalShowcaseOnUpcomingSlot(w2Slot.id, "intl_london")
        val schedVote = vm.submitUpcomingScheduleForApproval()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        if (!schedVote.passed) {
            vm.overrideLastVetoedVote()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue(vm.uiState.value.seasonScheduleState.upcomingPlan!!.isApproved)

        // Repository persistence round-trip for season length & schedule
        val repo = LeagueRepository(context)
        repo.saveLeagueSettings(vm.uiState.value.ruleBook)
        repo.saveSeasonScheduleState(vm.uiState.value.seasonScheduleState)
        val loadedRules = repo.loadLeagueSettings()!!
        val loadedSchedule = repo.loadSeasonScheduleState()!!
        assertEquals(18, loadedRules.regularSeasonWeeks)
        assertTrue(loadedSchedule.upcomingPlan!!.isApproved)
        assertEquals("intl_london", loadedSchedule.upcomingPlan!!.slots.first { it.id == w2Slot.id }.internationalVenueId)

        // Simulate past Week 14 into Week 16 -> verify League remains in REGULAR_SEASON (not early Playoffs!)
        vm.openOffSeasonSummit(false)
        vm.dismissBoardVoteModal()
        repeat(14) {
            if (vm.uiState.value.activeDilemma != null) {
                vm.resolveDilemma(0)
            }
            if (vm.uiState.value.activeLaborNegotiationOverlay != null) {
                vm.dismissLaborNegotiationOverlay()
            }
            vm.advanceWeek()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue(
            "Expanded 18-week season must stay in regular/all-star play past Week 14 (was ${vm.uiState.value.seasonPhase} at week ${vm.uiState.value.currentWeek})",
            vm.uiState.value.seasonPhase == SeasonPhase.REGULAR_SEASON ||
                vm.uiState.value.seasonPhase == SeasonPhase.ALL_STAR_INTERNATIONAL
        )
        assertTrue(vm.uiState.value.playoffMatches.isEmpty())
    }

    @Test
    fun postDraftScheduleApprovedPlanInauguratesIntoNewSeasonWeek1Matches() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.initializeOrRefreshUpcomingSchedule()
        val plan = vm.uiState.value.seasonScheduleState.upcomingPlan!!
        val w1First = plan.slotsForWeek(1).first()

        // Customize Week 1 Game 1 as Opening Night + Paris Global Series
        vm.setHolidayMarqueeOnUpcomingSlot(w1First.id, HolidayTentpole.OPENING_NIGHT, "Grand Opening Rivalry")
        vm.setInternationalShowcaseOnUpcomingSlot(w1First.id, "intl_paris")
        val vote = vm.submitUpcomingScheduleForApproval()
        if (!vote.passed) vm.overrideLastVetoedVote()
        vm.dismissBoardVoteModal()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Launch New Season and advance Pre-Season -> Regular Season Week 1
        vm.finalizeAndInaugurateNewSeason()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val week1Match = vm.uiState.value.currentWeekMatches.firstOrNull {
            it.homeTeamId == w1First.homeTeamId && it.awayTeamId == w1First.awayTeamId
        }
        assertNotNull("Approved schedule Week 1 custom game must become live Week 1 match", week1Match)
        assertTrue(week1Match!!.isHolidayMarquee)
        assertTrue(week1Match.isInternationalShowcase)
        assertTrue(week1Match.internationalVenueLabel!!.contains("Paris"))
    }

    @Test
    fun scheduleCalendarScreenAndOffSeasonStep8RenderInteractiveUi() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.openScheduleCalendarScreen(true)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                val state by vm.uiState.collectAsState()
                ScheduleCalendarScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("schedule_calendar_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_schedule_approval_status").assertIsDisplayed()

        val firstSlot = vm.uiState.value.seasonScheduleState.upcomingPlan!!.slotsForWeek(1).first()
        composeTestRule.onNodeWithTag("schedule_calendar_list")
            .performScrollToNode(hasTestTag("card_schedule_match_${firstSlot.id}"))
        composeTestRule.onNodeWithTag("btn_flip_ha_${firstSlot.id}").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val afterFlip = vm.uiState.value.seasonScheduleState.upcomingPlan!!.slots.first { it.id == firstSlot.id }
        assertEquals(firstSlot.awayTeamId, afterFlip.homeTeamId)

        composeTestRule.onNodeWithTag("schedule_calendar_list")
            .performScrollToNode(hasTestTag("btn_submit_schedule_approval"))
        composeTestRule.onNodeWithTag("btn_submit_schedule_approval").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertNotNull(vm.uiState.value.activeBoardVoteModal)
    }

    @Test
    fun offSeasonSummitStep8ScheduleTabAccessibleFromDraft() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        vm.setOffSeasonStep(OffSeasonStep.LEAGUE_DRAFT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                val state by vm.uiState.collectAsState()
                OffSeasonSummitScreen(uiState = state, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("offseason_steps_lazy_row")
            .performScrollToNode(hasTestTag("tab_offseason_step_8"))
        composeTestRule.onNodeWithTag("tab_offseason_step_8").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_schedule_approval_status").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_vote_season_length").performScrollTo().assertIsDisplayed()
    }
}
