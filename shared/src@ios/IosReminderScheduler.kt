package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.AlarmPlan
import com.usuallycoding.openadhd.domain.EntryRequests
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.ScheduledTime
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Clock

/**
 * Delivers reminders as local notifications. The system keeps at most 64 pending requests, so only the soonest
 * [MaxPending] times are scheduled; the shared coordinator hands over the whole list again whenever the app
 * starts or the data changes, which tops it up.
 */
class IosReminderScheduler(
    private val requests: EntryRequests = EntryRequests(),
    /** The alarm bridge Swift registered, read each time because it is set after this is made. */
    private val alarmBridge: () -> IosAlarmBridge? = { IosBridge.alarms },
) : ReminderScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private var authorized = false
    private var lastUpcoming: List<UpcomingReminder> = emptyList()

    /** UIKit keeps its delegate weakly, so it is held here. It makes a notification show as a banner while the app is open. */
    private val delegate = ForegroundDelegate(requests)

    init {
        center.delegate = delegate
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            authorized = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
        }
    }

    override val supportsAlarms: Boolean = true

    override fun hasPermission(): Boolean = authorized

    override suspend fun ensurePermission(): Boolean = suspendCancellableCoroutine { continuation ->
        // Answers at once without a prompt when the user has already decided.
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            authorized = granted
            if (continuation.isActive) continuation.resume(granted)
        }
    }

    /** Asks AlarmKit's permission the first time; older iOS has no alarms, so nothing is asked and reminders stay notifications. */
    override suspend fun ensureAlarmPermission(): Boolean {
        val bridge = alarmBridge()?.takeIf { it.isAvailable } ?: return true
        if (bridge.isAuthorized()) return true
        val allowed = suspendCancellableCoroutine { continuation ->
            bridge.requestAuthorization { granted -> if (continuation.isActive) continuation.resume(granted) }
        }
        // The reminder was saved before the answer, so it was handed over as a notification: route it again.
        if (allowed) replaceAll(lastUpcoming)
        return allowed
    }

    override fun replaceAll(upcoming: List<UpcomingReminder>) {
        lastUpcoming = upcoming
        val bridge = alarmBridge()
        val items = upcoming.flatMap { reminder ->
            reminder.times.map { ScheduledTime(reminder.entryId, reminder.text, it.toEpochMilliseconds(), reminder.kind) }
        }
        // Alarms ring only where AlarmKit exists and the user allowed them; otherwise they are notifications.
        val routed = AlarmPlan.route(items, alarmsUsable = bridge != null && bridge.isAvailable && bridge.isAuthorized())
        bridge?.takeIf { it.isAvailable }?.replaceAll(routed.alarms.map { IosAlarmItem(it.entryId, it.text, it.atMillis) })

        center.removeAllPendingNotificationRequests()
        val now = Clock.System.now().toEpochMilliseconds()
        routed.notifications
            .take(MaxPending)
            .forEach { item ->
                val seconds = (item.atMillis - now) / 1000.0
                if (seconds < 1.0) return@forEach
                val content = UNMutableNotificationContent().apply {
                    setTitle("Still to do")
                    setBody(item.text)
                    // Read back when the notification is tapped, to show the task it is about.
                    setUserInfo(mapOf<Any?, Any?>(EntryIdKey to item.entryId))
                    setSound(UNNotificationSound.defaultSound())
                }
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
                val request = UNNotificationRequest.requestWithIdentifier("${item.entryId}-${item.atMillis}", content, trigger)
                center.addNotificationRequest(request, withCompletionHandler = null)
            }
    }

    private class ForegroundDelegate(private val requests: EntryRequests) : NSObject(), UNUserNotificationCenterDelegateProtocol {
        /** A tap on a notification, with the app closed, in the background or open: ask the app to show its task. */
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit,
        ) {
            val entryId = didReceiveNotificationResponse.notification.request.content.userInfo[EntryIdKey] as? String
            if (entryId != null) requests.open(entryId)
            withCompletionHandler()
        }

        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
        ) {
            withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound)
        }
    }

    private companion object {
        const val MaxPending = 60
        const val EntryIdKey = "entryId"
    }
}
