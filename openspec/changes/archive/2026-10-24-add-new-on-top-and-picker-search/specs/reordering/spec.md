# Spec Delta

## RENAMED Requirements
- FROM: `### Requirement: New items go to the end`
- TO: `### Requirement: New templates go to the top`

## MODIFIED Requirements

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
