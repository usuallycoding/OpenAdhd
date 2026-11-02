package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.clickable
import com.usuallycoding.openadhd.ui.components.putKeyboardAway
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.formatCoins
import com.usuallycoding.openadhd.domain.ReminderRules
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
import com.usuallycoding.openadhd.ui.theme.MoreIcon
import com.usuallycoding.openadhd.ui.theme.DiceIcon
import com.usuallycoding.openadhd.ui.today.RandomizerState
import com.usuallycoding.openadhd.ui.history.HistoryViewModel
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
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
    /** Shows the Templates screen with the form to create a task template, for a user who has none. */
    onTakeMeToTemplates: () -> Unit = {},
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
            if (page == 0) TodayList(viewModel, onTakeMeToTemplates) else HistoryScreen(historyViewModel)
        }
    }
}

@Composable
private fun TodayList(viewModel: TodayViewModel, onTakeMeToTemplates: () -> Unit, modifier: Modifier = Modifier) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val randomizer by viewModel.randomizer.collectAsStateWithLifecycle()
    val collapsed by viewModel.collapsedEntryIds.collectAsStateWithLifecycle()
    val reordering by viewModel.reordering.collectAsStateWithLifecycle()
    TodayContent(
        entries = entries,
        templates = templates,
        onAssign = viewModel::assign,
        onRollTemplate = viewModel::rollTemplate,
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
        pickedEntryIds = remember(viewModel) { merge(viewModel.picked.map { it.entryId }, viewModel.revealed) },
        onPickForMe = viewModel::openRandomizer,
        onChooseEnergy = viewModel::chooseEnergy,
        onChooseAnotherEnergy = viewModel::chooseAnotherEnergy,
        onDismissRandomizer = viewModel::dismissRandomizer,
        reminders = ReminderActions(
            available = viewModel.canRemind,
            presets = viewModel::reminderPresets,
            startAt = viewModel::reminderStartAt,
            currentTime = viewModel::currentTime,
            supportsAlarms = viewModel.canAlarm,
            openAlarmSettings = viewModel::openAlarmSettings,
            openNotificationSettings = viewModel::openNotificationSettings,
            save = viewModel::saveReminder,
            clear = viewModel::clearReminder,
        ),
        onTakeMeToTemplates = onTakeMeToTemplates,
        modifier = modifier,
    )
}

/** How long Today waits for an entry to appear in the list before ignoring a request to show it. */
private const val RevealWaitMillis = 2000L

/** How long the entry chosen by "Pick one for me" stays highlighted. */
private const val PickHighlightMillis = 1500L

