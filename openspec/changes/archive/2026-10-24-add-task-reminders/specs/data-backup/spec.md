# Spec Delta

## MODIFIED Requirements

### Requirement: Backup contents
A backup SHALL contain everything the app holds: task templates with their subtasks, reward templates, the Today entries of every day with their subtasks, completion times and reminders, the coin ledger, the redemptions, the changed settings, and the order of every list. It MUST record a format version and that it is an OpenAdhd backup.

#### Scenario: Everything is included
- **WHEN** the app holds templates, entries, completed work, rewards, purchases and a changed debt limit, and the user exports
- **THEN** the file holds all of them

#### Scenario: Order is kept
- **WHEN** the user has reordered templates and exports and then imports the file
- **THEN** the templates are in the order they were in when exported

#### Scenario: Only changed settings
- **WHEN** only the debt limit differs from its default
- **THEN** the file lists the debt limit as the only setting

#### Scenario: Reminders are kept
- **WHEN** a pending entry has a reminder and the user exports and then imports the file
- **THEN** the entry has the same reminder and its upcoming times fire again
