package com.usuallycoding.openadhd

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Arms the system alarms that show reminders. The whole schedule is also kept in preferences, so the receivers
 * can show a notification and re-arm after a restart without opening the database.
 */
internal object ReminderAlarms {
    const val ChannelId = "reminders"
    const val ExtraEntryId = "entryId"
    const val ExtraText = "text"
    private const val Prefs = "openadhd_reminders"
    private const val Key = "schedule"
    private const val MaxArmed = 100

    data class Item(val entryId: String, val text: String, val atMillis: Long)

    /** Cancels everything armed before and arms [items], soonest first and at most [MaxArmed] of them. */
    fun replace(context: Context, items: List<Item>) {
        val before = load(context)
        before.forEach { cancel(context, it) }
        val chosen = items.sortedBy { it.atMillis }.take(MaxArmed)
        save(context, chosen)
        arm(context, chosen)
    }

    /** Arms the stored schedule again, as after a restart; times already past are skipped. */
    fun rearm(context: Context) = arm(context, load(context))

    private fun arm(context: Context, items: List<Item>) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        items.filter { it.atMillis > now }.forEach { item ->
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.atMillis, pendingIntent(context, item))
        }
    }

    private fun cancel(context: Context, item: Item) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, item))
    }

    /** One distinct PendingIntent per entry and time, told apart by the data Uri. */
    private fun pendingIntent(context: Context, item: Item): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setData(Uri.parse("openadhd://reminder/${Uri.encode(item.entryId)}/${item.atMillis}"))
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

    private fun save(context: Context, items: List<Item>) {
        val lines = items.joinToString("\n") { "${enc(it.entryId)}|${enc(it.text)}|${it.atMillis}" }
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString(Key, lines).apply()
    }

    private fun load(context: Context): List<Item> =
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, "").orEmpty()
            .lines()
            .mapNotNull { line ->
                val parts = line.split('|')
                val at = parts.getOrNull(2)?.toLongOrNull()
                if (parts.size == 3 && at != null) Item(dec(parts[0]), dec(parts[1]), at) else null
            }

    private fun enc(text: String) = URLEncoder.encode(text, "UTF-8")

    private fun dec(text: String) = URLDecoder.decode(text, "UTF-8")
}
