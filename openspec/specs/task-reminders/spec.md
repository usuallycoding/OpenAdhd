# task-reminders Specification

## Purpose

Lets the user ask for a notification about a task on Today that is not done yet, repeating until the task is finished, so a planned task is not forgotten once the app is closed.

## Requirements

### Requirement: Entry options sheet
A Today entry that is not completed SHALL show a "..." button on its right, in place of the remove button, that opens a bottom sheet titled with the entry's name. The sheet SHALL hold the entry's reminder controls and a Delete task button, and the Delete task button MUST be offered only while nothing in the entry has been completed. A completed entry, an entry on a past day and an entry while Reorder mode is on MUST NOT show the button.

#### Scenario: Pending entry
- **WHEN** the user looks at a pending entry on Today
- **THEN** it shows a "..." button on its right and no remove button

#### Scenario: Open the sheet
- **WHEN** the user taps the "..." button
- **THEN** a bottom sheet opens with the entry's name, the reminder controls and a Delete task button

#### Scenario: Entry in progress
- **WHEN** at least one subtask of an entry is done but the entry is not complete
- **THEN** its sheet offers the reminder controls and no Delete task button

#### Scenario: Delete from the sheet
- **WHEN** the user taps Delete task in the sheet of a pending entry with nothing done
- **THEN** the entry leaves the Today list, the sheet closes, and no coins are awarded

#### Scenario: Done entry
- **WHEN** an entry is completed
- **THEN** it shows no "..." button

#### Scenario: Past days and Reorder mode
- **WHEN** the user looks at an entry in the History tab, or Reorder mode is on
- **THEN** no "..." button is shown

### Requirement: Setting a reminder
The sheet SHALL let the user choose when the reminder starts and how it repeats, and save them. The start SHALL be chosen from the presets In 30 minutes, In 1 hour and This evening (18:00), or from a time picked on a clock. The repeat SHALL be one of Off or every 15, 30, 60 or 120 minutes, and a new reminder SHALL start with every 30 minutes selected. An entry has at most one reminder, and saving replaces the earlier one.

#### Scenario: A preset
- **WHEN** it is 14:00, the user chooses In 30 minutes and saves
- **THEN** the entry has a reminder starting at 14:30 that repeats every 30 minutes

#### Scenario: A picked time
- **WHEN** the user picks 16:15 on the clock, chooses every 60 minutes and saves
- **THEN** the entry has a reminder starting at 16:15 that repeats every 60 minutes

#### Scenario: No repeat
- **WHEN** the user saves a reminder with the repeat set to Off
- **THEN** the reminder fires once, at its start time

#### Scenario: Replacing
- **WHEN** an entry has a reminder and the user saves a different one
- **THEN** only the new reminder applies

#### Scenario: Presets that cannot work
- **WHEN** it is 19:00 and the stop time is 22:00
- **THEN** This evening is shown as unavailable

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

### Requirement: When a reminder fires
A reminder SHALL fire at its start time and then at every repeat interval after it, as long as the entry is not completed and the time is earlier than the stop time from Settings. It MUST NOT fire on a later day, and times already past are never fired late.

#### Scenario: Repeating until the stop time
- **WHEN** a reminder starts at 15:00, repeats every 30 minutes and the stop time is 22:00
- **THEN** it fires at 15:00, 15:30 and so on up to 21:30, and not at 22:00

#### Scenario: Once
- **WHEN** a reminder starts at 15:00 with no repeat
- **THEN** it fires at 15:00 only

#### Scenario: Changed stop time
- **WHEN** the stop time in Settings is changed to 18:00
- **THEN** reminders that would fire at 18:00 or later no longer fire

### Requirement: Reminder lifecycle
A reminder SHALL stop when its entry is completed, and unchecking the entry SHALL bring back the reminder times that are still to come. Deleting the entry removes its reminder, and the sheet SHALL offer Remove reminder while one is set. An entry in progress keeps its reminder until it is completed. Reminders MUST end at midnight with the day of their entry.

#### Scenario: Completing stops it
- **WHEN** an entry with a repeating reminder is completed
- **THEN** no further reminder fires for it

#### Scenario: Unchecking brings it back
- **WHEN** the entry is unchecked at 16:10 and its reminder repeats every 30 minutes until 22:00
- **THEN** the next reminder fires at 16:30

