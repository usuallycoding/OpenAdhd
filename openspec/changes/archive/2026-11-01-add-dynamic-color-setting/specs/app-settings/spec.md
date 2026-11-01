# Spec Delta

## ADDED Requirements

### Requirement: Dynamic color setting
Settings SHALL offer a Dynamic color setting in the Appearance group, after Theme, with the choices "Dynamic (wallpaper colors)" and "App colors". The default MUST be Dynamic. It SHALL be listed only on platforms that offer dynamic color, which is Android 12 or newer, and MUST NOT be listed anywhere else. Tapping it SHALL open a bottom sheet with the two choices, the current one highlighted, and choosing one applies it and closes the sheet.

#### Scenario: Android 12 or newer
- **WHEN** the user opens Settings on Android 12 or newer
- **THEN** Appearance lists Theme and then Dynamic color, showing "Dynamic (wallpaper colors)" with that as its default

#### Scenario: Not supported
- **WHEN** the user opens Settings on desktop, iOS, or Android older than 12
- **THEN** Appearance lists Theme only

#### Scenario: Choose App colors
- **WHEN** the user opens Dynamic color and chooses "App colors"
- **THEN** the sheet closes, the setting shows "App colors", and the app switches to its own colors

#### Scenario: Restore
- **WHEN** Dynamic color is "App colors" and the user taps Restore on it
- **THEN** it shows "Dynamic (wallpaper colors)" and its Restore control is disabled

## MODIFIED Requirements

### Requirement: Settings and their defaults
The settings and their defaults SHALL be: theme, follow system theme; dynamic color, dynamic; name, empty; debt limit, 500 coins; subtask ceiling, 10; the coins paid by task tiers 1 to 4, 5, 10, 20 and 40; the Shop price of reward tiers 1 to 4, 20, 50, 120 and 300; critical hit chance, 1 in 8; critical hit multiplier, 2; roll upgrade chance, 1 in 6; the hour reminders stop at, 22.

#### Scenario: Defaults match the original rules
- **WHEN** no setting has been changed
- **THEN** the debt limit, payouts, prices, subtask limit and chances behave as described in the other specs

#### Scenario: Stop time default
- **WHEN** no setting has been changed
- **THEN** "Stop reminders at" shows 22

#### Scenario: Theme default
- **WHEN** no setting has been changed
- **THEN** Theme shows "Follow system theme"

#### Scenario: Dynamic color default
- **WHEN** no setting has been changed
- **THEN** Dynamic color is on, where the platform offers it
