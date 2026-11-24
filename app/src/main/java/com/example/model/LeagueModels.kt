package com.example.model

enum class SportType(val displayName: String, val baseScoreScale: Int, val description: String) {
    BASKETBALL("Pro Basketball", 100, "High tempo, star player driven, massive digital hype"),
    FOOTBALL("Gridiron Football", 24, "Massive stadium gate receipts and record TV ratings"),
    SOCCER("Global Football", 2, "Ultimate international market reach and global fanbase"),
    HOCKEY("Ice Hockey", 3, "Intense regional loyalty, high arena merchandise revenue");

    val starterPositions: List<String>
        get() = when (this) {
            BASKETBALL -> listOf("PG", "SG", "SF", "PF", "C")
            FOOTBALL -> listOf("QB", "RB", "WR", "EDGE", "CB")
            SOCCER -> listOf("ST", "CAM", "WNG", "CB", "GK")
            HOCKEY -> listOf("C", "LW", "RW", "D", "G")
        }

    val benchDepthPositions: List<String>
        get() = when (this) {
            BASKETBALL -> listOf("PG", "SF", "C")
            FOOTBALL -> listOf("QB", "WR", "EDGE")
            SOCCER -> listOf("ST", "CAM", "CB")
            HOCKEY -> listOf("C", "RW", "D")
        }

    val archetypes: List<String>
        get() = when (this) {
            BASKETBALL -> listOf(
                "3-and-D Wing",
                "Floor General Virtuoso",
                "Rim-Running Anchor",
                "Microwave Sixth Man",
                "Two-Way Slasher",
                "Sharpshooting Specialist",
                "Lockdown Perimeter Defender"
            )
            FOOTBALL -> listOf(
                "Dual-Threat Field General",
                "Power Feature Back",
                "Deep-Threat Route Technician",
                "Relentless Edge Rusher",
                "Lockdown Island Corner",
                "Pocket Commander",
                "Zone Ball-Hawk"
            )
            SOCCER -> listOf(
                "Target Striker",
                "Midfield Maestro",
                "Inverted Wing Wizard",
                "Ball-Playing Center Back",
                "Sweeper Keeper",
                "Box-to-Box Engine",
                "Clinical Poacher"
            )
            HOCKEY -> listOf(
                "Pure Sniper",
                "Two-Way Center",
                "Offensive Defenseman",
                "Shutdown Blue-Liner",
                "Butterfly Goaltender",
                "Power Forward",
                "Playmaking Wing"
            )
        }

    val minorLeagueName: String
        get() = when (this) {
            BASKETBALL -> "Apex G-Circuit"
            FOOTBALL -> "Apex Spring Gridiron"
            SOCCER -> "Apex Reserve Premier"
            HOCKEY -> "Apex AHL Affiliate Circuit"
        }

    val primaryPositionSummary: String
        get() = starterPositions.joinToString(" • ")
}

enum class CommissionerBackground(val title: String, val perkDescription: String) {
    MARKETING_MAVEN("Media & Marketing Titan", "+25% Fan engagement growth & higher broadcast bidding wars"),
    WALL_STREET_FINANCIER("Financial Strategist", "+15% Investment returns & higher franchise expansion fees"),
    FORMER_LEGEND("Former MVP Athlete", "+20% Union satisfaction & better player recruitment compliance"),
    ANTITRUST_ATTORNEY("Constitutional Counsel", "+30% League integrity & reduced regulatory fines")
}

enum class MarketTier(val label: String, val mediaMultiplier: Double) {
    TIER_1_MEGA("Tier 1 Mega Market", 1.8),
    HIGH_GROWTH_SUNBELT("High-Growth Metro", 1.4),
    TECH_INNOVATION_HUB("Tech & Innovation Hub", 1.3),
    REGIONAL_HEARTLAND("Regional Heartland", 1.0),
    INTERNATIONAL_GATEWAY("International Gateway", 1.9)
}

enum class OwnershipType(
    val title: String,
    val description: String,
    val sponsorBonusPct: Int,
    val mediaBonusPct: Int
) {
    TECH_BILLIONAIRE("Tech Innovator", "Heavy tech analytics, invests heavily in star recruitment", 20, 25),
    PRIVATE_EQUITY("Private Equity Fund", "Demands high profit margins and strict fiscal discipline", 30, 15),
    COMMUNITY_TRUST("Fan Community Trust", "High local loyalty, resilient ticket sales during slumps", 10, 10),
    MEDIA_MOGUL("Media Conglomerate", "Massive broadcast hype, higher TV ratings, impatient with losses", 15, 35),
    SOVEREIGN_SYNDICATE("Sovereign Wealth Syndicate", "Massive international reach, unlocks global exhibition matches", 35, 30)
}

enum class OwnerArchetype(
    val title: String,
    val shortBadge: String,
    val badgeColorHex: Long,
    val manifesto: String,
    val preferredCapStrictness: SalaryCapStrictness,
    val idealRevenueSharePct: Int,
    val parityBias: Int,              // -2 (open market) .. +2 (strict parity/caps)
    val paceShowmanshipBias: Int,     // -2 (classic grind) .. +2 (fast tempo/broadcast drama)
    val playerWelfareBias: Int,       // -2 (cost hawk) .. +2 (players-first benefits)
    val integrityTraditionBias: Int,  // -2 (disruptor) .. +2 (strict rules/integrity)
    val globalGrowthBias: Int         // -2 (domestic core) .. +2 (international/expanded)
) {
    ANALYTICS_VISIONARY(
        title = "Analytics Visionary",
        shortBadge = "Analytics",
        badgeColorHex = 0xFF06B6D4,
        manifesto = "Data-driven pace, precision replay, sports-science recovery, and flexible soft-cap allocation.",
        preferredCapStrictness = SalaryCapStrictness.MODERATE_SOFT_CAP,
        idealRevenueSharePct = 50,
        parityBias = 1,
        paceShowmanshipBias = 1,
        playerWelfareBias = 1,
        integrityTraditionBias = 1,
        globalGrowthBias = 1
    ),
    FISCAL_HAWK(
        title = "Fiscal Hawk",
        shortBadge = "Fiscal Hawk",
        badgeColorHex = 0xFFF59E0B,
        manifesto = "Strict hard caps, rookie wage containment, disciplined revenue splits, and predictable EBITDA.",
        preferredCapStrictness = SalaryCapStrictness.STRICT_HARD_CAP,
        idealRevenueSharePct = 47,
        parityBias = 2,
        paceShowmanshipBias = 0,
        playerWelfareBias = -2,
        integrityTraditionBias = 1,
        globalGrowthBias = -1
    ),
    PLAYERS_FIRST_PATRON(
        title = "Players-First Patron",
        shortBadge = "Players-First",
        badgeColorHex = 0xFF10B981,
        manifesto = "Champions higher player revenue shares, elite medical/travel perks, and superstar player empowerment.",
        preferredCapStrictness = SalaryCapStrictness.MODERATE_SOFT_CAP,
        idealRevenueSharePct = 53,
        parityBias = -1,
        paceShowmanshipBias = 1,
        playerWelfareBias = 2,
        integrityTraditionBias = 0,
        globalGrowthBias = 1
    ),
    BROADCAST_SHOWMAN(
        title = "Broadcast Showman",
        shortBadge = "Showman",
        badgeColorHex = 0xFF8B5CF6,
        manifesto = "Prioritizes prime-time scoring explosions, play-in drama, Elam endings, and marquee superteams.",
        preferredCapStrictness = SalaryCapStrictness.UNRESTRICTED_MARKET,
        idealRevenueSharePct = 50,
        parityBias = -2,
        paceShowmanshipBias = 2,
        playerWelfareBias = 0,
        integrityTraditionBias = -1,
        globalGrowthBias = 1
    ),
    TRADITIONALIST_PURIST(
        title = "Traditionalist Purist",
        shortBadge = "Purist",
        badgeColorHex = 0xFF3B82F6,
        manifesto = "Defends 48-minute regulation, classic overtime, zero-tolerance conduct codes, and merit-based playoffs.",
        preferredCapStrictness = SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX,
        idealRevenueSharePct = 49,
        parityBias = 1,
        paceShowmanshipBias = -2,
        playerWelfareBias = 0,
        integrityTraditionBias = 2,
        globalGrowthBias = -1
    ),
    GLOBAL_EXPANSIONIST(
        title = "Global Expansionist",
        shortBadge = "Globalist",
        badgeColorHex = 0xFF14B8A6,
        manifesto = "Pushes continental realignment, deeper active rosters, international gateways, and progressive luxury taxes.",
        preferredCapStrictness = SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX,
        idealRevenueSharePct = 51,
        parityBias = 1,
        paceShowmanshipBias = 1,
        playerWelfareBias = 1,
        integrityTraditionBias = 0,
        globalGrowthBias = 2
    ),
    COMMUNITY_STEWARD(
        title = "Community Steward",
        shortBadge = "Steward",
        badgeColorHex = 0xFFEC4899,
        manifesto = "Protects small-market parity, gate revenue sharing, anti-tanking lotteries, and accessible fan loyalty.",
        preferredCapStrictness = SalaryCapStrictness.STRICT_HARD_CAP,
        idealRevenueSharePct = 49,
        parityBias = 2,
        paceShowmanshipBias = 0,
        playerWelfareBias = 1,
        integrityTraditionBias = 2,
        globalGrowthBias = 0
    )
}

data class FranchiseOwner(
    val id: String,
    val fullName: String,
    val roleTitle: String = "Principal Governor",
    val backgroundBio: String,
    val archetype: OwnerArchetype,
    val seatWeight: Int = 1
)

enum class VoteChoice(val label: String) {
    APPROVE("Yea"),
    OPPOSE("Nay"),
    ABSTAIN("Abstain")
}

enum class GovernanceProposalCategory(val label: String) {
    CBA_NEGOTIATION("CBA Negotiation"),
    RULE_CHANGE("Rule Change"),
    SALARY_CAP_POLICY("Cap & Economics"),
    SCHEDULE_CALENDAR("Schedule & Calendar")
}

data class OwnerBallot(
    val franchiseId: String,
    val teamName: String,
    val ownershipGroupName: String,
    val owner: FranchiseOwner,
    val choice: VoteChoice,
    val alignmentScore: Int,
    val rationale: String
)

data class BoardVoteResult(
    val id: String,
    val proposalTitle: String,
    val proposalSummary: String,
    val category: GovernanceProposalCategory,
    val ballots: List<OwnerBallot>,
    val yesVotes: Int,
    val noVotes: Int,
    val abstainVotes: Int,
    val totalVotes: Int,
    val passed: Boolean,
    val enactedViaCommissionerOverride: Boolean = false,
    val year: Int,
    val week: Int
) {
    val marginSummary: String
        get() = "$yesVotes-$noVotes${if (abstainVotes > 0) "-$abstainVotes" else ""}"
}

data class OwnershipGroup(
    val id: String,
    val name: String,
    val type: OwnershipType,
    val netWorthBillions: Double,
    val ownerApproval: Int = 80, // 0-100
    val primaryOwner: FranchiseOwner = OwnerGovernanceEngine.defaultOwnerForGroup(id, name, type)
)

const val MAX_OPEN_EXPANSION_CANDIDATES = 10

enum class MarketViabilityGrade(
    val label: String,
    val shortBadge: String,
    val badgeColorHex: Long,
    val riskLevel: String,
    val description: String
) {
    PRIME_METRO("Prime Metro (A+)", "A+", 0xFF10B981, "Low Risk", "Turnkey world-class facilities, massive revenue ceiling, rabid fan appetite"),
    SOLID_CONTENDER("Solid Contender (A)", "A", 0xFF3B82F6, "Low-Moderate Risk", "Robust regional economic fundamentals and established corporate backing"),
    HIGH_GROWTH("High Growth (B+)", "B+", 0xFF8B5CF6, "Moderate Risk", "Booming demographic inflow with rapidly emerging sports culture"),
    EMERGING_MARKET("Emerging Market (B)", "B", 0xFFF59E0B, "Elevated Risk", "Moderate media market with passionate grassroots supporters"),
    SPECULATIVE_WILDCARD("Speculative Wildcard (C)", "C", 0xFFEF4444, "High Risk / High Upside", "Unproven territory with major upside if local interest is unlocked")
}

data class MarketCity(
    val id: String,
    val name: String,
    val country: String,
    val region: String,
    val populationMillions: Double,
    val tier: MarketTier,
    val annualGrowthRatePct: Double,
    val sportsAppetite: Int, // 1-100 (Fan Interest / Sports Passion)
    val medianIncomeK: Int, // in thousands USD
    val isInternational: Boolean,
    val hasFranchise: Boolean = false,
    val expansionFeeMillions: Long = 75L,
    val digitalStreamingIndex: Int = 86,
    val localSponsorDepthMillions: Long = 38L,
    val arenaName: String = "Metropolitan Arena",
    val arenaCapacity: Int = 21000,
    val competingMajorTeamsCount: Int = 2,
    val arenaReadinessStatus: String = "Turnkey Modern Arena",
    val investorGroupProfile: String = "Regional Investment Consortium",
    val hasBuiltExpansionArena: Boolean = false,
    val arenaBuildCostSpentMillions: Long = 0L,
    val expansionArenaTierName: String = ""
) {
    val overallViabilityScore: Int
        get() = ((sportsAppetite * 0.45) + (populationMillions.coerceAtMost(10.0) * 3.5) + (annualGrowthRatePct * 4.0)).toInt().coerceIn(40, 99)

    val viabilityGrade: MarketViabilityGrade
        get() = when {
            overallViabilityScore >= 80 -> MarketViabilityGrade.PRIME_METRO
            overallViabilityScore >= 70 -> MarketViabilityGrade.SOLID_CONTENDER
            overallViabilityScore >= 60 -> MarketViabilityGrade.HIGH_GROWTH
            overallViabilityScore >= 50 -> MarketViabilityGrade.EMERGING_MARKET
            else -> MarketViabilityGrade.SPECULATIVE_WILDCARD
        }

    val marketQuadrantLabel: String
        get() = when {
            isInternational -> "Global Gateway Metros"
            annualGrowthRatePct >= 3.0 -> "High-Growth Sunbelt Target"
            populationMillions >= 3.0 -> "Tier 1 Mega Market"
            else -> "Passionate Heartland Hub"
        }
}

enum class Conference(
    val displayName: String,
    val shortCode: String,
    val badgeColorHex: Long,
    val description: String,
    val regionLabel: String
) {
    EASTERN(
        displayName = "Eastern Conference",
        shortCode = "EAST",
        badgeColorHex = 0xFF3B82F6, // Royal Blue
        description = "Atlantic seaboard, Midwest metropolises, and North American east corridor",
        regionLabel = "North America"
    ),
    WESTERN(
        displayName = "Western Conference",
        shortCode = "WEST",
        badgeColorHex = 0xFFF59E0B, // Electric Amber
        description = "Pacific coast, Sunbelt growth hubs, and Mountain circuits",
        regionLabel = "North America"
    ),
    EUROPEAN(
        displayName = "European Conference",
        shortCode = "EUR",
        badgeColorHex = 0xFF8B5CF6, // Royal Violet
        description = "Western, Central, Northern & Mediterranean European capitals across historic multi-sport venues",
        regionLabel = "Europe"
    ),
    SOUTH_AMERICAN(
        displayName = "South American Conference",
        shortCode = "SA",
        badgeColorHex = 0xFF10B981, // Emerald Green
        description = "Atlantic Brazil, Southern Cone, and Andean metropolitan basketball powerhouses",
        regionLabel = "South America"
    ),
    ASIA_OCEANIA(
        displayName = "Asia-Oceania Conference",
        shortCode = "APAC",
        badgeColorHex = 0xFF06B6D4, // Cyan Electric
        description = "East Asian megacities, Indo-Pacific tech hubs, and Australian sporting capitals",
        regionLabel = "Asia & Oceania"
    );

    val isContinentalInternational: Boolean
        get() = this == EUROPEAN || this == SOUTH_AMERICAN || this == ASIA_OCEANIA
}

enum class Division(
    val displayName: String,
    val shortCode: String,
    val conference: Conference,
    val geographicRegion: String
) {
    // Eastern Conference Divisions (North America)
    ATLANTIC(
        displayName = "Atlantic Division",
        shortCode = "ATL",
        conference = Conference.EASTERN,
        geographicRegion = "Northeast & Canadian Seaboard"
    ),
    CENTRAL(
        displayName = "Central Division",
        shortCode = "CEN",
        conference = Conference.EASTERN,
        geographicRegion = "Great Lakes & Midwest Industrial Hubs"
    ),
    SOUTHEAST(
        displayName = "Southeast Division",
        shortCode = "SE",
        conference = Conference.EASTERN,
        geographicRegion = "Southern Seaboard & Coastal Markets"
    ),

    // Western Conference Divisions (North America)
    PACIFIC(
        displayName = "Pacific Division",
        shortCode = "PAC",
        conference = Conference.WESTERN,
        geographicRegion = "West Coast & Pacific Islands"
    ),
    SOUTHWEST(
        displayName = "Southwest Division",
        shortCode = "SW",
        conference = Conference.WESTERN,
        geographicRegion = "Texas, Mexico & Sunbelt Growth Hubs"
    ),
    NORTHWEST(
        displayName = "Northwest Division",
        shortCode = "NW",
        conference = Conference.WESTERN,
        geographicRegion = "Pacific Northwest & Rocky Mountains"
    ),

    // European Conference Divisions
    WEST_EUROPE(
        displayName = "West Europe Division",
        shortCode = "WEU",
        conference = Conference.EUROPEAN,
        geographicRegion = "United Kingdom, France, Benelux & Ireland"
    ),
    CENTRAL_NORTH_EUROPE(
        displayName = "Central-North Europe Division",
        shortCode = "CNE",
        conference = Conference.EUROPEAN,
        geographicRegion = "Germany, Czechia, Nordics & Alpine Hubs"
    ),
    MEDITERRANEAN(
        displayName = "Mediterranean Division",
        shortCode = "MED",
        conference = Conference.EUROPEAN,
        geographicRegion = "Spain, Italy & Southern Europe Capitals"
    ),

    // South American Conference Divisions
    ATLANTIC_BRAZIL(
        displayName = "Atlantic Brazil Division",
        shortCode = "BRZ",
        conference = Conference.SOUTH_AMERICAN,
        geographicRegion = "São Paulo, Rio de Janeiro & Brazilian Coast"
    ),
    SOUTHERN_CONE(
        displayName = "Southern Cone Division",
        shortCode = "CON",
        conference = Conference.SOUTH_AMERICAN,
        geographicRegion = "Argentina, Chile & Rio de la Plata"
    ),
    ANDEAN_NORTH(
        displayName = "Andean Basin Division",
        shortCode = "AND",
        conference = Conference.SOUTH_AMERICAN,
        geographicRegion = "Colombia, Peru & Northern South America"
    ),

    // Asia-Oceania Conference Divisions
    EAST_ASIA(
        displayName = "East Asia Division",
        shortCode = "EAS",
        conference = Conference.ASIA_OCEANIA,
        geographicRegion = "Japan, South Korea & East Asian Megacities"
    ),
    OCEANIA(
        displayName = "Oceania Division",
        shortCode = "OCE",
        conference = Conference.ASIA_OCEANIA,
        geographicRegion = "Australia, New Zealand & Tasman Corridor"
    ),
    INDO_PACIFIC(
        displayName = "Indo-Pacific Division",
        shortCode = "IND",
        conference = Conference.ASIA_OCEANIA,
        geographicRegion = "Singapore, Southeast Asia & Gulf Gateways"
    )
}

enum class RealignmentPreset(val title: String, val description: String) {
    GEOGRAPHIC_OPTIMAL("Geographic Alignment", "Aligns North America & enabled continental conferences geographically"),
    COASTAL_VS_HEARTLAND("Coastal vs. Heartland", "Splits coastal mega-markets and heartland hubs across East & West"),
    BALANCED_PARITY("Competitive Parity Split", "Distributes top-tier rosters equally across active conferences")
}

enum class InjurySeverity(
    val label: String,
    val badgeColorHex: Long,
    val canPlayThrough: Boolean
) {
    DAY_TO_DAY("Day-to-Day (Playable)", 0xFFF59E0B, true),
    MODERATE("Moderate (Hobbled)", 0xFFFB923C, true),
    OUT_SHORT_TERM("Out (Sideline Rehab)", 0xFFEF4444, false),
    SEVERE("Severe / Long-Term IR", 0xFFDC2626, false)
}

