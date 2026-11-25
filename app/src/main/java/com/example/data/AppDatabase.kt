package com.example.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "league_saves")
data class LeagueSaveEntity(
    @PrimaryKey val id: Int = 1,
    val leagueName: String,
    val sportType: String,
    val commissionerBackground: String,
    val currentYear: Int,
    val currentWeek: Int,
    val seasonPhase: String,
    val cashReservesMillions: Long,
    val leagueValuationMillions: Long,
    val leagueIntegrityScore: Int,
    val competitiveParityScore: Int,
    val unionSatisfaction: Int,
    val strikeRisk: Int,
    val playerRevenueSharePct: Int,
    val salaryCapType: String,
    val salaryCapMillions: Long,
    val activeBroadcastName: String,
    val activeBroadcastPlatform: String,
    val activeBroadcastPayoutMillions: Long,
    val activeBroadcastYearsRemaining: Int,
    val franchisesJson: String,
    val citiesJson: String,
    val campaignsJson: String,
    val historyJson: String,
    val newsFeedJson: String,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "league_state")
data class LeagueState(
    @PrimaryKey val id: Int = 1,
    val leagueName: String,
    val currentYear: Int,
    val currentWeek: Int,
    val seasonPhase: String,
    val totalRevenueMillions: Long,
    val cashReservesMillions: Long,
    val leagueValuationMillions: Long,
    val annualBroadcastRevenueMillions: Long,
    val annualSponsorRevenueMillions: Long,
    val annualNamingRightsRevenueMillions: Long,
    val franchiseCount: Int,
    val isLaborUnionActive: Boolean,
    val isLockoutActive: Boolean,
    val laborUnionStatus: String,
    val cbaYearsRemaining: Int,
    val unionSatisfaction: Int,
    val strikeRisk: Int,
    val salaryCapMillions: Long,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "league_revenue_history")
data class LeagueRevenueHistoryEntity(
    @PrimaryKey val seasonYear: Int,
    val totalRevenueMillions: Long,
    val broadcastRevenueMillions: Long,
    val sponsorRevenueMillions: Long,
    val namingRightsRevenueMillions: Long,
    val valuationMillions: Long,
    val franchiseCount: Int,
    val championName: String,
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val tvBroadcastRevenueMillions: Long = 0L,
    val streamingBroadcastRevenueMillions: Long = 0L
)

