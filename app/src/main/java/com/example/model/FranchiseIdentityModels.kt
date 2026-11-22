package com.example.model

import kotlin.random.Random

data class FranchiseBrandColor(
    val id: String,
    val name: String,
    val hex: Long
)

object FranchiseIdentityEngine {

    val brandColorPalette: List<FranchiseBrandColor> = listOf(
        FranchiseBrandColor("color_deep_royal", "Deep Royal", 0xFF1E3A8AL),
        FranchiseBrandColor("color_electric_blue", "Electric Blue", 0xFF2563EBL),
        FranchiseBrandColor("color_pacific_blue", "Pacific Blue", 0xFF0284C7L),
        FranchiseBrandColor("color_cyan_pulse", "Cyan Pulse", 0xFF06B6D4L),
        FranchiseBrandColor("color_harbor_teal", "Harbor Teal", 0xFF0D9488L),
        FranchiseBrandColor("color_emerald_green", "Emerald Green", 0xFF059669L),
        FranchiseBrandColor("color_forest_pine", "Forest Pine", 0xFF15803DL),
        FranchiseBrandColor("color_volt_lime", "Volt Lime", 0xFF84CC16L),
        FranchiseBrandColor("color_championship_gold", "Championship Gold", 0xFFFACC15L),
        FranchiseBrandColor("color_electric_amber", "Electric Amber", 0xFFF59E0BL),
        FranchiseBrandColor("color_vegas_bronze", "Vegas Bronze", 0xFFD97706L),
        FranchiseBrandColor("color_blazing_orange", "Blazing Orange", 0xFFEA580CL),
        FranchiseBrandColor("color_crimson_red", "Crimson Red", 0xFFDC2626L),
        FranchiseBrandColor("color_burgundy_wine", "Burgundy Wine", 0xFF991B1BL),
        FranchiseBrandColor("color_scarlet_rose", "Scarlet Rose", 0xFFE11D48L),
        FranchiseBrandColor("color_miami_pink", "Miami Pink", 0xFFEC4899L),
        FranchiseBrandColor("color_violet_neon", "Violet Neon", 0xFF7C3AEDL),
        FranchiseBrandColor("color_indigo_surge", "Indigo Surge", 0xFF4F46E5L),
        FranchiseBrandColor("color_imperial_purple", "Imperial Purple", 0xFF581C87L),
        FranchiseBrandColor("color_heritage_copper", "Heritage Copper", 0xFF78350FL),
        FranchiseBrandColor("color_midnight_navy", "Midnight Navy", 0xFF0F172AL),
        FranchiseBrandColor("color_onyx_black", "Onyx Black", 0xFF111827L),
        FranchiseBrandColor("color_steel_slate", "Steel Slate", 0xFF64748BL),
        FranchiseBrandColor("color_frost_white", "Frost White", 0xFFF8FAFC)
    )

