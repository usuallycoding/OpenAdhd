# Proposal

## Why

Once a day is over, the app gives no way to look back at it: the Today list rolls over at midnight and the earlier entries are only kept in the database. Seeing what was planned and done on a past day (to review a week, or just to remember what yesterday held) is a natural next step, and the data is already stored.

## What Changes

- **History tab:** the Today screen gets two tabs, Today and History, changed by tapping or swiping like the Shop tabs. Today is the first tab and the app starts on it.
- **Date field:** the History tab opens on yesterday. A date field shows the chosen day, and the user can change it with previous and next day buttons or from a calendar dialog.
- **Calendar dialog:** only days before today that have planned tasks can be selected; today, future days and empty days are greyed out.
- **Read-only day list:** under the date field, the entries of the chosen day look like the Today list (name, description, tier and energy, progress, done time, subtasks with their coin share) but nothing can be checked, unchecked, removed or reordered, and there are no add or "Pick one for me" buttons. Entries with subtasks can still be collapsed and expanded. A day with no entries shows "Nothing was planned on this day".
- **Reorder button:** the top bar Reorder toggle shows only on the Today tab.
- No schema change and no migration; the database stays at version 5. One new read-only query lists the days that have tasks.

## Capabilities

### New Capabilities
- `day-history`: Viewing the entries of an earlier day read-only, choosing the day, and which days can be chosen.

### Modified Capabilities
- `tabbed-screens`: the Today screen joins Templates and Shop as a screen with swipeable, remembered tabs.

## Impact

- `shared`: `PlannerRepository` gains a `daysWithEntries` flow, implemented in `InMemoryStore`, `RoomStore` and the DAO (`SELECT DISTINCT date FROM planned_tasks`); `TodayScreen` gets the tab row and pager, and `EntryItem` and `SubtaskItem` get a read-only mode; a new `HistoryViewModel` (or equivalent) holds the chosen date and collapse state; `App` hoists a third pager state and hides the Reorder toggle on the History tab.
- Uses the Material3 `DatePicker` with `SelectableDates` (experimental API, already part of the Compose version in use); no new libraries.
- Existing Today behavior and the "Today is a single local day" requirement are unchanged.
- Out of scope: editing or completing past days, totals or coins-per-day summaries, search, and saving the chosen date or tab across restarts.
