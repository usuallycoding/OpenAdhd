package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.TemplateReminder
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.today.ReminderSaveResult
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {
    private val clock = FakeClock(Instant.parse("2026-10-23T14:00:00Z"))
    private val store = InMemoryStore(clock)
    private val day = LocalDate(2026, 10, 23)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(scheduler: FakeReminderScheduler? = FakeReminderScheduler()) =
        TodayViewModel(store, clock, TimeZone.UTC, dateCheckInterval = null, reminders = scheduler)

    private suspend fun entry() = store.assign(store.create("Clean", Tier.Two, Energy.Low), day)

    private fun at(time: String) = Instant.parse("2026-10-23T$time:00Z")

    private fun TodayViewModel.save(id: String, start: Instant, repeat: Int?): ReminderSaveResult {
        var result: ReminderSaveResult? = null
        saveReminder(id, start, repeat) { result = it }
        return result!!
    }

    @Test
    fun `reminders are available only where a scheduler exists`() = runTest {
        assertTrue(viewModel().canRemind)
        assertFalse(viewModel(null).canRemind)
    }

    @Test
    fun `a valid reminder is saved on the entry`() = runTest {
        val vm = viewModel()
        val e = entry()

        assertEquals(ReminderSaveResult.Saved, vm.save(e.id, at("15:00"), 30))

        assertEquals(Reminder(at("15:00"), 30), store.entriesFor(day).first().single().reminder)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a time that has passed is refused and nothing is saved`() = runTest {
        val vm = viewModel()
        val e = entry()
        val result = vm.save(e.id, at("13:00"), 30)
        assertEquals("That time has already passed", assertIs<ReminderSaveResult.Refused>(result).message)
        assertNull(store.entriesFor(day).first().single().reminder)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a time after the stop hour is refused with the hour`() = runTest {
        val vm = viewModel()
        val e = entry()
        val result = vm.save(e.id, at("22:30"), null)
        assertEquals("Reminders stop at 22:00, so choose an earlier time", assertIs<ReminderSaveResult.Refused>(result).message)

        store.saveSetting(SettingKey.ReminderStopHour, "23")
        assertEquals(ReminderSaveResult.Saved, vm.save(e.id, at("22:30"), null))
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a refused permission still saves the reminder and says notifications are off`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        val vm = viewModel(scheduler)
        val e = entry()

        assertEquals(ReminderSaveResult.SavedNotificationsOff, vm.save(e.id, at("15:00"), 30))

        assertEquals(1, scheduler.permissionRequests)
        assertEquals(Reminder(at("15:00"), 30), store.entriesFor(day).first().single().reminder)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `permission is asked when a reminder is saved and not before`() = runTest {
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = true)
        val vm = viewModel(scheduler)
        assertEquals(0, scheduler.permissionRequests)

        val e = entry()
        assertEquals(ReminderSaveResult.Saved, vm.save(e.id, at("15:00"), null))
        assertEquals(1, scheduler.permissionRequests)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a completed entry cannot get a reminder`() = runTest {
        val vm = viewModel()
        val e = entry()
        store.completeTask(e.id)
        assertIs<ReminderSaveResult.Refused>(vm.save(e.id, at("15:00"), 30))
        vm.viewModelScope.cancel()
    }

    @Test
    fun `clearing removes the reminder`() = runTest {
        val vm = viewModel()
        val e = entry()
        vm.save(e.id, at("15:00"), 30)
        vm.clearReminder(e.id)
        assertNull(store.entriesFor(day).first().single().reminder)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `presets follow the clock and the stop hour`() = runTest {
        val vm = viewModel()
        assertEquals(listOf("14:30", "15:00", "18:00"), vm.reminderPresets().map { com.usuallycoding.openadhd.domain.ReminderRules.clock(it.startAt, TimeZone.UTC) })
        assertTrue(vm.reminderPresets().all { it.available })

        store.saveSetting(SettingKey.ReminderStopHour, "15")
        assertEquals(listOf(true, false, false), vm.reminderPresets().map { it.available })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a time picked on the clock is today in the device zone`() = runTest {
        val vm = viewModel()
        assertEquals(at("16:45"), vm.reminderStartAt(16, 45))
        assertEquals(14, vm.currentTime().hour)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `adding a template from the plus button gives the entry its reminder`() = runTest {
        val template = store.create("Meds", Tier.One, Energy.Low, reminder = TemplateReminder(15 * 60, 60))
        val vm = viewModel()
        vm.assign(template)
        val entry = store.entriesFor(day).first().single()
        assertEquals(Reminder(at("15:00"), 60), entry.reminder)
        vm.viewModelScope.cancel()
    }
}
