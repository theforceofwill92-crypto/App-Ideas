package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialLeagueData
import com.example.data.LeagueRepository
import com.example.model.FranchiseIdentityEngine
import com.example.model.OwnershipType
import com.example.model.SportType
import com.example.ui.screens.FranchisesScreen
import com.example.ui.screens.MarketsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LeagueViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FranchiseIdentityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun brandPaletteHas24UniqueColorsAndDistinctPairs() {
        val palette = FranchiseIdentityEngine.brandColorPalette
        assertEquals(24, palette.size)
        assertEquals(24, palette.map { it.id }.distinct().size)
        assertEquals(24, palette.map { it.name }.distinct().size)
        assertEquals(24, palette.map { it.hex }.distinct().size)

        palette.forEach { option ->
            val defaultSecondary = FranchiseIdentityEngine.defaultSecondaryFor(option.hex)
            assertNotEquals(
                "Default secondary for ${option.name} must be different from primary",
                option.hex,
                defaultSecondary
            )
            val (normP, normS) = FranchiseIdentityEngine.normalizeColorPair(option.hex, option.hex)
            assertEquals(option.hex, normP)
            assertNotEquals(normP, normS)
        }

        val rng = Random(42)
        repeat(30) {
            val (p, s) = FranchiseIdentityEngine.generateRandomColorPair(random = rng)
            assertNotEquals(p, s)
            assertTrue(palette.any { it.hex == p })
            assertTrue(palette.any { it.hex == s })
        }
    }

    @Test
    fun realisticNicknameGeneratorAvoidsExistingAndCurrentNicknames() {
        val existing = InitialLeagueData.getInitialFranchises(SportType.BASKETBALL)
        val existingSet = existing.map { it.nickname.lowercase() }.toSet()
        val londonCity = InitialLeagueData.getDefaultCities().first { it.name == "London" }
        val rng = Random(99)

        val generated = mutableListOf<String>()
        var previous: String? = null
        repeat(20) {
            val next = FranchiseIdentityEngine.generateRandomRealisticNickname(
                city = londonCity,
                existingFranchises = existing,
                sportType = SportType.BASKETBALL,
                excludeNickname = previous,
                random = rng
            )
            assertTrue(next.isNotBlank())
            assertTrue("Should not collide with active franchise nicknames", next.lowercase() !in existingSet)
            if (previous != null) {
                assertNotEquals("Consecutive rolls should vary nickname", previous.lowercase(), next.lowercase())
            }
            generated.add(next)
            previous = next
        }
        assertTrue("Multiple rolls should produce variety", generated.distinct().size >= 5)
    }

    @Test
    fun viewModelAndRepositoryPersistDualColorsAndRebranding() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val targetCity = vm.uiState.value.cities.first { !it.hasFranchise }
        val randomNick = vm.generateRandomFranchiseNickname(targetCity.id)
        assertTrue(randomNick.isNotBlank())

        val colorA = FranchiseIdentityEngine.brandColorPalette[5].hex
        val colorB = FranchiseIdentityEngine.brandColorPalette[18].hex
        vm.expandLeague(
            targetCityId = targetCity.id,
            nickname = randomNick,
            colorHex = colorA,
            ownershipType = OwnershipType.SOVEREIGN_SYNDICATE,
            secondaryColorHex = colorB
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val newFranchise = vm.uiState.value.franchises.first { it.cityId == targetCity.id }
        assertEquals(randomNick, newFranchise.nickname)
        assertEquals(colorA, newFranchise.colorHex)
        assertEquals(colorB, newFranchise.secondaryColorHex)

        // Update existing franchise with two new distinct colors and verify save/reload round-trip
        val updatedNick = vm.generateRandomFranchiseNickname(targetCity.id, excludeNickname = randomNick)
        val newPrimary = FranchiseIdentityEngine.brandColorPalette[2].hex
        val newSecondary = FranchiseIdentityEngine.brandColorPalette[23].hex
        val (ok, _) = vm.updateFranchiseIdentity(
            franchiseId = newFranchise.id,
            newNickname = updatedNick,
            primaryColorHex = newPrimary,
            secondaryColorHex = newSecondary
        )
        assertTrue(ok)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val repo = LeagueRepository(context)
        val s = vm.uiState.value
        runBlocking {
            kotlinx.coroutines.delay(150)
            repo.saveLeague(
                leagueName = s.leagueName,
                sportType = s.sportType,
                commissionerBackground = s.commissionerBackground,
                currentYear = s.currentYear,
                currentWeek = s.currentWeek,
                seasonPhase = s.seasonPhase,
                cashReservesMillions = s.cashReservesMillions,
                leagueValuationMillions = s.leagueValuationMillions,
                leagueIntegrityScore = s.leagueIntegrityScore,
                competitiveParityScore = s.competitiveParityScore,
                unionAgreement = s.unionAgreement,
                activeBroadcast = s.activeBroadcast,
                franchises = s.franchises,
                cities = s.cities,
                campaigns = s.campaigns,
                history = s.valuationHistory,
                newsFeed = s.recentNews,
                ruleBook = s.ruleBook
            )
            val savedEntity = repo.loadLeague()
            assertNotNull(savedEntity)
            val restored = repo.deserializeFranchises(savedEntity!!.franchisesJson, s.currentYear)
            val restoredTeam = restored.first { it.id == newFranchise.id }
            assertEquals(updatedNick, restoredTeam.nickname)
            assertEquals(newPrimary, restoredTeam.colorHex)
            assertEquals(newSecondary, restoredTeam.secondaryColorHex)
            assertNotEquals(restoredTeam.colorHex, restoredTeam.secondaryColorHex)
        }
    }

    @Test
    fun marketsScreenExpansionModalRandomNicknameAnd24ColorDualPickerUi() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                MarketsScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Open Expansion Ratification Modal via top Expansion Committee button
        composeTestRule.onNodeWithTag("btn_open_expansion_draft_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("expansion_ratification_modal").assertIsDisplayed()

        // Verify all 24 swatches render in the 4x6 grid
        composeTestRule.onNodeWithTag("grid_franchise_24_colors").performScrollTo()
        for (i in 0 until 24) {
            composeTestRule.onNodeWithTag("brand_color_swatch_$i").assertIsDisplayed()
        }

        // Test random realistic nickname generation button
        composeTestRule.onNodeWithTag("input_franchise_nickname").performScrollTo()
        val initialNick = composeTestRule.onNodeWithTag("input_franchise_nickname")
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text
        assertTrue(initialNick.isNotBlank())

        composeTestRule.onNodeWithTag("btn_randomize_nickname").performClick()
        composeTestRule.waitForIdle()
        val randomizedNick = composeTestRule.onNodeWithTag("input_franchise_nickname")
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text
        assertTrue(randomizedNick.isNotBlank())
        assertNotEquals(initialNick, randomizedNick)

        // Pick 2 different colors out of the 24 options (swatch 4 Primary, swatch 9 Secondary)
        composeTestRule.onNodeWithTag("slot_primary_color").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("brand_color_swatch_4").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("slot_secondary_color").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("brand_color_swatch_9").performScrollTo().performClick()

        // Selecting Primary swatch equal to active Secondary should swap rather than collide
        composeTestRule.onNodeWithTag("slot_primary_color").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("brand_color_swatch_9").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        // Now Primary = swatch 9, Secondary = swatch 4; swap back or keep distinct pair and confirm
        composeTestRule.onNodeWithTag("btn_confirm_ratification").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val chartered = vm.uiState.value.franchises.last()
        assertEquals(randomizedNick, chartered.nickname)
        assertEquals(FranchiseIdentityEngine.brandColorPalette[9].hex, chartered.colorHex)
        assertEquals(FranchiseIdentityEngine.brandColorPalette[4].hex, chartered.secondaryColorHex)
        assertNotEquals(chartered.colorHex, chartered.secondaryColorHex)
    }

    @Test
    fun franchisesScreenCustomizeDialogAllowsRandomNicknameAndTwoColors() {
        val context = ApplicationProvider.getApplicationContext<Context>() as Application
        val vm = LeagueViewModel(context)
        val firstTeam = vm.uiState.value.franchises.first()
        val originalNick = firstTeam.nickname

        composeTestRule.setContent {
            val uiState by vm.uiState.collectAsState()
            MyApplicationTheme {
                FranchisesScreen(uiState = uiState, viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("franchises_screen_content")
            .performScrollToNode(hasTestTag("btn_customize_franchise_${firstTeam.id}"))
        for (attempt in 0 until 8) {
            val isVisible = runCatching {
                composeTestRule.onNodeWithTag("btn_customize_franchise_${firstTeam.id}").assertIsDisplayed()
                true
            }.getOrDefault(false)
            if (isVisible) break
            composeTestRule.onNodeWithTag("franchises_screen_content")
                .performTouchInput { swipeUp(startY = bottom * 0.75f, endY = bottom * 0.35f) }
            composeTestRule.waitForIdle()
        }
        // Nudge slightly more so the full 48dp button center is inside viewport
        composeTestRule.onNodeWithTag("franchises_screen_content")
            .performTouchInput { swipeUp(startY = centerY, endY = centerY - 80f) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_customize_franchise_${firstTeam.id}").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("franchise_identity_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_randomize_nickname").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val rolledNick = composeTestRule.onNodeWithTag("input_franchise_nickname")
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text
        assertNotEquals(originalNick, rolledNick)

        composeTestRule.onNodeWithTag("slot_primary_color").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("brand_color_swatch_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("slot_secondary_color").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("brand_color_swatch_8").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("btn_save_franchise_identity").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val updated = vm.uiState.value.franchises.first { it.id == firstTeam.id }
        assertEquals(rolledNick, updated.nickname)
        assertEquals(FranchiseIdentityEngine.brandColorPalette[1].hex, updated.colorHex)
        assertEquals(FranchiseIdentityEngine.brandColorPalette[8].hex, updated.secondaryColorHex)
    }
}
