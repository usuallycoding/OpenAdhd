# Spec Delta

## ADDED Requirements

### Requirement: Swiping down puts away only the keyboard
While the on-screen keyboard is open in a bottom sheet, a downward swipe by the user anywhere on the screen (on the sheet's body, title or Save and Cancel row, or on the dimmed area outside the sheet) SHALL hide the keyboard and take focus out of the field, and the sheet MUST stay open. A tap on the dimmed area SHALL do the same. With the keyboard hidden, the next downward swipe or tap outside SHALL close the sheet as usual, and Save and Cancel MUST always close it.

#### Scenario: Scroll down on the body
- **WHEN** a field in a form sheet has focus with the keyboard open and the user scrolls down on the sheet body
- **THEN** the keyboard is hidden and the sheet stays open where it was

#### Scenario: Swipe down on the title or the footer
- **WHEN** the keyboard is open and the user swipes down starting on the sheet's title or on its Save and Cancel row
- **THEN** the keyboard is hidden and the sheet stays open

#### Scenario: Swipe down outside the sheet
- **WHEN** the keyboard is open and the user swipes down starting on the dimmed area outside the sheet
- **THEN** the keyboard is hidden and the sheet stays open

#### Scenario: Tap outside the sheet
- **WHEN** the keyboard is open and the user taps the dimmed area outside the sheet
- **THEN** the keyboard is hidden and the sheet stays open

#### Scenario: Swipe down again
- **WHEN** the keyboard has been put away that way and the user swipes down on the sheet again, or taps outside it
- **THEN** the sheet closes as usual

#### Scenario: No keyboard
- **WHEN** no keyboard is open and the user swipes down on the sheet or taps outside it
- **THEN** the sheet closes as usual

#### Scenario: Save and Cancel with the keyboard open
- **WHEN** the keyboard is open and the user presses Cancel, or Save with valid input
- **THEN** the sheet closes

#### Scenario: Body with nothing to scroll
- **WHEN** the keyboard is open and the user drags down on a body that is too short to scroll
- **THEN** the keyboard is hidden and the sheet stays open

#### Scenario: Taps in forms keep focus
- **WHEN** the user taps a text field or empty space in a form's body
- **THEN** no keyboard is put away by that tap alone (only the picker does so)

#### Scenario: Desktop
- **WHEN** a sheet is swiped or scrolled on the desktop
- **THEN** it behaves as before, because there is no on-screen keyboard
