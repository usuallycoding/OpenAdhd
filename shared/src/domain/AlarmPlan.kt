package com.usuallycoding.openadhd.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** One time at which a reminder is armed on the device: whose it is, what to show, when, and how. */
data class ScheduledTime(
    val entryId: String,
    val text: String,
    val atMillis: Long,
    val kind: ReminderKind = ReminderKind.Notification,
)

/**
 * The rules a platform follows when it arms reminders and alarms: how many of each are armed, how long an alarm
 * may ring, and how the armed schedule is written down so it can be armed again after a restart.
 */
object AlarmPlan {
    /** Only this many alarm times are armed at once; alarms are listed by the system as the next alarm. */
    const val MaxArmedAlarms = 20

    /** Only this many notification times are armed at once. */
    const val MaxArmedNotifications = 100

    /** A ring stops by itself after this long. */
    val RingLimit: Duration = 10.minutes

    /** The soonest [MaxArmedAlarms] alarm times and the soonest [MaxArmedNotifications] notification times, soonest first. */
    fun arm(items: List<ScheduledTime>): List<ScheduledTime> {
        val soonest = items.sortedBy { it.atMillis }
        val alarms = soonest.filter { it.kind == ReminderKind.Alarm }.take(MaxArmedAlarms)
        val notifications = soonest.filter { it.kind != ReminderKind.Alarm }.take(MaxArmedNotifications)
        return (alarms + notifications).sortedBy { it.atMillis }
    }

    /** One line: `entryId|text|atMillis|kind`, with the separators and line breaks in the texts escaped. */
    fun encode(item: ScheduledTime): String = "${escape(item.entryId)}|${escape(item.text)}|${item.atMillis}|${item.kind.name}"

    /**
     * The item a line stands for, or null when it is not one. A line with three fields, written before kinds
     * existed, is a notification, and a kind that is not known reads as one too.
     */
    fun decode(line: String): ScheduledTime? {
        val parts = line.split('|')
        if (parts.size != 3 && parts.size != 4) return null
        val at = parts[2].toLongOrNull() ?: return null
        val kind = parts.getOrNull(3)?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } } ?: ReminderKind.Notification
        return ScheduledTime(unescape(parts[0]), unescape(parts[1]), at, kind)
    }

    // Percent-escapes the few characters that would break a line. Lines written by older versions used form
    // encoding (a plus for a space), so a plus is read as a space and a literal plus is always escaped.
    private fun escape(text: String): String = buildString {
        for (c in text) {
            when (c) {
                '%', '|', '+', '\n', '\r' -> append('%').append(c.code.toString(16).padStart(2, '0').uppercase())
                else -> append(c)
            }
        }
    }

    private fun unescape(text: String): String {
        val bytes = ArrayList<Byte>(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val hex = if (c == '%' && i + 2 < text.length) text.substring(i + 1, i + 3).toIntOrNull(16) else null
            when {
                hex != null -> { bytes += hex.toByte(); i += 3 }
                c == '+' -> { bytes += ' '.code.toByte(); i++ }
                else -> {
                    // A whole run at once, so a character made of two UTF-16 units is encoded as one.
                    var end = i
                    while (end < text.length && text[end] != '%' && text[end] != '+') end++
                    if (end == i) end = i + 1
                    bytes.addAll(text.substring(i, end).encodeToByteArray().toList())
                    i = end
                }
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
