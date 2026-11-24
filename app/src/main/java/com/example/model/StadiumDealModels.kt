package com.example.model

import com.example.data.InitialLeagueData
import kotlin.math.roundToLong
import kotlin.random.Random

enum class StadiumUltimatumChoice(
    val title: String,
    val badgeTag: String,
    val description: String,
    val leagueCashCostMillions: Long,
    val ownerApprovalDelta: Int,
    val fanSentimentDelta: Int,
    val relocationThreatProbabilityPct: Int
) {
    LOBBY_PUBLIC_TAX_FUNDING(
        title = "Lobby for Public Taxpayer Funding",
        badgeTag = "PUBLIC BOND",
        description = "Back municipal tax bonds to finance a brand-new stadium; thrills ownership but frustrates local taxpayers.",
        leagueCashCostMillions = 0L,
        ownerApprovalDelta = 8,
        fanSentimentDelta = -10,
        relocationThreatProbabilityPct = 0
    ),
    FORCE_OWNER_PRIVATE_FUNDING(
        title = "Force Owner Private Funding",
        badgeTag = "PRIVATE MANDATE",
        description = "Protect taxpayers and compel ownership to self-fund; boosts fans, angers owners (50% Relocation Threat).",
        leagueCashCostMillions = 0L,
        ownerApprovalDelta = -11,
        fanSentimentDelta = 9,
        relocationThreatProbabilityPct = 50
    ),
    SUBSIDIZE_WITH_LEAGUE_FUNDS(
        title = "Spend League Funds to Subsidize",
        badgeTag = "LEAGUE SUBSIDY",
        description = "Deploy \$28M from league treasury to co-fund the new arena locally, keeping owners and fans aligned.",
        leagueCashCostMillions = 28L,
        ownerApprovalDelta = 7,
        fanSentimentDelta = 3,
        relocationThreatProbabilityPct = 0
    )
}

data class StadiumUltimatumEvent(
    val id: String,
    val franchiseId: String,
    val franchiseName: String,
    val cityName: String,
    val ownerName: String,
    val ownershipGroupName: String,
    val franchiseColorHex: Long,
    val stadiumAge: Int,
    val stadiumCondition: Int,
    val stadiumCapacity: Int,
    val seasonYear: Int,
    val subsidyCostMillions: Long = 28L,
    val headline: String,
    val description: String
)

data class RelocationDealOffer(
    val cityId: String,
    val cityName: String,
    val region: String,
    val isInternational: Boolean,
    val populationMillions: Double,
    val arenaName: String,
    val newStadiumCapacity: Int,
    val publicContributionPct: Int,
    val newFacilityValueMillions: Long,
    val relocationFeeMillions: Long,
    val projectedSponsorRevenueMillions: Long,
    val projectedFanBaseMillions: Double,
    val dealPitch: String
)

data class RelocationVoteState(
    val id: String,
    val ultimatumId: String,
    val franchiseId: String,
    val franchiseName: String,
    val currentCityId: String,
    val currentCityName: String,
    val ownerName: String,
    val ownershipGroupName: String,
    val franchiseColorHex: Long,
    val seasonYear: Int,
    val stadiumAge: Int,
    val stadiumCondition: Int,
    val cityDeals: List<RelocationDealOffer>,
    val selectedCityId: String,
    val approveOwnerApprovalDelta: Int = 9,
    val approveFanSentimentDelta: Int = -6,
    val vetoOwnerApprovalDelta: Int = -14,
    val vetoFanSentimentDelta: Int = 8
)

data class StadiumDealState(
    val activeUltimatum: StadiumUltimatumEvent? = null,
    val pendingUltimatumQueue: List<StadiumUltimatumEvent> = emptyList(),
    val activeRelocationVote: RelocationVoteState? = null,
    val lastTickedSeasonYear: Int = 0,
    val lastDecisionSummary: String = "Monitoring municipal arena leases and stadium conditions across all franchises.",
    val historyLog: List<String> = emptyList()
)

