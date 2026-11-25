package com.example.model

import com.example.viewmodel.LeagueUiState
import kotlin.random.Random

/**
 * Procedural Random Labor Union Contract Dispute Engine.
 * Generates mid-season and offseason collective bargaining / contract disputes
 * that force Commissioner negotiations impacting team morale and league-wide
 * salary structures (salary cap, luxury tax apron, revenue split, rookie/min scale).
 */
object LaborDisputeEngine {

    enum class DisputeScenario(
        val id: String,
        val headline: String,
        val repName: String,
        val repTitle: String,
        val repQuote: String
    ) {
        SUPERMAX_CAP_STANDOFF(
            id = "dispute_supermax_cap",
            headline = "PA CONTRACT DISPUTE: SUPERMAX & CAP CEILING STANDOFF",
            repName = "Jaxson 'Flash' Montgomery",
            repTitle = "Superstar Player Council Chair",
            repQuote = "Franchise revenues keep climbing while max-tier extensions stay squeezed. Raise the salary cap ceiling or locker rooms will lose faith."
        ),
        ROOKIE_WAGE_SCALE_PROTEST(
            id = "dispute_rookie_scale",
            headline = "PA CONTRACT DISPUTE: ROOKIE WAGE SCALE & MIN SALARY CLASH",
            repName = "DeMarcus Vance",
            repTitle = "PA Executive Director",
            repQuote = "Young core starters carry heavy minutes on fixed rookie deals. Players demand an upgraded rookie scale and higher minimum pay floor."
        ),
        LUXURY_TAX_APRON_WALKOUT(
            id = "dispute_luxury_tax_apron",
            headline = "PA CONTRACT DISPUTE: LUXURY TAX APRON FREEZE PROTEST",
            repName = "Devon Brooks",
            repTitle = "Lead CBA Counsel",
            repQuote = "Punitive luxury tax aprons have frozen mid-tier veteran signings across contenders. Ease the tax threshold before locker-room morale fractures."
        ),
        BRI_ESCROW_GRIEVANCE(
            id = "dispute_bri_escrow",
            headline = "PA CONTRACT DISPUTE: REVENUE SPLIT & ESCROW CLAWBACK",
            repName = "Elena Rostova",
            repTitle = "Player Finance & Benefits Rep",
            repQuote = "Escrow withholdings ate directly into guaranteed player paychecks. Adjust the league revenue split to restore clubhouse trust."
        ),
        MIDLEVEL_FLOOR_DISPUTE(
            id = "dispute_midlevel_floor",
            headline = "PA CONTRACT DISPUTE: PAYROLL FLOOR & VETERAN EXCEPTION",
            repName = "Marcus Vance",
            repTitle = "PA Bargaining Committee",
            repQuote = "Too many clubs are hoarding unspent cap room without paying competitive veteran wages. Establish firmer salary structure protections."
        )
    }

    data class RandomDisputeRoll(
        val trigger: LaborUnionTriggerEvent,
        val overlaySession: LaborNegotiationOverlaySession
    )

