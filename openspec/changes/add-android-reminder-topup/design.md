# Design

## Context

`ReminderAlarms` (Android) keeps the schedule it armed in `SharedPreferences` (`openadhd_reminders`, key `schedule`) so the receivers can act after a restart without the database. `replace` cancels what was armed, runs `AlarmPlan.arm` (soonest 20 alarms and 100 notifications), saves and arms that, and `rearm` arms the stored list again at boot. `ReminderReceiver` and `AlarmReceiver` show or ring the reminder and do nothing else. `ReminderCoordinator` plans `TemplateReminders.Days` (7) days and hands the whole list over whenever the store changes. Everything else in the app, including the database, only exists while the activity does.

## Goals / Non-Goals

**Goals:**
- Reminders keep firing for weeks without the app opened, on Android.
- No headless database access and no background job.

**Non-Goals:**
- iOS, the desktop, unlimited horizon, or handling a user force-stopping the app or a phone that kills alarms.

## Decisions

- **Store the whole plan, arm a window of it.** `replace` saves every future time (soonest 1000) under one key and the armed subset under another. Each firing calls a top-up that drops past times, picks what `AlarmPlan.arm` selects from the rest and arms it. The same `PendingIntent` for a time is simply updated, so arming a time that is already armed is harmless. Alternative: a daily refresh alarm that opens the database and replans; rejected here because two Room instances over one file are unsafe while the app is open, and the refresh alarm would itself depend on the system delivering it.
- **Top-up runs from the receivers and from boot.** `ReminderReceiver` and `AlarmReceiver` call it after doing their work, and `ReminderBootReceiver.rearm` calls it before arming. The alarm path calls it after starting the service so the ring is never delayed. Alternative: only from the last armed alarm; rejected because arming a batch from each firing keeps the armed window full.
- **`planDays` on the scheduler.** `ReminderScheduler.planDays` is 7 by default and 30 on Android, and the coordinator passes it to `TemplateReminders.plan`. A day is cheap to plan; the 1000-time cap bounds storage (about 60 KB). Alternative: one horizon for all platforms; rejected because iOS can hold only 64 requests, so the extra days would only be recomputed to be dropped.
- **Selection stays pure and shared.** The cap on kept times and "drop the past, then arm what fits" are functions on `AlarmPlan`, so they are covered by unit tests; the Android file only does the storage and the `AlarmManager` calls. The stored line format is unchanged, and a stored list from the previous version still reads as a schedule.
- **Cancelling on replace uses the stored armed set**, so a replan cancels exactly what was armed, including times armed by a top-up.

## Risks / Trade-offs

- [A task done in the app is still in the stored plan until the app replans] → The coordinator replans on every store change, so a stored plan only outlives the app's own data when the app is not running at all, where nothing can be done.
- [A day's reminders run out after the 1000-time cap, about 40 days of a once-a-day reminder but only about 1-2 weeks of one repeating every 30 minutes] → Accepted; opening the app replans from scratch.
- [Top-up work in a receiver's `onReceive` must be quick] → It reads and writes one preferences string of at most 1000 lines and makes at most 120 `AlarmManager` calls.
- [A force-stopped app or a vendor battery saver cancels alarms] → Out of scope; opening the app arms everything again.
- [Two firings close together could top up at the same time] → Both run on the main thread in order, and arming is idempotent.
