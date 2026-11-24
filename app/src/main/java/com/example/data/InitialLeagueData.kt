package com.example.data

import com.example.model.*

object InitialLeagueData {

    fun getDefaultCities(): List<MarketCity> = listOf(
        MarketCity(
            id = "city_nyc",
            name = "New York",
            country = "USA",
            region = "East Coast",
            populationMillions = 8.8,
            tier = MarketTier.TIER_1_MEGA,
            annualGrowthRatePct = 0.8,
            sportsAppetite = 95,
            medianIncomeK = 88,
            isInternational = false,
            hasFranchise = true,
            expansionFeeMillions = 140L
        ),
        MarketCity(
            id = "city_la",
            name = "Los Angeles",
            country = "USA",
            region = "West Coast",
            populationMillions = 3.9,
            tier = MarketTier.TIER_1_MEGA,
            annualGrowthRatePct = 1.1,
            sportsAppetite = 92,
            medianIncomeK = 84,
            isInternational = false,
            hasFranchise = true,
            expansionFeeMillions = 135L
        ),
        MarketCity(
            id = "city_chicago",
            name = "Chicago",
            country = "USA",
            region = "Midwest",
            populationMillions = 2.7,
            tier = MarketTier.TIER_1_MEGA,
            annualGrowthRatePct = 0.5,
            sportsAppetite = 90,
            medianIncomeK = 76,
            isInternational = false,
            hasFranchise = true,
            expansionFeeMillions = 110L
        ),
        MarketCity(
            id = "city_austin",
            name = "Austin",
            country = "USA",
            region = "Sunbelt",
            populationMillions = 1.1,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 4.8,
            sportsAppetite = 88,
            medianIncomeK = 92,
            isInternational = false,
            hasFranchise = true,
            expansionFeeMillions = 85L
        ),
        MarketCity(
            id = "city_vegas",
            name = "Las Vegas",
            country = "USA",
            region = "Southwest",
            populationMillions = 0.7,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.9,
            sportsAppetite = 94,
            medianIncomeK = 72,
            isInternational = false,
            hasFranchise = true,
            expansionFeeMillions = 90L
        ),
        MarketCity(
            id = "city_seattle",
            name = "Seattle",
            country = "USA",
            region = "Pacific Northwest",
            populationMillions = 0.8,
            tier = MarketTier.TECH_INNOVATION_HUB,
            annualGrowthRatePct = 2.4,
            sportsAppetite = 91,
            medianIncomeK = 105,
            isInternational = false,
            hasFranchise = true,
            expansionFeeMillions = 95L
        ),
        // High-growth relocation & expansion candidates
        MarketCity(
            id = "city_london",
            name = "London",
            country = "United Kingdom",
            region = "Western Europe",
            populationMillions = 9.0,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.9,
            sportsAppetite = 96,
            medianIncomeK = 82,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 160L
        ),
        MarketCity(
            id = "city_tokyo",
            name = "Tokyo",
            country = "Japan",
            region = "East Asia",
            populationMillions = 14.0,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.2,
            sportsAppetite = 93,
            medianIncomeK = 78,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 150L
        ),
        MarketCity(
            id = "city_mexico",
            name = "Mexico City",
            country = "Mexico",
            region = "Latin America",
            populationMillions = 9.2,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 2.8,
            sportsAppetite = 98,
            medianIncomeK = 48,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 120L
        ),
        MarketCity(
            id = "city_berlin",
            name = "Berlin",
            country = "Germany",
            region = "Central Europe",
            populationMillions = 3.7,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 2.1,
            sportsAppetite = 87,
            medianIncomeK = 74,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 125L
        ),
        MarketCity(
            id = "city_toronto",
            name = "Toronto",
            country = "Canada",
            region = "North America",
            populationMillions = 3.0,
            tier = MarketTier.TECH_INNOVATION_HUB,
            annualGrowthRatePct = 2.6,
            sportsAppetite = 89,
            medianIncomeK = 80,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 100L
        ),
        MarketCity(
            id = "city_vancouver",
            name = "Vancouver",
            country = "Canada",
            region = "Pacific Rim",
            populationMillions = 0.7,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.2,
            sportsAppetite = 86,
            medianIncomeK = 83,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 85L
        ),
        MarketCity(
            id = "city_atlanta",
            name = "Atlanta",
            country = "USA",
            region = "Southeast",
            populationMillions = 0.5,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.5,
            sportsAppetite = 91,
            medianIncomeK = 75,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 80L
        ),
        MarketCity(
            id = "city_nashville",
            name = "Nashville",
            country = "USA",
            region = "Sunbelt",
            populationMillions = 0.7,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.8,
            sportsAppetite = 89,
            medianIncomeK = 78,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 80L
        ),
        MarketCity(
            id = "city_paris",
            name = "Paris",
            country = "France",
            region = "Western Europe",
            populationMillions = 2.2,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.6,
            sportsAppetite = 90,
            medianIncomeK = 80,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 145L,
            arenaReadinessStatus = "Accor Arena (20,300 cap, Ready)",
            investorGroupProfile = "Pan-European Sports Consortium"
        )
    )

