# Proposal

## Why

A tap on a checkbox is final today: a task or subtask marked done by mistake, or done in a way the user later decides did not count, cannot be taken back, and the coins stay. For someone whose day is chaotic, being able to correct the list matters, and an honest coin total matters with it. The done list is also in no useful order: it should read as a log of the day, newest first, with when each thing was finished.

## What Changes

- **Uncheck:** a completed task without subtasks, and a completed subtask, can be unchecked on Today. The item becomes pending again and every coin it paid is taken back, including a critical-hit bonus. The undo is immediate, with no confirmation, and a short message such as "Undone, -14 coins" is shown.
- **Ledger stays append-only:** unchecking adds a negative entry under a new "undone" reason that references the same task or subtask, for the full net amount that item has paid. Nothing is edited or removed.
- **Negative balance:** the coin balance may go below zero, for example after unchecking something whose coins were already spent. Redeeming and rolling are still refused while the balance is lower than the price.
- **Re-checking** an unchecked item pays again and gets a fresh critical-hit chance; because the full amount was taken back, checking and unchecking in a loop gains nothing.
- **Entries with subtasks:** unchecking a subtask of a finished entry makes the entry pending again; unchecking all subtasks leaves an untouched entry that can be removed again.
- **Done order and done time:** completed entries are listed after the pending ones, newest done first. The done time of an entry without subtasks is when it was checked; for an entry with subtasks it is when its latest-done subtask was checked. It is shown on the entry, for example "Done 14:32".
- **Done entries start collapsed:** a completed entry with subtasks starts with its subtasks hidden, so the done list stays short. An entry returns to its default (pending expanded, completed collapsed) whenever it becomes completed or pending again, so finishing the last subtask folds the entry away and unchecking one opens it again. Tapping still toggles, and the choice is kept while the app runs.
- No database change: completion times are already stored, and the ledger reason is stored as text.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `day-planner`: completed items can be unchecked, completed entries are ordered and labeled by when they were done, and completed entries start collapsed.
- `coin-economy`: undoing a completion is recorded as a negative ledger entry, and the balance may be negative.

## Impact

- `shared` domain and stores: `uncompleteTask` and `uncompleteSubtask` on the planner repository, one transaction each (clear the completion, append the reversal of the net amount recorded for that item), in `InMemoryStore` and Room; a new `LedgerReason.Undone`.
- View model and UI: completed checkboxes become tappable, the done group is sorted by done time, the done time is shown, completed entries start collapsed, and an undo message uses the existing snackbar.
- Tests: contract tests for both stores, view model tests, UI tests; the existing tests that assert completed checkboxes are disabled change.
- Not changed: the reordering rules (done entries are still not movable), the Shop, and the "spending cannot overdraw" rule. Dopamine Loans, which would relax that rule, stays a separate idea.
- Out of scope: undoing completions from earlier days, and a screen listing the ledger.
