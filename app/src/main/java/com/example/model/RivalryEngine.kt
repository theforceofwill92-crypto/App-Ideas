package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt

enum class RivalryOrigin(
    val displayName: String,
    val badgeColorHex: Long,
    val description: String
) {
    PLAYOFF_HISTORY(
        displayName = "Playoff History",
        badgeColorHex = 0xFFFFB800, // Gold
        description = "Forged through high-stakes postseason eliminations and championship rematches"
    ),
    GEOGRAPHICAL_PROXIMITY(
        displayName = "Geographical Proximity",
        badgeColorHex = 0xFF06B6D4, // Cyan
        description = "Fueled by neighboring metro markets, commuter corridors, and regional pride"
    ),
    DIVISIONAL_COMPETITION(
        displayName = "Divisional Competition",
        badgeColorHex = 0xFFFF4B4B, // Crimson
        description = "Heated annual battles for the same division pennant and automatic playoff berth"
    )
}

enum class RivalryTier(
    val title: String,
    val minIntensity: Int,
    val badgeColorHex: Long
) {
    DORMANT("Emerging Spark", 0, 0xFF64748B),
    SIMMERING("Simmering Rivalry", 25, 0xFF3B82F6),
    HEATED("Heated Rivalry", 50, 0xFFF59E0B),
    FIERCE("Fierce Feud", 75, 0xFFEF4444),
    ICONIC("Historic Blood Feud", 90, 0xFFE11D48)
}

data class TeamRivalry(
    val id: String,
    val teamAId: String,
    val teamBId: String,
    val rivalryName: String,
    val intensity: Int, // 0 - 100 (active rivalry threshold >= 25)
    val origins: List<RivalryOrigin>,
    val playoffPoints: Int = 0,
    val geographyPoints: Int = 0,
    val divisionalPoints: Int = 0,
    val playoffMeetingsCount: Int = 0,
    val finalsMeetingsCount: Int = 0,
    val regularSeasonMeetingsCount: Int = 0,
    val closeGamesCount: Int = 0,
    val teamAWins: Int = 0,
    val teamBWins: Int = 0,
    val lastMeetingSummary: String = "",
    val lastUpdatedYear: Int = 2026
) {
    val tier: RivalryTier
        get() = when {
            intensity >= 90 -> RivalryTier.ICONIC
            intensity >= 75 -> RivalryTier.FIERCE
            intensity >= 50 -> RivalryTier.HEATED
            intensity >= 25 -> RivalryTier.SIMMERING
            else -> RivalryTier.DORMANT
        }

    val isActiveRivalry: Boolean
        get() = intensity >= 25

    val ticketRevenueMultiplier: Double
        get() {
            if (!isActiveRivalry) return 1.0
            val raw = 1.05 + (intensity.coerceIn(25, 100) / 100.0) * 0.50
            return (raw * 100.0).roundToInt() / 100.0
        }

    val moraleMultiplier: Double
        get() {
            if (!isActiveRivalry) return 1.0
            val raw = 1.15 + (intensity.coerceIn(25, 100) / 100.0) * 0.85
            return (raw * 100.0).roundToInt() / 100.0
        }

    val preGameCrowdHypeBoost: Int
        get() = if (!isActiveRivalry) 0 else (intensity / 25).coerceIn(1, 4)

    fun involves(franchiseId: String): Boolean = teamAId == franchiseId || teamBId == franchiseId

    fun opponentIdOf(franchiseId: String): String? = when (franchiseId) {
        teamAId -> teamBId
        teamBId -> teamAId
        else -> null
    }
}

data class MatchupRivalryImpact(
    val rivalry: TeamRivalry?,
    val isActiveRivalry: Boolean,
    val ticketRevenueMultiplier: Double,
    val moraleMultiplier: Double,
    val winMoraleDelta: Int,
    val lossMoraleDelta: Int,
    val crowdHypeBoost: Int,
    val homeBonusTicketMillions: Double
)

data class RivalryWeekProcessingResult(
    val updatedRivalries: List<TeamRivalry>,
    val headlines: List<String>,
    val totalRivalryTicketBonusMillions: Double,
    val ticketBonusByFranchiseId: Map<String, Double>,
    val moraleAdjustByFranchiseId: Map<String, Int>
)

