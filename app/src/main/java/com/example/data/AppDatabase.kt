package com.example.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

typealias FinancialRecord = FinancialRecordEntity
typealias LeagueSettings = LeagueSettingsEntity

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

@Entity(tableName = "franchises")
data class FranchiseEntity(
    @PrimaryKey val franchiseId: String,
    val cityId: String,
    val cityName: String,
    val nickname: String,
    val teamFullName: String,
    val conferenceName: String,
    val divisionName: String,
    val colorHex: Long,
    val secondaryColorHex: Long,
    val rosterRating: Int,
    val fanBaseMillions: Double,
    val fanMorale: Int,
    val fanLoyalty: Int,
    val teamMorale: Int,
    val reputationScore: Int,
    val wins: Int,
    val losses: Int,
    val pointsFor: Int,
    val pointsAgainst: Int,
    val championships: Int,
    val annualPayrollMillions: Long,
    val localSponsorRevenueMillions: Long,
    val ticketPrice: Int,
    val stadiumCapacity: Int,
    val stadiumAge: Int,
    val stadiumCondition: Int,
    val stadiumSponsorName: String?,
    val stadiumSponsorTier: Int,
    val stadiumSponsorYearsLeft: Int,
    val stadiumNamingRightsRevenue: Long,
    val initialValuationMillions: Long = 0L,
    val relocationCount: Int,
    val lowSpendingSeasons: Int,
    val payloadJson: String,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "financial_records",
    indices = [Index(value = ["seasonYear", "weekNumber"], unique = false)]
)
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val seasonYear: Int,
    val weekNumber: Int,
    val periodLabel: String,
    val recordType: String = "WEEKLY_SNAPSHOT",
    val broadcastRevenueMillions: Float,
    val sponsorshipRevenueMillions: Float,
    val leagueSponsorsRevenueMillions: Float = 0f,
    val namingRightsRevenueMillions: Float = 0f,
    val expansionFeeRevenueMillions: Float,
    val luxuryTaxRevenueMillions: Float,
    val totalRevenueMillions: Float,
    val operationsExpenseMillions: Float,
    val marketingExpenseMillions: Float,
    val welfareAndGrantsExpenseMillions: Float,
    val totalExpensesMillions: Float,
    val netCashFlowMillions: Float,
    val cashReservesMillions: Float,
    val leagueValuationMillions: Float,
    val recordedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "league_settings")
data class LeagueSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val leagueName: String,
    val sportType: String,
    val commissionerBackground: String,
    val salaryCapType: String,
    val salaryCapStrictness: String,
    val salaryCapMillions: Long,
    val overtimeFormat: String,
    val rosterSizeRule: String,
    val injuryFrequency: String,
    val gameLength: String,
    val antiDopingPolicy: String,
    val antiTamperingGamblingPolicy: String,
    val draftParity: String,
    val instantReplayEnabled: Boolean,
    val shotClockFastPace: Boolean,
    val regularSeasonWeeks: Int,
    val tradeDeadlineWeek: Int,
    val ruleBookJson: String,
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

@Dao
interface FranchiseDao {
    @Query("SELECT * FROM franchises ORDER BY wins DESC, teamFullName ASC")
    fun getAllFranchisesFlow(): Flow<List<FranchiseEntity>>

    @Query("SELECT * FROM franchises WHERE conferenceName = :conferenceName ORDER BY wins DESC, teamFullName ASC")
    fun getFranchisesByConferenceFlow(conferenceName: String): Flow<List<FranchiseEntity>>

    @Query("SELECT * FROM franchises ORDER BY wins DESC, teamFullName ASC")
    suspend fun getAllFranchises(): List<FranchiseEntity>

