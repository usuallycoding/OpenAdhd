# Spec Delta

## MODIFIED Requirements

### Requirement: Mystery roll options
The Shop SHALL offer a mystery roll for each of the tiers 1 to 4, priced like that tier according to the reward tier prices in Settings: 20, 50, 120 and 300 coins by default. A roll MUST NOT be offered for tier 5, and tier 5 rewards MUST NOT be given by any roll.

#### Scenario: Four rolls
- **WHEN** the user opens the Rewards tab of the Shop
- **THEN** it shows a mystery roll for tiers 1, 2, 3 and 4 with prices 20, 50, 120 and 300

#### Scenario: Unavailable rolls
- **WHEN** the balance is -480 and tier 3 has no rewards
- **THEN** the tier 2 roll shows that 30 more coins are needed, the tier 3 roll shows that the tier has no rewards, and neither can be rolled

#### Scenario: Changed prices
- **WHEN** the tier 2 price in Settings is 60
- **THEN** the tier 2 roll is priced 60 coins

### Requirement: Roll outcome
A roll of a tier SHALL give one reward chosen at random, with equal chance for each reward, from that tier. With the upgrade chance from Settings, 1 in 6 by default, it SHALL instead give a reward chosen at random from the next tier up, if that tier has rewards. A tier 4 roll MUST NOT upgrade, and a roll MUST NOT give a reward from a lower tier than the one rolled.

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

#### Scenario: Changed chance
- **WHEN** the upgrade chance in Settings is 1 in 3
- **THEN** a roll upgrades with a 1 in 3 chance and the Shop's odds text says 1 in 3

### Requirement: Cost of a roll
A roll SHALL deduct the price of the rolled tier from the balance, whichever reward is given, and MUST be refused, with no change to the balance, the ledger or the history, when the balance after paying would be lower than minus the debt limit from Settings (500 coins by default) or the rolled tier has no rewards.

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