object RivalryEngine {

    private data class GeoCorridor(
        val cityA: String,
        val cityB: String,
        val points: Int,
        val derbyTitle: String
    )

    private val geographicalCorridors = listOf(
        // North America Corridors
        GeoCorridor("Los Angeles", "Las Vegas", 34, "Mojave I-15 Clash"),
        GeoCorridor("Los Angeles", "San Diego", 40, "SoCal Freeway Feud"),
        GeoCorridor("Los Angeles", "San Francisco", 36, "Golden Coast Classic"),
        GeoCorridor("San Francisco", "San Diego", 28, "California Showdown"),
        GeoCorridor("New York", "Boston", 40, "Northeast Blood Feud"),
        GeoCorridor("New York", "Philadelphia", 38, "Turnpike Rivalry"),
        GeoCorridor("Boston", "Philadelphia", 32, "Colonial Corridor Clash"),
        GeoCorridor("New York", "Toronto", 26, "Atlantic Border Series"),
        GeoCorridor("Toronto", "Montreal", 40, "St. Lawrence Derby"),
        GeoCorridor("Seattle", "Portland", 38, "Cascadia Cup Clash"),
        GeoCorridor("Seattle", "Vancouver", 36, "Salish Sea Derby"),
        GeoCorridor("Portland", "Vancouver", 30, "Pacific Northwest Cup"),
        GeoCorridor("Austin", "San Antonio", 38, "I-35 Corridor Showdown"),
        GeoCorridor("Austin", "Dallas", 34, "Lone Star Crown"),
        GeoCorridor("Austin", "Houston", 34, "Texas Bayou-Hill Duel"),
        GeoCorridor("Austin", "Mexico City", 26, "Rio Grande Cross-Border Cup"),
        GeoCorridor("Austin", "Monterrey", 30, "Sierra-Hill Country Clash"),
        GeoCorridor("Mexico City", "Monterrey", 38, "Liga Mexicana Clásico"),
        GeoCorridor("Chicago", "Detroit", 36, "Great Lakes Rust-Belt War"),
        GeoCorridor("Chicago", "Indianapolis", 34, "I-65 Midwest Grudge"),
        GeoCorridor("Chicago", "Louisville", 28, "Heartland Crossroads Duel"),
        GeoCorridor("Chicago", "Kansas City", 26, "Midwest Prairie Clash"),
        GeoCorridor("Atlanta", "Charlotte", 34, "Piedmont I-85 Derby"),
        GeoCorridor("Atlanta", "Nashville", 32, "Appalachian Crown"),
        GeoCorridor("Miami", "Orlando", 36, "Sunshine State Derby"),
        GeoCorridor("Las Vegas", "Phoenix", 32, "Sonoran-Mojave Showdown"),
        GeoCorridor("Las Vegas", "Salt Lake City", 28, "Great Basin Duel"),
        GeoCorridor("Phoenix", "Albuquerque", 30, "High Desert Classic"),
        // Europe Corridors
        GeoCorridor("London", "Paris", 40, "Eurostar Channel Derby"),
        GeoCorridor("London", "Dublin", 34, "Irish Sea Clash"),
        GeoCorridor("London", "Amsterdam", 32, "North Sea Harbor Cup"),
        GeoCorridor("Paris", "Amsterdam", 30, "Benelux-Seine Classic"),
        GeoCorridor("Berlin", "Munich", 38, "Der Klassiker Basketball"),
        GeoCorridor("Berlin", "Prague", 32, "Central European Express"),
        GeoCorridor("Madrid", "Barcelona", 40, "El Clásico Continental"),
        GeoCorridor("Madrid", "Lisbon", 34, "Iberian Peninsula Derby"),
        GeoCorridor("Rome", "Milan", 38, "Derby d'Italia"),
        // South America Corridors
        GeoCorridor("São Paulo", "Rio de Janeiro", 40, "Ponte Aérea Clássico"),
        GeoCorridor("Buenos Aires", "Santiago", 36, "Andes Cordillera Derby"),
        GeoCorridor("São Paulo", "Buenos Aires", 32, "Mercosur Superclásico"),
        // Asia-Oceania Corridors
        GeoCorridor("Tokyo", "Osaka", 38, "Shinkansen Bullet Derby"),
        GeoCorridor("Tokyo", "Seoul", 36, "Korea Strait Classic"),
        GeoCorridor("Sydney", "Melbourne", 40, "Hume Highway Blood Feud"),
        GeoCorridor("Sydney", "Auckland", 34, "Trans-Tasman Clash"),
        GeoCorridor("Singapore", "Manila", 30, "South China Sea Showdown")
    )

