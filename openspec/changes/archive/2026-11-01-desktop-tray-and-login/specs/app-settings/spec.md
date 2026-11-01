# Spec Delta

## ADDED Requirements

### Requirement: Launch at login setting
On macOS, when the app is the packaged app, Settings SHALL list a Launch at login switch in a Startup section placed after the Reminders group and before the Data section, off by default. It MUST NOT be listed anywhere else. It is not one of the saved settings: it MUST NOT be part of a backup, and Delete all data MUST NOT change it.

#### Scenario: On macOS
- **WHEN** the user opens Settings in the packaged macOS app
- **THEN** a Startup section after Reminders lists Launch at login, off

#### Scenario: Turn on
- **WHEN** the user turns Launch at login on
- **THEN** the switch shows on and the app starts at the next login

#### Scenario: Other platforms
- **WHEN** the user opens Settings on Android, iOS, Windows or Linux
- **THEN** there is no Startup section

#### Scenario: Backup and Delete all data
- **WHEN** Launch at login is on and the user exports a backup, or deletes all data
- **THEN** the backup does not mention it and the switch is still on afterwards
