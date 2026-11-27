package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FinancialRecord
import com.example.data.FinancialRecordEntity
import com.example.data.FranchiseEntity
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.data.LeagueSettings
import com.example.data.LeagueSettingsEntity
import com.example.model.CommissionerBackground
import com.example.model.FinancialSnapshot
import com.example.model.LaborUnionAgreement
import com.example.model.LeagueRuleBook
import com.example.model.SeasonPhase
import com.example.model.SportType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomPersistenceMigrationAndDaoTest {

    @Test
    fun appDatabaseConcreteInstanceInitializesSingletonAtVersion5() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = AppDatabase.getInstance(context)
        val second = AppDatabase.getInstance(context)

        assertSame("AppDatabase.getInstance must return concrete singleton instance", first, second)
        assertEquals(6, first.openHelper.writableDatabase.version)
        assertNotNull(first.franchiseDao())
        assertNotNull(first.financialRecordDao())
        assertNotNull(first.leagueSettingsDao())
    }

    @Test
    fun migrationStrategyUpgradesV1DatabaseToV5PreservingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_apex_v1_to_v5.db"
        context.deleteDatabase(dbName)

        // Create a real SQLite DB at schema version 1 with league_saves table and a saved row
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `league_saves` (
                            `id` INTEGER NOT NULL,
                            `leagueName` TEXT NOT NULL,
                            `sportType` TEXT NOT NULL,
                            `commissionerBackground` TEXT NOT NULL,
                            `currentYear` INTEGER NOT NULL,
                            `currentWeek` INTEGER NOT NULL,
                            `seasonPhase` TEXT NOT NULL,
                            `cashReservesMillions` INTEGER NOT NULL,
                            `leagueValuationMillions` INTEGER NOT NULL,
                            `leagueIntegrityScore` INTEGER NOT NULL,
                            `competitiveParityScore` INTEGER NOT NULL,
                            `unionSatisfaction` INTEGER NOT NULL,
                            `strikeRisk` INTEGER NOT NULL,
                            `playerRevenueSharePct` INTEGER NOT NULL,
                            `salaryCapType` TEXT NOT NULL,
                            `salaryCapMillions` INTEGER NOT NULL,
                            `activeBroadcastName` TEXT NOT NULL,
                            `activeBroadcastPlatform` TEXT NOT NULL,
                            `activeBroadcastPayoutMillions` INTEGER NOT NULL,
                            `activeBroadcastYearsRemaining` INTEGER NOT NULL,
                            `franchisesJson` TEXT NOT NULL,
                            `citiesJson` TEXT NOT NULL,
                            `campaignsJson` TEXT NOT NULL,
                            `historyJson` TEXT NOT NULL,
                            `newsFeedJson` TEXT NOT NULL,
                            `updatedTimestamp` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `league_saves` VALUES (
                            1, 'Legacy Apex League', 'BASKETBALL', 'MARKETING_MAVEN',
                            2026, 8, 'REGULAR_SEASON', 85, 620, 88, 81, 76, 14, 50,
                            'SOFT_CAP_LUXURY_TAX', 120, 'Apex Network', 'HYBRID_SYNDICATE', 110, 4,
                            '[]', '[]', '[]', '[]', '[]', 1700000000000
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            assertEquals(6, migratedDb.openHelper.writableDatabase.version)
            val legacySave = migratedDb.leagueDao().getLeagueSave()
            assertNotNull("v1 league save row must survive 1->5 migration", legacySave)
            assertEquals("Legacy Apex League", legacySave!!.leagueName)

            // Verify newly migrated v5 tables accept CRUD immediately
            migratedDb.leagueSettingsDao().insertOrUpdateLeagueSettings(
                LeagueSettings(
                    leagueName = "Legacy Apex League",
                    sportType = "BASKETBALL",
                    commissionerBackground = "MARKETING_MAVEN",
                    salaryCapType = "SOFT_CAP_LUXURY_TAX",
                    salaryCapStrictness = "PROGRESSIVE_LUXURY_TAX",
                    salaryCapMillions = 120L,
                    overtimeFormat = "STANDARD_PERIOD",
                    rosterSizeRule = "STANDARD_15",
                    injuryFrequency = "STANDARD_REALISTIC",
                    gameLength = "STANDARD_48",
                    antiDopingPolicy = "BALANCED",
                    antiTamperingGamblingPolicy = "ZERO_TOLERANCE",
                    draftParity = "WEIGHTED_LOTTERY",
                    instantReplayEnabled = true,
                    shotClockFastPace = true,
                    regularSeasonWeeks = 14,
                    tradeDeadlineWeek = 9,
                    ruleBookJson = "{}"
                )
            )
            assertNotNull(migratedDb.leagueSettingsDao().getLeagueSettings())
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun franchiseFinancialRecordAndLeagueSettingsDaosSupportFullCrudAndFlow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val franchiseDao = db.franchiseDao()
            val financialDao = db.financialRecordDao()
            val settingsDao = db.leagueSettingsDao()

            // 1. FranchiseDao CRUD
            val f1 = FranchiseEntity(
                franchiseId = "f_ny_empire",
                cityId = "city_ny",
                cityName = "New York",
                nickname = "Empire",
                teamFullName = "New York Empire",
                conferenceName = "Eastern Conference",
                divisionName = "Atlantic",
                colorHex = 0xFF1D4ED8,
                secondaryColorHex = 0xFFF59E0B,
                rosterRating = 86,
                fanBaseMillions = 3.2,
                fanMorale = 84,
                fanLoyalty = 88,
                teamMorale = 85,
                reputationScore = 82,
                wins = 11,
                losses = 3,
                pointsFor = 1520,
                pointsAgainst = 1360,
                championships = 2,
                annualPayrollMillions = 112L,
                localSponsorRevenueMillions = 42L,
                ticketPrice = 85,
                stadiumCapacity = 21500,
                stadiumAge = 8,
                stadiumCondition = 92,
                stadiumSponsorName = "Meridian Arena",
                stadiumSponsorTier = 2,
                stadiumSponsorYearsLeft = 4,
                stadiumNamingRightsRevenue = 15L,
                relocationCount = 0,
                lowSpendingSeasons = 0,
                payloadJson = "[]"
            )
            franchiseDao.insertFranchise(f1)
            assertEquals("New York Empire", franchiseDao.getFranchiseById("f_ny_empire")?.teamFullName)
            assertEquals(1, franchiseDao.getAllFranchisesFlow().first().size)

            franchiseDao.updateFranchise(f1.copy(wins = 12, reputationScore = 89))
            assertEquals(12, franchiseDao.getFranchiseById("f_ny_empire")?.wins)
            assertEquals(89, franchiseDao.getFranchiseById("f_ny_empire")?.reputationScore)

            franchiseDao.deleteFranchiseById("f_ny_empire")
            assertNull(franchiseDao.getFranchiseById("f_ny_empire"))

            // 2. FinancialRecordDao CRUD
            val recId = financialDao.insertFinancialRecord(
                FinancialRecord(
                    seasonYear = 2027,
                    weekNumber = 5,
                    periodLabel = "W5",
                    recordType = "WEEKLY_SNAPSHOT",
                    broadcastRevenueMillions = 12.5f,
                    sponsorshipRevenueMillions = 7.2f,
                    leagueSponsorsRevenueMillions = 3.0f,
                    namingRightsRevenueMillions = 1.8f,
                    expansionFeeRevenueMillions = 0f,
                    luxuryTaxRevenueMillions = 2.5f,
                    totalRevenueMillions = 27.0f,
                    operationsExpenseMillions = 7.0f,
                    marketingExpenseMillions = 1.8f,
                    welfareAndGrantsExpenseMillions = 1.2f,
                    totalExpensesMillions = 10.0f,
                    netCashFlowMillions = 17.0f,
                    cashReservesMillions = 92.0f,
                    leagueValuationMillions = 680.0f
                )
            )
            assertTrue(recId > 0L)
            val fetchedRec = financialDao.getFinancialRecordById(recId)
            assertNotNull(fetchedRec)
            assertEquals(27.0f, fetchedRec!!.totalRevenueMillions, 0.01f)
            assertEquals(1, financialDao.getFinancialRecordsBySeasonFlow(2027).first().size)

            financialDao.updateFinancialRecord(fetchedRec.copy(cashReservesMillions = 108.5f, netCashFlowMillions = 21.0f))
            assertEquals(108.5f, financialDao.getFinancialRecordById(recId)!!.cashReservesMillions, 0.01f)

            financialDao.deleteFinancialRecordById(recId)
            assertNull(financialDao.getFinancialRecordById(recId))

            // 3. LeagueSettingsDao CRUD
            val initialSettings = LeagueSettingsEntity(
                id = 1,
                leagueName = "Apex Championship",
                sportType = "BASKETBALL",
                commissionerBackground = "PLAYER_FAVORITE",
                salaryCapType = "SOFT_CAP_LUXURY_TAX",
                salaryCapStrictness = "PROGRESSIVE_LUXURY_TAX",
                salaryCapMillions = 128L,
                overtimeFormat = "ELAM_ENDING",
                rosterSizeRule = "STANDARD_15",
                injuryFrequency = "STANDARD_REALISTIC",
                gameLength = "STANDARD_48",
                antiDopingPolicy = "BALANCED",
                antiTamperingGamblingPolicy = "ZERO_TOLERANCE",
                draftParity = "WEIGHTED_LOTTERY",
                instantReplayEnabled = true,
                shotClockFastPace = true,
                regularSeasonWeeks = 16,
                tradeDeadlineWeek = 10,
                ruleBookJson = "{\"overtimeFormat\":\"ELAM_ENDING\"}"
            )
            settingsDao.insertOrUpdateLeagueSettings(initialSettings)
            val readSettings = settingsDao.getLeagueSettingsFlow().first()
            assertNotNull(readSettings)
            assertEquals("ELAM_ENDING", readSettings!!.overtimeFormat)
            assertEquals(128L, readSettings.salaryCapMillions)

            settingsDao.updateLeagueSettings(readSettings.copy(salaryCapMillions = 142L, instantReplayEnabled = false))
            val updatedSettings = settingsDao.getLeagueSettings()
            assertEquals(142L, updatedSettings!!.salaryCapMillions)
            assertEquals(false, updatedSettings.instantReplayEnabled)

            settingsDao.deleteLeagueSettings(updatedSettings)
            assertNull(settingsDao.getLeagueSettings())
        } finally {
            db.close()
        }
    }

    @Test
    fun repositorySaveAndResetPersistAndClearFranchisesFinancialRecordsAndSettings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LeagueRepository(context)
        repo.resetLeague()

        val franchises = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val broadcast = InitialLeagueData.getInitialBroadcastDeals().first()
        val union = LaborUnionAgreement(salaryCapMillions = 125L)
        val ruleBook = LeagueRuleBook(
            rules = InitialLeagueData.getDefaultLeagueRules(),
            regularSeasonWeeks = 16
        )
        val snapshots = listOf(
            FinancialSnapshot(
                periodLabel = "W1",
                weekNumber = 1,
                broadcastRevenue = 10f,
                sponsorshipRevenue = 5f,
                expansionFeeRevenue = 0f,
                luxuryTaxRevenue = 1f,
                totalRevenue = 16f,
                operationsExpense = 5f,
                marketingExpense = 1f,
                welfareAndGrantsExpense = 1f,
                totalExpenses = 7f,
                netCashFlow = 9f,
                cashReserves = 59f,
                valuation = 520f
            ),
            FinancialSnapshot(
                periodLabel = "W2",
                weekNumber = 2,
                broadcastRevenue = 11f,
                sponsorshipRevenue = 6f,
                expansionFeeRevenue = 0f,
                luxuryTaxRevenue = 2f,
                totalRevenue = 19f,
                operationsExpense = 5f,
                marketingExpense = 1f,
                welfareAndGrantsExpense = 1f,
                totalExpenses = 7f,
                netCashFlow = 12f,
                cashReserves = 71f,
                valuation = 540f
            )
        )

        repo.saveLeague(
            leagueName = "Apex Persistence Test",
            sportType = SportType.BASKETBALL,
            commissionerBackground = CommissionerBackground.MARKETING_MAVEN,
            currentYear = 2027,
            currentWeek = 2,
            seasonPhase = SeasonPhase.REGULAR_SEASON,
            cashReservesMillions = 71L,
            leagueValuationMillions = 540L,
            leagueIntegrityScore = 88,
            competitiveParityScore = 84,
            unionAgreement = union,
            activeBroadcast = broadcast,
            franchises = franchises,
            cities = InitialLeagueData.getDefaultCities(),
            campaigns = InitialLeagueData.getMarketingCampaignCatalog(),
            history = emptyList(),
            newsFeed = listOf("League persisted"),
            ruleBook = ruleBook,
            financialSnapshots = snapshots
        )

        val persistedFranchises = repo.loadFranchiseEntities()
        val persistedRecords: List<FinancialRecordEntity> = repo.loadAllFinancialRecords()
        val persistedSettings = repo.loadLeagueSettingsFromRoom()

        assertEquals(franchises.size, persistedFranchises.size)
        assertEquals(2, persistedRecords.size)
        assertEquals("W2", persistedRecords.last().periodLabel)
        assertNotNull(persistedSettings)
        assertEquals("Apex Persistence Test", persistedSettings!!.leagueName)
        assertEquals(16, persistedSettings.regularSeasonWeeks)

        repo.resetLeague()
        assertTrue(repo.loadFranchiseEntities().isEmpty())
        assertTrue(repo.loadAllFinancialRecords().isEmpty())
        assertNull(repo.loadLeagueSettingsFromRoom())
    }
}
