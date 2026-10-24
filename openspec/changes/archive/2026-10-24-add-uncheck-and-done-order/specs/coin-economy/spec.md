# Spec Delta

## MODIFIED Requirements

### Requirement: Append-only coin ledger
Every change to the user's coins SHALL be recorded as a ledger entry with an amount, a timestamp and a reason identifying what earned, spent or took back the coins, and existing entries MUST NOT be edited or removed.

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

#### Scenario: Undo recorded
- **WHEN** a subtask that paid 7 coins is unchecked
- **THEN** a separate ledger entry of -7 with an undone reason referencing that subtask is added, and the earlier entries are unchanged

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
- **THEN** the spend is refused and nothing is recorded

## ADDED Requirements

### Requirement: Undoing a completion takes back what it paid
Unchecking a completed task or subtask SHALL add one ledger entry whose amount is minus the net total already recorded for that task or subtask (its reward and any critical-hit bonus, less earlier undos). Unchecking an item whose net total is zero MUST NOT add an entry.

#### Scenario: Reward only
- **WHEN** a 20-coin task is unchecked
- **THEN** one entry of -20 is added

#### Scenario: Reward and bonus
- **WHEN** a subtask that paid 7 and a 7 bonus is unchecked
- **THEN** one entry of -14 is added

#### Scenario: Second round
- **WHEN** a 20-coin task is checked, unchecked, checked and unchecked again
- **THEN** the ledger holds +20, -20, +20, -20 and the balance is where it started

#### Scenario: Nothing to take back
- **WHEN** an item is unchecked that has no recorded payout
- **THEN** no ledger entry is added
