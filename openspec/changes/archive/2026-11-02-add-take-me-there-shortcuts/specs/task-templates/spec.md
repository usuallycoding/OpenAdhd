# Spec Delta

## ADDED Requirements

### Requirement: The create form can be opened on request
When another part of the app asks for a new task template, the Tasks tab SHALL open its form to create one, exactly as if the user had pressed +, whether or not the Tasks tab was on screen when the request was made. A request MUST open the form once and MUST NOT open it again later.

#### Scenario: Requested from another screen
- **WHEN** the app shows the Templates screen on its Tasks tab because a new task template was requested
- **THEN** the form to create a task template is open

#### Scenario: Not opened twice
- **WHEN** the user closes that form and leaves and returns to the Tasks tab
- **THEN** the form is not opened again
