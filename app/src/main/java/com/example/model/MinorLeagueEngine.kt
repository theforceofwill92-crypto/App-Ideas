package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

data class MinorMarketCity(
    val id: String,
    val cityName: String,
    val region: String,
    val preferredConference: Conference,
    val populationMillions: Double,
    val sportsAppetite: Int,
    val arenaName: String,
    val arenaCapacity: Int,
    val marketTierLabel: String
) {
    val marketScore: Int
        get() = ((sportsAppetite * 0.65) + (populationMillions.coerceAtMost(2.5) * 14.0)).roundToInt().coerceIn(50, 98)
}

data class MinorAffiliateFinances(
    val ticketRevenueMillions: Double = 0.0,
    val localSponsorMillions: Double = 0.0,
    val streamingRevenueMillions: Double = 0.0,
    val parentSubsidyMillions: Double = 0.0,
    val payrollCostMillions: Double = 0.0,
    val operationsAndTravelMillions: Double = 0.0,
    val gamesHosted: Int = 0,
    val cumulativeAttendance: Int = 0,
    val affiliateValuationMillions: Double = 18.0
) {
    val totalRevenueMillions: Double
        get() = round2(ticketRevenueMillions + localSponsorMillions + streamingRevenueMillions + parentSubsidyMillions)

    val totalExpensesMillions: Double
        get() = round2(payrollCostMillions + operationsAndTravelMillions)

    val netOperatingBalanceMillions: Double
        get() = round2(totalRevenueMillions - totalExpensesMillions)

    val avgAttendancePerGame: Int
        get() = if (gamesHosted <= 0) 0 else cumulativeAttendance / gamesHosted

    private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}

data class MinorAffiliateProspect(
    val id: String,
    val affiliateId: String,
    val name: String,
    val position: String,
    val overallRating: Int,
    val age: Int,
    val archetype: String,
    val salaryMillions: Double
)

data class MinorLeagueAffiliate(
    val id: String,
    val parentFranchiseId: String,
    val parentFranchiseName: String,
    val cityId: String,
    val cityName: String,
    val nickname: String,
    val colorHex: Long,
    val conference: Conference,
    val division: Division,
    val marketTierLabel: String,
    val arenaName: String,
    val arenaCapacity: Int,
    val populationMillions: Double,
    val ticketPrice: Int = 24,
    val fanBaseThousands: Int = 140,
    val fanLoyalty: Int = 74,
    val marketAppetite: Int = 78,
    val wins: Int = 0,
    val losses: Int = 0,
    val pointsFor: Int = 0,
    val pointsAgainst: Int = 0,
    val championships: Int = 0,
    val isExpansionAffiliate: Boolean = false,
    val foundedYear: Int = 2026,
    val finances: MinorAffiliateFinances = MinorAffiliateFinances(),
    val supplementalProspects: List<MinorAffiliateProspect> = emptyList(),
    val ovrBreakthroughsThisSeason: Int = 0
) {
    val fullName: String
        get() = "$cityName $nickname"

    val winPct: Double
        get() {
            val total = wins + losses
            return if (total == 0) 0.0 else wins.toDouble() / total
        }

    val pointDifferential: Int
        get() = pointsFor - pointsAgainst
}

data class MinorLeagueMatch(
    val id: String,
    val week: Int,
    val homeAffiliateId: String,
    val homeAffiliateName: String,
    val awayAffiliateId: String,
    val awayAffiliateName: String,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val attendance: Int = 0,
    val gateRevenueMillions: Double = 0.0,
    val isPlayed: Boolean = false,
    val isPlayoff: Boolean = false,
    val roundName: String = "",
    val topPerformerSummary: String = ""
)

data class MinorLeaguePlayerStat(
    val playerId: String,
    val playerName: String,
    val position: String,
    val affiliateId: String,
    val affiliateName: String,
    val parentFranchiseId: String,
    val parentFranchiseName: String,
    val isParentDepthPlayer: Boolean,
    val overallRating: Int,
    val age: Int,
    val gamesPlayed: Int = 0,
    val totalPrimaryStat: Int = 0,
    val totalSecondaryStat: Int = 0,
    val totalTertiaryStat: Int = 0,
    val totalDefenseStat: Int = 0,
    val playoffPrimaryStat: Int = 0,
    val devXp: Int = 0,
    val ovrGainsEarned: Int = 0
) {
    val primaryPerGame: Double
        get() = if (gamesPlayed <= 0) 0.0 else ((totalPrimaryStat.toDouble() / gamesPlayed) * 10.0).roundToInt() / 10.0

    val secondaryPerGame: Double
        get() = if (gamesPlayed <= 0) 0.0 else ((totalSecondaryStat.toDouble() / gamesPlayed) * 10.0).roundToInt() / 10.0

    val tertiaryPerGame: Double
        get() = if (gamesPlayed <= 0) 0.0 else ((totalTertiaryStat.toDouble() / gamesPlayed) * 10.0).roundToInt() / 10.0
}

data class MinorLeagueWeeklyAward(
    val week: Int,
    val seasonYear: Int,
    val playerId: String,
    val playerName: String,
    val position: String,
    val affiliateName: String,
    val parentFranchiseName: String,
    val statSummary: String,
    val impactScore: Int
)

enum class MinorLeagueAwardType(val title: String, val badge: String, val colorHex: Long) {
    MINOR_MVP("Development League MVP", "D-MVP", 0xFFD4AF37),
    TOP_PROSPECT_CALLUP("Top Call-Up Prospect", "PROSPECT", 0xFF06B6D4),
    DEFENSIVE_PROSPECT("Minor Defensive Player of the Year", "D-DPOY", 0xFF3B82F6),
    SCORING_LEADER("Development Scoring Champion", "SCORING", 0xFFEF4444),
    FINALS_MVP("Minor League Finals MVP", "FINALS MVP", 0xFF10B981)
}

data class MinorLeagueAward(
    val id: String,
    val year: Int,
    val type: MinorLeagueAwardType,
    val playerId: String,
    val playerName: String,
    val position: String,
    val affiliateId: String,
    val affiliateName: String,
    val parentFranchiseName: String,
    val statLine: String,
    val narrative: String
)

data class MinorLeagueSeasonArchive(
    val year: Int,
    val leagueName: String,
    val affiliatesCount: Int,
    val playoffFieldSize: Int,
    val championAffiliateId: String,
    val championAffiliateName: String,
    val championParentName: String,
    val runnerUpAffiliateName: String,
    val totalAttendance: Int,
    val circuitRevenueMillions: Double,
    val awards: List<MinorLeagueAward>
)

data class MinorLeagueState(
    val leagueName: String = "Apex G-Circuit",
    val affiliates: List<MinorLeagueAffiliate> = emptyList(),
    val currentWeekMatches: List<MinorLeagueMatch> = emptyList(),
    val playoffMatches: List<MinorLeagueMatch> = emptyList(),
    val playoffFieldSize: Int = 4,
    val playoffChampionAffiliateId: String? = null,
    val playoffChampionName: String? = null,
    val playoffRunnerUpName: String? = null,
    val playerStats: Map<String, MinorLeaguePlayerStat> = emptyMap(),
    val weeklyAwards: List<MinorLeagueWeeklyAward> = emptyList(),
    val annualAwards: List<MinorLeagueAward> = emptyList(),
    val pastSeasons: List<MinorLeagueSeasonArchive> = emptyList(),
    val manualParentBenchOptOutIds: Set<String> = emptySet(),
    val lastSyncedParentTeamCount: Int = 0
) {
    val totalCircuitRevenueMillions: Double
        get() = ((affiliates.sumOf { it.finances.totalRevenueMillions }) * 100.0).roundToInt() / 100.0

    val totalCircuitExpensesMillions: Double
        get() = ((affiliates.sumOf { it.finances.totalExpensesMillions }) * 100.0).roundToInt() / 100.0

    val totalCircuitNetMillions: Double
        get() = ((affiliates.sumOf { it.finances.netOperatingBalanceMillions }) * 100.0).roundToInt() / 100.0

    val totalCircuitAttendance: Int
        get() = affiliates.sumOf { it.finances.cumulativeAttendance }

    val totalOvrBreakthroughs: Int
        get() = affiliates.sumOf { it.ovrBreakthroughsThisSeason }

    val scalingFormatSummary: String
        get() = when {
            affiliates.size >= 12 -> "Expanded ${affiliates.size}-Club Circuit • $playoffFieldSize-Team Quarterfinal Bracket"
            affiliates.size >= 8 -> "Growth ${affiliates.size}-Club Network • $playoffFieldSize-Team Play-In & Semis"
            else -> "${affiliates.size}-Club Regional Circuit • $playoffFieldSize-Team Championship Bracket"
        }
}

data class MinorLeagueSyncResult(
    val updatedFranchises: List<Franchise>,
    val minorState: MinorLeagueState,
    val headlines: List<String>
)

