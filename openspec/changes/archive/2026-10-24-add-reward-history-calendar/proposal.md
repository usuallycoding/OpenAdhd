# Proposal

## Why

The Shop's History tab is one list of every redemption ever made, so it only grows, and it says nothing about what a reward actually was: the description the user wrote is not shown. The Today screen already has a History tab that looks at one day at a time with a calendar; the reward history should work the same way, and each entry should show its reward's description.

## What Changes

- **Per-day reward history:** the Shop's History tab replaces the flat all-time list with a view of one day: a date button, previous and next day arrows, and a calendar dialog, like the Today screen's History tab. It starts on **today** (not yesterday) and the next-day arrow is hidden while today is shown.
- **Calendar:** only days up to and including today that have at least one redemption can be picked. The days come from the redemptions' times, converted to local dates; no new database query.
- **Day list:** the redemptions of the chosen day, newest first, each with its name, its description when it has one, the existing note (tier, mystery roll, lucky upgrade, time) and the price paid. A day with none shows "Nothing redeemed on this day".
- **Description kept with the redemption:** a redemption stores the reward's description when it is made, for purchases and mystery rolls alike, so editing or deleting the reward later does not change the history. Redemptions made before this change get the description of their reward if it still exists, and none otherwise.
- **Shared date bar:** the date button, arrows and calendar dialog are extracted from the Today screen's History tab into one component used by both History tabs. The Today screen's History behaves exactly as before.
- The database goes from version 5 to 6 with a migration (one new column on redemptions).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `reward-shop`: the redemption history becomes a per-day view with a date, a calendar and descriptions, and a redemption keeps the reward's description.
- `local-data-storage`: redemptions are saved with their description, and an upgrade keeps earlier redemptions and gives them a description where it can.

## Impact

- `shared`: `Redemption` and `RedemptionEntity` gain `description`; `Migration5To6` and version 6 in `OpenAdhdDatabase`; `redeemReward` and `mysteryRoll` in the DAO and the same two operations in `InMemoryStore` copy the description; a new view model for the chosen day; the Shop's `HistoryList` is replaced; the date bar and calendar dialog move out of `HistoryScreen` into a shared component, and `HistoryDays` gets a way to include today in the allowed range.
- Tests: the existing Shop History tests change to the per-day view; new tests cover the migration, the stored description, the day rule and the screen.
- No new libraries.
- Out of scope: totals or spending per day, searching the history, filtering by tier, and saving the chosen day or any other tab state across restarts. The Today screen's History (`day-history`) is unchanged.