data class OffseasonStadiumCheckResult(
    val updatedFranchises: List<Franchise>,
    val updatedStadiumDealState: StadiumDealState,
    val triggeredUltimatums: List<StadiumUltimatumEvent>,
    val headlines: List<String>
)

data class StadiumUltimatumOutcome(
    val success: Boolean,
    val message: String,
    val updatedFranchises: List<Franchise>,
    val updatedStadiumDealState: StadiumDealState,
    val cashDeltaMillions: Long = 0L,
    val ownerApprovalDelta: Int = 0,
    val fanSentimentDelta: Int = 0,
    val valuationDeltaMillions: Long = 0L,
    val triggeredRelocationThreat: Boolean = false,
    val builtNewStadium: Boolean = false,
    val headline: String? = null
)

data class RelocationVoteOutcome(
    val success: Boolean,
    val approved: Boolean,
    val message: String,
    val updatedFranchises: List<Franchise>,
    val updatedCities: List<MarketCity>,
    val updatedStadiumDealState: StadiumDealState,
    val cashDeltaMillions: Long = 0L,
    val valuationDeltaMillions: Long = 0L,
    val ownerApprovalDelta: Int = 0,
    val fanSentimentDelta: Int = 0,
    val relocatedTeamId: String? = null,
    val destinationCityName: String? = null,
    val headline: String
)

object StadiumDealEngine {

    const val ULTIMATUM_AGE_THRESHOLD = 25
    const val ULTIMATUM_CONDITION_THRESHOLD = 50
    const val LEAGUE_SUBSIDY_COST_MILLIONS = 28L

    fun qualifiesForUltimatum(franchise: Franchise): Boolean =
        franchise.stadiumAge > ULTIMATUM_AGE_THRESHOLD &&
            franchise.stadiumCondition < ULTIMATUM_CONDITION_THRESHOLD

    fun processOffseasonStadiumsAndCheckUltimatums(
        currentState: StadiumDealState,
        franchises: List<Franchise>,
        seasonYear: Int
    ): OffseasonStadiumCheckResult {
        if (currentState.lastTickedSeasonYear >= seasonYear) {
            return OffseasonStadiumCheckResult(
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState,
                triggeredUltimatums = emptyList(),
                headlines = emptyList()
            )
        }

        val agedFranchises = franchises.map { team ->
            val annualWear = when {
                team.facilities.stadiumTier >= 4 -> 2
                team.facilities.stadiumTier >= 2 -> 3
                else -> 4
            }
            team.copy(
                stadiumAge = (team.stadiumAge + 1).coerceAtLeast(0),
                stadiumCondition = (team.stadiumCondition - annualWear).coerceIn(5, 100)
            )
        }

        val existingIds = buildSet {
            currentState.activeUltimatum?.let { add(it.franchiseId) }
            currentState.pendingUltimatumQueue.forEach { add(it.franchiseId) }
            currentState.activeRelocationVote?.let { add(it.franchiseId) }
        }

        val qualifying = agedFranchises
            .filter { qualifiesForUltimatum(it) && it.id !in existingIds }
            .sortedWith(compareBy<Franchise> { it.stadiumCondition }.thenByDescending { it.stadiumAge })

        val newEvents = qualifying.map { team ->
            StadiumUltimatumEvent(
                id = "ultimatum_${team.id}_y$seasonYear",
                franchiseId = team.id,
                franchiseName = team.teamFullName,
                cityName = team.cityName,
                ownerName = team.ownership.primaryOwner.fullName,
                ownershipGroupName = team.ownership.name,
                franchiseColorHex = team.colorHex,
                stadiumAge = team.stadiumAge,
                stadiumCondition = team.stadiumCondition,
                stadiumCapacity = team.stadiumCapacity,
                seasonYear = seasonYear,
                subsidyCostMillions = LEAGUE_SUBSIDY_COST_MILLIONS,
                headline = "STADIUM ULTIMATUM: ${team.teamFullName} arena (${team.stadiumAge} yrs, ${team.stadiumCondition}% cond.) requires replacement deal!",
                description = "${team.ownership.primaryOwner.fullName} warns ${team.cityName}'s ${team.stadiumAge}-year-old facility (${team.stadiumCondition}% condition) is functionally obsolete and demands a stadium financing ruling."
            )
        }

        val combinedQueue = buildList {
            currentState.activeUltimatum?.let { add(it) }
            addAll(currentState.pendingUltimatumQueue)
            addAll(newEvents)
        }
        val nextActive = combinedQueue.firstOrNull()
        val nextPending = if (combinedQueue.size > 1) combinedQueue.drop(1) else emptyList()

        val headlines = newEvents.map { it.headline }
        val updatedState = currentState.copy(
            activeUltimatum = nextActive,
            pendingUltimatumQueue = nextPending,
            lastTickedSeasonYear = seasonYear,
            lastDecisionSummary = newEvents.firstOrNull()?.headline ?: currentState.lastDecisionSummary,
            historyLog = (headlines + currentState.historyLog).take(20)
        )

        return OffseasonStadiumCheckResult(
            updatedFranchises = agedFranchises,
            updatedStadiumDealState = updatedState,
            triggeredUltimatums = newEvents,
            headlines = headlines
        )
    }

