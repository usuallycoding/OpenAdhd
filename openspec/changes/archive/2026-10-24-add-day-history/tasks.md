# Tasks

## 1. Data

- [x] 1.1 Add `daysWithEntries: Flow<Set<LocalDate>>` to `PlannerRepository`, with a `SELECT DISTINCT date FROM planned_tasks` DAO query in `RoomStore` and a derived flow in `InMemoryStore`; verify with store contract tests that it is empty at first, gains a day when an entry is assigned, keeps a day while any entry remains, and loses it when its last entry is removed
- [x] 1.2 Move the "pending first, then done newest first" ordering out of `TodayViewModel` into one shared function and use it there; verify the existing Today view-model tests still pass unchanged

## 2. History logic

- [x] 2.1 Add the pure selectable-day rule (before today and in `daysWithEntries`) and the `LocalDate` to and from UTC midnight millis conversions, with a `SelectableDates` implementation built on them; verify with unit tests that a past day with tasks is selectable, an empty past day, today and a future day are not, a year without qualifying days is not selectable, and the millis round trip gives the same date in UTC and in a zone far from UTC
- [x] 2.2 Add `HistoryViewModel` with the effective date (yesterday by default, following the clock past midnight, replaced by an explicit choice and clamped to before today), `previousDay`, `nextDay`, `pickDay`, the entries of that day in Today's order, the collapse set with Today's defaults, and `daysWithEntries`; verify with view-model tests for the default, moving back, the next-day limit at yesterday, picking a day, the default moving across midnight, the entries and their order, collapse defaults and toggling, and that nothing in it can change a balance or an entry

## 3. UI

- [x] 3.1 Add a `readOnly` mode to `EntryItem` and `SubtaskItem` (disabled checkboxes that still show state, no remove button, no move controls, collapse still working) and verify with UI tests that no control changes an entry, subtask or the balance
- [x] 3.2 Build the History tab: date field with previous and next day buttons (next hidden at yesterday), the calendar dialog (`DatePickerDialog` with the selectable-days rule, initial selection the shown day), the read-only list without FABs, and the "Nothing was planned on this day" state; verify with UI tests for the default yesterday, stepping back, no next button at yesterday, picking from the dialog, the empty state and the absence of add, pick-one, remove and move controls
- [x] 3.3 Turn `TodayScreen` into a `TabRow` over a `HorizontalPager` (Today, History) with the `PagerState` hoisted into `App`, and make the Reorder toggle show only on the Today page and end Reorder mode when History is opened; verify with UI tests that swiping and tapping switch tabs, the tab is remembered after visiting the Shop, nothing happens beyond the ends, the app starts on Today, and Reorder is not offered on History and is offered again on return

## 4. Integration

- [x] 4.1 Update the existing UI tests that assume Today has no tabs, then run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 4.2 Verify on desktop (and on Android or iOS if you can): complete some tasks, then check yesterday and older days after a date change or with seeded data, step with the previous and next buttons, open the calendar and confirm only past days with tasks can be picked, collapse and expand entries, swipe between the tabs, and confirm Reorder disappears on History
