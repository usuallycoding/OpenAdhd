# Design

## Context

The previous slice (see `openspec/specs/`) stores `TaskTemplate(id, name, tier, energy)` and `PlannedTask(id, date, name, tier, energy, templateId?, completedAt?)`. Completing an entry atomically marks it and appends one ledger entry worth `CoinRules.baseReward(tier)`, computed at completion time. Persistence is Room KMP (`OpenAdhdDatabase`, version 1, `exportSchema = false`, tables `templates`, `planned_tasks`, `ledger`) behind the `OpenAdhdStore` interfaces, with an in-memory implementation and a shared contract test suite. See proposal.md for the motivation and scope.

## Goals / Non-Goals

**Goals:**
- Descriptions and a flat subtask list on templates, copied onto entries at assignment.
- Pool-split rewards paid per subtask, with amounts frozen on the entry.
- A safe Room migration from version 1.
- A bounded economy and short lists: a task never has more subtasks than its coin pool or than 10, so every subtask is worth at least 1 coin and a task always pays exactly its pool.

**Non-Goals:**
- Subtasks with their own description, tier, energy or children; reordering subtasks after creation.
- Critical Hit / Wall of Awful bonuses, randomizer, shop.
- Un-completing anything (the ledger stays append-only).

## Decisions

- **A subtask is a plain value, not a task.** `TemplateSubtask` is just a title (a `List<String>` on the template is enough: identity is its position). `PlannedSubtask(id, title, reward, completedAt?)` lives on the entry. Because there is no recursive type, "subtasks cannot have subtasks" holds by construction and needs no depth check.
- **Rewards are snapshotted on the entry.** `PlannedTask` gains `description`, `reward` (the pool for its tier at assignment) and `subtasks`. Each `PlannedSubtask.reward` is its share. Completing awards the stored amount instead of recomputing from the tier, so later changes to the tier table or the template never alter an existing entry. This is what the "fixed at assignment" requirement needs.
- **Pool split as a pure function.** `CoinRules.splitPool(pool, parts): List<Int>` requires `1 <= parts <= pool` and gives each part `pool / parts` rounded down, with one extra coin for each of the first `pool % parts` parts, so the shares always add up exactly to the pool and each is at least 1. It is unit-tested for even, uneven and pool-equal-to-parts splits, for the largest split per tier, and for rejecting more parts than coins.
- **Derived completion.** An entry with subtasks has no completion state of its own: `isCompleted` is "has subtasks and all are completed", or "completedAt != null" when it has none. No separate flag is stored, so it cannot disagree with its subtasks.
- **Two explicit, idempotent operations instead of one `complete`.** `completeTask(entryId)` is valid only for entries without subtasks and `completeSubtask(subtaskId)` for subtasks; each marks the item and appends one ledger entry in a single transaction and returns false if already completed, unknown, or (for `completeTask`) the entry has subtasks. The ledger reason set grows with `SubtaskCompleted`; `refId` points at the subtask id.
- **Subtask limit follows the tier, with an overall ceiling.** `CoinRules.maxSubtasks(tier)` is the smaller of the tier's pool and `SubtaskCeiling = 10`, so 5 for tier 1 and 10 for tiers 2, 3 and 4. It is enforced where the data is written: `create` and `update` reject more subtasks than the template's tier allows with `IllegalArgumentException`, in both stores, which also covers lowering a template's tier below its current subtask count. The template dialog recomputes the limit whenever the tier chip changes: "Add subtask" is disabled at the limit, the counter reads `n/limit`, and saving after lowering the tier below the current count shows an inline error naming the limit. Existing data has no subtasks, so the migration is unaffected.
- **Removal rule.** `remove(entryId)` only deletes an entry none of whose parts is completed, so removing can never orphan earned coins.
- **Room schema version 2, new tables for subtasks.**
  - `templates` gets `description TEXT NOT NULL DEFAULT ''`; a new `template_subtasks(id, templateId, position, title)`.
  - `planned_tasks` gets `description TEXT NOT NULL DEFAULT ''` and `reward INTEGER NOT NULL`; a new `planned_subtasks(id, plannedTaskId, position, title, reward, completedAtMillis)`.
  - Rows are loaded with `@Relation` and sorted by `position` in the mapper, because relation queries do not guarantee order.
  - Real tables (not a JSON column) let `completeSubtask` be a single atomic `UPDATE ... WHERE id = ? AND completedAtMillis IS NULL` whose affected-row count gives idempotence, matching how entries are completed today.
- **Migration 1 to 2** is written as SQL in a Room `Migration`: add the columns, create the two tables, and backfill `planned_tasks.reward` from the tier (1 to 5, 2 to 10, 3 to 20, 4 to 40) so that old entries keep their original value. Existing completed entries stay completed (no subtasks, `completedAtMillis` set) and ledger rows are untouched, so the balance is unchanged. Because `exportSchema` is off, the migration is tested by creating a version 1 database with raw SQL through the bundled driver, opening it through Room, and asserting the data.
- **Store API.** `create(name, description, tier, energy, subtasks)` and `update(template)` trim the description and subtask titles and reject blank names and blank subtask titles with `IllegalArgumentException`, like the existing name check. The in-memory store and `StoreLogic` implement the same rules, and the shared contract suite is extended so both stores are held to them.
- **UI.**
  - The template dialog (made scrollable) gains a multi-line description field and a subtask list: one text row per subtask with a remove button, plus an "Add subtask" button. Empty-title rows block saving with an inline error.
  - The Today entry shows the description under the name, then its subtasks as indented checkboxes with their "+N" share and an "n of m" progress; an entry without subtasks keeps its single checkbox. The remove button is hidden once anything is completed.
  - Template list items show the subtask count.
- **View models** change minimally: `TemplatesViewModel.save` takes description and subtasks; `TodayViewModel` exposes `completeTask` and `completeSubtask`.

## Risks / Trade-offs

- [Migration bug corrupts or loses user data] → test the 1 to 2 migration against a hand-built v1 database that includes a completed entry and ledger rows, and assert balance and rows afterwards.
- [The ceiling of 10 means a tier 4 task (40 coins) has at most 10 subtasks of 4 coins each, so its steps are coarser than its pool could allow] → intended; short lists are the goal, and the ceiling is one constant to change.
- [Reward stored on the entry diverges from `CoinRules` after a future tuning] → intended; history is stable. New entries use the new table.
- [A user lowers a template's tier below its subtask count] → the save is rejected with a clear message naming the limit, so the user removes subtasks or keeps the higher tier; existing entries are unaffected because they keep their frozen subtasks.
- [Changing `complete` into two operations ripples through the view models, contract tests and UI test] → covered by tasks; compiler errors guide the call sites.

## Migration Plan

1. Ship database version 2 with the migration; no data export or manual step.
2. If the migration fails on open, the app should fail loudly in tests rather than silently recreating the database (no destructive-migration fallback).
3. Rollback is not supported for installed data (downgrading would not understand version 2), which is acceptable for an unreleased app.

