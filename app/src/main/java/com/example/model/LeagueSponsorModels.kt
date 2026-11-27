package com.example.model

import kotlin.math.roundToLong
import kotlin.random.Random

enum class LeagueSponsorSlotType(
    val key: String,
    val displayName: String,
    val categoryTitle: String,
    val badgeText: String,
    val accentHex: Long,
    val emptyHint: String
) {
    APPAREL(
        key = "apparel",
        displayName = "Apparel",
        categoryTitle = "Official Uniform & Footwear Partner",
        badgeText = "APPAREL SLOT",
        accentHex = 0xFF3B82F6,
        emptyHint = "Outfits all clubs with matchday kits, sideline gear, and retail merchandise."
    ),
    BEVERAGE(
        key = "beverage",
        displayName = "Beverage",
        categoryTitle = "Official Hydration & Pouring Partner",
        badgeText = "BEVERAGE SLOT",
        accentHex = 0xFF10B981,
        emptyHint = "Supplies arena concourses, locker-room hydration stations, and fan hospitality."
    ),
    TECH(
        key = "tech",
        displayName = "Tech",
        categoryTitle = "Official Cloud, Tracking & Replay Partner",
        badgeText = "TECH SLOT",
        accentHex = 0xFF06B6D4,
        emptyHint = "Powers broadcast telemetry, player wearable tracking, and automated replay labs."
    ),
    BETTING(
        key = "betting",
        displayName = "Betting",
        categoryTitle = "Official Odds, Picks & Analytics Partner",
        badgeText = "BETTING SLOT",
        accentHex = 0xFFF59E0B,
        emptyHint = "Monetizes official live odds feeds, pre-game lines, and league prediction broadcasts."
    )
}

enum class LeagueSponsorPerkType(
    val title: String,
    val shortBadge: String,
    val description: String,
    val accentHex: Long
) {
    MERCH_BOOST(
        title = "Global Merch Boost",
        shortBadge = "+Merch Licensing",
        description = "Expands league apparel & replica jersey licensing across all markets.",
        accentHex = 0xFF3B82F6
    ),
    FAN_MORALE_BOOST(
        title = "Fan Atmosphere Lift",
        shortBadge = "+Fan Morale",
        description = "Funds arena giveaways, concourse activations, and gameday fan morale.",
        accentHex = 0xFF10B981
    ),
    INTEGRITY_TECH(
        title = "Telemetry & Replay Shield",
        shortBadge = "+Audit & Replay",
        description = "Integrates real-time biometric and integrity telemetry across official feeds.",
        accentHex = 0xFF06B6D4
    ),
    BROADCAST_LIFT(
        title = "Primetime Audience Surge",
        shortBadge = "+TV Viewership",
        description = "Drives national broadcast engagement with live halftime overlays and pre-show segments.",
        accentHex = 0xFFEAB308
    ),
    PLAYER_RECOVERY(
        title = "Sports Science Grant",
        shortBadge = "+Player Recovery",
        description = "Provides league-wide recovery labs and hydration science for active rosters.",
        accentHex = 0xFF22C55E
    ),
    COMMERCIAL_REACH(
        title = "Market Expansion Reach",
        shortBadge = "+Market Reach",
        description = "Unlocks national co-marketing that increases global fanbase and club valuations.",
        accentHex = 0xFFA855F7
    )
}

enum class SponsorScandalSensitivity(
    val label: String,
    val shortLabel: String,
    val riskScore: Int,
    val isHigh: Boolean,
    val badgeColorHex: Long,
    val clauseSummary: String
) {
    LOW(
        label = "Low Sensitivity",
        shortLabel = "LOW RISK",
        riskScore = 20,
        isHigh = false,
        badgeColorHex = 0xFF10B981,
        clauseSummary = "Standard PR flexibility — will not issue ultimatums over isolated scandals."
    ),
    MODERATE(
        label = "Moderate Sensitivity",
        shortLabel = "MED RISK",
        riskScore = 45,
        isHigh = false,
        badgeColorHex = 0xFF06B6D4,
        clauseSummary = "Monitors public backlash, tolerating measured league discipline."
    ),
    HIGH(
        label = "High Sensitivity",
        shortLabel = "HIGH RISK",
        riskScore = 78,
        isHigh = true,
        badgeColorHex = 0xFFF59E0B,
        clauseSummary = "Strict morality clause — demands maximum suspension/fine or threatens pullout."
    ),
    CRITICAL(
        label = "Zero-Tolerance Clause",
        shortLabel = "EXTREME RISK",
        riskScore = 95,
        isHigh = true,
        badgeColorHex = 0xFFEF4444,
        clauseSummary = "Hair-trigger pullout clause — any player or league scandal sparks an immediate ultimatum."
    )
}

enum class SponsorRuleDemandStatus(val label: String) {
    QUEUED_FOR_OFFSEASON("Queued for Offseason Vote"),
    PASSED_ACTIVE("Passed & Active (+Bonus)"),
    VOTED_DOWN("Voted Down (Breached)"),
    REPEALED("Repealed (Breached)")
}

data class SponsorRuleDemand(
    val ruleId: String,
    val ruleName: String,
    val bonusPayout: Long,
    val fanPenalty: Int,
    val unionPenalty: Int,
    val summary: String = "",
    val status: SponsorRuleDemandStatus = SponsorRuleDemandStatus.QUEUED_FOR_OFFSEASON
) {
    val isPassed: Boolean
        get() = status == SponsorRuleDemandStatus.PASSED_ACTIVE

    val isBreached: Boolean
        get() = status == SponsorRuleDemandStatus.VOTED_DOWN || status == SponsorRuleDemandStatus.REPEALED
}

data class OffseasonSponsorRuleVoteItem(
    val ruleId: String,
    val ruleName: String,
    val bonusPayout: Long,
    val fanPenalty: Int,
    val unionPenalty: Int,
    val summary: String,
    val slotType: LeagueSponsorSlotType,
    val sponsorName: String,
    val targetOffseasonYear: Int
)

data class SponsorBreachWarning(
    val id: String,
    val slotType: LeagueSponsorSlotType,
    val sponsorName: String,
    val ruleId: String,
    val ruleName: String,
    val breachReason: String,
    val originalPayoutMillions: Long,
    val reducedPayoutMillions: Long,
    val forfeitedBonusMillions: Long,
    val title: String,
    val message: String,
    val seasonYear: Int,
    val weekNumber: Int
)

data class LeagueSponsorContract(
    val id: String,
    val slotType: LeagueSponsorSlotType,
    val name: String,
    val payout: Long, // Annual league revenue payout in $M
    val yearsLeft: Int,
    val perkType: LeagueSponsorPerkType,
    val scandalSensitivity: SponsorScandalSensitivity,
    val tagline: String = "",
    val ruleDemand: SponsorRuleDemand? = null,
    val stabilityScore: Int = 100,
    val hasMoralityWarning: Boolean = false,
    val moralityWarningReason: String? = null
) {
    val bonusPayoutActiveMillions: Long
        get() = if (ruleDemand != null && ruleDemand.isPassed) ruleDemand.bonusPayout.coerceAtLeast(0L) else 0L

    val totalEffectiveAnnualPayoutMillions: Long
        get() = payout.coerceAtLeast(0L) + bonusPayoutActiveMillions
}

data class LeagueSponsorOffer(
    val id: String,
    val slotType: LeagueSponsorSlotType,
    val name: String,
    val payout: Long, // Annual payout in $M
    val termYears: Int,
    val perkType: LeagueSponsorPerkType,
    val scandalSensitivity: SponsorScandalSensitivity,
    val tagline: String,
    val tradeoffNote: String,
    val ruleDemand: SponsorRuleDemand? = null
) {
    fun toContract(): LeagueSponsorContract = LeagueSponsorContract(
        id = "lspon_${slotType.key}_${id.takeLast(8)}",
        slotType = slotType,
        name = name,
        payout = payout,
        yearsLeft = termYears,
        perkType = perkType,
        scandalSensitivity = scandalSensitivity,
        tagline = tagline,
        ruleDemand = ruleDemand
    )
}

