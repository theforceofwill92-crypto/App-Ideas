package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

enum class RecordCategory(val displayName: String, val shortBadge: String) {
    PLAYER_GAME("Player Single-Game", "PLY GAME"),
    PLAYER_SEASON("Player Single-Season", "PLY SEASON"),
    PLAYER_CAREER("Player All-Time Career", "CAREER"),
    FRANCHISE_GAME("Franchise Single-Game", "TEAM GAME"),
    FRANCHISE_SEASON("Franchise Single-Season", "TEAM SEASON")
}

data class SportStatLabels(
    val primaryShort: String,
    val primaryFull: String,
    val secondaryShort: String,
    val secondaryFull: String,
    val tertiaryShort: String,
    val tertiaryFull: String,
    val defenseShort: String,
    val defenseFull: String
)

data class PlayerGameStatLine(
    val playerId: String,
    val playerName: String,
    val franchiseId: String,
    val franchiseName: String,
    val position: String,
    val week: Int,
    val seasonYear: Int,
    val isPlayoffs: Boolean = false,
    val roundName: String = "",
    val opponentId: String,
    val opponentName: String,
    val teamWon: Boolean,
    val points: Int,
    val rebounds: Int,
    val assists: Int,
    val stealsBlocks: Int,
    val minutes: Int,
    val efficiencyScore: Int,
    val statSummary: String
)

data class PlayerSeasonStats(
    val playerId: String,
    val playerName: String,
    val franchiseId: String,
    val franchiseName: String,
    val position: String,
    val seasonYear: Int,
    val gamesPlayed: Int = 0,
    val playoffGamesPlayed: Int = 0,
    val totalPoints: Int = 0,
    val totalRebounds: Int = 0,
    val totalAssists: Int = 0,
    val totalStealsBlocks: Int = 0,
    val playoffPoints: Int = 0,
    val playoffRebounds: Int = 0,
    val playoffAssists: Int = 0,
    val weeklyHonorsCount: Int = 0,
    val bestGamePoints: Int = 0,
    val overallRating: Int = 80
) {
    val totalAppearances: Int
        get() = (gamesPlayed + playoffGamesPlayed).coerceAtLeast(1)

    val ppg: Double
        get() = ((totalPoints.toDouble() / totalAppearances) * 10.0).roundToInt() / 10.0

    val rpg: Double
        get() = ((totalRebounds.toDouble() / totalAppearances) * 10.0).roundToInt() / 10.0

    val apg: Double
        get() = ((totalAssists.toDouble() / totalAppearances) * 10.0).roundToInt() / 10.0

    val spg: Double
        get() = ((totalStealsBlocks.toDouble() / totalAppearances) * 10.0).roundToInt() / 10.0
}

data class PlayerCareerStats(
    val playerId: String,
    val playerName: String,
    val primaryFranchiseId: String,
    val primaryFranchiseName: String,
    val position: String,
    val seasonsPlayed: Int = 1,
    val gamesPlayed: Int = 0,
    val totalPoints: Int = 0,
    val totalRebounds: Int = 0,
    val totalAssists: Int = 0,
    val totalStealsBlocks: Int = 0,
    val playoffPoints: Int = 0,
    val championships: Int = 0,
    val weeklyHonorsCount: Int = 0,
    val bestGamePoints: Int = 0,
    val peakRating: Int = 80,
    val isRetired: Boolean = false
) {
    val careerPpg: Double
        get() = if (gamesPlayed <= 0) 0.0 else ((totalPoints.toDouble() / gamesPlayed) * 10.0).roundToInt() / 10.0
}

data class WeeklyPlayerAward(
    val week: Int,
    val seasonYear: Int,
    val playerId: String,
    val playerName: String,
    val franchiseId: String,
    val franchiseName: String,
    val position: String,
    val statLineSummary: String,
    val points: Int,
    val rebounds: Int,
    val assists: Int,
    val impactScore: Int,
    val opponentName: String,
    val isPlayoffs: Boolean = false
)

data class RecordEntry(
    val id: String,
    val category: RecordCategory,
    val recordTitle: String,
    val holderName: String,
    val teamName: String,
    val valueDisplay: String,
    val numericValue: Double,
    val contextLabel: String,
    val isActiveEraRecord: Boolean = false,
    val isPlayoffRecord: Boolean = false
)

data class HallOfFameInductee(
    val id: String,
    val playerId: String,
    val name: String,
    val inductionYear: Int,
    val position: String,
    val primaryFranchiseId: String,
    val primaryFranchiseName: String,
    val careerPoints: Int,
    val careerRebounds: Int,
    val careerAssists: Int,
    val gamesPlayed: Int,
    val championships: Int,
    val weeklyHonors: Int,
    val peakRating: Int,
    val legacyTier: String,
    val bio: String,
    val inductionQuote: String
)

data class LeagueRecordBook(
    val playerGameRecords: List<RecordEntry> = emptyList(),
    val playerSeasonRecords: List<RecordEntry> = emptyList(),
    val playerCareerRecords: List<RecordEntry> = emptyList(),
    val franchiseGameRecords: List<RecordEntry> = emptyList(),
    val franchiseSeasonRecords: List<RecordEntry> = emptyList(),
    val recentRecordBrokenAlerts: List<String> = emptyList()
) {
    val allRecordsCount: Int
        get() = playerGameRecords.size + playerSeasonRecords.size + playerCareerRecords.size +
                franchiseGameRecords.size + franchiseSeasonRecords.size
}

enum class AnnualAwardType(
    val displayTitle: String,
    val badgeLabel: String,
    val accentColorHex: Long
) {
    LEAGUE_MVP("Most Valuable Player (MVP)", "MVP", 0xFFF59E0B),
    FINALS_MVP("Finals / Postseason MVP", "FMVP", 0xFF10B981),
    SCORING_CHAMPION("Scoring Champion", "SCORING", 0xFFEF4444),
    DEFENSIVE_POY("Defensive Player of the Year", "DPOY", 0xFF3B82F6),
    PLAYMAKER_OF_YEAR("Playmaker of the Year", "PLAYMAKER", 0xFF06B6D4),
    RISING_STAR("Rising Star Award", "RISING STAR", 0xFF8B5CF6),
    EXECUTIVE_FRANCHISE("Franchise of the Year", "FRANCHISE", 0xFFEAB308)
}

data class MvpFinalist(
    val playerId: String,
    val playerName: String,
    val position: String,
    val teamName: String,
    val mvpScore: Double,
    val voteSharePct: Int,
    val statLine: String
)

data class YearlyAwardWinner(
    val id: String,
    val year: Int,
    val awardType: AnnualAwardType,
    val winnerName: String,
    val position: String,
    val franchiseId: String,
    val franchiseName: String,
    val primaryStatLine: String,
    val supportingNarrative: String,
    val scoreValue: Double
)

data class YearlyAwardsCeremony(
    val year: Int,
    val hostCity: String,
    val ceremonyTitle: String,
    val awards: List<YearlyAwardWinner> = emptyList(),
    val mvpFinalists: List<MvpFinalist> = emptyList(),
    val allLeagueFirstTeam: List<PlayerSeasonStats> = emptyList(),
    val isPresented: Boolean = false
)

object RecordBookEngine {

    fun getStatLabels(sportType: SportType): SportStatLabels = when (sportType) {
        SportType.BASKETBALL -> SportStatLabels(
            primaryShort = "PTS", primaryFull = "Points",
            secondaryShort = "REB", secondaryFull = "Rebounds",
            tertiaryShort = "AST", tertiaryFull = "Assists",
            defenseShort = "STL+BLK", defenseFull = "Stocks"
        )
        SportType.FOOTBALL -> SportStatLabels(
            primaryShort = "PTS", primaryFull = "Scoring Impact",
            secondaryShort = "YDS", secondaryFull = "Scrimmage Yards",
            tertiaryShort = "TD", tertiaryFull = "Touchdowns/TKL",
            defenseShort = "SACK/PD", defenseFull = "Big Plays"
        )
        SportType.SOCCER -> SportStatLabels(
            primaryShort = "PTS", primaryFull = "Goal Contributions",
            secondaryShort = "SHOT", secondaryFull = "Shots / Clearances",
            tertiaryShort = "AST", tertiaryFull = "Key Passes / Assists",
            defenseShort = "INT+SV", defenseFull = "Defensive Actions"
        )
        SportType.HOCKEY -> SportStatLabels(
            primaryShort = "PTS", primaryFull = "Scoring Points",
            secondaryShort = "SOG", secondaryFull = "Shots / Saves",
            tertiaryShort = "AST", tertiaryFull = "Assists",
            defenseShort = "HIT+BLK", defenseFull = "Hits & Blocks"
        )
    }