    fun getProspectiveMarketPool(): List<MarketCity> = listOf(
        // --- NATIONAL PROSPECTIVE MARKETS (USA / NORTH AMERICA) ---
        MarketCity(
            id = "city_prospect_boston",
            name = "Boston",
            country = "USA",
            region = "Northeast",
            populationMillions = 4.9,
            tier = MarketTier.TIER_1_MEGA,
            annualGrowthRatePct = 1.1,
            sportsAppetite = 94,
            medianIncomeK = 98,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 140L,
            arenaName = "TD New England Center",
            arenaCapacity = 21500,
            arenaReadinessStatus = "Turnkey Championship Facility",
            investorGroupProfile = "New England Heritage Capital"
        ),
        MarketCity(
            id = "city_prospect_sf",
            name = "San Francisco",
            country = "USA",
            region = "West Coast",
            populationMillions = 3.3,
            tier = MarketTier.TECH_INNOVATION_HUB,
            annualGrowthRatePct = 1.6,
            sportsAppetite = 92,
            medianIncomeK = 120,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 145L,
            arenaName = "Silicon Bay Pavilion",
            arenaCapacity = 20500,
            arenaReadinessStatus = "Turnkey State-of-the-Art Arena",
            investorGroupProfile = "Bay Area Venture Consortium"
        ),
        MarketCity(
            id = "city_prospect_miami",
            name = "Miami",
            country = "USA",
            region = "Southeast",
            populationMillions = 2.7,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.6,
            sportsAppetite = 88,
            medianIncomeK = 76,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 95L,
            arenaName = "Biscayne Bay Colosseum",
            arenaCapacity = 20000,
            arenaReadinessStatus = "Turnkey Oceanfront Arena",
            investorGroupProfile = "South Florida Sports Media LLC"
        ),
        MarketCity(
            id = "city_prospect_phoenix",
            name = "Phoenix",
            country = "USA",
            region = "Southwest",
            populationMillions = 1.7,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.9,
            sportsAppetite = 87,
            medianIncomeK = 75,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 85L,
            arenaName = "Desert Sun Arena",
            arenaCapacity = 21000,
            arenaReadinessStatus = "Modernized 2024 Downtown Venue",
            investorGroupProfile = "Valley Commerce Athletic Trust"
        ),
        MarketCity(
            id = "city_prospect_sd",
            name = "San Diego",
            country = "USA",
            region = "West Coast",
            populationMillions = 1.4,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 2.4,
            sportsAppetite = 86,
            medianIncomeK = 92,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 90L,
            arenaName = "Pacific Coast Pavilion",
            arenaCapacity = 19500,
            arenaReadinessStatus = "Downtown Entertainment Hub",
            investorGroupProfile = "Coronado Bay Capital"
        ),
        MarketCity(
            id = "city_prospect_philly",
            name = "Philadelphia",
            country = "USA",
            region = "Northeast",
            populationMillions = 1.6,
            tier = MarketTier.TIER_1_MEGA,
            annualGrowthRatePct = 0.6,
            sportsAppetite = 90,
            medianIncomeK = 72,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 115L,
            arenaName = "Liberty Bell Fieldhouse",
            arenaCapacity = 21000,
            arenaReadinessStatus = "Turnkey Historic Complex",
            investorGroupProfile = "Keystone Athletic Syndicate"
        ),
        MarketCity(
            id = "city_prospect_charlotte",
            name = "Charlotte",
            country = "USA",
            region = "Southeast",
            populationMillions = 0.9,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.8,
            sportsAppetite = 86,
            medianIncomeK = 80,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 75L,
            arenaName = "Queen City Center",
            arenaCapacity = 20200,
            arenaReadinessStatus = "Turnkey Banking District Venue",
            investorGroupProfile = "Piedmont Financial Sports Partners"
        ),
        MarketCity(
            id = "city_prospect_slc",
            name = "Salt Lake City",
            country = "USA",
            region = "Mountain West",
            populationMillions = 1.2,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.5,
            sportsAppetite = 82,
            medianIncomeK = 84,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 70L,
            arenaName = "Wasatch Mountain Center",
            arenaCapacity = 19000,
            arenaReadinessStatus = "Olympic Legacy Arena Ready",
            investorGroupProfile = "Intermountain Tech Ventures"
        ),
        MarketCity(
            id = "city_prospect_portland",
            name = "Portland",
            country = "USA",
            region = "Pacific Northwest",
            populationMillions = 0.7,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 2.1,
            sportsAppetite = 85,
            medianIncomeK = 85,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 70L,
            arenaName = "Cascadia Garden",
            arenaCapacity = 20000,
            arenaReadinessStatus = "Turnkey Northwest Arena",
            investorGroupProfile = "Pacific Northwest Outdoor Brands"
        ),
        MarketCity(
            id = "city_prospect_sa",
            name = "San Antonio",
            country = "USA",
            region = "Southwest",
            populationMillions = 1.5,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.2,
            sportsAppetite = 85,
            medianIncomeK = 65,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 75L,
            arenaName = "Alamo Dome Pavilion",
            arenaCapacity = 20500,
            arenaReadinessStatus = "Turnkey River Walk Arena",
            investorGroupProfile = "Texas Lone Star Equity"
        ),
        MarketCity(
            id = "city_prospect_kc",
            name = "Kansas City",
            country = "USA",
            region = "Midwest",
            populationMillions = 0.5,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 1.8,
            sportsAppetite = 84,
            medianIncomeK = 68,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 60L,
            arenaName = "Heartland Center",
            arenaCapacity = 19000,
            arenaReadinessStatus = "Turnkey Prairie Arena",
            investorGroupProfile = "Midwest Agricultural Trust"
        ),
        MarketCity(
            id = "city_prospect_louisville",
            name = "Louisville",
            country = "USA",
            region = "Midwest",
            populationMillions = 0.6,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 1.2,
            sportsAppetite = 83,
            medianIncomeK = 60,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 55L,
            arenaName = "Bluegrass Fieldhouse",
            arenaCapacity = 22000,
            arenaReadinessStatus = "KFC Yum! Style Megadome",
            investorGroupProfile = "Bourbon Country Sports Group"
        ),
        MarketCity(
            id = "city_prospect_abq",
            name = "Albuquerque",
            country = "USA",
            region = "Southwest",
            populationMillions = 0.6,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 1.6,
            sportsAppetite = 74,
            medianIncomeK = 56,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 45L,
            arenaName = "Rio Grande Pit",
            arenaCapacity = 16500,
            arenaReadinessStatus = "Minor Renovation Required",
            investorGroupProfile = "Enchanted Mesa Sports Collective"
        ),
        MarketCity(
            id = "city_prospect_boise",
            name = "Boise",
            country = "USA",
            region = "Northwest",
            populationMillions = 0.3,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 4.4,
            sportsAppetite = 70,
            medianIncomeK = 72,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 40L,
            arenaName = "Treasure Valley Arena",
            arenaCapacity = 14500,
            arenaReadinessStatus = "Expansion Construction Planned",
            investorGroupProfile = "Gem State Development Partners"
        ),
        MarketCity(
            id = "city_prospect_honolulu",
            name = "Honolulu",
            country = "USA",
            region = "Pacific Island",
            populationMillions = 0.4,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 0.5,
            sportsAppetite = 72,
            medianIncomeK = 88,
            isInternational = false,
            hasFranchise = false,
            expansionFeeMillions = 42L,
            arenaName = "Pacific Diamond Arena",
            arenaCapacity = 15000,
            arenaReadinessStatus = "Tourism & Resort Arena",
            investorGroupProfile = "Aloha Hospitality Syndicate"
        ),

        // --- INTERNATIONAL PROSPECTIVE MARKETS ---
        MarketCity(
            id = "city_prospect_madrid",
            name = "Madrid",
            country = "Spain",
            region = "Southern Europe",
            populationMillions = 3.4,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.5,
            sportsAppetite = 93,
            medianIncomeK = 75,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 135L,
            arenaName = "Palacio de los Deportes",
            arenaCapacity = 21000,
            arenaReadinessStatus = "Turnkey Euroleague Megavenue",
            investorGroupProfile = "Iberian Sports Holdings"
        ),
        MarketCity(
            id = "city_prospect_rome",
            name = "Rome",
            country = "Italy",
            region = "Southern Europe",
            populationMillions = 2.9,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.2,
            sportsAppetite = 89,
            medianIncomeK = 72,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 120L,
            arenaName = "Palalottomatica Colosseum",
            arenaCapacity = 20000,
            arenaReadinessStatus = "Turnkey Capital Sports Complex",
            investorGroupProfile = "Roman Imperial Sports Capital"
        ),
        MarketCity(
            id = "city_prospect_amsterdam",
            name = "Amsterdam",
            country = "Netherlands",
            region = "Western Europe",
            populationMillions = 1.2,
            tier = MarketTier.TECH_INNOVATION_HUB,
            annualGrowthRatePct = 2.2,
            sportsAppetite = 87,
            medianIncomeK = 86,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 110L,
            arenaName = "Ziggo Dome International",
            arenaCapacity = 19500,
            arenaReadinessStatus = "Turnkey Digital Entertainment Dome",
            investorGroupProfile = "Orange Technology Investments"
        ),
        MarketCity(
            id = "city_prospect_dublin",
            name = "Dublin",
            country = "Ireland",
            region = "Western Europe",
            populationMillions = 1.3,
            tier = MarketTier.TECH_INNOVATION_HUB,
            annualGrowthRatePct = 2.8,
            sportsAppetite = 82,
            medianIncomeK = 95,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 100L,
            arenaName = "3Arena Grand Canal",
            arenaCapacity = 18500,
            arenaReadinessStatus = "Turnkey Waterfront Arena",
            investorGroupProfile = "Celtic Tech Capital Trust"
        ),
        MarketCity(
            id = "city_prospect_munich",
            name = "Munich",
            country = "Germany",
            region = "Central Europe",
            populationMillions = 1.5,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.8,
            sportsAppetite = 94,
            medianIncomeK = 92,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 125L,
            arenaName = "SAP Garden Olympic Park",
            arenaCapacity = 21500,
            arenaReadinessStatus = "Brand-New 2024 Olympic Megavenue",
            investorGroupProfile = "Bavarian Industrial Sports Syndicate"
        ),
        MarketCity(
            id = "city_prospect_barcelona",
            name = "Barcelona",
            country = "Spain",
            region = "Southern Europe",
            populationMillions = 1.6,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.7,
            sportsAppetite = 92,
            medianIncomeK = 76,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 130L,
            arenaName = "Palau Sant Jordi",
            arenaCapacity = 21000,
            arenaReadinessStatus = "Turnkey Olympic Multi-Sport Palace",
            investorGroupProfile = "Catalan Athletic Holdings"
        ),
        MarketCity(
            id = "city_prospect_seoul",
            name = "Seoul",
            country = "South Korea",
            region = "East Asia",
            populationMillions = 9.8,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.4,
            sportsAppetite = 93,
            medianIncomeK = 82,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 145L,
            arenaName = "Jamsil Olympic Arena",
            arenaCapacity = 23000,
            arenaReadinessStatus = "Turnkey High-Tech Megadome",
            investorGroupProfile = "Han River Entertainment Conglomerate"
        ),
        MarketCity(
            id = "city_prospect_sydney",
            name = "Sydney",
            country = "Australia",
            region = "Oceania",
            populationMillions = 5.3,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.8,
            sportsAppetite = 91,
            medianIncomeK = 88,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 135L,
            arenaName = "Qudos Bank SuperDome",
            arenaCapacity = 21000,
            arenaReadinessStatus = "Turnkey Olympic Legacy Superdome",
            investorGroupProfile = "Southern Cross Sports Equity"
        ),
        MarketCity(
            id = "city_prospect_melbourne",
            name = "Melbourne",
            country = "Australia",
            region = "Oceania",
            populationMillions = 5.1,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 2.2,
            sportsAppetite = 95,
            medianIncomeK = 86,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 140L,
            arenaName = "Rod Laver Arena Precinct",
            arenaCapacity = 22500,
            arenaReadinessStatus = "Turnkey World Sports Capital Arena",
            investorGroupProfile = "Victoria Sporting Capital Group"
        ),
        MarketCity(
            id = "city_prospect_saopaulo",
            name = "São Paulo",
            country = "Brazil",
            region = "Latin America",
            populationMillions = 12.3,
            tier = MarketTier.TIER_1_MEGA,
            annualGrowthRatePct = 1.2,
            sportsAppetite = 96,
            medianIncomeK = 52,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 125L,
            arenaName = "Ginásio do Ibirapuera",
            arenaCapacity = 22000,
            arenaReadinessStatus = "Turnkey Modernized Multi-Arena",
            investorGroupProfile = "Paulista Sports & Media Consortium"
        ),
        MarketCity(
            id = "city_prospect_ba",
            name = "Buenos Aires",
            country = "Argentina",
            region = "Latin America",
            populationMillions = 3.1,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 0.9,
            sportsAppetite = 92,
            medianIncomeK = 45,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 85L,
            arenaName = "Movistar Arena Argentina",
            arenaCapacity = 19500,
            arenaReadinessStatus = "Turnkey South American Arena",
            investorGroupProfile = "Rio de la Plata Athletic Group"
        ),
        MarketCity(
            id = "city_prospect_montreal",
            name = "Montreal",
            country = "Canada",
            region = "North America",
            populationMillions = 1.8,
            tier = MarketTier.TECH_INNOVATION_HUB,
            annualGrowthRatePct = 1.8,
            sportsAppetite = 88,
            medianIncomeK = 78,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 95L,
            arenaName = "Centre Bell Arena",
            arenaCapacity = 21273,
            arenaReadinessStatus = "Turnkey Legendary Hockey/Basketball Megavenue",
            investorGroupProfile = "Quebec Sports & Entertainment"
        ),
        MarketCity(
            id = "city_prospect_monterrey",
            name = "Monterrey",
            country = "Mexico",
            region = "Latin America",
            populationMillions = 1.1,
            tier = MarketTier.HIGH_GROWTH_SUNBELT,
            annualGrowthRatePct = 3.4,
            sportsAppetite = 87,
            medianIncomeK = 60,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 75L,
            arenaName = "Arena Monterrey",
            arenaCapacity = 21000,
            arenaReadinessStatus = "Turnkey Northern Mexico Arena",
            investorGroupProfile = "Sierra Madre Industrial Sports Group"
        ),
        MarketCity(
            id = "city_prospect_singapore",
            name = "Singapore",
            country = "Singapore",
            region = "Southeast Asia",
            populationMillions = 5.9,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.5,
            sportsAppetite = 85,
            medianIncomeK = 110,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 150L,
            arenaName = "Singapore Indoor Stadium",
            arenaCapacity = 20000,
            arenaReadinessStatus = "Turnkey Marina Bay Complex",
            investorGroupProfile = "Lion City Sovereign Partners"
        ),
        MarketCity(
            id = "city_prospect_dubai",
            name = "Dubai",
            country = "UAE",
            region = "Middle East",
            populationMillions = 3.6,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 4.2,
            sportsAppetite = 84,
            medianIncomeK = 115,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 160L,
            arenaName = "Coca-Cola Arena Dubai",
            arenaCapacity = 21000,
            arenaReadinessStatus = "Turnkey Ultra-Luxury Arena",
            investorGroupProfile = "Emirates Sovereign Wealth Syndicate"
        ),
        MarketCity(
            id = "city_prospect_bogota",
            name = "Bogotá",
            country = "Colombia",
            region = "Latin America",
            populationMillions = 7.9,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 1.6,
            sportsAppetite = 85,
            medianIncomeK = 42,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 65L,
            arenaName = "Movistar Arena Bogotá",
            arenaCapacity = 18000,
            arenaReadinessStatus = "Modernized Andean Arena",
            investorGroupProfile = "Andean Community Athletics"
        ),
        MarketCity(
            id = "city_prospect_prague",
            name = "Prague",
            country = "Czech Republic",
            region = "Central Europe",
            populationMillions = 1.3,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 1.4,
            sportsAppetite = 78,
            medianIncomeK = 65,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 60L,
            arenaName = "O2 Arena Prague",
            arenaCapacity = 19000,
            arenaReadinessStatus = "Turnkey Central European Megavenue",
            investorGroupProfile = "Bohemian Capital Trust"
        ),
        MarketCity(
            id = "city_prospect_reykjavik",
            name = "Reykjavik",
            country = "Iceland",
            region = "Northern Europe",
            populationMillions = 0.14,
            tier = MarketTier.REGIONAL_HEARTLAND,
            annualGrowthRatePct = 1.2,
            sportsAppetite = 76,
            medianIncomeK = 88,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 35L,
            arenaName = "Laugardalshöll Arena",
            arenaCapacity = 12000,
            arenaReadinessStatus = "Boutique Geothermal Arena",
            investorGroupProfile = "Viking Global Sports Syndicate"
        ),
        MarketCity(
            id = "city_prospect_rio",
            name = "Rio de Janeiro",
            country = "Brazil",
            region = "South America",
            populationMillions = 6.7,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.1,
            sportsAppetite = 95,
            medianIncomeK = 48,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 115L,
            arenaName = "Jeunesse Olympic Arena",
            arenaCapacity = 19000,
            arenaReadinessStatus = "Turnkey Olympic Basketball Arena",
            investorGroupProfile = "Carioca Athletic & Entertainment Group"
        ),
        MarketCity(
            id = "city_prospect_santiago",
            name = "Santiago",
            country = "Chile",
            region = "South America",
            populationMillions = 6.2,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 1.5,
            sportsAppetite = 86,
            medianIncomeK = 54,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 90L,
            arenaName = "Movistar Arena Santiago",
            arenaCapacity = 17500,
            arenaReadinessStatus = "Turnkey Parque O'Higgins Arena",
            investorGroupProfile = "Andes Pacific Investment Partners"
        ),
        MarketCity(
            id = "city_prospect_manila",
            name = "Manila",
            country = "Philippines",
            region = "Southeast Asia",
            populationMillions = 14.4,
            tier = MarketTier.INTERNATIONAL_GATEWAY,
            annualGrowthRatePct = 2.4,
            sportsAppetite = 99,
            medianIncomeK = 44,
            isInternational = true,
            hasFranchise = false,
            expansionFeeMillions = 130L,
            arenaName = "Mall of Asia SuperArena",
            arenaCapacity = 22000,
            arenaReadinessStatus = "Turnkey FIBA World Cup Flagship Venue",
            investorGroupProfile = "Archipelago Premier Hoops Consortium"
        )
    )

