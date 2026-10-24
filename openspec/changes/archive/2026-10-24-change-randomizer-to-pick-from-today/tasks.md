# Tasks

## 1. Icon

- [x] 1.1 Change `MoreIcon` in `AppIcons.kt` to three vertical dots and verify the Today row options button still opens the sheet (`AppUiTest`) and renders vertically in a preview or the running app

## 2. Picking logic

- [x] 2.1 Make `TaskPicker` pick among today's pending entries by energy with the 4/2/1 weights, and update its unit tests for energy limits, weights, completed entries excluded and no draw when nothing is eligible
- [x] 2.2 Replace the randomizer states in `TodayViewModel` with `Closed`, `ChoosingEnergy`, `NothingFits` and `NothingPending`; remove `shown`, `another` and `acceptSuggestion`; emit a picked-entry event on `chooseEnergy`; rewrite `RandomizerViewModelTest` for open, nothing pending, nothing fits, pick event, re-pick, choose another energy and dismiss, and verify it passes

## 3. Today screen

- [x] 3.1 Hoist the `LazyListState`, collect the picked-entry event to scroll to the entry and highlight it for about 1.5 s, and add the highlight parameter to `EntryItem`; verify with a `AppUiTest` that the picked row is shown and the dialog closes with the list unchanged
- [x] 3.2 Replace the suggestion, no-templates dialogs with the energy question, the "nothing fits" and "nothing left to pick today" messages; remove the "Pick one for me" button from the empty state; update the `AppUiTest` randomizer section and the empty-state test, and verify all UI tests pass

## 4. Spec check

- [x] 4.1 Run `openspec validate change-randomizer-to-pick-from-today --strict` and the full test suite, and verify both pass
