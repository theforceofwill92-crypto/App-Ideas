package com.example.model

import kotlin.math.roundToLong
import kotlin.random.Random

data class StatsProcessingResult(
    val seasonStats: Map<String, PlayerSeasonStats>,
    val careerStats: Map<String, PlayerCareerStats>,
    val playerOfTheWeek: WeeklyPlayerAward?,
    val recordBook: LeagueRecordBook,
    val brokenAlerts: List<String>
)

object LeagueSimulationEngine {

    fun calculateWeeklyRevenue(
        seasonYear: Int,
        week: Int,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        unionAgreement: LaborUnionAgreement,
        campaigns: List<MarketingCampaign>,
        rivalryTicketBonusMillions: Double,
        fanGrowthBonusPct: Double,
        facilityValuationGainMillions: Long,
        specialEventBonusMillions: Double = 0.0
    ): WeeklyRevenueBreakdown {
        val weeklyBroadcastPayout = (activeBroadcast.annualPayoutMillions / 16.0) + specialEventBonusMillions
        val weeklySponsorRevenue = franchises.sumOf {
            (it.localSponsorRevenueMillions + it.facilities.annualSuitePremiumMillions) *
                FanLoyaltyEngine.sponsorLoyaltyMultiplier(it.fanLoyalty)
        } / 16.0

        val avgLoyalty = if (franchises.isNotEmpty()) franchises.map { it.fanLoyalty }.average() else 75.0
        val rawLoyaltyGateShare = (((avgLoyalty - 65.0) / 100.0) * franchises.size * 0.9).coerceAtLeast(0.0)
        val rawRivalryGateCut = rivalryTicketBonusMillions * 0.25

        // High labor strike risk dampens attendance & gate confidence
        val strikePenaltyFactor = when {
            unionAgreement.strikeRisk >= 75 -> 0.30
            unionAgreement.strikeRisk >= 60 -> 0.15
            else -> 0.0
        }
        val strikePenaltyMillions = (rawLoyaltyGateShare + rawRivalryGateCut) * strikePenaltyFactor
        val netLoyaltyGateShare = (rawLoyaltyGateShare + rawRivalryGateCut - strikePenaltyMillions).coerceAtLeast(0.0)

        val baseLeagueCut = ((weeklyBroadcastPayout + weeklySponsorRevenue) * 0.15) + netLoyaltyGateShare
        val weeklyLeagueTaxCut = baseLeagueCut.roundToLong().coerceAtLeast(1L)

        val luxuryTaxCollected = franchises.sumOf { team ->
            if (team.annualPayrollMillions > unionAgreement.salaryCapMillions &&
                unionAgreement.salaryCapType == SalaryCapType.SOFT_CAP_LUXURY_TAX
            ) {
                (team.annualPayrollMillions - unionAgreement.salaryCapMillions) / 16L
            } else 0L
        }

        val playerRevenueShareCost = ((weeklyBroadcastPayout + weeklySponsorRevenue) *
            (unionAgreement.playerRevenueSharePct / 100.0))
        val operationsCost = 3L + (franchises.size * 0.5).toLong()
        val marketingCost = if (campaigns.any { it.isActive }) 1.8 else 0.6
        val welfareCost = (unionAgreement.playerHealthWelfareMillions / 16.0).coerceAtLeast(0.9)

        val grossRevenue = weeklyLeagueTaxCut + luxuryTaxCollected
        val netCashFlow = grossRevenue - operationsCost
        val valuationDelta = (netCashFlow * 2L) + (fanGrowthBonusPct * 3.0).toLong() + facilityValuationGainMillions

        val snapshot = FinancialSnapshot(
            periodLabel = "W$week",
            weekNumber = week,
            broadcastRevenue = (weeklyBroadcastPayout * 0.15).toFloat(),
            sponsorshipRevenue = (weeklySponsorRevenue * 0.15).toFloat(),
            expansionFeeRevenue = 0f,
            luxuryTaxRevenue = luxuryTaxCollected.toFloat(),
            totalRevenue = grossRevenue.toFloat(),
            operationsExpense = operationsCost.toFloat(),
            marketingExpense = marketingCost.toFloat(),
            welfareAndGrantsExpense = welfareCost.toFloat(),
            totalExpenses = (operationsCost + marketingCost + welfareCost).toFloat(),
            netCashFlow = netCashFlow.toFloat(),
            cashReserves = 0f, // Caller fills updated cash balance
            valuation = 0f     // Caller fills updated valuation balance
        )

        return WeeklyRevenueBreakdown(
            seasonYear = seasonYear,
            weekNumber = week,
            phaseLabel = "Week $week Regular Season",
            weeklyBroadcastRevenueMillions = weeklyBroadcastPayout,
            weeklySponsorRevenueMillions = weeklySponsorRevenue,
            loyaltyGateShareMillions = rawLoyaltyGateShare,
            rivalryGateShareMillions = rawRivalryGateCut,
            luxuryTaxCollectedMillions = luxuryTaxCollected,
            allStarShowcaseRevenueMillions = 0L,
            strikeRevenuePenaltyMillions = strikePenaltyMillions,
            grossLeagueRevenueMillions = grossRevenue,
            playerRevenueSharePct = unionAgreement.playerRevenueSharePct,
            playerRevenueShareCostMillions = playerRevenueShareCost,
            operationsCostMillions = operationsCost,
            marketingCostMillions = marketingCost,
            welfareAndGrantsCostMillions = welfareCost,
            netCashFlowMillions = netCashFlow,
            valuationDeltaMillions = valuationDelta,
            snapshot = snapshot
        )
    }

