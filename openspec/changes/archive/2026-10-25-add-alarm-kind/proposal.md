# Proposal

## Why

A quiet notification is easy to miss for the tasks that matter most, and the user wants some reminders to ring like an alarm. Ringing is a large piece of platform work on Android and iOS, so this change first adds the choice itself everywhere (the data, the controls, the backup), and the platform changes that follow only have to deliver it.

## What Changes

- **Reminder kind:** every reminder is either a Notification (the default, as today) or an Alarm. The kind is kept on a task's reminder and on a template's reminder, and a task made from a template, by schedule or with the + button, takes the template's kind.
- **Choosing it:** the task's "..." sheet and the template form get a Notification / Alarm choice next to the time and repeat. It is shown on Android and iOS and hidden on the desktop app, which keeps notifications only.
- **Seen on the row:** an alarm reminder reads "Alarm 15:00, every 30 min" on the task's row instead of "Reminder 15:00, ...".
- **Delivery for now:** until a platform change makes the alarm ring, an Alarm is delivered exactly like a Notification. The choice says so in a short line under it.
- **Backup:** the kind is part of the export and import; older files import as Notification.
- **Not in this change:** the ringing itself on Android (own change), iOS AlarmKit or Time Sensitive delivery (own spike and change), alarm actions (Open task and Dismiss come with the Android change), and anything on the desktop app.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `task-reminders`: the reminder kind, where it is chosen, how the row shows it, how it is copied from a template, and that it is delivered as a notification until a platform rings it.
- `task-templates`: a template's reminder has a kind.
- `data-backup`: a backup holds the reminder kind.

## Impact

- `shared` domain: `ReminderKind` on `Reminder` and `TemplateReminder`, carried through the template-to-task builder, `ReminderRules.summary`, `UpcomingReminder` and the coordinator, and a `supportsAlarms` flag on `ReminderScheduler` (false by default).
- `shared` data: a kind column on `planned_tasks` and `templates` and a Room migration from 9 to 10; both stores; the backup format (an additive optional field, version stays 1).
- `shared` UI: the kind choice in the entry sheet and the template form, shown only where the scheduler supports alarms; the row text.
- Platforms: Android and iOS schedulers declare `supportsAlarms`; their delivery code is unchanged.
- Tests: rules and summary, both stores, migration, backup round trip with and without the field, coordinator, view models and UI for the choice.
- No new libraries.