    fun canonicalRivalryId(team1Id: String, team2Id: String): String {
        return if (team1Id <= team2Id) "riv_${team1Id}_${team2Id}" else "riv_${team2Id}_${team1Id}"
    }

    fun findRivalry(team1Id: String, team2Id: String, rivalries: List<TeamRivalry>): TeamRivalry? {
        if (team1Id == team2Id) return null
        val targetId = canonicalRivalryId(team1Id, team2Id)
        return rivalries.find { it.id == targetId }
    }

    fun evaluateGeographyProximity(
        teamA: Franchise,
        teamB: Franchise,
        cities: List<MarketCity>
    ): Pair<Int, String?> {
        val corridor = geographicalCorridors.find {
            (it.cityA.equals(teamA.cityName, ignoreCase = true) && it.cityB.equals(teamB.cityName, ignoreCase = true)) ||
                (it.cityB.equals(teamA.cityName, ignoreCase = true) && it.cityA.equals(teamB.cityName, ignoreCase = true))
        }
        if (corridor != null) {
            return corridor.points to corridor.derbyTitle
        }

        val cityA = cities.find { it.id == teamA.cityId || it.name.equals(teamA.cityName, ignoreCase = true) }
        val cityB = cities.find { it.id == teamB.cityId || it.name.equals(teamB.cityName, ignoreCase = true) }
        if (cityA != null && cityB != null) {
            if (cityA.region.equals(cityB.region, ignoreCase = true)) {
                return 26 to "${cityA.region} Regional Derby"
            }
            if (cityA.country.equals(cityB.country, ignoreCase = true) && cityA.isInternational) {
                return 25 to "${cityA.country} National Derby"
            }
            if (!cityA.isInternational && !cityB.isInternational && teamA.conference == teamB.conference) {
                val closeUsRegions = setOf(
                    setOf("East Coast", "Midwest"),
                    setOf("West Coast", "Southwest"),
                    setOf("West Coast", "Pacific Northwest"),
                    setOf("Sunbelt", "Southwest")
                )
                if (setOf(cityA.region, cityB.region) in closeUsRegions) {
                    return 18 to "${teamA.cityName}–${teamB.cityName} Corridor"
                }
            }
        }
        return 0 to null
    }

