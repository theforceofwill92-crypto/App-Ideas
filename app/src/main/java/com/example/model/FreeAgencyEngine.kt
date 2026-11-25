package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

data class FreeAgentPlayer(
    val id: String,
    val name: String,
    val position: String,
    val overallRating: Int,
    val age: Int,
    val archetype: String,
    val experienceYears: Int,
    val askingSalaryMillions: Long,
    val preferredYears: Int,
    val formerFranchiseName: String,
    val bioTag: String
) {
    val marketTierLabel: String
        get() = when {
            overallRating >= 84 -> "Marquee Starter"
            overallRating >= 79 -> "Quality Rotation"
            overallRating >= 74 -> "Veteran Depth"
            else -> "Emergency / Call-Up"
        }

    val tierColorHex: Long
        get() = when {
            overallRating >= 84 -> 0xFFD4AF37
            overallRating >= 79 -> 0xFF10B981
            overallRating >= 74 -> 0xFF3B82F6
            else -> 0xFF94A3B8
        }
}

data class ContractOfferTerms(
    val salaryMillions: Long,
    val contractYears: Int,
    val contractTypeLabel: String,
    val totalContractValueMillions: Long,
    val isCapCompliant: Boolean,
    val complianceNote: String
)

data class FreeAgentSigningTransaction(
    val id: String,
    val playerId: String,
    val playerName: String,
    val position: String,
    val overallRating: Int,
    val age: Int,
    val archetype: String,
    val franchiseId: String,
    val franchiseName: String,
    val salaryMillions: Long,
    val contractYears: Int,
    val contractTypeLabel: String,
    val reason: String,
    val week: Int,
    val seasonYear: Int,
    val isAutoSigned: Boolean = true
)

data class DepthChartNeedSummary(
    val franchiseId: String,
    val healthyCount: Int,
    val injuredOutCount: Int,
    val maxRosterSlots: Int,
    val targetDepthCount: Int,
    val openRosterSlots: Int,
    val urgentHolesCount: Int,
    val neededPositions: List<String>,
    val primaryReason: String,
    val priorityScore: Int
)

data class SingleFreeAgencySigningResult(
    val success: Boolean,
    val message: String,
    val updatedFranchise: Franchise? = null,
    val updatedPool: List<FreeAgentPlayer> = emptyList(),
    val transaction: FreeAgentSigningTransaction? = null,
    val newsHeadline: String? = null
)

data class FreeAgencySweepResult(
    val updatedFranchises: List<Franchise>,
    val updatedPool: List<FreeAgentPlayer>,
    val newSignings: List<FreeAgentSigningTransaction>,
    val headlines: List<String>
)

object FreeAgencyEngine {

    private val firstNames = listOf(
        "Andre", "Kieran", "Devon", "Trey", "Hassan", "Milos", "Jevon", "Kameron",
        "Nolan", "Rohan", "Cedric", "Gideon", "Tariq", "Enzo", "Darius", "Collin",
        "Braxton", "Soren", "Keegan", "Rafael", "Jonas", "Matteo", "Dominic", "Zion",
        "Quentin", "Lamar", "Victor", "Tyson"
    )

    private val lastNames = listOf(
        "Bellamy", "Callahan", "Delgado", "kovacs", "Hargrove", "Nakamura", "Okafor",
        "Petrov", "Quintero", "Rutherford", "Sandoval", "Tavares", "Whitaker", "Yates",
        "Zimmerman", "Almeida", "Beaumont", "Cunningham", "Drummond", "Fletcher",
        "Gallagher", "Hendricks", "Ibrahim", "Kessler", "Montoya", "Nolan", "Pendleton", "Vargas"
    ).map { it.replaceFirstChar { c -> c.uppercase() } }

    private fun formerClubsForSport(sportType: SportType): List<String> = listOf(
        "Free Agent Pool",
        "${sportType.minorLeagueName} Call-Up",
        "League Waiver Wire",
        "International Pro Release",
        "Pacific Premier Club",
        "Buyout Market Veteran"
    )

    private val bioTags = listOf(
        "Proven Playoff Contributor",
        "High-Motor Injury Replacement",
        "Cap-Friendly Rotation Fit",
        "Veteran Leadership Presence",
        "Two-Way Depth Specialist",
        "Impact Bench Contributor"
    )

    fun positionsForSport(sportType: SportType): List<String> = sportType.starterPositions

