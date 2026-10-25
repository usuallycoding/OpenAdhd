# Proposal

## Why

Reminders can now be marked as alarms, but on every platform an alarm still arrives as a quiet notification. For the tasks that matter most the point of choosing an alarm is that it rings until the user deals with it, even with the phone locked or on silent. This change makes that true on Android; iOS and the desktop follow separately or stay as they are.

## What Changes

- **Alarms ring on Android:** at each time of an alarm reminder the device rings with the default alarm ringtone on the alarm volume, loops, vibrates and turns the screen on. It works with the app closed, on silent and after a restart, and an alarm that is set shows in the system's next-alarm display.
- **Alarm screen:** over the lock screen it shows the task's name and two buttons, Open task and Dismiss. Open task stops the ring and opens the task on Today, scrolled to and highlighted. Dismiss stops this ring; later repeats still ring. Tapping the notification works as Open task.
- **Limits:** a ring stops by itself after 10 minutes and stays as a notification; a new alarm replaces one already ringing; at most the soonest 20 alarm times are armed at a time.
- **Full-screen permission:** on Android 14 and later the lock-screen screen needs a permission the user may have to grant. Without it the alarm still rings, with a heads-up notification instead; saving an alarm says so and offers to open the setting. A template saved with an alarm shows the same note afterwards.
- **Notifications unchanged:** reminders of the Notification kind are delivered exactly as before.
- **Not in this change:** iOS (own spike and change), the desktop app, snooze or mark-done on the alarm, and choosing the sound.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `task-reminders`: an alarm rings on Android, its screen and two actions, the 10-minute limit, the permission note, and the fallback wording for platforms that cannot ring.

## Impact

- `shared` domain: a codec for the stored reminder lines with the kind, the limits (20 armed alarms, 10 minutes of ringing), a function that splits the schedule by kind, and two additions to `ReminderScheduler` (whether full-screen alarms are allowed, and opening that setting).
- `shared` UI: a save result and a note with an Open settings button in the entry sheet, and a notice after saving a template.
- `androidApp`: `setAlarmClock` scheduling, an alarm receiver, a foreground ringing service, an alarm screen activity, a channel, and manifest permissions (full-screen intent, foreground service, vibrate) and declarations.
- Tests: the pure parts in `shared`; the ringing, the screen, the lock-screen behaviour and the permission are checked by hand on a phone.
- No new libraries and no storage or backup change.