    fun seedInitialRivalries(
        franchises: List<Franchise>,
        cities: List<MarketCity>,
        seasonYear: Int = 2026
    ): List<TeamRivalry> {
        val list = mutableListOf<TeamRivalry>()
        for (i in franchises.indices) {
            for (j in i + 1 until franchises.size) {
                val a = franchises[i]
                val b = franchises[j]
                val (orderedA, orderedB) = if (a.id <= b.id) a to b else b to a
                val (geoPts, geoTitle) = evaluateGeographyProximity(orderedA, orderedB, cities)
                val sameDivision = orderedA.division == orderedB.division
                val divPts = if (sameDivision) 24 else 0

                // Inaugural league lore: NYC vs LA coastal flagship marquee + NY/Chicago charter grudge
                val isCoastFlagship = setOf(orderedA.cityName, orderedB.cityName) == setOf("New York", "Los Angeles")
                val isCharterEast = setOf(orderedA.cityName, orderedB.cityName) == setOf("New York", "Chicago")
                val playoffPts = when {
                    isCoastFlagship -> 28
                    isCharterEast -> 18
                    else -> 0
                }
                val playoffCount = when {
                    isCoastFlagship -> 2
                    isCharterEast -> 1
                    else -> 0
                }
                val finalsCount = if (isCoastFlagship) 1 else 0

                val origins = buildList {
                    if (playoffPts > 0) add(RivalryOrigin.PLAYOFF_HISTORY)
                    if (geoPts >= 20) add(RivalryOrigin.GEOGRAPHICAL_PROXIMITY)
                    if (divPts > 0) add(RivalryOrigin.DIVISIONAL_COMPETITION)
                }

                val intensity = (playoffPts + geoPts + divPts).coerceIn(0, 100)
                if (intensity >= 18) {
                    val title = when {
                        isCoastFlagship -> "Coast-to-Coast Finals Grudge"
                        geoTitle != null -> geoTitle
                        sameDivision -> "${orderedA.division.shortCode} Division Pennant War"
                        else -> "${orderedA.nickname}–${orderedB.nickname} Clash"
                    }
                    val lore = when {
                        isCoastFlagship -> "Historic championship finals rematch tension between flagship mega-markets."
                        sameDivision && geoPts >= 20 -> "Shared division crown & regional bragging rights at stake every matchup."
                        sameDivision -> "Direct ${orderedA.division.displayName} rivals battling for the automatic berth."
                        else -> "Geographic neighbors competing for regional supremacy and packed arenas."
                    }
                    list.add(
                        TeamRivalry(
                            id = canonicalRivalryId(orderedA.id, orderedB.id),
                            teamAId = orderedA.id,
                            teamBId = orderedB.id,
                            rivalryName = title,
                            intensity = intensity,
                            origins = origins.ifEmpty { listOf(RivalryOrigin.DIVISIONAL_COMPETITION) },
                            playoffPoints = playoffPts,
                            geographyPoints = geoPts,
                            divisionalPoints = divPts,
                            playoffMeetingsCount = playoffCount,
                            finalsMeetingsCount = finalsCount,
                            lastMeetingSummary = lore,
                            lastUpdatedYear = seasonYear
                        )
                    )
                }
            }
        }
        return list.sortedByDescending { it.intensity }
    }

    fun evaluateMatchupImpact(
        home: Franchise,
        away: Franchise,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>,
        isPlayoffs: Boolean = false
    ): MatchupRivalryImpact {
        val existing = findRivalry(home.id, away.id, rivalries)
        val active = existing?.takeIf { it.isActiveRivalry }

        // Even if an existing entry wasn't stored yet, check real-time proximity/division structural spark
        val ticketMult = active?.ticketRevenueMultiplier ?: 1.0
        val moraleMult = active?.moraleMultiplier ?: 1.0

        val baseWinMorale = if (isPlayoffs) 4 else 2
        val baseLossMorale = if (isPlayoffs) 3 else 2
        val winDelta = if (active != null) (baseWinMorale * moraleMult).roundToInt().coerceAtLeast(baseWinMorale + 1) else baseWinMorale
        val lossDelta = if (active != null) -(baseLossMorale * moraleMult).roundToInt().coerceAtLeast(baseLossMorale + 1) else -baseLossMorale

        val attendanceRate = FanLoyaltyEngine.computeAttendanceRate(home)
        val singleGameTicketMillions = (home.stadiumCapacity * attendanceRate * home.ticketPrice) / 1_000_000.0
        val bonusTicketMillions = if (active != null) {
            Math.round(singleGameTicketMillions * (ticketMult - 1.0) * 100.0) / 100.0
        } else 0.0

        return MatchupRivalryImpact(
            rivalry = existing,
            isActiveRivalry = active != null,
            ticketRevenueMultiplier = ticketMult,
            moraleMultiplier = moraleMult,
            winMoraleDelta = winDelta,
            lossMoraleDelta = lossDelta,
            crowdHypeBoost = active?.preGameCrowdHypeBoost ?: 0,
            homeBonusTicketMillions = bonusTicketMillions
        )
    }

