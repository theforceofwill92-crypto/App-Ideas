package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.*
import com.example.ui.screens.FanSentimentScreen
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
class FanSentimentMediaTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun initialFanSentimentSeedsCasualAndDieHardVoicesAcrossAllMediaChannels() {
        val teams = InitialLeagueData.getInitialFranchises(SportType.FOOTBALL)
        val initial = FanSentimentEngine.createInitialState(
            sportType = SportType.FOOTBALL,
            leagueName = "Gridiron Premier",
            franchises = teams,
            seasonYear = 2026
        )

        assertTrue(initial.casualSentimentScore in 30..95)
        assertTrue(initial.dieHardSentimentScore in 30..95)
        assertTrue(initial.socialFeed.any { it.segment == FanSegmentType.CASUAL })
        assertTrue(initial.socialFeed.any { it.segment == FanSegmentType.DIE_HARD })
        assertEquals(1, initial.radioSegments.size)
        assertEquals(1, initial.newspaperHeadlines.size)
    }

    @Test
    fun decisionReactionsTrackCasualVsDieHardDivergenceAndGenerateAllMediaChannels() {
        val teams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        var state = FanSentimentEngine.createInitialState(
            sportType = SportType.BASKETBALL,
            leagueName = "Apex Hoops",
            franchises = teams,
            seasonYear = 2026
        ).copy(casualSentimentScore = 72, dieHardSentimentScore = 76)

        // Decision that excites casuals (+6) while upsetting die-hards (-7)
        state = FanSentimentEngine.recordDecisionReaction(
            currentState = state,
            category = FanDecisionCategory.STADIUM_RELOCATION,
            decisionTitle = "Franchise Relocation Approved",
            decisionSummary = "Commissioners ratified a high-visibility franchise move.",
            casualDelta = 6,
            dieHardDelta = -7,
            franchiseName = teams.first().teamFullName,
            cityName = teams.first().cityName,
            sportType = SportType.BASKETBALL,
            seasonYear = 2026,
            weekNumber = 4
        )

        assertEquals(78, state.casualSentimentScore)
        assertEquals(69, state.dieHardSentimentScore)
        assertEquals(6, state.lastCasualDelta)
        assertEquals(-7, state.lastDieHardDelta)
        assertEquals(9, state.divergenceGap)

        // Two new social posts (one Casual hype, one Die-Hard backlash), plus Radio & Newspaper
        val newestSocial = state.socialFeed.take(2)
        assertTrue(newestSocial.any { it.segment == FanSegmentType.CASUAL && it.tone == MediaReactionTone.HYPE })
        assertTrue(newestSocial.any { it.segment == FanSegmentType.DIE_HARD && it.tone == MediaReactionTone.OUTRAGE })

        val newestRadio = state.radioSegments.first()
        assertEquals(78, newestRadio.casualApprovalPct)
        assertEquals(69, newestRadio.dieHardApprovalPct)
        assertTrue(newestRadio.segmentTitle.contains("Franchise Relocation Approved"))

        val newestPaper = state.newspaperHeadlines.first()
        assertEquals("FANBASE DIVIDE", newestPaper.sectionKicker)
        assertTrue(newestPaper.headline.contains("Franchise Relocation Approved"))
        assertEquals(1, state.decisionReactions.size)
    }

    @Test
    fun repositoryPersistsAndRestoresFanSentimentState() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        val teams = InitialLeagueData.getInitialFranchises(SportType.HOCKEY)
        val initial = FanSentimentEngine.createInitialState(SportType.HOCKEY, "Ice Federation", teams, 2026)
        val updated = FanSentimentEngine.recordDecisionReaction(
            currentState = initial,
            category = FanDecisionCategory.TICKET_PRICING,
            decisionTitle = "Gate Price Cut",
            decisionSummary = "Lowered family bowl seats by \$15.",
            casualDelta = 5,
            dieHardDelta = 3,
            franchiseName = teams.first().teamFullName,
            cityName = teams.first().cityName,
            sportType = SportType.HOCKEY,
            seasonYear = 2026,
            weekNumber = 2
        )

        repo.saveFanSentimentState(updated)
        val restored = repo.loadFanSentimentState()
        assertNotNull(restored)
        assertEquals(updated.casualSentimentScore, restored!!.casualSentimentScore)
        assertEquals(updated.dieHardSentimentScore, restored.dieHardSentimentScore)
        assertEquals(updated.socialFeed.size, restored.socialFeed.size)
        assertEquals(updated.radioSegments.size, restored.radioSegments.size)
        assertEquals(updated.newspaperHeadlines.size, restored.newspaperHeadlines.size)
        assertEquals(updated.decisionReactions.first().decisionTitle, restored.decisionReactions.first().decisionTitle)
    }

    @Test
    fun viewModelDecisionsAndSimulationUpdateCasualVsDieHardAndMediaPulse() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        viewModel.createNewCustomLeague("Metro Pro League", SportType.SOCCER, CommissionerBackground.MARKETING_MAVEN)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val startCasual = viewModel.uiState.value.fanSentimentState.casualSentimentScore
        val startDieHard = viewModel.uiState.value.fanSentimentState.dieHardSentimentScore
        val targetTeam = viewModel.uiState.value.franchises.first()

        // 1) Price hike hurts casuals harder than die-hards
        viewModel.updateFranchiseTicketPrice(targetTeam.id, targetTeam.ticketPrice + 24)
        val afterHike = viewModel.uiState.value.fanSentimentState
        assertTrue(afterHike.casualSentimentScore < startCasual)
        assertTrue(afterHike.dieHardSentimentScore < startDieHard)
        assertTrue(afterHike.decisionReactions.any { it.category == FanDecisionCategory.TICKET_PRICING })

        // 2) Rival Watch merger proposal hypes casuals while worrying traditionalists
        val preMergerCasual = afterHike.casualSentimentScore
        val preMergerDieHard = afterHike.dieHardSentimentScore
        val (okMerger, _) = viewModel.performRivalWatchAction(RivalWatchAction.PROPOSE_MERGER)
        assertTrue(okMerger)
        val afterMerger = viewModel.uiState.value.fanSentimentState
        assertTrue(afterMerger.casualSentimentScore > preMergerCasual)
        assertTrue(afterMerger.dieHardSentimentScore < preMergerDieHard)

        // 3) Advance week generates weekly broadcast pulse media
        val radioCountBefore = afterMerger.radioSegments.size
        viewModel.advanceWeek()
        val afterWeek = viewModel.uiState.value.fanSentimentState
        assertTrue(afterWeek.radioSegments.size > radioCountBefore)
    }

    @Test
    fun fanSentimentScreenRendersAllMediaChannelsFiltersAndInitiatives() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        viewModel.createNewCustomLeague("Pulse Cup", SportType.BASKETBALL, CommissionerBackground.WALL_STREET_FINANCIER)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                FanSentimentScreen(uiState = uiState, viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("fan_sentiment_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cohort_meter_casual").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cohort_meter_diehard").assertIsDisplayed()

        // Trigger Casual Blitz initiative
        composeTestRule.onNodeWithTag("btn_initiative_casual").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fan_initiative_feedback").assertIsDisplayed()

        // Switch through tabs (Radio, Newspapers, Social Feed)
        composeTestRule.onNodeWithTag("tab_media_newspaper").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("NEWSPAPER HEADLINES", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_media_radio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SPORTS TALK RADIO SEGMENTS", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_media_social").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chip_segment_diehard").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SOCIAL MEDIA FEED", substring = true).assertIsDisplayed()
    }
}
