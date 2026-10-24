# day-planner Specification

## Purpose

Lets the user choose which tasks they intend to do today and mark them done, forming the daily list that drives coin rewards.

## Requirements

### Requirement: Assign a template to today
The app SHALL let the user add any task template to today's list. The same template MAY be added more than once. The entry copies the template's name, description, tier, energy and subtasks, all not completed.

#### Scenario: Add to today
- **WHEN** the user picks a template to add to today
- **THEN** a new entry with that template's name, description, tier, energy and subtasks appears on the Today list, not completed

#### Scenario: Same template twice
- **WHEN** the user adds the same template twice
- **THEN** two separate entries appear on the Today list

### Requirement: Remove an entry from today
The app SHALL let the user remove an entry from today's list only while none of it has been completed, so that removing never discards earned coins.

#### Scenario: Remove pending entry
- **WHEN** the user removes a pending entry
- **THEN** it leaves the Today list and no coins are awarded

#### Scenario: Entry in progress cannot be removed
- **WHEN** at least one subtask of an entry has been completed
- **THEN** the app offers no way to remove that entry from today

### Requirement: Complete a Today entry
The app SHALL let the user mark a pending Today entry that has no subtasks as completed, and such an entry MUST be completable only once. An entry that has subtasks MUST NOT be completable directly.

#### Scenario: Complete a task
- **WHEN** the user marks a pending entry without subtasks complete
- **THEN** the entry is shown as completed and the full reward for its tier is awarded

#### Scenario: Completing twice
- **WHEN** a completed entry is completed again
- **THEN** no additional coins are awarded

#### Scenario: Entry with subtasks
- **WHEN** an entry has subtasks
- **THEN** the app offers no control to complete the entry as a whole

### Requirement: Today is a single local day
The Today list SHALL show only entries assigned for the current calendar day on the device, and entries from earlier days MUST NOT appear in it or be completable from it.

#### Scenario: New day
- **WHEN** the device date changes to the next day
- **THEN** the Today list is empty of the previous day's entries

#### Scenario: Empty Today
- **WHEN** nothing is assigned for today
- **THEN** the Today screen shows an empty state with a way to add a task

### Requirement: Complete a subtask
The app SHALL let the user mark a pending subtask of a Today entry as completed, awarding that subtask's coin share. A subtask MUST be completable only once and MUST NOT be un-completed.

#### Scenario: Complete a subtask
- **WHEN** the user completes a subtask worth 7 coins
- **THEN** the subtask is shown as completed and 7 coins are awarded

#### Scenario: Completing twice
- **WHEN** a completed subtask is completed again
- **THEN** no additional coins are awarded

#### Scenario: No undo
- **WHEN** a subtask has been completed
- **THEN** the app offers no way to mark it pending again

### Requirement: Entry completes with its last subtask
An entry with subtasks SHALL be shown as completed exactly when all its subtasks are completed, and as in progress while some but not all are.

#### Scenario: In progress
- **WHEN** 2 of an entry's 3 subtasks are completed
- **THEN** the entry shows progress of 2 of 3 and is not shown as completed

#### Scenario: Last subtask
- **WHEN** the user completes the last pending subtask of an entry
- **THEN** the entry is shown as completed

### Requirement: Today entry details
Each Today entry SHALL show its description when it has one, and, while it is expanded, list its subtasks in order with their completed state and coin share.

#### Scenario: Entry with description and subtasks
- **WHEN** an expanded entry has a description and three subtasks
- **THEN** the Today list shows the description and the three subtasks, each with its coin share

### Requirement: Collapse and expand subtasks
Tapping a Today entry that has subtasks SHALL toggle whether its subtasks are shown. Entries MUST start expanded. A collapsed entry SHALL still show its name, description and how many of its subtasks are done, and a marker SHALL show whether it is expanded or collapsed. Tapping an entry without subtasks MUST NOT change anything. Each entry's state is kept while the app is running and is not saved.

#### Scenario: Collapsing
- **WHEN** the user taps an expanded entry that has three subtasks
- **THEN** its subtasks are hidden and the entry shows its name, its description and "0 of 3 done"

#### Scenario: Expanding
- **WHEN** the user taps a collapsed entry
- **THEN** its subtasks are shown again

#### Scenario: Starting expanded
- **WHEN** an entry with subtasks is added to today
- **THEN** its subtasks are shown

#### Scenario: Entry without subtasks
- **WHEN** the user taps an entry that has no subtasks
- **THEN** nothing changes and its own checkbox still completes it

#### Scenario: Kept while the app runs
- **WHEN** the user collapses an entry, opens the Shop, and returns to Today
- **THEN** the entry is still collapsed

#### Scenario: After a restart
- **WHEN** the app is restarted
- **THEN** every entry with subtasks starts expanded
