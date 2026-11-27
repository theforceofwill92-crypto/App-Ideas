package com.example.model

import kotlin.math.roundToInt
import kotlin.random.Random

enum class LeagueScandalSeverity(
    val label: String,
    val badgeColorHex: Long,
    val baseStabilityDrop: Int,
    val unaddressedPayoutPenaltyPct: Int
) {
    MODERATE(
        label = "MODERATE SCANDAL",
        badgeColorHex = 0xFFF59E0B,
        baseStabilityDrop = 14,
        unaddressedPayoutPenaltyPct = 20
    ),
    MAJOR(
        label = "MAJOR SCANDAL",
        badgeColorHex = 0xFFEF4444,
        baseStabilityDrop = 22,
        unaddressedPayoutPenaltyPct = 30
    ),
    CRITICAL(
        label = "CRITICAL CRISIS",
        badgeColorHex = 0xFFDC2626,
        baseStabilityDrop = 32,
        unaddressedPayoutPenaltyPct = 40
    )
}

enum class LeagueScandalInterventionChoice(
    val title: String,
    val subtitle: String,
    val cashCostMillions: Long,
    val stabilityRestore: Int,
    val integrityDelta: Int,
    val ownerApprovalDelta: Int,
    val fanSentimentDelta: Int
) {
    AUDIT_AND_SANCTIONS(
        title = "Independent Audit & Sanctions",
        subtitle = "Launch full external review & levy compliance fines (-\$4M Cash, +22 Sponsor Stability, +5 Integrity, -3 Owners)",
        cashCostMillions = 4L,
        stabilityRestore = 22,
        integrityDelta = 5,
        ownerApprovalDelta = -3,
        fanSentimentDelta = 2
    ),
    TRANSPARENCY_TOWN_HALL(
        title = "Transparency & Compliance Restitution",
        subtitle = "Issue public restitution & sponsor assurances (-\$2M Cash, +16 Sponsor Stability, +3 Fan Sentiment, +2 Integrity)",
        cashCostMillions = 2L,
        stabilityRestore = 16,
        integrityDelta = 2,
        ownerApprovalDelta = 0,
        fanSentimentDelta = 3
    ),
    STONEWALL_IGNORE(
        title = "Stonewall / Decline Intervention",
        subtitle = "Refuse commissioner action — triggers immediate sponsor payout penalties, stability crash & potential pullouts",
        cashCostMillions = 0L,
        stabilityRestore = -18,
        integrityDelta = -8,
        ownerApprovalDelta = -4,
        fanSentimentDelta = -6
    )
}

data class LeagueScandalIncident(
    val id: String,
    val title: String,
    val summary: String,
    val severity: LeagueScandalSeverity,
    val affectedSlots: List<LeagueSponsorSlotType> = emptyList(),
    val affectedSponsorNames: List<String> = emptyList(),
    val stabilityDropApplied: Int = 0,
    val weeksUntilPenalty: Int = 2,
    val seasonYear: Int = 2026,
    val weekNumber: Int = 1,
    val implicatedFranchiseName: String? = null,
    val isResolved: Boolean = false,
    val penaltyApplied: Boolean = false,
    val resolutionSummary: String? = null
)

data class LeagueScandalResolutionOutcome(
    val success: Boolean,
    val message: String,
    val updatedSponsorsState: LeagueSponsorsState,
    val cashDeltaMillions: Long = 0L,
    val integrityDelta: Int = 0,
    val ownerApprovalDelta: Int = 0,
    val fanSentimentDelta: Int = 0,
    val penaltyApplied: Boolean = false,
    val pulledOutSponsors: List<String> = emptyList(),
    val headline: String
)

