# Spec Delta

## MODIFIED Requirements

### Requirement: Today entry details
Each Today entry SHALL show its description when it has one, and, while it is expanded, list its subtasks in order with their completed state and coin share.

#### Scenario: Entry with description and subtasks
- **WHEN** an expanded entry has a description and three subtasks
- **THEN** the Today list shows the description and the three subtasks, each with its coin share

## ADDED Requirements

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