data class LeagueSponsorOfferSession(
    val id: String,
    val slotType: LeagueSponsorSlotType,
    val seasonYear: Int,
    val offers: List<LeagueSponsorOffer>
)

data class SponsorUltimatumEvent(
    val id: String,
    val slotType: LeagueSponsorSlotType,
    val sponsorName: String,
    val sponsorPayoutMillions: Long,
    val scandalSensitivity: SponsorScandalSensitivity,
    val scandalSourceTitle: String,
    val scandalSummary: String,
    val relatedIncidentId: String? = null,
    val relatedPlayerName: String? = null,
    val relatedFranchiseId: String? = null,
    val relatedFranchiseName: String? = null,
    val requiredMaxSuspensionGames: Int,
    val requiredMaxFineThousands: Int,
    val seasonYear: Int,
    val weekNumber: Int
)

data class LeagueSponsorsState(
    val apparel: LeagueSponsorContract? = null,
    val beverage: LeagueSponsorContract? = null,
    val tech: LeagueSponsorContract? = null,
    val betting: LeagueSponsorContract? = null,
    val activeOfferSession: LeagueSponsorOfferSession? = null,
    val activeUltimatum: SponsorUltimatumEvent? = null,
    val pendingUltimatumQueue: List<SponsorUltimatumEvent> = emptyList(),
    val pendingOffseasonRuleVotes: List<OffseasonSponsorRuleVoteItem> = emptyList(),
    val activeBreachWarning: SponsorBreachWarning? = null,
    val breachWarningHistory: List<SponsorBreachWarning> = emptyList(),
    val expiredSlotNotices: List<String> = emptyList(),
    val activeLeagueScandal: LeagueScandalIncident? = null,
    val scandalHistory: List<LeagueScandalIncident> = emptyList(),
    val lastSummary: String = "4 official league sponsor slots open — balance annual payout size against scandal sensitivity."
) {
    val totalAnnualPayoutMillions: Long
        get() = listOfNotNull(apparel, beverage, tech, betting).sumOf { it.totalEffectiveAnnualPayoutMillions }

    val totalBonusPayoutMillions: Long
        get() = listOfNotNull(apparel, beverage, tech, betting).sumOf { it.bonusPayoutActiveMillions }

    val activeCount: Int
        get() = listOfNotNull(apparel, beverage, tech, betting).size

    fun getSlot(slotType: LeagueSponsorSlotType): LeagueSponsorContract? = when (slotType) {
        LeagueSponsorSlotType.APPAREL -> apparel
        LeagueSponsorSlotType.BEVERAGE -> beverage
        LeagueSponsorSlotType.TECH -> tech
        LeagueSponsorSlotType.BETTING -> betting
    }

    fun withSlot(slotType: LeagueSponsorSlotType, contract: LeagueSponsorContract?): LeagueSponsorsState = when (slotType) {
        LeagueSponsorSlotType.APPAREL -> copy(apparel = contract)
        LeagueSponsorSlotType.BEVERAGE -> copy(beverage = contract)
        LeagueSponsorSlotType.TECH -> copy(tech = contract)
        LeagueSponsorSlotType.BETTING -> copy(betting = contract)
    }

    fun allActiveContracts(): List<LeagueSponsorContract> = listOfNotNull(apparel, beverage, tech, betting)

    fun highSensitivityContracts(): List<LeagueSponsorContract> = allActiveContracts().filter { it.scandalSensitivity.isHigh }
}

data class LeagueSponsorSignOutcome(
    val success: Boolean,
    val message: String,
    val updatedState: LeagueSponsorsState,
    val signedContract: LeagueSponsorContract? = null,
    val queuedRuleVote: OffseasonSponsorRuleVoteItem? = null,
    val projectedNextCapMillions: Long = 0L,
    val headline: String
)

data class SponsorRuleVoteOutcome(
    val success: Boolean,
    val passed: Boolean,
    val ruleId: String,
    val ruleName: String,
    val message: String,
    val updatedSponsorsState: LeagueSponsorsState,
    val updatedFranchises: List<Franchise>,
    val updatedUnion: LaborUnionAgreement,
    val updatedRuleBook: LeagueRuleBook,
    val updatedRuleCommittee: RuleCommitteeState,
    val breachWarning: SponsorBreachWarning? = null,
    val bonusPayoutAddedMillions: Long = 0L,
    val headline: String
)

data class SponsorBreachResult(
    val triggered: Boolean,
    val updatedSponsorsState: LeagueSponsorsState,
    val warning: SponsorBreachWarning? = null,
    val headline: String? = null
)

data class SponsorUltimatumResolutionOutcome(
    val success: Boolean,
    val enforcedMaxPenalty: Boolean,
    val message: String,
    val updatedSponsorsState: LeagueSponsorsState,
    val updatedDisciplinaryState: DisciplinaryDeskState,
    val updatedFranchises: List<Franchise>,
    val updatedUnion: LaborUnionAgreement,
    val cashTreasuryGainMillions: Long = 0L,
    val integrityDelta: Int = 0,
    val ownerApprovalDelta: Int = 0,
    val valuationDeltaMillions: Long = 0L,
    val headline: String
)

object LeagueSponsorsEngine {

    private val apparelPool = listOf(
        Triple("Heritage Knitworks", "Crafted throwback jerseys & classic teamwear", LeagueSponsorPerkType.MERCH_BOOST),
        Triple("CivicThreads Athletic", "Durable sideline basics & community youth apparel", LeagueSponsorPerkType.FAN_MORALE_BOOST),
        Triple("NorthCrest Apparel", "All-weather training gear & fleece essentials", LeagueSponsorPerkType.PLAYER_RECOVERY),
        Triple("MetroPulse Athletics", "Modern performance compression & court sneakers", LeagueSponsorPerkType.MERCH_BOOST),
        Triple("Vanguard Sportswear", "High-margin specialty editions & arena fan stores", LeagueSponsorPerkType.COMMERCIAL_REACH),
        Triple("ApexStride Performance", "Premier championship signature footwear & kits", LeagueSponsorPerkType.MERCH_BOOST),
        Triple("Dynasty Couture Sport", "Ultra-premium fashion collabs & luxury tunnel wear", LeagueSponsorPerkType.COMMERCIAL_REACH),
        Triple("Titanium Pro Gear", "Flagship bespoke uniform engineering & global drops", LeagueSponsorPerkType.MERCH_BOOST)
    )

    private val beveragePool = listOf(
        Triple("Heartland Springs Co.", "Pure mineral water & family arena concessions", LeagueSponsorPerkType.PLAYER_RECOVERY),
        Triple("Sunbelt Citrus Soda", "Classic sparkling refreshments & stadium fountains", LeagueSponsorPerkType.FAN_MORALE_BOOST),
        Triple("Summit Valley Cold Brew", "Craft morning coffee & broadcast studio bar", LeagueSponsorPerkType.BROADCAST_LIFT),
        Triple("HydroCharge Isotonics", "Electrolyte sideline bottles & bench coolers", LeagueSponsorPerkType.PLAYER_RECOVERY),
        Triple("Cascade Spark Water", "Zero-sugar botanical fizz & eco aluminum cans", LeagueSponsorPerkType.COMMERCIAL_REACH),
        Triple("VoltRush Energy", "High-caffeine gametime energy & concert tailgates", LeagueSponsorPerkType.FAN_MORALE_BOOST),
        Triple("Apex Fuel Labs", "Pro-grade recovery shakes & high-spend signage", LeagueSponsorPerkType.PLAYER_RECOVERY),
        Triple("HyperPulse Elixir", "Aggressive neon energy drink with massive ad buy", LeagueSponsorPerkType.BROADCAST_LIFT)
    )

