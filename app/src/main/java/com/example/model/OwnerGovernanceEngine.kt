package com.example.model

import kotlin.math.abs

/**
 * Governance engine modeling each OwnershipGroup's Principal Owner, diverse OwnerArchetype,
 * and Board of Governors voting power on CBA negotiations and league rule changes.
 */
object OwnerGovernanceEngine {

    data class ExpansionOwnerTemplate(
        val fullName: String,
        val roleTitle: String,
        val backgroundBio: String,
        val archetype: OwnerArchetype
    )

    // Charter 6 owners with rich diversity and distinct archetypes
    private val charterOwnersByGroupId: Map<String, FranchiseOwner> = mapOf(
        "own_nyc" to FranchiseOwner(
            id = "owner_nyc",
            fullName = "Elena Vance-Kowalski",
            roleTitle = "Chairwoman & Managing Governor",
            backgroundBio = "Former prime-time sports network executive who champions high-scoring spectacle and marquee star power.",
            archetype = OwnerArchetype.BROADCAST_SHOWMAN
        ),
        "own_la" to FranchiseOwner(
            id = "owner_la",
            fullName = "Marcus Sterling",
            roleTitle = "Managing General Partner",
            backgroundBio = "Institutional buyout strategist focused on hard payroll ceilings, EBITDA stability, and disciplined labor splits.",
            archetype = OwnerArchetype.FISCAL_HAWK
        ),
        "own_chi" to FranchiseOwner(
            id = "owner_chi",
            fullName = "Dr. Aisha Brooks",
            roleTitle = "Trust Board Chairwoman",
            backgroundBio = "Urban economist and civic steward safeguarding revenue sharing, anti-tanking lotteries, and accessible fan tickets.",
            archetype = OwnerArchetype.COMMUNITY_STEWARD
        ),
        "own_atx" to FranchiseOwner(
            id = "owner_atx",
            fullName = "Sanjay Patel",
            roleTitle = "Founder & Principal Governor",
            backgroundBio = "Enterprise telemetry pioneer pushing shot-clock pace, automated officiating replay, and sports-science recovery.",
            archetype = OwnerArchetype.ANALYTICS_VISIONARY
        ),
        "own_las" to FranchiseOwner(
            id = "owner_las",
            fullName = "Camila Mendoza",
            roleTitle = "Principal Owner & Governor",
            backgroundBio = "Live-entertainment entrepreneur who backs player-friendly revenue sharing, charter travel, and wellness accords.",
            archetype = OwnerArchetype.PLAYERS_FIRST_PATRON
        ),
        "own_sea" to FranchiseOwner(
            id = "owner_sea",
            fullName = "Kenji Takahashi",
            roleTitle = "Lead Sovereign Governor",
            backgroundBio = "Pacific trade consortium architect driving continental expansion, deeper active rosters, and luxury-tax parity.",
            archetype = OwnerArchetype.GLOBAL_EXPANSIONIST
        )
    )

    // Diverse pool of governors across all 7 archetypes for expansion and custom franchises
    private val expansionOwnerPool: List<ExpansionOwnerTemplate> = listOf(
        ExpansionOwnerTemplate(
            fullName = "Arthur Beaumont",
            roleTitle = "Senior Managing Governor",
            backgroundBio = "Third-generation sporting club patron defending classic 48-minute regulation, strict conduct codes, and merit playoffs.",
            archetype = OwnerArchetype.TRADITIONALIST_PURIST
        ),
        ExpansionOwnerTemplate(
            fullName = "Nadia Okafor",
            roleTitle = "Chairwoman & Lead Investor",
            backgroundBio = "Fintech founder bringing algorithmic roster valuation, flexible soft caps, and injury-prevention telemetry.",
            archetype = OwnerArchetype.ANALYTICS_VISIONARY
        ),
        ExpansionOwnerTemplate(
            fullName = "Henrik Lindqvist",
            roleTitle = "Syndicate Governor",
            backgroundBio = "Nordic infrastructure investor advocating international conferences, expanded bench limits, and progressive luxury taxes.",
            archetype = OwnerArchetype.GLOBAL_EXPANSIONIST
        ),
        ExpansionOwnerTemplate(
            fullName = "Mei-Ling Chen",
            roleTitle = "Majority Governor",
            backgroundBio = "Global streaming producer focused on play-in elimination drama, fast-break tempo, and global prime-time reach.",
            archetype = OwnerArchetype.BROADCAST_SHOWMAN
        ),
        ExpansionOwnerTemplate(
            fullName = "Tariq Al-Mansoor",
            roleTitle = "Sovereign Board Governor",
            backgroundBio = "Cross-border investment director supporting elite player facilities, wellness charters, and sustainable global growth.",
            archetype = OwnerArchetype.PLAYERS_FIRST_PATRON
        ),
        ExpansionOwnerTemplate(
            fullName = "Claire Moreau",
            roleTitle = "Managing Partner",
            backgroundBio = "European private-capital partner demanding hard payroll caps, rookie wage containment, and tight operating margins.",
            archetype = OwnerArchetype.FISCAL_HAWK
        ),
        ExpansionOwnerTemplate(
            fullName = "Mateo Silva",
            roleTitle = "Civic Coalition Chair",
            backgroundBio = "Public-private arena trust leader protecting small-market revenue sharing, draft lotteries, and clean sport integrity.",
            archetype = OwnerArchetype.COMMUNITY_STEWARD
        ),
        ExpansionOwnerTemplate(
            fullName = "Dr. Hannah Goldstein",
            roleTitle = "Principal Governor",
            backgroundBio = "Sports medicine innovator championing load management, player safety mandates, and constructive CBA compromises.",
            archetype = OwnerArchetype.PLAYERS_FIRST_PATRON
        ),
        ExpansionOwnerTemplate(
            fullName = "Victor Volkov",
            roleTitle = "Executive Governor",
            backgroundBio = "Old-school championship architect backing traditional clock management, fierce physical defense, and anti-gambling shields.",
            archetype = OwnerArchetype.TRADITIONALIST_PURIST
        ),
        ExpansionOwnerTemplate(
            fullName = "Amara Diallo",
            roleTitle = "Co-Founder & Governor",
            backgroundBio = "Continental media strategist connecting emerging metro markets with high-tempo broadcast formats.",
            archetype = OwnerArchetype.GLOBAL_EXPANSIONIST
        ),
        ExpansionOwnerTemplate(
            fullName = "Liam O'Sullivan",
            roleTitle = "Board Governor",
            backgroundBio = "Value-investing fund chair enforcing cost certainty, luxury-tax discipline, and modest union splits.",
            archetype = OwnerArchetype.FISCAL_HAWK
        ),
        ExpansionOwnerTemplate(
            fullName = "Yuna Park",
            roleTitle = "Principal Partner",
            backgroundBio = "Esports and data-engineering executive backing rapid scoring formats, Elam endings, and digital replay.",
            archetype = OwnerArchetype.ANALYTICS_VISIONARY
        ),
        ExpansionOwnerTemplate(
            fullName = "Carlos Montoya",
            roleTitle = "Chairman & Governor",
            backgroundBio = "Latin American civic sports director defending fan-trust pricing, revenue sharing, and draft parity.",
            archetype = OwnerArchetype.COMMUNITY_STEWARD
        ),
        ExpansionOwnerTemplate(
            fullName = "Sienna Mercer",
            roleTitle = "Executive Chairwoman",
            backgroundBio = "Box-office arena promoter chasing blockbuster superstar rosters, open spending, and high-scoring showcases.",
            archetype = OwnerArchetype.BROADCAST_SHOWMAN
        )
    )

