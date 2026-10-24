# Tasks

## 1. Stores

- [x] 1.1 Move `StoreState` to the domain package and add `DataRepository` (`exportData`, `importData`, `deleteAllData`) to `OpenAdhdStore`, implemented by `InMemoryStore` (verify: the project builds and the existing tests pass)
- [x] 1.2 Implement them in `RoomStore` with DAO queries that read every table in list order and replace or clear all tables in one transaction, renumbering positions from the order (verify: contract tests in `StoreContractTest` for both stores: export of a populated store, import into an empty and into a populated store replaces everything, order and balance are kept, delete all empties everything including settings)
- [x] 1.3 Add a Room restart test (verify: in `RoomStoreTest`, data imported or deleted is as left after closing and reopening the database)

## 2. Backup format

- [x] 2.1 Switch on kotlinx-serialization JSON in `shared/module.yaml` and add `BackupJson.encode` and `decode` with the `@Serializable` DTOs, the format marker and version, and the validation listed in design.md (verify: unit tests for a round trip of a full `StoreState`, a changed-settings-only export, and each refusal: not a backup, not JSON, truncated, newer version, bad enum, bad tier, duplicate ids, unparsable date)

## 3. File dialogs

- [x] 3.1 Add the `DataFiles` interface and `AppContainer.files`, with a fake for tests, and the desktop implementation using `FileDialog` (verify: the desktop app saves and opens a file, checked by hand in 6.2)
- [x] 3.2 Add the Android implementation with the activity result API created in `MainActivity` and wire it into `createAndroidContainer` (verify: builds for Android; checked by hand in 6.3)
- [x] 3.3 Add the iOS implementation with `UIDocumentPickerViewController` and wire it into `createIosContainer` or `ViewController` (verify: the iOS native build compiles; checked by hand in 6.4)

## 4. Settings screen

- [x] 4.1 Add the Data section, the view model flows for export, import with its summary and confirmation, delete with its two-step dialog and "Export a backup first", and the result and error messages (verify: view model tests with a fake `DataFiles` covering saved, cancelled, bad file, confirmed and declined imports and delete)
- [x] 4.2 Add the Data rows and dialogs to `SettingsScreen`, hiding Export and Import when `files` is null (verify: UI tests for the rows, the import summary, a refused file, replace, the delete dialog's three choices, and the hidden rows without `DataFiles`)

## 5. Docs

- [x] 5.1 Add a Backup section to `README.md` saying what the file holds and how to move data to another device (verify: the steps work as written in 6.2 to 6.4)

## 6. Verification

- [x] 6.1 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 6.2 On macOS: export a backup, change data, import the file and see the earlier state, import a non-backup file and see it refused, delete all data and see the app empty after a restart (verify: manual, as the data-backup spec says)
- [x] 6.3 On an Android phone: the same, and import a backup made on the Mac (verify: manual)
- [x] 6.4 On the iOS simulator or a device: export to Files, import it back, delete all data (verify: manual)
