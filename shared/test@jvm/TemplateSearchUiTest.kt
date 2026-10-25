package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
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
import com.usuallycoding.openadhd.domain.RewardTier
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

/** Searching the Tasks and Rewards tabs of Templates. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class TemplateSearchUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // Listed top to bottom as Laundry, Dishes, Taxes; Dishes also says "wash up".
    private fun store(): InMemoryStore {
        val store = InMemoryStore()
        runBlocking {
            store.create("Taxes", Tier.One, Energy.Low)
            store.create("Dishes", Tier.One, Energy.Low, "wash up")
            store.create("Laundry", Tier.One, Energy.Low)
            store.createReward("Cake", RewardTier.One, Energy.Low)
            store.createReward("Tea", RewardTier.One, Energy.Low, "green")
            store.createReward("Movie", RewardTier.One, Energy.Low)
        }
        return store
    }

    private fun ComposeUiTest.open(store: InMemoryStore = store()) {
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
    }

    private fun ComposeUiTest.assertTasks(vararg names: String) {
        listOf("Laundry", "Dishes", "Taxes").forEach {
            if (it in names) onNodeWithText(it).assertExists() else onNodeWithText(it).assertDoesNotExist()
        }
    }

    private fun ComposeUiTest.assertRewards(vararg names: String) {
        listOf("Movie", "Tea", "Cake").forEach {
            if (it in names) onNodeWithText(it).assertExists() else onNodeWithText(it).assertDoesNotExist()
        }
    }

    private fun ComposeUiTest.openRewards() {
        onNodeWithText("Rewards").performClick()
        waitForIdle()
    }

    @Test
    fun `the tasks search filters by name and by description`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performTextInput("DIS")
        assertTasks("Dishes")
        onNodeWithContentDescription("Clear task search").performClick()
        assertTasks("Laundry", "Dishes", "Taxes")
        onNodeWithText("Search tasks").performTextInput("wash")
        assertTasks("Dishes")
    }

    @Test
    fun `the clear control shows only with text`() = runComposeUiTest {
        open()
        onNodeWithContentDescription("Clear task search").assertDoesNotExist()
        onNodeWithText("Search tasks").performTextInput("a")
        onNodeWithContentDescription("Clear task search").assertExists()
        onNodeWithContentDescription("Clear task search").performClick()
        onNodeWithContentDescription("Clear task search").assertDoesNotExist()
    }

    @Test
    fun `no match says so and the list returns when cleared`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performTextInput("xyz")
        onNodeWithText("No task matches xyz").assertExists()
        assertTasks()
        onNodeWithContentDescription("Clear task search").performClick()
        assertTasks("Laundry", "Dishes", "Taxes")
        onNodeWithText("No task matches xyz").assertDoesNotExist()
    }

    @Test
    fun `without templates there is no search field and the old message stays`() = runComposeUiTest {
        open(InMemoryStore())
        onNodeWithText("Search tasks").assertDoesNotExist()
        onNodeWithText("No templates yet. Tap + to create your first task.").assertExists()
    }

    @Test
    fun `the field is not focused when the tab opens`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").assertIsNotFocused()
    }

    @Test
    fun `the tasks search is kept after visiting the rewards tab`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performTextInput("dis")
        openRewards()
        onNodeWithText("Tasks").performClick()
        waitForIdle()
        assertTasks("Dishes")
    }

    @Test
    fun `a new template clears the search and is listed`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performTextInput("dis")
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Bills")
        onNodeWithText("Save").performClick()
        waitForIdle()
        assertTasks("Laundry", "Dishes", "Taxes")
        onNodeWithText("Bills").assertExists()
    }

    @Test
    fun `editing keeps the tasks search`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performTextInput("dis")
        onNodeWithContentDescription("Edit Dishes").performClick()
        onNodeWithText("Save").performClick()
        waitForIdle()
        assertTasks("Dishes")
    }

    @Test
    fun `the reorder toggle is hidden while the tasks search holds text`() = runComposeUiTest {
        open()
        onNodeWithContentDescription("Reorder").assertExists()
        onNodeWithText("Search tasks").performTextInput("a")
        onNodeWithContentDescription("Reorder").assertDoesNotExist()
        onNodeWithContentDescription("Clear task search").performClick()
        onNodeWithContentDescription("Reorder").assertExists()
    }

    @Test
    fun `typing in the search ends reorder mode`() = runComposeUiTest {
        open()
        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithContentDescription("Finish reordering").assertExists()
        onNodeWithText("Search tasks").performTextInput("l")
        onNodeWithContentDescription("Finish reordering").assertDoesNotExist()
        onNodeWithContentDescription("Clear task search").performClick()
        onNodeWithContentDescription("Finish reordering").assertDoesNotExist()
        onNodeWithContentDescription("Reorder").assertExists()
    }

    @Test
    fun `the rewards search filters by name and by description`() = runComposeUiTest {
        open()
        openRewards()
        onNodeWithText("Search rewards").performTextInput("TEA")
        assertRewards("Tea")
        onNodeWithContentDescription("Clear reward search").performClick()
        assertRewards("Movie", "Tea", "Cake")
        onNodeWithText("Search rewards").performTextInput("green")
        assertRewards("Tea")
    }

    @Test
    fun `the rewards tab says when nothing matches and has no field without rewards`() = runComposeUiTest {
        open()
        openRewards()
        onNodeWithText("Search rewards").performTextInput("xyz")
        onNodeWithText("No reward matches xyz").assertExists()
    }

    @Test
    fun `a rewards tab without rewards has no search field`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking { store.create("Taxes", Tier.One, Energy.Low) }
        open(store)
        openRewards()
        onNodeWithText("Search rewards").assertDoesNotExist()
        onNodeWithText("No rewards yet. Tap + to add something to look forward to.").assertExists()
    }

    @Test
    fun `a new reward clears the rewards search and the toggle follows it`() = runComposeUiTest {
        open()
        openRewards()
        onNodeWithText("Search rewards").performTextInput("tea")
        onNodeWithContentDescription("Reorder").assertDoesNotExist()
        onNodeWithContentDescription("Add reward").performClick()
        onNodeWithText("Name").performTextInput("Pie")
        onNodeWithText("Save").performClick()
        waitForIdle()
        assertRewards("Movie", "Tea", "Cake")
        onNodeWithText("Pie").assertExists()
        onNodeWithContentDescription("Reorder").assertExists()
    }

    @Test
    fun `each tab keeps its own search`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performTextInput("dis")
        openRewards()
        onNodeWithText("Search rewards").performTextInput("tea")
        onNodeWithText("Tasks").performClick()
        waitForIdle()
        assertTasks("Dishes")
        openRewards()
        assertRewards("Tea")
    }

    @Test
    fun `tapping the field focuses it`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performClick()
        onNodeWithText("Search tasks").assertIsFocused()
    }

    @Test
    fun `tapping a row puts the keyboard away and keeps the text`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performClick()
        onNodeWithText("Search tasks").performTextInput("a")
        onNodeWithText("Search tasks").assertIsFocused()
        onNodeWithText("Dishes").performClick()
        onNodeWithText("Search tasks").assertIsNotFocused()
        assertTasks("Laundry", "Dishes", "Taxes")
        onNodeWithContentDescription("Clear task search").assertExists()
    }

    @Test
    fun `tapping the no match message puts the keyboard away`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performClick()
        onNodeWithText("Search tasks").performTextInput("xyz")
        onNodeWithText("No task matches xyz").performClick()
        onNodeWithText("Search tasks").assertIsNotFocused()
    }

    @Test
    fun `the done key puts the keyboard away and keeps the text`() = runComposeUiTest {
        open()
        onNodeWithText("Search tasks").performClick()
        onNodeWithText("Search tasks").performTextInput("dis")
        onNodeWithText("Search tasks").performImeAction()
        onNodeWithText("Search tasks").assertIsNotFocused()
        assertTasks("Dishes")
    }

    @Test
    fun `scrolling the list puts the keyboard away`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking { (1..40).forEach { store.create("Chore $it", Tier.One, Energy.Low) } }
        open(store)
        onNodeWithText("Search tasks").performClick()
        onNodeWithText("Search tasks").performTextInput("Chore")
        onNodeWithText("Search tasks").assertIsFocused()
        onNodeWithText("Chore 40").performTouchInput { swipeUp() }
        onNodeWithText("Search tasks").assertIsNotFocused()
    }

    @Test
    fun `the rewards search puts the keyboard away too`() = runComposeUiTest {
        open()
        openRewards()
        onNodeWithText("Search rewards").performClick()
        onNodeWithText("Search rewards").performTextInput("e")
        onNodeWithText("Search rewards").assertIsFocused()
        onNodeWithText("Tea").performClick()
        onNodeWithText("Search rewards").assertIsNotFocused()

        onNodeWithText("Search rewards").performClick()
        onNodeWithText("Search rewards").performImeAction()
        onNodeWithText("Search rewards").assertIsNotFocused()
    }
}
