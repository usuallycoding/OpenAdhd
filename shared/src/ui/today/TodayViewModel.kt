package com.usuallycoding.openadhd.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PickerRoll
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.ReminderPreset
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.TaskPicker
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.collapsedIds
import com.usuallycoding.openadhd.domain.moved
import com.usuallycoding.openadhd.domain.pendingThenDone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** How often the current date is re-checked so the list rolls over at midnight. */
val DefaultDateCheckInterval = 30.seconds

/** A completion that turned out to be a critical hit, with the bonus coins it paid. */
data class CriticalHitEvent(val bonus: Int)

/** A completion that was unchecked, with the [coins] it had paid, all of which were taken back. */
data class UndoneEvent(val coins: Int)

/** The entry "Pick one for me" chose, for the list to scroll to and highlight. */
data class PickedEntryEvent(val entryId: String)

/** Where the "Pick one for me" dialog is. */
sealed interface RandomizerState {
    data object Closed : RandomizerState

    /** Every entry on today's list is completed, so there is nothing to pick. */
    data object NothingPending : RandomizerState

    data object ChoosingEnergy : RandomizerState

    /** No pending entry on today's list fits [energy]. */
    data class NothingFits(val energy: Energy) : RandomizerState
}

@OptIn(ExperimentalCoroutinesApi::class)
/** How saving a reminder ended. */
sealed interface ReminderSaveResult {
    data object Saved : ReminderSaveResult

    /** Saved, but the system does not let the app show notifications. */
    data object SavedNotificationsOff : ReminderSaveResult

    /** An alarm was saved and notifications are allowed, but the device does not let the app show full-screen alerts. */
    data object SavedFullScreenOff : ReminderSaveResult

