# Design

## Context

Coin amounts are interpolated as plain `Int`s into strings in `App.kt` (balance chip, snackbars), `TodayScreen`, `ShopScreen`, `RewardsScreen` and the import preview in `SettingsScreen`. The Templates tab and the template picker show no coin amounts. Singular wording ("1 coin") is chosen where the amount equals 1 and is unaffected. See proposal.md for the format.

## Goals / Non-Goals

**Goals:**
- One formatter owns the rule, so every screen agrees and the rule is unit-testable.

**Non-Goals:**
- No localisation of the suffixes or separators, no billions (an `Int` tops out near 2147M), no change to inputs or stored values.

## Decisions

- **A pure `formatCoins(Int): String` in the domain package**, next to `CoinRules`. It works on the absolute value with integer arithmetic, never floating point: below 1000 the number; below 100,000 `n / 100` tenths of thousands; below 1,000,000 `n / 1000`; below 100,000,000 `n / 100,000` tenths of millions; otherwise `n / 1,000,000`; the sign is put back in front. Integer division truncates, which gives the required "never round up", and avoids float artefacts such as 1.1k for 1123. A zero tenth is dropped.
  Alternative: `DecimalFormat`/`String.format` — rejected, since it rounds and is not available in common code.
- **Call sites switch from `$amount` to `formatCoins(amount)`** wherever an amount is displayed. Plural/singular choice keeps looking at the raw number.
- **Inputs and validation stay raw**: price and payout text fields, and messages like "must be larger than N", keep the exact number.
- **Accessibility**: content descriptions that carry a number keep the exact value; the compact form is for visible text.

## Risks / Trade-offs

- [Truncation hides precision: a 1999-coin reward reads 1.9k] → accepted by the user; it errs on the side of not overstating what you have or understating what you owe.
- [UI tests asserting full numbers of 1000 or more break] → update those assertions as part of the call-site task.
- [A call site is missed] → a search for `coins`, `balance` and `price` interpolations in `ui/` and `App.kt` is part of the final task.
