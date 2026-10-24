package com.usuallycoding.openadhd.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.SettingGroup
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One setting as the Settings screen shows it: its [value] and [default] as text. */
data class SettingRow(val key: SettingKey, val value: String, val default: String) {
    val isDefault: Boolean get() = value == default

    /** The value for display; an empty name reads "Not set". */
    val valueLabel: String get() = value.ifEmpty { "Not set" }

    val defaultLabel: String get() = default.ifEmpty { "Not set" }
}

/** The settings of one group, in order. */
data class SettingSection(val group: SettingGroup, val rows: List<SettingRow>)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val sections: StateFlow<List<SettingSection>> = repository.settings
        .map(::buildSections)
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildSections(Settings.Default))

    /** Saves [text] as the value of [key]; [onResult] gets why it was refused, or null when it was saved. */
    fun save(key: SettingKey, text: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repository.saveSetting(key, text)) }
    }

    fun restore(key: SettingKey) {
        viewModelScope.launch { repository.restoreSetting(key) }
    }

    companion object {
        fun buildSections(settings: Settings): List<SettingSection> =
            SettingGroup.entries.map { group ->
                SettingSection(
                    group,
                    SettingKey.entries.filter { it.group == group }.map { key ->
                        SettingRow(key, settings.text(key), Settings.Default.text(key))
                    },
                )
            }
    }
}
