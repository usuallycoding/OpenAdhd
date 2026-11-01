# Spec Delta

## MODIFIED Requirements

### Requirement: When a reminder fires
A reminder SHALL fire at its start time and then at every repeat interval after it, as long as the entry is not completed and the time is earlier than the stop time from Settings. It MUST NOT fire on a later day, and times already past are never fired late, except that on the desktop a reminder that came due while the computer was asleep is shown when it wakes if it is at most 2 minutes late.

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

### Requirement: Delivery on each platform
On Android and iOS reminders SHALL be delivered when the app is closed, and on Android also after the device restarts, including the reminders of scheduled templates whose entries have not been generated yet. On the desktop app reminders SHALL be delivered for as long as the app is running, including while its window is closed and it stays in the tray, as a system notification, or as a message inside the app when the system cannot show one. When the app is started again it SHALL work out the next reminder from the current time and MUST NOT show the ones that were missed.

#### Scenario: Phone with the app closed
- **WHEN** a reminder is due while the app is closed on Android or iOS
- **THEN** the notification appears

#### Scenario: Template reminder with the app closed
- **WHEN** a daily template's reminder is due and the app has not been opened that day on Android or iOS
- **THEN** the notification or alarm is delivered

#### Scenario: After a restart on Android
- **WHEN** the device is restarted with reminders still to come
- **THEN** they still fire at their times

#### Scenario: Desktop closed
- **WHEN** a reminder is due while the desktop app is not running because the user chose Quit or never started it
- **THEN** nothing is shown, and when the app is started the next time is used

#### Scenario: Desktop window closed
- **WHEN** a reminder is due while the desktop window is closed and the app is in the tray
- **THEN** a notification shows the task's name

#### Scenario: Desktop running
- **WHEN** a reminder is due while the desktop app is running
- **THEN** a notification or an in-app message shows the task's name
