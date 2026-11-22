package com.example.model

import kotlin.math.roundToLong
import kotlin.random.Random

enum class HolidayTentpole(
    val displayName: String,
    val shortBadge: String,
    val description: String,
    val tvBonusMillions: Double,
    val gateMultiplier: Double,
    val moraleBonus: Int,
    val defaultProgressFraction: Double
) {
    NONE(
        displayName = "Standard Slate",
        shortBadge = "Regular",
        description = "Standard regular-season league broadcast window.",
        tvBonusMillions = 0.0,
        gateMultiplier = 1.0,
        moraleBonus = 0,
        defaultProgressFraction = 0.5
    ),
    OPENING_NIGHT(
        displayName = "Opening Night Primetime",
        shortBadge = "Opening Night",
        description = "Nationally televised season-opening banner & rivalry showcase.",
        tvBonusMillions = 3.5,
        gateMultiplier = 1.28,
        moraleBonus = 3,
        defaultProgressFraction = 0.0
    ),
    THANKSGIVING_SLATE(
        displayName = "Thanksgiving Harvest Classic",
        shortBadge = "Thanksgiving",
        description = "Holiday tripleheader anchored by historic regional rivalries.",
        tvBonusMillions = 4.2,
        gateMultiplier = 1.32,
        moraleBonus = 3,
        defaultProgressFraction = 0.36
    ),
    CHRISTMAS_SHOWCASE(
        displayName = "Christmas Day Marquee",
        shortBadge = "Christmas Day",
        description = "Crown-jewel holiday national broadcast commanding peak ad rates.",
        tvBonusMillions = 5.5,
        gateMultiplier = 1.40,
        moraleBonus = 4,
        defaultProgressFraction = 0.58
    ),
    NEW_YEAR_CLASSIC(
        displayName = "New Year's Winter Derby",
        shortBadge = "New Year's",
        description = "Landmark New Year's holiday spectacle with elevated gate & viewership.",
        tvBonusMillions = 4.5,
        gateMultiplier = 1.34,
        moraleBonus = 3,
        defaultProgressFraction = 0.68
    ),
    MLK_HERITAGE_DAY(
        displayName = "Heritage & Unity Classic",
        shortBadge = "Heritage Day",
        description = "High-profile national holiday showcase spotlighting marquee stars.",
        tvBonusMillions = 3.8,
        gateMultiplier = 1.26,
        moraleBonus = 3,
        defaultProgressFraction = 0.78
    ),
    RIVALRY_FINALE(
        displayName = "Holiday Rivalry Finale",
        shortBadge = "Rivalry Finale",
        description = "Playoff-seeding regular season closing slate of fierce rivalries.",
        tvBonusMillions = 4.8,
        gateMultiplier = 1.36,
        moraleBonus = 4,
        defaultProgressFraction = 1.0
    );

    companion object {
        val selectableHolidays: List<HolidayTentpole>
            get() = values().filter { it != NONE }
    }
}

data class InternationalVenue(
    val id: String,
    val cityName: String,
    val country: String,
    val venueName: String,
    val regionLabel: String,
    val tvBonusMillions: Double,
    val gateMultiplier: Double,
    val fanbaseGainMillions: Double,
    val travelFatigueLoad: Int
) {
    val shortLabel: String
        get() = "$cityName Global Series"

    val fullVenueLabel: String
        get() = "$cityName ($venueName)"
}

