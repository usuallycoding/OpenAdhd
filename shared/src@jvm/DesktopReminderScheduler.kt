package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.AWTException
import java.awt.Color
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import kotlin.time.Clock

/**
 * Delivers reminders on the desktop while the app is running: a timer waits for each time in turn and calls
 * [notify] with the task's name. Nothing is shown while the app is closed, and times that were missed are not
 * replayed. [DesktopNotifier] shows the system notification.
 */
class DesktopReminderScheduler(
    private val notify: (text: String) -> Unit,
    /** Called with true while reminders are waiting and false once none are, so a tray icon can come and go. */
    private val onPending: (Boolean) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val clock: Clock = Clock.System,
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
        onPending(queue.isNotEmpty())
        if (queue.isEmpty()) return
        timer = scope.launch {
            for ((time, text) in queue) {
                val wait = time - clock.now()
                if (wait.isPositive()) delay(wait)
                notify(text)
            }
            onPending(false)
        }
    }
}

/**
 * Shows desktop notifications through the system tray, which on macOS appear in the notification centre under the
 * app's own name. The tray icon exists only while reminders are pending. Falls back to [fallback] (an in-app
 * message) where there is no system tray.
 */
class DesktopNotifier(private val fallback: (String) -> Unit) {
    private var icon: TrayIcon? = null

    @Synchronized
    fun setPending(pending: Boolean) {
        if (!SystemTray.isSupported()) return
        if (pending && icon == null) {
            try {
                val created = TrayIcon(image(), "OpenAdhd")
                SystemTray.getSystemTray().add(created)
                icon = created
            } catch (_: AWTException) {
                icon = null
            }
        } else if (!pending) {
            icon?.let { SystemTray.getSystemTray().remove(it) }
            icon = null
        }
    }

    @Synchronized
    fun show(text: String) {
        val tray = icon
        if (tray == null) fallback(text) else tray.displayMessage("Still to do", text, TrayIcon.MessageType.INFO)
    }

    private fun image(): BufferedImage {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color(0xE8, 0x7A, 0x1E)
        g.fillOval(3, 3, 26, 26)
        g.dispose()
        return image
    }
}