    fun calculatePlayoffRoundRevenue(
        seasonYear: Int,
        week: Int,
        roundLabel: String,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        unionAgreement: LaborUnionAgreement,
        rivalryTicketBonusMillions: Double,
        facilityValuationGainMillions: Long
    ): WeeklyRevenueBreakdown {
        val playoffRivalryCut = (rivalryTicketBonusMillions * 0.25).roundToLong()
        val broadcastShare = activeBroadcast.annualPayoutMillions / 16.0
        val sponsorShare = franchises.sumOf { it.localSponsorRevenueMillions } / 16.0
        val grossRevenue = playoffRivalryCut.coerceAtLeast(1L)
        val playerShareCost = (broadcastShare + sponsorShare) * (unionAgreement.playerRevenueSharePct / 100.0)
        val welfareCost = (unionAgreement.playerHealthWelfareMillions / 16.0).coerceAtLeast(0.9)

        val snapshot = FinancialSnapshot(
            periodLabel = "PO W$week",
            weekNumber = week,
            broadcastRevenue = (broadcastShare * 0.15).toFloat(),
            sponsorshipRevenue = (sponsorShare * 0.15).toFloat(),
            expansionFeeRevenue = 0f,
            luxuryTaxRevenue = 0f,
            totalRevenue = grossRevenue.toFloat(),
            operationsExpense = 2.5f,
            marketingExpense = 1.2f,
            welfareAndGrantsExpense = welfareCost.toFloat(),
            totalExpenses = (3.7 + welfareCost).toFloat(),
            netCashFlow = playoffRivalryCut.toFloat(),
            cashReserves = 0f,
            valuation = 0f
        )

        return WeeklyRevenueBreakdown(
            seasonYear = seasonYear,
            weekNumber = week,
            phaseLabel = roundLabel,
            weeklyBroadcastRevenueMillions = broadcastShare,
            weeklySponsorRevenueMillions = sponsorShare,
            loyaltyGateShareMillions = 0.0,
            rivalryGateShareMillions = rivalryTicketBonusMillions * 0.25,
            luxuryTaxCollectedMillions = 0L,
            allStarShowcaseRevenueMillions = 0L,
            strikeRevenuePenaltyMillions = 0.0,
            grossLeagueRevenueMillions = grossRevenue,
            playerRevenueSharePct = unionAgreement.playerRevenueSharePct,
            playerRevenueShareCostMillions = playerShareCost,
            operationsCostMillions = 2L,
            marketingCostMillions = 1.2,
            welfareAndGrantsCostMillions = welfareCost,
            netCashFlowMillions = playoffRivalryCut,
            valuationDeltaMillions = facilityValuationGainMillions,
            snapshot = snapshot
        )
    }

