package com.example.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

enum class RuleCommitteeMemberRole(val title: String) {
    COMPETITION_CHAIR("Committee Chair"),
    HEAD_COACHES_REP("Head Coaches Rep"),
    PLAYERS_UNION_LIAISON("Players Union Liaison"),
    OFFICIATING_DIRECTOR("Director of Officiating"),
    ANALYTICS_EXECUTIVE("Competition Analytics VP")
}

data class RuleCommitteeMember(
    val id: String,
    val name: String,
    val role: RuleCommitteeMemberRole,
    val backgroundNote: String,
    val biasDescription: String
)

enum class RuleCommitteeFocusLever(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    OFFENSIVE_TEMPO(
        title = "Offensive Pace & Scoring Surge",
        shortLabel = "+Scoring Pace",
        description = "Accelerates possessions, spacing, or attacking zones to raise team scoring averages."
    ),
    PHYSICAL_DEFENSE(
        title = "Physical Defense & Grit",
        shortLabel = "Defense Grind",
        description = "Tightens defensive contact allowances, reducing scoring totals while lifting steals, blocks & turnovers."
    ),
    PLAYER_SAFETY(
        title = "Player Safety & Load Protection",
        shortLabel = "SafetyShield",
        description = "Eliminates high-risk collisions and lowers in-game injury probabilities across regular & playoff schedules."
    ),
    CLUTCH_OVERTIME(
        title = "Clutch Drama & Tiebreak Reform",
        shortLabel = "Clutch Finish",
        description = "Increases late-game comeback swings, target-score endings, or shootout deciders on tied games."
    ),
    OFFICIATING_INTEGRITY(
        title = "Replay & Officiating Precision",
        shortLabel = "+Integrity",
        description = "Deploys precision review technology, suppressing blown-call late swings and lifting league integrity."
    ),
    UNDERDOG_PARITY(
        title = "Competitive Parity Balance",
        shortLabel = "Underdog Lift",
        description = "Narrows dynasty talent gaps and increases upset variance across divisions."
    )
}

enum class SportRuleProposalStatus(val label: String) {
    PROPOSED_DOCKET("On Docket"),
    COMMITTEE_RECOMMENDED("Committee Approved"),
    TABLED("Tabled by Committee"),
    ENACTED("Enacted Into Law")
}

data class SportRuleAmendment(
    val id: String,
    val sportType: SportType,
    val title: String,
    val categoryLabel: String,
    val sponsorName: String,
    val focusLever: RuleCommitteeFocusLever,
    val summary: String,
    val simulationEffectSummary: String,
    val proPerspective: String,
    val contraPerspective: String,
    val scoringDeltaPerTeam: Double = 0.0,
    val varianceDelta: Int = 0,
    val injuryRiskMultiplier: Double = 1.0,
    val closeGameSwingPct: Int = 0,
    val convertDrawsToShootout: Boolean = false,
    val suppressBlownCallSwing: Boolean = false,
    val underdogBoost: Int = 0,
    val defenseStatMultiplier: Double = 1.0,
    val reboundStatMultiplier: Double = 1.0,
    val integrityDelta: Int = 0,
    val parityDelta: Int = 0,
    val fanAppealDelta: Int = 0,
    val status: SportRuleProposalStatus = SportRuleProposalStatus.PROPOSED_DOCKET,
    val committeeVotesFor: Int = 0,
    val committeeVotesAgainst: Int = 0,
    val committeeReviewNote: String = "",
    val isCustomProposal: Boolean = false
) {
    val isEnacted: Boolean
        get() = status == SportRuleProposalStatus.ENACTED

    val simImpactTags: List<String>
        get() = buildList {
            if (abs(scoringDeltaPerTeam) >= 0.05) {
                val formatted = if (abs(scoringDeltaPerTeam) < 3.0 && sportType in listOf(SportType.SOCCER, SportType.HOCKEY)) {
                    val sign = if (scoringDeltaPerTeam > 0) "+" else ""
                    "$sign${String.format("%.2f", scoringDeltaPerTeam)} Goals/Team"
                } else {
                    val sign = if (scoringDeltaPerTeam > 0) "+" else ""
                    "$sign${scoringDeltaPerTeam.roundToInt()} PPG/Team"
                }
                add(formatted)
            }
            if (abs(injuryRiskMultiplier - 1.0) >= 0.03) {
                add("${String.format("%.2f", injuryRiskMultiplier)}x Injury")
            }
            if (closeGameSwingPct > 0) {
                add("+${closeGameSwingPct}% Late Swing")
            }
            if (convertDrawsToShootout) {
                add("Shootout Decider")
            }
            if (suppressBlownCallSwing) {
                add("Zero Ref Swing")
            }
            if (underdogBoost > 0) {
                add("+${underdogBoost} Underdog")
            }
            if (defenseStatMultiplier > 1.04) {
                add("+${((defenseStatMultiplier - 1.0) * 100).roundToInt()}% Def Stats")
            }
            if (reboundStatMultiplier > 1.04) {
                add("+${((reboundStatMultiplier - 1.0) * 100).roundToInt()}% Reb/Yds")
            }
            if (fanAppealDelta != 0) {
                add("${if (fanAppealDelta > 0) "+" else ""}$fanAppealDelta Fan Excitement")
            }
        }
}

data class RuleSimulationImpactLog(
    val id: String,
    val seasonYear: Int,
    val weekNumber: Int,
    val sportType: SportType,
    val activeRulesCount: Int,
    val activeRuleTitles: List<String>,
    val measuredAvgCombinedScore: Double,
    val sportBaselineCombinedScore: Double,
    val closeOrOvertimeGamesCount: Int,
    val injuriesOccurredCount: Int,
    val featuredImpactHeadline: String
) {
    val deltaVsBaseline: Double
        get() = Math.round((measuredAvgCombinedScore - sportBaselineCombinedScore) * 10.0) / 10.0
}