    fun generateMatchPlayerStats(
        match: GameMatch,
        home: Franchise,
        away: Franchise,
        homeScore: Int,
        awayScore: Int,
        sportType: SportType,
        seasonYear: Int,
        isPlayoffs: Boolean,
        random: Random = Random.Default,
        defenseStatMultiplier: Double = 1.0,
        reboundStatMultiplier: Double = 1.0
    ): List<PlayerGameStatLine> {
        val homeRoster = home.roster.ifEmpty {
            TradeEngine.generateDefaultRosterForFranchise(home.id, home.cityName, sportType, home.rosterRating)
        }
        val awayRoster = away.roster.ifEmpty {
            TradeEngine.generateDefaultRosterForFranchise(away.id, away.cityName, sportType, away.rosterRating)
        }

        val homeWon = homeScore > awayScore
        val homeLines = generateTeamBoxScore(
            roster = homeRoster,
            franchise = home,
            opponent = away,
            teamScore = homeScore,
            teamWon = homeWon,
            match = match,
            sportType = sportType,
            seasonYear = seasonYear,
            isPlayoffs = isPlayoffs,
            random = random,
            defenseStatMultiplier = defenseStatMultiplier,
            reboundStatMultiplier = reboundStatMultiplier
        )
        val awayLines = generateTeamBoxScore(
            roster = awayRoster,
            franchise = away,
            opponent = home,
            teamScore = awayScore,
            teamWon = !homeWon,
            match = match,
            sportType = sportType,
            seasonYear = seasonYear,
            isPlayoffs = isPlayoffs,
            random = random,
            defenseStatMultiplier = defenseStatMultiplier,
            reboundStatMultiplier = reboundStatMultiplier
        )
        return homeLines + awayLines
    }

    private fun generateTeamBoxScore(
        roster: List<RosterPlayer>,
        franchise: Franchise,
        opponent: Franchise,
        teamScore: Int,
        teamWon: Boolean,
        match: GameMatch,
        sportType: SportType,
        seasonYear: Int,
        isPlayoffs: Boolean,
        random: Random,
        defenseStatMultiplier: Double = 1.0,
        reboundStatMultiplier: Double = 1.0
    ): List<PlayerGameStatLine> {
        if (roster.isEmpty()) return emptyList()
        val labels = getStatLabels(sportType)

        // Exclude medically OUT / IR / suspended players from active game minutes; sort by Depth Chart order
        val activeRoster = roster
            .filter { !it.isUnavailableForGame }
            .ifEmpty { roster.take(5) }
            .sortedWith(compareByDescending<RosterPlayer> { it.isStarter }.thenBy { it.depthOrder }.thenByDescending { it.overallRating })
            .take(7)

        // Compute usage weights from effective OVR + starter depth priority + injury penalty
        val weights = activeRoster.mapIndexed { idx, p ->
            val ovrFactor = (p.effectiveOverallRating - 62).coerceAtLeast(4).toDouble()
            val roleBonus = when {
                p.isStarter && idx == 0 -> 1.38
                p.isStarter && idx == 1 -> 1.18
                p.isStarter -> 1.05
                else -> 0.52 // Bench rotation gets secondary usage
            }
            val injuryScale = p.statProductionMultiplier
            val hotHand = if (!p.isInjured && random.nextInt(100) < 18) 1.32 else random.nextDouble(0.80, 1.16)
            (ovrFactor * roleBonus * injuryScale * hotHand).coerceAtLeast(1.0)
        }
        val totalWeight = weights.sum().coerceAtLeast(1.0)

        // Starters + active rotation produce team scoring budget
        val starterScoringBudget = when (sportType) {
            SportType.BASKETBALL -> (teamScore * 0.92).roundToInt().coerceAtLeast(activeRoster.size * 5)
            SportType.FOOTBALL -> teamScore
            SportType.SOCCER -> teamScore
            SportType.HOCKEY -> teamScore
        }

        val allocatedPoints = IntArray(activeRoster.size)
        if (sportType == SportType.BASKETBALL) {
            var remaining = starterScoringBudget
            for (i in activeRoster.indices) {
                val player = activeRoster[i]
                val share = ((weights[i] / totalWeight) * starterScoringBudget).roundToInt()
                    .coerceIn(3, remaining.coerceAtLeast(3))
                val maxCap = if (player.isInjured) 30 else 56
                val pts = if (i == activeRoster.lastIndex) remaining.coerceIn(2, maxCap) else share.coerceAtMost(maxCap)
                allocatedPoints[i] = pts
                remaining = (remaining - pts).coerceAtLeast(0)
            }
            // Healthy star can erupt in high-scoring wins
            if (teamScore >= 118 && weights.isNotEmpty()) {
                val topIdx = weights.indices.maxByOrNull { weights[it] } ?: 0
                if (!activeRoster[topIdx].isInjured && allocatedPoints[topIdx] < 36) {
                    allocatedPoints[topIdx] = (allocatedPoints[topIdx] + 8).coerceAtMost(teamScore - 20)
                }
            }
        } else {
            val primaryEvents = when (sportType) {
                SportType.FOOTBALL -> (teamScore / 6).coerceAtLeast(1)
                else -> teamScore
            }
            for (i in activeRoster.indices) {
                allocatedPoints[i] = when (sportType) {
                    SportType.FOOTBALL -> {
                        val tdShare = ((weights[i] / totalWeight) * primaryEvents).roundToInt()
                        val bonusPts = if (i == 0 && !activeRoster[i].isInjured) 6 else 0
                        (tdShare * 6 + bonusPts).coerceIn(0, maxOf(6, teamScore))
                    }
                    SportType.SOCCER, SportType.HOCKEY -> {
                        val g = if (random.nextDouble() < (weights[i] / totalWeight) * 1.4 && primaryEvents > 0) 1 else 0
                        val extra = if (i == 0 && !activeRoster[i].isInjured && primaryEvents >= 2 && random.nextBoolean()) 1 else 0
                        (g + extra).coerceAtMost(maxOf(1, primaryEvents))
                    }
                    else -> 0
                }
            }
        }

        return activeRoster.mapIndexed { idx, player ->
            val ovrBonus = ((player.effectiveOverallRating - 75).coerceAtLeast(0)) / 3
            val playoffIntensity = if (isPlayoffs) 2 else 0
            val benchMinutesPenalty = if (!player.isStarter) -10 else 0
            val injScale = player.statProductionMultiplier
            val minutes = when (sportType) {
                SportType.BASKETBALL -> ((24 + ovrBonus * 2 + playoffIntensity + benchMinutesPenalty + random.nextInt(0, 8)) * injScale).roundToInt().coerceIn(10, 44)
                SportType.FOOTBALL -> ((38 + ovrBonus * 2 + benchMinutesPenalty + random.nextInt(0, 18)) * injScale).roundToInt().coerceIn(14, 60)
                SportType.SOCCER -> ((68 + ovrBonus * 3 + (benchMinutesPenalty * 2) + random.nextInt(0, 22)) * injScale).roundToInt().coerceIn(18, 90)
                SportType.HOCKEY -> ((15 + ovrBonus + (benchMinutesPenalty / 2) + random.nextInt(0, 8)) * injScale).roundToInt().coerceIn(6, 27)
            }

            val pts = allocatedPoints[idx]
            val rebounds = when (sportType) {
                SportType.BASKETBALL -> {
                    val posBias = when (player.position) {
                        "C" -> 6
                        "PF" -> 5
                        "SF" -> 3
                        else -> 1
                    }
                    ((2 + posBias + ovrBonus / 2 + random.nextInt(0, 6)) * injScale * reboundStatMultiplier).roundToInt().coerceIn(0, 26)
                }
                SportType.FOOTBALL -> {
                    val posBase = when (player.position) {
                        "QB" -> 210
                        "RB", "WR" -> 75
                        else -> 25
                    }
                    ((posBase + ovrBonus * 14 + random.nextInt(0, 85)) * injScale * reboundStatMultiplier).roundToInt().coerceIn(5, 520)
                }
                SportType.SOCCER -> ((2 + ovrBonus + random.nextInt(0, 5)) * injScale * reboundStatMultiplier).roundToInt().coerceIn(0, 18)
                SportType.HOCKEY -> ((2 + ovrBonus + random.nextInt(0, 6)) * injScale * reboundStatMultiplier).roundToInt().coerceIn(0, 20)
            }

            val assists = when (sportType) {
                SportType.BASKETBALL -> {
                    val posBias = when (player.position) {
                        "PG" -> 5
                        "SG" -> 3
                        "SF" -> 2
                        else -> 1
                    }
                    ((1 + posBias + ovrBonus / 2 + random.nextInt(0, 5)) * injScale).roundToInt().coerceIn(0, 17)
                }
                SportType.FOOTBALL -> ((1 + (pts / 7) + random.nextInt(0, 3)) * injScale).roundToInt().coerceIn(0, 6)
                SportType.SOCCER -> ((random.nextInt(0, 3) + if (player.position in listOf("CAM", "WNG", "ST")) 1 else 0) * injScale).roundToInt().coerceIn(0, 4)
                SportType.HOCKEY -> ((random.nextInt(0, 3) + if (ovrBonus >= 3) 1 else 0) * injScale).roundToInt().coerceIn(0, 4)
            }

            val stealsBlocks = when (sportType) {
                SportType.BASKETBALL -> ((random.nextInt(0, 4) + if (player.effectiveOverallRating >= 85) 1 else 0) * injScale * defenseStatMultiplier).roundToInt().coerceIn(0, 10)
                SportType.FOOTBALL -> ((1 + random.nextInt(0, 4)) * injScale * defenseStatMultiplier).roundToInt().coerceIn(0, 9)
                SportType.SOCCER -> ((1 + random.nextInt(0, 5)) * injScale * defenseStatMultiplier).roundToInt().coerceIn(0, 11)
                SportType.HOCKEY -> ((2 + random.nextInt(0, 5)) * injScale * defenseStatMultiplier).roundToInt().coerceIn(0, 12)
            }

            val efficiency = when (sportType) {
                SportType.BASKETBALL -> pts + (rebounds * 1.2).roundToInt() + (assists * 1.5).roundToInt() + (stealsBlocks * 2) + if (teamWon) 5 else 0
                SportType.FOOTBALL -> (pts * 2) + (rebounds / 12) + (assists * 4) + (stealsBlocks * 3) + if (teamWon) 6 else 0
                else -> (pts * 10) + (assists * 7) + (rebounds * 2) + (stealsBlocks * 2) + if (teamWon) 6 else 0
            }

            val statSummary = when (sportType) {
                SportType.BASKETBALL -> "$pts ${labels.primaryShort} • $rebounds ${labels.secondaryShort} • $assists ${labels.tertiaryShort} • $stealsBlocks ${labels.defenseShort}"
                SportType.FOOTBALL -> "$pts ${labels.primaryShort} • $rebounds ${labels.secondaryShort} • $assists ${labels.tertiaryShort}"
                else -> "$pts ${labels.primaryShort} • $assists ${labels.tertiaryShort} • $rebounds ${labels.secondaryShort}"
            }

            PlayerGameStatLine(
                playerId = player.id,
                playerName = player.name,
                franchiseId = franchise.id,
                franchiseName = franchise.teamFullName,
                position = player.position,
                week = match.week,
                seasonYear = seasonYear,
                isPlayoffs = isPlayoffs,
                roundName = match.roundName,
                opponentId = opponent.id,
                opponentName = opponent.teamFullName,
                teamWon = teamWon,
                points = pts,
                rebounds = rebounds,
                assists = assists,
                stealsBlocks = stealsBlocks,
                minutes = minutes,
                efficiencyScore = efficiency,
                statSummary = statSummary
            )
        }
    }

