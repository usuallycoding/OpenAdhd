# Design

## Context

The picker is `TemplatePickerDialog` in `TodayScreen`, a `FormSheet` whose body is a plain scrolling column holding the search field and the listed templates; `FormSheet` has a fixed footer but no fixed header. The query lives in the sheet and starts empty on open; `List<TaskTemplate>.matching(query)` filters on name or description. Today's "Pick one for me" is a weighted pick done by `TodayViewModel` with an injected `Random`, and it highlights its pick with a timed state and the `tertiaryContainer` colour. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- A header that stays put, without turning the sheet body into a lazy list.
- Filter and roll rules that are pure and deterministic under a seeded random source.

**Non-Goals:**
- Changing Today's randomizer or its weighting, persisting the filter, and automatic adding.

## Decisions

- **`FormSheet` gets an optional `header` slot.** It is drawn between the title and the scrolling body, outside the scroll, with the same horizontal padding; sheets that pass nothing look as before. Alternative: a lazy list with a sticky header; rejected, the sheet body is a vertically scrolling column and nesting a lazy list in it is not allowed, while a `header` slot also helps any later sheet.
- **Filter next to `matching`.** `List<TaskTemplate>.matching(query, energy: Energy?)` applies the text rule and then an equality test on energy; the existing one-argument form stays, so current callers and tests are unchanged. Energy state is a nullable `Energy` in the picker, selected chip equals it, tapping the selected chip sets null.
- **Roll as a pure function.** `PickerRoll.pick(random, shown, previousId): TaskTemplate?` returns null for an empty list, the only template for a list of one, and otherwise draws uniformly from the list without the template whose id is `previousId`. `TodayViewModel.rollTemplate(shown, previousId)` calls it with the view model's own `Random`, as the Today pick does, so tests seed it. Alternative: weighting by energy as Today does; rejected, the user has already chosen the energy with the chips, and a uniform draw is easier to explain.
- **Scroll and highlight.** The picker keeps `rolledId` and a highlight flag in its own state, like the query, so both reset on open. The rolled row shows the same `tertiaryContainer` background and clears after the same short delay used by Today (`PickedHighlightDuration`). Scrolling uses `bringIntoViewRequester` on that row, which works inside the sheet's scrolling column without a lazy list. The previous roll's id is remembered separately and also resets on open.
- **Dice placement.** The header holds the search field, then a row with the three chips and the dice button at its end, using the existing `DiceIcon` with the description "Roll a task from the list" so it is not confused with Today's "Pick one for me".
- **Filter and roll reset together.** Changing the text or the energy clears the highlight; the previous-roll memory is kept, so a roll after a filter change still avoids the last pick if it is still listed.
- **No templates.** The header is not shown at all (search, chips, dice), keeping the current message.

## Risks / Trade-offs

- [Fixed header, keyboard and a small screen leave little room for results] → The sheet already pads for the keyboard; the header is two compact rows, and the body keeps scrolling. To be checked on a phone.
- [A highlight that is scrolled to but hidden behind the header] → `bringIntoView` brings the row into the body's viewport, which starts below the header.
- [Two dice icons on screen (Today's and the picker's)] → Different descriptions, and the picker's is inside the sheet.
- [Uniform draw ignores energy fit that Today weights] → Intentional; the energy chips are the explicit choice.