    fun defaultArchetypeForOwnershipType(type: OwnershipType, seedKey: String = ""): OwnerArchetype {
        val hash = abs(seedKey.hashCode())
        return when (type) {
            OwnershipType.TECH_BILLIONAIRE ->
                if (hash % 3 == 0) OwnerArchetype.PLAYERS_FIRST_PATRON else OwnerArchetype.ANALYTICS_VISIONARY
            OwnershipType.PRIVATE_EQUITY ->
                if (hash % 3 == 0) OwnerArchetype.TRADITIONALIST_PURIST else OwnerArchetype.FISCAL_HAWK
            OwnershipType.COMMUNITY_TRUST ->
                if (hash % 3 == 0) OwnerArchetype.TRADITIONALIST_PURIST else OwnerArchetype.COMMUNITY_STEWARD
            OwnershipType.MEDIA_MOGUL ->
                if (hash % 3 == 0) OwnerArchetype.PLAYERS_FIRST_PATRON else OwnerArchetype.BROADCAST_SHOWMAN
            OwnershipType.SOVEREIGN_SYNDICATE ->
                if (hash % 3 == 0) OwnerArchetype.PLAYERS_FIRST_PATRON else OwnerArchetype.GLOBAL_EXPANSIONIST
        }
    }

    fun defaultOwnerForGroup(
        groupId: String,
        groupName: String,
        type: OwnershipType
    ): FranchiseOwner {
        charterOwnersByGroupId[groupId]?.let { return it }
        // Deterministic match for legacy saved charter groups like own_team_nyc
        charterOwnersByGroupId.entries.firstOrNull { (key, _) ->
            groupId.endsWith(key.removePrefix("own_"))
        }?.value?.let { return it }

        val targetArch = defaultArchetypeForOwnershipType(type, "$groupId:$groupName")
        val matching = expansionOwnerPool.filter { it.archetype == targetArch }
        val pool = matching.ifEmpty { expansionOwnerPool }
        val idx = abs("$groupId:$groupName".hashCode()) % pool.size
        val tpl = pool[idx]
        return FranchiseOwner(
            id = "owner_$groupId",
            fullName = tpl.fullName,
            roleTitle = tpl.roleTitle,
            backgroundBio = tpl.backgroundBio,
            archetype = tpl.archetype
        )
    }

    fun buildExpansionOwnershipGroup(
        city: MarketCity,
        ownershipType: OwnershipType,
        existingFranchises: List<Franchise>,
        preferredArchetype: OwnerArchetype? = null,
        customOwnerName: String? = null
    ): OwnershipGroup {
        val existingOwnerNames = existingFranchises.map { it.ownership.primaryOwner.fullName.lowercase() }.toSet()
        val archetypeCounts = OwnerArchetype.values().associateWith { arch ->
            existingFranchises.count { it.ownership.primaryOwner.archetype == arch }
        }

        // Prioritize least-represented archetypes across current league so Board stays diverse
        val chosenArchetype = preferredArchetype ?: run {
            val typeAffinities = when (ownershipType) {
                OwnershipType.TECH_BILLIONAIRE -> listOf(OwnerArchetype.ANALYTICS_VISIONARY, OwnerArchetype.PLAYERS_FIRST_PATRON, OwnerArchetype.GLOBAL_EXPANSIONIST)
                OwnershipType.PRIVATE_EQUITY -> listOf(OwnerArchetype.FISCAL_HAWK, OwnerArchetype.TRADITIONALIST_PURIST, OwnerArchetype.ANALYTICS_VISIONARY)
                OwnershipType.COMMUNITY_TRUST -> listOf(OwnerArchetype.COMMUNITY_STEWARD, OwnerArchetype.TRADITIONALIST_PURIST, OwnerArchetype.PLAYERS_FIRST_PATRON)
                OwnershipType.MEDIA_MOGUL -> listOf(OwnerArchetype.BROADCAST_SHOWMAN, OwnerArchetype.PLAYERS_FIRST_PATRON, OwnerArchetype.GLOBAL_EXPANSIONIST)
                OwnershipType.SOVEREIGN_SYNDICATE -> listOf(OwnerArchetype.GLOBAL_EXPANSIONIST, OwnerArchetype.TRADITIONALIST_PURIST, OwnerArchetype.BROADCAST_SHOWMAN)
            }
            // Prefer least-represented overall if TRADITIONALIST_PURIST hasn't appeared yet, or pick rarest affinity
            val zeroRep = OwnerArchetype.values().filter { (archetypeCounts[it] ?: 0) == 0 }
            zeroRep.firstOrNull { it in typeAffinities }
                ?: zeroRep.firstOrNull()
                ?: typeAffinities.minByOrNull { archetypeCounts[it] ?: 0 }
                ?: defaultArchetypeForOwnershipType(ownershipType, city.id)
        }

        val matchingTemplates = expansionOwnerPool.filter {
            it.archetype == chosenArchetype && it.fullName.lowercase() !in existingOwnerNames
        }.ifEmpty {
            expansionOwnerPool.filter { it.fullName.lowercase() !in existingOwnerNames }
        }.ifEmpty {
            expansionOwnerPool
        }
        val selectedTemplate = matchingTemplates[abs(city.id.hashCode()) % matchingTemplates.size]
        val finalName = customOwnerName?.trim()?.takeIf { it.isNotBlank() } ?: selectedTemplate.fullName
        val groupId = "own_exp_${city.id}"

        val owner = FranchiseOwner(
            id = "owner_exp_${city.id}",
            fullName = finalName,
            roleTitle = selectedTemplate.roleTitle,
            backgroundBio = if (selectedTemplate.archetype == chosenArchetype) {
                selectedTemplate.backgroundBio
            } else {
                "${chosenArchetype.title} governor representing ${city.name}. ${chosenArchetype.manifesto}"
            },
            archetype = chosenArchetype
        )

        val netWorth = when (ownershipType) {
            OwnershipType.SOVEREIGN_SYNDICATE -> 18.5
            OwnershipType.TECH_BILLIONAIRE -> 12.4
            OwnershipType.MEDIA_MOGUL -> 8.8
            OwnershipType.PRIVATE_EQUITY -> 6.9
            OwnershipType.COMMUNITY_TRUST -> 2.8
        }

        return OwnershipGroup(
            id = groupId,
            name = "${city.name} Athletic Holdings",
            type = ownershipType,
            netWorthBillions = netWorth,
            ownerApproval = 90,
            primaryOwner = owner
        )
    }