    fun isNorthAmericanCity(cityName: String): Boolean {
        val continentalCities = setOf(
            // Europe
            "London", "Paris", "Berlin", "Madrid", "Rome", "Amsterdam", "Dublin",
            "Munich", "Barcelona", "Prague", "Reykjavik", "Milan", "Athens", "Lisbon",
            // South America
            "São Paulo", "Rio de Janeiro", "Buenos Aires", "Bogotá", "Santiago", "Lima",
            // Asia-Oceania
            "Tokyo", "Seoul", "Sydney", "Melbourne", "Singapore", "Dubai", "Manila",
            "Auckland", "Osaka", "Taipei", "Jakarta", "Bangkok"
        )
        return cityName !in continentalCities
    }

    fun getTargetContinentalConference(cityName: String): Conference {
        return when (cityName) {
            "London", "Paris", "Berlin", "Madrid", "Rome", "Amsterdam", "Dublin",
            "Munich", "Barcelona", "Prague", "Reykjavik", "Milan", "Athens", "Lisbon" -> Conference.EUROPEAN
            "São Paulo", "Rio de Janeiro", "Buenos Aires", "Bogotá", "Santiago", "Lima" -> Conference.SOUTH_AMERICAN
            "Tokyo", "Seoul", "Sydney", "Melbourne", "Singapore", "Dubai", "Manila",
            "Auckland", "Osaka", "Taipei", "Jakarta", "Bangkok" -> Conference.ASIA_OCEANIA
            else -> getLegacyEastWestAlignment(cityName).first
        }
    }

    fun getGeographicAlignment(
        cityName: String,
        enabledConferences: Set<Conference> = setOf(Conference.EASTERN, Conference.WESTERN),
        enabledDivisions: Set<Division> = emptySet()
    ): Pair<Conference, Division> {
        if (!isNorthAmericanCity(cityName)) {
            val targetContinent = getTargetContinentalConference(cityName)
            if (targetContinent.isContinentalInternational && targetContinent in enabledConferences) {
                val idealDiv = when (cityName) {
                    // European divisions
                    "London", "Paris", "Amsterdam", "Dublin" -> Division.WEST_EUROPE
                    "Berlin", "Munich", "Prague", "Reykjavik" -> Division.CENTRAL_NORTH_EUROPE
                    "Madrid", "Rome", "Barcelona", "Milan", "Athens", "Lisbon" -> Division.MEDITERRANEAN
                    // South American divisions
                    "São Paulo", "Rio de Janeiro" -> Division.ATLANTIC_BRAZIL
                    "Buenos Aires", "Santiago" -> Division.SOUTHERN_CONE
                    "Bogotá", "Lima" -> Division.ANDEAN_NORTH
                    // Asia-Oceania divisions
                    "Tokyo", "Seoul", "Osaka", "Taipei" -> Division.EAST_ASIA
                    "Sydney", "Melbourne", "Auckland" -> Division.OCEANIA
                    "Singapore", "Dubai", "Manila", "Jakarta", "Bangkok" -> Division.INDO_PACIFIC
                    else -> Division.values().first { it.conference == targetContinent }
                }
                val resolvedDiv = if (enabledDivisions.isNotEmpty() && idealDiv !in enabledDivisions) {
                    Division.values().firstOrNull { it.conference == targetContinent && it in enabledDivisions } ?: idealDiv
                } else idealDiv
                return targetContinent to resolvedDiv
            }
        }

        return getLegacyEastWestAlignment(cityName)
    }

