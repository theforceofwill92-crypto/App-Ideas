package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt

enum class FanSegmentType(
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val accentColorHex: Long
) {
    CASUAL(
        displayName = "Casual Fans",
        shortLabel = "CASUAL",
        description = "Price-sensitive, entertainment-driven viewers focused on stars, pace & spectacle.",
        accentColorHex = 0xFF06B6D4L // Cyan
    ),
    DIE_HARD(
        displayName = "Die-Hard Fans",
        shortLabel = "DIE-HARD",
        description = "Traditionalist season-ticket holders focused on integrity, parity, history & home markets.",
        accentColorHex = 0xFFD4AF37L // Gold
    )
}

enum class MediaReactionTone(
    val label: String,
    val badgeColorHex: Long
) {
    HYPE("SURGE", 0xFF10B981L),
    DEBATE("SPLIT", 0xFFF59E0BL),
    OUTRAGE("BACKLASH", 0xFFEF4444L),
    ANALYTICAL("PRESS", 0xFF3B82F6L)
}

enum class FanMediaChannel(val label: String) {
    ALL("All Pulse"),
    SOCIAL("Social Feed"),
    RADIO("Talk Radio"),
    NEWSPAPER("Headlines")
}

enum class FanSegmentFilter(val label: String) {
    ALL("All Fans"),
    CASUAL("Casual"),
    DIE_HARD("Die-Hard")
}

enum class FanDecisionCategory(val label: String) {
    TICKET_PRICING("Ticket Pricing"),
    RULE_CHANGE("Rule Bylaws"),
    DISCIPLINARY("Disciplinary Ruling"),
    OFFICIATING("Officiating"),
    CRISIS_RULING("Crisis Response"),
    STADIUM_RELOCATION("Stadium & Relocation"),
    LABOR_CBA("Labor & CBA"),
    MARKETING_BROADCAST("Media & Marketing"),
    ALL_STAR_SCHEDULE("All-Star & Schedule"),
    RIVAL_LEAGUE("Rival League"),
    EXPANSION_TRADE("Expansion & Roster"),
    WEEKLY_SIM("Weekly Action"),
    TOWN_HALL("Fan Town Hall")
}

data class SocialMediaPostItem(
    val id: String,
    val handle: String,
    val displayName: String,
    val segment: FanSegmentType,
    val tone: MediaReactionTone,
    val body: String,
    val hashtags: List<String>,
    val likesCount: Int,
    val repostsCount: Int,
    val timestampLabel: String,
    val category: FanDecisionCategory,
    val sentimentDelta: Int
)

data class SportsTalkRadioSegment(
    val id: String,
    val showName: String,
    val hostNames: String,
    val callerName: String,
    val callerCity: String,
    val callerSegment: FanSegmentType,
    val segmentTitle: String,
    val hostTake: String,
    val callerQuote: String,
    val casualApprovalPct: Int,
    val dieHardApprovalPct: Int,
    val heatednessScore: Int,
    val tone: MediaReactionTone,
    val timestampLabel: String,
    val category: FanDecisionCategory
)

data class NewspaperHeadlineItem(
    val id: String,
    val publicationName: String,
    val sectionKicker: String,
    val headline: String,
    val subheadline: String,
    val byline: String,
    val leadParagraph: String,
    val tone: MediaReactionTone,
    val timestampLabel: String,
    val category: FanDecisionCategory,
    val relatedEntity: String? = null
)

data class FanDecisionReactionRecord(
    val id: String,
    val category: FanDecisionCategory,
    val decisionTitle: String,
    val summary: String,
    val casualDelta: Int,
    val dieHardDelta: Int,
    val casualScoreAfter: Int,
    val dieHardScoreAfter: Int,
    val timestampLabel: String
)

