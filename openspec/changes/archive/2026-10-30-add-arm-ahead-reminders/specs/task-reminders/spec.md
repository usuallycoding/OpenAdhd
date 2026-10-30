## ADDED Requirements

### Requirement: Scheduled template reminders are armed ahead
The app SHALL arm the reminders of scheduled templates for today and the following 6 days without waiting for their entries to be generated. For each of those days on which a template with a reminder is due and has not yet generated its entry, the reminder fires at the template's time of day, repeats as the template says, and is a notification or an alarm as the template says, with the template's name as its text. The stop time from Settings, the rule that times already past are never fired, and the limits on armed alarms and notifications apply as for any reminder, and when not everything fits, the soonest times are armed. Once the entry of a day has been generated, only that entry's own reminder applies for that day, and no time MUST fire twice.

#### Scenario: App not opened in the morning
- **WHEN** a daily template has an alarm at 07:00 and the app was last opened yesterday evening
- **THEN** the alarm rings at 07:00 today without the app having been opened

#### Scenario: Several days ahead
- **WHEN** a weekly template due on Monday and Thursday has a notification at 09:00 and the app was last opened on Sunday
- **THEN** the notifications for Monday and Thursday are armed, and none for the other days

#### Scenario: Entry already generated
- **WHEN** the entry of today's template has been generated and the user completes it before its reminder time
- **THEN** that reminder does not fire

#### Scenario: Entry deleted
- **WHEN** the user deletes the generated entry of today's template
- **THEN** the template's reminder does not fire again for today

#### Scenario: Not fired twice
- **WHEN** the app is opened at 08:00 and generates today's entry from a template with a reminder at 09:00
- **THEN** the reminder fires once at 09:00

#### Scenario: Stop time
- **WHEN** a template reminder starts at 09:00, repeats every 60 minutes and the stop time is 12:00
- **THEN** it is armed for 09:00, 10:00 and 11:00 on each planned day, and not for 12:00

#### Scenario: Not due
- **WHEN** a template has no schedule, or its schedule is not due on a day
- **THEN** nothing is armed for it that day

#### Scenario: Changed template
- **WHEN** the user changes a template's time or schedule
- **THEN** the planned reminders are worked out again from the new values

#### Scenario: Tapping a planned reminder
- **WHEN** the user taps a template's reminder before its entry exists
- **THEN** the app opens on Today, creates the day's entries as it does at start, and highlights nothing if the entry cannot be matched

## MODIFIED Requirements

### Requirement: Delivery on each platform
On Android and iOS reminders SHALL be delivered when the app is closed, and on Android also after the device restarts, including the reminders of scheduled templates whose entries have not been generated yet. On the desktop app reminders SHALL be delivered only while the app is running, as a system notification, or as a message inside the app when the system cannot show one. When the app is opened again it SHALL work out the next reminder from the current time and MUST NOT show the ones that were missed.

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
- **WHEN** a reminder is due while the desktop app is not running
- **THEN** nothing is shown, and when the app is opened the next time is used

#### Scenario: Desktop running
- **WHEN** a reminder is due while the desktop app is running
- **THEN** a notification or an in-app message shows the task's name