enum class PrDamageActionType(
    val displayName: String,
    val shortBadge: String,
    val baseFanSentimentGain: Int,
    val ownerApprovalDelta: Int,
    val cashCostMillions: Long,
    val stadiumRevenuePenaltyPct: Int,
    val clearsSponsorMoralityWarnings: Boolean,
    val requiresSentimentBelow: Int? = null,
    val oncePerSeason: Boolean = false,
    val blocksSponsorRulesYears: Int = 0,
    val cooldownTurns: Int = 1,
    val description: String
) {
    CAP_TICKET_CONCESSION_PRICES(
        displayName = "Cap Ticket & Concession Prices",
        shortBadge = "+14 Sentiment • -10 Owners • -20% Stadium Rev",
        baseFanSentimentGain = 14,
        ownerApprovalDelta = -10,
        cashCostMillions = 0L,
        stadiumRevenuePenaltyPct = 20,
        clearsSponsorMoralityWarnings = false,
        cooldownTurns = 1,
        description = "Mandates league-wide caps on arena tickets, parking, and concessions for the rest of this season."
    ),
    FREE_BROADCAST_WEEKEND(
        displayName = "Free Broadcast Weekend",
        shortBadge = "+8 Sentiment • -\$5M League Cash",
        baseFanSentimentGain = 8,
        ownerApprovalDelta = 0,
        cashCostMillions = 5L,
        stadiumRevenuePenaltyPct = 0,
        clearsSponsorMoralityWarnings = false,
        cooldownTurns = 1,
        description = "Underwrites national unencrypted streaming & broadcast access for every matchup this weekend."
    ),
    PUBLICLY_BLAME_OWNERS(
        displayName = "Publicly Blame Owners",
        shortBadge = "+7 Sentiment • -15 Owners",
        baseFanSentimentGain = 7,
        ownerApprovalDelta = -15,
        cashCostMillions = 0L,
        stadiumRevenuePenaltyPct = 0,
        clearsSponsorMoralityWarnings = false,
        cooldownTurns = 1,
        description = "Holds a fiery press briefing siding with supporters against ownership greed and front-office missteps."
    ),
    CHARITY_AND_YOUTH_TOUR(
        displayName = "Charity & Youth Tour",
        shortBadge = "+4 Sentiment • -\$2M Cash • Clears Morality Warnings",
        baseFanSentimentGain = 4,
        ownerApprovalDelta = 0,
        cashCostMillions = 2L,
        stadiumRevenuePenaltyPct = 0,
        clearsSponsorMoralityWarnings = true,
        cooldownTurns = 1,
        description = "Funds a grassroots community & youth clinics tour that restores sponsor goodwill and clears morality warnings."
    ),
    EMERGENCY_APOLOGY_PRESS_CONFERENCE(
        displayName = "Emergency Apology Press Conference",
        shortBadge = "+8 Sentiment • 1/Season (<25) • Blocks Sponsor Rules 1Y",
        baseFanSentimentGain = 8,
        ownerApprovalDelta = 0,
        cashCostMillions = 0L,
        stadiumRevenuePenaltyPct = 0,
        clearsSponsorMoralityWarnings = false,
        requiresSentimentBelow = 25,
        oncePerSeason = true,
        blocksSponsorRulesYears = 1,
        cooldownTurns = 1,
        description = "Available once per season when Fan Sentiment < 25. Delivers a prime-time commissioner apology, freezing new sponsor rules for 1 year."
    )
}

data class PrActionTracking(
    val actionType: PrDamageActionType,
    val cooldownTurnsRemaining: Int = 0,
    val diminishingReturnsTurnsRemaining: Int = 0,
    val usesWithinWindow: Int = 0,
    val lastUsedTurn: Int? = null,
    val totalUsesAllTime: Int = 0
)

data class PrActionLogEntry(
    val id: String,
    val actionType: PrDamageActionType,
    val seasonYear: Int,
    val weekNumber: Int,
    val turnNumber: Int,
    val sentimentGainApplied: Int,
    val wasDiminished: Boolean,
    val summary: String
)