data class MinorLeagueWeekResult(
    val updatedFranchises: List<Franchise>,
    val minorState: MinorLeagueState,
    val headlines: List<String>
)

object MinorLeagueEngine {

    private val feederNicknames = listOf(
        "Charge", "Knights", "Vipers", "Cruisers", "Surge", "Thunderbolts",
        "Wolves", "Outlaws", "Aces", "Blazers", "Pioneers", "Generals",
        "Comets", "Sentinels", "Express", "Phantoms", "Guardians", "Mavericks"
    )

    private val prospectFirstNames = listOf(
        "Coby", "Malik", "Jalen", "Bryce", "Tanner", "Niko", "Kaleb", "Dante",
        "Camden", "Luka", "Tariq", "Owen", "Desmond", "Elias", "Kieran", "Gavin"
    )

    private val prospectLastNames = listOf(
        "Pryor", "Sato", "Bauer", "Diallo", "Kessler", "Alverez", "Maddox", "Griffin",
        "Moreno", "Hollis", "kovalenko", "Reyes", "Mercer", "Kemp", "Stanton", "Vega"
    ).map { it.replaceFirstChar { ch -> ch.uppercase() } }

    fun getFeederMarketCatalog(): List<MinorMarketCity> = listOf(
        // Eastern Feeder Markets
        MinorMarketCity("mm_hartford", "Hartford", "New England", Conference.EASTERN, 1.21, 83, "Hartford Civic Pavilion", 7600, "Tier-2 Feeder Hub"),
        MinorMarketCity("mm_providence", "Providence", "New England", Conference.EASTERN, 1.62, 81, "Narragansett Dome", 8100, "Tier-2 Feeder Hub"),
        MinorMarketCity("mm_grand_rapids", "Grand Rapids", "Midwest", Conference.EASTERN, 1.08, 86, "Riverfront Fieldhouse", 7800, "Passionate Heartland"),
        MinorMarketCity("mm_fort_wayne", "Fort Wayne", "Midwest", Conference.EASTERN, 0.65, 84, "Summit Coliseum", 6400, "Heartland Dev Hub"),
        MinorMarketCity("mm_richmond", "Richmond", "Mid-Atlantic", Conference.EASTERN, 1.32, 79, "James River Arena", 7200, "Emerging Dev Market"),
        MinorMarketCity("mm_jacksonville", "Jacksonville", "Southeast", Conference.EASTERN, 1.63, 77, "First Coast Pavilion", 8200, "Growth Sunbelt Feeder"),
        MinorMarketCity("mm_rochester", "Rochester", "Upstate NY", Conference.EASTERN, 1.07, 82, "Genesee Valley Center", 6900, "Tier-2 Feeder Hub"),
        MinorMarketCity("mm_des_moines", "Des Moines", "Heartland", Conference.EASTERN, 0.72, 85, "Prairie Crown Arena", 6700, "Heartland Dev Hub"),
        // Western Feeder Markets
        MinorMarketCity("mm_reno", "Reno", "Sierra West", Conference.WESTERN, 0.51, 80, "Tahoe Basin Arena", 5800, "Emerging Dev Market"),
        MinorMarketCity("mm_bakersfield", "Bakersfield", "Central Valley", Conference.WESTERN, 0.91, 78, "Kern Valley Coliseum", 6300, "Regional Dev Hub"),
        MinorMarketCity("mm_el_paso", "El Paso", "Southwest", Conference.WESTERN, 0.87, 82, "Borderland Fieldhouse", 6800, "Growth Sunbelt Feeder"),
        MinorMarketCity("mm_spokane", "Spokane", "Pacific NW", Conference.WESTERN, 0.59, 87, "Inland Empire Pavilion", 6600, "Passionate Heartland"),
        MinorMarketCity("mm_tacoma", "Tacoma", "Puget Sound", Conference.WESTERN, 0.92, 84, "Rainier Sound Arena", 7100, "Tier-2 Feeder Hub"),
        MinorMarketCity("mm_tulsa", "Tulsa", "Plains", Conference.WESTERN, 1.03, 81, "Route 66 Coliseum", 7400, "Heartland Dev Hub"),
        MinorMarketCity("mm_tucson", "Tucson", "Desert SW", Conference.WESTERN, 1.06, 79, "Sonoran Sun Pavilion", 7000, "Growth Sunbelt Feeder"),
        MinorMarketCity("mm_omaha", "Omaha", "Plains", Conference.WESTERN, 0.97, 83, "Heartland Crossroads", 7500, "Passionate Heartland"),
        MinorMarketCity("mm_fresno", "Fresno", "Pacific", Conference.WESTERN, 1.01, 77, "Sierra Gateway Center", 6900, "Regional Dev Hub"),
        // European Academy Markets
        MinorMarketCity("mm_manchester", "Manchester", "United Kingdom", Conference.EUROPEAN, 2.75, 88, "Northern Quarter Dome", 8600, "European Academy Hub"),
        MinorMarketCity("mm_lyon", "Lyon", "France", Conference.EUROPEAN, 1.72, 84, "Rhone Metro Arena", 7800, "European Academy Hub"),
        MinorMarketCity("mm_hamburg", "Hamburg", "Germany", Conference.EUROPEAN, 1.85, 82, "Elbe Waterfront Hall", 7900, "European Academy Hub"),
        MinorMarketCity("mm_valencia", "Valencia", "Spain", Conference.EUROPEAN, 1.59, 85, "Turia Pavilion", 7400, "European Academy Hub"),
        // South American Academy Markets
        MinorMarketCity("mm_curitiba", "Curitiba", "Brazil", Conference.SOUTH_AMERICAN, 1.96, 86, "Araucaria Dome", 7600, "South American Feeder"),
        MinorMarketCity("mm_cordoba", "Córdoba", "Argentina", Conference.SOUTH_AMERICAN, 1.58, 87, "Sierra Cordoba Hall", 7100, "South American Feeder"),
        // Asia-Oceania Academy Markets
        MinorMarketCity("mm_yokohama", "Yokohama", "Japan", Conference.ASIA_OCEANIA, 3.77, 89, "Bayshore Minato Arena", 8900, "APAC Academy Hub"),
        MinorMarketCity("mm_brisbane", "Brisbane", "Australia", Conference.ASIA_OCEANIA, 2.56, 84, "River City Stadium", 8200, "APAC Academy Hub"),
        MinorMarketCity("mm_busan", "Busan", "South Korea", Conference.ASIA_OCEANIA, 3.35, 86, "Haeundae Arena", 8400, "APAC Academy Hub")
    )

    fun calculateScalingPlayoffFieldSize(affiliateCount: Int): Int = when {
        affiliateCount >= 12 -> 8
        affiliateCount >= 8 -> 6
        affiliateCount >= 4 -> 4
        affiliateCount >= 2 -> 2
        else -> 0
    }

    fun getParentAssignedDepthPlayers(franchise: Franchise): List<RosterPlayer> =
        franchise.roster.filter { !it.isStarter && !it.isMedicallyOut && it.isAssignedToMinorLeague }
            .sortedWith(compareBy<RosterPlayer> { it.depthOrder }.thenByDescending { it.overallRating })

    fun getEligibleParentBenchPlayers(franchise: Franchise): List<RosterPlayer> =
        InjuryEngine.getBenchPlayers(franchise).filter { !it.isMedicallyOut }

