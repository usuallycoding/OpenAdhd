package com.usuallycoding.openadhd

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.ScheduledTime
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

    /** From Android 14 an app may need the user's permission to show an alarm over the lock screen. */
    override fun fullScreenAlarmsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    override fun openAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val settings = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activity.startActivity(settings)
        } catch (_: ActivityNotFoundException) {
        }
    }

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
            reminder.times.map { ScheduledTime(reminder.entryId, reminder.text, it.toEpochMilliseconds(), reminder.kind) }
        }
        ReminderAlarms.replace(context, items)
    }
}
