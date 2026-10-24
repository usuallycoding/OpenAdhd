# Spec Delta

## MODIFIED Requirements

### Requirement: Template subtasks
A template SHALL be able to hold an ordered, flat list of subtasks, each with a non-empty title, but never more subtasks than its tier's coin pool and never more than the subtask ceiling from Settings: by default 5 for tier 1 and 10 for tiers 2, 3 and 4. Subtasks MUST NOT have subtasks, descriptions, tiers or energy levels of their own.

#### Scenario: Blank subtask title
- **WHEN** the user tries to save a template with a subtask whose title is empty or only whitespace
- **THEN** the template is not saved and the user is told every subtask needs a title

#### Scenario: No nesting
- **WHEN** the user edits a subtask
- **THEN** the only thing that can be edited is its title

#### Scenario: Limit follows the tier
- **WHEN** a tier 1 template already has 5 subtasks
- **THEN** the app offers no way to add a sixth, and a tier 1 template with more than 5 subtasks is not saved

#### Scenario: Higher tier allows more
- **WHEN** the user changes that template to tier 2
- **THEN** it can have up to 10 subtasks

#### Scenario: Overall ceiling
- **WHEN** a tier 4 template already has 10 subtasks
- **THEN** the app offers no way to add an eleventh, even though its coin pool is 40

#### Scenario: Lowering the tier below the subtask count
- **WHEN** a tier 2 template has 8 subtasks and the user changes it to tier 1 and saves
- **THEN** the template is not saved and the user is told a tier 1 task can have at most 5 subtasks

#### Scenario: Changed ceiling
- **WHEN** the subtask ceiling in Settings is 6
- **THEN** a tier 4 template can have at most 6 subtasks, and a tier 1 template still at most 5
