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
A reminder SHALL fire at its start time and then at every repeat interval after it, as long as the entry is not completed and the time is earlier than the stop time from Settings. It MUST NOT fire on a later day, and times already past are never fired late, except on the desktop: a notification that came due while the computer was asleep is shown when it wakes if it is at most 2 minutes late, and an alarm that came due while it was asleep rings once when it wakes, as the desktop alarm rules say.

#### Scenario: Repeating until the stop time
- **WHEN** a reminder starts at 15:00, repeats every 30 minutes and the stop time is 22:00
- **THEN** it fires at 15:00, 15:30 and so on up to 21:30, and not at 22:00

#### Scenario: Once
- **WHEN** a reminder starts at 15:00 with no repeat
- **THEN** it fires at 15:00 only

#### Scenario: Changed stop time
- **WHEN** the stop time in Settings is changed to 18:00
- **THEN** reminders that would fire at 18:00 or later no longer fire

#### Scenario: Desktop wakes just after the time
- **WHEN** a desktop reminder is due at 15:00, the computer sleeps from 14:55 and wakes at 15:01
- **THEN** the notification is shown on waking

#### Scenario: Desktop wakes long after the time
- **WHEN** a desktop reminder is due at 15:00 and a repeat at 15:30, and the computer wakes at 15:20
- **THEN** the 15:00 reminder is not shown and the 15:30 one fires on time

#### Scenario: Desktop alarm wakes long after the time
- **WHEN** a desktop alarm is due at 15:00 and the computer wakes at 15:20
- **THEN** the alarm rings on waking, unlike a notification, which would not be shown

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
An entry with a reminder SHALL show it on its row, as its kind, the start time and the repeat, and its sheet SHALL show the current reminder and the controls to change or remove it.

#### Scenario: On the row
- **WHEN** an entry has a notification reminder at 15:00 repeating every 30 minutes
- **THEN** its row shows a line such as "Reminder 15:00, every 30 min"

#### Scenario: An alarm on the row
- **WHEN** an entry has an alarm reminder at 15:00 repeating every 30 minutes
- **THEN** its row shows a line such as "Alarm 15:00, every 30 min"

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
On Android and iOS reminders SHALL be delivered when the app is closed, and on Android also after the device restarts, including the reminders of scheduled templates whose entries have not been generated yet. On the desktop app reminders SHALL be delivered for as long as the app is running, including while its window is closed and it stays in the tray, as a system notification, or as a message inside the app when the system cannot show one. When the app is started again it SHALL work out the next reminder from the current time and MUST NOT show the ones that were missed.

#### Scenario: Phone with the app closed
- **WHEN** a reminder is due while the app is closed on Android or iOS
- **THEN** the notification appears

#### Scenario: Template reminder with the app closed
- **WHEN** a daily template's reminder is due and the app has not been opened that day on Android or iOS
- **THEN** the notification or alarm is delivered

#### Scenario: After a restart on Android
- **WHEN** the device is restarted with reminders still to come
- **THEN** they still fire at their times

#### Scenario: Desktop closed
- **WHEN** a reminder is due while the desktop app is not running because the user chose Quit or never started it
- **THEN** nothing is shown, and when the app is started the next time is used

#### Scenario: Desktop window closed
- **WHEN** a reminder is due while the desktop window is closed and the app is in the tray
- **THEN** a notification shows the task's name

#### Scenario: Desktop running
- **WHEN** a reminder is due while the desktop app is running
- **THEN** a notification or an in-app message shows the task's name

### Requirement: Scheduled template reminders are armed ahead
The app SHALL arm the reminders of scheduled templates for today and the following 6 days on iOS and the desktop, and the following 29 days on Android, without waiting for their entries to be generated. For each of those days on which a template with a reminder is due and has not yet generated its entry, the reminder fires at the template's time of day, repeats as the template says, and is a notification or an alarm as the template says, with the template's name as its text. The stop time from Settings, the rule that times already past are never fired, and the limits on armed alarms and notifications apply as for any reminder, and when not everything fits, the soonest times are armed. Once the entry of a day has been generated, only that entry's own reminder applies for that day, and no time MUST fire twice.

