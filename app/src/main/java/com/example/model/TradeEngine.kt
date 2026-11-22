package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

object TradeEngine {

    fun generateDefaultRosterForFranchise(
        franchiseId: String,
        cityName: String,
        sportType: SportType,
        baseRating: Int
    ): List<RosterPlayer> {
        val random = Random(franchiseId.hashCode())
        val positions = sportType.starterPositions

        val firstNames = listOf(
            "Marcus", "Jordan", "Derrick", "Brandon", "Malachi", "Julian", "Terrence",
            "Caleb", "Tyler", "Isaac", "Xavier", "Damon", "Jaylen", "DeAndre", "Christian"
        )
        val lastNames = listOf(
            "Vance", "Mercer", "Cole", "Miller", "Hayes", "Vega", "Sterling",
            "Brooks", "Rhodes", "Jackson", "Rivers", "Adams", "Cruz", "Washington", "Chen"
        )
        val archetypes = sportType.archetypes

        // 5 Starters (indices 0..4) + 3 Bench Rotation Reserves (indices 5..7)
        val benchPositions = sportType.benchDepthPositions
        val rosterSlots = positions.mapIndexed { idx, p -> Triple(idx, p, true) } +
            benchPositions.mapIndexed { bIdx, p -> Triple(5 + bIdx, p, false) }

        return rosterSlots.map { (index, pos, starter) ->
            val ratingOffset = when (index) {
                0 -> random.nextInt(3, 7) // Star Player
                1 -> random.nextInt(1, 4) // Secondary Co-Star
                2 -> random.nextInt(-2, 2) // Starting Contributor
                3 -> random.nextInt(-4, 0) // Solid Starter
                4 -> random.nextInt(-5, -1) // 5th Starter
                5 -> random.nextInt(-6, -2) // Sixth Man / Top Guard Backup
                6 -> random.nextInt(-8, -3) // Rotation Wing/Forward Backup
                else -> random.nextInt(-10, -4) // Frontcourt / Depth Reserve
            }
            val rating = (baseRating + ratingOffset).coerceIn(67, 95)
            val age = random.nextInt(21, 34)
            val salary = when {
                rating >= 90 -> random.nextLong(32L, 44L)
                rating >= 85 -> random.nextLong(22L, 31L)
                rating >= 80 -> random.nextLong(14L, 21L)
                rating >= 75 -> random.nextLong(7L, 13L)
                else -> random.nextLong(4L, 7L)
            }
            val contractYears = random.nextInt(1, 5)
            val fn = firstNames[(random.nextInt(firstNames.size) + index * 2) % firstNames.size]
            val ln = lastNames[(random.nextInt(lastNames.size) + kotlin.math.abs(franchiseId.hashCode()) + index) % lastNames.size]

            RosterPlayer(
                id = "player_${franchiseId}_$index",
                name = "$fn $ln",
                franchiseId = franchiseId,
                position = pos,
                overallRating = rating,
                age = age,
                salaryMillions = salary,
                contractYearsRemaining = contractYears,
                archetype = archetypes[(index + random.nextInt(archetypes.size)) % archetypes.size],
                depthOrder = index,
                isStarter = starter,
                activeInjury = null
            )
        }
    }

    fun generateDefaultDraftPicksForFranchise(
        franchiseId: String,
        franchiseName: String,
        currentYear: Int
    ): List<DraftPick> {
        val picks = mutableListOf<DraftPick>()
        for (yearOffset in 0..2) {
            val pickYear = currentYear + yearOffset
            picks.add(
                DraftPick(
                    id = "pick_${franchiseId}_${pickYear}_r1",
                    year = pickYear,
                    round = 1,
                    originalFranchiseId = franchiseId,
                    originalFranchiseName = franchiseName,
                    currentFranchiseId = franchiseId,
                    isProtected = yearOffset == 2,
                    protectionDescription = if (yearOffset == 2) "Top-3 Protected" else "Unprotected"
                )
            )
            picks.add(
                DraftPick(
                    id = "pick_${franchiseId}_${pickYear}_r2",
                    year = pickYear,
                    round = 2,
                    originalFranchiseId = franchiseId,
                    originalFranchiseName = franchiseName,
                    currentFranchiseId = franchiseId,
                    isProtected = false,
                    protectionDescription = "Unprotected"
                )
            )
        }
        return picks
    }

