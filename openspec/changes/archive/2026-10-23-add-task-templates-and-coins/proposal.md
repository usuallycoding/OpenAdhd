# Proposal

## Why

OpenAdhd is meant to be an ADHD-focused task and reward app built around a point economy ("Dopamine Coins"). Today it is only a themed shell with a topbar. This first slice delivers the core loop end to end: define reusable tasks, plan the day, finish a task, and see coins arrive. It proves the data model and economy foundation before the richer mechanics (crits, shop, loans, streaks) are layered on.

## What Changes

- Add task templates: create, edit and delete reusable tasks with a name, a difficulty tier (1-4) and an energy level (Low, Medium, High).
- Add a "Today" list: manually assign templates to today, and mark them complete.
- Add the coin economy foundation: completing a task awards base coins by tier, recorded in an append-only ledger; the balance is shown in the topbar.
- Persist templates, today's entries and the ledger locally on Android, iOS and desktop, with no sync or accounts.
- Add simple navigation between the Today and Templates screens.

## Capabilities

### New Capabilities
- `task-templates`: Creating, editing, deleting and listing task templates with tier and energy.
- `day-planner`: Assigning templates to today and completing the resulting entries.
- `coin-economy`: Base coin rewards by tier, the append-only coin ledger, and the balance.
- `local-data-storage`: Keeping the user's data on the device across app restarts.

### Modified Capabilities

## Impact

- New domain, data and UI code in `shared` (templates, today entries, ledger, economy rules, repositories, Today and Templates screens); `App.kt` gains navigation and a balance display in the topbar (`app-topbar` behavior is extended in practice but its requirements are unchanged).
- New dependencies: a local persistence library and date/time support, to be settled by a spike (see design.md).
- Out of scope, planned as later changes: reward templates and shop, mystery rolls, Critical Hit, Wall of Awful, smart randomizer, streak protection, Dopamine Loans, and any cross-device sync.
