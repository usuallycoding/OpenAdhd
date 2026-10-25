# Spec Delta

## MODIFIED Requirements

### Requirement: Generated entry starts with the template's reminder
An entry generated from a scheduled template, or added to Today by hand, whose template has a reminder SHALL start with a reminder at the template's time of day, on the entry's date, with the template's repeat. Times that have already passed are never fired, so a repeating reminder resumes at its next time. The stop time from Settings applies as for any reminder.

#### Scenario: Time still ahead
- **WHEN** an entry is generated at 08:00 from a template with a reminder at 09:00 every 60 minutes
- **THEN** the entry has a reminder starting at 09:00 that repeats every 60 minutes

#### Scenario: Time already passed
- **WHEN** an entry is generated at 14:10 from a template with a reminder at 09:00 every 30 minutes
- **THEN** its next reminder fires at 14:30

#### Scenario: Passed without repeat
- **WHEN** an entry is generated at 14:10 from a template with a reminder at 09:00 and no repeat
- **THEN** no reminder fires for it

#### Scenario: Added with the + button
- **WHEN** the user adds a template with a reminder at 09:00 every 30 minutes to Today with the + button
- **THEN** the entry has a reminder starting at 09:00 that repeats every 30 minutes

#### Scenario: Added without a template reminder
- **WHEN** the user adds a template that has no reminder to Today with the + button
- **THEN** the entry has no reminder

#### Scenario: Added twice
- **WHEN** the user adds the same template with a reminder to Today twice
- **THEN** each entry has its own copy of the reminder

### Requirement: Generated entry's reminder is its own
The reminder of a generated or hand-added entry SHALL be editable and removable in its "..." sheet like any other, and changing it MUST NOT change the template.

#### Scenario: Editing on Today
- **WHEN** the user changes the reminder of a generated entry to 16:00 every 15 minutes
- **THEN** only that entry changes, and the template keeps its time and repeat

#### Scenario: Added by hand
- **WHEN** the user adds a template with a reminder to Today by hand and removes the entry's reminder in its "..." sheet
- **THEN** only that entry loses its reminder, and the template keeps its time and repeat

## ADDED Requirements

### Requirement: Permission when saving a template reminder
Saving a template that has a reminder time SHALL ask for permission to show notifications when the platform needs it. A refusal MUST NOT discard the template or its reminder, and the user SHALL be told after the form closes that notifications are turned off for OpenAdhd and where to turn them on. Saving a template without a reminder time, and starting the app, MUST NOT ask.

#### Scenario: First template reminder
- **WHEN** the user saves a template with a reminder time on a device that needs permission and has not granted it
- **THEN** the system asks for permission to show notifications

#### Scenario: Refused
- **WHEN** the user refuses
- **THEN** the template is saved with its reminder and a message says notifications are off for OpenAdhd and where to turn them on

#### Scenario: No reminder time
- **WHEN** the user saves a template without a reminder time
- **THEN** no permission is asked

#### Scenario: Already granted
- **WHEN** notification permission is already granted and the user saves a template with a reminder time
- **THEN** nothing is asked and no message is shown

#### Scenario: Desktop
- **WHEN** the user saves a template with a reminder time on the desktop app
- **THEN** no permission is asked
