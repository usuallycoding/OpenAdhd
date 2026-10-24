# Tasks

## 1. De-risk the desktop notification

- [x] 1.1 Try `SystemTray`/`TrayIcon.displayMessage` from the packaged `dist/OpenAdhd.app` with a throwaway test, note whether a notification appears on macOS, and record the result in design.md (verify: the finding is written down, and the desktop decision, tray notification or in-app message, is confirmed)

## 2. Rules and settings

- [x] 2.1 Add `Reminder` to `PlannedTask`, and `ReminderRules` with `check` and `upcoming` (verify: unit tests for presets, a passed time, a time after the stop hour, 21:55 with a repeat, a repeat every 30 minutes from 15:00 to 21:30, no repeat, and nothing after the stop hour)
- [x] 2.2 Add `Settings.reminderStopHour`, `SettingKey.ReminderStopHour`, the `Reminders` group, its default 22 and range 1 to 23 (verify: `SettingsTest` and `SettingsViewModelTest` cover the default, the limits and Restore, and the Settings screen lists it)

## 3. Storage and backup

- [x] 3.1 Add the two columns, `Migration7To8` and database version 8, and `setReminder` on `PlannerRepository` for `InMemoryStore` and `RoomStore` (verify: contract tests for setting, replacing, removing, refusing a completed or unknown entry, and keeping the reminder when an entry is completed and unchecked, and a migration test from a version 7 database)
- [x] 3.2 Carry reminders in `BackupJson` as optional fields and through export and import (verify: round-trip tests with and without a reminder, and an old file without the fields still imports)

## 4. Scheduling

- [x] 4.1 Add `ReminderScheduler`, `UpcomingReminder` and `ReminderCoordinator`, start it from `App`, and add a fake scheduler for tests (verify: coordinator tests with a fake clock and scheduler for set, repeat, complete, uncheck, delete, remove, a changed stop hour, a day change, import and start-up with times already passed)

## 5. Screens

- [x] 5.1 Replace the trash button with the "..." button and add the entry sheet: presets, Pick a time with a time picker, repeat chips, Save with errors, Remove reminder, Delete task, the permission line, and the reminder line on the row (verify: UI tests for the button on pending and in-progress entries and not on done, history or Reorder mode, saving each preset, a refused time, removing a reminder, deleting from the sheet, and no delete for an entry in progress)
- [x] 5.2 Wire the view model to `setReminder`, `ReminderRules.check` and `ensurePermission` (verify: view model tests for saving, refusing, a refused permission still saving, and delete from the sheet)

## 6. Platforms

- [x] 6.1 Android: manifest permission and receivers, a `Reminders` channel, `AndroidReminderScheduler` with alarms, the stored schedule, the boot receiver and the permission request, wired into `createAndroidContainer` (verify: builds; checked by hand in 8.3)
- [x] 6.2 iOS: `IosReminderScheduler` with authorization, the delegate for foreground display and the 60-notification limit, wired into the iOS container (verify: the iOS native build compiles; checked by hand in 8.4)
- [x] 6.3 Desktop: `DesktopReminderScheduler` with the timer, the tray notification and the in-app fallback, wired into `createDesktopContainer` (verify: builds; checked by hand in 8.2)

## 7. Docs

- [x] 7.1 Add a Reminders section to `README.md` saying how to set one, where it can fire, and that desktop reminders need the app open (verify: the steps work as written in 8.2 to 8.4)

## 8. Verification

- [x] 8.1 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 8.2 On macOS: set a reminder a few minutes ahead, see it fire, complete the task before the next one and see it stop, uncheck and see it resume, and change the stop time in Settings (verify: manual, as the task-reminders spec says)
- [x] 8.3 On an Android phone: the same with the app closed, after a restart of the phone, with notifications refused once, and tapping a notification (verify: manual)
- [x] 8.4 On iOS: the same with the app closed and with the permission prompt (verify: manual)
