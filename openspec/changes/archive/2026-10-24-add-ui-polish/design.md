# Design

## Context

`App` owns the bottom navigation and swaps the three screens with a plain `when (destination)`, so a screen leaves composition when you leave it. The Templates and Shop screens keep their selected tab in a local `rememberSaveable`, which is lost with the screen; that is why the tab resets today. The template form, reward form and "Add to today" picker are `AlertDialog`s; the redeem and roll confirmations, the roll reveal and the "Pick one for me" suggestion are also `AlertDialog`s. `EntryItem` always renders an entry's subtasks. Compose is 1.12.1, which already provides `ModalBottomSheet` (Material3) and `HorizontalPager` (foundation). See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Four UI behaviors with no change to stored data or to the domain.
- One shared sheet wrapper so the three sheets look and behave alike.
- A UI test suite that still reads like the behavior it checks.

**Non-Goals:**
- Reordering (its own change), persisting the tab or collapse state across restarts, bottom sheets for confirmations or reveals, and any change to what the forms contain or validate.

## Decisions

- **Hoist the pager state, not just an index.** `App` never leaves composition, so it creates one `PagerState` for Templates and one for Shop (`rememberPagerState { 2 }`) and passes them down, with a default for previews. The screen shows `HorizontalPager(state)`; the `TabRow` selects `state.currentPage`; tapping a tab calls `animateScrollToPage`. One piece of state therefore serves "remembered while the app runs" and "swipe", and the page the user was on is also what they return to. `rememberPagerState` is saveable, so it also survives rotation, but it is not persisted across restarts, as agreed.
- **Collapsed entries live in the view model.** `TodayViewModel` exposes `collapsedEntryIds: StateFlow<Set<String>>` and `toggleCollapsed(entryId)`. It survives leaving and returning to Today because the view model does, and resets on restart. Entries start expanded because an id is only in the set once toggled. Ids of removed entries are harmless leftovers.
- **Collapse UI.** For an entry with subtasks the header `ListItem` becomes clickable, a chevron takes the (otherwise empty) leading slot, and the subtask rows sit in an `AnimatedVisibility`. The header exposes expanded/collapsed to accessibility through a state description. The supporting text already carries "n of m done", so a collapsed entry keeps its progress without new UI. Entries without subtasks are not clickable, so their checkbox is unaffected.
- **One `FormSheet` wrapper for the three sheets.** It wraps `ModalBottomSheet` with `skipPartiallyExpanded = true`, applies `imePadding()`, puts the fields in a scrollable column, and keeps a fixed footer (Cancel and Save, or Close for the picker) outside the scroll area so the actions never scroll away. Dismissal by swipe, outside tap and Cancel all route to the same `onDismiss` that discards. A successful save hides the sheet through `sheetState.hide()` before removing it so it slides out.
- **The forms keep their logic.** `TemplateDialog` and `RewardDialog` become `TemplateForm`/`RewardForm` content hosted in `FormSheet`; validation, inline errors and the subtask editor are unchanged. The "Add to today" picker becomes a list in a sheet where tapping a template adds it and closes.
- **Confirmations stay dialogs.** `ConfirmRedeemDialog`, `ConfirmRollDialog`, `RollRevealDialog` and `RandomizerDialog` are untouched.
- **Spike first for the sheets.** Before converting the forms, the wrapper is built and a UI test proves that Compose UI tests can find and operate nodes inside a `ModalBottomSheet` on the desktop test host (clicks, text input, `performScrollTo`). If they cannot, the test approach changes before the work spreads.
- **Tests.** Tests that click through a dialog are updated to the sheet; new tests cover collapse, swipe (`swipeLeft`/`swipeRight` on the pager), tab remembering across Today and back, and sheet dismissal by Cancel. Existing wording of expectations is kept where possible.

## Risks / Trade-offs

- [Compose UI tests may behave differently inside a bottom sheet (animation, separate window)] → the spike task proves it before the conversion; sheets use `skipPartiallyExpanded` to avoid the half-open state.
- [Mouse-drag swiping may not work on desktop, where Compose scrolls by wheel and trackpad rather than drag] → mobile is the primary target; on desktop the tabs remain tappable and a trackpad swipe is checked manually; the limitation is noted rather than worked around.
- [The on-screen keyboard can cover fields in a sheet on Android and iOS] → `imePadding()` and a scrollable body; checked manually on a device or emulator.
- [AnimatedVisibility makes "subtasks are hidden" asynchronous in tests] → tests assert after the animation settles (the test clock idles), as for other animated UI.
- [Hoisting the pager state couples `App` to the two screens] → the state is a plain parameter with a default, so previews and tests are unaffected.

## Migration Plan

None. No stored data changes, and nothing needs a backup. Rolling back is a plain revert.

## Open Questions

- Whether the tab and collapsed state should survive a restart can be decided with the theme choice in one future "remember my preferences" change; it would add a small settings store, not change this design.
