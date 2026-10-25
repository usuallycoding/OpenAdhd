package com.usuallycoding.openadhd.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.GameRules
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.ScheduleProblem
import com.usuallycoding.openadhd.domain.ScheduleRules
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TemplateReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.TemplateRepository
import com.usuallycoding.openadhd.domain.matching
import com.usuallycoding.openadhd.domain.moved
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TemplateInputError { NameRequired, SubtaskTitleRequired, TooManySubtasks }

class TemplatesViewModel(
    private val repository: TemplateRepository,
    settings: Flow<Settings> = flowOf(Settings.Default),
    /** Delivers reminders, or null where there is no way to; then no notification permission is asked. */
    private val reminders: ReminderScheduler? = null,
) : ViewModel() {
    private val rules: StateFlow<GameRules> =
        settings.map { it.rules }.stateIn(viewModelScope, SharingStarted.Eagerly, GameRules.Default)

    /** Whether this platform offers alarms, so the template form offers the Notification / Alarm choice. */
    val canAlarm: Boolean = reminders?.supportsAlarms == true

    val templates: StateFlow<List<TaskTemplate>> =
        repository.templates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _query = MutableStateFlow("")

    /** The text of the search above the list; kept while the app runs and not saved. */
    val query: StateFlow<String> = _query

    /** The templates whose name or description contain the search text, in the list's order. */
    val visible: StateFlow<List<TaskTemplate>> =
        combine(templates, _query) { list, text -> list.matching(text) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Changes the search text. Typing turns Reorder off: moving rows in a filtered list would be confusing. */
    fun setQuery(text: String) {
        _query.value = text
        if (text.isNotBlank()) reorderRequested.value = false
    }

    /** Whether there are enough items for the order to be worth changing, and no search is narrowing the list. */
    val canReorder: StateFlow<Boolean> = combine(templates, _query) { list, text -> list.size >= 2 && text.isBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val reorderRequested = MutableStateFlow(false)

    /** Whether the list shows move controls. Ends by itself when fewer than two templates are left or a search starts; not saved. */
    val reordering: StateFlow<Boolean> = combine(reorderRequested, templates, _query) { on, list, text -> on && list.size >= 2 && text.isBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setReordering(on: Boolean) {
        reorderRequested.value = on
    }

    fun moveUp(id: String) = move(id, -1)

    fun moveDown(id: String) = move(id, 1)

    private fun move(id: String, offset: Int) {
        val ids = templates.value.map { it.id }
        val moved = ids.moved(id, offset)
        if (moved != ids) viewModelScope.launch { repository.reorderTemplates(moved) }
    }

    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Short messages for the screen, such as that notifications are off after saving a template reminder. */
    val notices: SharedFlow<String> = _notices

    /**
     * Creates a template, or updates the one with [id]. Invalid input is ignored; check [validate] first
     * so the user can be told what is wrong.
     */
    fun save(
        id: String?,
        name: String,
        tier: Tier,
        energy: Energy,
        description: String = "",
        subtasks: List<String> = emptyList(),
        schedule: TaskSchedule = TaskSchedule.None,
        reminder: TemplateReminder? = null,
    ) {
        if (validate(name, tier, subtasks, rules.value) != null) return
        if (validateSchedule(schedule, reminder) != null) return
        // A new template goes to the top and may not match the search, so the search is cleared to keep it in view.
        if (id == null) _query.value = ""
        viewModelScope.launch {
            if (id == null) {
                repository.create(name, tier, energy, description, subtasks, schedule, reminder)
            } else {
                repository.update(TaskTemplate(id, name, tier, energy, description, subtasks, schedule, reminder))
            }
            // The template is already saved: a refusal only tells the user why its reminder will not show.
            if (reminder != null && reminders != null) {
                val notificationsAllowed = reminders.ensurePermission()
                val alarmsAllowed = reminder.kind != ReminderKind.Alarm || reminders.ensureAlarmPermission()
                if (!alarmsAllowed) {
                    _notices.emit(AlarmsOff)
                } else if (!notificationsAllowed) {
                    _notices.emit(NotificationsOff)
                } else if (reminder.kind == ReminderKind.Alarm && !reminders.fullScreenAlarmsAllowed()) {
                    _notices.emit(FullScreenOff)
                }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        const val AlarmsOff = "Alarms are turned off for OpenAdhd, so this reminder will be shown as a notification. Turn them on in your device's settings."

        const val FullScreenOff = "Full-screen alarms are turned off for OpenAdhd. The alarm will still ring, but it will not appear over the lock screen. Allow full-screen alerts in your device's settings."

        const val NotificationsOff = "Notifications are turned off for OpenAdhd. Turn them on in your device's settings to get reminders."

        fun isValidName(name: String): Boolean = name.isNotBlank()

        /** What is wrong with the schedule or reminder, or null; shown in the form so the user can fix it. */
        fun validateSchedule(schedule: TaskSchedule, reminder: TemplateReminder?): ScheduleProblem? =
            ScheduleRules.check(schedule, reminder)

        fun validate(name: String, tier: Tier, subtasks: List<String>, rules: GameRules = GameRules.Default): TemplateInputError? = when {
            !isValidName(name) -> TemplateInputError.NameRequired
            subtasks.any { it.isBlank() } -> TemplateInputError.SubtaskTitleRequired
            subtasks.size > CoinRules.maxSubtasks(tier, rules) -> TemplateInputError.TooManySubtasks
            else -> null
        }
    }
}