@Composable
fun TodayContent(
    entries: List<PlannedTask>,
    templates: List<TaskTemplate>,
    onAssign: (TaskTemplate) -> Unit,
    /** Draws a template for the picker's dice from those it shows, avoiding the previous roll's id. */
    onRollTemplate: (shown: List<TaskTemplate>, previousId: String?) -> TaskTemplate? = { shown, _ -> shown.firstOrNull() },
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
    /** The ids of entries just picked by "Pick one for me", to scroll to and highlight. */
    pickedEntryIds: Flow<String> = emptyFlow(),
    onPickForMe: () -> Unit = {},
    onChooseEnergy: (Energy) -> Unit = {},
    onChooseAnotherEnergy: () -> Unit = {},
    onDismissRandomizer: () -> Unit = {},
    reminders: ReminderActions = ReminderActions.None,
    /** Called from the picker when there are no task templates; the picker closes first. */
    onTakeMeToTemplates: () -> Unit = {},
) {
    var picking by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    var highlightedId by remember { mutableStateOf<String?>(null) }
    val currentEntries by rememberUpdatedState(entries)
    LaunchedEffect(pickedEntryIds) {
        pickedEntryIds.collectLatest { id ->
            // At a cold start the list may not have loaded yet, so wait briefly for the entry; an unknown one is ignored.
            val index = withTimeoutOrNull(RevealWaitMillis) {
                snapshotFlow { currentEntries.indexOfFirst { it.id == id } }.first { it >= 0 }
            } ?: return@collectLatest
            highlightedId = id
            listState.animateScrollToItem(index)
            delay(PickHighlightMillis)
            highlightedId = null
        }
    }

    Box(modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            ) {
                Text("Nothing planned for today. Tap + to add a task.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 88.dp)) {
                val pendingIds = entries.filter { !it.isCompleted }.map { it.id }
                items(entries, key = { it.id }) { entry ->
                    val pendingIndex = pendingIds.indexOf(entry.id)
                    EntryItem(
                        entry = entry,
                        expanded = entry.id !in collapsedEntryIds,
                        highlighted = entry.id == highlightedId,
                        reordering = reordering,
                        canMoveUp = pendingIndex > 0,
                        canMoveDown = pendingIndex in 0 until pendingIds.lastIndex,
                        onMoveUp = { onMoveUp(entry.id) },
                        onMoveDown = { onMoveDown(entry.id) },
                        onToggleExpanded = { onToggleCollapsed(entry.id) },
                        onOptions = { optionsFor = it },
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
        onChooseAnotherEnergy = onChooseAnotherEnergy,
        onDismiss = onDismissRandomizer,
    )

    entries.firstOrNull { it.id == optionsFor && !it.isCompleted }?.let { entry ->
        EntrySheet(
            entry = entry,
            reminders = reminders,
            timeZone = timeZone,
            onDelete = { onRemove(entry.id) },
            onDismiss = { optionsFor = null },
        )
    }

    if (picking) {
        TemplatePickerDialog(
            templates = templates,
            onPick = { onAssign(it); picking = false },
            onRoll = onRollTemplate,
            onTakeMeThere = { picking = false; onTakeMeToTemplates() },
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
    /** Marks the entry that "Pick one for me" just chose. */
    highlighted: Boolean = false,
    /** Shows the entry as a record: checkboxes only display state, and nothing can be removed, moved or completed. */
    readOnly: Boolean = false,
    reordering: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    /** Opens the entry's options; the "..." button is shown only while this is set and the entry is not completed. */
    onOptions: ((String) -> Unit)? = null,
    onCompleteTask: (String) -> Unit = {},
    onCompleteSubtask: (String) -> Unit = {},
    onUncompleteTask: (String) -> Unit = {},
    onUncompleteSubtask: (String) -> Unit = {},
) {
    val hasSubtasks = entry.subtasks.isNotEmpty()
    // While reordering a row is only a name to move: nothing can be completed, removed or expanded.
    val showSubtasks = expanded && !reordering
    val chevronAngle by animateFloatAsState(if (expanded) 0f else -90f)
    val container by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
    )
    ListItem(
        colors = ListItemDefaults.colors(containerColor = container),
        // Tapping the header of an entry with subtasks shows or hides them; entries without subtasks are not tappable.
        modifier = (if (hasSubtasks && !reordering) {
            modifier
                .clickable(onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggleExpanded)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
        } else {
            modifier
        }).then(if (highlighted) Modifier.semantics { selected = true } else Modifier),
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
                val reminder = entry.reminder
                if (reminder != null && !entry.isCompleted && !readOnly) Text(ReminderRules.summary(reminder, timeZone))
            }
        },
        trailingContent = if (reordering) {
            if (entry.isCompleted) null else ({ MoveButtons(entry.name, canMoveUp, canMoveDown, onMoveUp, onMoveDown) })
        } else if (readOnly || entry.isCompleted || onOptions == null) {
            null
        } else ({
            IconButton(onClick = { onOptions(entry.id) }) {
                Icon(MoreIcon, contentDescription = "Options for ${entry.name}")
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
        trailingContent = { Text("+${formatCoins(subtask.reward)}") },
        modifier = Modifier.padding(start = 24.dp),
    )
}

internal fun PlannedTask.summary(timeZone: TimeZone): String {
    val base = "Tier ${tier.level} · ${energy.name} energy"
    val progress = if (subtasks.isEmpty()) {
        "$base · +${formatCoins(reward)} coins"
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
    onChooseAnotherEnergy: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        RandomizerState.Closed -> Unit

        RandomizerState.NothingPending -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pick one for me") },
            text = { Text("There is nothing left to pick today. Add a task with the + button.") },
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

        is RandomizerState.NothingFits -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nothing fits ${state.energy.name} energy") },
            text = { Text("None of today's pending tasks fits. Try a different energy.") },
            confirmButton = { TextButton(onClick = onChooseAnotherEnergy) { Text("Choose another energy") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplatePickerDialog(
    templates: List<TaskTemplate>,
    onPick: (TaskTemplate) -> Unit,
    onRoll: (shown: List<TaskTemplate>, previousId: String?) -> TaskTemplate?,
    onTakeMeThere: () -> Unit,
    onDismiss: () -> Unit,
) {
    // The query, the energy filter and the roll live in the sheet, so they start empty every time it opens.
    var query by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf<Energy?>(null) }
    var rolledId by remember { mutableStateOf<String?>(null) }
    var rollCount by remember { mutableStateOf(0) }
    var highlightedId by remember { mutableStateOf<String?>(null) }
    val shown = templates.matching(query, energy)

    LaunchedEffect(rollCount) {
        if (rollCount == 0) return@LaunchedEffect
        delay(PickHighlightMillis)
        highlightedId = null
    }

    FormSheet(
        title = "Add to today",
        onDismiss = onDismiss,
        dismissKeyboardOnBodyInteraction = true,
        // The search field, the energy chips and the dice stay put while the list below them scrolls.
        header = if (templates.isEmpty()) null else {
            {
                val focusRequester = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; highlightedId = null },
                    label = { Text("Search tasks") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.putKeyboardAway(keyboard) }),
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = ""; highlightedId = null }) {
                                Icon(ClearIcon, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Energy.entries.forEach { option ->
                            FilterChip(
                                selected = option == energy,
                                onClick = { energy = if (option == energy) null else option; highlightedId = null },
                                label = { Text(option.name) },
                            )
                        }
                    }
                    IconButton(
                        enabled = shown.isNotEmpty(),
                        onClick = {
                            onRoll(shown, rolledId)?.let { picked ->
                                rolledId = picked.id
                                highlightedId = picked.id
                                rollCount++
                            }
                        },
                    ) {
                        Icon(DiceIcon, contentDescription = "Roll a task from the list")
                    }
                }
            }
        },
        footer = { requestClose -> TextButton(onClick = requestClose) { Text("Close") } },
    ) {
        if (templates.isEmpty()) {
            Text("You have no task templates yet. Create one on the Templates tab first.")
            Button(onClick = onTakeMeThere, modifier = Modifier.padding(top = 12.dp)) { Text("Take me there") }
        } else if (shown.isEmpty()) {
            Text(noMatchMessage(query.trim(), energy))
        } else {
            // The sheet body already scrolls, so a plain column (not a LazyColumn) keeps long lists reachable.
            shown.forEach { template ->
                PickerRow(
                    template = template,
                    highlighted = template.id == highlightedId,
                    revealOn = if (template.id == rolledId) rollCount else null,
                    onPick = { onPick(template) },
                )
            }
        }
    }
}

/** "No task matches dis", "No Low energy task matches dis" or "No High energy task", for a search that found nothing. */
internal fun noMatchMessage(query: String, energy: Energy?): String {
    val kind = if (energy == null) "task" else "${energy.name} energy task"
    return if (query.isEmpty()) "No $kind" else "No $kind matches $query"
}

/** One template in the picker. [revealOn] changes with each roll that lands here, to scroll the row into view. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickerRow(
    template: TaskTemplate,
    highlighted: Boolean,
    revealOn: Int?,
    onPick: () -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(revealOn) {
        if (revealOn != null) requester.bringIntoView()
    }
    val container by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
    )
    ListItem(
        colors = ListItemDefaults.colors(containerColor = container),
        headlineContent = { Text(template.name) },
        supportingContent = { Text(template.description()) },
        // Selected while it is the rolled one, which also lets tests and screen readers tell it apart.
        modifier = Modifier.bringIntoViewRequester(requester).semantics { selected = highlighted }.clickable(onClick = onPick),
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
            randomizer = RandomizerState.ChoosingEnergy,
        )
    }
}