#### Scenario: App not opened in the morning
- **WHEN** a daily template has an alarm at 07:00 and the app was last opened yesterday evening
- **THEN** the alarm rings at 07:00 today without the app having been opened

#### Scenario: Weeks ahead on Android
- **WHEN** a daily template has a notification at 09:00 and the Android app was last opened 10 days ago
- **THEN** the notification is still delivered on each of those days, up to 29 days after the app last worked out its reminders

#### Scenario: Several days ahead
- **WHEN** a weekly template due on Monday and Thursday has a notification at 09:00 and the app was last opened on Sunday
- **THEN** the notifications for Monday and Thursday are armed, and none for the other days

#### Scenario: Entry already generated
- **WHEN** the entry of today's template has been generated and the user completes it before its reminder time
- **THEN** that reminder does not fire

#### Scenario: Entry deleted
- **WHEN** the user deletes the generated entry of today's template
- **THEN** the template's reminder does not fire again for today

#### Scenario: Not fired twice
- **WHEN** the app is opened at 08:00 and generates today's entry from a template with a reminder at 09:00
- **THEN** the reminder fires once at 09:00

#### Scenario: Stop time
- **WHEN** a template reminder starts at 09:00, repeats every 60 minutes and the stop time is 12:00
- **THEN** it is armed for 09:00, 10:00 and 11:00 on each planned day, and not for 12:00

#### Scenario: Not due
- **WHEN** a template has no schedule, or its schedule is not due on a day
- **THEN** nothing is armed for it that day

#### Scenario: Changed template
- **WHEN** the user changes a template's time or schedule
- **THEN** the planned reminders are worked out again from the new values

#### Scenario: Tapping a planned reminder
- **WHEN** the user taps a template's reminder before its entry exists
- **THEN** the app opens on Today, creates the day's entries as it does at start, and highlights nothing if the entry cannot be matched

### Requirement: Android arms the next reminders as they fire
On Android the app SHALL keep every planned reminder time, not only the ones that are armed, and whenever a reminder notification or an alarm fires, and after the device restarts, it SHALL arm the soonest times that are still to come within the limits on armed alarms and notifications, dropping the times already past. This MUST happen without the app being opened and without reading the database. At most 1000 times are kept, the soonest first.

#### Scenario: Armed times run out
- **WHEN** a repeating reminder has more future times than can be armed at once and the armed ones fire one after another with the app closed
- **THEN** each firing arms the next times, so the reminder keeps firing until the kept times are used up

#### Scenario: After a restart
- **WHEN** the device restarts with kept times that were not armed
- **THEN** the soonest of them are armed

#### Scenario: Past times
- **WHEN** the app is opened again after kept times have passed
- **THEN** it works out its reminders anew and the past times are not fired

#### Scenario: Limit on kept times
- **WHEN** the plan holds more than 1000 future times
- **THEN** the soonest 1000 are kept

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

### Requirement: Reminder kind
A reminder SHALL be either a Notification or an Alarm, and a new reminder SHALL be a Notification. The kind belongs to the reminder, so removing the reminder removes it, and unchecking an entry brings back its reminder with the same kind.

#### Scenario: Default
- **WHEN** the user saves a reminder without changing the kind
- **THEN** the reminder is a Notification

#### Scenario: Alarm saved
- **WHEN** the user chooses Alarm, picks 16:15 and saves
- **THEN** the entry has an alarm reminder starting at 16:15

#### Scenario: Changing the kind
- **WHEN** an entry has a notification reminder and the user chooses Alarm and saves
- **THEN** the entry has an alarm reminder with the same start and repeat unless they were changed

#### Scenario: Unchecking
- **WHEN** an entry with an alarm reminder is completed and then unchecked
- **THEN** its reminder is an alarm again

### Requirement: Choosing the kind
On Android, iOS and the desktop app the entry sheet SHALL offer the choice Notification or Alarm with the other reminder controls, with the current kind selected, and a short line under it saying that an alarm rings where the device supports it and is shown as a notification otherwise.

