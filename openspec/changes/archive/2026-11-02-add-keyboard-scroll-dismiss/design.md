# Design

## Context

See proposal.md for why and the specs for behavior. Observed state:

- `FormSheet` wraps every sheet in a `ModalBottomSheet`, with the body in a `verticalScroll` column. Its opt-in `dismissKeyboardOnBodyInteraction` flag (on only for the Add to today picker) applies `dismissKeyboardOnTapOrScroll()` to the body.
- That modifier puts a `NestedScrollConnection` above the body whose `onPreScroll` clears focus and hides the keyboard on any user scroll, plus a tap detector. It returns zero, so the rest of the gesture still reaches the sheet.
- The sheet's own swipe-to-dismiss lives in `ModalBottomSheet`: a drag on the sheet surface (title, footer, handle) moves it directly, and a drag that starts in the body gets the downward scroll the body did not use (in its post-scroll and post-fling), which is why the picker's keyboard hides and then the sheet drags closed. A tap on the dimmed area asks the sheet to close; a swipe there does nothing.
- `FormSheet` creates the sheet state with `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, which can take a `confirmValueChange` that refuses a move to Hidden. Save, Cancel and the other programmatic closes go through `requestClose`, which calls `sheetState.hide()`.
- Built so far: the body guard (`KeyboardScrollGuard`, `putKeyboardAwayOnScroll()`) is in place for every sheet, with unit tests.
- Nested scroll runs pre-scroll from the outside in (sheet, our connection, the body) and post-scroll from the inside out (body, our connection, sheet), so a connection placed above the body can starve the sheet by consuming in post-scroll.
- `Templates` and `Rewards` use the same modifier on list areas that are not sheets.

## Goals / Non-Goals

**Goals:**
- With the keyboard open, a downward swipe anywhere on the screen, and a tap on the dimmed area, put away only the keyboard, in every `FormSheet`.
- No change to closing the sheet when no keyboard is open, none to Save and Cancel, and none on the desktop.

**Non-Goals:**
- Making forms dismiss the keyboard on a tap inside the sheet.

## Decisions

**Keep the existing modifier and add a guard next to it.** `dismissKeyboardOnTapOrScroll()` stays exactly as it is: it clears focus on any user scroll and on tap, whether or not a keyboard is showing, and the picker's UI tests rely on that (they assert the field loses focus after a scroll on the desktop, where there is no keyboard). The new `Modifier.putKeyboardAwayOnScroll()` adds only the keyboard-open behavior below. `FormSheet` always applies the guard to the body; the existing `dismissKeyboardOnBodyInteraction` flag keeps adding the old modifier for the picker, with its comment updated to say it is about taps and focus. *Alternative:* split the old modifier into tap-only and scroll-only parts; rejected because a scroll-only part would clear focus only when a keyboard is showing and change the picker on the desktop.

**The scroll modifier swallows the rest of the gesture while the keyboard is open.** Its connection is a small class with the keyboard check and the put-away action passed in, so it can be unit tested without a UI:
- `onPreScroll` (user input): if the keyboard is visible, put it away and latch "this gesture is ours"; return zero so the body still scrolls what it can.
- `onPostScroll`: while latched, return all of `available`, so the sheet gets nothing.
- `onPostFling`: while latched, return all of `available` velocity, and release the latch. Compose dispatches this at the end of every drag, with or without velocity, so the next gesture starts clean.
- With the keyboard not visible and no latch, every method returns zero and the sheet behaves as before.

**The keyboard check is the bottom of `WindowInsets.ime` being above zero.** It is available in common code (`imePadding` is already used there; `isImeVisible` does not resolve for the iOS targets), and it is zero on the desktop, which keeps the desktop unchanged without a platform check. It is read through `rememberUpdatedState` so the connection is not rebuilt as the keyboard animates. The veto below uses the same check.

**One gesture only.** The latch is what makes the next swipe close the sheet: once the keyboard is gone the visibility check is false and nothing is consumed. *Alternative:* consume whenever the keyboard is visible, without a latch; rejected because the keyboard hides within the gesture, so the sheet would start moving partway through it. *Alternative:* `imeNestedScroll()` so the keyboard follows the finger; rejected as Android-only.

**A veto on the move to Hidden covers swipes on the sheet and taps outside it.** `FormSheet` passes a `confirmValueChange` to the sheet state: while the keyboard is showing it refuses a move to Hidden and puts the keyboard away instead; otherwise it agrees. Whatever closes the sheet by moving it to Hidden, a swipe that started on the title, footer or handle or a tap on the dimmed area, then only closes the keyboard, and the sheet springs back to open. The decision itself is a small function (keyboard showing, target is Hidden) so it can be unit tested. *Alternative:* intercept pointers on the title and footer one by one; rejected as more code that must track the sheet's layout.

**Save, Cancel and other programmatic closes bypass the veto.** They call `sheetState.hide()` through `requestClose`, which may also be checked by the veto, so `requestClose` sets a "closing" flag that the veto lets through before it hides the sheet. A UI test presses Cancel and Save with a keyboard-visible stand-in to prove the sheet still closes.

**Swipes on the dimmed area are a spike.** The dimmed area only reacts to taps, so a swipe there neither closes the sheet nor reaches our code. Handling it needs a pointer observer over the whole window of the sheet, which differs on Android (the sheet is a dialog window) and on iOS. The spike tries it on the Android emulator (driving swipes with `adb`) and on the iOS simulator and records the result in the section below. If it cannot be made to work on both platforms, the fallback is: swipes on the dimmed area keep doing nothing, taps and sheet drags still only close the keyboard, and the spec's "Swipe down outside the sheet" scenario is removed in the same change.

**Spike result.** On the devices the user tried, a swipe down on the dimmed area while the keyboard was open already closed only the keyboard and left the sheet open, with no code beyond the veto and the body guard, so no window-level handling was needed and the fallback was not taken. I could not check this myself: `adb` cannot deliver a drag the sheet acts on, and nothing here can inject touches into the iOS simulator. The result is the user's report, and it does not say which platform each check was run on.

## Risks / Trade-offs

- [The scroll hook may not fire when the body has nothing to scroll] -> The spec has a scenario for it; manual check on both platforms. If it does not fire, add a drag detector on the body for that case.
- [A stuck latch would swallow the next swipe] -> It is released in post-fling, which ends every drag; unit tests cover a gesture with and without a fling, and the manual check swipes twice in a row.
- [Platform keyboard timing: the keyboard inset can lag the keyboard by a frame] -> The latch is set on the first user scroll event, so a late value only matters for that first event; the manual check covers a quick swipe right after the keyboard opens.
- [The veto also blocks Save and Cancel while the keyboard is open] -> `requestClose` sets a flag the veto lets through; covered by a UI test and the device check.
- [A drag on the title moves the sheet with the finger and springs it back, instead of the sheet staying put] -> Accepted: it reads as a refused pull-down; only the body gesture is fully static.
- [The dimmed-area swipe may not work on both platforms] -> The spike decides; the fallback is written above.
- [Tapping the dimmed area now needs two taps to close a sheet that has the keyboard open] -> Intended, and only while the keyboard is open.
