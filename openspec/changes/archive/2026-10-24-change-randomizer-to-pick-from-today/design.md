# Design

## Context

The randomizer is a dialog state machine in `TodayViewModel` (`RandomizerState`) over `TaskPicker`, a pure weighted draw over templates with an injected `Random`. Today's entries are `PlannedTask`s, which already carry `energy` and `isCompleted`. The Today list is a `LazyColumn` with no hoisted state. A one-shot `SharedFlow` (`criticalHits`) already exists for ViewModel-to-screen events.

## Goals / Non-Goals

**Goals:**
- Reuse the 4/2/1 weighting and the injected `Random` so picks stay testable.
- Reveal the pick by scrolling and highlighting, with the ViewModel holding no UI state.

**Non-Goals:**
- No repeat avoidance, no subtask picks, no auto-expanding of collapsed entries.
- No changes to storage, backup or the History screen.

## Decisions

- **`TaskPicker` is generic over energy.** `pick` takes candidates plus a way to read their energy, or entries directly; eligibility becomes "not completed and weight > 0". `weight` is unchanged. Alternative: a second picker for entries — rejected as duplicated weighting.
- **Smaller state machine.** `RandomizerState` becomes `Closed`, `ChoosingEnergy`, `NothingFits(energy)` and `NothingPending`. `Suggesting`, `NoTemplates`, `shown`, `another` and `acceptSuggestion` are removed. `openRandomizer` goes straight to `NothingPending` when no entry is pending.
- **Reveal through an event.** `chooseEnergy` draws, closes the dialog and emits a `PickedEntryEvent(entryId)` on a `SharedFlow`, like `criticalHits`. The screen collects it, calls `animateScrollToItem` on a hoisted `LazyListState` using the entry's index, and sets a transient `highlightedId` cleared after about 1.5 s. Alternative: keeping the picked id in state — rejected, since it would replay after rotation or recomposition.
- **Highlight** is a background colour on the `ListItem` via an `EntryItem` parameter, animated with `animateColorAsState`. A collapsed entry stays collapsed.
- **Empty state** loses the "Pick one for me" button; the FAB is unchanged. The FAB remains hidden in Reorder mode and on past days.
- **Icon.** `MoreIcon` keeps its name and three dots, moved to x=12, y=5/12/19. The `task-reminders` spec says "..." only as the button's name, so it needs no delta.

## Risks / Trade-offs

- [Re-roll may repeat the same task] → accepted by the user; picking again is the intended way to ask for another.
- [Scrolling while the dialog closes] → emit the event after the state change and scroll from a `LaunchedEffect` collector so it runs after the dialog is gone.
- [Entry index changes between pick and scroll] → look the index up in the current list at scroll time; ignore if the entry has gone.
