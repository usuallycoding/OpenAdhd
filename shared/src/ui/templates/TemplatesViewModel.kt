package com.usuallycoding.openadhd.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.GameRules
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
import com.usuallycoding.openadhd.domain.moved
import kotlinx.coroutines.flow.MutableStateFlow
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
) : ViewModel() {
    private val rules: StateFlow<GameRules> =
        settings.map { it.rules }.stateIn(viewModelScope, SharingStarted.Eagerly, GameRules.Default)

    val templates: StateFlow<List<TaskTemplate>> =
        repository.templates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Whether there are enough items for the order to be worth changing. */
    val canReorder: StateFlow<Boolean> = templates.map { it.size >= 2 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val reorderRequested = MutableStateFlow(false)

    /** Whether the list shows move controls. Ends by itself when fewer than two templates are left; not saved. */
    val reordering: StateFlow<Boolean> = combine(reorderRequested, templates) { on, list -> on && list.size >= 2 }
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
        viewModelScope.launch {
            if (id == null) {
                repository.create(name, tier, energy, description, subtasks, schedule, reminder)
            } else {
                repository.update(TaskTemplate(id, name, tier, energy, description, subtasks, schedule, reminder))
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
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
