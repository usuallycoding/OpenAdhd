# critical-hit Specification

## Purpose

Adds a surprise to finishing work: every completed task or subtask has a small chance of a critical hit that pays a bonus on top of the normal reward.

## Requirements

### Requirement: Critical hit chance
Completing a task that has no subtasks, or completing a subtask, SHALL be a critical hit with the chance set in Settings, 1 in 8 by default, decided at the moment of completion and independently for each completion.

#### Scenario: Usual completion
- **WHEN** a completion is not a critical hit
- **THEN** only the normal reward is paid

#### Scenario: Critical completion
- **WHEN** a completion is a critical hit
- **THEN** the normal reward and a bonus are both paid

#### Scenario: Each subtask is decided on its own
- **WHEN** the user completes three subtasks of one entry
- **THEN** each completion is decided separately, so any, all or none of them can be critical hits

#### Scenario: Changed chance
- **WHEN** the critical hit chance in Settings is 1 in 2
- **THEN** each completion is a critical hit with a 1 in 2 chance

### Requirement: Critical hit bonus
A critical hit SHALL pay a bonus so that the completion pays the critical hit multiplier from Settings, 2 by default, times its normal reward in total. The normal reward MUST be the same amount it would have been without the critical hit.

#### Scenario: Critical task
- **WHEN** a tier 3 task without subtasks, worth 20 coins, is completed as a critical hit
- **THEN** 20 coins are paid as the reward and 20 more as the bonus, 40 in total

#### Scenario: Critical subtask
- **WHEN** a subtask worth 7 coins is completed as a critical hit
- **THEN** 7 coins are paid as the reward and 7 more as the bonus

#### Scenario: Changed multiplier
- **WHEN** the multiplier in Settings is 3 and a 20-coin task is completed as a critical hit
- **THEN** 20 coins are paid as the reward and 40 more as the bonus, 60 in total

### Requirement: Critical hit is recorded
A critical hit SHALL be recorded as its own ledger entry for the bonus, with a critical-hit reason referencing the same task or subtask as the reward entry, and the reward entry MUST be recorded exactly as for a usual completion.

#### Scenario: Two entries
- **WHEN** a subtask worth 7 coins is completed as a critical hit
- **THEN** the ledger gets a +7 entry with a subtask-completed reason and a +7 entry with a critical-hit reason, both referencing that subtask, and the balance rises by 14

#### Scenario: No bonus entry otherwise
- **WHEN** a completion is not a critical hit
- **THEN** only the reward entry is added

### Requirement: One chance per completion
Each completion SHALL have exactly one chance at a critical hit. A task or subtask that is already completed MUST NOT be completable again, and a refused completion MUST NOT produce a bonus.

#### Scenario: No second chance
- **WHEN** a completed subtask is completed again
- **THEN** nothing is paid, no bonus is possible and the balance does not change

#### Scenario: Refused completion
- **WHEN** completing an entry that has subtasks is attempted directly
- **THEN** it is refused and no bonus is paid

### Requirement: Critical hit celebration
When a completion is a critical hit, the app SHALL immediately show a short message with the bonus coins that dismisses itself and does not block further actions. The app MUST NOT show a message for usual completions.

#### Scenario: Message on a critical hit
- **WHEN** the user completes a subtask worth 7 coins and it is a critical hit
- **THEN** a message such as "Critical hit! +7 bonus coins" appears and the user can keep completing items without dismissing it

#### Scenario: No message otherwise
- **WHEN** a completion is not a critical hit
- **THEN** no critical hit message appears

### Requirement: Surprise is not announced
The app MUST NOT show or hint, before a task or subtask is completed, whether its completion will be a critical hit.

#### Scenario: Pending items
- **WHEN** a task or subtask is pending
- **THEN** it shows only its normal reward
