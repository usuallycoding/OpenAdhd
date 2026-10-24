package com.usuallycoding.openadhd.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Which days History can show, and how days travel to and from the calendar, which counts in UTC milliseconds. */
object HistoryDays {
    /**
     * A day can be picked when it is before [today] (or [today] too with [includeToday]) and something
     * happened on it, as listed in [daysWithEntries].
     */
    fun isSelectable(
        day: LocalDate,
        today: LocalDate,
        daysWithEntries: Set<LocalDate>,
        includeToday: Boolean = false,
    ): Boolean = (day < today || (includeToday && day == today)) && day in daysWithEntries

    fun isSelectableYear(
        year: Int,
        today: LocalDate,
        daysWithEntries: Set<LocalDate>,
        includeToday: Boolean = false,
    ): Boolean = daysWithEntries.any { it.year == year && isSelectable(it, today, daysWithEntries, includeToday) }

    /** The calendar is time-zone free, so a day is its UTC midnight; the device zone only decides what "today" is. */
    fun toPickerMillis(day: LocalDate): Long = day.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
}
