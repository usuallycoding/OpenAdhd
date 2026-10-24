package com.usuallycoding.openadhd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.usuallycoding.openadhd.domain.HistoryDays
import com.usuallycoding.openadhd.ui.history.SelectableHistoryDays
import com.usuallycoding.openadhd.ui.theme.ChevronIcon
import kotlinx.datetime.LocalDate

/**
 * The date bar of a History tab: previous and next day arrows around a button showing [date], which opens a
 * calendar where only days in [daysWithEntries] can be picked (before [today], or up to it with [includeToday]).
 */
@Composable
fun DayNavigator(
    date: LocalDate,
    today: LocalDate,
    canGoNext: Boolean,
    daysWithEntries: Set<LocalDate>,
    includeToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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

    if (picking) {
        DayPickerDialog(
            date = date,
            today = today,
            daysWithEntries = daysWithEntries,
            includeToday = includeToday,
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
    includeToday: Boolean,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val millis = HistoryDays.toPickerMillis(date)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = millis,
        initialDisplayedMonthMillis = millis,
        selectableDates = SelectableHistoryDays(today, daysWithEntries, includeToday),
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