    fun generateInitialFreeAgentPool(
        sportType: SportType,
        seasonYear: Int,
        count: Int = 24,
        excludeNames: Set<String> = emptySet(),
        seedOffset: Int = 0
    ): List<FreeAgentPlayer> {
        val positions = positionsForSport(sportType)
        val usedNames = excludeNames.toMutableSet()
        val pool = mutableListOf<FreeAgentPlayer>()
        val rng = Random(seasonYear * 97 + sportType.ordinal * 31 + seedOffset * 13)

        for (i in 0 until count) {
            var attempt = 0
            var name: String
            do {
                val fn = firstNames[(i * 3 + seedOffset + attempt + rng.nextInt(firstNames.size)) % firstNames.size]
                val ln = lastNames[(i * 5 + seedOffset + attempt * 2 + rng.nextInt(lastNames.size)) % lastNames.size]
                name = "$fn $ln"
                attempt++
            } while (!usedNames.add(name) && attempt < 30)

            // Distribute realistic OVR tiers: a few top marquee starters, solid rotation veterans, and affordable depth
            val ovr = when {
                i < 3 -> 85 + (2 - i) // 87, 86, 85
                i < 8 -> 80 + ((8 - i) % 5) // 80..84
                i < 16 -> 75 + ((16 - i) % 5) // 75..79
                else -> 70 + ((count - i) % 5) // 70..74
            }.coerceIn(69, 89)

            val age = when {
                ovr >= 84 -> 26 + (i % 5) // 26..30 prime veterans
                ovr >= 78 -> 24 + (i % 8) // 24..31
                else -> 21 + (i % 13) // 21..33
            }.coerceIn(21, 35)

            val experience = (age - 20).coerceIn(1, 14)
            val baseAsking = computeBaseMarketSalary(ovr, age)
            val prefYears = computePreferredContractYears(ovr, age)
            val pos = positions[i % positions.size]
            val sportArchetypes = sportType.archetypes
            val arch = sportArchetypes[(i + rng.nextInt(sportArchetypes.size)) % sportArchetypes.size]
            val clubs = formerClubsForSport(sportType)
            val former = clubs[(i + seedOffset) % clubs.size]
            val bio = bioTags[(i + rng.nextInt(bioTags.size)) % bioTags.size]

            pool.add(
                FreeAgentPlayer(
                    id = "fa_${seasonYear}_${seedOffset}_${i + 1}_${abs(name.hashCode()) % 1000}",
                    name = name,
                    position = pos,
                    overallRating = ovr,
                    age = age,
                    archetype = arch,
                    experienceYears = experience,
                    askingSalaryMillions = baseAsking,
                    preferredYears = prefYears,
                    formerFranchiseName = former,
                    bioTag = bio
                )
            )
        }
        return pool.sortedByDescending { it.overallRating }
    }

    fun replenishFreeAgentPool(
        currentPool: List<FreeAgentPlayer>,
        franchises: List<Franchise>,
        sportType: SportType,
        seasonYear: Int,
        minPoolSize: Int = 18,
        seedOffset: Int = 1
    ): List<FreeAgentPlayer> {
        if (currentPool.size >= minPoolSize) return currentPool.sortedByDescending { it.overallRating }
        val needed = (minPoolSize + 4) - currentPool.size
        val existingNames = (currentPool.map { it.name } + franchises.flatMap { f -> f.roster.map { it.name } }).toSet()
        val additions = generateInitialFreeAgentPool(
            sportType = sportType,
            seasonYear = seasonYear,
            count = needed.coerceAtLeast(4),
            excludeNames = existingNames,
            seedOffset = seedOffset + currentPool.size + seasonYear
        )
        return (currentPool + additions).sortedByDescending { it.overallRating }
    }

    fun computeBaseMarketSalary(overallRating: Int, age: Int): Long {
        val base = when {
            overallRating >= 87 -> 26L + ((overallRating - 87) * 3L)
            overallRating >= 84 -> 18L + ((overallRating - 84) * 2L)
            overallRating >= 80 -> 11L + ((overallRating - 80) * 1L)
            overallRating >= 76 -> 6L + ((overallRating - 76) / 2L)
            overallRating >= 72 -> 3L + ((overallRating - 72) / 2L)
            else -> 2L
        }
        val ageDiscount = if (age >= 32) (age - 31).toLong() else 0L
        return (base - ageDiscount).coerceIn(2L, 38L)
    }