data class FanSentimentState(
    val casualSentimentScore: Int = 74,
    val dieHardSentimentScore: Int = 76,
    val lastCasualDelta: Int = 0,
    val lastDieHardDelta: Int = 0,
    val socialFeed: List<SocialMediaPostItem> = emptyList(),
    val radioSegments: List<SportsTalkRadioSegment> = emptyList(),
    val newspaperHeadlines: List<NewspaperHeadlineItem> = emptyList(),
    val decisionReactions: List<FanDecisionReactionRecord> = emptyList(),
    val lastPulseNote: String = "Opening week sentiment steady across casual viewers and die-hard season ticket bases."
) {
    val blendedSentimentScore: Int
        get() = ((casualSentimentScore * 0.45) + (dieHardSentimentScore * 0.55)).roundToInt().coerceIn(0, 100)

    val divergenceGap: Int
        get() = abs(dieHardSentimentScore - casualSentimentScore)

    val polarizationLabel: String
        get() = when {
            divergenceGap >= 22 -> "Culture Clash"
            divergenceGap >= 13 -> "Split Base"
            divergenceGap >= 6 -> "Mild Divide"
            else -> "Unified Base"
        }

    val casualMoodLabel: String
        get() = moodLabelFor(casualSentimentScore)

    val dieHardMoodLabel: String
        get() = moodLabelFor(dieHardSentimentScore)

    companion object {
        fun moodLabelFor(score: Int): String = when {
            score >= 85 -> "Electric"
            score >= 72 -> "Hyped"
            score >= 58 -> "Steady"
            score >= 42 -> "Restless"
            else -> "Alienated"
        }
    }
}

object FanSentimentEngine {

    private data class PersonaSpec(val displayName: String, val handle: String)

    private fun casualPersonas(sport: SportType): List<PersonaSpec> = when (sport) {
        SportType.BASKETBALL -> listOf(
            PersonaSpec("Courtside Highlights", "@BucketsAndChill"),
            PersonaSpec("Weekend Hoophead", "@BuzzerBeaterVibes"),
            PersonaSpec("League Pass Casual", "@FourthQuarterPopcorn")
        )
        SportType.FOOTBALL -> listOf(
            PersonaSpec("Sunday RedZone Fan", "@CouchQuarterback99"),
            PersonaSpec("Tailgate Vibes", "@SundayHighlights"),
            PersonaSpec("Fourth Down Casual", "@BigPlayEnergy")
        )
        SportType.SOCCER -> listOf(
            PersonaSpec("Highlight Reel FC", "@TopBinsDaily"),
            PersonaSpec("Derby Day Streamer", "@90thMinuteHype"),
            PersonaSpec("Matchday Casual", "@GoalLineClips")
        )
        SportType.HOCKEY -> listOf(
            PersonaSpec("Overtime Thrills", "@BarDownClips"),
            PersonaSpec("Weekend Rink Fan", "@HatTrickEnergy"),
            PersonaSpec("Power Play Casual", "@ZamboniWatch")
        )
    }

    private fun dieHardPersonas(sport: SportType): List<PersonaSpec> = when (sport) {
        SportType.BASKETBALL -> listOf(
            PersonaSpec("Section 104 Ultras", "@HardwoodPurist"),
            PersonaSpec("Cap Sheet & Tape", "@PickAndRollTactician"),
            PersonaSpec("Since Expansion '89", "@BoxScoreOriginal")
        )
        SportType.FOOTBALL -> listOf(
            PersonaSpec("Upper Deck Dawg", "@TrenchWarfare1978"),
            PersonaSpec("All-22 Film Room", "@GridironTradition"),
            PersonaSpec("Cold Weather Loyal", "@SeasonTicketShield")
        )
        SportType.SOCCER -> listOf(
            PersonaSpec("North Terrace Supporter", "@SupportersTrust12"),
            PersonaSpec("Tactics & xG Purist", "@LowBlockChronicle"),
            PersonaSpec("Matchday BadgeHolder", "@HeritageClubSC")
        )
        SportType.HOCKEY -> listOf(
            PersonaSpec("Original Six Purist", "@BlueLineTraditional"),
            PersonaSpec("Glass Seat Loyal", "@NeutralZoneTrap"),
            PersonaSpec("Banner Rafters Club", "@OldBarnFaithful")
        )
    }