    fun calculateFairnessScore(teamAValue: Int, teamBValue: Int): Int {
        if (teamAValue <= 0 && teamBValue <= 0) return 50
        val maxVal = maxOf(teamAValue, teamBValue).toDouble()
        val minVal = minOf(teamAValue, teamBValue).toDouble()
        if (maxVal == 0.0) return 50
        val ratio = minVal / maxVal
        return (ratio * 100).roundToInt().coerceIn(10, 100)
    }

    fun isSalaryCapCompliant(
        salaryA: Long,
        salaryB: Long,
        strictnessPct: Int
    ): Boolean {
        if (salaryA == 0L && salaryB == 0L) return true
        val multiplier = strictnessPct.toDouble() / 100.0
        val maxDiff = maxOf(salaryA, salaryB)
        val minDiff = minOf(salaryA, salaryB)
        if (minDiff == 0L) {
            return maxDiff <= 8L // Allowance for minor absorptions
        }
        return (maxDiff.toDouble() <= minDiff.toDouble() * multiplier + 5.0)
    }

    fun generateRealisticTradeOffer(
        week: Int,
        franchises: List<Franchise>,
        tradeRules: TradeRules
    ): TradeProposal? {
        if (franchises.size < 2) return null

        val sortedByRecord = franchises.sortedByDescending { it.winPct }
        val buyer = sortedByRecord.firstOrNull { it.roster.isNotEmpty() } ?: franchises[0]
        val seller = sortedByRecord.lastOrNull { it.id != buyer.id && it.roster.isNotEmpty() }
            ?: franchises.firstOrNull { it.id != buyer.id } ?: return null

        // Prefer players actively listed on the Seller's Trade Block or sought by Buyer
        val buyerTargetsOnSeller = seller.roster.filter { it.id in buyer.seekingPlayerIds }
        val sellerBlockPlayers = seller.tradeBlockPlayers
        val starPlayer = buyerTargetsOnSeller.maxByOrNull { it.overallRating }
            ?: sellerBlockPlayers.maxByOrNull { it.overallRating }
            ?: seller.roster.maxByOrNull { it.overallRating }
            ?: return null

        // Buyer offers a player from its Trade Block (or seller's target) or a young prospect + draft pick(s)
        val sellerTargetsOnBuyer = buyer.roster.filter { it.id in seller.seekingPlayerIds && it.id != starPlayer.id }
        val buyerBlockCandidates = buyer.tradeBlockPlayers.filter { it.id != starPlayer.id }
        val youngProspect = sellerTargetsOnBuyer.firstOrNull()
            ?: buyerBlockCandidates.minByOrNull { abs(it.tradeValue - starPlayer.tradeValue) }
            ?: buyer.roster.filter { it.id != starPlayer.id }.minByOrNull { it.age }
            ?: buyer.roster.firstOrNull()
            ?: return null

        val orderedPicks = buyer.draftPicks.sortedWith(compareBy<DraftPick> { it.year }.thenBy { it.round })
        val pick1 = orderedPicks.firstOrNull { it.round == 1 } ?: DraftPick(
            id = "pick_${buyer.id}_${week}_r1",
            year = 2027,
            round = 1,
            originalFranchiseId = buyer.id,
            originalFranchiseName = buyer.teamFullName,
            currentFranchiseId = buyer.id
        )

        val pick2 = orderedPicks.firstOrNull { it.round == 2 && it.id != pick1.id }

        val teamASendsPlayers = listOf(youngProspect)
        val teamASendsPicks = listOfNotNull(pick1, pick2)
        val teamBSendsPlayers = listOf(starPlayer)
        val teamBSendsPicks = emptyList<DraftPick>()

        val aVal = teamASendsPlayers.sumOf { it.tradeValue } + teamASendsPicks.sumOf { it.tradeValue }
        val bVal = teamBSendsPlayers.sumOf { it.tradeValue }
        val fairness = calculateFairnessScore(aVal, bVal)

        val salaryCapCompliant = isSalaryCapCompliant(
            teamASendsPlayers.sumOf { it.salaryMillions },
            teamBSendsPlayers.sumOf { it.salaryMillions },
            tradeRules.salaryMatchingStrictnessPct
        )

        val id = "trade_${buyer.id}_${seller.id}_w${week}_${System.currentTimeMillis() % 10000}"

        return TradeProposal(
            id = id,
            weekProposed = week,
            teamAId = buyer.id,
            teamAName = buyer.teamFullName,
            teamBId = seller.id,
            teamBName = seller.teamFullName,
            teamASendsPlayers = teamASendsPlayers,
            teamASendsPicks = teamASendsPicks,
            teamASendsCashMillions = 0L,
            teamBSendsPlayers = teamBSendsPlayers,
            teamBSendsPicks = teamBSendsPicks,
            teamBSendsCashMillions = 0L,
            status = TradeStatus.PENDING_REVIEW,
            rationaleTeamA = "Contender pushing for championship glory; acquires ${starPlayer.name} (${starPlayer.overallRating} OVR ${starPlayer.position}) to bolster postseason rotation.",
            rationaleTeamB = "Rebuilding club stockpiling assets; acquires ${youngProspect.name} (${youngProspect.age} yo, ${youngProspect.overallRating} OVR) plus future draft capital.",
            fairnessScore = fairness,
            salaryCapCompliant = salaryCapCompliant,
            commissionerReviewNotes = "Balanced buyer-seller dynamic. Requires commissioner ratification."
        )
    }