#### Scenario: Partly done
- **WHEN** one subtask of two is done
- **THEN** the reminder keeps firing until the second is done

#### Scenario: Remove reminder
- **WHEN** the user taps Remove reminder
- **THEN** no reminder fires for the entry and the sheet shows no reminder

#### Scenario: Delete the entry
- **WHEN** the user deletes an entry that has a reminder
- **THEN** no reminder fires for it

#### Scenario: A new day
- **WHEN** an entry is still pending at midnight
- **THEN** it stays on its own day and its reminders have ended

### Requirement: The reminder is visible
An entry with a reminder SHALL show it on its row, as the start time and the repeat, and its sheet SHALL show the current reminder and the controls to change or remove it.

#### Scenario: On the row
- **WHEN** an entry has a reminder at 15:00 repeating every 30 minutes
- **THEN** its row shows a line such as "Reminder 15:00, every 30 min"

#### Scenario: In the sheet
- **WHEN** the user opens the sheet of an entry with a reminder
- **THEN** the current start and repeat are selected and a Remove reminder button is shown

### Requirement: The notification
A reminder SHALL appear as a notification with the title "Still to do" and the entry's name as its text, and tapping it SHALL open the app on the Today tab, showing the task that triggered it. A notification MUST NOT be shown for an entry that has been completed or deleted.

#### Scenario: Text
- **WHEN** a reminder fires for "Clean the kitchen"
- **THEN** the notification reads "Still to do" and "Clean the kitchen"

#### Scenario: Tap
- **WHEN** the user taps the notification
- **THEN** the app opens on the Today tab and shows "Clean the kitchen"

#### Scenario: Done before it fires
- **WHEN** the entry is completed before its next reminder time
- **THEN** that reminder does not appear

### Requirement: Notification permission
The app SHALL ask for permission to show notifications the first time the user saves a reminder, not when the app starts. A refused permission MUST NOT discard the reminder, and the sheet SHALL say that notifications are turned off for OpenAdhd and where to turn them on.

#### Scenario: First reminder
- **WHEN** the user saves a reminder for the first time on a device that needs permission
- **THEN** the system asks for permission to show notifications

#### Scenario: Refused
- **WHEN** the user refuses
- **THEN** the reminder is still saved and the sheet says notifications are off and how to turn them on

#### Scenario: App start
- **WHEN** the app starts
- **THEN** no notification permission is asked

### Requirement: Delivery on each platform
On Android and iOS reminders SHALL be delivered when the app is closed, and on Android also after the device restarts. On the desktop app reminders SHALL be delivered only while the app is running, as a system notification, or as a message inside the app when the system cannot show one. When the app is opened again it SHALL work out the next reminder from the current time and MUST NOT show the ones that were missed.

#### Scenario: Phone with the app closed
- **WHEN** a reminder is due while the app is closed on Android or iOS
- **THEN** the notification appears

#### Scenario: After a restart on Android
- **WHEN** the device is restarted with reminders still to come
- **THEN** they still fire at their times

#### Scenario: Desktop closed
- **WHEN** a reminder is due while the desktop app is not running
- **THEN** nothing is shown, and when the app is opened the next time is used

#### Scenario: Desktop running
- **WHEN** a reminder is due while the desktop app is running
- **THEN** a notification or an in-app message shows the task's name

### Requirement: Generated entry starts with the template's reminder
An entry generated from a scheduled template, or added to Today by hand, whose template has a reminder SHALL start with a reminder at the template's time of day, on the entry's date, with the template's repeat. Times that have already passed are never fired, so a repeating reminder resumes at its next time. The stop time from Settings applies as for any reminder.

#### Scenario: Time still ahead
- **WHEN** an entry is generated at 08:00 from a template with a reminder at 09:00 every 60 minutes
- **THEN** the entry has a reminder starting at 09:00 that repeats every 60 minutes

#### Scenario: Time already passed
- **WHEN** an entry is generated at 14:10 from a template with a reminder at 09:00 every 30 minutes
- **THEN** its next reminder fires at 14:30

#### Scenario: Passed without repeat
- **WHEN** an entry is generated at 14:10 from a template with a reminder at 09:00 and no repeat
- **THEN** no reminder fires for it

#### Scenario: Added with the + button
- **WHEN** the user adds a template with a reminder at 09:00 every 30 minutes to Today with the + button
- **THEN** the entry has a reminder starting at 09:00 that repeats every 30 minutes

