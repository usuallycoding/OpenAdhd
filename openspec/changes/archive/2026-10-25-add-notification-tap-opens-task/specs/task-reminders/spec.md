# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