    private val radioShows = listOf(
        Triple("Drive-Time Hot Takes", "Marcus & 'Big Mac' Miller", "National Syndicate AM 980"),
        Triple("The Film Room Call-In", "Elena Vance & Coach Ray", "Sports Talk 104.3 FM"),
        Triple("Press Box & Nosebleeds", "Dante Brooks & Sarah Kim", "Coast-to-Coast Sports Radio")
    )

    private val publications = listOf(
        Triple("THE NATIONAL SPORTS CHRONICLE", "FRONT PAGE", "Grant Mercer • Senior League Columnist"),
        Triple("THE METRO DAILY LEDGER", "COMMISSIONER DESK", "Tessa Holloway • Business & Governance"),
        Triple("APEX SPORTS TRIBUNE", "EDITORIAL BOARD", "Julian Park • National Beat Writer")
    )

    fun createInitialState(
        sportType: SportType,
        leagueName: String,
        franchises: List<Franchise>,
        seasonYear: Int = 2026
    ): FanSentimentState {
        val avgMorale = if (franchises.isNotEmpty()) franchises.map { it.fanMorale }.average().roundToInt() else 75
        val avgLoyalty = if (franchises.isNotEmpty()) franchises.map { it.fanLoyalty }.average().roundToInt() else 75
        val initialCasual = avgMorale.coerceIn(30, 95)
        val initialDieHard = avgLoyalty.coerceIn(30, 95)

        val anchorTeam = franchises.maxByOrNull { it.fanBaseMillions }
        val teamName = anchorTeam?.teamFullName ?: leagueName
        val cityName = anchorTeam?.cityName ?: "Metro"

        val casualP = casualPersonas(sportType).first()
        val dieHardP = dieHardPersonas(sportType).first()
        val timeTag = "S$seasonYear • W1"

        val openingSocial = listOf(
            SocialMediaPostItem(
                id = "soc_init_casual_$seasonYear",
                handle = casualP.handle,
                displayName = casualP.displayName,
                segment = FanSegmentType.CASUAL,
                tone = MediaReactionTone.HYPE,
                body = "Opening week of $leagueName (${sportType.displayName}) is finally here! Give us buzzer beaters, affordable weekend seats, and star-studded marquee nights.",
                hashtags = listOf("#OpeningWeek", "#${leagueName.replace(" ", "")}", "#HighlightSeason"),
                likesCount = 1420,
                repostsCount = 318,
                timestampLabel = timeTag,
                category = FanDecisionCategory.WEEKLY_SIM,
                sentimentDelta = 0
            ),
            SocialMediaPostItem(
                id = "soc_init_diehard_$seasonYear",
                handle = dieHardP.handle,
                displayName = dieHardP.displayName,
                segment = FanSegmentType.DIE_HARD,
                tone = MediaReactionTone.ANALYTICAL,
                body = "New season in $cityName, new commissioner mandate. We'll be watching officiating consistency, ticket loyalty, and competitive integrity from Week 1.",
                hashtags = listOf("#IntegrityFirst", "#SeasonTicketLife", "#PuristWatch"),
                likesCount = 684,
                repostsCount = 149,
                timestampLabel = timeTag,
                category = FanDecisionCategory.WEEKLY_SIM,
                sentimentDelta = 0
            )
        )

        val openingRadio = listOf(
            SportsTalkRadioSegment(
                id = "rad_init_$seasonYear",
                showName = radioShows[0].first,
                hostNames = radioShows[0].second,
                callerName = "Tony",
                callerCity = cityName,
                callerSegment = FanSegmentType.DIE_HARD,
                segmentTitle = "Can the Commissioner please BOTH casuals and traditionalists?",
                hostTake = "Casual viewers want fast pace and holiday spectacles. Die-hards want clean whistles, fair prices, and zero relocation drama. Good luck balancing that wire.",
                callerQuote = "\"If $teamName keeps ticket prices fair and the refs stay consistent, Section 104 will pack the building every night!\"",
                casualApprovalPct = initialCasual,
                dieHardApprovalPct = initialDieHard,
                heatednessScore = 44,
                tone = MediaReactionTone.DEBATE,
                timestampLabel = timeTag,
                category = FanDecisionCategory.WEEKLY_SIM
            )
        )

        val openingPaper = listOf(
            NewspaperHeadlineItem(
                id = "news_init_$seasonYear",
                publicationName = publications[0].first,
                sectionKicker = "SEASON $seasonYear PREVIEW",
                headline = "$leagueName Opens $seasonYear Under Spotlight of Split Fan Expectations",
                subheadline = "Front office vows competitive integrity while courting next-gen casual audiences across ${franchises.size} markets.",
                byline = publications[0].third,
                leadParagraph = "As $leagueName tips off its $seasonYear campaign, commissioner policy on pricing, rule tinkering, and market stability faces immediate scrutiny from two very different constituencies.",
                tone = MediaReactionTone.ANALYTICAL,
                timestampLabel = timeTag,
                category = FanDecisionCategory.WEEKLY_SIM,
                relatedEntity = teamName
            )
        )

        return FanSentimentState(
            casualSentimentScore = initialCasual,
            dieHardSentimentScore = initialDieHard,
            lastCasualDelta = 0,
            lastDieHardDelta = 0,
            socialFeed = openingSocial,
            radioSegments = openingRadio,
            newspaperHeadlines = openingPaper,
            decisionReactions = emptyList(),
            lastPulseNote = "Season $seasonYear fan sentiment initialized: Casual $initialCasual% • Die-Hard $initialDieHard%."
        )
    }