    fun syncAffiliatesWithParentLeague(
        franchises: List<Franchise>,
        currentMinorState: MinorLeagueState?,
        sportType: SportType,
        seasonYear: Int
    ): MinorLeagueSyncResult {
        val baseState = currentMinorState ?: MinorLeagueState(leagueName = sportType.minorLeagueName)
        val existingByParent = baseState.affiliates.associateBy { it.parentFranchiseId }
        val optedOutIds = baseState.manualParentBenchOptOutIds
        val parentCityNames = franchises.map { it.cityName.lowercase() }.toSet()
        val feederCatalog = getFeederMarketCatalog()
        val usedMarketIds = mutableSetOf<String>()
        val usedMarketNames = parentCityNames.toMutableSet()
        val headlines = mutableListOf<String>()

        // 1. Ensure healthy bench depth players are auto-assigned if not manually opted out & not starter
        val syncedFranchises = franchises.map { team ->
            val updatedRoster = team.roster.map { player ->
                when {
                    player.isStarter || player.isMedicallyOut -> {
                        if (player.isAssignedToMinorLeague) player.copy(isAssignedToMinorLeague = false) else player
                    }
                    player.id in optedOutIds -> player
                    !player.isAssignedToMinorLeague -> player.copy(isAssignedToMinorLeague = true)
                    else -> player
                }
            }
            if (updatedRoster != team.roster) team.copy(roster = updatedRoster) else team
        }

        // Preserve non-colliding markets of existing affiliates first
        existingByParent.values.forEach { aff ->
            if (aff.cityName.lowercase() !in parentCityNames) {
                usedMarketIds.add(aff.cityId)
                usedMarketNames.add(aff.cityName.lowercase())
            }
        }

        val syncedAffiliates = syncedFranchises.mapIndexed { idx, team ->
            val existing = existingByParent[team.id]
            val needsNewMarket = existing == null || existing.cityName.lowercase() in parentCityNames
            val chosenMarket = if (needsNewMarket) {
                val prefConfs = feederCatalog.filter {
                    it.id !in usedMarketIds &&
                        it.cityName.lowercase() !in usedMarketNames &&
                        it.preferredConference == team.conference
                }
                val fallback = feederCatalog.filter {
                    it.id !in usedMarketIds && it.cityName.lowercase() !in usedMarketNames
                }
                val picked = prefConfs.firstOrNull() ?: fallback.firstOrNull() ?: MinorMarketCity(
                    id = "mm_dyn_${team.id}",
                    cityName = "${team.cityName} Metro",
                    region = team.conference.regionLabel,
                    preferredConference = team.conference,
                    populationMillions = 0.85,
                    sportsAppetite = 78,
                    arenaName = "${team.nickname} Development Center",
                    arenaCapacity = 6200,
                    marketTierLabel = "Regional Dev Hub"
                )
                usedMarketIds.add(picked.id)
                usedMarketNames.add(picked.cityName.lowercase())
                picked
            } else null

            val depthPlayers = getParentAssignedDepthPlayers(team)
            val safeSeed = (team.id.hashCode() and 0x7FFF) + idx
            val supplemental = buildSupplementalProspectsForAffiliate(
                affiliateId = existing?.id ?: "minor_${team.id}",
                coveredPositions = depthPlayers.map { it.position }.toSet(),
                existingProspects = existing?.supplementalProspects ?: emptyList(),
                sportType = sportType,
                seed = safeSeed
            )

            if (existing != null && !needsNewMarket) {
                existing.copy(
                    parentFranchiseName = team.teamFullName,
                    colorHex = team.colorHex,
                    conference = team.conference,
                    division = team.division,
                    supplementalProspects = supplemental
                )
            } else if (existing != null && chosenMarket != null) {
                headlines.add(
                    "${sportType.minorLeagueName.uppercase()} MARKET SHIFT: ${team.teamFullName} affiliate relocates feeder home to ${chosenMarket.cityName} (${chosenMarket.arenaName})."
                )
                existing.copy(
                    parentFranchiseName = team.teamFullName,
                    cityId = chosenMarket.id,
                    cityName = chosenMarket.cityName,
                    conference = team.conference,
                    division = team.division,
                    marketTierLabel = chosenMarket.marketTierLabel,
                    arenaName = chosenMarket.arenaName,
                    arenaCapacity = chosenMarket.arenaCapacity,
                    populationMillions = chosenMarket.populationMillions,
                    marketAppetite = chosenMarket.sportsAppetite,
                    fanBaseThousands = (chosenMarket.populationMillions * 95.0).roundToInt().coerceIn(75, 290),
                    supplementalProspects = supplemental
                )
            } else {
                val market = chosenMarket!!
                val nick = feederNicknames[Math.floorMod(idx * 3 + safeSeed, feederNicknames.size)]
                val isExp = existingByParent.isNotEmpty()
                val ticket = when {
                    market.populationMillions >= 2.0 -> 28
                    market.populationMillions >= 1.2 -> 24
                    else -> 20
                }
                if (isExp) {
                    headlines.add(
                        "${sportType.minorLeagueName.uppercase()} EXPANSION: ${team.teamFullName} launch new affiliate ${market.cityName} $nick (${market.arenaCapacity} cap • ${market.marketTierLabel})!"
                    )
                }
                MinorLeagueAffiliate(
                    id = "minor_${team.id}",
                    parentFranchiseId = team.id,
                    parentFranchiseName = team.teamFullName,
                    cityId = market.id,
                    cityName = market.cityName,
                    nickname = nick,
                    colorHex = team.colorHex,
                    conference = team.conference,
                    division = team.division,
                    marketTierLabel = market.marketTierLabel,
                    arenaName = market.arenaName,
                    arenaCapacity = market.arenaCapacity,
                    populationMillions = market.populationMillions,
                    ticketPrice = ticket,
                    fanBaseThousands = (market.populationMillions * 95.0).roundToInt().coerceIn(75, 290),
                    fanLoyalty = (68 + (team.fanLoyalty - 75) / 2).coerceIn(58, 92),
                    marketAppetite = market.sportsAppetite,
                    isExpansionAffiliate = isExp,
                    foundedYear = seasonYear,
                    finances = MinorAffiliateFinances(
                        affiliateValuationMillions = round2(15.0 + market.populationMillions * 4.2 + (market.sportsAppetite - 70) * 0.25)
                    ),
                    supplementalProspects = supplemental
                )
            }
        }

        val scaledPlayoffSize = calculateScalingPlayoffFieldSize(syncedAffiliates.size)
        if (baseState.lastSyncedParentTeamCount > 0 && syncedAffiliates.size > baseState.lastSyncedParentTeamCount && scaledPlayoffSize != baseState.playoffFieldSize) {
            headlines.add(
                "${sportType.minorLeagueName.uppercase()} SCALING: Development playoff bracket expanded to $scaledPlayoffSize affiliates to match ${syncedAffiliates.size}-club league growth!"
            )
        }

        val updatedState = baseState.copy(
            leagueName = sportType.minorLeagueName,
            affiliates = syncedAffiliates,
            playoffFieldSize = scaledPlayoffSize,
            lastSyncedParentTeamCount = syncedFranchises.size
        )

        return MinorLeagueSyncResult(
            updatedFranchises = syncedFranchises,
            minorState = updatedState,
            headlines = headlines
        )
    }

    private fun buildSupplementalProspectsForAffiliate(
        affiliateId: String,
        coveredPositions: Set<String>,
        existingProspects: List<MinorAffiliateProspect>,
        sportType: SportType,
        seed: Int
    ): List<MinorAffiliateProspect> {
        val neededPositions = sportType.starterPositions.filter { it !in coveredPositions }
            .ifEmpty { listOf(sportType.starterPositions[1], sportType.starterPositions[3]) }
            .take(maxOf(2, 5 - coveredPositions.size))

        val existingByPos = existingProspects.filter { it.position in sportType.starterPositions }.associateBy { it.position }
        val archetypes = sportType.archetypes
        return neededPositions.mapIndexed { idx, pos ->
            val prev = existingByPos[pos]
            if (prev != null) {
                prev
            } else {
                val baseSeed = seed and 0x7FFF
                val fn = prospectFirstNames[Math.floorMod(baseSeed + idx * 3, prospectFirstNames.size)]
                val ln = prospectLastNames[Math.floorMod(baseSeed * 2 + idx * 5, prospectLastNames.size)]
                val ovr = 68 + Math.floorMod(baseSeed + idx, 6)
                MinorAffiliateProspect(
                    id = "minp_${affiliateId}_$pos",
                    affiliateId = affiliateId,
                    name = "$fn $ln",
                    position = pos,
                    overallRating = ovr,
                    age = 19 + Math.floorMod(baseSeed + idx, 4),
                    archetype = archetypes[Math.floorMod(baseSeed + idx, archetypes.size)],
                    salaryMillions = round2(0.18 + (ovr - 66) * 0.04)
                )
            }
        }
    }

    fun togglePlayerMinorAssignment(
        franchises: List<Franchise>,
        currentMinorState: MinorLeagueState,
        franchiseId: String,
        playerId: String,
        assignToMinor: Boolean,
        sportType: SportType
    ): MinorLeagueSyncResult {
        val updatedOptOut = currentMinorState.manualParentBenchOptOutIds.toMutableSet()
        if (assignToMinor) {
            updatedOptOut.remove(playerId)
        } else {
            updatedOptOut.add(playerId)
        }

        val updatedFranchises = franchises.map { team ->
            if (team.id != franchiseId) team
            else team.copy(
                roster = team.roster.map { p ->
                    if (p.id == playerId && !p.isStarter && !p.isMedicallyOut) {
                        p.copy(isAssignedToMinorLeague = assignToMinor)
                    } else p
                }
            )
        }

        val playerName = updatedFranchises.find { it.id == franchiseId }?.roster?.find { it.id == playerId }?.name ?: "Depth Prospect"
        val affName = currentMinorState.affiliates.find { it.parentFranchiseId == franchiseId }?.fullName ?: sportType.minorLeagueName
        val teamName = updatedFranchises.find { it.id == franchiseId }?.teamFullName ?: "Parent Club"
        val headline = if (assignToMinor) {
            "MINOR ASSIGNMENT: $teamName assign $playerName to $affName for developmental minutes."
        } else {
            "PARENT RECALL: $teamName recall $playerName from $affName to active parent bench rotation!"
        }

        val base = currentMinorState.copy(manualParentBenchOptOutIds = updatedOptOut)
        val syncResult = syncAffiliatesWithParentLeague(updatedFranchises, base, sportType, 2026)
        return syncResult.copy(headlines = listOf(headline) + syncResult.headlines)
    }

