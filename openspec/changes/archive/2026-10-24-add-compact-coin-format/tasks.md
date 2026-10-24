# Tasks

## 1. Formatter

- [x] 1.1 Add `formatCoins` to the domain package (integer arithmetic, truncating, drops a zero decimal, keeps the sign) with `CoinFormatTest` covering 0, 999, 1000, 1123, 1999, 12,500, 99,999, 100,000, 100,400, 999,999, 1,000,000, 1,250,000, 99,999,999, 123,456,789, -1500 and `Int.MIN_VALUE` without overflow, and verify the test passes

## 2. Screens

- [x] 2.1 Use `formatCoins` for the top bar balance chip and the critical hit and undo messages in `App.kt`, and for the import preview balance in `SettingsScreen`; add a UI test that a balance of 1123 shows 1.1k and a negative one shows -1.5k, and verify it passes
- [x] 2.2 Use `formatCoins` for the subtask share and entry reward in `TodayScreen`, and for prices, "coins needed", debt notes, roll prices, the roll result and purchase history in `ShopScreen`; verify with UI tests on a 1250-coin reward and a large "more coins needed"
- [x] 2.3 Use `formatCoins` for the Rewards tab summary and "Costs N coins" in `RewardsScreen`, leave the price input and validation messages exact, and verify with a UI test that the list shows 1.2k coins while the edit field shows 1250

## 3. Check

- [x] 3.1 Search `App.kt` and `ui/` for remaining coin interpolations, update any existing test assertions on amounts of 1000 or more, run `openspec validate add-compact-coin-format --strict` and the full test suite, and verify both pass
