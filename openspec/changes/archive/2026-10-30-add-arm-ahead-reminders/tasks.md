# Tasks

## 1. Planning from templates

- [x] 1.1 Add the pure planner for template reminders: for each of today and the next 6 days, each template that is due that day, has a reminder and has not generated that day's entry gives an `UpcomingReminder` with id `template:<templateId>:<date>`, the template's name, its kind, and the times from `ReminderRules.upcoming` (verify: unit tests for a daily template, a weekly template skipping non-due days, an `OnDate` and a `Period` template, a template without a reminder, a template already generated today, the stop hour cutting repeats, times in the past skipped, and an alarm kind being kept)
- [x] 1.2 Make `ReminderCoordinator` combine the templates with today's entries and the stop hour, so its list is today's entries' reminders plus the planned ones (verify: coordinator tests that a daily template's reminder is handed over before its entry exists, that once the entry is generated only the entry's reminder remains and nothing is doubled, that completing or deleting the generated entry removes it, that editing the template's time or schedule re-plans, and that an unchanged list is not handed over again; the existing coordinator tests still pass)

## 2. Platforms

- [x] 2.1 Check that the synthetic id works through Android and iOS request codes, URIs and `AlarmPlan.encode`/`decode` (verify: a round-trip test on an id such as `template:abc:2026-11-03` for `AlarmPlan` and `NotificationTarget`, and a test that arming picks the soonest times first when planned days exceed the caps, so today's reminders are never dropped for later days)

## 3. Docs and verification

- [x] 3.1 Add a sentence to the Scheduled templates section of `README.md` saying reminders and alarms of scheduled templates are armed for about a week ahead and fire even if the app is not opened, with the limit that a week of not opening runs them out (verify: it matches the behaviour in 3.3)
- [x] 3.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests; the iOS device build fails on provisioning signing only, unrelated to this change, and the shared module builds for every target)
- [ ] 3.3 On Android and on iOS: create a daily template with an alarm a few minutes ahead and one with a notification, open the app once so they are armed, force the day by setting the time to a minute before, close the app and wait (verify: manual; both fire without the app being opened, tapping one opens Today, and opening the app afterwards does not fire them twice)
