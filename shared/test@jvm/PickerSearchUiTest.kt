package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/** Searching the "Add to today" picker. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PickerSearchUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun store(): InMemoryStore {
        val store = InMemoryStore()
        runBlocking {
            // New templates go to the top, so they are created last to first to list Laundry, Dishes, Taxes, Reset.
            store.create("Reset the flat", Tier.One, Energy.Low, "wash the dishes")
            store.create("Taxes", Tier.One, Energy.Low)
            store.create("Dishes", Tier.One, Energy.Low)
            store.create("Laundry", Tier.One, Energy.Low)
        }
        return store
    }

    private fun ComposeUiTest.openPicker(store: InMemoryStore = store()) {
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithContentDescription("Add task to today").performClick()
    }

    private fun ComposeUiTest.type(text: String) = onNode(hasSetTextAction()).performTextInput(text)

    @Test
    fun `the search field is focused when the picker opens`() = runComposeUiTest {
        openPicker()
        onNodeWithText("Search tasks").assertExists()
        onNode(hasSetTextAction()).assertIsFocused()
        onNodeWithText("Laundry").assertExists()
        onNodeWithText("Taxes").assertExists()
    }

    @Test
    fun `typing filters the templates by name ignoring case`() = runComposeUiTest {
        openPicker()
        type("DIS")
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Laundry").assertDoesNotExist()
        onNodeWithText("Taxes").assertDoesNotExist()
    }

    @Test
    fun `typing also matches the description`() = runComposeUiTest {
        openPicker()
        type("wash")
        onNodeWithText("Reset the flat").assertExists()
        onNodeWithText("Dishes").assertDoesNotExist()
    }

    @Test
    fun `the clear control shows only with text and empties the field`() = runComposeUiTest {
        openPicker()
        onNodeWithContentDescription("Clear search").assertDoesNotExist()
        type("dis")
        onNodeWithText("Laundry").assertDoesNotExist()

        onNodeWithContentDescription("Clear search").performClick()
        onNodeWithContentDescription("Clear search").assertDoesNotExist()
        onNodeWithText("Laundry").assertExists()
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Taxes").assertExists()
    }

    @Test
    fun `no match says so and listing returns when the text is cleared`() = runComposeUiTest {
        openPicker()
        type("xyz")
        onNodeWithText("No task matches xyz").assertExists()
        onNodeWithText("Laundry").assertDoesNotExist()

        onNodeWithContentDescription("Clear search").performClick()
        onNodeWithText("No task matches xyz").assertDoesNotExist()
        onNodeWithText("Laundry").assertExists()
    }

    @Test
    fun `tapping a result adds it to today and closes the picker`() = runComposeUiTest {
        openPicker()
        type("dis")
        onNodeWithText("Dishes").performClick()
        waitForIdle()
        onNodeWithText("Search tasks").assertDoesNotExist()
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Nothing planned for today.").assertDoesNotExist()
    }

    @Test
    fun `the search starts empty each time the picker opens`() = runComposeUiTest {
        openPicker()
        type("dis")
        onNodeWithText("Laundry").assertDoesNotExist()
        onNodeWithText("Close").performClick()
        waitForIdle()

        onNodeWithContentDescription("Add task to today").performClick()
        onNodeWithText("Laundry").assertExists()
        onNodeWithText("Taxes").assertExists()
        onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun `without templates there is no search field`() = runComposeUiTest {
        openPicker(InMemoryStore())
        onNodeWithText("You have no task templates yet. Create one on the Templates tab first.").assertExists()
        onNode(hasSetTextAction()).assertDoesNotExist()
        onNodeWithText("Search tasks").assertDoesNotExist()
    }

    @Test
    fun `the done key puts the keyboard away and keeps the text`() = runComposeUiTest {
        openPicker()
        type("dis")
        onNode(hasSetTextAction()).assertIsFocused()
        onNode(hasSetTextAction()).performImeAction()
        onNode(hasSetTextAction()).assertIsNotFocused()
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Laundry").assertDoesNotExist()
    }

    @Test
    fun `tapping the no match message puts the keyboard away`() = runComposeUiTest {
        openPicker()
        type("xyz")
        onNodeWithText("No task matches xyz").performClick()
        onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun `scrolling the results puts the keyboard away`() = runComposeUiTest {
        val many = InMemoryStore()
        runBlocking { (1..40).forEach { many.create("Chore $it", Tier.One, Energy.Low) } }
        openPicker(many)
        onNode(hasSetTextAction()).assertIsFocused()
        onNodeWithText("Chore 40").performTouchInput { swipeUp() }
        onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun `tapping the field brings the keyboard back`() = runComposeUiTest {
        openPicker()
        onNode(hasSetTextAction()).performImeAction()
        onNode(hasSetTextAction()).assertIsNotFocused()
        onNode(hasSetTextAction()).performClick()
        onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun `tapping a template with the keyboard up still adds it`() = runComposeUiTest {
        openPicker()
        onNode(hasSetTextAction()).assertIsFocused()
        onNodeWithText("Taxes").performClick()
        waitForIdle()
        onNodeWithText("Search tasks").assertDoesNotExist()
        onNodeWithText("Taxes").assertExists()
    }
}
