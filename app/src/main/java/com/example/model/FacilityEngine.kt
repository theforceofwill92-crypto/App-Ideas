package com.example.model

import kotlin.math.roundToInt

enum class FacilityType(
    val title: String,
    val shortName: String,
    val description: String,
    val maxTier: Int = 5,
    val badgeColorHex: Long
) {
    STADIUM_SEATING(
        title = "Stadium & Bowl Expansion",
        shortName = "Stadium",
        description = "Expands upper-deck seating bowls, concourses, and matchday arena capacity.",
        maxTier = 5,
        badgeColorHex = 0xFF3B82F6
    ),
    TRAINING_CENTER(
        title = "Sports Science & Training Center",
        shortName = "Training",
        description = "Boosts player conditioning OVR, lowers injury risk, and speeds medical recovery.",
        maxTier = 5,
        badgeColorHex = 0xFF10B981
    ),
    LUXURY_SUITES(
        title = "VIP Luxury Suites & Club Boxes",
        shortName = "Suites",
        description = "Installs corporate skyboxes and hospitality lounges to drive recurring sponsorship & value.",
        maxTier = 5,
        badgeColorHex = 0xFFFFB800
    )
}

data class FacilityUpgradeSpec(
    val type: FacilityType,
    val targetTier: Int,
    val projectName: String,
    val costMillions: Long,
    val durationWeeks: Int,
    val seatingCapacityGain: Int,
    val annualSponsorGainMillions: Long,
    val rosterOvrGain: Int,
    val valuationBoostMillions: Long,
    val loyaltyGain: Int
)

data class FacilityProject(
    val id: String,
    val franchiseId: String,
    val type: FacilityType,
    val targetTier: Int,
    val projectName: String,
    val costMillions: Long,
    val totalWeeks: Int,
    val remainingWeeks: Int,
    val seatingCapacityGain: Int,
    val annualSponsorGainMillions: Long,
    val rosterOvrGain: Int,
    val valuationBoostMillions: Long,
    val loyaltyGain: Int
)

data class FranchiseFacilities(
    val stadiumTier: Int = 1,
    val trainingTier: Int = 1,
    val luxurySuitesTier: Int = 1,
    val cumulativeInvestedMillions: Long = 0L,
    val accumulatedFacilityValueMillions: Long = 0L,
    val ongoingProjects: List<FacilityProject> = emptyList(),
    val completedUpgradesCount: Int = 0
) {
    fun tierOf(type: FacilityType): Int = when (type) {
        FacilityType.STADIUM_SEATING -> stadiumTier
        FacilityType.TRAINING_CENTER -> trainingTier
        FacilityType.LUXURY_SUITES -> luxurySuitesTier
    }

    val averageTier: Double
        get() = Math.round(((stadiumTier + trainingTier + luxurySuitesTier) / 3.0) * 10.0) / 10.0

    val injuryRiskReductionPct: Int
        get() = ((trainingTier - 1) * 10).coerceIn(0, 40)

    val conditioningOvrBonus: Int
        get() = (trainingTier - 1).coerceIn(0, 4)

    val annualSuitePremiumMillions: Double
        get() = ((luxurySuitesTier - 1) * 3.5)
}

data class FranchiseValuationBreakdown(
    val totalValuationMillions: Long,
    val marketAndBrandMillions: Long,
    val commercialMultipleMillions: Long,
    val facilityAssetMillions: Long,
    val loyaltyGoodwillMillions: Long
)

data class FacilityConstructionTickResult(
    val updatedFranchises: List<Franchise>,
    val headlines: List<String>,
    val totalLeagueValuationGainMillions: Long
)

object FacilityEngine {

