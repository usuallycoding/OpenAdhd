# Proposal

## Why

Reminders of scheduled templates are now armed ahead of their entries, but the platform limits on armed times (20 alarms, about 100 notifications) cut that to the next 2-4 days for a reminder that repeats often, and Android throws the rest away: it stores only what it arms. If the app is not opened for several days the reminders stop although the plan would have covered them. On Android nothing needs the app to be open to carry on, because every firing is a chance to arm the next times.

## What Changes

- On Android the whole plan is stored (the soonest 1000 times), not just the armed part, and the soonest times within the limits are armed from it.
- Each time a reminder notification or an alarm fires, and after a restart, the next stored times are armed and the past ones dropped, without opening the app or the database.
- Android plans 30 days (today and 29 more) instead of 7; iOS and the desktop keep 7.
- Not changed: iOS (no way to run code when a notification fires), the desktop, how entries are generated, and the alarm ring itself.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `task-reminders`: adds the top-up requirement for Android, raises the planning horizon on Android in "Scheduled template reminders are armed ahead", and in "How many alarms are armed" says the rest is armed as the armed ones fire.

## Impact

- `shared/src/domain/ReminderScheduler.kt` (a `planDays` property, 7 by default, passed to `TemplateReminders.plan`), `TemplateReminders.kt` and `AlarmPlan.kt` (the pure selection of what to arm and the cap on kept times).
- `androidApp/src/ReminderAlarms.kt`, `ReminderReceiver.kt`, `AlarmReceiver.kt` and `AndroidReminderScheduler.kt`; no manifest, database or permission change.
- Tests in `AlarmPlanTest` and the coordinator tests; the Android side is checked on a device.
- Out of scope: a background job or headless database access, iOS changes, and anything for the desktop app.
