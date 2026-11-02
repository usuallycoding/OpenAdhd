# Tasks

## 1. The openers

- [x] 1.1 Add `openNotificationSettings()` (default no-op, documented) to `ReminderScheduler`, widen the comment of `openAlarmSettings()`, and give `FakeReminderScheduler` a counter for the notification opener; verify `./kotlin build` compiles on every target
- [x] 1.2 Android: implement `openNotificationSettings()` in `AndroidReminderScheduler` with `ACTION_APP_NOTIFICATION_SETTINGS` and the package extra, falling back to the app's details page before Android 8, ignoring `ActivityNotFoundException`; verify `./kotlin build -m androidApp` succeeds
- [x] 1.3 iOS: implement `openNotificationSettings()` (notification page on iOS 16 and later, else the app's settings page) and `openAlarmSettings()` (the app's settings page) in `IosReminderScheduler`; verify `./kotlin build -m iosApp` succeeds

## 2. The reminder sheet

- [x] 2.1 Add `openNotificationSettings` to `ReminderActions` and forward it from `TodayViewModel` (doing nothing without a scheduler); verify with a test in `ReminderViewModelTest` that it reaches the scheduler once and does nothing with no scheduler
- [x] 2.2 In `EntrySheet`, show an Open settings button under the notifications-off message (notification opener) and under the alarms-off message (alarm opener), keeping the full-screen button; verify with desktop UI tests (fake scheduler that refuses permission, and one that refuses alarms) that each message shows the button and that pressing it counts one call on the right opener

## 3. The template snackbar

- [x] 3.1 Replace the plain-string template notices with `TemplateNotice(text, settings)` and `SettingsTarget` (Notifications, Alarms): notifications-off carries Notifications, alarms-off and full-screen-off carry Alarms; verify with view model tests that saving a template with a reminder under each refused permission emits the right text with the right target and that other notices have none
- [x] 3.2 In `App`, show each notice with the Open settings action and a Long duration and call the matching opener on `container.reminders` when the action is pressed; verify with a desktop UI test that saving a template with a reminder while the fake scheduler refuses notifications shows the snackbar with Open settings and that pressing it counts one notification-opener call, and that a notice without a target shows no action

## 4. Docs and integration

- [x] 4.1 Mention in the README's Reminders section that the "turned off" messages have an Open settings button that opens the app's settings page; verify by reading the changed text
- [x] 4.2 Run `./kotlin test` for the whole project and `./kotlin build`; verify both succeed and that every existing message text is unchanged (search the repo for the three message strings)
- [x] 4.3 On an Android phone and an iPhone with notifications turned off for OpenAdhd, save a reminder on a task and a template and press Open settings in the sheet and in the snackbar, and verify the app's notification page opens; on an Android 14 phone with full-screen alerts off and on an iOS 26 iPhone with alarms refused, verify each alarm message opens the right page; then, with notifications off, save a notification reminder a few minutes ahead, turn notifications on in Settings, return to the app without restarting it, and verify on both phones that the notification appears at its time; do the same with an alarm saved while alarms are off on the iPhone and verify it rings after alarms are allowed (report each result, or say plainly what could not be checked)

## 5. Re-arming when the app returns to the front

- [x] 5.1 Add `onAppResumed()` (default no-op, documented) to `ReminderScheduler`, give `FakeReminderScheduler` a counter, and call it from `App` on the lifecycle's resume for `container.reminders`; verify with a desktop UI test that the fake scheduler's counter is above zero once the app is shown and that a container with no scheduler still starts
- [x] 5.2 iOS: implement `onAppResumed()` in `IosReminderScheduler` to refresh `authorized` from the notification settings and call `replaceAll(lastUpcoming)`, doing nothing until a plan was handed over once; verify `./kotlin build -m iosApp` succeeds and, if the plan logic is split out so it can be tested, with a test that resuming before the first plan submits nothing and resuming after it submits the same plan
- [x] 5.3 Android: read the result of the emulator test in the design's Open points; if the notification fires there without a restart, record that and look for what differs on the phone before writing code; if it does not, make `onAppResumed()` in `AndroidReminderScheduler` re-arm from the stored plan; verify `./kotlin build -m androidApp` succeeds and the result is recorded in design.md
- [x] 5.4 Add a test in `shared/test` that the coordinator or a scheduler wrapper hands the last plan to the scheduler again on resume without changing it, and that nothing is handed over before the first plan; verify `./kotlin test` passes
- [x] 5.5 Replace the README sentence "The app does not re-check the setting when you come back from it." with one saying that the app arms its reminders again when you come back, so a reminder set while notifications were off fires once you turn them on; verify by reading the changed line, and run `./kotlin test` and `./kotlin build` once more
