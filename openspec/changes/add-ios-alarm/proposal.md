# Proposal

## Why

Alarms ring on Android, but on an iPhone a reminder marked as an alarm is still a quiet notification. iOS 26 added AlarmKit, which lets an app schedule real system alarms that sound through Silent mode and Focus and show on the lock screen. Using it makes the Alarm kind mean the same thing on both phones.

## What Changes

- **Alarms ring on iOS 26 and later:** at each time of an alarm reminder the system rings, even with the app closed, the phone locked or on silent, and shows an alarm on the lock screen with a button that opens the task and the system's Stop button.
- **Open task and Stop:** Open task opens the app on Today with the task scrolled to and highlighted, through the path the notification tap already uses. Stop ends this ring; later repeats still ring.
- **Permission:** the first time an alarm is saved the system asks to allow alarms. A refusal still saves the alarm, tells the user alarms are off for OpenAdhd, and the reminder is delivered as a notification.
- **Fallback:** on an iPhone earlier than iOS 26, or when alarms are not allowed, an alarm is delivered as a notification, as it is today.
- **Limits:** at most the soonest 20 alarm times are scheduled at once, counted apart from notification times; completing a task cancels its alarms.
- **A spike comes first:** the first tasks build the smallest working alarm and have it tried on a phone, to settle things that cannot be known from the documentation (notably whether an alert-only alarm needs a Widget Extension, which this Xcode project may not be able to add). If it does not work as hoped, the design is revisited before the rest is built.
- **Not in this change:** the desktop app, a Time Sensitive notification fallback (it needs a capability a free Apple account may not have), custom sounds, countdown or snooze, and any change to Notification-kind reminders.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `task-reminders`: alarms ring on iOS, their two buttons, the iOS alarm permission and its message, the limit, and the fallback wording for iPhones that cannot ring.

## Impact

- `shared`: an interface through which Swift offers alarms to Kotlin and a holder that carries it and the entry requests; a routing rule that decides which items go to alarms and which stay notifications; two additions to `ReminderScheduler` (asking for alarm permission, and reporting that alarms are not allowed) with a save result and messages in the entry sheet and the template notice.
- `shared/src@ios`: `IosReminderScheduler` sends alarm-kind times to the bridge and keeps everything else as notifications.
- `iosApp`: Swift code for AlarmKit scheduling and an App Intent that opens the task, the `NSAlarmKitUsageDescription` entry in `Info.plist`, and registering the bridge at start-up.
- Tests: the routing rule and the permission flow in `shared`; the AlarmKit behaviour is checked by hand on a phone.
- No new libraries, and no storage or backup change.
