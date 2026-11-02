# task-templates Specification

## Purpose

Lets the user maintain a library of reusable tasks, each classified by difficulty tier and required energy level, to draw from when planning the day.

## Requirements

### Requirement: Create a task template
The app SHALL let the user create a task template with a non-empty name, an optional description, a difficulty tier from 1 to 4, an energy level of Low, Medium or High, and subtasks within the limit for its tier.

#### Scenario: Valid template
- **WHEN** the user enters the name "Reply to emails", picks tier 2 and energy Medium, and saves
- **THEN** the template appears in the template list with those values

#### Scenario: Empty name
- **WHEN** the user tries to save a template whose name is empty or only whitespace
- **THEN** the template is not saved and the user is told a name is required

#### Scenario: Template with description and subtasks
- **WHEN** the user saves "Clean the kitchen" with a description and the subtasks "Dishes", "Wipe counters" and "Take out trash"
- **THEN** the template is saved with that description and those three subtasks in that order

### Requirement: Edit a task template
The app SHALL let the user change the name, description, tier, energy level and subtasks of an existing template.

#### Scenario: Change tier
- **WHEN** the user edits a template from tier 2 to tier 4 and saves
- **THEN** the list shows the template at tier 4

#### Scenario: Edit does not alter past entries
- **WHEN** a template already assigned to today is edited
- **THEN** the existing Today entry keeps the name, description, tier, energy and subtasks it was assigned with

#### Scenario: Change subtasks
- **WHEN** the user removes one subtask and adds another, and saves
- **THEN** the template lists the remaining subtasks followed by the new one

### Requirement: Delete a task template
The app SHALL let the user delete a template, and deleting MUST NOT remove Today entries or ledger records that came from it.

#### Scenario: Delete assigned template
- **WHEN** the user deletes a template that is on today's list
- **THEN** the template leaves the template list and the Today entry remains

### Requirement: List task templates
The app SHALL show all task templates with their tier, energy level, and the number of subtasks when it has any.

#### Scenario: Empty library
- **WHEN** no templates exist
- **THEN** the template list shows an empty state that invites the user to create one

#### Scenario: Template with subtasks
- **WHEN** a template has three subtasks
- **THEN** its list item shows that it has 3 subtasks

### Requirement: Task description
A template SHALL have an optional free-text description, shown when the user edits it and on the Today entries created from it.

#### Scenario: No description
- **WHEN** the user saves a template without a description
- **THEN** the template is saved and no description is shown for it

### Requirement: Template subtasks
A template SHALL be able to hold an ordered, flat list of subtasks, each with a non-empty title, but never more subtasks than its tier's coin pool and never more than the subtask ceiling from Settings: by default 5 for tier 1 and 10 for tiers 2, 3 and 4. Subtasks MUST NOT have subtasks, descriptions, tiers or energy levels of their own.

#### Scenario: Blank subtask title
- **WHEN** the user tries to save a template with a subtask whose title is empty or only whitespace
- **THEN** the template is not saved and the user is told every subtask needs a title

#### Scenario: No nesting
- **WHEN** the user edits a subtask
- **THEN** the only thing that can be edited is its title

#### Scenario: Limit follows the tier
- **WHEN** a tier 1 template already has 5 subtasks
- **THEN** the app offers no way to add a sixth, and a tier 1 template with more than 5 subtasks is not saved

#### Scenario: Higher tier allows more
- **WHEN** the user changes that template to tier 2
- **THEN** it can have up to 10 subtasks

#### Scenario: Overall ceiling
- **WHEN** a tier 4 template already has 10 subtasks
- **THEN** the app offers no way to add an eleventh, even though its coin pool is 40

#### Scenario: Lowering the tier below the subtask count
- **WHEN** a tier 2 template has 8 subtasks and the user changes it to tier 1 and saves
- **THEN** the template is not saved and the user is told a tier 1 task can have at most 5 subtasks

#### Scenario: Changed ceiling
- **WHEN** the subtask ceiling in Settings is 6
- **THEN** a tier 4 template can have at most 6 subtasks, and a tier 1 template still at most 5

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

### Requirement: Template reminder permission notice
When saving a template with a reminder time ends with notification permission refused, the app SHALL close the form, keep the saved template, and show a short message that notifications are off for OpenAdhd.

#### Scenario: Refused after saving
- **WHEN** the user saves a template with a reminder time and refuses the notification permission
- **THEN** the form closes, the template is listed with its reminder, and a message says notifications are off for OpenAdhd

#### Scenario: Granted
- **WHEN** the user saves a template with a reminder time and allows notifications
- **THEN** the form closes and no message is shown

### Requirement: Template reminder kind
A template's reminder SHALL have a kind, Notification or Alarm, chosen in the template form on Android and iOS together with the time and repeat, and it SHALL be a Notification by default. The choice is offered only while a reminder time is set, and is not offered on the desktop app.