    data class Refused(val message: String) : ReminderSaveResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val store: OpenAdhdStore,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    dateCheckInterval: Duration? = DefaultDateCheckInterval,
    private val random: Random = Random.Default,
    /** Delivers reminders, or null where there is no way to; then the sheet offers no reminder controls. */
    private val reminders: ReminderScheduler? = null,
) : ViewModel() {
    /** The current local date; re-checked every [dateCheckInterval], or only once when it is null. */
    val today: StateFlow<LocalDate> = flow {
        while (true) {
            emit(clock.todayIn(timeZone))
            if (dateCheckInterval == null) break
            delay(dateCheckInterval)
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, clock.todayIn(timeZone))

    /** Pending entries first in the order the user put them, then completed ones, the most recently done first. */
    val entries: StateFlow<List<PlannedTask>> =
        today.flatMapLatest { store.entriesFor(it) }
            .map { it.pendingThenDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Whether at least two entries are pending, so there is an order to change. */
    val canReorder: StateFlow<Boolean> =
        entries.map { list -> list.count { !it.isCompleted } >= 2 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val reorderRequested = MutableStateFlow(false)

    /** Whether Today shows move controls. Ends by itself when fewer than two entries are pending; not saved. */
    val reordering: StateFlow<Boolean> =
        combine(reorderRequested, entries) { on, list -> on && list.count { !it.isCompleted } >= 2 }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setReordering(on: Boolean) {
        reorderRequested.value = on
    }

    fun moveUp(id: String) = move(id, -1)

    fun moveDown(id: String) = move(id, 1)

    /** Moves a pending entry among the pending ones; completed entries keep their place after them. */
    private fun move(id: String, offset: Int) {
        val (pending, done) = entries.value.partition { !it.isCompleted }
        val pendingIds = pending.map { it.id }
        val moved = pendingIds.moved(id, offset)
        if (moved == pendingIds) return
        viewModelScope.launch { store.reorderEntries(today.value, moved + done.map { it.id }) }
    }

    val templates: StateFlow<List<TaskTemplate>> =
        store.templates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _undone = MutableSharedFlow<UndoneEvent>(extraBufferCapacity = 16)

    /** One event per uncheck, with the coins taken back, so the screen can say so. */
    val undone: SharedFlow<UndoneEvent> = _undone

    private val _criticalHits = MutableSharedFlow<CriticalHitEvent>(extraBufferCapacity = 16)

    /** One event per critical hit, for a short celebration; a small buffer keeps quick successive hits from being lost. */
    val criticalHits: SharedFlow<CriticalHitEvent> = _criticalHits

    private val flippedEntryIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Entries with subtasks whose subtasks are hidden. A pending entry starts expanded and a completed one
     * collapsed; tapping flips an entry away from that, and it goes back to its default whenever it becomes
     * completed or pending again. Kept in memory only.
     */
    val collapsedEntryIds: StateFlow<Set<String>> = combine(flippedEntryIds, entries) { flipped, list -> list.collapsedIds(flipped) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleCollapsed(entryId: String) {
        flippedEntryIds.update { if (entryId in it) it - entryId else it + entryId }
    }

    init {
        // An entry that was finished or reopened goes back to its default, so finishing folds it and unchecking opens it.
        viewModelScope.launch {
            var completedBefore = emptyMap<String, Boolean>()
            entries.collect { list ->
                val now = list.associate { it.id to it.isCompleted }
                val changed = now.filter { (id, completed) -> completedBefore[id]?.let { it != completed } == true }.keys
                if (changed.isNotEmpty()) flippedEntryIds.update { it - changed }
                completedBefore = now
            }
        }
    }

    /** Draws a template from those the picker shows, without adding anything; see [PickerRoll]. */
    fun rollTemplate(shown: List<TaskTemplate>, previousId: String?): TaskTemplate? = PickerRoll.pick(random, shown, previousId)

    fun assign(template: TaskTemplate) {
        viewModelScope.launch { store.assign(template, today.value, timeZone) }
    }

    fun remove(id: String) {
        viewModelScope.launch { store.remove(id) }
    }

    /** Whether this platform can deliver reminders at all. */
    val canRemind: Boolean = reminders != null

    /** Whether this platform offers alarms, so the sheet offers the Notification / Alarm choice. */
    val canAlarm: Boolean = reminders?.supportsAlarms == true

    private val reminderStopHour: StateFlow<Int> =
        store.settings.map { it.reminderStopHour }.stateIn(viewModelScope, SharingStarted.Eagerly, Settings.Default.reminderStopHour)

    /** In 30 minutes, In 1 hour and This evening, measured from now, with the ones that cannot work marked. */
    fun reminderPresets(): List<ReminderPreset> =
        ReminderRules.presets(clock.now(), today.value, reminderStopHour.value, timeZone)

    /** The instant of [hour]:[minute] today, for a time picked on the clock. */
    fun reminderStartAt(hour: Int, minute: Int): Instant = ReminderRules.at(today.value, hour, minute, timeZone)

    /** The current local time, where the clock picker starts. */
    fun currentTime(): LocalTime = clock.now().toLocalDateTime(timeZone).time

    /**
     * Saves a reminder of [kind] for [entryId], starting at [start] and repeating every [repeatMinutes] (or once when null).
     * A start that cannot work is refused with the reason; otherwise the reminder is saved and, the first time,
     * permission to show notifications is asked for. A refused permission still saves the reminder.
     */
    fun saveReminder(
        entryId: String,
        start: Instant,
        repeatMinutes: Int?,
        kind: ReminderKind = ReminderKind.Notification,
        onResult: (ReminderSaveResult) -> Unit,
    ) {
        val problem = ReminderRules.check(start, clock.now(), today.value, reminderStopHour.value, timeZone)
        if (problem != null) return onResult(ReminderSaveResult.Refused(problem.message))
        viewModelScope.launch {
            if (!store.setReminder(entryId, Reminder(start, repeatMinutes, kind))) {
                onResult(ReminderSaveResult.Refused("This task can no longer have a reminder"))
                return@launch
            }
            val granted = reminders?.ensurePermission() ?: true
            onResult(
                when {
                    !granted -> ReminderSaveResult.SavedNotificationsOff
                    kind == ReminderKind.Alarm && reminders?.fullScreenAlarmsAllowed() == false -> ReminderSaveResult.SavedFullScreenOff
                    else -> ReminderSaveResult.Saved
                },
            )
        }
    }

    /** Opens the system setting for full-screen alerts, from the message shown after saving an alarm. */
    fun openAlarmSettings() {
        reminders?.openAlarmSettings()
    }

    fun clearReminder(entryId: String) {
        viewModelScope.launch { store.setReminder(entryId, null) }
    }

    private val _randomizer = MutableStateFlow<RandomizerState>(RandomizerState.Closed)

    val randomizer: StateFlow<RandomizerState> = _randomizer

    private val _picked = MutableSharedFlow<PickedEntryEvent>(extraBufferCapacity = 16)

    /** One event per pick, so the screen can scroll to the entry and highlight it. */
    val picked: SharedFlow<PickedEntryEvent> = _picked

    private val reveals = Channel<String>(Channel.CONFLATED)

    /**
     * Ids of entries to scroll to and highlight because of a tapped reminder notification. Each is delivered once,
     * and is kept until the Today list is on screen to take it, which may be after a cold start.
     */
    val revealed: Flow<String> = reveals.receiveAsFlow()

    /** Asks for the entry to be scrolled to and highlighted; nothing in the store changes. */
    fun reveal(entryId: String) {
        reveals.trySend(entryId)
    }

    fun openRandomizer() {
        _randomizer.value =
            if (entries.value.none { !it.isCompleted }) RandomizerState.NothingPending else RandomizerState.ChoosingEnergy
    }

    /** Picks a pending entry for [energy] and closes the dialog, or says nothing fits. Nothing on the list changes. */
    fun chooseEnergy(energy: Energy) {
        val pick = TaskPicker.pick(random, energy, entries.value)
        if (pick == null) {
            _randomizer.value = RandomizerState.NothingFits(energy)
        } else {
            _randomizer.value = RandomizerState.Closed
            _picked.tryEmit(PickedEntryEvent(pick.id))
        }
    }

    fun chooseAnotherEnergy() {
        _randomizer.value = RandomizerState.ChoosingEnergy
    }

    fun dismissRandomizer() {
        _randomizer.value = RandomizerState.Closed
    }

    /** Completes an entry that has no subtasks. */
    fun completeTask(id: String) {
        viewModelScope.launch { announce(store.completeTask(id)) }
    }

    fun completeSubtask(id: String) {
        viewModelScope.launch { announce(store.completeSubtask(id)) }
    }

    /** Unchecks a completed entry that has no subtasks, taking back what it paid. */
    fun uncompleteTask(id: String) {
        viewModelScope.launch { store.uncompleteTask(id)?.let { _undone.emit(UndoneEvent(it)) } }
    }

    fun uncompleteSubtask(id: String) {
        viewModelScope.launch { store.uncompleteSubtask(id)?.let { _undone.emit(UndoneEvent(it)) } }
    }

    /** Tells the screen about a critical hit; usual and refused completions say nothing. */
    private suspend fun announce(result: CompletionResult?) {
        if (result != null && result.isCritical) _criticalHits.emit(CriticalHitEvent(result.bonus))
    }
}
