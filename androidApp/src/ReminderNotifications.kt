package com.usuallycoding.openadhd

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** The quiet "Still to do" notification of a reminder; also what an ignored alarm leaves behind. */
internal object ReminderNotifications {
    fun show(context: Context, entryId: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ReminderAlarms.ensureChannel(context)
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
            .setContentIntent(TaskIntents.openTask(context, entryId))
            .setAutoCancel(true)
            .build()
        // One notification per entry: a later reminder for the same task replaces the earlier one.
        context.getSystemService(NotificationManager::class.java).notify(entryId.hashCode(), notification)
    }
}
