package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.ReminderCoordinator
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderCoordinatorTest {
    private val clock = FakeClock(Instant.parse("2026-10-23T16:10:00Z"))
    private val store = InMemoryStore(clock)
    private val scheduler = FakeReminderScheduler()
    private val day = LocalDate(2026, 10, 23)
    private val today = MutableStateFlow(day)

    private fun at(time: String, date: String = "2026-10-23") = Instant.parse("${date}T$time:00Z")

    private fun clocks(): List<String> = scheduler.current.flatMap { r -> r.times.map { ReminderRules.clock(it, TimeZone.UTC) } }

    private fun kotlinx.coroutines.test.TestScope.start() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ReminderCoordinator(store, scheduler, clock, today, TimeZone.UTC).run()
        }
    }

    private suspend fun entry(name: String = "Clean the kitchen", subtasks: List<String> = emptyList()) =
        store.assign(store.create(name, Tier.Two, Energy.Low, subtasks = subtasks), day)

    @Test
    fun `with no reminders an empty schedule is handed over`() = runTest {
        start()
        assertEquals(listOf(emptyList()), scheduler.schedules)
    }

    @Test
    fun `a reminder is handed over with its name and the times still to come`() = runTest {
        start()
        val e = entry()
        store.setReminder(e.id, Reminder(at("15:00"), 30))

        val reminder = scheduler.current.single()
        assertEquals(e.id, reminder.entryId)
        assertEquals("Clean the kitchen", reminder.text)
        assertEquals("16:30", ReminderRules.clock(reminder.times.first(), TimeZone.UTC))
        assertEquals("21:30", ReminderRules.clock(reminder.times.last(), TimeZone.UTC))
    }

    @Test
    fun `completing stops the reminders and unchecking brings them back`() = runTest {
        start()
        val e = entry()
        store.setReminder(e.id, Reminder(at("17:00"), 60))
        assertEquals(1, scheduler.current.size)

        store.completeTask(e.id)
        assertTrue(scheduler.current.isEmpty())

        store.uncompleteTask(e.id)
        assertEquals(listOf("17:00", "18:00", "19:00", "20:00", "21:00"), clocks())
    }

    @Test
    fun `an entry in progress keeps its reminders until it is done`() = runTest {
        start()
        val e = entry(subtasks = listOf("A", "B"))
        store.setReminder(e.id, Reminder(at("17:00"), null))

        store.completeSubtask(e.subtasks[0].id)
        assertEquals(1, scheduler.current.size)

        store.completeSubtask(e.subtasks[1].id)
        assertTrue(scheduler.current.isEmpty())
    }

    @Test
    fun `removing the reminder or the entry removes it from the schedule`() = runTest {
        start()
        val first = entry("First")
        val second = entry("Second")
        store.setReminder(first.id, Reminder(at("17:00"), null))
        store.setReminder(second.id, Reminder(at("18:00"), null))
        assertEquals(2, scheduler.current.size)

        store.setReminder(first.id, null)
        assertEquals(listOf(second.id), scheduler.current.map { it.entryId })

        store.remove(second.id)
        assertTrue(scheduler.current.isEmpty())
    }

    @Test
    fun `a changed stop hour changes the times`() = runTest {
        start()
        val e = entry()
        store.setReminder(e.id, Reminder(at("17:00"), 60))
        assertEquals("21:00", clocks().last())

        store.saveSetting(SettingKey.ReminderStopHour, "19")
        assertEquals(listOf("17:00", "18:00"), clocks())
    }

    @Test
    fun `times already past at start are not scheduled`() = runTest {
        val e = entry()
        store.setReminder(e.id, Reminder(at("17:00"), null))
        clock.current = at("17:30")

        start()
        assertTrue(scheduler.current.isEmpty())
    }

    @Test
    fun `reminders of another day are left out and a new day starts empty`() = runTest {
        start()
        val e = entry()
        store.setReminder(e.id, Reminder(at("17:00"), null))
        assertEquals(1, scheduler.current.size)

        clock.current = at("00:05", "2026-10-24")
        today.value = LocalDate(2026, 10, 24)
        assertTrue(scheduler.current.isEmpty())
    }

    @Test
    fun `an imported backup is scheduled`() = runTest {
        val source = InMemoryStore(clock)
        val e = source.assign(source.create("Imported", Tier.One, Energy.Low), day)
        source.setReminder(e.id, Reminder(at("18:00"), null))
        start()

        store.importData(source.exportData())

        assertEquals(listOf("Imported"), scheduler.current.map { it.text })
        assertEquals(listOf("18:00"), clocks())
    }

    @Test
    fun `an unchanged schedule is not handed over again`() = runTest {
        start()
        val e = entry()
        store.setReminder(e.id, Reminder(at("17:00"), null))
        val count = scheduler.schedules.size
        store.create("Unrelated", Tier.One, Energy.Low)
        assertEquals(count, scheduler.schedules.size)
    }

    @Test
    fun `the fake scheduler asks for permission only when it is missing`() = runTest {
        val denied = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        assertEquals(false, denied.ensurePermission())
        assertEquals(1, denied.permissionRequests)
        val ok = FakeReminderScheduler(granted = true)
        assertEquals(true, ok.ensurePermission())
        assertEquals(0, ok.permissionRequests)
    }

    @Test
    fun `the kind of a reminder is handed over and changing only the kind hands the schedule over again`() = runTest {
        start()
        val e = entry()
        store.setReminder(e.id, Reminder(at("17:00"), 60))
        assertEquals(ReminderKind.Notification, scheduler.current.single().kind)

        val count = scheduler.schedules.size
        store.setReminder(e.id, Reminder(at("17:00"), 60, ReminderKind.Alarm))
        assertEquals(count + 1, scheduler.schedules.size)
        assertEquals(ReminderKind.Alarm, scheduler.current.single().kind)
        assertEquals(clocks(), listOf("17:00", "18:00", "19:00", "20:00", "21:00"))
    }
}
