# Spec Delta

## MODIFIED Requirements

### Requirement: Data survives restart
On Android, iOS and desktop, task templates with their descriptions and subtasks, Today entries with their subtask progress, reward templates, redemptions with how each was obtained and the reward's description, and the coin ledger SHALL be saved on the device and restored when the app is reopened.

#### Scenario: Restart keeps data
- **WHEN** the user creates a template, completes an entry for coins, and restarts the app
- **THEN** the template, the completed entry and the coin balance are all present

#### Scenario: Restart keeps subtask progress
- **WHEN** the user completes one of an entry's three subtasks and restarts the app
- **THEN** that subtask is still completed, the other two are pending, and the coins earned are still in the balance

#### Scenario: Restart keeps rewards and purchases
- **WHEN** the user creates a reward, redeems it for coins, and restarts the app
- **THEN** the reward, the redemption in the history and the reduced balance are all present

#### Scenario: Restart keeps rolls
- **WHEN** the user makes a mystery roll and restarts the app
- **THEN** the won reward is still in the history, still marked as a mystery roll of the same tier, and the reduced balance is present

#### Scenario: Restart keeps the redemption description
- **WHEN** the user redeems a reward that has a description and restarts the app
- **THEN** the redemption in the history still shows that description

### Requirement: Existing data survives an upgrade
When the app is updated from an earlier version, all templates, Today entries and ledger entries saved by that version SHALL still be present and unchanged in their coin values, and templates, reward templates and Today entries SHALL keep the order they had. Redemptions saved by that version SHALL still be present, and one whose reward still exists SHALL get that reward's description, while one whose reward no longer exists gets none.

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

#### Scenario: Upgrade gives earlier redemptions a description
- **WHEN** the app is updated from a version whose redemptions have no description, and the history holds one redemption of a reward that still exists with the description "Treat" and one of a reward that was deleted
- **THEN** the first redemption shows "Treat", the second shows no description, and the balance is unchanged
