package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** How often the desktop scheduler looks at the wall clock while waiting for the next reminder. */
val DesktopReminderTick: Duration = 20.seconds

/** How late a reminder may be shown, which only happens when the computer slept through its time. */
val DesktopReminderGrace: Duration = 2.minutes

/**
 * An alarm that is due: [entryId] and [text] of its task, and [dueAt], the time it was due when it rang more than the
 * grace late (the computer slept through it), or null when it rang on time.
 */
data class DueAlarm(val entryId: String, val text: String, val dueAt: Instant? = null)

/**
 * Delivers reminders on the desktop while the app is running, with or without its window: a timer waits for each
 * time in turn. It looks at the wall clock every [tick] instead of waiting out the whole time in one delay, because a
 * long delay does not count the time the computer sleeps.
 *
 * A notification calls [notify] with the task's name, unless more than [grace] has passed since its time, which
 * only happens when the computer slept through it, and then it is dropped. An alarm calls [ring] once for each task
 * that has alarms due in a tick, for the latest of them, even when the computer slept through it, and also calls
 * [notify] so every ring leaves a notification. Times that had passed before a schedule was set, such as those missed
 * while the app was not running, are never delivered. [DesktopNotifier] shows the system notification.
 */
class DesktopReminderScheduler(
    private val notify: (text: String) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val clock: Clock = Clock.System,
    private val tick: Duration = DesktopReminderTick,
    private val grace: Duration = DesktopReminderGrace,
    private val ring: (DueAlarm) -> Unit = {},
) : ReminderScheduler {
    private var timer: Job? = null

    override val supportsAlarms: Boolean = true

    override fun hasPermission(): Boolean = true

    // macOS asks the user on its own the first time a notification is shown.
    override suspend fun ensurePermission(): Boolean = true

    private class Due(val time: Instant, val entryId: String, val text: String, val kind: ReminderKind)

    override fun replaceAll(upcoming: List<UpcomingReminder>) {
        timer?.cancel()
        val queue = upcoming
            .flatMap { reminder -> reminder.times.map { time -> Due(time, reminder.entryId, reminder.text, reminder.kind) } }
            .filter { it.time > clock.now() }
            .sortedBy { it.time }
        if (queue.isEmpty()) return
        timer = scope.launch {
            val waiting = ArrayDeque(queue)
            while (waiting.isNotEmpty()) {
                val now = clock.now()
                val alarms = LinkedHashMap<String, Due>()
                while (waiting.isNotEmpty() && waiting.first().time <= now) {
                    val due = waiting.removeFirst()
                    if (due.kind == ReminderKind.Alarm) alarms[due.entryId] = due
                    else if (now - due.time <= grace) notify(due.text)
                }
                alarms.values.forEach { due ->
                    notify(due.text)
                    ring(DueAlarm(due.entryId, due.text, if (now - due.time > grace) due.time else null))
                }
                if (waiting.isNotEmpty()) delay(minOf(tick, waiting.first().time - now))
            }
        }
    }
}

/**
 * Shows reminder notifications on the desktop: through [send] (the tray's notification, which on macOS appears in the
 * notification centre under the app's own name) when there is a system tray, and as an in-app message through
 * [fallback] where there is none.
 */
class DesktopNotifier(
    private val send: ((title: String, text: String) -> Unit)?,
    private val fallback: (String) -> Unit,
) {
    fun show(text: String) {
        if (send == null) fallback(text) else send("Still to do", text)
    }
}
