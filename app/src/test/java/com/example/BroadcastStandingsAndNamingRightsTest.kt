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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.data.LeagueRevenueHistoryEntity
import com.example.model.BroadcastContract
import com.example.model.BroadcastPlatform
import com.example.model.LeagueSimulationEngine
import com.example.model.NamingRightsOfferSession
import com.example.model.NamingRightsTriggerReason
import com.example.model.SportType
import com.example.model.StadiumDealEngine
import com.example.model.StadiumDealState
import com.example.model.TeamStadium
import com.example.ui.components.NamingRightsOfferModal
import com.example.ui.components.RechartsFiveYearBroadcastContractsChart
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.StandingsScheduleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueUiState
import com.example.viewmodel.LeagueViewModel
import com.example.viewmodel.StandingsSortMode
import com.example.viewmodel.StandingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class BroadcastStandingsAndNamingRightsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rechartsBroadcastContractsTrendsPlotsFiveSeasonsTvAndStreamingPerFranchise(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val firstTeam = franchises.first()
        val secondTeam = franchises[1]
        val broadcast = BroadcastContract(
            id = "bc_test_hybrid",
            partnerName = "Apex Global Stream+",
            platform = BroadcastPlatform.HYBRID_SYNDICATE,
            annualPayoutMillions = 180L,
            yearsRemaining = 4,
            reachMultiplier = 1.22
        )

        val records = (2023..2027).mapIndexed { idx, year ->
            LeagueRevenueHistoryEntity(
                seasonYear = year,
                totalRevenueMillions = 180L + (idx * 24L),
                broadcastRevenueMillions = 100L + (idx * 20L),
                sponsorRevenueMillions = 55L + (idx * 4L),
                namingRightsRevenueMillions = 25L + (idx * 2L),
                valuationMillions = 500L + (idx * 65L),
                franchiseCount = franchises.size,
                championName = firstTeam.teamFullName,
                tvBroadcastRevenueMillions = 62L + (idx * 7L),
                streamingBroadcastRevenueMillions = 38L + (idx * 13L)
            )
        }

        try {
            val dao = db.leagueRevenueHistoryDao()
            dao.upsertAllAnnualRevenues(records)
            val saved = dao.getLastFiveYearsRevenue().sortedBy { it.seasonYear }
            assertEquals(5, saved.size)
            assertEquals(62L, saved.first().tvBroadcastRevenueMillions)
            assertEquals(38L, saved.first().streamingBroadcastRevenueMillions)
            assertEquals(90L, saved.last().tvBroadcastRevenueMillions)
            assertEquals(90L, saved.last().streamingBroadcastRevenueMillions)
        } finally {
            db.close()
        }

        val teamTrends = LeagueSimulationEngine.buildFiveSeasonFranchiseBroadcastTrends(
            franchise = firstTeam,
            records = records,
            activeBroadcast = broadcast,
            totalFranchises = franchises.size,
            currentYear = 2027
        )
        assertEquals(5, teamTrends.size)
        assertEquals(listOf(2023, 2024, 2025, 2026, 2027), teamTrends.map { it.seasonYear })
        assertTrue(teamTrends.all { it.tvContractRevenueMillions > 0.0 && it.streamingContractRevenueMillions > 0.0 })
        assertTrue(
            "Streaming revenue should grow over 5 seasons (${teamTrends.first().streamingContractRevenueMillions} -> ${teamTrends.last().streamingContractRevenueMillions})",
            teamTrends.last().streamingContractRevenueMillions > teamTrends.first().streamingContractRevenueMillions
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                RechartsFiveYearBroadcastContractsChart(
                    franchises = franchises,
                    records = records,
                    activeBroadcast = broadcast,
                    currentYear = 2027
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("recharts_broadcast_contracts_chart").assertIsDisplayed()
        composeTestRule.onNodeWithTag("broadcast_trends_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("broadcast_trends_growth_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("broadcast_trends_tooltip")
            .assertIsDisplayed()
            .assertTextContains(firstTeam.teamFullName, substring = true)

        composeTestRule.onNodeWithTag("chip_broadcast_trend_franchise_${secondTeam.id}")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("broadcast_trends_tooltip")
            .assertTextContains(secondTeam.teamFullName, substring = true)
    }

    @Test
    fun mainDashboardDisplaysBroadcastContractsTrendsChart() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = LeagueViewModel(context as Application)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.setContent {
            MyApplicationTheme {
                MainDashboardScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("main_dashboard_screen")
            .performScrollToNode(hasTestTag("recharts_broadcast_contracts_chart"))
        composeTestRule.onNodeWithTag("recharts_broadcast_contracts_chart")
            .assertIsDisplayed()
    }

    @Test
    fun roomStandingsSchemaAndViewModelTrackAndDisplayWinsLossesAndPointDifferentials(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.clearPrefsSync()

        val db = AppDatabase.getInstance(context)
        db.leagueStandingDao().clearStandings()

        val baseTeams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val alphaTeam = baseTeams[0].copy(wins = 12, losses = 2, pointsFor = 1480, pointsAgainst = 1320) // +160, .857
        val betaTeam = baseTeams[1].copy(wins = 13, losses = 4, pointsFor = 1790, pointsAgainst = 1605)  // +185, .765
        val gammaTeam = baseTeams[2].copy(wins = 6, losses = 8, pointsFor = 1310, pointsAgainst = 1390)   // -80, .429
        val configuredTeams = listOf(alphaTeam, betaTeam, gammaTeam) + baseTeams.drop(3).map {
            it.copy(wins = 5, losses = 9, pointsFor = 1200, pointsAgainst = 1310)
        }

        val standingsVm = StandingsViewModel(context as Application, repo)
        standingsVm.syncStandingsFromFranchises(
            franchises = configuredTeams,
            seasonYear = 2028,
            currentWeek = 14
        )

        // Wait briefly for IO coroutine to persist into Room and verify directly via DAO
        var persisted = db.leagueStandingDao().getAllStandings()
        var attempts = 0
        while (persisted.size < configuredTeams.size && attempts < 20) {
            Thread.sleep(25L)
            persisted = db.leagueStandingDao().getAllStandings()
            attempts++
        }
        assertEquals(configuredTeams.size, persisted.size)
        val persistedBeta = persisted.first { it.franchiseId == betaTeam.id }
        assertEquals(13, persistedBeta.wins)
        assertEquals(4, persistedBeta.losses)
        assertEquals(185, persistedBeta.pointDifferential)

        // Check default WIN_PCT order vs POINT_DIFF / WINS order
        val winPctList = standingsVm.liveStandings.first { it.isNotEmpty() }
        assertEquals(alphaTeam.id, winPctList.first().franchiseId)

        standingsVm.setSortMode(StandingsSortMode.POINT_DIFF)
        val diffSorted = standingsVm.liveStandings.first {
            it.firstOrNull()?.franchiseId == betaTeam.id
        }
        assertEquals(betaTeam.id, diffSorted.first().franchiseId)
        assertEquals(185, diffSorted.first().pointDifferential)

        // Conference filtering works
        val targetConf = alphaTeam.conference.displayName
        standingsVm.setConferenceFilter(targetConf)
        val confFiltered = standingsVm.liveStandings.first {
            it.all { item -> item.conferenceName == targetConf }
        }
        assertTrue(confFiltered.isNotEmpty())
        assertTrue(confFiltered.all { it.conferenceName == targetConf })
        standingsVm.setConferenceFilter(null)
        standingsVm.setSortMode(StandingsSortMode.WIN_PCT)

        // Verify StandingsScheduleScreen displays Room live standings card & records/differentials
        val leagueVm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val uiState = LeagueUiState(franchises = configuredTeams, currentYear = 2028, currentWeek = 14)

        composeTestRule.setContent {
            MyApplicationTheme {
                StandingsScheduleScreen(
                    uiState = uiState,
                    viewModel = leagueVm,
                    standingsViewModel = standingsVm
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("standings_room_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("standing_row_${alphaTeam.id}")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("standing_record_${alphaTeam.id}", useUnmergedTree = true)
            .assertTextContains("12-2", substring = true)
        composeTestRule.onNodeWithTag("standing_diff_${alphaTeam.id}", useUnmergedTree = true)
            .assertTextContains("+160", substring = true)

        composeTestRule.onNodeWithTag("standings_sort_POINT_DIFF")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(StandingsSortMode.POINT_DIFF, standingsVm.sortMode.value)
    }

    @Test
    fun namingRightsOffersGenerateSixUniqueOffersMaxTwoPerTierWithRandomPayAndNoDuplicateStadiumSponsors() {
        val teams = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL).mapIndexed { idx, team ->
            team.copy(
                wins = 10,
                losses = 4,
                stadium = TeamStadium(
                    sponsorName = if (idx == 0) null else "UniqueExistingSponsor_$idx",
                    sponsorTier = 1,
                    sponsorYearsLeft = if (idx == 0) 0 else 4,
                    namingRightsRevenue = if (idx == 0) 0L else 6L
                )
            )
        }
        val targetTeam = teams.first()
        val existingSponsors = StadiumDealEngine.collectUsedStadiumSponsors(teams)

        val offersA = StadiumDealEngine.generateNamingRightsOffersForFranchise(
            franchise = targetTeam,
            seasonYear = 2027,
            excludedSponsorNames = existingSponsors,
            random = Random(101)
        )
        val offersB = StadiumDealEngine.generateNamingRightsOffersForFranchise(
            franchise = targetTeam,
            seasonYear = 2027,
            excludedSponsorNames = existingSponsors,
            random = Random(202)
        )

        // Strictly 6 offers, max 2 per tier
        assertEquals(6, offersA.size)
        val countsByTier = offersA.groupingBy { it.tier }.eachCount()
        assertEquals(2, countsByTier[1])
        assertEquals(2, countsByTier[2])
        assertEquals(2, countsByTier[3])
        assertTrue(countsByTier.values.all { it <= 2 })

        // Pay is within each tier's random range
        offersA.forEach { offer ->
            when (offer.tier) {
                1 -> assertTrue("Tier 1 pay in $4M-$9M", offer.annualRevenueMillions in 4L..9L)
                2 -> assertTrue("Tier 2 pay in $10M-$19M", offer.annualRevenueMillions in 10L..19L)
                3 -> assertTrue("Tier 3 pay in $20M-$34M", offer.annualRevenueMillions in 20L..34L)
                else -> org.junit.Assert.fail("Unexpected tier ${offer.tier}")
            }
        }
        // Across draws with different seeds, generated packages / pay amounts differ
        assertTrue(
            offersA.map { it.sponsorName to it.annualRevenueMillions } !=
                offersB.map { it.sponsorName to it.annualRevenueMillions }
        )

        // All 6 sponsor names are realistic, unique from each other, and not used by any stadium
        assertEquals(6, offersA.map { it.sponsorName.lowercase() }.toSet().size)
        assertTrue(offersA.none { it.sponsorName in existingSponsors })

        // Even if existing stadiums already use several Tier 1/2/3 pool sponsors, offers exclude all of them
        val preSponsoredLeague = teams.mapIndexed { idx, team ->
            val knownSponsor = when (idx) {
                1 -> "Heartland Mutual"
                2 -> "ApexCore Semiconductors"
                3 -> "NovaVault Crypto"
                4 -> "CivicFirst Credit Union"
                5 -> "Vanguard Cloud Systems"
                6 -> "HyperYield DeFi"
                else -> team.stadium.sponsorName
            }
            team.copy(
                stadium = TeamStadium(
                    sponsorName = knownSponsor,
                    sponsorTier = if (idx == 0) 0 else 1,
                    sponsorYearsLeft = if (idx == 0) 0 else 3,
                    namingRightsRevenue = if (idx == 0) 0L else 6L
                )
            )
        }
        val activeLeagueSponsors = StadiumDealEngine.collectUsedStadiumSponsors(preSponsoredLeague)
        val nonCollidingOffers = StadiumDealEngine.generateNamingRightsOffersForFranchise(
            franchise = preSponsoredLeague.first(),
            seasonYear = 2028,
            excludedSponsorNames = activeLeagueSponsors,
            random = Random(303)
        )
        assertEquals(6, nonCollidingOffers.size)
        assertTrue(nonCollidingOffers.none { it.sponsorName in activeLeagueSponsors })

        // Two franchises expiring in the same offseason cannot receive overlapping sponsors OR sign the same sponsor
        val expiringTwo = preSponsoredLeague.mapIndexed { idx, team ->
            if (idx <= 1) {
                team.copy(
                    stadium = TeamStadium(
                        sponsorName = "OldSponsor_$idx",
                        sponsorTier = 1,
                        sponsorYearsLeft = 1,
                        namingRightsRevenue = 5L
                    ),
                    stadiumAge = 5,
                    stadiumCondition = 90
                )
            } else team
        }
        val batchCheck = StadiumDealEngine.processOffseasonStadiumsAndCheckUltimatums(
            currentState = StadiumDealState(),
            franchises = expiringTwo,
            seasonYear = 2028,
            forceTier3ScandalRoll = false,
            random = Random(404)
        )
        assertEquals(2, batchCheck.triggeredNamingRightsOffers.size)
        val firstSession = batchCheck.updatedStadiumDealState.activeNamingRightsOffer!!
        val secondSession = batchCheck.updatedStadiumDealState.pendingNamingRightsQueue.first()
        val firstSet = firstSession.offers.map { it.sponsorName.lowercase() }.toSet()
        val secondSet = secondSession.offers.map { it.sponsorName.lowercase() }.toSet()
        assertTrue("Simultaneous sessions must have disjoint sponsors", firstSet.intersect(secondSet).isEmpty())

        // Attempt to sign a sponsor already held by another stadium is blocked
        val hijackedOffer = firstSession.offers.first().copy(sponsorName = "ApexCore Semiconductors")
        val hackedSession: NamingRightsOfferSession = firstSession.copy(
            offers = listOf(hijackedOffer) + firstSession.offers.drop(1)
        )
        val duplicateReject = StadiumDealEngine.resolveNamingRightsOffer(
            currentState = batchCheck.updatedStadiumDealState.copy(activeNamingRightsOffer = hackedSession),
            franchises = batchCheck.updatedFranchises,
            offerId = hijackedOffer.id
        )
        assertFalse("Duplicate stadium sponsor across league must be rejected", duplicateReject.success)

        // Compose UI test: NamingRightsOfferModal shows all 6 cards (2 per tier) and allows picking 2nd Tier 1 offer
        var signedId: String? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                NamingRightsOfferModal(
                    session = firstSession,
                    onSelectSponsor = { id -> signedId = id }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("naming_rights_offer_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_naming_offer_tier_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_naming_offer_tier_1_2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_naming_offer_tier_2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_naming_offer_tier_2_2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_naming_offer_tier_3").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_naming_offer_tier_3_2").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("card_naming_offer_tier_1_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_confirm_naming_rights").performClick()
        composeTestRule.waitForIdle()

        val expectedSecondTier1 = firstSession.offers.filter { it.tier == 1 }[1]
        assertEquals(expectedSecondTier1.id, signedId)
        assertNotNull(signedId)
    }
}
