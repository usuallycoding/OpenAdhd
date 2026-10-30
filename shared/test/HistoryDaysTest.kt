package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.HistoryDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryDaysTest {
    private val today = LocalDate(2026, 10, 24)

    @Test
    fun `every earlier day is selectable and today and later are not`() {
        assertTrue(HistoryDays.isSelectable(LocalDate(2026, 10, 20), today))
        assertTrue(HistoryDays.isSelectable(LocalDate(2026, 10, 21), today), "empty past day")
        assertFalse(HistoryDays.isSelectable(today, today), "today")
        assertFalse(HistoryDays.isSelectable(LocalDate(2026, 10, 25), today), "future day")
    }

    @Test
    fun `with includeToday today is selectable and nothing else changes`() {
        assertTrue(HistoryDays.isSelectable(today, today, includeToday = true))
        assertTrue(HistoryDays.isSelectable(LocalDate(2026, 10, 21), today, includeToday = true), "empty past day")
        assertFalse(HistoryDays.isSelectable(LocalDate(2026, 10, 25), today, includeToday = true), "future day")
    }

    @Test
    fun `a month grid has six weeks starting on Monday`() {
        // 1 June 2026 is a Monday.
        val june = HistoryDays.monthGrid(LocalDate(2026, 6, 1))
        assertEquals(42, june.size)
        assertEquals(LocalDate(2026, 6, 1), june[0])
        assertEquals(LocalDate(2026, 6, 30), june[29])
        assertNull(june[30])
        // 1 November 2026 is a Sunday, so six empty cells come first.
        val november = HistoryDays.monthGrid(LocalDate(2026, 11, 1))
        assertEquals(List(6) { null }, november.take(6))
        assertEquals(LocalDate(2026, 11, 1), november[6])
        assertEquals(LocalDate(2026, 11, 30), november[35])
        assertNull(november[36])
    }

    @Test
    fun `February has 29 days in a leap year`() {
        assertEquals(29, HistoryDays.monthGrid(LocalDate(2028, 2, 1)).filterNotNull().size)
        assertEquals(28, HistoryDays.monthGrid(LocalDate(2026, 2, 1)).filterNotNull().size)
    }

    @Test
    fun `months move across year ends`() {
        assertEquals(LocalDate(2027, 1, 1), HistoryDays.addMonths(LocalDate(2026, 12, 1), 1))
        assertEquals(LocalDate(2025, 12, 1), HistoryDays.addMonths(LocalDate(2026, 1, 1), -1))
        assertEquals(LocalDate(2026, 10, 1), HistoryDays.monthStart(today))
    }

    @Test
    fun `picker milliseconds round trip to the same day`() {
        listOf(LocalDate(2026, 10, 24), LocalDate(2024, 2, 29), LocalDate(2026, 1, 1), LocalDate(2026, 12, 31)).forEach {
            assertEquals(it, HistoryDays.fromPickerMillis(HistoryDays.toPickerMillis(it)))
        }
        assertEquals(0L, HistoryDays.toPickerMillis(LocalDate(1970, 1, 1)))
    }
}
