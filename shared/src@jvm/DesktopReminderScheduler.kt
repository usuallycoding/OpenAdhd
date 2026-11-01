package com.usuallycoding.openadhd.data

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

/** How often the desktop scheduler looks at the wall clock while waiting for the next reminder. */
val DesktopReminderTick: Duration = 20.seconds

/** How late a reminder may be shown, which only happens when the computer slept through its time. */
val DesktopReminderGrace: Duration = 2.minutes

/**
 * Delivers reminders on the desktop while the app is running, with or without its window: a timer waits for each
 * time in turn and calls [notify] with the task's name. It looks at the wall clock every [tick] instead of waiting
 * out the whole time in one delay, because a long delay does not count the time the computer sleeps. A time that has
 * passed by more than [grace] when it is noticed (the computer slept through it) is dropped, and times missed while
 * the app was not running are not replayed. [DesktopNotifier] shows the system notification.
 */
class DesktopReminderScheduler(
    private val notify: (text: String) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val clock: Clock = Clock.System,
    private val tick: Duration = DesktopReminderTick,
    private val grace: Duration = DesktopReminderGrace,
) : ReminderScheduler {
    private var timer: Job? = null

    override fun hasPermission(): Boolean = true

    // macOS asks the user on its own the first time a notification is shown.
    override suspend fun ensurePermission(): Boolean = true

    override fun replaceAll(upcoming: List<UpcomingReminder>) {
        timer?.cancel()
        val queue = upcoming
            .flatMap { reminder -> reminder.times.map { time -> time to reminder.text } }
            .filter { (time, _) -> time > clock.now() }
            .sortedBy { it.first }
        if (queue.isEmpty()) return
        timer = scope.launch {
            val waiting = ArrayDeque(queue)
            while (waiting.isNotEmpty()) {
                val now = clock.now()
                while (waiting.isNotEmpty() && waiting.first().first <= now) {
                    val (time, text) = waiting.removeFirst()
                    if (now - time <= grace) notify(text)
                }
                if (waiting.isNotEmpty()) delay(minOf(tick, waiting.first().first - now))
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