object InternationalVenueCatalog {
    val allVenues: List<InternationalVenue> = listOf(
        InternationalVenue(
            id = "intl_london",
            cityName = "London",
            country = "United Kingdom",
            venueName = "Wembley / The O2",
            regionLabel = "Europe",
            tvBonusMillions = 4.2,
            gateMultiplier = 1.34,
            fanbaseGainMillions = 0.42,
            travelFatigueLoad = 4
        ),
        InternationalVenue(
            id = "intl_tokyo",
            cityName = "Tokyo",
            country = "Japan",
            venueName = "Tokyo Dome",
            regionLabel = "Asia-Pacific",
            tvBonusMillions = 5.0,
            gateMultiplier = 1.32,
            fanbaseGainMillions = 0.50,
            travelFatigueLoad = 6
        ),
        InternationalVenue(
            id = "intl_mexico_city",
            cityName = "Mexico City",
            country = "Mexico",
            venueName = "Arena CDMX",
            regionLabel = "Latin America",
            tvBonusMillions = 3.6,
            gateMultiplier = 1.28,
            fanbaseGainMillions = 0.38,
            travelFatigueLoad = 2
        ),
        InternationalVenue(
            id = "intl_paris",
            cityName = "Paris",
            country = "France",
            venueName = "Accor Arena",
            regionLabel = "Europe",
            tvBonusMillions = 4.0,
            gateMultiplier = 1.30,
            fanbaseGainMillions = 0.39,
            travelFatigueLoad = 4
        ),
        InternationalVenue(
            id = "intl_munich",
            cityName = "Munich",
            country = "Germany",
            venueName = "Allianz / SAP Garden",
            regionLabel = "Europe",
            tvBonusMillions = 3.5,
            gateMultiplier = 1.29,
            fanbaseGainMillions = 0.34,
            travelFatigueLoad = 4
        ),
        InternationalVenue(
            id = "intl_sao_paulo",
            cityName = "Sao Paulo",
            country = "Brazil",
            venueName = "Allianz Parque",
            regionLabel = "South America",
            tvBonusMillions = 4.1,
            gateMultiplier = 1.31,
            fanbaseGainMillions = 0.44,
            travelFatigueLoad = 5
        ),
        InternationalVenue(
            id = "intl_sydney",
            cityName = "Sydney",
            country = "Australia",
            venueName = "Qudos Bank Arena",
            regionLabel = "Oceania",
            tvBonusMillions = 3.4,
            gateMultiplier = 1.26,
            fanbaseGainMillions = 0.32,
            travelFatigueLoad = 7
        ),
        InternationalVenue(
            id = "intl_montreal",
            cityName = "Montreal",
            country = "Canada",
            venueName = "Bell Centre",
            regionLabel = "North America",
            tvBonusMillions = 2.6,
            gateMultiplier = 1.22,
            fanbaseGainMillions = 0.24,
            travelFatigueLoad = 1
        )
    )

    fun findById(venueId: String?): InternationalVenue? =
        if (venueId.isNullOrBlank()) null else allVenues.firstOrNull { it.id == venueId }
}

data class ScheduledGameSlot(
    val id: String,
    val week: Int,
    val homeTeamId: String,
    val awayTeamId: String,
    val holidayTentpole: HolidayTentpole = HolidayTentpole.NONE,
    val isMarqueeRivalry: Boolean = false,
    val rivalryTitle: String? = null,
    val internationalVenueId: String? = null,
    val internationalVenueLabel: String? = null
) {
    val isHolidayGame: Boolean
        get() = holidayTentpole != HolidayTentpole.NONE

    val isInternationalGame: Boolean
        get() = !internationalVenueId.isNullOrBlank()

    val projectedTvBonusMillions: Double
        get() {
            val hol = if (isHolidayGame) holidayTentpole.tvBonusMillions else 0.0
            val intl = InternationalVenueCatalog.findById(internationalVenueId)?.tvBonusMillions ?: 0.0
            val marquee = if (isMarqueeRivalry) 1.5 else 0.0
            return Math.round((hol + intl + marquee) * 10.0) / 10.0
        }

    val projectedGateMultiplier: Double
        get() {
            val holMult = if (isHolidayGame) holidayTentpole.gateMultiplier else 1.0
            val intlMult = InternationalVenueCatalog.findById(internationalVenueId)?.gateMultiplier ?: 1.0
            val marqueeAdd = if (isMarqueeRivalry) 0.12 else 0.0
            return Math.round((maxOf(holMult, intlMult) + marqueeAdd) * 100.0) / 100.0
        }

    val projectedFanReachGainMillions: Double
        get() {
            val intlReach = InternationalVenueCatalog.findById(internationalVenueId)?.fanbaseGainMillions ?: 0.0
            val holReach = if (isHolidayGame) 0.08 else 0.0
            return Math.round((intlReach + holReach) * 100.0) / 100.0
        }

    fun toGameMatch(
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>
    ): GameMatch {
        val venue = InternationalVenueCatalog.findById(internationalVenueId)
        val effectiveVenueLabel = venue?.fullVenueLabel ?: internationalVenueLabel
        val base = GameMatch(
            id = "match_w${week}_${homeTeamId}_${awayTeamId}",
            week = week,
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            rivalryTitle = rivalryTitle,
            ticketRevenueMultiplier = projectedGateMultiplier,
            moraleMultiplier = if (isHolidayGame || isMarqueeRivalry) 1.12 else 1.0,
            holidayTentpoleLabel = if (isHolidayGame) holidayTentpole.displayName else null,
            isHolidayMarquee = isHolidayGame,
            internationalVenueLabel = effectiveVenueLabel,
            isInternationalShowcase = isInternationalGame,
            scheduleBroadcastBonusMillions = projectedTvBonusMillions,
            scheduleGlobalReachGainMillions = projectedFanReachGainMillions
        )
        val annotated = RivalryEngine.annotateMatchesWithRivalries(
            matches = listOf(base),
            franchises = franchises,
            rivalries = rivalries,
            cities = cities,
            isPlayoffs = false
        ).first()
        return annotated.copy(
            rivalryTitle = annotated.rivalryTitle ?: rivalryTitle,
            ticketRevenueMultiplier = Math.round(maxOf(annotated.ticketRevenueMultiplier, projectedGateMultiplier) * 100.0) / 100.0,
            holidayTentpoleLabel = if (isHolidayGame) holidayTentpole.displayName else null,
            isHolidayMarquee = isHolidayGame,
            internationalVenueLabel = effectiveVenueLabel,
            isInternationalShowcase = isInternationalGame,
            scheduleBroadcastBonusMillions = projectedTvBonusMillions,
            scheduleGlobalReachGainMillions = projectedFanReachGainMillions
        )
    }
}