    fun computePreferredContractYears(overallRating: Int, age: Int): Int = when {
        age >= 33 -> 1
        age >= 30 -> if (overallRating >= 82) 2 else 1
        overallRating >= 83 -> 3 + if (age <= 27) 1 else 0
        overallRating >= 77 -> 2
        else -> 1
    }.coerceIn(1, 4)

    fun calculateRealisticContract(
        freeAgent: FreeAgentPlayer,
        team: Franchise,
        ruleBook: LeagueRuleBook,
        unionAgreement: LaborUnionAgreement,
        isUrgentDepthNeed: Boolean = false
    ): ContractOfferTerms {
        val salaryCap = unionAgreement.salaryCapMillions.coerceAtLeast(80L)
        val capRoom = salaryCap - team.annualPayrollMillions
        val strictness = ruleBook.salaryCapStrictness
        val oddsPreview = TeamReputationEngine.evaluateFreeAgentSigningOdds(
            freeAgent = freeAgent,
            franchise = team,
            salaryCapMillions = salaryCap,
            offerSalaryMillions = freeAgent.askingSalaryMillions,
            isUrgentDepthNeed = isUrgentDepthNeed
        )
        val marketSalary = (freeAgent.askingSalaryMillions + oddsPreview.demandedSalaryPremiumMillions)
            .coerceAtMost(42L)

        var finalSalary = marketSalary
        var years = freeAgent.preferredYears
        var contractLabel: String
        var isCapCompliant = true
        var complianceNote = if (oddsPreview.demandedSalaryPremiumMillions > 0L) {
            "Cap Compliant (+\$${oddsPreview.demandedSalaryPremiumMillions}M Rep Tax)"
        } else {
            "Cap Compliant"
        }

        when (strictness) {
            SalaryCapStrictness.STRICT_HARD_CAP -> {
                if (capRoom <= 1L) {
                    isCapCompliant = false
                    complianceNote = "Blocked: Hard Cap ceiling reached (\$$capRoom M room)"
                } else if (marketSalary > capRoom) {
                    val affordable = capRoom.coerceAtLeast(2L)
                    if (affordable >= (marketSalary * 0.55).roundToInt().toLong() || freeAgent.overallRating <= 78) {
                        finalSalary = affordable
                        years = 1
                    } else {
                        isCapCompliant = false
                        complianceNote = "Insufficient hard cap space (\$$capRoom M left vs \$$marketSalary M ask)"
                    }
                }
            }
            SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX, SalaryCapStrictness.MODERATE_SOFT_CAP -> {
                if (capRoom < marketSalary) {
                    if (freeAgent.overallRating >= 79 && marketSalary <= 14L && capRoom >= -18L) {
                        finalSalary = minOf(marketSalary, 11L)
                        years = minOf(years, 2)
                        complianceNote = "Signed via Mid-Level Exception"
                    } else {
                        finalSalary = minOf(marketSalary, if (freeAgent.age >= 30) 3L else 4L).coerceAtLeast(2L)
                        years = 1
                        complianceNote = "Signed via Veteran / Depth Minimum Exception"
                    }
                }
            }
            SalaryCapStrictness.UNRESTRICTED_MARKET -> {
                val bigMarketBump = if (team.fanBaseMillions >= 3.2 && freeAgent.overallRating >= 83) 2L else 0L
                finalSalary = marketSalary + bigMarketBump
                complianceNote = "Unrestricted Free Market Deal"
            }
        }

        if (isUrgentDepthNeed && years > 2 && finalSalary <= 7L) {
            years = 1
        }

        contractLabel = when {
            finalSalary >= 22L -> "Max Marquee Deal"
            finalSalary >= 14L && years >= 2 -> "Multi-Year Starter Pact"
            complianceNote.contains("Mid-Level") || (finalSalary in 8L..13L && years >= 2) -> "Mid-Level Exception"
            isUrgentDepthNeed && finalSalary <= 5L -> "Injury Replacement Pact"
            finalSalary <= 3L && freeAgent.age >= 29 -> "Veteran Minimum"
            finalSalary <= 4L -> "Two-Way Depth Deal"
            else -> "Standard Free Agent Contract"
        }

        return ContractOfferTerms(
            salaryMillions = finalSalary.coerceAtLeast(2L),
            contractYears = years.coerceIn(1, 4),
            contractTypeLabel = contractLabel,
            totalContractValueMillions = finalSalary.coerceAtLeast(2L) * years.coerceIn(1, 4),
            isCapCompliant = isCapCompliant,
            complianceNote = complianceNote
        )
    }

