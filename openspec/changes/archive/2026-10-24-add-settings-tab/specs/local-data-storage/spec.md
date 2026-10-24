# Spec Delta

## ADDED Requirements

### Requirement: Settings are kept
Changed settings SHALL survive a restart of the app and an update from an earlier version, and only changed settings are stored: a setting that was restored or never changed has the default value.

#### Scenario: Restart keeps a setting
- **WHEN** the user sets the debt limit to 200 and the app is restarted
- **THEN** the debt limit is still 200

#### Scenario: Restore survives a restart
- **WHEN** the user changes the debt limit, restores it, and the app is restarted
- **THEN** the debt limit is 500

#### Scenario: Upgrade gives defaults
- **WHEN** the app is updated from a version without settings
- **THEN** every setting has its default, and templates, entries, rewards, redemptions and the balance are unchanged
