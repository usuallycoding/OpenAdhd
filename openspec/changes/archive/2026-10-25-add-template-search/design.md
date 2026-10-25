# Design

## Context

`TaskTemplate` lists already have `List<TaskTemplate>.matching(query)` (name or description, case-insensitive, trimmed), used by the Add to today picker. The Tasks and Rewards tabs are `TemplatesContent` and `RewardsContent`, each a list with an add button and an empty-state message, fed by `TemplatesViewModel` and `RewardTemplatesViewModel`. Each view model exposes the full list, `canReorder` (two or more items) and `reordering`; `App` reads those flows to decide whether the top bar shows the Reorder toggle for the visible tab. The two tabs live in a pager, so a page's composable state is lost when the page is two tabs away. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- The same matching rule as the picker on both tabs.
- The search text surviving tab swipes, and Reorder ruled out while it holds text.

**Non-Goals:**
- Other filters, search elsewhere, saving the text, and changing the picker.

## Decisions

- **A twin matching function for rewards.** `List<RewardTemplate>.matchingRewards(query)` beside the task one, with the same rule. It has a different name because two functions `matching(String)` on `List<TaskTemplate>` and `List<RewardTemplate>` would have the same signature after type erasure on the JVM. Alternative: one generic function taking a name-and-description selector; rejected, it would make the two call sites less readable for no gain.
- **The query lives in each view model.** A `MutableStateFlow<String>` with `setQuery(text)`, and `visible` = the list matched against it, as a `StateFlow`. Because the view models outlive the pager pages the text survives swipes and navigation, and it is lost on restart. Alternative: remembering it in the composable with `rememberSaveable`; rejected, the view model also has to know about it for Reorder.
- **Reorder rules in the view model.** `canReorder` becomes true only with two or more items and a blank query, and `reordering` additionally requires a blank query. `setQuery` with non-blank text also clears the requested flag, so clearing the search does not bring Reorder back on its own. `App` needs no change: it already follows `canReorder` and `reordering`.
- **Creating clears the query.** `save` with no id calls `setQuery("")` before creating. Editing and deleting leave it.
- **Two empty states.** The screens get the total count as well as the visible list: no items at all shows the existing message and no field; items but no matches shows "No task matches <text>" or "No reward matches <text>". The text in the message is the trimmed query, as in the picker.
- **UI.** An `OutlinedTextField` with the label "Search tasks" or "Search rewards", single line, a clear icon button while it has text, placed above the list inside the tab, and not focused. It scrolls out of view with nothing else: it is a fixed row above the `LazyColumn` so the list scrolls under it.
- **Putting the keyboard away.** On iOS a text field keeps the keyboard until focus is cleared, and on Android a tap elsewhere does not clear focus either. `SearchField` gets the Done key (`ImeAction.Done`) and clears focus and hides the keyboard when it is pressed. The area under the field, a box that fills the rest of the tab and holds the list or the no-match message, clears focus when it is tapped and when the list scrolls (a nested-scroll connection that clears focus before the scroll is applied). The detector is deliberately not on the whole tab: a tap on the field itself would otherwise take focus and then lose it again.
- **Reordering rows use the full list.** With a blank query the visible list is the full list, so the move controls and their first/last checks are unchanged.

## Risks / Trade-offs

- [The field takes a row of height on a small screen] → One row, and only on tabs that have items.
- [The query is kept but invisible when the user returns after a long time] → The field is on screen above the list with its text, so a filtered list is never mistaken for a short one.
- [Two near-identical matching functions] → Both are one line over the same rule and have tests; a shared helper would not be clearer.

## Migration Plan

No data or schema change. Rolling back means removing the field and the query state.
