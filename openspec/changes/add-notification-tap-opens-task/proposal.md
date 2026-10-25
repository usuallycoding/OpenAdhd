# Proposal

## Why

A reminder tells you a task is still to do, but tapping it only opens the app on whatever screen it was last on, so you have to find the task yourself. The task that triggered the notification is exactly where the user wants to land.

## What Changes

- **Tap opens the task:** tapping a reminder notification on Android or iOS opens the app on the Today tab for today, scrolls the list to the task that triggered it and highlights it briefly, in the same way "Pick one for me" does.
- **From anywhere:** from the History tab or another screen it switches to Today, and Reorder mode is turned off so the normal list is shown.
- **Every launch path:** it works whether the app was closed, in the background or in front.
- **Several notifications:** each notification opens its own task.
- **Edge cases:** a task that is already done is still scrolled to; a task that no longer exists, or a notification from an earlier day, just opens Today with nothing highlighted.
- **Android fixes needed for this:** each notification gets its own tap target (today every reminder shares one, so the tapped task could be the wrong one), and the activity reuses the running instance instead of opening another.
- **Not in this change:** the desktop app (a tray balloon click is not reliably delivered on macOS), buttons on the notification, and changes to when reminders fire.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `task-reminders`: tapping a notification opens Today and shows the task that triggered it, on Android and iOS.

## Impact

- `shared`: a small holder that platforms use to ask the app to show an entry, kept until the app can act on it; `TodayViewModel` reveals an entry through the same scroll and highlight the picker uses; `App` switches to Today and ends Reorder mode; the Today list waits briefly for the entry to appear.
- `androidApp`: the manifest, the notification's tap intent and `MainActivity` (new intents and cold start).
- `shared/src@ios`: the notification carries the entry id and the delegate reports the tap.
- Tests: the holder and the reveal in `shared`, and UI tests that a request made before or after the app shows leads to the entry being scrolled to and highlighted. The Android and iOS tap paths are checked by hand.
- No storage or backup change and no new libraries.
