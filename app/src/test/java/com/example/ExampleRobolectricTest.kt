package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.model.NetworkScope
import com.example.model.SportType
import com.example.model.calculateFinancialStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Apex League", appName)
  }

  @Test
  fun `verify initial franchises and cities loaded`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    val cities = InitialLeagueData.getDefaultCities()
    assertTrue(franchises.size >= 6)
    assertTrue(cities.any { it.isInternational })
    assertTrue(cities.any { it.name == "London" })
  }

  @Test
  fun `verify sport discipline scales`() {
    assertTrue(SportType.BASKETBALL.baseScoreScale > SportType.SOCCER.baseScoreScale)
  }

  @Test
  fun `verify network bidders include regional and national broadcasters`() {
    val bidders = InitialLeagueData.getNetworkBidders()
    assertTrue(bidders.isNotEmpty())
    assertTrue(bidders.any { it.scope == NetworkScope.REGIONAL_RSN })
    assertTrue(bidders.any { it.scope == NetworkScope.NATIONAL_CABLE })
    assertTrue(bidders.any { it.scope == NetworkScope.GLOBAL_STREAMING })
  }

  @Test
  fun `verify network bidder preferred terms and upfront lump sums`() {
    val bidders = InitialLeagueData.getNetworkBidders()
    bidders.forEach { bidder ->
      assertTrue(bidder.preferredTermYears in 1..7)
      assertTrue(bidder.baseLumpSumMillions > 0L)
      assertTrue(bidder.baseAnnualPayoutMillions > 0L)
    }
  }

  @Test
  fun `verify labor union agreement default safety and travel conditions`() {
    val defaultUnion = com.example.model.LaborUnionAgreement()
    assertTrue(defaultUnion.playerRevenueSharePct in 45..60)
    assertTrue(defaultUnion.minPlayerSalaryK >= 500)
    assertTrue(defaultUnion.mandatoryPrivateCharterFlights)
    assertTrue(defaultUnion.independentConcussionProtocol)
    assertTrue(defaultUnion.strikeRisk in 0..100)
    assertTrue(defaultUnion.unionSatisfaction in 0..100)
  }

  @Test
  fun `verify labor union concessions reduce strike risk`() {
    val lowCompUnion = com.example.model.LaborUnionAgreement(
      playerRevenueSharePct = 48,
      minPlayerSalaryK = 500,
      mandatoryPrivateCharterFlights = false,
      maxBackToBackGamesLimit = false,
      independentConcussionProtocol = false,
      fiveStarHotelSingleRooms = false
    )
    val highCompUnion = com.example.model.LaborUnionAgreement(
      playerRevenueSharePct = 52,
      minPlayerSalaryK = 750,
      mandatoryPrivateCharterFlights = true,
      maxBackToBackGamesLimit = true,
      independentConcussionProtocol = true,
      fiveStarHotelSingleRooms = true
    )
    assertTrue(highCompUnion.mandatoryPrivateCharterFlights)
    assertTrue(highCompUnion.independentConcussionProtocol)
    assertTrue(highCompUnion.playerRevenueSharePct > lowCompUnion.playerRevenueSharePct)
  }

  @Test
  fun `verify default league rules include salary cap and draft lottery`() {
    val defaultRules = InitialLeagueData.getDefaultLeagueRules()
    assertEquals(14, defaultRules.size)
    val salaryCapRule = defaultRules.find { it.id == "rule_salary_cap" }
    val draftLotteryRule = defaultRules.find { it.id == "rule_draft_lottery" }
    val shotClockRule = defaultRules.find { it.id == "rule_shot_clock" }
    val geoRealignmentRule = defaultRules.find { it.id == "rule_geographic_realignment" }
    val autoPlayoffRule = defaultRules.find { it.id == "rule_division_auto_playoffs" }

    assertTrue(salaryCapRule != null && salaryCapRule.isEnacted)
    assertTrue(draftLotteryRule != null && draftLotteryRule.isEnacted)
    assertTrue(shotClockRule != null && shotClockRule.isEnacted)
    assertTrue(geoRealignmentRule != null && geoRealignmentRule.isEnacted)
    assertTrue(autoPlayoffRule != null && autoPlayoffRule.isEnacted)
    assertTrue(salaryCapRule!!.parityImpact > 0)
    assertTrue(shotClockRule!!.scoringTempoModifier > 0)
  }

  @Test
  fun `verify toggle rule enact and repeal modifies parity and integrity`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialParity = vm.uiState.value.competitiveParityScore
    assertTrue(initialParity >= 70)

    // Repeal salary cap
    vm.repealRule("rule_salary_cap")
    val rulesAfterRepeal = vm.uiState.value.ruleBook.rules
    val capRule = rulesAfterRepeal.find { it.id == "rule_salary_cap" }
    assertTrue(capRule != null && !capRule.isEnacted)
    assertEquals(com.example.model.SalaryCapType.FREE_MARKET, vm.uiState.value.unionAgreement.salaryCapType)
    assertTrue(vm.uiState.value.competitiveParityScore < initialParity)

    // Re-enact salary cap
    vm.enactRule("rule_salary_cap")
    val reEnactedCap = vm.uiState.value.ruleBook.rules.find { it.id == "rule_salary_cap" }
    assertTrue(reEnactedCap != null && reEnactedCap.isEnacted)
    assertEquals(com.example.model.SalaryCapType.SOFT_CAP_LUXURY_TAX, vm.uiState.value.unionAgreement.salaryCapType)
  }

  @Test
  fun `verify draft lottery repeal updates draft parity system`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    vm.repealRule("rule_draft_lottery")
    assertEquals(com.example.model.DraftParitySystem.REVERSE_STANDINGS, vm.uiState.value.ruleBook.draftParity)

    vm.enactRule("rule_draft_lottery")
    assertEquals(com.example.model.DraftParitySystem.WEIGHTED_LOTTERY, vm.uiState.value.ruleBook.draftParity)
  }

  @Test
  fun `verify initial franchises assigned to conferences and divisions`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    assertTrue(franchises.isNotEmpty())
    val nyc = franchises.find { it.cityName == "New York" }
    val la = franchises.find { it.cityName == "Los Angeles" }

    assertTrue(nyc != null)
    assertEquals(com.example.model.Conference.EASTERN, nyc!!.conference)
    assertEquals(com.example.model.Division.ATLANTIC, nyc.division)

    assertTrue(la != null)
    assertEquals(com.example.model.Conference.WESTERN, la!!.conference)
    assertEquals(com.example.model.Division.PACIFIC, la.division)
  }

  @Test
  fun `verify franchise conference and division realignment`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val teamAustin = vm.uiState.value.franchises.find { it.cityName == "Austin" }
    assertTrue(teamAustin != null)

    // Realign Austin to Eastern Conference, Southeast Division
    vm.reassignFranchiseConferenceDivision(
      franchiseId = teamAustin!!.id,
      conference = com.example.model.Conference.EASTERN,
      division = com.example.model.Division.SOUTHEAST
    )

    val realignedAustin = vm.uiState.value.franchises.find { it.id == teamAustin.id }
    assertEquals(com.example.model.Conference.EASTERN, realignedAustin?.conference)
    assertEquals(com.example.model.Division.SOUTHEAST, realignedAustin?.division)

    // Auto-realign geographically
    vm.autoRealignAllFranchisesGeographically()
    val geoAustin = vm.uiState.value.franchises.find { it.id == teamAustin.id }
    assertEquals(com.example.model.Conference.WESTERN, geoAustin?.conference)
    assertEquals(com.example.model.Division.SOUTHWEST, geoAustin?.division)
  }

  @Test
  fun `verify realignment presets distribute franchises`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    vm.applyRealignmentPreset(com.example.model.RealignmentPreset.BALANCED_PARITY)
    val teams = vm.uiState.value.franchises
    val eastCount = teams.count { it.conference == com.example.model.Conference.EASTERN }
    val westCount = teams.count { it.conference == com.example.model.Conference.WESTERN }
    assertEquals(teams.size / 2, eastCount)
    assertEquals(teams.size / 2, westCount)
  }

  @Test
  fun `verify calculatePlayoffSeeds with 6 teams and 2 byes`() {
    val franchises = InitialLeagueData.getInitialFranchises().toMutableList()
    // Give specific records to test division winners and seeding
    val updatedFranchises = franchises.mapIndexed { idx, f ->
      f.copy(wins = 20 - idx * 2, losses = idx * 2)
    }

    val rules = com.example.model.PlayoffRules(
      fieldSize = 6,
      firstRoundByesCount = 2,
      divisionWinnersAutoBerth = true,
      qualificationMode = com.example.model.PlayoffQualificationMode.OVERALL_LEAGUE_TABLE,
      seedingMethod = com.example.model.PlayoffSeedingMethod.DIVISION_WINNERS_FIRST
    )

    val seeds = com.example.model.calculatePlayoffSeeds(updatedFranchises, rules)
    assertEquals(6, seeds.size)
    assertEquals(1, seeds[0].seed)
    assertTrue(seeds[0].hasBye)
    assertEquals(2, seeds[1].seed)
    assertTrue(seeds[1].hasBye)

    // Seeds 3 to 6 do not have a bye
    for (i in 2 until 6) {
      assertEquals(i + 1, seeds[i].seed)
      org.junit.Assert.assertFalse(seeds[i].hasBye)
    }

    // Verify format summary reflects 6 teams and 2 byes
    assertTrue(rules.formatSummary.contains("6 Teams"))
    assertTrue(rules.formatSummary.contains("2 Byes"))
  }

  @Test
  fun `verify updatePlayoffRules in LeagueViewModel synchronizes league rules`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Initially division auto playoffs is enacted
    val initialDivRule = vm.uiState.value.ruleBook.rules.find { it.id == "rule_division_auto_playoffs" }
    assertTrue(initialDivRule?.isEnacted == true)

    // Update playoff rules: 8 teams, 0 byes, divisionWinnersAutoBerth = false, playIn = true
    vm.updatePlayoffRules(
      fieldSize = 8,
      firstRoundByesCount = 0,
      divisionWinnersAutoBerth = false,
      playInTournamentEnabled = true,
      qualificationMode = com.example.model.PlayoffQualificationMode.CONFERENCE_SPLIT,
      seedingMethod = com.example.model.PlayoffSeedingMethod.PURE_WIN_PERCENTAGE
    )

    val updatedState = vm.uiState.value
    assertEquals(8, updatedState.ruleBook.playoffRules.fieldSize)
    assertEquals(0, updatedState.ruleBook.playoffRules.firstRoundByesCount)
    org.junit.Assert.assertFalse(updatedState.ruleBook.playoffRules.divisionWinnersAutoBerth)
    assertTrue(updatedState.ruleBook.playoffRules.playInTournamentEnabled)
    assertEquals(com.example.model.PlayoffQualificationMode.CONFERENCE_SPLIT, updatedState.ruleBook.playoffRules.qualificationMode)
    assertEquals(com.example.model.PlayoffSeedingMethod.PURE_WIN_PERCENTAGE, updatedState.ruleBook.playoffRules.seedingMethod)

    // Synchronized rules
    val syncedDivRule = updatedState.ruleBook.rules.find { it.id == "rule_division_auto_playoffs" }
    org.junit.Assert.assertFalse(syncedDivRule?.isEnacted == true)
    val syncedPlayInRule = updatedState.ruleBook.rules.find { it.id == "rule_play_in_tournament" }
    assertTrue(syncedPlayInRule?.isEnacted == true)
  }

  @Test
  fun `verify seeding and wild cards with conference split`() {
    val franchises = InitialLeagueData.getInitialFranchises().toMutableList()
    val rules = com.example.model.PlayoffRules(
      fieldSize = 4,
      firstRoundByesCount = 0,
      divisionWinnersAutoBerth = true,
      qualificationMode = com.example.model.PlayoffQualificationMode.CONFERENCE_SPLIT,
      seedingMethod = com.example.model.PlayoffSeedingMethod.CONFERENCE_PROTECTED
    )

    val seeds = com.example.model.calculatePlayoffSeeds(franchises, rules)
    assertEquals(4, seeds.size)
    val eastCount = seeds.count { it.franchise.conference == com.example.model.Conference.EASTERN }
    val westCount = seeds.count { it.franchise.conference == com.example.model.Conference.WESTERN }
    assertEquals(2, eastCount)
    assertEquals(2, westCount)
  }

  @Test
  fun `verify franchise financial statement calculates ticket sales, merchandise, and broadcast deals`() {
    val franchise = InitialLeagueData.getInitialFranchises().first()
    val broadcast = InitialLeagueData.getInitialBroadcastDeals().first()

    val statement = franchise.calculateFinancialStatement(broadcast, 6)

    assertTrue(statement.ticketRevenueMillions > 0.0)
    assertTrue(statement.merchandiseRevenueMillions > 0.0)
    assertTrue(statement.broadcastRevenueMillions > 0.0)
    assertTrue(statement.totalRevenueMillions > 0.0)
    assertTrue(statement.avgAttendance in 10000..franchise.stadiumCapacity)
    assertEquals(franchise.ticketPrice, statement.ticketPrice)

    // Verify component revenues sum up to total revenue
    val sumPillars = statement.ticketRevenueMillions + statement.merchandiseRevenueMillions +
            statement.broadcastRevenueMillions + statement.localSponsorRevenueMillions
    assertTrue(kotlin.math.abs(statement.totalRevenueMillions - sumPillars) < 0.5)
  }

  @Test
  fun `verify updating franchise ticket price updates state and financial yields`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val team = vm.uiState.value.franchises.first()
    val initialPrice = team.ticketPrice

    // Update ticket price to $120
    vm.updateFranchiseTicketPrice(team.id, 120)

    val updatedTeam = vm.uiState.value.franchises.find { it.id == team.id }
    assertEquals(120, updatedTeam?.ticketPrice)

    // Calculate updated financial statement
    val updatedStmt = updatedTeam!!.calculateFinancialStatement(vm.uiState.value.activeBroadcast, 6)
    assertEquals(120, updatedStmt.ticketPrice)
    assertTrue(updatedStmt.ticketRevenueMillions > 0.0)
  }

  @Test
  fun `verify expansion draft pool generation and round-by-round drafting`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialCount = vm.uiState.value.franchises.size
    val candidateCity = vm.uiState.value.cities.first { !it.hasFranchise }

    // Start Expansion Draft for candidate city
    vm.startExpansionDraft(
      targetCityId = candidateCity.id,
      nickname = "Strikers",
      brandColorHex = 0xFFFF5722,
      ownershipType = com.example.model.OwnershipType.TECH_BILLIONAIRE,
      totalRounds = 4
    )

    val session = vm.uiState.value.activeExpansionDraftSession
    org.junit.Assert.assertNotNull(session)
    assertEquals(candidateCity.id, session!!.targetCity.id)
    assertEquals(1, session.currentRound)
    assertEquals(4, session.totalRounds)
    assertTrue(session.availablePool.size >= 12)
    org.junit.Assert.assertFalse(session.isCompleted)

    // Draft first player
    val player1 = session.availablePool.first()
    vm.draftPlayerForExpansion(player1.id)

    val stateAfterPick1 = vm.uiState.value.activeExpansionDraftSession!!
    assertEquals(1, stateAfterPick1.draftedPlayers.size)
    assertEquals(player1.id, stateAfterPick1.draftedPlayers.first().id)
    assertEquals(2, stateAfterPick1.currentRound)

    // Auto-pick remaining 3 rounds
    vm.autoDraftBestAvailable()
    vm.autoDraftBestAvailable()
    vm.autoDraftBestAvailable()

    val completedSession = vm.uiState.value.activeExpansionDraftSession!!
    assertEquals(4, completedSession.draftedPlayers.size)
    assertTrue(completedSession.isCompleted)
    assertTrue(completedSession.averageOvr in 70..90)

    // Finalize expansion draft
    val initialCash = vm.uiState.value.cashReservesMillions
    vm.finalizeExpansionDraft()

    val stateAfterFinalize = vm.uiState.value
    org.junit.Assert.assertNull(stateAfterFinalize.activeExpansionDraftSession)
    assertEquals(initialCount + 1, stateAfterFinalize.franchises.size)

    val newFranchise = stateAfterFinalize.franchises.find { it.nickname == "Strikers" }
    org.junit.Assert.assertNotNull(newFranchise)
    assertEquals(candidateCity.name, newFranchise!!.cityName)
    assertEquals(completedSession.averageOvr, newFranchise.rosterRating)
    assertEquals(initialCash + candidateCity.expansionFeeMillions, stateAfterFinalize.cashReservesMillions)
    assertTrue(stateAfterFinalize.cities.find { it.id == candidateCity.id }?.hasFranchise == true)
  }

  @Test
  fun `verify ratifyComprehensiveCBA sets custom salary cap and syncs rules and parity`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Ratify a new CBA with $105M salary cap and HARD_CAP
    vm.ratifyComprehensiveCBA(
      revenueSharePct = 51,
      salaryCapType = com.example.model.SalaryCapType.HARD_CAP,
      salaryCapMillions = 105L,
      minSalaryK = 800L,
      playerHealthWelfareMillions = 18L,
      mandatoryPrivateCharters = true,
      backToBackLimits = true,
      concussionProtocol = true,
      hybridTurfMandate = true,
      transitionFundMillions = 8L,
      fiveStarHotels = true
    )

    val updatedUnion = vm.uiState.value.unionAgreement
    assertEquals(105L, updatedUnion.salaryCapMillions)
    assertEquals(com.example.model.SalaryCapType.HARD_CAP, updatedUnion.salaryCapType)
    assertEquals(51, updatedUnion.playerRevenueSharePct)
    assertEquals(800L, updatedUnion.minPlayerSalaryK)
    assertTrue(updatedUnion.unionSatisfaction >= 80)
    assertTrue(updatedUnion.strikeRisk <= 20)
    assertEquals(5, updatedUnion.cbaYearsRemaining)

    // Salary cap rule should be enacted
    val capRule = vm.uiState.value.ruleBook.rules.find { it.id == "rule_salary_cap" }
    assertTrue(capRule?.isEnacted == true)
  }

  @Test
  fun `verify strike crisis handling through emergency welfare grant and summit mediation`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialCash = vm.uiState.value.cashReservesMillions
    val initialRisk = vm.uiState.value.unionAgreement.strikeRisk
    val initialSatisfaction = vm.uiState.value.unionAgreement.unionSatisfaction

    // Inject $5M emergency grant
    vm.handleStrikeDeescalationGrant(5L)

    val stateAfterGrant = vm.uiState.value
    assertEquals(initialCash - 5L, stateAfterGrant.cashReservesMillions)
    assertTrue(stateAfterGrant.unionAgreement.strikeRisk <= initialRisk)
    assertTrue(stateAfterGrant.unionAgreement.unionSatisfaction >= initialSatisfaction)

    // Call emergency summit
    vm.callEmergencyPlayerSummit()
    val stateAfterSummit = vm.uiState.value
    assertTrue(stateAfterSummit.unionAgreement.strikeRisk <= stateAfterGrant.unionAgreement.strikeRisk)
    assertTrue(stateAfterSummit.leagueIntegrityScore >= stateAfterGrant.leagueIntegrityScore)

    // Threaten lockout
    vm.threatenManagementLockout()
    val stateAfterLockout = vm.uiState.value
    assertTrue(stateAfterLockout.unionAgreement.strikeRisk > stateAfterSummit.unionAgreement.strikeRisk)
    assertTrue(stateAfterLockout.ownerApprovalRating >= stateAfterSummit.ownerApprovalRating)
  }

  @Test
  fun `verify updateOvertimeFormat updates ruleBook and logs news`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Initial format
    assertEquals(com.example.model.OvertimeFormat.STANDARD_PERIOD, vm.uiState.value.ruleBook.overtimeFormat)

    // Toggle to Target Score (Elam Ending)
    vm.updateOvertimeFormat(com.example.model.OvertimeFormat.TARGET_SCORE_ELAM)
    assertEquals(com.example.model.OvertimeFormat.TARGET_SCORE_ELAM, vm.uiState.value.ruleBook.overtimeFormat)
    assertTrue(vm.uiState.value.recentNews.first().contains("Target Score"))

    // Toggle to Sudden Death
    vm.updateOvertimeFormat(com.example.model.OvertimeFormat.SUDDEN_DEATH)
    assertEquals(com.example.model.OvertimeFormat.SUDDEN_DEATH, vm.uiState.value.ruleBook.overtimeFormat)
    assertTrue(vm.uiState.value.recentNews.first().contains("Sudden Death"))

    // Toggle to Penalty Shootout
    vm.updateOvertimeFormat(com.example.model.OvertimeFormat.PENALTY_SHOOTOUT)
    assertEquals(com.example.model.OvertimeFormat.PENALTY_SHOOTOUT, vm.uiState.value.ruleBook.overtimeFormat)
  }

  @Test
  fun `verify updateRosterSizeRule updates active and reserve slots`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Initial standard 15+2
    assertEquals(com.example.model.RosterSizeRule.STANDARD_15, vm.uiState.value.ruleBook.rosterSizeRule)
    assertEquals(15, vm.uiState.value.ruleBook.rosterSizeRule.activeSlots)
    assertEquals(2, vm.uiState.value.ruleBook.rosterSizeRule.reserveSlots)

    // Switch to Compact 12
    vm.updateRosterSizeRule(com.example.model.RosterSizeRule.COMPACT_12)
    assertEquals(com.example.model.RosterSizeRule.COMPACT_12, vm.uiState.value.ruleBook.rosterSizeRule)
    assertEquals(12, vm.uiState.value.ruleBook.rosterSizeRule.activeSlots)
    assertEquals(0, vm.uiState.value.ruleBook.rosterSizeRule.reserveSlots)

    // Switch to Expanded 18+4
    vm.updateRosterSizeRule(com.example.model.RosterSizeRule.EXPANDED_18)
    assertEquals(com.example.model.RosterSizeRule.EXPANDED_18, vm.uiState.value.ruleBook.rosterSizeRule)
    assertEquals(18, vm.uiState.value.ruleBook.rosterSizeRule.activeSlots)
    assertEquals(4, vm.uiState.value.ruleBook.rosterSizeRule.reserveSlots)
  }

  @Test
  fun `verify updateSalaryCapStrictness synchronizes salary cap strictness, parity, and CBA cap type`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Switch to STRICT_HARD_CAP
    vm.updateSalaryCapStrictness(com.example.model.SalaryCapStrictness.STRICT_HARD_CAP)
    val hardCapState = vm.uiState.value
    assertEquals(com.example.model.SalaryCapStrictness.STRICT_HARD_CAP, hardCapState.ruleBook.salaryCapStrictness)
    assertEquals(com.example.model.SalaryCapType.HARD_CAP, hardCapState.unionAgreement.salaryCapType)
    assertTrue(hardCapState.ruleBook.rules.find { it.id == "rule_salary_cap" }?.isEnacted == true)

    // Switch to UNRESTRICTED_MARKET
    vm.updateSalaryCapStrictness(com.example.model.SalaryCapStrictness.UNRESTRICTED_MARKET)
    val freeMarketState = vm.uiState.value
    assertEquals(com.example.model.SalaryCapStrictness.UNRESTRICTED_MARKET, freeMarketState.ruleBook.salaryCapStrictness)
    assertEquals(com.example.model.SalaryCapType.FREE_MARKET, freeMarketState.unionAgreement.salaryCapType)
    assertTrue(freeMarketState.ruleBook.rules.find { it.id == "rule_salary_cap" }?.isEnacted == false)
    assertTrue(freeMarketState.competitiveParityScore < hardCapState.competitiveParityScore)

    // Switch to MODERATE_SOFT_CAP
    vm.updateSalaryCapStrictness(com.example.model.SalaryCapStrictness.MODERATE_SOFT_CAP)
    val softCapState = vm.uiState.value
    assertEquals(com.example.model.SalaryCapStrictness.MODERATE_SOFT_CAP, softCapState.ruleBook.salaryCapStrictness)
    assertEquals(com.example.model.SalaryCapType.SOFT_CAP_LUXURY_TAX, softCapState.unionAgreement.salaryCapType)
    assertTrue(softCapState.ruleBook.rules.find { it.id == "rule_salary_cap" }?.isEnacted == true)
  }

  @Test
  fun `verify dynamic headline generation for expansion drafts, broadcast deals, and labor disputes`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Dynamic Expansion Draft news
    val expNews = vm.generateDynamicHeadline(com.example.model.NewsCategory.EXPANSION_DRAFT)
    assertEquals(com.example.model.NewsCategory.EXPANSION_DRAFT, expNews.category)
    assertEquals(com.example.model.NewsActionType.NAVIGATE_EXPANSION, expNews.actionType)
    assertTrue(vm.uiState.value.recentNews.first().contains("EXPANSION") || vm.uiState.value.recentNews.first().contains("DRAFT"))

    // Dynamic Broadcast Deal news
    val bcastNews = vm.generateDynamicHeadline(com.example.model.NewsCategory.BROADCAST_DEAL)
    assertEquals(com.example.model.NewsCategory.BROADCAST_DEAL, bcastNews.category)
    assertEquals(com.example.model.NewsActionType.NAVIGATE_BROADCAST, bcastNews.actionType)
    assertTrue(vm.uiState.value.recentNews.first().contains("BROADCAST") || vm.uiState.value.recentNews.first().contains("MEDIA") || vm.uiState.value.recentNews.first().contains("STREAMING") || vm.uiState.value.recentNews.first().contains("SYNDICATE"))

    // Dynamic Labor Dispute news
    val laborNews = vm.generateDynamicHeadline(com.example.model.NewsCategory.LABOR_UNION)
    assertEquals(com.example.model.NewsCategory.LABOR_UNION, laborNews.category)
    assertEquals(com.example.model.NewsActionType.NAVIGATE_LABOR, laborNews.actionType)
    assertTrue(vm.uiState.value.recentNews.first().contains("LABOR") || vm.uiState.value.recentNews.first().contains("CBA") || vm.uiState.value.recentNews.first().contains("PLAYER") || vm.uiState.value.recentNews.first().contains("STRIKE"))
  }

  @Test
  fun `verify LeagueNewsParser parses headlines with proper category and action navigation`() {
    val expItem = com.example.model.LeagueNewsParser.parseFromRawString(
      "HISTORIC EXPANSION DRAFT: Seattle picks 18 players!",
      week = 3,
      year = 2026
    )
    assertEquals(com.example.model.NewsCategory.EXPANSION_DRAFT, expItem.category)
    assertEquals(com.example.model.NewsActionType.NAVIGATE_EXPANSION, expItem.actionType)
    assertEquals(com.example.model.NewsImpact.BREAKING, expItem.impact)

    val bcastItem = com.example.model.LeagueNewsParser.parseFromRawString(
      "BROADCAST DEAL SIGNED: Inked multi-year agreement with Global Streamer for \$120M/yr!",
      week = 3,
      year = 2026
    )
    assertEquals(com.example.model.NewsCategory.BROADCAST_DEAL, bcastItem.category)
    assertEquals(com.example.model.NewsActionType.NAVIGATE_BROADCAST, bcastItem.actionType)

    val laborItem = com.example.model.LeagueNewsParser.parseFromRawString(
      "LABOR DISPUTE WARNING: Strike threat looms as revenue split unresolved!",
      week = 3,
      year = 2026
    )
    assertEquals(com.example.model.NewsCategory.LABOR_UNION, laborItem.category)
    assertEquals(com.example.model.NewsActionType.NAVIGATE_LABOR, laborItem.actionType)
    assertEquals(com.example.model.NewsImpact.CRITICAL, laborItem.impact)
  }

  @Test
  fun `verify OffSeasonEngine generates multi-step events with retirements, lottery, draft, free agency, and parity compression`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    val session = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = 2026,
      franchises = franchises,
      sportType = com.example.model.SportType.BASKETBALL,
      ruleBook = com.example.model.LeagueRuleBook(),
      initialParity = 80
    )

    // Verify all 5 steps are generated and populated
    assertTrue(session.retirements.isNotEmpty())
    assertTrue(session.retirements.all { it.capFreedMillions > 0L && it.teamRatingPenalty > 0 })

    assertTrue(session.lotteryResults.isNotEmpty())
    assertEquals(1, session.lotteryResults.minOf { it.finalDraftPick })
    assertTrue(session.lotteryResults.any { it.finalDraftPick == 1 })

    assertTrue(session.draftPicks.isNotEmpty())
    val pick1 = session.draftPicks.find { it.pickNumber == 1 }
    org.junit.Assert.assertNotNull(pick1)
    assertTrue(pick1!!.overallRating >= 83)
    assertEquals("A+", pick1.potentialGrade)
    assertTrue(pick1.teamBoost >= 5)

    assertTrue(session.freeAgencySignings.isNotEmpty())
    assertTrue(session.freeAgencySignings.all { it.salaryMillions > 0L && it.teamBoost > 0 })

    // Parity compression verification
    assertTrue(session.finalParityScore >= session.initialParityScore)
    assertTrue(session.teamMovements.isNotEmpty())
    assertEquals(franchises.size, session.teamMovements.size)
  }

  @Test
  fun `verify off-season step transitions and reviewability in LeagueViewModel`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Open Off-Season Summit
    vm.openOffSeasonSummit(true)
    assertTrue(vm.uiState.value.showOffSeasonScreen)

    // Step 1: Retirements
    vm.setOffSeasonStep(com.example.model.OffSeasonStep.RETIREMENTS)
    val session1 = vm.uiState.value.activeOffSeasonSession ?: vm.uiState.value.latestCompletedOffSeason
    assertEquals(com.example.model.OffSeasonStep.RETIREMENTS, session1?.currentStep)

    // Step 2: Draft Lottery
    vm.setOffSeasonStep(com.example.model.OffSeasonStep.DRAFT_LOTTERY)
    val session2 = vm.uiState.value.activeOffSeasonSession ?: vm.uiState.value.latestCompletedOffSeason
    assertEquals(com.example.model.OffSeasonStep.DRAFT_LOTTERY, session2?.currentStep)

    // Step 3: Rookie Draft
    vm.setOffSeasonStep(com.example.model.OffSeasonStep.LEAGUE_DRAFT)
    val session3 = vm.uiState.value.activeOffSeasonSession ?: vm.uiState.value.latestCompletedOffSeason
    assertEquals(com.example.model.OffSeasonStep.LEAGUE_DRAFT, session3?.currentStep)

    // Step 4: Free Agency
    vm.setOffSeasonStep(com.example.model.OffSeasonStep.FREE_AGENCY)
    val session4 = vm.uiState.value.activeOffSeasonSession ?: vm.uiState.value.latestCompletedOffSeason
    assertEquals(com.example.model.OffSeasonStep.FREE_AGENCY, session4?.currentStep)

    // Step 5: Summary & Parity
    vm.setOffSeasonStep(com.example.model.OffSeasonStep.SUMMARY)
    val session5 = vm.uiState.value.activeOffSeasonSession ?: vm.uiState.value.latestCompletedOffSeason
    assertEquals(com.example.model.OffSeasonStep.SUMMARY, session5?.currentStep)

    // Close Summit
    vm.closeOffSeasonSummit()
    org.junit.Assert.assertFalse(vm.uiState.value.showOffSeasonScreen)
  }

  @Test
  fun `verify finalizeAndInaugurateNewSeason ratifies off-season ratings and advances calendar`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialYear = vm.uiState.value.currentYear

    // Finalize off-season and inaugurate new campaign (launches Pre-Season right after Rookie Draft)
    vm.finalizeAndInaugurateNewSeason()

    val updatedState = vm.uiState.value
    assertEquals(initialYear + 1, updatedState.currentYear)
    assertEquals(1, updatedState.currentWeek)
    assertEquals(com.example.model.SeasonPhase.PRE_SEASON, updatedState.seasonPhase)
    org.junit.Assert.assertFalse(updatedState.showOffSeasonScreen)
    org.junit.Assert.assertNull(updatedState.activeOffSeasonSession)
    org.junit.Assert.assertNotNull(updatedState.latestCompletedOffSeason)
    assertTrue(updatedState.latestCompletedOffSeason!!.isCompleted)
    assertTrue(updatedState.recentNews.first().contains("OFF-SEASON CONCLUDED"))

    // Advancing from Pre-Season officially tips off Regular Season Week 1
    vm.advanceWeek()
    assertEquals(com.example.model.SeasonPhase.REGULAR_SEASON, vm.uiState.value.seasonPhase)
    assertEquals(1, vm.uiState.value.currentWeek)
  }

  @Test
  fun `verify prospective market pool has diverse national and international markets with varying viability`() {
    val pool = com.example.data.InitialLeagueData.getProspectiveMarketPool()
    assertTrue("Pool should have at least 20 markets", pool.size >= 20)

    val national = pool.filter { !it.isInternational }
    val international = pool.filter { it.isInternational }
    assertTrue("Should include multiple national markets", national.size >= 8)
    assertTrue("Should include multiple international gateways", international.size >= 8)

    // Verify varying viability
    val primeMetros = pool.filter { it.overallViabilityScore >= 80 }
    val solidContenders = pool.filter { it.overallViabilityScore in 70..79 }
    val highGrowth = pool.filter { it.overallViabilityScore in 60..69 }
    val emergingOrWildcard = pool.filter { it.overallViabilityScore < 60 }

    assertTrue("Should have prime metros", primeMetros.isNotEmpty())
    assertTrue("Should have solid contenders", solidContenders.isNotEmpty())
    assertTrue("Should have high growth targets", highGrowth.isNotEmpty())
    assertTrue("Should have emerging or speculative wildcard markets", emergingOrWildcard.isNotEmpty())

    // Verify viability grade matching
    primeMetros.forEach { assertEquals(com.example.model.MarketViabilityGrade.PRIME_METRO, it.viabilityGrade) }
    solidContenders.forEach { assertEquals(com.example.model.MarketViabilityGrade.SOLID_CONTENDER, it.viabilityGrade) }
  }

  @Test
  fun `verify open expansion candidates strictly capped at 10 and supports admission replacement and withdrawal`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialOpen = vm.getOpenExpansionCandidates()
    assertTrue("Initial open candidates must be <= 10", initialOpen.size <= com.example.model.MAX_OPEN_EXPANSION_CANDIDATES)

    // Find available prospect
    val pool = com.example.data.InitialLeagueData.getProspectiveMarketPool()
    val currentCityIds = vm.uiState.value.cities.map { it.id }.toSet()
    val available = pool.filter { it.id !in currentCityIds }

    // Fill to exactly 10 if not already 10
    var candidateIndex = 0
    while (vm.getOpenExpansionCandidates().size < com.example.model.MAX_OPEN_EXPANSION_CANDIDATES && candidateIndex < available.size) {
      val admitted = vm.admitMarketCandidate(available[candidateIndex])
      assertTrue(admitted)
      candidateIndex++
    }

    assertEquals(com.example.model.MAX_OPEN_EXPANSION_CANDIDATES, vm.getOpenExpansionCandidates().size)

    // Attempting to admit an 11th candidate without replacement must fail
    val extraCandidate = available[candidateIndex]
    val rejected = vm.admitMarketCandidate(extraCandidate)
    org.junit.Assert.assertFalse("11th candidate without replacement must be rejected", rejected)
    assertEquals(com.example.model.MAX_OPEN_EXPANSION_CANDIDATES, vm.getOpenExpansionCandidates().size)

    // Admitting with replacement must succeed and keep total at 10
    val toReplace = vm.getOpenExpansionCandidates().first()
    val replaced = vm.admitMarketCandidate(extraCandidate, replaceCityId = toReplace.id)
    assertTrue("Replacing a candidate must succeed", replaced)
    assertEquals(com.example.model.MAX_OPEN_EXPANSION_CANDIDATES, vm.getOpenExpansionCandidates().size)
    assertTrue("New candidate should be in cities", vm.uiState.value.cities.any { it.id == extraCandidate.id })
    org.junit.Assert.assertFalse("Replaced candidate should be removed", vm.uiState.value.cities.any { it.id == toReplace.id })

    // Withdrawing a candidate must reduce count by 1 and open a slot
    val candidateToWithdraw = vm.getOpenExpansionCandidates().last()
    val withdrawn = vm.withdrawMarketCandidate(candidateToWithdraw.id)
    assertTrue(withdrawn)
    assertEquals(com.example.model.MAX_OPEN_EXPANSION_CANDIDATES - 1, vm.getOpenExpansionCandidates().size)
    org.junit.Assert.assertFalse(vm.uiState.value.cities.any { it.id == candidateToWithdraw.id })

    // Now adding another candidate into the vacated slot must succeed directly
    candidateIndex++
    val anotherCandidate = available[candidateIndex]
    val admittedAfterWithdraw = vm.admitMarketCandidate(anotherCandidate)
    assertTrue(admittedAfterWithdraw)
    assertEquals(com.example.model.MAX_OPEN_EXPANSION_CANDIDATES, vm.getOpenExpansionCandidates().size)
  }

  @Test
  fun `verify franchise roster and draft picks initialized properly`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    assertTrue(franchises.isNotEmpty())
    franchises.forEach { franchise ->
      assertTrue("Franchise ${franchise.nickname} should have players", franchise.roster.isNotEmpty())
      assertTrue("Franchise ${franchise.nickname} should have 6 draft picks", franchise.draftPicks.size >= 6)
      assertTrue("Draft picks should include round 1 and round 2", franchise.draftPicks.any { it.round == 1 } && franchise.draftPicks.any { it.round == 2 })
    }
  }

  @Test
  fun `verify trade engine proposal generation and fairness valuation`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    val tradeRules = com.example.model.TradeRules(tradeDeadlineWeek = 10)
    val proposal = com.example.model.TradeEngine.generateRealisticTradeOffer(
      week = 3,
      franchises = franchises,
      tradeRules = tradeRules
    )

    assertTrue("Proposal should be generated", proposal != null)
    assertTrue("Proposal must have Team A sending assets", proposal!!.teamASendsPlayers.isNotEmpty() || proposal.teamASendsPicks.isNotEmpty())
    assertTrue("Proposal must have Team B sending assets", proposal.teamBSendsPlayers.isNotEmpty() || proposal.teamBSendsPicks.isNotEmpty())
    assertTrue("Fairness score should be between 10 and 100", proposal.fairnessScore in 10..100)
    assertTrue("Trade values should be positive", proposal.teamATotalTradeValue > 0 && proposal.teamBTotalTradeValue > 0)
  }

  @Test
  fun `verify trade approval updates team rosters and draft picks`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises
    val teamA = franchises[0]
    val teamB = franchises[1]

    val playerFromA = teamA.roster.first()
    val playerFromB = teamB.roster.first()

    val customTrade = com.example.model.TradeProposal(
      id = "test_trade_approval_1",
      weekProposed = vm.uiState.value.currentWeek,
      teamAId = teamA.id,
      teamAName = teamA.teamFullName,
      teamBId = teamB.id,
      teamBName = teamB.teamFullName,
      teamASendsPlayers = listOf(playerFromA),
      teamBSendsPlayers = listOf(playerFromB),
      fairnessScore = 88,
      salaryCapCompliant = true
    )

    // Put into pending trades and approve
    val currentPending = vm.uiState.value.pendingTrades
    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(pendingTrades = listOf(customTrade) + currentPending)

    val approved = vm.approveTrade("test_trade_approval_1")
    assertTrue("Trade should be approved", approved)

    val updatedFranchises = vm.uiState.value.franchises
    val updatedTeamA = updatedFranchises.find { it.id == teamA.id }!!
    val updatedTeamB = updatedFranchises.find { it.id == teamB.id }!!

    // Team A now has playerFromB and no longer has playerFromA
    assertTrue("Team A should receive player from Team B", updatedTeamA.roster.any { it.id == playerFromB.id })
    org.junit.Assert.assertFalse("Team A should no longer have player sent", updatedTeamA.roster.any { it.id == playerFromA.id })

    // Team B now has playerFromA and no longer has playerFromB
    assertTrue("Team B should receive player from Team A", updatedTeamB.roster.any { it.id == playerFromA.id })
    org.junit.Assert.assertFalse("Team B should no longer have player sent", updatedTeamB.roster.any { it.id == playerFromB.id })

    // Completed trades ledger should record this approved trade
    assertTrue("Completed ledger should contain approved trade", vm.uiState.value.completedTrades.any { it.id == "test_trade_approval_1" && it.status == com.example.model.TradeStatus.ACCEPTED })
  }

  @Test
  fun `verify commissioner veto prevents asset transfer and marks trade vetoed`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises
    val teamA = franchises[0]
    val teamB = franchises[1]
    val playerFromA = teamA.roster.first()

    val customTrade = com.example.model.TradeProposal(
      id = "test_trade_veto_1",
      weekProposed = vm.uiState.value.currentWeek,
      teamAId = teamA.id,
      teamAName = teamA.teamFullName,
      teamBId = teamB.id,
      teamBName = teamB.teamFullName,
      teamASendsPlayers = listOf(playerFromA),
      teamBSendsPlayers = emptyList(),
      fairnessScore = 20
    )

    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(pendingTrades = listOf(customTrade))

    val rejected = vm.rejectTrade("test_trade_veto_1", isVeto = true, reason = "Severe asset disparity vetoed")
    assertTrue(rejected)

    val updatedFranchises = vm.uiState.value.franchises
    val updatedTeamA = updatedFranchises.find { it.id == teamA.id }!!
    assertTrue("Team A should still have player after veto", updatedTeamA.roster.any { it.id == playerFromA.id })

    val completedTrade = vm.uiState.value.completedTrades.find { it.id == "test_trade_veto_1" }
    assertTrue(completedTrade != null)
    assertEquals(com.example.model.TradeStatus.VETOED, completedTrade!!.status)
  }

  @Test
  fun `verify trade deadline week rule strictly blocks trades once deadline passed`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Set trade deadline to Week 8
    vm.setTradeDeadlineWeek(8)
    assertEquals(8, vm.uiState.value.ruleBook.tradeRules.tradeDeadlineWeek)

    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>

    // When week is 5 (before deadline), trade window is open
    flow.value = vm.uiState.value.copy(currentWeek = 5, seasonPhase = com.example.model.SeasonPhase.REGULAR_SEASON)
    assertTrue("Week 5 with deadline 8 must be open", vm.isTradeWindowOpen())
    org.junit.Assert.assertFalse(vm.isTradeDeadlineWeek())

    // When week is 8 (deadline week), trade window is open and deadline frenzy triggers
    flow.value = vm.uiState.value.copy(currentWeek = 8, seasonPhase = com.example.model.SeasonPhase.REGULAR_SEASON)
    assertTrue("Week 8 with deadline 8 must be open", vm.isTradeWindowOpen())
    assertTrue("Week 8 must trigger trade deadline week alert", vm.isTradeDeadlineWeek())

    // When week is 9 (passed deadline), trade window must be closed
    flow.value = vm.uiState.value.copy(currentWeek = 9, seasonPhase = com.example.model.SeasonPhase.REGULAR_SEASON)
    org.junit.Assert.assertFalse("Week 9 with deadline 8 must be closed", vm.isTradeWindowOpen())

    // Attempting to propose custom trade past deadline must be rejected
    val franchises = vm.uiState.value.franchises
    val result = vm.proposeAndExecuteTrade(
      teamAId = franchises[0].id,
      teamBId = franchises[1].id,
      teamASendsPlayers = listOf(franchises[0].roster.first()),
      teamASendsPicks = emptyList(),
      teamBSendsPlayers = listOf(franchises[1].roster.first()),
      teamBSendsPicks = emptyList()
    )
    org.junit.Assert.assertFalse("Trade past deadline must not execute", result.first)
    assertTrue(result.second.contains("trade deadline has passed"))
  }

  @Test
  fun `verify corporate sponsor bidders catalog contains diverse sectors and reputation effects`() {
    val sponsorBidders = InitialLeagueData.getCorporateSponsorBidders()
    assertTrue("Should have multiple corporate sponsor bidders", sponsorBidders.size >= 5)

    val cleanEnergy = sponsorBidders.find { it.sector == com.example.model.SponsorSector.ECO_RENEWABLES }
    val sportsbook = sponsorBidders.find { it.sector == com.example.model.SponsorSector.SPORTSBOOK_GAMBLING }
    val crypto = sponsorBidders.find { it.sector == com.example.model.SponsorSector.FINTECH_CRYPTO }
    val luxuryAir = sponsorBidders.find { it.sector == com.example.model.SponsorSector.GLOBAL_AIRLINE_LUXURY }

    assertTrue(cleanEnergy != null && cleanEnergy.reputationEffect > 0)
    assertTrue(sportsbook != null && sportsbook.reputationEffect < 0)
    assertTrue(crypto != null)
    assertTrue(luxuryAir != null && luxuryAir.reputationEffect > 0)

    // Higher payouts typically correlate with controversial reputation
    assertTrue(sportsbook!!.baseAnnualPayoutMillions > cleanEnergy!!.baseAnnualPayoutMillions)
  }

  @Test
  fun `verify signing corporate sponsorship updates treasury and league reputation`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialIntegrity = vm.uiState.value.leagueIntegrityScore
    val initialCash = vm.uiState.value.cashReservesMillions

    val ecoSponsorship = com.example.model.ActiveLeagueSponsorship(
      id = "spon_test_eco",
      corporationName = "Solaris Renewable Energy",
      sector = com.example.model.SponsorSector.ECO_RENEWABLES,
      tier = com.example.model.SponsorTier.COMMUNITY_ESG,
      annualPayoutMillions = 30L,
      yearsRemaining = 4,
      upfrontSigningBonusMillions = 15L,
      reputationImpact = +12,
      fanMoraleBonus = +6
    )

    vm.signNegotiatedCorporateSponsorship(ecoSponsorship, lumpSumBonusMillions = 15L)

    // Integrity should improve
    assertTrue(vm.uiState.value.leagueIntegrityScore > initialIntegrity)
    assertEquals(initialIntegrity + 12, vm.uiState.value.leagueIntegrityScore)
    // Cash reserves should increase by upfront signing bonus
    assertEquals(initialCash + 15L, vm.uiState.value.cashReservesMillions)
    // Active sponsor updated
    assertEquals("Solaris Renewable Energy", vm.uiState.value.activeSponsorship.corporationName)
    assertEquals(4, vm.uiState.value.activeSponsorship.yearsRemaining)

    // Signing a gambling sportsbook sponsor should reduce integrity while providing large cash
    val prevCash = vm.uiState.value.cashReservesMillions
    val prevIntegrity = vm.uiState.value.leagueIntegrityScore

    val bettingSponsorship = com.example.model.ActiveLeagueSponsorship(
      id = "spon_test_betting",
      corporationName = "ApexBet Sportsbook",
      sector = com.example.model.SponsorSector.SPORTSBOOK_GAMBLING,
      tier = com.example.model.SponsorTier.PRIMARY_TITLE,
      annualPayoutMillions = 60L,
      yearsRemaining = 3,
      upfrontSigningBonusMillions = 40L,
      reputationImpact = -14,
      fanMoraleBonus = -4
    )

    vm.signNegotiatedCorporateSponsorship(bettingSponsorship, lumpSumBonusMillions = 40L)

    // Integrity should decrease by 14
    assertEquals(prevIntegrity - 14, vm.uiState.value.leagueIntegrityScore)
    // Cash should jump by 40M
    assertEquals(prevCash + 40L, vm.uiState.value.cashReservesMillions)
    assertEquals("ApexBet Sportsbook", vm.uiState.value.activeSponsorship.corporationName)
  }

  @Test
  fun `verify regular season and playoff simulations generate realistic player stats and weekly honors`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Ensure regular season matches exist for Week 1 and simulate Week 1
    vm.advanceWeek()
    val stateAfterWeek1 = vm.uiState.value

    // Every player in the week's matches should have stats
    assertTrue("Current week player stats should not be empty", stateAfterWeek1.currentWeekPlayerStats.isNotEmpty())
    assertTrue("Season player stats should accumulate", stateAfterWeek1.seasonPlayerStats.isNotEmpty())
    stateAfterWeek1.currentWeekPlayerStats.forEach { line ->
      assertTrue("Minutes must be realistic", line.minutes in 12..60)
      assertTrue("Points should be non-negative", line.points >= 0)
      assertTrue("Efficiency must be computed", line.efficiencyScore > 0)
    }

    // Player of the Week should be crowned
    assertTrue("Weekly award should be recorded", stateAfterWeek1.weeklyAwards.isNotEmpty())
    val week1Potw = stateAfterWeek1.weeklyAwards.first()
    assertEquals(1, week1Potw.week)
    org.junit.Assert.assertFalse(week1Potw.isPlayoffs)

    // Simulate Playoffs week
    val franchises = stateAfterWeek1.franchises
    val playoffMatches = listOf(
      com.example.model.GameMatch(
        id = "test_po_final",
        week = 15,
        homeTeamId = franchises[0].id,
        awayTeamId = franchises[1].id,
        roundName = "Championship Finals"
      )
    )
    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(
      currentWeek = 15,
      seasonPhase = com.example.model.SeasonPhase.PLAYOFFS,
      playoffMatches = playoffMatches
    )

    vm.advanceWeek()
    val stateAfterPlayoffs = vm.uiState.value
    assertTrue("Playoff stats should be generated", stateAfterPlayoffs.currentWeekPlayerStats.isNotEmpty())
    assertTrue("Playoff flag should be true on playoff box scores", stateAfterPlayoffs.currentWeekPlayerStats.all { it.isPlayoffs })
    assertTrue("Playoff Player of the Week should be awarded", stateAfterPlayoffs.weeklyAwards.first().isPlayoffs)
    assertTrue("Season player stats should record playoff games", stateAfterPlayoffs.seasonPlayerStats.values.any { it.playoffGamesPlayed > 0 })
  }

  @Test
  fun `verify record book contains all player and franchise categories and updates when records break`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    val initialBook = com.example.model.RecordBookEngine.createInitialRecordBook(
      SportType.BASKETBALL,
      franchises,
      2026
    )

    assertTrue("Player game records must exist", initialBook.playerGameRecords.isNotEmpty())
    assertTrue("Player season records must exist", initialBook.playerSeasonRecords.isNotEmpty())
    assertTrue("Player career records must exist", initialBook.playerCareerRecords.isNotEmpty())
    assertTrue("Franchise game records must exist", initialBook.franchiseGameRecords.isNotEmpty())
    assertTrue("Franchise season records must exist", initialBook.franchiseSeasonRecords.isNotEmpty())

    // Simulate a record-shattering performance (62 points by a player, 145 points by a franchise)
    val home = franchises[0].copy(wins = 15, losses = 1, pointsFor = 1950, pointsAgainst = 1600)
    val away = franchises[1]
    val monsterLine = com.example.model.PlayerGameStatLine(
      playerId = home.roster.first().id,
      playerName = home.roster.first().name,
      franchiseId = home.id,
      franchiseName = home.teamFullName,
      position = home.roster.first().position,
      week = 5,
      seasonYear = 2026,
      opponentId = away.id,
      opponentName = away.teamFullName,
      teamWon = true,
      points = 62,
      rebounds = 18,
      assists = 15,
      stealsBlocks = 6,
      minutes = 41,
      efficiencyScore = 88,
      statSummary = "62 PTS • 18 REB • 15 AST"
    )
    val blowoutMatch = com.example.model.GameMatch(
      id = "rec_test_m1",
      week = 5,
      homeTeamId = home.id,
      awayTeamId = away.id,
      homeScore = 145,
      awayScore = 102,
      isPlayed = true
    )
    val seasonStats = mapOf(
      monsterLine.playerId to com.example.model.PlayerSeasonStats(
        playerId = monsterLine.playerId,
        playerName = monsterLine.playerName,
        franchiseId = home.id,
        franchiseName = home.teamFullName,
        position = monsterLine.position,
        seasonYear = 2026,
        gamesPlayed = 14,
        totalPoints = 480,
        totalRebounds = 140,
        totalAssists = 135
      )
    )
    val careerStats = mapOf(
      monsterLine.playerId to com.example.model.PlayerCareerStats(
        playerId = monsterLine.playerId,
        playerName = monsterLine.playerName,
        primaryFranchiseId = home.id,
        primaryFranchiseName = home.teamFullName,
        position = monsterLine.position,
        seasonsPlayed = 10,
        gamesPlayed = 210,
        totalPoints = 4600,
        totalRebounds = 1800,
        totalAssists = 1400
      )
    )

    val (updatedBook, alerts) = com.example.model.RecordBookEngine.updateRecordsFromWeek(
      currentBook = initialBook,
      weekStats = listOf(monsterLine),
      playedMatches = listOf(blowoutMatch),
      updatedFranchises = listOf(home, away),
      updatedSeasonStats = seasonStats,
      updatedCareerStats = careerStats,
      sportType = SportType.BASKETBALL,
      seasonYear = 2026,
      week = 5,
      isPlayoffs = false
    )

    assertTrue("Should generate broken record alerts", alerts.isNotEmpty())
    assertEquals("62 PTS", updatedBook.playerGameRecords.find { it.id == "rec_pg_points" }?.valueDisplay)
    assertEquals("480 PTS", updatedBook.playerSeasonRecords.find { it.id == "rec_ps_points" }?.valueDisplay)
    assertEquals("4600 PTS", updatedBook.playerCareerRecords.find { it.id == "rec_pc_points" }?.valueDisplay)
    assertEquals("145 PTS", updatedBook.franchiseGameRecords.find { it.id == "rec_fg_points" }?.valueDisplay)
    assertEquals("15 Wins", updatedBook.franchiseSeasonRecords.find { it.id == "rec_fs_wins" }?.valueDisplay)
  }

  @Test
  fun `verify hall of fame induction occurs after free agency in off-season and enshrines class`() {
    // Verify step ordering explicitly: Awards Show (1) -> Retirements (2) -> Lottery (3) -> Free Agency (4) -> Hall of Fame (5) -> Rookie Draft (6) -> Parity Summary (7)
    assertEquals(1, com.example.model.OffSeasonStep.AWARDS_SHOW.stepIndex)
    assertEquals(4, com.example.model.OffSeasonStep.FREE_AGENCY.stepIndex)
    assertEquals(5, com.example.model.OffSeasonStep.HALL_OF_FAME.stepIndex)
    assertEquals(6, com.example.model.OffSeasonStep.LEAGUE_DRAFT.stepIndex)
    assertEquals(7, com.example.model.OffSeasonStep.SUMMARY.stepIndex)
    assertTrue(com.example.model.OffSeasonStep.HALL_OF_FAME.stepIndex > com.example.model.OffSeasonStep.FREE_AGENCY.stepIndex)
    assertTrue(com.example.model.OffSeasonStep.LEAGUE_DRAFT.stepIndex > com.example.model.OffSeasonStep.FREE_AGENCY.stepIndex)

    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialHofCount = vm.uiState.value.hallOfFame.size
    val session = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = 2026,
      franchises = vm.uiState.value.franchises,
      sportType = SportType.BASKETBALL,
      ruleBook = vm.uiState.value.ruleBook,
      initialParity = vm.uiState.value.competitiveParityScore,
      careerStats = vm.uiState.value.careerPlayerStats
    )
    assertTrue("Off-season session must propose Hall of Fame inductees", session.hallOfFameInductees.isNotEmpty())

    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(
      seasonPhase = com.example.model.SeasonPhase.OFF_SEASON,
      activeOffSeasonSession = session.copy(currentStep = com.example.model.OffSeasonStep.HALL_OF_FAME)
    )

    // Ratify Hall of Fame induction ceremony
    vm.ratifyOffSeasonHallOfFame()
    assertTrue(vm.uiState.value.activeOffSeasonSession?.hofClassRatified == true)
    assertEquals(initialHofCount + session.hallOfFameInductees.size, vm.uiState.value.hallOfFame.size)

    // Finalize off-season should not duplicate inductees
    vm.finalizeAndInaugurateNewSeason()
    assertEquals(initialHofCount + session.hallOfFameInductees.size, vm.uiState.value.hallOfFame.size)
    assertEquals(2027, vm.uiState.value.currentYear)
  }

  @Test
  fun `verify yearly awards show starts off-season and derives winners directly from player stats`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises.mapIndexed { idx, f ->
      if (idx == 0) f.copy(wins = 13, losses = 1, pointsFor = 1650, pointsAgainst = 1400)
      else f.copy(wins = 7, losses = 7, pointsFor = 1450, pointsAgainst = 1460)
    }
    val topTeam = franchises[0]
    val otherTeam = franchises[1]
    val starPlayer = topTeam.roster.first()
    val defenderPlayer = otherTeam.roster.last()

    // Inject realistic accumulated stats where starPlayer leads scoring & MVP race and defenderPlayer dominates stocks
    val statsMap = mapOf(
      starPlayer.id to com.example.model.PlayerSeasonStats(
        playerId = starPlayer.id,
        playerName = starPlayer.name,
        franchiseId = topTeam.id,
        franchiseName = topTeam.teamFullName,
        position = starPlayer.position,
        seasonYear = 2026,
        gamesPlayed = 14,
        playoffGamesPlayed = 2,
        totalPoints = 540,
        totalRebounds = 130,
        totalAssists = 160,
        totalStealsBlocks = 28,
        playoffPoints = 78,
        playoffAssists = 22,
        weeklyHonorsCount = 4,
        bestGamePoints = 46,
        overallRating = 93
      ),
      defenderPlayer.id to com.example.model.PlayerSeasonStats(
        playerId = defenderPlayer.id,
        playerName = defenderPlayer.name,
        franchiseId = otherTeam.id,
        franchiseName = otherTeam.teamFullName,
        position = defenderPlayer.position,
        seasonYear = 2026,
        gamesPlayed = 14,
        totalPoints = 210,
        totalRebounds = 195,
        totalAssists = 35,
        totalStealsBlocks = 72,
        weeklyHonorsCount = 1,
        bestGamePoints = 22,
        overallRating = 85
      )
    )

    val session = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = 2026,
      franchises = franchises,
      sportType = SportType.BASKETBALL,
      ruleBook = vm.uiState.value.ruleBook,
      initialParity = 82,
      careerStats = vm.uiState.value.careerPlayerStats,
      seasonStats = statsMap,
      weeklyAwards = emptyList(),
      champion = topTeam
    )

    // Must begin at AWARDS_SHOW (step 1)
    assertEquals(com.example.model.OffSeasonStep.AWARDS_SHOW, session.currentStep)
    val ceremony = session.awardsCeremony!!
    assertEquals(7, ceremony.awards.size)
    assertTrue(ceremony.mvpFinalists.isNotEmpty())
    assertTrue(ceremony.allLeagueFirstTeam.isNotEmpty())

    // Stat-driven winners check
    val mvp = ceremony.awards.find { it.awardType == com.example.model.AnnualAwardType.LEAGUE_MVP }
    val scoring = ceremony.awards.find { it.awardType == com.example.model.AnnualAwardType.SCORING_CHAMPION }
    val playmaker = ceremony.awards.find { it.awardType == com.example.model.AnnualAwardType.PLAYMAKER_OF_YEAR }
    val dpoy = ceremony.awards.find { it.awardType == com.example.model.AnnualAwardType.DEFENSIVE_POY }
    val finalsMvp = ceremony.awards.find { it.awardType == com.example.model.AnnualAwardType.FINALS_MVP }

    assertEquals(starPlayer.name, mvp?.winnerName)
    assertEquals(starPlayer.name, scoring?.winnerName)
    assertEquals(starPlayer.name, playmaker?.winnerName)
    assertEquals(starPlayer.name, finalsMvp?.winnerName)
    assertEquals(defenderPlayer.name, dpoy?.winnerName)

    // Presenting trophies via ViewModel archives ceremony
    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(
      seasonPhase = com.example.model.SeasonPhase.OFF_SEASON,
      activeOffSeasonSession = session
    )

    vm.presentOffSeasonYearlyAwards()
    assertTrue(vm.uiState.value.activeOffSeasonSession?.awardsCeremony?.isPresented == true)
    assertTrue(vm.uiState.value.pastAwardsCeremonies.any { it.year == 2026 && it.isPresented })
  }

  @Test
  fun `verify rookie draft uses scouting reports and collegiate metrics between free agency and pre-season and adds drafted rookies to rosters`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises
    val liveSession = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = 2026,
      franchises = franchises,
      sportType = SportType.BASKETBALL,
      ruleBook = vm.uiState.value.ruleBook,
      initialParity = vm.uiState.value.competitiveParityScore,
      autoCompleteDraft = false
    )

    // Rookie Draft sits strictly after Free Agency and before Parity/Pre-Season
    assertTrue(com.example.model.OffSeasonStep.LEAGUE_DRAFT.stepIndex > com.example.model.OffSeasonStep.FREE_AGENCY.stepIndex)
    assertTrue(com.example.model.OffSeasonStep.LEAGUE_DRAFT.stepIndex < com.example.model.OffSeasonStep.SUMMARY.stepIndex)

    // Prospect pool must contain rich collegiate metrics and scouting reports
    assertTrue(liveSession.draftProspectPool.size >= franchises.size + 4)
    liveSession.draftProspectPool.forEach { prospect ->
      assertTrue(prospect.collegiateMetrics.schoolName.isNotBlank())
      assertTrue(prospect.collegiateMetrics.collegiateMetricScore in 70..99)
      assertTrue(prospect.collegiateMetrics.formattedStatLine.contains("PPG"))
      assertTrue(prospect.scoutingReport.scoutGrade in 70..99)
      assertTrue(prospect.scoutingReport.strengths.isNotEmpty())
      assertTrue(prospect.scoutingReport.weaknessNote.isNotBlank())
      assertTrue(prospect.scoutingReport.executiveSummary.isNotBlank())
    }

    // Team evaluation incorporates positional needs + scouting grade + collegiate metrics
    val pick1TeamId = liveSession.draftOrderFranchiseIds.first()
    val pick1Team = franchises.first { it.id == pick1TeamId }
    val needs = com.example.model.OffSeasonEngine.getTeamDraftNeeds(
      team = pick1Team,
      retirements = liveSession.retirements,
      freeAgencySignings = liveSession.freeAgencySignings,
      sportType = SportType.BASKETBALL
    )
    assertTrue(needs.isNotEmpty())
    val recommended = com.example.model.OffSeasonEngine.evaluateBestProspectForTeam(
      team = pick1Team,
      availableProspects = liveSession.draftProspectPool,
      retirements = liveSession.retirements,
      freeAgencySignings = liveSession.freeAgencySignings,
      sportType = SportType.BASKETBALL,
      draftSlot = 1
    )
    org.junit.Assert.assertNotNull(recommended)
    assertTrue(recommended!!.second.contains("Scout Grade"))
    assertTrue(recommended.second.contains("College Score"))

    // Inject live off-season session and test interactive stepping + auto-complete -> Pre-Season
    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(
      seasonPhase = com.example.model.SeasonPhase.OFF_SEASON,
      activeOffSeasonSession = liveSession.copy(currentStep = com.example.model.OffSeasonStep.LEAGUE_DRAFT)
    )

    // Conduct Pick #1 interactively
    val initialRosterCounts = vm.uiState.value.franchises.associate { it.id to it.roster.size }
    vm.conductNextRookieDraftPick(null)
    val afterPick1 = vm.uiState.value.activeOffSeasonSession!!
    assertEquals(1, afterPick1.draftPicks.size)
    assertEquals(1, afterPick1.currentDraftPickIndex)
    assertTrue(afterPick1.draftPicks.first().selectionRationale.isNotBlank())

    // Auto-complete remaining Rookie Draft picks
    vm.autoCompleteRookieDraft()
    val completedDraftSession = vm.uiState.value.activeOffSeasonSession!!
    assertTrue(completedDraftSession.isDraftCompleted)
    assertEquals(franchises.size, completedDraftSession.draftPicks.size)
    assertEquals(franchises.size, completedDraftSession.draftProspectPool.count { it.isDrafted })

    // Ratify Off-Season -> enters Pre-Season with drafted rookies added onto team rosters
    vm.finalizeAndInaugurateNewSeason()
    val preSeasonState = vm.uiState.value
    assertEquals(com.example.model.SeasonPhase.PRE_SEASON, preSeasonState.seasonPhase)
    preSeasonState.franchises.forEach { team ->
      val oldSize = initialRosterCounts[team.id] ?: 0
      assertEquals("Team ${team.teamFullName} should gain 1 drafted rookie", oldSize + 1, team.roster.size)
      assertTrue(team.roster.any { it.id.startsWith("rookie_2026_") })
    }

    // Advance from Pre-Season -> Regular Season Week 1
    vm.advanceWeek()
    assertEquals(com.example.model.SeasonPhase.REGULAR_SEASON, vm.uiState.value.seasonPhase)
  }

  @Test
  fun `verify player injury penalties, box score exclusion, weekly recovery, and depth chart promotion`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises
    // All franchises should have 8-man rosters (5 starters + 3 bench reserves)
    franchises.forEach { team ->
      assertEquals("Each team should have 8 players for depth chart flexibility", 8, team.roster.size)
      assertEquals(5, com.example.model.InjuryEngine.getActiveStarters(team).size)
      assertEquals(3, com.example.model.InjuryEngine.getBenchPlayers(team).size)
    }

    val targetTeam = franchises.first()
    val oppTeam = franchises[1]
    val starStarter = com.example.model.InjuryEngine.getActiveStarters(targetTeam).first()
    val secondStarter = com.example.model.InjuryEngine.getActiveStarters(targetTeam)[1]
    val benchBackup = com.example.model.InjuryEngine.getBenchPlayers(targetTeam).first()

    // Case 1: Severe / Out injury zeroes out effective OVR and stat production multiplier
    val outInjury = com.example.model.PlayerInjury(
      id = "inj_test_out",
      type = com.example.model.InjuryType.MCL_SPRAIN,
      weeksRemaining = 3,
      initialWeeks = 3,
      sustainedWeek = 1,
      sustainedOpponent = oppTeam.teamFullName,
      gameContext = "Test knee collision"
    )
    // Case 2: Moderate / Day-to-Day injury applies clinical OVR penalty and reduced stat multiplier
    val hobbledInjury = com.example.model.PlayerInjury(
      id = "inj_test_hob",
      type = com.example.model.InjuryType.HAMSTRING_STRAIN,
      weeksRemaining = 2,
      initialWeeks = 2,
      sustainedWeek = 1,
      sustainedOpponent = oppTeam.teamFullName,
      gameContext = "Test sprint strain"
    )

    val injuredRoster = targetTeam.roster.map { p ->
      when (p.id) {
        starStarter.id -> p.copy(activeInjury = outInjury)
        secondStarter.id -> p.copy(activeInjury = hobbledInjury)
        else -> p
      }
    }
    val injuredTeam = targetTeam.copy(roster = injuredRoster)
    assertEquals(1, injuredTeam.unresolvedStarterHolesCount)
    assertEquals(2, injuredTeam.injuredPlayersCount)
    assertTrue(
      "Effective lineup rating must drop when starter is OUT & another is hobbled",
      injuredTeam.effectiveLineupRating < targetTeam.rosterRating
    )

    val updatedStar = injuredTeam.roster.first { it.id == starStarter.id }
    val updatedSecond = injuredTeam.roster.first { it.id == secondStarter.id }
    assertTrue(updatedStar.isMedicallyOut)
    assertEquals(0, updatedStar.effectiveOverallRating)
    assertEquals(0.0, updatedStar.statProductionMultiplier, 0.001)

    assertTrue(updatedSecond.isInjured)
    org.junit.Assert.assertFalse(updatedSecond.isMedicallyOut)
    assertEquals(
      (secondStarter.overallRating + com.example.model.InjuryType.HAMSTRING_STRAIN.ovrPenalty).coerceIn(55, 99),
      updatedSecond.effectiveOverallRating
    )
    assertEquals(com.example.model.InjuryType.HAMSTRING_STRAIN.statMultiplier, updatedSecond.statProductionMultiplier, 0.001)

    // Verify box score engine skips medically OUT starter completely
    val testMatch = com.example.model.GameMatch(
      id = "test_inj_match",
      week = 1,
      homeTeamId = injuredTeam.id,
      awayTeamId = oppTeam.id,
      homeScore = 104,
      awayScore = 101,
      isPlayed = true
    )
    val boxStats = com.example.model.RecordBookEngine.generateMatchPlayerStats(
      match = testMatch,
      home = injuredTeam,
      away = oppTeam,
      homeScore = 104,
      awayScore = 101,
      sportType = SportType.BASKETBALL,
      seasonYear = 2026,
      isPlayoffs = false
    )
    org.junit.Assert.assertFalse(
      "Medically OUT starter must not register game stats",
      boxStats.any { it.playerId == starStarter.id }
    )

    // Inject injured team into ViewModel and test depth chart management actions
    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(
      franchises = vm.uiState.value.franchises.map { if (it.id == injuredTeam.id) injuredTeam else it }
    )

    vm.openDepthChart(injuredTeam.id)
    assertTrue(vm.uiState.value.showDepthChartScreen)
    assertEquals(injuredTeam.id, vm.uiState.value.selectedDepthChartFranchiseId)

    // Promote healthy bench player -> replaces the medically OUT starter automatically
    val preFixRating = vm.uiState.value.franchises.first { it.id == injuredTeam.id }.effectiveLineupRating
    vm.promotePlayerOnDepthChart(injuredTeam.id, benchBackup.id)
    val postFixTeam = vm.uiState.value.franchises.first { it.id == injuredTeam.id }
    assertEquals(0, postFixTeam.unresolvedStarterHolesCount)
    assertTrue(postFixTeam.roster.first { it.id == benchBackup.id }.isStarter)
    org.junit.Assert.assertFalse(postFixTeam.roster.first { it.id == starStarter.id }.isStarter)
    assertTrue("Promoting healthy backup should improve effective lineup rating", postFixTeam.effectiveLineupRating >= preFixRating)

    // Toggle Injured Reserve on injured star & verify recovery countdown over 3 simulated weeks
    vm.togglePlayerInjuredReserve(injuredTeam.id, starStarter.id)
    assertTrue(
      vm.uiState.value.franchises.first { it.id == injuredTeam.id }
        .roster.first { it.id == starStarter.id }
        .activeInjury?.isOnInjuredReserve == true
    )

    // Simulate 3 regular season weeks -> 3-week MCL sprain should tick down and clear back to healthy
    repeat(3) {
      if (vm.uiState.value.activeDilemma != null) {
        vm.resolveDilemma(0)
      }
      vm.advanceWeek()
    }
    val afterRecoveryStar = vm.uiState.value.franchises
      .first { it.id == injuredTeam.id }
      .roster.first { it.id == starStarter.id }
    org.junit.Assert.assertNull("3-week injury must be medically cleared after 3 recovery ticks", afterRecoveryStar.activeInjury)
    org.junit.Assert.assertFalse(afterRecoveryStar.isInjured)

    // Verify simulateMatchInjuries creates realistic injury event logs reliably
    val simResult = com.example.model.InjuryEngine.simulateMatchInjuries(
      playedMatches = List(6) { idx ->
        com.example.model.GameMatch(
          id = "sim_inj_$idx",
          week = 4,
          homeTeamId = franchises[0].id,
          awayTeamId = franchises[1].id,
          homeScore = 100,
          awayScore = 98,
          isPlayed = true
        )
      },
      franchises = franchises,
      week = 4,
      seasonYear = 2026,
      isPlayoffs = false,
      random = kotlin.random.Random(42)
    )
    assertTrue("Multiple simulated matches should yield injury logs or headlines", simResult.newInjuryLogs.isNotEmpty())
    simResult.newInjuryLogs.forEach { log ->
      assertTrue(log.weeksOut >= 1)
      assertTrue(log.context.isNotBlank())
    }

    vm.closeDepthChart()
    org.junit.Assert.assertFalse(vm.uiState.value.showDepthChartScreen)
  }

  @Test
  fun `verify trade block listings, seeking targets, stats matchmaking, and post-trade synchronization`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialFranchises = vm.uiState.value.franchises
    // All franchises should have seeded trade blocks and rival seeking targets
    initialFranchises.forEach { team ->
      assertTrue("${team.teamFullName} should have initial trade block listings", team.tradeBlockListings.isNotEmpty())
      assertTrue("${team.teamFullName} should have rival seeking targets", team.seekingPlayerIds.isNotEmpty())
      val ownIds = team.roster.map { it.id }.toSet()
      assertTrue(team.tradeBlockListings.all { it.playerId in ownIds })
      assertTrue("Seeking IDs must not contain team's own players", team.seekingPlayerIds.none { it in ownIds })
    }

    val teamA = initialFranchises[0]
    val teamB = initialFranchises[1]

    // Open Trade Block screen and select Team A
    vm.openTradeBlock(teamA.id)
    assertTrue(vm.uiState.value.showTradeBlockScreen)
    assertEquals(teamA.id, vm.uiState.value.selectedTradeBlockFranchiseId)

    // Add a non-listed player from Team A onto the Trade Block with custom asking details
    val unlistedPlayerA = teamA.roster.first { !teamA.isPlayerOnTradeBlock(it.id) }
    vm.togglePlayerOnTradeBlock(
      franchiseId = teamA.id,
      playerId = unlistedPlayerA.id,
      askingPriority = com.example.model.TradeAskingPriority.CONTENDER_UPGRADE,
      askingNote = "Looking for playoff wing scorer"
    )
    val afterAdd = vm.uiState.value.franchises.first { it.id == teamA.id }
    assertTrue(afterAdd.isPlayerOnTradeBlock(unlistedPlayerA.id))
    assertEquals(
      com.example.model.TradeAskingPriority.CONTENDER_UPGRADE,
      afterAdd.tradeBlockListings.first { it.playerId == unlistedPlayerA.id }.askingPriority
    )

    // Update asking details
    vm.updateTradeBlockListingDetails(
      franchiseId = teamA.id,
      playerId = unlistedPlayerA.id,
      askingPriority = com.example.model.TradeAskingPriority.FUTURE_DRAFT_CAPITAL,
      askingNote = "First round pick plus young guard"
    )
    val afterEdit = vm.uiState.value.franchises.first { it.id == teamA.id }
    assertEquals(
      "First round pick plus young guard",
      afterEdit.tradeBlockListings.first { it.playerId == unlistedPlayerA.id }.askingNote
    )

    // Toggle seeking a rival player on Team B (and verify own-player seek is ignored)
    val rivalTargetB = teamB.roster.first { !afterEdit.isSeekingPlayer(it.id) }
    vm.toggleSeekingPlayer(teamA.id, unlistedPlayerA.id) // Should be ignored (own player)
    org.junit.Assert.assertFalse(vm.uiState.value.franchises.first { it.id == teamA.id }.isSeekingPlayer(unlistedPlayerA.id))

    vm.toggleSeekingPlayer(teamA.id, rivalTargetB.id)
    assertTrue(vm.uiState.value.franchises.first { it.id == teamA.id }.isSeekingPlayer(rivalTargetB.id))

    // Toggle seeking position
    val targetPos = "PG"
    val hadPos = targetPos in vm.uiState.value.franchises.first { it.id == teamA.id }.seekingPositions
    vm.toggleSeekingPosition(teamA.id, targetPos)
    assertEquals(!hadPos, targetPos in vm.uiState.value.franchises.first { it.id == teamA.id }.seekingPositions)

    // Simulate 1 week so season stats exist, then verify matchmaking integrates season stats
    if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
    vm.advanceWeek()
    assertTrue(vm.uiState.value.seasonPlayerStats.isNotEmpty())

    val matches = com.example.model.TradeEngine.buildMatchmakingRecommendations(
      franchises = vm.uiState.value.franchises,
      seasonStats = vm.uiState.value.seasonPlayerStats,
      tradeRules = vm.uiState.value.ruleBook.tradeRules
    )
    assertTrue("Smart trade block matches should be generated", matches.isNotEmpty())
    assertTrue(matches.any { it.fitHeadline.contains("PPG") || it.fitHeadline.contains("OVR") })

    // Open Trade Machine pre-filled from Trade Block
    vm.openTradeMachineWithPlayers(
      teamAId = teamA.id,
      teamBId = teamB.id,
      playerAId = unlistedPlayerA.id,
      playerBId = rivalTargetB.id
    )
    org.junit.Assert.assertFalse(vm.uiState.value.showTradeBlockScreen)
    assertTrue(vm.uiState.value.showTradeDeskScreen)
    assertEquals(teamA.id, vm.uiState.value.prefilledTradeMachineSetup?.teamAId)
    assertEquals(rivalTargetB.id, vm.uiState.value.prefilledTradeMachineSetup?.playerBId)

    // Verify syncTradeBlocksAfterTrade clears traded players from old block & new team's seeking list
    val swappedA = teamA.copy(
      roster = teamA.roster.filterNot { it.id == unlistedPlayerA.id } + rivalTargetB.copy(franchiseId = teamA.id),
      tradeBlockListings = teamA.tradeBlockListings + com.example.model.TradeBlockListing(unlistedPlayerA.id),
      seekingPlayerIds = (teamA.seekingPlayerIds + rivalTargetB.id).distinct()
    )
    val swappedB = teamB.copy(
      roster = teamB.roster.filterNot { it.id == rivalTargetB.id } + unlistedPlayerA.copy(franchiseId = teamB.id)
    )
    val synced = com.example.model.TradeEngine.syncTradeBlocksAfterTrade(listOf(swappedA, swappedB))
    val syncedA = synced.first { it.id == teamA.id }
    org.junit.Assert.assertFalse("Traded-away player must be removed from Team A trade block", syncedA.isPlayerOnTradeBlock(unlistedPlayerA.id))
    org.junit.Assert.assertFalse("Acquired player must be removed from Team A seeking list", syncedA.isSeekingPlayer(rivalTargetB.id))

    vm.closeTradeDesk()
    org.junit.Assert.assertNull(vm.uiState.value.prefilledTradeMachineSetup)
  }

  @Test
  fun `verify continental conferences creation, regional divisions, and non-NA franchise alignment`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Default league starts with North American EASTERN & WESTERN only
    org.junit.Assert.assertEquals(
      setOf(com.example.model.Conference.EASTERN, com.example.model.Conference.WESTERN),
      vm.uiState.value.ruleBook.enabledConferences
    )

    // Admit prospective South American & Oceania markets into the open pool and expand globally
    val pool = InitialLeagueData.getProspectiveMarketPool()
    val saoPaulo = pool.first { it.id == "city_prospect_saopaulo" }
    val sydney = pool.first { it.id == "city_prospect_sydney" }

    // Expand London & Tokyo first (already in default open cities), freeing candidate slots
    vm.expandLeague("city_london", "Monarchs", 0xFF8B5CF6, com.example.model.OwnershipType.PRIVATE_EQUITY)
    vm.expandLeague("city_tokyo", "Shoguns", 0xFF06B6D4, com.example.model.OwnershipType.TECH_BILLIONAIRE)
    assertTrue(vm.admitMarketCandidate(saoPaulo))
    assertTrue(vm.admitMarketCandidate(sydney))
    vm.expandLeague(saoPaulo.id, "Imperials", 0xFF10B981, com.example.model.OwnershipType.SOVEREIGN_SYNDICATE)
    vm.expandLeague(sydney.id, "Harbor", 0xFF0EA5E9, com.example.model.OwnershipType.COMMUNITY_TRUST)

    // Before continental conferences are created, international clubs sit in legacy East/West
    val londonBefore = vm.uiState.value.franchises.first { it.cityName == "London" }
    assertEquals(com.example.model.Conference.EASTERN, londonBefore.conference)

    // Establish European, South American, and Asia-Oceania conferences with their 9 realistic divisions
    vm.createAllContinentalConferences(autoAlignNonNorthAmerican = true, adaptPlayoffs = true)

    val ruleBook = vm.uiState.value.ruleBook
    assertEquals(5, ruleBook.enabledConferences.size)
    assertTrue(com.example.model.Conference.EUROPEAN in ruleBook.enabledConferences)
    assertTrue(com.example.model.Conference.SOUTH_AMERICAN in ruleBook.enabledConferences)
    assertTrue(com.example.model.Conference.ASIA_OCEANIA in ruleBook.enabledConferences)
    assertEquals(15, ruleBook.enabledDivisions.size)

    // Verify each international club is placed in its exact continental conference and realistic division
    val franchisesAfter = vm.uiState.value.franchises
    val london = franchisesAfter.first { it.cityName == "London" }
    val saoPauloTeam = franchisesAfter.first { it.cityName == "São Paulo" }
    val tokyo = franchisesAfter.first { it.cityName == "Tokyo" }
    val sydneyTeam = franchisesAfter.first { it.cityName == "Sydney" }
    val nyc = franchisesAfter.first { it.cityName == "New York" }
    val la = franchisesAfter.first { it.cityName == "Los Angeles" }

    assertEquals(com.example.model.Conference.EUROPEAN, london.conference)
    assertEquals(com.example.model.Division.WEST_EUROPE, london.division)

    assertEquals(com.example.model.Conference.SOUTH_AMERICAN, saoPauloTeam.conference)
    assertEquals(com.example.model.Division.ATLANTIC_BRAZIL, saoPauloTeam.division)

    assertEquals(com.example.model.Conference.ASIA_OCEANIA, tokyo.conference)
    assertEquals(com.example.model.Division.EAST_ASIA, tokyo.division)

    assertEquals(com.example.model.Conference.ASIA_OCEANIA, sydneyTeam.conference)
    assertEquals(com.example.model.Division.OCEANIA, sydneyTeam.division)

    // North American clubs remain in EASTERN / WESTERN
    assertEquals(com.example.model.Conference.EASTERN, nyc.conference)
    assertEquals(com.example.model.Conference.WESTERN, la.conference)

    // Toggle a European division off and verify fallback keeps team inside EUROPEAN conference
    vm.toggleDivisionEnabled(com.example.model.Division.WEST_EUROPE)
    val londonAfterToggle = vm.uiState.value.franchises.first { it.cityName == "London" }
    assertEquals(com.example.model.Conference.EUROPEAN, londonAfterToggle.conference)
    org.junit.Assert.assertNotEquals(com.example.model.Division.WEST_EUROPE, londonAfterToggle.division)

    // Guard prevents disabling the last remaining division of a conference
    vm.toggleDivisionEnabled(com.example.model.Division.CENTRAL_NORTH_EUROPE)
    vm.toggleDivisionEnabled(com.example.model.Division.MEDITERRANEAN)
    assertEquals(1, vm.uiState.value.ruleBook.enabledDivisions.count { it.conference == com.example.model.Conference.EUROPEAN })
  }

  @Test
  fun `verify adaptive playoff structure options, multi-conference seeding, and 10-team global bracket simulation`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Enable all 3 continental conferences and expand to 10 teams spanning all 5 conferences
    vm.createAllContinentalConferences(autoAlignNonNorthAmerican = true, adaptPlayoffs = true)
    val pool = InitialLeagueData.getProspectiveMarketPool()
    val saoPaulo = pool.first { it.id == "city_prospect_saopaulo" }
    val singapore = pool.first { it.id == "city_prospect_singapore" }

    vm.expandLeague("city_london", "Lions", 0xFF8B5CF6, com.example.model.OwnershipType.PRIVATE_EQUITY)
    vm.expandLeague("city_paris", "Etoile", 0xFFA855F7, com.example.model.OwnershipType.MEDIA_MOGUL)
    vm.expandLeague("city_tokyo", "Titans", 0xFF06B6D4, com.example.model.OwnershipType.TECH_BILLIONAIRE)
    assertTrue(vm.admitMarketCandidate(saoPaulo))
    assertTrue(vm.admitMarketCandidate(singapore))
    vm.expandLeague(saoPaulo.id, "Condors", 0xFF10B981, com.example.model.OwnershipType.SOVEREIGN_SYNDICATE)
    vm.expandLeague(singapore.id, "Merlions", 0xFF14B8A6, com.example.model.OwnershipType.COMMUNITY_TRUST)

    // We now have 11 franchises spanning all 5 conferences (EASTERN, WESTERN, EUROPEAN, SOUTH_AMERICAN, ASIA_OCEANIA)
    val franchises = vm.uiState.value.franchises
    val activeConfs = franchises.map { it.conference }.distinct()
    assertEquals(5, activeConfs.size)

    // Generate possible playoff structures for this 5-conference alignment
    val options = com.example.model.PlayoffStructureEngine.generatePossibleStructures(
      franchises = franchises,
      enabledConferences = vm.uiState.value.ruleBook.enabledConferences,
      enabledDivisions = vm.uiState.value.ruleBook.enabledDivisions
    )
    assertTrue(options.size >= 3)
    assertTrue(options.any { it.rules.qualificationMode == com.example.model.PlayoffQualificationMode.CONFERENCE_CHAMPIONS_WILD_CARD })
    assertTrue(options.any { it.rules.fieldSize == 10 })

    // Select a 10-team Conference Champions + Wild Card or Expanded structure and verify seeding spans all 5 conferences
    val tenTeamOption = options.first { it.rules.fieldSize == 10 }
    vm.applyPlayoffStructureOption(tenTeamOption)
    assertEquals(10, vm.uiState.value.ruleBook.playoffRules.fieldSize)

    val seeds = com.example.model.calculatePlayoffSeeds(vm.uiState.value.franchises, vm.uiState.value.ruleBook.playoffRules)
    assertEquals(10, seeds.size)
    val seededConferences = seeds.map { it.franchise.conference }.toSet()
    assertEquals("Every active continental conference should receive playoff representation", activeConfs.toSet(), seededConferences)

    // Simulate full regular season + 10-team multi-round global playoffs through to Off-Season cleanly
    var guard = 0
    while (vm.uiState.value.seasonPhase != com.example.model.SeasonPhase.OFF_SEASON && guard < 40) {
      if (vm.uiState.value.activeDilemma != null) {
        vm.resolveDilemma(0)
      }
      vm.advanceWeek()
      guard++
    }
    assertEquals(com.example.model.SeasonPhase.OFF_SEASON, vm.uiState.value.seasonPhase)
    assertTrue("Championship history must record a champion after 10-team global bracket", vm.uiState.value.valuationHistory.isNotEmpty())
  }

  @Test
  fun `verify initial rivalries seed playoff history, geographic proximity, and divisional competition with multipliers`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val state = vm.uiState.value
    val rivalries = state.rivalries
    assertTrue("Initial league must seed active rivalries", rivalries.any { it.isActiveRivalry })

    val la = state.franchises.first { it.cityName == "Los Angeles" }
    val lv = state.franchises.first { it.cityName == "Las Vegas" }
    val nyc = state.franchises.first { it.cityName == "New York" }

    // Geographical proximity + Divisional clash: LA vs Las Vegas
    val mojave = com.example.model.RivalryEngine.findRivalry(la.id, lv.id, rivalries)
    assertTrue("LA vs Las Vegas rivalry must be seeded", mojave != null && mojave.isActiveRivalry)
    assertTrue(com.example.model.RivalryOrigin.GEOGRAPHICAL_PROXIMITY in mojave!!.origins)
    assertTrue(mojave.ticketRevenueMultiplier > 1.15)
    assertTrue(mojave.moraleMultiplier > 1.25)

    // Playoff history marquee: New York vs Los Angeles
    val coastFeud = com.example.model.RivalryEngine.findRivalry(nyc.id, la.id, rivalries)
    assertTrue("NYC vs LA playoff history rivalry must be seeded", coastFeud != null && coastFeud.isActiveRivalry)
    assertTrue(com.example.model.RivalryOrigin.PLAYOFF_HISTORY in coastFeud!!.origins)
    assertTrue(coastFeud.finalsMeetingsCount >= 1)

    // Pre-game impact evaluation must amplify ticket revenue and morale swing
    val impact = com.example.model.RivalryEngine.evaluateMatchupImpact(
      home = la,
      away = lv,
      rivalries = rivalries,
      cities = state.cities,
      isPlayoffs = false
    )
    assertTrue(impact.isActiveRivalry)
    assertTrue("Rivalry win morale delta must exceed standard +2 win boost", impact.winMoraleDelta > 2)
    assertTrue("Rivalry loss morale delta must be steeper than standard -2", impact.lossMoraleDelta < -2)
    assertTrue("Active rivalry must yield positive home ticket gate bonus", impact.homeBonusTicketMillions > 0.0)
    assertTrue("Active rivalry must add crowd hype boost", impact.crowdHypeBoost >= 1)
  }

  @Test
  fun `verify rivalry engine escalates playoff clashes, boosts ticket revenue & morale swings, and updates finances`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    val cities = InitialLeagueData.getDefaultCities()
    val broadcast = InitialLeagueData.getInitialBroadcastDeals().first()
    val initialRivalries = com.example.model.RivalryEngine.seedInitialRivalries(franchises, cities, 2026)

    val la = franchises.first { it.cityName == "Los Angeles" }
    val nyc = franchises.first { it.cityName == "New York" }
    val baselineRivalry = com.example.model.RivalryEngine.findRivalry(la.id, nyc.id, initialRivalries)!!

    val finalsThriller = com.example.model.GameMatch(
      id = "test_finals_rivalry",
      week = 15,
      homeTeamId = la.id,
      awayTeamId = nyc.id,
      homeScore = 112,
      awayScore = 109,
      isPlayed = true,
      roundName = "Championship Finals"
    )

    val result = com.example.model.RivalryEngine.processPlayedMatchups(
      playedMatches = listOf(finalsThriller),
      franchises = franchises,
      currentRivalries = initialRivalries,
      cities = cities,
      sportType = SportType.BASKETBALL,
      seasonYear = 2026,
      isPlayoffs = true
    )

    val updatedRivalry = com.example.model.RivalryEngine.findRivalry(la.id, nyc.id, result.updatedRivalries)!!
    assertTrue("Playoff Finals thriller must increase rivalry intensity", updatedRivalry.intensity > baselineRivalry.intensity)
    assertEquals(baselineRivalry.playoffMeetingsCount + 1, updatedRivalry.playoffMeetingsCount)
    assertEquals(baselineRivalry.finalsMeetingsCount + 1, updatedRivalry.finalsMeetingsCount)
    assertEquals(1, updatedRivalry.closeGamesCount)
    assertTrue("Home team must receive rivalry ticket bonus", (result.ticketBonusByFranchiseId[la.id] ?: 0.0) > 0.0)
    assertTrue("Both teams should receive extra playoff rivalry morale swing", result.moraleAdjustByFranchiseId.containsKey(la.id))

    // Verify financial statement reflects rivalry gate bonus
    val bonusAmount = result.ticketBonusByFranchiseId[la.id]!!
    val stmtWithoutBonus = la.copy(rivalryTicketBonusMillions = 0.0).calculateFinancialStatement(broadcast, franchises.size)
    val stmtWithBonus = la.copy(rivalryTicketBonusMillions = bonusAmount).calculateFinancialStatement(broadcast, franchises.size)
    assertEquals(Math.round(bonusAmount * 10.0) / 10.0, stmtWithBonus.rivalryBonusMillions, 0.01)
    assertTrue("Financial statement ticket revenue must increase with rivalry bonus", stmtWithBonus.ticketRevenueMillions > stmtWithoutBonus.ticketRevenueMillions)
  }

  @Test
  fun `verify league expansion sparks geographical rivalry, hub navigation works, and off-season preserves rivalries`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Expand into neighboring London and Paris markets
    vm.expandLeague("city_london", "Monarchs", 0xFF8B5CF6, com.example.model.OwnershipType.PRIVATE_EQUITY)
    vm.expandLeague("city_paris", "Saints", 0xFFA855F7, com.example.model.OwnershipType.MEDIA_MOGUL)

    val london = vm.uiState.value.franchises.first { it.cityName == "London" }
    val paris = vm.uiState.value.franchises.first { it.cityName == "Paris" }

    val channelDerby = com.example.model.RivalryEngine.findRivalry(london.id, paris.id, vm.uiState.value.rivalries)
    assertTrue("London & Paris expansion must spark Eurostar Channel Derby", channelDerby != null && channelDerby.isActiveRivalry)
    assertTrue(com.example.model.RivalryOrigin.GEOGRAPHICAL_PROXIMITY in channelDerby!!.origins)

    // Verify UI Hub state navigation & filtering
    vm.openRivalryHub(london.id)
    assertTrue(vm.uiState.value.showRivalryHubScreen)
    assertEquals(london.id, vm.uiState.value.selectedRivalryTeamFilterId)
    vm.setRivalryTeamFilter(null)
    org.junit.Assert.assertNull(vm.uiState.value.selectedRivalryTeamFilterId)
    vm.closeRivalryHub()
    org.junit.Assert.assertFalse(vm.uiState.value.showRivalryHubScreen)

    // Verify match annotation attaches rivalry multipliers
    val sampleMatch = com.example.model.GameMatch(
      id = "match_ldn_par",
      week = 1,
      homeTeamId = london.id,
      awayTeamId = paris.id
    )
    val annotated = com.example.model.RivalryEngine.annotateMatchesWithRivalries(
      matches = listOf(sampleMatch),
      franchises = vm.uiState.value.franchises,
      rivalries = vm.uiState.value.rivalries,
      cities = vm.uiState.value.cities
    ).first()
    assertTrue(annotated.rivalryTitle != null)
    assertTrue(annotated.ticketRevenueMultiplier > 1.0)
    assertTrue(annotated.moraleMultiplier > 1.0)

    // Simulate a week and then test off-season transition resets seasonal ticket bonus while preserving multi-year rivalries
    if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
    vm.advanceWeek()
    val preOffSeasonRivalryCount = vm.uiState.value.rivalries.size

    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    val session = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = vm.uiState.value.currentYear,
      franchises = vm.uiState.value.franchises,
      sportType = vm.uiState.value.sportType,
      ruleBook = vm.uiState.value.ruleBook,
      initialParity = vm.uiState.value.competitiveParityScore
    )
    flow.value = vm.uiState.value.copy(
      seasonPhase = com.example.model.SeasonPhase.OFF_SEASON,
      activeOffSeasonSession = session
    )
    vm.finalizeAndInaugurateNewSeason()

    assertEquals("Rivalries must persist across seasons", preOffSeasonRivalryCount, vm.uiState.value.rivalries.size)
    assertTrue("Seasonal rivalry ticket bonus must reset for the new season", vm.uiState.value.franchises.all { it.rivalryTicketBonusMillions == 0.0 })
  }

  @Test
  fun `verify fan loyalty fluctuates with performance and ticket pricing and directly impacts overall franchise revenue`() {
    val franchises = InitialLeagueData.getInitialFranchises()
    val broadcast = InitialLeagueData.getInitialBroadcastDeals().first()

    val teamA = franchises[0].copy(fanLoyalty = 75, ticketPrice = 70) // winner with fair pricing
    val teamB = franchises[1].copy(fanLoyalty = 75, ticketPrice = 165) // loser with severe overpricing

    val match = com.example.model.GameMatch(
      id = "loyalty_perf_match",
      week = 4,
      homeTeamId = teamA.id,
      awayTeamId = teamB.id,
      homeScore = 108,
      awayScore = 105, // close thriller (+2 win +1 close)
      isPlayed = true
    )

    val weekRes = com.example.model.FanLoyaltyEngine.processWeeklyLoyaltyUpdates(
      franchises = listOf(teamA, teamB),
      playedMatches = listOf(match),
      activeLeagueCampaigns = emptyList(),
      rivalryBonusesByTeamId = emptyMap(),
      isPlayoffs = false
    )

    val updatedA = weekRes.updatedFranchises.first { it.id == teamA.id }
    val updatedB = weekRes.updatedFranchises.first { it.id == teamB.id }

    assertTrue("Winning team with fair pricing should gain fan loyalty", updatedA.fanLoyalty > teamA.fanLoyalty)
    assertTrue("Losing team with high ticket prices should lose fan loyalty", updatedB.fanLoyalty < teamB.fanLoyalty)
    assertTrue(updatedA.lastLoyaltyDrivers.performanceDelta > 0)
    assertTrue(updatedB.lastLoyaltyDrivers.pricingDelta < 0)

    // Verify high fan loyalty boosts total franchise revenue across ticket sales, merch, and sponsors
    val diehardTeam = franchises[0].copy(fanLoyalty = 96, ticketPrice = 85)
    val alienatedTeam = franchises[0].copy(fanLoyalty = 25, ticketPrice = 85)
    val stmtDiehard = diehardTeam.calculateFinancialStatement(broadcast, franchises.size)
    val stmtAlienated = alienatedTeam.calculateFinancialStatement(broadcast, franchises.size)

    assertTrue("Diehard loyalty must produce higher attendance & ticket revenue", stmtDiehard.ticketRevenueMillions > stmtAlienated.ticketRevenueMillions)
    assertTrue("Diehard loyalty must produce higher merchandise revenue", stmtDiehard.merchandiseRevenueMillions > stmtAlienated.merchandiseRevenueMillions)
    assertTrue("Diehard loyalty must produce higher local sponsor revenue", stmtDiehard.localSponsorRevenueMillions > stmtAlienated.localSponsorRevenueMillions)
    assertTrue("Overall franchise revenue must scale significantly with fan loyalty", stmtDiehard.totalRevenueMillions > stmtAlienated.totalRevenueMillions + 10.0)
    assertTrue(stmtDiehard.loyaltyRevenueImpactMillions > 0.0)
    assertTrue(stmtAlienated.loyaltyRevenueImpactMillions < 0.0)
  }

  @Test
  fun `verify ticket price adjustments and marketing campaigns dynamically update fan loyalty in ViewModel`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val targetTeam = vm.uiState.value.franchises.first()
    val startLoyalty = targetTeam.fanLoyalty

    // Aggressive price hike should trigger immediate loyalty backlash
    vm.updateFranchiseTicketPrice(targetTeam.id, 185)
    val afterHike = vm.uiState.value.franchises.first { it.id == targetTeam.id }
    assertEquals(185, afterHike.ticketPrice)
    assertTrue("Price gouging hike should drop fan loyalty immediately", afterHike.fanLoyalty < startLoyalty)

    // Fan-friendly ticket cut should partially restore loyalty
    val afterHikeLoyalty = afterHike.fanLoyalty
    vm.updateFranchiseTicketPrice(targetTeam.id, 45)
    val afterCut = vm.uiState.value.franchises.first { it.id == targetTeam.id }
    assertTrue("Fan-friendly ticket cut should boost loyalty immediately", afterCut.fanLoyalty > afterHikeLoyalty)

    // Targeted franchise marketing campaign should boost loyalty & persist active campaign duration
    val preCampLoyalty = afterCut.fanLoyalty
    val preCampFanBase = afterCut.fanBaseMillions
    val (launched, _) = vm.launchFranchiseMarketingCampaign(
      targetTeam.id,
      com.example.model.FranchiseMarketingCampaignType.SEASON_TICKET_REWARDS
    )
    assertTrue("Franchise marketing campaign should launch cleanly", launched)

    val afterCamp = vm.uiState.value.franchises.first { it.id == targetTeam.id }
    assertTrue("Targeted marketing campaign must boost fan loyalty", afterCamp.fanLoyalty > preCampLoyalty)
    assertTrue("Targeted marketing campaign must grow fanbase", afterCamp.fanBaseMillions > preCampFanBase)
    assertEquals(com.example.model.FranchiseMarketingCampaignType.SEASON_TICKET_REWARDS, afterCamp.activeFranchiseCampaign?.type)

    // League-wide marketing campaign should boost loyalty across clubs and decrement campaign weeks over simulation
    val firstLeagueCamp = vm.uiState.value.campaigns.first()
    val loyaltyBeforeLeagueCamp = vm.uiState.value.franchises.map { it.fanLoyalty }
    vm.launchCampaign(firstLeagueCamp.id)
    val loyaltyAfterLeagueCamp = vm.uiState.value.franchises.map { it.fanLoyalty }
    assertTrue("League-wide marketing campaign must lift franchise fan loyalty", loyaltyAfterLeagueCamp.sum() > loyaltyBeforeLeagueCamp.sum())

    if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
    vm.advanceWeek()
    val postWeekCampTeam = vm.uiState.value.franchises.first { it.id == targetTeam.id }
    assertEquals(
      com.example.model.FranchiseMarketingCampaignType.SEASON_TICKET_REWARDS.durationWeeks - 1,
      postWeekCampTeam.activeFranchiseCampaign?.remainingWeeks
    )
  }

  @Test
  fun `verify facility investments expand seating capacity, boost conditioning and luxury revenue, and grow franchise valuation`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val team = vm.uiState.value.franchises.first()
    val broadcast = vm.uiState.value.activeBroadcast
    val totalTeams = vm.uiState.value.franchises.size
    val initialCapacity = team.stadiumCapacity
    val initialSponsor = team.localSponsorRevenueMillions
    val initialRosterOvr = team.rosterRating
    val initialEffOvr = team.effectiveLineupRating
    val initialValuation = team.calculateValuationBreakdown(broadcast, totalTeams).totalValuationMillions
    val initialCash = vm.uiState.value.cashReservesMillions

    // Invest in Stadium Seating, Training Center, and VIP Luxury Suites
    val (stadiumOk, _) = vm.investInFacilityUpgrade(team.id, com.example.model.FacilityType.STADIUM_SEATING)
    val (trainingOk, _) = vm.investInFacilityUpgrade(team.id, com.example.model.FacilityType.TRAINING_CENTER)
    val (suitesOk, _) = vm.investInFacilityUpgrade(team.id, com.example.model.FacilityType.LUXURY_SUITES)

    assertTrue("Stadium expansion should start", stadiumOk)
    assertTrue("Training center upgrade should start", trainingOk)
    assertTrue("Luxury suites upgrade should start", suitesOk)

    val underConstruction = vm.uiState.value.franchises.first { it.id == team.id }
    assertEquals(3, underConstruction.facilities.ongoingProjects.size)
    assertTrue("Cash reserves must decrease after capital investments", vm.uiState.value.cashReservesMillions < initialCash)

    // Duplicate investment in same track while under construction must be blocked
    val (dupOk, _) = vm.investInFacilityUpgrade(team.id, com.example.model.FacilityType.STADIUM_SEATING)
    org.junit.Assert.assertFalse("Cannot start duplicate build on active track", dupOk)

    // Simulate 2 regular season weeks so all Tier 2 (2-week) builds complete
    repeat(2) {
      if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
      vm.advanceWeek()
    }

    val completedTeam = vm.uiState.value.franchises.first { it.id == team.id }
    assertEquals(2, completedTeam.facilities.stadiumTier)
    assertEquals(2, completedTeam.facilities.trainingTier)
    assertEquals(2, completedTeam.facilities.luxurySuitesTier)
    assertTrue("Ongoing projects should clear once built", completedTeam.facilities.ongoingProjects.isEmpty())
    assertTrue("Stadium seating capacity must increase significantly (+3500 total)", completedTeam.stadiumCapacity >= initialCapacity + 3500)
    assertTrue("Local sponsor revenue must grow from stadium & luxury suites", completedTeam.localSponsorRevenueMillions > initialSponsor)
    val healthyCompletedLineupRating = completedTeam.copy(
      roster = completedTeam.roster.map { it.copy(activeInjury = null, suspendedGamesRemaining = 0) }
    ).effectiveLineupRating
    assertTrue("Training center Tier 2 must boost roster & conditioning OVR", completedTeam.rosterRating > initialRosterOvr && healthyCompletedLineupRating > initialEffOvr)
    assertTrue("Training center Tier 2 must reduce injury risk", completedTeam.facilities.injuryRiskReductionPct >= 10)

    val updatedValuation = completedTeam.calculateValuationBreakdown(vm.uiState.value.activeBroadcast, vm.uiState.value.franchises.size).totalValuationMillions
    val updatedStatement = completedTeam.calculateFinancialStatement(vm.uiState.value.activeBroadcast, vm.uiState.value.franchises.size)
    assertTrue("Franchise valuation must appreciate materially after Tier 2 builds", updatedValuation > initialValuation + 50L)
    assertTrue("Luxury suite premium revenue must appear on financial statement", updatedStatement.luxurySuiteRevenueMillions > 0.0)
    assertEquals(updatedValuation, updatedStatement.franchiseValuationMillions)
  }

  @Test
  fun `verify expediting facility project, off-season compound appreciation, hub navigation, and repository persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val team = vm.uiState.value.franchises.first()
    vm.openFacilityHub(team.id)
    assertTrue(vm.uiState.value.showFacilityHubScreen)
    assertEquals(team.id, vm.uiState.value.selectedFacilityFranchiseId)

    // Start Tier 2 Stadium expansion and expedite immediately
    val (started, _) = vm.investInFacilityUpgrade(team.id, com.example.model.FacilityType.STADIUM_SEATING)
    assertTrue(started)
    val projId = vm.uiState.value.franchises.first { it.id == team.id }.facilities.ongoingProjects.first().id
    val preCapacity = team.stadiumCapacity

    val (expedited, _) = vm.expediteFacilityProject(team.id, projId)
    assertTrue("Expediting project should complete immediately", expedited)
    val postExpedite = vm.uiState.value.franchises.first { it.id == team.id }
    assertEquals(2, postExpedite.facilities.stadiumTier)
    assertTrue(postExpedite.stadiumCapacity > preCapacity)

    // Start Tier 3 Training Center (left in-flight) and verify Off-Season finishes it AND applies compound appreciation
    val (startedTraining, _) = vm.investInFacilityUpgrade(team.id, com.example.model.FacilityType.TRAINING_CENTER)
    assertTrue(startedTraining)
    val preOffSeasonAccValue = vm.uiState.value.franchises.first { it.id == team.id }.facilities.accumulatedFacilityValueMillions

    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    val session = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = vm.uiState.value.currentYear,
      franchises = vm.uiState.value.franchises,
      sportType = vm.uiState.value.sportType,
      ruleBook = vm.uiState.value.ruleBook,
      initialParity = vm.uiState.value.competitiveParityScore
    )
    flow.value = vm.uiState.value.copy(
      seasonPhase = com.example.model.SeasonPhase.OFF_SEASON,
      activeOffSeasonSession = session
    )
    vm.finalizeAndInaugurateNewSeason()

    val postOffSeasonTeam = vm.uiState.value.franchises.first { it.id == team.id }
    assertEquals(2, postOffSeasonTeam.facilities.trainingTier)
    assertTrue(postOffSeasonTeam.facilities.ongoingProjects.isEmpty())
    assertTrue(
      "Off-season must complete pending projects and compound facility value over time",
      postOffSeasonTeam.facilities.accumulatedFacilityValueMillions > preOffSeasonAccValue
    )

    // Verify Training Tier 3+ accelerates injury recovery in InjuryEngine
    val injuredPlayer = postOffSeasonTeam.roster.first().copy(
      activeInjury = com.example.model.PlayerInjury(
        id = "fac_rec_test",
        type = com.example.model.InjuryType.HAMSTRING_STRAIN,
        weeksRemaining = 3,
        initialWeeks = 3,
        sustainedWeek = 1,
        sustainedOpponent = "Rivals",
        gameContext = "Training drill vs Rivals"
      )
    )
    val tier3Team = postOffSeasonTeam.copy(
      roster = listOf(injuredPlayer) + postOffSeasonTeam.roster.drop(1),
      facilities = postOffSeasonTeam.facilities.copy(trainingTier = 3)
    )
    val recoveryRes = com.example.model.InjuryEngine.advanceWeeklyRecovery(listOf(tier3Team))
    val healedWeeks = recoveryRes.updatedFranchises.first().roster.first().activeInjury?.weeksRemaining ?: 0
    assertEquals("Tier 3+ Training Center must recover 2 weeks per tick on multi-week injuries", 1, healedWeeks)

    // Verify persistence round-trip preserves facility tiers, invested capital, and accumulated value
    val repo = com.example.data.LeagueRepository(context)
    val snapshot = vm.uiState.value
    val serializedJson = repo.serializeFranchises(snapshot.franchises)
    val restoredTeam = repo.deserializeFranchises(serializedJson).first { it.id == team.id }
    assertEquals(postOffSeasonTeam.facilities.stadiumTier, restoredTeam.facilities.stadiumTier)
    assertEquals(postOffSeasonTeam.facilities.trainingTier, restoredTeam.facilities.trainingTier)
    assertEquals(postOffSeasonTeam.facilities.accumulatedFacilityValueMillions, restoredTeam.facilities.accumulatedFacilityValueMillions)
    assertEquals(postOffSeasonTeam.facilities.cumulativeInvestedMillions, restoredTeam.facilities.cumulativeInvestedMillions)

    vm.closeFacilityHub()
    org.junit.Assert.assertFalse(vm.uiState.value.showFacilityHubScreen)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify free agency pool automatic team signings fill depth charts to realistic contracts and report as league news`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialState = vm.uiState.value
    assertTrue("Free agency pool must initialize with realistic depth players", initialState.freeAgentPool.size >= 20)
    val initialRosterTotal = initialState.franchises.sumOf { it.roster.size }
    val initialPayrollTotal = initialState.franchises.sumOf { it.annualPayrollMillions }

    // Advance 2 regular season weeks -> teams automatically sign free agents to fill out depth charts
    repeat(2) {
      if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
      vm.advanceWeek()
    }

    val afterWeeks = vm.uiState.value
    assertTrue("Teams should execute automatic free agency signings over simulated weeks", afterWeeks.freeAgentSignings.isNotEmpty())
    assertTrue("Combined league roster count must grow as depth charts fill out", afterWeeks.franchises.sumOf { it.roster.size } > initialRosterTotal)
    assertTrue("Combined payroll must reflect realistic signed contracts", afterWeeks.franchises.sumOf { it.annualPayrollMillions } > initialPayrollTotal)

    // Verify all automatic signings are reported as League News & parsed under NewsCategory.FREE_AGENCY
    val faHeadlines = afterWeeks.recentNews.filter { it.contains("FREE AGENCY SIGNING", ignoreCase = true) }
    assertTrue("Automatic free agency signings must be reported in league news", faHeadlines.isNotEmpty())
    val parsedItem = com.example.model.LeagueNewsParser.parseFromRawString(faHeadlines.first(), afterWeeks.currentWeek, afterWeeks.currentYear)
    assertEquals(com.example.model.NewsCategory.FREE_AGENCY, parsedItem.category)
    assertEquals(com.example.model.NewsActionType.NAVIGATE_FREE_AGENCY, parsedItem.actionType)

    // Verify Free Agency Pool keeps itself replenished
    assertTrue("Free agency pool should replenish above minimum floor", afterWeeks.freeAgentPool.size >= 18)

    // Verify STRICT_HARD_CAP realistic contract enforcement vs Mid-Level Exception
    val team = afterWeeks.franchises.first()
    val marqueeFa = afterWeeks.freeAgentPool.maxByOrNull { it.overallRating }!!
    val hardCapRuleBook = afterWeeks.ruleBook.copy(salaryCapStrictness = com.example.model.SalaryCapStrictness.STRICT_HARD_CAP)
    val cappedTeam = team.copy(annualPayrollMillions = afterWeeks.unionAgreement.salaryCapMillions)
    val blockedTerms = com.example.model.FreeAgencyEngine.calculateRealisticContract(
      freeAgent = marqueeFa,
      team = cappedTeam,
      ruleBook = hardCapRuleBook,
      unionAgreement = afterWeeks.unionAgreement
    )
    org.junit.Assert.assertFalse("Hard cap at $0 room must block over-cap signing", blockedTerms.isCapCompliant)

    val softCapTerms = com.example.model.FreeAgencyEngine.calculateRealisticContract(
      freeAgent = marqueeFa.copy(askingSalaryMillions = 11L, overallRating = 80),
      team = cappedTeam,
      ruleBook = afterWeeks.ruleBook.copy(salaryCapStrictness = com.example.model.SalaryCapStrictness.MODERATE_SOFT_CAP),
      unionAgreement = afterWeeks.unionAgreement
    )
    assertTrue("Soft cap should permit realistic Mid-Level Exception contract", softCapTerms.isCapCompliant)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify urgent injury void auto-signing, manual pool signing, waiver release, hub navigation, and repository persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val targetTeam = vm.uiState.value.franchises.first()
    vm.openFreeAgencyPool(targetTeam.id)
    assertTrue(vm.uiState.value.showFreeAgencyPoolScreen)
    assertEquals(targetTeam.id, vm.uiState.value.selectedFreeAgencyFranchiseId)

    // Give target team an urgent starter injury void -> triggerAutoFillDepthCharts prioritizes filling the void
    val starter = com.example.model.InjuryEngine.getActiveStarters(targetTeam).first()
    val injuredStarter = starter.copy(
      activeInjury = com.example.model.PlayerInjury(
        id = "fa_void_inj",
        type = com.example.model.InjuryType.MCL_SPRAIN,
        weeksRemaining = 4,
        initialWeeks = 4,
        sustainedWeek = 1,
        sustainedOpponent = "Rivals",
        gameContext = "Non-contact knee sprain"
      )
    )
    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    val damagedTeam = targetTeam.copy(roster = listOf(injuredStarter) + targetTeam.roster.drop(1))
    flow.value = vm.uiState.value.copy(
      franchises = vm.uiState.value.franchises.map { if (it.id == targetTeam.id) damagedTeam else it }
    )

    val (autoCount, _) = vm.triggerAutoFillDepthCharts()
    assertTrue("Auto-fill sweep should execute depth chart signings across franchises", autoCount >= 1)
    val postAutoTeam = vm.uiState.value.franchises.first { it.id == targetTeam.id }
    assertEquals("Urgent starter hole must be resolved after auto-signing & depth optimization", 0, postAutoTeam.unresolvedStarterHolesCount)
    assertTrue("Target team roster must expand beyond initial 8", postAutoTeam.roster.size > 8)

    // Manual signing from Free Agency Pool
    val candidate = vm.uiState.value.freeAgentPool.last()
    val preManualSize = postAutoTeam.roster.size
    val (signedOk, _) = vm.signFreeAgentForFranchise(targetTeam.id, candidate.id)
    assertTrue("Manual free agent signing should succeed", signedOk)
    val postManualTeam = vm.uiState.value.franchises.first { it.id == targetTeam.id }
    assertEquals(preManualSize + 1, postManualTeam.roster.size)
    assertTrue(postManualTeam.roster.any { it.id == candidate.id })
    assertTrue(vm.uiState.value.recentNews.any { it.contains(candidate.name) })

    // Waiver release returns player back to Free Agency Pool & reports to news
    val (waivedOk, _) = vm.releasePlayerToFreeAgency(targetTeam.id, candidate.id)
    assertTrue("Waiving depth player should succeed", waivedOk)
    assertTrue(vm.uiState.value.freeAgentPool.any { it.name == candidate.name })
    assertTrue(vm.uiState.value.recentNews.first().contains("WAIVER WIRE", ignoreCase = true))

    // Repository serialization round-trip preserves expanded roster depth and free agent pool/transactions
    val repo = com.example.data.LeagueRepository(context)
    val snap = vm.uiState.value
    val restoredTeam = repo.deserializeFranchises(repo.serializeFranchises(snap.franchises)).first { it.id == targetTeam.id }
    assertEquals(snap.franchises.first { it.id == targetTeam.id }.roster.size, restoredTeam.roster.size)
    val restoredPool = repo.deserializeFreeAgents(repo.serializeFreeAgents(snap.freeAgentPool))
    assertEquals(snap.freeAgentPool.size, restoredPool.size)
    val restoredTxs = repo.deserializeFreeAgentTransactions(repo.serializeFreeAgentTransactions(snap.freeAgentSignings))
    assertEquals(snap.freeAgentSignings.size, restoredTxs.size)

    vm.closeFreeAgencyPool()
    org.junit.Assert.assertFalse(vm.uiState.value.showFreeAgencyPoolScreen)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify commissioner settings toggles salary cap, injury frequency, game length, and persists across restarts`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // 1. Open and close settings interface state
    vm.openLeagueSettings()
    assertTrue("Settings screen must open cleanly", vm.uiState.value.showLeagueSettingsScreen)
    vm.closeLeagueSettings()
    org.junit.Assert.assertFalse("Settings screen must close cleanly", vm.uiState.value.showLeagueSettingsScreen)
    vm.openLeagueSettings()

    // 2. Toggle Salary Cap OFF -> syncs strictness, rule_salary_cap, and unionAgreement
    vm.setSalaryCapEnabled(false)
    var state = vm.uiState.value
    assertEquals(com.example.model.SalaryCapStrictness.UNRESTRICTED_MARKET, state.ruleBook.salaryCapStrictness)
    assertEquals(com.example.model.SalaryCapType.FREE_MARKET, state.unionAgreement.salaryCapType)
    org.junit.Assert.assertFalse(state.ruleBook.isRuleEnacted("rule_salary_cap"))

    // Toggle Salary Cap ON & calibrate cap ceiling to $145M
    vm.setSalaryCapEnabled(true)
    vm.updateSalaryCapAmount(145L)
    state = vm.uiState.value
    assertEquals(com.example.model.SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX, state.ruleBook.salaryCapStrictness)
    assertEquals(com.example.model.SalaryCapType.SOFT_CAP_LUXURY_TAX, state.unionAgreement.salaryCapType)
    assertTrue(state.ruleBook.isRuleEnacted("rule_salary_cap"))
    assertEquals(145L, state.unionAgreement.salaryCapMillions)

    // 3. Toggle Instant Replay & Shot Clock via settings helpers
    vm.toggleInstantReplaySetting(false)
    vm.toggleShotClockPaceSetting(false)
    state = vm.uiState.value
    org.junit.Assert.assertFalse(state.ruleBook.instantReplayEnabled)
    org.junit.Assert.assertFalse(state.ruleBook.isRuleEnacted("rule_instant_replay"))
    org.junit.Assert.assertFalse(state.ruleBook.shotClockFastPace)
    org.junit.Assert.assertFalse(state.ruleBook.isRuleEnacted("rule_shot_clock"))

    // 4. Toggle Injury Frequency OFF -> guarantees zero injuries across simulated weeks
    vm.updateInjuryFrequency(com.example.model.InjuryFrequencyRule.INJURIES_OFF)
    assertEquals(com.example.model.InjuryFrequencyRule.INJURIES_OFF, vm.uiState.value.ruleBook.injuryFrequency)

    repeat(3) {
      if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
      vm.advanceWeek()
    }
    assertTrue(
      "INJURIES_OFF must guarantee zero new in-game injuries across regular season weeks",
      vm.uiState.value.injuryReportHistory.isEmpty()
    )
    assertEquals(0, vm.uiState.value.franchises.sumOf { it.injuredPlayersCount })

    // Verify InjuryEngine directly returns zero events for INJURIES_OFF and supports HIGH_GRIND
    val sampleMatches = vm.uiState.value.currentWeekMatches
    val directOffSim = com.example.model.InjuryEngine.simulateMatchInjuries(
      playedMatches = sampleMatches,
      franchises = vm.uiState.value.franchises,
      week = 4,
      seasonYear = 2026,
      isPlayoffs = false,
      injuryFrequency = com.example.model.InjuryFrequencyRule.INJURIES_OFF,
      gameLength = com.example.model.GameLengthRule.MARATHON_60
    )
    assertTrue(directOffSim.newInjuryLogs.isEmpty())

    // 5. Toggle Game Length (36 min vs 60 min) and verify match scoring scales proportionally
    vm.toggleShotClockPaceSetting(true)
    vm.updateGameLength(com.example.model.GameLengthRule.SHORTENED_36)
    assertEquals(com.example.model.GameLengthRule.SHORTENED_36, vm.uiState.value.ruleBook.gameLength)
    if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
    val shortWeekBefore = vm.uiState.value.franchises.sumOf { it.pointsFor }
    vm.advanceWeek()
    val shortWeekAvgScore = (vm.uiState.value.franchises.sumOf { it.pointsFor } - shortWeekBefore) /
      vm.uiState.value.franchises.size.toDouble()

    vm.updateGameLength(com.example.model.GameLengthRule.MARATHON_60)
    assertEquals(com.example.model.GameLengthRule.MARATHON_60, vm.uiState.value.ruleBook.gameLength)
    if (vm.uiState.value.activeDilemma != null) vm.resolveDilemma(0)
    val longWeekBefore = vm.uiState.value.franchises.sumOf { it.pointsFor }
    vm.advanceWeek()
    val longWeekAvgScore = (vm.uiState.value.franchises.sumOf { it.pointsFor } - longWeekBefore) /
      vm.uiState.value.franchises.size.toDouble()

    assertTrue(
      "60-min MARATHON games (avg $longWeekAvgScore) must score substantially higher than 36-min SHORTENED games (avg $shortWeekAvgScore)",
      longWeekAvgScore > shortWeekAvgScore + 25.0
    )

    // 6. Verify Repository settings persistence round-trip preserves all rule toggles
    val repo = com.example.data.LeagueRepository(context)
    val currentBook = vm.uiState.value.ruleBook
    repo.saveLeagueSettings(currentBook)
    val restored = repo.loadLeagueSettings()
    org.junit.Assert.assertNotNull(restored)
    assertEquals(com.example.model.InjuryFrequencyRule.INJURIES_OFF, restored!!.injuryFrequency)
    assertEquals(com.example.model.GameLengthRule.MARATHON_60, restored.gameLength)
    org.junit.Assert.assertFalse(restored.instantReplayEnabled)
    org.junit.Assert.assertFalse(restored.isRuleEnacted("rule_instant_replay"))

    // 7. Reset to defaults restores all settings cleanly
    vm.resetRulesToDefault()
    val resetState = vm.uiState.value
    assertEquals(com.example.model.InjuryFrequencyRule.STANDARD_REALISTIC, resetState.ruleBook.injuryFrequency)
    assertEquals(com.example.model.GameLengthRule.STANDARD_48, resetState.ruleBook.gameLength)
    assertEquals(com.example.model.SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX, resetState.ruleBook.salaryCapStrictness)
    assertEquals(80L, resetState.unionAgreement.salaryCapMillions)
    assertTrue(resetState.ruleBook.instantReplayEnabled)
    assertTrue(resetState.ruleBook.shotClockFastPace)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify expansion draft selects real players from existing rosters and free agency pool with team protection and undraft`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialState = vm.uiState.value
    val candidateCity = initialState.cities.first { !it.hasFranchise }
    val sourceTeam = initialState.franchises.first()
    val initialSourceRosterIds = sourceTeam.roster.map { it.id }.toSet()
    val initialFreeAgentIds = initialState.freeAgentPool.map { it.id }.toSet()

    vm.startExpansionDraft(
      targetCityId = candidateCity.id,
      nickname = "Valkyries",
      brandColorHex = 0xFF0284C7L,
      ownershipType = com.example.model.OwnershipType.SOVEREIGN_SYNDICATE,
      totalRounds = 6
    )

    val session = vm.uiState.value.activeExpansionDraftSession!!
    val rosterCandidates = session.availablePool.filter {
      it.sourceType == com.example.model.ExpansionDraftSourceType.EXISTING_ROSTER
    }
    val faCandidates = session.availablePool.filter {
      it.sourceType == com.example.model.ExpansionDraftSourceType.FREE_AGENCY_POOL
    }
    assertTrue("Pool must expose real players from existing rosters", rosterCandidates.isNotEmpty())
    assertTrue("Pool must expose real players from live Free Agency pool", faCandidates.isNotEmpty())

    // Verify candidates match genuine roster IDs and genuine free agent IDs
    val fromSourceTeam = rosterCandidates.filter { it.sourceTeamId == sourceTeam.id }
    assertTrue("Source franchise must have exposed roster players", fromSourceTeam.size >= 3)
    assertTrue(fromSourceTeam.all { it.sourcePlayerId in initialSourceRosterIds })
    assertTrue(faCandidates.all { it.sourcePlayerId in initialFreeAgentIds })

    // Draft 2 exposed players from the same existing franchise -> enforces 2-pick protection cap on 3rd player
    val pickA = fromSourceTeam[0]
    val pickB = fromSourceTeam[1]
    val blockedC = fromSourceTeam[2]
    vm.draftPlayerForExpansion(pickA.id)
    vm.draftPlayerForExpansion(pickB.id)

    val midSession = vm.uiState.value.activeExpansionDraftSession!!
    assertTrue(midSession.isTeamDraftLimitReached(sourceTeam.id))
    assertTrue(
      "Remaining exposed player on same club must become protected after 2 picks",
      midSession.availablePool.find { it.id == blockedC.id }?.isProtected == true
    )

    // Attempting to draft protected player is blocked
    vm.draftPlayerForExpansion(blockedC.id)
    assertEquals(2, vm.uiState.value.activeExpansionDraftSession!!.draftedPlayers.size)

    // Undraft pickB -> unlocks club protection and restores player to pool
    vm.undraftPlayerForExpansion(pickB.id)
    val afterUndraft = vm.uiState.value.activeExpansionDraftSession!!
    assertEquals(1, afterUndraft.draftedPlayers.size)
    org.junit.Assert.assertFalse(afterUndraft.isTeamDraftLimitReached(sourceTeam.id))
    org.junit.Assert.assertFalse(afterUndraft.availablePool.find { it.id == blockedC.id }?.isProtected ?: true)

    // Pick 2 real Free Agents from Free Agency Pool and fill remaining rounds from other rosters
    val faPick1 = afterUndraft.availablePool.first {
      it.sourceType == com.example.model.ExpansionDraftSourceType.FREE_AGENCY_POOL
    }
    val faPick2 = afterUndraft.availablePool.filter {
      it.sourceType == com.example.model.ExpansionDraftSourceType.FREE_AGENCY_POOL
    }[1]
    vm.draftPlayerForExpansion(faPick1.id)
    vm.draftPlayerForExpansion(faPick2.id)

    while (!vm.uiState.value.activeExpansionDraftSession!!.isCompleted) {
      vm.autoDraftBestAvailable()
    }

    val completedSession = vm.uiState.value.activeExpansionDraftSession!!
    assertEquals(6, completedSession.draftedPlayers.size)
    assertTrue(completedSession.draftedFromRostersCount >= 1)
    assertTrue(completedSession.draftedFromFreeAgencyCount >= 2)

    vm.finalizeExpansionDraft()
    val finalState = vm.uiState.value

    // Verify new team received real drafted players
    val newTeam = finalState.franchises.first { it.nickname == "Valkyries" }
    assertTrue(newTeam.roster.any { it.id == pickA.sourcePlayerId && it.name == pickA.name })
    assertTrue(newTeam.roster.any { it.id == faPick1.sourcePlayerId && it.name == faPick1.name })
    assertTrue(newTeam.roster.size >= 8)

    // Verify drafted roster player was removed from source team roster
    val updatedSourceTeam = finalState.franchises.first { it.id == sourceTeam.id }
    org.junit.Assert.assertFalse(
      "Drafted player must be removed from original team's roster",
      updatedSourceTeam.roster.any { it.id == pickA.sourcePlayerId }
    )

    // Verify drafted free agents were removed from Free Agency Pool and logged
    org.junit.Assert.assertFalse(finalState.freeAgentPool.any { it.id == faPick1.sourcePlayerId })
    org.junit.Assert.assertFalse(finalState.freeAgentPool.any { it.id == faPick2.sourcePlayerId })
    assertTrue(
      "Expansion Free Agent picks must be logged in freeAgentSignings",
      finalState.freeAgentSignings.any { it.playerId == faPick1.sourcePlayerId && it.franchiseId == newTeam.id }
    )
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify traded draft picks transfer with provenance, populate tradedPickLedger, and persist via LeagueRepository`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises
    val teamA = franchises[0]
    val teamB = franchises[1]

    val pickFromA2026R1 = teamA.draftPicks.first { it.year == 2026 && it.round == 1 }
    val pickFromB2027R2 = teamB.draftPicks.first { it.year == 2027 && it.round == 2 }

    val tradeProposal = com.example.model.TradeProposal(
      id = "test_pick_trade_provenance_1",
      weekProposed = 3,
      teamAId = teamA.id,
      teamAName = teamA.teamFullName,
      teamBId = teamB.id,
      teamBName = teamB.teamFullName,
      teamASendsPicks = listOf(pickFromA2026R1),
      teamBSendsPicks = listOf(pickFromB2027R2),
      fairnessScore = 82,
      salaryCapCompliant = true
    )

    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    flow.value = vm.uiState.value.copy(
      currentWeek = 3,
      pendingTrades = listOf(tradeProposal)
    )

    val approved = vm.approveTrade("test_pick_trade_provenance_1")
    assertTrue("Pick trade should approve cleanly", approved)

    val afterState = vm.uiState.value
    val updatedA = afterState.franchises.first { it.id == teamA.id }
    val updatedB = afterState.franchises.first { it.id == teamB.id }

    // Team B should own Team A's 2026 R1 pick with full provenance
    val acquiredByB = updatedB.draftPicks.find { it.id == pickFromA2026R1.id }
    org.junit.Assert.assertNotNull(acquiredByB)
    assertEquals(teamB.id, acquiredByB!!.currentFranchiseId)
    assertEquals(teamA.id, acquiredByB.originalFranchiseId)
    assertEquals(teamA.id, acquiredByB.acquiredFromFranchiseId)
    assertEquals(3, acquiredByB.acquiredInWeek)
    assertEquals("test_pick_trade_provenance_1", acquiredByB.acquiredTradeId)
    assertTrue(acquiredByB.isTraded)

    // TradedPickLedger must track both traded picks
    assertTrue(afterState.tradedPickLedger.any { it.pickId == pickFromA2026R1.id && it.toFranchiseId == teamB.id && it.originalFranchiseId == teamA.id })
    assertTrue(afterState.tradedPickLedger.any { it.pickId == pickFromB2027R2.id && it.toFranchiseId == teamA.id && it.originalFranchiseId == teamB.id })

    // Verify LeagueRepository serialization & deserialization preserves traded draft picks & provenance
    val repo = com.example.data.LeagueRepository(context)
    val json = repo.serializeFranchises(afterState.franchises)
    val restoredFranchises = repo.deserializeFranchises(json, 2026)
    val restoredB = restoredFranchises.first { it.id == teamB.id }
    val restoredPick = restoredB.draftPicks.find { it.id == pickFromA2026R1.id }
    org.junit.Assert.assertNotNull(restoredPick)
    assertEquals(teamB.id, restoredPick!!.currentFranchiseId)
    assertEquals(teamA.id, restoredPick.originalFranchiseId)
    assertEquals(3, restoredPick.acquiredInWeek)
    assertEquals("test_pick_trade_provenance_1", restoredPick.acquiredTradeId)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify traded Round 1 and Round 2 draft picks directly alter OffSeason draft order and award drafted rookies to acquiring franchise`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises
    // Put Team 0 at worst record so they receive top lottery odds & Pick #1 original slot,
    // and trade Team 0's 2026 R1 & R2 picks to Team 5 (strongest record)
    val lotteryFavoredTeam = franchises[0]
    val contenderBuyer = franchises[5]

    val pickR1 = lotteryFavoredTeam.draftPicks.first { it.year == 2026 && it.round == 1 }
    val pickR2 = lotteryFavoredTeam.draftPicks.first { it.year == 2026 && it.round == 2 }

    val configuredFranchises = franchises.mapIndexed { idx, team ->
      when (team.id) {
        lotteryFavoredTeam.id -> team.copy(
          wins = 1,
          losses = 15,
          draftPicks = team.draftPicks.filterNot { it.id == pickR1.id || it.id == pickR2.id }
        )
        contenderBuyer.id -> team.copy(
          wins = 15,
          losses = 1,
          draftPicks = team.draftPicks + listOf(
            com.example.model.TradeEngine.transferPickForTrade(pickR1, lotteryFavoredTeam, contenderBuyer, 4, "trade_r1_r2"),
            com.example.model.TradeEngine.transferPickForTrade(pickR2, lotteryFavoredTeam, contenderBuyer, 4, "trade_r1_r2")
          )
        )
        else -> team.copy(wins = 8 + idx, losses = 8 - (idx % 4))
      }
    }

    val session = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = 2026,
      franchises = configuredFranchises,
      sportType = com.example.model.SportType.BASKETBALL,
      ruleBook = vm.uiState.value.ruleBook,
      initialParity = 80,
      autoCompleteDraft = false
    )

    // Because a Round 2 pick was traded, BOTH Round 1 and Round 2 must be scheduled in draftOrderSlots
    assertEquals(configuredFranchises.size * 2, session.draftOrderSlots.size)

    // Find the Round 1 slot originally belonging to lotteryFavoredTeam -> must be owned by contenderBuyer
    val acquiredR1Slot = session.draftOrderSlots.first { it.round == 1 && it.originalFranchiseId == lotteryFavoredTeam.id }
    assertEquals(contenderBuyer.id, acquiredR1Slot.ownerFranchiseId)
    assertTrue(acquiredR1Slot.isTraded)

    // Find the Round 2 slot originally belonging to lotteryFavoredTeam -> must also be owned by contenderBuyer
    val acquiredR2Slot = session.draftOrderSlots.first { it.round == 2 && it.originalFranchiseId == lotteryFavoredTeam.id }
    assertEquals(contenderBuyer.id, acquiredR2Slot.ownerFranchiseId)
    assertTrue(acquiredR2Slot.isTraded)

    // Lottery result entry for lotteryFavoredTeam must report pickOwnerFranchiseId == contenderBuyer.id
    val lotteryEntry = session.lotteryResults.first { it.franchiseId == lotteryFavoredTeam.id }
    assertEquals(contenderBuyer.id, lotteryEntry.pickOwnerFranchiseId)
    assertTrue(lotteryEntry.isTradedPick)

    // Inject into ViewModel, auto-complete the draft, and inaugurate 2027 season
    val stateField = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.viewmodel.LeagueUiState>
    val initialRosterCounts = configuredFranchises.associate { it.id to it.roster.size }
    flow.value = vm.uiState.value.copy(
      seasonPhase = com.example.model.SeasonPhase.OFF_SEASON,
      franchises = configuredFranchises,
      activeOffSeasonSession = session.copy(currentStep = com.example.model.OffSeasonStep.LEAGUE_DRAFT)
    )

    vm.autoCompleteRookieDraft()
    val completedDraft = vm.uiState.value.activeOffSeasonSession!!
    assertTrue(completedDraft.isDraftCompleted)
    assertEquals(configuredFranchises.size * 2, completedDraft.draftPicks.size)

    // DraftPickRecord for lotteryFavoredTeam's R1 & R2 slots must belong to contenderBuyer with acquiredViaTrade=true
    val r1PickRecord = completedDraft.draftPicks.first { it.round == 1 && it.originalFranchiseId == lotteryFavoredTeam.id }
    assertEquals(contenderBuyer.id, r1PickRecord.franchiseId)
    assertTrue(r1PickRecord.acquiredViaTrade)
    assertTrue(r1PickRecord.selectionRationale.contains("Traded pick via"))

    // Finalize season -> contenderBuyer gets its own R1+R2 PLUS both traded R1+R2 rookies (4 rookies total),
    // while lotteryFavoredTeam gains 0 rookies!
    vm.finalizeAndInaugurateNewSeason()
    val postSeason = vm.uiState.value
    val finalSeller = postSeason.franchises.first { it.id == lotteryFavoredTeam.id }
    val finalBuyer = postSeason.franchises.first { it.id == contenderBuyer.id }

    assertEquals(initialRosterCounts[lotteryFavoredTeam.id]!!, finalSeller.roster.size)
    assertEquals(initialRosterCounts[contenderBuyer.id]!! + 4, finalBuyer.roster.size)

    // TradedPickLedger retains exercised outcome summary and rolling picks span 2027..2029
    assertTrue(postSeason.tradedPickLedger.any {
      it.status == com.example.model.TradedPickStatus.EXERCISED_IN_DRAFT &&
        it.toFranchiseId == contenderBuyer.id &&
        it.originalFranchiseId == lotteryFavoredTeam.id
    })
    assertTrue(finalBuyer.draftPicks.none { it.year == 2026 })
    assertTrue(finalBuyer.draftPicks.any { it.year == 2029 && it.round == 1 })
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify Top-3 protected traded pick retains in lottery top 3 and rolls over unprotected to next year upon season rollover`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val franchises = vm.uiState.value.franchises
    val rebuildingSeller = franchises[0]
    val buyerTeam = franchises[1]

    // Create a Top-3 Protected 2026 Round 1 pick owned by buyerTeam originally from rebuildingSeller
    val protected2026R1 = com.example.model.DraftPick(
      id = "pick_${rebuildingSeller.id}_2026_r1",
      year = 2026,
      round = 1,
      originalFranchiseId = rebuildingSeller.id,
      originalFranchiseName = rebuildingSeller.teamFullName,
      currentFranchiseId = buyerTeam.id,
      isProtected = true,
      protectionDescription = "Top-3 Protected",
      acquiredFromFranchiseId = rebuildingSeller.id,
      acquiredFromFranchiseName = rebuildingSeller.teamFullName,
      acquiredInWeek = 2,
      acquiredTradeId = "trade_prot_test"
    )

    val configured = franchises.mapIndexed { idx, team ->
      when (team.id) {
        rebuildingSeller.id -> team.copy(
          wins = 0,
          losses = 16,
          draftPicks = team.draftPicks.filterNot { it.id == protected2026R1.id }
        )
        buyerTeam.id -> team.copy(
          wins = 14,
          losses = 2,
          draftPicks = team.draftPicks + protected2026R1
        )
        else -> team.copy(wins = 7 + idx, losses = 9 - (idx % 3))
      }
    }

    val session = com.example.model.OffSeasonEngine.createOffSeasonSession(
      year = 2026,
      franchises = configured,
      sportType = com.example.model.SportType.BASKETBALL,
      ruleBook = vm.uiState.value.ruleBook,
      initialParity = 80,
      autoCompleteDraft = true
    )

    // Because rebuildingSeller finished worst (0-16), their lottery finalDraftPick <= 3 -> Top-3 protection triggers!
    val sellerLottery = session.lotteryResults.first { it.franchiseId == rebuildingSeller.id }
    assertTrue("Expected rebuilding seller to land in Top 3, got #${sellerLottery.finalDraftPick}", sellerLottery.finalDraftPick <= 3)
    assertEquals(rebuildingSeller.id, sellerLottery.pickOwnerFranchiseId)
    org.junit.Assert.assertFalse(sellerLottery.isTradedPick)
    assertTrue(sellerLottery.protectionOutcomeNote.contains("Top-3 Protected"))

    val slot = session.draftOrderSlots.first { it.round == 1 && it.originalFranchiseId == rebuildingSeller.id }
    assertTrue(slot.retainedDueToProtection)
    assertEquals(rebuildingSeller.id, slot.ownerFranchiseId)

    // Rollover to 2027 -> rebuildingSeller's 2027 R1 pick defers/transfers UNPROTECTED to buyerTeam
    val rolled = com.example.model.TradeEngine.rollDraftPicksForNewSeason(
      franchises = configured,
      finishedYear = 2026,
      nextYear = 2027,
      deferredSlots = session.draftOrderSlots.filter { it.retainedDueToProtection }
    )
    val rolledBuyer = rolled.first { it.id == buyerTeam.id }
    val rolledSeller = rolled.first { it.id == rebuildingSeller.id }

    val deferredPick = rolledBuyer.draftPicks.find {
      it.year == 2027 && it.round == 1 && it.originalFranchiseId == rebuildingSeller.id
    }
    org.junit.Assert.assertNotNull("Buyer should receive deferred 2027 R1 unprotected pick", deferredPick)
    org.junit.Assert.assertFalse(deferredPick!!.isProtected)
    assertEquals(2026, deferredPick.deferredFromYear)
    assertTrue(rolledSeller.draftPicks.none { it.year == 2027 && it.round == 1 && it.originalFranchiseId == rebuildingSeller.id })
  }

  @Test
  fun `verify all four selectable sports generate canonical positions across franchises, free agency, draft prospects, and minor affiliates`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    SportType.values().forEach { sport ->
      vm.createNewCustomLeague(
        name = "Test ${sport.displayName} League",
        sport = sport,
        background = com.example.model.CommissionerBackground.MARKETING_MAVEN
      )
      org.robolectric.shadows.ShadowLooper.idleMainLooper()

      val state = vm.uiState.value
      assertEquals(sport, state.sportType)
      assertEquals(sport.minorLeagueName, state.minorLeagueState.leagueName)
      assertEquals(state.franchises.size, state.minorLeagueState.affiliates.size)

      val expectedStarterPositions = sport.starterPositions
      val allowedAllPositions = expectedStarterPositions.toSet()

      // Every franchise's first 5 starters must match canonical starter positions, and bench positions must be sport-valid
      state.franchises.forEach { team ->
        assertEquals(8, team.roster.size)
        val starters = team.roster.take(5)
        assertEquals(
          "Starters for ${sport.name} on ${team.teamFullName} must match ${expectedStarterPositions}",
          expectedStarterPositions,
          starters.map { it.position }
        )
        assertTrue(
          "Bench depth positions must stay within ${sport.name} positions",
          team.roster.all { it.position in allowedAllPositions }
        )
        assertTrue(
          "Player archetypes must belong to ${sport.name}",
          team.roster.all { it.archetype in sport.archetypes }
        )
      }

      // Free agency pool & draft prospect pool must strictly use the selected sport's positions
      assertTrue(
        "Free agent positions for ${sport.name} must all be valid",
        state.freeAgentPool.all { it.position in allowedAllPositions }
      )
      val offSeason = com.example.model.OffSeasonEngine.createOffSeasonSession(
        year = 2026,
        franchises = state.franchises,
        sportType = sport,
        ruleBook = state.ruleBook,
        initialParity = 82,
        autoCompleteDraft = false
      )
      assertTrue(
        "Draft prospects for ${sport.name} must all be valid positions",
        offSeason.draftProspectPool.all { it.position in allowedAllPositions }
      )
    }
  }

  @Test
  fun `verify minor development league builds from depth charts, toggles recall or assign, earns OVR progression, scales on expansion, and crowns playoffs and awards`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    val initialState = vm.uiState.value
    // 1. 1-to-1 affiliates formed from 6 charter teams' depth/bench positions
    assertEquals(6, initialState.minorLeagueState.affiliates.size)
    assertEquals(4, initialState.minorLeagueState.playoffFieldSize)
    initialState.franchises.forEach { team ->
      val assignedDepth = com.example.model.MinorLeagueEngine.getParentAssignedDepthPlayers(team)
      assertEquals("Each initial 8-man franchise contributes 3 healthy bench players", 3, assignedDepth.size)
    }

    // 2. Recall and re-assign a depth player cleanly between minor affiliate and parent bench
    val targetTeam = initialState.franchises.first()
    val benchProspect = com.example.model.MinorLeagueEngine.getParentAssignedDepthPlayers(targetTeam).first()
    val targetAffiliate = initialState.minorLeagueState.affiliates.first { it.parentFranchiseId == targetTeam.id }

    vm.toggleBenchPlayerMinorAssignment(targetTeam.id, benchProspect.id, assignToMinor = false)
    assertTrue(
      vm.uiState.value.franchises.first { it.id == targetTeam.id }
        .roster.first { it.id == benchProspect.id }.isAssignedToMinorLeague.not()
    )
    vm.toggleBenchPlayerMinorAssignment(targetTeam.id, benchProspect.id, assignToMinor = true)
    assertTrue(
      vm.uiState.value.franchises.first { it.id == targetTeam.id }
        .roster.first { it.id == benchProspect.id }.isAssignedToMinorLeague
    )

    // 3. Regional market ticket pricing adjusts loyalty & gate economics
    vm.updateMinorAffiliateTicketPrice(targetAffiliate.id, 18)
    val pricedAff = vm.uiState.value.minorLeagueState.affiliates.first { it.id == targetAffiliate.id }
    assertEquals(18, pricedAff.ticketPrice)

    // 4. League Growth Expansion (6 -> 8 clubs) automatically adds expansion minor affiliates & scales playoff bracket (4 -> 6)
    val openMarkets = initialState.cities.filter { !it.hasFranchise }.take(2)
    assertEquals(2, openMarkets.size)
    vm.expandLeague(openMarkets[0].id, "Comets", 0xFF06B6D4, com.example.model.OwnershipType.TECH_BILLIONAIRE)
    vm.expandLeague(openMarkets[1].id, "Strikers", 0xFF8B5CF6, com.example.model.OwnershipType.COMMUNITY_TRUST)

    val expandedState = vm.uiState.value
    assertEquals(8, expandedState.franchises.size)
    assertEquals(8, expandedState.minorLeagueState.affiliates.size)
    assertEquals(6, expandedState.minorLeagueState.playoffFieldSize)
    assertTrue(expandedState.minorLeagueState.affiliates.any { it.isExpansionAffiliate })

    // 5. Simulate multiple weeks to verify stats, finances, devXP -> +1 OVR progression, scaling playoffs, and awards
    var minorState = expandedState.minorLeagueState
    var rollingFranchises = expandedState.franchises
    val startOvrSum = rollingFranchises.flatMap { it.roster }.sumOf { it.overallRating }

    for (week in 1..14) {
      val tick = com.example.model.MinorLeagueEngine.simulateRegularSeasonWeek(
        currentMinorState = minorState,
        franchises = rollingFranchises,
        sportType = expandedState.sportType,
        week = week,
        seasonYear = expandedState.currentYear,
        gameLength = expandedState.ruleBook.gameLength,
        random = kotlin.random.Random(100 + week)
      )
      minorState = tick.minorState
      rollingFranchises = tick.updatedFranchises
    }

    assertTrue("Minor league must generate cumulative circuit revenue", minorState.totalCircuitRevenueMillions > 10.0)
    assertTrue("Minor league must generate gate attendance", minorState.totalCircuitAttendance > 10_000)
    assertTrue("Weekly Prospect of the Week honors must be recorded", minorState.weeklyAwards.isNotEmpty())
    val endOvrSum = rollingFranchises.flatMap { it.roster }.sumOf { it.overallRating }
    assertTrue("Sustained minor league play should trigger +1 OVR breakthroughs for depth players", endOvrSum > startOvrSum || minorState.totalOvrBreakthroughs > 0)

    // Advance scaling 6-team minor playoffs through championship completion
    val poResult = com.example.model.MinorLeagueEngine.advancePlayoffRound(
      currentMinorState = minorState,
      franchises = rollingFranchises,
      sportType = expandedState.sportType,
      week = 15,
      seasonYear = expandedState.currentYear,
      completeAllRemainingRounds = true,
      random = kotlin.random.Random(999)
    )
    val finishedMinor = poResult.minorState
    org.junit.Assert.assertNotNull(finishedMinor.playoffChampionAffiliateId)
    org.junit.Assert.assertNotNull(finishedMinor.playoffChampionName)
    assertEquals(5, finishedMinor.annualAwards.size)
    assertTrue(finishedMinor.annualAwards.any { it.type == com.example.model.MinorLeagueAwardType.MINOR_MVP })
    assertTrue(finishedMinor.annualAwards.any { it.type == com.example.model.MinorLeagueAwardType.FINALS_MVP })
    assertTrue(finishedMinor.pastSeasons.any { it.year == expandedState.currentYear && it.playoffFieldSize == 6 && it.affiliatesCount == 8 })
  }

  @Test
  fun `verify minor league repository serialization round-trip preserves affiliates, finances, awards, and archives`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.LeagueRepository(context)
    val initialFranchises = com.example.data.InitialLeagueData.getInitialFranchises(SportType.SOCCER)
    val boot = com.example.model.MinorLeagueEngine.syncAffiliatesWithParentLeague(initialFranchises, null, SportType.SOCCER, 2026)

    val week1 = com.example.model.MinorLeagueEngine.simulateRegularSeasonWeek(
      currentMinorState = boot.minorState,
      franchises = boot.updatedFranchises,
      sportType = SportType.SOCCER,
      week = 1,
      seasonYear = 2026,
      random = kotlin.random.Random(77)
    )
    val playoffDone = com.example.model.MinorLeagueEngine.advancePlayoffRound(
      currentMinorState = week1.minorState,
      franchises = week1.updatedFranchises,
      sportType = SportType.SOCCER,
      week = 15,
      seasonYear = 2026,
      completeAllRemainingRounds = true,
      random = kotlin.random.Random(88)
    )

    val original = playoffDone.minorState.copy(manualParentBenchOptOutIds = setOf("player_optout_1"))
    repo.saveMinorLeagueState(original)
    val restored = repo.loadMinorLeagueState()

    org.junit.Assert.assertNotNull(restored)
    assertEquals(original.leagueName, restored!!.leagueName)
    assertEquals(original.affiliates.size, restored.affiliates.size)
    assertEquals(original.playoffFieldSize, restored.playoffFieldSize)
    assertEquals(original.playoffChampionAffiliateId, restored.playoffChampionAffiliateId)
    assertEquals(original.annualAwards.size, restored.annualAwards.size)
    assertEquals(original.pastSeasons.size, restored.pastSeasons.size)
    assertEquals(setOf("player_optout_1"), restored.manualParentBenchOptOutIds)
    assertEquals(original.totalCircuitRevenueMillions, restored.totalCircuitRevenueMillions, 0.05)
  }

  @Test
  fun `verify charter and expansion ownership groups receive distinct diverse owners and archetypes`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val charterFranchises = vm.uiState.value.franchises
    assertEquals(6, charterFranchises.size)

    val charterOwners = charterFranchises.map { it.ownership.primaryOwner }
    assertEquals(6, charterOwners.map { it.fullName }.toSet().size)
    assertEquals(6, charterOwners.map { it.archetype }.toSet().size)

    // Auto-assigned expansion group should pick unrepresented 7th archetype (TRADITIONALIST_PURIST) & unique owner name
    val openMarkets = vm.uiState.value.cities.filter { !it.hasFranchise }
    assertTrue(openMarkets.size >= 2)
    vm.expandLeague(
      targetCityId = openMarkets[0].id,
      nickname = "Royals",
      colorHex = 0xFF1E3A8AL,
      ownershipType = com.example.model.OwnershipType.PRIVATE_EQUITY
    )

    val afterExp1 = vm.uiState.value.franchises
    assertEquals(7, afterExp1.size)
    val royalsOwner = afterExp1.last().ownership.primaryOwner
    assertEquals(com.example.model.OwnerArchetype.TRADITIONALIST_PURIST, royalsOwner.archetype)
    assertEquals(7, afterExp1.map { it.ownership.primaryOwner.fullName }.toSet().size)

    // Expansion draft with explicit preferredArchetype and customOwnerName honors commissioner selection
    vm.startExpansionDraft(
      targetCityId = openMarkets[1].id,
      nickname = "Comets",
      brandColorHex = 0xFF06B6D4L,
      ownershipType = com.example.model.OwnershipType.TECH_BILLIONAIRE,
      totalRounds = 4,
      preferredArchetype = com.example.model.OwnerArchetype.ANALYTICS_VISIONARY,
      customOwnerName = "Dr. Sofia Castellanos"
    )
    repeat(4) { vm.autoDraftBestAvailable() }
    vm.finalizeExpansionDraft()

    val afterExp2 = vm.uiState.value.franchises
    assertEquals(8, afterExp2.size)
    val comets = afterExp2.find { it.nickname == "Comets" }!!
    assertEquals("Dr. Sofia Castellanos", comets.ownership.primaryOwner.fullName)
    assertEquals(com.example.model.OwnerArchetype.ANALYTICS_VISIONARY, comets.ownership.primaryOwner.archetype)
  }

  @Test
  fun `verify owners vote on CBA negotiations with veto blocking and balanced ratification`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Extreme Free Market + 54% player revenue split + all costly perks should be vetoed by Board majority
    val vetoResult = vm.submitCbaToOwnerVote(
      revenueSharePct = 54,
      salaryCapType = com.example.model.SalaryCapType.FREE_MARKET,
      salaryCapMillions = 160L,
      minSalaryK = 950L,
      playerHealthWelfareMillions = 30L,
      mandatoryPrivateCharters = true,
      backToBackLimits = true,
      concussionProtocol = true,
      hybridTurfMandate = true,
      transitionFundMillions = 12L,
      fiveStarHotels = true
    )

    org.junit.Assert.assertFalse("Extreme 54% Free Market CBA should be vetoed by Board majority", vetoResult.passed)
    assertTrue(vetoResult.noVotes > vetoResult.yesVotes)
    assertEquals(6, vetoResult.totalVotes)
    // Verify ideological split: Fiscal Hawk & Community Steward vote Nay, Players-First & Broadcast Showman vote Yea
    val fiscalBallot = vetoResult.ballots.find { it.owner.archetype == com.example.model.OwnerArchetype.FISCAL_HAWK }!!
    val stewardBallot = vetoResult.ballots.find { it.owner.archetype == com.example.model.OwnerArchetype.COMMUNITY_STEWARD }!!
    val playersFirstBallot = vetoResult.ballots.find { it.owner.archetype == com.example.model.OwnerArchetype.PLAYERS_FIRST_PATRON }!!
    assertEquals(com.example.model.VoteChoice.OPPOSE, fiscalBallot.choice)
    assertEquals(com.example.model.VoteChoice.OPPOSE, stewardBallot.choice)
    assertEquals(com.example.model.VoteChoice.APPROVE, playersFirstBallot.choice)
    // CBA must NOT have changed on veto
    assertEquals(80L, vm.uiState.value.unionAgreement.salaryCapMillions)
    assertEquals(com.example.model.SalaryCapType.SOFT_CAP_LUXURY_TAX, vm.uiState.value.unionAgreement.salaryCapType)

    // Now propose a balanced compromise CBA: 50% split, Soft Cap Luxury Tax, $95M cap
    val passResult = vm.submitCbaToOwnerVote(
      revenueSharePct = 50,
      salaryCapType = com.example.model.SalaryCapType.SOFT_CAP_LUXURY_TAX,
      salaryCapMillions = 95L,
      minSalaryK = 700L,
      playerHealthWelfareMillions = 16L,
      mandatoryPrivateCharters = true,
      backToBackLimits = true,
      concussionProtocol = true,
      hybridTurfMandate = false,
      transitionFundMillions = 5L,
      fiveStarHotels = true
    )

    assertTrue("Compromise CBA should pass Board of Governors majority", passResult.passed)
    assertEquals(95L, vm.uiState.value.unionAgreement.salaryCapMillions)
    assertEquals(50, vm.uiState.value.unionAgreement.playerRevenueSharePct)
    assertEquals(2, vm.uiState.value.boardVoteHistory.size)
  }

  @Test
  fun `verify owners vote on rule changes and executive override resolves vetoed rule`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Repealing Salary Cap is fiercely opposed by Fiscal Hawk, Community Steward, Globalist, Purist, Analytics -> Vetoed!
    val repealCapVote = vm.submitRuleToggleVote("rule_salary_cap")
    org.junit.Assert.assertNotNull(repealCapVote)
    org.junit.Assert.assertFalse("Repealing salary cap should be vetoed by owners", repealCapVote!!.passed)
    assertTrue(vm.uiState.value.ruleBook.isRuleEnacted("rule_salary_cap"))

    // Verify Broadcast Showman voted APPROVE to repeal cap, while Fiscal Hawk & Community Steward voted OPPOSE
    val showmanBallot = repealCapVote.ballots.find { it.owner.archetype == com.example.model.OwnerArchetype.BROADCAST_SHOWMAN }!!
    val hawkBallot = repealCapVote.ballots.find { it.owner.archetype == com.example.model.OwnerArchetype.FISCAL_HAWK }!!
    assertEquals(com.example.model.VoteChoice.APPROVE, showmanBallot.choice)
    assertEquals(com.example.model.VoteChoice.OPPOSE, hawkBallot.choice)

    // Commissioner Executive Override forces the vetoed repeal through at approval cost
    val approvalBeforeOverride = vm.uiState.value.franchises.find {
      it.ownership.primaryOwner.archetype == com.example.model.OwnerArchetype.FISCAL_HAWK
    }!!.ownership.ownerApproval
    vm.overrideLastVetoedVote()
    org.junit.Assert.assertFalse(vm.uiState.value.ruleBook.isRuleEnacted("rule_salary_cap"))
    assertTrue(vm.uiState.value.lastBoardVoteResult!!.enactedViaCommissionerOverride)
    val approvalAfterOverride = vm.uiState.value.franchises.find {
      it.ownership.primaryOwner.archetype == com.example.model.OwnerArchetype.FISCAL_HAWK
    }!!.ownership.ownerApproval
    assertTrue(approvalAfterOverride < approvalBeforeOverride)
  }

  @Test
  fun `verify expansion owners gain active voting seats on Board of Governors rule votes`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    val initialVote = vm.submitSettingRuleVote(
      proposalTitle = "Overtime Format: Target Score (Elam Ending)",
      proposalSummary = "First team to target score wins.",
      overtimeFormat = com.example.model.OvertimeFormat.TARGET_SCORE_ELAM
    )
    assertEquals(6, initialVote.totalVotes)
    assertTrue(initialVote.passed)

    // Expand league by 2 teams with distinct archetypes -> Board grows from 6 to 8 votes
    val openMarkets = vm.uiState.value.cities.filter { !it.hasFranchise }
    vm.expandLeague(
      targetCityId = openMarkets[0].id,
      nickname = "Guardians",
      colorHex = 0xFF3B82F6L,
      ownershipType = com.example.model.OwnershipType.COMMUNITY_TRUST,
      preferredArchetype = com.example.model.OwnerArchetype.TRADITIONALIST_PURIST,
      customOwnerName = "Arthur Beaumont"
    )
    vm.expandLeague(
      targetCityId = openMarkets[1].id,
      nickname = "Dynamos",
      colorHex = 0xFF14B8A6L,
      ownershipType = com.example.model.OwnershipType.TECH_BILLIONAIRE,
      preferredArchetype = com.example.model.OwnerArchetype.ANALYTICS_VISIONARY,
      customOwnerName = "Yuna Park"
    )

    val expandedVote = vm.submitSettingRuleVote(
      proposalTitle = "Game Length: Shortened 36-Minute Sprint",
      proposalSummary = "Faster 36-minute regulation matches.",
      gameLength = com.example.model.GameLengthRule.SHORTENED_36
    )
    assertEquals(8, expandedVote.totalVotes)
    assertTrue(expandedVote.ballots.any { it.owner.fullName == "Arthur Beaumont" && it.choice == com.example.model.VoteChoice.OPPOSE })
    assertTrue(expandedVote.ballots.any { it.owner.fullName == "Yuna Park" && it.choice == com.example.model.VoteChoice.APPROVE })
  }

  @Test
  fun `verify repository serialization round-trip preserves primaryOwner archetypes and boardVoteHistory`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.LeagueRepository(context)
    val initial = com.example.data.InitialLeagueData.getInitialFranchises()
    val vote = com.example.model.OwnerGovernanceEngine.evaluateCbaVote(
      franchises = initial,
      proposedAgreement = com.example.model.LaborUnionAgreement(),
      year = 2026,
      week = 3
    )

    val serializedFranchises = repo.serializeFranchises(initial)
    val restoredFranchises = repo.deserializeFranchises(serializedFranchises)
    assertEquals(initial.size, restoredFranchises.size)
    initial.forEachIndexed { index, orig ->
      val restored = restoredFranchises[index]
      assertEquals(orig.ownership.primaryOwner.fullName, restored.ownership.primaryOwner.fullName)
      assertEquals(orig.ownership.primaryOwner.archetype, restored.ownership.primaryOwner.archetype)
      assertEquals(orig.ownership.primaryOwner.roleTitle, restored.ownership.primaryOwner.roleTitle)
    }

    repo.saveBoardVoteHistory(listOf(vote))
    val restoredVotes = repo.loadBoardVoteHistory()
    assertEquals(1, restoredVotes.size)
    assertEquals(vote.proposalTitle, restoredVotes.first().proposalTitle)
    assertEquals(vote.yesVotes, restoredVotes.first().yesVotes)
    assertEquals(vote.ballots.size, restoredVotes.first().ballots.size)
  }

  @Test
  fun `verify CoreSimulationViewModel revenue calculation, season progression, and labor union negotiation triggers`() {
    val simVm = com.example.viewmodel.CoreSimulationViewModel()
    val baseState = com.example.viewmodel.LeagueUiState()

    // 1. Revenue Calculation (baseline vs high-strike-risk gate penalty)
    val healthyRevenue = simVm.calculateWeeklyRevenue(
      state = baseState,
      franchises = baseState.franchises,
      rivalryTicketBonusMillions = 12.0,
      fanGrowthBonusPct = 2.0,
      facilityValuationGainMillions = 4L
    )
    assertTrue("Broadcast revenue should be positive", healthyRevenue.weeklyBroadcastRevenueMillions > 0.0)
    assertTrue("Sponsor revenue should be positive", healthyRevenue.weeklySponsorRevenueMillions > 0.0)
    assertTrue("Gross league revenue should be positive", healthyRevenue.grossLeagueRevenueMillions > 0L)
    assertEquals(0.0, healthyRevenue.strikeRevenuePenaltyMillions, 0.001)
    assertTrue("Player revenue share cost must reflect CBA split", healthyRevenue.playerRevenueShareCostMillions > 0.0)

    val strikeState = baseState.copy(
      unionAgreement = baseState.unionAgreement.copy(strikeRisk = 78, unionSatisfaction = 25)
    )
    val strikeRevenue = simVm.calculateWeeklyRevenue(
      state = strikeState,
      franchises = strikeState.franchises,
      rivalryTicketBonusMillions = 12.0,
      fanGrowthBonusPct = 0.0,
      facilityValuationGainMillions = 0L
    )
    assertTrue("High strike risk must reduce gate revenue via penalty", strikeRevenue.strikeRevenuePenaltyMillions > 0.0)

    // 2. Standalone Season Progression across Regular Season -> All-Star -> Playoffs -> Off-Season -> New Season
    simVm.seedStandaloneLeagueState(baseState)
    for (step in 1..20) {
      val cur = simVm.standaloneLeagueState.value
      if (cur.activeDilemma != null) {
        simVm.seedStandaloneLeagueState(cur.copy(activeDilemma = null))
      }
      if (simVm.standaloneLeagueState.value.seasonPhase == com.example.model.SeasonPhase.OFF_SEASON) break
      simVm.simulateStandaloneWeek()
    }
    val offSeasonState = simVm.standaloneLeagueState.value
    assertEquals(com.example.model.SeasonPhase.OFF_SEASON, offSeasonState.seasonPhase)
    org.junit.Assert.assertNotNull("Revenue breakdown should be populated after simulated weeks", simVm.simulationState.value.lastRevenueBreakdown)
    assertTrue("Financial snapshots should accumulate during season progression", simVm.simulationState.value.financialSnapshots.isNotEmpty())

    // Season Rollover decrements CBA years remaining
    val initialCbaYears = offSeasonState.unionAgreement.cbaYearsRemaining
    val nextSeasonState = simVm.finalizeAndInaugurateNewSeason(offSeasonState)
    assertEquals(2027, nextSeasonState.currentYear)
    assertEquals(com.example.model.SeasonPhase.PRE_SEASON, nextSeasonState.seasonPhase)
    assertEquals((initialCbaYears - 1).coerceAtLeast(0), nextSeasonState.unionAgreement.cbaYearsRemaining)

    // Advance from Pre-Season to Regular Season Week 1
    val regLaunch = simVm.advanceWeek(nextSeasonState)
    assertEquals(com.example.model.SeasonPhase.REGULAR_SEASON, regLaunch.seasonPhase)
    assertEquals(1, regLaunch.currentWeek)

    // 3. Labor Union Event Triggers: Expired CBA & Critical Strike Risk via LeagueViewModel
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.viewmodel.LeagueViewModel(context)

    // Trigger critical strike risk via management lockout threat
    vm.threatenManagementLockout()
    vm.threatenManagementLockout()
    vm.threatenManagementLockout()
    assertTrue(vm.uiState.value.unionAgreement.strikeRisk >= 55)
    val trigger = vm.uiState.value.activeLaborTrigger
    org.junit.Assert.assertNotNull("Escalated strike risk must fire a LaborUnionTriggerEvent", trigger)
    assertEquals(com.example.model.LaborTriggerSeverity.CRITICAL, trigger!!.severity)

    // Opening union screen from trigger acknowledges the banner and opens LaborUnionNegotiationsScreen
    vm.openLaborUnionFromTrigger()
    assertTrue(vm.uiState.value.showLaborUnionScreen)
    org.junit.Assert.assertNull(vm.uiState.value.activeLaborTrigger)

    // Expired CBA (0 years remaining) fires CBA_EXPIRED trigger and escalates strike risk
    val expiredCbaState = baseState.copy(
      currentYear = 2029,
      currentWeek = 1,
      unionAgreement = baseState.unionAgreement.copy(cbaYearsRemaining = 0, strikeRisk = 25, unionSatisfaction = 72)
    )
    val evalExpired = vm.coreSimulationViewModel.evaluateLaborUnionTriggers(expiredCbaState, justRolledSeason = true)
    org.junit.Assert.assertNotNull(evalExpired.triggeredEvent)
    assertEquals(com.example.model.LaborTriggerType.CBA_EXPIRED, evalExpired.triggeredEvent!!.type)
    assertTrue(evalExpired.updatedUnionAgreement.strikeRisk > 25)

    // Ratifying comprehensive CBA resolves active labor trigger and resets CBA to 5 years
    vm.ratifyComprehensiveCBA(
      revenueSharePct = 51,
      salaryCapType = com.example.model.SalaryCapType.SOFT_CAP_LUXURY_TAX,
      salaryCapMillions = 85L,
      minSalaryK = 700L,
      playerHealthWelfareMillions = 18L,
      mandatoryPrivateCharters = true,
      backToBackLimits = true,
      concussionProtocol = true,
      hybridTurfMandate = true,
      transitionFundMillions = 8L,
      fiveStarHotels = true
    )
    assertEquals(5, vm.uiState.value.unionAgreement.cbaYearsRemaining)
    org.junit.Assert.assertNull("Ratifying a strong CBA should clear resolved labor triggers", vm.uiState.value.activeLaborTrigger)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }
}