    fun selectPlayerOfTheWeek(
        week: Int,
        seasonYear: Int,
        weekStats: List<PlayerGameStatLine>,
        isPlayoffs: Boolean
    ): WeeklyPlayerAward? {
        if (weekStats.isEmpty()) return null
        val winners = weekStats.filter { it.teamWon }
        val pool = winners.ifEmpty { weekStats }
        val mvpLine = pool.maxByOrNull { it.efficiencyScore } ?: return null

        return WeeklyPlayerAward(
            week = week,
            seasonYear = seasonYear,
            playerId = mvpLine.playerId,
            playerName = mvpLine.playerName,
            franchiseId = mvpLine.franchiseId,
            franchiseName = mvpLine.franchiseName,
            position = mvpLine.position,
            statLineSummary = mvpLine.statSummary,
            points = mvpLine.points,
            rebounds = mvpLine.rebounds,
            assists = mvpLine.assists,
            impactScore = mvpLine.efficiencyScore,
            opponentName = mvpLine.opponentName,
            isPlayoffs = isPlayoffs
        )
    }

    fun seedInitialCareerStats(franchises: List<Franchise>, currentYear: Int): Map<String, PlayerCareerStats> {
        val map = mutableMapOf<String, PlayerCareerStats>()
        franchises.forEach { team ->
            team.roster.forEachIndexed { idx, player ->
                val expYears = (player.age - 20).coerceIn(1, 13)
                val estGames = expYears * 14
                val avgPts = when {
                    player.overallRating >= 88 -> 23.4
                    player.overallRating >= 83 -> 17.8
                    player.overallRating >= 78 -> 13.5
                    else -> 9.8
                }
                val avgReb = if (player.position in listOf("C", "PF", "RB", "EDGE", "CB", "D")) 8.2 else 4.5
                val avgAst = if (player.position in listOf("PG", "QB", "CAM", "WNG")) 7.4 else 3.2
                val totalPts = (estGames * avgPts).roundToInt()
                val totalReb = (estGames * avgReb).roundToInt()
                val totalAst = (estGames * avgAst).roundToInt()
                val totalDef = (estGames * 1.9).roundToInt()

                map[player.id] = PlayerCareerStats(
                    playerId = player.id,
                    playerName = player.name,
                    primaryFranchiseId = team.id,
                    primaryFranchiseName = team.teamFullName,
                    position = player.position,
                    seasonsPlayed = expYears,
                    gamesPlayed = estGames,
                    totalPoints = totalPts,
                    totalRebounds = totalReb,
                    totalAssists = totalAst,
                    totalStealsBlocks = totalDef,
                    playoffPoints = (totalPts * 0.14).roundToInt(),
                    championships = team.championships.coerceAtMost(expYears),
                    weeklyHonorsCount = if (player.overallRating >= 86) expYears / 2 else 0,
                    bestGamePoints = (avgPts * 1.8).roundToInt(),
                    peakRating = player.overallRating,
                    isRetired = false
                )
            }
        }
        return map
    }