enum class ScheduleApprovalStatus(val label: String) {
    DRAFT_IN_PROGRESS("Draft — Awaiting Submission"),
    APPROVED_BY_BOARD("Board Approved"),
    APPROVED_BY_OVERRIDE("Enacted via Executive Override"),
    REJECTED_REVISION_REQUIRED("Board Vetoed — Revise")
}

data class SeasonSchedulePlan(
    val targetYear: Int,
    val regularSeasonWeeks: Int = 14,
    val slots: List<ScheduledGameSlot> = emptyList(),
    val approvalStatus: ScheduleApprovalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS,
    val lastVoteSummary: String = "Draft pending submission to Board of Governors.",
    val conflictWarnings: List<String> = emptyList(),
    val hasValidationErrors: Boolean = false
) {
    val isApproved: Boolean
        get() = approvalStatus == ScheduleApprovalStatus.APPROVED_BY_BOARD ||
            approvalStatus == ScheduleApprovalStatus.APPROVED_BY_OVERRIDE

    val holidayShowcaseCount: Int
        get() = slots.count { it.isHolidayGame }

    val internationalGameCount: Int
        get() = slots.count { it.isInternationalGame }

    val marqueeRivalryCount: Int
        get() = slots.count { it.isMarqueeRivalry }

    val projectedBroadcastBonusMillions: Double
        get() = Math.round(slots.sumOf { it.projectedTvBonusMillions } * 10.0) / 10.0

    val projectedGlobalReachGainMillions: Double
        get() = Math.round(slots.sumOf { it.projectedFanReachGainMillions } * 100.0) / 100.0

    fun slotsForWeek(week: Int): List<ScheduledGameSlot> =
        slots.filter { it.week == week }
}

data class SeasonScheduleState(
    val upcomingPlan: SeasonSchedulePlan? = null,
    val activeSeasonSchedule: SeasonSchedulePlan? = null,
    val pastSeasonPlans: List<SeasonSchedulePlan> = emptyList()
)

object ScheduleCalendarEngine {

    val ALLOWED_SEASON_LENGTHS: List<Int> = listOf(14, 16, 18, 20, 22)

    fun allStarWeekFor(regularSeasonWeeks: Int): Int {
        val clamped = regularSeasonWeeks.coerceIn(14, 22)
        return (clamped / 2).coerceIn(4, clamped - 2)
    }

    fun totalWeeksFor(regularSeasonWeeks: Int): Int = regularSeasonWeeks.coerceIn(14, 22) + 2

    fun recommendedWeekForHoliday(tentpole: HolidayTentpole, regularSeasonWeeks: Int): Int {
        val clamped = regularSeasonWeeks.coerceIn(14, 22)
        return when (tentpole) {
            HolidayTentpole.NONE -> 1
            HolidayTentpole.OPENING_NIGHT -> 1
            HolidayTentpole.RIVALRY_FINALE -> clamped
            else -> ((clamped * tentpole.defaultProgressFraction).roundToLong().toInt()).coerceIn(2, clamped - 1)
        }
    }

