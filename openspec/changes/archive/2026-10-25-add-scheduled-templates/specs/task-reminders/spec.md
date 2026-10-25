# Spec Delta

## ADDED Requirements

### Requirement: Generated entry starts with the template's reminder
An entry generated from a scheduled template whose reminder is set SHALL start with a reminder at the template's time of day, on the entry's date, with the template's repeat. Times that have already passed are never fired, so a repeating reminder resumes at its next time. The stop time from Settings applies as for any reminder.

#### Scenario: Time still ahead
- **WHEN** an entry is generated at 08:00 from a template with a reminder at 09:00 every 60 minutes
- **THEN** the entry has a reminder starting at 09:00 that repeats every 60 minutes

#### Scenario: Time already passed
- **WHEN** an entry is generated at 14:10 from a template with a reminder at 09:00 every 30 minutes
- **THEN** its next reminder fires at 14:30

#### Scenario: Passed without repeat
- **WHEN** an entry is generated at 14:10 from a template with a reminder at 09:00 and no repeat
- **THEN** no reminder fires for it

### Requirement: Generated entry's reminder is its own
The reminder of a generated entry SHALL be editable and removable in its "..." sheet like any other, and changing it MUST NOT change the template.

#### Scenario: Editing on Today
- **WHEN** the user changes the reminder of a generated entry to 16:00 every 15 minutes
- **THEN** only that entry changes, and the template keeps its time and repeat

#### Scenario: Added by hand
- **WHEN** the user adds a template with a reminder to Today by hand
- **THEN** the entry has no reminder