    fun createInitialRecordBook(
        sportType: SportType,
        franchises: List<Franchise>,
        year: Int
    ): LeagueRecordBook {
        val labels = getStatLabels(sportType)
        val teamA = franchises.getOrNull(0)?.teamFullName ?: "New York Monarchs"
        val teamB = franchises.getOrNull(1)?.teamFullName ?: "Los Angeles Waves"
        val teamC = franchises.getOrNull(2)?.teamFullName ?: "Chicago Steel"
        val teamD = franchises.getOrNull(3)?.teamFullName ?: "London Royals"

        val (gPts, sPts, cPts, fgPts, fgMargin) = when (sportType) {
            SportType.BASKETBALL -> listOf(38, 340, 4200, 128, 22)
            SportType.FOOTBALL -> listOf(24, 210, 2400, 41, 21)
            SportType.SOCCER -> listOf(3, 18, 195, 5, 4)
            SportType.HOCKEY -> listOf(3, 21, 240, 7, 4)
        }

        val playerGameRecords = listOf(
            RecordEntry(
                id = "rec_pg_points",
                category = RecordCategory.PLAYER_GAME,
                recordTitle = "Most ${labels.primaryFull} (Single Game)",
                holderName = "Devon Alston",
                teamName = teamA,
                valueDisplay = "$gPts ${labels.primaryShort}",
                numericValue = gPts.toDouble(),
                contextLabel = "${year - 2} Charter Era",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_pg_rebounds",
                category = RecordCategory.PLAYER_GAME,
                recordTitle = "Most ${labels.secondaryFull} (Single Game)",
                holderName = "Kieran Vance",
                teamName = teamC,
                valueDisplay = "${if (sportType == SportType.FOOTBALL) 285 else 14} ${labels.secondaryShort}",
                numericValue = if (sportType == SportType.FOOTBALL) 285.0 else 14.0,
                contextLabel = "${year - 3} Regular Season",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_pg_assists",
                category = RecordCategory.PLAYER_GAME,
                recordTitle = "Most ${labels.tertiaryFull} (Single Game)",
                holderName = "Julian Sterling",
                teamName = teamB,
                valueDisplay = "${if (sportType == SportType.BASKETBALL) 12 else 4} ${labels.tertiaryShort}",
                numericValue = if (sportType == SportType.BASKETBALL) 12.0 else 4.0,
                contextLabel = "${year - 1} Playoffs",
                isActiveEraRecord = false,
                isPlayoffRecord = true
            ),
            RecordEntry(
                id = "rec_pg_impact",
                category = RecordCategory.PLAYER_GAME,
                recordTitle = "Highest Single-Game Impact Rating",
                holderName = "Marcus Thorne",
                teamName = teamD,
                valueDisplay = "54 EFF",
                numericValue = 54.0,
                contextLabel = "${year - 2} Showcase",
                isActiveEraRecord = false
            )
        )

        val playerSeasonRecords = listOf(
            RecordEntry(
                id = "rec_ps_points",
                category = RecordCategory.PLAYER_SEASON,
                recordTitle = "Most ${labels.primaryFull} (Single Season)",
                holderName = "Devon Alston",
                teamName = teamA,
                valueDisplay = "$sPts ${labels.primaryShort}",
                numericValue = sPts.toDouble(),
                contextLabel = "Season ${year - 2}",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_ps_ppg",
                category = RecordCategory.PLAYER_SEASON,
                recordTitle = "Highest Season ${labels.primaryShort} Per Game",
                holderName = "Rafael Cruz",
                teamName = teamB,
                valueDisplay = "${if (sportType == SportType.BASKETBALL) "26.5" else "2.1"} ${labels.primaryShort}/G",
                numericValue = if (sportType == SportType.BASKETBALL) 26.5 else 2.1,
                contextLabel = "Season ${year - 1}",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_ps_assists",
                category = RecordCategory.PLAYER_SEASON,
                recordTitle = "Most ${labels.tertiaryFull} (Single Season)",
                holderName = "Julian Sterling",
                teamName = teamB,
                valueDisplay = "${if (sportType == SportType.BASKETBALL) 118 else 28} ${labels.tertiaryShort}",
                numericValue = if (sportType == SportType.BASKETBALL) 118.0 else 28.0,
                contextLabel = "Season ${year - 1}",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_ps_honors",
                category = RecordCategory.PLAYER_SEASON,
                recordTitle = "Most Player of the Week Honors (Season)",
                holderName = "Marcus Thorne",
                teamName = teamD,
                valueDisplay = "3 Honors",
                numericValue = 3.0,
                contextLabel = "Season ${year - 2}",
                isActiveEraRecord = false
            )
        )

        val playerCareerRecords = listOf(
            RecordEntry(
                id = "rec_pc_points",
                category = RecordCategory.PLAYER_CAREER,
                recordTitle = "All-Time Career ${labels.primaryFull}",
                holderName = "Arthur Pendelton",
                teamName = teamA,
                valueDisplay = "$cPts ${labels.primaryShort}",
                numericValue = cPts.toDouble(),
                contextLabel = "${year - 12}–${year - 1} Career",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_pc_assists",
                category = RecordCategory.PLAYER_CAREER,
                recordTitle = "All-Time Career ${labels.tertiaryFull}",
                holderName = "Julian Sterling",
                teamName = teamB,
                valueDisplay = "${if (sportType == SportType.BASKETBALL) 1320 else 310} ${labels.tertiaryShort}",
                numericValue = if (sportType == SportType.BASKETBALL) 1320.0 else 310.0,
                contextLabel = "${year - 10}–${year - 1} Career",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_pc_rebounds",
                category = RecordCategory.PLAYER_CAREER,
                recordTitle = "All-Time Career ${labels.secondaryFull}",
                holderName = "Victor Kowalski",
                teamName = teamC,
                valueDisplay = "${if (sportType == SportType.FOOTBALL) 12400 else 1680} ${labels.secondaryShort}",
                numericValue = if (sportType == SportType.FOOTBALL) 12400.0 else 1680.0,
                contextLabel = "${year - 11}–${year - 2} Career",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_pc_games",
                category = RecordCategory.PLAYER_CAREER,
                recordTitle = "Most Career Games Played",
                holderName = "Arthur Pendelton",
                teamName = teamA,
                valueDisplay = "196 Games",
                numericValue = 196.0,
                contextLabel = "${year - 12}–${year - 1} Ironman",
                isActiveEraRecord = false
            )
        )

        val franchiseGameRecords = listOf(
            RecordEntry(
                id = "rec_fg_points",
                category = RecordCategory.FRANCHISE_GAME,
                recordTitle = "Most Team Points Scored (Single Game)",
                holderName = teamA,
                teamName = teamA,
                valueDisplay = "$fgPts PTS",
                numericValue = fgPts.toDouble(),
                contextLabel = "${year - 2} Regular Season",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_fg_margin",
                category = RecordCategory.FRANCHISE_GAME,
                recordTitle = "Largest Margin of Victory",
                holderName = teamB,
                teamName = teamB,
                valueDisplay = "+$fgMargin PTS",
                numericValue = fgMargin.toDouble(),
                contextLabel = "${year - 1} vs $teamC",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_fg_combined",
                category = RecordCategory.FRANCHISE_GAME,
                recordTitle = "Highest Combined Score (Both Teams)",
                holderName = "$teamA vs $teamB",
                teamName = teamA,
                valueDisplay = "${(fgPts * 1.9).roundToInt()} PTS",
                numericValue = (fgPts * 1.9).roundToInt().toDouble(),
                contextLabel = "${year - 1} Thriller",
                isActiveEraRecord = false
            )
        )

        val franchiseSeasonRecords = listOf(
            RecordEntry(
                id = "rec_fs_wins",
                category = RecordCategory.FRANCHISE_SEASON,
                recordTitle = "Most Franchise Wins (Season)",
                holderName = teamA,
                teamName = teamA,
                valueDisplay = "12 Wins",
                numericValue = 12.0,
                contextLabel = "Season ${year - 1}",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_fs_points",
                category = RecordCategory.FRANCHISE_SEASON,
                recordTitle = "Highest Franchise Scoring Output (Season)",
                holderName = teamB,
                teamName = teamB,
                valueDisplay = "${fgPts * 12} PTS",
                numericValue = (fgPts * 12).toDouble(),
                contextLabel = "Season ${year - 2}",
                isActiveEraRecord = false
            ),
            RecordEntry(
                id = "rec_fs_diff",
                category = RecordCategory.FRANCHISE_SEASON,
                recordTitle = "Best Season Point Differential",
                holderName = teamD,
                teamName = teamD,
                valueDisplay = "+${fgMargin * 4} DIFF",
                numericValue = (fgMargin * 4).toDouble(),
                contextLabel = "Season ${year - 1}",
                isActiveEraRecord = false
            )
        )

        return LeagueRecordBook(
            playerGameRecords = playerGameRecords,
            playerSeasonRecords = playerSeasonRecords,
            playerCareerRecords = playerCareerRecords,
            franchiseGameRecords = franchiseGameRecords,
            franchiseSeasonRecords = franchiseSeasonRecords
        )
    }

