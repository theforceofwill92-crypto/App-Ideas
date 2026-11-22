package com.example.model

import kotlin.math.roundToInt

enum class GameplayRuleDomain(
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val badgeColorHex: Long
) {
    OVERTIME_PROCEDURES(
        title = "Overtime Procedures",
        shortLabel = "Overtime",
        subtitle = "Extra-time tension, fan drama & clutch scoring tempo",
        badgeColorHex = 0xFFF59E0B
    ),
    ROSTER_SIZES(
        title = "Roster Sizes & Bench",
        shortLabel = "Roster Size",
        subtitle = "Active/reserve slots, injury resiliency & payroll load",
        badgeColorHex = 0xFF3B82F6
    ),
    GAME_LENGTH(
        title = "Game Length & Pace",
        shortLabel = "Game Length",
        subtitle = "Regulation minutes, scoring volume & player stamina",
        badgeColorHex = 0xFF06B6D4
    ),
    INJURY_PHYSICALITY(
        title = "Physicality & Injuries",
        shortLabel = "Physicality",
        subtitle = "Contact whistle strictness & star health durability",
        badgeColorHex = 0xFFEF4444
    ),
    PACE_AND_SCORING(
        title = "Scoring & Tempo Bylaws",
        shortLabel = "Scoring Bylaws",
        subtitle = "Shot clock enforcement, freedom of movement & replay",
        badgeColorHex = 0xFF10B981
    )
}

data class LeagueGameplayEffectsSnapshot(
    val fanExcitementScore: Int,
    val fanExcitementBonusPct: Int,
    val scoringTempoBonusPct: Int,
    val teamInjuryResiliencyPct: Int,
    val teamPerformanceIndex: Int,
    val payrollOverheadDeltaPct: Int,
    val activeSummaryLabel: String
)

data class GameplayRuleProposalImpact(
    val fanExcitementDeltaPct: Int,
    val scoringTempoDeltaPct: Int,
    val teamPerformanceDeltaPct: Int,
    val injuryResiliencyDeltaPct: Int,
    val payrollOverheadDeltaPct: Int,
    val projectedFanExcitementScore: Int,
    val projectedTeamPerformanceIndex: Int,
    val verdictHeadline: String
)

data class GameplayRuleProposal(
    val id: String,
    val domain: GameplayRuleDomain,
    val optionLabel: String,
    val proposalTitle: String,
    val proposalSummary: String,
    val overtimeFormat: OvertimeFormat? = null,
    val rosterSizeRule: RosterSizeRule? = null,
    val gameLengthRule: GameLengthRule? = null,
    val injuryFrequencyRule: InjuryFrequencyRule? = null,
    val toggleRuleId: String? = null,
    val targetToggleEnacted: Boolean? = null,
    val impact: GameplayRuleProposalImpact,
    val projectedOwnerVote: BoardVoteResult,
    val isAlreadyActive: Boolean
)

object LeagueRulesEffectCalculator {

    fun computeCurrentEffects(
        ruleBook: LeagueRuleBook,
        franchises: List<Franchise>
    ): LeagueGameplayEffectsSnapshot {
        val isShotClock = ruleBook.isRuleEnacted("rule_shot_clock")
        val isFreedom = ruleBook.isRuleEnacted("rule_freedom_of_movement")
        val isReplay = ruleBook.isRuleEnacted("rule_instant_replay")

        val injuryAppealMod = when (ruleBook.injuryFrequency) {
            InjuryFrequencyRule.INJURIES_OFF -> -2
            InjuryFrequencyRule.LOW_PROTECTED -> +4
            InjuryFrequencyRule.STANDARD_REALISTIC -> 0
            InjuryFrequencyRule.HIGH_GRIND -> -4
        }
        val injuryResiliencyMod = when (ruleBook.injuryFrequency) {
            InjuryFrequencyRule.INJURIES_OFF -> +20
            InjuryFrequencyRule.LOW_PROTECTED -> +10
            InjuryFrequencyRule.STANDARD_REALISTIC -> 0
            InjuryFrequencyRule.HIGH_GRIND -> -14
        }
        val gameLengthFatigueMod = when (ruleBook.gameLength) {
            GameLengthRule.SHORTENED_36 -> +5
            GameLengthRule.STANDARD_48 -> 0
            GameLengthRule.MARATHON_60 -> -6
        }

        val fanAppealBonusPct = ruleBook.overtimeFormat.fanAppealBonus +
            ruleBook.gameLength.fanAppealBonus +
            (if (isShotClock) 6 else -5) +
            (if (isFreedom) 4 else -2) +
            injuryAppealMod

        val scoringTempoBonusPct = ruleBook.overtimeFormat.scoringTempoBonus +
            (if (isShotClock) 10 else -6) +
            (if (isFreedom) 5 else -3) +
            (if (ruleBook.rosterSizeRule == RosterSizeRule.EXPANDED_18) 2 else 0)

        val totalResiliencyPct = ruleBook.rosterSizeRule.injuryResiliencyBonus +
            injuryResiliencyMod +
            gameLengthFatigueMod

        val payrollOverheadPct = ruleBook.rosterSizeRule.payrollBurdenModifier

        val avgMorale = if (franchises.isNotEmpty()) franchises.map { it.fanMorale }.average() else 75.0
        val avgLoyalty = if (franchises.isNotEmpty()) franchises.map { it.fanLoyalty }.average() else 75.0
        val baseSentiment = (avgMorale * 0.55) + (avgLoyalty * 0.45)
        val fanExcitementScore = (baseSentiment + (fanAppealBonusPct * 0.45) + (scoringTempoBonusPct * 0.15))
            .roundToInt()
            .coerceIn(25, 100)

        val avgRosterRating = if (franchises.isNotEmpty()) franchises.map { it.effectiveLineupRating }.average() else 78.0
        val depthBonus = when (ruleBook.rosterSizeRule) {
            RosterSizeRule.COMPACT_12 -> -1.5
            RosterSizeRule.STANDARD_15 -> +1.0
            RosterSizeRule.EXPANDED_18 -> +3.5
        }
        val replayFairness = if (isReplay) 1.5 else -1.0
        val teamPerformanceIndex = (avgRosterRating + depthBonus + replayFairness + (totalResiliencyPct * 0.14) + (scoringTempoBonusPct * 0.10))
            .roundToInt()
            .coerceIn(30, 100)

        val summaryLabel = "${ruleBook.overtimeFormat.shortLabel} • ${ruleBook.rosterSizeRule.activeSlots}+${ruleBook.rosterSizeRule.reserveSlots} Roster"

        return LeagueGameplayEffectsSnapshot(
            fanExcitementScore = fanExcitementScore,
            fanExcitementBonusPct = fanAppealBonusPct,
            scoringTempoBonusPct = scoringTempoBonusPct,
            teamInjuryResiliencyPct = totalResiliencyPct,
            teamPerformanceIndex = teamPerformanceIndex,
            payrollOverheadDeltaPct = payrollOverheadPct,
            activeSummaryLabel = summaryLabel
        )
    }

