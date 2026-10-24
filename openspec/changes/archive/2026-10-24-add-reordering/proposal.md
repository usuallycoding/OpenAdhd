# Proposal

## Why

Lists in the app are always in the order things were created, and the user cannot change that. On Today the order is the plan: what to do first matters for someone who struggles to start. In the template libraries it decides what is easy to find and, through the picker, what is offered first. Being able to put things in the order you want makes the lists yours.

## What Changes

- **Reorder mode:** Today, the Tasks tab and the Rewards tab of Templates each get a Reorder toggle in the top bar. While it is on, every movable row shows move up and move down arrows. Turning it off returns to normal rows. Each move is saved immediately; there is no Save or Cancel.
- **Today:** only pending entries can be moved. Completed entries stay below the pending ones, so completing an entry sends it to the bottom group. While Reorder is on, the completion checkboxes, the subtask rows and the remove buttons are hidden so nothing is completed or removed by accident.
- **Templates:** task templates and reward templates can be moved anywhere in their own list. Edit and delete are hidden while Reorder is on.
- **Edges:** the first row cannot move up and the last row cannot move down.
- **New items** go to the end of their list. Today's order belongs to that day only.
- **Where order shows:** the Add to today picker follows the task template order, and the Shop lists rewards in template order within a tier (tier 5 stays sorted by price).
- **Storage:** a `position` is stored for task templates, reward templates and Today entries. The database moves to version 5. Existing items keep their current order. Both stores (in-memory and Room) get the same reorder operation.
- Arrows only: drag handles are out of scope.

## Capabilities

### New Capabilities
- `reordering`: Reorder mode and moving items up and down on Today, the Tasks tab and the Rewards tab, which rows can move, how the order is saved and where it shows.

### Modified Capabilities
- `local-data-storage`: the upgrade requirement also keeps the order of existing items.

## Impact

- `shared` domain and data: reorder functions on the template, reward and planner repositories; `position` columns and a v5 migration backfilled from the current order; list queries sorted by position; `InMemoryStore` equivalent.
- UI: a Reorder toggle and arrow controls on the Today, Templates and Rewards screens; view models gain a reorder mode and move functions.
- Tests: contract tests for both stores, a migration test from version 4, view model tests, and UI tests for the arrows and the hidden controls.
- No new libraries.
- Out of scope: drag handles, reordering subtasks, reordering the Shop directly, and remembering anything across restarts beyond the order itself.
