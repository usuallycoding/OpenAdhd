# day-planner Specification

## Purpose

Lets the user choose which tasks they intend to do today and mark them done, forming the daily list that drives coin rewards.

## Requirements

### Requirement: Assign a template to today
The app SHALL let the user add any task template to today's list. The same template MAY be added more than once. The entry copies the template's name, description, tier, energy and subtasks, all not completed.

#### Scenario: Add to today
- **WHEN** the user picks a template to add to today
- **THEN** a new entry with that template's name, description, tier, energy and subtasks appears on the Today list, not completed

#### Scenario: Same template twice
- **WHEN** the user adds the same template twice
- **THEN** two separate entries appear on the Today list

### Requirement: Remove an entry from today
The app SHALL let the user remove an entry from today's list only while none of it has been completed, so that removing never discards earned coins. The control to remove it SHALL be the Delete task button in the entry's options sheet, opened with the entry's "..." button.

#### Scenario: Remove pending entry
- **WHEN** the user removes a pending entry from its options sheet
- **THEN** it leaves the Today list and no coins are awarded

#### Scenario: Entry in progress cannot be removed
- **WHEN** at least one subtask of an entry has been completed
- **THEN** the app offers no way to remove that entry from today

#### Scenario: Not on the row
- **WHEN** the user looks at a pending entry on Today
- **THEN** the row has no remove button of its own

### Requirement: Complete a Today entry
The app SHALL let the user mark a pending Today entry that has no subtasks as completed, and such an entry MUST be completable only while it is pending. An entry that has subtasks MUST NOT be completable directly.

#### Scenario: Complete a task
- **WHEN** the user marks a pending entry without subtasks complete
- **THEN** the entry is shown as completed and the full reward for its tier is awarded

#### Scenario: Completing twice
- **WHEN** a completed entry is completed again without being unchecked in between
- **THEN** no additional coins are awarded

#### Scenario: Entry with subtasks
- **WHEN** an entry has subtasks
- **THEN** the app offers no control to complete the entry as a whole

### Requirement: Today is a single local day
The Today list SHALL show only entries assigned for the current calendar day on the device, and entries from earlier days MUST NOT appear in it or be completable from it.

#### Scenario: New day
- **WHEN** the device date changes to the next day
- **THEN** the Today list is empty of the previous day's entries

#### Scenario: Empty Today
- **WHEN** nothing is assigned for today
- **THEN** the Today screen shows "Nothing planned for today. Tap + to add a task." and no separate button for adding a task, because the + button is on screen

### Requirement: Entry completes with its last subtask
An entry with subtasks SHALL be shown as completed exactly when all its subtasks are completed, and as in progress while some but not all are.

#### Scenario: In progress
- **WHEN** 2 of an entry's 3 subtasks are completed
- **THEN** the entry shows progress of 2 of 3 and is not shown as completed

#### Scenario: Last subtask
- **WHEN** the user completes the last pending subtask of an entry
- **THEN** the entry is shown as completed

### Requirement: Today entry details
Each Today entry SHALL show its description when it has one, and, while it is expanded, list its subtasks in order with their completed state and coin share.

#### Scenario: Entry with description and subtasks
- **WHEN** an expanded entry has a description and three subtasks
- **THEN** the Today list shows the description and the three subtasks, each with its coin share

### Requirement: Collapse and expand subtasks
Tapping a Today entry that has subtasks SHALL toggle whether its subtasks are shown. A collapsed entry SHALL still show its name, description and how many of its subtasks are done, and a marker SHALL show whether it is expanded or collapsed. Tapping an entry without subtasks MUST NOT change anything. Each entry's state is kept while the app is running and is not saved.

#### Scenario: Collapsing
- **WHEN** the user taps an expanded entry that has three subtasks
- **THEN** its subtasks are hidden and the entry shows its name, its description and "0 of 3 done"

#### Scenario: Expanding
- **WHEN** the user taps a collapsed entry
- **THEN** its subtasks are shown again

#### Scenario: Entry without subtasks
- **WHEN** the user taps an entry that has no subtasks
- **THEN** nothing changes and its own checkbox still completes it

#### Scenario: Kept while the app runs
- **WHEN** the user collapses an entry, opens the Shop, and returns to Today
- **THEN** the entry is still collapsed

### Requirement: Default collapse state
A pending Today entry MUST start expanded and a completed entry MUST start collapsed, and an entry MUST return to that default whenever it becomes completed or pending again.

#### Scenario: Starting expanded
- **WHEN** an entry with subtasks is added to today
- **THEN** its subtasks are shown

#### Scenario: After a restart
- **WHEN** the app is restarted
- **THEN** every pending entry with subtasks starts expanded and every completed one starts collapsed

#### Scenario: Completed entries start collapsed
- **WHEN** Today is opened and holds a completed entry with three subtasks
- **THEN** the entry shows its name, its description and "3 of 3 done" with its subtasks hidden

