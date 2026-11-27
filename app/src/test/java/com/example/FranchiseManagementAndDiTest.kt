package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DatabaseModule
import com.example.data.DatabaseProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FranchiseManagementAndDiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        DatabaseModule.resetForTesting()
    }

    @Test
    fun databaseModuleProvidesConcreteRoomInstanceDaosRepositoryAndViewModelFactory() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        DatabaseModule.resetForTesting()

        val db1 = DatabaseModule.provideDatabase(app)
        val db2 = DatabaseProvider.provideDatabase(app)
        assertSame(db1, db2)
        assertEquals(6, db1.openHelper.writableDatabase.version)

        val container = DatabaseModule.provideAppContainer(app)
        assertSame(db1, container.database)
        assertNotNull(container.franchiseDao)
        assertNotNull(container.financialRecordDao)
        assertNotNull(container.leagueSettingsDao)

        val factory = DatabaseModule.provideLeagueViewModelFactory(app)
        val vm = factory.create(LeagueViewModel::class.java)
        assertNotNull(vm)
        assertTrue(vm.uiState.value.franchises.isNotEmpty())
    }

    @Test
    fun franchiseManagementModalValidatesAndPersistsNewFranchiseToRoom() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseModule.setTestDatabase(inMemoryDb)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DatabaseModule.provideLeagueViewModelFactory(app).create(LeagueViewModel::class.java)
        val initialCount = vm.uiState.value.franchises.size
        val initialLeagueValuation = vm.uiState.value.leagueValuationMillions

        composeTestRule.setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = vm)
            }
        }

        // Open Teams tab and click Add Franchise
        composeTestRule.onNodeWithTag("nav_tab_teams").performClick()
        composeTestRule.onNodeWithTag("btn_open_franchise_management").performClick()

        // Verify modal fields are rendered
        composeTestRule.onNodeWithTag("franchise_management_modal_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_franchise_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_franchise_city").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_franchise_initial_valuation").assertIsDisplayed()

        // Submit while name/city empty -> shows validation error, modal remains open
        composeTestRule.onNodeWithTag("btn_submit_add_franchise").performClick()
        composeTestRule.onNodeWithTag("franchise_management_error").assertIsDisplayed()

        // Enter Franchise Name, City, and Initial Valuation ($385M via preset/clear+input)
        composeTestRule.onNodeWithTag("input_franchise_name").performTextInput("Outlaws")
        composeTestRule.onNodeWithTag("input_franchise_city").performTextInput("Austin")
        composeTestRule.onNodeWithTag("chip_valuation_preset_350").performClick()
        composeTestRule.onNodeWithText("Austin Outlaws").assertIsDisplayed()

        // Submit
        composeTestRule.onNodeWithTag("btn_submit_add_franchise").performClick()
        composeTestRule.waitForIdle()

        // Modal closes & state updates
        assertFalse(vm.uiState.value.showFranchiseManagementModal)
        assertEquals(initialCount + 1, vm.uiState.value.franchises.size)
        assertEquals(initialLeagueValuation + 350L, vm.uiState.value.leagueValuationMillions)

        val added = vm.uiState.value.franchises.find { it.cityName == "Austin" && it.nickname == "Outlaws" }
        assertNotNull("Austin Outlaws must be added to league franchises", added)
        assertEquals(350L, added!!.initialValuationMillions)
        assertEquals(
            350L,
            added.calculateValuationBreakdown(
                vm.uiState.value.activeBroadcast,
                vm.uiState.value.franchises.size
            ).totalValuationMillions
        )

        // Verify record is persisted directly in Room FranchiseDao
        val persistedRows = inMemoryDb.franchiseDao().getAllFranchises()
        val persistedRow = persistedRows.find { it.franchiseId == added.id }
        assertNotNull("New franchise record must exist in Room franchises table", persistedRow)
        assertEquals("Austin Outlaws", persistedRow!!.teamFullName)
        assertEquals("Austin", persistedRow.cityName)
        assertEquals(350L, persistedRow.initialValuationMillions)

        inMemoryDb.close()
    }
}