    fun annotateMatchesWithRivalries(
        matches: List<GameMatch>,
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>,
        isPlayoffs: Boolean = false
    ): List<GameMatch> {
        return matches.map { match ->
            val home = franchises.find { it.id == match.homeTeamId }
            val away = franchises.find { it.id == match.awayTeamId }
            if (home == null || away == null) return@map match
            val impact = evaluateMatchupImpact(home, away, rivalries, cities, isPlayoffs)
            if (impact.isActiveRivalry && impact.rivalry != null) {
                match.copy(
                    rivalryId = impact.rivalry.id,
                    rivalryTitle = "${impact.rivalry.rivalryName} (${impact.rivalry.tier.title})",
                    rivalryIntensity = impact.rivalry.intensity,
                    ticketRevenueMultiplier = impact.ticketRevenueMultiplier,
                    moraleMultiplier = impact.moraleMultiplier,
                    rivalryBonusTicketMillions = impact.homeBonusTicketMillions
                )
            } else {
                match.copy(
                    rivalryId = impact.rivalry?.id,
                    rivalryTitle = null,
                    rivalryIntensity = impact.rivalry?.intensity ?: 0,
                    ticketRevenueMultiplier = 1.0,
                    moraleMultiplier = 1.0,
                    rivalryBonusTicketMillions = 0.0
                )
            }
        }
    }

