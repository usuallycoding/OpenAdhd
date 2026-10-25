# Spec Delta

## MODIFIED Requirements

### Requirement: The reminder is visible
An entry with a reminder SHALL show it on its row, as its kind, the start time and the repeat, and its sheet SHALL show the current reminder and the controls to change or remove it.

#### Scenario: On the row
- **WHEN** an entry has a notification reminder at 15:00 repeating every 30 minutes
- **THEN** its row shows a line such as "Reminder 15:00, every 30 min"

#### Scenario: An alarm on the row
- **WHEN** an entry has an alarm reminder at 15:00 repeating every 30 minutes
- **THEN** its row shows a line such as "Alarm 15:00, every 30 min"

#### Scenario: In the sheet
- **WHEN** the user opens the sheet of an entry with a reminder
- **THEN** the current start and repeat are selected and a Remove reminder button is shown

## ADDED Requirements

### Requirement: Reminder kind
A reminder SHALL be either a Notification or an Alarm, and a new reminder SHALL be a Notification. The kind belongs to the reminder, so removing the reminder removes it, and unchecking an entry brings back its reminder with the same kind.

#### Scenario: Default
- **WHEN** the user saves a reminder without changing the kind
- **THEN** the reminder is a Notification

#### Scenario: Alarm saved
- **WHEN** the user chooses Alarm, picks 16:15 and saves
- **THEN** the entry has an alarm reminder starting at 16:15

#### Scenario: Changing the kind
- **WHEN** an entry has a notification reminder and the user chooses Alarm and saves
- **THEN** the entry has an alarm reminder with the same start and repeat unless they were changed

#### Scenario: Unchecking
- **WHEN** an entry with an alarm reminder is completed and then unchecked
- **THEN** its reminder is an alarm again

### Requirement: Choosing the kind
On Android and iOS the entry sheet SHALL offer the choice Notification or Alarm with the other reminder controls, with the current kind selected, and a short line under it saying that an alarm rings where the device supports it and is shown as a notification otherwise. On the desktop app the choice MUST NOT be offered and every reminder is a Notification.

#### Scenario: On a phone
- **WHEN** the user opens the sheet of an entry on Android or iOS
- **THEN** the sheet offers Notification and Alarm

#### Scenario: Current kind
- **WHEN** the user opens the sheet of an entry that has an alarm reminder
- **THEN** Alarm is selected

#### Scenario: On the desktop
- **WHEN** the user opens the sheet of an entry on the desktop app
- **THEN** no kind choice is offered and the reminder is a Notification

### Requirement: The kind comes from the template
A task made from a template, by its schedule or with the + button, SHALL take the kind of the template's reminder, and the kind of the task's own reminder SHALL remain changeable in its sheet without changing the template.

#### Scenario: Scheduled alarm
- **WHEN** a template whose reminder is an alarm at 09:00 generates a task
- **THEN** the task has an alarm reminder at 09:00

#### Scenario: Added with the + button
- **WHEN** the user adds a template with an alarm reminder to Today with the + button
- **THEN** the entry has an alarm reminder

#### Scenario: Changed on the task
- **WHEN** the user changes the generated task's reminder to a Notification
- **THEN** only that task changes and the template keeps its alarm

### Requirement: Alarms are delivered as notifications until a platform rings them
Until a platform delivers an alarm by ringing, an alarm reminder SHALL fire at the same times as a notification and be shown as one, so choosing an alarm never loses a reminder.

#### Scenario: Alarm fires as a notification
- **WHEN** an alarm reminder is due on a platform that does not ring alarms yet
- **THEN** a notification "Still to do" with the task's name appears at that time
