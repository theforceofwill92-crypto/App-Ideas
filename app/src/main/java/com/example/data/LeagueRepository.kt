package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LeagueRepository(context: Context) {

    private val appContext = context.applicationContext

    private val db = AppDatabase.getInstance(appContext)

    private val dao = db.leagueDao()
    private val leagueStateDao = db.leagueStateDao()
    private val revenueHistoryDao = db.leagueRevenueHistoryDao()
    private val faPrefs by lazy {
        appContext.getSharedPreferences("apex_free_agency_prefs", Context.MODE_PRIVATE)
    }
    private val settingsPrefs by lazy {
        appContext.getSharedPreferences("apex_league_settings_prefs", Context.MODE_PRIVATE)
    }
    private val minorPrefs by lazy {
        appContext.getSharedPreferences("apex_minor_league_prefs", Context.MODE_PRIVATE)
    }

    val leagueSaveFlow: Flow<LeagueSaveEntity?> = dao.getLeagueSaveFlow()
    val leagueStateFlow: Flow<LeagueState?> = leagueStateDao.getLeagueStateFlow()
    val fiveYearRevenueHistoryFlow: Flow<List<LeagueRevenueHistoryEntity>> =
        revenueHistoryDao.getLastFiveYearsRevenueFlow().map { list ->
            list.sortedBy { it.seasonYear }
        }

    suspend fun loadLeague(): LeagueSaveEntity? = withContext(Dispatchers.IO) {
        dao.getLeagueSave()
    }

    suspend fun loadLeagueState(): LeagueState? = withContext(Dispatchers.IO) {
        leagueStateDao.getLeagueState()
    }

    suspend fun saveLeagueState(state: LeagueState) = withContext(Dispatchers.IO) {
        leagueStateDao.upsertLeagueState(state)
    }

    suspend fun loadFiveYearRevenueHistory(): List<LeagueRevenueHistoryEntity> = withContext(Dispatchers.IO) {
        revenueHistoryDao.getLastFiveYearsRevenue().sortedBy { it.seasonYear }
    }

    suspend fun upsertAnnualRevenueRecord(record: LeagueRevenueHistoryEntity) = withContext(Dispatchers.IO) {
        revenueHistoryDao.upsertAnnualRevenue(record)
    }

    suspend fun upsertAnnualRevenueRecords(records: List<LeagueRevenueHistoryEntity>) = withContext(Dispatchers.IO) {
        revenueHistoryDao.upsertAllAnnualRevenues(records)
    }

    fun buildCurrentYearRevenueRecord(
        currentYear: Int,
        leagueValuationMillions: Long,
        unionAgreement: LaborUnionAgreement,
        activeBroadcast: BroadcastContract,
        franchises: List<Franchise>,
        championName: String = "In Progress"
    ): LeagueRevenueHistoryEntity {
        val proj = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = franchises,
            activeBroadcast = activeBroadcast,
            unionAgreement = unionAgreement
        )
        val sponsorAnnual = franchises.sumOf { it.localSponsorRevenueMillions.coerceAtLeast(0L) }
        return LeagueRevenueHistoryEntity(
            seasonYear = currentYear,
            totalRevenueMillions = proj.annualLeagueRevenueMillions,
            broadcastRevenueMillions = activeBroadcast.annualPayoutMillions,
            sponsorRevenueMillions = sponsorAnnual,
            namingRightsRevenueMillions = proj.totalNamingRightsRevenueMillions,
            valuationMillions = leagueValuationMillions,
            franchiseCount = franchises.size,
            championName = championName
        )
    }

    fun buildLeagueStateSnapshot(
        leagueName: String,
        currentYear: Int,
        currentWeek: Int,
        seasonPhase: SeasonPhase,
        cashReservesMillions: Long,
        leagueValuationMillions: Long,
        unionAgreement: LaborUnionAgreement,
        activeBroadcast: BroadcastContract,
        franchises: List<Franchise>
    ): LeagueState {
        val revenueProjection = LeagueSimulationEngine.calculateAnnualLeagueRevenueAndNextCap(
            franchises = franchises,
            activeBroadcast = activeBroadcast,
            unionAgreement = unionAgreement
        )
        val sponsorAnnual = franchises.sumOf { it.localSponsorRevenueMillions.coerceAtLeast(0L) }
        val isUnionActive = !unionAgreement.isLockoutActive
        val unionStatus = when {
            unionAgreement.isLockoutActive -> "Owner Lockout Active"
            unionAgreement.strikeRisk >= 50 -> "Critical Strike Threat"
            unionAgreement.cbaYearsRemaining <= 1 -> "CBA Expiring Soon"
            else -> "Active CBA Accord"
        }
        return LeagueState(
            id = 1,
            leagueName = leagueName,
            currentYear = currentYear,
            currentWeek = currentWeek,
            seasonPhase = seasonPhase.name,
            totalRevenueMillions = revenueProjection.annualLeagueRevenueMillions,
            cashReservesMillions = cashReservesMillions,
            leagueValuationMillions = leagueValuationMillions,
            annualBroadcastRevenueMillions = activeBroadcast.annualPayoutMillions,
            annualSponsorRevenueMillions = sponsorAnnual,
            annualNamingRightsRevenueMillions = revenueProjection.totalNamingRightsRevenueMillions,
            franchiseCount = franchises.size,
            isLaborUnionActive = isUnionActive,
            isLockoutActive = unionAgreement.isLockoutActive,
            laborUnionStatus = unionStatus,
            cbaYearsRemaining = unionAgreement.cbaYearsRemaining,
            unionSatisfaction = unionAgreement.unionSatisfaction,
            strikeRisk = unionAgreement.strikeRisk,
            salaryCapMillions = unionAgreement.salaryCapMillions
        )
    }

    suspend fun saveLeague(
        leagueName: String,
        sportType: SportType,
        commissionerBackground: CommissionerBackground,
        currentYear: Int,
        currentWeek: Int,
        seasonPhase: SeasonPhase,
        cashReservesMillions: Long,
        leagueValuationMillions: Long,
        leagueIntegrityScore: Int,
        competitiveParityScore: Int,
        unionAgreement: LaborUnionAgreement,
        activeBroadcast: BroadcastContract,
        franchises: List<Franchise>,
        cities: List<MarketCity>,
        campaigns: List<MarketingCampaign>,
        history: List<LeagueValuationHistory>,
        newsFeed: List<String>,
        freeAgentPool: List<FreeAgentPlayer>? = null,
        freeAgentSignings: List<FreeAgentSigningTransaction>? = null,
        ruleBook: LeagueRuleBook? = null
    ) = withContext(Dispatchers.IO) {
        val entity = LeagueSaveEntity(
            id = 1,
            leagueName = leagueName,
            sportType = sportType.name,
            commissionerBackground = commissionerBackground.name,
            currentYear = currentYear,
            currentWeek = currentWeek,
            seasonPhase = seasonPhase.name,
            cashReservesMillions = cashReservesMillions,
            leagueValuationMillions = leagueValuationMillions,
            leagueIntegrityScore = leagueIntegrityScore,
            competitiveParityScore = competitiveParityScore,
            unionSatisfaction = unionAgreement.unionSatisfaction,
            strikeRisk = unionAgreement.strikeRisk,
            playerRevenueSharePct = unionAgreement.playerRevenueSharePct,
            salaryCapType = unionAgreement.salaryCapType.name,
            salaryCapMillions = unionAgreement.salaryCapMillions,
            activeBroadcastName = activeBroadcast.partnerName,
            activeBroadcastPlatform = activeBroadcast.platform.name,
            activeBroadcastPayoutMillions = activeBroadcast.annualPayoutMillions,
            activeBroadcastYearsRemaining = activeBroadcast.yearsRemaining,
            franchisesJson = serializeFranchises(franchises),
            citiesJson = serializeCities(cities),
            campaignsJson = serializeCampaigns(campaigns),
            historyJson = serializeHistory(history),
            newsFeedJson = serializeStringList(newsFeed)
        )
        dao.saveLeague(entity)
        val leagueState = buildLeagueStateSnapshot(
            leagueName = leagueName,
            currentYear = currentYear,
            currentWeek = currentWeek,
            seasonPhase = seasonPhase,
            cashReservesMillions = cashReservesMillions,
            leagueValuationMillions = leagueValuationMillions,
            unionAgreement = unionAgreement,
            activeBroadcast = activeBroadcast,
            franchises = franchises
        )
        leagueStateDao.upsertLeagueState(leagueState)
        val currentYearRecord = buildCurrentYearRevenueRecord(
            currentYear = currentYear,
            leagueValuationMillions = leagueValuationMillions,
            unionAgreement = unionAgreement,
            activeBroadcast = activeBroadcast,
            franchises = franchises,
            championName = history.firstOrNull { it.year == currentYear }?.championName ?: "In Progress"
        )
        val existingByYear = revenueHistoryDao.getLastFiveYearsRevenue().associateBy { it.seasonYear }
        val historicalRecords = history.distinctBy { it.year }.mapNotNull { h ->
            if (h.year == currentYear) {
                currentYearRecord.copy(
                    championName = h.championName,
                    valuationMillions = h.valuationMillions,
                    totalRevenueMillions = maxOf(h.totalRevenueMillions, currentYearRecord.totalRevenueMillions)
                )
            } else {
                val existing = existingByYear[h.year]
                LeagueRevenueHistoryEntity(
                    seasonYear = h.year,
                    totalRevenueMillions = h.totalRevenueMillions,
                    broadcastRevenueMillions = existing?.broadcastRevenueMillions ?: (h.totalRevenueMillions * 0.42).toLong(),
                    sponsorRevenueMillions = existing?.sponsorRevenueMillions ?: (h.totalRevenueMillions * 0.48).toLong(),
                    namingRightsRevenueMillions = existing?.namingRightsRevenueMillions ?: (h.totalRevenueMillions * 0.10).toLong(),
                    valuationMillions = h.valuationMillions,
                    franchiseCount = existing?.franchiseCount ?: franchises.size,
                    championName = h.championName
                )
            }
        }
        val recordsToPersist = (historicalRecords + currentYearRecord)
            .associateBy { it.seasonYear }
            .values
            .sortedBy { it.seasonYear }
        revenueHistoryDao.upsertAllAnnualRevenues(recordsToPersist)
        if (freeAgentPool != null || freeAgentSignings != null) {
            saveFreeAgencyState(
                pool = freeAgentPool ?: emptyList(),
                transactions = freeAgentSignings ?: emptyList()
            )
        }
        if (ruleBook != null) {
            saveLeagueSettings(ruleBook)
        }
    }

    fun clearPrefsSync() {
        faPrefs.edit().clear().apply()
        settingsPrefs.edit().clear().apply()
        minorPrefs.edit().clear().apply()
    }

    suspend fun resetLeague() = withContext(Dispatchers.IO) {
        dao.deleteLeagueSave()
        leagueStateDao.deleteLeagueState()
        revenueHistoryDao.clearRevenueHistory()
        clearPrefsSync()
    }

    fun saveBoardVoteHistory(votes: List<BoardVoteResult>) {
        val arr = JSONArray()
        votes.take(30).forEach { vote ->
            val ballotsArr = JSONArray()
            vote.ballots.forEach { b ->
                ballotsArr.put(
                    JSONObject().apply {
                        put("franchiseId", b.franchiseId)
                        put("teamName", b.teamName)
                        put("ownershipGroupName", b.ownershipGroupName)
                        put("ownerId", b.owner.id)
                        put("ownerName", b.owner.fullName)
                        put("ownerRole", b.owner.roleTitle)
                        put("ownerBio", b.owner.backgroundBio)
                        put("ownerArchetype", b.owner.archetype.name)
                        put("choice", b.choice.name)
                        put("alignmentScore", b.alignmentScore)
                        put("rationale", b.rationale)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", vote.id)
                    put("proposalTitle", vote.proposalTitle)
                    put("proposalSummary", vote.proposalSummary)
                    put("category", vote.category.name)
                    put("ballots", ballotsArr)
                    put("yesVotes", vote.yesVotes)
                    put("noVotes", vote.noVotes)
                    put("abstainVotes", vote.abstainVotes)
                    put("totalVotes", vote.totalVotes)
                    put("passed", vote.passed)
                    put("enactedViaCommissionerOverride", vote.enactedViaCommissionerOverride)
                    put("year", vote.year)
                    put("week", vote.week)
                }
            )
        }
        settingsPrefs.edit().putString("board_vote_history_json", arr.toString()).apply()
    }

    fun loadBoardVoteHistory(): List<BoardVoteResult> {
        val json = settingsPrefs.getString("board_vote_history_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<BoardVoteResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val ballotsArr = obj.optJSONArray("ballots") ?: JSONArray()
                val ballots = mutableListOf<OwnerBallot>()
                for (j in 0 until ballotsArr.length()) {
                    val bObj = ballotsArr.optJSONObject(j) ?: continue
                    val arch = runCatching {
                        OwnerArchetype.valueOf(bObj.optString("ownerArchetype"))
                    }.getOrDefault(OwnerArchetype.ANALYTICS_VISIONARY)
                    val choice = runCatching {
                        VoteChoice.valueOf(bObj.optString("choice"))
                    }.getOrDefault(VoteChoice.APPROVE)
                    ballots.add(
                        OwnerBallot(
                            franchiseId = bObj.optString("franchiseId"),
                            teamName = bObj.optString("teamName"),
                            ownershipGroupName = bObj.optString("ownershipGroupName"),
                            owner = FranchiseOwner(
                                id = bObj.optString("ownerId", "owner_$j"),
                                fullName = bObj.optString("ownerName", "Governor"),
                                roleTitle = bObj.optString("ownerRole", "Principal Governor"),
                                backgroundBio = bObj.optString("ownerBio", ""),
                                archetype = arch
                            ),
                            choice = choice,
                            alignmentScore = bObj.optInt("alignmentScore", 0),
                            rationale = bObj.optString("rationale", "")
                        )
                    )
                }
                result.add(
                    BoardVoteResult(
                        id = obj.optString("id"),
                        proposalTitle = obj.optString("proposalTitle"),
                        proposalSummary = obj.optString("proposalSummary"),
                        category = runCatching {
                            GovernanceProposalCategory.valueOf(obj.optString("category"))
                        }.getOrDefault(GovernanceProposalCategory.RULE_CHANGE),
                        ballots = ballots,
                        yesVotes = obj.optInt("yesVotes", 0),
                        noVotes = obj.optInt("noVotes", 0),
                        abstainVotes = obj.optInt("abstainVotes", 0),
                        totalVotes = obj.optInt("totalVotes", ballots.size),
                        passed = obj.optBoolean("passed", false),
                        enactedViaCommissionerOverride = obj.optBoolean("enactedViaCommissionerOverride", false),
                        year = obj.optInt("year", 2026),
                        week = obj.optInt("week", 1)
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAllStarRedesignState(state: AllStarRedesignState) {
        val incObj = JSONObject()
        state.incentives.forEach { incObj.put(it.id, it.isEnabled) }
        val evObj = JSONObject()
        state.sideEvents.forEach { evObj.put(it.id, it.isSelected) }
        val recapArr = JSONArray()
        state.recapHistory.take(10).forEach { r ->
            recapArr.put(
                JSONObject().apply {
                    put("seasonYear", r.seasonYear)
                    put("formatType", r.formatType.name)
                    put("hostCityName", r.hostCityName)
                    put("headline", r.headline)
                    put("matchupScoreSummary", r.matchupScoreSummary)
                    put("mvpPlayerName", r.mvpPlayerName)
                    put("mvpTeamName", r.mvpTeamName)
                    put("mvpStatLine", r.mvpStatLine)
                    put("actualViewersMillions", r.actualViewersMillions)
                    put("viewershipRatingScore", r.viewershipRatingScore)
                    put("playerEffortGrade", r.playerEffortGrade)
                    put("playerEffortScore", r.playerEffortScore)
                    put("netRevenueMillions", r.netRevenueMillions)
                    put("fanbaseGainMillions", r.fanbaseGainMillions)
                    put("isDressRehearsal", r.isDressRehearsal)
                }
            )
        }
        val root = JSONObject().apply {
            put("selectedFormat", state.selectedFormat.name)
            put("hostCityId", state.hostCityId)
            put("hostCityName", state.hostCityName)
            put("consecutiveSameFormatYears", state.consecutiveSameFormatYears)
            put("isRedesignedAndPromoted", state.isRedesignedAndPromoted)
            put("incentivesMap", incObj)
            put("sideEventsMap", evObj)
            put("recapHistory", recapArr)
        }
        settingsPrefs.edit().putString("all_star_redesign_json", root.toString()).apply()
    }

    fun loadAllStarRedesignState(cities: List<MarketCity>): AllStarRedesignState? {
        val json = settingsPrefs.getString("all_star_redesign_json", null) ?: return null
        return try {
            val obj = JSONObject(json)
            val base = AllStarEngine.createDefaultState(cities)
            val fmt = runCatching {
                AllStarFormatType.valueOf(obj.optString("selectedFormat", base.selectedFormat.name))
            }.getOrDefault(base.selectedFormat)
            val incObj = obj.optJSONObject("incentivesMap")
            val updatedIncentives = base.incentives.map { inc ->
                if (incObj != null && incObj.has(inc.id)) inc.copy(isEnabled = incObj.optBoolean(inc.id, inc.isEnabled)) else inc
            }
            val evObj = obj.optJSONObject("sideEventsMap")
            val updatedEvents = base.sideEvents.map { ev ->
                if (evObj != null && evObj.has(ev.id)) ev.copy(isSelected = evObj.optBoolean(ev.id, ev.isSelected)) else ev
            }
            val recapArr = obj.optJSONArray("recapHistory") ?: JSONArray()
            val recaps = mutableListOf<AllStarShowcaseRecap>()
            for (i in 0 until recapArr.length()) {
                val rObj = recapArr.optJSONObject(i) ?: continue
                recaps.add(
                    AllStarShowcaseRecap(
                        seasonYear = rObj.optInt("seasonYear", 2026),
                        formatType = runCatching { AllStarFormatType.valueOf(rObj.optString("formatType")) }.getOrDefault(fmt),
                        hostCityName = rObj.optString("hostCityName", base.hostCityName),
                        headline = rObj.optString("headline", ""),
                        matchupScoreSummary = rObj.optString("matchupScoreSummary", ""),
                        mvpPlayerName = rObj.optString("mvpPlayerName", ""),
                        mvpTeamName = rObj.optString("mvpTeamName", ""),
                        mvpStatLine = rObj.optString("mvpStatLine", ""),
                        actualViewersMillions = rObj.optDouble("actualViewersMillions", 8.0),
                        viewershipRatingScore = rObj.optInt("viewershipRatingScore", 70),
                        playerEffortGrade = rObj.optString("playerEffortGrade", "B"),
                        playerEffortScore = rObj.optInt("playerEffortScore", 70),
                        netRevenueMillions = rObj.optLong("netRevenueMillions", 15L),
                        fanbaseGainMillions = rObj.optDouble("fanbaseGainMillions", 1.2),
                        isDressRehearsal = rObj.optBoolean("isDressRehearsal", false)
                    )
                )
            }
            AllStarRedesignState(
                selectedFormat = fmt,
                incentives = updatedIncentives,
                sideEvents = updatedEvents,
                hostCityId = obj.optString("hostCityId", base.hostCityId),
                hostCityName = obj.optString("hostCityName", base.hostCityName),
                consecutiveSameFormatYears = obj.optInt("consecutiveSameFormatYears", base.consecutiveSameFormatYears),
                isRedesignedAndPromoted = obj.optBoolean("isRedesignedAndPromoted", false),
                lastRecap = recaps.firstOrNull(),
                recapHistory = recaps
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveOfficiatingState(state: OfficiatingState) {
        val refsArr = JSONArray()
        state.referees.forEach { ref ->
            refsArr.put(
                JSONObject().apply {
                    put("id", ref.id)
                    put("name", ref.name)
                    put("yearsExperience", ref.yearsExperience)
                    put("tier", ref.tier.name)
                    put("archetype", ref.archetype.name)
                    put("accuracyRating", ref.accuracyRating)
                    put("composureRating", ref.composureRating)
                    put("fatigueLevel", ref.fatigueLevel)
                    put("blownCallsSeason", ref.blownCallsSeason)
                    put("gamesOfficiatedSeason", ref.gamesOfficiatedSeason)
                    put("isRestedOrBenched", ref.isRestedOrBenched)
                    put("benchWeeksRemaining", ref.benchWeeksRemaining)
                    put("isUnionSteward", ref.isUnionSteward)
                }
            )
        }
        val assignArr = JSONArray()
        state.assignments.forEach { a ->
            assignArr.put(
                JSONObject().apply {
                    put("matchId", a.matchId)
                    put("week", a.week)
                    put("homeTeamId", a.homeTeamId)
                    put("awayTeamId", a.awayTeamId)
                    put("matchupTitle", a.matchupTitle)
                    put("leadRefereeId", a.leadRefereeId)
                    put("leadRefereeName", a.leadRefereeName)
                    put("isMarqueeOrRivalry", a.isMarqueeOrRivalry)
                    put("stakesLabel", a.stakesLabel)
                    put("projectedBlownCallRiskPct", a.projectedBlownCallRiskPct)
                }
            )
        }
        val techArr = JSONArray()
        state.techCatalog.forEach { t ->
            techArr.put(
                JSONObject().apply {
                    put("type", t.type.name)
                    put("isInstalled", t.isInstalled)
                    t.installedYear?.let { put("installedYear", it) }
                }
            )
        }
        val incArr = JSONArray()
        state.incidents.take(20).forEach { inc ->
            incArr.put(
                JSONObject().apply {
                    put("id", inc.id)
                    put("week", inc.week)
                    put("seasonYear", inc.seasonYear)
                    put("matchId", inc.matchId)
                    put("matchupTitle", inc.matchupTitle)
                    put("finalScoreSummary", inc.finalScoreSummary)
                    put("offendingRefereeId", inc.offendingRefereeId)
                    put("offendingRefereeName", inc.offendingRefereeName)
                    put("affectedTeamId", inc.affectedTeamId)
                    put("affectedTeamName", inc.affectedTeamName)
                    put("benefitingTeamName", inc.benefitingTeamName)
                    put("callDescription", inc.callDescription)
                    put("severity", inc.severity.name)
                    put("fanOutragePct", inc.fanOutragePct)
                    put("integrityDamage", inc.integrityDamage)
                    put("isResolved", inc.isResolved)
                    inc.chosenResponseTitle?.let { put("chosenResponseTitle", it) }
                    inc.resolutionHeadline?.let { put("resolutionHeadline", it) }
                }
            )
        }
        val unionObj = JSONObject().apply {
            put("unionSatisfaction", state.unionState.unionSatisfaction)
            put("lockoutRisk", state.unionState.lockoutRisk)
            put("annualRetainerMillions", state.unionState.annualRetainerMillions)
            put("perGameBonusK", state.unionState.perGameBonusK)
            put("cbaYearsRemaining", state.unionState.cbaYearsRemaining)
            put("mandatoryRestThreshold", state.unionState.mandatoryRestThreshold)
            put("workloadProtectionActive", state.unionState.workloadProtectionActive)
            put("publicDisciplineProtection", state.unionState.publicDisciplineProtection)
            put("replacementRefsActive", state.unionState.replacementRefsActive)
        }
        val root = JSONObject().apply {
            put("referees", refsArr)
            put("assignments", assignArr)
            put("unionState", unionObj)
            put("techCatalog", techArr)
            put("incidents", incArr)
            put("leagueCallAccuracyPct", state.leagueCallAccuracyPct)
            put("totalCallsAudited", state.totalCallsAudited)
            put("totalBlownCalls", state.totalBlownCalls)
            put("lastAuditHeadline", state.lastAuditHeadline)
        }
        settingsPrefs.edit().putString("officiating_state_json", root.toString()).apply()
    }

    fun loadOfficiatingState(franchises: List<Franchise>, currentWeek: Int, matches: List<GameMatch>): OfficiatingState? {
        val json = settingsPrefs.getString("officiating_state_json", null) ?: return null
        return try {
            val root = JSONObject(json)
            val base = OfficiatingEngine.createDefaultState(franchises, currentWeek, matches)
            val refsArr = root.optJSONArray("referees")
            val refs = if (refsArr != null && refsArr.length() > 0) {
                (0 until refsArr.length()).mapNotNull { i ->
                    val o = refsArr.optJSONObject(i) ?: return@mapNotNull null
                    RefereeProfile(
                        id = o.optString("id", "ref_$i"),
                        name = o.optString("name", "Official $i"),
                        yearsExperience = o.optInt("yearsExperience", 5),
                        tier = runCatching { RefereeTier.valueOf(o.optString("tier")) }.getOrDefault(RefereeTier.SENIOR_OFFICIAL),
                        archetype = runCatching { RefereeArchetype.valueOf(o.optString("archetype")) }.getOrDefault(RefereeArchetype.BY_THE_BOOK),
                        accuracyRating = o.optInt("accuracyRating", 82),
                        composureRating = o.optInt("composureRating", 82),
                        fatigueLevel = o.optInt("fatigueLevel", 15),
                        blownCallsSeason = o.optInt("blownCallsSeason", 0),
                        gamesOfficiatedSeason = o.optInt("gamesOfficiatedSeason", 0),
                        isRestedOrBenched = o.optBoolean("isRestedOrBenched", false),
                        benchWeeksRemaining = o.optInt("benchWeeksRemaining", 0),
                        isUnionSteward = o.optBoolean("isUnionSteward", false)
                    )
                }
            } else base.referees

            val assignArr = root.optJSONArray("assignments")
            val assignments = if (assignArr != null && assignArr.length() > 0) {
                (0 until assignArr.length()).mapNotNull { i ->
                    val o = assignArr.optJSONObject(i) ?: return@mapNotNull null
                    RefereeAssignment(
                        matchId = o.optString("matchId"),
                        week = o.optInt("week", currentWeek),
                        homeTeamId = o.optString("homeTeamId"),
                        awayTeamId = o.optString("awayTeamId"),
                        matchupTitle = o.optString("matchupTitle"),
                        leadRefereeId = o.optString("leadRefereeId"),
                        leadRefereeName = o.optString("leadRefereeName"),
                        isMarqueeOrRivalry = o.optBoolean("isMarqueeOrRivalry", false),
                        stakesLabel = o.optString("stakesLabel", "Regular Slate"),
                        projectedBlownCallRiskPct = o.optInt("projectedBlownCallRiskPct", 14)
                    )
                }
            } else base.assignments

            val uObj = root.optJSONObject("unionState")
            val union = if (uObj != null) {
                RefereeUnionState(
                    unionSatisfaction = uObj.optInt("unionSatisfaction", 72),
                    lockoutRisk = uObj.optInt("lockoutRisk", 18),
                    annualRetainerMillions = uObj.optLong("annualRetainerMillions", 8L),
                    perGameBonusK = uObj.optInt("perGameBonusK", 15),
                    cbaYearsRemaining = uObj.optInt("cbaYearsRemaining", 2),
                    mandatoryRestThreshold = uObj.optInt("mandatoryRestThreshold", 65),
                    workloadProtectionActive = uObj.optBoolean("workloadProtectionActive", true),
                    publicDisciplineProtection = uObj.optBoolean("publicDisciplineProtection", false),
                    replacementRefsActive = uObj.optBoolean("replacementRefsActive", false)
                )
            } else base.unionState

            val techArr = root.optJSONArray("techCatalog")
            val techByType = mutableMapOf<OfficiatingTechType, Pair<Boolean, Int?>>()
            if (techArr != null) {
                for (i in 0 until techArr.length()) {
                    val o = techArr.optJSONObject(i) ?: continue
                    val t = runCatching { OfficiatingTechType.valueOf(o.optString("type")) }.getOrNull() ?: continue
                    val yr = if (o.has("installedYear")) o.optInt("installedYear") else null
                    techByType[t] = o.optBoolean("isInstalled", false) to yr
                }
            }
            val techCatalog = base.techCatalog.map { item ->
                val savedItem = techByType[item.type]
                if (savedItem != null) item.copy(isInstalled = savedItem.first, installedYear = savedItem.second) else item
            }

            val incArr = root.optJSONArray("incidents") ?: JSONArray()
            val incidents = (0 until incArr.length()).mapNotNull { i ->
                val o = incArr.optJSONObject(i) ?: return@mapNotNull null
                BlownCallIncident(
                    id = o.optString("id"),
                    week = o.optInt("week", 1),
                    seasonYear = o.optInt("seasonYear", 2026),
                    matchId = o.optString("matchId"),
                    matchupTitle = o.optString("matchupTitle"),
                    finalScoreSummary = o.optString("finalScoreSummary"),
                    offendingRefereeId = o.optString("offendingRefereeId"),
                    offendingRefereeName = o.optString("offendingRefereeName"),
                    affectedTeamId = o.optString("affectedTeamId"),
                    affectedTeamName = o.optString("affectedTeamName"),
                    benefitingTeamName = o.optString("benefitingTeamName"),
                    callDescription = o.optString("callDescription"),
                    severity = runCatching { BlownCallSeverity.valueOf(o.optString("severity")) }.getOrDefault(BlownCallSeverity.MODERATE),
                    fanOutragePct = o.optInt("fanOutragePct", 45),
                    integrityDamage = o.optInt("integrityDamage", 4),
                    isResolved = o.optBoolean("isResolved", false),
                    chosenResponseTitle = o.optString("chosenResponseTitle", "").ifBlank { null },
                    resolutionHeadline = o.optString("resolutionHeadline", "").ifBlank { null }
                )
            }

            OfficiatingState(
                referees = refs,
                assignments = assignments,
                unionState = union,
                techCatalog = techCatalog,
                incidents = incidents,
                leagueCallAccuracyPct = root.optDouble("leagueCallAccuracyPct", 92.8),
                totalCallsAudited = root.optInt("totalCallsAudited", 180),
                totalBlownCalls = root.optInt("totalBlownCalls", 13),
                lastAuditHeadline = root.optString("lastAuditHeadline", base.lastAuditHeadline)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveRuleCommitteeState(state: RuleCommitteeState) {
        val proposalsArr = JSONArray()
        state.proposals.forEach { p ->
            proposalsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("sportType", p.sportType.name)
                    put("title", p.title)
                    put("categoryLabel", p.categoryLabel)
                    put("sponsorName", p.sponsorName)
                    put("focusLever", p.focusLever.name)
                    put("summary", p.summary)
                    put("simulationEffectSummary", p.simulationEffectSummary)
                    put("proPerspective", p.proPerspective)
                    put("contraPerspective", p.contraPerspective)
                    put("scoringDeltaPerTeam", p.scoringDeltaPerTeam)
                    put("varianceDelta", p.varianceDelta)
                    put("injuryRiskMultiplier", p.injuryRiskMultiplier)
                    put("closeGameSwingPct", p.closeGameSwingPct)
                    put("convertDrawsToShootout", p.convertDrawsToShootout)
                    put("suppressBlownCallSwing", p.suppressBlownCallSwing)
                    put("underdogBoost", p.underdogBoost)
                    put("defenseStatMultiplier", p.defenseStatMultiplier)
                    put("reboundStatMultiplier", p.reboundStatMultiplier)
                    put("integrityDelta", p.integrityDelta)
                    put("parityDelta", p.parityDelta)
                    put("fanAppealDelta", p.fanAppealDelta)
                    put("status", p.status.name)
                    put("committeeVotesFor", p.committeeVotesFor)
                    put("committeeVotesAgainst", p.committeeVotesAgainst)
                    put("committeeReviewNote", p.committeeReviewNote)
                    put("isCustomProposal", p.isCustomProposal)
                }
            )
        }
        val logsArr = JSONArray()
        state.impactLogs.take(24).forEach { log ->
            logsArr.put(
                JSONObject().apply {
                    put("id", log.id)
                    put("seasonYear", log.seasonYear)
                    put("weekNumber", log.weekNumber)
                    put("sportType", log.sportType.name)
                    put("activeRulesCount", log.activeRulesCount)
                    put("activeRuleTitles", JSONArray(log.activeRuleTitles))
                    put("measuredAvgCombinedScore", log.measuredAvgCombinedScore)
                    put("sportBaselineCombinedScore", log.sportBaselineCombinedScore)
                    put("closeOrOvertimeGamesCount", log.closeOrOvertimeGamesCount)
                    put("injuriesOccurredCount", log.injuriesOccurredCount)
                    put("featuredImpactHeadline", log.featuredImpactHeadline)
                }
            )
        }
        val root = JSONObject().apply {
            put("sportType", state.sportType.name)
            put("committeeApprovalRating", state.committeeApprovalRating)
            put("proposals", proposalsArr)
            put("impactLogs", logsArr)
        }
        settingsPrefs.edit().putString("rule_committee_state_json", root.toString()).apply()
    }

    fun loadRuleCommitteeState(expectedSport: SportType): RuleCommitteeState? {
        val json = settingsPrefs.getString("rule_committee_state_json", null) ?: return null
        return try {
            val root = JSONObject(json)
            val savedSport = runCatching {
                SportType.valueOf(root.optString("sportType", expectedSport.name))
            }.getOrDefault(expectedSport)
            if (savedSport != expectedSport) return null

            val base = RuleCommitteeEngine.createDefaultState(expectedSport)
            val propArr = root.optJSONArray("proposals") ?: JSONArray()
            val proposals = if (propArr.length() > 0) {
                (0 until propArr.length()).mapNotNull { i ->
                    val o = propArr.optJSONObject(i) ?: return@mapNotNull null
                    SportRuleAmendment(
                        id = o.optString("id"),
                        sportType = expectedSport,
                        title = o.optString("title"),
                        categoryLabel = o.optString("categoryLabel", "Competition"),
                        sponsorName = o.optString("sponsorName", "Rule Committee"),
                        focusLever = runCatching {
                            RuleCommitteeFocusLever.valueOf(o.optString("focusLever"))
                        }.getOrDefault(RuleCommitteeFocusLever.OFFENSIVE_TEMPO),
                        summary = o.optString("summary"),
                        simulationEffectSummary = o.optString("simulationEffectSummary"),
                        proPerspective = o.optString("proPerspective"),
                        contraPerspective = o.optString("contraPerspective"),
                        scoringDeltaPerTeam = o.optDouble("scoringDeltaPerTeam", 0.0),
                        varianceDelta = o.optInt("varianceDelta", 0),
                        injuryRiskMultiplier = o.optDouble("injuryRiskMultiplier", 1.0),
                        closeGameSwingPct = o.optInt("closeGameSwingPct", 0),
                        convertDrawsToShootout = o.optBoolean("convertDrawsToShootout", false),
                        suppressBlownCallSwing = o.optBoolean("suppressBlownCallSwing", false),
                        underdogBoost = o.optInt("underdogBoost", 0),
                        defenseStatMultiplier = o.optDouble("defenseStatMultiplier", 1.0),
                        reboundStatMultiplier = o.optDouble("reboundStatMultiplier", 1.0),
                        integrityDelta = o.optInt("integrityDelta", 0),
                        parityDelta = o.optInt("parityDelta", 0),
                        fanAppealDelta = o.optInt("fanAppealDelta", 0),
                        status = runCatching {
                            SportRuleProposalStatus.valueOf(o.optString("status"))
                        }.getOrDefault(SportRuleProposalStatus.PROPOSED_DOCKET),
                        committeeVotesFor = o.optInt("committeeVotesFor", 0),
                        committeeVotesAgainst = o.optInt("committeeVotesAgainst", 0),
                        committeeReviewNote = o.optString("committeeReviewNote", ""),
                        isCustomProposal = o.optBoolean("isCustomProposal", false)
                    )
                }
            } else base.proposals

            val logArr = root.optJSONArray("impactLogs") ?: JSONArray()
            val logs = (0 until logArr.length()).mapNotNull { i ->
                val o = logArr.optJSONObject(i) ?: return@mapNotNull null
                val titlesArr = o.optJSONArray("activeRuleTitles") ?: JSONArray()
                val titles = (0 until titlesArr.length()).mapNotNull { idx -> titlesArr.optString(idx) }
                RuleSimulationImpactLog(
                    id = o.optString("id"),
                    seasonYear = o.optInt("seasonYear", 2026),
                    weekNumber = o.optInt("weekNumber", 1),
                    sportType = expectedSport,
                    activeRulesCount = o.optInt("activeRulesCount", 0),
                    activeRuleTitles = titles,
                    measuredAvgCombinedScore = o.optDouble("measuredAvgCombinedScore", RuleCommitteeEngine.sportBaselineCombinedScore(expectedSport)),
                    sportBaselineCombinedScore = o.optDouble("sportBaselineCombinedScore", RuleCommitteeEngine.sportBaselineCombinedScore(expectedSport)),
                    closeOrOvertimeGamesCount = o.optInt("closeOrOvertimeGamesCount", 0),
                    injuriesOccurredCount = o.optInt("injuriesOccurredCount", 0),
                    featuredImpactHeadline = o.optString("featuredImpactHeadline", "")
                )
            }

            base.copy(
                proposals = proposals,
                impactLogs = logs,
                committeeApprovalRating = root.optInt("committeeApprovalRating", 82)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveDisciplinaryState(state: DisciplinaryDeskState) {
        val root = JSONObject().apply {
            put("sportType", state.sportType.name)
            put("precedentConsistencyScore", state.precedentConsistencyScore)
            put("totalFinesCollectedThousands", state.totalFinesCollectedThousands)
            put("totalSuspensionsIssuedGames", state.totalSuspensionsIssuedGames)
            put("landmarkPrecedentsCount", state.landmarkPrecedentsCount)
            put("nextCaseSequence", state.nextCaseSequence)

            val precArr = JSONArray()
            state.precedents.forEach { std ->
                precArr.put(
                    JSONObject().apply {
                        put("category", std.category.name)
                        put("benchmarkGames", std.benchmarkGames)
                        put("benchmarkFineThousands", std.benchmarkFineThousands)
                        put("repeatOffenderMultiplier", std.repeatOffenderMultiplier)
                        put("posture", std.posture.name)
                        put("priorRulingsCount", std.priorRulingsCount)
                        put("lastCaseCitation", std.lastCaseCitation)
                        put("landmarkNote", std.landmarkNote)
                    }
                )
            }
            put("precedents", precArr)

            val incArr = JSONArray()
            state.incidents.take(80).forEach { inc ->
                incArr.put(
                    JSONObject().apply {
                        put("id", inc.id)
                        put("caseCode", inc.caseCode)
                        put("sportType", inc.sportType.name)
                        put("category", inc.category.name)
                        put("severity", inc.severity.name)
                        put("playerId", inc.playerId)
                        put("playerName", inc.playerName)
                        put("playerPosition", inc.playerPosition)
                        put("playerOverall", inc.playerOverall)
                        put("franchiseId", inc.franchiseId)
                        put("franchiseName", inc.franchiseName)
                        put("franchiseColorHex", inc.franchiseColorHex)
                        put("opponentName", inc.opponentName)
                        put("seasonYear", inc.seasonYear)
                        put("weekNumber", inc.weekNumber)
                        put("priorOffensesCount", inc.priorOffensesCount)
                        put("incidentTitle", inc.incidentTitle)
                        put("incidentSummary", inc.incidentSummary)
                        put("evidenceSummary", inc.evidenceSummary)
                        put("precedentCitation", inc.precedentCitation)
                        put("recommendedGames", inc.recommendedGames)
                        put("recommendedFineThousands", inc.recommendedFineThousands)
                        put("status", inc.status.name)
                        inc.appealBrief?.let { put("appealBrief", it) }
                        inc.ruling?.let { r ->
                            put(
                                "ruling",
                                JSONObject().apply {
                                    put("suspensionGames", r.suspensionGames)
                                    put("fineThousands", r.fineThousands)
                                    put("issuedYear", r.issuedYear)
                                    put("issuedWeek", r.issuedWeek)
                                    put("setNewPrecedent", r.setNewPrecedent)
                                    put("alignmentTier", r.alignmentTier.name)
                                    put("deviationSummary", r.deviationSummary)
                                    put("consistencyDelta", r.consistencyDelta)
                                    put("appealRiskPct", r.appealRiskPct)
                                    put("commissionerNote", r.commissionerNote)
                                    put("appealOutcomeSummary", r.appealOutcomeSummary)
                                }
                            )
                        }
                    }
                )
            }
            put("incidents", incArr)
        }
        settingsPrefs.edit().putString("disciplinary_desk_state_json", root.toString()).apply()
    }

    fun loadDisciplinaryState(
        expectedSport: SportType,
        ruleBook: LeagueRuleBook = LeagueRuleBook()
    ): DisciplinaryDeskState? {
        val json = settingsPrefs.getString("disciplinary_desk_state_json", null) ?: return null
        return try {
            val root = JSONObject(json)
            val savedSport = runCatching {
                SportType.valueOf(root.optString("sportType", expectedSport.name))
            }.getOrDefault(expectedSport)
            if (savedSport != expectedSport) return null

            val defaultPrecedents = DisciplinaryEngine.createDefaultPrecedents(ruleBook)
            val precArr = root.optJSONArray("precedents") ?: JSONArray()
            val precedents = if (precArr.length() > 0) {
                (0 until precArr.length()).mapNotNull { i ->
                    val o = precArr.optJSONObject(i) ?: return@mapNotNull null
                    val cat = runCatching { DisciplinaryCategory.valueOf(o.optString("category")) }.getOrNull()
                        ?: return@mapNotNull null
                    PrecedentStandard(
                        category = cat,
                        benchmarkGames = o.optInt("benchmarkGames", cat.defaultGames),
                        benchmarkFineThousands = o.optInt("benchmarkFineThousands", cat.defaultFineThousands),
                        repeatOffenderMultiplier = o.optDouble("repeatOffenderMultiplier", 1.5),
                        posture = runCatching { PrecedentPosture.valueOf(o.optString("posture")) }.getOrDefault(PrecedentPosture.BALANCED),
                        priorRulingsCount = o.optInt("priorRulingsCount", 2),
                        lastCaseCitation = o.optString("lastCaseCitation", "Charter Standard"),
                        landmarkNote = o.optString("landmarkNote", "")
                    )
                }.ifEmpty { defaultPrecedents }
            } else defaultPrecedents

            val incArr = root.optJSONArray("incidents") ?: JSONArray()
            val incidents = (0 until incArr.length()).mapNotNull { i ->
                val o = incArr.optJSONObject(i) ?: return@mapNotNull null
                val cat = runCatching { DisciplinaryCategory.valueOf(o.optString("category")) }
                    .getOrDefault(DisciplinaryCategory.FLAGRANT_FOUL)
                val sev = runCatching { InfractionSeverity.valueOf(o.optString("severity")) }
                    .getOrDefault(InfractionSeverity.MODERATE)
                val status = runCatching { DisciplinaryStatus.valueOf(o.optString("status")) }
                    .getOrDefault(DisciplinaryStatus.PENDING_HEARING)
                val rObj = o.optJSONObject("ruling")
                val ruling = rObj?.let { r ->
                    DisciplinaryRuling(
                        suspensionGames = r.optInt("suspensionGames", 0),
                        fineThousands = r.optInt("fineThousands", 0),
                        issuedYear = r.optInt("issuedYear", 2026),
                        issuedWeek = r.optInt("issuedWeek", 1),
                        setNewPrecedent = r.optBoolean("setNewPrecedent", false),
                        alignmentTier = runCatching { PrecedentAlignmentTier.valueOf(r.optString("alignmentTier")) }
                            .getOrDefault(PrecedentAlignmentTier.ALIGNED),
                        deviationSummary = r.optString("deviationSummary", ""),
                        consistencyDelta = r.optInt("consistencyDelta", 0),
                        appealRiskPct = r.optInt("appealRiskPct", 10),
                        commissionerNote = r.optString("commissionerNote", ""),
                        appealOutcomeSummary = r.optString("appealOutcomeSummary", "")
                    )
                }
                DisciplinaryIncident(
                    id = o.optString("id"),
                    caseCode = o.optString("caseCode", "CASE-2026"),
                    sportType = expectedSport,
                    category = cat,
                    severity = sev,
                    playerId = o.optString("playerId"),
                    playerName = o.optString("playerName"),
                    playerPosition = o.optString("playerPosition", "F"),
                    playerOverall = o.optInt("playerOverall", 78),
                    franchiseId = o.optString("franchiseId"),
                    franchiseName = o.optString("franchiseName"),
                    franchiseColorHex = o.optLong("franchiseColorHex", 0xFFF59E0B),
                    opponentName = o.optString("opponentName", "League Rival"),
                    seasonYear = o.optInt("seasonYear", 2026),
                    weekNumber = o.optInt("weekNumber", 1),
                    priorOffensesCount = o.optInt("priorOffensesCount", 0),
                    incidentTitle = o.optString("incidentTitle"),
                    incidentSummary = o.optString("incidentSummary"),
                    evidenceSummary = o.optString("evidenceSummary"),
                    precedentCitation = o.optString("precedentCitation"),
                    recommendedGames = o.optInt("recommendedGames", cat.defaultGames),
                    recommendedFineThousands = o.optInt("recommendedFineThousands", cat.defaultFineThousands),
                    status = status,
                    ruling = ruling,
                    appealBrief = o.optString("appealBrief", "").ifBlank { null }
                )
            }

            DisciplinaryDeskState(
                sportType = expectedSport,
                incidents = incidents,
                precedents = precedents,
                precedentConsistencyScore = root.optInt("precedentConsistencyScore", 86).coerceIn(20, 100),
                totalFinesCollectedThousands = root.optLong("totalFinesCollectedThousands", 0L),
                totalSuspensionsIssuedGames = root.optInt("totalSuspensionsIssuedGames", 0),
                landmarkPrecedentsCount = root.optInt("landmarkPrecedentsCount", 0),
                nextCaseSequence = root.optInt("nextCaseSequence", 104)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveCrisisState(state: CrisisSystemState) {
        val root = JSONObject().apply {
            state.lastTriggeredArcId?.let { put("lastTriggeredArcId", it) }
            put("completedArcIds", JSONArray(state.completedArcIds.toList()))

            val schedArr = JSONArray()
            state.scheduledBranches.forEach { b ->
                schedArr.put(
                    JSONObject().apply {
                        put("id", b.id)
                        put("arcId", b.arcId)
                        put("archetype", b.archetype.name)
                        put("followUpNodeId", b.followUpNodeId)
                        put("triggerYear", b.triggerYear)
                        put("triggerWeek", b.triggerWeek)
                        put("stageNumber", b.stageNumber)
                        put("totalStages", b.totalStages)
                        put("teaserLabel", b.teaserLabel)
                        put("priorDecisionLabel", b.priorDecisionLabel)
                        b.targetFranchiseId?.let { put("targetFranchiseId", it) }
                        b.targetFranchiseName?.let { put("targetFranchiseName", it) }
                        b.targetPlayerId?.let { put("targetPlayerId", it) }
                        b.targetPlayerName?.let { put("targetPlayerName", it) }
                    }
                )
            }
            put("scheduledBranches", schedArr)

            val histArr = JSONArray()
            state.resolvedHistory.take(40).forEach { rec ->
                histArr.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("arcId", rec.arcId)
                        put("archetype", rec.archetype.name)
                        put("stageNumber", rec.stageNumber)
                        put("totalStages", rec.totalStages)
                        put("seasonYear", rec.seasonYear)
                        put("weekNumber", rec.weekNumber)
                        put("title", rec.title)
                        put("chosenLabel", rec.chosenLabel)
                        put("narrativeOutcome", rec.narrativeOutcome)
                        put("consequenceTags", JSONArray(rec.consequenceTags))
                        put("affectedEntityLabel", rec.affectedEntityLabel)
                        rec.spawnedBranchTeaser?.let { put("spawnedBranchTeaser", it) }
                    }
                )
            }
            put("resolvedHistory", histArr)
        }
        settingsPrefs.edit().putString("crisis_narrative_state_json", root.toString()).apply()
    }

    fun loadCrisisState(): CrisisSystemState? {
        val json = settingsPrefs.getString("crisis_narrative_state_json", null) ?: return null
        return try {
            val root = JSONObject(json)
            val completedArr = root.optJSONArray("completedArcIds") ?: JSONArray()
            val completed = (0 until completedArr.length()).mapNotNull { i ->
                completedArr.optString(i).takeIf { it.isNotBlank() }
            }.toSet()

            val schedArr = root.optJSONArray("scheduledBranches") ?: JSONArray()
            val branches = (0 until schedArr.length()).mapNotNull { i ->
                val o = schedArr.optJSONObject(i) ?: return@mapNotNull null
                val arch = runCatching {
                    CrisisArchetype.valueOf(o.optString("archetype"))
                }.getOrDefault(CrisisArchetype.SPORTSBOOK_INTEGRITY)
                ScheduledCrisisBranch(
                    id = o.optString("id"),
                    arcId = o.optString("arcId"),
                    archetype = arch,
                    followUpNodeId = o.optString("followUpNodeId"),
                    triggerYear = o.optInt("triggerYear", 2026),
                    triggerWeek = o.optInt("triggerWeek", 2),
                    stageNumber = o.optInt("stageNumber", 2),
                    totalStages = o.optInt("totalStages", 3),
                    teaserLabel = o.optString("teaserLabel", ""),
                    priorDecisionLabel = o.optString("priorDecisionLabel", ""),
                    targetFranchiseId = o.optString("targetFranchiseId", "").ifBlank { null },
                    targetFranchiseName = o.optString("targetFranchiseName", "").ifBlank { null },
                    targetPlayerId = o.optString("targetPlayerId", "").ifBlank { null },
                    targetPlayerName = o.optString("targetPlayerName", "").ifBlank { null }
                )
            }

            val histArr = root.optJSONArray("resolvedHistory") ?: JSONArray()
            val history = (0 until histArr.length()).mapNotNull { i ->
                val o = histArr.optJSONObject(i) ?: return@mapNotNull null
                val arch = runCatching {
                    CrisisArchetype.valueOf(o.optString("archetype"))
                }.getOrDefault(CrisisArchetype.SPORTSBOOK_INTEGRITY)
                val tagsArr = o.optJSONArray("consequenceTags") ?: JSONArray()
                val tags = (0 until tagsArr.length()).mapNotNull { idx ->
                    tagsArr.optString(idx).takeIf { it.isNotBlank() }
                }
                ResolvedCrisisRecord(
                    id = o.optString("id"),
                    arcId = o.optString("arcId"),
                    archetype = arch,
                    stageNumber = o.optInt("stageNumber", 1),
                    totalStages = o.optInt("totalStages", 3),
                    seasonYear = o.optInt("seasonYear", 2026),
                    weekNumber = o.optInt("weekNumber", 1),
                    title = o.optString("title"),
                    chosenLabel = o.optString("chosenLabel"),
                    narrativeOutcome = o.optString("narrativeOutcome"),
                    consequenceTags = tags,
                    affectedEntityLabel = o.optString("affectedEntityLabel", ""),
                    spawnedBranchTeaser = o.optString("spawnedBranchTeaser", "").ifBlank { null }
                )
            }

            CrisisSystemState(
                scheduledBranches = branches,
                resolvedHistory = history,
                completedArcIds = completed,
                lastTriggeredArcId = root.optString("lastTriggeredArcId", "").ifBlank { null }
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveFreeAgencyState(
        pool: List<FreeAgentPlayer>,
        transactions: List<FreeAgentSigningTransaction>
    ) {
        faPrefs.edit()
            .putString("fa_pool_json", serializeFreeAgents(pool))
            .putString("fa_tx_json", serializeFreeAgentTransactions(transactions))
            .apply()
    }

    fun loadFreeAgencyState(): Pair<List<FreeAgentPlayer>, List<FreeAgentSigningTransaction>>? {
        val poolJson = faPrefs.getString("fa_pool_json", null) ?: return null
        val txJson = faPrefs.getString("fa_tx_json", "[]") ?: "[]"
        return deserializeFreeAgents(poolJson) to deserializeFreeAgentTransactions(txJson)
    }

    fun saveLeagueSettings(ruleBook: LeagueRuleBook) {
        val obj = JSONObject().apply {
            put("overtimeFormat", ruleBook.overtimeFormat.name)
            put("rosterSizeRule", ruleBook.rosterSizeRule.name)
            put("salaryCapStrictness", ruleBook.salaryCapStrictness.name)
            put("injuryFrequency", ruleBook.injuryFrequency.name)
            put("gameLength", ruleBook.gameLength.name)
            put("antiDopingPolicy", ruleBook.antiDopingPolicy.name)
            put("antiTamperingGamblingPolicy", ruleBook.antiTamperingGamblingPolicy.name)
            put("draftParity", ruleBook.draftParity.name)
            put("instantReplayEnabled", ruleBook.instantReplayEnabled)
            put("shotClockFastPace", ruleBook.shotClockFastPace)
            put("regularSeasonWeeks", ruleBook.regularSeasonWeeks)
            val enactedObj = JSONObject()
            ruleBook.rules.forEach { r ->
                enactedObj.put(r.id, r.isEnacted)
            }
            put("rulesEnactedMap", enactedObj)
        }
        settingsPrefs.edit()
            .putString("rule_settings_json", obj.toString())
            .apply()
    }

    fun loadLeagueSettings(baseRuleBook: LeagueRuleBook = LeagueRuleBook(rules = InitialLeagueData.getDefaultLeagueRules())): LeagueRuleBook? {
        val json = settingsPrefs.getString("rule_settings_json", null) ?: return null
        return try {
            val obj = JSONObject(json)
            val overtime = runCatching {
                OvertimeFormat.valueOf(obj.optString("overtimeFormat", baseRuleBook.overtimeFormat.name))
            }.getOrDefault(baseRuleBook.overtimeFormat)
            val roster = runCatching {
                RosterSizeRule.valueOf(obj.optString("rosterSizeRule", baseRuleBook.rosterSizeRule.name))
            }.getOrDefault(baseRuleBook.rosterSizeRule)
            val strictness = runCatching {
                SalaryCapStrictness.valueOf(obj.optString("salaryCapStrictness", baseRuleBook.salaryCapStrictness.name))
            }.getOrDefault(baseRuleBook.salaryCapStrictness)
            val injuryFreq = runCatching {
                InjuryFrequencyRule.valueOf(obj.optString("injuryFrequency", baseRuleBook.injuryFrequency.name))
            }.getOrDefault(baseRuleBook.injuryFrequency)
            val gameLen = runCatching {
                GameLengthRule.valueOf(obj.optString("gameLength", baseRuleBook.gameLength.name))
            }.getOrDefault(baseRuleBook.gameLength)
            val antiDoping = runCatching {
                PolicyStrictness.valueOf(obj.optString("antiDopingPolicy", baseRuleBook.antiDopingPolicy.name))
            }.getOrDefault(baseRuleBook.antiDopingPolicy)
            val antiGambling = runCatching {
                PolicyStrictness.valueOf(obj.optString("antiTamperingGamblingPolicy", baseRuleBook.antiTamperingGamblingPolicy.name))
            }.getOrDefault(baseRuleBook.antiTamperingGamblingPolicy)
            val draftParity = runCatching {
                DraftParitySystem.valueOf(obj.optString("draftParity", baseRuleBook.draftParity.name))
            }.getOrDefault(baseRuleBook.draftParity)
            val replay = obj.optBoolean("instantReplayEnabled", baseRuleBook.instantReplayEnabled)
            val shotClock = obj.optBoolean("shotClockFastPace", baseRuleBook.shotClockFastPace)
            val regularSeasonWeeks = obj.optInt("regularSeasonWeeks", baseRuleBook.regularSeasonWeeks).coerceIn(14, 22)
            val enactedObj = obj.optJSONObject("rulesEnactedMap")
            val sourceRules = baseRuleBook.rules.ifEmpty { InitialLeagueData.getDefaultLeagueRules() }
            val updatedRules = if (enactedObj != null) {
                sourceRules.map { rule ->
                    if (enactedObj.has(rule.id)) {
                        rule.copy(isEnacted = enactedObj.optBoolean(rule.id, rule.isEnacted))
                    } else rule
                }
            } else sourceRules

            baseRuleBook.copy(
                rules = updatedRules,
                overtimeFormat = overtime,
                rosterSizeRule = roster,
                salaryCapStrictness = strictness,
                injuryFrequency = injuryFreq,
                gameLength = gameLen,
                antiDopingPolicy = antiDoping,
                antiTamperingGamblingPolicy = antiGambling,
                draftParity = draftParity,
                instantReplayEnabled = replay,
                shotClockFastPace = shotClock,
                regularSeasonWeeks = regularSeasonWeeks
            )
        } catch (_: Exception) {
            null
        }
    }

    // JSON Serializers
    fun serializeFranchises(list: List<Franchise>): String {
        val array = JSONArray()
        list.forEach { f ->
            val obj = JSONObject().apply {
                put("id", f.id)
                put("cityId", f.cityId)
                put("cityName", f.cityName)
                put("nickname", f.nickname)
                put("colorHex", f.colorHex)
                put("secondaryColorHex", f.secondaryColorHex)
                put("rosterRating", f.rosterRating)
                put("fanBaseMillions", f.fanBaseMillions)
                put("fanMorale", f.fanMorale)
                put("stadiumCapacity", f.stadiumCapacity)
                put("stadiumAge", f.stadiumAge)
                put("stadiumCondition", f.stadiumCondition)
                put("stadiumSponsorName", f.stadium.sponsorName ?: "")
                put("stadiumSponsorTier", f.stadium.sponsorTier)
                put("stadiumSponsorYearsLeft", f.stadium.sponsorYearsLeft)
                put("stadiumNamingRightsRevenue", f.stadium.namingRightsRevenue)
                put("relocationCount", f.relocationCount)
                put("lowSpendingSeasons", f.lowSpendingSeasons)
                put("reputationScore", f.reputationScore)
                put("lastReputationNote", f.lastReputationNote)
                put("ticketPrice", f.ticketPrice)
                put("annualPayrollMillions", f.annualPayrollMillions)
                put("localSponsorRevenueMillions", f.localSponsorRevenueMillions)
                put("wins", f.wins)
                put("losses", f.losses)
                put("pointsFor", f.pointsFor)
                put("pointsAgainst", f.pointsAgainst)
                put("championships", f.championships)
                put("conference", f.conference.name)
                put("division", f.division.name)
                put("ownerGroupId", f.ownership.id)
                put("ownerName", f.ownership.name)
                put("ownerType", f.ownership.type.name)
                put("ownerNetWorth", f.ownership.netWorthBillions)
                put("ownerApproval", f.ownership.ownerApproval)
                put("primaryOwnerId", f.ownership.primaryOwner.id)
                put("primaryOwnerName", f.ownership.primaryOwner.fullName)
                put("primaryOwnerRole", f.ownership.primaryOwner.roleTitle)
                put("primaryOwnerBio", f.ownership.primaryOwner.backgroundBio)
                put("primaryOwnerArchetype", f.ownership.primaryOwner.archetype.name)
                put("primaryOwnerSeatWeight", f.ownership.primaryOwner.seatWeight)
                put("fanLoyalty", f.fanLoyalty)
                f.activeFranchiseCampaign?.let { camp ->
                    put("activeCampaignType", camp.type.name)
                    put("activeCampaignWeeks", camp.remainingWeeks)
                    put("activeCampaignGained", camp.totalLoyaltyGained)
                }
                put("facilityStadiumTier", f.facilities.stadiumTier)
                put("facilityTrainingTier", f.facilities.trainingTier)
                put("facilitySuitesTier", f.facilities.luxurySuitesTier)
                put("facilityInvested", f.facilities.cumulativeInvestedMillions)
                put("facilityAccumulatedValue", f.facilities.accumulatedFacilityValueMillions)
                put("facilityCompletedCount", f.facilities.completedUpgradesCount)
                val projectsArray = JSONArray()
                f.facilities.ongoingProjects.forEach { proj ->
                    projectsArray.put(
                        JSONObject().apply {
                            put("id", proj.id)
                            put("franchiseId", proj.franchiseId)
                            put("type", proj.type.name)
                            put("targetTier", proj.targetTier)
                            put("projectName", proj.projectName)
                            put("costMillions", proj.costMillions)
                            put("totalWeeks", proj.totalWeeks)
                            put("remainingWeeks", proj.remainingWeeks)
                            put("seatingCapacityGain", proj.seatingCapacityGain)
                            put("annualSponsorGainMillions", proj.annualSponsorGainMillions)
                            put("rosterOvrGain", proj.rosterOvrGain)
                            put("valuationBoostMillions", proj.valuationBoostMillions)
                            put("loyaltyGain", proj.loyaltyGain)
                        }
                    )
                }
                put("facilityProjects", projectsArray)
                val rosterArray = JSONArray()
                f.roster.forEach { p ->
                    rosterArray.put(
                        JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("franchiseId", p.franchiseId)
                            put("position", p.position)
                            put("overallRating", p.overallRating)
                            put("age", p.age)
                            put("salaryMillions", p.salaryMillions)
                            put("contractYearsRemaining", p.contractYearsRemaining)
                            put("archetype", p.archetype)
                            put("depthOrder", p.depthOrder)
                            put("isStarter", p.isStarter)
                            put("isAssignedToMinorLeague", p.isAssignedToMinorLeague)
                            put("minorLeagueOvrProgress", p.minorLeagueOvrProgress)
                            put("suspendedGamesRemaining", p.suspendedGamesRemaining)
                        }
                    )
                }
                put("roster", rosterArray)
                val picksArray = JSONArray()
                f.draftPicks.forEach { pk ->
                    picksArray.put(
                        JSONObject().apply {
                            put("id", pk.id)
                            put("year", pk.year)
                            put("round", pk.round)
                            put("originalFranchiseId", pk.originalFranchiseId)
                            put("originalFranchiseName", pk.originalFranchiseName)
                            put("currentFranchiseId", pk.currentFranchiseId)
                            put("isProtected", pk.isProtected)
                            put("protectionDescription", pk.protectionDescription)
                            pk.acquiredFromFranchiseId?.let { put("acquiredFromFranchiseId", it) }
                            pk.acquiredFromFranchiseName?.let { put("acquiredFromFranchiseName", it) }
                            pk.acquiredInWeek?.let { put("acquiredInWeek", it) }
                            pk.acquiredTradeId?.let { put("acquiredTradeId", it) }
                            pk.deferredFromYear?.let { put("deferredFromYear", it) }
                        }
                    )
                }
                put("draftPicks", picksArray)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeFranchises(json: String, fallbackYear: Int = 2026): List<Franchise> {
        if (json.isBlank()) return InitialLeagueData.getInitialFranchises()
        val list = mutableListOf<Franchise>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val ownerGroupId = obj.optString("ownerGroupId", "").ifBlank { "own_${obj.getString("id")}" }
            val ownerGroupName = obj.getString("ownerName")
            val ownerType = try { OwnershipType.valueOf(obj.getString("ownerType")) } catch (e: Exception) { OwnershipType.TECH_BILLIONAIRE }
            val fallbackOwner = OwnerGovernanceEngine.defaultOwnerForGroup(ownerGroupId, ownerGroupName, ownerType)
            val restoredPrimaryOwner = if (obj.has("primaryOwnerName") && obj.has("primaryOwnerArchetype")) {
                val restoredArch = runCatching {
                    OwnerArchetype.valueOf(obj.optString("primaryOwnerArchetype", fallbackOwner.archetype.name))
                }.getOrDefault(fallbackOwner.archetype)
                FranchiseOwner(
                    id = obj.optString("primaryOwnerId", fallbackOwner.id),
                    fullName = obj.optString("primaryOwnerName", fallbackOwner.fullName),
                    roleTitle = obj.optString("primaryOwnerRole", fallbackOwner.roleTitle),
                    backgroundBio = obj.optString("primaryOwnerBio", fallbackOwner.backgroundBio),
                    archetype = restoredArch,
                    seatWeight = obj.optInt("primaryOwnerSeatWeight", 1).coerceAtLeast(1)
                )
            } else {
                fallbackOwner
            }
            val owner = OwnershipGroup(
                id = ownerGroupId,
                name = ownerGroupName,
                type = ownerType,
                netWorthBillions = obj.optDouble("ownerNetWorth", 5.0),
                ownerApproval = obj.optInt("ownerApproval", 80),
                primaryOwner = restoredPrimaryOwner
            )
            val cityName = obj.getString("cityName")
            val defaultGeo = InitialLeagueData.getGeographicAlignment(cityName)
            val conf = try {
                Conference.valueOf(obj.optString("conference", defaultGeo.first.name))
            } catch (e: Exception) {
                defaultGeo.first
            }
            val div = try {
                Division.valueOf(obj.optString("division", defaultGeo.second.name))
            } catch (e: Exception) {
                defaultGeo.second
            }
            val ongoingProjects = mutableListOf<FacilityProject>()
            val projectsArray = obj.optJSONArray("facilityProjects")
            if (projectsArray != null) {
                for (pIdx in 0 until projectsArray.length()) {
                    val pObj = projectsArray.optJSONObject(pIdx) ?: continue
                    val fType = try {
                        FacilityType.valueOf(pObj.optString("type", FacilityType.STADIUM_SEATING.name))
                    } catch (_: Exception) {
                        FacilityType.STADIUM_SEATING
                    }
                    ongoingProjects.add(
                        FacilityProject(
                            id = pObj.optString("id", "proj_${pIdx}"),
                            franchiseId = pObj.optString("franchiseId", obj.getString("id")),
                            type = fType,
                            targetTier = pObj.optInt("targetTier", 2),
                            projectName = pObj.optString("projectName", fType.title),
                            costMillions = pObj.optLong("costMillions", 15L),
                            totalWeeks = pObj.optInt("totalWeeks", 2),
                            remainingWeeks = pObj.optInt("remainingWeeks", 1),
                            seatingCapacityGain = pObj.optInt("seatingCapacityGain", 0),
                            annualSponsorGainMillions = pObj.optLong("annualSponsorGainMillions", 0L),
                            rosterOvrGain = pObj.optInt("rosterOvrGain", 0),
                            valuationBoostMillions = pObj.optLong("valuationBoostMillions", 20L),
                            loyaltyGain = pObj.optInt("loyaltyGain", 1)
                        )
                    )
                }
            }
            val restoredFacilities = FranchiseFacilities(
                stadiumTier = obj.optInt("facilityStadiumTier", 1).coerceIn(1, 5),
                trainingTier = obj.optInt("facilityTrainingTier", 1).coerceIn(1, 5),
                luxurySuitesTier = obj.optInt("facilitySuitesTier", 1).coerceIn(1, 5),
                cumulativeInvestedMillions = obj.optLong("facilityInvested", 0L),
                accumulatedFacilityValueMillions = obj.optLong("facilityAccumulatedValue", 0L),
                ongoingProjects = ongoingProjects,
                completedUpgradesCount = obj.optInt("facilityCompletedCount", 0)
            )
            val primaryHex = obj.getLong("colorHex")
            val secondaryHex = obj.optLong(
                "secondaryColorHex",
                FranchiseIdentityEngine.defaultSecondaryFor(primaryHex)
            ).let {
                if (it == primaryHex) FranchiseIdentityEngine.defaultSecondaryFor(primaryHex) else it
            }
            list.add(
                Franchise(
                    id = obj.getString("id"),
                    cityId = obj.getString("cityId"),
                    cityName = cityName,
                    nickname = obj.getString("nickname"),
                    colorHex = primaryHex,
                    secondaryColorHex = secondaryHex,
                    ownership = owner,
                    conference = conf,
                    division = div,
                    rosterRating = obj.getInt("rosterRating"),
                    fanBaseMillions = obj.getDouble("fanBaseMillions"),
                    fanMorale = obj.getInt("fanMorale"),
                    stadiumCapacity = obj.getInt("stadiumCapacity"),
                    stadiumAge = obj.optInt("stadiumAge", 14).coerceAtLeast(0),
                    stadiumCondition = obj.optInt("stadiumCondition", 78).coerceIn(0, 100),
                    stadium = TeamStadium(
                        sponsorName = obj.optString("stadiumSponsorName", "").takeIf { it.isNotBlank() },
                        sponsorTier = obj.optInt("stadiumSponsorTier", 0).coerceIn(0, 3),
                        sponsorYearsLeft = obj.optInt("stadiumSponsorYearsLeft", 0).coerceAtLeast(0),
                        namingRightsRevenue = obj.optLong("stadiumNamingRightsRevenue", 0L).coerceAtLeast(0L)
                    ),
                    relocationCount = obj.optInt("relocationCount", 0).coerceAtLeast(0),
                    lowSpendingSeasons = obj.optInt("lowSpendingSeasons", 0).coerceAtLeast(0),
                    reputationScore = obj.optInt("reputationScore", 78).coerceIn(10, 99),
                    lastReputationNote = obj.optString(
                        "lastReputationNote",
                        "Balanced ownership investment and market stability."
                    ),
                    ticketPrice = obj.getInt("ticketPrice"),
                    annualPayrollMillions = obj.getLong("annualPayrollMillions"),
                    localSponsorRevenueMillions = obj.getLong("localSponsorRevenueMillions"),
                    wins = obj.getInt("wins"),
                    losses = obj.getInt("losses"),
                    pointsFor = obj.getInt("pointsFor"),
                    pointsAgainst = obj.getInt("pointsAgainst"),
                    championships = obj.getInt("championships"),
                    fanLoyalty = obj.optInt("fanLoyalty", 75),
                    activeFranchiseCampaign = obj.optString("activeCampaignType", "").takeIf { it.isNotBlank() }?.let { typeName ->
                        try {
                            ActiveFranchiseCampaign(
                                type = FranchiseMarketingCampaignType.valueOf(typeName),
                                remainingWeeks = obj.optInt("activeCampaignWeeks", 1),
                                totalLoyaltyGained = obj.optInt("activeCampaignGained", 0)
                            )
                        } catch (_: Exception) { null }
                    },
                    facilities = restoredFacilities,
                    roster = run {
                        val rosterArray = obj.optJSONArray("roster")
                        val fId = obj.getString("id")
                        val fRating = obj.getInt("rosterRating")
                        if (rosterArray != null && rosterArray.length() > 0) {
                            (0 until rosterArray.length()).mapNotNull { rIdx ->
                                val pObj = rosterArray.optJSONObject(rIdx) ?: return@mapNotNull null
                                RosterPlayer(
                                    id = pObj.optString("id", "player_${fId}_$rIdx"),
                                    name = pObj.optString("name", "Roster Player"),
                                    franchiseId = pObj.optString("franchiseId", fId),
                                    position = pObj.optString("position", "SG"),
                                    overallRating = pObj.optInt("overallRating", 75),
                                    age = pObj.optInt("age", 25),
                                    salaryMillions = pObj.optLong("salaryMillions", 5L),
                                    contractYearsRemaining = pObj.optInt("contractYearsRemaining", 1),
                                    archetype = pObj.optString("archetype", "Rotation Contributor"),
                                    depthOrder = pObj.optInt("depthOrder", rIdx),
                                    isStarter = pObj.optBoolean("isStarter", rIdx < 5),
                                    isAssignedToMinorLeague = pObj.optBoolean("isAssignedToMinorLeague", false),
                                    minorLeagueOvrProgress = pObj.optInt("minorLeagueOvrProgress", 0),
                                    suspendedGamesRemaining = pObj.optInt("suspendedGamesRemaining", 0)
                                )
                            }
                        } else {
                            TradeEngine.generateDefaultRosterForFranchise(fId, cityName, SportType.BASKETBALL, fRating)
                        }
                    },
                    draftPicks = run {
                        val picksArray = obj.optJSONArray("draftPicks")
                        val fId = obj.getString("id")
                        val fNickname = obj.getString("nickname")
                        val fFullName = "$cityName $fNickname"
                        if (picksArray != null && picksArray.length() > 0) {
                            (0 until picksArray.length()).mapNotNull { pkIdx ->
                                val pkObj = picksArray.optJSONObject(pkIdx) ?: return@mapNotNull null
                                DraftPick(
                                    id = pkObj.optString("id", "pick_${fId}_${fallbackYear}_$pkIdx"),
                                    year = pkObj.optInt("year", fallbackYear),
                                    round = pkObj.optInt("round", 1),
                                    originalFranchiseId = pkObj.optString("originalFranchiseId", fId),
                                    originalFranchiseName = pkObj.optString("originalFranchiseName", fFullName),
                                    currentFranchiseId = pkObj.optString("currentFranchiseId", fId),
                                    isProtected = pkObj.optBoolean("isProtected", false),
                                    protectionDescription = pkObj.optString("protectionDescription", "Unprotected"),
                                    acquiredFromFranchiseId = pkObj.optString("acquiredFromFranchiseId", "").ifBlank { null },
                                    acquiredFromFranchiseName = pkObj.optString("acquiredFromFranchiseName", "").ifBlank { null },
                                    acquiredInWeek = if (pkObj.has("acquiredInWeek")) pkObj.optInt("acquiredInWeek") else null,
                                    acquiredTradeId = pkObj.optString("acquiredTradeId", "").ifBlank { null },
                                    deferredFromYear = if (pkObj.has("deferredFromYear")) pkObj.optInt("deferredFromYear") else null
                                )
                            }
                        } else {
                            TradeEngine.generateDefaultDraftPicksForFranchise(fId, fFullName, fallbackYear)
                        }
                    }
                )
            )
        }
        return list
    }

    private fun serializeCities(list: List<MarketCity>): String {
        val array = JSONArray()
        list.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("country", c.country)
                put("region", c.region)
                put("populationMillions", c.populationMillions)
                put("tier", c.tier.name)
                put("annualGrowthRatePct", c.annualGrowthRatePct)
                put("sportsAppetite", c.sportsAppetite)
                put("medianIncomeK", c.medianIncomeK)
                put("isInternational", c.isInternational)
                put("hasFranchise", c.hasFranchise)
                put("expansionFeeMillions", c.expansionFeeMillions)
                put("digitalStreamingIndex", c.digitalStreamingIndex)
                put("localSponsorDepthMillions", c.localSponsorDepthMillions)
                put("arenaName", c.arenaName)
                put("arenaCapacity", c.arenaCapacity)
                put("competingMajorTeamsCount", c.competingMajorTeamsCount)
                put("arenaReadinessStatus", c.arenaReadinessStatus)
                put("investorGroupProfile", c.investorGroupProfile)
                put("hasBuiltExpansionArena", c.hasBuiltExpansionArena)
                put("arenaBuildCostSpentMillions", c.arenaBuildCostSpentMillions)
                put("expansionArenaTierName", c.expansionArenaTierName)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeCities(json: String): List<MarketCity> {
        if (json.isBlank()) return InitialLeagueData.getDefaultCities()
        val list = mutableListOf<MarketCity>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                MarketCity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    country = obj.getString("country"),
                    region = obj.getString("region"),
                    populationMillions = obj.getDouble("populationMillions"),
                    tier = try { MarketTier.valueOf(obj.getString("tier")) } catch (e: Exception) { MarketTier.TIER_1_MEGA },
                    annualGrowthRatePct = obj.getDouble("annualGrowthRatePct"),
                    sportsAppetite = obj.getInt("sportsAppetite"),
                    medianIncomeK = obj.getInt("medianIncomeK"),
                    isInternational = obj.getBoolean("isInternational"),
                    hasFranchise = obj.getBoolean("hasFranchise"),
                    expansionFeeMillions = obj.getLong("expansionFeeMillions"),
                    digitalStreamingIndex = obj.optInt("digitalStreamingIndex", 86),
                    localSponsorDepthMillions = obj.optLong("localSponsorDepthMillions", 38L),
                    arenaName = obj.optString("arenaName", "Metropolitan Arena"),
                    arenaCapacity = obj.optInt("arenaCapacity", 21000),
                    competingMajorTeamsCount = obj.optInt("competingMajorTeamsCount", 2),
                    arenaReadinessStatus = obj.optString("arenaReadinessStatus", "Turnkey Modern Arena"),
                    investorGroupProfile = obj.optString("investorGroupProfile", "Regional Investment Consortium"),
                    hasBuiltExpansionArena = obj.optBoolean("hasBuiltExpansionArena", false),
                    arenaBuildCostSpentMillions = obj.optLong("arenaBuildCostSpentMillions", 0L),
                    expansionArenaTierName = obj.optString("expansionArenaTierName", "")
                )
            )
        }
        return list
    }

    private fun serializeCampaigns(list: List<MarketingCampaign>): String {
        val array = JSONArray()
        list.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("description", c.description)
                put("costMillions", c.costMillions)
                put("fanGrowthPct", c.fanGrowthPct)
                put("durationWeeks", c.durationWeeks)
                put("targetSegment", c.targetSegment)
                put("isInternational", c.isInternational)
                put("isActive", c.isActive)
                put("remainingWeeks", c.remainingWeeks)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeCampaigns(json: String): List<MarketingCampaign> {
        if (json.isBlank()) return InitialLeagueData.getMarketingCampaignCatalog()
        val list = mutableListOf<MarketingCampaign>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                MarketingCampaign(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    costMillions = obj.getLong("costMillions"),
                    fanGrowthPct = obj.getDouble("fanGrowthPct"),
                    durationWeeks = obj.getInt("durationWeeks"),
                    targetSegment = obj.getString("targetSegment"),
                    isInternational = obj.getBoolean("isInternational"),
                    isActive = obj.getBoolean("isActive"),
                    remainingWeeks = obj.optInt("remainingWeeks", 0)
                )
            )
        }
        return list
    }

    private fun serializeHistory(list: List<LeagueValuationHistory>): String {
        val array = JSONArray()
        list.forEach { h ->
            val obj = JSONObject().apply {
                put("year", h.year)
                put("valuationMillions", h.valuationMillions)
                put("totalRevenueMillions", h.totalRevenueMillions)
                put("championName", h.championName)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeHistory(json: String): List<LeagueValuationHistory> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<LeagueValuationHistory>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                LeagueValuationHistory(
                    year = obj.getInt("year"),
                    valuationMillions = obj.getLong("valuationMillions"),
                    totalRevenueMillions = obj.getLong("totalRevenueMillions"),
                    championName = obj.getString("championName")
                )
            )
        }
        return list
    }

    private fun serializeStringList(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    fun deserializeStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    fun serializeFreeAgents(list: List<FreeAgentPlayer>): String {
        val array = JSONArray()
        list.forEach { fa ->
            array.put(
                JSONObject().apply {
                    put("id", fa.id)
                    put("name", fa.name)
                    put("position", fa.position)
                    put("overallRating", fa.overallRating)
                    put("age", fa.age)
                    put("archetype", fa.archetype)
                    put("experienceYears", fa.experienceYears)
                    put("askingSalaryMillions", fa.askingSalaryMillions)
                    put("preferredYears", fa.preferredYears)
                    put("formerFranchiseName", fa.formerFranchiseName)
                    put("bioTag", fa.bioTag)
                }
            )
        }
        return array.toString()
    }

    fun deserializeFreeAgents(json: String): List<FreeAgentPlayer> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<FreeAgentPlayer>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                FreeAgentPlayer(
                    id = obj.optString("id", "fa_$i"),
                    name = obj.optString("name", "Free Agent"),
                    position = obj.optString("position", "PG"),
                    overallRating = obj.optInt("overallRating", 75),
                    age = obj.optInt("age", 26),
                    archetype = obj.optString("archetype", "Rotation Contributor"),
                    experienceYears = obj.optInt("experienceYears", 4),
                    askingSalaryMillions = obj.optLong("askingSalaryMillions", 5L),
                    preferredYears = obj.optInt("preferredYears", 2),
                    formerFranchiseName = obj.optString("formerFranchiseName", "Free Agent Pool"),
                    bioTag = obj.optString("bioTag", "Depth Specialist")
                )
            )
        }
        return list
    }

    fun serializeFreeAgentTransactions(list: List<FreeAgentSigningTransaction>): String {
        val array = JSONArray()
        list.forEach { tx ->
            array.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("playerId", tx.playerId)
                    put("playerName", tx.playerName)
                    put("position", tx.position)
                    put("overallRating", tx.overallRating)
                    put("age", tx.age)
                    put("archetype", tx.archetype)
                    put("franchiseId", tx.franchiseId)
                    put("franchiseName", tx.franchiseName)
                    put("salaryMillions", tx.salaryMillions)
                    put("contractYears", tx.contractYears)
                    put("contractTypeLabel", tx.contractTypeLabel)
                    put("reason", tx.reason)
                    put("week", tx.week)
                    put("seasonYear", tx.seasonYear)
                    put("isAutoSigned", tx.isAutoSigned)
                }
            )
        }
        return array.toString()
    }

    fun deserializeFreeAgentTransactions(json: String): List<FreeAgentSigningTransaction> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<FreeAgentSigningTransaction>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                FreeAgentSigningTransaction(
                    id = obj.optString("id", "tx_$i"),
                    playerId = obj.optString("playerId", ""),
                    playerName = obj.optString("playerName", ""),
                    position = obj.optString("position", "PG"),
                    overallRating = obj.optInt("overallRating", 75),
                    age = obj.optInt("age", 26),
                    archetype = obj.optString("archetype", ""),
                    franchiseId = obj.optString("franchiseId", ""),
                    franchiseName = obj.optString("franchiseName", ""),
                    salaryMillions = obj.optLong("salaryMillions", 4L),
                    contractYears = obj.optInt("contractYears", 1),
                    contractTypeLabel = obj.optString("contractTypeLabel", "Standard Contract"),
                    reason = obj.optString("reason", "Depth chart fill"),
                    week = obj.optInt("week", 1),
                    seasonYear = obj.optInt("seasonYear", 2026),
                    isAutoSigned = obj.optBoolean("isAutoSigned", true)
                )
            )
        }
        return list
    }

    fun saveMinorLeagueState(state: MinorLeagueState) {
        minorPrefs.edit()
            .putString("minor_state_json", serializeMinorLeagueState(state))
            .apply()
    }

    fun loadMinorLeagueState(): MinorLeagueState? {
        val json = minorPrefs.getString("minor_state_json", null) ?: return null
        return deserializeMinorLeagueState(json)
    }

    fun serializeMinorLeagueState(state: MinorLeagueState): String {
        val root = JSONObject().apply {
            put("leagueName", state.leagueName)
            put("playoffFieldSize", state.playoffFieldSize)
            put("playoffChampionAffiliateId", state.playoffChampionAffiliateId ?: "")
            put("playoffChampionName", state.playoffChampionName ?: "")
            put("playoffRunnerUpName", state.playoffRunnerUpName ?: "")
            put("lastSyncedParentTeamCount", state.lastSyncedParentTeamCount)
            put("manualOptOutIds", JSONArray(state.manualParentBenchOptOutIds.toList()))

            val affArr = JSONArray()
            state.affiliates.forEach { aff ->
                affArr.put(JSONObject().apply {
                    put("id", aff.id)
                    put("parentFranchiseId", aff.parentFranchiseId)
                    put("parentFranchiseName", aff.parentFranchiseName)
                    put("cityId", aff.cityId)
                    put("cityName", aff.cityName)
                    put("nickname", aff.nickname)
                    put("colorHex", aff.colorHex)
                    put("conference", aff.conference.name)
                    put("division", aff.division.name)
                    put("marketTierLabel", aff.marketTierLabel)
                    put("arenaName", aff.arenaName)
                    put("arenaCapacity", aff.arenaCapacity)
                    put("populationMillions", aff.populationMillions)
                    put("ticketPrice", aff.ticketPrice)
                    put("fanBaseThousands", aff.fanBaseThousands)
                    put("fanLoyalty", aff.fanLoyalty)
                    put("marketAppetite", aff.marketAppetite)
                    put("wins", aff.wins)
                    put("losses", aff.losses)
                    put("pointsFor", aff.pointsFor)
                    put("pointsAgainst", aff.pointsAgainst)
                    put("championships", aff.championships)
                    put("isExpansionAffiliate", aff.isExpansionAffiliate)
                    put("foundedYear", aff.foundedYear)
                    put("ovrBreakthroughsThisSeason", aff.ovrBreakthroughsThisSeason)
                    put("finances", JSONObject().apply {
                        put("ticketRevenueMillions", aff.finances.ticketRevenueMillions)
                        put("localSponsorMillions", aff.finances.localSponsorMillions)
                        put("streamingRevenueMillions", aff.finances.streamingRevenueMillions)
                        put("parentSubsidyMillions", aff.finances.parentSubsidyMillions)
                        put("payrollCostMillions", aff.finances.payrollCostMillions)
                        put("operationsAndTravelMillions", aff.finances.operationsAndTravelMillions)
                        put("gamesHosted", aff.finances.gamesHosted)
                        put("cumulativeAttendance", aff.finances.cumulativeAttendance)
                        put("affiliateValuationMillions", aff.finances.affiliateValuationMillions)
                    })
                    val spArr = JSONArray()
                    aff.supplementalProspects.forEach { sp ->
                        spArr.put(JSONObject().apply {
                            put("id", sp.id)
                            put("affiliateId", sp.affiliateId)
                            put("name", sp.name)
                            put("position", sp.position)
                            put("overallRating", sp.overallRating)
                            put("age", sp.age)
                            put("archetype", sp.archetype)
                            put("salaryMillions", sp.salaryMillions)
                        })
                    }
                    put("supplementalProspects", spArr)
                })
            }
            put("affiliates", affArr)

            fun serializeMatchList(matches: List<MinorLeagueMatch>): JSONArray {
                val arr = JSONArray()
                matches.forEach { m ->
                    arr.put(JSONObject().apply {
                        put("id", m.id)
                        put("week", m.week)
                        put("homeAffiliateId", m.homeAffiliateId)
                        put("homeAffiliateName", m.homeAffiliateName)
                        put("awayAffiliateId", m.awayAffiliateId)
                        put("awayAffiliateName", m.awayAffiliateName)
                        put("homeScore", m.homeScore)
                        put("awayScore", m.awayScore)
                        put("attendance", m.attendance)
                        put("gateRevenueMillions", m.gateRevenueMillions)
                        put("isPlayed", m.isPlayed)
                        put("isPlayoff", m.isPlayoff)
                        put("roundName", m.roundName)
                        put("topPerformerSummary", m.topPerformerSummary)
                    })
                }
                return arr
            }
            put("currentWeekMatches", serializeMatchList(state.currentWeekMatches))
            put("playoffMatches", serializeMatchList(state.playoffMatches))

            val statsArr = JSONArray()
            state.playerStats.values.forEach { st ->
                statsArr.put(JSONObject().apply {
                    put("playerId", st.playerId)
                    put("playerName", st.playerName)
                    put("position", st.position)
                    put("affiliateId", st.affiliateId)
                    put("affiliateName", st.affiliateName)
                    put("parentFranchiseId", st.parentFranchiseId)
                    put("parentFranchiseName", st.parentFranchiseName)
                    put("isParentDepthPlayer", st.isParentDepthPlayer)
                    put("overallRating", st.overallRating)
                    put("age", st.age)
                    put("gamesPlayed", st.gamesPlayed)
                    put("totalPrimaryStat", st.totalPrimaryStat)
                    put("totalSecondaryStat", st.totalSecondaryStat)
                    put("totalTertiaryStat", st.totalTertiaryStat)
                    put("totalDefenseStat", st.totalDefenseStat)
                    put("playoffPrimaryStat", st.playoffPrimaryStat)
                    put("devXp", st.devXp)
                    put("ovrGainsEarned", st.ovrGainsEarned)
                })
            }
            put("playerStats", statsArr)

            val weeklyArr = JSONArray()
            state.weeklyAwards.forEach { w ->
                weeklyArr.put(JSONObject().apply {
                    put("week", w.week)
                    put("seasonYear", w.seasonYear)
                    put("playerId", w.playerId)
                    put("playerName", w.playerName)
                    put("position", w.position)
                    put("affiliateName", w.affiliateName)
                    put("parentFranchiseName", w.parentFranchiseName)
                    put("statSummary", w.statSummary)
                    put("impactScore", w.impactScore)
                })
            }
            put("weeklyAwards", weeklyArr)

            fun serializeAwards(list: List<MinorLeagueAward>): JSONArray {
                val arr = JSONArray()
                list.forEach { a ->
                    arr.put(JSONObject().apply {
                        put("id", a.id)
                        put("year", a.year)
                        put("type", a.type.name)
                        put("playerId", a.playerId)
                        put("playerName", a.playerName)
                        put("position", a.position)
                        put("affiliateId", a.affiliateId)
                        put("affiliateName", a.affiliateName)
                        put("parentFranchiseName", a.parentFranchiseName)
                        put("statLine", a.statLine)
                        put("narrative", a.narrative)
                    })
                }
                return arr
            }
            put("annualAwards", serializeAwards(state.annualAwards))

            val pastArr = JSONArray()
            state.pastSeasons.forEach { s ->
                pastArr.put(JSONObject().apply {
                    put("year", s.year)
                    put("leagueName", s.leagueName)
                    put("affiliatesCount", s.affiliatesCount)
                    put("playoffFieldSize", s.playoffFieldSize)
                    put("championAffiliateId", s.championAffiliateId)
                    put("championAffiliateName", s.championAffiliateName)
                    put("championParentName", s.championParentName)
                    put("runnerUpAffiliateName", s.runnerUpAffiliateName)
                    put("totalAttendance", s.totalAttendance)
                    put("circuitRevenueMillions", s.circuitRevenueMillions)
                    put("awards", serializeAwards(s.awards))
                })
            }
            put("pastSeasons", pastArr)
        }
        return root.toString()
    }

    fun deserializeMinorLeagueState(json: String): MinorLeagueState? {
        if (json.isBlank()) return null
        return try {
            val root = JSONObject(json)
            val optOutSet = mutableSetOf<String>()
            val optArr = root.optJSONArray("manualOptOutIds")
            if (optArr != null) {
                for (i in 0 until optArr.length()) {
                    optOutSet.add(optArr.optString(i))
                }
            }

            val affiliates = mutableListOf<MinorLeagueAffiliate>()
            val affArr = root.optJSONArray("affiliates")
            if (affArr != null) {
                for (i in 0 until affArr.length()) {
                    val obj = affArr.optJSONObject(i) ?: continue
                    val finObj = obj.optJSONObject("finances")
                    val finances = if (finObj != null) {
                        MinorAffiliateFinances(
                            ticketRevenueMillions = finObj.optDouble("ticketRevenueMillions", 0.0),
                            localSponsorMillions = finObj.optDouble("localSponsorMillions", 0.0),
                            streamingRevenueMillions = finObj.optDouble("streamingRevenueMillions", 0.0),
                            parentSubsidyMillions = finObj.optDouble("parentSubsidyMillions", 0.0),
                            payrollCostMillions = finObj.optDouble("payrollCostMillions", 0.0),
                            operationsAndTravelMillions = finObj.optDouble("operationsAndTravelMillions", 0.0),
                            gamesHosted = finObj.optInt("gamesHosted", 0),
                            cumulativeAttendance = finObj.optInt("cumulativeAttendance", 0),
                            affiliateValuationMillions = finObj.optDouble("affiliateValuationMillions", 18.0)
                        )
                    } else MinorAffiliateFinances()

                    val spList = mutableListOf<MinorAffiliateProspect>()
                    val spArr = obj.optJSONArray("supplementalProspects")
                    if (spArr != null) {
                        for (j in 0 until spArr.length()) {
                            val spObj = spArr.optJSONObject(j) ?: continue
                            spList.add(
                                MinorAffiliateProspect(
                                    id = spObj.optString("id"),
                                    affiliateId = spObj.optString("affiliateId"),
                                    name = spObj.optString("name"),
                                    position = spObj.optString("position"),
                                    overallRating = spObj.optInt("overallRating", 70),
                                    age = spObj.optInt("age", 20),
                                    archetype = spObj.optString("archetype"),
                                    salaryMillions = spObj.optDouble("salaryMillions", 0.2)
                                )
                            )
                        }
                    }

                    affiliates.add(
                        MinorLeagueAffiliate(
                            id = obj.optString("id"),
                            parentFranchiseId = obj.optString("parentFranchiseId"),
                            parentFranchiseName = obj.optString("parentFranchiseName"),
                            cityId = obj.optString("cityId"),
                            cityName = obj.optString("cityName"),
                            nickname = obj.optString("nickname"),
                            colorHex = obj.optLong("colorHex", 0xFF3B82F6),
                            conference = runCatching { Conference.valueOf(obj.optString("conference")) }.getOrDefault(Conference.EASTERN),
                            division = runCatching { Division.valueOf(obj.optString("division")) }.getOrDefault(Division.ATLANTIC),
                            marketTierLabel = obj.optString("marketTierLabel", "Tier-2 Feeder Hub"),
                            arenaName = obj.optString("arenaName", "Civic Arena"),
                            arenaCapacity = obj.optInt("arenaCapacity", 6500),
                            populationMillions = obj.optDouble("populationMillions", 1.0),
                            ticketPrice = obj.optInt("ticketPrice", 24),
                            fanBaseThousands = obj.optInt("fanBaseThousands", 120),
                            fanLoyalty = obj.optInt("fanLoyalty", 72),
                            marketAppetite = obj.optInt("marketAppetite", 78),
                            wins = obj.optInt("wins", 0),
                            losses = obj.optInt("losses", 0),
                            pointsFor = obj.optInt("pointsFor", 0),
                            pointsAgainst = obj.optInt("pointsAgainst", 0),
                            championships = obj.optInt("championships", 0),
                            isExpansionAffiliate = obj.optBoolean("isExpansionAffiliate", false),
                            foundedYear = obj.optInt("foundedYear", 2026),
                            finances = finances,
                            supplementalProspects = spList,
                            ovrBreakthroughsThisSeason = obj.optInt("ovrBreakthroughsThisSeason", 0)
                        )
                    )
                }
            }

            fun parseMatches(arr: JSONArray?): List<MinorLeagueMatch> {
                if (arr == null) return emptyList()
                val res = mutableListOf<MinorLeagueMatch>()
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    res.add(
                        MinorLeagueMatch(
                            id = m.optString("id"),
                            week = m.optInt("week", 1),
                            homeAffiliateId = m.optString("homeAffiliateId"),
                            homeAffiliateName = m.optString("homeAffiliateName"),
                            awayAffiliateId = m.optString("awayAffiliateId"),
                            awayAffiliateName = m.optString("awayAffiliateName"),
                            homeScore = m.optInt("homeScore", 0),
                            awayScore = m.optInt("awayScore", 0),
                            attendance = m.optInt("attendance", 0),
                            gateRevenueMillions = m.optDouble("gateRevenueMillions", 0.0),
                            isPlayed = m.optBoolean("isPlayed", false),
                            isPlayoff = m.optBoolean("isPlayoff", false),
                            roundName = m.optString("roundName", ""),
                            topPerformerSummary = m.optString("topPerformerSummary", "")
                        )
                    )
                }
                return res
            }

            val statsMap = mutableMapOf<String, MinorLeaguePlayerStat>()
            val statsArr = root.optJSONArray("playerStats")
            if (statsArr != null) {
                for (i in 0 until statsArr.length()) {
                    val s = statsArr.optJSONObject(i) ?: continue
                    val pId = s.optString("playerId")
                    if (pId.isNotBlank()) {
                        statsMap[pId] = MinorLeaguePlayerStat(
                            playerId = pId,
                            playerName = s.optString("playerName"),
                            position = s.optString("position"),
                            affiliateId = s.optString("affiliateId"),
                            affiliateName = s.optString("affiliateName"),
                            parentFranchiseId = s.optString("parentFranchiseId"),
                            parentFranchiseName = s.optString("parentFranchiseName"),
                            isParentDepthPlayer = s.optBoolean("isParentDepthPlayer", true),
                            overallRating = s.optInt("overallRating", 72),
                            age = s.optInt("age", 22),
                            gamesPlayed = s.optInt("gamesPlayed", 0),
                            totalPrimaryStat = s.optInt("totalPrimaryStat", 0),
                            totalSecondaryStat = s.optInt("totalSecondaryStat", 0),
                            totalTertiaryStat = s.optInt("totalTertiaryStat", 0),
                            totalDefenseStat = s.optInt("totalDefenseStat", 0),
                            playoffPrimaryStat = s.optInt("playoffPrimaryStat", 0),
                            devXp = s.optInt("devXp", 0),
                            ovrGainsEarned = s.optInt("ovrGainsEarned", 0)
                        )
                    }
                }
            }

            val weeklyAwards = mutableListOf<MinorLeagueWeeklyAward>()
            val weeklyArr = root.optJSONArray("weeklyAwards")
            if (weeklyArr != null) {
                for (i in 0 until weeklyArr.length()) {
                    val w = weeklyArr.optJSONObject(i) ?: continue
                    weeklyAwards.add(
                        MinorLeagueWeeklyAward(
                            week = w.optInt("week", 1),
                            seasonYear = w.optInt("seasonYear", 2026),
                            playerId = w.optString("playerId"),
                            playerName = w.optString("playerName"),
                            position = w.optString("position"),
                            affiliateName = w.optString("affiliateName"),
                            parentFranchiseName = w.optString("parentFranchiseName"),
                            statSummary = w.optString("statSummary"),
                            impactScore = w.optInt("impactScore", 0)
                        )
                    )
                }
            }

            fun parseAwards(arr: JSONArray?): List<MinorLeagueAward> {
                if (arr == null) return emptyList()
                val res = mutableListOf<MinorLeagueAward>()
                for (i in 0 until arr.length()) {
                    val a = arr.optJSONObject(i) ?: continue
                    res.add(
                        MinorLeagueAward(
                            id = a.optString("id"),
                            year = a.optInt("year", 2026),
                            type = runCatching { MinorLeagueAwardType.valueOf(a.optString("type")) }.getOrDefault(MinorLeagueAwardType.MINOR_MVP),
                            playerId = a.optString("playerId"),
                            playerName = a.optString("playerName"),
                            position = a.optString("position"),
                            affiliateId = a.optString("affiliateId"),
                            affiliateName = a.optString("affiliateName"),
                            parentFranchiseName = a.optString("parentFranchiseName"),
                            statLine = a.optString("statLine"),
                            narrative = a.optString("narrative")
                        )
                    )
                }
                return res
            }

            val pastSeasons = mutableListOf<MinorLeagueSeasonArchive>()
            val pastArr = root.optJSONArray("pastSeasons")
            if (pastArr != null) {
                for (i in 0 until pastArr.length()) {
                    val p = pastArr.optJSONObject(i) ?: continue
                    pastSeasons.add(
                        MinorLeagueSeasonArchive(
                            year = p.optInt("year", 2026),
                            leagueName = p.optString("leagueName", "Apex G-Circuit"),
                            affiliatesCount = p.optInt("affiliatesCount", 6),
                            playoffFieldSize = p.optInt("playoffFieldSize", 4),
                            championAffiliateId = p.optString("championAffiliateId"),
                            championAffiliateName = p.optString("championAffiliateName"),
                            championParentName = p.optString("championParentName"),
                            runnerUpAffiliateName = p.optString("runnerUpAffiliateName"),
                            totalAttendance = p.optInt("totalAttendance", 0),
                            circuitRevenueMillions = p.optDouble("circuitRevenueMillions", 0.0),
                            awards = parseAwards(p.optJSONArray("awards"))
                        )
                    )
                }
            }

            MinorLeagueState(
                leagueName = root.optString("leagueName", "Apex G-Circuit"),
                affiliates = affiliates,
                currentWeekMatches = parseMatches(root.optJSONArray("currentWeekMatches")),
                playoffMatches = parseMatches(root.optJSONArray("playoffMatches")),
                playoffFieldSize = root.optInt("playoffFieldSize", 4),
                playoffChampionAffiliateId = root.optString("playoffChampionAffiliateId", "").ifBlank { null },
                playoffChampionName = root.optString("playoffChampionName", "").ifBlank { null },
                playoffRunnerUpName = root.optString("playoffRunnerUpName", "").ifBlank { null },
                playerStats = statsMap,
                weeklyAwards = weeklyAwards,
                annualAwards = parseAwards(root.optJSONArray("annualAwards")),
                pastSeasons = pastSeasons,
                manualParentBenchOptOutIds = optOutSet,
                lastSyncedParentTeamCount = root.optInt("lastSyncedParentTeamCount", affiliates.size)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveSeasonScheduleState(state: SeasonScheduleState) {
        fun planToJson(plan: SeasonSchedulePlan): JSONObject = JSONObject().apply {
            put("targetYear", plan.targetYear)
            put("regularSeasonWeeks", plan.regularSeasonWeeks)
            put("approvalStatus", plan.approvalStatus.name)
            put("lastVoteSummary", plan.lastVoteSummary)
            put("hasValidationErrors", plan.hasValidationErrors)
            val warningsArr = JSONArray()
            plan.conflictWarnings.forEach { warningsArr.put(it) }
            put("conflictWarnings", warningsArr)
            val slotsArr = JSONArray()
            plan.slots.forEach { s ->
                slotsArr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("week", s.week)
                        put("homeTeamId", s.homeTeamId)
                        put("awayTeamId", s.awayTeamId)
                        put("holidayTentpole", s.holidayTentpole.name)
                        put("isMarqueeRivalry", s.isMarqueeRivalry)
                        put("rivalryTitle", s.rivalryTitle ?: "")
                        put("internationalVenueId", s.internationalVenueId ?: "")
                        put("internationalVenueLabel", s.internationalVenueLabel ?: "")
                    }
                )
            }
            put("slots", slotsArr)
        }

        val root = JSONObject().apply {
            state.upcomingPlan?.let { put("upcomingPlan", planToJson(it)) }
            state.activeSeasonSchedule?.let { put("activeSeasonSchedule", planToJson(it)) }
            val pastArr = JSONArray()
            state.pastSeasonPlans.take(6).forEach { pastArr.put(planToJson(it)) }
            put("pastSeasonPlans", pastArr)
        }
        settingsPrefs.edit().putString("season_schedule_state_json", root.toString()).apply()
    }

    fun loadSeasonScheduleState(): SeasonScheduleState? {
        val raw = settingsPrefs.getString("season_schedule_state_json", null) ?: return null
        return try {
            fun jsonToPlan(obj: JSONObject?): SeasonSchedulePlan? {
                if (obj == null) return null
                val slots = mutableListOf<ScheduledGameSlot>()
                val slotsArr = obj.optJSONArray("slots") ?: JSONArray()
                for (i in 0 until slotsArr.length()) {
                    val sObj = slotsArr.optJSONObject(i) ?: continue
                    val rivTitle = sObj.optString("rivalryTitle", "").ifBlank { null }
                    val intlId = sObj.optString("internationalVenueId", "").ifBlank { null }
                    val intlLabel = sObj.optString("internationalVenueLabel", "").ifBlank { null }
                    slots.add(
                        ScheduledGameSlot(
                            id = sObj.optString("id"),
                            week = sObj.optInt("week", 1),
                            homeTeamId = sObj.optString("homeTeamId"),
                            awayTeamId = sObj.optString("awayTeamId"),
                            holidayTentpole = runCatching {
                                HolidayTentpole.valueOf(sObj.optString("holidayTentpole"))
                            }.getOrDefault(HolidayTentpole.NONE),
                            isMarqueeRivalry = sObj.optBoolean("isMarqueeRivalry", false),
                            rivalryTitle = rivTitle,
                            internationalVenueId = intlId,
                            internationalVenueLabel = intlLabel
                        )
                    )
                }
                val warnings = mutableListOf<String>()
                val warnArr = obj.optJSONArray("conflictWarnings")
                if (warnArr != null) {
                    for (i in 0 until warnArr.length()) {
                        warnArr.optString(i)?.takeIf { it.isNotBlank() }?.let { warnings.add(it) }
                    }
                }
                return SeasonSchedulePlan(
                    targetYear = obj.optInt("targetYear", 2026),
                    regularSeasonWeeks = obj.optInt("regularSeasonWeeks", 14).coerceIn(14, 22),
                    slots = slots,
                    approvalStatus = runCatching {
                        ScheduleApprovalStatus.valueOf(obj.optString("approvalStatus"))
                    }.getOrDefault(ScheduleApprovalStatus.DRAFT_IN_PROGRESS),
                    lastVoteSummary = obj.optString("lastVoteSummary", ""),
                    conflictWarnings = warnings,
                    hasValidationErrors = obj.optBoolean("hasValidationErrors", false)
                )
            }

            val root = JSONObject(raw)
            val upcoming = jsonToPlan(root.optJSONObject("upcomingPlan"))
            val active = jsonToPlan(root.optJSONObject("activeSeasonSchedule"))
            val past = mutableListOf<SeasonSchedulePlan>()
            val pastArr = root.optJSONArray("pastSeasonPlans")
            if (pastArr != null) {
                for (i in 0 until pastArr.length()) {
                    jsonToPlan(pastArr.optJSONObject(i))?.let { past.add(it) }
                }
            }
            SeasonScheduleState(
                upcomingPlan = upcoming,
                activeSeasonSchedule = active,
                pastSeasonPlans = past
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveJobSecurityLegacyState(state: JobSecurityLegacyState) {
        fun reportToJson(r: StakeholderPillarReport): JSONObject = JSONObject().apply {
            put("pillar", r.pillar.name)
            put("score", r.score)
            put("statusLabel", r.statusLabel)
            put("headlineDriver", r.headlineDriver)
            put("positiveDrivers", JSONArray(r.positiveDrivers))
            put("riskDrivers", JSONArray(r.riskDrivers))
        }

        fun breakdownToJson(b: StakeholderScoreBreakdown): JSONObject = JSONObject().apply {
            put("ownersReport", reportToJson(b.ownersReport))
            put("playersReport", reportToJson(b.playersReport))
            put("fansReport", reportToJson(b.fansReport))
            put("mediaReport", reportToJson(b.mediaReport))
            put("compositeApprovalScore", b.compositeApprovalScore)
            put("projectedJobSecurityScore", b.projectedJobSecurityScore)
            put("activeRiskAlerts", JSONArray(b.activeRiskAlerts))
            put("backgroundPerkNote", b.backgroundPerkNote)
        }

        fun reviewToJson(rev: AnnualPerformanceReview): JSONObject = JSONObject().apply {
            put("id", rev.id)
            put("seasonYear", rev.seasonYear)
            put("ownersScore", rev.ownersScore)
            put("playersScore", rev.playersScore)
            put("fansScore", rev.fansScore)
            put("mediaScore", rev.mediaScore)
            put("compositeApprovalScore", rev.compositeApprovalScore)
            put("overallGrade", rev.overallGrade.name)
            put("jobSecurityBefore", rev.jobSecurityBefore)
            put("jobSecurityAfter", rev.jobSecurityAfter)
            put("jobSecurityDelta", rev.jobSecurityDelta)
            put("legacyPointsAwarded", rev.legacyPointsAwarded)
            put("legacyTotalAfter", rev.legacyTotalAfter)
            put("contractAction", rev.contractAction.name)
            put("leagueValuationSnapshotMillions", rev.leagueValuationSnapshotMillions)
            put("cashSnapshotMillions", rev.cashSnapshotMillions)
            put("fanbaseSnapshotMillions", rev.fanbaseSnapshotMillions)
            put("championTeamName", rev.championTeamName)
            put("strategicMandateNextYear", rev.strategicMandateNextYear)
            rev.commissionerResponseChosen?.let { put("commissionerResponseChosen", it.name) }
            put("isRatified", rev.isRatified)
            val verdictsArr = JSONArray()
            rev.verdicts.forEach { v ->
                verdictsArr.put(
                    JSONObject().apply {
                        put("pillar", v.pillar.name)
                        put("representativeName", v.representativeName)
                        put("representativeTitle", v.representativeTitle)
                        put("score", v.score)
                        put("gradeLetter", v.gradeLetter)
                        put("quote", v.quote)
                        put("keyFactors", JSONArray(v.keyFactors))
                    }
                )
            }
            put("verdicts", verdictsArr)
        }

        val root = JSONObject().apply {
            put("jobSecurityScore", state.jobSecurityScore)
            put("legacyPoints", state.legacyPoints)
            put("contractYearsRemaining", state.contractYearsRemaining)
            put("seasonsServed", state.seasonsServed)
            put("extensionsEarned", state.extensionsEarned)
            put("warningsIssued", state.warningsIssued)
            put("lastUpdateHeadline", state.lastUpdateHeadline)
            put("latestBreakdown", breakdownToJson(state.latestBreakdown))
            state.pendingAnnualReview?.let { put("pendingAnnualReview", reviewToJson(it)) }
            val histArr = JSONArray()
            state.reviewHistory.take(20).forEach { histArr.put(reviewToJson(it)) }
            put("reviewHistory", histArr)
            put("unlockedMilestonesCount", state.unlockedMilestonesCount)
            put("recordBroadcastAnnualMillions", state.recordBroadcastAnnualMillions)
            put("consecutiveLockoutFreeSeasons", state.consecutiveLockoutFreeSeasons)
            put("maxConsecutiveChampionStreak", state.maxConsecutiveChampionStreak)
            state.dynastyTeamName?.let { put("dynastyTeamName", it) }
            val msArr = JSONArray()
            state.milestones.forEach { m ->
                msArr.put(
                    JSONObject().apply {
                        put("id", m.id.name)
                        put("currentProgress", m.currentProgress)
                        put("isUnlocked", m.isUnlocked)
                        m.unlockedYear?.let { put("unlockedYear", it) }
                        put("progressNote", m.progressNote)
                    }
                )
            }
            put("milestones", msArr)
        }
        settingsPrefs.edit().putString("job_security_legacy_state_json", root.toString()).apply()
    }

    fun loadJobSecurityLegacyState(
        fallbackInput: CommissionerPerformanceInput? = null
    ): JobSecurityLegacyState? {
        val raw = settingsPrefs.getString("job_security_legacy_state_json", null) ?: return null
        return try {
            fun jsonArrayToStrings(arr: JSONArray?): List<String> {
                if (arr == null) return emptyList()
                return (0 until arr.length()).mapNotNull { i ->
                    arr.optString(i).takeIf { it.isNotBlank() }
                }
            }

            fun jsonToReport(obj: JSONObject?, defaultPillar: StakeholderPillar): StakeholderPillarReport? {
                if (obj == null) return null
                val pillar = runCatching { StakeholderPillar.valueOf(obj.optString("pillar")) }
                    .getOrDefault(defaultPillar)
                return StakeholderPillarReport(
                    pillar = pillar,
                    score = obj.optInt("score", 75).coerceIn(10, 99),
                    statusLabel = obj.optString("statusLabel", "Supportive"),
                    headlineDriver = obj.optString("headlineDriver", "Steady governance"),
                    positiveDrivers = jsonArrayToStrings(obj.optJSONArray("positiveDrivers")),
                    riskDrivers = jsonArrayToStrings(obj.optJSONArray("riskDrivers"))
                )
            }

            fun jsonToBreakdown(obj: JSONObject?): StakeholderScoreBreakdown? {
                if (obj == null) return null
                val owners = jsonToReport(obj.optJSONObject("ownersReport"), StakeholderPillar.OWNERS) ?: return null
                val players = jsonToReport(obj.optJSONObject("playersReport"), StakeholderPillar.PLAYERS) ?: return null
                val fans = jsonToReport(obj.optJSONObject("fansReport"), StakeholderPillar.FANS) ?: return null
                val media = jsonToReport(obj.optJSONObject("mediaReport"), StakeholderPillar.MEDIA) ?: return null
                return StakeholderScoreBreakdown(
                    ownersReport = owners,
                    playersReport = players,
                    fansReport = fans,
                    mediaReport = media,
                    compositeApprovalScore = obj.optInt("compositeApprovalScore", 75).coerceIn(10, 99),
                    projectedJobSecurityScore = obj.optInt("projectedJobSecurityScore", 75).coerceIn(10, 99),
                    activeRiskAlerts = jsonArrayToStrings(obj.optJSONArray("activeRiskAlerts")),
                    backgroundPerkNote = obj.optString("backgroundPerkNote", "")
                )
            }

            fun jsonToReview(obj: JSONObject?): AnnualPerformanceReview? {
                if (obj == null) return null
                val verdictsArr = obj.optJSONArray("verdicts") ?: JSONArray()
                val verdicts = (0 until verdictsArr.length()).mapNotNull { i ->
                    val vo = verdictsArr.optJSONObject(i) ?: return@mapNotNull null
                    val pillar = runCatching { StakeholderPillar.valueOf(vo.optString("pillar")) }
                        .getOrDefault(StakeholderPillar.OWNERS)
                    StakeholderReviewVerdict(
                        pillar = pillar,
                        representativeName = vo.optString("representativeName", "Board Committee"),
                        representativeTitle = vo.optString("representativeTitle", "Governor"),
                        score = vo.optInt("score", 75),
                        gradeLetter = vo.optString("gradeLetter", "B"),
                        quote = vo.optString("quote", ""),
                        keyFactors = jsonArrayToStrings(vo.optJSONArray("keyFactors"))
                    )
                }
                val respName = obj.optString("commissionerResponseChosen", "")
                val respChoice = if (respName.isNotBlank()) {
                    runCatching { ReviewResponseChoice.valueOf(respName) }.getOrNull()
                } else null
                return AnnualPerformanceReview(
                    id = obj.optString("id"),
                    seasonYear = obj.optInt("seasonYear", 2026),
                    ownersScore = obj.optInt("ownersScore", 75),
                    playersScore = obj.optInt("playersScore", 75),
                    fansScore = obj.optInt("fansScore", 75),
                    mediaScore = obj.optInt("mediaScore", 75),
                    compositeApprovalScore = obj.optInt("compositeApprovalScore", 75),
                    overallGrade = runCatching { PerformanceReviewGrade.valueOf(obj.optString("overallGrade")) }
                        .getOrDefault(PerformanceReviewGrade.B),
                    jobSecurityBefore = obj.optInt("jobSecurityBefore", 75),
                    jobSecurityAfter = obj.optInt("jobSecurityAfter", 75),
                    jobSecurityDelta = obj.optInt("jobSecurityDelta", 0),
                    legacyPointsAwarded = obj.optInt("legacyPointsAwarded", 50),
                    legacyTotalAfter = obj.optInt("legacyTotalAfter", 170),
                    contractAction = runCatching { CommissionerContractAction.valueOf(obj.optString("contractAction")) }
                        .getOrDefault(CommissionerContractAction.MANDATE_RENEWED),
                    leagueValuationSnapshotMillions = obj.optLong("leagueValuationSnapshotMillions", 420L),
                    cashSnapshotMillions = obj.optLong("cashSnapshotMillions", 95L),
                    fanbaseSnapshotMillions = obj.optDouble("fanbaseSnapshotMillions", 18.2),
                    championTeamName = obj.optString("championTeamName", "League Champion TBD"),
                    verdicts = verdicts,
                    strategicMandateNextYear = obj.optString("strategicMandateNextYear", ""),
                    commissionerResponseChosen = respChoice,
                    isRatified = obj.optBoolean("isRatified", false)
                )
            }

            val root = JSONObject(raw)
            val fallbackBreakdown = fallbackInput?.let { JobSecurityLegacyEngine.evaluateStakeholders(it) }
            val breakdown = jsonToBreakdown(root.optJSONObject("latestBreakdown"))
                ?: fallbackBreakdown
                ?: return null
            val pending = jsonToReview(root.optJSONObject("pendingAnnualReview"))
            val histArr = root.optJSONArray("reviewHistory") ?: JSONArray()
            val history = (0 until histArr.length()).mapNotNull { i ->
                jsonToReview(histArr.optJSONObject(i))
            }

            val msArr = root.optJSONArray("milestones") ?: JSONArray()
            val restoredMilestones = (0 until msArr.length()).mapNotNull { i ->
                val mo = msArr.optJSONObject(i) ?: return@mapNotNull null
                val mId = runCatching { LeagueLegacyMilestoneId.valueOf(mo.optString("id")) }.getOrNull()
                    ?: return@mapNotNull null
                LeagueLegacyMilestone(
                    id = mId,
                    currentProgress = mo.optInt("currentProgress", 0),
                    isUnlocked = mo.optBoolean("isUnlocked", false),
                    unlockedYear = if (mo.has("unlockedYear")) mo.optInt("unlockedYear") else null,
                    progressNote = mo.optString("progressNote", "")
                )
            }

            JobSecurityLegacyState(
                jobSecurityScore = root.optInt("jobSecurityScore", 78).coerceIn(10, 99),
                legacyPoints = root.optInt("legacyPoints", 120).coerceAtLeast(0),
                contractYearsRemaining = root.optInt("contractYearsRemaining", 3).coerceAtLeast(1),
                seasonsServed = root.optInt("seasonsServed", 0).coerceAtLeast(0),
                extensionsEarned = root.optInt("extensionsEarned", 0).coerceAtLeast(0),
                warningsIssued = root.optInt("warningsIssued", 0).coerceAtLeast(0),
                latestBreakdown = breakdown,
                pendingAnnualReview = pending,
                reviewHistory = history,
                lastUpdateHeadline = root.optString(
                    "lastUpdateHeadline",
                    "Board mandate restored from league ledger."
                ),
                milestones = restoredMilestones,
                unlockedMilestonesCount = root.optInt("unlockedMilestonesCount", restoredMilestones.count { it.isUnlocked }),
                recordBroadcastAnnualMillions = root.optLong("recordBroadcastAnnualMillions", 65L),
                consecutiveLockoutFreeSeasons = root.optInt("consecutiveLockoutFreeSeasons", 0),
                maxConsecutiveChampionStreak = root.optInt("maxConsecutiveChampionStreak", 0),
                dynastyTeamName = root.optString("dynastyTeamName", "").ifBlank { null }
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveRivalLeague(rivalLeague: RivalLeague) {
        val root = JSONObject().apply {
            put("isActive", rivalLeague.isActive)
            put("name", rivalLeague.name)
            put("capital", rivalLeague.capital)
            put("hype", rivalLeague.hype)
            put("poachedPlayersJson", serializeFreeAgents(rivalLeague.poachedPlayers))
            put("sportType", rivalLeague.sportType.name)
            put("blacklistedPlayerIds", JSONArray(rivalLeague.blacklistedPlayerIds.toList()))
            put("lastPoachedSeasonYear", rivalLeague.lastPoachedSeasonYear)
            put("totalPoachesCount", rivalLeague.totalPoachesCount)
            put("mergerAttemptsCount", rivalLeague.mergerAttemptsCount)
            put("seasonsSurvived", rivalLeague.seasonsSurvived)
            put("highHypeSeasonsCount", rivalLeague.highHypeSeasonsCount)
            put("permanentTvRevenueBoostMillions", rivalLeague.permanentTvRevenueBoostMillions)
            put("hasResolvedAbsorptionEvent", rivalLeague.hasResolvedAbsorptionEvent)
            put("absorbedTeamNames", JSONArray(rivalLeague.absorbedTeamNames))
            rivalLeague.activeAbsorptionNegotiation?.let { ev ->
                val candArr = JSONArray()
                ev.candidateTeams.forEach { c ->
                    candArr.put(JSONObject().apply {
                        put("id", c.id)
                        put("cityId", c.cityId)
                        put("cityName", c.cityName)
                        put("nickname", c.nickname)
                        put("colorHex", c.colorHex)
                        put("secondaryColorHex", c.secondaryColorHex)
                        put("rosterRating", c.rosterRating)
                        put("fanBaseMillions", c.fanBaseMillions)
                        put("marketPopulationMillions", c.marketPopulationMillions)
                        put("stadiumCapacity", c.stadiumCapacity)
                        put("arenaName", c.arenaName)
                        put("localSponsorRevenueMillions", c.localSponsorRevenueMillions)
                        put("annualBroadcastContributionMillions", c.annualBroadcastContributionMillions)
                        put("signatureStarName", c.signatureStarName)
                        put("isSelected", c.isSelected)
                    })
                }
                put("activeAbsorptionNegotiation", JSONObject().apply {
                    put("id", ev.id)
                    put("rivalLeagueName", ev.rivalLeagueName)
                    put("sportType", ev.sportType.name)
                    put("seasonYear", ev.seasonYear)
                    put("seasonsSurvived", ev.seasonsSurvived)
                    put("rivalHypeSnapshot", ev.rivalHypeSnapshot)
                    put("rivalCapitalSnapshot", ev.rivalCapitalSnapshot)
                    put("baseTvBoostMillions", ev.baseTvBoostMillions)
                    put("ownerApprovalHitPerTeam", ev.ownerApprovalHitPerTeam)
                    put("headline", ev.headline)
                    put("description", ev.description)
                    put("candidateTeams", candArr)
                })
            }
            put("lastOffseasonPoachSummary", rivalLeague.lastOffseasonPoachSummary)
            put("lastActionFeedback", rivalLeague.lastActionFeedback)
            put("historyLog", JSONArray(rivalLeague.historyLog.take(16)))
        }
        settingsPrefs.edit().putString("rival_league_state_json", root.toString()).apply()
    }

    fun loadRivalLeague(): RivalLeague? {
        val raw = settingsPrefs.getString("rival_league_state_json", null) ?: return null
        return try {
            val root = JSONObject(raw)
            val sport = runCatching { SportType.valueOf(root.optString("sportType")) }
                .getOrDefault(SportType.BASKETBALL)
            val poachedJson = root.optString("poachedPlayersJson", "[]")
            val poachedPlayers = deserializeFreeAgents(poachedJson)
            val blacklistArr = root.optJSONArray("blacklistedPlayerIds") ?: JSONArray()
            val blacklisted = (0 until blacklistArr.length()).mapNotNull { i ->
                blacklistArr.optString(i).takeIf { it.isNotBlank() }
            }.toSet()
            val histArr = root.optJSONArray("historyLog") ?: JSONArray()
            val historyLog = (0 until histArr.length()).mapNotNull { i ->
                histArr.optString(i).takeIf { it.isNotBlank() }
            }
            val absorbedArr = root.optJSONArray("absorbedTeamNames") ?: JSONArray()
            val absorbedTeamNames = (0 until absorbedArr.length()).mapNotNull { i ->
                absorbedArr.optString(i).takeIf { it.isNotBlank() }
            }
            val negotiationObj = root.optJSONObject("activeAbsorptionNegotiation")
            val activeNegotiation = negotiationObj?.let { evObj ->
                val evSport = runCatching { SportType.valueOf(evObj.optString("sportType")) }
                    .getOrDefault(sport)
                val candArr = evObj.optJSONArray("candidateTeams") ?: JSONArray()
                val candidates = (0 until candArr.length()).mapNotNull { i ->
                    val c = candArr.optJSONObject(i) ?: return@mapNotNull null
                    RivalFranchiseCandidate(
                        id = c.optString("id"),
                        cityId = c.optString("cityId"),
                        cityName = c.optString("cityName"),
                        nickname = c.optString("nickname"),
                        colorHex = c.optLong("colorHex", 0xFFD97706L),
                        secondaryColorHex = c.optLong("secondaryColorHex", 0xFF1E293BL),
                        rosterRating = c.optInt("rosterRating", 82),
                        fanBaseMillions = c.optDouble("fanBaseMillions", 1.1),
                        marketPopulationMillions = c.optDouble("marketPopulationMillions", 2.4),
                        stadiumCapacity = c.optInt("stadiumCapacity", 20000),
                        arenaName = c.optString("arenaName", "Civic Pavilion"),
                        localSponsorRevenueMillions = c.optLong("localSponsorRevenueMillions", 18L),
                        annualBroadcastContributionMillions = c.optLong("annualBroadcastContributionMillions", 14L),
                        signatureStarName = c.optString("signatureStarName", "Franchise Ace"),
                        isSelected = c.optBoolean("isSelected", true)
                    )
                }
                if (candidates.isEmpty()) null else RivalAbsorptionNegotiationEvent(
                    id = evObj.optString("id"),
                    rivalLeagueName = evObj.optString("rivalLeagueName", RivalLeagueEngine.defaultNameForSport(evSport)),
                    sportType = evSport,
                    seasonYear = evObj.optInt("seasonYear", 2028),
                    seasonsSurvived = evObj.optInt("seasonsSurvived", 3),
                    rivalHypeSnapshot = evObj.optInt("rivalHypeSnapshot", 74),
                    rivalCapitalSnapshot = evObj.optLong("rivalCapitalSnapshot", 85L),
                    candidateTeams = candidates,
                    baseTvBoostMillions = evObj.optLong("baseTvBoostMillions", 26L),
                    ownerApprovalHitPerTeam = evObj.optInt("ownerApprovalHitPerTeam", 5),
                    headline = evObj.optString("headline"),
                    description = evObj.optString("description")
                )
            }
            RivalLeague(
                isActive = root.optBoolean("isActive", true),
                name = root.optString("name", RivalLeagueEngine.defaultNameForSport(sport)),
                capital = root.optLong("capital", 120L).coerceAtLeast(0L),
                hype = root.optInt("hype", 58).coerceIn(0, 100),
                poachedPlayers = poachedPlayers,
                sportType = sport,
                blacklistedPlayerIds = blacklisted,
                lastPoachedSeasonYear = root.optInt("lastPoachedSeasonYear", 0),
                totalPoachesCount = root.optInt("totalPoachesCount", poachedPlayers.size),
                mergerAttemptsCount = root.optInt("mergerAttemptsCount", 0),
                seasonsSurvived = root.optInt("seasonsSurvived", 0).coerceAtLeast(0),
                highHypeSeasonsCount = root.optInt("highHypeSeasonsCount", 0).coerceAtLeast(0),
                permanentTvRevenueBoostMillions = root.optLong("permanentTvRevenueBoostMillions", 0L).coerceAtLeast(0L),
                hasResolvedAbsorptionEvent = root.optBoolean("hasResolvedAbsorptionEvent", false),
                absorbedTeamNames = absorbedTeamNames,
                activeAbsorptionNegotiation = activeNegotiation,
                lastOffseasonPoachSummary = root.optString("lastOffseasonPoachSummary", ""),
                lastActionFeedback = root.optString("lastActionFeedback", ""),
                historyLog = historyLog
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveStadiumDealState(state: StadiumDealState) {
        val root = JSONObject().apply {
            put("lastTickedSeasonYear", state.lastTickedSeasonYear)
            put("lastDecisionSummary", state.lastDecisionSummary)
            put("historyLog", JSONArray(state.historyLog.take(20)))
            state.activeUltimatum?.let { u ->
                put("activeUltimatum", ultimatumToJson(u))
            }
            val queueArr = JSONArray()
            state.pendingUltimatumQueue.forEach { queueArr.put(ultimatumToJson(it)) }
            put("pendingUltimatumQueue", queueArr)
            state.activeRelocationVote?.let { v ->
                put("activeRelocationVote", JSONObject().apply {
                    put("id", v.id)
                    put("ultimatumId", v.ultimatumId)
                    put("franchiseId", v.franchiseId)
                    put("franchiseName", v.franchiseName)
                    put("currentCityId", v.currentCityId)
                    put("currentCityName", v.currentCityName)
                    put("ownerName", v.ownerName)
                    put("ownershipGroupName", v.ownershipGroupName)
                    put("franchiseColorHex", v.franchiseColorHex)
                    put("seasonYear", v.seasonYear)
                    put("stadiumAge", v.stadiumAge)
                    put("stadiumCondition", v.stadiumCondition)
                    put("selectedCityId", v.selectedCityId)
                    val dealsArr = JSONArray()
                    v.cityDeals.forEach { d ->
                        dealsArr.put(JSONObject().apply {
                            put("cityId", d.cityId)
                            put("cityName", d.cityName)
                            put("region", d.region)
                            put("isInternational", d.isInternational)
                            put("populationMillions", d.populationMillions)
                            put("arenaName", d.arenaName)
                            put("newStadiumCapacity", d.newStadiumCapacity)
                            put("publicContributionPct", d.publicContributionPct)
                            put("newFacilityValueMillions", d.newFacilityValueMillions)
                            put("relocationFeeMillions", d.relocationFeeMillions)
                            put("projectedSponsorRevenueMillions", d.projectedSponsorRevenueMillions)
                            put("projectedFanBaseMillions", d.projectedFanBaseMillions)
                            put("dealPitch", d.dealPitch)
                        })
                    }
                    put("cityDeals", dealsArr)
                })
            }
            state.activeNamingRightsOffer?.let { o ->
                put("activeNamingRightsOffer", namingOfferSessionToJson(o))
            }
            val nrQueueArr = JSONArray()
            state.pendingNamingRightsQueue.forEach { nrQueueArr.put(namingOfferSessionToJson(it)) }
            put("pendingNamingRightsQueue", nrQueueArr)
            state.activeBankruptcyScandal?.let { s ->
                put("activeBankruptcyScandal", scandalEventToJson(s))
            }
            val scQueueArr = JSONArray()
            state.pendingBankruptcyScandalQueue.forEach { scQueueArr.put(scandalEventToJson(it)) }
            put("pendingBankruptcyScandalQueue", scQueueArr)
        }
        settingsPrefs.edit().putString("stadium_deal_state_json", root.toString()).apply()
    }

    fun loadStadiumDealState(): StadiumDealState? {
        val raw = settingsPrefs.getString("stadium_deal_state_json", null) ?: return null
        return try {
            val root = JSONObject(raw)
            val activeUlt = root.optJSONObject("activeUltimatum")?.let { jsonToUltimatum(it) }
            val qArr = root.optJSONArray("pendingUltimatumQueue") ?: JSONArray()
            val queue = (0 until qArr.length()).mapNotNull { i ->
                qArr.optJSONObject(i)?.let { jsonToUltimatum(it) }
            }
            val voteObj = root.optJSONObject("activeRelocationVote")?.let { v ->
                val dArr = v.optJSONArray("cityDeals") ?: JSONArray()
                val deals = (0 until dArr.length()).mapNotNull { i ->
                    val d = dArr.optJSONObject(i) ?: return@mapNotNull null
                    RelocationDealOffer(
                        cityId = d.optString("cityId"),
                        cityName = d.optString("cityName"),
                        region = d.optString("region"),
                        isInternational = d.optBoolean("isInternational", false),
                        populationMillions = d.optDouble("populationMillions", 2.0),
                        arenaName = d.optString("arenaName", "Municipal Arena"),
                        newStadiumCapacity = d.optInt("newStadiumCapacity", 21000),
                        publicContributionPct = d.optInt("publicContributionPct", 75),
                        newFacilityValueMillions = d.optLong("newFacilityValueMillions", 420L),
                        relocationFeeMillions = d.optLong("relocationFeeMillions", 15L),
                        projectedSponsorRevenueMillions = d.optLong("projectedSponsorRevenueMillions", 32L),
                        projectedFanBaseMillions = d.optDouble("projectedFanBaseMillions", 2.0),
                        dealPitch = d.optString("dealPitch", "")
                    )
                }
                RelocationVoteState(
                    id = v.optString("id"),
                    ultimatumId = v.optString("ultimatumId"),
                    franchiseId = v.optString("franchiseId"),
                    franchiseName = v.optString("franchiseName"),
                    currentCityId = v.optString("currentCityId"),
                    currentCityName = v.optString("currentCityName"),
                    ownerName = v.optString("ownerName"),
                    ownershipGroupName = v.optString("ownershipGroupName"),
                    franchiseColorHex = v.optLong("franchiseColorHex", 0xFFF59E0BL),
                    seasonYear = v.optInt("seasonYear", 2026),
                    stadiumAge = v.optInt("stadiumAge", 26),
                    stadiumCondition = v.optInt("stadiumCondition", 46),
                    cityDeals = deals,
                    selectedCityId = v.optString("selectedCityId", deals.firstOrNull()?.cityId ?: "")
                )
            }
            val activeNamingOffer = root.optJSONObject("activeNamingRightsOffer")?.let { jsonToNamingOfferSession(it) }
            val nrQArr = root.optJSONArray("pendingNamingRightsQueue") ?: JSONArray()
            val namingQueue = (0 until nrQArr.length()).mapNotNull { i ->
                nrQArr.optJSONObject(i)?.let { jsonToNamingOfferSession(it) }
            }
            val activeScandal = root.optJSONObject("activeBankruptcyScandal")?.let { jsonToScandalEvent(it) }
            val scQArr = root.optJSONArray("pendingBankruptcyScandalQueue") ?: JSONArray()
            val scandalQueue = (0 until scQArr.length()).mapNotNull { i ->
                scQArr.optJSONObject(i)?.let { jsonToScandalEvent(it) }
            }
            val histArr = root.optJSONArray("historyLog") ?: JSONArray()
            val hist = (0 until histArr.length()).mapNotNull { i -> histArr.optString(i).takeIf { it.isNotBlank() } }
            StadiumDealState(
                activeUltimatum = activeUlt,
                pendingUltimatumQueue = queue,
                activeRelocationVote = voteObj,
                activeNamingRightsOffer = activeNamingOffer,
                pendingNamingRightsQueue = namingQueue,
                activeBankruptcyScandal = activeScandal,
                pendingBankruptcyScandalQueue = scandalQueue,
                lastTickedSeasonYear = root.optInt("lastTickedSeasonYear", 0),
                lastDecisionSummary = root.optString("lastDecisionSummary", "Monitoring municipal arena leases and stadium conditions across all franchises."),
                historyLog = hist
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun namingOfferSessionToJson(s: NamingRightsOfferSession): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("franchiseId", s.franchiseId)
        put("franchiseName", s.franchiseName)
        put("cityName", s.cityName)
        put("franchiseColorHex", s.franchiseColorHex)
        put("seasonYear", s.seasonYear)
        put("triggerReason", s.triggerReason.name)
        put("previousSponsorName", s.previousSponsorName ?: "")
        put("teamWins", s.teamWins)
        put("teamLosses", s.teamLosses)
        put("scandalPrDrain", s.scandalPrDrain)
        val offersArr = JSONArray()
        s.offers.forEach { o ->
            offersArr.put(JSONObject().apply {
                put("id", o.id)
                put("sponsorName", o.sponsorName)
                put("tier", o.tier)
                put("termYears", o.termYears)
                put("annualRevenueMillions", o.annualRevenueMillions)
                put("scandalRiskPct", o.scandalRiskPct)
                put("requiresWinningRecord", o.requiresWinningRecord)
                put("isEligible", o.isEligible)
                put("ineligibilityReason", o.ineligibilityReason ?: "")
                put("tagline", o.tagline)
                put("tierTitle", o.tierTitle)
                put("riskSummary", o.riskSummary)
            })
        }
        put("offers", offersArr)
    }

    private fun jsonToNamingOfferSession(obj: JSONObject): NamingRightsOfferSession {
        val reason = try {
            NamingRightsTriggerReason.valueOf(obj.optString("triggerReason", NamingRightsTriggerReason.NEW_STADIUM_BUILT.name))
        } catch (_: Exception) {
            NamingRightsTriggerReason.NEW_STADIUM_BUILT
        }
        val offersArr = obj.optJSONArray("offers") ?: JSONArray()
        val offers = (0 until offersArr.length()).mapNotNull { i ->
            val o = offersArr.optJSONObject(i) ?: return@mapNotNull null
            NamingRightsSponsorOffer(
                id = o.optString("id"),
                sponsorName = o.optString("sponsorName"),
                tier = o.optInt("tier", 1),
                termYears = o.optInt("termYears", 3),
                annualRevenueMillions = o.optLong("annualRevenueMillions", 5L),
                scandalRiskPct = o.optInt("scandalRiskPct", 0),
                requiresWinningRecord = o.optBoolean("requiresWinningRecord", false),
                isEligible = o.optBoolean("isEligible", true),
                ineligibilityReason = o.optString("ineligibilityReason", "").takeIf { it.isNotBlank() },
                tagline = o.optString("tagline", ""),
                tierTitle = o.optString("tierTitle", ""),
                riskSummary = o.optString("riskSummary", "")
            )
        }
        return NamingRightsOfferSession(
            id = obj.optString("id"),
            franchiseId = obj.optString("franchiseId"),
            franchiseName = obj.optString("franchiseName"),
            cityName = obj.optString("cityName"),
            franchiseColorHex = obj.optLong("franchiseColorHex", 0xFFF59E0BL),
            seasonYear = obj.optInt("seasonYear", 2026),
            triggerReason = reason,
            previousSponsorName = obj.optString("previousSponsorName", "").takeIf { it.isNotBlank() },
            teamWins = obj.optInt("teamWins", 0),
            teamLosses = obj.optInt("teamLosses", 0),
            offers = offers,
            scandalPrDrain = obj.optInt("scandalPrDrain", 0)
        )
    }

    private fun scandalEventToJson(e: SponsorBankruptcyScandalEvent): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("franchiseId", e.franchiseId)
        put("franchiseName", e.franchiseName)
        put("cityName", e.cityName)
        put("bankruptSponsorName", e.bankruptSponsorName)
        put("lostAnnualRevenueMillions", e.lostAnnualRevenueMillions)
        put("seasonYear", e.seasonYear)
        put("leaguePrDrain", e.leaguePrDrain)
        put("fanMoraleDrain", e.fanMoraleDrain)
        put("headline", e.headline)
        put("description", e.description)
    }

    private fun jsonToScandalEvent(obj: JSONObject): SponsorBankruptcyScandalEvent = SponsorBankruptcyScandalEvent(
        id = obj.optString("id"),
        franchiseId = obj.optString("franchiseId"),
        franchiseName = obj.optString("franchiseName"),
        cityName = obj.optString("cityName"),
        bankruptSponsorName = obj.optString("bankruptSponsorName"),
        lostAnnualRevenueMillions = obj.optLong("lostAnnualRevenueMillions", 20L),
        seasonYear = obj.optInt("seasonYear", 2026),
        leaguePrDrain = obj.optInt("leaguePrDrain", 9),
        fanMoraleDrain = obj.optInt("fanMoraleDrain", 4),
        headline = obj.optString("headline"),
        description = obj.optString("description")
    )

    private fun ultimatumToJson(u: StadiumUltimatumEvent): JSONObject = JSONObject().apply {
        put("id", u.id)
        put("franchiseId", u.franchiseId)
        put("franchiseName", u.franchiseName)
        put("cityName", u.cityName)
        put("ownerName", u.ownerName)
        put("ownershipGroupName", u.ownershipGroupName)
        put("franchiseColorHex", u.franchiseColorHex)
        put("stadiumAge", u.stadiumAge)
        put("stadiumCondition", u.stadiumCondition)
        put("stadiumCapacity", u.stadiumCapacity)
        put("seasonYear", u.seasonYear)
        put("subsidyCostMillions", u.subsidyCostMillions)
        put("headline", u.headline)
        put("description", u.description)
    }

    private fun jsonToUltimatum(o: JSONObject): StadiumUltimatumEvent = StadiumUltimatumEvent(
        id = o.optString("id"),
        franchiseId = o.optString("franchiseId"),
        franchiseName = o.optString("franchiseName"),
        cityName = o.optString("cityName"),
        ownerName = o.optString("ownerName"),
        ownershipGroupName = o.optString("ownershipGroupName"),
        franchiseColorHex = o.optLong("franchiseColorHex", 0xFFF59E0BL),
        stadiumAge = o.optInt("stadiumAge", 26),
        stadiumCondition = o.optInt("stadiumCondition", 46),
        stadiumCapacity = o.optInt("stadiumCapacity", 20000),
        seasonYear = o.optInt("seasonYear", 2026),
        subsidyCostMillions = o.optLong("subsidyCostMillions", 28L),
        headline = o.optString("headline"),
        description = o.optString("description")
    )

    fun saveFanSentimentState(state: FanSentimentState) {
        val root = JSONObject().apply {
            put("casualSentimentScore", state.casualSentimentScore)
            put("dieHardSentimentScore", state.dieHardSentimentScore)
            put("lastCasualDelta", state.lastCasualDelta)
            put("lastDieHardDelta", state.lastDieHardDelta)
            put("lastPulseNote", state.lastPulseNote)

            val socialArr = JSONArray()
            state.socialFeed.take(30).forEach { post ->
                socialArr.put(JSONObject().apply {
                    put("id", post.id)
                    put("handle", post.handle)
                    put("displayName", post.displayName)
                    put("segment", post.segment.name)
                    put("tone", post.tone.name)
                    put("body", post.body)
                    put("hashtags", JSONArray(post.hashtags))
                    put("likesCount", post.likesCount)
                    put("repostsCount", post.repostsCount)
                    put("timestampLabel", post.timestampLabel)
                    put("category", post.category.name)
                    put("sentimentDelta", post.sentimentDelta)
                })
            }
            put("socialFeed", socialArr)

            val radioArr = JSONArray()
            state.radioSegments.take(16).forEach { seg ->
                radioArr.put(JSONObject().apply {
                    put("id", seg.id)
                    put("showName", seg.showName)
                    put("hostNames", seg.hostNames)
                    put("callerName", seg.callerName)
                    put("callerCity", seg.callerCity)
                    put("callerSegment", seg.callerSegment.name)
                    put("segmentTitle", seg.segmentTitle)
                    put("hostTake", seg.hostTake)
                    put("callerQuote", seg.callerQuote)
                    put("casualApprovalPct", seg.casualApprovalPct)
                    put("dieHardApprovalPct", seg.dieHardApprovalPct)
                    put("heatednessScore", seg.heatednessScore)
                    put("tone", seg.tone.name)
                    put("timestampLabel", seg.timestampLabel)
                    put("category", seg.category.name)
                })
            }
            put("radioSegments", radioArr)

            val paperArr = JSONArray()
            state.newspaperHeadlines.take(16).forEach { item ->
                paperArr.put(JSONObject().apply {
                    put("id", item.id)
                    put("publicationName", item.publicationName)
                    put("sectionKicker", item.sectionKicker)
                    put("headline", item.headline)
                    put("subheadline", item.subheadline)
                    put("byline", item.byline)
                    put("leadParagraph", item.leadParagraph)
                    put("tone", item.tone.name)
                    put("timestampLabel", item.timestampLabel)
                    put("category", item.category.name)
                    put("relatedEntity", item.relatedEntity ?: "")
                })
            }
            put("newspaperHeadlines", paperArr)

            val recArr = JSONArray()
            state.decisionReactions.take(24).forEach { rec ->
                recArr.put(JSONObject().apply {
                    put("id", rec.id)
                    put("category", rec.category.name)
                    put("decisionTitle", rec.decisionTitle)
                    put("summary", rec.summary)
                    put("casualDelta", rec.casualDelta)
                    put("dieHardDelta", rec.dieHardDelta)
                    put("casualScoreAfter", rec.casualScoreAfter)
                    put("dieHardScoreAfter", rec.dieHardScoreAfter)
                    put("timestampLabel", rec.timestampLabel)
                })
            }
            put("decisionReactions", recArr)
        }
        settingsPrefs.edit().putString("fan_sentiment_state_json", root.toString()).apply()
    }

    fun loadFanSentimentState(): FanSentimentState? {
        val raw = settingsPrefs.getString("fan_sentiment_state_json", null) ?: return null
        return try {
            val root = JSONObject(raw)
            val socialArr = root.optJSONArray("socialFeed") ?: JSONArray()
            val socialFeed = (0 until socialArr.length()).mapNotNull { i ->
                val o = socialArr.optJSONObject(i) ?: return@mapNotNull null
                val hashArr = o.optJSONArray("hashtags") ?: JSONArray()
                val hashtags = (0 until hashArr.length()).mapNotNull { j ->
                    hashArr.optString(j).takeIf { it.isNotBlank() }
                }
                SocialMediaPostItem(
                    id = o.optString("id"),
                    handle = o.optString("handle", "@LeagueFan"),
                    displayName = o.optString("displayName", "Fan"),
                    segment = runCatching { FanSegmentType.valueOf(o.optString("segment")) }.getOrDefault(FanSegmentType.CASUAL),
                    tone = runCatching { MediaReactionTone.valueOf(o.optString("tone")) }.getOrDefault(MediaReactionTone.ANALYTICAL),
                    body = o.optString("body"),
                    hashtags = hashtags,
                    likesCount = o.optInt("likesCount", 100),
                    repostsCount = o.optInt("repostsCount", 25),
                    timestampLabel = o.optString("timestampLabel", "S2026 • W1"),
                    category = runCatching { FanDecisionCategory.valueOf(o.optString("category")) }.getOrDefault(FanDecisionCategory.WEEKLY_SIM),
                    sentimentDelta = o.optInt("sentimentDelta", 0)
                )
            }
            val radioArr = root.optJSONArray("radioSegments") ?: JSONArray()
            val radioSegments = (0 until radioArr.length()).mapNotNull { i ->
                val o = radioArr.optJSONObject(i) ?: return@mapNotNull null
                SportsTalkRadioSegment(
                    id = o.optString("id"),
                    showName = o.optString("showName", "Sports Talk 104.3 FM"),
                    hostNames = o.optString("hostNames", "Marcus & Coach Ray"),
                    callerName = o.optString("callerName", "Caller"),
                    callerCity = o.optString("callerCity", "Metro"),
                    callerSegment = runCatching { FanSegmentType.valueOf(o.optString("callerSegment")) }.getOrDefault(FanSegmentType.DIE_HARD),
                    segmentTitle = o.optString("segmentTitle"),
                    hostTake = o.optString("hostTake"),
                    callerQuote = o.optString("callerQuote"),
                    casualApprovalPct = o.optInt("casualApprovalPct", 74),
                    dieHardApprovalPct = o.optInt("dieHardApprovalPct", 76),
                    heatednessScore = o.optInt("heatednessScore", 50),
                    tone = runCatching { MediaReactionTone.valueOf(o.optString("tone")) }.getOrDefault(MediaReactionTone.DEBATE),
                    timestampLabel = o.optString("timestampLabel", "S2026 • W1"),
                    category = runCatching { FanDecisionCategory.valueOf(o.optString("category")) }.getOrDefault(FanDecisionCategory.WEEKLY_SIM)
                )
            }
            val paperArr = root.optJSONArray("newspaperHeadlines") ?: JSONArray()
            val newspaperHeadlines = (0 until paperArr.length()).mapNotNull { i ->
                val o = paperArr.optJSONObject(i) ?: return@mapNotNull null
                NewspaperHeadlineItem(
                    id = o.optString("id"),
                    publicationName = o.optString("publicationName", "THE NATIONAL SPORTS CHRONICLE"),
                    sectionKicker = o.optString("sectionKicker", "FRONT PAGE"),
                    headline = o.optString("headline"),
                    subheadline = o.optString("subheadline"),
                    byline = o.optString("byline"),
                    leadParagraph = o.optString("leadParagraph"),
                    tone = runCatching { MediaReactionTone.valueOf(o.optString("tone")) }.getOrDefault(MediaReactionTone.ANALYTICAL),
                    timestampLabel = o.optString("timestampLabel", "S2026 • W1"),
                    category = runCatching { FanDecisionCategory.valueOf(o.optString("category")) }.getOrDefault(FanDecisionCategory.WEEKLY_SIM),
                    relatedEntity = o.optString("relatedEntity").takeIf { it.isNotBlank() }
                )
            }
            val recArr = root.optJSONArray("decisionReactions") ?: JSONArray()
            val decisionReactions = (0 until recArr.length()).mapNotNull { i ->
                val o = recArr.optJSONObject(i) ?: return@mapNotNull null
                FanDecisionReactionRecord(
                    id = o.optString("id"),
                    category = runCatching { FanDecisionCategory.valueOf(o.optString("category")) }.getOrDefault(FanDecisionCategory.WEEKLY_SIM),
                    decisionTitle = o.optString("decisionTitle"),
                    summary = o.optString("summary"),
                    casualDelta = o.optInt("casualDelta", 0),
                    dieHardDelta = o.optInt("dieHardDelta", 0),
                    casualScoreAfter = o.optInt("casualScoreAfter", 74),
                    dieHardScoreAfter = o.optInt("dieHardScoreAfter", 76),
                    timestampLabel = o.optString("timestampLabel", "S2026 • W1")
                )
            }

            FanSentimentState(
                casualSentimentScore = root.optInt("casualSentimentScore", 74).coerceIn(10, 100),
                dieHardSentimentScore = root.optInt("dieHardSentimentScore", 76).coerceIn(10, 100),
                lastCasualDelta = root.optInt("lastCasualDelta", 0),
                lastDieHardDelta = root.optInt("lastDieHardDelta", 0),
                socialFeed = socialFeed,
                radioSegments = radioSegments,
                newspaperHeadlines = newspaperHeadlines,
                decisionReactions = decisionReactions,
                lastPulseNote = root.optString("lastPulseNote", "Fan sentiment restored.")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveDashboardTrends(trends: List<DashboardTrendPoint>) {
        val arr = JSONArray()
        trends.takeLast(24).forEach { pt ->
            arr.put(JSONObject().apply {
                put("id", pt.id)
                put("periodLabel", pt.periodLabel)
                put("seasonYear", pt.seasonYear)
                put("weekNumber", pt.weekNumber)
                put("leagueValuationMillions", pt.leagueValuationMillions)
                put("cashReservesMillions", pt.cashReservesMillions)
                put("financialGrowthPct", pt.financialGrowthPct.toDouble())
                put("fanSentimentPct", pt.fanSentimentPct)
                put("avgMoralePct", pt.avgMoralePct)
                put("avgLoyaltyPct", pt.avgLoyaltyPct)
                put("totalFanbaseMillions", pt.totalFanbaseMillions)
                put("leagueRevenueMillions", pt.leagueRevenueMillions)
                put("tvBroadcastRating", pt.tvBroadcastRating)
                put("playerSatisfactionPct", pt.playerSatisfactionPct)
            })
        }
        settingsPrefs.edit().putString("dashboard_trends_json", arr.toString()).apply()
    }

    fun loadDashboardTrends(): List<DashboardTrendPoint> {
        val raw = settingsPrefs.getString("dashboard_trends_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                DashboardTrendPoint(
                    id = o.optString("id"),
                    periodLabel = o.optString("periodLabel", "W1"),
                    seasonYear = o.optInt("seasonYear", 2026),
                    weekNumber = o.optInt("weekNumber", 1),
                    leagueValuationMillions = o.optLong("leagueValuationMillions", 420L),
                    cashReservesMillions = o.optLong("cashReservesMillions", 95L),
                    financialGrowthPct = o.optDouble("financialGrowthPct", 0.0).toFloat(),
                    fanSentimentPct = o.optInt("fanSentimentPct", 75),
                    avgMoralePct = o.optInt("avgMoralePct", 75),
                    avgLoyaltyPct = o.optInt("avgLoyaltyPct", 75),
                    totalFanbaseMillions = o.optDouble("totalFanbaseMillions", 18.2),
                    leagueRevenueMillions = o.optLong("leagueRevenueMillions", 8L),
                    tvBroadcastRating = o.optDouble("tvBroadcastRating", 4.2),
                    playerSatisfactionPct = o.optInt("playerSatisfactionPct", 78)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    data class LaborCbaExtendedSnapshot(
        val cbaYearsRemaining: Int,
        val isLockoutActive: Boolean,
        val lockoutWeeksLost: Int,
        val lastLockoutSeasonYear: Int,
        val negotiatedRosterRule: RosterSizeRule,
        val mandatoryPrivateCharterFlights: Boolean,
        val maxBackToBackGamesLimit: Boolean,
        val independentConcussionProtocol: Boolean,
        val advancedHybridTurfMandate: Boolean,
        val postCareerTransitionFundMillions: Long,
        val fiveStarHotelSingleRooms: Boolean,
        val minPlayerSalaryK: Long,
        val playerHealthWelfareMillions: Long
    )

    fun saveLaborCbaExtendedState(union: LaborUnionAgreement) {
        val root = JSONObject().apply {
            put("cbaYearsRemaining", union.cbaYearsRemaining)
            put("isLockoutActive", union.isLockoutActive)
            put("lockoutWeeksLost", union.lockoutWeeksLost)
            put("lastLockoutSeasonYear", union.lastLockoutSeasonYear)
            put("negotiatedRosterRule", union.negotiatedRosterRule.name)
            put("mandatoryPrivateCharterFlights", union.mandatoryPrivateCharterFlights)
            put("maxBackToBackGamesLimit", union.maxBackToBackGamesLimit)
            put("independentConcussionProtocol", union.independentConcussionProtocol)
            put("advancedHybridTurfMandate", union.advancedHybridTurfMandate)
            put("postCareerTransitionFundMillions", union.postCareerTransitionFundMillions)
            put("fiveStarHotelSingleRooms", union.fiveStarHotelSingleRooms)
            put("minPlayerSalaryK", union.minPlayerSalaryK)
            put("playerHealthWelfareMillions", union.playerHealthWelfareMillions)
        }
        settingsPrefs.edit().putString("labor_cba_extended_json", root.toString()).apply()
    }

    fun loadLaborCbaExtendedState(): LaborCbaExtendedSnapshot? {
        val raw = settingsPrefs.getString("labor_cba_extended_json", null) ?: return null
        return try {
            val o = JSONObject(raw)
            LaborCbaExtendedSnapshot(
                cbaYearsRemaining = o.optInt("cbaYearsRemaining", 3).coerceIn(0, 10),
                isLockoutActive = o.optBoolean("isLockoutActive", false),
                lockoutWeeksLost = o.optInt("lockoutWeeksLost", 0).coerceAtLeast(0),
                lastLockoutSeasonYear = o.optInt("lastLockoutSeasonYear", 0),
                negotiatedRosterRule = runCatching { RosterSizeRule.valueOf(o.optString("negotiatedRosterRule")) }
                    .getOrDefault(RosterSizeRule.STANDARD_15),
                mandatoryPrivateCharterFlights = o.optBoolean("mandatoryPrivateCharterFlights", true),
                maxBackToBackGamesLimit = o.optBoolean("maxBackToBackGamesLimit", true),
                independentConcussionProtocol = o.optBoolean("independentConcussionProtocol", true),
                advancedHybridTurfMandate = o.optBoolean("advancedHybridTurfMandate", false),
                postCareerTransitionFundMillions = o.optLong("postCareerTransitionFundMillions", 5L),
                fiveStarHotelSingleRooms = o.optBoolean("fiveStarHotelSingleRooms", true),
                minPlayerSalaryK = o.optLong("minPlayerSalaryK", 650L),
                playerHealthWelfareMillions = o.optLong("playerHealthWelfareMillions", 15L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveBroadcastExclusivityState(
        contract: BroadcastContract,
        session: BroadcastBiddingSession
    ) {
        val root = JSONObject().apply {
            put("hasExclusivity", contract.hasExclusivity)
            put("exclusivityLostPenaltyPct", contract.exclusivityLostPenaltyPct)
            put("networkScope", contract.networkScope.name)
            put("reachMultiplier", contract.reachMultiplier)
            put("bonusPerPlayoffGameMillions", contract.bonusPerPlayoffGameMillions)
            put("biddingRound", session.biddingRound)
            put("exclusivityRoundsTotal", session.exclusivityRoundsTotal)
            put("exclusivityRoundsRemaining", session.exclusivityRoundsRemaining)
            put("exclusivityExpired", session.exclusivityExpired)
            put("lastPeriodicBidWeek", session.lastPeriodicBidWeek)
            put("periodicBidsReceivedCount", session.periodicBidsReceivedCount)
            put("lastEscalationSummary", session.lastEscalationSummary ?: "")
            put("selectedOfferId", session.selectedOfferId ?: "")
            put("auctionLog", JSONArray(session.auctionLog.take(15)))
        }
        settingsPrefs.edit().putString("broadcast_exclusivity_json", root.toString()).apply()
    }

    fun applyLoadedBroadcastExclusivity(
        baseContract: BroadcastContract,
        bidders: List<NetworkBidder>,
        teamCount: Int,
        playerRevenueSharePct: Int,
        fanbaseMillions: Double,
        currentYear: Int
    ): Pair<BroadcastContract, BroadcastBiddingSession?> {
        val raw = settingsPrefs.getString("broadcast_exclusivity_json", null) ?: return baseContract to null
        return try {
            val o = JSONObject(raw)
            val restoredContract = baseContract.copy(
                hasExclusivity = o.optBoolean("hasExclusivity", true),
                exclusivityLostPenaltyPct = o.optInt("exclusivityLostPenaltyPct", 0),
                networkScope = runCatching { NetworkScope.valueOf(o.optString("networkScope")) }
                    .getOrDefault(baseContract.networkScope),
                reachMultiplier = o.optDouble("reachMultiplier", baseContract.reachMultiplier),
                bonusPerPlayoffGameMillions = o.optLong("bonusPerPlayoffGameMillions", baseContract.bonusPerPlayoffGameMillions)
            )
            var session = BroadcastBiddingEngine.createInitialSession(
                bidders = bidders,
                currentContract = restoredContract,
                teamCount = teamCount,
                playerRevenueSharePct = playerRevenueSharePct,
                currentFanbaseMillions = fanbaseMillions,
                currentYear = currentYear
            )
            val roundsRemaining = o.optInt("exclusivityRoundsRemaining", 3).coerceIn(0, 5)
            val expired = o.optBoolean("exclusivityExpired", false)
            if (expired) {
                session = BroadcastBiddingEngine.applyExclusivityExpiryPenalty(
                    session = session,
                    currentContract = restoredContract,
                    teamCount = teamCount,
                    playerRevenueSharePct = playerRevenueSharePct,
                    currentFanbaseMillions = fanbaseMillions,
                    currentYear = currentYear
                )
            }
            val logArr = o.optJSONArray("auctionLog") ?: JSONArray()
            val logs = (0 until logArr.length()).mapNotNull { idx -> logArr.optString(idx).takeIf { it.isNotBlank() } }
            val restoredSession = session.copy(
                biddingRound = o.optInt("biddingRound", 1).coerceAtLeast(1),
                exclusivityRoundsTotal = o.optInt("exclusivityRoundsTotal", 3).coerceAtLeast(1),
                exclusivityRoundsRemaining = roundsRemaining,
                exclusivityExpired = expired,
                lastPeriodicBidWeek = o.optInt("lastPeriodicBidWeek", 0),
                periodicBidsReceivedCount = o.optInt("periodicBidsReceivedCount", 0),
                lastEscalationSummary = o.optString("lastEscalationSummary").takeIf { it.isNotBlank() },
                selectedOfferId = o.optString("selectedOfferId").takeIf { it.isNotBlank() } ?: session.selectedOfferId,
                auctionLog = logs.ifEmpty { session.auctionLog }
            )
            restoredContract to restoredSession
        } catch (_: Exception) {
            baseContract to null
        }
    }
}

