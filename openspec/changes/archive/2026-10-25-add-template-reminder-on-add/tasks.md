# Tasks

## 1. Reminder on a hand-added task

- [x] 1.1 Give `assign` a `zone` parameter (default the system zone) and have both stores attach the template's reminder through the builder `generateDue` uses, then correct the "no reminder when added by hand" test and README line (verify: contract tests on both stores for a template with a reminder, one without, the same template added twice, a past time with and without repeat via `ReminderRules.upcoming`, and a UI or view model test that the + button path gives the entry its reminder)

## 2. Permission when saving a template reminder

- [x] 2.1 Pass the optional `ReminderScheduler` to `TemplatesViewModel`, call `ensurePermission()` after saving a template with a reminder time, and emit a notice on refusal; collect it in `App` into the snackbar host and wire the container's scheduler in (verify: view model tests with `FakeReminderScheduler` for asked once when undecided, not asked without a reminder time, not asked when no scheduler, refusal still saving and emitting the notice, and granted emitting nothing)
- [x] 2.2 Show the notice after the form closes (verify: a UI test saving a template with a reminder time with a refusing fake scheduler and seeing the message while the template is listed)

## 3. Verification

- [x] 3.1 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 3.2 On a phone: save a template with a reminder, see the permission prompt once, add the template with the + button and see its reminder on the row and fire (verify: manual, as the task-reminders spec says)
