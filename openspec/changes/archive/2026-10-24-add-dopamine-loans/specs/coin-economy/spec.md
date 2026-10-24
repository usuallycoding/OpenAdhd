# Spec Delta

## MODIFIED Requirements

### Requirement: Balance
The coin balance SHALL equal the sum of all ledger entry amounts, earnings, spending and coins taken back alike, and be shown in the topbar. The balance MAY be negative.

#### Scenario: Balance after rewards
- **WHEN** the user completes a tier 1 entry and a tier 4 entry
- **THEN** the balance increases by 45 and the topbar shows the new balance

#### Scenario: New user
- **WHEN** the app starts with no data
- **THEN** the balance is 0

#### Scenario: Balance after spending
- **WHEN** the user has earned 100 coins and redeems a reward costing 50
- **THEN** the balance is 50 and the topbar shows 50

#### Scenario: Balance below zero
- **WHEN** the user has earned 20 coins, redeemed a reward costing 20, and then unchecks the task that paid 20
- **THEN** the balance is -20 and the topbar shows -20

#### Scenario: Spending while negative
- **WHEN** the balance is -20 and a spend of 5 is attempted
- **THEN** the spend is recorded and the balance is -25

## ADDED Requirements

### Requirement: Spending may borrow up to the debt limit
A spend SHALL be allowed when the balance after it would be no lower than minus 500 coins, and refused, with no change to the ledger or the balance, when it would take the balance lower. There MUST be no interest, fee or due date: coins earned later simply raise the balance.

#### Scenario: Borrowing
- **WHEN** the balance is 30 and a spend of 50 is attempted
- **THEN** the spend is recorded and the balance is -20

#### Scenario: Exactly at the limit
- **WHEN** the balance is -450 and a spend of 50 is attempted
- **THEN** the spend is recorded and the balance is -500

#### Scenario: Beyond the limit
- **WHEN** the balance is -480 and a spend of 50 is attempted
- **THEN** nothing is recorded and the balance stays -480

#### Scenario: Earning pays the debt
- **WHEN** the balance is -20 and the user completes a 20-coin task
- **THEN** the balance is 0 and no other entry is needed

#### Scenario: Correction below the limit
- **WHEN** the balance is -490 and the user unchecks a completed 20-coin task
- **THEN** the balance becomes -510, and a further spend is refused until earnings bring it back within the limit

### Requirement: Debt is visible
While the balance is below zero, the top bar SHALL show it in a warning colour.

#### Scenario: In debt
- **WHEN** the balance is -20
- **THEN** the top bar shows -20 in the error colour

#### Scenario: Back in credit
- **WHEN** the balance is 0 or more
- **THEN** the top bar uses its usual colour

## REMOVED Requirements

### Requirement: Spending cannot overdraw the balance
**Reason**: Spending may now go below zero, up to a debt limit, so a spend larger than the balance is no longer always refused.
**Migration**: Replaced by "Spending may borrow up to the debt limit".
