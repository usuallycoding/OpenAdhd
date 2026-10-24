# Design

## Context

The rules are `const` values and `when` tables in four objects (`CoinRules`, `RewardRules`, `RollRules`, `CriticalRules`) read directly in about 25 places: both stores and the DAO transactions that pay, spend, roll and complete, the view models, and the screens that print amounts. A Task entry already stores the coins it was assigned with (and each subtask its share), and a redemption stores its price, so history does not depend on the rules. `Reward.price` is the exception: it is computed from the tier. The critical-hit chance is built once in `AppContainer` as `CriticalHits.Standard`, and the stores default to `CriticalHits.Never` so tests are deterministic. The database is at version 6; both stores run the same `StoreContractTest`. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One place that holds every tunable value, with defaults, read at the moment an action happens.
- The same behaviour in `RoomStore` and `InMemoryStore`, and no change in behaviour while every setting is at its default.
- A Settings tab that edits and restores each value, with clear refusals.

**Non-Goals:**
- Delete all data, import/export, a "restore everything" button.
- Changing how the theme is chosen, or per-tier chances.

## Decisions

- **A `GameRules` value, not more constants.** `GameRules` holds the debt limit, subtask ceiling, the four task payouts, the four reward prices, the critical chance and multiplier and the upgrade chance, with `GameRules.Default`. `Settings` is the name plus `GameRules`. The rule functions (`baseReward`, `maxSubtasks`, `canPay`, `coinsNeeded`, `fixedPrice`, `bonus`, roll `choose`) take the rules as a parameter, so they stay pure and testable. Alternative: a global mutable singleton; rejected because tests and the two stores would share hidden state.
- **Defaults in code, only overrides in the database.** A `settings` table `(key TEXT PRIMARY KEY, value TEXT NOT NULL)` holds a row only for a changed value. Restore deletes the row, and saving a value equal to the default also deletes it, so "is it the default" is simply "no row". Defaults can then change in a later version without stale copies, and the screen shows "Default: N" from code. Alternative: store defaults in a second column or table, as first suggested; rejected because it duplicates what the code already knows and goes stale. It can be added later if an export should list them.
- **Settings are read inside the transaction that uses them.** `RoomStore` reads the rows in the same transaction as the ledger check for a purchase, a roll, a completion and an assignment, so a setting cannot change between the check and the write. `InMemoryStore` reads its state. Migration 6 to 7 only creates the table.
- **`CriticalHits` is asked with the chance.** `fun isCritical(oneIn: Int): Boolean`: `Never` returns false, `Standard` draws `random.nextInt(oneIn) == 0`. The multiplier goes into `bonus(reward, multiplier)`. The container keeps one `Standard`, and tests keep `Never`.
- **`SettingsRepository` is part of `OpenAdhdStore`.** `settings: Flow<Settings>`, `saveSetting(key, text): SettingResult` and `restoreSetting(key)`. `SettingKey` is an enum of the 16 values. The view models that print amounts (Today, Templates, Rewards, Shop) combine `store.settings` with their own flows, so the screens update at once.
- **Validation is one shared function.** `validateSetting(key, text, settings, rewards, templates)` returns an error message or null: whole number, range, tier order, then the two data checks (tier 4 price against tier 5 rewards, a payout against the subtask count of templates of that tier). Both stores call it before writing, so they refuse the same things. Alternative: allow the change and repair the data; rejected because `splitPool` requires every subtask to get 1 coin, and a tier 5 reward cheaper than tier 4 breaks the shelf and roll logic.
- **Ceiling is not checked against existing templates.** Lowering it is allowed; a template with more subtasks keeps them and is refused on its next save until trimmed, which the form already explains. Alternative: refuse; rejected as it would force editing many templates before changing one number.
- **`RewardTemplate.price` becomes a function of the rules.** Tiers 1 to 4 follow the setting at once; tier 5 keeps its custom price. Redemptions already store the price paid.
- **Screen.** `SettingsScreen` lists the groups Profile, Coins, Subtasks, Shop and Luck. A row shows the name, the value and "Default: N"; tapping it opens a `FormSheet` with a numeric (or text, for the name) field and Save and Cancel; a Restore button on the row is disabled at the default. A fourth `Destination.Settings` joins the bottom bar with a gear icon; it has no reorder control.
- **Name in the title.** "<name>'s OpenAdhd" with no special handling of names ending in s; blank or default shows "OpenAdhd".

## Risks / Trade-offs

- [About 25 call sites change signature] → Do the domain first with the defaults as the only caller input, run the whole existing suite, which must stay green with the defaults, then wire the settings in.
- [Stores diverge on refusals] → One shared `validateSetting`, and contract tests that run against both stores.
- [A setting is changed while a completion is in flight] → Settings are read inside the same transaction as the work.
- [A lowered debt limit below the current debt blocks purchases] → The balance is never changed; the Shop already shows how many coins are needed.
- [Number input edge cases (empty, signs, huge numbers)] → Parse as whole numbers with a length cap, and report one clear message per rule.

## Migration Plan

Version 7 creates the `settings` table and nothing else; existing data and the balance are untouched, and every setting starts at its default. Rolling back means removing the table, which only loses the settings.
