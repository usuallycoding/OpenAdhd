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

#### Scenario: Critical hit recorded
- **WHEN** a subtask worth 7 coins is completed as a critical hit
- **THEN** besides the +7 reward entry, a separate ledger entry of +7 with a critical-hit reason referencing that subtask is added

#### Scenario: Redemption recorded
- **WHEN** a reward costing 50 coins is redeemed
- **THEN** a ledger entry of -50 with a reward-redeemed reason referencing that redemption is added

#### Scenario: Mystery roll recorded
- **WHEN** a tier 2 mystery roll is made
- **THEN** a ledger entry of -50 with a mystery-roll reason referencing the resulting redemption is added

#### Scenario: History preserved
- **WHEN** the source template of a ledger entry is deleted
- **THEN** the ledger entry remains unchanged
