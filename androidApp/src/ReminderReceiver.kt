package com.usuallycoding.openadhd

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.usuallycoding.openadhd.domain.NotificationTarget

/** Shows a reminder's notification when its alarm fires. Tapping it opens the app on that entry's task. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val entryId = intent.getStringExtra(ReminderAlarms.ExtraEntryId) ?: return
        val text = intent.getStringExtra(ReminderAlarms.ExtraText) ?: return
        ReminderAlarms.ensureChannel(context)

        // Opens the app on this entry's task. The data and request code are per entry: a pending intent is not told apart
        // by its extras, so without them every notification would open whichever entry was set last.
        val open = Intent(context, MainActivity::class.java)
            .setData(Uri.parse(NotificationTarget.uri(entryId)))
            .putExtra(ReminderAlarms.ExtraEntryId, entryId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(
            context, NotificationTarget.requestCode(entryId), open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, ReminderAlarms.ChannelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("Still to do")
            .setContentText(text)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        // One notification per entry: a later reminder for the same task replaces the earlier one.
        context.getSystemService(NotificationManager::class.java).notify(entryId.hashCode(), notification)
    }
}

/** Arms the stored reminders again after the device restarts, because alarms do not survive a restart. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ReminderAlarms.rearm(context)
    }
}
