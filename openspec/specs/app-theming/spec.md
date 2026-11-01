# app-theming Specification

## Purpose

Defines how the app decides whether to render in light or dark appearance, based on a user-selected theme mode that can follow the system setting.

## Requirements

### Requirement: Theme mode options
The app SHALL support three theme modes: System, Light and Dark. The default on a fresh install, and after the theme is restored or all data is deleted, MUST be System.

#### Scenario: Fresh launch
- **WHEN** the app starts for the first time
- **THEN** the theme mode is System

#### Scenario: After deleting all data
- **WHEN** the user has chosen Dark and then deletes all data
- **THEN** the theme mode is System

### Requirement: Appearance follows the theme mode
The app SHALL render with the dark color scheme when the mode is Dark, the light color scheme when the mode is Light, and the scheme matching the device's current dark/light setting when the mode is System.

#### Scenario: Explicit dark
- **WHEN** the mode is Dark and the device is set to light
- **THEN** the app renders with the dark color scheme

#### Scenario: Explicit light
- **WHEN** the mode is Light and the device is set to dark
- **THEN** the app renders with the light color scheme

#### Scenario: Following the system
- **WHEN** the mode is System and the device switches from light to dark
- **THEN** the app switches to the dark color scheme without restarting

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
