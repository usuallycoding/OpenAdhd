# Spec Delta

## MODIFIED Requirements

### Requirement: Template schedule
A template SHALL have a schedule that decides on which days it is added to Today automatically: None, Daily, Weekly, Date or Period, offered in that order in the template form. None means the template is only added by hand and is the default.

#### Scenario: Default
- **WHEN** the user creates a template without choosing a schedule
- **THEN** its schedule is None

#### Scenario: Order of the options
- **WHEN** the user opens the template form
- **THEN** the schedule options are listed as None, Daily, Weekly, Date, Period

#### Scenario: Date
- **WHEN** the user saves a template with the schedule Date and a chosen day
- **THEN** the template is due on that day only

#### Scenario: Weekly
- **WHEN** the user saves a template with the schedule Weekly and Monday, Wednesday and Friday selected
- **THEN** the template is due on every Monday, Wednesday and Friday

#### Scenario: Period
- **WHEN** the user saves a template with the schedule Period from 3 March to 7 March
- **THEN** the template is due on each day from 3 March to 7 March, both included

#### Scenario: Daily
- **WHEN** the user saves a template with the schedule Daily
- **THEN** the template is due every day
