# Tasks

## 1. Collapse and expand

- [x] 1.1 Add `collapsedEntryIds` and `toggleCollapsed` to `TodayViewModel`, and verify with view-model tests that entries start expanded, a toggle collapses and a second toggle expands, state is per entry, and it is kept while the view model lives
- [x] 1.2 Make an entry with subtasks tappable on its header with a chevron in the leading slot and the subtask rows in an `AnimatedVisibility`, with an accessible expanded/collapsed state, and leave entries without subtasks untouched; verify with UI tests that tapping hides the subtasks while the name, description and "0 of 3 done" stay, tapping again shows them, an entry without subtasks ignores the tap and still completes with its checkbox, and a collapsed entry is still collapsed after visiting the Shop and returning

## 2. Tabs

- [x] 2.1 Hoist a `PagerState` for Templates and a `PagerState` for Shop into `App`, pass them to the screens (with defaults for previews), and replace the `if (tab == 0)` switches with a `HorizontalPager` whose `TabRow` follows `currentPage` and whose tab taps call `animateScrollToPage`; verify the build compiles on all targets
- [x] 2.2 Add UI tests for: swiping left and right changes the tab on Templates and on Shop and the selected tab follows, swiping at the ends changes nothing, tapping a tab shows it, and each screen remembers its tab after going to another screen and back without affecting the other; update the existing tests that switch tabs; verify they pass

## 3. Bottom sheets

- [x] 3.1 Build the shared `FormSheet` wrapper (`ModalBottomSheet` with `skipPartiallyExpanded`, `imePadding`, a scrollable body, a fixed footer, one dismissal path) and a spike UI test proving nodes inside the sheet can be clicked, typed into and scrolled on the desktop test host; verify the spike passes before converting anything else
- [x] 3.2 Convert the task template form (create and edit) and the reward form (create and edit) to `FormSheet`, keeping validation and the subtask editor unchanged; update the existing UI tests that use these forms; verify they pass, including a swipe-down or Cancel discarding input and an invalid save keeping the sheet open
- [x] 3.3 Convert the "Add to today" picker to a sheet where tapping a template adds it and closes, leave the confirmations, the roll reveal and the randomizer as dialogs, update the affected tests, and add one UI test for a tall form (ten subtasks) keeping Save reachable; verify they pass

## 4. Integration

- [x] 4.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 4.2 Verify on desktop (and on Android or iOS if you can, for swiping and the keyboard): collapse and expand entries, swipe or trackpad-swipe the tabs, leave and return to confirm the tabs and collapse state are remembered, and open each sheet, dismiss it by swiping down and by Cancel, and save from it