data class PrDamageControlState(
    val totalTurnsElapsed: Int = 1,
    val actionTrackers: Map<PrDamageActionType, PrActionTracking> = defaultTrackers(),
    val ticketConcessionCapActiveThisSeason: Boolean = false,
    val emergencyApologyUsedThisSeason: Boolean = false,
    val sponsorRulesBlockedUntilYear: Int = 0,
    val actionHistory: List<PrActionLogEntry> = emptyList(),
    val lastActionNote: String = "PR Damage Control ready — intervene quickly when scandals threaten Fan Sentiment or sponsors."
) {
    fun trackerFor(type: PrDamageActionType): PrActionTracking =
        actionTrackers[type] ?: PrActionTracking(actionType = type)

    fun isSponsorRulePassingBlocked(currentYear: Int): Boolean =
        sponsorRulesBlockedUntilYear > currentYear

    fun previewSentimentGain(type: PrDamageActionType): Pair<Int, Boolean> {
        val tracker = trackerFor(type)
        val inWindow = tracker.diminishingReturnsTurnsRemaining > 0 && tracker.usesWithinWindow >= 1
        if (!inWindow) return type.baseFanSentimentGain to false
        val multiplier = if (tracker.usesWithinWindow == 1) 0.5 else 0.25
        val reduced = (type.baseFanSentimentGain * multiplier).roundToInt().coerceAtLeast(1)
        return reduced to true
    }

    fun availabilityError(
        type: PrDamageActionType,
        currentFanSentiment: Int,
        cashReservesMillions: Long
    ): String? {
        val tracker = trackerFor(type)
        if (type.oncePerSeason && emergencyApologyUsedThisSeason) {
            return "Already used once this season"
        }
        val reqBelow = type.requiresSentimentBelow
        if (reqBelow != null && currentFanSentiment >= reqBelow) {
            return "Requires Fan Sentiment < $reqBelow (currently $currentFanSentiment%)"
        }
        if (tracker.cooldownTurnsRemaining > 0) {
            return "Cooldown: ${tracker.cooldownTurnsRemaining} turn(s) remaining"
        }
        if (type.cashCostMillions > 0L && cashReservesMillions < type.cashCostMillions) {
            return "Requires \$${type.cashCostMillions}M League Cash"
        }
        return null
    }

    fun advanceTurn(): PrDamageControlState {
        val nextTrackers = PrDamageActionType.entries.associateWith { type ->
            val t = trackerFor(type)
            val nextCooldown = (t.cooldownTurnsRemaining - 1).coerceAtLeast(0)
            val nextDimWindow = (t.diminishingReturnsTurnsRemaining - 1).coerceAtLeast(0)
            t.copy(
                cooldownTurnsRemaining = nextCooldown,
                diminishingReturnsTurnsRemaining = nextDimWindow,
                usesWithinWindow = if (nextDimWindow == 0) 0 else t.usesWithinWindow
            )
        }
        return copy(
            totalTurnsElapsed = totalTurnsElapsed + 1,
            actionTrackers = nextTrackers
        )
    }

    fun advanceOffseason(nextYear: Int): PrDamageControlState {
        val turned = advanceTurn()
        return turned.copy(
            ticketConcessionCapActiveThisSeason = false,
            emergencyApologyUsedThisSeason = false,
            sponsorRulesBlockedUntilYear = if (turned.sponsorRulesBlockedUntilYear <= nextYear) 0 else turned.sponsorRulesBlockedUntilYear
        )
    }

    companion object {
        const val DIMINISHING_WINDOW_TURNS = 3

        fun defaultTrackers(): Map<PrDamageActionType, PrActionTracking> =
            PrDamageActionType.entries.associateWith { PrActionTracking(actionType = it) }
    }
}

data class PrDamageActionOutcome(
    val success: Boolean,
    val message: String,
    val updatedPrState: PrDamageControlState,
    val updatedSponsorsState: LeagueSponsorsState,
    val sentimentGainApplied: Int = 0,
    val wasDiminished: Boolean = false,
    val ownerApprovalDelta: Int = 0,
    val cashDeltaMillions: Long = 0L,
    val clearedMoralityWarningsCount: Int = 0,
    val headline: String = message
)

object LeagueScandalEngine {

    private data class ScandalTemplate(
        val title: String,
        val summaryTemplate: String,
        val severity: LeagueScandalSeverity
    )