    fun evaluateDepthChartNeeds(
        franchise: Franchise,
        sportType: SportType,
        ruleBook: LeagueRuleBook
    ): DepthChartNeedSummary {
        val positions = positionsForSport(sportType)
        val maxSlots = (ruleBook.rosterSizeRule.activeSlots + ruleBook.rosterSizeRule.reserveSlots).coerceAtLeast(10)
        // Standard balanced depth chart aims for 10 healthy players (or up to 12 under expanded rules)
        val targetHealthyDepth = (ruleBook.rosterSizeRule.activeSlots - 3).coerceIn(9, 12)
        val healthyPlayers = franchise.roster.filter { !it.isMedicallyOut }
        val healthyCount = healthyPlayers.size
        val injuredOutCount = franchise.roster.count { it.isMedicallyOut }
        val urgentHoles = franchise.unresolvedStarterHolesCount
        val openRosterSlots = (maxSlots - franchise.roster.size).coerceAtLeast(0)

        // Identify positions with zero healthy starters/backups or fewer than 2 healthy players
        val posCounts = positions.associateWith { pos ->
            healthyPlayers.count { it.position.equals(pos, ignoreCase = true) }
        }
        val missingPos = positions.filter { (posCounts[it] ?: 0) == 0 }
        val thinPos = positions.filter { (posCounts[it] ?: 0) == 1 }
        val neededPositions = (missingPos + thinPos).distinct().ifEmpty {
            listOf(positions.minByOrNull { posCounts[it] ?: 0 } ?: positions.first())
        }

        val depthDeficit = (targetHealthyDepth - healthyCount).coerceAtLeast(0)
        val priority = when {
            openRosterSlots <= 0 -> 0
            healthyCount < 7 -> 100 + (7 - healthyCount) * 20
            urgentHoles > 0 -> 80 + urgentHoles * 15
            missingPos.isNotEmpty() -> 65 + missingPos.size * 10
            depthDeficit > 0 -> 30 + depthDeficit * 8
            else -> 0
        }

        val reason = when {
            healthyCount < 7 -> "Emergency active roster depth shortage ($healthyCount healthy)"
            urgentHoles > 0 -> "Replace injured starter void (${missingPos.firstOrNull() ?: neededPositions.first()})"
            missingPos.isNotEmpty() -> "Fill missing ${missingPos.first()} depth chart slot"
            depthDeficit > 0 -> "Expand ${neededPositions.first()} bench rotation depth ($healthyCount/$targetHealthyDepth)"
            else -> "Roster depth chart full"
        }

        return DepthChartNeedSummary(
            franchiseId = franchise.id,
            healthyCount = healthyCount,
            injuredOutCount = injuredOutCount,
            maxRosterSlots = maxSlots,
            targetDepthCount = targetHealthyDepth,
            openRosterSlots = openRosterSlots,
            urgentHolesCount = urgentHoles,
            neededPositions = neededPositions,
            primaryReason = reason,
            priorityScore = priority
        )
    }