#### Scenario: On a phone
- **WHEN** the user opens the sheet of an entry on Android or iOS
- **THEN** the sheet offers Notification and Alarm

#### Scenario: Current kind
- **WHEN** the user opens the sheet of an entry that has an alarm reminder
- **THEN** Alarm is selected

#### Scenario: On the desktop
- **WHEN** the user opens the sheet of an entry on the desktop app
- **THEN** the sheet offers Notification and Alarm

### Requirement: The kind comes from the template
A task made from a template, by its schedule or with the + button, SHALL take the kind of the template's reminder, and the kind of the task's own reminder SHALL remain changeable in its sheet without changing the template.

#### Scenario: Scheduled alarm
- **WHEN** a template whose reminder is an alarm at 09:00 generates a task
- **THEN** the task has an alarm reminder at 09:00

#### Scenario: Added with the + button
- **WHEN** the user adds a template with an alarm reminder to Today with the + button
- **THEN** the entry has an alarm reminder

#### Scenario: Changed on the task
- **WHEN** the user changes the generated task's reminder to a Notification
- **THEN** only that task changes and the template keeps its alarm

### Requirement: Alarms are delivered as notifications until a platform rings them
Where a platform cannot ring an alarm (an iPhone earlier than iOS 26, or a device where alarms are not allowed), an alarm reminder SHALL fire at the same times as a notification and be shown as one, so choosing an alarm never loses a reminder.

#### Scenario: Alarm fires as a notification
- **WHEN** an alarm reminder is due on a platform that does not ring alarms, such as an iPhone earlier than iOS 26
- **THEN** a notification "Still to do" with the task's name appears at that time

#### Scenario: Older iPhone
- **WHEN** an alarm reminder is due on an iPhone earlier than iOS 26
- **THEN** a notification "Still to do" with the task's name appears at that time

#### Scenario: Alarms not allowed
- **WHEN** an alarm reminder is due on an iPhone where the user has not allowed alarms for OpenAdhd
- **THEN** a notification "Still to do" with the task's name appears at that time

### Requirement: An alarm rings on Android
On Android, at each time of an alarm reminder, the device SHALL ring with the default alarm ringtone on the alarm volume, repeating until it is stopped, and vibrate, and the screen SHALL turn on. It MUST ring with the app closed, with the device locked or on silent, and after the device restarts, and the next alarm time SHALL appear in the system's next-alarm display. A reminder of the Notification kind MUST be delivered as before.

#### Scenario: App closed
- **WHEN** an alarm reminder is due while the app is closed
- **THEN** the device rings and shows the alarm screen

#### Scenario: Locked and silent
- **WHEN** an alarm is due while the device is locked and its ringer is on silent
- **THEN** the alarm still rings at the alarm volume and the screen turns on

#### Scenario: After a restart
- **WHEN** the device is restarted with an alarm still to come
- **THEN** it still rings at its time

#### Scenario: Repeats ring again
- **WHEN** an alarm reminder repeats every 30 minutes and the first ring was dismissed
- **THEN** it rings again 30 minutes after its start, until the task is done or the stop time is reached

#### Scenario: Notification kind unchanged
- **WHEN** a notification reminder is due
- **THEN** it appears as a quiet notification and does not ring

#### Scenario: Completed task
- **WHEN** the task of an alarm reminder is completed before an alarm time
- **THEN** that alarm does not ring

### Requirement: The alarm screen
While an alarm rings, the app SHALL show an alarm screen above the lock screen with the task's name and two buttons, Open task and Dismiss. A notification with the same actions MUST be shown for as long as it rings. Tapping Open task, or the notification itself, SHALL stop the ring and open the app on Today with the task scrolled to and highlighted. Dismiss SHALL stop the ring and remove the notification, and MUST NOT cancel the alarm's later repeats. The notification MUST stay while it rings unless the user swipes it away where the system allows that, and swiping it away SHALL stop the ring like Dismiss.

#### Scenario: Open task
- **WHEN** the user taps Open task while an alarm rings
- **THEN** the ring stops and the app shows the task on Today, highlighted