#### Scenario: Finishing folds the entry
- **WHEN** the user completes the last pending subtask of an expanded entry
- **THEN** the entry moves to the done list collapsed

#### Scenario: Unchecking opens the entry
- **WHEN** the user unchecks a subtask of a completed entry
- **THEN** the entry is pending again and expanded, showing all its subtasks

#### Scenario: Opened done entry stays open
- **WHEN** the user expands a completed entry and then completes or unchecks nothing else in it
- **THEN** it stays expanded while the app runs

### Requirement: Complete a pending subtask
The app SHALL let the user mark a pending subtask of a Today entry as completed, awarding that subtask's coin share. A subtask MUST be completable only while it is pending.

#### Scenario: Complete a subtask
- **WHEN** the user completes a subtask worth 7 coins
- **THEN** the subtask is shown as completed and 7 coins are awarded

#### Scenario: Completing twice
- **WHEN** a completed subtask is completed again without being unchecked in between
- **THEN** no additional coins are awarded

### Requirement: Uncheck a completed task or subtask
The app SHALL let the user uncheck a completed Today task without subtasks and a completed subtask, with no confirmation. Unchecking MUST make the item pending again and take back every coin it has paid, critical-hit bonus included, and MUST confirm the undo with a short message that shows the coins taken back. Unchecking a pending item MUST change nothing.

#### Scenario: Uncheck a task
- **WHEN** the user unchecks a completed 20-coin task
- **THEN** the task is pending again, 20 coins are taken back and a message says so

#### Scenario: Uncheck a critical hit
- **WHEN** the user unchecks a subtask that paid 7 coins and a 7-coin critical-hit bonus
- **THEN** 14 coins are taken back

#### Scenario: Uncheck a subtask of a finished entry
- **WHEN** the user unchecks one subtask of a completed entry with three subtasks
- **THEN** that subtask is pending, the entry is shown as in progress with 2 of 3 done, and only that subtask's coins are taken back

#### Scenario: Entry removable again
- **WHEN** the user unchecks the only completed subtask of an entry
- **THEN** the entry has no completed part and can be removed from today again

#### Scenario: Check again
- **WHEN** an unchecked item is checked again
- **THEN** its coins are paid again, with a fresh chance of a critical hit

#### Scenario: Loop gains nothing
- **WHEN** the user checks and unchecks the same item several times
- **THEN** the balance after each uncheck is what it was before the item was first checked

### Requirement: Done entries are ordered by when they were done
On Today, completed entries SHALL be listed after all pending ones, ordered by done time with the most recently done first. The done time of an entry without subtasks is when it was checked, and the done time of an entry with subtasks is when its most recently checked subtask was checked. Reordering does not apply to completed entries.

#### Scenario: Newest first
- **WHEN** the user completes task A, then task B
- **THEN** the completed entries are listed B, then A

#### Scenario: Entry with subtasks uses its last subtask
- **WHEN** an entry with two subtasks is completed at 10:00 and 10:30 and a task is completed at 10:15
- **THEN** the entry is listed before the task, because its done time is 10:30

#### Scenario: Uncheck changes the done time
- **WHEN** the latest-checked subtask of a completed entry is unchecked
- **THEN** the entry is pending again, and once it is completed again its done time is the new completion

### Requirement: Done time is shown
A completed entry SHALL show when it was done, as the time of day on the device, for example "Done 14:32".

#### Scenario: Completed task
- **WHEN** a task is completed at 14:32
- **THEN** its entry shows "Done 14:32"

#### Scenario: Pending entry
- **WHEN** an entry is pending, even if some of its subtasks are done
- **THEN** it shows no done time

### Requirement: Search field in the Add to today picker
The Add to today picker SHALL have a search field above the task templates whenever at least one task template exists. The field MUST be focused, with the on-screen keyboard open, as soon as the picker opens, MUST start empty every time the picker opens, and SHALL offer a clear control while it holds text.

#### Scenario: Focus on open
- **WHEN** the user opens the Add to today picker with task templates
- **THEN** the search field is focused and the keyboard is open

#### Scenario: Starts empty each time
- **WHEN** the user types "dis", closes the picker and opens it again
- **THEN** the search field is empty and every template is listed

#### Scenario: Clear button
- **WHEN** the search field holds text
- **THEN** it offers a clear control that empties the field, and it offers none while the field is empty

#### Scenario: No templates
- **WHEN** no task template exists
- **THEN** the picker shows no search field and keeps its message about creating a template first

### Requirement: The picker lists the templates that match the search
While the search field holds text, the picker SHALL list only the task templates whose name or description contains that text, ignoring upper and lower case and leading and trailing spaces, in the task template order. With the field empty it SHALL list every task template. When none match it SHALL say so in place of the list. Tapping a listed template still adds it to today and closes the picker.

