# local-data-storage Specification

## Purpose

Guarantees that the user's templates, daily entries and coin history stay on their device and survive closing and reopening the app.

## Requirements

### Requirement: Data survives restart
On Android, iOS and desktop, task templates, Today entries and the coin ledger SHALL be saved on the device and restored when the app is reopened.

#### Scenario: Restart keeps data
- **WHEN** the user creates a template, completes an entry for coins, and restarts the app
- **THEN** the template, the completed entry and the coin balance are all present

### Requirement: Data stays on the device
The app SHALL NOT send the user's data to any remote service, and MUST NOT require an account.

#### Scenario: No account
- **WHEN** the app is installed and opened for the first time
- **THEN** the user can start creating templates immediately without signing in
