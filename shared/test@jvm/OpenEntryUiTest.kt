package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

/** A platform asking the app to show an entry, as a tapped reminder notification does. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class OpenEntryUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val store = InMemoryStore()
    private val container = AppContainer(store, dateCheckInterval = null)
    private lateinit var entries: List<PlannedTask>

    private fun fill(count: Int = 30) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        runBlocking {
            entries = (1..count).map { n -> store.assign(store.create("Task $n", Tier.One, Energy.Low), today) }
        }
    }

    private fun ComposeUiTest.highlighted(name: String) =
        onNode(isSelected() and hasAnyDescendant(hasText(name)), useUnmergedTree = true)

    private fun ComposeUiTest.selectedCount() = onAllNodes(isSelected() and hasAnyDescendant(hasText("Task", substring = true)), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `a request made before the app is shown scrolls to and highlights the entry`() = runComposeUiTest {
        fill()
        container.entryRequests.open(entries[27].id)
        setContent { App(container) }
        waitForIdle()
        highlighted("Task 28").assertExists()
        assertEquals(null, container.entryRequests.pending.value)
    }

    @Test
    fun `a request made while the app is showing does the same`() = runComposeUiTest {
        fill()
        setContent { App(container) }
        waitForIdle()
        container.entryRequests.open(entries[24].id)
        waitForIdle()
        highlighted("Task 25").assertExists()
    }

    @Test
    fun `a request from the History tab shows Today`() = runComposeUiTest {
        fill()
        setContent { App(container) }
        onNodeWithText("History").performClick()
        waitForIdle()
        container.entryRequests.open(entries[10].id)
        waitForIdle()
        highlighted("Task 11").assertExists()
    }

    @Test
    fun `a request from the Shop shows Today`() = runComposeUiTest {
        fill()
        setContent { App(container) }
        onNodeWithText("Shop").performClick()
        waitForIdle()
        container.entryRequests.open(entries[12].id)
        waitForIdle()
        highlighted("Task 13").assertExists()
        onNodeWithText("Pick one for me", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a request ends Reorder mode`() = runComposeUiTest {
        fill(5)
        setContent { App(container) }
        onNodeWithContentDescription("Reorder").performClick()
        waitForIdle()
        onNodeWithContentDescription("Finish reordering").assertExists()
        container.entryRequests.open(entries[3].id)
        waitForIdle()
        onNodeWithContentDescription("Finish reordering").assertDoesNotExist()
        highlighted("Task 4").assertExists()
    }

    @Test
    fun `a completed entry is still highlighted`() = runComposeUiTest {
        fill(5)
        runBlocking { store.completeTask(entries[1].id) }
        setContent { App(container) }
        container.entryRequests.open(entries[1].id)
        waitForIdle()
        highlighted("Task 2").assertExists()
    }

    @Test
    fun `an entry that does not exist opens Today with nothing highlighted`() = runComposeUiTest {
        fill(5)
        setContent { App(container) }
        onNodeWithText("Shop").performClick()
        waitForIdle()
        container.entryRequests.open("missing")
        mainClock.advanceTimeBy(5_000)
        waitForIdle()
        onNodeWithText("Task 1").assertExists()
        assertEquals(0, selectedCount())
        assertEquals(null, container.entryRequests.pending.value)
    }

    @Test
    fun `two requests show their own entries one after the other`() = runComposeUiTest {
        fill()
        setContent { App(container) }
        waitForIdle()
        container.entryRequests.open(entries[5].id)
        waitForIdle()
        highlighted("Task 6").assertExists()
        container.entryRequests.open(entries[20].id)
        waitForIdle()
        highlighted("Task 21").assertExists()
    }
}
