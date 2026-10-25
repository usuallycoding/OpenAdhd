# Proposal

## Why

The Add to today picker opens with its search field focused and the keyboard up, which is right for typing, but afterwards there is no way to put the keyboard away: pressing the keyboard has no Done key, and tapping or scrolling the results does nothing to it. On iOS this leaves the keyboard covering the list. The Tasks and Rewards tabs already got this fix; the picker should behave the same.

## What Changes

- **Done key:** the picker's search field gets a Done key that puts the keyboard away and keeps the text.
- **Scrolling the results:** scrolling the list of templates in the picker puts the keyboard away.
- **Tapping the results area:** tapping empty space or the no-match message in the list area puts the keyboard away. Tapping a template still adds it.
- **Only the picker opts in:** other sheets have text fields in their scrolling body, where a tap on another field must not clear its own focus, so the behaviour is an option on the sheet rather than the default.
- **Not in this change:** the energy chips and dice, which stay as they are, and any other sheet.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `day-planner`: the Add to today picker's keyboard can be put away.

## Impact

- `shared` UI: `FormSheet` gets an opt-in for putting the keyboard away on taps and scrolls in its body, and the picker turns it on and gives its field the Done key.
- Tests: UI tests for the Done key, a tap on the no-match message and a scroll in the picker, and that tapping the field still focuses it and tapping a template still adds it.
- No domain, storage or platform code, and no new libraries.
