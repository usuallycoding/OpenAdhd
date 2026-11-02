# Tasks

## 1. Scroll guard

- [x] 1.1 In `DismissKeyboard.kt`, add a testable nested-scroll class that puts the keyboard away on the first user scroll while it is visible, then consumes the gesture's post-scroll and post-fling and releases its latch; verify with unit tests in `shared/test` that cover keyboard visible and hidden, a gesture with and without a fling, and a second gesture after the first (the sheet gets it)
- [x] 1.2 Add `Modifier.putKeyboardAwayOnScroll()` (uses the bottom of `WindowInsets.ime` through `rememberUpdatedState`) and leave `dismissKeyboardOnTapOrScroll()` unchanged; verify `./kotlin build -m desktopApp` and the existing keyboard UI tests (`PickerSearchUiTest`, `TemplateSearchUiTest`) still pass

## 2. Every sheet

- [x] 2.1 In `FormSheet`, apply `putKeyboardAwayOnScroll()` to the body always, and keep `dismissKeyboardOnTapOrScroll()` only when `dismissKeyboardOnBodyInteraction` is set; update its documentation comment; verify `./kotlin test` passes, including `FormSheetTest`, and the picker still hides the keyboard on Done, scroll and tap

## 3. Veto the sheet's move to Hidden

- [x] 3.1 Add the veto decision as a small function (keyboard showing and the target is Hidden and the close is not a requested one) with unit tests in `shared/test` for keyboard showing and hidden, a requested close, and a target other than Hidden; verify the tests pass on the JVM and the iOS simulator
- [x] 3.2 In `FormSheet`, give the sheet state a `confirmValueChange` that uses it, puts the keyboard away when it refuses, and have `requestClose` set the flag that lets Save, Cancel and other programmatic closes through; verify `./kotlin test` passes, including UI tests that Cancel and Save still close a sheet and that closing with no keyboard is unchanged

## 4. Swipes outside the sheet

- [x] 4.1 Spike: try to observe a downward swipe that starts on the dimmed area outside the sheet and put the keyboard away without closing the sheet, on the Android emulator (swipes driven with `adb`) and on the iOS simulator; record in design.md under "Spike result" what worked on each platform and the approach chosen, or that it did not work
- [x] 4.2 Implement the approach the spike chose and verify on the emulator and the simulator that the swipe outside closes only the keyboard; if the spike failed on either platform, instead apply the fallback in design.md: remove the "Swipe down outside the sheet" scenario from the form-sheets delta and verify `openspec validate add-keyboard-scroll-dismiss` passes

## 5. Check on devices

- [x] 5.1 On an Android phone and an iPhone, open the task form, a reward form, the reminder sheet, a settings sheet that has a text field and the Add to today picker; with the keyboard open, swipe down on the body, on the title, on the Save and Cancel row and (if kept) on the dimmed area, and tap the dimmed area, and verify each time only the keyboard goes and the sheet stays; swipe down or tap outside again and verify the sheet closes; verify Cancel and Save close the sheet with the keyboard open, a short body, a quick swipe right after the keyboard opens, and that with no keyboard closing is unchanged; after closing the keyboard by swiping, tap every text field again and verify the keyboard comes back each time (report each result, or say plainly what could not be checked)
