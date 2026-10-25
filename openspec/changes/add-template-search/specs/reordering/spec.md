# Spec Delta

## ADDED Requirements

### Requirement: Reorder is unavailable while searching
On the Tasks and Rewards tabs the Reorder toggle SHALL NOT be offered while that tab's search holds text, and typing in the search while Reorder is on SHALL turn Reorder off. Clearing the search makes the toggle available again but does not turn Reorder back on.

#### Scenario: Search hides the toggle
- **WHEN** the Tasks tab has three templates and the search holds "dis"
- **THEN** no Reorder toggle is offered for it

#### Scenario: Typing ends Reorder
- **WHEN** Reorder is on in the Tasks tab and the user types in the search
- **THEN** Reorder is off and the normal rows are shown

#### Scenario: Clearing the search
- **WHEN** the search is cleared
- **THEN** the Reorder toggle is offered again and Reorder is off

#### Scenario: Each tab on its own
- **WHEN** the Tasks search holds text and the user opens the Rewards tab with no search text
- **THEN** the Reorder toggle is offered there
