# day-history Specification

## Purpose
Lets the user look back at what was planned and done on an earlier day, read-only, so past days can be reviewed without being able to change them or the coins they earned.

## Requirements

### Requirement: History tab
The Today screen SHALL have two tabs, Today and History. The History tab MUST open on yesterday's date, and the chosen date MUST be kept while the app is running and not saved.

#### Scenario: Default day
- **WHEN** the user opens the History tab for the first time since the app started
- **THEN** the date field shows yesterday's date and the list shows the entries of that day

#### Scenario: Kept while the app runs
- **WHEN** the user chooses another day, opens the Shop, and returns to the History tab
- **THEN** the chosen day is still shown

#### Scenario: After a restart
- **WHEN** the app is restarted
- **THEN** the History tab opens on yesterday's date

### Requirement: Choose the day
The History tab SHALL show the chosen date in a date field and let the user move to the previous or the next day and pick a day from a calendar dialog. The chosen day MUST always be before today: the next-day control MUST NOT move onto today or a later day.

#### Scenario: Previous day
- **WHEN** the user moves to the previous day while yesterday is shown
- **THEN** the day before yesterday is shown

#### Scenario: Next day stops before today
- **WHEN** yesterday is shown
- **THEN** the next-day control is not available

#### Scenario: Pick from the calendar
- **WHEN** the user opens the calendar, selects an available day and confirms with OK
- **THEN** the dialog closes and the date field and the list show that day

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

### Requirement: List for the chosen day
Under the date field the History tab SHALL list the entries of the chosen day in the same order as Today (pending ones in their saved order, then completed ones, most recently done first). Each entry shows its name, description, tier, energy, progress or reward and done time, and, while expanded, its subtasks with their completed state and coin share.

#### Scenario: Completed day
- **WHEN** the chosen day had a task completed at 14:32 and a pending task
- **THEN** the pending task is listed first, then the completed one showing "Done 14:32", and neither can be toggled

#### Scenario: Subtasks shown
- **WHEN** an expanded entry of the chosen day has three subtasks, one completed
- **THEN** the list shows the three subtasks in order with their coin shares and the completed one marked

### Requirement: History is read-only
The History tab MUST NOT offer any way to complete or uncheck an entry or a subtask, remove an entry, reorder entries, or add a task, and no coins may change because of anything done in it.

#### Scenario: No controls
- **WHEN** the History tab shows any day
- **THEN** there is no remove button, no move control, no add button and no "Pick one for me" button

#### Scenario: Balance untouched
- **WHEN** the user browses and collapses entries on the History tab
- **THEN** the coin balance and the ledger do not change

### Requirement: Collapse and expand in history
Tapping a History entry that has subtasks SHALL toggle whether its subtasks are shown, with the same defaults as Today: a pending entry starts expanded and a completed entry starts collapsed. The state is kept while the app runs and is not saved. Tapping an entry without subtasks MUST NOT change anything.

#### Scenario: Completed entry starts collapsed
- **WHEN** the chosen day holds a completed entry with three subtasks
- **THEN** it shows its name, description and "3 of 3 done" with its subtasks hidden

#### Scenario: Expanding
- **WHEN** the user taps that entry
- **THEN** its subtasks are shown, still without anything to check

### Requirement: Empty day
When the chosen day has no entries, the History tab SHALL show "Nothing was planned on this day" in place of the list, and no add button.

#### Scenario: Yesterday was empty
- **WHEN** nothing was assigned yesterday and the History tab is opened
- **THEN** the date field shows yesterday and the empty message is shown

### Requirement: Reorder only on Today
The top bar Reorder control SHALL be offered only while the Today tab is shown, and MUST NOT be offered on the History tab.

#### Scenario: Switching to History
- **WHEN** the user turns Reorder mode on, then opens the History tab
- **THEN** Reorder mode is off and no Reorder control is offered

#### Scenario: Back to Today
- **WHEN** the user returns to the Today tab with two or more pending entries
- **THEN** the Reorder control is offered again