    fun updateAffiliateTicketPrice(
        currentMinorState: MinorLeagueState,
        affiliateId: String,
        newPrice: Int
    ): Pair<MinorLeagueState, String?> {
        val clamped = newPrice.coerceIn(12, 48)
        var msg: String? = null
        val updated = currentMinorState.affiliates.map { aff ->
            if (aff.id == affiliateId) {
                val delta = when {
                    clamped <= 20 && aff.ticketPrice > 20 -> 2
                    clamped >= 36 && aff.ticketPrice < 36 -> -3
                    else -> 0
                }
                val newLoyalty = (aff.fanLoyalty + delta).coerceIn(35, 99)
                msg = "${aff.fullName} set feeder ticket price to \$${clamped} (Loyalty $newLoyalty/100)."
                aff.copy(ticketPrice = clamped, fanLoyalty = newLoyalty)
            } else aff
        }
        return currentMinorState.copy(affiliates = updated) to msg
    }

    fun simulateRegularSeasonWeek(
        currentMinorState: MinorLeagueState,
        franchises: List<Franchise>,
        sportType: SportType,
        week: Int,
        seasonYear: Int,
        gameLength: GameLengthRule = GameLengthRule.STANDARD_48,
        random: Random = Random.Default
    ): MinorLeagueWeekResult {
        val synced = syncAffiliatesWithParentLeague(franchises, currentMinorState, sportType, seasonYear)
        val affiliates = synced.minorState.affiliates
        if (affiliates.size < 2) {
            return MinorLeagueWeekResult(synced.updatedFranchises, synced.minorState, synced.headlines)
        }

        val parentMap = synced.updatedFranchises.associateBy { it.id }
        val shuffled = affiliates.shuffled(Random(seasonYear * 313 + week * 97))
        val matches = mutableListOf<MinorLeagueMatch>()
        val updatedAffiliates = affiliates.toMutableList()
        val updatedStats = synced.minorState.playerStats.toMutableMap()
        val updatedFranchises = synced.updatedFranchises.toMutableList()
        val headlines = synced.headlines.toMutableList()
        var weeklyBestStat: Triple<MinorLeaguePlayerStat, Int, String>? = null

        for (i in 0 until shuffled.size - 1 step 2) {
            val homeAff = shuffled[i]
            val awayAff = shuffled[i + 1]
            val homeParent = parentMap[homeAff.parentFranchiseId]
            val awayParent = parentMap[awayAff.parentFranchiseId]

            val homeStrength = computeAffiliateStrength(homeAff, homeParent)
            val awayStrength = computeAffiliateStrength(awayAff, awayParent)
            val (homeScore, awayScore) = simulateScores(sportType, homeStrength, awayStrength, gameLength, random)
            val homeWon = homeScore > awayScore

            val homeAtt = computeMatchAttendance(homeAff, isPlayoff = false, random = random)
            val awayAtt = computeMatchAttendance(awayAff, isPlayoff = false, random = random)
            val homeGate = round2((homeAtt * homeAff.ticketPrice) / 1_000_000.0)
            val awayGate = round2((awayAtt * awayAff.ticketPrice) / 1_000_000.0)

            // Process player box scores & OVR progression
            val homeLeaders = recordAffiliateMatchPerformance(
                affiliate = homeAff,
                parent = homeParent,
                sportType = sportType,
                teamScore = homeScore,
                won = homeWon,
                updatedStats = updatedStats,
                updatedFranchises = updatedFranchises,
                breakthroughHeadlines = headlines,
                random = random
            )
            val awayLeaders = recordAffiliateMatchPerformance(
                affiliate = awayAff,
                parent = awayParent,
                sportType = sportType,
                teamScore = awayScore,
                won = !homeWon,
                updatedStats = updatedStats,
                updatedFranchises = updatedFranchises,
                breakthroughHeadlines = headlines,
                random = random
            )

            listOfNotNull(homeLeaders, awayLeaders).forEach { cand ->
                if (weeklyBestStat == null || cand.second > weeklyBestStat!!.second) {
                    weeklyBestStat = cand
                }
            }

            val topLine = listOfNotNull(homeLeaders, awayLeaders).maxByOrNull { it.second }?.third ?: "$homeScore-$awayScore finish"

            val match = MinorLeagueMatch(
                id = "min_w${week}_${homeAff.id}_${awayAff.id}",
                week = week,
                homeAffiliateId = homeAff.id,
                homeAffiliateName = homeAff.fullName,
                awayAffiliateId = awayAff.id,
                awayAffiliateName = awayAff.fullName,
                homeScore = homeScore,
                awayScore = awayScore,
                attendance = homeAtt,
                gateRevenueMillions = homeGate,
                isPlayed = true,
                isPlayoff = false,
                roundName = "Week $week Circuit",
                topPerformerSummary = topLine
            )
            matches.add(match)

            // Apply standing + weekly finances to both affiliates
            updateAffiliateFinancialsAndRecord(
                affIndex = updatedAffiliates.indexOfFirst { it.id == homeAff.id },
                updatedAffiliates = updatedAffiliates,
                parent = homeParent,
                totalClubs = affiliates.size,
                won = homeWon,
                pointsFor = homeScore,
                pointsAgainst = awayScore,
                gateRev = homeGate,
                attendance = homeAtt,
                isHome = true,
                ovrBreakthroughsCount = homeLeaders?.first?.let { 0 } ?: 0 // counted cleanly below
            )
            updateAffiliateFinancialsAndRecord(
                affIndex = updatedAffiliates.indexOfFirst { it.id == awayAff.id },
                updatedAffiliates = updatedAffiliates,
                parent = awayParent,
                totalClubs = affiliates.size,
                won = !homeWon,
                pointsFor = awayScore,
                pointsAgainst = homeScore,
                gateRev = awayGate * 0.15,
                attendance = (awayAtt * 0.12).roundToInt(),
                isHome = false,
                ovrBreakthroughsCount = 0
            )
        }

        // Sync OVR breakthrough tallies per affiliate
        val breakthroughsByAff = updatedStats.values.groupBy { it.affiliateId }.mapValues { (_, list) -> list.sumOf { it.ovrGainsEarned } }
        val finalAffiliates = updatedAffiliates.map { aff ->
            val loyaltyDelta = if (matches.any { it.homeAffiliateId == aff.id && it.homeScore > it.awayScore }) 1 else 0
            aff.copy(
                fanLoyalty = (aff.fanLoyalty + loyaltyDelta).coerceIn(40, 99),
                ovrBreakthroughsThisSeason = breakthroughsByAff[aff.id] ?: aff.ovrBreakthroughsThisSeason
            )
        }

        val weeklyAward = weeklyBestStat?.let { (stat, impact, summary) ->
            MinorLeagueWeeklyAward(
                week = week,
                seasonYear = seasonYear,
                playerId = stat.playerId,
                playerName = stat.playerName,
                position = stat.position,
                affiliateName = stat.affiliateName,
                parentFranchiseName = stat.parentFranchiseName,
                statSummary = summary,
                impactScore = impact
            )
        }
        val updatedWeeklyAwards = if (weeklyAward != null) {
            listOf(weeklyAward) + synced.minorState.weeklyAwards
        } else synced.minorState.weeklyAwards

        return MinorLeagueWeekResult(
            updatedFranchises = updatedFranchises,
            minorState = synced.minorState.copy(
                affiliates = finalAffiliates,
                currentWeekMatches = matches,
                playerStats = updatedStats,
                weeklyAwards = updatedWeeklyAwards.take(24)
            ),
            headlines = headlines
        )
    }

