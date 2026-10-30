# Proposal

## Why

On Android an alarm that comes due while the phone is in use shows only a heads-up banner, because Android launches the full-screen alarm screen only when the device is locked or the screen is off. The banner collapses, the ringing notification cannot be swiped away, and its Dismiss action is hidden until the notification is expanded. Someone who goes straight to the app finds it still ringing: opening OpenAdhd from the launcher does not stop it, so the alarm kept ringing for five minutes in a real test. On newer Android versions swiping the notification away removes it while the ring carries on, so a user who dismisses the notification believes the alarm is off.

## What Changes

- On Android, bringing the app to the front by any route ends a ringing alarm: the sound and vibration stop and the ringing notification and alarm screen go away.
- Swiping the ringing notification away, where the system allows it, ends the ring as Dismiss does. The notification stays ongoing, so where the system does not allow a swipe nothing changes.
- Only the ring ends. Later repeats of the reminder still ring, as with Dismiss, and nothing else in the app changes.
- An alarm that comes due while the app is already in front is not ended by that: ending happens when the app comes to the front.
- Not changed: iOS (its alarm is a system alarm), the notification and alarm-screen buttons, the 10-minute limit, and when the alarm screen appears.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `task-reminders`: adds the requirement that opening the app ends a ringing alarm, and changes "The alarm screen" so that swiping the notification away stops the ring where the system allows a swipe, instead of saying it cannot be swiped.

## Impact

- `androidApp/src/AlarmService.kt` (a flag that says a ring is in progress), `androidApp/src/MainActivity.kt` (end the ring when the activity starts) and `androidApp/src/AlarmNotifications.kt` (a delete intent on the ringing notification).
- `README.md`: one sentence in the Android alarm description.
- No shared code, database, manifest or permission change. Checked on a device, since the behavior is Android activity and service lifecycle.
