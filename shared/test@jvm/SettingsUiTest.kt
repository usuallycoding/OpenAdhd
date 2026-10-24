package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.SettingKey
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

/** The Settings tab: listing, editing, refusals, restoring, and the name in the top bar. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class SettingsUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun ComposeUiTest.openSettings(store: InMemoryStore = InMemoryStore()): InMemoryStore {
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Settings").performClick()
        return store
    }

    private fun ComposeUiTest.edit(label: String, text: String) {
        onNodeWithText(label).performClick()
        onNode(hasSetTextAction()).performTextReplacement(text)
    }

    @Test
    fun `the tab lists the settings with their defaults`() = runComposeUiTest {
        openSettings()
        onNodeWithText("Profile").assertExists()
        onNodeWithText("Your name").assertExists()
        onNodeWithText("Debt limit").assertExists()
        onNodeWithText("500").assertExists()
        onNodeWithText("Tier 1 task pays").assertExists()
        onNodeWithText("Not set").assertExists()
    }

    @Test
    fun `the later groups are further down the list`() = runComposeUiTest {
        openSettings()
        for (label in listOf("Most subtasks per task", "Tier 4 reward price", "Critical hit multiplier", "Roll upgrade chance, 1 in", "Stop reminders at")) {
            onNode(hasScrollAction()).performScrollToNode(hasText(label))
            onNodeWithText(label).assertExists()
        }
        onNodeWithText("Luck").assertExists()
        onNode(hasScrollAction()).performScrollToNode(hasText("Stop reminders at"))
        onNodeWithText("Reminders").assertExists()
        onNodeWithText("22").assertExists()
    }

    @Test
    fun `restore is disabled while a setting holds its default`() = runComposeUiTest {
        openSettings()
        onNodeWithContentDescription("Restore Debt limit").assertIsNotEnabled()
    }

    @Test
    fun `editing a value saves it and enables restore`() = runComposeUiTest {
        val store = openSettings()
        edit("Debt limit", "200")
        onNodeWithText("Save").performClick()
        waitForIdle()

        onNodeWithText("200").assertExists()
        onNodeWithContentDescription("Restore Debt limit").assertIsEnabled()
        assertEquals(200, runBlocking { store.settings.first() }.rules.debtLimit)
    }

    @Test
    fun `cancelling leaves the value alone`() = runComposeUiTest {
        val store = openSettings()
        edit("Debt limit", "200")
        onNodeWithText("Cancel").performClick()
        waitForIdle()

        onNodeWithText("500").assertExists()
        assertEquals(500, runBlocking { store.settings.first() }.rules.debtLimit)
    }

    @Test
    fun `a value that is not a number is refused and the sheet stays open`() = runComposeUiTest {
        val store = openSettings()
        edit("Debt limit", "lots")
        onNodeWithText("Save").performClick()

        onNodeWithText("Enter a whole number").assertExists()
        assertEquals(500, runBlocking { store.settings.first() }.rules.debtLimit)
    }

    @Test
    fun `a value that breaks the tier order says which tier`() = runComposeUiTest {
        openSettings()
        edit("Tier 3 task pays", "10")
        onNodeWithText("Save").performClick()
        onNodeWithText("Tier 3 must pay more than tier 2").assertExists()
    }

    @Test
    fun `restore puts the default back and disables itself`() = runComposeUiTest {
        val store = openSettings()
        edit("Debt limit", "200")
        onNodeWithText("Save").performClick()
        waitForIdle()

        onNodeWithContentDescription("Restore Debt limit").performClick()
        waitForIdle()
        onNodeWithText("500").assertExists()
        onNodeWithContentDescription("Restore Debt limit").assertIsNotEnabled()
        assertEquals(500, runBlocking { store.settings.first() }.rules.debtLimit)
    }

    @Test
    fun `restoring one setting leaves the others`() = runComposeUiTest {
        val store = InMemoryStore().also {
            runBlocking {
                it.saveSetting(SettingKey.DebtLimit, "200")
                it.saveSetting(SettingKey.SubtaskCeiling, "6")
            }
        }
        openSettings(store)
        onNodeWithContentDescription("Restore Debt limit").performClick()
        waitForIdle()

        val rules = runBlocking { store.settings.first() }.rules
        assertEquals(500, rules.debtLimit)
        assertEquals(6, rules.subtaskCeiling)
    }

    @Test
    fun `the top bar shows the name and goes back to the plain title`() = runComposeUiTest {
        val store = InMemoryStore().also { runBlocking { it.saveSetting(SettingKey.Name, "Sam") } }
        openSettings(store)
        onNodeWithText("Sam's OpenAdhd").assertExists()

        onNodeWithContentDescription("Restore Your name").performClick()
        waitForIdle()
        onNodeWithText("Sam's OpenAdhd").assertDoesNotExist()
        onNodeWithText("OpenAdhd").assertExists()
    }

    @Test
    fun `a name typed in the form appears in the top bar`() = runComposeUiTest {
        openSettings()
        onNodeWithText("OpenAdhd").assertExists()
        edit("Your name", "Alex")
        onNodeWithText("Save").performClick()
        waitForIdle()
        onNodeWithText("Alex's OpenAdhd").assertExists()
    }
}