    fun generateMatchesForWeek(
        week: Int,
        teams: List<Franchise>,
        sport: SportType,
        rivalries: List<TeamRivalry> = emptyList(),
        cities: List<MarketCity> = emptyList()
    ): List<GameMatch> {
        val matches = mutableListOf<GameMatch>()
        if (teams.size < 2) return matches
        val shuffled = teams.shuffled(Random(week * 7919))
        for (i in 0 until shuffled.size - 1 step 2) {
            val home = shuffled[i]
            val away = shuffled[i + 1]
            matches.add(GameMatch("match_w${week}_${home.id}_${away.id}", week, home.id, away.id))
        }
        return if (rivalries.isNotEmpty()) {
            RivalryEngine.annotateMatchesWithRivalries(matches, teams, rivalries, cities)
        } else matches
    }

    fun generateDefaultFinancialSnapshots(
        currentWeek: Int,
        broadcastAnnual: Long,
        totalSponsorsAnnual: Long,
        currentCash: Long,
        currentValuation: Long
    ): List<FinancialSnapshot> {
        val list = mutableListOf<FinancialSnapshot>()
        val numWeeks = currentWeek.coerceAtLeast(6)
        var runningCash = (currentCash.toFloat() - (numWeeks * 1.2f)).coerceAtLeast(65f)
        var runningVal = (currentValuation.toFloat() - (numWeeks * 4.5f)).coerceAtLeast(320f)

        for (w in 1..numWeeks) {
            val bCast = (broadcastAnnual / 16f) * 0.15f
            val sponsor = (totalSponsorsAnnual / 16f) * 0.15f
            val expFee = if (w == 3) 25f else 0f
            val luxTax = if (w % 4 == 0) 2.2f else 0.4f
            val totalRev = bCast + sponsor + expFee + luxTax

            val ops = 3.2f + (w * 0.08f)
            val mkt = if (w % 2 == 0) 1.6f else 0.7f
            val welfare = 0.8f
            val totalExp = ops + mkt + welfare
            val net = totalRev - totalExp
            runningCash += net
            runningVal += (net * 2.2f) + 1.2f

            list.add(
                FinancialSnapshot(
                    periodLabel = "W$w",
                    weekNumber = w,
                    broadcastRevenue = bCast,
                    sponsorshipRevenue = sponsor,
                    expansionFeeRevenue = expFee,
                    luxuryTaxRevenue = luxTax,
                    totalRevenue = totalRev,
                    operationsExpense = ops,
                    marketingExpense = mkt,
                    welfareAndGrantsExpense = welfare,
                    totalExpenses = totalExp,
                    netCashFlow = net,
                    cashReserves = runningCash,
                    valuation = runningVal
                )
            )
        }
        return list
    }

