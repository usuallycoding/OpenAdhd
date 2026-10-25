# Proposal

## Why

Routine tasks (laundry on Mondays, a medication check every day, a deadline on one date) have to be added to Today by hand each time, which is exactly the kind of remembering the app should take off the user. A template that knows when it belongs on Today, and when to nudge about it, makes the day's plan appear by itself.

## What Changes

- **Schedule on a template:** a template gets one of five modes: None (manual only, as today), Date (one day), Weekly (a fixed set of weekdays), Period (every day from a start date to a later end date), or Daily (every day, open-ended).
- **Reminder on a template:** an optional time of day and, once a time is set, a repeat of Off or every 15, 30, 60 or 120 minutes.
- **Automatic entries:** on a day a template is due, an entry is added to Today once, when the app starts or when the date changes while it is running. Only today is generated; days the app was not opened are not filled in.
- **Reminder on the entry:** the generated entry carries a reminder built from the template's time and repeat. Times already past are skipped, so repeats resume at the next one. The entry's own reminder stays editable in its "..." sheet.
- **No resurrection:** a template is generated at most once per day; deleting the generated entry does not bring it back. A template created or edited so that today is due generates its entry once, immediately.
- **List and form:** the template form gets a schedule mode picker and a reminder time and repeat; the list item shows a short line such as "Mon, Wed, Fri".
- **Backup:** schedules, template reminders and the last-generated date are part of the export and import.
- **Not in this change:** generation while the app is closed (reminders on a day start once the app is opened), filling in missed days, "every N days", a recurring marker on generated entries, and applying the template reminder when a task is added by hand.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `task-templates`: schedule modes and their validation, template reminder, and the schedule line in the list.
- `day-planner`: entries generated from scheduled templates.
- `task-reminders`: a generated entry starts with its template's reminder.
- `data-backup`: a backup also holds schedules, template reminders and last-generated dates.

## Impact

- `shared` domain: `TaskSchedule` (with a pure `isDue(date)`), `TemplateReminder` on `TaskTemplate`, a last-generated date, `ScheduleRules` for validation and the list line, and a `ScheduledEntries` generator.
- `shared` data: template columns and a Room migration from 8 to 9; both stores; the backup format (additive fields, version stays 1).
- `shared` UI: schedule picker, weekday chips, date pickers and reminder controls in the template form; the schedule line in the list; the generator started from `App` beside the reminder coordinator.
- Tests: schedule rules, generation (due days, once per day, delete then restart, edit mid-day, past reminder times), both stores, migration, backup round trip, view model and form UI.
- No platform-specific code and no new libraries.