#### Scenario: Choose an alarm
- **WHEN** the user sets a reminder time 09:00, chooses Alarm and saves the template
- **THEN** the template keeps an alarm reminder at 09:00

#### Scenario: Default kind
- **WHEN** the user sets a reminder time and saves without choosing a kind
- **THEN** the template's reminder is a Notification

#### Scenario: Needs a time
- **WHEN** a template has no reminder time
- **THEN** the form offers no kind choice

#### Scenario: Editing
- **WHEN** the user edits a template whose reminder is an alarm
- **THEN** Alarm is selected in the form

#### Scenario: On the desktop
- **WHEN** the user opens the template form on the desktop app
- **THEN** no kind choice is offered

### Requirement: Search the task templates
The Tasks tab SHALL have a search field above the list whenever at least one task template exists. While it holds text, the list SHALL show only the templates whose name or description contains that text, ignoring upper and lower case and leading and trailing spaces, in the list's order. With the field empty every template is listed. The field SHALL offer a clear control while it holds text and MUST NOT take focus by itself.

#### Scenario: Filter by name
- **WHEN** the templates are "Laundry", "Dishes" and "Taxes" and the user types "dis"
- **THEN** only "Dishes" is listed

#### Scenario: Filter by description
- **WHEN** a template named "Reset the flat" has the description "wash the dishes" and the user types "dishes"
- **THEN** "Reset the flat" is listed

#### Scenario: Ignores case and outer spaces
- **WHEN** the user types "  LAUNDRY " and a template is named "Laundry"
- **THEN** "Laundry" is listed

#### Scenario: Empty field lists everything
- **WHEN** the user clears the search text
- **THEN** every task template is listed again

#### Scenario: Clear control
- **WHEN** the search field holds text
- **THEN** it offers a clear control that empties the field, and offers none while the field is empty

#### Scenario: Not focused on opening
- **WHEN** the user opens the Tasks tab
- **THEN** the search field is not focused and the keyboard is not shown

#### Scenario: No templates
- **WHEN** no task template exists
- **THEN** the tab shows no search field and keeps its message about creating the first one

### Requirement: Nothing matches the task search
When the search leaves no task template, the Tasks tab SHALL say so in place of the list, naming the text, and MUST NOT show the message for a tab without templates.

#### Scenario: No match
- **WHEN** no template matches the typed text "xyz"
- **THEN** the tab shows "No task matches xyz" in place of the list

### Requirement: The task search is kept and cleared
The text of the Tasks search SHALL be kept while the app runs, including when the user goes to the Rewards tab or another screen and comes back, and MUST NOT be saved. Saving a new task template SHALL clear it, so the new template is not hidden. Editing or deleting a template MUST leave it as it is.

#### Scenario: Kept across tabs
- **WHEN** the user types "dis" on the Tasks tab, swipes to Rewards and back
- **THEN** the search still holds "dis" and the list is still filtered

#### Scenario: Not saved
- **WHEN** the app is restarted
- **THEN** the search is empty

#### Scenario: Creating clears it
- **WHEN** the search holds "dis" and the user saves a new template named "Taxes"
- **THEN** the search is cleared and "Taxes" is listed

#### Scenario: Editing keeps it
- **WHEN** the search holds "dis" and the user edits "Dishes"
- **THEN** the search still holds "dis"

### Requirement: The task search keyboard can be put away
While the task search field has focus, the on-screen keyboard SHALL be hidden and the field SHALL lose focus when the user taps anywhere on the list area outside the field, scrolls the list, or presses the keyboard's Done key. The text stays as typed.

#### Scenario: Tap outside
- **WHEN** the keyboard is shown for the task search and the user taps an empty part of the list area or on a row
- **THEN** the keyboard is hidden and the field is no longer focused

#### Scenario: Scrolling
- **WHEN** the keyboard is shown for the task search and the user scrolls the list
- **THEN** the keyboard is hidden

#### Scenario: Done key
- **WHEN** the user presses Done on the keyboard
- **THEN** the keyboard is hidden and the search text is unchanged

#### Scenario: Tapping the field
- **WHEN** the user taps the search field
- **THEN** it takes focus and the keyboard is shown

### Requirement: The create form can be opened on request
When another part of the app asks for a new task template, the Tasks tab SHALL open its form to create one, exactly as if the user had pressed +, whether or not the Tasks tab was on screen when the request was made. A request MUST open the form once and MUST NOT open it again later.

#### Scenario: Requested from another screen
- **WHEN** the app shows the Templates screen on its Tasks tab because a new task template was requested
- **THEN** the form to create a task template is open

#### Scenario: Not opened twice
- **WHEN** the user closes that form and leaves and returns to the Tasks tab
- **THEN** the form is not opened again