    fun simulateRegularSeasonMatches(
        week: Int,
        sport: SportType,
        ruleBook: LeagueRuleBook,
        matchesToPlay: List<GameMatch>,
        updatedFranchises: MutableList<Franchise>,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>,
        committeeModifiers: RuleCommitteeModifierSummary = RuleCommitteeModifierSummary.NONE
    ): List<GameMatch> {
        val baseScore = sport.baseScoreScale
        val isSalaryCapEnacted = ruleBook.isRuleEnacted("rule_salary_cap")
        val isShotClockEnacted = ruleBook.isRuleEnacted("rule_shot_clock")
        val isFreedomOfMovementEnacted = ruleBook.isRuleEnacted("rule_freedom_of_movement")
        val isInstantReplayEnacted = ruleBook.isRuleEnacted("rule_instant_replay")
        val isRevenueSharingEnacted = ruleBook.isRuleEnacted("rule_revenue_sharing")

        val tempoBonus = (if (isShotClockEnacted) 10 else -8) + (if (isFreedomOfMovementEnacted) 5 else -3)
        val adjustedBaseScore = (baseScore + tempoBonus).coerceAtLeast(sport.baseScoreScale / 2)
        val bigMarkets = listOf("New York", "Los Angeles", "London", "Tokyo", "Chicago")
        val lengthScale = ruleBook.gameLength.scoreScaleMultiplier
        val commScoreDelta = if (committeeModifiers.sportType == sport) committeeModifiers.scoringDeltaPerTeam else 0.0
        val commVariance = if (committeeModifiers.sportType == sport) committeeModifiers.varianceDelta else 0
        val commUnderdogBoost = if (committeeModifiers.sportType == sport) committeeModifiers.underdogBoost else 0
        val commClutchSwing = if (committeeModifiers.sportType == sport) committeeModifiers.closeGameSwingPct else 0

        return matchesToPlay.map { match ->
            val home = updatedFranchises.find { it.id == match.homeTeamId } ?: return@map match
            val away = updatedFranchises.find { it.id == match.awayTeamId } ?: return@map match

            val homeBigMarketBonus = if (!isSalaryCapEnacted && home.cityName in bigMarkets) +10 else if (!isSalaryCapEnacted) -4 else 0
            val awayBigMarketBonus = if (!isSalaryCapEnacted && away.cityName in bigMarkets) +10 else if (!isSalaryCapEnacted) -4 else 0
            val homeSmallMarketBonus = if (isRevenueSharingEnacted && home.cityName !in bigMarkets) +4 else 0
            val awaySmallMarketBonus = if (isRevenueSharingEnacted && away.cityName !in bigMarkets) +4 else 0

            val homeUnderdogAdj = if (home.effectiveLineupRating + 3 < away.effectiveLineupRating) commUnderdogBoost else 0
            val awayUnderdogAdj = if (away.effectiveLineupRating + 3 < home.effectiveLineupRating) commUnderdogBoost else 0

            val rivalryImpact = RivalryEngine.evaluateMatchupImpact(
                home = home,
                away = away,
                rivalries = rivalries,
                cities = cities,
                isPlayoffs = false
            )

            val specialBoost = (if (match.isHolidayMarquee) 2 else 0) + (if (match.isInternationalShowcase) 1 else 0)
            val homeStrength = home.effectiveLineupRating + (home.fanMorale / 10) + (if (match.isInternationalShowcase) 1 else 3) + specialBoost + rivalryImpact.crowdHypeBoost + homeBigMarketBonus + homeSmallMarketBonus + homeUnderdogAdj + Random.nextInt(-6 - commVariance.coerceAtLeast(0), 7 + commVariance.coerceAtLeast(0))
            val awayStrength = away.effectiveLineupRating + (away.fanMorale / 10) + awayBigMarketBonus + awaySmallMarketBonus + awayUnderdogAdj + Random.nextInt(-6 - commVariance.coerceAtLeast(0), 7 + commVariance.coerceAtLeast(0))

            var homeScore: Int
            var awayScore: Int
            var shootoutDecided = false
            var clutchTriggered = false

            when (sport) {
                SportType.BASKETBALL -> {
                    val spread = (8 + commVariance).coerceIn(4, 16)
                    val rawHome = (adjustedBaseScore + (homeStrength * 0.4) + commScoreDelta + Random.nextInt(-spread, spread + 1)) * lengthScale
                    val rawAway = (adjustedBaseScore + (awayStrength * 0.4) + commScoreDelta + Random.nextInt(-spread, spread + 1)) * lengthScale
                    homeScore = maxOf(45, rawHome.toInt())
                    awayScore = maxOf(45, rawAway.toInt()).let {
                        if (it == homeScore) it + if (homeStrength >= awayStrength) -1 else 1 else it
                    }
                    if (commClutchSwing > 0 && kotlin.math.abs(homeScore - awayScore) <= 10 && Random.nextInt(100) < commClutchSwing) {
                        clutchTriggered = true
                        if (homeScore < awayScore) homeScore = awayScore + Random.nextInt(1, 5)
                        else awayScore = homeScore + Random.nextInt(1, 5)
                    }
                }
                SportType.FOOTBALL -> {
                    val spread = (7 + commVariance).coerceIn(4, 14)
                    val rawHome = (adjustedBaseScore + ((homeStrength - 75) * 0.3) + commScoreDelta + Random.nextInt(-spread, spread + 1)) * lengthScale
                    val rawAway = (adjustedBaseScore + ((awayStrength - 75) * 0.3) + commScoreDelta + Random.nextInt(-spread, spread + 1)) * lengthScale
                    homeScore = maxOf(3, rawHome.toInt())
                    awayScore = maxOf(3, rawAway.toInt()).let {
                        if (it == homeScore) it + if (homeStrength >= awayStrength) -3 else 3 else it
                    }
                    if (commClutchSwing > 0 && kotlin.math.abs(homeScore - awayScore) <= 8 && Random.nextInt(100) < commClutchSwing) {
                        clutchTriggered = true
                        if (homeScore < awayScore) homeScore = awayScore + 3 else awayScore = homeScore + 3
                    }
                }
                SportType.SOCCER -> {
                    val goalJitter = (1 + commVariance.coerceAtLeast(0).coerceAtMost(2))
                    val rawHome = (baseScore + ((homeStrength - 75) * 0.05) + commScoreDelta + Random.nextInt(-1, goalJitter + 1)) * lengthScale
                    val rawAway = (baseScore + ((awayStrength - 75) * 0.05) + commScoreDelta + Random.nextInt(-1, goalJitter + 1)) * lengthScale
                    homeScore = maxOf(0, kotlin.math.round(rawHome).toInt())
                    awayScore = maxOf(0, kotlin.math.round(rawAway).toInt())
                    if (homeScore == awayScore) {
                        if (committeeModifiers.convertDrawsToShootout) {
                            shootoutDecided = true
                            if (homeStrength >= awayStrength) homeScore += 1 else awayScore += 1
                        } else if (Random.nextBoolean()) {
                            awayScore += 1
                        }
                    }
                }
                SportType.HOCKEY -> {
                    val goalJitter = (2 + commVariance.coerceAtLeast(0).coerceAtMost(2))
                    val rawHome = (baseScore + ((homeStrength - 75) * 0.1) + commScoreDelta + Random.nextInt(-2, goalJitter + 1)) * lengthScale
                    val rawAway = (baseScore + ((awayStrength - 75) * 0.1) + commScoreDelta + Random.nextInt(-2, goalJitter + 1)) * lengthScale
                    homeScore = maxOf(1, kotlin.math.round(rawHome).toInt())
                    awayScore = maxOf(1, kotlin.math.round(rawAway).toInt())
                    if (homeScore == awayScore) {
                        if (committeeModifiers.convertDrawsToShootout || commClutchSwing > 0) {
                            shootoutDecided = true
                        }
                        if (homeStrength >= awayStrength) homeScore += 1 else awayScore += 1
                    }
                }
            }

            if (!isInstantReplayEnacted && !committeeModifiers.suppressBlownCallSwing &&
                kotlin.math.abs(homeScore - awayScore) in 1..3 && Random.nextInt(100) < 25
            ) {
                if (homeScore > awayScore) awayScore += 4 else homeScore += 4
            }

            val homeWon = homeScore > awayScore
            val homeIndex = updatedFranchises.indexOfFirst { it.id == home.id }
            val awayIndex = updatedFranchises.indexOfFirst { it.id == away.id }

            if (homeIndex != -1 && awayIndex != -1) {
                val specialEventMorale = (if (match.isHolidayMarquee) 2 else 0) + (if (match.isInternationalShowcase) 2 else 0)
                val moraleBonus = (if ((clutchTriggered || shootoutDecided) && committeeModifiers.totalFanAppealDelta >= 8) 1 else 0) + specialEventMorale
                updatedFranchises[homeIndex] = home.copy(
                    wins = home.wins + if (homeWon) 1 else 0,
                    losses = home.losses + if (!homeWon) 1 else 0,
                    pointsFor = home.pointsFor + homeScore,
                    pointsAgainst = home.pointsAgainst + awayScore,
                    fanMorale = (home.fanMorale + (if (homeWon) 2 else -2) + moraleBonus).coerceIn(30, 99)
                )
                updatedFranchises[awayIndex] = away.copy(
                    wins = away.wins + if (!homeWon) 1 else 0,
                    losses = away.losses + if (homeWon) 1 else 0,
                    pointsFor = away.pointsFor + awayScore,
                    pointsAgainst = away.pointsAgainst + homeScore,
                    fanMorale = (away.fanMorale + (if (!homeWon) 2 else -2) + moraleBonus).coerceIn(30, 99)
                )
            }

            val winnerName = if (homeWon) home.teamFullName else away.teamFullName
            val loserName = if (homeWon) away.teamFullName else home.teamFullName
            val ruleBadgeNote = when {
                committeeModifiers.activeRuleCount == 0 -> ""
                shootoutDecided && sport == SportType.SOCCER -> "Rule Effect: Penalty Shootout Decider"
                shootoutDecided && sport == SportType.HOCKEY -> "Rule Effect: Continuous 3v3 OT"
                clutchTriggered -> "Rule Effect: ${committeeModifiers.activeRuleTitles.firstOrNull() ?: "Clutch Reform"} Rally"
                kotlin.math.abs(commScoreDelta) >= 0.5 -> {
                    val firstTitle = committeeModifiers.activeRuleTitles.firstOrNull() ?: "Rule Committee"
                    val dir = if (commScoreDelta > 0) "Tempo Surge" else "Defensive Grind"
                    "Rule Effect: $firstTitle ($dir)"
                }
                else -> "Active Committee Rule: ${committeeModifiers.activeRuleTitles.firstOrNull()}"
            }
            val specialBanner = listOfNotNull(
                match.holidayTentpoleLabel?.let { it.uppercase() },
                match.internationalVenueLabel?.let { "AT $it".uppercase() }
            ).joinToString(" • ")
            val storyPrefix = when {
                specialBanner.isNotBlank() -> "$specialBanner: $winnerName defeat $loserName ($homeScore - $awayScore) before a sold-out showcase crowd!"
                shootoutDecided -> "DECISIVE TIEBREAK: $winnerName prevail over $loserName via late shootout/OT reform ($homeScore - $awayScore)!"
                clutchTriggered -> "COMEBACK CLASSIC: $winnerName storm back to stun $loserName ($homeScore - $awayScore) under new committee bylaws!"
                else -> "$winnerName edge out $loserName in thrilling Week $week finish ($homeScore - $awayScore)!"
            }

            match.copy(
                homeScore = homeScore,
                awayScore = awayScore,
                isPlayed = true,
                highlightStory = storyPrefix,
                ruleImpactNote = ruleBadgeNote
            )
        }
    }

