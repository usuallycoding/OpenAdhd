## MODIFIED Requirements

### Requirement: Only days with tasks can be picked
In the calendar dialog, every day before today SHALL be selectable, whether or not an entry was assigned to it. Days that have at least one entry assigned MUST be marked with a dot under the day number, and days without entries MUST NOT. Today and every later day MUST NOT be selectable, even if today has entries. The marker MUST stay visible on the selected day.

#### Scenario: Day with tasks
- **WHEN** entries were assigned on the 20th and the 20th is before today
- **THEN** the 20th shows a dot and can be selected in the calendar

#### Scenario: Day without tasks
- **WHEN** nothing was assigned on the 21st and the 21st is before today
- **THEN** the 21st shows no dot and can be selected, and choosing it shows "Nothing was planned on this day"

#### Scenario: Today and later
- **WHEN** the calendar is open
- **THEN** today and every later day cannot be selected, even if today has entries

#### Scenario: Newly assigned day
- **WHEN** an entry is assigned to today and the next day comes
- **THEN** that day, now an earlier day, shows a dot in the calendar

#### Scenario: Marker on the selected day
- **WHEN** the shown day has entries and the calendar is opened
- **THEN** that day is highlighted as selected and still shows its dot

#### Scenario: Moving between months
- **WHEN** the user pages to an earlier or later month in the calendar
- **THEN** that month's days are shown with their markers, and months entirely after today cannot be reached