data class RuleCommitteeModifierSummary(
    val sportType: SportType,
    val activeRuleCount: Int,
    val activeRuleIds: Set<String>,
    val activeRuleTitles: List<String>,
    val scoringDeltaPerTeam: Double,
    val varianceDelta: Int,
    val injuryRiskMultiplier: Double,
    val closeGameSwingPct: Int,
    val convertDrawsToShootout: Boolean,
    val suppressBlownCallSwing: Boolean,
    val underdogBoost: Int,
    val defenseStatMultiplier: Double,
    val reboundStatMultiplier: Double,
    val totalIntegrityDelta: Int,
    val totalParityDelta: Int,
    val totalFanAppealDelta: Int
) {
    companion object {
        val NONE = RuleCommitteeModifierSummary(
            sportType = SportType.BASKETBALL,
            activeRuleCount = 0,
            activeRuleIds = emptySet(),
            activeRuleTitles = emptyList(),
            scoringDeltaPerTeam = 0.0,
            varianceDelta = 0,
            injuryRiskMultiplier = 1.0,
            closeGameSwingPct = 0,
            convertDrawsToShootout = false,
            suppressBlownCallSwing = false,
            underdogBoost = 0,
            defenseStatMultiplier = 1.0,
            reboundStatMultiplier = 1.0,
            totalIntegrityDelta = 0,
            totalParityDelta = 0,
            totalFanAppealDelta = 0
        )
    }
}

data class RuleCommitteeState(
    val sportType: SportType = SportType.BASKETBALL,
    val members: List<RuleCommitteeMember> = emptyList(),
    val proposals: List<SportRuleAmendment> = emptyList(),
    val impactLogs: List<RuleSimulationImpactLog> = emptyList(),
    val committeeApprovalRating: Int = 82
) {
    val enactedProposals: List<SportRuleAmendment>
        get() = proposals.filter { it.status == SportRuleProposalStatus.ENACTED }

    val pendingDocketProposals: List<SportRuleAmendment>
        get() = proposals.filter { it.status != SportRuleProposalStatus.ENACTED }
}

object RuleCommitteeEngine {

    fun defaultMembersForSport(sportType: SportType): List<RuleCommitteeMember> {
        val sportLabel = when (sportType) {
            SportType.BASKETBALL -> "Hardwood"
            SportType.FOOTBALL -> "Gridiron"
            SportType.SOCCER -> "Pitch Laws"
            SportType.HOCKEY -> "Ice Ops"
        }
        return listOf(
            RuleCommitteeMember(
                id = "rc_chair",
                name = "Marcus Vance",
                role = RuleCommitteeMemberRole.COMPETITION_CHAIR,
                backgroundNote = "22-year $sportLabel Front Office Executive",
                biasDescription = "Favors balanced innovation & fan excitement"
            ),
            RuleCommitteeMember(
                id = "rc_coach",
                name = "Elena Rostova",
                role = RuleCommitteeMemberRole.HEAD_COACHES_REP,
                backgroundNote = "2x Championship Coach & Tactician",
                biasDescription = "Prioritizes tactical clarity & officiating fairness"
            ),
            RuleCommitteeMember(
                id = "rc_union",
                name = "Devon Mercer",
                role = RuleCommitteeMemberRole.PLAYERS_UNION_LIAISON,
                backgroundNote = "Former All-League Captain",
                biasDescription = "Champions player safety & lower injury wear"
            ),
            RuleCommitteeMember(
                id = "rc_ref",
                name = "Arthur Bellamy",
                role = RuleCommitteeMemberRole.OFFICIATING_DIRECTOR,
                backgroundNote = "Senior Replay & Rules Arbiter",
                biasDescription = "Backs automated tech & decisive review protocols"
            ),
            RuleCommitteeMember(
                id = "rc_analytics",
                name = "Dr. Priya Nair",
                role = RuleCommitteeMemberRole.ANALYTICS_EXECUTIVE,
                backgroundNote = "Director of Game Tempo & Parity Lab",
                biasDescription = "Supports data-driven tempo, scoring & parity reforms"
            )
        )
    }

