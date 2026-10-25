# Design

## Context

`generateDue` builds a scheduled entry with `generatedTask(date, zone, rules)`: the copy `toPlannedTask` makes, plus the template's reminder as an instant on that date. The + button goes through `TodayViewModel.assign` to `PlannerRepository.assign(template, date)`, which uses `toPlannedTask` only, so the reminder is left out. Notification permission is requested by `ReminderScheduler.ensurePermission()`, which only `TodayViewModel.saveReminder` calls; `TemplatesViewModel` has no scheduler. The app already shows one-line messages through a snackbar host in `App`. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One builder for the template's reminder, used by generation and by the + button.
- Permission asked at the moment the user commits to a template reminder.

**Non-Goals:**
- Asking for permission when a schedule generates an entry, a per-add reminder choice, and any schema or backup change.

## Decisions

- **`assign` attaches the reminder.** `PlannerRepository.assign(template, date, zone = currentSystemDefault)` returns the entry with `generatedTask`'s reminder, in the same write that inserts it, so the coordinator never sees an entry without it and there is no second store call. `generateDue` keeps its own `zone` argument. The two paths share the builder, so they cannot drift. Alternative: `TodayViewModel.assign` calling `assign` then `setReminder`; rejected, it is two writes and re-arms the schedule twice.
- **Dates other than today.** `assign` builds the reminder on the date it is given. Only Today calls it, and `ReminderRules.upcoming` already ignores anything past, so no extra rule is needed.
- **Permission in `TemplatesViewModel`.** It takes an optional `ReminderScheduler?` (null where reminders cannot be delivered, as in `TodayViewModel`) and, after a save with a reminder time, calls `ensurePermission()`. A false result emits one message on a `notices` flow. The save itself never depends on the answer.
- **Messaging.** `App` collects `TemplatesViewModel.notices` into the existing snackbar host, so the form closes as it does today and the message appears over the list: "Notifications are turned off for OpenAdhd. Turn them on in your device's settings to get reminders." Alternative: keeping the sheet open with an inline note; rejected, it forces a second tap to leave a saved form.
- **Only a reminder time triggers it.** Saving with no reminder, or saving an unchanged template whose permission is already granted, asks nothing: `ensurePermission()` returns true at once when granted, so it is called for every save with a reminder time.
- **Tests.** `FakeReminderScheduler` already counts permission requests and can refuse them, so the view model tests need no new fake.

## Risks / Trade-offs

- [A hand-added task whose reminder time has passed and has no repeat shows a reminder row that never fires] → Same as a generated entry today; the row is informational and editable from the sheet.
- [Permission asked on every save with a reminder while denied] → The platforms only prompt while the answer is undecided; a denied answer returns false quickly and the message is shown each time, which is the intended nudge.
- [`assign` gaining a parameter] → It has a default, so existing callers and tests compile unchanged.