enum class InjuryType(
    val displayName: String,
    val bodyPart: String,
    val severity: InjurySeverity,
    val minWeeks: Int,
    val maxWeeks: Int,
    val ovrPenalty: Int,
    val statMultiplier: Double,
    val clinicalNote: String
) {
    ANKLE_SPRAIN(
        displayName = "Lateral Ankle Sprain",
        bodyPart = "Ankle",
        severity = InjurySeverity.DAY_TO_DAY,
        minWeeks = 1,
        maxWeeks = 2,
        ovrPenalty = -4,
        statMultiplier = 0.74,
        clinicalNote = "Lateral burst and cutting stability reduced; playable with tape."
    ),
    WRIST_CONTUSION(
        displayName = "Shooting Wrist Contusion",
        bodyPart = "Wrist",
        severity = InjurySeverity.DAY_TO_DAY,
        minWeeks = 1,
        maxWeeks = 2,
        ovrPenalty = -3,
        statMultiplier = 0.78,
        clinicalNote = "Touch and release efficiency dipped; clears cleanly with rest."
    ),
    HIP_POINTER(
        displayName = "Deep Hip Contusion",
        bodyPart = "Hip",
        severity = InjurySeverity.DAY_TO_DAY,
        minWeeks = 1,
        maxWeeks = 2,
        ovrPenalty = -4,
        statMultiplier = 0.75,
        clinicalNote = "Explosive verticality limited in contact traffic."
    ),
    HAMSTRING_STRAIN(
        displayName = "Grade I Hamstring Strain",
        bodyPart = "Hamstring",
        severity = InjurySeverity.MODERATE,
        minWeeks = 2,
        maxWeeks = 3,
        ovrPenalty = -8,
        statMultiplier = 0.58,
        clinicalNote = "Sprint mechanics compromised; elevated re-injury risk if kept starting."
    ),
    SHOULDER_IMPINGEMENT(
        displayName = "Rotator Cuff Strain",
        bodyPart = "Shoulder",
        severity = InjurySeverity.MODERATE,
        minWeeks = 2,
        maxWeeks = 4,
        ovrPenalty = -7,
        statMultiplier = 0.62,
        clinicalNote = "Overhead contest reach and long-range power hindered."
    ),
    CONCUSSION_PROTOCOL(
        displayName = "League Concussion Protocol",
        bodyPart = "Head",
        severity = InjurySeverity.OUT_SHORT_TERM,
        minWeeks = 1,
        maxWeeks = 3,
        ovrPenalty = -20,
        statMultiplier = 0.0,
        clinicalNote = "Mandatory league safety clearance required before returning to lineup."
    ),
    RIB_FRACTURE(
        displayName = "Hairline Rib Contusion",
        bodyPart = "Torso",
        severity = InjurySeverity.OUT_SHORT_TERM,
        minWeeks = 3,
        maxWeeks = 5,
        ovrPenalty = -22,
        statMultiplier = 0.0,
        clinicalNote = "No-contact medical hold; promote bench depth immediately."
    ),
    MCL_SPRAIN(
        displayName = "Grade II MCL Sprain",
        bodyPart = "Knee",
        severity = InjurySeverity.OUT_SHORT_TERM,
        minWeeks = 3,
        maxWeeks = 6,
        ovrPenalty = -24,
        statMultiplier = 0.0,
        clinicalNote = "Knee immobilization required; team loses starter production until cleared."
    ),
    ACHILLES_TEAR(
        displayName = "Torn Knee / Achilles Ligament",
        bodyPart = "Lower Leg",
        severity = InjurySeverity.SEVERE,
        minWeeks = 6,
        maxWeeks = 9,
        ovrPenalty = -28,
        statMultiplier = 0.0,
        clinicalNote = "Major surgical rehab; assign to Injured Reserve and elevate backup."
    )
}

data class PlayerInjury(
    val id: String,
    val type: InjuryType,
    val weeksRemaining: Int,
    val initialWeeks: Int,
    val sustainedWeek: Int,
    val sustainedOpponent: String,
    val gameContext: String,
    val isOnInjuredReserve: Boolean = false
) {
    val isOut: Boolean
        get() = weeksRemaining > 0 && (!type.severity.canPlayThrough || isOnInjuredReserve)

    val effectiveOvrPenalty: Int
        get() = if (weeksRemaining <= 0) 0 else if (isOut) -28 else type.ovrPenalty

    val statMultiplier: Double
        get() = if (weeksRemaining <= 0) 1.0 else if (isOut) 0.0 else type.statMultiplier
}

data class InjuryEventLog(
    val id: String,
    val week: Int,
    val seasonYear: Int,
    val playerId: String,
    val playerName: String,
    val position: String,
    val franchiseId: String,
    val franchiseName: String,
    val injuryType: InjuryType,
    val weeksOut: Int,
    val opponentName: String,
    val context: String,
    val isPlayoffs: Boolean = false
)

data class RosterPlayer(
    val id: String,
    val name: String,
    val franchiseId: String,
    val position: String,
    val overallRating: Int, // 65-96
    val age: Int, // 19-38
    val salaryMillions: Long, // 5L to 45L
    val contractYearsRemaining: Int, // 1 to 5
    val archetype: String = "Star Contender",
    val depthOrder: Int = 0, // 0..4 starters, 5+ bench
    val isStarter: Boolean = true,
    val activeInjury: PlayerInjury? = null,
    val isAssignedToMinorLeague: Boolean = false,
    val minorLeagueOvrProgress: Int = 0,
    val suspendedGamesRemaining: Int = 0
) {
    val isInjured: Boolean
        get() = activeInjury != null && activeInjury.weeksRemaining > 0

    val isMedicallyOut: Boolean
        get() = activeInjury != null && activeInjury.weeksRemaining > 0 && activeInjury.isOut

    val isSuspended: Boolean
        get() = suspendedGamesRemaining > 0

    val isUnavailableForGame: Boolean
        get() = isMedicallyOut || isSuspended

    val effectiveOverallRating: Int
        get() = when {
            isSuspended -> 0
            !isInjured -> overallRating
            isMedicallyOut -> 0
            else -> (overallRating + activeInjury!!.type.ovrPenalty).coerceIn(55, 99)
        }

    val statProductionMultiplier: Double
        get() = if (isSuspended) 0.0 else (activeInjury?.statMultiplier ?: 1.0)

    val healthStatusLabel: String
        get() = when {
            isSuspended && isMedicallyOut -> "SUSP (${suspendedGamesRemaining}G) + OUT"
            isSuspended -> "SUSP (${suspendedGamesRemaining}G)"
            !isInjured -> "Healthy"
            activeInjury!!.isOnInjuredReserve -> "IR (${activeInjury.weeksRemaining}W)"
            !activeInjury.type.severity.canPlayThrough -> "OUT (${activeInjury.weeksRemaining}W)"
            else -> "${activeInjury.type.severity.label.substringBefore(" (")} (${activeInjury.weeksRemaining}W)"
        }

    val tradeValue: Int
        get() = ((overallRating - 60).coerceAtLeast(1) * 3) +
                ((35 - age).coerceAtLeast(0) * 2) +
                (contractYearsRemaining * 3)
}

data class DraftPick(
    val id: String,
    val year: Int,
    val round: Int,
    val originalFranchiseId: String,
    val originalFranchiseName: String,
    val currentFranchiseId: String,
    val isProtected: Boolean = false,
    val protectionDescription: String = "Unprotected",
    val acquiredFromFranchiseId: String? = null,
    val acquiredFromFranchiseName: String? = null,
    val acquiredInWeek: Int? = null,
    val acquiredTradeId: String? = null,
    val deferredFromYear: Int? = null
) {
    val isTraded: Boolean
        get() = currentFranchiseId != originalFranchiseId

    val tradeValue: Int
        get() = when (round) {
            1 -> if (isProtected) 45 else 65
            2 -> 25
            else -> 10
        }

    val displayName: String
        get() {
            val protLabel = if (isProtected) " - $protectionDescription" else ""
            val defLabel = if (deferredFromYear != null) " • Rollover from $deferredFromYear" else ""
            val viaLabel = if (isTraded) " • via $originalFranchiseName" else ""
            return "$year R$round Pick ($originalFranchiseName$protLabel$defLabel$viaLabel)"
        }
}

enum class TradedPickStatus(val label: String, val badgeColorHex: Long) {
    PENDING_CONVEYANCE("Active Traded Pick", 0xFFF59E0B),
    PROTECTION_DEFERRED("Protected • Deferred", 0xFF06B6D4),
    EXERCISED_IN_DRAFT("Exercised in Draft", 0xFF10B981)
}

data class TradedPickLedgerEntry(
    val pickId: String,
    val year: Int,
    val round: Int,
    val originalFranchiseId: String,
    val originalFranchiseName: String,
    val fromFranchiseId: String,
    val fromFranchiseName: String,
    val toFranchiseId: String,
    val toFranchiseName: String,
    val weekTraded: Int?,
    val isProtected: Boolean = false,
    val protectionDescription: String = "Unprotected",
    val deferredFromYear: Int? = null,
    val projectedSlot: Int? = null,
    val status: TradedPickStatus = TradedPickStatus.PENDING_CONVEYANCE,
    val outcomeSummary: String = ""
)

data class DraftOrderSlot(
    val pickNumber: Int,
    val round: Int,
    val slotInRound: Int,
    val originalFranchiseId: String,
    val originalFranchiseName: String,
    val ownerFranchiseId: String,
    val ownerFranchiseName: String,
    val sourcePickId: String = "",
    val isTraded: Boolean = false,
    val isProtected: Boolean = false,
    val protectionDescription: String = "Unprotected",
    val retainedDueToProtection: Boolean = false,
    val tradeSummaryNote: String = ""
)

enum class TradeStatus(val label: String, val badgeColorHex: Long) {
    PENDING_REVIEW("Pending Commissioner Review", 0xFFF59E0B),
    ACCEPTED("Approved & Ratified", 0xFF10B981),
    REJECTED("Rejected", 0xFFEF4444),
    VETOED("Vetoed by Commissioner", 0xFFDC2626),
    EXPIRED("Trade Deadline Passed", 0xFF6B7280)
}

data class TradeProposal(
    val id: String,
    val weekProposed: Int,
    val teamAId: String,
    val teamAName: String,
    val teamBId: String,
    val teamBName: String,
    val teamASendsPlayers: List<RosterPlayer> = emptyList(),
    val teamASendsPicks: List<DraftPick> = emptyList(),
    val teamASendsCashMillions: Long = 0L,
    val teamBSendsPlayers: List<RosterPlayer> = emptyList(),
    val teamBSendsPicks: List<DraftPick> = emptyList(),
    val teamBSendsCashMillions: Long = 0L,
    val status: TradeStatus = TradeStatus.PENDING_REVIEW,
    val rationaleTeamA: String = "",
    val rationaleTeamB: String = "",
    val fairnessScore: Int = 85, // 0-100
    val salaryCapCompliant: Boolean = true,
    val commissionerReviewNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val teamATotalTradeValue: Int
        get() = teamASendsPlayers.sumOf { it.tradeValue } + teamASendsPicks.sumOf { it.tradeValue } + (teamASendsCashMillions * 2).toInt()

    val teamBTotalTradeValue: Int
        get() = teamBSendsPlayers.sumOf { it.tradeValue } + teamBSendsPicks.sumOf { it.tradeValue } + (teamBSendsCashMillions * 2).toInt()

    val teamASalaryOut: Long
        get() = teamASendsPlayers.sumOf { it.salaryMillions }

    val teamBSalaryOut: Long
        get() = teamBSendsPlayers.sumOf { it.salaryMillions }
}

data class TradeRules(
    val tradeDeadlineWeek: Int = 10, // Regular season week (e.g. 4 to 14, default 10)
    val requireCommissionerApproval: Boolean = true,
    val allowDraftPickTrading: Boolean = true,
    val maxFuturePickYears: Int = 3,
    val salaryMatchingStrictnessPct: Int = 125, // % incoming salary allowed
    val allowCashConsiderations: Boolean = true
) {
    fun isTradeWindowOpen(currentWeek: Int, seasonPhase: SeasonPhase): Boolean {
        return seasonPhase == SeasonPhase.REGULAR_SEASON && currentWeek <= tradeDeadlineWeek
    }

    fun isTradeDeadlineWeek(currentWeek: Int, seasonPhase: SeasonPhase): Boolean {
        return seasonPhase == SeasonPhase.REGULAR_SEASON && currentWeek == tradeDeadlineWeek
    }
}

enum class TradeAskingPriority(
    val label: String,
    val badgeColorHex: Long,
    val description: String
) {
    CONTENDER_UPGRADE("Win-Now Star", 0xFF10B981, "Seeking high-impact veteran starter for playoff run"),
    FUTURE_DRAFT_CAPITAL("Draft Capital", 0xFFF59E0B, "Seeking 1st or 2nd round picks & rebuild flexibility"),
    YOUNG_PROSPECT("Young Talent", 0xFF06B6D4, "Seeking ascending under-25 rotation contributor"),
    CAP_RELIEF("Cap Relief", 0xFF3B82F6, "Seeking expiring salary & payroll space"),
    POSITIONAL_FILL("Positional Need", 0xFF8B5CF6, "Seeking immediate lineup upgrade at target position")
}

data class TradeBlockListing(
    val playerId: String,
    val askingPriority: TradeAskingPriority = TradeAskingPriority.FUTURE_DRAFT_CAPITAL,
    val askingNote: String = ""
)

data class Franchise(
    val id: String,
    val cityId: String,
    val cityName: String,
    val nickname: String,
    val colorHex: Long,
    val secondaryColorHex: Long = FranchiseIdentityEngine.defaultSecondaryFor(colorHex),
    val ownership: OwnershipGroup,
    val conference: Conference = Conference.EASTERN,
    val division: Division = Division.ATLANTIC,
    val rosterRating: Int = 75, // 50-99 (Team recruitment strength)
    val fanBaseMillions: Double = 1.2,
    val fanMorale: Int = 75, // 0-100
    val stadiumCapacity: Int = 22000,
    val ticketPrice: Int = 65,
    val annualPayrollMillions: Long = 65L,
    val localSponsorRevenueMillions: Long = 28L,
    val wins: Int = 0,
    val losses: Int = 0,
    val pointsFor: Int = 0,
    val pointsAgainst: Int = 0,
    val championships: Int = 0,
    val roster: List<RosterPlayer> = emptyList(),
    val draftPicks: List<DraftPick> = emptyList(),
    val tradeBlockListings: List<TradeBlockListing> = emptyList(),
    val seekingPlayerIds: List<String> = emptyList(),
    val seekingPositions: List<String> = emptyList(),
    val rivalryTicketBonusMillions: Double = 0.0,
    val fanLoyalty: Int = 75, // 0-100 driven by performance, ticket pricing & marketing
    val activeFranchiseCampaign: ActiveFranchiseCampaign? = null,
    val lastLoyaltyDrivers: FanLoyaltyDrivers = FanLoyaltyDrivers(),
    val facilities: FranchiseFacilities = FranchiseFacilities(),
    val stadiumAge: Int = 14,
    val stadiumCondition: Int = 78
) {
    val winPct: Double
        get() = if (wins + losses == 0) 0.500 else wins.toDouble() / (wins + losses)

    val loyaltyTier: FanLoyaltyTier
        get() = FanLoyaltyEngine.tierFor(fanLoyalty)

    val fairTicketPrice: Int
        get() = FanLoyaltyEngine.computeFairTicketPrice(this)

    fun calculateValuationBreakdown(activeBroadcast: BroadcastContract, totalTeams: Int): FranchiseValuationBreakdown =
        FacilityEngine.calculateValuationBreakdown(this, activeBroadcast, totalTeams)

    val teamFullName: String
        get() = "$cityName $nickname"

    val alignmentLabel: String
        get() = "${conference.shortCode} • ${division.shortCode}"

    val effectiveLineupRating: Int
        get() = InjuryEngine.calculateDepthChartEffectiveRating(this)

    val injuredPlayersCount: Int
        get() = roster.count { it.isInjured }

    val suspendedPlayersCount: Int
        get() = roster.count { it.isSuspended }

    val unresolvedStarterHolesCount: Int
        get() = InjuryEngine.countUnresolvedStarterHoles(this)

    val tradeBlockPlayers: List<RosterPlayer>
        get() = tradeBlockListings.mapNotNull { listing -> roster.find { it.id == listing.playerId } }

    fun isPlayerOnTradeBlock(playerId: String): Boolean =
        tradeBlockListings.any { it.playerId == playerId }

    fun isSeekingPlayer(playerId: String): Boolean =
        playerId in seekingPlayerIds
}

enum class BroadcastPlatform(val label: String, val description: String) {
    LINEAR_CABLE("National Cable Network", "Guaranteed steady upfront rights fees and older demographic reach"),
    GLOBAL_STREAMER("Tech Global Streamer", "High variable bonuses based on youth viewership and global reach"),
    FREE_TO_AIR("Free Broadcast Television", "Maximum domestic accessibility to build grassroots fanbase"),
    HYBRID_SYNDICATE("Hybrid Sports Network", "Balanced multi-channel broadcast package")
}

enum class NetworkScope(val label: String, val badgeColorHex: Long) {
    NATIONAL_CABLE("National Cable Network", 0xFF3B82F6),
    GLOBAL_STREAMING("Global Tech Streamer", 0xFF06B6D4),
    REGIONAL_RSN("Regional Sports Network (RSN)", 0xFF10B981),
    FREE_TO_AIR("Free Broadcast Television", 0xFFF59E0B)
}

data class BroadcastContract(
    val id: String,
    val partnerName: String,
    val platform: BroadcastPlatform,
    val annualPayoutMillions: Long,
    val yearsRemaining: Int,
    val reachMultiplier: Double,
    val bonusPerPlayoffGameMillions: Long = 4L,
    val upfrontSigningBonusMillions: Long = 0L,
    val networkScope: NetworkScope = NetworkScope.NATIONAL_CABLE,
    val hasExclusivity: Boolean = true,
    val exclusivityLostPenaltyPct: Int = 0
)

data class NetworkBidder(
    val id: String,
    val name: String,
    val scope: NetworkScope,
    val executiveName: String,
    val executiveTitle: String,
    val executiveQuote: String,
    val baseAnnualPayoutMillions: Long,
    val baseLumpSumMillions: Long,
    val preferredTermYears: Int,
    val maxTermYears: Int = 7,
    val minTermYears: Int = 1,
    val reachMultiplier: Double = 1.4,
    val patienceRating: Int = 100
)


enum class SalaryCapType(val displayName: String) {
    HARD_CAP("Hard Salary Cap (Strict Limit)"),
    SOFT_CAP_LUXURY_TAX("Soft Cap + 100% Luxury Tax Penalty"),
    FREE_MARKET("Unrestricted Open Market")
}

data class LaborUnionAgreement(
    val unionSatisfaction: Int = 78, // 0-100
    val strikeRisk: Int = 12, // 0-100
    val playerRevenueSharePct: Int = 50, // 46% - 54%
    val salaryCapType: SalaryCapType = SalaryCapType.SOFT_CAP_LUXURY_TAX,
    val salaryCapMillions: Long = 80L,
    val minPlayerSalaryK: Long = 650L,
    val playerHealthWelfareMillions: Long = 15L,
    val cbaYearsRemaining: Int = 3,
    val mandatoryPrivateCharterFlights: Boolean = true,
    val maxBackToBackGamesLimit: Boolean = true,
    val independentConcussionProtocol: Boolean = true,
    val advancedHybridTurfMandate: Boolean = false,
    val postCareerTransitionFundMillions: Long = 5L,
    val fiveStarHotelSingleRooms: Boolean = true,
    val isLockoutActive: Boolean = false,
    val lockoutWeeksLost: Int = 0,
    val lastLockoutSeasonYear: Int = 0,
    val negotiatedRosterRule: RosterSizeRule = RosterSizeRule.STANDARD_15
)

enum class PolicyStrictness(val title: String, val integrityModifier: Int, val playerMoraleModifier: Int) {
    LENIENT("Rehabilitation & Minor Warnings", -10, +10),
    BALANCED("Standard Fines & Suspensions", +5, 0),
    ZERO_TOLERANCE("Immediate Bans & Heavy Sanctions", +20, -12)
}

enum class DraftParitySystem(val title: String, val description: String) {
    WEIGHTED_LOTTERY("Weighted Draft Lottery", "Bottom 4 teams get highest odds for top recruits"),
    REVERSE_STANDINGS("Strict Reverse Standings", "Last place receives #1 draft pick directly"),
    OPEN_RECRUITMENT("Open Youth Market", "Teams bid directly from player development pools")
}

enum class RuleCategory(val title: String, val badgeColorHex: Long) {
    FINANCIAL_PARITY("Salary & Finances", 0xFF10B981), // Emerald
    DRAFT_TALENT("Draft & Parity", 0xFF3B82F6),        // Blue
    GAMEPLAY_PACE("Pace & Performance", 0xFFF59E0B),  // Amber
    INTEGRITY_CONDUCT("Integrity & Codex", 0xFF8B5CF6) // Purple
}

data class LeagueRule(
    val id: String,
    val name: String,
    val category: RuleCategory,
    val summary: String,
    val isEnacted: Boolean,
    val parityImpact: Int,             // +/- effect on competitive balance
    val scoringTempoModifier: Int,     // +/- effect on scoring pace (%)
    val integrityImpact: Int,          // +/- effect on officiating/integrity
    val fanAppealModifier: Int,        // +/- effect on fan engagement
    val smallMarketBenefitRating: Int, // 1 to 5 scale
    val description: String,
    val enactedEffect: String,
    val repealedEffect: String,
    val proPerspective: String,
    val contraPerspective: String
)

enum class PlayoffQualificationMode(val displayName: String, val description: String) {
    CONFERENCE_SPLIT(
        displayName = "Conference Allocation",
        description = "Playoff berths distributed fairly across every active conference"
    ),
    CONFERENCE_CHAMPIONS_WILD_CARD(
        displayName = "Conf. Champions + Wild Cards",
        description = "Every active conference champion earns an automatic berth; remaining slots go to best global records"
    ),
    DIVISION_CHAMPIONS_SHOWCASE(
        displayName = "Division Winners + At-Large",
        description = "Every active division champion qualifies automatically; wild cards fill remaining berths"
    ),
    OVERALL_LEAGUE_TABLE(
        displayName = "Single League Table",
        description = "Top overall franchises qualify strictly by league-wide win percentage"
    )
}

enum class PlayoffSeedingMethod(val displayName: String, val description: String) {
    DIVISION_WINNERS_FIRST(
        displayName = "Division Winners Top Seeds",
        description = "Division & conference champions guaranteed top seeds; wild cards seeded beneath"
    ),
    PURE_WIN_PERCENTAGE(
        displayName = "Strict Win Percentage",
        description = "All qualifiers seeded purely by regular-season win-loss percentage"
    ),
    CONFERENCE_PROTECTED(
        displayName = "Conference Protected",
        description = "Interleaves seeds across all active conferences to preserve cross-continental matchups for later rounds"
    )
}

data class PlayoffRules(
    val fieldSize: Int = 4, // 2, 4, 6, 8, 10, or 12 teams
    val qualificationMode: PlayoffQualificationMode = PlayoffQualificationMode.CONFERENCE_SPLIT,
    val divisionWinnersAutoBerth: Boolean = true,
    val seedingMethod: PlayoffSeedingMethod = PlayoffSeedingMethod.DIVISION_WINNERS_FIRST,
    val firstRoundByesCount: Int = 0, // 0, 1, 2, 4, or 6
    val playInTournamentEnabled: Boolean = false
) {
    val wildCardsCount: Int
        get() = (fieldSize - (if (divisionWinnersAutoBerth) 2 else 0)).coerceAtLeast(0)

    fun effectiveWildCardsCount(franchises: List<Franchise>): Int {
        if (franchises.isEmpty()) return wildCardsCount
        val activeDivs = franchises.map { it.division }.distinct().size
        val activeConfs = franchises.map { it.conference }.distinct().size
        val autoBerths = when (qualificationMode) {
            PlayoffQualificationMode.CONFERENCE_CHAMPIONS_WILD_CARD -> activeConfs
            PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE -> activeDivs
            PlayoffQualificationMode.CONFERENCE_SPLIT -> if (divisionWinnersAutoBerth) activeDivs.coerceAtMost(fieldSize) else activeConfs
            PlayoffQualificationMode.OVERALL_LEAGUE_TABLE -> if (divisionWinnersAutoBerth) activeDivs.coerceAtMost(fieldSize) else 0
        }
        return (fieldSize - autoBerths).coerceAtLeast(0)
    }

    val formatSummary: String
        get() = "$fieldSize Teams • ${if (firstRoundByesCount > 0) "$firstRoundByesCount Byes • " else ""}${if (divisionWinnersAutoBerth) "Div Winners Guaranteed • " else ""}${qualificationMode.displayName}"
}

data class PlayoffStructureOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val rationale: String,
    val recommendedBadge: String?,
    val rules: PlayoffRules,
    val roundsSummary: String,
    val activeConferenceCount: Int,
    val activeDivisionCount: Int
)

