# Spec Delta

## MODIFIED Requirements

### Requirement: Today is a single local day
The Today list SHALL show only entries assigned for the current calendar day on the device, and entries from earlier days MUST NOT appear in it or be completable from it.

#### Scenario: New day
- **WHEN** the device date changes to the next day
- **THEN** the Today list is empty of the previous day's entries

#### Scenario: Empty Today
- **WHEN** nothing is assigned for today
- **THEN** the Today screen shows "Nothing planned for today. Tap + to add a task." and no separate button for adding a task, because the + button is on screen

## ADDED Requirements

### Requirement: The picker leads to the Templates screen when there are no templates
When there are no task templates, the Add to today picker SHALL show its message about creating a template first together with a Take me there button. Pressing it SHALL close the picker, show the Templates screen on its Tasks tab and open the form to create a task template. With at least one template the picker MUST NOT show the button.

#### Scenario: Take me there
- **WHEN** no task templates exist, the user opens the Add to today picker and presses Take me there
- **THEN** the picker closes, the Templates screen is shown on its Tasks tab and the form to create a task template is open

#### Scenario: Templates exist
- **WHEN** the user opens the Add to today picker with task templates
- **THEN** the picker shows no Take me there button
