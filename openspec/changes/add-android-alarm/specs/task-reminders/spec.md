# Spec Delta

## MODIFIED Requirements

### Requirement: Alarms are delivered as notifications until a platform rings them
Until a platform delivers an alarm by ringing, an alarm reminder SHALL fire at the same times as a notification and be shown as one, so choosing an alarm never loses a reminder.

#### Scenario: Alarm fires as a notification
- **WHEN** an alarm reminder is due on a platform that does not ring alarms, such as the desktop app
- **THEN** a notification "Still to do" with the task's name appears at that time

## ADDED Requirements

### Requirement: An alarm rings on Android
On Android, at each time of an alarm reminder, the device SHALL ring with the default alarm ringtone on the alarm volume, repeating until it is stopped, and vibrate, and the screen SHALL turn on. It MUST ring with the app closed, with the device locked or on silent, and after the device restarts, and the next alarm time SHALL appear in the system's next-alarm display. A reminder of the Notification kind MUST be delivered as before.

#### Scenario: App closed
- **WHEN** an alarm reminder is due while the app is closed
- **THEN** the device rings and shows the alarm screen

#### Scenario: Locked and silent
- **WHEN** an alarm is due while the device is locked and its ringer is on silent
- **THEN** the alarm still rings at the alarm volume and the screen turns on

#### Scenario: After a restart
- **WHEN** the device is restarted with an alarm still to come
- **THEN** it still rings at its time

#### Scenario: Repeats ring again
- **WHEN** an alarm reminder repeats every 30 minutes and the first ring was dismissed
- **THEN** it rings again 30 minutes after its start, until the task is done or the stop time is reached

#### Scenario: Notification kind unchanged
- **WHEN** a notification reminder is due
- **THEN** it appears as a quiet notification and does not ring

#### Scenario: Completed task
- **WHEN** the task of an alarm reminder is completed before an alarm time
- **THEN** that alarm does not ring

### Requirement: The alarm screen
While an alarm rings, the app SHALL show an alarm screen above the lock screen with the task's name and two buttons, Open task and Dismiss. A notification with the same actions MUST be shown for as long as it rings. Tapping Open task, or the notification itself, SHALL stop the ring and open the app on Today with the task scrolled to and highlighted. Dismiss SHALL stop the ring and remove the notification, and MUST NOT cancel the alarm's later repeats.

#### Scenario: Open task
- **WHEN** the user taps Open task while an alarm rings
- **THEN** the ring stops and the app shows the task on Today, highlighted

#### Scenario: Tapping the notification
- **WHEN** the user taps the ringing notification
- **THEN** it behaves as Open task

#### Scenario: Dismiss
- **WHEN** the user taps Dismiss
- **THEN** the ring stops, the notification is removed and the alarm screen closes

#### Scenario: Dismiss keeps the repeats
- **WHEN** the user dismisses a ring of a reminder that repeats and the task is still pending
- **THEN** the next repeat still rings

#### Scenario: Not swipeable
- **WHEN** an alarm rings
- **THEN** its notification cannot be swiped away, only dismissed with its action

### Requirement: An alarm stops ringing by itself
A ring SHALL stop by itself after 10 minutes if the user has not acted, leaving a notification "Still to do" with the task's name in place of the ringing one. The next repeat, if any, SHALL ring again. An alarm that comes due while another rings MUST replace it, not ring over it.

#### Scenario: Ignored alarm
- **WHEN** an alarm rings for 10 minutes without being touched
- **THEN** the sound and vibration stop and a notification for the task remains

#### Scenario: Next repeat after a timeout
- **WHEN** an ignored alarm repeats every 30 minutes
- **THEN** it rings again 30 minutes after its start

#### Scenario: Overlap
- **WHEN** a second alarm comes due while the first is still ringing
- **THEN** only one ring is heard, for the second alarm's task

### Requirement: Alarms without the full-screen permission
When the device does not allow an app to show full-screen alerts (Android 14 and later, until the user allows it), an alarm SHALL still ring, with a heads-up notification in place of the alarm screen. Saving an alarm reminder for a task in that case SHALL still save it and tell the user that full-screen alarms are off, with a button that opens the setting. Saving a template whose reminder is an alarm SHALL show the same message after the form closes. Nothing is asked when the app starts.

#### Scenario: Ring without the permission
- **WHEN** an alarm comes due and full-screen alerts are not allowed
- **THEN** it rings and shows a notification with Open task and Dismiss, but no alarm screen over the lock screen

#### Scenario: Saving a task alarm
- **WHEN** the user saves an alarm for a task and full-screen alerts are not allowed
- **THEN** the alarm is saved and the sheet says full-screen alarms are off and offers to open the setting

#### Scenario: Opening the setting
- **WHEN** the user taps the button in that message
- **THEN** the system setting for full-screen alerts for OpenAdhd opens

#### Scenario: Saving a template alarm
- **WHEN** the user saves a template whose reminder is an alarm and full-screen alerts are not allowed
- **THEN** the template is saved and a message says full-screen alarms are off

#### Scenario: Allowed
- **WHEN** full-screen alerts are allowed or the device is older than Android 14
- **THEN** no message is shown on saving

### Requirement: How many alarms are armed
Only the soonest 20 alarm times SHALL be armed on the device at once; the rest are armed the next time the app works out its reminders. The limit MUST count alarm times only, never notification times.

#### Scenario: Many alarm times
- **WHEN** alarm reminders on several tasks add up to more than 20 future times
- **THEN** the soonest 20 are armed

#### Scenario: Notifications not counted
- **WHEN** 20 alarm times and 60 notification times are due later today
- **THEN** all 20 alarms and all 60 notifications are armed
