# Proposal

## Why

Choosing what to do next is itself a hurdle: staring at a list of templates and deciding is exactly where many people with ADHD stall. A "pick one for me" action removes the decision. Because every task already carries an energy level, the app can pick something that fits how much energy the user has right now instead of an arbitrary item.

## What Changes

- Add a "Pick one for me" action on the Today screen. The user chooses their current energy (Low, Medium or High) and the app suggests one task template.
- A suggestion is a random task template whose energy does not exceed the chosen energy, that is not already on today's list (pending or completed). Better fits are more likely: weight 4 when the template's energy equals the chosen energy, 2 when one level below, 1 when two levels below.
- The user can **Add to today** (exactly like choosing it from the list), ask for **Another** (never repeating a suggestion until every eligible one has been shown), or say **Not now**. Nothing is added without accepting.
- When nothing eligible is left, or there are no task templates at all, the dialog says so plainly and lets the user pick another energy.
- The suggestion shows the name, description, tier, energy, the coins it pays and the number of its subtasks.
- No data is stored for this: no schema change, no migration, no new table.

## Capabilities

### New Capabilities
- `task-randomizer`: Suggesting a random task that fits the user's current energy, with the weighting, exclusions, repeat avoidance and the actions to accept, skip or dismiss.

### Modified Capabilities

## Impact

- A pure `TaskPicker` in the domain, a small suggestion state machine in `TodayViewModel`, an optional injected `Random` on `AppContainer`, a dice icon, and a dialog on the Today screen in `shared`, with tests for each.
- No new libraries, no database change (the database stays at version 4), and no change to rewards, prices or the ledger.
- Assumptions to confirm later: the 4/2/1 weights are one small table; the energy is chosen each time and not remembered; "Another" is free and unlimited; there is no tier filter, no bias toward long-untouched tasks, and no animation.
- Out of scope, planned as later changes: Dopamine Loans, streak protection and a "today's reward" goal.
