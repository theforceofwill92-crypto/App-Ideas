package com.example.model

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Procedural Draft Class Generator that creates realistic draft classes with
 * current Overall ratings (OVR), numeric Potential ceilings (POT), upside deltas,
 * combine metrics, and collegiate production tailored to each sport.
 */
object DraftClassGenerator {

    private val firstNames = listOf(
        "Marcus", "Donovan", "Tariq", "Elena", "Lucas", "Mateo", "Darius", "Jalen",
        "Viktor", "Zion", "Caleb", "Kobe", "Nikola", "Luka", "Sandro", "Amari",
        "Kendrick", "Devin", "Dante", "Jaden", "Malik", "Isaiah", "Giancarlo", "Tre",
        "Camden", "Rocco", "Kieran", "Xavier", "enzo", "Bryce", "Tyson", "Kade"
    ).map { it.replaceFirstChar { ch -> ch.uppercase() } }

    private val lastNames = listOf(
        "Vance", "Mercer", "Sterling", "Kowalski", "Santos", "Novak", "O'Connor",
        "Washington", "Dubois", "Alvarez", "Chen", "Katsaros", "Fontaine", "Thornton",
        "Lindqvist", "Castillo", "Moreno", "Blackwood", "Ramirez", "St. John", "Holloway",
        "Vaughn", "Prescott", "Diallo", "Kessler", "Gallagher", "Monroe", "Bellamy"
    )

    private val collegesAndClubs = listOf(
        "Duke Blue Devils" to "ACC",
        "Kentucky Wildcats" to "SEC",
        "Real Madrid Baloncesto" to "EuroLeague U21",
        "Gonzaga Bulldogs" to "WCC",
        "UCLA Bruins" to "Big Ten",
        "Paris Basketball Academy" to "LNB Espoirs",
        "North Carolina Tar Heels" to "ACC",
        "Kansas Jayhawks" to "Big 12",
        "UConn Huskies" to "Big East",
        "Villanova Wildcats" to "Big East",
        "Arizona Wildcats" to "Big 12",
        "Michigan Wolverines" to "Big Ten",
        "Alabama Crimson Tide" to "SEC",
        "Oregon Ducks" to "Big Ten",
        "Texas Longhorns" to "SEC",
        "Mega Belgrade Academy" to "ABA League"
    )

    private val classYears = listOf("Freshman", "Sophomore", "Junior", "Senior", "Int'l Pro")

    private val strengthTags = listOf(
        "Elite three-level shot creation",
        "Switchable multi-position defense",
        "High-IQ pick-and-roll orchestration",
        "Relentless downhill rim pressure",
        "Lockdown weak-side help instincts",
        "Clutch fourth-quarter poise",
        "Fluid lateral agility & combine burst",
        "Rapid catch-and-shoot release",
        "Physical rebound & box-out anchor",
        "Precision transition vision"
    )

    private val riskNotes = listOf(
        "Needs added core strength to absorb pro physicality",
        "Shot consistency dips under heavy late-clock pressure",
        "Can gamble for steals and fall out of defensive positioning",
        "Foul discipline against veteran scorers requires polish",
        "Off-hand finishing craft still developing under contact",
        "High-usage style must adapt smoothly to structured pro sets"
    )

    private val accoladesList = listOf(
        "Consensus 1st-Team All-American • Conference POY",
        "Freshman of the Year • Final Four Most Outstanding Player",
        "National Defensive Player of the Year • 1st-Team All-Conf",
        "International U21 MVP • Tournament Scoring Leader",
        "Conference Scoring Champion • National Award Finalist",
        "2nd-Team All-American • All-Tournament Team",
        "All-Conference 1st-Team • League Efficiency Leader"
    )

    private val proComps = listOf(
        "Prime All-League Dual-Threat Lead",
        "Switchable Two-Way All-Star Wing",
        "High-Motor Championship Anchor",
        "Modern Stretch Floor Commander",
        "High-Efficiency Three-Level Scorer",
        "Defensive Cornerstone & Transition Catalyst"
    )

    fun potentialGradeFor(potentialRating: Int): String = when {
        potentialRating >= 94 -> "A+"
        potentialRating >= 89 -> "A"
        potentialRating >= 85 -> "A-"
        potentialRating >= 81 -> "B+"
        potentialRating >= 77 -> "B"
        else -> "B-"
    }

