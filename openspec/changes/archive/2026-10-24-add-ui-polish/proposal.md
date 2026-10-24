# Proposal

## Why

Four small frictions add up in daily use: finished or irrelevant subtasks keep a Today entry long; the long forms sit in a centered dialog that is awkward to reach with a thumb; switching away from Templates or Shop forgets which tab you were on; and tabs can only be changed by tapping. None of them changes what the app does, but all of them change how pleasant it is to use several times a day.

## What Changes

- **Collapse and expand:** tapping a Today entry that has subtasks hides or shows its subtasks. Entries start expanded (as today), a chevron shows the state, and a collapsed entry still shows its name, description and "n of m done". The state is kept while the app runs and is not saved.
- **Bottom sheets:** the template form (create and edit), the reward form (create and edit) and the "Add to today" picker open as bottom sheets instead of centered dialogs. Short decisions (confirmations, the roll reveal, "Pick one for me") stay as dialogs.
- **Remembered tabs:** the Templates and Shop screens each remember their selected tab while the app runs, including when you visit another screen and come back. It resets on restart.
- **Swipe between tabs:** the tabs on Templates (Tasks, Rewards) and Shop (Rewards, History) can be changed by swiping left and right, and the tab row follows the swipe. Tapping a tab still works.
- No data changes and no migration; the database stays at version 4.

## Capabilities

### New Capabilities
- `tabbed-screens`: Swiping and tapping between the tabs of the Templates and Shop screens, and remembering the selected tab while the app runs.
- `form-sheets`: Presenting the app's forms and the "Add to today" picker as bottom sheets, how they are dismissed, and which interactions stay as dialogs.

### Modified Capabilities
- `day-planner`: a Today entry can be collapsed and expanded, and the entry details requirement is now about expanded entries.

## Impact

- UI only, in `shared`: `TodayViewModel` gains a collapsed-entries set; the Today, Templates, Rewards and Shop screens change; a shared sheet wrapper is added; `App` hoists the two tab states.
- Many UI tests that click through dialogs change to sheets, and new tests cover the collapse, the swipe and the remembered tabs.
- No new libraries (`ModalBottomSheet` and `HorizontalPager` are already in Compose 1.12.1) and no database change.
- Out of scope, planned as its own change: reordering of Today entries, task templates and reward templates (needs a position column and a migration). Also out of scope: remembering the tab or the collapsed state across restarts (it would need a small settings store, ideally done once for the theme choice as well).
