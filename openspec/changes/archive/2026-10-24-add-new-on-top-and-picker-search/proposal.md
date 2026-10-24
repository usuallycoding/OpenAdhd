# Proposal

## Why

Two small frictions show up when the lists grow. A template you have just created lands at the bottom, so you have to scroll past everything else to find the thing you just made, and the same happens for rewards. And the "Add to today" picker is a plain, long list with no way to find one task except scrolling through all of them.

## What Changes

- **New templates go to the top:** a newly created task template or reward template is placed first in its list instead of last. This shows in the Tasks and Rewards tabs, in the "Add to today" picker (which follows the task template order) and in the Shop, where a new reward is first on its tier's shelf for tiers 1 to 4. Tier 5 stays ordered by price.
- **Today is unchanged:** a task added to today is still placed after the pending entries already on that day.
- **Existing order is kept:** lists that already exist keep their order; only items created from now on go to the top. There is no migration.
- **Search in the picker:** the "Add to today" sheet gets a search field at the top that filters the task templates as you type, by name or description, ignoring case. The field is focused and the keyboard opens as soon as the sheet opens. It has a clear button, shows "No task matches ..." when nothing fits, and starts empty every time the sheet opens.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `reordering`: the requirement "New items go to the end" becomes "New templates go to the top" for task templates and reward templates; the Today scenarios stay.
- `day-planner`: a new requirement for searching the "Add to today" picker.

## Impact

- `shared`: where a new template or reward gets its position, in `OpenAdhdDao` (Room) and `InMemoryStore`; the picker in `TodayScreen`; a small filter function; a clear icon.
- Tests: the migration test that says new items go last now expects them first, and other order tests are updated where needed. New tests cover new-on-top in both stores and in the Shop, and the search, its empty and no-match states, its reset, and focus on open.
- No database change (the version stays at 6) and no new libraries.
- Out of scope: search on the Templates tab or in the Shop, matching subtasks, and remembering the search between openings.