    fun evaluateAiTradeAcceptance(
        proposal: TradeProposal,
        respondingTeamId: String
    ): Pair<Boolean, String> {
        val aVal = proposal.teamATotalTradeValue
        val bVal = proposal.teamBTotalTradeValue

        val incomingValueForB = aVal
        val outgoingValueForB = bVal

        val ratio = if (outgoingValueForB > 0) incomingValueForB.toDouble() / outgoingValueForB.toDouble() else 1.0

        return when {
            !proposal.salaryCapCompliant -> false to "Trade rejected: Proposed player salaries violate the league's luxury tax & cap-matching rules."
            ratio < 0.75 -> false to "Trade rejected: The asset value offered is insufficient for the talent and draft capital requested (Value deficit: ${(100 - (ratio * 100)).toInt()}%)."
            proposal.teamBSendsPlayers.any { it.overallRating >= 90 } && proposal.teamASendsPicks.none { it.round == 1 } ->
                false to "Trade rejected: General Manager insists on at least one Future 1st Round Pick when moving a franchise superstar."
            else -> true to "Trade terms accepted: Value evaluation aligns with franchise strategic goals."
        }
    }

    fun getPositionsForSport(sportType: SportType): List<String> = sportType.starterPositions

    fun seedTradeBlocksForLeague(franchises: List<Franchise>): List<Franchise> {
        if (franchises.size < 2) return franchises
        return franchises.map { team ->
            if (team.tradeBlockListings.isNotEmpty() || team.seekingPlayerIds.isNotEmpty()) {
                team
            } else {
                buildAutoTradeBlockForTeam(team, franchises)
            }
        }
    }

