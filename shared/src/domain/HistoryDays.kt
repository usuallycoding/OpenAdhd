package com.usuallycoding.openadhd.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Which days History can show, and how a month is laid out in its calendar. */
object HistoryDays {
    /** A day can be picked when it is before [today], or [today] too with [includeToday]; whether it has entries does not matter. */
    fun isSelectable(day: LocalDate, today: LocalDate, includeToday: Boolean = false): Boolean =
        day < today || (includeToday && day == today)

    fun monthStart(day: LocalDate): LocalDate = LocalDate(day.year, day.month, 1)

    fun addMonths(monthStart: LocalDate, months: Int): LocalDate = monthStart.plus(months, DateTimeUnit.MONTH)

    /** The cells of the month starting at [monthStart]: six weeks of seven, Monday first, null outside the month. */
    fun monthGrid(monthStart: LocalDate): List<LocalDate?> {
        val leading = monthStart.dayOfWeek.ordinal
        val length = monthStart.daysUntil(addMonths(monthStart, 1))
        return List(6 * 7) { cell ->
            val offset = cell - leading
            if (offset in 0 until length) monthStart.plus(offset, DateTimeUnit.DAY) else null
        }
    }

    /** The stock date picker (still used when scheduling templates) is time-zone free, so a day is its UTC midnight in milliseconds. */
    fun toPickerMillis(day: LocalDate): Long = day.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
}
