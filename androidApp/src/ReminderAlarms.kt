package com.usuallycoding.openadhd

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.usuallycoding.openadhd.domain.AlarmPlan
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ScheduledTime

/**
 * Arms the system alarms that show reminders: quiet notifications with a normal alarm, and alarm-kind reminders
 * with an alarm-clock alarm that rings. The whole schedule is also kept in preferences, so the receivers can act and
 * re-arm after a restart without opening the database.
 */
internal object ReminderAlarms {
    const val ChannelId = "reminders"
    const val ExtraEntryId = "entryId"
    const val ExtraText = "text"

    /** Set on the intent that opens the app from a ringing alarm, so the app ends the ring. */
    const val ExtraStopAlarm = "stopAlarm"
    private const val Prefs = "openadhd_reminders"
    private const val Key = "schedule"

    /** Cancels everything armed before and arms what [AlarmPlan.arm] picks from [items]. */
    fun replace(context: Context, items: List<ScheduledTime>) {
        val before = load(context)
        before.forEach { cancel(context, it) }
        val chosen = AlarmPlan.arm(items)
        save(context, chosen)
        arm(context, chosen)
    }

    /** Arms the stored schedule again, as after a restart; times already past are skipped. */
    fun rearm(context: Context) = arm(context, load(context))

    private fun arm(context: Context, items: List<ScheduledTime>) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        items.filter { it.atMillis > now }.forEach { item ->
            if (item.kind == ReminderKind.Alarm) {
                // An alarm clock alarm is exempt from Doze, and the system lists it as the next alarm. It is an exact alarm, so it
                // needs the exact-alarm permission; if that is ever missing the alarm is still armed, only without exact timing.
                try {
                    alarms.setAlarmClock(AlarmManager.AlarmClockInfo(item.atMillis, TaskIntents.openApp(context)), pendingIntent(context, item))
                } catch (_: SecurityException) {
                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.atMillis, pendingIntent(context, item))
                }
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.atMillis, pendingIntent(context, item))
            }
        }
    }

    private fun cancel(context: Context, item: ScheduledTime) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, item))
    }

    /** One distinct PendingIntent per entry, time and kind, told apart by the data Uri. */
    private fun pendingIntent(context: Context, item: ScheduledTime): PendingIntent {
        val alarm = item.kind == ReminderKind.Alarm
        val intent = Intent(context, if (alarm) AlarmReceiver::class.java else ReminderReceiver::class.java)
            .setData(Uri.parse("openadhd://${if (alarm) "alarm" else "reminder"}/${Uri.encode(item.entryId)}/${item.atMillis}"))
            .putExtra(ExtraEntryId, item.entryId)
            .putExtra(ExtraText, item.text)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(ChannelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(ChannelId, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminders for tasks on Today that are not done yet"
                },
            )
        }
    }

    private fun save(context: Context, items: List<ScheduledTime>) {
        val lines = items.joinToString("\n") { AlarmPlan.encode(it) }
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString(Key, lines).apply()
    }

    private fun load(context: Context): List<ScheduledTime> =
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, "").orEmpty()
            .lines()
            .mapNotNull { AlarmPlan.decode(it) }
}