    private val scandalTemplates = listOf(
        ScandalTemplate(
            title = "League Scandal: Executive Expense & Cap-Skirting Leak",
            summaryTemplate = "Leaked internal audits allege off-book marketing perks tied to %TEAM%, rattling partner compliance teams.",
            severity = LeagueScandalSeverity.MAJOR
        ),
        ScandalTemplate(
            title = "League Scandal: Broadcast Hot-Mic & Morality Clause Fallout",
            summaryTemplate = "Viral sidelines audio from %TEAM% triggered overnight sponsor backlash and advertiser morality clause reviews.",
            severity = LeagueScandalSeverity.MODERATE
        ),
        ScandalTemplate(
            title = "League Scandal: Insider Odds & Referee Line Whisper",
            summaryTemplate = "Unusual betting movement around %TEAM%'s matchup sparked federal integrity questions threatening official partners.",
            severity = LeagueScandalSeverity.CRITICAL
        ),
        ScandalTemplate(
            title = "League Scandal: Ownership Slush Fund Investigation",
            summaryTemplate = "Investigative journalists linked %TEAM% commercial shell invoices to unapproved partner rebates.",
            severity = LeagueScandalSeverity.MAJOR
        ),
        ScandalTemplate(
            title = "League Scandal: Performance Clinic Supply Chain Leak",
            summaryTemplate = "Whistleblowers exposed unvetted supplement shipments to %TEAM%, triggering sponsor brand-safety alarms.",
            severity = LeagueScandalSeverity.MAJOR
        ),
        ScandalTemplate(
            title = "League Scandal: VIP Hospitality & Foundation Misallocation",
            summaryTemplate = "Audit watchdogs questioned %TEAM% arena charity receipts, prompting sponsors to issue morality warnings.",
            severity = LeagueScandalSeverity.MODERATE
        )
    )

    fun maybeGenerateRandomLeagueScandal(
        sponsorsState: LeagueSponsorsState,
        franchises: List<Franchise>,
        sportType: SportType,
        leagueIntegrityScore: Int,
        seasonYear: Int,
        week: Int,
        random: Random = Random.Default
    ): Pair<LeagueSponsorsState, LeagueScandalIncident?> {
        if (sponsorsState.activeLeagueScandal != null) return sponsorsState to null
        if (week < 2 || franchises.isEmpty()) return sponsorsState to null

        val activeContracts = sponsorsState.allActiveContracts()
        // Only trigger organically during regular season when sponsors exist or integrity dips
        val baseChance = when {
            activeContracts.isEmpty() && leagueIntegrityScore >= 78 -> 8
            activeContracts.any { it.scandalSensitivity == SponsorScandalSensitivity.CRITICAL } -> 32
            activeContracts.any { it.scandalSensitivity == SponsorScandalSensitivity.HIGH } -> 26
            activeContracts.isNotEmpty() -> 18
            else -> 14
        }
        val integrityBonus = ((82 - leagueIntegrityScore).coerceAtLeast(0) / 3)
        val totalChance = (baseChance + integrityBonus).coerceIn(8, 55)
        if (random.nextInt(100) >= totalChance) {
            return sponsorsState to null
        }

        return generateRandomLeagueScandal(
            sponsorsState = sponsorsState,
            franchises = franchises,
            sportType = sportType,
            seasonYear = seasonYear,
            week = week,
            random = random
        )
    }

