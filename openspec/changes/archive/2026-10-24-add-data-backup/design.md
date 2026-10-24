# Design

## Context

All data sits in one Room database that each platform's entry point opens once and keeps in `AppContainer`. `OpenAdhdStore` is implemented twice, by `RoomStore` and `InMemoryStore`, which share `StoreContractTest`. `InMemoryStore.kt` already defines `StoreState`, "the whole persisted state", holding templates, planned entries, the ledger, rewards, redemptions and the settings overrides; list order is the list order. Room keeps order in a `position` column (negative positions are possible for new items) and the stores read lists sorted by it. The project has no serialization library and no file-picker code, and the three platform entry points differ: `desktopApp/src/main.kt`, `MainActivity` and `iosApp/src/ViewController.kt`. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- A backup that round-trips the whole app exactly, readable by a later version of the app.
- Import that is all or nothing and needs no restart.
- One shared implementation of everything except the three file dialogs.

**Non-Goals:**
- Merging, automatic backups, cloud sync, encryption, or exporting a part of the data.

## Decisions

- **JSON, not a copy of the database file.** A versioned JSON file can be checked completely before anything changes and replaced live in one transaction, while swapping the database file needs the database closed, a restart and a way back from a bad file. JSON is also readable and survives schema changes through its `formatVersion`. Alternative: `VACUUM INTO` plus a file swap; rejected for those reasons.
- **`StoreState` is the backup model.** It moves to the domain package, and the store interface gets `DataRepository`: `exportData(): StoreState`, `importData(data: StoreState)` and `deleteAllData()`. Alternative: a separate backup type with its own mapping; rejected because `StoreState` already is that snapshot and both stores can read and write it.
- **Serialization DTOs stay out of the domain.** `BackupJson.encode(state, exportedAt)` and `BackupJson.decode(text)` convert to private `@Serializable` classes (dates as ISO text, instants as epoch milliseconds, enums by name). kotlinx-serialization is switched on in `shared/module.yaml` with `serialization: format: json`; the toolchain bundles version 1.11.0. Alternative: hand-written JSON; rejected as error-prone.
- **Format.** The file has `"app": "openadhd-backup"`, `"formatVersion": 1`, `"exportedAt"`, and the lists. Unknown extra fields are ignored so that a later version can add some. A `formatVersion` above the supported one is refused; the decoder is the one place that later maps older versions up.
- **Validation before the confirmation.** `decode` returns `Backup` or a `Problem(message)`. It checks the marker and version, JSON syntax, required fields, enum names, tiers 1 to 5, a custom price only on tier 5, parsable dates and instants, and unique ids per kind (templates, subtasks, entries, ledger, rewards, redemptions). Settings rows with unknown keys or invalid values are ignored, as `Settings.fromOverrides` already does. The import screen shows counts and the balance from the decoded data.
- **Room replaces in one transaction.** `importData` deletes every table and inserts the rows, with positions numbered 0..n-1 in list order (per date for Today entries and per template for subtasks), so negative positions disappear. `deleteAllData` is the same delete without the inserts. Flows then re-emit, so screens update without a restart. `InMemoryStore` swaps its state.
- **Delete all includes the settings.** "As on a fresh install": the settings table is emptied too. Alternative: a "Keep my settings" checkbox; rejected to keep the dialog simple, since a backup can bring the settings back.
- **`DataFiles` is the only platform seam.** `interface DataFiles { suspend fun save(suggestedName: String, text: String): Boolean; suspend fun open(): String? }`, held by `AppContainer.files` (null where there is none). Reading and writing the text happens inside it. Implementations:
  - desktop: `java.awt.FileDialog` shown on the AWT event thread, then plain file IO;
  - Android: `registerForActivityResult` with `CreateDocument("application/json")` and `OpenDocument`, created in `MainActivity` before it starts, bridged with a `CompletableDeferred`, text read and written through the content resolver;
  - iOS: `UIDocumentPickerViewController` for exporting a temp file and for opening JSON, presented from the key window's root view controller, with a delegate bridged by `suspendCoroutine` and security-scoped access around the read.
  Alternative: the FileKit library; rejected because three small classes avoid a dependency whose compatibility with this Compose version is unchecked.
- **UI.** The view model exposes `exportBackup()`, `chooseImport()`, `confirmImport()`, `delete()` and a message to show; the screen renders the Data rows, the import summary dialog, the delete dialog and the result messages as dialogs or a line of text, in the style of the existing Shop dialogs. The delete dialog's "Export a backup first" runs the export and returns to the dialog.

## Risks / Trade-offs

- [A bad file destroys current data] → Decode and validate fully first; replace in one transaction; always ask before replacing; the dialog says what is about to be replaced.
- [A write fails half way] → The delete and the inserts run inside one `@Transaction`, so Room rolls everything back and the old data stays.
- [Large ledgers make big files] → Fine for personal use; the JSON is written once from memory.
- [iOS picker cannot be tested automatically] → The class compiles in the native build; checked by hand on the simulator and, ideally, a device.
- [Desktop `FileDialog` threading on macOS] → Created and shown on the AWT event thread only.
- [Moving `StoreState` touches imports] → It is used in one file today; the move is mechanical.

## Migration Plan

No database change: the schema stays at version 7. `StoreState` changes package only. Old backups cannot exist yet, so format version 1 is the first.
