package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.AlarmPlan
import com.usuallycoding.openadhd.domain.EntryRequests
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.domain.RememberedPlan
import com.usuallycoding.openadhd.domain.ScheduledTime
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UIKit.UIApplicationOpenSettingsURLString
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
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
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
    private val plan = RememberedPlan()

    /** UIKit keeps its delegate weakly, so it is held here. It makes a notification show as a banner while the app is open. */
    private val delegate = ForegroundDelegate(requests)

    init {
        center.delegate = delegate
        refreshAuthorization {}
    }

    override val supportsAlarms: Boolean = true

    /** The app's Notifications page in Settings (iOS 15.4 and later, else the app's settings page, which lists them too). */
    override fun openNotificationSettings() {
        open(UIApplicationOpenNotificationSettingsURLString)
    }

    /** The app's settings page, where the Alarms permission is. */
    override fun openAlarmSettings() {
        open(UIApplicationOpenSettingsURLString)
    }

    /** Opens [address] in Settings; an address that cannot be opened is ignored. */
    private fun open(address: String?) {
        val url = (address ?: UIApplicationOpenSettingsURLString).let { NSURL.URLWithString(it) } ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }

    override fun hasPermission(): Boolean = authorized

    /** Reads whether notifications are allowed now, which the user may have changed in Settings, and then calls [onDone]. */
    private fun refreshAuthorization(onDone: () -> Unit) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            authorized = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
            onDone()
        }
    }

    /**
     * iOS does not keep a notification that was scheduled while notifications were off, and AlarmKit may have been allowed since, so on
     * return the permission is read again and the last plan is handed over again. Before the first plan there is nothing to hand over,
     * and an empty list would remove what is pending ([RememberedPlan]).
     */
    override fun onAppResumed() {
        refreshAuthorization {
            dispatch_async(dispatch_get_main_queue()) {
                plan.last?.let { replaceAll(it) }
            }
        }
    }

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
        if (allowed) plan.last?.let { replaceAll(it) }
        return allowed
    }

    override fun replaceAll(upcoming: List<UpcomingReminder>) {
        plan.remember(upcoming)
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