    fun signFreeAgentToFranchise(
        franchise: Franchise,
        freeAgent: FreeAgentPlayer,
        freeAgentPool: List<FreeAgentPlayer>,
        ruleBook: LeagueRuleBook,
        unionAgreement: LaborUnionAgreement,
        week: Int,
        seasonYear: Int,
        sportType: SportType = SportType.BASKETBALL,
        customYears: Int? = null,
        reasonOverride: String? = null,
        isAutoSigned: Boolean = false,
        enforceReputationCheck: Boolean = true,
        signingRollOverride: Int? = null
    ): SingleFreeAgencySigningResult {
        val needSummary = evaluateDepthChartNeeds(franchise, sportType, ruleBook)
        val maxRosterSlots = (ruleBook.rosterSizeRule.activeSlots + ruleBook.rosterSizeRule.reserveSlots).coerceAtLeast(10)
        if (franchise.roster.size >= maxRosterSlots) {
            return SingleFreeAgencySigningResult(
                success = false,
                message = "${franchise.teamFullName} roster is at capacity (${franchise.roster.size}/$maxRosterSlots). Release a player first."
            )
        }
        if (franchise.roster.any { it.id == freeAgent.id }) {
            return SingleFreeAgencySigningResult(
                success = false,
                message = "${freeAgent.name} is already on ${franchise.teamFullName}'s roster."
            )
        }

        val isUrgent = needSummary.urgentHolesCount > 0 || needSummary.healthyCount < 8
        val offer = calculateRealisticContract(
            freeAgent = freeAgent,
            team = franchise,
            ruleBook = ruleBook,
            unionAgreement = unionAgreement,
            isUrgentDepthNeed = isUrgent
        )
        if (!offer.isCapCompliant) {
            return SingleFreeAgencySigningResult(
                success = false,
                message = offer.complianceNote
            )
        }

        val signingOdds = TeamReputationEngine.evaluateFreeAgentSigningOdds(
            freeAgent = freeAgent,
            franchise = franchise,
            salaryCapMillions = unionAgreement.salaryCapMillions,
            offerSalaryMillions = offer.salaryMillions,
            isUrgentDepthNeed = isUrgent
        )
        if (enforceReputationCheck && !TeamReputationEngine.shouldAcceptFreeAgentOffer(
                odds = signingOdds,
                freeAgent = freeAgent,
                franchise = franchise,
                week = week,
                isUrgentDepthNeed = isUrgent,
                signingRollOverride = signingRollOverride
            )
        ) {
            val declineMsg = signingOdds.refusalReason
                ?: "${freeAgent.name} declined ${franchise.teamFullName}'s offer (${signingOdds.likelihoodPct}% signing odds)."
            return SingleFreeAgencySigningResult(
                success = false,
                message = declineMsg,
                newsHeadline = "FREE AGENT SNUB: $declineMsg"
            )
        }

        val finalYears = (customYears ?: offer.contractYears).coerceIn(1, 4)
        val newPlayer = RosterPlayer(
            id = freeAgent.id,
            name = freeAgent.name,
            franchiseId = franchise.id,
            position = freeAgent.position,
            overallRating = freeAgent.overallRating,
            age = freeAgent.age,
            salaryMillions = offer.salaryMillions,
            contractYearsRemaining = finalYears,
            archetype = freeAgent.archetype,
            depthOrder = franchise.roster.size,
            isStarter = false,
            activeInjury = null
        )

        val appendedRoster = franchise.roster + newPlayer
        val preUpdatedTeam = TeamReputationEngine.applyReputationToFranchise(
            franchise.copy(
                roster = appendedRoster,
                annualPayrollMillions = franchise.annualPayrollMillions + offer.salaryMillions
            ),
            unionAgreement.salaryCapMillions
        )

        // If an active starter is OUT/IR, fewer than 5 starters are active, or the new signing upgrades over the weakest active starter,
        // auto-slot the best 5-man starting unit cleanly
        val activeStarters = InjuryEngine.getActiveStarters(preUpdatedTeam)
        val worstActiveStarterOvr = activeStarters.minOfOrNull { it.effectiveOverallRating } ?: 0
        val slottedTeam = if (
            preUpdatedTeam.unresolvedStarterHolesCount > 0 ||
            activeStarters.size < 5 ||
            newPlayer.overallRating > worstActiveStarterOvr
        ) {
            InjuryEngine.autoOptimizeDepthChart(preUpdatedTeam)
        } else {
            // Preserve existing starter order and assign clean sequential bench depthOrder
            val starters = appendedRoster.filter { it.isStarter }.sortedBy { it.depthOrder }.take(5)
            val starterIds = starters.map { it.id }.toSet()
            val bench = appendedRoster.filter { it.id !in starterIds }
                .sortedWith(compareBy<RosterPlayer> { it.id == newPlayer.id }.thenBy { it.depthOrder }.thenByDescending { it.overallRating })
            val ordered = (starters.mapIndexed { idx, p -> p.copy(isStarter = true, depthOrder = idx) } +
                bench.mapIndexed { idx, p -> p.copy(isStarter = false, depthOrder = starters.size + idx) })
            preUpdatedTeam.copy(roster = ordered)
        }

        // Recalculate team OVR softly if the signing elevates top-8 rotation talent
        val top8Avg = slottedTeam.roster.sortedByDescending { it.overallRating }.take(8).map { it.overallRating }.average()
        val boostedRating = if (top8Avg > slottedTeam.rosterRating) {
            ((slottedTeam.rosterRating + top8Avg) / 2.0).roundToInt().coerceIn(slottedTeam.rosterRating, 96)
        } else {
            slottedTeam.rosterRating
        }
        val finalTeam = slottedTeam.copy(rosterRating = boostedRating)

        val signingReason = reasonOverride ?: needSummary.primaryReason
        val tx = FreeAgentSigningTransaction(
            id = "fa_tx_${seasonYear}_w${week}_${franchise.id}_${freeAgent.id}",
            playerId = freeAgent.id,
            playerName = freeAgent.name,
            position = freeAgent.position,
            overallRating = freeAgent.overallRating,
            age = freeAgent.age,
            archetype = freeAgent.archetype,
            franchiseId = franchise.id,
            franchiseName = franchise.teamFullName,
            salaryMillions = offer.salaryMillions,
            contractYears = finalYears,
            contractTypeLabel = offer.contractTypeLabel,
            reason = signingReason,
            week = week,
            seasonYear = seasonYear,
            isAutoSigned = isAutoSigned
        )

        val yearUnit = if (finalYears == 1) "1-yr" else "${finalYears}-yr"
        val headline = "FREE AGENCY SIGNING: ${franchise.teamFullName} sign ${freeAgent.name} (${freeAgent.position}, ${freeAgent.overallRating} OVR) to a $yearUnit, \$${offer.salaryMillions}M ${offer.contractTypeLabel} to ${signingReason.replaceFirstChar { it.lowercase() }}!"

        val updatedPool = freeAgentPool.filterNot { it.id == freeAgent.id }
        return SingleFreeAgencySigningResult(
            success = true,
            message = "Signed ${freeAgent.name} to ${franchise.teamFullName} ($yearUnit, \$${offer.salaryMillions}M ${offer.contractTypeLabel}).",
            updatedFranchise = finalTeam,
            updatedPool = updatedPool,
            transaction = tx,
            newsHeadline = headline
        )
    }