    private fun getLegacyEastWestAlignment(cityName: String): Pair<Conference, Division> {
        return when (cityName) {
            "New York", "Boston", "Philadelphia", "London", "Paris", "Toronto", "Montreal", "Madrid", "Rome", "Amsterdam", "Dublin", "Reykjavik" -> Conference.EASTERN to Division.ATLANTIC
            "Chicago", "Detroit", "Cleveland", "Indianapolis", "Minneapolis", "Berlin", "Munich", "Prague", "Kansas City", "Louisville" -> Conference.EASTERN to Division.CENTRAL
            "Atlanta", "Miami", "Charlotte", "Orlando", "Washington", "Tampa", "Barcelona", "Dubai" -> Conference.EASTERN to Division.SOUTHEAST
            "Los Angeles", "San Francisco", "Las Vegas", "San Diego", "Tokyo", "Honolulu", "Seoul", "Sydney", "Melbourne", "Singapore", "Manila" -> Conference.WESTERN to Division.PACIFIC
            "Austin", "Dallas", "Houston", "San Antonio", "Phoenix", "Nashville", "Memphis", "Mexico City", "Monterrey", "Albuquerque", "São Paulo", "Rio de Janeiro", "Buenos Aires", "Bogotá", "Santiago" -> Conference.WESTERN to Division.SOUTHWEST
            "Seattle", "Portland", "Denver", "Salt Lake City", "Vancouver", "Calgary", "Boise" -> Conference.WESTERN to Division.NORTHWEST
            else -> Conference.EASTERN to Division.ATLANTIC
        }
    }

    fun getInitialFranchises(sportType: SportType = SportType.BASKETBALL): List<Franchise> = listOf(
        Franchise(
            id = "team_nyc",
            cityId = "city_nyc",
            cityName = "New York",
            nickname = "Empire",
            colorHex = 0xFF1E3A8AL, // Deep Royal
            secondaryColorHex = 0xFFF59E0BL, // Electric Amber
            ownership = OwnershipGroup(
                id = "own_nyc",
                name = "Gotham Sports Media Group",
                type = OwnershipType.MEDIA_MOGUL,
                netWorthBillions = 9.5,
                primaryOwner = OwnerGovernanceEngine.defaultOwnerForGroup("own_nyc", "Gotham Sports Media Group", OwnershipType.MEDIA_MOGUL)
            ),
            conference = Conference.EASTERN,
            division = Division.ATLANTIC,
            rosterRating = 84,
            fanBaseMillions = 4.2,
            fanMorale = 80,
            stadiumCapacity = 21500,
            ticketPrice = 90,
            annualPayrollMillions = 78L,
            localSponsorRevenueMillions = 42L,
            fanLoyalty = 78,
            stadiumAge = 22,
            stadiumCondition = 64
        ),
        Franchise(
            id = "team_la",
            cityId = "city_la",
            cityName = "Los Angeles",
            nickname = "Vipers",
            colorHex = 0xFF7C3AEDL, // Violet Neon
            secondaryColorHex = 0xFFFACC15L, // Championship Gold
            ownership = OwnershipGroup(
                id = "own_la",
                name = "Pacific Horizon Equity",
                type = OwnershipType.PRIVATE_EQUITY,
                netWorthBillions = 6.2,
                primaryOwner = OwnerGovernanceEngine.defaultOwnerForGroup("own_la", "Pacific Horizon Equity", OwnershipType.PRIVATE_EQUITY)
            ),
            conference = Conference.WESTERN,
            division = Division.PACIFIC,
            rosterRating = 86,
            fanBaseMillions = 3.8,
            fanMorale = 84,
            stadiumCapacity = 20500,
            ticketPrice = 85,
            annualPayrollMillions = 82L,
            localSponsorRevenueMillions = 38L,
            fanLoyalty = 80,
            stadiumAge = 11,
            stadiumCondition = 86
        ),
        Franchise(
            id = "team_chicago",
            cityId = "city_chicago",
            cityName = "Chicago",
            nickname = "Titans",
            colorHex = 0xFFDC2626L, // Crimson Red
            secondaryColorHex = 0xFF111827L, // Onyx Black
            ownership = OwnershipGroup(
                id = "own_chi",
                name = "Midwest Fans Coalition",
                type = OwnershipType.COMMUNITY_TRUST,
                netWorthBillions = 2.1,
                primaryOwner = OwnerGovernanceEngine.defaultOwnerForGroup("own_chi", "Midwest Fans Coalition", OwnershipType.COMMUNITY_TRUST)
            ),
            conference = Conference.EASTERN,
            division = Division.CENTRAL,
            rosterRating = 77,
            fanBaseMillions = 2.9,
            fanMorale = 74,
            stadiumCapacity = 23000,
            ticketPrice = 65,
            annualPayrollMillions = 66L,
            localSponsorRevenueMillions = 30L,
            fanLoyalty = 84,
            stadiumAge = 25,
            stadiumCondition = 51
        ),
        Franchise(
            id = "team_austin",
            cityId = "city_austin",
            cityName = "Austin",
            nickname = "Apex",
            colorHex = 0xFFF59E0BL, // Electric Amber
            secondaryColorHex = 0xFF0F172AL, // Midnight Navy
            ownership = OwnershipGroup(
                id = "own_atx",
                name = "Nexus Capital Venture",
                type = OwnershipType.TECH_BILLIONAIRE,
                netWorthBillions = 14.8,
                primaryOwner = OwnerGovernanceEngine.defaultOwnerForGroup("own_atx", "Nexus Capital Venture", OwnershipType.TECH_BILLIONAIRE)
            ),
            conference = Conference.WESTERN,
            division = Division.SOUTHWEST,
            rosterRating = 81,
            fanBaseMillions = 2.1,
            fanMorale = 90,
            stadiumCapacity = 19500,
            ticketPrice = 75,
            annualPayrollMillions = 74L,
            localSponsorRevenueMillions = 35L,
            fanLoyalty = 82,
            stadiumAge = 8,
            stadiumCondition = 91
        ),
        Franchise(
            id = "team_vegas",
            cityId = "city_vegas",
            cityName = "Las Vegas",
            nickname = "Aces",
            colorHex = 0xFF059669L, // Emerald Green
            secondaryColorHex = 0xFFD97706L, // Vegas Bronze
            ownership = OwnershipGroup(
                id = "own_las",
                name = "High Roller Entertainment",
                type = OwnershipType.MEDIA_MOGUL,
                netWorthBillions = 7.8,
                primaryOwner = OwnerGovernanceEngine.defaultOwnerForGroup("own_las", "High Roller Entertainment", OwnershipType.MEDIA_MOGUL)
            ),
            conference = Conference.WESTERN,
            division = Division.PACIFIC,
            rosterRating = 79,
            fanBaseMillions = 1.9,
            fanMorale = 82,
            stadiumCapacity = 18800,
            ticketPrice = 80,
            annualPayrollMillions = 70L,
            localSponsorRevenueMillions = 34L,
            fanLoyalty = 74,
            stadiumAge = 16,
            stadiumCondition = 73
        ),
        Franchise(
            id = "team_seattle",
            cityId = "city_seattle",
            cityName = "Seattle",
            nickname = "Storm",
            colorHex = 0xFF0284C7L, // Pacific Teal
            secondaryColorHex = 0xFF84CC16L, // Volt Lime
            ownership = OwnershipGroup(
                id = "own_sea",
                name = "Rainier Sovereign Syndicate",
                type = OwnershipType.SOVEREIGN_SYNDICATE,
                netWorthBillions = 22.0,
                primaryOwner = OwnerGovernanceEngine.defaultOwnerForGroup("own_sea", "Rainier Sovereign Syndicate", OwnershipType.SOVEREIGN_SYNDICATE)
            ),
            conference = Conference.WESTERN,
            division = Division.NORTHWEST,
            rosterRating = 83,
            fanBaseMillions = 2.4,
            fanMorale = 86,
            stadiumCapacity = 20000,
            ticketPrice = 70,
            annualPayrollMillions = 76L,
            localSponsorRevenueMillions = 36L,
            fanLoyalty = 85,
            stadiumAge = 24,
            stadiumCondition = 54
        )
    ).map { f ->
        f.copy(
            roster = TradeEngine.generateDefaultRosterForFranchise(f.id, f.cityName, sportType, f.rosterRating),
            draftPicks = TradeEngine.generateDefaultDraftPicksForFranchise(f.id, f.teamFullName, 2026)
        )
    }.let { TradeEngine.seedTradeBlocksForLeague(it) }