#### Scenario: Tapping the notification
- **WHEN** the user taps the ringing notification
- **THEN** it behaves as Open task

#### Scenario: Dismiss
- **WHEN** the user taps Dismiss
- **THEN** the ring stops, the notification is removed and the alarm screen closes

#### Scenario: Dismiss keeps the repeats
- **WHEN** the user dismisses a ring of a reminder that repeats and the task is still pending
- **THEN** the next repeat still rings

#### Scenario: Swiped away
- **WHEN** the system lets the user swipe the ringing notification away and they do
- **THEN** the ring stops, the same as with Dismiss, and later repeats still ring

#### Scenario: Not swipeable
- **WHEN** the system does not let the user swipe the ringing notification away
- **THEN** it stays until the ring ends and the alarm is stopped with Dismiss, Open task, opening the app or the time limit

### Requirement: Opening the app ends a ringing alarm
On Android, whenever the app comes to the front while an alarm rings, the ring SHALL stop, with its vibration, and the ringing notification and the alarm screen MUST go away, as with Dismiss. Only the ring ends: the next repeat of the reminder, if the task is still pending, SHALL ring as usual. An alarm that comes due while the app is already in front MUST still ring, and is ended by the user or by the app next coming to the front.

#### Scenario: Opened from the launcher
- **WHEN** an alarm rings with a heads-up banner only and the user opens the app from the launcher
- **THEN** the sound and vibration stop and the ringing notification is removed

#### Scenario: Opened from the recent apps
- **WHEN** an alarm rings and the user returns to the app from the recent apps
- **THEN** the ring stops

#### Scenario: Repeats still ring
- **WHEN** the user opens the app during a ring of a reminder that repeats every 30 minutes and its task is still pending
- **THEN** the next repeat rings

#### Scenario: No ring in progress
- **WHEN** the user opens the app while no alarm rings
- **THEN** nothing changes

#### Scenario: Already in front
- **WHEN** an alarm comes due while the app is in front
- **THEN** it rings and stays ringing until the user ends it or the app comes to the front again

### Requirement: An alarm stops ringing by itself
A ring SHALL stop by itself after 10 minutes if the user has not acted, leaving a notification "Still to do" with the task's name in place of the ringing one. The next repeat, if any, SHALL ring again. An alarm that comes due while another rings MUST replace it, not ring over it.

#### Scenario: Ignored alarm
- **WHEN** an alarm rings for 10 minutes without being touched
- **THEN** the sound and vibration stop and a notification for the task remains

#### Scenario: Next repeat after a timeout
- **WHEN** an ignored alarm repeats every 30 minutes
- **THEN** it rings again 30 minutes after its start

#### Scenario: Overlap
- **WHEN** a second alarm comes due while the first is still ringing
- **THEN** only one ring is heard, for the second alarm's task

### Requirement: Alarms without the full-screen permission
When the device does not allow an app to show full-screen alerts (Android 14 and later, until the user allows it), an alarm SHALL still ring, with a heads-up notification in place of the alarm screen. Saving an alarm reminder for a task in that case SHALL still save it and tell the user that full-screen alarms are off, with a button that opens the setting. Saving a template whose reminder is an alarm SHALL show the same message after the form closes. Nothing is asked when the app starts.

#### Scenario: Ring without the permission
- **WHEN** an alarm comes due and full-screen alerts are not allowed
- **THEN** it rings and shows a notification with Open task and Dismiss, but no alarm screen over the lock screen

#### Scenario: Saving a task alarm
- **WHEN** the user saves an alarm for a task and full-screen alerts are not allowed
- **THEN** the alarm is saved and the sheet says full-screen alarms are off and offers to open the setting

#### Scenario: Opening the setting
- **WHEN** the user taps the button in that message
- **THEN** the system setting for full-screen alerts for OpenAdhd opens

#### Scenario: Saving a template alarm
- **WHEN** the user saves a template whose reminder is an alarm and full-screen alerts are not allowed
- **THEN** the template is saved and a message says full-screen alarms are off

#### Scenario: Allowed
- **WHEN** full-screen alerts are allowed or the device is older than Android 14
- **THEN** no message is shown on saving

