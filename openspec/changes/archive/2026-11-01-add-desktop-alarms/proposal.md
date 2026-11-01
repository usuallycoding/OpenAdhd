# Proposal

## Why

On the desktop an alarm is only a notification, and macOS silences notifications under Focus and Do Not Disturb, so the one reminder kind meant to be hard to miss is the easiest to miss on a Mac. The app now keeps running in the tray, so it can ring: a looping sound and an always-on-top card are an app window and app audio, which Focus does not silence. Desktop alarms were left out of `desktop-tray-and-login` on purpose, and macOS is the only desktop build so far.

## What Changes

- On the desktop, choosing the Alarm kind is offered in the entry sheet and the template form, and an alarm now rings instead of being shown as a notification. Every desktop reminder was a Notification before. **BREAKING** for the spec rule that the desktop offers no Alarm choice and shows alarms as notifications.
- A ring is a looping synthesized sound and a small always-on-top alarm card that appears even when the main window is hidden. On macOS the Dock icon also bounces until the app is noticed.
- Alarms that ring at the same time stack on one card: one row per task, each row with Open task and Dismiss. The sound lasts until the last row is gone, and a row stops by itself after 10 minutes.
- Open task shows the main window and scrolls to the task, highlighted, as on Android and iOS. Dismiss ends only that row and never cancels the task's later repeats. Showing the main window from the Dock or the tray does not end a ring.
- A row goes away by itself when its task is completed or deleted.
- An alarm whose time passed while the computer was asleep, with the app running, rings once on waking, for the task's most recent missed time, and its row says when it was due. This is an exception to "times already past are never fired late", for alarms only. Notifications keep the 2-minute rule.
- Each ring also posts the usual "Still to do" notification, so a missed ring leaves something in Notification Center.
- The ring is shared desktop code, so Windows and Linux builds inherit it. Only the Dock bounce is macOS behavior.

Out of scope: waking a sleeping Mac, ringing while the app is quit, an alarm volume separate from the system volume, ringing over the lock screen, choosing the alarm sound, and alarm actions inside the notification itself.

## Capabilities

### New Capabilities
- `desktop-alarms`: how an alarm rings on the desktop: the sound and the card, stacking, Open task and Dismiss, the 10-minute limit, dropping a row for a finished task, ringing on waking, and the companion notification.

### Modified Capabilities
- `task-reminders`: "Choosing the kind" (the desktop offers the Alarm choice), "Alarms are delivered as notifications until a platform rings them" (the desktop is no longer among the platforms that cannot ring) and "When a reminder fires" (the exception for an alarm missed while the computer slept).

## Impact

- `shared/src@jvm/DesktopReminderScheduler.kt`: carry each reminder's kind and entry, send alarms to a ringer, report `supportsAlarms`, and ring once on waking for missed alarms.
- New shared JVM code for the ringer (rows, limits, sound) and for the sound player, behind interfaces so tests run silently.
- `desktopApp/src/main.kt`: the always-on-top alarm card window, the Dock bounce, and Open task wired to the main window and `entryRequests`.
- `shared/src/ui/screens/EntrySheet.kt` and the template form already show the choice when the scheduler supports alarms, so they should need only tests.
- `README.md`: the line saying the desktop always shows an alarm as a notification.
- No database change and no new dependencies expected: `javax.sound` is part of `java.desktop`, which the app already ships.
