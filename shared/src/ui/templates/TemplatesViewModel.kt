package com.usuallycoding.openadhd.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.TemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TemplateInputError { NameRequired, SubtaskTitleRequired, TooManySubtasks }

class TemplatesViewModel(private val repository: TemplateRepository) : ViewModel() {
    val templates: StateFlow<List<TaskTemplate>> =
        repository.templates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
    ) {
        if (validate(name, tier, subtasks) != null) return
        viewModelScope.launch {
            if (id == null) {
                repository.create(name, tier, energy, description, subtasks)
            } else {
                repository.update(TaskTemplate(id, name, tier, energy, description, subtasks))
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        fun isValidName(name: String): Boolean = name.isNotBlank()

        fun validate(name: String, tier: Tier, subtasks: List<String>): TemplateInputError? = when {
            !isValidName(name) -> TemplateInputError.NameRequired
            subtasks.any { it.isBlank() } -> TemplateInputError.SubtaskTitleRequired
            subtasks.size > CoinRules.maxSubtasks(tier) -> TemplateInputError.TooManySubtasks
            else -> null
        }
    }
}
