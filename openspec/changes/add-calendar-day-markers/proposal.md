# Proposal

## Why

The History calendars only let the user tap days that have entries, and the stock Material3 date picker shows every other day dimmed. The user cannot open an empty day, and, because disabled days are all that distinguishes them, there is no room to mark days that do have something. The stock picker has no slot for a per-day marker.

## What Changes

- Replace the stock `DatePicker` in the day calendar dialog with an own month grid (month paging, weekday header, tappable days).
- Every day up to the limit (before today for task History, today or earlier for the Shop history) can be tapped, including days with nothing on them; an empty day opens on the existing empty message.
- Days that have entries (tasks, or redemptions in the Shop) show a small dot under the number, also on the selected day.
- Future days (and today in the task History) stay not selectable.
- Picking still takes effect on OK, and Cancel keeps the shown day.

## Capabilities

### New Capabilities

### Modified Capabilities
- `day-history`: "Only days with tasks can be picked" becomes "Which days can be picked": any earlier day is selectable and days with tasks are marked.
- `reward-shop`: "Only days with redemptions can be picked" becomes the same rule for the redemption history, with today included.

## Impact

- `shared/src/ui/components/DayNavigator.kt`: the dialog body swaps `DatePicker` for the new grid; used by `HistoryScreen` and `ShopScreen`.
- `shared/src/domain/HistoryDays.kt` and `shared/src/ui/history/SelectableHistoryDays.kt`: the selection rule no longer depends on `daysWithEntries`; the set only drives the markers. `SelectableHistoryDays` and `isSelectableYear` are no longer needed.
- Tests: `HistoryDaysTest`, `HistoryUiTest`, `RewardHistoryUiTest`.
- No data, schema or dependency changes.
