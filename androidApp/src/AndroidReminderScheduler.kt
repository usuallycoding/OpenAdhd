package com.usuallycoding.openadhd

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Delivers reminders with system alarms and notifications; asks for the notification permission on Android 13 and up. */
class AndroidReminderScheduler(private val activity: ComponentActivity) : ReminderScheduler {
    private val context = activity.applicationContext
    private var requests = 0

    init {
        ReminderAlarms.ensureChannel(context)
    }

    override val supportsAlarms: Boolean = true

    override fun hasPermission(): Boolean {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return allowed && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    override suspend fun ensurePermission(): Boolean {
        if (hasPermission()) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val launcher = activity.activityResultRegistry.register(
                    "openadhd-notifications-${requests++}",
                    ActivityResultContracts.RequestPermission(),
                ) { if (continuation.isActive) continuation.resume(it) }
                continuation.invokeOnCancellation { launcher.unregister() }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        return hasPermission()
    }

    override fun replaceAll(upcoming: List<UpcomingReminder>) {
        val items = upcoming.flatMap { reminder ->
            reminder.times.map { ReminderAlarms.Item(reminder.entryId, reminder.text, it.toEpochMilliseconds()) }
        }
        ReminderAlarms.replace(context, items)
    }
}