    fun buildAutoTradeBlockForTeam(
        team: Franchise,
        allFranchises: List<Franchise>
    ): Franchise {
        if (team.roster.isEmpty()) return team
        val isRebuilding = team.rosterRating < 81 || team.winPct < 0.48

        // Pick 2 trade-block candidates per team:
        // - Rebuilding teams dangle highest-paid veterans or expiring deals for draft capital
        // - Contenders shop bench reserves / mid-tier contracts for a win-now upgrade
        val candidateListings = if (isRebuilding) {
            val vet = team.roster.sortedWith(compareByDescending<RosterPlayer> { it.age }.thenByDescending { it.salaryMillions })
                .firstOrNull()
            val expiring = team.roster.filter { it.id != vet?.id }
                .sortedWith(compareBy<RosterPlayer> { it.contractYearsRemaining }.thenByDescending { it.overallRating })
                .firstOrNull()
            listOfNotNull(
                vet?.let {
                    TradeBlockListing(
                        playerId = it.id,
                        askingPriority = TradeAskingPriority.FUTURE_DRAFT_CAPITAL,
                        askingNote = "Seeking 1st Round Pick or ascending young core piece"
                    )
                },
                expiring?.let {
                    TradeBlockListing(
                        playerId = it.id,
                        askingPriority = TradeAskingPriority.YOUNG_PROSPECT,
                        askingNote = "Expiring rotation contributor available for youth"
                    )
                }
            )
        } else {
            val benchChip = team.roster.filter { !it.isStarter }
                .maxByOrNull { it.tradeValue }
            val redundantPiece = team.roster.filter { it.id != benchChip?.id }
                .sortedBy { it.overallRating }
                .drop(1)
                .firstOrNull()
            listOfNotNull(
                benchChip?.let {
                    TradeBlockListing(
                        playerId = it.id,
                        askingPriority = TradeAskingPriority.CONTENDER_UPGRADE,
                        askingNote = "Packaged with draft equity for proven playoff starter"
                    )
                },
                redundantPiece?.let {
                    TradeBlockListing(
                        playerId = it.id,
                        askingPriority = TradeAskingPriority.CAP_RELIEF,
                        askingNote = "Available for positional depth or cap flexibility"
                    )
                }
            )
        }

        // Identify weakest starting position to seek
        val weakestStarterPos = team.roster.filter { it.isStarter }.minByOrNull { it.overallRating }?.position
            ?: team.roster.minByOrNull { it.overallRating }?.position
        val secondaryNeedPos = team.roster.groupBy { it.position }
            .minByOrNull { (_, list) -> list.maxOfOrNull { it.overallRating } ?: 0 }?.key

        val seekingPositions = listOfNotNull(weakestStarterPos, secondaryNeedPos).distinct().take(2)

        // Pick 2 rival players matching team strategy & weakest position
        val rivalPool = allFranchises.filter { it.id != team.id }.flatMap { it.roster }
        val rivalTargets = if (isRebuilding) {
            rivalPool.filter { it.age <= 25 && it.overallRating >= 76 }
                .sortedWith(compareByDescending<RosterPlayer> { it.position in seekingPositions }.thenByDescending { it.overallRating })
                .take(2)
        } else {
            rivalPool.filter { it.overallRating >= 81 && !it.isMedicallyOut }
                .sortedWith(compareByDescending<RosterPlayer> { it.position in seekingPositions }.thenByDescending { it.overallRating })
                .take(2)
        }

        return team.copy(
            tradeBlockListings = candidateListings,
            seekingPlayerIds = rivalTargets.map { it.id },
            seekingPositions = seekingPositions
        )
    }

    fun syncTradeBlocksAfterTrade(franchises: List<Franchise>): List<Franchise> {
        val allRosterIds = franchises.flatMap { t -> t.roster.map { it.id } }.toSet()
        return franchises.map { team ->
            val ownedIds = team.roster.map { it.id }.toSet()
            val cleanedListings = team.tradeBlockListings.filter { it.playerId in ownedIds }
            val cleanedSeeking = team.seekingPlayerIds.filter { it in allRosterIds && it !in ownedIds }
            team.copy(
                tradeBlockListings = cleanedListings,
                seekingPlayerIds = cleanedSeeking
            )
        }
    }

