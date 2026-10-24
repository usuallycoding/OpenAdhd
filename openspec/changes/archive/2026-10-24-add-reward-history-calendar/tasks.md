# Tasks

## 1. Description on the redemption

- [x] 1.1 Add `description` to `Redemption` and `RedemptionEntity`, raise the database to version 6 with `Migration5To6` (add the column with default `''`, then fill it from `reward_templates` through `templateId`, blank when the reward is gone) and register it; verify with a migration test that starts from a version 5 database holding a redemption of an existing reward with the description "Treat", one of a deleted reward and one with no template id, and checks the descriptions "Treat", blank and blank, and that the ledger and balance are unchanged
- [x] 1.2 Copy the reward's description into the redemption in `redeemReward` and `mysteryRoll` (DAO) and in the two matching operations of `InMemoryStore`; verify with store contract tests, run against both stores, that a purchase and a roll each store the description of the reward obtained, that editing the reward afterwards leaves the redemption unchanged, that deleting it does too, and that a restart of the Room store keeps the description

## 2. Day logic

- [x] 2.1 Add an `includeToday` option (default false) to `HistoryDays.isSelectable`, `isSelectableYear` and `SelectableHistoryDays`; verify with unit tests that the default behaves exactly as before, and that with the option on, today with a redemption is selectable while future days and empty days are not
- [x] 2.2 Add `RewardHistoryViewModel` (shared `today` flow, time zone parameter) with the effective date (today until the user chooses, following the clock past midnight, an explicit choice clamped to at most today), `previousDay`, `nextDay`, `pickDay`, `canGoNext`, the days with redemptions from the redemption times as local dates, and the chosen day's redemptions newest first, and create it in `App`; verify with view-model tests for the default today, stepping back and forward with no next on today, picking a day, the default moving across midnight while a choice stays, grouping by local date including a redemption just before and just after local midnight in a zone far from UTC, and newest-first order within a day

## 3. UI

- [x] 3.1 Extract the date button, previous and next arrows and the calendar dialog from `HistoryScreen` into a shared composable that takes `includeToday`, and use it from `HistoryScreen` with `includeToday = false`; verify that the existing History UI tests and day-history unit tests pass without any change
- [x] 3.2 Replace the Shop's `HistoryList` with the shared date bar over the chosen day's redemptions, each row showing the name, the description when not blank, the existing note and `-price`, with "Nothing redeemed on this day" for an empty day, and remove `ShopViewModel.history` if nothing else uses it; verify with UI tests for the default today, previous and next with no next on today, an empty day, the description shown and absent, rolls still marked and lucky upgrades, redemptions of other days not shown, the calendar allowing only days up to today that have redemptions, OK picking a day and Cancel keeping it, and the chosen day kept after visiting another screen; update the existing Shop History tests to the per-day view

## 4. Integration

- [x] 4.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 4.2 Verify on desktop (and on Android or iOS if you can): redeem a reward with a description and see it in today's history, step to older days, open the calendar and confirm only days with redemptions up to today can be picked, roll a mystery reward and check its note and description, edit the reward afterwards and confirm the history does not change, and restart the app to confirm the description is kept
