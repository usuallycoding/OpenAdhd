# Design

## Context

Today entries are `PlannedTask` rows (`planned_tasks` in Room) with their own date; the Today screen shows only today's date, and past days are read-only in History. `EntryItem` shows a trash button when nothing in the entry is done. Settings are `SettingKey` values on `Settings` (name plus `GameRules`), stored as overrides, with ranges checked by `checkSetting`. Backups are `BackupJson` over `StoreState`. Platform-specific seams already exist (`DataFiles`, created in the three entry points and held by `AppContainer`) and activity-result handling is done in `AndroidDataFiles`. There are no notification permissions, receivers or channels on Android, no notification setup on iOS, and the desktop app quits when its window closes. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- A reminder that is a property of the entry, survives restarts, and is delivered by the platform even when the app is closed on Android and iOS.
- One piece of shared logic that decides what should fire and when, and thin platform code that only delivers it.
- Behaviour that is easy to test without a device: pure time rules and a coordinator driven by a fake scheduler.

**Non-Goals:**
- Notification buttons, several reminders per entry, sounds, per-task stop times, reminders for past days, or catching up on missed ones.

## Decisions

- **A `Reminder` on the entry.** `Reminder(startAt: Instant, repeatMinutes: Int?)` as `PlannedTask.reminder`, stored in two nullable columns on `planned_tasks` (`reminderAtMillis`, `reminderRepeatMinutes`) by a Room migration from 7 to 8 that only adds columns. The start is an absolute instant, chosen in the device's zone for the entry's date. Completing an entry leaves the reminder as it is, so unchecking brings back what is still to come. Alternative: a separate reminders table; rejected because there is at most one per entry.
- **`PlannerRepository.setReminder(entryId, reminder?): Boolean`.** It returns false, changing nothing, for an unknown or completed entry. Validation of the time lives outside the store, in `ReminderRules.check`, because it needs the clock, the zone and the stop hour; the sheet and view model call it before saving.
- **Pure time rules.** `ReminderRules` holds `check(start, now, date, stopHour, zone): Problem?` (passed, after the stop time, not today) and `upcoming(reminder, date, stopHour, now, zone): List<Instant>`: the start and every repeat after it that is later than `now` and earlier than the stop instant (the entry's date at `stopHour` in the zone). Because the stop hour is below 24, nothing can fire after midnight.
- **Stop time is a setting.** `Settings.reminderStopHour` (default 22, 1 to 23), a `SettingKey.ReminderStopHour` in a new `Reminders` group, handled by the existing text, validation, override and restore code. It lives on `Settings` rather than `GameRules` because it is not a game rule. Alternative: a stop time per reminder; rejected to keep the sheet to two controls.
- **A scheduler seam, reconciled as a whole.** `ReminderScheduler { fun hasPermission(): Boolean; suspend fun ensurePermission(): Boolean; fun replaceAll(upcoming: List<UpcomingReminder>) }`, with `UpcomingReminder(entryId, text, times)`. A `ReminderCoordinator` in shared code observes today's date, today's entries, the settings and the clock, and whenever anything relevant changes calls `replaceAll` with the full list of upcoming reminders of pending entries. The platform cancels what it had and arms the new list. This is simpler and more robust than adding and cancelling single alarms, and it makes backup import, uncheck, delete and a changed stop time all work through one path. The coordinator is started from `App` and also runs at app start.
- **Android delivery.** `AlarmManager.setAndAllowWhileIdle` for each upcoming time, soonest first and capped at 100, a `BroadcastReceiver` that posts the notification on a `Reminders` channel, `POST_NOTIFICATIONS` requested through the activity result API (as `AndroidDataFiles` does), and `RECEIVE_BOOT_COMPLETED` with a receiver that re-arms alarms after a restart. The armed schedule is kept in `SharedPreferences` so the receivers never need the database. No exact-alarm permission is requested, so a reminder may be a few minutes late when the device is dozing; that is acceptable for a nudge. Tapping a notification opens `MainActivity`.
- **iOS delivery.** `UNUserNotificationCenter`: remove all pending requests, then add the soonest 60 as non-repeating time-interval notifications (the system keeps at most 64 pending), request authorization through the same path, and set a delegate so a notification is still shown while the app is open. The coordinator tops the list up each time the app starts or the data changes. Alternative: repeating triggers; rejected because they cannot stop at the stop time.
- **Desktop delivery.** `DesktopReminderScheduler` keeps a coroutine that sleeps until the next time and shows `TrayIcon.displayMessage` when `SystemTray.isSupported()`, otherwise reports to an in-app message (`AppContainer.reminderNotices`, shown in the existing snackbar). It only runs while the app runs and needs no permission. The spike (task 1.1) confirmed that `TrayIcon.displayMessage` from a `jpackage` app on macOS shows a native notification under the app's own name ("Still to do" with the task name); macOS asks once to allow notifications for the app the first time one is shown. The in-app message stays as the fallback only for systems where `SystemTray.isSupported()` is false.
- **No catch-up.** At start the coordinator computes `upcoming` from the current time only, so missed reminders are dropped.
- **Permission when saving.** The sheet calls `ensurePermission()` when the user saves; a refusal still saves the reminder and the sheet shows a line that notifications are off for OpenAdhd and where to turn them on.
- **Backup.** `PlannedDto` gets optional `reminderAt` and `reminderRepeatMinutes`. They are additive and ignored by older readers, so the format version stays 1. Import goes through the store, so the coordinator re-arms everything.
- **UI.** `EntryItem` shows a `MoreIcon` ("..." in three dots) button for entries that are not completed, not read-only and not being reordered. The `EntrySheet` is a `FormSheet` with: reminder presets (as chips, disabled when they cannot work), a Pick a time button opening a Material 3 `TimePicker` in a dialog, repeat chips, Save, Remove reminder when one is set, and a Delete task button when nothing is done. The row's supporting text gets a line "Reminder 15:00, every 30 min". Repeat defaults to every 30 minutes for a new reminder.
- **Text.** Title "Still to do", body the entry name.

## Risks / Trade-offs

- [A reminder is late or missing on a phone because of battery rules] → Use alarms that run in Doze; document that some manufacturers' battery savers can still delay them.
- [The 64-notification limit on iOS] → Schedule only the soonest 60 and top up when the app opens; with a stop time most days have far fewer.
- [Desktop reminders are missed when the app is closed] → Stated in the spec; no catch-up.
- [The tray notification may be blocked by the user in macOS settings] → The first one triggers macOS's own allow prompt; if notifications are blocked there, nothing is shown, as for any app. The in-app message covers systems without a system tray.
- [Time zone or clock changes] → Times are absolute instants recomputed from the entry's date and the current zone every time the coordinator runs.
- [A scheduler and the store disagree] → The platform schedule is always replaced as a whole from the store, never edited piecemeal.
- [Schema change] → Two nullable columns; existing rows have no reminder.

## Migration Plan

Room moves from 7 to 8 by adding two nullable columns. Nothing else changes for existing data, and there are no reminders until the user sets one. Rolling back means ignoring the columns.
