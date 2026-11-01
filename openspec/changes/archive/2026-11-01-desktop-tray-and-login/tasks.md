# Tasks

## 1. Sleep-safe scheduler

- [x] 1.1 Change `DesktopReminderScheduler` to a wall-clock tick (20 s) with a 2-minute grace, tick, grace and clock as constructor parameters; due times older than the grace are dropped unshown, and `replaceAll` still resets the queue. Extend `DesktopReminderSchedulerTest` with virtual-time cases for the spec scenarios "wakes just after the time" and "wakes long after the time" (a clock jump with no ticks in between), and verify the existing cases still pass with `./kotlin test -m shared`
- [x] 1.2 Remove the `onPending` callback and `DesktopNotifier.setPending`, and update the tests that used them; verify the module compiles and tests pass

## 2. Tray and background lifecycle

- [x] 2.1 Rework `DesktopNotifier` into a sink that sends reminder notifications to a tray state, or to the in-app fallback when the system has no tray; wire it in `createDesktopContainer` so the container is still built once. Verify with a test that the fallback receives the text when no tray is supported and the tray sink receives it otherwise
- [x] 2.2 In `desktopApp/src/main.kt` add a window-visibility state, a `Tray` shown for the whole session with Open and Quit, icon click that shows the window, and hide-on-close when `SystemTray.isSupported()` (quit on close otherwise). Verify by running the app: closing the window leaves the tray icon and a reminder set one minute ahead still notifies; Quit removes the icon and exits
- [x] 2.3 Show the window from the macOS Dock click (`AppReopenedListener`) and from a reported click on a reminder notification, setting only visibility. Verify on the packaged `.app` that a Dock click shows the hidden window, and record in the task notes whether notification clicks are delivered
- [x] 2.4 Add the first-close notice with a marker file in `~/.openadhd/`, shown only when closing hid the window and never again afterwards. Verify with a unit test of the marker logic (first close shows, second does not, a fresh directory shows again) and by running the app

## 3. Launch at login on macOS

- [x] 3.1 Add the `LoginItem` seam and its macOS implementation writing and reading `~/Library/LaunchAgents/com.usuallycoding.openadhd.plist` from the `jpackage.app-path` launcher, with `--background`; `isAvailable` false without that property or off macOS. Verify with unit tests on a temporary directory: enable writes the file, disable removes it, `isEnabled` is false when the file is missing or names a different launcher
- [x] 3.2 Make `main` start with the window hidden when launched with `--background` and a tray exists. Verify by running the jar with and without the argument
- [x] 3.3 Add the Startup section and Launch at login switch to `SettingsScreen` and `SettingsViewModel`, taking an optional `LoginItem`, refreshed each time Settings is shown, absent when unavailable. Verify with a Compose UI test next to `SettingsUiTest`: the row is listed after Reminders and before Data when available and absent when not, turning it on and off calls the item, and the switch reflects a state changed outside the app on the next opening
- [x] 3.4 Check that backup export and Delete all data neither include nor change the login item. Verify with a test that a backup contains no login field and that Delete all data leaves the item enabled

## 4. Packaging and integration

- [x] 4.1 Build the app with `scripts/package-mac.sh`, open `dist/OpenAdhd.app`, and confirm `jpackage.app-path` is set so Launch at login is listed; adjust the script only if it is not. Verify by toggling the switch and checking the plist exists and then disappears
- [ ] 4.2 Verify end to end on macOS: set a reminder two minutes ahead, close the window, see the notification and the tray icon, then sleep the machine across a reminder time and confirm the late/grace behavior; turn Launch at login on, log out and in, and confirm the app is in the tray with no window
- [ ] 4.3 Update `README.md` desktop notes to say the app keeps running in the tray after the window is closed, how to quit, and how to turn on Launch at login, and verify the text matches the behavior seen in 4.2

## Workflow follow-up

- Archive the change after the tasks above are verified, then sync the specs.
