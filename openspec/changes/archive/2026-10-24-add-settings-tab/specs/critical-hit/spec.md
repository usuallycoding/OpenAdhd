# Spec Delta

## MODIFIED Requirements

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
