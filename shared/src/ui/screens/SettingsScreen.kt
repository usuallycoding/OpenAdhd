package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.ui.components.FormSheet
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
    }

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
