# day-planner Specification

## Purpose

Lets the user choose which tasks they intend to do today and mark them done, forming the daily list that drives coin rewards.

## Requirements

### Requirement: Assign a template to today
The app SHALL let the user add any task template to today's list. The same template MAY be added more than once.

#### Scenario: Add to today
- **WHEN** the user picks a template to add to today
- **THEN** a new entry with that template's name, tier and energy appears on the Today list, not completed

#### Scenario: Same template twice
- **WHEN** the user adds the same template twice
- **THEN** two separate entries appear on the Today list

### Requirement: Remove an entry from today
The app SHALL let the user remove a not-yet-completed entry from today's list.

#### Scenario: Remove pending entry
- **WHEN** the user removes a pending entry
- **THEN** it leaves the Today list and no coins are awarded

### Requirement: Complete a Today entry
The app SHALL let the user mark a pending Today entry as completed, and an entry MUST be completable only once.

#### Scenario: Complete a task
- **WHEN** the user marks a pending entry complete
- **THEN** the entry is shown as completed and the coin reward for its tier is awarded

#### Scenario: Completing twice
- **WHEN** a completed entry is completed again
- **THEN** no additional coins are awarded

### Requirement: Today is a single local day
The Today list SHALL show only entries assigned for the current calendar day on the device, and entries from earlier days MUST NOT appear in it or be completable from it.

#### Scenario: New day
- **WHEN** the device date changes to the next day
- **THEN** the Today list is empty of the previous day's entries

#### Scenario: Empty Today
- **WHEN** nothing is assigned for today
- **THEN** the Today screen shows an empty state with a way to add a task
