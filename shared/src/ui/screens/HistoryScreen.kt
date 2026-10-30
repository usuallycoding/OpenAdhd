package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.ui.components.DayNavigator
import com.usuallycoding.openadhd.ui.history.HistoryViewModel
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
    Column(modifier.fillMaxSize()) {
        DayNavigator(
            date = date,
            today = today,
            canGoNext = canGoNext,
            daysWithEntries = daysWithEntries,
            includeToday = false,
            markedLabel = "tasks",
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onPickDay = onPickDay,
        )
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
}