#### Scenario: Filter by name
- **WHEN** the templates are "Laundry", "Dishes" and "Taxes" and the user types "dis"
- **THEN** only "Dishes" is listed

#### Scenario: Ignores case
- **WHEN** the user types "LAUNDRY" and a template is named "Laundry"
- **THEN** "Laundry" is listed

#### Scenario: Filter by description
- **WHEN** a template named "Reset the flat" has the description "wash the dishes" and the user types "dishes"
- **THEN** "Reset the flat" is listed

#### Scenario: Keeps the template order
- **WHEN** two listed templates match the text
- **THEN** they appear in the task template order

#### Scenario: Empty field lists everything
- **WHEN** the user clears the search text
- **THEN** every task template is listed again

#### Scenario: Nothing matches
- **WHEN** no template matches the typed text "xyz"
- **THEN** the picker shows "No task matches xyz" in place of the list

#### Scenario: Picking from the results
- **WHEN** the user searches for "dis" and taps "Dishes"
- **THEN** "Dishes" is added to today and the picker closes

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

### Requirement: Energy filter in the Add to today picker
Under the search field, whenever at least one task template exists, the picker SHALL show the chips Low, Medium and High. At most one chip is selected; tapping a chip selects it and tapping the selected chip clears it. While one is selected the picker SHALL list only the templates with that energy, together with the text search. No chip is selected each time the picker opens.

#### Scenario: Filter by energy
- **WHEN** the templates are "Laundry" (Low), "Taxes" (High) and "Dishes" (Low) and the user selects Low
- **THEN** only "Laundry" and "Dishes" are listed

#### Scenario: Combined with the text
- **WHEN** the user selects Low and types "dis"
- **THEN** only "Dishes" is listed

#### Scenario: Clearing the filter
- **WHEN** the user taps the selected Low chip again
- **THEN** no chip is selected and every template matching the text is listed

#### Scenario: One at a time
- **WHEN** Low is selected and the user taps High
- **THEN** only High is selected and only High templates are listed

#### Scenario: Starts cleared
- **WHEN** the user selects High, closes the picker and opens it again
- **THEN** no chip is selected and every template is listed

#### Scenario: No templates
- **WHEN** no task template exists
- **THEN** the picker shows no energy chips

### Requirement: Nothing matches the filter
When the text search, the energy filter or both leave no template, the picker SHALL say so in place of the list, naming the energy filter when one is selected.

#### Scenario: Energy and text
- **WHEN** Low is selected, the user types "dis" and no Low template matches
- **THEN** the picker shows "No Low energy task matches dis"

#### Scenario: Energy only
- **WHEN** High is selected, the text is empty and no template has High energy
- **THEN** the picker shows "No High energy task"

### Requirement: Search and filters stay in view
The search field and the energy chips SHALL stay fixed at the top of the picker while the list of templates scrolls beneath them, so they are reachable however far the list has been scrolled.

#### Scenario: Scrolled list
- **WHEN** the list is longer than the sheet and the user scrolls it to the bottom
- **THEN** the search field and the energy chips are still visible and usable

#### Scenario: Typing after scrolling
- **WHEN** the user has scrolled the list down and then types in the search field
- **THEN** the list updates without the field having moved

### Requirement: The picker's keyboard can be put away
While the Add to today picker's search field has focus, the on-screen keyboard SHALL be hidden and the field SHALL lose focus when the user presses the keyboard's Done key, scrolls the list of templates, or taps empty space or the no-match message in the list area. The search text and the energy filter stay as they are, and tapping a listed template still adds it to today.

#### Scenario: Done key
- **WHEN** the user presses Done on the keyboard in the picker
- **THEN** the keyboard is hidden and the search text is unchanged

#### Scenario: Scrolling the results
- **WHEN** the keyboard is shown in the picker and the user scrolls the list
- **THEN** the keyboard is hidden

#### Scenario: Tapping the no-match message
- **WHEN** the keyboard is shown, nothing matches the text, and the user taps the message in the list area
- **THEN** the keyboard is hidden

#### Scenario: Tapping a template
- **WHEN** the keyboard is shown and the user taps a listed template
- **THEN** the template is added to today and the picker closes

#### Scenario: Tapping the field
- **WHEN** the user taps the search field after putting the keyboard away
- **THEN** it takes focus and the keyboard is shown again

### Requirement: The picker leads to the Templates screen when there are no templates
When there are no task templates, the Add to today picker SHALL show its message about creating a template first together with a Take me there button. Pressing it SHALL close the picker, show the Templates screen on its Tasks tab and open the form to create a task template. With at least one template the picker MUST NOT show the button.

#### Scenario: Take me there
- **WHEN** no task templates exist, the user opens the Add to today picker and presses Take me there
- **THEN** the picker closes, the Templates screen is shown on its Tasks tab and the form to create a task template is open

#### Scenario: Templates exist
- **WHEN** the user opens the Add to today picker with task templates
- **THEN** the picker shows no Take me there button