    /**
     * Evaluate a CBA proposal across every franchise's Principal Owner and archetype.
     */
    fun evaluateCbaVote(
        franchises: List<Franchise>,
        proposedAgreement: LaborUnionAgreement,
        year: Int,
        week: Int,
        enactedViaCommissionerOverride: Boolean = false
    ): BoardVoteResult {
        val avgPayroll = if (franchises.isNotEmpty()) franchises.map { it.annualPayrollMillions }.average() else 75.0
        val ballots = franchises.map { franchise ->
            val owner = franchise.ownership.primaryOwner
            val arch = owner.archetype
            var score = 10 // Baseline constructive bias toward avoiding catastrophic lockouts

            // 1. Revenue split vs owner's ideal split
            val revDelta = proposedAgreement.playerRevenueSharePct - arch.idealRevenueSharePct
            score -= revDelta * 12

            // 2. Salary Cap Type alignment
            score += when (proposedAgreement.salaryCapType) {
                SalaryCapType.HARD_CAP -> when (arch) {
                    OwnerArchetype.FISCAL_HAWK -> 32
                    OwnerArchetype.COMMUNITY_STEWARD -> 24
                    OwnerArchetype.TRADITIONALIST_PURIST -> 14
                    OwnerArchetype.GLOBAL_EXPANSIONIST -> 8
                    OwnerArchetype.ANALYTICS_VISIONARY -> 4
                    OwnerArchetype.PLAYERS_FIRST_PATRON -> -14
                    OwnerArchetype.BROADCAST_SHOWMAN -> -24
                }
                SalaryCapType.SOFT_CAP_LUXURY_TAX -> when (arch) {
                    OwnerArchetype.ANALYTICS_VISIONARY -> 20
                    OwnerArchetype.GLOBAL_EXPANSIONIST -> 22
                    OwnerArchetype.TRADITIONALIST_PURIST -> 18
                    OwnerArchetype.COMMUNITY_STEWARD -> 14
                    OwnerArchetype.PLAYERS_FIRST_PATRON -> 16
                    OwnerArchetype.FISCAL_HAWK -> 4
                    OwnerArchetype.BROADCAST_SHOWMAN -> 10
                }
                SalaryCapType.FREE_MARKET -> when (arch) {
                    OwnerArchetype.BROADCAST_SHOWMAN -> 28
                    OwnerArchetype.PLAYERS_FIRST_PATRON -> 18
                    OwnerArchetype.ANALYTICS_VISIONARY -> -6
                    OwnerArchetype.GLOBAL_EXPANSIONIST -> -12
                    OwnerArchetype.TRADITIONALIST_PURIST -> -24
                    OwnerArchetype.COMMUNITY_STEWARD -> -36
                    OwnerArchetype.FISCAL_HAWK -> -42
                }
            }

            // 3. Salary Cap ceiling vs franchise market size & fiscal stance
            val capDiff = proposedAgreement.salaryCapMillions - 85L
            if (proposedAgreement.salaryCapType != SalaryCapType.FREE_MARKET) {
                if (capDiff > 25) {
                    score += arch.playerWelfareBias * 5 - (if (arch == OwnerArchetype.FISCAL_HAWK) 14 else 0)
                } else if (capDiff < -10) {
                    score += (if (arch == OwnerArchetype.FISCAL_HAWK || arch == OwnerArchetype.COMMUNITY_STEWARD) 10 else -6)
                }
            }

            // 4. Player perks, travel & safety costs
            val costlyPerksCount = listOf(
                proposedAgreement.mandatoryPrivateCharterFlights,
                proposedAgreement.fiveStarHotelSingleRooms,
                proposedAgreement.advancedHybridTurfMandate,
                proposedAgreement.maxBackToBackGamesLimit,
                proposedAgreement.independentConcussionProtocol
            ).count { it }
            val extraWelfareCost = ((proposedAgreement.playerHealthWelfareMillions - 15L) / 3L).toInt() +
                ((proposedAgreement.postCareerTransitionFundMillions - 5L) / 2L).toInt()

            score += when (arch) {
                OwnerArchetype.PLAYERS_FIRST_PATRON -> costlyPerksCount * 5 + extraWelfareCost * 3
                OwnerArchetype.ANALYTICS_VISIONARY ->
                    (if (proposedAgreement.independentConcussionProtocol) 10 else -6) +
                        (if (proposedAgreement.maxBackToBackGamesLimit) 8 else -4)
                OwnerArchetype.FISCAL_HAWK -> -(costlyPerksCount * 4) - extraWelfareCost * 4
                OwnerArchetype.COMMUNITY_STEWARD ->
                    (if (proposedAgreement.independentConcussionProtocol) 6 else 0) - extraWelfareCost * 2
                OwnerArchetype.TRADITIONALIST_PURIST ->
                    (if (proposedAgreement.maxBackToBackGamesLimit) -4 else 4) +
                        (if (proposedAgreement.independentConcussionProtocol) 6 else 0)
                OwnerArchetype.BROADCAST_SHOWMAN ->
                    (if (proposedAgreement.mandatoryPrivateCharterFlights) 4 else 0) -
                        (if (proposedAgreement.maxBackToBackGamesLimit) 4 else 0)
                OwnerArchetype.GLOBAL_EXPANSIONIST ->
                    (if (proposedAgreement.mandatoryPrivateCharterFlights) 8 else -4) +
                        (if (proposedAgreement.fiveStarHotelSingleRooms) 4 else 0)
            }

            // 5. Roster Rules preference by archetype
            score += when (proposedAgreement.negotiatedRosterRule) {
                RosterSizeRule.COMPACT_12 -> when (arch) {
                    OwnerArchetype.FISCAL_HAWK -> 8
                    OwnerArchetype.TRADITIONALIST_PURIST -> 6
                    OwnerArchetype.COMMUNITY_STEWARD -> 4
                    OwnerArchetype.ANALYTICS_VISIONARY -> -4
                    OwnerArchetype.GLOBAL_EXPANSIONIST -> -6
                    OwnerArchetype.PLAYERS_FIRST_PATRON -> -10
                    OwnerArchetype.BROADCAST_SHOWMAN -> 0
                }
                RosterSizeRule.STANDARD_15 -> 4
                RosterSizeRule.EXPANDED_18 -> when (arch) {
                    OwnerArchetype.PLAYERS_FIRST_PATRON -> 10
                    OwnerArchetype.GLOBAL_EXPANSIONIST -> 8
                    OwnerArchetype.ANALYTICS_VISIONARY -> 6
                    OwnerArchetype.BROADCAST_SHOWMAN -> 4
                    OwnerArchetype.TRADITIONALIST_PURIST -> -4
                    OwnerArchetype.COMMUNITY_STEWARD -> -2
                    OwnerArchetype.FISCAL_HAWK -> -10
                }
            }

            // 6. Franchise financial context modifier (small-market vs big spender)
            if (franchise.annualPayrollMillions > avgPayroll + 4 && proposedAgreement.salaryCapType == SalaryCapType.FREE_MARKET) {
                score += 10
            } else if (franchise.fanBaseMillions < 2.5 && proposedAgreement.salaryCapType == SalaryCapType.FREE_MARKET) {
                score -= 10
            }

            val choice = if (score >= 0) VoteChoice.APPROVE else VoteChoice.OPPOSE
            val rationale = buildCbaRationale(owner, proposedAgreement, choice)

            OwnerBallot(
                franchiseId = franchise.id,
                teamName = franchise.teamFullName,
                ownershipGroupName = franchise.ownership.name,
                owner = owner,
                choice = choice,
                alignmentScore = score.coerceIn(-100, 100),
                rationale = rationale
            )
        }

        val yes = ballots.count { it.choice == VoteChoice.APPROVE }
        val no = ballots.count { it.choice == VoteChoice.OPPOSE }
        val abstain = ballots.count { it.choice == VoteChoice.ABSTAIN }
        val total = ballots.size
        // Strict majority (> 50%) required from the Board of Governors
        val passed = yes * 2 > total

        return BoardVoteResult(
            id = "vote_cba_${year}_${week}_${System.currentTimeMillis() % 10000}",
            proposalTitle = "5-Year Collective Bargaining Agreement",
            proposalSummary = "${proposedAgreement.playerRevenueSharePct}% Player Split • ${proposedAgreement.salaryCapType.displayName} (\$${proposedAgreement.salaryCapMillions}M) • ${proposedAgreement.negotiatedRosterRule.displayName}",
            category = GovernanceProposalCategory.CBA_NEGOTIATION,
            ballots = ballots,
            yesVotes = yes,
            noVotes = no,
            abstainVotes = abstain,
            totalVotes = total,
            passed = passed,
            enactedViaCommissionerOverride = enactedViaCommissionerOverride,
            year = year,
            week = week
        )
    }