    fun getDomainProposals(
        domain: GameplayRuleDomain,
        ruleBook: LeagueRuleBook,
        franchises: List<Franchise>,
        year: Int,
        week: Int
    ): List<GameplayRuleProposal> {
        val baseline = computeCurrentEffects(ruleBook, franchises)
        return when (domain) {
            GameplayRuleDomain.OVERTIME_PROCEDURES -> OvertimeFormat.values().map { fmt ->
                val simulatedRuleBook = ruleBook.copy(overtimeFormat = fmt)
                val projected = computeCurrentEffects(simulatedRuleBook, franchises)
                val title = "Overtime Format: ${fmt.displayName}"
                val vote = OwnerGovernanceEngine.evaluateSettingRuleVote(
                    franchises = franchises,
                    proposalTitle = title,
                    proposalSummary = fmt.description,
                    overtimeFormat = fmt,
                    year = year,
                    week = week
                )
                GameplayRuleProposal(
                    id = "prop_ot_${fmt.name.lowercase()}",
                    domain = domain,
                    optionLabel = fmt.shortLabel,
                    proposalTitle = title,
                    proposalSummary = fmt.description,
                    overtimeFormat = fmt,
                    impact = buildImpactDiff(baseline, projected),
                    projectedOwnerVote = vote,
                    isAlreadyActive = ruleBook.overtimeFormat == fmt
                )
            }

            GameplayRuleDomain.ROSTER_SIZES -> RosterSizeRule.values().map { rule ->
                val simulatedRuleBook = ruleBook.copy(rosterSizeRule = rule)
                val projected = computeCurrentEffects(simulatedRuleBook, franchises)
                val title = "Roster Limit: ${rule.displayName}"
                val vote = OwnerGovernanceEngine.evaluateSettingRuleVote(
                    franchises = franchises,
                    proposalTitle = title,
                    proposalSummary = rule.description,
                    rosterSizeRule = rule,
                    year = year,
                    week = week
                )
                GameplayRuleProposal(
                    id = "prop_roster_${rule.name.lowercase()}",
                    domain = domain,
                    optionLabel = "${rule.activeSlots}+${rule.reserveSlots} Slots",
                    proposalTitle = title,
                    proposalSummary = rule.description,
                    rosterSizeRule = rule,
                    impact = buildImpactDiff(baseline, projected),
                    projectedOwnerVote = vote,
                    isAlreadyActive = ruleBook.rosterSizeRule == rule
                )
            }

            GameplayRuleDomain.GAME_LENGTH -> GameLengthRule.values().map { len ->
                val simulatedRuleBook = ruleBook.copy(gameLength = len)
                val projected = computeCurrentEffects(simulatedRuleBook, franchises)
                val title = "Game Length: ${len.displayName}"
                val vote = OwnerGovernanceEngine.evaluateSettingRuleVote(
                    franchises = franchises,
                    proposalTitle = title,
                    proposalSummary = len.description,
                    gameLength = len,
                    year = year,
                    week = week
                )
                GameplayRuleProposal(
                    id = "prop_len_${len.name.lowercase()}",
                    domain = domain,
                    optionLabel = len.shortLabel,
                    proposalTitle = title,
                    proposalSummary = len.description,
                    gameLengthRule = len,
                    impact = buildImpactDiff(baseline, projected),
                    projectedOwnerVote = vote,
                    isAlreadyActive = ruleBook.gameLength == len
                )
            }

            GameplayRuleDomain.INJURY_PHYSICALITY -> InjuryFrequencyRule.values().map { freq ->
                val simulatedRuleBook = ruleBook.copy(injuryFrequency = freq)
                val projected = computeCurrentEffects(simulatedRuleBook, franchises)
                val title = "Injury & Contact Policy: ${freq.displayName}"
                val vote = OwnerGovernanceEngine.evaluateSettingRuleVote(
                    franchises = franchises,
                    proposalTitle = title,
                    proposalSummary = freq.description,
                    injuryFrequency = freq,
                    year = year,
                    week = week
                )
                GameplayRuleProposal(
                    id = "prop_inj_${freq.name.lowercase()}",
                    domain = domain,
                    optionLabel = freq.shortLabel,
                    proposalTitle = title,
                    proposalSummary = freq.description,
                    injuryFrequencyRule = freq,
                    impact = buildImpactDiff(baseline, projected),
                    projectedOwnerVote = vote,
                    isAlreadyActive = ruleBook.injuryFrequency == freq
                )
            }

            GameplayRuleDomain.PACE_AND_SCORING -> {
                val targetIds = listOf("rule_shot_clock", "rule_freedom_of_movement", "rule_instant_replay")
                targetIds.mapNotNull { ruleId ->
                    val currentRule = ruleBook.rules.find { it.id == ruleId } ?: return@mapNotNull null
                    val targetEnacted = !currentRule.isEnacted
                    val updatedRules = ruleBook.rules.map { if (it.id == ruleId) it.copy(isEnacted = targetEnacted) else it }
                    val simulatedRuleBook = ruleBook.copy(
                        rules = updatedRules,
                        shotClockFastPace = if (ruleId == "rule_shot_clock") targetEnacted else ruleBook.shotClockFastPace,
                        instantReplayEnabled = if (ruleId == "rule_instant_replay") targetEnacted else ruleBook.instantReplayEnabled
                    )
                    val projected = computeCurrentEffects(simulatedRuleBook, franchises)
                    val vote = OwnerGovernanceEngine.evaluateRuleToggleVote(
                        franchises = franchises,
                        rule = currentRule,
                        targetEnacted = targetEnacted,
                        year = year,
                        week = week
                    )
                    val actionVerb = if (targetEnacted) "Enact" else "Repeal"
                    GameplayRuleProposal(
                        id = "prop_toggle_$ruleId",
                        domain = domain,
                        optionLabel = "$actionVerb ${currentRule.name.substringBefore(" ")}",
                        proposalTitle = "$actionVerb ${currentRule.name}",
                        proposalSummary = currentRule.description,
                        toggleRuleId = ruleId,
                        targetToggleEnacted = targetEnacted,
                        impact = buildImpactDiff(baseline, projected),
                        projectedOwnerVote = vote,
                        isAlreadyActive = false
                    )
                }
            }
        }
    }