    fun getInitialBroadcastDeals(): List<BroadcastContract> = listOf(
        BroadcastContract(
            id = "deal_cable",
            partnerName = "Apex Sports Network (Linear Cable)",
            platform = BroadcastPlatform.LINEAR_CABLE,
            annualPayoutMillions = 65L,
            yearsRemaining = 3,
            reachMultiplier = 1.2,
            upfrontSigningBonusMillions = 15L,
            networkScope = NetworkScope.NATIONAL_CABLE
        ),
        BroadcastContract(
            id = "deal_stream",
            partnerName = "GlobalStream Plus (Digital Streamer)",
            platform = BroadcastPlatform.GLOBAL_STREAMER,
            annualPayoutMillions = 85L,
            yearsRemaining = 4,
            reachMultiplier = 1.8,
            upfrontSigningBonusMillions = 35L,
            networkScope = NetworkScope.GLOBAL_STREAMING
        ),
        BroadcastContract(
            id = "deal_freetoair",
            partnerName = "National Prime Broadcast (Free to Air)",
            platform = BroadcastPlatform.FREE_TO_AIR,
            annualPayoutMillions = 50L,
            yearsRemaining = 2,
            reachMultiplier = 2.4,
            upfrontSigningBonusMillions = 10L,
            networkScope = NetworkScope.FREE_TO_AIR
        )
    )

    fun getNetworkBidders(): List<NetworkBidder> = listOf(
        NetworkBidder(
            id = "bid_ansn",
            name = "Apex National Sports (ANSN)",
            scope = NetworkScope.NATIONAL_CABLE,
            executiveName = "Marcus Sterling",
            executiveTitle = "Senior VP of Sports Programming",
            executiveQuote = "We want guaranteed long-term stability on national cable. Deliver us 4+ years and we will secure your annual rights baseline.",
            baseAnnualPayoutMillions = 70L,
            baseLumpSumMillions = 18L,
            preferredTermYears = 5,
            maxTermYears = 7,
            minTermYears = 2,
            reachMultiplier = 1.3,
            patienceRating = 100
        ),
        NetworkBidder(
            id = "bid_omnistream",
            name = "OmniStream Global+",
            scope = NetworkScope.GLOBAL_STREAMING,
            executiveName = "Dr. Elena Vance",
            executiveTitle = "Chief of Live Sports & Streaming",
            executiveQuote = "We need live digital exclusivity to drive global app subscriptions. We will inject immediate massive upfront capital, but want shorter renegotiation rights.",
            baseAnnualPayoutMillions = 88L,
            baseLumpSumMillions = 42L,
            preferredTermYears = 3,
            maxTermYears = 5,
            minTermYears = 1,
            reachMultiplier = 1.8,
            patienceRating = 90
        ),
        NetworkBidder(
            id = "bid_metro_rsn",
            name = "Metropolitan RSN Consortium",
            scope = NetworkScope.REGIONAL_RSN,
            executiveName = "Vincent Romano",
            executiveTitle = "Regional Cable Syndicate Head",
            executiveQuote = "Our regional network covers East Coast and Midwest metros. We pay top dollar lump-sums to lock down local market territory.",
            baseAnnualPayoutMillions = 60L,
            baseLumpSumMillions = 32L,
            preferredTermYears = 4,
            maxTermYears = 6,
            minTermYears = 2,
            reachMultiplier = 1.1,
            patienceRating = 85
        ),
        NetworkBidder(
            id = "bid_sunbelt_rsn",
            name = "Pacific & Sunbelt Regional Group",
            scope = NetworkScope.REGIONAL_RSN,
            executiveName = "Chloe Chen",
            executiveTitle = "West Coast Media President",
            executiveQuote = "We specialize in booming high-growth cities like Austin, Vegas, and Seattle. Flexible on bridge terms if you provide local star promotions.",
            baseAnnualPayoutMillions = 64L,
            baseLumpSumMillions = 28L,
            preferredTermYears = 3,
            maxTermYears = 5,
            minTermYears = 1,
            reachMultiplier = 1.25,
            patienceRating = 95
        ),
        NetworkBidder(
            id = "bid_ppn",
            name = "Public Prime Television (PPN)",
            scope = NetworkScope.FREE_TO_AIR,
            executiveName = "Arthur Pendelton",
            executiveTitle = "Director of Broadcast Network",
            executiveQuote = "Every television in the country can watch your games for free. Modest rights fees, but your league's grassroots fanbase will explode.",
            baseAnnualPayoutMillions = 52L,
            baseLumpSumMillions = 12L,
            preferredTermYears = 2,
            maxTermYears = 6,
            minTermYears = 1,
            reachMultiplier = 2.4,
            patienceRating = 100
        )
    )

    fun getDefaultActiveSponsorship(): ActiveLeagueSponsorship = ActiveLeagueSponsorship(
        id = "spon_active_initial",
        corporationName = "Apex Global Mobility (AMG)",
        sector = SponsorSector.AUTOMOTIVE_MOBILITY,
        tier = SponsorTier.OFFICIAL_PRESENTING,
        annualPayoutMillions = 28L,
        yearsRemaining = 3,
        upfrontSigningBonusMillions = 10L,
        reputationImpact = +6,
        fanMoraleBonus = +4,
        activationDescription = "Official League Mobility Fleet & Jersey Crest"
    )

    fun getCorporateSponsorBidders(): List<CorporateSponsorBidder> = listOf(
        CorporateSponsorBidder(
            id = "corp_solaris_clean",
            corporationName = "Solaris Renewable Energy",
            sector = SponsorSector.ECO_RENEWABLES,
            tier = SponsorTier.COMMUNITY_ESG,
            executiveName = "Astrid Lindholm",
            executiveTitle = "Chief Sustainability Officer",
            executiveQuote = "Our board will only partner with leaders who champion carbon-neutral sports infrastructure. We offer a pristine reputation boost and fan praise, but require strict ESG reporting.",
            baseAnnualPayoutMillions = 24L,
            baseLumpSumMillions = 14L,
            preferredTermYears = 4,
            maxTermYears = 5,
            minTermYears = 1,
            reputationEffect = +15,
            fanMoraleBonus = +8,
            patienceRating = 100,
            activationInitiative = "Zero-Emission Arena Net-Zero Initiative"
        ),
        CorporateSponsorBidder(
            id = "corp_kryptos_pay",
            corporationName = "Kryptos Digital Ledger Holdings",
            sector = SponsorSector.FINTECH_CRYPTO,
            tier = SponsorTier.PRIMARY_TITLE,
            executiveName = "Darius Thorne",
            executiveTitle = "Global Head of Brand Capital",
            executiveQuote = "We move at hyper-speed with billions in liquid capital. We'll pay exorbitant upfront bonuses to plaster our digital currency across your primary title banner, regardless of traditional media skepticism.",
            baseAnnualPayoutMillions = 48L,
            baseLumpSumMillions = 36L,
            preferredTermYears = 3,
            maxTermYears = 5,
            minTermYears = 1,
            reputationEffect = -5,
            fanMoraleBonus = -2,
            patienceRating = 90,
            activationInitiative = "League Marquee Title Rights & Digital Token Fan Pass"
        ),
        CorporateSponsorBidder(
            id = "corp_highroller_bet",
            corporationName = "ApexBet Sportsbook & Casino",
            sector = SponsorSector.SPORTSBOOK_GAMBLING,
            tier = SponsorTier.PRIMARY_TITLE,
            executiveName = "Victor Sterling",
            executiveTitle = "Managing Director of Global Wagering",
            executiveQuote = "We bring the largest cash bag in modern sports entertainment. You will face sanctimonious editorial blowback, but your balance sheet will be unmatched.",
            baseAnnualPayoutMillions = 62L,
            baseLumpSumMillions = 50L,
            preferredTermYears = 3,
            maxTermYears = 5,
            minTermYears = 1,
            reputationEffect = -14,
            fanMoraleBonus = -6,
            patienceRating = 85,
            activationInitiative = "Official Odds Integrator & Court-side Sportsbook Lounges"
        ),
        CorporateSponsorBidder(
            id = "corp_aero_luxury",
            corporationName = "Emirates Royal Airways",
            sector = SponsorSector.GLOBAL_AIRLINE_LUXURY,
            tier = SponsorTier.OFFICIAL_PRESENTING,
            executiveName = "Tariq Al-Mansoor",
            executiveTitle = "VP of Global Sponsorships",
            executiveQuote = "A premier global sports league deserves first-class international elegance. We guarantee private team charters for all international games, bolstering player morale and global prestige.",
            baseAnnualPayoutMillions = 38L,
            baseLumpSumMillions = 22L,
            preferredTermYears = 4,
            maxTermYears = 5,
            minTermYears = 2,
            reputationEffect = +9,
            fanMoraleBonus = +6,
            patienceRating = 95,
            activationInitiative = "Global Fleet Charters & First-Class Fan VIP Concierge"
        ),
        CorporateSponsorBidder(
            id = "corp_hyper_ion",
            corporationName = "Hyperion Electric Motors",
            sector = SponsorSector.AUTOMOTIVE_MOBILITY,
            tier = SponsorTier.INNOVATION_TECH,
            executiveName = "Evelyn Zhang",
            executiveTitle = "Chief Marketing Strategist",
            executiveQuote = "High performance, zero emissions, and futuristic automotive engineering. We want marquee branding at all tournament venues and will provide player vehicle fleets.",
            baseAnnualPayoutMillions = 32L,
            baseLumpSumMillions = 18L,
            preferredTermYears = 3,
            maxTermYears = 5,
            minTermYears = 1,
            reputationEffect = +7,
            fanMoraleBonus = +5,
            patienceRating = 100,
            activationInitiative = "Player EV Fleet Sponsorship & Charging Hubs"
        ),
        CorporateSponsorBidder(
            id = "corp_vital_brew",
            corporationName = "Glacier Springs Beverage Corp",
            sector = SponsorSector.BEVERAGE_QUICK_SERVICE,
            tier = SponsorTier.OFFICIAL_PRESENTING,
            executiveName = "Jack Callahan",
            executiveTitle = "Director of Sports Partnerships",
            executiveQuote = "Family-friendly, all-American hydration and sports drinks. We appeal to all demographics without controversy, providing steady, reliable revenue every single quarter.",
            baseAnnualPayoutMillions = 26L,
            baseLumpSumMillions = 12L,
            preferredTermYears = 5,
            maxTermYears = 5,
            minTermYears = 2,
            reputationEffect = +4,
            fanMoraleBonus = +4,
            patienceRating = 100,
            activationInitiative = "Official Sideline Hydration & Concession Exclusivity"
        )
    )

