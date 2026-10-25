# Tasks

## 1. Filter and roll rules

- [x] 1.1 Add `matching(query, energy)` next to `matching(query)` and keep the one-argument form (verify: unit tests for energy alone, energy with text, a template matched by description, order kept, no energy meaning every template, and the existing `TemplateSearchTest` still passing)
- [x] 1.2 Add `PickerRoll.pick(random, shown, previousId)` and `TodayViewModel.rollTemplate(shown, previousId)` using the view model's random source (verify: tests with a scripted random for an empty list returning null, one template returned twice, two templates never repeating over many draws, three templates all reachable, a previous id no longer listed, and that rolling changes nothing in the store)

## 2. The picker

- [x] 2.1 Add the optional fixed `header` slot to `FormSheet`, drawn above the scrolling body (verify: a `FormSheet` UI test that a header stays visible when the body is scrolled to its end, and existing sheet tests unchanged)
- [x] 2.2 Put the search field, the Low, Medium and High chips and the dice button in the picker's header, filter with `matching(query, energy)`, reset the energy and the roll memory on open, and show the filtered no-match messages (verify: picker UI tests for filtering by each energy, energy with text, clearing by tapping again, one chip at a time, starting cleared after reopening, "No Low energy task matches dis" and "No High energy task", and no chips or dice without templates)
- [x] 2.3 Wire the dice: roll over the shown list, scroll the row into view with `bringIntoViewRequester`, highlight it briefly, disable the dice when nothing is listed, and avoid the previous pick (verify: UI tests that a roll highlights a listed template and adds nothing, respects the filters, rolls the other of two on a second tap, rolls the only one twice, is disabled for an empty result, and that tapping the rolled template adds it and closes the picker)
- [x] 2.4 Add a short note on the picker's filter and dice to `README.md` (verify: the steps work as written in 3.2)

## 3. Verification

- [x] 3.1 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 3.2 On a phone: open the picker with many templates, scroll the list and confirm the search and chips stay put, filter by each energy, roll twice and see the highlight move, and add the rolled template (verify: manual)