    private fun buildImpactDiff(
        baseline: LeagueGameplayEffectsSnapshot,
        projected: LeagueGameplayEffectsSnapshot
    ): GameplayRuleProposalImpact {
        val fanDelta = projected.fanExcitementBonusPct - baseline.fanExcitementBonusPct
        val tempoDelta = projected.scoringTempoBonusPct - baseline.scoringTempoBonusPct
        val resiliencyDelta = projected.teamInjuryResiliencyPct - baseline.teamInjuryResiliencyPct
        val perfDelta = projected.teamPerformanceIndex - baseline.teamPerformanceIndex
        val payrollDelta = projected.payrollOverheadDeltaPct - baseline.payrollOverheadDeltaPct

        val verdict = when {
            fanDelta >= 8 && resiliencyDelta >= 0 -> "Fan Thriller + Roster Stability Boost"
            fanDelta >= 6 -> "Major Fan Excitement & Broadcast Surge"
            resiliencyDelta >= 8 -> "Deep Roster Durability & Depth Boost"
            payrollDelta <= -8 -> "Lean Payroll & Superstar Showcase"
            fanDelta < 0 && resiliencyDelta > 0 -> "Conservative Pace — Protects Stars"
            fanDelta == 0 && tempoDelta == 0 && resiliencyDelta == 0 -> "Active Current League Standard"
            else -> "Balanced Competition Calibration"
        }

        return GameplayRuleProposalImpact(
            fanExcitementDeltaPct = fanDelta,
            scoringTempoDeltaPct = tempoDelta,
            teamPerformanceDeltaPct = perfDelta,
            injuryResiliencyDeltaPct = resiliencyDelta,
            payrollOverheadDeltaPct = payrollDelta,
            projectedFanExcitementScore = projected.fanExcitementScore,
            projectedTeamPerformanceIndex = projected.teamPerformanceIndex,
            verdictHeadline = verdict
        )
    }
}