    fun applyRivalryWeekResults(
        franchises: List<Franchise>,
        rivalryResult: RivalryWeekProcessingResult
    ): List<Franchise> {
        if (rivalryResult.ticketBonusByFranchiseId.isEmpty() && rivalryResult.moraleAdjustByFranchiseId.isEmpty()) {
            return franchises
        }
        return franchises.map { team ->
            val bonusGate = rivalryResult.ticketBonusByFranchiseId[team.id] ?: 0.0
            val moraleDelta = rivalryResult.moraleAdjustByFranchiseId[team.id] ?: 0
            if (bonusGate == 0.0 && moraleDelta == 0) {
                team
            } else {
                team.copy(
                    fanMorale = (team.fanMorale + moraleDelta).coerceIn(30, 99),
                    rivalryTicketBonusMillions = Math.round((team.rivalryTicketBonusMillions + bonusGate) * 100.0) / 100.0
                )
            }
        }
    }

    fun processWeekPlayerStatsAndRecords(
        currentYear: Int,
        sportType: SportType,
        seasonPlayerStats: Map<String, PlayerSeasonStats>,
        careerPlayerStats: Map<String, PlayerCareerStats>,
        recordBook: LeagueRecordBook,
        weekStats: List<PlayerGameStatLine>,
        playedMatches: List<GameMatch>,
        updatedFranchises: List<Franchise>,
        week: Int,
        isPlayoffs: Boolean
    ): StatsProcessingResult {
        val seasonMap = seasonPlayerStats.toMutableMap()
        val careerMap = careerPlayerStats.toMutableMap()

        val potw = RecordBookEngine.selectPlayerOfTheWeek(
            week = week,
            seasonYear = currentYear,
            weekStats = weekStats,
            isPlayoffs = isPlayoffs
        )

        weekStats.forEach { line ->
            val wonHonor = if (potw != null && potw.playerId == line.playerId) 1 else 0
            val existingSeason = seasonMap[line.playerId]
            val rosterOvr = updatedFranchises.flatMap { it.roster }.find { it.id == line.playerId }?.overallRating ?: 80

            seasonMap[line.playerId] = if (existingSeason != null) {
                existingSeason.copy(
                    franchiseId = line.franchiseId,
                    franchiseName = line.franchiseName,
                    gamesPlayed = existingSeason.gamesPlayed + if (!isPlayoffs) 1 else 0,
                    playoffGamesPlayed = existingSeason.playoffGamesPlayed + if (isPlayoffs) 1 else 0,
                    totalPoints = existingSeason.totalPoints + line.points,
                    totalRebounds = existingSeason.totalRebounds + line.rebounds,
                    totalAssists = existingSeason.totalAssists + line.assists,
                    totalStealsBlocks = existingSeason.totalStealsBlocks + line.stealsBlocks,
                    playoffPoints = existingSeason.playoffPoints + if (isPlayoffs) line.points else 0,
                    playoffRebounds = existingSeason.playoffRebounds + if (isPlayoffs) line.rebounds else 0,
                    playoffAssists = existingSeason.playoffAssists + if (isPlayoffs) line.assists else 0,
                    weeklyHonorsCount = existingSeason.weeklyHonorsCount + wonHonor,
                    bestGamePoints = maxOf(existingSeason.bestGamePoints, line.points),
                    overallRating = rosterOvr
                )
            } else {
                PlayerSeasonStats(
                    playerId = line.playerId,
                    playerName = line.playerName,
                    franchiseId = line.franchiseId,
                    franchiseName = line.franchiseName,
                    position = line.position,
                    seasonYear = currentYear,
                    gamesPlayed = if (!isPlayoffs) 1 else 0,
                    playoffGamesPlayed = if (isPlayoffs) 1 else 0,
                    totalPoints = line.points,
                    totalRebounds = line.rebounds,
                    totalAssists = line.assists,
                    totalStealsBlocks = line.stealsBlocks,
                    playoffPoints = if (isPlayoffs) line.points else 0,
                    playoffRebounds = if (isPlayoffs) line.rebounds else 0,
                    playoffAssists = if (isPlayoffs) line.assists else 0,
                    weeklyHonorsCount = wonHonor,
                    bestGamePoints = line.points,
                    overallRating = rosterOvr
                )
            }

            val existingCareer = careerMap[line.playerId]
            careerMap[line.playerId] = if (existingCareer != null) {
                existingCareer.copy(
                    primaryFranchiseId = line.franchiseId,
                    primaryFranchiseName = line.franchiseName,
                    gamesPlayed = existingCareer.gamesPlayed + 1,
                    totalPoints = existingCareer.totalPoints + line.points,
                    totalRebounds = existingCareer.totalRebounds + line.rebounds,
                    totalAssists = existingCareer.totalAssists + line.assists,
                    totalStealsBlocks = existingCareer.totalStealsBlocks + line.stealsBlocks,
                    playoffPoints = existingCareer.playoffPoints + if (isPlayoffs) line.points else 0,
                    weeklyHonorsCount = existingCareer.weeklyHonorsCount + wonHonor,
                    bestGamePoints = maxOf(existingCareer.bestGamePoints, line.points),
                    peakRating = maxOf(existingCareer.peakRating, rosterOvr)
                )
            } else {
                PlayerCareerStats(
                    playerId = line.playerId,
                    playerName = line.playerName,
                    primaryFranchiseId = line.franchiseId,
                    primaryFranchiseName = line.franchiseName,
                    position = line.position,
                    seasonsPlayed = 1,
                    gamesPlayed = 1,
                    totalPoints = line.points,
                    totalRebounds = line.rebounds,
                    totalAssists = line.assists,
                    totalStealsBlocks = line.stealsBlocks,
                    playoffPoints = if (isPlayoffs) line.points else 0,
                    championships = 0,
                    weeklyHonorsCount = wonHonor,
                    bestGamePoints = line.points,
                    peakRating = rosterOvr
                )
            }
        }

        val (updatedRecordBook, brokenAlerts) = RecordBookEngine.updateRecordsFromWeek(
            currentBook = recordBook,
            weekStats = weekStats,
            playedMatches = playedMatches,
            updatedFranchises = updatedFranchises,
            updatedSeasonStats = seasonMap,
            updatedCareerStats = careerMap,
            sportType = sportType,
            seasonYear = currentYear,
            week = week,
            isPlayoffs = isPlayoffs
        )

        return StatsProcessingResult(
            seasonStats = seasonMap,
            careerStats = careerMap,
            playerOfTheWeek = potw,
            recordBook = updatedRecordBook,
            brokenAlerts = brokenAlerts
        )
    }

