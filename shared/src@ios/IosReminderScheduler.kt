package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.ReminderScheduler
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
class IosReminderScheduler : ReminderScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private var authorized = false

    /** UIKit keeps its delegate weakly, so it is held here. It makes a notification show as a banner while the app is open. */
    private val delegate = ForegroundDelegate()

    init {
        center.delegate = delegate
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            authorized = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
        }
    }

    override fun hasPermission(): Boolean = authorized

    override suspend fun ensurePermission(): Boolean = suspendCancellableCoroutine { continuation ->
        // Answers at once without a prompt when the user has already decided.
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            authorized = granted
            if (continuation.isActive) continuation.resume(granted)
        }
    }

    override fun replaceAll(upcoming: List<UpcomingReminder>) {
        center.removeAllPendingNotificationRequests()
        val now = Clock.System.now()
        upcoming
            .flatMap { reminder -> reminder.times.map { time -> reminder to time } }
            .sortedBy { it.second }
            .take(MaxPending)
            .forEach { (reminder, time) ->
                val seconds = (time - now).inWholeMilliseconds / 1000.0
                if (seconds < 1.0) return@forEach
                val content = UNMutableNotificationContent().apply {
                    setTitle("Still to do")
                    setBody(reminder.text)
                    setSound(UNNotificationSound.defaultSound())
                }
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
                val request = UNNotificationRequest.requestWithIdentifier(
                    "${reminder.entryId}-${time.toEpochMilliseconds()}", content, trigger,
                )
                center.addNotificationRequest(request, withCompletionHandler = null)
            }
    }

    private class ForegroundDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
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
    }
}
