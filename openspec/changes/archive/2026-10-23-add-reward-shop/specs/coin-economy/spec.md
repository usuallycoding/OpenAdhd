# Spec Delta

## MODIFIED Requirements

### Requirement: Append-only coin ledger
Every change to the user's coins SHALL be recorded as a ledger entry with an amount, a timestamp and a reason identifying what earned or spent it, and existing entries MUST NOT be edited or removed.

#### Scenario: Reward recorded
- **WHEN** a tier 2 entry named "Reply to emails" without subtasks is completed
- **THEN** a ledger entry of +10 with a task-completed reason referencing that entry is added

#### Scenario: Subtask reward recorded
- **WHEN** a subtask worth 7 coins is completed
- **THEN** a ledger entry of +7 with a subtask-completed reason referencing that subtask is added

#### Scenario: Redemption recorded
- **WHEN** a reward costing 50 coins is redeemed
- **THEN** a ledger entry of -50 with a reward-redeemed reason referencing that redemption is added

#### Scenario: History preserved
- **WHEN** the source template of a ledger entry is deleted
- **THEN** the ledger entry remains unchanged

### Requirement: Balance
The coin balance SHALL equal the sum of all ledger entry amounts, earnings and spending alike, and be shown in the topbar.

#### Scenario: Balance after rewards
- **WHEN** the user completes a tier 1 entry and a tier 4 entry
- **THEN** the balance increases by 45 and the topbar shows the new balance

#### Scenario: New user
- **WHEN** the app starts with no data
- **THEN** the balance is 0

#### Scenario: Balance after spending
- **WHEN** the user has earned 100 coins and redeems a reward costing 50
- **THEN** the balance is 50 and the topbar shows 50

## ADDED Requirements

### Requirement: Spending cannot overdraw the balance
A spend SHALL be refused, with no change to the ledger or the balance, when its amount is larger than the current balance.

#### Scenario: Overdraw refused
- **WHEN** the balance is 30 and a spend of 50 is attempted
- **THEN** nothing is recorded and the balance stays 30

#### Scenario: Spend the whole balance
- **WHEN** the balance is 50 and a spend of 50 is attempted
- **THEN** the spend is recorded and the balance is 0