    fun recordDecisionReaction(
        currentState: FanSentimentState,
        category: FanDecisionCategory,
        decisionTitle: String,
        decisionSummary: String,
        casualDelta: Int,
        dieHardDelta: Int,
        franchiseName: String = "League-Wide",
        cityName: String = "National",
        sportType: SportType = SportType.BASKETBALL,
        seasonYear: Int = 2026,
        weekNumber: Int = 1,
        seedOffset: Int = 0
    ): FanSentimentState {
        val nextCasual = (currentState.casualSentimentScore + casualDelta).coerceIn(10, 100)
        val nextDieHard = (currentState.dieHardSentimentScore + dieHardDelta).coerceIn(10, 100)
        val actualCasualDelta = nextCasual - currentState.casualSentimentScore
        val actualDieHardDelta = nextDieHard - currentState.dieHardSentimentScore

        val seq = currentState.decisionReactions.size + 1 + abs(seedOffset)
        val timeTag = if (weekNumber > 0) "S$seasonYear • W$weekNumber" else "S$seasonYear • Off-Season"

        val casualPersona = casualPersonas(sportType)[seq % casualPersonas(sportType).size]
        val dieHardPersona = dieHardPersonas(sportType)[(seq + 1) % dieHardPersonas(sportType).size]
        val radioShow = radioShows[seq % radioShows.size]
        val pub = publications[seq % publications.size]

        val casualTone = toneForDelta(actualCasualDelta, actualDieHardDelta)
        val dieHardTone = toneForDelta(actualDieHardDelta, actualCasualDelta)
        val combinedDelta = actualCasualDelta + actualDieHardDelta
        val gapAfter = abs(nextDieHard - nextCasual)
        val showSplit = (actualCasualDelta > 0 && actualDieHardDelta < 0) ||
            (actualCasualDelta < 0 && actualDieHardDelta > 0) ||
            abs(actualCasualDelta - actualDieHardDelta) >= 4

        val headlineTone = when {
            showSplit -> MediaReactionTone.DEBATE
            combinedDelta >= 4 -> MediaReactionTone.HYPE
            combinedDelta <= -4 -> MediaReactionTone.OUTRAGE
            else -> MediaReactionTone.ANALYTICAL
        }

        val casualPost = buildSocialPost(
            id = "soc_c_${seasonYear}_${weekNumber}_$seq",
            persona = casualPersona,
            segment = FanSegmentType.CASUAL,
            tone = casualTone,
            category = category,
            decisionTitle = decisionTitle,
            franchiseName = franchiseName,
            cityName = cityName,
            ownDelta = actualCasualDelta,
            otherDelta = actualDieHardDelta,
            timestampLabel = timeTag,
            seed = seq
        )

        val dieHardPost = buildSocialPost(
            id = "soc_d_${seasonYear}_${weekNumber}_$seq",
            persona = dieHardPersona,
            segment = FanSegmentType.DIE_HARD,
            tone = dieHardTone,
            category = category,
            decisionTitle = decisionTitle,
            franchiseName = franchiseName,
            cityName = cityName,
            ownDelta = actualDieHardDelta,
            otherDelta = actualCasualDelta,
            timestampLabel = timeTag,
            seed = seq + 7
        )

        val callerSegment = if (abs(actualDieHardDelta) >= abs(actualCasualDelta)) {
            FanSegmentType.DIE_HARD
        } else {
            FanSegmentType.CASUAL
        }
        val callerNames = listOf("Mike", "Brianna", "Carlos", "Devon", "Hannah", "Rico")
        val callerName = callerNames[seq % callerNames.size]

        val heatedness = (38 + abs(actualCasualDelta) * 5 + abs(actualDieHardDelta) * 6 + (gapAfter / 2)).coerceIn(25, 99)

        val radioSegment = buildRadioSegment(
            id = "rad_${seasonYear}_${weekNumber}_$seq",
            showName = radioShow.first,
            hostNames = radioShow.second,
            callerName = callerName,
            callerCity = cityName,
            callerSegment = callerSegment,
            category = category,
            decisionTitle = decisionTitle,
            franchiseName = franchiseName,
            casualDelta = actualCasualDelta,
            dieHardDelta = actualDieHardDelta,
            nextCasual = nextCasual,
            nextDieHard = nextDieHard,
            heatedness = heatedness,
            tone = if (showSplit || heatedness >= 72) MediaReactionTone.DEBATE else headlineTone,
            timestampLabel = timeTag
        )

        val newspaperItem = buildNewspaperHeadline(
            id = "news_${seasonYear}_${weekNumber}_$seq",
            publicationName = pub.first,
            sectionKicker = when {
                showSplit -> "FANBASE DIVIDE"
                combinedDelta <= -5 -> "BACKLASH REPORT"
                combinedDelta >= 5 -> "LEAGUE SURGE"
                else -> pub.second
            },
            byline = pub.third,
            category = category,
            decisionTitle = decisionTitle,
            decisionSummary = decisionSummary,
            franchiseName = franchiseName,
            cityName = cityName,
            casualDelta = actualCasualDelta,
            dieHardDelta = actualDieHardDelta,
            nextCasual = nextCasual,
            nextDieHard = nextDieHard,
            tone = headlineTone,
            timestampLabel = timeTag
        )

        val record = FanDecisionReactionRecord(
            id = "rec_${seasonYear}_${weekNumber}_$seq",
            category = category,
            decisionTitle = decisionTitle,
            summary = decisionSummary,
            casualDelta = actualCasualDelta,
            dieHardDelta = actualDieHardDelta,
            casualScoreAfter = nextCasual,
            dieHardScoreAfter = nextDieHard,
            timestampLabel = timeTag
        )

        val deltaStrCasual = if (actualCasualDelta >= 0) "+$actualCasualDelta" else "$actualCasualDelta"
        val deltaStrDieHard = if (actualDieHardDelta >= 0) "+$actualDieHardDelta" else "$actualDieHardDelta"
        val note = "$decisionTitle -> Casual $deltaStrCasual ($nextCasual%), Die-Hard $deltaStrDieHard ($nextDieHard%)."

        return currentState.copy(
            casualSentimentScore = nextCasual,
            dieHardSentimentScore = nextDieHard,
            lastCasualDelta = actualCasualDelta,
            lastDieHardDelta = actualDieHardDelta,
            socialFeed = (listOf(casualPost, dieHardPost) + currentState.socialFeed).take(30),
            radioSegments = (listOf(radioSegment) + currentState.radioSegments).take(16),
            newspaperHeadlines = (listOf(newspaperItem) + currentState.newspaperHeadlines).take(16),
            decisionReactions = (listOf(record) + currentState.decisionReactions).take(24),
            lastPulseNote = note
        )
    }