    fun runAutomaticFreeAgencySweep(
        franchises: List<Franchise>,
        freeAgentPool: List<FreeAgentPlayer>,
        sportType: SportType,
        ruleBook: LeagueRuleBook,
        unionAgreement: LaborUnionAgreement,
        week: Int,
        seasonYear: Int,
        maxSigningsPerSweep: Int = 3,
        urgentOnly: Boolean = false
    ): FreeAgencySweepResult {
        if (franchises.isEmpty()) {
            return FreeAgencySweepResult(franchises, freeAgentPool, emptyList(), emptyList())
        }

        var workingPool = replenishFreeAgentPool(
            currentPool = freeAgentPool,
            franchises = franchises,
            sportType = sportType,
            seasonYear = seasonYear,
            minPoolSize = 18,
            seedOffset = week + seasonYear
        )
        val workingFranchises = franchises.toMutableList()
        val signings = mutableListOf<FreeAgentSigningTransaction>()
        val headlines = mutableListOf<String>()

        // Evaluate needs & team reputation across all franchises
        val prioritizedTeamIds = workingFranchises.map { team ->
            val need = evaluateDepthChartNeeds(team, sportType, ruleBook)
            val rep = TeamReputationEngine.evaluateReputation(team, unionAgreement.salaryCapMillions)
            need to rep.score
        }.filter { (summary, _) ->
            summary.openRosterSlots > 0 &&
                if (urgentOnly) (summary.urgentHolesCount > 0 || summary.healthyCount < 8) else summary.priorityScore > 0
        }.sortedByDescending { (summary, repScore) ->
            summary.priorityScore + ((repScore - 65) / 2)
        }.map { it.first }

        for (need in prioritizedTeamIds) {
            if (signings.size >= maxSigningsPerSweep) break
            val tIdx = workingFranchises.indexOfFirst { it.id == need.franchiseId }
            if (tIdx == -1) continue
            val team = workingFranchises[tIdx]
            val isUrgent = need.urgentHolesCount > 0 || need.healthyCount < 8

            // Pick best affordable free agent matching needed position and team reputation willingness
            val candidate = workingPool
                .mapNotNull { fa ->
                    val offer = calculateRealisticContract(
                        freeAgent = fa,
                        team = team,
                        ruleBook = ruleBook,
                        unionAgreement = unionAgreement,
                        isUrgentDepthNeed = isUrgent
                    )
                    if (!offer.isCapCompliant) {
                        null
                    } else {
                        val odds = TeamReputationEngine.evaluateFreeAgentSigningOdds(
                            freeAgent = fa,
                            franchise = team,
                            salaryCapMillions = unionAgreement.salaryCapMillions,
                            offerSalaryMillions = offer.salaryMillions,
                            isUrgentDepthNeed = isUrgent
                        )
                        if (!TeamReputationEngine.shouldAcceptFreeAgentOffer(odds, fa, team, week, isUrgent)) {
                            null
                        } else {
                            val posBonus = if (fa.position in need.neededPositions) 12 else 0
                            val contenderBonus = if (team.rosterRating >= 84 && fa.overallRating >= 80) 4 else 0
                            val repBonus = ((odds.likelihoodPct - 60) / 5)
                            val capSensitivity = if (offer.salaryMillions > 14L && need.urgentHolesCount == 0 && need.healthyCount >= 8) -6 else 0
                            val score = fa.overallRating + posBonus + contenderBonus + repBonus + capSensitivity
                            fa to score
                        }
                    }
                }
                .maxByOrNull { it.second }
                ?.first ?: continue

            val signRes = signFreeAgentToFranchise(
                franchise = team,
                freeAgent = candidate,
                freeAgentPool = workingPool,
                ruleBook = ruleBook,
                unionAgreement = unionAgreement,
                week = week,
                seasonYear = seasonYear,
                sportType = sportType,
                reasonOverride = need.primaryReason,
                isAutoSigned = true
            )

            if (signRes.success && signRes.updatedFranchise != null && signRes.transaction != null) {
                workingFranchises[tIdx] = signRes.updatedFranchise
                workingPool = signRes.updatedPool
                signings.add(signRes.transaction)
                signRes.newsHeadline?.let { headlines.add(it) }
            }
        }

        // Keep pool topped off after signings
        workingPool = replenishFreeAgentPool(
            currentPool = workingPool,
            franchises = workingFranchises,
            sportType = sportType,
            seasonYear = seasonYear,
            minPoolSize = 18,
            seedOffset = week + seasonYear + 7
        )

        return FreeAgencySweepResult(
            updatedFranchises = workingFranchises,
            updatedPool = workingPool,
            newSignings = signings,
            headlines = headlines
        )
    }

