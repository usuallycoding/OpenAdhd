# Design

## Context

The Tasks and Rewards tabs put the keyboard away with a modifier, `dismissKeyboardOnTapOrScroll`, on the area under their search field, and `SearchField` gives the field a Done key. The picker is a `FormSheet` whose search field and chips sit in its fixed header and whose templates are in the sheet's scrolling body; the field focuses itself when the sheet opens. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- The same ways of putting the keyboard away in the picker as on the tabs.
- No change for other sheets.

**Non-Goals:**
- Changing the picker's focus on opening, its chips or its dice.

## Decisions

- **An opt-in on `FormSheet`.** A parameter `dismissKeyboardOnBodyInteraction` (default false) adds the modifier to the body column. It is placed outside the vertical scroll in the modifier chain, so the scrolling container's pre-scroll reaches the nested-scroll connection that clears focus. Alternative: always on; rejected, the template form's body holds text fields and a tap on one of them would take focus and then lose it again.
- **The field stays in the header.** The tap detector covers only the body, so a tap on the search field is never seen by it.
- **Done key on the picker's field.** It uses the same options and action as `SearchField` (`ImeAction.Done`, clear focus and hide the keyboard). The picker's field keeps its own code because it also carries the focus requester that opens the keyboard.
- **Tapping a template.** A template row is clickable and consumes the tap, so the body's detector does not run and the row still adds the template.

## Risks / Trade-offs

- [The body is short and the tappable empty area small] → The no-match message and the gaps between rows count; scrolling and Done are the other routes.
- [A tap on a row that is not clickable, such as the empty-state text] → Counts as a tap in the area and hides the keyboard, which is intended.

## Migration Plan

No data change. Rolling back means removing the option and the Done key.
