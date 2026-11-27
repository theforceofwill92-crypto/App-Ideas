package com.example.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.viewmodel.LeagueViewModel

typealias DatabaseProvider = DatabaseModule

/**
 * Dependency Injection container exposing the Room persistence layer and repositories.
 */
interface LeagueAppContainer {
    val database: AppDatabase
    val leagueDao: LeagueDao
    val leagueStateDao: LeagueStateDao
    val leagueRevenueHistoryDao: LeagueRevenueHistoryDao
    val leagueStandingDao: LeagueStandingDao
    val franchiseDao: FranchiseDao
    val financialRecordDao: FinancialRecordDao
    val leagueSettingsDao: LeagueSettingsDao
    val leagueRepository: LeagueRepository
    val leagueViewModelFactory: ViewModelProvider.Factory
}

class DefaultLeagueAppContainer(
    private val context: Context
) : LeagueAppContainer {

    private val appContext: Context = context.applicationContext ?: context

    override val database: AppDatabase
        get() = DatabaseModule.provideDatabase(appContext)

    override val leagueDao: LeagueDao
        get() = DatabaseModule.provideLeagueDao(database)

    override val leagueStateDao: LeagueStateDao
        get() = DatabaseModule.provideLeagueStateDao(database)

    override val leagueRevenueHistoryDao: LeagueRevenueHistoryDao
        get() = DatabaseModule.provideLeagueRevenueHistoryDao(database)

    override val leagueStandingDao: LeagueStandingDao
        get() = DatabaseModule.provideLeagueStandingDao(database)

    override val franchiseDao: FranchiseDao
        get() = DatabaseModule.provideFranchiseDao(database)

    override val financialRecordDao: FinancialRecordDao
        get() = DatabaseModule.provideFinancialRecordDao(database)

    override val leagueSettingsDao: LeagueSettingsDao
        get() = DatabaseModule.provideLeagueSettingsDao(database)

    override val leagueRepository: LeagueRepository
        get() = DatabaseModule.provideLeagueRepository(appContext)

    override val leagueViewModelFactory: ViewModelProvider.Factory
        get() = DatabaseModule.provideLeagueViewModelFactory(appContext)
}

/**
 * Dependency Injection Module & Provider for initializing [AppDatabase], DAOs,
 * [LeagueRepository], and [LeagueViewModel] factories across the application.
 */
object DatabaseModule {

    @Volatile
    private var overrideDatabaseForTesting: AppDatabase? = null

    @Volatile
    private var repositoryInstance: LeagueRepository? = null

    @Volatile
    private var repositoryContext: Context? = null

    @Volatile
    private var containerInstance: LeagueAppContainer? = null

    fun provideDatabase(context: Context): AppDatabase {
        overrideDatabaseForTesting?.let { return it }
        val appCtx = context.applicationContext ?: context
        return AppDatabase.getInstance(appCtx)
    }

    fun provideAppContainer(context: Context): LeagueAppContainer {
        val appCtx = context.applicationContext ?: context
        val existing = containerInstance
        if (existing != null && repositoryContext === appCtx) {
            return existing
        }
        return synchronized(this) {
            val again = containerInstance
            if (again != null && repositoryContext === appCtx) {
                again
            } else {
                DefaultLeagueAppContainer(appCtx).also {
                    containerInstance = it
                    repositoryContext = appCtx
                }
            }
        }
    }

    fun provideLeagueDao(database: AppDatabase): LeagueDao = database.leagueDao()

    fun provideLeagueDao(context: Context): LeagueDao = provideDatabase(context).leagueDao()

    fun provideLeagueStateDao(database: AppDatabase): LeagueStateDao = database.leagueStateDao()

    fun provideLeagueStateDao(context: Context): LeagueStateDao = provideDatabase(context).leagueStateDao()

    fun provideLeagueRevenueHistoryDao(database: AppDatabase): LeagueRevenueHistoryDao =
        database.leagueRevenueHistoryDao()

    fun provideLeagueRevenueHistoryDao(context: Context): LeagueRevenueHistoryDao =
        provideDatabase(context).leagueRevenueHistoryDao()

    fun provideLeagueStandingDao(database: AppDatabase): LeagueStandingDao =
        database.leagueStandingDao()

    fun provideLeagueStandingDao(context: Context): LeagueStandingDao =
        provideDatabase(context).leagueStandingDao()

    fun provideFranchiseDao(database: AppDatabase): FranchiseDao = database.franchiseDao()

    fun provideFranchiseDao(context: Context): FranchiseDao = provideDatabase(context).franchiseDao()

    fun provideFinancialRecordDao(database: AppDatabase): FinancialRecordDao =
        database.financialRecordDao()

    fun provideFinancialRecordDao(context: Context): FinancialRecordDao =
        provideDatabase(context).financialRecordDao()

    fun provideLeagueSettingsDao(database: AppDatabase): LeagueSettingsDao =
        database.leagueSettingsDao()

    fun provideLeagueSettingsDao(context: Context): LeagueSettingsDao =
        provideDatabase(context).leagueSettingsDao()

    fun provideLeagueRepository(context: Context): LeagueRepository {
        val appCtx = context.applicationContext ?: context
        val db = provideDatabase(appCtx)
        val existing = repositoryInstance
        if (existing != null && repositoryContext === appCtx && overrideDatabaseForTesting == null) {
            return existing
        }
        return synchronized(this) {
            val again = repositoryInstance
            if (again != null && repositoryContext === appCtx && overrideDatabaseForTesting == null) {
                again
            } else {
                LeagueRepository(appCtx, db).also {
                    repositoryContext = appCtx
                    if (overrideDatabaseForTesting == null) {
                        repositoryInstance = it
                    }
                }
            }
        }
    }

    fun provideLeagueViewModelFactory(context: Context): ViewModelProvider.Factory {
        val appCtx = context.applicationContext ?: context
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LeagueViewModel::class.java)) {
                    val app = (appCtx as? Application)
                        ?: throw IllegalStateException("Application context is required to create LeagueViewModel")
                    return LeagueViewModel(
                        application = app,
                        repository = provideLeagueRepository(app)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    fun setTestDatabase(database: AppDatabase?) {
        synchronized(this) {
            overrideDatabaseForTesting = database
            repositoryInstance = null
            containerInstance = null
        }
    }

    fun resetForTesting() {
        synchronized(this) {
            overrideDatabaseForTesting = null
            repositoryInstance = null
            repositoryContext = null
            containerInstance = null
        }
    }
}
