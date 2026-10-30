# Tasks

## 1. Selection rule

- [x] 1.1 Change `HistoryDays.isSelectable` to depend only on `day`, `today` and `includeToday`, and remove `isSelectableYear` and `SelectableHistoryDays` (the picker-millis helpers stay: the Templates schedule picker uses them) (verify: updated `HistoryDaysTest` covers an empty past day selectable, today only with `includeToday`, and future days not selectable)
- [x] 1.2 Add a pure helper for a month's grid cells, Monday first, six rows (verify: unit tests for a month starting on Monday, one starting on Sunday, and February in a leap year)

## 2. The calendar

- [x] 2.1 Build `DayCalendar` in `shared/src/ui/components`: month header with previous and next buttons (next stops at today's month), weekday row, tappable day cells, selected-day highlight, a dot under days in the marked set (also visible on the selected day), dimmed non-selectable days, and content descriptions (verify: UI tests in `HistoryUiTest` for the dot on a day with tasks and none on an empty one, an empty day being tappable, today and future days not tappable, paging months)
- [x] 2.2 Swap `DayPickerDialog` in `DayNavigator` to use `DayCalendar`, keeping OK to confirm and Cancel to keep the shown day, with a label per screen for the content description (verify: UI tests that picking an empty day shows "Nothing was planned on this day", and cancelling keeps the shown day)
- [x] 2.3 Cover the Shop history (verify: `RewardHistoryUiTest` shows a dot on a redemption day, today is selectable with and without a redemption, an empty past day shows "Nothing redeemed on this day", and future days are not tappable)

## 3. Docs and verification

- [x] 3.1 Add a "Browsing past days" section to `README.md` (it had no History section) to say any earlier day can be opened and days with entries have a dot (verify: matches the behaviour in 3.3)
- [x] 3.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [ ] 3.3 On a phone and on desktop: open both calendars, check the dots, tap an empty day, page months, and confirm future days cannot be tapped (verify: manual, in light and dark themes)
