# Design

## Context

Completing a task or subtask sets `completedAt` on its row and appends a ledger entry (plus a bonus entry on a critical hit) as one operation; both stores share `StoreLogic` for the in-memory side and a DAO transaction for Room. The ledger is append-only by spec, its reason is stored as text, and the balance is the sum of all amounts. `PlannedTask.isCompleted` is derived (own `completedAt`, or all subtasks done). Today lists pending entries first (a stable sort by `isCompleted` over stored position), and a spend is refused when the balance is below the price. The checkboxes of completed items are disabled.

## Goals / Non-Goals

**Goals**
- Uncheck a completed task or subtask on Today, returning exactly what it paid.
- Keep the ledger append-only and the balance a plain sum, which may be negative.
- List done entries newest first and show when they were done.

**Non-Goals**
- Undoing anything from an earlier day, a ledger screen, or a confirmation step.
- Loans: relaxing the spending guard for a negative balance stays a separate idea.
- Any database change.

## Decisions

1. **Undo is a reversal entry, not a deletion.** A new `LedgerReason.Undone`. Unchecking appends one entry of minus the net total already recorded for the item's id, where the net total is the sum of all ledger amounts whose `refId` is that task or subtask id (reward, critical-hit bonus and earlier undos all share it). Because earlier undos are in the sum, a second round reverses only what the second check paid. A net of zero adds nothing.
2. **Reverse the bonus too.** Taking back only the base reward would let check, uncheck, check re-roll the 1-in-8 critical hit for free. With the full net reversed, a loop always returns to the starting balance, and the spec has a scenario for it.
3. **Store operations.** `uncompleteTask(id)` and `uncompleteSubtask(id)` on the planner repository, each returning the coins taken back or null when the item is unknown or still pending (changing nothing). Each is one transaction: clear `completedAt` / `completedAtMillis` with a guarded update (`WHERE ... IS NOT NULL`) so a repeated call is a no-op, then sum the ledger by `refId` and insert the reversal. Room gets a `SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE refId = :id` query; `InMemoryStore` does the same sum in `StoreLogic`. An uncheck of a task that has subtasks is refused, like completing one.
4. **Negative balance needs no code in the balance.** `CoinRules.balance` is already a sum, and `redeem`/`mysteryRoll` already compare it with the price, so they refuse while it is negative. The shop's "coins needed" is `price - balance`, which just gets larger. Only the display and the spec wording change.
5. **Done time is derived.** `PlannedTask.doneAt` is the task's own `completedAt`, or for an entry with subtasks the latest subtask `completedAt` when all are done, and null while pending. Nothing is stored; the "fall back to the previous latest subtask" behavior follows from clearing a subtask's time.
6. **Ordering in the view model.** `entries` becomes pending first in stored order, then done entries sorted by `doneAt` descending. The reorder move already works on the pending ids and appends the done ids in displayed order, so it needs no change, and the stored positions of done entries stop mattering (they are kept, so an unchecked entry returns to its old place among the pending ones).
7. **Showing it.** The completed entry's supporting text gets "Done HH:mm" in the device time zone, formatted in common code from the view model's time zone (no platform formatter). Completed checkboxes become enabled and call the view model's uncheck. An uncheck emits an event on a shared flow like `criticalHits`, and `App` shows "Undone, -N coins" on the existing snackbar. While Reorder is on nothing is tappable, as before.
8. **Collapsed by default when done.** Today the view model keeps a set of collapsed ids, so every entry starts expanded. It becomes a set of ids the user has flipped away from the default, where the default is expanded for a pending entry and collapsed for a completed one, and an entry is shown expanded when `(id in flipped) != isCompleted`. Whenever an entry's completed state changes its id is dropped from the set, so finishing the last subtask folds the entry and unchecking opens it again, and a tapped choice lasts only until the next change. The screens keep receiving a plain `expanded` flag per entry. Entries without subtasks have nothing to fold, so the default is irrelevant to them.
9. **Past days** are unreachable from the UI (Today shows only today), so no extra guard is added in the stores.

## Risks / Trade-offs

- **An accidental tap** on a checked box now costs coins (and a critical-hit bonus for good). The user chose no confirmation; the message states the coins taken back and checking again restores the reward, with a new roll for the bonus.
- **Negative balance in the top bar** may look alarming; it is intended and the only way out is earning.
- **Sum by `refId`** relies on ids being unique across tasks, subtasks and redemptions. They are random UUIDs or `parent#index` subtask ids, and redemptions use their own random ids.
- **Folding on finish** hides the subtasks the user was just working on. That is intended (the done list stays short), and one tap opens it.
- **Existing tests** that assert a completed checkbox is disabled must change.

## Migration Plan

None. Room stays at version 5; `Undone` is a new text value in an existing column.

## Open Questions

None.