    fun generateFullSeasonSchedule(
        targetYear: Int,
        regularSeasonWeeks: Int,
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>,
        sportType: SportType
    ): SeasonSchedulePlan {
        val weeks = regularSeasonWeeks.coerceIn(14, 22)
        if (franchises.size < 2) {
            return SeasonSchedulePlan(
                targetYear = targetYear,
                regularSeasonWeeks = weeks,
                slots = emptyList(),
                approvalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS
            )
        }

        val allSlots = mutableListOf<ScheduledGameSlot>()
        for (week in 1..weeks) {
            allSlots.addAll(generateBalancedSlotsForWeek(week, targetYear, franchises, weekOffset = week))
        }

        // Seed 2 automatic Holiday Marquee Rivalry games + 1 International Showcase game
        val seededSlots = seedSignatureMarqueeSlates(
            slots = allSlots,
            regularSeasonWeeks = weeks,
            franchises = franchises,
            rivalries = rivalries,
            sportType = sportType
        )

        return validateAndRecompute(
            plan = SeasonSchedulePlan(
                targetYear = targetYear,
                regularSeasonWeeks = weeks,
                slots = seededSlots,
                approvalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS,
                lastVoteSummary = "Draft generated ($weeks weeks). Customize matchups & submit for Board vote."
            ),
            franchises = franchises,
            unionAgreement = LaborUnionAgreement()
        )
    }

    private fun generateBalancedSlotsForWeek(
        week: Int,
        targetYear: Int,
        franchises: List<Franchise>,
        weekOffset: Int
    ): List<ScheduledGameSlot> {
        val ids = franchises.map { it.id }.toMutableList()
        if (ids.size < 2) return emptyList()

        // Circle-method round robin permutation so each week pairs fresh opponents
        val hasBye = ids.size % 2 != 0
        if (hasBye) ids.add("BYE")
        val n = ids.size
        val round = ((weekOffset - 1 + (targetYear % n)) % (n - 1))
        val rotated = mutableListOf(ids.first())
        val tail = ids.drop(1)
        for (i in tail.indices) {
            rotated.add(tail[(i + round) % tail.size])
        }

        val weekSlots = mutableListOf<ScheduledGameSlot>()
        for (i in 0 until n / 2) {
            val a = rotated[i]
            val b = rotated[n - 1 - i]
            if (a == "BYE" || b == "BYE") continue
            val flipHome = (week + i) % 2 == 1
            val homeId = if (flipHome) b else a
            val awayId = if (flipHome) a else b
            weekSlots.add(
                ScheduledGameSlot(
                    id = "slot_y${targetYear}_w${week}_$i",
                    week = week,
                    homeTeamId = homeId,
                    awayTeamId = awayId
                )
            )
        }
        return weekSlots
    }

    private fun seedSignatureMarqueeSlates(
        slots: List<ScheduledGameSlot>,
        regularSeasonWeeks: Int,
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        sportType: SportType
    ): List<ScheduledGameSlot> {
        val mutable = slots.toMutableList()
        val topRivalry = rivalries.maxByOrNull { it.intensity }

        // 1. Opening Night Marquee (Week 1)
        val w1Idx = if (topRivalry != null) {
            mutable.indexOfFirst {
                it.week == 1 && (
                    (it.homeTeamId == topRivalry.teamAId && it.awayTeamId == topRivalry.teamBId) ||
                        (it.homeTeamId == topRivalry.teamBId && it.awayTeamId == topRivalry.teamAId)
                    )
            }.takeIf { it >= 0 } ?: mutable.indexOfFirst { it.week == 1 }
        } else {
            mutable.indexOfFirst { it.week == 1 }
        }
        if (w1Idx >= 0) {
            val slot = mutable[w1Idx]
            val home = franchises.find { it.id == slot.homeTeamId }
            val away = franchises.find { it.id == slot.awayTeamId }
            mutable[w1Idx] = slot.copy(
                holidayTentpole = HolidayTentpole.OPENING_NIGHT,
                isMarqueeRivalry = true,
                rivalryTitle = topRivalry?.takeIf {
                    (it.teamAId == slot.homeTeamId && it.teamBId == slot.awayTeamId) ||
                        (it.teamBId == slot.homeTeamId && it.teamAId == slot.awayTeamId)
                }?.rivalryName ?: "Opening Night Rivalry: ${home?.nickname ?: "Home"} vs ${away?.nickname ?: "Away"}"
            )
        }

        // 2. Christmas / Midseason Holiday Showcase
        val holidayWeek = recommendedWeekForHoliday(HolidayTentpole.CHRISTMAS_SHOWCASE, regularSeasonWeeks)
        val holIdx = mutable.indexOfFirst { it.week == holidayWeek && !it.isHolidayGame }
        if (holIdx >= 0) {
            val slot = mutable[holIdx]
            val home = franchises.find { it.id == slot.homeTeamId }
            val away = franchises.find { it.id == slot.awayTeamId }
            val matchedRiv = rivalries.firstOrNull {
                (it.teamAId == slot.homeTeamId && it.teamBId == slot.awayTeamId) ||
                    (it.teamBId == slot.homeTeamId && it.teamAId == slot.awayTeamId)
            }
            mutable[holIdx] = slot.copy(
                holidayTentpole = HolidayTentpole.CHRISTMAS_SHOWCASE,
                isMarqueeRivalry = true,
                rivalryTitle = matchedRiv?.rivalryName ?: "Christmas Primetime: ${home?.cityName ?: ""} vs ${away?.cityName ?: ""}"
            )
        }

        // 3. International Showcase Game
        val intlWeek = (allStarWeekFor(regularSeasonWeeks) - 2).coerceIn(2, regularSeasonWeeks - 1)
        val defaultVenue = when (sportType) {
            SportType.FOOTBALL, SportType.SOCCER -> InternationalVenueCatalog.allVenues[0] // London
            SportType.BASKETBALL -> InternationalVenueCatalog.allVenues[3] // Paris
            SportType.HOCKEY -> InternationalVenueCatalog.allVenues[7] // Montreal / International
        }
        val intlIdx = mutable.indexOfFirst { it.week == intlWeek && !it.isHolidayGame }
            .takeIf { it >= 0 } ?: mutable.indexOfFirst { it.week == intlWeek }
        if (intlIdx >= 0) {
            val slot = mutable[intlIdx]
            mutable[intlIdx] = slot.copy(
                internationalVenueId = defaultVenue.id,
                internationalVenueLabel = defaultVenue.fullVenueLabel
            )
        }

        return mutable
    }

