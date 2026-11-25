package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

enum class TeamReputationTier(
    val label: String,
    val badgeText: String,
    val minScore: Int,
    val colorHex: Long,
    val baseLikelihoodDelta: Int,
    val summary: String
) {
    PREMIER_DESTINATION(
        label = "Premier Destination",
        badgeText = "ELITE",
        minScore = 82,
        colorHex = 0xFF10B981,
        baseLikelihoodDelta = +16,
        summary = "Top free agents prioritize this club for generous payroll investment and city stability."
    ),
    RESPECTED_CLUB(
        label = "Respected Club",
        badgeText = "SOLID",
        minScore = 64,
        colorHex = 0xFF06B6D4,
        baseLikelihoodDelta = +4,
        summary = "Dependable ownership behavior sustains healthy free-agent interest."
    ),
    SKEPTICAL_MARKET(
        label = "Skeptical Market",
        badgeText = "CAUTIOUS",
        minScore = 44,
        colorHex = 0xFFF59E0B,
        baseLikelihoodDelta = -18,
        summary = "Relocation history or frugal spending lowers agent interest and forces contract premiums."
    ),
    AVOIDED_FRANCHISE(
        label = "Avoided Franchise",
        badgeText = "AVOIDED",
        minScore = 0,
        colorHex = 0xFFEF4444,
        baseLikelihoodDelta = -38,
        summary = "Frequent relocations or chronic under-spending actively deter free agents from signing."
    );

    companion object {
        fun fromScore(score: Int): TeamReputationTier = when {
            score >= PREMIER_DESTINATION.minScore -> PREMIER_DESTINATION
            score >= RESPECTED_CLUB.minScore -> RESPECTED_CLUB
            score >= SKEPTICAL_MARKET.minScore -> SKEPTICAL_MARKET
            else -> AVOIDED_FRANCHISE
        }
    }
}

data class TeamReputationBreakdown(
    val score: Int,
    val tier: TeamReputationTier,
    val relocationCount: Int,
    val relocationPenalty: Int,
    val payrollPctOfCap: Int,
    val lowSpendingPenalty: Int,
    val lowSpendingSeasons: Int,
    val stabilityBonus: Int,
    val reputationSalaryPremiumMillions: Long,
    val baseFreeAgentLikelihoodPct: Int,
    val summaryNote: String,
    val drivers: List<String>
)

data class FreeAgentSigningOdds(
    val likelihoodPct: Int,
    val reputationScore: Int,
    val reputationTier: TeamReputationTier,
    val demandedSalaryPremiumMillions: Long,
    val isContestedOrRejected: Boolean,
    val driverSummary: String,
    val refusalReason: String?
)

object TeamReputationEngine {

    const val LOW_SPENDING_CAP_THRESHOLD_PCT = 72
    const val GENEROUS_SPENDING_CAP_THRESHOLD_PCT = 90

