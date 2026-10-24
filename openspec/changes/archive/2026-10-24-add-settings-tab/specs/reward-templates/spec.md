# Spec Delta

## MODIFIED Requirements

### Requirement: Price by tier
Rewards in tiers 1 to 4 SHALL cost a fixed price set by their tier in Settings: by default 20 coins for tier 1, 50 for tier 2, 120 for tier 3 and 300 for tier 4. A tier 5 reward SHALL cost the custom price the user entered, which MUST be a whole number larger than the tier 4 price.

#### Scenario: Fixed tier price
- **WHEN** a reward is saved in tier 3
- **THEN** its price is 120 coins

#### Scenario: Custom tier 5 price
- **WHEN** the user saves a tier 5 reward with a custom price of 500
- **THEN** its price is 500 coins

#### Scenario: Tier 5 price must exceed tier 4
- **WHEN** the user tries to save a tier 5 reward with a custom price of 300 or less
- **THEN** the reward is not saved and the user is told a tier 5 price must be larger than 300

#### Scenario: Smallest tier 5 price
- **WHEN** the user saves a tier 5 reward with a custom price of 301
- **THEN** the reward is saved with a price of 301 coins

#### Scenario: Tier 5 needs a price
- **WHEN** the user tries to save a tier 5 reward without entering a price
- **THEN** the reward is not saved and the user is told a price is required

#### Scenario: Changed tier price
- **WHEN** the tier 4 price in Settings is 400 and the user tries to save a tier 5 reward with a custom price of 400
- **THEN** the reward is not saved and the user is told a tier 5 price must be larger than 400
