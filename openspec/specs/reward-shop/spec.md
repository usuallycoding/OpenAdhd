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
The app SHALL show for each reward whether the current balance covers its price, and when it does not, how many more coins are needed, and MUST NOT allow redeeming a reward the balance does not cover.

#### Scenario: Not enough coins
- **WHEN** the balance is 30 and a reward costs 50
- **THEN** the reward shows that 20 more coins are needed and cannot be redeemed

#### Scenario: Exactly enough
- **WHEN** the balance is 50 and a reward costs 50
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
Each redemption SHALL record the reward's name, tier, energy level and the price paid when it was bought, and these MUST NOT change when the reward is later edited or deleted.

#### Scenario: Price changed later
- **WHEN** a reward bought for 50 coins is edited to tier 3
- **THEN** its redemption still shows 50 coins

### Requirement: Redemption history
The app SHALL show a history of redemptions inside the shop, newest first, each with its name, price paid and when it was redeemed.

#### Scenario: History order
- **WHEN** the user has redeemed rewards A and then B
- **THEN** the history lists B before A

#### Scenario: No purchases yet
- **WHEN** nothing has been redeemed
- **THEN** the history shows an empty state
