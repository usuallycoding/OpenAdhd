# Proposal

## Why

The Add to today picker lists every template and scrolls the search field away with the list, so finding something that fits how much energy you have means scrolling back up, and choosing between many templates is still a decision. Filtering by energy, keeping the controls in reach, and a dice roll over what is shown take that effort out of adding a task.

## What Changes

- **Energy filter:** under the search field, the chips Low, Medium and High filter the listed templates by their energy. One can be selected at a time, tapping the selected one clears it, and it combines with the text search (energy and text). It starts cleared each time the picker opens.
- **Fixed header:** the search field and the energy chips stay at the top of the picker while the results scroll under them.
- **No-match message:** when a filter is on and nothing matches, the message names it, for example "No Low energy task matches dis".
- **Dice roll in the picker:** a dice button next to the chips draws one template, uniformly, from the templates currently shown. The list scrolls to it and highlights it briefly; nothing is added, so tapping it still adds it. A roll avoids the previous pick when more than one template is shown. The dice is disabled when nothing is shown.
- **Unchanged:** searching by name and description, and the Today "Pick one for me", which keeps its energy question and weighting.
- **Not in this change:** adding the rolled template automatically, multi-select energy, and filters other than energy (such as tier).

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `day-planner`: the picker gets an energy filter, a header that stays in view, and the filtered no-match message.
- `task-randomizer`: the picker gets a dice roll over the shown templates.

## Impact

- `shared` domain: a filter that takes an optional energy next to `matching`, and a pure roll function with the no-repeat rule.
- `shared` UI: `FormSheet` gets an optional fixed header slot; the picker uses it for the search field, the chips and the dice, and scrolls to and highlights the rolled template.
- `TodayViewModel` draws the roll with its injected random source, so tests are deterministic.
- Tests: filter and roll rules, view model roll, and picker UI tests. No storage, backup or platform code, and no new libraries.