    fun buildRelocationDealOffers(
        cities: List<MarketCity>,
        franchise: Franchise,
        seasonYear: Int
    ): List<RelocationDealOffer> {
        val candidatePool = cities
            .filter { !it.hasFranchise && it.id != franchise.cityId }
            .ifEmpty { InitialLeagueData.getProspectiveMarketPool().filter { it.id != franchise.cityId } }

        val topThree = candidatePool
            .sortedWith(compareByDescending<MarketCity> { it.overallViabilityScore }.thenByDescending { it.sportsAppetite })
            .take(3)

        return topThree.mapIndexed { idx, city ->
            val publicPct = (82 - (idx * 7)).coerceIn(60, 88)
            val newCapacity = city.arenaCapacity.coerceAtLeast(20500)
            val sponsorProj = (city.populationMillions * city.tier.mediaMultiplier * 4.2).roundToLong().coerceAtLeast(24L)
            val fanProj = ((city.populationMillions * 0.45) * 10.0).roundToLong() / 10.0
            val relocFee = (18L - (idx * 3L)).coerceAtLeast(10L)
            val facilityVal = (320L + (publicPct * 2L))

            RelocationDealOffer(
                cityId = city.id,
                cityName = city.name,
                region = city.region,
                isInternational = city.isInternational,
                populationMillions = city.populationMillions,
                arenaName = city.arenaName,
                newStadiumCapacity = newCapacity,
                publicContributionPct = publicPct,
                newFacilityValueMillions = facilityVal,
                relocationFeeMillions = relocFee,
                projectedSponsorRevenueMillions = sponsorProj,
                projectedFanBaseMillions = fanProj,
                dealPitch = "${publicPct}% municipal bond financing for ${city.arenaName} (${"%,d".format(newCapacity)} seats • Y$seasonYear turnkey lease)."
            )
        }
    }

