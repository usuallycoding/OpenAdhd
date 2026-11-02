# reward-shop Specification

## Purpose

Lets the user spend earned coins on rewards, see at a glance what they can afford, and look back at what they have bought.

## Requirements

### Requirement: Shop listing
The app SHALL provide a Shop screen that lists all reward templates grouped by tier from 1 to 5, each with its name, description, energy level and price. Within tier 5, rewards SHALL be ordered by price from lowest to highest.

#### Scenario: Grouped by tier
- **WHEN** rewards exist in tiers 1, 2 and 5
- **THEN** the shop shows those three tiers in order, each with its rewards and their prices

#### Scenario: Tier 5 ordering
- **WHEN** the shop holds tier 5 rewards priced 900 and 500
- **THEN** the 500 reward is listed before the 900 reward

#### Scenario: Empty shop
- **WHEN** no reward templates exist
- **THEN** the shop shows "The shop is empty." with a Take me there button under it

#### Scenario: Take me there
- **WHEN** no reward templates exist and the user presses Take me there
- **THEN** the Templates screen is shown on its Rewards tab and the form to create a reward template is open

### Requirement: Affordability
The app SHALL treat a reward as affordable when the balance after paying its price would be no lower than the debt limit of minus 500 coins, and MUST NOT allow redeeming one that is not. For a reward that is not affordable it SHALL show how many more coins are needed to bring the balance back to the limit.

#### Scenario: Not enough coins
- **WHEN** the balance is -480 and a reward costs 50
- **THEN** the reward shows that 30 more coins are needed and cannot be redeemed

#### Scenario: Exactly enough
- **WHEN** the balance is 50 and a reward costs 50
- **THEN** the reward can be redeemed

#### Scenario: Borrowing
- **WHEN** the balance is 30 and a reward costs 50
- **THEN** the reward can be redeemed and shows no coins needed

#### Scenario: Exactly at the limit
- **WHEN** the balance is -450 and a reward costs 50
- **THEN** the reward can be redeemed

### Requirement: Redeem a reward
The app SHALL let the user redeem an affordable reward after confirming the reward and its price. Redeeming MUST deduct the price from the balance, record a redemption, and add a ledger entry of minus the price, as one action that happens exactly once per confirmation.

#### Scenario: Successful redemption
- **WHEN** the balance is 80 and the user confirms redeeming a tier 2 reward costing 50
- **THEN** the balance becomes 30 and the reward appears first in the redemption history

#### Scenario: Cancelling
- **WHEN** the user opens the confirmation for a reward and cancels
- **THEN** nothing is spent and no redemption is recorded

#### Scenario: Confirming twice quickly
- **WHEN** the user activates the confirm button twice in quick succession
- **THEN** exactly one redemption is recorded and the price is deducted once

#### Scenario: Buying the same reward again
- **WHEN** the balance is 100 and the user redeems a reward costing 50 twice, confirming each time
- **THEN** two redemptions are recorded and the balance is 0

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

### Requirement: Redeeming into debt
When redeeming a reward would take the balance below zero, the confirmation SHALL say how many coins in debt the purchase leaves the user, and the redemption MUST be recorded exactly as for any other purchase.

#### Scenario: Confirmation mentions the debt
- **WHEN** the balance is 30 and the user opens the confirmation for a reward costing 50
- **THEN** the confirmation says the purchase puts the user 20 coins in debt

#### Scenario: No debt, no warning
- **WHEN** the balance is 80 and the user opens the confirmation for a reward costing 50
- **THEN** the confirmation says nothing about debt

#### Scenario: Recorded like any purchase
- **WHEN** the user confirms redeeming a 50-coin reward with a balance of 30
- **THEN** a redemption is recorded with price 50, a ledger entry of -50 is added, and the balance is -20
