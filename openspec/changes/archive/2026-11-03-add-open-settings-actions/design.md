# Design

## Context

See proposal.md for why and the specs for behavior. Observed state:

- `ReminderScheduler` is the platform seam for reminders. It already has `openAlarmSettings()`, a no-op by default, overridden only on Android to open the full-screen alerts page for the app (and only from Android 14).
- Task sheet: `EntrySheet` shows the three messages as text from state flags set when a save returns `SavedNotificationsOff`, `SavedAlarmsOff` or `SavedFullScreenOff`. Only the full-screen one has a button, which calls `reminders.openAlarmSettings` from `ReminderActions`. `TodayViewModel.openAlarmSettings()` forwards to the scheduler.
- Template form: `TemplatesViewModel` emits `NotificationsOff`, `AlarmsOff` and `FullScreenOff` as plain strings on `notices`, and `App` shows each with `snackbarHostState.showSnackbar(text)`. `container.reminders` is the scheduler (null where there is none).
- The messages cannot appear on the desktop: its `ensurePermission()` is always true and it has no alarm or full-screen permission, so the new openers stay no-ops there.
- The Android module sets no explicit minimum SDK, so the opener must not assume an API level newer than the code already uses.
- Reminders are armed from the shared `ReminderCoordinator`, which hands the whole plan to `ReminderScheduler.replaceAll` when the app starts and whenever the data changes. Nothing runs it when the app returns to the front from the system settings, and `IosReminderScheduler` reads its permission only at start and when it asks, so after the user turns notifications on in Settings its answer is stale and no pending request exists for a reminder that was saved while they were off.
- Observed by the user on a phone: after turning notifications on, an alarm saved earlier fires, but a notification reminder does not, on Android and on iOS; restarting the app makes it fire. So the arming itself is right and only its trigger is missing.

## Goals / Non-Goals

**Goals:**
- Each of the three messages offers a one-press way to the right system page, in the sheet and in the snackbar.
- One mechanism for both places, with the platform code in the existing scheduler.

- A reminder saved while notifications or alarms were off fires at its time once they are on, without restarting the app.

**Non-Goals:**
- Showing a different message after returning from the settings.
- A button on any message that is not about permission.

## Decisions

**Two openers on the scheduler, each meaning one thing.** `openNotificationSettings()` is new: it opens the page where the user allows notifications for the app. `openAlarmSettings()` stays and its meaning widens a little: it opens the page where the user allows what alarms need. Both default to doing nothing. *Alternative:* one `openSettings(target)` method; rejected because the existing method and its fake and tests already have this shape, and an enum parameter buys nothing for two targets.

**Which opener each message calls.**

| Message | Where it appears | Opener | What opens |
|---|---|---|---|
| Notifications are turned off | Android, iOS | `openNotificationSettings` | Android: the app's notification page. iOS: the app's Notifications page (iOS 16 and later), else the app's settings page |
| Alarms are turned off | iOS 26+ | `openAlarmSettings` | iOS: the app's settings page, where the Alarms permission is |
| Full-screen alarms are turned off | Android 14+ | `openAlarmSettings` | Android: the full-screen alerts page (unchanged) |

Alarms use the alarm opener, not the notification one, because on iOS the alarm permission is on the app's settings page, which is not the same page as Notifications; the user asked for the notifications message to open the notification page, and the alarm messages to reach where alarms are allowed.

**Android implementation.** `openNotificationSettings()` starts `Settings.ACTION_APP_NOTIFICATION_SETTINGS` with `EXTRA_APP_PACKAGE` set to the package, with the new-task flag as `openAlarmSettings` has, and ignores `ActivityNotFoundException`. Before Android 8 that action does not exist, so it starts `ACTION_APPLICATION_DETAILS_SETTINGS` for the package instead.

**iOS implementation.** `openNotificationSettings()` opens `UIApplication.openNotificationSettingsURLString` where it exists (iOS 16 and later) and `openSettingsURLString` otherwise. `openAlarmSettings()` opens `openSettingsURLString`. Both call `UIApplication.openURL(url, options, completionHandler)` on the main thread and ignore a failure to open.