    fun advancePlayoffRound(
        currentMinorState: MinorLeagueState,
        franchises: List<Franchise>,
        sportType: SportType,
        week: Int,
        seasonYear: Int,
        completeAllRemainingRounds: Boolean = false,
        random: Random = Random.Default
    ): MinorLeagueWeekResult {
        val synced = syncAffiliatesWithParentLeague(franchises, currentMinorState, sportType, seasonYear)
        var state = synced.minorState
        val updatedFranchises = synced.updatedFranchises.toMutableList()
        val headlines = synced.headlines.toMutableList()

        if (state.playoffChampionAffiliateId != null || state.affiliates.size < 2) {
            return MinorLeagueWeekResult(updatedFranchises, state, headlines)
        }

        // Build opening round bracket if none exists yet
        if (state.playoffMatches.isEmpty()) {
            state = state.copy(playoffMatches = buildOpeningPlayoffBracket(state.affiliates, week))
            headlines.add(
                "${sportType.minorLeagueName.uppercase()} PLAYOFFS: ${state.playoffFieldSize}-team postseason bracket locks across ${state.affiliates.size} clubs!"
            )
        }

        var loopGuard = 0
        var activeWeek = week
        do {
            loopGuard++
            val unplayed = state.playoffMatches.filter { !it.isPlayed }
            if (unplayed.isEmpty()) break

            val parentMap = updatedFranchises.associateBy { it.id }
            val updatedAffiliates = state.affiliates.toMutableList()
            val updatedStats = state.playerStats.toMutableMap()
            val roundWinners = mutableListOf<MinorLeagueAffiliate>()
            var lastRunnerUp: MinorLeagueAffiliate? = null

            val playedRound = unplayed.map { m ->
                val home = updatedAffiliates.find { it.id == m.homeAffiliateId }!!
                val away = updatedAffiliates.find { it.id == m.awayAffiliateId }!!
                val homeParent = parentMap[home.parentFranchiseId]
                val awayParent = parentMap[away.parentFranchiseId]
                val (hScore, aScore) = simulateScores(
                    sportType,
                    computeAffiliateStrength(home, homeParent) + 2,
                    computeAffiliateStrength(away, awayParent),
                    GameLengthRule.STANDARD_48,
                    random
                )
                val homeWon = hScore > aScore
                val winner = if (homeWon) home else away
                val loser = if (homeWon) away else home
                roundWinners.add(winner)
                lastRunnerUp = loser

                val att = computeMatchAttendance(home, isPlayoff = true, random = random)
                val gate = round2((att * home.ticketPrice * 1.35) / 1_000_000.0)

                val hLead = recordAffiliateMatchPerformance(
                    affiliate = home, parent = homeParent, sportType = sportType,
                    teamScore = hScore, won = homeWon, updatedStats = updatedStats,
                    updatedFranchises = updatedFranchises, breakthroughHeadlines = headlines,
                    isPlayoffs = true, random = random
                )
                val aLead = recordAffiliateMatchPerformance(
                    affiliate = away, parent = awayParent, sportType = sportType,
                    teamScore = aScore, won = !homeWon, updatedStats = updatedStats,
                    updatedFranchises = updatedFranchises, breakthroughHeadlines = headlines,
                    isPlayoffs = true, random = random
                )
                val topLine = listOfNotNull(hLead, aLead).maxByOrNull { it.second }?.third ?: "$hScore-$aScore playoff thriller"

                val hIdx = updatedAffiliates.indexOfFirst { it.id == home.id }
                if (hIdx != -1) {
                    val hCur = updatedAffiliates[hIdx]
                    updatedAffiliates[hIdx] = hCur.copy(
                        finances = hCur.finances.copy(
                            ticketRevenueMillions = round2(hCur.finances.ticketRevenueMillions + gate),
                            gamesHosted = hCur.finances.gamesHosted + 1,
                            cumulativeAttendance = hCur.finances.cumulativeAttendance + att
                        )
                    )
                }

                m.copy(
                    homeScore = hScore,
                    awayScore = aScore,
                    attendance = att,
                    gateRevenueMillions = gate,
                    isPlayed = true,
                    topPerformerSummary = topLine
                )
            }

            val mergedPlayoffMatches = state.playoffMatches.map { old -> playedRound.find { it.id == old.id } ?: old }
            val currentRoundName = unplayed.first().roundName

            if (currentRoundName == "Minor Finals" || unplayed.size == 1 && mergedPlayoffMatches.size >= 1 && roundWinners.size == 1) {
                val champ = roundWinners.first()
                val runnerUp = lastRunnerUp
                val boostedAffiliates = updatedAffiliates.map { aff ->
                    if (aff.id == champ.id) {
                        aff.copy(
                            championships = aff.championships + 1,
                            fanLoyalty = (aff.fanLoyalty + 6).coerceAtMost(99),
                            finances = aff.finances.copy(
                                affiliateValuationMillions = round2(aff.finances.affiliateValuationMillions + 4.5)
                            )
                        )
                    } else aff
                }
                val annualAwards = computeAnnualAwards(
                    year = seasonYear,
                    sportType = sportType,
                    affiliates = boostedAffiliates,
                    playerStats = updatedStats,
                    championAffiliate = champ
                )
                val champParent = updatedFranchises.find { it.id == champ.parentFranchiseId }
                headlines.add(
                    "${sportType.minorLeagueName.uppercase()} CHAMPION: ${champ.fullName} (${champ.parentFranchiseName}) capture the Minor League Title!"
                )
                annualAwards.firstOrNull { it.type == MinorLeagueAwardType.MINOR_MVP }?.let { mvp ->
                    headlines.add(
                        "${sportType.minorLeagueName.uppercase()} MVP: ${mvp.playerName} (${mvp.affiliateName}) wins Season $seasonYear Honors — ${mvp.statLine}!"
                    )
                }
                val archive = MinorLeagueSeasonArchive(
                    year = seasonYear,
                    leagueName = sportType.minorLeagueName,
                    affiliatesCount = boostedAffiliates.size,
                    playoffFieldSize = state.playoffFieldSize,
                    championAffiliateId = champ.id,
                    championAffiliateName = champ.fullName,
                    championParentName = champParent?.teamFullName ?: champ.parentFranchiseName,
                    runnerUpAffiliateName = runnerUp?.fullName ?: "Finalist",
                    totalAttendance = boostedAffiliates.sumOf { it.finances.cumulativeAttendance },
                    circuitRevenueMillions = round2(boostedAffiliates.sumOf { it.finances.totalRevenueMillions }),
                    awards = annualAwards
                )
                val nextArchives = listOf(archive) + state.pastSeasons.filterNot { it.year == seasonYear }
                state = state.copy(
                    affiliates = boostedAffiliates,
                    playoffMatches = mergedPlayoffMatches,
                    playoffChampionAffiliateId = champ.id,
                    playoffChampionName = champ.fullName,
                    playoffRunnerUpName = runnerUp?.fullName,
                    playerStats = updatedStats,
                    annualAwards = annualAwards,
                    pastSeasons = nextArchives
                )
                break
            } else {
                val nextRoundMatches = buildNextPlayoffRound(
                    currentRoundName = currentRoundName,
                    winners = roundWinners,
                    allAffiliates = updatedAffiliates,
                    fieldSize = state.playoffFieldSize,
                    nextWeek = activeWeek + 1
                )
                state = state.copy(
                    affiliates = updatedAffiliates,
                    playoffMatches = mergedPlayoffMatches + nextRoundMatches,
                    playerStats = updatedStats
                )
                activeWeek++
            }
        } while (completeAllRemainingRounds && loopGuard < 5)

        return MinorLeagueWeekResult(updatedFranchises, state, headlines)
    }

    fun rolloverForNewSeason(
        currentMinorState: MinorLeagueState,
        franchises: List<Franchise>,
        sportType: SportType,
        finishedYear: Int,
        nextYear: Int
    ): MinorLeagueSyncResult {
        // Ensure champion & awards are crowned before archiving if playoff ended early
        val finalized = if (currentMinorState.playoffChampionAffiliateId == null && currentMinorState.affiliates.size >= 2) {
            advancePlayoffRound(
                currentMinorState = currentMinorState,
                franchises = franchises,
                sportType = sportType,
                week = 16,
                seasonYear = finishedYear,
                completeAllRemainingRounds = true
            )
        } else {
            MinorLeagueWeekResult(franchises, currentMinorState, emptyList())
        }

        val resetAffiliates = finalized.minorState.affiliates.map { aff ->
            aff.copy(
                wins = 0,
                losses = 0,
                pointsFor = 0,
                pointsAgainst = 0,
                ovrBreakthroughsThisSeason = 0,
                finances = MinorAffiliateFinances(
                    affiliateValuationMillions = aff.finances.affiliateValuationMillions
                )
            )
        }

        val resetState = finalized.minorState.copy(
            affiliates = resetAffiliates,
            currentWeekMatches = emptyList(),
            playoffMatches = emptyList(),
            playoffChampionAffiliateId = null,
            playoffChampionName = null,
            playoffRunnerUpName = null,
            playerStats = emptyMap(),
            weeklyAwards = emptyList(),
            annualAwards = emptyList()
        )
        return syncAffiliatesWithParentLeague(finalized.updatedFranchises, resetState, sportType, nextYear)
    }

