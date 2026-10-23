# app-theming Specification

## Purpose

Defines how the app decides whether to render in light or dark appearance, based on a user-selected theme mode that can follow the system setting.

## Requirements

### Requirement: Theme mode options
The app SHALL support three theme modes: System, Light and Dark. The default on every launch MUST be System.

#### Scenario: Fresh launch
- **WHEN** the app starts
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
On Android 12 or newer the app SHALL use the system's dynamic color scheme, in the light or dark variant resolved from the theme mode. On all other platforms and Android versions it MUST use the app's own color schemes.

#### Scenario: Android 12+
- **WHEN** the app runs on Android 12 or newer
- **THEN** colors come from the system dynamic scheme

#### Scenario: Other platforms
- **WHEN** the app runs on desktop, iOS, or Android older than 12
- **THEN** colors come from the app's light or dark scheme

### Requirement: Theme choice is not persisted
The selected theme mode SHALL be kept only for the running app session.

#### Scenario: Restart
- **WHEN** the user selects Dark and restarts the app
- **THEN** the mode is System again
