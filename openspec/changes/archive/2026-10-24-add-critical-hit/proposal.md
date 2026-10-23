# Proposal

## Why

Earning is perfectly predictable today: finishing a task pays exactly the same amount every time. Unpredictable rewards are a core part of what makes reward systems motivating, and the app's idea names this "Critical Hit". A small chance of a bonus on any completion adds that surprise to the daily loop, without changing what anything costs or how much a task is normally worth.

## What Changes

- Completing a task without subtasks, or a subtask, has a 1 in 8 chance of being a **critical hit**, decided at the moment of completion.
- A critical hit pays a bonus equal to that completion's normal reward, so it pays double (x2). There is a single tier.
- The bonus is a separate ledger entry with its own reason, referencing the same task or subtask, so the balance stays explainable ("+7 subtask, +7 critical hit").
- The app tells the user right away with a short self-dismissing message showing the bonus coins; it never blocks the next action. Whether a completion will be a crit is never shown beforehand.
- Completing now reports what was earned (the reward and any bonus) instead of only success, so the screen can react.
- No database change: the ledger already stores its reason as text, so there is no migration.

## Capabilities

### New Capabilities
- `critical-hit`: When a completion becomes a critical hit, what it pays, how it is recorded, that it cannot be re-rolled, and how the app celebrates it.

### Modified Capabilities
- `coin-economy`: the ledger also records a critical hit bonus as its own entry.

## Impact

- Domain (`CriticalRules`, a `CriticalHits` source, a completion result type, a new ledger reason), both stores' completion functions and their callers, the Today view model, and a snackbar in the app shell in `shared`, with tests for each.
- No new libraries and no schema change (the database stays at version 4).
- Stores get critical hits switched off by default, so every existing test stays deterministic; the real app switches them on in one shared place used by all three platform entry points, and a test checks that wiring.
- Assumptions to confirm later: 1 in 8 and x2 are single constants; only the base reward is doubled (a later Wall of Awful bonus would be separate and not doubled); the celebration is a snackbar, not a dialog; no mega-crit tier; no history screen for bonuses.
- Out of scope, planned as later changes: Wall of Awful, Dopamine Loans, a "today's reward" goal, the smart randomizer and streak protection.
