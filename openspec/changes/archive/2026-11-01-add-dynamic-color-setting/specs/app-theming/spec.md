# Spec Delta

## MODIFIED Requirements

### Requirement: Android dynamic color
On Android 12 or newer, when the Dynamic color setting is on, the app SHALL use the system's dynamic color scheme, in the light or dark variant resolved from the theme mode. When the setting is off, and on all other platforms and Android versions, it MUST use the app's own color schemes.

#### Scenario: Android 12+
- **WHEN** the app runs on Android 12 or newer and Dynamic color is on
- **THEN** colors come from the system dynamic scheme

#### Scenario: Dynamic color turned off
- **WHEN** the app runs on Android 12 or newer and the user turns Dynamic color off
- **THEN** colors come from the app's light or dark scheme at once, without restarting

#### Scenario: Other platforms
- **WHEN** the app runs on desktop, iOS, or Android older than 12
- **THEN** colors come from the app's light or dark scheme

### Requirement: Theme choice is kept
The selected theme mode and the Dynamic color choice SHALL be kept between launches, as the Theme and Dynamic color settings in Settings, and SHALL be part of a backup like every other setting.

#### Scenario: Restart
- **WHEN** the user selects Dark and restarts the app
- **THEN** the mode is Dark again

#### Scenario: Dynamic color kept
- **WHEN** the user turns Dynamic color off and restarts the app
- **THEN** the app still uses its own colors

#### Scenario: Restored
- **WHEN** the user restores the Theme setting and restarts the app
- **THEN** the mode is System

#### Scenario: In a backup
- **WHEN** the theme is Light, the user exports a backup, deletes all data and imports the backup
- **THEN** the mode is Light again

#### Scenario: Dynamic color in a backup
- **WHEN** Dynamic color is off, the user exports a backup, deletes all data and imports the backup
- **THEN** Dynamic color is off again
