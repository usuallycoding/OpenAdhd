package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** The "..." button and options sheet of a Today entry: reminders and deleting. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class EntryOptionsUiTest {
    private val zone = TimeZone.currentSystemDefault()
    private val clock = FakeClock(Instant.parse("2026-10-23T09:00:00Z"))
    private val store = InMemoryStore(clock)
    private val today get() = clock.todayIn(zone)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun entry(name: String = "Clean the kitchen", subtasks: List<String> = emptyList()): PlannedTask = runBlocking {
        store.assign(store.create(name, Tier.Two, Energy.Low, subtasks = subtasks), today)
    }

    private fun entries(): List<PlannedTask> = runBlocking { store.entriesFor(today).first() }

    private fun ComposeUiTest.show(scheduler: FakeReminderScheduler? = FakeReminderScheduler()) {
        setContent { App(AppContainer(store, clock = clock, dateCheckInterval = null, reminders = scheduler)) }
    }

    private fun ComposeUiTest.openOptions(name: String = "Clean the kitchen") {
        onNodeWithContentDescription("Options for $name").performClick()
    }

    private fun preset(label: String) = ReminderRules.presets(clock.now(), today, 22, zone).first { it.label == label }

    @Test
    fun `a pending entry shows the options button and no remove button`() = runComposeUiTest {
        entry()
        show()
        onNodeWithContentDescription("Options for Clean the kitchen").assertExists()
        onNodeWithContentDescription("Remove Clean the kitchen from today").assertDoesNotExist()
    }

    @Test
    fun `the sheet shows the reminder controls and delete`() = runComposeUiTest {
        entry()
        show()
        openOptions()
        onNodeWithText("Reminder").assertExists()
        onNodeWithText("In 30 minutes").assertExists()
        onNodeWithText("In 1 hour").assertExists()
        onNodeWithText("This evening").assertExists()
        onNodeWithText("Pick a time").assertExists()
        onNodeWithText("Off").assertExists()
        onNodeWithText("Every 30 min").assertExists()
        onNodeWithText("Save reminder").assertExists()
        onNodeWithText("Delete task").assertExists()
        onNodeWithText("Remove reminder").assertDoesNotExist()
    }

    @Test
    fun `a completed entry has no options button`() = runComposeUiTest {
        val e = entry()
        runBlocking { store.completeTask(e.id) }
        show()
        onNodeWithContentDescription("Options for Clean the kitchen").assertDoesNotExist()
    }

    @Test
    fun `an entry in progress has reminders but cannot be deleted`() = runComposeUiTest {
        val e = entry(subtasks = listOf("A", "B"))
        runBlocking { store.completeSubtask(e.subtasks[0].id) }
        show()
        openOptions()
        onNodeWithText("Save reminder").assertExists()
        onNodeWithText("Delete task").assertDoesNotExist()
    }

    @Test
    fun `choosing a preset and saving sets the reminder with every thirty minutes`() = runComposeUiTest {
        entry()
        show()
        openOptions()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()

        assertEquals(Reminder(preset("In 30 minutes").startAt, 30), entries().single().reminder)
        onNodeWithText("Save reminder").assertDoesNotExist()
        onNodeWithText("Reminder ${ReminderRules.clock(preset("In 30 minutes").startAt, zone)}, every 30 min").assertExists()
    }

    @Test
    fun `the evening preset and a different repeat are saved`() = runComposeUiTest {
        entry()
        show()
        openOptions()
        onNodeWithText("This evening").performClick()
        onNodeWithText("Every 1 h").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()

        assertEquals(Reminder(preset("This evening").startAt, 60), entries().single().reminder)
    }

    @Test
    fun `a repeat of off saves a single reminder`() = runComposeUiTest {
        entry()
        show()
        openOptions()
        onNodeWithText("In 1 hour").performClick()
        onNodeWithText("Off").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()

        assertEquals(Reminder(preset("In 1 hour").startAt, null), entries().single().reminder)
    }

    @Test
    fun `saving without a start asks for one`() = runComposeUiTest {
        entry()
        show()
        openOptions()
        onNodeWithText("Save reminder").performClick()
        onNodeWithText("Choose when the reminder starts").assertExists()
        assertNull(entries().single().reminder)
    }

    @Test
    fun `a picked time that has already passed is refused`() = runComposeUiTest {
        entry()
        show()
        openOptions()
        onNodeWithText("Pick a time").performClick()
        // the clock starts at the current time, which is not later than now
        onNodeWithText("OK").performClick()
        onNodeWithText("Save reminder").performClick()

        onNodeWithText("That time has already passed").assertExists()
        assertNull(entries().single().reminder)
    }

    @Test
    fun `an existing reminder is shown on the row and in the sheet and can be removed`() = runComposeUiTest {
        val e = entry()
        val reminder = Reminder(preset("In 1 hour").startAt, 30)
        runBlocking { store.setReminder(e.id, reminder) }
        show()

        val line = ReminderRules.summary(reminder, zone)
        onNodeWithText(line).assertExists()
        openOptions()
        onNodeWithText("Remove reminder").performClick()
        waitForIdle()

        assertNull(entries().single().reminder)
        onNodeWithText(line).assertDoesNotExist()
    }

    @Test
    fun `a refused permission still saves and explains`() = runComposeUiTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        entry()
        show(scheduler)
        openOptions()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Save reminder").performClick()

        onNodeWithText("Notifications are turned off for OpenAdhd. Turn them on in your device's settings to get reminders.").assertExists()
        assertTrue(entries().single().reminder != null)
        assertEquals(1, scheduler.permissionRequests)
    }

    @Test
    fun `delete task removes the entry and closes the sheet`() = runComposeUiTest {
        entry()
        show()
        openOptions()
        onNodeWithText("Delete task").performClick()
        waitForIdle()

        assertTrue(entries().isEmpty())
        onNodeWithText("Delete task").assertDoesNotExist()
        onNodeWithText("Nothing planned for today.").assertExists()
    }

    @Test
    fun `without a way to deliver reminders the sheet has only delete`() = runComposeUiTest {
        entry()
        show(null)
        openOptions()
        onNodeWithText("Delete task").assertExists()
        onNodeWithText("Save reminder").assertDoesNotExist()
        onNodeWithText("Reminder").assertDoesNotExist()
    }

    @Test
    fun `a saved reminder reaches the scheduler`() = runComposeUiTest {
        val scheduler = FakeReminderScheduler()
        entry()
        show(scheduler)
        openOptions()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()

        assertEquals(listOf("Clean the kitchen"), scheduler.current.map { it.text })
    }
}
