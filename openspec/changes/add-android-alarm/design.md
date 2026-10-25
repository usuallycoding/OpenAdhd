# Design

## Context

`ReminderAlarms` (Android) arms every upcoming time with `setAndAllowWhileIdle`, capped at 100, keeps the whole schedule in SharedPreferences as `entryId|text|atMillis` lines, and re-arms it after a restart; `ReminderReceiver` posts the notification, whose tap opens the task (see the earlier change). `UpcomingReminder` carries the `kind` and `AndroidReminderScheduler` already reports `supportsAlarms`, but nothing reads the kind. `ReminderScheduler` has `hasPermission`, `ensurePermission` and `replaceAll`; the entry sheet shows a `ReminderSaveResult` after saving. See proposal.md for motivation and for how the alarm work is split.

## Goals / Non-Goals

**Goals:**
- A ring that works from a closed app, a locked screen and a restart, with a way to stop it that opens the task.
- Pure rules testable in `shared`; the Android platform classes kept thin.

**Non-Goals:**
- iOS, the desktop, snooze, mark-done, choosing the sound, or ringing on the wearable.

## Decisions

- **`setAlarmClock` for alarm times.** It is exempt from Doze and shows the next alarm in the system. Its `AlarmClockInfo` show-intent opens `MainActivity`. Notification times keep `setAndAllowWhileIdle`. Alternative: `setExactAndAllowWhileIdle`; rejected, `setAlarmClock` is the supported path for alarms and also gives the system's next-alarm display.
- **Exact-alarm permission.** `setAlarmClock` is an exact alarm, so from Android 13 it needs `USE_EXACT_ALARM` (granted at install, which suits an alarm app) and before that `SCHEDULE_EXACT_ALARM` (granted by default up to Android 12L, so declared with `maxSdkVersion` 32). A first version of this design assumed no permission was needed, and saving an alarm crashed with a `SecurityException`. As a safety net `ReminderAlarms` catches that exception and arms the same alarm with `setAndAllowWhileIdle`, so a missing permission costs exact timing and never crashes or loses the alarm. Alternative: asking the user to grant `SCHEDULE_EXACT_ALARM` in settings; rejected, it is denied by default from Android 14 and adds a second permission flow.
- **Two kinds of pending intent, told apart by address.** Notification times keep `openadhd://reminder/<id>/<at>` to `ReminderReceiver`; alarm times use `openadhd://alarm/<id>/<at>` to a new `AlarmReceiver`. `replace` cancels the previously stored items of either kind before arming the new ones.
- **A foreground service rings.** `AlarmReceiver` starts `AlarmService` (foreground type `mediaPlayback`). It plays the default alarm ringtone with `AudioAttributes.USAGE_ALARM` in a loop, vibrates in a pattern, posts an ongoing high-importance notification in an `Alarms` channel (category alarm, with a full-screen intent to `AlarmActivity`, the tap opening the task, and a Dismiss action), and schedules its own stop after 10 minutes. A second start replaces the first ring. When the time limit is reached it stops the sound, drops the foreground state and posts a normal "Still to do" notification. Alternative: ringing from the activity alone; rejected, it would stop when the screen is dismissed or the activity is killed.
- **A foreground-service fallback.** If the system refuses to start the service from the background, `AlarmReceiver` posts the same high-importance notification with the channel's alarm sound, so the user still hears a short alert. This is a risk check on a real device (below).
- **`AlarmActivity`.** A separate Compose activity declared `showWhenLocked` and `turnScreenOn`, excluded from recents and with its own task affinity so it never disturbs `MainActivity`. It shows the task name, Open task and Dismiss. Open task starts `MainActivity` with the entry id (the existing path that scrolls and highlights) and tells the service to stop; Dismiss tells the service to stop and finishes. It finishes itself when the service reports the ring ended.
- **Full-screen permission.** `NotificationManager.canUseFullScreenIntent()` on Android 14 and later (always true before). `ReminderScheduler` gains `fullScreenAlarmsAllowed()` (default true) and `openAlarmSettings()` (default nothing); Android opens `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` for the package. `TodayViewModel.saveReminder` returns a new `SavedFullScreenOff` result when the kind is Alarm and the check fails, after the notification permission step; the entry sheet shows the message with an Open settings button. `TemplatesViewModel` emits the message as a notice after saving a template with an alarm reminder. The alarm itself never depends on the answer.
- **Stored lines carry the kind.** `entryId|text|atMillis|kind`; a line with three fields reads as Notification, so a schedule stored by the current version keeps working. The codec and the rule that picks the soonest 20 alarm times and all notification times live in `shared` and are unit tested.
- **Limits as constants.** `MaxArmedAlarms = 20` and `RingLimit = 10 minutes` in one shared object, used by the platform and the tests.
- **Manifest.** Adds `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (up to Android 12L), `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `VIBRATE`, the receiver, the service with its type and the activity.
- **Completed tasks.** Completing a task changes the schedule the coordinator hands over, so `replace` cancels its future alarms as it cancels notifications today. A ring in progress is stopped by Open task, Dismiss or the limit; the schedule cannot name it because its time has passed.

## Risks / Trade-offs

- [The system may refuse to start a foreground service from the alarm broadcast] → `setAlarmClock` alarms are exempt from the background-start limits as far as documented; checked on a device in the verification task, with the notification fallback in place.
- [Android 14+ may hide the lock-screen screen] → The ring and a heads-up notification still work; the user is told and can open the setting.
- [A ring left unattended] → Stops after 10 minutes and leaves a notification.
- [The system lists every armed alarm as the next alarm] → Accepted and useful; only the soonest 20 are armed.
- [Battery managers on some phones delay or kill the service] → Documented in the README, as for notifications.
- [Nothing here can be unit tested end to end] → The pure rules are tested in `shared` and the platform behaviour has an explicit manual checklist.

## Migration Plan

No data change. Schedules stored by the previous version read as notifications until the app next hands over its schedule. Rolling back means ignoring the kind and arming everything as notifications.
