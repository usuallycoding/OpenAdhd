package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usuallycoding.openadhd.domain.HistoryDays
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.ui.history.HistoryViewModel
import com.usuallycoding.openadhd.ui.history.SelectableHistoryDays
import com.usuallycoding.openadhd.ui.theme.ChevronIcon
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val date by viewModel.date.collectAsStateWithLifecycle()
    val canGoNext by viewModel.canGoNext.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val collapsed by viewModel.collapsedEntryIds.collectAsStateWithLifecycle()
    val days by viewModel.daysWithEntries.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    HistoryContent(
        date = date,
        today = today,
        canGoNext = canGoNext,
        entries = entries,
        collapsedEntryIds = collapsed,
        daysWithEntries = days,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onPickDay = viewModel::pickDay,
        onToggleCollapsed = viewModel::toggleCollapsed,
        modifier = modifier,
    )
}

/** A read-only look at one earlier day: a date field to change it, and the day's entries without any controls. */
@Composable
fun HistoryContent(
    date: LocalDate,
    today: LocalDate,
    canGoNext: Boolean,
    entries: List<PlannedTask>,
    collapsedEntryIds: Set<String>,
    daysWithEntries: Set<LocalDate>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDay: (LocalDate) -> Unit,
    onToggleCollapsed: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    var picking by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(ChevronIcon, contentDescription = "Previous day", modifier = Modifier.rotate(90f))
            }
            OutlinedButton(onClick = { picking = true }) { Text(date.label()) }
            // Kept in the layout when hidden so the date field does not move.
            IconButton(onClick = onNextDay, enabled = canGoNext) {
                if (canGoNext) Icon(ChevronIcon, contentDescription = "Next day", modifier = Modifier.rotate(-90f))
            }
        }
        HorizontalDivider()
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    "Nothing was planned on this day",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            }
        } else {
            LazyColumn {
                items(entries, key = { it.id }) { entry ->
                    EntryItem(
                        entry = entry,
                        expanded = entry.id !in collapsedEntryIds,
                        onToggleExpanded = { onToggleCollapsed(entry.id) },
                        timeZone = timeZone,
                        readOnly = true,
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (picking) {
        DayPickerDialog(
            date = date,
            today = today,
            daysWithEntries = daysWithEntries,
            onPick = { onPickDay(it); picking = false },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    date: LocalDate,
    today: LocalDate,
    daysWithEntries: Set<LocalDate>,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val millis = HistoryDays.toPickerMillis(date)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = millis,
        initialDisplayedMonthMillis = millis,
        selectableDates = SelectableHistoryDays(today, daysWithEntries),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(HistoryDays.fromPickerMillis(it)) } ?: onDismiss() },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state)
    }
}

/** For example "Fri 23 Oct 2026". */
private fun LocalDate.label(): String {
    val weekday = dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$weekday $day $month $year"
}
