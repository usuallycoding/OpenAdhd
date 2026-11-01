package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.AlarmSound
import com.usuallycoding.openadhd.data.DesktopAlarmRinger
import com.usuallycoding.openadhd.data.DueAlarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopAlarmRingerTest {
    private val start = Instant.parse("2026-10-23T10:00:00Z")

    private class FakeSound : AlarmSound {
        val events = mutableListOf<String>()

        override fun start() {
            events += "start"
        }

        override fun stop() {
            events += "stop"
        }
    }

    /** Time passed asleep, which the wall clock counts and the test's virtual time does not. */
    private var slept = 0.minutes

    private fun TestScope.ringer(sound: FakeSound) = DesktopAlarmRinger(
        sound = sound,
        scope = backgroundScope,
        clock = object : Clock {
            override fun now(): Instant = start + testScheduler.currentTime.milliseconds + slept
        },
        zone = TimeZone.UTC,
    )

    private fun DesktopAlarmRinger.ids() = rows.value.map { it.entryId }

    @Test
    fun `an alarm adds a row and starts the sound`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))

        assertEquals(listOf("a"), ringer.ids())
        assertEquals("Take medication", ringer.rows.value.single().text)
        assertEquals(listOf("start"), sound.events)
    }

    @Test
    fun `a second task stacks and there is still one sound`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        ringer.ring(DueAlarm("b", "Taxes"))

        assertEquals(listOf("a", "b"), ringer.ids())
        assertEquals(listOf("start"), sound.events)
    }

    @Test
    fun `a repeat replaces its own row in place and starts its time again`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        ringer.ring(DueAlarm("b", "Taxes"))
        advanceTimeBy(3 * 60_000L)
        ringer.ring(DueAlarm("a", "Take medication"))

        assertEquals(listOf("a", "b"), ringer.ids())
        assertEquals(start + 3.minutes, ringer.rows.value.first().startedAt)
        assertEquals(listOf("start"), sound.events)
    }

    @Test
    fun `dismissing one row keeps the sound and dismissing the last stops it`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        ringer.ring(DueAlarm("b", "Taxes"))

        ringer.dismiss("a")
        assertEquals(listOf("b"), ringer.ids())
        assertEquals(listOf("start"), sound.events)
        ringer.dismiss("b")
        assertTrue(ringer.rows.value.isEmpty())
        assertEquals(listOf("start", "stop"), sound.events)
    }

    @Test
    fun `dismissing a row that is not ringing changes nothing`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.dismiss("nothing")
        ringer.ring(DueAlarm("a", "Take medication"))
        ringer.dismiss("other")
        assertEquals(listOf("a"), ringer.ids())
        assertEquals(listOf("start"), sound.events)
    }

    @Test
    fun `dismiss all clears every row and stops the sound once`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        ringer.ring(DueAlarm("b", "Taxes"))

        ringer.dismissAll()
        ringer.dismissAll()
        assertTrue(ringer.rows.value.isEmpty())
        assertEquals(listOf("start", "stop"), sound.events)
    }

    @Test
    fun `rows end ten minutes after they started, each on its own time`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        advanceTimeBy(4 * 60_000L)
        ringer.ring(DueAlarm("b", "Taxes"))

        advanceTimeBy(5 * 60_000L)
        assertEquals(listOf("a", "b"), ringer.ids())
        advanceTimeBy(2 * 60_000L)
        assertEquals(listOf("b"), ringer.ids())
        assertEquals(listOf("start"), sound.events)
        advanceTimeBy(4 * 60_000L)
        assertTrue(ringer.rows.value.isEmpty())
        assertEquals(listOf("start", "stop"), sound.events)
    }

    @Test
    fun `time asleep counts towards the ten minutes`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        advanceTimeBy(60_000L)

        slept = 30.minutes
        advanceTimeBy(10_000L)
        assertTrue(ringer.rows.value.isEmpty())
        assertEquals(listOf("start", "stop"), sound.events)
    }

    @Test
    fun `an alarm can ring again after the ring has ended`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        advanceTimeBy(11 * 60_000L)
        assertTrue(ringer.rows.value.isEmpty())

        ringer.ring(DueAlarm("a", "Take medication"))
        assertEquals(listOf("a"), ringer.ids())
        advanceTimeBy(11 * 60_000L)
        assertTrue(ringer.rows.value.isEmpty())
        assertEquals(listOf("start", "stop", "start", "stop"), sound.events)
    }

    @Test
    fun `finished tasks end only their own rows`() = runTest {
        val sound = FakeSound()
        val ringer = ringer(sound)
        ringer.ring(DueAlarm("a", "Take medication"))
        ringer.ring(DueAlarm("b", "Taxes"))

        ringer.finished(setOf("a", "unknown"))
        assertEquals(listOf("b"), ringer.ids())
        assertEquals(listOf("start"), sound.events)
    }

    @Test
    fun `a row keeps the time it was due and the day it started`() = runTest {
        val ringer = ringer(FakeSound())
        ringer.ring(DueAlarm("a", "Take medication", dueAt = start - 20.minutes))

        val row = ringer.rows.value.single()
        assertEquals(start - 20.minutes, row.dueAt)
        assertEquals("2026-10-23", row.day.toString())
    }
}
