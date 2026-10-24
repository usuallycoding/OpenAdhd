# Proposal

## Why

The numbers that make up the app's economy are fixed in the code: the debt limit, the coins each task tier pays, the Shop prices, the subtask ceiling, the critical-hit and roll-upgrade chances. The only way to try a different balance is to change the source and rebuild, and the app has no place for anything personal such as a name. A Settings tab lets each of these be edited, and put back to its default, on the device.

## What Changes

- **Settings tab:** a fourth bottom-bar item. Each setting shows its current value and its default, can be edited in a bottom-sheet form, and has a Restore button that puts the default back. Restore is disabled while the value already is the default.
- **Settings:**
  - Your name, shown in the top bar as "<name>'s OpenAdhd" (blank keeps "OpenAdhd").
  - The debt limit (default 500).
  - The subtask ceiling (default 10).
  - The coins each of the four task tiers pays (default 5, 10, 20, 40).
  - The Shop price of each of the four reward tiers (default 20, 50, 120, 300). The tier 5 minimum price follows tier 4.
  - The critical hit chance, 1 in N (default 8), and its multiplier (default 2).
  - The roll upgrade chance, 1 in N (default 6).
- **Defaults live in code, overrides in the database:** the database holds only the values you changed. Restoring a value removes its row, and the screen shows the default next to each value.
- **Validation:** each value has a range, tier values must keep increasing from tier 1 to tier 4, and a change that would make existing data invalid is refused with a message naming the item (a tier 4 price at or above a tier 5 reward's price, a task payout below the subtask count of a template of that tier).
- **History is untouched:** a change applies to what happens next. Assigned entries keep their coins, redemptions keep their price, and the ledger is never rewritten. A reward's Shop price follows its tier, so a new tier price applies to rewards already created.
- **Storage:** a new `settings` table and a Room migration from version 6 to 7 that only creates it.
- **Later, not here:** deleting all data and import/export of the database.

## Capabilities

### New Capabilities
- `app-settings`: the Settings tab, the list of settings with their defaults and ranges, editing, restoring defaults, the validation rules and what a change does and does not affect.

### Modified Capabilities
- `coin-economy`: the pool of each task tier and the debt limit are settings (defaults 5/10/20/40 and 500).
- `critical-hit`: the chance and the multiplier are settings (defaults 1 in 8 and 2).
- `mystery-rolls`: roll prices follow the reward tier prices, the upgrade chance is a setting, and the debt limit is the setting.
- `reward-templates`: tier prices are settings, and the tier 5 minimum follows tier 4.
- `task-templates`: the subtask ceiling is a setting.
- `app-topbar`: the title includes the name.
- `local-data-storage`: settings are kept across restarts and upgrades.

## Impact

- `shared` domain: a `GameRules` value with `GameRules.Default` replaces the constants in `CoinRules`, `RewardRules`, `RollRules` and `CriticalRules`; `CriticalHits` takes the chance when asked; a settings repository joins `OpenAdhdStore`; validation of setting values.
- `shared` data: `settings` table, `SettingsEntity` and DAO queries, `Migration6To7` (database version 7), settings reads inside the transactions that pay, spend, roll and complete; the same in `InMemoryStore`.
- `shared` UI: `SettingsScreen` and view model, a settings icon, a fourth destination in `App.kt`, the top bar title, and the screens that show amounts (Today, Templates form, Shop) reading the current rules.
- Tests: store contract tests for settings in both stores, a migration test, rule and validation unit tests, view model tests, and UI tests for the tab, editing, restoring and the refused changes. Existing tests keep passing with the defaults.
- No new libraries.
- Out of scope: deleting all data, import/export, a "restore everything" button, per-tier chances, and moving the theme chooser into Settings.