    @Query("SELECT * FROM franchises WHERE franchiseId = :franchiseId LIMIT 1")
    suspend fun getFranchiseById(franchiseId: String): FranchiseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFranchise(franchise: FranchiseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFranchises(franchises: List<FranchiseEntity>)

    @Update
    suspend fun updateFranchise(franchise: FranchiseEntity)

    @Delete
    suspend fun deleteFranchise(franchise: FranchiseEntity)

    @Query("DELETE FROM franchises WHERE franchiseId = :franchiseId")
    suspend fun deleteFranchiseById(franchiseId: String)

    @Query("DELETE FROM franchises")
    suspend fun clearFranchises()

    @Transaction
    suspend fun replaceAllFranchises(franchises: List<FranchiseEntity>) {
        clearFranchises()
        insertAllFranchises(franchises)
    }
}

@Dao
interface FinancialRecordDao {
    @Query("SELECT * FROM financial_records ORDER BY seasonYear DESC, weekNumber DESC, id DESC")
    fun getAllFinancialRecordsFlow(): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financial_records WHERE seasonYear = :seasonYear ORDER BY weekNumber ASC, id ASC")
    fun getFinancialRecordsBySeasonFlow(seasonYear: Int): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financial_records ORDER BY seasonYear ASC, weekNumber ASC, id ASC")
    suspend fun getAllFinancialRecords(): List<FinancialRecordEntity>

    @Query("SELECT * FROM financial_records WHERE id = :id LIMIT 1")
    suspend fun getFinancialRecordById(id: Long): FinancialRecordEntity?

    @Query("SELECT * FROM financial_records WHERE seasonYear = :seasonYear ORDER BY weekNumber ASC, id ASC")
    suspend fun getFinancialRecordsBySeason(seasonYear: Int): List<FinancialRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(record: FinancialRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFinancialRecords(records: List<FinancialRecordEntity>)

    @Update
    suspend fun updateFinancialRecord(record: FinancialRecordEntity)

    @Delete
    suspend fun deleteFinancialRecord(record: FinancialRecordEntity)

    @Query("DELETE FROM financial_records WHERE id = :id")
    suspend fun deleteFinancialRecordById(id: Long)

    @Query("DELETE FROM financial_records WHERE seasonYear = :seasonYear")
    suspend fun deleteFinancialRecordsForSeason(seasonYear: Int)

    @Query("DELETE FROM financial_records")
    suspend fun clearFinancialRecords()

    @Transaction
    suspend fun replaceSeasonFinancialRecords(seasonYear: Int, records: List<FinancialRecordEntity>) {
        deleteFinancialRecordsForSeason(seasonYear)
        insertAllFinancialRecords(records)
    }
}

@Dao
interface LeagueSettingsDao {
    @Query("SELECT * FROM league_settings WHERE id = 1 LIMIT 1")
    fun getLeagueSettingsFlow(): Flow<LeagueSettingsEntity?>

    @Query("SELECT * FROM league_settings WHERE id = 1 LIMIT 1")
    suspend fun getLeagueSettings(): LeagueSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLeagueSettings(settings: LeagueSettingsEntity)

    @Update
    suspend fun updateLeagueSettings(settings: LeagueSettingsEntity)

    @Delete
    suspend fun deleteLeagueSettings(settings: LeagueSettingsEntity)

