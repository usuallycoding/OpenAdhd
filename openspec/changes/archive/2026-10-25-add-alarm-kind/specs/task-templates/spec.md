# Spec Delta

## ADDED Requirements

### Requirement: Template reminder kind
A template's reminder SHALL have a kind, Notification or Alarm, chosen in the template form on Android and iOS together with the time and repeat, and it SHALL be a Notification by default. The choice is offered only while a reminder time is set, and is not offered on the desktop app.

#### Scenario: Choose an alarm
- **WHEN** the user sets a reminder time 09:00, chooses Alarm and saves the template
- **THEN** the template keeps an alarm reminder at 09:00

#### Scenario: Default kind
- **WHEN** the user sets a reminder time and saves without choosing a kind
- **THEN** the template's reminder is a Notification

#### Scenario: Needs a time
- **WHEN** a template has no reminder time
- **THEN** the form offers no kind choice

#### Scenario: Editing
- **WHEN** the user edits a template whose reminder is an alarm
- **THEN** Alarm is selected in the form

#### Scenario: On the desktop
- **WHEN** the user opens the template form on the desktop app
- **THEN** no kind choice is offered
