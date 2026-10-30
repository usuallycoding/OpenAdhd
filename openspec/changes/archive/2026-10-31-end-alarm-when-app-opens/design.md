# Design

## Context

`AlarmService` rings in a foreground service and ends in `end()`, which stops the sound, removes the notification, broadcasts `ActionEnded` (the alarm screen finishes on it) and stops itself. It is started with `ActionDismiss` by the Dismiss button, by the alarm screen's Open task and by `MainActivity.showEntryOf` when the app is opened from the ringing notification. Opening the app any other way never reaches it. The service and `MainActivity` share one process.

## Goals / Non-Goals

**Goals:**
- Whatever way the user reaches the app, a ring in progress ends.

**Non-Goals:**
- Making the alarm screen appear while the device is in use, snoozing, or changing the notification's buttons.

## Decisions

- **End the ring in `MainActivity.onStart`.** `onStart` runs each time the activity becomes visible: from the launcher, from the recents, and after another activity such as the alarm screen goes away. It does not run for an alarm that starts while the activity is already visible, which gives the "already in front" rule for free. Alternative: `onResume`; rejected because it also runs after dialogs and permission prompts that leave the activity started.
- **A process-wide flag says a ring is in progress.** `AlarmService.ringing` is a `@Volatile` boolean set when the sound starts and cleared in `end()` and `onDestroy`. `onStart` sends `ActionDismiss` only when it is true, so opening the app when nothing rings starts no service. Alternative: always send the action; rejected because starting a service to do nothing wakes the process for no reason and can fail on restricted devices.
- **Reuse `ActionDismiss`.** It already keeps repeats, since only the ring ends and the armed times are untouched, and it already closes the alarm screen through `ActionEnded`.
- **Open task keeps working.** `showEntryOf` also sends `ActionDismiss`, from `onCreate` or `onNewIntent` before `onStart`. A second `ActionDismiss` from `onStart` before the service has handled the first is harmless: ending twice is idempotent.

- **A delete intent on the ringing notification.** `AlarmNotifications.build` sets the existing Dismiss `PendingIntent` as the delete intent of the ringing notification (not the fallback one, which has no ring to stop). The system sends it only when the user dismisses the notification, not when the service removes it, so ending a ring does not start another. The notification stays ongoing, so on versions where the system still refuses the swipe nothing changes, and from Android 14, where the system lets users swipe away foreground-service notifications, the swipe now stops the ring instead of leaving it ringing with no notification.

## Risks / Trade-offs

- [The user opens the app for some other reason and loses the ring they meant to answer] → The reminder's notification "Still to do" is not left behind; the ring stops but the task is still shown on Today when they open the app, and the next repeat still rings.
- [The flag could be left set if the process dies mid-ring] → It lives in the process, so a new process starts with it cleared.
- [`onStart` runs when the alarm screen closes over the app] → The ring has already ended by then, so the flag is false.
