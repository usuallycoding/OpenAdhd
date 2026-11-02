# Spec Delta

## MODIFIED Requirements

### Requirement: Shop listing
The app SHALL provide a Shop screen that lists all reward templates grouped by tier from 1 to 5, each with its name, description, energy level and price. Within tier 5, rewards SHALL be ordered by price from lowest to highest.

#### Scenario: Grouped by tier
- **WHEN** rewards exist in tiers 1, 2 and 5
- **THEN** the shop shows those three tiers in order, each with its rewards and their prices

#### Scenario: Tier 5 ordering
- **WHEN** the shop holds tier 5 rewards priced 900 and 500
- **THEN** the 500 reward is listed before the 900 reward

#### Scenario: Empty shop
- **WHEN** no reward templates exist
- **THEN** the shop shows "The shop is empty." with a Take me there button under it

#### Scenario: Take me there
- **WHEN** no reward templates exist and the user presses Take me there
- **THEN** the Templates screen is shown on its Rewards tab and the form to create a reward template is open
