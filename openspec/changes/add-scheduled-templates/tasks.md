# Tasks

## 1. Schedule rules

- [x] 1.1 Add `TaskSchedule` (None, OnDate, Weekly, Period, Daily) with `isDue`, `hasEnded` and validity, and `ScheduleRules` with `check` and `summary` (verify: unit tests for each mode's due days, a Period's both ends, a Weekly with no day, a Period ending on or before its start, an ended date and period, and the list lines "Mon, Wed, Fri", "Every day", "Ended")
- [x] 1.2 Add `TemplateReminder` and the schedule, reminder and `lastGenerated` fields to `TaskTemplate`, with `create` and `update` rejecting invalid schedules (verify: contract tests for creating and updating with each mode, rejection of invalid ones, and clearing a reminder time clearing its repeat)

## 2. Storage and backup

- [x] 2.1 Add the template columns, `Migration8To9` and database version 9 to Room, and carry the fields through `RoomStore` and `InMemoryStore` (verify: contract tests run on both stores, and a migration test from a version 8 database keeps templates with schedule None)
- [x] 2.2 Carry schedules, template reminders and `lastGenerated` in `BackupJson` as optional fields (verify: round-trip tests for each mode with a reminder, and an old file without the fields imports as None with no reminder)

## 3. Generation

- [x] 3.1 Add `generateDue(today, zone)` to the store: due and not yet generated today templates get an entry copied as `assign` does, with the template's reminder as an instant on today, and `lastGenerated` set, in one transaction (verify: tests for due and not due days, once per day on repeated calls, delete then call again, a manual add not affecting it, an edit keeping `lastGenerated`, a past reminder time and the resulting `upcoming` times)
- [x] 3.2 Add `ScheduledEntries`, run it from `App` for the current date, on every date change and on every template change (verify: tests with a fake clock for start-up, midnight while running, a new daily template appearing at once, a template edited to include today, and no catch-up for missed days)

## 4. Screens

- [x] 4.1 Add the schedule mode picker with date, weekday and period fields, and the reminder time and repeat controls, to the template form and its view model, showing validation messages (verify: view model tests for saving each mode, the two validation errors, and repeat hidden without a time; UI tests for switching modes and weekday selection)
- [x] 4.2 Show the schedule line on template list items (verify: a UI test for Weekly, Daily, a period, an ended one and None)

## 5. Docs

- [x] 5.1 Add a Scheduled templates section to `README.md` covering the modes, the template reminder, and that entries are added when the app is opened (verify: the steps work as written in 6.2)

## 6. Verification

- [ ] 6.1 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [ ] 6.2 On macOS: schedule a template for today with a reminder a few minutes ahead, see the entry appear and the reminder fire, delete the entry and restart to confirm it stays gone, and edit the reminder on the entry (verify: manual, as the specs say)