    fun getMarketingCampaignCatalog(): List<MarketingCampaign> = listOf(
        MarketingCampaign(
            id = "mkt_docuseries",
            title = "Behind the Shield: Streaming Docuseries",
            description = "High-production streaming documentary following star rivalries and owner drama",
            costMillions = 12L,
            fanGrowthPct = 14.5,
            durationWeeks = 6,
            targetSegment = "Casual & Young Fans"
        ),
        MarketingCampaign(
            id = "mkt_youth_clinics",
            title = "National Grassroots Youth Academy",
            description = "Community coaching clinics in 20 major metropolitan areas to build lifelong loyalty",
            costMillions = 6L,
            fanGrowthPct = 8.0,
            durationWeeks = 8,
            targetSegment = "Local Communities"
        ),
        MarketingCampaign(
            id = "mkt_london_showcase",
            title = "London International Series Gala",
            description = "Host marquee regular season game at Wembley Arena with global media broadcast",
            costMillions = 18L,
            fanGrowthPct = 22.0,
            durationWeeks = 4,
            targetSegment = "European & Global",
            isInternational = true
        ),
        MarketingCampaign(
            id = "mkt_tokyo_showcase",
            title = "Tokyo Neon Cup Showcase",
            description = "Exhibition tournament in Tokyo Dome capturing Asian media rights & merchandise",
            costMillions = 20L,
            fanGrowthPct = 25.0,
            durationWeeks = 4,
            targetSegment = "Asia-Pacific Gateway",
            isInternational = true
        ),
        MarketingCampaign(
            id = "mkt_viral_social",
            title = "Creator League & Short-Form Blitz",
            description = "Partner with top 50 digital sports creators for instant highlight clips and game reactions",
            costMillions = 8L,
            fanGrowthPct = 11.0,
            durationWeeks = 5,
            targetSegment = "Digital Gen-Z"
        )
    )

    fun getCommissionerDilemmas(): List<CommissionerDilemma> = listOf(
        CommissionerDilemma(
            id = "dil_doping_scandal",
            week = 2,
            title = "Marquee Superstar Tests Positive for Banned Substance",
            category = "Integrity & Rules",
            urgencyTag = "URGENT INTEGRITY BREACH",
            description = "The reigning MVP and league scoring leader has tested positive for an unapproved synthetic booster right before the televised Friday primetime showdown. The team owner urges leniency citing an over-the-counter tainted supplement.",
            choices = listOf(
                CommissionerChoice(
                    label = "Strict 10-Game Ban (Enforce Zero-Tolerance)",
                    narrativeOutcome = "League integrity praised by government regulators. Short-term TV ratings dip, but credibility rises.",
                    cashImpactMillions = -2L,
                    fanMoraleImpact = -4,
                    unionSatisfactionImpact = -10,
                    integrityImpact = +18,
                    ownerApprovalImpact = -12
                ),
                CommissionerChoice(
                    label = "2-Game Suspension with Rehabilitation Appeal",
                    narrativeOutcome = "A compromise that protects viewership while issuing formal discipline. Union agrees with the process.",
                    cashImpactMillions = 0L,
                    fanMoraleImpact = +2,
                    unionSatisfactionImpact = +6,
                    integrityImpact = 0,
                    ownerApprovalImpact = +4
                ),
                CommissionerChoice(
                    label = "Quiet Confidential Fine ($500K) & Retest",
                    narrativeOutcome = "Preserves the superstar for broadcast ratings, but media leaks spark outrage about favoritism.",
                    cashImpactMillions = +1L,
                    fanMoraleImpact = -8,
                    unionSatisfactionImpact = +8,
                    integrityImpact = -20,
                    ownerApprovalImpact = +15
                )
            )
        ),
        CommissionerDilemma(
            id = "dil_union_strike_threat",
            week = 5,
            title = "Union Threatens Mid-Season Strike Over Travel Fatigue",
            category = "Labor Union Relations",
            urgencyTag = "STRIKE THREAT CRITICAL",
            description = "The Players Union files an official grievance over grueling back-to-back road trips and short recovery intervals. The Union President threatens a walkout on next week's televised double-header unless concessions are made.",
            choices = listOf(
                CommissionerChoice(
                    label = "Fund $10M Luxury Charter Travel & Wellness Protocol",
                    narrativeOutcome = "Union stands down happily! Players morale reaches all-time high, but team owners grumble over expenses.",
                    cashImpactMillions = -10L,
                    fanMoraleImpact = +6,
                    unionSatisfactionImpact = +25,
                    integrityImpact = +5,
                    ownerApprovalImpact = -10
                ),
                CommissionerChoice(
                    label = "Adjust Schedule: Extend Season by 1 Week",
                    narrativeOutcome = "Broadcast partner charges a reschedule penalty, but strike risk is eliminated and players recover well.",
                    cashImpactMillions = -4L,
                    fanMoraleImpact = +2,
                    unionSatisfactionImpact = +15,
                    integrityImpact = +2,
                    ownerApprovalImpact = -2
                ),
                CommissionerChoice(
                    label = "Hardline Stance: Threaten Legal Injunction & Lockout",
                    narrativeOutcome = "Owners cheer the iron-fisted leadership, but player trust collapses and strike risk simmers dangerously.",
                    cashImpactMillions = 0L,
                    fanMoraleImpact = -12,
                    unionSatisfactionImpact = -30,
                    integrityImpact = -8,
                    ownerApprovalImpact = +18
                )
            )
        ),
        CommissionerDilemma(
            id = "dil_broadcaster_renegotiation",
            week = 8,
            title = "Streaming Giant Offers $120M Exclusive Buyout Bid",
            category = "Broadcast Rights",
            urgencyTag = "COMMERCIAL OPPORTUNITY",
            description = "A global streaming conglomerate offers an unprecedented $120M/yr broadcast rights deal, but demands exclusive broadcast rights that would take games off traditional public television.",
            choices = listOf(
                CommissionerChoice(
                    label = "Accept Exclusive Stream Deal ($120M Payout)",
                    narrativeOutcome = "League coffers overflow with cash! Older fans complain about app subscription paywalls, but league valuation surges.",
                    cashImpactMillions = +35L,
                    fanMoraleImpact = -6,
                    unionSatisfactionImpact = +12,
                    integrityImpact = 0,
                    ownerApprovalImpact = +22
                ),
                CommissionerChoice(
                    label = "Reject: Retain Free-to-Air Public Broadcaster",
                    narrativeOutcome = "Domestic ratings soar as games stay free. Grassroots fan adoption reaches new heights.",
                    cashImpactMillions = 0L,
                    fanMoraleImpact = +16,
                    unionSatisfactionImpact = -4,
                    integrityImpact = +6,
                    ownerApprovalImpact = -8
                ),
                CommissionerChoice(
                    label = "Counter: Hybrid Deal (Stream + Cable Simulcast)",
                    narrativeOutcome = "Masterstroke diplomacy! Secures $90M while preserving cable accessibility.",
                    cashImpactMillions = +18L,
                    fanMoraleImpact = +8,
                    unionSatisfactionImpact = +8,
                    integrityImpact = +5,
                    ownerApprovalImpact = +12
                )
            )
        ),
        CommissionerDilemma(
            id = "dil_owner_tampering",
            week = 11,
            title = "Las Vegas Owner Caught In Illegal Secret Player Tampering",
            category = "Regulatory Challenge",
            urgencyTag = "REGULATORY INQUIRY",
            description = "Leaked audio reveals the Las Vegas owner offered illegal private equity equity and luxury housing to sign Chicago's franchise star before free agency opens. Chicago owners demand immediate severe sanctions.",
            choices = listOf(
                CommissionerChoice(
                    label = "Strip Las Vegas of #1 Draft Pick + $8M Fine",
                    narrativeOutcome = "Decisive ruling restores competitive balance. Owners realize no one is above the law.",
                    cashImpactMillions = +8L,
                    fanMoraleImpact = +6,
                    unionSatisfactionImpact = +4,
                    integrityImpact = +22,
                    ownerApprovalImpact = +5
                ),
                CommissionerChoice(
                    label = "Order Private Mediation & Financial Settlement",
                    narrativeOutcome = "Vegas pays Chicago a $4M compensation fee. Matter is kept out of public headlines.",
                    cashImpactMillions = +2L,
                    fanMoraleImpact = 0,
                    unionSatisfactionImpact = +2,
                    integrityImpact = -6,
                    ownerApprovalImpact = +8
                ),
                CommissionerChoice(
                    label = "Dismiss for Lack of Formal Physical Evidence",
                    narrativeOutcome = "Avoids a prolonged legal battle with Vegas, but small-market teams accuse the league of bias.",
                    cashImpactMillions = 0L,
                    fanMoraleImpact = -10,
                    unionSatisfactionImpact = -8,
                    integrityImpact = -25,
                    ownerApprovalImpact = -15
                )
            )
        ),
        CommissionerDilemma(
            id = "dil_international_expansion_bid",
            week = 14,
            title = "London Investment Group Submits Official Franchise Bid",
            category = "Market Expansion",
            urgencyTag = "HISTORIC EXPANSION",
            description = "A prestigious British sports ownership consortium submits a formal bid to establish an expansion franchise in London, offering a staggering $160M expansion fee and a state-of-the-art arena lease.",
            choices = listOf(
                CommissionerChoice(
                    label = "Approve London Expansion Franchise (Collect $160M)",
                    narrativeOutcome = "Historic milestone! The league becomes a true transatlantic powerhouse. Travel logistics will need careful planning.",
                    cashImpactMillions = +160L,
                    fanMoraleImpact = +18,
                    unionSatisfactionImpact = +10,
                    integrityImpact = +8,
                    ownerApprovalImpact = +25
                ),
                CommissionerChoice(
                    label = "Decline for Now: Prioritize Domestic High-Growth Cities",
                    narrativeOutcome = "Preserves simple domestic travel corridors. Focus shifts to booming domestic hubs like Austin & Atlanta.",
                    cashImpactMillions = 0L,
                    fanMoraleImpact = -2,
                    unionSatisfactionImpact = +5,
                    integrityImpact = +2,
                    ownerApprovalImpact = -5
                ),
                CommissionerChoice(
                    label = "Offer London an Annual 4-Game Exhibition Showcase First",
                    narrativeOutcome = "Low risk trial that generates $25M in international sponsorship while testing player travel endurance.",
                    cashImpactMillions = +25L,
                    fanMoraleImpact = +10,
                    unionSatisfactionImpact = +4,
                    integrityImpact = +5,
                    ownerApprovalImpact = +14
                )
            )
        )
    )

