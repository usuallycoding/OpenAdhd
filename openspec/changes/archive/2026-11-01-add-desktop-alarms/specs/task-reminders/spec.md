# Spec Delta

## MODIFIED Requirements

### Requirement: When a reminder fires
A reminder SHALL fire at its start time and then at every repeat interval after it, as long as the entry is not completed and the time is earlier than the stop time from Settings. It MUST NOT fire on a later day, and times already past are never fired late, except on the desktop: a notification that came due while the computer was asleep is shown when it wakes if it is at most 2 minutes late, and an alarm that came due while it was asleep rings once when it wakes, as the desktop alarm rules say.

#### Scenario: Repeating until the stop time
- **WHEN** a reminder starts at 15:00, repeats every 30 minutes and the stop time is 22:00
- **THEN** it fires at 15:00, 15:30 and so on up to 21:30, and not at 22:00

#### Scenario: Once
- **WHEN** a reminder starts at 15:00 with no repeat
- **THEN** it fires at 15:00 only

#### Scenario: Changed stop time
- **WHEN** the stop time in Settings is changed to 18:00
- **THEN** reminders that would fire at 18:00 or later no longer fire

#### Scenario: Desktop wakes just after the time
- **WHEN** a desktop reminder is due at 15:00, the computer sleeps from 14:55 and wakes at 15:01
- **THEN** the notification is shown on waking

#### Scenario: Desktop wakes long after the time
- **WHEN** a desktop reminder is due at 15:00 and a repeat at 15:30, and the computer wakes at 15:20
- **THEN** the 15:00 reminder is not shown and the 15:30 one fires on time

#### Scenario: Desktop alarm wakes long after the time
- **WHEN** a desktop alarm is due at 15:00 and the computer wakes at 15:20
- **THEN** the alarm rings on waking, unlike a notification, which would not be shown

### Requirement: Choosing the kind
On Android, iOS and the desktop app the entry sheet SHALL offer the choice Notification or Alarm with the other reminder controls, with the current kind selected, and a short line under it saying that an alarm rings where the device supports it and is shown as a notification otherwise.

#### Scenario: On a phone
- **WHEN** the user opens the sheet of an entry on Android or iOS
- **THEN** the sheet offers Notification and Alarm

#### Scenario: Current kind
- **WHEN** the user opens the sheet of an entry that has an alarm reminder
- **THEN** Alarm is selected

#### Scenario: On the desktop
- **WHEN** the user opens the sheet of an entry on the desktop app
- **THEN** the sheet offers Notification and Alarm

### Requirement: Alarms are delivered as notifications until a platform rings them
Where a platform cannot ring an alarm (an iPhone earlier than iOS 26, or a device where alarms are not allowed), an alarm reminder SHALL fire at the same times as a notification and be shown as one, so choosing an alarm never loses a reminder.

#### Scenario: Alarm fires as a notification
- **WHEN** an alarm reminder is due on a platform that does not ring alarms, such as an iPhone earlier than iOS 26
- **THEN** a notification "Still to do" with the task's name appears at that time

#### Scenario: Older iPhone
- **WHEN** an alarm reminder is due on an iPhone earlier than iOS 26
- **THEN** a notification "Still to do" with the task's name appears at that time

#### Scenario: Alarms not allowed
- **WHEN** an alarm reminder is due on an iPhone where the user has not allowed alarms for OpenAdhd
- **THEN** a notification "Still to do" with the task's name appears at that time