    fun resizeScheduleForSeasonLength(
        plan: SeasonSchedulePlan,
        newRegularSeasonWeeks: Int,
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        unionAgreement: LaborUnionAgreement
    ): SeasonSchedulePlan {
        val targetWeeks = newRegularSeasonWeeks.coerceIn(14, 22)
        if (targetWeeks == plan.regularSeasonWeeks && plan.slots.isNotEmpty()) {
            return validateAndRecompute(plan, franchises, unionAgreement)
        }

        val kept = plan.slots.filter { it.week <= targetWeeks }.toMutableList()
        if (targetWeeks > plan.regularSeasonWeeks) {
            for (week in (plan.regularSeasonWeeks + 1)..targetWeeks) {
                kept.addAll(generateBalancedSlotsForWeek(week, plan.targetYear, franchises, weekOffset = week))
            }
        }

        // Migrate any RIVALRY_FINALE game previously on old final week to the new final week
        if (targetWeeks > plan.regularSeasonWeeks) {
            val oldFinaleSlotIdx = kept.indexOfFirst { it.holidayTentpole == HolidayTentpole.RIVALRY_FINALE && it.week == plan.regularSeasonWeeks }
            val newFinaleIdx = kept.indexOfFirst { it.week == targetWeeks }
            if (oldFinaleSlotIdx >= 0 && newFinaleIdx >= 0) {
                val oldSlot = kept[oldFinaleSlotIdx]
                val newSlot = kept[newFinaleIdx]
                kept[oldFinaleSlotIdx] = oldSlot.copy(holidayTentpole = HolidayTentpole.NONE, isMarqueeRivalry = false, rivalryTitle = null)
                kept[newFinaleIdx] = newSlot.copy(
                    holidayTentpole = HolidayTentpole.RIVALRY_FINALE,
                    isMarqueeRivalry = true,
                    rivalryTitle = oldSlot.rivalryTitle ?: "Season Finale Showdown"
                )
            }
        }

        val resetStatus = if (plan.isApproved) ScheduleApprovalStatus.DRAFT_IN_PROGRESS else plan.approvalStatus
        return validateAndRecompute(
            plan = plan.copy(
                regularSeasonWeeks = targetWeeks,
                slots = kept,
                approvalStatus = resetStatus,
                lastVoteSummary = "Calendar resized to $targetWeeks regular season weeks — submit for Board approval."
            ),
            franchises = franchises,
            unionAgreement = unionAgreement
        )
    }

    fun flipHomeAway(
        plan: SeasonSchedulePlan,
        slotId: String,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement
    ): SeasonSchedulePlan {
        val updated = plan.slots.map { slot ->
            if (slot.id == slotId) {
                slot.copy(homeTeamId = slot.awayTeamId, awayTeamId = slot.homeTeamId)
            } else slot
        }
        return validateAndRecompute(
            plan = plan.copy(
                slots = updated,
                approvalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS
            ),
            franchises = franchises,
            unionAgreement = unionAgreement
        )
    }