    fun maybeGenerateRandomContractDispute(
        state: LeagueUiState,
        triggerHistory: List<LaborUnionTriggerEvent>,
        seed: Long? = null,
        forceTrigger: Boolean = false
    ): RandomDisputeRoll? {
        val year = state.currentYear
        val week = state.currentWeek

        // Avoid crowding back-to-back weeks unless explicitly triggered
        if (!forceTrigger) {
            val recentRandom = triggerHistory.any {
                it.type == LaborTriggerType.CONTRACT_DISPUTE_RANDOM &&
                    it.seasonYear == year &&
                    kotlin.math.abs(it.week - week) < 4
            }
            if (recentRandom) return null

            val rng = Random(seed ?: (year * 4099L + week * 97L + state.unionAgreement.strikeRisk * 13L))
            val roll = rng.nextInt(100)
            // ~28% weekly chance in regular season (higher if strikeRisk > 25)
            val threshold = (26 + (state.unionAgreement.strikeRisk / 4)).coerceIn(22, 48)
            if (roll >= threshold) return null
        }

        val scenarioRng = Random(seed ?: (year * 7907L + week * 173L + System.currentTimeMillis() % 97L))
        val scenarios = DisputeScenario.values()
        val chosen = scenarios[scenarioRng.nextInt(scenarios.size)]
        val union = state.unionAgreement

        val avgTeamMorale = if (state.franchises.isNotEmpty()) state.franchises.map { it.teamMorale }.average().toInt() else 78
        val avgFanMorale = if (state.franchises.isNotEmpty()) state.franchises.map { it.fanMorale }.average().toInt() else 75
        val avgLoyalty = if (state.franchises.isNotEmpty()) state.franchises.map { it.fanLoyalty }.average().toInt() else 75
        val fanSentimentPct = ((avgFanMorale * 0.55) + (avgLoyalty * 0.45)).toInt().coerceIn(0, 100)

        val elevatedCap = (union.salaryCapMillions + 8L).coerceIn(70L, 180L)
        val compromiseCap = (union.salaryCapMillions + 4L).coerceIn(70L, 180L)
        val tightenedCap = (union.salaryCapMillions - 4L).coerceAtLeast(60L)

        val choices = when (chosen) {
            DisputeScenario.SUPERMAX_CAP_STANDOFF -> listOf(
                LaborNegotiationChoice(
                    id = "dispute_concede_supermax",
                    title = "Raise Salary Cap to \$${elevatedCap}M & +2% Max Share",
                    stanceTag = "CONCEDE • CAP EXPANSION",
                    summary = "Expand the league salary cap ceiling to \$${elevatedCap}M and max contract share to ${(union.maxContractCapPct + 2).coerceAtMost(36)}%, surging team morale (+7%).",
                    projectedCashImpactMillions = -4L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 12L,
                    projectedFanMoraleDelta = 5,
                    projectedFanLoyaltyDelta = 3,
                    projectedTeamMoraleDelta = 7,
                    projectedUnionSatisfactionDelta = 12,
                    projectedStrikeRiskDelta = -10,
                    requestedSalaryCapMillions = elevatedCap,
                    requestedLuxuryTaxThresholdDelta = 8L,
                    requestedMaxContractCapDeltaPct = 2
                ),
                LaborNegotiationChoice(
                    id = "dispute_compromise_supermax",
                    title = "Compromise: +\$4M Cap (\$${compromiseCap}M) + Veteran Escalator",
                    stanceTag = "COMPROMISE • BALANCED",
                    summary = "Split the gap with a \$4M salary cap lift and +1% max contract tier, stabilizing clubhouse morale (+3%).",
                    projectedCashImpactMillions = -2L,
                    projectedWeeklyNetImpactMillions = 0L,
                    projectedValuationImpactMillions = 6L,
                    projectedFanMoraleDelta = 2,
                    projectedFanLoyaltyDelta = 1,
                    projectedTeamMoraleDelta = 3,
                    projectedUnionSatisfactionDelta = 6,
                    projectedStrikeRiskDelta = -5,
                    requestedSalaryCapMillions = compromiseCap,
                    requestedLuxuryTaxThresholdDelta = 4L,
                    requestedMaxContractCapDeltaPct = 1
                ),
                LaborNegotiationChoice(
                    id = "dispute_freeze_supermax",
                    title = "Enforce Cap Freeze & Tight Max Threshold",
                    stanceTag = "OWNER HARDLINE",
                    summary = "Hold payroll limits tight (-\$$4M cap squeeze) to shield owner margins, hurting team morale (-8%) and raising strike risk.",
                    projectedCashImpactMillions = 3L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = -8L,
                    projectedFanMoraleDelta = -5,
                    projectedFanLoyaltyDelta = -3,
                    projectedTeamMoraleDelta = -8,
                    projectedUnionSatisfactionDelta = -10,
                    projectedStrikeRiskDelta = 11,
                    requestedSalaryCapMillions = tightenedCap,
                    requestedMaxContractCapDeltaPct = -1
                )
            )

            DisputeScenario.ROOKIE_WAGE_SCALE_PROTEST -> listOf(
                LaborNegotiationChoice(
                    id = "dispute_concede_rookie_scale",
                    title = "Boost Rookie Scale (+12%) & Min Salary (+\$150K)",
                    stanceTag = "CONCEDE • PAY SCALE",
                    summary = "Lift league rookie wage contracts by +12% and raise minimum player pay to \$${union.minPlayerSalaryK + 150L}K, boosting locker-room morale (+6%).",
                    projectedCashImpactMillions = -3L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 9L,
                    projectedFanMoraleDelta = 4,
                    projectedFanLoyaltyDelta = 2,
                    projectedTeamMoraleDelta = 6,
                    projectedUnionSatisfactionDelta = 11,
                    projectedStrikeRiskDelta = -9,
                    requestedRookieScaleDeltaPct = 12,
                    requestedMinSalaryDeltaK = 150L,
                    requestedSalaryCapMillions = (union.salaryCapMillions + 3L).coerceAtMost(180L)
                ),
                LaborNegotiationChoice(
                    id = "dispute_compromise_rookie_scale",
                    title = "Indexed +5% Rookie Scale & +\$50K Min Salary",
                    stanceTag = "COMPROMISE • INDEXED",
                    summary = "Introduce modest rookie contract incentives (+5% scale, +\$50K min) with steady clubhouse morale (+3%).",
                    projectedCashImpactMillions = -1L,
                    projectedWeeklyNetImpactMillions = 0L,
                    projectedValuationImpactMillions = 4L,
                    projectedFanMoraleDelta = 2,
                    projectedFanLoyaltyDelta = 1,
                    projectedTeamMoraleDelta = 3,
                    projectedUnionSatisfactionDelta = 5,
                    projectedStrikeRiskDelta = -4,
                    requestedRookieScaleDeltaPct = 5,
                    requestedMinSalaryDeltaK = 50L
                ),
                LaborNegotiationChoice(
                    id = "dispute_reject_rookie_scale",
                    title = "Hold Rookie Scale (-6%) & Cap Veteran Minimum",
                    stanceTag = "OWNER HARDLINE",
                    summary = "Trim rookie scale growth by 6% to preserve draft asset bargains, triggering locker-room frustration (-7% team morale).",
                    projectedCashImpactMillions = 2L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = -6L,
                    projectedFanMoraleDelta = -4,
                    projectedFanLoyaltyDelta = -2,
                    projectedTeamMoraleDelta = -7,
                    projectedUnionSatisfactionDelta = -9,
                    projectedStrikeRiskDelta = 10,
                    requestedRookieScaleDeltaPct = -6
                )
            )

            DisputeScenario.LUXURY_TAX_APRON_WALKOUT -> listOf(
                LaborNegotiationChoice(
                    id = "dispute_concede_apron",
                    title = "Raise Luxury Tax Apron +\$12M & Soft Cap Relief",
                    stanceTag = "CONCEDE • TAX RELIEF",
                    summary = "Lift the league luxury tax apron +\$12M and enact Soft Cap + Luxury Tax structure, lifting team morale (+7%).",
                    projectedCashImpactMillions = -3L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 10L,
                    projectedFanMoraleDelta = 5,
                    projectedFanLoyaltyDelta = 3,
                    projectedTeamMoraleDelta = 7,
                    projectedUnionSatisfactionDelta = 12,
                    projectedStrikeRiskDelta = -10,
                    requestedSalaryCapMillions = (union.salaryCapMillions + 6L).coerceAtMost(180L),
                    requestedSalaryCapType = SalaryCapType.SOFT_CAP_LUXURY_TAX,
                    requestedLuxuryTaxThresholdDelta = 12L
                ),
                LaborNegotiationChoice(
                    id = "dispute_compromise_apron",
                    title = "Targeted +\$6M Luxury Tax Buffer",
                    stanceTag = "COMPROMISE • SMOOTHING",
                    summary = "Expand the luxury tax threshold +\$6M for contenders while maintaining cap discipline (+3% team morale).",
                    projectedCashImpactMillions = -1L,
                    projectedWeeklyNetImpactMillions = 0L,
                    projectedValuationImpactMillions = 5L,
                    projectedFanMoraleDelta = 2,
                    projectedFanLoyaltyDelta = 1,
                    projectedTeamMoraleDelta = 3,
                    projectedUnionSatisfactionDelta = 6,
                    projectedStrikeRiskDelta = -5,
                    requestedSalaryCapMillions = (union.salaryCapMillions + 3L).coerceAtMost(180L),
                    requestedLuxuryTaxThresholdDelta = 6L
                ),
                LaborNegotiationChoice(
                    id = "dispute_enforce_hard_cap",
                    title = "Tighten Tax Apron & Mandate Hard Cap Line",
                    stanceTag = "HARD CAP ENFORCEMENT",
                    summary = "Impose Hard Salary Cap enforcement and shrink apron flexibility, depressing team morale (-9%) and spiking strike risk.",
                    projectedCashImpactMillions = 3L,
                    projectedWeeklyNetImpactMillions = 2L,
                    projectedValuationImpactMillions = -10L,
                    projectedFanMoraleDelta = -6,
                    projectedFanLoyaltyDelta = -3,
                    projectedTeamMoraleDelta = -9,
                    projectedUnionSatisfactionDelta = -11,
                    projectedStrikeRiskDelta = 12,
                    requestedSalaryCapType = SalaryCapType.HARD_CAP,
                    requestedLuxuryTaxThresholdDelta = -6L
                )
            )

            DisputeScenario.BRI_ESCROW_GRIEVANCE -> listOf(
                LaborNegotiationChoice(
                    id = "dispute_concede_escrow",
                    title = "Refund Escrow & Raise Revenue Split (+2%)",
                    stanceTag = "CONCEDE • +2% SPLIT",
                    summary = "Return escrow withholding (-\$5M cash), lift player share +2% and expand cap +\$5M, energizing team morale (+8%).",
                    projectedCashImpactMillions = -5L,
                    projectedWeeklyNetImpactMillions = -2L,
                    projectedValuationImpactMillions = 11L,
                    projectedFanMoraleDelta = 5,
                    projectedFanLoyaltyDelta = 4,
                    projectedTeamMoraleDelta = 8,
                    projectedUnionSatisfactionDelta = 14,
                    projectedStrikeRiskDelta = -12,
                    projectedRevenueShareDeltaPct = 2,
                    requestedSalaryCapMillions = (union.salaryCapMillions + 5L).coerceAtMost(180L),
                    requestedLuxuryTaxThresholdDelta = 5L
                ),
                LaborNegotiationChoice(
                    id = "dispute_compromise_escrow",
                    title = "Cap Smoothing Accord (+1% Split, +\$2M Cap)",
                    stanceTag = "COMPROMISE • +1% SPLIT",
                    summary = "Trim escrow penalties via +1% revenue share and +\$2M cap growth, lifting team morale (+4%).",
                    projectedCashImpactMillions = -2L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 5L,
                    projectedFanMoraleDelta = 3,
                    projectedFanLoyaltyDelta = 2,
                    projectedTeamMoraleDelta = 4,
                    projectedUnionSatisfactionDelta = 7,
                    projectedStrikeRiskDelta = -6,
                    projectedRevenueShareDeltaPct = 1,
                    requestedSalaryCapMillions = (union.salaryCapMillions + 2L).coerceAtMost(180L)
                ),
                LaborNegotiationChoice(
                    id = "dispute_withhold_escrow",
                    title = "Enforce Full Owner Escrow Clawback",
                    stanceTag = "OWNER HARDLINE",
                    summary = "Retain \$4M in escrow reserves without split relief, damaging team morale (-8%) and union goodwill.",
                    projectedCashImpactMillions = 4L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = -9L,
                    projectedFanMoraleDelta = -5,
                    projectedFanLoyaltyDelta = -3,
                    projectedTeamMoraleDelta = -8,
                    projectedUnionSatisfactionDelta = -10,
                    projectedStrikeRiskDelta = 11,
                    projectedRevenueShareDeltaPct = -1
                )
            )

            DisputeScenario.MIDLEVEL_FLOOR_DISPUTE -> listOf(
                LaborNegotiationChoice(
                    id = "dispute_concede_midlevel",
                    title = "Expand Mid-Level Exception & +\$6M Cap Room",
                    stanceTag = "CONCEDE • PAY FLOOR",
                    summary = "Add +\$6M salary cap room and \$100K minimum wage lift, boosting veteran depth and team morale (+6%).",
                    projectedCashImpactMillions = -3L,
                    projectedWeeklyNetImpactMillions = -1L,
                    projectedValuationImpactMillions = 8L,
                    projectedFanMoraleDelta = 4,
                    projectedFanLoyaltyDelta = 2,
                    projectedTeamMoraleDelta = 6,
                    projectedUnionSatisfactionDelta = 11,
                    projectedStrikeRiskDelta = -9,
                    requestedSalaryCapMillions = (union.salaryCapMillions + 6L).coerceAtMost(180L),
                    requestedMinSalaryDeltaK = 100L,
                    requestedLuxuryTaxThresholdDelta = 6L
                ),
                LaborNegotiationChoice(
                    id = "dispute_compromise_midlevel",
                    title = "Balanced +\$3M Exception & Welfare Boost",
                    stanceTag = "COMPROMISE • EXCEPTION",
                    summary = "Authorize a +\$3M cap exception for mid-tier veterans, raising team morale (+3%).",
                    projectedCashImpactMillions = -1L,
                    projectedWeeklyNetImpactMillions = 0L,
                    projectedValuationImpactMillions = 4L,
                    projectedFanMoraleDelta = 2,
                    projectedFanLoyaltyDelta = 1,
                    projectedTeamMoraleDelta = 3,
                    projectedUnionSatisfactionDelta = 6,
                    projectedStrikeRiskDelta = -5,
                    requestedSalaryCapMillions = (union.salaryCapMillions + 3L).coerceAtMost(180L)
                ),
                LaborNegotiationChoice(
                    id = "dispute_reject_midlevel",
                    title = "Reject Exception Expansion & Hold Floor",
                    stanceTag = "COST CONTAINMENT",
                    summary = "Keep team payroll floors unchanged to protect small-market cash, reducing team morale (-6%) and nudging strike risk up.",
                    projectedCashImpactMillions = 2L,
                    projectedWeeklyNetImpactMillions = 1L,
                    projectedValuationImpactMillions = -5L,
                    projectedFanMoraleDelta = -4,
                    projectedFanLoyaltyDelta = -2,
                    projectedTeamMoraleDelta = -6,
                    projectedUnionSatisfactionDelta = -8,
                    projectedStrikeRiskDelta = 8
                )
            )
        }

        val severity = if (union.strikeRisk >= 35 || avgTeamMorale < 65) {
            LaborTriggerSeverity.CRITICAL
        } else {
            LaborTriggerSeverity.WARNING
        }

        val triggerId = "labor_${year}_w${week}_${chosen.id}_${scenarioRng.nextInt(100, 999)}"
        val trigger = LaborUnionTriggerEvent(
            id = triggerId,
            type = LaborTriggerType.CONTRACT_DISPUTE_RANDOM,
            severity = severity,
            title = chosen.headline,
            description = "${chosen.repName} (${chosen.repTitle}) triggered a league-wide contract dispute in Week $week. Current Salary Cap: \$${union.salaryCapMillions}M • Avg Team Morale: $avgTeamMorale%.",
            recommendedAction = "Negotiate contract terms in the Labor Bargaining Overlay to protect team morale and stabilize league salary structures.",
            seasonYear = year,
            week = week,
            strikeRiskSnapshot = union.strikeRisk,
            cbaYearsRemainingSnapshot = union.cbaYearsRemaining,
            autoPromptNegotiation = true,
            disputeScenarioId = chosen.id
        )

        val overlay = LaborNegotiationOverlaySession(
            id = "overlay_$triggerId",
            triggerType = LaborTriggerType.CONTRACT_DISPUTE_RANDOM,
            severity = severity,
            headline = chosen.headline,
            subheadline = "Random Week $week Union Contract Dispute • Avg Team Morale: $avgTeamMorale% • Cap: \$${union.salaryCapMillions}M • Lux Tax: \$${union.luxuryTaxThresholdMillions}M • Rookie Scale: ${union.rookieScaleMultiplierPct}%.",
            unionRepName = chosen.repName,
            unionRepTitle = chosen.repTitle,
            unionRepQuote = chosen.repQuote,
            currentCashMillions = state.cashReservesMillions,
            currentFanSentimentPct = fanSentimentPct,
            currentStrikeRiskPct = union.strikeRisk,
            choices = choices,
            currentTeamMoralePct = avgTeamMorale,
            disputeScenarioId = chosen.id
        )

        return RandomDisputeRoll(trigger = trigger, overlaySession = overlay)
    }
}
