# Spec Delta

## MODIFIED Requirements

### Requirement: Redemption keeps what was paid
Each redemption SHALL record the reward's name, tier, energy level and the price paid when it was bought, and whether it was a purchase or a mystery roll and, for a roll, the tier that was rolled. None of this MUST change when the reward is later edited or deleted.

#### Scenario: Price changed later
- **WHEN** a reward bought for 50 coins is edited to tier 3
- **THEN** its redemption still shows 50 coins

#### Scenario: Roll details kept
- **WHEN** a reward won by rolling tier 2 is later deleted
- **THEN** its redemption still shows the name, the 50 coins paid and that it was a tier 2 mystery roll

### Requirement: Redemption history
The app SHALL show a history of redemptions inside the shop, newest first, each with its name, price paid and when it was redeemed, and for mystery rolls also a note that it was a roll, the tier rolled and whether it was a lucky upgrade.

#### Scenario: History order
- **WHEN** the user has redeemed rewards A and then B
- **THEN** the history lists B before A

#### Scenario: No purchases yet
- **WHEN** nothing has been redeemed
- **THEN** the history shows an empty state

#### Scenario: Rolls are marked
- **WHEN** the user rolled tier 2 and won a tier 3 reward
- **THEN** its history entry shows the reward name, the 50 coins paid, that it was a mystery roll, and that it was a lucky upgrade
