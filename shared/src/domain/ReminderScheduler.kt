package com.usuallycoding.openadhd.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** What still has to be shown for one entry: its [text] at each of [times], all in the future when it was worked out. */
data class UpcomingReminder(
    val entryId: String,
    val text: String,
    val times: List<Instant>,
    val kind: ReminderKind = ReminderKind.Notification,
)

/**
 * The only platform-specific part of reminders: asking for permission and arming the system to show
 * notifications. The shared code works out what should fire, and the platform delivers it.
 */
interface ReminderScheduler {
    /**
     * Whether this platform offers alarms, so the app shows the Notification / Alarm choice. An alarm is delivered as
     * a notification wherever it cannot ring yet. False by default, as on the desktop.
     */
    val supportsAlarms: Boolean get() = false

    /**
     * Whether the device lets the app show full-screen alerts, which an alarm needs to appear over the lock screen.
     * Without it an alarm still rings. True by default and wherever the question does not arise.
     */
    fun fullScreenAlarmsAllowed(): Boolean = true

    /**
     * Asks the user to allow alarms if the platform needs that (iOS, once per install), and reports whether an alarm
     * can ring. True where nothing is asked, including where alarms are not available and fall back to notifications.
     */
    suspend fun ensureAlarmPermission(): Boolean = true

    /**
     * Opens the system page where the user can allow what alarms need: the full-screen alerts setting on Android, the app's
     * settings page on iOS, where alarms are allowed; nothing where there is none.
     */
    fun openAlarmSettings() {}

    /** Opens the system page where the user can allow notifications for the app; nothing where there is none, as on the desktop. */
    fun openNotificationSettings() {}

    /**
     * Called each time the app returns to the front, for example from the system settings after the user turned notifications on.
     * A platform that cannot keep what it armed while a permission was off arms it again here; the same arming as at start, so it is
     * safe to repeat. Nothing by default, as on the desktop.
     */
    fun onAppResumed() {}

    /**
     * How many days from today the reminders of scheduled templates are planned for ([TemplateReminders]). A platform that can keep
     * and arm times as they come up, as Android does, asks for more than one that can only hold what it is given.
     */
    val planDays: Int get() = TemplateReminders.Days

    /** Whether notifications can be shown right now. */
    fun hasPermission(): Boolean

    /** Asks the user for permission to show notifications if needed; returns whether it is now granted. */
    suspend fun ensurePermission(): Boolean

    /**
     * Makes [upcoming] the whole schedule: everything armed before is cancelled, and each time that is still in
     * the future is armed. Called again with the full list whenever anything changes.
     */
    fun replaceAll(upcoming: List<UpcomingReminder>)
}

/**
 * The last plan a platform scheduler was handed, kept so it can be handed over again when the app returns to the front. There is
 * none until the first one: an empty list is a plan (nothing is due), but "no plan yet" is not, and handing over an empty list
 * then would remove what is already pending.
 */
class RememberedPlan {
    /** The last plan, or null before the first. */
    var last: List<UpcomingReminder>? = null
        private set

    fun remember(plan: List<UpcomingReminder>) {
        last = plan
    }
}

/**
 * Keeps a [ReminderScheduler] equal to the store: whenever today, today's entries, the templates or the stop hour
 * change, the full list of reminders still to fire is worked out again and handed over. It holds the reminders of
 * today's entries and, for the days ahead, those of scheduled templates whose entries do not exist yet
 * ([TemplateReminders]). Replacing the whole schedule makes setting, completing, unchecking, removing and importing
 * all go through one path.
 */
class ReminderCoordinator(
    private val store: OpenAdhdStore,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
    private val today: Flow<LocalDate>,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    /** Runs until cancelled. */
    suspend fun run() {
        today
            .flatMapLatest { date ->
                combine(
                    store.entriesFor(date),
                    store.templates,
                    store.settings.map { it.reminderStopMinute }.distinctUntilChanged(),
                ) { entries, templates, stopMinute ->
                    upcoming(date, entries, stopMinute) + TemplateReminders.plan(templates, date, stopMinute, clock.now(), zone, scheduler.planDays)
                }
            }
            .distinctUntilChanged()
            .collect { scheduler.replaceAll(it) }
    }

    /** The reminders of the pending entries of [date] that still have a time to fire, measured from now. */
    fun upcoming(date: LocalDate, entries: List<PlannedTask>, stopMinute: Int): List<UpcomingReminder> {
        val now = clock.now()
        return entries
            .filter { !it.isCompleted }
            .mapNotNull { entry ->
                val reminder = entry.reminder ?: return@mapNotNull null
                val times = ReminderRules.upcoming(reminder, date, stopMinute, now, zone)
                if (times.isEmpty()) null else UpcomingReminder(entry.id, entry.name, times, reminder.kind)
            }
    }
}
