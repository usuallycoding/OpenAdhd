package com.usuallycoding.openadhd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.ui.theme.ChevronIcon
import kotlinx.datetime.LocalDate

/**
 * The date bar of a History tab: previous and next day arrows around a button showing [date], which opens a
 * calendar where any day before [today] (or up to it with [includeToday]) can be picked and days in
 * [daysWithEntries] carry a dot; [markedLabel] names what they have ("tasks", "redemptions") for screen readers.
 */
@Composable
fun DayNavigator(
    date: LocalDate,
    today: LocalDate,
    canGoNext: Boolean,
    daysWithEntries: Set<LocalDate>,
    includeToday: Boolean,
    markedLabel: String,
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
            markedLabel = markedLabel,
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
    markedLabel: String,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf(date) }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(chosen) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DayCalendar(
            selected = chosen,
            today = today,
            markedDays = daysWithEntries,
            includeToday = includeToday,
            markedLabel = markedLabel,
            onSelect = { chosen = it },
        )
    }
}

/** For example "Fri 23 Oct 2026". */
private fun LocalDate.label(): String {
    val weekday = dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$weekday $day $month $year"
}