    private val techPool = listOf(
        Triple("BlueRidge Cloud Co.", "Reliable league ticketing & stadium Wi-Fi", LeagueSponsorPerkType.COMMERCIAL_REACH),
        Triple("Beacon Fiber Systems", "High-uptime arena press-box connectivity", LeagueSponsorPerkType.BROADCAST_LIFT),
        Triple("ClearMetric Analytics", "Clean play-by-play shot charts & coaching tablets", LeagueSponsorPerkType.INTEGRITY_TECH),
        Triple("Helios Biometrics", "Player fatigue wearables & recovery dashboards", LeagueSponsorPerkType.PLAYER_RECOVERY),
        Triple("Orion Vision Replay", "Ultra-slow-motion officiating review cameras", LeagueSponsorPerkType.INTEGRITY_TECH),
        Triple("Aether AI Cloud", "Autonomous highlight clipping & global stream CDN", LeagueSponsorPerkType.BROADCAST_LIFT),
        Triple("QuantumCore Silicon", "Premier AI telemetry powerhouse with strict brand standards", LeagueSponsorPerkType.INTEGRITY_TECH),
        Triple("Vertex Neural Labs", "Mega-cap flagship AI suite demanding spotless optics", LeagueSponsorPerkType.COMMERCIAL_REACH)
    )

    private val bettingPool = listOf(
        Triple("Gridiron FairPicks", "Low-key free-to-play bracket picks & civic trivia", LeagueSponsorPerkType.FAN_MORALE_BOOST),
        Triple("StatLine Predict Co.", "Statistical probability graphics for broadcast desks", LeagueSponsorPerkType.BROADCAST_LIFT),
        Triple("Summit Fantasy Club", "Season-long roster fantasy league sponsorship", LeagueSponsorPerkType.COMMERCIAL_REACH),
        Triple("Pinnacle Odds Network", "Official pre-game line graphics & analytical feeds", LeagueSponsorPerkType.BROADCAST_LIFT),
        Triple("TrueLine Analytics", "In-game win probability & integrity monitoring", LeagueSponsorPerkType.INTEGRITY_TECH),
        Triple("CrownWager Partners", "High-volume marquee sportsbook broadcast partner", LeagueSponsorPerkType.BROADCAST_LIFT),
        Triple("blitzEdge LiveOdds", "Aggressive live-prop sponsor demanding zero integrity hiccups", LeagueSponsorPerkType.COMMERCIAL_REACH),
        Triple("ApexBet Global", "Record-breaking headline bookmaker with zero-tolerance compliance", LeagueSponsorPerkType.BROADCAST_LIFT)
    )

    private fun poolFor(slotType: LeagueSponsorSlotType): List<Triple<String, String, LeagueSponsorPerkType>> = when (slotType) {
        LeagueSponsorSlotType.APPAREL -> apparelPool
        LeagueSponsorSlotType.BEVERAGE -> beveragePool
        LeagueSponsorSlotType.TECH -> techPool
        LeagueSponsorSlotType.BETTING -> bettingPool
    }

    /**
     * Generates 6 distinct procedural sponsor offers for a given slot, explicitly trading off
     * annual payout size ($M/yr) against scandal sensitivity (Low -> Moderate -> High -> Critical).
     */
    fun generateOffersForSlot(
        slotType: LeagueSponsorSlotType,
        seasonYear: Int,
        existingSponsorNames: Set<String> = emptySet(),
        random: Random = Random.Default
    ): List<LeagueSponsorOffer> {
        val pool = poolFor(slotType)
        val normalizedExcluded = existingSponsorNames.map { it.trim().lowercase() }.toMutableSet()
        val lowBand = pool.take(3).filter { it.first.lowercase() !in normalizedExcluded }.shuffled(random)
        val midBand = pool.drop(3).take(3).filter { it.first.lowercase() !in normalizedExcluded }.shuffled(random)
        val highBand = pool.drop(6).filter { it.first.lowercase() !in normalizedExcluded }.shuffled(random)

        val chosenEntries = mutableListOf<Pair<Triple<String, String, LeagueSponsorPerkType>, SponsorScandalSensitivity>>()

        fun addUnique(
            candidates: List<Triple<String, String, LeagueSponsorPerkType>>,
            fallbackPool: List<Triple<String, String, LeagueSponsorPerkType>>,
            count: Int,
            sensitivity: SponsorScandalSensitivity
        ) {
            var added = 0
            (candidates + fallbackPool.shuffled(random)).forEach { item ->
                if (added >= count) return
                val key = item.first.lowercase()
                if (key !in normalizedExcluded) {
                    chosenEntries.add(item to sensitivity)
                    normalizedExcluded.add(key)
                    added++
                }
            }
        }

        // 2 Low Sensitivity (Lower Payout), 2 Moderate Sensitivity (Balanced), 2 High/Critical Sensitivity (Highest Payout)
        addUnique(lowBand, pool, 2, SponsorScandalSensitivity.LOW)
        addUnique(midBand, pool, 2, SponsorScandalSensitivity.MODERATE)
        addUnique(highBand.take(1), pool, 1, SponsorScandalSensitivity.HIGH)
        addUnique(highBand.drop(1), pool, 1, SponsorScandalSensitivity.CRITICAL)

        val categoryBaseMultiplier = when (slotType) {
            LeagueSponsorSlotType.APPAREL -> 1L
            LeagueSponsorSlotType.BEVERAGE -> 0L
            LeagueSponsorSlotType.TECH -> 2L
            LeagueSponsorSlotType.BETTING -> 4L
        }

        return chosenEntries.mapIndexed { idx, (entry, sensitivity) ->
            val payout = when (sensitivity) {
                SponsorScandalSensitivity.LOW -> random.nextLong(6L, 12L) + categoryBaseMultiplier
                SponsorScandalSensitivity.MODERATE -> random.nextLong(13L, 21L) + categoryBaseMultiplier
                SponsorScandalSensitivity.HIGH -> random.nextLong(22L, 30L) + categoryBaseMultiplier
                SponsorScandalSensitivity.CRITICAL -> random.nextLong(30L, 40L) + categoryBaseMultiplier
            }
            val termYears = when (sensitivity) {
                SponsorScandalSensitivity.LOW -> random.nextInt(3, 6)
                SponsorScandalSensitivity.MODERATE -> random.nextInt(2, 5)
                SponsorScandalSensitivity.HIGH -> random.nextInt(2, 4)
                SponsorScandalSensitivity.CRITICAL -> random.nextInt(1, 4)
            }
            val fixedName = entry.first.replaceFirstChar { it.uppercaseChar() }
            val ruleDemand = if (sensitivity.isHigh) {
                createRuleDemandForOffer(
                    slotType = slotType,
                    sensitivity = sensitivity,
                    sponsorName = fixedName
                )
            } else {
                null
            }
            val tradeoffNote = when (sensitivity) {
                SponsorScandalSensitivity.LOW -> "Safe Payout • Zero ultimatum trigger on league scandals"
                SponsorScandalSensitivity.MODERATE -> "Mid-Tier Payout • Stable under routine league turbulence"
                SponsorScandalSensitivity.HIGH -> "High Payout Tradeoff • Demands '${ruleDemand?.ruleName}' (+$${ruleDemand?.bonusPayout}M bonus) & strict scandal ultimatum"
                SponsorScandalSensitivity.CRITICAL -> "Mega Payout Tradeoff • Demands '${ruleDemand?.ruleName}' (+$${ruleDemand?.bonusPayout}M bonus) & zero-tolerance ultimatum"
            }
            LeagueSponsorOffer(
                id = "lso_${slotType.key}_y${seasonYear}_${idx}_${random.nextInt(1000, 9999)}",
                slotType = slotType,
                name = fixedName,
                payout = payout,
                termYears = termYears,
                perkType = entry.third,
                scandalSensitivity = sensitivity,
                tagline = entry.second,
                tradeoffNote = tradeoffNote,
                ruleDemand = ruleDemand
            )
        }.sortedBy { it.payout }
    }