    fun defaultProposalsForSport(sportType: SportType): List<SportRuleAmendment> = when (sportType) {
        SportType.BASKETBALL -> listOf(
            SportRuleAmendment(
                id = "rc_bball_4pt_arc",
                sportType = SportType.BASKETBALL,
                title = "30-Foot 4-Point Scoring Arc",
                categoryLabel = "Court Geometry",
                sponsorName = "Analytics Lab • Dr. Priya Nair",
                focusLever = RuleCommitteeFocusLever.OFFENSIVE_TEMPO,
                summary = "Introduces a deep 30-foot arc worth 4 points, stretching floor spacing across all 4 quarters.",
                simulationEffectSummary = "Raises team scoring by ~11 PPG, expands score variance (+4), and triggers deep-shot box-score eruptions.",
                proPerspective = "Creates viral highlight comebacks and rewards elite perimeter shooters.",
                contraPerspective = "Old-school traditionalists warn of 135-point shootouts and inflated records.",
                scoringDeltaPerTeam = 11.0,
                varianceDelta = 4,
                closeGameSwingPct = 14,
                fanAppealDelta = 10,
                parityDelta = -3
            ),
            SportRuleAmendment(
                id = "rc_bball_14s_reset",
                sportType = SportType.BASKETBALL,
                title = "14-Second Offensive Rebound Clock",
                categoryLabel = "Pace & Tempo",
                sponsorName = "Committee Chair • Marcus Vance",
                focusLever = RuleCommitteeFocusLever.OFFENSIVE_TEMPO,
                summary = "Resets shot clock to 14 seconds instead of 24 after offensive rebounds to eliminate late stalling.",
                simulationEffectSummary = "Adds +7.5 PPG per team and +10% rebound/assist volume with a modest 1.08x pace wear factor.",
                proPerspective = "Keeps possessions crisp and increases late-game comeback windows.",
                contraPerspective = "Slightly elevates conditioning fatigue (1.08x injury risk).",
                scoringDeltaPerTeam = 7.5,
                varianceDelta = 2,
                injuryRiskMultiplier = 1.08,
                reboundStatMultiplier = 1.12,
                fanAppealDelta = 7,
                parityDelta = 2
            ),
            SportRuleAmendment(
                id = "rc_bball_hand_check",
                sportType = SportType.BASKETBALL,
                title = "Restore Perimeter Hand-Checking",
                categoryLabel = "Defensive Physicality",
                sponsorName = "Head Coaches Rep • Elena Rostova",
                focusLever = RuleCommitteeFocusLever.PHYSICAL_DEFENSE,
                summary = "Permits legal forearm contact above the free-throw line extended, reviving physical perimeter defense.",
                simulationEffectSummary = "Cuts average team scoring by -10.5 PPG, boosts defensive steals/blocks (+26%), and adds physical wear (1.16x injury).",
                proPerspective = "Rebalances offense vs defense and gives gritty rosters a genuine identity.",
                contraPerspective = "Lower-scoring defensive battles can bruise star guards.",
                scoringDeltaPerTeam = -10.5,
                varianceDelta = -2,
                injuryRiskMultiplier = 1.16,
                defenseStatMultiplier = 1.26,
                integrityDelta = 2,
                parityDelta = 5,
                fanAppealDelta = -2
            ),
            SportRuleAmendment(
                id = "rc_bball_elam_finish",
                sportType = SportType.BASKETBALL,
                title = "Mandatory Elam Target-Score Ending",
                categoryLabel = "Endgame Format",
                sponsorName = "Committee Chair • Marcus Vance",
                focusLever = RuleCommitteeFocusLever.CLUTCH_OVERTIME,
                summary = "Shuts off game clock at 4:00 in Q4; both clubs race to Leading Score + 8 points to finish cleanly.",
                simulationEffectSummary = "Eliminates intentional foul parades, lifts clutch comeback swings (+24%), and produces walk-off winner highlights.",
                proPerspective = "Every single game finishes on a live made basket—zero anticlimax.",
                contraPerspective = "Purists prefer traditional clock-management drama.",
                scoringDeltaPerTeam = 3.0,
                closeGameSwingPct = 24,
                underdogBoost = 2,
                fanAppealDelta = 12,
                parityDelta = 4
            ),
            SportRuleAmendment(
                id = "rc_bball_challenge_retain",
                sportType = SportType.BASKETBALL,
                title = "Retain Coach Challenge & Replay Shield",
                categoryLabel = "Officiating Protocol",
                sponsorName = "Officiating Director • Arthur Bellamy",
                focusLever = RuleCommitteeFocusLever.OFFICIATING_INTEGRITY,
                summary = "Coaches keep their replay challenge on successful overturns, paired with automated out-of-bounds sensors.",
                simulationEffectSummary = "Eliminates late-game blown-call score reversals in tight games and raises League Integrity (+8).",
                proPerspective = "Protects authentic outcomes in 1-possession classics.",
                contraPerspective = "Can add brief review stoppages down the stretch.",
                suppressBlownCallSwing = true,
                integrityDelta = 8,
                parityDelta = 3,
                fanAppealDelta = 4
            ),
            SportRuleAmendment(
                id = "rc_bball_transition_safety",
                sportType = SportType.BASKETBALL,
                title = "Landing-Space & Take-Foul Protection",
                categoryLabel = "Player Welfare",
                sponsorName = "Union Liaison • Devon Mercer",
                focusLever = RuleCommitteeFocusLever.PLAYER_SAFETY,
                summary = "Enforces strict flagrant penalties on shooter landing-zone encroachment and clears transition paths.",
                simulationEffectSummary = "Slashes in-game injury odds to 0.78x while adding +4.0 PPG from transition bonus possessions.",
                proPerspective = "Keeps marquee stars healthy across the full 16-week grind.",
                contraPerspective = "Slows down physical fast-break disruption tactics.",
                scoringDeltaPerTeam = 4.0,
                injuryRiskMultiplier = 0.78,
                integrityDelta = 3,
                fanAppealDelta = 5
            )
        )

        SportType.FOOTBALL -> listOf(
            SportRuleAmendment(
                id = "rc_fball_4th_15_alt",
                sportType = SportType.FOOTBALL,
                title = "4th-and-15 Onside Kick Alternative",
                categoryLabel = "Special Teams / Comeback",
                sponsorName = "Analytics Lab • Dr. Priya Nair",
                focusLever = RuleCommitteeFocusLever.CLUTCH_OVERTIME,
                summary = "Trailing teams in Q4 may opt for one 4th-and-15 offensive snap from their own 25 instead of an onside kick.",
                simulationEffectSummary = "Boosts Q4 comeback probability (+28%), lifts team scoring (+4.5 PPG), and fuels thrilling late rallies.",
                proPerspective = "Turns hopeless 2-score deficits into electrifying late-game television.",
                contraPerspective = "Defensive coaches hate risking 14-point leads on one penalty flag.",
                scoringDeltaPerTeam = 4.5,
                varianceDelta = 4,
                closeGameSwingPct = 28,
                underdogBoost = 3,
                fanAppealDelta = 12
            ),
            SportRuleAmendment(
                id = "rc_fball_dynamic_kickoff",
                sportType = SportType.FOOTBALL,
                title = "Dynamic Low-Impact Kickoff & 35 Touchback",
                categoryLabel = "Player Safety",
                sponsorName = "Union Liaison • Devon Mercer",
                focusLever = RuleCommitteeFocusLever.PLAYER_SAFETY,
                summary = "Aligns kick coverage units 5 yards apart to eliminate high-speed collisions while moving touchbacks to the 35.",
                simulationEffectSummary = "Cuts injury risk to 0.76x across the league while improving field position (+3.5 PPG).",
                proPerspective = "Dramatically reduces special-teams concussions while preserving returns.",
                contraPerspective = "Gives offenses shorter fields on average.",
                scoringDeltaPerTeam = 3.5,
                injuryRiskMultiplier = 0.76,
                reboundStatMultiplier = 1.08,
                integrityDelta = 3,
                fanAppealDelta = 5
            ),
            SportRuleAmendment(
                id = "rc_fball_strict_pass_pi",
                sportType = SportType.FOOTBALL,
                title = "5-Yard Strict Contact & Passing Window",
                categoryLabel = "Air Raid Tempo",
                sponsorName = "Committee Chair • Marcus Vance",
                focusLever = RuleCommitteeFocusLever.OFFENSIVE_TEMPO,
                summary = "Strictly penalizes defensive holding beyond 5 yards, opening explosive downfield passing lanes.",
                simulationEffectSummary = "Surges scoring by +6.5 PPG per team and increases passing/receiving yardage (+16%).",
                proPerspective = "Delivers 38-34 quarterback duels and record-breaking passing nights.",
                contraPerspective = "Corners and pass rushers lose leverage in red-zone coverage.",
                scoringDeltaPerTeam = 6.5,
                varianceDelta = 3,
                reboundStatMultiplier = 1.16,
                fanAppealDelta = 9,
                parityDelta = -2
            ),
            SportRuleAmendment(
                id = "rc_fball_double_ot_possess",
                sportType = SportType.FOOTBALL,
                title = "Guaranteed Dual-Possession Overtime",
                categoryLabel = "Overtime Equality",
                sponsorName = "Head Coaches Rep • Elena Rostova",
                focusLever = RuleCommitteeFocusLever.OFFICIATING_INTEGRITY,
                summary = "Guarantees both clubs at least one offensive overtime drive regardless of opening-drive touchdowns.",
                simulationEffectSummary = "Eliminates coin-toss sudden-death outcomes (+6 Integrity) and lifts close-game totals (+2.5 PPG).",
                proPerspective = "Both offenses and defenses decide playoff-style finishes fairly.",
                contraPerspective = "Extends tight overtime games slightly (+1.06x injury risk).",
                scoringDeltaPerTeam = 2.5,
                injuryRiskMultiplier = 1.06,
                closeGameSwingPct = 15,
                suppressBlownCallSwing = true,
                integrityDelta = 6,
                parityDelta = 4,
                fanAppealDelta = 8
            ),
            SportRuleAmendment(
                id = "rc_fball_continuous_clock",
                sportType = SportType.FOOTBALL,
                title = "Continuous Out-of-Bounds Clock Protocol",
                categoryLabel = "Defensive Grind",
                sponsorName = "Head Coaches Rep • Elena Rostova",
                focusLever = RuleCommitteeFocusLever.PHYSICAL_DEFENSE,
                summary = "Keeps game clock rolling on out-of-bounds plays prior to the final 2 minutes, favoring physical run games.",
                simulationEffectSummary = "Suppresses average team scores by -5.5 PPG while rewarding ball-control defenses (+18% tackles/sacks).",
                proPerspective = "Restores smashmouth clock-control football and tightens upsets.",
                contraPerspective = "Fewer total plays for fantasy offense fans.",
                scoringDeltaPerTeam = -5.5,
                varianceDelta = -2,
                defenseStatMultiplier = 1.18,
                parityDelta = 5,
                fanAppealDelta = -2
            ),
            SportRuleAmendment(
                id = "rc_fball_skyjudge_var",
                sportType = SportType.FOOTBALL,
                title = "Full-Time SkyJudge Penalty Assist",
                categoryLabel = "Replay & Precision",
                sponsorName = "Officiating Director • Arthur Bellamy",
                focusLever = RuleCommitteeFocusLever.OFFICIATING_INTEGRITY,
                summary = "Empowers booth official with live headset feed to fix missed pass interference and roughing calls instantly.",
                simulationEffectSummary = "Blocks late-game blown-call reversals completely and lifts League Integrity (+9).",
                proPerspective = "Prevents viral blown-call controversies on Monday morning.",
                contraPerspective = "Adds marginal review scrutiny in trenches.",
                suppressBlownCallSwing = true,
                integrityDelta = 9,
                parityDelta = 2,
                fanAppealDelta = 4
            )
        )

        SportType.SOCCER -> listOf(
            SportRuleAmendment(
                id = "rc_soc_daylight_offside",
                sportType = SportType.SOCCER,
                title = "Full-Daylight Striker Offside Law",
                categoryLabel = "Attacking Law",
                sponsorName = "Analytics Lab • Dr. Priya Nair",
                focusLever = RuleCommitteeFocusLever.OFFENSIVE_TEMPO,
                summary = "A striker is only offside if their entire body clears the second-last defender—ending millimeter VAR denials.",
                simulationEffectSummary = "Increases goal output by +0.95 goals per club (~3-2 & 4-2 scorelines) and lifts fan excitement (+11).",
                proPerspective = "Unleashes thrilling counter-attacks and cuts frustrating disallowed goals.",
                contraPerspective = "High defensive lines are forced to drop deeper.",
                scoringDeltaPerTeam = 0.95,
                varianceDelta = 1,
                reboundStatMultiplier = 1.15,
                fanAppealDelta = 11,
                parityDelta = 2
            ),
            SportRuleAmendment(
                id = "rc_soc_draw_shootout",
                sportType = SportType.SOCCER,
                title = "Decisive Penalty Shootout on Regulation Draws",
                categoryLabel = "Match Tiebreaker",
                sponsorName = "Committee Chair • Marcus Vance",
                focusLever = RuleCommitteeFocusLever.CLUTCH_OVERTIME,
                summary = "Every tied league match advances directly to a 5-kick penalty shootout for a decisive winner.",
                simulationEffectSummary = "Converts tied matches into dramatic penalty shootout victories and highlights.",
                proPerspective = "Every stadium ticket ends with a clear match-day winner.",
                contraPerspective = "Traditionalists cherish hard-fought away draws.",
                scoringDeltaPerTeam = 0.25,
                closeGameSwingPct = 22,
                convertDrawsToShootout = true,
                fanAppealDelta = 10,
                parityDelta = 3
            ),
            SportRuleAmendment(
                id = "rc_soc_stoppage_clock",
                sportType = SportType.SOCCER,
                title = "60-Minute Effective Stopped Clock",
                categoryLabel = "Anti-Timewasting",
                sponsorName = "Officiating Director • Arthur Bellamy",
                focusLever = RuleCommitteeFocusLever.OFFENSIVE_TEMPO,
                summary = "Halts match clock during throw-ins, goal kicks, and injuries, adding +12 minutes of genuine open play.",
                simulationEffectSummary = "Raises team scoring (+0.75 goals/team) and eliminates late time-wasting stalls.",
                proPerspective = "Fans get true non-stop ball-in-play value.",
                contraPerspective = "Adds extra late-match running fatigue (1.07x injury risk).",
                scoringDeltaPerTeam = 0.75,
                injuryRiskMultiplier = 1.07,
                integrityDelta = 5,
                fanAppealDelta = 8
            ),
            SportRuleAmendment(
                id = "rc_soc_tactical_sin_bin",
                sportType = SportType.SOCCER,
                title = "10-Minute Blue Card Tactical Sin Bin",
                categoryLabel = "Discipline & Space",
                sponsorName = "Head Coaches Rep • Elena Rostova",
                focusLever = RuleCommitteeFocusLever.UNDERDOG_PARITY,
                summary = "Cynical midfield tactical fouls earn an immediate 10-minute off-pitch suspension, creating 11v10 openings.",
                simulationEffectSummary = "Boosts scoring (+0.55 goals/team), gives fast underdogs breakaway chances (+4 Underdog), and cuts cynical play.",
                proPerspective = "Restores flowing counter-attacks when favorites try to break up tempo.",
                contraPerspective = "Managers argue temporary 10-man windows swing momentum too sharply.",
                scoringDeltaPerTeam = 0.55,
                closeGameSwingPct = 15,
                underdogBoost = 4,
                integrityDelta = 4,
                parityDelta = 5
            ),
            SportRuleAmendment(
                id = "rc_soc_five_subs",
                sportType = SportType.SOCCER,
                title = "Five-Substitute Welfare & Fresh Legs Mandate",
                categoryLabel = "Player Welfare",
                sponsorName = "Union Liaison • Devon Mercer",
                focusLever = RuleCommitteeFocusLever.PLAYER_SAFETY,
                summary = "Allows 5 tactical substitutions plus temporary concussion replacements to safeguard star stamina.",
                simulationEffectSummary = "Slashes match injury probability to 0.75x while sustaining late-game defensive energy.",
                proPerspective = "Protects players across dense continental fixtures and lowers hamstring tears.",
                contraPerspective = "Favors clubs with deeper benches.",
                injuryRiskMultiplier = 0.75,
                defenseStatMultiplier = 1.10,
                parityDelta = -1,
                fanAppealDelta = 3
            ),
            SportRuleAmendment(
                id = "rc_soc_catenaccio_var",
                sportType = SportType.SOCCER,
                title = "Semi-Automated Laser VAR & Keeper Shield",
                categoryLabel = "Defensive Integrity",
                sponsorName = "Officiating Director • Arthur Bellamy",
                focusLever = RuleCommitteeFocusLever.OFFICIATING_INTEGRITY,
                summary = "Deploys 3D limb-tracking offside lines and strict 6-yard box protection for goalkeepers.",
                simulationEffectSummary = "Eliminates blown-call swing outcomes, trims scoring slightly (-0.35 goals/team), and raises Integrity (+9).",
                proPerspective = "Makes every goal and clean sheet 100% indisputable.",
                contraPerspective = "Marginally fewer scrappy set-piece goals.",
                scoringDeltaPerTeam = -0.35,
                suppressBlownCallSwing = true,
                defenseStatMultiplier = 1.15,
                integrityDelta = 9,
                parityDelta = 3
            )
        )

        SportType.HOCKEY -> listOf(
            SportRuleAmendment(
                id = "rc_hock_full_pp",
                sportType = SportType.HOCKEY,
                title = "Full 2-Minute Minor Power Play (Uninterrupted)",
                categoryLabel = "Special Teams",
                sponsorName = "Analytics Lab • Dr. Priya Nair",
                focusLever = RuleCommitteeFocusLever.OFFENSIVE_TEMPO,
                summary = "Teams remain short-handed for the full 2:00 penalty duration even if the power play scores once.",
                simulationEffectSummary = "Surges scoring by +1.15 goals/team and lifts shots/assists across power-play units.",
                proPerspective = "Deters penalties while turning special teams into explosive showcases.",
                contraPerspective = "Punishes physical stay-at-home defensemen.",
                scoringDeltaPerTeam = 1.15,
                varianceDelta = 1,
                reboundStatMultiplier = 1.14,
                fanAppealDelta = 10
            ),
            SportRuleAmendment(
                id = "rc_hock_smaller_pads",
                sportType = SportType.HOCKEY,
                title = "Contoured Streamlined Goalie Pad Spec",
                categoryLabel = "Equipment Standards",
                sponsorName = "Committee Chair • Marcus Vance",
                focusLever = RuleCommitteeFocusLever.OFFENSIVE_TEMPO,
                summary = "Trims oversized chest protectors and blocker-pad width by 12% to restore sniper scoring geometry.",
                simulationEffectSummary = "Directly lifts league goals by +1.30 goals per team and spikes highlight-reel finishes.",
                proPerspective = "Brings back classic 5-4 barnburners and 50-goal star chases.",
                contraPerspective = "Goaltenders demand enhanced torso kevlar inserts.",
                scoringDeltaPerTeam = 1.30,
                varianceDelta = 1,
                fanAppealDelta = 11,
                parityDelta = -2
            ),
            SportRuleAmendment(
                id = "rc_hock_3on3_continuous",
                sportType = SportType.HOCKEY,
                title = "Continuous 3-on-3 Overtime (No Shootout)",
                categoryLabel = "Overtime Format",
                sponsorName = "Head Coaches Rep • Elena Rostova",
                focusLever = RuleCommitteeFocusLever.CLUTCH_OVERTIME,
                summary = "Extends open-ice 3-on-3 overtime until a sudden-death goal decides the game, scrapping skills shootouts.",
                simulationEffectSummary = "Boosts overtime scoring (+0.65 goals/team), increases late comeback flips (+20%), and wins purist acclaim.",
                proPerspective = "Delivers non-stop 2-on-1 breakaways until someone scores a real hockey goal.",
                contraPerspective = "Extended OT shifts slightly tax top defense pairs (1.06x fatigue risk).",
                scoringDeltaPerTeam = 0.65,
                injuryRiskMultiplier = 1.06,
                closeGameSwingPct = 20,
                convertDrawsToShootout = true,
                fanAppealDelta = 12
            ),
            SportRuleAmendment(
                id = "rc_hock_safety_guards",
                sportType = SportType.HOCKEY,
                title = "Mandatory Cut-Resistant Neck & Visor Shield",
                categoryLabel = "Player Safety",
                sponsorName = "Union Liaison • Devon Mercer",
                focusLever = RuleCommitteeFocusLever.PLAYER_SAFETY,
                summary = "Requires Kevlar neck guards and hybrid half-visors for every active skater on game night.",
                simulationEffectSummary = "Reduces match injury frequency to 0.72x across the entire league and raises Union peace.",
                proPerspective = "Prevents catastrophic skate/high-stick lacerations across 16 weeks.",
                contraPerspective = "Veterans take time adjusting to neck collar feel.",
                injuryRiskMultiplier = 0.72,
                integrityDelta = 4,
                fanAppealDelta = 3
            ),
            SportRuleAmendment(
                id = "rc_hock_deadpuck_trap",
                sportType = SportType.HOCKEY,
                title = "Permissive Blue-Line Neutral Zone Trap",
                categoryLabel = "Defensive Structure",
                sponsorName = "Head Coaches Rep • Elena Rostova",
                focusLever = RuleCommitteeFocusLever.PHYSICAL_DEFENSE,
                summary = "Allows tight neutral-zone clutching and crease battle clears, favoring tight 2-1 playoff-style defenses.",
                simulationEffectSummary = "Drops scoring by -0.85 goals/team while boosting hits/blocked shots (+24%) and underdog upsets (+3).",
                proPerspective = "Levels the playing field for gritty, defense-first expansion clubs.",
                contraPerspective = "Restricts high-flying offensive pace.",
                scoringDeltaPerTeam = -0.85,
                underdogBoost = 3,
                defenseStatMultiplier = 1.24,
                parityDelta = 6,
                fanAppealDelta = -3
            ),
            SportRuleAmendment(
                id = "rc_hock_coach_video",
                sportType = SportType.HOCKEY,
                title = "Automated Puck-Tracking Goal & Minor Review",
                categoryLabel = "Officiating Precision",
                sponsorName = "Officiating Director • Arthur Bellamy",
                focusLever = RuleCommitteeFocusLever.OFFICIATING_INTEGRITY,
                summary = "Embeds microchips inside every game puck for instant goal-line confirmation and double-minor reviews.",
                simulationEffectSummary = "Eliminates phantom goal-line swings and boosts League Integrity (+8).",
                proPerspective = "100% certainty on kicked/deflected crease scrambles.",
                contraPerspective = "No impact on live skating flow.",
                suppressBlownCallSwing = true,
                integrityDelta = 8,
                parityDelta = 2,
                fanAppealDelta = 4
            )
        )
    }

