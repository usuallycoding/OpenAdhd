# Spec Delta

## MODIFIED Requirements

### Requirement: Alarms are delivered as notifications until a platform rings them
Where a platform cannot ring an alarm (the desktop app, an iPhone earlier than iOS 26, or a device where alarms are not allowed), an alarm reminder SHALL fire at the same times as a notification and be shown as one, so choosing an alarm never loses a reminder.

#### Scenario: Alarm fires as a notification
- **WHEN** an alarm reminder is due on a platform that does not ring alarms, such as the desktop app
- **THEN** a notification "Still to do" with the task's name appears at that time

#### Scenario: Older iPhone
- **WHEN** an alarm reminder is due on an iPhone earlier than iOS 26
- **THEN** a notification "Still to do" with the task's name appears at that time

#### Scenario: Alarms not allowed
- **WHEN** an alarm reminder is due on an iPhone where the user has not allowed alarms for OpenAdhd
- **THEN** a notification "Still to do" with the task's name appears at that time

## ADDED Requirements

### Requirement: An alarm rings on iOS
On an iPhone with iOS 26 or later where alarms are allowed, at each time of an alarm reminder the system SHALL ring an alarm that sounds through Silent mode and Focus and appears on the lock screen, with the app closed, in the background or in front. A reminder of the Notification kind MUST be delivered as before.

#### Scenario: App closed and locked
- **WHEN** an alarm reminder is due while the app is closed and the phone is locked
- **THEN** the phone rings and shows the alarm on the lock screen

#### Scenario: Silent mode
- **WHEN** an alarm is due while the phone is on silent
- **THEN** the alarm still rings

#### Scenario: Repeats ring again
- **WHEN** an alarm reminder repeats every 30 minutes and the first ring was stopped
- **THEN** it rings again 30 minutes after its start, until the task is done or the stop time is reached

#### Scenario: Notification kind unchanged
- **WHEN** a notification reminder is due on the same phone
- **THEN** it appears as a notification and does not ring

#### Scenario: Completed task
- **WHEN** the task of an alarm reminder is completed before an alarm time
- **THEN** that alarm does not ring

### Requirement: The iOS alarm buttons
A ringing alarm on iOS SHALL show the task's name with the system's Stop button and a button Open task. Open task SHALL open the app on Today with the task scrolled to and highlighted. Stop SHALL end this ring and MUST NOT cancel the alarm's later repeats.

#### Scenario: Open task
- **WHEN** the user taps Open task on a ringing alarm
- **THEN** the app opens on Today with the task scrolled to and highlighted

#### Scenario: Open task from the lock screen
- **WHEN** the phone is locked and the user taps Open task
- **THEN** the phone asks to be unlocked and then opens the task

#### Scenario: Stop
- **WHEN** the user stops a ring of a reminder that repeats and the task is still pending
- **THEN** the ring ends and the next repeat still rings

### Requirement: iOS alarm permission
The app SHALL ask the system for permission to ring alarms the first time the user saves an alarm reminder for a task or a template, never when the app starts and never for a notification reminder. A refusal MUST NOT discard the reminder, and the user SHALL be told that alarms are turned off for OpenAdhd and where to turn them on; the reminder is then delivered as a notification.

#### Scenario: First alarm
- **WHEN** the user saves an alarm for the first time on an iPhone with iOS 26 or later
- **THEN** the system asks for permission to ring alarms

#### Scenario: Refused for a task
- **WHEN** the user refuses and the alarm was saved for a task
- **THEN** the alarm is saved and the sheet says alarms are turned off for OpenAdhd and where to turn them on

#### Scenario: Refused for a template
- **WHEN** the user refuses and the alarm was saved for a template
- **THEN** the template is saved and a message says alarms are turned off for OpenAdhd

#### Scenario: Notification reminders
- **WHEN** the user saves a notification reminder
- **THEN** no alarm permission is asked

#### Scenario: App start
- **WHEN** the app starts
- **THEN** no alarm permission is asked

### Requirement: How many alarms are scheduled on iOS
On iOS only the soonest 20 alarm times SHALL be scheduled at once, counted apart from notification times, and the rest are scheduled the next time the app works out its reminders.

#### Scenario: Many alarm times
- **WHEN** alarm reminders add up to more than 20 future times
- **THEN** the soonest 20 are scheduled as alarms

#### Scenario: Notifications not counted
- **WHEN** 20 alarm times and 60 notification times are due later today
- **THEN** all 20 alarms and all 60 notifications are scheduled
