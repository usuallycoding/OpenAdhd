# Tasks

## 1. Ending the ring

- [x] 1.1 Add the ring-in-progress flag to `AlarmService` (set when the sound starts, cleared in `end()` and `onDestroy`) and have `MainActivity.onStart` send `ActionDismiss` only when it is set (verify: `./kotlin build -m androidApp` succeeds; on a device with the phone unlocked and in use, an alarm that shows only a banner stops when the app is opened from the launcher and from the recent apps)
- [x] 1.2 Check the neighbouring paths (verify on a device: Dismiss, Open task and tapping the notification still stop the ring; a repeating alarm still rings at its next repeat after the app was opened; opening the app with no alarm ringing starts no service, seen in `adb shell dumpsys activity services com.usuallycoding.openadhd`)

- [x] 1.3 Set the Dismiss `PendingIntent` as the delete intent of the ringing alarm notification (verify: `./kotlin build -m androidApp` succeeds; on a device, swiping the ringing notification away, where the system allows it, stops the sound and vibration, and later repeats still ring; ending a ring from Dismiss or the app does not start another)

## 2. Docs and verification

- [x] 2.1 Add a sentence to the Android alarm description in the Reminders section of `README.md` saying that opening the app ends a ringing alarm (verify: matches the behaviour in 1.1)
- [x] 2.2 Run `./kotlin test` and `./kotlin build -m androidApp` (verify: both succeed)
