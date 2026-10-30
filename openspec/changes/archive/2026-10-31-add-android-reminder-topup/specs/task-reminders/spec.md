## ADDED Requirements

### Requirement: Android arms the next reminders as they fire
On Android the app SHALL keep every planned reminder time, not only the ones that are armed, and whenever a reminder notification or an alarm fires, and after the device restarts, it SHALL arm the soonest times that are still to come within the limits on armed alarms and notifications, dropping the times already past. This MUST happen without the app being opened and without reading the database. At most 1000 times are kept, the soonest first.

#### Scenario: Armed times run out
- **WHEN** a repeating reminder has more future times than can be armed at once and the armed ones fire one after another with the app closed
- **THEN** each firing arms the next times, so the reminder keeps firing until the kept times are used up

#### Scenario: After a restart
- **WHEN** the device restarts with kept times that were not armed
- **THEN** the soonest of them are armed

#### Scenario: Past times
- **WHEN** the app is opened again after kept times have passed
- **THEN** it works out its reminders anew and the past times are not fired

#### Scenario: Limit on kept times
- **WHEN** the plan holds more than 1000 future times
- **THEN** the soonest 1000 are kept

## MODIFIED Requirements

### Requirement: Scheduled template reminders are armed ahead
The app SHALL arm the reminders of scheduled templates for today and the following 6 days on iOS and the desktop, and the following 29 days on Android, without waiting for their entries to be generated. For each of those days on which a template with a reminder is due and has not yet generated its entry, the reminder fires at the template's time of day, repeats as the template says, and is a notification or an alarm as the template says, with the template's name as its text. The stop time from Settings, the rule that times already past are never fired, and the limits on armed alarms and notifications apply as for any reminder, and when not everything fits, the soonest times are armed. Once the entry of a day has been generated, only that entry's own reminder applies for that day, and no time MUST fire twice.

#### Scenario: App not opened in the morning
- **WHEN** a daily template has an alarm at 07:00 and the app was last opened yesterday evening
- **THEN** the alarm rings at 07:00 today without the app having been opened

#### Scenario: Weeks ahead on Android
- **WHEN** a daily template has a notification at 09:00 and the Android app was last opened 10 days ago
- **THEN** the notification is still delivered on each of those days, up to 29 days after the app last worked out its reminders

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

### Requirement: How many alarms are armed
Only the soonest 20 alarm times SHALL be armed on the device at once; the rest are kept and armed as the armed ones fire, or the next time the app works out its reminders. The limit MUST count alarm times only, never notification times.

#### Scenario: Many alarm times
- **WHEN** alarm reminders on several tasks add up to more than 20 future times
- **THEN** the soonest 20 are armed

#### Scenario: Notifications not counted
- **WHEN** 20 alarm times and 60 notification times are due later today
- **THEN** all 20 alarms and all 60 notifications are armed
