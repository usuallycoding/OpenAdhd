# Spec Delta

## MODIFIED Requirements

### Requirement: Valid reminder times
A reminder's start SHALL be later than the current time, on today's date, and earlier than the stop time from Settings. Anything else MUST NOT be saved, and the sheet SHALL say why.

#### Scenario: Time has passed
- **WHEN** it is 15:00 and the user picks 14:00 and saves
- **THEN** nothing is saved and the sheet says that time has already passed

#### Scenario: After the stop time
- **WHEN** the stop time is 22:00 and the user picks 22:30 and saves
- **THEN** nothing is saved and the sheet says reminders stop at 22:00

#### Scenario: Just before the stop time
- **WHEN** the stop time is 22:00 and the user picks 21:55 with every 30 minutes
- **THEN** it is saved and fires once, at 21:55

#### Scenario: Stop time with minutes
- **WHEN** the stop time is 22:30 and the user picks 22:15 and saves, and then picks 22:30 and saves
- **THEN** the first is saved, and the second is not and the sheet says reminders stop at 22:30
