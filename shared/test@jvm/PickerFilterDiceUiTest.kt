package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The energy filter and the dice of the "Add to today" picker. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PickerFilterDiceUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val dice = "Roll a task from the list"

    // The list order is Laundry (Low), Taxes (High), Dishes (Low), Report (Medium); new templates go on top.
    private fun store(): InMemoryStore {
        val store = InMemoryStore()
        runBlocking {
            store.create("Report", Tier.One, Energy.Medium, "wash the numbers")
            store.create("Dishes", Tier.One, Energy.Low)
            store.create("Taxes", Tier.One, Energy.High)
            store.create("Laundry", Tier.One, Energy.Low)
        }
        return store
    }

    private fun ComposeUiTest.openPicker(store: InMemoryStore = store(), random: Random = Random.Default) {
        setContent { App(AppContainer(store, dateCheckInterval = null, random = random)) }
        onNodeWithContentDescription("Add task to today").performClick()
    }

    private fun ComposeUiTest.type(text: String) = onNode(hasSetTextAction()).performTextInput(text)

    private fun ComposeUiTest.assertListed(vararg names: String) {
        listOf("Laundry", "Taxes", "Dishes", "Report").forEach { name ->
            if (name in names) onNodeWithText(name).assertExists() else onNodeWithText(name).assertDoesNotExist()
        }
    }

    @Test
    fun `each energy chip lists only templates of that energy`() = runComposeUiTest {
        openPicker()
        onNodeWithText("Low").performClick()
        assertListed("Laundry", "Dishes")
        onNodeWithText("High").performClick()
        assertListed("Taxes")
        onNodeWithText("Medium").performClick()
        assertListed("Report")
    }

    @Test
    fun `the energy filter combines with the text`() = runComposeUiTest {
        openPicker()
        onNodeWithText("Low").performClick()
        type("dis")
        assertListed("Dishes")
    }

    @Test
    fun `tapping the selected chip clears the filter`() = runComposeUiTest {
        openPicker()
        onNodeWithText("Low").performClick()
        assertListed("Laundry", "Dishes")
        onNodeWithText("Low").performClick()
        assertListed("Laundry", "Taxes", "Dishes", "Report")
    }

    @Test
    fun `the filter starts cleared each time the picker opens`() = runComposeUiTest {
        openPicker()
        onNodeWithText("High").performClick()
        assertListed("Taxes")
        onNodeWithText("Close").performClick()
        waitForIdle()

        onNodeWithContentDescription("Add task to today").performClick()
        assertListed("Laundry", "Taxes", "Dishes", "Report")
    }

    @Test
    fun `an empty result names the energy and the text`() = runComposeUiTest {
        openPicker()
        onNodeWithText("High").performClick()
        type("dis")
        onNodeWithText("No High energy task matches dis").assertExists()

        onNodeWithContentDescription("Clear search").performClick()
        onNodeWithText("Taxes").assertExists()
        onNodeWithText("Low").performClick()
        onNodeWithText("Medium").performClick()
        type("zzz")
        onNodeWithText("No Medium energy task matches zzz").assertExists()
    }

    @Test
    fun `an energy with no templates says so without a text`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking { store.create("Laundry", Tier.One, Energy.Low) }
        openPicker(store)
        onNodeWithText("High").performClick()
        onNodeWithText("No High energy task").assertExists()
    }

    @Test
    fun `without templates there are no chips and no dice`() = runComposeUiTest {
        openPicker(InMemoryStore())
        onNodeWithText("Low").assertDoesNotExist()
        onNodeWithText("Medium").assertDoesNotExist()
        onNodeWithContentDescription(dice).assertDoesNotExist()
    }

    @Test
    fun `rolling highlights a listed template and adds nothing`() = runComposeUiTest {
        val store = store()
        // Index 2 of Laundry, Taxes, Dishes, Report.
        openPicker(store, ScriptedRandom(2))
        onNodeWithContentDescription(dice).performClick()

        onNodeWithText("Dishes").assertIsSelected()
        onNodeWithText("Laundry").assertIsNotSelected()
        assertTrue(runBlocking { store.daysWithEntries.first() }.isEmpty())
        assertTrue(runBlocking { store.entries.first() }.isEmpty())
    }

    @Test
    fun `rolling respects the energy filter and the text`() = runComposeUiTest {
        val store = store()
        openPicker(store, ScriptedRandom())
        onNodeWithText("Low").performClick()
        type("dis")
        // One template is shown, so it is returned without drawing.
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Dishes").assertIsSelected()
    }

    @Test
    fun `rolling again picks the other of two and a single one is picked twice`() = runComposeUiTest {
        openPicker(random = ScriptedRandom(1, 0))
        onNodeWithText("Low").performClick()
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Dishes").assertIsSelected()
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Laundry").assertIsSelected()
        onNodeWithText("Dishes").assertIsNotSelected()

        onNodeWithText("High").performClick()
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Taxes").assertIsSelected()
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Taxes").assertIsSelected()
    }

    @Test
    fun `the dice is disabled while nothing is listed`() = runComposeUiTest {
        openPicker()
        onNodeWithContentDescription(dice).assertIsEnabled()
        type("xyz")
        onNodeWithContentDescription(dice).assertIsNotEnabled()
    }

    @Test
    fun `tapping the rolled template adds it and closes the picker`() = runComposeUiTest {
        val store = store()
        openPicker(store, ScriptedRandom(2))
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Dishes").performClick()
        waitForIdle()

        onNodeWithText("Search tasks").assertDoesNotExist()
        assertEquals(listOf("Dishes"), runBlocking { store.templates.first() }.filter { it.name == "Dishes" }.map { it.name })
        assertEquals(1, runBlocking { store.daysWithEntries.first() }.size)
    }

    @Test
    fun `the roll is forgotten when the picker closes`() = runComposeUiTest {
        // Two Low templates: the first roll draws index 1 (Dishes). After reopening nothing is excluded, so index 1 is Dishes again.
        openPicker(random = ScriptedRandom(1, 1))
        onNodeWithText("Low").performClick()
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Dishes").assertIsSelected()
        onNodeWithText("Close").performClick()
        waitForIdle()

        onNodeWithContentDescription("Add task to today").performClick()
        onNodeWithText("Low").performClick()
        onNodeWithContentDescription(dice).performClick()
        onNodeWithText("Dishes").assertIsSelected()
    }
}