    fun generateProceduralClass(
        year: Int,
        poolSize: Int,
        sportType: SportType,
        seed: Long? = null
    ): List<RookieProspect> {
        val rng = Random(seed ?: (year * 7919L + sportType.ordinal * 313L + poolSize * 41L))
        val positions = sportType.starterPositions
        val archetypes = sportType.archetypes

        val (pLabel, sLabel, tLabel) = when (sportType) {
            SportType.BASKETBALL -> Triple("PPG", "RPG", "APG")
            SportType.FOOTBALL -> Triple("YPG", "TD/G", "QBR/TKL")
            SportType.SOCCER -> Triple("G/30", "SOT/G", "KP/90")
            SportType.HOCKEY -> Triple("PTS/G", "SOG/G", "+/-")
        }

        val usedNames = mutableSetOf<String>()
        val draftEntries = mutableListOf<RookieProspect>()

        for (idx in 0 until poolSize) {
            var attempts = 0
            var fullName: String
            do {
                val fName = firstNames[rng.nextInt(firstNames.size)]
                val lName = lastNames[rng.nextInt(lastNames.size)]
                fullName = "$fName $lName"
                attempts++
            } while (!usedNames.add(fullName) && attempts < 40)
            if (!usedNames.contains(fullName)) {
                fullName = "${firstNames[idx % firstNames.size]} ${lastNames[(idx + 7) % lastNames.size]} Jr."
                usedNames.add(fullName)
            }

            val cYear = classYears[rng.nextInt(classYears.size)]
            val age = when (cYear) {
                "Freshman" -> 19
                "Sophomore", "Int'l Pro" -> 20
                "Junior" -> 21
                else -> 22
            }

            // Tiered base overall with procedural variation + age-linked potential upside
            val tierRatio = idx.toDouble() / poolSize.coerceAtLeast(1).toDouble()
            val baseOvr = when {
                idx == 0 -> rng.nextInt(82, 87)
                idx in 1..2 -> rng.nextInt(79, 85)
                tierRatio < 0.35 -> rng.nextInt(75, 82)
                tierRatio < 0.70 -> rng.nextInt(71, 78)
                else -> rng.nextInt(67, 75)
            }
            // Younger raw prospects get higher upside variance; seniors get higher current OVR floor
            val ageOvrAdj = if (age >= 22) rng.nextInt(1, 4) else if (age == 19) rng.nextInt(-2, 2) else 0
            val overallRating = (baseOvr + ageOvrAdj).coerceIn(66, 87)

            val rawUpsideBonus = when (age) {
                19 -> rng.nextInt(10, 21)
                20 -> rng.nextInt(8, 18)
                21 -> rng.nextInt(5, 13)
                else -> rng.nextInt(3, 9)
            }
            val tierCeilingBoost = if (idx < 3) rng.nextInt(3, 7) else rng.nextInt(0, 4)
            val potentialRating = (overallRating + rawUpsideBonus + tierCeilingBoost).coerceIn(overallRating + 2, 99)
            val potentialGrade = potentialGradeFor(potentialRating)

            val scoutBase = ((overallRating * 0.55) + (potentialRating * 0.45) + rng.nextInt(-2, 3))
                .roundToInt()
                .coerceIn(70, 99)

            val pos = positions[(idx + rng.nextInt(positions.size)) % positions.size]
            val archetype = archetypes[rng.nextInt(archetypes.size)]
            val (school, conf) = collegesAndClubs[rng.nextInt(collegesAndClubs.size)]

            val collegeScore = (scoutBase - 1 + rng.nextInt(-3, 4)).coerceIn(70, 98)
            val combineScore = (scoutBase + rng.nextInt(-4, 6)).coerceIn(72, 99)
            val defGrade = (overallRating + rng.nextInt(-5, 9)).coerceIn(68, 96)
            val iqGrade = (overallRating + rng.nextInt(-3, 10)).coerceIn(70, 97)

            val games = when (sportType) {
                SportType.BASKETBALL -> 30 + rng.nextInt(9)
                SportType.FOOTBALL -> 11 + rng.nextInt(4)
                SportType.SOCCER -> 26 + rng.nextInt(10)
                SportType.HOCKEY -> 32 + rng.nextInt(8)
            }

            val (pVal, sVal, tVal, eff) = when (sportType) {
                SportType.BASKETBALL -> {
                    val basePpg = 13.5 + ((collegeScore - 70) * 0.44) + (if (pos in listOf("PG", "SG", "SF")) 1.8 else -0.5)
                    val baseRpg = if (pos in listOf("PF", "C")) 8.2 + (collegeScore - 74) * 0.15 else 4.0 + (collegeScore - 74) * 0.08
                    val baseApg = if (pos == "PG") 6.0 + (collegeScore - 74) * 0.14 else 2.4 + (collegeScore - 74) * 0.06
                    val effVal = 50.5 + (collegeScore - 70) * 0.46
                    listOf(
                        (basePpg * 10.0).roundToInt() / 10.0,
                        (baseRpg * 10.0).roundToInt() / 10.0,
                        (baseApg * 10.0).roundToInt() / 10.0,
                        (effVal * 10.0).roundToInt() / 10.0
                    )
                }
                SportType.FOOTBALL -> {
                    val baseYpg = if (pos == "QB") 260.0 + (collegeScore - 70) * 4.4 else 80.0 + (collegeScore - 70) * 2.2
                    val baseTd = if (pos == "QB") 2.5 + (collegeScore - 74) * 0.06 else 0.8 + (collegeScore - 74) * 0.04
                    val baseTkl = if (pos in listOf("EDGE", "CB")) 5.6 + (collegeScore - 74) * 0.13 else 86.0 + (collegeScore - 74) * 0.55
                    val effVal = 57.0 + (collegeScore - 70) * 0.52
                    listOf(
                        (baseYpg * 10.0).roundToInt() / 10.0,
                        (baseTd * 10.0).roundToInt() / 10.0,
                        (baseTkl * 10.0).roundToInt() / 10.0,
                        (effVal * 10.0).roundToInt() / 10.0
                    )
                }
                SportType.SOCCER -> {
                    val baseGa = 13.0 + (collegeScore - 70) * 0.78
                    val baseSot = 2.0 + (collegeScore - 70) * 0.09
                    val baseKp = 1.7 + (collegeScore - 70) * 0.09
                    val effVal = 53.0 + (collegeScore - 70) * 0.58
                    listOf(
                        (baseGa * 10.0).roundToInt() / 10.0,
                        (baseSot * 10.0).roundToInt() / 10.0,
                        (baseKp * 10.0).roundToInt() / 10.0,
                        (effVal * 10.0).roundToInt() / 10.0
                    )
                }
                SportType.HOCKEY -> {
                    val basePts = 0.90 + (collegeScore - 70) * 0.042
                    val baseSog = 3.0 + (collegeScore - 70) * 0.09
                    val basePm = 9.0 + (collegeScore - 70) * 0.82
                    val effVal = 54.0 + (collegeScore - 70) * 0.5
                    listOf(
                        (basePts * 10.0).roundToInt() / 10.0,
                        (baseSog * 10.0).roundToInt() / 10.0,
                        (basePm * 10.0).roundToInt() / 10.0,
                        (effVal * 10.0).roundToInt() / 10.0
                    )
                }
            }

            val s1 = strengthTags[idx % strengthTags.size]
            val s2 = strengthTags[(idx + 3 + rng.nextInt(3)) % strengthTags.size]
            val s3 = strengthTags[(idx + 6 + rng.nextInt(3)) % strengthTags.size]
            val risk = riskNotes[rng.nextInt(riskNotes.size)]
            val comp = proComps[rng.nextInt(proComps.size)]
            val accolade = accoladesList[rng.nextInt(accoladesList.size)]

            val colMetrics = CollegiateMetrics(
                schoolName = school,
                conferenceName = conf,
                classYear = cYear,
                gamesPlayed = games,
                primaryStatLabel = pLabel,
                primaryStatValue = pVal,
                secondaryStatLabel = sLabel,
                secondaryStatValue = sVal,
                tertiaryStatLabel = tLabel,
                tertiaryStatValue = tVal,
                efficiencyPct = eff,
                collegiateMetricScore = collegeScore,
                accolades = accolade
            )

            val upsideDelta = potentialRating - overallRating
            val execSummary = "$cYear $pos out of $school ($conf). Rated $overallRating OVR with $potentialRating POT (+$upsideDelta upside, $potentialGrade). Scout Grade $scoutBase • ${colMetrics.formattedStatLine}."

            val report = ScoutingReport(
                scoutGrade = scoutBase,
                potentialGrade = potentialGrade,
                projectedOverall = overallRating,
                combineAthleticScore = combineScore,
                defensiveGrade = defGrade,
                iqAndIntangibles = iqGrade,
                strengths = listOf(s1, s2, s3).distinct(),
                weaknessNote = risk,
                proComparison = comp,
                executiveSummary = execSummary
            )

            draftEntries.add(
                RookieProspect(
                    id = "prospect_${year}_${seed?.let { "${kotlin.math.abs(it % 10000)}_" } ?: ""}${idx + 1}",
                    name = fullName,
                    age = age,
                    position = pos,
                    archetype = archetype,
                    consensusRank = idx + 1,
                    collegiateMetrics = colMetrics,
                    scoutingReport = report,
                    overallRating = overallRating,
                    potentialRating = potentialRating
                )
            )
        }

        // Rank procedurally by weighted OVR + Potential so consensus order reflects ratings & upside
        return draftEntries
            .sortedByDescending { (it.overallRating * 0.55) + (it.potentialRating * 0.45) }
            .mapIndexed { rankIdx, p ->
                p.copy(consensusRank = rankIdx + 1)
            }
    }
}
