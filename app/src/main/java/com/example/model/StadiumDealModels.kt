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

enum class NamingRightsTriggerReason(val label: String, val badgeText: String) {
    NEW_STADIUM_BUILT("New Stadium Inauguration", "NEW STADIUM"),
    CONTRACT_EXPIRED("Naming Rights Contract Expired", "RENEWAL DUE"),
    BANKRUPTCY_SCANDAL("Sponsor Bankruptcy Scandal", "SCANDAL REPLACEMENT")
}

data class NamingRightsSponsorOffer(
    val id: String,
    val sponsorName: String,
    val tier: Int, // 1 = Safe/Low Pay, 2 = Medium/Winning Record, 3 = Huge/High Scandal Risk
    val termYears: Int,
    val annualRevenueMillions: Long,
    val scandalRiskPct: Int,
    val requiresWinningRecord: Boolean,
    val isEligible: Boolean,
    val ineligibilityReason: String? = null,
    val tagline: String,
    val tierTitle: String,
    val riskSummary: String
)

data class NamingRightsOfferSession(
    val id: String,
    val franchiseId: String,
    val franchiseName: String,
    val cityName: String,
    val franchiseColorHex: Long,
    val seasonYear: Int,
    val triggerReason: NamingRightsTriggerReason,
    val previousSponsorName: String? = null,
    val teamWins: Int = 0,
    val teamLosses: Int = 0,
    val offers: List<NamingRightsSponsorOffer>,
    val scandalPrDrain: Int = 0
)

data class SponsorBankruptcyScandalEvent(
    val id: String,
    val franchiseId: String,
    val franchiseName: String,
    val cityName: String,
    val bankruptSponsorName: String,
    val lostAnnualRevenueMillions: Long,
    val seasonYear: Int,
    val leaguePrDrain: Int,
    val fanMoraleDrain: Int,
    val headline: String,
    val description: String
)

data class NamingRightsSelectionOutcome(
    val success: Boolean,
    val message: String,
    val updatedFranchises: List<Franchise>,
    val updatedStadiumDealState: StadiumDealState,
    val chosenOffer: NamingRightsSponsorOffer? = null,
    val targetFranchiseId: String? = null,
    val valuationDeltaMillions: Long = 0L,
    val headline: String
)

data class StadiumDealState(
    val activeUltimatum: StadiumUltimatumEvent? = null,
    val pendingUltimatumQueue: List<StadiumUltimatumEvent> = emptyList(),
    val activeRelocationVote: RelocationVoteState? = null,
    val activeNamingRightsOffer: NamingRightsOfferSession? = null,
    val pendingNamingRightsQueue: List<NamingRightsOfferSession> = emptyList(),
    val activeBankruptcyScandal: SponsorBankruptcyScandalEvent? = null,
    val pendingBankruptcyScandalQueue: List<SponsorBankruptcyScandalEvent> = emptyList(),
    val lastTickedSeasonYear: Int = 0,
    val lastDecisionSummary: String = "Monitoring municipal arena leases and stadium conditions across all franchises.",
    val historyLog: List<String> = emptyList()
)

