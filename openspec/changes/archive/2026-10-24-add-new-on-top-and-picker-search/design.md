# Design

## Context

Each task template, reward template and Today entry has a stored `position`; lists are read `ORDER BY position, rowid`. A new template or reward gets `MAX(position) + 1` in `OpenAdhdDao` (`nextTemplatePosition`, `nextRewardPosition`), so it lands last; `InMemoryStore` appends to its list. Reordering rewrites a whole list as 0..n-1, and the Shop and the "Add to today" picker read the template and reward orders. The picker is `TemplatePickerDialog` in `TodayScreen`: a `FormSheet` whose scrolling body is a plain column of `ListItem`s, with a "Close" footer; `FormSheet` already pads for the keyboard. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A newly created template or reward is first, without touching existing data.
- A picker search that is quick to use: one tap fewer, the keyboard already there.
- The matching rule is a plain function that is unit-tested on its own.

**Non-Goals:**
- Search elsewhere, fuzzy or subtask matching, remembering the search, and any change to how Today orders its entries.

## Decisions

- **New items get `MIN(position) - 1`.** The queries `SELECT COALESCE(MIN(position), 1) - 1 FROM templates` (and the same for rewards) replace the `MAX + 1` ones, and an empty list gets 0. Positions may therefore be negative; every read already sorts by position and then rowid, so nothing else needs to know. The alternative, shifting every row by +1 and inserting at 0, rewrites the whole table on each create and buys nothing. Reordering renumbers a list from 0, so negative values do not accumulate beyond what is created between reorders. Existing rows are untouched, so there is no migration and the database stays at version 6.
- **`InMemoryStore` prepends** (`listOf(new) + list`), which is the same order the Room store produces.
- **Today is left alone.** `nextPlannedPosition` keeps `MAX + 1`: an entry added to today goes after the pending ones, which the spec keeps.
- **Spec change is a rename plus a modification.** "New items go to the end" becomes "New templates go to the top", with the same two Today scenarios, so nothing is lost from the old requirement.
- **Search is a pure function.** `filterTemplates(templates, query)` returns every template for a blank query, otherwise those whose name or description contains the trimmed query, ignoring case, in the given order. It lives in the domain next to `TaskPicker` so it can be tested without UI.
- **The search field lives in the picker's sheet body.** The query is local state of the sheet's content, so it is reset whenever the sheet leaves composition (satisfying "starts empty each time") and nothing outside the picker has to hold it. It is an `OutlinedTextField` with a single line, a "Search tasks" label, and a trailing clear `IconButton` shown only while it holds text. A new clear icon is added with the other icons.
- **Focus and keyboard on open.** A `FocusRequester` on the field is requested from a `LaunchedEffect` once the sheet is composed, and the soft keyboard is shown through `LocalSoftwareKeyboardController`; the sheet already uses `imePadding`, so the list stays reachable. The field is only composed when there are templates, so the request cannot target a missing field.
- **The field scrolls with the list.** `FormSheet` scrolls its whole body, so on a very long list the field scrolls out of view. With the keyboard open and typing narrowing the list this is rarely noticeable, and a sticky header slot in `FormSheet` would touch every form sheet, so it is not added here.
- **No-match message.** "No task matches <query>" replaces the list; the message uses the trimmed text as typed.

## Risks / Trade-offs

- [The keyboard opening on its own covers half the sheet when the user only wanted to tap a task near the top] → a chosen trade-off: the user asked for auto-open; the sheet pads for the keyboard and the keyboard can be dismissed.
- [Requesting focus before the field is attached throws] → the request runs in a `LaunchedEffect` after composition, and a UI test checks the field is focused on open.
- [Soft-keyboard behavior differs on desktop and iOS] → the focus request is the shared part and is tested on desktop; the keyboard itself is checked by hand on a device.
- [Negative positions surprise someone reading the data later] → documented in the DAO next to the query; reordering resets positions to 0..n-1.
- [A new reward in tier 5 does not appear first on its shelf] → tier 5 stays ordered by price, as the spec says; it is first in the Rewards tab.

## Migration Plan

None. No stored data changes and the schema is the same. Rolling back is a plain revert; any items created meanwhile keep their negative positions, which an older version also sorts correctly.