    fun generateRandomLeagueScandal(
        sponsorsState: LeagueSponsorsState,
        franchises: List<Franchise>,
        sportType: SportType,
        seasonYear: Int,
        week: Int,
        random: Random = Random.Default
    ): Pair<LeagueSponsorsState, LeagueScandalIncident> {
        val targetTeam = franchises.randomOrNull(random)
        val teamName = targetTeam?.teamFullName ?: "${sportType.displayName} Front Office"
        val template = scandalTemplates[random.nextInt(scandalTemplates.size)]
        val summary = template.summaryTemplate.replace("%TEAM%", teamName)

        var updatedState = sponsorsState
        val affectedSlots = mutableListOf<LeagueSponsorSlotType>()
        val affectedNames = mutableListOf<String>()
        var totalStabilityDrop = 0

        LeagueSponsorSlotType.entries.forEach { slot ->
            val contract = updatedState.getSlot(slot) ?: return@forEach
            val sensitivityFactor = when (contract.scandalSensitivity) {
                SponsorScandalSensitivity.LOW -> 0.35
                SponsorScandalSensitivity.MODERATE -> 0.75
                SponsorScandalSensitivity.HIGH -> 1.15
                SponsorScandalSensitivity.CRITICAL -> 1.50
            }
            val drop = (template.severity.baseStabilityDrop * sensitivityFactor).roundToInt().coerceIn(5, 45)
            val nextStability = (contract.stabilityScore - drop).coerceIn(10, 100)
            val warn = contract.scandalSensitivity != SponsorScandalSensitivity.LOW || nextStability < 75
            val updatedContract = contract.copy(
                stabilityScore = nextStability,
                hasMoralityWarning = contract.hasMoralityWarning || warn,
                moralityWarningReason = if (warn) template.title else contract.moralityWarningReason
            )
            updatedState = updatedState.withSlot(slot, updatedContract)
            if (warn || drop >= 8) {
                affectedSlots.add(slot)
                affectedNames.add(contract.name)
            }
            totalStabilityDrop += drop
        }

        val incident = LeagueScandalIncident(
            id = "scandal_y${seasonYear}_w${week}_${random.nextInt(1000, 9999)}",
            title = template.title,
            summary = summary,
            severity = template.severity,
            affectedSlots = affectedSlots,
            affectedSponsorNames = affectedNames,
            stabilityDropApplied = totalStabilityDrop,
            weeksUntilPenalty = 2,
            seasonYear = seasonYear,
            weekNumber = week,
            implicatedFranchiseName = teamName,
            isResolved = false,
            penaltyApplied = false
        )

        // High-sensitivity sponsors also enqueue ultimatum if applicable
        updatedState = LeagueSponsorsEngine.evaluateScandalForUltimatum(
            currentState = updatedState,
            scandalSourceTitle = incident.title,
            scandalSummary = incident.summary,
            seasonYear = seasonYear,
            weekNumber = week,
            relatedFranchise = targetTeam,
            forceTriggerOnAnyIncident = true
        ).copy(
            activeLeagueScandal = incident,
            scandalHistory = (listOf(incident) + updatedState.scandalHistory).take(20),
            lastSummary = if (affectedNames.isNotEmpty()) {
                "LEAGUE SCANDAL: ${incident.title} rattled ${affectedNames.joinToString(", ")} — intervene within ${incident.weeksUntilPenalty}w to avoid sponsor penalties!"
            } else {
                "LEAGUE SCANDAL: ${incident.title} — commissioner intervention needed within ${incident.weeksUntilPenalty}w!"
            }
        )

        return updatedState to incident
    }

