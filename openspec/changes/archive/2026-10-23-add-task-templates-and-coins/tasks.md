# Tasks

## 1. Domain model and economy rules

- [x] 1.1 Add `domain` models (`Tier`, `Energy`, `TaskTemplate`, `PlannedTask`, `LedgerEntry`, ledger reason) and repository interfaces, and verify `./kotlin build` compiles on jvm, android and ios
- [x] 1.2 Add `CoinRules` with the base reward per tier (5/10/20/40) and a balance function summing ledger amounts, with unit tests in `shared/test` for every tier and an empty/mixed ledger, and verify `./kotlin test` passes

## 2. Repositories (in-memory)

- [x] 2.1 Add in-memory repository implementations plus an injectable clock, supporting template create/edit/delete, assign/remove/list-by-date, and atomic idempotent `complete` that appends the ledger entry; verify with unit tests for validation (empty name), snapshot-after-edit, delete-keeps-entries, complete-once-only, and balance after multiple completions
- [x] 2.2 Add tests for "today" selection with a fake clock (entry from yesterday is not in today's list, same template added twice gives two entries) and verify they pass

## 3. Local persistence

- [x] 3.1 Spike the persistence library on android, ios and jvm with this toolchain (SQLDelight or Room KMP first, JSON file store as fallback), record the outcome in design.md, and verify a trivial write/read round-trip builds and passes on jvm
- [x] 3.2 Add per-platform container factories that choose the storage location (android, ios, jvm) and verify `./kotlin build` succeeds on all targets
- [x] 3.3 Implement the persistent repositories with the same behavior as the in-memory ones, with a transactional (or atomic-write) `complete`, and verify the shared repository test suite passes against it on jvm
- [x] 3.4 Add a restart test that writes templates, entries and ledger data, reopens the store, and verifies everything including the balance is restored

## 4. UI state

- [x] 4.1 Add the Templates view model (list, create, edit, delete, validation error for empty name) and verify with view-model tests using in-memory repositories
- [x] 4.2 Add the Today view model (today's entries, add from template, remove pending, complete) and a wallet state exposing the balance, and verify with tests including day rollover via a fake clock

## 5. Screens and navigation

- [x] 5.1 Build the Templates screen (list with tier and energy, empty state, create/edit dialog, delete) and verify it with a Compose preview and by running the desktop app (`./kotlin run -m desktopApp`)
- [x] 5.2 Build the Today screen (entries with complete/remove actions, empty state, template picker) and verify completing an entry in the desktop app shows it completed and increases the balance
- [x] 5.3 Add the app container wiring per platform entry point, `NavigationBar` navigation between Today and Templates, and the balance in the topbar beside the theme chooser, and verify the topbar title stays centered and navigation works on desktop
- [x] 5.4 Update the README with a short note that data is stored locally on the device, and verify the note matches the implemented storage location

## 6. Integration

- [x] 6.1 Verify end to end on desktop: create a template, add it to today, complete it, restart the app, and confirm the template, completed entry and balance persist
- [x] 6.2 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed, then confirm the app launches on Android and iOS
