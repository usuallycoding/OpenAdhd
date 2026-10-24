# Spec Delta

## MODIFIED Requirements

### Requirement: Mystery roll options
The Shop SHALL offer a mystery roll for each of the tiers 1 to 4, priced like that tier: 20, 50, 120 and 300 coins. A roll MUST NOT be offered for tier 5, and tier 5 rewards MUST NOT be given by any roll.

#### Scenario: Four rolls
- **WHEN** the user opens the Rewards tab of the Shop
- **THEN** it shows a mystery roll for tiers 1, 2, 3 and 4 with prices 20, 50, 120 and 300

#### Scenario: Unavailable rolls
- **WHEN** the balance is -480 and tier 3 has no rewards
- **THEN** the tier 2 roll shows that 30 more coins are needed, the tier 3 roll shows that the tier has no rewards, and neither can be rolled

### Requirement: Cost of a roll
A roll SHALL deduct the price of the rolled tier from the balance, whichever reward is given, and MUST be refused, with no change to the balance, the ledger or the history, when the balance after paying would be lower than the debt limit of minus 500 coins or the rolled tier has no rewards.

#### Scenario: Successful roll
- **WHEN** the balance is 80 and the user rolls tier 2
- **THEN** the balance becomes 30 and a reward is given

#### Scenario: Upgrade costs the same
- **WHEN** the user rolls tier 2 and the roll upgrades to a tier 3 reward
- **THEN** only the tier 2 price of 50 coins is deducted

#### Scenario: Not enough coins
- **WHEN** the balance is -480 and a tier 2 roll is attempted
- **THEN** nothing is spent and no reward is given

#### Scenario: Empty tier
- **WHEN** a tier has no rewards and a roll of it is attempted
- **THEN** nothing is spent and no reward is given

#### Scenario: Rolling on credit
- **WHEN** the balance is 30 and a tier 2 roll is attempted
- **THEN** the roll is made, a reward is given and the balance is -20

## ADDED Requirements

### Requirement: Rolling into debt
When a roll would take the balance below zero, its confirmation SHALL say how many coins in debt the roll leaves the user.

#### Scenario: Confirmation mentions the debt
- **WHEN** the balance is 30 and the user opens the confirmation for a tier 2 roll
- **THEN** the confirmation says the roll puts the user 20 coins in debt

#### Scenario: No debt, no warning
- **WHEN** the balance is 80 and the user opens the confirmation for a tier 2 roll
- **THEN** the confirmation says nothing about debt