    private fun buildOpeningPlayoffBracket(affiliates: List<MinorLeagueAffiliate>, week: Int): List<MinorLeagueMatch> {
        val ranked = affiliates.sortedWith(
            compareByDescending<MinorLeagueAffiliate> { it.winPct }
                .thenByDescending { it.pointDifferential }
                .thenByDescending { it.pointsFor }
        )
        val fieldSize = calculateScalingPlayoffFieldSize(ranked.size).coerceAtMost(ranked.size)
        val seeds = ranked.take(fieldSize)
        return when {
            fieldSize <= 2 -> listOf(
                MinorLeagueMatch(
                    id = "min_po_finals",
                    week = week,
                    homeAffiliateId = seeds[0].id,
                    homeAffiliateName = seeds[0].fullName,
                    awayAffiliateId = seeds[1].id,
                    awayAffiliateName = seeds[1].fullName,
                    isPlayoff = true,
                    roundName = "Minor Finals"
                )
            )
            fieldSize <= 5 -> listOf(
                MinorLeagueMatch(
                    id = "min_po_semi_1", week = week,
                    homeAffiliateId = seeds[0].id, homeAffiliateName = seeds[0].fullName,
                    awayAffiliateId = seeds[3].id, awayAffiliateName = seeds[3].fullName,
                    isPlayoff = true, roundName = "Minor Semifinals"
                ),
                MinorLeagueMatch(
                    id = "min_po_semi_2", week = week,
                    homeAffiliateId = seeds[1].id, homeAffiliateName = seeds[1].fullName,
                    awayAffiliateId = seeds[2].id, awayAffiliateName = seeds[2].fullName,
                    isPlayoff = true, roundName = "Minor Semifinals"
                )
            )
            fieldSize in 6..7 -> listOf(
                // Seeds #1 and #2 earn first-round byes
                MinorLeagueMatch(
                    id = "min_po_wc_1", week = week,
                    homeAffiliateId = seeds[2].id, homeAffiliateName = seeds[2].fullName,
                    awayAffiliateId = seeds[5].id, awayAffiliateName = seeds[5].fullName,
                    isPlayoff = true, roundName = "Minor Wild Card"
                ),
                MinorLeagueMatch(
                    id = "min_po_wc_2", week = week,
                    homeAffiliateId = seeds[3].id, homeAffiliateName = seeds[3].fullName,
                    awayAffiliateId = seeds[4].id, awayAffiliateName = seeds[4].fullName,
                    isPlayoff = true, roundName = "Minor Wild Card"
                )
            )
            else -> listOf(
                MinorLeagueMatch(
                    id = "min_po_qf_1", week = week,
                    homeAffiliateId = seeds[0].id, homeAffiliateName = seeds[0].fullName,
                    awayAffiliateId = seeds[7].id, awayAffiliateName = seeds[7].fullName,
                    isPlayoff = true, roundName = "Minor Quarterfinals"
                ),
                MinorLeagueMatch(
                    id = "min_po_qf_2", week = week,
                    homeAffiliateId = seeds[1].id, homeAffiliateName = seeds[1].fullName,
                    awayAffiliateId = seeds[6].id, awayAffiliateName = seeds[6].fullName,
                    isPlayoff = true, roundName = "Minor Quarterfinals"
                ),
                MinorLeagueMatch(
                    id = "min_po_qf_3", week = week,
                    homeAffiliateId = seeds[2].id, homeAffiliateName = seeds[2].fullName,
                    awayAffiliateId = seeds[5].id, awayAffiliateName = seeds[5].fullName,
                    isPlayoff = true, roundName = "Minor Quarterfinals"
                ),
                MinorLeagueMatch(
                    id = "min_po_qf_4", week = week,
                    homeAffiliateId = seeds[3].id, homeAffiliateName = seeds[3].fullName,
                    awayAffiliateId = seeds[4].id, awayAffiliateName = seeds[4].fullName,
                    isPlayoff = true, roundName = "Minor Quarterfinals"
                )
            )
        }
    }

    private fun buildNextPlayoffRound(
        currentRoundName: String,
        winners: List<MinorLeagueAffiliate>,
        allAffiliates: List<MinorLeagueAffiliate>,
        fieldSize: Int,
        nextWeek: Int
    ): List<MinorLeagueMatch> {
        val ranked = allAffiliates.sortedWith(
            compareByDescending<MinorLeagueAffiliate> { it.winPct }
                .thenByDescending { it.pointDifferential }
        )
        return when (currentRoundName) {
            "Minor Wild Card" -> {
                val seed1 = ranked[0]
                val seed2 = ranked[1]
                val w0 = winners.getOrElse(0) { ranked[2] }
                val w1 = winners.getOrElse(1) { ranked[3] }
                listOf(
                    MinorLeagueMatch(
                        id = "min_po_semi_1", week = nextWeek,
                        homeAffiliateId = seed1.id, homeAffiliateName = seed1.fullName,
                        awayAffiliateId = w1.id, awayAffiliateName = w1.fullName,
                        isPlayoff = true, roundName = "Minor Semifinals"
                    ),
                    MinorLeagueMatch(
                        id = "min_po_semi_2", week = nextWeek,
                        homeAffiliateId = seed2.id, homeAffiliateName = seed2.fullName,
                        awayAffiliateId = w0.id, awayAffiliateName = w0.fullName,
                        isPlayoff = true, roundName = "Minor Semifinals"
                    )
                )
            }
            "Minor Quarterfinals" -> {
                val f0 = winners.getOrElse(0) { ranked[0] }
                val f1 = winners.getOrElse(1) { ranked[1] }
                val f2 = winners.getOrElse(2) { ranked[2] }
                val f3 = winners.getOrElse(3) { ranked[3] }
                listOf(
                    MinorLeagueMatch(
                        id = "min_po_semi_1", week = nextWeek,
                        homeAffiliateId = f0.id, homeAffiliateName = f0.fullName,
                        awayAffiliateId = f3.id, awayAffiliateName = f3.fullName,
                        isPlayoff = true, roundName = "Minor Semifinals"
                    ),
                    MinorLeagueMatch(
                        id = "min_po_semi_2", week = nextWeek,
                        homeAffiliateId = f1.id, homeAffiliateName = f1.fullName,
                        awayAffiliateId = f2.id, awayAffiliateName = f2.fullName,
                        isPlayoff = true, roundName = "Minor Semifinals"
                    )
                )
            }
            else -> {
                val h = winners.getOrElse(0) { ranked[0] }
                val a = winners.getOrElse(1) { ranked[1] }
                listOf(
                    MinorLeagueMatch(
                        id = "min_po_finals", week = nextWeek,
                        homeAffiliateId = h.id, homeAffiliateName = h.fullName,
                        awayAffiliateId = a.id, awayAffiliateName = a.fullName,
                        isPlayoff = true, roundName = "Minor Finals"
                    )
                )
            }
        }
    }

    private fun computeAffiliateStrength(affiliate: MinorLeagueAffiliate, parent: Franchise?): Int {
        val assigned = if (parent != null) getParentAssignedDepthPlayers(parent) else emptyList()
        val combinedRatings = assigned.map { it.overallRating } + affiliate.supplementalProspects.map { it.overallRating }
        val avg = if (combinedRatings.isNotEmpty()) combinedRatings.average().roundToInt() else 71
        val facilityBoost = parent?.facilities?.trainingTier ?: 1
        return avg + facilityBoost
    }

    private fun simulateScores(
        sportType: SportType,
        homeStrength: Int,
        awayStrength: Int,
        gameLength: GameLengthRule,
        random: Random
    ): Pair<Int, Int> {
        val lengthScale = gameLength.scoreScaleMultiplier
        return when (sportType) {
            SportType.BASKETBALL -> {
                val h = ((86 + (homeStrength - 70) * 0.6 + random.nextInt(-8, 10)) * lengthScale).roundToInt().coerceAtLeast(58)
                val rawA = ((84 + (awayStrength - 70) * 0.6 + random.nextInt(-8, 10)) * lengthScale).roundToInt().coerceAtLeast(56)
                val a = if (rawA == h) rawA + (if (homeStrength >= awayStrength) -2 else 2) else rawA
                h to a
            }
            SportType.FOOTBALL -> {
                val h = ((19 + (homeStrength - 70) * 0.4 + random.nextInt(-7, 9)) * lengthScale).roundToInt().coerceAtLeast(6)
                val rawA = ((17 + (awayStrength - 70) * 0.4 + random.nextInt(-7, 9)) * lengthScale).roundToInt().coerceAtLeast(3)
                val a = if (rawA == h) rawA + (if (homeStrength >= awayStrength) -3 else 3) else rawA
                h to a
            }
            SportType.SOCCER -> {
                val h = ((1.8 + (homeStrength - 70) * 0.05 + random.nextDouble(-1.0, 1.8)) * lengthScale).roundToInt().coerceIn(0, 6)
                val rawA = ((1.5 + (awayStrength - 70) * 0.05 + random.nextDouble(-1.0, 1.7)) * lengthScale).roundToInt().coerceIn(0, 5)
                val a = if (rawA == h) (if (homeStrength >= awayStrength) maxOf(0, rawA - 1) else rawA + 1) else rawA
                val finalH = if (h == a) h + 1 else h
                finalH to a
            }
            SportType.HOCKEY -> {
                val h = ((3.1 + (homeStrength - 70) * 0.08 + random.nextDouble(-1.5, 2.2)) * lengthScale).roundToInt().coerceIn(1, 7)
                val rawA = ((2.7 + (awayStrength - 70) * 0.08 + random.nextDouble(-1.5, 2.1)) * lengthScale).roundToInt().coerceIn(1, 6)
                val a = if (rawA == h) rawA + (if (homeStrength >= awayStrength) -1 else 1) else rawA
                val finalA = a.coerceAtLeast(0)
                val finalH = if (h == finalA) h + 1 else h
                finalH to finalA
            }
        }
    }

