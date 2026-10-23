# reward-templates Specification

## Purpose

Lets the user keep a library of rewards they want to earn, each placed in a tier that sets its price and tagged with the energy it takes, to be bought in the shop.

## Requirements

### Requirement: Create a reward template
The app SHALL let the user create a reward template with a non-empty name, an optional description, a tier from 1 to 5, an energy level of Low, Medium or High, and, for tier 5 only, a custom price.

#### Scenario: Valid reward
- **WHEN** the user enters the name "Movie night", picks tier 2 and energy Low, and saves
- **THEN** the reward appears in the reward list with tier 2, energy Low and a price of 50 coins

#### Scenario: Empty name
- **WHEN** the user tries to save a reward whose name is empty or only whitespace
- **THEN** the reward is not saved and the user is told a name is required

### Requirement: Price by tier
Rewards in tiers 1 to 4 SHALL cost a fixed price set by their tier: 20 coins for tier 1, 50 for tier 2, 120 for tier 3 and 300 for tier 4. A tier 5 reward SHALL cost the custom price the user entered, which MUST be a whole number larger than 300.

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

### Requirement: Edit a reward template
The app SHALL let the user change the name, description, tier, energy level and custom price of an existing reward, and the reward's price MUST follow its new tier.

#### Scenario: Change tier
- **WHEN** the user edits a reward from tier 2 to tier 3 and saves
- **THEN** the list shows the reward at tier 3 with a price of 120 coins

#### Scenario: Leaving tier 5
- **WHEN** the user edits a tier 5 reward with a custom price of 500 to tier 2 and saves
- **THEN** the reward costs 50 coins and no custom price is kept

#### Scenario: Edit does not alter past purchases
- **WHEN** a reward that has been redeemed is edited
- **THEN** the redemption history keeps the name and price the reward had when it was bought

### Requirement: Delete a reward template
The app SHALL let the user delete a reward template, and deleting MUST NOT remove redemptions or ledger entries that came from it.

#### Scenario: Delete a redeemed reward
- **WHEN** the user deletes a reward that has been redeemed before
- **THEN** the reward leaves the reward list and the shop, and the redemption history and the balance are unchanged

### Requirement: List reward templates
The app SHALL show all reward templates on the Rewards tab of the Templates screen, each with its tier, energy level and price.

#### Scenario: Tasks and rewards tabs
- **WHEN** the user opens the Templates screen
- **THEN** it offers a Tasks tab and a Rewards tab, and the Rewards tab lists the reward templates

#### Scenario: Empty library
- **WHEN** no reward templates exist
- **THEN** the Rewards tab shows an empty state that invites the user to create one