data class OffseasonStadiumCheckResult(
    val updatedFranchises: List<Franchise>,
    val updatedStadiumDealState: StadiumDealState,
    val triggeredUltimatums: List<StadiumUltimatumEvent>,
    val headlines: List<String>,
    val triggeredBankruptcyScandals: List<SponsorBankruptcyScandalEvent> = emptyList(),
    val triggeredNamingRightsOffers: List<NamingRightsOfferSession> = emptyList(),
    val totalLeaguePrDrain: Int = 0
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
    const val TIER3_SCANDAL_CHANCE_PCT = 20
    const val TIER3_LEAGUE_PR_DRAIN = 9

    private val tier1SponsorPool = listOf(
        "Heartland Mutual" to "Trusted civic insurer & community partner",
        "CivicFirst Credit Union" to "Member-owned regional banking stability",
        "Liberty Oak Hardware" to "Blue-collar hometown building supplier",
        "Metro Transit Authority" to "Municipal rail & mobility backbone",
        "Summit Valley Roasters" to "Regional heritage coffee & hospitality",
        "HarborLight Health" to "Local hospital network & community clinic",
        "PrairieStone Insurance" to "Midwest family property & casualty carrier",
        "Cascadia Timber Co." to "Sustainable forestry & civic construction",
        "Redwood Regional Bank" to "Main Street commercial & savings institution",
        "Bayside Energy Coop" to "Publicly accountable municipal electric utility",
        "IronBridge Steelworks" to "American structural manufacturing leader",
        "Lakeside Fresh Grocers" to "Community neighborhood supermarket chain",
        "ClearView Optical" to "Regional family vision & wellness centers",
        "Beacon Civic Broadband" to "Municipal fiber & public connectivity group",
        "Greenfield Foods" to "Farm-to-table packaged staples & dairy",
        "Heritage Rail Lines" to "Regional passenger & freight corridor",
        "Riverbend Bottling Co." to "Hometown sparkling water & craft soda",
        "NorthStar Hardware Group" to "Contractor supply & home improvement",
        "Patriot Mutual Trust" to "Civic pension & municipal bond custodian",
        "Keystone Dairy Co-op" to "Farmer-owned regional nutrition partner",
        "CedarCrest Hospitals" to "Pediatric & sports medicine regional network",
        "Founders State Bank" to "Century-old small-business lending partner",
        "Sunbelt Produce Co." to "Wholesale agricultural & cold-chain grocer",
        "Union Station Properties" to "Downtown civic transit & retail developer"
    )

    private val tier2SponsorPool = listOf(
        "ApexCore Semiconductors" to "Enterprise silicon & AI hardware leader",
        "Vanguard Cloud Systems" to "Global cloud infrastructure & data centers",
        "Pinnacle Aviation" to "International commercial airline carrier",
        "Meridian Biotech" to "Fortune-500 precision medical sciences",
        "Veritas Wealth Partners" to "Institutional asset management group",
        "Titan Logistics Group" to "Continental freight & supply-chain giant",
        "Stratosphere Telecom" to "5G wireless carrier & fiber backbone",
        "Helios Clean Grid" to "Utility-scale solar & battery infrastructure",
        "Kestrel Aerospace" to "Commercial jetliner & avionics manufacturer",
        "BlueRidge Financial" to "National investment banking & card network",
        "Aether Motors" to "Electric SUV & performance sedan automaker",
        "Monarch Pharmaceuticals" to "Global therapeutics & research giant",
        "Orion Cloud Security" to "Enterprise zero-trust cybersecurity leader",
        "Continental Express" to "Global air cargo & parcel delivery network",
        "Sterling Heavy Industries" to "Clean-tech industrial automation conglomerate",
        "CobaltStream Media" to "Interactive home entertainment & hardware",
        "Vertex Medical Robotics" to "Surgical robotics & diagnostics leader",
        "Atlas Global Insurance" to "Multinational commercial risk underwriter",
        "Pacific Rim Electronics" to "Consumer displays, audio & smart devices",
        "CrownPoint Hotels" to "Luxury international hospitality & resorts",
        "Sovereign Payments" to "Worldwide merchant settlement & fintech rail",
        "Horizon Defense Tech" to "Satellite navigation & aerospace telemetry",
        "SilverLine Automotive" to "Autonomous mobility & luxury fleet maker",
        "Crestview Consumer Brands" to "Global athletic apparel & beverage giant"
    )

    private val tier3SponsorPool = listOf(
        "NovaVault Crypto" to "Hyper-growth offshore digital token exchange",
        "HyperYield DeFi" to "Speculative algorithmic yield conglomerate",
        "OmniChain Markets" to "Unregulated leveraged derivatives platform",
        "Vortex Energy Trading" to "Aggressive commodities & carbon hedge fund",
        "QuantumBet Syndicate" to "High-stakes offshore sportsbook operator",
        "TitanCoin Holdings" to "Volatile crypto treasury & NFT conglomerate",
        "BlitzWager Global" to "High-leverage live micro-betting syndicate",
        "ApexMargin Exchange" to "100x perpetual futures trading venue",
        "Mirage Digital Assets" to "Cayman-domiciled stablecoin issuer",
        "TurboLoan Direct" to "Subprime instant fintech credit underwriter",
        "CipherCash Protocol" to "Privacy-coin liquidity & staking pool",
        "PhantomOdds International" to "Unlicensed global prop-betting bookmaker",
        "Krypton Capital Fund" to "Speculative SPAC & short-volatility fund",
        "Velox FlashCredit" to "High-APR consumer BNPL lending startup",
        "Nebula Token Labs" to "Web3 metaverse land & gaming token studio",
        "OmegaLeverage Markets" to "Synthetic equity & crypto options broker",
        "Solstice Offshore Bank" to "Aggressive tax-haven private wealth desk",
        "PulseBets Entertainment" to "VIP high-roller mobile casino network",
        "Zenith Mineral Futures" to "Leveraged lithium & rare-earth speculator",
        "Obsidian Hedge Partners" to "High-frequency distressed debt syndicate",
        "AstraMint Exchange" to "Meme-token launchpad & retail margin app",
        "Valkyrie Crypto Lending" to "Uncollateralized institutional coin lender",
        "Dynamo Wager Exchange" to "Peer-to-peer international sports exchange",
        "CobaltCoin Ventures" to "Leveraged blockchain infrastructure fund"
    )

    fun qualifiesForUltimatum(franchise: Franchise): Boolean =
        franchise.stadiumAge > ULTIMATUM_AGE_THRESHOLD &&
            franchise.stadiumCondition < ULTIMATUM_CONDITION_THRESHOLD

    fun collectUsedStadiumSponsors(
        franchises: List<Franchise>,
        extraExclusions: Collection<String> = emptyList()
    ): Set<String> = buildSet {
        franchises.forEach { team ->
            team.stadium.sponsorName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { add(it) }
        }
        extraExclusions.forEach { name ->
            name.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
        }
    }

    private fun normalizeSponsorKey(name: String): String = name.trim().lowercase()

    fun generateNamingRightsOffersForFranchise(
        franchise: Franchise,
        seasonYear: Int,
        winsOverride: Int? = null,
        lossesOverride: Int? = null,
        excludeSponsorName: String? = null,
        excludedSponsorNames: Set<String> = emptySet(),
        random: Random = Random.Default
    ): List<NamingRightsSponsorOffer> {
        val wins = winsOverride ?: franchise.wins
        val losses = lossesOverride ?: franchise.losses
        val hasWinningRecord = wins > losses

        val excludedKeys = buildSet {
            excludeSponsorName?.trim()?.takeIf { it.isNotEmpty() }?.let { add(normalizeSponsorKey(it)) }
            excludedSponsorNames.forEach { name ->
                name.trim().takeIf { it.isNotEmpty() }?.let { add(normalizeSponsorKey(it)) }
            }
        }.toMutableSet()

        fun pickTwoFromPool(
            pool: List<Pair<String, String>>,
            tierLabel: String
        ): List<Pair<String, String>> {
            val available = pool
                .filter { normalizeSponsorKey(it.first) !in excludedKeys }
                .shuffled(random)
            val chosen = mutableListOf<Pair<String, String>>()
            available.take(2).forEach { pair ->
                chosen.add(pair)
                excludedKeys.add(normalizeSponsorKey(pair.first))
            }
            var suffixCounter = 1
            while (chosen.size < 2) {
                val base = pool[(suffixCounter + kotlin.math.abs(franchise.id.hashCode())) % pool.size]
                val uniqueName = "${base.first} $tierLabel #$suffixCounter"
                if (normalizeSponsorKey(uniqueName) !in excludedKeys) {
                    chosen.add(uniqueName to base.second)
                    excludedKeys.add(normalizeSponsorKey(uniqueName))
                }
                suffixCounter++
            }
            return chosen
        }

        val tier1Picks = pickTwoFromPool(tier1SponsorPool, "Civic")
        val tier2Picks = pickTwoFromPool(tier2SponsorPool, "Corp")
        val tier3Picks = pickTwoFromPool(tier3SponsorPool, "Global")

        val offers = mutableListOf<NamingRightsSponsorOffer>()

        tier1Picks.forEachIndexed { idx, pair ->
            val pay = random.nextLong(4L, 10L) // Random within Tier 1: $4M - $9M
            val years = random.nextInt(3, 6) // 3 - 5 yrs
            offers.add(
                NamingRightsSponsorOffer(
                    id = "nr_${franchise.id}_y${seasonYear}_t1_${idx + 1}_${random.nextInt(1000, 9999)}",
                    sponsorName = pair.first,
                    tier = 1,
                    termYears = years,
                    annualRevenueMillions = pay,
                    scandalRiskPct = 0,
                    requiresWinningRecord = false,
                    isEligible = true,
                    ineligibilityReason = null,
                    tagline = pair.second,
                    tierTitle = "Tier 1 • Safe / Low Pay",
                    riskSummary = "0% Scandal Risk • Guaranteed civic payout"
                )
            )
        }

        tier2Picks.forEachIndexed { idx, pair ->
            val pay = random.nextLong(10L, 20L) // Random within Tier 2: $10M - $19M
            val years = random.nextInt(2, 5) // 2 - 4 yrs
            offers.add(
                NamingRightsSponsorOffer(
                    id = "nr_${franchise.id}_y${seasonYear}_t2_${idx + 1}_${random.nextInt(1000, 9999)}",
                    sponsorName = pair.first,
                    tier = 2,
                    termYears = years,
                    annualRevenueMillions = pay,
                    scandalRiskPct = 0,
                    requiresWinningRecord = true,
                    isEligible = hasWinningRecord,
                    ineligibilityReason = if (hasWinningRecord) null else "Requires Winning Record (Current: $wins-$losses)",
                    tagline = pair.second,
                    tierTitle = "Tier 2 • Medium Pay / Contender Req.",
                    riskSummary = if (hasWinningRecord) {
                        "0% Scandal Risk • Unlocked via $wins-$losses winning record"
                    } else {
                        "Locked • Requires a winning record ($wins-$losses)"
                    }
                )
            )
        }

        tier3Picks.forEachIndexed { idx, pair ->
            val pay = random.nextLong(20L, 35L) // Random within Tier 3: $20M - $34M
            val years = random.nextInt(2, 5) // 2 - 4 yrs
            offers.add(
                NamingRightsSponsorOffer(
                    id = "nr_${franchise.id}_y${seasonYear}_t3_${idx + 1}_${random.nextInt(1000, 9999)}",
                    sponsorName = pair.first,
                    tier = 3,
                    termYears = years,
                    annualRevenueMillions = pay,
                    scandalRiskPct = TIER3_SCANDAL_CHANCE_PCT,
                    requiresWinningRecord = false,
                    isEligible = true,
                    ineligibilityReason = null,
                    tagline = pair.second,
                    tierTitle = "Tier 3 • Huge Pay / High Scandal Risk",
                    riskSummary = "${TIER3_SCANDAL_CHANCE_PCT}% Offseason Bankruptcy Scandal Risk"
                )
            )
        }

        return offers
    }

    fun buildNamingRightsOfferSession(
        franchise: Franchise,
        seasonYear: Int,
        triggerReason: NamingRightsTriggerReason,
        previousSponsorName: String? = null,
        scandalPrDrain: Int = 0,
        winsOverride: Int? = null,
        lossesOverride: Int? = null,
        excludedSponsorNames: Set<String> = emptySet(),
        random: Random = Random.Default
    ): NamingRightsOfferSession {
        val wins = winsOverride ?: franchise.wins
        val losses = lossesOverride ?: franchise.losses
        val offers = generateNamingRightsOffersForFranchise(
            franchise = franchise,
            seasonYear = seasonYear,
            winsOverride = wins,
            lossesOverride = losses,
            excludeSponsorName = previousSponsorName,
            excludedSponsorNames = excludedSponsorNames,
            random = random
        )
        return NamingRightsOfferSession(
            id = "nr_session_${franchise.id}_y${seasonYear}_${triggerReason.name.lowercase()}",
            franchiseId = franchise.id,
            franchiseName = franchise.teamFullName,
            cityName = franchise.cityName,
            franchiseColorHex = franchise.colorHex,
            seasonYear = seasonYear,
            triggerReason = triggerReason,
            previousSponsorName = previousSponsorName,
            teamWins = wins,
            teamLosses = losses,
            offers = offers,
            scandalPrDrain = scandalPrDrain
        )
    }

    fun enqueueNamingRightsOfferForFranchise(
        currentState: StadiumDealState,
        franchise: Franchise,
        seasonYear: Int,
        triggerReason: NamingRightsTriggerReason,
        previousSponsorName: String? = null,
        scandalPrDrain: Int = 0,
        winsOverride: Int? = null,
        lossesOverride: Int? = null,
        allFranchises: List<Franchise> = emptyList(),
        excludedSponsorNames: Set<String> = emptySet(),
        random: Random = Random.Default
    ): StadiumDealState {
        val combinedExclusions = collectUsedStadiumSponsors(allFranchises, excludedSponsorNames)
        val session = buildNamingRightsOfferSession(
            franchise = franchise,
            seasonYear = seasonYear,
            triggerReason = triggerReason,
            previousSponsorName = previousSponsorName,
            scandalPrDrain = scandalPrDrain,
            winsOverride = winsOverride,
            lossesOverride = lossesOverride,
            excludedSponsorNames = combinedExclusions,
            random = random
        )
        val filteredActive = currentState.activeNamingRightsOffer?.takeIf { it.franchiseId != franchise.id }
        val filteredPending = currentState.pendingNamingRightsQueue.filterNot { it.franchiseId == franchise.id }
        return if (filteredActive == null) {
            currentState.copy(
                activeNamingRightsOffer = session,
                pendingNamingRightsQueue = filteredPending
            )
        } else {
            currentState.copy(
                activeNamingRightsOffer = filteredActive,
                pendingNamingRightsQueue = filteredPending + session
            )
        }
    }

    fun processOffseasonStadiumsAndCheckUltimatums(
        currentState: StadiumDealState,
        franchises: List<Franchise>,
        seasonYear: Int,
        seasonRecordsByFranchiseId: Map<String, Pair<Int, Int>> = emptyMap(),
        forceTier3ScandalRoll: Boolean? = null,
        random: Random = Random.Default
    ): OffseasonStadiumCheckResult {
        if (currentState.lastTickedSeasonYear >= seasonYear) {
            return OffseasonStadiumCheckResult(
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState,
                triggeredUltimatums = emptyList(),
                headlines = emptyList()
            )
        }

        val triggeredScandals = mutableListOf<SponsorBankruptcyScandalEvent>()
        val pendingOfferSpecs = mutableListOf<Triple<Franchise, NamingRightsTriggerReason, Triple<String?, Int, Pair<Int, Int>>>>()
        val sponsorHeadlines = mutableListOf<String>()
        var totalPrDrain = 0

        val agedAndSponsoredFranchises = franchises.map { team ->
            val annualWear = when {
                team.facilities.stadiumTier >= 4 -> 2
                team.facilities.stadiumTier >= 2 -> 3
                else -> 4
            }
            var updatedTeam = team.copy(
                stadiumAge = (team.stadiumAge + 1).coerceAtLeast(0),
                stadiumCondition = (team.stadiumCondition - annualWear).coerceIn(5, 100)
            )

            val recordPair = seasonRecordsByFranchiseId[team.id] ?: (team.wins to team.losses)
            val currentStadium = updatedTeam.stadium
            val hadSponsor = currentStadium.hasActiveSponsor

            if (hadSponsor) {
                val isTier3 = currentStadium.sponsorTier == 3
                val scandalTriggered = if (isTier3) {
                    forceTier3ScandalRoll ?: (random.nextInt(100) < TIER3_SCANDAL_CHANCE_PCT)
                } else false

                if (scandalTriggered) {
                    val bankruptName = currentStadium.sponsorName ?: "Tier 3 Sponsor"
                    val lostRev = currentStadium.namingRightsRevenue
                    val prDrain = TIER3_LEAGUE_PR_DRAIN
                    val moraleDrain = 4
                    totalPrDrain += prDrain

                    val scandalHeadline = "SPONSOR BANKRUPTCY SCANDAL: $bankruptName collapsed and defaulted on ${team.teamFullName}'s \$${lostRev}M/yr stadium deal! (-$prDrain League PR)"
                    val scandalEvent = SponsorBankruptcyScandalEvent(
                        id = "scandal_${team.id}_y$seasonYear",
                        franchiseId = team.id,
                        franchiseName = team.teamFullName,
                        cityName = team.cityName,
                        bankruptSponsorName = bankruptName,
                        lostAnnualRevenueMillions = lostRev,
                        seasonYear = seasonYear,
                        leaguePrDrain = prDrain,
                        fanMoraleDrain = moraleDrain,
                        headline = scandalHeadline,
                        description = "$bankruptName filed for emergency bankruptcy amid regulatory fraud probes, voiding ${team.teamFullName}'s \$${lostRev}M/yr naming rights contract. League PR dropped -$prDrain pts and a replacement sponsor must be signed."
                    )
                    triggeredScandals.add(scandalEvent)
                    sponsorHeadlines.add(scandalHeadline)

                    updatedTeam = updatedTeam.copy(
                        stadium = TeamStadium(),
                        fanMorale = (updatedTeam.fanMorale - moraleDrain).coerceIn(20, 99),
                        ownership = updatedTeam.ownership.copy(
                            ownerApproval = (updatedTeam.ownership.ownerApproval - 3).coerceIn(15, 100)
                        )
                    )
                    pendingOfferSpecs.add(
                        Triple(
                            updatedTeam,
                            NamingRightsTriggerReason.BANKRUPTCY_SCANDAL,
                            Triple(bankruptName, prDrain, recordPair)
                        )
                    )
                } else {
                    val nextYearsLeft = (currentStadium.sponsorYearsLeft - 1).coerceAtLeast(0)
                    if (nextYearsLeft == 0) {
                        val expiredName = currentStadium.sponsorName
                        updatedTeam = updatedTeam.copy(stadium = TeamStadium())
                        val expHeadline = "NAMING RIGHTS EXPIRED: ${expiredName ?: "Arena"} naming rights deal for ${team.teamFullName} has ended — 6 new sponsor offers await selection."
                        sponsorHeadlines.add(expHeadline)
                        pendingOfferSpecs.add(
                            Triple(
                                updatedTeam,
                                NamingRightsTriggerReason.CONTRACT_EXPIRED,
                                Triple(expiredName, 0, recordPair)
                            )
                        )
                    } else {
                        updatedTeam = updatedTeam.copy(
                            stadium = currentStadium.copy(sponsorYearsLeft = nextYearsLeft)
                        )
                    }
                }
            } else if (!currentStadium.sponsorName.isNullOrBlank() && currentStadium.sponsorYearsLeft <= 0) {
                val expiredName = currentStadium.sponsorName
                updatedTeam = updatedTeam.copy(stadium = TeamStadium())
                val expHeadline = "NAMING RIGHTS EXPIRED: $expiredName naming rights deal for ${team.teamFullName} hit 0 years remaining — select a new sponsor."
                sponsorHeadlines.add(expHeadline)
                pendingOfferSpecs.add(
                    Triple(
                        updatedTeam,
                        NamingRightsTriggerReason.CONTRACT_EXPIRED,
                        Triple(expiredName, 0, recordPair)
                    )
                )
            }

            updatedTeam
        }

        val activeTakenSponsors = collectUsedStadiumSponsors(agedAndSponsoredFranchises).toMutableSet()
        val triggeredNamingOffers = pendingOfferSpecs.map { (team, reason, meta) ->
            val session = buildNamingRightsOfferSession(
                franchise = team,
                seasonYear = seasonYear,
                triggerReason = reason,
                previousSponsorName = meta.first,
                scandalPrDrain = meta.second,
                winsOverride = meta.third.first,
                lossesOverride = meta.third.second,
                excludedSponsorNames = activeTakenSponsors,
                random = random
            )
            session
        }

        val existingIds = buildSet {
            currentState.activeUltimatum?.let { add(it.franchiseId) }
            currentState.pendingUltimatumQueue.forEach { add(it.franchiseId) }
            currentState.activeRelocationVote?.let { add(it.franchiseId) }
        }

        val qualifying = agedAndSponsoredFranchises
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

        val combinedUltQueue = buildList {
            currentState.activeUltimatum?.let { add(it) }
            addAll(currentState.pendingUltimatumQueue)
            addAll(newEvents)
        }
        val nextActiveUlt = combinedUltQueue.firstOrNull()
        val nextPendingUlt = if (combinedUltQueue.size > 1) combinedUltQueue.drop(1) else emptyList()

        val existingOfferTeamIds = buildSet {
            currentState.activeNamingRightsOffer?.let { add(it.franchiseId) }
            currentState.pendingNamingRightsQueue.forEach { add(it.franchiseId) }
        }
        val newUniqueNamingOffers = triggeredNamingOffers.filter { it.franchiseId !in existingOfferTeamIds }
        val combinedNamingQueue = buildList {
            currentState.activeNamingRightsOffer?.let { add(it) }
            addAll(currentState.pendingNamingRightsQueue)
            addAll(newUniqueNamingOffers)
        }
        val nextActiveNaming = combinedNamingQueue.firstOrNull()
        val nextPendingNaming = if (combinedNamingQueue.size > 1) combinedNamingQueue.drop(1) else emptyList()

        val existingScandalIds = buildSet {
            currentState.activeBankruptcyScandal?.let { add(it.franchiseId) }
            currentState.pendingBankruptcyScandalQueue.forEach { add(it.franchiseId) }
        }
        val newUniqueScandals = triggeredScandals.filter { it.franchiseId !in existingScandalIds }
        val combinedScandalQueue = buildList {
            currentState.activeBankruptcyScandal?.let { add(it) }
            addAll(currentState.pendingBankruptcyScandalQueue)
            addAll(newUniqueScandals)
        }
        val nextActiveScandal = combinedScandalQueue.firstOrNull()
        val nextPendingScandals = if (combinedScandalQueue.size > 1) combinedScandalQueue.drop(1) else emptyList()

        val allHeadlines = newEvents.map { it.headline } + sponsorHeadlines
        val updatedState = currentState.copy(
            activeUltimatum = nextActiveUlt,
            pendingUltimatumQueue = nextPendingUlt,
            activeNamingRightsOffer = nextActiveNaming,
            pendingNamingRightsQueue = nextPendingNaming,
            activeBankruptcyScandal = nextActiveScandal,
            pendingBankruptcyScandalQueue = nextPendingScandals,
            lastTickedSeasonYear = seasonYear,
            lastDecisionSummary = allHeadlines.firstOrNull() ?: currentState.lastDecisionSummary,
            historyLog = (allHeadlines + currentState.historyLog).take(20)
        )

        return OffseasonStadiumCheckResult(
            updatedFranchises = agedAndSponsoredFranchises,
            updatedStadiumDealState = updatedState,
            triggeredUltimatums = newEvents,
            headlines = allHeadlines,
            triggeredBankruptcyScandals = triggeredScandals,
            triggeredNamingRightsOffers = newUniqueNamingOffers,
            totalLeaguePrDrain = totalPrDrain
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
                var rebuiltTeam = targetTeam
                val updatedTeams = franchises.map { team ->
                    if (team.id == targetTeam.id) {
                        rebuiltTeam = team.copy(
                            stadiumAge = 0,
                            stadiumCondition = 100,
                            stadiumCapacity = team.stadiumCapacity + 2500,
                            stadium = TeamStadium(),
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
                        rebuiltTeam
                    } else team
                }
                val summary = "PUBLIC STADIUM DEAL: ${targetTeam.teamFullName} secured taxpayer bond financing for a new arena (+${choice.ownerApprovalDelta} Owner, ${choice.fanSentimentDelta} Fan Sentiment)."
                val nextUltimatum = currentState.pendingUltimatumQueue.firstOrNull()
                val nextQueue = currentState.pendingUltimatumQueue.drop(1)
                val stateWithOffer = enqueueNamingRightsOfferForFranchise(
                    currentState = currentState.copy(
                        activeUltimatum = nextUltimatum,
                        pendingUltimatumQueue = nextQueue,
                        lastDecisionSummary = summary,
                        historyLog = (listOf(summary) + currentState.historyLog).take(20)
                    ),
                    franchise = rebuiltTeam,
                    seasonYear = ultimatum.seasonYear,
                    triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                    previousSponsorName = targetTeam.stadium.sponsorName,
                    allFranchises = updatedTeams,
                    random = random
                )
                return StadiumUltimatumOutcome(
                    success = true,
                    message = summary,
                    updatedFranchises = updatedTeams,
                    updatedStadiumDealState = stateWithOffer,
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
                    var rebuiltTeam = targetTeam
                    val updatedTeams = franchises.map { team ->
                        if (team.id == targetTeam.id) {
                            rebuiltTeam = team.copy(
                                stadiumAge = 0,
                                stadiumCondition = 100,
                                stadiumCapacity = team.stadiumCapacity + 2000,
                                stadium = TeamStadium(),
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
                            rebuiltTeam
                        } else team
                    }
                    val summary = "PRIVATE STADIUM BUILT: ${targetTeam.ownership.primaryOwner.fullName} relented and self-funded a new arena in ${targetTeam.cityName} (+${choice.fanSentimentDelta} Fan Sentiment, ${choice.ownerApprovalDelta} Owner)."
                    val nextUltimatum = currentState.pendingUltimatumQueue.firstOrNull()
                    val nextQueue = currentState.pendingUltimatumQueue.drop(1)
                    val stateWithOffer = enqueueNamingRightsOfferForFranchise(
                        currentState = currentState.copy(
                            activeUltimatum = nextUltimatum,
                            pendingUltimatumQueue = nextQueue,
                            lastDecisionSummary = summary,
                            historyLog = (listOf(summary) + currentState.historyLog).take(20)
                        ),
                        franchise = rebuiltTeam,
                        seasonYear = ultimatum.seasonYear,
                        triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                        previousSponsorName = targetTeam.stadium.sponsorName,
                        allFranchises = updatedTeams,
                        random = random
                    )
                    return StadiumUltimatumOutcome(
                        success = true,
                        message = summary,
                        updatedFranchises = updatedTeams,
                        updatedStadiumDealState = stateWithOffer,
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
                var rebuiltTeam = targetTeam
                val updatedTeams = franchises.map { team ->
                    if (team.id == targetTeam.id) {
                        rebuiltTeam = team.copy(
                            stadiumAge = 0,
                            stadiumCondition = 100,
                            stadiumCapacity = team.stadiumCapacity + 2200,
                            stadium = TeamStadium(),
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
                        rebuiltTeam
                    } else team
                }
                val summary = "LEAGUE STADIUM SUBSIDY: Invested \$${choice.leagueCashCostMillions}M in league funds to build ${targetTeam.teamFullName}'s new arena (+${choice.ownerApprovalDelta} Owner, +${choice.fanSentimentDelta} Fans)."
                val nextUltimatum = currentState.pendingUltimatumQueue.firstOrNull()
                val nextQueue = currentState.pendingUltimatumQueue.drop(1)
                val stateWithOffer = enqueueNamingRightsOfferForFranchise(
                    currentState = currentState.copy(
                        activeUltimatum = nextUltimatum,
                        pendingUltimatumQueue = nextQueue,
                        lastDecisionSummary = summary,
                        historyLog = (listOf(summary) + currentState.historyLog).take(20)
                    ),
                    franchise = rebuiltTeam,
                    seasonYear = ultimatum.seasonYear,
                    triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                    previousSponsorName = targetTeam.stadium.sponsorName,
                    allFranchises = updatedTeams,
                    random = random
                )
                return StadiumUltimatumOutcome(
                    success = true,
                    message = summary,
                    updatedFranchises = updatedTeams,
                    updatedStadiumDealState = stateWithOffer,
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
        selectedCityIdOverride: String? = null,
        salaryCapMillions: Long = 140L
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

            var relocatedTeam = targetTeam
            val updatedFranchises = franchises.map { team ->
                if (team.id == targetTeam.id) {
                    val moved = team.copy(
                        cityId = chosenDeal.cityId,
                        cityName = chosenDeal.cityName,
                        conference = if (shouldRealign) relocConf else team.conference,
                        division = if (shouldRealign) relocDiv else team.division,
                        stadiumAge = 0,
                        stadiumCondition = 100,
                        stadiumCapacity = chosenDeal.newStadiumCapacity,
                        stadium = TeamStadium(),
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
                    relocatedTeam = TeamReputationEngine.recordRelocationBehavior(
                        franchise = moved,
                        salaryCapMillions = salaryCapMillions
                    )
                    relocatedTeam
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
            val stateWithOffer = enqueueNamingRightsOfferForFranchise(
                currentState = currentState.copy(
                    activeUltimatum = nextUltimatum,
                    pendingUltimatumQueue = nextQueue,
                    activeRelocationVote = null,
                    lastDecisionSummary = headline,
                    historyLog = (listOf(headline) + currentState.historyLog).take(20)
                ),
                franchise = relocatedTeam,
                seasonYear = vote.seasonYear,
                triggerReason = NamingRightsTriggerReason.NEW_STADIUM_BUILT,
                previousSponsorName = targetTeam.stadium.sponsorName,
                allFranchises = updatedFranchises
            )
            return RelocationVoteOutcome(
                success = true,
                approved = true,
                message = headline,
                updatedFranchises = updatedFranchises,
                updatedCities = updatedCities,
                updatedStadiumDealState = stateWithOffer,
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

    fun acknowledgeBankruptcyScandal(currentState: StadiumDealState): StadiumDealState {
        val nextScandal = currentState.pendingBankruptcyScandalQueue.firstOrNull()
        val nextQueue = currentState.pendingBankruptcyScandalQueue.drop(1)
        return currentState.copy(
            activeBankruptcyScandal = nextScandal,
            pendingBankruptcyScandalQueue = nextQueue
        )
    }

    fun resolveNamingRightsOffer(
        currentState: StadiumDealState,
        franchises: List<Franchise>,
        offerId: String
    ): NamingRightsSelectionOutcome {
        val session = currentState.activeNamingRightsOffer
            ?: return NamingRightsSelectionOutcome(
                success = false,
                message = "No active Naming Rights Offer is awaiting selection.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState,
                headline = "No Naming Rights Offer active."
            )

        val chosen = session.offers.firstOrNull { it.id == offerId }
            ?: return NamingRightsSelectionOutcome(
                success = false,
                message = "Selected naming rights offer not found.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState,
                headline = "Invalid sponsor offer selection."
            )

        if (!chosen.isEligible) {
            return NamingRightsSelectionOutcome(
                success = false,
                message = chosen.ineligibilityReason ?: "Franchise is ineligible for this sponsor tier.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState,
                headline = chosen.ineligibilityReason ?: "Sponsor tier locked."
            )
        }

        val targetTeam = franchises.firstOrNull { it.id == session.franchiseId }
            ?: return NamingRightsSelectionOutcome(
                success = false,
                message = "Target franchise no longer exists.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState,
                headline = "Franchise not found."
            )

        val conflictTeam = franchises.firstOrNull {
            it.id != targetTeam.id &&
                !it.stadium.sponsorName.isNullOrBlank() &&
                it.stadium.sponsorName.equals(chosen.sponsorName, ignoreCase = true)
        }
        if (conflictTeam != null) {
            return NamingRightsSelectionOutcome(
                success = false,
                message = "${chosen.sponsorName} already holds naming rights for ${conflictTeam.teamFullName}'s stadium.",
                updatedFranchises = franchises,
                updatedStadiumDealState = currentState,
                headline = "Sponsor exclusivity conflict detected."
            )
        }

        val updatedFranchises = franchises.map { team ->
            if (team.id == targetTeam.id) {
                team.copy(
                    stadium = TeamStadium(
                        sponsorName = chosen.sponsorName,
                        sponsorTier = chosen.tier,
                        sponsorYearsLeft = chosen.termYears,
                        namingRightsRevenue = chosen.annualRevenueMillions
                    )
                )
            } else team
        }

        // Refresh any queued sessions so a newly-claimed sponsor is never offered to another franchise's stadium
        val usedKeysAfterSign = collectUsedStadiumSponsors(updatedFranchises)
            .map { normalizeSponsorKey(it) }
            .toSet()
        val cleanedQueue = currentState.pendingNamingRightsQueue.map { queuedSession ->
            val hasCollision = queuedSession.offers.any { normalizeSponsorKey(it.sponsorName) in usedKeysAfterSign }
            if (!hasCollision) {
                queuedSession
            } else {
                val queuedTeam = updatedFranchises.find { it.id == queuedSession.franchiseId }
                if (queuedTeam != null) {
                    buildNamingRightsOfferSession(
                        franchise = queuedTeam,
                        seasonYear = queuedSession.seasonYear,
                        triggerReason = queuedSession.triggerReason,
                        previousSponsorName = queuedSession.previousSponsorName,
                        scandalPrDrain = queuedSession.scandalPrDrain,
                        winsOverride = queuedSession.teamWins,
                        lossesOverride = queuedSession.teamLosses,
                        excludedSponsorNames = collectUsedStadiumSponsors(
                            updatedFranchises,
                            listOfNotNull(queuedSession.previousSponsorName)
                        )
                    )
                } else queuedSession
            }
        }

        val riskTag = if (chosen.tier == 3) " (${chosen.scandalRiskPct}% scandal risk)" else ""
        val headline = "NAMING RIGHTS SIGNED: ${targetTeam.teamFullName} partner with ${chosen.sponsorName} (Tier ${chosen.tier}) for \$${chosen.annualRevenueMillions}M/yr over ${chosen.termYears} yrs$riskTag!"
        val nextOffer = cleanedQueue.firstOrNull()
        val nextOfferQueue = cleanedQueue.drop(1)
        val valBoost = (chosen.annualRevenueMillions / 2L).coerceAtLeast(3L)

        return NamingRightsSelectionOutcome(
            success = true,
            message = headline,
            updatedFranchises = updatedFranchises,
            updatedStadiumDealState = currentState.copy(
                activeNamingRightsOffer = nextOffer,
                pendingNamingRightsQueue = nextOfferQueue,
                lastDecisionSummary = headline,
                historyLog = (listOf(headline) + currentState.historyLog).take(20)
            ),
            chosenOffer = chosen,
            targetFranchiseId = targetTeam.id,
            valuationDeltaMillions = valBoost,
            headline = headline
        )
    }
}
