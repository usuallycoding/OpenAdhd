# Proposal

## Why

The dice button suggests a task *template* and asks the user to add it, but the real decision-fatigue moment is later: today's list is already planned and the user cannot choose which task to start. The randomizer should choose among what is already on today's list.

## What Changes

- The dice button asks for the current energy, then picks one pending entry from today's list whose energy is not higher than the chosen one, scrolls the list to it and briefly highlights it. No result dialog, no "Add to today" step.
- Templates are no longer suggested. **BREAKING**: the "Another" button, the no-repeat memory, the suggestion dialog and the "no task templates" message are removed. Tapping the dice again re-rolls and may land on the same task.
- Same 4/2/1 energy weighting as before, applied to today's pending entries. Completed entries and subtasks are never picked.
- When nothing pending fits, the app says so and offers to choose another energy. When nothing is pending at all, it says there is nothing left to pick, without asking for energy.
- The empty-today state drops its "Pick one for me" button and keeps "Add a task".
- The "..." options icon on a Today row becomes three vertical dots.

## Capabilities

### New Capabilities

### Modified Capabilities
- `task-randomizer`: picks and reveals a pending entry from today instead of suggesting a template.
- `form-sheets`: the "Pick one for me" suggestion dialog is gone; only the energy question remains a centered dialog.

## Impact

- `TaskPicker` (domain), `TodayViewModel` randomizer state and events, `TodayScreen` (FAB, empty state, dialogs, list scroll and highlight), `AppIcons.kt` (`MoreIcon`).
- `RandomizerViewModelTest`, `TaskPickerTest` if present, and the randomizer section of `AppUiTest` are rewritten.
- No data, storage or backup changes.