    fun evaluateReputation(
        franchise: Franchise,
        salaryCapMillions: Long
    ): TeamReputationBreakdown {
        val cap = salaryCapMillions.coerceAtLeast(80L)
        val payrollPct = ((franchise.annualPayrollMillions.toDouble() / cap.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 220)

        val relocCount = franchise.relocationCount.coerceAtLeast(0)
        val relocationPenalty = when {
            relocCount == 0 -> 0
            relocCount == 1 -> 16
            else -> 16 + ((relocCount - 1) * 18)
        }

        val liveLowSpendPenalty = when {
            payrollPct < 58 -> 22
            payrollPct < LOW_SPENDING_CAP_THRESHOLD_PCT -> 14
            payrollPct < 82 -> 6
            else -> 0
        }
        val cumulativeLowSpendPenalty = (franchise.lowSpendingSeasons.coerceAtLeast(0) * 6).coerceAtMost(24)
        val totalLowSpendPenalty = liveLowSpendPenalty + cumulativeLowSpendPenalty

        val generousPayrollBonus = when {
            payrollPct >= 100 -> 10
            payrollPct >= GENEROUS_SPENDING_CAP_THRESHOLD_PCT -> 7
            payrollPct >= 82 -> 3
            else -> 0
        }
        val anchorLoyaltyBonus = if (relocCount == 0) 4 else 0
        val facilityBonus = when {
            franchise.facilities.trainingTier >= 4 -> 6
            franchise.facilities.trainingTier >= 3 -> 4
            franchise.facilities.trainingTier >= 2 -> 2
            else -> 0
        }
        val titleBonus = (franchise.championships * 3).coerceAtMost(9)
        val ownerTrustDelta = when {
            franchise.ownership.ownerApproval >= 82 -> 3
            franchise.ownership.ownerApproval < 52 -> -6
            else -> 0
        }
        val fanLoyaltyDelta = when {
            franchise.fanLoyalty >= 82 -> 3
            franchise.fanLoyalty < 50 -> -5
            else -> 0
        }

        val stabilityBonus = (
            generousPayrollBonus +
                anchorLoyaltyBonus +
                facilityBonus +
                titleBonus +
                ownerTrustDelta +
                fanLoyaltyDelta
            )

        val rawScore = 72 - relocationPenalty - totalLowSpendPenalty + stabilityBonus
        val score = rawScore.coerceIn(10, 99)
        val tier = TeamReputationTier.fromScore(score)

        val salaryPremium = when {
            score < 44 -> 4L
            score < 64 -> 2L
            else -> 0L
        }

        val baseLikelihood = (72 + ((score - 65) * 1.05).roundToInt()).coerceIn(14, 96)

        val drivers = buildList {
            if (relocCount > 0) {
                add("-$relocationPenalty Rep: $relocCount franchise relocation(s) rattled free agents")
            } else {
                add("+$anchorLoyaltyBonus Rep: Unbroken city roots & stable home market")
            }
            if (liveLowSpendPenalty > 0) {
                add("-$liveLowSpendPenalty Rep: Frugal payroll at $payrollPct% of \$${cap}M cap")
            } else if (generousPayrollBonus > 0) {
                add("+$generousPayrollBonus Rep: Competitive payroll investment ($payrollPct% of cap)")
            }
            if (franchise.lowSpendingSeasons > 0) {
                add("-$cumulativeLowSpendPenalty Rep: ${franchise.lowSpendingSeasons} prior low-spending season(s)")
            }
            if (facilityBonus > 0) {
                add("+$facilityBonus Rep: Tier-${franchise.facilities.trainingTier} training infrastructure")
            }
            if (titleBonus > 0) {
                add("+$titleBonus Rep: ${franchise.championships} championship banner(s)")
            }
            if (ownerTrustDelta < 0) {
                add("$ownerTrustDelta Rep: Ownership turmoil (${franchise.ownership.ownerApproval}% approval)")
            }
        }

        val summaryNote = when {
            relocCount >= 2 && totalLowSpendPenalty >= 12 ->
                "Repeated relocations ($relocCount) and low payroll ($payrollPct% cap) heavily deter free agents."
            relocCount >= 2 ->
                "$relocCount recent relocations make players skeptical of long-term stability."
            totalLowSpendPenalty >= 14 ->
                "Frugal spending ($payrollPct% of cap) signals weak ownership championship commitment."
            score >= 82 ->
                "Premier ownership reputation & $payrollPct% cap spending attract top free agents."
            else ->
                "Balanced ownership investment ($payrollPct% of cap) and market stability."
        }

        return TeamReputationBreakdown(
            score = score,
            tier = tier,
            relocationCount = relocCount,
            relocationPenalty = relocationPenalty,
            payrollPctOfCap = payrollPct,
            lowSpendingPenalty = totalLowSpendPenalty,
            lowSpendingSeasons = franchise.lowSpendingSeasons,
            stabilityBonus = stabilityBonus,
            reputationSalaryPremiumMillions = salaryPremium,
            baseFreeAgentLikelihoodPct = baseLikelihood,
            summaryNote = summaryNote,
            drivers = drivers
        )
    }

    fun applyReputationToFranchise(
        franchise: Franchise,
        salaryCapMillions: Long
    ): Franchise {
        val breakdown = evaluateReputation(franchise, salaryCapMillions)
        return franchise.copy(
            reputationScore = breakdown.score,
            lastReputationNote = breakdown.summaryNote
        )
    }

    fun recordRelocationBehavior(
        franchise: Franchise,
        salaryCapMillions: Long
    ): Franchise {
        val moved = franchise.copy(relocationCount = franchise.relocationCount + 1)
        return applyReputationToFranchise(moved, salaryCapMillions)
    }

    fun refreshAllFranchiseReputations(
        franchises: List<Franchise>,
        salaryCapMillions: Long,
        incrementLowSpendingSeason: Boolean = false
    ): List<Franchise> {
        val cap = salaryCapMillions.coerceAtLeast(80L)
        return franchises.map { team ->
            val payrollPct = ((team.annualPayrollMillions.toDouble() / cap.toDouble()) * 100.0).roundToInt()
            val updatedLowSpendSeasons = if (incrementLowSpendingSeason) {
                when {
                    payrollPct < LOW_SPENDING_CAP_THRESHOLD_PCT -> (team.lowSpendingSeasons + 1).coerceAtMost(6)
                    payrollPct >= GENEROUS_SPENDING_CAP_THRESHOLD_PCT -> (team.lowSpendingSeasons - 1).coerceAtLeast(0)
                    else -> team.lowSpendingSeasons
                }
            } else {
                team.lowSpendingSeasons
            }
            applyReputationToFranchise(
                team.copy(lowSpendingSeasons = updatedLowSpendSeasons),
                cap
            )
        }
    }

    fun evaluateFreeAgentSigningOdds(
        freeAgent: FreeAgentPlayer,
        franchise: Franchise,
        salaryCapMillions: Long,
        offerSalaryMillions: Long = freeAgent.askingSalaryMillions,
        isUrgentDepthNeed: Boolean = false
    ): FreeAgentSigningOdds {
        val rep = evaluateReputation(franchise, salaryCapMillions)
        val starSensitivityPenalty = when {
            freeAgent.overallRating >= 84 && rep.score < 58 -> 18
            freeAgent.overallRating >= 80 && rep.score < 50 -> 12
            freeAgent.overallRating >= 84 && rep.score >= 82 -> -8 // converted to bonus below
            else -> 0
        }
        val relocPenalty = if (rep.relocationCount >= 2) 10 else if (rep.relocationCount == 1) 4 else 0
        val frugalPenalty = if (rep.payrollPctOfCap < LOW_SPENDING_CAP_THRESHOLD_PCT && freeAgent.overallRating >= 78) 9 else 0
        val contenderBonus = when {
            franchise.rosterRating >= 85 || franchise.winPct >= 0.62 -> 7
            franchise.rosterRating >= 80 -> 3
            franchise.rosterRating < 72 && franchise.winPct < 0.35 -> -6
            else -> 0
        }
        val overpayBonus = ((offerSalaryMillions - freeAgent.askingSalaryMillions).coerceAtLeast(0L) * 3L)
            .toInt()
            .coerceAtMost(12)
        val depthToleranceBonus = if (isUrgentDepthNeed && freeAgent.overallRating <= 75) 14 else 0

        val rawOdds = 74 +
            ((rep.score - 65) * 1.10).roundToInt() -
            starSensitivityPenalty -
            relocPenalty -
            frugalPenalty +
            contenderBonus +
            overpayBonus +
            depthToleranceBonus

        val likelihoodPct = rawOdds.coerceIn(8, 98)
        val extraStarTax = if (rep.score < 64 && freeAgent.overallRating >= 83) 1L else 0L
        val demandedPremium = rep.reputationSalaryPremiumMillions + extraStarTax

        val driverSummary = buildString {
            append("${rep.tier.label} (${rep.score}/100)")
            if (rep.relocationCount > 0) append(" • ${rep.relocationCount} Reloc")
            append(" • ${rep.payrollPctOfCap}% Cap Spend")
            if (demandedPremium > 0L) append(" • +\$${demandedPremium}M Rep Tax")
        }

        val refusalReason = if (likelihoodPct < 60) {
            val causes = buildList {
                if (rep.relocationCount > 0) add("${rep.relocationCount} franchise relocation(s)")
                if (rep.payrollPctOfCap < LOW_SPENDING_CAP_THRESHOLD_PCT) {
                    add("low salary spending (${rep.payrollPctOfCap}% of \$${salaryCapMillions}M cap)")
                }
                if (rep.lowSpendingSeasons > 0) add("${rep.lowSpendingSeasons} low-spend season(s)")
            }.ifEmpty { listOf("sub-par organizational reputation (${rep.score}/100)") }
            "${freeAgent.name} (${freeAgent.overallRating} OVR) declined ${franchise.teamFullName} ($likelihoodPct% odds): Agents cited ${causes.joinToString(" & ")}."
        } else null

        return FreeAgentSigningOdds(
            likelihoodPct = likelihoodPct,
            reputationScore = rep.score,
            reputationTier = rep.tier,
            demandedSalaryPremiumMillions = demandedPremium,
            isContestedOrRejected = likelihoodPct < 60,
            driverSummary = driverSummary,
            refusalReason = refusalReason
        )
    }

    fun shouldAcceptFreeAgentOffer(
        odds: FreeAgentSigningOdds,
        freeAgent: FreeAgentPlayer,
        franchise: Franchise,
        week: Int,
        isUrgentDepthNeed: Boolean,
        signingRollOverride: Int? = null
    ): Boolean {
        val roll = signingRollOverride?.coerceIn(0, 99)
            ?: Random(abs(franchise.id.hashCode() * 31 + freeAgent.id.hashCode() * 17 + week)).nextInt(100)

        // Emergency low-OVR depth players accept slightly lower thresholds so teams aren't soft-locked out of fielding 7 players
        if (isUrgentDepthNeed && freeAgent.overallRating <= 74) {
            return odds.likelihoodPct >= 22 && (signingRollOverride == null || roll < (odds.likelihoodPct + 20).coerceAtMost(98))
        }

        // Strongly negative reputation causes marquee & rotation free agents to turn down the franchise
        if (odds.likelihoodPct < 36) return false

        // High-reputation clubs close signings reliably unless explicitly rolled above odds in probabilistic checks
        if (odds.likelihoodPct >= 75 && signingRollOverride == null) return true

        return roll < odds.likelihoodPct
    }
}
