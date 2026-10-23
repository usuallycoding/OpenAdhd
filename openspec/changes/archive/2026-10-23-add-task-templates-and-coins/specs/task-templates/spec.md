# Spec Delta

## Purpose

Lets the user maintain a library of reusable tasks, each classified by difficulty tier and required energy level, to draw from when planning the day.

## ADDED Requirements

### Requirement: Create a task template
The app SHALL let the user create a task template with a non-empty name, a difficulty tier from 1 to 4, and an energy level of Low, Medium or High.

#### Scenario: Valid template
- **WHEN** the user enters the name "Reply to emails", picks tier 2 and energy Medium, and saves
- **THEN** the template appears in the template list with those values

#### Scenario: Empty name
- **WHEN** the user tries to save a template whose name is empty or only whitespace
- **THEN** the template is not saved and the user is told a name is required

### Requirement: Edit a task template
The app SHALL let the user change the name, tier and energy level of an existing template.

#### Scenario: Change tier
- **WHEN** the user edits a template from tier 2 to tier 4 and saves
- **THEN** the list shows the template at tier 4

#### Scenario: Edit does not alter past entries
- **WHEN** a template already assigned to today is edited
- **THEN** the existing Today entry keeps the name, tier and energy it was assigned with

### Requirement: Delete a task template
The app SHALL let the user delete a template, and deleting MUST NOT remove Today entries or ledger records that came from it.

#### Scenario: Delete assigned template
- **WHEN** the user deletes a template that is on today's list
- **THEN** the template leaves the template list and the Today entry remains

### Requirement: List task templates
The app SHALL show all task templates with their tier and energy level.

#### Scenario: Empty library
- **WHEN** no templates exist
- **THEN** the template list shows an empty state that invites the user to create one