    /**
     * Evaluate a LeagueRule enact/repeal proposal across every franchise's Principal Owner.
     */
    fun evaluateRuleToggleVote(
        franchises: List<Franchise>,
        rule: LeagueRule,
        targetEnacted: Boolean,
        year: Int,
        week: Int,
        enactedViaCommissionerOverride: Boolean = false
    ): BoardVoteResult {
        val actionWord = if (targetEnacted) "Enact" else "Repeal"
        val ballots = franchises.map { franchise ->
            val owner = franchise.ownership.primaryOwner
            val arch = owner.archetype
            val score = scoreRuleToggleForArchetype(arch, rule.id, targetEnacted, franchise)
            val choice = if (score >= 0) VoteChoice.APPROVE else VoteChoice.OPPOSE
            val rationale = buildRuleRationale(owner, rule.name, targetEnacted, choice)

            OwnerBallot(
                franchiseId = franchise.id,
                teamName = franchise.teamFullName,
                ownershipGroupName = franchise.ownership.name,
                owner = owner,
                choice = choice,
                alignmentScore = score.coerceIn(-100, 100),
                rationale = rationale
            )
        }

        val yes = ballots.count { it.choice == VoteChoice.APPROVE }
        val no = ballots.count { it.choice == VoteChoice.OPPOSE }
        val abstain = ballots.count { it.choice == VoteChoice.ABSTAIN }
        val total = ballots.size
        val passed = yes * 2 > total

        return BoardVoteResult(
            id = "vote_rule_${rule.id}_${year}_${week}_${System.currentTimeMillis() % 10000}",
            proposalTitle = "$actionWord: ${rule.name}",
            proposalSummary = if (targetEnacted) rule.enactedEffect else rule.repealedEffect,
            category = GovernanceProposalCategory.RULE_CHANGE,
            ballots = ballots,
            yesVotes = yes,
            noVotes = no,
            abstainVotes = abstain,
            totalVotes = total,
            passed = passed,
            enactedViaCommissionerOverride = enactedViaCommissionerOverride,
            year = year,
            week = week
        )
    }

    fun evaluateSettingRuleVote(
        franchises: List<Franchise>,
        proposalTitle: String,
        proposalSummary: String,
        category: GovernanceProposalCategory = GovernanceProposalCategory.RULE_CHANGE,
        capStrictness: SalaryCapStrictness? = null,
        overtimeFormat: OvertimeFormat? = null,
        rosterSizeRule: RosterSizeRule? = null,
        injuryFrequency: InjuryFrequencyRule? = null,
        gameLength: GameLengthRule? = null,
        year: Int,
        week: Int,
        enactedViaCommissionerOverride: Boolean = false
    ): BoardVoteResult {
        val ballots = franchises.map { franchise ->
            val owner = franchise.ownership.primaryOwner
            val arch = owner.archetype
            val score = when {
                capStrictness != null -> scoreCapStrictnessForArchetype(arch, capStrictness, franchise)
                overtimeFormat != null -> scoreOvertimeForArchetype(arch, overtimeFormat)
                rosterSizeRule != null -> scoreRosterSizeForArchetype(arch, rosterSizeRule)
                injuryFrequency != null -> scoreInjuryRuleForArchetype(arch, injuryFrequency)
                gameLength != null -> scoreGameLengthForArchetype(arch, gameLength)
                else -> 15
            }
            val choice = if (score >= 0) VoteChoice.APPROVE else VoteChoice.OPPOSE
            val rationale = buildSettingRationale(owner, proposalTitle, choice)
            OwnerBallot(
                franchiseId = franchise.id,
                teamName = franchise.teamFullName,
                ownershipGroupName = franchise.ownership.name,
                owner = owner,
                choice = choice,
                alignmentScore = score.coerceIn(-100, 100),
                rationale = rationale
            )
        }

        val yes = ballots.count { it.choice == VoteChoice.APPROVE }
        val no = ballots.count { it.choice == VoteChoice.OPPOSE }
        val abstain = ballots.count { it.choice == VoteChoice.ABSTAIN }
        val total = ballots.size
        val passed = yes * 2 > total

        return BoardVoteResult(
            id = "vote_setting_${year}_${week}_${System.currentTimeMillis() % 10000}",
            proposalTitle = proposalTitle,
            proposalSummary = proposalSummary,
            category = category,
            ballots = ballots,
            yesVotes = yes,
            noVotes = no,
            abstainVotes = abstain,
            totalVotes = total,
            passed = passed,
            enactedViaCommissionerOverride = enactedViaCommissionerOverride,
            year = year,
            week = week
        )
    }

