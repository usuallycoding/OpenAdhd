# Spec Delta

## MODIFIED Requirements

### Requirement: Base coin reward by tier
Every task SHALL have a reward pool determined by its tier and the task tier payouts in Settings: by default 5 coins for tier 1, 10 for tier 2, 20 for tier 3 and 40 for tier 4. The pool MUST be fixed when the entry is assigned. A task without subtasks awards its whole pool when completed. A task with subtasks awards its pool only through its subtasks and MUST NOT award anything extra when it becomes completed.

#### Scenario: Tier 3 task
- **WHEN** the user completes a tier 3 entry that has no subtasks
- **THEN** 20 coins are awarded

#### Scenario: No bonus for finishing a task with subtasks
- **WHEN** the user completes the last subtask of a tier 3 entry that has 3 subtasks
- **THEN** the coins awarded for that entry in total equal 20 and nothing extra is awarded for the entry itself

#### Scenario: Changed payout
- **WHEN** the tier 3 payout in Settings is 25 and the user assigns a tier 3 template and completes it
- **THEN** 25 coins are awarded

### Requirement: Spending may borrow up to the debt limit
A spend SHALL be allowed when the balance after it would be no lower than minus the debt limit from Settings, 500 coins by default, and refused, with no change to the ledger or the balance, when it would take the balance lower. There MUST be no interest, fee or due date: coins earned later simply raise the balance.

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

#### Scenario: Changed limit
- **WHEN** the debt limit in Settings is 100 and the balance is -80
- **THEN** a spend of 30 is refused and a spend of 20 is recorded
