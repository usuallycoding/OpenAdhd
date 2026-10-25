package com.usuallycoding.openadhd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.screens.TodayContent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Scrolling to and highlighting an entry in the Today list because of a request to reveal it. */
@OptIn(ExperimentalTestApi::class)
class RevealEntryUiTest {
    private val day = LocalDate(2026, 10, 25)

    private fun task(n: Int) = PlannedTask("id$n", day, "Task $n", Tier.One, Energy.Low, null)

    private val many = (1..30).map(::task)

    private fun androidx.compose.ui.test.ComposeUiTest.highlighted(name: String) =
        onNode(isSelected() and hasAnyDescendant(hasText(name)), useUnmergedTree = true)

    @Test
    fun `an entry far down the list is scrolled to and highlighted`() = runComposeUiTest {
        val requests = Channel<String>(Channel.CONFLATED)
        requests.trySend("id28")
        setContent {
            TodayContent(
                entries = many, templates = emptyList(), onAssign = {}, onRemove = {}, onCompleteTask = {}, onCompleteSubtask = {},
                pickedEntryIds = requests.receiveAsFlow(),
            )
        }
        waitForIdle()
        onNodeWithText("Task 28").assertExists()
        highlighted("Task 28").assertExists()
    }

    @Test
    fun `the highlight ends after a short while`() = runComposeUiTest {
        val requests = Channel<String>(Channel.CONFLATED)
        requests.trySend("id3")
        setContent {
            TodayContent(
                entries = many, templates = emptyList(), onAssign = {}, onRemove = {}, onCompleteTask = {}, onCompleteSubtask = {},
                pickedEntryIds = requests.receiveAsFlow(),
            )
        }
        waitForIdle()
        highlighted("Task 3").assertExists()
        mainClock.advanceTimeBy(5_000)
        waitForIdle()
        assertEquals(0, onAllNodesSelected())
    }

    private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesSelected() =
        onAllNodes(isSelected(), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `a request that arrives before the list has loaded waits for the entry`() = runComposeUiTest {
        val requests = Channel<String>(Channel.CONFLATED)
        var entries by mutableStateOf(emptyList<PlannedTask>())
        // One flow for the whole test: a new one on each recomposition would restart the collector and lose the request.
        val reveals = requests.receiveAsFlow()
        setContent {
            TodayContent(
                entries = entries, templates = emptyList(), onAssign = {}, onRemove = {}, onCompleteTask = {}, onCompleteSubtask = {},
                pickedEntryIds = reveals,
            )
        }
        requests.trySend("id20")
        mainClock.advanceTimeBy(500)
        entries = many
        waitForIdle()
        highlighted("Task 20").assertExists()
    }

    @Test
    fun `an entry that never appears is ignored and nothing is highlighted`() = runComposeUiTest {
        val requests = Channel<String>(Channel.CONFLATED)
        requests.trySend("missing")
        setContent {
            TodayContent(
                entries = many.take(3), templates = emptyList(), onAssign = {}, onRemove = {}, onCompleteTask = {}, onCompleteSubtask = {},
                pickedEntryIds = requests.receiveAsFlow(),
            )
        }
        mainClock.advanceTimeBy(5_000)
        waitForIdle()
        assertEquals(0, onAllNodesSelected())
        onNodeWithText("Task 1").assertExists()
    }

    @Test
    fun `a completed entry is still scrolled to and highlighted`() = runComposeUiTest {
        val requests = Channel<String>(Channel.CONFLATED)
        val done = many.take(2).map { it.copy(completedAt = kotlin.time.Instant.fromEpochSeconds(1)) }
        requests.trySend("id2")
        setContent {
            TodayContent(
                entries = many.drop(2) + done, templates = emptyList(), onAssign = {}, onRemove = {}, onCompleteTask = {}, onCompleteSubtask = {},
                pickedEntryIds = requests.receiveAsFlow(),
            )
        }
        waitForIdle()
        highlighted("Task 2").assertExists()
    }
}
