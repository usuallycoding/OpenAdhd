# Spec Delta

## RENAMED Requirements
- FROM: `### Requirement: Theme choice is not persisted`
- TO: `### Requirement: Theme choice is kept`

## MODIFIED Requirements

### Requirement: Theme mode options
The app SHALL support three theme modes: System, Light and Dark. The default on a fresh install, and after the theme is restored or all data is deleted, MUST be System.

#### Scenario: Fresh launch
- **WHEN** the app starts for the first time
- **THEN** the theme mode is System

#### Scenario: After deleting all data
- **WHEN** the user has chosen Dark and then deletes all data
- **THEN** the theme mode is System

### Requirement: Theme choice is kept
The selected theme mode SHALL be kept between launches, as the Theme setting in Settings, and SHALL be part of a backup like every other setting.

#### Scenario: Restart
- **WHEN** the user selects Dark and restarts the app
- **THEN** the mode is Dark again

#### Scenario: Restored
- **WHEN** the user restores the Theme setting and restarts the app
- **THEN** the mode is System

#### Scenario: In a backup
- **WHEN** the theme is Light, the user exports a backup, deletes all data and imports the backup
- **THEN** the mode is Light again