    private fun createRuleDemandForOffer(
        slotType: LeagueSponsorSlotType,
        sensitivity: SponsorScandalSensitivity,
        sponsorName: String
    ): SponsorRuleDemand {
        val isCritical = sensitivity == SponsorScandalSensitivity.CRITICAL
        return when (slotType) {
            LeagueSponsorSlotType.APPAREL -> if (!isCritical) {
                SponsorRuleDemand(
                    ruleId = "rule_spon_apparel_jersey_patches",
                    ruleName = "Mandatory Commercial Jersey Patches",
                    bonusPayout = 8L,
                    fanPenalty = 5,
                    unionPenalty = 4,
                    summary = "$sponsorName requires oversized brand patches and QR hem tags on all gameday uniforms."
                )
            } else {
                SponsorRuleDemand(
                    ruleId = "rule_spon_apparel_exclusive_sideline",
                    ruleName = "Exclusive Footwear & Compression Mandate",
                    bonusPayout = 12L,
                    fanPenalty = 7,
                    unionPenalty = 9,
                    summary = "$sponsorName bars competing footwear and personal athlete tape-over logos during all televised play."
                )
            }

            LeagueSponsorSlotType.BEVERAGE -> if (!isCritical) {
                SponsorRuleDemand(
                    ruleId = "rule_spon_beverage_hydration_timeouts",
                    ruleName = "Mandatory 90s Hydration Timeout Stops",
                    bonusPayout = 7L,
                    fanPenalty = 6,
                    unionPenalty = 4,
                    summary = "$sponsorName requires two extra stoppage-clock commercial hydration breaks every half."
                )
            } else {
                SponsorRuleDemand(
                    ruleId = "rule_spon_beverage_bench_exclusivity",
                    ruleName = "Bench Cooler & Bottle Lockout Mandate",
                    bonusPayout = 11L,
                    fanPenalty = 8,
                    unionPenalty = 6,
                    summary = "$sponsorName prohibits non-branded drinks anywhere on benches or media podiums with automatic player fines."
                )
            }

            LeagueSponsorSlotType.TECH -> if (!isCritical) {
                SponsorRuleDemand(
                    ruleId = "rule_spon_tech_biometric_wearables",
                    ruleName = "Mandatory Live Biometric Telemetry",
                    bonusPayout = 9L,
                    fanPenalty = 3,
                    unionPenalty = 8,
                    summary = "$sponsorName requires every active player to wear heart-rate and fatigue telemetry vests during live broadcasts."
                )
            } else {
                SponsorRuleDemand(
                    ruleId = "rule_spon_tech_locker_microphones",
                    ruleName = "Live Bench & Locker-Room Audio Access",
                    bonusPayout = 14L,
                    fanPenalty = 4,
                    unionPenalty = 11,
                    summary = "$sponsorName demands hot-mic sideline huddles and halftime locker-room camera telemetry."
                )
            }

            LeagueSponsorSlotType.BETTING -> if (!isCritical) {
                SponsorRuleDemand(
                    ruleId = "rule_spon_betting_injury_wire",
                    ruleName = "Real-Time Pre-Game Injury Wire Rule",
                    bonusPayout = 10L,
                    fanPenalty = 5,
                    unionPenalty = 7,
                    summary = "$sponsorName mandates public medical status and minute-by-minute availability updates 4 hours before kickoff/tipoff."
                )
            } else {
                SponsorRuleDemand(
                    ruleId = "rule_spon_betting_live_odds_overlay",
                    ruleName = "Live Broadcast Odds & Prop Integration",
                    bonusPayout = 16L,
                    fanPenalty = 9,
                    unionPenalty = 8,
                    summary = "$sponsorName requires persistent live point-spread crawls and in-arena odds boards during crucial plays."
                )
            }
        }
    }

    fun buildLeagueRuleFromDemand(
        demand: SponsorRuleDemand,
        slotType: LeagueSponsorSlotType,
        sponsorName: String,
        isEnacted: Boolean
    ): LeagueRule = LeagueRule(
        id = demand.ruleId,
        name = demand.ruleName,
        category = when (slotType) {
            LeagueSponsorSlotType.APPAREL, LeagueSponsorSlotType.BEVERAGE -> RuleCategory.FINANCIAL_PARITY
            LeagueSponsorSlotType.TECH -> RuleCategory.GAMEPLAY_PACE
            LeagueSponsorSlotType.BETTING -> RuleCategory.INTEGRITY_CONDUCT
        },
        summary = demand.summary.ifBlank {
            "$sponsorName commercial covenant rule (+$${demand.bonusPayout}M/yr revenue, -${demand.fanPenalty} Fan, -${demand.unionPenalty} Union)."
        },
        isEnacted = isEnacted,
        parityImpact = 0,
        scoringTempoModifier = 0,
        integrityImpact = if (slotType == LeagueSponsorSlotType.BETTING) -2 else 0,
        fanAppealModifier = -demand.fanPenalty,
        smallMarketBenefitRating = 3,
        description = "Official League Sponsor covenant attached to $sponsorName (${slotType.displayName}). Sustaining this law unlocks +$${demand.bonusPayout}M/yr in league revenue.",
        enactedEffect = "Enacted: Adds +$${demand.bonusPayout}M/yr to league revenue and Salary Cap; Fan Morale -${demand.fanPenalty}, Union Satisfaction -${demand.unionPenalty}.",
        repealedEffect = "Repealing while $sponsorName is active breaches the covenant: sponsor payout is cut in half and bonus revenue is forfeited.",
        proPerspective = "Commercial Partners & Owners: Unlocks +$${demand.bonusPayout}M/yr in guaranteed revenue sharing.",
        contraPerspective = "Fans (-${demand.fanPenalty}) & Players Union (-${demand.unionPenalty}): Resent intrusive commercial mandates."
    )

    fun buildCommitteeProposalFromDemand(
        demand: SponsorRuleDemand,
        slotType: LeagueSponsorSlotType,
        sponsorName: String,
        sportType: SportType
    ): SportRuleAmendment = SportRuleAmendment(
        id = demand.ruleId,
        sportType = sportType,
        title = demand.ruleName,
        categoryLabel = "Sponsor Rule Demand • Offseason Vote",
        sponsorName = "$sponsorName (${slotType.displayName})",
        focusLever = RuleCommitteeFocusLever.OFFICIATING_INTEGRITY,
        summary = demand.summary.ifBlank {
            "Contractual rule demand by $sponsorName queued for the next Offseason Rules Committee vote."
        },
        simulationEffectSummary = "If Passed: +$${demand.bonusPayout}M/yr annual league revenue & +Salary Cap, Fan -${demand.fanPenalty}, Union -${demand.unionPenalty}. If Voted Down/Repealed: sponsor payout slashed 50%.",
        proPerspective = "Unlocks +$${demand.bonusPayout}M/yr bonus revenue for league treasury & salary cap growth.",
        contraPerspective = "Incurs Fan penalty (-${demand.fanPenalty}) and Players Union penalty (-${demand.unionPenalty}).",
        fanAppealDelta = -demand.fanPenalty,
        status = if (demand.isPassed) SportRuleProposalStatus.ENACTED else SportRuleProposalStatus.PROPOSED_DOCKET,
        committeeReviewNote = if (demand.isPassed) {
            "Passed Offseason Rules Committee vote (+$${demand.bonusPayout}M/yr active)."
        } else {
            "Queued for Offseason Rules Committee vote by $sponsorName."
        },
        isCustomProposal = true
    )

    fun openOfferSessionForSlot(
        currentState: LeagueSponsorsState,
        slotType: LeagueSponsorSlotType,
        seasonYear: Int,
        random: Random = Random.Default
    ): LeagueSponsorsState {
        val activeNames = currentState.allActiveContracts().map { it.name }.toSet()
        val offers = generateOffersForSlot(
            slotType = slotType,
            seasonYear = seasonYear,
            existingSponsorNames = activeNames,
            random = random
        )
        return currentState.copy(
            activeOfferSession = LeagueSponsorOfferSession(
                id = "ls_session_${slotType.key}_y${seasonYear}_${random.nextInt(100, 999)}",
                slotType = slotType,
                seasonYear = seasonYear,
                offers = offers
            )
        )
    }

