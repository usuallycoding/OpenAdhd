# Design

## Context

Every `TaskTemplate` has an `Energy` (Low, Medium, High). The Today screen is driven by `TodayViewModel`, which already exposes today's `entries` and all task `templates` as state and has `assign(template)`, the single way an entry is created (it copies the template and freezes its rewards). `AppContainer` carries the app's long-lived dependencies, and tests inject scripted randomness elsewhere (`ScriptedRandom` for mystery rolls). See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A pure, deterministic-under-test way to pick a task for a given energy, with the 4/2/1 weighting.
- Reuse of the existing assignment path, so accepting a suggestion is identical to picking from the list.
- No stored data.

**Non-Goals:**
- Remembering the chosen energy, filtering by tier, biasing toward long-untouched tasks, suggesting rewards or subtasks, animation, and any change to the ledger, prices or the database.

## Decisions

- **`TaskPicker` is a pure object in the domain.**
  - `weight(chosen, templateEnergy)` is 4 for an equal level, 2 for one level below, 1 for two below, and 0 when the template needs more energy than chosen.
  - `eligible(chosen, templates, excludedTemplateIds)` keeps the templates with a positive weight that are not excluded, in their given order.
  - `pick(random, chosen, templates, excluded)` returns null when nothing is eligible; otherwise it draws once with `random.nextInt(totalWeight)` and walks the cumulative weights. A single draw over a stable order makes scripted tests exact and the proportions easy to verify statistically.
- **Exclusions come from two places.** The template ids of today's entries (pending or completed), and the ids already shown during the current energy choice. Entries are looked up by their `templateId`; entries from deleted templates simply do not match any current template.
- **A small state machine lives in `TodayViewModel`.** It already holds `entries` and `templates`, so no new view model or repository is needed. `RandomizerState` is one of `Closed`, `NoTemplates`, `ChoosingEnergy`, `Suggesting(energy, template)` or `NothingFits(energy, exhausted)`.
  - `openRandomizer()` goes to `NoTemplates` when there are no task templates at all, otherwise to `ChoosingEnergy`.
  - `chooseEnergy(e)` clears the shown set and suggests.
  - `another()` suggests again for the same energy, skipping shown templates.
  - `accept()` calls the existing `assign(template)` and closes.
  - `dismiss()` closes; `chooseAnotherEnergy()` returns to `ChoosingEnergy`.
  - "exhausted" distinguishes "everything that fits has been shown" from "nothing fits this energy" so the message can be accurate.
- **Randomness is injected.** `TodayViewModel` takes a `Random` (default `Random.Default`), supplied from a new `AppContainer.random` (default `Random.Default`), so UI tests can pass a `ScriptedRandom` exactly as the roll tests do. Nothing is persisted, so there is no migration; the database stays at version 4.
- **UI.**
  - On the Today screen a small dice button sits above the existing "+" button and a "Pick one for me" button sits beside "Add a task" in the empty state.
  - One dialog renders the five states: energy chips; a suggestion card (name, description, tier and energy, coins, subtask count) with "Add to today", "Another" and "Not now"; a plain message for "no templates" that points to creating one; and a message for "nothing fits" or "that was everything" with a "Choose another energy" button.
  - Wording stays warm and neutral.
  - The dice icon is a code-defined vector like the others.
- **No new libraries.**

## Risks / Trade-offs

- [A small template list makes the same few suggestions] → "Another" never repeats until the eligible set is exhausted, and the exhausted message points to another energy or the full list.
- [Weights bias toward exact fits, so a High-energy day rarely suggests Low tasks] → intended (best fit first); the table is three numbers in one place and can be tuned.
- [The picker could suggest something the user already did today] → completed and pending entries for the same template are both excluded.
- [A template edited while the dialog is open] → the suggestion shows the template as it was when picked; accepting assigns the current stored template state at that moment through the normal path.
- [Randomness in tests] → the random source is injected, and the weighting is also checked statistically with a seeded generator.

## Migration Plan

None. No schema change, no data, no migration; downgrading is unaffected.

## Open Questions

- Whether to remember the last chosen energy, or to bias toward tasks that have waited long, can be decided after trying the feature; each would be a small addition, not a change of the model.