    fun buildMatchmakingRecommendations(
        franchises: List<Franchise>,
        seasonStats: Map<String, PlayerSeasonStats>,
        tradeRules: TradeRules
    ): List<TradeBlockMatch> {
        val matches = mutableListOf<TradeBlockMatch>()
        val seenPairs = mutableSetOf<String>()

        for (teamA in franchises) {
            val blockA = teamA.tradeBlockPlayers
            if (blockA.isEmpty()) continue

            for (teamB in franchises) {
                if (teamB.id <= teamA.id) continue
                val blockB = teamB.tradeBlockPlayers
                val seekBFromA = blockA.filter { it.id in teamB.seekingPlayerIds || it.position in teamB.seekingPositions }
                val seekAFromB = teamB.roster.filter { it.id in teamA.seekingPlayerIds || (it.isPlayerInBlock(teamB) && it.position in teamA.seekingPositions) }

                val offerA = seekBFromA.firstOrNull() ?: blockA.firstOrNull() ?: continue
                val targetB = seekAFromB.firstOrNull() ?: blockB.firstOrNull() ?: continue

                val key = "${teamA.id}_${teamB.id}_${offerA.id}_${targetB.id}"
                if (!seenPairs.add(key)) continue

                val fairness = calculateFairnessScore(offerA.tradeValue, targetB.tradeValue)
                val capOk = isSalaryCapCompliant(
                    offerA.salaryMillions,
                    targetB.salaryMillions,
                    tradeRules.salaryMatchingStrictnessPct
                )

                val mutualMatch = (offerA.id in teamB.seekingPlayerIds || offerA.position in teamB.seekingPositions) &&
                    (targetB.id in teamA.seekingPlayerIds || teamB.isPlayerOnTradeBlock(targetB.id))

                val score = fairness + (if (mutualMatch) 20 else 0) + (if (capOk) 10 else -15)
                val statA = seasonStats[offerA.id]
                val statB = seasonStats[targetB.id]
                val summaryA = if (statA != null && statA.gamesPlayed > 0) "${statA.ppg} PPG" else "${offerA.overallRating} OVR"
                val summaryB = if (statB != null && statB.gamesPlayed > 0) "${statB.ppg} PPG" else "${targetB.overallRating} OVR"

                matches.add(
                    TradeBlockMatch(
                        id = key,
                        teamA = teamA,
                        teamB = teamB,
                        teamAOfferedPlayer = offerA,
                        teamBTargetPlayer = targetB,
                        fairnessScore = fairness,
                        salaryCapCompliant = capOk,
                        isMutualInterest = mutualMatch,
                        matchScore = score,
                        fitHeadline = if (mutualMatch) {
                            "Mutual Fit: ${teamA.nickname} (${offerA.position} • $summaryA) ⇄ ${teamB.nickname} (${targetB.position} • $summaryB)"
                        } else {
                            "Block Proposal: ${offerA.name} ($summaryA) for ${targetB.name} ($summaryB)"
                        }
                    )
                )
            }
        }

        return matches.sortedByDescending { it.matchScore }.take(12)
    }

    private fun RosterPlayer.isPlayerInBlock(team: Franchise): Boolean =
        team.isPlayerOnTradeBlock(id)

    fun transferPickForTrade(
        pick: DraftPick,
        fromTeam: Franchise,
        toTeam: Franchise,
        week: Int,
        tradeId: String
    ): DraftPick = pick.copy(
        currentFranchiseId = toTeam.id,
        acquiredFromFranchiseId = fromTeam.id,
        acquiredFromFranchiseName = fromTeam.teamFullName,
        acquiredInWeek = week,
        acquiredTradeId = tradeId
    )

