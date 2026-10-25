package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.ScheduleProblem
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TemplateReminder
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateScheduleViewModelTest {
    private val store = InMemoryStore()
    private val day = LocalDate(2026, 3, 3)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `each schedule mode is saved with its reminder`() = runTest {
        val vm = TemplatesViewModel(store)
        val schedules = listOf(
            TaskSchedule.None,
            TaskSchedule.OnDate(day),
            TaskSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            TaskSchedule.Period(day, LocalDate(2026, 3, 7)),
            TaskSchedule.Daily,
        )
        schedules.forEach { schedule ->
            vm.save(null, "T", Tier.One, Energy.Low, schedule = schedule, reminder = TemplateReminder(540, 60))
            val saved = vm.templates.value.first()
            assertEquals(schedule, saved.schedule)
            assertEquals(TemplateReminder(540, 60), saved.reminder)
        }
    }

    @Test
    fun `a weekly schedule without a day and a backwards period are not saved`() = runTest {
        val vm = TemplatesViewModel(store)
        vm.save(null, "T", Tier.One, Energy.Low, schedule = TaskSchedule.Weekly(emptySet()))
        vm.save(null, "T", Tier.One, Energy.Low, schedule = TaskSchedule.Period(day, day))
        assertTrue(vm.templates.value.isEmpty())
        assertEquals(ScheduleProblem.NoWeekday, TemplatesViewModel.validateSchedule(TaskSchedule.Weekly(emptySet()), null))
        assertEquals(ScheduleProblem.PeriodOrder, TemplatesViewModel.validateSchedule(TaskSchedule.Period(day, day), null))
        assertNull(TemplatesViewModel.validateSchedule(TaskSchedule.Daily, TemplateReminder(540, null)))
    }

    @Test
    fun `editing a template can change and clear its schedule and reminder`() = runTest {
        val vm = TemplatesViewModel(store)
        vm.save(null, "T", Tier.One, Energy.Low, schedule = TaskSchedule.Daily, reminder = TemplateReminder(540, 30))
        val id = vm.templates.value.single().id
        vm.save(id, "T", Tier.One, Energy.Low)
        val edited = vm.templates.value.single()
        assertEquals(TaskSchedule.None, edited.schedule)
        assertNull(edited.reminder)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateReminderPermissionTest {
    private val store = InMemoryStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun notices(vm: TemplatesViewModel): MutableList<String> {
        val seen = mutableListOf<String>()
        vm.viewModelScope.launch(UnconfinedTestDispatcher()) { vm.notices.collect { seen += it } }
        return seen
    }

    @Test
    fun `saving a template reminder asks for permission once and a refusal still saves it`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        val vm = TemplatesViewModel(store, reminders = scheduler)
        val seen = notices(vm)
        vm.save(null, "Meds", Tier.One, Energy.Low, schedule = TaskSchedule.Daily, reminder = TemplateReminder(540, 30))

        assertEquals(1, scheduler.permissionRequests)
        assertEquals(TemplateReminder(540, 30), vm.templates.value.single().reminder)
        assertEquals(listOf(TemplatesViewModel.NotificationsOff), seen)
    }

    @Test
    fun `a granted permission shows no notice and a later save asks nothing more`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = true)
        val vm = TemplatesViewModel(store, reminders = scheduler)
        val seen = notices(vm)
        vm.save(null, "Meds", Tier.One, Energy.Low, reminder = TemplateReminder(540, null))
        vm.save(null, "More", Tier.One, Energy.Low, reminder = TemplateReminder(600, null))

        assertEquals(1, scheduler.permissionRequests)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun `saving without a reminder time asks nothing`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        val vm = TemplatesViewModel(store, reminders = scheduler)
        val seen = notices(vm)
        vm.save(null, "Plain", Tier.One, Energy.Low, schedule = TaskSchedule.Daily)

        assertEquals(0, scheduler.permissionRequests)
        assertTrue(seen.isEmpty())
        assertEquals(1, vm.templates.value.size)
    }

    @Test
    fun `without a scheduler nothing is asked and nothing is shown`() = runTest {
        val vm = TemplatesViewModel(store, reminders = null)
        val seen = notices(vm)
        vm.save(null, "Meds", Tier.One, Energy.Low, reminder = TemplateReminder(540, null))

        assertTrue(seen.isEmpty())
        assertEquals(TemplateReminder(540, null), vm.templates.value.single().reminder)
    }

    @Test
    fun `an invalid template asks nothing`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        val vm = TemplatesViewModel(store, reminders = scheduler)
        vm.save(null, "  ", Tier.One, Energy.Low, reminder = TemplateReminder(540, null))
        assertEquals(0, scheduler.permissionRequests)
    }

    @Test
    fun `editing a template with a reminder asks as well`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        val vm = TemplatesViewModel(store, reminders = scheduler)
        val seen = notices(vm)
        vm.save(null, "Meds", Tier.One, Energy.Low)
        vm.save(vm.templates.value.single().id, "Meds", Tier.One, Energy.Low, reminder = TemplateReminder(540, 15))
        assertEquals(1, scheduler.permissionRequests)
        assertEquals(1, seen.size)
    }

    @Test
    fun `an alarm template without full screen permission saves and shows its own notice`() = runTest {
        val scheduler = FakeReminderScheduler(supportsAlarms = true, fullScreenAllowed = false)
        val vm = TemplatesViewModel(store, reminders = scheduler)
        val seen = notices(vm)
        vm.save(null, "Meds", Tier.One, Energy.Low, reminder = TemplateReminder(540, null, com.usuallycoding.openadhd.domain.ReminderKind.Alarm))
        assertEquals(listOf(TemplatesViewModel.FullScreenOff), seen)
        assertEquals(1, vm.templates.value.size)
    }

    @Test
    fun `a notification template or an allowed alarm shows no full screen notice`() = runTest {
        val denied = FakeReminderScheduler(supportsAlarms = true, fullScreenAllowed = false)
        val first = TemplatesViewModel(store, reminders = denied)
        val seenFirst = notices(first)
        first.save(null, "Plain", Tier.One, Energy.Low, reminder = TemplateReminder(540, null))
        assertTrue(seenFirst.isEmpty())

        val allowed = FakeReminderScheduler(supportsAlarms = true, fullScreenAllowed = true)
        val second = TemplatesViewModel(store, reminders = allowed)
        val seenSecond = notices(second)
        second.save(null, "Loud", Tier.One, Energy.Low, reminder = TemplateReminder(540, null, com.usuallycoding.openadhd.domain.ReminderKind.Alarm))
        assertTrue(seenSecond.isEmpty())
    }

    @Test
    fun `a refused notification permission shows only the notifications notice for an alarm`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false, supportsAlarms = true, fullScreenAllowed = false)
        val vm = TemplatesViewModel(store, reminders = scheduler)
        val seen = notices(vm)
        vm.save(null, "Meds", Tier.One, Energy.Low, reminder = TemplateReminder(540, null, com.usuallycoding.openadhd.domain.ReminderKind.Alarm))
        assertEquals(listOf(TemplatesViewModel.NotificationsOff), seen)
    }
}
