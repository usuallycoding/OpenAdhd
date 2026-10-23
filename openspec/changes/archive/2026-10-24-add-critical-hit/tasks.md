# Tasks

## 1. Domain and rules

- [x] 1.1 Add `CriticalRules` (`OneIn = 8`, `Multiplier = 2`, `bonus(reward)`), the `CriticalHits` interface (`Never`, `oneIn(n, random)`, `Standard`), `LedgerReason.CriticalHit` and `CompletionResult(reward, bonus)` with `isCritical`; verify `./kotlin build -m shared` compiles
- [x] 1.2 Add unit tests for the bonus amount (20 gives 20, 7 gives 7, the 1-coin case), `Never`, `oneIn` mapped from scripted draws (a draw of zero is a crit, anything else is not), and a seeded statistical check of about 1 in 8 over thousands of draws; verify `./kotlin test -m shared` passes

## 2. Stores and contract tests

- [x] 2.1 Change `completeTask` and `completeSubtask` to return `CompletionResult?`, add the `criticalHits` parameter (default `Never`) to `InMemoryStore`/`StoreLogic`, and on a successful completion draw once and append a separate `CriticalHit` ledger entry referencing the same task or subtask when critical; update the callers (`TodayViewModel`) and every existing test for the new return type; verify the build compiles and the existing tests still pass
- [x] 2.2 Extend the contract tests to let a store be built with scripted crits (a queue of booleans) and cover: a usual completion pays only the reward, a critical task pays 20 plus a 20 bonus, a critical subtask pays 7 plus a 7 bonus with two entries sharing the same ref id, the reasons are `TaskCompleted`/`SubtaskCompleted` and `CriticalHit`, each subtask decides on its own, a repeated or refused completion neither pays nor draws, and the balance reflects both entries; verify the in-memory store passes

## 3. Room and wiring

- [x] 3.1 Pass `CriticalHits` into the Room store and its completion DAO transactions (draw only after the item was marked completed, then insert the bonus entry in the same transaction), with the default `Never` and no schema change; verify the shared contract suite passes against `RoomStore` on jvm and the database version is still 4
- [x] 3.2 Add a common `createContainer(database)` that builds the Room store with `CriticalHits.Standard`, switch the Android, iOS and desktop factories to it, and add a jvm test that builds a container through it and completes 800 tasks, asserting between 50 and 160 critical hits; verify it passes and all three factories compile
- [x] 3.3 Extend the restart test so a critical-hit ledger entry is saved and restored with the right reason and balance; verify it passes

## 4. UI state

- [x] 4.1 Make `TodayViewModel` return the completion results and emit a one-shot critical-hit event (bonus coins) only for critical completions, and verify with view-model tests including no event for usual completions, one event per crit when completing quickly, and no event for refused completions

## 5. Screens

- [x] 5.1 Add a `SnackbarHostState` and `SnackbarHost` to the app shell, collect the Today view model's critical-hit events and show "Critical hit! +N bonus coins"; verify with UI tests using scripted crits that a critical subtask shows the message and the balance rises by twice the reward, that a usual completion shows no message, that the user can keep completing items while the message is up, and that pending items show only their normal reward
- [x] 5.2 Update the README with a short line about critical hits if it describes earning, and verify the text matches the behavior

## 6. Integration

- [x] 6.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 6.2 Verify on desktop: run the app, complete tasks and subtasks until a critical hit appears (about 1 in 8), and confirm the message, the doubled payout in the balance, and that the balance persists after a restart
