package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.ScheduleProblem
import com.usuallycoding.openadhd.domain.ScheduleRules
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TemplateReminder
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleRulesTest {
    private val monday = LocalDate(2026, 10, 19)
    private val friday = LocalDate(2026, 10, 23)

    @Test
    fun `none is never due`() {
        assertFalse(TaskSchedule.None.isDue(friday))
    }

    @Test
    fun `a date is due on that day only`() {
        val schedule = TaskSchedule.OnDate(friday)
        assertTrue(schedule.isDue(friday))
        assertFalse(schedule.isDue(LocalDate(2026, 10, 24)))
        assertFalse(schedule.isDue(LocalDate(2026, 10, 22)))
    }

    @Test
    fun `weekly is due on its weekdays`() {
        val schedule = TaskSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        assertTrue(schedule.isDue(monday))
        assertTrue(schedule.isDue(friday))
        assertFalse(schedule.isDue(LocalDate(2026, 10, 20)))
        assertFalse(schedule.isDue(LocalDate(2026, 10, 25)))
    }

    @Test
    fun `a period includes both of its ends`() {
        val schedule = TaskSchedule.Period(LocalDate(2026, 3, 3), LocalDate(2026, 3, 7))
        assertFalse(schedule.isDue(LocalDate(2026, 3, 2)))
        assertTrue(schedule.isDue(LocalDate(2026, 3, 3)))
        assertTrue(schedule.isDue(LocalDate(2026, 3, 5)))
        assertTrue(schedule.isDue(LocalDate(2026, 3, 7)))
        assertFalse(schedule.isDue(LocalDate(2026, 3, 8)))
    }

    @Test
    fun `daily is due every day`() {
        assertTrue(TaskSchedule.Daily.isDue(monday))
        assertTrue(TaskSchedule.Daily.isDue(LocalDate(2030, 1, 1)))
    }

    @Test
    fun `a weekly schedule needs a day`() {
        assertEquals(ScheduleProblem.NoWeekday, ScheduleRules.check(TaskSchedule.Weekly(emptySet())))
        assertNull(ScheduleRules.check(TaskSchedule.Weekly(setOf(DayOfWeek.SUNDAY))))
    }

    @Test
    fun `a period must end after it starts`() {
        val day = LocalDate(2026, 3, 3)
        assertEquals(ScheduleProblem.PeriodOrder, ScheduleRules.check(TaskSchedule.Period(day, day)))
        assertEquals(ScheduleProblem.PeriodOrder, ScheduleRules.check(TaskSchedule.Period(day, LocalDate(2026, 3, 2))))
        assertNull(ScheduleRules.check(TaskSchedule.Period(day, LocalDate(2026, 3, 4))))
        assertEquals("The end date must be after the start date", ScheduleProblem.PeriodOrder.message)
    }

    @Test
    fun `a reminder must be a time within the day with an allowed repeat`() {
        assertNull(ScheduleRules.check(TaskSchedule.Daily, TemplateReminder(9 * 60, 30)))
        assertNull(ScheduleRules.check(TaskSchedule.Daily, TemplateReminder(0, null)))
        assertEquals(ScheduleProblem.BadReminderTime, ScheduleRules.check(TaskSchedule.Daily, TemplateReminder(24 * 60)))
        assertEquals(ScheduleProblem.BadRepeat, ScheduleRules.check(TaskSchedule.Daily, TemplateReminder(540, 20)))
    }

    @Test
    fun `a date or period wholly in the past has ended and others have not`() {
        assertTrue(TaskSchedule.OnDate(monday).hasEnded(friday))
        assertFalse(TaskSchedule.OnDate(friday).hasEnded(friday))
        assertTrue(TaskSchedule.Period(LocalDate(2026, 10, 1), monday).hasEnded(friday))
        assertFalse(TaskSchedule.Period(LocalDate(2026, 10, 1), friday).hasEnded(friday))
        assertFalse(TaskSchedule.Daily.hasEnded(friday))
        assertFalse(TaskSchedule.Weekly(setOf(DayOfWeek.MONDAY)).hasEnded(friday))
    }

    @Test
    fun `the list line describes each schedule`() {
        assertNull(ScheduleRules.summary(TaskSchedule.None, friday))
        assertEquals("Mon, Wed, Fri", ScheduleRules.summary(TaskSchedule.Weekly(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)), friday))
        assertEquals("Every day", ScheduleRules.summary(TaskSchedule.Daily, friday))
        assertEquals("On 23 Oct 2026", ScheduleRules.summary(TaskSchedule.OnDate(friday), monday))
        assertEquals("3 Mar 2027 to 7 Mar 2027", ScheduleRules.summary(TaskSchedule.Period(LocalDate(2027, 3, 3), LocalDate(2027, 3, 7)), friday))
        assertEquals("Ended", ScheduleRules.summary(TaskSchedule.OnDate(monday), friday))
        assertEquals("Ended", ScheduleRules.summary(TaskSchedule.Period(LocalDate(2026, 10, 1), monday), friday))
    }

    @Test
    fun `a schedule survives being stored`() {
        listOf(
            TaskSchedule.None,
            TaskSchedule.OnDate(friday),
            TaskSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)),
            TaskSchedule.Period(monday, friday),
            TaskSchedule.Daily,
        ).forEach { assertEquals(it, ScheduleRules.decode(ScheduleRules.encode(it))) }
    }

    @Test
    fun `an unreadable stored schedule is none`() {
        assertEquals(TaskSchedule.None, ScheduleRules.decode(com.usuallycoding.openadhd.domain.StoredSchedule("Date", "not a date")))
        assertEquals(TaskSchedule.None, ScheduleRules.decode(com.usuallycoding.openadhd.domain.StoredSchedule("Whatever")))
    }
}
