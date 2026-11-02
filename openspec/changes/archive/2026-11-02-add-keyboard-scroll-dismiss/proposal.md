# Proposal

## Why

On Android and iOS, a form sheet with a focused text field and the keyboard open can only be rid of the keyboard by pressing Done or tapping elsewhere, and a swipe down on it closes the whole sheet and the work in it. The natural gesture, swiping down to put the keyboard away, either does nothing in the forms or, in the Add to today picker, hides the keyboard and then lets the same swipe drag the sheet closed.

## What Changes

- In every bottom sheet (task and reward forms, the reminder sheet, the settings sheets and the Add to today picker), a downward swipe while the on-screen keyboard is open closes only the keyboard, wherever on the screen it is made: on the sheet's body, title or Save/Cancel row, or on the dimmed area outside the sheet. The sheet stays open.
- A tap on the dimmed area while the keyboard is open also closes only the keyboard.
- With the keyboard gone, the next downward swipe or tap outside closes the sheet as it does today. With no keyboard open, nothing changes. Save and Cancel always close the sheet.
- Forms get no tap-to-dismiss inside the sheet: a tap on a text field must keep its focus. The picker keeps its existing tap behavior.
- The desktop is unchanged, since it has no on-screen keyboard.

Out of scope: the Android-only interactive keyboard follow (`imeNestedScroll`), and any change to the keyboard behavior of the Templates and Rewards list screens (they are not sheets).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `form-sheets`: a new requirement that, with the keyboard open, a downward swipe anywhere or a tap on the dimmed area puts away only the keyboard.
- `day-planner`: the picker's "keyboard can be put away" requirement says that scrolling the results leaves the picker open.

## Impact

- `shared/src/ui/components/DismissKeyboard.kt`: the body's scroll guard (done): it puts the keyboard away on scroll and keeps the leftover scroll and fling from the sheet while the keyboard is open.
- `shared/src/ui/components/FormSheet.kt`: the guard on every body (done), and a veto on the sheet's move to Hidden while the keyboard is open, which hides the keyboard instead and covers swipes on the title and footer and taps on the dimmed area. Save, Cancel and the other programmatic closes bypass the veto.
- Swipes on the dimmed area outside the sheet need handling beyond the sheet's own gestures; a spike on the Android emulator and the iOS simulator decides how, or whether it works on both.
- Unit tests for the scroll and veto logic in `shared/test`. No data or dependency changes.
- Needs a manual check on an Android phone and an iPhone: desktop UI tests cannot show a keyboard.
