package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PlannedSubtask
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.matching
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.components.FormSheet
import com.usuallycoding.openadhd.ui.components.MoveButtons
import com.usuallycoding.openadhd.ui.theme.AddIcon
import com.usuallycoding.openadhd.ui.theme.ChevronIcon
import com.usuallycoding.openadhd.ui.theme.ClearIcon
import com.usuallycoding.openadhd.ui.theme.DeleteIcon
import com.usuallycoding.openadhd.ui.theme.DiceIcon
import com.usuallycoding.openadhd.ui.today.RandomizerState
import com.usuallycoding.openadhd.ui.history.HistoryViewModel
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { 2 },
) {
    val scope = rememberCoroutineScope()
    Column(modifier) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Today") },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("History") },
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) TodayList(viewModel) else HistoryScreen(historyViewModel)
        }
    }
}

@Composable
private fun TodayList(viewModel: TodayViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val randomizer by viewModel.randomizer.collectAsStateWithLifecycle()
    val collapsed by viewModel.collapsedEntryIds.collectAsStateWithLifecycle()
    val reordering by viewModel.reordering.collectAsStateWithLifecycle()
    TodayContent(
        entries = entries,
        templates = templates,
        onAssign = viewModel::assign,
        onRemove = viewModel::remove,
        onCompleteTask = viewModel::completeTask,
        onCompleteSubtask = viewModel::completeSubtask,
        onUncompleteTask = viewModel::uncompleteTask,
        onUncompleteSubtask = viewModel::uncompleteSubtask,
        collapsedEntryIds = collapsed,
        onToggleCollapsed = viewModel::toggleCollapsed,
        reordering = reordering,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
        randomizer = randomizer,
        onPickForMe = viewModel::openRandomizer,
        onChooseEnergy = viewModel::chooseEnergy,
        onAnother = viewModel::another,
        onAcceptSuggestion = viewModel::acceptSuggestion,
        onChooseAnotherEnergy = viewModel::chooseAnotherEnergy,
        onDismissRandomizer = viewModel::dismissRandomizer,
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
    onUncompleteTask: (id: String) -> Unit = {},
    onUncompleteSubtask: (id: String) -> Unit = {},
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    collapsedEntryIds: Set<String> = emptySet(),
    onToggleCollapsed: (id: String) -> Unit = {},
    reordering: Boolean = false,
    onMoveUp: (id: String) -> Unit = {},
    onMoveDown: (id: String) -> Unit = {},
    randomizer: RandomizerState = RandomizerState.Closed,
    onPickForMe: () -> Unit = {},
    onChooseEnergy: (Energy) -> Unit = {},
    onAnother: () -> Unit = {},
    onAcceptSuggestion: () -> Unit = {},
    onChooseAnotherEnergy: () -> Unit = {},
    onDismissRandomizer: () -> Unit = {},
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
                Button(onClick = onPickForMe) { Text("Pick one for me") }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                val pendingIds = entries.filter { !it.isCompleted }.map { it.id }
                items(entries, key = { it.id }) { entry ->
                    val pendingIndex = pendingIds.indexOf(entry.id)
                    EntryItem(
                        entry = entry,
                        expanded = entry.id !in collapsedEntryIds,
                        reordering = reordering,
                        canMoveUp = pendingIndex > 0,
                        canMoveDown = pendingIndex in 0 until pendingIds.lastIndex,
                        onMoveUp = { onMoveUp(entry.id) },
                        onMoveDown = { onMoveDown(entry.id) },
                        onToggleExpanded = { onToggleCollapsed(entry.id) },
                        onRemove = onRemove,
                        onCompleteTask = onCompleteTask,
                        onCompleteSubtask = onCompleteSubtask,
                        onUncompleteTask = onUncompleteTask,
                        onUncompleteSubtask = onUncompleteSubtask,
                        timeZone = timeZone,
                    )
                    HorizontalDivider()
                }
            }
        }
        if (!reordering) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                SmallFloatingActionButton(onClick = onPickForMe) {
                    Icon(DiceIcon, contentDescription = "Pick one for me")
                }
                FloatingActionButton(onClick = { picking = true }) {
                    Icon(AddIcon, contentDescription = "Add task to today")
                }
            }
        }
    }

    RandomizerDialog(
        state = randomizer,
        onChooseEnergy = onChooseEnergy,
        onAnother = onAnother,
        onAccept = onAcceptSuggestion,
        onChooseAnotherEnergy = onChooseAnotherEnergy,
        onDismiss = onDismissRandomizer,
    )

    if (picking) {
        TemplatePickerDialog(
            templates = templates,
            onPick = { onAssign(it); picking = false },
            onDismiss = { picking = false },
        )
    }
}

