# Spec Delta

## ADDED Requirements

### Requirement: Scheduled templates are added to today
When the app starts, and when the date changes while it is running, the app SHALL add one entry to Today for each template that is due that day, copying the template as when it is added by hand. Only the current day is generated: days the app was not opened are not filled in.

#### Scenario: Due on start
- **WHEN** the app starts on a Wednesday and a template is scheduled Weekly on Wednesday
- **THEN** Today has an entry for it with the template's name, description, tier, energy and subtasks, not completed

#### Scenario: Date changes while running
- **WHEN** the app is running at midnight and a Daily template exists
- **THEN** the new day's Today has an entry for it

#### Scenario: Not due
- **WHEN** the app starts on a Tuesday and the only template is scheduled Weekly on Wednesday
- **THEN** nothing is added to Today

#### Scenario: Missed days
- **WHEN** the app was not opened for three days and a Daily template exists
- **THEN** only today gets an entry, and the earlier days stay empty

### Requirement: Generated once per day
A template SHALL add at most one entry per day by schedule, however often the app starts, and deleting that entry MUST NOT bring it back the same day. Adding the same template by hand is unaffected.

#### Scenario: Restart
- **WHEN** the app is restarted the same day after an entry was generated
- **THEN** Today still has that one entry and no second one

#### Scenario: Deleted entry stays deleted
- **WHEN** the user deletes the generated entry and restarts the app
- **THEN** the entry does not come back

#### Scenario: Also added by hand
- **WHEN** a template that is due today has already been generated and the user adds it by hand
- **THEN** Today has two entries for it, and none is generated again

### Requirement: Scheduling changes apply right away
A template that is created or edited so that it is due today, and has not yet been generated today, SHALL add its entry to Today at once. Editing a template MUST NOT alter entries already on Today.

#### Scenario: New daily template
- **WHEN** the user saves a new Daily template
- **THEN** Today gets its entry immediately, once

#### Scenario: Edited to include today
- **WHEN** a template scheduled Weekly on Mondays is edited on a Wednesday to include Wednesday
- **THEN** its entry is added to today

#### Scenario: Already generated
- **WHEN** a template was generated today, its entry was deleted, and the template is edited
- **THEN** the entry is not added again