    fun buildInitialPlayoffBracket(
        week: Int,
        franchises: List<Franchise>,
        playoffRules: PlayoffRules,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>
    ): Pair<List<GameMatch>, String>? {
        val seeds = calculatePlayoffSeeds(franchises, playoffRules)
        if (seeds.size < 2) return null

        val qualifiedTeams = seeds.map { it.franchise }
        val byesCount = playoffRules.firstRoundByesCount.coerceIn(0, seeds.size / 2)
        val initialMatches = mutableListOf<GameMatch>()

        when {
            seeds.size == 2 -> {
                initialMatches.add(
                    GameMatch("playoff_finals", week, qualifiedTeams[0].id, qualifiedTeams[1].id, roundName = "Championship Finals")
                )
            }
            seeds.size == 4 -> {
                if (byesCount >= 1) {
                    initialMatches.add(
                        GameMatch("playoff_semi_2v3", week, qualifiedTeams[1].id, qualifiedTeams[2].id, roundName = "Championship Semifinals")
                    )
                } else {
                    initialMatches.addAll(
                        listOf(
                            GameMatch("playoff_semi1", week, qualifiedTeams[0].id, qualifiedTeams[3].id, roundName = "Championship Semifinals"),
                            GameMatch("playoff_semi2", week, qualifiedTeams[1].id, qualifiedTeams[2].id, roundName = "Championship Semifinals")
                        )
                    )
                }
            }
            seeds.size == 6 -> {
                initialMatches.addAll(
                    listOf(
                        GameMatch("playoff_wc_3v6", week, qualifiedTeams[2].id, qualifiedTeams[5].id, roundName = "Wild Card Opening Round"),
                        GameMatch("playoff_wc_4v5", week, qualifiedTeams[3].id, qualifiedTeams[4].id, roundName = "Wild Card Opening Round")
                    )
                )
            }
            seeds.size in 7..8 -> {
                if (seeds.size == 7) {
                    initialMatches.addAll(
                        listOf(
                            GameMatch("playoff_qf1", week, qualifiedTeams[1].id, qualifiedTeams[6].id, roundName = "Playoff Quarterfinals"),
                            GameMatch("playoff_qf2", week, qualifiedTeams[2].id, qualifiedTeams[5].id, roundName = "Playoff Quarterfinals"),
                            GameMatch("playoff_qf3", week, qualifiedTeams[3].id, qualifiedTeams[4].id, roundName = "Playoff Quarterfinals")
                        )
                    )
                } else {
                    initialMatches.addAll(
                        listOf(
                            GameMatch("playoff_qf1", week, qualifiedTeams[0].id, qualifiedTeams[7].id, roundName = "Playoff Quarterfinals"),
                            GameMatch("playoff_qf2", week, qualifiedTeams[1].id, qualifiedTeams[6].id, roundName = "Playoff Quarterfinals"),
                            GameMatch("playoff_qf3", week, qualifiedTeams[2].id, qualifiedTeams[5].id, roundName = "Playoff Quarterfinals"),
                            GameMatch("playoff_qf4", week, qualifiedTeams[3].id, qualifiedTeams[4].id, roundName = "Playoff Quarterfinals")
                        )
                    )
                }
            }
            else -> {
                val prelimGamesCount = (seeds.size - 8).coerceAtLeast(1)
                val qfByeCount = seeds.size - (prelimGamesCount * 2)
                for (i in 0 until prelimGamesCount) {
                    val highIdx = qfByeCount + i
                    val lowIdx = seeds.size - 1 - i
                    if (highIdx < lowIdx) {
                        initialMatches.add(
                            GameMatch(
                                id = "playoff_global_wc_${i + 1}",
                                week = week,
                                homeTeamId = qualifiedTeams[highIdx].id,
                                awayTeamId = qualifiedTeams[lowIdx].id,
                                roundName = "Global Opening Round"
                            )
                        )
                    }
                }
            }
        }

        val activeConfsCount = franchises.map { it.conference }.distinct().size
        val effectiveByesDisplayed = when {
            seeds.size > 8 -> seeds.size - ((seeds.size - 8) * 2)
            seeds.size == 7 -> 1
            seeds.size == 6 -> 2
            else -> byesCount
        }
        val byeNotes = if (effectiveByesDisplayed > 0) " (${seeds.take(effectiveByesDisplayed).joinToString { it.franchise.nickname }} earn 1st-round BYES)" else ""
        val newsItem = "POSTSEASON COMMENCES: ${seeds.size}-team intercontinental championship bracket ($activeConfsCount conferences) locked under league bylaws$byeNotes!"

        val annotated = RivalryEngine.annotateMatchesWithRivalries(
            matches = initialMatches,
            franchises = franchises,
            rivalries = rivalries,
            cities = cities,
            isPlayoffs = true
        )
        return annotated to newsItem
    }
}