@Composable
internal fun EntryItem(
    entry: PlannedTask,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    timeZone: TimeZone,
    modifier: Modifier = Modifier,
    /** Shows the entry as a record: checkboxes only display state, and nothing can be removed, moved or completed. */
    readOnly: Boolean = false,
    reordering: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRemove: (String) -> Unit = {},
    onCompleteTask: (String) -> Unit = {},
    onCompleteSubtask: (String) -> Unit = {},
    onUncompleteTask: (String) -> Unit = {},
    onUncompleteSubtask: (String) -> Unit = {},
) {
    val hasSubtasks = entry.subtasks.isNotEmpty()
    // While reordering a row is only a name to move: nothing can be completed, removed or expanded.
    val showSubtasks = expanded && !reordering
    val chevronAngle by animateFloatAsState(if (expanded) 0f else -90f)
    ListItem(
        // Tapping the header of an entry with subtasks shows or hides them; entries without subtasks are not tappable.
        modifier = if (hasSubtasks && !reordering) {
            modifier
                .clickable(onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggleExpanded)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
        } else {
            modifier
        },
        leadingContent = if (reordering) {
            null
        } else if (hasSubtasks) {
            { Icon(ChevronIcon, contentDescription = null, modifier = Modifier.rotate(chevronAngle)) }
        } else ({
            Checkbox(
                checked = entry.isCompleted,
                onCheckedChange = if (readOnly) null else { _ ->
                    if (entry.isCompleted) onUncompleteTask(entry.id) else onCompleteTask(entry.id)
                },
                enabled = !readOnly,
            )
        }),
        headlineContent = {
            Text(entry.name, textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else null)
        },
        supportingContent = {
            Column {
                if (entry.description.isNotBlank()) Text(entry.description)
                Text(entry.summary(timeZone))
            }
        },
        trailingContent = if (reordering) {
            if (entry.isCompleted) null else ({ MoveButtons(entry.name, canMoveUp, canMoveDown, onMoveUp, onMoveDown) })
        } else if (readOnly || entry.hasProgress) {
            null
        } else ({
            IconButton(onClick = { onRemove(entry.id) }) {
                Icon(DeleteIcon, contentDescription = "Remove ${entry.name} from today")
            }
        }),
    )
    AnimatedVisibility(visible = showSubtasks) {
        Column {
            entry.subtasks.forEach { subtask ->
                SubtaskItem(
                    subtask,
                    readOnly = readOnly,
                    onToggle = { if (subtask.isCompleted) onUncompleteSubtask(subtask.id) else onCompleteSubtask(subtask.id) },
                )
            }
        }
    }
}

@Composable
private fun SubtaskItem(subtask: PlannedSubtask, readOnly: Boolean, onToggle: () -> Unit) {
    ListItem(
        leadingContent = {
            Checkbox(
                checked = subtask.isCompleted,
                onCheckedChange = if (readOnly) null else { _ -> onToggle() },
                enabled = !readOnly,
            )
        },
        headlineContent = {
            Text(subtask.title, textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null)
        },
        trailingContent = { Text("+${subtask.reward}") },
        modifier = Modifier.padding(start = 24.dp),
    )
}

internal fun PlannedTask.summary(timeZone: TimeZone): String {
    val base = "Tier ${tier.level} · ${energy.name} energy"
    val progress = if (subtasks.isEmpty()) {
        "$base · +$reward coins"
    } else {
        "$base · ${subtasks.count { it.isCompleted }} of ${subtasks.size} done"
    }
    val doneAt = doneAt ?: return progress
    val time = doneAt.toLocalDateTime(timeZone).time
    return "$progress · Done ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

@Composable
private fun RandomizerDialog(
    state: RandomizerState,
    onChooseEnergy: (Energy) -> Unit,
    onAnother: () -> Unit,
    onAccept: () -> Unit,
    onChooseAnotherEnergy: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        RandomizerState.Closed -> Unit

        RandomizerState.NoTemplates -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pick one for me") },
            text = { Text("You have no task templates yet. Create one on the Templates tab first.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )

        RandomizerState.ChoosingEnergy -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("How much energy do you have?") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Energy.entries.forEach { energy ->
                        FilterChip(selected = false, onClick = { onChooseEnergy(energy) }, label = { Text(energy.name) })
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
        )

        is RandomizerState.Suggesting -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("How about this one?") },
            text = {
                val template = state.template
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    if (template.description.isNotBlank()) Text(template.description)
                    Text(template.description())
                    Text("${CoinRules.baseReward(template.tier)} coins")
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = onAnother) { Text("Another") }
                    TextButton(onClick = onAccept) { Text("Add to today") }
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
        )

        is RandomizerState.NothingFits -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(if (state.exhausted) "That was everything that fits" else "Nothing fits ${state.energy.name} energy")
            },
            text = { Text("Try a different energy, or pick from your list with the + button.") },
            confirmButton = { TextButton(onClick = onChooseAnotherEnergy) { Text("Choose another energy") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
    }
}

@Composable
private fun TemplatePickerDialog(
    templates: List<TaskTemplate>,
    onPick: (TaskTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    FormSheet(
        title = "Add to today",
        onDismiss = onDismiss,
        footer = { requestClose -> TextButton(onClick = requestClose) { Text("Close") } },
    ) {
        if (templates.isEmpty()) {
            Text("You have no task templates yet. Create one on the Templates tab first.")
        } else {
            // The query lives in the sheet, so it starts empty every time the sheet opens.
            var query by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search tasks") },
                singleLine = true,
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(ClearIcon, contentDescription = "Clear search")
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
            val shown = templates.matching(query)
            if (shown.isEmpty()) {
                Text("No task matches ${query.trim()}")
            } else {
                // The sheet body already scrolls, so a plain column (not a LazyColumn) keeps long lists reachable.
                shown.forEach { template ->
                    ListItem(
                        headlineContent = { Text(template.name) },
                        supportingContent = { Text(template.description()) },
                        modifier = Modifier.clickable { onPick(template) },
                    )
                }
            }
        }
    }
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
            randomizer = RandomizerState.Suggesting(
                Energy.Medium,
                TaskTemplate("t", "Clean the kitchen", Tier.Three, Energy.Medium, "Whole room", listOf("Dishes", "Counters")),
            ),
        )
    }
}
