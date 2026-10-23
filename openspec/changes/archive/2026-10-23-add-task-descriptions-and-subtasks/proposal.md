# Proposal

## Why

A single checkbox per task is too coarse for big or vague tasks: nothing is rewarded until the whole task is done, and a task like "Clean the kitchen" gives no way to start small. Letting a task carry a description and break into steps, and paying coins for each step, rewards task initiation and progress instead of only the finish line.

## What Changes

- Tasks (templates and Today entries) gain an optional free-text description.
- Tasks gain subtasks: a flat, ordered list of titled steps, limited by the task's coin pool and by an overall ceiling of 10 (at most 5 subtasks for tier 1 and 10 for tiers 2, 3 and 4). Subtasks cannot have subtasks, description, tier or energy of their own.
- **BREAKING** (coin rules): a task's tier now defines a reward pool. A task without subtasks pays the whole pool when completed (unchanged). A task with subtasks pays nothing for itself; its pool is split across its subtasks and paid as each one is completed, with every subtask worth at least one coin. A task with subtasks is completed automatically when its last subtask is done.
- Each subtask's coin share is fixed when the entry is assigned, so later template edits never change an entry's rewards.
- Template editing gets a description field and a subtask editor; Today shows the description and checkable subtasks with their coin shares.
- The local database moves to a new schema version and existing data is kept.

## Capabilities

### New Capabilities

### Modified Capabilities
- `task-templates`: templates get a description and subtasks; create, edit and list requirements change; two requirements are added.
- `day-planner`: entries copy the description and subtasks; completing and removing change; subtask completion and entry details are added.
- `coin-economy`: the tier reward becomes a pool; a split rule across subtasks is added; ledger entries can reference a subtask.
- `local-data-storage`: persistence covers descriptions, subtasks and subtask progress; data from the previous version survives the upgrade.

## Impact

- Domain, in-memory store, Room entities/DAO/store (schema version 2 with a migration), view models, the template dialog and the Today screen in `shared`; tests for each.
- No new libraries.
- Assumptions to confirm later: subtasks have only a title; the overall ceiling of 10 is a first choice; there is no reordering after creation. Because a task can never have more subtasks than its pool, every subtask is worth at least 1 coin and a task always pays exactly its pool.
- Out of scope: Critical Hit and Wall of Awful per-step bonuses, the randomizer, rewards and the shop.
