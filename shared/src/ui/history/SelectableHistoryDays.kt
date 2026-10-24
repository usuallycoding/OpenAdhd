package com.usuallycoding.openadhd.ui.history

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import com.usuallycoding.openadhd.domain.HistoryDays
import kotlinx.datetime.LocalDate

/** The calendar's rule: only earlier days (and today with [includeToday]) that have entries can be picked. */
@OptIn(ExperimentalMaterial3Api::class)
class SelectableHistoryDays(
    private val today: LocalDate,
    private val daysWithEntries: Set<LocalDate>,
    private val includeToday: Boolean = false,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        HistoryDays.isSelectable(HistoryDays.fromPickerMillis(utcTimeMillis), today, daysWithEntries, includeToday)

    override fun isSelectableYear(year: Int): Boolean = HistoryDays.isSelectableYear(year, today, daysWithEntries, includeToday)
}
