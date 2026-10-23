package com.usuallycoding.openadhd.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.TemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplatesViewModel(private val repository: TemplateRepository) : ViewModel() {
    val templates: StateFlow<List<TaskTemplate>> =
        repository.templates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Creates a template, or updates the one with [id]. Blank names are ignored; check [isValidName] first. */
    fun save(id: String?, name: String, tier: Tier, energy: Energy) {
        if (!isValidName(name)) return
        viewModelScope.launch {
            if (id == null) {
                repository.create(name, tier, energy)
            } else {
                repository.update(TaskTemplate(id, name, tier, energy))
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        fun isValidName(name: String): Boolean = name.isNotBlank()
    }
}
