# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
