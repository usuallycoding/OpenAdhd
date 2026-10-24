# Tasks

## 1. Domain and stores

- [x] 1.1 Add `reorderTemplates(ids)`, `reorderRewards(ids)` and `reorderEntries(date, ids)` to the repository interfaces, with the rule that listed ids come first in the given order and any others keep their relative order after them; implement them in `InMemoryStore` and add them to `StoreContractTest` (swap two items, ids missing from the call keep order, unknown ids are ignored, new items go last, Today order is per date, order survives other updates such as editing or completing); verify the contract passes against `InMemoryStore`
- [x] 1.2 Add `position` columns to the three entities, sort the queries by `position, rowid`, give inserts the next position (per date for planned tasks), implement the three reorder functions in one transaction each in the DAO and `RoomStore`, and verify the contract passes against `RoomStore`
- [x] 1.3 Add `Migration4To5` (three `ADD COLUMN` plus backfill `UPDATE`s from rowid, per date for planned tasks), bump the database to version 5, register it, and verify with a migration test that lists with several rows per table and two dates keep their order and a new item goes last

## 2. View models

- [x] 2.1 Add a reorder mode flag and `moveUp`/`moveDown` to `TemplatesViewModel` and `RewardTemplatesViewModel`, ending the mode when fewer than two items remain, and verify with view-model tests (moves, edges, order saved, mode ends)
- [x] 2.2 Add the reorder mode and pending-only `moveUp`/`moveDown` to `TodayViewModel`, show pending entries before completed ones, and verify with view-model tests (pending moves among themselves, completed stay below with their order, completing sends an entry down, a new entry is last among pending, the collapsed state is kept)

## 3. UI

- [x] 3.1 Add a Reorder toggle to the top bar of Today, the Tasks tab and the Rewards tab (shown only with two or more movable items), show move up and move down arrows on the rows while it is on with the first up and last down disabled, and hide the controls that change data (checkboxes, remove, subtasks on Today; edit, delete and add on Templates) while it is on
- [x] 3.2 Add UI tests for the three screens: toggling shows and hides the arrows, a move changes the displayed order, edge arrows are disabled, the data-changing controls are hidden, the toggle is absent with fewer than two items, and the Shop and the Add to today picker follow the new order with tier 5 still by price

## 4. Wrap-up

- [x] 4.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 4.2 Back up `~/.openadhd`, then verify on desktop: your existing lists open in the same order, reorder each of the three lists, restart and confirm the order was kept, complete a Today entry and see it drop below the pending ones
