package com.example.data

import androidx.room.*
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

@Database(entities = [LeagueSaveEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leagueDao(): LeagueDao

    companion object {
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
                    ).build()
                    cachedContext = appCtx
                    instance = created
                    created
                }
            }
        }
    }
}
