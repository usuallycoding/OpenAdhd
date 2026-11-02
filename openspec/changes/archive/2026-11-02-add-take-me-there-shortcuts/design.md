# Design

## Context

See proposal.md for why and the specs for behavior. Observed state:

- `App` owns the selected destination and the three tab pagers, and already navigates from outside a screen (a tapped notification sets the destination, scrolls the Today pager and asks the view model to reveal an entry).
- The create forms are opened by private `editing` state inside `TemplatesContent` and `RewardsContent`; nothing outside can open them.
- `HorizontalPager` composes only the page on screen, so when the app jumps to the Rewards tab the Rewards screen does not exist until the scroll finishes.
- `TodayScreen` builds a private `TodayList`, which builds `TodayContent`, which owns the picker dialog (`picking` state). The picker already shows its "no task templates" message.
- Both templates view models are created in `App` and passed to their screens. ViewModel tests live in `shared/test`.

## Goals / Non-Goals

**Goals:**
- One tap from an empty Today picker or an empty Shop to the right create form.
- The same code path for both, with no duplicate navigation logic.

**Non-Goals:**
- Returning the user to where they came from.
- A general navigation or deep-link system.

## Decisions

**A sticky create-request flag on each templates view model.** `TemplatesViewModel` and `RewardTemplatesViewModel` each get `createRequested: StateFlow<Boolean>`, `requestCreate()` and `consumeCreateRequest()`. The content composable opens its form when the flag is true and then consumes it. Because the flag is state and not an event, a request made before the Rewards page is composed is still there when it appears, and consuming it is what keeps the form from opening a second time. *Alternative:* a `SharedFlow` event; rejected because it is lost when nobody is collecting yet, which is exactly the Rewards tab case. *Alternative:* hoisting `editing` into the view models; rejected as a larger change to two screens for no extra behavior. *Alternative:* an `EntryRequests`-style object in `AppContainer`; rejected because `App` already holds both view models.

**`App` does the navigating, screens only get callbacks.** `App` passes `onTakeMeToTaskTemplates` to `TodayScreen` and `onTakeMeToRewardTemplates` to `ShopScreen`. Each sets `destination = Templates`, scrolls the Templates pager (`scrollToPage(0)` or `(1)`, as the notification handler does for Today) and calls `requestCreate()` on the matching view model. Reorder mode needs no extra handling, because changing destination already turns it off. The callbacks default to a no-op so previews and tests that build the content directly keep compiling.

**The callback is threaded through the existing layers.** `TodayScreen` to `TodayList` to `TodayContent` to `TemplatePickerDialog`; `ShopScreen` to `ShopContent` to `ShelvesList`. The picker closes itself before calling the callback.

**Texts.** Today: "Nothing planned for today. Tap + to add a task." The shop: "The shop is empty." and the button "Take me there". The picker keeps its existing sentence and gains the same button.

## Risks / Trade-offs

- [The flag stays set if the form is never composed, so it would open later by surprise] -> Navigation and the request happen together and the target page composes straight away; a unit test checks the flag is consumed once, and the manual check opens the tab by hand afterwards.
- [The pager scroll and the destination change race] -> The form opens from the flag, not from the scroll, so the order does not matter.
- [The picker dialog and the destination change at the same time] -> The picker sets `picking = false` first; the dialog is part of Today's composition and leaves with it.
- [A user who gives up on the form is left on Templates] -> Accepted; the bottom bar is right there.