object PlayoffStructureEngine {
    fun generatePossibleStructures(
        franchises: List<Franchise>,
        enabledConferences: Set<Conference>,
        enabledDivisions: Set<Division>
    ): List<PlayoffStructureOption> {
        val teamCount = franchises.size.coerceAtLeast(2)
        val populatedConfs = franchises.map { it.conference }.distinct()
        val populatedDivs = franchises.map { it.division }.distinct()
        val activeConfs = (populatedConfs + enabledConferences).distinct()
        val activeDivs = (populatedDivs + enabledDivisions).distinct()
        val confCount = populatedConfs.size.coerceAtLeast(activeConfs.size).coerceAtLeast(1)
        val divCount = populatedDivs.size.coerceAtLeast(1)
        val options = mutableListOf<PlayoffStructureOption>()

        when {
            populatedConfs.size <= 2 -> {
                val standardSize = if (teamCount >= 8) 8 else if (teamCount >= 6) 6 else 4
                options.add(
                    PlayoffStructureOption(
                        id = "struct_dual_conf_split",
                        title = "Bylaw Classic ${standardSize}-Team Dual Conference",
                        subtitle = "${populatedConfs.size} Conferences • Balanced Conference Split",
                        rationale = "Top qualifiers split evenly across ${populatedConfs.joinToString(" & ") { it.shortCode }}, protecting conference rivalry Finals.",
                        recommendedBadge = "RECOMMENDED FOR 2 CONFERENCES",
                        rules = PlayoffRules(
                            fieldSize = standardSize,
                            qualificationMode = PlayoffQualificationMode.CONFERENCE_SPLIT,
                            divisionWinnersAutoBerth = true,
                            seedingMethod = PlayoffSeedingMethod.CONFERENCE_PROTECTED,
                            firstRoundByesCount = if (standardSize == 6) 2 else 0,
                            playInTournamentEnabled = teamCount >= 10
                        ),
                        roundsSummary = if (standardSize == 8) "Quarterfinals -> Semis -> Finals" else if (standardSize == 6) "Wild Card -> Semis -> Finals" else "Semis -> Finals",
                        activeConferenceCount = confCount,
                        activeDivisionCount = divCount
                    )
                )
                val divField = if (teamCount >= 8) 8 else 6.coerceAtMost(teamCount)
                options.add(
                    PlayoffStructureOption(
                        id = "struct_div_showcase_2conf",
                        title = "Division Champions Showcase ($divField Teams)",
                        subtitle = "$divCount Populated Divisions • Auto-Berths + Wild Cards",
                        rationale = "Guarantees every active division leader a berth with top seeds awarded to division champs.",
                        recommendedBadge = null,
                        rules = PlayoffRules(
                            fieldSize = divField,
                            qualificationMode = PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE,
                            divisionWinnersAutoBerth = true,
                            seedingMethod = PlayoffSeedingMethod.DIVISION_WINNERS_FIRST,
                            firstRoundByesCount = if (divField == 6) 2 else 0
                        ),
                        roundsSummary = if (divField >= 8) "Quarterfinals -> Semis -> Finals" else "Wild Card -> Semis -> Finals",
                        activeConferenceCount = confCount,
                        activeDivisionCount = divCount
                    )
                )
            }
            populatedConfs.size == 3 -> {
                val triSize = if (teamCount >= 8) 8 else 6.coerceAtMost(teamCount)
                val byes = if (triSize == 6) 2 else 0
                options.add(
                    PlayoffStructureOption(
                        id = "struct_tri_conf_champs",
                        title = "Tri-Continental Champions + Wild Card ($triSize Teams)",
                        subtitle = "3 Active Conferences (${populatedConfs.joinToString(" / ") { it.shortCode }})",
                        rationale = "Guarantees all 3 conference champions an automatic bid while reserving ${triSize - 3} wild card spots for top runners-up.",
                        recommendedBadge = "RECOMMENDED FOR 3 CONFERENCES",
                        rules = PlayoffRules(
                            fieldSize = triSize,
                            qualificationMode = PlayoffQualificationMode.CONFERENCE_CHAMPIONS_WILD_CARD,
                            divisionWinnersAutoBerth = true,
                            seedingMethod = PlayoffSeedingMethod.CONFERENCE_PROTECTED,
                            firstRoundByesCount = byes,
                            playInTournamentEnabled = teamCount >= 10
                        ),
                        roundsSummary = if (triSize == 8) "Quarterfinals (8) -> Semis (4) -> Finals" else "Wild Card (Top 2 Byes) -> Semis -> Finals",
                        activeConferenceCount = confCount,
                        activeDivisionCount = divCount
                    )
                )
                options.add(
                    PlayoffStructureOption(
                        id = "struct_tri_conf_balanced",
                        title = "Balanced 3-Conference Allocation (${minOf(teamCount, 6)} Teams)",
                        subtitle = "Equal 2-Per-Conference Representation",
                        rationale = "Allocates 2 berths to each of the 3 active conferences (${populatedConfs.joinToString(", ") { it.shortCode }}) for strict parity.",
                        recommendedBadge = null,
                        rules = PlayoffRules(
                            fieldSize = minOf(teamCount, 6),
                            qualificationMode = PlayoffQualificationMode.CONFERENCE_SPLIT,
                            divisionWinnersAutoBerth = true,
                            seedingMethod = PlayoffSeedingMethod.CONFERENCE_PROTECTED,
                            firstRoundByesCount = if (minOf(teamCount, 6) == 6) 2 else 0
                        ),
                        roundsSummary = "Opening Round -> Semis -> Finals",
                        activeConferenceCount = confCount,
                        activeDivisionCount = divCount
                    )
                )
            }
            else -> {
                // 4 or 5 active conferences (North America + Continental Conferences)
                val globalField = when {
                    teamCount >= 12 -> 12
                    teamCount >= 10 -> 10
                    else -> minOf(teamCount, 8)
                }
                val standard8 = minOf(teamCount, 8)
                options.add(
                    PlayoffStructureOption(
                        id = "struct_global_conf_champs_8",
                        title = "Intercontinental $standard8-Team Champions Cup",
                        subtitle = "${populatedConfs.size} Active Conferences (${populatedConfs.joinToString(" • ") { it.shortCode }})",
                        rationale = "Every active conference champion clinches an automatic berth; remaining ${standard8 - populatedConfs.size} slots go to top global wild cards with conference-protected seeding.",
                        recommendedBadge = if (teamCount < 12) "RECOMMENDED FOR ${populatedConfs.size} CONFERENCES" else "CLASSIC 8-TEAM GLOBAL",
                        rules = PlayoffRules(
                            fieldSize = standard8,
                            qualificationMode = PlayoffQualificationMode.CONFERENCE_CHAMPIONS_WILD_CARD,
                            divisionWinnersAutoBerth = true,
                            seedingMethod = PlayoffSeedingMethod.CONFERENCE_PROTECTED,
                            firstRoundByesCount = 0,
                            playInTournamentEnabled = teamCount >= 10
                        ),
                        roundsSummary = if (standard8 >= 8) "Quarterfinals -> Semis -> Finals" else "Opening Round -> Semis -> Finals",
                        activeConferenceCount = confCount,
                        activeDivisionCount = divCount
                    )
                )
                if (teamCount >= 10) {
                    val prelimByes = if (globalField == 12) 4 else 6
                    options.add(
                        PlayoffStructureOption(
                            id = "struct_mega_global_expanded",
                            title = "Expanded $globalField-Team World Cup Bracket",
                            subtitle = "${populatedConfs.size} Conferences • $divCount Divisions • $prelimByes Byes",
                            rationale = "Accommodates continental expansion: top $prelimByes conference/division leaders earn Quarterfinal byes while ${globalField - prelimByes} contenders battle in the Opening Round.",
                            recommendedBadge = if (teamCount >= 12) "RECOMMENDED FOR ${populatedConfs.size} CONFERENCES" else "EXPANSION FORMAT",
                            rules = PlayoffRules(
                                fieldSize = globalField,
                                qualificationMode = PlayoffQualificationMode.CONFERENCE_SPLIT,
                                divisionWinnersAutoBerth = true,
                                seedingMethod = PlayoffSeedingMethod.CONFERENCE_PROTECTED,
                                firstRoundByesCount = prelimByes,
                                playInTournamentEnabled = true
                            ),
                            roundsSummary = "Global Wild Card ($prelimByes Byes) -> Quarterfinals -> Semis -> Finals",
                            activeConferenceCount = confCount,
                            activeDivisionCount = divCount
                        )
                    )
                }
            }
        }

        // Division-centric option when expanded divisions exist (>= 4 divisions)
        if (divCount >= 4) {
            val divBracketSize = when {
                divCount >= 9 && teamCount >= 12 -> 12
                divCount >= 7 && teamCount >= 10 -> 10
                teamCount >= 8 -> 8
                else -> minOf(teamCount, 6)
            }
            if (options.none { it.rules.qualificationMode == PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE && it.rules.fieldSize == divBracketSize }) {
                options.add(
                    PlayoffStructureOption(
                        id = "struct_all_division_leaders",
                        title = "All-Division Champions Tournament ($divBracketSize Teams)",
                        subtitle = "$divCount Populated Divisions • Regional Rivalry Focus",
                        rationale = "Prioritizes every populated regional division champion across North America and international continents, filling leftover seats by Win%.",
                        recommendedBadge = "DIVISION-FIRST",
                        rules = PlayoffRules(
                            fieldSize = divBracketSize,
                            qualificationMode = PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE,
                            divisionWinnersAutoBerth = true,
                            seedingMethod = PlayoffSeedingMethod.DIVISION_WINNERS_FIRST,
                            firstRoundByesCount = when (divBracketSize) {
                                12 -> 4
                                10 -> 6
                                6 -> 2
                                else -> 0
                            }
                        ),
                        roundsSummary = if (divBracketSize > 8) "Opening Wild Card -> Quarterfinals -> Semis -> Finals" else if (divBracketSize == 8) "Quarterfinals -> Semis -> Finals" else "Wild Card -> Semis -> Finals",
                        activeConferenceCount = confCount,
                        activeDivisionCount = divCount
                    )
                )
            }
        }

        // Always provide Unified Global League Table option
        val tableField = minOf(teamCount, if (teamCount >= 8) 8 else 4)
        options.add(
            PlayoffStructureOption(
                id = "struct_global_merit_table",
                title = "Single Global Merit Table ($tableField Teams)",
                subtitle = "Conference-Agnostic Pure Win% Bracket",
                rationale = "Disregards continental boundaries completely—the top $tableField records league-wide qualify on pure regular-season merit.",
                recommendedBadge = null,
                rules = PlayoffRules(
                    fieldSize = tableField,
                    qualificationMode = PlayoffQualificationMode.OVERALL_LEAGUE_TABLE,
                    divisionWinnersAutoBerth = false,
                    seedingMethod = PlayoffSeedingMethod.PURE_WIN_PERCENTAGE,
                    firstRoundByesCount = if (tableField == 6) 2 else 0
                ),
                roundsSummary = if (tableField >= 8) "Quarterfinals -> Semis -> Finals" else "Semis -> Finals",
                activeConferenceCount = confCount,
                activeDivisionCount = divCount
            )
        )

        return options
    }

    fun adaptRulesForLeagueStructure(
        currentRules: PlayoffRules,
        franchises: List<Franchise>,
        enabledConferences: Set<Conference>
    ): PlayoffRules {
        val populatedConfs = franchises.map { it.conference }.distinct()
        val populatedDivs = franchises.map { it.division }.distinct()
        val teamCount = franchises.size.coerceAtLeast(2)

        if (populatedConfs.size <= 2) {
            return currentRules
        }

        // Multi-continental league active (3, 4, or 5 populated conferences)
        val recommendedField = when {
            teamCount >= 12 && (populatedConfs.size >= 4 || populatedDivs.size >= 8) -> 12
            teamCount >= 10 && populatedDivs.size >= 7 -> 10
            teamCount >= 8 -> 8
            else -> maxOf(currentRules.fieldSize, minOf(teamCount, 6))
        }.coerceIn(2, teamCount)

        val recommendedMode = when (currentRules.qualificationMode) {
            PlayoffQualificationMode.OVERALL_LEAGUE_TABLE -> PlayoffQualificationMode.OVERALL_LEAGUE_TABLE
            PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE -> PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE
            else -> if (populatedConfs.size >= 3 && recommendedField < populatedConfs.size * 2) {
                PlayoffQualificationMode.CONFERENCE_CHAMPIONS_WILD_CARD
            } else {
                PlayoffQualificationMode.CONFERENCE_SPLIT
            }
        }

        val recommendedByes = when (recommendedField) {
            12 -> 4
            10 -> 6
            6 -> 2
            else -> 0
        }

        return currentRules.copy(
            fieldSize = maxOf(currentRules.fieldSize, recommendedField).coerceAtMost(teamCount),
            qualificationMode = recommendedMode,
            firstRoundByesCount = recommendedByes,
            seedingMethod = if (currentRules.seedingMethod == PlayoffSeedingMethod.PURE_WIN_PERCENTAGE) {
                PlayoffSeedingMethod.PURE_WIN_PERCENTAGE
            } else {
                PlayoffSeedingMethod.CONFERENCE_PROTECTED
            }
        )
    }
}

data class PlayoffSeedInfo(
    val seed: Int,
    val franchise: Franchise,
    val eligibilityTag: String,
    val eligibilityDescription: String,
    val hasBye: Boolean,
    val isDivisionChampion: Boolean,
    val isWildCard: Boolean
)

enum class OvertimeFormat(
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val scoringTempoBonus: Int,
    val fanAppealBonus: Int
) {
    STANDARD_PERIOD(
        displayName = "Standard Timed Overtime",
        shortLabel = "5-Min Period",
        description = "Classic 5-minute extra period with full clock; repeated until a winner emerges.",
        scoringTempoBonus = +4,
        fanAppealBonus = +6
    ),
    TARGET_SCORE_ELAM(
        displayName = "Target Score (Elam Ending)",
        shortLabel = "Target Score",
        description = "Clock shuts off in final minutes; teams race to target score (+8 points), ending on an electric game-winner.",
        scoringTempoBonus = +12,
        fanAppealBonus = +18
    ),
    SUDDEN_DEATH(
        displayName = "Sudden Death (Golden Score)",
        shortLabel = "Golden Score",
        description = "First team to score any points in extra time immediately clinches victory; maximum sudden-death tension.",
        scoringTempoBonus = +2,
        fanAppealBonus = +10
    ),
    PENALTY_SHOOTOUT(
        displayName = "Penalty Shootout Contest",
        shortLabel = "Shootout",
        description = "Best-of-5 direct penalty/free-throw attempts to decide outcome without physical player exhaustion.",
        scoringTempoBonus = +6,
        fanAppealBonus = +12
    )
}

enum class RosterSizeRule(
    val activeSlots: Int,
    val reserveSlots: Int,
    val displayName: String,
    val description: String,
    val injuryResiliencyBonus: Int,
    val payrollBurdenModifier: Int
) {
    COMPACT_12(
        activeSlots = 12,
        reserveSlots = 0,
        displayName = "12 Active (Compact)",
        description = "12 active players, zero reserve slots. Maximizes superstar minutes and lowers team payroll overhead.",
        injuryResiliencyBonus = -6,
        payrollBurdenModifier = -10
    ),
    STANDARD_15(
        activeSlots = 15,
        reserveSlots = 2,
        displayName = "15 Active + 2 Two-Way (Standard)",
        description = "15 active players plus 2 developmental two-way contracts. Optimal balance between depth and star usage.",
        injuryResiliencyBonus = +6,
        payrollBurdenModifier = 0
    ),
    EXPANDED_18(
        activeSlots = 18,
        reserveSlots = 4,
        displayName = "18 Active + 4 Taxi Squad (Expanded)",
        description = "18 active players plus 4 taxi squad reserve slots. Maximum injury protection and prospect development.",
        injuryResiliencyBonus = +16,
        payrollBurdenModifier = +12
    )
}