@Entity(tableName = "league_standings")
data class LeagueStandingEntity(
    @PrimaryKey val franchiseId: String,
    val cityName: String,
    val nickname: String,
    val teamFullName: String,
    val conferenceName: String,
    val divisionName: String,
    val colorHex: Long,
    val secondaryColorHex: Long,
    val wins: Int,
    val losses: Int,
    val pointsFor: Int,
    val pointsAgainst: Int,
    val pointDifferential: Int,
    val winPct: Double,
    val seasonYear: Int,
    val currentWeek: Int,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface LeagueDao {
    @Query("SELECT * FROM league_saves WHERE id = 1 LIMIT 1")
    fun getLeagueSaveFlow(): Flow<LeagueSaveEntity?>

    @Query("SELECT * FROM league_saves WHERE id = 1 LIMIT 1")
    suspend fun getLeagueSave(): LeagueSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLeague(save: LeagueSaveEntity)

    @Query("DELETE FROM league_saves WHERE id = 1")
    suspend fun deleteLeagueSave()
}

@Dao
interface LeagueStateDao {
    @Query("SELECT * FROM league_state WHERE id = 1 LIMIT 1")
    fun getLeagueStateFlow(): Flow<LeagueState?>

    @Query("SELECT * FROM league_state WHERE id = 1 LIMIT 1")
    suspend fun getLeagueState(): LeagueState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeagueState(state: LeagueState)

    @Query("DELETE FROM league_state WHERE id = 1")
    suspend fun deleteLeagueState()
}

@Dao
interface LeagueRevenueHistoryDao {
    @Query("SELECT * FROM league_revenue_history ORDER BY seasonYear DESC LIMIT 5")
    fun getLastFiveYearsRevenueFlow(): Flow<List<LeagueRevenueHistoryEntity>>

    @Query("SELECT * FROM league_revenue_history ORDER BY seasonYear DESC LIMIT 5")
    suspend fun getLastFiveYearsRevenue(): List<LeagueRevenueHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnualRevenue(record: LeagueRevenueHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllAnnualRevenues(records: List<LeagueRevenueHistoryEntity>)

    @Query("DELETE FROM league_revenue_history")
    suspend fun clearRevenueHistory()
}

@Dao
interface LeagueStandingDao {
    @Query("SELECT * FROM league_standings ORDER BY winPct DESC, pointDifferential DESC, wins DESC, teamFullName ASC")
    fun getLiveStandingsFlow(): Flow<List<LeagueStandingEntity>>

    @Query("SELECT * FROM league_standings WHERE conferenceName = :conferenceName ORDER BY winPct DESC, pointDifferential DESC, wins DESC, teamFullName ASC")
    fun getLiveStandingsByConferenceFlow(conferenceName: String): Flow<List<LeagueStandingEntity>>

    @Query("SELECT * FROM league_standings ORDER BY winPct DESC, pointDifferential DESC, wins DESC, teamFullName ASC")
    suspend fun getAllStandings(): List<LeagueStandingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStanding(standing: LeagueStandingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllStandings(standings: List<LeagueStandingEntity>)

    @Query("DELETE FROM league_standings")
    suspend fun clearStandings()

    @Transaction
    suspend fun replaceStandings(standings: List<LeagueStandingEntity>) {
        clearStandings()
        upsertAllStandings(standings)
    }
}

@Database(
    entities = [
        LeagueSaveEntity::class,
        LeagueState::class,
        LeagueRevenueHistoryEntity::class,
        LeagueStandingEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leagueDao(): LeagueDao
    abstract fun leagueStateDao(): LeagueStateDao
    abstract fun leagueRevenueHistoryDao(): LeagueRevenueHistoryDao
    abstract fun leagueStandingDao(): LeagueStandingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `league_state` (
                        `id` INTEGER NOT NULL,
                        `leagueName` TEXT NOT NULL,
                        `currentYear` INTEGER NOT NULL,
                        `currentWeek` INTEGER NOT NULL,
                        `seasonPhase` TEXT NOT NULL,
                        `totalRevenueMillions` INTEGER NOT NULL,
                        `cashReservesMillions` INTEGER NOT NULL,
                        `leagueValuationMillions` INTEGER NOT NULL,
                        `annualBroadcastRevenueMillions` INTEGER NOT NULL,
                        `annualSponsorRevenueMillions` INTEGER NOT NULL,
                        `annualNamingRightsRevenueMillions` INTEGER NOT NULL,
                        `franchiseCount` INTEGER NOT NULL,
                        `isLaborUnionActive` INTEGER NOT NULL,
                        `isLockoutActive` INTEGER NOT NULL,
                        `laborUnionStatus` TEXT NOT NULL,
                        `cbaYearsRemaining` INTEGER NOT NULL,
                        `unionSatisfaction` INTEGER NOT NULL,
                        `strikeRisk` INTEGER NOT NULL,
                        `salaryCapMillions` INTEGER NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `league_revenue_history` (
                        `seasonYear` INTEGER NOT NULL,
                        `totalRevenueMillions` INTEGER NOT NULL,
                        `broadcastRevenueMillions` INTEGER NOT NULL,
                        `sponsorRevenueMillions` INTEGER NOT NULL,
                        `namingRightsRevenueMillions` INTEGER NOT NULL,
                        `valuationMillions` INTEGER NOT NULL,
                        `franchiseCount` INTEGER NOT NULL,
                        `championName` TEXT NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`seasonYear`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `league_revenue_history` ADD COLUMN `tvBroadcastRevenueMillions` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `league_revenue_history` ADD COLUMN `streamingBroadcastRevenueMillions` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `league_standings` (
                        `franchiseId` TEXT NOT NULL,
                        `cityName` TEXT NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `teamFullName` TEXT NOT NULL,
                        `conferenceName` TEXT NOT NULL,
                        `divisionName` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `secondaryColorHex` INTEGER NOT NULL,
                        `wins` INTEGER NOT NULL,
                        `losses` INTEGER NOT NULL,
                        `pointsFor` INTEGER NOT NULL,
                        `pointsAgainst` INTEGER NOT NULL,
                        `pointDifferential` INTEGER NOT NULL,
                        `winPct` REAL NOT NULL,
                        `seasonYear` INTEGER NOT NULL,
                        `currentWeek` INTEGER NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`franchiseId`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var cachedContext: android.content.Context? = null

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            val appCtx = context.applicationContext ?: context
            val existing = instance
            if (existing != null && cachedContext === appCtx) {
                return existing
            }
            return synchronized(this) {
                val again = instance
                if (again != null && cachedContext === appCtx) {
                    again
                } else {
                    val created = Room.databaseBuilder(
                        appCtx,
                        AppDatabase::class.java,
                        "apex_league_database.db"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .build()
                    cachedContext = appCtx
                    instance = created
                    created
                }
            }
        }
    }
}
