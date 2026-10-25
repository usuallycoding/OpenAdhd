package com.usuallycoding.openadhd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Shows a reminder's notification when its alarm fires. Tapping it opens the app on that entry's task. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getStringExtra(ReminderAlarms.ExtraEntryId) ?: return
        val text = intent.getStringExtra(ReminderAlarms.ExtraText) ?: return
        ReminderNotifications.show(context, entryId, text)
    }
}

/** Arms the stored reminders again after the device restarts, because alarms do not survive a restart. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ReminderAlarms.rearm(context)
    }
}