enum class SalaryCapStrictness(
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val parityImpact: Int,
    val penaltyRatePct: Int
) {
    STRICT_HARD_CAP(
        displayName = "Strict Hard Cap (Ironclad)",
        shortLabel = "Hard Cap",
        description = "Absolute payroll ceiling with zero exceptions. Violations result in automatic loss of draft picks.",
        parityImpact = +18,
        penaltyRatePct = 0
    ),
    PROGRESSIVE_LUXURY_TAX(
        displayName = "Progressive Luxury Tax",
        shortLabel = "Progressive Tax",
        description = "Soft cap with dollar-for-dollar progressive tax penalties (100% to 300%) pooled into small-market grants.",
        parityImpact = +12,
        penaltyRatePct = 150
    ),
    MODERATE_SOFT_CAP(
        displayName = "Moderate Soft Cap + Mid-Level",
        shortLabel = "Soft Cap",
        description = "Permits mid-level and bird-rights player contract exceptions with moderate luxury tax thresholds.",
        parityImpact = +6,
        penaltyRatePct = 75
    ),
    UNRESTRICTED_MARKET(
        displayName = "Unrestricted Free Market",
        shortLabel = "Free Market",
        description = "Zero payroll limits. Franchises can spend without bounds, widening gap between large and small markets.",
        parityImpact = -14,
        penaltyRatePct = 0
    )
}

enum class InjuryFrequencyRule(
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val chanceMultiplier: Double,
    val fanRealismDelta: Int
) {
    INJURIES_OFF(
        displayName = "Injuries Disabled",
        shortLabel = "Off",
        description = "Zero in-game injuries across regular season and playoffs; full healthy lineups every week.",
        chanceMultiplier = 0.0,
        fanRealismDelta = -4
    ),
    LOW_PROTECTED(
        displayName = "Low / Load Managed (0.5x)",
        shortLabel = "Low (0.5x)",
        description = "50% reduced injury frequency simulating proactive load management and recovery protocols.",
        chanceMultiplier = 0.5,
        fanRealismDelta = +2
    ),
    STANDARD_REALISTIC(
        displayName = "Standard Pro Simulation (1.0x)",
        shortLabel = "Standard",
        description = "Balanced, realistic injury risk tied to minutes load, fatigue, and medical facility tier.",
        chanceMultiplier = 1.0,
        fanRealismDelta = 0
    ),
    HIGH_GRIND(
        displayName = "Physical Grind (1.6x)",
        shortLabel = "High (1.6x)",
        description = "60% higher in-game contact & fatigue injury risk, placing heavy value on deep benches.",
        chanceMultiplier = 1.6,
        fanRealismDelta = -3
    )
}

enum class GameLengthRule(
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val minutesEquivalent: Int,
    val scoreScaleMultiplier: Double,
    val injuryRiskMultiplier: Double,
    val fanAppealBonus: Int
) {
    SHORTENED_36(
        displayName = "Shortened Showcase (36 Min)",
        shortLabel = "36 Min",
        description = "Crisp 36-minute regulation games (~0.78x score totals) with lower fatigue & reduced injury wear.",
        minutesEquivalent = 36,
        scoreScaleMultiplier = 0.78,
        injuryRiskMultiplier = 0.80,
        fanAppealBonus = +2
    ),
    STANDARD_48(
        displayName = "Standard Regulation (48 Min)",
        shortLabel = "48 Min",
        description = "Traditional full-length pro regulation format with standard scoring volume and fatigue.",
        minutesEquivalent = 48,
        scoreScaleMultiplier = 1.0,
        injuryRiskMultiplier = 1.0,
        fanAppealBonus = 0
    ),
    MARATHON_60(
        displayName = "Extended Endurance (60 Min)",
        shortLabel = "60 Min",
        description = "Marathon 60-minute regulation (~1.22x score totals) demanding deep rotation stamina.",
        minutesEquivalent = 60,
        scoreScaleMultiplier = 1.22,
        injuryRiskMultiplier = 1.25,
        fanAppealBonus = +4
    )
}

data class LeagueRuleBook(
    val rules: List<LeagueRule> = emptyList(),
    val overtimeFormat: OvertimeFormat = OvertimeFormat.STANDARD_PERIOD,
    val rosterSizeRule: RosterSizeRule = RosterSizeRule.STANDARD_15,
    val salaryCapStrictness: SalaryCapStrictness = SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX,
    val injuryFrequency: InjuryFrequencyRule = InjuryFrequencyRule.STANDARD_REALISTIC,
    val gameLength: GameLengthRule = GameLengthRule.STANDARD_48,
    val antiDopingPolicy: PolicyStrictness = PolicyStrictness.BALANCED,
    val antiTamperingGamblingPolicy: PolicyStrictness = PolicyStrictness.ZERO_TOLERANCE,
    val draftParity: DraftParitySystem = DraftParitySystem.WEIGHTED_LOTTERY,
    val instantReplayEnabled: Boolean = true,
    val shotClockFastPace: Boolean = true,
    val playoffRules: PlayoffRules = PlayoffRules(),
    val tradeRules: TradeRules = TradeRules(),
    val enabledConferences: Set<Conference> = setOf(Conference.EASTERN, Conference.WESTERN),
    val enabledDivisions: Set<Division> = Division.values().filter { !it.conference.isContinentalInternational }.toSet(),
    val regularSeasonWeeks: Int = 14,
    val integrityScore: Int = 85, // 0-100
    val competitiveParityScore: Int = 82 // 0-100
) {
    val enactedRulesCount: Int
        get() = rules.count { it.isEnacted }

    val repealedRulesCount: Int
        get() = rules.count { !it.isEnacted }

    fun isRuleEnacted(ruleId: String): Boolean =
        rules.find { it.id == ruleId }?.isEnacted ?: false

    fun effectiveConferences(franchises: List<Franchise>): List<Conference> =
        Conference.values().filter { conf ->
            conf in enabledConferences || franchises.any { it.conference == conf }
        }

    fun effectiveDivisionsFor(conference: Conference, franchises: List<Franchise>): List<Division> =
        Division.values().filter { div ->
            div.conference == conference && (div in enabledDivisions || franchises.any { it.division == div })
        }.ifEmpty {
            Division.values().filter { it.conference == conference }
        }
}

data class MarketingCampaign(
    val id: String,
    val title: String,
    val description: String,
    val costMillions: Long,
    val fanGrowthPct: Double,
    val durationWeeks: Int,
    val targetSegment: String,
    val isInternational: Boolean = false,
    val isActive: Boolean = false,
    val remainingWeeks: Int = 0
)

data class CommissionerChoice(
    val label: String,
    val narrativeOutcome: String,
    val cashImpactMillions: Long = 0L,
    val fanMoraleImpact: Int = 0,
    val unionSatisfactionImpact: Int = 0,
    val integrityImpact: Int = 0,
    val ownerApprovalImpact: Int = 0,
    val id: String = "",
    val consequenceSummary: String = "",
    val valuationImpactMillions: Long = 0L,
    val fanbaseImpactMillions: Double = 0.0,
    val parityImpact: Int = 0,
    val teamChemistryImpact: Int = 0,
    val playerMoraleImpact: Int = 0,
    val playerSuspensionGames: Int = 0,
    val officiatingAccuracyImpact: Int = 0,
    val refLockoutRiskImpact: Int = 0,
    val precedentConsistencyImpact: Int = 0,
    val branchFollowUp: CrisisBranchFollowUpSpec? = null
)

data class CommissionerDilemma(
    val id: String,
    val week: Int,
    val title: String,
    val category: String, // "Regulatory", "Labor Union", "Franchise Scandal", "Market Expansion", "Rules"
    val urgencyTag: String,
    val description: String,
    val choices: List<CommissionerChoice>,
    val arcId: String = id,
    val archetype: CrisisArchetype = CrisisArchetype.SPORTSBOOK_INTEGRITY,
    val stageNumber: Int = 1,
    val totalStages: Int = 1,
    val nodeId: String = id,
    val targetFranchiseId: String? = null,
    val targetFranchiseName: String? = null,
    val targetPlayerId: String? = null,
    val targetPlayerName: String? = null,
    val priorDecisionLabel: String? = null,
    val isBranchFollowUp: Boolean = false
)

data class GameMatch(
    val id: String,
    val week: Int,
    val homeTeamId: String,
    val awayTeamId: String,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val isPlayed: Boolean = false,
    val highlightStory: String = "",
    val roundName: String = "",
    val rivalryId: String? = null,
    val rivalryTitle: String? = null,
    val rivalryIntensity: Int = 0,
    val ticketRevenueMultiplier: Double = 1.0,
    val moraleMultiplier: Double = 1.0,
    val rivalryBonusTicketMillions: Double = 0.0,
    val ruleImpactNote: String = "",
    val holidayTentpoleLabel: String? = null,
    val isHolidayMarquee: Boolean = false,
    val internationalVenueLabel: String? = null,
    val isInternationalShowcase: Boolean = false,
    val scheduleBroadcastBonusMillions: Double = 0.0,
    val scheduleGlobalReachGainMillions: Double = 0.0
)

fun calculatePlayoffSeeds(franchises: List<Franchise>, rules: PlayoffRules): List<PlayoffSeedInfo> {
    if (franchises.isEmpty()) return emptyList()

    val recordComparator = compareByDescending<Franchise> { it.winPct }
        .thenByDescending { it.pointsFor - it.pointsAgainst }
        .thenByDescending { it.pointsFor }
        .thenByDescending { it.rosterRating }

    val divisionLeaders = franchises
        .groupBy { it.division }
        .mapNotNull { (_, teams) -> teams.maxWithOrNull(recordComparator) }
        .sortedWith(recordComparator)
    val divisionLeaderIds = divisionLeaders.map { it.id }.toSet()

    val activeConferences = Conference.values().filter { conf -> franchises.any { it.conference == conf } }
    val conferenceChampions = activeConferences.mapNotNull { conf ->
        franchises.filter { it.conference == conf }.maxWithOrNull(recordComparator)
    }.sortedWith(recordComparator)
    val conferenceChampIds = conferenceChampions.map { it.id }.toSet()

    val qualifiers = mutableListOf<Franchise>()
    val effectiveFieldSize = rules.fieldSize.coerceIn(2.coerceAtMost(franchises.size), franchises.size)

    when (rules.qualificationMode) {
        PlayoffQualificationMode.CONFERENCE_SPLIT -> {
            val confCount = activeConferences.size.coerceAtLeast(1)
            val baseQuota = effectiveFieldSize / confCount
            val remainder = effectiveFieldSize % confCount

            // Rank conferences for remainder distribution by conference's top contender record
            val confsRanked = activeConferences.sortedWith(
                compareByDescending<Conference> { conf ->
                    franchises.filter { it.conference == conf }.maxWithOrNull(recordComparator)?.winPct ?: 0.0
                }.thenBy { it.ordinal }
            )

            val quotaMap = mutableMapOf<Conference, Int>()
            confsRanked.forEachIndexed { idx, conf ->
                quotaMap[conf] = baseQuota + if (idx < remainder) 1 else 0
            }

            activeConferences.forEach { conf ->
                val confTarget = quotaMap[conf] ?: 0
                if (confTarget > 0) {
                    val confTeams = franchises.filter { it.conference == conf }.sortedWith(recordComparator)
                    val confDivLeaders = confTeams.filter { it.id in divisionLeaderIds }
                    val confQualifiers = mutableListOf<Franchise>()
                    if (rules.divisionWinnersAutoBerth) {
                        confQualifiers.addAll(confDivLeaders.take(confTarget))
                    }
                    val remainingConf = confTeams.filter { it !in confQualifiers }
                    val confNeeded = confTarget - confQualifiers.size
                    if (confNeeded > 0) {
                        confQualifiers.addAll(remainingConf.take(confNeeded))
                    }
                    qualifiers.addAll(confQualifiers)
                }
            }

            // Fill any unfilled berths (e.g. if a continental conference only had 1 team but quota was 2) globally
            val globalNeeded = effectiveFieldSize - qualifiers.size
            if (globalNeeded > 0) {
                val leftover = franchises.filter { it !in qualifiers }.sortedWith(recordComparator)
                qualifiers.addAll(leftover.take(globalNeeded))
            }
        }
        PlayoffQualificationMode.CONFERENCE_CHAMPIONS_WILD_CARD -> {
            // Every active conference champion earns an automatic berth
            qualifiers.addAll(conferenceChampions.take(effectiveFieldSize))
            if (rules.divisionWinnersAutoBerth && qualifiers.size < effectiveFieldSize) {
                val otherDivLeaders = divisionLeaders.filter { it !in qualifiers }
                qualifiers.addAll(otherDivLeaders.take(effectiveFieldSize - qualifiers.size))
            }
            val needed = effectiveFieldSize - qualifiers.size
            if (needed > 0) {
                val wildCardPool = franchises.filter { it !in qualifiers }.sortedWith(recordComparator)
                qualifiers.addAll(wildCardPool.take(needed))
            }
        }
        PlayoffQualificationMode.DIVISION_CHAMPIONS_SHOWCASE -> {
            qualifiers.addAll(divisionLeaders.take(effectiveFieldSize))
            val needed = effectiveFieldSize - qualifiers.size
            if (needed > 0) {
                val wildCardPool = franchises.filter { it !in qualifiers }.sortedWith(recordComparator)
                qualifiers.addAll(wildCardPool.take(needed))
            }
        }
        PlayoffQualificationMode.OVERALL_LEAGUE_TABLE -> {
            if (rules.divisionWinnersAutoBerth) {
                qualifiers.addAll(divisionLeaders.take(effectiveFieldSize))
            }
            val remaining = franchises.filter { it !in qualifiers }.sortedWith(recordComparator)
            val needed = effectiveFieldSize - qualifiers.size
            if (needed > 0) {
                qualifiers.addAll(remaining.take(needed))
            }
        }
    }

    val uniqueQualifiers = qualifiers.distinctBy { it.id }.take(effectiveFieldSize)

    val sortedQualifiers: List<Franchise> = when (rules.seedingMethod) {
        PlayoffSeedingMethod.DIVISION_WINNERS_FIRST -> {
            val (champs, wildCards) = uniqueQualifiers.partition {
                it.id in divisionLeaderIds || it.id in conferenceChampIds
            }
            champs.sortedWith(recordComparator) + wildCards.sortedWith(recordComparator)
        }
        PlayoffSeedingMethod.PURE_WIN_PERCENTAGE -> {
            uniqueQualifiers.sortedWith(recordComparator)
        }
        PlayoffSeedingMethod.CONFERENCE_PROTECTED -> {
            val buckets = activeConferences.mapNotNull { conf ->
                val list = uniqueQualifiers.filter { it.conference == conf }.sortedWith(recordComparator)
                if (list.isEmpty()) null else conf to list
            }.sortedWith(
                compareByDescending<Pair<Conference, List<Franchise>>> { it.second.first().winPct }
                    .thenBy { it.first.ordinal }
            )
            val interleaved = mutableListOf<Franchise>()
            val maxPerBucket = buckets.maxOfOrNull { it.second.size } ?: 0
            for (round in 0 until maxPerBucket) {
                buckets.forEach { (_, teamsInConf) ->
                    if (round < teamsInConf.size) {
                        interleaved.add(teamsInConf[round])
                    }
                }
            }
            interleaved
        }
    }

    val maxByes = rules.firstRoundByesCount.coerceIn(0, effectiveFieldSize / 2)
    return sortedQualifiers.take(effectiveFieldSize).mapIndexed { index, team ->
        val seedNumber = index + 1
        val isConfChamp = team.id in conferenceChampIds
        val isDivChamp = team.id in divisionLeaderIds
        val hasBye = seedNumber <= maxByes

        val tag = when {
            hasBye -> "1ST ROUND BYE"
            isConfChamp && activeConferences.size > 2 -> "CONF CHAMP"
            isDivChamp -> "DIV CHAMPION"
            else -> "WILD CARD"
        }

        val desc = when {
            hasBye && isConfChamp && activeConferences.size > 2 ->
                "${team.conference.displayName} Champion • #$seedNumber Seed (First-Round Bye)"
            hasBye && isDivChamp ->
                "Division Winner • #$seedNumber Seed (First-Round Bye)"
            hasBye ->
                "Top Qualifier • #$seedNumber Seed (First-Round Bye)"
            isConfChamp && activeConferences.size > 2 ->
                "${team.conference.displayName} Champion • Seed #$seedNumber"
            isDivChamp ->
                "${team.division.displayName} Champion • Seed #$seedNumber"
            else ->
                "Wild Card Berth • Seed #$seedNumber (${team.conference.displayName})"
        }

        PlayoffSeedInfo(
            seed = seedNumber,
            franchise = team,
            eligibilityTag = tag,
            eligibilityDescription = desc,
            hasBye = hasBye,
            isDivisionChampion = isDivChamp,
            isWildCard = !isDivChamp
        )
    }
}

enum class SeasonPhase(val title: String) {
    PRE_SEASON("Pre-Season & Recruitment"),
    REGULAR_SEASON("Regular Season Championship"),
    ALL_STAR_INTERNATIONAL("All-Star & Global Showcase"),
    PLAYOFFS("Championship Playoffs"),
    OFF_SEASON("Off-Season Summit & Relocations")
}

data class LeagueValuationHistory(
    val year: Int,
    val valuationMillions: Long,
    val totalRevenueMillions: Long,
    val championName: String
)

data class FinancialSnapshot(
    val periodLabel: String,
    val weekNumber: Int,
    val broadcastRevenue: Float,
    val sponsorshipRevenue: Float,
    val expansionFeeRevenue: Float,
    val luxuryTaxRevenue: Float,
    val totalRevenue: Float,
    val operationsExpense: Float,
    val marketingExpense: Float,
    val welfareAndGrantsExpense: Float,
    val totalExpenses: Float,
    val netCashFlow: Float,
    val cashReserves: Float,
    val valuation: Float
)

data class FranchiseFinancialStatement(
    val franchiseId: String,
    val franchiseName: String,
    val ticketRevenueMillions: Double,
    val merchandiseRevenueMillions: Double,
    val broadcastRevenueMillions: Double,
    val localSponsorRevenueMillions: Double,
    val totalRevenueMillions: Double,
    val payrollExpenseMillions: Double,
    val operationsExpenseMillions: Double,
    val totalExpensesMillions: Double,
    val netOperatingIncomeMillions: Double,
    val profitMarginPct: Double,
    val avgAttendance: Int,
    val attendancePct: Double,
    val ticketPrice: Int,
    val topSellingPlayer: String,
    val topSellingProduct: String,
    val rsnPartner: String,
    val rivalryBonusMillions: Double = 0.0,
    val fanLoyalty: Int = 75,
    val loyaltyTierTitle: String = "Devoted",
    val loyaltyRevenueImpactMillions: Double = 0.0,
    val loyaltyMerchMultiplier: Double = 1.0,
    val loyaltySponsorMultiplier: Double = 1.0,
    val luxurySuiteRevenueMillions: Double = 0.0,
    val franchiseValuationMillions: Long = 0L
)

