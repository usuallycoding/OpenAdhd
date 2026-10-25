# Spec Delta

## ADDED Requirements

### Requirement: Template schedule
A template SHALL have a schedule that decides on which days it is added to Today automatically: None, Date, Weekly, Period or Daily. None means the template is only added by hand and is the default.

#### Scenario: Default
- **WHEN** the user creates a template without choosing a schedule
- **THEN** its schedule is None

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

### Requirement: Valid schedules
A Weekly schedule SHALL have at least one weekday, and a Period SHALL have an end date later than its start date. A schedule that breaks either rule MUST NOT be saved, and the form SHALL say why. A Date or Period that lies wholly in the past is allowed and is shown as ended.

#### Scenario: Weekly without a day
- **WHEN** the user tries to save a Weekly schedule with no weekday selected
- **THEN** the template is not saved and the user is told to choose at least one day

#### Scenario: Period ends before it starts
- **WHEN** the user tries to save a Period whose end date is the same as or before its start date
- **THEN** the template is not saved and the user is told the end must be after the start

#### Scenario: Ended period
- **WHEN** a Period ended last week
- **THEN** the template is still listed, its schedule is shown as ended, and it is never added to Today

### Requirement: Template reminder
A template SHALL be able to have a reminder: a time of day and a repeat of Off or every 15, 30, 60 or 120 minutes. A repeat can be chosen only while a time is set, and a new reminder SHALL start with every 30 minutes selected. A template has no reminder by default. The reminder is applied to the entries the schedule generates.

#### Scenario: Set a reminder
- **WHEN** the user sets 09:00 and every 60 minutes on a template and saves
- **THEN** the template keeps that time and repeat

#### Scenario: Repeat needs a time
- **WHEN** a template has no reminder time
- **THEN** the form offers no repeat choice

#### Scenario: Remove the reminder
- **WHEN** the user clears the time of a template's reminder and saves
- **THEN** the template has no reminder

#### Scenario: Not tied to the schedule
- **WHEN** a template with the schedule None has a reminder time
- **THEN** it is saved, and the reminder has no effect until the template is scheduled

### Requirement: Schedule shown in the list
A template item in the list SHALL show a short line for its schedule unless it is None: the date for Date, the weekday names for Weekly, the first and last date for Period, "Every day" for Daily, and "Ended" for a Date or Period in the past.

#### Scenario: Weekly line
- **WHEN** a template is scheduled for Monday, Wednesday and Friday
- **THEN** its list item shows "Mon, Wed, Fri"

#### Scenario: No schedule
- **WHEN** a template has the schedule None
- **THEN** its list item shows no schedule line
