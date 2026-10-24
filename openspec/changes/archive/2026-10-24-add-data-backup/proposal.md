# Proposal

## Why

Everything the user builds up, templates, history, coins and settings, lives only in one database file on one device. A lost phone, a reinstall or a move to a new Mac means starting over, and there is no way to wipe the app and begin again short of deleting its files by hand. A backup file and a reset button close both gaps.

## What Changes

- **Data section in Settings:** three rows under the settings groups: Export a backup, Import a backup and Delete all data.
- **Export:** writes everything to one JSON file the user saves through the system's file dialog: templates with subtasks, Today entries and their progress, the coin ledger, rewards, redemptions, the order of every list and the changed settings. The file records a format version.
- **Import:** reads a backup file, checks it completely, shows what it contains and asks before replacing all current data. A file that is not a backup, is damaged, or comes from a newer format changes nothing and says why. A valid import replaces everything in one step, with no restart.
- **Delete all data:** after a two-step confirmation that offers "Export a backup first", removes every template, entry, ledger entry, reward, redemption and changed setting, so the app is as on a fresh install.
- **All three platforms:** macOS (desktop), Android and iOS each get their own file dialog behind one small `DataFiles` interface. Where a platform has none, Export and Import are hidden and Delete all data stays.
- **Not in this change:** merging a backup into existing data, scheduled or automatic backups, cloud sync, and encryption of the file.

## Capabilities

### New Capabilities
- `data-backup`: exporting, importing and deleting all data, the file's contents and format version, and what happens with bad files.

### Modified Capabilities
- `app-settings`: the Settings tab also shows a Data section after the settings groups.

## Impact

- `shared` domain: `StoreState` moves from the data package to the domain package; a `DataRepository` (`exportData`, `importData`, `deleteAllData`) joins `OpenAdhdStore`; a backup format with encoding, decoding and validation; the `DataFiles` interface.
- `shared` data: `RoomStore` reads every table in list order and replaces them in one transaction (positions renumbered from the order); `InMemoryStore` does the same on its state; `AppContainer` gains an optional `files`.
- Platform code: a file dialog for desktop (`java.awt.FileDialog`), Android (the system document picker through the activity result API) and iOS (`UIDocumentPickerViewController`).
- `shared` UI: the Data section in `SettingsScreen`, its confirmations and messages, and the matching view model code.
- Build: kotlinx-serialization for JSON, switched on in `shared/module.yaml`; no database change (the version stays at 7).
- Tests: round trips and replace-all behaviour in the shared store contract tests, backup format and validation tests, UI tests with a fake `DataFiles`. The three real file dialogs are checked by hand.
