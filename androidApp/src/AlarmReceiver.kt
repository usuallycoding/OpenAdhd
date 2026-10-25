package com.usuallycoding.openadhd

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/** Starts the ringing when an alarm-kind reminder comes due. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getStringExtra(ReminderAlarms.ExtraEntryId) ?: return
        val text = intent.getStringExtra(ReminderAlarms.ExtraText) ?: return
        val ring = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ActionRing)
            .putExtra(ReminderAlarms.ExtraEntryId, entryId)
            .putExtra(ReminderAlarms.ExtraText, text)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(ring) else context.startService(ring)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "The alarm service could not be started", e)
            // The system refused to start the service from the background: an alarm notification still makes a sound.
            AlarmNotifications.ensureChannels(context)
            context.getSystemService(NotificationManager::class.java)
                .notify(AlarmNotifications.NotificationId, AlarmNotifications.build(context, entryId, text, ringing = false))
        }
    }
}