    fun createDefaultState(sportType: SportType = SportType.BASKETBALL): RuleCommitteeState {
        return RuleCommitteeState(
            sportType = sportType,
            members = defaultMembersForSport(sportType),
            proposals = defaultProposalsForSport(sportType),
            impactLogs = emptyList(),
            committeeApprovalRating = 82
        )
    }

    fun computeSimulationModifiers(
        state: RuleCommitteeState,
        sportType: SportType
    ): RuleCommitteeModifierSummary {
        val enacted = state.proposals.filter {
            it.status == SportRuleProposalStatus.ENACTED && it.sportType == sportType
        }
        if (enacted.isEmpty()) {
            return RuleCommitteeModifierSummary.NONE.copy(sportType = sportType)
        }

        val scoreDelta = enacted.sumOf { it.scoringDeltaPerTeam }
        val varDelta = enacted.sumOf { it.varianceDelta }.coerceIn(-4, 8)
        val injuryMult = enacted.fold(1.0) { acc, item -> acc * item.injuryRiskMultiplier }
            .coerceIn(0.45, 1.85)
        val clutchSwing = enacted.sumOf { it.closeGameSwingPct }.coerceIn(0, 60)
        val shootout = enacted.any { it.convertDrawsToShootout }
        val suppressBlown = enacted.any { it.suppressBlownCallSwing }
        val underdogBoost = enacted.sumOf { it.underdogBoost }.coerceIn(0, 10)
        val defMult = enacted.fold(1.0) { acc, item -> acc * item.defenseStatMultiplier }
            .coerceIn(0.85, 1.75)
        val rebMult = enacted.fold(1.0) { acc, item -> acc * item.reboundStatMultiplier }
            .coerceIn(0.85, 1.75)

        return RuleCommitteeModifierSummary(
            sportType = sportType,
            activeRuleCount = enacted.size,
            activeRuleIds = enacted.map { it.id }.toSet(),
            activeRuleTitles = enacted.map { it.title },
            scoringDeltaPerTeam = Math.round(scoreDelta * 100.0) / 100.0,
            varianceDelta = varDelta,
            injuryRiskMultiplier = Math.round(injuryMult * 100.0) / 100.0,
            closeGameSwingPct = clutchSwing,
            convertDrawsToShootout = shootout,
            suppressBlownCallSwing = suppressBlown,
            underdogBoost = underdogBoost,
            defenseStatMultiplier = Math.round(defMult * 100.0) / 100.0,
            reboundStatMultiplier = Math.round(rebMult * 100.0) / 100.0,
            totalIntegrityDelta = enacted.sumOf { it.integrityDelta },
            totalParityDelta = enacted.sumOf { it.parityDelta },
            totalFanAppealDelta = enacted.sumOf { it.fanAppealDelta }
        )
    }