    fun resolveLeagueScandalIncident(
        sponsorsState: LeagueSponsorsState,
        choice: LeagueScandalInterventionChoice,
        cashReservesMillions: Long,
        seasonYear: Int,
        weekNumber: Int
    ): LeagueScandalResolutionOutcome {
        val incident = sponsorsState.activeLeagueScandal
            ?: return LeagueScandalResolutionOutcome(
                success = false,
                message = "No active league scandal incident requires intervention.",
                updatedSponsorsState = sponsorsState,
                headline = "No active league scandal."
            )

        if (choice != LeagueScandalInterventionChoice.STONEWALL_IGNORE &&
            choice.cashCostMillions > 0L &&
            cashReservesMillions < choice.cashCostMillions
        ) {
            return LeagueScandalResolutionOutcome(
                success = false,
                message = "Insufficient League Cash (\$${choice.cashCostMillions}M needed for ${choice.title}).",
                updatedSponsorsState = sponsorsState,
                headline = "Insufficient funds for scandal intervention."
            )
        }

        if (choice == LeagueScandalInterventionChoice.STONEWALL_IGNORE) {
            return applyUnaddressedScandalPenalty(
                sponsorsState = sponsorsState,
                reasonLabel = "Commissioner declined to intervene",
                seasonYear = seasonYear,
                weekNumber = weekNumber
            )
        }

        var updatedState = sponsorsState
        LeagueSponsorSlotType.entries.forEach { slot ->
            val contract = updatedState.getSlot(slot) ?: return@forEach
            val nextStability = (contract.stabilityScore + choice.stabilityRestore).coerceIn(15, 100)
            updatedState = updatedState.withSlot(
                slot,
                contract.copy(
                    stabilityScore = nextStability,
                    hasMoralityWarning = false,
                    moralityWarningReason = null
                )
            )
        }
        val resolvedIncident = incident.copy(
            isResolved = true,
            penaltyApplied = false,
            resolutionSummary = "Contained via ${choice.title} (Y$seasonYear W$weekNumber)."
        )
        val nextHistory = updatedState.scandalHistory.map {
            if (it.id == resolvedIncident.id) resolvedIncident else it
        }
        val headline = "SCANDAL CONTAINED: ${choice.title} stabilized sponsors and cleared morality warnings for ${incident.title}."
        val finalState = updatedState.copy(
            activeLeagueScandal = null,
            scandalHistory = nextHistory,
            lastSummary = headline
        )
        return LeagueScandalResolutionOutcome(
            success = true,
            message = "Containment succeeded! Sponsor stability restored (+${choice.stabilityRestore}) and payout penalties avoided.",
            updatedSponsorsState = finalState,
            cashDeltaMillions = -choice.cashCostMillions,
            integrityDelta = choice.integrityDelta,
            ownerApprovalDelta = choice.ownerApprovalDelta,
            fanSentimentDelta = choice.fanSentimentDelta,
            penaltyApplied = false,
            headline = headline
        )
    }

    fun tickScandalCountdownOnAdvanceWeek(
        sponsorsState: LeagueSponsorsState,
        seasonYear: Int,
        weekNumber: Int
    ): LeagueScandalResolutionOutcome? {
        val incident = sponsorsState.activeLeagueScandal ?: return null
        val remaining = incident.weeksUntilPenalty - 1
        if (remaining > 0) {
            val ticked = incident.copy(weeksUntilPenalty = remaining)
            val nextHistory = sponsorsState.scandalHistory.map {
                if (it.id == ticked.id) ticked else it
            }
            val updated = sponsorsState.copy(
                activeLeagueScandal = ticked,
                scandalHistory = nextHistory,
                lastSummary = "SCANDAL DEADLINE WARNING: '${ticked.title}' enters final $remaining week(s) before sponsor payout penalties!"
            )
            return LeagueScandalResolutionOutcome(
                success = true,
                message = "Scandal intervention deadline ticking down ($remaining week left).",
                updatedSponsorsState = updated,
                penaltyApplied = false,
                headline = updated.lastSummary
            )
        }
        return applyUnaddressedScandalPenalty(
            sponsorsState = sponsorsState,
            reasonLabel = "Intervention window expired",
            seasonYear = seasonYear,
            weekNumber = weekNumber
        )
    }

