# Spec Delta

## MODIFIED Requirements

### Requirement: Top bar
The app SHALL display a top app bar above its content showing the app name centered. When a name is set in Settings, the title SHALL be that name followed by "'s OpenAdhd".

#### Scenario: Top bar visible
- **WHEN** the app is shown on any supported platform
- **THEN** a top bar with the title "OpenAdhd" centered is displayed above the content

#### Scenario: Title with a name
- **WHEN** the name in Settings is "Sam"
- **THEN** the top bar title reads "Sam's OpenAdhd"

#### Scenario: Name removed
- **WHEN** the user restores the name to its default
- **THEN** the top bar title reads "OpenAdhd"
