# Spec Delta

## MODIFIED Requirements

### Requirement: Settings and their defaults
The settings and their defaults SHALL be: theme, follow system theme; dynamic color, dynamic; name, empty; debt limit, 500 coins; subtask ceiling, 10; the coins paid by task tiers 1 to 4, 5, 10, 20 and 40; the Shop price of reward tiers 1 to 4, 20, 50, 120 and 300; critical hit chance, 1 in 8; critical hit multiplier, 2; roll upgrade chance, 1 in 6; the time reminders stop at, 22:00.

#### Scenario: Defaults match the original rules
- **WHEN** no setting has been changed
- **THEN** the debt limit, payouts, prices, subtask limit and chances behave as described in the other specs

#### Scenario: Stop time default
- **WHEN** no setting has been changed
- **THEN** "Stop reminders at" shows 22:00

#### Scenario: Theme default
- **WHEN** no setting has been changed
- **THEN** Theme shows "Follow system theme"

#### Scenario: Dynamic color default
- **WHEN** no setting has been changed
- **THEN** Dynamic color is on, where the platform offers it

### Requirement: Value limits
The name SHALL be trimmed and at most 40 characters long. The debt limit SHALL be a whole number from 0 to 100000. The subtask ceiling SHALL be from 1 to 50. A task tier payout SHALL be from 1 to 1000. A reward tier price SHALL be from 1 to 100000. The critical hit chance and the roll upgrade chance SHALL each be 1 in a whole number from 2 to 1000, and the critical hit multiplier a whole number from 2 to 10. The time reminders stop at SHALL be from 00:01 to 23:59.

#### Scenario: Debt limit of zero
- **WHEN** the user saves a debt limit of 0
- **THEN** it is saved, and no purchase may take the balance below zero

#### Scenario: Out of range
- **WHEN** the user tries to save a critical hit chance of 1 in 1
- **THEN** nothing is saved and the form says the chance must be between 2 and 1000

#### Scenario: Long name
- **WHEN** the user tries to save a name of 41 characters
- **THEN** nothing is saved and the form says the name can be at most 40 characters

#### Scenario: Stop hour out of range
- **WHEN** the user picks 00:00 as the time reminders stop at and saves
- **THEN** nothing is saved and the dialog says the time must be between 00:01 and 23:59

## ADDED Requirements

### Requirement: Stop reminders at setting
The Reminders group SHALL have a "Stop reminders at" setting that is a time of day with minutes. The row SHALL show it as hours and minutes on 24 hours, such as 22:30. Tapping it SHALL open a 24-hour clock picker with the current time, and saving a valid time SHALL apply it at once and keep it. A saved value that is only an hour, such as 22, MUST be read as that hour and 00 minutes, and the value written from now on SHALL be hours and minutes.

#### Scenario: Opening the clock
- **WHEN** the user taps "Stop reminders at"
- **THEN** a 24-hour clock picker opens showing the current stop time

#### Scenario: Choosing a time with minutes
- **WHEN** the user picks 22:30 in the clock and confirms
- **THEN** the row shows 22:30 and reminders stop at 22:30 from then on

#### Scenario: Cancelling
- **WHEN** the user opens the clock, changes the time and cancels
- **THEN** the row keeps its time

#### Scenario: An hour saved by an earlier version
- **WHEN** the stored value of the setting is 18, from an earlier version or from a backup of one
- **THEN** the row shows 18:00 and reminders stop at 18:00

#### Scenario: Restoring the default
- **WHEN** the stop time is 21:15 and the user taps Restore on it
- **THEN** it shows 22:00 again

#### Scenario: A value that is not a time
- **WHEN** the stored value of the setting is not a valid time, such as 24 or 25:00
- **THEN** it is ignored and the setting shows its default, 22:00
