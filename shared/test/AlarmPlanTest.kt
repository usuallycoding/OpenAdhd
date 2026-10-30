package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.AlarmPlan
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ScheduledTime
import com.usuallycoding.openadhd.domain.TemplateReminders
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class AlarmPlanTest {
    private fun alarm(at: Long) = ScheduledTime("e", "Task", at, ReminderKind.Alarm)

    private fun note(at: Long) = ScheduledTime("e", "Task", at, ReminderKind.Notification)

    @Test
    fun `the limits are twenty alarms hundred notifications and ten minutes of ringing`() {
        assertEquals(20, AlarmPlan.MaxArmedAlarms)
        assertEquals(100, AlarmPlan.MaxArmedNotifications)
        assertEquals(10.minutes, AlarmPlan.RingLimit)
    }

    @Test
    fun `only the soonest twenty alarm times are armed`() {
        val items = (25 downTo 1).map { alarm(it.toLong()) }
        val armed = AlarmPlan.arm(items)
        assertEquals((1L..20L).toList(), armed.map { it.atMillis })
    }

    @Test
    fun `notifications are never counted against the alarm limit`() {
        val items = (1..20).map { alarm(it.toLong()) } + (21..80).map { note(it.toLong()) }
        val armed = AlarmPlan.arm(items)
        assertEquals(80, armed.size)
        assertEquals(20, armed.count { it.kind == ReminderKind.Alarm })
        assertEquals(60, armed.count { it.kind == ReminderKind.Notification })
    }

    @Test
    fun `notifications have their own larger limit and the result is soonest first`() {
        val items = (150 downTo 1).map { note(it.toLong()) } + listOf(alarm(500), alarm(3))
        val armed = AlarmPlan.arm(items)
        assertEquals(100, armed.count { it.kind == ReminderKind.Notification })
        assertEquals(armed.map { it.atMillis }.sorted(), armed.map { it.atMillis })
        assertEquals(listOf(3L, 500L), armed.filter { it.kind == ReminderKind.Alarm }.map { it.atMillis })
    }

    @Test
    fun `an empty schedule arms nothing`() {
        assertEquals(emptyList(), AlarmPlan.arm(emptyList()))
    }

    @Test
    fun `a line round trips with either kind`() {
        for (kind in ReminderKind.entries) {
            val item = ScheduledTime("id-1", "Clean the kitchen", 1_700_000_000_123, kind)
            assertEquals(item, AlarmPlan.decode(AlarmPlan.encode(item)))
        }
    }

    @Test
    fun `separators line breaks percent signs plus signs and non latin text survive`() {
        val item = ScheduledTime("a|b", "Pay 50% + tax | now\nthen\r\nrelax - ăîșț 日本語 🙂", 42, ReminderKind.Alarm)
        val line = AlarmPlan.encode(item)
        assertEquals(false, line.contains('\n'))
        assertEquals(4, line.split('|').size)
        assertEquals(item, AlarmPlan.decode(line))
    }

    @Test
    fun `a line written before kinds existed is a notification`() {
        // Old lines used form encoding: a plus for a space and percent escapes for the rest.
        val legacy = "entry-7|Take+out+the+trash+%7C+now|1700000000000"
        assertEquals(ScheduledTime("entry-7", "Take out the trash | now", 1_700_000_000_000, ReminderKind.Notification), AlarmPlan.decode(legacy))
    }

    @Test
    fun `a legacy line with non latin text decodes`() {
        val legacy = "e|%C8%98tergere+%F0%9F%99%82|5"
        assertEquals("Ștergere 🙂", AlarmPlan.decode(legacy)?.text)
    }

    @Test
    fun `an unknown kind reads as a notification and bad lines are skipped`() {
        assertEquals(ReminderKind.Notification, AlarmPlan.decode("e|t|5|Siren")?.kind)
        assertNull(AlarmPlan.decode(""))
        assertNull(AlarmPlan.decode("only|two"))
        assertNull(AlarmPlan.decode("e|t|notanumber|Alarm"))
        assertNull(AlarmPlan.decode("a|b|1|Alarm|extra"))
    }

    @Test
    fun `usable alarms split by kind and keep notifications as notifications`() {
        val items = listOf(alarm(5), note(1), alarm(2), note(9))
        val routed = AlarmPlan.route(items, alarmsUsable = true)
        assertEquals(listOf(2L, 5L), routed.alarms.map { it.atMillis })
        assertEquals(listOf(1L, 9L), routed.notifications.map { it.atMillis })
    }

    @Test
    fun `alarms that cannot be used become notifications and nothing is lost`() {
        val items = listOf(alarm(5), note(1), alarm(2))
        val routed = AlarmPlan.route(items, alarmsUsable = false)
        assertEquals(emptyList(), routed.alarms)
        assertEquals(listOf(1L, 2L, 5L), routed.notifications.map { it.atMillis })
    }

    @Test
    fun `the soonest twenty alarms are routed and notifications are not counted against them`() {
        val items = (25 downTo 1).map { alarm(it.toLong()) } + (100..160).map { note(it.toLong()) }
        val routed = AlarmPlan.route(items, alarmsUsable = true)
        assertEquals((1L..20L).toList(), routed.alarms.map { it.atMillis })
        assertEquals(61, routed.notifications.size)
    }

    @Test
    fun `nothing to route`() {
        assertEquals(AlarmPlan.Routed(emptyList(), emptyList()), AlarmPlan.route(emptyList(), true))
        assertEquals(AlarmPlan.Routed(emptyList(), emptyList()), AlarmPlan.route(emptyList(), false))
    }

    @Test
    fun `the id of a planned template reminder survives the stored line`() {
        val id = TemplateReminders.id("3f2c-91", LocalDate(2026, 11, 3))
        assertEquals("template:3f2c-91:2026-11-03", id)
        val item = ScheduledTime(id, "Water plants", 1_700_000_000_000, ReminderKind.Alarm)
        assertEquals(item, AlarmPlan.decode(AlarmPlan.encode(item)))
    }

    @Test
    fun `today comes before later days when planned days exceed the limits`() {
        val todays = (1L..15L).map { ScheduledTime("entry", "Today", it, ReminderKind.Alarm) }
        val planned = (100L..130L).map { ScheduledTime("template:t:2026-10-24", "Later", it, ReminderKind.Alarm) }
        val armed = AlarmPlan.arm(planned + todays)
        assertEquals(20, armed.size)
        assertEquals(15, armed.count { it.entryId == "entry" })
        assertEquals((100L..104L).toList(), armed.filter { it.entryId != "entry" }.map { it.atMillis })
    }

    @Test
    fun `only the soonest thousand future times are kept and past ones are dropped`() {
        val items = (1500 downTo 1).map { note(it.toLong()) }
        val kept = AlarmPlan.keep(items, nowMillis = 100)
        assertEquals(1000, AlarmPlan.MaxKept)
        assertEquals((101L..1100L).toList(), kept.map { it.atMillis })
        assertEquals(emptyList(), AlarmPlan.keep(emptyList(), 0))
        assertEquals(emptyList(), AlarmPlan.keep(listOf(note(50)), nowMillis = 50), "a time at now is past")
    }

    @Test
    fun `arming the next times respects the limits and moves on as time passes`() {
        val kept = (1L..300L).map { note(it) } + (1L..30L).map { alarm(it * 10) }
        val first = AlarmPlan.armNext(kept, nowMillis = 0)
        assertEquals(20, first.count { it.kind == ReminderKind.Alarm })
        assertEquals(100, first.count { it.kind == ReminderKind.Notification })
        assertEquals((1L..100L).toList(), first.filter { it.kind == ReminderKind.Notification }.map { it.atMillis })

        val later = AlarmPlan.armNext(kept, nowMillis = 100)
        assertEquals((101L..200L).toList(), later.filter { it.kind == ReminderKind.Notification }.map { it.atMillis })
        assertEquals((11L..30L).map { it * 10 }, later.filter { it.kind == ReminderKind.Alarm }.map { it.atMillis })
        assertEquals(emptyList(), AlarmPlan.armNext(kept, nowMillis = 100_000))
    }
}