    fun releasePlayerToFreeAgency(
        franchise: Franchise,
        playerId: String,
        freeAgentPool: List<FreeAgentPlayer>,
        week: Int,
        seasonYear: Int
    ): Pair<Pair<Franchise, List<FreeAgentPlayer>>?, String> {
        if (franchise.roster.size <= 6) {
            return null to "${franchise.teamFullName} must carry at least 6 players on the active roster."
        }
        val player = franchise.roster.find { it.id == playerId }
            ?: return null to "Player not found on ${franchise.teamFullName} roster."

        val remainingRoster = franchise.roster.filterNot { it.id == playerId }
        val preTeam = franchise.copy(
            roster = remainingRoster,
            annualPayrollMillions = (franchise.annualPayrollMillions - player.salaryMillions).coerceAtLeast(15L)
        )
        val optimizedTeam = InjuryEngine.autoOptimizeDepthChart(preTeam)

        val freedPlayer = FreeAgentPlayer(
            id = if (player.id.startsWith("fa_")) player.id else "fa_waived_${seasonYear}_${player.id}",
            name = player.name,
            position = player.position,
            overallRating = player.overallRating,
            age = player.age,
            archetype = player.archetype,
            experienceYears = (player.age - 20).coerceIn(1, 15),
            askingSalaryMillions = computeBaseMarketSalary(player.overallRating, player.age),
            preferredYears = computePreferredContractYears(player.overallRating, player.age),
            formerFranchiseName = franchise.teamFullName,
            bioTag = "Waiver Wire Veteran Release"
        )
        val updatedPool = (listOf(freedPlayer) + freeAgentPool.filterNot { it.id == freedPlayer.id })
            .sortedByDescending { it.overallRating }

        val headline = "FREE AGENCY WAIVER WIRE: ${franchise.teamFullName} release ${player.name} (${player.position}, ${player.overallRating} OVR) into the league free agent pool, clearing \$${player.salaryMillions}M."
        return (optimizedTeam to updatedPool) to headline
    }
}
