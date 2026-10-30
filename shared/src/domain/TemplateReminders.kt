package com.usuallycoding.openadhd.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Instant

/**
 * The reminders of scheduled templates whose entries do not exist yet. The entry that carries a template's reminder is only
 * created while the app runs, so without this nothing is armed for a day on which the app is not opened.
 */
object TemplateReminders {
    /** Today and the 6 days after it are planned unless the platform asks for more ([ReminderScheduler.planDays]); the limits on armed times decide how much is armed. */
    const val Days = 7

    /** The id of the planned reminder of [templateId] on [date]. No entry has it, so a tap on it opens Today with nothing highlighted. */
    fun id(templateId: String, date: LocalDate): String = "template:$templateId:$date"

    /**
     * For each of the [days] days from [today], the reminder of every template that is due that day, has a reminder and has not
     * generated that day's entry. A generated entry, even one the user has since deleted, brings its own reminder instead.
     * Times follow [ReminderRules.upcoming], so the stop hour and the rule that past times never fire apply.
     */
    fun plan(
        templates: List<TaskTemplate>,
        today: LocalDate,
        stopHour: Int,
        now: Instant,
        zone: TimeZone,
        days: Int = Days,
    ): List<UpcomingReminder> {
        val reminders = templates.filter { it.reminder != null && it.schedule != TaskSchedule.None }
        return (0 until days).flatMap { offset ->
            val date = today.plus(offset, DateTimeUnit.DAY)
            reminders
                .filter { it.schedule.isDue(date) && it.lastGenerated != date }
                .mapNotNull { template ->
                    val reminder = template.reminder ?: return@mapNotNull null
                    val start = ReminderRules.at(date, reminder.hour, reminder.minute, zone)
                    val times = ReminderRules.upcoming(Reminder(start, reminder.repeatMinutes, reminder.kind), date, stopHour, now, zone)
                    if (times.isEmpty()) null else UpcomingReminder(id(template.id, date), template.name, times, reminder.kind)
                }
        }
    }
}