fun Franchise.calculateFinancialStatement(activeBroadcast: BroadcastContract, totalTeams: Int): FranchiseFinancialStatement {
    val attendanceRate = FanLoyaltyEngine.computeAttendanceRate(this)
    val avgAttendance = (stadiumCapacity * attendanceRate).toInt()
    val homeGames = 8
    val baseTicketRevenue = (avgAttendance * ticketPrice * homeGames) / 1_000_000.0
    val ticketRevenue = baseTicketRevenue + rivalryTicketBonusMillions

    val merchMultiplier = if (championships > 0) 1.25 else 1.0
    val loyaltyMerchMult = FanLoyaltyEngine.merchandiseLoyaltyMultiplier(fanLoyalty)
    val merchandiseRevenue = (fanBaseMillions * 8.5 * (fanMorale / 100.0) * (rosterRating / 75.0) * merchMultiplier * loyaltyMerchMult)
        .coerceAtLeast(4.0)

    val nationalBroadcastCut = (activeBroadcast.annualPayoutMillions.toDouble() / totalTeams.coerceAtLeast(1))
    val rsnCut = (localSponsorRevenueMillions * 0.55).coerceAtLeast(8.0)
    val broadcastRevenue = nationalBroadcastCut + rsnCut

    val loyaltySponsorMult = FanLoyaltyEngine.sponsorLoyaltyMultiplier(fanLoyalty)
    val suitePremium = facilities.annualSuitePremiumMillions
    val localSponsor = (localSponsorRevenueMillions.toDouble() + suitePremium) * loyaltySponsorMult
    val totalRevenue = ticketRevenue + merchandiseRevenue + broadcastRevenue + localSponsor

    // Baseline comparison against neutral 65 loyalty so UI & analytics quantify loyalty revenue impact
    val baseAttRate65 = FanLoyaltyEngine.computeAttendanceRate(this, loyaltyOverride = 65)
    val baseTicket65 = ((stadiumCapacity * baseAttRate65).toInt() * ticketPrice * homeGames) / 1_000_000.0 + rivalryTicketBonusMillions
    val baseMerch65 = (fanBaseMillions * 8.5 * (fanMorale / 100.0) * (rosterRating / 75.0) * merchMultiplier * FanLoyaltyEngine.merchandiseLoyaltyMultiplier(65)).coerceAtLeast(4.0)
    val baseSponsor65 = (localSponsorRevenueMillions.toDouble() + suitePremium) * FanLoyaltyEngine.sponsorLoyaltyMultiplier(65)
    val baselineTotalRevenue = baseTicket65 + baseMerch65 + broadcastRevenue + baseSponsor65
    val loyaltyImpact = totalRevenue - baselineTotalRevenue

    val payroll = annualPayrollMillions.toDouble()
    val opsExpense = 12.0 + (stadiumCapacity * 0.00035 * homeGames)
    val totalExpenses = payroll + opsExpense
    val netIncome = totalRevenue - totalExpenses
    val margin = if (totalRevenue > 0) (netIncome / totalRevenue) * 100.0 else 0.0
    val valuationBreakdown = FacilityEngine.calculateValuationFromRevenue(this, totalRevenue)

    return FranchiseFinancialStatement(
        franchiseId = id,
        franchiseName = teamFullName,
        ticketRevenueMillions = Math.round(ticketRevenue * 10.0) / 10.0,
        merchandiseRevenueMillions = Math.round(merchandiseRevenue * 10.0) / 10.0,
        broadcastRevenueMillions = Math.round(broadcastRevenue * 10.0) / 10.0,
        localSponsorRevenueMillions = Math.round(localSponsor * 10.0) / 10.0,
        totalRevenueMillions = Math.round(totalRevenue * 10.0) / 10.0,
        payrollExpenseMillions = Math.round(payroll * 10.0) / 10.0,
        operationsExpenseMillions = Math.round(opsExpense * 10.0) / 10.0,
        totalExpensesMillions = Math.round(totalExpenses * 10.0) / 10.0,
        netOperatingIncomeMillions = Math.round(netIncome * 10.0) / 10.0,
        profitMarginPct = Math.round(margin * 10.0) / 10.0,
        avgAttendance = avgAttendance,
        attendancePct = Math.round(attendanceRate * 1000.0) / 10.0,
        ticketPrice = ticketPrice,
        topSellingPlayer = "${nickname} Star Jersey #${(Math.abs(id.hashCode()) % 89 + 10)}",
        topSellingProduct = "Official Home Replica & City Edition Kits",
        rsnPartner = "${cityName} Regional Sports Network",
        rivalryBonusMillions = Math.round(rivalryTicketBonusMillions * 10.0) / 10.0,
        fanLoyalty = fanLoyalty,
        loyaltyTierTitle = FanLoyaltyEngine.tierFor(fanLoyalty).title,
        loyaltyRevenueImpactMillions = Math.round(loyaltyImpact * 10.0) / 10.0,
        loyaltyMerchMultiplier = Math.round(loyaltyMerchMult * 100.0) / 100.0,
        loyaltySponsorMultiplier = Math.round(loyaltySponsorMult * 100.0) / 100.0,
        luxurySuiteRevenueMillions = Math.round((suitePremium * loyaltySponsorMult) * 10.0) / 10.0,
        franchiseValuationMillions = valuationBreakdown.totalValuationMillions
    )
}

enum class ExpansionDraftSourceType(val label: String) {
    EXISTING_ROSTER("Existing Roster"),
    FREE_AGENCY_POOL("Free Agency Pool")
}

data class ExpansionDraftPlayer(
    val id: String,
    val name: String,
    val position: String,
    val overallRating: Int, // 70 to 88
    val age: Int,
    val salaryMillions: Double,
    val sourceTeamId: String,
    val sourceTeamName: String,
    val archetype: String,
    val isProtected: Boolean = false,
    val isDrafted: Boolean = false,
    val keyTrait: String = "",
    val sourceType: ExpansionDraftSourceType = ExpansionDraftSourceType.EXISTING_ROSTER,
    val sourcePlayerId: String = "",
    val contractYearsRemaining: Int = 2
)

data class ExpansionDraftSession(
    val targetCity: MarketCity,
    val franchiseNickname: String,
    val brandColorHex: Long,
    val secondaryColorHex: Long = FranchiseIdentityEngine.defaultSecondaryFor(brandColorHex),
    val ownershipType: OwnershipType,
    val totalRounds: Int = 6,
    val currentRound: Int = 1,
    val availablePool: List<ExpansionDraftPlayer> = emptyList(),
    val draftedPlayers: List<ExpansionDraftPlayer> = emptyList(),
    val isCompleted: Boolean = false,
    val maxPicksPerExistingTeam: Int = 2,
    val ownerArchetype: OwnerArchetype? = null,
    val ownerFullName: String? = null
) {
    val averageOvr: Int
        get() = if (draftedPlayers.isEmpty()) 75 else draftedPlayers.map { it.overallRating }.average().toInt()

    val totalSalaryCommitted: Double
        get() = Math.round(draftedPlayers.sumOf { it.salaryMillions } * 10.0) / 10.0

    val draftedFromRostersCount: Int
        get() = draftedPlayers.count { it.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER }

    val draftedFromFreeAgencyCount: Int
        get() = draftedPlayers.count { it.sourceType == ExpansionDraftSourceType.FREE_AGENCY_POOL }

    fun draftedCountFromTeam(teamId: String): Int =
        draftedPlayers.count { it.sourceType == ExpansionDraftSourceType.EXISTING_ROSTER && it.sourceTeamId == teamId }

    fun isTeamDraftLimitReached(teamId: String): Boolean =
        draftedCountFromTeam(teamId) >= maxPicksPerExistingTeam
}

object ExpansionDraftPoolGenerator {
    private val firstNames = listOf(
        "Marcus", "Mateo", "Darius", "Jonas", "Tariq", "Kaito", "Lucas", "Liam",
        "Elijah", "Devon", "Rafael", "Hassan", "Malik", "Nikola", "Zachary", "Viktor",
        "Andre", "Cameron", "Gianni", "Zane", "Tobias", "Soren", "Christian", "Dominic"
    )
    private val lastNames = listOf(
        "Vance", "Rossi", "Sterling", "Lindqvist", "Al-Mansoor", "Tanaka", "Moretti",
        "Kowalski", "Mercer", "Sinclair", "Hawthorne", "Dubois", "Navarro", "Sloan",
        "Prescott", "Valdez", "O'Connor", "Benoit", "Novak", "Ibrahim", "Castillo"
    )

    private val archetypesBySport = mapOf(
        SportType.BASKETBALL to listOf("Sharpshooter", "Lockdown Defender", "Floor General", "Rim Protector", "Athletic Slasher", "Pick & Pop Specialist"),
        SportType.FOOTBALL to listOf("Gunslinger", "Power Rusher", "Deep Threat", "Ball Hawk", "Pocket Passer", "Zone Buster"),
        SportType.SOCCER to listOf("Target Striker", "Midfield Maestro", "Wing Wizard", "Ball-Playing CB", "Shot Stopper", "Box-to-Box Engine"),
        SportType.HOCKEY to listOf("Sniper", "Two-Way Forward", "Offensive Defenseman", "Shutdown Enforcer", "Puck-Handling Goalie")
    )

    private val traits = listOf(
        "Clutch Performer", "Locker Room Leader", "Elite Work Ethic", "Iron Man Durability",
        "High Motor", "Franchise Cornerstone", "Playoff Veteran", "Fan Favorite", "Quick Release"
    )

    fun generatePool(
        sportType: SportType,
        existingFranchises: List<Franchise>,
        freeAgentPool: List<FreeAgentPlayer> = emptyList()
    ): List<ExpansionDraftPlayer> {
        val positions = sportType.starterPositions
        val archetypes = sportType.archetypes

        val pool = mutableListOf<ExpansionDraftPlayer>()
        var playerIndex = 0

        // 1. Expose unprotected roster players from every existing franchise (protecting each team's top core starters)
        existingFranchises.forEach { team ->
            if (team.roster.isNotEmpty()) {
                val protectCount = if (team.roster.size >= 8) 4 else maxOf(1, team.roster.size / 2)
                val exposedRoster = team.roster
                    .sortedWith(
                        compareByDescending<RosterPlayer> { it.isStarter }
                            .thenByDescending { it.overallRating }
                    )
                    .drop(protectCount)

                exposedRoster.forEachIndexed { idx, rosterPlayer ->
                    playerIndex++
                    val roleLabel = if (rosterPlayer.isStarter) "Rotation Starter" else "Bench / Exposed"
                    pool.add(
                        ExpansionDraftPlayer(
                            id = "exp_roster_${team.id}_${rosterPlayer.id}",
                            name = rosterPlayer.name,
                            position = rosterPlayer.position,
                            overallRating = rosterPlayer.overallRating,
                            age = rosterPlayer.age,
                            salaryMillions = rosterPlayer.salaryMillions.toDouble().coerceAtLeast(2.0),
                            sourceTeamId = team.id,
                            sourceTeamName = team.teamFullName,
                            archetype = rosterPlayer.archetype.ifBlank { archetypes[idx % archetypes.size] },
                            isProtected = false,
                            keyTrait = "$roleLabel • ${rosterPlayer.contractYearsRemaining}y deal",
                            sourceType = ExpansionDraftSourceType.EXISTING_ROSTER,
                            sourcePlayerId = rosterPlayer.id,
                            contractYearsRemaining = rosterPlayer.contractYearsRemaining
                        )
                    )
                }
            } else {
                // Fallback for synthetic teams without populated rosters
                for (i in 0 until 3) {
                    playerIndex++
                    val fName = firstNames[(playerIndex * 7 + i) % firstNames.size]
                    val lName = lastNames[(playerIndex * 11 + i) % lastNames.size]
                    val pos = positions[(i + team.id.hashCode().let { if (it < 0) -it else it }) % positions.size]
                    val ovr = when (i) {
                        0 -> 80 + (playerIndex % 6)
                        1 -> 76 + (playerIndex % 5)
                        else -> 72 + (playerIndex % 5)
                    }
                    val age = when (i) {
                        0 -> 28 + (playerIndex % 4)
                        1 -> 25 + (playerIndex % 4)
                        else -> 21 + (playerIndex % 3)
                    }
                    val sal = Math.round((ovr * 0.16 - 4.5 + (i * 1.5)) * 10.0) / 10.0
                    pool.add(
                        ExpansionDraftPlayer(
                            id = "draft_player_${team.id}_$i",
                            name = "$fName $lName",
                            position = pos,
                            overallRating = ovr,
                            age = age,
                            salaryMillions = sal.coerceAtLeast(2.5),
                            sourceTeamId = team.id,
                            sourceTeamName = team.teamFullName,
                            archetype = archetypes[(i + playerIndex) % archetypes.size],
                            keyTrait = traits[(i * 3 + playerIndex) % traits.size],
                            sourceType = ExpansionDraftSourceType.EXISTING_ROSTER,
                            sourcePlayerId = "draft_player_${team.id}_$i",
                            contractYearsRemaining = 2
                        )
                    )
                }
            }
        }

        // 2. Pull live Free Agents directly from the Free Agency Pool
        val effectiveFreeAgents = if (freeAgentPool.isNotEmpty()) {
            freeAgentPool
        } else {
            FreeAgencyEngine.generateInitialFreeAgentPool(sportType, 2026)
        }
        effectiveFreeAgents.forEach { fa ->
            pool.add(
                ExpansionDraftPlayer(
                    id = "exp_fa_${fa.id}",
                    name = fa.name,
                    position = fa.position,
                    overallRating = fa.overallRating,
                    age = fa.age,
                    salaryMillions = fa.askingSalaryMillions.toDouble().coerceAtLeast(2.0),
                    sourceTeamId = "free_agency_pool",
                    sourceTeamName = if (fa.formerFranchiseName.isNotBlank()) {
                        "FA • ex-${fa.formerFranchiseName}"
                    } else {
                        "Free Agency Pool"
                    },
                    archetype = fa.archetype,
                    isProtected = false,
                    keyTrait = fa.bioTag.ifBlank { "${fa.experienceYears}y Vet • ${fa.preferredYears}y Ask" },
                    sourceType = ExpansionDraftSourceType.FREE_AGENCY_POOL,
                    sourcePlayerId = fa.id,
                    contractYearsRemaining = fa.preferredYears
                )
            )
        }

        return pool.sortedByDescending { it.overallRating }
    }
}

enum class NewsCategory(
    val displayName: String,
    val badgeColorHex: Long
) {
    EXPANSION_DRAFT("Expansion & Draft", 0xFF06B6D4), // Cyan
    BROADCAST_DEAL("Broadcast Deals", 0xFF3B82F6), // Royal Blue
    LABOR_UNION("Labor & Union CBA", 0xFFEF4444), // Crimson
    RULES_GOVERNANCE("Rules & Bylaws", 0xFFF59E0B), // Amber
    FINANCIALS("Franchise Revenue", 0xFF10B981), // Emerald
    FREE_AGENCY("Free Agency & Signings", 0xFF8B5CF6), // Violet
    COMPETITION("League Action", 0xFFD4AF37) // Gold
}

enum class NewsImpact(val label: String, val badgeColorHex: Long) {
    BREAKING("BREAKING", 0xFFEF4444),
    CRITICAL("CRITICAL", 0xFFF59E0B),
    MAJOR("MAJOR REPORT", 0xFF3B82F6),
    UPDATE("LEAGUE WIRE", 0xFF10B981)
}

enum class NewsActionType {
    NONE,
    NAVIGATE_EXPANSION,
    NAVIGATE_BROADCAST,
    NAVIGATE_LABOR,
    NAVIGATE_RULES,
    NAVIGATE_FINANCES,
    NAVIGATE_FREE_AGENCY
}

data class LeagueNewsItem(
    val id: String,
    val headline: String,
    val summary: String,
    val category: NewsCategory,
    val impact: NewsImpact,
    val timestampLabel: String,
    val sourceAgency: String = "Apex Commissioner Wire",
    val relatedEntity: String? = null,
    val actionType: NewsActionType = NewsActionType.NONE
)

object LeagueNewsParser {
    fun parseFromRawString(rawHeadline: String, week: Int, year: Int): LeagueNewsItem {
        val upper = rawHeadline.uppercase()
        val timeLabel = if (week > 0) "Season $year • Week $week" else "Season $year • Off-Season"
        val id = "news_${Math.abs(rawHeadline.hashCode())}_$week"

        return when {
            upper.contains("FREE AGENCY") || upper.contains("FREE AGENT") || upper.contains("WAIVER WIRE") ||
                (upper.contains("SIGN") && (upper.contains("CONTRACT") || upper.contains("EXCEPTION") || upper.contains("DEPTH") || upper.contains("OVR"))) -> {
                LeagueNewsItem(
                    id = id,
                    headline = rawHeadline,
                    summary = "Official league transaction wire: free agent contract terms, salary cap compliance, and depth chart slot adjustments verified.",
                    category = NewsCategory.FREE_AGENCY,
                    impact = if (upper.contains("MAX") || upper.contains("MARQUEE") || upper.contains("MULTI-YEAR")) NewsImpact.BREAKING else NewsImpact.MAJOR,
                    timestampLabel = timeLabel,
                    sourceAgency = "League Transactions & Cap Desk",
                    actionType = NewsActionType.NAVIGATE_FREE_AGENCY
                )
            }
            upper.contains("EXPANSION") || upper.contains("DRAFT") -> {
                LeagueNewsItem(
                    id = id,
                    headline = rawHeadline,
                    summary = "League expansion committee confirms new franchise awards, expansion draft acquisitions, and market capitalization growth.",
                    category = NewsCategory.EXPANSION_DRAFT,
                    impact = if (upper.contains("HISTORIC") || upper.contains("WELCOME")) NewsImpact.BREAKING else NewsImpact.MAJOR,
                    timestampLabel = timeLabel,
                    sourceAgency = "Expansion Committee Bureau",
                    actionType = NewsActionType.NAVIGATE_EXPANSION
                )
            }
            upper.contains("BROADCAST") || upper.contains("MEDIA") || upper.contains("TV") || upper.contains("STREAMER") || upper.contains("RIGHTS") -> {
                LeagueNewsItem(
                    id = id,
                    headline = rawHeadline,
                    summary = "National and global television rights package updates dictate substantial revenue distributions across member franchises.",
                    category = NewsCategory.BROADCAST_DEAL,
                    impact = if (upper.contains("SIGNED") || upper.contains("INKED")) NewsImpact.BREAKING else NewsImpact.MAJOR,
                    timestampLabel = timeLabel,
                    sourceAgency = "Broadcast Rights Syndicate",
                    actionType = NewsActionType.NAVIGATE_BROADCAST
                )
            }
            upper.contains("LABOR") || upper.contains("CBA") || upper.contains("STRIKE") || upper.contains("UNION") || upper.contains("LOCKOUT") || upper.contains("WELFARE") -> {
                LeagueNewsItem(
                    id = id,
                    headline = rawHeadline,
                    summary = "Collective bargaining negotiations between team owners and the Player Representatives Council impact season schedule stability and salary cap parameters.",
                    category = NewsCategory.LABOR_UNION,
                    impact = if (upper.contains("STRIKE") || upper.contains("WARNING") || upper.contains("LOCKOUT")) NewsImpact.CRITICAL else NewsImpact.MAJOR,
                    timestampLabel = timeLabel,
                    sourceAgency = "Labor Relations Council",
                    actionType = NewsActionType.NAVIGATE_LABOR
                )
            }
            upper.contains("BYLAW") || upper.contains("OVERTIME") || upper.contains("ROSTER") || upper.contains("SALARY CAP") || upper.contains("GOVERNANCE") || upper.contains("CODEX") || upper.contains("RULES") -> {
                LeagueNewsItem(
                    id = id,
                    headline = rawHeadline,
                    summary = "Commissioner bylaws recalibrate competitive balance, extra-time resolutions, active roster capacity limits, and salary cap enforcement.",
                    category = NewsCategory.RULES_GOVERNANCE,
                    impact = NewsImpact.MAJOR,
                    timestampLabel = timeLabel,
                    sourceAgency = "League Competition Committee",
                    actionType = NewsActionType.NAVIGATE_RULES
                )
            }
            upper.contains("TICKET") || upper.contains("REVENUE") || upper.contains("PAYROLL") || upper.contains("COMMERCIAL") || upper.contains("FINANCIAL") || upper.contains("VALUATION") -> {
                LeagueNewsItem(
                    id = id,
                    headline = rawHeadline,
                    summary = "Commercial performance indicators including ticket gate receipts, merchandise sales, and corporate sponsorships reflect franchise operating health.",
                    category = NewsCategory.FINANCIALS,
                    impact = NewsImpact.UPDATE,
                    timestampLabel = timeLabel,
                    sourceAgency = "Financial Auditing Desk",
                    actionType = NewsActionType.NAVIGATE_FINANCES
                )
            }
            else -> {
                LeagueNewsItem(
                    id = id,
                    headline = rawHeadline,
                    summary = "Official dispatch from the Office of the Commissioner regarding league championship competition and operational highlights.",
                    category = NewsCategory.COMPETITION,
                    impact = if (upper.contains("CHAMPION") || upper.contains("FINALS") || upper.contains("COMMENCES")) NewsImpact.BREAKING else NewsImpact.UPDATE,
                    timestampLabel = timeLabel,
                    sourceAgency = "Apex Commissioner Wire",
                    actionType = NewsActionType.NONE
                )
            }
        }
    }
}

