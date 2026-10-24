# Tasks

## 1. Domain and stores

- [x] 1.1 Add `LedgerReason.Undone`, `PlannedTask.doneAt` and `uncompleteTask`/`uncompleteSubtask` to the planner repository (returning the coins taken back, or null when unknown, still pending or, for a task, having subtasks), implement them in `StoreLogic`/`InMemoryStore` by clearing the completion and appending one reversal of the net ledger total for that refId (nothing when the net is zero), and add contract tests: uncheck a task, a subtask and a subtask that was a critical hit, the entry becomes pending again, a second round reverses only the second payout, a check-uncheck loop returns to the starting balance, a pending or unknown item changes nothing, the balance can go negative and a redeem or roll is then refused, `doneAt` for tasks and entries with subtasks; verify they pass against `InMemoryStore`
- [x] 1.2 Implement the same two operations in the Room DAO and `RoomStore` (guarded update, sum by refId, insert, one transaction each) and verify the contract passes against `RoomStore`

## 2. View model

- [x] 2.1 Sort the completed entries by `doneAt` descending after the pending ones, make completed entries start collapsed (the view model keeps the ids the user flipped from the default and drops an id when its entry's completed state changes), add `uncompleteTask`/`uncompleteSubtask` to `TodayViewModel` emitting an undone event with the coins taken back, and verify with view-model tests: done order newest first, entry with subtasks ordered by its latest subtask, uncheck brings an entry back among the pending ones at its stored position, reorder still moves only pending entries, the undone event carries the amount and is not emitted for a no-op, a completed entry starts collapsed and a pending one expanded, finishing the last subtask folds the entry, unchecking opens it, and a tapped choice is kept until the entry's completed state changes; update the existing collapse tests that assume every entry starts expanded

## 3. UI

- [x] 3.1 Enable the checkboxes of completed tasks and subtasks to uncheck, show "Done HH:mm" on completed entries (formatted in the device time zone), and show "Undone, -N coins" on the existing snackbar when an uncheck is reported; keep everything disabled while Reorder is on
- [x] 3.2 Update the UI tests that assume a completed checkbox is disabled and add tests for: unchecking a task and a subtask changes the balance and the list, the message text, the done time label, done entries newest first, the top bar showing a negative balance, a spend being refused while the balance is negative, a completed entry with subtasks showing "n of n done" with its subtasks hidden until tapped, and finishing an entry folding it

## 4. Wrap-up

- [x] 4.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 4.2 Verify on desktop: complete and uncheck a task and a subtask, see the coins taken back and the message, complete several things and check the done order and times, uncheck something whose coins you already spent and see the negative balance, and try to redeem a reward while it is negative
