# Spec Delta

## ADDED Requirements

### Requirement: The create form can be opened on request
When another part of the app asks for a new reward template, the Rewards tab SHALL open its form to create one, exactly as if the user had pressed +, even when the Rewards tab was not on screen when the request was made. A request MUST open the form once and MUST NOT open it again later.

#### Scenario: Requested from the Shop
- **WHEN** the app shows the Templates screen on its Rewards tab because a new reward template was requested
- **THEN** the form to create a reward template is open

#### Scenario: Not opened twice
- **WHEN** the user closes that form and leaves and returns to the Rewards tab
- **THEN** the form is not opened again
