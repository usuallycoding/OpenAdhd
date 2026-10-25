package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.DesktopReminderScheduler
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopReminderSchedulerTest {
    private val start = Instant.parse("2026-10-23T10:00:00Z")

    /** A clock that follows the test's virtual time, so waiting and the clock agree. */
    private fun kotlinx.coroutines.test.TestScope.clockOf() = object : kotlin.time.Clock {
        override fun now(): Instant = start + testScheduler.currentTime.let { kotlin.time.Duration.parse("${it}ms") }
    }

    private class Recorder {
        val shown = mutableListOf<String>()
        val pending = mutableListOf<Boolean>()
    }

    private fun kotlinx.coroutines.test.TestScope.scheduler(recorder: Recorder) = DesktopReminderScheduler(
        notify = { recorder.shown += it },
        onPending = { recorder.pending += it },
        scope = backgroundScope,
        clock = clockOf(),
    )

    @Test
    fun `each reminder is shown at its time in turn`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(
            listOf(
                UpcomingReminder("a", "First", listOf(start + 10.minutes, start + 40.minutes)),
                UpcomingReminder("b", "Second", listOf(start + 25.minutes)),
            ),
        )

        advanceTimeBy(9 * 60_000L)
        assertTrue(recorder.shown.isEmpty())
        advanceTimeBy(2 * 60_000L)
        assertEquals(listOf("First"), recorder.shown)
        advanceTimeBy(15 * 60_000L)
        assertEquals(listOf("First", "Second"), recorder.shown)
        advanceTimeBy(20 * 60_000L)
        assertEquals(listOf("First", "Second", "First"), recorder.shown)
    }

    @Test
    fun `a new schedule replaces the old one`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Old", listOf(start + 10.minutes))))
        scheduler.replaceAll(listOf(UpcomingReminder("b", "New", listOf(start + 20.minutes))))

        advanceTimeBy(30 * 60_000L)
        assertEquals(listOf("New"), recorder.shown)
    }

    @Test
    fun `an empty schedule shows nothing and says nothing is pending`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Task", listOf(start + 10.minutes))))
        scheduler.replaceAll(emptyList())

        advanceTimeBy(60 * 60_000L)
        assertTrue(recorder.shown.isEmpty())
        assertEquals(listOf(true, false), recorder.pending)
    }

    @Test
    fun `times already past are not shown`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Late", listOf(start - 5.minutes))))

        advanceTimeBy(60 * 60_000L)
        assertTrue(recorder.shown.isEmpty())
        assertEquals(listOf(false), recorder.pending)
    }

    @Test
    fun `pending turns off after the last reminder`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Task", listOf(start + 10.minutes))))
        advanceTimeBy(11 * 60_000L)
        assertEquals(listOf(true, false), recorder.pending)
    }

    @Test
    fun `permission is always there on the desktop`() = runTest {
        val scheduler = scheduler(Recorder())
        assertTrue(scheduler.hasPermission())
        assertTrue(scheduler.ensurePermission())
    }

    @Test
    fun `the desktop scheduler does not offer alarms and shows an alarm as a notification`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        assertEquals(false, scheduler.supportsAlarms)
        scheduler.replaceAll(
            listOf(UpcomingReminder("a", "Wake up", listOf(start + 10.minutes), com.usuallycoding.openadhd.domain.ReminderKind.Alarm)),
        )
        advanceTimeBy(11 * 60_000L)
        assertEquals(listOf("Wake up"), recorder.shown)
    }
}