    fun evaluateCommitteeReview(
        proposal: SportRuleAmendment,
        members: List<RuleCommitteeMember>
    ): SportRuleAmendment {
        var yes = 0
        var no = 0
        members.forEach { member ->
            val supports = when (member.role) {
                RuleCommitteeMemberRole.COMPETITION_CHAIR -> proposal.fanAppealDelta >= 0 || proposal.parityDelta >= 0
                RuleCommitteeMemberRole.HEAD_COACHES_REP -> proposal.focusLever != RuleCommitteeFocusLever.OFFENSIVE_TEMPO || proposal.varianceDelta <= 3
                RuleCommitteeMemberRole.PLAYERS_UNION_LIAISON -> proposal.injuryRiskMultiplier <= 1.09
                RuleCommitteeMemberRole.OFFICIATING_DIRECTOR -> proposal.integrityDelta >= 0 && !proposal.title.contains("Abuse", ignoreCase = true)
                RuleCommitteeMemberRole.ANALYTICS_EXECUTIVE -> abs(proposal.scoringDeltaPerTeam) > 0.1 || proposal.closeGameSwingPct > 0 || proposal.underdogBoost > 0
            }
            if (supports) yes++ else no++
        }
        val recommended = yes >= 3
        val note = if (recommended) {
            "Committee endorsed ($yes-$no): Cleared for league implementation."
        } else {
            "Committee tabled ($yes-$no): Union/Coaching concerns noted; Commissioner may still enact."
        }
        return proposal.copy(
            status = if (recommended) SportRuleProposalStatus.COMMITTEE_RECOMMENDED else SportRuleProposalStatus.TABLED,
            committeeVotesFor = yes,
            committeeVotesAgainst = no,
            committeeReviewNote = note
        )
    }

