package com.usuallycoding.openadhd

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.usuallycoding.openadhd.domain.NotificationTarget

/** The intents that open the app, shared by reminder notifications and alarms. */
internal object TaskIntents {
    /** Opens the app on the task of [entryId]. A per-entry address and request code keep each target distinct. */
    fun openTaskIntent(context: Context, entryId: String, stopsAlarm: Boolean = false): Intent =
        Intent(context, MainActivity::class.java)
            .setData(Uri.parse(if (stopsAlarm) NotificationTarget.alarmUri(entryId) else NotificationTarget.uri(entryId)))
            .putExtra(ReminderAlarms.ExtraEntryId, entryId)
            .putExtra(ReminderAlarms.ExtraStopAlarm, stopsAlarm)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    /** The tap target of a reminder notification, or of a ringing alarm's, which also ends the ring when it opens the app. */
    fun openTask(context: Context, entryId: String, stopsAlarm: Boolean = false): PendingIntent = PendingIntent.getActivity(
        context, NotificationTarget.requestCode(entryId), openTaskIntent(context, entryId, stopsAlarm),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Opens the app as it was, for the system's next-alarm display. */
    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