    fun getNextUpgradeSpec(facilities: FranchiseFacilities, type: FacilityType): FacilityUpgradeSpec? {
        val current = facilities.tierOf(type)
        if (current >= type.maxTier) return null
        val target = current + 1
        return when (type) {
            FacilityType.STADIUM_SEATING -> when (target) {
                2 -> FacilityUpgradeSpec(type, 2, "Upper Concourse & Mezzanine Bowl", 18L, 2, 3000, 2L, 0, 28L, 2)
                3 -> FacilityUpgradeSpec(type, 3, "360° Ribbon Bowl Expansion", 28L, 3, 4000, 3L, 0, 42L, 2)
                4 -> FacilityUpgradeSpec(type, 4, "Retractable Canopy & End-Zone Tiers", 40L, 3, 5000, 4L, 0, 58L, 3)
                else -> FacilityUpgradeSpec(type, 5, "Colosseum 40K Mega-Bowl", 55L, 4, 6500, 6L, 0, 82L, 4)
            }
            FacilityType.TRAINING_CENTER -> when (target) {
                2 -> FacilityUpgradeSpec(type, 2, "Cryo & Biomechanics Recovery Lab", 12L, 2, 0, 0L, 1, 18L, 1)
                3 -> FacilityUpgradeSpec(type, 3, "AI Motion & Analytics Performance Campus", 20L, 2, 0, 1L, 1, 30L, 2)
                4 -> FacilityUpgradeSpec(type, 4, "Olympic Sports Medicine & Conditioning Hub", 30L, 3, 0, 1L, 2, 46L, 2)
                else -> FacilityUpgradeSpec(type, 5, "Apex Vanguard Human Performance Institute", 42L, 3, 0, 2L, 2, 65L, 3)
            }
            FacilityType.LUXURY_SUITES -> when (target) {
                2 -> FacilityUpgradeSpec(type, 2, "Founders Club Mezzanine Suites", 16L, 2, 500, 6L, 0, 26L, 1)
                3 -> FacilityUpgradeSpec(type, 3, "Courtside Loge & Corporate Skyboxes", 25L, 3, 750, 9L, 0, 40L, 2)
                4 -> FacilityUpgradeSpec(type, 4, "Platinum Penthouse & Tunnel Club", 36L, 3, 1000, 13L, 0, 58L, 2)
                else -> FacilityUpgradeSpec(type, 5, "Sovereign Sky-Deck & Hospitality Pavilion", 48L, 4, 1250, 18L, 0, 80L, 3)
            }
        }
    }

    fun calculateValuationBreakdown(
        franchise: Franchise,
        activeBroadcast: BroadcastContract,
        totalTeams: Int
    ): FranchiseValuationBreakdown {
        val stmt = franchise.calculateFinancialStatement(activeBroadcast, totalTeams)
        return calculateValuationFromRevenue(franchise, stmt.totalRevenueMillions)
    }

    fun calculateValuationFromRevenue(
        franchise: Franchise,
        totalRevenueMillions: Double
    ): FranchiseValuationBreakdown {
        val marketValue = (franchise.fanBaseMillions * 48.0 +
            (franchise.rosterRating - 65).coerceAtLeast(0) * 3.5 +
            franchise.championships * 32.0).roundToInt().toLong().coerceAtLeast(60L)
        val commercialValue = (totalRevenueMillions * 2.25).roundToInt().toLong().coerceAtLeast(45L)
        val facilityAsset = ((franchise.stadiumCapacity * 0.0042) +
            (franchise.facilities.stadiumTier - 1) * 14.0 +
            (franchise.facilities.trainingTier - 1) * 12.0 +
            (franchise.facilities.luxurySuitesTier - 1) * 20.0 +
            franchise.facilities.accumulatedFacilityValueMillions +
            franchise.facilities.ongoingProjects.sumOf { it.costMillions } * 0.45).roundToInt().toLong()
        val loyaltyGoodwill = ((franchise.fanLoyalty - 65) * 1.4).roundToInt().toLong()

        val total = (marketValue + commercialValue + facilityAsset + loyaltyGoodwill).coerceAtLeast(120L)
        return FranchiseValuationBreakdown(
            totalValuationMillions = total,
            marketAndBrandMillions = marketValue,
            commercialMultipleMillions = commercialValue,
            facilityAssetMillions = facilityAsset,
            loyaltyGoodwillMillions = loyaltyGoodwill
        )
    }

    fun startUpgradeProject(
        franchise: Franchise,
        type: FacilityType,
        currentYear: Int,
        currentWeek: Int
    ): Pair<Franchise, FacilityUpgradeSpec>? {
        val spec = getNextUpgradeSpec(franchise.facilities, type) ?: return null
        if (franchise.facilities.ongoingProjects.any { it.type == type }) return null

        val project = FacilityProject(
            id = "fac_${franchise.id}_${type.name.lowercase()}_t${spec.targetTier}_${currentYear}_w$currentWeek",
            franchiseId = franchise.id,
            type = type,
            targetTier = spec.targetTier,
            projectName = spec.projectName,
            costMillions = spec.costMillions,
            totalWeeks = spec.durationWeeks,
            remainingWeeks = spec.durationWeeks,
            seatingCapacityGain = spec.seatingCapacityGain,
            annualSponsorGainMillions = spec.annualSponsorGainMillions,
            rosterOvrGain = spec.rosterOvrGain,
            valuationBoostMillions = spec.valuationBoostMillions,
            loyaltyGain = spec.loyaltyGain
        )

        val updatedFacilities = franchise.facilities.copy(
            cumulativeInvestedMillions = franchise.facilities.cumulativeInvestedMillions + spec.costMillions,
            ongoingProjects = franchise.facilities.ongoingProjects + project
        )
        return franchise.copy(facilities = updatedFacilities) to spec
    }

