# form-sheets Specification

## Purpose
Presents the app's forms and its template picker as bottom sheets that slide up from the bottom edge, so the controls are within thumb reach and the screen behind stays in view, while short decisions stay as dialogs.

## Requirements

### Requirement: Forms and the picker open as bottom sheets
Creating or editing a task template, creating or editing a reward template, and choosing a template to add to today SHALL each open in a bottom sheet, not in a centered dialog.

#### Scenario: New task template
- **WHEN** the user taps the add button on the Tasks tab of Templates
- **THEN** the task form opens as a bottom sheet

#### Scenario: New reward template
- **WHEN** the user taps the add button on the Rewards tab of Templates
- **THEN** the reward form opens as a bottom sheet

#### Scenario: Editing
- **WHEN** the user taps edit on a task template or a reward template
- **THEN** its form opens as a bottom sheet with the current values

#### Scenario: Add to today
- **WHEN** the user taps the add button on Today
- **THEN** the list of templates to add opens as a bottom sheet

### Requirement: Closing a sheet
A form sheet SHALL close without saving when the user cancels, swipes it down or taps outside it. Saving valid input SHALL close it. Invalid input MUST keep it open with the same inline errors as before.

#### Scenario: Swipe down discards
- **WHEN** the user has typed a name in the task form and swipes the sheet down
- **THEN** the sheet closes and no template is created

#### Scenario: Cancel
- **WHEN** the user taps Cancel in a form sheet
- **THEN** the sheet closes and nothing is saved

#### Scenario: Valid save
- **WHEN** the user taps Save with valid input
- **THEN** the item is saved and the sheet closes

#### Scenario: Invalid save
- **WHEN** the user taps Save in the task form with an empty name
- **THEN** the sheet stays open and tells the user a name is required

### Requirement: Tall content stays usable
When a form is taller than the available height, its fields SHALL scroll while the Save and Cancel actions stay reachable at the bottom of the sheet, and the focused field MUST NOT be hidden by the on-screen keyboard.

#### Scenario: Many subtasks
- **WHEN** a task form has ten subtasks on a short screen
- **THEN** the fields scroll and Save and Cancel remain visible

### Requirement: Short decisions stay dialogs
Confirming a redeem or a roll, the roll reveal, and the "Pick one for me" energy question SHALL remain centered dialogs.

#### Scenario: Confirming a purchase
- **WHEN** the user taps Redeem on a reward
- **THEN** the confirmation is a centered dialog
