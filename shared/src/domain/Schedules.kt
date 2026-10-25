package com.usuallycoding.openadhd.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** On which days a template is added to Today by itself. [None] means only by hand. */
sealed interface TaskSchedule {
    data object None : TaskSchedule

    data class OnDate(val date: LocalDate) : TaskSchedule

    /** Every week on [days]. */
    data class Weekly(val days: Set<DayOfWeek>) : TaskSchedule

    /** Every day from [start] to [end], both included. */
    data class Period(val start: LocalDate, val end: LocalDate) : TaskSchedule

    data object Daily : TaskSchedule

    /** Whether a template with this schedule is due on [date]. */
    fun isDue(date: LocalDate): Boolean = when (this) {
        None -> false
        is OnDate -> date == this.date
        is Weekly -> date.dayOfWeek in days
        is Period -> date >= start && date <= end
        Daily -> true
    }

    /** True for a date or period that lies wholly before [today]: it will never be due again. */
    fun hasEnded(today: LocalDate): Boolean = when (this) {
        is OnDate -> date < today
        is Period -> end < today
        else -> false
    }
}

/** A time of day, an optional repeat in minutes and a kind, applied to the entries a schedule generates. */
data class TemplateReminder(
    val minuteOfDay: Int,
    val repeatMinutes: Int? = null,
    val kind: ReminderKind = ReminderKind.Notification,
) {
    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60
}

/** Why a schedule cannot be saved. */
sealed interface ScheduleProblem {
    val message: String

    data object NoWeekday : ScheduleProblem {
        override val message = "Choose at least one day of the week"
    }

    data object PeriodOrder : ScheduleProblem {
        override val message = "The end date must be after the start date"
    }

    data object BadReminderTime : ScheduleProblem {
        override val message = "The reminder time must be within a day"
    }

    data object BadRepeat : ScheduleProblem {
        override val message = "Choose a repeat of 15, 30, 60 or 120 minutes, or none"
    }
}

/** The pure rules for template schedules: what can be saved, how it is stored, and how the list shows it. */
object ScheduleRules {
    private val WeekOrder = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )

    /** What is wrong with [schedule] and [reminder], or null when they can be saved. */
    fun check(schedule: TaskSchedule, reminder: TemplateReminder? = null): ScheduleProblem? = when {
        schedule is TaskSchedule.Weekly && schedule.days.isEmpty() -> ScheduleProblem.NoWeekday
        schedule is TaskSchedule.Period && schedule.end <= schedule.start -> ScheduleProblem.PeriodOrder
        reminder != null && reminder.minuteOfDay !in 0 until 24 * 60 -> ScheduleProblem.BadReminderTime
        reminder?.repeatMinutes != null && reminder.repeatMinutes !in ReminderRules.RepeatChoices -> ScheduleProblem.BadRepeat
        else -> null
    }

    /** The line shown on a list item, or null when the schedule is None. */
    fun summary(schedule: TaskSchedule, today: LocalDate): String? = when {
        schedule == TaskSchedule.None -> null
        schedule.hasEnded(today) -> "Ended"
        schedule is TaskSchedule.OnDate -> "On ${dateText(schedule.date)}"
        schedule is TaskSchedule.Weekly -> WeekOrder.filter { it in schedule.days }.joinToString(", ") { dayName(it) }
        schedule is TaskSchedule.Period -> "${dateText(schedule.start)} to ${dateText(schedule.end)}"
        else -> "Every day"
    }

    fun dayName(day: DayOfWeek): String = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    /** The weekdays in the order a week is shown, Monday first. */
    val Weekdays: List<DayOfWeek> get() = WeekOrder

    private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun dateText(date: LocalDate): String = "${date.day} ${Months[date.month.ordinal]} ${date.year}"

    /** "09:00" for a reminder's time of day. */
    fun clock(reminder: TemplateReminder): String =
        "${reminder.hour.toString().padStart(2, '0')}:${reminder.minute.toString().padStart(2, '0')}"

    // ---- storage: a kind name, two optional dates and a 7-bit weekday mask (Monday is bit 0)

    /** The kind, start, end and weekday mask that [schedule] is stored as. */
    fun encode(schedule: TaskSchedule): StoredSchedule = when (schedule) {
        TaskSchedule.None -> StoredSchedule("None")
        is TaskSchedule.OnDate -> StoredSchedule("Date", schedule.date.toString())
        is TaskSchedule.Weekly -> StoredSchedule("Weekly", days = WeekOrder.foldIndexed(0) { i, mask, day -> if (day in schedule.days) mask or (1 shl i) else mask })
        is TaskSchedule.Period -> StoredSchedule("Period", schedule.start.toString(), schedule.end.toString())
        TaskSchedule.Daily -> StoredSchedule("Daily")
    }

    /** The schedule a stored one stands for; anything unreadable is None, so a bad value never blocks start-up. */
    fun decode(stored: StoredSchedule): TaskSchedule = runCatching {
        when (stored.kind) {
            "Date" -> TaskSchedule.OnDate(LocalDate.parse(stored.start!!))
            "Weekly" -> TaskSchedule.Weekly(WeekOrder.filterIndexed { i, _ -> stored.days and (1 shl i) != 0 }.toSet())
            "Period" -> TaskSchedule.Period(LocalDate.parse(stored.start!!), LocalDate.parse(stored.end!!))
            "Daily" -> TaskSchedule.Daily
            else -> TaskSchedule.None
        }
    }.getOrDefault(TaskSchedule.None)
}

/** A schedule as it is saved: see [ScheduleRules.encode]. */
data class StoredSchedule(val kind: String, val start: String? = null, val end: String? = null, val days: Int = 0)