    fun applyUnaddressedScandalPenalty(
        sponsorsState: LeagueSponsorsState,
        reasonLabel: String,
        seasonYear: Int,
        weekNumber: Int
    ): LeagueScandalResolutionOutcome {
        val incident = sponsorsState.activeLeagueScandal
            ?: return LeagueScandalResolutionOutcome(
                success = false,
                message = "No active league scandal.",
                updatedSponsorsState = sponsorsState,
                headline = "No active league scandal."
            )

        var updatedState = sponsorsState
        val pulledOut = mutableListOf<String>()
        val penalizedNames = mutableListOf<String>()
        val penaltyPct = incident.severity.unaddressedPayoutPenaltyPct

        LeagueSponsorSlotType.entries.forEach { slot ->
            val contract = updatedState.getSlot(slot) ?: return@forEach
            val wasAffected = slot in incident.affectedSlots || contract.hasMoralityWarning || contract.stabilityScore < 85
            if (wasAffected) {
                val nextStability = (contract.stabilityScore - 18).coerceIn(0, 100)
                if (nextStability < 30 && contract.scandalSensitivity.isHigh) {
                    pulledOut.add("${contract.name} (${slot.displayName})")
                    updatedState = updatedState.withSlot(slot, null)
                } else {
                    val reducedPayout = ((contract.payout * (100 - penaltyPct)) / 100L).coerceAtLeast(1L)
                    penalizedNames.add("${contract.name} (\$${contract.payout}M->\$${reducedPayout}M)")
                    updatedState = updatedState.withSlot(
                        slot,
                        contract.copy(
                            payout = reducedPayout,
                            stabilityScore = nextStability,
                            hasMoralityWarning = true,
                            moralityWarningReason = "Penalized after unaddressed ${incident.title}"
                        )
                    )
                }
            }
        }

        val resolvedIncident = incident.copy(
            isResolved = true,
            penaltyApplied = true,
            weeksUntilPenalty = 0,
            resolutionSummary = "$reasonLabel (Y$seasonYear W$weekNumber): sponsor penalties enforced."
        )
        val nextHistory = updatedState.scandalHistory.map {
            if (it.id == resolvedIncident.id) resolvedIncident else it
        }
        val penaltyDetail = buildString {
            if (penalizedNames.isNotEmpty()) append("Payout cuts: ${penalizedNames.joinToString(", ")}. ")
            if (pulledOut.isNotEmpty()) append("Terminated contracts: ${pulledOut.joinToString(", ")}.")
            if (isEmpty()) append("League credibility & Integrity took a sharp hit.")
        }.trim()
        val headline = "SCANDAL PENALTY ($reasonLabel): ${incident.title} — $penaltyDetail"

        return LeagueScandalResolutionOutcome(
            success = true,
            message = headline,
            updatedSponsorsState = updatedState.copy(
                activeLeagueScandal = null,
                scandalHistory = nextHistory,
                lastSummary = headline
            ),
            cashDeltaMillions = 0L,
            integrityDelta = LeagueScandalInterventionChoice.STONEWALL_IGNORE.integrityDelta,
            ownerApprovalDelta = LeagueScandalInterventionChoice.STONEWALL_IGNORE.ownerApprovalDelta,
            fanSentimentDelta = LeagueScandalInterventionChoice.STONEWALL_IGNORE.fanSentimentDelta,
            penaltyApplied = true,
            pulledOutSponsors = pulledOut,
            headline = headline
        )
    }
}

object PrDamageControlEngine {

