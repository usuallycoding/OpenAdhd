# task-randomizer Specification

## Purpose

Takes the decision of what to do next away from the user: given how much energy they have right now, it suggests one task template that fits, which they can add to today, swap for another, or dismiss.

## Requirements

### Requirement: Pick one for me
The Today screen SHALL offer a "Pick one for me" action that starts a pick by asking for the user's current energy: Low, Medium or High. The action MUST be available whether or not today has entries, but the empty-day message MUST offer only "Add a task".

#### Scenario: Starting a suggestion
- **WHEN** the user taps "Pick one for me"
- **THEN** the app asks for their current energy with the options Low, Medium and High

#### Scenario: Available on a busy day
- **WHEN** today already has entries
- **THEN** the "Pick one for me" action is still offered

#### Scenario: Empty day
- **WHEN** today has no entries
- **THEN** the empty-day message offers "Add a task" and no "Pick one for me" button

### Requirement: Better fits are more likely
Among the templates that can be suggested, each SHALL be chosen with a chance proportional to a weight: 4 when its energy equals the chosen energy, 2 when it is one level below and 1 when it is two levels below.

#### Scenario: High energy day
- **WHEN** the user chooses High and the templates are one High, one Medium and one Low
- **THEN** they are suggested with chances of 4 in 7, 2 in 7 and 1 in 7

#### Scenario: Medium energy day
- **WHEN** the user chooses Medium and the templates are one Medium and one Low
- **THEN** the Medium template is suggested with a chance of 2 in 3 and the Low one with 1 in 3

#### Scenario: Equal fits
- **WHEN** two eligible templates have the same energy
- **THEN** they have the same chance

### Requirement: Which tasks can be picked
A pick SHALL be an entry on today's list that is not completed and whose energy is not higher than the chosen energy. Completed entries and subtasks MUST NOT be picked.

#### Scenario: Low energy
- **WHEN** the user chooses Low and today has pending entries of Low, Medium and High energy
- **THEN** only the Low entry can be picked

#### Scenario: Medium energy
- **WHEN** the user chooses Medium and today has pending entries of Low, Medium and High energy
- **THEN** only the Low and Medium entries can be picked

#### Scenario: Completed entries
- **WHEN** an entry on today's list is completed
- **THEN** it is not picked

### Requirement: The pick is shown in the list
Once an entry is picked, the energy question SHALL close and the list SHALL scroll so the entry is visible, and the entry SHALL be highlighted for a short while. Nothing MUST be added, changed or spent by picking, and no result dialog is shown. Tapping "Pick one for me" again picks anew, and MAY pick the same entry.

#### Scenario: Picked entry is revealed
- **WHEN** the user chooses an energy and an entry far down the list is picked
- **THEN** the dialog closes, the list scrolls so that entry is visible and it is highlighted briefly

#### Scenario: Nothing is changed by picking
- **WHEN** an entry is picked
- **THEN** today's list, its order and the balance are unchanged

#### Scenario: Picking again
- **WHEN** the user picks again after a pick
- **THEN** a new pick is made among the same eligible entries, and it may be the same entry

### Requirement: When there is nothing to pick
If no entry is eligible for the chosen energy, the app SHALL say nothing fits that energy and offer to choose a different energy, and MUST NOT scroll or highlight anything. If today has no pending entries at all, the app SHALL say there is nothing left to pick today without asking for an energy.

#### Scenario: Nothing fits
- **WHEN** the user chooses Low and no pending entry on today's list has Low energy
- **THEN** the app says nothing fits Low energy and offers to choose a different energy

#### Scenario: Nothing pending
- **WHEN** every entry on today's list is completed and the user taps "Pick one for me"
- **THEN** the app says there is nothing left to pick today and does not ask for energy
