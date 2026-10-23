# Spec Delta

## MODIFIED Requirements

### Requirement: Base coin reward by tier
Every task SHALL have a reward pool determined by its tier: 5 coins for tier 1, 10 for tier 2, 20 for tier 3 and 40 for tier 4. A task without subtasks awards its whole pool when completed. A task with subtasks awards its pool only through its subtasks and MUST NOT award anything extra when it becomes completed.

#### Scenario: Tier 3 task
- **WHEN** the user completes a tier 3 entry that has no subtasks
- **THEN** 20 coins are awarded

#### Scenario: No bonus for finishing a task with subtasks
- **WHEN** the user completes the last subtask of a tier 3 entry that has 3 subtasks
- **THEN** the coins awarded for that entry in total equal 20 and nothing extra is awarded for the entry itself

### Requirement: Append-only coin ledger
Every change to the user's coins SHALL be recorded as a ledger entry with an amount, a timestamp and a reason identifying what earned it, and existing entries MUST NOT be edited or removed.

#### Scenario: Reward recorded
- **WHEN** a tier 2 entry named "Reply to emails" without subtasks is completed
- **THEN** a ledger entry of +10 with a task-completed reason referencing that entry is added

#### Scenario: Subtask reward recorded
- **WHEN** a subtask worth 7 coins is completed
- **THEN** a ledger entry of +7 with a subtask-completed reason referencing that subtask is added

#### Scenario: History preserved
- **WHEN** the source template of a ledger entry is deleted
- **THEN** the ledger entry remains unchanged

## ADDED Requirements

### Requirement: Reward pool split across subtasks
The pool of an entry with subtasks SHALL be divided among its subtasks in order: each gets the pool divided by the number of subtasks, rounded down, and the leftover coins go one each to the first subtasks. Because a task never has more subtasks than its pool, every subtask is worth at least 1 coin and the shares add up exactly to the pool. Shares MUST be fixed when the entry is assigned.

#### Scenario: Even split
- **WHEN** a tier 4 entry has 4 subtasks
- **THEN** each subtask is worth 10 coins

#### Scenario: Uneven split
- **WHEN** a tier 3 entry has 3 subtasks
- **THEN** the subtasks are worth 7, 7 and 6 coins in that order

#### Scenario: Pool equal to the number of subtasks
- **WHEN** a tier 1 entry has 5 subtasks
- **THEN** each subtask is worth 1 coin and the shares add up to the pool of 5

#### Scenario: Largest entry
- **WHEN** a tier 4 entry has the maximum of 10 subtasks
- **THEN** each subtask is worth 4 coins and the entry pays 40 coins in total

#### Scenario: Fixed at assignment
- **WHEN** a template's subtasks or tier are edited after an entry was created from it
- **THEN** the existing entry's subtask shares do not change