object LeagueNewsGenerator {
    private var generatorCounter = 0

    fun generateExpansionDraftNews(
        franchises: List<Franchise>,
        unawardedCities: List<MarketCity>,
        sportType: SportType,
        week: Int
    ): LeagueNewsItem {
        generatorCounter++
        val targetCity = unawardedCities.firstOrNull() ?: MarketCity(
            id = "expansion_metro",
            name = "Austin",
            country = "United States",
            region = "Texas Sunbelt",
            populationMillions = 2.4,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.6,
            sportsAppetite = 88,
            medianIncomeK = 86,
            isInternational = false
        )
        val candidateTeam = franchises.lastOrNull()?.teamFullName ?: "Expansion Select"
        val timeLabel = if (week > 0) "Week $week Wire" else "Off-Season Wire"

        val templates = listOf(
            Triple(
                "HISTORIC EXPANSION DRAFT: 18 marquee players claimed as ${targetCity.name} prepares inaugural franchise launch!",
                "Expansion Draft war rooms concluded late last night as protected lists were unlocked and front offices executed selection maneuvers to assemble an immediate contender.",
                NewsImpact.BREAKING
            ),
            Triple(
                "EXPANSION SCOUT REPORT: ${targetCity.name} prospective ownership group presents \$${targetCity.expansionFeeMillions}M franchise bid and state-of-the-art arena lease.",
                "League governors reviewed demographic and digital streaming indices showcasing top-quartile sports appetite among target demographics.",
                NewsImpact.MAJOR
            ),
            Triple(
                "ROSTER EXPANSION AUDIT: $candidateTeam front office praises expansion draft depth, noting balanced cap allocation and veteran leadership.",
                "Analytics department confirms roster efficiency index ranks in the top half of the league following successful expansion selections.",
                NewsImpact.UPDATE
            ),
            Triple(
                "EXPANSION DRAFT SHAKEUP: Protected lists revealed! Rival clubs scramble to retain cornerstone talent ahead of upcoming expansion draft.",
                "Franchise general managers debate protecting rising young stars versus managing luxury tax implications under expansion draft bylaws.",
                NewsImpact.CRITICAL
            )
        )

        val selected = templates[generatorCounter % templates.size]
        return LeagueNewsItem(
            id = "dyn_exp_${System.currentTimeMillis()}_$generatorCounter",
            headline = selected.first,
            summary = selected.second,
            category = NewsCategory.EXPANSION_DRAFT,
            impact = selected.third,
            timestampLabel = timeLabel,
            sourceAgency = "Expansion Bureau Wire",
            relatedEntity = targetCity.name,
            actionType = NewsActionType.NAVIGATE_EXPANSION
        )
    }

    fun generateBroadcastDealNews(
        activeBroadcast: BroadcastContract,
        bidders: List<NetworkBidder>,
        week: Int
    ): LeagueNewsItem {
        generatorCounter++
        val rivalBidder = bidders.firstOrNull { it.name != activeBroadcast.partnerName }?.name ?: "Global Sports Network"
        val timeLabel = if (week > 0) "Week $week Wire" else "Off-Season Wire"

        val templates = listOf(
            Triple(
                "BROADCAST DEAL SIGNED: Inked landmark \$${activeBroadcast.annualPayoutMillions}M/yr national media pact with ${activeBroadcast.partnerName}!",
                "The multi-year package guarantees unprecedented digital streaming distribution and prime-time weekend broadcasting across domestic and global households.",
                NewsImpact.BREAKING
            ),
            Triple(
                "MEDIA RIGHTS BIDDING WAR: $rivalBidder challenges ${activeBroadcast.partnerName} with lucrative \$${activeBroadcast.annualPayoutMillions + 25}M extension offer!",
                "Record viewership ratings have triggered intense executive competition among linear cable networks and global tech streamers vying for live game packages.",
                NewsImpact.CRITICAL
            ),
            Triple(
                "STREAMING AUDIENCE SURGE: ${activeBroadcast.platform.label} reports digital reach expanded ${activeBroadcast.reachMultiplier}x with record youth engagement.",
                "Apex League streaming telemetry confirms global viewership up 32% year-over-year, outpacing all traditional competing sports leagues.",
                NewsImpact.MAJOR
            ),
            Triple(
                "REGIONAL SYNDICATE PACT: Local affiliate networks finalize \$${activeBroadcast.annualPayoutMillions / 4}M regional broadcasting pool for member franchises.",
                "Franchise governors express optimism as local television revenues inject guaranteed operational liquidity into mid-market balance sheets.",
                NewsImpact.UPDATE
            )
        )

        val selected = templates[generatorCounter % templates.size]
        return LeagueNewsItem(
            id = "dyn_bcast_${System.currentTimeMillis()}_$generatorCounter",
            headline = selected.first,
            summary = selected.second,
            category = NewsCategory.BROADCAST_DEAL,
            impact = selected.third,
            timestampLabel = timeLabel,
            sourceAgency = "Apex Broadcast Syndicate",
            relatedEntity = activeBroadcast.partnerName,
            actionType = NewsActionType.NAVIGATE_BROADCAST
        )
    }

    fun generateLaborDisputeNews(
        unionAgreement: LaborUnionAgreement,
        week: Int
    ): LeagueNewsItem {
        generatorCounter++
        val timeLabel = if (week > 0) "Week $week Wire" else "Off-Season Wire"

        val templates = if (unionAgreement.strikeRisk >= 20) {
            listOf(
                Triple(
                    "LABOR DISPUTE WARNING: Player Union tensions flare as strike risk climbs to ${unionAgreement.strikeRisk}% over revenue split impasse!",
                    "Player representatives convened an emergency delegation demanding greater health welfare guarantees and an increase beyond the current ${unionAgreement.playerRevenueSharePct}% revenue split.",
                    NewsImpact.CRITICAL
                ),
                Triple(
                    "CBA DEADLOCK ALERT: Commissioner and Player Council schedule emergency summit to avoid threatened season stoppage.",
                    "Franchise owners hold firm on existing \$${unionAgreement.salaryCapMillions}M salary cap ceiling as union leaders authorize strike contingency procedures.",
                    NewsImpact.CRITICAL
                ),
                Triple(
                    "STRIKE THREAT DE-ESCALATION: Neutral federal mediator joins CBA table as commissioner proposes enhanced health welfare endowment.",
                    "Both negotiating parties agree to a 48-hour cooling-off period while financial analysts model revised luxury tax redistribution grants.",
                    NewsImpact.MAJOR
                )
            )
        } else {
            listOf(
                Triple(
                    "CBA ACCORD RATIFIED: Labor peace secured with ${unionAgreement.unionSatisfaction}% player approval rating and ${unionAgreement.cbaYearsRemaining} years remaining!",
                    "Comprehensive agreement establishes \$${unionAgreement.salaryCapMillions}M salary cap structure, \$${unionAgreement.playerHealthWelfareMillions}M health fund, and guaranteed private charters.",
                    NewsImpact.BREAKING
                ),
                Triple(
                    "PLAYER WELFARE EXPANSION: Union Council praises commissioner office for concussion protocols and hybrid turf standards.",
                    "Health and safety oversight committee reports zero unaddressed player grievances following implementation of latest collective bargaining bylaws.",
                    NewsImpact.UPDATE
                ),
                Triple(
                    "LABOR HARMONY PREVAILS: Strike risk drops to negligible ${unionAgreement.strikeRisk}%; franchises focus entirely on on-field competition.",
                    "Executive representatives celebrate productive collaborative partnership between ownership groups and professional player delegations.",
                    NewsImpact.UPDATE
                )
            )
        }

        val selected = templates[generatorCounter % templates.size]
        return LeagueNewsItem(
            id = "dyn_labor_${System.currentTimeMillis()}_$generatorCounter",
            headline = selected.first,
            summary = selected.second,
            category = NewsCategory.LABOR_UNION,
            impact = selected.third,
            timestampLabel = timeLabel,
            sourceAgency = "Labor Relations Desk",
            relatedEntity = "Players Association",
            actionType = NewsActionType.NAVIGATE_LABOR
        )
    }

    fun generateRulesGovernanceNews(
        ruleBook: LeagueRuleBook,
        week: Int
    ): LeagueNewsItem {
        generatorCounter++
        val timeLabel = if (week > 0) "Week $week Wire" else "Off-Season Wire"

        val templates = listOf(
            Triple(
                "COMMISSIONER BYLAW UPDATE: Overtime format codified as '${ruleBook.overtimeFormat.displayName}' (${ruleBook.overtimeFormat.shortLabel})!",
                "Competition committee approves new extra-time regulation providing +${ruleBook.overtimeFormat.scoringTempoBonus}% scoring tempo and +${ruleBook.overtimeFormat.fanAppealBonus}% spectator excitement.",
                NewsImpact.MAJOR
            ),
            Triple(
                "ROSTER SIZE DIRECTIVE: Bench limits calibrated to ${ruleBook.rosterSizeRule.displayName} to optimize competitive depth.",
                "Franchise medical directors applaud ${ruleBook.rosterSizeRule.activeSlots} active and ${ruleBook.rosterSizeRule.reserveSlots} reserve slots, delivering ${if (ruleBook.rosterSizeRule.injuryResiliencyBonus >= 0) "+${ruleBook.rosterSizeRule.injuryResiliencyBonus}%" else "${ruleBook.rosterSizeRule.injuryResiliencyBonus}%"} injury resiliency.",
                NewsImpact.UPDATE
            ),
            Triple(
                "SALARY CAP ENFORCEMENT: Financial compliance committee mandates '${ruleBook.salaryCapStrictness.displayName}'.",
                "League integrity auditors affirm parity balance rating of ${ruleBook.competitiveParityScore}/100 under current luxury tax enforcement protocols.",
                NewsImpact.MAJOR
            )
        )

        val selected = templates[generatorCounter % templates.size]
        return LeagueNewsItem(
            id = "dyn_rules_${System.currentTimeMillis()}_$generatorCounter",
            headline = selected.first,
            summary = selected.second,
            category = NewsCategory.RULES_GOVERNANCE,
            impact = selected.third,
            timestampLabel = timeLabel,
            sourceAgency = "Rules & Bylaws Registry",
            actionType = NewsActionType.NAVIGATE_RULES
        )
    }

    fun generateFinancialRevenueNews(
        franchises: List<Franchise>,
        activeBroadcast: BroadcastContract,
        week: Int
    ): LeagueNewsItem {
        generatorCounter++
        val topTeam = franchises.maxByOrNull { it.ticketPrice * it.stadiumCapacity } ?: franchises.firstOrNull()
        val teamName = topTeam?.teamFullName ?: "Metropolitan Franchise"
        val timeLabel = if (week > 0) "Week $week Wire" else "Off-Season Wire"

        val templates = listOf(
            Triple(
                "GATE RECEIPTS SURGE: $teamName sets franchise record with \$${topTeam?.ticketPrice ?: 75}/seat average ticket demand!",
                "Arena sellouts across weekend dates power robust ticket sales growth as metropolitan fans flock to championship playoff contenders.",
                NewsImpact.MAJOR
            ),
            Triple(
                "MERCHANDISE PHENOMENON: Replica jersey and apparel sales surge 28% across all official league team stores.",
                "E-commerce telemetry highlights massive international demand for star player merchandise in global metropolitan hubs.",
                NewsImpact.UPDATE
            ),
            Triple(
                "FINANCIAL PARITY REPORT: Central revenue sharing distributes record dividends to small-market franchises.",
                "Broadcast rights pools and luxury tax penalty collections ensure sustainable operating margins across all member clubs.",
                NewsImpact.UPDATE
            )
        )

        val selected = templates[generatorCounter % templates.size]
        return LeagueNewsItem(
            id = "dyn_fin_${System.currentTimeMillis()}_$generatorCounter",
            headline = selected.first,
            summary = selected.second,
            category = NewsCategory.FINANCIALS,
            impact = selected.third,
            timestampLabel = timeLabel,
            sourceAgency = "Franchise Commercial Desk",
            relatedEntity = teamName,
            actionType = NewsActionType.NAVIGATE_FINANCES
        )
    }

    fun generateFreeAgencyNews(
        franchises: List<Franchise>,
        freeAgentPool: List<FreeAgentPlayer>,
        recentSignings: List<FreeAgentSigningTransaction>,
        week: Int
    ): LeagueNewsItem {
        generatorCounter++
        val timeLabel = "Week $week • Transaction Wire"
        val latest = recentSignings.firstOrNull()
        val topAgent = freeAgentPool.maxByOrNull { it.overallRating }
        val team = franchises.firstOrNull()
        val headline = when {
            latest != null -> "FREE AGENCY SIGNING: ${latest.franchiseName} sign ${latest.playerName} (${latest.position}, ${latest.overallRating} OVR) on ${latest.contractYears}-yr, \$${latest.salaryMillions}M ${latest.contractTypeLabel}."
            topAgent != null && team != null -> "FREE AGENCY WIRE: ${team.teamFullName} & league contenders scout ${topAgent.name} (${topAgent.position}, ${topAgent.overallRating} OVR, \$${topAgent.askingSalaryMillions}M ask) to fortify depth charts."
            else -> "FREE AGENCY POOL: League front offices evaluate veteran depth additions under salary cap exceptions."
        }
        return LeagueNewsItem(
            id = "dyn_fa_${System.currentTimeMillis()}_$generatorCounter",
            headline = headline,
            summary = "Front offices continually balance salary cap exceptions, injury cover, and active depth chart needs via the league Free Agency Pool.",
            category = NewsCategory.FREE_AGENCY,
            impact = if (latest != null && latest.overallRating >= 82) NewsImpact.BREAKING else NewsImpact.MAJOR,
            timestampLabel = timeLabel,
            sourceAgency = "League Transactions & Cap Desk",
            relatedEntity = latest?.franchiseName ?: team?.teamFullName,
            actionType = NewsActionType.NAVIGATE_FREE_AGENCY
        )
    }
}

enum class OffSeasonStep(
    val stepIndex: Int,
    val title: String,
    val subtitle: String
) {
    AWARDS_SHOW(1, "Yearly Awards Show", "Stat-driven MVP, Finals MVP, DPOY & All-League Gala"),
    RETIREMENTS(2, "Player Retirements", "Veterans step down & salary cap space freed"),
    DRAFT_LOTTERY(3, "Draft Lottery Draw", "Weighted odds determine #1 draft pick"),
    FREE_AGENCY(4, "Free Agency Frenzy", "Bidding battles under salary cap strictness"),
    HALL_OF_FAME(5, "Hall of Fame Induction", "Enshrinement of retired legends after Free Agency"),
    LEAGUE_DRAFT(6, "Rookie Draft", "Post-FA scouting & collegiate-driven draft before Pre-Season"),
    SUMMARY(7, "Parity & Review", "Reconstruction audit & Pre-Season launch"),
    SCHEDULE_CALENDAR(8, "Season Schedule", "Edit matchups, holiday rivalries, global showcases & Board approval"),
    ANNUAL_REVIEW(9, "Annual Review", "Owners, Players, Fans & Media grade, Job Security & Legacy")
}

data class RetiredPlayer(
    val id: String,
    val name: String,
    val position: String,
    val teamId: String,
    val teamName: String,
    val age: Int,
    val yearsActive: Int,
    val peakRating: Int,
    val finalRating: Int,
    val championships: Int,
    val legacyTier: String,
    val teamRatingPenalty: Int,
    val capFreedMillions: Long
)

data class DraftLotteryResult(
    val franchiseId: String,
    val franchiseName: String,
    val conference: Conference,
    val regularSeasonWinPct: Double,
    val preLotterySeed: Int,
    val lotteryOddsPct: Double,
    val finalDraftPick: Int,
    val spotMovement: Int,
    val pickOwnerFranchiseId: String = franchiseId,
    val pickOwnerFranchiseName: String = franchiseName,
    val isTradedPick: Boolean = false,
    val protectionOutcomeNote: String = ""
)

data class CollegiateMetrics(
    val schoolName: String,
    val conferenceName: String,
    val classYear: String,
    val gamesPlayed: Int,
    val primaryStatLabel: String,
    val primaryStatValue: Double,
    val secondaryStatLabel: String,
    val secondaryStatValue: Double,
    val tertiaryStatLabel: String,
    val tertiaryStatValue: Double,
    val efficiencyPct: Double,
    val collegiateMetricScore: Int, // 0-100 composite collegiate production rating
    val accolades: String
) {
    val formattedStatLine: String
        get() = "${String.format("%.1f", primaryStatValue)} $primaryStatLabel • " +
                "${String.format("%.1f", secondaryStatValue)} $secondaryStatLabel • " +
                "${String.format("%.1f", tertiaryStatValue)} $tertiaryStatLabel • " +
                "${String.format("%.1f", efficiencyPct)}% EFF"
}

data class ScoutingReport(
    val scoutGrade: Int, // 70-98
    val potentialGrade: String, // "A+", "A", "A-", "B+"
    val projectedOverall: Int, // 74-86 OVR
    val combineAthleticScore: Int, // 75-99
    val defensiveGrade: Int, // 70-96
    val iqAndIntangibles: Int, // 72-97
    val strengths: List<String>,
    val weaknessNote: String,
    val proComparison: String,
    val executiveSummary: String
)

data class RookieProspect(
    val id: String,
    val name: String,
    val age: Int,
    val position: String,
    val archetype: String,
    val consensusRank: Int,
    val collegiateMetrics: CollegiateMetrics,
    val scoutingReport: ScoutingReport,
    val isDrafted: Boolean = false,
    val draftedByFranchiseId: String? = null,
    val draftedPickNumber: Int? = null
) {
    fun evaluateTeamFitScore(teamNeeds: List<String>, draftSlot: Int): Double {
        val scoutingWeight = (scoutingReport.scoutGrade * 0.50) + (scoutingReport.combineAthleticScore * 0.12)
        val collegiateWeight = collegiateMetrics.collegiateMetricScore * 0.38
        val needBonus = when {
            teamNeeds.firstOrNull() == position -> when {
                draftSlot == 1 -> 1.8
                draftSlot == 2 -> 3.0
                else -> 8.5
            }
            position in teamNeeds -> when {
                draftSlot == 1 -> 1.0
                draftSlot == 2 -> 1.8
                else -> 5.0
            }
            else -> 0.0
        }
        val raw = scoutingWeight + collegiateWeight + needBonus
        return Math.round(raw * 10.0) / 10.0
    }
}

data class DraftPickRecord(
    val pickNumber: Int,
    val round: Int,
    val franchiseId: String,
    val franchiseName: String,
    val playerName: String,
    val position: String,
    val archetype: String,
    val origin: String,
    val overallRating: Int,
    val potentialGrade: String,
    val teamBoost: Int,
    val prospectId: String = "",
    val age: Int = 20,
    val scoutingGrade: Int = 85,
    val collegiateMetricScore: Int = 84,
    val collegeStatsSummary: String = "",
    val scoutingSummary: String = "",
    val selectionRationale: String = "",
    val teamNeedMet: Boolean = false,
    val draftedPlayer: RosterPlayer? = null,
    val originalFranchiseId: String = franchiseId,
    val originalFranchiseName: String = franchiseName,
    val acquiredViaTrade: Boolean = false,
    val sourcePickId: String = ""
)

data class FreeAgencySigning(
    val id: String,
    val playerName: String,
    val position: String,
    val rating: Int,
    val formerTeamName: String,
    val newTeamId: String,
    val newTeamName: String,
    val salaryMillions: Long,
    val termYears: Int,
    val signingType: String,
    val teamBoost: Int
)

data class TeamParityMovement(
    val franchiseId: String,
    val franchiseName: String,
    val startRating: Int,
    val endRating: Int,
    val netChange: Int,
    val primaryAcquisition: String,
    val primaryLoss: String,
    val grade: String
)

data class OffSeasonSession(
    val year: Int,
    val currentStep: OffSeasonStep = OffSeasonStep.AWARDS_SHOW,
    val isCompleted: Boolean = false,
    val awardsCeremony: YearlyAwardsCeremony? = null,
    val retirements: List<RetiredPlayer> = emptyList(),
    val lotteryResults: List<DraftLotteryResult> = emptyList(),
    val draftPicks: List<DraftPickRecord> = emptyList(),
    val draftProspectPool: List<RookieProspect> = emptyList(),
    val draftOrderFranchiseIds: List<String> = emptyList(),
    val draftOrderSlots: List<DraftOrderSlot> = emptyList(),
    val tradedPickAdjustments: List<String> = emptyList(),
    val currentDraftPickIndex: Int = 0,
    val isDraftCompleted: Boolean = false,
    val freeAgencySignings: List<FreeAgencySigning> = emptyList(),
    val hallOfFameInductees: List<HallOfFameInductee> = emptyList(),
    val hofClassRatified: Boolean = false,
    val initialParityScore: Int = 80,
    val finalParityScore: Int = 86,
    val teamMovements: List<TeamParityMovement> = emptyList()
)