    fun draftCustomProposal(
        sportType: SportType,
        title: String,
        sponsorName: String,
        focusLever: RuleCommitteeFocusLever,
        summaryInput: String,
        intensityLevel: Int = 2 // 1 = Moderate, 2 = Bold, 3 = Radical
    ): SportRuleAmendment {
        val cleanTitle = title.trim().ifBlank {
            "${sportType.displayName} ${focusLever.shortLabel} Reform"
        }
        val scale = intensityLevel.coerceIn(1, 3)
        val isLowScoringSport = sportType == SportType.SOCCER || sportType == SportType.HOCKEY

        val scoreDelta = when (focusLever) {
            RuleCommitteeFocusLever.OFFENSIVE_TEMPO -> if (isLowScoringSport) 0.45 * scale else if (sportType == SportType.FOOTBALL) 2.5 * scale else 4.5 * scale
            RuleCommitteeFocusLever.PHYSICAL_DEFENSE -> if (isLowScoringSport) -0.35 * scale else if (sportType == SportType.FOOTBALL) -2.2 * scale else -4.0 * scale
            RuleCommitteeFocusLever.PLAYER_SAFETY -> if (isLowScoringSport) 0.10 * scale else 1.5 * scale
            RuleCommitteeFocusLever.CLUTCH_OVERTIME -> if (isLowScoringSport) 0.25 * scale else 2.0 * scale
            RuleCommitteeFocusLever.OFFICIATING_INTEGRITY -> 0.0
            RuleCommitteeFocusLever.UNDERDOG_PARITY -> if (isLowScoringSport) 0.15 * scale else 1.5 * scale
        }

        val injMult = when (focusLever) {
            RuleCommitteeFocusLever.PLAYER_SAFETY -> (1.0 - (0.09 * scale)).coerceAtLeast(0.68)
            RuleCommitteeFocusLever.PHYSICAL_DEFENSE -> 1.0 + (0.07 * scale)
            RuleCommitteeFocusLever.OFFENSIVE_TEMPO -> 1.0 + (0.03 * scale)
            else -> 1.0
        }

        val cleanSummary = summaryInput.trim().ifBlank {
            "Commissioner-drafted ${sportType.displayName} competition rule targeting ${focusLever.title.lowercase()} (Tier $scale)."
        }

        return SportRuleAmendment(
            id = "rc_custom_${sportType.name.lowercase()}_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}",
            sportType = sportType,
            title = cleanTitle,
            categoryLabel = "Commissioner Proposal",
            sponsorName = sponsorName.trim().ifBlank { "League Commissioner Office" },
            focusLever = focusLever,
            summary = cleanSummary,
            simulationEffectSummary = "Custom ${focusLever.shortLabel} directive directly adjusts live match scoring, player stats & weekly outcomes.",
            proPerspective = "Directly addresses Commissioner strategic mandate for ${sportType.displayName}.",
            contraPerspective = "Aggressive mid-cycle rule shifts require committee & owner buy-in.",
            scoringDeltaPerTeam = Math.round(scoreDelta * 100.0) / 100.0,
            varianceDelta = if (focusLever == RuleCommitteeFocusLever.UNDERDOG_PARITY || focusLever == RuleCommitteeFocusLever.OFFENSIVE_TEMPO) scale else 0,
            injuryRiskMultiplier = Math.round(injMult * 100.0) / 100.0,
            closeGameSwingPct = if (focusLever == RuleCommitteeFocusLever.CLUTCH_OVERTIME) 8 * scale else 0,
            convertDrawsToShootout = focusLever == RuleCommitteeFocusLever.CLUTCH_OVERTIME && isLowScoringSport,
            suppressBlownCallSwing = focusLever == RuleCommitteeFocusLever.OFFICIATING_INTEGRITY,
            underdogBoost = if (focusLever == RuleCommitteeFocusLever.UNDERDOG_PARITY) 2 * scale else 0,
            defenseStatMultiplier = if (focusLever == RuleCommitteeFocusLever.PHYSICAL_DEFENSE) 1.0 + (0.09 * scale) else 1.0,
            reboundStatMultiplier = if (focusLever == RuleCommitteeFocusLever.OFFENSIVE_TEMPO) 1.0 + (0.06 * scale) else 1.0,
            integrityDelta = if (focusLever == RuleCommitteeFocusLever.OFFICIATING_INTEGRITY) 3 * scale else 1,
            parityDelta = if (focusLever == RuleCommitteeFocusLever.UNDERDOG_PARITY) 3 * scale else 0,
            fanAppealDelta = if (focusLever == RuleCommitteeFocusLever.PHYSICAL_DEFENSE) -1 else 3 * scale,
            status = SportRuleProposalStatus.PROPOSED_DOCKET,
            isCustomProposal = true
        )
    }

