    # Tasks

## 1. New templates go to the top

- [x] 1.1 Give a new task template or reward template the position `MIN(position) - 1` (0 for an empty list) in the Room store and put it first in `InMemoryStore`, leaving the position of Today entries as it is; verify with contract tests run against both stores that a new task template and a new reward are first, that the others keep their order, that a new one is still first after the list was reordered and after another was created, that editing keeps the place, and that a task added to today is still last among the pending entries
- [x] 1.2 Update the tests that expect new items last (the version 4 migration test and any order tests that fail) and add tests that the Tasks and Rewards tabs, the "Add to today" picker and the Shop shelf of a tier 1 to 4 show a newly created item first while a tier 5 shelf stays ordered by price; verify the full suite passes

## 2. Picker search

- [x] 2.1 Add the template filter as a pure function (blank query lists all; otherwise name or description contains the trimmed text, ignoring case, in the given order); verify with unit tests for name, description, case, surrounding spaces, order, a blank query and no match
- [x] 2.2 Add the search field to the Add to today picker with a clear icon and button, filtering live through that function, "No task matches ..." when nothing fits, no field when there are no templates, an empty field each time the sheet opens, and focus plus the keyboard on open; verify with UI tests for typing to filter by name and description, the clear control appearing and emptying the field, the no-match message, tapping a result adding it and closing, the field being empty after closing and reopening, the field being focused on open, and no field when there are no templates

## 3. Integration

- [x] 3.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 3.2 Verify on desktop (and on Android or iOS if you can): create a task template and a reward and see each first in its list, in the picker and in the Shop, open the Add to today picker and confirm the keyboard comes up and typing filters, clear the search, close and reopen to confirm it resets, and add a task from the results