    fun runWeeklyBroadcastPulse(
        currentState: FanSentimentState,
        franchises: List<Franchise>,
        sportType: SportType,
        seasonYear: Int,
        weekNumber: Int
    ): FanSentimentState {
        if (franchises.isEmpty()) return currentState
        val hottestTeam = franchises.maxByOrNull { it.wins - it.losses + (it.fanMorale / 10) } ?: franchises.first()
        val restlessTeam = franchises.minByOrNull { it.fanLoyalty + it.fanMorale } ?: franchises.last()
        val avgMorale = franchises.map { it.fanMorale }.average().roundToInt()
        val avgLoyalty = franchises.map { it.fanLoyalty }.average().roundToInt()

        val casualDrift = ((avgMorale - currentState.casualSentimentScore) / 4).coerceIn(-3, 3)
        val dieHardDrift = ((avgLoyalty - currentState.dieHardSentimentScore) / 4).coerceIn(-3, 3)

        val weeklyTitle = "Week $weekNumber Pulse: ${hottestTeam.nickname} Surge vs ${restlessTeam.nickname} Watch"
        val weeklySummary = "${hottestTeam.teamFullName} (${hottestTeam.wins}-${hottestTeam.losses}) energize highlight feeds while ${restlessTeam.cityName} supporters grade front-office direction."

        return recordDecisionReaction(
            currentState = currentState,
            category = FanDecisionCategory.WEEKLY_SIM,
            decisionTitle = weeklyTitle,
            decisionSummary = weeklySummary,
            casualDelta = casualDrift,
            dieHardDelta = dieHardDrift,
            franchiseName = hottestTeam.teamFullName,
            cityName = hottestTeam.cityName,
            sportType = sportType,
            seasonYear = seasonYear,
            weekNumber = weekNumber,
            seedOffset = weekNumber
        )
    }