    private fun computeMatchAttendance(affiliate: MinorLeagueAffiliate, isPlayoff: Boolean, random: Random): Int {
        val baseRate = (0.48 + ((affiliate.fanLoyalty - 60) * 0.006) + ((affiliate.marketAppetite - 70) * 0.004) +
            (affiliate.winPct * 0.12) + (if (isPlayoff) 0.18 else 0.0) + random.nextDouble(-0.04, 0.05))
            .coerceIn(0.38, 0.99)
        return (affiliate.arenaCapacity * baseRate).roundToInt()
    }

    private fun recordAffiliateMatchPerformance(
        affiliate: MinorLeagueAffiliate,
        parent: Franchise?,
        sportType: SportType,
        teamScore: Int,
        won: Boolean,
        updatedStats: MutableMap<String, MinorLeaguePlayerStat>,
        updatedFranchises: MutableList<Franchise>,
        breakthroughHeadlines: MutableList<String>,
        isPlayoffs: Boolean = false,
        random: Random
    ): Triple<MinorLeaguePlayerStat, Int, String>? {
        val labels = RecordBookEngine.getStatLabels(sportType)
        val depthPlayers = if (parent != null) getParentAssignedDepthPlayers(parent) else emptyList()
        val trainingTier = parent?.facilities?.trainingTier ?: 1

        data class UnitParticipant(
            val id: String,
            val name: String,
            val pos: String,
            val ovr: Int,
            val age: Int,
            val isParentDepth: Boolean
        )

        val unit = depthPlayers.map { p ->
            UnitParticipant(p.id, p.name, p.position, p.overallRating, p.age, true)
        } + affiliate.supplementalProspects.map { sp ->
            UnitParticipant(sp.id, sp.name, sp.position, sp.overallRating, sp.age, false)
        }
        if (unit.isEmpty()) return null

        var bestCandidate: Triple<MinorLeaguePlayerStat, Int, String>? = null
        val pIdx = updatedFranchises.indexOfFirst { it.id == affiliate.parentFranchiseId }

        unit.take(5).forEachIndexed { idx, actor ->
            val ovrFactor = (actor.ovr - 64).coerceAtLeast(3)
            val prim = when (sportType) {
                SportType.BASKETBALL -> ((teamScore / 5.2) + ovrFactor * 0.45 + random.nextInt(-3, 7)).roundToInt().coerceIn(6, 36)
                SportType.FOOTBALL -> (6 + (ovrFactor / 2) + random.nextInt(0, 10)).coerceIn(0, 24)
                SportType.SOCCER, SportType.HOCKEY -> if (idx == 0 || random.nextInt(100) < 35) 1 + if (teamScore >= 3 && random.nextBoolean()) 1 else 0 else 0
            }
            val sec = when (sportType) {
                SportType.BASKETBALL -> (3 + ovrFactor / 4 + random.nextInt(0, 6)).coerceIn(1, 15)
                SportType.FOOTBALL -> (45 + ovrFactor * 7 + random.nextInt(0, 65)).coerceIn(15, 290)
                else -> (2 + ovrFactor / 4 + random.nextInt(0, 5)).coerceIn(1, 11)
            }
            val tert = when (sportType) {
                SportType.BASKETBALL -> (2 + ovrFactor / 4 + random.nextInt(0, 6)).coerceIn(0, 13)
                SportType.FOOTBALL -> (1 + (prim / 7) + random.nextInt(0, 3)).coerceIn(0, 5)
                else -> (random.nextInt(0, 3) + if (idx <= 1) 1 else 0).coerceIn(0, 4)
            }
            val def = (1 + ovrFactor / 6 + random.nextInt(0, 4)).coerceIn(0, 7)

            val prev = updatedStats[actor.id]
            val xpGain = if (actor.isParentDepth) {
                18 + (trainingTier * 5) + (if (won) 5 else 0) + (if (actor.age <= 24) 6 else 0) + (prim / 6)
            } else 0

            val rawXp = (prev?.devXp ?: 0) + xpGain
            val earnedLevelUp = actor.isParentDepth && actor.ovr < 88 && rawXp >= 100
            val nextXp = if (earnedLevelUp) rawXp - 100 else rawXp
            val nextOvr = if (earnedLevelUp) actor.ovr + 1 else actor.ovr
            val nextGains = (prev?.ovrGainsEarned ?: 0) + if (earnedLevelUp) 1 else 0

            if (earnedLevelUp && pIdx != -1) {
                val curTeam = updatedFranchises[pIdx]
                val updatedRoster = curTeam.roster.map { rp ->
                    if (rp.id == actor.id) {
                        rp.copy(
                            overallRating = nextOvr,
                            minorLeagueOvrProgress = rp.minorLeagueOvrProgress + 1
                        )
                    } else rp
                }
                val newTeamRating = if (updatedRoster.isNotEmpty()) {
                    updatedRoster.sortedByDescending { it.overallRating }.take(5).map { it.overallRating }.average().roundToInt()
                } else curTeam.rosterRating
                updatedFranchises[pIdx] = curTeam.copy(
                    roster = updatedRoster,
                    rosterRating = maxOf(curTeam.rosterRating, newTeamRating)
                )
                breakthroughHeadlines.add(
                    "DEVELOPMENT BREAKTHROUGH (${affiliate.fullName}): ${actor.name} (${actor.pos}, ${curTeam.teamFullName}) surged to $nextOvr OVR (+1) in ${sportType.minorLeagueName}!"
                )
            }

            val updatedStat = MinorLeaguePlayerStat(
                playerId = actor.id,
                playerName = actor.name,
                position = actor.pos,
                affiliateId = affiliate.id,
                affiliateName = affiliate.fullName,
                parentFranchiseId = affiliate.parentFranchiseId,
                parentFranchiseName = affiliate.parentFranchiseName,
                isParentDepthPlayer = actor.isParentDepth,
                overallRating = nextOvr,
                age = actor.age,
                gamesPlayed = (prev?.gamesPlayed ?: 0) + 1,
                totalPrimaryStat = (prev?.totalPrimaryStat ?: 0) + prim,
                totalSecondaryStat = (prev?.totalSecondaryStat ?: 0) + sec,
                totalTertiaryStat = (prev?.totalTertiaryStat ?: 0) + tert,
                totalDefenseStat = (prev?.totalDefenseStat ?: 0) + def,
                playoffPrimaryStat = (prev?.playoffPrimaryStat ?: 0) + if (isPlayoffs) prim else 0,
                devXp = nextXp,
                ovrGainsEarned = nextGains
            )
            updatedStats[actor.id] = updatedStat

            val impact = when (sportType) {
                SportType.BASKETBALL -> prim + sec + tert * 2 + def * 2 + if (won) 5 else 0
                SportType.FOOTBALL -> prim * 2 + sec / 10 + tert * 4 + def * 3 + if (won) 5 else 0
                else -> prim * 10 + tert * 7 + sec * 2 + def * 2 + if (won) 5 else 0
            }
            val summary = "${actor.name}: $prim ${labels.primaryShort} • $sec ${labels.secondaryShort} • $tert ${labels.tertiaryShort}"
            if (bestCandidate == null || impact > bestCandidate!!.second) {
                bestCandidate = Triple(updatedStat, impact, summary)
            }
        }
        return bestCandidate
    }

