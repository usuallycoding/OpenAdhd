package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.ReminderCoordinator
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TemplateReminder
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

    // ---- templates whose entries do not exist yet

    private suspend fun dailyTemplate(time: Int = 17 * 60 + 30, repeat: Int? = null, kind: ReminderKind = ReminderKind.Notification) =
        store.create("Water plants", Tier.One, Energy.Low, schedule = TaskSchedule.Daily, reminder = TemplateReminder(time, repeat, kind))

    private fun entryIds(): List<String> = scheduler.current.map { it.entryId }

    @Test
    fun `a daily template is armed for today and the days ahead before its entry exists`() = runTest {
        start()
        val template = dailyTemplate()

        assertEquals(7, scheduler.current.size)
        assertEquals("template:${template.id}:2026-10-23", entryIds().first())
        assertEquals("Water plants", scheduler.current.first().text)
        assertEquals(at("17:30"), scheduler.current.first().times.single())
        assertEquals(at("17:30", "2026-10-29"), scheduler.current.last().times.single())
    }

    @Test
    fun `an alarm template hands over its kind`() = runTest {
        start()
        dailyTemplate(kind = ReminderKind.Alarm)
        assertTrue(scheduler.current.all { it.kind == ReminderKind.Alarm })
    }

    @Test
    fun `once the entry is generated only its own reminder is left for that day`() = runTest {
        start()
        val template = dailyTemplate()
        store.generateDue(day, TimeZone.UTC)

        val generated = store.entriesFor(day).first().single()
        assertEquals(7, scheduler.current.size)
        assertEquals(generated.id, entryIds().first())
        assertEquals(listOf(at("17:30")), scheduler.current.first().times)
        assertEquals(6, entryIds().count { it.startsWith("template:${template.id}:") })
        assertEquals(1, scheduler.current.count { it.times.contains(at("17:30")) })
    }

    @Test
    fun `completing or deleting the generated entry leaves nothing for that day`() = runTest {
        start()
        dailyTemplate()
        store.generateDue(day, TimeZone.UTC)
        val generated = store.entriesFor(day).first().single()

        store.completeTask(generated.id)
        assertEquals(6, scheduler.current.size)
        assertTrue(entryIds().none { it.endsWith("2026-10-23") })

        store.uncompleteTask(generated.id)
        assertEquals(7, scheduler.current.size)
        store.remove(generated.id)
        assertEquals(6, scheduler.current.size)
        assertTrue(entryIds().none { it.endsWith("2026-10-23") })
    }

    @Test
    fun `editing the template re-plans and removing its schedule stops the reminders`() = runTest {
        start()
        val template = dailyTemplate()

        store.update(template.copy(reminder = TemplateReminder(20 * 60)))
        assertEquals(at("20:00"), scheduler.current.first().times.single())

        store.update(template.copy(reminder = TemplateReminder(20 * 60), schedule = TaskSchedule.None))
        assertTrue(scheduler.current.isEmpty())
    }

    @Test
    fun `the stop hour limits the planned repeats`() = runTest {
        start()
        dailyTemplate(time = 9 * 60, repeat = 60)
        store.saveSetting(SettingKey.ReminderStopHour, "12")
        val tomorrow = scheduler.current.first { it.entryId.endsWith("2026-10-24") }
        assertEquals(listOf("09:00", "10:00", "11:00"), tomorrow.times.map { ReminderRules.clock(it, TimeZone.UTC) })
    }

    @Test
    fun `an unchanged plan is not handed over again`() = runTest {
        start()
        dailyTemplate()
        val count = scheduler.schedules.size
        store.create("Unrelated", Tier.One, Energy.Low)
        assertEquals(count, scheduler.schedules.size)
    }

    @Test
    fun `a platform that plans 30 days gets 30 days of a daily template`() = runTest {
        val long = FakeReminderScheduler(planDays = 30)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ReminderCoordinator(store, long, clock, today, TimeZone.UTC).run()
        }
        dailyTemplate()
        assertEquals(30, long.current.size)
        assertEquals(at("17:30", "2026-11-21"), long.current.last().times.single())
    }
}
