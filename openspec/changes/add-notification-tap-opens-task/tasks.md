# Tasks

## 1. Showing an entry in the app

- [x] 1.1 Add `EntryRequests` (open, consume, a state flow of the pending id) and an `entryRequests` property on `AppContainer` and `createContainer` (verify: unit tests that a request made before anyone collects is still there afterwards, that consuming clears only the same id, and that a newer request replaces an older one)
- [x] 1.2 Add `TodayViewModel.reveal(id)` backed by a conflated channel, merge it with the picked ids in the Today list, make the list wait up to two seconds for the entry to appear before giving up, and mark the highlighted entry as selected (verify: view model and UI tests that a reveal made before the list is shown is still scrolled to and highlighted, that a second visit does not highlight again, that an unknown id does nothing, and that the existing "Pick one for me" tests still pass)
- [x] 1.3 Collect `container.entryRequests` in `App`: switch to Today, scroll the Today pager to its first page, end Reorder mode, reveal the entry and consume the request (verify: UI tests starting from History, from the Shop and with Reorder mode on, ending on Today with the entry highlighted, for a completed entry, and for an entry that does not exist leaving Today shown with nothing highlighted)

## 2. Android

- [x] 2.1 Declare `MainActivity` `singleTop`; build each notification's tap intent for `MainActivity` with the entry id as an extra, a per-entry data URI and request code and the single-top and new-task flags; handle the extra in `onCreate` (not on recreation) and `onNewIntent` by calling `entryRequests.open`, then removing it (verify: the Android build succeeds, and a unit test on the helper that builds the intent and request code shows two entries get different targets; the tap itself is checked by hand in 4.2)

## 3. iOS

- [x] 3.1 Put the entry id in each notification's `userInfo`, handle `didReceiveNotificationResponse` in the delegate by calling `entryRequests.open`, and create the holder once and pass it to both `IosReminderScheduler` and the container (verify: the iOS native build compiles; checked by hand in 4.3)

## 4. Docs and verification

- [x] 4.1 Add a line to the Reminders section of `README.md` saying that tapping a notification on Android and iOS opens the task on Today, and that desktop only opens the app (verify: the steps work as written in 4.2 and 4.3)
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for all targets, then on an Android phone tap a reminder with the app closed, in the background, on History and with two reminders showing (verify: tests pass and each tap lands on its own task, highlighted; manual)
- [x] 4.3 On iOS tap a reminder with the app killed, in the background and in front (verify: manual)
