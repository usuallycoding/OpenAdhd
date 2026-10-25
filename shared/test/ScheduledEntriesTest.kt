package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.ScheduledEntries
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TemplateReminder
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduledEntriesTest {
    private val clock = FakeClock(Instant.parse("2026-10-23T08:00:00Z"))
    private val store = InMemoryStore(clock)
    private val friday = LocalDate(2026, 10, 23)
    private val saturday = LocalDate(2026, 10, 24)
    private val today = MutableStateFlow(friday)

    private fun kotlinx.coroutines.test.TestScope.start() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { ScheduledEntries(store, today, TimeZone.UTC).run() }
    }

    private suspend fun names(date: LocalDate) = store.entriesFor(date).first().map { it.name }

    @Test
    fun `a due template is added when the app starts`() = runTest {
        store.create("Laundry", Tier.One, Energy.Low, schedule = TaskSchedule.Weekly(setOf(DayOfWeek.FRIDAY)))
        start()
        assertEquals(listOf("Laundry"), names(friday))
    }

    @Test
    fun `a template that is not due adds nothing and starting again adds no second entry`() = runTest {
        store.create("Mondays", Tier.One, Energy.Low, schedule = TaskSchedule.Weekly(setOf(DayOfWeek.MONDAY)))
        store.create("Daily", Tier.One, Energy.Low, schedule = TaskSchedule.Daily)
        start()
        start()
        assertEquals(listOf("Daily"), names(friday))
    }

    @Test
    fun `the date changing while running generates the new day`() = runTest {
        store.create("Daily", Tier.One, Energy.Low, schedule = TaskSchedule.Daily)
        start()
        assertTrue(names(saturday).isEmpty())
        today.value = saturday
        assertEquals(listOf("Daily"), names(saturday))
        assertEquals(listOf("Daily"), names(friday))
    }

    @Test
    fun `a new daily template appears at once`() = runTest {
        start()
        assertTrue(names(friday).isEmpty())
        store.create("Daily", Tier.One, Energy.Low, schedule = TaskSchedule.Daily, reminder = TemplateReminder(9 * 60, 30))
        assertEquals(listOf("Daily"), names(friday))
        assertEquals(30, store.entriesFor(friday).first().single().reminder?.repeatMinutes)
    }

    @Test
    fun `a template edited to include today is added once`() = runTest {
        val template = store.create("Weekly", Tier.One, Energy.Low, schedule = TaskSchedule.Weekly(setOf(DayOfWeek.SATURDAY)))
        start()
        assertTrue(names(friday).isEmpty())
        store.update(template.copy(schedule = TaskSchedule.Weekly(setOf(DayOfWeek.SATURDAY, DayOfWeek.FRIDAY))))
        store.update(store.templates.first().single().copy(description = "edited again"))
        assertEquals(listOf("Weekly"), names(friday))
    }

    @Test
    fun `a deleted entry does not come back after an edit`() = runTest {
        val template = store.create("Daily", Tier.One, Energy.Low, schedule = TaskSchedule.Daily)
        start()
        store.remove(store.entriesFor(friday).first().single().id)
        store.update(template.copy(name = "Renamed"))
        assertTrue(names(friday).isEmpty())
    }

    @Test
    fun `missed days are not filled in`() = runTest {
        store.create("Daily", Tier.One, Energy.Low, schedule = TaskSchedule.Daily)
        today.value = LocalDate(2026, 10, 27)
        start()
        assertEquals(setOf(LocalDate(2026, 10, 27)), store.daysWithEntries.first())
    }
}
