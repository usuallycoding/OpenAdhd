# Spec Delta

## ADDED Requirements

### Requirement: Backup holds schedules and template reminders
An export SHALL include each template's schedule, reminder time and repeat, and the day it was last generated for, and an import SHALL restore them. A backup made before schedules existed MUST still import, with every template's schedule None and no reminder.

#### Scenario: Round trip
- **WHEN** the user exports a Weekly template with a reminder at 09:00 every 30 minutes and imports the file
- **THEN** the template has the same schedule, reminder and last-generated day

#### Scenario: Older file
- **WHEN** the user imports a backup that has no schedule fields
- **THEN** the import succeeds and every template has the schedule None and no reminder