    fun applyApprovalReactions(
        franchises: List<Franchise>,
        voteResult: BoardVoteResult
    ): List<Franchise> {
        val ballotByTeam = voteResult.ballots.associateBy { it.franchiseId }
        return franchises.map { franchise ->
            val ballot = ballotByTeam[franchise.id] ?: return@map franchise
            val delta = when {
                voteResult.enactedViaCommissionerOverride && ballot.choice == VoteChoice.OPPOSE -> -10
                voteResult.enactedViaCommissionerOverride && ballot.choice == VoteChoice.APPROVE -> +3
                voteResult.passed && ballot.choice == VoteChoice.APPROVE -> +3
                voteResult.passed && ballot.choice == VoteChoice.OPPOSE -> -3
                !voteResult.passed && ballot.choice == VoteChoice.OPPOSE -> +2
                !voteResult.passed && ballot.choice == VoteChoice.APPROVE -> -2
                else -> 0
            }
            val nextApproval = (franchise.ownership.ownerApproval + delta).coerceIn(15, 100)
            franchise.copy(ownership = franchise.ownership.copy(ownerApproval = nextApproval))
        }
    }

    private fun scoreRuleToggleForArchetype(
        arch: OwnerArchetype,
        ruleId: String,
        enacting: Boolean,
        franchise: Franchise
    ): Int {
        val sign = if (enacting) 1 else -1
        val baseEnactScore = when (ruleId) {
            "rule_salary_cap" -> when (arch) {
                OwnerArchetype.FISCAL_HAWK -> 38
                OwnerArchetype.COMMUNITY_STEWARD -> 34
                OwnerArchetype.TRADITIONALIST_PURIST -> 18
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 16
                OwnerArchetype.ANALYTICS_VISIONARY -> 14
                OwnerArchetype.PLAYERS_FIRST_PATRON -> -10
                OwnerArchetype.BROADCAST_SHOWMAN -> -24
            } + (if (franchise.fanBaseMillions < 2.5) 8 else -4)
            "rule_draft_lottery" -> when (arch) {
                OwnerArchetype.COMMUNITY_STEWARD -> 30
                OwnerArchetype.ANALYTICS_VISIONARY -> 22
                OwnerArchetype.BROADCAST_SHOWMAN -> 18
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 12
                OwnerArchetype.FISCAL_HAWK -> 10
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 6
                OwnerArchetype.TRADITIONALIST_PURIST -> -12
            }
            "rule_revenue_sharing" -> when (arch) {
                OwnerArchetype.COMMUNITY_STEWARD -> 40
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 24
                OwnerArchetype.FISCAL_HAWK -> 10
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 12
                OwnerArchetype.ANALYTICS_VISIONARY -> 8
                OwnerArchetype.TRADITIONALIST_PURIST -> 6
                OwnerArchetype.BROADCAST_SHOWMAN -> -18
            } + (if (franchise.fanBaseMillions < 2.5) 10 else -8)
            "rule_rookie_wage_scale" -> when (arch) {
                OwnerArchetype.FISCAL_HAWK -> 38
                OwnerArchetype.ANALYTICS_VISIONARY -> 24
                OwnerArchetype.COMMUNITY_STEWARD -> 20
                OwnerArchetype.TRADITIONALIST_PURIST -> 16
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 12
                OwnerArchetype.BROADCAST_SHOWMAN -> -4
                OwnerArchetype.PLAYERS_FIRST_PATRON -> -24
            }
            "rule_play_in_tournament" -> when (arch) {
                OwnerArchetype.BROADCAST_SHOWMAN -> 36
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 26
                OwnerArchetype.COMMUNITY_STEWARD -> 18
                OwnerArchetype.ANALYTICS_VISIONARY -> 10
                OwnerArchetype.FISCAL_HAWK -> 14
                OwnerArchetype.PLAYERS_FIRST_PATRON -> -8
                OwnerArchetype.TRADITIONALIST_PURIST -> -26
            }
            "rule_shot_clock" -> when (arch) {
                OwnerArchetype.ANALYTICS_VISIONARY -> 32
                OwnerArchetype.BROADCAST_SHOWMAN -> 36
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 20
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 12
                OwnerArchetype.COMMUNITY_STEWARD -> 8
                OwnerArchetype.FISCAL_HAWK -> 6
                OwnerArchetype.TRADITIONALIST_PURIST -> -18
            }
            "rule_instant_replay" -> when (arch) {
                OwnerArchetype.ANALYTICS_VISIONARY -> 34
                OwnerArchetype.TRADITIONALIST_PURIST -> 22
                OwnerArchetype.COMMUNITY_STEWARD -> 20
                OwnerArchetype.FISCAL_HAWK -> 10
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 14
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 8
                OwnerArchetype.BROADCAST_SHOWMAN -> -12
            }
            "rule_freedom_of_movement" -> when (arch) {
                OwnerArchetype.BROADCAST_SHOWMAN -> 34
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 26
                OwnerArchetype.ANALYTICS_VISIONARY -> 20
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 16
                OwnerArchetype.COMMUNITY_STEWARD -> 4
                OwnerArchetype.FISCAL_HAWK -> -4
                OwnerArchetype.TRADITIONALIST_PURIST -> -28
            }
            "rule_anti_doping", "rule_anti_gambling" -> when (arch) {
                OwnerArchetype.TRADITIONALIST_PURIST -> 36
                OwnerArchetype.COMMUNITY_STEWARD -> 34
                OwnerArchetype.FISCAL_HAWK -> 24
                OwnerArchetype.ANALYTICS_VISIONARY -> 20
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 18
                OwnerArchetype.BROADCAST_SHOWMAN -> 6
                OwnerArchetype.PLAYERS_FIRST_PATRON -> -8
            }
            "rule_geographic_realignment" -> when (arch) {
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 40
                OwnerArchetype.ANALYTICS_VISIONARY -> 20
                OwnerArchetype.FISCAL_HAWK -> 18
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 16
                OwnerArchetype.COMMUNITY_STEWARD -> 10
                OwnerArchetype.TRADITIONALIST_PURIST -> -14
                OwnerArchetype.BROADCAST_SHOWMAN -> -6
            }
            "rule_division_auto_playoffs" -> when (arch) {
                OwnerArchetype.TRADITIONALIST_PURIST -> 32
                OwnerArchetype.COMMUNITY_STEWARD -> 24
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 16
                OwnerArchetype.FISCAL_HAWK -> 8
                OwnerArchetype.BROADCAST_SHOWMAN -> -6
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 4
                OwnerArchetype.ANALYTICS_VISIONARY -> -14
            }
            else -> (arch.parityBias + arch.integrityTraditionBias) * 8
        }
        return baseEnactScore * sign
    }

