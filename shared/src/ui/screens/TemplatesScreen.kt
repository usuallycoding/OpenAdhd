package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.ui.LocalGameRules
import com.usuallycoding.openadhd.domain.HistoryDays
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.ScheduleProblem
import com.usuallycoding.openadhd.domain.ScheduleRules
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.TemplateReminder
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.templates.TemplateInputError
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import com.usuallycoding.openadhd.ui.components.FormSheet
import com.usuallycoding.openadhd.ui.components.MoveButtons
import com.usuallycoding.openadhd.ui.theme.AddIcon
import kotlinx.coroutines.launch
import com.usuallycoding.openadhd.ui.theme.DeleteIcon
import com.usuallycoding.openadhd.ui.theme.EditIcon

/**
 * The Templates destination: a Tasks tab and a Rewards tab that can be swiped between. [pagerState] is created by
 * the caller so the selected tab survives leaving this screen while the app runs.
 */
@Composable
fun TemplatesScreen(
    taskViewModel: TemplatesViewModel,
    rewardViewModel: RewardTemplatesViewModel,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { 2 },
) {
    val scope = rememberCoroutineScope()
    Column(modifier) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Tasks") },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Rewards") },
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) TaskTemplatesScreen(taskViewModel) else RewardsScreen(rewardViewModel)
        }
    }
}

@Composable
fun TaskTemplatesScreen(viewModel: TemplatesViewModel, modifier: Modifier = Modifier) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val reordering by viewModel.reordering.collectAsStateWithLifecycle()
    TemplatesContent(
        templates = templates,
        reordering = reordering,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        modifier = modifier,
        today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    )
}

