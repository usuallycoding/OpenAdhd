package com.usuallycoding.openadhd.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.collapsedIds
import com.usuallycoding.openadhd.domain.pendingThenDone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The read-only look at an earlier day. It can read entries and nothing else, so it cannot complete, remove
 * or reorder anything. [today] is the current local date; it comes from outside so Today and History agree on it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val store: OpenAdhdStore,
    /** The current local date, shared with Today. */
    val today: StateFlow<LocalDate>,
) : ViewModel() {
    private val chosen = MutableStateFlow<LocalDate?>(null)

    private fun yesterday(today: LocalDate) = today.minus(1, DateTimeUnit.DAY)

    /** The day shown: the user's choice, or yesterday until they choose. Always before today, and not saved. */
    val date: StateFlow<LocalDate> =
        combine(chosen, today) { choice, now -> minOf(choice ?: yesterday(now), yesterday(now)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, yesterday(today.value))

    /** Whether a later day than [date] can be shown; it cannot once [date] is yesterday. */
    val canGoNext: StateFlow<Boolean> =
        combine(date, today) { shown, now -> shown < yesterday(now) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Days that can be picked in the calendar: earlier than today and with at least one entry. */
    val daysWithEntries: StateFlow<Set<LocalDate>> =
        store.daysWithEntries.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** The entries of [date] in the order Today uses. */
    val entries: StateFlow<List<PlannedTask>> =
        date.flatMapLatest { store.entriesFor(it) }
            .map { it.pendingThenDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val flippedEntryIds = MutableStateFlow<Set<String>>(emptySet())

    /** Entries with subtasks whose subtasks are hidden, with the same defaults as Today. Kept in memory only. */
    val collapsedEntryIds: StateFlow<Set<String>> =
        combine(flippedEntryIds, entries) { flipped, list -> list.collapsedIds(flipped) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleCollapsed(entryId: String) {
        flippedEntryIds.update { if (entryId in it) it - entryId else it + entryId }
    }

    fun previousDay() {
        chosen.value = date.value.minus(1, DateTimeUnit.DAY)
    }

    fun nextDay() {
        if (canGoNext.value) chosen.value = date.value.plus(1, DateTimeUnit.DAY)
    }

    /** Shows [day]; a day that is not before today is shown as yesterday instead. */
    fun pickDay(day: LocalDate) {
        chosen.value = day
    }
}
