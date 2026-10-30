## MODIFIED Requirements

### Requirement: Only days with redemptions can be picked
In the calendar dialog, every day that is today or earlier SHALL be selectable by the device's local date, whether or not a redemption was made on it. Days with at least one redemption MUST be marked with a dot under the day number, and days without redemptions MUST NOT. Future days MUST NOT be selectable. The marker MUST stay visible on the selected day. Picking a day takes effect when the user confirms with OK, and cancelling keeps the shown day.

#### Scenario: Day with a redemption
- **WHEN** a reward was redeemed on the 20th and the 20th is before today
- **THEN** the 20th shows a dot and can be selected in the calendar

#### Scenario: Day without a redemption
- **WHEN** nothing was redeemed on the 21st and the 21st is before today
- **THEN** the 21st shows no dot and can be selected, and choosing it shows "Nothing redeemed on this day"

#### Scenario: Today with a redemption
- **WHEN** a reward was redeemed today
- **THEN** today shows a dot and can be selected in the calendar

#### Scenario: Today without a redemption
- **WHEN** nothing was redeemed today
- **THEN** today shows no dot and can be selected in the calendar

#### Scenario: Future days
- **WHEN** the calendar is open
- **THEN** every day after today cannot be selected

#### Scenario: Cancelling
- **WHEN** the user selects a day in the calendar and cancels
- **THEN** the shown day does not change
