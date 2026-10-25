# Design

## Context

A task's reminder is `Reminder(startAt, repeatMinutes)` stored in two nullable columns on `planned_tasks`; a template's is `TemplateReminder(minuteOfDay, repeatMinutes)` in two columns on `templates`. `generatedTask` builds the task's reminder from the template for scheduled generation and for `assign`. `ReminderCoordinator` turns pending tasks into `UpcomingReminder(entryId, text, times)` and hands the whole list to a `ReminderScheduler`, whose Android and iOS implementations show notifications and whose desktop implementation shows a tray balloon or an in-app message. The entry sheet reads `ReminderActions` built by `TodayViewModel`; the template form is `TemplateDialog`. Backups are `BackupJson` with additive optional fields. See proposal.md for motivation and for how the alarm work is split.

## Goals / Non-Goals

**Goals:**
- One `kind` that travels everywhere a reminder does, with the same default everywhere.
- A scheduler contract that platform changes can build on without touching the data model again.

**Non-Goals:**
- Any ringing, new permission or alarm screen, and anything on the desktop.

## Decisions

- **`ReminderKind { Notification, Alarm }` as a field with a default.** `Reminder`, `TemplateReminder` and `UpcomingReminder` each gain `kind` defaulting to `Notification`, so every existing call and test compiles and means what it did. Alternative: a separate boolean `alarm`; rejected, an enum reads better in specs and leaves room for a third kind.
- **Stored as text.** `reminderKind TEXT NOT NULL DEFAULT 'Notification'` on both tables, added by a Room migration from 9 to 10 that only adds columns. A value that is not a known kind reads as `Notification`, as an unreadable schedule reads as none, so a bad row never blocks start-up. `setReminder` writes the kind with the time and repeat in the same update.
- **Copied by the one builder.** `generatedTask` copies the template reminder's kind, so scheduled generation and the + button agree without extra code.
- **The coordinator hands the kind over.** `UpcomingReminder` carries it, and because the list is compared before it is handed over, changing only the kind re-hands the schedule. The platform delivery code ignores it in this change.
- **`ReminderScheduler.supportsAlarms`.** A property with a default of false. Android and iOS return true, because they will ring (iOS may only approximate); the desktop scheduler keeps the default. `TodayViewModel` and `TemplatesViewModel` expose it as `canAlarm`, and the controls are drawn only when it is true. Alternative: deciding by platform name in the UI; rejected, the scheduler already is the platform seam for reminders.
- **UI.** The kind is two filter chips, Notification and Alarm, in the entry sheet below the repeat and in the template form below the repeat, the latter only once a time is set. A short line under them says that an alarm rings where the device supports it and is shown as a notification otherwise. The text stays true after the platform changes land, so it does not need editing then. The entry sheet's `save` carries the kind (default Notification, ahead of the result callback so existing callers compile).
- **Row text.** `ReminderRules.summary` starts with "Alarm" for an alarm and "Reminder" otherwise.
- **Backup.** `PlannedDto` and `TemplateDto` get `reminderKind: String? = null`. Missing means Notification; a present value that is not a known kind is refused with a message, as other unknown values are.
- **Permission.** Saving an alarm asks for notification permission the same way a notification does; alarm-only permissions belong to the Android change.

## Risks / Trade-offs

- [Choosing Alarm now gives only a notification] → Stated in the line under the choice and in the spec; every reminder still fires.
- [Two sources of the default] → One constant on the enum's usage (the parameter default), and tests that every layer defaults the same way.
- [Schema change] → Two columns with defaults; existing rows read as Notification.

## Migration Plan

Room moves from 9 to 10 by adding the two columns. Nothing changes for existing reminders. Rolling back means ignoring the columns.
