# Spec Delta

## MODIFIED Requirements

### Requirement: Notification permission
The app SHALL ask for permission to show notifications the first time the user saves a reminder, not when the app starts. A refused permission MUST NOT discard the reminder, and the sheet SHALL say that notifications are turned off for OpenAdhd and where to turn them on, with an Open settings button that opens the app's notification settings.

#### Scenario: First reminder
- **WHEN** the user saves a reminder for the first time on a device that needs permission
- **THEN** the system asks for permission to show notifications

#### Scenario: Refused
- **WHEN** the user refuses
- **THEN** the reminder is still saved and the sheet says notifications are off and how to turn them on, with an Open settings button

#### Scenario: Opening the notification settings
- **WHEN** the user presses Open settings under that message
- **THEN** the system settings page for notifications of OpenAdhd opens

#### Scenario: App start
- **WHEN** the app starts
- **THEN** no notification permission is asked

### Requirement: Permission when saving a template reminder
Saving a template that has a reminder time SHALL ask for permission to show notifications when the platform needs it. A refusal MUST NOT discard the template or its reminder, and the user SHALL be told after the form closes, in a message with an Open settings action, that notifications are turned off for OpenAdhd. Saving a template without a reminder time, and starting the app, MUST NOT ask.

#### Scenario: First template reminder
- **WHEN** the user saves a template with a reminder time on a device that needs permission and has not granted it
- **THEN** the system asks for permission to show notifications

#### Scenario: Refused
- **WHEN** the user refuses
- **THEN** the template is saved with its reminder and a message says notifications are off for OpenAdhd and where to turn them on, with an Open settings action

#### Scenario: Opening the notification settings from the message
- **WHEN** the user presses Open settings on that message
- **THEN** the system settings page for notifications of OpenAdhd opens

#### Scenario: No reminder time
- **WHEN** the user saves a template without a reminder time
- **THEN** no permission is asked

#### Scenario: Already granted
- **WHEN** notification permission is already granted and the user saves a template with a reminder time
- **THEN** nothing is asked and no message is shown

#### Scenario: Desktop
- **WHEN** the user saves a template with a reminder time on the desktop app
- **THEN** no permission is asked

### Requirement: Alarms without the full-screen permission
When the device does not allow an app to show full-screen alerts (Android 14 and later, until the user allows it), an alarm SHALL still ring, with a heads-up notification in place of the alarm screen. Saving an alarm reminder for a task in that case SHALL still save it and tell the user that full-screen alarms are off, with a button that opens the setting. Saving a template whose reminder is an alarm SHALL show the same message after the form closes, with the same action. Nothing is asked when the app starts.

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
- **THEN** the template is saved and a message says full-screen alarms are off, with an Open settings action

#### Scenario: Opening the setting from the template message
- **WHEN** the user presses Open settings on that message
- **THEN** the system setting for full-screen alerts for OpenAdhd opens

#### Scenario: Allowed
- **WHEN** full-screen alerts are allowed or the device is older than Android 14
- **THEN** no message is shown on saving

### Requirement: iOS alarm permission
The app SHALL ask the system for permission to ring alarms the first time the user saves an alarm reminder for a task or a template, never when the app starts and never for a notification reminder. A refusal MUST NOT discard the reminder, and the user SHALL be told that alarms are turned off for OpenAdhd and where to turn them on, with an Open settings button or action that opens the app's settings; the reminder is then delivered as a notification.

#### Scenario: First alarm
- **WHEN** the user saves an alarm for the first time on an iPhone with iOS 26 or later
- **THEN** the system asks for permission to ring alarms

#### Scenario: Refused for a task
- **WHEN** the user refuses and the alarm was saved for a task
- **THEN** the alarm is saved and the sheet says alarms are turned off for OpenAdhd and where to turn them on, with an Open settings button

#### Scenario: Refused for a template
- **WHEN** the user refuses and the alarm was saved for a template
- **THEN** the template is saved and a message says alarms are turned off for OpenAdhd, with an Open settings action

#### Scenario: Opening the settings
- **WHEN** the user presses Open settings on either message
- **THEN** the settings page of OpenAdhd opens, where alarms can be allowed

#### Scenario: Notification reminders
- **WHEN** the user saves a notification reminder
- **THEN** no alarm permission is asked

#### Scenario: App start
- **WHEN** the app starts
- **THEN** no alarm permission is asked

## ADDED Requirements

### Requirement: Reminders are armed again when the app returns to the front
When the app returns to the front, for example from the system settings, it SHALL arm its reminders again from the current schedule and permissions, without restarting. A reminder saved while notifications or alarms were off is therefore delivered at its time once they are on. The desktop is unchanged.

#### Scenario: Notification reminder on Android
- **WHEN** the user saves a notification reminder with notifications off for OpenAdhd, turns notifications on in the system settings and returns to the app without restarting it
- **THEN** the notification is shown at the reminder's time

#### Scenario: Notification reminder on iOS
- **WHEN** the user saves a notification reminder on an iPhone with notifications off for OpenAdhd, turns them on in Settings and returns to the app without restarting it
- **THEN** the notification is shown at the reminder's time

#### Scenario: Alarm allowed afterwards
- **WHEN** an alarm was saved on an iPhone with alarms off, so it was delivered as a notification, and the user allows alarms in Settings and returns to the app
- **THEN** the alarm rings at its time

#### Scenario: Returning with nothing changed
- **WHEN** the user leaves the app and comes back with no setting changed
- **THEN** each reminder time is still delivered once

#### Scenario: Before the schedule is known
- **WHEN** the app starts and comes to the front before it has worked out its reminders
- **THEN** no reminder that was already armed is removed

#### Scenario: Desktop
- **WHEN** the desktop app comes to the front
- **THEN** its reminders behave as before
