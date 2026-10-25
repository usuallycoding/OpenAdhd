package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.UpcomingReminder

/** A [ReminderScheduler] for tests: remembers every schedule it was given and answers permission from [granted]. */
class FakeReminderScheduler(
    var granted: Boolean = true,
    var grantsWhenAsked: Boolean = true,
    override val supportsAlarms: Boolean = false,
    var fullScreenAllowed: Boolean = true,
) : ReminderScheduler {
    var settingsOpened = 0
        private set

    val schedules = mutableListOf<List<UpcomingReminder>>()
    var permissionRequests = 0
        private set

    /** The schedule most recently handed over. */
    val current: List<UpcomingReminder> get() = schedules.last()

    override fun fullScreenAlarmsAllowed(): Boolean = fullScreenAllowed

    override fun openAlarmSettings() {
        settingsOpened++
    }

    override fun hasPermission(): Boolean = granted

    override suspend fun ensurePermission(): Boolean {
        if (granted) return true
        permissionRequests++
        granted = grantsWhenAsked
        return granted
    }

    override fun replaceAll(upcoming: List<UpcomingReminder>) {
        schedules.add(upcoming)
    }
}
