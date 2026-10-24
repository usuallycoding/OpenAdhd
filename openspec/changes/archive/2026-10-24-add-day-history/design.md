# Design

## Context

`App` swaps the three screens with a plain `when (destination)` and hoists the Templates and Shop `PagerState`s so their tab survives leaving the screen. The Today screen has no tabs: `TodayScreen` shows one `TodayContent` for `TodayViewModel.entries`, which is `store.entriesFor(today)` with pending entries first and completed ones by done time. `planned_tasks` already stores a `date` per entry and nothing deletes old rows, so history needs no new data, only a way to read other days and a way to know which days have rows. The top bar Reorder toggle in `App` follows the screen on show, and `TodayViewModel.setReordering(false)` is called when leaving a list. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Reuse the Today list rendering and ordering so history looks like Today without a second copy of it.
- Make read-only a property of the rendering, not a set of no-op callbacks that could still show controls.
- Keep the calendar's "which days are selectable" rule a plain, unit-testable function.

**Non-Goals:**
- Any change to stored data, the ledger or Today's behavior; summaries or totals per day; persisting the chosen date, tab or collapse state.

## Decisions

- **Add one read, `daysWithEntries`.** `PlannerRepository` gets `daysWithEntries: Flow<Set<LocalDate>>`, backed by `SELECT DISTINCT date FROM planned_tasks` in the DAO (dates are stored as ISO strings, so they parse back with `LocalDate.parse`). `InMemoryStore` derives it from its state. No column, no migration, database stays at version 5. A Flow rather than a one-off read keeps the calendar right if the app stays open across midnight.
- **A separate `HistoryViewModel`, not more state in `TodayViewModel`.** It owns the chosen date, the collapse set and the entries of that day. Entries come from `store.entriesFor(date)` via `flatMapLatest`, then go through the same "pending, then done newest first" ordering as Today; that ordering moves from `TodayViewModel` into a small shared function so both use it. It needs no completion, removal or reorder functions at all, so the read-only guarantee holds at the view-model level too.
- **The chosen date defaults to "yesterday" until the user picks.** The state is a nullable chosen date; when null the effective date is `today - 1 day`, derived from the same ticking `today` flow Today uses. So an app left open over midnight moves the default forward, while an explicit choice stays. Every chosen date is clamped to before today.
- **Previous and next buttons beside the date field, plus the calendar.** Previous moves one calendar day back (it may land on an empty day; the empty state covers it); next is hidden while yesterday is shown. The date field itself is a read-only text field or button that opens the dialog. This gives a quick way to step through consecutive days and a way to jump, and only the dialog has the "days with tasks" rule, as asked.
- **Selectable days are a pure function.** `isSelectableDay(date, today, daysWithEntries) = date < today && date in daysWithEntries`, wrapped in a `SelectableDates` implementation: `isSelectableDate(utcMillis)` converts the UTC midnight millis the picker passes to a `LocalDate` (via `Instant.fromEpochMilliseconds(..).toLocalDateTime(UTC).date`) and calls it; `isSelectableYear` accepts years of days that qualify. The initial selection is the shown day converted back to UTC midnight millis, and the picked millis convert back the same way. UTC is used on both sides because the picker is timezone-free; the device time zone is only used to get `today`.
- **Read-only is a flag on the rendering.** `EntryItem` and `SubtaskItem` get a `readOnly` parameter: checkboxes stay visible but disabled so completed state still shows, and the remove button, move controls and every complete/uncheck callback are not wired. Collapse stays active. `TodayContent` keeps its signature; the History tab builds its list from the same item composables (extracted if needed) in a `LazyColumn` with no FABs, so read-only cannot leak controls through defaults.
- **Tabs like the Shop.** `TodayScreen` becomes a `TabRow` (Today, History) over a `HorizontalPager`, with the `PagerState` hoisted into `App` (`todayPager`) like the other two. This gives swiping, tapping and "remembered while the app runs" without new machinery, and the tab resets on restart.
- **Reorder follows the shown page.** In `App` the Today `ReorderToggle` is only built when `todayPager.currentPage == 0`, and the effect that ends Reorder mode on leaving a list is keyed on that page as well, so switching to History turns it off.
- **Dependencies.** Material3 `DatePicker`, `DatePickerDialog` and `SelectableDates` are part of the Compose version already used; the picker API is experimental, so the use is opted in at the call site.

## Risks / Trade-offs

- [The Material3 `DatePicker` may render or be testable differently on desktop and iOS] → the selectable-day rule and the millis conversion are unit-tested as plain functions; one UI test checks the dialog opens, and the full dialog is checked manually on desktop and a device.
- [A time-zone bug turns the picker's UTC millis into the wrong day] → use UTC for both conversions, test the round trip around midnight, and test with a non-UTC zone.
- [Past entries left pending look unfinished and can never be completed] → accepted; it is what "view only" and the existing "Today is a single local day" rule imply, and it is the honest record of the day.
- [Moving the ordering function out of `TodayViewModel` could change Today] → existing Today view-model tests stay as they are and must keep passing.
- [`daysWithEntries` counts a day whose entries were all removed as having none] → correct, since removed entries no longer exist.

## Migration Plan

None. No stored data changes, so nothing needs a backup. Rolling back is a plain revert.
