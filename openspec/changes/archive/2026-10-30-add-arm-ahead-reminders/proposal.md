# Proposal

## Why

Reminders and alarms of scheduled templates only reach the device once the app has run on that day: the entry that carries the reminder is created when the app starts, and only then is its time armed. If the app is not opened on a morning, the 09:00 reminder or alarm of a daily template never fires, although the system would deliver it with the app closed. The schedule simply runs out at the end of the last day the app was used.

## What Changes

- While working out its schedule, the app also plans the reminders of scheduled templates for today and the next days, from the template's schedule and reminder, even though the entries do not exist yet, and hands them to the platform together with those of today's entries.
- A template's planned reminder is left out for a day on which its entry has already been generated; that entry's own reminder is used instead, so completing, removing or editing it behaves as now and nothing fires twice.
- The stop time, the past-times rule, the Notification/Alarm kind and the caps on armed alarms and notifications apply as for any reminder. The soonest times are armed first, so today's come before later days'.
- Opening the app, or tapping the reminder, still generates the entry; a tapped reminder whose entry is not on Today opens Today with nothing highlighted, as already specified.
- Not changed: the desktop app still delivers only while running. An Android job that refreshes the schedule on its own in the background is a separate, later change.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `task-reminders`: adds reminders for scheduled templates that are armed ahead of their entries, and states in "Delivery on each platform" that this works with the app not opened.

## Impact

- `shared/src/domain/Reminders.kt` / `ReminderScheduler.kt`: `ReminderCoordinator` also observes the templates and plans the next days; the rule for a template's times sits beside `ReminderRules`.
- `shared/test/ReminderCoordinatorTest.kt` and the reminder rules tests; no platform code, database or schema change.
- Limits that remain: how far ahead anything is armed is bounded by the platform caps (20 alarms on Android and iOS, 100 notifications on Android, 64 pending requests on iOS), and a day's reminders that have not been armed yet are armed the next time the app opens.
- Out of scope: the Android background refresh, desktop background delivery, and any change to how entries are generated.
