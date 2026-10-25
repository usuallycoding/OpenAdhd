# Design

## Context

Templates are `TaskTemplate` rows with no notion of time; Today entries are `PlannedTask` rows made by `PlannerRepository.assign`, which copies the template and freezes rewards. A `Reminder` is an absolute `Instant` plus a repeat on the entry, and `ReminderRules.upcoming` already drops times that have passed and stops at the stop hour. `ReminderCoordinator` runs from `App` over a `today` flow that follows the device date. Room is at version 8; backups are `BackupJson` over `StoreState` with additive optional fields. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Schedule rules that are pure and testable without a clock or a database.
- Generation that is idempotent per template and day, and survives restarts and deletes.
- Reuse `assign` and the existing reminder pipeline, so a generated entry is an ordinary entry.

**Non-Goals:**
- Generation while the app is closed, catch-up for missed days, "every N days", a recurring marker on entries, and copying the template reminder on a manual add.

## Decisions

- **`TaskSchedule` is a sealed type, not three loose fields.** `None`, `OnDate(date)`, `Weekly(days: Set<DayOfWeek>)`, `Period(start, end)`, `Daily`, with `isDue(date)`, `hasEnded(today)` and a validity check (Weekly non-empty, Period end after start). It matches the five modes in the form and keeps invalid combinations unrepresentable. Alternative: nullable start, end and weekday set, which also allows "weekdays within a window" for free; rejected because the form offers modes and the extra combinations would need UI and wording nobody asked for.
- **Storage: four nullable columns on `templates`.** `scheduleKind` (text), `scheduleStart` and `scheduleEnd` (ISO dates) and `scheduleDays` (a 7-bit mask), plus `reminderMinuteOfDay` and `reminderRepeatMinutes` and `lastGenerated` (ISO date). Migration 8 to 9 only adds columns; existing rows read as None, no reminder, never generated. Alternative: a separate schedules table; rejected, there is at most one per template.
- **`TemplateReminder(minuteOfDay, repeatMinutes?)` on the template.** A time of day, not an instant, because it has no date until an entry exists. Repeat uses `ReminderRules.RepeatChoices` and `DefaultRepeat`. Clearing the time clears the repeat.
- **Generation is a store operation, `generateDue(today, zone)`.** In one transaction it finds templates whose schedule `isDue(today)` and whose `lastGenerated` is not `today`, calls the same copy logic as `assign` for each, attaches the reminder, and sets `lastGenerated = today`. The reminder is built as `Reminder(at(today, time, zone), repeat)` using `ReminderRules.at`; no `check` is applied, because `upcoming` already ignores passed times and the stop hour. Because the marker is set whether or not the entry is later deleted, nothing regenerates. Alternative: derive "already generated" from existing entries with that `templateId`; rejected because deleting the entry would regenerate it, and manual adds would suppress it.
- **Who calls it.** A `ScheduledEntries` coroutine started from `App` next to the reminder coordinator: it runs for the current date, again whenever the `today` flow changes, and again whenever the templates change. The templates trigger covers "created or edited so that today is due" with no special case, and `lastGenerated` stops it repeating. It runs before the coordinator needs the entries, but the coordinator re-arms on any change, so ordering does not matter.
- **Editing keeps `lastGenerated`.** `update` writes the schedule and reminder but never resets the marker, so edit-after-delete does not regenerate. Import restores it from the backup.
- **Zone and date.** "Today" and the weekday come from the same device date flow the app already uses, in the device zone; a time zone change is treated as an ordinary date change.
- **Validation sits with the rules.** `ScheduleRules.check(schedule)` returns a problem for the form and `create`/`update` throw `IllegalArgumentException` for invalid input, like name and subtask checks. `ScheduleRules.summary(schedule, today)` produces the list line ("Mon, Wed, Fri", "Every day", "Ended").
- **Form.** A mode selector (None, Date, Weekly, Period, Daily) with the fields of the chosen mode: a date picker, seven weekday chips, or start and end pickers, and below it a reminder row reusing the clock dialog and repeat chips from the entry sheet. Switching mode keeps the other mode's values until save, so a slip does not lose input.
- **Backup.** `TemplateDto` gets optional `schedule` fields, `reminderMinuteOfDay`, `reminderRepeatMinutes` and `lastGenerated`; missing means None, so the format version stays 1.

## Risks / Trade-offs

- [Reminders on a day start only once the app is opened] → Stated in the proposal and the specs; pre-arming from schedules can follow as its own change.
- [A template reminder at or after the stop hour never fires] → Not blocked in the form, since the stop hour can change; documented in the spec.
- [A backup imported on another day carries an old `lastGenerated`] → Harmless: it is only compared with today.
- [Two generation triggers racing (start and templates change)] → The operation is one transaction and checks `lastGenerated` inside it, so the second finds nothing to do.
- [Schema change] → Added nullable columns only; defaults read as no schedule.

## Migration Plan

Room moves from 8 to 9 by adding the columns. Nothing changes for existing templates until the user schedules one. Rolling back means ignoring the columns.
