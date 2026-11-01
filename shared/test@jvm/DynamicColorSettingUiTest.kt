package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.ui.screens.SettingsScreen
import com.usuallycoding.openadhd.ui.settings.SettingsViewModel
import com.usuallycoding.openadhd.ui.theme.AppTheme
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Dynamic color setting in Settings, shown on a platform that supports it: its row, its sheet and Restore. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DynamicColorSettingUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun ComposeUiTest.openSettings(supported: Boolean, store: InMemoryStore = InMemoryStore()): InMemoryStore {
        val viewModel = SettingsViewModel(store, dynamicColorSupported = supported)
        setContent { AppTheme { SettingsScreen(viewModel) } }
        return store
    }

    private fun InMemoryStore.dynamicColor() = runBlocking { settings.first() }.dynamicColor

    @Test
    fun `the row follows the theme with its default and a disabled restore`() = runComposeUiTest {
        openSettings(supported = true)
        onNodeWithText("Dynamic color").assertExists()
        onNodeWithText("Default: Dynamic (wallpaper colors)").assertExists()
        onNodeWithContentDescription("Restore Dynamic color").assertIsNotEnabled()
    }

    @Test
    fun `the row is absent where dynamic color is not supported`() = runComposeUiTest {
        openSettings(supported = false)
        onNodeWithText("Theme").assertExists()
        onNodeWithText("Dynamic color").assertDoesNotExist()
    }

    @Test
    fun `the sheet highlights the current choice`() = runComposeUiTest {
        openSettings(supported = true)
        onNodeWithText("Dynamic color").performClick()
        onNodeWithTag("dynamic-color-option-dynamic").assertIsSelected()
        onNodeWithTag("dynamic-color-option-app").assertIsNotSelected()
    }

    @Test
    fun `choosing app colors saves it closes the sheet shows it and enables restore`() = runComposeUiTest {
        val store = openSettings(supported = true)
        onNodeWithText("Dynamic color").performClick()
        onNodeWithTag("dynamic-color-option-app").performClick()
        waitForIdle()

        assertFalse(store.dynamicColor())
        onNodeWithTag("dynamic-color-option-app").assertDoesNotExist()
        onNodeWithText("App colors").assertExists()
        onNodeWithContentDescription("Restore Dynamic color").assertIsEnabled()
    }

    @Test
    fun `restore puts dynamic color back and disables itself`() = runComposeUiTest {
        val store = InMemoryStore().also { runBlocking { it.saveSetting(SettingKey.DynamicColor, "app") } }
        openSettings(supported = true, store = store)
        onNodeWithContentDescription("Restore Dynamic color").performClick()
        waitForIdle()

        assertTrue(store.dynamicColor())
        onNodeWithContentDescription("Restore Dynamic color").assertIsNotEnabled()
    }

    @Test
    fun `closing the sheet changes nothing`() = runComposeUiTest {
        val store = openSettings(supported = true)
        onNodeWithText("Dynamic color").performClick()
        onNodeWithText("Close").performClick()
        waitForIdle()
        assertEquals(true, store.dynamicColor())
        onNodeWithTag("dynamic-color-option-app").assertDoesNotExist()
    }
}
