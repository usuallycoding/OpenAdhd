# Design

## Context

Completing work is done by `completeTask(id)` (a task without subtasks) and `completeSubtask(id)`. Each is atomic and idempotent: it marks the item, appends one ledger entry (`TaskCompleted` or `SubtaskCompleted`) with the reward frozen on the entry, and returns a Boolean (false if refused). The ledger stores its reason as text, the balance is the sum of the ledger, and both stores (in-memory and Room, database version 4) are run against one contract test suite. A single injected `Random` already exists for mystery rolls. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A critical hit decided inside the same atomic completion, recorded as its own ledger entry, with no schema change.
- Every existing test staying deterministic, and the real app reliably having crits switched on.
- A celebration that never interrupts the user.

**Non-Goals:**
- A mega-crit tier, Wall of Awful, crits on mystery rolls, a bonus history screen, and changing any reward amounts or prices.

## Decisions

- **Rules are pure and tiny.** `CriticalRules` holds `OneIn = 8` and `Multiplier = 2`, and `bonus(reward) = reward * (Multiplier - 1)` (whole coins, so no rounding). The bonus is computed from the reward amount frozen on the entry or subtask, never from a recomputed one.
- **A separate `CriticalHits` source decides.** A small interface `CriticalHits { fun isCritical(): Boolean }` with `Never`, `oneIn(n, random)` and `Standard` (= `oneIn(CriticalRules.OneIn)`). It is separate from the `Random` used for mystery rolls, so scripted roll tests are not disturbed by crit draws, and tests can script crits with a queue of booleans.
- **Stores default to `Never`.** `InMemoryStore` and `RoomStore` take a `criticalHits` parameter that defaults to `CriticalHits.Never`. Every existing test and preview therefore stays deterministic by construction; without this, about 1 in 8 completions in existing tests would randomly pay extra and make them flaky.
- **The real app turns crits on in one shared place.** A common `createContainer(database)` builds the Room store with `CriticalHits.Standard`, and the Android, iOS and desktop factories all call it instead of constructing the store themselves. This replaces three copies of the same wiring, and a jvm test builds a container through it and completes hundreds of tasks to check that crits actually occur at roughly 1 in 8, so the feature can never be silently off.
- **The draw happens only when the completion succeeds.** In memory the draw happens inside the state transition after validation; in Room, `OpenAdhdDao` gets the `CriticalHits` object and calls it inside the `@Transaction` only after the item was marked completed. A refused or repeated completion therefore never draws (which is also what keeps scripted tests exact) and cannot be used to re-roll.
- **Ledger.** `LedgerReason.CriticalHit`; the bonus entry has `amount = bonus`, the same `refId` as the reward entry (the task or subtask id), and the same timestamp. The reward entry is written exactly as before. No migration and no version bump, because the reason is already stored as text.
- **Completion returns a result.** `completeTask` and `completeSubtask` return `CompletionResult(reward, bonus)` (with `isCritical = bonus > 0`) or null when refused, replacing the Boolean. This is what lets the screen celebrate, and it ripples through the view model and the existing completion tests, which are updated mechanically.
- **Celebration is a snackbar.** `TodayViewModel` emits a one-shot event when a completion is critical (a shared flow with a small buffer, so rapid completions are not lost). The app shell, which owns the `Scaffold`, collects it and calls `showSnackbar("Critical hit! +N bonus coins")` on a `SnackbarHostState`; Material's snackbar auto-dismisses and does not block taps, and queued messages show one after another. No dialog, because dialogs are reserved for flows that spend coins.
- **Nothing is announced beforehand.** The Today screen already shows only the normal reward for pending items; no new UI reveals or hints at the next result.
- **Expected effect.** 1 in 8 at x2 raises average earnings by 12.5%. Because the bonus is proportional to the reward, splitting a task into more subtasks changes only the variance, not the average.

## Risks / Trade-offs

- [Crits silently disabled in production because a platform factory is missed] → one shared `createContainer` used by all three factories, plus a jvm test that exercises it statistically.
- [The statistical wiring test could fail by chance] → 800 completions with bounds far from the expected 100 (50 to 160, about 5 standard deviations), so a chance failure is vanishingly unlikely.
- [Changing the completion return type touches many tests] → mechanical changes guided by the compiler; the contract suite is extended in the same group.
- [Rapid completions flood the screen with snackbars] → Material queues them and each is short; only crits (1 in 8) show one.
- [A crit on a tiny reward feels small (a 1-coin subtask gives +1)] → accepted; the celebration still marks the moment.
- [Double-draw on a compare-and-set retry in memory] → harmless, since only the successful attempt's outcome is stored.

## Migration Plan

No data migration and no schema change; the database stays at version 4. Existing ledgers are untouched, and future entries may carry the new reason. Downgrading is not supported, as before.

## Open Questions

- Whether a rarer "mega crit" tier (for example a small chance of x3) should be added can wait for real use; it would extend `CriticalRules` and one requirement, not the data model.
