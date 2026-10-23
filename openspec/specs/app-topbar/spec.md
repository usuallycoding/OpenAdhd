# app-topbar Specification

## Purpose

Defines the app's top bar and the control it provides for choosing the theme mode.

## Requirements

### Requirement: Top bar
The app SHALL display a top app bar above its content showing the app name centered.

#### Scenario: Top bar visible
- **WHEN** the app is shown on any supported platform
- **THEN** a top bar with the title "OpenAdhd" centered is displayed above the content

### Requirement: Theme chooser
The top bar SHALL contain an icon button that opens a dropdown of three options, each with an icon and label: follow system theme, light, and dark. The button MUST show the icon of the current mode, and the current option MUST be highlighted in the dropdown.

#### Scenario: Initial selection
- **WHEN** the app starts
- **THEN** the button shows the "follow system theme" icon and that option is highlighted when the dropdown is opened

#### Scenario: Selecting an option
- **WHEN** the user opens the dropdown and taps the "dark" option
- **THEN** the dropdown closes, the app switches to dark appearance, and the button shows the "dark" icon

### Requirement: Chooser accessibility
The chooser button SHALL have a content description, and each option has a visible text label naming its mode.

#### Scenario: Screen reader label
- **WHEN** a screen reader focuses the chooser button
- **THEN** it announces a label identifying it as the theme chooser