    private fun scoreCapStrictnessForArchetype(
        arch: OwnerArchetype,
        strictness: SalaryCapStrictness,
        franchise: Franchise
    ): Int {
        val base = when (strictness) {
            SalaryCapStrictness.STRICT_HARD_CAP -> when (arch) {
                OwnerArchetype.FISCAL_HAWK -> 40
                OwnerArchetype.COMMUNITY_STEWARD -> 30
                OwnerArchetype.TRADITIONALIST_PURIST -> 16
                OwnerArchetype.ANALYTICS_VISIONARY -> 8
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 6
                OwnerArchetype.PLAYERS_FIRST_PATRON -> -18
                OwnerArchetype.BROADCAST_SHOWMAN -> -26
            }
            SalaryCapStrictness.PROGRESSIVE_LUXURY_TAX -> when (arch) {
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 26
                OwnerArchetype.TRADITIONALIST_PURIST -> 22
                OwnerArchetype.COMMUNITY_STEWARD -> 18
                OwnerArchetype.ANALYTICS_VISIONARY -> 18
                OwnerArchetype.FISCAL_HAWK -> 10
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 8
                OwnerArchetype.BROADCAST_SHOWMAN -> 4
            }
            SalaryCapStrictness.MODERATE_SOFT_CAP -> when (arch) {
                OwnerArchetype.ANALYTICS_VISIONARY -> 24
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 24
                OwnerArchetype.BROADCAST_SHOWMAN -> 18
                OwnerArchetype.GLOBAL_EXPANSIONIST -> 14
                OwnerArchetype.TRADITIONALIST_PURIST -> 6
                OwnerArchetype.COMMUNITY_STEWARD -> -6
                OwnerArchetype.FISCAL_HAWK -> -14
            }
            SalaryCapStrictness.UNRESTRICTED_MARKET -> when (arch) {
                OwnerArchetype.BROADCAST_SHOWMAN -> 36
                OwnerArchetype.PLAYERS_FIRST_PATRON -> 22
                OwnerArchetype.ANALYTICS_VISIONARY -> -8
                OwnerArchetype.GLOBAL_EXPANSIONIST -> -14
                OwnerArchetype.TRADITIONALIST_PURIST -> -26
                OwnerArchetype.COMMUNITY_STEWARD -> -36
                OwnerArchetype.FISCAL_HAWK -> -44
            }
        }
        val marketMod = if (franchise.fanBaseMillions >= 3.5 && strictness == SalaryCapStrictness.UNRESTRICTED_MARKET) 10 else 0
        return base + marketMod
    }

    private fun scoreOvertimeForArchetype(arch: OwnerArchetype, format: OvertimeFormat): Int = when (format) {
        OvertimeFormat.STANDARD_PERIOD -> when (arch) {
            OwnerArchetype.TRADITIONALIST_PURIST -> 36
            OwnerArchetype.COMMUNITY_STEWARD -> 16
            OwnerArchetype.FISCAL_HAWK -> 10
            OwnerArchetype.PLAYERS_FIRST_PATRON -> 8
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 6
            OwnerArchetype.ANALYTICS_VISIONARY -> 4
            OwnerArchetype.BROADCAST_SHOWMAN -> -10
        }
        OvertimeFormat.TARGET_SCORE_ELAM -> when (arch) {
            OwnerArchetype.ANALYTICS_VISIONARY -> 34
            OwnerArchetype.BROADCAST_SHOWMAN -> 34
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 20
            OwnerArchetype.PLAYERS_FIRST_PATRON -> 16
            OwnerArchetype.COMMUNITY_STEWARD -> 8
            OwnerArchetype.FISCAL_HAWK -> 6
            OwnerArchetype.TRADITIONALIST_PURIST -> -28
        }
        OvertimeFormat.SUDDEN_DEATH -> when (arch) {
            OwnerArchetype.BROADCAST_SHOWMAN -> 30
            OwnerArchetype.FISCAL_HAWK -> 12
            OwnerArchetype.ANALYTICS_VISIONARY -> 10
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 8
            OwnerArchetype.COMMUNITY_STEWARD -> -6
            OwnerArchetype.PLAYERS_FIRST_PATRON -> -10
            OwnerArchetype.TRADITIONALIST_PURIST -> -24
        }
        OvertimeFormat.PENALTY_SHOOTOUT -> when (arch) {
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 28
            OwnerArchetype.BROADCAST_SHOWMAN -> 26
            OwnerArchetype.PLAYERS_FIRST_PATRON -> 14
            OwnerArchetype.ANALYTICS_VISIONARY -> 8
            OwnerArchetype.COMMUNITY_STEWARD -> 4
            OwnerArchetype.FISCAL_HAWK -> 6
            OwnerArchetype.TRADITIONALIST_PURIST -> -22
        }
    }

    private fun scoreRosterSizeForArchetype(arch: OwnerArchetype, rule: RosterSizeRule): Int = when (rule) {
        RosterSizeRule.COMPACT_12 -> when (arch) {
            OwnerArchetype.FISCAL_HAWK -> 34
            OwnerArchetype.BROADCAST_SHOWMAN -> 12
            OwnerArchetype.TRADITIONALIST_PURIST -> 8
            OwnerArchetype.ANALYTICS_VISIONARY -> -8
            OwnerArchetype.COMMUNITY_STEWARD -> -10
            OwnerArchetype.GLOBAL_EXPANSIONIST -> -22
            OwnerArchetype.PLAYERS_FIRST_PATRON -> -28
        }
        RosterSizeRule.STANDARD_15 -> 16
        RosterSizeRule.EXPANDED_18 -> when (arch) {
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 34
            OwnerArchetype.PLAYERS_FIRST_PATRON -> 30
            OwnerArchetype.ANALYTICS_VISIONARY -> 22
            OwnerArchetype.COMMUNITY_STEWARD -> 14
            OwnerArchetype.BROADCAST_SHOWMAN -> 8
            OwnerArchetype.TRADITIONALIST_PURIST -> -12
            OwnerArchetype.FISCAL_HAWK -> -28
        }
    }

    private fun scoreInjuryRuleForArchetype(arch: OwnerArchetype, rule: InjuryFrequencyRule): Int = when (rule) {
        InjuryFrequencyRule.INJURIES_OFF -> when (arch) {
            OwnerArchetype.BROADCAST_SHOWMAN -> 24
            OwnerArchetype.PLAYERS_FIRST_PATRON -> 22
            OwnerArchetype.ANALYTICS_VISIONARY -> 4
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 6
            OwnerArchetype.COMMUNITY_STEWARD -> -6
            OwnerArchetype.FISCAL_HAWK -> 4
            OwnerArchetype.TRADITIONALIST_PURIST -> -30
        }
        InjuryFrequencyRule.LOW_PROTECTED -> when (arch) {
            OwnerArchetype.PLAYERS_FIRST_PATRON -> 34
            OwnerArchetype.ANALYTICS_VISIONARY -> 24
            OwnerArchetype.BROADCAST_SHOWMAN -> 22
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 16
            OwnerArchetype.COMMUNITY_STEWARD -> 10
            OwnerArchetype.FISCAL_HAWK -> 6
            OwnerArchetype.TRADITIONALIST_PURIST -> -12
        }
        InjuryFrequencyRule.STANDARD_REALISTIC -> 18
        InjuryFrequencyRule.HIGH_GRIND -> when (arch) {
            OwnerArchetype.TRADITIONALIST_PURIST -> 22
            OwnerArchetype.BROADCAST_SHOWMAN -> 8
            OwnerArchetype.FISCAL_HAWK -> -4
            OwnerArchetype.COMMUNITY_STEWARD -> -10
            OwnerArchetype.GLOBAL_EXPANSIONIST -> -12
            OwnerArchetype.ANALYTICS_VISIONARY -> -22
            OwnerArchetype.PLAYERS_FIRST_PATRON -> -34
        }
    }

