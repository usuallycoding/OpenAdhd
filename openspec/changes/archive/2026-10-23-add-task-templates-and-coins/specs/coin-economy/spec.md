# Spec Delta

## Purpose

Defines how completing tasks earns Dopamine Coins and how every coin movement is recorded, so the balance is always explainable.

## ADDED Requirements

### Requirement: Base coin reward by tier
Completing a task SHALL award coins determined by its tier: 5 for tier 1, 10 for tier 2, 20 for tier 3 and 40 for tier 4.

#### Scenario: Tier 3 task
- **WHEN** the user completes a tier 3 entry
- **THEN** 20 coins are awarded

### Requirement: Append-only coin ledger
Every change to the user's coins SHALL be recorded as a ledger entry with an amount, a timestamp and a reason, and existing entries MUST NOT be edited or removed.

#### Scenario: Reward recorded
- **WHEN** a tier 2 entry named "Reply to emails" is completed
- **THEN** a ledger entry of +10 with a task-completed reason referencing that entry is added

#### Scenario: History preserved
- **WHEN** the source template of a ledger entry is deleted
- **THEN** the ledger entry remains unchanged

### Requirement: Balance
The coin balance SHALL equal the sum of all ledger entry amounts and be shown in the topbar.

#### Scenario: Balance after rewards
- **WHEN** the user completes a tier 1 entry and a tier 4 entry
- **THEN** the balance increases by 45 and the topbar shows the new balance

#### Scenario: New user
- **WHEN** the app starts with no data
- **THEN** the balance is 0