    private fun updateAffiliateFinancialsAndRecord(
        affIndex: Int,
        updatedAffiliates: MutableList<MinorLeagueAffiliate>,
        parent: Franchise?,
        totalClubs: Int,
        won: Boolean,
        pointsFor: Int,
        pointsAgainst: Int,
        gateRev: Double,
        attendance: Int,
        isHome: Boolean,
        ovrBreakthroughsCount: Int
    ) {
        if (affIndex == -1) return
        val aff = updatedAffiliates[affIndex]
        val scaleFactor = 1.0 + ((totalClubs - 6).coerceAtLeast(0) * 0.04)
        val weeklySponsor = round2((0.06 + aff.populationMillions * 0.035) * scaleFactor)
        val weeklyStream = round2(0.055 * scaleFactor)
        val parentStipend = round2(0.05 + ((parent?.facilities?.trainingTier ?: 1) * 0.015))
        val weeklyPayroll = round2(0.11 + (aff.supplementalProspects.sumOf { it.salaryMillions } / 14.0))
        val travelCost = if (aff.conference.isContinentalInternational) 0.085 else 0.055
        val weeklyOps = round2(travelCost + (aff.arenaCapacity * 0.000005))

        val curFin = aff.finances
        val nextFin = curFin.copy(
            ticketRevenueMillions = round2(curFin.ticketRevenueMillions + gateRev),
            localSponsorMillions = round2(curFin.localSponsorMillions + weeklySponsor),
            streamingRevenueMillions = round2(curFin.streamingRevenueMillions + weeklyStream),
            parentSubsidyMillions = round2(curFin.parentSubsidyMillions + parentStipend),
            payrollCostMillions = round2(curFin.payrollCostMillions + weeklyPayroll),
            operationsAndTravelMillions = round2(curFin.operationsAndTravelMillions + weeklyOps),
            gamesHosted = curFin.gamesHosted + if (isHome) 1 else 0,
            cumulativeAttendance = curFin.cumulativeAttendance + if (isHome) attendance else 0,
            affiliateValuationMillions = round2(
                curFin.affiliateValuationMillions + if (won) 0.15 else 0.03
            )
        )

        updatedAffiliates[affIndex] = aff.copy(
            wins = aff.wins + if (won) 1 else 0,
            losses = aff.losses + if (!won) 1 else 0,
            pointsFor = aff.pointsFor + pointsFor,
            pointsAgainst = aff.pointsAgainst + pointsAgainst,
            finances = nextFin
        )
    }

    private fun computeAnnualAwards(
        year: Int,
        sportType: SportType,
        affiliates: List<MinorLeagueAffiliate>,
        playerStats: Map<String, MinorLeaguePlayerStat>,
        championAffiliate: MinorLeagueAffiliate?
    ): List<MinorLeagueAward> {
        val labels = RecordBookEngine.getStatLabels(sportType)
        val affById = affiliates.associateBy { it.id }

        // Ensure fallback candidates exist even if season jumped directly to finals in tests
        val candidates = if (playerStats.isNotEmpty()) {
            playerStats.values.toList()
        } else {
            affiliates.flatMap { aff ->
                aff.supplementalProspects.map { sp ->
                    MinorLeaguePlayerStat(
                        playerId = sp.id,
                        playerName = sp.name,
                        position = sp.position,
                        affiliateId = aff.id,
                        affiliateName = aff.fullName,
                        parentFranchiseId = aff.parentFranchiseId,
                        parentFranchiseName = aff.parentFranchiseName,
                        isParentDepthPlayer = false,
                        overallRating = sp.overallRating,
                        age = sp.age,
                        gamesPlayed = (aff.wins + aff.losses).coerceAtLeast(10),
                        totalPrimaryStat = if (sportType == SportType.BASKETBALL) 185 else 18,
                        totalSecondaryStat = 64,
                        totalTertiaryStat = 52,
                        totalDefenseStat = 22
                    )
                }
            }
        }
        if (candidates.isEmpty()) return emptyList()

        fun mvpScore(s: MinorLeaguePlayerStat): Double {
            val winPct = affById[s.affiliateId]?.winPct ?: 0.5
            return when (sportType) {
                SportType.BASKETBALL -> s.primaryPerGame * 1.6 + s.secondaryPerGame + s.tertiaryPerGame * 1.4 + s.ovrGainsEarned * 8.0 + winPct * 20.0
                SportType.FOOTBALL -> s.primaryPerGame * 2.0 + s.secondaryPerGame * 0.08 + s.tertiaryPerGame * 2.2 + winPct * 20.0
                else -> s.primaryPerGame * 12.0 + s.tertiaryPerGame * 8.0 + s.secondaryPerGame * 1.5 + winPct * 20.0
            }
        }

        val awards = mutableListOf<MinorLeagueAward>()
        candidates.maxByOrNull { mvpScore(it) }?.let { mvp ->
            awards.add(
                MinorLeagueAward(
                    id = "min_aw_${year}_mvp",
                    year = year,
                    type = MinorLeagueAwardType.MINOR_MVP,
                    playerId = mvp.playerId,
                    playerName = mvp.playerName,
                    position = mvp.position,
                    affiliateId = mvp.affiliateId,
                    affiliateName = mvp.affiliateName,
                    parentFranchiseName = mvp.parentFranchiseName,
                    statLine = "${mvp.primaryPerGame} ${labels.primaryShort}/G • ${mvp.secondaryPerGame} ${labels.secondaryShort}/G • ${mvp.tertiaryPerGame} ${labels.tertiaryShort}/G",
                    narrative = "Spearheaded ${mvp.affiliateName} (${mvp.parentFranchiseName} system) with premier minor-circuit consistency."
                )
            )
        }

        val prospectPool = candidates.filter { it.isParentDepthPlayer }.ifEmpty { candidates }
        prospectPool.maxByOrNull { (it.ovrGainsEarned * 30.0) + (28 - it.age).coerceAtLeast(0) * 3.0 + it.primaryPerGame }?.let { topP ->
            awards.add(
                MinorLeagueAward(
                    id = "min_aw_${year}_prospect",
                    year = year,
                    type = MinorLeagueAwardType.TOP_PROSPECT_CALLUP,
                    playerId = topP.playerId,
                    playerName = topP.playerName,
                    position = topP.position,
                    affiliateId = topP.affiliateId,
                    affiliateName = topP.affiliateName,
                    parentFranchiseName = topP.parentFranchiseName,
                    statLine = "${topP.overallRating} OVR (+${topP.ovrGainsEarned} Dev) • Age ${topP.age} • ${topP.primaryPerGame} ${labels.primaryShort}/G",
                    narrative = "Parent-club depth standout ready for immediate main-roster promotion after rapid development."
                )
            )
        }

        candidates.maxByOrNull { it.totalDefenseStat * 3.0 + it.secondaryPerGame }?.let { dpoy ->
            awards.add(
                MinorLeagueAward(
                    id = "min_aw_${year}_dpoy",
                    year = year,
                    type = MinorLeagueAwardType.DEFENSIVE_PROSPECT,
                    playerId = dpoy.playerId,
                    playerName = dpoy.playerName,
                    position = dpoy.position,
                    affiliateId = dpoy.affiliateId,
                    affiliateName = dpoy.affiliateName,
                    parentFranchiseName = dpoy.parentFranchiseName,
                    statLine = "${dpoy.totalDefenseStat} ${labels.defenseShort} • ${dpoy.secondaryPerGame} ${labels.secondaryShort}/G",
                    narrative = "Anchored ${dpoy.affiliateName}'s defensive identity all season."
                )
            )
        }

        candidates.maxByOrNull { it.totalPrimaryStat * 100.0 + it.primaryPerGame }?.let { scorer ->
            awards.add(
                MinorLeagueAward(
                    id = "min_aw_${year}_scoring",
                    year = year,
                    type = MinorLeagueAwardType.SCORING_LEADER,
                    playerId = scorer.playerId,
                    playerName = scorer.playerName,
                    position = scorer.position,
                    affiliateId = scorer.affiliateId,
                    affiliateName = scorer.affiliateName,
                    parentFranchiseName = scorer.parentFranchiseName,
                    statLine = "${scorer.primaryPerGame} ${labels.primaryShort}/G (${scorer.totalPrimaryStat} Total)",
                    narrative = "Led all ${affiliates.size} affiliates in season scoring output."
                )
            )
        }

        val finalsPool = candidates.filter { championAffiliate == null || it.affiliateId == championAffiliate.id }.ifEmpty { candidates }
        finalsPool.maxByOrNull { it.playoffPrimaryStat * 10.0 + mvpScore(it) }?.let { fmvp ->
            awards.add(
                MinorLeagueAward(
                    id = "min_aw_${year}_fmvp",
                    year = year,
                    type = MinorLeagueAwardType.FINALS_MVP,
                    playerId = fmvp.playerId,
                    playerName = fmvp.playerName,
                    position = fmvp.position,
                    affiliateId = fmvp.affiliateId,
                    affiliateName = championAffiliate?.fullName ?: fmvp.affiliateName,
                    parentFranchiseName = championAffiliate?.parentFranchiseName ?: fmvp.parentFranchiseName,
                    statLine = "${fmvp.playoffPrimaryStat} Playoff ${labels.primaryShort} • ${fmvp.primaryPerGame} ${labels.primaryShort}/G",
                    narrative = "Captured Championship Finals MVP honors leading ${championAffiliate?.fullName ?: fmvp.affiliateName} to the Season $year crown."
                )
            )
        }

        return awards
    }

    private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}
