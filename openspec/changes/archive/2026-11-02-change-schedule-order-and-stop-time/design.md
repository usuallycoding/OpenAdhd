# Design

## Context

See proposal.md for why and the specs for behavior. Observed state:

- The schedule options come from the declaration order of the private `ScheduleMode` enum in `TemplatesScreen.kt` (`None, Date, Weekly, Period, Daily`); nothing is stored by position.
- "Stop reminders at" is `SettingKey.ReminderStopHour`: storage key `reminder_stop_hour`, a number with the range 1 to 23, held in `Settings.reminderStopHour: Int` (default 22). Settings are stored and backed up as text by key, and `Settings.fromOverrides` ignores a stored value that fails `problemWith`.
- The stop moment is `ReminderRules.stopInstant` = `LocalTime(stopHour, 0)`. The hour is passed as `stopHour: Int` through `Reminders.kt` (`check`, `presets`, `upcoming`, `ReminderProblem.AfterStop`), `TemplateReminders.plan`, `ReminderScheduler` and `TodayViewModel`.
- Every setting is edited in one text sheet (`SettingDialog`) except Theme and Dynamic color, which have their own sheets. The template form already has a 24-hour Material `TimePicker` in an `AlertDialog` for the reminder time, so it works on every platform the app runs on.
- Some tests save plain hours (`saveSetting(SettingKey.ReminderStopHour, "23")`) and others call the domain with `stopHour = 12`.

## Goals / Non-Goals

**Goals:**
- Schedule options in the new order.
- A stop time with minutes, chosen on a clock, that reads old saved hours and old backups unchanged.

**Non-Goals:**
- A migration or a new storage key; a different reminder start picker; changing how other settings are edited.

## Decisions

**Keep the key and accept two stored forms.** The key stays `reminder_stop_hour`. A stored text is read as either a plain hour (`22`, one or two digits, 1 to 23, meaning that hour and 00 minutes) or `H:mm` / `HH:mm` (hour 0 to 23, minutes 00 to 59), with 00:00 refused. The app writes `HH:mm`. Existing rows and backups need no migration, and `fromOverrides` already falls back to the default for anything that does not parse. *Alternative:* a new key holding minutes of the day; rejected because it needs a migration and a second key to read from old backups. A backup written by this version holds `HH:mm`, which an older app does not understand and replaces with its default; accepted, since backups are restored to newer or equal versions.

**Minutes of the day inside the app.** `Settings.reminderStopMinute: Int` (1 to 1439, default 1320) replaces `reminderStopHour`, and every `stopHour: Int` parameter becomes `stopMinute: Int`. `stopInstant` builds `LocalTime(minute / 60, minute % 60)`. One `Int` keeps all call sites as they are, only renamed. The enum entry `SettingKey.ReminderStopHour` keeps its name so the storage key and the existing tests that save it do not churn; its comment says it is a time.

**A time kind of setting.** `SettingKey` gets a time kind beside number, choice and text: `problemWith` parses both forms, and refuses with "Enter a time, such as 22:30" when it is not a time and "Stop reminders at must be between 00:01 and 23:59" when it is 00:00. `Settings.text` writes `HH:mm`, so the default text is "22:00" and the row, which compares its text with the default's, shows 22:00 and stays "default" after a legacy 22 is read. `display` normalizes any accepted text to `HH:mm`.

**A clock dialog, not a sheet.** For a time setting, the Settings screen opens an `AlertDialog` with `TimePicker(is24Hour = true)` initialised to the current stop time, with OK and Cancel, and shows a refused value (00:00) as text under the picker. OK saves `HH:mm` through the same `onSave` path as every other setting, so applying at once, Restore and Delete all data need no new code. The same pattern is already in the template form.

**The message uses the real time.** `ReminderProblem.AfterStop` formats the stop minute as `HH:mm` with the same clock formatting the template form uses.

**Schedule order.** Reordering the enum is the whole change: `None, Daily, Weekly, Date, Period`. `ScheduleMode.of` maps by type, so nothing else moves.

## Risks / Trade-offs

- [Renaming `stopHour` touches about ten call sites and several tests] -> The rename is mechanical and the compiler finds every one; tests that saved a plain hour through the setting keep passing unchanged as the backward-compatibility cases, and tests that called the domain with an hour now pass minutes.
- [A stop time earlier than a reminder's start on the same day, such as 00:30] -> Same rules as before: a reminder whose start is at or after the stop is refused, and the stop is on the same date; no change in behavior.
- [Picker interaction is hard to drive in a UI test] -> The dialog opening, Cancel and the shown time are UI-tested; the saved values and the refusal of 00:00 are tested at the view model, and the picker is checked by hand on a phone.
- [A newer backup read by an older app] -> The older app ignores the value and uses 22; documented, not handled.
