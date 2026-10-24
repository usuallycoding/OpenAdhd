package com.usuallycoding.openadhd.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.Redemption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * The reward history one day at a time. It only reads redemptions. [today] is the current local date, shared with
 * Today, and a redemption belongs to the day it was made on in [timeZone].
 */
class RewardHistoryViewModel(
    store: OpenAdhdStore,
    /** The current local date. */
    val today: StateFlow<LocalDate>,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val chosen = MutableStateFlow<LocalDate?>(null)

    /** The day shown: the user's choice, or today until they choose. Never after today, and not saved. */
    val date: StateFlow<LocalDate> =
        combine(chosen, today) { choice, now -> minOf(choice ?: now, now) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, today.value)

    /** Whether a later day than [date] can be shown; it cannot once today is shown. */
    val canGoNext: StateFlow<Boolean> =
        combine(date, today) { shown, now -> shown < now }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Days on which something was redeemed, for the calendar. */
    val daysWithRedemptions: StateFlow<Set<LocalDate>> =
        store.redemptions.map { list -> list.map { it.localDate() }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** The redemptions made on [date], newest first. */
    val redemptions: StateFlow<List<Redemption>> =
        combine(store.redemptions, date) { list, shown ->
            // The store lists newest first and the sort is stable, so redemptions made at the same instant keep that order.
            list.filter { it.localDate() == shown }.sortedByDescending { it.redeemedAt }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun Redemption.localDate(): LocalDate = redeemedAt.toLocalDateTime(timeZone).date

    fun previousDay() {
        chosen.value = date.value.minus(1, DateTimeUnit.DAY)
    }

    fun nextDay() {
        if (canGoNext.value) chosen.value = date.value.plus(1, DateTimeUnit.DAY)
    }

    /** Shows [day]; a day after today is shown as today instead. */
    fun pickDay(day: LocalDate) {
        chosen.value = day
    }
}
