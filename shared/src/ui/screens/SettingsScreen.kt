package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.ui.components.FormSheet
import com.usuallycoding.openadhd.ui.settings.DataDialog
import com.usuallycoding.openadhd.ui.settings.SettingRow
import com.usuallycoding.openadhd.ui.settings.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SettingRow?>(null) }

    LazyColumn(modifier = modifier) {
        sections.forEach { section ->
            item(key = "group-${section.group.name}") {
                Text(
                    section.group.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(section.rows, key = { it.key.name }) { row ->
                ListItem(
                    headlineContent = { Text(row.key.label) },
                    supportingContent = { Text("Default: ${row.defaultLabel}") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.valueLabel, style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = { viewModel.restore(row.key) },
                                enabled = !row.isDefault,
                                modifier = Modifier.semantics { contentDescription = "Restore ${row.key.label}" },
                            ) { Text("Restore") }
                        }
                    },
                    modifier = Modifier.clickable { editing = row },
                )
                HorizontalDivider()
            }
        }
        item(key = "group-Data") {
            Text(
                "Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        if (viewModel.canTransfer) {
            item(key = "data-export") {
                DataRow("Export a backup", "Save everything to a file", viewModel::exportBackup)
            }
            item(key = "data-import") {
                DataRow("Import a backup", "Replace everything with a saved backup", viewModel::chooseImport)
            }
        }
        item(key = "data-delete") {
            DataRow("Delete all data", "Remove everything and start from scratch", viewModel::requestDelete, danger = true)
        }
    }

    val dataDialog by viewModel.dialog.collectAsStateWithLifecycle()
    dataDialog?.let { DataDialogs(it, viewModel) }

    editing?.let { row ->
        SettingDialog(
            row = row,
            onDismiss = { editing = null },
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun SettingDialog(
    row: SettingRow,
    onDismiss: () -> Unit,
    onSave: (key: SettingKey, text: String, onResult: (String?) -> Unit) -> Unit,
) {
    var text by remember { mutableStateOf(row.value) }
    var error by remember { mutableStateOf<String?>(null) }

    FormSheet(
        title = row.key.label,
        onDismiss = onDismiss,
        footer = { requestClose ->
            TextButton(onClick = requestClose) { Text("Cancel") }
            TextButton(onClick = {
                onSave(row.key, text) { problem ->
                    if (problem == null) requestClose() else error = problem
                }
            }) { Text("Save") }
        },
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; error = null },
            label = { Text(row.key.label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (row.key.isText) KeyboardType.Text else KeyboardType.Number),
            isError = error != null,
            supportingText = { Text(error ?: "Default: ${row.defaultLabel}") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DataRow(title: String, detail: String, onClick: () -> Unit, danger: Boolean = false) {
    ListItem(
        headlineContent = {
            Text(title, color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified)
        },
        supportingContent = { Text(detail) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

private fun count(number: Int, one: String, many: String) = "$number ${if (number == 1) one else many}"

@Composable
private fun DataDialogs(dialog: DataDialog, viewModel: SettingsViewModel) {
    when (dialog) {
        is DataDialog.Notice -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(dialog.title) },
            text = { Text(dialog.text) },
            confirmButton = { TextButton(onClick = viewModel::dismissDialog) { Text("OK") } },
        )

        is DataDialog.ConfirmImport -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Replace all data with this backup?") },
            text = {
                val summary = dialog.summary
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(count(summary.taskTemplates, "task template", "task templates"))
                    Text(count(summary.rewardTemplates, "reward template", "reward templates"))
                    Text(count(summary.todayEntries, "Today entry", "Today entries"))
                    Text(count(summary.redemptions, "redemption", "redemptions"))
                    Text("Balance: ${summary.balance} coins")
                    Text(
                        "Everything now in the app will be replaced.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmImport) { Text("Replace everything") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") } },
        )

        DataDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Delete all data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This removes every task, reward, history entry, coin and setting. " +
                            "It cannot be undone.",
                    )
                    if (viewModel.canTransfer) {
                        TextButton(onClick = viewModel::exportBeforeDelete) { Text("Export a backup first") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") } },
        )
    }
}