    private fun scoreGameLengthForArchetype(arch: OwnerArchetype, rule: GameLengthRule): Int = when (rule) {
        GameLengthRule.SHORTENED_36 -> when (arch) {
            OwnerArchetype.BROADCAST_SHOWMAN -> 28
            OwnerArchetype.ANALYTICS_VISIONARY -> 22
            OwnerArchetype.PLAYERS_FIRST_PATRON -> 18
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 12
            OwnerArchetype.FISCAL_HAWK -> 8
            OwnerArchetype.COMMUNITY_STEWARD -> -8
            OwnerArchetype.TRADITIONALIST_PURIST -> -36
        }
        GameLengthRule.STANDARD_48 -> 20
        GameLengthRule.MARATHON_60 -> when (arch) {
            OwnerArchetype.BROADCAST_SHOWMAN -> 18
            OwnerArchetype.GLOBAL_EXPANSIONIST -> 14
            OwnerArchetype.TRADITIONALIST_PURIST -> 12
            OwnerArchetype.FISCAL_HAWK -> 10
            OwnerArchetype.COMMUNITY_STEWARD -> 6
            OwnerArchetype.ANALYTICS_VISIONARY -> -14
            OwnerArchetype.PLAYERS_FIRST_PATRON -> -28
        }
    }

    private fun buildCbaRationale(
        owner: FranchiseOwner,
        cba: LaborUnionAgreement,
        choice: VoteChoice
    ): String {
        val arch = owner.archetype
        return if (choice == VoteChoice.APPROVE) {
            when (arch) {
                OwnerArchetype.FISCAL_HAWK -> "Disciplined ${cba.playerRevenueSharePct}% split and ${cba.salaryCapType.displayName} keep team margins healthy."
                OwnerArchetype.PLAYERS_FIRST_PATRON -> "Player share (${cba.playerRevenueSharePct}%) and welfare/travel commitments secure long-term labor harmony."
                OwnerArchetype.ANALYTICS_VISIONARY -> "Balanced cap structure (\$${cba.salaryCapMillions}M) and recovery safeguards support roster efficiency."
                OwnerArchetype.BROADCAST_SHOWMAN -> "Labor peace guarantees uninterrupted prime-time showcases and marquee star availability."
                OwnerArchetype.TRADITIONALIST_PURIST -> "Constructive 5-year CBA framework preserves competitive order across the league."
                OwnerArchetype.GLOBAL_EXPANSIONIST -> "Stable 5-year labor horizon supports international expansion and franchise asset growth."
                OwnerArchetype.COMMUNITY_STEWARD -> "Predictable payroll ceilings and parity protections shield small-market clubs."
            }
        } else {
            when (arch) {
                OwnerArchetype.FISCAL_HAWK -> "Opposes ${cba.playerRevenueSharePct}% revenue split and overhead concessions eroding franchise EBITDA."
                OwnerArchetype.PLAYERS_FIRST_PATRON -> "Too restrictive on player split (${cba.playerRevenueSharePct}%) or safety/travel guarantees."
                OwnerArchetype.ANALYTICS_VISIONARY -> "Cap inflexibility or missing player-recovery protections undermines roster optimization."
                OwnerArchetype.BROADCAST_SHOWMAN -> "Spending ceilings risk constraining star-studded superteam rosters in flagship markets."
                OwnerArchetype.TRADITIONALIST_PURIST -> "Rejects radical market deregulation that threatens historic franchise parity."
                OwnerArchetype.GLOBAL_EXPANSIONIST -> "Needs stronger travel & global parity safeguards before ratifying a 5-year pact."
                OwnerArchetype.COMMUNITY_STEWARD -> "Unrestricted payroll terms threaten small-market solvency and competitive equity."
            }
        }
    }

    private fun buildRuleRationale(
        owner: FranchiseOwner,
        ruleName: String,
        targetEnacted: Boolean,
        choice: VoteChoice
    ): String {
        val action = if (targetEnacted) "enacting" else "repealing"
        val verb = if (choice == VoteChoice.APPROVE) "Backs" else "Opposes"
        return "$verb $action $ruleName (${owner.archetype.shortBadge} stance)."
    }

    private fun buildSettingRationale(
        owner: FranchiseOwner,
        proposalTitle: String,
        choice: VoteChoice
    ): String {
        val verb = if (choice == VoteChoice.APPROVE) "Supports" else "Votes against"
        return "$verb '$proposalTitle' per ${owner.archetype.title} mandate."
    }