    fun swapMatchupTeam(
        plan: SeasonSchedulePlan,
        slotId: String,
        replaceHome: Boolean,
        newTeamId: String,
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        unionAgreement: LaborUnionAgreement
    ): SeasonSchedulePlan {
        val target = plan.slots.firstOrNull { it.id == slotId } ?: return plan
        val oldTeamId = if (replaceHome) target.homeTeamId else target.awayTeamId
        val otherTeamInSlot = if (replaceHome) target.awayTeamId else target.homeTeamId
        if (newTeamId == oldTeamId || newTeamId == otherTeamInSlot) return plan

        // Find if newTeamId is already scheduled in another slot this same week and swap cleanly
        val siblingSlot = plan.slots.firstOrNull {
            it.week == target.week && it.id != slotId && (it.homeTeamId == newTeamId || it.awayTeamId == newTeamId)
        }

        val updated = plan.slots.map { slot ->
            when (slot.id) {
                slotId -> {
                    val nextHome = if (replaceHome) newTeamId else slot.homeTeamId
                    val nextAway = if (replaceHome) slot.awayTeamId else newTeamId
                    val riv = rivalries.firstOrNull {
                        (it.teamAId == nextHome && it.teamBId == nextAway) ||
                            (it.teamBId == nextHome && it.teamAId == nextAway)
                    }
                    val homeName = franchises.find { it.id == nextHome }?.cityName ?: "Home"
                    val awayName = franchises.find { it.id == nextAway }?.cityName ?: "Away"
                    slot.copy(
                        homeTeamId = nextHome,
                        awayTeamId = nextAway,
                        rivalryTitle = when {
                            riv != null -> riv.rivalryName
                            slot.isHolidayGame -> "${slot.holidayTentpole.shortBadge}: $homeName vs $awayName"
                            else -> slot.rivalryTitle
                        }
                    )
                }
                siblingSlot?.id -> {
                    if (slot.homeTeamId == newTeamId) {
                        slot.copy(homeTeamId = oldTeamId)
                    } else {
                        slot.copy(awayTeamId = oldTeamId)
                    }
                }
                else -> slot
            }
        }
        return validateAndRecompute(
            plan = plan.copy(
                slots = updated,
                approvalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS
            ),
            franchises = franchises,
            unionAgreement = unionAgreement
        )
    }

