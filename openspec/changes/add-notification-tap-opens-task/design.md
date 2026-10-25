# Design

## Context

Android's `ReminderReceiver` builds each notification with `getLaunchIntentForPackage` and a `PendingIntent` using request code 0 and `FLAG_UPDATE_CURRENT`; intent extras do not take part in `PendingIntent` identity, so any two reminders share one target. `MainActivity` has the default launch mode and no `onNewIntent`. On iOS `IosReminderScheduler` sets a notification delegate only to show banners while the app is open, and the notification has no `userInfo`. On Today, "Pick one for me" emits `PickedEntryEvent` on a `SharedFlow` with no replay, and `TodayContent` scrolls to and highlights the entry in a `LaunchedEffect`, looking it up in the entries it has at that moment. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One path from a platform tap to the existing scroll and highlight, working from a cold start.
- Each notification opens its own task on Android.

**Non-Goals:**
- Desktop, notification actions, and changing the highlight look or length.

## Decisions

- **`EntryRequests` holder in `AppContainer`.** It keeps the id of the entry to show in a `StateFlow<String?>` with `open(id)` and `consume(id)`. A plain event flow would drop a tap that arrives before `App` is collecting (a cold start); a state holds it until it is taken. Platforms call `open`; `App` collects, acts and consumes. iOS creates the holder first and passes it to both `IosReminderScheduler` and the container, because the delegate exists before the container does.
- **`App` does the navigation.** On a request it sets the destination to Today, scrolls the Today pager to its first page, ends Reorder mode, calls `TodayViewModel.reveal(id)` and consumes the request. Both `destination` and the pager are state `App` already owns.
- **`reveal` rides the picker's scroll and highlight.** `TodayViewModel` gets a conflated `Channel<String>` exposed as a flow; the Today list merges it with the picked ids. A channel keeps the id until the Today list collects it (it may not be composed yet) and delivers it once, so leaving Today and coming back does not highlight again. A `StateFlow` would replay on every recollection.
- **Waiting for the entry.** At a cold start the entries flow still holds its empty initial value when the id arrives, and the current code gives up when the id is not in the list. For both sources the list now waits up to two seconds for the entry to appear before giving up silently, which also covers a deleted task and yesterday's notification (those never appear, so nothing is highlighted and Today is simply shown).
- **Highlight look.** The highlighted entry reuses the existing `tertiaryContainer` background and 1.5 second duration, and is marked selected for accessibility and tests, as the picker's row is.
- **Android tap target.** `ReminderReceiver` builds an explicit intent for `MainActivity` with `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_SINGLE_TOP`, the entry id as an extra, a data URI containing the id, and the id's hash as the request code, so every entry has its own `PendingIntent`. The activity is declared `singleTop`.
- **Android activity.** `MainActivity` hands the extra to `container.entryRequests` in `onCreate` (only when `savedInstanceState` is null, so a rotation does not repeat it) and in `onNewIntent`, then removes the extra from the intent. Alternative: `singleTask`; rejected, `singleTop` is enough for a notification launching the activity at the top of its own task and keeps the usual back stack.
- **iOS.** The notification content gets `userInfo["entryId"]`, and the delegate implements `didReceiveNotificationResponse`, reading the id and calling `entryRequests.open`. The delegate is set when the scheduler is created, early in start-up, so a tap that launches the app is delivered to it.
- **No desktop.** `DesktopNotifier` is unchanged.

## Risks / Trade-offs

- [On iOS a tap that launches the app can be missed if the delegate is set too late] → The scheduler is created with the container, which `ViewController` builds as soon as the first screen is made; checked by hand on a device with the app killed.
- [Old notifications already posted before the update carry no entry id] → They open the app on Today with nothing highlighted, which is the previous behaviour plus the tab switch.
- [Two seconds of waiting hides a slow database] → The entries flow normally answers within a frame; if it does not, the user still lands on Today.
- [`singleTop` and a task already in the background] → `onNewIntent` handles it; covered by the manual check.

## Migration Plan

No data or schema change. Rolling back means restoring the plain launch intent.