    fun sportBaselineCombinedScore(sportType: SportType): Double = when (sportType) {
        SportType.BASKETBALL -> 212.0
        SportType.FOOTBALL -> 48.0
        SportType.SOCCER -> 4.0
        SportType.HOCKEY -> 6.0
    }

    fun buildImpactLog(
        seasonYear: Int,
        weekNumber: Int,
        sportType: SportType,
        playedMatches: List<GameMatch>,
        modifiers: RuleCommitteeModifierSummary,
        injuriesCount: Int
    ): RuleSimulationImpactLog {
        val avgCombined = if (playedMatches.isNotEmpty()) {
            val raw = playedMatches.map { (it.homeScore + it.awayScore).toDouble() }.average()
            Math.round(raw * 10.0) / 10.0
        } else {
            sportBaselineCombinedScore(sportType)
        }
        val closeThreshold = when (sportType) {
            SportType.BASKETBALL -> 6
            SportType.FOOTBALL -> 7
            SportType.SOCCER, SportType.HOCKEY -> 1
        }
        val closeCount = playedMatches.count { abs(it.homeScore - it.awayScore) <= closeThreshold }
        val featured = playedMatches.firstOrNull { it.ruleImpactNote.isNotBlank() }?.let {
            "${it.ruleImpactNote} (${it.homeScore}-${it.awayScore})"
        } ?: playedMatches.firstOrNull()?.highlightStory ?: "Week $weekNumber simulation completed."

        return RuleSimulationImpactLog(
            id = "rc_log_y${seasonYear}_w${weekNumber}",
            seasonYear = seasonYear,
            weekNumber = weekNumber,
            sportType = sportType,
            activeRulesCount = modifiers.activeRuleCount,
            activeRuleTitles = modifiers.activeRuleTitles,
            measuredAvgCombinedScore = avgCombined,
            sportBaselineCombinedScore = sportBaselineCombinedScore(sportType),
            closeOrOvertimeGamesCount = closeCount,
            injuriesOccurredCount = injuriesCount,
            featuredImpactHeadline = featured
        )
    }
}
