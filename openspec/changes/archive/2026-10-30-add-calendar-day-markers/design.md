# Design

## Context

`DayNavigator` opens `DayPickerDialog`, a Material3 `DatePickerDialog` around `DatePicker`. The only per-day hook is `SelectableDates` (`SelectableHistoryDays`, backed by `HistoryDays.isSelectable`), which merely dims disabled days. There is no slot to draw on a day, so a marker cannot be added to the stock picker. The dialog is shared by the task History (`includeToday = false`) and the Shop history (`includeToday = true`), and the set of marked days already reaches it (`daysWithEntries`). Days travel as `LocalDate`; `HistoryDays.toPickerMillis`/`fromPickerMillis` exist only for the stock picker.

## Goals / Non-Goals

**Goals:**
- One calendar in shared Compose code, identical on Android, desktop and iOS.
- Keep the dialog's flow: select a day, OK to confirm, Cancel to keep the shown day.

**Non-Goals:**
- No year picker, text-input mode, range selection or swipe gestures between months.
- No locale-dependent first weekday; weeks start on Monday.

## Decisions

- **Own month grid instead of a hack on `DatePicker`.** A `DayCalendar` composable shows a header (previous month, "October 2026", next month), a Mon-Sun row and a 7-column grid built from `LocalDate` and kotlinx-datetime. Alternatives: keeping the stock picker with every past day enabled (no way to tell days apart, so it fails the request) and overlaying markers on the stock picker (its layout is not exposed, so it would break on library updates).
- **Selection rule moves to `HistoryDays`.** `isSelectable(day, today, includeToday)` becomes `day < today || (includeToday && day == today)`. The `daysWithEntries` set is only used for markers. `isSelectableYear` and `SelectableHistoryDays` are removed. The picker-millis conversions stay because the Templates schedule date picker still uses them.
- **Marker is a dot under the number**, drawn in the primary colour, or in the on-primary colour on the selected day's filled circle, so it stays visible. Non-selectable days (future, and today in task History) are dimmed and not clickable. The dialog opens with the shown day selected.
- **Month limits.** The grid opens on the month of the shown day. Paging forward stops at today's month; paging back has no limit, so the user can reach days with no data.
- **Accessibility.** Each day cell has a content description such as "20 October 2026, has tasks" (or "has redemptions" for the Shop, via a label parameter), and the paging buttons are labelled.

## Risks / Trade-offs

- [No platform date-picker niceties such as locale first weekday] → content descriptions on cells and a fixed Monday start, noted as a non-goal.
- [Unbounded paging back through years is tedious] → acceptable for a personal app; the previous/next day arrows still exist, and a year jump can be a later change.
- [Dialog height changes between 5-row and 6-row months] → always reserve six rows so the dialog does not jump.