    private fun toneForDelta(ownDelta: Int, otherDelta: Int): MediaReactionTone = when {
        ownDelta >= 3 -> MediaReactionTone.HYPE
        ownDelta <= -3 -> MediaReactionTone.OUTRAGE
        ownDelta > 0 && otherDelta < 0 -> MediaReactionTone.HYPE
        ownDelta < 0 && otherDelta > 0 -> MediaReactionTone.OUTRAGE
        abs(ownDelta - otherDelta) >= 3 -> MediaReactionTone.DEBATE
        else -> MediaReactionTone.ANALYTICAL
    }

    private fun buildSocialPost(
        id: String,
        persona: PersonaSpec,
        segment: FanSegmentType,
        tone: MediaReactionTone,
        category: FanDecisionCategory,
        decisionTitle: String,
        franchiseName: String,
        cityName: String,
        ownDelta: Int,
        otherDelta: Int,
        timestampLabel: String,
        seed: Int
    ): SocialMediaPostItem {
        val teamTag = franchiseName.replace(" ", "").take(18)
        val catTag = category.label.replace(" ", "").replace("&", "")
        val body = if (segment == FanSegmentType.CASUAL) {
            when {
                ownDelta >= 3 && otherDelta < 0 ->
                    "Old heads are complaining again, but $decisionTitle is pure entertainment! Exactly what casual fans wanted to see for $franchiseName."
                ownDelta >= 3 ->
                    "Massive W by the Commissioner! $decisionTitle has everyone in our group chat locked into $franchiseName clips tonight."
                ownDelta <= -3 && otherDelta >= 0 ->
                    "Wait why are we paying/waiting more for $decisionTitle? Hardcore fans might nod at this, but casual viewers are tuning out fast."
                ownDelta <= -3 ->
                    "Rough optics on $decisionTitle. Nobody in $cityName wants extra friction or steep prices just to catch a game."
                else ->
                    "Keeping an eye on $decisionTitle around $franchiseName — let's see if the highlights match the headlines this week."
            }
        } else {
            when {
                ownDelta >= 3 && otherDelta < 0 ->
                    "Finally a decision for true supporters! $decisionTitle protects tradition and $cityName loyalty even if highlight-chasers don't get it."
                ownDelta >= 3 ->
                    "Respect where it's due: $decisionTitle honors season-ticket holders in $cityName and keeps competitive integrity front and center."
                ownDelta <= -3 && otherDelta >= 0 ->
                    "Gimmicks over substance. $decisionTitle chases casual clicks while ignoring loyal fans who pack $cityName every single season."
                ownDelta <= -3 ->
                    "Unacceptable disrespect to the faithful in $cityName. $decisionTitle erodes trust with the people who buy seats in lean years."
                else ->
                    "Mixed verdict on $decisionTitle for $franchiseName. The tape and long-term precedent over the next month will tell the real story."
            }
        }

        val baseLikes = when (segment) {
            FanSegmentType.CASUAL -> 950 + abs(ownDelta) * 420 + (seed * 73) % 500
            FanSegmentType.DIE_HARD -> 320 + abs(ownDelta) * 190 + (seed * 41) % 280
        }
        val baseReposts = (baseLikes * 0.24).roundToInt()

        return SocialMediaPostItem(
            id = id,
            handle = persona.handle,
            displayName = persona.displayName,
            segment = segment,
            tone = tone,
            body = body,
            hashtags = listOf("#$catTag", "#$teamTag", if (segment == FanSegmentType.CASUAL) "#CasualPulse" else "#DieHardBase"),
            likesCount = baseLikes,
            repostsCount = baseReposts,
            timestampLabel = timestampLabel,
            category = category,
            sentimentDelta = ownDelta
        )
    }