    fun signOfferIntoSlot(
        currentState: LeagueSponsorsState,
        offerId: String,
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        unionAgreement: LaborUnionAgreement
    ): LeagueSponsorSignOutcome {
        val session = currentState.activeOfferSession
            ?: return LeagueSponsorSignOutcome(
                success = false,
                message = "No active League Sponsor offer session open.",
                updatedState = currentState,
                headline = "Sponsor offer session unavailable."
            )
        if (currentState.getSlot(session.slotType) != null) {
            return LeagueSponsorSignOutcome(
                success = false,
                message = "${session.slotType.displayName} slot is already occupied.",
                updatedState = currentState.copy(activeOfferSession = null),
                headline = "${session.slotType.displayName} slot is already occupied."
            )
        }
        val offer = session.offers.firstOrNull { it.id == offerId }
            ?: return LeagueSponsorSignOutcome(
                success = false,
                message = "Selected sponsor offer could not be found.",
                updatedState = currentState,
                headline = "Invalid sponsor offer selection."
            )

        val contract = offer.toContract()
        val queuedVote = contract.ruleDemand?.let { demand ->
            OffseasonSponsorRuleVoteItem(
                ruleId = demand.ruleId,
                ruleName = demand.ruleName,
                bonusPayout = demand.bonusPayout,
                fanPenalty = demand.fanPenalty,
                unionPenalty = demand.unionPenalty,
                summary = demand.summary,
                slotType = session.slotType,
                sponsorName = contract.name,
                targetOffseasonYear = session.seasonYear
            )
        }
        val updatedPendingVotes = if (queuedVote != null) {
            currentState.pendingOffseasonRuleVotes.filterNot {
                it.ruleId == queuedVote.ruleId || it.slotType == queuedVote.slotType
            } + queuedVote
        } else {
            currentState.pendingOffseasonRuleVotes
        }
        val ruleDemandSuffix = if (queuedVote != null) {
            " '${queuedVote.ruleName}' queued for next Offseason Rules Committee vote."
        } else ""

        val updatedSponsors = currentState
            .withSlot(session.slotType, contract)
            .copy(
                activeOfferSession = null,
                pendingOffseasonRuleVotes = updatedPendingVotes,
                lastSummary = "Signed ${contract.name} to ${session.slotType.displayName} slot (+\$${contract.payout}M/yr, ${contract.yearsLeft}Y, ${contract.scandalSensitivity.label}).$ruleDemandSuffix"
            )
        val projection = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = franchises,
            activeBroadcast = activeBroadcast,
            unionAgreement = unionAgreement,
            leagueSponsors = updatedSponsors
        )
        val headline = "LEAGUE SPONSOR SIGNED (${session.slotType.displayName.uppercase()}): ${contract.name} inked ${contract.yearsLeft}Y/\$${contract.payout}M per yr (${contract.perkType.title} • ${contract.scandalSensitivity.label}).$ruleDemandSuffix Projected Cap: \$${projection.nextSalaryCapMillions}M."
        return LeagueSponsorSignOutcome(
            success = true,
            message = "Signed ${contract.name} (+\$${contract.payout}M/yr) to ${session.slotType.displayName}.$ruleDemandSuffix",
            updatedState = updatedSponsors,
            signedContract = contract,
            queuedRuleVote = queuedVote,
            projectedNextCapMillions = projection.nextSalaryCapMillions,
            headline = headline
        )
    }

    /**
     * Resolves a sponsor ruleDemand during the Offseason Rules Committee vote.
     * - approve = true: passes & activates rule in league rules list, applies fanPenalty & unionPenalty,
     *   and activates bonusPayout toward annual league revenue.
     * - approve = false: votes down the rule, cuts the sponsor's payout in half, and triggers an
     *   angry 'Sponsor Breach Warning' event card.
     */
    fun resolveSponsorRuleDemandVote(
        currentSponsorsState: LeagueSponsorsState,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement,
        ruleBook: LeagueRuleBook,
        ruleCommitteeState: RuleCommitteeState,
        ruleId: String,
        approve: Boolean,
        seasonYear: Int,
        weekNumber: Int
    ): SponsorRuleVoteOutcome {
        val matchingContract = currentSponsorsState.allActiveContracts()
            .firstOrNull { it.ruleDemand?.ruleId == ruleId }
        val queuedVote = currentSponsorsState.pendingOffseasonRuleVotes.firstOrNull { it.ruleId == ruleId }
        val demand = matchingContract?.ruleDemand ?: queuedVote?.let {
            SponsorRuleDemand(
                ruleId = it.ruleId,
                ruleName = it.ruleName,
                bonusPayout = it.bonusPayout,
                fanPenalty = it.fanPenalty,
                unionPenalty = it.unionPenalty,
                summary = it.summary
            )
        }
        val slotType = matchingContract?.slotType ?: queuedVote?.slotType
        val sponsorName = matchingContract?.name ?: queuedVote?.sponsorName

        if (demand == null || slotType == null || sponsorName == null) {
            return SponsorRuleVoteOutcome(
                success = false,
                passed = approve,
                ruleId = ruleId,
                ruleName = "",
                message = "No active sponsor rule demand found for '$ruleId'.",
                updatedSponsorsState = currentSponsorsState,
                updatedFranchises = franchises,
                updatedUnion = unionAgreement,
                updatedRuleBook = ruleBook,
                updatedRuleCommittee = ruleCommitteeState,
                headline = "Sponsor rule demand not found."
            )
        }

        val remainingVotes = currentSponsorsState.pendingOffseasonRuleVotes.filterNot { it.ruleId == ruleId }
        val currentRules = ruleBook.rules.toMutableList()
        val existingRuleIdx = currentRules.indexOfFirst { it.id == demand.ruleId }

        if (approve) {
            val passedDemand = demand.copy(status = SponsorRuleDemandStatus.PASSED_ACTIVE)
            val updatedContract = matchingContract?.copy(ruleDemand = passedDemand)
            val updatedSponsors = (if (updatedContract != null) {
                currentSponsorsState.withSlot(slotType, updatedContract)
            } else currentSponsorsState).copy(
                pendingOffseasonRuleVotes = remainingVotes,
                lastSummary = "PASSED SPONSOR RULE: '${demand.ruleName}' enacted for $sponsorName (+\$${demand.bonusPayout}M/yr bonus active; Fan -${demand.fanPenalty}, Union -${demand.unionPenalty})."
            )

            // Activate rule in league rules list
            val leagueRule = buildLeagueRuleFromDemand(
                demand = passedDemand,
                slotType = slotType,
                sponsorName = sponsorName,
                isEnacted = true
            )
            if (existingRuleIdx >= 0) {
                currentRules[existingRuleIdx] = currentRules[existingRuleIdx].copy(isEnacted = true)
            } else {
                currentRules.add(leagueRule)
            }
            val updatedRuleBook = ruleBook.copy(rules = currentRules)

            // Sync Rule Committee proposals
            val propIdx = ruleCommitteeState.proposals.indexOfFirst { it.id == demand.ruleId }
            val updatedProposals = ruleCommitteeState.proposals.toMutableList()
            if (propIdx >= 0) {
                updatedProposals[propIdx] = updatedProposals[propIdx].copy(
                    status = SportRuleProposalStatus.ENACTED,
                    committeeReviewNote = "Passed Offseason Rules Committee vote (+$${demand.bonusPayout}M/yr bonus active)."
                )
            } else {
                updatedProposals.add(
                    0,
                    buildCommitteeProposalFromDemand(
                        demand = passedDemand,
                        slotType = slotType,
                        sponsorName = sponsorName,
                        sportType = ruleCommitteeState.sportType
                    )
                )
            }
            val updatedCommittee = ruleCommitteeState.copy(proposals = updatedProposals)

            // Apply fanPenalty and unionPenalty
            val updatedFranchises = franchises.map { team ->
                team.copy(
                    fanMorale = (team.fanMorale - demand.fanPenalty).coerceIn(15, 99),
                    fanLoyalty = (team.fanLoyalty - (demand.fanPenalty / 2)).coerceIn(20, 99)
                )
            }
            val updatedUnion = unionAgreement.copy(
                unionSatisfaction = (unionAgreement.unionSatisfaction - demand.unionPenalty).coerceIn(10, 100),
                strikeRisk = (unionAgreement.strikeRisk + ((demand.unionPenalty + 1) / 2)).coerceIn(0, 100)
            )
            val headline = "OFFSEASON RULES COMMITTEE PASSED '${demand.ruleName}' ($sponsorName): +$${demand.bonusPayout}M/yr bonus added to annual league revenue! (Fan -${demand.fanPenalty}, Union -${demand.unionPenalty})"
            return SponsorRuleVoteOutcome(
                success = true,
                passed = true,
                ruleId = demand.ruleId,
                ruleName = demand.ruleName,
                message = headline,
                updatedSponsorsState = updatedSponsors,
                updatedFranchises = updatedFranchises,
                updatedUnion = updatedUnion,
                updatedRuleBook = updatedRuleBook,
                updatedRuleCommittee = updatedCommittee,
                bonusPayoutAddedMillions = demand.bonusPayout,
                headline = headline
            )
        } else {
            // Voted down -> halve active sponsor payout and fire angry Sponsor Breach Warning
            val breachResult = if (matchingContract != null) {
                applyBreachPenaltyToContract(
                    currentState = currentSponsorsState.copy(pendingOffseasonRuleVotes = remainingVotes),
                    contract = matchingContract,
                    newStatus = SponsorRuleDemandStatus.VOTED_DOWN,
                    breachReason = "voted down in the Offseason Rules Committee",
                    seasonYear = seasonYear,
                    weekNumber = weekNumber
                )
            } else {
                SponsorBreachResult(
                    triggered = false,
                    updatedSponsorsState = currentSponsorsState.copy(pendingOffseasonRuleVotes = remainingVotes),
                    warning = null,
                    headline = "Voted down '${demand.ruleName}'."
                )
            }

            if (existingRuleIdx >= 0) {
                currentRules[existingRuleIdx] = currentRules[existingRuleIdx].copy(isEnacted = false)
            }
            val updatedRuleBook = ruleBook.copy(rules = currentRules)

            val updatedProposals = ruleCommitteeState.proposals.map { prop ->
                if (prop.id == demand.ruleId) {
                    prop.copy(
                        status = SportRuleProposalStatus.TABLED,
                        committeeReviewNote = "Voted down in Offseason Rules Committee — $sponsorName payout cut 50%."
                    )
                } else prop
            }
            val updatedCommittee = ruleCommitteeState.copy(proposals = updatedProposals)
            val headline = breachResult.headline
                ?: "OFFSEASON RULES COMMITTEE VOTED DOWN '${demand.ruleName}' — $sponsorName sponsorship breached!"

            return SponsorRuleVoteOutcome(
                success = true,
                passed = false,
                ruleId = demand.ruleId,
                ruleName = demand.ruleName,
                message = headline,
                updatedSponsorsState = breachResult.updatedSponsorsState,
                updatedFranchises = franchises,
                updatedUnion = unionAgreement,
                updatedRuleBook = updatedRuleBook,
                updatedRuleCommittee = updatedCommittee,
                breachWarning = breachResult.warning,
                bonusPayoutAddedMillions = 0L,
                headline = headline
            )
        }
    }

    /**
     * Checks if repealing [ruleId] breaches an active League Sponsor's ruleDemand.
     * Cuts the sponsor's payout in half, forfeits any active bonus, and triggers
     * an angry 'Sponsor Breach Warning' event card.
     */
    fun handleRuleRepealed(
        currentState: LeagueSponsorsState,
        ruleId: String,
        seasonYear: Int,
        weekNumber: Int
    ): SponsorBreachResult {
        val matchingContract = currentState.allActiveContracts().firstOrNull {
            val demand = it.ruleDemand
            demand != null && demand.ruleId == ruleId && !demand.isBreached
        } ?: return SponsorBreachResult(
            triggered = false,
            updatedSponsorsState = currentState
        )

        return applyBreachPenaltyToContract(
            currentState = currentState,
            contract = matchingContract,
            newStatus = SponsorRuleDemandStatus.REPEALED,
            breachReason = "repealed while sponsorship was active",
            seasonYear = seasonYear,
            weekNumber = weekNumber
        )
    }

    private fun applyBreachPenaltyToContract(
        currentState: LeagueSponsorsState,
        contract: LeagueSponsorContract,
        newStatus: SponsorRuleDemandStatus,
        breachReason: String,
        seasonYear: Int,
        weekNumber: Int
    ): SponsorBreachResult {
        val demand = contract.ruleDemand ?: return SponsorBreachResult(
            triggered = false,
            updatedSponsorsState = currentState
        )
        if (demand.isBreached) {
            return SponsorBreachResult(
                triggered = false,
                updatedSponsorsState = currentState
            )
        }

        val originalPayout = contract.payout
        val halvedPayout = (originalPayout / 2L).coerceAtLeast(1L)
        val updatedDemand = demand.copy(status = newStatus)
        val updatedContract = contract.copy(
            payout = halvedPayout,
            ruleDemand = updatedDemand
        )
        val warning = SponsorBreachWarning(
            id = "spon_breach_${contract.slotType.key}_y${seasonYear}_w${weekNumber}_${Random.nextInt(100, 999)}",
            slotType = contract.slotType,
            sponsorName = contract.name,
            ruleId = demand.ruleId,
            ruleName = demand.ruleName,
            breachReason = breachReason,
            originalPayoutMillions = originalPayout,
            reducedPayoutMillions = halvedPayout,
            forfeitedBonusMillions = demand.bonusPayout,
            title = "Sponsor Breach Warning",
            message = "ANGRY PARTNER BREACH: ${contract.name} (${contract.slotType.displayName}) is furious that '${demand.ruleName}' was $breachReason! Annual base payout has been SLASHED IN HALF from \$${originalPayout}M/yr to \$${halvedPayout}M/yr and the +\$${demand.bonusPayout}M/yr bonus is canceled.",
            seasonYear = seasonYear,
            weekNumber = weekNumber
        )
        val remainingPending = currentState.pendingOffseasonRuleVotes.filterNot { it.ruleId == demand.ruleId }
        val updatedState = currentState
            .withSlot(contract.slotType, updatedContract)
            .copy(
                pendingOffseasonRuleVotes = remainingPending,
                activeBreachWarning = warning,
                breachWarningHistory = (listOf(warning) + currentState.breachWarningHistory).take(12),
                lastSummary = "SPONSOR BREACH WARNING: ${contract.name} slashed payout in half (\$${originalPayout}M -> \$${halvedPayout}M/yr) after '${demand.ruleName}' was $breachReason!"
            )

        return SponsorBreachResult(
            triggered = true,
            updatedSponsorsState = updatedState,
            warning = warning,
            headline = warning.message
        )
    }

    /**
     * Decrements yearsLeft on all 4 League Sponsor slots during offseason rollover.
     * Slots reaching 0 expire back to null so the commissioner can sign a new offer.
     */
    fun advanceOffseasonContracts(
        currentState: LeagueSponsorsState
    ): Pair<LeagueSponsorsState, List<String>> {
        val expiredMessages = mutableListOf<String>()
        fun tickContract(slotType: LeagueSponsorSlotType, c: LeagueSponsorContract?): LeagueSponsorContract? {
            if (c == null) return null
            val remaining = c.yearsLeft - 1
            return if (remaining <= 0) {
                expiredMessages.add("LEAGUE SPONSOR EXPIRED: ${c.name} (${slotType.displayName}, \$${c.totalEffectiveAnnualPayoutMillions}M/yr) contract concluded — slot now open for new bids.")
                null
            } else {
                val recoveredStability = if (currentState.activeLeagueScandal == null) {
                    (c.stabilityScore + 8).coerceIn(10, 100)
                } else c.stabilityScore
                c.copy(yearsLeft = remaining, stabilityScore = recoveredStability)
            }
        }

        val nextApparel = tickContract(LeagueSponsorSlotType.APPAREL, currentState.apparel)
        val nextBeverage = tickContract(LeagueSponsorSlotType.BEVERAGE, currentState.beverage)
        val nextTech = tickContract(LeagueSponsorSlotType.TECH, currentState.tech)
        val nextBetting = tickContract(LeagueSponsorSlotType.BETTING, currentState.betting)
        val activeRuleIds = listOfNotNull(nextApparel, nextBeverage, nextTech, nextBetting)
            .mapNotNull { it.ruleDemand?.ruleId }
            .toSet()

        val updated = currentState.copy(
            apparel = nextApparel,
            beverage = nextBeverage,
            tech = nextTech,
            betting = nextBetting,
            pendingOffseasonRuleVotes = currentState.pendingOffseasonRuleVotes.filter { it.ruleId in activeRuleIds },
            expiredSlotNotices = expiredMessages
        )
        return updated to expiredMessages
    }

    /**
     * Checks active sponsors with high scandalSensitivity whenever a player disciplinary
     * scandal or league scandal event occurs. Enqueues a 'Sponsor Ultimatum' threatening
     * pullout unless a maximum suspension & fine are levied.
     */
    fun evaluateScandalForUltimatum(
        currentState: LeagueSponsorsState,
        scandalSourceTitle: String,
        scandalSummary: String,
        seasonYear: Int,
        weekNumber: Int,
        relatedIncident: DisciplinaryIncident? = null,
        relatedFranchise: Franchise? = null,
        forceTriggerOnAnyIncident: Boolean = false
    ): LeagueSponsorsState {
        val isMajorIncident = relatedIncident == null ||
            forceTriggerOnAnyIncident ||
            relatedIncident.severity == InfractionSeverity.SEVERE ||
            relatedIncident.severity == InfractionSeverity.CRITICAL ||
            relatedIncident.category == DisciplinaryCategory.GAMBLING_INTEGRITY ||
            relatedIncident.category == DisciplinaryCategory.SUBSTANCE_POLICY ||
            relatedIncident.category == DisciplinaryCategory.CONDUCT_TAMPERING
        if (!isMajorIncident) return currentState

        val highRiskContracts = currentState.highSensitivityContracts()
        if (highRiskContracts.isEmpty()) return currentState

        val maxGames = relatedIncident?.category?.maxGames ?: 16
        val maxFine = relatedIncident?.category?.maxFineThousands ?: 500
        val existingUltimatumSlots = buildSet {
            currentState.activeUltimatum?.let { add(it.slotType) }
            currentState.pendingUltimatumQueue.forEach { add(it.slotType) }
        }

        val newUltimatums = highRiskContracts
            .filter { it.slotType !in existingUltimatumSlots }
            .sortedByDescending { it.scandalSensitivity.riskScore }
            .map { sponsor ->
                SponsorUltimatumEvent(
                    id = "spon_ult_${sponsor.slotType.key}_y${seasonYear}_w${weekNumber}_${Random.nextInt(100, 999)}",
                    slotType = sponsor.slotType,
                    sponsorName = sponsor.name,
                    sponsorPayoutMillions = sponsor.payout,
                    scandalSensitivity = sponsor.scandalSensitivity,
                    scandalSourceTitle = scandalSourceTitle,
                    scandalSummary = scandalSummary,
                    relatedIncidentId = relatedIncident?.id,
                    relatedPlayerName = relatedIncident?.playerName,
                    relatedFranchiseId = relatedIncident?.franchiseId ?: relatedFranchise?.id,
                    relatedFranchiseName = relatedIncident?.franchiseName ?: relatedFranchise?.teamFullName,
                    requiredMaxSuspensionGames = maxGames,
                    requiredMaxFineThousands = maxFine,
                    seasonYear = seasonYear,
                    weekNumber = weekNumber
                )
            }

        if (newUltimatums.isEmpty()) return currentState

        var markedState = currentState
        highRiskContracts.forEach { contract ->
            val currentContract = markedState.getSlot(contract.slotType) ?: return@forEach
            val nextStability = if (currentContract.stabilityScore > 75) {
                (currentContract.stabilityScore - 16).coerceIn(15, 100)
            } else currentContract.stabilityScore
            markedState = markedState.withSlot(
                contract.slotType,
                currentContract.copy(
                    stabilityScore = nextStability,
                    hasMoralityWarning = true,
                    moralityWarningReason = currentContract.moralityWarningReason ?: scandalSourceTitle
                )
            )
        }

        val nextActive = markedState.activeUltimatum ?: newUltimatums.first()
        val extraQueue = if (markedState.activeUltimatum == null) newUltimatums.drop(1) else newUltimatums
        return markedState.copy(
            activeUltimatum = nextActive,
            pendingUltimatumQueue = markedState.pendingUltimatumQueue + extraQueue,
            lastSummary = "SPONSOR ULTIMATUM: ${nextActive.sponsorName} threatens to void \$${nextActive.sponsorPayoutMillions}M/yr ${nextActive.slotType.displayName} deal!"
        )
    }

    /**
     * Resolves an active Sponsor Ultimatum:
     * - enforceMaxPenalty = true: enforces max suspension & fine (via DisciplinaryEngine if linked,
     *   or league-wide emergency max crackdown) and keeps the sponsor active.
     * - enforceMaxPenalty = false: sponsor pulls out immediately, clearing the slot back to null.
     */
    fun resolveSponsorUltimatum(
        currentSponsorsState: LeagueSponsorsState,
        disciplinaryState: DisciplinaryDeskState,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement,
        ruleBook: LeagueRuleBook,
        enforceMaxPenalty: Boolean
    ): SponsorUltimatumResolutionOutcome {
        val ultimatum = currentSponsorsState.activeUltimatum
            ?: return SponsorUltimatumResolutionOutcome(
                success = false,
                enforcedMaxPenalty = enforceMaxPenalty,
                message = "No active Sponsor Ultimatum pending.",
                updatedSponsorsState = currentSponsorsState,
                updatedDisciplinaryState = disciplinaryState,
                updatedFranchises = franchises,
                updatedUnion = unionAgreement,
                headline = "No Sponsor Ultimatum active."
            )

        val nextQueued = currentSponsorsState.pendingUltimatumQueue.firstOrNull()
        val remainingQueue = if (currentSponsorsState.pendingUltimatumQueue.isNotEmpty()) {
            currentSponsorsState.pendingUltimatumQueue.drop(1)
        } else emptyList()

        if (!enforceMaxPenalty) {
            val clearedState = currentSponsorsState
                .withSlot(ultimatum.slotType, null)
                .copy(
                    activeUltimatum = nextQueued,
                    pendingUltimatumQueue = remainingQueue,
                    lastSummary = "${ultimatum.sponsorName} pulled out of ${ultimatum.slotType.displayName} (-\$$${ultimatum.sponsorPayoutMillions}M/yr) after declining maximum penalty."
                )
            val updatedUnion = unionAgreement.copy(
                unionSatisfaction = (unionAgreement.unionSatisfaction + 3).coerceIn(10, 100),
                strikeRisk = (unionAgreement.strikeRisk - 3).coerceIn(0, 100)
            )
            val headline = "SPONSOR PULLOUT: ${ultimatum.sponsorName} terminated their ${ultimatum.slotType.displayName} deal (-\$$${ultimatum.sponsorPayoutMillions}M/yr) after Commissioner declined maximum sanctions."
            return SponsorUltimatumResolutionOutcome(
                success = true,
                enforcedMaxPenalty = false,
                message = "${ultimatum.sponsorName} pulled out of the ${ultimatum.slotType.displayName} slot.",
                updatedSponsorsState = clearedState,
                updatedDisciplinaryState = disciplinaryState,
                updatedFranchises = franchises,
                updatedUnion = updatedUnion,
                cashTreasuryGainMillions = 0L,
                integrityDelta = -2,
                ownerApprovalDelta = -5,
                valuationDeltaMillions = -(ultimatum.sponsorPayoutMillions / 2L).coerceAtLeast(3L),
                headline = headline
            )
        }

        // Commissioner enforces maximum suspension & fine to appease sponsor
        val linkedIncident = ultimatum.relatedIncidentId?.let { id ->
            disciplinaryState.incidents.firstOrNull { it.id == id && it.status == DisciplinaryStatus.PENDING_HEARING }
        }

        if (linkedIncident != null) {
            val adj = DisciplinaryEngine.adjudicateIncident(
                state = disciplinaryState,
                franchises = franchises,
                unionAgreement = unionAgreement,
                ruleBook = ruleBook,
                incidentId = linkedIncident.id,
                suspensionGames = ultimatum.requiredMaxSuspensionGames,
                fineThousands = ultimatum.requiredMaxFineThousands,
                setNewPrecedent = false,
                commissionerNote = "Maximum sanction enforced under ${ultimatum.sponsorName} Sponsor Ultimatum."
            )
            val updatedSponsors = currentSponsorsState.copy(
                activeUltimatum = nextQueued,
                pendingUltimatumQueue = remainingQueue,
                lastSummary = "Sponsor Ultimatum satisfied: ${ultimatum.sponsorName} stays active (+\$${ultimatum.sponsorPayoutMillions}M/yr) after max ${ultimatum.requiredMaxSuspensionGames}G / \$${ultimatum.requiredMaxFineThousands}K sanction."
            )
            val headline = "SPONSOR ULTIMATUM APPEASED: Levied maximum ${ultimatum.requiredMaxSuspensionGames}G ban & \$${ultimatum.requiredMaxFineThousands}K fine on ${linkedIncident.playerName} — ${ultimatum.sponsorName} (\$${ultimatum.sponsorPayoutMillions}M/yr) retained!"
            return SponsorUltimatumResolutionOutcome(
                success = true,
                enforcedMaxPenalty = true,
                message = "${ultimatum.sponsorName} partnership preserved after maximum suspension/fine.",
                updatedSponsorsState = updatedSponsors,
                updatedDisciplinaryState = adj.updatedState,
                updatedFranchises = adj.updatedFranchises,
                updatedUnion = adj.updatedUnion,
                cashTreasuryGainMillions = adj.cashTreasuryGainMillions,
                integrityDelta = adj.integrityDelta + 2,
                ownerApprovalDelta = 4,
                valuationDeltaMillions = 3L,
                headline = headline
            )
        } else {
            // League-scandal fallback: suspend marquee player on implicated franchise (or leading club) + max fine
            val targetTeam = ultimatum.relatedFranchiseId?.let { tid -> franchises.firstOrNull { it.id == tid } }
                ?: franchises.firstOrNull()
            val updatedTeams = if (targetTeam != null) {
                franchises.map { team ->
                    if (team.id != targetTeam.id) team
                    else {
                        val candidatePlayer = team.roster.firstOrNull { !it.isInjured && it.suspendedGamesRemaining == 0 }
                            ?: team.roster.firstOrNull()
                        val updatedRoster = team.roster.map { p ->
                            if (candidatePlayer != null && p.id == candidatePlayer.id) {
                                p.copy(suspendedGamesRemaining = maxOf(p.suspendedGamesRemaining, ultimatum.requiredMaxSuspensionGames))
                            } else p
                        }
                        team.copy(
                            roster = updatedRoster,
                            fanMorale = (team.fanMorale - 3).coerceIn(25, 99)
                        )
                    }
                }
            } else franchises

            val treasuryFineMillions = (ultimatum.requiredMaxFineThousands / 1000.0).roundToLong().coerceAtLeast(1L)
            val updatedUnion = unionAgreement.copy(
                unionSatisfaction = (unionAgreement.unionSatisfaction - 7).coerceIn(10, 100),
                strikeRisk = (unionAgreement.strikeRisk + 6).coerceIn(0, 100)
            )
            val updatedSponsors = currentSponsorsState.copy(
                activeUltimatum = nextQueued,
                pendingUltimatumQueue = remainingQueue,
                lastSummary = "Emergency maximum sanction (${ultimatum.requiredMaxSuspensionGames}G / \$${ultimatum.requiredMaxFineThousands}K) satisfied ${ultimatum.sponsorName}."
            )
            val headline = "SPONSOR ULTIMATUM ENFORCED: Maximum ${ultimatum.requiredMaxSuspensionGames}G suspension & \$${ultimatum.requiredMaxFineThousands}K fine levied — ${ultimatum.sponsorName} remains locked in (\$${ultimatum.sponsorPayoutMillions}M/yr)."
            return SponsorUltimatumResolutionOutcome(
                success = true,
                enforcedMaxPenalty = true,
                message = "Enforced maximum ${ultimatum.requiredMaxSuspensionGames}G suspension & \$${ultimatum.requiredMaxFineThousands}K fine; ${ultimatum.sponsorName} retained.",
                updatedSponsorsState = updatedSponsors,
                updatedDisciplinaryState = disciplinaryState.copy(
                    totalFinesCollectedThousands = disciplinaryState.totalFinesCollectedThousands + ultimatum.requiredMaxFineThousands,
                    totalSuspensionsIssuedGames = disciplinaryState.totalSuspensionsIssuedGames + ultimatum.requiredMaxSuspensionGames
                ),
                updatedFranchises = updatedTeams,
                updatedUnion = updatedUnion,
                cashTreasuryGainMillions = treasuryFineMillions,
                integrityDelta = 4,
                ownerApprovalDelta = 4,
                valuationDeltaMillions = 3L,
                headline = headline
            )
        }
    }

    /**
     * Checks if an existing pending Sponsor Ultimatum was satisfied when a commissioner
     * manually adjudicated its linked DisciplinaryIncident, or if a lenient ruling triggers a pullout threat!
     */
    fun onManualDisciplinaryRuling(
        currentSponsorsState: LeagueSponsorsState,
        incident: DisciplinaryIncident,
        proposedGames: Int,
        proposedFineThousands: Int,
        seasonYear: Int,
        weekNumber: Int
    ): LeagueSponsorsState {
        val highRisk = currentSponsorsState.highSensitivityContracts()
        if (highRisk.isEmpty()) return currentSponsorsState

        val maxGames = incident.category.maxGames
        val maxFine = incident.category.maxFineThousands
        val metMax = proposedGames >= maxGames && proposedFineThousands >= maxFine

        // If an active ultimatum matches this incident and commissioner just enforced max penalty, satisfy it!
        val activeUlt = currentSponsorsState.activeUltimatum
        if (activeUlt != null && activeUlt.relatedIncidentId == incident.id && metMax) {
            val nextQ = currentSponsorsState.pendingUltimatumQueue.firstOrNull()
            val restQ = currentSponsorsState.pendingUltimatumQueue.drop(1)
            return currentSponsorsState.copy(
                activeUltimatum = nextQ,
                pendingUltimatumQueue = restQ,
                lastSummary = "${activeUlt.sponsorName} satisfied after manual maximum ruling (${proposedGames}G / \$${proposedFineThousands}K)."
            )
        }

        // Otherwise if the ruling fell below maximum on a scandal, trigger an ultimatum!
        if (!metMax) {
            return evaluateScandalForUltimatum(
                currentState = currentSponsorsState,
                scandalSourceTitle = "${incident.playerName} (${incident.category.shortLabel}) Scandal",
                scandalSummary = "Commissioner issued ${proposedGames}G / \$${proposedFineThousands}K for ${incident.incidentTitle}, short of maximum ${maxGames}G / \$${maxFine}K.",
                seasonYear = seasonYear,
                weekNumber = weekNumber,
                relatedIncident = incident,
                forceTriggerOnAnyIncident = true
            )
        }
        return currentSponsorsState
    }
}