    fun createInauguralHallOfFame(
        sportType: SportType,
        franchises: List<Franchise>,
        currentYear: Int
    ): List<HallOfFameInductee> {
        val posList = listOf(
            sportType.starterPositions[0],
            sportType.starterPositions[4],
            sportType.starterPositions[2],
            sportType.starterPositions[1]
        )
        val t0 = franchises.getOrNull(0)
        val t1 = franchises.getOrNull(1)
        val t2 = franchises.getOrNull(2)
        val t3 = franchises.getOrNull(3)

        return listOf(
            HallOfFameInductee(
                id = "hof_founder_pendelton",
                playerId = "legend_pendelton",
                name = "Arthur Pendelton",
                inductionYear = currentYear - 2,
                position = posList[0],
                primaryFranchiseId = t0?.id ?: "team_ny",
                primaryFranchiseName = t0?.teamFullName ?: "New York Monarchs",
                careerPoints = if (sportType == SportType.BASKETBALL) 4200 else 285,
                careerRebounds = if (sportType == SportType.BASKETBALL) 980 else 410,
                careerAssists = if (sportType == SportType.BASKETBALL) 1210 else 190,
                gamesPlayed = 196,
                championships = 3,
                weeklyHonors = 18,
                peakRating = 96,
                legacyTier = "Unanimous Charter Immortal",
                bio = "Founding-era floor general who led three title runs and anchored the league's golden broadcast expansion.",
                inductionQuote = "Every banner hanging in our arena belongs to the fans who believed on Day One."
            ),
            HallOfFameInductee(
                id = "hof_founder_kowalski",
                playerId = "legend_kowalski",
                name = "Victor Kowalski",
                inductionYear = currentYear - 3,
                position = posList[1],
                primaryFranchiseId = t2?.id ?: "team_chi",
                primaryFranchiseName = t2?.teamFullName ?: "Chicago Steel",
                careerPoints = if (sportType == SportType.BASKETBALL) 3650 else 210,
                careerRebounds = if (sportType == SportType.BASKETBALL) 1680 else 560,
                careerAssists = if (sportType == SportType.BASKETBALL) 540 else 95,
                gamesPlayed = 184,
                championships = 2,
                weeklyHonors = 14,
                peakRating = 94,
                legacyTier = "Defensive Anchor Immortal",
                bio = "Relentless interior guardian whose lockdown defense defined championship toughness across a decade.",
                inductionQuote = "Championships are forged when nobody wants to cross the paint in the fourth."
            ),
            HallOfFameInductee(
                id = "hof_founder_alston",
                playerId = "legend_alston",
                name = "Devon Alston",
                inductionYear = currentYear - 1,
                position = posList[2],
                primaryFranchiseId = t1?.id ?: "team_la",
                primaryFranchiseName = t1?.teamFullName ?: "Los Angeles Waves",
                careerPoints = if (sportType == SportType.BASKETBALL) 3940 else 245,
                careerRebounds = if (sportType == SportType.BASKETBALL) 1140 else 390,
                careerAssists = if (sportType == SportType.BASKETBALL) 780 else 160,
                gamesPlayed = 172,
                championships = 2,
                weeklyHonors = 16,
                peakRating = 95,
                legacyTier = "First-Ballot Scoring Legend",
                bio = "Electric primetime scorer whose iconic postseason performances shattered broadcast viewership records.",
                inductionQuote = "When the bright lights came on across the coast, we never blinked once."
            ),
            HallOfFameInductee(
                id = "hof_founder_thorne",
                playerId = "legend_thorne",
                name = "Marcus Thorne",
                inductionYear = currentYear - 1,
                position = posList[3],
                primaryFranchiseId = t3?.id ?: "team_lon",
                primaryFranchiseName = t3?.teamFullName ?: "London Royals",
                careerPoints = if (sportType == SportType.BASKETBALL) 3490 else 220,
                careerRebounds = if (sportType == SportType.BASKETBALL) 890 else 340,
                careerAssists = if (sportType == SportType.BASKETBALL) 950 else 175,
                gamesPlayed = 168,
                championships = 1,
                weeklyHonors = 15,
                peakRating = 93,
                legacyTier = "Global Pioneer Inductee",
                bio = "Transatlantic trailblazer who proved international franchises could capture premier league dominance.",
                inductionQuote = "This sport knows no borders—only heart, preparation, and respect for the crown."
            )
        )
    }

