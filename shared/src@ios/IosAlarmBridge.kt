package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.EntryRequests

/** One time at which the system should ring an alarm for a task. */
data class IosAlarmItem(val entryId: String, val text: String, val atMillis: Long)

/**
 * What the Swift side offers Kotlin: system alarms (AlarmKit, iOS 26 and later). AlarmKit has no Objective-C
 * interface, so Kotlin cannot call it; Swift implements this and registers it in [IosBridge] before the first screen.
 */
interface IosAlarmBridge {
    /** True on iOS 26 and later, where AlarmKit exists. */
    val isAvailable: Boolean

    /** True when the user has allowed alarms for this app. */
    fun isAuthorized(): Boolean

    /** Asks the user to allow alarms if they have not decided yet, and reports whether alarms are now allowed. */
    fun requestAuthorization(onResult: (Boolean) -> Unit)

    /** Makes [items] the app's alarms: the ones not ringing now are cancelled, and each item is scheduled. */
    fun replaceAll(items: List<IosAlarmItem>)
}

/** What Swift and Kotlin share on iOS: the alarm bridge Swift registers, and the entry requests an App Intent can make. */
object IosBridge {
    /** Set by the Swift app at start-up; null until then and wherever AlarmKit is not used. */
    var alarms: IosAlarmBridge? = null

    /** Where a tapped notification or alarm button asks the app to show an entry. */
    val entryRequests: EntryRequests = EntryRequests()
}
