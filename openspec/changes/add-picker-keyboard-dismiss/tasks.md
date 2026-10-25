# Tasks

## 1. The picker

- [x] 1.1 Add the `dismissKeyboardOnBodyInteraction` option to `FormSheet`, turn it on for the picker, and give the picker's search field the Done key (verify: UI tests that the Done key, a scroll with many templates and a tap on the no-match message each leave the field unfocused with its text and energy filter kept, that tapping the field focuses it again, and that tapping a listed template still adds it; the other sheets' tests still pass)

## 2. Docs and verification

- [x] 2.1 Add a sentence to the Adding a task to Today section of `README.md` (verify: it matches the behaviour in 2.3)
- [x] 2.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 2.3 On a phone: open the picker, type, then put the keyboard away with Done, by scrolling and by tapping the empty area, and tap the field to bring it back (verify: manual, especially on iOS)
