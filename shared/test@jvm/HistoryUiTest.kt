package com.usuallycoding.openadhd

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The History tab of the Today screen, with the clock fixed to Saturday 24 October 2026 around noon. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class HistoryUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val clock = FakeClock(Instant.parse("2026-10-24T12:00:00Z"))
    private val yesterday = LocalDate(2026, 10, 23)
    private val dayBefore = LocalDate(2026, 10, 22)

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    // The bottom bar items are tabs too, and the tab row comes first in the tree.
    private fun ComposeUiTest.todayTab() = onAllNodes(hasText("Today") and isTab)[0]

    private fun container(store: InMemoryStore) = AppContainer(store, clock, dateCheckInterval = null)

    private fun ComposeUiTest.openHistory(store: InMemoryStore) {
        setContent { App(container(store)) }
        onNode(hasText("History") and isTab).performClick()
    }

    /** Yesterday: a pending task, a done task and a done task with three subtasks. The day before: one pending task. */
    private fun historyStore(): InMemoryStore {
        val store = InMemoryStore(clock)
        runBlocking {
            val pending = store.create("Water plants", Tier.One, Energy.Low)
            val done = store.create("Pay rent", Tier.One, Energy.Low)
            val chores = store.create("Clean kitchen", Tier.Three, Energy.Medium, subtasks = listOf("Dishes", "Counters", "Trash"))
            val old = store.create("Old errand", Tier.One, Energy.Low)
            store.assign(pending, yesterday)
            store.completeTask(store.assign(done, yesterday).id)
            val entry = store.assign(chores, yesterday)
            entry.subtasks.forEach { store.completeSubtask(it.id) }
            store.assign(old, dayBefore)
        }
        return store
    }

    private fun ComposeUiTest.swipeContentLeft() = onRoot().performTouchInput { swipeLeft() }

    private fun ComposeUiTest.swipeContentRight() = onRoot().performTouchInput { swipeRight() }

    // ---- the day and the list

    /** A day in the calendar, found by its description, which also says whether the day has tasks. */
    private fun ComposeUiTest.calendarDay(label: String) = onNode(hasContentDescription(label, substring = true))

    @Test
    fun `history opens on yesterday with its entries and no next day`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithText("Fri 23 Oct 2026").assertExists()
        onNodeWithText("Water plants").assertExists()
        onNodeWithText("Pay rent").assertExists()
        onNodeWithText("Clean kitchen").assertExists()
        onNodeWithText("Old errand").assertDoesNotExist()
        onNodeWithContentDescription("Next day").assertDoesNotExist()
        onNodeWithContentDescription("Previous day").assertExists()
    }

    @Test
    fun `stepping back and forth changes the day`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithContentDescription("Previous day").performClick()
        onNodeWithText("Thu 22 Oct 2026").assertExists()
        onNodeWithText("Old errand").assertExists()
        onNodeWithText("Water plants").assertDoesNotExist()

        onNodeWithContentDescription("Next day").performClick()
        onNodeWithText("Fri 23 Oct 2026").assertExists()
        onNodeWithText("Water plants").assertExists()
        onNodeWithContentDescription("Next day").assertDoesNotExist()
    }

    @Test
    fun `an empty day says nothing was planned and has no add button`() = runComposeUiTest {
        openHistory(InMemoryStore(clock))
        onNodeWithText("Fri 23 Oct 2026").assertExists()
        onNodeWithText("Nothing was planned on this day").assertExists()
        onNodeWithContentDescription("Add task to today").assertDoesNotExist()
        onNodeWithContentDescription("Pick one for me").assertDoesNotExist()
    }

    @Test
    fun `the list has no controls and browsing changes no coins`() = runComposeUiTest {
        val store = historyStore()
        val ledgerBefore = runBlocking { store.entries.first() }
        openHistory(store)

        onAllNodes(isToggleable() and isEnabled()).assertCountEquals(0)
        onNodeWithContentDescription("Options for Water plants").assertDoesNotExist()
        onNodeWithContentDescription("Move Water plants up").assertDoesNotExist()
        onNodeWithContentDescription("Move Water plants down").assertDoesNotExist()
        onNodeWithContentDescription("Add task to today").assertDoesNotExist()
        onNodeWithContentDescription("Pick one for me").assertDoesNotExist()

        // Done entries still show their state and when they were done.
        onAllNodesWithText("Tier 1 · Low energy · +5 coins", substring = true).assertCountEquals(2)
        onAllNodesWithText("Done ", substring = true).assertCountEquals(2)

        // Poking at rows changes nothing in the ledger.
        onNodeWithText("Water plants").performClick()
        onNodeWithText("Pay rent").performClick()
        onNodeWithText("Clean kitchen").performClick()
        assertEquals(ledgerBefore, runBlocking { store.entries.first() })
    }

    @Test
    fun `a done entry starts collapsed and can be expanded without anything to check`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithText("3 of 3 done", substring = true).assertExists()
        onNodeWithText("Dishes").assertDoesNotExist()

        onNodeWithText("Clean kitchen").performClick()
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Counters").assertExists()
        onNodeWithText("Trash").assertExists()
        onAllNodes(isToggleable() and isEnabled()).assertCountEquals(0)

        onNodeWithText("Clean kitchen").performClick()
        onNodeWithText("Dishes").assertDoesNotExist()
    }

    @Test
    fun `a pending entry with subtasks starts expanded`() = runComposeUiTest {
        val store = InMemoryStore(clock)
        runBlocking {
            store.assign(store.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("Dishes", "Counters")), yesterday)
        }
        openHistory(store)
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Counters").assertExists()
    }

    @Test
    fun `the chosen day is kept after visiting the Shop`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithContentDescription("Previous day").performClick()
        onNodeWithText("Shop").performClick()
        onNodeWithText("Today").performClick()
        onNodeWithText("Thu 22 Oct 2026").assertExists()
    }

    // ---- the calendar

    @Test
    fun `the calendar marks days with tasks and allows every earlier day`() = runComposeUiTest {
        val store = historyStore()
        runBlocking {
            store.assign(store.create("Today only", Tier.One, Energy.Low), LocalDate(2026, 10, 24))
        }
        openHistory(store)
        onNodeWithText("Fri 23 Oct 2026").performClick()

        calendarDay("Thursday, October 22, 2026, has tasks").assertIsEnabled()
        calendarDay("Friday, October 23, 2026, has tasks").assertIsEnabled()
        calendarDay("Wednesday, October 21, 2026").assertIsEnabled()
        onNode(hasContentDescription("Wednesday, October 21, 2026, has tasks")).assertDoesNotExist()
        calendarDay("Saturday, October 24, 2026").assertIsNotEnabled()
        calendarDay("Sunday, October 25, 2026").assertIsNotEnabled()
        onNodeWithContentDescription("Next month").assertDoesNotExist()
    }

    @Test
    fun `picking a day with tasks shows it and an empty day says nothing was planned`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithText("Fri 23 Oct 2026").performClick()
        calendarDay("Thursday, October 22, 2026").performClick()
        onNodeWithText("OK").performClick()
        onNodeWithText("Thu 22 Oct 2026").assertExists()
        onNodeWithText("Old errand").assertExists()
        onNodeWithText("OK").assertDoesNotExist()

        onNodeWithText("Thu 22 Oct 2026").performClick()
        calendarDay("Wednesday, October 21, 2026").performClick()
        onNodeWithText("OK").performClick()
        onNodeWithText("Wed 21 Oct 2026").assertExists()
        onNodeWithText("Nothing was planned on this day").assertExists()
    }

    @Test
    fun `the calendar pages to earlier months and back`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithText("Fri 23 Oct 2026").performClick()
        onNodeWithText("October 2026").assertExists()
        onNodeWithContentDescription("Previous month").performClick()
        onNodeWithText("September 2026").assertExists()
        calendarDay("Tuesday, September 15, 2026").assertIsEnabled()
        onNodeWithContentDescription("Next month").performClick()
        onNodeWithText("October 2026").assertExists()
    }

    @Test
    fun `cancelling the calendar keeps the day`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithText("Fri 23 Oct 2026").performClick()
        calendarDay("Thursday, October 22, 2026").performClick()
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Fri 23 Oct 2026").assertExists()
        onNodeWithText("Cancel").assertDoesNotExist()
    }

    // ---- the tabs

    @Test
    fun `the Today screen starts on Today and swiping and tapping change the tab`() = runComposeUiTest {
        setContent { App(container(historyStore())) }
        todayTab().assertIsSelected()
        onNodeWithText("Nothing planned for today.").assertExists()

        swipeContentRight()
        todayTab().assertIsSelected()

        swipeContentLeft()
        onNode(hasText("History") and isTab).assertIsSelected()
        onNodeWithText("Fri 23 Oct 2026").assertExists()

        swipeContentLeft()
        onNode(hasText("History") and isTab).assertIsSelected()

        swipeContentRight()
        todayTab().assertIsSelected()

        onNode(hasText("History") and isTab).performClick()
        onNode(hasText("History") and isTab).assertIsSelected()
    }

    @Test
    fun `the Today screen remembers its tab after visiting another screen`() = runComposeUiTest {
        openHistory(historyStore())
        onNodeWithText("Shop").performClick()
        onNodeWithText("Today").performClick()
        onNode(hasText("History") and isTab).assertIsSelected()
        onNodeWithText("Fri 23 Oct 2026").assertExists()
    }

    @Test
    fun `reorder is offered on Today but not on History and is off when coming back`() = runComposeUiTest {
        val store = InMemoryStore(clock)
        runBlocking {
            val today = LocalDate(2026, 10, 24)
            store.assign(store.create("Alpha", Tier.One, Energy.Low), today)
            store.assign(store.create("Beta", Tier.One, Energy.Low), today)
        }
        setContent { App(container(store)) }
        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithContentDescription("Move Alpha down").assertExists()

        onNode(hasText("History") and isTab).performClick()
        onNodeWithContentDescription("Reorder").assertDoesNotExist()
        onNodeWithContentDescription("Finish reordering").assertDoesNotExist()

        todayTab().performClick()
        onNodeWithContentDescription("Reorder").assertExists()
        onNodeWithContentDescription("Move Alpha down").assertDoesNotExist()
    }
}