#### Scenario: Added without a template reminder
- **WHEN** the user adds a template that has no reminder to Today with the + button
- **THEN** the entry has no reminder

#### Scenario: Added twice
- **WHEN** the user adds the same template with a reminder to Today twice
- **THEN** each entry has its own copy of the reminder

### Requirement: Generated entry's reminder is its own
The reminder of a generated or hand-added entry SHALL be editable and removable in its "..." sheet like any other, and changing it MUST NOT change the template.

#### Scenario: Editing on Today
- **WHEN** the user changes the reminder of a generated entry to 16:00 every 15 minutes
- **THEN** only that entry changes, and the template keeps its time and repeat

#### Scenario: Added by hand
- **WHEN** the user adds a template with a reminder to Today by hand and removes the entry's reminder in its "..." sheet
- **THEN** only that entry loses its reminder, and the template keeps its time and repeat

### Requirement: Permission when saving a template reminder
Saving a template that has a reminder time SHALL ask for permission to show notifications when the platform needs it. A refusal MUST NOT discard the template or its reminder, and the user SHALL be told after the form closes that notifications are turned off for OpenAdhd and where to turn them on. Saving a template without a reminder time, and starting the app, MUST NOT ask.

#### Scenario: First template reminder
- **WHEN** the user saves a template with a reminder time on a device that needs permission and has not granted it
- **THEN** the system asks for permission to show notifications

#### Scenario: Refused
- **WHEN** the user refuses
- **THEN** the template is saved with its reminder and a message says notifications are off for OpenAdhd and where to turn them on

#### Scenario: No reminder time
- **WHEN** the user saves a template without a reminder time
- **THEN** no permission is asked

#### Scenario: Already granted
- **WHEN** notification permission is already granted and the user saves a template with a reminder time
- **THEN** nothing is asked and no message is shown

#### Scenario: Desktop
- **WHEN** the user saves a template with a reminder time on the desktop app
- **THEN** no permission is asked

### Requirement: Tapping a notification shows its task
On Android and iOS, tapping a reminder notification SHALL open the app on the Today tab for today, scroll Today's list so the task that triggered it is visible, and highlight that task for a short while. Nothing MUST be added, changed or spent by the tap. This SHALL work whether the app was closed, in the background or in front.

#### Scenario: App closed
- **WHEN** the app is closed and the user taps the reminder for the 12th task on the list
- **THEN** the app opens on Today, the list is scrolled to that task and it is highlighted briefly

#### Scenario: App in the background
- **WHEN** the app is running in the background and the user taps a reminder
- **THEN** the app comes to the front on Today with that task scrolled to and highlighted

#### Scenario: From History
- **WHEN** the user is on the History tab and taps a reminder
- **THEN** the Today tab is shown with the task scrolled to and highlighted

#### Scenario: From another destination
- **WHEN** the user is on the Shop and taps a reminder
- **THEN** Today is shown with the task scrolled to and highlighted

#### Scenario: Reorder mode
- **WHEN** Reorder mode is on and the user taps a reminder
- **THEN** Reorder mode ends and the task is scrolled to and highlighted in the normal list

#### Scenario: Several notifications
- **WHEN** reminders for "Taxes" and "Laundry" are both showing and the user taps the one for "Taxes"
- **THEN** "Taxes" is scrolled to and highlighted, not "Laundry"

#### Scenario: Shown once
- **WHEN** the user taps a reminder, then leaves Today and comes back
- **THEN** the task is not highlighted again

### Requirement: A tapped task that cannot be shown
When the task of a tapped notification is no longer on Today's list, tapping SHALL still open the app on the Today tab, with no error and nothing highlighted. A task that is completed and still on the list SHALL be scrolled to and highlighted like any other.

#### Scenario: Task completed
- **WHEN** the user completes a task, then taps the notification that was still showing for it
- **THEN** the app opens on Today and the completed task is scrolled to and highlighted

#### Scenario: Task deleted
- **WHEN** the task of a notification was deleted and the user taps the notification
- **THEN** the app opens on Today with nothing highlighted

#### Scenario: Notification from an earlier day
- **WHEN** the user taps a notification for a task from yesterday
- **THEN** the app opens on today's Today with nothing highlighted
