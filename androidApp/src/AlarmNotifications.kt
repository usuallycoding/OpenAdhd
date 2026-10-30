package com.usuallycoding.openadhd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.usuallycoding.openadhd.domain.NotificationTarget

/** The notifications of a ringing alarm and the channels they use. */
internal object AlarmNotifications {
    /** Silent: the ringing service plays the sound itself. */
    const val RingingChannel = "alarm_ringing"

    /** Plays the alarm sound by itself, for when the ringing service cannot be started. */
    const val FallbackChannel = "alarm_fallback"
    const val NotificationId = 4242

    fun alarmSound(context: Context): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(RingingChannel) == null) {
            manager.createNotificationChannel(
                NotificationChannel(RingingChannel, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Shown while an alarm for a task rings"
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
        if (manager.getNotificationChannel(FallbackChannel) == null) {
            manager.createNotificationChannel(
                NotificationChannel(FallbackChannel, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alarms for tasks, when they cannot ring in the usual way"
                    setSound(
                        alarmSound(context),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                    enableVibration(true)
                },
            )
        }
    }

    /**
     * The alarm's notification: it opens the task when tapped, offers Dismiss, and asks for the alarm screen over the
     * lock screen. [ringing] makes it ongoing, for the foreground service that is playing the sound, and swiping it away ends the ring.
     */
    fun build(context: Context, entryId: String, text: String, ringing: Boolean): Notification {
        val channel = if (ringing) RingingChannel else FallbackChannel
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channel)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_MAX)
        }
        val dismiss = PendingIntent.getService(
            context, NotificationTarget.requestCode(entryId),
            Intent(context, AlarmService::class.java).setAction(AlarmService.ActionDismiss),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val screen = PendingIntent.getActivity(
            context, NotificationTarget.requestCode(entryId),
            AlarmActivity.intent(context, entryId, text),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return builder
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("Still to do")
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(ringing)
            .setAutoCancel(!ringing)
            .setContentIntent(TaskIntents.openTask(context, entryId, stopsAlarm = ringing))
            // Swiping the ringing notification away (allowed from Android 14) must not leave the alarm ringing with nothing to show for it.
            .apply { if (ringing) setDeleteIntent(dismiss) }
            .setFullScreenIntent(screen, true)
            .addAction(Notification.Action.Builder(null, "Dismiss", dismiss).build())
            .build()
    }
}
