# Proposal

## Why

A task on Today that is not done yet only helps if the user remembers it. Nothing in the app can nudge them: with ADHD the plan on the screen is easily forgotten once the app is closed. A reminder that keeps coming back until the task is finished closes that gap. The trash icon on each row is also a one-purpose control that crowds the place where entry options should live.

## What Changes

- **"..." button on not-done entries:** it replaces the trash icon on the right of a pending Today entry and opens a bottom sheet with that entry's options: its reminder, and the Delete task button that moved there. Delete is still offered only while nothing in the entry is done.
- **Reminder per entry:** one start time and an optional repeat. Start with a preset (in 30 minutes, in 1 hour, this evening at 18:00) or a picked time; repeat every 15, 30, 60 or 120 minutes, or not at all. It fires at the start time and then at each repeat, until the entry is done or the stop time is reached.
- **Stop time as a setting:** Settings gets a Reminders group with "Stop reminders at", default 22:00, with the same Restore control as the other settings.
- **Lifecycle:** completing the entry stops its reminders, unchecking brings back the ones still to come, deleting the entry removes them, and at midnight they end.
- **Notifications:** "Still to do" with the task's name; tapping opens the app. The permission is asked the first time a reminder is saved, and a refusal is explained in the sheet.
- **All three platforms:** Android and iOS deliver while the app is closed (Android re-arms them after a restart); the desktop app delivers only while it is running, as a system notification, falling back to an in-app message.
- **Backup:** reminders are part of the export and import.
- **Not in this change:** buttons on the notification (Done, Snooze), several reminders per entry, sounds, and reminders for past days.

## Capabilities

### New Capabilities
- `task-reminders`: the options sheet, setting and removing a reminder, valid times, repeat, lifecycle, notifications, permission, and delivery on each platform.

### Modified Capabilities
- `day-planner`: removing an entry moves from the row to the options sheet.
- `app-settings`: a Reminders group with the stop time, its default and its limits.
- `data-backup`: a backup also holds the reminders.

## Impact

- `shared` domain: a `Reminder` on `PlannedTask`; `ReminderRules` (validation and the list of upcoming times, pure); `ReminderScheduler` seam; a `ReminderCoordinator` that keeps the platform schedule equal to the store; `Settings.reminderStopHour`; `PlannerRepository.setReminder`.
- `shared` data: two nullable columns on `planned_tasks` and a Room migration from 7 to 8; both stores and the backup format (additive fields, format version stays 1).
- `shared` UI: the "..." button, the entry sheet with presets, a time picker and the repeat choices, the reminder line on a row, and the Reminders settings row.
- Platform code: Android (notification permission, alarms, a notification channel, a boot receiver, manifest entries), iOS (local notifications, authorization, foreground presentation), desktop (an in-process timer and tray notification).
- Tests: rules and validation, both stores, migration, backup round trip, the coordinator with a fake scheduler, and UI tests for the sheet. The three real notification paths are checked by hand.
- No new libraries expected.
