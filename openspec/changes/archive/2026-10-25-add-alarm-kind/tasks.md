# Tasks

## 1. Kind in the model and storage

- [x] 1.1 Add `ReminderKind` and the `kind` field (default Notification) to `Reminder`, `TemplateReminder` and `UpcomingReminder`; copy it in the template-to-task builder; show "Alarm" in `ReminderRules.summary`; pass it through `ReminderCoordinator` (verify: unit tests for the summary of both kinds, the builder copying the kind for generation and `assign`, and the coordinator re-handing the schedule when only the kind changes)
- [x] 1.2 Add the `reminderKind` columns, `Migration9To10` and database version 10, carry the kind through `RoomStore` and `InMemoryStore` including `setReminder`, and make an unknown stored value read as Notification (verify: store contract tests on both stores for saving, replacing, removing and uncheck keeping the kind, and a migration test from a version 9 database leaving every reminder a Notification)
- [x] 1.3 Carry the kind in `BackupJson` as an optional field on tasks and templates, refusing an unknown value (verify: round-trip tests for both kinds, an old file without the field importing as Notification, and a refusal for an unknown kind that replaces nothing)

## 2. Choosing the kind

- [x] 2.1 Add `supportsAlarms` to `ReminderScheduler` (default false), return true from the Android and iOS schedulers and the test fake, and expose it as `canAlarm` on `TodayViewModel` and `TemplatesViewModel` (verify: view model tests that it follows the scheduler, is false with no scheduler and false for the desktop scheduler)
- [x] 2.2 Add the Notification / Alarm chips and the explanatory line to the entry sheet, pass the kind through `ReminderActions.save` and `TodayViewModel.saveReminder`, and show it on the row (verify: UI and view model tests for saving an alarm, changing and keeping the kind, the current kind selected on opening, no choice without alarm support, and the "Alarm 15:00, every 30 min" row)
- [x] 2.3 Add the chips and line to the template form, shown only with a reminder time and alarm support, and save the kind with the template (verify: view model and UI tests for choosing Alarm, the default, no choice without a time, Alarm selected when editing, and a generated task and a task added with the + button taking the kind)

## 3. Docs and verification

- [x] 3.1 Add a note on alarms to the Reminders section of `README.md` saying the kind can be chosen on phones, and that an alarm is shown as a notification until the ringing changes arrive (verify: the steps work as written in 3.3)
- [x] 3.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 3.3 On a phone: set an alarm on a task and on a template, see the row say Alarm, restart the app and see it kept, export and import a backup and see it kept, and see the reminder arrive as a notification (verify: manual)