    fun resolveStadiumUltimatum(
        currentState: StadiumDealState,
        franchises: List<Franchise>,
        cities: List<MarketCity>,
        currentCashMillions: Long,
        choice: StadiumUltimatumChoice,
        forceRelocationRoll: Boolean? = null,
        random: Random = Random.Default
    ): StadiumUltimatumOutcome {
        val ultimatum = currentState.activeUltimatum
            ?: return StadiumUltimatumOutcome(
                success = false,
                message = "No active Stadium Ultimatum requires a ruling.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState
            )

        if (choice == StadiumUltimatumChoice.SUBSIDIZE_WITH_LEAGUE_FUNDS &&
            currentCashMillions < choice.leagueCashCostMillions
        ) {
            return StadiumUltimatumOutcome(
                success = false,
                message = "Need \$${choice.leagueCashCostMillions}M League Cash to subsidize a new stadium for ${ultimatum.franchiseName}.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState
            )
        }

        val targetTeam = franchises.firstOrNull { it.id == ultimatum.franchiseId }
            ?: return StadiumUltimatumOutcome(
                success = false,
                message = "Target franchise no longer exists.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState
            )

        when (choice) {
            StadiumUltimatumChoice.LOBBY_PUBLIC_TAX_FUNDING -> {
                val updatedTeams = franchises.map { team ->
                    if (team.id == targetTeam.id) {
                        team.copy(
                            stadiumAge = 0,
                            stadiumCondition = 100,
                            stadiumCapacity = team.stadiumCapacity + 2500,
                            fanMorale = (team.fanMorale + choice.fanSentimentDelta).coerceIn(20, 99),
                            fanLoyalty = (team.fanLoyalty - 8).coerceIn(15, 100),
                            ownership = team.ownership.copy(
                                ownerApproval = (team.ownership.ownerApproval + choice.ownerApprovalDelta).coerceIn(15, 100)
                            ),
                            facilities = team.facilities.copy(
                                stadiumTier = (team.facilities.stadiumTier + 1).coerceAtMost(5),
                                accumulatedFacilityValueMillions = team.facilities.accumulatedFacilityValueMillions + 45L
                            )
                        )
                    } else team
                }
                val summary = "PUBLIC STADIUM DEAL: ${targetTeam.teamFullName} secured taxpayer bond financing for a new arena (+${choice.ownerApprovalDelta} Owner, ${choice.fanSentimentDelta} Fan Sentiment)."
                val nextUltimatum = currentState.pendingUltimatumQueue.firstOrNull()
                val nextQueue = currentState.pendingUltimatumQueue.drop(1)
                return StadiumUltimatumOutcome(
                    success = true,
                    message = summary,
                    updatedFranchises = updatedTeams,
                    updatedStadiumDealState = currentState.copy(
                        activeUltimatum = nextUltimatum,
                        pendingUltimatumQueue = nextQueue,
                        lastDecisionSummary = summary,
                        historyLog = (listOf(summary) + currentState.historyLog).take(20)
                    ),
                    cashDeltaMillions = 0L,
                    ownerApprovalDelta = choice.ownerApprovalDelta,
                    fanSentimentDelta = choice.fanSentimentDelta,
                    valuationDeltaMillions = 24L,
                    triggeredRelocationThreat = false,
                    builtNewStadium = true,
                    headline = summary
                )
            }

            StadiumUltimatumChoice.FORCE_OWNER_PRIVATE_FUNDING -> {
                val relocationTriggered = forceRelocationRoll ?: random.nextBoolean()
                if (relocationTriggered) {
                    val offers = buildRelocationDealOffers(cities, targetTeam, ultimatum.seasonYear)
                    val voteState = RelocationVoteState(
                        id = "reloc_vote_${targetTeam.id}_y${ultimatum.seasonYear}",
                        ultimatumId = ultimatum.id,
                        franchiseId = targetTeam.id,
                        franchiseName = targetTeam.teamFullName,
                        currentCityId = targetTeam.cityId,
                        currentCityName = targetTeam.cityName,
                        ownerName = targetTeam.ownership.primaryOwner.fullName,
                        ownershipGroupName = targetTeam.ownership.name,
                        franchiseColorHex = targetTeam.colorHex,
                        seasonYear = ultimatum.seasonYear,
                        stadiumAge = targetTeam.stadiumAge,
                        stadiumCondition = targetTeam.stadiumCondition,
                        cityDeals = offers,
                        selectedCityId = offers.firstOrNull()?.cityId ?: ""
                    )
                    val updatedTeams = franchises.map { team ->
                        if (team.id == targetTeam.id) {
                            team.copy(
                                fanMorale = (team.fanMorale + choice.fanSentimentDelta).coerceIn(20, 99),
                                fanLoyalty = (team.fanLoyalty + 6).coerceIn(15, 100),
                                ownership = team.ownership.copy(
                                    ownerApproval = (team.ownership.ownerApproval + choice.ownerApprovalDelta).coerceIn(15, 100)
                                )
                            )
                        } else team
                    }
                    val threatHeadline = "RELOCATION THREAT TRIGGERED! ${targetTeam.ownership.primaryOwner.fullName} rejected private stadium mandate in ${targetTeam.cityName} and solicited ${offers.size} outside stadium deals!"
                    return StadiumUltimatumOutcome(
                        success = true,
                        message = threatHeadline,
                        updatedFranchises = updatedTeams,
                        updatedStadiumDealState = currentState.copy(
                            activeUltimatum = null,
                            activeRelocationVote = voteState,
                            lastDecisionSummary = threatHeadline,
                            historyLog = (listOf(threatHeadline) + currentState.historyLog).take(20)
                        ),
                        cashDeltaMillions = 0L,
                        ownerApprovalDelta = choice.ownerApprovalDelta,
                        fanSentimentDelta = choice.fanSentimentDelta,
                        valuationDeltaMillions = 0L,
                        triggeredRelocationThreat = true,
                        builtNewStadium = false,
                        headline = threatHeadline
                    )
                } else {
                    val updatedTeams = franchises.map { team ->
                        if (team.id == targetTeam.id) {
                            team.copy(
                                stadiumAge = 0,
                                stadiumCondition = 100,
                                stadiumCapacity = team.stadiumCapacity + 2000,
                                fanMorale = (team.fanMorale + choice.fanSentimentDelta).coerceIn(20, 99),
                                fanLoyalty = (team.fanLoyalty + 6).coerceIn(15, 100),
                                ownership = team.ownership.copy(
                                    ownerApproval = (team.ownership.ownerApproval + choice.ownerApprovalDelta).coerceIn(15, 100)
                                ),
                                facilities = team.facilities.copy(
                                    stadiumTier = (team.facilities.stadiumTier + 1).coerceAtMost(5),
                                    accumulatedFacilityValueMillions = team.facilities.accumulatedFacilityValueMillions + 40L
                                )
                            )
                        } else team
                    }
                    val summary = "PRIVATE STADIUM BUILT: ${targetTeam.ownership.primaryOwner.fullName} relented and self-funded a new arena in ${targetTeam.cityName} (+${choice.fanSentimentDelta} Fan Sentiment, ${choice.ownerApprovalDelta} Owner)."
                    val nextUltimatum = currentState.pendingUltimatumQueue.firstOrNull()
                    val nextQueue = currentState.pendingUltimatumQueue.drop(1)
                    return StadiumUltimatumOutcome(
                        success = true,
                        message = summary,
                        updatedFranchises = updatedTeams,
                        updatedStadiumDealState = currentState.copy(
                            activeUltimatum = nextUltimatum,
                            pendingUltimatumQueue = nextQueue,
                            lastDecisionSummary = summary,
                            historyLog = (listOf(summary) + currentState.historyLog).take(20)
                        ),
                        cashDeltaMillions = 0L,
                        ownerApprovalDelta = choice.ownerApprovalDelta,
                        fanSentimentDelta = choice.fanSentimentDelta,
                        valuationDeltaMillions = 20L,
                        triggeredRelocationThreat = false,
                        builtNewStadium = true,
                        headline = summary
                    )
                }
            }

            StadiumUltimatumChoice.SUBSIDIZE_WITH_LEAGUE_FUNDS -> {
                val updatedTeams = franchises.map { team ->
                    if (team.id == targetTeam.id) {
                        team.copy(
                            stadiumAge = 0,
                            stadiumCondition = 100,
                            stadiumCapacity = team.stadiumCapacity + 2200,
                            fanMorale = (team.fanMorale + choice.fanSentimentDelta).coerceIn(20, 99),
                            fanLoyalty = (team.fanLoyalty + 3).coerceIn(15, 100),
                            ownership = team.ownership.copy(
                                ownerApproval = (team.ownership.ownerApproval + choice.ownerApprovalDelta).coerceIn(15, 100)
                            ),
                            facilities = team.facilities.copy(
                                stadiumTier = (team.facilities.stadiumTier + 1).coerceAtMost(5),
                                cumulativeInvestedMillions = team.facilities.cumulativeInvestedMillions + choice.leagueCashCostMillions,
                                accumulatedFacilityValueMillions = team.facilities.accumulatedFacilityValueMillions + 42L
                            )
                        )
                    } else team
                }
                val summary = "LEAGUE STADIUM SUBSIDY: Invested \$${choice.leagueCashCostMillions}M in league funds to build ${targetTeam.teamFullName}'s new arena (+${choice.ownerApprovalDelta} Owner, +${choice.fanSentimentDelta} Fans)."
                val nextUltimatum = currentState.pendingUltimatumQueue.firstOrNull()
                val nextQueue = currentState.pendingUltimatumQueue.drop(1)
                return StadiumUltimatumOutcome(
                    success = true,
                    message = summary,
                    updatedFranchises = updatedTeams,
                    updatedStadiumDealState = currentState.copy(
                        activeUltimatum = nextUltimatum,
                        pendingUltimatumQueue = nextQueue,
                        lastDecisionSummary = summary,
                        historyLog = (listOf(summary) + currentState.historyLog).take(20)
                    ),
                    cashDeltaMillions = -choice.leagueCashCostMillions,
                    ownerApprovalDelta = choice.ownerApprovalDelta,
                    fanSentimentDelta = choice.fanSentimentDelta,
                    valuationDeltaMillions = 22L,
                    triggeredRelocationThreat = false,
                    builtNewStadium = true,
                    headline = summary
                )
            }
        }
    }

