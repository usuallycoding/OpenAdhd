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
- **THEN** the shop shows an empty state that points to creating rewards on the Templates screen

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
Each redemption SHALL record the reward's name, tier, energy level and the price paid when it was bought, and whether it was a purchase or a mystery roll and, for a roll, the tier that was rolled. None of this MUST change when the reward is later edited or deleted.

#### Scenario: Price changed later
- **WHEN** a reward bought for 50 coins is edited to tier 3
- **THEN** its redemption still shows 50 coins

#### Scenario: Roll details kept
- **WHEN** a reward won by rolling tier 2 is later deleted
- **THEN** its redemption still shows the name, the 50 coins paid and that it was a tier 2 mystery roll

### Requirement: Redemption history
The app SHALL show a history of redemptions inside the shop, newest first, each with its name, price paid and when it was redeemed, and for mystery rolls also a note that it was a roll, the tier rolled and whether it was a lucky upgrade.

#### Scenario: History order
- **WHEN** the user has redeemed rewards A and then B
- **THEN** the history lists B before A

#### Scenario: No purchases yet
- **WHEN** nothing has been redeemed
- **THEN** the history shows an empty state

#### Scenario: Rolls are marked
- **WHEN** the user rolled tier 2 and won a tier 3 reward
- **THEN** its history entry shows the reward name, the 50 coins paid, that it was a mystery roll, and that it was a lucky upgrade

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
