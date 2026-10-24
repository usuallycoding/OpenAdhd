# Spec Delta

## MODIFIED Requirements

### Requirement: Settings tab
The app SHALL have a Settings destination in the bottom bar, after Shop, that lists every setting grouped as Appearance, Profile, Coins, Subtasks, Shop, Luck and Reminders, followed by a Data section. Each setting SHALL show its name, its current value and its default.

#### Scenario: Opening Settings
- **WHEN** the user taps Settings in the bottom bar
- **THEN** the screen lists the settings with their current values and defaults

#### Scenario: Nothing changed yet
- **WHEN** the user opens Settings on a fresh install
- **THEN** every setting shows its default value

#### Scenario: No reorder control
- **WHEN** the Settings destination is shown
- **THEN** the top bar offers no Reorder control

#### Scenario: Data section last
- **WHEN** the user scrolls to the end of the Settings list
- **THEN** the Data section is shown after the Reminders group

#### Scenario: Appearance first
- **WHEN** the user opens Settings
- **THEN** the Appearance group is the first one listed

### Requirement: Settings and their defaults
The settings and their defaults SHALL be: theme, follow system theme; name, empty; debt limit, 500 coins; subtask ceiling, 10; the coins paid by task tiers 1 to 4, 5, 10, 20 and 40; the Shop price of reward tiers 1 to 4, 20, 50, 120 and 300; critical hit chance, 1 in 8; critical hit multiplier, 2; roll upgrade chance, 1 in 6; the hour reminders stop at, 22.

#### Scenario: Defaults match the original rules
- **WHEN** no setting has been changed
- **THEN** the debt limit, payouts, prices, subtask limit and chances behave as described in the other specs

#### Scenario: Stop time default
- **WHEN** no setting has been changed
- **THEN** "Stop reminders at" shows 22

#### Scenario: Theme default
- **WHEN** no setting has been changed
- **THEN** Theme shows "Follow system theme"

## ADDED Requirements

### Requirement: Theme setting
The Appearance group SHALL have a Theme setting with three options, each with an icon and a text label: Follow system theme, Light and Dark. Tapping it SHALL open a bottom sheet listing the options with the current one highlighted. Choosing an option SHALL apply it to the whole app at once, close the sheet and keep the choice, and the top bar MUST NOT have a theme control.

#### Scenario: Choosing dark
- **WHEN** the user taps Theme, chooses Dark in the sheet
- **THEN** the sheet closes, the app switches to the dark appearance, and the Theme row shows Dark

#### Scenario: Current option highlighted
- **WHEN** the theme is Light and the user opens the sheet
- **THEN** Light is highlighted and the other two are not

#### Scenario: Restore
- **WHEN** the theme is Dark and the user taps Restore on the Theme row
- **THEN** the theme becomes Follow system theme and the Restore control is disabled

#### Scenario: Options are labelled
- **WHEN** a screen reader reads the options in the sheet
- **THEN** each option has a visible text label naming its mode

#### Scenario: No theme control in the top bar
- **WHEN** the app is shown on any screen
- **THEN** the top bar has no theme button

### Requirement: The theme is applied from the start
The saved theme SHALL be in use from the first frame the app draws after it starts, so there is no moment in which the app shows the wrong colours before switching to the saved theme.

#### Scenario: Saved dark on a light device
- **WHEN** the theme is saved as Dark, the device is set to light, and the app starts
- **THEN** the first screen is drawn in the dark appearance

#### Scenario: Saved name shown at once
- **WHEN** a name is saved and the app starts
- **THEN** the top bar shows the name's title on the first screen, not "OpenAdhd" first
