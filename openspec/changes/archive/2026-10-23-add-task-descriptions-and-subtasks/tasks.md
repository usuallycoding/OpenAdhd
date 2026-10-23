# Tasks

## 1. Domain model and reward rules

- [x] 1.1 Add `description` and `subtasks` to `TaskTemplate` and `CoinRules.maxSubtasks(tier)` (the tier's pool, capped by an overall ceiling of 10); add `description`, `reward` and `subtasks` to `PlannedTask`; add `PlannedSubtask`; add the `SubtaskCompleted` ledger reason; make `PlannedTask.isCompleted` derived from its subtasks; verify `./kotlin build -m shared` compiles
- [x] 1.2 Add `CoinRules.splitPool` and tests for even, uneven (20 over 3 gives 7,7,6) and pool-equal-to-parts (5 over 5 gives five 1s) splits, every share at least 1 and shares always summing to the pool for every tier and every allowed part count, rejection of zero parts and of more parts than coins, `maxSubtasks` per tier including the ceiling, and the tier pools; verify `./kotlin test -m shared` passes

## 2. Stores and contract tests

- [x] 2.1 Update the repository interfaces and `InMemoryStore`/`StoreLogic`: create/update with description and subtasks (trim, reject blank names, blank subtask titles and more subtasks than the tier allows), assign copying them and freezing rewards via `splitPool`, `completeTask` (entries without subtasks only), `completeSubtask`, and `remove` only when nothing is completed; verify the build compiles
- [x] 2.2 Extend `StoreContractTest` to cover: description and subtasks round trip, blank subtask title rejected, the limit per tier accepted and one more rejected (5/6 for tier 1, 10/11 for tier 4), lowering a template's tier below its subtask count rejected, assign copies description/subtasks and freezes shares after the template is edited, each subtask pays its share once and appends a subtask ledger entry, completing the last subtask completes the entry with the entry's total equal to its pool and nothing extra for the entry itself (including a tier 1 entry with 5 one-coin subtasks), an entry with subtasks cannot be completed directly, an in-progress entry cannot be removed, and the existing cases still hold; verify the in-memory store passes

## 3. Room version 2

- [x] 3.1 Update the entities, DAO and `RoomStore`: new columns, `template_subtasks` and `planned_subtasks` tables with `@Relation` loading sorted by position, atomic idempotent `completeTask`/`completeSubtask` transactions; bump the database to version 2; verify the shared contract suite passes against `RoomStore` on jvm
- [x] 3.2 Add the 1 to 2 `Migration` (columns, tables, `reward` backfilled from tier) and a migration test that builds a version 1 database with raw SQL (templates, a completed and a pending entry, ledger rows), opens it through Room, and verifies the rows, the backfilled rewards and an unchanged balance; verify it passes on jvm
- [x] 3.3 Update the restart test to include a description, subtasks and partial subtask progress, and verify it passes

## 4. UI state

- [x] 4.1 Update `TemplatesViewModel.save` for description and subtasks (validate blank subtask titles and the tier-based subtask limit) and `TodayViewModel` for `completeTask`/`completeSubtask`; verify with view-model tests including partial progress and the removal rule

## 5. Screens

- [x] 5.1 Update the template dialog (scrollable; description field; subtask rows with remove; "Add subtask"; inline error for blank titles; "Add subtask" disabled at the tier's limit and re-enabled when a higher tier is chosen; counter shows n/limit; inline error when saving after lowering the tier below the subtask count) and the template list item (subtask count), and verify with the UI test: create a template with a description and two subtasks and see the count in the list, and the add-subtask control is no longer available at the limit for tier 1 (5) but is available again after switching to tier 2
- [x] 5.2 Update the Today screen (description, indented subtask checkboxes with "+N" shares, "n of m" progress, single checkbox for entries without subtasks, no remove button once anything is done), update previews, and verify with the UI test: completing a subtask raises the balance by its share, the last subtask completes the entry, and no direct completion control exists for an entry with subtasks
- [x] 5.3 Update the README data note if the storage description changed, and verify it still matches the implementation

## 6. Integration

- [x] 6.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 6.2 Verify on desktop: with data created by the previous version present, open the app, confirm templates, entries and balance are intact, then create a task with subtasks, complete some, restart, and confirm progress and balance persist