    fun resolveRelocationVote(
        currentState: StadiumDealState,
        franchises: List<Franchise>,
        cities: List<MarketCity>,
        ruleBook: LeagueRuleBook,
        approve: Boolean,
        selectedCityIdOverride: String? = null
    ): RelocationVoteOutcome {
        val vote = currentState.activeRelocationVote
            ?: return RelocationVoteOutcome(
                success = false,
                approved = approve,
                message = "No active Relocation Vote is pending.",
                updatedFranchises = franchises,
                updatedCities = cities,
                updatedStadiumDealState = currentState,
                headline = "No pending Relocation Vote."
            )

        val targetTeam = franchises.firstOrNull { it.id == vote.franchiseId }
            ?: return RelocationVoteOutcome(
                success = false,
                approved = approve,
                message = "Target franchise no longer found.",
                updatedFranchises = franchises,
                updatedCities = cities,
                updatedStadiumDealState = currentState,
                headline = "Relocation vote cancelled."
            )

        val nextUltimatum = currentState.pendingUltimatumQueue.firstOrNull()
        val nextQueue = currentState.pendingUltimatumQueue.drop(1)

        if (approve) {
            val targetCityId = selectedCityIdOverride?.takeIf { id -> vote.cityDeals.any { it.cityId == id } }
                ?: vote.selectedCityId
            val chosenDeal = vote.cityDeals.firstOrNull { it.cityId == targetCityId } ?: vote.cityDeals.firstOrNull()
                ?: return RelocationVoteOutcome(
                    success = false,
                    approved = false,
                    message = "No relocation stadium deal available.",
                    updatedFranchises = franchises,
                    updatedCities = cities,
                    updatedStadiumDealState = currentState,
                    headline = "No city stadium deal selected."
                )

            val (relocConf, relocDiv) = InitialLeagueData.getGeographicAlignment(
                cityName = chosenDeal.cityName,
                enabledConferences = ruleBook.enabledConferences,
                enabledDivisions = ruleBook.enabledDivisions
            )
            val shouldRealign = relocConf.isContinentalInternational || ruleBook.isRuleEnacted("rule_geographic_realignment")

            val updatedFranchises = franchises.map { team ->
                if (team.id == targetTeam.id) {
                    team.copy(
                        cityId = chosenDeal.cityId,
                        cityName = chosenDeal.cityName,
                        conference = if (shouldRealign) relocConf else team.conference,
                        division = if (shouldRealign) relocDiv else team.division,
                        stadiumAge = 0,
                        stadiumCondition = 100,
                        stadiumCapacity = chosenDeal.newStadiumCapacity,
                        fanBaseMillions = chosenDeal.projectedFanBaseMillions,
                        fanMorale = 86,
                        fanLoyalty = 84,
                        localSponsorRevenueMillions = chosenDeal.projectedSponsorRevenueMillions,
                        ownership = team.ownership.copy(
                            ownerApproval = (team.ownership.ownerApproval + vote.approveOwnerApprovalDelta).coerceIn(15, 100)
                        ),
                        facilities = team.facilities.copy(
                            stadiumTier = (team.facilities.stadiumTier + 1).coerceAtMost(5),
                            accumulatedFacilityValueMillions = team.facilities.accumulatedFacilityValueMillions + 55L
                        )
                    )
                } else {
                    team.copy(
                        fanMorale = (team.fanMorale - 4).coerceIn(20, 99),
                        fanLoyalty = (team.fanLoyalty - 3).coerceIn(15, 100)
                    )
                }
            }

            val updatedCities = cities.map { city ->
                when (city.id) {
                    chosenDeal.cityId -> city.copy(hasFranchise = true)
                    vote.currentCityId -> city.copy(hasFranchise = false)
                    else -> city
                }
            }

            val headline = "RELOCATION APPROVED: ${targetTeam.nickname} depart ${vote.currentCityName} for ${chosenDeal.cityName}'s ${chosenDeal.arenaName} (${chosenDeal.publicContributionPct}% municipal stadium deal, +\$${chosenDeal.relocationFeeMillions}M fee)."
            return RelocationVoteOutcome(
                success = true,
                approved = true,
                message = headline,
                updatedFranchises = updatedFranchises,
                updatedCities = updatedCities,
                updatedStadiumDealState = currentState.copy(
                    activeUltimatum = nextUltimatum,
                    pendingUltimatumQueue = nextQueue,
                    activeRelocationVote = null,
                    lastDecisionSummary = headline,
                    historyLog = (listOf(headline) + currentState.historyLog).take(20)
                ),
                cashDeltaMillions = chosenDeal.relocationFeeMillions,
                valuationDeltaMillions = 30L,
                ownerApprovalDelta = vote.approveOwnerApprovalDelta,
                fanSentimentDelta = vote.approveFanSentimentDelta,
                relocatedTeamId = targetTeam.id,
                destinationCityName = chosenDeal.cityName,
                headline = headline
            )
        } else {
            val updatedFranchises = franchises.map { team ->
                if (team.id == targetTeam.id) {
                    team.copy(
                        stadiumAge = 0,
                        stadiumCondition = 82,
                        stadiumCapacity = team.stadiumCapacity + 1000,
                        fanMorale = (team.fanMorale + 10).coerceIn(20, 99),
                        fanLoyalty = (team.fanLoyalty + 8).coerceIn(15, 100),
                        ownership = team.ownership.copy(
                            ownerApproval = (team.ownership.ownerApproval + vote.vetoOwnerApprovalDelta).coerceIn(15, 100)
                        )
                    )
                } else team
            }
            val headline = "RELOCATION VETOED: Commissioner blocked ${targetTeam.teamFullName} from leaving ${vote.currentCityName}; forced local arena overhaul (82% cond., ${vote.vetoOwnerApprovalDelta} Owner, +${vote.vetoFanSentimentDelta} Fans)."
            return RelocationVoteOutcome(
                success = true,
                approved = false,
                message = headline,
                updatedFranchises = updatedFranchises,
                updatedCities = cities,
                updatedStadiumDealState = currentState.copy(
                    activeUltimatum = nextUltimatum,
                    pendingUltimatumQueue = nextQueue,
                    activeRelocationVote = null,
                    lastDecisionSummary = headline,
                    historyLog = (listOf(headline) + currentState.historyLog).take(20)
                ),
                cashDeltaMillions = 0L,
                valuationDeltaMillions = 8L,
                ownerApprovalDelta = vote.vetoOwnerApprovalDelta,
                fanSentimentDelta = vote.vetoFanSentimentDelta,
                relocatedTeamId = null,
                destinationCityName = null,
                headline = headline
            )
        }
    }
}