    private val cityThemedNicknames: Map<String, List<String>> = mapOf(
        "london" to listOf("Monarchs", "Royals", "Lions", "Sovereigns", "Knights"),
        "tokyo" to listOf("Shoguns", "Samurai", "Kitsune", "Rising", "Ronin"),
        "mexico city" to listOf("Aztecas", "Jaguars", "Condors", "Sol", "Diablos"),
        "monterrey" to listOf("Sultans", "Regios", "Condors", "Lancers"),
        "berlin" to listOf("Eagles", "Vanguards", "Bears", "Ironclads"),
        "toronto" to listOf("Northmen", "Huskies", "Timberwolves", "Sentinels"),
        "vancouver" to listOf("Cascadia", "Grizzlies", "Mariners", "Orcas"),
        "calgary" to listOf("Stampeders", "Outlaws", "Ranchers", "Wranglers"),
        "atlanta" to listOf("Phoenix", "Firebirds", "Peaches", "Vanguards"),
        "nashville" to listOf("Sounds", "Outlaws", "Rhythm", "Strings"),
        "paris" to listOf("Lumiere", "Etoile", "Musketeers", "Monarchs"),
        "boston" to listOf("Minutemen", "Patriots", "Harbormen", "Mariners"),
        "san francisco" to listOf("Founders", "Fog", "Goldminers", "Admirals"),
        "miami" to listOf("Flamingos", "Tides", "Corsairs", "Hurricanes"),
        "phoenix" to listOf("Scorpions", "Coyotes", "Sidewinders", "Suns"),
        "san diego" to listOf("Breakers", "Admirals", "Mariners", "Surf"),
        "philadelphia" to listOf("Liberty", "Minutemen", "Founders", "Bellringers"),
        "charlotte" to listOf("Crowns", "Monarchs", "Hornets", "Aviators"),
        "salt lake city" to listOf("Peaks", "Avalanche", "Pioneers", "Mountaineers"),
        "portland" to listOf("Pioneers", "Trailblazers", "Timberwolves", "Lumberjacks"),
        "san antonio" to listOf("Guardians", "Rangers", "Mustangs", "Spurs"),
        "kansas city" to listOf("Royals", "Monarchs", "Wranglers", "Cyclones"),
        "seoul" to listOf("Tigers", "Dragons", "Guardians", "Emperors"),
        "sydney" to listOf("Harbor", "Mariners", "Sharks", "Southern Cross"),
        "melbourne" to listOf("Rangers", "Victors", "Southern Stars", "Brigade"),
        "singapore" to listOf("Merlions", "Griffins", "Voyagers", "Tigers"),
        "manila" to listOf("Typhoons", "Eagles", "Voyagers", "Archipelago"),
        "são paulo" to listOf("Imperials", "Jaguars", "Toucans", "Paulistas"),
        "rio de janeiro" to listOf("Samba", "Condors", "Corsairs", "Tides"),
        "buenos aires" to listOf("Condors", "Gauchos", "Pampas", "Stallions"),
        "denver" to listOf("Avalanche", "Summit", "Mountaineers", "Outlaws"),
        "dallas" to listOf("Mavericks", "Mustangs", "Wranglers", "Rangers"),
        "houston" to listOf("Comets", "Voyagers", "Astronauts", "Barons")
    )

    private val sportThemedNicknames: Map<SportType, List<String>> = mapOf(
        SportType.BASKETBALL to listOf(
            "Surge", "Hoopsters", "Flight", "Phantoms", "Raptors", "Suns", "Reign", "Crowns"
        ),
        SportType.FOOTBALL to listOf(
            "Dreadnoughts", "Brigade", "Stallions", "Outlaws", "Centurions", "Sentinels", "Gladiators", "Wolverines"
        ),
        SportType.SOCCER to listOf(
            "United", "Athletic", "Sporting", "Dynamo", "Wanderers", "Rovers", "Olympique", "Rangers"
        ),
        SportType.HOCKEY to listOf(
            "Avalanche", "Timberwolves", "Kodiak", "Grizzlies", "Northmen", "Tempest", "Icebreakers", "Huskies"
        )
    )

    private val universalRealisticNicknames: List<String> = listOf(
        "Monarchs", "Vanguards", "Stallions", "Sentinels", "Raptors", "Comets",
        "Avalanche", "Outlaws", "Admirals", "Cyclones", "Corsairs", "Griffins",
        "Timberwolves", "Thunderbirds", "Reign", "Dragons", "Mavericks", "Spartans",
        "Phantoms", "Knights", "Barracudas", "Voyagers", "Founders", "Guardians",
        "Trailblazers", "Express", "Huskies", "Condors", "Ocelots", "Wolverines",
        "Scorpions", "Brigade", "Kodiak", "Minutemen", "Tempest", "Grizzlies",
        "Falcons", "Pioneers", "Gladiators", "Dreadnoughts", "Renegades", "Phoenix",
        "Royals", "Surge", "Mustangs", "Strikers", "Wolves", "Centurions",
        "Hurricanes", "Crusaders", "Tornadoes", "Mariners", "Barons", "Captains",
        "Imperials", "Aviators", "Harbormen", "Highlanders", "Lancers", "Rangers"
    )

    fun colorNameFor(hex: Long): String =
        brandColorPalette.find { it.hex == hex }?.name ?: "Custom Color"

