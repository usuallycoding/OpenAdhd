# Spec Delta

## MODIFIED Requirements

### Requirement: Settings tab
The app SHALL have a Settings destination in the bottom bar, after Shop, that lists every setting grouped as Profile, Coins, Subtasks, Shop, Luck and Reminders, followed by a Data section. Each setting SHALL show its name, its current value and its default.

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

### Requirement: Settings and their defaults
The settings and their defaults SHALL be: name, empty; debt limit, 500 coins; subtask ceiling, 10; the coins paid by task tiers 1 to 4, 5, 10, 20 and 40; the Shop price of reward tiers 1 to 4, 20, 50, 120 and 300; critical hit chance, 1 in 8; critical hit multiplier, 2; roll upgrade chance, 1 in 6; the hour reminders stop at, 22.

#### Scenario: Defaults match the original rules
- **WHEN** no setting has been changed
- **THEN** the debt limit, payouts, prices, subtask limit and chances behave as described in the other specs

#### Scenario: Stop time default
- **WHEN** no setting has been changed
- **THEN** "Stop reminders at" shows 22

### Requirement: Value limits
The name SHALL be trimmed and at most 40 characters long. The debt limit SHALL be a whole number from 0 to 100000. The subtask ceiling SHALL be from 1 to 50. A task tier payout SHALL be from 1 to 1000. A reward tier price SHALL be from 1 to 100000. The critical hit chance and the roll upgrade chance SHALL each be 1 in a whole number from 2 to 1000, and the critical hit multiplier a whole number from 2 to 10. The hour reminders stop at SHALL be a whole number from 1 to 23.

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
- **WHEN** the user tries to save 24 as the hour reminders stop at
- **THEN** nothing is saved and the form says it must be between 1 and 23
