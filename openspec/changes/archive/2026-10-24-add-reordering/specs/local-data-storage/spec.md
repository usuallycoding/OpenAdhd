# Spec Delta

## MODIFIED Requirements

### Requirement: Existing data survives an upgrade
When the app is updated from an earlier version, all templates, Today entries and ledger entries saved by that version SHALL still be present and unchanged in their coin values, and templates, reward templates and Today entries SHALL keep the order they had.

#### Scenario: Upgrade keeps data
- **WHEN** the app is updated while the device holds templates, a completed entry and a coin balance of 40
- **THEN** after the update the templates and the completed entry are present and the balance is still 40

#### Scenario: Upgrade adds an empty shop
- **WHEN** the app is updated from the version that has no rewards
- **THEN** the reward list and the redemption history are empty and the balance is unchanged

#### Scenario: Upgrade keeps earlier purchases
- **WHEN** the app is updated from the version that has the shop but no mystery rolls, and the history holds two purchases
- **THEN** both purchases are still in the history, shown as purchases rather than rolls, and the balance is unchanged

#### Scenario: Upgrade keeps order
- **WHEN** the app is updated from the version that has no reordering while the device holds three templates, two reward templates and two Today entries
- **THEN** each list is in the same order as before the update