    fun rollDraftPicksForNewSeason(
        franchises: List<Franchise>,
        finishedYear: Int,
        nextYear: Int,
        deferredSlots: List<DraftOrderSlot> = emptyList()
    ): List<Franchise> {
        // Identify any Top-3 protected Round 1 picks that were retained by the original team in finishedYear;
        // their obligation defers to nextYear Round 1 Unprotected!
        val deferralMap = mutableMapOf<String, String>() // originalFranchiseId -> holderFranchiseId
        franchises.forEach { holder ->
            holder.draftPicks
                .filter { it.year == finishedYear && it.round == 1 && it.originalFranchiseId != holder.id }
                .forEach { finishedPick ->
                    val wasDeferred = deferredSlots.any {
                        it.retainedDueToProtection && it.originalFranchiseId == finishedPick.originalFranchiseId && it.round == 1
                    }
                    if (wasDeferred) {
                        deferralMap[finishedPick.originalFranchiseId] = holder.id
                    }
                }
        }

        // First strip finishedYear picks, and resolve deferred nextYear picks if applicable
        val workingPicksByTeam = franchises.associate { team ->
            team.id to team.draftPicks.filter { it.year > finishedYear }.toMutableList()
        }.toMutableMap()

        deferralMap.forEach { (origTeamId, recipientTeamId) ->
            val origList = workingPicksByTeam[origTeamId] ?: return@forEach
            val recipientList = workingPicksByTeam[recipientTeamId] ?: return@forEach
            val nextYearR1Idx = origList.indexOfFirst { it.year == nextYear && it.round == 1 && it.originalFranchiseId == origTeamId }
            val origName = franchises.find { it.id == origTeamId }?.teamFullName ?: origTeamId
            if (nextYearR1Idx >= 0) {
                val moved = origList.removeAt(nextYearR1Idx)
                recipientList.add(
                    moved.copy(
                        currentFranchiseId = recipientTeamId,
                        isProtected = false,
                        protectionDescription = "Unprotected (Rollover from $finishedYear)",
                        acquiredFromFranchiseId = origTeamId,
                        acquiredFromFranchiseName = origName,
                        deferredFromYear = finishedYear
                    )
                )
            }
        }

        // Ensure every franchise has a rolling 3-year window (nextYear..nextYear+2) for any unowned slots
        val allClaimedOrigYearRound = workingPicksByTeam.values.flatten()
            .map { Triple(it.originalFranchiseId, it.year, it.round) }
            .toSet()

        return franchises.map { team ->
            val list = workingPicksByTeam[team.id]?.toMutableList() ?: mutableListOf()
            for (yOffset in 0..2) {
                val targetYear = nextYear + yOffset
                for (round in 1..2) {
                    val key = Triple(team.id, targetYear, round)
                    if (key !in allClaimedOrigYearRound && list.none { it.originalFranchiseId == team.id && it.year == targetYear && it.round == round }) {
                        val isTop3Prot = (round == 1 && yOffset == 2)
                        list.add(
                            DraftPick(
                                id = "pick_${team.id}_${targetYear}_r$round",
                                year = targetYear,
                                round = round,
                                originalFranchiseId = team.id,
                                originalFranchiseName = team.teamFullName,
                                currentFranchiseId = team.id,
                                isProtected = isTop3Prot,
                                protectionDescription = if (isTop3Prot) "Top-3 Protected" else "Unprotected"
                            )
                        )
                    }
                }
            }
            team.copy(
                draftPicks = list.sortedWith(compareBy<DraftPick> { it.year }.thenBy { it.round }.thenBy { it.originalFranchiseName })
            )
        }
    }

