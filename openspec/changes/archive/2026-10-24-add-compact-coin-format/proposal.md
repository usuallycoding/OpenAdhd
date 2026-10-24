# Proposal

## Why

Coin amounts are shown as full numbers everywhere. Once balances and prices grow into the thousands, long numbers crowd the top bar chip and the Shop rows, and are harder to scan. A compact form (1.1k) keeps them short.

## What Changes

- Every coin amount the app displays uses one compact format: below 1000 it is shown as is; from 1000 it is shown in thousands with one decimal (1123 is 1.1k); from 100,000 without the decimal (100,400 is 100k); the same rule repeats in millions (1,250,000 is 1.2M, from 100M no decimal).
- Values are truncated, never rounded up, so 1999 is 1.9k and 999,999 is 999k. A trailing .0 is dropped (1000 is 1k). A negative amount keeps its sign (-1500 is -1.5k).
- Applies to: the top bar balance, the critical hit and undo messages, Today rewards and subtask shares, Shop prices, "coins needed", debt notes, roll prices, purchase history and the roll result, Rewards tab prices, and the balance in the import preview.
- Text inputs (tier prices, payouts, custom reward prices) and validation messages that quote a limit stay exact. Spoken labels that carry a number stay exact.
- No data, storage or backup changes: only how numbers are shown.

## Capabilities

### New Capabilities

### Modified Capabilities
- `coin-economy`: adds the display format for coin amounts.

## Impact

- A new pure formatter in the domain package, with unit tests.
- `App.kt` (balance chip, snackbars), `TodayScreen`, `ShopScreen`, `RewardsScreen` and `SettingsScreen` (import preview balance) call it.
- UI tests that assert on coin text with values of 1000 or more, if any, change to the compact form.
