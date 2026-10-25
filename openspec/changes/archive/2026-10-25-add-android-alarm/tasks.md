# Tasks

## 1. Shared rules and the permission note

- [x] 1.1 Add a codec for the stored reminder lines with the kind (three-field lines read as Notification), the limits `MaxArmedAlarms = 20` and `RingLimit = 10 minutes`, and a function that splits a schedule into notification times and the soonest 20 alarm times (verify: unit tests for encode and decode with both kinds and a three-field legacy line, a text containing the separator, the limit with 25 alarm times and 80 notification times, an empty schedule, and the order kept soonest first)
- [x] 1.2 Add `fullScreenAlarmsAllowed()` (default true) and `openAlarmSettings()` (default nothing) to `ReminderScheduler` and the test fake, a `SavedFullScreenOff` save result, and return it from `TodayViewModel.saveReminder` after the notification permission step for alarms only (verify: view model tests for an alarm with the setting off, a notification with it off, an alarm with it on, a refused notification permission taking precedence, and the reminder being saved in every case)
- [x] 1.3 Show the message with an Open settings button in the entry sheet, and emit it as a notice after saving a template whose reminder is an alarm (verify: UI tests that the message and button appear only for an alarm with the setting off, that the button calls `openAlarmSettings`, and that the template notice shows once after saving; both absent when allowed)

## 2. Android scheduling

- [x] 2.1 Rework `ReminderAlarms`: items carry the kind, notification times are armed as before and alarm times with `setAlarmClock` (show-intent opening `MainActivity`, operation a broadcast to `AlarmReceiver` at a per-time address), `replace` cancels both kinds and uses the shared split and codec, boot re-arm reads the kind, and `AndroidReminderScheduler` passes the kind and implements the two permission functions (verify: the Android build succeeds and the shared tests pass; behaviour is checked by hand in 4.3)

## 3. Android ringing

- [x] 3.1 Add `AlarmReceiver`, `AlarmService` (alarm-stream ringtone in a loop, vibration, ongoing high-importance notification with the full-screen intent, Open task and Dismiss actions, the 10-minute stop leaving a normal notification, replacement of a ring in progress, the notification fallback if the service cannot start) and the `Alarms` channel (verify: the Android build succeeds; checked by hand in 4.3)
- [x] 3.2 Add `AlarmActivity` (shown over the lock screen, turns the screen on, task name, Open task opening the task through the existing path, Dismiss, closing when the ring ends), the manifest permissions, service and activity declarations, and any strings (verify: the Android build succeeds and the manifest merges; checked by hand in 4.3)

## 4. Docs and verification

- [x] 4.1 Update the Reminders section of `README.md`: an alarm rings on Android, its actions and 10-minute limit, the Android 14 full-screen setting, and that iOS and the desktop still show alarms as notifications (verify: the steps work as written in 4.3)
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 4.3 On an Android phone: an alarm with the app closed, locked, on silent and after a restart; Open task, Dismiss and the repeat after a dismiss; ignoring a ring for 10 minutes; two alarms close together; an alarm for a completed task; a notification-kind reminder still quiet; and with full-screen alerts off on Android 14 or later, the heads-up ring and the message with the settings button (verify: manual)
