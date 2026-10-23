# Spec Delta

## Purpose

Lets the user spend coins on a surprise: a mystery roll costs a tier's price and gives a random reward from that tier, with a small chance of a better one, so spending is exciting and never gives less than what was paid for.

## ADDED Requirements

### Requirement: Mystery roll options
The Shop SHALL offer a mystery roll for each of the tiers 1 to 4, priced like that tier: 20, 50, 120 and 300 coins. A roll MUST NOT be offered for tier 5, and tier 5 rewards MUST NOT be given by any roll.

#### Scenario: Four rolls
- **WHEN** the user opens the Rewards tab of the Shop
- **THEN** it shows a mystery roll for tiers 1, 2, 3 and 4 with prices 20, 50, 120 and 300

#### Scenario: Unavailable rolls
- **WHEN** the balance is 30 and tier 3 has no rewards
- **THEN** the tier 2 roll shows that 20 more coins are needed, the tier 3 roll shows that the tier has no rewards, and neither can be rolled

### Requirement: Roll outcome
A roll of a tier SHALL give one reward chosen at random, with equal chance for each reward, from that tier. With a 1 in 6 chance it SHALL instead give a reward chosen at random from the next tier up, if that tier has rewards. A tier 4 roll MUST NOT upgrade, and a roll MUST NOT give a reward from a lower tier than the one rolled.

#### Scenario: Usual outcome
- **WHEN** the user rolls tier 2 and the roll does not upgrade
- **THEN** the reward given is one of the tier 2 rewards

#### Scenario: Lucky upgrade
- **WHEN** the user rolls tier 2, tier 3 has rewards and the roll upgrades
- **THEN** the reward given is one of the tier 3 rewards

#### Scenario: Upgrade without a higher tier
- **WHEN** the roll would upgrade but the next tier has no rewards
- **THEN** the reward given is one of the rolled tier's rewards and the roll is not reported as an upgrade

#### Scenario: Tier 4 never upgrades
- **WHEN** the user rolls tier 4
- **THEN** the reward given is always one of the tier 4 rewards

### Requirement: Cost of a roll
A roll SHALL deduct the price of the rolled tier from the balance, whichever reward is given, and MUST be refused, with no change to the balance, the ledger or the history, when the balance is lower than that price or the rolled tier has no rewards.

#### Scenario: Successful roll
- **WHEN** the balance is 80 and the user rolls tier 2
- **THEN** the balance becomes 30 and a reward is given

#### Scenario: Upgrade costs the same
- **WHEN** the user rolls tier 2 and the roll upgrades to a tier 3 reward
- **THEN** only the tier 2 price of 50 coins is deducted

#### Scenario: Not enough coins
- **WHEN** the balance is 30 and a tier 2 roll is attempted
- **THEN** nothing is spent and no reward is given

#### Scenario: Empty tier
- **WHEN** a tier has no rewards and a roll of it is attempted
- **THEN** nothing is spent and no reward is given

### Requirement: Confirming a roll
The app SHALL ask for confirmation, showing the rolled tier and its price, before spending coins on a roll, and a single confirmation MUST roll exactly once.

#### Scenario: Cancelling
- **WHEN** the user opens the confirmation for a roll and cancels
- **THEN** nothing is spent and no reward is given

#### Scenario: Confirming twice quickly
- **WHEN** the user activates the confirm button twice in quick succession
- **THEN** exactly one roll happens and the price is deducted once

### Requirement: Roll reveal
After a roll the app SHALL show which reward was won, and when the roll upgraded it SHALL also say that it was a lucky upgrade and from which tier to which.

#### Scenario: Plain reveal
- **WHEN** a tier 2 roll gives the reward "Movie night" without an upgrade
- **THEN** the reveal shows "Movie night" and no upgrade message

#### Scenario: Upgrade reveal
- **WHEN** a tier 2 roll upgrades and gives the tier 3 reward "Dinner out"
- **THEN** the reveal shows "Dinner out" and says it is a lucky upgrade from tier 2 to tier 3

### Requirement: A roll is a redemption
The reward won SHALL be recorded as a redemption with its name, tier and energy level and the roll price as the price paid, together with the fact that it came from a mystery roll and which tier was rolled.

#### Scenario: Roll recorded
- **WHEN** the user rolls tier 2 and wins "Movie night"
- **THEN** the redemption history has an entry for "Movie night" with a price paid of 50 that is marked as a mystery roll of tier 2
