# task-templates Specification

## Purpose

Lets the user maintain a library of reusable tasks, each classified by difficulty tier and required energy level, to draw from when planning the day.

## Requirements

### Requirement: Create a task template
The app SHALL let the user create a task template with a non-empty name, an optional description, a difficulty tier from 1 to 4, an energy level of Low, Medium or High, and subtasks within the limit for its tier.

#### Scenario: Valid template
- **WHEN** the user enters the name "Reply to emails", picks tier 2 and energy Medium, and saves
- **THEN** the template appears in the template list with those values

#### Scenario: Empty name
- **WHEN** the user tries to save a template whose name is empty or only whitespace
- **THEN** the template is not saved and the user is told a name is required

#### Scenario: Template with description and subtasks
- **WHEN** the user saves "Clean the kitchen" with a description and the subtasks "Dishes", "Wipe counters" and "Take out trash"
- **THEN** the template is saved with that description and those three subtasks in that order

### Requirement: Edit a task template
The app SHALL let the user change the name, description, tier, energy level and subtasks of an existing template.

#### Scenario: Change tier
- **WHEN** the user edits a template from tier 2 to tier 4 and saves
- **THEN** the list shows the template at tier 4

#### Scenario: Edit does not alter past entries
- **WHEN** a template already assigned to today is edited
- **THEN** the existing Today entry keeps the name, description, tier, energy and subtasks it was assigned with

#### Scenario: Change subtasks
- **WHEN** the user removes one subtask and adds another, and saves
- **THEN** the template lists the remaining subtasks followed by the new one

### Requirement: Delete a task template
The app SHALL let the user delete a template, and deleting MUST NOT remove Today entries or ledger records that came from it.

#### Scenario: Delete assigned template
- **WHEN** the user deletes a template that is on today's list
- **THEN** the template leaves the template list and the Today entry remains

### Requirement: List task templates
The app SHALL show all task templates with their tier, energy level, and the number of subtasks when it has any.

#### Scenario: Empty library
- **WHEN** no templates exist
- **THEN** the template list shows an empty state that invites the user to create one

#### Scenario: Template with subtasks
- **WHEN** a template has three subtasks
- **THEN** its list item shows that it has 3 subtasks

### Requirement: Task description
A template SHALL have an optional free-text description, shown when the user edits it and on the Today entries created from it.

#### Scenario: No description
- **WHEN** the user saves a template without a description
- **THEN** the template is saved and no description is shown for it

### Requirement: Template subtasks
A template SHALL be able to hold an ordered, flat list of subtasks, each with a non-empty title, but never more subtasks than its tier's coin pool and never more than 10: 5 for tier 1 and 10 for tiers 2, 3 and 4. Subtasks MUST NOT have subtasks, descriptions, tiers or energy levels of their own.

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