    fun isLightColor(hex: Long): Boolean {
        val r = ((hex shr 16) and 0xFF) / 255.0
        val g = ((hex shr 8) and 0xFF) / 255.0
        val b = (hex and 0xFF) / 255.0
        val luminance = (0.299 * r) + (0.587 * g) + (0.114 * b)
        return luminance > 0.68
    }

    fun defaultSecondaryFor(primaryHex: Long): Long {
        val preferredMatches = mapOf(
            0xFF1E3A8AL to 0xFFF59E0BL, // Deep Royal -> Electric Amber
            0xFF7C3AEDL to 0xFFFACC15L, // Violet Neon -> Championship Gold
            0xFFDC2626L to 0xFF111827L, // Crimson Red -> Onyx Black
            0xFFF59E0BL to 0xFF0F172AL, // Electric Amber -> Midnight Navy
            0xFF059669L to 0xFFD97706L, // Emerald Green -> Vegas Bronze
            0xFF0284C7L to 0xFF84CC16L, // Pacific Blue -> Volt Lime
            0xFF06B6D4L to 0xFF0F172AL, // Cyan Pulse -> Midnight Navy
            0xFF8B5CF6L to 0xFFF8FAFC   // Frost White accent
        )
        val mapped = preferredMatches[primaryHex]
        if (mapped != null && mapped != primaryHex) return mapped
        return if (isLightColor(primaryHex)) {
            if (primaryHex != 0xFF0F172AL) 0xFF0F172AL else 0xFF1E3A8AL
        } else {
            if (primaryHex != 0xFFFACC15L) 0xFFFACC15L else 0xFFF8FAFC
        }
    }

    fun normalizeColorPair(primaryHex: Long, secondaryHex: Long?): Pair<Long, Long> {
        val chosenPrimary = primaryHex
        val candidateSecondary = secondaryHex ?: defaultSecondaryFor(chosenPrimary)
        val chosenSecondary = if (candidateSecondary == chosenPrimary) {
            defaultSecondaryFor(chosenPrimary)
        } else {
            candidateSecondary
        }
        return chosenPrimary to chosenSecondary
    }

    fun generateRandomColorPair(
        currentPrimaryHex: Long? = null,
        currentSecondaryHex: Long? = null,
        random: Random = Random.Default
    ): Pair<Long, Long> {
        val primaryPool = brandColorPalette.filter { it.hex != currentPrimaryHex }
        val primary = primaryPool.random(random).hex
        val secondaryPool = brandColorPalette.filter {
            it.hex != primary && !(it.hex == currentSecondaryHex && primary == currentPrimaryHex)
        }
        val secondary = secondaryPool.random(random).hex
        return primary to secondary
    }

    fun generateRandomRealisticNickname(
        city: MarketCity? = null,
        existingFranchises: List<Franchise> = emptyList(),
        sportType: SportType = SportType.BASKETBALL,
        excludeNickname: String? = null,
        random: Random = Random.Default
    ): String {
        val taken = existingFranchises.map { it.nickname.trim().lowercase() }.toSet()
        val excluded = excludeNickname?.trim()?.lowercase()
        val cityKey = city?.name?.trim()?.lowercase()
        val citySuggestions = if (cityKey != null) cityThemedNicknames[cityKey].orEmpty() else emptyList()
        val sportSuggestions = sportThemedNicknames[sportType].orEmpty()

        val combinedPool = (citySuggestions + sportSuggestions + universalRealisticNicknames)
            .distinct()
            .filter { cand ->
                val norm = cand.trim().lowercase()
                norm !in taken && (excluded == null || norm != excluded)
            }

        if (combinedPool.isNotEmpty()) {
            // Favor city-specific options early while keeping full realistic variety across clicks
            val weighted = if (citySuggestions.any { it.trim().lowercase() !in taken && it.trim().lowercase() != excluded }) {
                val freshCity = citySuggestions.filter {
                    val norm = it.trim().lowercase()
                    norm !in taken && norm != excluded
                }
                if (random.nextFloat() < 0.45f) freshCity else combinedPool
            } else {
                combinedPool
            }
            return weighted.random(random)
        }

        val fallback = universalRealisticNicknames.filter { it.trim().lowercase() != excluded }
        return if (fallback.isNotEmpty()) fallback.random(random) else "Vanguards"
    }
}