    fun buildTradedPickLedger(
        franchises: List<Franchise>,
        currentYear: Int,
        activeSession: OffSeasonSession? = null,
        latestCompletedOffSeason: OffSeasonSession? = null
    ): List<TradedPickLedgerEntry> {
        val franchiseMap = franchises.associateBy { it.id }

        // Estimate current-year slot per original team from active lottery or inverse standings
        val projectedSlotByOrigId: Map<String, Int> = if (activeSession != null && activeSession.lotteryResults.isNotEmpty()) {
            activeSession.lotteryResults.associate { it.franchiseId to it.finalDraftPick }
        } else {
            franchises.sortedWith(
                compareBy<Franchise> { it.winPct }
                    .thenBy { it.pointsFor - it.pointsAgainst }
                    .thenBy { it.rosterRating }
            ).mapIndexed { idx, team -> team.id to (idx + 1) }.toMap()
        }

        val draftRecords = (activeSession?.draftPicks.orEmpty() + latestCompletedOffSeason?.draftPicks.orEmpty())
            .associateBy { it.sourcePickId }

        val entries = mutableListOf<TradedPickLedgerEntry>()
        val seenIds = mutableSetOf<String>()

        franchises.forEach { holder ->
            holder.draftPicks
                .filter { it.currentFranchiseId != it.originalFranchiseId || it.deferredFromYear != null }
                .forEach { pick ->
                    seenIds.add(pick.id)
                    val fromId = pick.acquiredFromFranchiseId ?: pick.originalFranchiseId
                    val fromName = pick.acquiredFromFranchiseName
                        ?: franchiseMap[fromId]?.teamFullName
                        ?: pick.originalFranchiseName
                    val toName = franchiseMap[pick.currentFranchiseId]?.teamFullName ?: holder.teamFullName

                    val exercisedRecord = draftRecords[pick.id]
                    val deferredSlot = activeSession?.draftOrderSlots?.find {
                        it.sourcePickId == pick.id && it.retainedDueToProtection
                    }

                    val status = when {
                        exercisedRecord != null -> TradedPickStatus.EXERCISED_IN_DRAFT
                        deferredSlot != null || pick.deferredFromYear != null -> TradedPickStatus.PROTECTION_DEFERRED
                        else -> TradedPickStatus.PENDING_CONVEYANCE
                    }

                    val outcomeSummary = when {
                        exercisedRecord != null ->
                            "Pick #${exercisedRecord.pickNumber} (R${exercisedRecord.round}): Drafted ${exercisedRecord.playerName} (${exercisedRecord.position}, ${exercisedRecord.overallRating} OVR)"
                        deferredSlot != null ->
                            deferredSlot.tradeSummaryNote
                        pick.deferredFromYear != null ->
                            "Converted from ${pick.deferredFromYear} Top-3 protected obligation"
                        else -> ""
                    }

                    val projSlot = if (pick.year == currentYear && pick.round == 1) {
                        projectedSlotByOrigId[pick.originalFranchiseId]
                    } else null

                    entries.add(
                        TradedPickLedgerEntry(
                            pickId = pick.id,
                            year = pick.year,
                            round = pick.round,
                            originalFranchiseId = pick.originalFranchiseId,
                            originalFranchiseName = pick.originalFranchiseName,
                            fromFranchiseId = fromId,
                            fromFranchiseName = fromName,
                            toFranchiseId = pick.currentFranchiseId,
                            toFranchiseName = toName,
                            weekTraded = pick.acquiredInWeek,
                            isProtected = pick.isProtected,
                            protectionDescription = pick.protectionDescription,
                            deferredFromYear = pick.deferredFromYear,
                            projectedSlot = projSlot,
                            status = status,
                            outcomeSummary = outcomeSummary
                        )
                    )
                }
        }

        // Also surface already-exercised traded picks from recent off-seasons after rolling forward
        listOfNotNull(activeSession, latestCompletedOffSeason).forEach { session ->
            session.draftPicks
                .filter { it.acquiredViaTrade }
                .forEach { rec ->
                    val key = rec.sourcePickId.ifBlank { "exercised_${session.year}_r${rec.round}_p${rec.pickNumber}_${rec.originalFranchiseId}" }
                    if (key !in seenIds) {
                        seenIds.add(key)
                        entries.add(
                            TradedPickLedgerEntry(
                                pickId = key,
                                year = session.year,
                                round = rec.round,
                                originalFranchiseId = rec.originalFranchiseId,
                                originalFranchiseName = rec.originalFranchiseName,
                                fromFranchiseId = rec.originalFranchiseId,
                                fromFranchiseName = rec.originalFranchiseName,
                                toFranchiseId = rec.franchiseId,
                                toFranchiseName = rec.franchiseName,
                                weekTraded = null,
                                isProtected = false,
                                protectionDescription = "Conveyed",
                                projectedSlot = rec.pickNumber,
                                status = TradedPickStatus.EXERCISED_IN_DRAFT,
                                outcomeSummary = "Pick #${rec.pickNumber} (R${rec.round}): Drafted ${rec.playerName} (${rec.position}, ${rec.overallRating} OVR)"
                            )
                        )
                    }
                }
        }

        return entries.sortedWith(
            compareBy<TradedPickLedgerEntry> { it.status.ordinal }
                .thenBy { it.year }
                .thenBy { it.round }
                .thenBy { it.projectedSlot ?: 99 }
        )
    }
}

data class TradeBlockMatch(
    val id: String,
    val teamA: Franchise,
    val teamB: Franchise,
    val teamAOfferedPlayer: RosterPlayer,
    val teamBTargetPlayer: RosterPlayer,
    val fairnessScore: Int,
    val salaryCapCompliant: Boolean,
    val isMutualInterest: Boolean,
    val matchScore: Int,
    val fitHeadline: String
)