object OffSeasonEngine {
    private val firstNames = listOf(
        "Marcus", "Donovan", "Tariq", "Elena", "Lucas", "Mateo", "Darius", "Jalen",
        "Viktor", "Zion", "Caleb", "Kobe", "Nikola", "Luka", "Sandro", "Amari",
        "Kendrick", "Devin", "Dante", "Jaden", "Malik", "Isaiah", "Giancarlo"
    )
    private val lastNames = listOf(
        "Vance", "Mercer", "Sterling", "Kowalski", "Santos", "Novak", "O'Connor",
        "Washington", "Dubois", "Alvarez", "Chen", "Katsaros", "Fontaine", "Thornton",
        "Lindqvist", "Castillo", "Moreno", "Blackwood", "Ramirez", "St. John", "Holloway"
    )

    private val collegesAndClubs = listOf(
        "Duke Blue Devils" to "ACC",
        "Kentucky Wildcats" to "SEC",
        "Real Madrid Baloncesto" to "EuroLeague U21",
        "Gonzaga Bulldogs" to "WCC",
        "UCLA Bruins" to "Big Ten",
        "Paris Basketball Academy" to "LNB Espoirs",
        "North Carolina Tar Heels" to "ACC",
        "Kansas Jayhawks" to "Big 12",
        "UConn Huskies" to "Big East",
        "Villanova Wildcats" to "Big East",
        "Arizona Wildcats" to "Big 12",
        "Michigan Wolverines" to "Big Ten",
        "Alabama Crimson Tide" to "SEC",
        "Oregon Ducks" to "Big Ten"
    )

    private val classYears = listOf("Freshman", "Sophomore", "Junior", "Senior", "Int'l Pro")

    private val strengthTags = listOf(
        "Elite three-level shot creation",
        "Switchable multi-position defense",
        "High-IQ pick-and-roll orchestration",
        "Relentless downhill rim pressure",
        "Lockdown weak-side help instincts",
        "Clutch fourth-quarter poise",
        "Fluid lateral agility & combine burst",
        "Rapid catch-and-shoot release",
        "Physical rebound & box-out anchor",
        "Precision transition vision"
    )

    private val riskNotes = listOf(
        "Needs added core strength to absorb pro physicality",
        "Jump-shot consistency dips under heavy late-clock pressure",
        "Can gamble for steals and fall out of defensive positioning",
        "Foul discipline against veteran scorers requires polish",
        "Off-hand finishing craft still developing at the rim",
        "High-usage style must adapt smoothly to structured pro sets"
    )

    private val accoladesList = listOf(
        "Consensus 1st-Team All-American • Conference POY",
        "Freshman of the Year • Final Four Most Outstanding Player",
        "National Defensive Player of the Year • 1st-Team All-Conf",
        "International U21 MVP • Tournament Scoring Leader",
        "Conference Scoring Champion • Wooden Award Finalist",
        "2nd-Team All-American • All-Tournament Team",
        "All-Conference 1st-Team • League Efficiency Leader"
    )

    fun generateDraftProspectPool(
        year: Int,
        poolSize: Int,
        sportType: SportType
    ): List<RookieProspect> {
        val positions = sportType.starterPositions
        val archetypes = sportType.archetypes
        val proComps = listOf(
            "Prime All-League Dual-Threat Lead",
            "Switchable Two-Way All-Star Wing",
            "High-Motor Championship Anchor",
            "Modern Stretch Floor Commander",
            "High-Efficiency Three-Level Scorer",
            "Defensive Cornerstone & Transition Catalyst"
        )

        val (pLabel, sLabel, tLabel) = when (sportType) {
            SportType.BASKETBALL -> Triple("PPG", "RPG", "APG")
            SportType.FOOTBALL -> Triple("YPG", "TD/G", "QBR/TKL")
            SportType.SOCCER -> Triple("G/30", "SOT/G", "KP/90")
            SportType.HOCKEY -> Triple("PTS/G", "SOG/G", "+/-")
        }

        val usedNames = mutableSetOf<String>()
        val prospects = mutableListOf<RookieProspect>()
        for (idx in 0 until poolSize) {
            var offset = 0
            var fullName: String
            do {
                val fName = firstNames[(idx * 5 + year * 3 + 1 + offset) % firstNames.size]
                val lName = lastNames[(idx * 7 + year * 2 + 3 + offset * 2) % lastNames.size]
                fullName = "$fName $lName"
                offset++
            } while (!usedNames.add(fullName) && offset < 25)

            val tierDecay = idx
            val (projOvr, potentialGrade, scoutBase) = when (tierDecay) {
                0 -> Triple(85, "A+", 96)
                1 -> Triple(83, "A+", 94)
                2 -> Triple(81, "A", 91)
                3 -> Triple(80, "A", 89)
                4 -> Triple(79, "A-", 87)
                5 -> Triple(78, "A-", 85)
                6, 7 -> Triple(77, "B+", 82 - (tierDecay - 6))
                else -> Triple((76 - ((tierDecay - 8) / 3)).coerceAtLeast(73), if (tierDecay < 10) "B+" else "B", (80 - (tierDecay - 8) * 2).coerceAtLeast(72))
            }

            val pos = positions[(idx + (year % positions.size)) % positions.size]
            val archetype = archetypes[idx % archetypes.size]
            val (school, conf) = collegesAndClubs[(idx + year) % collegesAndClubs.size]
            val cYear = classYears[(idx + year) % classYears.size]
            val age = when (cYear) {
                "Freshman" -> 19
                "Sophomore", "Int'l Pro" -> 20
                "Junior" -> 21
                else -> 22
            }

            // Collegiate metric variance: create realistic separation between high-ceiling raw scouts & hyper-productive collegians
            val prodVariance = ((idx * 3 + year) % 5) - 2 // -2..+2
            val collegeScore = (scoutBase - 1 + prodVariance).coerceIn(71, 98)
            val combineScore = (scoutBase + (((idx + 2) % 5) - 2)).coerceIn(74, 99)
            val defGrade = (scoutBase - 3 + ((idx * 2) % 6)).coerceIn(70, 96)
            val iqGrade = (scoutBase - 1 + ((idx + 1) % 5)).coerceIn(72, 97)

            val games = when (sportType) {
                SportType.BASKETBALL -> 32 + (idx % 6)
                SportType.FOOTBALL -> 12 + (idx % 3)
                SportType.SOCCER -> 28 + (idx % 8)
                SportType.HOCKEY -> 34 + (idx % 6)
            }

            val (pVal, sVal, tVal, eff) = when (sportType) {
                SportType.BASKETBALL -> {
                    val basePpg = 14.5 + ((collegeScore - 72) * 0.42) + (if (pos in listOf("PG", "SG", "SF")) 1.8 else -0.6)
                    val baseRpg = if (pos in listOf("PF", "C")) 8.4 + (collegeScore - 75) * 0.14 else 4.2 + (collegeScore - 75) * 0.08
                    val baseApg = if (pos == "PG") 6.2 + (collegeScore - 75) * 0.12 else 2.5 + (collegeScore - 75) * 0.06
                    val effVal = 51.5 + (collegeScore - 72) * 0.45
                    listOf(
                        Math.round(basePpg * 10.0) / 10.0,
                        Math.round(baseRpg * 10.0) / 10.0,
                        Math.round(baseApg * 10.0) / 10.0,
                        Math.round(effVal * 10.0) / 10.0
                    )
                }
                SportType.FOOTBALL -> {
                    val baseYpg = if (pos == "QB") 265.0 + (collegeScore - 72) * 4.2 else 82.0 + (collegeScore - 72) * 2.1
                    val baseTd = if (pos == "QB") 2.6 + (collegeScore - 75) * 0.06 else 0.9 + (collegeScore - 75) * 0.04
                    val baseTkl = if (pos in listOf("EDGE", "CB")) 5.8 + (collegeScore - 75) * 0.12 else 88.0 + (collegeScore - 75) * 0.5
                    val effVal = 58.0 + (collegeScore - 72) * 0.55
                    listOf(
                        Math.round(baseYpg * 10.0) / 10.0,
                        Math.round(baseTd * 10.0) / 10.0,
                        Math.round(baseTkl * 10.0) / 10.0,
                        Math.round(effVal * 10.0) / 10.0
                    )
                }
                SportType.SOCCER -> {
                    val baseGa = 14.0 + (collegeScore - 72) * 0.75
                    val baseSot = 2.1 + (collegeScore - 72) * 0.08
                    val baseKp = 1.8 + (collegeScore - 72) * 0.09
                    val effVal = 54.0 + (collegeScore - 72) * 0.6
                    listOf(
                        Math.round(baseGa * 10.0) / 10.0,
                        Math.round(baseSot * 10.0) / 10.0,
                        Math.round(baseKp * 10.0) / 10.0,
                        Math.round(effVal * 10.0) / 10.0
                    )
                }
                SportType.HOCKEY -> {
                    val basePts = 0.95 + (collegeScore - 72) * 0.04
                    val baseSog = 3.1 + (collegeScore - 72) * 0.09
                    val basePm = 10.0 + (collegeScore - 72) * 0.8
                    val effVal = 55.0 + (collegeScore - 72) * 0.5
                    listOf(
                        Math.round(basePts * 10.0) / 10.0,
                        Math.round(baseSog * 10.0) / 10.0,
                        Math.round(basePm * 10.0) / 10.0,
                        Math.round(effVal * 10.0) / 10.0
                    )
                }
            }

            val s1 = strengthTags[idx % strengthTags.size]
            val s2 = strengthTags[(idx + 3) % strengthTags.size]
            val s3 = strengthTags[(idx + 6) % strengthTags.size]
            val risk = riskNotes[idx % riskNotes.size]
            val comp = proComps[idx % proComps.size]
            val accolade = accoladesList[idx % accoladesList.size]

            val colMetrics = CollegiateMetrics(
                schoolName = school,
                conferenceName = conf,
                classYear = cYear,
                gamesPlayed = games,
                primaryStatLabel = pLabel,
                primaryStatValue = pVal,
                secondaryStatLabel = sLabel,
                secondaryStatValue = sVal,
                tertiaryStatLabel = tLabel,
                tertiaryStatValue = tVal,
                efficiencyPct = eff,
                collegiateMetricScore = collegeScore,
                accolades = accolade
            )

            val execSummary = "$cYear $pos out of $school ($conf). Graded $scoutBase by league scouts with $collegeScore collegiate metric score (${colMetrics.formattedStatLine})."

            val report = ScoutingReport(
                scoutGrade = scoutBase,
                potentialGrade = potentialGrade,
                projectedOverall = projOvr,
                combineAthleticScore = combineScore,
                defensiveGrade = defGrade,
                iqAndIntangibles = iqGrade,
                strengths = listOf(s1, s2, s3),
                weaknessNote = risk,
                proComparison = comp,
                executiveSummary = execSummary
            )

            prospects.add(
                RookieProspect(
                    id = "prospect_${year}_${idx + 1}",
                    name = fullName,
                    age = age,
                    position = pos,
                    archetype = archetype,
                    consensusRank = idx + 1,
                    collegiateMetrics = colMetrics,
                    scoutingReport = report
                )
            )
        }
        return prospects
    }

    fun getTeamDraftNeeds(
        team: Franchise,
        retirements: List<RetiredPlayer>,
        freeAgencySignings: List<FreeAgencySigning>,
        sportType: SportType
    ): List<String> {
        val positions = sportType.starterPositions

        val retiredPositions = retirements.filter { it.teamId == team.id }.map { it.position }.toSet()
        val faCoveredPositions = freeAgencySignings.filter { it.newTeamId == team.id }.map { it.position }.toSet()

        // Score positional weakness: missing position or lost retiree not replaced in FA or lowest-rated position
        val posStrength = positions.map { pos ->
            val maxAtPos = team.roster.filter { it.position == pos }.maxOfOrNull { it.overallRating } ?: 0
            val retiredPenalty = if (pos in retiredPositions && pos !in faCoveredPositions) -12 else 0
            val faBoost = if (pos in faCoveredPositions) 8 else 0
            val effectiveStrength = maxAtPos + retiredPenalty + faBoost
            pos to effectiveStrength
        }.sortedBy { it.second }

        return posStrength.take(2).map { it.first }
    }

    fun evaluateBestProspectForTeam(
        team: Franchise,
        availableProspects: List<RookieProspect>,
        retirements: List<RetiredPlayer>,
        freeAgencySignings: List<FreeAgencySigning>,
        sportType: SportType,
        draftSlot: Int
    ): Pair<RookieProspect, String>? {
        if (availableProspects.isEmpty()) return null
        val teamNeeds = getTeamDraftNeeds(team, retirements, freeAgencySignings, sportType)
        val best = availableProspects.maxByOrNull { it.evaluateTeamFitScore(teamNeeds, draftSlot) } ?: availableProspects.first()
        val fitScore = best.evaluateTeamFitScore(teamNeeds, draftSlot)
        val metNeed = best.position in teamNeeds
        val needPhrase = if (metNeed) "Addresses ${best.position} post-FA roster need" else "Best overall talent on board"
        val rationale = "$needPhrase • Scout Grade ${best.scoutingReport.scoutGrade}, College Score ${best.collegiateMetrics.collegiateMetricScore} (${best.collegiateMetrics.formattedStatLine}) [Fit $fitScore]"
        return best to rationale
    }

    fun conductSingleDraftPick(
        session: OffSeasonSession,
        franchises: List<Franchise>,
        sportType: SportType,
        chosenProspectId: String? = null
    ): OffSeasonSession {
        if (session.isDraftCompleted || session.currentDraftPickIndex >= session.draftOrderFranchiseIds.size) {
            return session.copy(isDraftCompleted = true)
        }

        val pickIndex = session.currentDraftPickIndex
        val pickNumber = pickIndex + 1
        val slot = session.draftOrderSlots.getOrNull(pickIndex)
        val teamId = slot?.ownerFranchiseId ?: session.draftOrderFranchiseIds[pickIndex]
        val team = franchises.find { it.id == teamId } ?: return session.copy(
            currentDraftPickIndex = pickIndex + 1,
            isDraftCompleted = pickIndex + 1 >= session.draftOrderFranchiseIds.size
        )

        val available = session.draftProspectPool.filter { !it.isDrafted }
        if (available.isEmpty()) {
            return session.copy(isDraftCompleted = true)
        }

        val round = slot?.round ?: 1
        val origId = slot?.originalFranchiseId ?: team.id
        val origName = slot?.originalFranchiseName ?: team.teamFullName
        val acquiredViaTrade = slot?.isTraded ?: false

        val teamNeeds = getTeamDraftNeeds(team, session.retirements, session.freeAgencySignings, sportType)
        val selectedPair = if (chosenProspectId != null) {
            val manual = available.find { it.id == chosenProspectId }
            if (manual != null) {
                val fitScore = manual.evaluateTeamFitScore(teamNeeds, pickNumber)
                val metNeed = manual.position in teamNeeds
                val prefix = if (metNeed) "Commissioner/Team fills ${manual.position} need" else "High-upside selection"
                manual to "$prefix • Scout Grade ${manual.scoutingReport.scoutGrade}, College Score ${manual.collegiateMetrics.collegiateMetricScore} (${manual.collegiateMetrics.formattedStatLine}) [Fit $fitScore]"
            } else {
                evaluateBestProspectForTeam(team, available, session.retirements, session.freeAgencySignings, sportType, pickNumber)
            }
        } else {
            evaluateBestProspectForTeam(team, available, session.retirements, session.freeAgencySignings, sportType, pickNumber)
        } ?: return session.copy(isDraftCompleted = true)

        val chosen = selectedPair.first
        val tradeNote = if (acquiredViaTrade) " • Traded pick via $origName" else ""
        val rationale = "${selectedPair.second}$tradeNote"
        val metNeed = chosen.position in teamNeeds

        val rookieSalary = if (round >= 2) {
            2L
        } else {
            when (pickNumber) {
                1 -> 9L
                2 -> 8L
                in 3..5 -> 6L
                else -> 4L
            }
        }
        val teamBoost = if (round >= 2) {
            if (chosen.scoutingReport.projectedOverall >= 78) 2 else 1
        } else {
            when {
                pickNumber == 1 && chosen.scoutingReport.projectedOverall >= 84 -> 6
                pickNumber <= 3 && chosen.scoutingReport.projectedOverall >= 81 -> 5
                chosen.scoutingReport.projectedOverall >= 79 -> 4
                else -> 3
            }
        }

        val rookiePlayer = RosterPlayer(
            id = "rookie_${session.year}_${chosen.id}",
            name = chosen.name,
            franchiseId = team.id,
            position = chosen.position,
            overallRating = chosen.scoutingReport.projectedOverall,
            age = chosen.age,
            salaryMillions = rookieSalary,
            contractYearsRemaining = if (round >= 2) 3 else 4,
            archetype = chosen.archetype
        )

        val newRecord = DraftPickRecord(
            pickNumber = pickNumber,
            round = round,
            franchiseId = team.id,
            franchiseName = team.teamFullName,
            playerName = chosen.name,
            position = chosen.position,
            archetype = chosen.archetype,
            origin = chosen.collegiateMetrics.schoolName,
            overallRating = chosen.scoutingReport.projectedOverall,
            potentialGrade = chosen.scoutingReport.potentialGrade,
            teamBoost = teamBoost,
            prospectId = chosen.id,
            age = chosen.age,
            scoutingGrade = chosen.scoutingReport.scoutGrade,
            collegiateMetricScore = chosen.collegiateMetrics.collegiateMetricScore,
            collegeStatsSummary = chosen.collegiateMetrics.formattedStatLine,
            scoutingSummary = chosen.scoutingReport.executiveSummary,
            selectionRationale = rationale,
            teamNeedMet = metNeed,
            draftedPlayer = rookiePlayer,
            originalFranchiseId = origId,
            originalFranchiseName = origName,
            acquiredViaTrade = acquiredViaTrade,
            sourcePickId = slot?.sourcePickId ?: ""
        )

        val updatedPool = session.draftProspectPool.map { p ->
            if (p.id == chosen.id) p.copy(
                isDrafted = true,
                draftedByFranchiseId = team.id,
                draftedPickNumber = pickNumber
            ) else p
        }
        val updatedPicks = session.draftPicks + newRecord
        val nextIndex = pickIndex + 1
        val completed = nextIndex >= session.draftOrderFranchiseIds.size

        val updatedSession = session.copy(
            draftProspectPool = updatedPool,
            draftPicks = updatedPicks,
            currentDraftPickIndex = nextIndex,
            isDraftCompleted = completed
        )

        return recalculateParityMovements(updatedSession, franchises)
    }

    fun autoCompleteDraft(
        session: OffSeasonSession,
        franchises: List<Franchise>,
        sportType: SportType
    ): OffSeasonSession {
        var current = session
        var guard = 0
        while (!current.isDraftCompleted && current.currentDraftPickIndex < current.draftOrderFranchiseIds.size && guard < 40) {
            current = conductSingleDraftPick(current, franchises, sportType, null)
            guard++
        }
        return current
    }

