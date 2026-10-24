# Spec Delta

## ADDED Requirements

### Requirement: Choose the day of the redemption history
The redemption history SHALL show one day at a time. It MUST open on today's date, show the chosen date in a date field, and let the user move to the previous or the next day and pick a day from a calendar dialog. The chosen day MUST never be after today: the next-day control MUST NOT be offered while today is shown. The chosen day is kept while the app is running and is not saved, and until the user chooses a day it MUST stay on the current date, also after midnight.

#### Scenario: Default day
- **WHEN** the user opens the History tab of the Shop
- **THEN** the date field shows today's date and the list shows today's redemptions

#### Scenario: Previous and next day
- **WHEN** the user moves to the previous day and then to the next day
- **THEN** yesterday is shown and then today, and no next-day control is offered on today

#### Scenario: Kept while the app runs
- **WHEN** the user chooses another day, opens Today, and returns to the Shop History
- **THEN** the chosen day is still shown

#### Scenario: After midnight
- **WHEN** the user has not chosen a day and the date changes to the next day
- **THEN** the History shows the new date

#### Scenario: After a restart
- **WHEN** the app is restarted
- **THEN** the History tab opens on today's date

### Requirement: Only days with redemptions can be picked
In the calendar dialog, a day SHALL be selectable only if it is today or earlier and at least one redemption was made on it, by the device's local date. Every other day, including future days and earlier days with no redemptions, MUST NOT be selectable. Picking a day takes effect when the user confirms with OK, and cancelling keeps the shown day.

#### Scenario: Day with a redemption
- **WHEN** a reward was redeemed on the 20th and the 20th is before today
- **THEN** the 20th can be selected in the calendar

#### Scenario: Day without a redemption
- **WHEN** nothing was redeemed on the 21st
- **THEN** the 21st cannot be selected in the calendar

#### Scenario: Today with a redemption
- **WHEN** a reward was redeemed today
- **THEN** today can be selected in the calendar

#### Scenario: Future days
- **WHEN** the calendar is open
- **THEN** every day after today cannot be selected

#### Scenario: Cancelling
- **WHEN** the user selects a day in the calendar and cancels
- **THEN** the shown day does not change

## MODIFIED Requirements

### Requirement: Redemption keeps what was paid
Each redemption SHALL record the reward's name, description, tier, energy level and the price paid when it was bought, and whether it was a purchase or a mystery roll and, for a roll, the tier that was rolled. None of this MUST change when the reward is later edited or deleted.

#### Scenario: Price changed later
- **WHEN** a reward bought for 50 coins is edited to tier 3
- **THEN** its redemption still shows 50 coins

#### Scenario: Roll details kept
- **WHEN** a reward won by rolling tier 2 is later deleted
- **THEN** its redemption still shows the name, the 50 coins paid and that it was a tier 2 mystery roll

#### Scenario: Description kept
- **WHEN** a reward described as "Pick anything, no guilt" is redeemed and its description is then edited
- **THEN** its redemption still shows "Pick anything, no guilt"

#### Scenario: Description of a rolled reward
- **WHEN** a mystery roll wins a reward that has a description
- **THEN** its redemption records that reward's description

### Requirement: Redemption history
The app SHALL show a history of redemptions inside the shop for the chosen day, newest first, each with its name, the reward's description when it has one, the price paid and when it was redeemed, and for mystery rolls also a note that it was a roll, the tier rolled and whether it was a lucky upgrade. A day on which nothing was redeemed SHALL show "Nothing redeemed on this day". The history MUST NOT offer a way to change or undo a redemption.

#### Scenario: History order
- **WHEN** the user redeemed rewards A and then B today
- **THEN** the history of today lists B before A

#### Scenario: Only the chosen day
- **WHEN** a reward was redeemed yesterday and another today
- **THEN** the history of today lists only the second, and the history of yesterday lists only the first

#### Scenario: No purchases yet
- **WHEN** nothing was redeemed on the chosen day, or nothing has been redeemed at all
- **THEN** the history shows "Nothing redeemed on this day"

#### Scenario: Rolls are marked
- **WHEN** the user rolled tier 2 today and won a tier 3 reward
- **THEN** its history entry shows the reward name, the 50 coins paid, that it was a mystery roll, and that it was a lucky upgrade

#### Scenario: Description shown
- **WHEN** a redemption's reward has the description "Pick anything, no guilt"
- **THEN** its history entry shows that description under the name

#### Scenario: No description
- **WHEN** a redemption's reward has no description
- **THEN** its history entry shows no description line
