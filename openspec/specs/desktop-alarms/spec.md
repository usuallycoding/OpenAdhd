# desktop-alarms Specification

## Purpose
Makes an alarm reminder ring on the desktop with a looping sound and an alarm card that sits above other windows, so it is not lost to Focus or Do Not Disturb the way a notification can be.

## Requirements

### Requirement: An alarm rings on the desktop
While the desktop app is running, with its window shown or hidden, an alarm reminder that comes due SHALL ring: a sound that repeats until the ring ends, and the alarm card shown above other windows. On macOS the Dock icon SHALL also bounce until the app is noticed. Nothing rings while the app is not running.

#### Scenario: Window hidden
- **WHEN** an alarm comes due while the main window is hidden in the tray
- **THEN** the sound starts and the alarm card appears above other windows

#### Scenario: Window in front
- **WHEN** an alarm comes due while the main window is in front
- **THEN** the sound starts and the alarm card appears above it

#### Scenario: Dock on macOS
- **WHEN** an alarm rings on macOS and nobody has acted on it
- **THEN** the Dock icon keeps bouncing until the app is brought to the front or the ring ends

#### Scenario: App quit
- **WHEN** an alarm is due after the user chose Quit in the tray menu
- **THEN** nothing rings

### Requirement: The alarm card
The alarm card SHALL list one row for each ringing task, showing the task's name with the buttons Open task and Dismiss. The card MUST be shown for as long as any row is ringing and MUST go away when the last row ends.

#### Scenario: One alarm
- **WHEN** an alarm rings for "Take medication"
- **THEN** the card shows one row, "Take medication", with Open task and Dismiss

#### Scenario: Last row ends
- **WHEN** the only ringing row is dismissed
- **THEN** the sound stops and the card goes away

### Requirement: Alarms stack on one card
An alarm for another task that comes due while the card is showing SHALL add a row to it, and the sound SHALL continue as one sound, not one per alarm. A repeat of a task that is already ringing MUST replace that task's row, not add a second one, and its 10 minutes start again.

#### Scenario: A second task
- **WHEN** the alarm for "Taxes" comes due while "Take medication" rings
- **THEN** the card shows both rows and one sound is heard

#### Scenario: A repeat while still ringing
- **WHEN** the repeat of an alarm comes due while its earlier ring has not been dismissed
- **THEN** the card still shows one row for that task

#### Scenario: Other rows stay
- **WHEN** one of two rows is dismissed
- **THEN** the other row stays and the sound continues

### Requirement: Dismissing an alarm
Dismiss SHALL end the ring of its row only, and MUST NOT cancel the task's later repeats. Closing the card window SHALL dismiss every row on it.

#### Scenario: Dismiss keeps the repeats
- **WHEN** the user dismisses a ring of a reminder that repeats and the task is still pending
- **THEN** the next repeat rings

#### Scenario: Close the card
- **WHEN** two rows are ringing and the user closes the card window
- **THEN** both rows end and the sound stops

### Requirement: Open task from an alarm
Open task SHALL end the ring of its row and show the main window on the Today tab with that task scrolled to and highlighted, as when a reminder notification is tapped on Android and iOS, and nothing MUST be added, changed or spent by it. When the task is no longer on Today's list the main window is shown with nothing highlighted.

#### Scenario: Window hidden
- **WHEN** the main window is hidden in the tray and the user chooses Open task
- **THEN** the window is shown on Today with the task highlighted and that row ends

#### Scenario: Task gone
- **WHEN** the task of the row was deleted and the user chooses Open task
- **THEN** the window is shown on Today with nothing highlighted

### Requirement: Showing the window does not end a ring
Showing the main window from the Dock, the tray icon or the tray menu MUST NOT end a ring or remove a row, because the card has its own buttons.

#### Scenario: Open from the tray
- **WHEN** an alarm rings and the user chooses Open in the tray menu
- **THEN** the main window is shown and the alarm keeps ringing

### Requirement: An alarm stops ringing by itself on the desktop
Each row SHALL end by itself 10 minutes after it started ringing. The sound stops when the last row has ended, and the next repeat of the reminder, if any, SHALL ring again.

#### Scenario: Ignored alarm
- **WHEN** a row has rung for 10 minutes without being touched
- **THEN** it ends, and the sound stops if it was the last row

#### Scenario: Rows time out separately
- **WHEN** a second row starts 4 minutes after the first
- **THEN** the first ends at 10 minutes and the second at 14 minutes

#### Scenario: Next repeat
- **WHEN** an ignored alarm repeats every 30 minutes
- **THEN** it rings again 30 minutes after its start

### Requirement: A finished task ends its ring
A ringing row SHALL end when its task is completed or deleted, in the main window or anywhere else. A row of a scheduled template whose task has not been generated yet MUST NOT end this way.

#### Scenario: Completed while ringing
- **WHEN** an alarm rings for a task and the user completes it in the main window
- **THEN** its row ends and the sound stops if it was the last row

#### Scenario: Deleted while ringing
- **WHEN** an alarm rings for a task and the task is deleted
- **THEN** its row ends

#### Scenario: Other rows unaffected
- **WHEN** two rows ring and one task is completed
- **THEN** only its row ends

### Requirement: A missed alarm rings on waking
When the app was running and the computer slept through the time of an alarm, the alarm SHALL ring once on waking, with one row for each task that missed alarms and no more, showing the task's most recent missed time as "Due 07:30" in the form of the system clock. Repeats missed in between MUST NOT ring separately. The stop time from Settings and the end of the day still apply, and an alarm whose time passed before the app was started MUST NOT ring.

#### Scenario: Slept through an alarm
- **WHEN** an alarm is set for 07:00 and the computer wakes at 07:20 with the app still running
- **THEN** the alarm rings on waking and its row says it was due at 07:00

#### Scenario: Repeats while asleep
- **WHEN** an alarm starts at 07:00 and repeats every 30 minutes, and the computer wakes at 07:50
- **THEN** one row rings for the task, saying it was due at 07:30

#### Scenario: Two tasks missed
- **WHEN** alarms for two different tasks were missed during the sleep
- **THEN** the card shows a row for each

#### Scenario: Slightly late
- **WHEN** an alarm is due at 15:00 and the computer wakes at 15:01
- **THEN** the alarm rings as usual and its row does not say "Due"

#### Scenario: Started after the time
- **WHEN** the app is started at 07:20 for an alarm that was set for 07:00
- **THEN** nothing rings for it

#### Scenario: After the stop time
- **WHEN** an alarm was due before the stop time from Settings and the computer wakes after it
- **THEN** the alarm still rings once, for the missed time before the stop time

### Requirement: The notification that goes with a ring
Every ring SHALL also show the notification "Still to do" with the task's name, so that a ring that was missed leaves it in the notification centre. Where the system has no tray, the in-app message is shown instead.

#### Scenario: Ring with notification
- **WHEN** an alarm for "Take medication" rings
- **THEN** the notification "Still to do" with that name is shown with the card

#### Scenario: Without a tray
- **WHEN** an alarm rings on a system with no tray
- **THEN** the card is shown and the in-app message shows the task's name

### Requirement: A ring survives a missing sound device
When the sound cannot be played, because the system has no audio output or refuses it, the alarm card and the notification MUST still be shown and the ring MUST still end as usual.

#### Scenario: No audio output
- **WHEN** an alarm rings on a computer where the sound cannot start
- **THEN** the card and the notification are shown with no sound