    fun moveMatchToWeek(
        plan: SeasonSchedulePlan,
        slotId: String,
        targetWeek: Int,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement
    ): SeasonSchedulePlan {
        val clampedWeek = targetWeek.coerceIn(1, plan.regularSeasonWeeks)
        val source = plan.slots.firstOrNull { it.id == slotId } ?: return plan
        if (source.week == clampedWeek) return plan

        val sourceWeek = source.week
        val slotForHome = plan.slots.firstOrNull {
            it.week == clampedWeek && (it.homeTeamId == source.homeTeamId || it.awayTeamId == source.homeTeamId)
        }
        val slotForAway = plan.slots.firstOrNull {
            it.week == clampedWeek && (it.homeTeamId == source.awayTeamId || it.awayTeamId == source.awayTeamId)
        }

        val updated: List<ScheduledGameSlot> = when {
            // Same slot in targetWeek already pairs both teams: swap weeks cleanly
            slotForHome != null && slotForHome.id == slotForAway?.id -> {
                plan.slots.map { slot ->
                    when (slot.id) {
                        source.id -> slot.copy(week = clampedWeek)
                        slotForHome.id -> slot.copy(week = sourceWeek)
                        else -> slot
                    }
                }
            }
            // Both teams play different opponents C and D in targetWeek:
            // targetWeek gets source (A vs B) + slotForAway repurposed as (C vs D)
            // sourceWeek gets slotForHome (A vs C) and C's former sourceWeek slot replaces C with B -> (B vs E)
            slotForHome != null && slotForAway != null -> {
                val partnerC = if (slotForHome.homeTeamId == source.homeTeamId) slotForHome.awayTeamId else slotForHome.homeTeamId
                val partnerD = if (slotForAway.homeTeamId == source.awayTeamId) slotForAway.awayTeamId else slotForAway.homeTeamId
                val sourceWeekSlotForC = plan.slots.firstOrNull {
                    it.week == sourceWeek && it.id != source.id && (it.homeTeamId == partnerC || it.awayTeamId == partnerC)
                }
                plan.slots.map { slot ->
                    when (slot.id) {
                        source.id -> slot.copy(week = clampedWeek)
                        slotForAway.id -> slot.copy(
                            homeTeamId = partnerC,
                            awayTeamId = partnerD,
                            rivalryTitle = if (slot.isHolidayGame) "${slot.holidayTentpole.shortBadge} Matchup" else null
                        )
                        slotForHome.id -> slot.copy(week = sourceWeek)
                        sourceWeekSlotForC?.id -> {
                            val nextHome = if (slot.homeTeamId == partnerC) source.awayTeamId else slot.homeTeamId
                            val nextAway = if (slot.awayTeamId == partnerC) source.awayTeamId else slot.awayTeamId
                            slot.copy(
                                homeTeamId = nextHome,
                                awayTeamId = nextAway,
                                rivalryTitle = if (slot.isHolidayGame) "${slot.holidayTentpole.shortBadge} Matchup" else null
                            )
                        }
                        else -> slot
                    }
                }
            }
            // Only homeTeam has a game in targetWeek (awayTeam had a bye in targetWeek)
            slotForHome != null -> {
                val partnerC = if (slotForHome.homeTeamId == source.homeTeamId) slotForHome.awayTeamId else slotForHome.homeTeamId
                val sourceWeekSlotForC = plan.slots.firstOrNull {
                    it.week == sourceWeek && it.id != source.id && (it.homeTeamId == partnerC || it.awayTeamId == partnerC)
                }
                plan.slots.map { slot ->
                    when (slot.id) {
                        source.id -> slot.copy(week = clampedWeek)
                        slotForHome.id -> slot.copy(week = sourceWeek)
                        sourceWeekSlotForC?.id -> {
                            val nextHome = if (slot.homeTeamId == partnerC) source.awayTeamId else slot.homeTeamId
                            val nextAway = if (slot.awayTeamId == partnerC) source.awayTeamId else slot.awayTeamId
                            slot.copy(homeTeamId = nextHome, awayTeamId = nextAway)
                        }
                        else -> slot
                    }
                }
            }
            // Only awayTeam has a game in targetWeek (homeTeam had a bye in targetWeek)
            slotForAway != null -> {
                val partnerD = if (slotForAway.homeTeamId == source.awayTeamId) slotForAway.awayTeamId else slotForAway.homeTeamId
                val sourceWeekSlotForD = plan.slots.firstOrNull {
                    it.week == sourceWeek && it.id != source.id && (it.homeTeamId == partnerD || it.awayTeamId == partnerD)
                }
                plan.slots.map { slot ->
                    when (slot.id) {
                        source.id -> slot.copy(week = clampedWeek)
                        slotForAway.id -> slot.copy(week = sourceWeek)
                        sourceWeekSlotForD?.id -> {
                            val nextHome = if (slot.homeTeamId == partnerD) source.homeTeamId else slot.homeTeamId
                            val nextAway = if (slot.awayTeamId == partnerD) source.homeTeamId else slot.awayTeamId
                            slot.copy(homeTeamId = nextHome, awayTeamId = nextAway)
                        }
                        else -> slot
                    }
                }
            }
            else -> {
                val swapCandidate = plan.slots.firstOrNull { it.week == clampedWeek && !it.isHolidayGame && !it.isInternationalGame }
                    ?: plan.slots.firstOrNull { it.week == clampedWeek }
                plan.slots.map { slot ->
                    when {
                        slot.id == source.id -> slot.copy(week = clampedWeek)
                        swapCandidate != null && slot.id == swapCandidate.id -> slot.copy(week = sourceWeek)
                        else -> slot
                    }
                }
            }
        }
        return validateAndRecompute(
            plan = plan.copy(
                slots = updated,
                approvalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS
            ),
            franchises = franchises,
            unionAgreement = unionAgreement
        )
    }