    private fun buildRadioSegment(
        id: String,
        showName: String,
        hostNames: String,
        callerName: String,
        callerCity: String,
        callerSegment: FanSegmentType,
        category: FanDecisionCategory,
        decisionTitle: String,
        franchiseName: String,
        casualDelta: Int,
        dieHardDelta: Int,
        nextCasual: Int,
        nextDieHard: Int,
        heatedness: Int,
        tone: MediaReactionTone,
        timestampLabel: String
    ): SportsTalkRadioSegment {
        val segmentTitle = when {
            casualDelta > 0 && dieHardDelta < 0 -> "SELLOUT OR EVOLUTION? $decisionTitle Splits Listeners!"
            casualDelta < 0 && dieHardDelta > 0 -> "PURIST WIN, CASUAL GROAN: Unpacking $decisionTitle"
            casualDelta <= -3 && dieHardDelta <= -3 -> "LINES LIT UP: Fans Blast Commissioner Over $decisionTitle"
            casualDelta >= 3 && dieHardDelta >= 3 -> "VICTORY LAP CALL-IN: $decisionTitle Wins Over Everyone"
            else -> "${category.label.uppercase()} TALKBACK: Grading $decisionTitle"
        }

        val hostTake = when {
            casualDelta > 0 && dieHardDelta < 0 ->
                "Our phone lines in $callerCity are split down the middle! Casual listeners love the buzz around $decisionTitle, while 20-year ticket holders think the league sold out its soul."
            casualDelta < 0 && dieHardDelta > 0 ->
                "Die-hard callers are applauding $decisionTitle as textbook integrity for $franchiseName, though social polls show casual fans losing patience."
            casualDelta <= -3 || dieHardDelta <= -3 ->
                "Look at the board—nearly every caller from $callerCity is hammering the league office over $decisionTitle. Commissioner needs a quick course correction."
            else ->
                "Following $decisionTitle, casual approval sits at $nextCasual% and die-hard trust is at $nextDieHard%. Every move in $franchiseName is magnified right now."
        }

        val callerQuote = when (callerSegment) {
            FanSegmentType.CASUAL -> if (casualDelta >= 0) {
                "\"Hey guys, $callerName in $callerCity here—honestly $decisionTitle makes $franchiseName way more fun to watch on Friday nights!\""
            } else {
                "\"$callerName from $callerCity—why does the league keep making it harder and pricier for regular fans after $decisionTitle?\""
            }
            FanSegmentType.DIE_HARD -> if (dieHardDelta >= 0) {
                "\"Longtime listener $callerName in $callerCity. Finally somebody respected history and real competition with $decisionTitle!\""
            } else {
                "\"First time caller, 15-year ticket holder in $callerCity. $decisionTitle feels like a slap in the face to die-hard fans!\""
            }
        }

        return SportsTalkRadioSegment(
            id = id,
            showName = showName,
            hostNames = hostNames,
            callerName = callerName,
            callerCity = callerCity,
            callerSegment = callerSegment,
            segmentTitle = segmentTitle,
            hostTake = hostTake,
            callerQuote = callerQuote,
            casualApprovalPct = nextCasual,
            dieHardApprovalPct = nextDieHard,
            heatednessScore = heatedness,
            tone = tone,
            timestampLabel = timestampLabel,
            category = category
        )
    }

