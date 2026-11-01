package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.DesktopNotifier
import com.usuallycoding.openadhd.data.DesktopReminderGrace
import com.usuallycoding.openadhd.data.DesktopReminderScheduler
import com.usuallycoding.openadhd.data.DesktopReminderTick
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopReminderSchedulerTest {
    private val start = Instant.parse("2026-10-23T10:00:00Z")

    /** The time the computer spent asleep, which the wall clock counts and the test's virtual time does not. */
    private var slept = 0.minutes

    /** A clock that follows the test's virtual time plus [slept], so waiting and the clock agree until the computer sleeps. */
    private fun kotlinx.coroutines.test.TestScope.clockOf() = object : kotlin.time.Clock {
        override fun now(): Instant = start + testScheduler.currentTime.milliseconds + slept
    }

    private class Recorder {
        val shown = mutableListOf<String>()
    }

    private fun kotlinx.coroutines.test.TestScope.scheduler(recorder: Recorder) = DesktopReminderScheduler(
        notify = { recorder.shown += it },
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
    fun `an empty schedule shows nothing`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Task", listOf(start + 10.minutes))))
        scheduler.replaceAll(emptyList())

        advanceTimeBy(60 * 60_000L)
        assertTrue(recorder.shown.isEmpty())
    }

    @Test
    fun `times already past are not shown`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Late", listOf(start - 5.minutes))))

        advanceTimeBy(60 * 60_000L)
        assertTrue(recorder.shown.isEmpty())
    }

    @Test
    fun `a reminder is shown on waking when the computer slept through it by a little`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Task", listOf(start + 5.minutes))))

        advanceTimeBy(60_000L)
        slept = 5.minutes
        advanceTimeBy(DesktopReminderTick.inWholeMilliseconds)
        assertEquals(listOf("Task"), recorder.shown)
    }

    @Test
    fun `a reminder the computer slept through by long is dropped and the next one fires on time`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(
            listOf(UpcomingReminder("a", "Task", listOf(start + 5.minutes, start + 30.minutes))),
        )

        advanceTimeBy(60_000L)
        slept = 20.minutes
        advanceTimeBy(DesktopReminderTick.inWholeMilliseconds)
        assertTrue(recorder.shown.isEmpty())
        advanceTimeBy(7 * 60_000L)
        assertTrue(recorder.shown.isEmpty())
        advanceTimeBy(3 * 60_000L)
        assertEquals(listOf("Task"), recorder.shown)
    }

    @Test
    fun `a reminder is shown at its time and not twice`() = runTest {
        val recorder = Recorder()
        val scheduler = scheduler(recorder)
        scheduler.replaceAll(listOf(UpcomingReminder("a", "Task", listOf(start + 90.seconds))))

        advanceTimeBy(89_000L)
        assertTrue(recorder.shown.isEmpty())
        advanceTimeBy(2_000L)
        advanceTimeBy(10 * 60_000L)
        assertEquals(listOf("Task"), recorder.shown)
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

    @Test
    fun `the tick is shorter than the grace so a slept-through reminder is still inside it`() {
        assertTrue(DesktopReminderTick < DesktopReminderGrace)
    }

    @Test
    fun `a notification goes to the tray when there is one`() {
        val tray = mutableListOf<Pair<String, String>>()
        val fallback = mutableListOf<String>()
        DesktopNotifier({ title, text -> tray += title to text }) { fallback += it }.show("Taxes")
        assertEquals(listOf("Still to do" to "Taxes"), tray)
        assertTrue(fallback.isEmpty())
    }

    @Test
    fun `a notification becomes an in-app message when there is no tray`() {
        val fallback = mutableListOf<String>()
        DesktopNotifier(null) { fallback += it }.show("Taxes")
        assertEquals(listOf("Taxes"), fallback)
    }
}
