# Tasks

## 1. Rules as values

- [x] 1.1 Add `GameRules` with `GameRules.Default`, `Settings` (name plus rules) and the `SettingKey` enum with each key's default and range; make `CoinRules`, `RewardRules`, `RollRules` and `CriticalRules` functions take the rules; give `CriticalHits` the chance as `isCritical(oneIn)` and `bonus(reward, multiplier)` (verify: unit tests for each rule function with default and changed rules, and the existing domain tests still pass with the defaults)
- [x] 1.2 Add the shared `validateSetting` (whole number, range, tier order, tier 4 price against tier 5 rewards, payout against template subtask counts) with its message texts (verify: unit tests for every rule in the app-settings spec, including accepted boundaries)

## 2. Storage

- [x] 2.1 Add the `settings` table (`SettingEntity`, DAO queries to read, upsert and delete), `Migration6To7` and database version 7 (verify: the migration test opens a version 6 database with data and finds the data unchanged and every setting at its default)
- [x] 2.2 Add `SettingsRepository` to `OpenAdhdStore` and implement it in `RoomStore` and `InMemoryStore`: `settings` flow, `saveSetting` (validated, equal to the default stores nothing) and `restoreSetting` (verify: contract tests in `StoreContractTest` for both stores covering save, restore, refusal messages and surviving a store restart in `RoomStoreTest`)
- [x] 2.3 Read the rules inside the transactions and store operations that assign a task, complete a task or subtask, redeem, roll and create or update a template or reward, and use the new `CriticalHits` (verify: contract tests with a changed debt limit, payouts, prices, chances and multiplier in both stores, and all existing tests unchanged with the defaults)

## 3. Screens use the rules

- [x] 3.1 Make the Today picker and list, the template form and its validation, the reward form and the Shop (shelves, affordability, roll options and odds text) read the current rules from `store.settings` (verify: view model tests with changed rules, and the existing UI tests pass with the defaults)
- [x] 3.2 Show "<name>'s OpenAdhd" in the top bar when a name is set (verify: UI test for a name, a blank name and a restored name)

## 4. Settings tab

- [x] 4.1 Add a gear icon, `Destination.Settings` as the fourth bottom-bar item without a reorder control, and `SettingsViewModel` (verify: view model test that saving and restoring update the shown value, default and restore state)
- [x] 4.2 Add `SettingsScreen` with the groups, rows (name, value, default), the edit `FormSheet` with numeric input and inline errors, and the Restore button disabled at the default (verify: UI tests for opening the tab, editing a value, cancelling, a refused value, restoring, and the Restore button's state)

## 5. Verification

- [x] 5.1 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 5.2 Try it on desktop or a device: set a name, lower the debt limit and the subtask ceiling, change a payout and a price, set the critical chance to 1 in 2, restore each one, restart the app and see the values kept, and see the refusals for a tier 4 price at a tier 5 reward and a payout under a template's subtask count (verify: manual, everything behaves as the app-settings spec says)
