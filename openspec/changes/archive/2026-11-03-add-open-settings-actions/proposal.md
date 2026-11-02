# Proposal

## Why

When a reminder cannot work because notifications, alarms or full-screen alerts are off, the app says so and tells the user to "turn them on in your device's settings", then leaves them to find the right page alone. Only one of the three messages, the full-screen one in a task's sheet, has a button. The same messages after saving a template are plain snackbars with no way forward.

## What Changes

- The three "turned off" messages each get an **Open settings** button in a task's reminder sheet and an **Open settings** action on the snackbar shown after saving a template:
  - "Notifications are turned off for OpenAdhd" opens the app's notification settings page.
  - "Alarms are turned off for OpenAdhd" (iPhone, iOS 26 or later) opens the app's settings page, where alarms are allowed.
  - "Full-screen alarms are turned off for OpenAdhd" (Android 14 and later) opens the full-screen alerts setting for the app, as its sheet button already does; the snackbar gets the action it lacked.
- The snackbar stays on screen long enough to read and press, and pressing the action does not close anything else.
- When the app returns to the front, for example from the system settings, it arms its reminders again. A notification reminder saved while notifications were off now fires once they are turned on, without restarting the app. Today it does not on Android or iOS, although restarting the app makes it fire, and an alarm already fires after its setting is turned on. On iOS this also picks up a changed alarm permission.
- Nothing changes where these messages never appear, such as the desktop.

Out of scope: new permission prompts, and any change to when the messages appear.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `task-reminders`: the requirements for the notification permission, the permission when saving a template reminder, alarms without the full-screen permission and the iOS alarm permission now say that the message offers to open the system settings, in the sheet and in the snackbar; and a new requirement that reminders are armed again when the app returns to the front.

## Impact

- `shared/src/domain/ReminderScheduler.kt`: a new `openNotificationSettings()` next to `openAlarmSettings()`, both no-ops by default.
- `androidApp/src/AndroidReminderScheduler.kt`: the app's notification settings page. `shared/src@ios/IosReminderScheduler.kt`: the app's notification page, and the app's settings page for alarms.
- `shared/src/ui/screens/EntrySheet.kt` and `TodayScreen.kt` / `TodayViewModel.kt`: the opener in `ReminderActions` and a button under the notifications and alarms messages.
- `shared/src/ui/templates/TemplatesViewModel.kt` and `shared/src/App.kt`: the template notices carry an optional settings target, and the snackbar shows the action.
- `shared/src/domain/ReminderScheduler.kt` and `shared/src/App.kt`: a new `onAppResumed()` on the scheduler, a no-op by default, called when the app returns to the front. `shared/src@ios/IosReminderScheduler.kt`: refreshes its permission and submits the last plan again. Android: decided by a test on the emulator (see the design).
- Tests: desktop UI and view model tests with the fake scheduler counting opener calls. No data, dependency or desktop changes. The openers are platform code and need a manual check on an Android phone and an iPhone.
