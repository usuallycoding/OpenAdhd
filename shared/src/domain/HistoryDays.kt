package com.usuallycoding.openadhd.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Which days History can show, and how days travel to and from the calendar, which counts in UTC milliseconds. */
object HistoryDays {
    /** A day can be picked when it is before [today] and something was assigned to it. */
    fun isSelectable(day: LocalDate, today: LocalDate, daysWithEntries: Set<LocalDate>): Boolean =
        day < today && day in daysWithEntries

    fun isSelectableYear(year: Int, today: LocalDate, daysWithEntries: Set<LocalDate>): Boolean =
        daysWithEntries.any { it.year == year && isSelectable(it, today, daysWithEntries) }

    /** The calendar is time-zone free, so a day is its UTC midnight; the device zone only decides what "today" is. */
    fun toPickerMillis(day: LocalDate): Long = day.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
}
