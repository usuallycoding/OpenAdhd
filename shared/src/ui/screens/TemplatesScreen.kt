package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.templates.TemplateInputError
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import com.usuallycoding.openadhd.ui.theme.AddIcon
import com.usuallycoding.openadhd.ui.theme.DeleteIcon
import com.usuallycoding.openadhd.ui.theme.EditIcon

@Composable
fun TemplatesScreen(viewModel: TemplatesViewModel, modifier: Modifier = Modifier) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    TemplatesContent(
        templates = templates,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@Composable
fun TemplatesContent(
    templates: List<TaskTemplate>,
    onSave: (id: String?, name: String, tier: Tier, energy: Energy, description: String, subtasks: List<String>) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // null = closed; a template with a blank id = creating; otherwise editing that template
    var editing by remember { mutableStateOf<TaskTemplate?>(null) }

    Box(modifier.fillMaxSize()) {
        if (templates.isEmpty()) {
            Text(
                "No templates yet. Tap + to create your first task.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(templates, key = { it.id }) { template ->
                    ListItem(
                        headlineContent = { Text(template.name) },
                        supportingContent = { Text(template.description()) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editing = template }) {
                                    Icon(EditIcon, contentDescription = "Edit ${template.name}")
                                }
                                IconButton(onClick = { onDelete(template.id) }) {
                                    Icon(DeleteIcon, contentDescription = "Delete ${template.name}")
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = TaskTemplate("", "", Tier.One, Energy.Medium) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(AddIcon, contentDescription = "Add template")
        }
    }

    editing?.let { template ->
        TemplateDialog(
            initial = template,
            onDismiss = { editing = null },
            onConfirm = { name, tier, energy, description, subtasks ->
                onSave(template.id.ifEmpty { null }, name, tier, energy, description, subtasks)
                editing = null
            },
        )
    }
}

@Composable
private fun TemplateDialog(
    initial: TaskTemplate,
    onDismiss: () -> Unit,
    onConfirm: (name: String, tier: Tier, energy: Energy, description: String, subtasks: List<String>) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var tier by remember { mutableStateOf(initial.tier) }
    var energy by remember { mutableStateOf(initial.energy) }
    val subtasks = remember { mutableStateListOf<String>().apply { addAll(initial.subtasks) } }
    var error by remember { mutableStateOf<TemplateInputError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isEmpty()) "New task" else "Edit task") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error == TemplateInputError.NameRequired,
                    supportingText = if (error == TemplateInputError.NameRequired) ({ Text("A name is required") }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Difficulty tier", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tier.entries.forEach { option ->
                        FilterChip(
                            selected = option == tier,
                            onClick = { tier = option; error = null },
                            label = { Text("${option.level}") },
                        )
                    }
                }
                Text("Energy needed", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Energy.entries.forEach { option ->
                        FilterChip(
                            selected = option == energy,
                            onClick = { energy = option },
                            label = { Text(option.name) },
                        )
                    }
                }
                val limit = CoinRules.maxSubtasks(tier)
                Text("Subtasks (${subtasks.size}/$limit)", style = MaterialTheme.typography.labelLarge)
                subtasks.forEachIndexed { index, title ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { subtasks[index] = it; error = null },
                            label = { Text("Subtask ${index + 1}") },
                            singleLine = true,
                            isError = error == TemplateInputError.SubtaskTitleRequired && title.isBlank(),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { subtasks.removeAt(index); error = null }) {
                            Icon(DeleteIcon, contentDescription = "Remove subtask ${index + 1}")
                        }
                    }
                }
                if (error == TemplateInputError.SubtaskTitleRequired) {
                    Text(
                        "Every subtask needs a title",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (error == TemplateInputError.TooManySubtasks) {
                    Text(
                        "A tier ${tier.level} task can have at most $limit subtasks. " +
                            "Remove some or choose a higher tier.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(
                    onClick = { subtasks.add("") },
                    enabled = subtasks.size < limit,
                ) {
                    Icon(AddIcon, contentDescription = null)
                    Text("Add subtask", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val problem = TemplatesViewModel.validate(name, tier, subtasks.toList())
                if (problem == null) onConfirm(name, tier, energy, description, subtasks.toList()) else error = problem
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal fun TaskTemplate.description(): String {
    val base = "Tier ${tier.level} · ${energy.name} energy"
    return when (subtasks.size) {
        0 -> base
        1 -> "$base · 1 subtask"
        else -> "$base · ${subtasks.size} subtasks"
    }
}

@Preview
@Composable
private fun TemplatesContentPreview() {
    MaterialTheme {
        TemplatesContent(
            templates = listOf(
                TaskTemplate("1", "Reply to emails", Tier.Two, Energy.Medium, "Inbox zero", listOf("Reply to boss", "Archive newsletters")),
                TaskTemplate("2", "File taxes", Tier.Four, Energy.High),
            ),
            onSave = { _, _, _, _, _, _ -> },
            onDelete = {},
        )
    }
}
