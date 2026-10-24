package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.HistoryDays
import com.usuallycoding.openadhd.ui.history.SelectableHistoryDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class HistoryDaysTest {
    private val today = LocalDate(2026, 10, 24)
    private val withEntries = setOf(LocalDate(2026, 10, 20), LocalDate(2025, 3, 2), today)

    @Test
    fun `only earlier days with entries are selectable`() {
        assertTrue(HistoryDays.isSelectable(LocalDate(2026, 10, 20), today, withEntries))
        assertFalse(HistoryDays.isSelectable(LocalDate(2026, 10, 21), today, withEntries), "empty past day")
        assertFalse(HistoryDays.isSelectable(today, today, withEntries), "today, even with entries")
        assertFalse(HistoryDays.isSelectable(LocalDate(2026, 10, 25), today, withEntries), "future day")
    }

    @Test
    fun `with includeToday a day today with entries is selectable and nothing else changes`() {
        assertTrue(HistoryDays.isSelectable(today, today, withEntries, includeToday = true))
        assertFalse(HistoryDays.isSelectable(today, today, setOf(LocalDate(2026, 10, 20)), includeToday = true), "today without entries")
        assertTrue(HistoryDays.isSelectable(LocalDate(2026, 10, 20), today, withEntries, includeToday = true))
        assertFalse(HistoryDays.isSelectable(LocalDate(2026, 10, 21), today, withEntries, includeToday = true), "empty past day")
        assertFalse(HistoryDays.isSelectable(LocalDate(2026, 10, 25), today, withEntries + LocalDate(2026, 10, 25), includeToday = true), "future day")
        assertTrue(HistoryDays.isSelectableYear(2026, today, setOf(today), includeToday = true))
        val rule = SelectableHistoryDays(today, withEntries, includeToday = true)
        assertTrue(rule.isSelectableDate(HistoryDays.toPickerMillis(today)))
        assertTrue(rule.isSelectableYear(2026))
    }

    @Test
    fun `a year is selectable only if it has a selectable day`() {
        assertTrue(HistoryDays.isSelectableYear(2026, today, withEntries))
        assertTrue(HistoryDays.isSelectableYear(2025, today, withEntries))
        assertFalse(HistoryDays.isSelectableYear(2024, today, withEntries))
        assertFalse(HistoryDays.isSelectableYear(2026, today, setOf(today)), "only today in that year")
    }

    @Test
    fun `the calendar rule works on picker milliseconds`() {
        val rule = SelectableHistoryDays(today, withEntries)
        assertTrue(rule.isSelectableDate(HistoryDays.toPickerMillis(LocalDate(2026, 10, 20))))
        assertFalse(rule.isSelectableDate(HistoryDays.toPickerMillis(today)))
        // Any moment of a day counts as that day, not only its midnight.
        assertTrue(rule.isSelectableDate(HistoryDays.toPickerMillis(LocalDate(2026, 10, 20)) + 86_399_000))
    }

    @Test
    fun `picker milliseconds round trip to the same day`() {
        listOf(LocalDate(2026, 10, 24), LocalDate(2024, 2, 29), LocalDate(2026, 1, 1), LocalDate(2026, 12, 31)).forEach {
            assertEquals(it, HistoryDays.fromPickerMillis(HistoryDays.toPickerMillis(it)))
        }
        assertEquals(0L, HistoryDays.toPickerMillis(LocalDate(1970, 1, 1)))
    }

    @Test
    fun `the day does not shift when the device zone is far from UTC`() {
        // 2026-10-23 23:30 UTC is already the 24th in Kiritimati (+14) and still the 23rd in Pago Pago (-11).
        val now = Instant.parse("2026-10-23T23:30:00Z")
        val clock = FakeClock(now)
        val kiritimati = clock.todayIn(TimeZone.of("Pacific/Kiritimati"))
        val pagoPago = clock.todayIn(TimeZone.of("Pacific/Pago_Pago"))
        assertEquals(LocalDate(2026, 10, 24), kiritimati)
        assertEquals(LocalDate(2026, 10, 23), pagoPago)
        assertEquals(kiritimati, HistoryDays.fromPickerMillis(HistoryDays.toPickerMillis(kiritimati)))
        assertEquals(pagoPago, HistoryDays.fromPickerMillis(HistoryDays.toPickerMillis(pagoPago)))
    }
}
