# Tasks

## 1. Domain

- [x] 1.1 Add `TaskPicker` (`weight` with 4/2/1/0, `eligible`, `pick` drawing once over cumulative weights, null when nothing is eligible), and unit tests for: the weight table for every chosen and template energy, Low never getting Medium or High, Medium never getting High, excluded ids never picked, equal energies having equal weight, exact scripted draws mapping to the right template (High day with weights 4/2/1 over 7), null when nothing is eligible, and a seeded statistical check of about 4:2:1 over thousands of draws; verify `./kotlin test -m shared` passes

## 2. Suggestion state

- [x] 2.1 Add an injectable `Random` to `TodayViewModel` and `AppContainer` (default `Random.Default`), the `RandomizerState` states, and `openRandomizer`, `chooseEnergy`, `another`, `accept`, `dismiss` and `chooseAnotherEnergy`; verify the build compiles
- [x] 2.2 Add view-model tests with a scripted `Random` for: no templates giving `NoTemplates`, choosing an energy giving the expected suggestion, templates already on today's list (pending and completed) never suggested, "Another" never repeating until the eligible set is exhausted and then reporting exhausted, choosing the energy again forgetting what was shown, accepting adding the entry with its frozen reward and closing, dismissing adding nothing and changing no balance, and nothing fitting a Low energy choice; verify they pass

## 3. Screen

- [x] 3.1 Add the dice button above the "+" button, the "Pick one for me" button in the empty Today state, a dice icon, and the dialog for all five states (energy chips, suggestion card with name, description, tier, energy, coins and subtask count and the three actions, the no-templates message, and the nothing-fits or exhausted message with "Choose another energy"), plus a preview; verify it compiles on all targets
- [x] 3.2 Add UI tests with a scripted `Random` and a seeded store for: the full flow (start, choose energy, see the suggestion with its details, accept, see the entry on Today with its reward), "Another" showing different templates and then the exhausted message, "Not now" leaving Today unchanged, Low energy never offering a higher-energy template, an already planned template never suggested, and the no-templates message; verify they pass

## 4. Integration

- [x] 4.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 4.2 Verify on desktop: with templates of different energies, use "Pick one for me" at each energy, check that suggestions respect the energy, that "Another" cycles without repeats, and that accepting adds the entry to Today
