# Spec Delta

## ADDED Requirements

### Requirement: Backup holds the reminder kind
An export SHALL include the kind of each task's reminder and of each template's reminder, and an import SHALL restore it. A backup made before reminder kinds existed MUST still import, with every reminder a Notification.

#### Scenario: Round trip
- **WHEN** the user exports a task with an alarm reminder and a template with an alarm reminder and imports the file
- **THEN** both reminders are alarms again

#### Scenario: Older file
- **WHEN** the user imports a backup whose reminders have no kind
- **THEN** the import succeeds and every reminder is a Notification

#### Scenario: Unknown kind
- **WHEN** the user imports a file whose reminder kind is neither Notification nor Alarm
- **THEN** the import is refused and nothing is replaced
