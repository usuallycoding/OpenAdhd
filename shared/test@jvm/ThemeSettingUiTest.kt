package com.usuallycoding.openadhd

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Theme setting in Settings: its row, its options sheet, Restore, and the effect on the whole app. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ThemeSettingUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun ComposeUiTest.openSettings(store: InMemoryStore = InMemoryStore()): InMemoryStore {
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Settings").performClick()
        return store
    }

    private fun InMemoryStore.theme() = runBlocking { settings.first() }.themeMode

    private fun ComposeUiTest.openThemeSheet() {
        onNodeWithText("Theme").performClick()
    }

    @Test
    fun `the appearance group comes first with the theme row and its default`() = runComposeUiTest {
        openSettings()
        onNodeWithText("Appearance").assertExists()
        onNodeWithText("Theme").assertExists()
        onNodeWithText("Default: Follow system theme").assertExists()
        val appearanceTop = onNodeWithText("Appearance").getUnclippedBoundsInRoot().top
        val profileTop = onNodeWithText("Profile").getUnclippedBoundsInRoot().top
        assertTrue(appearanceTop < profileTop, "Appearance should be above Profile")
    }

    @Test
    fun `the row shows the current theme and restore is disabled at the default`() = runComposeUiTest {
        openSettings()
        onNodeWithContentDescription("Restore Theme").assertIsNotEnabled()
        onAllNodesWithText("Follow system theme").assertCountEqualsAtLeast(1)
    }

    @Test
    fun `the sheet lists the three options with labels`() = runComposeUiTest {
        openSettings()
        openThemeSheet()
        for (mode in ThemeMode.entries) {
            onNodeWithTag("theme-option-${mode.name}").assertExists()
        }
        onNodeWithText("Light").assertExists()
        onNodeWithText("Dark").assertExists()
    }

    @Test
    fun `the current option is the selected one`() = runComposeUiTest {
        val store = InMemoryStore().also { runBlocking { it.saveSetting(SettingKey.Theme, "Light") } }
        openSettings(store)
        openThemeSheet()
        onNodeWithTag("theme-option-Light").assertIsSelected()
        onNodeWithTag("theme-option-Dark").assertIsNotSelected()
        onNodeWithTag("theme-option-System").assertIsNotSelected()
    }

    @Test
    fun `choosing dark saves it applies it and closes the sheet`() = runComposeUiTest {
        val store = openSettings()
        val before = backgroundLuminance()
        openThemeSheet()
        onNodeWithTag("theme-option-Dark").performClick()
        waitForIdle()

        assertEquals(ThemeMode.Dark, store.theme())
        onNodeWithTag("theme-option-Dark").assertDoesNotExist()
        onNodeWithText("Dark").assertExists()
        onNodeWithContentDescription("Restore Theme").assertIsEnabled()
        assertTrue(backgroundLuminance() < 0.3f, "the app should be dark now (it was $before)")
    }

    @Test
    fun `choosing light on a dark app switches it back`() = runComposeUiTest {
        val store = InMemoryStore().also { runBlocking { it.saveSetting(SettingKey.Theme, "Dark") } }
        openSettings(store)
        openThemeSheet()
        onNodeWithTag("theme-option-Light").performClick()
        waitForIdle()

        assertEquals(ThemeMode.Light, store.theme())
        assertTrue(backgroundLuminance() > 0.7f, "the app should be light now")
    }

    @Test
    fun `restore puts the system theme back and disables itself`() = runComposeUiTest {
        val store = InMemoryStore().also { runBlocking { it.saveSetting(SettingKey.Theme, "Dark") } }
        openSettings(store)
        onNodeWithContentDescription("Restore Theme").performClick()
        waitForIdle()

        assertEquals(ThemeMode.System, store.theme())
        onNodeWithContentDescription("Restore Theme").assertIsNotEnabled()
    }

    @Test
    fun `closing the sheet changes nothing`() = runComposeUiTest {
        val store = openSettings()
        openThemeSheet()
        onNodeWithText("Close").performClick()
        waitForIdle()
        assertEquals(ThemeMode.System, store.theme())
        onNodeWithTag("theme-option-Dark").assertDoesNotExist()
    }

    private fun ComposeUiTest.backgroundLuminance(): Float {
        val image = onRoot().captureToImage().toPixelMap()
        return image[image.width / 2, image.height * 3 / 4].luminance()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEqualsAtLeast(count: Int) {
        assertTrue(fetchSemanticsNodes().size >= count)
    }
}
