# Spec Delta

## MODIFIED Requirements

### Requirement: Remove an entry from today
The app SHALL let the user remove an entry from today's list only while none of it has been completed, so that removing never discards earned coins. The control to remove it SHALL be the Delete task button in the entry's options sheet, opened with the entry's "..." button.

#### Scenario: Remove pending entry
- **WHEN** the user removes a pending entry from its options sheet
- **THEN** it leaves the Today list and no coins are awarded

#### Scenario: Entry in progress cannot be removed
- **WHEN** at least one subtask of an entry has been completed
- **THEN** the app offers no way to remove that entry from today

#### Scenario: Not on the row
- **WHEN** the user looks at a pending entry on Today
- **THEN** the row has no remove button of its own
