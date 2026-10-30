package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.TemplateReminder
import com.usuallycoding.openadhd.domain.TemplateReminders
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TemplateRemindersTest {
    private val zone = TimeZone.UTC

    // Friday 23 October 2026, 16:10.
    private val now = Instant.parse("2026-10-23T16:10:00Z")
    private val today = LocalDate(2026, 10, 23)

    private fun template(
        id: String = "t",
        schedule: TaskSchedule = TaskSchedule.Daily,
        reminder: TemplateReminder? = TemplateReminder(9 * 60),
        lastGenerated: LocalDate? = null,
    ) = TaskTemplate(id, "Water plants", Tier.One, Energy.Low, schedule = schedule, reminder = reminder, lastGenerated = lastGenerated)

    private fun plan(vararg templates: TaskTemplate, stopHour: Int = 22, at: Instant = now) =
        TemplateReminders.plan(templates.toList(), today, stopHour, at, zone)

    private fun days(reminders: List<com.usuallycoding.openadhd.domain.UpcomingReminder>): List<String> =
        reminders.map { it.entryId.substringAfterLast(':') }

    @Test
    fun `a daily template is planned for the next days but not for today once its time has passed`() {
        val plan = plan(template())
        assertEquals(
            listOf("2026-10-24", "2026-10-25", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-29"),
            days(plan),
        )
        val first = plan.first()
        assertEquals("template:t:2026-10-24", first.entryId)
        assertEquals("Water plants", first.text)
        assertEquals(listOf(Instant.parse("2026-10-24T09:00:00Z")), first.times)
    }

    @Test
    fun `today is planned while its time is still ahead and the app has not generated the entry`() {
        val early = Instant.parse("2026-10-23T07:00:00Z")
        val plan = plan(template(), at = early)
        assertEquals(7, plan.size)
        assertEquals("template:t:2026-10-23", plan.first().entryId)
        assertEquals(listOf(Instant.parse("2026-10-23T09:00:00Z")), plan.first().times)
    }

    @Test
    fun `a weekly template is planned only on its days`() {
        // Monday 26 and Thursday 29 October are the only such days in the 7 days from Friday 23.
        val plan = plan(template(schedule = TaskSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))))
        assertEquals(listOf("2026-10-26", "2026-10-29"), days(plan))
    }

    @Test
    fun `a template on one date or in a period is planned on those days only`() {
        val onDate = template("d", TaskSchedule.OnDate(LocalDate(2026, 10, 25)))
        val period = template("p", TaskSchedule.Period(LocalDate(2026, 10, 27), LocalDate(2026, 10, 28)))
        assertEquals(listOf("template:d:2026-10-25", "template:p:2026-10-27", "template:p:2026-10-28"), plan(onDate, period).map { it.entryId }.sorted())
        assertTrue(plan(template("far", TaskSchedule.OnDate(LocalDate(2026, 11, 20)))).isEmpty(), "beyond the 7 days")
    }

    @Test
    fun `a template without a reminder or without a schedule gives nothing`() {
        assertTrue(plan(template(reminder = null)).isEmpty())
        assertTrue(plan(template(schedule = TaskSchedule.None)).isEmpty())
    }

    @Test
    fun `a day whose entry was generated is left to that entry`() {
        val early = Instant.parse("2026-10-23T07:00:00Z")
        val plan = plan(template(lastGenerated = today), at = early)
        assertEquals(6, plan.size)
        assertEquals("template:t:2026-10-24", plan.first().entryId)
    }

    @Test
    fun `the stop hour cuts the repeats and past times are skipped`() {
        val repeating = template(reminder = TemplateReminder(9 * 60, repeatMinutes = 60))
        val tomorrow = plan(repeating, stopHour = 12).first()
        assertEquals(listOf("09:00", "10:00", "11:00"), tomorrow.times.map { ReminderRules.clock(it, zone) })

        val afternoon = Instant.parse("2026-10-23T10:30:00Z")
        val todays = plan(repeating, stopHour = 12, at = afternoon).first()
        assertEquals("template:t:2026-10-23", todays.entryId)
        assertEquals(listOf("11:00"), todays.times.map { ReminderRules.clock(it, zone) })
    }

    @Test
    fun `the kind of the template reminder is kept`() {
        val alarm = template(reminder = TemplateReminder(7 * 60, kind = ReminderKind.Alarm))
        assertTrue(plan(alarm).all { it.kind == ReminderKind.Alarm })
    }
}