    fun getDefaultLeagueRules(): List<LeagueRule> = listOf(
        LeagueRule(
            id = "rule_salary_cap",
            name = "Strict Salary Cap & 100% Luxury Tax",
            category = RuleCategory.FINANCIAL_PARITY,
            summary = "Imposes a hard $80M payroll limit with dollar-for-dollar punitive luxury taxes for exceeding.",
            isEnacted = true,
            parityImpact = +18,
            scoringTempoModifier = 0,
            integrityImpact = +10,
            fanAppealModifier = +8,
            smallMarketBenefitRating = 5,
            description = "Compresses roster spending disparity between market tiers. Prevents mega-wealthy media markets from hoarding all top-tier talent.",
            enactedEffect = "Roster ratings disparity between top and bottom franchises reduced by 45%. Parity score +18.",
            repealedEffect = "Unrestricted free market. Large-market clubs spend $150M+ payrolls; small-market teams become feeder clubs.",
            proPerspective = "Owners Association: Guarantees fiscal stability and prevents destructive spending arms races.",
            contraPerspective = "Big Market Moguls: Artificially restricts free enterprise and prevents fielding global superteams."
        ),
        LeagueRule(
            id = "rule_draft_lottery",
            name = "Weighted Draft Lottery Anti-Tanking System",
            category = RuleCategory.DRAFT_TALENT,
            summary = "Replaces automatic #1 pick for worst record with weighted lottery odds among bottom four clubs.",
            isEnacted = true,
            parityImpact = +15,
            scoringTempoModifier = 0,
            integrityImpact = +14,
            fanAppealModifier = +10,
            smallMarketBenefitRating = 4,
            description = "Deters late-season intentional losing (tanking) while still giving struggling franchises a realistic path to draft generational stars.",
            enactedEffect = "Eliminates cynical late-season tanking. Competitive integrity +14 and late-season TV viewership +10%.",
            repealedEffect = "Strict reverse standings. Worst team is guaranteed the #1 draft pick, encouraging blatant tanking down the stretch.",
            proPerspective = "Fans & Broadcasters: Every match matters down to the final whistle of the regular season.",
            contraPerspective = "Rebuilding GMs: Lowers certainty when executing a multi-year teardown and rebuilding strategy."
        ),
        LeagueRule(
            id = "rule_revenue_sharing",
            name = "Small-Market Equalization & Gate Sharing",
            category = RuleCategory.FINANCIAL_PARITY,
            summary = "Transfers 25% of top-tier local media and premium gate revenues into an equalization development pool.",
            isEnacted = true,
            parityImpact = +14,
            scoringTempoModifier = 0,
            integrityImpact = +8,
            fanAppealModifier = +6,
            smallMarketBenefitRating = 5,
            description = "Subsidizes franchise infrastructure, analytics departments, and scouting networks for clubs in smaller metropolitan areas.",
            enactedEffect = "Small-market franchises maintain sustainable positive cash flow, preventing insolvency or relocation distress.",
            repealedEffect = "Each team keeps 100% of local revenues. Small markets post annual operating losses and slash payroll.",
            proPerspective = "Small Market Owners: Vital lifeblood that enables competitive balance across all 30 potential markets.",
            contraPerspective = "High Revenue Owners: Punishes franchises with superior marketing and stadium ticket demand."
        ),
        LeagueRule(
            id = "rule_rookie_wage_scale",
            name = "Structured Rookie Wage Scale & Cap Holds",
            category = RuleCategory.FINANCIAL_PARITY,
            summary = "Sets predetermined 4-year salary brackets for drafted players with restricted free agency team options.",
            isEnacted = true,
            parityImpact = +11,
            scoringTempoModifier = 0,
            integrityImpact = +7,
            fanAppealModifier = +4,
            smallMarketBenefitRating = 4,
            description = "Prevents rookies from holding out for astronomical unearned contracts before playing a single official regular-season game.",
            enactedEffect = "Drafting teams retain star rookies for 4+ seasons, allowing small markets to build organic championship contenders.",
            repealedEffect = "Wild West rookie bidding where deep-pocketed owners sign unproven college sensations directly to $25M deals.",
            proPerspective = "General Managers: Provides cost-certainty and prevents crippling rookie bust contracts.",
            contraPerspective = "Players Union: Restricts earning power of generational young superstars during peak physical years."
        ),
        LeagueRule(
            id = "rule_play_in_tournament",
            name = "Post-Season Play-In Tournament (Seeds 7–10)",
            category = RuleCategory.DRAFT_TALENT,
            summary = "Introduces a high-stakes 4-team elimination tournament for the final two playoff seeds.",
            isEnacted = true,
            parityImpact = +10,
            scoringTempoModifier = +4,
            integrityImpact = +5,
            fanAppealModifier = +16,
            smallMarketBenefitRating = 4,
            description = "Keeps up to 80% of franchises in meaningful playoff contention well into the final weeks of the regular season calendar.",
            enactedEffect = "Massive spike in late-season attendance, broadcast ratings, and sudden-death elimination drama.",
            repealedEffect = "Top 6 teams cruise into playoffs while middle seeds go through the motions in weeks 12-16.",
            proPerspective = "Broadcast Networks: Play-in games deliver championship-caliber television viewership and ad revenue.",
            contraPerspective = "Top Seed Coaches: Added games fatigue fringe contenders and dilute traditional regular-season merit."
        ),
        LeagueRule(
            id = "rule_shot_clock",
            name = "High-Tempo Shot Clock & Action Accelerometer",
            category = RuleCategory.GAMEPLAY_PACE,
            summary = "Strict 24-second possession clock and rapid restart rules force continuous fast-paced offensive play.",
            isEnacted = true,
            parityImpact = 0,
            scoringTempoModifier = +15,
            integrityImpact = 0,
            fanAppealModifier = +14,
            smallMarketBenefitRating = 3,
            description = "Eliminates stalling and defensive slowdown tactics, boosting offensive scoring tempo and highlight reel frequency.",
            enactedEffect = "League scoring increases by +15 points per contest; game broadcast duration trimmed by 18 minutes.",
            repealedEffect = "Teams deploy deliberate stall ball and clock bleeding tactics, resulting in low-scoring defensive slugfests.",
            proPerspective = "Youth Fanbase: Electrifying pace, rapid transition scoring, and viral digital social media highlights.",
            contraPerspective = "Veteran Defensive Coaches: Devalues methodical set-piece half-court game plans."
        ),
        LeagueRule(
            id = "rule_instant_replay",
            name = "Coach's Video Review & Central Command Center",
            category = RuleCategory.GAMEPLAY_PACE,
            summary = "Equips head coaches with 2 challenge flags per match, verified via a centralized high-definition video war room.",
            isEnacted = true,
            parityImpact = +6,
            scoringTempoModifier = -3,
            integrityImpact = +18,
            fanAppealModifier = +5,
            smallMarketBenefitRating = 3,
            description = "Ensures critical out-of-bounds, foul, and scoring decisions in the final two minutes are officiated with zero error.",
            enactedEffect = "Eliminates game-deciding referee blunders. Integrity score +18; match fairness at all-time high.",
            repealedEffect = "Human referee errors stand as final. Faster game flow, but frequent officiating controversies erupt.",
            proPerspective = "Commissioners & Integrity Regulators: Guarantees competitive legitimacy on the world stage.",
            contraPerspective = "Arena Spectators: Occasional 2-minute review stoppages disrupt in-stadium emotional momentum."
        ),
        LeagueRule(
            id = "rule_freedom_of_movement",
            name = "Perimeter Freedom of Movement (Anti-Hand-Checking)",
            category = RuleCategory.GAMEPLAY_PACE,
            summary = "Strictly penalizes defensive contact on perimeter ball-handlers and cutters without legal positioning.",
            isEnacted = true,
            parityImpact = -4,
            scoringTempoModifier = +12,
            integrityImpact = +6,
            fanAppealModifier = +12,
            smallMarketBenefitRating = 2,
            description = "Protects skilled offensive playmakers from rough defensive hand-checking, opening up perimeter driving lanes.",
            enactedEffect = "Superstar offensive numbers skyrocket (+12 PPG). Three-point and transition efficiency peak.",
            repealedEffect = "Brutal physical defense dominates. Hand-checking slows down superstar scoring outputs.",
            proPerspective = "Marketing Executives: Showcases supreme individual player skill, agility, and offensive scoring brilliance.",
            contraPerspective = "Defensive Specialists: Makes perimeter defense borderline impossible against elite ball-handlers."
        ),
        LeagueRule(
            id = "rule_anti_doping",
            name = "Olympic-Standard Randomized Anti-Doping & PED Testing",
            category = RuleCategory.INTEGRITY_CONDUCT,
            summary = "Year-round unannounced blood and urine testing administered by independent international anti-doping authorities.",
            isEnacted = true,
            parityImpact = +8,
            scoringTempoModifier = -2,
            integrityImpact = +20,
            fanAppealModifier = +5,
            smallMarketBenefitRating = 3,
            description = "Establishes a clean sport standard with automatic 25-game unpaid suspensions for first-time banned substance violations.",
            enactedEffect = "Protects athlete health, establishes pristine corporate sponsorship appeal, and eliminates PED scandals.",
            repealedEffect = "Internal testing only with discretionary fines. Puts league at severe risk of public doping scandals.",
            proPerspective = "Global Sponsors & Medical Regulators: Pure, clean athletic competition worthy of global family audiences.",
            contraPerspective = "Franchise Medical Staff: False positives or bureaucratic overreach can sideline star players unexpectedly."
        ),
        LeagueRule(
            id = "rule_anti_gambling",
            name = "Zero-Tolerance Sports Betting & Anti-Tampering Shield",
            category = RuleCategory.INTEGRITY_CONDUCT,
            summary = "Strict lifetime ban for any insider sports wagering, line manipulation, or covert player recruitment tampering.",
            isEnacted = true,
            parityImpact = +5,
            scoringTempoModifier = 0,
            integrityImpact = +22,
            fanAppealModifier = +8,
            smallMarketBenefitRating = 3,
            description = "Monitors real-time global betting exchange movements and audits digital communications during off-season transfer windows.",
            enactedEffect = "Zero tampering or match-fixing threats. International broadcast integrity certified at highest institutional tier.",
            repealedEffect = "Lax enforcement allows aggressive tampering by big-city teams and invites sports betting integrity doubts.",
            proPerspective = "League Integrity Directorate: Absolute firewall protecting the sanctity and unpredictability of sport.",
            contraPerspective = "Aggressive General Managers: Strictly restricts informal networking and early recruitment discussions."
        ),
        LeagueRule(
            id = "rule_geographic_realignment",
            name = "Mandatory Geographic Realignment & Travel Balancing",
            category = RuleCategory.DRAFT_TALENT,
            summary = "Enforces regional conference and division alignment to curb flight fatigue and foster fierce local rivalries.",
            isEnacted = true,
            parityImpact = +12,
            scoringTempoModifier = 0,
            integrityImpact = +10,
            fanAppealModifier = +8,
            smallMarketBenefitRating = 5,
            description = "Mandates that franchises align strictly within regional travel corridors, reducing coast-to-coast fatigue by 30% and building natural divisional rivalries.",
            enactedEffect = "Team road trip fatigue reduced by 30%. Intense regional divisional rivalries generate +15% ticket demand and balanced conference parity.",
            repealedEffect = "Open circuit alignment. Teams can be aligned without geographic limits, risking exhausting cross-country road trips.",
            proPerspective = "Players Association: Greatly reduces travel exhaustion and protects athlete recovery between away games.",
            contraPerspective = "National Broadcasters: Prefer custom super-circuits pairing New York and Los Angeles in the same division."
        ),
        LeagueRule(
            id = "rule_division_auto_playoffs",
            name = "Division Champions Automatic Playoff Seeding",
            category = RuleCategory.DRAFT_TALENT,
            summary = "Guarantees top playoff seeds to division winners, protecting division races from wild-card dilution.",
            isEnacted = true,
            parityImpact = +10,
            scoringTempoModifier = 0,
            integrityImpact = +6,
            fanAppealModifier = +14,
            smallMarketBenefitRating = 4,
            description = "Every division crown carries an automatic ticket to the postseason, ensuring every divisional matchup carries championship weight.",
            enactedEffect = "Division title races stay fiercely contested until the final week of the regular season calendar.",
            repealedEffect = "Pure overall win percentage decides playoff berths; division titles become purely symbolic ribbons.",
            proPerspective = "Regional Fanbases: Winning the local division crown creates an unforgettable annual celebration and playoff berth.",
            contraPerspective = "Statistical Pundits: A weaker division winner might host a higher-win wild card challenger."
        ),
        LeagueRule(
            id = "rule_trade_deadline",
            name = "Mid-Season In-Season Trade Deadline",
            category = RuleCategory.FINANCIAL_PARITY,
            summary = "Enforces an official cutoff week after which no in-season trades can be conducted until the off-season.",
            isEnacted = true,
            parityImpact = +14,
            scoringTempoModifier = 0,
            integrityImpact = +12,
            fanAppealModifier = +15,
            smallMarketBenefitRating = 5,
            description = "Prevents playoff-bound superteams from raiding eliminated teams for star talent on the eve of the championship tournament.",
            enactedEffect = "Creates thrilling Trade Deadline Week drama and forces teams to build organic roster continuity heading into the postseason.",
            repealedEffect = "Year-round unrestricted trades. Contenders buy stars minutes before the finals, damaging competitive integrity.",
            proPerspective = "Competitive Parity Board: Crucial barrier against late-season mercenary rentals and tanking fire-sales.",
            contraPerspective = "Desperate Contenders: Cannot patch sudden late-season injuries via blockbuster trades."
        ),
        LeagueRule(
            id = "rule_draft_pick_trading",
            name = "Future Draft Capital Market Exchange",
            category = RuleCategory.DRAFT_TALENT,
            summary = "Authorizes franchises to trade future 1st and 2nd round draft picks up to 3 seasons in advance.",
            isEnacted = true,
            parityImpact = +8,
            scoringTempoModifier = 0,
            integrityImpact = +6,
            fanAppealModifier = +12,
            smallMarketBenefitRating = 4,
            description = "Allows rebuilding small-market franchises to stockpile blue-chip future draft equity in exchange for veteran players.",
            enactedEffect = "Enables dynamic trade negotiations with draft pick protection clauses and multi-asset blockbusters.",
            repealedEffect = "Player-for-player trades only. Eliminates draft picks from trade market, restricting rebuilding flexibility.",
            proPerspective = "Rebuilding General Managers: Primary mechanism to rebuild a depleted franchise into a sustainable powerhouse.",
            contraPerspective = "Traditionalist Owners: Reckless GMs might mortgage a decade of future draft capital for short-term wins."
        )
    )
}
