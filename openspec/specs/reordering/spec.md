# reordering Specification

## Purpose
Lets the user put Today's tasks, task templates and reward templates in the order they want, using a Reorder mode with move up and move down controls, and keeps that order across restarts.

## Requirements

### Requirement: Reorder mode
Today, the Tasks tab of Templates and the Rewards tab of Templates SHALL each offer a Reorder toggle in the top bar. While it is on, each movable row shows a move up and a move down control. Turning it off MUST restore the normal rows. The mode is not saved.

#### Scenario: Turning it on
- **WHEN** the user turns Reorder on in the Tasks tab with three templates
- **THEN** each of the three rows shows move up and move down controls

#### Scenario: Turning it off
- **WHEN** the user turns Reorder off
- **THEN** the rows show their normal controls again and no move controls

#### Scenario: Nothing to reorder
- **WHEN** a list has fewer than two movable items
- **THEN** no Reorder toggle is offered for it

### Requirement: Moving an item
Using a row's move up or move down control SHALL swap it with its neighbour and save the new order immediately. The first movable row MUST NOT be able to move up and the last movable row MUST NOT be able to move down.

#### Scenario: Move down
- **WHEN** the user moves the first of three task templates down
- **THEN** it is now second, and the previous second is first

#### Scenario: Edges
- **WHEN** Reorder is on
- **THEN** the first row's move up control and the last row's move down control are disabled

#### Scenario: Order is saved
- **WHEN** the user moves a reward template and the app is restarted
- **THEN** the reward templates are still in the new order

### Requirement: Today reorders pending entries only
On Today, only pending entries SHALL be movable, and completed entries MUST be shown after all pending ones. Completing an entry moves it to the completed group, and the remaining pending entries keep their order. While Reorder is on, Today MUST NOT offer completing, removing or subtask controls.

#### Scenario: Pending entries move among themselves
- **WHEN** Today has pending A, B, C and the user moves C up
- **THEN** the pending order is A, C, B

#### Scenario: Completed entries stay below
- **WHEN** Today has pending A and B and completed C, and the user moves B up
- **THEN** the order is B, A, C and C has no move controls

#### Scenario: Completing sends an entry down
- **WHEN** the user completes the first of three pending entries
- **THEN** it is shown after the two remaining pending entries

#### Scenario: Hidden controls
- **WHEN** Reorder is on in Today
- **THEN** no completion checkbox, remove button or subtask row is shown

### Requirement: Templates reorder while editing is hidden
On the Tasks and Rewards tabs, while Reorder is on, the edit and delete controls and the add button MUST be hidden so a row cannot be changed or removed by accident.

#### Scenario: Controls hidden
- **WHEN** Reorder is on in the Tasks tab
- **THEN** no edit, delete or add control is shown

### Requirement: New templates go to the top
A newly created task template or reward template SHALL be placed first in its list, ahead of the ones that already exist, and a task added to today SHALL be placed after the pending entries already on today's list. Today's order applies to that day only. Lists that already exist keep their order.

#### Scenario: New template
- **WHEN** the user creates a task template while two exist
- **THEN** it is the first in the list and the two others follow in their previous order

#### Scenario: New reward template
- **WHEN** the user creates a reward template while two exist
- **THEN** it is the first in the Rewards tab, and the first of its tier on the Shop shelf for tiers 1 to 4

#### Scenario: After reordering
- **WHEN** the user has moved task templates into a new order and then creates another
- **THEN** the new one is first and the others keep the order the user gave them

#### Scenario: Add to today
- **WHEN** Today has pending A and B and the user adds a template
- **THEN** the new entry is third among the pending entries

#### Scenario: New day
- **WHEN** the date changes to the next day and the user adds tasks
- **THEN** the new day's entries are ordered independently of the previous day

### Requirement: Order is shown elsewhere
The Add to today picker SHALL list task templates in the task template order. The Shop SHALL list the rewards of each of tiers 1 to 4 in the reward template order, and tier 5 MUST stay ordered by price.

#### Scenario: Picker order
- **WHEN** the user moves a task template to the top and opens Add to today
- **THEN** that template is the first in the picker

#### Scenario: Shop order
- **WHEN** two tier 2 rewards are swapped in the Rewards tab
- **THEN** the Shop lists them in the swapped order within tier 2

#### Scenario: Tier 5 stays by price
- **WHEN** tier 5 rewards are reordered in the Rewards tab
- **THEN** the Shop still lists tier 5 from lowest to highest price
