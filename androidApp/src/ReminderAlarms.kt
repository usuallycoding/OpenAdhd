package com.usuallycoding.openadhd

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.usuallycoding.openadhd.domain.AlarmPlan
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ScheduledTime

/**
 * Arms the system alarms that show reminders: quiet notifications with a normal alarm, and alarm-kind reminders
 * with an alarm-clock alarm that rings. The whole plan (the soonest [AlarmPlan.MaxKept] times) and the part of it that
 * is armed are kept in preferences, so the receivers can act, arm the next times as each one fires, and re-arm after a
 * restart without opening the database.
 */
internal object ReminderAlarms {
    const val ChannelId = "reminders"
    const val ExtraEntryId = "entryId"
    const val ExtraText = "text"

    /** Set on the intent that opens the app from a ringing alarm, so the app ends the ring. */
    const val ExtraStopAlarm = "stopAlarm"
    private const val Prefs = "openadhd_reminders"

    /** The plan. Before the plan was kept whole this held only what was armed, which reads as a plan all the same. */
    private const val Key = "schedule"
    private const val ArmedKey = "armed"

    /** Cancels everything armed before, keeps the soonest of [items] as the plan and arms what [AlarmPlan.armNext] picks from it. */
    fun replace(context: Context, items: List<ScheduledTime>) {
        loadArmed(context).forEach { cancel(context, it) }
        topUp(context, AlarmPlan.keep(items, System.currentTimeMillis()))
    }

    /**
     * Arms the next times of the kept plan, as after a reminder fired or a restart: times already past are dropped, and the soonest
     * within the limits are armed. Arming a time that is already armed only updates it.
     */
    fun topUp(context: Context) = topUp(context, load(context))

    /** [topUp] for the receivers: a failure here must never stop the reminder that is being shown. */
    fun topUpSafely(context: Context) {
        try {
            topUp(context)
        } catch (e: Exception) {
            Log.e("ReminderAlarms", "The next reminders could not be armed", e)
        }
    }

    private fun topUp(context: Context, plan: List<ScheduledTime>) {
        val now = System.currentTimeMillis()
        val kept = AlarmPlan.keep(plan, now)
        val armed = AlarmPlan.armNext(kept, now)
        save(context, Key, kept)
        save(context, ArmedKey, armed)
        arm(context, armed)
    }

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

    private fun save(context: Context, key: String, items: List<ScheduledTime>) {
        val lines = items.joinToString("\n") { AlarmPlan.encode(it) }
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString(key, lines).apply()
    }

    private fun load(context: Context, key: String = Key): List<ScheduledTime> =
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(key, "").orEmpty()
            .lines()
            .mapNotNull { AlarmPlan.decode(it) }

    /** What was armed. A version that kept only the armed times has no such list, and its plan is what was armed. */
    private fun loadArmed(context: Context): List<ScheduledTime> {
        val prefs = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        return load(context, if (prefs.contains(ArmedKey)) ArmedKey else Key)
    }
}
