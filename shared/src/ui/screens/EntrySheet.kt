package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderPreset
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.ui.components.FormSheet
import com.usuallycoding.openadhd.ui.today.ReminderSaveResult
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** What the entry sheet needs to set reminders; [None] (no reminder controls) where reminders cannot be delivered. */
class ReminderActions(
    val available: Boolean,
    val presets: () -> List<ReminderPreset>,
    val startAt: (hour: Int, minute: Int) -> Instant,
    val currentTime: () -> LocalTime,
    val save: (entryId: String, start: Instant, repeatMinutes: Int?, kind: ReminderKind, onResult: (ReminderSaveResult) -> Unit) -> Unit,
    val clear: (entryId: String) -> Unit,
    /** Whether the Notification / Alarm choice is offered; false where only notifications exist, as on the desktop. */
    val supportsAlarms: Boolean = false,
    /** Opens the system setting for full-screen alerts, offered after an alarm is saved without them. */
    val openAlarmSettings: () -> Unit = {},
) {
    companion object {
        val None = ReminderActions(
            available = false,
            presets = { emptyList() },
            startAt = { _, _ -> error("No reminders here") },
            currentTime = { LocalTime(0, 0) },
            save = { _, _, _, _, _ -> },
            clear = {},
        )
    }
}

/** The options of a pending entry in a bottom sheet: its reminder, and deleting it while nothing in it is done. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntrySheet(
    entry: PlannedTask,
    reminders: ReminderActions,
    timeZone: TimeZone,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val current = entry.reminder
    var start by remember { mutableStateOf(current?.startAt) }
    var repeat by remember { mutableStateOf(if (current != null) current.repeatMinutes else ReminderRules.DefaultRepeat) }
    var kind by remember { mutableStateOf(current?.kind ?: ReminderKind.Notification) }
    var message by remember { mutableStateOf<String?>(null) }
    var notificationsOff by remember { mutableStateOf(false) }
    var fullScreenOff by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    FormSheet(
        title = entry.name,
        onDismiss = onDismiss,
        footer = { requestClose -> TextButton(onClick = requestClose) { Text("Close") } },
    ) {
        if (reminders.available) {
            Text("Reminder", style = MaterialTheme.typography.titleMedium)
            if (current != null) Text(ReminderRules.summary(current, timeZone), style = MaterialTheme.typography.bodyMedium)

            Text("Start", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reminders.presets().forEach { preset ->
                    FilterChip(
                        selected = start == preset.startAt,
                        enabled = preset.available,
                        onClick = { start = preset.startAt; message = null },
                        label = { Text(preset.label) },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { pickingTime = true },
                    label = { Text("Pick a time") },
                )
            }
            start?.let { Text("Starts at ${ReminderRules.clock(it, timeZone)}") }

            Text("Repeat", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = repeat == null, onClick = { repeat = null }, label = { Text("Off") })
                ReminderRules.RepeatChoices.forEach { minutes ->
                    FilterChip(
                        selected = repeat == minutes,
                        onClick = { repeat = minutes },
                        label = { Text("Every ${ReminderRules.repeatLabel(minutes)}") },
                    )
                }
            }

            if (reminders.supportsAlarms) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderKind.entries.forEach { option ->
                        FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(option.name) })
                    }
                }
                Text(
                    "An alarm rings where your device supports it; otherwise it is shown as a notification.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (notificationsOff) {
                Text(
                    "Notifications are turned off for OpenAdhd. Turn them on in your device's settings to get reminders.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (fullScreenOff) {
                Text(
                    "Full-screen alarms are turned off for OpenAdhd. The alarm will still ring, but it will not appear over the lock screen.",
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = reminders.openAlarmSettings) { Text("Open settings") }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val chosen = start
                        if (chosen == null) {
                            message = "Choose when the reminder starts"
                        } else {
                            reminders.save(entry.id, chosen, repeat, if (reminders.supportsAlarms) kind else ReminderKind.Notification) { result ->
                                when (result) {
                                    ReminderSaveResult.Saved -> onDismiss()
                                    ReminderSaveResult.SavedNotificationsOff -> { notificationsOff = true; fullScreenOff = false; message = null }
                                    ReminderSaveResult.SavedFullScreenOff -> { fullScreenOff = true; notificationsOff = false; message = null }
                                    is ReminderSaveResult.Refused -> message = result.message
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save reminder") }
                if (current != null) {
                    OutlinedButton(
                        onClick = { reminders.clear(entry.id); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Remove reminder") }
                }
            }
        }
        if (!entry.hasProgress) {
            OutlinedButton(
                onClick = { onDelete(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete task", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (pickingTime) {
        val initial = remember { reminders.currentTime() }
        val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Pick a time") },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    start = reminders.startAt(picker.hour, picker.minute)
                    message = null
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
}