    fun updateRecordsFromWeek(
        currentBook: LeagueRecordBook,
        weekStats: List<PlayerGameStatLine>,
        playedMatches: List<GameMatch>,
        updatedFranchises: List<Franchise>,
        updatedSeasonStats: Map<String, PlayerSeasonStats>,
        updatedCareerStats: Map<String, PlayerCareerStats>,
        sportType: SportType,
        seasonYear: Int,
        week: Int,
        isPlayoffs: Boolean
    ): Pair<LeagueRecordBook, List<String>> {
        val labels = getStatLabels(sportType)
        val brokenAlerts = mutableListOf<String>()
        val eraTag = if (isPlayoffs) "$seasonYear Playoffs W$week" else "$seasonYear W$week"

        // 1. Player Game Records
        val newPlayerGameRecords = currentBook.playerGameRecords.map { rec ->
            when (rec.id) {
                "rec_pg_points" -> {
                    val best = weekStats.maxByOrNull { it.points }
                    if (best != null && best.points > rec.numericValue) {
                        brokenAlerts.add("NEW RECORD! ${best.playerName} (${best.franchiseName}) scored ${best.points} ${labels.primaryShort} vs ${best.opponentName}!")
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.points} ${labels.primaryShort}",
                            numericValue = best.points.toDouble(),
                            contextLabel = "$eraTag vs ${best.opponentName}",
                            isActiveEraRecord = true,
                            isPlayoffRecord = isPlayoffs
                        )
                    } else rec
                }
                "rec_pg_rebounds" -> {
                    val best = weekStats.maxByOrNull { it.rebounds }
                    if (best != null && best.rebounds > rec.numericValue) {
                        brokenAlerts.add("NEW RECORD! ${best.playerName} tallied ${best.rebounds} ${labels.secondaryShort} in a single game!")
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.rebounds} ${labels.secondaryShort}",
                            numericValue = best.rebounds.toDouble(),
                            contextLabel = "$eraTag vs ${best.opponentName}",
                            isActiveEraRecord = true,
                            isPlayoffRecord = isPlayoffs
                        )
                    } else rec
                }
                "rec_pg_assists" -> {
                    val best = weekStats.maxByOrNull { it.assists }
                    if (best != null && best.assists > rec.numericValue) {
                        brokenAlerts.add("NEW RECORD! ${best.playerName} dished ${best.assists} ${labels.tertiaryShort} in a single game!")
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.assists} ${labels.tertiaryShort}",
                            numericValue = best.assists.toDouble(),
                            contextLabel = "$eraTag vs ${best.opponentName}",
                            isActiveEraRecord = true,
                            isPlayoffRecord = isPlayoffs
                        )
                    } else rec
                }
                "rec_pg_impact" -> {
                    val best = weekStats.maxByOrNull { it.efficiencyScore }
                    if (best != null && best.efficiencyScore > rec.numericValue) {
                        brokenAlerts.add("NEW RECORD! ${best.playerName} posted a historic ${best.efficiencyScore} EFF rating!")
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.efficiencyScore} EFF",
                            numericValue = best.efficiencyScore.toDouble(),
                            contextLabel = "$eraTag vs ${best.opponentName}",
                            isActiveEraRecord = true,
                            isPlayoffRecord = isPlayoffs
                        )
                    } else rec
                }
                else -> rec
            }
        }

        // 2. Player Season Records
        val seasonValues = updatedSeasonStats.values.toList()
        val newPlayerSeasonRecords = currentBook.playerSeasonRecords.map { rec ->
            when (rec.id) {
                "rec_ps_points" -> {
                    val best = seasonValues.maxByOrNull { it.totalPoints }
                    if (best != null && best.totalPoints > rec.numericValue) {
                        if (!rec.isActiveEraRecord || best.playerName != rec.holderName) {
                            brokenAlerts.add("SEASON RECORD! ${best.playerName} sets new single-season mark with ${best.totalPoints} ${labels.primaryShort}!")
                        }
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.totalPoints} ${labels.primaryShort}",
                            numericValue = best.totalPoints.toDouble(),
                            contextLabel = "Season $seasonYear",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_ps_ppg" -> {
                    val qualified = seasonValues.filter { it.totalAppearances >= 4 }
                    val best = qualified.maxByOrNull { it.ppg }
                    if (best != null && best.ppg > rec.numericValue) {
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.ppg} ${labels.primaryShort}/G",
                            numericValue = best.ppg,
                            contextLabel = "Season $seasonYear (${best.totalAppearances}G)",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_ps_assists" -> {
                    val best = seasonValues.maxByOrNull { it.totalAssists }
                    if (best != null && best.totalAssists > rec.numericValue) {
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.totalAssists} ${labels.tertiaryShort}",
                            numericValue = best.totalAssists.toDouble(),
                            contextLabel = "Season $seasonYear",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_ps_honors" -> {
                    val best = seasonValues.maxByOrNull { it.weeklyHonorsCount }
                    if (best != null && best.weeklyHonorsCount > rec.numericValue) {
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.franchiseName,
                            valueDisplay = "${best.weeklyHonorsCount} Honors",
                            numericValue = best.weeklyHonorsCount.toDouble(),
                            contextLabel = "Season $seasonYear",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                else -> rec
            }
        }

        // 3. Player Career Records
        val careerValues = updatedCareerStats.values.toList()
        val newPlayerCareerRecords = currentBook.playerCareerRecords.map { rec ->
            when (rec.id) {
                "rec_pc_points" -> {
                    val best = careerValues.maxByOrNull { it.totalPoints }
                    if (best != null && best.totalPoints > rec.numericValue) {
                        brokenAlerts.add("ALL-TIME CAREER RECORD! ${best.playerName} becomes all-time scoring king (${best.totalPoints} ${labels.primaryShort})!")
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.primaryFranchiseName,
                            valueDisplay = "${best.totalPoints} ${labels.primaryShort}",
                            numericValue = best.totalPoints.toDouble(),
                            contextLabel = "Active Career ($seasonYear)",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_pc_assists" -> {
                    val best = careerValues.maxByOrNull { it.totalAssists }
                    if (best != null && best.totalAssists > rec.numericValue) {
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.primaryFranchiseName,
                            valueDisplay = "${best.totalAssists} ${labels.tertiaryShort}",
                            numericValue = best.totalAssists.toDouble(),
                            contextLabel = "Active Career ($seasonYear)",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_pc_rebounds" -> {
                    val best = careerValues.maxByOrNull { it.totalRebounds }
                    if (best != null && best.totalRebounds > rec.numericValue) {
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.primaryFranchiseName,
                            valueDisplay = "${best.totalRebounds} ${labels.secondaryShort}",
                            numericValue = best.totalRebounds.toDouble(),
                            contextLabel = "Active Career ($seasonYear)",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_pc_games" -> {
                    val best = careerValues.maxByOrNull { it.gamesPlayed }
                    if (best != null && best.gamesPlayed > rec.numericValue) {
                        rec.copy(
                            holderName = best.playerName,
                            teamName = best.primaryFranchiseName,
                            valueDisplay = "${best.gamesPlayed} Games",
                            numericValue = best.gamesPlayed.toDouble(),
                            contextLabel = "Active Career ($seasonYear)",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                else -> rec
            }
        }

        // 4. Franchise Single-Game Records
        val newFranchiseGameRecords = currentBook.franchiseGameRecords.map { rec ->
            when (rec.id) {
                "rec_fg_points" -> {
                    var bestTeamName = rec.holderName
                    var bestOpponent = ""
                    var bestScore = rec.numericValue.toInt()
                    var broken = false
                    playedMatches.forEach { m ->
                        val home = updatedFranchises.find { it.id == m.homeTeamId }
                        val away = updatedFranchises.find { it.id == m.awayTeamId }
                        if (home != null && m.homeScore > bestScore) {
                            bestScore = m.homeScore
                            bestTeamName = home.teamFullName
                            bestOpponent = away?.teamFullName ?: ""
                            broken = true
                        }
                        if (away != null && m.awayScore > bestScore) {
                            bestScore = m.awayScore
                            bestTeamName = away.teamFullName
                            bestOpponent = home?.teamFullName ?: ""
                            broken = true
                        }
                    }
                    if (broken) {
                        brokenAlerts.add("FRANCHISE RECORD! $bestTeamName erupted for $bestScore PTS vs $bestOpponent!")
                        rec.copy(
                            holderName = bestTeamName,
                            teamName = bestTeamName,
                            valueDisplay = "$bestScore PTS",
                            numericValue = bestScore.toDouble(),
                            contextLabel = "$eraTag vs $bestOpponent",
                            isActiveEraRecord = true,
                            isPlayoffRecord = isPlayoffs
                        )
                    } else rec
                }
                "rec_fg_margin" -> {
                    var bestTeam = rec.holderName
                    var bestLoser = ""
                    var bestMargin = rec.numericValue.toInt()
                    var broken = false
                    playedMatches.forEach { m ->
                        val margin = abs(m.homeScore - m.awayScore)
                        if (margin > bestMargin) {
                            val home = updatedFranchises.find { it.id == m.homeTeamId }
                            val away = updatedFranchises.find { it.id == m.awayTeamId }
                            val winner = if (m.homeScore > m.awayScore) home else away
                            val loser = if (m.homeScore > m.awayScore) away else home
                            if (winner != null) {
                                bestMargin = margin
                                bestTeam = winner.teamFullName
                                bestLoser = loser?.teamFullName ?: ""
                                broken = true
                            }
                        }
                    }
                    if (broken) {
                        brokenAlerts.add("FRANCHISE BLOWOUT RECORD! $bestTeam crushed $bestLoser by +$bestMargin PTS!")
                        rec.copy(
                            holderName = bestTeam,
                            teamName = bestTeam,
                            valueDisplay = "+$bestMargin PTS",
                            numericValue = bestMargin.toDouble(),
                            contextLabel = "$eraTag vs $bestLoser",
                            isActiveEraRecord = true,
                            isPlayoffRecord = isPlayoffs
                        )
                    } else rec
                }
                "rec_fg_combined" -> {
                    var bestMatchup = rec.holderName
                    var bestPrimaryTeam = rec.teamName
                    var bestCombined = rec.numericValue.toInt()
                    var broken = false
                    playedMatches.forEach { m ->
                        val comb = m.homeScore + m.awayScore
                        if (comb > bestCombined) {
                            val home = updatedFranchises.find { it.id == m.homeTeamId }
                            val away = updatedFranchises.find { it.id == m.awayTeamId }
                            if (home != null && away != null) {
                                bestCombined = comb
                                bestMatchup = "${home.teamFullName} vs ${away.teamFullName}"
                                bestPrimaryTeam = home.teamFullName
                                broken = true
                            }
                        }
                    }
                    if (broken) {
                        rec.copy(
                            holderName = bestMatchup,
                            teamName = bestPrimaryTeam,
                            valueDisplay = "$bestCombined PTS",
                            numericValue = bestCombined.toDouble(),
                            contextLabel = eraTag,
                            isActiveEraRecord = true,
                            isPlayoffRecord = isPlayoffs
                        )
                    } else rec
                }
                else -> rec
            }
        }

        // 5. Franchise Season Records
        val newFranchiseSeasonRecords = currentBook.franchiseSeasonRecords.map { rec ->
            when (rec.id) {
                "rec_fs_wins" -> {
                    val best = updatedFranchises.maxByOrNull { it.wins }
                    if (best != null && best.wins > rec.numericValue) {
                        rec.copy(
                            holderName = best.teamFullName,
                            teamName = best.teamFullName,
                            valueDisplay = "${best.wins} Wins",
                            numericValue = best.wins.toDouble(),
                            contextLabel = "Season $seasonYear (${best.wins}-${best.losses})",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_fs_points" -> {
                    val best = updatedFranchises.maxByOrNull { it.pointsFor }
                    if (best != null && best.pointsFor > rec.numericValue) {
                        rec.copy(
                            holderName = best.teamFullName,
                            teamName = best.teamFullName,
                            valueDisplay = "${best.pointsFor} PTS",
                            numericValue = best.pointsFor.toDouble(),
                            contextLabel = "Season $seasonYear",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                "rec_fs_diff" -> {
                    val best = updatedFranchises.maxByOrNull { it.pointsFor - it.pointsAgainst }
                    val diff = if (best != null) best.pointsFor - best.pointsAgainst else 0
                    if (best != null && diff > rec.numericValue) {
                        rec.copy(
                            holderName = best.teamFullName,
                            teamName = best.teamFullName,
                            valueDisplay = "+$diff DIFF",
                            numericValue = diff.toDouble(),
                            contextLabel = "Season $seasonYear",
                            isActiveEraRecord = true
                        )
                    } else rec
                }
                else -> rec
            }
        }

        val updatedBook = currentBook.copy(
            playerGameRecords = newPlayerGameRecords,
            playerSeasonRecords = newPlayerSeasonRecords,
            playerCareerRecords = newPlayerCareerRecords,
            franchiseGameRecords = newFranchiseGameRecords,
            franchiseSeasonRecords = newFranchiseSeasonRecords,
            recentRecordBrokenAlerts = (brokenAlerts + currentBook.recentRecordBrokenAlerts).take(12)
        )
        return updatedBook to brokenAlerts
    }

    fun computeYearlyAwards(
        year: Int,
        sportType: SportType,
        franchises: List<Franchise>,
        seasonStats: Map<String, PlayerSeasonStats>,
        weeklyAwards: List<WeeklyPlayerAward>,
        champion: Franchise?
    ): YearlyAwardsCeremony {
        val labels = getStatLabels(sportType)
        val rosterPlayersById = franchises.flatMap { f -> f.roster.map { it.id to (it to f) } }.toMap()

        // Use real accumulated season stats; synthesize baseline stats for any unlogged roster player so gala always works cleanly
        val effectiveStats: List<PlayerSeasonStats> = if (seasonStats.isNotEmpty()) {
            seasonStats.values.toList()
        } else {
            franchises.flatMap { team ->
                team.roster.mapIndexed { idx, p ->
                    val gp = (team.wins + team.losses).coerceAtLeast(14)
                    val basePpg = when (sportType) {
                        SportType.BASKETBALL -> (p.overallRating - 62).coerceAtLeast(8).toDouble() + if (idx == 0) 4.0 else 0.0
                        SportType.FOOTBALL -> ((p.overallRating - 70).coerceAtLeast(4) * 0.65)
                        SportType.SOCCER -> ((p.overallRating - 70).coerceAtLeast(3) * 0.07)
                        SportType.HOCKEY -> ((p.overallRating - 70).coerceAtLeast(3) * 0.09)
                    }
                    val totalPts = (basePpg * gp).roundToInt()
                    val totalReb = (gp * (if (p.position in listOf("C", "PF", "RB", "EDGE", "CB", "D")) 9.2 else 4.8)).roundToInt()
                    val totalAst = (gp * (if (p.position in listOf("PG", "CAM", "QB", "WNG")) 7.6 else 3.4)).roundToInt()
                    val totalDef = (gp * (1.8 + (p.overallRating - 75).coerceAtLeast(0) * 0.14)).roundToInt()
                    PlayerSeasonStats(
                        playerId = p.id,
                        playerName = p.name,
                        franchiseId = team.id,
                        franchiseName = team.teamFullName,
                        position = p.position,
                        seasonYear = year,
                        gamesPlayed = gp,
                        playoffGamesPlayed = if (team.id == champion?.id) 2 else 0,
                        totalPoints = totalPts,
                        totalRebounds = totalReb,
                        totalAssists = totalAst,
                        totalStealsBlocks = totalDef,
                        playoffPoints = if (team.id == champion?.id) (basePpg * 2.2).roundToInt() else 0,
                        playoffRebounds = if (team.id == champion?.id) 18 else 0,
                        playoffAssists = if (team.id == champion?.id) 14 else 0,
                        weeklyHonorsCount = if (idx == 0 && p.overallRating >= 85) 2 else 0,
                        bestGamePoints = (basePpg * 1.55).roundToInt(),
                        overallRating = p.overallRating
                    )
                }
            }
        }

        val franchisesById = franchises.associateBy { it.id }

        fun mvpCompositeScore(s: PlayerSeasonStats): Double {
            val team = franchisesById[s.franchiseId]
            val winPct = team?.winPct ?: 0.500
            val potwBonus = (weeklyAwards.count { it.playerId == s.playerId && it.seasonYear == year }
                .coerceAtLeast(s.weeklyHonorsCount)) * 4.5
            return when (sportType) {
                SportType.BASKETBALL -> (s.ppg * 1.8) + (s.rpg * 1.05) + (s.apg * 1.35) + (s.spg * 2.1) + potwBonus + (winPct * 26.0)
                SportType.FOOTBALL -> (s.ppg * 2.2) + (s.rpg * 0.08) + (s.apg * 2.4) + (s.spg * 2.5) + potwBonus + (winPct * 28.0)
                else -> (s.ppg * 14.0) + (s.apg * 10.0) + (s.rpg * 2.0) + (s.spg * 3.0) + potwBonus + (winPct * 26.0)
            }
        }

        val rankedForMvp = effectiveStats
            .map { it to mvpCompositeScore(it) }
            .sortedByDescending { it.second }

        val top3 = rankedForMvp.take(3)
        val top3Sum = top3.sumOf { it.second }.coerceAtLeast(1.0)
        val finalists = top3.mapIndexed { idx, (stat, score) ->
            val rawPct = ((score / top3Sum) * 100.0).roundToInt()
            val adjustedPct = if (idx == 0) rawPct.coerceAtLeast(38) else rawPct
            MvpFinalist(
                playerId = stat.playerId,
                playerName = stat.playerName,
                position = stat.position,
                teamName = stat.franchiseName,
                mvpScore = (score * 10.0).roundToInt() / 10.0,
                voteSharePct = adjustedPct,
                statLine = "${stat.ppg} ${labels.primaryShort} • ${stat.rpg} ${labels.secondaryShort} • ${stat.apg} ${labels.tertiaryShort}"
            )
        }

        val awards = mutableListOf<YearlyAwardWinner>()

        // 1. League MVP
        rankedForMvp.firstOrNull()?.let { (mvp, score) ->
            val team = franchisesById[mvp.franchiseId]
            val recordText = if (team != null) "${team.wins}-${team.losses}" else "Contender"
            awards.add(
                YearlyAwardWinner(
                    id = "award_${year}_mvp",
                    year = year,
                    awardType = AnnualAwardType.LEAGUE_MVP,
                    winnerName = mvp.playerName,
                    position = mvp.position,
                    franchiseId = mvp.franchiseId,
                    franchiseName = mvp.franchiseName,
                    primaryStatLine = "${mvp.ppg} ${labels.primaryShort}/G • ${mvp.rpg} ${labels.secondaryShort}/G • ${mvp.apg} ${labels.tertiaryShort}/G (${mvp.totalPoints} Total ${labels.primaryShort})",
                    supportingNarrative = "Anchored ${mvp.franchiseName} ($recordText) with ${mvp.weeklyHonorsCount} Weekly Honor(s) and dominant all-around production.",
                    scoreValue = score
                )
            )
        }

        // 2. Finals / Postseason MVP
        val playoffCandidates = effectiveStats
            .filter { champion == null || it.franchiseId == champion.id }
            .ifEmpty { effectiveStats }
        val finalsMvp = playoffCandidates.maxByOrNull {
            (it.playoffPoints * 1.5) + (it.playoffAssists * 1.4) + (it.playoffRebounds * 1.1) + it.ppg
        }
        if (finalsMvp != null) {
            val poPts = if (finalsMvp.playoffPoints > 0) finalsMvp.playoffPoints else finalsMvp.bestGamePoints
            val poGames = finalsMvp.playoffGamesPlayed.coerceAtLeast(1)
            val poPpg = ((poPts.toDouble() / poGames) * 10.0).roundToInt() / 10.0
            awards.add(
                YearlyAwardWinner(
                    id = "award_${year}_fmvp",
                    year = year,
                    awardType = AnnualAwardType.FINALS_MVP,
                    winnerName = finalsMvp.playerName,
                    position = finalsMvp.position,
                    franchiseId = finalsMvp.franchiseId,
                    franchiseName = champion?.teamFullName ?: finalsMvp.franchiseName,
                    primaryStatLine = if (finalsMvp.playoffPoints > 0) {
                        "$poPpg ${labels.primaryShort}/G in Playoffs ($poPts ${labels.primaryShort}, ${finalsMvp.playoffAssists} ${labels.tertiaryShort})"
                    } else {
                        "${finalsMvp.ppg} ${labels.primaryShort}/G • ${finalsMvp.bestGamePoints} Peak ${labels.primaryShort}"
                    },
                    supportingNarrative = "Delivered clutch championship moments to lift ${champion?.teamFullName ?: finalsMvp.franchiseName} to the Season $year title.",
                    scoreValue = poPts.toDouble()
                )
            )
        }

        // 3. Scoring Champion
        effectiveStats.maxByOrNull { it.totalPoints * 1000.0 + it.ppg }?.let { scorer ->
            awards.add(
                YearlyAwardWinner(
                    id = "award_${year}_scoring",
                    year = year,
                    awardType = AnnualAwardType.SCORING_CHAMPION,
                    winnerName = scorer.playerName,
                    position = scorer.position,
                    franchiseId = scorer.franchiseId,
                    franchiseName = scorer.franchiseName,
                    primaryStatLine = "${scorer.ppg} ${labels.primaryShort}/G (${scorer.totalPoints} Total ${labels.primaryShort} • High: ${scorer.bestGamePoints})",
                    supportingNarrative = "Led all ${franchises.size} franchises in cumulative scoring output across ${scorer.totalAppearances} appearances.",
                    scoreValue = scorer.totalPoints.toDouble()
                )
            )
        }

        // 4. Defensive Player of the Year (DPOY)
        effectiveStats.maxByOrNull { s ->
            val team = franchisesById[s.franchiseId]
            val teamGames = (team?.wins ?: 0) + (team?.losses ?: 0)
            val paPerGame = if (team != null && teamGames > 0) team.pointsAgainst.toDouble() / teamGames else 100.0
            (s.spg * 4.0) + (s.rpg * 0.9) + ((120.0 - paPerGame) * 0.15)
        }?.let { dpoy ->
            awards.add(
                YearlyAwardWinner(
                    id = "award_${year}_dpoy",
                    year = year,
                    awardType = AnnualAwardType.DEFENSIVE_POY,
                    winnerName = dpoy.playerName,
                    position = dpoy.position,
                    franchiseId = dpoy.franchiseId,
                    franchiseName = dpoy.franchiseName,
                    primaryStatLine = "${dpoy.spg} ${labels.defenseShort}/G (${dpoy.totalStealsBlocks} Total) • ${dpoy.rpg} ${labels.secondaryShort}/G",
                    supportingNarrative = "Disrupted opposing offenses all season with elite rim/perimeter containment and relentless defensive stops.",
                    scoreValue = dpoy.totalStealsBlocks.toDouble()
                )
            )
        }

        // 5. Playmaker of the Year
        effectiveStats.maxByOrNull { it.totalAssists * 1000.0 + it.apg }?.let { playmaker ->
            awards.add(
                YearlyAwardWinner(
                    id = "award_${year}_playmaker",
                    year = year,
                    awardType = AnnualAwardType.PLAYMAKER_OF_YEAR,
                    winnerName = playmaker.playerName,
                    position = playmaker.position,
                    franchiseId = playmaker.franchiseId,
                    franchiseName = playmaker.franchiseName,
                    primaryStatLine = "${playmaker.apg} ${labels.tertiaryShort}/G (${playmaker.totalAssists} Total ${labels.tertiaryShort})",
                    supportingNarrative = "Engineered the league's sharpest offensive orchestration and topped the table in total playmaking.",
                    scoreValue = playmaker.totalAssists.toDouble()
                )
            )
        }

        // 6. Rising Star Award (age <= 25 priority)
        val risingCandidates = effectiveStats.filter { stat ->
            val age = rosterPlayersById[stat.playerId]?.first?.age ?: 26
            age <= 25
        }.ifEmpty { effectiveStats }
        risingCandidates.maxByOrNull { mvpCompositeScore(it) }?.let { rising ->
            val age = rosterPlayersById[rising.playerId]?.first?.age ?: 23
            awards.add(
                YearlyAwardWinner(
                    id = "award_${year}_rising",
                    year = year,
                    awardType = AnnualAwardType.RISING_STAR,
                    winnerName = rising.playerName,
                    position = rising.position,
                    franchiseId = rising.franchiseId,
                    franchiseName = rising.franchiseName,
                    primaryStatLine = "Age $age • ${rising.ppg} ${labels.primaryShort}/G • ${rising.apg} ${labels.tertiaryShort}/G (${rising.overallRating} OVR)",
                    supportingNarrative = "Emerged as the premier next-generation cornerstone poised to shape the next decade.",
                    scoreValue = rising.ppg
                )
            )
        }

        // 7. Franchise / Executive of the Year
        franchises.maxWithOrNull(
            compareBy<Franchise> { it.winPct }
                .thenBy { it.pointsFor - it.pointsAgainst }
                .thenBy { it.championships }
        )?.let { bestTeam ->
            val diff = bestTeam.pointsFor - bestTeam.pointsAgainst
            val diffStr = if (diff >= 0) "+$diff" else "$diff"
            awards.add(
                YearlyAwardWinner(
                    id = "award_${year}_exec",
                    year = year,
                    awardType = AnnualAwardType.EXECUTIVE_FRANCHISE,
                    winnerName = bestTeam.teamFullName,
                    position = bestTeam.conference.shortCode,
                    franchiseId = bestTeam.id,
                    franchiseName = bestTeam.ownership.name,
                    primaryStatLine = "${bestTeam.wins}-${bestTeam.losses} Record (${String.format("%.3f", bestTeam.winPct)}) • $diffStr DIFF",
                    supportingNarrative = "Posted the premier front-office and on-field campaign under ${bestTeam.ownership.name}.",
                    scoreValue = bestTeam.winPct * 100.0
                )
            )
        }

        // All-League First Team (Top 5 distinct-impact performers)
        val firstTeam = rankedForMvp.take(5).map { it.first }
        val hostCity = champion?.cityName ?: franchises.firstOrNull()?.cityName ?: "New York"

        return YearlyAwardsCeremony(
            year = year,
            hostCity = hostCity,
            ceremonyTitle = "Season $year Apex League Honors Gala",
            awards = awards,
            mvpFinalists = finalists,
            allLeagueFirstTeam = firstTeam,
            isPresented = false
        )
    }
}
