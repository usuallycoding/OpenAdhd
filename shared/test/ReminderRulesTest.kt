package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderProblem
import com.usuallycoding.openadhd.domain.ReminderRules
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ReminderRulesTest {
    private val utc = TimeZone.UTC
    private val day = LocalDate(2026, 10, 24)

    private fun at(time: String) = Instant.parse("2026-10-24T${time}:00Z")

    private fun times(reminder: Reminder, now: String = "08:00", stop: Int = 22 * 60, zone: TimeZone = utc) =
        ReminderRules.upcoming(reminder, day, stop, at(now), zone).map { ReminderRules.clock(it, zone) }

    @Test
    fun `a start in the future before the stop time is accepted`() {
        assertNull(ReminderRules.check(at("15:30"), at("15:00"), day, 22 * 60, utc))
        assertNull(ReminderRules.check(at("21:55"), at("15:00"), day, 22 * 60, utc))
    }

    @Test
    fun `a start that has passed is refused`() {
        assertEquals(ReminderProblem.Passed, ReminderRules.check(at("14:00"), at("15:00"), day, 22 * 60, utc))
        assertEquals(ReminderProblem.Passed, ReminderRules.check(at("15:00"), at("15:00"), day, 22 * 60, utc))
        assertEquals("That time has already passed", ReminderProblem.Passed.message)
    }

    @Test
    fun `a start at or after the stop time is refused and says when reminders stop`() {
        val late = ReminderRules.check(at("22:30"), at("15:00"), day, 22 * 60, utc)
        assertEquals(ReminderProblem.AfterStop(22 * 60), late)
        assertEquals(ReminderProblem.AfterStop(22 * 60), ReminderRules.check(at("22:00"), at("15:00"), day, 22 * 60, utc))
        assertEquals("Reminders stop at 22:00, so choose an earlier time", late!!.message)
        assertEquals("Reminders stop at 08:00, so choose an earlier time", ReminderProblem.AfterStop(8 * 60).message)
    }

    @Test
    fun `a start on another day is refused`() {
        val tomorrow = Instant.parse("2026-10-25T09:00:00Z")
        assertEquals(ReminderProblem.NotToday, ReminderRules.check(tomorrow, at("15:00"), day, 22 * 60, utc))
    }

    @Test
    fun `presets are thirty minutes and an hour and the evening`() {
        val presets = ReminderRules.presets(at("14:00"), day, 22 * 60, utc)
        assertEquals(listOf("In 30 minutes", "In 1 hour", "This evening"), presets.map { it.label })
        assertEquals(listOf("14:30", "15:00", "18:00"), presets.map { ReminderRules.clock(it.startAt, utc) })
        assertTrue(presets.all { it.available })
    }

    @Test
    fun `a preset that cannot work is unavailable`() {
        val afterSix = ReminderRules.presets(at("19:00"), day, 22 * 60, utc)
        assertEquals(listOf(true, true, false), afterSix.map { it.available })

        val nearStop = ReminderRules.presets(at("21:20"), day, 22 * 60, utc)
        assertEquals(listOf(true, false, false), nearStop.map { it.available })
        assertEquals(listOf(false, false, false), ReminderRules.presets(at("21:40"), day, 22 * 60, utc).map { it.available })
    }

    @Test
    fun `a preset counts from the start of the current minute`() {
        val now = Instant.parse("2026-10-24T14:00:40Z")
        assertEquals("14:30", ReminderRules.clock(ReminderRules.presets(now, day, 22 * 60, utc).first().startAt, utc))
    }

    @Test
    fun `a repeating reminder fires until the stop time and not at it`() {
        val reminder = Reminder(at("15:00"), 30)
        val fired = times(reminder)
        assertEquals("15:00", fired.first())
        assertEquals("21:30", fired.last())
        assertEquals(14, fired.size)
        assertTrue("22:00" !in fired)
    }

    @Test
    fun `a reminder without a repeat fires once`() {
        assertEquals(listOf("15:00"), times(Reminder(at("15:00"), null)))
    }

    @Test
    fun `a start just before the stop time fires once even with a repeat`() {
        assertEquals(listOf("21:55"), times(Reminder(at("21:55"), 30)))
    }

    @Test
    fun `times already past are not included`() {
        val fired = times(Reminder(at("15:00"), 30), now = "16:10")
        assertEquals("16:30", fired.first())
        assertTrue("16:00" !in fired)
        assertEquals(listOf("21:00", "21:30").takeLast(2), fired.takeLast(2))
    }

    @Test
    fun `nothing fires once the stop time has passed`() {
        assertTrue(times(Reminder(at("15:00"), 30), now = "22:05").isEmpty())
    }

    @Test
    fun `a stop time with minutes accepts a start before it and refuses one at it`() {
        val stop = 22 * 60 + 30
        assertNull(ReminderRules.check(at("22:15"), at("15:00"), day, stop, utc))
        val atStop = ReminderRules.check(at("22:30"), at("15:00"), day, stop, utc)
        assertEquals(ReminderProblem.AfterStop(stop), atStop)
        assertEquals("Reminders stop at 22:30, so choose an earlier time", atStop!!.message)
    }

    @Test
    fun `repeats stop before a stop time with minutes`() {
        val fired = times(Reminder(at("21:00"), 30), stop = 22 * 60 + 15)
        assertEquals(listOf("21:00", "21:30", "22:00"), fired)
    }

    @Test
    fun `a changed stop time cuts the times`() {
        val fired = times(Reminder(at("15:00"), 60), stop = 18 * 60)
        assertEquals(listOf("15:00", "16:00", "17:00"), fired)
    }

    @Test
    fun `every repeat choice steps by its own interval`() {
        for (minutes in ReminderRules.RepeatChoices) {
            val fired = ReminderRules.upcoming(Reminder(at("10:00"), minutes), day, 22 * 60, at("08:00"), utc)
            assertEquals(minutes * 60_000L, (fired[1] - fired[0]).inWholeMilliseconds)
        }
    }

    @Test
    fun `times follow the device zone`() {
        val bucharest = TimeZone.of("Europe/Bucharest") // UTC+3 on this date
        val start = Instant.parse("2026-10-24T12:00:00Z") // 15:00 local
        assertNull(ReminderRules.check(start, Instant.parse("2026-10-24T11:00:00Z"), day, 22 * 60, bucharest))
        val fired = ReminderRules.upcoming(Reminder(start, 60), day, 22 * 60, Instant.parse("2026-10-24T05:00:00Z"), bucharest)
        assertEquals("15:00", ReminderRules.clock(fired.first(), bucharest))
        assertEquals("21:00", ReminderRules.clock(fired.last(), bucharest))
    }

    @Test
    fun `the summary shows the start and the repeat`() {
        assertEquals("Reminder 15:00, every 30 min", ReminderRules.summary(Reminder(at("15:00"), 30), utc))
        assertEquals("Reminder 09:05, every 1 h", ReminderRules.summary(Reminder(at("09:05"), 60), utc))
        assertEquals("Reminder 09:05, every 2 h", ReminderRules.summary(Reminder(at("09:05"), 120), utc))
        assertEquals("Reminder 15:00", ReminderRules.summary(Reminder(at("15:00"), null), utc))
    }

    @Test
    fun `an alarm is named as one in the summary and a notification is the default`() {
        assertEquals("Alarm 15:00, every 30 min", ReminderRules.summary(Reminder(at("15:00"), 30, ReminderKind.Alarm), utc))
        assertEquals("Alarm 09:05", ReminderRules.summary(Reminder(at("09:05"), null, ReminderKind.Alarm), utc))
        assertEquals(ReminderKind.Notification, Reminder(at("15:00"), 30).kind)
        assertEquals("Reminder 15:00", ReminderRules.summary(Reminder(at("15:00"), null, ReminderKind.Notification), utc))
    }
}