    fun recalculateParityMovements(
        session: OffSeasonSession,
        franchises: List<Franchise>
    ): OffSeasonSession {
        val teamMovements = mutableListOf<TeamParityMovement>()
        franchises.forEach { team ->
            val retPenalty = session.retirements.filter { it.teamId == team.id }.sumOf { it.teamRatingPenalty }
            val draftBoost = session.draftPicks.filter { it.franchiseId == team.id }.sumOf { it.teamBoost }
            val faBoost = session.freeAgencySignings.filter { it.newTeamId == team.id }.sumOf { it.teamBoost }
            val net = draftBoost + faBoost - retPenalty

            val primaryAcq = when {
                faBoost > 0 && draftBoost > 0 -> "FA ${session.freeAgencySignings.first { it.newTeamId == team.id }.playerName} + Drafted ${session.draftPicks.first { it.franchiseId == team.id }.playerName}"
                draftBoost > 0 -> "Drafted ${session.draftPicks.first { it.franchiseId == team.id }.playerName} (+$draftBoost OVR)"
                faBoost > 0 -> "Signed ${session.freeAgencySignings.first { it.newTeamId == team.id }.playerName} (+$faBoost OVR)"
                else -> "Developmental Roster Continuity"
            }

            val primaryLoss = when {
                retPenalty > 0 -> "Retired ${session.retirements.first { it.teamId == team.id }.name} (-$retPenalty OVR)"
                else -> "Minor Depth Waivers"
            }

            val grade = when {
                net >= 5 -> "A+"
                net in 3..4 -> "A"
                net in 1..2 -> "B+"
                net == 0 -> "B"
                net in -2..-1 -> "C"
                else -> "D+"
            }

            val endRating = (team.rosterRating + net).coerceIn(70, 94)

            teamMovements.add(
                TeamParityMovement(
                    franchiseId = team.id,
                    franchiseName = team.teamFullName,
                    startRating = team.rosterRating,
                    endRating = endRating,
                    netChange = net,
                    primaryAcquisition = primaryAcq,
                    primaryLoss = primaryLoss,
                    grade = grade
                )
            )
        }

        val startRatings = teamMovements.map { it.startRating }
        val endRatings = teamMovements.map { it.endRating }
        val startSpread = (startRatings.maxOrNull() ?: 80) - (startRatings.minOrNull() ?: 70)
        val endSpread = (endRatings.maxOrNull() ?: 80) - (endRatings.minOrNull() ?: 70)
        val parityGain = (startSpread - endSpread).coerceIn(2, 8)
        val calculatedFinalParity = (session.initialParityScore + parityGain).coerceIn(75, 96)

        return session.copy(
            teamMovements = teamMovements,
            finalParityScore = calculatedFinalParity
        )
    }

    data class DraftOrderResolution(
        val updatedLotteryResults: List<DraftLotteryResult>,
        val slots: List<DraftOrderSlot>,
        val tradeAdjustments: List<String>
    )

    fun resolveDraftOrderFromPicks(
        year: Int,
        sortedLottery: List<DraftLotteryResult>,
        franchises: List<Franchise>,
        includeRound2Always: Boolean = false
    ): DraftOrderResolution {
        val franchiseMap = franchises.associateBy { it.id }

        // Index all picks for this draft year held across franchise inventories
        val heldPicksByOrigAndRound = mutableMapOf<Pair<String, Int>, Pair<String, DraftPick>>()
        franchises.forEach { holder ->
            holder.draftPicks
                .filter { it.year == year }
                .forEach { pick ->
                    val key = pick.originalFranchiseId to pick.round
                    val existing = heldPicksByOrigAndRound[key]
                    // Prefer explicitly traded holder entry if duplicates ever appear
                    if (existing == null || holder.id != pick.originalFranchiseId) {
                        heldPicksByOrigAndRound[key] = holder.id to pick
                    }
                }
        }

        val slots = mutableListOf<DraftOrderSlot>()
        val adjustments = mutableListOf<String>()
        val updatedLottery = mutableListOf<DraftLotteryResult>()

        // Round 1: driven by lottery order + trade ownership + Top-3 protection check
        sortedLottery.forEach { lotteryItem ->
            val origId = lotteryItem.franchiseId
            val origName = franchiseMap[origId]?.teamFullName ?: lotteryItem.franchiseName
            val heldPair = heldPicksByOrigAndRound[origId to 1]
            val holderId = heldPair?.first ?: origId
            val pickObj = heldPair?.second

            val wasTradedAway = pickObj != null && holderId != origId && franchiseMap.containsKey(holderId)
            val protectionTriggered = wasTradedAway && pickObj!!.isProtected && lotteryItem.finalDraftPick <= 3
            val actualOwnerId = if (protectionTriggered || !wasTradedAway) origId else holderId
            val actualOwnerName = franchiseMap[actualOwnerId]?.teamFullName ?: origName
            val isSlotTraded = actualOwnerId != origId

            val note = when {
                protectionTriggered ->
                    "Top-3 Protected (#${lotteryItem.finalDraftPick}) — Retained by $origName; obligation defers to ${year + 1}"
                isSlotTraded -> {
                    val protText = if (pickObj?.isProtected == true) " • ${pickObj.protectionDescription} conveyed" else ""
                    "Acquired by $actualOwnerName from $origName$protText"
                }
                else -> ""
            }
            if (note.isNotBlank()) {
                adjustments.add("R1 #${lotteryItem.finalDraftPick}: $note")
            }

            updatedLottery.add(
                lotteryItem.copy(
                    pickOwnerFranchiseId = actualOwnerId,
                    pickOwnerFranchiseName = actualOwnerName,
                    isTradedPick = isSlotTraded,
                    protectionOutcomeNote = note
                )
            )

            slots.add(
                DraftOrderSlot(
                    pickNumber = slots.size + 1,
                    round = 1,
                    slotInRound = lotteryItem.finalDraftPick,
                    originalFranchiseId = origId,
                    originalFranchiseName = origName,
                    ownerFranchiseId = actualOwnerId,
                    ownerFranchiseName = actualOwnerName,
                    sourcePickId = pickObj?.id ?: "",
                    isTraded = isSlotTraded,
                    isProtected = pickObj?.isProtected ?: false,
                    protectionDescription = pickObj?.protectionDescription ?: "Unprotected",
                    retainedDueToProtection = protectionTriggered,
                    tradeSummaryNote = note
                )
            )
        }

        // Round 2: automatically activate whenever any Round 2 pick for this year has been traded (or explicitly requested)
        val hasTradedRound2 = heldPicksByOrigAndRound.any { (key, pair) ->
            key.second == 2 && pair.first != key.first && franchiseMap.containsKey(pair.first)
        }
        if (hasTradedRound2 || includeRound2Always) {
            sortedLottery.forEachIndexed { idx, lotteryItem ->
                val origId = lotteryItem.franchiseId
                val origName = franchiseMap[origId]?.teamFullName ?: lotteryItem.franchiseName
                val heldPair = heldPicksByOrigAndRound[origId to 2]
                val holderId = heldPair?.first ?: origId
                val pickObj = heldPair?.second
                val isSlotTraded = holderId != origId && franchiseMap.containsKey(holderId)
                val actualOwnerId = if (isSlotTraded) holderId else origId
                val actualOwnerName = franchiseMap[actualOwnerId]?.teamFullName ?: origName
                val overallPickNum = slots.size + 1
                val note = if (isSlotTraded) "Acquired by $actualOwnerName from $origName" else ""
                if (note.isNotBlank()) {
                    adjustments.add("R2 #$overallPickNum: $note")
                }

                slots.add(
                    DraftOrderSlot(
                        pickNumber = overallPickNum,
                        round = 2,
                        slotInRound = idx + 1,
                        originalFranchiseId = origId,
                        originalFranchiseName = origName,
                        ownerFranchiseId = actualOwnerId,
                        ownerFranchiseName = actualOwnerName,
                        sourcePickId = pickObj?.id ?: "",
                        isTraded = isSlotTraded,
                        isProtected = false,
                        protectionDescription = "Unprotected",
                        retainedDueToProtection = false,
                        tradeSummaryNote = note
                    )
                )
            }
        }

        return DraftOrderResolution(
            updatedLotteryResults = updatedLottery,
            slots = slots,
            tradeAdjustments = adjustments
        )
    }

    fun createOffSeasonSession(
        year: Int,
        franchises: List<Franchise>,
        sportType: SportType,
        ruleBook: LeagueRuleBook,
        initialParity: Int,
        careerStats: Map<String, PlayerCareerStats> = emptyMap(),
        seasonStats: Map<String, PlayerSeasonStats> = emptyMap(),
        weeklyAwards: List<WeeklyPlayerAward> = emptyList(),
        champion: Franchise? = null,
        autoCompleteDraft: Boolean = true,
        includeRound2Always: Boolean = false
    ): OffSeasonSession {
        val positions = sportType.starterPositions

        // 1. Generate Retirements
        val retirements = mutableListOf<RetiredPlayer>()
        val sortedByRatingDesc = franchises.sortedByDescending { it.rosterRating }
        sortedByRatingDesc.take(4).forEachIndexed { idx, team ->
            val pName = "${firstNames[(idx * 7 + year) % firstNames.size]} ${lastNames[(idx * 11 + year) % lastNames.size]}"
            val age = 35 + (idx % 4)
            val peak = 85 + (idx % 5)
            val fin = 75 + (idx % 3)
            val champs = team.championships.coerceAtLeast(idx % 3)
            val penalty = if (idx == 0) 3 else 2
            val capFreed = 14L + (idx * 3L)
            val legacy = when {
                champs >= 2 || peak >= 88 -> "Hall of Fame First Ballot"
                peak >= 85 -> "Ring of Honor Legend"
                else -> "Franchise Cornerstone"
            }

            retirements.add(
                RetiredPlayer(
                    id = "retire_${team.id}_$year",
                    name = pName,
                    position = positions[(idx + 2) % positions.size],
                    teamId = team.id,
                    teamName = team.teamFullName,
                    age = age,
                    yearsActive = 12 + (idx % 5),
                    peakRating = peak,
                    finalRating = fin,
                    championships = champs,
                    legacyTier = legacy,
                    teamRatingPenalty = penalty,
                    capFreedMillions = capFreed
                )
            )
        }

        // 2. Generate Draft Lottery
        val sortedByWinAsc = franchises.sortedWith(
            compareBy<Franchise> { it.winPct }
                .thenBy { it.pointsFor - it.pointsAgainst }
                .thenBy { it.rosterRating }
        )

        val oddsList = listOf(24.0, 18.0, 14.0, 12.0, 10.0, 8.0, 6.0, 4.0, 2.0, 2.0)

        // Simple weighted lottery permutation: top 3 can shift, remainder fall in reverse standings
        val seedOrder = sortedByWinAsc.indices.toList()
        val lotteryPicksOrder = mutableListOf<Int>()
        val jumpIndex = if (seedOrder.size >= 3) (year % 3) else 0
        lotteryPicksOrder.add(seedOrder[jumpIndex])
        seedOrder.filter { it != seedOrder[jumpIndex] }.forEach { lotteryPicksOrder.add(it) }

        val lotteryResults = mutableListOf<DraftLotteryResult>()
        sortedByWinAsc.forEachIndexed { seedIdx, team ->
            val finalPickIndex = lotteryPicksOrder.indexOf(seedIdx)
            val pickNum = finalPickIndex + 1
            val preSeed = seedIdx + 1
            val odds = oddsList.getOrElse(seedIdx) { 2.0 }
            val movement = preSeed - pickNum

            lotteryResults.add(
                DraftLotteryResult(
                    franchiseId = team.id,
                    franchiseName = team.teamFullName,
                    conference = team.conference,
                    regularSeasonWinPct = team.winPct,
                    preLotterySeed = preSeed,
                    lotteryOddsPct = odds,
                    finalDraftPick = pickNum,
                    spotMovement = movement
                )
            )
        }
        val sortedLottery = lotteryResults.sortedBy { it.finalDraftPick }

        // 3. Generate Free Agency Signings (Occurs BEFORE Rookie Draft & Hall of Fame)
        val freeAgencySignings = mutableListOf<FreeAgencySigning>()
        val faNames = listOf(
            "Christian Thorne", "Julian Vance", "Darius Sterling", "Amir Kowalski",
            "Sandro Dubois", "Kenji Takahashi"
        )
        val faTypes = listOf("Max Contract Signing", "Cap Space Marquee Acquisition", "Mid-Level Exception Pact", "Hometown Re-signing")

        val sortedTeamsForFA = sortedLottery.mapNotNull { l -> franchises.find { it.id == l.franchiseId } }
        val faCount = minOf(4, sortedTeamsForFA.size)

        for (i in 0 until faCount) {
            val targetTeam = sortedTeamsForFA[i]
            val formerTeam = franchises[(i + 3) % franchises.size]
            val name = faNames[i % faNames.size]
            val pos = positions[(i + 1) % positions.size]
            val rating = 82 + (i % 4)
            val salary = 18L + (i * 4L)
            val boost = 3 + (i % 2)

            freeAgencySignings.add(
                FreeAgencySigning(
                    id = "fa_${targetTeam.id}_$year",
                    playerName = name,
                    position = pos,
                    rating = rating,
                    formerTeamName = formerTeam.teamFullName,
                    newTeamId = targetTeam.id,
                    newTeamName = targetTeam.teamFullName,
                    salaryMillions = salary,
                    termYears = 3 + (i % 2),
                    signingType = faTypes[i % faTypes.size],
                    teamBoost = boost
                )
            )
        }

        // 4. Generate Hall of Fame Induction Class (Occurs after Free Agency)
        val hofCandidates = retirements
            .filter { it.legacyTier.contains("Hall of Fame", ignoreCase = true) || it.peakRating >= 86 || it.championships >= 1 }
            .ifEmpty { retirements.take(1) }
            .take(2)

        val hofQuotes = listOf(
            "Immortalized forever among the titans who defined the modern era of ${sportType.displayName}.",
            "Every banner and sold-out arena night remains a testament to relentless excellence.",
            "From rookie ambition to championship glory—this honor belongs to teammates and loyal fans."
        )

        val hallOfFameInductees = hofCandidates.mapIndexed { idx, ret ->
            val matchedCareer = careerStats.values.find { it.playerName.equals(ret.name, ignoreCase = true) }
            val games = matchedCareer?.gamesPlayed ?: (ret.yearsActive * 14)
            val ptsPerGame = when (sportType) {
                SportType.BASKETBALL -> 19.5 + (ret.peakRating - 84).coerceAtLeast(0) * 1.1
                SportType.FOOTBALL -> 10.5
                SportType.SOCCER -> 1.1
                SportType.HOCKEY -> 1.3
            }
            val careerPts = matchedCareer?.totalPoints ?: (games * ptsPerGame).toInt()
            val careerReb = matchedCareer?.totalRebounds ?: (games * 6.4).toInt()
            val careerAst = matchedCareer?.totalAssists ?: (games * 5.2).toInt()
            val honors = matchedCareer?.weeklyHonorsCount ?: (2 + ret.championships * 2)

            HallOfFameInductee(
                id = "hof_${year}_${ret.id}",
                playerId = ret.id,
                name = ret.name,
                inductionYear = year,
                position = ret.position,
                primaryFranchiseId = ret.teamId,
                primaryFranchiseName = ret.teamName,
                careerPoints = careerPts,
                careerRebounds = careerReb,
                careerAssists = careerAst,
                gamesPlayed = games,
                championships = ret.championships,
                weeklyHonors = honors,
                peakRating = ret.peakRating,
                legacyTier = ret.legacyTier,
                bio = "${ret.yearsActive}-year ${ret.position} icon for ${ret.teamName} (${ret.championships}x Champion, ${ret.peakRating} Peak OVR).",
                inductionQuote = hofQuotes[(idx + year) % hofQuotes.size]
            )
        }

        // 5. Resolve Rookie Draft Order from Live Traded Draft Picks & Generate Prospect Pool
        val draftOrderResolution = resolveDraftOrderFromPicks(
            year = year,
            sortedLottery = sortedLottery,
            franchises = franchises,
            includeRound2Always = includeRound2Always
        )
        val draftOrderSlots = draftOrderResolution.slots
        val draftOrderIds = draftOrderSlots.map { it.ownerFranchiseId }
        val prospectPool = generateDraftProspectPool(
            year = year,
            poolSize = maxOf(draftOrderSlots.size + 6, 14),
            sportType = sportType
        )

        // 0. Yearly Awards Gala (Placed at the start of the off-season, tied directly to player/team stats)
        val awardsCeremony = RecordBookEngine.computeYearlyAwards(
            year = year,
            sportType = sportType,
            franchises = franchises,
            seasonStats = seasonStats,
            weeklyAwards = weeklyAwards,
            champion = champion
        )

        val baseSession = OffSeasonSession(
            year = year,
            currentStep = OffSeasonStep.AWARDS_SHOW,
            isCompleted = false,
            awardsCeremony = awardsCeremony,
            retirements = retirements,
            lotteryResults = draftOrderResolution.updatedLotteryResults,
            draftPicks = emptyList(),
            draftProspectPool = prospectPool,
            draftOrderFranchiseIds = draftOrderIds,
            draftOrderSlots = draftOrderSlots,
            tradedPickAdjustments = draftOrderResolution.tradeAdjustments,
            currentDraftPickIndex = 0,
            isDraftCompleted = draftOrderIds.isEmpty(),
            freeAgencySignings = freeAgencySignings,
            hallOfFameInductees = hallOfFameInductees,
            hofClassRatified = false,
            initialParityScore = initialParity,
            finalParityScore = initialParity,
            teamMovements = emptyList()
        )

        val computedSession = if (autoCompleteDraft) {
            autoCompleteDraft(baseSession, franchises, sportType)
        } else {
            recalculateParityMovements(baseSession, franchises)
        }

        return computedSession
    }
}

// ==========================================
// === CORPORATE SPONSORSHIP DATA MODELS ===
// ==========================================

enum class SponsorSector(
    val title: String,
    val description: String,
    val badgeColorHex: Long,
    val defaultReputationDelta: Int
) {
    FINTECH_CRYPTO(
        title = "Fintech & Digital Assets",
        description = "High liquidity & massive signing bonuses, but elevated volatility and slight integrity scrutiny",
        badgeColorHex = 0xFF8B5CF6,
        defaultReputationDelta = -4
    ),
    SPORTSBOOK_GAMBLING(
        title = "Sportsbook & Gaming",
        description = "Extravagant annual payouts & stadium naming rights; substantial integrity & fan ethics penalty",
        badgeColorHex = 0xFFEF4444,
        defaultReputationDelta = -12
    ),
    ECO_RENEWABLES(
        title = "Clean Energy & ESG",
        description = "Moderate payouts with massive public relations prestige, fan morale praise, and clean governance boost",
        badgeColorHex = 0xFF10B981,
        defaultReputationDelta = +14
    ),
    GLOBAL_AIRLINE_LUXURY(
        title = "Global Airline & Luxury",
        description = "Prestigious international brand association, premier fan experiences, and top-tier corporate status",
        badgeColorHex = 0xFF06B6D4,
        defaultReputationDelta = +8
    ),
    BEVERAGE_QUICK_SERVICE(
        title = "Consumer FMCG & Beverage",
        description = "Broad domestic mass appeal, family-friendly marketing reach, and solid dependable annual cash flow",
        badgeColorHex = 0xFFF59E0B,
        defaultReputationDelta = +3
    ),
    AUTOMOTIVE_MOBILITY(
        title = "Automotive & Electric Mobility",
        description = "High-tech innovation appeal, modern vehicle fleet provision for all franchises, and solid prestige",
        badgeColorHex = 0xFF3B82F6,
        defaultReputationDelta = +6
    )
}

enum class SponsorTier(val title: String, val badgeColorHex: Long) {
    PRIMARY_TITLE("Primary Title Partner", 0xFFEAB308), // Gold
    OFFICIAL_PRESENTING("Official Presenting Partner", 0xFF3B82F6), // Blue
    INNOVATION_TECH("Innovation & Tech Partner", 0xFF06B6D4), // Cyan
    COMMUNITY_ESG("Community & ESG Partner", 0xFF10B981) // Emerald
}

data class CorporateSponsorBidder(
    val id: String,
    val corporationName: String,
    val sector: SponsorSector,
    val tier: SponsorTier,
    val executiveName: String,
    val executiveTitle: String,
    val executiveQuote: String,
    val baseAnnualPayoutMillions: Long,
    val baseLumpSumMillions: Long,
    val preferredTermYears: Int,
    val maxTermYears: Int = 5,
    val minTermYears: Int = 1,
    val reputationEffect: Int, // e.g. -12 to +15 impact on league integrity / reputation
    val fanMoraleBonus: Int = 0, // e.g. -5 to +10
    val patienceRating: Int = 100,
    val activationInitiative: String = "Global Jersey Crest & Stadium Signage"
)

data class ActiveLeagueSponsorship(
    val id: String,
    val corporationName: String,
    val sector: SponsorSector,
    val tier: SponsorTier,
    val annualPayoutMillions: Long,
    val yearsRemaining: Int,
    val upfrontSigningBonusMillions: Long,
    val reputationImpact: Int,
    val fanMoraleBonus: Int = 0,
    val activationDescription: String = "Primary Jersey & Arena Deck Activation"
)



