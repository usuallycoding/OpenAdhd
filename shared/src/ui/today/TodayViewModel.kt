package com.usuallycoding.openadhd.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.TaskTemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How often the current date is re-checked so the list rolls over at midnight. */
val DefaultDateCheckInterval = 30.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val store: OpenAdhdStore,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    dateCheckInterval: Duration? = DefaultDateCheckInterval,
) : ViewModel() {
    /** The current local date; re-checked every [dateCheckInterval], or only once when it is null. */
    val today: StateFlow<LocalDate> = flow {
        while (true) {
            emit(clock.todayIn(timeZone))
            if (dateCheckInterval == null) break
            delay(dateCheckInterval)
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, clock.todayIn(timeZone))

    val entries: StateFlow<List<PlannedTask>> =
        today.flatMapLatest { store.entriesFor(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val templates: StateFlow<List<TaskTemplate>> =
        store.templates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun assign(template: TaskTemplate) {
        viewModelScope.launch { store.assign(template, today.value) }
    }

    fun remove(id: String) {
        viewModelScope.launch { store.remove(id) }
    }

    fun complete(id: String) {
        viewModelScope.launch { store.complete(id) }
    }
}
