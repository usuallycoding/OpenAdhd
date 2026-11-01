package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.TemplateReminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Ends the ring of a task once it is finished. The scheduler only knows the times still to come, so it cannot tell a
 * task with no repeats left from one that was completed or deleted; this looks at the entries of the days that have
 * rows, and while there are none it watches nothing. A row of a scheduled template whose entry has not been generated
 * yet has no entry to look for, so it is left alone.
 */
class FinishedAlarmWatcher(
    private val store: OpenAdhdStore,
    private val ringer: DesktopAlarmRinger,
    private val scope: CoroutineScope,
) {
    fun start(): Job = scope.launch {
        ringer.rows
            .map { rows -> rows.filterNot { it.isPlanned() }.map { it.day }.toSet() }
            .distinctUntilChanged()
            .flatMapLatest { days ->
                if (days.isEmpty()) {
                    emptyFlow()
                } else {
                    combine(
                        combine(days.map { day -> store.entriesFor(day) }) { lists -> lists.toList().flatten() },
                        ringer.rows,
                    ) { entries, rows ->
                        val pending = entries.filterNot { it.isCompleted }.map { it.id }.toSet()
                        rows.filterNot { it.isPlanned() }.map { it.entryId }.filter { it !in pending }.toSet()
                    }
                }
            }
            .collect { finished -> if (finished.isNotEmpty()) ringer.finished(finished) }
    }

    private fun AlarmRow.isPlanned() = entryId.startsWith(TemplateReminders.IdPrefix)
}
