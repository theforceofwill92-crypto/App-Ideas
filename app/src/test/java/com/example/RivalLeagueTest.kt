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
import com.example.ui.components.RivalWatchModal
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
class RivalLeagueTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun samplePool(): List<FreeAgentPlayer> = listOf(
        FreeAgentPlayer("fa_1", "Marcus Sterling", "PG", 91, 26, "Floor General", 5, 22L, 3, "Boston Royals", "All-League Guard"),
        FreeAgentPlayer("fa_2", "Devon Vance", "SF", 88, 27, "Two-Way Wing", 6, 19L, 3, "Chicago Titans", "Lockdown Scorer"),
        FreeAgentPlayer("fa_3", "Tariq Boone", "C", 85, 25, "Rim Anchor", 4, 16L, 3, "Miami Sharks", "Double-Double Machine"),
        FreeAgentPlayer("fa_4", "Caleb Mercer", "SG", 79, 28, "Sharpshooter", 7, 9L, 2, "Denver Peaks", "Perimeter Threat"),
        FreeAgentPlayer("fa_5", "Julian Cruz", "PF", 73, 29, "Stretch Four", 8, 5L, 1, "Seattle Reign", "Veteran Glue Guy")
    )

    @Test
    fun offseasonPoachingTargetsOneToThreeHighRatedFreeAgentsBasedOnCapitalAndHype() {
        val pool = samplePool()

        // High capital & hype -> poaches 3 top-rated players
        val strongRival = RivalLeagueEngine.createDefaultRivalLeague(SportType.BASKETBALL).copy(
            capital = 140L,
            hype = 76
        )
        val highResult = RivalLeagueEngine.processOffseasonPoaching(
            rivalLeague = strongRival,
            freeAgentPool = pool,
            seasonYear = 2026
        )
        assertEquals(3, highResult.newlyPoachedPlayers.size)
        assertEquals(listOf("fa_1", "fa_2", "fa_3"), highResult.newlyPoachedPlayers.map { it.id })
        assertEquals(3, highResult.updatedRivalLeague.poachedPlayers.size)
        assertEquals(2, highResult.updatedFreeAgentPool.size)
        assertTrue(highResult.updatedFreeAgentPool.none { it.id in setOf("fa_1", "fa_2", "fa_3") })
        assertTrue(highResult.updatedRivalLeague.capital < strongRival.capital)
        assertTrue(highResult.updatedRivalLeague.hype >= strongRival.hype)

        // Same year repeat does not double-poach
        val repeatResult = RivalLeagueEngine.processOffseasonPoaching(
            rivalLeague = highResult.updatedRivalLeague,
            freeAgentPool = highResult.updatedFreeAgentPool,
            seasonYear = 2026
        )
        assertTrue(repeatResult.newlyPoachedPlayers.isEmpty())
        assertEquals(3, repeatResult.updatedRivalLeague.poachedPlayers.size)

        // Low capital & hype -> poaches 1 top-rated player
        val weakRival = RivalLeagueEngine.createDefaultRivalLeague(SportType.BASKETBALL).copy(
            capital = 30L,
            hype = 25
        )
        val lowResult = RivalLeagueEngine.processOffseasonPoaching(
            rivalLeague = weakRival,
            freeAgentPool = pool,
            seasonYear = 2027
        )
        assertEquals(1, lowResult.newlyPoachedPlayers.size)
        assertEquals("fa_1", lowResult.newlyPoachedPlayers.first().id)
        assertEquals(4, lowResult.updatedFreeAgentPool.size)
    }

    @Test
    fun rivalWatchActionsApplyExactTradeOffsAndFoldReturnsPoachedPlayers() {
        val initialPool = samplePool()
        val poached = RivalLeagueEngine.processOffseasonPoaching(
            rivalLeague = RivalLeagueEngine.createDefaultRivalLeague(SportType.BASKETBALL).copy(
                capital = 160L,
                hype = 70
            ),
            freeAgentPool = initialPool,
            seasonYear = 2026
        )
        assertEquals(3, poached.updatedRivalLeague.poachedPlayers.size)
        assertEquals(2, poached.updatedFreeAgentPool.size)
        assertEquals(90L, poached.updatedRivalLeague.capital)

        // 1. Blacklist Defectors: $0M cash, +7 Owner, -10 Union, drains capital & hype (90 -> 68)
        val afterBlacklist = RivalLeagueEngine.applyRivalWatchAction(
            rivalLeague = poached.updatedRivalLeague,
            action = RivalWatchAction.BLACKLIST_DEFECTORS,
            currentCashMillions = 80L,
            freeAgentPool = poached.updatedFreeAgentPool
        )
        assertTrue(afterBlacklist.success)
        assertFalse(afterBlacklist.didFoldRivalLeague)
        assertEquals(68L, afterBlacklist.updatedRivalLeague.capital)
        assertEquals(0L, afterBlacklist.cashDeltaMillions)
        assertEquals(7, afterBlacklist.ownerApprovalDelta)
        assertEquals(-10, afterBlacklist.unionSatisfactionDelta)
        assertTrue(afterBlacklist.updatedRivalLeague.blacklistedPlayerIds.containsAll(setOf("fa_1", "fa_2", "fa_3")))

        // 2. Counter-Program: -$12M cash, +4 Owner, +2 Union (68 -> 33)
        val afterCounter = RivalLeagueEngine.applyRivalWatchAction(
            rivalLeague = afterBlacklist.updatedRivalLeague,
            action = RivalWatchAction.COUNTER_PROGRAM,
            currentCashMillions = 80L,
            freeAgentPool = afterBlacklist.updatedFreeAgentPool
        )
        assertTrue(afterCounter.success)
        assertFalse(afterCounter.didFoldRivalLeague)
        assertEquals(33L, afterCounter.updatedRivalLeague.capital)
        assertEquals(-12L, afterCounter.cashDeltaMillions)
        assertEquals(4, afterCounter.ownerApprovalDelta)
        assertEquals(2, afterCounter.unionSatisfactionDelta)

        // Insufficient cash rejection check
        val rejected = RivalLeagueEngine.applyRivalWatchAction(
            rivalLeague = afterCounter.updatedRivalLeague,
            action = RivalWatchAction.PROPOSE_MERGER,
            currentCashMillions = 5L,
            freeAgentPool = afterCounter.updatedFreeAgentPool
        )
        assertFalse(rejected.success)

        // 3. Propose Merger folds remaining capital <= 50M (33 -> 0), isActive = false, players returned
        val afterMerger = RivalLeagueEngine.applyRivalWatchAction(
            rivalLeague = afterCounter.updatedRivalLeague,
            action = RivalWatchAction.PROPOSE_MERGER,
            currentCashMillions = 68L,
            freeAgentPool = afterCounter.updatedFreeAgentPool
        )
        assertTrue(afterMerger.success)
        assertTrue(afterMerger.didFoldRivalLeague)
        assertFalse(afterMerger.updatedRivalLeague.isActive)
        assertEquals(0L, afterMerger.updatedRivalLeague.capital)
        assertEquals(0, afterMerger.updatedRivalLeague.hype)
        assertTrue(afterMerger.updatedRivalLeague.poachedPlayers.isEmpty())
        assertEquals(5, afterMerger.updatedFreeAgentPool.size)
        assertEquals(-20L, afterMerger.cashDeltaMillions)
        assertEquals(-6, afterMerger.ownerApprovalDelta)
        assertEquals(9, afterMerger.unionSatisfactionDelta)
    }

    @Test
    fun repositoryPersistsAndLoadsRivalLeagueRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        val poached = samplePool().take(2)
        val original = RivalLeagueEngine.createDefaultRivalLeague(SportType.HOCKEY).copy(
            isActive = true,
            capital = 74L,
            hype = 61,
            poachedPlayers = poached,
            blacklistedPlayerIds = setOf("fa_1"),
            lastPoachedSeasonYear = 2027,
            totalPoachesCount = 2,
            mergerAttemptsCount = 1,
            lastOffseasonPoachSummary = "Poached 2 stars",
            lastActionFeedback = "Counter-programmed TV slate"
        )
        repo.saveRivalLeague(original)
        val loaded = repo.loadRivalLeague()
        assertNotNull(loaded)
        assertEquals(original.name, loaded!!.name)
        assertEquals(original.capital, loaded.capital)
        assertEquals(original.hype, loaded.hype)
        assertEquals(original.isActive, loaded.isActive)
        assertEquals(2, loaded.poachedPlayers.size)
        assertEquals(setOf("fa_1"), loaded.blacklistedPlayerIds)
        assertEquals(2027, loaded.lastPoachedSeasonYear)
    }

    @Test
    fun rivalWatchModalExecutesThreeActionsAndFoldsLeagueEndToEnd() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("apex_league_data", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("apex_league_settings", Context.MODE_PRIVATE).edit().clear().commit()

        val vm = LeagueViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Run a season rollover to confirm ViewModel starts with active rival league
        val startState = vm.uiState.value
        assertTrue(startState.rivalLeague.isActive)
        assertEquals(120L, startState.rivalLeague.capital)

        val initialCash = startState.cashReservesMillions
        val initialOwner = startState.ownerApprovalRating
        val initialUnion = startState.unionAgreement.unionSatisfaction

        vm.openRivalWatchModal(true)
        assertTrue(vm.uiState.value.showRivalWatchModal)

        composeTestRule.setContent {
            MyApplicationTheme {
                val state by vm.uiState.collectAsState()
                if (state.showRivalWatchModal) {
                    RivalWatchModal(
                        rivalLeague = state.rivalLeague,
                        cashReservesMillions = state.cashReservesMillions,
                        ownerApprovalRating = state.ownerApprovalRating,
                        unionHappinessPct = state.unionAgreement.unionSatisfaction,
                        freeAgentPoolSize = state.freeAgentPool.size,
                        onExecuteAction = { action -> vm.performRivalWatchAction(action) },
                        onDismiss = { vm.openRivalWatchModal(false) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("rival_watch_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rival_capital_text").assertTextEquals("\$120M")

        // Execute Blacklist Defectors via UI button
        composeTestRule.onNodeWithTag("btn_rival_action_BLACKLIST_DEFECTORS")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(98L, vm.uiState.value.rivalLeague.capital)
        assertEquals((initialOwner + 7).coerceIn(15, 100), vm.uiState.value.ownerApprovalRating)
        assertEquals((initialUnion - 10).coerceIn(10, 100), vm.uiState.value.unionAgreement.unionSatisfaction)

        // Execute Counter-Program via UI button (-35M -> 63M)
        composeTestRule.onNodeWithTag("btn_rival_action_COUNTER_PROGRAM")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(63L, vm.uiState.value.rivalLeague.capital)
        assertEquals(initialCash - 12L, vm.uiState.value.cashReservesMillions)

        // Execute Propose Merger (-60M -> 3M) then Propose Merger again (<=50M -> folds to 0M)
        composeTestRule.onNodeWithTag("btn_rival_action_PROPOSE_MERGER")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(3L, vm.uiState.value.rivalLeague.capital)
        assertTrue(vm.uiState.value.rivalLeague.isActive)

        composeTestRule.onNodeWithTag("btn_rival_action_PROPOSE_MERGER")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(vm.uiState.value.rivalLeague.isActive)
        assertEquals(0L, vm.uiState.value.rivalLeague.capital)
        composeTestRule.onNodeWithTag("rival_status_badge").assertTextEquals("Folded / Absorbed")
        composeTestRule.onNodeWithTag("rival_capital_text").assertTextEquals("\$0M")

        // Dismiss modal
        composeTestRule.onNodeWithTag("btn_close_rival_watch").performClick()
        composeTestRule.waitForIdle()
        assertFalse(vm.uiState.value.showRivalWatchModal)
    }
}
