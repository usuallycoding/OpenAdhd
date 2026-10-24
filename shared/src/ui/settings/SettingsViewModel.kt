package com.usuallycoding.openadhd.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.DataFiles
import com.usuallycoding.openadhd.data.OpenResult
import com.usuallycoding.openadhd.data.SaveResult
import com.usuallycoding.openadhd.domain.BackupJson
import com.usuallycoding.openadhd.domain.BackupResult
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.SettingGroup
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.SettingsRepository
import com.usuallycoding.openadhd.domain.StoreState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One setting as the Settings screen shows it: its [value] and [default] as text. */
data class SettingRow(val key: SettingKey, val value: String, val default: String) {
    val isDefault: Boolean get() = value == default

    /** The value for display: a choice shows its label, and an empty name reads "Not set". */
    val valueLabel: String get() = key.display(value)

    val defaultLabel: String get() = key.display(default)
}

/** The settings of one group, in order. */
data class SettingSection(val group: SettingGroup, val rows: List<SettingRow>)

/** What the Data section of Settings is asking or telling the user, in a dialog. */
sealed interface DataDialog {
    /** A result or an error, with an OK button. */
    data class Notice(val title: String, val text: String) : DataDialog

    /** A valid backup was read; [data] replaces everything once the user confirms. */
    data class ConfirmImport(val summary: BackupSummary, val data: StoreState) : DataDialog

    /** The two-step confirmation of Delete all data. */
    data object ConfirmDelete : DataDialog
}

/** What a backup holds, shown before it replaces the current data. */
data class BackupSummary(
    val taskTemplates: Int,
    val rewardTemplates: Int,
    val todayEntries: Int,
    val redemptions: Int,
    val balance: Int,
) {
    companion object {
        fun of(state: StoreState) = BackupSummary(
            state.templates.size, state.rewards.size, state.planned.size, state.redemptions.size,
            CoinRules.balance(state.ledger),
        )
    }
}

class SettingsViewModel(
    private val store: OpenAdhdStore,
    private val files: DataFiles? = null,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val repository: SettingsRepository get() = store

    /** Whether this platform can save and open backup files; Export and Import are hidden when it cannot. */
    val canTransfer: Boolean = files != null

    private val _dialog = MutableStateFlow<DataDialog?>(null)
    val dialog: StateFlow<DataDialog?> = _dialog

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

    fun dismissDialog() {
        _dialog.value = null
    }

    /** Asks for a place to save a backup, and says whether it was saved. A cancelled dialog says nothing. */
    fun exportBackup() {
        viewModelScope.launch {
            when (saveBackup()) {
                SaveResult.Saved -> _dialog.value = DataDialog.Notice("Backup saved", "Your data was saved to the file you chose.")
                SaveResult.Failed -> _dialog.value = DataDialog.Notice("Backup not saved", "The file could not be written.")
                SaveResult.Cancelled -> Unit
            }
        }
    }

    private suspend fun saveBackup(): SaveResult {
        val files = files ?: return SaveResult.Cancelled
        val text = BackupJson.encode(store.exportData(), clock.now())
        return files.save("openadhd-${clock.todayIn(timeZone)}.json", text)
    }

    /** Asks for a backup file and checks all of it; a valid one is shown for confirmation, a bad one is explained. */
    fun chooseImport() {
        val files = files ?: return
        viewModelScope.launch {
            when (val opened = files.open()) {
                OpenResult.Cancelled -> Unit
                OpenResult.Failed -> _dialog.value = cannotImport("The file could not be read. It may be damaged.")
                is OpenResult.Text -> _dialog.value = when (val result = BackupJson.decode(opened.text)) {
                    is BackupResult.Invalid -> cannotImport(result.message)
                    is BackupResult.Valid -> DataDialog.ConfirmImport(BackupSummary.of(result.state), result.state)
                }
            }
        }
    }

    private fun cannotImport(message: String) = DataDialog.Notice("Cannot import this file", "$message Nothing was changed.")

    /** Replaces everything with the backup being shown. */
    fun confirmImport() {
        val pending = _dialog.value as? DataDialog.ConfirmImport ?: return
        viewModelScope.launch {
            store.importData(pending.data)
            _dialog.value = DataDialog.Notice("Backup imported", "Your data was replaced with the backup.")
        }
    }

    fun requestDelete() {
        _dialog.value = DataDialog.ConfirmDelete
    }

    /** Offers a backup before deleting; the confirmation stays open when the file is saved or cancelled. */
    fun exportBeforeDelete() {
        viewModelScope.launch {
            if (saveBackup() == SaveResult.Failed) {
                _dialog.value = DataDialog.Notice("Backup not saved", "The file could not be written. Nothing was deleted.")
            }
        }
    }

    fun confirmDelete() {
        if (_dialog.value != DataDialog.ConfirmDelete) return
        viewModelScope.launch {
            store.deleteAllData()
            _dialog.value = DataDialog.Notice("All data deleted", "The app is as new.")
        }
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