    fun processPlayedMatchups(
        playedMatches: List<GameMatch>,
        franchises: List<Franchise>,
        currentRivalries: List<TeamRivalry>,
        cities: List<MarketCity>,
        sportType: SportType,
        seasonYear: Int,
        isPlayoffs: Boolean
    ): RivalryWeekProcessingResult {
        val rivalryMap = currentRivalries.associateBy { it.id }.toMutableMap()
        val headlines = mutableListOf<String>()
        val ticketBonusByTeam = mutableMapOf<String, Double>()
        val moraleAdjustByTeam = mutableMapOf<String, Int>()
        var totalBonusMillions = 0.0

        val closeMarginThreshold = when (sportType) {
            SportType.SOCCER, SportType.HOCKEY -> 1
            SportType.FOOTBALL -> 6
            SportType.BASKETBALL -> 6
        }

        playedMatches.filter { it.isPlayed }.forEach { match ->
            val home = franchises.find { it.id == match.homeTeamId } ?: return@forEach
            val away = franchises.find { it.id == match.awayTeamId } ?: return@forEach

            val (orderedA, orderedB) = if (home.id <= away.id) home to away else away to home
            val rid = canonicalRivalryId(orderedA.id, orderedB.id)
            val prev = rivalryMap[rid]

            // Evaluate pre-game financial & morale multipliers using rivalry before post-game escalation
            val preImpact = evaluateMatchupImpact(home, away, currentRivalries, cities, isPlayoffs)
            if (preImpact.homeBonusTicketMillions > 0.0) {
                ticketBonusByTeam[home.id] = (ticketBonusByTeam[home.id] ?: 0.0) + preImpact.homeBonusTicketMillions
                totalBonusMillions += preImpact.homeBonusTicketMillions
            }

            val homeWon = match.homeScore > match.awayScore
            if (preImpact.isActiveRivalry) {
                val baseWin = if (isPlayoffs) 4 else 2
                val baseLoss = if (isPlayoffs) -3 else -2
                val homeExtra = if (homeWon) (preImpact.winMoraleDelta - baseWin) else (preImpact.lossMoraleDelta - baseLoss)
                val awayExtra = if (!homeWon) (preImpact.winMoraleDelta - baseWin) else (preImpact.lossMoraleDelta - baseLoss)
                if (homeExtra != 0) moraleAdjustByTeam[home.id] = (moraleAdjustByTeam[home.id] ?: 0) + homeExtra
                if (awayExtra != 0) moraleAdjustByTeam[away.id] = (moraleAdjustByTeam[away.id] ?: 0) + awayExtra
            }

            // Compute escalation points earned from this game
            val margin = abs(match.homeScore - match.awayScore)
            val isCloseGame = margin <= closeMarginThreshold
            val sameDivision = orderedA.division == orderedB.division
            val isFinals = isPlayoffs && match.roundName.contains("Finals", ignoreCase = true)
            val (geoPtsBase, geoTitle) = evaluateGeographyProximity(orderedA, orderedB, cities)

            val gainedPlayoff = when {
                isFinals -> 22 + if (isCloseGame) 6 else 0
                isPlayoffs -> 15 + if (isCloseGame) 5 else 0
                else -> 0
            }
            val gainedDivision = if (sameDivision) {
                7 + (if (isCloseGame) 5 else 0) + (if (orderedA.winPct >= 0.5 && orderedB.winPct >= 0.5) 3 else 0)
            } else 0
            val gainedGeo = if (geoPtsBase >= 20) {
                if (prev == null) geoPtsBase else (3 + if (isCloseGame) 2 else 0)
            } else 0

            val newPlayoffPoints = (prev?.playoffPoints ?: 0) + gainedPlayoff
            val newDivisionPoints = (prev?.divisionalPoints ?: 0) + gainedDivision
            val newGeoPoints = maxOf(prev?.geographyPoints ?: 0, geoPtsBase) + if (prev != null && geoPtsBase >= 20) gainedGeo else 0

            val newIntensity = if (prev != null) {
                (prev.intensity + gainedPlayoff + gainedDivision + gainedGeo + (if (isCloseGame && !sameDivision && !isPlayoffs) 4 else 0)).coerceIn(0, 100)
            } else {
                (newPlayoffPoints + newDivisionPoints + newGeoPoints + (if (isCloseGame) 4 else 0)).coerceIn(0, 100)
            }

            // Only store rivalry once there's meaningful heat (>= 12) or any playoff/divisional/geo spark
            if (newIntensity >= 12 || prev != null) {
                val updatedOrigins = ((prev?.origins ?: emptyList()) + buildList {
                    if (newPlayoffPoints > 0) add(RivalryOrigin.PLAYOFF_HISTORY)
                    if (newGeoPoints >= 20) add(RivalryOrigin.GEOGRAPHICAL_PROXIMITY)
                    if (newDivisionPoints > 0) add(RivalryOrigin.DIVISIONAL_COMPETITION)
                }).distinct().ifEmpty { listOf(RivalryOrigin.DIVISIONAL_COMPETITION) }

                val title = prev?.rivalryName ?: when {
                    isFinals -> "Championship Finals Feud"
                    geoTitle != null -> geoTitle
                    sameDivision -> "${orderedA.division.shortCode} Division War"
                    isPlayoffs -> "${orderedA.nickname}–${orderedB.nickname} Playoff Grudge"
                    else -> "${orderedA.cityName}–${orderedB.cityName} Series"
                }

                val winner = if (homeWon) home else away
                val loser = if (homeWon) away else home
                val contextTag = if (isPlayoffs) match.roundName.ifBlank { "Playoffs" } else "Week ${match.week}"
                val summary = "$contextTag: ${winner.teamFullName} def. ${loser.teamFullName} (${match.homeScore}-${match.awayScore})${if (isCloseGame) " [Thriller]" else ""}"

                val aWon = (homeWon && orderedA.id == home.id) || (!homeWon && orderedA.id == away.id)
                val updated = TeamRivalry(
                    id = rid,
                    teamAId = orderedA.id,
                    teamBId = orderedB.id,
                    rivalryName = title,
                    intensity = newIntensity,
                    origins = updatedOrigins,
                    playoffPoints = newPlayoffPoints,
                    geographyPoints = newGeoPoints,
                    divisionalPoints = newDivisionPoints,
                    playoffMeetingsCount = (prev?.playoffMeetingsCount ?: 0) + if (isPlayoffs) 1 else 0,
                    finalsMeetingsCount = (prev?.finalsMeetingsCount ?: 0) + if (isFinals) 1 else 0,
                    regularSeasonMeetingsCount = (prev?.regularSeasonMeetingsCount ?: 0) + if (!isPlayoffs) 1 else 0,
                    closeGamesCount = (prev?.closeGamesCount ?: 0) + if (isCloseGame) 1 else 0,
                    teamAWins = (prev?.teamAWins ?: 0) + if (aWon) 1 else 0,
                    teamBWins = (prev?.teamBWins ?: 0) + if (!aWon) 1 else 0,
                    lastMeetingSummary = summary,
                    lastUpdatedYear = seasonYear
                )
                rivalryMap[rid] = updated

                val wasActive = prev?.isActiveRivalry == true
                val prevTier = prev?.tier ?: RivalryTier.DORMANT
                if (!wasActive && updated.isActiveRivalry) {
                    headlines.add("NEW RIVALRY IGNITED: ${orderedA.teamFullName} vs ${orderedB.teamFullName} ('${updated.rivalryName}') reaches ${updated.tier.title}! (${updated.ticketRevenueMultiplier}x Ticket Gate)")
                } else if (wasActive && updated.tier != prevTier && updated.intensity > (prev?.intensity ?: 0)) {
                    headlines.add("RIVALRY ESCALATION: '${updated.rivalryName}' (${orderedA.nickname} vs ${orderedB.nickname}) surges to ${updated.tier.title} (${updated.intensity}/100 Intensity • ${updated.ticketRevenueMultiplier}x Gate)!")
                } else if (isPlayoffs && updated.isActiveRivalry) {
                    headlines.add("PLAYOFF GRUDGE MATCH: ${winner.teamFullName} takes '${updated.rivalryName}' ${match.roundName} thriller (${match.homeScore}-${match.awayScore})!")
                }
            }
        }

        return RivalryWeekProcessingResult(
            updatedRivalries = rivalryMap.values.sortedByDescending { it.intensity },
            headlines = headlines.distinct(),
            totalRivalryTicketBonusMillions = Math.round(totalBonusMillions * 100.0) / 100.0,
            ticketBonusByFranchiseId = ticketBonusByTeam,
            moraleAdjustByFranchiseId = moraleAdjustByTeam
        )
    }

