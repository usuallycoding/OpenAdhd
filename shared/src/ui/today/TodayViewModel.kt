package com.usuallycoding.openadhd.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.TaskTemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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

/** A completion that turned out to be a critical hit, with the bonus coins it paid. */
data class CriticalHitEvent(val bonus: Int)

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

    private val _criticalHits = MutableSharedFlow<CriticalHitEvent>(extraBufferCapacity = 16)

    /** One event per critical hit, for a short celebration; a small buffer keeps quick successive hits from being lost. */
    val criticalHits: SharedFlow<CriticalHitEvent> = _criticalHits

    fun assign(template: TaskTemplate) {
        viewModelScope.launch { store.assign(template, today.value) }
    }

    fun remove(id: String) {
        viewModelScope.launch { store.remove(id) }
    }

    /** Completes an entry that has no subtasks. */
    fun completeTask(id: String) {
        viewModelScope.launch { announce(store.completeTask(id)) }
    }

    fun completeSubtask(id: String) {
        viewModelScope.launch { announce(store.completeSubtask(id)) }
    }

    /** Tells the screen about a critical hit; usual and refused completions say nothing. */
    private suspend fun announce(result: CompletionResult?) {
        if (result != null && result.isCritical) _criticalHits.emit(CriticalHitEvent(result.bonus))
    }
}