@Composable
fun TemplatesContent(
    templates: List<TaskTemplate>,
    onSave: (
        id: String?, name: String, tier: Tier, energy: Energy, description: String, subtasks: List<String>,
        schedule: TaskSchedule, reminder: TemplateReminder?,
    ) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    reordering: Boolean = false,
    onMoveUp: (id: String) -> Unit = {},
    onMoveDown: (id: String) -> Unit = {},
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
                itemsIndexed(templates, key = { _, template -> template.id }) { index, template ->
                    ListItem(
                        headlineContent = { Text(template.name) },
                        supportingContent = {
                            Column {
                                Text(template.description())
                                ScheduleRules.summary(template.schedule, today)?.let { Text(it) }
                            }
                        },
                        trailingContent = {
                            if (reordering) {
                                MoveButtons(
                                    name = template.name,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < templates.lastIndex,
                                    onMoveUp = { onMoveUp(template.id) },
                                    onMoveDown = { onMoveDown(template.id) },
                                )
                            } else {
                                Row {
                                    IconButton(onClick = { editing = template }) {
                                        Icon(EditIcon, contentDescription = "Edit ${template.name}")
                                    }
                                    IconButton(onClick = { onDelete(template.id) }) {
                                        Icon(DeleteIcon, contentDescription = "Delete ${template.name}")
                                    }
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
        if (!reordering) {
            FloatingActionButton(
                onClick = { editing = TaskTemplate("", "", Tier.One, Energy.Medium) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(AddIcon, contentDescription = "Add template")
            }
        }
    }

    editing?.let { template ->
        TemplateDialog(
            initial = template,
            today = today,
            onDismiss = { editing = null },
            onConfirm = { name, tier, energy, description, subtasks, schedule, reminder ->
                onSave(template.id.ifEmpty { null }, name, tier, energy, description, subtasks, schedule, reminder)
            },
        )
    }
}

@Composable
private fun TemplateDialog(
    initial: TaskTemplate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String, tier: Tier, energy: Energy, description: String, subtasks: List<String>,
        schedule: TaskSchedule, reminder: TemplateReminder?,
    ) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var tier by remember { mutableStateOf(initial.tier) }
    var energy by remember { mutableStateOf(initial.energy) }
    val subtasks = remember { mutableStateListOf<String>().apply { addAll(initial.subtasks) } }
    var error by remember { mutableStateOf<TemplateInputError?>(null) }
    val rules = LocalGameRules.current

    // Each mode keeps its own values while the sheet is open, so switching modes by mistake loses nothing.
    var mode by remember { mutableStateOf(ScheduleMode.of(initial.schedule)) }
    var onDate by remember { mutableStateOf((initial.schedule as? TaskSchedule.OnDate)?.date ?: today) }
    var weekdays by remember { mutableStateOf((initial.schedule as? TaskSchedule.Weekly)?.days ?: emptySet()) }
    var periodStart by remember { mutableStateOf((initial.schedule as? TaskSchedule.Period)?.start ?: today) }
    var periodEnd by remember { mutableStateOf((initial.schedule as? TaskSchedule.Period)?.end ?: today.plus(1, DateTimeUnit.DAY)) }
    var reminderTime by remember { mutableStateOf(initial.reminder?.minuteOfDay) }
    var reminderRepeat by remember { mutableStateOf(if (initial.reminder != null) initial.reminder.repeatMinutes else ReminderRules.DefaultRepeat) }
    var scheduleProblem by remember { mutableStateOf<ScheduleProblem?>(null) }
    var pickingTime by remember { mutableStateOf(false) }

    fun schedule(): TaskSchedule = when (mode) {
        ScheduleMode.None -> TaskSchedule.None
        ScheduleMode.Date -> TaskSchedule.OnDate(onDate)
        ScheduleMode.Weekly -> TaskSchedule.Weekly(weekdays)
        ScheduleMode.Period -> TaskSchedule.Period(periodStart, periodEnd)
        ScheduleMode.Daily -> TaskSchedule.Daily
    }

    fun reminder(): TemplateReminder? = reminderTime?.let { TemplateReminder(it, reminderRepeat) }

    FormSheet(
        title = if (initial.id.isEmpty()) "New task" else "Edit task",
        onDismiss = onDismiss,
        footer = { requestClose ->
            TextButton(onClick = requestClose) { Text("Cancel") }
            TextButton(onClick = {
                val problem = TemplatesViewModel.validate(name, tier, subtasks.toList(), rules)
                val badSchedule = TemplatesViewModel.validateSchedule(schedule(), reminder())
                if (problem == null && badSchedule == null) {
                    onConfirm(name, tier, energy, description, subtasks.toList(), schedule(), reminder())
                    requestClose()
                } else {
                    error = problem
                    scheduleProblem = badSchedule
                }
            }) { Text("Save") }
        },
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
        val limit = CoinRules.maxSubtasks(tier, rules)
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

        Text("Reminder settings", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { pickingTime = true }) {
                Text(reminderTime?.let { ScheduleRules.clock(TemplateReminder(it)) } ?: "Set a time")
            }
            if (reminderTime != null) {
                TextButton(onClick = { reminderTime = null; scheduleProblem = null }) { Text("Clear time") }
            }
        }
        if (reminderTime != null) {
            Text("Repeat reminder", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = reminderRepeat == null, onClick = { reminderRepeat = null }, label = { Text("Off") })
                ReminderRules.RepeatChoices.forEach { minutes ->
                    FilterChip(
                        selected = reminderRepeat == minutes,
                        onClick = { reminderRepeat = minutes },
                        label = { Text("Every ${ReminderRules.repeatLabel(minutes)}") },
                    )
                }
            }
        }

        Text("Add to Today schedule", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleMode.entries.forEach { option ->
                FilterChip(
                    selected = option == mode,
                    onClick = { mode = option; scheduleProblem = null },
                    label = { Text(option.label) },
                )
            }
        }
        when (mode) {
            ScheduleMode.None -> Text("Only when you add it by hand.", style = MaterialTheme.typography.bodySmall)
            ScheduleMode.Date -> DateButton("Date", onDate) { onDate = it; scheduleProblem = null }
            ScheduleMode.Weekly -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleRules.Weekdays.forEach { day ->
                    FilterChip(
                        selected = day in weekdays,
                        onClick = { weekdays = if (day in weekdays) weekdays - day else weekdays + day; scheduleProblem = null },
                        label = { Text(ScheduleRules.dayName(day)) },
                    )
                }
            }
            ScheduleMode.Period -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateButton("From", periodStart) { periodStart = it; scheduleProblem = null }
                DateButton("To", periodEnd) { periodEnd = it; scheduleProblem = null }
            }
            ScheduleMode.Daily -> Text("Every day.", style = MaterialTheme.typography.bodySmall)
        }
        scheduleProblem?.let {
            Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (pickingTime) {
        val start = reminderTime ?: (9 * 60)
        val picker = rememberTimePickerState(start / 60, start % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = picker.hour * 60 + picker.minute
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
}

/** How a template is added to Today, as the form offers it. */
private enum class ScheduleMode(val label: String) {
    None("None"), Date("Date"), Weekly("Weekly"), Period("Period"), Daily("Daily");

    companion object {
        fun of(schedule: TaskSchedule): ScheduleMode = when (schedule) {
            TaskSchedule.None -> None
            is TaskSchedule.OnDate -> Date
            is TaskSchedule.Weekly -> Weekly
            is TaskSchedule.Period -> Period
            TaskSchedule.Daily -> Daily
        }
    }
}

/** A button showing [label] and the chosen [date] that opens a calendar to change it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateButton(label: String, date: LocalDate, onPick: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { picking = true }) { Text("$label: ${ScheduleRules.dateText(date)}") }
    if (picking) {
        val millis = HistoryDays.toPickerMillis(date)
        val state = rememberDatePickerState(initialSelectedDateMillis = millis, initialDisplayedMonthMillis = millis)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(HistoryDays.fromPickerMillis(it)) }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
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
            onSave = { _, _, _, _, _, _, _, _ -> },
            onDelete = {},
        )
    }
}