    fun executeAction(
        prState: PrDamageControlState,
        sponsorsState: LeagueSponsorsState,
        actionType: PrDamageActionType,
        currentFanSentiment: Int,
        cashReservesMillions: Long,
        currentYear: Int,
        currentWeek: Int
    ): PrDamageActionOutcome {
        val blocker = prState.availabilityError(
            type = actionType,
            currentFanSentiment = currentFanSentiment,
            cashReservesMillions = cashReservesMillions
        )
        if (blocker != null) {
            return PrDamageActionOutcome(
                success = false,
                message = "${actionType.displayName} unavailable: $blocker.",
                updatedPrState = prState,
                updatedSponsorsState = sponsorsState
            )
        }

        val (effectiveGain, wasDiminished) = prState.previewSentimentGain(actionType)
        val tracker = prState.trackerFor(actionType)
        val updatedTracker = tracker.copy(
            cooldownTurnsRemaining = actionType.cooldownTurns,
            diminishingReturnsTurnsRemaining = PrDamageControlState.DIMINISHING_WINDOW_TURNS,
            usesWithinWindow = tracker.usesWithinWindow + 1,
            lastUsedTurn = prState.totalTurnsElapsed,
            totalUsesAllTime = tracker.totalUsesAllTime + 1
        )
        val nextTrackers = prState.actionTrackers.toMutableMap().apply {
            put(actionType, updatedTracker)
        }

        var nextSponsors = sponsorsState
        var clearedWarnings = 0
        if (actionType.clearsSponsorMoralityWarnings) {
            LeagueSponsorSlotType.entries.forEach { slot ->
                val c = nextSponsors.getSlot(slot) ?: return@forEach
                if (c.hasMoralityWarning || c.stabilityScore < 90) {
                    if (c.hasMoralityWarning) clearedWarnings++
                    nextSponsors = nextSponsors.withSlot(
                        slot,
                        c.copy(
                            hasMoralityWarning = false,
                            moralityWarningReason = null,
                            stabilityScore = (c.stabilityScore + 20).coerceIn(75, 100)
                        )
                    )
                }
            }
            if (nextSponsors.activeUltimatum != null || nextSponsors.pendingUltimatumQueue.isNotEmpty()) {
                clearedWarnings += 1 + nextSponsors.pendingUltimatumQueue.size
                nextSponsors = nextSponsors.copy(
                    activeUltimatum = null,
                    pendingUltimatumQueue = emptyList()
                )
            }
            nextSponsors.activeLeagueScandal?.let { activeScandal ->
                val resolvedScandal = activeScandal.copy(
                    isResolved = true,
                    penaltyApplied = false,
                    resolutionSummary = "Cleared via Charity & Youth Tour (Y$currentYear W$currentWeek)."
                )
                clearedWarnings++
                nextSponsors = nextSponsors.copy(
                    activeLeagueScandal = null,
                    scandalHistory = nextSponsors.scandalHistory.map {
                        if (it.id == resolvedScandal.id) resolvedScandal else it
                    }
                )
            }
            nextSponsors = nextSponsors.copy(
                lastSummary = "Charity & Youth Tour cleared sponsor morality warnings and steadied commercial partners."
            )
        }

        val dimSuffix = if (wasDiminished) " (Diminishing Returns: +$effectiveGain instead of +${actionType.baseFanSentimentGain})" else ""
        val extraSummary = buildList {
            if (actionType.ownerApprovalDelta != 0) add("${actionType.ownerApprovalDelta} Owner Approval")
            if (actionType.cashCostMillions > 0L) add("-\$${actionType.cashCostMillions}M League Cash")
            if (actionType.stadiumRevenuePenaltyPct > 0) add("-${actionType.stadiumRevenuePenaltyPct}% Stadium Revenue this season")
            if (actionType.clearsSponsorMoralityWarnings) add("Cleared $clearedWarnings morality warning(s)")
            if (actionType.blocksSponsorRulesYears > 0) add("Blocked new sponsor rules until ${currentYear + actionType.blocksSponsorRulesYears}")
        }.joinToString(" • ")

        val logSummary = "${actionType.displayName}: +$effectiveGain Fan Sentiment$dimSuffix${if (extraSummary.isNotBlank()) " ($extraSummary)" else ""}."
        val logEntry = PrActionLogEntry(
            id = "pr_${actionType.name.lowercase()}_t${prState.totalTurnsElapsed}_${prState.actionHistory.size + 1}",
            actionType = actionType,
            seasonYear = currentYear,
            weekNumber = currentWeek,
            turnNumber = prState.totalTurnsElapsed,
            sentimentGainApplied = effectiveGain,
            wasDiminished = wasDiminished,
            summary = logSummary
        )

        val nextPrState = prState.copy(
            actionTrackers = nextTrackers,
            ticketConcessionCapActiveThisSeason = prState.ticketConcessionCapActiveThisSeason || actionType.stadiumRevenuePenaltyPct > 0,
            emergencyApologyUsedThisSeason = prState.emergencyApologyUsedThisSeason || actionType.oncePerSeason,
            sponsorRulesBlockedUntilYear = if (actionType.blocksSponsorRulesYears > 0) {
                maxOf(prState.sponsorRulesBlockedUntilYear, currentYear + actionType.blocksSponsorRulesYears)
            } else prState.sponsorRulesBlockedUntilYear,
            actionHistory = (listOf(logEntry) + prState.actionHistory).take(25),
            lastActionNote = logSummary
        )

        return PrDamageActionOutcome(
            success = true,
            message = logSummary,
            updatedPrState = nextPrState,
            updatedSponsorsState = nextSponsors,
            sentimentGainApplied = effectiveGain,
            wasDiminished = wasDiminished,
            ownerApprovalDelta = actionType.ownerApprovalDelta,
            cashDeltaMillions = -actionType.cashCostMillions,
            clearedMoralityWarningsCount = clearedWarnings,
            headline = "PR DAMAGE CONTROL — $logSummary"
        )
    }
}