    fun syncRivalriesForLeagueTopology(
        franchises: List<Franchise>,
        cities: List<MarketCity>,
        currentRivalries: List<TeamRivalry>,
        seasonYear: Int
    ): List<TeamRivalry> {
        val validIds = franchises.map { it.id }.toSet()
        val rivalryMap = currentRivalries
            .filter { it.teamAId in validIds && it.teamBId in validIds }
            .associateBy { it.id }
            .toMutableMap()

        for (i in franchises.indices) {
            for (j in i + 1 until franchises.size) {
                val a = franchises[i]
                val b = franchises[j]
                val (orderedA, orderedB) = if (a.id <= b.id) a to b else b to a
                val rid = canonicalRivalryId(orderedA.id, orderedB.id)
                val prev = rivalryMap[rid]
                val (geoPts, geoTitle) = evaluateGeographyProximity(orderedA, orderedB, cities)
                val sameDiv = orderedA.division == orderedB.division
                val divBase = if (sameDiv) 24 else 0

                if (geoPts >= 20 || sameDiv) {
                    val newGeo = maxOf(prev?.geographyPoints ?: 0, geoPts)
                    val newDiv = maxOf(prev?.divisionalPoints ?: 0, divBase)
                    val baseStructural = (prev?.playoffPoints ?: 0) + newGeo + newDiv
                    val newIntensity = maxOf(prev?.intensity ?: 0, baseStructural).coerceIn(0, 100)
                    val origins = ((prev?.origins ?: emptyList()) + buildList {
                        if (newGeo >= 20) add(RivalryOrigin.GEOGRAPHICAL_PROXIMITY)
                        if (newDiv > 0) add(RivalryOrigin.DIVISIONAL_COMPETITION)
                    }).distinct()

                    val title = prev?.rivalryName ?: geoTitle ?: "${orderedA.division.shortCode} Division Pennant War"
                    rivalryMap[rid] = (prev ?: TeamRivalry(
                        id = rid,
                        teamAId = orderedA.id,
                        teamBId = orderedB.id,
                        rivalryName = title,
                        intensity = newIntensity,
                        origins = origins,
                        lastMeetingSummary = if (geoPts >= 20 && sameDiv) "Divisional & regional neighbors forged after alignment." else if (geoPts >= 20) "Regional proximity derby sparked across metro markets." else "New divisional rival pairing.",
                        lastUpdatedYear = seasonYear
                    )).copy(
                        rivalryName = title,
                        intensity = newIntensity,
                        origins = origins,
                        geographyPoints = newGeo,
                        divisionalPoints = newDiv,
                        lastUpdatedYear = seasonYear
                    )
                }
            }
        }
        return rivalryMap.values.sortedByDescending { it.intensity }
    }
}