    fun completeProjectImmediately(
        franchise: Franchise,
        project: FacilityProject
    ): Franchise {
        val currentFac = franchise.facilities
        val nextStadiumTier = if (project.type == FacilityType.STADIUM_SEATING) {
            maxOf(currentFac.stadiumTier, project.targetTier)
        } else currentFac.stadiumTier
        val nextTrainingTier = if (project.type == FacilityType.TRAINING_CENTER) {
            maxOf(currentFac.trainingTier, project.targetTier)
        } else currentFac.trainingTier
        val nextSuitesTier = if (project.type == FacilityType.LUXURY_SUITES) {
            maxOf(currentFac.luxurySuitesTier, project.targetTier)
        } else currentFac.luxurySuitesTier

        val updatedFacilities = currentFac.copy(
            stadiumTier = nextStadiumTier,
            trainingTier = nextTrainingTier,
            luxurySuitesTier = nextSuitesTier,
            accumulatedFacilityValueMillions = currentFac.accumulatedFacilityValueMillions + project.valuationBoostMillions,
            ongoingProjects = currentFac.ongoingProjects.filterNot { it.id == project.id },
            completedUpgradesCount = currentFac.completedUpgradesCount + 1
        )

        val newRosterRating = (franchise.rosterRating + project.rosterOvrGain).coerceIn(50, 99)
        val newLoyalty = (franchise.fanLoyalty + project.loyaltyGain).coerceIn(10, 100)

        return franchise.copy(
            stadiumCapacity = franchise.stadiumCapacity + project.seatingCapacityGain,
            localSponsorRevenueMillions = franchise.localSponsorRevenueMillions + project.annualSponsorGainMillions,
            rosterRating = newRosterRating,
            fanLoyalty = newLoyalty,
            facilities = updatedFacilities
        )
    }

    fun advanceWeeklyConstruction(
        franchises: List<Franchise>,
        weeksElapsed: Int = 1,
        completeAllRemaining: Boolean = false
    ): FacilityConstructionTickResult {
        val headlines = mutableListOf<String>()
        var totalValGain = 0L

        val updated = franchises.map { team ->
            if (team.facilities.ongoingProjects.isEmpty()) return@map team

            var workingTeam = team
            val stillOngoing = mutableListOf<FacilityProject>()
            team.facilities.ongoingProjects.forEach { proj ->
                val newRemaining = if (completeAllRemaining) 0 else (proj.remainingWeeks - weeksElapsed).coerceAtLeast(0)
                if (newRemaining == 0) {
                    workingTeam = completeProjectImmediately(workingTeam, proj)
                    totalValGain += proj.valuationBoostMillions
                    val seatNote = if (proj.seatingCapacityGain > 0) " (+${proj.seatingCapacityGain} seats -> ${workingTeam.stadiumCapacity})" else ""
                    val sponsorNote = if (proj.annualSponsorGainMillions > 0) " (+\$${proj.annualSponsorGainMillions}M/yr suite revenue)" else ""
                    val trainingNote = if (proj.rosterOvrGain > 0) " (+${proj.rosterOvrGain} OVR conditioning)" else ""
                    headlines.add(
                        "GRAND OPENING: ${team.teamFullName} complete Tier ${proj.targetTier} ${proj.projectName}$seatNote$sponsorNote$trainingNote — Franchise value +\$${proj.valuationBoostMillions}M!"
                    )
                } else {
                    stillOngoing.add(proj.copy(remainingWeeks = newRemaining))
                }
            }

            if (stillOngoing.size == workingTeam.facilities.ongoingProjects.size) {
                workingTeam.copy(facilities = workingTeam.facilities.copy(ongoingProjects = stillOngoing))
            } else {
                // Ensure completed items removed & remaining decremented cleanly
                workingTeam.copy(facilities = workingTeam.facilities.copy(ongoingProjects = stillOngoing))
            }
        }

        return FacilityConstructionTickResult(
            updatedFranchises = updated,
            headlines = headlines,
            totalLeagueValuationGainMillions = totalValGain
        )
    }

    /**
     * Annual facility compound appreciation for clubs investing in elite infrastructure over seasons.
     */
    fun applyOffSeasonFacilityAppreciation(franchises: List<Franchise>): List<Franchise> {
        return franchises.map { team ->
            val tierSum = team.facilities.stadiumTier + team.facilities.trainingTier + team.facilities.luxurySuitesTier
            val annualAppreciation = ((tierSum - 3) * 3L).coerceAtLeast(0L)
            if (annualAppreciation > 0L) {
                team.copy(
                    facilities = team.facilities.copy(
                        accumulatedFacilityValueMillions = team.facilities.accumulatedFacilityValueMillions + annualAppreciation
                    )
                )
            } else team
        }
    }
}
