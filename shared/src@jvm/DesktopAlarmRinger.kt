package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.AlarmPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The sound of a ring: [start] makes it repeat until [stop]. Neither may throw, and a sound that cannot play stays silent. */
interface AlarmSound {
    fun start()

    fun stop()
}

/** One ringing task on the alarm card: [dueAt] is when it was due if it rang late, and [day] the day it started ringing. */
data class AlarmRow(
    val entryId: String,
    val text: String,
    val startedAt: Instant,
    val dueAt: Instant?,
    val day: LocalDate,
)

/** How often the ringer looks at the wall clock to see whether a row has rung for too long. */
val DesktopRingCheck: Duration = 5.seconds

/**
 * What is ringing on the desktop: one [AlarmRow] for each task, and one [AlarmSound] for all of them, started with the
 * first row and stopped with the last. A task that rings again replaces its own row and starts its time again. A row
 * ends when it is dismissed, when its task is finished, or [limit] after it started, which is measured on the wall
 * clock every [check] so that time asleep counts. Everything is safe to call from any thread.
 */
class DesktopAlarmRinger(
    private val sound: AlarmSound,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
    private val check: Duration = DesktopRingCheck,
    private val limit: Duration = AlarmPlan.RingLimit,
) {
    private val lock = Any()
    private val _rows = MutableStateFlow<List<AlarmRow>>(emptyList())
    private var watcher: Job? = null

    /** The rows that are ringing, in the order they started. */
    val rows: StateFlow<List<AlarmRow>> = _rows

    fun ring(alarm: DueAlarm) = synchronized(lock) {
        val now = clock.now()
        val row = AlarmRow(alarm.entryId, alarm.text, now, alarm.dueAt, now.toLocalDateTime(zone).date)
        val before = _rows.value
        _rows.value = if (before.any { it.entryId == row.entryId }) {
            before.map { if (it.entryId == row.entryId) row else it }
        } else {
            before + row
        }
        if (before.isEmpty()) sound.start()
        if (watcher == null) watcher = scope.launch { watch() }
    }

    fun dismiss(entryId: String) = remove { it.entryId == entryId }

    fun dismissAll() = remove { true }

    /** Ends the rows of tasks that are done: those whose id is in [entryIds]. */
    fun finished(entryIds: Set<String>) = remove { it.entryId in entryIds }

    private fun remove(matches: (AlarmRow) -> Boolean) = synchronized(lock) {
        val before = _rows.value
        val after = before.filterNot(matches)
        if (after.size == before.size) return@synchronized
        _rows.value = after
        if (after.isEmpty()) sound.stop()
    }

    private suspend fun watch() {
        while (true) {
            delay(check)
            val finished = synchronized(lock) {
                val now = clock.now()
                remove { now - it.startedAt >= limit }
                if (_rows.value.isEmpty()) watcher = null
                _rows.value.isEmpty()
            }
            if (finished) return
        }
    }
}
