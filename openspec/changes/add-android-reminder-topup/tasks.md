# Tasks

## 1. Shared planning

- [x] 1.1 Add `ReminderScheduler.planDays` (default `TemplateReminders.Days`), take the number of days as a parameter of `TemplateReminders.plan` and have `ReminderCoordinator` pass the scheduler's value (verify: planner tests with 7 and 30 days; the coordinator test with a fake scheduler of 30 days hands over 30 days of a daily template, and the existing tests still pass)
- [x] 1.2 Add to `AlarmPlan` a cap of 1000 kept times (the soonest) and a pure function that drops times at or before `now` and returns what to arm from the rest by the existing limits (verify: unit tests for the cap, past times dropped, 20 alarms and 100 notifications still the limits, and an empty list)

## 2. Android

- [x] 2.1 Make `ReminderAlarms` store the whole plan and the armed subset separately, cancel the stored armed set on replace, and add a top-up that drops past times, arms what the pure function returns and saves the armed set; read an older stored schedule as the plan (verify: on a device, a repeating reminder with more than 100 future times keeps firing past the 100th after the app is closed; `adb shell dumpsys alarm` shows only the soonest armed)
- [x] 2.2 Call the top-up from `ReminderReceiver`, from `AlarmReceiver` after the ring or fallback has started, and from the boot receiver before it arms, and set `AndroidReminderScheduler.planDays` to 30 (verify: after a restart with kept times that were not armed, the soonest are armed; a daily template reminder fires on a later day without the app opened)

## 3. Docs and verification

- [x] 3.1 Update the Reminders and Scheduled templates sections of `README.md`: Android keeps up to about 30 days and 1000 times and tops up as reminders fire, iOS and the desktop as before (verify: matches the behaviour in 3.3)
- [x] 3.2 Run `./kotlin test` and `./kotlin build -m androidApp` (verify: both succeed; the iOS device build is excluded because of the unrelated signing failure)
- [x] 3.3 On an Android phone: create a daily template with a notification repeating every 15 minutes, open the app once, close it and leave it for more than the first armed batch (verify: manual; reminders keep arriving after the first 100, and tapping one opens Today)
