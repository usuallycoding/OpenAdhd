# Spec Delta

## MODIFIED Requirements

### Requirement: Swipe between tabs
On the Today, Templates and Shop screens, swiping horizontally SHALL move between the tabs, and the selected tab in the tab row MUST always match the tab being shown.

#### Scenario: Swipe to the second tab
- **WHEN** the user swipes left on the Tasks tab of the Templates screen
- **THEN** the Rewards tab is shown and the Rewards tab is marked as selected

#### Scenario: Swipe back
- **WHEN** the user swipes right on the Rewards tab of the Templates screen
- **THEN** the Tasks tab is shown and marked as selected

#### Scenario: No tab beyond the ends
- **WHEN** the user swipes right on the first tab or left on the last tab
- **THEN** the shown tab does not change

#### Scenario: Shop tabs
- **WHEN** the user swipes left on the Rewards tab of the Shop
- **THEN** the History tab is shown and marked as selected

#### Scenario: Today tabs
- **WHEN** the user swipes left on the Today tab of the Today screen
- **THEN** the History tab is shown and marked as selected

### Requirement: Tapping a tab
Tapping a tab on the Today, Templates or Shop screen SHALL show that tab.

#### Scenario: Tap
- **WHEN** the user taps the History tab on the Shop
- **THEN** the History tab is shown and marked as selected

#### Scenario: Tap on Today
- **WHEN** the user taps the History tab on the Today screen
- **THEN** the day history is shown and the History tab is marked as selected

### Requirement: Selected tab is remembered
The Today, Templates and Shop screens SHALL each remember their selected tab while the app is running, including when the user goes to another screen and returns, and the screens MUST NOT affect each other. The selection is not saved: after the app is restarted each screen starts on its first tab.

#### Scenario: Leave and return
- **WHEN** the user selects the Rewards tab on Templates, goes to Today, and returns to Templates
- **THEN** the Rewards tab is shown

#### Scenario: Independent screens
- **WHEN** the user selects the History tab on the Shop and then opens Templates
- **THEN** Templates shows the tab it last had, not the History tab

#### Scenario: Today screen remembers
- **WHEN** the user selects the History tab on the Today screen, opens the Shop, and returns to Today
- **THEN** the History tab is shown

#### Scenario: After a restart
- **WHEN** the app is restarted
- **THEN** Today shows its Today tab, Templates shows its Tasks tab and the Shop shows its Rewards tab
