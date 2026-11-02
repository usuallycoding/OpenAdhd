# Proposal

## Why

Two small rough edges in the forms. The schedule options of a template are listed in an order that puts the rare ones (Date, Period) before the common one (Daily). And "Stop reminders at" can only be a whole hour typed into a text box, so a user who wants reminders to stop at 22:30, or who finds a clock easier than typing a number, cannot.

## What Changes

- The schedule options in the template form ("Add to Today schedule") are listed in the order None, Daily, Weekly, Date, Period. None stays the default.
- "Stop reminders at" becomes a time of day with minutes. Tapping its row opens a 24-hour clock picker, the same kind the reminder form uses, instead of the text sheet. The row shows the time, such as "22:00", and the default is shown as "22:00" instead of "22".
- 00:00 is refused: the stop time is from 00:01 to 23:59.
- Saved values and backups keep working: a stored plain hour such as "22" still means 22:00, and a stored "22:30" means 22:30. The app writes the "HH:mm" form from now on. There is no migration.
- The message a reminder shows when its start is after the stop time reads "Reminders stop at 22:30, so choose an earlier time" with the real minutes.

Out of scope: any other setting, the reminder start time picker, and the order in which templates or entries are listed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `task-templates`: the schedule options are listed in the new order.
- `app-settings`: the default and the limits of the stop time, and a new requirement for the clock picker and the two accepted stored forms.
- `task-reminders`: the valid reminder times account for a stop time with minutes.

## Impact

- `shared/src/domain/Settings.kt` and `SettingsValidation.kt`: the stop time as minutes of the day, a time kind of setting next to the number, choice and text kinds, parsing of both stored forms.
- `shared/src/domain/Reminders.kt`, `TemplateReminders.kt`, `ReminderScheduler.kt` and `shared/src/ui/today/TodayViewModel.kt`: the stop time passed around as minutes of the day instead of an hour; the message with minutes.
- `shared/src/ui/screens/SettingsScreen.kt`: the clock dialog and the row text. `TemplatesScreen.kt`: the order of the schedule options.
- `README.md`: the default is written 22:00.
- Tests: the settings, reminder and template tests that pass an hour change to minutes where they call the domain directly; tests that save plain hours through the setting keep passing as the backward-compatibility cases. No database or backup format change; a backup written by this version holds "HH:mm", which an older app ignores and replaces with its default.
