package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PlannedSubtask
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.theme.AddIcon
import com.usuallycoding.openadhd.ui.theme.DeleteIcon
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Composable
fun TodayScreen(viewModel: TodayViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    TodayContent(
        entries = entries,
        templates = templates,
        onAssign = viewModel::assign,
        onRemove = viewModel::remove,
        onCompleteTask = viewModel::completeTask,
        onCompleteSubtask = viewModel::completeSubtask,
        modifier = modifier,
    )
}

@Composable
fun TodayContent(
    entries: List<PlannedTask>,
    templates: List<TaskTemplate>,
    onAssign: (TaskTemplate) -> Unit,
    onRemove: (id: String) -> Unit,
    onCompleteTask: (id: String) -> Unit,
    onCompleteSubtask: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            ) {
                Text("Nothing planned for today.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { picking = true }) { Text("Add a task") }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(entries, key = { it.id }) { entry ->
                    EntryItem(entry, onRemove, onCompleteTask, onCompleteSubtask)
                    HorizontalDivider()
                }
            }
        }
        FloatingActionButton(
            onClick = { picking = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(AddIcon, contentDescription = "Add task to today")
        }
    }

    if (picking) {
        TemplatePickerDialog(
            templates = templates,
            onPick = { onAssign(it); picking = false },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun EntryItem(
    entry: PlannedTask,
    onRemove: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onCompleteSubtask: (String) -> Unit,
) {
    val hasSubtasks = entry.subtasks.isNotEmpty()
    ListItem(
        leadingContent = if (hasSubtasks) null else ({
            Checkbox(
                checked = entry.isCompleted,
                enabled = !entry.isCompleted,
                onCheckedChange = { onCompleteTask(entry.id) },
            )
        }),
        headlineContent = {
            Text(entry.name, textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else null)
        },
        supportingContent = {
            Column {
                if (entry.description.isNotBlank()) Text(entry.description)
                Text(entry.summary())
            }
        },
        trailingContent = if (entry.hasProgress) null else ({
            IconButton(onClick = { onRemove(entry.id) }) {
                Icon(DeleteIcon, contentDescription = "Remove ${entry.name} from today")
            }
        }),
    )
    entry.subtasks.forEach { subtask ->
        SubtaskItem(subtask, onComplete = { onCompleteSubtask(subtask.id) })
    }
}

@Composable
private fun SubtaskItem(subtask: PlannedSubtask, onComplete: () -> Unit) {
    ListItem(
        leadingContent = {
            Checkbox(
                checked = subtask.isCompleted,
                enabled = !subtask.isCompleted,
                onCheckedChange = { onComplete() },
            )
        },
        headlineContent = {
            Text(subtask.title, textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null)
        },
        trailingContent = { Text("+${subtask.reward}") },
        modifier = Modifier.padding(start = 24.dp),
    )
}

private fun PlannedTask.summary(): String {
    val base = "Tier ${tier.level} · ${energy.name} energy"
    return if (subtasks.isEmpty()) {
        "$base · +$reward coins"
    } else {
        "$base · ${subtasks.count { it.isCompleted }} of ${subtasks.size} done"
    }
}

@Composable
private fun TemplatePickerDialog(
    templates: List<TaskTemplate>,
    onPick: (TaskTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to today") },
        text = {
            if (templates.isEmpty()) {
                Text("You have no task templates yet. Create one on the Templates tab first.")
            } else {
                LazyColumn {
                    items(templates, key = { it.id }) { template ->
                        ListItem(
                            headlineContent = { Text(template.name) },
                            supportingContent = { Text(template.description()) },
                            modifier = Modifier.clickable { onPick(template) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Preview
@Composable
private fun TodayContentPreview() {
    val date = LocalDate(2026, 10, 23)
    MaterialTheme {
        TodayContent(
            entries = listOf(
                PlannedTask("1", date, "Reply to emails", Tier.Two, Energy.Medium, null, Instant.fromEpochSeconds(0)),
                PlannedTask(
                    "2", date, "Clean the kitchen", Tier.Three, Energy.Medium, null,
                    description = "Whole room", reward = 20,
                    subtasks = listOf(
                        PlannedSubtask("a", "Dishes", 7, Instant.fromEpochSeconds(0)),
                        PlannedSubtask("b", "Wipe counters", 7),
                        PlannedSubtask("c", "Take out trash", 6),
                    ),
                ),
            ),
            templates = emptyList(),
            onAssign = {},
            onRemove = {},
            onCompleteTask = {},
            onCompleteSubtask = {},
        )
    }
}
