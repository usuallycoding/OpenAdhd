package com.usuallycoding.openadhd.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.ReminderRules
import kotlinx.datetime.TimeZone

/**
 * The card shown while alarms ring: one row for each ringing task with its name, "Due 07:30" when it rang late, and
 * the buttons Open task and Dismiss, each acting on its own row. The card scrolls when many tasks ring at once.
 */
@Composable
fun AlarmCard(
    rows: List<AlarmRow>,
    onOpenTask: (entryId: String) -> Unit,
    onDismiss: (entryId: String) -> Unit,
    modifier: Modifier = Modifier,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Still to do", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            rows.forEach { row ->
                Card(modifier = Modifier.fillMaxWidth().testTag("alarm-row-${row.entryId}")) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(row.text, style = MaterialTheme.typography.titleLarge)
                        row.dueAt?.let { Text("Due ${ReminderRules.clock(it, timeZone)}", style = MaterialTheme.typography.bodyMedium) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { onDismiss(row.entryId) }, modifier = Modifier.testTag("alarm-dismiss-${row.entryId}")) {
                                Text("Dismiss")
                            }
                            Button(onClick = { onOpenTask(row.entryId) }, modifier = Modifier.testTag("alarm-open-${row.entryId}")) {
                                Text("Open task")
                            }
                        }
                    }
                }
            }
        }
    }
}