    fun evaluateScheduleApprovalVote(
        franchises: List<Franchise>,
        plan: SeasonSchedulePlan,
        unionAgreement: LaborUnionAgreement,
        year: Int,
        week: Int,
        enactedViaCommissionerOverride: Boolean = false
    ): BoardVoteResult {
        val ballots = franchises.map { franchise ->
            val owner = franchise.ownership.primaryOwner
            val arch = owner.archetype
            val score = if (plan.hasValidationErrors) {
                -80
            } else {
                var base = 18
                base += arch.paceShowmanshipBias * minOf(3, plan.holidayShowcaseCount) * 4
                base += arch.globalGrowthBias * minOf(3, plan.internationalGameCount) * 6
                if (plan.holidayShowcaseCount == 0 && arch == OwnerArchetype.BROADCAST_SHOWMAN) base -= 24
                if (plan.internationalGameCount > 1 && !unionAgreement.mandatoryPrivateCharterFlights) {
                    base += when (arch) {
                        OwnerArchetype.PLAYERS_FIRST_PATRON -> -32
                        OwnerArchetype.COMMUNITY_STEWARD -> -18
                        OwnerArchetype.FISCAL_HAWK -> 6
                        else -> -6
                    }
                }
                if (plan.internationalGameCount >= 3 && arch == OwnerArchetype.COMMUNITY_STEWARD) {
                    base -= 22 // Too many domestic home dates shipped overseas
                }
                if (franchise.ownership.ownerApproval < 45) {
                    base -= 10
                }
                base
            }
            val choice = if (score >= 0) VoteChoice.APPROVE else VoteChoice.OPPOSE
            val rationale = when {
                plan.hasValidationErrors -> "Rejects schedule due to team double-bookings or missing week slates."
                choice == VoteChoice.APPROVE && plan.internationalGameCount > 0 && arch.globalGrowthBias > 0 ->
                    "Backs Season ${plan.targetYear} calendar: ${plan.holidayShowcaseCount} Holiday Marquees & ${plan.internationalGameCount} International Games boost rights value."
                choice == VoteChoice.APPROVE ->
                    "Approves ${plan.regularSeasonWeeks}-week Season ${plan.targetYear} slate (+\$${plan.projectedBroadcastBonusMillions}M TV upside)."
                plan.internationalGameCount > 1 && !unionAgreement.mandatoryPrivateCharterFlights ->
                    "Opposes multi-continent travel slate without Mandatory Private Charter CBA safeguards."
                plan.holidayShowcaseCount == 0 ->
                    "Withholds vote: demands marquee Holiday Rivalry tentpoles to anchor network rights."
                else ->
                    "Raises gate-equity concerns over international neutral-site home-date reallocations."
            }
            OwnerBallot(
                franchiseId = franchise.id,
                teamName = franchise.teamFullName,
                ownershipGroupName = franchise.ownership.name,
                owner = owner,
                choice = choice,
                alignmentScore = score.coerceIn(-100, 100),
                rationale = rationale
            )
        }

        val yes = ballots.count { it.choice == VoteChoice.APPROVE }
        val no = ballots.count { it.choice == VoteChoice.OPPOSE }
        val abstain = ballots.count { it.choice == VoteChoice.ABSTAIN }
        val total = ballots.size
        val passed = !plan.hasValidationErrors && yes * 2 > total

        return BoardVoteResult(
            id = "vote_schedule_${plan.targetYear}_${week}_${System.currentTimeMillis() % 10000}",
            proposalTitle = "Ratify Season ${plan.targetYear} Regular Season Schedule (${plan.regularSeasonWeeks}W)",
            proposalSummary = "${plan.slots.size} matchups • ${plan.holidayShowcaseCount} Holiday Showcases • ${plan.internationalGameCount} International Games • +\$${plan.projectedBroadcastBonusMillions}M TV Bonus",
            category = GovernanceProposalCategory.SCHEDULE_CALENDAR,
            ballots = ballots,
            yesVotes = yes,
            noVotes = no,
            abstainVotes = abstain,
            totalVotes = total,
            passed = passed,
            enactedViaCommissionerOverride = enactedViaCommissionerOverride,
            year = year,
            week = week
        )
    }

    fun evaluateSeasonExpansionVote(
        franchises: List<Franchise>,
        currentWeeks: Int,
        targetWeeks: Int,
        unionAgreement: LaborUnionAgreement,
        year: Int,
        week: Int,
        enactedViaCommissionerOverride: Boolean = false
    ): BoardVoteResult {
        val delta = targetWeeks - currentWeeks
        val ballots = franchises.map { franchise ->
            val owner = franchise.ownership.primaryOwner
            val arch = owner.archetype
            val score = if (delta >= 0) {
                var base = when (arch) {
                    OwnerArchetype.BROADCAST_SHOWMAN -> 30
                    OwnerArchetype.GLOBAL_EXPANSIONIST -> 26
                    OwnerArchetype.FISCAL_HAWK -> if (targetWeeks <= 18) 20 else 8
                    OwnerArchetype.ANALYTICS_VISIONARY -> if (targetWeeks <= 18) 14 else -4
                    OwnerArchetype.COMMUNITY_STEWARD -> if (targetWeeks <= 18) 10 else -12
                    OwnerArchetype.PLAYERS_FIRST_PATRON -> {
                        if (unionAgreement.maxBackToBackGamesLimit && unionAgreement.playerHealthWelfareMillions >= 16L) 12 else -18
                    }
                    OwnerArchetype.TRADITIONALIST_PURIST -> if (targetWeeks <= 16) -4 else -24
                }
                if (targetWeeks >= 20 && !unionAgreement.maxBackToBackGamesLimit) base -= 14
                if (unionAgreement.strikeRisk >= 60) base -= 16
                base
            } else {
                // Contraction / shortening back toward 14W
                when (arch) {
                    OwnerArchetype.TRADITIONALIST_PURIST -> 26
                    OwnerArchetype.PLAYERS_FIRST_PATRON -> 24
                    OwnerArchetype.COMMUNITY_STEWARD -> 12
                    OwnerArchetype.ANALYTICS_VISIONARY -> 10
                    OwnerArchetype.FISCAL_HAWK -> -14
                    OwnerArchetype.GLOBAL_EXPANSIONIST -> -22
                    OwnerArchetype.BROADCAST_SHOWMAN -> -28
                }
            }
            val choice = if (score >= 0) VoteChoice.APPROVE else VoteChoice.OPPOSE
            val rationale = if (delta >= 0) {
                if (choice == VoteChoice.APPROVE) {
                    "Supports ${targetWeeks}-week expansion to unlock extra national TV windows and gate revenue."
                } else {
                    "Opposes ${targetWeeks}-week expansion citing player workload saturation or record-book distortion."
                }
            } else {
                if (choice == VoteChoice.APPROVE) {
                    "Supports streamlined ${targetWeeks}-week slate to protect star durability and integrity."
                } else {
                    "Opposes trimming to ${targetWeeks} weeks due to foregone broadcast & sponsorship inventory."
                }
            }
            OwnerBallot(
                franchiseId = franchise.id,
                teamName = franchise.teamFullName,
                ownershipGroupName = franchise.ownership.name,
                owner = owner,
                choice = choice,
                alignmentScore = score.coerceIn(-100, 100),
                rationale = rationale
            )
        }

        val yes = ballots.count { it.choice == VoteChoice.APPROVE }
        val no = ballots.count { it.choice == VoteChoice.OPPOSE }
        val abstain = ballots.count { it.choice == VoteChoice.ABSTAIN }
        val total = ballots.size
        val passed = yes * 2 > total
        val verb = if (targetWeeks >= currentWeeks) "Expand" else "Adjust"

        return BoardVoteResult(
            id = "vote_season_len_${targetWeeks}_${year}_${week}_${System.currentTimeMillis() % 10000}",
            proposalTitle = "$verb Regular Season to $targetWeeks Weeks",
            proposalSummary = "Proposal shifts regular season from $currentWeeks weeks to $targetWeeks weeks (All-Star Week ${ScheduleCalendarEngine.allStarWeekFor(targetWeeks)}).",
            category = GovernanceProposalCategory.SCHEDULE_CALENDAR,
            ballots = ballots,
            yesVotes = yes,
            noVotes = no,
            abstainVotes = abstain,
            totalVotes = total,
            passed = passed,
            enactedViaCommissionerOverride = enactedViaCommissionerOverride,
            year = year,
            week = week
        )
    }
}