    fun setHolidayMarqueeOnSlot(
        plan: SeasonSchedulePlan,
        slotId: String,
        tentpole: HolidayTentpole,
        customRivalryTitle: String? = null,
        promoteAsMarqueeRivalry: Boolean = tentpole != HolidayTentpole.NONE,
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        unionAgreement: LaborUnionAgreement
    ): SeasonSchedulePlan {
        val updated = plan.slots.map { slot ->
            if (slot.id == slotId) {
                val home = franchises.find { it.id == slot.homeTeamId }
                val away = franchises.find { it.id == slot.awayTeamId }
                val matchedRiv = rivalries.firstOrNull {
                    (it.teamAId == slot.homeTeamId && it.teamBId == slot.awayTeamId) ||
                        (it.teamBId == slot.homeTeamId && it.teamAId == slot.awayTeamId)
                }
                val nextTitle = when {
                    tentpole == HolidayTentpole.NONE -> matchedRiv?.rivalryName
                    !customRivalryTitle.isNullOrBlank() -> customRivalryTitle
                    matchedRiv != null -> matchedRiv.rivalryName
                    else -> "${tentpole.shortBadge} Showdown: ${home?.nickname ?: "Home"} vs ${away?.nickname ?: "Away"}"
                }
                slot.copy(
                    holidayTentpole = tentpole,
                    isMarqueeRivalry = tentpole != HolidayTentpole.NONE && promoteAsMarqueeRivalry,
                    rivalryTitle = nextTitle
                )
            } else slot
        }
        return validateAndRecompute(
            plan = plan.copy(
                slots = updated,
                approvalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS
            ),
            franchises = franchises,
            unionAgreement = unionAgreement
        )
    }

    fun assignInternationalVenueToSlot(
        plan: SeasonSchedulePlan,
        slotId: String,
        venueId: String?,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement
    ): SeasonSchedulePlan {
        val venue = InternationalVenueCatalog.findById(venueId)
        val updated = plan.slots.map { slot ->
            if (slot.id == slotId) {
                slot.copy(
                    internationalVenueId = venue?.id,
                    internationalVenueLabel = venue?.fullVenueLabel
                )
            } else slot
        }
        return validateAndRecompute(
            plan = plan.copy(
                slots = updated,
                approvalStatus = ScheduleApprovalStatus.DRAFT_IN_PROGRESS
            ),
            franchises = franchises,
            unionAgreement = unionAgreement
        )
    }

    fun validateAndRecompute(
        plan: SeasonSchedulePlan,
        franchises: List<Franchise>,
        unionAgreement: LaborUnionAgreement
    ): SeasonSchedulePlan {
        val warnings = mutableListOf<String>()
        var hasErrors = false

        val teamNames = franchises.associate { it.id to it.teamFullName }

        for (w in 1..plan.regularSeasonWeeks) {
            val weekSlots = plan.slots.filter { it.week == w }
            if (weekSlots.isEmpty() && franchises.size >= 2) {
                hasErrors = true
                warnings.add("Week $w has zero scheduled games.")
                continue
            }
            val seen = mutableSetOf<String>()
            weekSlots.forEach { slot ->
                if (slot.homeTeamId == slot.awayTeamId) {
                    hasErrors = true
                    warnings.add("Week $w: ${teamNames[slot.homeTeamId] ?: slot.homeTeamId} is scheduled against itself.")
                }
                if (!seen.add(slot.homeTeamId)) {
                    hasErrors = true
                    warnings.add("Week $w conflict: ${teamNames[slot.homeTeamId] ?: slot.homeTeamId} is double-booked.")
                }
                if (!seen.add(slot.awayTeamId)) {
                    hasErrors = true
                    warnings.add("Week $w conflict: ${teamNames[slot.awayTeamId] ?: slot.awayTeamId} is double-booked.")
                }
            }
        }

        val intlCount = plan.slots.count { it.isInternationalGame }
        if (intlCount > 1 && !unionAgreement.mandatoryPrivateCharterFlights) {
            warnings.add("CBA Advisory: $intlCount international games scheduled without Mandatory Private Charters (+Players Union fatigue risk).")
        }
        if (plan.regularSeasonWeeks >= 20 && !unionAgreement.maxBackToBackGamesLimit) {
            warnings.add("Workload Advisory: ${plan.regularSeasonWeeks}-week slate without Back-to-Back Limits elevates player injury/strike pressure.")
        }

        return plan.copy(
            conflictWarnings = warnings.distinct(),
            hasValidationErrors = hasErrors
        )
    }

    fun buildWeekMatchesFromPlan(
        plan: SeasonSchedulePlan?,
        week: Int,
        franchises: List<Franchise>,
        rivalries: List<TeamRivalry>,
        cities: List<MarketCity>,
        sportType: SportType
    ): List<GameMatch> {
        val weekSlots = plan?.slotsForWeek(week).orEmpty()
        if (weekSlots.isNotEmpty()) {
            val mapped = weekSlots.map { it.toGameMatch(franchises, rivalries, cities) }
            if (mapped.isNotEmpty()) return mapped
        }
        return LeagueSimulationEngine.generateMatchesForWeek(
            week = week,
            teams = franchises,
            sport = sportType,
            rivalries = rivalries,
            cities = cities
        )
    }
}
