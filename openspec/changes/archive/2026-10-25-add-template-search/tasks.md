# Tasks

## 1. Matching and view models

- [x] 1.1 Add `matchingRewards(query)` for reward templates with the same rule as `matching(query)` (verify: unit tests for name, description, case, outer spaces, blank query listing all in order, and no match)
- [x] 1.2 Give `TemplatesViewModel` and `RewardTemplatesViewModel` a `query` state, `setQuery`, a `visible` list, `canReorder` and `reordering` that require a blank query, Reorder turned off when text is typed, and the query cleared when a new item is saved (verify: view model tests for filtering, the query surviving, a new item clearing it, an edit and a delete keeping it, the toggle unavailable while searching, typing ending Reorder, and clearing not bringing it back)

## 2. The lists

- [x] 2.1 Add the search field with a clear control and the no-match message to the Tasks tab, shown only when the tab has items and not focused (verify: UI tests for filtering by name and description, the clear control, the "No task matches xyz" message, no field and the old message with no templates, no focus on opening, the filter kept after swiping to Rewards and back, a new template clearing it, and the Reorder toggle hidden while searching)
- [x] 2.2 Add the same to the Rewards tab (verify: the matching UI tests for rewards, with "No reward matches xyz")

- [x] 2.3 Put the keyboard away on both tabs: the Done key on `SearchField`, and tapping or scrolling the list area clears focus and hides the keyboard, without a tap on the field itself doing so (verify: UI tests that a tap on a row, a tap on empty space, a scroll and the Done key each leave the field unfocused with its text kept, and that tapping the field focuses it)

## 3. Docs and verification

- [x] 3.1 Add a line to `README.md` about searching the Tasks and Rewards tabs and that Reorder is unavailable while searching (verify: the steps work as written in 3.3)
- [x] 3.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 3.3 On a phone: search the Tasks and Rewards tabs, swipe between them, create a template while filtered, and try Reorder with and without search text (verify: manual)