### Requirement: How many alarms are armed
Only the soonest 20 alarm times SHALL be armed on the device at once; the rest are kept and armed as the armed ones fire, or the next time the app works out its reminders. The limit MUST count alarm times only, never notification times.

#### Scenario: Many alarm times
- **WHEN** alarm reminders on several tasks add up to more than 20 future times
- **THEN** the soonest 20 are armed

#### Scenario: Notifications not counted
- **WHEN** 20 alarm times and 60 notification times are due later today
- **THEN** all 20 alarms and all 60 notifications are armed

### Requirement: An alarm rings on iOS
On an iPhone with iOS 26 or later where alarms are allowed, at each time of an alarm reminder the system SHALL ring an alarm that sounds through Silent mode and Focus and appears on the lock screen, with the app closed, in the background or in front. A reminder of the Notification kind MUST be delivered as before.

#### Scenario: App closed and locked
- **WHEN** an alarm reminder is due while the app is closed and the phone is locked
- **THEN** the phone rings and shows the alarm on the lock screen

#### Scenario: Silent mode
- **WHEN** an alarm is due while the phone is on silent
- **THEN** the alarm still rings

#### Scenario: Repeats ring again
- **WHEN** an alarm reminder repeats every 30 minutes and the first ring was stopped
- **THEN** it rings again 30 minutes after its start, until the task is done or the stop time is reached

#### Scenario: Notification kind unchanged
- **WHEN** a notification reminder is due on the same phone
- **THEN** it appears as a notification and does not ring

#### Scenario: Completed task
- **WHEN** the task of an alarm reminder is completed before an alarm time
- **THEN** that alarm does not ring

### Requirement: The iOS alarm buttons
A ringing alarm on iOS SHALL show the task's name with the system's Stop button and a button Open task. Open task SHALL open the app on Today with the task scrolled to and highlighted. Stop SHALL end this ring and MUST NOT cancel the alarm's later repeats.

#### Scenario: Open task
- **WHEN** the user taps Open task on a ringing alarm
- **THEN** the app opens on Today with the task scrolled to and highlighted

#### Scenario: Open task from the lock screen
- **WHEN** the phone is locked and the user taps Open task
- **THEN** the phone asks to be unlocked and then opens the task

#### Scenario: Stop
- **WHEN** the user stops a ring of a reminder that repeats and the task is still pending
- **THEN** the ring ends and the next repeat still rings

### Requirement: iOS alarm permission
The app SHALL ask the system for permission to ring alarms the first time the user saves an alarm reminder for a task or a template, never when the app starts and never for a notification reminder. A refusal MUST NOT discard the reminder, and the user SHALL be told that alarms are turned off for OpenAdhd and where to turn them on; the reminder is then delivered as a notification.

#### Scenario: First alarm
- **WHEN** the user saves an alarm for the first time on an iPhone with iOS 26 or later
- **THEN** the system asks for permission to ring alarms

#### Scenario: Refused for a task
- **WHEN** the user refuses and the alarm was saved for a task
- **THEN** the alarm is saved and the sheet says alarms are turned off for OpenAdhd and where to turn them on

#### Scenario: Refused for a template
- **WHEN** the user refuses and the alarm was saved for a template
- **THEN** the template is saved and a message says alarms are turned off for OpenAdhd

#### Scenario: Notification reminders
- **WHEN** the user saves a notification reminder
- **THEN** no alarm permission is asked

#### Scenario: App start
- **WHEN** the app starts
- **THEN** no alarm permission is asked

### Requirement: How many alarms are scheduled on iOS
On iOS only the soonest 20 alarm times SHALL be scheduled at once, counted apart from notification times, and the rest are scheduled the next time the app works out its reminders.

#### Scenario: Many alarm times
- **WHEN** alarm reminders add up to more than 20 future times
- **THEN** the soonest 20 are scheduled as alarms

#### Scenario: Notifications not counted
- **WHEN** 20 alarm times and 60 notification times are due later today
- **THEN** all 20 alarms and all 60 notifications are scheduled