**The sheet gets the button through `ReminderActions`.** It gains `openNotificationSettings` next to `openAlarmSettings`, filled from `TodayViewModel` (which forwards to the scheduler, doing nothing when there is none). `EntrySheet` shows an `Open settings` `TextButton` under the notifications message (opener: notifications) and the alarms message (opener: alarms), as it already does under the full-screen message.

**The snackbar gets the action through a small notice type.** The notices become `TemplateNotice(text, settings: SettingsTarget?)` with `SettingsTarget` being `Notifications` or `Alarms`; `NotificationsOff` carries `Notifications`, `AlarmsOff` and `FullScreenOff` carry `Alarms`, and any other notice has none. `App` shows each with `showSnackbar(text, actionLabel = "Open settings" when there is a target, duration = SnackbarDuration.Long)` and on `ActionPerformed` calls the matching opener on `container.reminders`. A snackbar with an action would otherwise stay until dismissed, so the duration is set explicitly. The notices are shown one after another, as now.

**Re-arm when the app returns to the front.** `ReminderScheduler` gets `onAppResumed()`, a no-op by default so the desktop is unchanged. `App` calls it on the lifecycle's resume (`LifecycleResumeEffect` on `container.reminders`). It is the same arming the app already does at start, run again, not a restart. *Alternative:* restart the process when the permission changes; rejected as heavy and visible, and not needed since the arming is repeatable. *Alternative:* watch the permission and re-arm only when it changed; rejected for now because the permission APIs differ per platform and re-arming is cheap and idempotent.

**iOS.** `onAppResumed()` reads the notification settings (`getNotificationSettingsWithCompletionHandler`) to refresh `authorized`, then calls `replaceAll(lastUpcoming)`. That re-adds the pending requests that iOS did not keep while notifications were off, and re-routes the alarms, so a changed AlarmKit permission is picked up as well. Until the coordinator has handed over a plan once, `onAppResumed()` does nothing: the first resume of a cold start would otherwise submit an empty list and remove the pending requests before the real plan arrives.

**Android.** Arming does not depend on the permission here: `ReminderAlarms.replace` arms the alarms whatever the notification setting is, and the receiver checks the permission only when a reminder fires. `onAppResumed()` runs `ReminderAlarms.topUpSafely`, which arms the next times of the stored plan again, as after a restart. It is done although the emulator did not show the problem (see "Open points"), because the user's phone does: restarting the app there makes the notification fire, and a restart runs the same arming.

**Cost.** Re-arming on every resume repeats work the app already does on every change. It is idempotent, so a reminder is never delivered twice. It also covers the other direction: after notifications are turned off while reminders are pending, the app learns that on return.

## Open points

- **Android cause:** not explained. On the Android 16 emulator, a notification reminder saved with the permission refused, the permission then granted with `pm grant` and the app not restarted, was posted at its time; so Android's arming is not broken there. On the user's phone the same sequence needs a restart. Differences not ruled out: the permission was changed from the system settings page, not with `pm grant` (which may restart the app or work differently), the phone's Android version and maker, battery savers, and the inexact window of the alarm (the emulator showed a window of about four minutes for a reminder six minutes ahead). The re-arm on resume does what a restart does, so it is expected to fix the phone, but that is checked in the device task, not assumed.

## Risks / Trade-offs

- [Re-arming on every resume could submit an empty plan at start] -> The iOS hook does nothing until a plan has been handed over once; a test covers it.
- [iOS may not keep notifications that were scheduled while they were denied] -> Covered by re-submitting the last plan on return; checked on an iPhone in the device task.
- [The iOS alarm permission may not be where I expect] -> The alarm opener opens the app's settings page, the most general place; check on an iPhone with iOS 26 that the Alarms switch is reachable from it.
- [The openers are platform code with no automated test] -> Tests count calls on the fake scheduler; the real pages are checked by hand on an Android phone and an iPhone.
- [`ACTION_APP_NOTIFICATION_SETTINGS` opens a different page on some Android skins] -> It is the documented action; an activity that cannot be found is ignored rather than crashing.
- [The snackbar stays up for the Long duration while an action is shown] -> Accepted; it is dismissed by the next notice, by the action or by timing out.
