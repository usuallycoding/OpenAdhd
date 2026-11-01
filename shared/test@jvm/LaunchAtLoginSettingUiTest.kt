package com.usuallycoding.openadhd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.LoginItem
import com.usuallycoding.openadhd.ui.screens.SettingsScreen
import com.usuallycoding.openadhd.ui.settings.SettingsViewModel
import com.usuallycoding.openadhd.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Launch at login switch in Settings, shown only where the platform has a login item. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class LaunchAtLoginSettingUiTest {
    private class FakeLoginItem(var on: Boolean = false) : LoginItem {
        val calls = mutableListOf<Boolean>()

        override fun isEnabled() = on

        override fun setEnabled(enabled: Boolean) {
            calls += enabled
            on = enabled
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** The screen in a window tall enough to lay out every row, so the rows can be compared by position. */
    private fun ComposeUiTest.openSettings(loginItem: LoginItem?, tall: Boolean = true) {
        val viewModel = SettingsViewModel(InMemoryStore(), loginItem = loginItem)
        setContent {
            AppTheme {
                if (tall) Box(Modifier.requiredHeight(4000.dp)) { SettingsScreen(viewModel) } else SettingsScreen(viewModel)
            }
        }
    }

    @Test
    fun `the switch is listed after reminders and before data and is off by default`() = runComposeUiTest {
        openSettings(FakeLoginItem())
        onNodeWithText("Startup").assertExists()
        onNodeWithText("Launch at login").assertExists()
        onNodeWithTag("launch-at-login-switch").assertIsOff()

        val reminders = onNodeWithText("Stop reminders at").getBoundsInRoot().top
        val startup = onNodeWithText("Startup").getBoundsInRoot().top
        val data = onNodeWithText("Data").getBoundsInRoot().top
        assertTrue(reminders < startup && startup < data)
    }

    @Test
    fun `without a login item there is no startup section`() = runComposeUiTest {
        openSettings(null)
        onNodeWithText("Data").assertExists()
        onNodeWithText("Startup").assertDoesNotExist()
        onNodeWithText("Launch at login").assertDoesNotExist()
    }

    @Test
    fun `turning it on and off asks the system`() = runComposeUiTest {
        val item = FakeLoginItem()
        openSettings(item, tall = false)
        onNode(hasScrollAction()).performScrollToNode(hasTestTag("launch-at-login-row"))

        onNodeWithTag("launch-at-login-switch").performClick()
        waitForIdle()
        onNodeWithTag("launch-at-login-switch").assertIsOn()
        assertEquals(true, item.on)

        onNodeWithTag("launch-at-login-row").performClick()
        waitForIdle()
        onNodeWithTag("launch-at-login-switch").assertIsOff()
        assertEquals(listOf(true, false), item.calls)
    }

    @Test
    fun `a login item changed outside the app shows its real state the next time settings opens`() = runComposeUiTest {
        val item = FakeLoginItem(on = true)
        val viewModel = SettingsViewModel(InMemoryStore(), loginItem = item)
        var shown by mutableStateOf(true)
        setContent { AppTheme { Box(Modifier.requiredHeight(4000.dp)) { if (shown) SettingsScreen(viewModel) } } }
        onNodeWithTag("launch-at-login-switch").assertIsOn()

        shown = false
        waitForIdle()
        item.on = false
        shown = true
        waitForIdle()
        onNodeWithTag("launch-at-login-switch").assertIsOff()
    }
}