    private fun buildNewspaperHeadline(
        id: String,
        publicationName: String,
        sectionKicker: String,
        byline: String,
        category: FanDecisionCategory,
        decisionTitle: String,
        decisionSummary: String,
        franchiseName: String,
        cityName: String,
        casualDelta: Int,
        dieHardDelta: Int,
        nextCasual: Int,
        nextDieHard: Int,
        tone: MediaReactionTone,
        timestampLabel: String
    ): NewspaperHeadlineItem {
        val headline = when {
            casualDelta >= 2 && dieHardDelta <= -2 ->
                "Spectacle Over Tradition? $decisionTitle Sparks Casual Buzz, Purist Outcry"
            casualDelta <= -2 && dieHardDelta >= 2 ->
                "Steadying the Base: $decisionTitle Wins Die-Hard Praise Despite Casual Chill"
            casualDelta <= -3 && dieHardDelta <= -3 ->
                "Fan Revolt Brews in $cityName After '$decisionTitle' Ruling"
            casualDelta >= 3 && dieHardDelta >= 3 ->
                "Rare Consensus: '$decisionTitle' Unites Casuals and Die-Hards Alike"
            else ->
                "Commissioner Verdict: $decisionTitle Reshapes $franchiseName Narrative"
        }

        val cStr = if (casualDelta >= 0) "+$casualDelta" else "$casualDelta"
        val dStr = if (dieHardDelta >= 0) "+$dieHardDelta" else "$dieHardDelta"
        val subheadline = "Post-decision polling places casual sentiment at $nextCasual% ($cStr) and die-hard loyalty at $nextDieHard% ($dStr)."

        return NewspaperHeadlineItem(
            id = id,
            publicationName = publicationName,
            sectionKicker = sectionKicker,
            headline = headline,
            subheadline = subheadline,
            byline = byline,
            leadParagraph = decisionSummary,
            tone = tone,
            timestampLabel = timestampLabel,
            category = category,
            relatedEntity = franchiseName
        )
    }
}
