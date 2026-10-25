# data-backup Specification

## Purpose

Lets the user save everything the app holds to a file and bring it back, and wipe the app to start from scratch, so data survives a lost device, a reinstall or a move to another device.

## Requirements

### Requirement: Data section
The Settings tab SHALL show a Data section after the settings groups with the rows Export a backup, Import a backup and Delete all data. Export and Import MUST be hidden on a platform that has no way to save or open a file; Delete all data is always shown.

#### Scenario: Rows shown
- **WHEN** the user opens Settings on a platform with a file dialog and scrolls to the end
- **THEN** the Data section shows Export a backup, Import a backup and Delete all data

#### Scenario: No file dialog
- **WHEN** the platform has no way to save or open a file
- **THEN** the Data section shows only Delete all data

### Requirement: Export a backup
Export a backup SHALL write one JSON file, saved where the user chooses in the system's file dialog, with a suggested name made of "openadhd-", today's date and ".json". The user MUST be told whether the file was saved, and cancelling the dialog saves nothing and is not an error.

#### Scenario: Saving a backup
- **WHEN** the user taps Export a backup and chooses a place to save
- **THEN** the file is written there and the app says the backup was saved

#### Scenario: Suggested name
- **WHEN** the file dialog opens on 24 October 2026
- **THEN** the suggested name is "openadhd-2026-10-24.json"

#### Scenario: Cancelled
- **WHEN** the user closes the file dialog without choosing a place
- **THEN** nothing is written and the app shows no error

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

### Requirement: Import a backup
Import a backup SHALL let the user choose a file, read it, and show what it contains, as the number of task templates, reward templates, Today entries and redemptions and the coin balance, before asking whether to replace all current data with it. Confirming SHALL replace everything at once, with no restart, and cancelling SHALL change nothing.

#### Scenario: Import with confirmation
- **WHEN** the user chooses a valid backup file
- **THEN** the app shows its counts and balance and asks to replace all current data

#### Scenario: Confirmed
- **WHEN** the user confirms
- **THEN** every screen shows the backup's data, the coin balance is the backup's balance, and the app says the backup was imported

#### Scenario: Declined
- **WHEN** the user cancels at the confirmation
- **THEN** the current data and settings are unchanged

#### Scenario: No file chosen
- **WHEN** the user closes the file dialog without choosing a file
- **THEN** nothing changes and the app shows no error

### Requirement: A bad file changes nothing
A file that is not a backup, is not valid JSON, is damaged, has missing or invalid values, or has a format newer than the app understands MUST NOT change any data or setting, and the app SHALL say what is wrong in plain words. The whole file MUST be checked before the confirmation is shown.

#### Scenario: Not a backup
- **WHEN** the user chooses a JSON file that is not an OpenAdhd backup
- **THEN** the app says it is not an OpenAdhd backup and nothing changes

#### Scenario: Damaged file
- **WHEN** the user chooses a file that is cut off or is not JSON
- **THEN** the app says the file could not be read and nothing changes

#### Scenario: Newer format
- **WHEN** the user chooses a backup whose format version is higher than the app supports
- **THEN** the app says the backup was made by a newer version of OpenAdhd and nothing changes

#### Scenario: Invalid values
- **WHEN** the file has an unknown energy level, a tier outside 1 to 5 or two entries with the same id
- **THEN** the app refuses it, says which kind of value is wrong, and nothing changes

### Requirement: A backup restores the same state
Importing a backup SHALL leave the app in the state it had when the backup was made: the same templates, entries, rewards, redemptions, ledger entries, coin balance and settings, in the same order. It MUST NOT create, merge or keep anything from the data it replaced.

#### Scenario: Round trip
- **WHEN** the user exports, makes other changes, and then imports that file
- **THEN** the app is as it was when exported, and the later changes are gone

#### Scenario: Moving to a new device
- **WHEN** a backup made on one device is imported on a fresh install of another
- **THEN** the second device shows the same templates, history, coins and settings

#### Scenario: Replaces, not merges
- **WHEN** the app holds a template that is not in the backup and the user imports it
- **THEN** that template is gone after the import

### Requirement: Delete all data
Delete all data SHALL ask for confirmation in a dialog that says it removes everything and cannot be undone, and that offers Export a backup first, Delete everything and Cancel. Delete everything SHALL remove every task template, reward template, Today entry, ledger entry, redemption and changed setting, so the app is as on a fresh install, with no restart.

#### Scenario: Confirmation dialog
- **WHEN** the user taps Delete all data
- **THEN** a dialog warns that everything will be removed and cannot be undone, with the choices Export a backup first, Delete everything and Cancel

#### Scenario: Delete everything
- **WHEN** the user confirms Delete everything
- **THEN** every list is empty, the coin balance is 0, every setting shows its default, and the top bar title is "OpenAdhd"

#### Scenario: Cancel
- **WHEN** the user taps Cancel
- **THEN** nothing is removed

#### Scenario: Export first
- **WHEN** the user taps Export a backup first and saves the file
- **THEN** the dialog comes back so the user can then choose Delete everything or Cancel

#### Scenario: Deleted data is gone after a restart
- **WHEN** the user deletes everything and restarts the app
- **THEN** the app is still empty

### Requirement: Backup works on every platform
Export and import SHALL work on desktop, Android and iOS, each using its own system file dialog, and a backup made on one platform MUST be importable on any other.

#### Scenario: Across platforms
- **WHEN** a backup made on the desktop app is opened on a phone
- **THEN** it imports with the same data

### Requirement: Backup holds schedules and template reminders
An export SHALL include each template's schedule, reminder time and repeat, and the day it was last generated for, and an import SHALL restore them. A backup made before schedules existed MUST still import, with every template's schedule None and no reminder.

#### Scenario: Round trip
- **WHEN** the user exports a Weekly template with a reminder at 09:00 every 30 minutes and imports the file
- **THEN** the template has the same schedule, reminder and last-generated day

#### Scenario: Older file
- **WHEN** the user imports a backup that has no schedule fields
- **THEN** the import succeeds and every template has the schedule None and no reminder

### Requirement: Backup holds the reminder kind
An export SHALL include the kind of each task's reminder and of each template's reminder, and an import SHALL restore it. A backup made before reminder kinds existed MUST still import, with every reminder a Notification.

#### Scenario: Round trip
- **WHEN** the user exports a task with an alarm reminder and a template with an alarm reminder and imports the file
- **THEN** both reminders are alarms again

#### Scenario: Older file
- **WHEN** the user imports a backup whose reminders have no kind
- **THEN** the import succeeds and every reminder is a Notification

#### Scenario: Unknown kind
- **WHEN** the user imports a file whose reminder kind is neither Notification nor Alarm
- **THEN** the import is refused and nothing is replaced
