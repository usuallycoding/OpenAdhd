# Tasks

## 1. Schedule order

- [x] 1.1 Reorder `ScheduleMode` in `TemplatesScreen.kt` to None, Daily, Weekly, Date, Period; verify with a UI test in `TemplateScheduleUiTest` that the options appear in that order in the template form and that None is still selected for a new template, and that the existing schedule tests pass

## 2. The stop time as a setting

- [x] 2.1 In `Settings.kt`, replace `reminderStopHour` with `reminderStopMinute` (default 1320), add the time kind to `SettingKey` with parsing of a plain hour and of `H:mm` / `HH:mm`, refuse 00:00 and anything else, write `HH:mm` from `text`, and normalize `display`; verify with `SettingsTest` updated and extended: default text "22:00", "22" and "22:00" and "22:30" and "7:05" accepted, "00:00" and "0" and "24" and "25:00" and "22:60" and "late" refused with the messages from design.md, a stored "18" read as 18:00, a stored invalid value ignored, and `overrides` writing "22:30"
- [x] 2.2 Cover backups: verify with a test in `BackupJsonTest` (or the nearest settings backup test) that a backup holding `reminder_stop_hour` as "18" imports as 18:00, and that a backup written with a 22:30 stop time holds "22:30" and reads back the same

## 3. Reminders use the stop minute

- [x] 3.1 Rename `stopHour` to `stopMinute` in `Reminders.kt`, `TemplateReminders.kt`, `ReminderScheduler.kt` and `TodayViewModel.kt`, build the stop instant from hours and minutes, and make `ReminderProblem.AfterStop` read "Reminders stop at HH:mm, so choose an earlier time"; verify `./kotlin build` compiles on every target
- [x] 3.2 Update the tests that called the domain with an hour (`TemplateRemindersTest` `stopHour = 12` and the like) to minutes, keep the tests that save plain hours through the setting unchanged, and add tests for a stop time with minutes: a start before 22:30 is accepted and one at 22:30 is refused with the message "22:30", repeats stop before it, and a template reminder is planned up to it; verify `./kotlin test` passes for the reminder, coordinator and template-reminder tests

## 4. Settings screen

- [x] 4.1 In `SettingsScreen.kt`, open a 24-hour clock dialog (`TimePicker`, OK and Cancel) for a time setting instead of the text sheet, initialised to the current time, saving `HH:mm` through the existing save path and showing a refusal under the picker; verify by running the app on the desktop that the row reads 22:00, tapping it opens the clock, Cancel keeps the time and OK with 22:30 shows 22:30
- [x] 4.2 Add UI tests in `SettingsUiTest` that the row shows 22:00 by default, that tapping it opens the clock with a title "Stop reminders at", that Cancel closes it with the row unchanged, and that a stop time saved through the view model as "22:30" shows as 22:30 and 00:00 is refused (view model test in `SettingsViewModelTest`); verify `./kotlin test` passes

## 5. Docs and integration

- [x] 5.1 Update the README line on "Stop reminders at" to say the default is 22:00 and that it is chosen on a clock; verify by reading the changed line
- [x] 5.2 Run `./kotlin test` for the whole project and `./kotlin build -m desktopApp`; verify both succeed, and that no remaining reference to `reminderStopHour` or "stopHour" exists in `shared` (search the repo)
- [x] 5.3 On an Android phone and an iPhone, open Settings and "Stop reminders at", pick 22:30 on the clock, save, set a reminder for 22:15 and verify it is accepted and one for 22:30 is refused with the new message; verify the setting survives closing and reopening the app (report each result, or say plainly what could not be checked)
