package com.usuallycoding.openadhd.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** How a reminder is delivered: quietly as a notification, or by ringing like an alarm where the device can. */
enum class ReminderKind { Notification, Alarm }

/**
 * A reminder for a Today entry: it fires at [startAt], then every [repeatMinutes] if set, as long as the entry
 * is not done and the stop time has not been reached. [kind] says how it is delivered.
 */
data class Reminder(val startAt: Instant, val repeatMinutes: Int?, val kind: ReminderKind = ReminderKind.Notification)

/** Why a reminder cannot be saved. */
sealed interface ReminderProblem {
    val message: String

    data object Passed : ReminderProblem {
        override val message = "That time has already passed"
    }

    data object NotToday : ReminderProblem {
        override val message = "A reminder must be set for today"
    }

    data class AfterStop(val stopHour: Int) : ReminderProblem {
        override val message = "Reminders stop at ${stopHour.toString().padStart(2, '0')}:00, so choose an earlier time"
    }
}

/** A quick choice for when a reminder starts, and whether it can be used right now. */
data class ReminderPreset(val label: String, val startAt: Instant, val available: Boolean)

/** The pure rules for reminders: what can be saved, what the presets are, and when a reminder fires. */
object ReminderRules {
    val RepeatChoices: List<Int> = listOf(15, 30, 60, 120)

    /** A new reminder starts with this repeat selected. */
    const val DefaultRepeat = 30

    const val EveningHour = 18

    /** The moment reminders stop on [date]: [stopHour] o'clock in [zone]. Nothing fires at or after it. */
    fun stopInstant(date: LocalDate, stopHour: Int, zone: TimeZone): Instant =
        LocalDateTime(date, LocalTime(stopHour, 0)).toInstant(zone)

    /** What is wrong with starting a reminder at [start] on [date], or null when it can be saved. */
    fun check(start: Instant, now: Instant, date: LocalDate, stopHour: Int, zone: TimeZone): ReminderProblem? = when {
        start <= now -> ReminderProblem.Passed
        start.toLocalDateTime(zone).date != date -> ReminderProblem.NotToday
        start >= stopInstant(date, stopHour, zone) -> ReminderProblem.AfterStop(stopHour)
        else -> null
    }

    /** In 30 minutes, In 1 hour and This evening, each marked unavailable when it could not be saved now. */
    fun presets(now: Instant, date: LocalDate, stopHour: Int, zone: TimeZone): List<ReminderPreset> {
        val thisMinute = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() / 60_000 * 60_000)
        val evening = LocalDateTime(date, LocalTime(EveningHour, 0)).toInstant(zone)
        return listOf(
            "In 30 minutes" to thisMinute + 30.minutes,
            "In 1 hour" to thisMinute + 60.minutes,
            "This evening" to evening,
        ).map { (label, start) -> ReminderPreset(label, start, check(start, now, date, stopHour, zone) == null) }
    }

    /**
     * Every time [reminder] still has to fire for an entry of [date]: its start and each repeat after it that is
     * later than [now] and earlier than the stop time. Times already past are never included.
     */
    fun upcoming(reminder: Reminder, date: LocalDate, stopHour: Int, now: Instant, zone: TimeZone): List<Instant> {
        val stop = stopInstant(date, stopHour, zone)
        val times = mutableListOf<Instant>()
        var time = reminder.startAt
        val step = reminder.repeatMinutes?.takeIf { it > 0 }?.minutes
        while (time < stop) {
            if (time > now) times += time
            time = time + (step ?: break)
        }
        return times
    }

    /** [hour]:[minute] on [date] in [zone], as the instant a reminder picked on a clock starts. */
    fun at(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Instant =
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone)

    /** The reminder as a line for a row, such as "Reminder 15:00, every 30 min". */
    fun summary(reminder: Reminder, zone: TimeZone): String {
        val at = clock(reminder.startAt, zone)
        val repeat = reminder.repeatMinutes?.let { ", every ${repeatLabel(it)}" }.orEmpty()
        val name = if (reminder.kind == ReminderKind.Alarm) "Alarm" else "Reminder"
        return "$name $at$repeat"
    }

    /** "30 min", "1 h" or "2 h" for a repeat in minutes. */
    fun repeatLabel(minutes: Int): String = if (minutes % 60 == 0) "${minutes / 60} h" else "$minutes min"

    /** The time of day of [instant] in [zone] as HH:mm. */
    fun clock(instant: Instant, zone: TimeZone): String {
        val time = instant.toLocalDateTime(zone).time
        return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }
}
