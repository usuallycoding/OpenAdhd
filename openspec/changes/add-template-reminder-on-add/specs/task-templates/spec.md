# Spec Delta

## ADDED Requirements

### Requirement: Template reminder permission notice
When saving a template with a reminder time ends with notification permission refused, the app SHALL close the form, keep the saved template, and show a short message that notifications are off for OpenAdhd.

#### Scenario: Refused after saving
- **WHEN** the user saves a template with a reminder time and refuses the notification permission
- **THEN** the form closes, the template is listed with its reminder, and a message says notifications are off for OpenAdhd

#### Scenario: Granted
- **WHEN** the user saves a template with a reminder time and allows notifications
- **THEN** the form closes and no message is shown