    @Query("DELETE FROM league_settings WHERE id = 1")
    suspend fun deleteLeagueSettingsByDefaultId()
}

@Database(
    entities = [
        LeagueSaveEntity::class,
        LeagueState::class,
        LeagueRevenueHistoryEntity::class,
        LeagueStandingEntity::class,
        FranchiseEntity::class,
        FinancialRecordEntity::class,
        LeagueSettingsEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leagueDao(): LeagueDao
    abstract fun leagueStateDao(): LeagueStateDao
    abstract fun leagueRevenueHistoryDao(): LeagueRevenueHistoryDao
    abstract fun leagueStandingDao(): LeagueStandingDao
    abstract fun franchiseDao(): FranchiseDao
    abstract fun financialRecordDao(): FinancialRecordDao
    abstract fun leagueSettingsDao(): LeagueSettingsDao

    companion object {
        const val DATABASE_NAME = "apex_league_database.db"

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `franchises` (
                        `franchiseId` TEXT NOT NULL,
                        `cityId` TEXT NOT NULL,
                        `cityName` TEXT NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `teamFullName` TEXT NOT NULL,
                        `conferenceName` TEXT NOT NULL,
                        `divisionName` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `secondaryColorHex` INTEGER NOT NULL,
                        `rosterRating` INTEGER NOT NULL,
                        `fanBaseMillions` REAL NOT NULL,
                        `fanMorale` INTEGER NOT NULL,
                        `fanLoyalty` INTEGER NOT NULL,
                        `teamMorale` INTEGER NOT NULL,
                        `reputationScore` INTEGER NOT NULL,
                        `wins` INTEGER NOT NULL,
                        `losses` INTEGER NOT NULL,
                        `pointsFor` INTEGER NOT NULL,
                        `pointsAgainst` INTEGER NOT NULL,
                        `championships` INTEGER NOT NULL,
                        `annualPayrollMillions` INTEGER NOT NULL,
                        `localSponsorRevenueMillions` INTEGER NOT NULL,
                        `ticketPrice` INTEGER NOT NULL,
                        `stadiumCapacity` INTEGER NOT NULL,
                        `stadiumAge` INTEGER NOT NULL,
                        `stadiumCondition` INTEGER NOT NULL,
                        `stadiumSponsorName` TEXT,
                        `stadiumSponsorTier` INTEGER NOT NULL,
                        `stadiumSponsorYearsLeft` INTEGER NOT NULL,
                        `stadiumNamingRightsRevenue` INTEGER NOT NULL,
                        `relocationCount` INTEGER NOT NULL,
                        `lowSpendingSeasons` INTEGER NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`franchiseId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `seasonYear` INTEGER NOT NULL,
                        `weekNumber` INTEGER NOT NULL,
                        `periodLabel` TEXT NOT NULL,
                        `recordType` TEXT NOT NULL,
                        `broadcastRevenueMillions` REAL NOT NULL,
                        `sponsorshipRevenueMillions` REAL NOT NULL,
                        `leagueSponsorsRevenueMillions` REAL NOT NULL,
                        `namingRightsRevenueMillions` REAL NOT NULL,
                        `expansionFeeRevenueMillions` REAL NOT NULL,
                        `luxuryTaxRevenueMillions` REAL NOT NULL,
                        `totalRevenueMillions` REAL NOT NULL,
                        `operationsExpenseMillions` REAL NOT NULL,
                        `marketingExpenseMillions` REAL NOT NULL,
                        `welfareAndGrantsExpenseMillions` REAL NOT NULL,
                        `totalExpensesMillions` REAL NOT NULL,
                        `netCashFlowMillions` REAL NOT NULL,
                        `cashReservesMillions` REAL NOT NULL,
                        `leagueValuationMillions` REAL NOT NULL,
                        `recordedTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_financial_records_seasonYear_weekNumber` ON `financial_records` (`seasonYear`, `weekNumber`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `league_settings` (
                        `id` INTEGER NOT NULL,
                        `leagueName` TEXT NOT NULL,
                        `sportType` TEXT NOT NULL,
                        `commissionerBackground` TEXT NOT NULL,
                        `salaryCapType` TEXT NOT NULL,
                        `salaryCapStrictness` TEXT NOT NULL,
                        `salaryCapMillions` INTEGER NOT NULL,
                        `overtimeFormat` TEXT NOT NULL,
                        `rosterSizeRule` TEXT NOT NULL,
                        `injuryFrequency` TEXT NOT NULL,
                        `gameLength` TEXT NOT NULL,
                        `antiDopingPolicy` TEXT NOT NULL,
                        `antiTamperingGamblingPolicy` TEXT NOT NULL,
                        `draftParity` TEXT NOT NULL,
                        `instantReplayEnabled` INTEGER NOT NULL,
                        `shotClockFastPace` INTEGER NOT NULL,
                        `regularSeasonWeeks` INTEGER NOT NULL,
                        `tradeDeadlineWeek` INTEGER NOT NULL,
                        `ruleBookJson` TEXT NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `franchises` ADD COLUMN `initialValuationMillions` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

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
                        DATABASE_NAME
                    )
                        .addMigrations(*ALL_MIGRATIONS)
                        .build()
                    cachedContext = appCtx
                    instance = created
                    created
                }
            }
        }
    }
}
