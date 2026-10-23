# Spec Delta

## MODIFIED Requirements

### Requirement: Data survives restart
On Android, iOS and desktop, task templates with their descriptions and subtasks, Today entries with their subtask progress, and the coin ledger SHALL be saved on the device and restored when the app is reopened.

#### Scenario: Restart keeps data
- **WHEN** the user creates a template, completes an entry for coins, and restarts the app
- **THEN** the template, the completed entry and the coin balance are all present

#### Scenario: Restart keeps subtask progress
- **WHEN** the user completes one of an entry's three subtasks and restarts the app
- **THEN** that subtask is still completed, the other two are pending, and the coins earned are still in the balance

## ADDED Requirements

### Requirement: Existing data survives an upgrade
When the app is updated from the version that has no descriptions or subtasks, all templates, Today entries and ledger entries saved by that version SHALL still be present and unchanged in their coin values.

#### Scenario: Upgrade keeps data
- **WHEN** the app is updated while the device holds templates, a completed entry and a coin balance of 40
- **THEN** after the update the templates and the completed entry are present, they have no description or subtasks, and the balance is still 40
