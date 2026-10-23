package com.usuallycoding.openadhd.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.TaskPicker
import com.usuallycoding.openadhd.domain.TaskTemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How often the current date is re-checked so the list rolls over at midnight. */
val DefaultDateCheckInterval = 30.seconds

/** A completion that turned out to be a critical hit, with the bonus coins it paid. */
data class CriticalHitEvent(val bonus: Int)

/** Where the "Pick one for me" dialog is. */
sealed interface RandomizerState {
    data object Closed : RandomizerState

    /** There are no task templates at all, so there is nothing to suggest. */
    data object NoTemplates : RandomizerState

    data object ChoosingEnergy : RandomizerState

    data class Suggesting(val energy: Energy, val template: TaskTemplate) : RandomizerState

    /** No suggestion for [energy]: nothing fits it, or [exhausted] when everything that fits has already been shown. */
    data class NothingFits(val energy: Energy, val exhausted: Boolean) : RandomizerState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val store: OpenAdhdStore,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    dateCheckInterval: Duration? = DefaultDateCheckInterval,
    private val random: Random = Random.Default,
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

    private val _randomizer = MutableStateFlow<RandomizerState>(RandomizerState.Closed)

    val randomizer: StateFlow<RandomizerState> = _randomizer

    /** Template ids already suggested for the current energy choice, so "Another" never repeats. */
    private val shown = mutableSetOf<String>()

    fun openRandomizer() {
        shown.clear()
        _randomizer.value = if (templates.value.isEmpty()) RandomizerState.NoTemplates else RandomizerState.ChoosingEnergy
    }

    /** Starts a fresh round for [energy]: whatever was shown before is forgotten. */
    fun chooseEnergy(energy: Energy) {
        shown.clear()
        suggest(energy)
    }

    fun another() {
        val state = _randomizer.value
        if (state is RandomizerState.Suggesting) suggest(state.energy)
    }

    fun chooseAnotherEnergy() {
        shown.clear()
        _randomizer.value = RandomizerState.ChoosingEnergy
    }

    /** Adds the suggested template to today exactly like picking it from the list, then closes. */
    fun acceptSuggestion() {
        val state = _randomizer.value as? RandomizerState.Suggesting ?: return
        assign(state.template)
        dismissRandomizer()
    }

    fun dismissRandomizer() {
        shown.clear()
        _randomizer.value = RandomizerState.Closed
    }

    private fun suggest(energy: Energy) {
        val planned = entries.value.mapNotNull { it.templateId }.toSet()
        val picked = TaskPicker.pick(random, energy, templates.value, planned + shown)
        _randomizer.value = if (picked == null) {
            RandomizerState.NothingFits(energy, exhausted = shown.isNotEmpty())
        } else {
            shown += picked.id
            RandomizerState.Suggesting(energy, picked)
        }
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
